@file:Suppress("unused")

package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TransitionState
import com.jermey.quo.vadis.core.navigation.core.TransitionStateManager
import com.jermey.quo.vadis.core.navigation.core.TreeMutator

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
// PREDICTIVE BACK COORDINATOR
// =============================================================================

/**
 * Coordinates predictive back gestures with navigation state.
 *
 * This class implements the speculative pop algorithm for predictive back:
 *
 * 1. When a gesture starts, compute the pop result via [TreeMutator.pop]
 * 2. Start a proposed transition with [TransitionStateManager.startProposed]
 * 3. Update progress as the gesture progresses
 * 4. On cancel, revert to original state via [TransitionStateManager.cancelProposed]
 * 5. On commit, apply the speculative state via [TransitionStateManager.commitProposed]
 *
 * ## State Flow
 *
 * ```
 * onBackStarted()
 *     │
 *     ▼
 * [Check if idle] ──No──► return false
 *     │
 *     Yes
 *     │
 *     ▼
 * [Compute speculative pop] ──null──► return false
 *     │
 *     non-null
 *     │
 *     ▼
 * [startProposed(speculativeState)]
 *     │
 *     return true
 *     │
 *     ▼
 * ┌───────────────────┐
 * │  Gesture active   │◄─────────────┐
 * └───────────────────┘              │
 *     │                              │
 * onBackProgress(progress)───────────┘
 *     │
 *     ▼
 * ┌───────────────────┬───────────────────┐
 * │                   │                   │
 * onBackCancelled()   onBackCommitted()
 * │                   │
 * cancelProposed()    commitProposed()
 * │                   updateState(speculative)
 * │                   │
 * └───────────────────┴───────────────────┘
 * ```
 *
 * ## Usage
 *
 * ```kotlin
 * val coordinator = PredictiveBackCoordinator(navigator, transitionManager)
 *
 * PredictiveBackHandler(
 *     enabled = true,
 *     callback = coordinator
 * ) {
 *     // Navigation content
 * }
 * ```
 *
 * @param navigator The navigator to update state on commit
 * @param transitionManager The transition state manager for gesture coordination
 *
 * @see PredictiveBackCallback
 * @see PredictiveBackHandler
 */
class PredictiveBackCoordinator(
    private val navigator: Navigator,
    private val transitionManager: TransitionStateManager
) : PredictiveBackCallback {

    /**
     * Holds the speculative pop result during an active gesture.
     *
     * This is computed in [onBackStarted] and used in [onBackCommitted]
     * to update the navigator's state.
     */
    private var speculativeState: NavNode? = null

    /**
     * Handles the start of a back gesture.
     *
     * Checks if the transition manager is idle, computes the speculative
     * pop result, and starts a proposed transition if valid.
     *
     * @return true if the gesture should be handled, false otherwise
     */
    override fun onBackStarted(): Boolean {
        // Check if transition manager is in idle state
        if (!transitionManager.currentState.isIdle) {
            return false
        }

        // Get current navigation state
        val currentState = navigator.state.value

        // Compute speculative pop result
        val popResult = TreeMutator.pop(currentState)
        if (popResult == null) {
            // Cannot pop - at root or empty stack
            return false
        }

        // Store speculative state for later commit
        speculativeState = popResult

        // Start proposed transition
        transitionManager.startProposed(popResult)

        return true
    }

    /**
     * Updates the transition progress during a back gesture.
     *
     * @param progress Gesture progress from 0.0 to 1.0
     */
    override fun onBackProgress(progress: Float) {
        transitionManager.updateProgress(progress)
    }

    /**
     * Cancels the back gesture and reverts to original state.
     *
     * Clears the speculative state and cancels the proposed transition,
     * returning the transition manager to idle with the original state.
     */
    override fun onBackCancelled() {
        speculativeState = null
        transitionManager.cancelProposed()
    }

    /**
     * Commits the back gesture and applies the navigation change.
     *
     * Commits the proposed transition and updates the navigator's
     * state to the speculative pop result.
     */
    override fun onBackCommitted() {
        val targetState = speculativeState ?: return

        // Commit the proposed transition (moves to Animating state)
        transitionManager.commitProposed()

        // Update navigator state to the speculative result
        navigator.updateState(targetState)

        // Clear speculative state
        speculativeState = null
    }
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
 *     val coordinator = remember { PredictiveBackCoordinator(navigator, transitionManager) }
 *
 *     PredictiveBackHandler(
 *         enabled = true,
 *         callback = coordinator
 *     ) {
 *         // Your navigation content here
 *         QuoVadisHost(navigator = navigator) { destination ->
 *             // Render destinations
 *         }
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
 * @see PredictiveBackCoordinator
 */
@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
)
