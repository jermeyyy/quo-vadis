package com.jermey.navplayground.demo.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.graphs.tabContentGraph
import com.jermey.navplayground.demo.ui.components.BottomNavigationBar
import com.jermey.quo.vadis.core.navigation.compose.TabTransitionSpec
import com.jermey.quo.vadis.core.navigation.compose.TabbedNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberTabNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Main tab container with bottom navigation bar.
 *
 * This composable uses the generated [MainTabsContainer] from the @TabGraph annotation
 * on [MainTabs]. It provides a complete tabbed navigation experience with:
 * - Independent navigation stacks per tab
 * - State preservation across tab switches
 * - Automatic back press delegation
 * - All demo navigation graphs included
 *
 * Important: Uses tabContentGraph (not appRootGraph) to avoid infinite recomposition.
 * The parent navigator uses appRootGraph which only contains MainTabs.
 * Tab navigators use tabContentGraph which contains all navigable destinations.
 *
 * @param parentNavigator The parent navigator for back press delegation
 * @param modifier Modifier to be applied to the root container
 */
@Composable
fun MainTabsScreen(
    parentNavigator: Navigator,
    modifier: Modifier = Modifier
) {
    println("DEBUG_TAB_NAV: MainTabsScreen - Starting composition")
    
    // Build tab content graph - includes all destinations accessible from tabs
    // This is separate from appRootGraph to prevent conflicts
    val tabGraph = remember<NavigationGraph> { 
        println("DEBUG_TAB_NAV: MainTabsScreen - Creating tabContentGraph")
        tabContentGraph() 
    }

    println("DEBUG_TAB_NAV: MainTabsScreen - tabGraph destinations: ${tabGraph.destinations.size}")
    println("DEBUG_TAB_NAV: MainTabsScreen - tabGraph route: ${tabGraph.graphRoute}")

    println("DEBUG_TAB_NAV: MainTabsScreen - About to render MainTabsContainer")
    
    // Create tab state
    val tabState = rememberTabNavigator(MainTabsConfig, parentNavigator)
    val selectedTab by tabState.selectedTab.collectAsState()
    
    // Use TabbedNavHost with custom bottom navigation UI
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = MainTabsConfig.allTabs.associateWith { tabGraph },
        navigator = parentNavigator,
        modifier = modifier,
        tabUI = @Composable { content ->
            // Wrap content with Scaffold and BottomNavigationBar
            Scaffold(
                modifier = Modifier.consumeWindowInsets(
                    WindowInsets()
                        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                ),
                bottomBar = {
                    val currentDest = tabState.selectedTab.collectAsState().value
                    // Get the route string from the tab's root destination
                    val currentRoute = currentDest?.rootDestination?.let { dest ->
                        when (dest) {
                            is com.jermey.navplayground.demo.destinations.TabDestination.Home -> "home"
                            is com.jermey.navplayground.demo.destinations.TabDestination.Explore -> "explore"
                            is com.jermey.navplayground.demo.destinations.TabDestination.Profile -> "profile"
                            is com.jermey.navplayground.demo.destinations.TabDestination.Settings -> "settings"
                            else -> null
                        }
                    }
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { destination ->
                            println("DEBUG_TAB_NAV: BottomNav clicked - destination: ${destination::class.simpleName}")
                            // Switch to the tab that contains this destination
                            val tab = MainTabsConfig.allTabs.find { it.rootDestination == destination }
                            println("DEBUG_TAB_NAV: Found tab: ${tab?.route}")
                            if (tab != null) {
                                println("DEBUG_TAB_NAV: Selecting tab: ${tab.route}")
                                tabState.selectTab(tab)
                            } else {
                                println("DEBUG_TAB_NAV: WARNING - No tab found for destination:" +
                                        " ${destination::class.simpleName}")
                            }
                        }
                    )
                }
            ) { paddingValues ->
                // Render the tab content inside Scaffold
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    content()
                }
            }
        },
        tabTransitionSpec = TabTransitionSpec.Crossfade,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
    
    println("DEBUG_TAB_NAV: MainTabsScreen - MainTabsContainer rendered")
}
