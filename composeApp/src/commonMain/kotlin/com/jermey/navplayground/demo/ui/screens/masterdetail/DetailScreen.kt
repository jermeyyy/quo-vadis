package com.jermey.navplayground.demo.ui.screens.masterdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.DetailRow
import com.jermey.navplayground.demo.ui.components.SpecificationRow

private const val RELATED_ITEMS_COUNT = 5

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
        (1..RELATED_ITEMS_COUNT).map { "Related item $it" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                            text = "This is a detailed view of $itemId. In a real application, " +
                                    "this would show comprehensive information about the selected item.",
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

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
