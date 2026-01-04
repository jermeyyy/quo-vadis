package com.jermey.quo.vadis.core.compose.transition

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions

/**
 * Custom transition builder for creating custom animations.
 *
 * ## Usage
 *
 * ```kotlin
 * val myTransition = customTransition {
 *     enter = fadeIn() + slideInHorizontally()
 *     exit = fadeOut() + slideOutHorizontally()
 *     popEnter = fadeIn() + slideInHorizontally { -it }
 *     popExit = fadeOut() + slideOutHorizontally { it }
 * }
 * ```
 *
 * @see customTransition
 * @see NavigationTransition
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
 *
 * ## Usage
 *
 * ```kotlin
 * val transition = customTransition {
 *     enter = slideInVertically() + fadeIn()
 *     exit = slideOutVertically() + fadeOut()
 * }
 * ```
 *
 * @param block Configuration block for the transition builder
 * @return A new [NavigationTransition] with the specified animations
 */
@Suppress("unused")
fun customTransition(block: TransitionBuilder.() -> Unit): NavigationTransition {
    return TransitionBuilder().apply(block).build()
}
