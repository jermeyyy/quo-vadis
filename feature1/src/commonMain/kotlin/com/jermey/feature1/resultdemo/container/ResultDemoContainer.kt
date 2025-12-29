package com.jermey.feature1.resultdemo.container

import com.jermey.feature1.resultdemo.ResultDemoDestination
import com.jermey.feature1.resultdemo.SelectedItem
import com.jermey.feature1.resultdemo.container.ResultDemoContainer.Action
import com.jermey.feature1.resultdemo.container.ResultDemoContainer.Intent
import com.jermey.quo.vadis.core.navigation.navigateForResult
import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import kotlin.time.Duration.Companion.seconds

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
    val message: String = "No item selected yet",
    val timerValue: Int = 0
) : MVIState

/**
 * Container for the Result Demo screen.
 *
 * Demonstrates:
 * - Navigation with result using [navigateForResult]
 * - Lifecycle integration via [NavigationContainer]
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun ResultDemoScreen() {
 *     val store = rememberContainer<ResultDemoContainer, ResultDemoState, Intent, Action>()
 *     // use store
 * }
 * ```
 *
 * @param scope The NavigationContainerScope for navigation and lifecycle
 */
class ResultDemoContainer(
    scope: NavigationContainerScope,
) : NavigationContainer<ResultDemoState, Intent, Action>(scope) {
    sealed class Intent : MVIIntent {
        data object PickItem : Intent()
        data object ClearSelection : Intent()
        data object StartTimer : Intent()
    }

    data object Action : MVIAction

    init {
        println("ResultDemoContainer created with screenKey: $screenKey")
    }

    override val store = store(ResultDemoState()) {
        init {
            intent(Intent.StartTimer)
        }
        reduce { intent ->
            when (intent) {
                Intent.ClearSelection -> clearSelection()
                Intent.PickItem -> pickItem()
                Intent.StartTimer -> startTimer()
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

    /**
     * Start a timer to demonstrate lifecycle integration.
     *
     * Updates the message every second.
     */
    private fun Ctx.startTimer() {
        coroutineScope.launch {
            while (true) {
                updateState {
                    val newTimer = timerValue + 1
                    println("ResultDemoContainer: Timer tick: $newTimer")
                    copy(timerValue = newTimer)
                }
                delay(1.seconds)
            }
        }
    }
}
