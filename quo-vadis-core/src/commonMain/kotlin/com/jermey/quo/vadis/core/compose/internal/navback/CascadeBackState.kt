package com.jermey.quo.vadis.core.compose.internal.navback

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

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
     * The key of the stack that should handle the predictive back animation.
     * For normal pops (cascadeDepth == 0), this is the parent stack of the exiting node.
     * For cascade pops, this is the root stack.
     */
    val animatingStackKey: NodeKey?,

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
 * @param isCompact Whether in compact mode. In expanded mode, PaneNodes are popped entirely.
 * @return CascadeBackState describing what the back action will do
 */
fun calculateCascadeBackState(root: NavNode, isCompact: Boolean = true): CascadeBackState {
    val activeStack = root.activeStack()
    val activeChild = activeStack?.activeChild

    if (activeStack == null || activeChild == null) {
        return CascadeBackState(
            sourceNode = root,
            exitingNode = root,
            targetNode = null,
            animatingStackKey = null,
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
            animatingStackKey = activeStack.key,
            cascadeDepth = 0,
            delegatesToSystem = false
        )
    }

    // Cascade case: stack has only 1 child, need to walk up the tree
    return calculateCascadeFromStack(root, activeStack, activeChild, isCompact = isCompact)
}

/**
 * Internal recursive function to calculate cascade state starting from a stack.
 */
private fun calculateCascadeFromStack(
    root: NavNode,
    stack: StackNode,
    sourceNode: NavNode,
    currentDepth: Int = 0,
    isCompact: Boolean = true
): CascadeBackState {
    val parentKey = stack.parentKey

    // Root stack with 1 child - delegate to system
    if (parentKey == null) {
        return CascadeBackState(
            sourceNode = sourceNode,
            exitingNode = stack.activeChild ?: stack,
            targetNode = null,
            animatingStackKey = null,
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
                    animatingStackKey = parent.key,
                    cascadeDepth = newDepth,
                    delegatesToSystem = false
                )
            } else if (parent.parentKey == null) {
                // Parent is root with only 1 child - delegate to system
                CascadeBackState(
                    sourceNode = sourceNode,
                    exitingNode = stack,
                    targetNode = null,
                    animatingStackKey = null,
                    cascadeDepth = newDepth,
                    delegatesToSystem = true
                )
            } else {
                // Continue cascade up through parent
                calculateCascadeFromStack(root, parent, sourceNode, newDepth, isCompact)
            }
        }
        is TabNode -> {
            // TabNode with active stack having 1 child - pop entire TabNode
            // No tab switching - always cascade to pop TabNode
            calculateCascadeFromContainer(root, parent, sourceNode, newDepth)
        }
        is PaneNode -> {
            if (isCompact) {
                // Compact mode: Check if we can switch to PRIMARY pane
                val activePaneRole = parent.activePaneRole
                if (activePaneRole != PaneRole.Primary) {
                    // Non-primary pane - back should switch to PRIMARY pane
                    // Enable predictive back: show PRIMARY pane's StackNode behind SECONDARY
                    // Use the StackNode directly (not leaf screen) to match what SinglePaneRenderer passes
                    val primaryPaneContent = parent.paneContent(PaneRole.Primary)
                    
                    CascadeBackState(
                        sourceNode = sourceNode,
                        exitingNode = sourceNode,
                        // Use the StackNode, not the leaf - matches AnimatedNavContent's targetState type
                        targetNode = primaryPaneContent,
                        // Use the pane node's key as animating key so pane-level animation handles it
                        animatingStackKey = parent.key,
                        cascadeDepth = 0,
                        delegatesToSystem = false
                    )
                } else {
                    // Already on PRIMARY pane with 1 child - cascade to pop PaneNode
                    calculateCascadeFromContainer(root, parent, sourceNode, newDepth)
                }
            } else {
                // Expanded mode: Always pop the entire PaneNode
                calculateCascadeFromContainer(root, parent, sourceNode, newDepth)
            }
        }
        else -> {
            // Unknown parent type - delegate to system
            CascadeBackState(
                sourceNode = sourceNode,
                exitingNode = stack,
                targetNode = null,
                animatingStackKey = null,
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
            animatingStackKey = null,
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
                    animatingStackKey = containerParent.key,
                    cascadeDepth = newDepth,
                    delegatesToSystem = false
                )
            } else if (containerParent.parentKey == null) {
                CascadeBackState(
                    sourceNode = sourceNode,
                    exitingNode = container,
                    targetNode = null,
                    animatingStackKey = null,
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
                animatingStackKey = null,
                cascadeDepth = newDepth,
                delegatesToSystem = true
            )
        }
    }
}

/**
 * Finds the node that should be shown as the back target.
 * 
 * For container nodes (TabNode, PaneNode), returns the container itself
 * since we want to show the entire container during predictive back.
 * For StackNodes, returns the active child recursively.
 * For ScreenNodes, returns the screen itself.
 */
private fun findActiveDescendant(node: NavNode): NavNode? {
    return when (node) {
        is ScreenNode -> node
        is StackNode -> node.activeChild?.let { findActiveDescendant(it) }
        // For TabNode and PaneNode, return the container itself, not the leaf screen
        // This ensures predictive back shows the full tab wrapper, not just the screen content
        is TabNode -> node
        is PaneNode -> node
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
