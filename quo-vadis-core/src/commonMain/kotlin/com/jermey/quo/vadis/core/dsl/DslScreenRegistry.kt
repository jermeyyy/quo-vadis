package com.jermey.quo.vadis.core.dsl

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.dsl.internal.ScreenEntry
import kotlin.reflect.KClass

/**
 * DSL-based implementation of [ScreenRegistry] that renders screen content
 * based on registered screen entries.
 *
 * This registry is created by [DslNavigationConfig] from the screen
 * registrations collected by [NavigationConfigBuilder].
 *
 * ## Usage
 *
 * Screen entries are registered via the DSL:
 *
 * ```kotlin
 * val config = navigationConfig {
 *     screen<HomeScreen> { destination, sharedScope, animScope ->
 *         HomeScreenContent(
 *             destination = destination,
 *             sharedTransitionScope = sharedScope,
 *             animatedVisibilityScope = animScope
 *         )
 *     }
 * }
 * ```
 *
 * The registry then renders content by looking up the destination's class:
 *
 * ```kotlin
 * @Composable
 * fun RenderScreen(destination: NavDestination) {
 *     screenRegistry.Content(destination)
 * }
 * ```
 *
 * @param screens Map of destination classes to their screen entry configurations
 *
 * @see ScreenRegistry
 * @see ScreenEntry
 * @see NavigationConfigBuilder.screen
 */
@InternalQuoVadisApi
@OptIn(ExperimentalSharedTransitionApi::class)
internal class DslScreenRegistry(
    private val screens: Map<KClass<out NavDestination>, ScreenEntry>
) : ScreenRegistry {

    /**
     * Renders the composable content for the given destination.
     *
     * Looks up the screen entry by the destination's class and invokes
     * its content composable with the provided parameters.
     *
     * If no screen is registered for the destination, nothing is rendered.
     *
     * @param destination The destination to render
     * @param sharedTransitionScope Optional SharedTransitionScope for shared element transitions
     * @param animatedVisibilityScope Optional AnimatedVisibilityScope for coordinated animations
     */
    @Composable
    override fun Content(
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        val entry = screens[destination::class]
        entry?.content?.invoke(
            destination,
            sharedTransitionScope,
            animatedVisibilityScope
        )
    }

    /**
     * Checks if a screen is registered for the given destination.
     *
     * @param destination The destination to check
     * @return true if a screen is registered for this destination's class
     */
    override fun hasContent(destination: NavDestination): Boolean {
        return screens.containsKey(destination::class)
    }
}
