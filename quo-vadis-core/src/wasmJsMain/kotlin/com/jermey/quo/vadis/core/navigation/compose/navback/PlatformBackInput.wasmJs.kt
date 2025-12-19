package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable

/**
 * WASM JavaScript implementation of platform back input registration.
 *
 * WASM JS runs in a web browser environment and, like regular JS,
 * does not have a native back gesture.
 *
 * Browser back button integration (if needed) would be handled separately
 * via the History API and popstate events.
 *
 * This is a no-op implementation as there is no system back gesture to register.
 */
@Composable
internal actual fun RegisterPlatformBackInput() {
    // No platform back gesture on WASM JS - noop
    // Browser back button uses browser history API
}
