package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Represents the type of NavNode that produced this surface.
 *
 * Used for:
 * - Selecting appropriate default animations
 * - Debugging and inspection
 * - Conditional rendering logic
 *
 * @deprecated This enum is part of the flattened rendering approach which will be removed
 *   in a future version. Use [RenderingMode.Hierarchical] instead.
 */
@Deprecated(
    message = "SurfaceNodeType is part of the flattened rendering approach which will be removed. " +
        "Use RenderingMode.Hierarchical instead.",
    level = DeprecationLevel.WARNING
)
public enum class SurfaceNodeType {
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
 * Represents the specific rendering mode for a surface.
 *
 * While [SurfaceNodeType] indicates the source NavNode type, this enum
 * specifies exactly how the surface should be rendered, distinguishing
 * between wrapper surfaces and content surfaces for complex node types.
 *
 * This distinction is critical for:
 * - Differentiated caching strategies (wrapper vs content caching)
 * - Animation selection (cross-navigator vs intra-navigator)
 * - Predictive back gesture handling
 *
 * @deprecated This enum is part of the flattened rendering approach which will be removed
 *   in a future version. Use [RenderingMode.Hierarchical] instead.
 */
@Deprecated(
    message = "SurfaceRenderingMode is part of the flattened rendering approach which will be removed. " +
        "Use RenderingMode.Hierarchical instead.",
    level = DeprecationLevel.WARNING
)
public enum class SurfaceRenderingMode {
    /** ScreenNode - just one composable */
    SINGLE_SCREEN,

    /** Content inside a StackNode */
    STACK_CONTENT,

    /** User's wrapper composable for TabNode */
    TAB_WRAPPER,

    /** Content of individual tab */
    TAB_CONTENT,

    /** User's wrapper composable for PaneNode */
    PANE_WRAPPER,

    /** Content of individual pane */
    PANE_CONTENT,

    /** PaneNode rendered as StackNode (small screens) */
    PANE_AS_STACK
}

/**
 * Represents the transition state of a surface during navigation.
 *
 * The transition state determines how the surface should be animated:
 * - [Visible] surfaces are fully shown without animation
 * - [Entering] surfaces are animating into view
 * - [Exiting] surfaces are animating out of view
 * - [Hidden] surfaces should not be rendered
 *
 * @deprecated This interface is part of the flattened rendering approach which will be removed
 *   in a future version. Use [RenderingMode.Hierarchical] instead.
 */
@Deprecated(
    message = "SurfaceTransitionState is part of the flattened rendering approach which will be removed. " +
        "Use RenderingMode.Hierarchical instead.",
    level = DeprecationLevel.WARNING
)
@Immutable
public sealed interface SurfaceTransitionState {

    /**
     * Surface is fully visible and not animating.
     */
    public data object Visible : SurfaceTransitionState

    /**
     * Surface is animating into view.
     *
     * @property progress Animation progress from 0.0 (start) to 1.0 (complete)
     * @property isPredictive True if driven by predictive back gesture
     */
    public data class Entering(
        val progress: Float = 0f,
        val isPredictive: Boolean = false
    ) : SurfaceTransitionState

    /**
     * Surface is animating out of view.
     *
     * @property progress Animation progress from 0.0 (start) to 1.0 (complete)
     * @property isPredictive True if driven by predictive back gesture
     */
    public data class Exiting(
        val progress: Float = 0f,
        val isPredictive: Boolean = false
    ) : SurfaceTransitionState

