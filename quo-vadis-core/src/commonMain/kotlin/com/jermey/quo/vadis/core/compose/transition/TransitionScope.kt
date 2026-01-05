package com.jermey.quo.vadis.core.compose.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * Unified scope interface combining [SharedTransitionScope] and [AnimatedVisibilityScope].
 *
 * This interface simplifies shared element transitions by providing both required scopes
 * in a single parameter, reducing boilerplate when implementing screens with shared elements.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyScreen(
 *     navigator: Navigator,
 *     transitionScope: TransitionScope? = null
 * ) {
 *     transitionScope?.let {
 *         Image(
 *             modifier = Modifier.sharedElement(it, "imageKey")
 *         )
 *     }
 * }
 * ```
 *
 * @property sharedTransitionScope The SharedTransitionScope for coordinating shared element animations
 * @property animatedVisibilityScope The AnimatedVisibilityScope for tracking visibility during transitions
 */
@OptIn(ExperimentalSharedTransitionApi::class)
interface TransitionScope {
    val sharedTransitionScope: SharedTransitionScope
    val animatedVisibilityScope: AnimatedVisibilityScope
}

/**
 * Default implementation of [TransitionScope].
 *
 * @param sharedTransitionScope The SharedTransitionScope instance
 * @param animatedVisibilityScope The AnimatedVisibilityScope instance
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private data class TransitionScopeImpl(
    override val sharedTransitionScope: SharedTransitionScope,
    override val animatedVisibilityScope: AnimatedVisibilityScope
) : TransitionScope

/**
 * Creates a [TransitionScope] from the given scopes.
 *
 * @param sharedTransitionScope The SharedTransitionScope instance
 * @param animatedVisibilityScope The AnimatedVisibilityScope instance
 * @return A TransitionScope combining both scopes
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun TransitionScope(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
): TransitionScope = TransitionScopeImpl(sharedTransitionScope, animatedVisibilityScope)

/**
 * CompositionLocal providing access to the unified [TransitionScope].
 * This scope combines SharedTransitionScope and AnimatedVisibilityScope for easier usage.
 *
 * Returns null if shared elements are not enabled or if not within a transition context.
 */
val LocalTransitionScope = compositionLocalOf<TransitionScope?> { null }
