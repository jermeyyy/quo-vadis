package com.jermey.quo.vadis.core.navigation.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.NavigationTransition
import com.jermey.quo.vadis.core.navigation.TransitionState
import kotlinx.coroutines.flow.StateFlow

/**
 * Internal interface for transition and animation control.
 *
 * This interface is used by the navigation framework internals (NavigationHost, renderers)
 * to coordinate animations. Not intended for end-user consumption.
 *
 * @suppress This is an internal API
 */
@InternalQuoVadisApi
interface TransitionController {

    /**
     * The current transition state for animations.
     *
     * During navigation, this holds transition metadata for animation
     * coordination. Observe this to drive animations in the renderer.
     */
    val transitionState: StateFlow<TransitionState>

    /**
     * Current transition animation (null if idle).
     *
     * Derived from [transitionState] for convenience.
     */
    val currentTransition: StateFlow<NavigationTransition?>

    /**
     * Update transition progress during animations.
     *
     * Called by the renderer to update animation progress.
     *
     * @param progress Animation progress from 0.0 to 1.0
     */
    fun updateTransitionProgress(progress: Float)

    /**
     * Start a predictive back gesture.
     *
     * Called when the user initiates a back gesture.
     */
    fun startPredictiveBack()

    /**
     * Update predictive back gesture progress.
     *
     * @param progress Gesture progress from 0.0 to 1.0
     * @param touchX Normalized x-coordinate of touch (0-1)
     * @param touchY Normalized y-coordinate of touch (0-1)
     */
    fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float)

    /**
     * Cancel the predictive back gesture.
     *
     * Called when the user releases the gesture without completing it.
     */
    fun cancelPredictiveBack()

    /**
     * Commit the predictive back gesture.
     *
     * Called when the user completes the back gesture.
     */
    fun commitPredictiveBack()

    /**
     * Complete the current transition animation.
     *
     * Called when the animation finishes.
     */
    fun completeTransition()
}