    /**
     * Surface should not be rendered.
     * Used for surfaces that have completed exit animations.
     */
    public data object Hidden : SurfaceTransitionState
}

/**
 * Animation specification for a surface transition.
 *
 * Encapsulates the enter and exit animations to apply when this surface
 * transitions in or out of the visible state.
 *
 * @property enter Animation to play when this surface enters the viewport.
 * @property exit Animation to play when this surface exits the viewport.
 *
 * @deprecated This class is part of the flattened rendering approach which will be removed
 *   in a future version. Use [NavTransition] with [RenderingMode.Hierarchical] instead.
 */
@Deprecated(
    message = "SurfaceAnimationSpec is part of the flattened rendering approach which will be removed. " +
        "Use NavTransition with RenderingMode.Hierarchical instead.",
    level = DeprecationLevel.WARNING
)
@Immutable
public data class SurfaceAnimationSpec(
    val enter: EnterTransition,
    val exit: ExitTransition
) {
    public companion object {
        /**
         * No animation - instant appear/disappear.
         */
        public val None: SurfaceAnimationSpec = SurfaceAnimationSpec(
            enter = EnterTransition.None,
            exit = ExitTransition.None
        )
    }
}

/**
 * Represents the structure of a single pane within a multi-pane layout.
 *
 * Used by [RenderableSurface] with [SurfaceRenderingMode.PANE_WRAPPER] to provide
 * the pane layout information to the user's wrapper composable.
 *
 * @property paneRole The role/identifier of this pane in the layout
 * @property content The composable content to render in this pane
 *
 * @deprecated This class is part of the flattened rendering approach which will be removed
 *   in a future version. Use [PaneContentSlot] with [RenderingMode.Hierarchical] instead.
 */
@Deprecated(
    message = "PaneStructure is part of the flattened rendering approach which will be removed. " +
        "Use PaneContentSlot with RenderingMode.Hierarchical instead.",
    level = DeprecationLevel.WARNING
)
@Immutable
public data class PaneStructure(
    val paneRole: PaneRole,
    val content: @Composable () -> Unit
)

/**
 * Intermediate representation of a renderable layer in the navigation UI.
 *
 * A `RenderableSurface` is produced by flattening the NavNode tree and represents
 * a single composable layer that should be rendered. The QuoVadisHost iterates
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
 * create a new instance with the modified properties using [withTransitionState] or
 * [withProgress].
 *
 * ## Usage Example
 *
 * ```kotlin
 * val surface = RenderableSurface(
 *     id = screenNode.key,
 *     zOrder = 100,
 *     nodeType = SurfaceNodeType.SCREEN,
 *     renderingMode = SurfaceRenderingMode.SINGLE_SCREEN,
 *     content = { ScreenContent(screenNode.destination) }
 * )
 * ```
 *
 * @property id Unique identifier, typically matches the NavNode.key
 * @property zOrder Rendering order (higher = on top). Based on tree depth.
 * @property nodeType Type of NavNode that produced this surface
 * @property renderingMode Specific rendering mode for this surface
 * @property transitionState Current animation state of this surface
 * @property animationSpec Enter/exit animations to apply
 * @property content The composable content to render for this surface
 * @property parentWrapperId For TAB_CONTENT and PANE_CONTENT, identifies the wrapper this belongs to
 * @property previousSurfaceId For animations/predictive back: the previous surface in the same container
 * @property paneStructures For PANE_WRAPPER in multi-pane mode: pane structures provided to user
 *
 * @deprecated This class is part of the flattened rendering approach which will be removed
 *   in a future version. Use [RenderingMode.Hierarchical] with annotation-based wrappers
 *   (`@TabWrapper`, `@PaneWrapper`, `@Screen`) instead.
 *   See migration guide: `quo-vadis-core/docs/MIGRATION_HIERARCHICAL_RENDERING.md`
 */
@Deprecated(
    message = "RenderableSurface is part of the flattened rendering approach which will be removed. " +
        "Use RenderingMode.Hierarchical with @TabWrapper/@PaneWrapper/@Screen annotations instead.",
    level = DeprecationLevel.WARNING
)
@Immutable
public data class RenderableSurface(
    val id: String,
    val zOrder: Int,
    val nodeType: SurfaceNodeType,
    val renderingMode: SurfaceRenderingMode,
    val transitionState: SurfaceTransitionState = SurfaceTransitionState.Visible,
    val animationSpec: SurfaceAnimationSpec = SurfaceAnimationSpec.None,
    val content: @Composable () -> Unit,
    val parentWrapperId: String? = null,
    val previousSurfaceId: String? = null,
    val paneStructures: List<PaneStructure>? = null
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
     *
     * @return Animation progress from 0.0 to 1.0, or null if [transitionState] is
     *         [SurfaceTransitionState.Visible] or [SurfaceTransitionState.Hidden]
     */
    val animationProgress: Float?
        get() = when (val state = transitionState) {
            is SurfaceTransitionState.Entering -> state.progress
            is SurfaceTransitionState.Exiting -> state.progress
            else -> null
        }

    /**
     * Creates a copy of this surface with an updated transition state.
     *
     * @param newState The new transition state to apply
     * @return A new [RenderableSurface] instance with the updated state
     */
    public fun withTransitionState(newState: SurfaceTransitionState): RenderableSurface {
        return copy(transitionState = newState)
    }

    /**
     * Creates a copy of this surface with updated animation progress.
     *
     * Only applicable when [transitionState] is [SurfaceTransitionState.Entering]
     * or [SurfaceTransitionState.Exiting]. For other states, returns the same surface
     * unchanged.
     *
     * @param progress The new animation progress value (0.0 to 1.0)
     * @return A new [RenderableSurface] instance with the updated progress
     */
    public fun withProgress(progress: Float): RenderableSurface {
        val newState = when (val state = transitionState) {
            is SurfaceTransitionState.Entering -> state.copy(progress = progress)
            is SurfaceTransitionState.Exiting -> state.copy(progress = progress)
            else -> transitionState
        }
        return copy(transitionState = newState)
    }
}

// =============================================================================
// Builder Pattern
// =============================================================================

/**
 * Builder for creating [RenderableSurface] instances with a fluent API.
 *
 * Useful when constructing surfaces in the TreeFlattener where
 * properties are determined incrementally.
 *
 * ## Example
 *
 * ```kotlin
 * val surface = RenderableSurfaceBuilder("screen-1")
 *     .zOrder(100)
 *     .nodeType(SurfaceNodeType.SCREEN)
 *     .renderingMode(SurfaceRenderingMode.SINGLE_SCREEN)
 *     .transitionState(SurfaceTransitionState.Entering(0f))
 *     .animation(slideIn(), slideOut())
 *     .content { ScreenContent(destination) }
 *     .build()
 * ```
 *
 * @property id The unique identifier for the surface being built
 */
public class RenderableSurfaceBuilder(private val id: String) {
    private var zOrder: Int = 0
    private var nodeType: SurfaceNodeType = SurfaceNodeType.SCREEN
    private var renderingMode: SurfaceRenderingMode = SurfaceRenderingMode.SINGLE_SCREEN
    private var transitionState: SurfaceTransitionState = SurfaceTransitionState.Visible
    private var animationSpec: SurfaceAnimationSpec = SurfaceAnimationSpec.None
    private var content: (@Composable () -> Unit)? = null
    private var parentWrapperId: String? = null
    private var previousSurfaceId: String? = null
    private var paneStructures: List<PaneStructure>? = null

