# Task 3.3: Refactor Existing Generators

> **Task Status**: ✅ Completed  
> **Completed**: December 16, 2024  
> **Estimated Effort**: 3-4 days  
> **Dependencies**: Task 3.1, Task 3.2  
> **Blocks**: Task 3.4

---

## Objective

Convert existing generators to produce DSL blocks that integrate with the new `NavigationConfigGenerator`, and establish a deprecation strategy for generators that become obsolete.

**Key Outcomes**:
1. Existing generators produce DSL-compatible output (or delegate to new sub-generators)
2. `NavNodeBuilderGenerator` is deprecated with backward compatibility
3. Clear migration path for any code depending on old generated structures
4. Legacy mode available for gradual adoption

---

## Generator Transformation Table

| Generator | Current Output | Action | New Behavior |
|-----------|---------------|--------|--------------|
| `ScreenRegistryGenerator` | `GeneratedScreenRegistry` object | **Convert** | Provides data to `ScreenBlockGenerator` |
| `ContainerRegistryGenerator` | `GeneratedContainerRegistry` object | **Convert** | Provides data to `ContainerBlockGenerator` |
| `ScopeRegistryGenerator` | `GeneratedScopeRegistry` object | **Convert** | Provides data to `ScopeBlockGenerator` |
| `TransitionRegistryGenerator` | `GeneratedTransitionRegistry` object | **Convert** | Provides data to `TransitionBlockGenerator` |
| `WrapperRegistryGenerator` | `GeneratedWrapperRegistry` object | **Convert** | Provides data to `WrapperBlockGenerator` |
| `NavNodeBuilderGenerator` | `build*NavNode()` functions | **Deprecate** | Keep for compatibility, generate deprecation warnings |
| `DeepLinkHandlerGenerator` | `GeneratedDeepLinkHandler` object | **Integrate** | Part of unified config, may keep separate handler |
| `NavigatorExtGenerator` | Extension functions | **Keep** | Standalone extensions remain useful |

---

## Detailed Transformation Specs

### 1. ScreenRegistryGenerator Transformation

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScreenRegistryGenerator.kt`

**Current Behavior**:
- Generates `GeneratedScreenRegistry` object
- Implements `ScreenRegistry` interface
- Contains `Content()` function with `when` expression

**New Behavior**:
- Option A: Provide `ScreenInfo` list directly to `ScreenBlockGenerator`
- Option B: Generate both old format (deprecated) and new format during transition

**Transformation**:

```kotlin
// BEFORE: ScreenRegistryGenerator generates complete object
object GeneratedScreenRegistry : ScreenRegistry {
    @Composable
    override fun Content(destination: Destination, navigator: Navigator, scope: NavRenderScope) {
        when (destination) {
            is HomeDestination.Feed -> FeedScreen(navigator = navigator)
            is HomeDestination.Detail -> DetailScreen(destination = destination, navigator = navigator)
            else -> error("No screen for $destination")
        }
    }
}

// AFTER: Data flows to ScreenBlockGenerator which produces:
screen<HomeDestination.Feed> { dest ->
    FeedScreen(navigator = navigator)
}

screen<HomeDestination.Detail> { dest ->
    DetailScreen(destination = dest, navigator = navigator)
}
```

**Changes Required**:
1. Add `generateDslBlocks(screens: List<ScreenInfo>): CodeBlock` method
2. Keep `generate()` method for legacy mode (add `@Deprecated`)
3. Mark generated object with `@Deprecated` annotation pointing to new API

### 2. ContainerRegistryGenerator Transformation

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ContainerRegistryGenerator.kt`

**Current Behavior**:
- Generates `GeneratedContainerRegistry` object
- Maps destinations to `ContainerInfo` instances
- Contains references to `build*NavNode()` functions

**New Behavior**:
- Container info derived from DSL container definitions
- `buildNavNode()` delegated to `NavigationConfig`

**Transformation**:

