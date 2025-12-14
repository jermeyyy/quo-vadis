# Code Style and Conventions - Quo Vadis Navigation Library

## Naming Conventions

### Classes & Interfaces
**PascalCase** - First letter of each word capitalized
- `Navigator`, `NavNode`, `Destination`
- `PredictiveBackNavigation`, `ComposableCache`
- `NavigationGraph`, `DeepLinkHandler`

### Functions & Methods
**camelCase** - First word lowercase, subsequent words capitalized
- `navigate()`, `navigateBack()`, `navigateAndClearTo()`
- `lockEntry()`, `unlockEntry()`
- `startAnimation()`, `finishAnimation()`, `cancelAnimation()`

### Properties & Variables
**camelCase**
- `currentDestination`, `backStack`, `canGoBack`
- `gestureProgress`, `exitProgress`, `isAnimating`
- `displayedCurrentEntry`, `displayedPreviousEntry`

### Constants
**SCREAMING_SNAKE_CASE** - All uppercase with underscores
- `DEFAULT_MAX_CACHE_SIZE`
- `ANIMATION_DURATION_MS`

### Destinations
**PascalCase** with `Destination` suffix
- `HomeDestination`, `DetailsDestination`
- `ProfileDestination`, `SettingsDestination`

### Test Fakes
**`Fake` prefix**
- `FakeNavigator`, `FakeNavNode`
- `FakeDeepLinkHandler`

### Default Implementations
**`Default` prefix**
- `DefaultNavigator`, `DefaultDeepLinkHandler`
- `DefaultStateSerializer`

### Animation Modifiers
**camelCase** with descriptive suffix
- `material3BackAnimation()`, `material3ExitAnimation()`
- `scaleBackAnimation()`, `scaleExitAnimation()`
- `slideBackAnimation()`, `slideExitAnimation()`

## Package Structure

### Core Library (quo-vadis-core)
```
com.jermey.quo.vadis.core.navigation/
├── core/          - Core navigation components
├── compose/       - Compose UI integration
├── integration/   - DI framework integration
├── testing/       - Testing utilities
├── utils/         - Extension functions
└── serialization/ - State save/restore
```

For MVI architecture, use the separate `quo-vadis-core-flow-mvi` module.

### Demo App (composeApp)
```
com.jermey.navplayground/
├── demo/          - Demo application
│   ├── destinations/  - All destination definitions
│   ├── graphs/        - Navigation graph definitions
│   └── ui/screens/    - Demo screens
└── App.kt         - Main entry point
```

## Documentation Standards

### KDoc Format
All public APIs MUST have KDoc documentation:

```kotlin
/**
 * Brief one-line description.
 *
 * More detailed explanation if needed. Can span multiple
 * paragraphs and include examples.
 *
 * @param navigator The navigation controller
 * @param graph The navigation graph containing screen definitions
 * @return The navigation state
 * @throws NavigationException if navigation fails
 */
```

### File Headers
No file headers required, but package declarations must be first:
```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import ...
```

### Inline Comments
Use inline comments for complex logic:
```kotlin
// Gesture completed - animate exit
isGesturing = false

// Lock cache entries to prevent premature destruction
coordinator.startAnimation(currentEntry, previousEntry)
```

## Kotlin Features & Idioms

### Sealed Classes for Destination Hierarchies
```kotlin
sealed class FeatureDestination : Destination {
    object Home : FeatureDestination()
    data class Details(val id: String) : FeatureDestination()
}
```

### Data Classes for Destinations with Arguments
```kotlin
data class UserDestination(val userId: String) : Destination {
    override val route = "user"
    override val arguments = mapOf("userId" to userId)
}
```

### Extension Functions in Utils
```kotlin
fun Navigator.navigateIfNotCurrent(destination: Destination) {
    if (currentDestination.value?.route != destination.route) {
        navigate(destination)
    }
}
```

### DSL Builders with Lambda Receivers
```kotlin
fun navigationGraph(
    route: String,
    builder: NavigationGraphBuilder.() -> Unit
): NavigationGraph
```

### StateFlow over Callbacks
```kotlin
// ✅ Good - reactive state
val currentDestination: StateFlow<Destination?>

// ❌ Bad - callbacks
fun setNavigationListener(listener: (Destination) -> Unit)
```

### Explicit Null Safety
```kotlin
// ✅ Good - explicit handling
val currentNode by navigator.state.collectAsState()
currentNode?.let { handleNode(it) }

// ❌ Bad - nullable in public API when not needed
fun navigate(destination: Destination?) // Should not be nullable
```

