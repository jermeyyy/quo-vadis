package com.jermey.navplayground.demo.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Android-specific implementation for configuring system UI.
 * Updates status bar and navigation bar icon colors based on the theme.
 *
 * @param useDarkTheme Whether dark theme is active
 */
@Composable
actual fun ConfigureSystemUI(useDarkTheme: Boolean) {
    val view = LocalView.current
    
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            
            // Set status bar icon colors
            // Light theme = dark icons (true), Dark theme = light icons (false)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            
            // Set navigation bar icon colors
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }
}
