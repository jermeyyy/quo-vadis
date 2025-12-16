package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

/**
 * iOS implementation of platform default for predictive back support.
 *
 * Returns `false` because iOS uses native swipe-back gestures provided by the system.
 */
@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
