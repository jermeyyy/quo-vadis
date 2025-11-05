package com.jermey.quo.vadis.flowmvi.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.NavHost
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.flowmvi.core.NavigationAction
import com.jermey.quo.vadis.flowmvi.core.NavigationIntent
import com.jermey.quo.vadis.flowmvi.core.NavigationState
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.compose.dsl.subscribe

/**
 * Store-aware NavHost that integrates FlowMVI state management with Quo Vadis navigation.
 * 
 * Combines the power of FlowMVI's reactive state management with Quo Vadis's
 * type-safe navigation system. The NavHost automatically subscribes to navigation
 * state changes and renders the appropriate destination.
 * 
 * Features:
 * - Reactive state management via FlowMVI
 * - Type-safe navigation through intents
 * - Automatic state synchronization
 * - Predictive back gesture support
 * - Custom transitions per navigation
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = remember { DefaultNavigator() }
 *     val container = remember { NavigatorContainer(navigator) }
 *     val graph = remember { createNavigationGraph() }
 *     
 *     StoreAwareNavHost(
 *         container = container,
 *         navigator = navigator,
 *         graph = graph,
 *         defaultTransition = NavigationTransitions.SlideHorizontal
 *     ) { action ->
 *         when (action) {
 *             is NavigationAction.ShowError -> {
 *                 // Handle navigation errors
 *             }
 *         }
 *     }
 * }
 * ```
 * 
 * @param container The FlowMVI container managing navigation state
 * @param navigator The Quo Vadis navigator instance
 * @param graph The navigation graph defining destinations
 * @param modifier Optional modifier for the NavHost
 * @param defaultTransition Default transition for navigation (required)
 * @param enablePredictiveBack Enable predictive back gestures (default: true)
 * @param onAction Handler for navigation actions (side effects)
 */
@Composable
fun StoreAwareNavHost(
    container: Container<NavigationState, NavigationIntent, NavigationAction>,
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition,
    enablePredictiveBack: Boolean = true,
    onAction: suspend (NavigationAction) -> Unit = {}
) {
    // Subscribe to navigation state
    with(container.store) {
        val state by subscribe { action ->
            onAction(action)
        }
        
        // Render NavHost with current state
        NavHost(
            graph = graph,
            navigator = navigator,
            modifier = modifier,
            defaultTransition = defaultTransition,
            enablePredictiveBack = enablePredictiveBack
        )
    }
}

/**
 * Simplified Store-aware NavHost with inline content builder.
 * 
 * Use this variant when you want to define destinations inline without creating
 * a separate NavigationGraph.
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun SimpleApp() {
 *     val navigator = remember { DefaultNavigator() }
 *     val container = remember { NavigatorContainer(navigator) }
 *     
 *     StoreAwareNavHostInline(
 *         container = container,
 *         navigator = navigator,
 *         startDestination = HomeDestination
 *     ) { destination ->
 *         when (destination) {
 *             is HomeDestination -> HomeScreen(container)
 *             is DetailsDestination -> DetailsScreen(container, destination.id)
 *         }
 *     }
 * }
 * ```
 * 
 * @param container The FlowMVI container managing navigation state
 * @param navigator The Quo Vadis navigator instance
 * @param startDestination The initial destination
 * @param modifier Optional modifier
 * @param defaultTransition Default transition (optional)
 * @param enablePredictiveBack Enable predictive back (default: true)
 * @param onAction Action handler
 * @param content Composable content for each destination
 */
@Composable
fun StoreAwareNavHostInline(
    container: Container<NavigationState, NavigationIntent, NavigationAction>,
    navigator: Navigator,
    startDestination: Destination,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition? = null,
    enablePredictiveBack: Boolean = true,
    onAction: suspend (NavigationAction) -> Unit = {},
    content: @Composable (Destination) -> Unit
) {
    // Subscribe to navigation state
    with(container.store) {
        val state by subscribe { action ->
            onAction(action)
        }
        
        // Render current destination
        state.currentDestination?.let { destination ->
            content(destination)
        } ?: run {
            // Initialize with start destination if needed
            if (state.backStackSize == 0) {
                navigator.navigate(startDestination)
            }
        }
    }
}

/**
 * Store-aware NavHost with explicit state access.
 * 
 * Use when you need direct access to navigation state within the content.
 * 
 * Usage:
 * ```kotlin
 * @Composable
 * fun AdvancedApp() {
 *     StoreAwareNavHostWithState(
 *         container = container,
 *         navigator = navigator,
 *         graph = graph,
 *         defaultTransition = NavigationTransitions.Fade
 *     ) { state, action ->
 *         when (action) {
 *             is NavigationAction.ShowError -> {
 *                 // Handle error
 *             }
 *         }
 *         
 *         // Access state.currentDestination, state.canGoBack, etc.
 *         if (!state.canGoBack) {
 *             // Show exit dialog
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun StoreAwareNavHostWithState(
    container: Container<NavigationState, NavigationIntent, NavigationAction>,
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition,
    enablePredictiveBack: Boolean = true,
    content: @Composable (state: NavigationState, onAction: suspend (NavigationAction) -> Unit) -> Unit
) {
    with(container.store) {
        var actionHandler: (suspend (NavigationAction) -> Unit)? = null
        val state by subscribe { action ->
            actionHandler?.invoke(action)
        }
        
        content(state) { action ->
            actionHandler = { a -> }
        }
        
        NavHost(
            graph = graph,
            navigator = navigator,
            modifier = modifier,
            defaultTransition = defaultTransition,
            enablePredictiveBack = enablePredictiveBack
        )
    }
}
