package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack

@Stack(name = "settings", startDestination = "Profile")
sealed class SettingsDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "settings/profile")
    data object Profile : SettingsDestination()

    @Destination(route = "settings/notifications")
    data object Notifications : SettingsDestination()

    @Destination(route = "settings/about")
    data object About : SettingsDestination()
}
