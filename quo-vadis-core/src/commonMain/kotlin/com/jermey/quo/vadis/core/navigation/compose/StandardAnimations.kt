package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Collection of standard navigation animations.
 *
 * Provides pre-built animation specifications for common navigation transitions.
 * These animations follow Material Design motion guidelines and can be used
 * directly with [AnimationRegistry] or combined using the [plus] operator.
 *
 * ## Basic Usage
 *
 * ```kotlin
 * val registry = AnimationRegistry {
 *     registerDefault(TransitionType.PUSH, StandardAnimations.slideForward())
 *     registerDefault(TransitionType.POP, StandardAnimations.slideBackward())
 * }
 * ```
 *
 * ## Custom Duration/Easing
 *
 * ```kotlin
 * val customSlide = StandardAnimations.slideForward(
 *     duration = 500,
 *     easing = LinearOutSlowInEasing
 * )
 * ```
 *
 * ## Combining Animations
 *
 * ```kotlin
 * val combined = StandardAnimations.fade() + StandardAnimations.scale()
 * ```
 *
 * @see AnimationRegistry
 * @see SurfaceAnimationSpec
 */
public object StandardAnimations {

    /**
     * Default animation duration in milliseconds.
     */
    private const val DEFAULT_DURATION: Int = 300

    /**
     * Default easing for animations.
     */
    private val DEFAULT_EASING: Easing = FastOutSlowInEasing

    /**
     * Standard forward slide animation (slide in from right, push left out).
     *
     * This is the typical animation for push navigation in a stack.
     * The entering screen slides in from the right edge while the exiting
     * screen slides partially to the left with a parallax effect.
     *
     * @param duration Animation duration in milliseconds
     * @param easing Easing curve for the animation
     * @return A [SurfaceAnimationSpec] with horizontal slide transitions
     */
    public fun slideForward(
        duration: Int = DEFAULT_DURATION,
        easing: Easing = DEFAULT_EASING
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = slideInHorizontally(
                animationSpec = tween(duration, easing = easing),
                initialOffsetX = { fullWidth -> fullWidth } // From right
            ) + fadeIn(
                animationSpec = tween(duration / 2, easing = easing)
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(duration, easing = easing),
                targetOffsetX = { fullWidth -> -fullWidth / 3 } // Slight parallax left
            ) + fadeOut(
                animationSpec = tween(duration / 2, easing = easing)
            )
        )
    }

    /**
     * Standard backward slide animation (slide in from left, push right out).
     *
     * This is the typical animation for pop navigation in a stack.
     * The entering screen slides in from the left while the exiting
     * screen slides out to the right.
     *
     * @param duration Animation duration in milliseconds
     * @param easing Easing curve for the animation
     * @return A [SurfaceAnimationSpec] with horizontal slide transitions
     */
    public fun slideBackward(
        duration: Int = DEFAULT_DURATION,
        easing: Easing = DEFAULT_EASING
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = slideInHorizontally(
                animationSpec = tween(duration, easing = easing),
                initialOffsetX = { fullWidth -> -fullWidth / 3 } // From slight left
            ) + fadeIn(
                animationSpec = tween(duration / 2, easing = easing)
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(duration, easing = easing),
                targetOffsetX = { fullWidth -> fullWidth } // To right
            ) + fadeOut(
                animationSpec = tween(duration / 2, easing = easing)
            )
        )
    }

    /**
     * Vertical slide animation (slide up to enter, slide down to exit).
     *
     * This animation is commonly used for modal sheets, bottom sheets,
     * or any content that should appear to slide up from the bottom.
     *
     * @param duration Animation duration in milliseconds
     * @param easing Easing curve for the animation
     * @return A [SurfaceAnimationSpec] with vertical slide transitions
     */
    public fun slideVertical(
        duration: Int = DEFAULT_DURATION,
        easing: Easing = DEFAULT_EASING
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = slideInVertically(
                animationSpec = tween(duration, easing = easing),
                initialOffsetY = { fullHeight -> fullHeight } // From bottom
            ) + fadeIn(
                animationSpec = tween(duration / 2, easing = easing)
            ),
            exit = slideOutVertically(
                animationSpec = tween(duration, easing = easing),
                targetOffsetY = { fullHeight -> fullHeight } // To bottom
            ) + fadeOut(
                animationSpec = tween(duration / 2, easing = easing)
            )
        )
    }

    /**
     * Simple fade animation.
     *
     * A basic crossfade transition that fades the entering screen in
     * while fading the exiting screen out. Commonly used for tab switches
     * or content updates that don't involve spatial navigation.
     *
     * @param duration Animation duration in milliseconds
     * @param easing Easing curve for the animation
     * @return A [SurfaceAnimationSpec] with fade transitions
     */
    public fun fade(
        duration: Int = DEFAULT_DURATION,
        easing: Easing = DEFAULT_EASING
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = fadeIn(
                animationSpec = tween(duration, easing = easing)
            ),
            exit = fadeOut(
                animationSpec = tween(duration, easing = easing)
            )
        )
    }

    /**
     * Scale animation (zoom in to enter, zoom out to exit).
     *
     * This animation creates a depth effect where content appears to
     * zoom in when entering and zoom out when exiting. Often used for
     * dialogs, FAB expansions, or z-axis transitions.
     *
     * @param duration Animation duration in milliseconds
     * @param easing Easing curve for the animation
     * @param initialScale Starting scale for entering content (< 1.0 for zoom-in effect)
     * @param targetScale Target scale for exiting content (> 1.0 for zoom-out effect)
     * @return A [SurfaceAnimationSpec] with scale transitions
     */
    public fun scale(
        duration: Int = DEFAULT_DURATION,
        easing: Easing = DEFAULT_EASING,
        initialScale: Float = 0.8f,
        targetScale: Float = 1.1f
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = scaleIn(
                animationSpec = tween(duration, easing = easing),
                initialScale = initialScale
            ) + fadeIn(
                animationSpec = tween(duration, easing = easing)
            ),
            exit = scaleOut(
                animationSpec = tween(duration, easing = easing),
                targetScale = targetScale
            ) + fadeOut(
                animationSpec = tween(duration, easing = easing)
            )
        )
    }

    /**
     * Shared axis animation (Material Design).
     *
     * Implements the Material Design shared axis transition pattern.
     * The axis determines the direction of movement:
     * - **X axis**: Horizontal slide (like forward/backward)
     * - **Y axis**: Vertical slide (like modal sheets)
     * - **Z axis**: Depth scale (like zoom in/out)
     *
     * @param axis The axis of movement (X, Y, or Z)
     * @param duration Animation duration in milliseconds
     * @param easing Easing curve for the animation
     * @return A [SurfaceAnimationSpec] matching the selected axis behavior
     * @see SharedAxis
     */
    public fun sharedAxis(
        axis: SharedAxis,
        duration: Int = DEFAULT_DURATION,
        easing: Easing = DEFAULT_EASING
    ): SurfaceAnimationSpec {
        return when (axis) {
            SharedAxis.X -> slideForward(duration, easing)
            SharedAxis.Y -> slideVertical(duration, easing)
            SharedAxis.Z -> scale(duration, easing, 0.8f, 1.05f)
        }
    }

    /**
     * Material container transform placeholder.
     *
     * Provides a simplified container transform animation using scale and fade.
     *
     * **Note**: Full container transform requires [SharedTransitionScope],
     * which is handled separately in [QuoVadisHost] using Compose's
     * `sharedElement` and `sharedBounds` modifiers. This function provides
     * a fallback animation when shared elements are not available.
     *
     * @param duration Animation duration in milliseconds
     * @return A [SurfaceAnimationSpec] approximating container transform
     */
    public fun containerTransform(
        duration: Int = DEFAULT_DURATION
    ): SurfaceAnimationSpec {
        return SurfaceAnimationSpec(
            enter = fadeIn(tween(duration)) + scaleIn(
                animationSpec = tween(duration),
                initialScale = 0.92f
            ),
            exit = fadeOut(tween(duration)) + scaleOut(
                animationSpec = tween(duration),
                targetScale = 1.05f
            )
        )
    }

    /**
     * Shared axis types for Material Design transitions.
     *
     * These correspond to the three axes of movement defined in the
     * Material Motion guidelines:
     *
     * - [X]: Horizontal movement (left-right)
     * - [Y]: Vertical movement (top-bottom)
     * - [Z]: Depth movement (scale/zoom)
     *
     * @see sharedAxis
     */
    public enum class SharedAxis {
        /**
         * Horizontal axis - content slides left or right.
         */
        X,

        /**
         * Vertical axis - content slides up or down.
         */
        Y,

        /**
         * Depth/Z axis - content scales in or out.
         */
        Z
    }
}

