# Stack Cascade Back Handling Refactoring Plan

## Overview

This document outlines the plan to fix back handling for stack nodes that should not allow removing their last screen. Instead, when back is requested on a stack with only one item, the entire stack node (and potentially its parent containers) should be popped.

## Problem Statement

### Current Behavior (Broken)

When back is pressed on a stack with only one item:

| Scenario | Expected | Current |
|----------|----------|---------|
| **Root stack, 1 item** | Delegate to system (close app) | ✅ Works correctly |
| **Nested stack in stack, both with 1 item** | Cascade up - pop parent stack | ❌ Returns `CannotHandle` |
| **Stack in TabNode, initial tab, 1 item** | Pop entire TabNode | ❌ Returns `DelegateToSystem` |
| **Stack in TabNode in stack, all with 1 item** | Pop from grandparent stack | ❌ Does not cascade |

### Root Cause Analysis

The issue stems from two functions in `TreeMutator.kt`:

#### 1. `handleNestedStackBack()` - Incomplete Cascading

```kotlin
private fun handleNestedStackBack(
    root: NavNode,
    parentStack: StackNode,
    childStack: StackNode
): BackResult {
    return if (parentStack.children.size > 1) {
        // ✅ Can pop child from parent
        val newState = removeNode(root, childStack.key)
        if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
    } else if (parentStack.parentKey == null) {
        // ✅ Parent is root - delegate to system
        BackResult.DelegateToSystem
    } else {
        // ❌ BUG: Returns CannotHandle instead of cascading up
        BackResult.CannotHandle
    }
}
```

#### 2. `handleTabBack()` - Missing Grandparent Cascade

```kotlin
// In handleTabBack(), when tabParent.children.size == 1:
} else {
    // ❌ BUG: Should cascade to grandparent, not delegate to system
    BackResult.DelegateToSystem
}
```

### Additional Issue: No Predictive Back for Cascade Pops

Currently, predictive back gestures are **only enabled for root stacks**:

```kotlin
// In StackRenderer (NavTreeRenderer.kt)
val predictiveBackEnabled = node.parentKey == null
```

This means:
- Nested stacks don't show predictive back animation during gesture
- When cascade pop occurs, the visual transition is abrupt
- User doesn't get visual feedback for where back will go

---

## Target Behavior Specification

### 1. Stack Cascade Rules

When back is pressed on a stack with only one child:

```
┌────────────────────────────────────────────────────────────────┐
│                    STACK BACK HANDLING FLOW                     │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Stack has > 1 children?                                        │
│       │                                                         │
│       ├── YES → Pop last child (normal pop)                    │
│       │                                                         │
│       └── NO → Check parent type                               │
│                   │                                             │
│                   ├── null (root) → DelegateToSystem           │
│                   │                                             │
│                   ├── StackNode → Try to remove this stack     │
│                   │                  from parent, if parent     │
│                   │                  has 1 child → cascade up   │
│                   │                                             │
│                   ├── TabNode → If initial tab, try to pop     │
│                   │             TabNode from its parent         │
│                   │                                             │
│                   └── PaneNode → Handle per pane behavior      │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

### 2. Tab Container Pop Rules

When back cascades to a TabNode:

```
┌────────────────────────────────────────────────────────────────┐
│                     TAB BACK HANDLING FLOW                      │
├────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Is initial tab (index 0)?                                      │
│       │                                                         │
│       ├── NO → Switch to initial tab (index 0)                 │
│       │                                                         │
│       └── YES → Check TabNode's parent                         │
│                   │                                             │
│                   ├── null (root) → DelegateToSystem           │
│                   │                                             │
│                   ├── StackNode → Try to remove TabNode        │
│                   │                from parent, cascade if     │
│                   │                parent has only this child  │
│                   │                                             │
│                   └── Other → DelegateToSystem                 │
│                                                                  │
└────────────────────────────────────────────────────────────────┘
```

### 3. Predictive Back Animation (Configurable)

Two modes of operation:

| Mode | Description | When to Use |
|------|-------------|-------------|
| **Root Only** (default) | Only root stack gets predictive back | Simple apps, performance-constrained |
| **Full Cascade** | All stacks get predictive back, including cascade animations | Rich navigation experience |

---

## Implementation Plan

### Phase 1: Fix Cascade Logic in TreeMutator

#### Task 1.1: Refactor `handleNestedStackBack()` to Cascade

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt`

