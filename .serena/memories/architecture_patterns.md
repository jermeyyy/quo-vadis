# Architecture Patterns and Design Guidelines

## Core Architecture Principles

### 1. Modularization First
The library is designed with a **gray box pattern** for feature modules:
- Modules expose **entry points** but hide internal navigation
- Each feature defines its own navigation graph
- Internal screens remain private to the module
- Promotes loose coupling and high cohesion

### 2. Type Safety
- All navigation is compile-time safe
- **No string-based routing** in application code
- Sealed classes for destination hierarchies
- Type-safe argument passing

### 3. Reactive State Management
- **StateFlow** for observable state
- **SharedFlow** for one-time events
- All navigation state is reactive
- UI observes state changes automatically

### 4. MVI Architecture Support
The library provides first-class MVI support:
```
ViewModel → NavigationIntent → Navigator → BackStack → State
                                              ↓
                                      NavigationEffect
```

### 5. Testability
- Interface-based design for easy mocking
- `FakeNavigator` for unit testing
- No dependency on UI framework for core logic
- Separation of concerns enables isolated testing

## Layer Architecture

```
┌─────────────────────────────────────────┐
│        Application Layer                │
│  (Screens, ViewModels, Features)        │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│       Integration Layer                 │
│  (MVI, DI, Testing Support)             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         Compose Layer                   │
│  (NavHost, GraphNavHost, Rendering)     │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│          Core Layer                     │
│  (Navigator, BackStack, Destination)    │
└─────────────────────────────────────────┘
```

## Core Components

### Destination
**Purpose**: Represents a navigation target
- Simple data class with route and optional arguments
- Can be typed for compile-time safety
- Serializable for deep links and state restoration

**Pattern**:
```kotlin
sealed class AppDestination : Destination {
    object Home : AppDestination() {
        override val route = "home"
    }
    
    data class Details(val id: String) : AppDestination() {
        override val route = "details"
        override val arguments = mapOf("id" to id)
    }
}
```

### Navigator
**Purpose**: Central navigation controller
- Manages backstack operations
- Handles deep links
- Coordinates with navigation graphs
- Provides observable state

**Responsibilities**:
- Execute navigation commands
- Manage the backstack
- Handle transitions
- Provide reactive state

### BackStack
**Purpose**: Direct access to navigation stack
- Observable via StateFlow
- Direct manipulation (push, pop, replace, clear)
- Advanced operations (popUntil, popToRoot)
- Supports complex navigation patterns

### NavigationGraph
**Purpose**: Modular navigation structure
- Defines destinations and their composables
- Can be nested and composed
- Start destination configuration
- Deep link registration

## Design Patterns Used

### 1. Builder Pattern (DSL)
Navigation graphs use Kotlin DSL for configuration:
```kotlin
navigationGraph("feature") {
    startDestination(Screen1)
    destination(Screen1) { _, nav -> Screen1UI(nav) }
    destination(Screen2) { _, nav -> Screen2UI(nav) }
}
```

### 2. Observer Pattern
All state is observable through StateFlow:
```kotlin
navigator.currentDestination.collectAsState()
backStack.canGoBack.collectAsState()
```

### 3. Strategy Pattern
Navigation transitions are strategies that can be swapped:
```kotlin
navigator.navigate(dest, NavigationTransitions.SlideHorizontal)
```

### 4. Facade Pattern
Navigator acts as a facade for complex navigation operations

### 5. Factory Pattern
Used for creating destinations and graphs

## State Management

### Unidirectional Data Flow
```
User Action → Intent → Navigator → BackStack Update
                                         ↓
                                    State Change
                                         ↓
                                   UI Recomposition
```

### State Holders
- **Navigator**: Holds current destination
- **BackStack**: Holds stack of entries
- All state exposed as **StateFlow** (immutable from outside)

### Side Effects
- Navigation effects emitted via **SharedFlow**
- Collected in UI layer for one-time events
- Examples: show error, navigate to external app

