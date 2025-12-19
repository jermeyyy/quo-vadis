package com.jermey.navplayground.demo.ui.screens.masterdetail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.ui.components.DetailRow
import com.jermey.navplayground.demo.ui.components.SpecificationRow
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.compose.animation.quoVadisSharedElement
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.sharedBounds
import com.jermey.quo.vadis.core.navigation.core.sharedElement
import org.koin.compose.koinInject

private const val RELATED_ITEMS_COUNT = 5

/**
 * Detail Screen - Shows details of selected item (Detail view)
 */
@Screen(MasterDetailDestination.Detail::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DetailScreen(
    destination: MasterDetailDestination.Detail,
    navigator: Navigator = koinInject()
) {
    val itemId = destination.itemId
    val relatedItems = remember(itemId) {
        (1..RELATED_ITEMS_COUNT).map { "Related item $it" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
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
                DetailHeaderCard(itemId)
            }

            item {
                SpecificationsCard()
            }

            item {
                RelatedItemsHeader()
            }

            items(relatedItems.size) { index ->
                val relatedId = "related_${itemId}_$index"
                RelatedItemCard(
                    relatedItemName = relatedItems[index],
                    onNavigateToRelated = { navigator.navigate(MasterDetailDestination.Detail(itemId = relatedId)) }
                )
            }

            item {
                ActionButtons(onBack = { navigator.navigateBack() })
            }
        }
    }
}

/**
 * Header card showing main item information with shared element transitions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun DetailHeaderCard(itemId: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with large icon and title (shared element transitions)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Apply shared element transition to icon (matching key from ItemCard) - larger in detail
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Item icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(80.dp)
                        .quoVadisSharedElement(sharedElement(key = "icon-$itemId"))
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Apply shared element transition to title (matching key from ItemCard)
                    Text(
                        itemId.replace("_", " ").capitalize(),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.quoVadisSharedElement(sharedBounds(key = "title-$itemId"))
                    )

                    Text(
                        "Detailed Information",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "This is a detailed view of $itemId. In a real application, " +
                        "this would show comprehensive information about the selected item.",
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            DetailRow("ID", itemId)
            DetailRow("Category", "Sample Category")
            DetailRow("Status", "Available")
            DetailRow("Price", "$99.99")
        }
    }
}

/**
 * Card displaying item specifications.
 */
@Composable
private fun SpecificationsCard() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Specifications",
            style = MaterialTheme.typography.titleLarge
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                SpecificationRow("Weight", "500g")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SpecificationRow("Dimensions", "10 x 5 x 2 cm")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SpecificationRow("Material", "Premium")
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SpecificationRow("Color", "Blue")
            }
        }
    }
}

/**
 * Header for related items section.
 */
@Composable
private fun RelatedItemsHeader() {
    Text(
        "Related Items",
        style = MaterialTheme.typography.titleLarge
    )
}

/**
 * Card for a single related item.
 */
@Composable
private fun RelatedItemCard(
    relatedItemName: String,
    onNavigateToRelated: () -> Unit
) {
    Card(
        onClick = onNavigateToRelated,
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(relatedItemName) },
            supportingContent = { Text("Tap to view details") },
            trailingContent = { Icon(Icons.Default.KeyboardArrowRight, "View") }
        )
    }
}

/**
 * Action buttons at the bottom of the detail screen.
 */
@Composable
private fun ActionButtons(onBack: () -> Unit) {
    Column {
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

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
