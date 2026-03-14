package com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations

import com.jermey.navplayground.navigation.MainTabs
import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Transition
import com.jermey.quo.vadis.annotations.TransitionType
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Home tab - Main entry point and navigation patterns showcase.
 *
 * Icon: "home" (material icon name)
 */
@TabItem(parent = MainTabs::class, ordinal = 0)
@Destination(route = "main/home")
@Transition(type = TransitionType.Fade)
data object HomeTab : NavDestination

/**
 * Explore tab - Master-detail patterns and deep navigation.
 *
 * Uses a dedicated stack for the explore feature with Feed, Detail,
 * and CategoryView destinations for content discovery.
 *
 * Icon: "explore" (material icon name)
 */
@TabItem(parent = MainTabs::class, ordinal = 1)
@Stack(name = "exploreTabStack", startDestination = ExploreTab.Feed::class)
@Transition(type = TransitionType.Fade)
sealed class ExploreTab : NavDestination {
    /**
     * Main explore feed screen.
     */
    @Destination(route = "explore/feed")
    @Transition(type = TransitionType.Fade)
    data object Feed : ExploreTab()

    /**
     * Detail screen for a specific explore item.
     *
     * @property itemId The unique identifier of the item to display
     */
    @Destination(route = "explore/detail/{itemId}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class Detail(
        @Argument val itemId: String
    ) : ExploreTab()

    /**
     * Category view showing items filtered by category.
     *
     * @property category The category name to filter by
     */
    @Destination(route = "explore/category/{category}")
    @Transition(type = TransitionType.SlideHorizontal)
    data class CategoryView(
        @Argument val category: String
    ) : ExploreTab()
}

/**
 * Profile tab - User profile.
 *
 * Icon: "person" (material icon name)
 */
@TabItem(parent = MainTabs::class, ordinal = 2)
@Destination(route = "main/profile")
@Transition(type = TransitionType.Fade)
data object ProfileTab : NavDestination

/**
 * Settings tab - App configuration.
 *
 * Icon: "settings" (material icon name)
 */
@TabItem(parent = MainTabs::class, ordinal = 3)
@Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
@Transition(type = TransitionType.Fade)
sealed class SettingsTab : NavDestination {
    /** Root destination for the Settings tab. */

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
