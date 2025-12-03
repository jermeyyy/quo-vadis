# RENDER-001: Define RenderableSurface Data Class

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-001 |
| **Task Name** | Define RenderableSurface Data Class |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Low |
| **Estimated Time** | 1 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | RENDER-002, RENDER-004 |

---

## Overview

The `RenderableSurface` is the intermediate representation (IR) between the abstract `NavNode` tree and the actual Compose UI rendering. It represents a single "layer" in the flattened rendering stack, containing all information needed to position and animate that layer.

### Purpose in the Rendering Pipeline

```
NavNode Tree → TreeFlattener → List<RenderableSurface> → QuoVadisHost → Compose UI
     │                              │                           │
     │                              │                           └── Applies zIndex, animations
     │                              └── Ordered list of surfaces to render
     └── Abstract navigation state
```

The `RenderableSurface` serves as a decoupling layer that:

1. **Flattens hierarchy** - Converts recursive tree structure into a linear list
2. **Carries rendering metadata** - zIndex, transition state, animation specs
3. **Enables diffing** - Old vs new surface lists for enter/exit animations
4. **Simplifies renderer** - QuoVadisHost only needs to iterate surfaces

### Design Rationale

| Concern | Handled By |
|---------|------------|
| **Layering order** | `zOrder: Int` field |
| **Animation state** | `transitionState: SurfaceTransitionState` |
| **Content identity** | `id: String` (matches NavNode key) |
| **Render function** | `content: @Composable () -> Unit` |
| **Type information** | `nodeType: SurfaceNodeType` |

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/RenderableSurface.kt
```

---

## Implementation

### Core Data Structures

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Represents the type of NavNode that produced this surface.
 * 
 * Used for:
 * - Selecting appropriate default animations
 * - Debugging and inspection
 * - Conditional rendering logic
 */
enum class SurfaceNodeType {
    /** From a ScreenNode - leaf destination */
    SCREEN,
    
    /** From a StackNode - navigation container */
    STACK,
    
    /** From a TabNode - parallel tab container */
    TAB,
    
    /** From a PaneNode - side-by-side pane container */
    PANE
}

/**
 * Represents the transition state of a surface during navigation.
 * 
 * The transition state determines how the surface should be animated:
 * - [Visible] surfaces are fully shown without animation
 * - [Entering] surfaces are animating into view
 * - [Exiting] surfaces are animating out of view
 * - [Hidden] surfaces should not be rendered
 */
@Immutable
sealed interface SurfaceTransitionState {
    
    /**
     * Surface is fully visible and not animating.
     */
    data object Visible : SurfaceTransitionState
    
    /**
     * Surface is animating into view.
     * 
     * @property progress Animation progress from 0.0 (start) to 1.0 (complete)
     * @property isPredictive True if driven by predictive back gesture
     */
    data class Entering(
        val progress: Float = 0f,
        val isPredictive: Boolean = false
    ) : SurfaceTransitionState
    
    /**
     * Surface is animating out of view.
     * 
     * @property progress Animation progress from 0.0 (start) to 1.0 (complete)
     * @property isPredictive True if driven by predictive back gesture
     */
    data class Exiting(
        val progress: Float = 0f,
        val isPredictive: Boolean = false
    ) : SurfaceTransitionState
    
    /**
     * Surface should not be rendered.
     * Used for surfaces that have completed exit animations.
     */
    data object Hidden : SurfaceTransitionState
}

/**
 * Animation specification for a surface transition.
 * 
 * Encapsulates the enter and exit animations to apply when this surface
 * transitions in or out of the visible state.
 */
@Immutable
data class SurfaceAnimationSpec(
    /**
     * Animation to play when this surface enters the viewport.
     */
    val enter: EnterTransition,
    
    /**
     * Animation to play when this surface exits the viewport.
     */
    val exit: ExitTransition
) {
    companion object {
        /**
         * No animation - instant appear/disappear.
         */
        val None = SurfaceAnimationSpec(
            enter = EnterTransition.None,
            exit = ExitTransition.None
        )
    }
}

/**
 * Intermediate representation of a renderable layer in the navigation UI.
 * 
 * A `RenderableSurface` is produced by flattening the NavNode tree and represents
 * a single composable layer that should be rendered. The [QuoVadisHost] iterates
 * over a list of these surfaces and renders each one with appropriate z-ordering
 * and animations.
 * 
 * ## Lifecycle
 * 
 * 1. **Creation**: TreeFlattener converts NavNode tree to List<RenderableSurface>
 * 2. **Diffing**: Old vs new lists determine which surfaces enter/exit
 * 3. **Animation**: TransitionState drives enter/exit animations
 * 4. **Rendering**: QuoVadisHost renders each surface with zIndex modifier
 * 5. **Cleanup**: Exited surfaces are removed after animation completes
 * 
 * ## Immutability
 * 
 * RenderableSurface is immutable. To update a surface (e.g., change transition state),
 * create a new instance with the modified properties.
 * 
 * @property id Unique identifier, typically matches the NavNode.key
 * @property zOrder Rendering order (higher = on top). Based on tree depth.
 * @property nodeType Type of NavNode that produced this surface
 * @property transitionState Current animation state of this surface
 * @property animationSpec Enter/exit animations to apply
 * @property content The composable content to render for this surface
 */
@Immutable
data class RenderableSurface(
    val id: String,
    val zOrder: Int,
    val nodeType: SurfaceNodeType,
    val transitionState: SurfaceTransitionState = SurfaceTransitionState.Visible,
    val animationSpec: SurfaceAnimationSpec = SurfaceAnimationSpec.None,
    val content: @Composable () -> Unit
) {
    
    /**
     * Returns true if this surface should currently be rendered.
     * 
     * A surface should be rendered if it's visible, entering, or exiting.
     * Hidden surfaces should not be composed.
     */
    val shouldRender: Boolean
        get() = transitionState !is SurfaceTransitionState.Hidden
    
    /**
     * Returns true if this surface is currently animating.
     */
    val isAnimating: Boolean
        get() = transitionState is SurfaceTransitionState.Entering ||
                transitionState is SurfaceTransitionState.Exiting
    
    /**
     * Returns true if this surface is being driven by a predictive gesture.
     */
    val isPredictive: Boolean
        get() = when (val state = transitionState) {
            is SurfaceTransitionState.Entering -> state.isPredictive
            is SurfaceTransitionState.Exiting -> state.isPredictive
            else -> false
        }
    
    /**
     * Returns the current animation progress, or null if not animating.
     */
    val animationProgress: Float?
        get() = when (val state = transitionState) {
            is SurfaceTransitionState.Entering -> state.progress
            is SurfaceTransitionState.Exiting -> state.progress
            else -> null
        }
    
    /**
     * Creates a copy with updated transition state.
     */
    fun withTransitionState(newState: SurfaceTransitionState): RenderableSurface {
        return copy(transitionState = newState)
    }
    
    /**
     * Creates a copy with updated animation progress.
     * Only applicable to Entering or Exiting states.
     */
    fun withProgress(progress: Float): RenderableSurface {
        val newState = when (val state = transitionState) {
            is SurfaceTransitionState.Entering -> state.copy(progress = progress)
            is SurfaceTransitionState.Exiting -> state.copy(progress = progress)
            else -> transitionState
        }
        return copy(transitionState = newState)
    }
}
```

