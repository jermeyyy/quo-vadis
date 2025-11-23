package com.jermey.quo.vadis.core.navigation.testing

import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TabDefinition
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorConfig
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * Fake implementation of tab navigation for testing.
 *
 * Provides a simple way to test tab navigation logic without needing
 * to set up full navigation infrastructure. Records all navigation operations
 * for verification in tests.
 *
 * Example usage:
 * ```kotlin
 * val config = TabNavigatorConfig(
 *     allTabs = listOf(HomeTab, ProfileTab),
 *     initialTab = HomeTab
 * )
 * val fake = FakeTabNavigator(config)
 *
 * // Perform navigation
 * fake.selectTab(ProfileTab)
 *
 * // Verify
 * assertEquals(ProfileTab, fake.currentTab)
 * assertTrue(fake.tabSelections.contains(ProfileTab))
 * ```
 */
class FakeTabNavigator(
    config: TabNavigatorConfig
) {
    private val state = TabNavigatorState(config)
    
    /**
     * Record of all tab selections made during testing.
     */
    val tabSelections = mutableListOf<TabDefinition>()
    
    /**
     * Record of all back press events and their results.
     */
    val backPressResults = mutableListOf<Boolean>()
    
    /**
     * Currently selected tab.
     */
    val currentTab: TabDefinition
        get() = state.selectedTab.value
    
    /**
     * All configured tabs.
     */
    val allTabs: List<TabDefinition>
        get() = state.getAllTabs()
    
    /**
     * Select a tab.
     */
    fun selectTab(tab: TabDefinition) {
        tabSelections.add(tab)
        state.selectTab(tab)
    }
    
    /**
     * Perform a back press.
     * @return true if consumed, false if passed to parent.
     */
    fun onBack(): Boolean {
        val result = state.onBack()
        backPressResults.add(result)
        return result
    }
    
    /**
     * Get the navigator for a specific tab.
     */
    fun getNavigatorForTab(tab: TabDefinition): Navigator {
        return state.getNavigatorForTab(tab)
    }
    
    /**
     * Get the current tab's navigator.
     */
    fun getCurrentNavigator(): Navigator {
        return state.getCurrentNavigator()
    }
    
    /**
     * Check if a tab has been initialized.
     */
    fun isTabInitialized(tab: TabDefinition): Boolean {
        return state.isTabInitialized(tab)
    }
    
    /**
     * Get the backstack size for a tab.
     */
    fun getTabStackSize(tab: TabDefinition): Int {
        return state.getTabStackSize(tab)
    }
    
    /**
     * Reset all recorded actions (for test isolation).
     */
    fun reset() {
        tabSelections.clear()
        backPressResults.clear()
    }
}