## Compose Conventions

### Composable Naming
**PascalCase** for composables:
```kotlin
@Composable
fun NavHost(navigator: Navigator) { }

@Composable
fun PredictiveBackNavigation(navigator: Navigator) { }
```

### Modifier Parameter
**Last parameter** with default value:
```kotlin
@Composable
fun NavHost(
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier  // Last, with default
)
```

### State Hoisting
Hoist state at appropriate level:
```kotlin
// State in calling composable
var selectedTab by remember { mutableStateOf(0) }
TabRow(selectedTab) { ... }

// State in ViewModel for business logic
val uiState by viewModel.state.collectAsState()
```

### Remember for Recomposition Stability
```kotlin
val coordinator = remember { PredictiveBackAnimationCoordinator() }
val cache = rememberComposableCache(maxCacheSize)
val navigator = rememberNavigator()
```

### LaunchedEffect for Side Effects
```kotlin
LaunchedEffect(coordinator.isAnimating) {
    if (coordinator.isAnimating) {
        cache.lockEntry(entryId)
    }
}
```

## Animation & Graphics

### graphicsLayer for Performance
```kotlin
Modifier.graphicsLayer {
    scaleX = scale
    scaleY = scale
    alpha = alphaValue
    translationX = offsetX
}
```

### Z-Index Layering
```kotlin
Box(Modifier.zIndex(1f)) { /* Current screen */ }
Box(Modifier.zIndex(0.5f)) { /* Scrim */ }
Box(Modifier.zIndex(0f)) { /* Previous screen */ }
```

## State Management

### Private Mutable, Public Immutable
```kotlin
class Navigator {
    private val _currentDestination = MutableStateFlow<Destination?>(null)
    val currentDestination: StateFlow<Destination?> = _currentDestination.asStateFlow()
}
```

### StateFlow for State, SharedFlow for Events
```kotlin
// State - has current value
val navigationState: StateFlow<NavigationState>

// Events - one-time occurrences
val navigationEffects: SharedFlow<NavigationEffect>
```

## Error Handling

### Nullable Returns for Failure
```kotlin
fun navigateBack(): Boolean  // Returns false if can't go back
fun pop(): Boolean  // Returns false if stack empty
```

### Exceptions for Critical Errors
```kotlin
class NavigationException(message: String) : Exception(message)

throw NavigationException("Cannot navigate to null destination")
```

## Testing Conventions

### Test Naming
```kotlin
@Test
fun `navigate to details screen adds to backstack`() { }

@Test
fun `back navigation removes from backstack`() { }

@Test
fun `predictive back animation completes before navigation`() { }
```

### Use FakeNavigator
```kotlin
val navigator = FakeNavigator()
viewModel.onItemClick("123")
assertTrue(navigator.verifyNavigateTo("details"))
```

## Code Organization

### File Structure Order
1. Package declaration
2. Imports (organized)
3. File-level documentation
4. Constants
5. Interfaces/sealed classes
6. Main classes/functions
7. Private helper classes/functions
8. Extension functions

### Class Member Order
1. Companion object
2. Properties (public then private)
3. Constructor
4. Public methods
5. Internal methods
6. Private methods
7. Nested classes/interfaces

## Anti-Patterns to Avoid

### ❌ String-Based Navigation
```kotlin
// Bad
navigator.navigate("details")

// Good
navigator.navigate(DetailsDestination)
```

### ❌ Mutable State in Public API
```kotlin
// Bad
val currentDestination: MutableStateFlow<Destination?>

// Good
val currentDestination: StateFlow<Destination?>
```

### ❌ Blocking Operations
```kotlin
// Bad
fun navigate(destination: Destination) {
    Thread.sleep(100)  // Never block
}

// Good
suspend fun navigate(destination: Destination) {
    delay(100)  // Use coroutines
}
```

### ❌ Creating StateFlows in Composables
```kotlin
// Bad
@Composable
fun MyScreen() {
    val state = MutableStateFlow(0)  // Recreated on recomposition!
}

// Good
@Composable
fun MyScreen() {
    val state = remember { MutableStateFlow(0) }
}
```

## Markdown Documentation Style

### Headers
n
# Main Title (H1) - Only one per file
## Section (H2) - Major sections
### Subsection (H3) - Minor sections
```

### Code Blocks
Use language-specific code fencing:
n
```kotlin
fun example() { }
```
```

### Lists
n
- Unordered list
- Another item

1. Ordered list
2. Another item
```

### Emphasis
n
**Bold** for important terms
*Italic* for emphasis
`code` for inline code
```