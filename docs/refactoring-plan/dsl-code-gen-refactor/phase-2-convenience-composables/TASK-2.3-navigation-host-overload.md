````markdown
# Task 2.3: Add NavigationHost Config Overload

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 1 day  
> **Dependencies**: Task 1.1 (NavigationConfig Interface)  
> **Blocks**: Task 2.2 (QuoVadisNavigation uses this overload)

---

## Objective

Add a new overload of `NavigationHost` that accepts a single `NavigationConfig` parameter instead of individual registry parameters. This simplifies the API surface while maintaining full backward compatibility with existing signatures.

**Target Usage Pattern**:
```kotlin
// New simplified API
NavigationHost(
    navigator = navigator,
    config = GeneratedNavigationConfig,
    modifier = Modifier.fillMaxSize()
)

// Instead of current verbose API
NavigationHost(
    navigator = navigator,
    screenRegistry = GeneratedScreenRegistry,
    wrapperRegistry = GeneratedWrapperRegistry,
    transitionRegistry = GeneratedTransitionRegistry,
    scopeRegistry = GeneratedScopeRegistry,
    modifier = Modifier.fillMaxSize()
)
```

---

## Files to Modify

### Existing File to Modify

| File | Path | Change |
|------|------|--------|
| `NavigationHost.kt` | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavigationHost.kt` | Add new overload |

### Files to Reference

| File | Purpose |
|------|---------|
| `NavigationConfig.kt` | Config interface (Task 1.1) |
| Existing `NavigationHost.kt` | Current implementation to delegate to |

---

## New Function Signature

### NavigationHost Config Overload

Add this to the existing `NavigationHost.kt` file:

```kotlin
/**
 * NavigationHost that renders navigation content using a unified NavigationConfig.
 * 
 * This overload accepts a single [NavigationConfig] instead of individual registries,
 * providing a cleaner API surface. It extracts the required registries from the config
 * and delegates to the full [NavigationHost] implementation.
 * 
 * ## Basic Usage
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(MainTabs::class, GeneratedNavigationConfig)
 *     
 *     NavigationHost(
 *         navigator = navigator,
 *         config = GeneratedNavigationConfig
 *     )
 * }
 * ```
 * 
 * ## With All Options
 * ```kotlin
 * NavigationHost(
 *     navigator = navigator,
 *     config = GeneratedNavigationConfig,
 *     modifier = Modifier.fillMaxSize(),
 *     enablePredictiveBack = true,
 *     predictiveBackMode = PredictiveBackMode.FULL_CASCADE,
 *     windowSizeClass = currentWindowSizeClass()
 * )
 * ```
 * 
 * ## Multi-Module Composition
 * ```kotlin
 * val combinedConfig = AppConfig + FeatureAConfig + FeatureBConfig
 * 
 * NavigationHost(
 *     navigator = navigator,
 *     config = combinedConfig
 * )
 * ```
 * 
 * ## Choosing Between Overloads
 * 
 * Use this overload (with [NavigationConfig]) when:
 * - Using generated navigation configuration
 * - Combining multiple module configurations
 * - You want the simplest API surface
 * 
 * Use the individual registry overload when:
 * - You need fine-grained control over which registries to use
 * - Mixing generated and custom registries
 * - Gradual migration from legacy code
 * 
 * @param navigator The Navigator managing navigation state. 
 *                  Typically created with [rememberQuoVadisNavigator].
 * @param config The NavigationConfig providing all required registries:
 *               [screenRegistry][NavigationConfig.screenRegistry],
 *               [wrapperRegistry][NavigationConfig.wrapperRegistry],
 *               [transitionRegistry][NavigationConfig.transitionRegistry],
 *               [scopeRegistry][NavigationConfig.scopeRegistry].
 * @param modifier Modifier to apply to the host container.
 * @param enablePredictiveBack Whether to enable predictive back gesture support.
 *                             When enabled, back gestures provide visual feedback
 *                             before completing the navigation.
 * @param predictiveBackMode The mode for predictive back behavior:
 *                           - [PredictiveBackMode.ROOT_ONLY]: Only animate the root container
 *                           - [PredictiveBackMode.FULL_CASCADE]: Animate all affected containers
 * @param windowSizeClass Optional window size class for responsive layouts.
 *                        When provided, navigation containers can adapt their
 *                        presentation based on available space.
 * 
 * @see NavigationConfig for configuration details
 * @see rememberQuoVadisNavigator for creating a Navigator
 * @see QuoVadisNavigation for one-liner setup
 */
