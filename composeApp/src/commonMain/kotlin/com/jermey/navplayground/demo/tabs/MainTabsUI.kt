package com.jermey.navplayground.demo.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MainTabs
import com.jermey.navplayground.demo.ui.components.glassmorphism.LocalHazeState
import com.jermey.quo.vadis.annotations.TabsContainer
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Tabs container wrapper for the main tabs with bottom navigation.
 *
 * The @TabsContainer annotation pattern gives full control over the scaffold structure
 * while the library handles tab content rendering and state management.
 *
 * Features:
 * - Edge-to-edge display with content extending under navigation bar
 * - Glassmorphic bottom navigation bar with blur effect
 * - Haze state provided via LocalHazeState for child screens
 *
 * @param scope The TabsContainerScope providing access to tab state and navigation
 * @param content The content slot where active tab content is rendered
 */
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsContainer(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    val hazeState = remember { HazeState() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(
                WindowInsets(0, 0, 0, 0)
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            )
    ) {
        // Content fills entire screen (edge-to-edge, under navigation bar)
        // Marked as haze source for bottom bar blur effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            // Provide haze state to child screens via CompositionLocal
            androidx.compose.runtime.CompositionLocalProvider(
                LocalHazeState provides hazeState
            ) {
                content()
            }
        }
        
        // Glassmorphic bottom navigation bar - overlaid on top
        GlassBottomNavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            activeTabIndex = scope.activeTabIndex,
            tabs = scope.tabs,
            onTabSelected = { index -> scope.switchTab(index) },
            isTransitioning = scope.isTransitioning,
            hazeState = hazeState
        )
    }
}

/**
 * Glassmorphic bottom navigation bar with blur effect.
 *
 * Uses [HazeState] to create a frosted glass effect with content visible through the blur.
 * Supports edge-to-edge display with proper navigation bar insets.
 *
 * @param activeTabIndex The currently selected tab index (0-based)
 * @param tabs List of tab destinations for pattern matching
 * @param onTabSelected Callback when a tab is selected
 * @param isTransitioning Whether tab switch animation is in progress
 * @param hazeState The HazeState for blur effect
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassBottomNavigationBar(
    modifier: Modifier = Modifier,
    activeTabIndex: Int,
    tabs: List<NavDestination>,
    onTabSelected: (Int) -> Unit,
    isTransitioning: Boolean = false,
    hazeState: HazeState
) {
    val hazeStyle = HazeMaterials.ultraThin()
    
    NavigationBar(
        modifier = modifier
            .hazeEffect(state = hazeState) {
                style = hazeStyle
            }
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
            .windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        tabs.forEachIndexed { index, tab ->
            val (label, icon) = when (tab) {
                is MainTabs.HomeTab -> "Home" to Icons.Default.Home
                is MainTabs.ExploreTab -> "Explore" to Icons.Default.Explore
                is MainTabs.ProfileTab -> "Profile" to Icons.Default.Person
                is MainTabs.SettingsTab -> "Settings" to Icons.Default.Settings
                else -> "Tab" to Icons.Default.Circle
            }
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = activeTabIndex == index,
                onClick = { onTabSelected(index) },
                enabled = !isTransitioning // Disable during animations
            )
        }
    }
}

/**
 * Standard bottom navigation bar without blur effect.
 *
 * Uses pattern matching on tab destinations to determine labels and icons.
 *
 * @param activeTabIndex The currently selected tab index (0-based)
 * @param tabs List of tab destinations for pattern matching
 * @param onTabSelected Callback when a tab is selected
 * @param isTransitioning Whether tab switch animation is in progress
 */
@Composable
private fun MainBottomNavigationBar(
    modifier: Modifier = Modifier,
    activeTabIndex: Int,
    tabs: List<NavDestination>,
    onTabSelected: (Int) -> Unit,
    isTransitioning: Boolean = false
) {
    NavigationBar(modifier = modifier) {
        tabs.forEachIndexed { index, tab ->
            val (label, icon) = when (tab) {
                is MainTabs.HomeTab -> "Home" to Icons.Default.Home
                is MainTabs.ExploreTab -> "Explore" to Icons.Default.Explore
                is MainTabs.ProfileTab -> "Profile" to Icons.Default.Person
                is MainTabs.SettingsTab -> "Settings" to Icons.Default.Settings
                else -> "Tab" to Icons.Default.Circle
            }
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = activeTabIndex == index,
                onClick = { onTabSelected(index) },
                enabled = !isTransitioning // Disable during animations
            )
        }
    }
}