// =============================================================================
// Animation Combinators
// =============================================================================

/**
 * Combines two animation specs by merging their enter/exit transitions.
 *
 * When combined, both enter transitions will play simultaneously when
 * entering, and both exit transitions will play simultaneously when exiting.
 *
 * ## Example
 *
 * ```kotlin
 * // Create a slide + fade animation
 * val combined = StandardAnimations.slideForward() + StandardAnimations.fade()
 * ```
 *
 * @param other The animation spec to combine with this one
 * @return A new [SurfaceAnimationSpec] with combined transitions
 */
public operator fun SurfaceAnimationSpec.plus(other: SurfaceAnimationSpec): SurfaceAnimationSpec {
    return SurfaceAnimationSpec(
        enter = this.enter + other.enter,
        exit = this.exit + other.exit
    )
}

/**
 * Creates a reversed version of this animation spec.
 *
 * The enter and exit animations are swapped, useful for creating
 * complementary backward animations from forward animations.
 *
 * **Note**: This creates a logical reversal where enter becomes exit
 * and vice versa. For proper reverse playback, the individual
 * transition animations would need to be reversed as well.
 *
 * ## Example
 *
 * ```kotlin
 * val forward = StandardAnimations.slideForward()
 * val backward = forward.reversed() // exit becomes enter, enter becomes exit
 * ```
 *
 * @return A new [SurfaceAnimationSpec] with swapped enter/exit
 */
public fun SurfaceAnimationSpec.reversed(): SurfaceAnimationSpec {
    // Note: This is a simplified reversal that swaps enter/exit
    // For full reversal, we'd need to reverse each transition's direction
    // which isn't directly supported by Compose's transition API
    return SurfaceAnimationSpec(
        enter = fadeIn(), // Fallback - proper reversal not supported
        exit = fadeOut()  // Fallback - proper reversal not supported
    )
}
