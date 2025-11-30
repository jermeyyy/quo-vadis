package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.Flow

/**
 * A state-driven backstack implementation that integrates directly with Compose's state system.
 *
 * Unlike [MutableBackStack] which uses [kotlinx.coroutines.flow.StateFlow], this implementation
 * uses Compose's [SnapshotStateList] for the entries, allowing direct observation in Compose UI
 * without collecting flows. This enables a Navigation 3-style API where the backstack state
 * drives the UI declaratively.
 *
 * Key features:
 * - **Compose-native state**: Uses [mutableStateListOf] for automatic recomposition
 * - **Derived properties**: [current], [previous], and [canGoBack] are computed using [derivedStateOf]
 * - **Flow interop**: Provides [entriesFlow] for non-Compose consumers via [snapshotFlow]
 * - **Rich manipulation API**: Supports push, pop, insert, remove, swap, move, and more
 *
 * Example usage:
 * ```kotlin
 * val backStack = StateBackStack()
 *
 * // Push destinations
 * backStack.push(HomeDestination)
 * backStack.push(DetailDestination(id = "123"))
 *
 * // Observe in Compose
 * @Composable
 * fun NavHost(backStack: StateBackStack) {
 *     val current = backStack.current
 *     current?.let { entry ->
 *         // Render based on current entry
 *     }
 * }
 * ```
 *
 * @see StateNavigator for a higher-level navigation API built on top of StateBackStack
 * @see BackStack for the flow-based backstack interface
 */
@Stable
class StateBackStack {

    /**
     * The mutable list of backstack entries.
     *
     * This list is backed by Compose's snapshot system, meaning any modifications
     * will automatically trigger recomposition in observing composables.
     *
     * While this property is publicly readable, prefer using the provided methods
     * ([push], [pop], etc.) for modifications to ensure consistency.
     */
    val entries: SnapshotStateList<BackStackEntry> = mutableStateListOf()

    /**
     * The current (topmost) entry in the backstack.
     *
     * Returns `null` if the backstack is empty. This property is computed using
     * [derivedStateOf], so it only triggers recomposition when the actual top
     * entry changes, not on every backstack modification.
     */
    val current: BackStackEntry? by derivedStateOf {
        entries.lastOrNull()
    }

    /**
     * The previous entry in the backstack (second from top).
     *
     * Returns `null` if the backstack has fewer than 2 entries. Useful for
     * implementing preview animations or showing the previous screen during
     * transitions.
     */
    val previous: BackStackEntry? by derivedStateOf {
        if (entries.size > 1) entries[entries.lastIndex - 1] else null
    }

    /**
     * Whether backward navigation is possible.
     *
     * Returns `true` if there are at least 2 entries in the backstack,
     * meaning popping the current entry would leave at least one entry.
     */
    val canGoBack: Boolean by derivedStateOf {
        entries.size > 1
    }

    /**
     * A [Flow] of the current entries list for non-Compose consumers.
     *
     * This flow emits a new list whenever the backstack changes, allowing
     * integration with ViewModels, repositories, or other non-Compose code
     * that needs to observe navigation state.
     *
     * The flow is created using [snapshotFlow], which efficiently bridges
     * Compose's snapshot system to Kotlin coroutines.
     */
    val entriesFlow: Flow<List<BackStackEntry>>
        get() = snapshotFlow { entries.toList() }

    /**
     * Pushes a new destination onto the backstack.
     *
     * Creates a new [BackStackEntry] with a unique ID and adds it to the top
     * of the stack. The entry becomes the new [current] destination.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation for this navigation
     */
    fun push(destination: Destination, transition: NavigationTransition? = null) {
        val entry = BackStackEntry.create(destination, transition)
        entries.add(entry)
    }

    /**
     * Removes and returns the topmost entry from the backstack.
     *
     * @return The removed entry, or `null` if the backstack was empty
     */
    fun pop(): BackStackEntry? {
        return if (entries.isNotEmpty()) {
            entries.removeAt(entries.lastIndex)
        } else {
            null
        }
    }

    /**
     * Removes the entry at the specified index.
     *
     * @param index The index of the entry to remove (0 = bottom of stack)
     * @return The removed entry
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun removeAt(index: Int): BackStackEntry {
        return entries.removeAt(index)
    }

    /**
     * Removes the first entry with the specified ID.
     *
     * @param id The unique identifier of the entry to remove
     * @return `true` if an entry was removed, `false` if no entry with that ID was found
     */
    fun removeById(id: String): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        return if (index >= 0) {
            entries.removeAt(index)
            true
        } else {
            false
        }
    }

    /**
     * Inserts a destination at the specified index.
     *
     * @param index The index at which to insert (0 = bottom of stack)
     * @param destination The destination to insert
     * @param transition Optional transition animation for this entry
     * @throws IndexOutOfBoundsException if index is out of range
     */
    fun insert(index: Int, destination: Destination, transition: NavigationTransition? = null) {
        val entry = BackStackEntry.create(destination, transition)
        entries.add(index, entry)
    }

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
    fun swap(indexA: Int, indexB: Int) {
        val temp = entries[indexA]
        entries[indexA] = entries[indexB]
        entries[indexB] = temp
    }

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
    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val entry = entries.removeAt(fromIndex)
        entries.add(toIndex, entry)
    }

    /**
     * Removes all entries from the backstack.
     *
     * After this operation, [current] will be `null` and [canGoBack] will be `false`.
     */
    fun clear() {
        entries.clear()
    }

    /**
     * Replaces the entire backstack with new destinations.
     *
     * Creates new [BackStackEntry] instances for each destination. The last
     * destination in the list becomes the new [current].
     *
     * @param destinations The list of destinations to populate the backstack with
     */
    fun replaceAll(destinations: List<Destination>) {
        entries.clear()
        destinations.forEach { destination ->
            entries.add(BackStackEntry.create(destination))
        }
    }

    /**
     * Replaces the entire backstack with pre-created entries.
     *
     * Unlike [replaceAll] which creates new entries, this method uses the
     * provided entries directly. Useful for state restoration or when you
     * need to preserve entry IDs.
     *
     * @param newEntries The list of entries to populate the backstack with
     */
    fun replaceAllWithEntries(newEntries: List<BackStackEntry>) {
        entries.clear()
        entries.addAll(newEntries)
    }

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
}
