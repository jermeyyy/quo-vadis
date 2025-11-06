package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Animation constants
private const val SCALE_FACTOR = 0.1f
private const val TRANSLATION_FACTOR = 100f
private const val ALPHA_FACTOR = 0.2f
private const val COMPLETE_SCALE = 0.8f
private const val COMPLETE_TRANSLATION = 200f
private const val COMPLETE_ALPHA = 0f

/**
 * Modifier that applies predictive back animation to tab content.
 *
 * This modifier applies visual feedback during predictive back gestures
 * on platforms that support it (Android 33+, iOS).
 *
 * The animation:
 * - Scales and translates the current tab during gesture
 * - Reveals the previous tab (or animates tab switch)
 * - Smoothly completes or cancels based on gesture completion
 *
 * @param tabState The tab navigation state.
 * @param enabled Whether predictive back is enabled.
 * @return Modifier with predictive back animation applied.
 */
@Composable
fun Modifier.tabPredictiveBack(
    tabState: TabNavigatorState,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this

    val predictiveBackState = rememberTabPredictiveBackState(tabState)

    return this.graphicsLayer(
        scaleX = predictiveBackState.scale.value,
        scaleY = predictiveBackState.scale.value,
        translationX = predictiveBackState.translationX.value,
        alpha = predictiveBackState.alpha.value
    )
}

/**
 * State holder for tab predictive back animations.
 *
 * Manages animation state for:
 * - Gesture progress (scale, translation, alpha)
 * - Gesture completion (animate to final state)
 * - Gesture cancellation (restore to original state)
 */
@Stable
class TabPredictiveBackState internal constructor(
    private val tabState: TabNavigatorState,
    private val scope: CoroutineScope
) {
    internal val scale = Animatable(1f)
    internal val translationX = Animatable(0f)
    internal val alpha = Animatable(1f)

    /**
     * Update animation based on predictive back gesture progress.
     *
     * @param progress Gesture progress from 0.0 (start) to 1.0 (complete).
     */
    fun onGestureProgress(progress: Float) {
        scope.launch {
            // Scale down slightly as gesture progresses
            scale.snapTo(1f - (progress * SCALE_FACTOR))

            // Translate to the right (revealing content behind)
            translationX.snapTo(progress * TRANSLATION_FACTOR)

            // Fade out slightly
            alpha.snapTo(1f - (progress * ALPHA_FACTOR))
        }
    }

    /**
     * Complete the predictive back gesture animation.
     *
     * Called when the user completes the back gesture.
     */
    fun onGestureComplete() {
        scope.launch {
            // Animate to fully hidden state
            launch { scale.animateTo(COMPLETE_SCALE, animationSpec = spring()) }
            launch { translationX.animateTo(COMPLETE_TRANSLATION, animationSpec = spring()) }
            launch { alpha.animateTo(COMPLETE_ALPHA, animationSpec = spring()) }

            // Then reset for next navigation
            scale.snapTo(1f)
            translationX.snapTo(0f)
            alpha.snapTo(1f)
        }
    }

    /**
     * Cancel the predictive back gesture animation.
     *
     * Called when the user cancels the back gesture.
     */
    fun onGestureCancel() {
        scope.launch {
            // Animate back to original state
            launch { scale.animateTo(1f, animationSpec = spring()) }
            launch { translationX.animateTo(0f, animationSpec = spring()) }
            launch { alpha.animateTo(1f, animationSpec = spring()) }
        }
    }
}

/**
 * Remember a predictive back state for tab navigation.
 *
 * @param tabState The tab navigation state.
 * @return A stable [TabPredictiveBackState] instance.
 */
@Composable
fun rememberTabPredictiveBackState(
    tabState: TabNavigatorState
): TabPredictiveBackState {
    val scope = rememberCoroutineScope()
    return remember(tabState) {
        TabPredictiveBackState(tabState, scope)
    }
}

/**
 * Platform-specific back handler for tab navigation.
 *
 * This composable provides platform-appropriate back handling:
 * - **Android (API 33+)**: Predictive back gesture with animations
 * - **iOS**: Swipe-back gesture support
 * - **Desktop/Web**: Keyboard/mouse back button support
 *
 * The back handler integrates with [TabNavigatorState] to provide
 * intelligent back navigation across tabs.
 *
 * @param tabState The tab navigation state.
 * @param enabled Whether the back handler is enabled.
 * @param onBack Callback invoked when back is pressed/gestured.
 */
@Composable
expect fun TabBackHandler(
    tabState: TabNavigatorState,
    enabled: Boolean = true,
    onBack: () -> Unit
)