**Current Code:**
```kotlin
private fun handleNestedStackBack(
    root: NavNode,
    parentStack: StackNode,
    childStack: StackNode
): BackResult {
    return if (parentStack.children.size > 1) {
        val newState = removeNode(root, childStack.key)
        if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
    } else if (parentStack.parentKey == null) {
        BackResult.DelegateToSystem
    } else {
        BackResult.CannotHandle  // BUG
    }
}
```

**Target Code:**
```kotlin
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
        val newState = removeNode(root, childStack.key)
        if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
    } else if (parentStack.parentKey == null) {
        // Parent is root with only one child - delegate to system
        BackResult.DelegateToSystem
    } else {
        // Parent has only one child and is not root - CASCADE UP
        // Try to pop the parent stack from its grandparent
        val grandparentKey = parentStack.parentKey
        val grandparent = root.findByKey(grandparentKey)
        
        when (grandparent) {
            is StackNode -> handleNestedStackBack(root, grandparent, parentStack)
            is TabNode -> handleTabBack(root, grandparent, parentStack)
            is PaneNode -> handlePaneBack(root, grandparent, parentStack)
            else -> BackResult.DelegateToSystem
        }
    }
}
```

**Effort:** 1 hour

---

#### Task 1.2: Refactor `handleTabBack()` to Cascade

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt`

**Current Code:**
```kotlin
private fun handleTabBack(root: NavNode, tabNode: TabNode, activeStack: StackNode): BackResult {
    // Case 2: Not on initial tab → switch to initial tab
    if (tabNode.activeStackIndex != 0) {
        val newState = switchTab(root, tabNode.key, 0)
        return BackResult.Handled(newState)
    }

    // Case 3: On initial tab at root → check if TabNode itself can be popped
    val tabParentKey = tabNode.parentKey
    if (tabParentKey == null) {
        return BackResult.DelegateToSystem
    }

    val tabParent = root.findByKey(tabParentKey)
    return when (tabParent) {
        is StackNode -> {
            if (tabParent.children.size > 1) {
                val newState = removeNode(root, tabNode.key)
                if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
            } else if (tabParent.parentKey == null) {
                BackResult.DelegateToSystem
            } else {
                BackResult.DelegateToSystem  // BUG
            }
        }
        else -> BackResult.DelegateToSystem
    }
}
```

**Target Code:**
```kotlin
/**
 * Handle back when active stack is inside a TabNode.
 * 
 * Cascade behavior:
 * - If not on initial tab: switch to initial tab
 * - If on initial tab and TabNode can be popped from parent: pop TabNode
 * - If parent also has only one child: cascade further up the tree
 */
private fun handleTabBack(root: NavNode, tabNode: TabNode, activeStack: StackNode): BackResult {
    // Case 1: Not on initial tab → switch to initial tab
    if (tabNode.activeStackIndex != 0) {
        val newState = switchTab(root, tabNode.key, 0)
        return BackResult.Handled(newState)
    }

    // Case 2: On initial tab → try to pop the entire TabNode
    val tabParentKey = tabNode.parentKey
    if (tabParentKey == null) {
        // TabNode is root - delegate to system
        return BackResult.DelegateToSystem
    }

    // TabNode has a parent - try to pop from it
    val tabParent = root.findByKey(tabParentKey)
    return when (tabParent) {
        is StackNode -> {
            if (tabParent.children.size > 1) {
                // Parent stack has multiple children - can pop TabNode
                val newState = removeNode(root, tabNode.key)
                if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
            } else if (tabParent.parentKey == null) {
                // Parent is root stack with only TabNode - delegate to system
                BackResult.DelegateToSystem
            } else {
                // CASCADE: Parent stack has only TabNode - try to pop parent from grandparent
                val grandparentKey = tabParent.parentKey
                val grandparent = root.findByKey(grandparentKey)
                
                when (grandparent) {
                    is StackNode -> handleNestedStackBack(root, grandparent, tabParent)
                    is TabNode -> handleTabBack(root, grandparent, tabParent)
                    is PaneNode -> handlePaneBack(root, grandparent, tabParent)
                    else -> BackResult.DelegateToSystem
                }
            }
        }
        is TabNode -> {
            // TabNode inside another TabNode (edge case)
            // Treat parent TabNode as if we're on its stack
            handleTabBack(root, tabParent, tabNode.activeStack)
        }
        else -> BackResult.DelegateToSystem
    }
}
```

**Effort:** 1.5 hours

---

#### Task 1.3: Add Cascade Helper Functions

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt`

