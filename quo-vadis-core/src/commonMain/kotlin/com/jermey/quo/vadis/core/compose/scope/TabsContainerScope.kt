package com.jermey.quo.vadis.core.compose.scope

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.navigator.Navigator

/**
 * Scope interface for tabs container wrapper composables.
 *
 * This scope provides access to the tab navigation state and actions,
 * allowing user-defined wrappers to render custom tab UI while
 * delegating content rendering to the library.
 *
 * ## Usage
 *
 * The scope is receiver for [TabsContainer] composables. Tab UI customization
 * (labels, icons) is done using type-safe pattern matching on the [tabs] property:
 *
 * ```kotlin
 * @TabsContainer(MainTabs::class)
 * @Composable
 * fun MainTabsContainer(scope: TabsContainerScope, content: @Composable () -> Unit) {
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 scope.tabs.forEach { tab ->
 *                     val (label, icon) = when (tab) {
 *                         is HomeTab -> "Home" to Icons.Default.Home
 *                         is ExploreTab -> "Explore" to Icons.Default.Explore
 *                         is ProfileTab -> "Profile" to Icons.Default.Person
 *                         else -> "Tab" to Icons.Default.Circle
 *                     }
 *                     NavigationBarItem(
 *                         selected = activeTab == tab,
 *                         onClick = { switchTab(tab) },
 *                         icon = { Icon(icon, contentDescription = label) },
 *                         label = { Text(label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(modifier = Modifier.padding(padding)) {
 *             content()
 *         }
 *     }
 * }
 * ```
 *
 * @see TabsContainer
 */
@Stable
interface TabsContainerScope {

    /**
     * The navigator instance for this tab container.
     *
     * Can be used for programmatic navigation or accessing
     * navigation state beyond tab switching.
     */
    val navigator: Navigator

    /**
     * The currently active tab destination.
     *
     * Use this to highlight the selected tab in your UI.
     */
    val activeTab: NavDestination

    /**
     * Set of destination instances for all tabs.
     *
     * Use this with pattern matching to customize tab UI:
     * ```kotlin
     * tabs.forEach { tab ->
     *     val (label, icon) = when (tab) {
     *         is HomeTab -> "Home" to Icons.Default.Home
     *         is ExploreTab -> "Explore" to Icons.Default.Explore
     *         else -> "Tab" to Icons.Default.Circle
     *     }
     *     // Use label and icon in your tab bar
     * }
     * ```
     */
    val tabs: Set<NavDestination>

    /**
     * Whether tab switching animation is currently in progress.
     *
     * Can be used to disable user interaction during transitions.
     */
    val isTransitioning: Boolean

    /**
     * Switch to the tab with the given destination.
     *
     * This triggers the tab change through the navigation system,
     * handling any necessary state updates and animations.
     *
     * @param destination The destination of the tab to switch to
     * @throws IllegalArgumentException if destination is not a tab in this container
     */
    fun switchTab(destination: NavDestination)

}

/**
 * Internal implementation of [TabsContainerScope].
 *
 * Created when processing TabNode to provide the wrapper composable
 * with access to tab navigation state and actions.
 *
 * @property navigator The navigator instance for this tab container
 * @property activeTab The currently active tab destination
 * @property tabs Set of destination instances for all tabs
 * @property isTransitioning Whether a tab transition is in progress
 * @property onSwitchTab Callback invoked when user switches tabs
 */
internal class TabsContainerScopeImpl(
    override val navigator: Navigator,
    override val activeTab: NavDestination,
    override val tabs: Set<NavDestination>,
    override val isTransitioning: Boolean,
    private val onSwitchTab: (NavDestination) -> Unit
) : TabsContainerScope {

    override fun switchTab(destination: NavDestination) {
        require(destination in tabs) {
            "Destination $destination is not a tab in this container"
        }
        onSwitchTab(destination)
    }

}

/**
 * Creates a [TabsContainerScopeImpl] with the given parameters.
 *
 * Factory function for creating tabs container scopes.
 *
 * @param navigator The navigator instance
 * @param activeTab The currently active tab destination
 * @param tabs Set of destination instances for all tabs
 * @param isTransitioning Whether transitioning between tabs
 * @param onSwitchTab Callback for tab switching
 * @return A new [TabsContainerScope] implementation
 */
internal fun createTabsContainerScope(
    navigator: Navigator,
    activeTab: NavDestination,
    tabs: Set<NavDestination>,
    isTransitioning: Boolean,
    onSwitchTab: (NavDestination) -> Unit
): TabsContainerScope = TabsContainerScopeImpl(
    navigator = navigator,
    activeTab = activeTab,
    tabs = tabs,
    isTransitioning = isTransitioning,
    onSwitchTab = onSwitchTab
)
