package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry

/**
 * Cache for composables associated with backstack entries.
 * Keeps composables alive to enable smooth transitions and predictive back gestures.
 *
 * @param maxCacheSize Maximum number of composables to keep in cache (default 3)
 */
@Stable
class ComposableCache(
    private val maxCacheSize: Int = 3
) {
    private val accessTimeMap = mutableStateMapOf<String, Long>()
    private val lockedEntries = mutableStateSetOf<String>()
    private var counter = 0L

    /**
     * Lock an entry to prevent it from being evicted during animations.
     * Locked entries are protected from cache cleanup.
     */
    fun lockEntry(entryId: String) {
        lockedEntries.add(entryId)
    }

    /**
     * Unlock an entry, allowing it to be evicted if needed.
     */
    fun unlockEntry(entryId: String) {
        lockedEntries.remove(entryId)
    }

    /**
     * Renders a cached composable for the given entry.
     *
     * @param entry The backstack entry
     * @param saveableStateHolder State holder for saveable state
     * @param content The composable content provider
     */
    @Composable
    fun Entry(
        entry: BackStackEntry,
        saveableStateHolder: SaveableStateHolder,
        content: @Composable (BackStackEntry) -> Unit
    ) {
        // Track access
        val entryId = entry.id

        DisposableEffect(entryId) {
            // Update access time on composition
            accessTimeMap[entryId] = counter++

            // Cleanup old entries if cache is full
            if (accessTimeMap.size > maxCacheSize) {
                val oldestEntry = accessTimeMap.entries
                    .filter { it.key !in lockedEntries } // Don't evict locked entries
                    .minByOrNull { it.value }
                oldestEntry?.let { (oldId, _) ->
                    if (oldId != entryId) {
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
        saveableStateHolder.SaveableStateProvider(entryId) {
            content(entry)
        }
    }
}

/**
 * Remember a composable cache across recompositions.
 */
@Composable
fun rememberComposableCache(maxCacheSize: Int = 3): ComposableCache {
    return remember { ComposableCache(maxCacheSize) }
}


