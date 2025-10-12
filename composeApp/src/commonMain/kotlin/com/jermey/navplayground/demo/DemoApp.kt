package com.jermey.navplayground.demo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssistantDirection
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.graphs.mainBottomNavGraph
import com.jermey.navplayground.demo.graphs.masterDetailGraph
import com.jermey.navplayground.demo.graphs.processGraph
import com.jermey.navplayground.demo.graphs.tabsGraph
import com.jermey.navplayground.demo.ui.BottomNavigationBar
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.compose.NavHost
import kotlinx.coroutines.launch

/**
 * Main Demo Application showcasing all navigation patterns:
 * - Bottom Navigation with main tabs
 * - Master-Detail navigation
 * - Nested tabs
 * - Process/wizard navigation with branches
 * - Modal bottom sheet navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoApp() {
    val navigator = rememberNavigator()
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Register all navigation graphs
    LaunchedEffect(Unit) {
        navigator.registerGraph(mainBottomNavGraph())
        navigator.registerGraph(masterDetailGraph())
        navigator.registerGraph(tabsGraph())
        navigator.registerGraph(processGraph())
        navigator.setStartDestination(MainDestination.Home)

        // Setup deep link handlers
        setupDemoDeepLinks(navigator)
    }

    // Track current route for bottom nav selection
    val currentRoute by navigator.currentDestination.collectAsState()

    Scaffold(
        topBar = {
            // Only show TopAppBar on main screens (not on nested navigation screens)
            if (shouldShowTopAppBar(currentRoute)) {
                TopAppBar(
                    title = { Text(getScreenTitle(currentRoute)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { showBottomSheet = true }
                        ) {
                            Icon(Icons.Default.Menu, "Menu")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (shouldShowBottomNav(currentRoute)) {
                BottomNavigationBar(
                    currentRoute = currentRoute?.route,
                    onNavigate = { navigator.navigateAndReplace(it) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Use PlatformAwareNavHost for Android predictive back support
            NavHost(
                graph = mainBottomNavGraph(),
                navigator = navigator,
                modifier = Modifier,
                defaultTransition = NavigationTransitions.Fade,
                enablePredictiveBack = true
            )
        }
    }

    // Modal Bottom Sheet for navigation
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            NavigationBottomSheetContent(
                currentRoute = currentRoute?.route,
                onNavigate = { destination: Destination ->
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

@Composable
private fun NavigationBottomSheetContent(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Text(
            "Navigation Patterns",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider()

        // Navigation items
        BottomSheetNavigationItem(
            icon = Icons.Default.Home,
            label = "Home",
            description = "Main dashboard",
            selected = currentRoute == "home",
            onClick = { onNavigate(MainDestination.Home) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.List,
            label = "Master-Detail",
            description = "List with detail view pattern",
            selected = currentRoute?.startsWith("master_detail") == true,
            onClick = { onNavigate(MasterDetailDestination.List) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.Dashboard,
            label = "Tabs Example",
            description = "Nested tabs navigation",
            selected = currentRoute?.startsWith("tabs") == true,
            onClick = { onNavigate(TabsDestination.Main) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.AssistantDirection,
            label = "Process Flow",
            description = "Multi-step wizard",
            selected = currentRoute?.startsWith("process") == true,
            onClick = { onNavigate(ProcessDestination.Start) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.Link,
            label = "Deep Link Demo",
            description = "Deep linking examples",
            selected = currentRoute == "deeplink_demo",
            onClick = { onNavigate(MainDestination.DeepLinkDemo) }
        )

        BottomSheetNavigationItem(
            icon = Icons.Default.Settings,
            label = "Settings",
            description = "App settings",
            selected = currentRoute == "settings",
            onClick = { onNavigate(MainDestination.Settings) }
        )
    }
}

@Composable
private fun BottomSheetNavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
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

/**
 * Setup deep link handlers for the demo app
 */
private fun setupDemoDeepLinks(navigator: Navigator) {
    // Get the actual handler from the navigator
    val handler = navigator.getDeepLinkHandler()

    // Register deep link patterns for demo navigation

    // Pattern: app://demo/home
    handler.register("app://demo/home") { _ ->
        MainDestination.Home
    }

    // Pattern: app://demo/item/{id}
    handler.register("app://demo/item/{id}") { params ->
        MasterDetailDestination.Detail(params["id"] ?: "unknown")
    }

    // Pattern: app://demo/process/start
    handler.register("app://demo/process/start") { _ ->
        ProcessDestination.Start
    }

    // Pattern: app://demo/tabs
    handler.register("app://demo/tabs") { _ ->
        TabsDestination.Main
    }

    // Pattern: app://demo/settings
    handler.register("app://demo/settings") { _ ->
        MainDestination.Settings
    }

    // Pattern: app://demo/deeplink
    handler.register("app://demo/deeplink") { _ ->
        MainDestination.DeepLinkDemo
    }
}

private fun shouldShowTopAppBar(destination: Destination?): Boolean {
    val route = destination?.route ?: return true
    // Only show on main bottom nav screens, not on nested navigation
    return route in listOf("home", "explore", "profile", "settings")
}

private fun shouldShowBottomNav(destination: Destination?): Boolean {
    val route = destination?.route ?: return true
    return route in listOf("home", "explore", "profile", "settings")
}
