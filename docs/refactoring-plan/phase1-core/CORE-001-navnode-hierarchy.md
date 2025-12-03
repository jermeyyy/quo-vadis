# CORE-001: Define NavNode Sealed Hierarchy

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | CORE-001 |
| **Task Name** | Define NavNode Sealed Hierarchy |
| **Phase** | Phase 1: Core State Refactoring |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | CORE-002, CORE-003, CORE-004, CORE-005 |

---

## Overview

This task establishes the foundation of the new navigation architecture by defining the `NavNode` sealed hierarchy. The hierarchy replaces the linear `List<Destination>` backstack with a recursive tree structure capable of modeling:

- **Linear stacks** (standard push/pop navigation)
- **Tabbed navigators** (parallel stacks with active tab tracking)
- **Adaptive panes** (simultaneous display of multiple nodes)

### Node Taxonomy

| Node Type | Description | Navigation Behavior | Theoretical Equivalent |
|-----------|-------------|---------------------|------------------------|
| **ScreenNode** | Leaf node representing a specific destination | Terminal state | Vertex |
| **StackNode** | Linear history of nodes (index N covers index N-1) | Push appends, Pop removes tail | Directed Path |
| **TabNode** | Collection of parallel StackNodes with active index | SwitchTab changes index | Disjoint Union |
| **PaneNode** | Role-based collection of panes with adaptive layout support | Active role tracks focus; visibility determined by renderer | Cartesian Product with Role Mapping |

This recursive structure allows infinite nesting (e.g., a Stack inside a Pane inside a Tab inside a Stack).

### Key Design Principle: Separation of Navigation State and Visual Layout

The NavNode tree represents **logical navigation state**, NOT visual layout state.

**What NavNode stores:**
- Which destinations exist in the navigation hierarchy
- Which pane/stack/tab is "active" (has navigation focus)
- Adaptation strategies (how panes SHOULD adapt when space is limited)

**What the Renderer (QuoVadisHost) determines:**
- Which panes are currently VISIBLE (based on WindowSizeClass)
- Layout arrangement (side-by-side, stacked, levitated)
- Animation states during transitions

This separation is critical for adaptive morphing—when a device rotates from portrait to landscape, the NAVIGATION STATE doesn't change (user is still viewing the same content), only the VISUAL REPRESENTATION changes (panes go from single-view to side-by-side).

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavNode.kt
```

---

## Implementation

### Core Data Structures

```kotlin
package com.jermey.quo.vadis.core.navigation.core

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
 */
@Serializable
data class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0
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
 */
