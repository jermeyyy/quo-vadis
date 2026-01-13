package com.jermey.navplayground.demo.ui.screens.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Types of filter changes that can occur when updating the explore grid.
 */
enum class FilterChangeType {
    /** Initial load, no previous state to compare. */
    INITIAL,

    /** No changes detected between previous and current items. */
    NONE,

    /** New items were added (filter relaxed or new content loaded). */
    ITEMS_ADDED,

    /** Items were removed (filter applied more strictly). */
    ITEMS_REMOVED,

    /** Both additions and removals occurred (different filter applied). */
    BOTH
}

/**
 * Tracks filter state changes to animate grid content appropriately.
 *
 * This class helps determine what kind of animation to apply when the
 * filtered items list changes:
 * - [FilterChangeType.INITIAL]: First load, fade in all items
 * - [FilterChangeType.ITEMS_ADDED]: New items appeared, animate them in
 * - [FilterChangeType.ITEMS_REMOVED]: Items disappeared, animate them out
 * - [FilterChangeType.BOTH]: Items changed, crossfade transition
 * - [FilterChangeType.NONE]: No change, skip animation
 *
 * Usage:
 * ```kotlin
 * val filterState = rememberExploreFilterState()
 * val changeType = filterState.update(currentItems.map { it.id }.toSet())
 *
 * when (changeType) {
 *     FilterChangeType.ITEMS_ADDED -> // animate new items in
 *     FilterChangeType.ITEMS_REMOVED -> // animate removed items out
 *     // ...
 * }
 * ```
 */
@Stable
class ExploreFilterState {
    /**
     * Set of item IDs from the previous update.
     */
    var previousItemIds: Set<String> = emptySet()
        private set

    /**
     * Whether this is the first update (no previous state).
     */
    private var isFirstUpdate: Boolean = true

    /**
     * Updates the tracked state and returns the type of change detected.
     *
     * @param currentIds Set of current item IDs after filtering
     * @return The type of change detected between previous and current state
     */
    fun update(currentIds: Set<String>): FilterChangeType {
        if (isFirstUpdate) {
            isFirstUpdate = false
            previousItemIds = currentIds
            return FilterChangeType.INITIAL
        }

        val added = currentIds - previousItemIds
        val removed = previousItemIds - currentIds

        val changeType = when {
            added.isEmpty() && removed.isEmpty() -> FilterChangeType.NONE
            added.isNotEmpty() && removed.isNotEmpty() -> FilterChangeType.BOTH
            added.isNotEmpty() -> FilterChangeType.ITEMS_ADDED
            else -> FilterChangeType.ITEMS_REMOVED
        }

        previousItemIds = currentIds
        return changeType
    }

    /**
     * Checks if a specific item is new (wasn't in the previous state).
     *
     * @param itemId The ID of the item to check
     * @return true if the item is newly added
     */
    fun isNewItem(itemId: String): Boolean = itemId !in previousItemIds

    /**
     * Resets the state to initial (useful for pull-to-refresh).
     */
    fun reset() {
        previousItemIds = emptySet()
        isFirstUpdate = true
    }
}

/**
 * Creates and remembers an [ExploreFilterState] instance.
 *
 * @return A remembered instance of ExploreFilterState
 */
@Composable
fun rememberExploreFilterState(): ExploreFilterState {
    return remember { ExploreFilterState() }
}
