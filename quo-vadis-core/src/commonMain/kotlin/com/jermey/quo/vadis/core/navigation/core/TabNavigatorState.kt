package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Manages the state for tabbed navigation.
 *
 * This class handles:
 * - Tab selection and switching
 * - Independent navigation stacks per tab
 * - Intelligent back press behavior
 * - State preservation across tab switches
 *
 * Thread-safe and reactive via StateFlow.
 *
 * Example usage:
 * ```kotlin
 * val config = TabNavigatorConfig(
 *     allTabs = listOf(HomeTab, ProfileTab),
 *     initialTab = HomeTab
 * )
 * val state = TabNavigatorState(config)
 *
 * // Switch tabs
 * state.selectTab(ProfileTab)
 *
 * // Check current tab
 * val current = state.selectedTab.value
 *
 * // Get a tab's navigator
 * val homeNav = state.getNavigatorForTab(HomeTab)
 * ```
 */
class TabNavigatorState(
    val config: TabNavigatorConfig
) : BackPressHandler {
    
    /**
     * Currently selected tab.
     */
    private val _selectedTab = MutableStateFlow(config.initialTab)
    val selectedTab: StateFlow<TabDefinition> = _selectedTab.asStateFlow()
    
    /**
     * Map of tab definitions to their navigators.
     * Each tab maintains its own independent Navigator instance.
     */
    private val _tabNavigators = mutableMapOf<TabDefinition, Navigator>()
    
    /**
     * Tracks whether each tab has been initialized (first visit).
     * Used to lazily initialize tab navigators and their root destinations.
     */
    private val _tabInitialized = mutableMapOf<TabDefinition, Boolean>()
    
    init {
        // Initialize all tabs as not initialized
        config.allTabs.forEach { tab ->
            _tabInitialized[tab] = false
        }
        
        // Initialize the initial tab immediately
        initializeTabIfNeeded(config.initialTab)
    }
    
    /**
     * Get the Navigator for a specific tab.
     * Creates the navigator if it doesn't exist yet.
     *
     * @param tab The tab to get the navigator for.
     * @return The Navigator instance for this tab.
     */
    fun getNavigatorForTab(tab: TabDefinition): Navigator {
        require(tab in config.allTabs) { "Tab $tab is not in the configured tabs" }
        
        return _tabNavigators.getOrPut(tab) {
            DefaultNavigator().apply {
                // Set the root destination for this tab
                setStartDestination(tab.rootDestination)
            }
        }
    }
    
    /**
     * Get the currently active tab's navigator.
     *
     * @return The Navigator for the currently selected tab.
     */
    fun getCurrentNavigator(): Navigator {
        return getNavigatorForTab(_selectedTab.value)
    }
    
    /**
     * Select a tab, switching the active navigation context.
     *
     * This preserves the current tab's navigation state and
     * restores the target tab's state.
     *
     * @param tab The tab to select.
     */
    fun selectTab(tab: TabDefinition) {
        require(tab in config.allTabs) { "Tab $tab is not in the configured tabs" }
        
        if (_selectedTab.value == tab) {
            // Already on this tab, do nothing
            return
        }
        
        // Initialize the tab if this is the first visit
        initializeTabIfNeeded(tab)
        
        // Switch to the new tab
        _selectedTab.update { tab }
    }
    
    /**
     * Initialize a tab's navigator if it hasn't been initialized yet.
     */
    private fun initializeTabIfNeeded(tab: TabDefinition) {
        if (_tabInitialized[tab] == true) {
            return // Already initialized
        }
        
        // Get or create the navigator (which sets up the root destination)
        getNavigatorForTab(tab)
        
        // Mark as initialized
        _tabInitialized[tab] = true
    }
    
    /**
     * Handle back press with intelligent tab navigation logic.
     *
     * Logic:
     * 1. If current tab's stack has > 1 entry → pop from tab stack (CONSUMED)
     * 2. Else if not on primary tab → switch to primary tab (CONSUMED)
     * 3. Else on primary tab with 1 entry → pass to parent (NOT CONSUMED)
     *
     * @return `true` if the back press was consumed, `false` if it should bubble to parent.
     */
    @Suppress("ReturnCount")
    override fun onBack(): Boolean {
        val currentTab = _selectedTab.value
        val currentNavigator = getCurrentNavigator()
        
        // Check if we can navigate back in the current tab
        // Only try to navigate back if the stack has more than 1 entry (can actually go back)
        if (currentNavigator.backStack.canGoBack.value) {
            currentNavigator.navigateBack()
            return true // Consumed by tab's navigator
        }
        
        // Tab stack is at root (can't pop), check if we should switch to primary tab
        if (currentTab != config.primaryTab) {
            // Not on primary tab, switch to it
            selectTab(config.primaryTab)
            return true // Consumed by switching tabs
        }
        
        // On primary tab at root, pass to parent
        return false
    }
    
    /**
     * Get all tab definitions.
     *
     * @return List of all configured tabs.
     */
    fun getAllTabs(): List<TabDefinition> = config.allTabs
    
    /**
     * Check if a tab has been visited (initialized).
     *
     * @param tab The tab to check.
     * @return `true` if the tab has been visited, `false` otherwise.
     */
    fun isTabInitialized(tab: TabDefinition): Boolean {
        return _tabInitialized[tab] == true
    }
    
    /**
     * Get the current backstack size for a tab.
     * Useful for debugging or UI indicators.
     *
     * @param tab The tab to check.
     * @return Number of destinations in the tab's stack.
     */
    fun getTabStackSize(tab: TabDefinition): Int {
        if (!isTabInitialized(tab)) {
            return 0
        }
        return getNavigatorForTab(tab).backStack.stack.value.size
    }
}
