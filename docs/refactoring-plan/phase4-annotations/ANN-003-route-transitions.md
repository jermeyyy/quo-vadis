# Task ANN-003: Define @Tab and @TabItem Annotations

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-003 |
| **Name** | Define @Tab and @TabItem Annotations |
| **Phase** | 3 - Annotations |
| **Complexity** | Medium |
| **Estimated Time** | 1 day |
| **Dependencies** | ANN-001, ANN-002 |

## Overview

Define the `@Tab` and `@TabItem` annotations for tabbed navigation containers with parallel stacks. A `@Tab`-annotated sealed class becomes a `TabNode` in the navigation tree, where each sealed subclass represents a tab with its own independent navigation stack.

The `@Tab` annotation marks the container, while `@TabItem` provides metadata for each tab including its label, icon, and root graph reference.

## Implementation

### File Location

`quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`

### Annotation Definitions

```kotlin
package com.jermey.quo.vadis.annotations

import kotlin.reflect.KClass

/**
 * Marks a sealed class/interface as a tabbed navigation container.
 *
 * Each sealed subclass represents a tab with its own independent navigation stack.
 * The container is transformed into a [TabNode] in the navigation tree, with each
 * tab maintaining its own [StackNode] for back navigation.
 *
 * Tabs support:
 * - Independent back stacks per tab
 * - State preservation when switching tabs
 * - Deep linking to specific tabs
 * - Configurable initial tab selection
 *
 * @param name Unique identifier for this tab container. Used in route generation
 *             and navigation tree construction (e.g., "mainTabs", "settingsTabs").
 * @param initialTab Simple name of the initially selected tab. Must match one of
 *                   the sealed subclass names. If empty, the first tab in
 *                   declaration order is selected.
 *
 * @sample Basic tab container
 * ```kotlin
 * @Tab(name = "mainTabs", initialTab = "Home")
 * sealed class MainTabs : Destination {
 *
 *     @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     @Destination(route = "tabs/home")
 *     data object Home : MainTabs()
 *
 *     @TabItem(label = "Search", icon = "search", rootGraph = SearchDestination::class)
 *     @Destination(route = "tabs/search")
 *     data object Search : MainTabs()
 *
 *     @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
 *     @Destination(route = "tabs/profile")
 *     data object Profile : MainTabs()
 * }
 * ```
 *
 * @sample Generated TabNode structure
 * ```kotlin
 * // KSP generates: buildMainTabsNavNode()
 * TabNode(
 *     key = "mainTabs",
 *     parentKey = null,
 *     stacks = listOf(
 *         StackNode(key = "mainTabs/home", parentKey = "mainTabs", children = [...]),
 *         StackNode(key = "mainTabs/search", parentKey = "mainTabs", children = [...]),
 *         StackNode(key = "mainTabs/profile", parentKey = "mainTabs", children = [...])
 *     ),
 *     activeStackIndex = 0  // Home is initial
 * )
 * ```
 *
 * @see TabItem
 * @see Destination
 * @see Stack
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Tab(
    /**
     * Unique name for this tab container.
     */
    val name: String,

    /**
     * Simple name of the initially selected tab.
     * Must match one of the sealed subclass names.
     * If empty, the first tab in declaration order is selected.
     */
    val initialTab: String = ""
)

/**
 * Provides metadata for a tab within a [@Tab] container.
 *
 * Applied to sealed subclasses of a [@Tab]-annotated class to configure
 * the tab's display properties and navigation content. Each tab item
 * references a root graph (a [@Stack]-annotated sealed class) that defines
 * the navigation destinations available within that tab.
 *
 * @param label Display label for the tab shown in the tab bar UI.
 *              Should be localized for internationalization support.
 * @param icon Icon identifier for the tab. Platform-specific interpretation:
 *             - Android: Material icon name (e.g., "home", "search", "person")
 *               or drawable resource reference
 *             - iOS: SF Symbol name
 *             - Desktop/Web: Icon library identifier
 * @param rootGraph The root navigation graph class for this tab's content.
 *                  Must be a sealed class annotated with [@Stack]. The stack's
 *                  start destination becomes the tab's initial screen.
 *
 * @sample Tab with icon and root graph
 * ```kotlin
 * @Tab(name = "main", initialTab = "Home")
 * sealed class MainTabs : Destination {
 *
 *     @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
 *     @Destination(route = "tab/home")
 *     data object Home : MainTabs()
 *
 *     @TabItem(label = "Favorites", icon = "favorite", rootGraph = FavoritesDestination::class)
 *     @Destination(route = "tab/favorites")
 *     data object Favorites : MainTabs()
 *
 *     @TabItem(label = "Settings", icon = "settings", rootGraph = SettingsDestination::class)
 *     @Destination(route = "tab/settings")
 *     data object Settings : MainTabs()
 * }
 *
 * // Referenced stack graph
 * @Stack(name = "home", startDestination = "Feed")
 * sealed class HomeDestination : Destination {
 *     @Destination(route = "home/feed")
 *     data object Feed : HomeDestination()
 *
 *     @Destination(route = "home/article/{id}")
 *     data class Article(val id: String) : HomeDestination()
 * }
 * ```
 *
 * @sample Nested tabs with deep linking
 * ```kotlin
 * @Tab(name = "dashboard")
 * sealed class DashboardTabs : Destination {
 *
 *     @TabItem(label = "Overview", icon = "dashboard", rootGraph = OverviewGraph::class)
 *     @Destination(route = "dashboard/overview")
 *     data object Overview : DashboardTabs()
 *
 *     @TabItem(label = "Analytics", icon = "analytics", rootGraph = AnalyticsGraph::class)
 *     @Destination(route = "dashboard/analytics")
 *     data object Analytics : DashboardTabs()
 * }
 *
 * // Deep link: app://dashboard/analytics/report/123
 * // Opens Analytics tab with ReportDetail screen
 * ```
 *
 * @see Tab
 * @see Stack
 * @see Destination
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class TabItem(
    /**
     * Display label for the tab.
     */
    val label: String,

    /**
     * Icon identifier (platform-specific).
     * On Android: Material icon name or resource reference.
     * On iOS: SF Symbol name.
     */
    val icon: String = "",

    /**
     * The root graph class for this tab's content.
     * Must be a sealed class annotated with @Stack.
     */
    val rootGraph: KClass<*>
)
```