Add utility functions to support cascade logic:

```kotlin
/**
 * Finds the node that will become active after a cascade pop.
 * 
 * This is used for predictive back to know what to preview.
 * 
 * @param root The navigation tree root
 * @param startNode The node from which back is initiated
 * @return The node that will be revealed after cascade, or null if delegating to system
 */
fun findCascadeTarget(root: NavNode, startNode: NavNode): NavNode? {
    val activeStack = startNode as? StackNode ?: root.activeStack() ?: return null
    
    // If stack has > 1 items, target is the previous item
    if (activeStack.children.size > 1) {
        return activeStack.children.dropLast(1).lastOrNull()
    }
    
    // Otherwise, cascade up
    val parentKey = activeStack.parentKey ?: return null // Root - no target
    val parent = root.findByKey(parentKey) ?: return null
    
    return when (parent) {
        is StackNode -> {
            if (parent.children.size > 1) {
                // Target is previous sibling in parent stack
                val siblingIndex = parent.children.indexOfFirst { it.key == activeStack.key }
                if (siblingIndex > 0) parent.children[siblingIndex - 1] else null
            } else {
                // Continue cascade
                findCascadeTarget(root, parent)
            }
        }
        is TabNode -> {
            if (parent.activeStackIndex != 0) {
                // Target is the initial tab's content
                parent.stacks.firstOrNull()?.activeChild
            } else {
                // Continue cascade to TabNode's parent
                parent.parentKey?.let { root.findByKey(it) }?.let { 
                    findCascadeTarget(root, parent) 
                }
            }
        }
        else -> null
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

/**
 * Determines what visual transition should occur on back.
 * 
 * @return The node that will be visually removed (could be screen, stack, or tab container)
 */
fun findBackExitingNode(root: NavNode): NavNode? {
    val activeStack = root.activeStack() ?: return null
    
    if (activeStack.children.size > 1) {
        // Normal pop - exiting the last screen
        return activeStack.activeChild
    }
    
    // Cascade - need to find what container is exiting
    val parentKey = activeStack.parentKey ?: return activeStack.activeChild
    val parent = root.findByKey(parentKey) ?: return activeStack.activeChild
    
    return when (parent) {
        is StackNode -> {
            if (parent.children.size > 1) activeStack else findBackExitingNode(root.copy())
        }
        is TabNode -> {
            if (parent.activeStackIndex != 0) {
                // Switching tabs - exiting current tab's content
                activeStack.activeChild
            } else {
                // Popping entire TabNode
                parent
            }
        }
        else -> activeStack.activeChild
    }
}
```

**Effort:** 2 hours

---

### Phase 2: Configurable Predictive Back for Nested Stacks

#### Task 2.1: Add PredictiveBackMode Configuration

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/gesture/PredictiveBackMode.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.gesture

/**
 * Defines how predictive back gestures are handled in the navigation tree.
 */
public enum class PredictiveBackMode {
    /**
     * Predictive back gestures are only enabled for the root stack.
     * 
     * Nested stacks and stacks inside tabs do not show predictive back animation.
     * Cascade pops occur instantly after the gesture completes.
     * 
     * This is the default mode and is recommended for:
     * - Simple navigation structures
     * - Performance-constrained devices
     * - Apps where most navigation is in the root stack
     */
    ROOT_ONLY,
    
