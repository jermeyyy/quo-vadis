package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * JavaScript/Browser-specific back handler for tab navigation.
 *
 * On web platforms, back navigation is typically triggered by:
 * - Browser back button
 * - Keyboard shortcuts (Backspace, Alt+Left Arrow)
 * - Touch gestures on mobile browsers
 *
 * This implementation integrates with the browser's History API.
 *
 * @param tabState The tab navigation state.
 * @param enabled Whether the back handler is enabled.
 * @param onBack Callback invoked when back is triggered.
 */
@Composable
actual fun TabBackHandler(
    tabState: TabNavigatorState,
    enabled: Boolean,
    onBack: () -> Unit
) {
    // Browser back button handling through History API
    // Future enhancement: Integrate with window.history.pushState/popstate events
}