```kotlin
// BEFORE: ContainerRegistryGenerator generates
object GeneratedContainerRegistry : ContainerRegistry {
    override fun getContainerInfo(destination: Destination): ContainerInfo? {
        return when (destination) {
            is MainTabs.HomeTab -> ContainerInfo.TabContainer(::buildMainTabsNavNode, 0, "MainTabs")
            is MainTabs.ExploreTab -> ContainerInfo.TabContainer(::buildMainTabsNavNode, 1, "MainTabs")
            else -> null
        }
    }
}

// AFTER: Info embedded in DSL container blocks
tabs<MainTabs>(scopeKey = "MainTabs") {
    initialTab = 0
    tab(MainTabs.HomeTab, title = "Home")
    tab(MainTabs.ExploreTab, title = "Explore")
}
```

**Changes Required**:
1. Add method to extract container data for `ContainerBlockGenerator`
2. Deprecate direct generation of registry object
3. Container info now computed from DSL config at runtime

### 3. ScopeRegistryGenerator Transformation

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScopeRegistryGenerator.kt`

**Current Behavior**:
- Generates `GeneratedScopeRegistry` object
- Contains `scopeMap` with scope key to destinations mapping
- Methods: `isInScope()`, `getScopeKey()`

**New Behavior**:
- Scopes defined via `scope()` DSL calls
- Scope membership computed from container definitions

**Transformation**:

```kotlin
// BEFORE: ScopeRegistryGenerator generates
object GeneratedScopeRegistry : ScopeRegistry {
    private val scopeMap = mapOf(
        "MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.ExploreTab::class),
        "ProfileStack" to setOf(ProfileDestination.Main::class, ProfileDestination.Settings::class)
    )
    
    override fun isInScope(scopeKey: String, destination: Destination): Boolean {
        return scopeMap[scopeKey]?.contains(destination::class) == true
    }
}

// AFTER: Defined in DSL
scope("MainTabs",
    MainTabs.HomeTab::class,
    MainTabs.ExploreTab::class
)

scope("ProfileStack",
    ProfileDestination.Main::class,
    ProfileDestination.Settings::class
)
```

**Changes Required**:
1. Collect scope data from containers (tabs have scopeKey, stacks have scopeKey, etc.)
2. Pass collected data to `ScopeBlockGenerator`
3. Deprecate standalone registry generation

### 4. TransitionRegistryGenerator Transformation

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/TransitionRegistryGenerator.kt`

**Current Behavior**:
- Generates `GeneratedTransitionRegistry` object
- Maps destination classes to `NavTransition` instances

**New Behavior**:
- Transitions defined via `transition<T>()` DSL calls

**Transformation**:

```kotlin
// BEFORE: TransitionRegistryGenerator generates
object GeneratedTransitionRegistry : TransitionRegistry {
    override fun getTransition(destination: Destination): NavTransition? {
        return when (destination) {
            is HomeDestination.Detail -> NavTransitions.SharedElement
            is ProfileDestination -> NavTransitions.Slide
            else -> null
        }
    }
}

// AFTER: Defined in DSL
transition<HomeDestination.Detail>(NavTransitions.SharedElement)
transition<ProfileDestination>(NavTransitions.Slide)
```

**Changes Required**:
1. Extract `TransitionInfo` and pass to `TransitionBlockGenerator`
2. Deprecate standalone registry generation

### 5. WrapperRegistryGenerator Transformation

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/WrapperRegistryGenerator.kt`

**Current Behavior**:
- Generates `GeneratedWrapperRegistry` object
- Contains `TabWrapper()` and `PaneWrapper()` composable functions
- Maps wrapper keys to composable implementations

**New Behavior**:
- Wrappers defined via `tabWrapper()` and `paneWrapper()` DSL blocks

**Transformation**:

```kotlin
// BEFORE: WrapperRegistryGenerator generates
object GeneratedWrapperRegistry : WrapperRegistry {
    @Composable
    override fun TabWrapper(wrapperKey: String, scope: TabWrapperScope) {
        when (wrapperKey) {
            "mainTabsWrapper" -> MainTabsWrapper(scope)
            else -> scope.content()
        }
    }
}

