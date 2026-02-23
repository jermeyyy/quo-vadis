@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.config

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.registry.internal.CompositeContainerRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeScopeRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeScreenRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeTransitionRegistry
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.registry.ScreenRegistry
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.config.EmptyNavigationConfig
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlin.reflect.KClass

/**
 * Composite implementation of [NavigationConfig] that combines two configs.
 *
 * This class implements the composition behavior for the [NavigationConfig.plus] operator.
 * When combining configs, the [secondary] (right-hand) config takes priority for
 * duplicate registrations, with [primary] (left-hand) as fallback.
 *
 * ## Priority Rules
 *
 * For all registry lookups:
 * 1. First check [secondary]'s registry
 * 2. If not found/null, fall back to [primary]'s registry
 *
 * ## Example
 *
 * ```kotlin
 * val combined = primaryConfig + secondaryConfig
 * // secondaryConfig takes priority for duplicate keys
 *
 * // Chaining preserves priority (left-to-right, later wins)
 * val app = config1 + config2 + config3
 * // Equivalent to: (config1 + config2) + config3
 * // config3 has highest priority, config1 has lowest
 * ```
 *
 * @param primary The base config (lower priority)
 * @param secondary The overlay config (higher priority)
 */
class CompositeNavigationConfig(
    private val primary: NavigationConfig,
    private val secondary: NavigationConfig
) : NavigationConfig {

    /**
     * Composite screen registry that checks secondary first, then primary.
     */
    override val screenRegistry: ScreenRegistry = CompositeScreenRegistry(
        primary = primary.screenRegistry,
        secondary = secondary.screenRegistry
    )

    /**
     * Composite scope registry that checks secondary first, then primary.
     */
    override val scopeRegistry: ScopeRegistry = CompositeScopeRegistry(
        primary = primary.scopeRegistry,
        secondary = secondary.scopeRegistry
    )

    /**
     * Composite transition registry that checks secondary first, then primary.
     */
    override val transitionRegistry: TransitionRegistry = CompositeTransitionRegistry(
        primary = primary.transitionRegistry,
        secondary = secondary.transitionRegistry
    )

    /**
     * Composite container registry that checks secondary first, then primary.
     * Handles both container building and wrapper rendering.
     *
     * Uses lazy initialization to allow passing [buildNavNode] reference,
     * which enables cross-config destination resolution when building
     * container nodes.
     */
    override val containerRegistry: ContainerRegistry by lazy {
        CompositeContainerRegistry(
            primary = primary.containerRegistry,
            secondary = secondary.containerRegistry,
            navNodeBuilder = ::buildNavNode
        )
    }

    /**
     * Composite deep link registry that tries secondary first, then falls back to primary.
     *
     * Creates an anonymous registry that delegates to both registries appropriately:
     * - Lookups try secondary first, then primary
     * - Registrations go to secondary (higher priority)
     * - Pattern lists combine both registries
     */
    override val deepLinkRegistry: DeepLinkRegistry = run {
        val secondaryReg = secondary.deepLinkRegistry
        val primaryReg = primary.deepLinkRegistry
        object : DeepLinkRegistry {
            override fun resolve(uri: String): NavDestination? =
                secondaryReg.resolve(uri) ?: primaryReg.resolve(uri)

            override fun resolve(deepLink: DeepLink): NavDestination? =
                secondaryReg.resolve(deepLink) ?: primaryReg.resolve(deepLink)

            override fun register(
                pattern: String,
                factory: (params: Map<String, String>) -> NavDestination
            ) {
                // Delegate to secondary (higher priority)
                secondaryReg.register(pattern, factory)
            }

            override fun registerAction(
                pattern: String,
                action: (navigator: Navigator, params: Map<String, String>) -> Unit
            ) {
                secondaryReg.registerAction(pattern, action)
            }

            override fun handle(uri: String, navigator: Navigator): Boolean =
                secondaryReg.handle(uri, navigator) || primaryReg.handle(uri, navigator)

            override fun createUri(destination: NavDestination, scheme: String): String? =
                secondaryReg.createUri(destination, scheme) ?: primaryReg.createUri(
                    destination,
                    scheme
                )

            override fun canHandle(uri: String): Boolean =
                secondaryReg.canHandle(uri) || primaryReg.canHandle(uri)

            override fun getRegisteredPatterns(): List<String> =
                secondaryReg.getRegisteredPatterns() + primaryReg.getRegisteredPatterns()
        }
    }

    /**
     * Composite pane role registry that checks secondary first, then primary.
     */
    override val paneRoleRegistry: PaneRoleRegistry = run {
        val secondaryReg = secondary.paneRoleRegistry
        val primaryReg = primary.paneRoleRegistry
        object : PaneRoleRegistry {
            override fun getPaneRole(scopeKey: ScopeKey, destination: NavDestination): PaneRole? =
                secondaryReg.getPaneRole(scopeKey, destination)
                    ?: primaryReg.getPaneRole(scopeKey, destination)

            override fun getPaneRole(
                scopeKey: ScopeKey,
                destinationClass: kotlin.reflect.KClass<out NavDestination>
            ): PaneRole? =
                secondaryReg.getPaneRole(scopeKey, destinationClass)
                    ?: primaryReg.getPaneRole(scopeKey, destinationClass)
        }
    }

    /**
     * Builds a NavNode, checking secondary first, then primary.
     *
     * @param destinationClass The destination class to build a node for
     * @param key Optional explicit key for the node
     * @param parentKey Optional key of the parent node
     * @return NavNode from secondary if available, otherwise from primary
     */
    override fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String?,
        parentKey: String?
    ): NavNode? {
        return secondary.buildNavNode(destinationClass, key, parentKey)
            ?: primary.buildNavNode(destinationClass, key, parentKey)
    }

    /**
     * Creates a new composite config by combining this config with another.
     *
     * @param other The config to combine with this one
     * @return A new [CompositeNavigationConfig] with [other] as secondary
     */
    override fun plus(other: NavigationConfig): NavigationConfig {
        // Optimize: if other is Empty, return this unchanged
        if (other === EmptyNavigationConfig) return this
        return CompositeNavigationConfig(primary = this, secondary = other)
    }
}

