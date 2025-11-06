package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.DefaultNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TabDefinition
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState
import com.jermey.quo.vadis.core.navigation.core.TabScopedNavigator

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
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    tabTransitionSpec: TabTransitionSpec = TabTransitionSpec.Default,
    enableComposableCache: Boolean = true,
    enablePredictiveBack: Boolean = true,
    maxCacheSize: Int = 3
) {
    val selectedTab by tabState.selectedTab.collectAsState()
    val allTabs = tabState.getAllTabs()

    // Create and remember navigators for all tabs
    val tabNavigators = remember(allTabs) {
        allTabs.associateWith { tab ->
            val navigator = DefaultNavigator()
            TabScopedNavigator(tab, tabState, navigator)
        }
    }

    // Initialize navigators with their start destinations
    DisposableEffect(tabNavigators, tabGraphs) {
        tabNavigators.forEach { (tab, navigator) ->
            val graph = tabGraphs[tab]
            if (graph != null && navigator.backStack.stack.value.isEmpty()) {
                navigator.setStartDestination(graph.startDestination)
            }
        }
        onDispose { }
    }

    // Render tab container with all tab content
    TabNavigationContainer(
        selectedTab = selectedTab,
        allTabs = allTabs,
        modifier = modifier,
        transitionSpec = tabTransitionSpec
    ) { tab ->
        val navigator = tabNavigators[tab]
        val graph = tabGraphs[tab]

        if (navigator != null && graph != null) {
            GraphNavHost(
                graph = graph,
                navigator = navigator,
                modifier = Modifier.fillMaxSize(),
                defaultTransition = defaultTransition,
                enableComposableCache = enableComposableCache,
                enablePredictiveBack = enablePredictiveBack,
                maxCacheSize = maxCacheSize
            )
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
 */
@Composable
fun TabbedNavHost(
    tabState: TabNavigatorState,
    navigationGraph: NavigationGraph,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    tabTransitionSpec: TabTransitionSpec = TabTransitionSpec.Default
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
        tabTransitionSpec = tabTransitionSpec
    )
}
