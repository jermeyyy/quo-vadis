package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Metadata for a single tab in a TabNode.
 *
 * This class provides information about a tab that can be used
 * to render navigation UI elements like bottom navigation items
 * or tab bar items.
 *
 * @property label Human-readable label for the tab
 * @property icon Optional icon for the tab
 * @property route The route identifier for this tab
 * @property contentDescription Accessibility content description
 * @property badge Optional badge content (e.g., notification count)
 */
public data class TabMetadata(
    val label: String,
    val icon: ImageVector? = null,
    val route: String,
    val contentDescription: String? = null,
    val badge: String? = null
)

/**
 * Scope interface for tab wrapper composables.
 *
 * This scope provides access to the tab navigation state and actions,
 * allowing user-defined wrappers to render custom tab UI while
 * delegating content rendering to the library.
 *
 * ## Usage
 *
 * The scope is receiver for [TabWrapper] composables:
 *
 * ```kotlin
 * val myTabWrapper: TabWrapper = { tabContent ->
 *     Scaffold(
 *         bottomBar = {
 *             NavigationBar {
 *                 tabMetadata.forEachIndexed { index, meta ->
 *                     NavigationBarItem(
 *                         selected = activeTabIndex == index,
 *                         onClick = { switchTab(index) },
 *                         icon = { Icon(meta.icon, meta.label) },
 *                         label = { Text(meta.label) }
 *                     )
 *                 }
 *             }
 *         }
 *     ) { padding ->
 *         Box(modifier = Modifier.padding(padding)) {
 *             tabContent()
 *         }
 *     }
 * }
 * ```
 *
 * @see TabWrapper
 * @see TabMetadata
 */
@Stable
public interface TabWrapperScope {

    /**
     * The navigator instance for this tab container.
     *
     * Can be used for programmatic navigation or accessing
     * navigation state beyond tab switching.
     */
    public val navigator: Navigator

    /**
     * The currently active tab index (0-based).
     *
     * Use this to highlight the selected tab in your UI.
     */
    public val activeTabIndex: Int

    /**
     * Total number of tabs in this container.
     */
    public val tabCount: Int

    /**
     * Metadata for all tabs in order.
     *
     * Use this to render tab items with their labels, icons, and routes.
     */
    public val tabMetadata: List<TabMetadata>

    /**
     * Whether tab switching animation is currently in progress.
     *
     * Can be used to disable user interaction during transitions.
     */
    public val isTransitioning: Boolean

    /**
     * Switch to the tab at the given index.
     *
     * This triggers the tab change through the navigation system,
     * handling any necessary state updates and animations.
     *
     * @param index The 0-based index of the tab to switch to
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public fun switchTab(index: Int)

}

/**
 * Internal implementation of [TabWrapperScope].
 *
 * Created when processing TabNode to provide the wrapper composable
 * with access to tab navigation state and actions.
 *
 * @property navigator The navigator instance for this tab container
 * @property activeTabIndex The currently selected tab index (0-based)
 * @property tabCount Total number of tabs
 * @property tabMetadata Metadata for all tabs in order
 * @property isTransitioning Whether a tab transition is in progress
 * @property onSwitchTab Callback invoked when user switches tabs
 */
internal class TabWrapperScopeImpl(
    override val navigator: Navigator,
    override val activeTabIndex: Int,
    override val tabCount: Int,
    override val tabMetadata: List<TabMetadata>,
    override val isTransitioning: Boolean,
    private val onSwitchTab: (Int) -> Unit
) : TabWrapperScope {

    override fun switchTab(index: Int) {
        require(index in 0 until tabCount) {
            "Tab index $index out of bounds [0, $tabCount)"
        }
        onSwitchTab(index)
    }

}

/**
 * Creates a [TabWrapperScopeImpl] with the given parameters.
 *
 * Factory function for creating tab wrapper scopes.
 *
 * @param navigator The navigator instance
 * @param activeTabIndex Currently selected tab index
 * @param tabMetadata Metadata for all tabs
 * @param isTransitioning Whether transitioning between tabs
 * @param onSwitchTab Callback for tab switching
 * @return A new [TabWrapperScope] implementation
 */
internal fun createTabWrapperScope(
    navigator: Navigator,
    activeTabIndex: Int,
    tabMetadata: List<TabMetadata>,
    isTransitioning: Boolean,
    onSwitchTab: (Int) -> Unit
): TabWrapperScope = TabWrapperScopeImpl(
    navigator = navigator,
    activeTabIndex = activeTabIndex,
    tabCount = tabMetadata.size,
    tabMetadata = tabMetadata,
    isTransitioning = isTransitioning,
    onSwitchTab = onSwitchTab
)
