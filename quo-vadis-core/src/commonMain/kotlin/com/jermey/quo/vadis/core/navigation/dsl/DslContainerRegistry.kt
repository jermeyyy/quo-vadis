package com.jermey.quo.vadis.core.navigation.dsl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerInfo
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlin.reflect.KClass

/**
 * DSL-based implementation of [ContainerRegistry] that provides container
 * information for destinations requiring container structures and custom
 * wrapper composables for tab and pane containers.
 *
 * This registry is created by [DslNavigationConfig] from container and wrapper
 * registrations collected by [NavigationConfigBuilder].
 *
 * ## Purpose
 *
 * The ContainerRegistry enables:
 * - Automatic container creation during navigation (for `@Tabs` and `@Pane` containers)
 * - Custom chrome/UI surrounding navigation content (tab bars, bottom navigation, multi-pane layouts)
 *
 * ## Usage
 *
 * Containers and wrappers are registered via the DSL:
 *
 * ```kotlin
 * val config = navigationConfig {
 *     tabs<MainTabs>("main-tabs") {
 *         initialTab = 0
 *         tab(HomeTab, title = "Home")
 *         tab(ProfileTab, title = "Profile")
 *     }
 *
 *     tabsContainer("main-tabs") { content ->
 *         Scaffold(
 *             bottomBar = { BottomNavigation(tabMetadata, activeTabIndex) }
 *         ) { content() }
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
 * @param containers Map of destination classes to their container builders
 * @param tabsContainers Map of wrapper keys to tabs container composables
 * @param paneContainers Map of wrapper keys to pane container composables
 * @param navNodeBuilder Function to build NavNodes from destination classes
 *
 * @see ContainerRegistry
 * @see ContainerInfo
 * @see TabsContainerScope
 * @see PaneContainerScope
 * @see NavigationConfigBuilder.tabs
 * @see NavigationConfigBuilder.panes
 * @see NavigationConfigBuilder.tabsContainer
 * @see NavigationConfigBuilder.paneContainer
 */
@Stable
internal class DslContainerRegistry(
    private val containers: Map<KClass<out NavDestination>, ContainerBuilder>,
    private val tabsContainers: Map<String, @Composable TabsContainerScope.(@Composable () -> Unit) -> Unit>,
    private val paneContainers: Map<String, @Composable PaneContainerScope.(@Composable () -> Unit) -> Unit>,
    private val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode?
) : ContainerRegistry {

    /**
     * Mapping from tab member destinations to their container class.
     *
     * Built lazily to enable looking up containers by member destinations.
     */
    private val tabMemberToContainer: Map<KClass<out NavDestination>, KClass<out NavDestination>> by lazy {
        buildTabMemberMapping()
    }

    /**
     * Mapping from pane member destinations to their container class.
     */
    private val paneMemberToContainer: Map<KClass<out NavDestination>, KClass<out NavDestination>> by lazy {
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
    override fun getContainerInfo(destination: NavDestination): ContainerInfo? {
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
        containerClass: KClass<out NavDestination>
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
        containerClass: KClass<out NavDestination>,
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
            scopeKey = builder.scopeKey,
            containerClass = containerClass
        )
    }

    /**
     * Creates PaneContainer info with the builder function.
     */
    private fun createPaneContainerInfo(
        builder: ContainerBuilder.Panes,
        containerClass: KClass<out NavDestination>
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
            scopeKey = builder.scopeKey,
            containerClass = containerClass
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
        destClass: KClass<out NavDestination>
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
        containerClass: KClass<out NavDestination>,
        destClass: KClass<out NavDestination>
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
    private fun buildTabMemberMapping(): Map<KClass<out NavDestination>, KClass<out NavDestination>> {
        val result = mutableMapOf<KClass<out NavDestination>, KClass<out NavDestination>>()

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
                            addReferencedContainerMembers(
                                entry.containerClass,
                                containerClass,
                                result
                            )
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
        referencedClass: KClass<out NavDestination>,
        tabContainerClass: KClass<out NavDestination>,
        result: MutableMap<KClass<out NavDestination>, KClass<out NavDestination>>
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
                            addReferencedContainerMembers(
                                entry.containerClass,
                                tabContainerClass,
                                result
                            )
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
    private fun buildPaneMemberMapping(): Map<KClass<out NavDestination>, KClass<out NavDestination>> {
        val result = mutableMapOf<KClass<out NavDestination>, KClass<out NavDestination>>()

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

    // ================================
    // Wrapper Rendering
    // ================================

    /**
     * Renders the tabs container wrapper for the given tab node.
     *
     * If a custom wrapper is registered for [tabNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the content is rendered
     * directly without any wrapper.
     *
     * @param tabNodeKey Unique identifier for the tab node
     * @param scope Scope providing tab state and navigation actions
     * @param content Composable lambda that renders the actual tab content
     */
    @Composable
    override fun TabsContainer(
        tabNodeKey: String,
        scope: TabsContainerScope,
        content: @Composable () -> Unit
    ) {
        val wrapper = tabsContainers[tabNodeKey]
        if (wrapper != null) {
            wrapper.invoke(scope, content)
        } else {
            // Default: render content directly without wrapper
            content()
        }
    }

    /**
     * Renders the pane container for the given pane node.
     *
     * If a custom container is registered for [paneNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the content is rendered
     * directly without any container.
     *
     * @param paneNodeKey Unique identifier for the pane node
     * @param scope Scope providing pane state and layout information
     * @param content Composable lambda that renders the pane content
     */
    @Composable
    override fun PaneContainer(
        paneNodeKey: String,
        scope: PaneContainerScope,
        content: @Composable () -> Unit
    ) {
        val wrapper = paneContainers[paneNodeKey]
        if (wrapper != null) {
            wrapper.invoke(scope, content)
        } else {
            // Default: render content directly without container
            content()
        }
    }

    /**
     * Checks whether a custom tabs container wrapper is registered for the given key.
     *
     * @param tabNodeKey Unique identifier for the tab node
     * @return `true` if a custom wrapper is registered, `false` if default will be used
     */
    override fun hasTabsContainer(tabNodeKey: String): Boolean {
        return tabsContainers.containsKey(tabNodeKey)
    }

    /**
     * Checks whether a custom pane container is registered for the given key.
     *
     * @param paneNodeKey Unique identifier for the pane node
     * @return `true` if a custom container is registered, `false` if default will be used
     */
    override fun hasPaneContainer(paneNodeKey: String): Boolean {
        return paneContainers.containsKey(paneNodeKey)
    }
}
