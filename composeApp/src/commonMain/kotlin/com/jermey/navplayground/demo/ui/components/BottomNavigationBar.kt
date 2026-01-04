package com.jermey.navplayground.demo.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.compose.scope.TabMetadata

/**
 * Bottom navigation bar component using index-based tab selection.
 *
 * This component renders a Material 3 NavigationBar with items based on
 * the provided [tabMetadata]. Selection is determined by comparing
 * the current [activeTabIndex] with each tab's position.
 *
 * @param activeTabIndex The index of the currently selected tab
 * @param tabMetadata List of metadata for each tab (labels, icons, etc.)
 * @param onTabSelected Callback invoked when a tab is selected, receives the tab index
 * @param isTransitioning Whether a tab transition is in progress (disables interaction)
 * @param modifier Modifier for the NavigationBar
 */
@Composable
fun BottomNavigationBar(
    activeTabIndex: Int,
    tabMetadata: List<com.jermey.quo.vadis.core.compose.scope.TabMetadata>,
    onTabSelected: (Int) -> Unit,
    isTransitioning: Boolean = false,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        tabMetadata.forEachIndexed { index, meta ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = meta.icon ?: Icons.Default.Circle,
                        contentDescription = meta.contentDescription ?: meta.label
                    )
                },
                label = { Text(meta.label) },
                selected = activeTabIndex == index,
                onClick = { onTabSelected(index) },
                enabled = !isTransitioning
            )
        }
    }
}
