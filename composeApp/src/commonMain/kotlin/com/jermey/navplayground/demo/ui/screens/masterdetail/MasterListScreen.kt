package com.jermey.navplayground.demo.ui.screens.masterdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.ItemCard
import com.jermey.quo.vadis.core.navigation.compose.TransitionScope

private const val MASTER_LIST_ITEMS_COUNT = 50

/**
 * Master List Screen - Shows list of items (Master view)
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun MasterListScreen(
    onItemClick: (String) -> Unit,
    onBack: () -> Unit,
    transitionScope: TransitionScope? = null
) {
    val items = remember {
        (1..MASTER_LIST_ITEMS_COUNT).map {
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