## Usage Examples

### Basic Tab Navigation

```kotlin
// Define the tab container
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : Destination {

    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()

    @TabItem(label = "Search", icon = "search", rootGraph = SearchDestination::class)
    @Destination(route = "tabs/search")
    data object Search : MainTabs()

    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tabs/profile")
    data object Profile : MainTabs()
}

// Define each tab's stack
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/article/{articleId}")
    data class Article(val articleId: String) : HomeDestination()
}

@Stack(name = "search", startDestination = "SearchMain")
sealed class SearchDestination : Destination {
    @Destination(route = "search/main")
    data object SearchMain : SearchDestination()

    @Destination(route = "search/results/{query}")
    data class Results(val query: String) : SearchDestination()
}

@Stack(name = "profile", startDestination = "ProfileMain")
sealed class ProfileDestination : Destination {
    @Destination(route = "profile/main")
    data object ProfileMain : ProfileDestination()

    @Destination(route = "profile/settings")
    data object Settings : ProfileDestination()
}
```

### App Entry Point

```kotlin
@Composable
fun App() {
    // KSP-generated function builds the TabNode tree
    val navTree = remember { buildMainTabsNavNode() }
    val navigator = rememberNavigator(navTree)

    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry
    )
}
```

### Tab Navigation Actions

```kotlin
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    Column {
        // Navigate within current tab's stack
        Button(onClick = { navigator.navigate(HomeDestination.Article("123")) }) {
            Text("View Article")
        }

        // Switch to another tab
        Button(onClick = { navigator.switchTab(MainTabs.Search) }) {
            Text("Go to Search")
        }
    }
}
```

## Generated Code

KSP generates the following from `@Tab` annotations:

```kotlin
// Generated: MainTabsNavNode.kt

/**
 * Builds the navigation tree for MainTabs.
 */
fun buildMainTabsNavNode(): TabNode {
    return TabNode(
        key = "mainTabs",
        parentKey = null,
        stacks = listOf(
            buildHomeDestinationNavNode().copy(parentKey = "mainTabs"),
            buildSearchDestinationNavNode().copy(parentKey = "mainTabs"),
            buildProfileDestinationNavNode().copy(parentKey = "mainTabs")
        ),
        activeStackIndex = 0,  // Home is initial
        tabMetadata = listOf(
            TabMetadata(label = "Home", icon = "home"),
            TabMetadata(label = "Search", icon = "search"),
            TabMetadata(label = "Profile", icon = "person")
        )
    )
}

/**
 * Tab metadata for UI rendering.
 */
data class TabMetadata(
    val label: String,
    val icon: String
)
```

## Files Affected

| File | Change Type | Description |
|------|-------------|-------------|
| `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt` | New | @Tab and @TabItem annotation definitions |

## Acceptance Criteria

- [ ] `@Tab` annotation created with `name` and `initialTab` parameters
- [ ] `@TabItem` annotation created with `label`, `icon`, and `rootGraph` parameters
- [ ] Both annotations have `@Target(AnnotationTarget.CLASS)`
- [ ] Both annotations have `@Retention(AnnotationRetention.SOURCE)`
- [ ] Comprehensive KDoc documentation with examples
- [ ] `initialTab` defaults to empty string (first tab selected)
- [ ] `icon` parameter is optional with empty string default
- [ ] `rootGraph` parameter accepts `KClass<*>` for stack references

## References

- [INDEX.md](../INDEX.md) - Overall refactoring plan
- [CORE-001: NavNode Hierarchy](../phase1-core/CORE-001-navnode-hierarchy.md) - TabNode definition
- [KSP-002: Class References Generator](../phase3-ksp/KSP-002-class-references.md) - Code generation details
