````markdown
# Task 2.2: Create QuoVadisNavigation One-Liner

> **Task Status**: ⬜ Not Started  
> **Estimated Effort**: 1-2 days  
> **Dependencies**: Task 2.1 (rememberQuoVadisNavigator), Task 2.3 (NavigationHost Config Overload)  
> **Blocks**: Phase 4.2 (Demo App Update)

---

## Objective

Create a single Composable function `QuoVadisNavigation` that combines navigator creation and `NavigationHost` rendering into a one-liner setup. This function is designed for the 80% use case where developers want minimal boilerplate.

**Target Usage Pattern**:
```kotlin
@Composable
fun App() {
    // That's it! One line for complete navigation setup
    QuoVadisNavigation(MainTabs::class)
}

// Or with customization
@Composable
fun App() {
    QuoVadisNavigation(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig,
        enablePredictiveBack = true,
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## Files to Create/Modify

### Files to Modify

| File | Path | Change |
|------|------|--------|
| `QuoVadisComposables.kt` | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisComposables.kt` | Add `QuoVadisNavigation` function |

### Files to Reference

| File | Purpose |
|------|---------|
| Task 2.1 output | `rememberQuoVadisNavigator` function |
| Task 2.3 output | `NavigationHost` config overload |
| Platform detection | For default parameter values |

---

## Function Signature and Implementation

### QuoVadisNavigation (add to QuoVadisComposables.kt)

```kotlin
/**
 * One-liner navigation setup combining navigator creation and NavigationHost.
 * 
 * This is the simplest way to set up Quo Vadis navigation. It:
 * - Creates and remembers a Navigator instance
 * - Renders the NavigationHost with the navigator
 * - Applies sensible platform-specific defaults
 * 
 * ## Basic Usage (Simplest)
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(MainTabs::class)
 * }
 * ```
 * 
 * ## With Configuration
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(
 *         rootDestination = MainTabs::class,
 *         config = GeneratedNavigationConfig,
 *         modifier = Modifier.fillMaxSize(),
 *         enablePredictiveBack = true
 *     )
 * }
 * ```
 * 
 * ## Multi-Module Setup
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(
 *         rootDestination = MainTabs::class,
 *         config = GeneratedNavigationConfig + FeatureAConfig + FeatureBConfig
 *     )
 * }
 * ```
 * 
 * ## When to Use This vs. Separate Navigator
 * 
 * Use `QuoVadisNavigation` when:
 * - You want the simplest setup possible
 * - You don't need direct access to the Navigator outside NavigationHost
 * - Default behavior is sufficient
 * 
 * Use [rememberQuoVadisNavigator] + [NavigationHost] when:
 * - You need to access the Navigator from other Composables
 * - You want to perform navigation from outside the navigation tree
 * - You need custom lifecycle handling
 * 
 * @param rootDestination The destination class for the root container.
 *                        Must be registered in the config (annotated with @Tabs, @Stack, or @Pane).
 * @param modifier Modifier to apply to the NavigationHost container.
 * @param config The NavigationConfig providing all navigation registries.
 *               Defaults to GeneratedNavigationConfig.
 * @param key Optional custom key for the root navigator node.
 * @param enablePredictiveBack Whether to enable predictive back gesture support.
 *                             Defaults based on platform (true for Android, false for others).
 * @param predictiveBackMode The mode for predictive back behavior.
 *                           Only applicable when [enablePredictiveBack] is true.
 * @param windowSizeClass Optional window size class for adaptive layouts.
 *                        Pass this to enable responsive navigation patterns.
 * 
 * @see rememberQuoVadisNavigator for creating a Navigator with manual control
 * @see NavigationHost for the underlying host implementation
 * @see NavigationConfig for configuration details
 */
@Composable
fun QuoVadisNavigation(
    rootDestination: KClass<out Destination>,
    modifier: Modifier = Modifier,
    config: NavigationConfig,
    key: String? = null,
    enablePredictiveBack: Boolean = platformDefaultPredictiveBack(),
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    windowSizeClass: WindowSizeClass? = null
) {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = rootDestination,
        config = config,
        key = key
    )
    
    NavigationHost(
        navigator = navigator,
        config = config,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
        predictiveBackMode = predictiveBackMode,
        windowSizeClass = windowSizeClass
    )
}

/**
 * Overload accepting a destination instance for parameterized roots.
 * 
 * ## Usage
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(
 *         rootDestination = MainTabs(initialTab = 1),
 *         config = GeneratedNavigationConfig
 *     )
 * }
 * ```
 * 
 * @param rootDestination The destination instance for the root container.
 * @see QuoVadisNavigation for the main overload with full documentation
 */
@Composable
fun QuoVadisNavigation(
    rootDestination: Destination,
    modifier: Modifier = Modifier,
    config: NavigationConfig,
    key: String? = null,
    enablePredictiveBack: Boolean = platformDefaultPredictiveBack(),
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    windowSizeClass: WindowSizeClass? = null
) {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = rootDestination,
        config = config,
        key = key
    )
    
    NavigationHost(
        navigator = navigator,
        config = config,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
        predictiveBackMode = predictiveBackMode,
        windowSizeClass = windowSizeClass
    )
}

