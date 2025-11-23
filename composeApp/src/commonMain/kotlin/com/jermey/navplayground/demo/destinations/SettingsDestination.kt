package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Graph
import com.jermey.quo.vadis.annotations.Route
import com.jermey.quo.vadis.core.navigation.core.Destination

@Graph("settings")
sealed class SettingsDestination : Destination {
    @Route("settings/profile")
    data object Profile : SettingsDestination()

    @Route("settings/notifications")
    data object Notifications : SettingsDestination()

    @Route("settings/about")
    data object About : SettingsDestination()
}
