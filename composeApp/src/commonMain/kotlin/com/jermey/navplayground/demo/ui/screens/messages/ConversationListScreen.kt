package com.jermey.navplayground.demo.ui.screens.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MessagesPane
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.navigator.asPaneNavigator
import org.koin.compose.koinInject

/**
 * Conversation list screen showing all conversations.
 *
 * This is the PRIMARY pane in the messages demo, displaying a list of
 * conversations that users can tap to view details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MessagesPane.ConversationList::class)
@Composable
fun ConversationListScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        modifier = modifier,
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            items(
                items = MockConversations.conversations,
                key = { it.id }
            ) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    onClick = {
                        // Use navigateToPane to REPLACE the secondary pane content
                        // so each selection swaps the detail view instead of stacking multiple details
                        navigator.asPaneNavigator()
                            ?.navigateToPane(MessagesPane.ConversationDetail(conversation.id))
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

/**
 * Individual conversation item in the list.
 *
 * @param conversation The conversation data to display
 * @param onClick Called when the item is clicked
 */
@Composable
private fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar with initials
        Avatar(
            initials = conversation.avatarInitials,
            modifier = Modifier.size(48.dp),
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Content: name, last message, timestamp
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = conversation.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conversation.unreadCount > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = conversation.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Unread badge
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    UnreadBadge(count = conversation.unreadCount)
                }
            }
        }
    }
}

/**
 * Avatar component showing initials in a colored circle.
 *
 * @param initials Two-letter initials to display
 */
@Composable
private fun Avatar(
    initials: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * Badge showing unread message count.
 *
 * @param count Number of unread messages
 */
@Composable
private fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
) {
    Badge(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
