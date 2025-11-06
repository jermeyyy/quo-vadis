package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
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
 *
 * Implements [ParentNavigator] to support hierarchical navigation with child navigators.
 */
@Stable
interface Navigator : ParentNavigator {
    /**
     * Access to the backstack for direct manipulation.
     */
    val backStack: BackStack

    /**
     * Current destination.
     */
    val currentDestination: StateFlow<Destination?>

    /**
     * Previous destination (if any).
     */
    val previousDestination: StateFlow<Destination?>

    /**
     * Current transition being applied (null if no animation in progress).
     */
    val currentTransition: StateFlow<NavigationTransition?>

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
    
    /**
     * Set the active child navigator for back press delegation.
     * 
     * When a child navigator is set, back press events will be delegated
     * to the child first before being handled by this navigator.
     * 
     * @param child The child navigator to delegate to, or null to clear.
     */
    fun setActiveChild(child: BackPressHandler?)
}

/**
 * Default implementation of Navigator.
 */
@Suppress("TooManyFunctions")
class DefaultNavigator(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler()
) : Navigator {

    private val _backStack: MutableBackStack = MutableBackStack()
    override val backStack: BackStack = _backStack

    private val _currentDestination = MutableStateFlow<Destination?>(_backStack.current.value?.destination)
    override val currentDestination: StateFlow<Destination?> = _currentDestination

    private val _previousDestination = MutableStateFlow<Destination?>(_backStack.previous.value?.destination)
    override val previousDestination: StateFlow<Destination?> = _previousDestination

    private val _currentTransition = MutableStateFlow<NavigationTransition?>(null)
    override val currentTransition: StateFlow<NavigationTransition?> = _currentTransition

    private val graphs = mutableMapOf<String, NavigationGraph>()
    
    // Child navigator support for hierarchical navigation
    private var _activeChild: BackPressHandler? = null
    override val activeChild: BackPressHandler?
        get() = _activeChild

    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        // Use provided transition, or fall back to destination's default
        val effectiveTransition = transition ?: destination.transition
        _backStack.push(destination, effectiveTransition)
        _currentTransition.value = effectiveTransition
        updateDestinationFlows()
    }
    
    /**
     * Handle back press for this navigator's backstack.
     * Implementation of ParentNavigator.handleBackInternal().
     */
    override fun handleBackInternal(): Boolean {
        // Only pop if we can actually go back (more than 1 entry in stack)
        if (!_backStack.canGoBack.value) {
            return false
        }
        
        val result = _backStack.pop()
        if (result) {
            // Current transition should be the one from the entry being revealed
            _currentTransition.value = _backStack.current.value?.transition
            updateDestinationFlows()
        }
        return result
    }

    override fun navigateBack(): Boolean {
        // Use ParentNavigator's delegation logic (tries child first, then handleBackInternal)
        return onBack()
    }
    
    private fun updateDestinationFlows() {
        _currentDestination.value = _backStack.current.value?.destination
        _previousDestination.value = _backStack.previous.value?.destination
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
        _currentTransition.value = null
        updateDestinationFlows()
    }

    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        _backStack.replace(destination, transition)
        _currentTransition.value = transition
        updateDestinationFlows()
    }

    override fun navigateAndClearAll(destination: Destination) {
        _backStack.clear()
        _backStack.push(destination)
        _currentTransition.value = null
        updateDestinationFlows()
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
        updateDestinationFlows()
    }

    override fun getDeepLinkHandler(): DeepLinkHandler = deepLinkHandler
    
    override fun setActiveChild(child: BackPressHandler?) {
        _activeChild = child
    }
}