@Composable
fun NavigationHost(
    navigator: Navigator,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = false,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    windowSizeClass: WindowSizeClass? = null
) {
    // Delegate to the existing NavigationHost with individual registries
    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        screenRegistry = config.screenRegistry,
        wrapperRegistry = config.wrapperRegistry,
        transitionRegistry = config.transitionRegistry,
        scopeRegistry = config.scopeRegistry,
        enablePredictiveBack = enablePredictiveBack,
        predictiveBackMode = predictiveBackMode,
        windowSizeClass = windowSizeClass
    )
}
```

---

## How It Delegates to Existing NavigationHost

### Current NavigationHost Signature (Reference)

The existing `NavigationHost` likely has a signature similar to:

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry,
    wrapperRegistry: WrapperRegistry,
    transitionRegistry: TransitionRegistry,
    scopeRegistry: ScopeRegistry,
    enablePredictiveBack: Boolean = false,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    windowSizeClass: WindowSizeClass? = null
)
```

### Delegation Pattern

```kotlin
// New overload
NavigationHost(navigator, config, modifier, ...) {
    // Extracts registries from config
    // Calls existing implementation
    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        screenRegistry = config.screenRegistry,     // Extract
        wrapperRegistry = config.wrapperRegistry,   // Extract
        transitionRegistry = config.transitionRegistry,  // Extract
        scopeRegistry = config.scopeRegistry,       // Extract
        enablePredictiveBack = enablePredictiveBack,
        predictiveBackMode = predictiveBackMode,
        windowSizeClass = windowSizeClass
    )
}
```

### Registry Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    NavigationConfig                          │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────┐  │
│  │screenRegistry│ │wrapperRegistry│ │transitionRegistry│  │  │
│  └──────┬───────┘ └──────┬───────┘ └──────────┬─────────┘  │
│         │                │                     │            │
│  ┌──────┴───────┐ ┌──────┴───────┐ ┌──────────┴─────────┐  │
│  │scopeRegistry │ │containerReg  │ │deepLinkHandler     │  │
│  └──────┬───────┘ └──────────────┘ └────────────────────┘  │
│         │              (used by Navigator, not Host)       │
└─────────┼──────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────┐
│          NavigationHost (config overload)                   │
│                         │                                   │
│                         ▼                                   │
│   Extracts: screenRegistry, wrapperRegistry,                │
│             transitionRegistry, scopeRegistry               │
│                         │                                   │
│                         ▼                                   │
│   ┌─────────────────────────────────────────────────────┐   │
│   │       NavigationHost (registry overload)            │   │
│   │                                                     │   │
│   │   Uses registries to render navigation tree         │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Backward Compatibility Requirements

### No Breaking Changes

| Aspect | Requirement | Status |
|--------|-------------|--------|
| Existing signature | Unchanged | ✅ Required |
| Existing behavior | Identical | ✅ Required |
| Binary compatibility | Maintained | ✅ Required |
| Source compatibility | Maintained | ✅ Required |

### Both Overloads Coexist

```kotlin
// Old way - still works!
NavigationHost(
    navigator = navigator,
    screenRegistry = myScreenRegistry,
    wrapperRegistry = myWrapperRegistry,
    transitionRegistry = myTransitionRegistry,
    scopeRegistry = myScopeRegistry
)

// New way - also works!
NavigationHost(
    navigator = navigator,
    config = myNavigationConfig
)
```

### Mixed Usage (Valid)

```kotlin
// Custom screen registry with generated config for others
NavigationHost(
    navigator = navigator,
    screenRegistry = CustomScreenRegistry,           // Custom
    wrapperRegistry = config.wrapperRegistry,        // From config
    transitionRegistry = config.transitionRegistry,  // From config
    scopeRegistry = config.scopeRegistry             // From config
)
```

---

## KDoc Documentation Requirements

### Required Sections

1. **Function Summary**: One-line description
2. **Detailed Description**: 
   - What this overload provides
   - How it differs from the existing overload
3. **Usage Examples**:
   - Basic usage
   - With all options
   - Multi-module composition
4. **Decision Guide**:
   - When to use this overload
   - When to use individual registries
5. **Parameter Documentation**:
   - Each parameter fully documented
   - Default values noted
6. **See Also Links**:
   - `NavigationConfig`
   - `rememberQuoVadisNavigator`
   - `QuoVadisNavigation`

### KDoc Template Compliance

The implementation above follows the existing project's KDoc style:
- Uses Markdown code blocks with language specifier
- Groups related examples
- Includes `@param`, `@see` tags
- Provides decision guidance for API choices

---

## Dependencies on Other Tasks

### Task 1.1 (NavigationConfig Interface)

```kotlin
// Requires NavigationConfig with these properties
interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val wrapperRegistry: WrapperRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    // ...
}
```

### Used By Task 2.2 (QuoVadisNavigation)

```kotlin
// Task 2.2 uses this overload
@Composable
fun QuoVadisNavigation(..., config: NavigationConfig) {
    val navigator = rememberQuoVadisNavigator(...)
    
    NavigationHost(
        navigator = navigator,
        config = config,  // Uses this new overload
        ...
    )
}
```

