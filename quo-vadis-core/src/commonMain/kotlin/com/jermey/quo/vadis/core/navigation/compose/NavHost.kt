package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition

/**
 * Platform-aware NavHost that supports predictive back navigation on all platforms.
 *
 * This composable automatically switches between [PredictiveBackNavigation] and [GraphNavHost]
 * based on the [enablePredictiveBack] parameter.
 *
 * @param graph The navigation graph containing destination definitions and composables
 * @param navigator The navigator instance managing navigation state
 * @param modifier Optional modifier for the NavHost
 * @param defaultTransition Default transition to use when navigating between destinations
 * @param enablePredictiveBack Whether to enable predictive back gesture support (default: true)
 */
@Composable
fun NavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition,
    enablePredictiveBack: Boolean = true
) {
    if (enablePredictiveBack) {
        // PredictiveBackNavigation handles all rendering
        PredictiveBackNavigation(
            navigator = navigator,
            graph = graph,
            enabled = true,
            modifier = modifier
        )
    } else {
        GraphNavHost(
            graph = graph,
            navigator = navigator,
            modifier = modifier,
            defaultTransition = defaultTransition
        )
    }
}

