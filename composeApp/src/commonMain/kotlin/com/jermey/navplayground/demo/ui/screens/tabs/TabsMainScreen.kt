package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.tabs.DemoTabs
import com.jermey.navplayground.demo.tabs.generated.buildDemoTabsNavNode
import com.jermey.navplayground.demo.tabs.generated.getMainTabsMetadata
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.TabWrapper
import com.jermey.quo.vadis.core.navigation.compose.NavigationHost
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator

/**
 * Tab wrapper for DemoTabs with scrollable tab row at the top.
 *
 * Uses @TabWrapper annotation pattern to provide custom tab UI chrome
 * while the library handles tab content rendering and state management.
 *
 * @param scope The TabWrapperScope providing access to tab state and navigation
 * @param content The content slot where active tab content is rendered
 */
@TabWrapper(DemoTabs::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoTabsWrapper(
    scope: TabWrapperScope,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryScrollableTabRow(
            selectedTabIndex = scope.activeTabIndex
        ) {
            getMainTabsMetadata().forEachIndexed { index, meta ->
                Tab(
                    selected = scope.activeTabIndex == index,
                    onClick = { scope.switchTab(index) },
                    text = { Text(meta.label) },
                    icon = {
                        Icon(
                            imageVector = getDemoTabIconFallback(meta.route),
                            contentDescription = null
                        )
                    }
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * Maps route identifier to a fallback Material icon for demo tabs.
 *
 * @param route The tab route identifier
 * @return An [ImageVector] icon for the route
 */
private fun getDemoTabIconFallback(route: String): ImageVector = when {
    route.contains("tab1", ignoreCase = true) -> Icons.Default.Star
    route.contains("tab2", ignoreCase = true) -> Icons.Default.Favorite
    route.contains("tab3", ignoreCase = true) -> Icons.Default.Bookmark
    else -> Icons.Default.Star // Fallback
}

/**
 * Tabs Main Screen - Demonstrates nested tabs navigation using the new NavNode architecture.
 *
 * This screen showcases the **new @Tab annotation pattern** and KSP-generated code:
 * - Uses `buildDemoTabsNavNode()` KSP-generated function to create a TabNode
 * - Nested `TreeNavigator` manages the tab navigation state
 * - Uses `HierarchicalQuoVadisHost` to render tab content
 * - Three independent tabs (DemoTab1, DemoTab2, DemoTab3) with their own stacks
 * - Custom `@TabWrapper(DemoTabs::class)` provides the tab UI chrome
 *
 * ## Architecture
 *
 * ```
 * TabsMainScreen
 * └── DemoTabs (TabNode)
 *     ├── DemoTab1 (StackNode) → Content
 *     ├── DemoTab2 (StackNode) → Content
 *     └── DemoTab3 (StackNode) → Content
 * ```
 *
 * Each tab contains a list of items that can be clicked to navigate to detail screens,
 * demonstrating how tabs maintain independent navigation stacks.
 *
 * @param navigator The parent navigator for back navigation to the calling screen
 */
@Screen(TabsDestination.Main::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsMainScreen(
    navigator: Navigator
) {
    // Step 1: Build the nested tab tree from KSP-generated function
    val tabTree = remember { buildDemoTabsNavNode() }

    // Step 2: Create a nested TreeNavigator for the tabs
    val tabNavigator = remember { TreeNavigator(initialState = tabTree) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tabs Navigation Demo") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        // HierarchicalQuoVadisHost uses @TabWrapper(DemoTabs::class) from registry
        NavigationHost(
            navigator = tabNavigator,
            modifier = Modifier.fillMaxSize().padding(padding),
            enablePredictiveBack = true
        )
    }
}