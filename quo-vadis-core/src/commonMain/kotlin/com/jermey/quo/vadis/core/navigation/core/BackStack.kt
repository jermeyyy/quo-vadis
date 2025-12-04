package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents the navigation backstack with direct access and modification capabilities.
 * Allows for advanced navigation scenarios like clearing stack, replacing, etc.
 *
 * This interface supports both traditional flow-based observation via [stack], [current],
 * [previous], and [canGoBack] StateFlows, as well as direct state manipulation via
 * the [entries] SnapshotStateList.
 *
 * The [entries] list can be directly observed in Compose UI without collecting flows,
 * enabling declarative navigation where the backstack state drives the UI.
 */
@Stable
interface BackStack {
    // ═══════════════════════════════════════════════════════════════
    // Direct State Access
    // ═══════════════════════════════════════════════════════════════

    /**
     * Direct access to the mutable list of backstack entries.
     *
     * This list is backed by Compose's snapshot system, meaning any modifications
     * will automatically trigger recomposition in observing composables.
     *
     * For advanced manipulation scenarios, you can directly modify this list.
     * For standard navigation, prefer using the provided methods ([push], [pop], etc.).
     */
    val entries: SnapshotStateList<BackStackEntry>

    /**
     * Returns the number of entries in the backstack.
     */
    val size: Int
        get() = entries.size

    /**
     * Returns `true` if the backstack is empty.
     */
    val isEmpty: Boolean
        get() = entries.isEmpty()

    /**
     * Returns `true` if the backstack is not empty.
     */
    val isNotEmpty: Boolean
        get() = entries.isNotEmpty()

    // ═══════════════════════════════════════════════════════════════
    // Flow-Based State
    // ═══════════════════════════════════════════════════════════════

    /**
     * Current stack as a flow of entries.
     * Derived from [entries] for compatibility with flow-based consumers.
     */
    val stack: StateFlow<List<BackStackEntry>>

    /**
     * Current top entry in the stack.
     */
    val current: StateFlow<BackStackEntry?>

    /**
     * Previous entry in the stack (if any).
     */
    val previous: StateFlow<BackStackEntry?>

    /**
     * Whether we can navigate back.
     */
    val canGoBack: StateFlow<Boolean>

    // ═══════════════════════════════════════════════════════════════
    // Standard Navigation Operations
    // ═══════════════════════════════════════════════════════════════

    /**
     * Push a destination onto the stack.
     */
    fun push(destination: Destination, transition: NavigationTransition? = null)

    /**
     * Pop the current destination from the stack.
     */
    fun pop(): Boolean

    /**
     * Pop until a destination matching the predicate is found.
     */
    fun popUntil(predicate: (Destination) -> Boolean): Boolean

    /**
     * Replace the current destination.
     */
    fun replace(destination: Destination, transition: NavigationTransition? = null)

    /**
     * Replace the entire stack with new destinations.
     */
    fun replaceAll(destinations: List<Destination>)

    /**
     * Clear the entire stack.
     */
    fun clear()

    /**
     * Pop to the root (first) destination.
     */
    fun popToRoot(): Boolean

    // ═══════════════════════════════════════════════════════════════
    // Advanced Stack Manipulation
    // ═══════════════════════════════════════════════════════════════

    /**
     * Inserts a destination at the specified index.
     *
     * @param index The index at which to insert (0 = bottom of stack)
     * @param destination The destination to insert
     * @param transition Optional transition animation for this entry
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun insert(index: Int, destination: Destination, transition: NavigationTransition? = null)

    /**
     * Removes the entry at the specified index.
     *
     * @param index The index of the entry to remove (0 = bottom of stack)
     * @return The removed entry
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun removeAt(index: Int): BackStackEntry

    /**
     * Removes the first entry with the specified ID.
     *
     * @param id The unique identifier of the entry to remove
     * @return `true` if an entry was removed, `false` if no entry with that ID was found
     */
    fun removeById(id: String): Boolean

    /**
     * Swaps two entries in the backstack by their indices.
     *
     * This operation is atomic from Compose's perspective - both changes
     * happen within the same snapshot.
     *
     * @param indexA The index of the first entry
     * @param indexB The index of the second entry
     * @throws IndexOutOfBoundsException if either index is out of range
     */
    fun swap(indexA: Int, indexB: Int)

    /**
     * Moves an entry from one position to another.
     *
     * This is useful for reordering the backstack, for example when implementing
     * tab-based navigation where the order matters.
     *
     * @param fromIndex The current index of the entry to move
     * @param toIndex The target index for the entry
     * @throws IndexOutOfBoundsException if either index is out of range
     */
    fun move(fromIndex: Int, toIndex: Int)

    /**
     * Replaces the entire backstack with pre-created entries.
     *
     * Unlike [replaceAll] which creates new entries, this method uses the
     * provided entries directly. Useful for state restoration or when you
     * need to preserve entry IDs.
     *
     * @param entries The list of entries to populate the backstack with
     */
    fun replaceAllWithEntries(entries: List<BackStackEntry>)
}

/**
 * Key for storing the selected tab route in a [BackStackEntry]'s extras.
 *
 * This constant is used to persist and restore the selected tab when navigating
 * back to a tabbed navigation destination.
 */
const val EXTRA_SELECTED_TAB_ROUTE = "quo_vadis_selected_tab_route"

/**
 * Entry in the backstack.
 *
 * Each entry represents a destination in the navigation stack along with its associated state.
 * The [extras] map can be used to store arbitrary data that should persist across navigation
 * and be restored when returning to this entry.
 *
 * @property id Unique identifier for this entry
 * @property destination The navigation destination this entry represents
 * @property savedState State saved for this entry (used for state restoration)
 * @property transition The transition animation to use when navigating to/from this entry
 * @property isPopping Whether this entry is currently being popped from the stack
 * @property extras Mutable map for storing arbitrary data that persists with this entry
 */
