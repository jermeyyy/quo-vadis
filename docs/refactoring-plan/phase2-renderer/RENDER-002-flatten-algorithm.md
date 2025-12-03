# RENDER-002: Implement flattenState Algorithm

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-002 |
| **Task Name** | Implement flattenState Algorithm |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | CORE-001, RENDER-001 |
| **Blocked By** | CORE-001, RENDER-001 |
| **Blocks** | RENDER-004 |

---

## Overview

The `TreeFlattener` is responsible for transforming the recursive `NavNode` tree into a flat, ordered list of `RenderableSurface` objects. This transformation is the critical bridge between the abstract navigation state and the concrete UI rendering.

### The Flattening Problem

A navigation tree can have arbitrary depth and nesting:

```
StackNode (root)
├── ScreenNode (splash)
├── TabNode (main)
│   ├── StackNode [0] (home tab)
│   │   ├── ScreenNode (feed)
│   │   └── ScreenNode (detail)  ← ACTIVE
│   └── StackNode [1] (profile tab)
│       └── ScreenNode (profile)
└── ScreenNode (settings) ← Would overlay if pushed
```

The renderer needs a simple list:

```kotlin
listOf(
    RenderableSurface(id="feed", zOrder=200, ...),      // Below
    RenderableSurface(id="detail", zOrder=300, ...)    // On top
)
```

### Design Goals