    /**
     * Predictive back gestures are enabled for all stacks, including nested ones.
     * 
     * When back would cascade (pop entire container), the gesture shows a preview
     * of the target screen with the container animating away.
     * 
     * This mode provides a richer navigation experience but requires:
     * - Pre-calculation of cascade targets during gesture start
     * - More complex animation coordination
     * - Additional memory for caching preview content
     * 
     * Recommended for:
     * - Apps with complex nested navigation
     * - When visual consistency across all back actions is important
     */
    FULL_CASCADE
}
```

**Effort:** 30 minutes

---

#### Task 2.2: Update NavRenderScope with Configuration

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavRenderScope.kt`

Add predictive back mode to the render scope:

```kotlin
/**
 * Scope providing dependencies and context for hierarchical navigation rendering.
 */
@Stable
public class NavRenderScope(
    // ... existing parameters ...
    
    /**
     * The predictive back mode for this navigation tree.
     * Determines whether nested stacks receive predictive back gestures.
     */
    public val predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
) {
    // ... existing methods ...
    
    /**
     * Determines if predictive back should be enabled for a given node.
     * 
     * @param node The node to check
     * @return true if predictive back gestures should be enabled for this node
     */
    public fun shouldEnablePredictiveBack(node: NavNode): Boolean {
        return when (predictiveBackMode) {
            PredictiveBackMode.ROOT_ONLY -> node.parentKey == null
            PredictiveBackMode.FULL_CASCADE -> true
        }
    }
}
```

**Effort:** 1 hour

---

#### Task 2.3: Update StackRenderer to Use Configuration

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt`

**Current Code:**
```kotlin
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    // ...
    
    // Enable predictive back only for root stacks (no parent container)
    val predictiveBackEnabled = node.parentKey == null
    
    // ...
}
```

**Target Code:**
```kotlin
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    // ...
    
    // Use configurable predictive back mode
    val predictiveBackEnabled = scope.shouldEnablePredictiveBack(node)
    
    // ...
}
```

**Effort:** 30 minutes

---

#### Task 2.4: Update NavigationHost to Accept Mode

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavigationHost.kt`

Add parameter to the composable:

```kotlin
/**
 * Main navigation host composable for hierarchical navigation.
 * 
 * @param navigator The navigator managing navigation state
 * @param predictiveBackMode Controls predictive back behavior for nested navigation
 *        - ROOT_ONLY (default): Only root stack shows predictive back animation
 *        - FULL_CASCADE: All stacks show predictive back, including cascade animations
 * @param modifier Modifier for the host container
 * @param content Optional wrapper content
 */
@Composable
public fun NavigationHost(
    navigator: TreeNavigator,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    // Create NavRenderScope with the mode
    val renderScope = remember(navigator, predictiveBackMode) {
        NavRenderScope(
            // ... other params ...
            predictiveBackMode = predictiveBackMode,
        )
    }
    
    // ... rest of implementation
}
```

**Effort:** 1 hour

---

### Phase 3: Cascade-Aware Predictive Back Animation

#### Task 3.1: Create CascadeBackState

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/gesture/CascadeBackState.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.gesture

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.core.NavNode

/**
 * State information for a predictive back gesture that may cascade.
 * 
 * Calculated at gesture start to determine:
 * - What will be visually removed (screen, stack, or tab container)
 * - What will be revealed (the target screen after back)
 * - How many levels the cascade goes
 */