### Builder Pattern (Optional)

```kotlin
/**
 * Builder for creating RenderableSurface instances with a fluent API.
 * 
 * Useful when constructing surfaces in the TreeFlattener where
 * properties are determined incrementally.
 * 
 * ## Example
 * 
 * ```kotlin
 * val surface = RenderableSurfaceBuilder(screenNode.key)
 *     .zOrder(depth * 100)
 *     .nodeType(SurfaceNodeType.SCREEN)
 *     .transitionState(SurfaceTransitionState.Entering(0f))
 *     .animation(slideIn(), slideOut())
 *     .content { ScreenContent(screenNode.destination) }
 *     .build()
 * ```
 */
class RenderableSurfaceBuilder(private val id: String) {
    private var zOrder: Int = 0
    private var nodeType: SurfaceNodeType = SurfaceNodeType.SCREEN
    private var transitionState: SurfaceTransitionState = SurfaceTransitionState.Visible
    private var animationSpec: SurfaceAnimationSpec = SurfaceAnimationSpec.None
    private var content: (@Composable () -> Unit)? = null
    
    fun zOrder(order: Int) = apply { this.zOrder = order }
    
    fun nodeType(type: SurfaceNodeType) = apply { this.nodeType = type }
    
    fun transitionState(state: SurfaceTransitionState) = apply { this.transitionState = state }
    
    fun animation(enter: EnterTransition, exit: ExitTransition) = apply {
        this.animationSpec = SurfaceAnimationSpec(enter, exit)
    }
    
    fun animation(spec: SurfaceAnimationSpec) = apply { this.animationSpec = spec }
    
    fun content(block: @Composable () -> Unit) = apply { this.content = block }
    
    fun build(): RenderableSurface {
        requireNotNull(content) { "Content must be set before building RenderableSurface" }
        return RenderableSurface(
            id = id,
            zOrder = zOrder,
            nodeType = nodeType,
            transitionState = transitionState,
            animationSpec = animationSpec,
            content = content!!
        )
    }
}

/**
 * DSL function to create a RenderableSurface.
 */
inline fun renderableSurface(
    id: String,
    block: RenderableSurfaceBuilder.() -> Unit
): RenderableSurface {
    return RenderableSurfaceBuilder(id).apply(block).build()
}
```

