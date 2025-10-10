package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.navplayground.navigation.compose.GraphNavHost
import com.jermey.navplayground.navigation.compose.PredictiveBackNavigation
import com.jermey.navplayground.navigation.core.Navigator
import com.jermey.navplayground.navigation.core.NavigationGraph
import com.jermey.navplayground.navigation.core.NavigationTransition

/**
 * Android implementation of PlatformAwareNavHost with predictive back support.
 */
@Composable
actual fun PlatformAwareNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier,
    defaultTransition: NavigationTransition,
    enablePredictiveBack: Boolean
) {
    if (enablePredictiveBack) {
        // Wrap with multiplatform predictive back handler
        PredictiveBackNavigation(
            navigator = navigator,
            enabled = true,
            modifier = modifier
        ) {
            GraphNavHost(
                graph = graph,
                navigator = navigator,
                modifier = Modifier,
                defaultTransition = defaultTransition
            )
        }
    } else {
        GraphNavHost(
            graph = graph,
            navigator = navigator,
            modifier = modifier,
            defaultTransition = defaultTransition
        )
    }
}
