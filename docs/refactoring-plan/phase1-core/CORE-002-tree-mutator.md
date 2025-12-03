# CORE-002: Implement TreeMutator Operations

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | CORE-002 |
| **Task Name** | Implement TreeMutator Operations |
| **Phase** | Phase 1: Core State Refactoring |
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | CORE-001 |
| **Blocked By** | CORE-001 |
| **Blocks** | CORE-003 |

---

## Overview

The `TreeMutator` is a pure functional utility that performs immutable tree transformations on the `NavNode` hierarchy. All operations take the current tree state and return a new tree, enabling:

- **Predictable state management** via pure functions
- **Time-travel debugging** by keeping references to previous states
- **Efficient rendering** through structural diff calculation
- **Thread safety** without locks or synchronization

### Mathematical Model

Navigation operations are functional reducers:

$$S_{new} = f(S_{old}, Action)$$

Where:
- $S_{old}$ is the current `NavNode` tree
- $Action$ is a navigation intent (push, pop, switchTab, etc.)
- $S_{new}$ is the resulting `NavNode` tree

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt
```

---

## Implementation

### Core TreeMutator Object

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Pure functional operations for manipulating the NavNode tree.
 * 
 * All operations are immutable - they return new tree instances rather than
 * modifying existing ones. This enables:
 * 
 * - **Structural sharing**: Unchanged subtrees are reused by reference
 * - **Predictable updates**: No side effects, easy to test
 * - **Transaction support**: Compare old/new states for animations
 * 
 * ## Usage
 * 
 * ```kotlin
 * val navigator = Navigator()
 * val newState = TreeMutator.push(navigator.state.value, destination)
 * navigator.updateState(newState)
 * ```
 */
object TreeMutator {

    /**
     * Configures behavior when a StackNode becomes empty after pop.
     */
    enum class PopBehavior {
        /** Remove the empty stack from parent (cascading pop) */
        CASCADE,
        /** Keep the empty stack in place */
        PRESERVE_EMPTY
    }

    // =========================================================================
    // PUSH OPERATIONS
    // =========================================================================

    /**
     * Push a destination onto the deepest active stack.
     * 
     * Algorithm:
     * 1. Traverse depth-first following the active path
     * 2. Find the deepest StackNode that is active
     * 3. Append a new ScreenNode to that stack's children
     * 4. Rebuild the tree with the updated stack
     * 
     * @param root The current tree root
     * @param destination The destination to push
     * @param generateKey Optional key generator (defaults to UUID)
     * @return New tree with the destination pushed
     * @throws IllegalStateException if no active stack is found
     */
    @OptIn(ExperimentalUuidApi::class)
    fun push(
        root: NavNode,
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
    ): NavNode {
        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found in tree")
        
        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )
        
        val newStack = targetStack.copy(
            children = targetStack.children + newScreen
        )
        
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Push a destination onto a specific stack identified by key.
     * 
     * @param root The current tree root
     * @param stackKey Key of the target stack
     * @param destination The destination to push
     * @param generateKey Optional key generator
     * @return New tree with the destination pushed
     * @throws IllegalArgumentException if stackKey doesn't reference a StackNode
     */
    @OptIn(ExperimentalUuidApi::class)
    fun pushToStack(
        root: NavNode,
        stackKey: String,
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
    ): NavNode {
        val targetNode = root.findByKey(stackKey)
            ?: throw IllegalArgumentException("No node found with key: $stackKey")
        
        val targetStack = targetNode as? StackNode
            ?: throw IllegalArgumentException("Node $stackKey is not a StackNode")
        
        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = stackKey,
            destination = destination
        )
        
        val newStack = targetStack.copy(
            children = targetStack.children + newScreen
        )
        
        return replaceNode(root, stackKey, newStack)
    }

    // =========================================================================
    // POP OPERATIONS
    // =========================================================================

    /**
     * Pop the active screen from the deepest active stack.
     * 
     * Algorithm:
     * 1. Find the deepest active StackNode
     * 2. Remove the last child from that stack
     * 3. If stack becomes empty and CASCADE behavior, remove the stack
     * 4. Rebuild the tree with updates
     * 
     * @param root The current tree root
     * @param behavior How to handle empty stacks after pop
     * @return New tree with the screen popped, or null if cannot pop (at root)
     */
    fun pop(
        root: NavNode,
        behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY
    ): NavNode? {
        val targetStack = root.activeStack() ?: return null
        
        // Cannot pop if stack is empty or has only one item (at root level)
        if (targetStack.children.size <= 1) {
            return handleEmptyStackPop(root, targetStack, behavior)
        }
        
        val newStack = targetStack.copy(
            children = targetStack.children.dropLast(1)
        )
        
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Pop all screens from the active stack until the predicate matches.
     * 
     * The matching screen is NOT removed (non-inclusive by default).
     * 
     * @param root The current tree root
     * @param inclusive If true, also remove the matching screen
     * @param predicate Function to identify the target screen
     * @return New tree with screens popped, or original if no match
     */
    fun popTo(
        root: NavNode,
        inclusive: Boolean = false,
        predicate: (NavNode) -> Boolean
    ): NavNode {
        val targetStack = root.activeStack() ?: return root
        
        val targetIndex = targetStack.children.indexOfLast { predicate(it) }
        if (targetIndex == -1) return root
        
        val endIndex = if (inclusive) targetIndex else targetIndex + 1
        val newChildren = targetStack.children.take(endIndex)
        
        if (newChildren.isEmpty()) {
            return root // Don't create empty stack
        }
        
        val newStack = targetStack.copy(children = newChildren)
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Pop to a screen with the given route.
     * 
     * @param root The current tree root
     * @param route The route to pop to
     * @param inclusive If true, also remove the matching screen
     * @return New tree with screens popped
     */
    fun popToRoute(
        root: NavNode,
        route: String,
        inclusive: Boolean = false
    ): NavNode {
        return popTo(root, inclusive) { node ->
            node is ScreenNode && node.destination.route == route
        }
    }

    /**
     * Handle pop when the active stack would become empty.
     */
    private fun handleEmptyStackPop(
        root: NavNode,
        stack: StackNode,
        behavior: PopBehavior
    ): NavNode? {
        return when (behavior) {
            PopBehavior.CASCADE -> {
                // Try to pop at parent level
                val parentKey = stack.parentKey ?: return null
                val parent = root.findByKey(parentKey) ?: return null
                
                when (parent) {
                    is TabNode -> {
                        // Cannot cascade from tab - would need to remove entire tab
                        null
                    }
                    is StackNode -> {
                        // Pop the entire child stack from parent
                        if (parent.children.size <= 1) return null
                        val newParent = parent.copy(
                            children = parent.children.dropLast(1)
                        )
                        replaceNode(root, parentKey, newParent)
                    }
                    is PaneNode -> {
                        // Cannot cascade from pane - panes are fixed
                        null
                    }
                    is ScreenNode -> null
                }
            }
            PopBehavior.PRESERVE_EMPTY -> null
        }
    }

    // =========================================================================
    // TAB OPERATIONS
    // =========================================================================

    /**
     * Switch to a different tab in a TabNode.
     * 
     * @param root The current tree root
     * @param tabNodeKey Key of the TabNode to modify
     * @param newIndex The new active tab index
     * @return New tree with the tab switched
     * @throws IllegalArgumentException if key doesn't reference a TabNode
     * @throws IndexOutOfBoundsException if newIndex is invalid
     */
    fun switchTab(
        root: NavNode,
        tabNodeKey: String,
        newIndex: Int
    ): NavNode {
        val tabNode = root.findByKey(tabNodeKey) as? TabNode
            ?: throw IllegalArgumentException("No TabNode found with key: $tabNodeKey")
        
        require(newIndex in tabNode.stacks.indices) {
            "Tab index $newIndex out of bounds for ${tabNode.stacks.size} tabs"
        }
        
        if (tabNode.activeStackIndex == newIndex) {
            return root // No change needed
        }
        
        val newTabNode = tabNode.copy(activeStackIndex = newIndex)
        return replaceNode(root, tabNodeKey, newTabNode)
    }

    /**
     * Switch to a different tab in the first TabNode found in the active path.
     * 
     * @param root The current tree root
     * @param newIndex The new active tab index
     * @return New tree with the tab switched
     * @throws IllegalStateException if no TabNode in active path
     */
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode {
        val tabNode = findActiveTabNode(root)
            ?: throw IllegalStateException("No TabNode found in active path")
        
        return switchTab(root, tabNode.key, newIndex)
    }

    /**
     * Find the TabNode in the active path, if any.
     */
    private fun findActiveTabNode(node: NavNode): TabNode? {
        return when (node) {
            is ScreenNode -> null
            is StackNode -> node.activeChild?.let { findActiveTabNode(it) }
            is TabNode -> node
            is PaneNode -> findActiveTabNode(node.activePane)
        }
    }

    // =========================================================================
    // PANE OPERATIONS
    // =========================================================================

    /**
     * Replace a pane in a PaneNode with a new node.
     * 
     * @param root The current tree root
     * @param paneNodeKey Key of the PaneNode to modify
     * @param paneIndex Index of the pane to replace
     * @param newNode The new node to place in that pane
     * @return New tree with the pane replaced
     * @throws IllegalArgumentException if key doesn't reference a PaneNode
     * @throws IndexOutOfBoundsException if paneIndex is invalid
     */
    fun replacePane(
        root: NavNode,
        paneNodeKey: String,
        paneIndex: Int,
        newNode: NavNode
    ): NavNode {
        val paneNode = root.findByKey(paneNodeKey) as? PaneNode
            ?: throw IllegalArgumentException("No PaneNode found with key: $paneNodeKey")
        
        require(paneIndex in paneNode.panes.indices) {
            "Pane index $paneIndex out of bounds for ${paneNode.panes.size} panes"
        }
        
        val newPanes = paneNode.panes.toMutableList()
        newPanes[paneIndex] = newNode
        
        val newPaneNode = paneNode.copy(panes = newPanes)
        return replaceNode(root, paneNodeKey, newPaneNode)
    }

    /**
     * Set the active pane index in a PaneNode.
     * 
     * @param root The current tree root
     * @param paneNodeKey Key of the PaneNode to modify
     * @param newIndex The new active pane index
     * @return New tree with the active pane changed
     */
    fun setActivePane(
        root: NavNode,
        paneNodeKey: String,
        newIndex: Int
    ): NavNode {
        val paneNode = root.findByKey(paneNodeKey) as? PaneNode
            ?: throw IllegalArgumentException("No PaneNode found with key: $paneNodeKey")
        
        require(newIndex in paneNode.panes.indices) {
            "Pane index $newIndex out of bounds for ${paneNode.panes.size} panes"
        }
        
        val newPaneNode = paneNode.copy(activePaneIndex = newIndex)
        return replaceNode(root, paneNodeKey, newPaneNode)
    }

    // =========================================================================
    // TREE MANIPULATION UTILITIES
    // =========================================================================

    /**
     * Replace a node in the tree by its key.
     * 
     * This is the core tree transformation function. It performs a depth-first
     * traversal, rebuilding only the path to the target node.
     * 
     * @param root The current tree root
     * @param targetKey Key of the node to replace
     * @param newNode The replacement node
     * @return New tree with the replacement applied
     */
    fun replaceNode(root: NavNode, targetKey: String, newNode: NavNode): NavNode {
        if (root.key == targetKey) {
            return newNode
        }
        
        return when (root) {
            is ScreenNode -> root // Leaf node, not found
            
            is StackNode -> {
                val newChildren = root.children.map { child ->
                    if (containsKey(child, targetKey)) {
                        replaceNode(child, targetKey, newNode)
                    } else {
                        child
                    }
                }
                if (newChildren == root.children) root else root.copy(children = newChildren)
            }
            
            is TabNode -> {
                val newStacks = root.stacks.map { stack ->
                    if (containsKey(stack, targetKey)) {
                        replaceNode(stack, targetKey, newNode) as StackNode
                    } else {
                        stack
                    }
                }
                if (newStacks == root.stacks) root else root.copy(stacks = newStacks)
            }
            
            is PaneNode -> {
                val newPanes = root.panes.map { pane ->
                    if (containsKey(pane, targetKey)) {
                        replaceNode(pane, targetKey, newNode)
                    } else {
                        pane
                    }
                }
                if (newPanes == root.panes) root else root.copy(panes = newPanes)
            }
        }
    }

    /**
     * Remove a node from the tree by its key.
     * 
     * @param root The current tree root
     * @param targetKey Key of the node to remove
     * @return New tree with the node removed, or null if root was removed
     */
    fun removeNode(root: NavNode, targetKey: String): NavNode? {
        if (root.key == targetKey) {
            return null
        }
        
        return when (root) {
            is ScreenNode -> root
            
            is StackNode -> {
                val newChildren = root.children.mapNotNull { child ->
                    if (child.key == targetKey) null
                    else if (containsKey(child, targetKey)) removeNode(child, targetKey)
                    else child
                }
                root.copy(children = newChildren)
            }
            
            is TabNode -> {
                val newStacks = root.stacks.mapNotNull { stack ->
                    if (stack.key == targetKey) null
                    else if (containsKey(stack, targetKey)) removeNode(stack, targetKey) as? StackNode
                    else stack
                }
                if (newStacks.isEmpty()) null
                else root.copy(
                    stacks = newStacks,
                    activeStackIndex = minOf(root.activeStackIndex, newStacks.lastIndex)
                )
            }
            
            is PaneNode -> {
                val newPanes = root.panes.mapNotNull { pane ->
                    if (pane.key == targetKey) null
                    else if (containsKey(pane, targetKey)) removeNode(pane, targetKey)
                    else pane
                }
                if (newPanes.isEmpty()) null
                else root.copy(
                    panes = newPanes,
                    activePaneIndex = minOf(root.activePaneIndex, newPanes.lastIndex)
                )
            }
        }
    }

    /**
     * Check if a subtree contains a node with the given key.
     */
    private fun containsKey(node: NavNode, key: String): Boolean {
        if (node.key == key) return true
        
        return when (node) {
            is ScreenNode -> false
            is StackNode -> node.children.any { containsKey(it, key) }
            is TabNode -> node.stacks.any { containsKey(it, key) }
            is PaneNode -> node.panes.any { containsKey(it, key) }
        }
    }

    // =========================================================================
    // ADVANCED OPERATIONS
    // =========================================================================

    /**
     * Clear all children from the active stack and push a single destination.
     * 
     * @param root The current tree root
     * @param destination The destination to set as the only screen
     * @param generateKey Optional key generator
     * @return New tree with the stack cleared and new screen added
     */
    @OptIn(ExperimentalUuidApi::class)
    fun clearAndPush(
        root: NavNode,
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
    ): NavNode {
        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found")
        
        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )
        
        val newStack = targetStack.copy(children = listOf(newScreen))
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Replace the current screen with a new destination.
     * 
     * @param root The current tree root
     * @param destination The replacement destination
     * @param generateKey Optional key generator
     * @return New tree with the current screen replaced
     */
    @OptIn(ExperimentalUuidApi::class)
    fun replaceCurrent(
        root: NavNode,
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
    ): NavNode {
        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found")
        
        if (targetStack.children.isEmpty()) {
            return push(root, destination, generateKey)
        }
        
        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )
        
        val newStack = targetStack.copy(
            children = targetStack.children.dropLast(1) + newScreen
        )
        return replaceNode(root, targetStack.key, newStack)
    }
}
```

