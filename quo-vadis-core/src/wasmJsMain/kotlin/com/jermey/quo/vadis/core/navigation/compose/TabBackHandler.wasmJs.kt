package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * WebAssembly-specific back handler for tab navigation.
 *
 * On Wasm platforms running in browsers, back navigation is handled
 * similarly to JavaScript, integrating with the browser's History API.
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
    // Browser back button handling through History API (Wasm context)
    // For now, back handling is triggered by browser navigation events
}
