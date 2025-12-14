package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * iOS edge swipe gesture input that generates NavigationEvents.
 *
 * Detects swipes from the left edge and converts them to NavigationEvent flow
 * for consistent handling with Android predictive back.
 *
 * ## Gesture Detection
 *
 * The detector recognizes horizontal drags that start within the left edge threshold
 * (default 20dp from the edge). Once a gesture starts:
 * - Progress is calculated based on how far the finger has traveled
 * - Progress values range from 0.0 (at edge) to 1.0 (at complete threshold)
 * - If released past the commit threshold (50%), navigation completes
 * - If released before commit threshold, the gesture is cancelled
 *
 * ## Usage
 *
 * ```kotlin
 * IOSEdgeSwipeGestureDetector(
 *     enabled = canGoBack,
 *     onGestureStart = { true }, // Return true if handling
 *     onProgress = { progress -> /* Animate with progress */ },
 *     onComplete = { navigator.goBack() },
 *     onCancel = { /* Reset animation */ }
 * ) {
 *     // Screen content
 * }
 * ```
 *
 * @param enabled Whether edge swipe detection is enabled. When false, gestures pass through.
 * @param onGestureStart Called when a gesture starts from the edge. Return `true` to indicate
 *                       the gesture should be handled, `false` to let it pass through.
 * @param onProgress Called with progress (0.0 to 1.0) during gesture. Use this to drive animations.
 * @param onComplete Called when the gesture is completed (finger lifted past commit threshold).
 *                   Perform the actual navigation here.
 * @param onCancel Called when the gesture is cancelled (finger lifted before commit threshold
 *                 or dragged back to start). Reset animation state here.
 * @param content The content to wrap with edge swipe detection.
 */
@Composable
internal fun IOSEdgeSwipeGestureDetector(
    enabled: Boolean,
    onGestureStart: () -> Boolean,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var isHandling by remember { mutableStateOf(false) }
    var startX by remember { mutableFloatStateOf(0f) }
    var currentProgress by remember { mutableFloatStateOf(0f) }

    // Edge detection threshold - how close to the left edge the gesture must start
    val edgeThreshold = with(density) { EDGE_THRESHOLD_DP.dp.toPx() }
    // Complete threshold - how far the finger must travel for full progress
    val completeThreshold = with(density) { COMPLETE_THRESHOLD_DP.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        // Only start handling if the gesture begins near the left edge
                        if (offset.x <= edgeThreshold) {
                            startX = offset.x
                            isHandling = onGestureStart()
                            currentProgress = 0f
                            if (isHandling) {
                                onProgress(0f)
                            }
                        }
                    },
                    onDragEnd = {
                        if (isHandling) {
                            // Commit if past the threshold, otherwise cancel
                            if (currentProgress >= COMMIT_THRESHOLD) {
                                onComplete()
                            } else {
                                onCancel()
                            }
                            isHandling = false
                            currentProgress = 0f
                        }
                    },
                    onDragCancel = {
                        if (isHandling) {
                            onCancel()
                            isHandling = false
                            currentProgress = 0f
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (isHandling) {
                            // Calculate progress based on distance from start position
                            currentProgress = ((change.position.x - edgeThreshold) / completeThreshold)
                                .coerceIn(0f, 1f)
                            onProgress(currentProgress)
                        }
                    }
                )
            }
    ) {
        content()
    }
}

/**
 * Configuration constants for iOS edge swipe gesture detection.
 */
private const val EDGE_THRESHOLD_DP = 20
private const val COMPLETE_THRESHOLD_DP = 100
private const val COMMIT_THRESHOLD = 0.5f
