package com.jermey.quo.vadis.core.compose.transition

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions

/**
 * Immutable configuration for navigation transitions.
 *
 * Defines a complete set of enter and exit transitions for both forward navigation
 * and back navigation (pop). This data class enables consistent, direction-aware
 * animations throughout the navigation system.
 *
 * ## Transition Direction
 *
 * Navigation has two directions that use different transition pairs:
 *
 * - **Forward (push)**: New screen enters with [enter], current screen exits with [exit]
 * - **Back (pop)**: Previous screen enters with [popEnter], current screen exits with [popExit]
 *
 * ## Usage
 *
 * ```kotlin
 * val transition = NavTransition.SlideHorizontal
 * 
 * AnimatedContent(
 *     targetState = currentScreen,
 *     transitionSpec = { transition.createTransitionSpec(isBack = navigator.isNavigatingBack) }
 * ) { screen ->
 *     ScreenContent(screen)
 * }
 * ```
 *
 * ## Custom Transitions
 *
 * Create custom transitions by combining Compose animation primitives:
 *
 * ```kotlin
 * val myTransition = NavTransition(
 *     enter = fadeIn() + expandHorizontally(),
 *     exit = fadeOut() + shrinkHorizontally(),
 *     popEnter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
 *     popExit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
 * )
 * ```
 *
 * @property enter Transition for new screen appearing during forward navigation
 * @property exit Transition for current screen disappearing during forward navigation
 * @property popEnter Transition for previous screen appearing during back navigation
 * @property popExit Transition for current screen disappearing during back navigation
 *
 * @see ContentTransform
 * @see EnterTransition
 * @see ExitTransition
 */
