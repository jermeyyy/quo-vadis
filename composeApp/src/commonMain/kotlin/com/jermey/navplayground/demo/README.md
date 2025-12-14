# Navigation Demo Application

This demo application showcases all common navigation patterns using the Quo Vadis Kotlin Multiplatform navigation library with the new **NavNode tree-based architecture**.

## Architecture Overview

The navigation system is built on a **NavNode tree** structure:

- **NavNode**: The base type for all navigation nodes
- **ScreenNode**: Represents a screen/destination in the tree
- **StackNode**: A container for stacked screens (back-stack)
- **TabsNode**: A container for tabbed navigation
- **PaneNode**: A container for split/pane-based navigation

### Core Components

| Component | Purpose |
|-----------|---------|
| `TreeNavigator` | Main navigator that manages the NavNode tree |
| `TreeMutator` | Utility for immutable tree transformations |
| `StateFlow<NavNode>` | Reactive state observation |

## Implemented Patterns

### 1. **Bottom Navigation** (Main View)
- **Location**: `MainScreens.kt`, `BottomNavigationBar.kt`
- **Features**:
  - 4 main tabs: Home, Explore, Profile, Settings
  - Persistent bottom navigation bar
  - Tab selection state management via `StateFlow`
  - Navigation between main sections

**Key Implementation:**
```kotlin
// Observe current destination from navigator's state
val currentDestination by navigator.currentDestination.collectAsState()

// Bottom navigation with 4 tabs
NavigationBar {
    NavigationBarItem(
        icon = Home, 
        selected = currentDestination is MainDestination.Home,
        onClick = { navigator.navigate(MainDestination.Home) }
    )
    // ... other tabs
}
```

### 2. **Master-Detail Navigation**
- **Location**: `Item.kt`, `MasterDetailDestination`
- **Features**:
  - List of 50 items with categories
  - Detail view with specifications
  - Related items navigation
  - Deep navigation between detail screens
  - Horizontal slide transitions

**Key Implementation:**
```kotlin
// Navigate to detail with transition
navigator.navigate(
    MasterDetailDestination.Detail(itemId),
    NavigationTransitions.SlideHorizontal
)

// Navigate to related item (stacking detail views)
onNavigateToRelated = { relatedId ->
    navigator.navigate(MasterDetailDestination.Detail(relatedId))
}
```

**Pattern**: List → Detail → Related Detail → ...

### 3. **Tabs Navigation**
- **Location**: `TabsScreens.kt`, `TabsDestination`
- **Features**:
  - 3 nested tabs with different content
  - Tab-specific content and icons
  - Sub-navigation from tab items
  - Vertical slide transitions for sub-items

**Key Implementation:**
```kotlin
// Switch tabs using navigator
navigator.switchTab(tabIndex)

// Navigate to sub-item with vertical transition
navigator.navigate(
    TabsDestination.SubItem(tabId, itemId),
    NavigationTransitions.SlideVertical
)
```

**Pattern**: Tabs Main → Tab Content → Sub-Item Details

### 4. **Process/Wizard Navigation** (with Branches)
- **Location**: `ProcessScreens.kt`, `ProcessDestination`
- **Features**:
  - Multi-step wizard (4 steps)
  - Branching logic based on user choice
  - Step progress indicator
  - Data passing between steps
  - Stack clearing on completion

**Key Implementation:**
```kotlin
// Branch based on user selection
if (selectedType == "personal") {
    navigator.navigate(ProcessDestination.Step2A(data))
} else {
    navigator.navigate(ProcessDestination.Step2B(data))
}

// Clear stack to specific point after completion
navigator.navigateAndClearTo(
    ProcessDestination.Complete,
    clearRoute = "process_start",
    inclusive = false
)

// Clear all and go home
navigator.navigateAndClearAll(MainDestination.Home)
```

**Pattern**:
```
Start → Step1 → Step2A (personal) → Step3 → Complete
              ↘ Step2B (business) ↗
```

