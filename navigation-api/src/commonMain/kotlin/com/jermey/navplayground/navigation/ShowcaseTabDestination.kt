package com.jermey.navplayground.navigation

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Showcase tab — demonstrates cross-module navigation patterns.
 *
 * Defined in feature3-api, screens implemented in feature3.
 * Ordinal = 4 (after Settings ordinal).
 */
@TabItem(parent = MainTabs::class, ordinal = 4)
@Stack(name = "showcaseTabStack", startDestination = ShowcaseTab.Overview::class)
@Transition(type = TransitionType.Fade)
sealed class ShowcaseTab : NavDestination {

    @Destination(route = "showcase/overview")
    @Transition(type = TransitionType.Fade)
    data object Overview : ShowcaseTab()

    @Destination(route = "showcase/detail/{itemId}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class Detail(@Argument val itemId: String) : ShowcaseTab()
}
