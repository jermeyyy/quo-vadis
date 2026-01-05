package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activePathToLeaf
import com.jermey.quo.vadis.core.navigation.node.findByKey

/**
 * Tab switching operations for TabNode.
 *
 * Handles tab navigation within TabNode containers:
 * - Switch to tab by key and index
 * - Switch active tab (finds active TabNode automatically)
 */
@InternalQuoVadisApi
object TabOperations {

    /**
     * Switch to a different tab in a TabNode.
     *
     * Updates the [TabNode.activeStackIndex] to the specified index.
     * The stacks themselves are unchanged - each tab preserves its history.
     *
     * @param root The root NavNode of the navigation tree
     * @param tabNodeKey Key of the TabNode to modify
     * @param newIndex The tab index to switch to (0-based)
     * @return New tree with the tab switched
     * @throws IllegalArgumentException if tabNodeKey not found or not a TabNode
     * @throws IndexOutOfBoundsException if newIndex is out of bounds
     */
    fun switchTab(
        root: NavNode,
        tabNodeKey: String,
        newIndex: Int
    ): NavNode {
        val tabNode = root.findByKey(tabNodeKey) as? TabNode
            ?: throw IllegalArgumentException("TabNode with key '$tabNodeKey' not found")

        require(newIndex in 0 until tabNode.tabCount) {
            "Tab index $newIndex out of bounds for ${tabNode.tabCount} tabs"
        }

        if (tabNode.activeStackIndex == newIndex) return root

        val newTabNode = tabNode.copy(activeStackIndex = newIndex)
        return TreeNodeOperations.replaceNode(root, tabNodeKey, newTabNode)
    }

    /**
     * Switch to a different tab in the first TabNode found in the active path.
     *
     * Traverses the active path to find the first TabNode, then switches
     * to the specified tab index. Useful when you don't know or don't want
     * to specify the exact TabNode key.
     *
     * @param root The root NavNode of the navigation tree
     * @param newIndex The tab index to switch to (0-based)
     * @return New tree with the tab switched
     * @throws IllegalStateException if no TabNode found in active path
     */
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode {
        // Find the first TabNode in the active path
        val activePath = root.activePathToLeaf()
        val tabNode = activePath.filterIsInstance<TabNode>().firstOrNull()
            ?: throw IllegalStateException("No TabNode found in active path")

        return switchTab(root, tabNode.key, newIndex)
    }
}
