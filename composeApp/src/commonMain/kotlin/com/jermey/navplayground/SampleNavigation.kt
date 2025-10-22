package com.jermey.navplayground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.DetailData
import com.jermey.quo.vadis.core.navigation.compose.GraphNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.BaseModuleNavigation
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import com.jermey.quo.vadis.core.navigation.core.navigationGraph
import com.jermey.quo.vadis.core.navigation.core.typedDestination
import kotlinx.serialization.Serializable

/**
 * Example destinations for the sample app.
 */
sealed class SampleDestinations : Destination {
    object Home : SampleDestinations() {
        override val route = "home"
    }

    /**
     * Serializable data for Details destination.
     */


    object Settings : SampleDestinations() {
        override val route = "settings"
    }

    data class Details(val itemId: String) : SampleDestinations(),
        TypedDestination<Details.DetailsData> {
        override val route = "details"
        override val data = DetailsData(itemId)

        @Serializable
        data class DetailsData(val itemId: String)
    }

    data class Profile(val userId: String) : SampleDestinations(),
        TypedDestination<Profile.ProfileData> {
        override val route = "profile"
        override val data = ProfileData(userId)

        @Serializable
        data class ProfileData(val userId: String)
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

            typedDestination("details") { data: DetailData, navigator ->
                DetailsScreen(
                    itemId = data.itemId,
                    navigator = navigator
                )
            }

            destination(SampleDestinations.Settings) { _, navigator ->
                SettingsScreen(navigator)
            }

            typedDestination("profile") { data: SampleDestinations.Profile.ProfileData, navigator ->
                ProfileScreen(
                    userId = data.userId,
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
                SampleDestinations.Details(itemId = "item-123"),
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
                SampleDestinations.Profile(userId = "user-456"),
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