---

## Algorithm Details

### Depth-First Active Path Traversal

The core traversal pattern follows the "active" path through the tree:

```
StackNode (root)
├── ScreenNode (home)
├── TabNode (main)
│   ├── StackNode [0] (feed tab)
│   │   ├── ScreenNode (feed-list)
│   │   └── ScreenNode (feed-detail)  <-- ACTIVE (if tab 0 selected)
│   └── StackNode [1] (profile tab)
│       └── ScreenNode (profile)
└── ScreenNode (settings) <-- would be ACTIVE if pushed after TabNode
```

When `activeStackIndex = 0`, the active path is:
`root → TabNode → StackNode[0] → ScreenNode(feed-detail)`

The deepest active `StackNode` is `StackNode[0]`, so push/pop operates there.

### Structural Sharing

```kotlin
// Original tree
val old = StackNode(
    key = "root",
    parentKey = null,
    children = listOf(screen1, screen2, screen3)
)

// After push - screen1, screen2, screen3 are reused by reference
val new = TreeMutator.push(old, newDestination)
// new.children = [screen1, screen2, screen3, newScreen]
//                 ^        ^        ^
//                 |        |        └── same reference
//                 |        └── same reference
//                 └── same reference
```

---

## Edge Cases

### 1. Empty Stack

