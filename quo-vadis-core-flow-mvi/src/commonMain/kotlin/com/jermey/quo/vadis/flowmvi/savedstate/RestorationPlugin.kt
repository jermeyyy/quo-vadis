package com.jermey.quo.vadis.flowmvi.savedstate

import com.jermey.quo.vadis.flowmvi.core.NavigationAction
import com.jermey.quo.vadis.flowmvi.core.NavigationIntent
import com.jermey.quo.vadis.flowmvi.core.NavigationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * State restoration manager for navigation state.
 * 
 * Provides manual restore and auto-save functionality.
 * 
 * Usage:
 * ```kotlin
 * val restorationManager = NavigationStateRestorationManager(
 *     key = "main_navigation",
 *     serializer = MyNavigationStateSerializer(),
 *     stateManager = createPlatformSavedStateManager()
 * )
 * 
 * // Restore initial state
 * val initialState = restorationManager.restoreState() ?: defaultState
 * 
 * // Start auto-save
 * restorationManager.startAutoSave(scope, navigator.state)
 * ```
 */
class NavigationStateRestorationManager(
    private val key: String,
    private val serializer: NavigationStateSerializer = DefaultNavigationStateSerializer(),
    private val stateManager: SavedStateManager = createPlatformSavedStateManager()
) {
    private var saveJob: Job? = null
    
    /**
     * Restore navigation state from saved state.
     * 
     * @return Restored state or null if not found/failed
     */
    suspend fun restoreState(): NavigationState? {
        return try {
            val json = stateManager.restore(key)
            json?.toNavigationState(serializer)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Save navigation state.
     * 
     * @param state State to save
     */
    suspend fun saveState(state: NavigationState) {
        try {
            val json = state.toJsonString(serializer)
            stateManager.save(key, json)
        } catch (e: Exception) {
            // Save failed, ignore
        }
    }
    
    /**
     * Start auto-saving state on changes.
     * 
     * @param scope Coroutine scope
     * @param stateFlow State flow to observe
     */
    fun startAutoSave(scope: CoroutineScope, stateFlow: StateFlow<NavigationState>) {
        saveJob?.cancel()
        saveJob = scope.launch {
            stateFlow.collect { state ->
                saveState(state)
            }
        }
    }
    
    /**
     * Stop auto-saving.
     */
    fun stopAutoSave() {
        saveJob?.cancel()
        saveJob = null
    }
    
    /**
     * Clear saved state.
     */
    suspend fun clearState() {
        stateManager.delete(key)
    }
    
    /**
     * Check if saved state exists.
     */
    suspend fun hasState(): Boolean {
        return stateManager.exists(key)
    }
}

/**
 * Clear saved navigation state.
 */
suspend fun clearNavigationState(
    key: String = "navigation_state",
    stateManager: SavedStateManager = createPlatformSavedStateManager()
) {
    stateManager.delete(key)
}

/**
 * Check if saved navigation state exists.
 */
suspend fun hasNavigationState(
    key: String = "navigation_state",
    stateManager: SavedStateManager = createPlatformSavedStateManager()
): Boolean {
    return stateManager.exists(key)
}
