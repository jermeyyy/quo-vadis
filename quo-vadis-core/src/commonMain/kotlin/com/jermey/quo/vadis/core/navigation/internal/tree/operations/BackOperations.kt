package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.tree.result.BackResult
import com.jermey.quo.vadis.core.navigation.internal.tree.result.PopResult
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.internal.tree.operations.PaneOperations.popWithPaneBehavior
import com.jermey.quo.vadis.core.navigation.internal.tree.operations.PopOperations.pop
import com.jermey.quo.vadis.core.navigation.internal.tree.operations.TreeNodeOperations.removeNode
import com.jermey.quo.vadis.core.navigation.internal.tree.result.TreeOperationResult

/**
 * Tree-aware back navigation operations.
 * 
 * Handles back navigation with awareness of the full tree structure:
 * - Pop with tab behavior (return to first tab before exiting)
 * - Nested stack handling
 * - Pane back behavior
 * - Query whether back navigation is possible
 */
@InternalQuoVadisApi
object BackOperations {

    /**
     * Check if back navigation is possible from the current state.
     *
     * Returns true if there is at least one screen that can be popped
     * from any active stack in the tree.
     *
     * @param root The root NavNode of the navigation tree
     * @return true if back navigation is possible
     */
    fun canGoBack(root: NavNode): Boolean {
        val activeStack = root.activeStack() ?: return false
        return activeStack.canGoBack
    }

    /**
     * Get the current active destination.
     *
     * @param root The root NavNode of the navigation tree
     * @return The current active Destination, or null if tree is empty
     */
    fun currentDestination(root: NavNode): NavDestination? {
        return root.activeLeaf()?.destination
    }

    /**
     * Pop with intelligent tab handling.
     *
     * Handles back navigation with proper tab behavior:
     * 1. If active tab's stack has > 1 items → pop from stack
     * 2. If active tab's stack is at root AND not initial tab → switch to initial tab
     * 3. If on initial tab at root → continue to next stack level
     *
     * @param root The root NavNode
     * @param isCompact Whether in compact mode (single pane visible). Default true.
     *                  In expanded mode, panes are popped entirely rather than switching.
     * @return BackResult indicating the outcome
     */
    fun popWithTabBehavior(root: NavNode, isCompact: Boolean = true): BackResult {
        val activeStack = root.activeStack() ?: return BackResult.CannotHandle

        // Case 1: Stack has items to pop
        if (activeStack.canGoBack) {
            val newState = pop(root) ?: return BackResult.CannotHandle
            return BackResult.Handled(newState)
        }

        // Find the parent tab node if any
        val parentKey = activeStack.parentKey ?: return handleRootStackBack(root, activeStack)
        return when (val parent = root.findByKey(parentKey)) {
            is TabNode -> handleTabBack(root, parent)
            is StackNode -> handleNestedStackBack(root, parent, activeStack)
            is PaneNode -> handlePaneBack(root, parent, isCompact)
            else -> BackResult.CannotHandle
        }
    }

    /**
     * Check if back navigation is possible, considering root constraints.
     *
     * Unlike [canGoBack], this method considers:
     * - Root stack must keep at least one item (would delegate to system)
     * - Tab switching as an alternative to popping
     * - Cascade back: removing TabNode from parent when on initial tab
     *
     * @param root The root NavNode
     * @return true if the navigation system can handle back (not delegated to system)
     */
    fun canHandleBackNavigation(root: NavNode): Boolean {
        val activeStack = root.activeStack() ?: return false

        // Can pop from active stack
        if (activeStack.canGoBack) return true

        // Check for tab switch opportunity or cascade back
        val parentKey = activeStack.parentKey ?: return false
        return when (val parent = root.findByKey(parentKey)) {
            is TabNode -> {
                // TabNode can be popped if its parent stack has multiple children
                val tabParentKey = parent.parentKey ?: return false
                val tabParent = root.findByKey(tabParentKey)
                (tabParent as? StackNode)?.children?.size?.let { it > 1 } ?: false
            }

            is StackNode -> parent.canGoBack

            is PaneNode -> {
                // Check if any pane's stack can go back
                val anyPaneCanGoBack = parent.paneConfigurations.values.any {
                    it.content.activeStack()?.canGoBack == true
                }
                if (anyPaneCanGoBack) return true

                // If all panes are at root, check if PaneNode itself can be popped
                val paneParentKey = parent.parentKey ?: return false
                val paneParent = root.findByKey(paneParentKey)
                (paneParent as? StackNode)?.children?.size?.let { it > 1 } ?: false
            }

            else -> false
        }
    }

    /**
     * Handle back when the active stack is a root stack.
     */
    private fun handleRootStackBack(root: NavNode, activeStack: StackNode): BackResult {
        // Root stack at minimum size - delegate to system
        return if (activeStack.children.size <= 1) {
            BackResult.DelegateToSystem
        } else {
            val newState = pop(root) ?: return BackResult.CannotHandle
            BackResult.Handled(newState)
        }
    }

