@file:Suppress("unused")

package com.jermey.quo.vadis.recipes.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapper
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
import com.jermey.quo.vadis.core.navigation.core.Navigator

// =============================================================================
// MIG-003: BOTTOM TABS NAVIGATION RECIPE
// =============================================================================

/**
 * # Bottom Tabs Navigation Recipe
 *
 * Demonstrates tabbed navigation with bottom navigation bar using the
 * `@Tab`, `@TabItem`, and `tabWrapper` pattern from the Quo Vadis library.
 *
 * ## What This Recipe Shows
 *
 * 1. **Tab Container Definition** - Using `@Tab` on sealed class
 * 2. **Tab Items** - Using `@TabItem` + `@Destination` on each tab subclass
 * 3. **Per-Tab Stacks** - Using `@Stack` for each tab's navigation history
 * 4. **Screen Binding** - Using `@Screen` to map destinations to composables
 * 5. **TabWrapper Pattern** - User-controlled scaffold with library-rendered content
 * 6. **Tab Switching** - Using `navigator.switchTab(index)` API
 *
 * ## Architecture Overview
 *
 * ```
 * @Tab MainTabs
 * ├── @TabItem Home → @Stack HomeDestination
 * │   ├── Feed (start)
 * │   └── ArticleDetail
 * ├── @TabItem Search → @Stack SearchDestination
 * │   ├── SearchMain (start)
 * │   └── SearchResults
 * └── @TabItem Profile → @Stack ProfileDestination
 *     ├── ProfileMain (start)
 *     └── Settings
 * ```
 *
 * ## Key Concepts
 *
 * ### Tab Container (`@Tab`)
 *
 * The `@Tab` annotation marks a sealed class as a tabbed navigation container.
 * Each sealed subclass becomes a tab with its own independent navigation stack.
 *
 * ### Tab Items (`@TabItem` + `@Destination`)
 *
 * Each tab subclass has TWO annotations:
 * - `@TabItem` - Provides UI metadata (label, icon) and links to root graph
 * - `@Destination` - Enables deep linking to this tab
 *
 * ### TabWrapper Pattern
 *
 * The `tabWrapper` parameter lets YOU control the scaffold structure:
 * - You build the `Scaffold`, `TopAppBar`, `NavigationBar`, etc.
 * - Library provides `tabContent()` lambda with the active tab's content
 * - `TabWrapperScope` gives access to `activeTabIndex`, `tabMetadata`, `switchTab()`
 *
 * ## Production App Setup
 *
 * ```kotlin
 * @Composable
 * fun TabbedApp() {
 *     // KSP generates buildMainTabsNavNode() from @Tab annotations
 *     val navTree = remember { buildMainTabsNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         tabWrapper = bottomTabsWrapper()
 *     )
 * }
 * ```
 *
 * @see MainTabs Tab container definition
 * @see HomeDestination Per-tab stack example
 * @see bottomTabsWrapper TabWrapper implementation
 * @see com.jermey.quo.vadis.annotations.Tab
 * @see com.jermey.quo.vadis.annotations.TabItem
 * @see com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
 */

// =============================================================================
// TAB CONTAINER DEFINITION
// =============================================================================

/**
 * Main tabs container defining the bottom navigation structure.
 *
 * The `@Tab` annotation creates a [TabNode] in the navigation tree with:
 * - A [StackNode] for each tab (maintaining independent back stacks)
 * - Tab metadata (labels, icons) for UI rendering
 * - Deep link support for each tab
 *
 * ## How It Works
 *
 * 1. `@Tab` on sealed class → Creates TabNode container
 * 2. `@TabItem` on subclass → Configures tab UI and links to content graph
 * 3. `@Destination` on subclass → Enables deep linking to tab
 *
 * ## Generated Code (KSP)
 *
 * ```kotlin
 * // KSP generates:
 * fun buildMainTabsNavNode(): TabNode = TabNode(
 *     key = "mainTabs",
 *     stacks = listOf(
 *         buildHomeNavNode(),    // Home tab stack
 *         buildSearchNavNode(),  // Search tab stack
 *         buildProfileNavNode()  // Profile tab stack
 *     ),
 *     activeStackIndex = 0  // Home is initial tab
 * )
 * ```
 *
 * ## Deep Linking
 *
 * Each tab supports deep links via `@Destination(route = ...)`:
 * - `myapp://tabs/home` → Opens Home tab
 * - `myapp://tabs/search` → Opens Search tab
 * - `myapp://tabs/profile` → Opens Profile tab
 *
 * @see TabItem
 * @see HomeDestination
 * @see SearchDestination
 * @see ProfileDestination
 */
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : DestinationInterface {

    /**
     * Home tab - the initial tab shown when the app opens.
     *
     * ## Annotations Explained
     *
     * - `@TabItem(label, icon, rootGraph)` - UI metadata and content source
     *   - `label` - Display text in navigation bar
     *   - `icon` - Icon identifier (Material icon name)
     *   - `rootGraph` - The `@Stack` class providing this tab's content
     *
     * - `@Destination(route)` - Deep link route for this tab
     *
     * ## Deep Link
     *
     * Route: `myapp://tabs/home`
     */
    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()

    /**
     * Search tab for content discovery.
     *
     * ## Deep Link
     *
     * Route: `myapp://tabs/search`
     */
    @TabItem(label = "Search", icon = "search", rootGraph = SearchDestination::class)
    @Destination(route = "tabs/search")
    data object Search : MainTabs()

    /**
     * Profile tab for user account and settings.
     *
     * ## Deep Link
     *
     * Route: `myapp://tabs/profile`
     */
    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tabs/profile")
    data object Profile : MainTabs()
}

