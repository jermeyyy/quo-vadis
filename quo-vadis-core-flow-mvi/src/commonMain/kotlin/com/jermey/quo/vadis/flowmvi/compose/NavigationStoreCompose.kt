package com.jermey.quo.vadis.flowmvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.flowmvi.core.NavigationAction
import com.jermey.quo.vadis.flowmvi.core.NavigationIntent
import com.jermey.quo.vadis.flowmvi.core.NavigationState
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Subscribe to navigation state changes with action handling.
 * 
 * Provides reactive state subscription with automatic lifecycle management.
 * Actions are delivered as one-time side effects.
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun MyScreen(container: NavigatorContainer) {
 *     val state = container.subscribeNavigation { action ->
 *         when (action) {
 *             is NavigationAction.ShowError -> {
 *                 snackbarHostState.showSnackbar(action.message)
 *             }
 *             is NavigationAction.NavigationFailed -> {
 *                 Log.e("Navigation", "Failed", action.error)
 *             }
 *         }
 *     }
 *     
 *     // Use state.value to access current state
 *     Text("Current: ${state.value.currentDestination}")
 * }
 * ```
 * 
 * @param onAction Handler for navigation actions (side effects)
 * @return State holder for NavigationState
 */
@Composable
fun Container<NavigationState, NavigationIntent, NavigationAction>.subscribeNavigation(
    onAction: suspend (NavigationAction) -> Unit
): State<NavigationState> {
    return store.subscribe { action ->
        onAction(action)
    }
}

/**
 * Remember an intent receiver for navigation intents.
 * 
 * Creates a stable IntentReceiver reference that survives recompositions.
 * Use this to dispatch navigation intents from UI components.
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun NavigationButton(container: NavigatorContainer) {
 *     val intentReceiver = rememberNavigationIntentReceiver(container)
 *     
 *     Button(
 *         onClick = { 
 *             intentReceiver.intent(NavigationIntent.Navigate(DetailsDestination))
 *         }
 *     ) {
 *         Text("Navigate")
 *     }
 * }
 * ```
 * 
 * @return Stable IntentReceiver for dispatching intents
 */
@Composable
fun rememberNavigationIntentReceiver(
    container: Container<NavigationState, NavigationIntent, NavigationAction>
): IntentReceiver<NavigationIntent> {
    return remember(container) {
        container.store
    }
}

/**
 * Extension: Emit a navigation intent.
 * 
 * Convenience method for dispatching intents with receiver scope.
 * 
 * Usage:
 * ```kotlin
 * with(intentReceiver) {
 *     navigateTo(DetailsDestination)
 * }
 * ```
 */
fun IntentReceiver<NavigationIntent>.navigateTo(
    destination: NavDestination,
    transition: com.jermey.quo.vadis.core.navigation.core.NavigationTransition? = null
) {
    intent(NavigationIntent.Navigate(destination, transition))
}

/**
 * Extension: Emit a navigate back intent.
 */
fun IntentReceiver<NavigationIntent>.navigateBack() {
    intent(NavigationIntent.NavigateBack)
}

/**
 * Extension: Emit a navigate and clear to intent.
 */
fun IntentReceiver<NavigationIntent>.navigateAndClearTo(
    destination: NavDestination,
    popUpToRoute: String,
    inclusive: Boolean = false
) {
    intent(NavigationIntent.NavigateAndClearTo(destination, popUpToRoute, inclusive))
}

/**
 * Extension: Emit a navigate and replace intent.
 */
fun IntentReceiver<NavigationIntent>.navigateAndReplace(
    destination: NavDestination
) {
    intent(NavigationIntent.NavigateAndReplace(destination))
}

/**
 * Extension: Emit a navigate and clear all intent.
 */
fun IntentReceiver<NavigationIntent>.navigateAndClearAll(
    destination: NavDestination
) {
    intent(NavigationIntent.NavigateAndClearAll(destination))
}