@Stable
public data class CascadeBackState(
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
 * @param root The root of the navigation tree
 * @return CascadeBackState describing what the back action will do
 */
public fun calculateCascadeBackState(root: NavNode): CascadeBackState {
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
    
    // Normal pop case
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
    
    // Cascade case - walk up the tree
    var currentNode: NavNode = activeStack
    var depth = 0
    
    while (true) {
        val parentKey = (currentNode as? StackNode)?.parentKey 
            ?: (currentNode as? TabNode)?.parentKey 
            ?: break
            
        val parent = root.findByKey(parentKey) ?: break
        depth++
        
        when (parent) {
            is StackNode -> {
                if (parent.children.size > 1) {
                    // Found a parent that can pop currentNode
                    val siblingIndex = parent.children.indexOfFirst { it.key == currentNode.key }
                    val target = if (siblingIndex > 0) parent.children[siblingIndex - 1].activeDescendant() else null
                    return CascadeBackState(
                        sourceNode = activeChild,
                        exitingNode = currentNode,
                        targetNode = target,
                        cascadeDepth = depth,
                        delegatesToSystem = false
                    )
                }
                // Continue cascade
                currentNode = parent
            }
            is TabNode -> {
                if (parent.activeStackIndex != 0) {
                    // Can switch to initial tab
                    val target = parent.stacks.firstOrNull()?.activeChild
                    return CascadeBackState(
                        sourceNode = activeChild,
                        exitingNode = parent.activeStack.activeChild ?: currentNode,
                        targetNode = target,
                        cascadeDepth = depth,
                        delegatesToSystem = false
                    )
                }
                // Continue cascade
                currentNode = parent
            }
            else -> break
        }
    }
    
    // Reached root - delegate to system
    return CascadeBackState(
        sourceNode = activeChild,
        exitingNode = currentNode,
        targetNode = null,
        cascadeDepth = depth,
        delegatesToSystem = true
    )
}
```

**Effort:** 2 hours

---

#### Task 3.2: Update PredictiveBackController for Cascade

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/gesture/PredictiveBackController.kt`

Add cascade state tracking:

```kotlin
@Stable
public class PredictiveBackController {
    // ... existing state ...
    
    /**
     * The current cascade back state, calculated at gesture start.
     * Null when no gesture is active.
     */
    private var _cascadeState by mutableStateOf<CascadeBackState?>(null)
    
    /**
     * The cascade back state for the current gesture.
     * Use this to determine what to animate during predictive back.
     */
    public val cascadeState: State<CascadeBackState?>
        get() = object : State<CascadeBackState?> {
            override val value: CascadeBackState? get() = _cascadeState
        }
    
    /**
     * Whether the current gesture will result in a cascade pop.
     */
    public val willCascade: Boolean
        get() = _cascadeState?.cascadeDepth ?: 0 > 0
    
    /**
     * Starts a predictive back gesture with cascade awareness.
     *
     * @param cascadeState Pre-calculated cascade state from the navigation tree
     */
    public fun startGestureWithCascade(cascadeState: CascadeBackState) {
        _isActive = true
        _progress = 0f
        _cascadeState = cascadeState
    }
    
    // Update existing complete/cancel to clear cascade state
    public fun completeGesture() {
        _isActive = false
        _progress = 0f
        _cascadeState = null
    }
    
    public fun cancelGesture() {
        _isActive = false
        _progress = 0f
        _cascadeState = null
    }
}
```

**Effort:** 1.5 hours

---

#### Task 3.3: Create CascadePredictiveBackContent Composable

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/CascadePredictiveBackContent.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.navigation.compose.gesture.CascadeBackState
import com.jermey.quo.vadis.core.navigation.core.NavNode

private const val PARALLAX_FACTOR = 0.3f
private const val SCALE_FACTOR = 0.1f
private const val CONTAINER_SCALE_FACTOR = 0.15f

/**
 * Renders predictive back content with cascade awareness.
 * 
 * This composable handles both normal pops (single screen exits) and
 * cascade pops (entire container exits). The animation behavior differs:
 * 
 * ## Normal Pop (cascadeDepth = 0)
 * - Previous screen: Parallax slide in from left
 * - Current screen: Slide right + scale down
 * 
 * ## Cascade Pop (cascadeDepth > 0)
 * - Target screen: Static in place (the destination after cascade)
 * - Exiting container: Entire container (stack/tab) slides right + scales down
 * 
 * @param cascadeState The pre-calculated cascade state
 * @param progress Gesture progress from 0 to 1
 * @param scope Render scope for accessing cache and renderers
 * @param containerContent Composable for rendering the exiting container (when cascade)
 * @param screenContent Composable for rendering individual screens
 */
@Composable
internal fun <T : NavNode> CascadePredictiveBackContent(
    cascadeState: CascadeBackState,
    progress: Float,
    scope: NavRenderScope,
    containerContent: @Composable (NavNode) -> Unit,
    screenContent: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    if (cascadeState.cascadeDepth == 0) {
        // Normal pop - use standard PredictiveBackContent
        @Suppress("UNCHECKED_CAST")
        PredictiveBackContent(
            current = cascadeState.exitingNode as T,
            previous = cascadeState.targetNode as? T,
            progress = progress,
            scope = scope,
            content = screenContent
        )
    } else {
        // Cascade pop - animate container exit
        Box(modifier = Modifier.fillMaxSize()) {
            // Target content - static behind (what will be revealed)
            cascadeState.targetNode?.let { target ->
                Box(modifier = Modifier.fillMaxSize()) {
                    StaticAnimatedVisibilityScope {
                        @Suppress("UNCHECKED_CAST")
                        screenContent(target as T)
                    }
                }
            }
            
            // Exiting container - slides away
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Slide right and scale down (more pronounced for container)
                        translationX = size.width * progress
                        val scale = 1f - (progress * CONTAINER_SCALE_FACTOR)
                        scaleX = scale
                        scaleY = scale
                        // Add elevation/shadow effect for depth
                        shadowElevation = (1f - progress) * 8f
                    }
            ) {
                // Render the full container that's exiting
                containerContent(cascadeState.exitingNode)
            }
        }
    }
}
```

**Effort:** 2 hours

---

#### Task 3.4: Update AnimatedNavContent for Cascade Mode

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/AnimatedNavContent.kt`

