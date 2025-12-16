package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

/**
 * Android implementation of platform default for predictive back support.
 *
 * Returns `true` because Android has native system back gesture integration
 * (particularly on Android 13+ with predictive back gesture support).
 */
@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = true
