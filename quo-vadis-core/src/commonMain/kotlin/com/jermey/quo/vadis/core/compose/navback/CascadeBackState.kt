package com.jermey.quo.vadis.core.compose.navback

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.PaneNode
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.navigation.TabNode
import com.jermey.quo.vadis.core.navigation.activeStack
import com.jermey.quo.vadis.core.navigation.findByKey

/**
 * State information for a predictive back gesture that may cascade.
 *
 * Calculated at gesture start to determine:
 * - What will be visually removed (screen, stack, or tab container)
 * - What will be revealed (the target screen after back)
 * - How many levels the cascade goes
 */
@Stable
data class CascadeBackState(
    /**
     * The node that initiated the back gesture (usually a ScreenNode).
     */
    val sourceNode: NavNode,

    /**
     * The node that will be visually removed by the back action.
     * Could be:
     * - ScreenNode: Normal pop, just the screen exits
     * - StackNode: Cascade pop, the entire stack container exits
     * - TabNode: Tab cascade, the entire tab wrapper exits
     */
    val exitingNode: NavNode,

    /**
     * The node that will be revealed after back completes.
     * This is what should be shown in the predictive back preview.
     * Null if delegating to system (e.g., closing app).
     */
    val targetNode: NavNode?,

    /**
     * The number of levels the cascade goes.
     * 0 = normal pop (no cascade)
     * 1 = pop to parent
     * 2+ = deeper cascade
     */
    val cascadeDepth: Int,

    /**
     * Whether this back action would delegate to the system (e.g., close app).
     */
    val delegatesToSystem: Boolean
)

/**
 * Calculates the cascade back state for the current navigation state.
 *
 * This function analyzes the navigation tree to determine what the back action
 * will do, including whether it will cascade up through multiple container levels.
 *
 * @param root The root of the navigation tree
 * @return CascadeBackState describing what the back action will do
 */
fun calculateCascadeBackState(root: NavNode): CascadeBackState {
    val activeStack = root.activeStack()
    val activeChild = activeStack?.activeChild

    if (activeStack == null || activeChild == null) {
        return CascadeBackState(
            sourceNode = root,
            exitingNode = root,
            targetNode = null,
            cascadeDepth = 0,
            delegatesToSystem = true
        )
    }

    // Normal pop case: stack has more than one child
    if (activeStack.children.size > 1) {
        val previousChild = activeStack.children[activeStack.children.size - 2]
        return CascadeBackState(
            sourceNode = activeChild,
            exitingNode = activeChild,
            targetNode = previousChild,
            cascadeDepth = 0,
            delegatesToSystem = false
        )
    }

    // Cascade case: stack has only 1 child, need to walk up the tree
    return calculateCascadeFromStack(root, activeStack, activeChild)
}

/**
 * Internal recursive function to calculate cascade state starting from a stack.
 */
