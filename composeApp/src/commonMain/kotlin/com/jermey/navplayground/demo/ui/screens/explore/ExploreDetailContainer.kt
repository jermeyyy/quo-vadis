package com.jermey.navplayground.demo.ui.screens.explore

import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import org.koin.core.annotation.Qualifier
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.enableLogging
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce

/**
 * State for the explore detail screen.
 * Uses Content as initial state with synchronously loaded item to support shared element transitions.
 */
sealed interface ExploreDetailState : MVIState {
    data class Content(val item: ExploreItem) : ExploreDetailState
    data class Error(val message: String) : ExploreDetailState
}

/**
 * Intents for the explore detail screen.
 */
sealed interface ExploreDetailIntent : MVIIntent {
    data object NavigateBack : ExploreDetailIntent
}

/**
 * Actions for the explore detail screen.
 */
sealed interface ExploreDetailAction : MVIAction {
    data class ShowError(val message: String) : ExploreDetailAction
}

private typealias DetailCtx = PipelineContext<ExploreDetailState, ExploreDetailIntent, ExploreDetailAction>

/**
 * Explore detail feature container with FlowMVI store.
 *
 * Demonstrates:
 * - Synchronous initial state for shared element transitions
 * - Error handling
 * - Navigation integration
 *
 * Uses [NavigationContainer] for proper lifecycle integration with the navigation system.
 */
@Scoped
@Scope(NavigationContainerScope::class)
@Qualifier(ExploreDetailContainer::class)
class ExploreDetailContainer(
    scope: NavigationContainerScope,
    repository: ExploreRepository,
    itemId: String,
    private val debuggable: Boolean = false
) : NavigationContainer<ExploreDetailState, ExploreDetailIntent, ExploreDetailAction>(scope) {

    // Initialize with item immediately (synchronous) for shared element transitions
    private val initialState: ExploreDetailState = repository.getItemById(itemId)
        ?.let { ExploreDetailState.Content(it) }
        ?: ExploreDetailState.Error("Item not found")

    override val store = store(initial = initialState) {
        configure {
            debuggable = this@ExploreDetailContainer.debuggable
            name = "ExploreDetailStore"
            parallelIntents = false
        }

        reduce { intent ->
            when (intent) {
                is ExploreDetailIntent.NavigateBack -> handleNavigateBack()
            }
        }

        recover { exception ->
            action(ExploreDetailAction.ShowError(exception.message ?: "Unknown error"))
            updateState { ExploreDetailState.Error(exception.message ?: "An error occurred") }
            null
        }

        if (debuggable) {
            enableLogging()
        }
    }

    private suspend fun DetailCtx.handleNavigateBack() {
        try {
            navigator.navigateBack()
        } catch (e: Exception) {
            // Already at root, ignore
        }
    }
}
