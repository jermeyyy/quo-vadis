package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

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
 * **What the Renderer (QuoVadisHost) determines:**
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

/**
 * Leaf node representing a single screen/destination.
 *
 * A ScreenNode is the terminal state in the navigation tree - it cannot
 * contain children. It holds a reference to the [Destination] that defines
 * the actual content to render.
 *
 * @property key Unique identifier for this screen instance
 * @property parentKey Key of the containing StackNode or PaneNode
 * @property destination The destination data (route, arguments, transitions)
 */
@Serializable
@SerialName("screen")
data class ScreenNode(
    override val key: String,
    override val parentKey: String?,
    val destination: Destination
) : NavNode

/**
 * Container node representing a linear navigation stack.
 *
 * A StackNode maintains an ordered list of child nodes where the last
 * element is the "active" (visible) child. Push operations append to
 * the list, pop operations remove from the tail.
 *
 * ## Behavior
 *
 * - **Push**: Appends new node to [children]
 * - **Pop**: Removes last node from [children]
 * - **Empty Stack**: May trigger cascading pop to parent (configurable)
 *
 * @property key Unique identifier for this stack
 * @property parentKey Key of the containing TabNode or PaneNode (null if root)
 * @property children Ordered list of child nodes (last = active)
 */
@Serializable
@SerialName("stack")
data class StackNode(
    override val key: String,
    override val parentKey: String?,
    val children: List<NavNode> = emptyList()
) : NavNode {

    /**
     * The currently active (visible) child node.
     * Returns null if the stack is empty.
     */
    val activeChild: NavNode?
        get() = children.lastOrNull()

    /**
     * Returns true if this stack has navigable history (more than one entry).
     */
    val canGoBack: Boolean
        get() = children.size > 1

    /**
     * Returns true if the stack is empty.
     */
    val isEmpty: Boolean
        get() = children.isEmpty()

    /**
     * Returns the number of entries in this stack.
     */
    val size: Int
        get() = children.size
}

/**
 * Container node representing tabbed navigation with parallel stacks.
 *
 * A TabNode maintains multiple [StackNode]s, one for each tab, along with
 * an [activeStackIndex] indicating which tab is currently selected. Each
 * tab preserves its own navigation history independently.
 *
 * ## Behavior
 *
 * - **SwitchTab**: Updates [activeStackIndex]
 * - **Push**: Affects only the active stack
 * - **Pop**: Removes from active stack; if empty, may switch tabs (configurable)
 *
 * @property key Unique identifier for this tab container
 * @property parentKey Key of the containing node (null if root)
 * @property stacks List of StackNodes, one per tab
 * @property activeStackIndex Index of the currently active tab (0-based)
 * @property wrapperKey Key used to lookup the wrapper in [WrapperRegistry].
 *   This is typically the simple name of the tab class (e.g., "MainTabs")
 *   and is used by the hierarchical renderer to find the correct wrapper.
 *   Defaults to null, which means no custom wrapper is registered.
 */
@Serializable
@SerialName("tab")
data class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,
    val wrapperKey: String? = null
) : NavNode {

    init {
        require(stacks.isNotEmpty()) { "TabNode must have at least one stack" }
        require(activeStackIndex in stacks.indices) {
            "activeStackIndex ($activeStackIndex) out of bounds for ${stacks.size} stacks"
        }
    }

    /**
     * The currently active stack.
     */
    val activeStack: StackNode
        get() = stacks[activeStackIndex]

    /**
     * Number of tabs in this container.
     */
    val tabCount: Int
        get() = stacks.size

    /**
     * Returns the stack at the given index.
     * @throws IndexOutOfBoundsException if index is invalid
     */
    fun stackAt(index: Int): StackNode = stacks[index]
}

/**
 * Defines the semantic role of a pane within an adaptive layout.
 * Roles determine adaptation strategies and navigation behavior.
 */
@Serializable
enum class PaneRole {
    /** The primary content pane (e.g., document editor, video player) */
    Primary,

    /** Supporting content that provides context to primary (e.g., comments, related items) */
    Supporting,

    /** Optional extra pane for supplementary content */
    Extra
}

/**
 * Strategy for adapting a pane when space is insufficient.
 */
@Serializable
enum class AdaptStrategy {
    /** Hide the pane completely */
    Hide,

    /** Show as a levitated overlay (modal/dialog-like) */
    Levitate,

    /** Reflow under another pane (vertical stacking) */
    Reflow
}

