package com.jermey.quo.vadis.core.navigation.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.PaneNode
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.navigation.TabNode
import com.jermey.quo.vadis.core.navigation.findByKey

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
     * Performs a depth-first search for the node with [targetKey], then
     * rebuilds the tree path with [newNode] in place of the old node.
     * Unchanged subtrees are reused by reference (structural sharing).
     *
     * @param root The root NavNode of the navigation tree
     * @param targetKey Key of the node to replace
     * @param newNode The replacement node
     * @return New tree with the node replaced
     * @throws IllegalArgumentException if targetKey not found
     */
    fun replaceNode(root: NavNode, targetKey: String, newNode: NavNode): NavNode {
        if (root.key == targetKey) return newNode

        return when (root) {
            is ScreenNode -> {
                throw IllegalArgumentException("Node with key '$targetKey' not found")
            }

            is StackNode -> {
                val newChildren = root.children.map { child ->
                    if (child.key == targetKey) newNode
                    else if (child.findByKey(targetKey) != null) replaceNode(
                        child,
                        targetKey,
                        newNode
                    )
                    else child
                }
                if (newChildren == root.children) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(children = newChildren)
            }

            is TabNode -> {
                val newStacks = root.stacks.map { stack ->
                    if (stack.key == targetKey) newNode as StackNode
                    else if (stack.findByKey(targetKey) != null) {
                        replaceNode(stack, targetKey, newNode) as StackNode
                    } else stack
                }
                if (newStacks == root.stacks) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(stacks = newStacks)
            }

            is PaneNode -> {
                val newConfigurations = root.paneConfigurations.mapValues { (_, config) ->
                    if (config.content.key == targetKey) {
                        config.copy(content = newNode)
                    } else if (config.content.findByKey(targetKey) != null) {
                        config.copy(content = replaceNode(config.content, targetKey, newNode))
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
    fun removeNode(root: NavNode, targetKey: String): NavNode? {
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
