package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * Desktop-specific back handler for tab navigation.
 *
 * On desktop platforms, back navigation is typically triggered by:
 * - Mouse back button
 * - Keyboard shortcuts (Alt+Left Arrow, Backspace)
 * - System back button (if available)
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
    // Desktop platforms don't have system back gestures
    // Back handling is typically done through keyboard shortcuts or UI buttons
    // For now, this is a no-op; actual back handling will be triggered by
    // explicit UI elements or keyboard event handlers
}
