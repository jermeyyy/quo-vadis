package com.jermey.navplayground.demo.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.navplayground.navigation.NavigationMenuDestination
import com.jermey.navplayground.demo.ui.components.NavigationBottomSheetContent
import com.jermey.feature1.ui.glassmorphism.GlassBottomSheet
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import dev.chrisbanes.haze.HazeState
import org.koin.compose.koinInject

/**
 * Modal screen for the navigation menu bottom sheet.
 *
 * Demonstrates `@Modal` annotation — the library renders the previous screen underneath,
 * and this composable draws the scrim + bottom sheet content on top via [GlassBottomSheet].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(NavigationMenuDestination::class)
@Composable
fun NavigationMenuScreen(
    navigator: Navigator = koinInject()
) {
    val hazeState = remember { HazeState() }

    GlassBottomSheet(
        onDismissRequest = { navigator.navigateBack() },
        hazeState = hazeState
    ) {
        NavigationBottomSheetContent(
            currentRoute = null,
            onNavigate = { destination ->
                navigator.navigateBack()
                navigator.navigate(destination)
            }
        )
    }
}
