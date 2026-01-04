package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.internal.tree.config.PopBehavior
import com.jermey.quo.vadis.core.navigation.internal.tree.operations.TreeNodeOperations.removeNode
import com.jermey.quo.vadis.core.navigation.internal.tree.operations.TreeNodeOperations.replaceNode

/**
 * Pop operations for the navigation tree.
 *
 * Handles all backward navigation:
 * - Simple pop from active stack
 * - Pop to specific screen by key
 * - Pop to route pattern
 * - Pop to destination type
 */
@InternalQuoVadisApi
object PopOperations {

    /**
     * Pop the active screen from the deepest active stack.
     *
     * Removes the last child from the deepest active StackNode. The [behavior]
     * parameter controls what happens if the stack becomes empty after popping.
     *
     * ## Cascade Behavior
     *
     * With [PopBehavior.CASCADE], if popping leaves a stack empty:
     * - The empty stack is removed from its parent
     * - This may recursively propagate up the tree
     * - Returns null if popping would leave the root empty
     *
     * ## Example
     *
     * ```kotlin
     * val result = TreeMutator.pop(root, PopBehavior.PRESERVE_EMPTY)
     * if (result == null) {
     *     // Cannot pop - at root or tree is empty
     * }
     * ```
     *
     * @param root The root NavNode of the navigation tree
     * @param behavior How to handle an empty stack after popping
     * @return New tree with the top screen popped, or null if cannot pop
     */
    fun pop(
        root: NavNode,
        behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY
    ): NavNode? {
        val targetStack = root.activeStack() ?: return null

        // Cannot pop from empty stack
        if (targetStack.isEmpty) return null

        // Pop the last child
        val newChildren = targetStack.children.dropLast(1)

        // Handle empty stack based on behavior
        if (newChildren.isEmpty()) {
            return handleEmptyStackPop(root, targetStack, behavior)
        }

        val newStack = targetStack.copy(children = newChildren)
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Pop all screens from the active stack until the predicate matches.
     *
     * Searches from the back (most recent) to the front of the active stack
     * for a node matching the predicate. All nodes after the match are removed.
     *
     * @param root The root NavNode of the navigation tree
     * @param inclusive If true, also removes the matching node
     * @param predicate Function to identify the target node
     * @return New tree with nodes popped, or original tree if no match found
     */
    fun popTo(
        root: NavNode,
        inclusive: Boolean = false,
        predicate: (NavNode) -> Boolean
    ): NavNode {
        val targetStack = root.activeStack() ?: return root

        // Find the index of the matching node (searching from back to front)
        val matchIndex = targetStack.children.indexOfLast { predicate(it) }
        if (matchIndex == -1) return root

        // Calculate how many to keep
        val keepCount = if (inclusive) matchIndex else matchIndex + 1
        if (keepCount == 0) return root // Would make stack empty

        val newChildren = targetStack.children.take(keepCount)
        val newStack = targetStack.copy(children = newChildren)
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Pop to a screen with the given key.
     *
     * Convenience wrapper around [popTo] that matches by node key.
     *
     * @param root The root NavNode of the navigation tree
     * @param targetKey The key of the node to pop to
     * @param inclusive If true, also removes the matching screen
     * @return New tree with nodes popped, or original tree if key not found
     */
    fun popTo(
        root: NavNode,
        targetKey: String,
        inclusive: Boolean = false
    ): NavNode {
        return popTo(root, inclusive) { node ->
            node.key == targetKey
        }
    }

    /**
     * Pop to a screen with the given route.
     *
     * Convenience wrapper around [popTo] that matches by destination route.
     * Compares the route against [NavDestination.toString] or a route property
     * if available.
     *
     * @param root The root NavNode of the navigation tree
     * @param route The route string to match
     * @param inclusive If true, also removes the matching screen
     * @return New tree with nodes popped, or original tree if route not found
     */
    fun popToRoute(
        root: NavNode,
        route: String,
        inclusive: Boolean = false
    ): NavNode {
        return popTo(root, inclusive) { node ->
            node is ScreenNode && node.destination.toString() == route
        }
    }

    /**
     * Pop to a screen with destination matching the given type.
     *
     * @param root The root NavNode of the navigation tree
     * @param inclusive If true, also removes the matching screen
     * @return New tree with nodes popped, or original tree if not found
     */
    inline fun <reified D : NavDestination> popToDestination(
        root: NavNode,
        inclusive: Boolean = false
    ): NavNode {
        return popTo(root, inclusive) { node ->
            node is ScreenNode && node.destination is D
        }
    }

    /**
     * Handle pop when the active stack would become empty.
     */
    private fun handleEmptyStackPop(
        root: NavNode,
        emptyStack: StackNode,
        behavior: PopBehavior
    ): NavNode? {
        return when (behavior) {
            PopBehavior.PRESERVE_EMPTY -> {
                // Just update to empty stack
                val newStack = emptyStack.copy(children = emptyList())
                replaceNode(root, emptyStack.key, newStack)
            }

            PopBehavior.CASCADE -> {
                // If this is the root, cannot cascade further
                if (emptyStack.parentKey == null) return null

                // Try to remove this stack from its parent
                val parent = root.findByKey(emptyStack.parentKey)
                    ?: return null // Parent not found, cannot cascade

                when (parent) {
                    is TabNode -> {
                        // Cannot remove a tab's stack - that would break the tab structure
                        // Instead, just preserve the empty stack
                        val newStack = emptyStack.copy(children = emptyList())
                        replaceNode(root, emptyStack.key, newStack)
                    }

                    is StackNode -> {
                        // Remove the empty stack from parent's children
                        removeNode(root, emptyStack.key)
                    }

                    is PaneNode -> {
                        // Cannot remove a pane's content - preserve empty
                        val newStack = emptyStack.copy(children = emptyList())
                        replaceNode(root, emptyStack.key, newStack)
                    }

                    is ScreenNode -> {
                        // ScreenNode cannot be a parent - this shouldn't happen
                        null
                    }
                }
            }
        }
    }
}
