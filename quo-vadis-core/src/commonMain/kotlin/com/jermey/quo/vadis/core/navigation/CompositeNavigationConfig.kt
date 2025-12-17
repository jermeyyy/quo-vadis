package com.jermey.quo.vadis.core.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerInfo
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.Navigator
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
internal class CompositeNavigationConfig(
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
     */
    override val containerRegistry: ContainerRegistry = CompositeContainerRegistry(
        primary = primary.containerRegistry,
        secondary = secondary.containerRegistry
    )

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
        destinationClass: KClass<out Destination>,
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

/**
 * Composite screen registry that delegates to secondary first, then primary.
 */
private class CompositeScreenRegistry(
    private val primary: ScreenRegistry,
    private val secondary: ScreenRegistry
) : ScreenRegistry {

    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        if (secondary.hasContent(destination)) {
            secondary.Content(destination, navigator, sharedTransitionScope, animatedVisibilityScope)
        } else {
            primary.Content(destination, navigator, sharedTransitionScope, animatedVisibilityScope)
        }
    }

    override fun hasContent(destination: Destination): Boolean {
        return secondary.hasContent(destination) || primary.hasContent(destination)
    }
}

/**
 * Composite scope registry that delegates to secondary first, then primary.
 */
private class CompositeScopeRegistry(
    private val primary: ScopeRegistry,
    private val secondary: ScopeRegistry
) : ScopeRegistry {

    override fun isInScope(scopeKey: String, destination: Destination): Boolean {
        // Check secondary first for an explicit registration
        val secondaryScopeKey = secondary.getScopeKey(destination)
        if (secondaryScopeKey != null) {
            return secondary.isInScope(scopeKey, destination)
        }
        // Fall back to primary
        return primary.isInScope(scopeKey, destination)
    }

    override fun getScopeKey(destination: Destination): String? {
        return secondary.getScopeKey(destination) ?: primary.getScopeKey(destination)
    }
}

/**
 * Composite transition registry that delegates to secondary first, then primary.
 */
private class CompositeTransitionRegistry(
    private val primary: TransitionRegistry,
    private val secondary: TransitionRegistry
) : TransitionRegistry {

    override fun getTransition(destinationClass: KClass<*>): NavTransition? {
        return secondary.getTransition(destinationClass) ?: primary.getTransition(destinationClass)
    }
}

/**
 * Composite container registry that delegates to secondary first, then primary.
 * Handles both container building (getContainerInfo) and wrapper rendering
 * (TabsContainer, PaneContainer).
 */
private class CompositeContainerRegistry(
    private val primary: ContainerRegistry,
    private val secondary: ContainerRegistry
) : ContainerRegistry {

    override fun getContainerInfo(destination: Destination): ContainerInfo? {
        return secondary.getContainerInfo(destination) ?: primary.getContainerInfo(destination)
    }

    @Composable
    override fun TabsContainer(
        tabNodeKey: String,
        scope: TabsContainerScope,
        content: @Composable () -> Unit
    ) {
        if (secondary.hasTabsContainer(tabNodeKey)) {
            secondary.TabsContainer(tabNodeKey, scope, content)
        } else {
            primary.TabsContainer(tabNodeKey, scope, content)
        }
    }

    @Composable
    override fun PaneContainer(
        paneNodeKey: String,
        scope: PaneContainerScope,
        content: @Composable () -> Unit
    ) {
        if (secondary.hasPaneContainer(paneNodeKey)) {
            secondary.PaneContainer(paneNodeKey, scope, content)
        } else {
            primary.PaneContainer(paneNodeKey, scope, content)
        }
    }

    override fun hasTabsContainer(tabNodeKey: String): Boolean {
        return secondary.hasTabsContainer(tabNodeKey) || primary.hasTabsContainer(tabNodeKey)
    }

    override fun hasPaneContainer(paneNodeKey: String): Boolean {
        return secondary.hasPaneContainer(paneNodeKey) || primary.hasPaneContainer(paneNodeKey)
    }
}

/**
 * Composite deep link handler that delegates to secondary first, then primary.
 */
private class CompositeDeepLinkHandler(
    private val primary: GeneratedDeepLinkHandler,
    private val secondary: GeneratedDeepLinkHandler
) : GeneratedDeepLinkHandler {

    override fun handleDeepLink(uri: String): DeepLinkResult {
        val secondaryResult = secondary.handleDeepLink(uri)
        if (secondaryResult is DeepLinkResult.Matched) {
            return secondaryResult
        }
        return primary.handleDeepLink(uri)
    }

    override fun createDeepLinkUri(destination: Destination, scheme: String): String? {
        return secondary.createDeepLinkUri(destination, scheme)
            ?: primary.createDeepLinkUri(destination, scheme)
    }

    override fun resolve(deepLink: DeepLink): Destination? {
        return secondary.resolve(deepLink) ?: primary.resolve(deepLink)
    }

    override fun register(
        pattern: String,
        action: (navigator: Navigator, parameters: Map<String, String>) -> Unit
    ) {
        // Register on both handlers to support full pattern matching
        secondary.register(pattern, action)
        primary.register(pattern, action)
    }

    override fun handle(deepLink: DeepLink, navigator: Navigator) {
        // Try secondary first, then primary
        val secondaryResolved = secondary.resolve(deepLink)
        if (secondaryResolved != null) {
            secondary.handle(deepLink, navigator)
        } else {
            primary.handle(deepLink, navigator)
        }
    }
}
