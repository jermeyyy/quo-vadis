package com.jermey.quo.vadis.core.dsl.internal

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.dsl.ContainerBuilder
import com.jermey.quo.vadis.core.dsl.StackScreenEntry
import com.jermey.quo.vadis.core.dsl.TabEntry
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.registry.ScreenRegistry
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.internal.config.CompositeNavigationConfig
import com.jermey.quo.vadis.core.navigation.internal.config.EmptyNavigationConfig
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import com.jermey.quo.vadis.core.navigation.internal.GeneratedTabMetadata
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlin.reflect.KClass

/**
 * Implementation of [NavigationConfig] that converts DSL builder configurations
 * into runtime-usable navigation registries.
 *
 * This class is the concrete implementation created by the [navigationConfig] DSL function.
 * It transforms the builder-collected data into proper registry implementations that
 * can be used by the navigation system.
 *
 * ## Usage
 *
 * ```kotlin
 * val config = navigationConfig {
 *     screen<HomeScreen> { destination, navigator, _, _ ->
 *         HomeScreenContent(destination, navigator)
 *     }
 *
 *     tabs<MainTabs>("main-tabs") {
 *         tab(HomeTab, title = "Home", icon = Icons.Home)
 *         tab(ProfileTab, title = "Profile", icon = Icons.Person)
 *     }
 * }
 *
 * val navigator = rememberQuoVadisNavigator(MainTabs::class, config)
 * NavigationHost(navigator)  // Config is read from navigator
 * ```
 *
 * @param screens Map of destination classes to their screen entry configurations
 * @param containers Map of destination classes to their container builder configurations
 * @param scopes Map of scope keys to sets of destination classes belonging to each scope
 * @param transitions Map of destination classes to their custom transitions
 * @param tabsContainers Map of wrapper keys to tab container composables
 * @param paneContainers Map of wrapper keys to pane container composables
 *
 * @see NavigationConfig
 * @see navigationConfig
 */
