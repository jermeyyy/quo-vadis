package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.destinations.buildTabsDestinationGraph
import com.jermey.navplayground.demo.tabs.DemoTab1
import com.jermey.navplayground.demo.tabs.DemoTab2
import com.jermey.navplayground.demo.tabs.DemoTab3
import com.jermey.navplayground.demo.tabs.DemoTabs
import com.jermey.navplayground.demo.tabs.generated.DemoTabsConfig
import com.jermey.navplayground.demo.ui.components.DetailInfoRow
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.compose.rememberTabNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Tabs Main Screen - Demonstrates nested tabs navigation using the new tabbed navigation API.
 *
 * This screen showcases the @TabGraph annotation and generated code in action:
 * - Uses DemoTabs sealed class with @TabGraph annotation
 * - Generated DemoTabsConfig and DemoTabsContainer
 * - Three independent tabs with their own content
 * - Custom ScrollableTabRow for tab switching UI
 *
 * Each tab contains a list of items that can be clicked to navigate to detail screens,
 * demonstrating how tabs maintain independent navigation stacks.
 */
@Screen(TabsDestination.Main::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsMainScreen(
    navigator: Navigator
) {
    // Use the generated config and tab navigator
    val tabState = rememberTabNavigator(DemoTabsConfig, navigator)
    val selectedTab by tabState.selectedTab.collectAsState()

    // Build navigation graph for tabs
    val tabsGraph = remember<NavigationGraph> {
        buildTabsDestinationGraph()
    }

    // Register graph with navigator
    LaunchedEffect(navigator, tabsGraph) {
        navigator.registerGraph(tabsGraph)
    }

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
            // Custom tab bar UI
            PrimaryScrollableTabRow(
                selectedTabIndex = when (selectedTab) {
                    is DemoTab1 -> 0
                    is DemoTab2 -> 1
                    is DemoTab3 -> 2
                    else -> 0
                }
            ) {
                Tab(
                    selected = selectedTab is DemoTab1,
                    onClick = { tabState.selectTab(DemoTab1.Tab) },
                    text = { Text("Tab 1") },
                    icon = { Icon(Icons.Default.Star, null) }
                )
                Tab(
                    selected = selectedTab is DemoTab2,
                    onClick = { tabState.selectTab(DemoTab2.Tab) },
                    text = { Text("Tab 2") },
                    icon = { Icon(Icons.Default.Favorite, null) }
                )
                Tab(
                    selected = selectedTab is DemoTab3,
                    onClick = { tabState.selectTab(DemoTab3.Tab) },
                    text = { Text("Tab 3") },
                    icon = { Icon(Icons.Default.Bookmark, null) }
                )
            }

            // Tab content based on selected tab
            when (selectedTab) {
                is DemoTab1 -> TabContent(
                    tabId = "tab1",
                    title = "First Tab",
                    items = (1..10).map { "Item $it in Tab 1" },
                    onItemClick = { itemId ->
                        navigator.navigate(
                            TabsDestination.SubItem("tab1", itemId),
                            NavigationTransitions.SlideHorizontal
                        )
                    },
                    icon = Icons.Default.Star
                )
                is DemoTab2 -> TabContent(
                    tabId = "tab2",
                    title = "Second Tab",
                    items = (1..15).map { "Item $it in Tab 2" },
                    onItemClick = { itemId ->
                        navigator.navigate(
                            TabsDestination.SubItem("tab2", itemId),
                            NavigationTransitions.SlideHorizontal
                        )
                    },
                    icon = Icons.Default.Favorite
                )
                is DemoTab3 -> TabContent(
                    tabId = "tab3",
                    title = "Third Tab",
                    items = (1..8).map { "Item $it in Tab 3" },
                    onItemClick = { itemId ->
                        navigator.navigate(
                            TabsDestination.SubItem("tab3", itemId),
                            NavigationTransitions.SlideHorizontal
                        )
                    },
                    icon = Icons.Default.Bookmark
                )
                else -> {}
            }
        }
    }
}

@Composable
private fun TabContent(
    tabId: String,
    title: String,
    items: List<String>,
    onItemClick: (String) -> Unit,
    icon: ImageVector
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "This tab contains ${items.size} items. Click on any item to see its details.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        items(items) { item ->
            Card(
                onClick = { onItemClick(item) },
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text(item) },
                    supportingContent = { Text("Click to view details") },
                    leadingContent = {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingContent = {
                        Icon(Icons.Default.ChevronRight, "View")
                    }
                )
            }
        }
    }
}

/**
 * Tab Sub-Item Screen - Shows details of an item from a specific tab
 */
@Screen(TabsDestination.SubItem::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSubItemScreen(
    destination: TabsDestination.SubItem,
    navigator: Navigator
) {
    val tabId = destination.tabId
    val itemId = destination.itemId
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        when (tabId) {
                            "tab1" -> Icons.Default.Star
                            "tab2" -> Icons.Default.Favorite
                            else -> Icons.Default.Bookmark
                        },
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        itemId,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        "From ${tabId.uppercase()}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Details",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    DetailInfoRow("Tab", tabId.uppercase())
                    DetailInfoRow("Item", itemId)
                    DetailInfoRow("Type", "Tab Item")
                    DetailInfoRow("Status", "Active")
                }
            }

            Text(
                "Description",
                style = MaterialTheme.typography.titleMedium
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "This is a detailed view of an item from a nested tab. " +
                    "In a real application, this would display specific content " +
                    "related to the selected item from $tabId.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Tabs")
            }
        }
    }
}

