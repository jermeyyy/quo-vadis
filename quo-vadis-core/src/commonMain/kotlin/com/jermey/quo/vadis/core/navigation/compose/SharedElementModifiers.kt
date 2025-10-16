package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.SharedElementConfig
import com.jermey.quo.vadis.core.navigation.core.SharedElementType

/**
 * Apply a shared element modifier using the current [SharedTransitionScope] and [AnimatedVisibilityScope].
 *
 * This is a convenience extension that automatically retrieves the scopes from the current composition
 * and applies the appropriate shared element modifier based on the configuration type.
 *
 * Usage:
 * ```kotlin
 * Image(
 *     painter = painterResource(imageRes),
 *     contentDescription = null,
 *     modifier = Modifier
 *         .size(100.dp)
 *         .quoVadisSharedElement(sharedElement(key = "image_${item.id}"))
 * )
 * ```
 *
 * @param config SharedElementConfig specifying the key, type, and optional bounds transform
 * @return Modified Modifier with shared element transition, or original if scopes unavailable
 *
 * @see SharedElementConfig
 * @see com.jermey.quo.vadis.core.navigation.core.sharedElement
 * @see com.jermey.quo.vadis.core.navigation.core.sharedBounds
 */
@ExperimentalSharedTransitionApi
@androidx.compose.runtime.Composable
fun Modifier.quoVadisSharedElement(
    config: SharedElementConfig
): Modifier {
    val sharedTransitionScope = currentSharedTransitionScope() ?: return this
    val animatedVisibilityScope = currentNavAnimatedVisibilityScope() ?: return this

    return with(sharedTransitionScope) {
        when (config.type) {
            SharedElementType.Element -> {
                sharedElement(
                    sharedContentState = rememberSharedContentState(key = config.key),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = config.boundsTransform ?: BoundsTransform { _, _ ->
                        androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        )
                    }
                )
            }
            SharedElementType.Bounds -> {
                sharedBounds(
                    sharedContentState = rememberSharedContentState(key = config.key),
                    animatedVisibilityScope = animatedVisibilityScope,
                    boundsTransform = config.boundsTransform ?: BoundsTransform { _, _ ->
                        androidx.compose.animation.core.spring(
                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
                        )
                    }
                )
            }
        }
    }
}

/**
 * Apply a shared bounds modifier using the current scopes.
 *
 * Convenience wrapper around [quoVadisSharedElement] that creates a SharedElementConfig
 * with type [SharedElementType.Bounds].
 *
 * Usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .quoVadisSharedBounds(key = "container_${item.id}")
 * ) {
 *     // Content that changes between screens
 * }
 * ```
 *
 * @param key Unique identifier for matching bounds between screens
 * @param boundsTransform Optional custom bounds animation spec
 * @return Modified Modifier with shared bounds transition
 */
@ExperimentalSharedTransitionApi
@androidx.compose.runtime.Composable
fun Modifier.quoVadisSharedBounds(
    key: Any,
    boundsTransform: BoundsTransform? = null
): Modifier = quoVadisSharedElement(
    SharedElementConfig(
        key = key,
        type = SharedElementType.Bounds,
        boundsTransform = boundsTransform
    )
)

/**
 * Apply a shared element modifier using the current scopes, or no-op if scopes unavailable.
 *
 * This is a safe variant that gracefully degrades when shared transitions are disabled
 * or when called outside a navigation context.
 *
 * Usage:
 * ```kotlin
 * // Will apply shared element if available, otherwise just use the size
 * Image(
 *     painter = painterResource(imageRes),
 *     contentDescription = null,
 *     modifier = Modifier
 *         .size(100.dp)
 *         .quoVadisSharedElementOrNoop(key = "image_${item.id}")
 * )
 * ```
 *
 * @param key Unique identifier for matching elements between screens
 * @param type Type of shared element transition (Element or Bounds)
 * @param boundsTransform Optional custom bounds animation spec
 * @return Modified Modifier with shared element transition, or original if scopes unavailable
 */
@ExperimentalSharedTransitionApi
@androidx.compose.runtime.Composable
fun Modifier.quoVadisSharedElementOrNoop(
    key: Any,
    type: SharedElementType = SharedElementType.Element,
    boundsTransform: BoundsTransform? = null
): Modifier {
    val sharedTransitionScope = currentSharedTransitionScope()
    val animatedVisibilityScope = currentNavAnimatedVisibilityScope()

    // Return original modifier if scopes unavailable (graceful degradation)
    if (sharedTransitionScope == null || animatedVisibilityScope == null) {
        return this
    }

    return quoVadisSharedElement(
        SharedElementConfig(
            key = key,
            type = type,
            boundsTransform = boundsTransform
        )
    )
}
