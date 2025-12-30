package com.jermey.navplayground.demo.ui.screens.messages

/**
 * Represents a conversation in the messages demo.
 *
 * @param id Unique identifier for the conversation
 * @param name Contact or group name
 * @param lastMessage Preview of the most recent message
 * @param timestamp Formatted time of the last message
 * @param unreadCount Number of unread messages
 * @param avatarInitials Two-letter initials for avatar placeholder
 */
data class Conversation(
    val id: String,
    val name: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int,
    val avatarInitials: String,
)

/**
 * Represents a single message in a conversation.
 *
 * @param id Unique identifier for the message
 * @param text The message content
 * @param isFromMe Whether the message was sent by the current user
 * @param timestamp Formatted time when the message was sent
 */
data class Message(
    val id: String,
    val text: String,
    val isFromMe: Boolean,
    val timestamp: String,
)

/**
 * Provides mock conversation and message data for the messages demo.
 */
object MockConversations {

    val conversations: List<Conversation> = listOf(
        Conversation(
            id = "1",
            name = "Alice Johnson",
            lastMessage = "See you at the coffee shop! ☕",
            timestamp = "10:30 AM",
            unreadCount = 2,
            avatarInitials = "AJ",
        ),
        Conversation(
            id = "2",
            name = "Bob Smith",
            lastMessage = "The project deadline is tomorrow",
            timestamp = "9:15 AM",
            unreadCount = 0,
            avatarInitials = "BS",
        ),
        Conversation(
            id = "3",
            name = "Work Team",
            lastMessage = "Meeting moved to 3 PM",
            timestamp = "Yesterday",
            unreadCount = 5,
            avatarInitials = "WT",
        ),
        Conversation(
            id = "4",
            name = "Family Group",
            lastMessage = "Mom: Don't forget Sunday dinner!",
            timestamp = "Yesterday",
            unreadCount = 0,
            avatarInitials = "FG",
        ),
        Conversation(
            id = "5",
            name = "Charlie Brown",
            lastMessage = "Thanks for your help!",
            timestamp = "Tuesday",
            unreadCount = 0,
            avatarInitials = "CB",
        ),
        Conversation(
            id = "6",
            name = "Diana Prince",
            lastMessage = "Can you send me the files?",
            timestamp = "Monday",
            unreadCount = 1,
            avatarInitials = "DP",
        ),
        Conversation(
            id = "7",
            name = "Gym Buddies",
            lastMessage = "Workout at 6 AM tomorrow?",
            timestamp = "Sunday",
            unreadCount = 3,
            avatarInitials = "GB",
        ),
        Conversation(
            id = "8",
            name = "Emily Chen",
            lastMessage = "Happy holidays! 🎉",
            timestamp = "Dec 25",
            unreadCount = 0,
            avatarInitials = "EC",
        ),
    )

