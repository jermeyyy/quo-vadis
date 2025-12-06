package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * JavaScript/Browser implementation calculating from window dimensions.
 *
 * Observes browser resize events to update size class.
 * Uses window.innerWidth/innerHeight for viewport dimensions.
 */
@Composable
public actual fun calculateWindowSizeClass(): WindowSizeClass {
    var windowSizeClass by remember { mutableStateOf(calculateFromBrowser()) }

    DisposableEffect(Unit) {
        val resizeListener: (Event) -> Unit = {
            windowSizeClass = calculateFromBrowser()
        }

        window.addEventListener("resize", resizeListener)

        onDispose {
            window.removeEventListener("resize", resizeListener)
        }
    }

    return windowSizeClass
}

private fun calculateFromBrowser(): WindowSizeClass {
    // Browser dimensions are in CSS pixels (already density-independent)
    val widthDp = window.innerWidth.dp
    val heightDp = window.innerHeight.dp

    return WindowSizeClass.calculateFromSize(widthDp, heightDp)
}
