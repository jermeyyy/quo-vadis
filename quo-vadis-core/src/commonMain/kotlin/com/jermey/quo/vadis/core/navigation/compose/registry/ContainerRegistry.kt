package com.jermey.quo.vadis.core.navigation.compose.registry

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.TabNode

/**
 * Information about a container and how to create it.
 *
 * When navigating to a destination that belongs to a `@Tabs` or `@Pane`
 * container, this sealed class provides the builder function and initial
 * state needed to create the appropriate container node structure.
 */
public sealed class ContainerInfo {

    /**
     * The scope key for this container, typically the sealed class simple name.
     * Used for scope-aware navigation to determine if we're already inside
     * the required container.
     */
    public abstract val scopeKey: String

    /**
     * Tab container information.
     *
     * Contains everything needed to create a [TabNode] for a tabs-based
     * navigation container. The [builder] function creates the complete
     * tab structure with all stacks initialized.
     *
     * @property builder Function to build the [TabNode]. Accepts:
     *   - `key`: Unique identifier for the container
     *   - `parentKey`: Key of the parent node (nullable)
     *   - `initialTabIndex`: Which tab should be initially active
     * @property initialTabIndex Which tab should be active based on the destination
     *   being navigated to. Index is 0-based.
     * @property scopeKey The scope key for this container, typically the sealed
     *   class simple name (e.g., "MainTabs"). Used for scope-aware navigation.
     */
    public data class TabContainer(
        val builder: (key: String, parentKey: String?, initialTabIndex: Int) -> TabNode,
        val initialTabIndex: Int,
        override val scopeKey: String
    ) : ContainerInfo()

    /**
     * Pane container information.
     *
     * Contains everything needed to create a [PaneNode] for an adaptive
     * pane-based navigation container. The [builder] function creates the
     * complete pane structure with all configured panes.
     *
     * @property builder Function to build the [PaneNode]. Accepts:
     *   - `key`: Unique identifier for the container
     *   - `parentKey`: Key of the parent node (nullable)
     * @property initialPane Which pane should be initially active
     * @property scopeKey The scope key for this container, typically the sealed
     *   class simple name. Used for scope-aware navigation.
     */
    public data class PaneContainer(
        val builder: (key: String, parentKey: String?) -> PaneNode,
        val initialPane: PaneRole,
        override val scopeKey: String
    ) : ContainerInfo()
}

/**
 * Registry that maps destinations to their container structures.
 *
 * When navigating to a destination that belongs to a `@Tabs` or `@Pane`
 * container, this registry provides the builder function to create
 * the appropriate container node structure.
 *
 * ## Purpose
 *
 * The ContainerRegistry enables automatic container creation during navigation.
 * Instead of manually creating TabNode or PaneNode structures, the navigator
 * can query this registry and receive the appropriate builder for destinations
 * that require container wrapping.
 *
 * ## Example
 *
 * ```kotlin
 * // When navigating to a tab destination
 * val containerInfo = registry.getContainerInfo(DemoTabs.MusicTab.List)
 * if (containerInfo is ContainerInfo.TabContainer) {
 *     val tabNode = containerInfo.builder(key, parentKey, containerInfo.initialTabIndex)
 *     // Push tabNode onto the navigation stack
 * }
 * ```
 *
 * ## Implementation
 *
 * KSP-generated code registers container builders automatically based on
 * `@Tabs` and `@Pane` annotations. The generated registry maps each
 * destination within a container to its builder function.
 *
 * @see ContainerInfo for the container information structure
 * @see com.jermey.quo.vadis.core.navigation.core.TabNode for tab container structure
 * @see com.jermey.quo.vadis.core.navigation.core.PaneNode for pane container structure
 * @see ScopeRegistry for scope-aware navigation within containers
 */
public interface ContainerRegistry {

    /**
     * Check if a destination requires a container structure.
     *
     * Returns container information if the destination belongs to a `@Tabs`
     * or `@Pane` annotated container, allowing the navigator to create the
     * appropriate container node.
     *
     * @param destination The destination being navigated to
     * @return [ContainerInfo] if destination needs a container, null otherwise
     */
    public fun getContainerInfo(destination: Destination): ContainerInfo?

    /**
     * Default implementations and factory methods for [ContainerRegistry].
     */
    public companion object {
        /**
         * Empty registry that never creates containers.
         *
         * Use this as a default when container-aware navigation is not needed,
         * or for backward compatibility with existing navigation code.
         */
        public val Empty: ContainerRegistry = object : ContainerRegistry {
            override fun getContainerInfo(destination: Destination): ContainerInfo? = null
        }
    }
}
