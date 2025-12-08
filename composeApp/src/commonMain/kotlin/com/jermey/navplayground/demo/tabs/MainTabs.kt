package com.jermey.navplayground.demo.tabs

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface

/**
 * Main bottom navigation tabs for the Quo Vadis demo app.
 *
 * This demonstrates the **new @Tab annotation pattern** for defining tabbed navigation
 * with automatic code generation. Each tab is a top-level class with @TabItem + @Stack.
 *
 * ## New Pattern
 *
 * Each tab is defined as a separate @TabItem + @Stack class:
 * - [HomeTab]: Primary tab, navigation patterns showcase
 * - [ExploreTab]: Master-detail with deep navigation
 * - [ProfileTab]: Profile management flows
 * - [SettingsTab]: App configuration
 *
 * ## Generated Code
 *
 * KSP processes these annotations and generates:
 * - `MainTabsConfig`: TabNavigatorConfig with tab definitions
 * - `MainTabsContainer`: @Composable container function
 * - Tab-specific builders for each stack
 *
 * ## Features Demonstrated
 *
 * 1. **Independent Navigation Stacks**: Each tab maintains its own back stack
 * 2. **State Preservation**: Content state preserved when switching tabs
 * 3. **Smart Back Press**: Hierarchical back navigation across tabs
 * 4. **Type Safety**: Compile-time checked tab definitions with KClass references
 *
 * Back press behavior:
 * 1. Pop from current tab's stack (if not at root)
 * 2. Switch to Home tab (if on another tab)
 * 3. Exit app (if on Home tab at root)
 *
 * @see MainTabsScreen for the UI implementation
 */

// ============================================================================
// Tab Definitions - Each tab is a @TabItem + @Stack class
// ============================================================================

/**
 * Home tab - Main entry point and navigation patterns showcase.
 *
 * Icon: "home" (material icon name)
 */
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeTabStack", startDestination = "Tab")
sealed class HomeTab : DestinationInterface {
    /** Root destination for the Home tab. */
    @Destination(route = "home/tab")
    data object Tab : HomeTab()
}

/**
 * Explore tab - Master-detail patterns and deep navigation.
 *
 * Icon: "explore" (material icon name)
 */
@TabItem(label = "Explore", icon = "explore")
@Stack(name = "exploreTabStack", startDestination = "Tab")
sealed class ExploreTab : DestinationInterface {
    /** Root destination for the Explore tab. */
    @Destination(route = "explore/tab")
    data object Tab : ExploreTab()
}

/**
 * Profile tab - User profile and settings.
 *
 * Icon: "person" (material icon name)
 */
@TabItem(label = "Profile", icon = "person")
@Stack(name = "profileTabStack", startDestination = "Tab")
sealed class ProfileTab : DestinationInterface {
    /** Root destination for the Profile tab. */
    @Destination(route = "profile/tab")
    data object Tab : ProfileTab()
}

/**
 * Settings tab - App configuration.
 *
 * Icon: "settings" (material icon name)
 */
@TabItem(label = "Settings", icon = "settings")
@Stack(name = "settingsTabStack", startDestination = "Tab")
sealed class SettingsTab : DestinationInterface {
    /** Root destination for the Settings tab. */
    @Destination(route = "settings/tab")
    data object Tab : SettingsTab()
}

// ============================================================================
// Tab Container - References the @TabItem classes
// ============================================================================

/**
 * Main tabs container that aggregates all tab definitions.
 *
 * The [Tab] annotation with [items] array provides type-safe tab references
 * and enables KSP to generate the complete navigation structure.
 */
@Tab(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class, ProfileTab::class, SettingsTab::class]
)
sealed class MainTabs : DestinationInterface
