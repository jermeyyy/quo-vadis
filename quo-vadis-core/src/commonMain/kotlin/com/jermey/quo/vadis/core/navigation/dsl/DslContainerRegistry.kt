package com.jermey.quo.vadis.core.navigation.dsl

import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerInfo
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlin.reflect.KClass

/**
 * DSL-based implementation of [ContainerRegistry] that provides container
 * information for destinations requiring container structures.
 *
 * This registry is created by [DslNavigationConfig] from container
 * registrations collected by [NavigationConfigBuilder].
 *
 * ## Purpose
 *
 * The ContainerRegistry enables automatic container creation during navigation.
 * When navigating to a destination that belongs to a `@Tabs` or `@Pane` container,
 * this registry provides the builder function to create the appropriate container
 * node structure.
 *
 * ## Usage
 *
 * Containers are registered via the DSL:
 *
 * ```kotlin
 * val config = navigationConfig {
 *     tabs<MainTabs>("main-tabs") {
 *         initialTab = 0
 *         tab(HomeTab, title = "Home")
 *         tab(ProfileTab, title = "Profile")
 *     }
 *
 *     panes<ListDetail>("list-detail") {
 *         initialPane = PaneRole.Primary
 *         primary { root(ListScreen) }
 *         secondary { root(DetailScreen) }
 *     }
 * }
 * ```
 *
 * The registry is then consulted to create container nodes:
 *
 * ```kotlin
 * val containerInfo = containerRegistry.getContainerInfo(destination)
 * if (containerInfo is ContainerInfo.TabContainer) {
 *     val tabNode = containerInfo.builder(key, parentKey, initialTabIndex)
 *     // Push tabNode onto navigation stack
 * }
 * ```
 *
 * @param containers Map of destination classes to their container builders
 * @param navNodeBuilder Function to build NavNodes from destination classes
 *
 * @see ContainerRegistry
 * @see ContainerInfo
 * @see NavigationConfigBuilder.tabs
 * @see NavigationConfigBuilder.panes
 */
