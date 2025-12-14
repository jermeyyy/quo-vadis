package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable

/**
 * iOS implementation of platform back input registration.
 *
 * On iOS, back navigation is triggered via edge swipe gestures from the left side
 * of the screen. The gesture detection is implemented via [IOSEdgeSwipeGestureDetector]
 * which should wrap the navigation host content.
 *
 * ## Architecture
 *
 * Unlike Android (which has system-level `OnBackInvokedDispatcher`), iOS relies on
 * Compose-level gesture detection:
 *
 * 1. [IOSEdgeSwipeGestureDetector] wraps the navigation content
 * 2. Detects horizontal drags starting from the left edge (within 20dp threshold)
 * 3. Reports progress to callbacks for animation
 * 4. Commits navigation if gesture completes past 50% threshold
 *
 * ## Integration
 *
 * The [QuoVadisBackHandler] on iOS should use [IOSEdgeSwipeGestureDetector] internally
 * to provide platform-appropriate back gesture handling while maintaining the same
 * callback interface as Android.
 *
 * This function is a no-op because iOS gesture detection is done at the composable
 * level via [IOSEdgeSwipeGestureDetector], not through platform-level registration.
 *
 * @see IOSEdgeSwipeGestureDetector
 * @see QuoVadisBackHandler
 */
@Composable
internal actual fun RegisterPlatformBackInput() {
    // iOS back gesture is handled via IOSEdgeSwipeGestureDetector which wraps
    // navigation content in NavigationHost. No additional platform registration
    // is needed here since iOS doesn't have a system-level back dispatcher like Android.
    //
    // The gesture flow on iOS:
    // User swipe → IOSEdgeSwipeGestureDetector → QuoVadisBackHandler callbacks → Navigation
}
