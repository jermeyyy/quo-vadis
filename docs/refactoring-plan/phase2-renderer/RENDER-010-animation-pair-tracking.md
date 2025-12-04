# RENDER-010: Animation Pair Tracking for Transitions

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-010 |
| **Task Name** | Animation Pair Tracking for Transitions |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002A, RENDER-003 |
| **Blocked By** | RENDER-002A, RENDER-003 |
| **Blocks** | RENDER-005 |

---

## Overview

Explicitly track and expose current/previous screen pairs for animations, shared element transitions, and predictive back. The renderer needs direct access to composables of BOTH current AND previous screens simultaneously.

### Problem Statement

For animations and shared element transitions to work, the renderer must have access to:

1. **The current screen's composable (entering)**
2. **The previous screen's composable (exiting)**

Both must be available at the same time during the transition.

### Why This Matters

```
Navigation Event → State Change → Animation Required
                                        │
                    ┌───────────────────┴───────────────────┐
                    │                                       │
              Current Screen                         Previous Screen
              (entering view)                        (exiting view)
                    │                                       │
                    └───────────────────┬───────────────────┘
                                        │
                              Animation Pair Created
                                        │
                    ┌───────────────────┴───────────────────┐
                    │                   │                   │
              Enter Animation    Shared Elements     Exit Animation
```

Without animation pair tracking:
- Shared element transitions cannot reference both screens
- Predictive back cannot show both screens during the gesture
- Enter/exit animations cannot be coordinated
- The exiting screen may be disposed before animation completes

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/AnimationPairTracker.kt
```

---

## Implementation

### Core Data Structures

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Immutable

/**
 * Describes the type of navigation transition occurring.
 * 
 * Used to select appropriate animations and behaviors:
 * - Stack transitions (PUSH/POP) typically use slide animations
 * - Tab switches may use crossfade or no animation
 * - Pane switches may animate the active pane indicator
 */
enum class TransitionType {
    /** New screen pushed onto stack - slide in from right */
    PUSH,
    
    /** Screen popped from stack - slide out to right */
    POP,
    
    /** Tab changed - crossfade or instant switch */
    TAB_SWITCH,
    
    /** Active pane changed in multi-pane layout */
    PANE_SWITCH,
    
    /** Initial render or instant navigation - no animation */
    NONE
}

/**
 * Represents a pair of surfaces that are animating together during a transition.
 * 
 * An animation pair captures the relationship between the entering (current)
 * and exiting (previous) surfaces during navigation. This pairing is essential for:
 * 
 * 1. **Coordinated animations** - Enter and exit animations run together
 * 2. **Shared element transitions** - Elements can match across the pair
 * 3. **Predictive back** - Both surfaces move together with gesture
 * 
 * ## Lifecycle
 * 
 * 1. Navigation event triggers state change
 * 2. AnimationPairTracker detects the change
 * 3. AnimationPair created with both surface IDs
 * 4. Renderer uses pair to animate both surfaces
 * 5. Animation completes, pair is disposed
 * 
 * @property currentId ID of the entering/current surface (always present)
 * @property previousId ID of the exiting/previous surface (null for initial render)
 * @property transitionType The type of navigation that triggered this pair
 * @property currentSurface The entering RenderableSurface
 * @property previousSurface The exiting RenderableSurface (null if none)
 * @property containerId ID of the container where this transition occurs
 */
@Immutable
data class AnimationPair(
    val currentId: String,
    val previousId: String?,
    val transitionType: TransitionType,
    val currentSurface: RenderableSurface,
    val previousSurface: RenderableSurface?,
    val containerId: String? = null
) {
    /**
     * Returns true if this pair has both entering and exiting surfaces.
     * Initial renders only have a current surface.
     */
    val hasBothSurfaces: Boolean
        get() = previousSurface != null
    
    /**
     * Returns true if this transition should animate.
     * NONE transitions don't animate.
     */
    val shouldAnimate: Boolean
        get() = transitionType != TransitionType.NONE && hasBothSurfaces
    
    /**
     * Returns true if this is a stack-based transition (PUSH or POP).
     */
    val isStackTransition: Boolean
        get() = transitionType == TransitionType.PUSH || transitionType == TransitionType.POP
    
    /**
     * Returns true if this transition can support shared elements.
     * Only stack transitions currently support shared elements.
     */
    val supportsSharedElements: Boolean
        get() = isStackTransition && hasBothSurfaces
}
```

