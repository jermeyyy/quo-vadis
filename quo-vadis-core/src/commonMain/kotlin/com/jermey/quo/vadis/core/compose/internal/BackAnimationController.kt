@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jermey.quo.vadis.core.compose.internal.navback.BackNavigationEvent

/**
 * Controller for predictive back animations in the hierarchical navigation system.
 *
 * This controller manages animation state during predictive back gestures, providing
 * a centralized place for renderers to observe and react to gesture progress.
 *
 * ## State Management
 *
 * The controller tracks:
 * - [isAnimating] - Whether a back gesture animation is currently active
 * - [progress] - Current gesture progress (0.0 to 1.0)
 * - [currentEvent] - The most recent [BackNavigationEvent] with full gesture details
 *
 * ## Usage in Renderers
 *
 * Renderers can observe this controller to apply custom animations during back gestures:
 *
 * ```kotlin
 * @Composable
 * fun StackRenderer(node: StackNode, scope: NavRenderScope) {
 *     val backController = LocalBackAnimationController.current
 *
 *     // Apply animation based on gesture progress
 *     val offsetX = if (backController?.isAnimating == true) {
 *         with(LocalDensity.current) {
 *             (backController.progress * 100).dp.toPx()
 *         }
 *     } else 0f
 *
 *     Box(modifier = Modifier.offset { IntOffset(offsetX.toInt(), 0) }) {
 *         // Content
 *     }
 * }
 * ```
 *
 * ## Lifecycle
 *
 * 1. [startAnimation] - Called when the back gesture begins
 * 2. [updateProgress] - Called repeatedly as the gesture progresses
 * 3. [completeAnimation] or [cancelAnimation] - Called when the gesture ends
 *
 * @see BackNavigationEvent
 * @see LocalBackAnimationController
 */
@InternalQuoVadisApi
@Stable
class BackAnimationController {

    /**
     * Whether a back gesture animation is currently in progress.
     *
     * When `true`, renderers should apply gesture-based animations.
     * When `false`, renderers should use standard transitions.
     */
    var isAnimating: Boolean by mutableStateOf(false)
        private set

    /**
     * Current progress of the back gesture, ranging from 0.0 to 1.0.
     *
     * - 0.0 = Gesture just started
     * - 1.0 = Gesture at or beyond completion threshold
     *
     * Renderers can use this value to interpolate animation parameters
     * like offset, scale, or alpha.
     */
    var progress: Float by mutableStateOf(0f)
        private set

    /**
     * The most recent back navigation event with full gesture details.
     *
     * Contains:
     * - [BackNavigationEvent.progress] - Same as [progress]
     * - [BackNavigationEvent.touchX] - X coordinate of touch
     * - [BackNavigationEvent.touchY] - Y coordinate of touch
     * - [BackNavigationEvent.swipeEdge] - Which edge the swipe started from
     *
     * This is `null` when no gesture is active.
     */
    var currentEvent: BackNavigationEvent? by mutableStateOf(null)
        private set

    /**
     * Starts a new back gesture animation.
     *
     * Called when the user begins a back gesture. Sets [isAnimating] to `true`
     * and initializes [progress] and [currentEvent] from the provided event.
     *
     * @param event The initial back navigation event
     */
    fun startAnimation(event: BackNavigationEvent) {
        isAnimating = true
        progress = event.progress
        currentEvent = event
    }

    /**
     * Updates the animation progress during an active gesture.
     *
     * Called repeatedly as the user continues the back gesture.
     * Updates [progress] and [currentEvent] with the latest values.
     *
     * @param event The latest back navigation event
     */
    fun updateProgress(event: BackNavigationEvent) {
        progress = event.progress
        currentEvent = event
    }

    /**
     * Completes the back gesture animation.
     *
     * Called when the user successfully completes the back gesture
     * (released past the threshold). Resets all animation state.
     *
     * After this call, navigation will occur and content will change.
     */
    fun completeAnimation() {
        isAnimating = false
        progress = 0f
        currentEvent = null
    }

    /**
     * Cancels the back gesture animation.
     *
     * Called when the user cancels the back gesture (released before
     * reaching the threshold). Resets all animation state.
     *
     * After this call, content remains unchanged and animations should
     * reverse/reset to the original state.
     */
    fun cancelAnimation() {
        isAnimating = false
        progress = 0f
        currentEvent = null
    }
}

/**
 * CompositionLocal providing access to the current [BackAnimationController].
 *
 * This is provided by [NavigationHost] and allows any composable in the
 * navigation hierarchy to observe and react to predictive back gestures.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreenContent() {
 *     val backController = LocalBackAnimationController.current
 *
 *     val alpha = if (backController?.isAnimating == true) {
 *         1f - (backController.progress * 0.3f)
 *     } else 1f
 *
 *     Box(modifier = Modifier.alpha(alpha)) {
 *         // Content dims slightly during back gesture
 *     }
 * }
 * ```
 *
 * @see BackAnimationController
 */
val LocalBackAnimationController = compositionLocalOf<BackAnimationController?> { null }

/**
 * Remember a [BackAnimationController] instance.
 *
 * Creates and remembers a [BackAnimationController] for use within a navigation host.
 * The controller persists across recompositions.
 *
 * @return A remembered [BackAnimationController] instance
 */
@Composable
fun rememberBackAnimationController(): BackAnimationController {
    return remember { BackAnimationController() }
}
