package com.jermey.navplayground.demo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Bottom navigation bar component using destination-based tab selection.
 *
 * This component renders a Material 3 NavigationBar with items based on
 * the provided [tabs] set. Use [getTabDisplayInfo] to map destinations to labels/icons.
 *
 * @param activeTab The currently active tab destination
 * @param tabs Set of tab destinations
 * @param getTabDisplayInfo Callback to get display info (label, icon) for each tab destination
 * @param onTabSelected Callback invoked when a tab is selected, receives the tab destination
 * @param isTransitioning Whether a tab transition is in progress (disables interaction)
 * @param modifier Modifier for the NavigationBar
 */
@Composable
fun BottomNavigationBar(
    activeTab: NavDestination,
    tabs: Set<NavDestination>,
    getTabDisplayInfo: (NavDestination) -> Pair<String, ImageVector>,
    onTabSelected: (NavDestination) -> Unit,
    isTransitioning: Boolean = false,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        tabs.forEach { tab ->
            val (label, icon) = getTabDisplayInfo(tab)
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = { Text(label) },
                selected = activeTab == tab,
                onClick = { onTabSelected(tab) },
                enabled = !isTransitioning
            )
        }
    }
}
