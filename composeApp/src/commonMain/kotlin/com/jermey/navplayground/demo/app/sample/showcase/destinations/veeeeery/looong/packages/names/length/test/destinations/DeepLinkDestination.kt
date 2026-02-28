package com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Deep link demo destination.
 *
 * Accessible from anywhere via modal bottom sheet.
 * Allows navigation to all linked screens for testing deep links.
 */
@Stack(name = "deeplink", startDestination = DeepLinkDestination.Demo::class)
sealed class DeepLinkDestination : NavDestination {
    @Destination(route = "deeplink/demo")
    data object Demo : DeepLinkDestination()
}
