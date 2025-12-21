package com.jermey.quo.vadis.core.navigation.compose.registry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabsContainerScope
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.TabNode
import kotlin.reflect.KClass

/**
 * Information about a container and how to create it.
 *
 * When navigating to a destination that belongs to a `@Tabs` or `@Pane`
 * container, this sealed class provides the builder function and initial
 * state needed to create the appropriate container node structure.
 */
sealed class ContainerInfo {

    /**
     * The scope key for this container, typically the sealed class simple name.
     * Used for scope-aware navigation to determine if we're already inside
     * the required container.
     */
    abstract val scopeKey: String

    /**
     * The destination class that defines this container.
     * Used for rebuilding the container with a different navNodeBuilder
     * when combining navigation configs.
     */
    abstract val containerClass: KClass<out NavDestination>

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
     * @property containerClass The destination class that defines this container.
     */
    data class TabContainer(
        val builder: (key: String, parentKey: String?, initialTabIndex: Int) -> TabNode,
        val initialTabIndex: Int,
        override val scopeKey: String,
        override val containerClass: KClass<out NavDestination>
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
     * @property containerClass The destination class that defines this container.
     */
    data class PaneContainer(
        val builder: (key: String, parentKey: String?) -> PaneNode,
        val initialPane: PaneRole,
        override val scopeKey: String,
        override val containerClass: KClass<out NavDestination>
    ) : ContainerInfo()
}

/**
 * Registry that maps destinations to their container structures and provides
 * custom wrapper composables for tab and pane containers.
 *
 * This interface combines two responsibilities:
 * 1. **Building containers** - Creating TabNode/PaneNode structures for destinations
 * 2. **Rendering wrappers** - Custom UI wrappers like tab bars and pane layouts
 *
 * ## Purpose
 *
 * The ContainerRegistry enables:
 * - Automatic container creation during navigation (for `@Tabs` and `@Pane` annotated destinations)
 * - Custom chrome/UI surrounding navigation content (tab bars, bottom navigation, multi-pane layouts)
 *
 * ## Example: Container Building
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
 * ## Example: Wrapper Rendering
 *
 * ```kotlin
 * containerRegistry.TabsContainer(
 *     tabNodeKey = tabNode.key,
 *     scope = tabsContainerScope
 * ) {
 *     // Tab content rendered here
 *     AnimatedNavContent(activeTab) { tab ->
 *         NavTreeRenderer(tab, scope)
 *     }
 * }
 * ```
 *
 * ## KSP Generation
 *
 * The KSP processor scans for annotated destinations and functions:
 * - `@Tabs` and `@Pane` annotations create container builders
 * - `@TabsContainer` and `@PaneContainer` annotations create wrapper composables
 *
 * ## Thread Safety
 *
 * Implementations are marked [Stable], indicating they can be safely read
 * during composition without triggering unnecessary recompositions.
 *
 * @see ContainerInfo for the container information structure
 * @see com.jermey.quo.vadis.core.navigation.core.TabNode for tab container structure
 * @see com.jermey.quo.vadis.core.navigation.core.PaneNode for pane container structure
 * @see ScopeRegistry for scope-aware navigation within containers
 * @see TabsContainerScope
 * @see PaneContainerScope
 */
@Stable
interface ContainerRegistry {

    // ================================
    // Container Building
    // ================================

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
    fun getContainerInfo(destination: NavDestination): ContainerInfo?

    // ================================
    // Wrapper Rendering
    // ================================

    /**
     * Renders the tabs container wrapper for the given tab node.
     *
     * If a custom wrapper is registered for [tabNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the default wrapper
     * is used, which simply renders [content] directly.
     *
     * ## Wrapper Contract
     *
     * Wrappers **must** invoke [content] exactly once to render the tab content.
     * The wrapper is responsible for:
     * - Rendering navigation UI (tab bar, bottom navigation, etc.)
     * - Positioning the content appropriately
     * - Forwarding any necessary modifiers or padding
     *
     * ## Example
     *
     * ```kotlin
     * containerRegistry.TabsContainer(
     *     tabNodeKey = tabNode.key,
     *     scope = tabsContainerScope
     * ) {
     *     // Tab content rendered here
     *     AnimatedNavContent(activeTab) { tab ->
     *         NavTreeRenderer(tab, scope)
     *     }
     * }
     * ```
     *
     * @param tabNodeKey Unique identifier for the tab node (typically destination class name)
     * @param scope Scope providing tab state and navigation actions
     * @param content Composable lambda that renders the actual tab content
     */
    @Composable
    fun TabsContainer(
        tabNodeKey: String,
        scope: TabsContainerScope,
        content: @Composable () -> Unit
    )