### 5. **Modal/Drawer Navigation**
- **Location**: `DemoApp.kt`
- **Features**:
  - Side drawer with navigation menu
  - Drawer items with icons and selection state
  - Navigate to any major section
  - Drawer auto-closes on navigation

**Key Implementation:**
```kotlin
val currentDestination by navigator.currentDestination.collectAsState()

ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
        NavigationDrawerItem(
            icon = { Icon(...) },
            label = { Text(...) },
            selected = currentDestination is TargetDestination,
            onClick = {
                navigator.navigate(destination)
                scope.launch { drawerState.close() }
            }
        )
    }
) {
    // Main content
}
```

### 6. **State-Driven Navigation Demo**
- **Location**: `ui/screens/statedriven/`
- **Features**:
  - Direct NavNode tree manipulation
  - Real-time state observation via `StateFlow`
  - Declarative navigation without NavController
  - Interactive back-stack editor

**Key Implementation:**
```kotlin
// Create a state-driven back-stack using NavNode tree
class DemoBackStack {
    private val _state = MutableStateFlow<NavNode>(
        StackNode(key = NavKeyGenerator.generate(), parentKey = null, children = emptyList())
    )
    
    val state: StateFlow<NavNode> = _state
    
    // Push using TreeMutator
    fun push(destination: Destination) {
        val newState = TreeMutator.push(_state.value, destination)
        _state.value = newState
    }
    
    // Pop using TreeMutator
    fun pop(): Boolean {
        val newState = TreeMutator.pop(_state.value)
        if (newState != null) {
            _state.value = newState
            return true
        }
        return false
    }
}

// Observe state changes
val currentEntry = backStack.current
val canGoBack = backStack.canNavigateBack
```

## NavNode Tree Structure

The navigation uses a hierarchical **NavNode tree**:

```
StackNode (root)
├── ScreenNode(Home)
├── ScreenNode(Explore)
├── StackNode (Master-Detail)
│   ├── ScreenNode(List)
│   └── ScreenNode(Detail)
├── TabsNode (Tabs)
│   ├── ScreenNode(Tab1)
│   ├── ScreenNode(Tab2)
│   └── ScreenNode(Tab3)
└── StackNode (Process)
    ├── ScreenNode(Start)
    ├── ScreenNode(Step1)
    └── ScreenNode(Complete)
```

## Key Navigation Features Demonstrated

### 1. **State Observation via StateFlow**
```kotlin
// Observe current destination
val currentDestination by navigator.currentDestination.collectAsState()

// Observe full navigation state
val state by navigator.state.collectAsState()

// Observe back navigation capability
val canGoBack by navigator.canNavigateBack.collectAsState()
```

### 2. **Transitions**
```kotlin
NavigationTransitions.SlideHorizontal  // Master-Detail
NavigationTransitions.SlideVertical    // Tabs sub-items
NavigationTransitions.Fade             // Default
```

### 3. **Stack Manipulation via TreeMutator**
```kotlin
// Push a new screen
val newState = TreeMutator.push(currentState, destination)

// Pop the top screen
val newState = TreeMutator.pop(currentState)

// Remove a specific node by key
val newState = TreeMutator.removeNode(currentState, nodeKey)
```

### 4. **Navigator API**
```kotlin
// Clear to specific route
navigator.navigateAndClearTo(dest, clearRoute, inclusive)

// Clear all
navigator.navigateAndClearAll(dest)

// Replace current
navigator.navigateAndReplace(dest)

// Standard navigation
navigator.navigate(dest)
navigator.navigateBack()
```

### 5. **Conditional UI**
```kotlin
// Show bottom nav only on main screens
val currentDest by navigator.currentDestination.collectAsState()
if (shouldShowBottomNav(currentDest)) {
    BottomNavigationBar(...)
}

// Dynamic title based on destination
TopAppBar(title = { Text(getScreenTitle(currentDest)) })
```

