# Task 4.2: Update Demo Application

> **Task Status**: ✅ Completed  
> **Completed**: December 2024  
> **Estimated Effort**: 2 days  
> **Dependencies**: Phase 1, Phase 2, Phase 3 complete  
> **Blocks**: None (parallel with 4.1, 4.3)

---

## Objective

Update the demo application to showcase the new DSL-based navigation patterns. The demo should serve as a **reference implementation** demonstrating:

1. **One-liner integration** - Simplest possible setup
2. **Standard usage** - Most common configuration pattern  
3. **Advanced configuration** - Full control scenarios

The demo should also include clear before/after comparisons to help users understand the migration path.

---

## Location

**Demo Application Path**: `composeApp/src/commonMain/kotlin/`

### Key Files to Update

| File | Purpose | Changes |
|------|---------|---------|
| `DemoApp.kt` | Main entry point | Migrate to new patterns |
| Navigation setup files | Navigator creation | Use `rememberQuoVadisNavigator` |
| Multi-module examples | Composition demo | Show `config + config` pattern |

---

## Usage Patterns to Demonstrate

### Pattern 1: One-Liner (Simple Use Case)

**When to Use**: Quick prototypes, simple apps, getting started

**Target Code**:
```kotlin
@Composable
fun App() {
    QuoVadisNavigation(MainTabs::class)
}
```

