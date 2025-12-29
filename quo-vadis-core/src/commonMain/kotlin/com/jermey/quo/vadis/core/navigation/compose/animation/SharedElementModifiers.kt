package com.jermey.quo.vadis.core.navigation.compose.animation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable

/**
 * Provides access to [SharedTransitionScope] for shared element transitions.
 *
 * Use this to access the shared transition scope from any composable within
 * a NavigationHost. The scope is required to use Compose's native shared
 * element modifiers.
 *
 * ## Usage Pattern (Recommended)
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     // Get the transition scope from navigation context
 *     val transitionScope = LocalTransitionScope.current
 *
 *     // Use Compose's native shared element API
 *     transitionScope?.let { scope ->
 *         with(scope.sharedTransitionScope) {
 *             Image(
 *                 painter = painterResource(imageRes),
 *                 contentDescription = null,
 *                 modifier = Modifier
 *                     .size(100.dp)
 *                     .sharedElement(
 *                         state = rememberSharedContentState(key = "image_key"),
 *                         animatedVisibilityScope = scope.animatedVisibilityScope
 *                     )
 *             )
 *         }
 *     }
 * }
 * ```
 *
 * ## For Shared Bounds (Container Transforms)
 *
 * ```kotlin
 * transitionScope?.let { scope ->
 *     with(scope.sharedTransitionScope) {
 *         Card(
 *             modifier = Modifier
 *                 .sharedBounds(
 *                     sharedContentState = rememberSharedContentState(key = "card_${item.id}"),
 *                     animatedVisibilityScope = scope.animatedVisibilityScope
 *                 )
 *         ) {
 *             // Card content
 *         }
 *     }
 * }
 * ```
 *
 * ## Key Matching
 *
 * Shared elements match between screens when they have the same key.
 * Use unique, stable keys like `"icon_${item.id}"` or data class keys.
 *
 * @return The current [TransitionScope] or null if not in a navigation context
 * @see TransitionScope
 * @see LocalTransitionScope
 * @see SharedTransitionScope.sharedElement
 * @see SharedTransitionScope.sharedBounds
 */
@ExperimentalSharedTransitionApi
@Composable
fun currentSharedTransitionScope(): SharedTransitionScope? {
    return currentTransitionScope()?.sharedTransitionScope
}
