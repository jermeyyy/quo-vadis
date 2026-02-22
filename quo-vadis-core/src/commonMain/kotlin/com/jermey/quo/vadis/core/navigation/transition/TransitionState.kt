package com.jermey.quo.vadis.core.navigation.transition

/**
 * Transition state interface for navigation animations.
 *
 * Represents the current state of a navigation transition,
 * including progress, gesture tracking, and transition metadata.
 */
sealed interface TransitionState {

    /**
     * No transition is occurring.
     */
    data object Idle : TransitionState

    /**
     * Common interface for all active (non-idle) transition states.
     *
     * Provides uniform access to [progress] and a type-safe way to
     * update progress without pattern-matching each subtype.
     */
    sealed interface Active : TransitionState {
        /** Animation progress from 0.0 to 1.0. */
        val progress: Float

        /** Returns a copy of this state with the given [progress]. */
        fun withProgress(progress: Float): Active
    }

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
        override val progress: Float = 0f,
        val fromKey: String? = null,
        val toKey: String? = null
    ) : Active {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1, was: $progress" }
        }

        override fun withProgress(progress: Float) = copy(progress = progress)
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
        override val progress: Float,
        val currentKey: String? = null,
        val previousKey: String? = null,
        val touchX: Float = 0f,
        val touchY: Float = 0f,
        val isCommitted: Boolean = false
    ) : Active {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1, was: $progress" }
            require(touchX in 0f..1f) { "touchX must be between 0 and 1, was: $touchX" }
            require(touchY in 0f..1f) { "touchY must be between 0 and 1, was: $touchY" }
        }

        override fun withProgress(progress: Float) = copy(progress = progress)
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
        override val progress: Float,
        val isPaused: Boolean = false
    ) : Active {
        init {
            require(progress in 0f..1f) { "Progress must be between 0 and 1, was: $progress" }
        }

        override fun withProgress(progress: Float) = copy(progress = progress)
    }
}

/**
 * Extension to get the current progress, regardless of transition type.
 * Returns 0 for [TransitionState.Idle].
 */
val TransitionState.progress: Float
    get() = when (this) {
        is TransitionState.Idle -> 0f
        is TransitionState.Active -> progress
    }
