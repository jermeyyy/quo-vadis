package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.tabs.MainTabs.*
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationDefinition

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


// ============================================================================
// Tab Container - References the @TabItem classes
// ============================================================================

/**
 * Main tabs container that aggregates all tab definitions.
 *
 * The [Tabs] annotation with [items] array provides type-safe tab references
 * and enables KSP to generate the complete navigation structure.
 */
@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class, ProfileTab::class, SettingsTab::class]
)
@Destination(route = "main/tabs")
sealed class MainTabs : DestinationDefinition {

    @TabItem(label = "Home", icon = "home")
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    /**
     * Explore tab - Master-detail patterns and deep navigation.
     *
     * Icon: "explore" (material icon name)
     */
    @TabItem(label = "Explore", icon = "explore")
    @Destination(route = "main/explore")
    data object ExploreTab : MainTabs()

    /**
     * Profile tab - User profile and settings.
     *
     * Icon: "person" (material icon name)
     */
    @TabItem(label = "Profile", icon = "person")
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()

    /**
     * Settings tab - App configuration.
     *
     * Icon: "settings" (material icon name)
     */
    @TabItem(label = "Settings", icon = "settings")
    @Stack(name = "settingsTabStack", startDestinationClass = SettingsTab.SettingsMain::class)
    sealed class SettingsTab : DestinationDefinition {
        /** Root destination for the Settings tab. */
        @Destination(route = "settings/tab")
        data object SettingsMain : SettingsTab()
    }

}
