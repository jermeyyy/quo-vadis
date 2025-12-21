package com.jermey.feature1.resultdemo.container

import com.jermey.feature1.resultdemo.ResultDemoDestination
import com.jermey.feature1.resultdemo.SelectedItem
import com.jermey.feature1.resultdemo.container.ResultDemoContainer.Action
import com.jermey.feature1.resultdemo.container.ResultDemoContainer.Intent
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.navigateForResult
import com.jermey.quo.vadis.flowmvi.BaseContainer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

private typealias Ctx = PipelineContext<ResultDemoState, Intent, Action>

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

    private suspend fun Ctx.pickItem() {
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
    private suspend fun Ctx.clearSelection() {
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
