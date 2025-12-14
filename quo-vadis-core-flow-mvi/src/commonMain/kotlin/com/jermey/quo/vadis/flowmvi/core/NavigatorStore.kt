package com.jermey.quo.vadis.flowmvi.core

import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.activeStack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.enableLogging
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed

/**
 * FlowMVI Container that wraps Quo Vadis Navigator.
 * 
 * Provides reactive state management for navigation operations using FlowMVI patterns.
 * The container synchronizes Navigator's state with FlowMVI store state automatically.
 * 
 * Features:
 * - Type-safe navigation through intents
 * - Reactive state updates via StateFlow
 * - Error recovery with NavigationAction.NavigationFailed
 * - Lifecycle-aware subscriptions
 * - Optional debug logging
 * 
 * Usage:
 * ```kotlin
 * val navigator = DefaultNavigator()
 * val container = NavigatorContainer(
 *     navigator = navigator,
 *     initialDestination = HomeDestination,
 *     debuggable = BuildConfig.DEBUG
 * )
 * 
 * // In Compose
 * val state by container.store.subscribe { action ->
 *     when (action) {
 *         is NavigationAction.ShowError -> /* handle error */
 *     }
 * }
 * ```
 * 
 * @param navigator The Quo Vadis Navigator instance to wrap
 * @param initialDestination Optional initial destination (null = not initialized)
 * @param debuggable Whether to enable debug logging (default: false)
 */
class NavigatorContainer(
    private val navigator: Navigator,
    initialDestination: com.jermey.quo.vadis.core.navigation.core.Destination? = null,
    private val debuggable: Boolean = false
) : Container<NavigationState, NavigationIntent, NavigationAction> {

    /**
     * Internal state implementation.
     */
    private data class NavigatorState(
        override val currentDestination: com.jermey.quo.vadis.core.navigation.core.Destination?,
        override val backStackSize: Int
    ) : NavigationState

    /**
     * The FlowMVI store managing navigation state.
     */
    override val store: Store<NavigationState, NavigationIntent, NavigationAction> = store(
        initial = NavigatorState(
            currentDestination = initialDestination,
            backStackSize = if (initialDestination != null) 1 else 0
        )
    ) {
        configure {
            debuggable = this@NavigatorContainer.debuggable
            name = "NavigatorStore"
            parallelIntents = false // Process navigation intents sequentially
        }

        // Initialize: set up navigator synchronization
        init {
            // Navigate to initial destination if provided
            if (initialDestination != null) {
                navigator.navigate(initialDestination)
            }
            
            // Sync Navigator's state changes to Store state
            navigator.currentDestination
                .onEach { destination ->
                    updateState {
                        NavigatorState(
                            currentDestination = destination,
                            backStackSize = navigator.state.value.activeStack()?.children?.size ?: 0
                        )
                    }
                }
                .launchIn(this)
        }

        // Reduce: handle navigation intents
        reduce { intent ->
            when (intent) {
                is NavigationIntent.Navigate -> handleNavigate(intent)
                is NavigationIntent.NavigateBack -> handleNavigateBack()
                is NavigationIntent.NavigateAndClearTo -> handleNavigateAndClearTo(intent)
                is NavigationIntent.NavigateAndReplace -> handleNavigateAndReplace(intent)
                is NavigationIntent.NavigateAndClearAll -> handleNavigateAndClearAll(intent)
                is NavigationIntent.NavigateToGraph -> handleNavigateToGraph(intent)
            }
        }

        // Recover: handle errors gracefully
        recover { exception ->
            action(
                NavigationAction.NavigationFailed(
                    error = exception,
                    destinationRoute = null
                )
            )
            null // Don't update state on error
        }

        // WhileSubscribed: lifecycle-aware operations
        whileSubscribed {
            // Could add analytics tracking, logging, etc.
        }

        // Enable logging if debuggable
        if (debuggable) {
            enableLogging()
        }
    }

    /**
     * Handle Navigate intent.
     */
    private suspend fun PipelineContext<NavigationState, NavigationIntent, NavigationAction>.handleNavigate(
        intent: NavigationIntent.Navigate
    ) {
        try {
            navigator.navigate(intent.destination, intent.transition)
            // State will be updated via Navigator.currentDestination flow
        } catch (e: Exception) {
            action(
                NavigationAction.NavigationFailed(
                    error = e,
                    destinationRoute = intent.destination::class.simpleName
                )
            )
        }
    }

    /**
     * Handle NavigateBack intent.
     */
    private suspend fun PipelineContext<NavigationState, NavigationIntent, NavigationAction>.handleNavigateBack() {
        try {
            if (navigator.canNavigateBack.value) {
                navigator.navigateBack()
                // State will be updated via Navigator.currentDestination flow
            } else {
                // Already at root, could emit action if needed
            }
        } catch (e: Exception) {
            action(NavigationAction.NavigationFailed(error = e))
        }
    }

    /**
     * Handle NavigateAndClearTo intent.
     */
    private suspend fun PipelineContext<NavigationState, NavigationIntent, NavigationAction>.handleNavigateAndClearTo(
        intent: NavigationIntent.NavigateAndClearTo
    ) {
        try {
            navigator.navigateAndClearTo(
                destination = intent.destination,
                clearRoute = intent.popUpToRoute,
                inclusive = intent.inclusive
            )
            // State will be updated via Navigator.currentDestination flow
        } catch (e: Exception) {
            action(
                NavigationAction.NavigationFailed(
                    error = e,
                    destinationRoute = intent.destination::class.simpleName
                )
            )
        }
    }

    /**
     * Handle NavigateAndReplace intent.
     */
    private suspend fun PipelineContext<NavigationState, NavigationIntent, NavigationAction>.handleNavigateAndReplace(
        intent: NavigationIntent.NavigateAndReplace
    ) {
        try {
            navigator.navigateAndReplace(intent.destination)
            // State will be updated via Navigator.currentDestination flow
        } catch (e: Exception) {
            action(
                NavigationAction.NavigationFailed(
                    error = e,
                    destinationRoute = intent.destination::class.simpleName
                )
            )
        }
    }

    /**
     * Handle NavigateAndClearAll intent.
     */
    private suspend fun PipelineContext<NavigationState, NavigationIntent, NavigationAction>.handleNavigateAndClearAll(
        intent: NavigationIntent.NavigateAndClearAll
    ) {
        try {
            navigator.navigateAndClearAll(intent.destination)
            // State will be updated via Navigator.currentDestination flow
        } catch (e: Exception) {
            action(
                NavigationAction.NavigationFailed(
                    error = e,
                    destinationRoute = intent.destination::class.simpleName
                )
            )
        }
    }

    /**
     * Handle NavigateToGraph intent.
     */
    private suspend fun PipelineContext<NavigationState, NavigationIntent, NavigationAction>.handleNavigateToGraph(
        intent: NavigationIntent.NavigateToGraph
    ) {
        try {
            // Set the graph's start destination then navigate
            val graph = navigator.getDeepLinkHandler()?.let { /* get graph if available */ }
            if (graph != null) {
                navigator.navigate(intent.startDestination)
            } else {
                // Just navigate to start destination
                navigator.navigate(intent.startDestination)
            }
            // State will be updated via Navigator.currentDestination flow
        } catch (e: Exception) {
            action(
                NavigationAction.NavigationFailed(
                    error = e,
                    destinationRoute = intent.graphId
                )
            )
        }
    }
}
