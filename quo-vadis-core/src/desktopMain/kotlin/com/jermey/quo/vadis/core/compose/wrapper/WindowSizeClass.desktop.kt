package com.jermey.quo.vadis.core.compose.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import com.jermey.quo.vadis.core.navigation.compose.wrapper.WindowSizeClass

/**
 * Desktop implementation calculating from window size.
 *
 * Uses LocalWindowInfo to get current window dimensions and
 * LocalDensity to convert pixels to dp.
 */
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current

    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val heightDp = with(density) { windowInfo.containerSize.height.toDp() }

    return WindowSizeClass.Companion.calculateFromSize(widthDp, heightDp)
}
