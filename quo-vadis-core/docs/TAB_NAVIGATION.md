# Tabbed Navigation

## Overview

Quo Vadis provides a powerful annotation-driven system for implementing tabbed navigation with independent backstacks. Each tab maintains its own navigation history and state, similar to popular apps like Instagram or Twitter.

### Key Features

- 🎯 **Zero Boilerplate** - KSP code generation eliminates manual configuration
- 🔒 **Type-Safe** - Compile-time checked tab definitions  
- 📚 **Independent Stacks** - Each tab has its own navigation history
- 💾 **State Preservation** - Tab content survives tab switches
- ⬅️ **Smart Back Press** - Hierarchical navigation across tabs
- 🎨 **Flexible UI** - Works with BottomNavigationBar, NavigationRail, or custom UI

## Quick Start

### 1. Add Dependencies

```kotlin
// build.gradle.kts
plugins {
    id("com.google.devtools.ksp") version "2.2.20-1.0.29"
}

dependencies {
    implementation("io.github.jermeyyy:quo-vadis-core:0.1.0")
    implementation("io.github.jermeyyy:quo-vadis-annotations:0.1.0")
    ksp("io.github.jermeyyy:quo-vadis-ksp:0.1.0")
}
```

### 2. Define Your Tabs

Create a sealed class with `@TabGraph` and `@Tab` annotations:

```kotlin
@TabGraph(
    name = "main_tabs",
    initialTab = "Home",
    primaryTab = "Home"
)
sealed class MainTabs : TabDefinition {

    @Tab(
        route = "tab_home",
        label = "Home",
        icon = "home",
        rootGraph = AppDestination::class,
        rootDestination = AppDestination.Home::class
    )
    data object Home : MainTabs() {
        override val route = "tab_home"
        override val rootDestination = AppDestination.Home
    }

    @Tab(
        route = "tab_profile",
        label = "Profile",
        icon = "person",
        rootGraph = AppDestination::class,
        rootDestination = AppDestination.Profile::class
    )
    data object Profile : MainTabs() {
        override val route = "tab_profile"
        override val rootDestination = AppDestination.Profile
    }
}
```

KSP generates a `MainTabsConfig` object with all tab configuration.

### 3. Use in Your UI

```kotlin
@Composable
fun App() {
    val parentNavigator = rememberNavigator()
    val tabState = rememberTabNavigator(MainTabsConfig, parentNavigator)
    val tabGraph = remember { buildAppDestinationGraph() }
    
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = MainTabsConfig.allTabs.associateWith { tabGraph },
        navigator = parentNavigator,
        tabUI = { content ->
            Scaffold(
                bottomBar = {
                    val selectedTab by tabState.selectedTab.collectAsState()
                    BottomNavigationBar(
                        currentTab = selectedTab,
                        onTabSelected = { tab -> tabState.selectTab(tab) }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    content()
                }
            }
        }
    )
}
```

## Annotation Reference

### @TabGraph

Defines the tab container configuration.

**Parameters:**
- `name: String` - Base name for generated code (e.g., "main_tabs" → `MainTabsConfig`)
- `initialTab: String` - Tab to display on first launch
- `primaryTab: String` - Primary tab for back navigation (defaults to `initialTab`)

### @Tab

Defines an individual tab.

**Parameters:**
- `route: String` - Unique identifier for this tab
- `label: String` - Display name for UI
- `icon: String` - Icon identifier (Material Icons name or custom)
- `rootGraph: KClass<*>` - Destination class this tab belongs to
- `rootDestination: KClass<*>` - Initial destination when tab is selected

## Architecture

Each tab maintains an independent navigation stack:

```
TabbedNavHost
├── Tab 1 Navigator
│   ├── Screen C
│   ├── Screen B
│   └── Screen A (root)
├── Tab 2 Navigator
│   ├── Screen F
│   ├── Screen E  
│   └── Screen D (root)
└── Tab 3 Navigator
    └── Screen G (root)
```

### Back Press Behavior

1. **Not at root:** Pop from current tab's stack
2. **At root (not primary tab):** Switch to primary tab
3. **At root (primary tab):** Delegate to parent navigator (exit app)

## API Reference

### rememberTabNavigator

```kotlin
@Composable
fun rememberTabNavigator(
    config: TabNavigatorConfig,
    parentNavigator: Navigator
): TabNavigatorState
```

Creates and remembers tab navigation state.

### TabbedNavHost