| Goal | Approach |
|------|----------|
| **Correct z-ordering** | Depth-based z-index calculation |
| **Active-only rendering** | Only flatten visible branches |
| **Transition support** | Include previous child during animations |
| **Performance** | Memoization to avoid redundant traversals |
| **Testability** | Pure functions with no side effects |

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattener.kt
```

---

## Implementation

### Core TreeFlattener Class

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.core.*
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable

/**
 * Transforms a NavNode tree into a flat list of RenderableSurfaces.
 * 
 * The TreeFlattener performs a depth-first traversal of the navigation tree,
 * extracting only the currently active/visible nodes and converting them into
 * renderable surfaces with appropriate z-ordering.
 * 
 * ## Algorithm Overview
 * 
 * 1. Start at the root node with base z-order 0
 * 2. For each node type, apply specific flattening rules:
 *    - ScreenNode: Create a single surface
 *    - StackNode: Flatten active child (+ previous during transition)
 *    - TabNode: Flatten the active stack only
 *    - PaneNode: Flatten all panes (all are visible)
 * 3. Increment z-order as we go deeper
 * 4. Return sorted list of surfaces
 * 
 * ## Z-Order Strategy
 * 
 * Z-order is calculated based on tree depth:
 * - Root level: 0
 * - Each depth level adds 100 (configurable via [zOrderIncrement])
 * - During transitions, exiting surface gets current z-order, entering gets +50
 * 
 * This ensures deeper nodes render on top of shallower ones.
 * 
 * @property contentResolver Function to resolve NavNode to @Composable content
 * @property animationResolver Function to resolve animations for node transitions
 * @property zOrderIncrement Z-order increment per depth level
 */
class TreeFlattener(
    private val contentResolver: ContentResolver,
    private val animationResolver: AnimationResolver = DefaultAnimationResolver,
    private val zOrderIncrement: Int = 100
) {

    /**
     * Resolves a NavNode to its composable content.
     */
    fun interface ContentResolver {
        @Composable
        fun resolve(node: NavNode): @Composable () -> Unit
    }

    /**
     * Resolves animation specs for node transitions.
     */
    fun interface AnimationResolver {
        fun resolve(
            from: NavNode?,
            to: NavNode?,
            direction: TransitionDirection
        ): SurfaceAnimationSpec
    }

    /**
     * Direction of the navigation transition.
     */
    enum class TransitionDirection {
        FORWARD,  // Push, switch to higher tab index
        BACKWARD, // Pop, switch to lower tab index
        NONE      // No transition (initial state)
    }

    /**
     * Context passed during flattening to track state.
     */
    private data class FlattenContext(
        val baseZOrder: Int,
        val transitionState: TransitionState?,
        val transitionDirection: TransitionDirection
    )

    /**
     * Flattens the NavNode tree into a list of RenderableSurfaces.
     * 
     * This is the main entry point for tree flattening. It handles both
     * static states and transition states (during animations).
     * 
     * @param root The root of the navigation tree
     * @param transitionState Optional transition state for animations
     * @return List of surfaces sorted by z-order
     */
    fun flattenState(
        root: NavNode,
        transitionState: TransitionState? = null
    ): List<RenderableSurface> {
        val context = FlattenContext(
            baseZOrder = 0,
            transitionState = transitionState,
            transitionDirection = transitionState?.direction ?: TransitionDirection.NONE
        )
        
        return flatten(root, context)
            .sortedBy { it.zOrder }
    }

    /**
     * Internal recursive flattening function.
     */
    private fun flatten(
        node: NavNode,
        context: FlattenContext
    ): List<RenderableSurface> {
        return when (node) {
            is ScreenNode -> flattenScreen(node, context)
            is StackNode -> flattenStack(node, context)
            is TabNode -> flattenTab(node, context)
            is PaneNode -> flattenPane(node, context)
        }
    }

    /**
     * Flattens a ScreenNode into a single surface.
     * 
     * ScreenNodes are leaf nodes that always produce exactly one surface.
     */
    private fun flattenScreen(
        screen: ScreenNode,
        context: FlattenContext
    ): List<RenderableSurface> {
        val transitionState = determineTransitionState(screen, context)
        val animationSpec = animationResolver.resolve(
            from = null,
            to = screen,
            direction = context.transitionDirection
        )
        
        return listOf(
            RenderableSurface(
                id = screen.key,
                zOrder = context.baseZOrder,
                nodeType = SurfaceNodeType.SCREEN,
                transitionState = transitionState,
                animationSpec = animationSpec,
                content = contentResolver.resolve(screen)
            )
        )
    }

    /**
     * Flattens a StackNode by processing its active (and optionally previous) child.
     * 
     * During transitions:
     * - The exiting (previous) child gets current z-order
     * - The entering (new) child gets z-order + 50
     * 
     * @param stack The StackNode to flatten
     * @param context Current flattening context
     * @return List of surfaces from the stack's active path
     */
    private fun flattenStack(
        stack: StackNode,
        context: FlattenContext
    ): List<RenderableSurface> {
        val surfaces = mutableListOf<RenderableSurface>()
        val activeChild = stack.activeChild ?: return emptyList()
        
        // Check if we're in a transition that involves this stack
        val stackTransition = context.transitionState?.let { transition ->
            if (transition.affectsStack(stack.key)) transition else null
        }
        
        if (stackTransition != null && stackTransition is TransitionState.Animating) {
            // During animation, render both previous and current
            val previousChild = stackTransition.previousChildOf(stack.key)
            
            if (previousChild != null) {
                // Previous child (exiting) - lower z-order
                val exitContext = context.copy(
                    baseZOrder = context.baseZOrder,
                    transitionDirection = TransitionDirection.BACKWARD
                )
                surfaces.addAll(flattenWithExiting(previousChild, exitContext, stackTransition.progress))
            }
            
            // Current child (entering) - higher z-order
            val enterContext = context.copy(
                baseZOrder = context.baseZOrder + zOrderIncrement / 2,
                transitionDirection = TransitionDirection.FORWARD
            )
            surfaces.addAll(flattenWithEntering(activeChild, enterContext, stackTransition.progress))
            
        } else if (stackTransition != null && stackTransition is TransitionState.Proposed) {
            // During predictive back, render both with gesture-driven progress
            val previousChild = stackTransition.previousChildOf(stack.key)
            
            if (previousChild != null) {
                // Previous child (will be shown if gesture completes)
                val exitContext = context.copy(
                    baseZOrder = context.baseZOrder
                )
                surfaces.addAll(flattenWithPredictiveExiting(activeChild, exitContext, stackTransition.progress))
            }
            
            // Current child (will exit if gesture completes)
            val enterContext = context.copy(
                baseZOrder = context.baseZOrder + zOrderIncrement / 2
            )
            surfaces.addAll(flattenWithPredictiveEntering(previousChild ?: activeChild, enterContext, stackTransition.progress))
            
        } else {
            // No transition - just flatten the active child
            val childContext = context.copy(
                baseZOrder = context.baseZOrder + zOrderIncrement
            )
            surfaces.addAll(flatten(activeChild, childContext))
        }
        
        return surfaces
    }

    /**
     * Flattens a TabNode by processing only the active stack.
     * 
     * TabNode's inactive stacks are not rendered but their state is preserved
     * via SaveableStateHolder in the QuoVadisHost.
     * 
     * @param tab The TabNode to flatten
     * @param context Current flattening context
     * @return List of surfaces from the active tab's stack
     */
    private fun flattenTab(
        tab: TabNode,
        context: FlattenContext
    ): List<RenderableSurface> {
        val activeStack = tab.activeStack
        
        // Check if we're in a tab switch transition
        val tabTransition = context.transitionState?.let { transition ->
            if (transition.affectsTab(tab.key)) transition else null
        }
        
        if (tabTransition != null && tabTransition is TransitionState.Animating) {
            val previousIndex = tabTransition.previousTabIndex(tab.key)
            if (previousIndex != null && previousIndex != tab.activeStackIndex) {
                val surfaces = mutableListOf<RenderableSurface>()
                
                // Previous tab (exiting)
                val previousStack = tab.stacks[previousIndex]
                val exitContext = context.copy(
                    baseZOrder = context.baseZOrder,
                    transitionDirection = if (previousIndex < tab.activeStackIndex) 
                        TransitionDirection.BACKWARD else TransitionDirection.FORWARD
                )
                surfaces.addAll(flattenWithExiting(previousStack, exitContext, tabTransition.progress))
                
                // Current tab (entering)
                val enterContext = context.copy(
                    baseZOrder = context.baseZOrder + zOrderIncrement / 2,
                    transitionDirection = if (tab.activeStackIndex > previousIndex)
                        TransitionDirection.FORWARD else TransitionDirection.BACKWARD
                )
                surfaces.addAll(flattenWithEntering(activeStack, enterContext, tabTransition.progress))
                
                return surfaces
            }
        }
        
        // No transition - flatten active stack only
        val childContext = context.copy(
            baseZOrder = context.baseZOrder + zOrderIncrement
        )
        return flatten(activeStack, childContext)
    }

    /**
     * Flattens a PaneNode by processing all panes.
     * 
     * Unlike StackNode and TabNode, PaneNode renders ALL children simultaneously.
     * Each pane gets an incremented z-order based on its index.
     * 
     * @param pane The PaneNode to flatten
     * @param context Current flattening context
     * @return List of surfaces from all panes
     */
    private fun flattenPane(
        pane: PaneNode,
        context: FlattenContext
    ): List<RenderableSurface> {
        return pane.panes.flatMapIndexed { index, childNode ->
            val paneContext = context.copy(
                baseZOrder = context.baseZOrder + (index * zOrderIncrement)
            )
            flatten(childNode, paneContext)
        }
    }

    // =========================================================================
    // TRANSITION HELPERS
    // =========================================================================

    private fun flattenWithEntering(
        node: NavNode,
        context: FlattenContext,
        progress: Float
    ): List<RenderableSurface> {
        return flatten(node, context).map { surface ->
            surface.copy(
                transitionState = SurfaceTransitionState.Entering(
                    progress = progress,
                    isPredictive = false
                )
            )
        }
    }

    private fun flattenWithExiting(
        node: NavNode,
        context: FlattenContext,
        progress: Float
    ): List<RenderableSurface> {
        return flatten(node, context).map { surface ->
            surface.copy(
                transitionState = SurfaceTransitionState.Exiting(
                    progress = progress,
                    isPredictive = false
                )
            )
        }
    }

    private fun flattenWithPredictiveEntering(
        node: NavNode,
        context: FlattenContext,
        progress: Float
    ): List<RenderableSurface> {
        return flatten(node, context).map { surface ->
            surface.copy(
                transitionState = SurfaceTransitionState.Entering(
                    progress = progress,
                    isPredictive = true
                )
            )
        }
    }

    private fun flattenWithPredictiveExiting(
        node: NavNode,
        context: FlattenContext,
        progress: Float
    ): List<RenderableSurface> {
        return flatten(node, context).map { surface ->
            surface.copy(
                transitionState = SurfaceTransitionState.Exiting(
                    progress = progress,
                    isPredictive = true
                )
            )
        }
    }

    private fun determineTransitionState(
        node: NavNode,
        context: FlattenContext
    ): SurfaceTransitionState {
        return when (context.transitionState) {
            null -> SurfaceTransitionState.Visible
            is TransitionState.Idle -> SurfaceTransitionState.Visible
            is TransitionState.Proposed -> SurfaceTransitionState.Visible
            is TransitionState.Animating -> SurfaceTransitionState.Visible
        }
    }

    companion object {
        /**
         * Default animation resolver providing standard slide animations.
         */
        val DefaultAnimationResolver = AnimationResolver { from, to, direction ->
            when (direction) {
                TransitionDirection.FORWARD -> SurfaceAnimationSpec(
                    enter = slideInHorizontally { it } + fadeIn(),
                    exit = slideOutHorizontally { -it / 3 } + fadeOut()
                )
                TransitionDirection.BACKWARD -> SurfaceAnimationSpec(
                    enter = slideInHorizontally { -it / 3 } + fadeIn(),
                    exit = slideOutHorizontally { it } + fadeOut()
                )
                TransitionDirection.NONE -> SurfaceAnimationSpec.None
            }
        }
    }
}
```