```kotlin
val emptyStack = StackNode("root", null, emptyList())
val result = TreeMutator.pop(emptyStack)
// result = null (cannot pop)
```

### 2. Single-Item Stack at Root

```kotlin
val stack = StackNode("root", null, listOf(screen1))
val result = TreeMutator.pop(stack)
// result = null (would leave empty root)
```

### 3. Invalid Tab Index

```kotlin
val tabs = TabNode("tabs", null, listOf(stack1, stack2), activeStackIndex = 0)
TreeMutator.switchTab(root, "tabs", 5)
// throws IndexOutOfBoundsException
```

### 4. Pop with Cascade

```kotlin
// Tab 0 has empty stack, Tab 1 has content
val tabs = TabNode("tabs", null, listOf(emptyStack, stack1), activeStackIndex = 0)
val result = TreeMutator.pop(root, PopBehavior.CASCADE)
// result = null (cannot cascade out of tabs)
```

### 5. Node Not Found

```kotlin
TreeMutator.switchTab(root, "nonexistent", 0)
// throws IllegalArgumentException
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../core/TreeMutator.kt` | Create | New file with all operations |
| `quo-vadis-core/.../core/NavNode.kt` | Modify | Add extension functions if not in CORE-001 |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| CORE-001 (NavNode Hierarchy) | Hard | Must complete first |

