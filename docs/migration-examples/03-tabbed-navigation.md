n
# Migration Guide: Tabbed Navigation

> **Difficulty**: Medium | **Time Estimate**: 45-90 minutes | **Prerequisites**: [01-simple-stack.md](./01-simple-stack.md) complete

This guide demonstrates how to migrate tabbed navigation from the complex `TabbedNavHost` + `TabNavigatorState` pattern to the declarative `@Tab` annotation with user-controlled wrapper composables.

---

## Table of Contents

1. [Overview](#overview)
2. [Before (Old API)](#before-old-api)
3. [After (New API)](#after-new-api)
4. [Key Migration Steps](#key-migration-steps)
5. [What KSP Generates](#what-ksp-generates)
6. [Tab State Preservation](#tab-state-preservation)
7. [Common Pitfalls](#common-pitfalls)
8. [Next Steps](#next-steps)
9. [Related Resources](#related-resources)

---

## Overview

The new NavNode architecture dramatically simplifies tabbed navigation by:

- **Replacing configuration objects** with declarative `@Tab` annotations
- **Eliminating `TabbedNavHost`** in favor of unified `QuoVadisHost` with `tabWrapper`
- **Removing `rememberTabNavigator()`** — tab state is part of the NavNode tree
- **Simplifying tab switching** — `navigator.switchTab(Tab)` replaces `tabState.selectTab()`
- **Automatic state preservation** — each tab's `StackNode` maintains its own backstack

### Annotation Changes Summary

| Old Annotation/Class | New Annotation | Purpose |
|---------------------|----------------|---------|
| `@TabGraph(name, initialTab)` | `@Tab(name, initialTab)` | Define tab container |
| `@Tab(route, label, icon, ...)` | `@TabItem(label, icon, rootGraph)` | Define individual tab |
| `TabDefinition` interface | `Destination` base class | Tab destination type |
| `TabNavigatorConfig` | Not needed | Tab configuration |

### Host Changes Summary

| Old Component | New Component | Change |
|---------------|---------------|--------|
| `TabbedNavHost(tabState, tabGraphs, ...)` | `QuoVadisHost(navigator, screenRegistry, tabWrapper)` | Unified host with wrapper |
| `rememberTabNavigator(config, parentNav, ...)` | `rememberNavigator(initialNavNode)` | NavNode tree initialization |
| `tabState.selectTab(tab)` | `navigator.switchTab(Tab)` | Tab switching API |
| `tabState.selectedTab.collectAsState()` | `tabNode.activeStackIndex` | Active tab query |
| `TabNavigatorConfig.allTabs` | KSP-generated `TabMetadata` | Tab info for UI |

---

## Before (Old API)

### Complete Working Example

```kotlin
package com.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabGraph
import com.jermey.quo.vadis.core.navigation.compose.*
import com.jermey.quo.vadis.core.navigation.core.*

// ═══════════════════════════════════════════════════════════
// STEP 1: Define Tab Configuration with @TabGraph
// ═══════════════════════════════════════════════════════════

/**
 * Main bottom navigation tabs.
 * 
 * OLD: Uses @TabGraph annotation on a sealed class implementing TabDefinition.
 * Each tab requires @Tab annotation with route, label, icon, rootGraph, and rootDestination.
 */
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
        rootGraph = HomeDestination::class,
        rootDestination = HomeDestination.Feed::class
    )
    data object Home : MainTabs() {
        override val route = "tab_home"
        override val rootDestination = HomeDestination.Feed
    }

    @Tab(
        route = "tab_search",
        label = "Search",
        icon = "search",
        rootGraph = SearchDestination::class,
        rootDestination = SearchDestination.Main::class
    )
    data object Search : MainTabs() {
        override val route = "tab_search"
        override val rootDestination = SearchDestination.Main
    }

    @Tab(
        route = "tab_profile",
        label = "Profile",
        icon = "person",
        rootGraph = ProfileDestination::class,
        rootDestination = ProfileDestination.Main::class
    )
    data object Profile : MainTabs() {
        override val route = "tab_profile"
        override val rootDestination = ProfileDestination.Main
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 2: Define Per-Tab Navigation Graphs
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Each tab has its own @Graph-annotated sealed class.
 */
@Graph("home", startDestination = "home/feed")
sealed class HomeDestination : Destination {
    @Route("home/feed")
    data object Feed : HomeDestination()
    
    @Route("home/article/{id}")
    data class Article(val id: String) : HomeDestination()
}

@Graph("search", startDestination = "search/main")
sealed class SearchDestination : Destination {
    @Route("search/main")
    data object Main : SearchDestination()
    
    @Route("search/results/{query}")
    data class Results(val query: String) : SearchDestination()
}

@Graph("profile", startDestination = "profile/main")
sealed class ProfileDestination : Destination {
    @Route("profile/main")
    data object Main : ProfileDestination()
    
    @Route("profile/settings")
    data object Settings : ProfileDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Create Tab Screen Content
// ═══════════════════════════════════════════════════════════

@Content(HomeDestination.Feed::class)
@Composable
fun HomeFeedContent(navigator: Navigator) {
    Column {
        Text("Home Feed")
        Button(onClick = { navigator.navigate(HomeDestination.Article("123")) }) {
            Text("View Article")
        }
    }
}

@Content(HomeDestination.Article::class)
@Composable
fun ArticleContent(destination: HomeDestination.Article, navigator: Navigator) {
    Column {
        Text("Article: ${destination.id}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

@Content(SearchDestination.Main::class)
@Composable
fun SearchMainContent(navigator: Navigator) {
    Column {
        Text("Search")
        Button(onClick = { navigator.navigate(SearchDestination.Results("kotlin")) }) {
            Text("Search for 'kotlin'")
        }
    }
}

@Content(SearchDestination.Results::class)
@Composable
fun SearchResultsContent(destination: SearchDestination.Results, navigator: Navigator) {
    Column {
        Text("Results for: ${destination.query}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

@Content(ProfileDestination.Main::class)
@Composable
fun ProfileMainContent(navigator: Navigator) {
    Column {
        Text("Profile")
        Button(onClick = { navigator.navigate(ProfileDestination.Settings) }) {
            Text("Settings")
        }
    }
}

@Content(ProfileDestination.Settings::class)
@Composable
fun ProfileSettingsContent(navigator: Navigator) {
    Column {
        Text("Settings")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 4: Set Up TabbedNavHost with Manual Configuration
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Requires rememberTabNavigator(), manual graph mapping, and TabbedNavHost.
 */
@Composable
fun MainTabsScreen(
    parentNavigator: Navigator,
    parentEntry: BackStackEntry,
    modifier: Modifier = Modifier
) {
    // Build separate graphs for each tab
    val homeGraph = remember { homeGraph() }
    val searchGraph = remember { searchGraph() }
    val profileGraph = remember { profileGraph() }
    
    // Manual graph mapping - must maintain separately!
    val tabGraphs = remember {
        mapOf(
            MainTabs.Home to homeGraph,
            MainTabs.Search to searchGraph,
            MainTabs.Profile to profileGraph
        )
    }
    
    // Create tab state with parent integration
    val tabState = rememberTabNavigator(
        config = MainTabsConfig,  // KSP-generated config
        parentNavigator = parentNavigator,
        parentEntry = parentEntry
    )
    val selectedTab by tabState.selectedTab.collectAsState()
    
    // OLD: TabbedNavHost with tabUI wrapper
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = tabGraphs,
        navigator = parentNavigator,
        modifier = modifier,
        tabUI = { tabContent ->
            // User-provided scaffold with bottom nav
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        MainTabsConfig.allTabs.forEach { tab ->
                            NavigationBarItem(
                                icon = { Icon(tabIcon(tab), contentDescription = null) },
                                label = { Text(tabLabel(tab)) },
                                selected = selectedTab == tab,
                                onClick = { tabState.selectTab(tab) }  // OLD: tabState.selectTab()
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    tabContent()  // Renders active tab's graph
                }
            }
        },
        tabTransitionSpec = TabTransitionSpec.Crossfade,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}

// Helper functions for tab UI (must be maintained manually!)
fun tabIcon(tab: MainTabs): ImageVector = when (tab) {
    MainTabs.Home -> Icons.Default.Home
    MainTabs.Search -> Icons.Default.Search
    MainTabs.Profile -> Icons.Default.Person
}

fun tabLabel(tab: MainTabs): String = when (tab) {
    MainTabs.Home -> "Home"
    MainTabs.Search -> "Search"
    MainTabs.Profile -> "Profile"
}

// ═══════════════════════════════════════════════════════════
// STEP 5: App Root Setup
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Requires manual route initialization and graph setup.
 */
@Composable
fun App() {
    // Initialize routes globally
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val rootGraph = remember { appRootGraph() }
    
    // Manual setup
    LaunchedEffect(navigator, rootGraph) {
        navigator.registerGraph(rootGraph)
        navigator.setStartDestination(AppDestination.MainTabs)
    }
    
    GraphNavHost(
        graph = rootGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}
```

### Old API Characteristics

1. **`@TabGraph` + `@Tab` annotations** define tab structure
2. **`TabDefinition` interface** must be implemented with `route` and `rootDestination`
3. **`TabNavigatorConfig`** generated by KSP for configuration
4. **`rememberTabNavigator()`** manages tab state with parent integration
5. **`TabbedNavHost`** renders tabs with `tabGraphs` map
6. **`tabState.selectTab(tab)`** for switching tabs
7. **`tabState.selectedTab.collectAsState()`** for observing current tab
8. **Manual graph mapping** — `tabGraphs` map must be maintained
9. **Helper functions** — `tabIcon()`, `tabLabel()` for UI

---

## After (New API)

### Complete Migrated Example

```kotlin
package com.example.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TabNode
import com.jermey.quo.vadis.core.navigation.core.rememberNavigator
import com.example.app.generated.GeneratedScreenRegistry
import com.example.app.generated.buildMainTabsNavNode
import com.example.app.generated.MainTabsMetadata

// ═══════════════════════════════════════════════════════════
// STEP 1: Define Tab Container with @Tab Annotation
// ═══════════════════════════════════════════════════════════

/**
 * Main bottom navigation tabs.
 * 
 * NEW: Uses @Tab annotation on sealed class extending Destination.
 * Each tab uses @TabItem with label, icon, and rootGraph reference.
 * Each tab ALSO needs @Destination for deep linking support.
 */
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : com.jermey.quo.vadis.core.navigation.core.Destination {

    @TabItem(
        label = "Home",
        icon = "home",
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

// ═══════════════════════════════════════════════════════════
// STEP 2: Define Per-Tab Stacks (Structure Simplified)
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Each tab has its own @Stack-annotated sealed class.
 * Uses @Destination instead of @Route.
 */
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/article/{id}")
    data class Article(val id: String) : HomeDestination()
}

@Stack(name = "search", startDestination = "Main")
sealed class SearchDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "search/main")
    data object Main : SearchDestination()
    
    @Destination(route = "search/results/{query}")
    data class Results(val query: String) : SearchDestination()
}

@Stack(name = "profile", startDestination = "Main")
sealed class ProfileDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "profile/main")
    data object Main : ProfileDestination()
    
    @Destination(route = "profile/settings")
    data object Settings : ProfileDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Create Tab Screen Content
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Uses @Screen instead of @Content.
 */
@Screen(HomeDestination.Feed::class)
@Composable
fun HomeFeedScreen(navigator: Navigator) {
    Column {
        Text("Home Feed")
        Button(onClick = { navigator.navigate(HomeDestination.Article("123")) }) {
            Text("View Article")
        }
    }
}

@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) {
    Column {
        Text("Article: ${destination.id}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

@Screen(SearchDestination.Main::class)
@Composable
fun SearchMainScreen(navigator: Navigator) {
    Column {
        Text("Search")
        Button(onClick = { navigator.navigate(SearchDestination.Results("kotlin")) }) {
            Text("Search for 'kotlin'")
        }
    }
}

@Screen(SearchDestination.Results::class)
@Composable
fun SearchResultsScreen(destination: SearchDestination.Results, navigator: Navigator) {
    Column {
        Text("Results for: ${destination.query}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

@Screen(ProfileDestination.Main::class)
@Composable
fun ProfileMainScreen(navigator: Navigator) {
    Column {
        Text("Profile")
        Button(onClick = { navigator.navigate(ProfileDestination.Settings) }) {
            Text("Settings")
        }
    }
}

@Screen(ProfileDestination.Settings::class)
@Composable
fun ProfileSettingsScreen(navigator: Navigator) {
    Column {
        Text("Settings")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 4: Set Up QuoVadisHost with tabWrapper
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Minimal setup with QuoVadisHost and tabWrapper parameter.
 * No TabbedNavHost, no rememberTabNavigator(), no manual graph mapping!
 */
@Composable
fun App() {
    // KSP generates buildMainTabsNavNode() from @Tab annotation
    val navTree = remember { buildMainTabsNavNode() }
    
    // Navigator initialized directly with NavNode tree (TabNode)
    val navigator = rememberNavigator(navTree)
    
    // NEW: QuoVadisHost with tabWrapper for custom tab UI
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        tabWrapper = { tabNode, tabContent ->
            // User controls the entire scaffold!
            MainTabScaffold(
                tabNode = tabNode,
                navigator = navigator,
                content = tabContent
            )
        }
    )
}

// ═══════════════════════════════════════════════════════════
// STEP 5: Custom Tab Scaffold (User-Controlled UI)
// ═══════════════════════════════════════════════════════════

/**
 * NEW: User-defined scaffold with full control over tab UI.
 * TabNode provides activeStackIndex for selection state.
 * navigator.switchTab() for tab switching.
 */
@Composable
fun MainTabScaffold(
    tabNode: TabNode,
    navigator: Navigator,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            MainBottomNavigation(
                activeTabIndex = tabNode.activeStackIndex,
                onTabSelected = { tab -> navigator.switchTab(tab) }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            content()  // Library provides active tab content
        }
    }
}

/**
 * NEW: Reusable bottom navigation using KSP-generated metadata.
 */
@Composable
fun MainBottomNavigation(
    activeTabIndex: Int,
    onTabSelected: (MainTabs) -> Unit
) {
    // Use KSP-generated metadata for tab info
    NavigationBar {
        MainTabsMetadata.tabs.forEachIndexed { index, tabInfo ->
            NavigationBarItem(
                icon = { Icon(tabInfo.icon, contentDescription = null) },
                label = { Text(tabInfo.label) },
                selected = activeTabIndex == index,
                onClick = { onTabSelected(tabInfo.tab) }
            )
        }
    }
}
```

### New API Characteristics

1. **`@Tab` annotation** on sealed class extending `Destination`
2. **`@TabItem` annotation** on each tab with label, icon, and rootGraph
3. **`@Destination` annotation** also on each tab for deep linking
4. **No `TabDefinition` interface** — standard `Destination` base class
5. **No `TabNavigatorConfig`** — configuration in annotations
6. **No `rememberTabNavigator()`** — state in NavNode tree
7. **No `TabbedNavHost`** — use `QuoVadisHost` with `tabWrapper`
8. **`navigator.switchTab(Tab)`** for switching tabs
9. **`tabNode.activeStackIndex`** for current tab state
10. **KSP-generated `TabMetadata`** for UI helpers

---

## Key Migration Steps

Follow these steps to migrate your tabbed navigation:

### Step 1: Replace @TabGraph with @Tab

```diff
- @TabGraph(
-     name = "main_tabs",
-     initialTab = "Home",
-     primaryTab = "Home"
- )
- sealed class MainTabs : TabDefinition {
+ @Tab(name = "mainTabs", initialTab = "Home")
+ sealed class MainTabs : com.jermey.quo.vadis.core.navigation.core.Destination {
```

> ⚠️ **Important**: Change base interface from `TabDefinition` to `Destination`.

### Step 2: Replace @Tab with @TabItem + @Destination

```diff
-     @Tab(
-         route = "tab_home",
-         label = "Home",
-         icon = "home",
-         rootGraph = HomeDestination::class,
-         rootDestination = HomeDestination.Feed::class
-     )
-     data object Home : MainTabs() {
-         override val route = "tab_home"
-         override val rootDestination = HomeDestination.Feed
-     }
+     @TabItem(
+         label = "Home",
+         icon = "home",
+         rootGraph = HomeDestination::class
+     )
+     @Destination(route = "tabs/home")
+     data object Home : MainTabs()
```

> 💡 **Tip**: `@TabItem` handles tab metadata, `@Destination` handles routing. No more `override val route` needed!

### Step 3: Update Per-Tab Graphs to Stacks

```diff
- @Graph("home", startDestination = "home/feed")
+ @Stack(name = "home", startDestination = "Feed")
  sealed class HomeDestination : Destination {
-     @Route("home/feed")
+     @Destination(route = "home/feed")
      data object Feed : HomeDestination()
```

> ⚠️ **Important**: `startDestination` uses class name (`"Feed"`) not route (`"home/feed"`).

### Step 4: Update Content Annotations to Screen

```diff
- @Content(HomeDestination.Feed::class)
+ @Screen(HomeDestination.Feed::class)
  @Composable
- fun HomeFeedContent(navigator: Navigator) {
+ fun HomeFeedScreen(navigator: Navigator) {
```

### Step 5: Remove rememberTabNavigator() and Manual Graph Mapping

```diff
  @Composable
  fun MainTabsScreen(...) {
-     val homeGraph = remember { homeGraph() }
-     val searchGraph = remember { searchGraph() }
-     val profileGraph = remember { profileGraph() }
-     
-     val tabGraphs = remember {
-         mapOf(
-             MainTabs.Home to homeGraph,
-             MainTabs.Search to searchGraph,
-             MainTabs.Profile to profileGraph
-         )
-     }
-     
-     val tabState = rememberTabNavigator(
-         config = MainTabsConfig,
-         parentNavigator = parentNavigator,
-         parentEntry = parentEntry
-     )
+     val navTree = remember { buildMainTabsNavNode() }
+     val navigator = rememberNavigator(navTree)
```

### Step 6: Replace TabbedNavHost with QuoVadisHost + tabWrapper

```diff
-     TabbedNavHost(
-         tabState = tabState,
-         tabGraphs = tabGraphs,
-         navigator = parentNavigator,
-         modifier = modifier,
-         tabUI = { tabContent ->
-             Scaffold(
-                 bottomBar = {
-                     NavigationBar {
-                         MainTabsConfig.allTabs.forEach { tab ->
-                             NavigationBarItem(
-                                 icon = { Icon(tabIcon(tab), contentDescription = null) },
-                                 label = { Text(tabLabel(tab)) },
-                                 selected = selectedTab == tab,
-                                 onClick = { tabState.selectTab(tab) }
-                             )
-                         }
-                     }
-                 }
-             ) { paddingValues ->
-                 Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
-                     tabContent()
-                 }
-             }
-         },
-         tabTransitionSpec = TabTransitionSpec.Crossfade,
-         defaultTransition = NavigationTransitions.SlideHorizontal
-     )
+     QuoVadisHost(
+         navigator = navigator,
+         screenRegistry = GeneratedScreenRegistry,
+         tabWrapper = { tabNode, tabContent ->
+             Scaffold(
+                 bottomBar = {
+                     NavigationBar {
+                         MainTabsMetadata.tabs.forEachIndexed { index, tabInfo ->
+                             NavigationBarItem(
+                                 icon = { Icon(tabInfo.icon, contentDescription = null) },
+                                 label = { Text(tabInfo.label) },
+                                 selected = tabNode.activeStackIndex == index,
+                                 onClick = { navigator.switchTab(tabInfo.tab) }
+                             )
+                         }
+                     }
+                 }
+             ) { paddingValues ->
+                 Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
+                     tabContent()
+                 }
+             }
+         }
+     )
```

### Step 7: Update Tab Switching Calls

```diff
- onClick = { tabState.selectTab(tab) }
+ onClick = { navigator.switchTab(tab) }
```

### Step 8: Update Tab Selection Check

```diff
- selected = selectedTab == tab
+ selected = tabNode.activeStackIndex == index
```

### Step 9: Remove Helper Functions (Use Generated Metadata)

```diff
- // Helper functions for tab UI (must be maintained manually!)
- fun tabIcon(tab: MainTabs): ImageVector = when (tab) {
-     MainTabs.Home -> Icons.Default.Home
-     MainTabs.Search -> Icons.Default.Search
-     MainTabs.Profile -> Icons.Default.Person
- }
- 
- fun tabLabel(tab: MainTabs): String = when (tab) {
-     MainTabs.Home -> "Home"
-     MainTabs.Search -> "Search"
-     MainTabs.Profile -> "Profile"
- }
+ // Use MainTabsMetadata.tabs for tab info (KSP-generated)
```

### Step 10: Update Imports

```diff
- import com.jermey.quo.vadis.annotations.Tab
- import com.jermey.quo.vadis.annotations.TabGraph
- import com.jermey.quo.vadis.core.navigation.compose.TabbedNavHost
- import com.jermey.quo.vadis.core.navigation.compose.rememberTabNavigator
- import com.jermey.quo.vadis.core.navigation.compose.TabTransitionSpec
- import com.jermey.quo.vadis.core.navigation.core.TabDefinition
- import com.jermey.quo.vadis.core.navigation.core.TabNavigatorConfig
+ import com.jermey.quo.vadis.annotations.Destination
+ import com.jermey.quo.vadis.annotations.Screen
+ import com.jermey.quo.vadis.annotations.Stack
+ import com.jermey.quo.vadis.annotations.Tab
+ import com.jermey.quo.vadis.annotations.TabItem
+ import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
+ import com.jermey.quo.vadis.core.navigation.core.TabNode
+ import com.example.app.generated.GeneratedScreenRegistry
+ import com.example.app.generated.buildMainTabsNavNode
+ import com.example.app.generated.MainTabsMetadata
```

### Step 11: Build and Verify

```bash
./gradlew :app:assembleDebug
```

Check for generated files in:
```
build/generated/ksp/debug/kotlin/com/example/app/generated/
├── MainTabsNavNodeBuilder.kt      # buildMainTabsNavNode() function
├── HomeNavNodeBuilder.kt          # Per-tab stack builders
├── SearchNavNodeBuilder.kt
├── ProfileNavNodeBuilder.kt
├── GeneratedScreenRegistry.kt     # ScreenRegistry with all screens
└── MainTabsMetadata.kt            # Tab info for UI
```

---

## What KSP Generates

KSP processes your annotations and generates the following code:

### MainTabsNavNodeBuilder.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.app.generated

import com.jermey.quo.vadis.core.navigation.core.TabNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.example.app.MainTabs

/**
 * Builds the initial TabNode tree for the "mainTabs" tab container.
 * 
 * @return A TabNode containing all tab stacks
 */
fun buildMainTabsNavNode(): TabNode {
    return TabNode(
        id = "mainTabs",
        parentId = null,
        stacks = listOf(
            buildHomeNavNode().copy(parentId = "mainTabs"),
            buildSearchNavNode().copy(parentId = "mainTabs"),
            buildProfileNavNode().copy(parentId = "mainTabs")
        ),
        activeStackIndex = 0  // Initial tab: Home (index 0)
    )
}
```

### MainTabsMetadata.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.app.generated

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.app.MainTabs

/**
 * Metadata for MainTabs, used for building tab UI.
 */
object MainTabsMetadata {
    
    /**
     * All tabs in order with their metadata.
     */
    val tabs: List<TabInfo> = listOf(
        TabInfo(
            tab = MainTabs.Home,
            label = "Home",
            icon = Icons.Default.Home,
            index = 0
        ),
        TabInfo(
            tab = MainTabs.Search,
            label = "Search",
            icon = Icons.Default.Search,
            index = 1
        ),
        TabInfo(
            tab = MainTabs.Profile,
            label = "Profile",
            icon = Icons.Default.Person,
            index = 2
        )
    )
    
    /**
     * Get tab info by tab instance.
     */
    fun getInfo(tab: MainTabs): TabInfo = tabs.first { it.tab == tab }
    
    /**
     * Get tab by index.
     */
    fun getTab(index: Int): MainTabs = tabs[index].tab
}

/**
 * Information about a single tab for UI purposes.
 */
data class TabInfo(
    val tab: MainTabs,
    val label: String,
    val icon: ImageVector,
    val index: Int
)
```

### GeneratedScreenRegistry.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.app.generated

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenRegistry
import com.example.app.*

/**
 * KSP-generated screen registry mapping destinations to composable content.
 */
object GeneratedScreenRegistry : ScreenRegistry {

    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        when (destination) {
            // HomeDestination screens
            is HomeDestination.Feed -> HomeFeedScreen(navigator)
            is HomeDestination.Article -> ArticleScreen(destination, navigator)
            
            // SearchDestination screens
            is SearchDestination.Main -> SearchMainScreen(navigator)
            is SearchDestination.Results -> SearchResultsScreen(destination, navigator)
            
            // ProfileDestination screens
            is ProfileDestination.Main -> ProfileMainScreen(navigator)
            is ProfileDestination.Settings -> ProfileSettingsScreen(navigator)

            else -> error("No screen registered for destination: $destination")
        }
    }

    override fun hasContent(destination: Destination): Boolean = when (destination) {
        is HomeDestination.Feed,
        is HomeDestination.Article,
        is SearchDestination.Main,
        is SearchDestination.Results,
        is ProfileDestination.Main,
        is ProfileDestination.Settings -> true
        else -> false
    }
}
```

### How the Generated Code Works

```
┌─────────────────────────────────────────────────────────────────┐
│                         Your Code                                │
├─────────────────────────────────────────────────────────────────┤
│  @Tab(name = "mainTabs", initialTab = "Home")                    │
│  sealed class MainTabs : Destination {                           │
│      @TabItem(label = "Home", icon = "home", rootGraph = ...)    │
│      @Destination(route = "tabs/home")                           │
│      data object Home : MainTabs()                               │
│  }                                                               │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼ KSP Processes
┌─────────────────────────────────────────────────────────────────┐
│                    Generated Code                                │
├─────────────────────────────────────────────────────────────────┤
│  buildMainTabsNavNode()   →  Creates TabNode with child stacks  │
│  MainTabsMetadata         →  Tab info for UI (label, icon)       │
│  GeneratedScreenRegistry  →  Maps destinations to screens        │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼ Used by
┌─────────────────────────────────────────────────────────────────┐
│                       Runtime                                    │
├─────────────────────────────────────────────────────────────────┤
│  val navTree = buildMainTabsNavNode()                            │
│  val navigator = rememberNavigator(navTree)                      │
│                                                                  │
│  QuoVadisHost(                                                   │
│      navigator = navigator,                                      │
│      screenRegistry = GeneratedScreenRegistry,                   │
│      tabWrapper = { tabNode, content ->                          │
│          // tabNode.activeStackIndex for selection               │
│          // navigator.switchTab(Tab) for switching               │
│          // MainTabsMetadata.tabs for UI info                    │
│      }                                                           │
│  )                                                               │
└─────────────────────────────────────────────────────────────────┘
```

### TabNode Tree Structure

```
TabNode("mainTabs")
├── activeStackIndex: 0
└── stacks:
    ├── [0] StackNode("home", parentId="mainTabs")
    │       └── children: [ScreenNode(HomeDestination.Feed)]
    ├── [1] StackNode("search", parentId="mainTabs")
    │       └── children: [ScreenNode(SearchDestination.Main)]
    └── [2] StackNode("profile", parentId="mainTabs")
            └── children: [ScreenNode(ProfileDestination.Main)]
```

---

## Tab State Preservation

### How State is Preserved Across Tab Switches

```kotlin
// OLD: Complex manual preservation
TabbedNavHost(
    tabState = tabState,
    // Tab state managed separately from navigation state
)

// NEW: Automatic via NavNode tree
// Each tab's StackNode maintains its own backstack
// Switching tabs preserves history automatically
```

### Example User Flow

```
1. Start at Home/Feed
   TabNode(activeStackIndex=0)
   └── StackNode("home") → [Feed]

2. Navigate to Home/Article("1")
   TabNode(activeStackIndex=0)
   └── StackNode("home") → [Feed, Article("1")]

3. Switch to Search tab
   TabNode(activeStackIndex=1)  // Changed!
   ├── StackNode("home") → [Feed, Article("1")]  // Preserved!
   └── StackNode("search") → [Main]

4. Navigate to Search/Results("kotlin")
   TabNode(activeStackIndex=1)
   ├── StackNode("home") → [Feed, Article("1")]  // Still preserved!
   └── StackNode("search") → [Main, Results("kotlin")]

5. Switch back to Home tab
   TabNode(activeStackIndex=0)  // Changed back!
   └── StackNode("home") → [Feed, Article("1")]  // Still at Article!

6. User is still at Home/Article("1") ✓
```

### Back Press Behavior

```
Back press order:
1. Pop from current tab's stack (if not at root)
2. If at tab root AND not primary tab → Switch to primary tab
3. If at primary tab root → Exit app / propagate to parent

// This is handled automatically by the NavNode tree structure
```

---

## Tab Wrapper API Details

### tabWrapper Signature

```kotlin
tabWrapper: @Composable (
    tabNode: TabNode,                    // Current tab state
    tabContent: @Composable () -> Unit   // Active tab's content
) -> Unit
```

### Available from TabNode

```kotlin
tabNode.id                    // "mainTabs"
tabNode.activeStackIndex      // 0, 1, 2...
tabNode.stacks                // List<StackNode> - all tab stacks
tabNode.activeStack           // Current StackNode (stacks[activeStackIndex])
```

### User Responsibilities

- Scaffold layout
- Top/bottom bars
- Tab selection UI
- Calling `tabContent()` in the right place

### Library Responsibilities

- Providing correct content for active tab
- Managing tab state transitions
- Handling predictive back
- State preservation

### Full tabWrapper Example

```kotlin
tabWrapper = { tabNode, tabContent ->
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(MainTabsMetadata.tabs[tabNode.activeStackIndex].label) 
                }
            )
        },
        bottomBar = {
            NavigationBar {
                MainTabsMetadata.tabs.forEachIndexed { index, tabInfo ->
                    NavigationBarItem(
                        icon = { Icon(tabInfo.icon, contentDescription = tabInfo.label) },
                        label = { Text(tabInfo.label) },
                        selected = tabNode.activeStackIndex == index,
                        onClick = { navigator.switchTab(tabInfo.tab) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            tabContent()  // IMPORTANT: Must be called!
        }
    }
}
```

---

## Common Pitfalls

| Pitfall | Symptom | Solution |
|---------|---------|----------|
| **Forgetting `tabWrapper` parameter** | Tabs don't render, no bottom bar | Add `tabWrapper` to `QuoVadisHost` — it's required for tabs |
| **Not calling `tabContent()`** | Empty screen, no tab content | Wrapper MUST invoke `tabContent()` somewhere in its body |
| **Using wrong index for selection** | Wrong tab highlighted | Use `tabNode.activeStackIndex`, not manual tracking |
| **Expecting tab state in Navigator** | Compiler error or null | Use `tabNode` parameter in wrapper, not navigator state |
| **Missing `@Destination` on tab items** | Deep linking doesn't work | Each `@TabItem` also needs `@Destination(route = "...")` |
| **Using old `tabState.selectTab()`** | Unresolved reference | Use `navigator.switchTab(Tab)` instead |
| **Forgetting to update `startDestination`** | Wrong format error | Use class name (`"Feed"`) not route (`"home/feed"`) |
| **Using `TabDefinition` interface** | Type mismatch | Extend `Destination` base class instead |
| **Manual graph mapping still present** | Redundant code, potential conflicts | Remove `tabGraphs` map — KSP handles it |
| **Using `TabNavigatorConfig.allTabs`** | Unresolved reference | Use `MainTabsMetadata.tabs` instead |

### Debugging Tips

1. **Check `tabWrapper` is provided**: `QuoVadisHost` without `tabWrapper` won't render tabs
2. **Verify `tabContent()` is called**: Add logging inside wrapper to confirm
3. **Log `tabNode.activeStackIndex`**: Verify it changes on tab switch
4. **Check generated code**: Review `MainTabsMetadata.kt` for tab info
5. **Build clean**: `./gradlew clean build` forces KSP regeneration

---

## Next Steps

After migrating tabbed navigation:

- **[04-adaptive-panes.md](./04-adaptive-panes.md)** — Migrate adaptive multi-pane layouts
- **[05-nested-tabs-detail.md](./05-nested-tabs-detail.md)** — Migrate complex nested tabs with detail screens

---

## Related Resources

- [01-simple-stack.md](./01-simple-stack.md) — Prerequisite: Simple stack migration guide
- [02-master-detail.md](./02-master-detail.md) — Master-detail pattern migration
- [API Change Summary](./api-change-summary.md) — Complete annotation and API reference
- [Phase 1: NavNode Architecture](../refactoring-plan/phase1-core/CORE-001-navnode-hierarchy.md) — NavNode type definitions
- [Phase 2: QuoVadisHost](../refactoring-plan/phase2-renderer/RENDER-004-quovadis-host.md) — Unified renderer details
- [Phase 2: User Wrapper API](../refactoring-plan/phase2-renderer/RENDER-008-user-wrapper-api.md) — tabWrapper design
- [Phase 4: Annotations](../refactoring-plan/phase4-annotations/) — Full annotation specifications
- [MIG-003 Spec](../refactoring-plan/phase5-migration/MIG-003-tabbed-navigation-example.md) — Original task specification

```
