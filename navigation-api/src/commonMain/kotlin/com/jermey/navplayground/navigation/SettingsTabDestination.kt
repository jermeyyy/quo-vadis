package com.jermey.navplayground.navigation

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@TabItem(parent = MainTabs::class)
@Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
@Transition(type = TransitionType.Fade)
sealed class SettingsTab : NavDestination {
    @Destination(route = "settings/main")
    @Transition(type = TransitionType.Fade)
    data object Main : SettingsTab()

    @Destination(route = "settings/profile")
    @Transition(type = TransitionType.SlideHorizontal)
    data object Profile : SettingsTab()

    @Destination(route = "settings/notifications")
    @Transition(type = TransitionType.SlideHorizontal)
    data object Notifications : SettingsTab()

    @Destination(route = "settings/about")
    @Transition(type = TransitionType.SlideHorizontal)
    data object About : SettingsTab()
}
