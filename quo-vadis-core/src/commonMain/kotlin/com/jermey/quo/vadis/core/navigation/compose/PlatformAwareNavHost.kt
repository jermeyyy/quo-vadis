package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.navplayground.navigation.core.Navigator
import com.jermey.navplayground.navigation.core.NavigationGraph
import com.jermey.navplayground.navigation.core.NavigationTransition

/**
 * Common interface for NavHost across all platforms.
 */
@Composable
expect fun PlatformAwareNavHost(
    graph: NavigationGraph,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition,
    enablePredictiveBack: Boolean = true
)