// =============================================================================
// PER-TAB NAVIGATION STACKS
// =============================================================================

/**
 * Home tab navigation stack with feed and article detail screens.
 *
 * Each tab has its own `@Stack` defining:
 * - Independent navigation history (back stack)
 * - Start destination shown when tab is selected
 * - Destinations navigable within this tab
 *
 * ## Navigation Flow
 *
 * ```
 * Feed (start) → ArticleDetail → (back) → Feed
 * ```
 *
 * ## State Preservation
 *
 * When switching tabs:
 * - Each tab's stack state is preserved
 * - Returning to a tab restores its exact navigation state
 * - Back navigation only affects the current tab's stack
 *
 * @see MainTabs.Home
 */
@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : DestinationInterface {

    /**
     * Home feed screen - start destination for Home tab.
     *
     * Shows a list of articles/content items.
     */
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    /**
     * Article detail screen - navigated from feed items.
     *
     * @Argument annotation marks navigation arguments explicitly for KSP processing.
     *
     * @property articleId Unique identifier for the article
     */
    @Destination(route = "home/article/{articleId}")
    data class ArticleDetail(
        @Argument val articleId: String
    ) : HomeDestination()
}

/**
 * Search tab navigation stack with search and results screens.
 *
 * ## Navigation Flow
 *
 * ```
 * SearchMain (start) → SearchResults → (back) → SearchMain
 * ```
 *
 * @see MainTabs.Search
 */
@Stack(name = "search", startDestination = "SearchMain")
sealed class SearchDestination : DestinationInterface {

    /**
     * Search main screen - start destination with search input.
     */
    @Destination(route = "search/main")
    data object SearchMain : SearchDestination()

    /**
     * Search results screen - shows results for a query.
     *
     * @Argument annotation marks navigation arguments explicitly for KSP processing.
     *
     * @property query The search query string
     */
    @Destination(route = "search/results/{query}")
    data class SearchResults(
        @Argument val query: String
    ) : SearchDestination()
}

/**
 * Profile tab navigation stack with profile and settings screens.
 *
 * ## Navigation Flow
 *
 * ```
 * ProfileMain (start) → Settings → (back) → ProfileMain
 * ```
 *
 * @see MainTabs.Profile
 */
@Stack(name = "profile", startDestination = "ProfileMain")
sealed class ProfileDestination : DestinationInterface {

    /**
     * Profile main screen - start destination showing user info.
     */
    @Destination(route = "profile/main")
    data object ProfileMain : ProfileDestination()

    /**
     * Settings screen - app and account settings.
     */
    @Destination(route = "profile/settings")
    data object Settings : ProfileDestination()
}

// =============================================================================
// TAB WRAPPER PATTERN
// =============================================================================