/**
 * Returns the platform-specific default for predictive back support.
 * 
 * - Android: `true` (system back gesture integration)
 * - iOS: `false` (uses native swipe-back)
 * - Desktop: `false` (no native gesture)
 * - Web: `false` (browser handles back)
 */
@Composable
internal expect fun platformDefaultPredictiveBack(): Boolean
```

### Platform-Specific Implementations

#### Android Implementation
`quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PlatformDefaults.android.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = true
```

#### iOS Implementation
`quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PlatformDefaults.ios.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
```

#### Desktop Implementation
`quo-vadis-core/src/desktopMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PlatformDefaults.desktop.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
```

#### JS Implementation
`quo-vadis-core/src/jsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PlatformDefaults.js.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
```

#### WASM Implementation
`quo-vadis-core/src/wasmJsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PlatformDefaults.wasmJs.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
```

---

## How It Combines Navigator + NavigationHost

### Data Flow

```
QuoVadisNavigation(MainTabs::class, config)
         │
         ▼
┌────────────────────────────────────┐
│ rememberQuoVadisNavigator(         │
│   rootDestination = MainTabs::class│
│   config = config                  │
│ )                                  │
│         │                          │
│         ▼                          │
│   config.buildNavNode(MainTabs)    │
│         │                          │
│         ▼                          │
│   TreeNavigator(                   │
│     initialState = navNode,        │
│     scopeRegistry = ...,           │
│     containerRegistry = ...,       │
│     deepLinkHandler = ...          │
│   )                                │
└────────────────────────────────────┘
         │
         ▼
┌────────────────────────────────────┐
│ NavigationHost(                    │
│   navigator = navigator,           │
│   config = config,                 │
│   modifier = modifier,             │
│   enablePredictiveBack = ...,      │
│   ...                              │
│ )                                  │
│         │                          │
│         ▼                          │
│   Uses config.screenRegistry       │
│   Uses config.containerRegistry    │  // includes wrapper functionality
│   Uses config.transitionRegistry   │
│   Uses config.scopeRegistry        │
└────────────────────────────────────┘
```

### Registry Usage Split

| Component | Uses |
|-----------|------|
| `TreeNavigator` | `scopeRegistry`, `containerRegistry`, `deepLinkHandler` |
| `NavigationHost` | `screenRegistry`, `containerRegistry` (incl. wrappers), `transitionRegistry`, `scopeRegistry` |

The `config` object is passed to both, ensuring consistent registry access.

---

## Platform-Specific Considerations

### Android

| Feature | Default | Notes |
|---------|---------|-------|
| Predictive Back | `true` | Integrates with system back gesture (Android 13+) |
| Back Handler | Automatic | `NavigationHost` handles `BackHandler` |
| Activity Lifecycle | ✅ | Navigator follows Composable lifecycle |

```kotlin
// Android-specific usage
@Composable
fun App() {
    QuoVadisNavigation(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig,
        // predictive back enabled by default on Android
        predictiveBackMode = PredictiveBackMode.FULL_CASCADE
    )
}
```

### iOS

| Feature | Default | Notes |
|---------|---------|-------|
| Predictive Back | `false` | iOS uses native swipe-back gesture |
| Back Gesture | Native | UIKit handles navigation gestures |
| Scene Lifecycle | ✅ | Navigator follows Composable lifecycle |

```kotlin
// iOS-specific usage
@Composable
fun App() {
    QuoVadisNavigation(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig
        // predictive back disabled by default on iOS
    )
}
```

### Desktop

| Feature | Default | Notes |
|---------|---------|-------|
| Predictive Back | `false` | No native gesture support |
| Keyboard Nav | ✅ | Consider Escape key handling |
| Window Lifecycle | ✅ | Navigator follows window lifecycle |

```kotlin
// Desktop-specific usage
@Composable
fun App() {
    QuoVadisNavigation(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig,
        modifier = Modifier.fillMaxSize(),
        windowSizeClass = calculateWindowSizeClass()
    )
}
```

### Web (JS/WASM)

| Feature | Default | Notes |
|---------|---------|-------|
| Predictive Back | `false` | Browser handles back button |
| History API | Consider | May want browser history integration |
| Mobile Web | Consider | Touch gestures on mobile browsers |

```kotlin
// Web-specific usage
@Composable
fun App() {
    QuoVadisNavigation(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig
        // May want to integrate with browser history
    )
}
```

---

## Default Values Strategy

### Parameter Defaults

| Parameter | Default Value | Rationale |
|-----------|--------------|-----------|
| `modifier` | `Modifier` | Standard Compose pattern |
| `key` | `null` | Use destination's default key |
| `enablePredictiveBack` | `platformDefaultPredictiveBack()` | Platform-appropriate |
| `predictiveBackMode` | `ROOT_ONLY` | Safest default |
| `windowSizeClass` | `null` | Opt-in for responsive |

### Why No Default Config?

```kotlin
// NOT this:
config: NavigationConfig = GeneratedNavigationConfig // ❌