// AFTER: Defined in DSL
tabWrapper("mainTabsWrapper") {
    MainTabsWrapper(this)
}
```

**Changes Required**:
1. Extract `WrapperInfo` and pass to `WrapperBlockGenerator`
2. Deprecate standalone registry generation
3. Ensure wrapper scope is properly passed in DSL context

### 6. NavNodeBuilderGenerator Deprecation Strategy

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt`

**Current Behavior**:
- Generates `build*NavNode()` functions for each container
- Functions create `TabNode`, `StackNode`, `PaneNode` instances
- Used to construct initial navigation state

**Deprecation Strategy**:

1. **Keep generating** for backward compatibility
2. **Add `@Deprecated` annotation** with migration message
3. **Suggest `GeneratedNavigationConfig.buildNavNode()`** as replacement

```kotlin
// Generated with deprecation warning
@Deprecated(
    message = "Use GeneratedNavigationConfig.buildNavNode(MainTabs::class) instead",
    replaceWith = ReplaceWith(
        "GeneratedNavigationConfig.buildNavNode(MainTabs::class)",
        "com.example.navigation.generated.GeneratedNavigationConfig"
    ),
    level = DeprecationLevel.WARNING
)
fun buildMainTabsNavNode(
    key: String = "MainTabs",
    parentKey: String? = null,
    initialTabIndex: Int = 0
): TabNode {
    // ... existing implementation
}
```

**Changes Required**:
1. Add deprecation annotation to generated functions
2. Include clear migration path in deprecation message
3. Ensure `NavigationConfig.buildNavNode()` produces equivalent output
4. Add KSP option to disable legacy generation

### 7. DeepLinkHandlerGenerator Integration

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/DeepLinkHandlerGenerator.kt`

**Current Behavior**:
- Generates `GeneratedDeepLinkHandlerImpl` object
- Contains route patterns and handling logic
- Complex implementation with regex matching

**New Behavior**:
- Keep separate generation for now (complex implementation)
- Reference in `GeneratedNavigationConfig.deepLinkHandler`
- Future: May integrate into DSL

```kotlin
// In GeneratedNavigationConfig
override val deepLinkHandler: DeepLinkHandler = GeneratedDeepLinkHandlerImpl
```

**Changes Required**:
1. Ensure generated handler is referenced by `NavigationConfig`
2. Keep existing generation logic
3. Future task: Consider DSL-based deep link configuration

### 8. NavigatorExtGenerator

**Current File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavigatorExtGenerator.kt`

**Action**: **Keep unchanged**

Extension functions for Navigator remain useful and orthogonal to the config system:

```kotlin
// These extensions are still valuable
fun Navigator.navigateToHome() { ... }
fun Navigator.navigateToProfile(userId: String) { ... }
```

---

## File Changes Summary

### Files to Modify

| File | Changes |
|------|---------|
| `ScreenRegistryGenerator.kt` | Add DSL extraction method, deprecate main `generate()` |
| `ContainerRegistryGenerator.kt` | Add DSL extraction method, deprecate main `generate()` |
| `ScopeRegistryGenerator.kt` | Add DSL extraction method, deprecate main `generate()` |
| `TransitionRegistryGenerator.kt` | Add DSL extraction method, deprecate main `generate()` |
| `WrapperRegistryGenerator.kt` | Add DSL extraction method, deprecate main `generate()` |
| `NavNodeBuilderGenerator.kt` | Add deprecation annotations to generated code |
| `DeepLinkHandlerGenerator.kt` | Minor changes for integration |

### New Directory Structure

