package com.jermey.navplayground.demo.container

import com.jermey.navplayground.demo.destinations.SelectedItem
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.navigateBackWithResult
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

/**
 * Represents an item available for selection in the picker.
 *
 * @property id Unique identifier
 * @property name Display name
 * @property description Optional description
 * @property icon Emoji icon for visual distinction
 */
data class PickerItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: String
)

/**
 * State for the Item Picker screen.
 *
 * @property items List of items available for selection
 */
data class ItemPickerState(
    val items: List<PickerItem> = defaultItems
) : MVIState

/**
 * Default items available in the picker.
 */
private val defaultItems = listOf(
    PickerItem("1", "Red Apple", "Fresh and crispy", "🍎"),
    PickerItem("2", "Banana", "Perfectly ripe", "🍌"),
    PickerItem("3", "Orange", "Sweet and juicy", "🍊"),
    PickerItem("4", "Grapes", "Seedless variety", "🍇"),
    PickerItem("5", "Strawberry", "Garden fresh", "🍓"),
    PickerItem("6", "Watermelon", "Summer favorite", "🍉"),
)

/**
 * Container for the Item Picker screen.
 *
 * Demonstrates:
 * - Returning a result using [navigateBackWithResult]
 * - Lifecycle integration via [BaseContainer]
 *
 * ## Usage
 *
 * ```kotlin
 * val screenKey = LocalScreenNode.current?.key ?: return
 * val container = remember(screenKey) {
 *     ItemPickerContainer(navigator, screenKey)
 * }
 * container.selectItem(item) // Returns result to caller
 * container.cancel() // Navigates back without result
 * ```
 *
 * @param navigator The Navigator instance
 * @param screenKey The unique screen key from LocalScreenNode
 */
class ItemPickerContainer(
    navigator: Navigator,
    screenKey: String,
) : BaseContainer<ItemPickerState, ItemPickerContainer.Intent, ItemPickerContainer.Action>(
    navigator,
    screenKey
) {

    sealed class Intent : MVIIntent {
        data class SelectItem(val item: PickerItem) : Intent()
        data object Cancel : Intent()
    }

    data object Action : MVIAction


    override val store = store(ItemPickerState()) {
        configure { }
        reduce { intent ->
            when (intent) {
                is Intent.SelectItem -> selectItem(intent.item)
                Intent.Cancel -> cancel()
            }
        }
    }

    /**
     * Select an item and return it as a result to the caller.
     *
     * Uses [navigateBackWithResult] to:
     * 1. Create a [SelectedItem] from the picker item
     * 2. Return the result to the calling screen
     * 3. Navigate back
     *
     * @param item The item to select
     */
    private fun selectItem(item: PickerItem) {
        val result = SelectedItem(
            id = item.id,
            name = item.name
        )
        navigator.navigateBackWithResult(result)
    }

    /**
     * Cancel the selection and navigate back without a result.
     *
     * The caller will receive `null` from [navigateForResult].
     */
    private fun cancel() {
        navigator.navigateBack()
    }

    override fun onEnter() {
        println("ItemPickerContainer: onEnter")
    }

    override fun onExit() {
        println("ItemPickerContainer: onExit")
    }
}