data class BackStackEntry(
    val id: String = generateId(),
    val destination: Destination,
    val savedState: Map<String, Any?> = emptyMap(),
    val transition: NavigationTransition? = null,
    val isPopping: Boolean = false,
    val extras: MutableMap<String, Any?> = mutableMapOf()
) {
    companion object {
        fun create(
            destination: Destination,
            transition: NavigationTransition? = null
        ): BackStackEntry {
            return BackStackEntry(
                id = generateId(),
                destination = destination,
                transition = transition
            )
        }

        @OptIn(ExperimentalUuidApi::class)
        private fun generateId(): String {
            return Uuid.random().toString()
        }
    }
}

/**
 * Retrieves an extra value from this [BackStackEntry].
 *
 * @param key The key associated with the extra value
 * @return The value associated with the key, or `null` if not found
 */
fun BackStackEntry.getExtra(key: String): Any? = extras[key]

/**
 * Stores an extra value in this [BackStackEntry].
 *
 * @param key The key to associate with the value
 * @param value The value to store (can be `null` to remove the entry)
 */
fun BackStackEntry.setExtra(key: String, value: Any?) {
    extras[key] = value
}

/**
 * Mutable implementation of BackStack using SnapshotStateList internally.
 *
 * This implementation provides both direct Compose state observation via [entries]
 * and flow-based observation via [stack], [current], [previous], and [canGoBack].
 * The StateFlow properties are updated synchronously when the entries list changes.
 *
 * Key features:
 * - **Compose-native state**: Uses [mutableStateListOf] for automatic recomposition
 * - **Flow interop**: Provides StateFlow properties that sync with the snapshot state
 * - **Rich manipulation API**: Supports push, pop, insert, remove, swap, move, and more
 */
class MutableBackStack : BackStack {

    // ═══════════════════════════════════════════════════════════════
    // Internal State (SnapshotStateList as source of truth)
    // ═══════════════════════════════════════════════════════════════

    override val entries: SnapshotStateList<BackStackEntry> = mutableStateListOf()

    // ═══════════════════════════════════════════════════════════════
    // Derived StateFlow Properties (for flow-based consumers)
    // Updated synchronously when entries change via updateFlows()
    // ═══════════════════════════════════════════════════════════════

    private val _stack = MutableStateFlow<List<BackStackEntry>>(emptyList())
    override val stack: StateFlow<List<BackStackEntry>> = _stack

    private val _current = MutableStateFlow<BackStackEntry?>(null)
    override val current: StateFlow<BackStackEntry?> = _current

    private val _previous = MutableStateFlow<BackStackEntry?>(null)
    override val previous: StateFlow<BackStackEntry?> = _previous

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack

    private fun updateFlows() {
        _stack.value = entries.toList()
        _current.value = entries.lastOrNull()
        _previous.value = if (entries.size > 1) entries[entries.lastIndex - 1] else null
        _canGoBack.value = entries.size > 1
    }

    // ═══════════════════════════════════════════════════════════════
    // Standard Navigation Operations
    // ═══════════════════════════════════════════════════════════════

    override fun push(destination: Destination, transition: NavigationTransition?) {
        val entry = BackStackEntry.create(destination, transition)
        entries.add(entry)
        updateFlows()
    }

    override fun pop(): Boolean {
        if (entries.isEmpty()) return false
        entries.removeAt(entries.lastIndex)
        updateFlows()
        return true
    }

    override fun popUntil(predicate: (Destination) -> Boolean): Boolean {
        val index = entries.indexOfLast { predicate(it.destination) }
        if (index == -1) return false

        // Remove all entries after the matching one
        while (entries.size > index + 1) {
            entries.removeAt(entries.lastIndex)
        }
        updateFlows()
        return true
    }

    override fun replace(destination: Destination, transition: NavigationTransition?) {
        if (entries.isEmpty()) {
            push(destination, transition)
            return
        }

        val entry = BackStackEntry.create(destination, transition)
        entries[entries.lastIndex] = entry
        updateFlows()
    }

    override fun replaceAll(destinations: List<Destination>) {
        entries.clear()
        destinations.forEach { destination ->
            entries.add(BackStackEntry.create(destination))
        }
        updateFlows()
    }

    override fun clear() {
        entries.clear()
        updateFlows()
    }

    override fun popToRoot(): Boolean {
        if (entries.size <= 1) return false

        while (entries.size > 1) {
            entries.removeAt(entries.lastIndex)
        }
        updateFlows()
        return true
    }

    // ═══════════════════════════════════════════════════════════════
    // Advanced Stack Manipulation
    // ═══════════════════════════════════════════════════════════════

    override fun insert(index: Int, destination: Destination, transition: NavigationTransition?) {
        val entry = BackStackEntry.create(destination, transition)
        entries.add(index, entry)
        updateFlows()
    }

    override fun removeAt(index: Int): BackStackEntry {
        val entry = entries.removeAt(index)
        updateFlows()
        return entry
    }

    override fun removeById(id: String): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        return if (index >= 0) {
            entries.removeAt(index)
            updateFlows()
            true
        } else {
            false
        }
    }

    override fun swap(indexA: Int, indexB: Int) {
        val temp = entries[indexA]
        entries[indexA] = entries[indexB]
        entries[indexB] = temp
        updateFlows()
    }

    override fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val entry = entries.removeAt(fromIndex)
        entries.add(toIndex, entry)
        updateFlows()
    }

    override fun replaceAllWithEntries(entries: List<BackStackEntry>) {
        this.entries.clear()
        this.entries.addAll(entries)
        updateFlows()
    }
}
