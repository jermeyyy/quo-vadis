package com.jermey.navplayground.demo.ui.screens.statedriven

import com.jermey.quo.vadis.core.navigation.core.NavDestination

/**
 * Entry in the demo backstack.
 *
 * Represents a single entry in the navigation stack, containing
 * a unique identifier and the destination associated with this entry.
 *
 * @property id Unique identifier for this entry.
 * @property destination The navigation destination for this entry.
 */
data class BackStackEntry(
    val id: String,
    val destination: NavDestination
)
