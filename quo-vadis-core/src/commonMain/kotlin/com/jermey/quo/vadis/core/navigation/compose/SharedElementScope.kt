package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal providing access to the [SharedTransitionScope].
 * This scope is required for shared element transitions.
 *
 * Returns null if shared elements are not enabled or if not within a SharedTransitionLayout.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * CompositionLocal providing access to the [AnimatedVisibilityScope] for navigation transitions.
 * This scope is required for shared element transitions to coordinate with screen visibility.
 *
 * Returns null if not within an AnimatedContent or AnimatedVisibility.
 */
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Returns the current [SharedTransitionScope] or null if not available.
 *
 * Use this when you want to conditionally apply shared element modifiers.
 * If null, shared elements are not enabled or not available in the current context.
 *
 * @return The current SharedTransitionScope or null
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun currentSharedTransitionScope(): SharedTransitionScope? {
    return LocalSharedTransitionScope.current
}

/**
 * Returns the current [AnimatedVisibilityScope] or null if not available.
 *
 * Use this when you want to conditionally apply shared element modifiers.
 * If null, not within an animated transition.
 *
 * @return The current AnimatedVisibilityScope or null
 */
@Composable
fun currentNavAnimatedVisibilityScope(): AnimatedVisibilityScope? {
    return LocalNavAnimatedVisibilityScope.current
}

/**
 * Returns the current [SharedTransitionScope], throwing an exception if not available.
 *
 * Use this when shared elements are required and their absence is an error.
 *
 * @return The current SharedTransitionScope
 * @throws IllegalStateException if SharedTransitionScope is not available
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun requireSharedTransitionScope(): SharedTransitionScope {
    return currentSharedTransitionScope()
        ?: error("SharedTransitionScope not found. Ensure shared elements are enabled in GraphNavHost.")
}

/**
 * Returns the current [AnimatedVisibilityScope], throwing an exception if not available.
 *
 * Use this when animated visibility scope is required and its absence is an error.
 *
 * @return The current AnimatedVisibilityScope
 * @throws IllegalStateException if AnimatedVisibilityScope is not available
 */
@Composable
fun requireNavAnimatedVisibilityScope(): AnimatedVisibilityScope {
    return currentNavAnimatedVisibilityScope()
        ?: error("AnimatedVisibilityScope not found. Ensure you are within an animated navigation transition.")
}