internal class DslContainerRegistry(
    private val containers: Map<KClass<out Destination>, ContainerBuilder>,
    private val navNodeBuilder: (KClass<out Destination>, String?, String?) -> NavNode?
) : ContainerRegistry {

    /**
     * Mapping from tab member destinations to their container class.
     *
     * Built lazily to enable looking up containers by member destinations.
     */
    private val tabMemberToContainer: Map<KClass<out Destination>, KClass<out Destination>> by lazy {
        buildTabMemberMapping()
    }

    /**
     * Mapping from pane member destinations to their container class.
     */
    private val paneMemberToContainer: Map<KClass<out Destination>, KClass<out Destination>> by lazy {
        buildPaneMemberMapping()
    }

    /**
     * Check if a destination requires a container structure.
     *
     * Returns container information if:
     * 1. The destination is a registered container root (e.g., MainTabs itself)
     * 2. The destination is a member of a registered container (e.g., HomeTab within MainTabs)
     *
     * @param destination The destination being navigated to
     * @return [ContainerInfo] if destination needs a container, null otherwise
     */
    @Suppress("ReturnCount")
    override fun getContainerInfo(destination: Destination): ContainerInfo? {
        val destClass = destination::class

        // First check if this is a container root
        containers[destClass]?.let { builder ->
            return createContainerInfo(builder, destClass)
        }

        // Check if this destination is a tab member
        tabMemberToContainer[destClass]?.let { containerClass ->
            val builder = containers[containerClass]
            if (builder is ContainerBuilder.Tabs) {
                val tabIndex = findTabIndex(builder, destClass)
                return createTabContainerInfo(builder, containerClass, tabIndex)
            }
        }

        // Check if this destination is a pane member
        paneMemberToContainer[destClass]?.let { containerClass ->
            val builder = containers[containerClass]
            if (builder is ContainerBuilder.Panes) {
                return createPaneContainerInfo(builder, containerClass)
            }
        }

        return null
    }

    /**
     * Creates container info based on the builder type.
     */
    private fun createContainerInfo(
        builder: ContainerBuilder,
        containerClass: KClass<out Destination>
    ): ContainerInfo? {
        return when (builder) {
            is ContainerBuilder.Stack -> null // Stacks don't create ContainerInfo
            is ContainerBuilder.Tabs -> createTabContainerInfo(builder, containerClass, 0)
            is ContainerBuilder.Panes -> createPaneContainerInfo(builder, containerClass)
        }
    }

    /**
     * Creates TabContainer info with the builder function.
     */
    private fun createTabContainerInfo(
        builder: ContainerBuilder.Tabs,
        containerClass: KClass<out Destination>,
        initialTabIndex: Int
    ): ContainerInfo.TabContainer {
        return ContainerInfo.TabContainer(
            builder = { key, parentKey, tabIndex ->
                val node = navNodeBuilder(containerClass, key, parentKey)
                    ?: throw IllegalStateException("Failed to build TabNode for $containerClass")

                if (node !is TabNode) {
                    throw IllegalStateException("Expected TabNode but got ${node::class}")
                }

                // Return with correct initial tab index
                if (tabIndex != node.activeStackIndex) {
                    node.copy(activeStackIndex = tabIndex.coerceIn(0, node.stacks.size - 1))
                } else {
                    node
                }
            },
            initialTabIndex = initialTabIndex,
            scopeKey = builder.scopeKey
        )
    }

    /**
     * Creates PaneContainer info with the builder function.
     */
    private fun createPaneContainerInfo(
        builder: ContainerBuilder.Panes,
        containerClass: KClass<out Destination>
    ): ContainerInfo.PaneContainer {
        return ContainerInfo.PaneContainer(
            builder = { key, parentKey ->
                val node = navNodeBuilder(containerClass, key, parentKey)
                    ?: throw IllegalStateException("Failed to build PaneNode for $containerClass")

                if (node !is PaneNode) {
                    throw IllegalStateException("Expected PaneNode but got ${node::class}")
                }

                node
            },
            initialPane = builder.config.initialPane,
            scopeKey = builder.scopeKey
        )
    }

    /**
     * Finds the tab index for a destination within a tabs container.
     *
     * For [TabEntry.ContainerReference] entries, this also checks if the
     * destination is a member of the referenced container.
     */
    @Suppress("ReturnCount")
    private fun findTabIndex(
        builder: ContainerBuilder.Tabs,
        destClass: KClass<out Destination>
    ): Int {
        builder.config.tabs.forEachIndexed { index, entry ->
            when (entry) {
                is TabEntry.FlatScreen -> {
                    if (entry.destinationClass == destClass) return index
                }
                is TabEntry.NestedStack -> {
                    if (entry.destinationClass == destClass) return index
                    entry.screens.forEach { screen ->
                        if (screen.destinationClass == destClass) return index
                    }
                }
                is TabEntry.ContainerReference -> {
                    if (entry.containerClass == destClass) return index
                    // Check if destination is a member of the referenced container
                    if (isDestinationInContainer(entry.containerClass, destClass)) return index
                }
            }
        }
        return builder.config.initialTab
    }

    /**
     * Checks if a destination class is a member of a container.
     *
     * @param containerClass The container to check
     * @param destClass The destination class to look for
     * @return true if destClass is within the container
     */
    private fun isDestinationInContainer(
        containerClass: KClass<out Destination>,
        destClass: KClass<out Destination>
    ): Boolean {
        val containerBuilder = containers[containerClass] ?: return false

        return when (containerBuilder) {
            is ContainerBuilder.Stack -> {
                containerBuilder.screens.any { it.destinationClass == destClass }
            }
            is ContainerBuilder.Tabs -> {
                containerBuilder.config.tabs.any { entry ->
                    when (entry) {
                        is TabEntry.FlatScreen -> entry.destinationClass == destClass
                        is TabEntry.NestedStack -> {
                            entry.destinationClass == destClass ||
                                entry.screens.any { it.destinationClass == destClass }
                        }
                        is TabEntry.ContainerReference -> {
                            entry.containerClass == destClass ||
                                isDestinationInContainer(entry.containerClass, destClass)
                        }
                    }
                }
            }
            is ContainerBuilder.Panes -> {
                containerBuilder.config.panes.values.any { entry ->
                    entry.content.rootDestination?.let { it::class == destClass } ?: false
                }
            }
        }
    }

    /**
     * Builds mapping from tab member destinations to their container class.
     *
     * For [TabEntry.ContainerReference] entries, this also recursively includes
     * all destinations from the referenced container, enabling navigation to
     * destinations within nested containers (e.g., `DemoTabs.BooksTab.List`)
     * to correctly create the parent tab container (`DemoTabs`).
     */
    @Suppress("NestedBlockDepth")
    private fun buildTabMemberMapping(): Map<KClass<out Destination>, KClass<out Destination>> {
        val result = mutableMapOf<KClass<out Destination>, KClass<out Destination>>()

        containers.forEach { (containerClass, builder) ->
            if (builder is ContainerBuilder.Tabs) {
                builder.config.tabs.forEach { entry ->
                    when (entry) {
                        is TabEntry.FlatScreen -> {
                            result[entry.destinationClass] = containerClass
                        }
                        is TabEntry.NestedStack -> {
                            result[entry.destinationClass] = containerClass
                            entry.screens.forEach { screen ->
                                screen.destinationClass?.let {
                                    result[it] = containerClass
                                }
                            }
                        }
                        is TabEntry.ContainerReference -> {
                            result[entry.containerClass] = containerClass
                            // Also map all destinations from the referenced container
                            // This enables navigating to nested destinations like
                            // DemoTabs.BooksTab.List to create the DemoTabs container
                            addReferencedContainerMembers(entry.containerClass, containerClass, result)
                        }
                    }
                }
            }
        }

        return result
    }

    /**
     * Recursively adds members of a referenced container to the tab member mapping.
     *
     * When a tab uses [TabEntry.ContainerReference], the destinations within
     * that referenced container should also map to the parent tab container.
     * This enables navigation like `DemoTabs.BooksTab.List` to correctly create
     * the `DemoTabs` container with `DemoTabsWrapper`.
     *
     * @param referencedClass The container class being referenced
     * @param tabContainerClass The parent tab container class
     * @param result The mapping being built
     */
    private fun addReferencedContainerMembers(
        referencedClass: KClass<out Destination>,
        tabContainerClass: KClass<out Destination>,
        result: MutableMap<KClass<out Destination>, KClass<out Destination>>
    ) {
        val referencedBuilder = containers[referencedClass] ?: return

        when (referencedBuilder) {
            is ContainerBuilder.Stack -> {
                // Add all screen destinations from the stack
                referencedBuilder.screens.forEach { screen ->
                    screen.destinationClass?.let {
                        result[it] = tabContainerClass
                    }
                }
            }
            is ContainerBuilder.Tabs -> {
                // Recursively add tab members
                referencedBuilder.config.tabs.forEach { entry ->
                    when (entry) {
                        is TabEntry.FlatScreen -> {
                            result[entry.destinationClass] = tabContainerClass
                        }
                        is TabEntry.NestedStack -> {
                            result[entry.destinationClass] = tabContainerClass
                            entry.screens.forEach { screen ->
                                screen.destinationClass?.let {
                                    result[it] = tabContainerClass
                                }
                            }
                        }
                        is TabEntry.ContainerReference -> {
                            result[entry.containerClass] = tabContainerClass
                            addReferencedContainerMembers(entry.containerClass, tabContainerClass, result)
                        }
                    }
                }
            }
            is ContainerBuilder.Panes -> {
                // Add pane member destinations
                referencedBuilder.config.panes.values.forEach { entry ->
                    entry.content.rootDestination?.let { dest ->
                        result[dest::class] = tabContainerClass
                    }
                }
            }
        }
    }

    /**
     * Builds mapping from pane member destinations to their container class.
     */
    private fun buildPaneMemberMapping(): Map<KClass<out Destination>, KClass<out Destination>> {
        val result = mutableMapOf<KClass<out Destination>, KClass<out Destination>>()

        containers.forEach { (containerClass, builder) ->
            if (builder is ContainerBuilder.Panes) {
                builder.config.panes.values.forEach { entry ->
                    entry.content.rootDestination?.let { dest ->
                        result[dest::class] = containerClass
                    }
                }
            }
        }

        return result
    }
}
