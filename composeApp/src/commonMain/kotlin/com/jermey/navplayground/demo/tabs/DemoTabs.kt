package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.quo.vadis.annotations.Destination as DestinationAnnotation
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.core.Destination

/**
 * Demo tabs for the nested tabs example screen.
 *
 * This demonstrates the new tabbed navigation API within the demo itself,
 * showing a three-tab example with independent content per tab.
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
@Tab(name = "demoTabs", initialTab = "Tab1")
sealed class DemoTabs : Destination {

    /**
     * First tab - Star themed items.
     */
    @TabItem(label = "Tab 1", icon = "star", rootGraph = TabsDestination::class)
    @DestinationAnnotation(route = "demo/tab1")
    data object Tab1 : DemoTabs()

    /**
     * Second tab - Heart themed items.
     */
    @TabItem(label = "Tab 2", icon = "favorite", rootGraph = TabsDestination::class)
    @DestinationAnnotation(route = "demo/tab2")
    data object Tab2 : DemoTabs()

    /**
     * Third tab - Bookmark themed items.
     */
    @TabItem(label = "Tab 3", icon = "bookmark", rootGraph = TabsDestination::class)
    @DestinationAnnotation(route = "demo/tab3")
    data object Tab3 : DemoTabs()
}
