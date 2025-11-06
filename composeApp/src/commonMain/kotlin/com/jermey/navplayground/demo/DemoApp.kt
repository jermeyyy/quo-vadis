package com.jermey.navplayground.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.navplayground.demo.destinations.AppDestination
import com.jermey.navplayground.demo.destinations.initializeQuoVadisRoutes
import com.jermey.navplayground.demo.graphs.appRootGraph
import com.jermey.quo.vadis.core.navigation.compose.GraphNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

/**
 * Main entry point for the demo application.
 *
 * Architecture:
 * - appRootGraph contains only AppDestination.MainTabs
 * - MainTabsScreen hosts tabbed navigation with 4 tabs
 * - Each tab uses tabContentGraph for navigation within its stack
 * - MasterDetail, Process, Tabs demos open in new stacks
 * - DeepLink demo accessible via modal bottom sheet
 */
@Composable
fun DemoApp() {
    // Initialize auto-generated route registrations
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val appGraph = remember<NavigationGraph> { appRootGraph() }

    LaunchedEffect(navigator, appGraph) {
        navigator.registerGraph(appGraph)
        navigator.setStartDestination(AppDestination.MainTabs)
    }

    GraphNavHost(
        graph = appGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal,
        enablePredictiveBack = true
    )
}
