package com.jermey.navplayground.demo.ui.screens.tabs

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.tabs.generated.buildDemoTabsNavNode
import com.jermey.navplayground.demo.tabs.mainTabsWrapper
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import com.jermey.quo.vadis.generated.GeneratedScreenRegistry

/**
 * Tabs Main Screen - Demonstrates nested tabs navigation using the new NavNode architecture.
 *
 * This screen showcases the **new @Tab annotation pattern** and KSP-generated code:
 * - Uses `buildDemoTabsNavNode()` KSP-generated function to create a TabNode
 * - Nested `TreeNavigator` manages the tab navigation state
 * - Uses `QuoVadisHost` to render tab content
 * - Three independent tabs (DemoTab1, DemoTab2, DemoTab3) with their own stacks
 * - Custom `PrimaryScrollableTabRow` for tab switching UI
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

    // Step 3: Observe the active tab index for UI state
    val navState by tabNavigator.state.collectAsState()
    val activeTabIndex = tabNavigator.activeTabIndex ?: 0

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Custom tab bar UI - uses activeTabIndex from TreeNavigator
            PrimaryScrollableTabRow(
                selectedTabIndex = activeTabIndex
            ) {
                Tab(
                    selected = activeTabIndex == 0,
                    onClick = { tabNavigator.switchTab(0) },
                    text = { Text("Tab 1") },
                    icon = { Icon(Icons.Default.Star, null) }
                )
                Tab(
                    selected = activeTabIndex == 1,
                    onClick = { tabNavigator.switchTab(1) },
                    text = { Text("Tab 2") },
                    icon = { Icon(Icons.Default.Favorite, null) }
                )
                Tab(
                    selected = activeTabIndex == 2,
                    onClick = { tabNavigator.switchTab(2) },
                    text = { Text("Tab 3") },
                    icon = { Icon(Icons.Default.Bookmark, null) }
                )
            }

            // Render tab content using QuoVadisHost with the nested navigator
            QuoVadisHost(
                navigator = tabNavigator,
                modifier = Modifier.fillMaxSize(),
                tabWrapper = mainTabsWrapper(),
                enablePredictiveBack = true
            ) { destination ->
                // Use GeneratedScreenRegistry to render the destination content
                // The tab screens (DemoTab1.Tab, etc.) will render TabContent
                GeneratedScreenRegistry.Content(
                    destination = destination,
                    navigator = tabNavigator,
                    sharedTransitionScope = this,
                    animatedVisibilityScope = null
                )
            }
        }
    }
}