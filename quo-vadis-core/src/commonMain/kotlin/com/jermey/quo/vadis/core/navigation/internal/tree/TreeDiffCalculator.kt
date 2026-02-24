package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode

/**
 * Result of diffing old and new navigation trees in a single pass.
 *
 * @property removedLifecycleNodes Lifecycle-aware nodes present in old tree but not in new tree
 * @property removedScreenKeys Screen keys present in old tree but not in new tree
 */
internal data class TreeDiff(
    val removedLifecycleNodes: List<LifecycleAwareNode>,
    val removedScreenKeys: Set<NodeKey>
)

/**
 * Computes differences between navigation trees in a single traversal.
 *
 * This replaces separate traversals in [LifecycleNotifier] and [ScreenKeyCollector]
 * with a combined diff that collects both lifecycle nodes and screen keys
 * in one pass over each tree.
 */
internal object TreeDiffCalculator {

    /**
     * Computes the diff between old and new navigation trees.
     *
     * Single traversal of each tree (old + new) instead of the previous
     * 4 separate traversals (2 for lifecycle + 2 for screen keys).
     */
    fun computeDiff(oldState: NavNode, newState: NavNode): TreeDiff {
        val oldInfo = collectNodeInfo(oldState)
        val newInfo = collectNodeInfo(newState)

        val removedLifecycleNodeKeys = oldInfo.lifecycleNodeKeys - newInfo.lifecycleNodeKeys
        val removedLifecycleNodes = oldInfo.lifecycleNodes
            .filter { it.key in removedLifecycleNodeKeys }
            .map { it.node }
        val removedScreenKeys = oldInfo.screenKeys - newInfo.screenKeys

        return TreeDiff(
            removedLifecycleNodes = removedLifecycleNodes,
            removedScreenKeys = removedScreenKeys
        )
    }

    private data class NodeInfo(
        val lifecycleNodes: List<LifecycleNodeEntry>,
        val lifecycleNodeKeys: Set<NodeKey>,
        val screenKeys: Set<NodeKey>
    )

    private data class LifecycleNodeEntry(
        val key: NodeKey,
        val node: LifecycleAwareNode
    )

    private fun collectNodeInfo(root: NavNode): NodeInfo {
        val lifecycleNodes = mutableListOf<LifecycleNodeEntry>()
        val lifecycleNodeKeys = mutableSetOf<NodeKey>()
        val screenKeys = mutableSetOf<NodeKey>()

        collectRecursive(root, lifecycleNodes, lifecycleNodeKeys, screenKeys)

        return NodeInfo(lifecycleNodes, lifecycleNodeKeys, screenKeys)
    }

    private fun collectRecursive(
        node: NavNode,
        lifecycleNodes: MutableList<LifecycleNodeEntry>,
        lifecycleNodeKeys: MutableSet<NodeKey>,
        screenKeys: MutableSet<NodeKey>
    ) {
        // Collect lifecycle-aware nodes (ScreenNode, TabNode, PaneNode — not StackNode)
        if (node is LifecycleAwareNode) {
            lifecycleNodes.add(LifecycleNodeEntry(node.key, node))
            lifecycleNodeKeys.add(node.key)
        }

        // Collect screen keys
        if (node is ScreenNode) {
            screenKeys.add(node.key)
        }

        // Recurse into children
        when (node) {
            is ScreenNode -> { /* leaf node */ }
            is StackNode -> node.children.forEach {
                collectRecursive(it, lifecycleNodes, lifecycleNodeKeys, screenKeys)
            }
            is TabNode -> node.stacks.forEach {
                collectRecursive(it, lifecycleNodes, lifecycleNodeKeys, screenKeys)
            }
            is PaneNode -> node.paneConfigurations.values.forEach {
                collectRecursive(it.content, lifecycleNodes, lifecycleNodeKeys, screenKeys)
            }
        }
    }
}
