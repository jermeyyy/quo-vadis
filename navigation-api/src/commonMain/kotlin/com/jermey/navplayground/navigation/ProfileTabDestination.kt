package com.jermey.navplayground.navigation

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

@TabItem(parent = MainTabs::class, ordinal = 2)
@Destination(route = "main/profile")
@Transition(type = TransitionType.Fade)
data object ProfileTab : NavDestination
