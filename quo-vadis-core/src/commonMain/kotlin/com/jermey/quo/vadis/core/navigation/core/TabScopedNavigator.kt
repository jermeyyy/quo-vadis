package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Navigator wrapper that synchronizes with a specific tab's state.
 *
 * This internal implementation detail ensures that:
 * - Navigation operations within a tab update the tab's stack
 * - The tab state remains synchronized with the navigator
 * - Each tab maintains its own independent navigation history
 *
 * All navigation operations are transparently delegated to the underlying
 * navigator while keeping the [TabNavigatorState] informed of changes.
 *
 * **NOTE**: This class is being refactored to use tree-based navigation.
 * Many methods are currently stubbed with [NotImplementedError] and will
 * be properly implemented in Phase 2 (Renderer).
 *
 * @param tab The tab definition this navigator is scoped to.
 * @param tabState The parent tab navigation state.
 * @param delegate The actual navigator instance to delegate operations to.
 */
@Suppress("TooManyFunctions", "NotImplementedDeclaration")
internal class TabScopedNavigator(
    private val tab: TabDefinition,
    private val tabState: TabNavigatorState,
    private val delegate: Navigator
) : Navigator {

    // Tab navigators maintain their own graph registry separate from parent
    private val graphs = mutableMapOf<String, NavigationGraph>()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // =========================================================================
    // TREE-BASED STATE (New API)
    // =========================================================================

    private val _state = MutableStateFlow<NavNode>(
        ScreenNode(
            key = NavKeyGenerator.generate(),
            parentKey = null,
            destination = tab.rootDestination
        )
    )
    override val state: StateFlow<NavNode> = _state.asStateFlow()

    private val _transitionState = MutableStateFlow<TransitionState>(TransitionState.Idle)
    override val transitionState: StateFlow<TransitionState> = _transitionState.asStateFlow()

    private val _canNavigateBack = MutableStateFlow(false)
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    private val _currentDestination = MutableStateFlow<Destination?>(tab.rootDestination)
    override val currentDestination: StateFlow<Destination?> = _currentDestination.asStateFlow()

    private val _previousDestination = MutableStateFlow<Destination?>(null)
    override val previousDestination: StateFlow<Destination?> = _previousDestination.asStateFlow()

    override val currentTransition: StateFlow<NavigationTransition?>
        get() = delegate.currentTransition

    override val activeChild: BackPressHandler?
        get() = delegate.activeChild

    // =========================================================================
    // TAB NAVIGATION (Stubbed - N/A for tab-scoped navigator)
    // =========================================================================

    override fun switchTab(index: Int) {
        // TabScopedNavigator doesn't handle tab switching directly
        // This should be handled by the parent TabNavigatorState
        throw NotImplementedError("Tab switching should be handled by TabNavigatorState, not TabScopedNavigator")
    }

    override val activeTabIndex: Int?
        get() = null // Tab-scoped navigator doesn't manage tabs

    // =========================================================================
    // PANE NAVIGATION (Delegated to parent)
    // =========================================================================

    override fun navigateToPane(
        role: PaneRole,
        destination: Destination,
        switchFocus: Boolean,
        transition: NavigationTransition?
    ) {
        delegate.navigateToPane(role, destination, switchFocus, transition)
    }

    override fun switchPane(role: PaneRole) {
        delegate.switchPane(role)
    }

    override fun isPaneAvailable(role: PaneRole): Boolean {
        return delegate.isPaneAvailable(role)
    }

    override fun paneContent(role: PaneRole): NavNode? {
        return delegate.paneContent(role)
    }

    override fun navigateBackInPane(role: PaneRole): Boolean {
        return delegate.navigateBackInPane(role)
    }

    override fun clearPane(role: PaneRole) {
        delegate.clearPane(role)
    }

    // =========================================================================
    // STATE MANIPULATION
    // =========================================================================

    override fun updateState(newState: NavNode, transition: NavigationTransition?) {
        _state.value = newState
        updateDerivedState()
    }

    private fun updateDerivedState() {
        val currentState = _state.value
        val activeLeaf = currentState.activeLeaf()
        _currentDestination.value = (activeLeaf as? ScreenNode)?.destination
        
        // Update canNavigateBack based on stack depth
        val activeStack = currentState.activeStack()
        _canNavigateBack.value = activeStack != null && activeStack.children.size > 1
        
        // Update previous destination
        if (activeStack != null && activeStack.children.size > 1) {
            val previousNode = activeStack.children.getOrNull(activeStack.children.size - 2)
            _previousDestination.value = (previousNode as? ScreenNode)?.destination
        } else {
            _previousDestination.value = null
        }
    }

    // =========================================================================
    // TRANSITION CONTROL
    // =========================================================================

    override fun updateTransitionProgress(progress: Float) {
        val current = _transitionState.value
        when (current) {
            is TransitionState.InProgress -> {
                _transitionState.value = current.copy(progress = progress)
            }
            is TransitionState.PredictiveBack -> {
                _transitionState.value = current.copy(progress = progress)
            }
            else -> { /* Ignore if not in transition */ }
        }
    }

    override fun startPredictiveBack() {
        _transitionState.value = TransitionState.PredictiveBack(
            progress = 0f,
            touchX = 0f,
            touchY = 0f
        )
    }

    override fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float) {
        val current = _transitionState.value
        if (current is TransitionState.PredictiveBack) {
            _transitionState.value = current.copy(
                progress = progress,
                touchX = touchX,
                touchY = touchY
            )
        }
    }

    override fun cancelPredictiveBack() {
        _transitionState.value = TransitionState.Idle
    }

    override fun commitPredictiveBack() {
        navigateBack()
        _transitionState.value = TransitionState.Idle
    }

    override fun completeTransition() {
        _transitionState.value = TransitionState.Idle
    }

    // =========================================================================
    // NAVIGATION OPERATIONS
    // =========================================================================

    /**
     * Check if destination is a root destination for any tab.
     * If so, switch to that tab instead of navigating within current tab.
     */
    private fun isTabRootDestination(destination: Destination): TabDefinition? {
        return tabState.getAllTabs().find { it.rootDestination::class == destination::class }
    }

    /**
     * Check if destination exists in this tab's registered graphs.
     */
    private fun isDestinationInTabGraph(destination: Destination): Boolean {
        return graphs.values.any { graph ->
            graph.destinations.any { it.destination::class == destination::class }
        }
    }

    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        println("DEBUG_TAB_NAV: TabScopedNavigator.navigate - tab: ${tab.route}, destination: ${destination::class.simpleName}")

        // First check: Is this a tab root destination? If so, switch tabs
        val targetTab = isTabRootDestination(destination)
        if (targetTab != null) {
            println("DEBUG_TAB_NAV: TabScopedNavigator - Destination is tab root for ${targetTab.route}, switching tabs")
            tabState.selectTab(targetTab)
            return
        }

        // Second check: Is this destination in the tab's graph?
        if (isDestinationInTabGraph(destination)) {
            println("DEBUG_TAB_NAV: TabScopedNavigator - Destination found in tab graph, navigating within tab")
            // Push to stack using tree-based state
            val currentState = _state.value
            val activeStack = currentState.activeStack()
            if (activeStack != null) {
                val newScreen = ScreenNode(
                    key = NavKeyGenerator.generate(),
                    parentKey = activeStack.key,
                    destination = destination
                )
                val newStack = activeStack.copy(
                    children = activeStack.children + newScreen
                )
                _state.value = replaceNode(currentState, activeStack.key, newStack)
            } else {
                // Create new stack with screen
                val newScreen = ScreenNode(
                    key = NavKeyGenerator.generate(),
                    parentKey = null,
                    destination = destination
                )
                _state.value = StackNode(
                    key = NavKeyGenerator.generate(),
                    parentKey = null,
                    children = listOf(newScreen)
                )
            }
            updateDerivedState()
        } else {
            println("DEBUG_TAB_NAV: TabScopedNavigator - Destination NOT in tab graph, delegating to parent navigator")
            delegate.navigate(destination, transition)
        }
    }

    private fun replaceNode(root: NavNode, targetKey: String, newNode: NavNode): NavNode {
        if (root.key == targetKey) return newNode
        return when (root) {
            is ScreenNode -> root
            is StackNode -> root.copy(
                children = root.children.map { replaceNode(it, targetKey, newNode) }
            )
            is TabNode -> root.copy(
                stacks = root.stacks.map { replaceNode(it, targetKey, newNode) as StackNode }
            )
            is PaneNode -> root.copy(
                paneConfigurations = root.paneConfigurations.mapValues { (_, config) ->
                    config.copy(content = replaceNode(config.content, targetKey, newNode))
                }
            )
        }
    }

    override fun navigateBack(): Boolean {
        val currentState = _state.value
        val activeStack = currentState.activeStack()
        
        if (activeStack != null && activeStack.children.size > 1) {
            val newStack = activeStack.copy(
                children = activeStack.children.dropLast(1)
            )
            _state.value = replaceNode(currentState, activeStack.key, newStack)
            updateDerivedState()
            return true
        }
        // If can't pop, delegate to parent for cross-tab navigation
        return delegate.navigateBack()
    }

    override fun navigateUp(): Boolean {
        return navigateBack()
    }

    override fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String?,
        inclusive: Boolean
    ) {
        // First check: Is this a tab root destination? If so, switch tabs
        val targetTab = isTabRootDestination(destination)
        if (targetTab != null) {
            println("DEBUG_TAB_NAV: TabScopedNavigator.navigateAndClearTo - Switching to tab ${targetTab.route}")
            tabState.selectTab(targetTab)
            return
        }

        // For now, simplified implementation: just navigate
        // TODO: Implement proper clear logic with tree-based state
        if (isDestinationInTabGraph(destination)) {
            navigate(destination)
        } else {
            delegate.navigateAndClearTo(destination, clearRoute, inclusive)
        }
    }

    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        // First check: Is this a tab root destination? If so, switch tabs
        val targetTab = isTabRootDestination(destination)
        if (targetTab != null) {
            println("DEBUG_TAB_NAV: TabScopedNavigator.navigateAndReplace - Switching to tab ${targetTab.route}")
            tabState.selectTab(targetTab)
            return
        }

        // Second check: Is destination in tab's graph?
        if (isDestinationInTabGraph(destination)) {
            val currentState = _state.value
            val activeStack = currentState.activeStack()
            if (activeStack != null && activeStack.children.isNotEmpty()) {
                val newScreen = ScreenNode(
                    key = NavKeyGenerator.generate(),
                    parentKey = activeStack.key,
                    destination = destination
                )
                val newStack = activeStack.copy(
                    children = activeStack.children.dropLast(1) + newScreen
                )
                _state.value = replaceNode(currentState, activeStack.key, newStack)
                updateDerivedState()
            }
        } else {
            delegate.navigateAndReplace(destination, transition)
        }
    }

    override fun navigateAndClearAll(destination: Destination) {
        // First check: Is this a tab root destination? If so, switch tabs
        val targetTab = isTabRootDestination(destination)
        if (targetTab != null) {
            println("DEBUG_TAB_NAV: TabScopedNavigator.navigateAndClearAll - Switching to tab ${targetTab.route}")
            tabState.selectTab(targetTab)
            return
        }

        // Second check: Is destination in tab's graph?
        if (isDestinationInTabGraph(destination)) {
            _state.value = StackNode(
                key = NavKeyGenerator.generate(),
                parentKey = null,
                children = listOf(
                    ScreenNode(
                        key = NavKeyGenerator.generate(),
                        parentKey = null,
                        destination = destination
                    )
                )
            )
            updateDerivedState()
        } else {
            delegate.navigateAndClearAll(destination)
        }
    }

    override fun setStartDestination(destination: Destination) {
        _state.value = StackNode(
            key = NavKeyGenerator.generate(),
            parentKey = null,
            children = listOf(
                ScreenNode(
                    key = NavKeyGenerator.generate(),
                    parentKey = null,
                    destination = destination
                )
            )
        )
        updateDerivedState()
    }

    override fun handleDeepLink(deepLink: DeepLink) {
        // Deep links are handled by parent navigator
        delegate.handleDeepLink(deepLink)
    }

    override fun registerGraph(graph: NavigationGraph) {
        // Register graph with this tab navigator's own registry, not the parent
        graphs[graph.graphRoute] = graph
    }

    override fun getDeepLinkHandler(): DeepLinkHandler {
        return delegate.getDeepLinkHandler()
    }

    override fun setActiveChild(child: BackPressHandler?) {
        delegate.setActiveChild(child)
    }

    override fun handleBackInternal(): Boolean {
        return navigateBack()
    }
}
