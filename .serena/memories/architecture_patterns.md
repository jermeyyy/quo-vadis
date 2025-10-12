# Navigation Library - Complete Architecture & Implementation

## Overview
**Quo Vadis** (Latin for "Where are you going?") - A comprehensive, type-safe navigation library for Kotlin Multiplatform with Compose Multiplatform UI.

## Core Architecture Principles

### 1. Type Safety First
- Compile-time safe navigation with sealed classes
- NO string-based routing in application code
- Typed argument passing with validation

### 2. Modularization (Gray Box Pattern)
- Feature modules expose entry points
- Internal navigation stays private
- Clear module boundaries
- Easy refactoring within modules

### 3. Reactive State Management
- StateFlow for observable state
- SharedFlow for one-time events
- All navigation state is reactive
- Automatic UI updates

### 4. Direct Backstack Access
- Full control over navigation stack
- Observable state via StateFlow
- Advanced operations (popUntil, popTo, etc.)
- Complex navigation patterns supported

### 5. MVI Architecture Integration
- First-class support for MVI pattern
- NavigationIntent for actions
- NavigationEffect for side effects
- NavigationState for UI

### 6. Predictive Back Navigation
- Smooth gesture-based navigation
- Two-phase animation system
- Automatic screen caching
- Works on both Android and iOS
- Prevents premature screen destruction

## Package Structure

```
com.jermey.quo.vadis.core.navigation/
├── core/
│   ├── Destination.kt              - Navigation targets (type-safe)
│   ├── BackStack.kt                - Stack manipulation & state
│   ├── Navigator.kt                - Central controller
│   ├── NavigationGraph.kt          - Modular graph definitions
│   ├── NavigationTransition.kt     - Animation support
│   └── DeepLink.kt                 - URI-based navigation
├── compose/
│   ├── NavHost.kt                  - Basic navigation host
│   ├── GraphNavHost.kt             - Graph-based host
│   ├── PredictiveBackNavigation.kt - Gesture navigation
│   └── ComposableCache.kt          - Screen caching
├── mvi/
│   ├── NavigationIntent.kt         - MVI intents
│   ├── NavigationEffect.kt         - Side effects
│   └── NavigationViewModel.kt      - Base ViewModel
├── integration/
│   └── KoinIntegration.kt          - DI support
├── utils/
│   └── NavigationExtensions.kt     - Utility functions
├── testing/
│   └── FakeNavigator.kt            - Testing utilities
└── serialization/
    └── StateSerializer.kt          - State persistence
```

## Key Components

### Navigator
Central navigation controller with reactive state:
- `navigate(destination, transition)` - Navigate to destination
- `navigateBack()` - Go back
- `navigateAndClearTo()` - Navigate and clear stack
- `backStack` - Direct backstack access
- `currentDestination` - Observable current destination

### BackStack
Direct stack manipulation with StateFlow:
- `push()`, `pop()`, `replace()`, `clear()`
- `popUntil()`, `popTo()`, `popToRoot()`
- `current`, `previous`, `canGoBack` - Observable state
- `stack` - Full stack as StateFlow

### NavigationGraph
Modular graph definitions with DSL:
```kotlin
navigationGraph("feature") {
    startDestination(Screen1)
    destination(Screen1) { _, nav -> Screen1UI(nav) }
    destination(Screen2) { _, nav -> Screen2UI(nav) }
}
```

### PredictiveBackNavigation
Gesture-based navigation with smooth animations:

**Two-Phase Animation System:**
1. **Gesture Phase**: User dragging, real-time animation
2. **Exit Phase**: After release, smooth completion

**Animation Coordinator Pattern:**
- Captures entries at animation start
- Freezes previous screen for rendering
- Current screen uses live entry for updates
- Locks cache entries during animation
- Defers navigation until animation completes

**Three Animation Types:**
- Material3: Scale + translate + corners + shadow
- Scale: Simple scale with fade
- Slide: Slide right with fade

Each type has matching gesture and exit animations.

