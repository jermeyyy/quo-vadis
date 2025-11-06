package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.TabDefinition

/**
 * Container that manages multiple tab content screens with visibility control.
 *
 * This composable:
 * - Keeps all tabs in composition for state preservation
 * - Animates tab transitions with customizable animations
 * - Uses [AnimatedVisibility] for smooth enter/exit transitions
 * - Maintains independent navigation stacks per tab
 *
 * **Important**: All tab content remains in the composition tree but is hidden
 * when not selected. This preserves scroll positions, form inputs, and navigation
 * state across tab switches.
 *
 * @param selectedTab The currently selected tab definition.
 * @param allTabs List of all tabs to render (maintains state for all).
 * @param modifier Modifier for the container Box.
 * @param transitionSpec Animation specification for tab transitions.
 * @param content Composable lambda providing content for each tab.
 *
 * @sample
 * ```kotlin
 * TabNavigationContainer(
 *     selectedTab = state.selectedTab.value,
 *     allTabs = listOf(HomeTab, ProfileTab),
 *     transitionSpec = TabTransitionSpec.SlideHorizontal
 * ) { tab ->
 *     when (tab) {
 *         HomeTab -> HomeScreen()
 *         ProfileTab -> ProfileScreen()
 *     }
 * }
 * ```
 */
@Composable
fun TabNavigationContainer(
    selectedTab: TabDefinition,
    allTabs: List<TabDefinition>,
    modifier: Modifier = Modifier,
    transitionSpec: TabTransitionSpec = TabTransitionSpec.Default,
    content: @Composable (TabDefinition) -> Unit
) {
    Box(modifier = modifier) {
        allTabs.forEach { tab ->
            key(tab.route) {
                TabContent(
                    visible = tab == selectedTab,
                    transitionSpec = transitionSpec,
                    content = { content(tab) }
                )
            }
        }
    }
}

/**
 * Individual tab content with visibility animation.
 *
 * Wraps tab content in [AnimatedVisibility] to provide smooth transitions
 * when tabs are switched.
 */
@Composable
private fun TabContent(
    visible: Boolean,
    transitionSpec: TabTransitionSpec,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = transitionSpec.enter,
        exit = transitionSpec.exit
    ) {
        content()
    }
}

/**
 * Specification for tab transition animations.
 *
 * Defines enter and exit transitions for tab content when switching between tabs.
 */
data class TabTransitionSpec(
    val enter: EnterTransition,
    val exit: ExitTransition
) {
    companion object {
        /**
         * Default fade transition (Material Design 3 recommendation).
         *
         * Simple fade in/out with 300ms duration.
         */
        val Default = TabTransitionSpec(
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        )

        /**
         * Horizontal slide transition with fade.
         *
         * Slides content horizontally (like ViewPager) with accompanying fade.
         * Suitable for tab bars at the top.
         */
        val SlideHorizontal = TabTransitionSpec(
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth / 4 },
                animationSpec = tween(400)
            ) + fadeIn(animationSpec = tween(400)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(400)
            ) + fadeOut(animationSpec = tween(400))
        )

        /**
         * Cross-fade transition (no movement).
         *
         * Simple cross-fade between tabs, no sliding.
         * Fastest and most subtle option.
         */
        val Crossfade = TabTransitionSpec(
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
        )

        /**
         * No animation (instant switch).
         *
         * For performance-critical scenarios or user preference.
         */
        val None = TabTransitionSpec(
            enter = EnterTransition.None,
            exit = ExitTransition.None
        )
    }
}