### 6. **Data Passing**
```kotlin
// Via destination data classes
data class Detail(val itemId: String) : Destination {
    override val route = "detail/$itemId"
}

// Used directly in screen
@Screen(MasterDetailDestination.Detail::class)
@Composable
fun DetailScreen(destination: MasterDetailDestination.Detail) {
    val itemId = destination.itemId  // Type-safe access
}
```

## Running the Demo

1. Build and run the project
2. The app starts with the Home screen
3. Use bottom navigation to switch between main sections
4. Open the drawer (hamburger menu) to jump to specific patterns
5. Explore each pattern:

### Master-Detail
- Go to Explore tab or use drawer
- Click any item to see details
- Click related items to stack detail views
- Use back button to navigate back through the stack

### Tabs
- Navigate via drawer to "Tabs Example"
- Switch between tabs
- Click items in any tab to see sub-item details
- Back button returns to tabs

### Process Flow
- Navigate via drawer to "Process Flow"
- Start the wizard
- Choose Personal or Business (branches the flow)
- Fill in forms at each step
- Review and complete
- Notice the stack is cleared appropriately

### State-Driven Demo
- Navigate via drawer to "State-Driven Demo"
- See real-time state display at top
- Use the back-stack editor panel to:
  - Push new destinations
  - Pop entries from stack
  - Remove entries at any position
  - Swap entry positions
- Observe how content updates reactively

## Code Organization

```
demo/
├── DemoApp.kt                    # Main app with drawer & bottom nav
├── DemoKoin.kt                   # Dependency injection setup
├── DeepLinkSetup.kt              # Deep link configuration
├── destinations/
│   └── Destinations.kt           # All destination definitions
└── ui/
    ├── BottomNavigationBar.kt    # Bottom nav component
    └── screens/
        ├── MainScreens.kt        # Home, Explore, Profile, Settings
        ├── Item.kt               # Master list & detail views
        ├── TabsScreens.kt        # Tabs & sub-items
        ├── ProcessScreens.kt     # Wizard steps (6 screens)
        └── statedriven/          # State-driven navigation demo
            ├── StateDrivenDemoScreen.kt
            ├── DemoBackStack.kt        # NavNode-based back-stack
            ├── BackstackEditorPanel.kt
            └── ContentScreens.kt
```

## Best Practices Demonstrated

1. ✅ **Type-safe destinations** - Sealed classes with typed data
2. ✅ **NavNode tree structure** - Hierarchical navigation state
3. ✅ **StateFlow observation** - Reactive UI updates
4. ✅ **TreeMutator** - Immutable tree transformations
5. ✅ **Transitions** - Appropriate animations per pattern
6. ✅ **Stack management** - Clear, replace, pop operations
7. ✅ **Data passing** - Type-safe destination arguments
8. ✅ **Conditional UI** - Show/hide based on current destination
9. ✅ **Branching flows** - Process with multiple paths
10. ✅ **Deep navigation** - Stack detail screens

## Extending the Demo

To add a new pattern:

1. Define destinations in `Destinations.kt` as sealed class
2. Create screens in `ui/screens/` with `@Screen` annotation
3. Add navigation setup in `DemoApp.kt`
4. Add drawer/nav item for access

## Testing Navigation

Each pattern can be tested independently:

```kotlin
// Test with TreeNavigator
val navigator = TreeNavigator(
    deepLinkHandler = null,
    coroutineScope = testScope,
    scopeRegistry = ScopeRegistry()
)

// Set initial destination
navigator.setStartDestination(MainDestination.Home)

// Test navigation
navigator.navigate(MasterDetailDestination.Detail("123"))
assertEquals(
    MasterDetailDestination.Detail("123"),
    navigator.currentDestination.value
)

// Test back navigation
navigator.navigateBack()
assertTrue(navigator.canNavigateBack.value)
```
