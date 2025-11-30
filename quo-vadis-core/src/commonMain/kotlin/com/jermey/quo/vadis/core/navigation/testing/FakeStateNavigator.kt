package com.jermey.quo.vadis.core.navigation.testing

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.StateBackStack
import com.jermey.quo.vadis.core.navigation.core.StateNavigator
import com.jermey.quo.vadis.core.navigation.core.route

/**
 * A fake implementation of [StateNavigator] for testing purposes.
 *
 * This class records all navigation operations, allowing tests to verify
 * that the correct navigation calls were made. It mirrors the [StateNavigator]
 * API while providing additional assertion helpers.
 *
 * Key features:
 * - **Operation recording**: All navigation operations are recorded in [navigationOperations]
 * - **Assertion helpers**: Methods like [verifyNavigatedTo] and [verifyStackSize] for easy verification
 * - **Full state access**: Access to the current navigation state through [currentDestination], [entries], etc.
 *
 * Example usage:
 * ```kotlin
 * @Test
 * fun `test navigation to detail screen`() {
 *     val navigator = FakeStateNavigator()
 *
 *     // Perform navigation
 *     navigator.navigate(HomeDestination)
 *     navigator.navigate(DetailDestination(id = "123"))
 *
 *     // Verify
 *     navigator.verifyNavigatedTo("detail")
 *     navigator.verifyStackSize(2)
 *     navigator.verifyCurrentDestination<DetailDestination>()
 * }
 * ```
 *
 * @see StateNavigator for the production implementation
 * @see FakeNavigator for the flow-based fake navigator
 */
@Stable
class FakeStateNavigator {

    private val backStack = StateBackStack()

    /**
     * List of all navigation operations performed on this navigator.
     *
     * Each operation is recorded with its type and relevant parameters.
     * Use [clearOperations] to reset this list between test cases.
     */
    val navigationOperations: SnapshotStateList<StateNavigationOperation> = mutableStateListOf()

    /**
     * The list of backstack entries.
     */
    val entries: SnapshotStateList<BackStackEntry>
        get() = backStack.entries

    /**
     * The current destination being displayed.
     */
    val currentDestination: Destination? by derivedStateOf {
        backStack.current?.destination
    }

    /**
     * The previous destination in the backstack.
     */
    val previousDestination: Destination? by derivedStateOf {
        backStack.previous?.destination
    }

    /**
     * Whether backward navigation is possible.
     */
    val canGoBack: Boolean
        get() = backStack.canGoBack

    /**
     * The current backstack entry.
     */
    val currentEntry: BackStackEntry?
        get() = backStack.current

    /**
     * The number of entries in the backstack.
     */
    val stackSize: Int
        get() = backStack.size

    /**
     * Navigates to a new destination by pushing it onto the backstack.
     * Records a [StateNavigationOperation.Navigate] operation.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation for this navigation
     */
    fun navigate(destination: Destination, transition: NavigationTransition? = null) {
        navigationOperations.add(StateNavigationOperation.Navigate(destination, transition))
        backStack.push(destination, transition)
    }

    /**
     * Navigates back by removing the current destination from the backstack.
     * Records a [StateNavigationOperation.NavigateBack] operation.
     *
     * @return `true` if navigation was successful, `false` otherwise
     */
    fun navigateBack(): Boolean {
        val success = if (backStack.canGoBack) {
            backStack.pop()
            true
        } else {
            false
        }
        navigationOperations.add(StateNavigationOperation.NavigateBack(success))
        return success
    }

    /**
     * Navigates to a destination, replacing the current one.
     * Records a [StateNavigationOperation.NavigateAndReplace] operation.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation for this navigation
     */
    fun navigateAndReplace(destination: Destination, transition: NavigationTransition? = null) {
        navigationOperations.add(StateNavigationOperation.NavigateAndReplace(destination, transition))
        if (backStack.isNotEmpty) {
            backStack.pop()
        }
        backStack.push(destination, transition)
    }

    /**
     * Navigates to a destination after clearing the entire backstack.
     * Records a [StateNavigationOperation.NavigateAndClearAll] operation.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation for this navigation
     */
    fun navigateAndClearAll(destination: Destination, transition: NavigationTransition? = null) {
        navigationOperations.add(StateNavigationOperation.NavigateAndClearAll(destination, transition))
        backStack.clear()
        backStack.push(destination, transition)
    }