### FlattenResult Update

```kotlin
/**
 * Result of flattening the NavNode tree into renderable surfaces.
 * 
 * Contains all information needed by the renderer to display the current
 * navigation state, including animation pairs for transitions.
 * 
 * @property surfaces Ordered list of surfaces to render
 * @property animationPairs Pairs of surfaces that are transitioning together
 * @property cachingHints Hints for the caching system
 */
@Immutable
data class FlattenResult(
    val surfaces: List<RenderableSurface>,
    val animationPairs: List<AnimationPair>,
    val cachingHints: CachingHints
) {
    /**
     * Returns surfaces that are part of an active animation pair.
     */
    val animatingSurfaces: Set<String>
        get() = animationPairs.flatMap { pair ->
            listOfNotNull(pair.currentId, pair.previousId)
        }.toSet()
    
    /**
     * Finds the animation pair containing a specific surface.
     */
    fun findPairForSurface(surfaceId: String): AnimationPair? {
        return animationPairs.find { 
            it.currentId == surfaceId || it.previousId == surfaceId 
        }
    }
    
    /**
     * Returns animation pairs that support shared elements.
     */
    val sharedElementPairs: List<AnimationPair>
        get() = animationPairs.filter { it.supportsSharedElements }
}
```

### AnimationPairTracker

