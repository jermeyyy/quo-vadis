package com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Back Handler Demo destination.
 *
 * Demonstrates the [com.jermey.quo.vadis.core.compose.NavBackHandler] composable
 * for intercepting user-initiated back navigation with an "unsaved changes" pattern.
 */
@Destination(route = "demo/back-handler")
data object BackHandlerDemoDestination : NavDestination
