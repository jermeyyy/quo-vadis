package com.jermey.navplayground.demo.ui.screens.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.MessagesPane
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.compose.wrapper.calculateWindowSizeClass
import com.jermey.quo.vadis.core.navigation.Navigator
import org.koin.compose.koinInject

/**
 * Detail screen showing messages for a specific conversation.
 *
 * Displays a chat-style view with:
 * - Top app bar with conversation name and back button
 * - LazyColumn of messages in chat bubble style
 * - Mock message input area at bottom
 *
 * @param destination The destination containing the conversation ID
 * @param navigator Navigator for handling back navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MessagesPane.ConversationDetail::class)
@Composable
fun ConversationDetailScreen(
    destination: MessagesPane.ConversationDetail,
    navigator: Navigator = koinInject(),
) {
    val conversation = remember(destination.conversationId) {
        MockConversations.getConversation(destination.conversationId)
    }
    val messages = remember(destination.conversationId) {
        MockConversations.getMessages(destination.conversationId)
    }
    val listState = rememberLazyListState(messages.lastIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(conversation?.name ?: "Conversation") },
                navigationIcon = {
                    if (calculateWindowSizeClass().isAtLeastMediumWidth.not()) {
                        IconButton(onClick = { navigator.navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            MessageInputArea()
        },
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/**
 * A single message bubble in the chat view.
 *
 * Messages from the current user are aligned to the right with primary color background.
 * Messages from others are aligned to the left with secondary color background.
 *
 * @param message The message to display
 */
@Composable
private fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier,
) {
    val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val backgroundColor = if (message.isFromMe) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (message.isFromMe) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val bubbleShape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
        bottomStart = if (message.isFromMe) 16.dp else 4.dp,
        bottomEnd = if (message.isFromMe) 4.dp else 16.dp,
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        Text(
            text = message.timestamp,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp),
        )
    }
}

/**
 * Mock message input area at the bottom of the chat screen.
 *
 * Contains a text field and send button. Non-functional for demo purposes.
 */
@Composable
private fun MessageInputArea(
    modifier: Modifier = Modifier,
) {
    var messageText by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
            )

            IconButton(
                onClick = {
                    // Non-functional for demo
                    messageText = ""
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
