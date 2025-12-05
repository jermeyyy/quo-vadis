package com.jermey.quo.vadis.core.navigation.serialization

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import com.jermey.quo.vadis.core.navigation.core.NavNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Android implementation of StateRestoration using SavedStateHandle.
 *
 * Integrates with ViewModel SavedStateHandle for automatic process death survival.
 * State is persisted as JSON string in the SavedStateHandle, which Android
 * automatically saves/restores across configuration changes and process death.
 *
 * ## Usage with ViewModel
 *
 * ```kotlin
 * class NavigationViewModel(
 *     savedStateHandle: SavedStateHandle
 * ) : ViewModel() {
 *     private val stateRestoration = AndroidStateRestoration(savedStateHandle)
 *
 *     val navigator = TreeNavigator(
 *         initialState = runBlocking { stateRestoration.restoreState() }
 *             ?: createDefaultState()
 *     ).also { nav ->
 *         stateRestoration.enableAutoSave(nav.state)
 *     }
 *
 *     override fun onCleared() {
 *         super.onCleared()
 *         stateRestoration.disableAutoSave()
 *     }
 * }
 * ```
 *
 * ## Size Limitations
 *
 * Android has a ~1MB limit for saved state across the entire app.
 * For complex navigation states, consider:
 * - Storing only essential state (keys, indices)
 * - Using compression for large trees
 * - Caching full state to disk and only saving a reference
 *
 * @param savedStateHandle The SavedStateHandle from ViewModel
 * @param key The key under which to store the navigation state
 * @param debounceMillis Debounce time for auto-save in milliseconds
 *
 * @see StateRestoration for the interface contract
 */
class AndroidStateRestoration(
    private val savedStateHandle: SavedStateHandle,
    private val key: String = NAV_STATE_KEY,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MS
) : StateRestoration {

    private var autoSaveJob: Job? = null
    private var autoSaveScope: CoroutineScope? = null

    override val autoSaveEnabled: Boolean
        get() = autoSaveJob?.isActive == true

    override suspend fun saveState(state: NavNode) {
        val json = NavNodeSerializer.toJson(state)
        savedStateHandle[key] = json
    }

    override suspend fun restoreState(): NavNode? {
        val json: String? = savedStateHandle[key]
        return NavNodeSerializer.fromJsonOrNull(json)
    }

    override suspend fun clearState() {
        savedStateHandle.remove<String>(key)
    }

    override fun enableAutoSave(stateFlow: StateFlow<NavNode>) {
        // Cancel any existing auto-save
        disableAutoSave()

        // Create a new scope for auto-save
        autoSaveScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        autoSaveJob = autoSaveScope?.launch {
            if (debounceMillis > 0) {
                stateFlow
                    .debounce(debounceMillis)
                    .collectLatest { state ->
                        try {
                            saveState(state)
                        } catch (e: Exception) {
                            // Log but don't crash - state persistence is best-effort
                            println("AndroidStateRestoration: Failed to save state - ${e.message}")
                        }
                    }
            } else {
                stateFlow.collectLatest { state ->
                    try {
                        saveState(state)
                    } catch (e: Exception) {
                        println("AndroidStateRestoration: Failed to save state - ${e.message}")
                    }
                }
            }
        }
    }

    override fun disableAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = null
        autoSaveScope?.cancel()
        autoSaveScope = null
    }

    companion object {
        /**
         * Default key for storing navigation state in SavedStateHandle.
         */
        const val NAV_STATE_KEY = "quo_vadis_nav_state"

        /**
         * Default debounce time for auto-save (100ms).
         * Balances responsiveness with avoiding excessive writes.
         */
        const val DEFAULT_DEBOUNCE_MS = 100L
    }
}

/**
 * Extension function to restore NavNode from a Bundle.
 *
 * Useful for restoring navigation state in Activity/Fragment lifecycle
 * callbacks where SavedStateHandle is not available.
 *
 * ```kotlin
 * override fun onCreate(savedInstanceState: Bundle?) {
 *     super.onCreate(savedInstanceState)
 *     val restoredState = savedInstanceState?.restoreNavState()
 *     // Initialize navigator with restored state or default
 * }
 * ```
 *
 * @param key The key under which the state was saved
 * @return The restored NavNode, or null if not found or parsing fails
 */
fun Bundle.restoreNavState(
    key: String = AndroidStateRestoration.NAV_STATE_KEY
): NavNode? {
    val json = getString(key) ?: return null
    return NavNodeSerializer.fromJsonOrNull(json)
}

/**
 * Extension function to save NavNode to a Bundle.
 *
 * Useful for saving navigation state in Activity/Fragment lifecycle
 * callbacks where SavedStateHandle is not available.
 *
 * ```kotlin
 * override fun onSaveInstanceState(outState: Bundle) {
 *     super.onSaveInstanceState(outState)
 *     outState.saveNavState(navigator.state.value)
 * }
 * ```
 *
 * @param state The NavNode tree to save
 * @param key The key under which to store the state
 */
fun Bundle.saveNavState(
    state: NavNode,
    key: String = AndroidStateRestoration.NAV_STATE_KEY
) {
    putString(key, NavNodeSerializer.toJson(state))
}

/**
 * Create a NavNode from a Bundle's saved state.
 *
 * Standalone function alternative to the Bundle extension.
 *
 * @param bundle The Bundle containing saved state, or null
 * @param key The key under which the state was saved
 * @return The restored NavNode, or null if not found or parsing fails
 */
fun createStateRestorationFromBundle(
    bundle: Bundle?,
    key: String = AndroidStateRestoration.NAV_STATE_KEY
): NavNode? {
    val json = bundle?.getString(key) ?: return null
    return NavNodeSerializer.fromJsonOrNull(json)
}