```
quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/
├── base/                           # From Task 3.1
│   ├── DslCodeGenerator.kt
│   ├── CodeBlockBuilders.kt
│   └── StringTemplates.kt
├── dsl/                            # From Task 3.2
│   ├── NavigationConfigGenerator.kt
│   ├── ScreenBlockGenerator.kt
│   └── ... other block generators
├── legacy/                         # NEW: Move deprecated generators
│   └── NavNodeBuilderGenerator.kt
├── ScreenRegistryGenerator.kt      # Modified with deprecations
├── ContainerRegistryGenerator.kt   # Modified with deprecations
├── ScopeRegistryGenerator.kt       # Modified with deprecations
├── TransitionRegistryGenerator.kt  # Modified with deprecations
├── WrapperRegistryGenerator.kt     # Modified with deprecations
├── DeepLinkHandlerGenerator.kt     # Minor modifications
└── NavigatorExtGenerator.kt        # Unchanged
```

---

## Legacy Mode Support

For gradual migration, add a KSP option to control generation mode:

```kotlin
// In QuoVadisSymbolProcessor
class QuoVadisSymbolProcessor(...) {
    
    private val useLegacyGeneration: Boolean = 
        options["quoVadis.legacyGeneration"]?.toBoolean() ?: false
    
    private val generateBothModes: Boolean =
        options["quoVadis.generateBoth"]?.toBoolean() ?: true
    
    override fun process(resolver: Resolver): List<KSAnnotated> {
        when {
            useLegacyGeneration -> generateLegacy(resolver)
            generateBothModes -> {
                generateLegacy(resolver)  // With deprecation warnings
                generateDsl(resolver)     // New unified config
            }
            else -> generateDsl(resolver) // Only new DSL config
        }
    }
}
```

**KSP Options**:
```kotlin
// In build.gradle.kts
ksp {
    arg("quoVadis.legacyGeneration", "false")  // Default: false
    arg("quoVadis.generateBoth", "true")       // Default: true (transition period)
}
```

---

## Deprecation Message Templates

### For Registry Objects

```kotlin
@Deprecated(
    message = """
        This registry is deprecated. Use GeneratedNavigationConfig instead.
        
        Migration:
        // Before
        NavigationHost(
            navigator = navigator,
            screenRegistry = GeneratedScreenRegistry,
            // ... other registries
        )
        
        // After
        NavigationHost(
            navigator = navigator,
            config = GeneratedNavigationConfig
        )
    """,
    replaceWith = ReplaceWith(
        "GeneratedNavigationConfig",
        "com.example.navigation.generated.GeneratedNavigationConfig"
    ),
    level = DeprecationLevel.WARNING
)
object GeneratedScreenRegistry : ScreenRegistry { ... }
```

### For NavNode Builder Functions

```kotlin
@Deprecated(
    message = """
        This function is deprecated. Use GeneratedNavigationConfig.buildNavNode() instead.
        
        Migration:
        // Before
        val navigator = TreeNavigator(
            initialState = buildMainTabsNavNode(),
            // ...
        )
        
        // After
        val navigator = rememberQuoVadisNavigator(MainTabs::class)
        // Or
        val initialState = GeneratedNavigationConfig.buildNavNode(MainTabs::class)
    """,
    replaceWith = ReplaceWith(
        "GeneratedNavigationConfig.buildNavNode(MainTabs::class)",
        "com.example.navigation.generated.GeneratedNavigationConfig"
    ),
    level = DeprecationLevel.WARNING
)
fun buildMainTabsNavNode(...): TabNode { ... }
```

---

## Dependencies

### This Task Depends On

| Dependency | Description | Status |
|------------|-------------|--------|
| Task 3.1 | Base classes for refactored generators | Required |
| Task 3.2 | New DSL generators to delegate to | Required |

### What This Task Blocks

| Task | Dependency Type |
|------|-----------------|
| Task 3.4 (Processor Orchestration) | Integration with all generators |

---

## Acceptance Criteria Checklist

### ScreenRegistryGenerator
- [ ] `generateDslData()` method extracts screen data for DSL generation
- [ ] Original `generate()` method marked deprecated
- [ ] Generated object includes `@Deprecated` annotation
- [ ] Both modes work (legacy and DSL)

### ContainerRegistryGenerator
- [ ] Container data extraction for DSL generation
- [ ] Original `generate()` method marked deprecated
- [ ] Generated object includes `@Deprecated` annotation

