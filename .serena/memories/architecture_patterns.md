# Navigation Library - Complete Architecture & Implementation

> ⚠️ **PARTIALLY UPDATED**: This file is being updated to reflect the new NavNode tree-based architecture.

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

### 5. FlowMVI Architecture Integration
- MVI support via separate `quo-vadis-core-flow-mvi` module
- FlowMVI library integration
- Type-safe navigation intents and actions
- Observable navigation state

### 6. Predictive Back Navigation
- Smooth gesture-based navigation
- Two-phase animation system
- Automatic screen caching
- Works on both Android and iOS
- Prevents premature screen destruction

### 7. Shared Element Transitions (NEW!)
- Material Design-compliant shared elements
- Works in BOTH forward AND backward navigation
- Compatible with predictive back gestures
- Per-destination opt-in via `destinationWithScopes()`
- Type-safe with compile-time guarantees
- Full multiplatform support

## Package Structure

```
com.jermey.quo.vadis.core.navigation/
├── core/
│   ├── Destination.kt              - Navigation targets (type-safe)
│   ├── NavNode.kt                  - Tree-based navigation state
│   ├── TreeNavigator.kt            - Central controller
│   ├── TreeMutator.kt              - Tree manipulation
│   ├── NavigationTransition.kt     - Animation support + SharedElementConfig
│   └── DeepLink.kt                 - URI-based navigation
├── compose/
│   ├── QuoVadisHost.kt             - Unified navigation host
│   ├── PredictiveBackNavigation.kt - Gesture navigation
│   ├── ComposableCache.kt          - Screen caching
│   ├── SharedElementScope.kt       - ✨ CompositionLocal providers
│   └── SharedElementModifiers.kt   - ✨ Convenience extensions
├── integration/
│   └── KoinIntegration.kt          - DI support
├── utils/
│   └── NavigationExtensions.kt     - Utility functions
├── testing/
│   └── FakeNavigator.kt            - Testing utilities
└── serialization/
    └── StateSerializer.kt          - State persistence
```

**FlowMVI Module** (`quo-vadis-core-flow-mvi/`):
For MVI architecture, use the separate FlowMVI integration module which provides:
- NavigationIntent - Type-safe navigation actions
- NavigationAction - Side effects
- NavigationState - Observable state
- NavigatorContainer - FlowMVI store integration

## Key Components

### TreeNavigator
Central navigation controller with reactive state:
- `navigate(destination, transition)` - Navigate to destination
- `navigateBack()` - Go back
- `navigateAndClearTo()` - Navigate and clear to destination
- `state` - NavNode tree state as StateFlow
- `currentDestination` - Observable current destination

### NavNode Tree
Tree-based navigation state with StateFlow:
- Immutable tree structure
- `TreeMutator` for tree manipulation
- `push()`, `pop()`, `replace()`, `clear()` operations
- Observable state via StateFlow

### NavigationGraph
Modular graph definitions with DSL:
```kotlin
navigationGraph("feature") {
    startDestination(Screen1)
    destination(Screen1) { _, nav -> Screen1UI(nav) }
    
    // ✨ NEW: Per-destination shared elements
    destinationWithScopes(Screen2) { _, nav, shared, animated ->
        Screen2UI(nav, shared, animated)
    }
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

## Predictive Back Animation Flow

```
User Gesture Start
    ↓
Coordinator captures entries (current + previous)
    ↓
Lock cache entries (prevent destruction)
    ↓
Gesture Phase: Real-time animation (gestureProgress)
    ↓ (Shared elements follow gesture if enabled)
User releases → Exit Phase starts
    ↓
Exit animation plays to completion (exitProgress)
    ↓ (Shared elements continue animating)
Animation completes → navigator.navigateBack()
    ↓
Coordinator finishes → Unlock cache
    ↓
New screen renders smoothly
```

## Shared Element Transition Flow

```
User initiates navigation (forward or back)
    ↓
QuoVadisHost uses AnimatedContent
    ↓
Provides AnimatedVisibilityScope via CompositionLocal
    ↓
Screens receive scopes via destinationWithScopes()
    ↓
Elements with matching keys identified
    ↓
SharedTransitionLayout animates:
    - Position (bounds)
    - Size
    - Shape (if applicable)
    - Content (crossfade for sharedBounds)
    ↓
Animation completes → Navigation finishes
```

## Platform Support

### Common (commonMain)
- All core logic
- Compose UI integration
- Platform-agnostic abstractions
- Shared element transitions

### Android (androidMain)
- System back button handling
- Deep link intent handling
- Activity integration
- Predictive back (API 33+) with shared elements

### iOS (iosMain)
- Navigation bar integration
- Universal link handling
- iOS gesture support
- Native back gesture with shared elements

### Web/Desktop (jsMain/wasmJsMain/desktopMain)
- Shared element transitions
- Browser back button (Web)
- Keyboard shortcuts (Desktop)

## Best Practices

### DO ✅
- Use sealed classes for destinations
- Keep destinations simple (data only)
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
- **NEW**: Same key for multiple elements
- **NEW**: Mix shared element modifier types
- **NEW**: Forget AnimatedVisibilityScope parameter
- **NEW**: Use global shared element flags (removed!)

## Performance Optimizations

1. **Lazy Graph Registration**: Graphs created on-demand
2. **Efficient StateFlows**: Batched updates
3. **No Reflection**: Compile-time safety
4. **Smart Caching**: Only necessary screens
5. **GPU Animations**: graphicsLayer for performance
6. **Cache Locking**: Prevents unnecessary recreation
7. **NEW**: Shared elements GPU-accelerated on all platforms
8. **NEW**: SharedTransitionLayout minimal overhead when not animating

## Extension Points

Extendable via:
1. Custom transitions (implement NavigationTransition)
2. Custom serializers (implement StateSerializer)
3. Custom deep link handlers (implement DeepLinkHandler)
4. Custom destinations (extend Destination)
5. Custom animation types (add to PredictiveBackAnimationType)
6. **NEW**: Custom shared element animations (BoundsTransform, EnterTransition, ExitTransition)

## Recent Architectural Changes (December 2024)

### Shared Element Transitions Implementation
1. **Always-On SharedTransitionLayout**: Removed global flag, always enabled (lightweight)
2. **AnimatedContent Unification**: Both forward and back navigation use AnimatedContent
4. **Graceful Degradation**: Elements render normally if scopes null
5. **Type-Safe Keys**: String keys but typed at call sites

This maintains architectural consistency while adding powerful new capabilities.