package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

/**
 * WasmJS implementation of platform default for predictive back support.
 *
 * Returns `false` because web browsers handle their own back button navigation.
 */
@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
