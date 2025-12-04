````markdown
# RENDER-002A: Core flattenState Algorithm (Screen/Stack)

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-002A |
| **Task Name** | Core flattenState Algorithm (Screen/Stack) |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | High |
| **Estimated Time** | 2 days |
| **Dependencies** | CORE-001, RENDER-001 |
| **Blocked By** | CORE-001, RENDER-001 |
| **Blocks** | RENDER-002B, RENDER-002C, RENDER-004 |

---

## Overview

This task implements the core flattening algorithm for `ScreenNode` and `StackNode`. It defines the base `TreeFlattener` class and the fundamental output structures (`FlattenResult`, `AnimationPair`, `CachingHints`) that will be used by all subsequent flattening tasks.

### Purpose

The `TreeFlattener` transforms the recursive `NavNode` tree into a flat, ordered list of `RenderableSurface` objects. This task focuses on:

1. **Core data structures** - `FlattenResult`, `AnimationPair`, `CachingHints`, `TransitionType`
2. **ScreenNode flattening** - Simple case producing a single surface
3. **StackNode flattening** - Handles active child and tracks previous for animations

### Why Split from RENDER-002?

The original RENDER-002 was too large, covering all node types. This split allows:
- Incremental development with clear milestones
- Easier testing and review
- Parallel work on Tab (RENDER-002B) and Pane (RENDER-002C) once this is complete

### Flattening Problem (Screen/Stack Focus)

```
StackNode (root)
├── ScreenNode (home)       ← PREVIOUS (track for animation)
└── ScreenNode (detail)     ← ACTIVE (current visible)

Output:
FlattenResult(
    surfaces = [
        RenderableSurface(id="detail", renderingMode=STACK_CONTENT, previousSurfaceId="home")
    ],
    animationPairs = [
        AnimationPair(currentId="detail", previousId="home", transitionType=PUSH)
    ],
    cachingHints = CachingHints(...)
)
```

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattener.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/FlattenResult.kt
```

---

## Implementation

### Core Data Structures

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Immutable

/**
 * Type of transition between surfaces.
 * 
 * Used to determine which animation to apply and how to handle
 * caching during navigation.
 */
enum class TransitionType {
    /** Forward navigation in a stack (push) */
    PUSH,
    
    /** Backward navigation in a stack (pop) */
    POP,
    
    /** Switching between tabs in a TabNode */
    TAB_SWITCH,
    
    /** Switching between panes or pane configurations */
    PANE_SWITCH,
    
    /** No transition (initial state or no animation needed) */
    NONE
}

/**
 * Represents a pair of surfaces involved in a transition animation.
 * 
 * This is used by the renderer to coordinate enter/exit animations
 * between the current and previous surfaces.
 * 
 * @property currentId ID of the surface that is entering/visible
 * @property previousId ID of the surface that is exiting (null if no previous)
 * @property transitionType Type of transition being performed
 */
@Immutable
data class AnimationPair(
    val currentId: String,
    val previousId: String?,
    val transitionType: TransitionType
)

/**
 * Hints for the caching system about how to cache surfaces.
 * 
 * These hints help the renderer decide which surfaces to cache
 * and at what granularity during navigation.
 * 
 * @property shouldCacheWrapper Whether wrapper surfaces (TAB_WRAPPER, PANE_WRAPPER) should be cached
 * @property shouldCacheContent Whether content surfaces should be cached
 * @property cacheableIds IDs of surfaces that are safe to cache
 * @property invalidatedIds IDs of surfaces whose cache should be invalidated
 */
@Immutable
data class CachingHints(
    val shouldCacheWrapper: Boolean = false,
    val shouldCacheContent: Boolean = true,
    val cacheableIds: Set<String> = emptySet(),
    val invalidatedIds: Set<String> = emptySet()
) {
    companion object {
        /** Default hints with no special caching behavior */
        val Default = CachingHints()
        
        /** Hints indicating no caching should occur */
        val NoCache = CachingHints(
            shouldCacheWrapper = false,
            shouldCacheContent = false
        )
    }
}

/**
 * Result of flattening a NavNode tree.
 * 
 * Contains all information needed by the renderer to display
 * the navigation state with proper animations and caching.
 * 
 * @property surfaces Ordered list of surfaces to render (sorted by zOrder)
 * @property animationPairs Pairs of surfaces involved in transitions
 * @property cachingHints Hints for the caching system
 */
@Immutable
data class FlattenResult(
    val surfaces: List<RenderableSurface>,
    val animationPairs: List<AnimationPair>,
    val cachingHints: CachingHints
) {
    companion object {
        /** Empty result with no surfaces */
        val Empty = FlattenResult(
            surfaces = emptyList(),
            animationPairs = emptyList(),
            cachingHints = CachingHints.Default
        )
    }
    
    /**
     * Returns surfaces that should be rendered (not hidden).
     */
    val renderableSurfaces: List<RenderableSurface>
        get() = surfaces.filter { it.shouldRender }
    
    /**
     * Returns surfaces sorted by z-order for proper layering.
     */
    val sortedSurfaces: List<RenderableSurface>
        get() = surfaces.sortedBy { it.zOrder }
    
    /**
     * Finds the animation pair for a given surface ID.
     */
    fun findAnimationPair(surfaceId: String): AnimationPair? {
        return animationPairs.find { it.currentId == surfaceId || it.previousId == surfaceId }
    }
}
```

