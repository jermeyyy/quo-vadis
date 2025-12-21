# Navigation Config Consolidation Plan

## Problem Statement

Currently, the Quo Vadis navigation library requires users to pass `NavigationConfig` to **both** `rememberQuoVadisNavigator()` and `NavigationHost()`:

```kotlin
// Current redundant API pattern
val navigator = rememberQuoVadisNavigator(
    rootDestination = MainTabs::class,
    config = GeneratedNavigationConfig  // ← Config passed here
)

NavigationHost(
    navigator = navigator,
    config = GeneratedNavigationConfig  // ← Config passed AGAIN here
)
```

This creates several issues:
1. **Redundancy** - Same config passed twice
2. **Potential mismatch** - Users could accidentally pass different configs
3. **Increased API surface** - More parameters to understand and manage
4. **Violates DRY** - Don't Repeat Yourself principle violated

## Current Architecture Analysis

### What Navigator Uses from Config
The `TreeNavigator` uses these registries for **navigation decisions**:

| Registry | Purpose in Navigator |
|----------|---------------------|
| `scopeRegistry` | Determines if destination belongs to current container's scope |
| `containerRegistry` | Creates TabNode/PaneNode when navigating to container destinations |
| `deepLinkHandler` | Resolves deep link URIs to destinations |

These are needed at **navigation time** to make correct state mutations.

### What NavigationHost Uses from Config  
The `NavigationHost` uses these registries for **rendering**:

| Registry | Purpose in Host |
|----------|-----------------|
| `screenRegistry` | Renders composable content for each destination |
| `containerRegistry` | Provides wrapper composables (tab bars, pane layouts) |
| `transitionRegistry` | Resolves custom transitions for animations |
| `scopeRegistry` | Currently unused in rendering (passed for completeness) |

### Registry Usage Matrix

| Registry | Navigator | NavigationHost | Notes |
|----------|-----------|----------------|-------|
| `screenRegistry` | ❌ | ✅ | Rendering only |
| `scopeRegistry` | ✅ | (✅) | Navigation logic; passed to host but not directly used |
| `transitionRegistry` | ❌ | ✅ | Animation only |
| `containerRegistry` | ✅ | ✅ | **Shared** - navigation creates containers, host renders wrappers |
| `deepLinkHandler` | ✅ | ❌ | Navigation only |

## Proposed Solution

### Core Principle
**Navigator owns the config. NavigationHost reads config from Navigator.**

The Navigator should be the single source of truth for configuration. NavigationHost should retrieve the config from the Navigator instance it receives.

### API Changes

#### 1. Add `config` property to Navigator interface

```kotlin
interface Navigator : BackPressHandler {
    // ... existing properties ...
    
    /**
     * The navigation configuration.
     *
     * Provides access to all registries for rendering and navigation.
     * NavigationHost uses this to resolve screen content, transitions,
     * and container wrappers.
     */
    val config: NavigationConfig
}
```

#### 2. Update TreeNavigator constructor

```kotlin
@Stable
class TreeNavigator(
    val config: NavigationConfig = NavigationConfig.Empty,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    initialState: NavNode? = null
) : Navigator {
    
    // Derived from config for internal use
    private val scopeRegistry: ScopeRegistry get() = config.scopeRegistry
    private val containerRegistry: ContainerRegistry get() = config.containerRegistry
    private val deepLinkHandler: DeepLinkHandler = config.deepLinkHandler ?: DefaultDeepLinkHandler()
    
    // ... rest of implementation
}
```

#### 3. Simplify rememberQuoVadisNavigator

```kotlin
@Composable
fun rememberQuoVadisNavigator(
    rootDestination: KClass<out NavDestination>,
    config: NavigationConfig,
    key: String? = null
): Navigator {
    val coroutineScope = rememberCoroutineScope()

    return remember(rootDestination, config, key, coroutineScope) {
        val initialState = config.buildNavNode(
            destinationClass = rootDestination,
            key = key,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination.simpleName}. " +
                "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                "or manually registered in the NavigationConfig."
        )

        TreeNavigator(
            config = config,
            initialState = initialState,
            coroutineScope = coroutineScope
        )
    }
}
```

#### 4. Simplify NavigationHost to read config from Navigator

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    windowSizeClass: WindowSizeClass? = null
) {
    // Read config from navigator
    val config = navigator.config
    
    NavigationHostImpl(
        navigator = navigator,
        modifier = modifier,
        screenRegistry = config.screenRegistry,
        containerRegistry = config.containerRegistry,
        transitionRegistry = config.transitionRegistry,
        scopeRegistry = config.scopeRegistry,
        enablePredictiveBack = enablePredictiveBack,
        windowSizeClass = windowSizeClass
    )
}
```

#### 5. Update QuoVadisNavigation one-liner

```kotlin
@Composable
fun QuoVadisNavigation(
    rootDestination: KClass<out NavDestination>,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    key: String? = null,
    enablePredictiveBack: Boolean = platformDefaultPredictiveBack(),
    windowSizeClass: WindowSizeClass? = null
) {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = rootDestination,
        config = config,
        key = key
    )

    // Config no longer passed - NavigationHost reads from navigator
    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
        windowSizeClass = windowSizeClass
    )
}
```

### New Simplified User API

```kotlin
// NEW simplified API
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig
    )
    
    // Config is now implicit - read from navigator
    NavigationHost(
        navigator = navigator,
        modifier = Modifier.fillMaxSize(),
        enablePredictiveBack = true
    )
}