```kotlin
/**
 * Tracks navigation state changes to produce animation pairs.
 * 
 * The tracker maintains a history of the previous navigation state and surfaces,
 * comparing them to the new state to detect and classify transitions.
 * 
 * ## Thread Safety
 * 
 * This class is not thread-safe and should only be accessed from the main thread
 * (composition context).
 * 
 * ## Usage
 * 
 * ```kotlin
 * val tracker = AnimationPairTracker()
 * 
 * // On each navigation state change
 * val pairs = tracker.trackTransition(
 *     newSurfaces = flattenedSurfaces,
 *     transitionState = currentTransitionState
 * )
 * ```
 */
class AnimationPairTracker {
    
    private var lastSurfaces: Map<String, RenderableSurface> = emptyMap()
    private var lastSurfacesByContainer: Map<String?, String> = emptyMap()
    
    /**
     * Analyzes the difference between old and new surfaces to produce animation pairs.
     * 
     * @param newSurfaces The new list of surfaces after navigation
     * @param transitionState Optional transition state for predictive gestures
     * @return List of animation pairs representing active transitions
     */
    fun trackTransition(
        newSurfaces: List<RenderableSurface>,
        transitionState: TransitionState? = null
    ): List<AnimationPair> {
        val pairs = mutableListOf<AnimationPair>()
        val newSurfacesMap = newSurfaces.associateBy { it.id }
        val newIds = newSurfacesMap.keys
        val oldIds = lastSurfaces.keys
        
        // Detect entering surfaces (in new but not in old)
        val enteringIds = newIds - oldIds
        
        // Detect exiting surfaces (in old but not in new)
        val exitingIds = oldIds - newIds
        
        // Process entering surfaces
        for (enterId in enteringIds) {
            val enteringSurface = newSurfacesMap[enterId] ?: continue
            
            // Find the corresponding exiting surface in the same container
            val exitingSurface = findExitingSurfaceForContainer(
                enteringSurface = enteringSurface,
                exitingIds = exitingIds
            )
            
            val transitionType = determineTransitionType(
                enteringSurface = enteringSurface,
                exitingSurface = exitingSurface,
                transitionState = transitionState
            )
            
            pairs.add(
                AnimationPair(
                    currentId = enterId,
                    previousId = exitingSurface?.id,
                    transitionType = transitionType,
                    currentSurface = enteringSurface,
                    previousSurface = exitingSurface,
                    containerId = enteringSurface.parentWrapperId
                )
            )
        }
        
        // Handle exiting surfaces without a corresponding entering surface
        // (e.g., when navigating away from a tab navigator entirely)
        val unmatchedExiting = exitingIds - pairs.mapNotNull { it.previousId }.toSet()
        for (exitId in unmatchedExiting) {
            val exitingSurface = lastSurfaces[exitId] ?: continue
            
            // Create a pair with no entering surface
            // This represents a surface that is just exiting
            pairs.add(
                AnimationPair(
                    currentId = exitId, // Using exit ID as current for tracking
                    previousId = exitId,
                    transitionType = TransitionType.POP,
                    currentSurface = exitingSurface.withTransitionState(
                        SurfaceTransitionState.Exiting()
                    ),
                    previousSurface = exitingSurface,
                    containerId = exitingSurface.parentWrapperId
                )
            )
        }
        
        // Update state for next comparison
        lastSurfaces = newSurfacesMap
        updateContainerTracking(newSurfaces)
        
        return pairs
    }
    
    /**
     * Finds an exiting surface that belongs to the same container as the entering surface.
     */
    private fun findExitingSurfaceForContainer(
        enteringSurface: RenderableSurface,
        exitingIds: Set<String>
    ): RenderableSurface? {
        // First, try to find by previousSurfaceId if set
        enteringSurface.previousSurfaceId?.let { prevId ->
            lastSurfaces[prevId]?.let { return it }
        }
        
        // Then, try to find by matching container
        val containerId = enteringSurface.parentWrapperId
        for (exitId in exitingIds) {
            val exitingSurface = lastSurfaces[exitId] ?: continue
            if (exitingSurface.parentWrapperId == containerId) {
                return exitingSurface
            }
        }
        
        // Finally, try to match by node type for top-level transitions
        if (containerId == null) {
            for (exitId in exitingIds) {
                val exitingSurface = lastSurfaces[exitId] ?: continue
                if (exitingSurface.parentWrapperId == null &&
                    exitingSurface.nodeType == enteringSurface.nodeType) {
                    return exitingSurface
                }
            }
        }
        
        return null
    }
    
    /**
     * Determines the transition type based on surface characteristics.
     */
    private fun determineTransitionType(
        enteringSurface: RenderableSurface,
        exitingSurface: RenderableSurface?,
        transitionState: TransitionState?
    ): TransitionType {
        // If we have explicit transition state, use it
        transitionState?.let {
            return when (it) {
                is TransitionState.Push -> TransitionType.PUSH
                is TransitionState.Pop -> TransitionType.POP
                is TransitionState.TabSwitch -> TransitionType.TAB_SWITCH
                is TransitionState.PaneSwitch -> TransitionType.PANE_SWITCH
                else -> TransitionType.NONE
            }
        }
        
        // If no previous surface, this is initial render
        if (exitingSurface == null) {
            return TransitionType.NONE
        }
        
        // Infer from rendering mode
        return when (enteringSurface.renderingMode) {
            SurfaceRenderingMode.STACK_CONTENT -> {
                // Stack transitions are PUSH unless we detect a POP
                // (POP detection would need additional context)
                TransitionType.PUSH
            }
            SurfaceRenderingMode.TAB_CONTENT -> TransitionType.TAB_SWITCH
            SurfaceRenderingMode.PANE_CONTENT -> TransitionType.PANE_SWITCH
            else -> TransitionType.NONE
        }
    }
    
    /**
     * Updates container tracking for future comparisons.
     */
    private fun updateContainerTracking(surfaces: List<RenderableSurface>) {
        lastSurfacesByContainer = surfaces
            .filter { it.renderingMode.isContentMode() }
            .associate { it.parentWrapperId to it.id }
    }
    
    /**
     * Resets the tracker state. Call when navigation is fully reset.
     */
    fun reset() {
        lastSurfaces = emptyMap()
        lastSurfacesByContainer = emptyMap()
    }
}

/**
 * Extension to check if a rendering mode represents content (vs wrapper).
 */
private fun SurfaceRenderingMode.isContentMode(): Boolean {
    return this == SurfaceRenderingMode.STACK_CONTENT ||
           this == SurfaceRenderingMode.TAB_CONTENT ||
           this == SurfaceRenderingMode.PANE_CONTENT ||
           this == SurfaceRenderingMode.SINGLE_SCREEN
}
```

### TransitionState Types

```kotlin
/**
 * Represents the current navigation transition state.
 * 
 * Used to provide explicit transition type information to the tracker
 * when it cannot be inferred from surface changes alone.
 */
sealed interface TransitionState {
    /** A new screen is being pushed onto a stack */
    data class Push(val targetId: String) : TransitionState
    
    /** A screen is being popped from a stack */
    data class Pop(val sourceId: String) : TransitionState
    
    /** A tab is being switched */
    data class TabSwitch(val fromTab: String, val toTab: String) : TransitionState
    
    /** A pane is being switched (in single-pane mode) */
    data class PaneSwitch(val fromPane: String, val toPane: String) : TransitionState
    
    /** No transition (instant navigation) */
    data object None : TransitionState
}
```

