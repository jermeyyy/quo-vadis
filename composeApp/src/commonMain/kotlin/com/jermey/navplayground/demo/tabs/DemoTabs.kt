package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabGraph
import com.jermey.quo.vadis.core.navigation.core.TabDefinition

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
@TabGraph(
    name = "demo_tabs",
    initialTab = "Tab1",
    primaryTab = "Tab1"
)
sealed class DemoTabs : TabDefinition {

    /**
     * First tab - Star themed items.
     */
    @Tab(
        route = "demo_tab1",
        label = "Tab 1",
        icon = "star",
        rootGraph = TabsDestination::class,
        rootDestination = TabsDestination.Main::class
    )
    data object Tab1 : DemoTabs() {
        override val route = "demo_tab1"
        override val rootDestination = TabsDestination.Main
    }

    /**
     * Second tab - Heart themed items.
     */
    @Tab(
        route = "demo_tab2",
        label = "Tab 2",
        icon = "favorite",
        rootGraph = TabsDestination::class,
        rootDestination = TabsDestination.Main::class
    )
    data object Tab2 : DemoTabs() {
        override val route = "demo_tab2"
        override val rootDestination = TabsDestination.Main
    }

    /**
     * Third tab - Bookmark themed items.
     */
    @Tab(
        route = "demo_tab3",
        label = "Tab 3",
        icon = "bookmark",
        rootGraph = TabsDestination::class,
        rootDestination = TabsDestination.Main::class
    )
    data object Tab3 : DemoTabs() {
        override val route = "demo_tab3"
        override val rootDestination = TabsDestination.Main
    }
}
