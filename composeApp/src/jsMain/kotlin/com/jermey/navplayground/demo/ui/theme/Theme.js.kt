package com.jermey.navplayground.demo.ui.theme

import androidx.compose.runtime.Composable

/**
 * Web (JS) implementation for configuring system UI.
 * Web browsers handle UI automatically, no explicit configuration needed.
 *
 * @param useDarkTheme Whether dark theme is active (unused on web)
 */
@Composable
actual fun ConfigureSystemUI(useDarkTheme: Boolean) {
    // No-op on web - browsers handle theme/UI automatically
}
