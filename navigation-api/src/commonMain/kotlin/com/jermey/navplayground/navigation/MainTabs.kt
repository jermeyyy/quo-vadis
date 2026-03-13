package com.jermey.navplayground.navigation

import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Main bottom navigation tabs container for the Quo Vadis demo app.
 *
 * This is the parent container for all main tabs. Individual tab items
 * declare their membership via `@TabItem(parent = MainTabs::class, ordinal = N)`.
 *
 * Tab items can be defined in any module that depends on this module,
 * enabling cross-module tab composition.
 *
 * @see com.jermey.quo.vadis.annotations.TabItem
 */
@Tabs(name = "mainTabs")
class MainTabs : NavDestination {
    companion object : NavDestination
}
