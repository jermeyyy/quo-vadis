package com.jermey.navplayground.demo.destinations

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.graphs.mainBottomNavGraph
import com.jermey.navplayground.demo.ui.components.BottomNavigationBar
import com.jermey.navplayground.demo.ui.components.NavigationBottomSheetContent
import com.jermey.quo.vadis.core.navigation.compose.NavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen() {
    val navigator = rememberNavigator()
    val mainGraph = remember { mainBottomNavGraph() }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(navigator) {
        navigator.registerGraph(mainGraph)
        navigator.setStartDestination(MainDestination.Home)
        setupDemoDeepLinks(navigator)
    }

    val currentDestination by navigator.currentDestination.collectAsState()
    val currentRoute = currentDestination?.route

    Scaffold(
        topBar = {
            if (shouldShowTopAppBar(currentDestination)) {
                TopAppBar(
                    title = { Text(getScreenTitle(currentDestination)) },
                    navigationIcon = {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomNav(currentDestination)) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination -> navigator.navigateAndReplace(destination) }
                )
            }
        }
    ) { padding ->
            NavHost(
                graph = mainGraph,
                navigator = navigator,
                modifier = Modifier.fillMaxSize(),
                defaultTransition = NavigationTransitions.Fade,
                enablePredictiveBack = true
            )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            NavigationBottomSheetContent(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    navigator.navigate(destination)
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
    }
}



private fun getScreenTitle(destination: Destination?): String {
    return when (destination?.route) {
        "home" -> "Home"
        "explore" -> "Explore"
        "profile" -> "Profile"
        "settings" -> "Settings"
        else -> "Demo App"
    }
}

private fun setupDemoDeepLinks(navigator: Navigator) {
    val handler = navigator.getDeepLinkHandler()

    handler.register("app://demo/home") { _ ->
        MainDestination.Home
    }

    handler.register("app://demo/item/{id}") { params ->
        MasterDetailDestination.Detail(params["id"] ?: "unknown")
    }

    handler.register("app://demo/process/start") { _ ->
        ProcessDestination.Start
    }

    handler.register("app://demo/tabs") { _ ->
        TabsDestination.Main
    }

    handler.register("app://demo/settings") { _ ->
        MainDestination.Settings
    }

    handler.register("app://demo/deeplink") { _ ->
        MainDestination.DeepLinkDemo
    }
}

private fun shouldShowTopAppBar(destination: Destination?): Boolean {
    val route = destination?.route ?: return true
    return route in listOf("home", "explore", "profile", "settings")
}

private fun shouldShowBottomNav(destination: Destination?): Boolean {
    val route = destination?.route ?: return true
    return route in listOf("home", "explore", "profile", "settings")
}
