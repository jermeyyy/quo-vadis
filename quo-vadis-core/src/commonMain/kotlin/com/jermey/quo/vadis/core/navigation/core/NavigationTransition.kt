package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

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

    val SlideHorizontal = object : NavigationTransition {
        override val enter = slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeIn(animationSpec = tween(ANIMATION_DURATION))

        override val exit = fadeOut(animationSpec = tween(ANIMATION_DURATION))

        override val popEnter = fadeIn(animationSpec = tween(ANIMATION_DURATION))

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

        override val exit = fadeOut(animationSpec = tween(ANIMATION_DURATION))

        override val popEnter = fadeIn(animationSpec = tween(ANIMATION_DURATION))

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
    val boundsTransform: androidx.compose.animation.BoundsTransform? = null
)

/**
 * Shared element transition key.
 * Identifies elements that should animate between screens.
 * 
 * @deprecated Use SharedElementConfig with type parameter instead
 */
@Deprecated("Use SharedElementConfig with type parameter instead", ReplaceWith("SharedElementConfig(key, type)"))
data class SharedElementKey(
    val key: String,
    val type: SharedElementType = SharedElementType.Bounds
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
    boundsTransform: androidx.compose.animation.BoundsTransform? = null
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
    boundsTransform: androidx.compose.animation.BoundsTransform? = null
): SharedElementConfig = SharedElementConfig(
    key = key,
    type = SharedElementType.Bounds,
    boundsTransform = boundsTransform
)
