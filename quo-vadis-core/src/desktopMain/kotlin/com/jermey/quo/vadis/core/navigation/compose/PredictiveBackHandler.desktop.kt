package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

/**
 * Desktop implementation of PredictiveBackHandler.
 *
 * Predictive back gestures are not supported on Desktop.
 * This is a no-op implementation that simply renders the content.
 *
 * Back navigation on Desktop is typically handled through:
 * - Keyboard shortcuts (e.g., Escape key, Alt+Left Arrow)
 * - Explicit back buttons in the UI
 * - Mouse back button (if supported by hardware)
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
    // Predictive back is not supported on Desktop
    // Simply render content without gesture handling
    content()
}