    /**
     * Sets the z-order for the surface.
     *
     * @param order The rendering order (higher = on top)
     * @return This builder for chaining
     */
    public fun zOrder(order: Int): RenderableSurfaceBuilder = apply { this.zOrder = order }

    /**
     * Sets the node type for the surface.
     *
     * @param type The type of NavNode that produced this surface
     * @return This builder for chaining
     */
    public fun nodeType(type: SurfaceNodeType): RenderableSurfaceBuilder = apply { this.nodeType = type }

    /**
     * Sets the rendering mode for the surface.
     *
     * @param mode The specific rendering mode
     * @return This builder for chaining
     */
    public fun renderingMode(mode: SurfaceRenderingMode): RenderableSurfaceBuilder = apply { this.renderingMode = mode }

    /**
     * Sets the transition state for the surface.
     *
     * @param state The current animation state
     * @return This builder for chaining
     */
    public fun transitionState(state: SurfaceTransitionState): RenderableSurfaceBuilder = apply {
        this.transitionState = state
    }

    /**
     * Sets the animation specification using enter and exit transitions.
     *
     * @param enter The enter transition animation
     * @param exit The exit transition animation
     * @return This builder for chaining
     */
    public fun animation(enter: EnterTransition, exit: ExitTransition): RenderableSurfaceBuilder = apply {
        this.animationSpec = SurfaceAnimationSpec(enter, exit)
    }

    /**
     * Sets the animation specification using a pre-built spec.
     *
     * @param spec The animation specification
     * @return This builder for chaining
     */
    public fun animation(spec: SurfaceAnimationSpec): RenderableSurfaceBuilder = apply { this.animationSpec = spec }

    /**
     * Sets the composable content for the surface.
     *
     * @param block The composable content lambda
     * @return This builder for chaining
     */
    public fun content(block: @Composable () -> Unit): RenderableSurfaceBuilder = apply { this.content = block }

    /**
     * Sets the parent wrapper ID for content surfaces.
     *
     * @param id The ID of the parent wrapper surface, or null
     * @return This builder for chaining
     */
    public fun parentWrapperId(id: String?): RenderableSurfaceBuilder = apply { this.parentWrapperId = id }

    /**
     * Sets the previous surface ID for animation pairing.
     *
     * @param id The ID of the previous surface in the same container, or null
     * @return This builder for chaining
     */
    public fun previousSurfaceId(id: String?): RenderableSurfaceBuilder = apply { this.previousSurfaceId = id }