---

## Usage Examples

### Basic Usage in QuoVadisHost

```kotlin
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val navState by navigator.state.collectAsState()
    val animationPairTracker = remember { AnimationPairTracker() }
    
    // Flatten the navigation tree
    val flattenResult = remember(navState) {
        TreeFlattener.flatten(navState)
    }
    
    // Track transitions to get animation pairs
    val animationPairs = remember(flattenResult) {
        animationPairTracker.trackTransition(
            newSurfaces = flattenResult.surfaces,
            transitionState = navigator.currentTransitionState
        )
    }
    
    // Render with SharedTransitionLayout for shared elements
    SharedTransitionLayout {
        Box(modifier = modifier) {
            // Render animation pairs
            animationPairs.forEach { pair ->
                AnimatedTransitionContainer(
                    pair = pair,
                    sharedTransitionScope = this@SharedTransitionLayout
                )
            }
            
            // Render non-animating surfaces
            flattenResult.surfaces
                .filterNot { it.id in flattenResult.animatingSurfaces }
                .forEach { surface ->
                    StaticSurfaceContainer(surface = surface)
                }
        }
    }
}
```

### AnimatedTransitionContainer

```kotlin
@Composable
private fun SharedTransitionScope.AnimatedTransitionContainer(
    pair: AnimationPair,
    modifier: Modifier = Modifier
) {
    if (!pair.shouldAnimate) {
        // No animation needed, just render current
        Box(modifier = modifier.zIndex(pair.currentSurface.zOrder.toFloat())) {
            pair.currentSurface.content()
        }
        return
    }
    
    // Use AnimatedContent for coordinated enter/exit
    AnimatedContent(
        targetState = pair.currentId,
        modifier = modifier,
        transitionSpec = {
            getTransitionSpec(pair.transitionType)
        }
    ) { targetId ->
        val surface = if (targetId == pair.currentId) {
            pair.currentSurface
        } else {
            pair.previousSurface
        }
        
        surface?.let {
            Box(modifier = Modifier.zIndex(it.zOrder.toFloat())) {
                it.content()
            }
        }
    }
}

private fun AnimatedContentTransitionScope<String>.getTransitionSpec(
    type: TransitionType
): ContentTransform {
    return when (type) {
        TransitionType.PUSH -> {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start) togetherWith
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start)
        }
        TransitionType.POP -> {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End) togetherWith
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End)
        }
        TransitionType.TAB_SWITCH -> {
            fadeIn() togetherWith fadeOut()
        }
        TransitionType.PANE_SWITCH -> {
            fadeIn() togetherWith fadeOut()
        }
        TransitionType.NONE -> {
            EnterTransition.None togetherWith ExitTransition.None
        }
    }
}
```

### Integration with Predictive Back

```kotlin
@Composable
fun PredictiveBackHandler(
    pair: AnimationPair,
    onBack: () -> Unit
) {
    if (!pair.isStackTransition) return
    
    var progress by remember { mutableFloatStateOf(0f) }
    
    BackHandler(enabled = pair.hasBothSurfaces) {
        // Gesture-driven back
    }
    
    // Use animation pair to show both screens during gesture
    Box {
        // Previous screen (partially visible during gesture)
        pair.previousSurface?.let { previous ->
            Box(
                modifier = Modifier
                    .zIndex(0f)
                    .graphicsLayer {
                        translationX = -size.width * 0.3f * (1 - progress)
                    }
            ) {
                previous.content()
            }
        }
        
        // Current screen (sliding out with gesture)
        Box(
            modifier = Modifier
                .zIndex(1f)
                .graphicsLayer {
                    translationX = size.width * progress
                }
        ) {
            pair.currentSurface.content()
        }
    }
}
```

### Integration with Shared Elements

```kotlin
@Composable
fun SharedTransitionScope.ScreenWithSharedElements(
    pair: AnimationPair,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // Both screens can reference shared elements during transition
    Box {
        // The shared element API can now work because both screens
        // are composed within the same SharedTransitionLayout
        
        // Current screen
        pair.currentSurface.content()
        
        // Previous screen (if animating)
        if (pair.shouldAnimate) {
            pair.previousSurface?.content()
        }
    }
}
```

