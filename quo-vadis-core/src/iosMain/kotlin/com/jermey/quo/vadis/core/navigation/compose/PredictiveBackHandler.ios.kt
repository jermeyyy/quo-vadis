package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of [PredictiveBackHandler].
 *
 * Uses edge swipe gesture recognition to mimic iOS system back navigation.
 * Only responds to gestures that start from the left edge of the screen,
 * providing a native-feeling back gesture experience.
 *
 * ## Gesture Behavior
 *
 * - **Edge threshold**: Gesture must start within 20dp of the left edge
 * - **Complete threshold**: Swiping 100dp reaches full progress (1.0)
 * - **Progress calculation**: Linear interpolation between edge and complete threshold
 *
 * ## Gesture Lifecycle
 *
 * 1. User begins horizontal drag near left edge
 * 2. [PredictiveBackCallback.onBackStarted] is called
 * 3. As user drags right, [PredictiveBackCallback.onBackProgress] is called with progress 0.0-1.0
 * 4. User lifts finger: [PredictiveBackCallback.onBackCommitted] is called
 * 5. User cancels gesture: [PredictiveBackCallback.onBackCancelled] is called
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val coordinator = remember { PredictiveBackCoordinator(navigator, transitionManager) }
 *
 *     PredictiveBackHandler(
 *         enabled = true,
 *         callback = coordinator
 *     ) {
 *         // Screen content
 *     }
 * }
 * ```
 *
 * @param enabled Whether predictive back handling is enabled. When false, gestures are ignored.
 * @param callback The callback to receive gesture lifecycle events.
 * @param content The composable content to wrap with gesture detection.
 *
 * @see PredictiveBackCallback
 * @see PredictiveBackCoordinator
 */
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    // Track whether we're currently handling a back gesture
    var isHandling by remember { mutableStateOf(false) }

    // Track the starting X position of the gesture
    var startX by remember { mutableStateOf(0f) }

    // Edge threshold: gesture must start within 20dp of left edge
    val edgeThreshold = with(density) { 20.dp.toPx() }

    // Complete threshold: swipe 100dp to reach progress = 1.0
    val completeThreshold = with(density) { 100.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        // Only start if gesture begins near left edge
                        if (offset.x <= edgeThreshold) {
                            startX = offset.x
                            isHandling = callback.onBackStarted()
                        }
                    },
                    onDragEnd = {
                        if (isHandling) {
                            callback.onBackCommitted()
                            isHandling = false
                        }
                    },
                    onDragCancel = {
                        if (isHandling) {
                            callback.onBackCancelled()
                            isHandling = false
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (isHandling) {
                            val currentX = change.position.x
                            // Calculate progress: 0.0 at edge, 1.0 at complete threshold
                            val progress = ((currentX - edgeThreshold) / completeThreshold)
                                .coerceIn(0f, 1f)
                            callback.onBackProgress(progress)
                        }
                    }
                )
            }
    ) {
        content()
    }
}
