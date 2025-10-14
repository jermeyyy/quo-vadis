package com.jermey.navplayground.demo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for theme preferences across the app.
 * Stores the current theme mode and provides methods to change it.
 */
class ThemeManager {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    /**
     * Update the current theme mode.
     */
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    companion object {
        private var instance: ThemeManager? = null

        /**
         * Get the singleton instance of ThemeManager.
         */
        fun getInstance(): ThemeManager {
            if (instance == null) {
                instance = ThemeManager()
            }
            return instance!!
        }
    }
}

/**
 * Remember the theme manager instance across recompositions.
 */
@Composable
fun rememberThemeManager(): ThemeManager {
    return remember { ThemeManager.getInstance() }
}