### TreeFlattener Base Class

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.core.*
import androidx.compose.runtime.Composable

/**
 * Transforms a NavNode tree into a FlattenResult containing RenderableSurfaces.
 * 
 * The TreeFlattener performs a depth-first traversal of the navigation tree,
 * extracting only the currently active/visible nodes and converting them into
 * renderable surfaces with appropriate z-ordering and animation pairing.
 * 
 * ## Algorithm Overview (Screen/Stack)
 * 
 * 1. Start at the root node with base z-order 0
 * 2. For ScreenNode: Create a single surface with SINGLE_SCREEN mode
 * 3. For StackNode: Flatten active child, track previous for animations
 * 4. Populate previousSurfaceId for animation coordination
 * 5. Return FlattenResult with surfaces, animation pairs, and caching hints
 * 
 * ## Z-Order Strategy
 * 
 * - Root level: 0
 * - Each depth level adds [zOrderIncrement] (default: 100)
 * - During transitions, exiting surface gets current z-order, entering gets +50
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
            transitionType: TransitionType
        ): SurfaceAnimationSpec
    }

    /**
     * Context passed during flattening to track state.
     */
    private data class FlattenContext(
        val baseZOrder: Int,
        val parentId: String?,
        val previousSiblingId: String?,
        val transitionType: TransitionType
    )

    /**
     * Accumulated state during flattening.
     */
    private class FlattenAccumulator {
        val surfaces = mutableListOf<RenderableSurface>()
        val animationPairs = mutableListOf<AnimationPair>()
        val cacheableIds = mutableSetOf<String>()
        val invalidatedIds = mutableSetOf<String>()
        
        fun addSurface(surface: RenderableSurface) {
            surfaces.add(surface)
        }
        
        fun addAnimationPair(pair: AnimationPair) {
            animationPairs.add(pair)
        }
        
        fun toResult(shouldCacheWrapper: Boolean = false): FlattenResult {
            return FlattenResult(
                surfaces = surfaces.sortedBy { it.zOrder },
                animationPairs = animationPairs.toList(),
                cachingHints = CachingHints(
                    shouldCacheWrapper = shouldCacheWrapper,
                    shouldCacheContent = true,
                    cacheableIds = cacheableIds.toSet(),
                    invalidatedIds = invalidatedIds.toSet()
                )
            )
        }
    }

    /**
     * Flattens the NavNode tree into a FlattenResult.
     * 
     * This is the main entry point for tree flattening.
     * 
     * @param root The root of the navigation tree
     * @param previousRoot Optional previous root for transition detection
     * @return FlattenResult containing surfaces, animation pairs, and caching hints
     */
    fun flattenState(
        root: NavNode,
        previousRoot: NavNode? = null
    ): FlattenResult {
        val accumulator = FlattenAccumulator()
        val transitionType = detectTransitionType(previousRoot, root)
        
        val context = FlattenContext(
            baseZOrder = 0,
            parentId = null,
            previousSiblingId = findPreviousSiblingId(previousRoot, root),
            transitionType = transitionType
        )
        
        flatten(root, context, accumulator)
        
        return accumulator.toResult()
    }

    /**
     * Internal recursive flattening function.
     * 
     * Dispatches to node-type-specific handlers.
     */
    private fun flatten(
        node: NavNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        when (node) {
            is ScreenNode -> flattenScreen(node, context, accumulator)
            is StackNode -> flattenStack(node, context, accumulator)
            is TabNode -> flattenTab(node, context, accumulator)
            is PaneNode -> flattenPane(node, context, accumulator)
        }
    }

    /**
     * Flattens a ScreenNode into a single surface.
     * 
     * ScreenNodes are leaf nodes that always produce exactly one surface
     * with [SurfaceRenderingMode.SINGLE_SCREEN].
     * 
     * @param screen The ScreenNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenScreen(
        screen: ScreenNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val animationSpec = animationResolver.resolve(
            from = null,
            to = screen,
            transitionType = context.transitionType
        )
        
        val surface = RenderableSurface(
            id = screen.key,
            zOrder = context.baseZOrder,
            nodeType = SurfaceNodeType.SCREEN,
            renderingMode = SurfaceRenderingMode.SINGLE_SCREEN,
            transitionState = SurfaceTransitionState.Visible,
            animationSpec = animationSpec,
            content = contentResolver.resolve(screen),
            parentWrapperId = context.parentId,
            previousSurfaceId = context.previousSiblingId
        )
        
        accumulator.addSurface(surface)
        
        // Add animation pair if there's a previous surface
        if (context.previousSiblingId != null) {
            accumulator.addAnimationPair(
                AnimationPair(
                    currentId = screen.key,
                    previousId = context.previousSiblingId,
                    transitionType = context.transitionType
                )
            )
        }
    }

    /**
     * Flattens a StackNode by processing its active child.
     * 
     * Tracks the previous child for animation pairing and sets
     * [SurfaceRenderingMode.STACK_CONTENT] for proper rendering.
     * 
     * Key behaviors:
     * - Only the active (top) child is rendered
     * - Previous child ID is tracked for animations
     * - Z-order increments for nested content
     * 
     * @param stack The StackNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenStack(
        stack: StackNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val children = stack.children
        if (children.isEmpty()) return
        
        val activeChild = children.last()
        val previousChild = if (children.size > 1) children[children.size - 2] else null
        
        // Determine transition type based on stack state
        val transitionType = when {
            previousChild != null && context.transitionType == TransitionType.NONE -> TransitionType.PUSH
            else -> context.transitionType
        }
        
        val childContext = FlattenContext(
            baseZOrder = context.baseZOrder + zOrderIncrement,
            parentId = stack.key,
            previousSiblingId = previousChild?.key,
            transitionType = transitionType
        )
        
        // Flatten the active child
        when (activeChild) {
            is ScreenNode -> {
                // Override to use STACK_CONTENT mode for screens in a stack
                val animationSpec = animationResolver.resolve(
                    from = previousChild,
                    to = activeChild,
                    transitionType = transitionType
                )
                
                val surface = RenderableSurface(
                    id = activeChild.key,
                    zOrder = childContext.baseZOrder,
                    nodeType = SurfaceNodeType.SCREEN,
                    renderingMode = SurfaceRenderingMode.STACK_CONTENT,
                    transitionState = SurfaceTransitionState.Visible,
                    animationSpec = animationSpec,
                    content = contentResolver.resolve(activeChild),
                    parentWrapperId = stack.key,
                    previousSurfaceId = previousChild?.key
                )
                
                accumulator.addSurface(surface)
                
                // Add animation pair for stack navigation
                if (previousChild != null) {
                    accumulator.addAnimationPair(
                        AnimationPair(
                            currentId = activeChild.key,
                            previousId = previousChild.key,
                            transitionType = transitionType
                        )
                    )
                }
            }
            else -> {
                // For non-screen children (nested containers), recurse
                flatten(activeChild, childContext, accumulator)
            }
        }
    }

    /**
     * Placeholder for TabNode flattening.
     * 
     * Full implementation in RENDER-002B.
     */
    private fun flattenTab(
        tab: TabNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        // Placeholder - will be implemented in RENDER-002B
        // For now, just flatten the active stack
        val activeStack = tab.activeStack
        val childContext = context.copy(
            baseZOrder = context.baseZOrder + zOrderIncrement,
            parentId = tab.key
        )
        flatten(activeStack, childContext, accumulator)
    }

    /**
     * Placeholder for PaneNode flattening.
     * 
     * Full implementation in RENDER-002C.
     */
    private fun flattenPane(
        pane: PaneNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        // Placeholder - will be implemented in RENDER-002C
        // For now, flatten all panes
        pane.panes.forEachIndexed { index, childNode ->
            val paneContext = context.copy(
                baseZOrder = context.baseZOrder + (index * zOrderIncrement),
                parentId = pane.key
            )
            flatten(childNode, paneContext, accumulator)
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Detects the type of transition between previous and current state.
     */
    private fun detectTransitionType(
        previousRoot: NavNode?,
        currentRoot: NavNode
    ): TransitionType {
        if (previousRoot == null) return TransitionType.NONE
        
        // Simple heuristic - compare active paths
        val previousPath = getActivePath(previousRoot)
        val currentPath = getActivePath(currentRoot)
        
        return when {
            currentPath.size > previousPath.size -> TransitionType.PUSH
            currentPath.size < previousPath.size -> TransitionType.POP
            else -> TransitionType.NONE
        }
    }

    /**
     * Finds the previous sibling ID for animation pairing.
     */
    private fun findPreviousSiblingId(
        previousRoot: NavNode?,
        currentRoot: NavNode
    ): String? {
        if (previousRoot == null) return null
        
        val previousLeaf = getActiveLeaf(previousRoot)
        val currentLeaf = getActiveLeaf(currentRoot)
        
        return if (previousLeaf?.key != currentLeaf?.key) {
            previousLeaf?.key
        } else {
            null
        }
    }

    /**
     * Gets the active path from root to leaf.
     */
    private fun getActivePath(node: NavNode): List<NavNode> {
        val path = mutableListOf<NavNode>()
        var current: NavNode? = node
        
        while (current != null) {
            path.add(current)
            current = when (current) {
                is ScreenNode -> null
                is StackNode -> current.activeChild
                is TabNode -> current.activeStack
                is PaneNode -> current.panes.firstOrNull()
            }
        }
        
        return path
    }

    /**
     * Gets the active leaf node.
     */
    private fun getActiveLeaf(node: NavNode): NavNode? {
        return when (node) {
            is ScreenNode -> node
            is StackNode -> node.activeChild?.let { getActiveLeaf(it) }
            is TabNode -> getActiveLeaf(node.activeStack)
            is PaneNode -> node.panes.firstOrNull()?.let { getActiveLeaf(it) }
        }
    }

    companion object {
        /**
         * Default animation resolver providing standard slide animations.
         */
        val DefaultAnimationResolver = AnimationResolver { from, to, transitionType ->
            when (transitionType) {
                TransitionType.PUSH -> SurfaceAnimationSpec(
                    enter = androidx.compose.animation.slideInHorizontally { it } + 
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutHorizontally { -it / 3 } + 
                           androidx.compose.animation.fadeOut()
                )
                TransitionType.POP -> SurfaceAnimationSpec(
                    enter = androidx.compose.animation.slideInHorizontally { -it / 3 } + 
                            androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.slideOutHorizontally { it } + 
                           androidx.compose.animation.fadeOut()
                )
                TransitionType.TAB_SWITCH -> SurfaceAnimationSpec(
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                )
                TransitionType.PANE_SWITCH -> SurfaceAnimationSpec.None
                TransitionType.NONE -> SurfaceAnimationSpec.None
            }
        }
    }
}
```

---

## Implementation Steps

### Step 1: Create FlattenResult.kt

1. Create new file `FlattenResult.kt`
2. Define `TransitionType` enum
3. Define `AnimationPair` data class
4. Define `CachingHints` data class
5. Define `FlattenResult` data class

### Step 2: Create TreeFlattener Base

1. Create `TreeFlattener.kt` file
2. Define `ContentResolver` interface
3. Define `AnimationResolver` interface
4. Create `FlattenContext` and `FlattenAccumulator` internal classes

### Step 3: Implement Screen Flattening

1. Implement `flattenScreen()` method
2. Output single surface with `SINGLE_SCREEN` mode
3. Populate `previousSurfaceId` from context
4. Add animation pair if previous exists

### Step 4: Implement Stack Flattening

1. Implement `flattenStack()` method
2. Track active child (top of stack)
3. Track previous child for animations
4. Output `STACK_CONTENT` mode for screens in stack
5. Populate `previousSurfaceId` with previous child's key

### Step 5: Add Helper Methods

1. Implement `detectTransitionType()`
2. Implement `findPreviousSiblingId()`
3. Implement `getActivePath()` and `getActiveLeaf()`

### Step 6: Add Placeholder Methods

1. Add placeholder `flattenTab()` (for RENDER-002B)
2. Add placeholder `flattenPane()` (for RENDER-002C)

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/FlattenResult.kt` | Create | FlattenResult, AnimationPair, CachingHints, TransitionType |
| `quo-vadis-core/.../compose/TreeFlattener.kt` | Create | TreeFlattener class with Screen/Stack flattening |
| `quo-vadis-core/.../compose/RenderableSurface.kt` | Reference | Uses RenderableSurface from RENDER-001 |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| CORE-001 (NavNode Hierarchy) | Hard | Must complete first |
| RENDER-001 (RenderableSurface) | Hard | Must complete first |

---

## Acceptance Criteria

- [ ] `FlattenResult` data class defined with surfaces, animationPairs, cachingHints
- [ ] `AnimationPair` data class with currentId, previousId, transitionType
- [ ] `CachingHints` data class with caching flags and ID sets
- [ ] `TransitionType` enum with PUSH, POP, TAB_SWITCH, PANE_SWITCH, NONE
- [ ] `TreeFlattener` class with `flattenState()` entry point
- [ ] `ContentResolver` interface for resolving node content
- [ ] `AnimationResolver` interface for resolving animations
- [ ] `flattenScreen()` outputs single surface with `SINGLE_SCREEN` mode
- [ ] `flattenStack()` outputs surfaces with `STACK_CONTENT` mode
- [ ] `flattenStack()` correctly tracks `previousSurfaceId` for top item
- [ ] `AnimationPair` populated for stack push/pop transitions
- [ ] Placeholder methods for Tab and Pane (stubs for RENDER-002B/C)
- [ ] Default animation resolver with slide animations for push/pop
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for Screen flattening
- [ ] Unit tests for Stack flattening
- [ ] Unit tests for AnimationPair generation
- [ ] Code compiles on all target platforms

---

## Testing Notes

```kotlin
class TreeFlattenerScreenStackTest {

    private val mockContentResolver = TreeFlattener.ContentResolver { {} }
    private val flattener = TreeFlattener(mockContentResolver)

    @Test
    fun `flattenScreen produces single SINGLE_SCREEN surface`() {
        val screen = ScreenNode("s1", null, mockDestination)
        
        val result = flattener.flattenState(screen)
        
        assertEquals(1, result.surfaces.size)
        assertEquals("s1", result.surfaces[0].id)
        assertEquals(SurfaceRenderingMode.SINGLE_SCREEN, result.surfaces[0].renderingMode)
        assertNull(result.surfaces[0].previousSurfaceId)
    }

    @Test
    fun `flattenStack produces STACK_CONTENT surface for top item`() {
        val screen1 = ScreenNode("s1", "stack", mockDestination)
        val screen2 = ScreenNode("s2", "stack", mockDestination)
        val stack = StackNode("stack", null, listOf(screen1, screen2))
        
        val result = flattener.flattenState(stack)
        
        assertEquals(1, result.surfaces.size)
        assertEquals("s2", result.surfaces[0].id)
        assertEquals(SurfaceRenderingMode.STACK_CONTENT, result.surfaces[0].renderingMode)
    }

    @Test
    fun `flattenStack populates previousSurfaceId for animation`() {
        val screen1 = ScreenNode("s1", "stack", mockDestination)
        val screen2 = ScreenNode("s2", "stack", mockDestination)
        val stack = StackNode("stack", null, listOf(screen1, screen2))
        
        val result = flattener.flattenState(stack)
        
        assertEquals("s1", result.surfaces[0].previousSurfaceId)
    }

    @Test
    fun `flattenStack generates AnimationPair for transitions`() {
        val screen1 = ScreenNode("s1", "stack", mockDestination)
        val screen2 = ScreenNode("s2", "stack", mockDestination)
        val stack = StackNode("stack", null, listOf(screen1, screen2))
        
        val result = flattener.flattenState(stack)
        
        assertEquals(1, result.animationPairs.size)
        assertEquals("s2", result.animationPairs[0].currentId)
        assertEquals("s1", result.animationPairs[0].previousId)
        assertEquals(TransitionType.PUSH, result.animationPairs[0].transitionType)
    }

    @Test
    fun `flattenStack with single item has no previousSurfaceId`() {
        val screen = ScreenNode("s1", "stack", mockDestination)
        val stack = StackNode("stack", null, listOf(screen))
        
        val result = flattener.flattenState(stack)
        
        assertEquals(1, result.surfaces.size)
        assertNull(result.surfaces[0].previousSurfaceId)
        assertTrue(result.animationPairs.isEmpty())
    }

    @Test
    fun `flattenStack with empty children returns empty result`() {
        val stack = StackNode("stack", null, emptyList())
        
        val result = flattener.flattenState(stack)
        
        assertTrue(result.surfaces.isEmpty())
        assertTrue(result.animationPairs.isEmpty())
    }

    @Test
    fun `FlattenResult sortedSurfaces returns surfaces by zOrder`() {
        val result = FlattenResult(
            surfaces = listOf(
                mockSurface("a", zOrder = 200),
                mockSurface("b", zOrder = 100),
                mockSurface("c", zOrder = 150)
            ),
            animationPairs = emptyList(),
            cachingHints = CachingHints.Default
        )
        
        val sorted = result.sortedSurfaces
        
        assertEquals("b", sorted[0].id)
        assertEquals("c", sorted[1].id)
        assertEquals("a", sorted[2].id)
    }

    @Test
    fun `FlattenResult findAnimationPair returns correct pair`() {
        val result = FlattenResult(
            surfaces = emptyList(),
            animationPairs = listOf(
                AnimationPair("current", "previous", TransitionType.PUSH)
            ),
            cachingHints = CachingHints.Default
        )
        
        val pair = result.findAnimationPair("current")
        
        assertNotNull(pair)
        assertEquals("previous", pair.previousId)
    }

    @Test
    fun `TransitionType enum has all required values`() {
        val types = TransitionType.values()
        
        assertTrue(types.contains(TransitionType.PUSH))
        assertTrue(types.contains(TransitionType.POP))
        assertTrue(types.contains(TransitionType.TAB_SWITCH))
        assertTrue(types.contains(TransitionType.PANE_SWITCH))
        assertTrue(types.contains(TransitionType.NONE))
    }
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-001](./RENDER-001-renderable-surface.md) - RenderableSurface definition
- *This task replaced the original combined RENDER-002 task*
- [RENDER-002B](./RENDER-002B-tab-flatten.md) - TabNode flattening (next task)
- [RENDER-002C](./RENDER-002C-pane-flatten.md) - PaneNode flattening
- [RENDER-004](./RENDER-004-quovadis-host.md) - Consumer of flattened surfaces
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions

````
