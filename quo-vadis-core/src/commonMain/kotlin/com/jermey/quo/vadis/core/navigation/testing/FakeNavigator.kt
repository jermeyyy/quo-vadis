package com.jermey.navplayground.navigation.testing

import com.jermey.navplayground.navigation.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fake Navigator implementation for testing purposes.
 * Allows you to verify navigation calls without actual UI rendering.
 */
class FakeNavigator : Navigator {

    private val _backStack = MutableBackStack()
    override val backStack: BackStack = _backStack

    override val currentDestination: StateFlow<Destination?>
        get() = MutableStateFlow(_backStack.current.value?.destination)

    override val previousDestination: StateFlow<Destination?>
        get() = MutableStateFlow(_backStack.previous.value?.destination)

    // Track navigation calls for verification
    val navigationCalls = mutableListOf<NavigationCall>()

    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        navigationCalls.add(NavigationCall.Navigate(destination, transition))
        _backStack.push(destination)
    }

    override fun navigateBack(): Boolean {
        val result = _backStack.pop()
        navigationCalls.add(NavigationCall.NavigateBack(result))
        return result
    }

    override fun navigateAndClearTo(destination: Destination, clearRoute: String?, inclusive: Boolean) {
        navigationCalls.add(NavigationCall.NavigateAndClearTo(destination, clearRoute, inclusive))
        if (clearRoute != null) {
            _backStack.popUntil { it.route == clearRoute }
            if (inclusive) {
                _backStack.pop()
            }
        }
        _backStack.push(destination)
    }

    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        navigationCalls.add(NavigationCall.NavigateAndReplace(destination, transition))
        _backStack.replace(destination)
    }

    override fun navigateAndClearAll(destination: Destination) {
        navigationCalls.add(NavigationCall.NavigateAndClearAll(destination))
        _backStack.clear()
        _backStack.push(destination)
    }

    override fun handleDeepLink(deepLink: DeepLink) {
        navigationCalls.add(NavigationCall.HandleDeepLink(deepLink))
        // Default implementation does nothing in tests
    }

    override fun registerGraph(graph: NavigationGraph) {
        navigationCalls.add(NavigationCall.RegisterGraph(graph))
    }

    override fun setStartDestination(destination: Destination) {
        navigationCalls.add(NavigationCall.SetStartDestination(destination))
        _backStack.clear()
        _backStack.push(destination)
    }

    private val fakeDeepLinkHandler = DefaultDeepLinkHandler()

    override fun getDeepLinkHandler(): DeepLinkHandler {
        return fakeDeepLinkHandler
    }

    /**
     * Clear all recorded navigation calls.
     */
    fun clearCalls() {
        navigationCalls.clear()
    }

    /**
     * Verify that a specific navigation call was made.
     */
    fun verifyNavigateTo(route: String): Boolean {
        return navigationCalls.any { call ->
            call is NavigationCall.Navigate && call.destination.route == route
        }
    }

    /**
     * Verify that navigateBack was called.
     */
    fun verifyNavigateBack(): Boolean {
        return navigationCalls.any { it is NavigationCall.NavigateBack }
    }

    /**
     * Get the count of navigate calls.
     */
    fun getNavigateCallCount(route: String): Int {
        return navigationCalls.count { call ->
            call is NavigationCall.Navigate && call.destination.route == route
        }
    }
}

/**
 * Sealed class representing different navigation calls for testing.
 */
sealed class NavigationCall {
    data class Navigate(
        val destination: Destination,
        val transition: NavigationTransition?
    ) : NavigationCall()

    data class NavigateBack(val success: Boolean) : NavigationCall()

    data class NavigateAndClearTo(
        val destination: Destination,
        val clearRoute: String?,
        val inclusive: Boolean
    ) : NavigationCall()

    data class NavigateAndReplace(
        val destination: Destination,
        val transition: NavigationTransition?
    ) : NavigationCall()

    data class NavigateAndClearAll(val destination: Destination) : NavigationCall()

    data class HandleDeepLink(val deepLink: DeepLink) : NavigationCall()

    data class RegisterGraph(val graph: NavigationGraph) : NavigationCall()

    data class SetStartDestination(val destination: Destination) : NavigationCall()
}

/**
 * Test builder for creating test navigation scenarios.
 */
class NavigationTestBuilder {
    private val navigator = FakeNavigator()

    fun given(block: FakeNavigator.() -> Unit): NavigationTestBuilder {
        navigator.block()
        return this
    }

    fun `when`(block: FakeNavigator.() -> Unit): NavigationTestBuilder {
        navigator.block()
        return this
    }

    fun then(block: FakeNavigator.() -> Unit): NavigationTestBuilder {
        navigator.block()
        return this
    }

    fun build() = navigator
}

/**
 * DSL for creating navigation tests.
 */
fun navigationTest(block: NavigationTestBuilder.() -> Unit): FakeNavigator {
    return NavigationTestBuilder().apply(block).build()
}
