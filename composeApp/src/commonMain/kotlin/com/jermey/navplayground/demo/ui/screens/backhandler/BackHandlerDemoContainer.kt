package com.jermey.navplayground.demo.ui.screens.backhandler

import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import org.koin.core.annotation.Qualifier
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed

/**
 * State for the Back Handler demo screen.
 *
 * @property text The current text in the editor
 * @property showDiscardDialog Whether to show the discard confirmation dialog
 */
data class BackHandlerDemoState(
    val text: String = "",
    val showDiscardDialog: Boolean = false,
) : MVIState {
    val hasUnsavedChanges: Boolean get() = text.isNotBlank()
}

/**
 * Intents for the Back Handler demo screen.
 */
sealed interface BackHandlerDemoIntent : MVIIntent {
    data class UpdateText(val text: String) : BackHandlerDemoIntent
    data object HandleSystemBack : BackHandlerDemoIntent
    data object ShowDiscardDialog : BackHandlerDemoIntent
    data object DismissDiscardDialog : BackHandlerDemoIntent
    data object DiscardAndNavigateBack : BackHandlerDemoIntent
    data object NavigateBack : BackHandlerDemoIntent
}

/**
 * Actions for the Back Handler demo screen.
 */
sealed interface BackHandlerDemoAction : MVIAction

/**
 * Back Handler Demo container with FlowMVI store.
 *
 * Demonstrates:
 * - `scope.registerBackHandler()` for intercepting user-initiated back events from an MVI container
 * - The handler is automatically scoped to the screen's lifecycle via [NavigationContainerScope]
 * - Programmatic `navigateBack()` bypasses the registered handler
 *
 * Uses [NavigationContainer] for proper lifecycle integration with the navigation system.
 */
@Scoped
@Scope(NavigationContainerScope::class)
@Qualifier(BackHandlerDemoContainer::class)
class BackHandlerDemoContainer(
    scope: NavigationContainerScope,
) : NavigationContainer<BackHandlerDemoState, BackHandlerDemoIntent, BackHandlerDemoAction>(scope) {

    override val store = store(initial = BackHandlerDemoState()) {
        configure {
            name = "BackHandlerDemoStore"
        }

        init {
            // Register a back handler via the MVI container scope.
            // Always intercept and delegate the decision to the store via intent.
            // The store checks current state and either shows a dialog or navigates back.
            // This handler is automatically unregistered when the screen is destroyed.
            scope.registerBackHandler {
                intent(BackHandlerDemoIntent.HandleSystemBack)
                true // always consume — let the store decide based on current state
            }
        }


        reduce { intent ->
            when (intent) {
                is BackHandlerDemoIntent.UpdateText -> {
                    updateState { copy(text = intent.text) }
                }

                is BackHandlerDemoIntent.HandleSystemBack -> {
                    // Check current state atomically and decide
                    var shouldNavigate = false
                    updateState {
                        if (hasUnsavedChanges) {
                            copy(showDiscardDialog = true)
                        } else {
                            shouldNavigate = true
                            this
                        }
                    }
                    if (shouldNavigate) {
                        // No unsaved changes — navigate back programmatically
                        navigator.navigateBack()
                    }
                }

                is BackHandlerDemoIntent.ShowDiscardDialog -> {
                    updateState { copy(showDiscardDialog = true) }
                }

                is BackHandlerDemoIntent.DismissDiscardDialog -> {
                    updateState { copy(showDiscardDialog = false) }
                }

                is BackHandlerDemoIntent.DiscardAndNavigateBack -> {
                    updateState { copy(showDiscardDialog = false, text = "") }
                    // Programmatic navigateBack() bypasses the back handler registry
                    navigator.navigateBack()
                }

                is BackHandlerDemoIntent.NavigateBack -> {
                    navigator.navigateBack()
                }
            }
        }
    }
}