data class NavTransition(
    val enter: EnterTransition,
    val exit: ExitTransition,
    val popEnter: EnterTransition,
    val popExit: ExitTransition
) {

    /**
     * Creates a [ContentTransform] for use with `AnimatedContent`.
     *
     * This method selects the appropriate enter/exit pair based on navigation
     * direction and combines them into a [ContentTransform] that can be used
     * directly in the `transitionSpec` parameter of `AnimatedContent`.
     *
     * ## Direction Handling
     *
     * - **Forward (`isBack = false`)**: Uses [enter] with [exit], entering screen on top (zIndex = 1)
     * - **Back (`isBack = true`)**: Uses [popEnter] with [popExit], exiting screen on top (zIndex = 1)
     *
     * ## Z-Index and Clipping
     *
     * The z-index ensures proper layering:
     * - Forward: New screen slides over the old screen
     * - Back: Old screen slides away revealing the new screen underneath
     *
     * Clipping is disabled to prevent visual artifacts during slide transitions.
     *
     * ## Example
     *
     * ```kotlin
     * AnimatedContent(
     *     targetState = currentScreen,
     *     transitionSpec = { 
     *         navTransition.createTransitionSpec(isBack = isBackNavigation)
     *     }
     * ) { screen ->
     *     ScreenContent(screen)
     * }
     * ```
     *
     * @param isBack Whether navigation is going backwards (pop) or forwards (push)
     * @return A [ContentTransform] combining the appropriate enter and exit transitions
     */
    fun createTransitionSpec(isBack: Boolean): ContentTransform {
        return if (isBack) {
            // Back navigation: exiting screen (current) should be on top, sliding away
            // to reveal the entering screen (previous) underneath
            ContentTransform(
                targetContentEnter = popEnter,
                initialContentExit = popExit,
                targetContentZIndex = 0f, // Entering (previous) screen behind
                sizeTransform = SizeTransform(clip = false)
            )
        } else {
            // Forward navigation: entering screen (new) should be on top, sliding over
            // the exiting screen (current) which stays underneath
            ContentTransform(
                targetContentEnter = enter,
                initialContentExit = exit,
                targetContentZIndex = 1f, // Entering (new) screen on top
                sizeTransform = SizeTransform(clip = false)
            )
        }
    }

    /**
     * Creates a reversed version of this transition.
     *
     * Swaps forward and back transitions, useful when you need to invert the
     * direction of an existing transition without creating a new one.
     *
     * ## Transformation
     *
     * - [enter] ↔ [popEnter]
     * - [exit] ↔ [popExit]
     *
     * ## Use Case
     *
     * Useful when the same animation should play in reverse for the opposite
     * navigation direction:
     *
     * ```kotlin
     * val slideIn = NavTransition.SlideHorizontal
     * val slideOut = slideIn.reversed() // Same animation, opposite direction
     * ```
     *
     * @return A new [NavTransition] with swapped enter/exit and popEnter/popExit
     */
    fun reversed(): NavTransition {
        return NavTransition(
            enter = popEnter,
            exit = popExit,
            popEnter = enter,
            popExit = exit
        )
    }

    /**
     * Companion object providing preset transition configurations.
     *
     * These presets cover common navigation animation patterns and are derived
     * from the existing [NavigationTransitions] object for consistency.
     */
    companion object {

        /**
         * Default animation duration in milliseconds.
         *
         * Matches the duration used by [NavigationTransitions] for consistency.
         */
        const val ANIMATION_DURATION: Int = NavigationTransitions.ANIMATION_DURATION

        /**
         * Horizontal slide transition with fade.
         *
         * The new screen slides in from the right with a fade, while the
         * current screen fades out. Back navigation reverses this, with
         * the screen sliding out to the right.
         *
         * This is the default transition for stack-based navigation and
         * provides familiar platform-like behavior.
         *
         * ## Animation Details
         *
         * - **Forward enter**: Slide from right + fade in
         * - **Forward exit**: Fade out
         * - **Back enter**: Fade in
         * - **Back exit**: Slide to right + fade out
         */
        val SlideHorizontal: NavTransition = NavTransition(
            enter = NavigationTransitions.SlideHorizontal.enter,
            exit = NavigationTransitions.SlideHorizontal.exit,
            popEnter = NavigationTransitions.SlideHorizontal.popEnter,
            popExit = NavigationTransitions.SlideHorizontal.popExit
        )

        /**
         * Simple fade transition.
         *
         * Both screens fade in and out with no spatial movement. Useful for
         * modal-style transitions or when slide animations are inappropriate.
         *
         * ## Animation Details
         *
         * - All transitions use fade in/out with [ANIMATION_DURATION]
         * - No spatial movement (translation, scale, etc.)
         */
        val Fade: NavTransition = NavTransition(
            enter = NavigationTransitions.Fade.enter,
            exit = NavigationTransitions.Fade.exit,
            popEnter = NavigationTransitions.Fade.popEnter,
            popExit = NavigationTransitions.Fade.popExit
        )

        /**
         * No transition - instant switch.
         *
         * Screens change immediately with no animation. Useful for:
         * - Performance-sensitive scenarios
         * - Cases where animation would be jarring
         * - Testing and debugging
         *
         * ## Animation Details
         *
         * - All transitions are [EnterTransition.None] / [ExitTransition.None]
         * - Zero duration, instant state change
         */
        val None: NavTransition = NavTransition(
            enter = NavigationTransitions.None.enter,
            exit = NavigationTransitions.None.exit,
            popEnter = NavigationTransitions.None.popEnter,
            popExit = NavigationTransitions.None.popExit
        )

        /**
         * Vertical slide transition with fade.
         *
         * The new screen slides in from the bottom with a fade, while the
         * current screen fades out. Back navigation reverses this.
         *
         * Useful for:
         * - Modal presentations
         * - Bottom sheet-style navigation
         * - iOS-style vertical pushes
         *
         * ## Animation Details
         *
         * - **Forward enter**: Slide from bottom + fade in
         * - **Forward exit**: Fade out
         * - **Back enter**: Fade in
         * - **Back exit**: Slide to bottom + fade out
         */
        val SlideVertical: NavTransition = NavTransition(
            enter = NavigationTransitions.SlideVertical.enter,
            exit = NavigationTransitions.SlideVertical.exit,
            popEnter = NavigationTransitions.SlideVertical.popEnter,
            popExit = NavigationTransitions.SlideVertical.popExit
        )

        /**
         * Scale transition with fade.
         *
         * The new screen scales up from smaller size with a fade, while the
         * current screen scales down slightly with a fade. Creates a zoom-like
         * effect.
         *
         * Useful for:
         * - Detail view transitions
         * - Emphasis on content change
         * - Material Design-style expansions
         *
         * ## Animation Details
         *
         * - **Forward enter**: Scale from 80% + fade in
         * - **Forward exit**: Scale to 95% + fade out
         * - **Back enter**: Scale from 95% + fade in
         * - **Back exit**: Scale to 80% + fade out
         */
        val ScaleIn: NavTransition = NavTransition(
            enter = NavigationTransitions.ScaleIn.enter,
            exit = NavigationTransitions.ScaleIn.exit,
            popEnter = NavigationTransitions.ScaleIn.popEnter,
            popExit = NavigationTransitions.ScaleIn.popExit
        )
    }
}
