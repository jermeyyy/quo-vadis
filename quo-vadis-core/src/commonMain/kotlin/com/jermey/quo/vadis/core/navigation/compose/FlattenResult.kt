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
 * 3. AnimationPair created with both surface references
 * 4. Renderer uses pair to animate both surfaces
 * 5. Animation completes, pair is disposed
 *
 * ## Usage Patterns
 *
 * ### Basic pair (TreeFlattener - transition tracking only)
 * ```kotlin
 * val pair = AnimationPair(
 *     currentId = "detail-screen",
 *     previousId = "list-screen",
 *     transitionType = TransitionType.PUSH
 * )
 * ```
 *
 * ### Full pair (AnimationPairTracker - animation execution)
 * ```kotlin
 * val pair = AnimationPair(
 *     currentId = "detail-screen",
 *     previousId = "list-screen",
 *     transitionType = TransitionType.PUSH,
 *     currentSurface = detailSurface,
 *     previousSurface = listSurface,
 *     containerId = null
 * )
 * // Renderer uses this to animate list sliding out and detail sliding in
 * ```
 *
 * @property currentId ID of the entering/current surface (always present)
 * @property previousId ID of the exiting/previous surface (null for initial render)
 * @property transitionType The type of navigation that triggered this pair
 * @property currentSurface The entering [RenderableSurface] (null for basic pairs)
 * @property previousSurface The exiting [RenderableSurface] (null if none or basic pair)
 * @property containerId ID of the container where this transition occurs
 */
@Immutable
public data class AnimationPair(
    val currentId: String,
    val previousId: String?,
    val transitionType: TransitionType,
    val currentSurface: RenderableSurface? = null,
    val previousSurface: RenderableSurface? = null,
    val containerId: String? = null
) {
    /**
     * Returns true if this pair has both entering and exiting surfaces.
     * Initial renders only have a current surface.
     */
    val hasBothSurfaces: Boolean
        get() = previousSurface != null

    /**
     * Returns true if this pair has full surface references.
     * Basic pairs from TreeFlattener only have IDs, not surface references.
     */
    val hasFullSurfaces: Boolean
        get() = currentSurface != null

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
 * ## Tab/Pane Navigation Behavior
 *
 * - **Cross-node navigation** (e.g., Stack → Tab): Cache entire wrapper + content
 * - **Intra-tab navigation** (tab switch): Cache only content surfaces, not wrapper
 *
 * ## Usage Example
 *
 * ```kotlin
 * val hints = CachingHints(
 *     shouldCacheWrapper = true,
 *     shouldCacheContent = true,
 *     cacheableIds = setOf("tab-wrapper", "tab-0-content", "tab-1-content"),
 *     invalidatedIds = setOf("old-screen"),
 *     wrapperIds = setOf("tab-wrapper"),
 *     contentIds = setOf("tab-0-content", "tab-1-content"),
 *     isCrossNodeTypeNavigation = false
 * )
 * ```
 *
 * @property shouldCacheWrapper Whether wrapper surfaces (TAB_WRAPPER, PANE_WRAPPER) should be cached
 * @property shouldCacheContent Whether content surfaces should be cached
 * @property cacheableIds IDs of surfaces that are safe to cache
 * @property invalidatedIds IDs of surfaces whose cache should be invalidated
 * @property wrapperIds IDs of wrapper surfaces (TAB_WRAPPER, PANE_WRAPPER)
 * @property contentIds IDs of content surfaces (TAB_CONTENT, PANE_CONTENT)
 * @property isCrossNodeTypeNavigation True if navigating between different node types (e.g., Stack → Tab)
 */
@Immutable
public data class CachingHints(
    val shouldCacheWrapper: Boolean = false,
    val shouldCacheContent: Boolean = true,
    val cacheableIds: Set<String> = emptySet(),
    val invalidatedIds: Set<String> = emptySet(),
    val wrapperIds: Set<String> = emptySet(),
    val contentIds: Set<String> = emptySet(),
    val isCrossNodeTypeNavigation: Boolean = false
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

    /**
     * Returns surface IDs that are part of an active animation pair.
     *
     * These surfaces are currently animating and should be handled by the
     * animation system rather than rendered statically.
     */
    val animatingSurfaces: Set<String>
        get() = animationPairs.flatMap { pair ->
            listOfNotNull(pair.currentId, pair.previousId)
        }.toSet()

    /**
     * Finds the animation pair containing a specific surface.
     *
     * This is an alias for [findAnimationPair] with a different name
     * to match the spec naming convention.
     *
     * @param surfaceId The ID of the surface to search for
     * @return The [AnimationPair] involving this surface, or null if not found
     */
    public fun findPairForSurface(surfaceId: String): AnimationPair? {
        return animationPairs.find {
            it.currentId == surfaceId || it.previousId == surfaceId
        }
    }

    /**
     * Returns animation pairs that support shared elements.
     *
     * Only stack transitions (PUSH/POP) with both surfaces present
     * can support shared element transitions.
     */
    val sharedElementPairs: List<AnimationPair>
        get() = animationPairs.filter { it.supportsSharedElements }
}
