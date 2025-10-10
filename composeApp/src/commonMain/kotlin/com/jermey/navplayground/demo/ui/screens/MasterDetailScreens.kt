package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Master List Screen - Shows list of items (Master view)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterListScreen(
    onItemClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val items = remember {
        (1..50).map {
            Item(
                id = "item_$it",
                title = "Item $it",
                subtitle = "Description for item $it",
                category = listOf("Electronics", "Books", "Clothing", "Food")[it % 4]
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master-Detail Pattern") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Select an item to view details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(items) { item ->
                ItemCard(
                    item = item,
                    onClick = { onItemClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: Item,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(item.category, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(24.dp)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Detail Screen - Shows details of selected item (Detail view)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onNavigateToRelated: (String) -> Unit
) {
    val relatedItems = remember(itemId) {
        (1..5).map { "Related item $it" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Favorite, "Favorite")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Main content
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            itemId.replace("_", " ").capitalize(),
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            "This is a detailed view of $itemId. In a real application, this would show comprehensive information about the selected item.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Divider()

                        DetailRow("ID", itemId)
                        DetailRow("Category", "Sample Category")
                        DetailRow("Status", "Available")
                        DetailRow("Price", "$99.99")
                    }
                }
            }

            item {
                Text(
                    "Specifications",
                    style = MaterialTheme.typography.titleLarge
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SpecificationRow("Weight", "500g")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SpecificationRow("Dimensions", "10 x 5 x 2 cm")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SpecificationRow("Material", "Premium")
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        SpecificationRow("Color", "Blue")
                    }
                }
            }

            item {
                Text(
                    "Related Items",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(relatedItems.size) { index ->
                val relatedId = "related_${itemId}_$index"
                Card(
                    onClick = { onNavigateToRelated(relatedId) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(relatedItems[index]) },
                        supportingContent = { Text("Tap to view details") },
                        trailingContent = { Icon(Icons.Default.ChevronRight, "View") }
                    )
                }
            }

            item {
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back to List")
                    }

                    Button(
                        onClick = {},
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add to Cart")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SpecificationRow(spec: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(spec, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private data class Item(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String
)

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

