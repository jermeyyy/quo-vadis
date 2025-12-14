package com.jermey.quo.vadis.core.navigation.utils

import com.jermey.quo.vadis.core.navigation.core.route
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.allScreens

/**
 * Extension functions and utilities for the navigation library.
 */

// =========================================================================
// NAVNODE TREE EXTENSIONS
// =========================================================================

/**
 * Extension to check if a destination is in the navigation tree.
 */
fun NavNode.containsRoute(route: String): Boolean {
    return allScreens().any { it.destination.route == route }
}

/**
 * Extension to find a screen node by route.
 */
fun NavNode.findByRoute(route: String): ScreenNode? {
    return allScreens().find { it.destination.route == route }
}

/**
 * Extension to get all routes in the navigation tree.
 */
val NavNode.routes: List<String>
    get() = allScreens().map { it.destination.route }

// =========================================================================
// NAVIGATOR EXTENSIONS
// =========================================================================

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
    if (currentDestination.value?.route == destination.route) {
        navigateAndReplace(destination, transition)
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