    /**
     * Renders the pane container for the given pane node.
     *
     * If a custom container is registered for [paneNodeKey], it is invoked with
     * the provided [scope] and [content]. Otherwise, the default container
     * is used, which implements a basic split-pane layout.
     *
     * ## Container Contract
     *
     * Containers **must** invoke [content] exactly once to render the pane content.
     * The container is responsible for:
     * - Determining pane layout based on screen size
     * - Positioning multiple panes (in expanded mode)
     * - Handling pane visibility and transitions
     *
     * ## Example
     *
     * ```kotlin
     * containerRegistry.PaneContainer(
     *     paneNodeKey = paneNode.key,
     *     scope = paneContainerScope
     * ) {
     *     // Pane content rendered here
     *     Row {
     *         PrimaryPane()
     *         if (scope.isExpanded) {
     *             DetailPane()
     *         }
     *     }
     * }
     * ```
     *
     * @param paneNodeKey Unique identifier for the pane node (typically destination class name)
     * @param scope Scope providing pane state and layout information
     * @param content Composable lambda that renders the pane content
     */
    @Composable
    fun PaneContainer(
        paneNodeKey: String,
        scope: PaneContainerScope,
        content: @Composable () -> Unit
    )

    /**
     * Checks whether a custom tabs container wrapper is registered for the given key.
     *
     * This method allows renderers to optimize by skipping wrapper invocation
     * when no custom wrapper exists, potentially reducing composition overhead.
     *
     * @param tabNodeKey Unique identifier for the tab node
     * @return `true` if a custom wrapper is registered, `false` if default will be used
     */
    fun hasTabsContainer(tabNodeKey: String): Boolean

    /**
     * Checks whether a custom pane container is registered for the given key.
     *
     * This method allows renderers to optimize by skipping container invocation
     * when no custom container exists, potentially reducing composition overhead.
     *
     * @param paneNodeKey Unique identifier for the pane node
     * @return `true` if a custom container is registered, `false` if default will be used
     */
    fun hasPaneContainer(paneNodeKey: String): Boolean

    /**
     * Default implementations and factory methods for [ContainerRegistry].
     */
    companion object {
        /**
         * Empty registry that never creates containers and provides default wrapper behavior.
         *
         * ## Default Behavior
         *
         * - [getContainerInfo]: Always returns `null`
         * - [TabsContainer]: Renders [content] directly without any wrapper UI
         * - [PaneContainer]: Renders [content] directly without any container UI
         * - [hasTabsContainer]: Always returns `false`
         * - [hasPaneContainer]: Always returns `false`
         *
         * ## Usage
         *
         * Use this as a default when container-aware navigation is not needed,
         * or for backward compatibility with existing navigation code:
         *
         * ```kotlin
         * val scope = NavRenderScopeImpl(
         *     // ...
         *     containerRegistry = ContainerRegistry.Empty
         * )
         * ```
         */
        val Empty: ContainerRegistry = object : ContainerRegistry {
            override fun getContainerInfo(destination: NavDestination): ContainerInfo? = null

            @Composable
            override fun TabsContainer(
                tabNodeKey: String,
                scope: TabsContainerScope,
                content: @Composable () -> Unit
            ) {
                // Default: render content directly without wrapper
                content()
            }

            @Composable
            override fun PaneContainer(
                paneNodeKey: String,
                scope: PaneContainerScope,
                content: @Composable () -> Unit
            ) {
                // Default: render content directly without container
                content()
            }

            override fun hasTabsContainer(tabNodeKey: String): Boolean = false

            override fun hasPaneContainer(paneNodeKey: String): Boolean = false
        }
    }
}