### TransitionState Extension (Referenced)

This task assumes `TransitionState` from [RENDER-003](./RENDER-003-transition-state.md). For compilation, add stubs or import when available:

```kotlin
/**
 * Stub interface - see RENDER-003 for full implementation.
 */
sealed interface TransitionState {
    val direction: TreeFlattener.TransitionDirection
    
    fun affectsStack(stackKey: String): Boolean
    fun affectsTab(tabKey: String): Boolean
    fun previousChildOf(stackKey: String): NavNode?
    fun previousTabIndex(tabKey: String): Int?
    
    data class Idle(val current: NavNode) : TransitionState {
        override val direction = TreeFlattener.TransitionDirection.NONE
        override fun affectsStack(stackKey: String) = false
        override fun affectsTab(tabKey: String) = false
        override fun previousChildOf(stackKey: String): NavNode? = null
        override fun previousTabIndex(tabKey: String): Int? = null
    }
    
    data class Proposed(
        val current: NavNode,
        val next: NavNode,
        val progress: Float
    ) : TransitionState {
        override val direction = TreeFlattener.TransitionDirection.BACKWARD
        override fun affectsStack(stackKey: String) = true // Simplified
        override fun affectsTab(tabKey: String) = false
        override fun previousChildOf(stackKey: String): NavNode? = null // Simplified
        override fun previousTabIndex(tabKey: String): Int? = null
    }
    
    data class Animating(
        val current: NavNode,
        val next: NavNode,
        val progress: Float
    ) : TransitionState {
        override val direction = TreeFlattener.TransitionDirection.FORWARD
        override fun affectsStack(stackKey: String) = true // Simplified
        override fun affectsTab(tabKey: String) = false
        override fun previousChildOf(stackKey: String): NavNode? = null // Simplified
        override fun previousTabIndex(tabKey: String): Int? = null
    }
}
```