## Dependency Management

### No External Navigation Dependencies
- Independent from Jetpack Navigation
- Independent from Voyager
- Independent from Decompose
- Only depends on Compose and Kotlin stdlib

### DI Framework Support
- Interfaces for all core components
- Factory patterns for creation
- Support for Koin, Kodein, etc.

## Navigation Patterns

### 1. Simple Navigation
```kotlin
navigator.navigate(DetailsDestination("123"))
```

### 2. Navigation with Transitions
```kotlin
navigator.navigate(
    destination = Details,
    transition = NavigationTransitions.SlideHorizontal
)
```

### 3. Stack Clearing
```kotlin
// Clear to specific destination
navigator.navigateAndClearTo(Home, "login", inclusive = true)

// Clear all and navigate
navigator.navigateAndClearAll(Home)
```

### 4. Conditional Navigation
```kotlin
if (backStack.contains("details")) {
    navigator.navigateBack()
} else {
    navigator.navigate(DetailsDestination())
}
```

### 5. Deep Linking
```kotlin
val handler = DefaultDeepLinkHandler()
handler.register("app://product/{id}") { params ->
    ProductDestination(params["id"]!!)
}
navigator.handleDeepLink(DeepLink.parse(uri))
```

## Compose Integration

### NavHost
- Renders current destination
- Handles transitions
- Manages composition lifecycle

### State Hoisting
- Navigation state managed by Navigator
- Composables observe state
- Actions passed down as callbacks

### Lifecycle Awareness
- LaunchedEffect for side effects
- DisposableEffect for cleanup
- rememberNavigator for scoped instances

## Multiplatform Considerations

### Common Code (commonMain)
- All core navigation logic
- Compose UI integration
- Platform-agnostic abstractions

### Platform-Specific (androidMain/iosMain)
- System back button handling (Android)
- Navigation bar integration (iOS)
- Deep link intent handling (Android)
- Universal link handling (iOS)

### Expect/Actual
- Platform-specific back handling
- Platform-specific deep link parsing
- Platform-specific transitions (if needed)

## Testing Strategy

### Unit Testing
- Use `FakeNavigator` for ViewModel tests
- Test navigation logic in isolation
- Verify navigation calls

### Integration Testing
- Test graph configurations
- Test deep link handling
- Test backstack operations

### UI Testing
- Test screen transitions
- Test user flows
- Test back button behavior

## Performance Optimization

### Lazy Graph Registration
Graphs created lazily to avoid upfront cost

### State Flow Efficiency
- State updates are batched
- Only changed values trigger recomposition

### Minimal Allocations
- Reuse data structures where possible
- Avoid creating new flows unnecessarily

### No Reflection
All operations are compile-time safe (no runtime reflection)

## Extension Points

The library is designed to be extended:

1. **Custom Transitions**: Implement NavigationTransition
2. **Custom Serializers**: Implement NavigationStateSerializer
3. **Custom Deep Link Handlers**: Implement DeepLinkHandler
4. **Custom Destinations**: Extend Destination interface

## Best Practices

1. ✅ Keep Destinations simple (data only, no logic)
2. ✅ Use sealed classes for related destinations
3. ✅ One graph per feature module
4. ✅ Test navigation with FakeNavigator
5. ✅ Handle deep links early in app lifecycle
6. ✅ Use transitions sparingly (default fade is often enough)
7. ✅ Observe state reactively (don't poll)
8. ✅ Clear backstack judiciously (users expect back to work)
9. ✅ Document public APIs with KDoc
10. ✅ Keep platform-specific code minimal

## Anti-Patterns to Avoid

1. ❌ String-based navigation in application code
2. ❌ Circular navigation dependencies
3. ❌ Storing UI state in Navigator
4. ❌ Blocking operations in navigation callbacks
5. ❌ Exposing mutable state from Navigator
6. ❌ Creating new StateFlows in Composables
7. ❌ Using global singletons for navigation state
8. ❌ Mixing navigation and business logic