    /**
     * Clears the entire backstack.
     * Records a [StateNavigationOperation.Clear] operation.
     */
    fun clear() {
        navigationOperations.add(StateNavigationOperation.Clear)
        backStack.clear()
    }

    /**
     * Provides direct access to the underlying [StateBackStack].
     *
     * @return The underlying [StateBackStack] instance
     */
    fun getBackStack(): StateBackStack = backStack

    // ==================== Test Utilities ====================

    /**
     * Clears all recorded navigation operations.
     *
     * Call this between test cases to reset the operation history
     * while preserving the current navigation state.
     */
    fun clearOperations() {
        navigationOperations.clear()
    }

    /**
     * Resets the navigator to its initial state.
     *
     * Clears both the navigation operations and the backstack.
     */
    fun reset() {
        navigationOperations.clear()
        backStack.clear()
    }

    // ==================== Assertion Helpers ====================

    /**
     * Verifies that a navigation to the specified route occurred.
     *
     * @param route The route string to check for
     * @return `true` if any [StateNavigationOperation.Navigate] operation with the given route was recorded
     */
    fun verifyNavigatedTo(route: String): Boolean {
        return navigationOperations.any { operation ->
            operation is StateNavigationOperation.Navigate && operation.destination.route == route
        }
    }

    /**
     * Verifies that a navigation to a destination of the specified type occurred.
     *
     * @param T The destination type to check for
     * @return `true` if any [StateNavigationOperation.Navigate] operation with a destination of type [T] was recorded
     */
    inline fun <reified T : Destination> verifyNavigatedToType(): Boolean {
        return navigationOperations.any { operation ->
            operation is StateNavigationOperation.Navigate && operation.destination is T
        }
    }

    /**
     * Verifies that [navigateBack] was called.
     *
     * @param successOnly If `true`, only counts successful back navigations
     * @return `true` if a matching [StateNavigationOperation.NavigateBack] operation was recorded
     */
    fun verifyNavigatedBack(successOnly: Boolean = false): Boolean {
        return navigationOperations.any { operation ->
            operation is StateNavigationOperation.NavigateBack &&
                (!successOnly || operation.success)
        }
    }

    /**
     * Verifies that the backstack has the expected size.
     *
     * @param expectedSize The expected number of entries in the backstack
     * @return `true` if the current stack size matches the expected size
     */
    fun verifyStackSize(expectedSize: Int): Boolean {
        return backStack.size == expectedSize
    }

    /**
     * Verifies that the current destination is of the specified type.
     *
     * @param T The expected destination type
     * @return `true` if the current destination is of type [T]
     */
    inline fun <reified T : Destination> verifyCurrentDestination(): Boolean {
        return currentDestination is T
    }

    /**
     * Verifies that the current destination matches the specified route.
     *
     * @param route The expected route string
     * @return `true` if the current destination's route matches
     */
    fun verifyCurrentRoute(route: String): Boolean {
        return currentDestination?.route == route
    }

    /**
     * Returns the count of [StateNavigationOperation.Navigate] operations to the specified route.
     *
     * @param route The route string to count navigations to
     * @return The number of times navigation to this route was recorded
     */
    fun getNavigateCallCount(route: String): Int {
        return navigationOperations.count { operation ->
            operation is StateNavigationOperation.Navigate && operation.destination.route == route
        }
    }

    /**
     * Returns the total count of navigation operations of the specified type.
     *
     * @param T The operation type to count
     * @return The number of operations of type [T]
     */
    inline fun <reified T : StateNavigationOperation> getOperationCount(): Int {
        return navigationOperations.count { it is T }
    }

    /**
     * Asserts that a navigation to the specified route occurred.
     *
     * @param route The route string to check for
     * @throws AssertionError if no navigation to this route was recorded
     */
    fun assertNavigatedTo(route: String) {
        if (!verifyNavigatedTo(route)) {
            throw AssertionError(
                "Expected navigation to route '$route', but it was not found. " +
                    "Recorded operations: $navigationOperations"
            )
        }
    }