---

## Acceptance Criteria

- [ ] `TreeMutator.push()` correctly appends to the deepest active stack
- [ ] `TreeMutator.pop()` removes from active stack with configurable empty behavior
- [ ] `TreeMutator.popTo()` correctly slices stack to target predicate
- [ ] `TreeMutator.popToRoute()` finds screen by route
- [ ] `TreeMutator.switchTab()` updates `activeStackIndex` correctly
- [ ] `TreeMutator.switchActiveTab()` finds TabNode in active path
- [ ] `TreeMutator.replacePane()` correctly replaces pane at index
- [ ] `TreeMutator.setActivePane()` updates `activePaneIndex` correctly
- [ ] `TreeMutator.replaceNode()` rebuilds tree with minimal allocations
- [ ] `TreeMutator.removeNode()` handles all node types
- [ ] `TreeMutator.clearAndPush()` clears stack and adds single screen
- [ ] `TreeMutator.replaceCurrent()` replaces active screen
- [ ] All operations are pure functions (no side effects)
- [ ] Structural sharing is verified (unchanged subtrees reused)
- [ ] Edge cases documented and tested (see CORE-006)
- [ ] Comprehensive KDoc on all public functions
- [ ] Thread-safe (no mutable state)

---

## Testing Notes

See [CORE-006](./CORE-006-unit-tests.md) for comprehensive test requirements.

Key test scenarios:

```kotlin
@Test
fun `push adds screen to deepest active stack`() {
    val root = StackNode("root", null, listOf(
        TabNode("tabs", "root", listOf(
            StackNode("tab0", "tabs", listOf(screen1)),
            StackNode("tab1", "tabs", listOf(screen2))
        ), activeStackIndex = 0)
    ))
    
    val result = TreeMutator.push(root, newDestination)
    
    // New screen should be in tab0's stack
    val tab0 = (result.children[0] as TabNode).stacks[0]
    assertEquals(2, tab0.children.size)
    assertEquals(newDestination, (tab0.activeChild as ScreenNode).destination)
}

@Test
fun `pop with cascade removes from parent when stack empty`() {
    // ... test cascading behavior
}

@Test
fun `switchTab preserves history in other tabs`() {
    // ... verify tab state preservation
}
```

---

## References

- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.1.2 "The Reducer Logic"
- [INDEX](../INDEX.md) - Phase 1 Overview
- [CORE-001](./CORE-001-navnode-hierarchy.md) - NavNode definitions
