package com.jermey.quo.vadis.core.navigation.compose

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler as AndroidPredictiveBackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Android implementation of [PredictiveBackHandler].
 *
 * Uses the AndroidX `PredictiveBackHandler` API to integrate with Android's
 * predictive back gesture system. On Android 14+ (API 34+), this provides
 * a smooth preview animation during the back gesture. On older versions,
 * the gesture still works but without the visual preview.
 *
 * ## Implementation Details
 *
 * The AndroidX `PredictiveBackHandler` provides a [Flow] of [BackEventCompat]
 * events during the gesture:
 *
 * - **Flow emission**: Each emission contains progress (0.0 to 1.0)
 * - **Flow completion**: Gesture was committed (released past threshold)
 * - **Flow cancellation**: Gesture was cancelled (swiped back)
 *
 * ## Lifecycle
 *
 * ```
 * User starts gesture
 *     │
 *     ▼
 * AndroidPredictiveBackHandler lambda invoked
 *     │
 *     ▼
 * callback.onBackStarted()
 *     │
 *     ▼ (if returns true)
 * ┌─────────────────────────┐
 * │ progress.collect { }    │ ◄── callback.onBackProgress()
 * └─────────────────────────┘
 *     │
 *     ├── Flow completes normally ──► callback.onBackCommitted()
 *     │
 *     └── CancellationException ──► callback.onBackCancelled()
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * PredictiveBackHandler(
 *     enabled = canGoBack,
 *     callback = predictiveBackCoordinator
 * ) {
 *     NavigationContent()
 * }
 * ```
 *
 * @param enabled Whether predictive back handling is enabled. When false,
 *                the gesture is not intercepted and passes through to the system.
 * @param callback The callback to receive gesture events. See [PredictiveBackCallback].
 * @param content The composable content to wrap with predictive back handling.
 *
 * @see PredictiveBackCallback
 * @see PredictiveBackCoordinator
 * @see androidx.activity.compose.PredictiveBackHandler
 */
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
) {
    // Track whether we're actively handling a gesture
    var isHandling by remember { mutableStateOf(false) }

    AndroidPredictiveBackHandler(enabled = enabled) { progress: Flow<BackEventCompat> ->
        // Gesture started - notify callback and check if we should handle
        isHandling = callback.onBackStarted()
        if (!isHandling) {
            // Callback declined to handle this gesture
            return@AndroidPredictiveBackHandler
        }

        try {
            // Collect progress events during gesture
            progress.collect { backEvent ->
                // BackEventCompat.progress is already normalized to 0-1 range
                callback.onBackProgress(backEvent.progress)
            }

            // Flow completed normally - gesture was committed
            // (user released past threshold)
            callback.onBackCommitted()
        } catch (e: CancellationException) {
            // Flow was cancelled - gesture was cancelled
            // (user swiped back before threshold)
            callback.onBackCancelled()

            // Re-throw to maintain proper coroutine cancellation semantics
            throw e
        } finally {
            // Reset handling state regardless of outcome
            isHandling = false
        }
    }

    // Render the wrapped content
    content()
}
