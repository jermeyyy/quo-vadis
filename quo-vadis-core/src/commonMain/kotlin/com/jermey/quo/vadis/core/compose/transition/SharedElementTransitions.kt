package com.jermey.quo.vadis.core.compose.transition

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi

/**
 * Shared element transition configuration.
 * Provides support for shared element animations between screens using Compose's SharedTransitionLayout.
 *
 * ## Usage
 *
 * ```kotlin
 * // For exact visual match (same image, same text)
 * val config = sharedElement(key = "image-${item.id}")
 *
 * // For different content in same bounds (list item -> detail)
 * val config = sharedBounds(key = "container-${item.id}")
 * ```
 *
 * @param key Unique identifier for matching shared elements between screens (can be Any type for flexibility)
 * @param type Type of shared element transition (Element vs Bounds)
 * @param boundsTransform Custom animation spec for bounds transformation (optional)
 *
 * @see sharedElement
 * @see sharedBounds
 * @see SharedElementType
 */
@ExperimentalSharedTransitionApi
data class SharedElementConfig(
    val key: Any,
    val type: SharedElementType = SharedElementType.Element,
    val boundsTransform: BoundsTransform? = null
)

/**
 * Type of shared element transition.
 */
enum class SharedElementType {
    /**
     * Use sharedElement() - for exact visual match between screens.
     * The content should look the same (e.g., same image, same text).
     */
    Element,

    /**
     * Use sharedBounds() - for different content occupying same space.
     * The bounds transition while content can change (e.g., list item expanding to detail).
     */
    Bounds
}

/**
 * Create a SharedElementConfig for sharedElement() modifier.
 *
 * Use this when the content looks exactly the same between screens,
 * such as an image or text that should animate its position.
 *
 * ## Example
 *
 * ```kotlin
 * // Source screen
 * Image(
 *     painter = painterResource(item.imageRes),
 *     modifier = Modifier.quoVadisSharedElement(sharedElement("image-${item.id}"))
 * )
 *
 * // Target screen
 * Image(
 *     painter = painterResource(item.imageRes),
 *     modifier = Modifier.quoVadisSharedElement(sharedElement("image-${item.id}"))
 * )
 * ```
 *
 * @param key Unique identifier for matching elements between screens
 * @param boundsTransform Optional custom bounds transform animation
 */
@ExperimentalSharedTransitionApi
fun sharedElement(
    key: Any,
    boundsTransform: BoundsTransform? = null
): SharedElementConfig = SharedElementConfig(
    key = key,
    type = SharedElementType.Element,
    boundsTransform = boundsTransform
)

/**
 * Create a SharedElementConfig for sharedBounds() modifier.
 *
 * Use this when the bounds should animate but the content changes,
 * such as a list item expanding into a full detail view.
 *
 * ## Example
 *
 * ```kotlin
 * // Source screen (list item)
 * Card(
 *     modifier = Modifier.quoVadisSharedBounds(sharedBounds("card-${item.id}"))
 * ) {
 *     CompactItemContent(item)
 * }
 *
 * // Target screen (detail view)
 * Card(
 *     modifier = Modifier.quoVadisSharedBounds(sharedBounds("card-${item.id}"))
 * ) {
 *     ExpandedDetailContent(item)
 * }
 * ```
 *
 * @param key Unique identifier for matching bounds between screens
 * @param boundsTransform Optional custom bounds transform animation
 */
@ExperimentalSharedTransitionApi
fun sharedBounds(
    key: Any,
    boundsTransform: BoundsTransform? = null
): SharedElementConfig = SharedElementConfig(
    key = key,
    type = SharedElementType.Bounds,
    boundsTransform = boundsTransform
)
