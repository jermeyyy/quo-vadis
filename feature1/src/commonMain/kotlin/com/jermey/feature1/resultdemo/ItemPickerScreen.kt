package com.jermey.feature1.resultdemo

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jermey.feature1.resultdemo.container.ItemPickerContainer
import com.jermey.feature1.resultdemo.container.ItemPickerContainer.Action
import com.jermey.feature1.resultdemo.container.ItemPickerContainer.Intent
import com.jermey.feature1.resultdemo.container.ItemPickerState
import com.jermey.feature1.resultdemo.container.PickerItem
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.flowmvi.rememberContainer
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Item Picker Screen - Allows selecting an item to return as result.
 *
 * Demonstrates:
 * - Returning a result using [navigateBackWithResult]
 * - Container integration with [com.jermey.navplayground.demo.container.ItemPickerContainer]
 * - List-based selection UI
 *
 * ## Features
 *
 * - Displays a list of items to choose from
 * - Clicking an item returns it as the result
 * - Cancel button navigates back without result (caller receives null)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(ResultDemoDestination.ItemPicker::class)
@Composable
fun ItemPickerScreen(
    container: Store<ItemPickerState, Intent, Action> = rememberContainer<ItemPickerContainer, ItemPickerState, Intent, Action>()
) {

    val state by container.subscribe()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pick an Item") },
                navigationIcon = {
                    IconButton(onClick = { container.intent(Intent.Cancel) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { container.intent(Intent.Cancel) }) {
                        Text("Cancel")
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
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select an item to return",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Or press back/cancel to return null",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            // Items List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    PickerItemCard(
                        item = item,
                        onClick = { container.intent(Intent.SelectItem(item)) }
                    )
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * Card displaying a single picker item.
 *
 * @param item The item to display
 * @param onClick Called when the item is clicked
 */
@Composable
private fun PickerItemCard(
    item: PickerItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji Icon
            Text(
                text = item.icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.size(48.dp),
            )

            Spacer(Modifier.width(16.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "ID: ${item.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            // Arrow
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
