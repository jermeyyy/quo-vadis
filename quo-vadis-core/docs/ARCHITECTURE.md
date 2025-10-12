# Navigation Library - Architecture Overview

## Design Principles

This navigation library follows these key principles:

1. **Modularization First**: Support for feature modules with gray box pattern
2. **Type Safety**: Compile-time safety for navigation
3. **MVI Compatible**: First-class support for MVI architecture
4. **Testable**: Easy to test with fake implementations
5. **Multiplatform**: Works on Android, iOS, Desktop, Web
6. **No External Dependencies**: Independent from other navigation libraries

## Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                    │
│  (Your screens, ViewModels, feature modules)            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Integration Layer                     │
│  - MVI Support (Intents, Effects, State)                │
│  - DI Integration (Koin, Kodein, etc.)                  │
│  - Testing Support (FakeNavigator)                       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Compose Layer                         │
│  - NavHost (Rendering)                                   │
│  - GraphNavHost (Graph-based rendering)                 │
│  - Animation/Transition support                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Core Layer                           │
│  - Navigator (Controller)                                │
│  - BackStack (State management)                          │
│  - Destination (Data model)                              │
│  - NavigationGraph (Modular graphs)                      │
│  - DeepLink (URL handling)                               │
└─────────────────────────────────────────────────────────┘
```

## Core Concepts

### 1. Destination
A `Destination` represents where you want to navigate. It's a simple data class with a route and optional arguments.

**Benefits:**
- Type-safe navigation targets
- Serializable for deep links and state restoration
- Can carry typed data

### 2. Navigator
The `Navigator` is the central controller that manages all navigation operations.

**Responsibilities:**
- Execute navigation commands
- Manage the backstack
- Handle deep links
- Coordinate with navigation graphs
- Provide observable state

### 3. BackStack
Direct access to the navigation stack with reactive state.

**Features:**
- Observable via StateFlow
- Direct manipulation (push, pop, replace, clear)
- Advanced operations (popUntil, popToRoot)
- Supports complex navigation patterns

### 4. NavigationGraph
Enables modular architecture by allowing features to define their navigation independently.

**Gray Box Pattern:**
```
Feature Module A          Feature Module B
    ┌──────────┐              ┌──────────┐
    │  Screen1 │              │  Screen3 │
    │  Screen2 │              │  Screen4 │
    └──────────┘              └──────────┘
         │                         │
    Entry Points             Entry Points
         │                         │
         └──────────┬──────────────┘
                    │
              ┌─────▼─────┐
              │ Navigator │
              └───────────┘
```

Modules expose entry points but hide internal navigation details.

### 5. NavigationTransition
Declarative API for screen transitions.

**Supported:**
- Fade
- Horizontal/Vertical slides
- Scale animations
- Custom compositions
- Shared element transitions (framework ready)

### 6. DeepLink
URI-based navigation with pattern matching.

**Features:**
- Pattern matching with parameters
- Query parameter support
- Integration with navigation graphs
- Universal link support

### 7. Predictive Back Navigation
Provides smooth, animated back gestures on both iOS and Android with automatic screen caching.

**Key Features:**
- **Gesture Tracking**: Real-time progress during user drag
- **Separate Animation Phases**: Gesture animation vs exit animation
- **Screen Caching**: Keeps screens alive during animations
- **Cache Locking**: Prevents premature destruction
- **Deferred Navigation**: Navigation happens after animation completes

**Animation Coordinator Pattern:**
```
User Gesture → Capture Entries → Lock Cache
     ↓                                ↓
Gesture Animation         Exit Animation (after release)
     ↓                                ↓
Previous Screen Rendered    Current Screen Animates Out
     ↓                                ↓
                      Navigation After Animation
                               ↓
                      Unlock Cache → Show New Screen
