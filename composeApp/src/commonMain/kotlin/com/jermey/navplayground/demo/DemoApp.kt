package com.jermey.navplayground.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.graphs.appRootGraph
import com.jermey.quo.vadis.core.navigation.compose.NavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

/**
 * Main entry point for the demo application.
 *
 * Uses a single-level navigation structure where all destinations (including main screens
 * with Scaffold) are defined in the same graph. This ensures predictive back animations
 * work correctly by caching complete screen structures.
 */
@Composable
fun DemoApp() {
    val navigator = rememberNavigator()
    val appGraph = remember<NavigationGraph> { appRootGraph() }

    LaunchedEffect(navigator, appGraph) {
        navigator.registerGraph(appGraph)
        navigator.setStartDestination(MainDestination.Home)
        setupDemoDeepLinks(navigator)
    }

    NavHost(
        graph = appGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.Fade,
        enablePredictiveBack = true
    )
}
