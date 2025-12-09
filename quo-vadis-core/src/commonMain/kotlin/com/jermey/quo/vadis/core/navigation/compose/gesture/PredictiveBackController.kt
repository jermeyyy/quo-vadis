package com.jermey.quo.vadis.core.navigation.compose.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow

/**
 * Progress value representing the gesture threshold during predictive back.
 *
 * During an active gesture, progress is clamped to this maximum value to prevent
 * the animation from progressing too far before the gesture is committed.
 * The full 0-1 range is only used during completion/cancellation animations.
 */
private const val GESTURE_MAX_PROGRESS = 0.25f

/**
 * Centralized controller for predictive back gesture handling.
 *
 * This class manages the state and animations for predictive back gestures,
 * providing a unified interface for renderers to respond to back gesture
 * interactions. It tracks gesture progress, handles completion and cancellation
 * animations, and exposes observable state for UI updates.
 *
 * ## Architecture
 *
 * The controller operates in three phases:
 *
 * 1. **Idle**: No gesture active, [isActive] is `false`, [progress] is `0f`
 * 2. **Gesturing**: User is dragging back, [isActive] is `true`, [progress] reflects gesture
 * 3. **Animating**: Gesture ended, animating to completion or cancellation
 *
 * ## Progress Clamping
 *
 * During the gesture phase, progress is clamped to a maximum of [GESTURE_MAX_PROGRESS] (0.25f).
 * This prevents excessive movement during the gesture while still providing visual feedback.
 * The full 0-1 range is used during completion animation.
 *
 * ## Usage
 *
 * ```kotlin
 * val predictiveBackController = remember { PredictiveBackController() }
 *
 * // In your renderer
 * if (predictiveBackController.isActive.value) {
 *     // Use progress for gesture-driven animations
 *     val scale = 1f - (predictiveBackController.progress.value * 0.1f)
 *     Box(modifier = Modifier.scale(scale)) {
 *         content()
 *     }
 * } else {
 *     // Use standard AnimatedContent
 *     AnimatedContent(targetState) { ... }
 * }
 * ```
 *
 * ## Platform Integration
 *
 * This controller is platform-agnostic. Platform-specific predictive back handlers
 * (e.g., Android's `PredictiveBackHandler`) should call [handleGesture] with their
 * back event flow.
 *
 * @see handleGesture
 */
@Stable
public class PredictiveBackController {

    // region Internal Mutable State

    /**
     * Internal mutable state for gesture active status.
     */
    private var _isActive by mutableStateOf(false)

    /**
     * Internal mutable state for gesture progress.
     */
    private var _progress by mutableFloatStateOf(0f)

    /**
     * Animatable for smooth completion and cancellation animations.
     */
    private val progressAnimatable = Animatable(0f)

    // endregion

    // region Public State

    /**
     * Whether a predictive back gesture is currently active.
     *
     * This is `true` during:
     * - Active gesture dragging
     * - Completion animation (after gesture committed)
     * - Cancellation animation (after gesture cancelled)
     *
     * Renderers should check this state to determine whether to use
     * gesture-driven rendering or standard animated transitions.
     *
     * ## Example
     *
     * ```kotlin
     * if (predictiveBackController.isActive.value) {
     *     PredictiveBackContent(progress = predictiveBackController.progress.value)
     * } else {
     *     AnimatedNavContent(currentScreen)
     * }
     * ```
     */
    public val isActive: State<Boolean>
        get() = object : State<Boolean> {
            override val value: Boolean get() = _isActive
        }

    /**
     * Current progress of the predictive back gesture, from 0f to 1f.
     *
     * ## Progress Ranges
     *
     * - **During gesture**: 0f to 0.25f (clamped to prevent excessive movement)
     * - **During completion**: 0f to 1f (animated from current to 1f)
     * - **During cancellation**: 0f (animated from current to 0f)
     * - **Idle**: 0f
     *
     * ## Usage for Transforms
     *
     * Use this value to drive visual transforms during the gesture:
     *
     * ```kotlin
     * val progress = predictiveBackController.progress.value
     * 
     * // Scale: 1.0 -> 0.9 as progress increases
     * val scale = 1f - (progress * 0.1f)
     * 
     * // Translation: 0 -> width * 0.25 as progress increases
     * val translationX = width * progress
     * 
     * Box(modifier = Modifier.graphicsLayer {
     *     scaleX = scale
     *     scaleY = scale
     *     this.translationX = translationX
     * }) {
     *     content()
     * }
     * ```
     */
    public val progress: State<Float>
        get() = object : State<Float> {
            override val value: Float get() = _progress
        }

    // endregion

    // region Public API

    /**
     * Handles a predictive back gesture flow from the platform.
     *
     * This method should be called from platform-specific back handlers
     * (e.g., Android's `PredictiveBackHandler`). It manages the gesture
     * lifecycle and triggers navigation on completion.
     *
     * ## Lifecycle
     *
     * 1. Sets [isActive] to `true`
     * 2. Collects progress from [backEvent] flow, updating [progress]
     * 3. On flow completion (gesture committed): animates to 1f, calls [onNavigateBack]
     * 4. On flow cancellation (gesture cancelled): animates to 0f
     * 5. Sets [isActive] to `false` after animation completes
     *
     * ## Platform Integration
     *
     * ```kotlin
     * // Android
     * PredictiveBackHandler { backEventFlow ->
     *     predictiveBackController.handleGesture(
     *         backEvent = backEventFlow.map { it.progress },
     *         onNavigateBack = { navigator.navigateBack() }
     *     )
     * }
     * ```
     *
     * @param backEvent Flow of progress values (0f-1f) from the back gesture
     * @param onNavigateBack Callback invoked when gesture completes successfully
     */
    public suspend fun handleGesture(
        backEvent: Flow<Float>,
        onNavigateBack: () -> Unit
    ) {
        _isActive = true
        _progress = 0f

        try {
            // Collect gesture progress
            backEvent.collect { rawProgress ->
                // Clamp progress during gesture to prevent excessive movement
                _progress = (rawProgress * GESTURE_MAX_PROGRESS).coerceIn(0f, GESTURE_MAX_PROGRESS)
            }

            // Flow completed normally - gesture was committed
            animateToCompletion(onNavigateBack)
        } catch (_: Exception) {
            // Flow was cancelled - gesture was abandoned
            animateToCancel()
        }
    }

    // endregion

    // region Internal Animations

    /**
     * Animates progress to completion and triggers navigation.
     *
     * Uses a spring animation for natural feel. Progress animates from current
     * value to 1f, then [onComplete] is called to perform navigation.
     *
     * @param onComplete Callback invoked when animation completes
     */
    private suspend fun animateToCompletion(onComplete: () -> Unit) {
        progressAnimatable.snapTo(_progress)
        progressAnimatable.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) {
            _progress = value
        }

        // Animation complete - trigger navigation
        onComplete()

        // Reset state
        _isActive = false
        _progress = 0f
        progressAnimatable.snapTo(0f)
    }

    /**
     * Animates progress to cancellation (back to 0f).
     *
     * Uses a spring animation to smoothly return to the initial state
     * when the user cancels the back gesture.
     */
    private suspend fun animateToCancel() {
        progressAnimatable.snapTo(_progress)
        progressAnimatable.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) {
            _progress = value
        }

        // Animation complete - reset state
        _isActive = false
        _progress = 0f
    }

    // endregion
}
