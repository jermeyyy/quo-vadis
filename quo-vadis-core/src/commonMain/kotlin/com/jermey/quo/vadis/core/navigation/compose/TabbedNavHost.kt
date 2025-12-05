package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TabDefinition
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState
import com.jermey.quo.vadis.core.navigation.core.TabScopedNavigator
import com.jermey.quo.vadis.core.navigation.core.backStack

/**
 * High-level composable for tabbed navigation with integrated back press handling.
 *
 * This composable provides complete tabbed navigation functionality:
 * - Independent navigation stacks per tab
 * - State preservation across tab switches
 * - Automatic back press delegation
 * - Smooth tab transition animations
 * - Integration with existing GraphNavHost
 *
 * Each tab gets its own [Navigator] instance and navigation graph, allowing
 * completely independent navigation hierarchies within each tab.
 *
 * **Usage**:
 * ```kotlin
 * val tabState = rememberTabNavigatorState(
 *     TabNavigatorConfig(
 *         allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
 *         initialTab = HomeTab,
 *         primaryTab = HomeTab
 *     )
 * )
 *
 * TabbedNavHost(
 *     tabState = tabState,
 *     modifier = Modifier.fillMaxSize(),
 *     tabGraphs = mapOf(
 *         HomeTab to homeNavigationGraph,
 *         ProfileTab to profileNavigationGraph,
 *         SettingsTab to settingsNavigationGraph
 *     )
 * )
 * ```
 *
 * @param tabState The tab navigation state managing tab selection.
 * @param tabGraphs Map of tab definitions to their navigation graphs.
 * @param modifier Modifier for the navigation host.
 * @param defaultTransition Default transition for navigation within tabs.
 * @param tabTransitionSpec Transition specification for tab switches.
 * @param enableComposableCache Whether to enable composable caching.
 * @param enablePredictiveBack Whether to enable predictive back gestures.
 * @param maxCacheSize Maximum composable cache size per tab.
 */
