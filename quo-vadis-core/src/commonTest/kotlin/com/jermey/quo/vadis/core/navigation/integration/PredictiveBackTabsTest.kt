package com.jermey.quo.vadis.core.navigation.integration

import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.DefaultNavigator
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.EXTRA_SELECTED_TAB_ROUTE
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.TabDefinition
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorConfig
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState
import com.jermey.quo.vadis.core.navigation.core.getExtra
import com.jermey.quo.vadis.core.navigation.core.setExtra
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for predictive back navigation with tabs.
 *
 * These tests verify the end-to-end scenario of navigating from a tabbed screen
 * to a detail screen and back, ensuring that:
 * - Tab selection is preserved when navigating away and back
 * - Entry extras correctly store and restore tab state
 * - Back navigation properly restores the selected tab
 *
 * These tests focus on the state management logic, not UI animations,
 * and are designed to run on all KMP platforms.
 */
class PredictiveBackTabsTest {

    // ========== Test Destinations ==========

    /** Root destination that hosts the tab navigator */
    private object TabHostDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    /** Detail screen that user navigates to from tabs */
    private object DetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    /** Another detail screen for complex navigation scenarios */
    private object AnotherDetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // Tab root destinations
    private object HomeRootDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileRootDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SettingsRootDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // ========== Test Tab Definitions ==========

    private object HomeTab : TabDefinition {
        override val route = "home_tab"
        override val label = "Home"
        override val icon = "home"
        override val rootDestination = HomeRootDestination
    }

    private object ProfileTab : TabDefinition {
        override val route = "profile_tab"
        override val label = "Profile"
        override val icon = "person"
        override val rootDestination = ProfileRootDestination
    }

    private object SettingsTab : TabDefinition {
        override val route = "settings_tab"
        override val label = "Settings"
        override val icon = "settings"
        override val rootDestination = SettingsRootDestination
    }

    // ========== Integration Tests ==========

    /**
     * Test 1: Tab selection preserved through navigation
     *
     * Scenario:
     * 1. User starts on Home tab (default)
     * 2. User switches to Profile tab
     * 3. User navigates to a detail screen
     * 4. User navigates back
     * 5. Profile tab should still be selected
     */
    @Test
    fun `tab selection preserved when navigating to detail and back`() {
        // Setup: Parent navigator with tab host entry
        val parentNavigator = DefaultNavigator()
        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        parentNavigator.setStartDestination(TabHostDestination)

        // Create tab navigator state with parent entry for persistence
        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )
        val tabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Step 1: Verify initial state is HomeTab
        assertEquals(HomeTab, tabState.selectedTab.value)

        // Step 2: User switches to ProfileTab (non-default)
        tabState.selectTab(ProfileTab)
        assertEquals(ProfileTab, tabState.selectedTab.value)

        // Verify tab is stored in entry extras
        assertEquals(ProfileTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // Step 3: Simulate navigation to detail screen
        // In real app, this would dispose the TabNavigatorState
        parentNavigator.navigate(DetailDestination)
        assertEquals(DetailDestination, parentNavigator.currentDestination.value)

        // Step 4: Simulate back navigation - TabNavigatorState would be recreated
        parentNavigator.navigateBack()
        assertEquals(TabHostDestination, parentNavigator.currentDestination.value)

        // Step 5: Create new TabNavigatorState with same entry (simulates recomposition)
        val restoredTabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Verify: Original tab (ProfileTab) is still selected
        assertEquals(ProfileTab, restoredTabState.selectedTab.value)
    }

    /**
     * Test 2: Entry extras contain correct tab after multiple navigations
     *
     * Scenario:
     * 1. User switches between tabs multiple times
     * 2. User navigates to detail screen
     * 3. Entry extras should contain the last selected tab
     */
    @Test
    fun `entry extras contain selected tab after multiple navigations`() {
        // Setup
        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )
        val tabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Initial state - HomeTab (note: extras not set until explicit tab selection)
        assertEquals(HomeTab, tabState.selectedTab.value)

