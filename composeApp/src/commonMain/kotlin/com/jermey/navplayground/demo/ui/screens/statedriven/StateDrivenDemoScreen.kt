package com.jermey.navplayground.demo.ui.screens.statedriven

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.StateDrivenDemoDestination
import com.jermey.navplayground.demo.destinations.StateDrivenDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import org.koin.compose.koinInject

/**
 * State-Driven Navigation Demo Screen.
 *
 * This screen demonstrates the Navigation 3-style state-driven navigation API.
 * It shows a split view with:
 * - Left/Top: BackstackEditorPanel for manipulating the navigation stack
 * - Right/Bottom: StateNavHost showing the current destination content
 *
 * Key concepts demonstrated:
 * - Direct backstack manipulation (push, pop, insert, remove, swap)
 * - Real-time state observation using Compose's snapshot system
 * - Declarative navigation without NavController
 */
@Screen(StateDrivenDemoDestination.Demo::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateDrivenDemoScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    val backStack = remember {
        DemoBackStack().apply {
            push(StateDrivenDestination.Home)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("State-Driven Navigation") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        StateDrivenDemoContent(
            backStack = backStack,
            modifier = Modifier.padding(padding)
        )
    }
}

/**
 * Breakpoint for switching between horizontal and vertical layouts.
 */
private val WIDE_LAYOUT_BREAKPOINT = 600.dp

@Suppress("MagicNumber")
@Composable
private fun StateDrivenDemoContent(
    backStack: DemoBackStack,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= WIDE_LAYOUT_BREAKPOINT

        if (isWideLayout) {
            // Horizontal layout for wider screens (tablets, desktop)
            Row(modifier = Modifier.fillMaxSize()) {
                // Left panel: Backstack Editor
                BackstackEditorPanel(
                    backStack = backStack,
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                )

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Right panel: Content area with StateNavHost
                Column(
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxHeight()
                ) {
                    StateInfoBar(
                        backStack = backStack,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ContentHost(
                        backStack = backStack,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Vertical layout for narrow screens (phones)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // State info bar at top
                StateInfoBar(
                    backStack = backStack,
                    modifier = Modifier.fillMaxWidth()
                )

                // Content area (fixed height)
                ContentHost(
                    backStack = backStack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                )

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                // Backstack Editor below
                BackstackEditorPanel(
                    backStack = backStack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentHost(
    backStack: DemoBackStack,
    modifier: Modifier = Modifier
) {
    val currentEntry = backStack.current

    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        AnimatedContent(
            targetState = currentEntry,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            contentKey = { it?.id }
        ) { entry ->
            if (entry != null) {
                when (val destination = entry.destination) {
                    is StateDrivenDestination.Home -> HomeContent()
                    is StateDrivenDestination.Profile -> ProfileContent(userId = destination.userId)
                    is StateDrivenDestination.Settings -> SettingsContent()
                    is StateDrivenDestination.Detail -> DetailContent(itemId = destination.itemId)
                    else -> {
                        // Fallback for any unknown destination
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Unknown destination: $destination")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StateInfoBar(
    backStack: DemoBackStack,
    modifier: Modifier = Modifier
) {
    // Re-read the state to trigger recomposition when backstack changes
    @Suppress("UNUSED_VARIABLE")
    val stateValue = backStack.state.collectAsState()
    val canGoBack = backStack.canNavigateBack
    val current = backStack.current

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
                value = backStack.size.toString()
            )

            Spacer(Modifier.width(16.dp))

            StateInfoItem(
                label = "Can Go Back",
                value = if (canGoBack) "Yes" else "No"
            )

            Spacer(Modifier.width(16.dp))

            StateInfoItem(
                label = "Current",
                value = (current?.destination as? StateDrivenDestination)?.let {
                    StateDrivenDestination.getDisplayName(it)
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