@Composable
fun TabbedNavHost(
    tabState: TabNavigatorState,
    tabGraphs: Map<TabDefinition, NavigationGraph>,
    modifier: Modifier = Modifier,
    tabUI: (@Composable (content: @Composable () -> Unit) -> Unit)? = null,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    tabTransitionSpec: TabTransitionSpec = TabTransitionSpec.Default,
    enableComposableCache: Boolean = true,
    enablePredictiveBack: Boolean = true,
    maxCacheSize: Int = 3,
    navigator : Navigator
) {
    println("DEBUG_TAB_NAV: TabbedNavHost - Starting composition")
    val selectedTab by tabState.selectedTab.collectAsState()
    val allTabs = tabState.getAllTabs()
    
    println("DEBUG_TAB_NAV: TabbedNavHost - Selected tab: ${selectedTab?.route}")
    println("DEBUG_TAB_NAV: TabbedNavHost - All tabs: ${allTabs.map { it.route }}")
    println("DEBUG_TAB_NAV: TabbedNavHost - Tab graphs count: ${tabGraphs.size}")

    // Create and remember navigators for all tabs
    val tabNavigators = remember(allTabs) {
        println("DEBUG_TAB_NAV: TabbedNavHost - Creating tab navigators for ${allTabs.size} tabs")
        allTabs.associateWith { tab ->
            println("DEBUG_TAB_NAV: TabbedNavHost - Creating navigator for tab: ${tab.route}")
            TabScopedNavigator(tab, tabState, navigator)
        }
    }

    // Register graphs and initialize navigators with their start destinations
    DisposableEffect(tabNavigators, tabGraphs) {
        println("DEBUG_TAB_NAV: TabbedNavHost - DisposableEffect: Registering graphs")
        tabNavigators.forEach { (tab, tabNavigator) ->
            val graph = tabGraphs[tab]
            println("DEBUG_TAB_NAV: TabbedNavHost - Tab: ${tab.route}, Graph: ${graph?.graphRoute}, Destinations: ${graph?.destinations?.size}")
            if (graph != null) {
                // Register the graph with the tab navigator
                tabNavigator.registerGraph(graph)
                println("DEBUG_TAB_NAV: TabbedNavHost - Registered graph '${graph.graphRoute}' with tab navigator for ${tab.route}")
                
                // Set start destination if navigator is empty
                // Use the tab's rootDestination, NOT the graph's startDestination
                if (tabNavigator.backStack.stack.value.isEmpty()) {
                    val startDest = tab.rootDestination
                    println("DEBUG_TAB_NAV: TabbedNavHost - Setting start destination for ${tab.route}: ${startDest::class.simpleName}")
                    tabNavigator.setStartDestination(startDest)
                } else {
                    println("DEBUG_TAB_NAV: TabbedNavHost - Tab ${tab.route} already has backstack: ${tabNavigator.backStack.stack.value.size} entries")
                }
            } else {
                println("DEBUG_TAB_NAV: TabbedNavHost - WARNING: No graph found for tab ${tab.route}")
            }
        }
        onDispose {
            println("DEBUG_TAB_NAV: TabbedNavHost - DisposableEffect disposed")
        }
    }

    // Render tab container with all tab content
    println("DEBUG_TAB_NAV: TabbedNavHost - About to render TabNavigationContainer")
    
    val tabContent: @Composable () -> Unit = {
        TabNavigationContainer(
            selectedTab = selectedTab,
            allTabs = allTabs,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = tabTransitionSpec
        ) { tab ->
            println("DEBUG_TAB_NAV: TabbedNavHost - TabNavigationContainer rendering content for tab: ${tab.route}")
            val tabNavigator = tabNavigators[tab]
            val graph = tabGraphs[tab]

            println("DEBUG_TAB_NAV: TabbedNavHost - Tab: ${tab.route}, Navigator: ${tabNavigator != null}, Graph: ${graph != null}")
            
            if (tabNavigator != null && graph != null) {
                println("DEBUG_TAB_NAV: TabbedNavHost - About to render GraphNavHost for tab ${tab.route}")
                println("DEBUG_TAB_NAV: TabbedNavHost - Graph destinations: ${graph.destinations.size}")
                println("DEBUG_TAB_NAV: TabbedNavHost - Navigator backstack size: ${tabNavigator.backStack.stack.value.size}")
                
                GraphNavHost(
                    graph = graph,
                    navigator = tabNavigator,
                    modifier = Modifier.fillMaxSize(),
                    defaultTransition = defaultTransition,
                    enableComposableCache = enableComposableCache,
                    enablePredictiveBack = enablePredictiveBack,
                    maxCacheSize = maxCacheSize
                )
            } else {
                println("DEBUG_TAB_NAV: TabbedNavHost - WARNING: Cannot render tab ${tab.route} - Navigator or Graph is null")
            }
        }
    }
    
    // Wrap content with tabUI if provided, otherwise render directly
    if (tabUI != null) {
        println("DEBUG_TAB_NAV: TabbedNavHost - Using custom tabUI wrapper")
        Box(modifier = modifier) {
            tabUI(tabContent)
        }
    } else {
        println("DEBUG_TAB_NAV: TabbedNavHost - No tabUI wrapper, rendering content directly")
        Box(modifier = modifier) {
            tabContent()
        }
    }
}

/**
 * Simplified tabbed navigation host with a single navigation graph.
 *
 * Use this when all tabs share the same navigation graph structure
 * but want independent navigation stacks.
 *
 * @param tabState The tab navigation state.
 * @param navigationGraph The navigation graph used by all tabs.
 * @param modifier Modifier for the navigation host.
 * @param defaultTransition Default transition for navigation.
 * @param tabTransitionSpec Transition specification for tab switches.
 * @param navigator The parent navigator for back press delegation.
 */
@Composable
fun TabbedNavHost(
    tabState: TabNavigatorState,
    navigationGraph: NavigationGraph,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    tabTransitionSpec: TabTransitionSpec = TabTransitionSpec.Default,
    navigator: Navigator
) {
    val allTabs = tabState.getAllTabs()
    val tabGraphs = remember(allTabs, navigationGraph) {
        allTabs.associateWith { navigationGraph }
    }

    TabbedNavHost(
        tabState = tabState,
        tabGraphs = tabGraphs,
        modifier = modifier,
        defaultTransition = defaultTransition,
        tabTransitionSpec = tabTransitionSpec,
        navigator = navigator
    )
}
