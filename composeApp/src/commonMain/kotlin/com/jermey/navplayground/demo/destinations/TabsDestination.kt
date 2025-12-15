package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination

/**
 * Tabs navigation destinations
 */
@Stack(name = "tabs", startDestination = "Main")
sealed class TabsDestination : Destination {

    @com.jermey.quo.vadis.annotations.Destination(route = "tabs/main")
    data object Main : TabsDestination()

    /**
     * Dynamic tab destination.
     * Tab routes are handled dynamically based on tabId.
     */
    data class Tab(val tabId: String) : TabsDestination()

    @com.jermey.quo.vadis.annotations.Destination(route = "tabs/subitem/{tabId}/{itemId}")
    data class SubItem(
        @Argument val tabId: String,
        @Argument val itemId: String
    ) : TabsDestination()
}