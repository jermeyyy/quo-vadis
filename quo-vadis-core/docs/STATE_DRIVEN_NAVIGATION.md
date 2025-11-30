# State-Driven Navigation Guide

## Overview

State-driven navigation is an alternative navigation paradigm where **the UI is directly driven by observable backstack state**. Instead of calling imperative navigation methods, you manipulate a state-backed list and the UI automatically reflects the changes.

This approach, inspired by Navigation 3 patterns, provides:

- **Direct State Access**: The backstack is a `SnapshotStateList` that you can observe and manipulate directly
- **Compose-Native Integration**: No flow collection needed - state changes trigger recomposition automatically
- **Flexible Operations**: Insert, remove, swap, and reorder entries anywhere in the stack
- **Predictable Behavior**: The UI always reflects the current state of the backstack

## Comparison with Traditional API

| Aspect | Traditional API | State-Driven API |
|--------|-----------------|------------------|
| **State Container** | `StateFlow<List<BackStackEntry>>` | `SnapshotStateList<BackStackEntry>` |
| **Observation** | Requires `collectAsState()` | Direct property access |
| **Navigation** | Semantic methods (`navigate()`, `navigateBack()`) | Direct list manipulation or wrapper methods |
| **Flexibility** | Predefined operations | Full list manipulation (insert, remove, swap, move) |
| **Use Case** | Standard navigation flows | Complex navigation patterns, state-driven UIs |
| **Testing** | `FakeNavigator` | `FakeStateNavigator` |

### Code Comparison

**Traditional API:**
```kotlin
val navigator = LocalNavigator.current

// Navigate
navigator.navigate(DetailDestination(id = "123"))

// Observe current destination
val currentDestination by navigator.currentDestination.collectAsState(null)
```

**State-Driven API:**
```kotlin
val backStack = rememberStateBackStack(HomeDestination)

// Navigate
backStack.push(DetailDestination(id = "123"))

// Observe current destination (no collection needed!)
val currentDestination = backStack.current?.destination
```

## When to Use State-Driven Navigation

State-driven navigation is ideal for:

### ✅ Complex Navigation Patterns
- Removing arbitrary entries from the middle of the stack
- Reordering entries (e.g., bringing a background screen to front)
- Conditional stack manipulation based on business logic

### ✅ Highly Dynamic UIs
- When navigation state needs to be visible and reactive
- UIs that show multiple entries or navigation previews
- Custom navigation chrome that depends on stack state

### ✅ Testing Requirements
- When you need to verify exact stack state
- Testing complex navigation sequences
- Snapshot testing of navigation state

### ✅ State Management Integration
- When backstack state is part of a larger state management solution
- Integration with state machines or redux-like patterns

### ❌ When Traditional API is Better
- Simple linear navigation flows
- When you don't need direct stack manipulation
- Graph-based navigation with declarative routes

## Core Concepts

### StateBackStack

The foundation of state-driven navigation. It wraps a `SnapshotStateList` from Compose's runtime, enabling automatic recomposition when the backstack changes.

```kotlin
@Stable
class StateBackStack {
    // The underlying state list - observable in Compose
    val entries: SnapshotStateList<BackStackEntry>
    
    // Derived properties (use derivedStateOf for efficiency)
    val current: BackStackEntry?      // Top entry
    val previous: BackStackEntry?     // Second from top
    val canGoBack: Boolean            // Has 2+ entries
    val size: Int
    val isEmpty: Boolean
    val isNotEmpty: Boolean
    
    // Flow for non-Compose consumers
    val entriesFlow: Flow<List<BackStackEntry>>
}
```

**Key Design Decisions:**

1. **`SnapshotStateList`**: Integrates with Compose's snapshot system for automatic recomposition
2. **`derivedStateOf`**: Derived properties only trigger recomposition when their values actually change
3. **`snapshotFlow`**: Bridges to coroutines for non-Compose code (ViewModels, etc.)

### StateNavigator

A higher-level wrapper around `StateBackStack` that provides semantic navigation methods while maintaining full state observability.

