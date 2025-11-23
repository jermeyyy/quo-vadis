package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.navigation.testing.FakeTabNavigator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Comprehensive tests for TabNavigatorState.
 *
 * Tests cover:
 * - Tab selection and switching
 * - Independent navigation stacks per tab
 * - Intelligent back press behavior
 * - State preservation across tab switches
 * - Configuration validation
 * - Lazy initialization
 */
class TabNavigatorStateTest {

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

    private object ExploreDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // Additional destinations for navigation within tabs
    private object HomeDetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileEditDestination : Destination {
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

    private object ExploreTab : TabDefinition {
        override val route = "explore_tab"
        override val label = "Explore"
        override val icon = "explore"
        override val rootDestination = ExploreDestination
    }

    @Test
    fun `initial tab is selected on creation`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        assertEquals(HomeTab, state.selectedTab.value)
    }

    @Test
    fun `initial tab navigator is initialized on creation`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        assertTrue(state.isTabInitialized(HomeTab))
        assertFalse(state.isTabInitialized(ProfileTab))
    }

    @Test
    fun `selectTab switches to target tab`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        state.selectTab(ProfileTab)

        assertEquals(ProfileTab, state.selectedTab.value)
    }

    @Test
    fun `selectTab initializes tab on first visit`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        assertFalse(state.isTabInitialized(ProfileTab))

        state.selectTab(ProfileTab)

        assertTrue(state.isTabInitialized(ProfileTab))
    }

    @Test
    fun `selectTab does nothing when already on target tab`() {
        val fake = FakeTabNavigator(
            TabNavigatorConfig(
                allTabs = listOf(HomeTab, ProfileTab),
                initialTab = HomeTab
            )
        )

        fake.selectTab(HomeTab)

        assertEquals(1, fake.tabSelections.size)
        assertEquals(HomeTab, fake.currentTab)
    }

    @Test
    fun `selectTab throws when tab not in config`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        assertFailsWith<IllegalArgumentException> {
            state.selectTab(SettingsTab) // Not in config
        }
    }

    @Test
    fun `each tab maintains independent navigator`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        val homeNav = state.getNavigatorForTab(HomeTab)
        val profileNav = state.getNavigatorForTab(ProfileTab)

        assertTrue(homeNav !== profileNav)
    }

    @Test
    fun `tab navigators maintain independent stacks`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        // Navigate in home tab
        val homeNav = state.getNavigatorForTab(HomeTab)
        homeNav.navigate(HomeDetailDestination)

        // Switch to profile tab and navigate
        state.selectTab(ProfileTab)
        val profileNav = state.getNavigatorForTab(ProfileTab)
        profileNav.navigate(ProfileEditDestination)

        // Verify stacks are independent
        assertEquals(2, state.getTabStackSize(HomeTab)) // Root + Detail
        assertEquals(2, state.getTabStackSize(ProfileTab)) // Root + Edit
    }

    @Test
    fun `switching tabs preserves previous tab state`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        // Navigate in home tab
        val homeNav = state.getNavigatorForTab(HomeTab)
        homeNav.navigate(HomeDetailDestination)
        assertEquals(2, state.getTabStackSize(HomeTab))

        // Switch to profile
        state.selectTab(ProfileTab)

        // Switch back to home
        state.selectTab(HomeTab)

        // Home stack should be preserved
        assertEquals(2, state.getTabStackSize(HomeTab))
        assertEquals(HomeDetailDestination, homeNav.currentDestination.value)
    }

    @Test
    fun `onBack pops from current tab stack when stack has multiple entries`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )
        val state = TabNavigatorState(config)

        // Navigate deeper in home tab
        val homeNav = state.getNavigatorForTab(HomeTab)
        homeNav.navigate(HomeDetailDestination)
        assertEquals(2, state.getTabStackSize(HomeTab))

        // Back press should pop from stack
        val consumed = state.onBack()

        assertTrue(consumed)
        assertEquals(1, state.getTabStackSize(HomeTab))
        assertEquals(HomeDestination, homeNav.currentDestination.value)
    }

    @Test
    fun `onBack switches to primary tab when on non-primary tab at root`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )
        val state = TabNavigatorState(config)

        // Switch to profile (non-primary)
        state.selectTab(ProfileTab)
        assertEquals(ProfileTab, state.selectedTab.value)

        // Back press should switch to primary tab
        val consumed = state.onBack()

        assertTrue(consumed)
        assertEquals(HomeTab, state.selectedTab.value)
    }

    @Test
    fun `onBack returns false when on primary tab at root`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )
        val state = TabNavigatorState(config)

        // Already on primary tab at root
        assertEquals(HomeTab, state.selectedTab.value)
        assertEquals(1, state.getTabStackSize(HomeTab))

        // Back press should not be consumed (pass to parent)
        val consumed = state.onBack()

        assertFalse(consumed)
    }

    @Test
    fun `onBack complex scenario - multi-level navigation then tab switches`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )
        val state = TabNavigatorState(config)

        // Navigate deep in home
        val homeNav = state.getNavigatorForTab(HomeTab)
        homeNav.navigate(HomeDetailDestination)

        // Switch to profile and navigate
        state.selectTab(ProfileTab)
        val profileNav = state.getNavigatorForTab(ProfileTab)
        profileNav.navigate(ProfileEditDestination)

        // Back press 1: Should pop from profile stack
        assertTrue(state.onBack())
        assertEquals(ProfileTab, state.selectedTab.value)
        assertEquals(1, state.getTabStackSize(ProfileTab))

        // Back press 2: Should switch to home (primary)
        assertTrue(state.onBack())
        assertEquals(HomeTab, state.selectedTab.value)

        // Back press 3: Should pop from home stack
        assertTrue(state.onBack())
        assertEquals(1, state.getTabStackSize(HomeTab))

        // Back press 4: Should pass to parent (at primary root)
        assertFalse(state.onBack())
    }

    @Test
    fun `getCurrentNavigator returns navigator for selected tab`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        val homeNav = state.getCurrentNavigator()
        assertEquals(HomeDestination, homeNav.currentDestination.value)

        state.selectTab(ProfileTab)
        val profileNav = state.getCurrentNavigator()
        assertEquals(ProfileDestination, profileNav.currentDestination.value)

        assertTrue(homeNav !== profileNav)
    }

    @Test
    fun `getAllTabs returns all configured tabs`() {
        val allTabs = listOf(HomeTab, ProfileTab, SettingsTab)
        val config = TabNavigatorConfig(
            allTabs = allTabs,
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        assertEquals(allTabs, state.getAllTabs())
    }

    @Test
    fun `getTabStackSize returns 0 for uninitialized tab`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        assertEquals(0, state.getTabStackSize(ProfileTab))
    }

    @Test
    fun `getTabStackSize returns correct size for initialized tab`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = HomeTab
        )
        val state = TabNavigatorState(config)

        assertEquals(1, state.getTabStackSize(HomeTab)) // Root only

        val homeNav = state.getNavigatorForTab(HomeTab)
        homeNav.navigate(HomeDetailDestination)

        assertEquals(2, state.getTabStackSize(HomeTab)) // Root + Detail
    }

    // Configuration validation tests
    @Test
    fun `config requires non-empty allTabs`() {
        assertFailsWith<IllegalArgumentException> {
            TabNavigatorConfig(
                allTabs = emptyList(),
                initialTab = HomeTab
            )
        }
    }

    @Test
    fun `config requires initialTab in allTabs`() {
        assertFailsWith<IllegalArgumentException> {
            TabNavigatorConfig(
                allTabs = listOf(ProfileTab, SettingsTab),
                initialTab = HomeTab // Not in allTabs
            )
        }
    }

    @Test
    fun `config requires primaryTab in allTabs`() {
        assertFailsWith<IllegalArgumentException> {
            TabNavigatorConfig(
                allTabs = listOf(HomeTab, ProfileTab),
                initialTab = HomeTab,
                primaryTab = SettingsTab // Not in allTabs
            )
        }
    }

    @Test
    fun `config requires unique tab routes`() {
        val duplicateTab = object : TabDefinition {
            override val route = "home_tab" // Same as HomeTab
            override val rootDestination = SettingsDestination
        }

        assertFailsWith<IllegalArgumentException> {
            TabNavigatorConfig(
                allTabs = listOf(HomeTab, duplicateTab),
                initialTab = HomeTab
            )
        }
    }

    @Test
    fun `config defaults primaryTab to initialTab`() {
        val config = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab),
            initialTab = ProfileTab
        )

        assertEquals(ProfileTab, config.primaryTab)
    }
}
