package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Extracted metadata from a @Screen annotation.
 *
 * @property functionDeclaration The KSP function declaration for this screen
 * @property functionName Simple function name (e.g., "DetailScreen")
 * @property destinationClass Class declaration for the destination this screen renders
 * @property hasDestinationParam True if the function accepts a destination parameter
 * @property hasSharedTransitionScope True if the function accepts SharedTransitionScope
 * @property hasAnimatedVisibilityScope True if the function accepts AnimatedVisibilityScope
 * @property packageName Package containing this function
 */
data class ScreenInfo(
    val functionDeclaration: KSFunctionDeclaration,
    val functionName: String,
    val destinationClass: KSClassDeclaration,
    val hasDestinationParam: Boolean,
    val hasSharedTransitionScope: Boolean,
    val hasAnimatedVisibilityScope: Boolean,
    val packageName: String
)
