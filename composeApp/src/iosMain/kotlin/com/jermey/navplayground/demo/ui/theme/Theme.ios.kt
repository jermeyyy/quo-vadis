package com.jermey.navplayground.demo.ui.theme

import androidx.compose.runtime.Composable

/**
 * iOS-specific implementation for configuring system UI.
 * iOS automatically handles status bar appearance based on the content,
 * so no explicit configuration is needed.
 *
 * @param useDarkTheme Whether dark theme is active (unused on iOS)
 */
@Composable
actual fun ConfigureSystemUI(useDarkTheme: Boolean) {
    // No-op on iOS - system handles status bar appearance automatically
}