Modify to support cascade predictive back:

```kotlin
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    var displayedState by remember { mutableStateOf(targetState) }
    var previousState by remember { mutableStateOf<T?>(null) }

    if (targetState.key != displayedState.key) {
        previousState = displayedState
        displayedState = targetState
    }

    val isPredictiveBackActive = predictiveBackEnabled &&
        scope.predictiveBackController.isActive.value
    
    val cascadeState = scope.predictiveBackController.cascadeState.value

    if (isPredictiveBackActive && cascadeState != null) {
        // Use cascade-aware content for all predictive back
        CascadePredictiveBackContent(
            cascadeState = cascadeState,
            progress = scope.predictiveBackController.progress.value,
            scope = scope,
            containerContent = { node ->
                // Render the full container being removed
                NavNodeRenderer(
                    node = node,
                    previousNode = null,
                    scope = scope
                )
            },
            screenContent = content
        )
    } else {
        // Standard AnimatedContent transition
        AnimatedContent(
            targetState = targetState,
            contentKey = { it.key },
            transitionSpec = {
                val isBack = targetState.key != displayedState.key &&
                    previousState?.key == targetState.key
                transition.createTransitionSpec(isBack = isBack)
            },
            modifier = modifier,
            label = "AnimatedNavContent"
        ) { animatingState ->
            scope.withAnimatedVisibilityScope(this) {
                content(animatingState)
            }
        }
    }
}
```

**Effort:** 2 hours

---

### Phase 4: Tab Renderer Predictive Back Support

#### Task 4.1: Update TabRenderer for Cascade Back

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt`

When in FULL_CASCADE mode, the TabRenderer needs to support predictive back for the "pop entire tabs wrapper" scenario:

```kotlin
@Composable
internal fun TabRenderer(
    node: TabNode,
    previousNode: TabNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    // ... existing setup code ...
    
    // Check if this TabNode is the exiting node for cascade back
    val cascadeState = scope.predictiveBackController.cascadeState.value
    val isExitingInCascade = cascadeState?.exitingNode?.key == node.key &&
        scope.predictiveBackController.isActive.value
    
    // Apply exit animation if this tab is being cascade-popped
    val animatedModifier = if (isExitingInCascade) {
        val progress = scope.predictiveBackController.progress.value
        modifier.graphicsLayer {
            translationX = size.width * progress
            val scale = 1f - (progress * 0.15f)
            scaleX = scale
            scaleY = scale
        }
    } else {
        modifier
    }
    
    scope.cache.CachedEntry(
        key = node.key,
        saveableStateHolder = scope.saveableStateHolder
    ) {
        Box(modifier = animatedModifier) {
            scope.wrapperRegistry.TabWrapper(
                tabNodeKey = node.wrapperKey ?: node.key,
                scope = updatedTabWrapperScope
            ) {
                AnimatedNavContent(
                    targetState = activeStack,
                    transition = scope.animationCoordinator.getTabTransition(
                        fromIndex = previousNode?.activeStackIndex,
                        toIndex = node.activeStackIndex
                    ),
                    scope = scope,
                    // Enable predictive back for tab's internal stack based on mode
                    predictiveBackEnabled = scope.shouldEnablePredictiveBack(activeStack),
                    modifier = Modifier
                ) { stack ->
                    NavNodeRenderer(
                        node = stack,
                        previousNode = previousActiveStack,
                        scope = scope
                    )
                }
            }
        }
    }
}
```

**Effort:** 2 hours

---

### Phase 5: Testing

#### Task 5.1: Unit Tests for Cascade Logic

**New File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutatorCascadeTest.kt`

