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
 *                 scope.tabs.forEachIndexed { index, tab ->
 *                     val (label, icon) = when (tab) {
 *                         is HomeTab -> "Home" to Icons.Default.Home
 *                         is ExploreTab -> "Explore" to Icons.Default.Explore
 *                         is ProfileTab -> "Profile" to Icons.Default.Person
 *                         else -> "Tab" to Icons.Default.Circle
 *                     }
 *                     NavigationBarItem(
 *                         selected = activeTabIndex == index,
 *                         onClick = { switchTab(index) },
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
     * The currently active tab index (0-based).
     *
     * Use this to highlight the selected tab in your UI.
     */
    val activeTabIndex: Int

    /**
     * Total number of tabs in this container.
     */
    val tabCount: Int

    /**
     * List of destination instances for all tabs in order.
     *
     * Use this with pattern matching to customize tab UI:
     * ```kotlin
     * tabs.forEachIndexed { index, tab ->
     *     val (label, icon) = when (tab) {
     *         is HomeTab -> "Home" to Icons.Default.Home
     *         is ExploreTab -> "Explore" to Icons.Default.Explore
     *         else -> "Tab" to Icons.Default.Circle
     *     }
     *     // Use label and icon in your tab bar
     * }
     * ```
     */
    val tabs: List<NavDestination>

    /**
     * Whether tab switching animation is currently in progress.
     *
     * Can be used to disable user interaction during transitions.
     */
    val isTransitioning: Boolean

    /**
     * Switch to the tab at the given index.
     *
     * This triggers the tab change through the navigation system,
     * handling any necessary state updates and animations.
     *
     * @param index The 0-based index of the tab to switch to
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun switchTab(index: Int)

}

/**
 * Internal implementation of [TabsContainerScope].
 *
 * Created when processing TabNode to provide the wrapper composable
 * with access to tab navigation state and actions.
 *
 * @property navigator The navigator instance for this tab container
 * @property activeTabIndex The currently selected tab index (0-based)
 * @property tabCount Total number of tabs
 * @property tabs List of destination instances for all tabs in order
 * @property isTransitioning Whether a tab transition is in progress
 * @property onSwitchTab Callback invoked when user switches tabs
 */
internal class TabsContainerScopeImpl(
    override val navigator: Navigator,
    override val activeTabIndex: Int,
    override val tabCount: Int,
    override val tabs: List<NavDestination>,
    override val isTransitioning: Boolean,
    private val onSwitchTab: (Int) -> Unit
) : TabsContainerScope {

    override fun switchTab(index: Int) {
        require(index in 0 until tabCount) {
            "Tab index $index out of bounds [0, $tabCount)"
        }
        onSwitchTab(index)
    }

}

/**
 * Creates a [TabsContainerScopeImpl] with the given parameters.
 *
 * Factory function for creating tabs container scopes.
 *
 * @param navigator The navigator instance
 * @param activeTabIndex Currently selected tab index
 * @param tabs List of destination instances for all tabs
 * @param isTransitioning Whether transitioning between tabs
 * @param onSwitchTab Callback for tab switching
 * @return A new [TabsContainerScope] implementation
 */
internal fun createTabsContainerScope(
    navigator: Navigator,
    activeTabIndex: Int,
    tabs: List<NavDestination>,
    isTransitioning: Boolean,
    onSwitchTab: (Int) -> Unit
): TabsContainerScope = TabsContainerScopeImpl(
    navigator = navigator,
    activeTabIndex = activeTabIndex,
    tabCount = tabs.size,
    tabs = tabs,
    isTransitioning = isTransitioning,
    onSwitchTab = onSwitchTab
)