### Extension Functions

```kotlin
/**
 * Extension functions for working with lists of RenderableSurface.
 */

/**
 * Returns surfaces sorted by zOrder for proper rendering.
 */
fun List<RenderableSurface>.sortedByZOrder(): List<RenderableSurface> {
    return sortedBy { it.zOrder }
}

/**
 * Returns only surfaces that should be rendered.
 */
fun List<RenderableSurface>.renderable(): List<RenderableSurface> {
    return filter { it.shouldRender }
}

/**
 * Finds a surface by its ID.
 */
fun List<RenderableSurface>.findById(id: String): RenderableSurface? {
    return find { it.id == id }
}

/**
 * Returns surfaces that are currently animating.
 */
fun List<RenderableSurface>.animating(): List<RenderableSurface> {
    return filter { it.isAnimating }
}

/**
 * Diffs two surface lists to identify surfaces that need enter/exit animations.
 * 
 * @param newSurfaces The new list of surfaces after navigation
 * @return Pair of (entering surfaces, exiting surfaces)
 */
fun List<RenderableSurface>.diffWith(
    newSurfaces: List<RenderableSurface>
): Pair<List<RenderableSurface>, List<RenderableSurface>> {
    val oldIds = this.map { it.id }.toSet()
    val newIds = newSurfaces.map { it.id }.toSet()
    
    val entering = newSurfaces.filter { it.id !in oldIds }
    val exiting = this.filter { it.id !in newIds }
    
    return entering to exiting
}
```

---

## Implementation Steps

### Step 1: Create File Structure

1. Create `RenderableSurface.kt` in the compose package
2. Add necessary imports for Compose animation APIs

### Step 2: Implement Core Types

1. Define `SurfaceNodeType` enum
2. Define `SurfaceTransitionState` sealed interface
3. Define `SurfaceAnimationSpec` data class
4. Define main `RenderableSurface` data class

### Step 3: Add Helper Functions

1. Implement computed properties (`shouldRender`, `isAnimating`, etc.)
2. Add `withTransitionState()` and `withProgress()` methods
3. Create builder class (optional)

### Step 4: Add Extension Functions

1. Implement list sorting/filtering extensions
2. Add `diffWith()` for surface list diffing

### Step 5: Documentation

1. Add comprehensive KDoc to all public APIs
2. Include usage examples in documentation

---

## Usage Examples

### Creating a Surface for a Screen

