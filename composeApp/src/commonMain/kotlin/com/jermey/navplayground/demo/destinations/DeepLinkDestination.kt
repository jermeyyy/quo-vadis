package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination

/**
 * Deep link demo destination.
 *
 * Accessible from anywhere via modal bottom sheet.
 * Allows navigation to all linked screens for testing deep links.
 */
@Stack(name = "deeplink", startDestination = "Demo")
sealed class DeepLinkDestination : Destination {
    @com.jermey.quo.vadis.annotations.Destination(route = "deeplink/demo")
    data object Demo : DeepLinkDestination()
}
