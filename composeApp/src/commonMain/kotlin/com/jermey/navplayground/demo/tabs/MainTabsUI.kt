package com.jermey.navplayground.demo.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.quo.vadis.annotations.TabWrapper
import com.jermey.quo.vadis.core.navigation.compose.HierarchicalQuoVadisHost
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabMetadata
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Tab wrapper for the main tabs with bottom navigation.
 *
 * The @TabWrapper annotation pattern gives full control over the scaffold structure
 * while the library handles tab content rendering and state management.
 *
 * @param scope The TabWrapperScope providing access to tab state and navigation
 * @param content The content slot where active tab content is rendered
 */
@TabWrapper(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabWrapperScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.consumeWindowInsets(
            WindowInsets()
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
        ),
        bottomBar = {
            MainBottomNavigationBar(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                activeTabIndex = scope.activeTabIndex,
                tabMetadata = scope.tabMetadata,
                onTabSelected = { index -> scope.switchTab(index) },
                isTransitioning = scope.isTransitioning
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Library renders active tab content
            content()
        }
    }
}

/**
 * Main app tabs screen using HierarchicalQuoVadisHost.
 *
 * This composable renders the main tab navigation with:
 * - Independent navigation stacks per tab
 * - State preservation across tab switches
 * - Custom bottom navigation bar via @TabWrapper annotation
 * - Automatic back press handling
 *
 * @param navigator The navigator instance for this tab container
 * @param modifier Modifier to be applied to the root container
 */
@Composable
fun MainTabsScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    HierarchicalQuoVadisHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = true
    )
}

/**
 * Bottom navigation bar for the main tabs.
 *
 * Uses [TabMetadata] from [TabWrapperScope] to render navigation items
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
 * This is used when [TabMetadata.icon] is null and provides
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
