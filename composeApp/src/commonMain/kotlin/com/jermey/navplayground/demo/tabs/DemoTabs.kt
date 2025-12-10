package com.jermey.navplayground.demo.tabs

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface

/**
 * Demo tabs for the nested tabs example screen.
 *
 * This demonstrates the new tabbed navigation API within the demo itself,
 * showing a three-tab example with independent content per tab.
 *
 * ## New Pattern
 *
 * Each tab is defined as a separate @TabItem + @Stack class:
 * - [DemoTab1]: Star themed items
 * - [DemoTab2]: Heart themed items  
 * - [DemoTab3]: Bookmark themed items
 *
 * ## Generated Code
 *
 * KSP processes this annotation and generates:
 * - `DemoTabsConfig`: TabNavigatorConfig with tab definitions
 * - `DemoTabsContainer`: @Composable container function
 *
 * ## Usage
 *
 * This is used in the TabsMainScreen to demonstrate the tabbed navigation
 * pattern with three tabs, each containing a list of items that can be
 * clicked to navigate to detail screens.
 *
 * @see com.jermey.navplayground.demo.ui.screens.tabs.TabsMainScreen
 */

// ============================================================================
// Tab Definitions
// ============================================================================

/**
 * First tab - Star themed items.
 */
@TabItem(label = "Tab 1", icon = "star")
@Stack(name = "demoTab1Stack", startDestination = "Tab")
sealed class DemoTab1 : DestinationInterface {
    /** Root destination for Tab 1. */
    @Destination(route = "demo/tab1")
    data object Tab : DemoTab1()
}

/**
 * Second tab - Heart themed items.
 */
@TabItem(label = "Tab 2", icon = "favorite")
@Stack(name = "demoTab2Stack", startDestination = "Tab")
sealed class DemoTab2 : DestinationInterface {
    /** Root destination for Tab 2. */
    @Destination(route = "demo/tab2")
    data object Tab : DemoTab2()
}

/**
 * Third tab - Bookmark themed items.
 */
@TabItem(label = "Tab 3", icon = "bookmark")
@Stack(name = "demoTab3Stack", startDestination = "Tab")
sealed class DemoTab3 : DestinationInterface {
    /** Root destination for Tab 3. */
    @Destination(route = "demo/tab3")
    data object Tab : DemoTab3()
}

// ============================================================================
// Tab Container
// ============================================================================

/**
 * Demo tabs container that aggregates all tab definitions.
 */
@Tabs(
    name = "demoTabs",
    initialTab = DemoTab1::class,
    items = [DemoTab1::class, DemoTab2::class, DemoTab3::class]
)
sealed class DemoTabs : DestinationInterface
