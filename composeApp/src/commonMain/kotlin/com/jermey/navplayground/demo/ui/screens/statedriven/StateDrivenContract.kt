package com.jermey.navplayground.demo.ui.screens.statedriven

import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.StateDrivenDemoDestination.DemoTab
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * State-driven navigation demo feature state.
 *
 * Captures the current state of the backstack editor demo,
 * including entries and UI state for dialogs.
 */
data class StateDrivenState(
    /**
     * Current list of backstack entries.
     */
    val entries: List<BackStackEntry> = emptyList(),
    
    /**
     * Whether the add destination dialog is currently showing.
     */
    val showAddDialog: Boolean = false
) : MVIState {
    
    /**
     * ID of the current (top) entry, or null if stack is empty.
     */
    val currentEntryId: String?
        get() = entries.lastOrNull()?.id
    
    /**
     * Current (topmost) entry, or null if stack is empty.
     */
    val currentEntry: BackStackEntry?
        get() = entries.lastOrNull()
    
    /**
     * Whether back navigation is possible (more than one entry).
     */
    val canNavigateBack: Boolean
        get() = entries.size > 1
    
    /**
     * Size of the backstack.
     */
    val size: Int
        get() = entries.size
    
    /**
     * Whether the backstack is empty.
     */
    val isEmpty: Boolean
        get() = entries.isEmpty()
    
    /**
     * Whether the backstack is not empty.
     */
    val isNotEmpty: Boolean
        get() = entries.isNotEmpty()
}

/**
 * State-driven navigation demo intents.
 *
 * User actions for manipulating the backstack and UI.
 */
sealed interface StateDrivenIntent : MVIIntent {
    
    /**
     * Push a new destination onto the backstack.
     */
    data class PushDestination(val destination: DemoTab) : StateDrivenIntent
    
    /**
     * Pop the top entry from the backstack.
     */
    data object Pop : StateDrivenIntent
    
    /**
     * Clear all entries from the backstack.
     */
    data object Clear : StateDrivenIntent
    
    /**
     * Clear and push Home destination (reset to initial state).
     */
    data object Reset : StateDrivenIntent
    
    /**
     * Remove entry at a specific index.
     */
    data class RemoveAt(val index: Int) : StateDrivenIntent
    
    /**
     * Swap entries at two indices.
     */
    data class Swap(val index1: Int, val index2: Int) : StateDrivenIntent
    
    /**
     * Show the add destination dialog.
     */
    data object ShowAddDialog : StateDrivenIntent
    
    /**
     * Hide the add destination dialog.
     */
    data object HideAddDialog : StateDrivenIntent
    
    /**
     * Navigate back using the main navigator (exit the demo screen).
     */
    data object NavigateBack : StateDrivenIntent
}

/**
 * State-driven navigation demo actions (side effects).
 *
 * One-off events for user feedback.
 */
sealed interface StateDrivenAction : MVIAction {
    
    /**
     * Show a toast message to the user.
     */
    data class ShowToast(val message: String) : StateDrivenAction
}
