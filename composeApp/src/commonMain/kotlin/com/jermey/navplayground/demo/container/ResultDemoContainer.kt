package com.jermey.navplayground.demo.container

import com.jermey.navplayground.demo.destinations.ResultDemoDestination
import com.jermey.navplayground.demo.destinations.SelectedItem
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.navigateForResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the Result Demo screen.
 *
 * @property selectedItem The currently selected item, or null if none selected
 * @property isLoading Whether a result navigation is in progress
 * @property message Status message to display
 */
data class ResultDemoState(
    val selectedItem: SelectedItem? = null,
    val isLoading: Boolean = false,
    val message: String = "No item selected yet"
)

/**
 * Container for the Result Demo screen.
 *
 * Demonstrates:
 * - Navigation with result using [navigateForResult]
 * - Lifecycle integration via [BaseContainer]
 * - State management with [StateFlow]
 *
 * ## Usage
 *
 * ```kotlin
 * val screenKey = LocalScreenNode.current?.key ?: return
 * val container = remember(screenKey) {
 *     ResultDemoContainer(navigator, screenKey)
 * }
 * container.pickItem() // Navigates to picker and awaits result
 * container.state.collect { state ->
 *     // React to state changes
 * }
 * ```
 *
 * @param navigator The Navigator instance
 * @param screenKey The unique screen key from LocalScreenNode
 */
class ResultDemoContainer(
    navigator: Navigator,
    screenKey: String,
) : BaseContainer(navigator, screenKey) {

    init {
        println("ResultDemoContainer created with screenKey: $screenKey")
    }

    private val _state = MutableStateFlow(ResultDemoState())

    /**
     * Observable state for the UI.
     */
    val state: StateFlow<ResultDemoState> = _state.asStateFlow()

    /**
     * Navigate to the item picker and await a result.
     *
     * Uses [navigateForResult] to:
     * 1. Navigate to [ResultDemoDestination.ItemPicker]
     * 2. Suspend until the picker returns a result or is cancelled
     * 3. Update state with the selected item
     */
    fun pickItem() {
        println("ResultDemoContainer.pickItem() called")
        _state.update { it.copy(isLoading = true) }
        scope.launch {
            println("ResultDemoContainer: launching coroutine for navigateForResult")

            val result: SelectedItem? = navigator.navigateForResult(
                ResultDemoDestination.ItemPicker
            )

            println("ResultDemoContainer: navigateForResult returned: $result")

            _state.update {
                val newState = if (result != null) {
                    it.copy(
                        selectedItem = result,
                        isLoading = false,
                        message = "Selected: ${result.name}"
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        message = "Selection cancelled"
                    )
                }
                println("ResultDemoContainer: updating state to: $newState")
                newState
            }
            
            println("ResultDemoContainer: state after update: ${_state.value}")
        }
    }

    /**
     * Clear the current selection.
     */
    fun clearSelection() {
        _state.update {
            it.copy(
                selectedItem = null,
                message = "No item selected yet"
            )
        }
    }

    override fun onEnter() {
        println("ResultDemoContainer: onEnter")
    }

    override fun onExit() {
        println("ResultDemoContainer: onExit")
    }
}
