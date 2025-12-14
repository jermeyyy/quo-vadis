# State-Driven Navigation Guide

## Overview

State-driven navigation is the core paradigm of Quo Vadis where **the UI is directly driven by observable NavNode tree state**. The navigation state is represented as an immutable `NavNode` tree, and all mutations are performed through `TreeMutator` operations.

This approach provides:

- **Immutable State**: The NavNode tree is immutable - all changes create new trees
- **Observable via StateFlow**: `Navigator.state: StateFlow<NavNode>` for reactive updates
- **Pure Functional Mutations**: `TreeMutator` operations are pure functions
- **Hierarchical Structure**: StackNode, TabNode, PaneNode for complex navigation patterns
- **Predictable Behavior**: The UI always reflects the current state of the NavNode tree

## NavNode Tree Architecture

The navigation state is represented as a tree of `NavNode` objects:

```kotlin
sealed interface NavNode {
    val key: String
    val parentKey: String?
}

// Leaf node for a single destination
data class ScreenNode(
    override val key: String,
    val destination: Destination
) : NavNode

// Stack container (back-stack)
data class StackNode(
    override val key: String,
    val children: List<NavNode>
) : NavNode

// Tab container
data class TabNode(
    override val key: String,
    val children: List<StackNode>,
    val activeIndex: Int
) : NavNode

// Pane container (adaptive layouts)
data class PaneNode(
    override val key: String,
    val paneConfigurations: Map<PaneRole, PaneContent>
) : NavNode
```

## TreeMutator - Pure Functional State Transformations

All navigation state mutations are performed through `TreeMutator`, which provides pure functional operations:

```kotlin
// Create a navigator
val navigator = rememberNavigator(startDestination = HomeDestination)

// Navigate using Navigator methods (uses TreeMutator internally)
navigator.navigate(DetailDestination(id = "123"))
navigator.navigateBack()

// Or use TreeMutator directly for advanced scenarios
val currentState = navigator.state.value
val newState = TreeMutator.push(currentState, DetailDestination(id = "123"))
navigator.updateState(newState)
```

### Key TreeMutator Operations

```kotlin
object TreeMutator {
    // Stack operations
    fun push(root: NavNode, destination: Destination): NavNode
    fun pop(root: NavNode): NavNode?
    fun replaceCurrent(root: NavNode, destination: Destination): NavNode
    fun clearAndPush(root: NavNode, destination: Destination): NavNode
    
    // Navigation queries
    fun canGoBack(root: NavNode): Boolean
    fun currentDestination(root: NavNode): Destination?
    
    // Tab operations
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode
    
    // Pane operations
    fun navigateToPane(root: NavNode, role: PaneRole, destination: Destination): NavNode
    fun switchActivePane(root: NavNode, paneKey: String, role: PaneRole): NavNode
    
    // Back handling
    fun popWithTabBehavior(root: NavNode): BackResult
    fun canHandleBackNavigation(root: NavNode): Boolean
}
```

## Observing Navigation State

### In Compose (Direct Observation)

```kotlin
@Composable
fun NavigationInfo(navigator: Navigator) {
    val navState by navigator.state.collectAsState()
    val currentDest by navigator.currentDestination.collectAsState()
    val canGoBack by navigator.canNavigateBack.collectAsState()
    
    Column {
        Text("Current: ${currentDest?.route}")
        Text("Can go back: $canGoBack")
        Text("Tree depth: ${navState.depth()}")
        
        // List all screens in the tree
        navState.allScreens().forEach { screen ->
            Text("- ${screen.destination.route}")
        }
    }
}
```

### In Non-Compose Code (ViewModel)

```kotlin
class NavigationViewModel(
    private val navigator: Navigator
) : ViewModel() {
    
    val navigationState = navigator.state
        .map { navNode ->
            NavigationUiState(
                currentRoute = navNode.activeLeaf()?.destination?.route,
                canGoBack = TreeMutator.canGoBack(navNode),
                stackDepth = navNode.depth()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NavigationUiState()
        )
}
```

## NavNode State Benefits

| Aspect | Description |
|--------|-------------|
| **State Container** | `StateFlow<NavNode>` - immutable tree |
| **Observation** | `collectAsState()` or `collect()` |
| **Mutations** | Pure functions via `TreeMutator` |
| **Hierarchy** | StackNode, TabNode, PaneNode support |
| **Animations** | Full support |
| **Predictive Back** | Full support with cascade |
| **Serialization** | Full tree serialization via NavNodeSerializer |

### Code Comparison

**NavNode Tree Observation:**
```kotlin
val navigator = rememberNavigator(startDestination = HomeDestination)

// Observe NavNode tree state
val navState by navigator.state.collectAsState()
val currentDest by navigator.currentDestination.collectAsState()

// Navigate using Navigator methods
navigator.navigate(DetailDestination(id = "123"))
navigator.navigateBack()

// Or use TreeMutator for direct state manipulation
val newState = TreeMutator.push(navState, AnotherDestination)
navigator.updateState(newState)
```

## When to Use TreeMutator Directly

TreeMutator operations are useful for:

### ✅ Complex Navigation Patterns
- Custom navigation flows not covered by Navigator methods
- Conditional navigation based on tree analysis
- Advanced state restoration scenarios

### ✅ State Analysis
- Querying the current navigation tree
- Finding specific nodes by key or route
- Calculating navigation depth and paths

### ✅ Testing
- Unit testing navigation logic without UI
- Creating specific navigation states for snapshot tests
- Verifying tree structure after operations

### ❌ When Navigator Methods are Better
- Simple linear navigation flows
- Standard push/pop operations
- When you don't need direct tree manipulation

## NavNode Tree Extensions

Useful extension functions for working with NavNode trees:

```kotlin
// Find nodes
fun NavNode.findByKey(key: String): NavNode?
fun NavNode.findByRoute(route: String): ScreenNode?

// Tree traversal
fun NavNode.activePathToLeaf(): List<NavNode>
fun NavNode.activeLeaf(): ScreenNode?
fun NavNode.activeStack(): StackNode?
fun NavNode.allScreens(): List<ScreenNode>

// Tree analysis
fun NavNode.depth(): Int
fun NavNode.nodeCount(): Int
fun NavNode.canHandleBackInternally(): Boolean
fun NavNode.containsRoute(route: String): Boolean
val NavNode.routes: List<String>
```

## State Serialization

NavNode trees can be fully serialized for state persistence:

```kotlin
// Serialize navigation state
val json = NavNodeSerializer.toJson(navigator.state.value)

// Restore navigation state
val restoredState = NavNodeSerializer.fromJson(json)
navigator.updateState(restoredState)

// Safe restoration with fallback
val state = NavNodeSerializer.fromJsonOrNull(savedJson) 
    ?: createDefaultState()
```

---

## Testing with FakeNavigator

`FakeNavigator` provides testing utilities for navigation verification:

```kotlin
@Test
fun `navigate to detail screen`() {
    val navigator = FakeNavigator()
    
    // Perform navigation
    navigator.navigate(HomeDestination)
    navigator.navigate(DetailDestination(id = "123"))
    
    // Verify using assertion helpers
    assertTrue(navigator.verifyNavigateTo("detail"))
    assertEquals(2, navigator.state.value.allScreens().size)
}
```

---

## See Also

- [Architecture Overview](ARCHITECTURE.md) - Overall library architecture
- [Navigation Implementation](NAVIGATION_IMPLEMENTATION.md) - Core navigation details
- [API Reference](API_REFERENCE.md) - Complete API documentation
- [Tab Navigation](TAB_NAVIGATION.md) - Tab-based navigation patterns
    
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
