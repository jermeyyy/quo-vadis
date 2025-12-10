package com.jermey.navplayground.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.jermey.quo.vadis.core.navigation.compose.RenderingMode

/**
 * Settings item for rendering mode selection with dialog.
 *
 * Allows users to toggle between [RenderingMode.Flattened] and [RenderingMode.Hierarchical]
 * to compare the two rendering approaches in QuoVadisHost.
 *
 * @param currentMode The currently selected rendering mode
 * @param onModeChange Callback when the rendering mode is changed
 */
@Composable
fun RenderingModeSettingItem(
    currentMode: RenderingMode,
    onModeChange: (RenderingMode) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text("Rendering Mode") },
        supportingContent = {
            Text(
                when (currentMode) {
                    RenderingMode.Flattened -> "Flattened (stable)"
                    RenderingMode.Hierarchical -> "Hierarchical (experimental)"
                }
            )
        },
        leadingContent = {
            Icon(
                Icons.Default.Layers,
                contentDescription = "Rendering Mode"
            )
        },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        RenderingModeSelectionDialog(
            currentMode = currentMode,
            onModeSelected = { newMode ->
                onModeChange(newMode)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

/**
 * Dialog for selecting rendering mode.
 */
@Composable
private fun RenderingModeSelectionDialog(
    currentMode: RenderingMode,
    onModeSelected: (RenderingMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Rendering Mode") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RenderingModeOption(
                    label = "Flattened",
                    description = "Stable rendering with z-ordered surfaces. Tab/pane wrappers as siblings.",
                    selected = currentMode == RenderingMode.Flattened,
                    onClick = { onModeSelected(RenderingMode.Flattened) }
                )

                RenderingModeOption(
                    label = "Hierarchical",
                    description = "Experimental recursive rendering. True parent-child hierarchy for wrappers.",
                    selected = currentMode == RenderingMode.Hierarchical,
                    onClick = { onModeSelected(RenderingMode.Hierarchical) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Individual rendering mode option in the selection dialog.
 */
@Composable
private fun RenderingModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}