**Key Classes:**
- `PredictiveBackAnimationCoordinator` - Manages animation state
- `ComposableCache` - Caches screens with locking
- Animation modifiers: `material3BackAnimation()`, etc.

## Design Patterns Used

### 1. Builder Pattern (DSL)
Navigation graphs use Kotlin DSL:
```kotlin
navigationGraph("app") {
    startDestination(Home)
    destination(Home) { _, nav -> HomeUI(nav) }
}
```

### 2. Observer Pattern
All state observable via StateFlow:
```kotlin
val current by navigator.backStack.current.collectAsState()
```

### 3. Strategy Pattern
Transitions are swappable strategies:
```kotlin
navigator.navigate(dest, NavigationTransitions.Slide)
```

### 4. Coordinator Pattern
PredictiveBackAnimationCoordinator separates logical and visual state

### 5. Facade Pattern
Navigator acts as facade for complex operations

## State Management Flow

```
User Action → Intent → Navigator → BackStack Update
                                         ↓
                                    State Change
                                         ↓
                                   UI Recomposition
```

For MVI:
```
User Action → NavigationIntent → ViewModel → Navigator
                                                  ↓
                                             BackStack
                                                  ↓
                                         NavigationState
                                                  ↓
                                         NavigationEffect
```

## Predictive Back Animation Flow

```
User Gesture Start
    ↓
Coordinator captures entries (current + previous)
    ↓
Lock cache entries (prevent destruction)
    ↓
Gesture Phase: Real-time animation (gestureProgress)
    ↓
User releases → Exit Phase starts
    ↓
Exit animation plays to completion (exitProgress)
    ↓
Animation completes → navigator.navigateBack()
    ↓
Coordinator finishes → Unlock cache
    ↓
New screen renders smoothly
```

## Platform Support

### Common (commonMain)
- All core logic
- Compose UI integration
- Platform-agnostic abstractions

### Android (androidMain)
- System back button handling
- Deep link intent handling
- Activity integration
- Predictive back (API 33+)

### iOS (iosMain)
- Navigation bar integration
- Universal link handling
- iOS gesture support
- Native back gesture

## Testing Strategy

### Unit Testing
```kotlin
val fakeNavigator = FakeNavigator()
viewModel.navigate(destination)
assertTrue(fakeNavigator.verifyNavigateTo("details"))
```

### Integration Testing
- Graph configurations
- Deep link handling
- Backstack operations

### UI Testing
- Screen transitions
- Back button behavior
- Gesture animations

## Best Practices

### DO ✅
- Use sealed classes for destinations
- Keep destinations simple (data only)
- One graph per feature module
- Test with FakeNavigator
- Observe state reactively
- Use type-safe navigation
- Document public APIs
- Handle deep links early
- Lock cache during animations

### DON'T ❌
- String-based navigation
- Circular dependencies
- Store UI state in Navigator
- Blocking operations in navigation
- Expose mutable state
- Create StateFlows in Composables
- Use global navigation singletons
- Mix navigation and business logic

## Performance Optimizations

1. **Lazy Graph Registration**: Graphs created on-demand
2. **Efficient StateFlows**: Batched updates
3. **No Reflection**: Compile-time safety
4. **Smart Caching**: Only necessary screens
5. **GPU Animations**: graphicsLayer for performance
6. **Cache Locking**: Prevents unnecessary recreation

## Extension Points

Extendable via:
1. Custom transitions (implement NavigationTransition)
2. Custom serializers (implement StateSerializer)
3. Custom deep link handlers (implement DeepLinkHandler)
4. Custom destinations (extend Destination)
5. Custom animation types (add to PredictiveBackAnimationType)

## Related Documentation

- `API_REFERENCE.md` - Complete API documentation
- `ARCHITECTURE.md` - Detailed architecture
- `MULTIPLATFORM_PREDICTIVE_BACK.md` - Predictive back details
- `NAVIGATION_IMPLEMENTATION.md` - Implementation summary