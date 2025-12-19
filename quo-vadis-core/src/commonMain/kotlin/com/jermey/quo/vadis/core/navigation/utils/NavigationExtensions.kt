package com.jermey.quo.vadis.core.navigation.utils

import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator

// =========================================================================
// NAVIGATOR EXTENSIONS
// =========================================================================

/**
 * Extension to navigate with a lambda for building the destination.
 */
fun Navigator.navigateTo(
    transition: NavigationTransition? = null,
    builder: () -> NavDestination
) {
    navigate(builder(), transition)
}