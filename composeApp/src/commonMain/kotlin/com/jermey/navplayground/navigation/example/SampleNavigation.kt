package com.jermey.navplayground.navigation.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.navigation.core.*
import com.jermey.navplayground.navigation.compose.*

/**
 * Example destinations for the sample app.
 */
object SampleDestinations {
    object Home : Destination {
        override val route = "home"
    }

    data class Details(val itemId: String) : Destination {
        override val route = "details"
        override val arguments = mapOf("itemId" to itemId)
    }

    object Settings : Destination {
        override val route = "settings"
    }

    data class Profile(val userId: String) : Destination {
        override val route = "profile"
        override val arguments = mapOf("userId" to userId)
    }
}

/**
 * Example navigation graph for a feature module.
 */
class SampleFeatureNavigation : BaseModuleNavigation() {
    override fun buildGraph(): NavigationGraph {
        return navigationGraph("sample_feature") {
            startDestination(SampleDestinations.Home)

            destination(SampleDestinations.Home) { _, navigator ->
                HomeScreen(navigator)
            }

            destination(SimpleDestination("details")) { dest, navigator ->
                DetailsScreen(
                    itemId = dest.arguments["itemId"] as? String ?: "",
                    navigator = navigator
                )
            }

            destination(SampleDestinations.Settings) { _, navigator ->
                SettingsScreen(navigator)
            }

            destination(SimpleDestination("profile")) { dest, navigator ->
                ProfileScreen(
                    userId = dest.arguments["userId"] as? String ?: "",
                    navigator = navigator
                )
            }
        }
    }
}

/**
 * Example home screen.
 */
@Composable
fun HomeScreen(navigator: Navigator) {
    val canGoBack by navigator.backStack.canGoBack.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Home Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = {
            navigator.navigate(
                SampleDestinations.Details("item-123"),
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("Go to Details")
        }

        Button(onClick = {
            navigator.navigate(
                SampleDestinations.Settings,
                NavigationTransitions.Fade
            )
        }) {
            Text("Go to Settings")
        }

        Button(onClick = {
            navigator.navigate(
                SampleDestinations.Profile("user-456"),
                NavigationTransitions.ScaleIn
            )
        }) {
            Text("Go to Profile")
        }

        if (canGoBack) {
            Button(onClick = { navigator.navigateBack() }) {
                Text("Go Back")
            }
        }
    }
}

/**
 * Example details screen.
 */
@Composable
fun DetailsScreen(itemId: String, navigator: Navigator) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Details Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Item ID: $itemId")

        Button(onClick = {
            navigator.navigateAndReplace(SampleDestinations.Settings)
        }) {
            Text("Replace with Settings")
        }

        Button(onClick = { navigator.navigateBack() }) {
            Text("Go Back")
        }

        Button(onClick = {
            navigator.navigateAndClearAll(SampleDestinations.Home)
        }) {
            Text("Go to Home (Clear Stack)")
        }
    }
}

/**
 * Example settings screen.
 */
@Composable
fun SettingsScreen(navigator: Navigator) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Settings Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = { navigator.navigateBack() }) {
            Text("Go Back")
        }

        Button(onClick = { navigator.backStack.popToRoot() }) {
            Text("Pop to Root")
        }
    }
}

/**
 * Example profile screen.
 */
@Composable
fun ProfileScreen(userId: String, navigator: Navigator) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = "Profile Screen",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("User ID: $userId")

        Button(onClick = { navigator.navigateBack() }) {
            Text("Go Back")
        }
    }
}

/**
 * Example app demonstrating the navigation library.
 */
@Composable
fun SampleNavigationApp() {
    MaterialTheme {
        val navigator = rememberNavigator()
        val featureNavigation = remember { SampleFeatureNavigation() }

        LaunchedEffect(Unit) {
            navigator.registerGraph(featureNavigation.provideGraph())
            navigator.setStartDestination(SampleDestinations.Home)
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            GraphNavHost(
                graph = featureNavigation.provideGraph(),
                navigator = navigator,
                defaultTransition = NavigationTransitions.SlideHorizontal
            )
        }
    }
}

