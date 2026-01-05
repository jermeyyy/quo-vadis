package com.jermey.quo.vadis.core.compose.internal.navback

import androidx.compose.runtime.Immutable

/**
 * Platform-agnostic representation of a back navigation event.
 * This class wraps the NavigationEvent API to provide a stable interface
 * that can be used across all platforms.
 */
@Immutable
data class BackNavigationEvent(
    /** Progress of the back gesture, 0.0 to 1.0 */
    val progress: Float,
    /** X coordinate of the touch point */
    val touchX: Float = 0f,
    /** Y coordinate of the touch point */
    val touchY: Float = 0f,
    /** Which edge the swipe started from */
    val swipeEdge: Int = EDGE_LEFT
) {
    companion object {
        const val EDGE_LEFT: Int = 0
        const val EDGE_RIGHT: Int = 1
    }
}

/**
 * Represents the state of a back navigation transition.
 */
sealed interface BackTransitionState {
    /** No back gesture in progress */
    data object Idle : BackTransitionState

    /** Back gesture is in progress with the given event */
    data class InProgress(
        val event: BackNavigationEvent
    ) : BackTransitionState
}