**Full Example with Theme**:
```kotlin
@Composable
fun App() {
    MaterialTheme {
        QuoVadisNavigation(
            rootDestination = MainTabs::class,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**Demonstrates**:
- Minimal boilerplate
- Sensible defaults
- Quick setup for new projects

### Pattern 2: Standard Usage (Most Common)

**When to Use**: Production apps with standard requirements

**Target Code**:
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    MaterialTheme {
        NavigationHost(
            navigator = navigator,
            config = GeneratedNavigationConfig,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**With Navigator Access**:
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    MaterialTheme {
        Column {
            // Custom header with navigator access
            AppHeader(
                onMenuClick = { navigator.navigate(MenuDestination) }
            )
            
            NavigationHost(
                navigator = navigator,
                config = GeneratedNavigationConfig,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

**Demonstrates**:
- Explicit navigator access
- Custom UI around navigation
- Config-based setup

### Pattern 3: Advanced Configuration (Full Control)

**When to Use**: Multi-module apps, custom deep link handlers, feature flags

**Target Code**:
```kotlin
@Composable
fun App() {
    // Combine configs from multiple modules
    val combinedConfig = remember {
        GeneratedNavigationConfig + 
        FeatureANavigationConfig + 
        FeatureBNavigationConfig
    }
    
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = combinedConfig
    )
    
    MaterialTheme {
        NavigationHost(
            navigator = navigator,
            config = combinedConfig,
            enablePredictiveBack = true,
            predictiveBackMode = PredictiveBackMode.FULL_CASCADE,
            windowSizeClass = currentWindowSizeClass()
        )
    }
}
```

**With Custom Deep Link Handler**:
```kotlin
@Composable
fun App() {
    val customDeepLinkHandler = remember {
        object : DeepLinkHandler {
            override fun handle(deepLink: DeepLink, navigator: Navigator): Boolean {
                // Custom handling logic
                return GeneratedNavigationConfig.deepLinkHandler.handle(deepLink, navigator)
            }
            
            override fun canHandle(deepLink: DeepLink): Boolean {
                return GeneratedNavigationConfig.deepLinkHandler.canHandle(deepLink)
            }
        }
    }
    
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig,
        deepLinkHandler = customDeepLinkHandler
    )
    
    // ... rest of setup
}
```

**Demonstrates**:
- Multi-module composition
- Custom handlers
- Full platform configuration
- Predictive back customization

---

## Before/After Examples

### Example 1: Basic App Setup

**BEFORE (Old Pattern)**:
```kotlin
@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    
    val navigator = remember {
        TreeNavigator(
            initialState = buildMainTabsNavNode(),
            scopeRegistry = GeneratedScopeRegistry,
            containerRegistry = GeneratedContainerRegistry,
            deepLinkHandler = GeneratedDeepLinkHandlerImpl,
            coroutineScope = coroutineScope
        )
    }
    
    MaterialTheme {
        NavigationHost(
            navigator = navigator,
            screenRegistry = GeneratedScreenRegistry,
            containerRegistry = GeneratedContainerRegistry, // includes wrapper functionality
            scopeRegistry = GeneratedScopeRegistry,
            transitionRegistry = GeneratedTransitionRegistry,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**AFTER (New Pattern)**:
```kotlin
@Composable
fun App() {
    MaterialTheme {
        QuoVadisNavigation(
            rootDestination = MainTabs::class,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

**Improvement**: 25+ lines → 7 lines

---

### Example 2: Navigator with Custom Usage

**BEFORE (Old Pattern)**:
```kotlin
@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    
    val navigator = remember {
        TreeNavigator(
            initialState = buildMainTabsNavNode(),
            scopeRegistry = GeneratedScopeRegistry,
            containerRegistry = GeneratedContainerRegistry,
            deepLinkHandler = GeneratedDeepLinkHandlerImpl,
            coroutineScope = coroutineScope
        )
    }
    
    MaterialTheme {
        Column {
            TopBar(onBackClick = { navigator.navigateBack() })
            
            NavigationHost(
                navigator = navigator,
                screenRegistry = GeneratedScreenRegistry,
                containerRegistry = GeneratedContainerRegistry, // includes wrapper functionality
                scopeRegistry = GeneratedScopeRegistry,
                transitionRegistry = GeneratedTransitionRegistry,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

**AFTER (New Pattern)**:
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    MaterialTheme {
        Column {
            TopBar(onBackClick = { navigator.navigateBack() })
            
            NavigationHost(
                navigator = navigator,
                config = GeneratedNavigationConfig,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

**Improvement**: 
- 6 registry parameters → 1 config parameter
- Manual TreeNavigator → `rememberQuoVadisNavigator()`
- `buildMainTabsNavNode()` → class reference

---

### Example 3: Multi-Module Composition

**BEFORE (Old Pattern)**:
```kotlin
@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    
    // Manually combine registries from multiple modules
    val combinedScreenRegistry = object : ScreenRegistry {
        @Composable
        override fun Content(destination: Destination, navigator: Navigator, scope: NavRenderScope) {
            when {
                AppScreenRegistry.hasScreen(destination) -> 
                    AppScreenRegistry.Content(destination, navigator, scope)
                FeatureAScreenRegistry.hasScreen(destination) -> 
                    FeatureAScreenRegistry.Content(destination, navigator, scope)
                FeatureBScreenRegistry.hasScreen(destination) -> 
                    FeatureBScreenRegistry.Content(destination, navigator, scope)
                else -> error("No screen for $destination")
            }
        }
    }
    
    // ... similar for other registries
    
    val navigator = remember {
        TreeNavigator(
            initialState = buildMainTabsNavNode(),
            scopeRegistry = combinedScopeRegistry,  // manually combined
            containerRegistry = combinedContainerRegistry,  // manually combined
            deepLinkHandler = combinedDeepLinkHandler,  // manually combined
            coroutineScope = coroutineScope
        )
    }
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = combinedScreenRegistry,
        // ... more combined registries
    )
}
```

**AFTER (New Pattern)**:
```kotlin
@Composable
fun App() {
    // Simple composition via + operator
    val combinedConfig = GeneratedNavigationConfig + 
                         FeatureANavigationConfig + 
                         FeatureBNavigationConfig
    
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = combinedConfig
    )
    
    NavigationHost(
        navigator = navigator,
        config = combinedConfig
    )
}
```

**Improvement**: 50+ lines of manual combination → 3 lines with `+` operator

---

## Demo App Structure

### Recommended File Organization

```
composeApp/src/commonMain/kotlin/
├── App.kt                          # Main entry - showcase patterns
├── navigation/
│   ├── AppNavigation.kt           # Navigation setup examples
│   ├── MultiModuleExample.kt      # Composition demonstration
│   └── AdvancedExample.kt         # Full control examples
├── screens/
│   └── ... (existing screens)
└── destinations/
    └── ... (existing destinations)
```

### Code Comments

Add clear comments explaining each pattern:

```kotlin
/**
 * Pattern 1: One-Liner Setup
 * 
 * Use this pattern for:
 * - Quick prototypes
 * - Simple applications
 * - Getting started with Quo Vadis
 * 
 * This single line handles:
 * - Navigator creation
 * - NavigationHost setup
 * - All registry wiring
 */
@Composable
fun OneLinerExample() {
    QuoVadisNavigation(MainTabs::class)
}

/**
 * Pattern 2: Standard Setup
 * 
 * Use this pattern when you need:
 * - Direct navigator access for programmatic navigation
 * - Custom UI around NavigationHost
 * - Standard production apps
 */
@Composable
fun StandardExample() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig
    )
}

/**
 * Pattern 3: Advanced Setup
 * 
 * Use this pattern for:
 * - Multi-module applications
 * - Custom deep link handling
 * - Platform-specific configuration
 * - Full control over all options
 */
@Composable
fun AdvancedExample() {
    val config = GeneratedNavigationConfig + FeatureConfig
    val navigator = rememberQuoVadisNavigator(MainTabs::class, config)
    
    NavigationHost(
        navigator = navigator,
        config = config,
        enablePredictiveBack = true,
        predictiveBackMode = PredictiveBackMode.FULL_CASCADE
    )
}
```

---

## Implementation Steps

### Step 1: Identify Current Setup

1. Locate the main `App.kt` or entry point
2. Find all navigator creation code
3. List all registry usages
4. Note any custom configurations

### Step 2: Migrate to New Patterns

1. Replace `buildMainTabsNavNode()` with class reference
2. Replace individual registries with `GeneratedNavigationConfig`
3. Replace manual `TreeNavigator` with `rememberQuoVadisNavigator()`
4. Simplify `NavigationHost` call to use `config` parameter

### Step 3: Add Example Variants

1. Create one-liner example (commented or separate file)
2. Keep standard pattern as main implementation
3. Add advanced example showing composition

### Step 4: Update Comments and Documentation

1. Add KDoc to each pattern example
2. Explain when to use each pattern
3. Reference migration guide for more details

---

## Testing Checklist

### Functional Testing

- [ ] App launches without errors
- [ ] All navigation flows work correctly
- [ ] Tab switching functions properly
- [ ] Back navigation works
- [ ] Deep links resolve correctly
- [ ] Transitions animate properly

### Pattern Verification

- [ ] One-liner pattern compiles and works
- [ ] Standard pattern compiles and works
- [ ] Advanced pattern compiles and works
- [ ] Multi-module composition works (if applicable)

### Regression Testing

- [ ] All existing screens accessible
- [ ] No visual regressions
- [ ] Performance unchanged
- [ ] All platforms work (Android, iOS, Desktop, Web)

---

## Acceptance Criteria

### Code Requirements

- [ ] Demo app uses new DSL patterns exclusively (no deprecated APIs in main path)
- [ ] At least one example of each pattern (one-liner, standard, advanced)
- [ ] Clear before/after comparisons in comments or documentation
- [ ] All example code compiles without errors or warnings

### Documentation Requirements

- [ ] Each pattern has explanatory comments
- [ ] When-to-use guidance included
- [ ] Code is well-formatted and readable
- [ ] Examples are copy-pasteable as starting points

### Quality Requirements

- [ ] No deprecated API warnings in demo (uses new APIs)
- [ ] Consistent code style with project conventions
- [ ] All functionality preserved from old implementation
- [ ] Clean separation of example patterns

---

## Estimated Effort Breakdown

| Activity | Time |
|----------|------|
| Analyze current demo setup | 0.25 days |
| Migrate main App.kt | 0.5 days |
| Create one-liner example | 0.25 days |
| Create advanced example | 0.25 days |
| Add comments and documentation | 0.25 days |
| Test all platforms | 0.25 days |
| Code review and fixes | 0.25 days |
| **Total** | **~2 days** |

---

## Related Files

- [Phase 4 Summary](./SUMMARY.md)
- [Task 4.1 - Deprecation Warnings](./TASK-4.1-deprecation-warnings.md)
- [Task 4.3 - Migration Guide](./TASK-4.3-migration-guide.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)
