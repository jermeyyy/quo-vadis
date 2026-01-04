package com.jermey.navplayground.demo.ui.screens.statedriven

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.StateDrivenDemoDestination.DemoTab
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Panel for editing and viewing the backstack state.
 *
 * Provides controls for:
 * - Viewing the current stack as a list
 * - Adding new destinations (with parameter input)
 * - Removing individual entries
 * - Pop, Clear, and Reset operations
 * - Reordering entries (swap, move)
 *
 * @param state The current state of the backstack.
 * @param onIntent Callback to dispatch intents for state changes.
 * @param modifier Modifier for the panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BackstackEditorPanel(
    state: StateDrivenState,
    onIntent: (StateDrivenIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header
        Text(
            text = "Backstack Editor",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Manipulate the navigation stack directly",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(4.dp))

        // Quick action buttons
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { onIntent(StateDrivenIntent.ShowAddDialog) },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }

            OutlinedButton(
                onClick = { onIntent(StateDrivenIntent.Pop) },
                enabled = state.canNavigateBack,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Default.ArrowUpward, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pop")
            }

            OutlinedButton(
                onClick = { onIntent(StateDrivenIntent.Clear) },
                enabled = state.isNotEmpty,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Default.Clear, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }

            OutlinedButton(
                onClick = { onIntent(StateDrivenIntent.Reset) },
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset")
            }
        }

        Spacer(Modifier.height(8.dp))

        // Stack label
        Text(
            text = "Stack (${state.size} entries)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Stack list
        if (state.isEmpty) {
            EmptyStackPlaceholder(
                onAddClick = { onIntent(StateDrivenIntent.ShowAddDialog) },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = state.entries,
                    key = { _, entry -> entry.id }
                ) { index, entry ->
                    val isTop = index == state.entries.lastIndex

                    BackstackEntryItem(
                        index = index,
                        destination = entry.destination,
                        entryId = entry.id,
                        isTop = isTop,
                        canMoveUp = index < state.entries.lastIndex,
                        canMoveDown = index > 0,
                        onRemove = { onIntent(StateDrivenIntent.RemoveAt(index)) },
                        onMoveUp = { onIntent(StateDrivenIntent.Swap(index, index + 1)) },
                        onMoveDown = { onIntent(StateDrivenIntent.Swap(index, index - 1)) }
                    )
                }
            }
        }

        // Info text
        Text(
            text = "Top of stack is at the bottom of the list",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // Add destination dialog
    if (state.showAddDialog) {
        AddDestinationDialog(
            onDismiss = { onIntent(StateDrivenIntent.HideAddDialog) },
            onAdd = { destination ->
                onIntent(StateDrivenIntent.PushDestination(destination))
                onIntent(StateDrivenIntent.HideAddDialog)
            }
        )
    }
}

@Composable
private fun EmptyStackPlaceholder(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Home,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "Stack is empty",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddClick) {
                Text("Add First Destination")
            }
        }
    }
}

@Composable
private fun BackstackEntryItem(
    index: Int,
    destination: NavDestination,
    entryId: String,
    isTop: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTop) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index badge
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelMedium,
                color = if (isTop) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(Modifier.width(12.dp))

            // Destination info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isTop) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = "ID: ${entryId.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isTop) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Reorder buttons
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = "Move down",
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "Move up",
                    modifier = Modifier.size(18.dp)
                )
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddDestinationDialog(
    onDismiss: () -> Unit,
    onAdd: (DemoTab) -> Unit
) {
    var selectedType by remember { mutableStateOf("Home") }
    var parameterValue by remember { mutableStateOf("") }

    val needsParameter = selectedType in listOf("Profile", "Detail")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Destination") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Destination type selector
                Text(
                    text = "Select destination type:",
                    style = MaterialTheme.typography.labelMedium
                )

                Column {
                    DemoTab.allTypes.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    parameterValue = when (type) {
                                        "Profile" -> "user_${(1..999).random()}"
                                        "Detail" -> "item_${(1..999).random()}"
                                        else -> ""
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = type)
                        }
                    }
                }

                // Parameter input if needed
                if (needsParameter) {
                    OutlinedTextField(
                        value = parameterValue,
                        onValueChange = { parameterValue = it },
                        label = {
                            Text(
                                when (selectedType) {
                                    "Profile" -> "User ID"
                                    "Detail" -> "Item ID"
                                    else -> "Parameter"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val destination = when (selectedType) {
                        "Home" -> DemoTab.Home
                        "Profile" -> DemoTab.Profile(
                            parameterValue.ifBlank { "default_user" }
                        )

                        "Settings" -> DemoTab.Settings
                        "Detail" -> DemoTab.Detail(
                            parameterValue.ifBlank { "default_item" }
                        )

                        else -> DemoTab.Home
                    }
                    onAdd(destination)
                },
                enabled = !needsParameter || parameterValue.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
