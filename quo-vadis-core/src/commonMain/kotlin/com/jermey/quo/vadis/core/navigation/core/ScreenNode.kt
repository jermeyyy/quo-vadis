package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Leaf node representing a single screen/destination.
 *
 * A ScreenNode is the terminal state in the navigation tree - it cannot
 * contain children. It holds a reference to the [Destination] that defines
 * the actual content to render.
 *
 * @property key Unique identifier for this screen instance
 * @property parentKey Key of the containing StackNode or PaneNode
 * @property destination The destination data (route, arguments, transitions)
 */
@Serializable
@SerialName("screen")
data class ScreenNode(
    override val key: String,
    override val parentKey: String?,
    val destination: Destination
) : NavNode