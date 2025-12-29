package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlinx.serialization.Serializable

/**
 * Base sealed interface for all navigation tree nodes.
 *
 * The navigation state is represented as an immutable tree where:
 * - Each node has a unique [key] for identification
 * - Optional [parentKey] links to the parent node (null for root)
 * - The tree structure models the logical navigation hierarchy
 *
 * ## Design Principles
 *
 * 1. **Immutability**: All nodes are data classes, enabling structural sharing
 *    and efficient diff calculation for animations.
 *
 * 2. **Serialization**: Full kotlinx.serialization support for process death
 *    survival and state restoration.
 *
 * 3. **Type Safety**: Sealed hierarchy ensures exhaustive when expressions
 *    and compile-time verification of node handling.
 *
 * ## Key Design Principle: Separation of Navigation State and Visual Layout
 *
 * The NavNode tree represents **logical navigation state**, NOT visual layout state.
 *
 * **What NavNode stores:**
 * - Which destinations exist in the navigation hierarchy
 * - Which pane/stack/tab is "active" (has navigation focus)
 * - Adaptation strategies (how panes SHOULD adapt when space is limited)
 *
 * **What the Renderer (NavigationHost) determines:**
 * - Which panes are currently VISIBLE (based on WindowSizeClass)
 * - Layout arrangement (side-by-side, stacked, levitated)
 * - Animation states during transitions
 *
 * This separation is critical for adaptive morphing—when a device rotates from
 * portrait to landscape, the NAVIGATION STATE doesn't change (user is still
 * viewing the same content), only the VISUAL REPRESENTATION changes (panes go
 * from single-view to side-by-side).
 */
@Serializable
sealed interface NavNode {
    /**
     * Unique identifier for this node within the navigation tree.
     *
     * Recommended format: UUID or structured path (e.g., "root/stack0/screen1")
     * for debugging purposes.
     */
    val key: String

    /**
     * Key of the parent node, or null if this is the root node.
     */
    val parentKey: String?
}

// =============================================================================
// Serialization Module
// =============================================================================

// =============================================================================
// Extension Functions for Tree Traversal and Manipulation
// =============================================================================

/**
 * Recursively finds a node by its key.
 * @return The node with the given key, or null if not found
 */
fun NavNode.findByKey(key: String): NavNode? {
    if (this.key == key) return this

    return when (this) {
        is ScreenNode -> null
        is StackNode -> children.firstNotNullOfOrNull { it.findByKey(key) }
        is TabNode -> stacks.firstNotNullOfOrNull { it.findByKey(key) }
        is PaneNode -> paneConfigurations.values.firstNotNullOfOrNull { it.content.findByKey(key) }
    }
}

/**
 * Returns the depth-first active path from this node to the deepest active leaf.
 * @return List of nodes from this node to the active leaf (inclusive)
 */
fun NavNode.activePathToLeaf(): List<NavNode> {
    val path = mutableListOf<NavNode>()
    var current: NavNode? = this

    while (current != null) {
        path.add(current)
        current = when (current) {
            is ScreenNode -> null
            is StackNode -> current.activeChild
            is TabNode -> current.activeStack
            is PaneNode -> current.activePaneContent
        }
    }

    return path
}

/**
 * Returns the deepest active leaf node (ScreenNode) in this subtree.
 * @return The active ScreenNode, or null if no screen is active
 */
fun NavNode.activeLeaf(): ScreenNode? {
    return when (this) {
        is ScreenNode -> this
        is StackNode -> activeChild?.activeLeaf()
        is TabNode -> activeStack.activeLeaf()
        is PaneNode -> activePaneContent?.activeLeaf()
    }
}

/**
 * Returns the deepest active StackNode in this subtree.
 * This is the stack that will receive push/pop operations.
 */
fun NavNode.activeStack(): StackNode? {
    return when (this) {
        is ScreenNode -> null
        is StackNode -> {
            // Check if active child has a deeper stack
            activeChild?.activeStack() ?: this
        }

        is TabNode -> activeStack.activeStack() ?: activeStack
        is PaneNode -> activePaneContent?.activeStack()
    }
}

