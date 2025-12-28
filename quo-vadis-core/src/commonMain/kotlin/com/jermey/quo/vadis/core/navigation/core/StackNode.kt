package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container node representing a linear navigation stack.
 *
 * A StackNode maintains an ordered list of child nodes where the last
 * element is the "active" (visible) child. Push operations append to
 * the list, pop operations remove from the tail.
 *
 * ## Behavior
 *
 * - **Push**: Appends new node to [children]
 * - **Pop**: Removes last node from [children]
 * - **Empty Stack**: May trigger cascading pop to parent (configurable)
 *
 * ## Scope-Aware Navigation
 *
 * When [scopeKey] is set, [TreeMutator.push] with a [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] will check
 * if destinations belong to this stack's scope. Out-of-scope destinations
 * navigate to the parent stack instead, preserving this stack for
 * predictive back gestures.
 *
 * @property key Unique identifier for this stack
 * @property parentKey Key of the containing TabNode or PaneNode (null if root)
 * @property children Ordered list of child nodes (last = active)
 * @property scopeKey Identifier for scope-aware navigation. When set, destinations
 *   not in this scope will navigate outside the stack. Typically the
 *   sealed class simple name (e.g., "AuthFlow"). Defaults to null (no scope enforcement).
 */
@Serializable
@SerialName("stack")
data class StackNode(
    override val key: String,
    override val parentKey: String?,
    val children: List<NavNode> = emptyList(),
    val scopeKey: String? = null
) : NavNode {

    /**
     * The currently active (visible) child node.
     * Returns null if the stack is empty.
     */
    val activeChild: NavNode?
        get() = children.lastOrNull()

    /**
     * Returns true if this stack has navigable history (more than one entry).
     */
    val canGoBack: Boolean
        get() = children.size > 1

    /**
     * Returns true if the stack is empty.
     */
    val isEmpty: Boolean
        get() = children.isEmpty()

    /**
     * Returns the number of entries in this stack.
     */
    val size: Int
        get() = children.size

    companion object
}