```kotlin
@Stable
class StateNavigator(
    private val backStack: StateBackStack = StateBackStack()
) {
    // Observable state
    val entries: SnapshotStateList<BackStackEntry>
    val currentDestination: Destination?
    val previousDestination: Destination?
    val canGoBack: Boolean
    val currentEntry: BackStackEntry?
    val stackSize: Int
    
    // Navigation methods
    fun navigate(destination: Destination, transition: NavigationTransition? = null)
    fun navigateBack(): Boolean
    fun navigateAndReplace(destination: Destination, transition: NavigationTransition? = null)
    fun navigateAndClearAll(destination: Destination, transition: NavigationTransition? = null)
    fun clear()
    
    // Access underlying backstack for advanced operations
    fun getBackStack(): StateBackStack
}
```

### StateNavHost

A Compose host that renders destinations based on the current `StateBackStack` state.

```kotlin
@Composable
fun StateNavHost(
    stateBackStack: StateBackStack,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<BackStackEntry>.() -> ContentTransform,
    entryProvider: @Composable AnimatedContentScope.(Destination) -> Unit
)
```

**Features:**
- Uses `AnimatedContent` for smooth transitions
- `SaveableStateHolder` preserves screen state across configuration changes
- Entry ID used as content key for correct animation targeting

## Basic Usage

### Setting Up with StateBackStack

```kotlin
@Composable
fun App() {
    // Create a remembered backstack with initial destination
    val backStack = rememberStateBackStack(HomeDestination)
    
    StateNavHost(
        stateBackStack = backStack,
        modifier = Modifier.fillMaxSize()
    ) { destination ->
        when (destination) {
            is HomeDestination -> HomeScreen(
                onNavigateToDetail = { id -> 
                    backStack.push(DetailDestination(id)) 
                }
            )
            is DetailDestination -> DetailScreen(
                id = destination.id,
                onBack = { backStack.pop() }
            )
            is SettingsDestination -> SettingsScreen(
                onBack = { backStack.pop() }
            )
        }
    }
}
```

### Setting Up with StateNavigator

```kotlin
@Composable
fun App() {
    // Create a remembered navigator with initial destination
    val navigator = rememberStateNavigator(HomeDestination)
    
    StateNavHost(
        stateBackStack = navigator.getBackStack(),
        modifier = Modifier.fillMaxSize()
    ) { destination ->
        when (destination) {
            is HomeDestination -> HomeScreen(
                onNavigateToDetail = { id -> 
                    navigator.navigate(DetailDestination(id)) 
                }
            )
            is DetailDestination -> DetailScreen(
                id = destination.id,
                onBack = { navigator.navigateBack() }
            )
            is SettingsDestination -> SettingsScreen(
                onBack = { navigator.navigateBack() },
                onLogout = {
                    // Clear all and go to login
                    navigator.navigateAndClearAll(LoginDestination)
                }
            )
        }
    }
}
```

### Helper Functions

```kotlin
// Create a remembered StateBackStack
@Composable
fun rememberStateBackStack(initialDestination: Destination? = null): StateBackStack

// Create a remembered StateNavigator
@Composable
fun rememberStateNavigator(initialDestination: Destination? = null): StateNavigator
```

## Advanced Operations

The real power of state-driven navigation comes from direct backstack manipulation.

### Direct List Manipulation

```kotlin
val backStack = rememberStateBackStack(HomeDestination)

// Direct access to the SnapshotStateList
val entries = backStack.entries

// Add entries
entries.add(BackStackEntry.create(DetailDestination("1")))
entries.add(0, BackStackEntry.create(RootDestination)) // Insert at bottom

// Remove entries
entries.removeAt(entries.lastIndex)
entries.removeFirst()
```

### Removing Arbitrary Entries

```kotlin
// Remove by index
backStack.removeAt(1)  // Remove second entry

// Remove by ID
backStack.removeById("entry-uuid-123")

// Remove entries matching a condition
backStack.entries.removeAll { entry ->
    entry.destination is TemporaryDestination
}
```

### Reordering Entries

```kotlin
// Swap two entries
backStack.swap(0, 2)  // Swap first and third entries

// Move an entry to a new position
backStack.move(fromIndex = 0, toIndex = 3)  // Move first to fourth position

// Bring a background entry to the front
val targetIndex = backStack.entries.indexOfFirst { 
    it.destination is ImportantDestination 
}
if (targetIndex >= 0) {
    backStack.move(targetIndex, backStack.entries.lastIndex)
}
```

### Inserting at Positions

