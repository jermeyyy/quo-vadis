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
 * Bottom navigation bar component using index-based tab selection.
 *
 * This component renders a Material 3 NavigationBar with items based on
 * the provided [tabs] list. Use [getTabDisplayInfo] to map destinations to labels/icons.
 *
 * @param activeTabIndex The index of the currently selected tab
 * @param tabs List of tab destinations
 * @param getTabDisplayInfo Callback to get display info (label, icon) for each tab destination
 * @param onTabSelected Callback invoked when a tab is selected, receives the tab index
 * @param isTransitioning Whether a tab transition is in progress (disables interaction)
 * @param modifier Modifier for the NavigationBar
 */
@Composable
fun BottomNavigationBar(
    activeTabIndex: Int,
    tabs: List<NavDestination>,
    getTabDisplayInfo: (NavDestination) -> Pair<String, ImageVector>,
    onTabSelected: (Int) -> Unit,
    isTransitioning: Boolean = false,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        tabs.forEachIndexed { index, tab ->
            val (label, icon) = getTabDisplayInfo(tab)
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
                enabled = !isTransitioning
            )
        }
    }
}
