package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Pane
import com.jermey.quo.vadis.annotations.PaneBackBehavior
import com.jermey.quo.vadis.annotations.PaneItem
import com.jermey.quo.vadis.annotations.PaneRole
import com.jermey.quo.vadis.core.navigation.NavDestination

/**
 * Messages pane container demonstrating adaptive list-detail layout.
 *
 * This pane container implements a messaging/conversations pattern with:
 * - **PRIMARY pane**: Conversation list (always visible)
 * - **SECONDARY pane**: Conversation detail (overlay on compact, side-by-side on expanded)
 *
 * ## Adaptive Behavior
 *
 * - **Expanded (tablet/desktop)**: Two-column layout with list and detail side-by-side
 * - **Compact (phone)**: Single pane with overlay navigation to detail
 *
 * ## Back Navigation
 *
 * Uses [PaneBackBehavior.PopUntilContentChange] to stop back navigation when any
 * visible pane's content updates, providing natural navigation for messaging apps.
 */
@Pane(name = "messagesPane", backBehavior = PaneBackBehavior.PopUntilContentChange)
sealed class MessagesPane : NavDestination {

    companion object : NavDestination

    /**
     * PRIMARY pane: Conversation list.
     *
     * Always visible, shows the list of conversations.
     */
    @PaneItem(role = PaneRole.PRIMARY)
    @Destination(route = "messages/conversations")
    data object ConversationList : MessagesPane()

    /**
     * Conversation detail showing messages.
     *
     * Pushed onto the secondary pane's stack when a conversation is selected.
     * Uses paneRole = SECONDARY to enable transparent navigation routing.
     * The secondary pane starts empty - the container shows a placeholder.
     */
    @PaneItem(role = PaneRole.SECONDARY)
    @Destination(route = "messages/conversation/{conversationId}")
    data class ConversationDetail(val conversationId: String) : MessagesPane()
}