```kotlin
// Insert at specific position
backStack.insert(1, MiddleDestination)  // Insert as second entry

// Insert before current
backStack.insert(backStack.size - 1, PreviewDestination)

// Insert at bottom (as root)
backStack.insert(0, NewRootDestination)
```

### Bulk Operations

```kotlin
// Replace entire backstack with destinations
backStack.replaceAll(listOf(
    HomeDestination,
    CategoryDestination("electronics"),
    ProductDestination("laptop-123")
))

// Replace with existing entries (preserves IDs)
backStack.replaceAllWithEntries(savedEntries)

// Clear and start fresh
backStack.clear()
backStack.push(LoginDestination)
```

## Observing State

### In Compose (Direct Observation)

Since `StateBackStack` uses Compose's snapshot system, observation is automatic:

```kotlin
@Composable
fun NavigationInfo(backStack: StateBackStack) {
    // These will trigger recomposition when they change
    val current = backStack.current
    val canGoBack = backStack.canGoBack
    val stackSize = backStack.size
    
    Column {
        Text("Current: ${current?.destination?.route}")
        Text("Stack size: $stackSize")
        Text("Can go back: $canGoBack")
        
        // Observe the full list
        backStack.entries.forEach { entry ->
            Text("- ${entry.destination.route}")
        }
    }
}
```

### In Non-Compose Code (Using entriesFlow)

For ViewModels or other non-Compose consumers:

```kotlin
class NavigationViewModel(
    private val backStack: StateBackStack
) : ViewModel() {
    
    // Observe navigation state
    val navigationState = backStack.entriesFlow
        .map { entries ->
            NavigationUiState(
                currentRoute = entries.lastOrNull()?.destination?.route,
                canGoBack = entries.size > 1,
                stackDepth = entries.size
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NavigationUiState()
        )
    
    // React to navigation changes
    init {
        viewModelScope.launch {
            backStack.entriesFlow.collect { entries ->
                // Log navigation changes
                analytics.logNavigationState(entries.map { it.destination.route })
            }
        }
    }
}
```

## Testing with FakeStateNavigator

`FakeStateNavigator` records all navigation operations and provides assertion helpers for comprehensive testing.

### Basic Testing

```kotlin
@Test
fun `navigate to detail screen`() {
    val navigator = FakeStateNavigator()
    
    // Perform navigation
    navigator.navigate(HomeDestination)
    navigator.navigate(DetailDestination(id = "123"))
    
    // Verify using assertion helpers
    navigator.assertNavigatedTo("detail")
    navigator.assertStackSize(2)
    navigator.assertCurrentDestination<DetailDestination>()
}

@Test
fun `navigate back removes top entry`() {
    val navigator = FakeStateNavigator()
    navigator.navigate(HomeDestination)
    navigator.navigate(DetailDestination(id = "123"))
    
    navigator.navigateBack()
    
    navigator.assertStackSize(1)
    navigator.assertCurrentDestination<HomeDestination>()
}
```

### Verification Methods

```kotlin
// Verify by route
navigator.verifyNavigatedTo("detail")          // Returns Boolean
navigator.assertNavigatedTo("detail")          // Throws on failure

// Verify by type
navigator.verifyNavigatedToType<DetailDestination>()
navigator.assertCurrentDestination<DetailDestination>()

// Verify back navigation
navigator.verifyNavigatedBack()
navigator.verifyNavigatedBack(successOnly = true)

// Verify stack state
navigator.verifyStackSize(expectedSize = 3)
navigator.verifyCurrentRoute("home")
```

### Operation Counting

```kotlin
// Count specific operations
val navigateCount = navigator.getNavigateCallCount("detail")
val backCount = navigator.getOperationCount<StateNavigationOperation.NavigateBack>()

// Access all recorded operations
navigator.navigationOperations.forEach { operation ->
    when (operation) {
        is StateNavigationOperation.Navigate -> 
            println("Navigate to: ${operation.destination.route}")
        is StateNavigationOperation.NavigateBack -> 
            println("Navigate back (success: ${operation.success})")
        // ... other operations
    }
}
```

### Test DSL Builder

