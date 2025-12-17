package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavNode
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
        @androidx.compose.runtime.Composable
        override fun Content(
            destination: Destination,
            navigator: com.jermey.quo.vadis.core.navigation.core.Navigator,
            sharedTransitionScope: androidx.compose.animation.SharedTransitionScope?,
            animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?
        ) {
            // Empty implementation - no content registered
        }

        override fun hasContent(destination: Destination): Boolean = false
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
     * Returns null since no deep link handling is configured.
     */
    override val deepLinkHandler: GeneratedDeepLinkHandler? = null

    /**
     * Always returns null since no destinations are registered.
     *
     * @param destinationClass The destination class to build a node for
     * @param key Optional explicit key for the node
     * @param parentKey Optional key of the parent node
     * @return Always null in empty config
     */
    override fun buildNavNode(
        destinationClass: KClass<out Destination>,
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
