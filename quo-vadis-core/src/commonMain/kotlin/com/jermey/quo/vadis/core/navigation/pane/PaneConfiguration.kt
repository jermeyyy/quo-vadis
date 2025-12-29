package com.jermey.quo.vadis.core.navigation.pane

import com.jermey.quo.vadis.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.AdaptStrategy
import kotlinx.serialization.Serializable

/**
 * Configuration for a single pane within a PaneNode.
 *
 * @property content The navigation content within this pane (typically a StackNode)
 * @property adaptStrategy Adaptation strategy when this pane cannot be expanded
 */
@Serializable
data class PaneConfiguration(
    val content: NavNode,
    val adaptStrategy: AdaptStrategy = AdaptStrategy.Hide
)
