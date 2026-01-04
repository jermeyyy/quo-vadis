package com.jermey.quo.vadis.core.navigation.transition

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
 *
 * ## Usage
 *
 * Use one of the predefined transitions from [NavigationTransitions]:
 * ```kotlin
 * transition<DetailScreen>(NavigationTransitions.SlideHorizontal)
 * ```
 *
 * Or create a custom transition using [customTransition][com.jermey.quo.vadis.core.compose.transition.customTransition]:
 * ```kotlin
 * val myTransition = customTransition {
 *     enter = fadeIn() + slideInHorizontally()
 *     exit = fadeOut() + slideOutHorizontally()
 * }
 * ```
 *
 * @see NavigationTransitions
 * @see com.jermey.quo.vadis.core.compose.transition.customTransition
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
 *
 * Provides common transition patterns for navigation:
 * - [None] - No animation
 * - [Fade] - Fade in/out
 * - [SlideHorizontal] - Slide from right with parallax effect
 * - [SlideVertical] - Slide from bottom with parallax effect
 * - [ScaleIn] - Scale and fade
 *
 * ## Usage
 *
 * ```kotlin
 * val config = navigationConfig {
 *     transition<DetailScreen>(NavigationTransitions.SlideHorizontal)
 *     transition<ModalScreen>(NavigationTransitions.SlideVertical)
 * }
 * ```
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