### ScopeRegistryGenerator
- [ ] Scope data extraction for DSL generation
- [ ] Original `generate()` method marked deprecated
- [ ] Generated object includes `@Deprecated` annotation

### TransitionRegistryGenerator
- [ ] Transition data extraction for DSL generation
- [ ] Original `generate()` method marked deprecated
- [ ] Generated object includes `@Deprecated` annotation

### WrapperRegistryGenerator
- [ ] Wrapper data extraction for DSL generation
- [ ] Original `generate()` method marked deprecated
- [ ] Generated object includes `@Deprecated` annotation

### NavNodeBuilderGenerator
- [ ] Generated functions include `@Deprecated` annotation
- [ ] Deprecation message includes migration path
- [ ] `replaceWith` points to new API
- [ ] Functions still work correctly (backward compatible)

### DeepLinkHandlerGenerator
- [ ] Generated handler referenced in NavigationConfig
- [ ] Existing functionality preserved

### Legacy Mode
- [ ] KSP option `quoVadis.legacyGeneration` works
- [ ] KSP option `quoVadis.generateBoth` works
- [ ] Default behavior generates both with deprecation warnings

### Testing
- [ ] Existing tests still pass with legacy mode
- [ ] New DSL generation produces equivalent functionality
- [ ] Deprecation warnings appear in IDE
- [ ] Migration following deprecation instructions works

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| ScreenRegistryGenerator refactor | 0.5 days |
| ContainerRegistryGenerator refactor | 0.5 days |
| ScopeRegistryGenerator refactor | 0.25 days |
| TransitionRegistryGenerator refactor | 0.25 days |
| WrapperRegistryGenerator refactor | 0.5 days |
| NavNodeBuilderGenerator deprecation | 0.5 days |
| DeepLinkHandlerGenerator integration | 0.25 days |
| Legacy mode support | 0.5 days |
| Testing & verification | 0.75 days |
| **Total** | **3-4 days** |

---

## Migration Guide Preview

This task produces the foundation for the Phase 4 migration guide. Key migration patterns:

### Pattern 1: NavigationHost Parameters

```kotlin
// BEFORE (multiple registries)
NavigationHost(
    navigator = navigator,
    screenRegistry = GeneratedScreenRegistry,
    wrapperRegistry = GeneratedWrapperRegistry,
    scopeRegistry = GeneratedScopeRegistry,
    transitionRegistry = GeneratedTransitionRegistry
)

// AFTER (single config)
NavigationHost(
    navigator = navigator,
    config = GeneratedNavigationConfig
)
```

### Pattern 2: Navigator Creation

```kotlin
// BEFORE
val navigator = TreeNavigator(
    initialState = buildMainTabsNavNode(),
    scopeRegistry = GeneratedScopeRegistry,
    containerRegistry = GeneratedContainerRegistry
)

// AFTER
val navigator = rememberQuoVadisNavigator(MainTabs::class)
```

### Pattern 3: Accessing Individual Registries

```kotlin
// BEFORE
val scopeRegistry = GeneratedScopeRegistry

// AFTER (if still needed)
val scopeRegistry = GeneratedNavigationConfig.scopeRegistry
```

---

## Related Files

- [Phase 3 Summary](./SUMMARY.md)
- [Task 3.1 - Generator Base Classes](./TASK-3.1-generator-base-classes.md)
- [Task 3.2 - NavigationConfigGenerator](./TASK-3.2-navigation-config-generator.md)
- [Task 3.4 - Processor Orchestration](./TASK-3.4-processor-orchestration.md)

### Current Generator Files
- [ScreenRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScreenRegistryGenerator.kt)
- [ContainerRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ContainerRegistryGenerator.kt)
- [ScopeRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/ScopeRegistryGenerator.kt)
- [TransitionRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/TransitionRegistryGenerator.kt)
- [WrapperRegistryGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/WrapperRegistryGenerator.kt)
- [NavNodeBuilderGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/NavNodeBuilderGenerator.kt)
- [DeepLinkHandlerGenerator.kt](quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/DeepLinkHandlerGenerator.kt)
