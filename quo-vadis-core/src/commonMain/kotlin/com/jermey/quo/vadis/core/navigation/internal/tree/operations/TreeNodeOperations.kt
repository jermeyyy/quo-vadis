package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.findByKey

/**
 * Core tree manipulation utilities.
 *
 * Provides foundational operations for modifying the NavNode tree:
 * - Node replacement (structural sharing for immutability)
 * - Node removal
 *
 * These operations are used internally by specialized operation classes
 * (PushOperations, PopOperations, etc.) and the TreeMutator façade.
 */
@InternalQuoVadisApi
object TreeNodeOperations {

    /**
     * Replace a node in the tree by key.
     *
     * Performs a single depth-first traversal to find and replace the node
     * with [targetKey], rebuilding the tree path with [newNode] in place.
     * Unchanged subtrees are reused by reference (structural sharing).
     *
     * @param root The root NavNode of the navigation tree
     * @param targetKey Key of the node to replace
     * @param newNode The replacement node
     * @return New tree with the node replaced
     * @throws IllegalArgumentException if targetKey not found
     */
    fun replaceNode(root: NavNode, targetKey: NodeKey, newNode: NavNode): NavNode {
        return tryReplaceNode(root, targetKey, newNode)
            ?: throw IllegalArgumentException("Node with key '$targetKey' not found")
    }

    /**
     * Attempts to replace a node in the subtree rooted at [root].
     *
     * @return The modified subtree if [targetKey] was found, or null if not found.
     */
    private fun tryReplaceNode(root: NavNode, targetKey: NodeKey, newNode: NavNode): NavNode? {
        if (root.key == targetKey) return newNode

        return when (root) {
            is ScreenNode -> null

            is StackNode -> {
                var found = false
                val newChildren = root.children.map { child ->
                    if (!found) {
                        val result = tryReplaceNode(child, targetKey, newNode)
                        if (result != null) { found = true; result } else child
                    } else {
                        child
                    }
                }
                if (found) root.copy(children = newChildren) else null
            }

            is TabNode -> {
                var found = false
                val newStacks = root.stacks.map { stack ->
                    if (!found) {
                        val result = tryReplaceNode(stack, targetKey, newNode)
                        if (result != null) { found = true; result as StackNode } else stack
                    } else {
                        stack
                    }
                }
                if (found) root.copy(stacks = newStacks) else null
            }

            is PaneNode -> {
                var found = false
                val newConfigurations = root.paneConfigurations.mapValues { (_, config) ->
                    if (!found) {
                        val result = tryReplaceNode(config.content, targetKey, newNode)
                        if (result != null) { found = true; config.copy(content = result) } else config
                    } else {
                        config
                    }
                }
                if (found) root.copy(paneConfigurations = newConfigurations) else null
            }
        }
    }

    /**
     * Remove a node from the tree by key.
     *
     * The behavior depends on the parent node type:
     * - From StackNode: Removes child from children list
     * - From TabNode: Cannot remove stacks (throws exception)
     * - From PaneNode: Cannot remove panes (throws exception)
     *
     * @param root The root NavNode of the navigation tree
     * @param targetKey Key of the node to remove
     * @return New tree with the node removed, or null if root itself would be removed
     * @throws IllegalArgumentException if targetKey not found or removal not allowed
     */
    fun removeNode(root: NavNode, targetKey: NodeKey): NavNode? {
        // Cannot remove the root
        if (root.key == targetKey) return null

        return when (root) {
            is ScreenNode -> {
                throw IllegalArgumentException("Node with key '$targetKey' not found")
            }

            is StackNode -> {
                // Check if any child has this key
                val childIndex = root.children.indexOfFirst { it.key == targetKey }
                if (childIndex != -1) {
                    val newChildren = root.children.toMutableList().apply { removeAt(childIndex) }
                    return root.copy(children = newChildren)
                }

                // Search recursively
                val newChildren = root.children.map { child ->
                    if (child.findByKey(targetKey) != null) {
                        removeNode(child, targetKey)
                            ?: throw IllegalArgumentException("Cannot remove root of subtree '$targetKey'")
                    } else child
                }
                if (newChildren == root.children) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(children = newChildren)
            }

            is TabNode -> {
                // Cannot remove stacks directly from TabNode
                if (root.stacks.any { it.key == targetKey }) {
                    throw IllegalArgumentException(
                        "Cannot remove stack '$targetKey' from TabNode - use switchTab instead"
                    )
                }

                val newStacks = root.stacks.map { stack ->
                    if (stack.findByKey(targetKey) != null) {
                        removeNode(stack, targetKey) as? StackNode
                            ?: throw IllegalArgumentException("Cannot remove root of subtree '$targetKey'")
                    } else stack
                }
                if (newStacks == root.stacks) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(stacks = newStacks)
            }

            is PaneNode -> {
                // Cannot remove pane configurations directly
                if (root.paneConfigurations.values.any { it.content.key == targetKey }) {
                    throw IllegalArgumentException(
                        "Cannot remove pane content '$targetKey' directly - use removePaneConfiguration instead"
                    )
                }

                val newConfigurations = root.paneConfigurations.mapValues { (_, config) ->
                    if (config.content.findByKey(targetKey) != null) {
                        val newContent = removeNode(config.content, targetKey)
                            ?: throw IllegalArgumentException("Cannot remove root of subtree '$targetKey'")
                        config.copy(content = newContent)
                    } else {
                        config
                    }
                }
                if (newConfigurations == root.paneConfigurations) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(paneConfigurations = newConfigurations)
            }
        }
    }
}