```

**Three Animation Types:**
1. **Material3**: Scale + translate + rounded corners + shadow
2. **Scale**: Simple scale down with fade
3. **Slide**: Slide right with fade

Each type has matching gesture and exit animations for consistency.

**Implementation:**
- `PredictiveBackAnimationCoordinator`: Manages animation state
- `ComposableCache`: Caches screens with locking mechanism
- Type-specific animations: `material3BackAnimation()`, `material3ExitAnimation()`, etc.

## Modularization Strategy

### Feature Module Structure
```kotlin
// Feature module exposes this
class FeatureNavigation : BaseModuleNavigation() {
    override fun buildGraph() = navigationGraph("feature") {
        startDestination(InternalDestination1)
        
        destination(InternalDestination1) { _, nav -> Screen1(nav) }
        destination(InternalDestination2) { _, nav -> Screen2(nav) }
    }
    
    override fun entryPoints() = listOf(
        PublicEntryPoint // Only this is exposed
    )
}

// Other modules can navigate to the feature
fun navigateToFeature() {
    navigator.navigate(featureNavigation.entryPoints().first())
}
```

**Benefits:**
- Clear module boundaries
- Internal navigation stays private
- Easy to refactor within module
- Compile-time safety across modules

## MVI Integration

The library provides first-class MVI support:

```kotlin
ViewModel → NavigationIntent → Navigator → BackStack → State
                                              ↓
                                      NavigationEffect
```

**Flow:**
1. User action triggers an Intent
2. ViewModel handles Intent, creates NavigationIntent
3. Navigator executes navigation
4. BackStack updates state
5. UI observes state changes
6. Side effects emit NavigationEffect

## State Management

All navigation state is reactive and observable:

- `Navigator.currentDestination: StateFlow<Destination?>`
- `BackStack.current: StateFlow<BackStackEntry?>`
- `BackStack.stack: StateFlow<List<BackStackEntry>>`
- `BackStack.canGoBack: StateFlow<Boolean>`

This enables:
- Reactive UI updates
- Easy testing
- State persistence
- Integration with any state management pattern

## Testing Strategy

Use `FakeNavigator` for unit tests:

```kotlin
@Test
fun `navigate to details screen`() {
    val navigator = FakeNavigator()
    val viewModel = MyViewModel(navigator)
    
    viewModel.onItemClicked("123")
    
    assertTrue(navigator.verifyNavigateTo("details"))
    assertEquals(1, navigator.getNavigateCallCount("details"))
}
```

## Performance Considerations

1. **Lazy Graph Registration**: Graphs are created lazily
2. **State Flows**: Efficient reactive updates
3. **No Reflection**: All navigation is compile-time safe
4. **Minimal Allocations**: Reuses data structures where possible

## Extension Points

The library is designed to be extended:

1. **Custom Transitions**: Implement `NavigationTransition`
2. **Custom Serializers**: Implement `NavigationStateSerializer`
3. **Custom DeepLink Handlers**: Implement `DeepLinkHandler`
4. **Custom Destinations**: Extend `Destination` interface

## Platform Considerations

### Android
- Integrates with Activity/Fragment lifecycle
- Handles system back button
- Supports deep links from Intent

### iOS
- Works with UIKit integration layer
- Supports universal links
- Handles navigation bar

### Desktop
- Window-based navigation
- Keyboard shortcuts support

### Web
- Browser history integration
- URL-based routing
- Deep link support via URL

## Migration Path

If migrating from other libraries:

1. **From Compose Navigation**: Similar concepts, easier backstack control
2. **From Voyager**: Similar screen-based approach, better modularization
3. **From Custom Solution**: Gradual migration, can coexist

## Best Practices

1. **Keep Destinations Simple**: Just data, no logic
2. **Use Sealed Classes**: For related destination groups
3. **One Graph Per Feature**: Clear module boundaries
4. **Test Navigation**: Use FakeNavigator extensively
5. **Handle Deep Links Early**: Setup in app initialization
6. **Use Transitions Sparingly**: Default fade is often enough
7. **Observe State Reactively**: Don't poll, use StateFlow
8. **Clear Backstack Judiciously**: Users expect back button to work

## Future Enhancements

Potential additions:

- [ ] Result passing between screens
- [ ] Multi-window support
- [ ] Tab-based navigation helpers
- [ ] Bottom sheet destinations
- [ ] Dialog destinations
- [ ] Saved state handles
- [ ] Nested navigation graphs
- [ ] Conditional navigation
- [ ] Navigation analytics hooks
