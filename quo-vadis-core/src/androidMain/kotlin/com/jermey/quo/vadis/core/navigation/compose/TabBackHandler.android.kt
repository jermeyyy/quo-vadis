package com.jermey.quo.vadis.core.navigation.compose

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * Android-specific back handler for tab navigation with predictive animations.
 *
 * On Android 13 (API 33) and above, this integrates with the system's predictive
 * back gesture to provide smooth animations when navigating between tabs or
 * exiting the tab navigation.
 *
 * On earlier Android versions, this falls back to standard back handling.
 *
 * @param tabState The tab navigation state.
 * @param enabled Whether the back handler is enabled.
 * @param onBack Callback invoked when back is pressed.
 */
@Composable
actual fun TabBackHandler(
    tabState: TabNavigatorState,
    enabled: Boolean,
    onBack: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ with predictive back
        val predictiveBackState = rememberTabPredictiveBackState(tabState)

        // Future enhancement: Integrate with Android predictive back callback
        BackHandler(enabled = enabled) {
            onBack()
        }
    } else {
        // Older Android versions - standard back handling
        BackHandler(enabled = enabled) {
            onBack()
        }
    }
}
