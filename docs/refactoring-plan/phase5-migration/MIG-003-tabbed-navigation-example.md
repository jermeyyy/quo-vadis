# MIG-003: Tabbed Navigation Example

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-003 |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | MIG-001, MIG-002 |
| **Output** | `docs/migration-examples/03-tabbed-navigation.md` |

## Objective

Demonstrate migration of tabbed navigation from the complex `TabbedNavHost` + `TabNavigatorState` pattern to the declarative `@Tab` annotation with user-controlled wrapper composables.

## Patterns Demonstrated

| Pattern | Old API | New API |
|---------|---------|---------|
| Tab configuration | `TabbedNavigatorConfig` object | `@Tab` + `@TabItem` annotations |
| Tab host | `TabbedNavHost` | `QuoVadisHost` with `tabWrapper` |
| Tab state | `rememberTabNavigator()` | Part of NavNode tree |
| Tab switching | `tabState.switchTab(tab)` | `navigator.switchTab(Tab)` |
| Tab UI | `tabUI` lambda | `tabWrapper` parameter |
| Per-tab graphs | Manual `tabGraphs` map | `rootGraph` in `@TabItem` |

## Example Content Structure

### 1. Before (Old API)

```kotlin
// === Tab Configuration Object ===
sealed class MainTab {
    data object Home : MainTab()
    data object Search : MainTab()
    data object Profile : MainTab()
}

object MainTabsConfig : TabbedNavigatorConfig<MainTab> {
    override val allTabs: List<MainTab> = listOf(
        MainTab.Home,
        MainTab.Search,
        MainTab.Profile
    )
    override val defaultTab: MainTab = MainTab.Home
}

// === Each Tab Has Its Own Graph ===
@Graph("home", startDestination = "feed")
sealed class HomeDestination : Destination {
    @Route("home/feed") data object Feed : HomeDestination()
    @Route("home/article") data class Article(val id: String) : HomeDestination()
}

@Graph("search", startDestination = "search_main")
sealed class SearchDestination : Destination {
    @Route("search/main") data object Main : SearchDestination()
    @Route("search/results") data class Results(val query: String) : SearchDestination()
}

@Graph("profile", startDestination = "profile_main")
sealed class ProfileDestination : Destination {
    @Route("profile/main") data object Main : ProfileDestination()
    @Route("profile/settings") data object Settings : ProfileDestination()
}

// === Tabbed Navigation Host ===
@Content(AppDestination.MainTabs::class)
@Composable
fun MainTabsContent(
    parentNavigator: Navigator,
    parentEntry: BackStackEntry
) {
    val tabState = rememberTabNavigator(
        config = MainTabsConfig,
        parentNavigator = parentNavigator,
        parentEntry = parentEntry
    )
    
    // Manual graph mapping
    val tabGraphs = remember {
        mapOf(
            MainTab.Home to homeGraph(),
            MainTab.Search to searchGraph(),
            MainTab.Profile to profileGraph()
        )
    }
    
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = tabGraphs,
        tabUI = { tabContent ->
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("My App") })
                },
                bottomBar = {
                    NavigationBar {
                        MainTabsConfig.allTabs.forEach { tab ->
                            NavigationBarItem(
                                icon = { Icon(tabIcon(tab), contentDescription = null) },
                                label = { Text(tabLabel(tab)) },
                                selected = tabState.activeTab == tab,
                                onClick = { tabState.switchTab(tab) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    tabContent()  // Renders active tab's graph
                }
            }
        }
    )
}

// Helper functions for tab UI
fun tabIcon(tab: MainTab): ImageVector = when (tab) {
    MainTab.Home -> Icons.Default.Home
    MainTab.Search -> Icons.Default.Search
    MainTab.Profile -> Icons.Default.Person
}

fun tabLabel(tab: MainTab): String = when (tab) {
    MainTab.Home -> "Home"
    MainTab.Search -> "Search"
    MainTab.Profile -> "Profile"
}
```

### 2. After (New API)

