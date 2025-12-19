package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavNode
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
     * Returns secondary's deep link handler if available, otherwise primary's.
     *
     * If both configs provide handlers, a composite handler is created that
     * tries secondary first, then falls back to primary.
     */
    override val deepLinkHandler: GeneratedDeepLinkHandler? = when {
        secondary.deepLinkHandler != null && primary.deepLinkHandler != null -> {
            CompositeDeepLinkHandler(primary.deepLinkHandler!!, secondary.deepLinkHandler!!)
        }
        secondary.deepLinkHandler != null -> secondary.deepLinkHandler
        else -> primary.deepLinkHandler
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

