package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for tab state persistence in BackStackEntry extras.
 *
 * Verifies that:
 * - Tab selection is stored in BackStackEntry extras
 * - Tab selection is restored when entry is re-rendered
 * - Tab selection survives navigation away and back
 * - Tab selection works across TabNavigatorState recreation
 */
class TabStatePersistenceTest {

    // Test destinations
    private object HomeDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SettingsDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // Test tab definitions
    private object HomeTab : TabDefinition {
        override val route = "home_tab"
        override val label = "Home"
        override val icon = "home"
        override val rootDestination = HomeDestination
    }

    private object ProfileTab : TabDefinition {
        override val route = "profile_tab"
        override val label = "Profile"
        override val icon = "person"
        override val rootDestination = ProfileDestination
    }

    private object SettingsTab : TabDefinition {
        override val route = "settings_tab"
        override val label = "Settings"
        override val icon = "settings"
        override val rootDestination = SettingsDestination
    }

    // Parent destination for the entry that hosts the tab navigator
    private object TabHostDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    @Test
    fun `selectTab stores tab route in entry extras`() {
        // Given: TabNavigatorState with BackStackEntry
        val entry = BackStackEntry(destination = TabHostDestination)
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config, parentEntry = entry)

        // When: selectTab is called
        state.selectTab(ProfileTab)

        // Then: entry extras contain the tab route
        assertEquals(ProfileTab.route, entry.getExtra(EXTRA_SELECTED_TAB_ROUTE))
    }

    @Test
    fun `TabNavigatorState restores tab from entry extras`() {
        // Given: BackStackEntry with tab route in extras
        val entry = BackStackEntry(destination = TabHostDestination)
        entry.setExtra(EXTRA_SELECTED_TAB_ROUTE, ProfileTab.route)

        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )

        // When: TabNavigatorState is created with that entry
        val state = TabNavigatorState(config, parentEntry = entry)

        // Then: selectedTab matches the stored route
        assertEquals(ProfileTab, state.selectedTab.value)
    }

    @Test
    fun `TabNavigatorState uses initial tab when no extras`() {
        // Given: Empty BackStackEntry (no tab route in extras)
        val entry = BackStackEntry(destination = TabHostDestination)
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )

        // When: TabNavigatorState is created
        val state = TabNavigatorState(config, parentEntry = entry)

        // Then: selectedTab is initialTab from config
        assertEquals(HomeTab, state.selectedTab.value)
    }

    @Test
    fun `tab selection survives state recreation`() {
        // Given: TabNavigatorState with selected non-default tab
        val entry = BackStackEntry(destination = TabHostDestination)
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )

        val state1 = TabNavigatorState(config, parentEntry = entry)
        state1.selectTab(SettingsTab)

        // Verify the tab was stored in extras
        assertEquals(SettingsTab.route, entry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // When: New TabNavigatorState created with same entry (simulates recomposition)
        val state2 = TabNavigatorState(config, parentEntry = entry)

        // Then: Same tab is selected (restored from entry extras)
        assertEquals(SettingsTab, state2.selectedTab.value)
    }

    @Test
    fun `tab selection persists through multiple switches`() {
        // Given: TabNavigatorState with BackStackEntry
        val entry = BackStackEntry(destination = TabHostDestination)
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config, parentEntry = entry)

        // When: Multiple tab switches occur
        state.selectTab(ProfileTab)
        assertEquals(ProfileTab.route, entry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        state.selectTab(SettingsTab)
        assertEquals(SettingsTab.route, entry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        state.selectTab(HomeTab)

        // Then: The latest tab is stored in extras
        assertEquals(HomeTab.route, entry.getExtra(EXTRA_SELECTED_TAB_ROUTE))
    }

    @Test
    fun `TabNavigatorState without parent entry does not throw`() {
        // Given: TabNavigatorConfig with no parent entry
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )

        // When: TabNavigatorState is created without parent entry
        val state = TabNavigatorState(config, parentEntry = null)

        // Then: State works normally, defaults to initial tab
        assertEquals(HomeTab, state.selectedTab.value)

        // And: Tab selection works without error (even though nothing is persisted)
        state.selectTab(ProfileTab)
        assertEquals(ProfileTab, state.selectedTab.value)
    }

    @Test
    fun `unknown tab route in extras falls back to initial tab`() {
        // Given: BackStackEntry with invalid tab route in extras
        val entry = BackStackEntry(destination = TabHostDestination)
        entry.setExtra(EXTRA_SELECTED_TAB_ROUTE, "non_existent_tab")

        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )

        // When: TabNavigatorState is created with that entry
        val state = TabNavigatorState(config, parentEntry = entry)

        // Then: selectedTab falls back to initialTab (unknown route is ignored)
        assertEquals(HomeTab, state.selectedTab.value)
    }

    @Test
    fun `tab state simulates navigation away and back scenario`() {
        // This test simulates the scenario where:
        // 1. User is on tab screen, selects a non-default tab
        // 2. User navigates to another screen (entry stays in backstack)
        // 3. User navigates back (TabNavigatorState recreated with same entry)
        // 4. The previously selected tab should be restored

        // Given: Initial tab screen with user switching tabs
        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )

        // User on tab screen, switches to SettingsTab
        val initialTabState = TabNavigatorState(config, parentEntry = tabHostEntry)
        initialTabState.selectTab(SettingsTab)

        // Verify state is stored
        assertEquals(SettingsTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // Simulate: TabNavigatorState is disposed when navigating away
        // (the entry remains in the backstack with its extras intact)

        // When: User navigates back, TabNavigatorState is recreated with same entry
        val restoredTabState = TabNavigatorState(config, parentEntry = tabHostEntry)

        // Then: The SettingsTab should be selected (restored from entry extras)
        assertEquals(SettingsTab, restoredTabState.selectedTab.value)
    }
}
