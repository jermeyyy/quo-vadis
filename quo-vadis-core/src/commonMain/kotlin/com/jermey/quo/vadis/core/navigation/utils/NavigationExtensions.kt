package com.jermey.quo.vadis.core.navigation.utils

import com.jermey.quo.vadis.core.navigation.core.BackStack
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.route
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Extension functions and utilities for the navigation library.
 */

/**
 * Extension to check if a destination is in the backstack.
 */
fun BackStack.contains(route: String): Boolean {
    return stack.value.any { it.destination.route == route }
}

/**
 * Extension to get a destination from the backstack by route.
 */
fun BackStack.findByRoute(route: String): BackStackEntry? {
    return stack.value.find { it.destination.route == route }
}

/**
 * Extension to get the size of the backstack.
 * @deprecated Use BackStack.size property directly instead.
 */
@Deprecated(
    message = "Use BackStack.size property directly instead",
    replaceWith = ReplaceWith("size"),
    level = DeprecationLevel.WARNING
)
val BackStack.extensionSize: Int
    get() = stack.value.size

/**
 * Extension to check if the backstack is empty.
 * @deprecated Use BackStack.isEmpty property directly instead.
 */
@Deprecated(
    message = "Use BackStack.isEmpty property directly instead",
    replaceWith = ReplaceWith("isEmpty"),
    level = DeprecationLevel.WARNING
)
val BackStack.extensionIsEmpty: Boolean
    get() = stack.value.isEmpty()

/**
 * Extension to get all routes in the backstack.
 */
val BackStack.routes: List<String>
    get() = stack.value.map { it.destination.route }

/**
 * Pop multiple entries from the backstack.
 */
fun BackStack.popCount(count: Int): Boolean {
    if (count <= 0) return false

    repeat(count) {
        if (!pop()) return false
    }
    return true
}

/**
 * Extension to navigate with a lambda for building the destination.
 */
fun Navigator.navigateTo(
    transition: NavigationTransition? = null,
    builder: () -> Destination
) {
    navigate(builder(), transition)
}

/**
 * Extension to navigate only if not already at the destination.
 */
fun Navigator.navigateIfNotCurrent(
    destination: Destination,
    transition: NavigationTransition? = null
) {
    if (currentDestination.value?.route != destination.route) {
        navigate(destination, transition)
    }
}

/**
 * Extension to navigate with single top behavior (replace if same route exists).
 */
fun Navigator.navigateSingleTop(
    destination: Destination,
    transition: NavigationTransition? = null
) {
    if (backStack.current.value?.destination?.route == destination.route) {
        backStack.replace(destination)
    } else {
        navigate(destination, transition)
    }
}

/**
 * Safe navigation that handles errors.
 */
fun Navigator.navigateSafely(
    destination: Destination,
    transition: NavigationTransition? = null,
    onError: (Exception) -> Unit = {}
): Boolean {
    return try {
        navigate(destination, transition)
        true
    } catch (e: Exception) {
        onError(e)
        false
    }
}

/**
 * Navigation scope for building complex navigation flows.
 */
class NavigationScope(private val navigator: Navigator) {
    fun navigate(destination: Destination, transition: NavigationTransition? = null) {
        navigator.navigate(destination, transition)
    }

    fun back() = navigator.navigateBack()

    fun replace(destination: Destination, transition: NavigationTransition? = null) {
        navigator.navigateAndReplace(destination, transition)
    }

    fun clearAndNavigate(destination: Destination) {
        navigator.navigateAndClearAll(destination)
    }
}

/**
 * Execute navigation in a scope.
 */
fun Navigator.inScope(block: NavigationScope.() -> Unit) {
    NavigationScope(this).block()
}