---

## Implementation Steps

### Step 1: Create TransitionType Enum

1. Create `AnimationPairTracker.kt` file
2. Define `TransitionType` enum with all transition types
3. Add documentation for each type

### Step 2: Create AnimationPair Data Class

1. Define `AnimationPair` data class with all properties
2. Add computed properties (`hasBothSurfaces`, `shouldAnimate`, etc.)
3. Ensure immutability with `@Immutable` annotation

### Step 3: Create TransitionState Sealed Interface

1. Define `TransitionState` sealed interface
2. Add all transition state variants
3. Document usage patterns

### Step 4: Implement AnimationPairTracker

1. Implement state tracking (previous surfaces)
2. Implement `trackTransition()` method
3. Implement `findExitingSurfaceForContainer()` helper
4. Implement `determineTransitionType()` helper
5. Add `reset()` method

### Step 5: Update FlattenResult

1. Add `animationPairs` property to `FlattenResult`
2. Add helper methods (`findPairForSurface`, `sharedElementPairs`)
3. Update `TreeFlattener` to populate animation pairs

### Step 6: Integration Testing

1. Test push transition pair creation
2. Test pop transition pair creation
3. Test tab switch pair creation
4. Test pane switch pair creation
5. Test initial render (no previous)

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/AnimationPairTracker.kt` | Create | New file with all types |
| `quo-vadis-core/.../compose/FlattenResult.kt` | Modify | Add animationPairs property |
| `quo-vadis-core/.../compose/TreeFlattener.kt` | Modify | Populate animation pairs |

---

## Acceptance Criteria

- [ ] `AnimationPair` data class defined with all properties
- [ ] `TransitionType` enum defined with PUSH, POP, TAB_SWITCH, PANE_SWITCH, NONE
- [ ] `TransitionState` sealed interface defined
- [ ] `AnimationPairTracker` class implemented
- [ ] `FlattenResult.animationPairs` property added
- [ ] Push transition creates pair (new surface, old surface)
- [ ] Pop transition creates pair (revealed surface, removed surface)
- [ ] Tab switch creates pair (new tab content, old tab content)
- [ ] Pane switch creates pair (new pane content, old pane content)
- [ ] Initial render creates pair with no previous surface
- [ ] Previous composable accessible during entire transition duration
- [ ] SharedTransitionLayout integration demonstrated
- [ ] PredictiveBack can use animation pairs for gesture-driven navigation
- [ ] Container tracking correctly matches entering/exiting surfaces
- [ ] Unit tests for all pair tracking scenarios
- [ ] All types are `@Immutable` annotated where appropriate
- [ ] Comprehensive KDoc documentation on all public APIs

---

## Testing Notes

```kotlin
@Test
fun `push creates animation pair with new and old surfaces`() {
    val tracker = AnimationPairTracker()
    
    // Initial state
    val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
    tracker.trackTransition(listOf(surface1))
    
    // Push new screen
    val surface2 = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT)
    val pairs = tracker.trackTransition(listOf(surface2))
    
    assertEquals(1, pairs.size)
    assertEquals("screen2", pairs[0].currentId)
    assertEquals("screen1", pairs[0].previousId)
    assertEquals(TransitionType.PUSH, pairs[0].transitionType)
}

@Test
fun `pop creates animation pair with revealed and removed surfaces`() {
    val tracker = AnimationPairTracker()
    
    // Two screens on stack
    val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
    val surface2 = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT)
    tracker.trackTransition(listOf(surface1, surface2))
    
    // Pop back to first screen
    val pairs = tracker.trackTransition(
        newSurfaces = listOf(surface1),
        transitionState = TransitionState.Pop("screen2")
    )
    
    assertEquals(1, pairs.size)
    assertEquals(TransitionType.POP, pairs[0].transitionType)
}