```kotlin
@Composable
fun TabbedNavHost(
    tabState: TabNavigatorState,
    tabGraphs: Map<TabDefinition, NavigationGraph>,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    tabUI: (@Composable (content: @Composable () -> Unit) -> Unit)? = null,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    enableComposableCache: Boolean = true,
    enablePredictiveBack: Boolean = true
)
```

Main composable for rendering tabbed navigation with custom UI.

### TabNavigatorState

State holder for tab navigation.

**Methods:**
- `selectTab(tab: TabDefinition)` - Switch to specific tab
- `getCurrentNavigator(): Navigator?` - Get navigator for current tab

**Flows:**
- `selectedTab: StateFlow<TabDefinition?>` - Currently selected tab

## Complete Example: Bottom Navigation

```kotlin
// 1. Define tabs
@TabGraph(name = "main_tabs", initialTab = "Home", primaryTab = "Home")
sealed class MainTabs : TabDefinition {
    
    @Tab(
        route = "tab_home",
        label = "Home",
        icon = "home",
        rootGraph = AppDestination::class,
        rootDestination = AppDestination.Home::class
    )
    data object Home : MainTabs() {
        override val route = "tab_home"
        override val rootDestination = AppDestination.Home
    }
    
    @Tab(
        route = "tab_search",
        label = "Search",
        icon = "search",
        rootGraph = AppDestination::class,
        rootDestination = AppDestination.Search::class
    )
    data object Search : MainTabs() {
        override val route = "tab_search"
        override val rootDestination = AppDestination.Search
    }
    
    @Tab(
        route = "tab_profile",
        label = "Profile",
        icon = "person",
        rootGraph = AppDestination::class,
        rootDestination = AppDestination.Profile::class
    )
    data object Profile : MainTabs() {
        override val route = "tab_profile"
        override val rootDestination = AppDestination.Profile
    }
}

// 2. Create bottom navigation UI
@Composable
fun BottomNavigationBar(
    currentTab: TabDefinition?,
    onTabSelected: (TabDefinition) -> Unit
) {
    NavigationBar {
        MainTabsConfig.allTabs.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(getIconForTab(tab), contentDescription = tab.label) },
                label = { Text(tab.label ?: "") },
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

// 3. Use in app
@Composable
fun App() {
    val parentNavigator = rememberNavigator()
    val tabState = rememberTabNavigator(MainTabsConfig, parentNavigator)
    val tabGraph = remember { buildAppDestinationGraph() }
    
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = MainTabsConfig.allTabs.associateWith { tabGraph },
        navigator = parentNavigator,
        tabUI = { content ->
            Scaffold(
                bottomBar = {
                    val selectedTab by tabState.selectedTab.collectAsState()
                    BottomNavigationBar(
                        currentTab = selectedTab,
                        onTabSelected = { tabState.selectTab(it) }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    content()
                }
            }
        }
    )
}
```

## Navigation Within Tabs

```kotlin
@Composable
fun HomeScreen(navigator: Navigator) {
    Column {
        // Navigate within current tab
        Button(onClick = {
            navigator.navigate(AppDestination.Details("item123"))
        }) {
            Text("View Details")
        }
    }
}
```

## Testing

```kotlin
@Test
fun testTabSwitching() = runTest {
    val fakeNavigator = FakeNavigator()
    val tabState = TabNavigatorState(
        config = MainTabsConfig,
        parentNavigator = fakeNavigator
    )
    
    tabState.selectTab(MainTabs.Profile)
    assertEquals(MainTabs.Profile, tabState.selectedTab.value)
}
```

## Best Practices

### ✅ DO:
- Keep tabs at top level for main app sections
- Use 3-5 tabs maximum for mobile
- Set a logical primary tab for back navigation
- Use clear, recognizable icons

### ❌ DON'T:
- Don't use tabs for linear flows
- Don't nest too deeply (max 2 levels)
- Don't change tab structure dynamically
- Don't ignore back behavior configuration

## Common Issues

### Tab content not showing
Ensure each tab's `rootDestination` is registered in the navigation graph.

### State not preserved
Use `rememberSaveable` in your screens for state that should survive tab switches.

### Back press doesn't work  
Ensure `TabbedNavHost` receives the correct parent navigator.

## See Also

- [API Reference](API_REFERENCE.md)
- [Annotation API](ANNOTATION_API.md)
- [Architecture Guide](ARCHITECTURE.md)
