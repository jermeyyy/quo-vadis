package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MainTabs
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTabsUiTest {

    @Test
    fun `resolveMainTabKind maps top level tab destinations`() {
        assertEquals(MainTabKind.Home, resolveMainTabKind(MainTabs.HomeTab))
        assertEquals(MainTabKind.Profile, resolveMainTabKind(MainTabs.ProfileTab))
    }

    @Test
    fun `resolveMainTabKind maps stack-backed tab start destinations`() {
        assertEquals(MainTabKind.Explore, resolveMainTabKind(MainTabs.ExploreTab.Feed))
        assertEquals(MainTabKind.Settings, resolveMainTabKind(MainTabs.SettingsTab.Main))
    }

    @Test
    fun `resolveMainTabKind keeps nested stack destinations associated with their parent tab`() {
        assertEquals(MainTabKind.Explore, resolveMainTabKind(MainTabs.ExploreTab.Detail("item-1")))
        assertEquals(MainTabKind.Settings, resolveMainTabKind(MainTabs.SettingsTab.Notifications))
    }

    @Test
    fun `resolveMainTabKind falls back to unknown for unrelated destinations`() {
        assertEquals(MainTabKind.Unknown, resolveMainTabKind(UnknownDestination))
    }
}

private data object UnknownDestination : NavDestination