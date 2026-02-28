package com.jermey.navplayground.demo.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MessagesPane
import com.jermey.quo.vadis.annotations.PaneContainer
import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * Pane container wrapper for the Messages adaptive layout.
 *
 * This container provides the UI chrome for the [MessagesPane] destination.
 * In expanded mode (medium+ screen width), it arranges panes side-by-side
 * with a vertical divider. In compact mode, it renders the single active pane.
 *
 * ## Layout
 *
 * - **Expanded (tablet/desktop)**: Two-column layout
 *   - Primary pane (conversation list): 40% width
 *   - Vertical divider
 *   - Supporting pane (conversation detail): 60% width
 *
 * - **Compact (phone)**: Single pane, handled by library
 *
 * @param scope Provides access to pane state (isExpanded, paneContents, activePaneRole, etc.)
 * @param content The pane contents rendered by the framework (used in compact mode)
 */
@PaneContainer(MessagesPane::class)
@Composable
fun MessagesPaneContainer(
    scope: PaneContainerScope,
    content: @Composable () -> Unit
) {
    if (scope.isExpanded) {
        // Expanded mode: Custom two-column layout with divider
        Row(modifier = Modifier.fillMaxSize()) {
            scope.paneContents.filter { it.isVisible }.sortedBy { it.role }.forEachIndexed { index, pane ->
                // Add divider between panes
                if (index > 0) {
                    VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                }

                // Use role-based weights for list-detail pattern
                val weight = when (pane.role) {
                    PaneRole.Primary -> 0.4f      // Conversation list (narrower)
                    PaneRole.Supporting -> 0.6f   // Conversation detail (wider)
                    PaneRole.Extra -> 0.25f       // Extra pane if any
                }

                Box(modifier = Modifier.weight(weight).fillMaxHeight()) {
                    // Show placeholder for empty secondary pane
                    if (pane.role == PaneRole.Supporting && !pane.hasContent) {
                        EmptyConversationPlaceholder()
                    } else {
                        pane.content()
                    }
                }
            }
        }
    } else {
        // Compact mode: Single pane navigation handled by library
        content()
    }
}

/**
 * Placeholder shown in expanded mode when no conversation is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyConversationPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(
                text = "Select a conversation",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Choose a conversation from the list to start chatting",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
