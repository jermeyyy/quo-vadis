package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.navplayground.demo.destinations.DemoTabs
import com.jermey.quo.vadis.annotations.TabsContainer
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
import com.jermey.quo.vadis.flowmvi.rememberSharedContainer
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Tabs container wrapper for the Demo Tabs with a tab strip at the top.
 *
 * This demonstrates the @TabsContainer pattern with TabRow for switching tabs.
 * Each tab maintains its own navigation stack, allowing nested navigation
 * within each tab.
 *
 * Also demonstrates [com.jermey.quo.vadis.flowmvi.SharedNavigationContainer] usage:
 * - Shared state across all tabs (items viewed count, favorites)
 * - State that persists when switching tabs
 * - Cross-tab communication via [LocalDemoTabsStore]
 *
 * @param scope The TabsContainerScope providing access to tab state and navigation
 * @param content The content slot where active tab content is rendered
 */
@OptIn(ExperimentalMaterial3Api::class)
@TabsContainer(DemoTabs.Companion::class)
@Composable
fun DemoTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    // Get the shared store for cross-tab state
    val sharedStore = rememberSharedContainer<DemoTabsContainer, DemoTabsState, DemoTabsIntent, DemoTabsAction>()
    val state by sharedStore.subscribe()

    CompositionLocalProvider(LocalDemoTabsStore provides sharedStore) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        // Show items viewed count in the title
                        Text("Tabs Demo (${state.totalItemsViewed} viewed)")
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.navigator.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Tab strip + content inside scaffold padding
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tab strip with badges showing favorites count per category
                TabRow(
                    selectedTabIndex = scope.activeTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    scope.tabMetadata.forEachIndexed { index, meta ->
                        val favoriteCount = getFavoriteCountForTab(meta.route, state.favoriteItems)
                        Tab(
                            selected = scope.activeTabIndex == index,
                            onClick = { scope.switchTab(index) },
                            enabled = !scope.isTransitioning,
                            text = { Text(meta.label) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (favoriteCount > 0) {
                                            Badge { Text(favoriteCount.toString()) }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = getTabIcon(meta.route),
                                        contentDescription = meta.contentDescription ?: meta.label
                                    )
                                }
                            }
                        )
                    }
                }

                // Tab content
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Maps route to a Material icon for the demo tabs.
 *
 * @param route The tab route identifier
 * @return An ImageVector icon for the route
 */
private fun getTabIcon(route: String): ImageVector = when {
    route.contains("music", ignoreCase = true) -> Icons.Default.MusicNote
    route.contains("movies", ignoreCase = true) -> Icons.Default.Movie
    route.contains("books", ignoreCase = true) -> Icons.AutoMirrored.Default.MenuBook
    else -> Icons.Default.MusicNote // Fallback
}

/**
 * Counts favorites for a specific tab based on the route and favorite item IDs.
 *
 * @param route The tab route identifier
 * @param favoriteItems List of favorited item IDs
 * @return Count of favorites for this tab
 */
private fun getFavoriteCountForTab(route: String, favoriteItems: List<String>): Int = when {
    route.contains("music", ignoreCase = true) -> favoriteItems.count { it.startsWith("music_") }
    route.contains("movies", ignoreCase = true) -> favoriteItems.count { it.startsWith("movie_") }
    route.contains("books", ignoreCase = true) -> favoriteItems.count { it.startsWith("book_") }
    else -> 0
}
