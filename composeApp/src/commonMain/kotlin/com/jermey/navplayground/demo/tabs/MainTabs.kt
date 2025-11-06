package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.destinations.TabDestination
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabGraph
import com.jermey.quo.vadis.core.navigation.core.TabDefinition

/**
 * Main bottom navigation tabs for the Quo Vadis demo app.
 *
 * This demonstrates the **@TabGraph annotation** for defining tabbed navigation
 * with automatic code generation.
 *
 * ## Generated Code
 *
 * KSP processes this annotation and generates:
 * - `MainTabsConfig`: TabNavigatorConfig with tab definitions
 * - `MainTabsContainer`: @Composable container function
 *
 * ## Features Demonstrated
 *
 * 1. **Independent Navigation Stacks**: Each tab maintains its own back stack
 * 2. **State Preservation**: Content state preserved when switching tabs
 * 3. **Smart Back Press**: Hierarchical back navigation across tabs
 * 4. **Type Safety**: Compile-time checked tab definitions
 *
 * ## Tab Behavior
 *
 * - **Home**: Primary tab, navigation patterns showcase
 * - **Explore**: Master-detail with deep navigation
 * - **Profile**: Profile management flows
 * - **Settings**: App configuration
 *
 * Back press behavior:
 * 1. Pop from current tab's stack (if not at root)
 * 2. Switch to Home tab (if on another tab)
 * 3. Exit app (if on Home tab at root)
 *
 * @see MainTabsScreen for the UI implementation
 */
@TabGraph(
    name = "main_tabs",
    initialTab = "Home",
    primaryTab = "Home"
)
sealed class MainTabs : TabDefinition {

    /**
     * Home tab - Main entry point and navigation patterns showcase.
     *
     * Root destination: TabDestination.Home
     * Icon: "home" (material icon name)
     */
    @Tab(
        route = "tab_home",
        label = "Home",
        icon = "home",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Home::class
    )
    data object Home : MainTabs() {
        override val route = "tab_home"
        override val rootDestination = TabDestination.Home
    }

    /**
     * Explore tab - Master-detail patterns and deep navigation.
     *
     * Root destination: TabDestination.Explore
     * Icon: "explore" (material icon name)
     */
    @Tab(
        route = "tab_explore",
        label = "Explore",
        icon = "explore",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Explore::class
    )
    data object Explore : MainTabs() {
        override val route = "tab_explore"
        override val rootDestination = TabDestination.Explore
    }

    /**
     * Profile tab - User profile and settings.
     *
     * Root destination: TabDestination.Profile
     * Icon: "person" (material icon name)
     */
    @Tab(
        route = "tab_profile",
        label = "Profile",
        icon = "person",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Profile::class
    )
    data object Profile : MainTabs() {
        override val route = "tab_profile"
        override val rootDestination = TabDestination.Profile
    }

    /**
     * Settings tab - App configuration.
     *
     * Root destination: TabDestination.Settings
     * Icon: "settings" (material icon name)
     */
    @Tab(
        route = "tab_settings",
        label = "Settings",
        icon = "settings",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Settings::class
    )
    data object Settings : MainTabs() {
        override val route = "tab_settings"
        override val rootDestination = TabDestination.Settings
    }
}
