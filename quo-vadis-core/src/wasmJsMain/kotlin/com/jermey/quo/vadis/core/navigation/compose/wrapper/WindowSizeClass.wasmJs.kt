package com.jermey.quo.vadis.core.navigation.compose.wrapper

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
 * WebAssembly/Browser implementation calculating from window dimensions.
 *
 * Same logic as JS implementation but for Wasm target.
 */
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
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
    val widthDp = window.innerWidth.dp
    val heightDp = window.innerHeight.dp

    return WindowSizeClass.calculateFromSize(widthDp, heightDp)
}
