package com.jermey.navplayground.navigation

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Modal
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.result.ReturnsResult

@TabItem(parent = MainTabs::class)
@Stack(name = "exploreTabStack", startDestination = ExploreTab.Feed::class)
@Transition(type = TransitionType.Fade)
sealed class ExploreTab : NavDestination {
    @Destination(route = "explore/feed")
    @Transition(type = TransitionType.Fade)
    data object Feed : ExploreTab()

    @Destination(route = "explore/detail/{itemId}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class Detail(@Argument val itemId: String) : ExploreTab()

    @Destination(route = "explore/category/{category}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class CategoryView(@Argument val category: String) : ExploreTab()

    @Modal
    @Destination(route = "explore/filters/{sortOrder}/{ratingFilter}/{category}")
    data class Filters(
        @Argument val sortOrder: String,
        @Argument val ratingFilter: String,
        @Argument val category: String,
    ) : ExploreTab(), ReturnsResult<ExploreFilterResult>
}