// Instead:
config: NavigationConfig  // ✅ Required parameter
```

Reasons:
1. **Explicitness**: Makes dependency visible in calling code
2. **Multi-module**: No assumption about which config is "the" config
3. **Testability**: Easier to provide mock configs in tests
4. **Compilation**: No KSP dependency in common code

---

## Dependencies on Other Tasks

### Task 2.1 (rememberQuoVadisNavigator)

```kotlin
// Uses Task 2.1's function
val navigator = rememberQuoVadisNavigator(
    rootDestination = rootDestination,
    config = config,
    key = key
)
```

### Task 2.3 (NavigationHost Config Overload)

```kotlin
// Uses Task 2.3's overload
NavigationHost(
    navigator = navigator,
    config = config,  // Single config instead of 4 registries
    modifier = modifier,
    ...
)
```

---

## Acceptance Criteria Checklist

### Core Functionality
- [ ] `QuoVadisNavigation(KClass, config)` renders navigation correctly
- [ ] `QuoVadisNavigation(Destination, config)` overload works
- [ ] All `NavigationHost` parameters are forwardable
- [ ] Combines navigator creation and host rendering seamlessly
- [ ] Same config instance used for navigator and host

### Platform Defaults
- [ ] `platformDefaultPredictiveBack()` returns `true` on Android
- [ ] `platformDefaultPredictiveBack()` returns `false` on iOS
- [ ] `platformDefaultPredictiveBack()` returns `false` on Desktop
- [ ] `platformDefaultPredictiveBack()` returns `false` on JS
- [ ] `platformDefaultPredictiveBack()` returns `false` on WASM

### Integration
- [ ] Works end-to-end with generated config
- [ ] Works with composed configs
- [ ] Navigator state persists across recompositions
- [ ] Navigation actions work correctly (push, pop, etc.)

### Documentation
- [ ] KDoc with usage examples
- [ ] Comparison with manual navigator pattern documented
- [ ] Platform differences documented
- [ ] All parameters documented

### Code Quality
- [ ] No compiler warnings
- [ ] Follows project code style
- [ ] Proper expect/actual structure for platform defaults

---

## Demo App Usage Examples

### Basic Usage (App.kt)

```kotlin
// composeApp/src/commonMain/kotlin/App.kt

@Composable
fun App() {
    MaterialTheme {
        // One line for complete navigation setup!
        QuoVadisNavigation(
            rootDestination = MainTabs::class,
            config = GeneratedNavigationConfig
        )
    }
}
```

### With Theming and Modifiers

```kotlin
@Composable
fun App() {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            QuoVadisNavigation(
                rootDestination = MainTabs::class,
                config = GeneratedNavigationConfig,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

### Multi-Module App

```kotlin
@Composable
fun App() {
    val combinedConfig = remember {
        GeneratedNavigationConfig + 
        FeatureANavigationConfig + 
        FeatureBNavigationConfig +
        FeatureCNavigationConfig
    }
    
    MaterialTheme {
        QuoVadisNavigation(
            rootDestination = MainTabs::class,
            config = combinedConfig
        )
    }
}
```

### Responsive Navigation

```kotlin
@Composable
fun App() {
    val windowSizeClass = calculateWindowSizeClass()
    
    MaterialTheme {
        QuoVadisNavigation(
            rootDestination = MainTabs::class,
            config = GeneratedNavigationConfig,
            windowSizeClass = windowSizeClass,
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

### With Custom Predictive Back Mode

```kotlin
@Composable
fun App() {
    MaterialTheme {
        QuoVadisNavigation(
            rootDestination = MainTabs::class,
            config = GeneratedNavigationConfig,
            enablePredictiveBack = true,
            predictiveBackMode = PredictiveBackMode.FULL_CASCADE
        )
    }
}
```

---

## Implementation Notes

### Design Decisions

1. **Required Config Parameter**
   - No default to ensure explicit dependency
   - Better for multi-module projects

2. **Platform-Specific Defaults via expect/actual**
   - Clean separation of platform concerns
   - Composable function for future flexibility

3. **Forward All Parameters**
   - Every NavigationHost parameter available
   - No loss of control vs. manual setup

### Potential Issues

1. **Config Identity Stability**
   - `config1 + config2` creates new instance each call
   - Advise using `remember` for composed configs

2. **Navigator Access**
   - No direct navigator access from outside
   - Document when to use manual pattern instead

3. **Testing**
   - Need platform-specific test runners
   - Mock config for unit tests

---

## Related Files

- [Phase 2 Summary](./SUMMARY.md)
- [Task 2.1 - rememberQuoVadisNavigator](./TASK-2.1-remember-navigator.md)
- [Task 2.3 - NavigationHost Config Overload](./TASK-2.3-navigation-host-overload.md)
- [Phase 4.2 - Update Demo App](../phase-4-migration-deprecation/TASK-4.2-update-demo-app.md)
- [Full Refactoring Plan](../../DSL_CODE_GENERATION_REFACTORING.md)

````