---

## Acceptance Criteria Checklist

### Core Functionality
- [ ] New overload compiles without errors
- [ ] Delegates to existing NavigationHost correctly
- [ ] All registries extracted from config correctly
- [ ] All parameters forwarded correctly
- [ ] Navigation rendering works identically to registry overload

### Backward Compatibility
- [ ] Existing NavigationHost signature unchanged
- [ ] Existing code compiles without modification
- [ ] No runtime behavior changes for existing usage
- [ ] Binary compatibility maintained

### API Design
- [ ] Config parameter is first after navigator (follows convention)
- [ ] Default values match existing overload
- [ ] Parameter names are clear and consistent
- [ ] Overload resolution works correctly (no ambiguity)

### Documentation
- [ ] KDoc with function summary
- [ ] KDoc with detailed description
- [ ] At least 3 usage examples in KDoc
- [ ] All parameters documented with `@param`
- [ ] Default values documented
- [ ] `@see` links to related APIs
- [ ] Decision guide for choosing overload

### Testing
- [ ] Unit test: Registries extracted correctly
- [ ] Unit test: Parameters forwarded correctly
- [ ] Integration test: Full navigation flow works
- [ ] Test: Works with composed configs (a + b)

### Code Quality
- [ ] No compiler warnings
- [ ] Follows existing code style
- [ ] Imports organized properly
- [ ] No code duplication

---

## Implementation Notes

### Design Decisions

1. **Simple Delegation**
   - No additional logic, just extraction and forwarding
   - Ensures identical behavior to existing overload

2. **Parameter Order**
   - `navigator` first (required, most important)
   - `config` second (main new parameter)
   - `modifier` third (standard Compose convention)
   - Optional parameters follow

3. **Default Values**
   - Match existing overload exactly
   - Ensures consistent behavior

### Potential Issues

1. **Overload Resolution**
   - Kotlin should resolve correctly based on parameter types
   - `NavigationConfig` vs individual registries are distinct types
   - Test that IDE autocomplete shows both options

2. **Parameter Name Consistency**
   - Ensure `enablePredictiveBack`, `predictiveBackMode`, etc. match existing overload
   - Check actual parameter names in existing implementation

3. **WindowSizeClass Import**
   - May be from Material3 or custom type
   - Match existing import

### Pre-Implementation Checklist

- [ ] Verify existing NavigationHost signature
- [ ] Verify exact parameter names and types
- [ ] Verify WindowSizeClass source and import
- [ ] Verify PredictiveBackMode enum values

---

## Test Scenarios

### Test: Registry Extraction

```kotlin
@Test
fun `config overload extracts registries correctly`() {
    val config = createTestConfig()
    var capturedScreenRegistry: ScreenRegistry? = null
    var capturedWrapperRegistry: WrapperRegistry? = null
    
    composeRule.setContent {
        // Use test composition that captures arguments
        TestNavigationHost(
            navigator = testNavigator,
            config = config,
            onRegistriesCaptured = { screen, wrapper, _, _ ->
                capturedScreenRegistry = screen
                capturedWrapperRegistry = wrapper
            }
        )
    }
    
    assertSame(config.screenRegistry, capturedScreenRegistry)
    assertSame(config.wrapperRegistry, capturedWrapperRegistry)
}
```

### Test: Parameter Forwarding

```kotlin
@Test
fun `all parameters forwarded correctly`() {
    val testModifier = Modifier.size(100.dp)
    val testWindowSize = WindowSizeClass.Compact
    
    composeRule.setContent {
        NavigationHost(
            navigator = testNavigator,
            config = testConfig,
            modifier = testModifier,
            enablePredictiveBack = true,
            predictiveBackMode = PredictiveBackMode.FULL_CASCADE,
            windowSizeClass = testWindowSize
        )
    }
    
    // Verify parameters reached underlying implementation
    // (implementation depends on testing approach)
}
```

### Test: Composed Config

```kotlin
@Test
fun `works with composed configs`() {
    val combinedConfig = ConfigA + ConfigB
    
    composeRule.setContent {
        NavigationHost(
            navigator = testNavigator,
            config = combinedConfig
        )
    }
    
    // Verify screens from both configs are accessible
    composeRule.onNodeWithTag("screenFromA").assertExists()
    composeRule.onNodeWithTag("screenFromB").assertExists()
}
```

---

## Related Files

- [Phase 2 Summary](./SUMMARY.md)
- [Task 2.1 - rememberQuoVadisNavigator](./TASK-2.1-remember-navigator.md)
- [Task 2.2 - QuoVadisNavigation One-Liner](./TASK-2.2-one-liner-composable.md)
- [Task 1.1 - NavigationConfig Interface](../phase-1-core-dsl-infrastructure/TASK-1.1-navigation-config-interface.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)

````
