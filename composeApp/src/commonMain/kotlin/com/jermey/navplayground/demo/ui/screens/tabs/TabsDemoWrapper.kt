package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.navplayground.demo.destinations.DemoTabs
import com.jermey.quo.vadis.annotations.TabWrapper
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope

/**
 * Tab wrapper for the Demo Tabs with a tab strip at the top.
 *
 * This demonstrates the @TabWrapper pattern with TabRow for switching tabs.
 * Each tab maintains its own navigation stack, allowing nested navigation
 * within each tab.
 *
 * @param scope The TabWrapperScope providing access to tab state and navigation
 * @param content The content slot where active tab content is rendered
 */
@OptIn(ExperimentalMaterial3Api::class)
@TabWrapper(DemoTabs.Companion::class)
@Composable
fun DemoTabsWrapper(
    scope: TabWrapperScope,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top app bar with back navigation
        TopAppBar(
            title = { Text("Tabs Demo") },
            navigationIcon = {
                IconButton(onClick = { scope.navigator.navigateBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Tab strip
        TabRow(
            selectedTabIndex = scope.activeTabIndex,
            modifier = Modifier.fillMaxWidth()
        ) {
            scope.tabMetadata.forEachIndexed { index, meta ->
                Tab(
                    selected = scope.activeTabIndex == index,
                    onClick = { scope.switchTab(index) },
                    enabled = !scope.isTransitioning,
                    text = { Text(meta.label) },
                    icon = {
                        Icon(
                            imageVector = getTabIcon(meta.route),
                            contentDescription = meta.contentDescription ?: meta.label
                        )
                    }
                )
            }
        }

        // Tab content
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Maps route to a Material icon for the demo tabs.
 *
 * @param route The tab route identifier
 * @return An ImageVector icon for the route
 */
private fun getTabIcon(route: String): ImageVector = when {
    route.contains("music", ignoreCase = true) -> Icons.Default.MusicNote
    route.contains("movies", ignoreCase = true) -> Icons.Default.Movie
    route.contains("books", ignoreCase = true) -> Icons.AutoMirrored.Default.MenuBook
    else -> Icons.Default.MusicNote // Fallback
}
