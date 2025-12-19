package com.jermey.quo.vadis.flowmvi.core

import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * Base navigation state interface for FlowMVI integration.
 * 
 * Represents the current state of navigation with type-safe access to:
 * - Current destination in the navigation stack
 * - Back stack size for UI indicators
 * - Navigation capability flags
 * 
 * Extend this interface to create feature-specific navigation states.
 * 
 * Example:
 * ```kotlin
 * data class ProfileNavigationState(
 *     override val currentDestination: NavDestination?,
 *     override val backStackSize: Int,
 *     val userData: UserData?
 * ) : NavigationState
 * ```
 */
interface NavigationState : MVIState {
    /**
     * The currently active destination in the navigation stack.
     * Null if navigation hasn't been initialized.
     */
    val currentDestination: NavDestination?
    
    /**
     * Number of destinations in the back stack (including current).
     * Used for UI indicators like "back" button visibility.
     */
    val backStackSize: Int
    
    /**
     * Whether the navigator can go back.
     * True if backStackSize > 1, false otherwise.
     */
    val canGoBack: Boolean
        get() = backStackSize > 1
}

/**
 * Base navigation intent interface for FlowMVI integration.
 * 
 * Represents user-initiated navigation actions that can be dispatched to the store.
 * All navigation operations from Quo Vadis Navigator are mapped to intents.
 * 
 * Use these intents in your reducers to handle navigation:
 * ```kotlin
 * reduce { intent ->
 *     when (intent) {
 *         is NavigationIntent.Navigate -> {
 *             navigator.navigate(intent.destination, intent.transition)
 *             updateState { /* update state */ }
 *         }
 *         // ... handle other intents
 *     }
 * }
 * ```
 */
sealed interface NavigationIntent : MVIIntent {
    /**
     * Navigate to a specific destination with optional transition.
     * 
     * @param destination The target destination (type-safe)
     * @param transition Optional custom transition animation
     */
    data class Navigate(
        val destination: NavDestination,
        val transition: NavigationTransition? = null
    ) : NavigationIntent
    
    /**
     * Navigate back to the previous destination in the stack.
     * Does nothing if already at the root.
     */
    data object NavigateBack : NavigationIntent
    
    /**
     * Navigate to a destination and clear back stack up to a specific destination.
     * 
     * @param destination The new destination to navigate to
     * @param popUpToRoute The route to pop up to (inclusive or exclusive)
     * @param inclusive Whether to also pop the popUpToRoute destination
     * 
     * Example: Navigate to Home and clear all previous screens
     * ```kotlin
     * NavigationIntent.NavigateAndClearTo(
     *     destination = HomeDestination,
     *     popUpToRoute = "login",
     *     inclusive = true
     * )
     * ```
     */
    data class NavigateAndClearTo(
        val destination: NavDestination,
        val popUpToRoute: String,
        val inclusive: Boolean = false
    ) : NavigationIntent
    
    /**
     * Replace the current destination with a new one (no back stack entry).
     * 
     * @param destination The replacement destination
     * 
     * Use case: Login -> Home (don't allow back to login)
     */
    data class NavigateAndReplace(
        val destination: NavDestination
    ) : NavigationIntent
    
    /**
     * Navigate to a destination and clear entire back stack.
     * 
     * @param destination The new root destination
     * 
     * Use case: Logout -> Login (clear all previous screens)
     */
    data class NavigateAndClearAll(
        val destination: NavDestination
    ) : NavigationIntent
    
    /**
     * Navigate to a graph by its ID with a specific start destination.
     * 
     * @param graphId The identifier of the navigation graph
     * @param startDestination The initial destination within the graph
     * 
     * Use case: Navigate to a feature module's navigation graph
     */
    data class NavigateToGraph(
        val graphId: String,
        val startDestination: NavDestination
    ) : NavigationIntent
}

/**
 * Base navigation action interface for FlowMVI integration.
 * 
 * Represents side effects that should be handled outside the store,
 * such as showing error messages, analytics events, or other one-time effects.
 * 
 * Subscribe to actions in Compose:
 * ```kotlin
 * val state by store.subscribe { action ->
 *     when (action) {
 *         is NavigationAction.ShowError -> {
 *             snackbarHostState.showSnackbar(action.message)
 *         }
 *         is NavigationAction.NavigationFailed -> {
 *             Log.e("Navigation", "Failed", action.error)
 *         }
 *     }
 * }
 * ```
 */
sealed interface NavigationAction : MVIAction {
    /**
     * Show an error message to the user.
     * 
     * @param message User-friendly error message
     */
    data class ShowError(val message: String) : NavigationAction
    
    /**
     * Navigation operation failed with an exception.
     * 
     * @param error The exception that caused the failure
     * @param destinationRoute Optional route that failed to navigate to
     */
    data class NavigationFailed(
        val error: Throwable,
        val destinationRoute: String? = null
    ) : NavigationAction
    
    /**
     * Deep link handling failed.
     * 
     * @param uri The URI that failed to handle
     * @param reason Human-readable failure reason
     */
    data class DeepLinkFailed(
        val uri: String,
        val reason: String
    ) : NavigationAction
}
