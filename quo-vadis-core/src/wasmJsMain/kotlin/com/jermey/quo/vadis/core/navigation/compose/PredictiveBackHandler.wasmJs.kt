package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

/**
 * WebAssembly/JavaScript implementation of PredictiveBackHandler.
 *
 * Predictive back gestures are not supported in browser environments.
 * This is a no-op implementation that simply renders the content.
 *
 * Back navigation in browsers is typically handled through:
 * - Browser back button
 * - Keyboard shortcuts (Alt+Left Arrow on Windows, Cmd+Left Arrow on macOS)
 * - History API (`popstate` events)
 *
 * @param enabled Ignored on this platform
 * @param callback Ignored on this platform
 * @param content The content to render
 */
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
) {
    // Predictive back is not supported in WebAssembly/browser environments
    // Simply render content without gesture handling
    content()
}
