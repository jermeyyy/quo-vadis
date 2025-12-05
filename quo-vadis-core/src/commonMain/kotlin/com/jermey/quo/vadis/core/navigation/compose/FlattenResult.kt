package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Immutable

/**
 * Type of transition between surfaces.
 *
 * Used to determine which animation to apply and how to handle
 * caching during navigation. The transition type affects both
 * the visual animations and the caching strategy used by the renderer.
 */
public enum class TransitionType {
    /**
     * Forward navigation in a stack (push).
     *
     * Typically involves a slide-in-from-right animation for the entering
     * surface and a partial slide-out-left for the exiting surface.
     */
    PUSH,

    /**
     * Backward navigation in a stack (pop).
     *
     * Typically involves a slide-in-from-left animation for the entering
     * surface and a slide-out-right for the exiting surface.
     * Often driven by predictive back gestures.
     */
    POP,

    /**
     * Switching between tabs in a TabNode.
     *
     * Typically uses crossfade or no animation, depending on user preference.
     * Tab content may be cached for instant switching.
     */
    TAB_SWITCH,

    /**
     * Switching between panes or pane configurations.
     *
     * Used when the active pane changes in a PaneNode, or when
     * adaptive layout changes cause pane visibility to change.
     */
    PANE_SWITCH,

    /**
     * No transition (initial state or no animation needed).
     *
     * Used for the initial render or when navigation state changes
     * without requiring visual animation.
     */
    NONE
}

/**
 * Represents a pair of surfaces involved in a transition animation.
 *
 * This is used by the renderer to coordinate enter/exit animations
 * between the current and previous surfaces. The animation pair provides
 * the necessary context for the renderer to determine which animations
 * to apply and how to synchronize them.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val pair = AnimationPair(
 *     currentId = "detail-screen",
 *     previousId = "list-screen",
 *     transitionType = TransitionType.PUSH
 * )
 * // Renderer uses this to animate list sliding out and detail sliding in
 * ```
 *
 * @property currentId ID of the surface that is entering/becoming visible
 * @property previousId ID of the surface that is exiting (null if no previous)
 * @property transitionType Type of transition being performed
 */
@Immutable
public data class AnimationPair(
    val currentId: String,
    val previousId: String?,
    val transitionType: TransitionType
)

/**
 * Hints for the caching system about how to cache surfaces.
 *
 * These hints help the renderer decide which surfaces to cache
 * and at what granularity during navigation. Proper caching improves
 * performance by avoiding unnecessary recomposition and layout passes.
 *
 * ## Caching Strategy
 *
 * - **Wrapper caching**: Useful for TabNode/PaneNode wrappers that don't change
 *   frequently. Caching the wrapper allows instant content swapping.
 *
 * - **Content caching**: Useful for individual screens that may be revisited.
 *   Tab content is a prime candidate for content caching.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val hints = CachingHints(
 *     shouldCacheWrapper = true,
 *     shouldCacheContent = true,
 *     cacheableIds = setOf("tab-wrapper", "tab-0-content", "tab-1-content"),
 *     invalidatedIds = setOf("old-screen")
 * )
 * ```
 *
 * @property shouldCacheWrapper Whether wrapper surfaces (TAB_WRAPPER, PANE_WRAPPER) should be cached
 * @property shouldCacheContent Whether content surfaces should be cached
 * @property cacheableIds IDs of surfaces that are safe to cache
 * @property invalidatedIds IDs of surfaces whose cache should be invalidated
 */
