package com.jermey.quo.vadis.core.navigation.compose.wrapper.internal

import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContent
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabMetadata
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneRole

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

    override fun switchTab(route: String) {
        val index = tabMetadata.indexOfFirst { it.route == route }
        require(index >= 0) {
            "No tab found with route: $route. Available routes: ${tabMetadata.map { it.route }}"
        }
        switchTab(index)
    }
}

/**
 * Internal implementation of [PaneWrapperScope].
 *
 * Created when processing PaneNode to provide the wrapper composable
 * with access to pane layout state and actions.
 *
 * @property navigator The navigator instance for this pane container
 * @property activePaneRole The currently active pane role
 * @property paneCount Total number of configured panes
 * @property visiblePaneCount Number of currently visible panes
 * @property isExpanded Whether in expanded (multi-pane) mode
 * @property isTransitioning Whether a pane transition is in progress
 * @property onNavigateToPane Callback invoked when navigating to a pane
 */
internal class PaneWrapperScopeImpl(
    override val navigator: Navigator,
    override val activePaneRole: PaneRole,
    override val paneCount: Int,
    override val visiblePaneCount: Int,
    override val isExpanded: Boolean,
    override val isTransitioning: Boolean,
    private val onNavigateToPane: (PaneRole) -> Unit
) : PaneWrapperScope {

    override fun navigateToPane(role: PaneRole) {
        onNavigateToPane(role)
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

/**
 * Creates a [PaneWrapperScopeImpl] with the given parameters.
 *
 * Factory function for creating pane wrapper scopes.
 *
 * @param navigator The navigator instance
 * @param activePaneRole Currently active pane role
 * @param paneContents List of pane contents with visibility
 * @param isExpanded Whether in expanded mode
 * @param isTransitioning Whether transitioning between panes
 * @param onNavigateToPane Callback for pane navigation
 * @return A new [PaneWrapperScope] implementation
 */
internal fun createPaneWrapperScope(
    navigator: Navigator,
    activePaneRole: PaneRole,
    paneContents: List<PaneContent>,
    isExpanded: Boolean,
    isTransitioning: Boolean,
    onNavigateToPane: (PaneRole) -> Unit
): PaneWrapperScope = PaneWrapperScopeImpl(
    navigator = navigator,
    activePaneRole = activePaneRole,
    paneCount = paneContents.size,
    visiblePaneCount = paneContents.count { it.isVisible },
    isExpanded = isExpanded,
    isTransitioning = isTransitioning,
    onNavigateToPane = onNavigateToPane
)
