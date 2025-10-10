# Navigation Demo Application

This demo application showcases all common navigation patterns using our custom Kotlin Multiplatform navigation library.

## Implemented Patterns

### 1. **Bottom Navigation** (Main View)
- **Location**: `MainScreens.kt`, `BottomNavigationBar.kt`
- **Features**:
  - 4 main tabs: Home, Explore, Profile, Settings
  - Persistent bottom navigation bar
  - Tab selection state management
  - Navigation between main sections

**Key Implementation:**
```kotlin
// Bottom navigation with 4 tabs
NavigationBar {
    NavigationBarItem(icon = Home, selected = currentRoute == "home", ...)
    NavigationBarItem(icon = Explore, ...)
    NavigationBarItem(icon = Profile, ...)
    NavigationBarItem(icon = Settings, ...)
}
```

### 2. **Master-Detail Navigation**
- **Location**: `MasterDetailScreens.kt`, `MasterDetailDestination`
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
// Main tabs container
TabRow(selectedTabIndex = selectedTabIndex) {
    Tab(selected = ..., text = "Tab 1")
    Tab(selected = ..., text = "Tab 2")
    Tab(selected = ..., text = "Tab 3")
}

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
ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
        NavigationDrawerItem(
            icon = { Icon(...) },
            label = { Text(...) },
            selected = currentRoute == route,
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

## Navigation Graph Structure

The demo uses a hierarchical graph structure:

```
Main Graph (mainBottomNavGraph)
├── Home
├── Explore
├── Profile
├── Settings
├── Master-Detail Graph (included)
│   ├── List
│   └── Detail
├── Tabs Graph (included)
│   ├── Main
│   └── SubItem
└── Process Graph (included)
    ├── Start
    ├── Step1
    ├── Step2A / Step2B
    ├── Step3
    └── Complete
```

## Key Navigation Features Demonstrated

### 1. **State Management**
```kotlin
val currentRoute by navigator.currentDestination.collectAsState()
```

### 2. **Transitions**
```kotlin
NavigationTransitions.SlideHorizontal  // Master-Detail
NavigationTransitions.SlideVertical    // Tabs sub-items
NavigationTransitions.Fade             // Default
```

### 3. **Backstack Manipulation**
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

### 4. **Conditional UI**
```kotlin
// Show bottom nav only on main screens
if (shouldShowBottomNav(currentRoute)) {
    BottomNavigationBar(...)
}

// Dynamic title based on route
TopAppBar(title = { Text(getScreenTitle(currentRoute)) })
```

### 5. **Data Passing**
```kotlin
// Via destination arguments
data class Detail(val itemId: String) : Destination {
    override val arguments = mapOf("itemId" to itemId)
}

// Retrieve in screen
val itemId = dest.arguments["itemId"] as? String
```

### 6. **Graph Inclusion**
```kotlin
fun mainBottomNavGraph() = navigationGraph("main") {
    // Main destinations
    destination(Home) { ... }
    
    // Include other graphs
    include(masterDetailGraph())
    include(tabsGraph())
    include(processGraph())
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

### Modal Drawer
- Open drawer from any screen
- Navigate to different sections
- Drawer closes automatically
- Current section is highlighted

## Code Organization

```
demo/
├── DemoApp.kt                    # Main app with drawer & bottom nav
├── destinations/
│   └── Destinations.kt           # All destination definitions
├── graphs/
│   └── NavigationGraphs.kt       # All navigation graphs
└── ui/
    ├── BottomNavigationBar.kt    # Bottom nav component
    └── screens/
        ├── MainScreens.kt        # Home, Explore, Profile, Settings
        ├── MasterDetailScreens.kt # Master list & detail views
        ├── TabsScreens.kt        # Tabs & sub-items
        └── ProcessScreens.kt     # Wizard steps (6 screens)
```

## Best Practices Demonstrated

1. ✅ **Type-safe destinations** - Sealed classes with data
2. ✅ **Modular graphs** - Separate graph per feature
3. ✅ **Graph composition** - Include nested graphs
4. ✅ **Reactive state** - StateFlow for current route
5. ✅ **Transitions** - Appropriate animations per pattern
6. ✅ **Stack management** - Clear, replace, pop operations
7. ✅ **Data passing** - Arguments in destinations
8. ✅ **Conditional UI** - Show/hide based on route
9. ✅ **Branching flows** - Process with multiple paths
10. ✅ **Deep navigation** - Stack detail screens

## Extending the Demo

To add a new pattern:

1. Define destinations in `Destinations.kt`
2. Create screens in `ui/screens/`
3. Add navigation graph in `NavigationGraphs.kt`
4. Include graph in main graph
5. Add drawer/nav item in `DemoApp.kt`

## Testing Navigation

Each pattern can be tested independently:

```kotlin
val navigator = FakeNavigator()

// Test navigation
navigator.navigate(MasterDetailDestination.Detail("123"))
assertTrue(navigator.verifyNavigateTo("master_detail_detail"))

// Test backstack
navigator.navigateBack()
assertTrue(navigator.backStack.isEmpty)
```

