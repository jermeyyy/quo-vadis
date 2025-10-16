package com.jermey.navplayground.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.graphs.appRootGraph
import com.jermey.quo.vadis.core.navigation.compose.GraphNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

/**
 * Main entry point for the demo application.
 *
 * Uses unified GraphNavHost with support for:
 * - Animated forward/back navigation with correct directional transitions
 * - Predictive back gesture animations
 * - Composable caching for smooth transitions
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

    GraphNavHost(
        graph = appGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal,
        enablePredictiveBack = true
        // Note: SharedTransitionLayout is always enabled. Use destinationWithScopes() to opt-in per destination.
    )
}
