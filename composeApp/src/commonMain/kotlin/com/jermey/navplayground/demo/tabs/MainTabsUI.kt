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
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.navplayground.demo.destinations.MainTabs
import com.jermey.navplayground.demo.ui.components.glassmorphism.LocalHazeState
import com.jermey.quo.vadis.annotations.TabsContainer
import com.jermey.quo.vadis.core.compose.scope.TabMetadata
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
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
            tabMetadata = scope.tabMetadata,
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
 * @param tabMetadata Metadata for all tabs (labels, icons, routes)
 * @param onTabSelected Callback when a tab is selected
 * @param isTransitioning Whether tab switch animation is in progress
 * @param hazeState The HazeState for blur effect
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassBottomNavigationBar(
    modifier: Modifier = Modifier,
    activeTabIndex: Int,
    tabMetadata: List<TabMetadata>,
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
        tabMetadata.forEachIndexed { index, meta ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = meta.icon ?: getTabIconFallback(meta.route),
                        contentDescription = meta.contentDescription ?: meta.label
                    )
                },
                label = { Text(meta.label) },
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
 * Uses [com.jermey.quo.vadis.core.compose.wrapper.TabMetadata] from [com.jermey.quo.vadis.core.compose.wrapper.TabsContainerScope] to render navigation items
 * with proper selection state and click handling.
 *
 * @param activeTabIndex The currently selected tab index (0-based)
 * @param tabMetadata Metadata for all tabs (labels, icons, routes)
 * @param onTabSelected Callback when a tab is selected
 * @param isTransitioning Whether tab switch animation is in progress
 */
@Composable
private fun MainBottomNavigationBar(
    modifier: Modifier = Modifier,
    activeTabIndex: Int,
    tabMetadata: List<TabMetadata>,
    onTabSelected: (Int) -> Unit,
    isTransitioning: Boolean = false
) {
    NavigationBar(modifier = modifier) {
        tabMetadata.forEachIndexed { index, meta ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = meta.icon ?: getTabIconFallback(meta.route),
                        contentDescription = meta.contentDescription ?: meta.label
                    )
                },
                label = { Text(meta.label) },
                selected = activeTabIndex == index,
                onClick = { onTabSelected(index) },
                enabled = !isTransitioning // Disable during animations
            )
        }
    }
}

/**
 * Maps route identifier to a fallback Material icon.
 *
 * This is used when [com.jermey.quo.vadis.core.compose.wrapper.TabMetadata.icon] is null and provides
 * default icons based on common route patterns.
 *
 * @param route The tab route identifier
 * @return An [ImageVector] icon for the route
 */
private fun getTabIconFallback(route: String): ImageVector = when {
    route.contains("home", ignoreCase = true) -> Icons.Default.Home
    route.contains("explore", ignoreCase = true) -> Icons.Default.Explore
    route.contains("person", ignoreCase = true) ||
            route.contains("profile", ignoreCase = true) -> Icons.Default.Person

    route.contains("settings", ignoreCase = true) -> Icons.Default.Settings
    else -> Icons.Default.Circle // Fallback
}