@Serializable
data class PaneConfiguration(
    /** The navigation content within this pane (typically a StackNode) */
    val content: NavNode,
    /** Adaptation strategy when this pane cannot be expanded */
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
```

### Serialization Module

```kotlin
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
```

### Extension Functions

```kotlin
/**
 * Extension functions for NavNode tree traversal and manipulation.
 */

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
```

---

## Implementation Considerations

### 1. Immutability

All node types are Kotlin `data class` implementations, ensuring:

- **Structural sharing**: Unchanged branches of the tree are reused when creating new states
- **Thread safety**: Immutable objects can be safely shared across coroutines
- **Efficient diffing**: Reference equality checks identify unchanged subtrees

```kotlin
// Example: Pushing a new screen creates minimal new objects
val oldState: StackNode = /* ... */
val newState = oldState.copy(
    children = oldState.children + newScreen
)
// oldState.children[0..n-1] are reused by reference
```

### 2. Key Generation Strategy

**Recommended approach**: UUID with optional debug labels

```kotlin
object NavKeyGenerator {
    fun generate(debugLabel: String? = null): String {
        val uuid = UUID.randomUUID().toString().take(8)
        return debugLabel?.let { "$it-$uuid" } ?: uuid
    }
}

// Usage
val screenKey = NavKeyGenerator.generate("profile") // "profile-a1b2c3d4"
```

**Alternative**: Structured paths for debugging

```kotlin
val screenKey = "root/tabs/home/screen-3"
```

### 3. Serialization Considerations

- `Destination` must also be `@Serializable`
- Use `@SerialName` for stable serialization across versions
- Consider versioning for migration support

```kotlin
@Serializable
@SerialName("screen_v1")
data class ScreenNode(/* ... */)
```

### 4. Validation

Nodes validate their invariants in `init` blocks:

- `TabNode` requires at least one stack
- `PaneNode` requires at least one pane
- Index values must be within bounds

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../core/NavNode.kt` | Create | New file with sealed hierarchy |
| `quo-vadis-core/build.gradle.kts` | Modify | Ensure kotlinx.serialization dependency |

---

## Dependencies

This task has **no dependencies** and can be started immediately.

---

## Acceptance Criteria

- [ ] `NavNode` sealed interface defined with `key` and `parentKey` properties
- [ ] `ScreenNode` data class holding `Destination` reference
- [ ] `StackNode` data class with `children: List<NavNode>` and `activeChild` property
- [ ] `TabNode` data class with `stacks: List<StackNode>` and `activeStackIndex`
- [ ] `PaneNode` data class with `paneConfigurations: Map<PaneRole, PaneConfiguration>` and `activePaneRole`
- [ ] `PaneRole` enum defined with Primary, Supporting, Extra values
- [ ] `AdaptStrategy` enum defined with Hide, Levitate, Reflow values
- [ ] `PaneBackBehavior` enum defined with 4 back navigation strategies
- [ ] `PaneConfiguration` data class with content node and adapt strategy
- [ ] `PaneNode` uses `Map<PaneRole, PaneConfiguration>` instead of `List<NavNode>`
- [ ] KDoc explains that visual adaptation is renderer responsibility, not state
- [ ] Extension function `paneForRole(role: PaneRole)` for role-based traversal
- [ ] Extension function `allPaneNodes()` for collecting all pane containers
- [ ] All node types annotated with `@Serializable`
- [ ] Serializers module defined for polymorphic serialization
- [ ] Extension functions for tree traversal (`findByKey`, `activeLeaf`, etc.)
- [ ] Validation in `init` blocks for container nodes
- [ ] Unit tests for node creation and validation (see CORE-006)
- [ ] KDoc documentation on all public APIs
- [ ] Code compiles on all target platforms (Android, iOS, Desktop, Web)

---

## Testing Notes

See [CORE-006](./CORE-006-unit-tests.md) for comprehensive test requirements.

Basic tests to verify during development:

```kotlin
@Test
fun `ScreenNode holds destination`() {
    val dest = BasicDestination("profile")
    val node = ScreenNode(key = "s1", parentKey = "stack1", destination = dest)
    assertEquals(dest, node.destination)
}

@Test
fun `StackNode activeChild returns last element`() {
    val screen1 = ScreenNode("s1", "stack1", BasicDestination("home"))
    val screen2 = ScreenNode("s2", "stack1", BasicDestination("profile"))
    val stack = StackNode("stack1", null, listOf(screen1, screen2))
    
    assertEquals(screen2, stack.activeChild)
}

@Test
fun `TabNode requires valid activeStackIndex`() {
    val stack = StackNode("s1", "tabs", emptyList())
    
    assertThrows<IllegalArgumentException> {
        TabNode("tabs", null, listOf(stack), activeStackIndex = 5)
    }
}

@Test
fun `PaneNode requires Primary pane`() {
    assertThrows<IllegalArgumentException> {
        PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Supporting to PaneConfiguration(
                    content = ScreenNode("s1", "panes", BasicDestination("detail"))
                )
            )
        )
    }
}

@Test
fun `PaneNode activePaneRole must exist in configurations`() {
    assertThrows<IllegalArgumentException> {
        PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = ScreenNode("s1", "panes", BasicDestination("list"))
                )
            ),
            activePaneRole = PaneRole.Supporting // Not configured!
        )
    }
}

@Test
fun `PaneNode paneContent returns correct content by role`() {
    val listScreen = ScreenNode("s1", "panes", BasicDestination("list"))
    val detailScreen = ScreenNode("s2", "panes", BasicDestination("detail"))
    
    val paneNode = PaneNode(
        key = "panes",
        parentKey = null,
        paneConfigurations = mapOf(
            PaneRole.Primary to PaneConfiguration(content = listScreen),
            PaneRole.Supporting to PaneConfiguration(
                content = detailScreen,
                adaptStrategy = AdaptStrategy.Levitate
            )
        )
    )
    
    assertEquals(listScreen, paneNode.paneContent(PaneRole.Primary))
    assertEquals(detailScreen, paneNode.paneContent(PaneRole.Supporting))
    assertNull(paneNode.paneContent(PaneRole.Extra))
}
```

---

## References

- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.1.1 "Defining the Node Hierarchy"
- [INDEX](../INDEX.md) - Phase 1 Overview
- [Current Destination Interface](../../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Destination.kt)