/**
 * Back navigation behavior within a PaneNode.
 */
@Serializable
enum class PaneBackBehavior {
    /** Back forces a change in which pane(s) are visible */
    PopUntilScaffoldValueChange,

    /** Back forces a change in which pane is "active" (focused) */
    PopUntilCurrentDestinationChange,

    /** Back forces a change in content of any pane */
    PopUntilContentChange,

    /** Simple pop from active pane's stack */
    PopLatest
}

/**
 * Configuration for a single pane within a PaneNode.
 *
 * @property content The navigation content within this pane (typically a StackNode)
 * @property adaptStrategy Adaptation strategy when this pane cannot be expanded
 */
@Serializable
data class PaneConfiguration(
    val content: NavNode,
    val adaptStrategy: AdaptStrategy = AdaptStrategy.Hide
)

/**
 * Container node for adaptive pane layouts.
 *
 * PaneNode represents layouts where multiple panes can be displayed
 * simultaneously on large screens or collapsed to single-pane on compact screens.
 *
 * ## Adaptive Behavior
 *
 * The NavNode tree represents LOGICAL navigation state, not visual layout.
 * Visual adaptation (which panes are visible, side-by-side vs stacked) is
 * determined by the renderer based on:
 * - WindowSizeClass (observed from platform)
 * - AdaptStrategy (stored per pane)
 * - Animation state (during transitions)
 *
 * - On **compact** screens: Only [activePaneRole] is visible
 * - On **medium** screens: Primary visible, others can levitate (overlay)
 * - On **expanded** screens: Multiple panes displayed side-by-side
 *
 * ## Use Cases
 *
 * - **Master-Detail**: List pane (Primary) + Detail pane (Supporting)
 * - **Supporting Pane**: Main content (Primary) + Context panel (Supporting)
 * - **Multi-Column**: Navigation rail + Content + Detail (all three roles)
 *
 * @property key Unique identifier for this pane container
 * @property parentKey Key of containing node (null if root)
 * @property paneConfigurations Map of pane roles to their configurations
 * @property activePaneRole The pane that currently has navigation focus
 * @property backBehavior How back navigation should behave in this container
 */
@Serializable
@SerialName("pane")
data class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole = PaneRole.Primary,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
) : NavNode {

    init {
        require(paneConfigurations.containsKey(PaneRole.Primary)) {
            "PaneNode must have at least a Primary pane"
        }
        require(paneConfigurations.containsKey(activePaneRole)) {
            "activePaneRole ($activePaneRole) must exist in paneConfigurations"
        }
    }

    /**
     * The content node for the given role, or null if role not configured.
     */
    fun paneContent(role: PaneRole): NavNode? = paneConfigurations[role]?.content

    /**
     * The adaptation strategy for the given role.
     */
    fun adaptStrategy(role: PaneRole): AdaptStrategy? = paneConfigurations[role]?.adaptStrategy

    /**
     * The actively focused pane's content.
     */
    val activePaneContent: NavNode?
        get() = paneContent(activePaneRole)

    /**
     * Number of configured panes.
     */
    val paneCount: Int
        get() = paneConfigurations.size

    /**
     * All configured pane roles.
     */
    val configuredRoles: Set<PaneRole>
        get() = paneConfigurations.keys
}

// =============================================================================
// Serialization Module
// =============================================================================

/**
 * Serializers module for NavNode polymorphic serialization.
 *
 * Register this module with your Json instance to enable serialization
 * of the NavNode hierarchy:
 *
 * ```kotlin
 * val json = Json {
 *     serializersModule = navNodeSerializersModule
 * }
 * ```
 */
val navNodeSerializersModule = SerializersModule {
    polymorphic(NavNode::class) {
        subclass(ScreenNode::class)
        subclass(StackNode::class)
        subclass(TabNode::class)
        subclass(PaneNode::class)
    }
}

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

// =============================================================================
// Key Generation Utility
// =============================================================================

/**
 * Utility object for generating unique navigation node keys.
 */
object NavKeyGenerator {
    private var counter = 0L

    /**
     * Generates a unique key with an optional debug label.
     *
     * @param debugLabel Optional label for debugging (e.g., "profile", "home")
     * @return A unique key string
     */
    fun generate(debugLabel: String? = null): String {
        val id = counter++
        return debugLabel?.let { "$it-$id" } ?: "node-$id"
    }

    /**
     * Resets the counter (useful for testing).
     */
    fun reset() {
        counter = 0L
    }
}
