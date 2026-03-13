@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.config

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import com.jermey.quo.vadis.core.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.registry.ScreenRegistry
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeContainerRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeScreenRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeScopeRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeTransitionRegistry
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

    init {
        @OptIn(InternalQuoVadisApi::class)
        primary.setNodeResolver(::buildNavNode)
        @OptIn(InternalQuoVadisApi::class)
        secondary.setNodeResolver(::buildNavNode)
    }

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

    @InternalQuoVadisApi
    override fun setNodeResolver(
        resolver: ((KClass<out NavDestination>, String?, String?) -> NavNode?)?
    ) {
        @OptIn(InternalQuoVadisApi::class)
        primary.setNodeResolver(resolver)
        @OptIn(InternalQuoVadisApi::class)
        secondary.setNodeResolver(resolver)
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
        val secondaryNode = secondary.buildNavNode(destinationClass, key, parentKey)
        val primaryNode = primary.buildNavNode(destinationClass, key, parentKey)

        // Merge TabNodes from different modules (cross-module tab support)
        if (primaryNode is TabNode && secondaryNode is TabNode) {
            return mergeTabNodes(primaryNode, secondaryNode)
        }

        return secondaryNode ?: primaryNode
    }

    /**
     * Merges two TabNodes from different modules by appending non-duplicate
     * stacks from [secondary] after [primary]'s stacks, re-keying them to
     * avoid key collisions (both modules generate 0-based tab indices).
     */
    private fun mergeTabNodes(primary: TabNode, secondary: TabNode): TabNode {
        val primaryRoutes = primary.tabMetadata.map { it.route }.toSet()

        // Find secondary tabs that aren't already in primary (by route)
        val newIndices = secondary.tabMetadata.indices
            .filter { i -> secondary.tabMetadata[i].route !in primaryRoutes }

        // Re-key secondary stacks with indices continuing after primary
        val tabNodeKey = primary.key.value
        val additionalStacks = newIndices.mapIndexed { offset, secondaryIndex ->
            val newTabIndex = primary.stacks.size + offset
            val newStackKey = "$tabNodeKey/tab$newTabIndex"
            rekeyStack(secondary.stacks[secondaryIndex], newStackKey, tabNodeKey)
        }
        val additionalMetadata = newIndices.map { secondary.tabMetadata[it] }

        return TabNode(
            key = primary.key,
            parentKey = primary.parentKey,
            stacks = primary.stacks + additionalStacks,
            activeStackIndex = 0,
            wrapperKey = primary.wrapperKey ?: secondary.wrapperKey,
            tabMetadata = primary.tabMetadata + additionalMetadata,
            scopeKey = primary.scopeKey ?: secondary.scopeKey
        )
    }

    /**
     * Creates a copy of [stack] with a new key and parentKey,
     * recursively replacing the old key prefix with the new one
     * throughout the entire subtree.
     */
    private fun rekeyStack(
        stack: StackNode,
        newKey: String,
        newParentKey: String
    ): StackNode {
        val oldPrefix = stack.key.value
        val nodeKey = NodeKey(newKey)
        return stack.copy(
            key = nodeKey,
            parentKey = NodeKey(newParentKey),
            children = stack.children.map { child -> rekeySubtree(child, oldPrefix, newKey) }
        )
    }

    /**
     * Recursively replaces [oldPrefix] with [newPrefix] in all keys
     * and parentKeys throughout the node subtree.
     */
    private fun rekeySubtree(node: NavNode, oldPrefix: String, newPrefix: String): NavNode {
        fun rekey(key: NodeKey): NodeKey =
            NodeKey(key.value.replaceFirst(oldPrefix, newPrefix))

        fun rekeyParent(key: NodeKey?): NodeKey? =
            key?.let { NodeKey(it.value.replaceFirst(oldPrefix, newPrefix)) }

        return when (node) {
            is ScreenNode -> node.copy(
                key = rekey(node.key),
                parentKey = rekeyParent(node.parentKey)
            )
            is StackNode -> node.copy(
                key = rekey(node.key),
                parentKey = rekeyParent(node.parentKey),
                children = node.children.map { rekeySubtree(it, oldPrefix, newPrefix) }
            )
            is TabNode -> TabNode(
                key = rekey(node.key),
                parentKey = rekeyParent(node.parentKey),
                stacks = node.stacks.map {
                    rekeySubtree(it, oldPrefix, newPrefix) as StackNode
                },
                activeStackIndex = node.activeStackIndex,
                wrapperKey = node.wrapperKey,
                tabMetadata = node.tabMetadata,
                scopeKey = node.scopeKey
            )
            is PaneNode -> PaneNode(
                key = rekey(node.key),
                parentKey = rekeyParent(node.parentKey),
                paneConfigurations = node.paneConfigurations,
                activePaneRole = node.activePaneRole,
                backBehavior = node.backBehavior,
                scopeKey = node.scopeKey
            )
        }
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