```kotlin
@Test
fun `completing checkout clears cart screens`() {
    stateNavigationTest {
        // Set up initial state
        given {
            navigate(HomeDestination)
            navigate(CartDestination)
            navigate(CheckoutDestination)
        }
        
        // Perform the action
        `when` {
            navigateAndClearAll(OrderConfirmationDestination)
        }
        
        // Verify results
        then {
            assertStackSize(1)
            assertCurrentDestination<OrderConfirmationDestination>()
        }
    }
}
```

### Testing Direct Backstack Operations

```kotlin
@Test
fun `remove intermediate entry`() {
    val navigator = FakeStateNavigator()
    val backStack = navigator.getBackStack()
    
    // Setup
    backStack.push(HomeDestination)
    backStack.push(DetailDestination("1"))
    backStack.push(DetailDestination("2"))
    backStack.push(DetailDestination("3"))
    
    // Remove second entry
    backStack.removeAt(1)
    
    // Verify
    assertEquals(3, backStack.size)
    assertEquals(
        listOf("home", "detail", "detail"),
        backStack.entries.map { it.destination.route }
    )
}
```

## Best Practices

### 1. Choose the Right Abstraction Level

```kotlin
// For simple apps: Use StateNavigator
val navigator = rememberStateNavigator(HomeDestination)
navigator.navigate(DetailDestination(id))

// For complex manipulation: Use StateBackStack directly
val backStack = rememberStateBackStack(HomeDestination)
backStack.insert(1, MiddleDestination)
```

### 2. Prefer Helper Methods Over Direct List Access

```kotlin
// ✅ Preferred - Uses typed helpers
backStack.push(destination)
backStack.removeAt(index)
backStack.swap(a, b)

// ⚠️ Use carefully - Direct list access
backStack.entries.add(BackStackEntry.create(destination))
backStack.entries.removeAt(index)
```

### 3. Use entriesFlow for Side Effects

```kotlin
// ✅ Good - Side effects in LaunchedEffect with flow
LaunchedEffect(backStack) {
    backStack.entriesFlow.collect { entries ->
        analytics.trackNavigation(entries.last().destination.route)
    }
}

// ⚠️ Avoid - Side effects during composition
val current = backStack.current
analytics.trackNavigation(current?.destination?.route) // Don't do this!
```

### 4. Handle Empty State

```kotlin
@Composable
fun SafeStateNavHost(backStack: StateBackStack) {
    val current = backStack.current
    
    if (current != null) {
        StateNavHost(stateBackStack = backStack) { destination ->
            // Render destination
        }
    } else {
        // Handle empty backstack
        LoadingScreen()
    }
}
```

### 5. Use derivedStateOf for Computed Values

```kotlin
@Composable
fun NavigationBar(backStack: StateBackStack) {
    // ✅ Good - Only recomposes when canGoBack actually changes
    val showBackButton by remember {
        derivedStateOf { backStack.canGoBack }
    }
    
    // ⚠️ Less efficient - Recomposes on any entries change
    val showBackButtonInefficient = backStack.entries.size > 1
}
```

### 6. Scope State Appropriately

```kotlin
// ✅ Good - Backstack scoped to the navigation host
@Composable
fun AppNavigation() {
    val backStack = rememberStateBackStack(HomeDestination)
    StateNavHost(stateBackStack = backStack) { /* ... */ }
}

// ⚠️ Avoid - Backstack in global state (unless intentional)
object GlobalNavigation {
    val backStack = StateBackStack() // Usually not what you want
}
```

## Migration Guide

### From Traditional Navigator

**Before (Traditional API):**
```kotlin
@Composable
fun App() {
    val navController = rememberNavController()
    
    NavigationGraph(navigator = navController) {
        destination(HomeDestination) { HomeScreen() }
        destination(DetailDestination) { DetailScreen() }
    }
}

// Navigation calls
navigator.navigate(DetailDestination(id))
navigator.navigateBack()
```

**After (State-Driven API):**
```kotlin
@Composable
fun App() {
    val backStack = rememberStateBackStack(HomeDestination)
    
    StateNavHost(stateBackStack = backStack) { destination ->
        when (destination) {
            is HomeDestination -> HomeScreen()
            is DetailDestination -> DetailScreen()
        }
    }
}

// Navigation calls
backStack.push(DetailDestination(id))
backStack.pop()
```

### Step-by-Step Migration

1. **Replace NavController with StateBackStack/StateNavigator**
   ```kotlin
   // Old
   val navController = rememberNavController()
   
   // New
   val backStack = rememberStateBackStack(HomeDestination)
   // or
   val navigator = rememberStateNavigator(HomeDestination)
   ```

