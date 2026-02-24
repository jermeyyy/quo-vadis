@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.testing

import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.node.allScreens
import com.jermey.quo.vadis.core.navigation.node.nodeCount
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers

/**
 * DSL marker to prevent scope leakage in navigation test blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class NavigationTestDsl

/**
 * Scope for writing concise navigation tests using a DSL.
 *
 * Wraps a [TreeNavigator] and exposes navigation actions and assertion helpers.
 *
 * ## Usage
 * ```kotlin
 * @Test
 * fun `navigate to detail and back`() = navigationTest(HomeDestination) {
 *     navigateTo(DetailDestination)
 *     navigateBack()
 *     assertCurrentDestination<HomeDestination>()
 *     assertBackStackSize(1)
 * }
 * ```
 */
@NavigationTestDsl
class NavigationTestScope(val navigator: TreeNavigator) {

    // =========================================================================
    // STATE ACCESSORS
    // =========================================================================

    /** The current navigation tree state. */
    val state: NavNode get() = navigator.state.value

    /** The current active destination, or null if the tree is empty. */
    val currentDestination: NavDestination? get() = navigator.currentDestination.value

    /** Whether the navigator can navigate back from the current state. */
    val canNavigateBack: Boolean get() = navigator.canNavigateBack.value

    // =========================================================================
    // NAVIGATION ACTIONS
    // =========================================================================

    /**
     * Navigate to the given [destination], pushing it onto the active stack.
     */
    fun navigateTo(destination: NavDestination, transition: NavigationTransition? = null) {
        navigator.navigate(destination, transition)
    }

    /**
     * Navigate back in the active stack.
     *
     * @return true if navigation was successful, false if at root
     */
    fun navigateBack(): Boolean = navigator.navigateBack()

    /**
     * Alias for [navigateBack].
     */
    fun pop(): Boolean = navigateBack()

    /**
     * Pop the back stack until the current destination is of type [D].
     *
     * @throws AssertionError if [D] is not found in the back stack
     */
    inline fun <reified D : NavDestination> popTo() {
        while (navigator.currentDestination.value !is D) {
            if (!navigator.canNavigateBack.value) {
                fail("Cannot pop to ${D::class.simpleName}: destination not found in back stack")
            }
            navigator.navigateBack()
        }
    }

    /**
     * Replace the current destination with [destination].
     */
    fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition? = null) {
        navigator.navigateAndReplace(destination, transition)
    }

    /**
     * Navigate to [destination] and clear the back stack up to [clearRoute].
     */
    fun navigateAndClearTo(
        destination: NavDestination,
        clearRoute: String? = null,
        inclusive: Boolean = false,
    ) {
        navigator.navigateAndClearTo(destination, clearRoute, inclusive)
    }

    /**
     * Navigate to [destination] and clear the entire active stack.
     */
    fun navigateAndClearAll(destination: NavDestination) {
        navigator.navigateAndClearAll(destination)
    }

    // =========================================================================
    // ASSERTIONS
    // =========================================================================

    /**
     * Assert that the current destination is of type [D] and return it.
     *
     * @return the current destination cast to [D]
     * @throws AssertionError if the current destination is not of type [D]
     */
    inline fun <reified D : NavDestination> assertCurrentDestination(): D {
        val current = navigator.currentDestination.value
        assertTrue(
            current is D,
            "Expected current destination to be ${D::class.simpleName}, " +
                "but was ${current?.let { it::class.simpleName } ?: "null"}"
        )
        @Suppress("UNCHECKED_CAST")
        return current as D
    }

    /**
     * Assert the number of children in the deepest active [StackNode].
     */
    fun assertBackStackSize(expectedSize: Int) {
        val activeStack = assertNotNull(navigator.state.value.activeStack(), "No active stack found")
        assertEquals(expectedSize, activeStack.size, "Back stack size mismatch")
    }

    /**
     * Assert that backward navigation is possible.
     */
    fun assertCanNavigateBack() {
        assertTrue(navigator.canNavigateBack.value, "Expected to be able to navigate back, but cannot")
    }

    /**
     * Assert that backward navigation is **not** possible.
     */
    fun assertCannotNavigateBack() {
        assertFalse(navigator.canNavigateBack.value, "Expected not to be able to navigate back, but can")
    }

    /**
     * Assert the total number of nodes in the navigation tree.
     */
    fun assertNodeCount(expectedCount: Int) {
        assertEquals(expectedCount, navigator.state.value.nodeCount(), "Node count mismatch")
    }

    /**
     * Assert that a destination of type [D] exists somewhere in the active stack's children.
     */
    inline fun <reified D : NavDestination> assertDestinationInBackStack() {
        val screens = navigator.state.value.allScreens()
        val found = screens.any { it.destination is D }
        assertTrue(
            found,
            "Expected ${D::class.simpleName} in back stack, but it was not found. " +
                "Destinations present: ${screens.map { it.destination::class.simpleName }}"
        )
    }
}

// =============================================================================
// ENTRY POINTS
// =============================================================================

/**
 * Run a navigation test starting from [startDestination].
 *
 * Creates a [TreeNavigator] initialized with a single-screen stack and executes [block]
 * inside a [NavigationTestScope].
 *
 * ```kotlin
 * @Test
 * fun `basic navigation`() = navigationTest(HomeDestination) {
 *     navigateTo(DetailDestination)
 *     assertCurrentDestination<DetailDestination>()
 *     assertBackStackSize(2)
 * }
 * ```
 */
fun navigationTest(
    startDestination: NavDestination,
    block: NavigationTestScope.() -> Unit,
) {
    NavKeyGenerator.reset()
    val navigator = TreeNavigator.withDestination(
        destination = startDestination,
        coroutineContext = Dispatchers.Unconfined,
    )
    NavigationTestScope(navigator).block()
}

/**
 * Run a navigation test with a custom initial [NavNode] tree.
 *
 * Use this overload when you need a complex initial state (e.g. tabs, panes, pre-filled stacks).
 *
 * ```kotlin
 * @Test
 * fun `complex state test`() = navigationTest(
 *     initialState = StackNode(
 *         key = NodeKey("root"), parentKey = null,
 *         children = listOf(
 *             ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
 *             ScreenNode(NodeKey("s2"), NodeKey("root"), DetailDestination),
 *         )
 *     )
 * ) {
 *     navigateBack()
 *     assertCurrentDestination<HomeDestination>()
 * }
 * ```
 */
fun navigationTest(
    initialState: NavNode,
    block: NavigationTestScope.() -> Unit,
) {
    NavKeyGenerator.reset()
    val navigator = TreeNavigator.withState(
        initialState = initialState,
        coroutineContext = Dispatchers.Unconfined,
    )
    NavigationTestScope(navigator).block()
}

// =============================================================================
// PRIVATE HELPERS
// =============================================================================

/**
 * Inline assertNotNull that returns a non-null value for chaining.
 */
private fun <T : Any> assertNotNull(value: T?, message: String): T {
    kotlin.test.assertNotNull(value, message)
    return value
}
