package com.jermey.navplayground.navigation.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.navplayground.navigation.core.BackStackEntry

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
    private val cache = mutableStateMapOf<String, CachedComposable>()

    /**
     * Get or create a cached composable for the given entry.
     *
     * @param entry The backstack entry
     * @param saveableStateHolder State holder for saveable state
     * @param content The composable content provider
     * @return A composable that wraps the content with state preservation
     */
    @Composable
    fun getOrCreate(
        entry: BackStackEntry,
        saveableStateHolder: SaveableStateHolder,
        content: @Composable (BackStackEntry) -> Unit
    ): @Composable () -> Unit {
        // Update or create cache entry
        if (!cache.containsKey(entry.id)) {
            cache[entry.id] = CachedComposable(
                entry = entry,
                lastAccessTime = System.currentTimeMillis()
            )

            // Cleanup old entries if cache is full
            if (cache.size > maxCacheSize) {
                val oldestKey = cache.entries
                    .minByOrNull { it.value.lastAccessTime }
                    ?.key

                oldestKey?.let {
                    cache.remove(it)
                    saveableStateHolder.removeState(it)
                }
            }
        } else {
            // Update access time
            cache[entry.id] = cache[entry.id]!!.copy(
                lastAccessTime = System.currentTimeMillis()
            )
        }

        // Return composable that preserves state
        return {
            saveableStateHolder.SaveableStateProvider(entry.id) {
                content(entry)
            }
        }
    }

    /**
     * Check if an entry is cached.
     */
    fun isCached(entryId: String): Boolean = cache.containsKey(entryId)

    /**
     * Remove an entry from cache.
     */
    fun remove(entryId: String) {
        cache.remove(entryId)
    }

    /**
     * Clear all cached entries.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Get all cached entry IDs.
     */
    fun getCachedIds(): Set<String> = cache.keys.toSet()
}

/**
 * Cached composable metadata.
 */
@Stable
private data class CachedComposable(
    val entry: BackStackEntry,
    val lastAccessTime: Long
)

/**
 * Remember a composable cache across recompositions.
 */
@Composable
fun rememberComposableCache(maxCacheSize: Int = 3): ComposableCache {
    return remember { ComposableCache(maxCacheSize) }
}

/**
 * System time provider for testing purposes.
 */
internal object System {
    private var counter = 0L

    fun currentTimeMillis(): Long {
        // Simple counter-based timestamp for cache ordering
        return counter++
    }
}