/**
 * Returns all ScreenNodes in this subtree.
 */
fun NavNode.allScreens(): List<ScreenNode> {
    return when (this) {
        is ScreenNode -> listOf(this)
        is StackNode -> children.flatMap { it.allScreens() }
        is TabNode -> stacks.flatMap { it.allScreens() }
        is PaneNode -> paneConfigurations.values.flatMap { it.content.allScreens() }
    }
}

/**
 * Returns the pane content for a specific role, searching recursively.
 * @return The NavNode content for the role, or null if not found
 */
fun NavNode.paneForRole(role: PaneRole): NavNode? {
    return when (this) {
        is ScreenNode -> null
        is StackNode -> children.firstNotNullOfOrNull { it.paneForRole(role) }
        is TabNode -> stacks.firstNotNullOfOrNull { it.paneForRole(role) }
        is PaneNode -> paneContent(role) ?: paneConfigurations.values
            .firstNotNullOfOrNull { it.content.paneForRole(role) }
    }
}

/**
 * Returns all PaneNodes in this subtree.
 */
fun NavNode.allPaneNodes(): List<PaneNode> {
    return when (this) {
        is ScreenNode -> emptyList()
        is StackNode -> children.flatMap { it.allPaneNodes() }
        is TabNode -> stacks.flatMap { it.allPaneNodes() }
        is PaneNode -> listOf(this) + paneConfigurations.values.flatMap { it.content.allPaneNodes() }
    }
}

/**
 * Returns all TabNodes in this subtree.
 */
fun NavNode.allTabNodes(): List<TabNode> {
    return when (this) {
        is ScreenNode -> emptyList()
        is StackNode -> children.flatMap { it.allTabNodes() }
        is TabNode -> listOf(this) + stacks.flatMap { it.allTabNodes() }
        is PaneNode -> paneConfigurations.values.flatMap { it.content.allTabNodes() }
    }
}

/**
 * Returns all StackNodes in this subtree.
 */
fun NavNode.allStackNodes(): List<StackNode> {
    return when (this) {
        is ScreenNode -> emptyList()
        is StackNode -> listOf(this) + children.flatMap { it.allStackNodes() }
        is TabNode -> stacks.flatMap { it.allStackNodes() }
        is PaneNode -> paneConfigurations.values.flatMap { it.content.allStackNodes() }
    }
}

/**
 * Calculates the depth of this node in a tree traversal.
 * ScreenNodes have depth 0 (leaf), containers have depth = max(child depths) + 1.
 */
fun NavNode.depth(): Int {
    return when (this) {
        is ScreenNode -> 0
        is StackNode -> if (children.isEmpty()) 0 else children.maxOf { it.depth() } + 1
        is TabNode -> stacks.maxOf { it.depth() } + 1
        is PaneNode -> paneConfigurations.values.maxOf { it.content.depth() } + 1
    }
}

/**
 * Returns the total count of all nodes in this subtree (including this node).
 */
fun NavNode.nodeCount(): Int {
    return when (this) {
        is ScreenNode -> 1
        is StackNode -> 1 + children.sumOf { it.nodeCount() }
        is TabNode -> 1 + stacks.sumOf { it.nodeCount() }
        is PaneNode -> 1 + paneConfigurations.values.sumOf { it.content.nodeCount() }
    }
}

/**
 * Determines if this node can handle back internally (has content to pop/switch).
 * For StackNode: true if size > 1
 * For TabNode: true if active stack can go back OR not on initial tab
 * For PaneNode: true if any pane has content to pop
 */
fun NavNode.canHandleBackInternally(): Boolean = when (this) {
    is ScreenNode -> false
    is StackNode -> canGoBack
    is TabNode -> activeStack.canGoBack || activeStackIndex != 0
    is PaneNode -> paneConfigurations.values.any {
        it.content.activeStack()?.canGoBack == true
    }
}

/**
 * Check if this is a root node (has no parent).
 */
fun NavNode.isRoot(): Boolean = parentKey == null

// =============================================================================
// Key Generation Utility
// =============================================================================
