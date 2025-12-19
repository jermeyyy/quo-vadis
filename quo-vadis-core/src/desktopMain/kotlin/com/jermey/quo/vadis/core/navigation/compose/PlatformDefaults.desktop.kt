package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

/**
 * Desktop implementation of platform default for predictive back support.
 *
 * Returns `false` because desktop platforms typically don't have native back gestures.
 */
@Composable
internal actual fun platformDefaultPredictiveBack(): Boolean = false
