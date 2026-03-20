package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.BackHandlerDemoDestination
import com.jermey.navplayground.demo.ui.screens.backhandler.BackHandlerDemoAction
import com.jermey.navplayground.demo.ui.screens.backhandler.BackHandlerDemoContainer
import com.jermey.navplayground.demo.ui.screens.backhandler.BackHandlerDemoIntent
import com.jermey.navplayground.demo.ui.screens.backhandler.BackHandlerDemoState
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.flowmvi.rememberContainer
import org.koin.core.qualifier.qualifier
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Back Handler Demo Screen - Demonstrates MVI container back handler interception.
 *
 * Shows an editor with unsaved changes detection. When the user has modified the text
 * and tries to navigate back (via system back button or gesture), a confirmation dialog
 * is shown instead of immediately navigating away.
 *
 * This demonstrates:
 * - `scope.registerBackHandler()` in a [BackHandlerDemoContainer] for intercepting user back events
 * - MVI state-driven back handler that checks `hasUnsavedChanges`
 * - Programmatic `navigateBack()` (via intent) to bypass the handler after confirmation
 * - Automatic handler cleanup when the container scope is closed
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(BackHandlerDemoDestination::class)
@Composable
fun BackHandlerDemoScreen(
    modifier: Modifier = Modifier,
    container: Store<BackHandlerDemoState, BackHandlerDemoIntent, BackHandlerDemoAction> =
        rememberContainer(qualifier<BackHandlerDemoContainer>()),
) {
    val state by container.subscribe()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Back Handler Demo") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.hasUnsavedChanges) {
                            container.intent(BackHandlerDemoIntent.ShowDiscardDialog)
                        } else {
                            container.intent(BackHandlerDemoIntent.NavigateBack)
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "MVI Container Back Handler",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Type something below, then try pressing the back button or swiping back. " +
                                "The MVI container's registerBackHandler() intercepts the event and " +
                                "shows a confirmation dialog.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedTextField(
                value = state.text,
                onValueChange = { container.intent(BackHandlerDemoIntent.UpdateText(it)) },
                label = { Text("Edit something...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (state.hasUnsavedChanges) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Back handler:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (state.hasUnsavedChanges) "Active (intercepting back)" else "Inactive",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.hasUnsavedChanges) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How it works",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Container uses scope.registerBackHandler() in whileSubscribed\n" +
                                "• Handler always returns true and sends HandleSystemBack intent\n" +
                                "• Store checks current state via updateState { }\n" +
                                "• Unsaved changes → shows dialog via state update\n" +
                                "• No changes → navigator.navigateBack() (bypasses registry)\n" +
                                "• Handler auto-cleaned on container scope close",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Discard changes confirmation dialog
    if (state.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { container.intent(BackHandlerDemoIntent.DismissDiscardDialog) },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(onClick = {
                    container.intent(BackHandlerDemoIntent.DiscardAndNavigateBack)
                }) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    container.intent(BackHandlerDemoIntent.DismissDiscardDialog)
                }) {
                    Text("Keep editing")
                }
            }
        )
    }
}