    /**
     * Asserts that the backstack has the expected size.
     *
     * @param expectedSize The expected number of entries
     * @throws AssertionError if the stack size doesn't match
     */
    fun assertStackSize(expectedSize: Int) {
        val actualSize = backStack.size
        if (actualSize != expectedSize) {
            throw AssertionError(
                "Expected stack size $expectedSize, but was $actualSize. " +
                    "Entries: ${entries.map { it.destination.route }}"
            )
        }
    }

    /**
     * Asserts that the current destination is of the specified type.
     *
     * @param T The expected destination type
     * @throws AssertionError if the current destination is not of type [T]
     */
    inline fun <reified T : Destination> assertCurrentDestination() {
        if (!verifyCurrentDestination<T>()) {
            throw AssertionError(
                "Expected current destination to be ${T::class.simpleName}, " +
                    "but was ${currentDestination?.let { it::class.simpleName } ?: "null"}"
            )
        }
    }
}

/**
 * Sealed class representing different navigation operations for testing.
 *
 * Each subclass captures the parameters of the corresponding navigation method
 * in [FakeStateNavigator].
 */
sealed class StateNavigationOperation {

    /**
     * Represents a [FakeStateNavigator.navigate] call.
     *
     * @property destination The destination that was navigated to
     * @property transition The transition animation used, if any
     */
    data class Navigate(
        val destination: Destination,
        val transition: NavigationTransition?
    ) : StateNavigationOperation()

    /**
     * Represents a [FakeStateNavigator.navigateBack] call.
     *
     * @property success Whether the back navigation was successful
     */
    data class NavigateBack(val success: Boolean) : StateNavigationOperation()

    /**
     * Represents a [FakeStateNavigator.navigateAndReplace] call.
     *
     * @property destination The destination that replaced the previous one
     * @property transition The transition animation used, if any
     */
    data class NavigateAndReplace(
        val destination: Destination,
        val transition: NavigationTransition?
    ) : StateNavigationOperation()

    /**
     * Represents a [FakeStateNavigator.navigateAndClearAll] call.
     *
     * @property destination The destination that became the only entry
     * @property transition The transition animation used, if any
     */
    data class NavigateAndClearAll(
        val destination: Destination,
        val transition: NavigationTransition?
    ) : StateNavigationOperation()

    /**
     * Represents a [FakeStateNavigator.clear] call.
     */
    data object Clear : StateNavigationOperation()
}

/**
 * Test builder for creating state-driven navigation test scenarios.
 *
 * Provides a fluent DSL for setting up navigation state and performing verifications.
 *
 * Example:
 * ```kotlin
 * stateNavigationTest {
 *     given {
 *         navigate(HomeDestination)
 *     }
 *     `when` {
 *         navigate(DetailDestination(id = "123"))
 *     }
 *     then {
 *         assertStackSize(2)
 *         assertNavigatedTo("detail")
 *     }
 * }
 * ```
 */
@Suppress("FunctionNaming")
class StateNavigationTestBuilder {
    private val navigator = FakeStateNavigator()

    /**
     * Sets up the initial navigation state.
     */
    fun given(block: FakeStateNavigator.() -> Unit): StateNavigationTestBuilder {
        navigator.block()
        navigator.clearOperations() // Clear operations from setup
        return this
    }

    /**
     * Performs the navigation action being tested.
     */
    fun `when`(block: FakeStateNavigator.() -> Unit): StateNavigationTestBuilder {
        navigator.block()
        return this
    }

    /**
     * Verifies the expected navigation state.
     */
    fun then(block: FakeStateNavigator.() -> Unit): StateNavigationTestBuilder {
        navigator.block()
        return this
    }

    /**
     * Returns the configured [FakeStateNavigator] for additional assertions.
     */
    fun build(): FakeStateNavigator = navigator
}

/**
 * DSL entry point for creating state-driven navigation tests.
 *
 * @param block The test configuration block
 * @return The configured [FakeStateNavigator] for additional assertions
 *
 * @see StateNavigationTestBuilder for the available DSL methods
 */
fun stateNavigationTest(block: StateNavigationTestBuilder.() -> Unit): FakeStateNavigator {
    return StateNavigationTestBuilder().apply(block).build()
}
