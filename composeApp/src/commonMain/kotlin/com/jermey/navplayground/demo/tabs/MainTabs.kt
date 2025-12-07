package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.destinations.TabDestination
import com.jermey.quo.vadis.annotations.Destination as DestinationAnnotation
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.core.Destination

/**
 * Main bottom navigation tabs for the Quo Vadis demo app.
 *
 * This demonstrates the **@Tab annotation** for defining tabbed navigation
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
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : Destination {

    /**
     * Home tab - Main entry point and navigation patterns showcase.
     *
     * Root destination: TabDestination.Home
     * Icon: "home" (material icon name)
     */
    @TabItem(label = "Home", icon = "home", rootGraph = TabDestination::class)
    @DestinationAnnotation(route = "tabs/home")
    data object Home : MainTabs()

    /**
     * Explore tab - Master-detail patterns and deep navigation.
     *
     * Root destination: TabDestination.Explore
     * Icon: "explore" (material icon name)
     */
    @TabItem(label = "Explore", icon = "explore", rootGraph = TabDestination::class)
    @DestinationAnnotation(route = "tabs/explore")
    data object Explore : MainTabs()

    /**
     * Profile tab - User profile and settings.
     *
     * Root destination: TabDestination.Profile
     * Icon: "person" (material icon name)
     */
    @TabItem(label = "Profile", icon = "person", rootGraph = TabDestination::class)
    @DestinationAnnotation(route = "tabs/profile")
    data object Profile : MainTabs()

    /**
     * Settings tab - App configuration.
     *
     * Root destination: TabDestination.Settings
     * Icon: "settings" (material icon name)
     */
    @TabItem(label = "Settings", icon = "settings", rootGraph = TabDestination::class)
    @DestinationAnnotation(route = "tabs/settings")
    data object Settings : MainTabs()
}
