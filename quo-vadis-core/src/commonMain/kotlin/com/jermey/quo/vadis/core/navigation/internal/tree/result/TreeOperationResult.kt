package com.jermey.quo.vadis.core.navigation.internal.tree.result

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey

/**
 * Result of a tree node operation (replace or remove).
 *
 * Replaces exception-based error handling with sealed result types
 * for explicit and idiomatic Kotlin error handling.
 */
@InternalQuoVadisApi
sealed interface TreeOperationResult {
    /** The operation completed successfully, producing a new tree. */
    data class Success(val newTree: NavNode) : TreeOperationResult

    /** The target node key was not found in the tree. */
    data class NodeNotFound(val key: NodeKey) : TreeOperationResult
}

/**
 * Returns the new tree from a [TreeOperationResult.Success],
 * or [fallback] if the node was not found.
 */
@InternalQuoVadisApi
fun TreeOperationResult.getOrElse(fallback: NavNode): NavNode = when (this) {
    is TreeOperationResult.Success -> newTree
    is TreeOperationResult.NodeNotFound -> fallback
}

/**
 * Returns the new tree from a [TreeOperationResult.Success],
 * or null if the node was not found.
 */
@InternalQuoVadisApi
fun TreeOperationResult.getOrNull(): NavNode? = when (this) {
    is TreeOperationResult.Success -> newTree
    is TreeOperationResult.NodeNotFound -> null
}