2. **Replace NavHost with StateNavHost**
   ```kotlin
   // Old
   NavHost(navController = navController, startDestination = "home") {
       composable("home") { HomeScreen() }
   }
   
   // New
   StateNavHost(stateBackStack = backStack) { destination ->
       when (destination) {
           is HomeDestination -> HomeScreen()
       }
   }
   ```

3. **Update Navigation Calls**
   ```kotlin
   // Old
   navController.navigate("detail/$id")
   navController.popBackStack()
   
   // New (StateBackStack)
   backStack.push(DetailDestination(id))
   backStack.pop()
   
   // New (StateNavigator)
   navigator.navigate(DetailDestination(id))
   navigator.navigateBack()
   ```

4. **Update State Observation**
   ```kotlin
   // Old
   val currentRoute = navController.currentBackStackEntry
       ?.destination?.route
   
   // New
   val currentDestination = backStack.current?.destination
   ```

5. **Update Tests**
   ```kotlin
   // Old
   val fakeNavigator = FakeNavigator()
   
   // New
   val fakeNavigator = FakeStateNavigator()
   ```

### Gradual Migration

You can use both APIs in the same app during migration:

```kotlin
@Composable
fun App() {
    // New state-driven navigation for new features
    val stateBackStack = rememberStateBackStack()
    
    // Existing traditional navigation (during migration)
    val legacyNavigator = LocalNavigator.current
    
    // Coordinate between them as needed
}
```

## API Reference Summary

### StateBackStack

| Method/Property | Description |
|----------------|-------------|
| `entries` | The `SnapshotStateList<BackStackEntry>` |
| `current` | Current (top) entry |
| `previous` | Previous entry |
| `canGoBack` | Whether back navigation is possible |
| `entriesFlow` | Flow for non-Compose observation |
| `push(destination, transition?)` | Add to top |
| `pop()` | Remove from top |
| `removeAt(index)` | Remove at position |
| `removeById(id)` | Remove by entry ID |
| `insert(index, destination, transition?)` | Insert at position |
| `swap(indexA, indexB)` | Swap two entries |
| `move(from, to)` | Move entry to new position |
| `clear()` | Remove all entries |
| `replaceAll(destinations)` | Replace with new destinations |
| `replaceAllWithEntries(entries)` | Replace with existing entries |

### StateNavigator

| Method/Property | Description |
|----------------|-------------|
| `entries` | Access to the backstack entries |
| `currentDestination` | Current destination |
| `previousDestination` | Previous destination |
| `canGoBack` | Whether back navigation is possible |
| `currentEntry` | Current backstack entry |
| `stackSize` | Number of entries |
| `navigate(destination, transition?)` | Push new destination |
| `navigateBack()` | Pop current destination |
| `navigateAndReplace(destination, transition?)` | Replace current |
| `navigateAndClearAll(destination, transition?)` | Clear and navigate |
| `clear()` | Clear all entries |
| `getBackStack()` | Access underlying StateBackStack |

### FakeStateNavigator

| Method | Description |
|--------|-------------|
| `navigationOperations` | List of recorded operations |
| `clearOperations()` | Clear operation history |
| `reset()` | Clear operations and backstack |
| `verifyNavigatedTo(route)` | Check if navigated to route |
| `verifyNavigatedToType<T>()` | Check if navigated to type |
| `verifyNavigatedBack(successOnly?)` | Check if navigated back |
| `verifyStackSize(size)` | Check stack size |
| `verifyCurrentDestination<T>()` | Check current destination type |
| `assertNavigatedTo(route)` | Assert navigation occurred |
| `assertStackSize(size)` | Assert stack size |
| `assertCurrentDestination<T>()` | Assert current destination |
| `getNavigateCallCount(route)` | Count navigations to route |
| `getOperationCount<T>()` | Count operations of type |

## See Also

- [Architecture Overview](ARCHITECTURE.md) - Overall library architecture
- [Navigation Implementation](NAVIGATION_IMPLEMENTATION.md) - Core navigation details
- [Typed Destinations](TYPED_DESTINATIONS.md) - Type-safe destination definitions
- [Tab Navigation](TAB_NAVIGATION.md) - Tab-based navigation patterns
