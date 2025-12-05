package com.jermey.quo.vadis.core.navigation.serialization

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
 * Interface for platform-specific state restoration.
 *
 * Implementations handle saving and restoring NavNode state
 * in a platform-appropriate manner:
 * - Android: SavedStateHandle integration for process death survival
 * - iOS: NSUserDefaults or State Restoration API
 * - Web: localStorage
 * - Desktop: FileSystem-based storage
 *
 * ## Basic Usage
 *
 * ```kotlin
 * class NavigationViewModel(
 *     private val stateRestoration: StateRestoration
 * ) {
 *     val navigator = TreeNavigator(
 *         initialState = runBlocking { stateRestoration.restoreState() }
 *             ?: createDefaultState()
 *     )
 *
 *     init {
 *         // Enable automatic state persistence
 *         stateRestoration.enableAutoSave(navigator.state)
 *     }
 * }
 * ```
 *
 * ## Auto-Save Behavior
 *
 * When [enableAutoSave] is called, the implementation will observe the provided
 * StateFlow and automatically persist state changes. Implementations should:
 * - Debounce rapid changes to avoid excessive I/O
 * - Handle errors gracefully without crashing the app
 * - Respect platform limitations (e.g., Android Bundle size limits)
 *
 * @see AndroidStateRestoration for Android implementation
 * @see InMemoryStateRestoration for testing
 */
interface StateRestoration {

    /**
     * Save the navigation state.
     *
     * @param state The NavNode tree to persist
     */
    suspend fun saveState(state: NavNode)

    /**
     * Restore the navigation state.
     *
     * @return The previously saved NavNode tree, or null if no state exists
     *         or if restoration fails
     */
    suspend fun restoreState(): NavNode?

    /**
     * Clear any saved navigation state.
     */
    suspend fun clearState()

    /**
     * Whether auto-save is currently enabled.
     */
    val autoSaveEnabled: Boolean

    /**
     * Enable auto-save that persists state on every change.
     *
     * The implementation should:
     * - Debounce rapid changes (recommended: 100-500ms)
     * - Handle errors gracefully
     * - Stop previous auto-save if called again
     *
     * @param stateFlow The StateFlow to observe for state changes
     */
    fun enableAutoSave(stateFlow: StateFlow<NavNode>)

    /**
     * Disable auto-save.
     *
     * After calling this, state changes will no longer be automatically persisted.
     * Manual [saveState] calls will still work.
     */
    fun disableAutoSave()
}

/**
 * In-memory implementation of StateRestoration for testing.
 *
 * This implementation stores state in memory without any persistence.
 * Useful for:
 * - Unit tests
 * - UI previews
 * - Development without persistence overhead
 *
 * ## Usage
 *
 * ```kotlin
 * @Test
 * fun `navigation state persists across simulated recreation`() = runTest {
 *     val stateRestoration = InMemoryStateRestoration()
 *
 *     // Save state
 *     val testState = StackNode(key = "root", parentKey = null)
 *     stateRestoration.saveState(testState)
 *
 *     // Simulate recreation
 *     val restored = stateRestoration.restoreState()
 *
 *     assertEquals(testState, restored)
 * }
 * ```
 *
 * @param debounceMillis Debounce time for auto-save in milliseconds.
 *                       Set to 0 to disable debouncing (immediate save).
 */
class InMemoryStateRestoration(
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MS
) : StateRestoration {

    private var savedState: NavNode? = null
    private var autoSaveJob: Job? = null
    private var autoSaveScope: CoroutineScope? = null

    override val autoSaveEnabled: Boolean
        get() = autoSaveJob?.isActive == true

    override suspend fun saveState(state: NavNode) {
        savedState = state
    }

    override suspend fun restoreState(): NavNode? {
        return savedState
    }

    override suspend fun clearState() {
        savedState = null
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
                        saveState(state)
                    }
            } else {
                stateFlow.collectLatest { state ->
                    saveState(state)
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

    /**
     * Returns the last saved state for testing inspection.
     * This is specific to InMemoryStateRestoration and not part of the interface.
     */
    fun getLastSavedState(): NavNode? = savedState

    companion object {
        /**
         * Default debounce time for auto-save (100ms).
         * Balances responsiveness with I/O efficiency.
         */
        const val DEFAULT_DEBOUNCE_MS = 100L
    }
}

/**
 * No-op implementation of StateRestoration.
 *
 * Use when state persistence is not needed but the API expects
 * a StateRestoration instance.
 */
object NoOpStateRestoration : StateRestoration {
    override val autoSaveEnabled: Boolean = false
    override suspend fun saveState(state: NavNode) = Unit
    override suspend fun restoreState(): NavNode? = null
    override suspend fun clearState() = Unit
    override fun enableAutoSave(stateFlow: StateFlow<NavNode>) = Unit
    override fun disableAutoSave() = Unit
}
