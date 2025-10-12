package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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

        override val exit = slideOutHorizontally(
            targetOffsetX = { -it / 3 },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

        override val popEnter = slideInHorizontally(
            initialOffsetX = { -it / 3 },
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

        override val exit = slideOutVertically(
            targetOffsetY = { -it / 3 },
            animationSpec = tween(ANIMATION_DURATION)
        ) + fadeOut(animationSpec = tween(ANIMATION_DURATION))

        override val popEnter = slideInVertically(
            initialOffsetY = { -it / 3 },
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
fun customTransition(block: TransitionBuilder.() -> Unit): NavigationTransition {
    return TransitionBuilder().apply(block).build()
}

/**
 * Shared element transition configuration.
 * Provides support for shared element animations between screens.
 */
data class SharedElementConfig(
    val key: String,
    val animationSpec: FiniteAnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)

/**
 * Marker interface for destinations that support shared element transitions.
 */
interface SharedElementDestination : Destination {
    val sharedElements: List<SharedElementConfig>
}

/**
 * Extension function to mark a composable as a shared element.
 */
@Composable
fun Modifier.sharedElement(
    key: String,
    config: SharedElementConfig = SharedElementConfig(key)
): Modifier {
    // This will be implemented with actual shared element transition logic
    // For now, return the modifier as-is
    return this
}