---

## Algorithm Visualization

### Example 1: Simple Stack

```
Input Tree:
StackNode("root")
├── ScreenNode("home")
└── ScreenNode("detail")  ← ACTIVE

Output:
[
  RenderableSurface(id="home", zOrder=100),   // Not rendered (not active)
  RenderableSurface(id="detail", zOrder=100)  // Rendered
]

Wait - only active path is flattened:
[
  RenderableSurface(id="detail", zOrder=100)
]
```

### Example 2: TabNode with Active Stack

```
Input Tree:
StackNode("root")
└── TabNode("tabs", activeStackIndex=0)
    ├── StackNode("home-stack") [0]  ← ACTIVE
    │   └── ScreenNode("feed")
    └── StackNode("profile-stack") [1]
        └── ScreenNode("profile")

Output (only active path):
[
  RenderableSurface(id="feed", zOrder=200)
]
```

### Example 3: During Push Transition

```
Input Tree (after push, animation in progress):
StackNode("root")
├── ScreenNode("home")      ← PREVIOUS (exiting)
└── ScreenNode("detail")    ← ACTIVE (entering)

TransitionState: Animating(progress=0.5)

Output:
[
  RenderableSurface(id="home", zOrder=100, state=Exiting(0.5)),
  RenderableSurface(id="detail", zOrder=150, state=Entering(0.5))
]
```

