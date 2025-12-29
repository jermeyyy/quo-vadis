package com.jermey.quo.vadis.core.compose

import androidx.compose.runtime.Composable

/**
 * JS implementation of platform default for predictive back support.
 *
 * Returns `false` because web browsers handle their own back button navigation.
 */
@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
