package com.jermey.quo.vadis.core.navigation.compose

/**
 * Represents the current navigation transition state for the tracker.
 *
 * Used to provide explicit transition type information to the [AnimationPairTracker]
 * when it cannot be inferred from surface changes alone.
 *
 * **Note**: This is separate from [com.jermey.quo.vadis.core.navigation.tree.TransitionState]
 * which is used by TreeNavigator for tree operations.
 */
public sealed interface TrackerTransitionState {
    /**
     * A new screen is being pushed onto a stack.
     *
     * @property targetId ID of the screen being pushed
     */
    public data class Push(val targetId: String) : TrackerTransitionState

    /**
     * A screen is being popped from a stack.
     *
     * @property sourceId ID of the screen being popped
     */
    public data class Pop(val sourceId: String) : TrackerTransitionState

    /**
     * A tab is being switched.
     *
     * @property fromTab ID of the tab being left
     * @property toTab ID of the tab being activated
     */
    public data class TabSwitch(val fromTab: String, val toTab: String) : TrackerTransitionState

    /**
     * A pane is being switched (in single-pane mode).
     *
     * @property fromPane ID of the pane being left
     * @property toPane ID of the pane being activated
     */
    public data class PaneSwitch(val fromPane: String, val toPane: String) : TrackerTransitionState

    /**
     * No transition (instant navigation).
     */
    public data object None : TrackerTransitionState
}

/**
 * Tracks navigation state changes to produce animation pairs.
 *
 * The tracker maintains a history of the previous navigation state and surfaces,
 * comparing them to the new state to detect and classify transitions. This enables
 * the renderer to animate both entering and exiting surfaces simultaneously.
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
 *
 * // Use pairs to animate transitions
 * pairs.forEach { pair ->
 *     if (pair.shouldAnimate) {
 *         animateTransition(pair)
 *     }
 * }
 * ```
 *
 * ## Container Matching
 *
 * The tracker uses container IDs to match entering and exiting surfaces. When a surface
 * enters, the tracker looks for an exiting surface in the same container to pair with it.
 * This ensures tab content transitions are paired with the previous tab content, not
 * with unrelated stack content.
 */
public class AnimationPairTracker {

    private var lastSurfaces: Map<String, RenderableSurface> = emptyMap()
    private var lastSurfacesByContainer: Map<String?, String> = emptyMap()

    /**
     * Analyzes the difference between old and new surfaces to produce animation pairs.
     *
     * Compares the new surfaces against the previously tracked surfaces to detect:
     * - Entering surfaces (in new but not in old)
     * - Exiting surfaces (in old but not in new)
     *
     * For each entering surface, tries to find a corresponding exiting surface
     * in the same container to create an animation pair.
     *
     * @param newSurfaces The new list of surfaces after navigation
     * @param transitionState Optional transition state for explicit transition type
     * @return List of animation pairs representing active transitions
     */
    public fun trackTransition(
        newSurfaces: List<RenderableSurface>,
        transitionState: TrackerTransitionState? = null
    ): List<AnimationPair> {
        val pairs = mutableListOf<AnimationPair>()
        val newSurfacesMap = newSurfaces.associateBy { it.id }
        val newIds = newSurfacesMap.keys
        val oldIds = lastSurfaces.keys

        // Detect entering surfaces (in new but not in old)
        val enteringIds = newIds - oldIds

        // Detect exiting surfaces (in old but not in new)
        val exitingIds = oldIds - newIds

        // Track which exiting surfaces have been matched
        val matchedExitingIds = mutableSetOf<String>()

        // Process entering surfaces
        for (enterId in enteringIds) {
            val enteringSurface = newSurfacesMap[enterId] ?: continue

            // Find the corresponding exiting surface in the same container
            val exitingSurface = findExitingSurfaceForContainer(
                enteringSurface = enteringSurface,
                exitingIds = exitingIds
            )

            exitingSurface?.let { matchedExitingIds.add(it.id) }

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
        val unmatchedExiting = exitingIds - matchedExitingIds
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
     *
     * Uses a multi-step matching strategy:
     * 1. First, tries to match by previousSurfaceId if set on the entering surface
     * 2. Then, tries to match by container ID (parentWrapperId)
     * 3. Finally, for top-level transitions, matches by node type
     *
     * @param enteringSurface The surface that is entering
     * @param exitingIds Set of IDs for surfaces that are exiting
     * @return The matched exiting surface, or null if no match found
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
                    exitingSurface.nodeType == enteringSurface.nodeType
                ) {
                    return exitingSurface
                }
            }
        }

        return null
    }

    /**
     * Determines the transition type based on surface characteristics.
     *
     * Priority order:
     * 1. Explicit transition state (if provided)
     * 2. Initial render detection (no previous surface)
     * 3. Inference from rendering mode
     *
     * @param enteringSurface The surface that is entering
     * @param exitingSurface The surface that is exiting (may be null)
     * @param transitionState Optional explicit transition state
     * @return The determined [TransitionType]
     */
    private fun determineTransitionType(
        enteringSurface: RenderableSurface,
        exitingSurface: RenderableSurface?,
        transitionState: TrackerTransitionState?
    ): TransitionType {
        // If we have explicit transition state, use it
        transitionState?.let {
            return when (it) {
                is TrackerTransitionState.Push -> TransitionType.PUSH
                is TrackerTransitionState.Pop -> TransitionType.POP
                is TrackerTransitionState.TabSwitch -> TransitionType.TAB_SWITCH
                is TrackerTransitionState.PaneSwitch -> TransitionType.PANE_SWITCH
                is TrackerTransitionState.None -> TransitionType.NONE
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
     *
     * Maintains a mapping of container IDs to the currently active content surface
     * within that container. Used to quickly find the exiting surface for a container.
     *
     * @param surfaces The current list of surfaces
     */
    private fun updateContainerTracking(surfaces: List<RenderableSurface>) {
        lastSurfacesByContainer = surfaces
            .filter { it.renderingMode.isContentMode() }
            .associate { it.parentWrapperId to it.id }
    }

    /**
     * Resets the tracker state.
     *
     * Call when navigation is fully reset or when the tracker should forget
     * all previous state. After reset, the next call to [trackTransition]
     * will treat all surfaces as entering with no previous.
     */
    public fun reset() {
        lastSurfaces = emptyMap()
        lastSurfacesByContainer = emptyMap()
    }
}

/**
 * Extension to check if a rendering mode represents content (vs wrapper).
 *
 * Content modes are surfaces that display actual navigation content:
 * - [SurfaceRenderingMode.STACK_CONTENT] - Content inside a stack
 * - [SurfaceRenderingMode.TAB_CONTENT] - Content of a tab
 * - [SurfaceRenderingMode.PANE_CONTENT] - Content of a pane
 * - [SurfaceRenderingMode.SINGLE_SCREEN] - A standalone screen
 *
 * @return true if this mode represents content, false for wrapper modes
 */
public fun SurfaceRenderingMode.isContentMode(): Boolean {
    return this == SurfaceRenderingMode.STACK_CONTENT ||
        this == SurfaceRenderingMode.TAB_CONTENT ||
        this == SurfaceRenderingMode.PANE_CONTENT ||
        this == SurfaceRenderingMode.SINGLE_SCREEN
}