    private val messagesByConversation: Map<String, List<Message>> = mapOf(
        "1" to listOf(
            Message("1-1", "Hey! Are you free today?", isFromMe = false, timestamp = "10:15 AM"),
            Message("1-2", "Hi Alice! Yes, I'm free this afternoon", isFromMe = true, timestamp = "10:18 AM"),
            Message("1-3", "Great! Want to grab coffee?", isFromMe = false, timestamp = "10:20 AM"),
            Message("1-4", "Sounds perfect! Where should we meet?", isFromMe = true, timestamp = "10:22 AM"),
            Message("1-5", "How about the new place on Main Street?", isFromMe = false, timestamp = "10:25 AM"),
            Message("1-6", "Perfect, what time works for you?", isFromMe = true, timestamp = "10:27 AM"),
            Message("1-7", "See you at the coffee shop! ☕", isFromMe = false, timestamp = "10:30 AM"),
        ),
        "2" to listOf(
            Message("2-1", "Hey, quick question about the project", isFromMe = false, timestamp = "8:45 AM"),
            Message("2-2", "Sure, what's up?", isFromMe = true, timestamp = "8:50 AM"),
            Message("2-3", "Did you finish the documentation?", isFromMe = false, timestamp = "8:52 AM"),
            Message("2-4", "Almost done, just need to review it once more", isFromMe = true, timestamp = "9:00 AM"),
            Message("2-5", "Great! Remember, the deadline is tomorrow", isFromMe = false, timestamp = "9:05 AM"),
            Message("2-6", "The project deadline is tomorrow", isFromMe = false, timestamp = "9:15 AM"),
        ),
        "3" to listOf(
            Message("3-1", "Team, we need to reschedule the meeting", isFromMe = false, timestamp = "2:00 PM"),
            Message("3-2", "What time works better?", isFromMe = true, timestamp = "2:05 PM"),
            Message("3-3", "How about 3 PM?", isFromMe = false, timestamp = "2:10 PM"),
            Message("3-4", "Works for me!", isFromMe = true, timestamp = "2:12 PM"),
            Message("3-5", "Same here", isFromMe = false, timestamp = "2:15 PM"),
            Message("3-6", "Perfect, I'll update the calendar", isFromMe = false, timestamp = "2:20 PM"),
            Message("3-7", "Don't forget to bring your laptops", isFromMe = false, timestamp = "2:25 PM"),
            Message("3-8", "Meeting moved to 3 PM", isFromMe = false, timestamp = "2:30 PM"),
        ),
        "4" to listOf(
            Message("4-1", "Hi everyone! 👋", isFromMe = false, timestamp = "6:00 PM"),
            Message("4-2", "Hey Mom!", isFromMe = true, timestamp = "6:05 PM"),
            Message("4-3", "Are we still doing Sunday dinner?", isFromMe = false, timestamp = "6:10 PM"),
            Message("4-4", "Yes! I'll bring dessert", isFromMe = true, timestamp = "6:15 PM"),
            Message("4-5", "Perfect! Your sister is bringing salad", isFromMe = false, timestamp = "6:20 PM"),
            Message("4-6", "Sounds good!", isFromMe = true, timestamp = "6:25 PM"),
            Message("4-7", "Mom: Don't forget Sunday dinner!", isFromMe = false, timestamp = "6:30 PM"),
        ),
        "5" to listOf(
            Message("5-1", "Hey Charlie, do you need help with the setup?", isFromMe = true, timestamp = "11:00 AM"),
            Message("5-2", "Yes please! I'm stuck on the configuration", isFromMe = false, timestamp = "11:05 AM"),
            Message("5-3", "No problem, let me walk you through it", isFromMe = true, timestamp = "11:10 AM"),
            Message("5-4", "You're a lifesaver!", isFromMe = false, timestamp = "11:30 AM"),
            Message("5-5", "Thanks for your help!", isFromMe = false, timestamp = "11:35 AM"),
        ),
        "6" to listOf(
            Message("6-1", "Hi! I saw your presentation yesterday", isFromMe = false, timestamp = "3:00 PM"),
            Message("6-2", "Thanks Diana! How did you like it?", isFromMe = true, timestamp = "3:10 PM"),
            Message("6-3", "It was great! Very informative", isFromMe = false, timestamp = "3:15 PM"),
            Message("6-4", "I'm glad you enjoyed it", isFromMe = true, timestamp = "3:20 PM"),
            Message("6-5", "Can you send me the files?", isFromMe = false, timestamp = "3:25 PM"),
        ),
        "7" to listOf(
            Message("7-1", "Who's in for tomorrow's workout?", isFromMe = false, timestamp = "8:00 PM"),
            Message("7-2", "I'm in! 💪", isFromMe = true, timestamp = "8:05 PM"),
            Message("7-3", "Count me in too", isFromMe = false, timestamp = "8:10 PM"),
            Message("7-4", "Should we do cardio or weights?", isFromMe = false, timestamp = "8:15 PM"),
            Message("7-5", "Let's do both!", isFromMe = true, timestamp = "8:20 PM"),
            Message("7-6", "Sounds intense, I love it", isFromMe = false, timestamp = "8:25 PM"),
            Message("7-7", "Workout at 6 AM tomorrow?", isFromMe = false, timestamp = "8:30 PM"),
        ),
        "8" to listOf(
            Message("8-1", "Merry Christmas! 🎄", isFromMe = true, timestamp = "9:00 AM"),
            Message("8-2", "Happy holidays! 🎉", isFromMe = false, timestamp = "9:30 AM"),
            Message("8-3", "Hope you're having a great time with family", isFromMe = true, timestamp = "10:00 AM"),
            Message("8-4", "Yes! It's been wonderful. You too!", isFromMe = false, timestamp = "10:15 AM"),
            Message("8-5", "See you in the new year! 🥳", isFromMe = true, timestamp = "10:30 AM"),
        ),
    )

    /**
     * Returns the list of messages for a given conversation.
     *
     * @param conversationId The ID of the conversation to get messages for
     * @return List of messages, or empty list if conversation not found
     */
    fun getMessages(conversationId: String): List<Message> =
        messagesByConversation[conversationId].orEmpty()

    /**
     * Returns a conversation by its ID.
     *
     * @param conversationId The ID of the conversation to find
     * @return The conversation, or null if not found
     */
    fun getConversation(conversationId: String): Conversation? =
        conversations.find { it.id == conversationId }
}
