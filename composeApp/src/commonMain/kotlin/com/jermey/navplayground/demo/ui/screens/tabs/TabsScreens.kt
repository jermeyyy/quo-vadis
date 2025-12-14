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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.tabs.DemoTab1
import com.jermey.navplayground.demo.tabs.DemoTab2
import com.jermey.navplayground.demo.tabs.DemoTab3
import com.jermey.navplayground.demo.ui.components.DetailInfoRow
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator

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

// ============================================================================
// DemoTab Screen Implementations - Required for KSP-generated screen registry
// ============================================================================

/**
 * Screen for DemoTab1.Tab - Star themed items.
 */
@Screen(DemoTab1.Tab::class)
@Composable
fun DemoTab1Screen(navigator: Navigator) {
    val items = remember {
        listOf(
            "Star Item 1",
            "Star Item 2",
            "Star Item 3",
            "Star Item 4",
            "Star Item 5"
        )
    }
    TabContent(
        tabId = "tab1",
        title = "Tab 1 - Stars",
        items = items,
        onItemClick = { itemId ->
            navigator.navigate(TabsDestination.SubItem(tabId = "tab1", itemId = itemId))
        },
        icon = Icons.Default.Star
    )
}

/**
 * Screen for DemoTab2.Tab - Heart themed items.
 */
@Screen(DemoTab2.Tab::class)
@Composable
fun DemoTab2Screen(navigator: Navigator) {
    val items = remember { listOf("Heart Item 1", "Heart Item 2", "Heart Item 3", "Heart Item 4") }
    TabContent(
        tabId = "tab2",
        title = "Tab 2 - Hearts",
        items = items,
        onItemClick = { itemId ->
            navigator.navigate(TabsDestination.SubItem(tabId = "tab2", itemId = itemId))
        },
        icon = Icons.Default.Favorite
    )
}

/**
 * Screen for DemoTab3.Tab - Bookmark themed items.
 */
@Screen(DemoTab3.Tab::class)
@Composable
fun DemoTab3Screen(navigator: Navigator) {
    val items = remember { listOf("Bookmark Item 1", "Bookmark Item 2", "Bookmark Item 3") }
    TabContent(
        tabId = "tab3",
        title = "Tab 3 - Bookmarks",
        items = items,
        onItemClick = { itemId ->
            navigator.navigate(TabsDestination.SubItem(tabId = "tab3", itemId = itemId))
        },
        icon = Icons.Default.Bookmark
    )
}

