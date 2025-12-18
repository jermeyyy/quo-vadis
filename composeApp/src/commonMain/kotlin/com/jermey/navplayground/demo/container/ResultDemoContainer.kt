package com.jermey.navplayground.demo.container

import com.jermey.navplayground.demo.container.ResultDemoContainer.Action
import com.jermey.navplayground.demo.container.ResultDemoContainer.Intent
import com.jermey.navplayground.demo.destinations.ResultDemoDestination
import com.jermey.navplayground.demo.destinations.SelectedItem
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.navigateForResult
import com.jermey.quo.vadis.core.navigation.core.registerNavigationLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.StateReceiver
import pro.respawn.flowmvi.dsl.state
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

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
) : MVIState

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
) : BaseContainer<ResultDemoState, Intent, Action>(navigator, screenKey) {
    sealed class Intent : MVIIntent {
        data object PickItem : Intent()
        data object ClearSelection : Intent()
    }

    data object Action : MVIAction

    init {
        println("ResultDemoContainer created with screenKey: $screenKey")
    }

    override val store = store(ResultDemoState()) {
        configure { }
        reduce { intent ->
            when (intent) {
                Intent.ClearSelection -> clearSelection()
                Intent.PickItem -> pickItem()
            }
        }
    }

    private suspend fun StateReceiver<ResultDemoState>.pickItem() {
        println("ResultDemoContainer.pickItem() called")
        updateState { copy(isLoading = true) }
        coroutineScope.launch {
            println("ResultDemoContainer: launching coroutine for navigateForResult")

            val result: SelectedItem? = navigator.navigateForResult(
                ResultDemoDestination.ItemPicker
            )

            println("ResultDemoContainer: navigateForResult returned: $result")

            updateState {
                copy(
                    selectedItem = result,
                    isLoading = false,
                    message = if (result != null) "Selected: ${result.name}" else "Selection cancelled"
                )
            }
        }
    }

    /**
     * Clear the current selection.
     */
    private suspend fun StateReceiver<ResultDemoState>.clearSelection() {
        updateState {
            copy(
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