Test cases:
1. Nested stack cascade (stack → stack, both with 1 item)
2. Tab cascade (stack → tab → stack, all with 1 item)
3. Deep cascade (3+ levels)
4. Mixed cascade (stack → tab → stack)
5. Root constraint respected
6. Non-cascade normal pop still works

```kotlin
class TreeMutatorCascadeTest {
    
    @Test
    fun `nested stack cascade - parent has 1 child - cascades to grandparent`() {
        // Given: RootStack → ChildStack(1 item) → GrandchildStack(1 item)
        val grandchildScreen = ScreenNode("gc1", TestDestination1)
        val grandchildStack = StackNode("grandchild", listOf(grandchildScreen), parentKey = "child")
        val childStack = StackNode("child", listOf(grandchildStack), parentKey = "root")
        val rootScreen = ScreenNode("r1", TestDestination2)
        val root = StackNode("root", listOf(rootScreen, childStack), parentKey = null)
        
        // When
        val result = TreeMutator.popWithTabBehavior(root)
        
        // Then: Should pop childStack from root, revealing rootScreen
        assertIs<BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(1, newState.children.size)
        assertEquals("r1", newState.activeChild?.key)
    }
    
    @Test
    fun `tab cascade - initial tab with 1 item - pops entire TabNode`() {
        // Given: RootStack → [Screen1, TabNode(initial tab with 1 item)]
        val tabScreen = ScreenNode("t1", TestDestination1)
        val tabStack = StackNode("tab-stack-0", listOf(tabScreen))
        val tabNode = TabNode("tabs", listOf(tabStack), activeStackIndex = 0, parentKey = "root")
        val rootScreen = ScreenNode("r1", TestDestination2)
        val root = StackNode("root", listOf(rootScreen, tabNode), parentKey = null)
        
        // When
        val result = TreeMutator.popWithTabBehavior(root)
        
        // Then: Should pop TabNode from root, revealing rootScreen
        assertIs<BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(1, newState.children.size)
        assertEquals("r1", newState.activeChild?.key)
    }
    
    // ... more test cases
}
```

**Effort:** 4 hours

---

#### Task 5.2: Unit Tests for CascadeBackState Calculation

**New File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/gesture/CascadeBackStateTest.kt`

Test cases:
1. Normal pop returns cascadeDepth = 0
2. Single cascade returns cascadeDepth = 1
3. Deep cascade returns correct depth
4. Correct exitingNode for each scenario
5. Correct targetNode for each scenario
6. Root with 1 item → delegatesToSystem = true

**Effort:** 2 hours

---

#### Task 5.3: Integration Tests

**New File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/integration/CascadeBackIntegrationTest.kt`

Test full navigation flows:
1. Navigate deep, back out with cascades
2. Tab navigation with cascade back
3. Mixed navigation patterns
4. Predictive back with cascade (mode testing)

**Effort:** 3 hours

---

### Phase 6: Documentation

#### Task 6.1: Update API Documentation

**File:** `quo-vadis-core/docs/API_REFERENCE.md`

Add sections for:
- `PredictiveBackMode` enum
- Cascade back behavior explanation
- Configuration examples

**Effort:** 1 hour

---

#### Task 6.2: Add Cascade Back Guide

**New File:** `quo-vadis-core/docs/CASCADE_BACK_HANDLING.md`

Document:
- Problem and solution
- Configuration options
- Visual examples of cascade scenarios
- Migration guide

**Effort:** 1 hour

---

## Implementation Order & Timeline

