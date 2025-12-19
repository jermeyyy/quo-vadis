package com.jermey.feature1.resultdemo

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.ReturnsResult

/**
 * Result type returned from [ItemPickerDestination].
 *
 * Contains information about the selected item.
 *
 * @property id Unique identifier of the selected item
 * @property name Display name of the selected item
 */
data class SelectedItem(
    val id: String,
    val name: String
)

/**
 * Navigation Result Demo destinations.
 *
 * Demonstrates:
 * - Type-safe result-returning navigation with [ReturnsResult]
 * - [navigateForResult] and [navigateBackWithResult] APIs
 * - Container lifecycle integration
 *
 * ## Flow
 *
 * ```
 * ResultDemo (entry screen)
 *     │
 *     │ navigateForResult(ItemPicker)
 *     ▼
 * ItemPicker (picks an item)
 *     │
 *     │ navigateBackWithResult(SelectedItem)
 *     ▼
 * ResultDemo (receives result)
 * ```
 */
@Stack(name = "result_demo", startDestination = ResultDemoDestination.Demo::class)
sealed class ResultDemoDestination : NavDestination {

    /**
     * Entry screen for the navigation result demo.
     *
     * Shows the currently selected item (if any) and allows
     * the user to pick a new item.
     */
    @Destination(route = "result_demo/demo")
    data object Demo : ResultDemoDestination()

    /**
     * Item picker screen that returns a [SelectedItem] result.
     *
     * Implements [ReturnsResult] to enable type-safe result navigation.
     * The calling screen uses [navigateForResult] to navigate here
     * and await the result.
     */
    @Destination(route = "result_demo/picker")
    data object ItemPicker : ResultDemoDestination(), ReturnsResult<SelectedItem>
}
