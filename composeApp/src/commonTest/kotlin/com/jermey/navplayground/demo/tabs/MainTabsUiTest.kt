package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.ExploreTab
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.HomeTab
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.ProfileTab
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.SettingsTab
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTabsUiTest {

    @Test
    fun `resolveMainTabKind maps top level tab destinations`() {
        assertEquals(MainTabKind.Home, resolveMainTabKind(HomeTab))
        assertEquals(MainTabKind.Profile, resolveMainTabKind(ProfileTab))
    }

    @Test
    fun `resolveMainTabKind maps stack-backed tab start destinations`() {
        assertEquals(MainTabKind.Explore, resolveMainTabKind(ExploreTab.Feed))
        assertEquals(MainTabKind.Settings, resolveMainTabKind(SettingsTab.Main))
    }

    @Test
    fun `resolveMainTabKind keeps nested stack destinations associated with their parent tab`() {
        assertEquals(MainTabKind.Explore, resolveMainTabKind(ExploreTab.Detail("item-1")))
        assertEquals(MainTabKind.Settings, resolveMainTabKind(SettingsTab.Notifications))
    }

    @Test
    fun `resolveMainTabKind falls back to unknown for unrelated destinations`() {
        assertEquals(MainTabKind.Unknown, resolveMainTabKind(UnknownDestination))
    }
}

private data object UnknownDestination : NavDestination