```
Phase 1: Fix Cascade Logic (5 hours)
├── Task 1.1: Refactor handleNestedStackBack
├── Task 1.2: Refactor handleTabBack
└── Task 1.3: Add cascade helper functions

Phase 2: Configurable Mode (3 hours)
├── Task 2.1: Create PredictiveBackMode enum
├── Task 2.2: Update NavRenderScope
├── Task 2.3: Update StackRenderer
└── Task 2.4: Update NavigationHost

Phase 3: Cascade Animation (7.5 hours)
├── Task 3.1: Create CascadeBackState
├── Task 3.2: Update PredictiveBackController
├── Task 3.3: Create CascadePredictiveBackContent
└── Task 3.4: Update AnimatedNavContent

Phase 4: Tab Cascade Support (2 hours)
└── Task 4.1: Update TabRenderer

Phase 5: Testing (9 hours)
├── Task 5.1: Unit tests for cascade logic
├── Task 5.2: Unit tests for CascadeBackState
└── Task 5.3: Integration tests

Phase 6: Documentation (2 hours)
├── Task 6.1: Update API docs
└── Task 6.2: Cascade back guide

Total Estimated Effort: ~28.5 hours
```

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Breaking existing back behavior | HIGH | MEDIUM | Extensive testing, default to ROOT_ONLY mode |
| Animation jank during cascade | MEDIUM | LOW | GPU-accelerated animations, test on low-end devices |
| Complex state management | MEDIUM | MEDIUM | Clear separation of concerns, comprehensive tests |
| Edge cases in deep nesting | MEDIUM | LOW | Test cases for 3+ level nesting |

---

## Success Criteria

1. ✅ **Cascade works correctly**: Nested stack with 1 item → pops parent correctly
2. ✅ **Tab cascade works**: Stack in initial tab with 1 item → pops TabNode
3. ✅ **Root constraint respected**: Root stack with 1 item → delegates to system
4. ✅ **Mode configurable**: User can choose ROOT_ONLY or FULL_CASCADE
5. ✅ **Animation smooth**: Cascade pops animate container exit smoothly
6. ✅ **No regressions**: All existing navigation still works
7. ✅ **Tests comprehensive**: Unit and integration tests cover all scenarios

---

## API Usage Examples

### Default Mode (Root Only)

```kotlin
@Composable
fun MyApp() {
    val navigator = rememberTreeNavigator(/* ... */)
    
    // Default: only root stack gets predictive back animation
    NavigationHost(
        navigator = navigator,
        // predictiveBackMode = PredictiveBackMode.ROOT_ONLY (default)
    )
}
```

### Full Cascade Mode

```kotlin
@Composable
fun MyApp() {
    val navigator = rememberTreeNavigator(/* ... */)
    
    // All stacks get predictive back, including cascade animations
    NavigationHost(
        navigator = navigator,
        predictiveBackMode = PredictiveBackMode.FULL_CASCADE
    )
}
```

### Navigation Structure with Cascade

```kotlin
// This navigation structure:
// RootStack → [HomeScreen, TabNode([TabStack1 → Screen1], [TabStack2 → Screen2])]

// With FULL_CASCADE mode:
// 1. Back on Screen1 (TabStack1 has 1 item) → Pops TabNode, shows HomeScreen
// 2. User sees TabNode animating away with predictive back gesture
// 3. HomeScreen revealed underneath

// With ROOT_ONLY mode:
// 1. Back on Screen1 (TabStack1 has 1 item) → Pops TabNode, shows HomeScreen
// 2. No predictive back animation (instant pop after gesture completes)
```

---

## Dependencies

This plan depends on the existing architecture:
- `TreeMutator` for state mutations
- `PredictiveBackController` for gesture handling
- `NavRenderScope` for rendering context
- `AnimatedNavContent` for transitions

No external dependencies are added.

---

## References

- [BACK_HANDLING_REFACTORING_PLAN.md](./BACK_HANDLING_REFACTORING_PLAN.md) - User-defined back handlers
- [PREDICTIVE_BACK_NAVIGATIONEVENT_REFACTORING_PLAN.md](./PREDICTIVE_BACK_NAVIGATIONEVENT_REFACTORING_PLAN.md) - NavigationEvent API
- [Architecture Patterns Memory](/.serena/memory/architecture_patterns) - Overall architecture
