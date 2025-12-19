package com.jermey.quo.vadis.core.navigation.compose.wrapper

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass as calculateMaterial3WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation using Material3 WindowSizeClass API.
 *
 * Leverages the official Material3 implementation which correctly
 * handles configuration changes, multi-window mode, and foldables.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    val activity = LocalContext.current as Activity
    val m3SizeClass = calculateMaterial3WindowSizeClass(activity)

    return WindowSizeClass(
        widthSizeClass = when (m3SizeClass.widthSizeClass) {
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact ->
                WindowWidthSizeClass.Compact
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium ->
                WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        },
        heightSizeClass = when (m3SizeClass.heightSizeClass) {
            androidx.compose.material3.windowsizeclass.WindowHeightSizeClass.Compact ->
                WindowHeightSizeClass.Compact
            androidx.compose.material3.windowsizeclass.WindowHeightSizeClass.Medium ->
                WindowHeightSizeClass.Medium
            else -> WindowHeightSizeClass.Expanded
        }
    )
}