### Example 4: PaneNode (All Rendered)

```
Input Tree:
PaneNode("split")
├── StackNode("list-pane")
│   └── ScreenNode("list")
└── StackNode("detail-pane")
    └── ScreenNode("detail")

Output (both panes rendered):
[
  RenderableSurface(id="list", zOrder=100),
  RenderableSurface(id="detail", zOrder=200)
]
```

---

## Z-Index Calculation

The z-index is calculated to ensure proper layering:

| Scenario | Z-Order Formula |
|----------|-----------------|
| Base level | `baseZOrder` |
| Child of container | `parentZOrder + zOrderIncrement` |
| Entering during transition | `exitingZOrder + zOrderIncrement/2` |
| Pane at index N | `baseZOrder + (N * zOrderIncrement)` |

### Example Calculation

```
StackNode (z=0)
└── TabNode (z=100)
    └── StackNode [active] (z=200)
        ├── ScreenNode (z=300, exiting)
        └── ScreenNode (z=350, entering)
```

---

## Implementation Steps

### Step 1: Create Core Structure

1. Create `TreeFlattener.kt` file
2. Define `ContentResolver` and `AnimationResolver` interfaces
3. Define `TransitionDirection` enum
4. Create `FlattenContext` data class

### Step 2: Implement Node-Specific Flattening

1. Implement `flattenScreen()` - straightforward
2. Implement `flattenStack()` - handles active child + transitions
3. Implement `flattenTab()` - active stack only
4. Implement `flattenPane()` - all panes

### Step 3: Add Transition Support

1. Implement transition detection methods
2. Add `flattenWithEntering()` and `flattenWithExiting()`
3. Add predictive back variants
4. Wire up `TransitionState` handling

### Step 4: Optimization

1. Add memoization for repeated flattening (see RISK-001)
2. Profile for large trees
3. Consider lazy evaluation if needed

### Step 5: Testing

