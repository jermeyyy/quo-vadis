package com.jermey.navplayground.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.navplayground.demo.destinations.DemoDestination
import com.jermey.navplayground.demo.graphs.demoRootGraph
import com.jermey.quo.vadis.core.navigation.compose.NavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

@Composable
fun DemoApp() {
    val navigator = rememberNavigator()
    val demoGraph = remember { demoRootGraph() }

    LaunchedEffect(navigator, demoGraph) {
        navigator.registerGraph(demoGraph)
        navigator.setStartDestination(DemoDestination.Root)
    }

    NavHost(
        graph = demoGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.ScaleIn,
        enablePredictiveBack = true
    )
}
