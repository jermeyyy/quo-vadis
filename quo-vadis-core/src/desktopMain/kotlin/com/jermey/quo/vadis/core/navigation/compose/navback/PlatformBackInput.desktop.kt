package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable

/**
 * Desktop implementation of platform back input registration.
 *
 * Desktop platforms do not have a native back gesture like mobile devices.
 * Back navigation on desktop is typically handled via:
 * - Keyboard shortcuts (e.g., Alt+Left, Backspace)
 * - UI back buttons
 * - Mouse navigation buttons
 *
 * This is a no-op implementation as there is no system back gesture to register.
 */
@Composable
internal actual fun RegisterPlatformBackInput() {
    // No platform back gesture on Desktop - noop
    // Back navigation is handled via keyboard shortcuts or UI buttons
}
