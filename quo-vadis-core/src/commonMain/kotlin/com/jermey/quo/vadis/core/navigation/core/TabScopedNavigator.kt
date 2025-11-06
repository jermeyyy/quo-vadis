package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
 * @param tab The tab definition this navigator is scoped to.
 * @param tabState The parent tab navigation state.
 * @param delegate The actual navigator instance to delegate operations to.
 */
@Suppress("TooManyFunctions")
internal class TabScopedNavigator(
    private val tab: TabDefinition,
    private val tabState: TabNavigatorState,
    private val delegate: Navigator
) : Navigator {

    // Tab navigators maintain their own graph registry separate from parent
    private val graphs = mutableMapOf<String, NavigationGraph>()
    
    // Tab navigators maintain their own backstack separate from parent
    override val backStack: BackStack = MutableBackStack()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override val currentDestination: StateFlow<Destination?> = 
        backStack.current.map { it?.destination }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    override val previousDestination: StateFlow<Destination?> = 
        backStack.previous.map { it?.destination }.stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, null)

    override val currentTransition: StateFlow<NavigationTransition?>
        get() = delegate.currentTransition

    override val activeChild: BackPressHandler?
        get() = delegate.activeChild
    
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
            backStack.push(destination, transition)
        } else {
            println("DEBUG_TAB_NAV: TabScopedNavigator - Destination NOT in tab graph, delegating to parent navigator")
            delegate.navigate(destination, transition)
        }
    }

    override fun navigateBack(): Boolean {
        val canGoBack = backStack.canGoBack.value
        if (canGoBack) {
            backStack.pop()
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
        
        // Second check: Is destination in tab's graph?
        if (isDestinationInTabGraph(destination)) {
            if (clearRoute != null) {
                backStack.popUntil { it.route == clearRoute }
                if (inclusive) {
                    backStack.pop()
                }
            }
            backStack.push(destination)
        } else {
            // Delegate to parent if destination not in tab
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
            backStack.pop()
            backStack.push(destination, transition)
        } else {
            // Delegate to parent if destination not in tab
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
            backStack.clear()
            backStack.push(destination)
        } else {
            // Delegate to parent if destination not in tab
            delegate.navigateAndClearAll(destination)
        }
    }

    override fun setStartDestination(destination: Destination) {
        backStack.clear()
        backStack.push(destination)
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
        return delegate.handleBackInternal()
    }
}
