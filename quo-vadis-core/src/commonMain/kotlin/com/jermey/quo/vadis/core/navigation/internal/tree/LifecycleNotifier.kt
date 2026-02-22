package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode

/**
 * Handles lifecycle notifications for navigation tree changes.
 *
 * When nodes are removed from the tree, this notifier ensures their
 * [LifecycleAwareNode.detachFromNavigator] callbacks are properly invoked.
 */
internal class LifecycleNotifier {

    /**
     * Notify pre-computed removed lifecycle-aware nodes that they have been detached.
     *
     * Calls [LifecycleAwareNode.detachFromNavigator] on each removed node.
     * Used with [TreeDiffCalculator] for single-pass tree diffing.
     *
     * @param removedNodes Lifecycle-aware nodes that were removed from the tree
     */
    fun notifyRemovedNodes(removedNodes: List<LifecycleAwareNode>) {
        removedNodes.forEach { node ->
            node.detachFromNavigator()
        }
    }

    /**
     * Notify lifecycle-aware nodes that were removed from the navigation tree.
     *
     * Compares the old and new tree states, and calls
     * [LifecycleAwareNode.detachFromNavigator] on all nodes that existed
     * in the old state but are not present in the new state.
     *
     * @param oldState The previous navigation tree state
     * @param newState The new navigation tree state
     */
    fun notifyRemovedNodesDetached(oldState: NavNode, newState: NavNode) {
        val oldNodes = collectLifecycleAwareNodes(oldState)
        val newNodeKeys = collectLifecycleAwareNodeKeys(newState)

        oldNodes.forEach { node ->
            if (node.key !in newNodeKeys) {
                (node as? LifecycleAwareNode)?.detachFromNavigator()
            }
        }
    }

    private fun collectLifecycleAwareNodes(node: NavNode): List<NavNode> {
        val nodes = mutableListOf<NavNode>()
        collectLifecycleAwareNodesRecursive(node, nodes)
        return nodes
    }

    private fun collectLifecycleAwareNodesRecursive(node: NavNode, nodes: MutableList<NavNode>) {
        when (node) {
            is ScreenNode -> nodes.add(node)
            is StackNode -> node.children.forEach { collectLifecycleAwareNodesRecursive(it, nodes) }
            is TabNode -> {
                nodes.add(node)
                node.stacks.forEach { collectLifecycleAwareNodesRecursive(it, nodes) }
            }

            is PaneNode -> {
                nodes.add(node)
                node.paneConfigurations.values.forEach {
                    collectLifecycleAwareNodesRecursive(it.content, nodes)
                }
            }
        }
    }

    private fun collectLifecycleAwareNodeKeys(node: NavNode): Set<NodeKey> {
        val keys = mutableSetOf<NodeKey>()
        collectLifecycleAwareNodeKeysRecursive(node, keys)
        return keys
    }

    private fun collectLifecycleAwareNodeKeysRecursive(node: NavNode, keys: MutableSet<NodeKey>) {
        when (node) {
            is ScreenNode -> keys.add(node.key)
            is StackNode -> node.children.forEach {
                collectLifecycleAwareNodeKeysRecursive(it, keys)
            }

            is TabNode -> {
                keys.add(node.key)
                node.stacks.forEach { collectLifecycleAwareNodeKeysRecursive(it, keys) }
            }

            is PaneNode -> {
                keys.add(node.key)
                node.paneConfigurations.values.forEach {
                    collectLifecycleAwareNodeKeysRecursive(it.content, keys)
                }
            }
        }
    }
}
