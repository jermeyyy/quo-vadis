package com.jermey.navplayground.navigation

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Container Demo — simple flat tabs with arguments.
 *
 * Navigate with `ContainerDemoDestination.Info(itemId = "42")` to open
 * the tab container. The shared MVI container reads `itemId` from
 * `TabNode.destinationArgs` via `DestinationFactory`.
 */
@Tabs(name = "containerDemoTabs")
@Destination(route = "container-demo/{itemId}")
data class ContainerDemoDestination(
    @Argument val itemId: Int
) : NavDestination {

    @TabItem(parent = ContainerDemoDestination::class, isDefault = true)
    @Destination(route = "container-demo/{itemId}/info")
    data object Info : NavDestination

    @TabItem(parent = ContainerDemoDestination::class)
    @Destination(route = "container-demo/{itemId}/stats")
    data object Stats : NavDestination
}

