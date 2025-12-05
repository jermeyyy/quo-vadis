package com.jermey.quo.vadis.core.navigation.core

/**
 * Represents the current state of navigation transitions.
 *
 * This sealed interface models all possible transition states during navigation,
 * including animations, predictive back gestures, and idle states.
 *
 * ## Usage
 *
 * Observe the transition state to coordinate animations:
 *
 * ```kotlin
 * navigator.transitionState.collect { state ->
 *     when (state) {
 *         is TransitionState.Idle -> // No animation
 *         is TransitionState.InProgress -> // Animate with state.progress
 *         is TransitionState.PredictiveBack -> // Gesture-driven animation
 *     }
 * }
 * ```
 */
sealed interface TransitionState {

    /**
     * No transition is occurring.
     */
    data object Idle : TransitionState

    /**
     * A transition is in progress.
     *
     * @property transition The transition definition being applied
     * @property progress Animation progress from 0.0 to 1.0
     * @property fromKey Key of the source node being animated from
     * @property toKey Key of the destination node being animated to
     */
    data class InProgress(
        val transition: NavigationTransition,
        val progress: Float = 0f,
        val fromKey: String? = null,
        val toKey: String? = null
    ) : TransitionState {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1, was: $progress" }
        }
    }

    /**
     * Predictive back gesture is in progress.
     *
     * This state is used during interactive back navigation where
     * the user controls the animation progress via touch gesture.
     *
     * @property progress Gesture progress from 0.0 (start) to 1.0 (complete)
     * @property currentKey Key of the currently visible node
     * @property previousKey Key of the node being revealed during back
     * @property touchX Current x-coordinate of the gesture (normalized 0-1)
     * @property touchY Current y-coordinate of the gesture (normalized 0-1)
     * @property isCommitted Whether the gesture has passed the commit threshold
     */
    data class PredictiveBack(
        val progress: Float,
        val currentKey: String? = null,
        val previousKey: String? = null,
        val touchX: Float = 0f,
        val touchY: Float = 0f,
        val isCommitted: Boolean = false
    ) : TransitionState {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1, was: $progress" }
            require(touchX in 0f..1f) { "touchX must be between 0 and 1, was: $touchX" }
            require(touchY in 0f..1f) { "touchY must be between 0 and 1, was: $touchY" }
        }

        /**
         * Returns true if the gesture should complete navigation when released.
         * Default threshold is 20% progress.
         */
        fun shouldComplete(threshold: Float = 0.2f): Boolean = progress >= threshold || isCommitted
    }

    /**
     * A seek-based transition for fine-grained animation control.
     *
     * This is used for shared element transitions and other scenarios
     * where the animation needs to be synchronized with external factors.
     *
     * @property transition The transition definition
     * @property progress Current seek position (0.0 to 1.0)
     * @property isPaused Whether the seek animation is paused
     */
    data class Seeking(
        val transition: NavigationTransition,
        val progress: Float,
        val isPaused: Boolean = false
    ) : TransitionState {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1, was: $progress" }
        }
    }
}

/**
 * Extension to check if any transition is active.
 */
val TransitionState.isAnimating: Boolean
    get() = this !is TransitionState.Idle

/**
 * Extension to get the current progress, regardless of transition type.
 * Returns 0 for [TransitionState.Idle].
 */
val TransitionState.progress: Float
    get() = when (this) {
        is TransitionState.Idle -> 0f
        is TransitionState.InProgress -> progress
        is TransitionState.PredictiveBack -> progress
        is TransitionState.Seeking -> progress
    }
