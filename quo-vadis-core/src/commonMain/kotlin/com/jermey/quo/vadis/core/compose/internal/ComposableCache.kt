@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.quo.vadis.core.InternalQuoVadisApi

/**
 * Cache for composables associated with NavNode keys.
 *
 * Keeps composables alive to enable smooth transitions and predictive back gestures.
 * Uses NavNode key-based caching for the hierarchical rendering system.
 *
 * The cache supports two protection mechanisms:
 * - **Locked entries**: Temporarily protected during animations (via [lockEntry]/[unlockEntry] or [lock]/[unlock])
 * - **Priority entries**: Permanently protected from eviction (via [setPriority])
 *
 * Priority entries are intended for screens that contain nested navigators (e.g., tabbed screens).
 * These screens should not be evicted as recreating them causes visual glitches during back navigation.
 *
 * Entries are never automatically evicted. Use [removeEntry] to explicitly remove entries
 * when a node is permanently removed from the navigation tree.
 */
@InternalQuoVadisApi
@Stable
class ComposableCache {
    private val lockedEntries = mutableStateSetOf<String>()
    private val priorityEntries = mutableStateSetOf<String>()

    /**
     * Movable content entries per key. Each entry wraps screen content in
     * [movableContentOf] to ensure stable `currentCompositeKeyHash` across
     * different call sites (AnimatedContent vs predictive back underlay).
     */
    private val movableScreens =
        mutableMapOf<String, @Composable (content: @Composable () -> Unit) -> Unit>()

    // TODO: Replace with AtomicLong when kotlinx-atomicfu is added as a dependency
    private var counter = 0L

    /**
     * Lock an entry to prevent it from being evicted during animations.
     * Locked entries are protected from cache cleanup.
     *
     * @param entryId The ID of the entry to lock
     */
    fun lockEntry(entryId: String) {
        lockedEntries.add(entryId)
    }

    /**
     * Unlock an entry, allowing it to be evicted if needed.
     *
     * @param entryId The ID of the entry to unlock
     */
    fun unlockEntry(entryId: String) {
        lockedEntries.remove(entryId)
    }

    /**
     * Lock an entry by key to prevent eviction during animations.
     *
     * This is the preferred method for hierarchical rendering, working directly
     * with NavNode keys rather than BackStackEntry IDs.
     *
     * ## Usage
     *
     * ```kotlin
     * // Lock during animation
     * cache.lock(node.key)
     * animateTransition()
     * cache.unlock(node.key)
     * ```
     *
     * @param key Unique key for the cached content (typically NavNode.key)
     */
    fun lock(key: String) {
        lockedEntries.add(key)
    }

    /**
     * Unlock an entry by key, allowing eviction if needed.
     *
     * Should be called after animation completes to allow cache cleanup.
     *
     * @param key Unique key for the cached content (typically NavNode.key)
     */
    fun unlock(key: String) {
        lockedEntries.remove(key)
    }

    /**
     * Set the priority status for an entry.
     * Priority entries are never evicted from the cache, regardless of access time.
     *
     * Use this for screens that contain nested navigators (e.g., tabbed navigation screens).
     * These screens are expensive to recreate and their eviction can cause visual glitches
     * during back navigation or predictive back gestures.
     *
     * @param entryId The ID of the entry to mark as priority
     * @param isPriority Whether the entry should be treated as priority (true) or not (false)
     */
    fun setPriority(entryId: String, isPriority: Boolean) {
        if (isPriority) {
            priorityEntries.add(entryId)
        } else {
            priorityEntries.remove(entryId)
        }
    }

    /**
     * Explicitly remove a cached entry and its saved state.
     * Called when a node is permanently removed from the navigation tree.
     *
     * @param key The key of the entry to remove
     * @param saveableStateHolder State holder to clear saved state from
     */
    fun removeEntry(key: String, saveableStateHolder: SaveableStateHolder) {
        saveableStateHolder.removeState(key)
        movableScreens.remove(key)
        lockedEntries.remove(key)
        priorityEntries.remove(key)
    }

    /**
     * Renders cached content for a NavNode key.
     *
     * ## State Preservation
     *
     * Content rendered through this method maintains its state across
     * recompositions and navigation transitions. The [SaveableStateHolder]
     * persists state using the provided key.
     *
     * ## Example
     *
     * ```kotlin
     * cache.CachedEntry(
     *     key = screenNode.key,
     *     saveableStateHolder = saveableStateHolder
     * ) {
     *     ScreenContent(screenNode.destination)
     * }
     * ```
     *
     * @param key Unique key for the cached content (typically NavNode.key)
     * @param saveableStateHolder State holder for saveable state
     * @param content The composable content to render
     */
    @Composable
    fun CachedEntry(
        key: String,
        saveableStateHolder: SaveableStateHolder,
        content: @Composable () -> Unit
    ) {
        // Use movableContentOf so the composition subtree (including
        // SaveableStateProvider and all rememberSaveable calls) keeps a
        // stable currentCompositeKeyHash regardless of where it is called.
        // This lets the predictive back underlay and AnimatedContent share
        // the same movable instance — when one stops calling it and the
        // other starts, Compose *moves* the composition, preserving all
        // saved state with matching hash keys.
        val movable = movableScreens.getOrPut(key) {
            movableContentOf { innerContent: @Composable () -> Unit ->
                saveableStateHolder.SaveableStateProvider(key) {
                    innerContent()
                }
            }
        }
        movable(content)
    }
}

/**
 * Remember a composable cache across recompositions.
 */
@Composable
fun rememberComposableCache(): ComposableCache {
    return remember { ComposableCache() }
}