/**
 * Creates a [TabWrapper] with Material 3 bottom navigation bar.
 *
 * ## TabWrapper Pattern
 *
 * The tabWrapper pattern gives YOU full control over the scaffold while
 * the library handles content rendering. Inside the wrapper lambda,
 * `this` is a [TabWrapperScope] providing:
 *
 * | Property/Method | Description |
 * |-----------------|-------------|
 * | `activeTabIndex` | Currently selected tab (0-based) |
 * | `tabCount` | Total number of tabs |
 * | `tabMetadata` | List of [TabMetadata] for UI |
 * | `isTransitioning` | Whether tab switch animation is in progress |
 * | `switchTab(index)` | Switch to tab by index |
 * | `switchTab(route)` | Switch to tab by route |
 * | `navigator` | Navigator instance |
 *
 * ## Example Usage
 *
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     screenRegistry = GeneratedScreenRegistry,
 *     tabWrapper = bottomTabsWrapper()
 * )
 * ```
 *
 * ## Customization
 *
 * You can fully customize the wrapper:
 * - Add TopAppBar
 * - Use NavigationRail instead of NavigationBar
 * - Add FAB or other scaffold elements
 * - Apply custom themes/colors
 *
 * ```kotlin
 * val customWrapper: TabWrapper = { tabContent ->
 *     Scaffold(
 *         topBar = { TopAppBar(title = { Text("My App") }) },
 *         bottomBar = { /* your navigation bar */ },
 *         floatingActionButton = { /* your FAB */ }
 *     ) { padding ->
 *         Box(Modifier.padding(padding)) {
 *             tabContent()  // MUST call this!
 *         }
 *     }
 * }
 * ```
 *
 * @return TabWrapper for use with QuoVadisHost
 * @see TabWrapperScope
 * @see com.jermey.quo.vadis.core.navigation.compose.wrapper.TabMetadata
 */
fun bottomTabsWrapper(): TabWrapper = { tabContent ->
    Scaffold(
        bottomBar = {
            NavigationBar {
                // tabMetadata and activeTabIndex from TabWrapperScope
                tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        icon = { Text(getTabIcon(index)) },
                        label = { Text(meta.label) },
                        selected = activeTabIndex == index,
                        onClick = { switchTab(index) },
                        enabled = !isTransitioning
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Library renders active tab content here
            tabContent()
        }
    }
}

/**
 * Returns emoji icon for tab index (demo purposes).
 *
 * In production, use [TabMetadata.icon] which provides ImageVector
 * from the `@TabItem(icon = "...")` annotation.
 *
 * @param index Tab index (0-based)
 * @return Emoji representing the tab
 */
private fun getTabIcon(index: Int): String = when (index) {
    0 -> "🏠"  // Home
    1 -> "🔍"  // Search
    2 -> "👤"  // Profile
    else -> "•"
}

// =============================================================================
// SCREEN COMPOSABLES
// =============================================================================

/**
 * Home feed screen - displays content feed.
 *
 * ## Screen Binding
 *
 * The `@Screen` annotation binds this composable to [HomeDestination.Feed].
 * KSP generates a registry entry for content resolution.
 *
 * ## Navigation from This Screen
 *
 * ```kotlin
 * // Navigate to article detail within Home tab
 * navigator.navigate(HomeDestination.ArticleDetail("article-123"))
 * ```
 *
 * @param navigator Navigator instance for programmatic navigation
 */
@Screen(HomeDestination.Feed::class)
@Composable
fun HomeFeedScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🏠 Home Feed",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "This is the Home tab's start destination.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Navigate within Home tab
        androidx.compose.material3.Button(
            onClick = { navigator.navigate(HomeDestination.ArticleDetail("sample-article")) }
        ) {
            Text("View Article Detail")
        }

        // Switch to different tab
        androidx.compose.material3.OutlinedButton(
            onClick = { navigator.switchTab(2) }  // Switch to Profile tab
        ) {
            Text("Go to Profile Tab")
        }
    }
}

/**
 * Article detail screen - displays single article content.
 *
 * ## Destination with Parameters
 *
 * This screen receives [HomeDestination.ArticleDetail] which contains
 * the `articleId` parameter extracted from the route template.
 *
 * @param destination The destination instance containing article ID
 * @param navigator Navigator instance for programmatic navigation
 */
@Screen(HomeDestination.ArticleDetail::class)
@Composable
fun ArticleDetailScreen(
    destination: HomeDestination.ArticleDetail,
    navigator: Navigator
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📄 Article Detail",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Article ID: ${destination.articleId}",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Button(
            onClick = { navigator.navigateBack() }
        ) {
            Text("← Back to Feed")
        }
    }
}

/**
 * Search main screen - search input interface.
 *
 * @param navigator Navigator instance for programmatic navigation
 */
@Screen(SearchDestination.SearchMain::class)
@Composable
fun SearchMainScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🔍 Search",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "This is the Search tab's start destination.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Navigate to search results within Search tab
        androidx.compose.material3.Button(
            onClick = { navigator.navigate(SearchDestination.SearchResults("kotlin navigation")) }
        ) {
            Text("Search for 'kotlin navigation'")
        }
    }
}

/**
 * Search results screen - displays search results.
 *
 * @param destination The destination instance containing search query
 * @param navigator Navigator instance for programmatic navigation
 */
@Screen(SearchDestination.SearchResults::class)
@Composable
fun SearchResultsScreen(
    destination: SearchDestination.SearchResults,
    navigator: Navigator
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📋 Search Results",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Query: \"${destination.query}\"",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Button(
            onClick = { navigator.navigateBack() }
        ) {
            Text("← Back to Search")
        }
    }
}

/**
 * Profile main screen - user profile information.
 *
 * @param navigator Navigator instance for programmatic navigation
 */
@Screen(ProfileDestination.ProfileMain::class)
@Composable
fun ProfileMainScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👤 Profile",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "This is the Profile tab's start destination.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Navigate within Profile tab
        androidx.compose.material3.Button(
            onClick = { navigator.navigate(ProfileDestination.Settings) }
        ) {
            Text("Open Settings")
        }

        // Switch to different tab using index
        androidx.compose.material3.OutlinedButton(
            onClick = { navigator.switchTab(0) }  // Switch to Home tab
        ) {
            Text("Go to Home Tab")
        }
    }
}

/**
 * Settings screen - app and account settings.
 *
 * @param navigator Navigator instance for programmatic navigation
 */
@Screen(ProfileDestination.Settings::class)
@Composable
fun SettingsScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "⚙️ Settings",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Configure your app preferences.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        androidx.compose.material3.Button(
            onClick = { navigator.navigateBack() }
        ) {
            Text("← Back to Profile")
        }
    }
}

// =============================================================================
// APP ENTRY POINT (Documentation)
// =============================================================================

/**
 * Entry point for the Bottom Tabs Navigation recipe.
 *
 * ## Production Usage
 *
 * In production with KSP processing enabled:
 *
 * ```kotlin
 * @Composable
 * fun TabbedApp() {
 *     // KSP-generated function creates the navigation tree
 *     val navTree = remember { buildMainTabsNavNode() }
 *
 *     // Create navigator with the initial tree
 *     val navigator = rememberNavigator(navTree)
 *
 *     // Render with QuoVadisHost and custom tabWrapper
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         tabWrapper = bottomTabsWrapper()
 *     )
 * }
 * ```
 *
 * ## TabWrapperScope API Reference
 *
 * Inside the `tabWrapper` lambda, you have access to:
 *
 * ```kotlin
 * tabWrapper = { tabContent ->
 *     // 'this' is TabWrapperScope
 *
 *     // Read-only state
 *     val current: Int = activeTabIndex        // Currently selected tab (0-based)
 *     val count: Int = tabCount                // Total number of tabs
 *     val tabs: List<TabMetadata> = tabMetadata // Tab metadata for UI
 *     val animating: Boolean = isTransitioning // Is tab switch animating?
 *     val nav: Navigator = navigator           // Navigator instance
 *
 *     // Actions
 *     switchTab(0)              // Switch to tab by index
 *     switchTab("tabs/home")    // Switch to tab by route
 *
 *     // Build your scaffold
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 tabs.forEachIndexed { index, meta ->
 *                     NavigationBarItem(
 *                         selected = current == index,
 *                         onClick = { switchTab(index) },
 *                         icon = { meta.icon?.let { Icon(it, null) } },
 *                         label = { Text(meta.label) },
 *                         enabled = !animating
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(Modifier.padding(padding)) {
 *             tabContent()  // MUST call this!
 *         }
 *     }
 * }
 * ```
 *
 * ## Key Points
 *
 * 1. **You control the scaffold** - TopAppBar, BottomBar, FAB, etc.
 * 2. **Library renders content** - `tabContent()` displays active tab
 * 3. **State from scope** - Use `activeTabIndex`, not manual tracking
 * 4. **Switch via scope** - Use `switchTab(index)` from TabWrapperScope
 * 5. **Or via navigator** - Use `navigator.switchTab(index)` from screens
 *
 * ## Navigation Operations
 *
 * | Operation | API | Effect |
 * |-----------|-----|--------|
 * | Switch tab (wrapper) | `switchTab(index)` | Changes active tab |
 * | Switch tab (screen) | `navigator.switchTab(index)` | Changes active tab |
 * | Navigate in tab | `navigator.navigate(dest)` | Pushes to current tab's stack |
 * | Back in tab | `navigator.navigateBack()` | Pops from current tab's stack |
 *
 * ## Tab State Preservation
 *
 * - Each tab maintains independent navigation history
 * - Switching tabs preserves each tab's stack state
 * - Returning to a tab shows last viewed screen
 * - Back button only affects current tab's stack
 */
@Composable
fun BottomTabsApp() {
    // Placeholder - in production, use:
    //   val navTree = remember { buildMainTabsNavNode() }
    //   val navigator = rememberNavigator(navTree)
    //   QuoVadisHost(navigator, GeneratedScreenRegistry, bottomTabsWrapper())

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bottom Tabs Recipe",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "See KDoc for production implementation",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