@InternalQuoVadisApi
internal class DslNavigationConfig(
    private val screens: Map<KClass<out NavDestination>, ScreenEntry>,
    private val containers: Map<KClass<out NavDestination>, ContainerBuilder>,
    private val scopes: Map<ScopeKey, Set<KClass<out NavDestination>>>,
    private val transitions: Map<KClass<out NavDestination>, NavTransition>,
    private val tabsContainers: Map<String, @Composable TabsContainerScope.(@Composable () -> Unit) -> Unit>,
    private val paneContainers: Map<String, @Composable PaneContainerScope.(@Composable () -> Unit) -> Unit>
) : NavigationConfig {

    /**
     * Registry for rendering screen content based on destination type.
     */
    override val screenRegistry: ScreenRegistry by lazy {
        DslScreenRegistry(screens)
    }

    /**
     * Registry for determining scope membership of destinations.
     */
    override val scopeRegistry: ScopeRegistry by lazy {
        DslScopeRegistry(buildCombinedScopes())
    }

    /**
     * Registry for destination-specific transition animations.
     */
    override val transitionRegistry: TransitionRegistry by lazy {
        DslTransitionRegistry(transitions)
    }

    /**
     * Registry for building container nodes from destinations and wrapper composables.
     */
    override val containerRegistry: ContainerRegistry by lazy {
        DslContainerRegistry(containers, tabsContainers, paneContainers, ::buildNavNode)
    }

    /**
     * Deep link registry - returns [DeepLinkRegistry.Companion.Empty] for DSL-based configs.
     * Full deep link support requires KSP generation.
     */
    override val deepLinkRegistry: DeepLinkRegistry = DeepLinkRegistry.Companion.Empty

    /**
     * Builds the appropriate NavNode for the given destination class.
     *
     * Looks up the container configuration for the destination class and
     * creates the corresponding node structure (StackNode, TabNode, or PaneNode).
     *
     * @param destinationClass The destination class to build a node for
     * @param key Optional explicit key for the node (auto-generated if null)
     * @param parentKey Optional key of the parent node
     * @return The constructed NavNode, or null if no container is registered
     */
    override fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String?,
        parentKey: String?
    ): NavNode? {
        val containerBuilder = containers[destinationClass] ?: return null
        val effectiveKey = key ?: containerBuilder.scopeKey.value

        return when (containerBuilder) {
            is ContainerBuilder.Stack -> buildStackNode(containerBuilder, effectiveKey, parentKey)
            is ContainerBuilder.Tabs -> buildTabNode(containerBuilder, effectiveKey, parentKey)
            is ContainerBuilder.Panes -> buildPaneNode(containerBuilder, effectiveKey, parentKey)
        }
    }

    /**
     * Combines this config with another, creating a composite config.
     *
     * @param other The config to combine with this one
     * @return A new CompositeNavigationConfig with [other] taking priority
     */
    override fun plus(other: NavigationConfig): NavigationConfig {
        if (other === EmptyNavigationConfig) return this
        return CompositeNavigationConfig(this, other)
    }

    /**
     * Builds combined scopes from both explicit scope definitions and
     * auto-inferred scopes from container configurations.
     *
     * Container registrations automatically infer scope membership for
     * their contained destinations.
     *
     * @return Combined map of scope keys to destination class sets
     */
    private fun buildCombinedScopes(): Map<ScopeKey, Set<KClass<out NavDestination>>> {
        val combined = scopes.toMutableMap()

        // Add container-inferred scopes
        containers.values.forEach { builder ->
            val scopeKey = builder.scopeKey
            val members = combined.getOrPut(scopeKey) { mutableSetOf() }.toMutableSet()

            when (builder) {
                is ContainerBuilder.Stack -> {
                    builder.screens.forEach { entry ->
                        entry.destinationClass?.let { members.add(it) }
                    }
                }

                is ContainerBuilder.Tabs -> {
                    addTabEntriesToScope(builder.config, members)
                }

                is ContainerBuilder.Panes -> {
                    // Pane destinations are inferred from root destinations
                    builder.config.panes.values.forEach { entry ->
                        entry.content.rootDestination?.let { dest ->
                            members.add(dest::class)
                        }
                    }
                }
            }

            combined[scopeKey] = members
        }

        return combined
    }

    /**
     * Adds destination classes from tab entries to the scope member set.
     */
    private fun addTabEntriesToScope(
        config: BuiltTabsConfig,
        members: MutableSet<KClass<out NavDestination>>
    ) {
        config.tabs.forEach { entry ->
            when (entry) {
                is TabEntry.FlatScreen -> {
                    members.add(entry.destinationClass)
                }

                is TabEntry.NestedStack -> {
                    members.add(entry.destinationClass)
                    entry.screens.forEach { screen ->
                        screen.destinationClass?.let { members.add(it) }
                    }
                }

                is TabEntry.ContainerReference -> {
                    members.add(entry.containerClass)
                }
            }
        }
    }

    /**
     * Builds a StackNode from a stack container builder.
     *
     * @param builder The stack container configuration
     * @param key The key for the stack node
     * @param parentKey Optional parent node key
     * @return Constructed StackNode with initial children
     */
    private fun buildStackNode(
        builder: ContainerBuilder.Stack,
        key: String,
        parentKey: String?
    ): StackNode {
        val children = buildStackChildren(builder.screens, key)
        return StackNode(
            key = NodeKey(key),
            parentKey = parentKey?.let { NodeKey(it) },
            children = children,
            scopeKey = builder.scopeKey
        )
    }

    /**
     * Builds child nodes for a stack from screen entries.
     *
     * Only creates nodes for entries that have a destination instance.
     * Entries with only a destinationClass (no instance) are skipped,
     * as we cannot reflectively instantiate them in multiplatform code.
     *
     * @param screens List of screen entries to convert to nodes
     * @param stackKey The parent stack's key for constructing child keys
     * @return List of child NavNodes (typically ScreenNodes)
     */
    @Suppress("ReturnCount")
    private fun buildStackChildren(
        screens: List<StackScreenEntry>,
        stackKey: String
    ): List<NavNode> {
        if (screens.isEmpty()) return emptyList()

        // Only create the root screen initially if a destination instance is provided
        val rootEntry = screens.first()
        val rootDestination = rootEntry.destination ?: return emptyList()

        val rootKey = "$stackKey/root"
        return listOf(
            ScreenNode(
                key = NodeKey(rootKey),
                parentKey = NodeKey(stackKey),
                destination = rootDestination
            )
        )
    }

    /**
     * Builds a TabNode from a tabs container builder.
     *
     * Creates the tab node with all configured stacks, each containing
     * its root destination.
     *
     * @param builder The tabs container configuration
     * @param key The key for the tab node
     * @param parentKey Optional parent node key
     * @return Constructed TabNode with all tab stacks
     */
    private fun buildTabNode(
        builder: ContainerBuilder.Tabs,
        key: String,
        parentKey: String?
    ): TabNode {
        val config = builder.config
        val stacks = config.tabs.mapIndexed { index, tabEntry ->
            buildTabStack(tabEntry, key, index)
        }

        val tabMetadata = config.tabs.map { tabEntry ->
            GeneratedTabMetadata(
                route = getTabRoute(tabEntry)
            )
        }

        return TabNode(
            key = NodeKey(key),
            parentKey = parentKey?.let { NodeKey(it) },
            stacks = stacks,
            activeStackIndex = config.initialTab,
            wrapperKey = builder.scopeKey.value,
            tabMetadata = tabMetadata,
            scopeKey = builder.scopeKey
        )
    }

    /**
     * Builds a StackNode for a single tab entry.
     *
     * @param tabEntry The tab entry configuration
     * @param tabNodeKey The parent tab node's key
     * @param tabIndex The index of this tab (for key generation)
     * @return StackNode for this tab
     */
    private fun buildTabStack(
        tabEntry: TabEntry,
        tabNodeKey: String,
        tabIndex: Int
    ): StackNode {
        val stackKey = "$tabNodeKey/tab$tabIndex"

        val children = when (tabEntry) {
            is TabEntry.FlatScreen -> {
                listOf(
                    ScreenNode(
                        key = NodeKey("$stackKey/root"),
                        parentKey = NodeKey(stackKey),
                        destination = tabEntry.destination
                    )
                )
            }

            is TabEntry.NestedStack -> {
                listOf(
                    ScreenNode(
                        key = NodeKey("$stackKey/root"),
                        parentKey = NodeKey(stackKey),
                        destination = tabEntry.rootDestination
                    )
                )
            }

            is TabEntry.ContainerReference -> {
                // For container references, we need to build the referenced container
                val containerNode = buildNavNode(
                    tabEntry.containerClass,
                    stackKey,
                    tabNodeKey
                )
                if (containerNode != null) {
                    listOf(containerNode)
                } else {
                    emptyList()
                }
            }
        }

        return StackNode(
            key = NodeKey(stackKey),
            parentKey = NodeKey(tabNodeKey),
            children = children,
            scopeKey = null // Tab stacks don't have their own scope
        )
    }

    /**
     * Gets the route identifier for a tab entry.
     */
    private fun getTabRoute(tabEntry: TabEntry): String {
        return when (tabEntry) {
            is TabEntry.FlatScreen -> tabEntry.destinationClass.simpleName ?: "tab"
            is TabEntry.NestedStack -> tabEntry.destinationClass.simpleName ?: "tab"
            is TabEntry.ContainerReference -> tabEntry.containerClass.simpleName ?: "tab"
        }
    }

    /**
     * Builds a PaneNode from a panes container builder.
     *
     * Creates the pane node with all configured panes, each containing
     * its root destination in a stack.
     *
     * @param builder The panes container configuration
     * @param key The key for the pane node
     * @param parentKey Optional parent node key
     * @return Constructed PaneNode with all pane configurations
     */
    private fun buildPaneNode(
        builder: ContainerBuilder.Panes,
        key: String,
        parentKey: String?
    ): PaneNode {
        val config = builder.config
        val paneConfigurations = buildPaneConfigurations(config, key)

        return PaneNode(
            key = NodeKey(key),
            parentKey = parentKey?.let { NodeKey(it) },
            paneConfigurations = paneConfigurations,
            activePaneRole = config.initialPane,
            backBehavior = config.backBehavior,
            scopeKey = builder.scopeKey
        )
    }

    /**
     * Builds pane configurations from the builder config.
     *
     * @param config The built panes configuration
     * @param paneNodeKey The parent pane node's key
     * @return Map of pane roles to their configurations
     */
    private fun buildPaneConfigurations(
        config: BuiltPanesConfig,
        paneNodeKey: String
    ): Map<PaneRole, PaneConfiguration> {
        return config.panes.mapValues { (role, entry) ->
            val paneStackKey = "$paneNodeKey/${role.name.lowercase()}"

            val stackContent = if (entry.content.rootDestination != null) {
                StackNode(
                    key = NodeKey(paneStackKey),
                    parentKey = NodeKey(paneNodeKey),
                    children = listOf(
                        ScreenNode(
                            key = NodeKey("$paneStackKey/root"),
                            parentKey = NodeKey(paneStackKey),
                            destination = entry.content.rootDestination
                        )
                    ),
                    scopeKey = null
                )
            } else {
                StackNode(
                    key = NodeKey(paneStackKey),
                    parentKey = NodeKey(paneNodeKey),
                    children = emptyList(),
                    scopeKey = null
                )
            }

            PaneConfiguration(
                content = stackContent,
                adaptStrategy = if (entry.content.isAlwaysVisible) {
                    AdaptStrategy.Levitate
                } else {
                    AdaptStrategy.Hide
                }
            )
        }
    }
}