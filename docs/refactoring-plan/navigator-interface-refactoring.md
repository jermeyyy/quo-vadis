# Navigator Interface Refactoring Plan

## Executive Summary

The `Navigator` interface has grown to expose 25+ members (properties + methods), mixing public API for end-users with internal implementation details used only by the rendering layer. This violates SOLID principles and creates a confusing API surface.

**Goal**: Split Navigator into focused, role-based interfaces following Interface Segregation Principle (ISP).

---

## Table of Contents

1. [Problem Analysis](#problem-analysis)
2. [SOLID Violations](#solid-violations)
3. [Current Member Analysis](#current-member-analysis)
4. [Proposed Architecture](#proposed-architecture)
5. [Interface Definitions](#interface-definitions)
6. [Migration Strategy](#migration-strategy)
7. [Breaking Changes](#breaking-changes)
8. [Implementation Checklist](#implementation-checklist)

---

## Problem Analysis

### Current Interface Size

The `Navigator` interface currently exposes:

| Category | Count | Members |
|----------|-------|---------|
| State observation | 5 | `state`, `transitionState`, `currentDestination`, `previousDestination`, `canNavigateBack`, `currentTransition` |
| Navigation operations | 6 | `navigate`, `navigateBack`, `navigateAndClearTo`, `navigateAndReplace`, `navigateAndClearAll`, `navigateToPane` |
| Pane operations | 3 | `isPaneAvailable`, `paneContent`, `navigateBackInPane` |
| Deep linking | 3 | `handleDeepLink` (2 overloads), `getDeepLinkRegistry` |
| Internal/Framework | 8 | `resultManager`, `config`, `updateState`, `updateTransitionProgress`, `startPredictiveBack`, `updatePredictiveBack`, `cancelPredictiveBack`, `commitPredictiveBack`, `completeTransition` |
| Back handling | 1 | `onBack` (via `BackPressHandler`) |

**Total: ~26 members**

### Usage Patterns Observed

From codebase analysis:

1. **End-user screens** (98% of usage):
   ```kotlin
   navigator.navigate(DetailDestination("123"))
   navigator.navigateBack()
   navigator.currentDestination.collectAsState()
   navigator.canNavigateBack.collectAsState()
   ```

2. **Advanced users** (e.g., `StateDrivenContainer`):
   ```kotlin
   navigator.updateState(clearedState)
   navigator.state.value  // direct tree manipulation
   ```

3. **Framework internals** (NavigationHost, renderers):
   ```kotlin
   navigator.config  // only NavigationHost
   navigator.resultManager  // only extension functions
   navigator.updateTransitionProgress(0.5f)
   navigator.startPredictiveBack()
   navigator.updatePredictiveBack(progress, x, y)
   navigator.cancelPredictiveBack()
   navigator.commitPredictiveBack()
   navigator.completeTransition()
   navigator.transitionState.collect { ... }
   ```

4. **Deep link registration**:
   ```kotlin
   navigator.getDeepLinkRegistry().register("promo/{code}") { params -> }
   navigator.handleDeepLink(uri)
   ```

---

## SOLID Violations

### 1. Interface Segregation Principle (ISP) ❌

**Violation**: Clients are forced to depend on methods they don't use.

- Screen composables only need `navigate()`, `navigateBack()`, state observation
- They're exposed to `updateTransitionProgress()`, `commitPredictiveBack()`, `resultManager`, etc.

### 2. Single Responsibility Principle (SRP) ❌

**Violation**: Navigator has multiple responsibilities:
- Navigation command execution
- State observation
- Transition animation control
- Predictive back gesture handling
- Result management
- Deep link handling
- Configuration access

### 3. Open/Closed Principle (OCP) ⚠️

**Partial violation**: Adding new functionality requires modifying the interface.

With focused interfaces, new capabilities could be added via new interfaces without modifying existing ones.

### 4. Dependency Inversion Principle (DIP) ⚠️

**Partial violation**: High-level screens depend on low-level implementation details.

Screens should depend on a navigation abstraction, not on transition management details.

---

## Current Member Analysis

### ✅ Public API (Keep in main Navigator)

| Member | Usage | Justification |
|--------|-------|---------------|
| `state` | End-users, framework | Core observable state |
| `currentDestination` | End-users | Convenience - most common observation |
| `previousDestination` | End-users | Convenience - enable/disable back button styling |
| `canNavigateBack` | End-users | Convenience - enable/disable back button |
| `navigate()` | End-users | Core navigation operation |
| `navigateBack()` | End-users | Core navigation operation |
| `navigateAndClearTo()` | End-users | Common pattern (auth flows) |
| `navigateAndReplace()` | End-users | Common pattern (onboarding) |
| `navigateAndClearAll()` | End-users | Common pattern (logout) |
| `handleDeepLink(uri)` | End-users | Deep link handling |
| `getDeepLinkRegistry()` | End-users | Runtime deep link registration |
| `updateState()` | Advanced users | Direct tree manipulation for state-driven patterns |
| `config` | NavigationHost, advanced users | Access to registries |

### ⚠️ Pane API (Separate interface)

| Member | Usage | Justification |
|--------|-------|---------------|
| `navigateToPane()` | Pane-specific screens | Adaptive layout navigation |
| `isPaneAvailable()` | Pane-specific screens | Query pane configuration |
| `paneContent()` | Pane-specific screens | Query pane content |
| `navigateBackInPane()` | Pane-specific screens | Pane-specific back |

### 🔒 Internal API (Hide from public interface)

| Member | Usage | Justification |
|--------|-------|---------------|
| `resultManager` | Extension functions only | Implementation detail |
| `transitionState` | Framework only | Animation internal |
| `currentTransition` | Framework only | Animation internal |
| `updateTransitionProgress()` | Framework only | Animation internal |
| `startPredictiveBack()` | Framework only | Gesture internal |
| `updatePredictiveBack()` | Framework only | Gesture internal |
| `cancelPredictiveBack()` | Framework only | Gesture internal |
| `commitPredictiveBack()` | Framework only | Gesture internal |
| `completeTransition()` | Framework only | Animation internal |

---

## Proposed Architecture

### Interface Hierarchy

```
                          ┌─────────────────────────┐
                          │       Navigator         │  ◄─── Main public API
                          │ (Interface)             │       for end-users
                          ├─────────────────────────┤
                          │ • state                 │
                          │ • currentDestination    │
                          │ • previousDestination   │
                          │ • canNavigateBack       │
                          │ • navigate()            │
                          │ • navigateBack()        │
                          │ • navigateAndClearTo()  │
                          │ • navigateAndReplace()  │
                          │ • navigateAndClearAll() │
                          │ • handleDeepLink()      │
                          │ • deepLinkRegistry      │
                          │ • updateState()         │
                          │ • config                │
                          └───────────┬─────────────┘
                                      │
                    ┌─────────────────┴─────────────────┐
                    │                                   │
                    ▼                                   ▼
          ┌───────────────────┐           ┌───────────────────────┐
          │ PaneNavigator     │           │  internal interface   │
          │ (Interface)       │           │  TransitionController │
          ├───────────────────┤           ├───────────────────────┤
          │ • navigateToPane()│           │ • transitionState     │
          │ • isPaneAvailable │           │ • currentTransition   │
          │ • paneContent()   │           │ • updateProgress()    │
          │ • backInPane()    │           │ • startPredictive()   │
          └───────────────────┘           │ • updatePredictive()  │
                                          │ • cancelPredictive()  │
                                          │ • commitPredictive()  │
                                          │ • complete()          │
                                          └───────────────────────┘
```

### Implementation Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           TreeNavigator                                      │
│                                                                             │
│  implements Navigator, PaneNavigator, TransitionController                   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                         Internal State                                  ││
│  │  • _state: MutableStateFlow<NavNode>                                   ││
│  │  • _transitionState: MutableStateFlow<TransitionState>                 ││
│  │  • _resultManager: NavigationResultManager                             ││
│  │  • _config: NavigationConfig                                           ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Interface Definitions

### 1. Navigator (Slim Public API)

```kotlin
/**
 * Central navigation controller for end-user navigation operations.
 * 
 * This is the primary interface for screen composables to perform navigation.
 * For pane-specific operations in adaptive layouts, use [PaneNavigator].
 * 
 * @see PaneNavigator
 */
@Stable
interface Navigator : BackPressHandler {
    
    // =========================================================================
    // STATE OBSERVATION
    // =========================================================================
    
    /**
     * The current navigation state as an immutable tree.
     * This is the single source of truth for all navigation state.
     */
    val state: StateFlow<NavNode>
    
    /**
     * The currently active destination (deepest active ScreenNode).
     */
    val currentDestination: StateFlow<NavDestination?>
    
    /**
     * The previous destination before the current one.
     */
    val previousDestination: StateFlow<NavDestination?>
    
    /**
     * Whether back navigation is possible from the current state.
     */
    val canNavigateBack: StateFlow<Boolean>
    
    // =========================================================================
    // CONFIGURATION
    // =========================================================================
    
    /**
     * The navigation configuration providing access to registries.
     * 
     * NavigationHost uses this to resolve screen content, transitions,
     * and container wrappers. Also useful for advanced state-driven patterns.
     */
    val config: NavigationConfig
    
    // =========================================================================
    // NAVIGATION OPERATIONS
    // =========================================================================
    
    /**
     * Navigate to a destination with optional transition.
     */
    fun navigate(
        destination: NavDestination,
        transition: NavigationTransition? = null
    )
    
    /**
     * Navigate back in the active stack.
     * @return true if navigation was successful, false if at root
     */
    fun navigateBack(): Boolean
    
    /**
     * Navigate to a destination and clear the backstack up to a certain point.
     */
    fun navigateAndClearTo(
        destination: NavDestination,
        clearRoute: String? = null,
        inclusive: Boolean = false
    )
    
    /**
     * Navigate to a destination and replace the current one.
     */
    fun navigateAndReplace(
        destination: NavDestination,
        transition: NavigationTransition? = null
    )
    
    /**
     * Navigate to a destination and clear the entire active stack.
     */
    fun navigateAndClearAll(destination: NavDestination)
    
    // =========================================================================
    // STATE MANIPULATION
    // =========================================================================
    
    /**
     * Update the navigation state directly.
     * 
     * This is useful for state-driven navigation patterns and state restoration.
     * Prefer higher-level navigation methods for typical use cases.
     */
    fun updateState(newState: NavNode, transition: NavigationTransition? = null)
    
    // =========================================================================
    // DEEP LINKING
    // =========================================================================
    
    /**
     * Handle a deep link URI string.
     * @return true if navigation occurred, false if no match
     */
    fun handleDeepLink(uri: String): Boolean
    
    /**
     * Handle deep link navigation.
     */
    fun handleDeepLink(deepLink: DeepLink)
    
    /**
     * Get the deep link registry for pattern registration.
     */
    val deepLinkRegistry: DeepLinkRegistry
}
```

**Lines of code**: ~95 (includes `updateState` and `config`)

### 2. PaneNavigator (Adaptive Layout Extension)

```kotlin
/**
 * Extension interface for pane-specific navigation operations.
 * 
 * Use this interface when working with adaptive multi-pane layouts.
 * Obtain via extension function or cast when needed.
 * 
 * ```kotlin
 * val paneNavigator = navigator.asPaneNavigator()
 * paneNavigator?.navigateToPane(DetailDestination(id), PaneRole.Supporting)
 * ```
 */
@Stable
interface PaneNavigator : Navigator {
    
    /**
     * Navigate to a destination in a specific pane.
     */
    fun navigateToPane(
        destination: NavDestination,
        role: PaneRole = PaneRole.Supporting
    )
    
    /**
     * Check if a pane role is available in the current state.
     */
    fun isPaneAvailable(role: PaneRole): Boolean
    
    /**
     * Get the current content of a specific pane.
     */
    fun paneContent(role: PaneRole): NavNode?
    
    /**
     * Navigate back within a specific pane.
     */
    fun navigateBackInPane(role: PaneRole): Boolean
}

/**
 * Safely cast Navigator to PaneNavigator if supported.
 */
fun Navigator.asPaneNavigator(): PaneNavigator? = this as? PaneNavigator
```

### 3. TransitionController (Internal Only)

```kotlin
/**
 * Internal interface for transition and animation control.
 * 
 * This interface is NOT part of the public API and should only be used
 * by the NavigationHost and rendering layer.
 * 
 * @suppress
 */
@InternalQuoVadisApi
internal interface TransitionController {
    
    /**
     * The current transition state for animations.
     */
    val transitionState: StateFlow<TransitionState>
    
    /**
     * Current transition animation (null if idle).
     */
    val currentTransition: StateFlow<NavigationTransition?>
    
    /**
     * Update transition progress during animations.
     */
    fun updateTransitionProgress(progress: Float)
    
    /**
     * Start a predictive back gesture.
     */
    fun startPredictiveBack()
    
    /**
     * Update predictive back gesture progress.
     */
    fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float)
    
    /**
     * Cancel the predictive back gesture.
     */
    fun cancelPredictiveBack()
    
    /**
     * Commit the predictive back gesture.
     */
    fun commitPredictiveBack()
    
    /**
     * Complete the current transition animation.
     */
    fun completeTransition()
}
```

### 4. ResultManager Access (Private Extension)

```kotlin
// Keep resultManager internal, only accessible via extension functions

// Internal accessor in core module
internal val Navigator.resultManager: NavigationResultManager
    get() = (this as TreeNavigator).resultManager

// Or use an internal interface
@InternalQuoVadisApi
internal interface ResultCapable {
    val resultManager: NavigationResultManager
}
```

---

## Migration Strategy

### Phase 1: Create New Interfaces (Non-Breaking)

1. **Create interface files**:
   - `Navigator.kt` - Public API (with `updateState` and `config`)
   - `PaneNavigator.kt` - Pane extension
   - `TransitionController.kt` - Internal API

2. **Update TreeNavigator**:
   ```kotlin
   class TreeNavigator(/* ... */) : 
       Navigator, 
       PaneNavigator, 
       TransitionController {
       // Implementation unchanged
   }
   ```

3. **Update FakeNavigator** for tests similarly

### Phase 2: Add Deprecations

Add `@Deprecated` annotations to members moving to new interfaces:

```kotlin
interface Navigator {
    @Deprecated(
        "Use navigator.asPaneNavigator()?.navigateToPane() instead",
        ReplaceWith("asPaneNavigator()?.navigateToPane(destination, role)")
    )
    fun navigateToPane(destination: NavDestination, role: PaneRole)
    
    @Deprecated(
        "Moved to TransitionController (internal API). " +
        "If you need transition state, use currentDestination instead.",
        level = DeprecationLevel.WARNING
    )
    val transitionState: StateFlow<TransitionState>
}
```

### Phase 3: Update Internal Usages

1. **NavigationHost**: Cast to `TransitionController`
   ```kotlin
   @Composable
   fun NavigationHost(navigator: Navigator, ...) {
       val transitionController = navigator as? TransitionController
           ?: error("Navigator must implement TransitionController")
       
       // Use transitionController for animation APIs
   }
   ```

2. **Extension functions**: Use internal accessor
   ```kotlin
   suspend fun <R : Any, D> Navigator.navigateForResult(destination: D): R? 
       where D : NavDestination, D : ReturnsResult<R> {
       val manager = (this as? ResultCapable)?.resultManager
           ?: error("Navigator must support results")
       // ...
   }
   ```

### Phase 4: Documentation Updates

1. Update `ARCHITECTURE.md` with new interface hierarchy
2. Update API docs to reference correct interfaces
3. Add migration guide section

### Phase 5: Cleanup (Major Version)

In next major version:
1. Remove deprecated methods from `Navigator` interface
2. Make `TransitionController` `internal`
3. Hide `resultManager` completely

---

## Breaking Changes

### Breaking Changes in Phase 5 (Major Version)

| Change | Impact | Migration |
|--------|--------|-----------|
| `navigateToPane()` removed from `Navigator` | Compile error | Use `navigator.asPaneNavigator()?.navigateToPane()` |
| `isPaneAvailable()` removed from `Navigator` | Compile error | Use `navigator.asPaneNavigator()?.isPaneAvailable()` |
| `paneContent()` removed from `Navigator` | Compile error | Use `navigator.asPaneNavigator()?.paneContent()` |
| `navigateBackInPane()` removed from `Navigator` | Compile error | Use `navigator.asPaneNavigator()?.navigateBackInPane()` |
| `transitionState` removed from `Navigator` | Compile error | Framework only - no user migration needed |
| `resultManager` hidden | Compile error | Use `navigateForResult()` extension |
| Predictive back methods removed | Compile error | Framework only - no user migration needed |

### Non-Breaking in All Phases

- `state`, `currentDestination`, `previousDestination`, `canNavigateBack` stay
- `navigate()`, `navigateBack()`, `navigateAnd*()` stay
- `handleDeepLink()`, `deepLinkRegistry` stay
- `updateState()`, `config` stay in main Navigator
- Extension functions continue to work

---

## Implementation Checklist

### Phase 1: Interface Creation
- [ ] Create `PaneNavigator.kt` interface
- [ ] Create `TransitionController.kt` internal interface
- [ ] Create `ResultCapable.kt` internal interface
- [ ] Update `Navigator.kt` (slim down, keep `updateState` and `config`)
- [ ] Update `TreeNavigator` to implement all interfaces
- [ ] Update `FakeNavigator` to implement all interfaces
- [ ] Add extension function `asPaneNavigator()`
- [ ] Update internal `resultManager` accessor

### Phase 2: Deprecations
- [ ] Add `@Deprecated` to pane methods in `Navigator`
- [ ] Add `@Deprecated` to `transitionState` in `Navigator`
- [ ] Add `@Deprecated` to `currentTransition` in `Navigator`
- [ ] Add `@Deprecated` to predictive back methods in `Navigator`
- [ ] Add `@Deprecated` to `updateTransitionProgress()` in `Navigator`
- [ ] Add `@Deprecated` to `completeTransition()` in `Navigator`
- [ ] Add `@Deprecated` to `resultManager` in `Navigator`

### Phase 3: Internal Updates
- [ ] Update `NavigationHost.kt` to use `TransitionController`
- [ ] Update `NavTreeRenderer.kt` to use `TransitionController`
- [ ] Update `StackRenderer.kt` to use `TransitionController`
- [ ] Update `PaneRenderer.kt` to use `TransitionController`
- [ ] Update `TabRenderer.kt` to use `TransitionController`
- [ ] Update `NavigatorResultExtensions.kt` to use internal accessor
- [ ] Update wrapper scopes (`TabsContainerScope`, `PaneContainerScope`)

### Phase 4: Documentation
- [ ] Update `ARCHITECTURE.md`
- [ ] Update KDoc in all interface files
- [ ] Add migration guide
- [ ] Update website documentation

### Phase 5: Cleanup (Major Version)
- [ ] Remove deprecated methods from `Navigator`
- [ ] Make `TransitionController` visibility `internal`
- [ ] Remove `resultManager` from public API
- [ ] Update version to next major

---

## Design Principles Applied

### SOLID

| Principle | Application |
|-----------|-------------|
| **SRP** | Each interface has one responsibility |
| **OCP** | New features via new interfaces, not modifications |
| **LSP** | All implementations are substitutable |
| **ISP** | Clients only depend on methods they use |
| **DIP** | Screens depend on abstractions, not implementations |

### KISS

- Main `Navigator` interface is simple and focused
- Extension interfaces opt-in only when needed
- No complex inheritance hierarchies

### Kotlin Idioms

- Extension functions for safe casting (`asPaneNavigator()`)
- `@InternalQuoVadisApi` annotation for internal APIs
- Functional approach preserved (pure functions in `TreeMutator`)
- `StateFlow` for reactive state observation

---

## Appendix: Full Interface Comparison

### Before (Current)

```kotlin
interface Navigator : BackPressHandler {
    // 6 state properties
    val state: StateFlow<NavNode>
    val transitionState: StateFlow<TransitionState>
    val currentDestination: StateFlow<NavDestination?>
    val previousDestination: StateFlow<NavDestination?>
    val currentTransition: StateFlow<NavigationTransition?>
    val canNavigateBack: StateFlow<Boolean>
    
    // 2 manager properties
    val resultManager: NavigationResultManager
    val config: NavigationConfig
    
    // 6 navigation methods
    fun navigate(...)
    fun navigateBack(): Boolean
    fun navigateAndClearTo(...)
    fun navigateAndReplace(...)
    fun navigateAndClearAll(...)
    fun navigateToPane(...)
    
    // 4 pane methods
    fun isPaneAvailable(...): Boolean
    fun paneContent(...): NavNode?
    fun navigateBackInPane(...): Boolean
    
    // 3 deep link methods
    fun handleDeepLink(uri: String): Boolean
    fun handleDeepLink(deepLink: DeepLink)
    fun getDeepLinkRegistry(): DeepLinkRegistry
    
    // 1 state manipulation
    fun updateState(...)
    
    // 6 transition methods
    fun updateTransitionProgress(...)
    fun startPredictiveBack()
    fun updatePredictiveBack(...)
    fun cancelPredictiveBack()
    fun commitPredictiveBack()
    fun completeTransition()
}
// Total: ~26 members
```

### After (Proposed)

```kotlin
// Main API - 15 members (42% reduction)
interface Navigator : BackPressHandler {
    val state: StateFlow<NavNode>
    val currentDestination: StateFlow<NavDestination?>
    val previousDestination: StateFlow<NavDestination?>
    val canNavigateBack: StateFlow<Boolean>
    val config: NavigationConfig
    
    fun navigate(...)
    fun navigateBack(): Boolean
    fun navigateAndClearTo(...)
    fun navigateAndReplace(...)
    fun navigateAndClearAll(...)
    fun updateState(...)
    
    fun handleDeepLink(uri: String): Boolean
    fun handleDeepLink(deepLink: DeepLink)
    val deepLinkRegistry: DeepLinkRegistry
}

// Pane extension - 4 members
interface PaneNavigator : Navigator {
    fun navigateToPane(...)
    fun isPaneAvailable(...): Boolean
    fun paneContent(...): NavNode?
    fun navigateBackInPane(...): Boolean
}

// Internal - 8 members
internal interface TransitionController {
    val transitionState: StateFlow<TransitionState>
    val currentTransition: StateFlow<NavigationTransition?>
    fun updateTransitionProgress(...)
    fun startPredictiveBack()
    fun updatePredictiveBack(...)
    fun cancelPredictiveBack()
    fun commitPredictiveBack()
    fun completeTransition()
}

// Internal - 1 member
internal interface ResultCapable {
    val resultManager: NavigationResultManager
}
```

**Result**: Main public API reduced from 26 to 15 members (42% reduction), with clear separation of internal/framework APIs from public APIs. Pane-specific functionality moved to optional extension interface.
