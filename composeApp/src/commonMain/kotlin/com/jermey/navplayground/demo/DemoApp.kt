package com.jermey.navplayground.demo

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.navigation.compose.rememberNavigator
import com.jermey.navplayground.navigation.compose.GraphNavHost
import com.jermey.navplayground.navigation.core.*
import com.jermey.navplayground.demo.destinations.*
import com.jermey.navplayground.demo.graphs.*
import com.jermey.navplayground.demo.ui.BottomNavigationBar
import kotlinx.coroutines.launch

/**
 * Main Demo Application showcasing all navigation patterns:
 * - Bottom Navigation with main tabs
 * - Master-Detail navigation
 * - Nested tabs
 * - Process/wizard navigation with branches
 * - Modal drawer navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoApp() {
    val navigator = rememberNavigator()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Register all navigation graphs
    LaunchedEffect(Unit) {
        navigator.registerGraph(mainBottomNavGraph())
        navigator.registerGraph(masterDetailGraph())
        navigator.registerGraph(tabsGraph())
        navigator.registerGraph(processGraph())
        navigator.setStartDestination(MainDestination.Home)
    }

    // Track current route for bottom nav selection
    val currentRoute by navigator.currentDestination.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DemoDrawerContent(
                currentRoute = currentRoute?.route,
                onNavigate = { destination ->
                    navigator.navigate(destination)
                    scope.launch {
                        drawerState.close()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getScreenTitle(currentRoute)) },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                )
            },
            bottomBar = {
                if (shouldShowBottomNav(currentRoute)) {
                    BottomNavigationBar(
                        currentRoute = currentRoute?.route,
                        onNavigate = { navigator.navigate(it) }
                    )
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                GraphNavHost(
                    graph = mainBottomNavGraph(),
                    navigator = navigator,
                    defaultTransition = NavigationTransitions.Fade
                )
            }
        }
    }
}

@Composable
private fun DemoDrawerContent(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    ModalDrawerSheet {
        Spacer(Modifier.height(16.dp))

        Text(
            "Navigation Patterns",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        Divider()

        DrawerItem(
            icon = Icons.Default.Home,
            label = "Home",
            selected = currentRoute == "home",
            onClick = { onNavigate(MainDestination.Home) }
        )

        DrawerItem(
            icon = Icons.Default.List,
            label = "Master-Detail",
            selected = currentRoute?.startsWith("master_detail") == true,
            onClick = { onNavigate(MasterDetailDestination.List) }
        )

        DrawerItem(
            icon = Icons.Default.Dashboard,
            label = "Tabs Example",
            selected = currentRoute?.startsWith("tabs") == true,
            onClick = { onNavigate(TabsDestination.Main) }
        )

        DrawerItem(
            icon = Icons.Default.AssistantDirection,
            label = "Process Flow",
            selected = currentRoute?.startsWith("process") == true,
            onClick = { onNavigate(ProcessDestination.Start) }
        )

        DrawerItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            selected = currentRoute == "settings",
            onClick = { onNavigate(MainDestination.Settings) }
        )
    }
}

@Composable
private fun DrawerItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

private fun getScreenTitle(destination: Destination?): String {
    return when (destination?.route) {
        "home" -> "Home"
        "explore" -> "Explore"
        "profile" -> "Profile"
        "settings" -> "Settings"
        "master_detail_list" -> "Items"
        "tabs_main" -> "Tabs"
        else -> destination?.route?.let { route ->
            when {
                route.startsWith("master_detail_detail") -> "Item Details"
                route.startsWith("process") -> "Setup Process"
                route.startsWith("tabs") -> "Tabs"
                else -> "Demo App"
            }
        } ?: "Demo App"
    }
}

private fun shouldShowBottomNav(destination: Destination?): Boolean {
    val route = destination?.route ?: return true
    return route in listOf("home", "explore", "profile", "settings")
}
