package com.jermey.quo.vadis.core.compose.navback

import androidx.compose.runtime.Composable

/**
 * Android implementation of platform back input registration.
 *
 * On Android, the Activity already provides NavigationEventDispatcher
 * via `ComponentActivity.navigationEventDispatcher` (since Activity 1.12.0).
 * The NavigationBackHandler automatically connects to it through the
 * `LocalNavigationEventDispatcherOwner` composition local.
 *
 * No additional registration is needed here as the system handles
 * back gesture events through the OnBackInvokedDispatcher.
 */
@Composable
internal actual fun RegisterPlatformBackInput() {
    // On Android, the Activity already provides NavigationEventDispatcher
    // via ComponentActivity.navigationEventDispatcher (since Activity 1.12.0)
    // The NavigationBackHandler automatically connects to it.
    // No additional registration needed.
}