package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable

/**
 * JavaScript implementation of platform back input registration.
 *
 * Web browsers handle back navigation through the browser's history API.
 * There is no native back gesture on web platforms.
 *
 * Browser back button integration (if needed) would be handled separately
 * via the History API and popstate events.
 *
 * This is a no-op implementation as there is no system back gesture to register.
 */
@Composable
internal actual fun RegisterPlatformBackInput() {
    // No platform back gesture on JS - noop
    // Browser back button uses browser history API
}
