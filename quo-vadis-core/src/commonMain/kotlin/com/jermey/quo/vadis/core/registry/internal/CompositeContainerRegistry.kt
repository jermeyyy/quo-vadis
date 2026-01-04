package com.jermey.quo.vadis.core.registry.internal

import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.registry.ContainerInfo
import kotlin.reflect.KClass

/**
 * Composite container registry that delegates to secondary first, then primary.
 * Handles both container building (getContainerInfo) and wrapper rendering
 * (TabsContainer, PaneContainer).
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Composite registries are managed internally by the navigation system.
 *
 * ## NavNode Builder Resolution
 *
 * When combining navigation configs, each config's container registry captures
 * its own `buildNavNode` function. This causes issues when a container in one
 * config references destinations from another config - the original builder
 * won't find them.
 *
 * This class solves this by accepting a composite `navNodeBuilder` function
 * that can resolve destinations across all combined configs. When returning
 * [ContainerInfo], the builders are wrapped to use the composite's builder
 * instead of the original config's builder.
 *
 * @param primary The base registry (lower priority)
 * @param secondary The overlay registry (higher priority)
 * @param navNodeBuilder Function to build NavNodes from destination classes,
 *   using the composite config's resolution logic
 */
@InternalQuoVadisApi
internal class CompositeContainerRegistry(
    private val primary: ContainerRegistry,
    private val secondary: ContainerRegistry,
    private val navNodeBuilder: (KClass<out NavDestination>, String?, String?) -> NavNode?
) : ContainerRegistry {

    /**
     * Gets container info for a destination, wrapping the builder to use
     * the composite's navNodeBuilder.
     *
     * This ensures that when the builder creates child nodes (e.g., for
     * TabEntry.ContainerReference), it can find destinations from any
     * of the combined configs, not just the one where the container
     * was originally registered.
     */
    override fun getContainerInfo(destination: NavDestination): ContainerInfo? {
        val info = secondary.getContainerInfo(destination)
            ?: primary.getContainerInfo(destination)
            ?: return null

        // Wrap the ContainerInfo to use the composite's navNodeBuilder
        return wrapContainerInfo(info)
    }

    /**
     * Wraps ContainerInfo builders to use the composite's navNodeBuilder.
     *
     * The original builders capture their config's buildNavNode function,
     * which only sees that config's registrations. We create new builders
     * that use the composite's navNodeBuilder, enabling cross-config
     * destination resolution.
     *
     * This ensures that:
     * - TabEntry.ContainerReference entries that reference containers from other configs work
     * - Nested container structures spanning multiple configs are resolved correctly
     */
    private fun wrapContainerInfo(info: ContainerInfo): ContainerInfo {
        return when (info) {
            is ContainerInfo.TabContainer -> ContainerInfo.TabContainer(
                builder = { key, parentKey, initialTabIndex ->
                    // Use the composite's navNodeBuilder to build the node,
                    // ensuring cross-config resolution
                    val node = navNodeBuilder(info.containerClass, key, parentKey)
                        ?: throw IllegalStateException(
                            "Failed to build TabNode for ${info.containerClass}"
                        )

                    if (node !is TabNode) {
                        throw IllegalStateException("Expected TabNode but got ${node::class}")
                    }

                    // Apply the requested initial tab index
                    if (initialTabIndex != node.activeStackIndex) {
                        node.copy(activeStackIndex = initialTabIndex.coerceIn(0, node.stacks.size - 1))
                    } else {
                        node
                    }
                },
                initialTabIndex = info.initialTabIndex,
                scopeKey = info.scopeKey,
                containerClass = info.containerClass
            )
            is ContainerInfo.PaneContainer -> ContainerInfo.PaneContainer(
                builder = { key, parentKey ->
                    // Use the composite's navNodeBuilder to build the node
                    val node = navNodeBuilder(info.containerClass, key, parentKey)
                        ?: throw IllegalStateException(
                            "Failed to build PaneNode for ${info.containerClass}"
                        )

                    if (node !is PaneNode) {
                        throw IllegalStateException("Expected PaneNode but got ${node::class}")
                    }

                    node
                },
                initialPane = info.initialPane,
                scopeKey = info.scopeKey,
                containerClass = info.containerClass
            )
        }
    }

    @Composable
    override fun TabsContainer(
        tabNodeKey: String,
        scope: TabsContainerScope,
        content: @Composable () -> Unit
    ) {
        // Check secondary first (higher priority), then primary
        when {
            secondary.hasTabsContainer(tabNodeKey) ->
                secondary.TabsContainer(tabNodeKey, scope, content)
            primary.hasTabsContainer(tabNodeKey) ->
                primary.TabsContainer(tabNodeKey, scope, content)
            // If neither has it, let primary handle (will use default or throw)
            else -> primary.TabsContainer(tabNodeKey, scope, content)
        }
    }

    @Composable
    override fun PaneContainer(
        paneNodeKey: String,
        scope: PaneContainerScope,
        content: @Composable () -> Unit
    ) {
        // Check secondary first (higher priority), then primary
        when {
            secondary.hasPaneContainer(paneNodeKey) ->
                secondary.PaneContainer(paneNodeKey, scope, content)
            primary.hasPaneContainer(paneNodeKey) ->
                primary.PaneContainer(paneNodeKey, scope, content)
            // If neither has it, let primary handle (will use default or throw)
            else -> primary.PaneContainer(paneNodeKey, scope, content)
        }
    }

    override fun hasTabsContainer(tabNodeKey: String): Boolean {
        return secondary.hasTabsContainer(tabNodeKey) || primary.hasTabsContainer(tabNodeKey)
    }

    override fun hasPaneContainer(paneNodeKey: String): Boolean {
        return secondary.hasPaneContainer(paneNodeKey) || primary.hasPaneContainer(paneNodeKey)
    }
}