@file:Suppress("unused")

package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

// =============================================================================
// PREDICTIVE BACK CALLBACK INTERFACE
// =============================================================================

/**
 * Callback interface for handling predictive back gestures.
 *
 * This interface defines the contract for components that want to respond to
 * predictive back gestures. The lifecycle follows:
 *
 * 1. [onBackStarted] - Gesture begins, return true to handle
 * 2. [onBackProgress] - Called repeatedly as gesture progresses
 * 3. [onBackCancelled] OR [onBackCommitted] - Gesture ends
 *
 * ## Usage
 *
 * ```kotlin
 * val callback = object : PredictiveBackCallback {
 *     override fun onBackStarted(): Boolean {
 *         // Return true if we can handle back
 *         return canNavigateBack
 *     }
 *
 *     override fun onBackProgress(progress: Float) {
 *         // Update UI based on gesture progress
 *     }
 *
 *     override fun onBackCancelled() {
 *         // Reset UI to original state
 *     }
 *
 *     override fun onBackCommitted() {
 *         // Complete the back navigation
 *     }
 * }
 * ```
 *
 * @see PredictiveBackCoordinator
 * @see PredictiveBackHandler
 */
interface PredictiveBackCallback {
    /**
     * Called when a back gesture starts.
     *
     * Implementations should check if back navigation is possible and
     * prepare for the gesture by computing the speculative pop state.
     *
     * @return true if the gesture should be handled, false to ignore
     */
    fun onBackStarted(): Boolean

    /**
     * Called repeatedly as the back gesture progresses.
     *
     * @param progress Gesture progress from 0.0 (start) to 1.0 (threshold)
     */
    fun onBackProgress(progress: Float)

    /**
     * Called when the back gesture is cancelled.
     *
     * The user released the gesture without completing it.
     * Implementations should reset any visual state to the original.
     */
    fun onBackCancelled()

    /**
     * Called when the back gesture is committed.
     *
     * The user completed the gesture and navigation should occur.
     * Implementations should finalize the navigation state change.
     */
    fun onBackCommitted()
}

// =============================================================================
// PREDICTIVE BACK HANDLER (EXPECT)
// =============================================================================

/**
 * A composable that handles predictive back gestures.
 *
 * This is an expect function with platform-specific actual implementations.
 * On Android, it integrates with the system's predictive back gesture API.
 * On other platforms, it may provide fallback behavior or no-op implementations.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun NavigationContent() {
 *     val callback = object : PredictiveBackCallback {
 *         override fun onBackStarted() = canNavigateBack
 *         override fun onBackProgress(progress: Float) { /* Update UI */ }
 *         override fun onBackCancelled() { /* Reset UI */ }
 *         override fun onBackCommitted() { /* Complete navigation */ }
 *     }
 *
 *     PredictiveBackHandler(
 *         enabled = true,
 *         callback = callback
 *     ) {
 *         // Your navigation content here
 *     }
 * }
 * ```
 *
 * ## Platform Behavior
 *
 * - **Android**: Integrates with `OnBackPressedCallback` and predictive back APIs
 * - **iOS**: May integrate with swipe-to-go-back gesture
 * - **Desktop/Web**: Typically no-op, back handled via UI buttons
 *
 * @param enabled Whether predictive back handling is enabled
 * @param callback The callback to receive gesture events
 * @param content The composable content to wrap
 *
 * @see PredictiveBackCallback
 */
@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
)