    /**
     * Sets the pane structures for multi-pane mode.
     *
     * @param structures The list of pane structures, or null
     * @return This builder for chaining
     */
    public fun paneStructures(structures: List<PaneStructure>?): RenderableSurfaceBuilder = apply {
        this.paneStructures = structures
    }

    /**
     * Builds the [RenderableSurface] instance.
     *
     * @return The constructed [RenderableSurface]
     * @throws IllegalStateException if [content] has not been set
     */
    public fun build(): RenderableSurface {
        requireNotNull(content) { "Content must be set before building RenderableSurface" }
        return RenderableSurface(
            id = id,
            zOrder = zOrder,
            nodeType = nodeType,
            renderingMode = renderingMode,
            transitionState = transitionState,
            animationSpec = animationSpec,
            content = content!!,
            parentWrapperId = parentWrapperId,
            previousSurfaceId = previousSurfaceId,
            paneStructures = paneStructures
        )
    }
}

/**
 * DSL function to create a [RenderableSurface] using the builder pattern.
 *
 * ## Example
 *
 * ```kotlin
 * val surface = renderableSurface("screen-1") {
 *     zOrder(100)
 *     nodeType(SurfaceNodeType.SCREEN)
 *     renderingMode(SurfaceRenderingMode.SINGLE_SCREEN)
 *     content { MyScreenContent() }
 * }
 * ```
 *
 * @param id The unique identifier for the surface
 * @param block The builder configuration block
 * @return The constructed [RenderableSurface]
 */
public inline fun renderableSurface(
    id: String,
    block: RenderableSurfaceBuilder.() -> Unit
): RenderableSurface {
    return RenderableSurfaceBuilder(id).apply(block).build()
}

// =============================================================================
// Extension Functions for List<RenderableSurface>
// =============================================================================

/**
 * Returns surfaces sorted by [RenderableSurface.zOrder] for proper rendering order.
 *
 * Surfaces with lower z-order are rendered first (at the bottom), and surfaces
 * with higher z-order are rendered last (on top).
 *
 * @return A new list sorted by z-order in ascending order
 */
public fun List<RenderableSurface>.sortedByZOrder(): List<RenderableSurface> {
    return sortedBy { it.zOrder }
}

/**
 * Returns only surfaces that should be rendered.
 *
 * Filters out surfaces where [RenderableSurface.shouldRender] is false
 * (i.e., surfaces with [SurfaceTransitionState.Hidden] state).
 *
 * @return A new list containing only renderable surfaces
 */
public fun List<RenderableSurface>.renderable(): List<RenderableSurface> {
    return filter { it.shouldRender }
}

/**
 * Finds a surface by its unique identifier.
 *
 * @param id The ID to search for
 * @return The matching [RenderableSurface], or null if not found
 */
public fun List<RenderableSurface>.findById(id: String): RenderableSurface? {
    return find { it.id == id }
}

/**
 * Returns surfaces that are currently animating.
 *
 * Filters to surfaces where [RenderableSurface.isAnimating] is true
 * (i.e., surfaces with [SurfaceTransitionState.Entering] or
 * [SurfaceTransitionState.Exiting] state).
 *
 * @return A new list containing only animating surfaces
 */
public fun List<RenderableSurface>.animating(): List<RenderableSurface> {
    return filter { it.isAnimating }
}

/**
 * Diffs two surface lists to identify surfaces that need enter/exit animations.
 *
 * Compares this list (old state) with [newSurfaces] (new state) to determine:
 * - **Entering surfaces**: Surfaces in [newSurfaces] that are not in this list
 * - **Exiting surfaces**: Surfaces in this list that are not in [newSurfaces]
 *
 * ## Example
 *
 * ```kotlin
 * val oldSurfaces = listOf(surfaceA, surfaceB)
 * val newSurfaces = listOf(surfaceB, surfaceC)
 *
 * val (entering, exiting) = oldSurfaces.diffWith(newSurfaces)
 * // entering = [surfaceC]
 * // exiting = [surfaceA]
 * ```
 *
 * @param newSurfaces The new list of surfaces after navigation
 * @return A [Pair] of (entering surfaces, exiting surfaces)
 */
public fun List<RenderableSurface>.diffWith(
    newSurfaces: List<RenderableSurface>
): Pair<List<RenderableSurface>, List<RenderableSurface>> {
    val oldIds = this.map { it.id }.toSet()
    val newIds = newSurfaces.map { it.id }.toSet()

    val entering = newSurfaces.filter { it.id !in oldIds }
    val exiting = this.filter { it.id !in newIds }

    return entering to exiting
}
