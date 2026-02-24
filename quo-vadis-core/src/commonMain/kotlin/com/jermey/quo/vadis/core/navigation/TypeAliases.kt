package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition

/**
 * Key generator function type for creating unique node identifiers.
 */
typealias NavKeyGenerator = () -> NodeKey

/**
 * Callback invoked when a node is destroyed (detached and no longer displayed).
 */
typealias OnDestroyCallback = () -> Unit

/**
 * Provider function that resolves a navigation transition for a given destination.
 */
typealias NavTransitionProvider = (NavDestination) -> NavigationTransition?
