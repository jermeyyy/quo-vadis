package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition

/**
 * Coordinates all navigation animations including regular navigation and predictive back.
 *
 * Manages:
 * - Animation state (idle, animating forward, animating back, gesture in progress)
 * - Transition selection (per-entry or fallback to default)
 * - Screen caching during animations
 * - Proper enter/exit direction based on navigation type
 */
@Stable
class AnimationCoordinator(
    private val defaultTransition: NavigationTransition
) {
    /**
     * Animation states.
     */
    sealed class AnimationState {
        object Idle : AnimationState()
        data class AnimatingForward(
            val from: BackStackEntry?,
            val to: BackStackEntry,
            val transition: NavigationTransition
        ) : AnimationState()
        data class AnimatingBack(
            val from: BackStackEntry,
            val to: BackStackEntry?,
            val transition: NavigationTransition
        ) : AnimationState()
        data class GestureInProgress(
            val current: BackStackEntry,
            val previous: BackStackEntry?,
            val progress: Float
        ) : AnimationState()
    }

    var state by mutableStateOf<AnimationState>(AnimationState.Idle)
        private set

    /**
     * Entries to display (frozen during animation).
     */
    var displayedCurrent by mutableStateOf<BackStackEntry?>(null)
        private set
    var displayedPrevious by mutableStateOf<BackStackEntry?>(null)
        private set

    /**
     * Start forward navigation animation.
     */
    fun startForwardAnimation(
        from: BackStackEntry?,
        to: BackStackEntry
    ) {
        val transition = to.transition ?: defaultTransition
        state = AnimationState.AnimatingForward(from, to, transition)
        displayedCurrent = to
        displayedPrevious = from
    }

    /**
     * Start back navigation animation.
     */
    fun startBackAnimation(
        from: BackStackEntry,
        to: BackStackEntry?
    ) {
        // Use the transition from the entry we're going back to
        val transition = to?.transition ?: defaultTransition
        state = AnimationState.AnimatingBack(from, to, transition)
        displayedCurrent = to
        displayedPrevious = from
    }

    /**
     * Start predictive back gesture.
     */
    fun startGesture(
        current: BackStackEntry,
        previous: BackStackEntry?
    ) {
        state = AnimationState.GestureInProgress(current, previous, 0f)
        displayedCurrent = current
        displayedPrevious = previous
    }

    /**
     * Update gesture progress.
     */
    fun updateGestureProgress(progress: Float) {
        val currentState = state
        if (currentState is AnimationState.GestureInProgress) {
            state = currentState.copy(progress = progress)
        }
    }

    /**
     * Commit gesture (convert to back animation).
     */
    fun commitGesture() {
        val currentState = state
        if (currentState is AnimationState.GestureInProgress) {
            // Transition to exit animation
            startBackAnimation(currentState.current, currentState.previous)
        }
    }

    /**
     * Cancel gesture (return to idle).
     */
    fun cancelGesture() {
        state = AnimationState.Idle
    }

    /**
     * Finish current animation.
     */
    fun finishAnimation() {
        state = AnimationState.Idle
    }

    /**
     * Get the appropriate transition for current state.
     */
    fun getActiveTransition(): NavigationTransition {
        return when (val s = state) {
            is AnimationState.AnimatingForward -> s.transition
            is AnimationState.AnimatingBack -> s.transition
            is AnimationState.GestureInProgress ->
                s.previous?.transition ?: defaultTransition
            AnimationState.Idle -> defaultTransition
        }
    }

    /**
     * Check if back direction (for proper enter/exit selection).
     */
    fun isBackwardAnimation(): Boolean {
        return state is AnimationState.AnimatingBack ||
               state is AnimationState.GestureInProgress
    }
}

/**
 * Remember an animation coordinator across recompositions.
 */
@Composable
fun rememberAnimationCoordinator(
    defaultTransition: NavigationTransition
): AnimationCoordinator {
    return remember(defaultTransition) {
        AnimationCoordinator(defaultTransition)
    }
}