```kotlin
val surface = RenderableSurface(
    id = screenNode.key,
    zOrder = 100, // Base z-order for screens
    nodeType = SurfaceNodeType.SCREEN,
    transitionState = SurfaceTransitionState.Visible,
    content = {
        ScreenContent(
            destination = screenNode.destination,
            sharedTransitionScope = sharedTransitionScope
        )
    }
)
```

### Updating Animation Progress

```kotlin
// During predictive back gesture
val updatedSurface = surface.withProgress(gestureProgress)

// When gesture completes
val exitingSurface = surface.withTransitionState(
    SurfaceTransitionState.Exiting(progress = 0f, isPredictive = false)
)
```

### Rendering Surfaces in QuoVadisHost

```kotlin
@Composable
fun QuoVadisHost(surfaces: List<RenderableSurface>) {
    Box {
        surfaces
            .sortedByZOrder()
            .renderable()
            .forEach { surface ->
                Box(
                    modifier = Modifier.zIndex(surface.zOrder.toFloat())
                ) {
                    AnimatedVisibility(
                        visible = surface.transitionState != SurfaceTransitionState.Exiting,
                        enter = surface.animationSpec.enter,
                        exit = surface.animationSpec.exit
                    ) {
                        surface.content()
                    }
                }
            }
    }
}
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/RenderableSurface.kt` | Create | New file with all types |

---

## Dependencies

This task has **no dependencies** and can be started immediately.

---

## Acceptance Criteria

- [ ] `SurfaceNodeType` enum defined with SCREEN, STACK, TAB, PANE values
- [ ] `SurfaceTransitionState` sealed interface with Visible, Entering, Exiting, Hidden
- [ ] `SurfaceAnimationSpec` data class with enter/exit transitions
- [ ] `RenderableSurface` data class with all required properties
- [ ] `shouldRender`, `isAnimating`, `isPredictive` computed properties implemented
- [ ] `withTransitionState()` and `withProgress()` methods functional
- [ ] Builder pattern implemented (optional but recommended)
- [ ] Extension functions for list operations implemented
- [ ] `diffWith()` extension correctly identifies entering/exiting surfaces
- [ ] All types are `@Immutable` annotated where appropriate
- [ ] Comprehensive KDoc documentation on all public APIs
- [ ] Code compiles on all target platforms

---

## Testing Notes

Basic tests to verify during development:

```kotlin
@Test
fun `surface with Visible state should render`() {
    val surface = RenderableSurface(
        id = "test",
        zOrder = 0,
        nodeType = SurfaceNodeType.SCREEN,
        transitionState = SurfaceTransitionState.Visible,
        content = {}
    )
    assertTrue(surface.shouldRender)
    assertFalse(surface.isAnimating)
}

@Test
fun `surface with Hidden state should not render`() {
    val surface = RenderableSurface(
        id = "test",
        zOrder = 0,
        nodeType = SurfaceNodeType.SCREEN,
        transitionState = SurfaceTransitionState.Hidden,
        content = {}
    )
    assertFalse(surface.shouldRender)
}

@Test
fun `withProgress updates Entering state correctly`() {
    val surface = RenderableSurface(
        id = "test",
        zOrder = 0,
        nodeType = SurfaceNodeType.SCREEN,
        transitionState = SurfaceTransitionState.Entering(0f),
        content = {}
    )
    
    val updated = surface.withProgress(0.5f)
    
    assertEquals(0.5f, updated.animationProgress)
}

@Test
fun `diffWith identifies entering and exiting surfaces`() {
    val surface1 = RenderableSurface("a", 0, SurfaceNodeType.SCREEN) {}
    val surface2 = RenderableSurface("b", 1, SurfaceNodeType.SCREEN) {}
    val surface3 = RenderableSurface("c", 2, SurfaceNodeType.SCREEN) {}
    
    val oldList = listOf(surface1, surface2)
    val newList = listOf(surface2, surface3)
    
    val (entering, exiting) = oldList.diffWith(newList)
    
    assertEquals(listOf(surface3), entering)
    assertEquals(listOf(surface1), exiting)
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-002](./RENDER-002-flatten-algorithm.md) - TreeFlattener that produces these surfaces
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost that consumes these surfaces
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.2 "The Single Rendering Component"