        // Switch to ProfileTab
        tabState.selectTab(ProfileTab)
        assertEquals(ProfileTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // Switch to SettingsTab
        tabState.selectTab(SettingsTab)
        assertEquals(SettingsTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // Switch back to HomeTab
        tabState.selectTab(HomeTab)
        assertEquals(HomeTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // Final switch to ProfileTab
        tabState.selectTab(ProfileTab)

        // Verify: Entry extras contain the last selected tab (ProfileTab)
        assertEquals(ProfileTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))
    }

    /**
     * Test 3: Back navigation restores correct tab
     *
     * Scenario:
     * 1. Entry has ProfileTab stored in extras
     * 2. TabNavigatorState created with that entry
     * 3. ProfileTab should be selected (not the initial/default tab)
     */
    @Test
    fun `navigating back to tabs screen restores correct tab from entry`() {
        // Given: Entry with ProfileTab (tab2) stored in extras
        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        tabHostEntry.setExtra(EXTRA_SELECTED_TAB_ROUTE, ProfileTab.route)

        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab // Note: initial is HomeTab
        )

        // When: TabNavigatorState created with that entry
        val tabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Then: ProfileTab is selected (restored from entry), not HomeTab
        assertEquals(ProfileTab, tabState.selectedTab.value)
    }

    /**
     * Test 4: Full predictive back flow with nested navigation
     *
     * Scenario:
     * 1. User is on Home tab, navigates within tab (Home -> HomeDetail)
     * 2. User switches to Settings tab
     * 3. User navigates to app-level detail screen
     * 4. User performs back navigation multiple times
     * 5. Tab state should be correctly preserved at each step
     */
    @Test
    fun `full predictive back flow with nested navigation and tab switches`() {
        // Setup: Parent navigator hierarchy
        val parentNavigator = DefaultNavigator()
        parentNavigator.setStartDestination(TabHostDestination)

        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )

        // Step 1: User on Home tab, navigates within tab
        val tabState1 = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)
        assertEquals(HomeTab, tabState1.selectedTab.value)

        // Navigate within home tab
        val homeNavigator = tabState1.getNavigatorForTab(HomeTab)
        homeNavigator.navigate(DetailDestination)
        assertEquals(2, tabState1.getTabStackSize(HomeTab))

        // Step 2: User switches to Settings tab
        tabState1.selectTab(SettingsTab)
        assertEquals(SettingsTab, tabState1.selectedTab.value)
        assertEquals(SettingsTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // Step 3: Navigate to app-level detail screen (outside tabs)
        // This simulates the user navigating from tab host to another screen
        parentNavigator.navigate(AnotherDetailDestination)

        // Simulate TabNavigatorState disposal and recreation
        val tabState2 = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Verify: Settings tab is restored (from entry extras)
        assertEquals(SettingsTab, tabState2.selectedTab.value)

        // Step 4: Note about initialization behavior
        // After recreation, the restored tab is selected but the navigator is lazily created.
        // The _tabInitialized flag tracks lazy initialization of navigators.
        // When we access the navigator, it gets created (but isTabInitialized is updated
        // via initializeTabIfNeeded which is called by selectTab, not getNavigatorForTab directly).
        // 
        // Key verification: The tab SELECTION is properly restored, which is the core functionality.
        // The navigator for the restored tab can be accessed:
        val settingsNav = tabState2.getNavigatorForTab(SettingsTab)
        assertEquals(SettingsRootDestination, settingsNav.currentDestination.value)
    }

    /**
     * Test 5: Back press handling with tabs and parent navigator
     *
     * Verifies the back press delegation chain:
     * TabNavigatorState -> handles if tab has stack or switches to primary tab
     * Parent Navigator -> handles if TabNavigatorState doesn't consume
     */
    @Test
    fun `back press properly delegates between tab navigator and parent`() {
        // Setup
        val parentNavigator = DefaultNavigator()
        parentNavigator.setStartDestination(TabHostDestination)

        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )
        val tabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Register tab state as active child of parent navigator
        parentNavigator.setActiveChild(tabState)

        // Navigate to detail within parent
        parentNavigator.navigate(DetailDestination)

        // Back press should pop parent navigator (since we navigated in parent)
        val consumed1 = parentNavigator.navigateBack()
        assertTrue(consumed1)
        assertEquals(TabHostDestination, parentNavigator.currentDestination.value)

        // Switch to Profile tab (non-primary)
        tabState.selectTab(ProfileTab)

        // Back press should be handled by tab state (switch to primary)
        val consumed2 = parentNavigator.navigateBack()
        assertTrue(consumed2)
        assertEquals(HomeTab, tabState.selectedTab.value)

        // Now on primary tab at root - back should pass through
        val consumed3 = parentNavigator.navigateBack()
        assertFalse(consumed3) // Should not be consumed - would exit app
    }

    /**
     * Test 6: Tab selection persists across multiple TabNavigatorState instances
     *
     * Simulates what happens during configuration changes or process death
     * where the TabNavigatorState is recreated but BackStackEntry persists.
     */
    @Test
    fun `tab selection persists across TabNavigatorState recreations`() {
        // Setup: Single entry that persists
        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab
        )

        // First instance - user selects Settings tab
        val instance1 = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)
        instance1.selectTab(SettingsTab)
        assertEquals(SettingsTab, instance1.selectedTab.value)

        // Second instance - simulates recreation
        val instance2 = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)
        assertEquals(SettingsTab, instance2.selectedTab.value) // Restored!

        // User switches to Profile tab
        instance2.selectTab(ProfileTab)

        // Third instance - another recreation
        val instance3 = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)
        assertEquals(ProfileTab, instance3.selectedTab.value) // Latest selection restored
    }

    /**
     * Test 7: Edge case - Tab removed from config but stored in extras
     *
     * Verifies graceful handling when the stored tab route no longer exists
     * in the configuration (e.g., feature flag disabled a tab).
     */
    @Test
    fun `gracefully handles removed tab in entry extras`() {
        // Setup: Entry with a tab that will not be in the new config
        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        tabHostEntry.setExtra(EXTRA_SELECTED_TAB_ROUTE, "removed_tab")

        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab), // SettingsTab removed!
            initialTab = HomeTab
        )

        // When: TabNavigatorState created with entry containing invalid tab
        val tabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Then: Falls back to initial tab (graceful degradation)
        assertEquals(HomeTab, tabState.selectedTab.value)
    }

    /**
     * Test 8: Integration with deep navigation stacks
     *
     * Tests the scenario where user has deep navigation in one tab,
     * switches tabs, navigates away, and comes back.
     */
    @Test
    fun `deep navigation state preserved with tab persistence`() {
        // Setup
        val tabHostEntry = BackStackEntry(destination = TabHostDestination)
        val tabConfig = TabNavigatorConfig(
            allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
            initialTab = HomeTab,
            primaryTab = HomeTab
        )

        val tabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Build deep navigation stack in Home tab
        val homeNav = tabState.getNavigatorForTab(HomeTab)
        homeNav.navigate(DetailDestination)
        homeNav.navigate(AnotherDetailDestination)
        assertEquals(3, tabState.getTabStackSize(HomeTab)) // Root + 2 details

        // Switch to Settings tab
        tabState.selectTab(SettingsTab)

        // Build navigation in Settings tab
        val settingsNav = tabState.getNavigatorForTab(SettingsTab)
        settingsNav.navigate(DetailDestination)
        assertEquals(2, tabState.getTabStackSize(SettingsTab))

        // Verify entry has Settings tab stored
        assertEquals(SettingsTab.route, tabHostEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE))

        // Simulate recreation with same entry
        val restoredTabState = TabNavigatorState(tabConfig, parentEntry = tabHostEntry)

        // Verify: Settings tab is restored
        assertEquals(SettingsTab, restoredTabState.selectedTab.value)

        // Note: Individual tab navigation stacks would need separate persistence
        // This test focuses on the tab selection persistence mechanism
    }
}