1. Unit tests for each node type
2. Integration tests with transitions
3. Performance benchmarks

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/TreeFlattener.kt` | Create | Main flattener implementation |
| `quo-vadis-core/.../compose/RenderableSurface.kt` | Reference | Uses RenderableSurface from RENDER-001 |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| CORE-001 (NavNode Hierarchy) | Hard | Must complete first |
| RENDER-001 (RenderableSurface) | Hard | Must complete first |
| RENDER-003 (TransitionState) | Soft | Can use stubs initially |

---

## Acceptance Criteria

- [ ] `TreeFlattener` class implemented with `flattenState()` method
- [ ] `ContentResolver` interface for resolving node content
- [ ] `AnimationResolver` interface for resolving animations
- [ ] `flattenScreen()` correctly creates single surface
- [ ] `flattenStack()` handles active child + transition states
- [ ] `flattenTab()` only flattens active stack
- [ ] `flattenPane()` flattens all panes with correct z-ordering
- [ ] Z-order calculation based on tree depth
- [ ] Transition state support (entering/exiting)
- [ ] Predictive back gesture support (proposed state)
- [ ] Default animation resolver with slide animations
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for all node types
- [ ] Unit tests for transition scenarios
- [ ] Code compiles on all target platforms

---

## Performance Considerations

### Memoization Strategy

The flattener may be called frequently during recomposition. Consider:

```kotlin
class MemoizedTreeFlattener(
    private val flattener: TreeFlattener
) {
    private var lastRoot: NavNode? = null
    private var lastTransition: TransitionState? = null
    private var cachedResult: List<RenderableSurface>? = null
    
    fun flattenState(
        root: NavNode,
        transitionState: TransitionState? = null
    ): List<RenderableSurface> {
        if (root === lastRoot && transitionState === lastTransition) {
            return cachedResult ?: flattener.flattenState(root, transitionState)
        }
        
        lastRoot = root
        lastTransition = transitionState
        cachedResult = flattener.flattenState(root, transitionState)
        return cachedResult!!
    }
}
```

See [RISK-001](../phase6-risks/RISK-001-memoized-flatten.md) for detailed memoization implementation.

---

## Testing Notes

```kotlin
@Test
fun `flattenState returns empty list for empty stack`() {
    val flattener = TreeFlattener(contentResolver = { {} })
    val root = StackNode("root", null, emptyList())
    
    val result = flattener.flattenState(root)
    
    assertTrue(result.isEmpty())
}

@Test
fun `flattenState returns single surface for screen`() {
    val flattener = TreeFlattener(contentResolver = { {} })
    val screen = ScreenNode("s1", null, mockDestination)
    
    val result = flattener.flattenState(screen)
    
    assertEquals(1, result.size)
    assertEquals("s1", result[0].id)
    assertEquals(SurfaceNodeType.SCREEN, result[0].nodeType)
}

@Test
fun `flattenStack only includes active child`() {
    val flattener = TreeFlattener(contentResolver = { {} })
    val screen1 = ScreenNode("s1", "stack", mockDestination)
    val screen2 = ScreenNode("s2", "stack", mockDestination)
    val stack = StackNode("stack", null, listOf(screen1, screen2))
    
    val result = flattener.flattenState(stack)
    
    assertEquals(1, result.size)
    assertEquals("s2", result[0].id) // Only active child
}

@Test
fun `flattenTab only includes active stack`() {
    val flattener = TreeFlattener(contentResolver = { {} })
    val stack1 = StackNode("s1", "tabs", listOf(ScreenNode("home", "s1", mockDestination)))
    val stack2 = StackNode("s2", "tabs", listOf(ScreenNode("profile", "s2", mockDestination)))
    val tabs = TabNode("tabs", null, listOf(stack1, stack2), activeStackIndex = 0)
    
    val result = flattener.flattenState(tabs)
    
    assertEquals(1, result.size)
    assertEquals("home", result[0].id)
}

@Test
fun `flattenPane includes all panes`() {
    val flattener = TreeFlattener(contentResolver = { {} })
    val pane1 = ScreenNode("list", "panes", mockDestination)
    val pane2 = ScreenNode("detail", "panes", mockDestination)
    val panes = PaneNode("panes", null, listOf(pane1, pane2))
    
    val result = flattener.flattenState(panes)
    
    assertEquals(2, result.size)
    assertTrue(result.any { it.id == "list" })
    assertTrue(result.any { it.id == "detail" })
}

@Test
fun `z-order increases with depth`() {
    val flattener = TreeFlattener(contentResolver = { {} }, zOrderIncrement = 100)
    val screen = ScreenNode("s1", "stack", mockDestination)
    val stack = StackNode("stack", null, listOf(screen))
    
    val result = flattener.flattenState(stack)
    
    assertEquals(100, result[0].zOrder) // One level deep
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-001](./RENDER-001-renderable-surface.md) - RenderableSurface definition
- [RENDER-003](./RENDER-003-transition-state.md) - TransitionState for animations
- [RENDER-004](./RENDER-004-quovadis-host.md) - Consumer of flattened surfaces
- [RISK-001](../phase6-risks/RISK-001-memoized-flatten.md) - Memoization optimization
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.2.1 "Tree Flattening Algorithm"
