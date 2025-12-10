package com.jermey.navplayground.demo.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.compose.RenderingMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for rendering mode preferences across the app.
 *
 * Stores the current rendering mode for QuoVadisHost and provides methods to change it.
 * This allows users to toggle between [RenderingMode.Flattened] and [RenderingMode.Hierarchical]
 * to compare the two rendering approaches.
 */
class RenderingModeManager {
    private val _renderingMode = MutableStateFlow(RenderingMode.Hierarchical)
    val renderingMode: StateFlow<RenderingMode> = _renderingMode.asStateFlow()

    /**
     * Update the current rendering mode.
     *
     * @param mode The new rendering mode to use
     */
    fun setRenderingMode(mode: RenderingMode) {
        _renderingMode.value = mode
    }

    companion object {
        private var instance: RenderingModeManager? = null

        /**
         * Get the singleton instance of RenderingModeManager.
         */
        fun getInstance(): RenderingModeManager {
            if (instance == null) {
                instance = RenderingModeManager()
            }
            return instance!!
        }
    }
}

/**
 * Remember the rendering mode manager instance across recompositions.
 */
@Composable
fun rememberRenderingModeManager(): RenderingModeManager {
    return remember { RenderingModeManager.getInstance() }
}
