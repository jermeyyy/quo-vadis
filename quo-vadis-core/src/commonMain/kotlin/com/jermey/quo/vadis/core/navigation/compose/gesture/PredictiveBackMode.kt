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
