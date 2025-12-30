package com.jermey.quo.vadis.core.navigation.config

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.dsl.registry.ContainerRegistry
import com.jermey.quo.vadis.core.dsl.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry
import com.jermey.quo.vadis.core.dsl.registry.ScreenRegistry
import com.jermey.quo.vadis.core.dsl.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.config.EmptyNavigationConfig.plus
import com.jermey.quo.vadis.core.dsl.registry.DeepLinkRegistry
import com.jermey.quo.vadis.core.navigation.NavDestination
import kotlin.reflect.KClass

/**
 * Empty implementation of [NavigationConfig] with no registrations.
 *
 * All registries return their respective [Empty] implementations,
 * and all lookups return null or false.
 *
 * ## Identity Element
 *
 * This implementation acts as an identity element for the [plus] operator:
 * - `Empty + config` returns `config` (delegated to config's plus)
 * - `config + Empty` returns `config` (since Empty contributes nothing)
 *
 * ## Usage
 *
 * ```kotlin
 * // Use as default when no config is provided
 * val config = providedConfig ?: NavigationConfig.Empty
 *
 * // Starting point for building configs
 * var config: NavigationConfig = NavigationConfig.Empty
 * config = config + featureAConfig
 * config = config + featureBConfig
 * ```
 */
internal object EmptyNavigationConfig : NavigationConfig {

    /**
     * Returns a no-op screen registry that throws for any content rendering.
     *
     * This is intentionally not using [ScreenRegistry.Empty] pattern since
     * screen registry doesn't have an Empty companion. Content rendering
     * will fail if attempted with this empty config.
     */
    override val screenRegistry: ScreenRegistry = object : ScreenRegistry {
        @Composable
        override fun Content(
            destination: NavDestination,
            sharedTransitionScope: SharedTransitionScope?,
            animatedVisibilityScope: AnimatedVisibilityScope?
        ) {
            // Empty implementation - no content registered
        }

        override fun hasContent(destination: NavDestination): Boolean = false
    }

    /**
     * Returns [ScopeRegistry.Empty] which allows all destinations in all scopes.
     */
    override val scopeRegistry: ScopeRegistry = ScopeRegistry.Empty

    /**
     * Returns [TransitionRegistry.Empty] which returns null for all lookups.
     */
    override val transitionRegistry: TransitionRegistry = TransitionRegistry.Empty

    /**
     * Returns [ContainerRegistry.Empty] which never creates containers
     * and provides default pass-through wrapper behavior.
     */
    override val containerRegistry: ContainerRegistry = ContainerRegistry.Empty

    /**
     * Returns [DeepLinkRegistry.Empty] which provides no-op behavior.
     */
    override val deepLinkRegistry: DeepLinkRegistry = DeepLinkRegistry.Empty

    /**
     * Returns [PaneRoleRegistry.Empty] which returns null for all lookups.
     */
    override val paneRoleRegistry: PaneRoleRegistry = PaneRoleRegistry.Empty

    /**
     * Always returns null since no destinations are registered.
     *
     * @param destinationClass The destination class to build a node for
     * @param key Optional explicit key for the node
     * @param parentKey Optional key of the parent node
     * @return Always null in empty config
     */
    override fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String?,
        parentKey: String?
    ): NavNode? = null

    /**
     * Returns the other config unchanged.
     *
     * Since this is the empty config, combining with any other config
     * should simply return the other config (identity element behavior).
     *
     * @param other The config to combine with
     * @return [other] unchanged
     */
    override fun plus(other: NavigationConfig): NavigationConfig = other
}
