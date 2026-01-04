package com.jermey.quo.vadis.core.dsl

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.animation.NavTransition
import com.jermey.quo.vadis.core.dsl.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.NavDestination
import kotlin.reflect.KClass

/**
 * DSL-based implementation of [TransitionRegistry] that provides custom
 * transitions for destination classes.
 *
 * This registry is created by [DslNavigationConfig] from transition
 * registrations collected by [NavigationConfigBuilder].
 *
 * ## Usage
 *
 * Transitions are registered via the DSL:
 *
 * ```kotlin
 * val config = navigationConfig {
 *     transition<DetailScreen>(NavTransition.SlideHorizontal)
 *     transition<ModalScreen>(NavTransition.SlideVertical)
 *     transition<SettingsScreen>(NavTransition.Fade)
 * }
 * ```
 *
 * The registry is then consulted during navigation to determine
 * the appropriate transition animation:
 *
 * ```kotlin
 * val transition = transitionRegistry.getTransition(DetailScreen::class)
 *     ?: AnimationCoordinator.defaultTransition
 * ```
 *
 * ## Fallback Behavior
 *
 * When no transition is registered for a destination, the registry returns
 * `null`, allowing the navigation system to fall back to default transitions
 * based on the navigation context.
 *
 * @param transitions Map of destination classes to their custom transitions
 *
 * @see TransitionRegistry
 * @see NavTransition
 * @see NavigationConfigBuilder.transition
 */
@InternalQuoVadisApi
@Stable
internal class DslTransitionRegistry(
    private val transitions: Map<KClass<out NavDestination>, NavTransition>
) : TransitionRegistry {

    /**
     * Gets the custom transition configuration for a destination class.
     *
     * @param destinationClass The [KClass] of the destination to look up
     * @return The [NavTransition] for this destination, or `null` if not registered
     */
    override fun getTransition(destinationClass: KClass<*>): NavTransition? {
        return transitions[destinationClass]
    }
}