```kotlin
// === Tab Definition with Annotations ===
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : Destination {
    
    @TabItem(
        label = "Home",
        icon = "home",  // Material icon name or resource
        rootGraph = HomeDestination::class
    )
    @Destination(route = "tabs/home")
    data object Home : MainTabs()
    
    @TabItem(
        label = "Search",
        icon = "search",
        rootGraph = SearchDestination::class
    )
    @Destination(route = "tabs/search")
    data object Search : MainTabs()
    
    @TabItem(
        label = "Profile",
        icon = "person",
        rootGraph = ProfileDestination::class
    )
    @Destination(route = "tabs/profile")
    data object Profile : MainTabs()
}

// === Per-Tab Stack Destinations (Unchanged Structure) ===
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed") data object Feed : HomeDestination()
    @Destination(route = "home/article/{id}") data class Article(val id: String) : HomeDestination()
}

@Stack(name = "search", startDestination = "Main")
sealed class SearchDestination : Destination {
    @Destination(route = "search/main") data object Main : SearchDestination()
    @Destination(route = "search/results/{query}") data class Results(val query: String) : SearchDestination()
}

@Stack(name = "profile", startDestination = "Main")
sealed class ProfileDestination : Destination {
    @Destination(route = "profile/main") data object Main : ProfileDestination()
    @Destination(route = "profile/settings") data object Settings : ProfileDestination()
}

// === App with QuoVadisHost + tabWrapper ===
@Composable
fun MainApp() {
    val navTree = remember { buildMainTabsNavNode() }  // KSP-generated TabNode
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        tabWrapper = { tabNode, tabContent ->
            // User controls the entire scaffold!
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("My App") })
                },
                bottomBar = {
                    MainBottomNavigation(
                        activeTabIndex = tabNode.activeStackIndex,
                        onTabSelected = { tab -> navigator.switchTab(tab) }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    tabContent()  // Library provides active tab content
                }
            }
        }
    )
}

// === Reusable Bottom Navigation Component ===
@Composable
fun MainBottomNavigation(
    activeTabIndex: Int,
    onTabSelected: (MainTabs) -> Unit
) {
    val tabs = listOf(
        Triple(MainTabs.Home, Icons.Default.Home, "Home"),
        Triple(MainTabs.Search, Icons.Default.Search, "Search"),
        Triple(MainTabs.Profile, Icons.Default.Person, "Profile")
    )
    
    NavigationBar {
        tabs.forEachIndexed { index, (tab, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) },
                selected = activeTabIndex == index,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}
```

### 3. Key Migration Steps

1. **Replace config object** - `TabbedNavigatorConfig` → `@Tab` annotation on sealed class
2. **Add `@TabItem`** - Each tab gets label, icon, and rootGraph reference
3. **Add `@Destination`** - Each tab item also needs a route for deep linking
4. **Remove `TabbedNavHost`** - Use `QuoVadisHost` with `tabWrapper` parameter
5. **Remove `rememberTabNavigator()`** - State is in NavNode tree
6. **Update tab switching** - `tabState.switchTab()` → `navigator.switchTab()`
7. **Update tab selection check** - `tabState.activeTab` → `tabNode.activeStackIndex`

### 4. What KSP Generates

```kotlin
// Generated: MainTabsNavNodeBuilder.kt
fun buildMainTabsNavNode(): TabNode {
    return TabNode(
        id = "mainTabs",
        parentId = null,
        stacks = listOf(
            buildHomeNavNode().copy(parentId = "mainTabs"),
            buildSearchNavNode().copy(parentId = "mainTabs"),
            buildProfileNavNode().copy(parentId = "mainTabs")
        ),
        activeStackIndex = 0  // Initial tab: Home
    )
}

// Generated: TabMetadata (for UI helpers)
object MainTabsMetadata {
    val tabs = listOf(
        TabInfo(MainTabs.Home, "Home", "home", 0),
        TabInfo(MainTabs.Search, "Search", "search", 1),
        TabInfo(MainTabs.Profile, "Profile", "person", 2)
    )
}
```

### 5. Tab State Preservation

```kotlin
// Old: Complex manual preservation
TabbedNavHost(
    tabState = tabState,
    // Tab state managed separately from navigation state
)

// New: Automatic via NavNode tree
// Each tab's StackNode maintains its own backstack
// Switching tabs preserves history automatically

// Example: User flow
// 1. Home/Feed → Home/Article("1")
// 2. Switch to Search tab
// 3. Search/Main → Search/Results("kotlin")
// 4. Switch back to Home tab
// 5. Still at Home/Article("1") ✓
```

### 6. Tab Wrapper API Details

```kotlin
// tabWrapper signature
tabWrapper: @Composable (
    tabNode: TabNode,           // Current tab state
    tabContent: @Composable () -> Unit  // Active tab's content
) -> Unit

// Available from TabNode:
tabNode.id                    // "mainTabs"
tabNode.activeStackIndex      // 0, 1, 2...
tabNode.stacks                // List<StackNode> - all tab stacks
tabNode.activeStack           // Current StackNode

// User responsibilities:
// - Scaffold layout
// - Top/bottom bars
// - Tab selection UI
// - Calling tabContent() in the right place

// Library responsibilities:
// - Providing correct content for active tab
// - Managing tab state transitions
// - Handling predictive back
// - State preservation
```

## Acceptance Criteria

- [ ] Complete before/after example with all components
- [ ] `@Tab` and `@TabItem` usage is clear
- [ ] `tabWrapper` pattern is well documented
- [ ] Tab switching via `navigator.switchTab()` shown
- [ ] Tab state preservation is explained
- [ ] Generated code examples included

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Forgetting `tabWrapper` parameter | `QuoVadisHost` won't render tabs without it |
| Using wrong index for selection | Use `tabNode.activeStackIndex`, not manual tracking |
| Not calling `tabContent()` | Wrapper must invoke the content lambda |
| Expecting tab state in Navigator | Use `tabNode` parameter in wrapper |

## Related Tasks

- [MIG-001: Simple Stack Navigation](./MIG-001-simple-stack-example.md)
- [MIG-005: Nested Tabs + Detail](./MIG-005-nested-tabs-detail-example.md)
- [RENDER-008: User Wrapper API](../phase2-renderer/RENDER-008-user-wrapper-api.md)