@Immutable
public data class CachingHints(
    val shouldCacheWrapper: Boolean = false,
    val shouldCacheContent: Boolean = true,
    val cacheableIds: Set<String> = emptySet(),
    val invalidatedIds: Set<String> = emptySet()
) {
    public companion object {
        /**
         * Default hints with content caching enabled but no wrapper caching.
         */
        public val Default: CachingHints = CachingHints()

        /**
         * Hints indicating no caching should occur.
         * Useful during development or when debugging caching issues.
         */
        public val NoCache: CachingHints = CachingHints(
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
 * This is the primary output of the [TreeFlattener] and serves
 * as the input to the QuoVadisHost rendering pipeline.
 *
 * ## Structure
 *
 * - **surfaces**: The list of [RenderableSurface] objects to render,
 *   already sorted by z-order for proper layering.
 *
 * - **animationPairs**: Information about which surfaces are transitioning,
 *   used to coordinate enter/exit animations.
 *
 * - **cachingHints**: Guidance for the caching system to optimize
 *   rendering performance.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val result = flattener.flattenState(rootNode, previousRootNode)
 *
 * // Render all visible surfaces
 * result.renderableSurfaces.forEach { surface ->
 *     Box(modifier = Modifier.zIndex(surface.zOrder.toFloat())) {
 *         surface.content()
 *     }
 * }
 *
 * // Check for animations
 * result.animationPairs.forEach { pair ->
 *     when (pair.transitionType) {
 *         TransitionType.PUSH -> animatePush(pair)
 *         TransitionType.POP -> animatePop(pair)
 *         // ...
 *     }
 * }
 * ```
 *
 * @property surfaces Ordered list of surfaces to render (sorted by zOrder)
 * @property animationPairs Pairs of surfaces involved in transitions
 * @property cachingHints Hints for the caching system
 */
@Immutable
public data class FlattenResult(
    val surfaces: List<RenderableSurface>,
    val animationPairs: List<AnimationPair>,
    val cachingHints: CachingHints
) {
    public companion object {
        /**
         * Empty result with no surfaces.
         * Useful as a default value or for empty navigation states.
         */
        public val Empty: FlattenResult = FlattenResult(
            surfaces = emptyList(),
            animationPairs = emptyList(),
            cachingHints = CachingHints.Default
        )
    }

    /**
     * Returns surfaces that should be rendered (not hidden).
     *
     * Filters out surfaces with [SurfaceTransitionState.Hidden] state,
     * which should not be composed to the UI.
     */
    val renderableSurfaces: List<RenderableSurface>
        get() = surfaces.filter { it.shouldRender }

    /**
     * Returns surfaces sorted by z-order for proper layering.
     *
     * Surfaces with lower z-order values are rendered first (at the back),
     * and surfaces with higher z-order values are rendered last (on top).
     */
    val sortedSurfaces: List<RenderableSurface>
        get() = surfaces.sortedBy { it.zOrder }

    /**
     * Returns true if there are no surfaces to render.
     */
    val isEmpty: Boolean
        get() = surfaces.isEmpty()

    /**
     * Returns true if any surface is currently animating.
     */
    val hasAnimatingSurfaces: Boolean
        get() = surfaces.any { it.isAnimating }

    /**
     * Returns the number of surfaces in this result.
     */
    val surfaceCount: Int
        get() = surfaces.size

    /**
     * Finds the animation pair for a given surface ID.
     *
     * Searches both currentId and previousId fields to find
     * any animation pair involving the given surface.
     *
     * @param surfaceId The ID of the surface to search for
     * @return The [AnimationPair] involving this surface, or null if not found
     */
    public fun findAnimationPair(surfaceId: String): AnimationPair? {
        return animationPairs.find { it.currentId == surfaceId || it.previousId == surfaceId }
    }

    /**
     * Finds a surface by its unique identifier.
     *
     * @param surfaceId The ID to search for
     * @return The matching [RenderableSurface], or null if not found
     */
    public fun findSurface(surfaceId: String): RenderableSurface? {
        return surfaces.find { it.id == surfaceId }
    }

    /**
     * Returns the topmost surface (highest z-order).
     *
     * @return The surface with the highest z-order, or null if empty
     */
    public fun topSurface(): RenderableSurface? {
        return surfaces.maxByOrNull { it.zOrder }
    }

    /**
     * Returns the bottom-most surface (lowest z-order).
     *
     * @return The surface with the lowest z-order, or null if empty
     */
    public fun bottomSurface(): RenderableSurface? {
        return surfaces.minByOrNull { it.zOrder }
    }
}
