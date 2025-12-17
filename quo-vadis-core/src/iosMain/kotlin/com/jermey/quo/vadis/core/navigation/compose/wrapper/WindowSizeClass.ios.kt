package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIDeviceOrientationDidChangeNotification
import platform.UIKit.UIScreen
import platform.darwin.NSObject

/**
 * iOS implementation calculating from UIScreen bounds.
 *
 * Observes device orientation changes to update size class.
 * Uses UIScreen.main.bounds scaled by density for dp conversion.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    var windowSizeClass by remember { mutableStateOf(calculateCurrentSizeClass()) }

    DisposableEffect(Unit) {
        val observer = OrientationObserver { windowSizeClass = calculateCurrentSizeClass() }

        NSNotificationCenter.defaultCenter.addObserver(
            observer = observer,
            selector = NSSelectorFromString("orientationDidChange"),
            name = UIDeviceOrientationDidChangeNotification,
            `object` = null
        )

        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }

    return windowSizeClass
}

@OptIn(ExperimentalForeignApi::class)
private fun calculateCurrentSizeClass(): WindowSizeClass {
    val screen = UIScreen.mainScreen
    val bounds = screen.bounds

    // Convert points to dp (iOS points are already density-independent)
    val widthDp = bounds.useContents { size.width }.dp
    val heightDp = bounds.useContents { size.height }.dp

    return WindowSizeClass.calculateFromSize(widthDp, heightDp)
}

/**
 * Helper class to observe orientation changes.
 */
@OptIn(BetaInteropApi::class)
private class OrientationObserver(
    private val onOrientationChange: () -> Unit
) : NSObject() {
    @ObjCAction
    fun orientationDidChange() {
        onOrientationChange()
    }
}
