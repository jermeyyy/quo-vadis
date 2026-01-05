@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder

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
 * ## Cache Eviction Strategy
 *
 * When the cache exceeds [maxCacheSize], the oldest non-protected entry is evicted.
 * Eviction is LRU-based (Least Recently Used), where access time is updated on each composition.
 *
 * @param maxCacheSize Maximum number of composables to keep in cache (default 5)
 */
@InternalQuoVadisApi
@Stable
class ComposableCache(
    private val maxCacheSize: Int = 5
) {
    private val accessTimeMap = mutableStateMapOf<String, Long>()
    private val lockedEntries = mutableStateSetOf<String>()
    private val priorityEntries = mutableStateSetOf<String>()
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
        DisposableEffect(key) {
            // Update access time on composition
            accessTimeMap[key] = counter++

            // Cleanup old entries if cache is full
            if (accessTimeMap.size > maxCacheSize) {
                val oldestEntry = accessTimeMap.entries
                    .filter { it.key !in lockedEntries && it.key !in priorityEntries }
                    .minByOrNull { it.value }
                oldestEntry?.let { (oldId, _) ->
                    if (oldId != key) {
                        accessTimeMap.remove(oldId)
                        saveableStateHolder.removeState(oldId)
                    }
                }
            }

            onDispose {
                // Don't remove on dispose - keep in cache
            }
        }

        // Render with state preservation
        saveableStateHolder.SaveableStateProvider(key) {
            content()
        }
    }
}

/**
 * Remember a composable cache across recompositions.
 */
@Composable
fun rememberComposableCache(maxCacheSize: Int = 5): ComposableCache {
    return remember { ComposableCache(maxCacheSize) }
}


