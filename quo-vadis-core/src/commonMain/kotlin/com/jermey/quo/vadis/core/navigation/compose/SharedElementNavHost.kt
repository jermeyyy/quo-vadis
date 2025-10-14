package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Navigation host with shared element transition support.
 * Wraps the underlying NavHost in a SharedTransitionLayout.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedElementNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition,
    enablePredictiveBack: Boolean = true
) {
    // Wrap in SharedTransitionLayout for coordinated shared element animations
    SharedTransitionLayout(modifier = modifier) {
        // Use regular NavHost inside
        NavHost(
            graph = graph,
            navigator = navigator,
            modifier = Modifier,
            defaultTransition = defaultTransition,
            enablePredictiveBack = enablePredictiveBack
        )
    }
}