@Test
fun `tab switch creates pair for tab content`() {
    val tracker = AnimationPairTracker()
    
    // Tab A active
    val tabWrapper = createTestSurface("tabs", SurfaceRenderingMode.TAB_WRAPPER)
    val tabA = createTestSurface("tabA", SurfaceRenderingMode.TAB_CONTENT, parentWrapperId = "tabs")
    tracker.trackTransition(listOf(tabWrapper, tabA))
    
    // Switch to Tab B
    val tabB = createTestSurface("tabB", SurfaceRenderingMode.TAB_CONTENT, parentWrapperId = "tabs")
    val pairs = tracker.trackTransition(listOf(tabWrapper, tabB))
    
    // Wrapper should not create a pair (unchanged)
    // Only tab content should create a pair
    val contentPairs = pairs.filter { it.currentSurface.renderingMode == SurfaceRenderingMode.TAB_CONTENT }
    assertEquals(1, contentPairs.size)
    assertEquals("tabB", contentPairs[0].currentId)
    assertEquals("tabA", contentPairs[0].previousId)
    assertEquals(TransitionType.TAB_SWITCH, contentPairs[0].transitionType)
}

@Test
fun `initial render creates pair with no previous`() {
    val tracker = AnimationPairTracker()
    
    val surface = createTestSurface("screen1", SurfaceRenderingMode.SINGLE_SCREEN)
    val pairs = tracker.trackTransition(listOf(surface))
    
    assertEquals(1, pairs.size)
    assertEquals("screen1", pairs[0].currentId)
    assertNull(pairs[0].previousId)
    assertEquals(TransitionType.NONE, pairs[0].transitionType)
    assertFalse(pairs[0].shouldAnimate)
}

@Test
fun `animation pair supports shared elements for stack transitions`() {
    val pair = AnimationPair(
        currentId = "screen2",
        previousId = "screen1",
        transitionType = TransitionType.PUSH,
        currentSurface = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT),
        previousSurface = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
    )
    
    assertTrue(pair.supportsSharedElements)
    assertTrue(pair.isStackTransition)
    assertTrue(pair.shouldAnimate)
}

@Test
fun `tab switch does not support shared elements`() {
    val pair = AnimationPair(
        currentId = "tabB",
        previousId = "tabA",
        transitionType = TransitionType.TAB_SWITCH,
        currentSurface = createTestSurface("tabB", SurfaceRenderingMode.TAB_CONTENT),
        previousSurface = createTestSurface("tabA", SurfaceRenderingMode.TAB_CONTENT)
    )
    
    assertFalse(pair.supportsSharedElements)
    assertFalse(pair.isStackTransition)
    assertTrue(pair.shouldAnimate)
}

private fun createTestSurface(
    id: String,
    mode: SurfaceRenderingMode,
    parentWrapperId: String? = null
): RenderableSurface {
    return RenderableSurface(
        id = id,
        zOrder = 0,
        nodeType = when (mode) {
            SurfaceRenderingMode.TAB_WRAPPER, SurfaceRenderingMode.TAB_CONTENT -> SurfaceNodeType.TAB
            SurfaceRenderingMode.PANE_WRAPPER, SurfaceRenderingMode.PANE_CONTENT -> SurfaceNodeType.PANE
            SurfaceRenderingMode.STACK_CONTENT -> SurfaceNodeType.STACK
            else -> SurfaceNodeType.SCREEN
        },
        renderingMode = mode,
        content = {},
        parentWrapperId = parentWrapperId
    )
}
```

---

## Edge Cases

### Multiple Simultaneous Transitions

When multiple containers have transitions at the same time (e.g., tab switch while also pushing within a tab):

```kotlin
// The tracker produces multiple pairs:
// 1. Pair for tab switch (tabA -> tabB)
// 2. Pair for push within the new tab (if applicable)
```

### Nested Navigators

When a stack is inside a tab, and we switch tabs while the stack is animating:

- The tab switch pair takes precedence
- The inner stack transition may be canceled or completed instantly
- Container IDs help disambiguate which pair belongs to which navigator

### Rapid Navigation

When navigation happens faster than animations complete:

- The tracker always uses the most recent state
- Previous animation pairs may be superseded
- The renderer should handle cancellation gracefully

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-001](./RENDER-001-renderable-surface.md) - RenderableSurface data class
- [RENDER-002A](./RENDER-002A-tree-flattener-overview.md) - TreeFlattener that produces surfaces
- [RENDER-003](./RENDER-003-flatten-result.md) - FlattenResult structure
- [RENDER-005](./RENDER-005-shared-elements.md) - Shared element transitions
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Animation architecture
