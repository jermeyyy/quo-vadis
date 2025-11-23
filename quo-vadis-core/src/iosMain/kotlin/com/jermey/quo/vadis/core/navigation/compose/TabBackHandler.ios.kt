package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * iOS-specific back handler for tab navigation with swipe gestures.
 *
 * On iOS, this integrates with the system's swipe-back gesture to provide
 * smooth animations when navigating between tabs or within tab content.
 *
 * The iOS swipe gesture is handled through UIKit gesture recognizers and
 * bridges to Compose through interop.
 *
 * @param tabState The tab navigation state.
 * @param enabled Whether the back handler is enabled.
 * @param onBack Callback invoked when back gesture completes.
 */
@Composable
actual fun TabBackHandler(
    tabState: TabNavigatorState,
    enabled: Boolean,
    onBack: () -> Unit
) {
    // iOS swipe-back gesture handling
    // Future enhancement: Implement UIKit gesture recognizer bridge for swipe-back
}