    /**
     * Handle back when active stack is inside a TabNode.
     *
     * Simplified behavior:
     * - If active tab's stack has only 1 child: pop entire TabNode (regardless of which tab)
     * - If parent also has only one child: cascade further up the tree
     *
     * Note: This does NOT switch tabs on back - it pops the entire TabNode.
     */
    private fun handleTabBack(root: NavNode, tabNode: TabNode): BackResult {
        // Active tab's stack has only 1 child → try to pop the entire TabNode
        val tabParentKey = tabNode.parentKey ?: // TabNode is root - delegate to system
        return BackResult.DelegateToSystem

        // TabNode has a parent - try to pop from it
        return when (val tabParent = root.findByKey(tabParentKey)) {
            is StackNode -> {
                if (tabParent.children.size > 1) {
                    // Parent stack has multiple children - can pop TabNode
                    when (val result = removeNode(root, tabNode.key)) {
                        is TreeOperationResult.Success -> BackResult.Handled(result.newTree)
                        is TreeOperationResult.NodeNotFound, null -> BackResult.CannotHandle
                    }
                } else if (tabParent.parentKey == null) {
                    // Parent is root stack with only TabNode - delegate to system
                    BackResult.DelegateToSystem
                } else {
                    // CASCADE: Parent stack has only TabNode - try to pop parent from grandparent
                    val grandparentKey = tabParent.parentKey
                    when (val grandparent = root.findByKey(grandparentKey)) {
                        is StackNode -> handleNestedStackBack(root, grandparent, tabParent)
                        is TabNode -> handleTabBack(root, grandparent)
                        is PaneNode -> handlePaneBack(
                            root,
                            grandparent,
                            isCompact = true
                        )

                        else -> BackResult.DelegateToSystem
                    }
                }
            }

            is TabNode -> {
                // TabNode inside another TabNode (edge case)
                // Treat parent TabNode as if we're on its stack
                handleTabBack(root, tabParent)
            }

            else -> BackResult.DelegateToSystem
        }
    }

    /**
     * Handle back when active stack is nested inside another stack.
     *
     * Cascade behavior:
     * - If parent can pop child (size > 1): pop child
     * - If parent is root: delegate to system
     * - If parent also has only 1 child: cascade to grandparent
     */
    private fun handleNestedStackBack(
        root: NavNode,
        parentStack: StackNode,
        childStack: StackNode
    ): BackResult {
        return if (parentStack.children.size > 1) {
            // Parent has multiple children - can remove the child stack
            when (val result = removeNode(root, childStack.key)) {
                is TreeOperationResult.Success -> BackResult.Handled(result.newTree)
                is TreeOperationResult.NodeNotFound, null -> BackResult.CannotHandle
            }
        } else if (parentStack.parentKey == null) {
            // Parent is root with only one child - delegate to system
            BackResult.DelegateToSystem
        } else {
            // Parent has only one child and is not root - CASCADE UP
            // Try to pop the parent stack from its grandparent
            val grandparentKey = parentStack.parentKey
            when (val grandparent = root.findByKey(grandparentKey)) {
                is StackNode -> handleNestedStackBack(root, grandparent, parentStack)
                is TabNode -> handleTabBack(root, grandparent)
                is PaneNode -> handlePaneBack(root, grandparent, isCompact = true)
                else -> BackResult.DelegateToSystem
            }
        }
    }

    /**
     * Handle back when active stack is inside a PaneNode.
     *
     * Behavior depends on window size:
     * - In compact mode: use pane back behavior (switch panes, pop within panes)
     * - In expanded mode: always pop the entire PaneNode (back exits the pane view)
     *
     * Cascade behavior:
     * - If PaneNode is root: delegate to system
     * - If PaneNode's parent can pop it: pop PaneNode
     * - If parent also has only one child: cascade further up the tree
     *
     * @param isCompact Whether in compact/single-pane mode
     */
    private fun handlePaneBack(
        root: NavNode,
        paneNode: PaneNode,
        isCompact: Boolean
    ): BackResult {
        // In expanded mode, always pop the entire PaneNode
        // Back should exit the pane view, not navigate within it
        if (!isCompact) {
            return popEntirePaneNode(root, paneNode)
        }

        // In compact mode, use pane behavior (switch panes, pop within panes)
        return when (val result = popWithPaneBehavior(root)) {
            is PopResult.Popped -> BackResult.Handled(result.newState)
            is PopResult.CannotPop, is PopResult.PaneEmpty -> {
                // Pane handling exhausted - pop entire PaneNode
                popEntirePaneNode(root, paneNode)
            }

            is PopResult.RequiresScaffoldChange -> BackResult.CannotHandle
        }
    }

    /**
     * Pop the entire PaneNode from its parent.
     */
    private fun popEntirePaneNode(root: NavNode, paneNode: PaneNode): BackResult {
        val paneParentKey = paneNode.parentKey ?: return BackResult.DelegateToSystem

        // Try to pop PaneNode from its parent
        return when (val paneParent = root.findByKey(paneParentKey)) {
            is StackNode -> {
                if (paneParent.children.size > 1) {
                    when (val result = removeNode(root, paneNode.key)) {
                        is TreeOperationResult.Success -> BackResult.Handled(result.newTree)
                        is TreeOperationResult.NodeNotFound, null -> BackResult.CannotHandle
                    }
                } else if (paneParent.parentKey == null) {
                    BackResult.DelegateToSystem
                } else {
                    // Continue cascading
                    val grandparentKey = paneParent.parentKey
                    when (val grandparent = root.findByKey(grandparentKey)) {
                        is StackNode -> handleNestedStackBack(root, grandparent, paneParent)
                        is TabNode -> handleTabBack(root, grandparent)
                        is PaneNode -> handlePaneBack(
                            root,
                            grandparent,
                            true
                        ) // compact mode for cascade
                        else -> BackResult.DelegateToSystem
                    }
                }
            }

            is TabNode -> {
                val activeStack = paneNode.activePaneContent?.activeStack()
                if (activeStack != null) {
                    handleTabBack(root, paneParent)
                } else {
                    BackResult.DelegateToSystem
                }
            }

            is PaneNode -> {
                val activeStack = paneNode.activePaneContent?.activeStack()
                if (activeStack != null) {
                    handlePaneBack(root, paneParent, true) // compact mode for cascade
                } else {
                    BackResult.DelegateToSystem
                }
            }

            else -> BackResult.DelegateToSystem
        }
    }
}
