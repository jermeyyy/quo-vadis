package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.StateFlow

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

    override val backStack: BackStack
        get() = delegate.backStack

    override val currentDestination: StateFlow<Destination?>
        get() = delegate.currentDestination

    override val previousDestination: StateFlow<Destination?>
        get() = delegate.previousDestination

    override val currentTransition: StateFlow<NavigationTransition?>
        get() = delegate.currentTransition

    override val activeChild: BackPressHandler?
        get() = delegate.activeChild

    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        delegate.navigate(destination, transition)
        // Tab state is automatically updated via the delegate's StateFlows
    }

    override fun navigateBack(): Boolean {
        return delegate.navigateBack()
    }

    override fun navigateUp(): Boolean {
        return delegate.navigateUp()
    }

    override fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String?,
        inclusive: Boolean
    ) {
        delegate.navigateAndClearTo(destination, clearRoute, inclusive)
    }

    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        delegate.navigateAndReplace(destination, transition)
    }

    override fun navigateAndClearAll(destination: Destination) {
        delegate.navigateAndClearAll(destination)
    }

    override fun setStartDestination(destination: Destination) {
        delegate.setStartDestination(destination)
    }

    override fun handleDeepLink(deepLink: DeepLink) {
        delegate.handleDeepLink(deepLink)
    }

    override fun registerGraph(graph: NavigationGraph) {
        delegate.registerGraph(graph)
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
