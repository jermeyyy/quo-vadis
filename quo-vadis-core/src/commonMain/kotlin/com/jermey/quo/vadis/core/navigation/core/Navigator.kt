package com.jermey.navplayground.navigation.core

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted

/**
 * Central navigation controller for the application.
 * Manages navigation state, backstack, and coordinates with graphs.
 *
 * Thread-safe and designed to work with MVI architecture pattern.
 */
@Stable
interface Navigator {
    /**
     * Access to the backstack for direct manipulation.
     */
    val backStack: BackStack

    /**
     * Current destination.
     */
    val currentDestination: StateFlow<Destination?>

    /**
     * Navigate to a destination with optional transition.
     */
    fun navigate(
        destination: Destination,
        transition: NavigationTransition? = null
    )

    /**
     * Navigate back in the stack.
     * @return true if navigation was successful
     */
    fun navigateBack(): Boolean

    /**
     * Navigate up in the hierarchy (similar to navigateBack but semantic).
     */
    fun navigateUp(): Boolean = navigateBack()

    /**
     * Navigate to a destination and clear the backstack up to a certain point.
     */
    fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String? = null,
        inclusive: Boolean = false
    )

    /**
     * Navigate to a destination and replace the current one.
     */
    fun navigateAndReplace(destination: Destination, transition: NavigationTransition? = null)

    /**
     * Navigate to a destination and clear the entire backstack.
     */
    fun navigateAndClearAll(destination: Destination)

    /**
     * Handle deep link navigation.
     */
    fun handleDeepLink(deepLink: DeepLink)

    /**
     * Register a navigation graph for modular navigation.
     */
    fun registerGraph(graph: NavigationGraph)

    /**
     * Set the start destination.
     */
    fun setStartDestination(destination: Destination)

    /**
     * Get the deep link handler to register patterns.
     */
    fun getDeepLinkHandler(): DeepLinkHandler
}

/**
 * Default implementation of Navigator.
 */
class DefaultNavigator(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler()
) : Navigator {

    private val _backStack: MutableBackStack = MutableBackStack()
    override val backStack: BackStack = _backStack

    override val currentDestination: StateFlow<Destination?> =
        _backStack.current.map { it?.destination }
            .stateIn(
                scope = CoroutineScope(Dispatchers.Default),
                started = SharingStarted.Eagerly,
                initialValue = null
            )

    private val graphs = mutableMapOf<String, NavigationGraph>()

    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        _backStack.push(destination)
    }

    override fun navigateBack(): Boolean {
        return _backStack.pop()
    }

    override fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String?,
        inclusive: Boolean
    ) {
        if (clearRoute != null) {
            _backStack.popUntil { it.route == clearRoute }
            if (inclusive) {
                _backStack.pop()
            }
        }
        _backStack.push(destination)
    }

    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        _backStack.replace(destination)
    }

    override fun navigateAndClearAll(destination: Destination) {
        _backStack.clear()
        _backStack.push(destination)
    }

    override fun handleDeepLink(deepLink: DeepLink) {
        val destination = deepLinkHandler.resolve(deepLink, graphs)
        destination?.let { navigate(it) }
    }

    override fun registerGraph(graph: NavigationGraph) {
        graphs[graph.graphRoute] = graph
    }

    override fun setStartDestination(destination: Destination) {
        _backStack.clear()
        _backStack.push(destination)
    }

    override fun getDeepLinkHandler(): DeepLinkHandler = deepLinkHandler
}