private fun calculateCascadeFromStack(
    root: NavNode,
    stack: StackNode,
    sourceNode: NavNode,
    currentDepth: Int = 0
): CascadeBackState {
    val parentKey = stack.parentKey

    // Root stack with 1 child - delegate to system
    if (parentKey == null) {
        return CascadeBackState(
            sourceNode = sourceNode,
            exitingNode = stack.activeChild ?: stack,
            targetNode = null,
            cascadeDepth = currentDepth,
            delegatesToSystem = true
        )
    }

    val parent = root.findByKey(parentKey)
    val newDepth = currentDepth + 1

    return when (parent) {
        is StackNode -> {
            if (parent.children.size > 1) {
                // Parent can pop this stack - find the sibling that will be revealed
                val siblingIndex = parent.children.indexOfFirst { it.key == stack.key }
                val target = if (siblingIndex > 0) {
                    findActiveDescendant(parent.children[siblingIndex - 1])
                } else null

                CascadeBackState(
                    sourceNode = sourceNode,
                    exitingNode = stack,
                    targetNode = target,
                    cascadeDepth = newDepth,
                    delegatesToSystem = false
                )
            } else if (parent.parentKey == null) {
                // Parent is root with only 1 child - delegate to system
                CascadeBackState(
                    sourceNode = sourceNode,
                    exitingNode = stack,
                    targetNode = null,
                    cascadeDepth = newDepth,
                    delegatesToSystem = true
                )
            } else {
                // Continue cascade up through parent
                calculateCascadeFromStack(root, parent, sourceNode, newDepth)
            }
        }
        is TabNode -> {
            // TabNode with active stack having 1 child - pop entire TabNode
            // No tab switching - always cascade to pop TabNode
            calculateCascadeFromContainer(root, parent, sourceNode, newDepth)
        }
        is PaneNode -> {
            // Continue cascade through pane
            calculateCascadeFromContainer(root, parent, sourceNode, newDepth)
        }
        else -> {
            // Unknown parent type - delegate to system
            CascadeBackState(
                sourceNode = sourceNode,
                exitingNode = stack,
                targetNode = null,
                cascadeDepth = newDepth,
                delegatesToSystem = true
            )
        }
    }
}

/**
 * Continue cascade from a container (TabNode or PaneNode).
 */
private fun calculateCascadeFromContainer(
    root: NavNode,
    container: NavNode,
    sourceNode: NavNode,
    currentDepth: Int
): CascadeBackState {
    val containerParentKey = when (container) {
        is TabNode -> container.parentKey
        is PaneNode -> container.parentKey
        else -> null
    }

    if (containerParentKey == null) {
        return CascadeBackState(
            sourceNode = sourceNode,
            exitingNode = container,
            targetNode = null,
            cascadeDepth = currentDepth,
            delegatesToSystem = true
        )
    }

    val containerParent = root.findByKey(containerParentKey)
    val newDepth = currentDepth + 1

    return when (containerParent) {
        is StackNode -> {
            if (containerParent.children.size > 1) {
                val siblingIndex = containerParent.children.indexOfFirst { it.key == container.key }
                val target = if (siblingIndex > 0) {
                    findActiveDescendant(containerParent.children[siblingIndex - 1])
                } else null

                CascadeBackState(
                    sourceNode = sourceNode,
                    exitingNode = container,
                    targetNode = target,
                    cascadeDepth = newDepth,
                    delegatesToSystem = false
                )
            } else if (containerParent.parentKey == null) {
                CascadeBackState(
                    sourceNode = sourceNode,
                    exitingNode = container,
                    targetNode = null,
                    cascadeDepth = newDepth,
                    delegatesToSystem = true
                )
            } else {
                calculateCascadeFromStack(root, containerParent, sourceNode, newDepth)
            }
        }
        is TabNode -> {
            // TabNode - always cascade to pop the entire TabNode (no tab switching)
            calculateCascadeFromContainer(root, containerParent, sourceNode, newDepth)
        }
        else -> {
            CascadeBackState(
                sourceNode = sourceNode,
                exitingNode = container,
                targetNode = null,
                cascadeDepth = newDepth,
                delegatesToSystem = true
            )
        }
    }
}

/**
 * Finds the active descendant (deepest active child) of a node.
 */
private fun findActiveDescendant(node: NavNode): NavNode? {
    return when (node) {
        is ScreenNode -> node
        is StackNode -> node.activeChild?.let { findActiveDescendant(it) }
        is TabNode -> node.activeStack.activeChild?.let { findActiveDescendant(it) }
        is PaneNode -> node.activeStack()?.activeChild?.let { findActiveDescendant(it) }
    }
}

/**
 * Determines if back handling would result in a cascade pop.
 *
 * @return true if the back action would pop a container (stack/tab), not just a screen
 */
fun wouldCascade(root: NavNode): Boolean {
    val activeStack = root.activeStack() ?: return false
    return activeStack.children.size <= 1 && activeStack.parentKey != null
}
