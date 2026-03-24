package com.jermey.navplayground.navigation

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Modal
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Navigation menu displayed as a modal overlay.
 *
 * Demonstrates `@Modal` annotation usage — when this destination is navigated to,
 * the library renders the previous screen underneath, and this screen draws on top.
 * The user composable controls all visual presentation (scrim, bottom sheet, etc.).
 */
@Modal
@Destination(route = "navigation_menu")
data object NavigationMenuDestination : NavDestination