// Or with one-liner (unchanged interface)
@Composable
fun App() {
    QuoVadisNavigation(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig
    )
}
```

## Migration Strategy

### Phase 1: Add config to Navigator (Non-Breaking)

1. Add `val config: NavigationConfig` to `Navigator` interface with default implementation
2. Update `TreeNavigator` to accept config and expose it
3. Update `rememberQuoVadisNavigator` to pass config to TreeNavigator
4. **Keep existing NavigationHost overloads working**

### Phase 2: Add New Simplified NavigationHost Overload

1. Add new `NavigationHost(navigator, modifier, enablePredictiveBack, windowSizeClass)` overload
2. This overload reads config from navigator
3. Document as the preferred API

### Phase 3: Deprecate Config Parameter in NavigationHost

1. Mark `NavigationHost(navigator, config, ...)` overload as `@Deprecated`
2. Provide `ReplaceWith` pointing to new overload
3. Update all documentation and examples

### Phase 4: Remove Deprecated API (Next Major Version)

1. Remove deprecated overload
2. Clean up internal code

## Files to Modify

### Core Changes
| File | Changes |
|------|---------|
| [Navigator.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt) | Add `config` property to interface |
| [TreeNavigator.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt) | Accept `config` in constructor, expose via property |
| [NavigationHost.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavigationHost.kt) | Add overload reading config from navigator, deprecate old overload |
| [QuoVadisComposables.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisComposables.kt) | Update `rememberQuoVadisNavigator`, update `QuoVadisNavigation` |

### Testing Updates
| File | Changes |
|------|---------|
| [FakeNavigator.kt](../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt) | Add `config` property |
| Existing tests | Update to use new API pattern |

### Documentation Updates
| Location | Changes |
|----------|---------|
| README.md | Update examples |
| KDoc comments | Update all relevant documentation |
| docs/site | Update website documentation |

## Backward Compatibility

### Preserved Behaviors
- Existing code using `NavigationHost(navigator, config, ...)` continues to work (deprecated but functional)
- `QuoVadisNavigation` API unchanged
- All navigation logic unchanged

### Breaking Changes (None in Phase 1-3)
- Phase 4 (future major version) removes deprecated overload

## Edge Cases

### 1. Navigator without Config
For testing or special use cases, `NavigationConfig.Empty` provides safe defaults:

```kotlin
// Testing scenario
val testNavigator = TreeNavigator(
    config = NavigationConfig.Empty,
    initialState = testState
)
```

### 2. DI-provided Navigator
When Navigator is provided via DI (Koin, etc.), config must be provided at construction:

```kotlin
// Koin module
single<Navigator> {
    TreeNavigator(
        config = get<NavigationConfig>(),
        initialState = get<NavigationConfig>().buildNavNode(MainTabs::class)
    )
}
```

### 3. FakeNavigator in Tests
Update `FakeNavigator` to accept optional config:

```kotlin
class FakeNavigator(
    override val config: NavigationConfig = NavigationConfig.Empty,
    // ... other params
) : Navigator
```

## Implementation Checklist

- [ ] Add `config` property to `Navigator` interface
- [ ] Update `TreeNavigator` constructor to accept and store `config`
- [ ] Derive internal registries from `config` in `TreeNavigator`
- [ ] Update `rememberQuoVadisNavigator` to pass config to TreeNavigator
- [ ] Add new simplified `NavigationHost` overload
- [ ] Deprecate `NavigationHost(navigator, config, ...)` overload
- [ ] Update `QuoVadisNavigation` implementation
- [ ] Update `FakeNavigator` to support config
- [ ] Update unit tests
- [ ] Update integration tests
- [ ] Update demo app (`DemoApp.kt`)
- [ ] Update README examples
- [ ] Update website documentation

## Alternatives Considered

### Alternative 1: Config Wrapper Class
Create a `ConfiguredNavigator` that bundles navigator + config. Rejected because:
- Adds unnecessary complexity
- Doesn't solve the root issue of redundancy

### Alternative 2: Global/Ambient Config
Use `CompositionLocal` for config. Rejected because:
- Makes dependencies implicit
- Harder to test
- Doesn't work well with DI

### Alternative 3: Keep Current API
Accept the redundancy. Rejected because:
- User feedback indicates confusion
- Violates principle of least surprise
- Easy to misuse (pass different configs)

## Success Criteria

1. ✅ Config passed only once (to Navigator or `rememberQuoVadisNavigator`)
2. ✅ NavigationHost works without explicit config parameter
3. ✅ All existing functionality preserved
4. ✅ Smooth migration path with deprecation warnings
5. ✅ Clear documentation of new pattern
