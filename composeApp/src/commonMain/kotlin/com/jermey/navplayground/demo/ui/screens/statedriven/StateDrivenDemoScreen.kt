package com.jermey.navplayground.demo.ui.screens.statedriven

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.StateDrivenDemoDestination
import com.jermey.navplayground.demo.destinations.StateDrivenDemoDestination.DemoTab
import com.jermey.quo.vadis.annotations.TabsContainer
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.flowmvi.rememberSharedContainer
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * State-Driven Navigation Demo TabsContainer.
 *
 * This screen demonstrates a state-driven navigation pattern using the Quo Vadis navigation framework.
 * It shows a split view with:
 * - Left/Top: BackstackEditorPanel for manipulating the navigation stack
 * - Right/Bottom: The actual NavigationHost content (rendered via the content slot)
 *
 * Key concepts demonstrated:
 * - Direct backstack manipulation (push, pop, insert, remove, swap)
 * - Real-time state observation using FlowMVI SharedContainer
 * - The content slot renders the actual DemoTab screens via NavigationHost
 *
 * @param scope The TabsContainerScope providing access to tab state and navigation
 * @param content The content slot where active tab content (the inner stack) is rendered
 */
@TabsContainer(StateDrivenDemoDestination.Companion::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateDrivenDemoContainer(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Get the shared store for state-driven navigation manipulation
    val sharedStore = rememberSharedContainer<StateDrivenContainer, StateDrivenState, StateDrivenIntent, StateDrivenAction>()

    val state by sharedStore.subscribe { action ->
        coroutineScope.launch {
            when (action) {
                is StateDrivenAction.ShowToast -> {
                    snackbarHostState.showSnackbar(
                        message = action.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    CompositionLocalProvider(LocalStateDrivenStore provides sharedStore) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("State-Driven Navigation") },
                    navigationIcon = {
                        IconButton(onClick = { scope.navigator.navigateBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            StateDrivenDemoContent(
                state = state,
                onIntent = sharedStore::intent,
                content = content,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

/**
 * Breakpoint for switching between horizontal and vertical layouts.
 */
private val WIDE_LAYOUT_BREAKPOINT = 600.dp

@Suppress("MagicNumber")
@Composable
private fun StateDrivenDemoContent(
    state: StateDrivenState,
    onIntent: (StateDrivenIntent) -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= WIDE_LAYOUT_BREAKPOINT

        if (isWideLayout) {
            // Horizontal layout for wider screens (tablets, desktop)
            Row(modifier = Modifier.fillMaxSize()) {
                // Left panel: Backstack Editor
                BackstackEditorPanel(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Right panel: Content area with actual NavigationHost
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    StateInfoBar(
                        state = state,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // The actual navigation content rendered by NavigationHost
                    Box(modifier = Modifier.fillMaxSize()) {
                        content()
                    }
                }
            }
        } else {
            // Vertical layout for narrow screens (phones)
            Column(modifier = Modifier.fillMaxSize()) {
                // State info bar at top
                StateInfoBar(
                    state = state,
                    modifier = Modifier.fillMaxWidth()
                )

                // Content area - actual NavigationHost content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    content()
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Backstack Editor below
                BackstackEditorPanel(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        }
    }
}

@Composable
private fun StateInfoBar(
    state: StateDrivenState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StateInfoItem(
                label = "Stack Size",
                value = state.size.toString()
            )

            Spacer(Modifier.width(16.dp))

            StateInfoItem(
                label = "Can Go Back",
                value = if (state.canNavigateBack) "Yes" else "No"
            )

            Spacer(Modifier.width(16.dp))

            StateInfoItem(
                label = "Current",
                value = (state.currentEntry?.destination as? DemoTab)?.let {
                    DemoTab.getDisplayName(it)
                } ?: "None"
            )
        }
    }
}

@Composable
private fun StateInfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
