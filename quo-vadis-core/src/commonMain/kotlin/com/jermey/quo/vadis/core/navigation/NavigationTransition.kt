package com.jermey.quo.vadis.core.navigation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
 * Defines navigation transitions between screens.
 * Supports custom animations including shared element transitions.
 */
interface NavigationTransition {
    /**
     * Enter animation when navigating to this destination.
     */
    val enter: EnterTransition

    /**
     * Exit animation when navigating away from this destination.
     */
    val exit: ExitTransition

    /**
     * Enter animation when returning to this destination (pop back).
     */
    val popEnter: EnterTransition

    /**
     * Exit animation when popping this destination.
     */
    val popExit: ExitTransition
}

/**
 * Default transition configurations.
 */
object NavigationTransitions {

    const val ANIMATION_DURATION = 300

    val None = object : NavigationTransition {
        override val enter = EnterTransition.None
        override val exit = ExitTransition.None
        override val popEnter = EnterTransition.None
        override val popExit = ExitTransition.None
    }

    val Fade = object : NavigationTransition {
        override val enter = fadeIn(animationSpec = tween(ANIMATION_DURATION))
        override val exit = fadeOut(animationSpec = tween(ANIMATION_DURATION))
        override val popEnter = fadeIn(animationSpec = tween(ANIMATION_DURATION))
        override val popExit = fadeOut(animationSpec = tween(ANIMATION_DURATION))
    }

    /**
     * Factor for parallax effect on the "background" screen during transitions.
     * The background screen moves at this fraction of the foreground screen's movement.
     * This prevents the white/transparent background from showing during crossfades.
     */
    private const val PARALLAX_FACTOR = 0.3f

    val SlideHorizontal = object : NavigationTransition {
        override val enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

        // Exit with parallax: slide left slightly while fading to prevent white flash
        override val exit = slideOutHorizontally(
            targetOffsetX = { -(it * PARALLAX_FACTOR).toInt() },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

        // Pop enter with parallax: start offset left and slide in while fading
        override val popEnter = slideInHorizontally(
            initialOffsetX = { -(it * PARALLAX_FACTOR).toInt() },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

        override val popExit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
    }

    val SlideVertical = object : NavigationTransition {
        override val enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

        // Exit with parallax: slide up slightly while fading to prevent white flash
        override val exit = slideOutVertically(
            targetOffsetY = { -(it * PARALLAX_FACTOR).toInt() },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

        // Pop enter with parallax: start offset up and slide in while fading
        override val popEnter = slideInVertically(
            initialOffsetY = { -(it * PARALLAX_FACTOR).toInt() },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

        override val popExit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
    }

    val ScaleIn = object : NavigationTransition {
        override val enter = scaleIn(
            initialScale = 0.8f,
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

        override val exit = scaleOut(
            targetScale = 0.95f,
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

        override val popEnter = scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

        override val popExit = scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))
    }
}

/**
 * Custom transition builder for creating custom animations.
 */
class TransitionBuilder {
    var enter: EnterTransition = NavigationTransitions.Fade.enter
    var exit: ExitTransition = NavigationTransitions.Fade.exit
    var popEnter: EnterTransition = NavigationTransitions.Fade.popEnter
    var popExit: ExitTransition = NavigationTransitions.Fade.popExit

    fun build(): NavigationTransition = object : NavigationTransition {
        override val enter = this@TransitionBuilder.enter
        override val exit = this@TransitionBuilder.exit
        override val popEnter = this@TransitionBuilder.popEnter
        override val popExit = this@TransitionBuilder.popExit
    }
}

/**
 * DSL for creating custom transitions.
 */
@Suppress("unused")
fun customTransition(block: TransitionBuilder.() -> Unit): NavigationTransition {
    return TransitionBuilder().apply(block).build()
}

/**
 * Shared element transition configuration.
 * Provides support for shared element animations between screens using Compose's SharedTransitionLayout.
 *
 * @param key Unique identifier for matching shared elements between screens (can be Any type for flexibility)
 * @param type Type of shared element transition (Element vs Bounds)
 * @param boundsTransform Custom animation spec for bounds transformation (optional)
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
