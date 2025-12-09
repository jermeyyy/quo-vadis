# HIER-007: ComposableCache Enhancement

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-007 |
| **Task Name** | Enhance ComposableCache for NavNode Caching |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-017, HIER-020, HIER-021, HIER-022 |

---

## Overview

The `ComposableCache` manages state preservation for composables across navigation transitions. This enhancement extends the existing cache to support NavNode-keyed caching, entry locking during animations, and LRU eviction that respects locked entries.

### Purpose

- Preserve composable state across recompositions
- Enable smooth animations by keeping both old and new content
- Prevent cache eviction during active transitions
- Support priority entries for expensive-to-recreate screens

### Design Decisions

1. **LRU eviction**: Most recently used entries kept, oldest evicted
2. **Entry locking**: Temporarily protect entries during animations
3. **Priority entries**: Permanently protect important entries
4. **SaveableStateHolder integration**: State survives process death

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/cache/ComposableCache.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.cache

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue

/**
 * Cache for preserving composable state across navigation transitions.
 *
 * The cache uses an LRU (Least Recently Used) eviction strategy with support
 * for entry locking during animations. This ensures smooth transitions by
 * keeping both the current and previous content in memory.
 *
 * ## Key Features
 *
 * - **LRU Eviction**: Oldest unused entries are evicted when cache is full
 * - **Entry Locking**: Temporarily protect entries during animations
 * - **Priority Entries**: Permanently protect important screens
 * - **State Preservation**: Integration with [SaveableStateHolder] for process death
 *
 * ## Usage
 *
 * ```kotlin
 * val cache = remember { ComposableCache(maxSize = 10) }
 *
 * // In renderer
 * cache.CachedEntry(key = screenNode.key) {
 *     ScreenContent(screenNode.destination)
 * }
 *
 * // During animation
 * cache.lock(previousKey)
 * cache.lock(currentKey)
 * // ... perform animation ...
 * cache.unlock(previousKey)
 * cache.unlock(currentKey)
 * ```
 *
 * ## Cache Key Strategy
 *
 * | NavNode Type | Key Pattern | Example |
 * |--------------|-------------|---------|
 * | ScreenNode | `{parentKey}/screen/{route}` | `stack/screen/home` |
 * | TabNode | `{parentKey}/tabs/{type}` | `root/tabs/main` |
 * | PaneNode | `{parentKey}/panes/{type}` | `root/panes/master-detail` |
 *
 * @property maxSize Maximum number of entries to keep in cache
 *
 * @see CachedEntry
 * @see lock
 * @see unlock
 */
@Stable
class ComposableCache(
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {
    
    // Access time tracking for LRU
    private val accessTimes = mutableStateMapOf<String, Long>()
    private var accessCounter by mutableLongStateOf(0L)
    
    // Entry protection
    private val lockedEntries = mutableStateMapOf<String, Boolean>()
    private val priorityEntries = mutableStateMapOf<String, Boolean>()
    
    // State holder for composable state preservation
    private var stateHolder: SaveableStateHolder? = null
    
    /**
     * Cached composable entry that preserves state across recompositions.
     *
     * Wraps content in a [SaveableStateHolder] keyed by [key], ensuring
     * state like `rememberSaveable`, scroll positions, and text field
     * values survive navigation transitions.
     *
     * @param key Unique identifier for this entry (usually NavNode.key)
     * @param content The composable content to cache
     */
    @Composable
    fun CachedEntry(
        key: String,
        content: @Composable () -> Unit
    ) {
        // Initialize state holder on first use
        val holder = stateHolder ?: rememberSaveableStateHolder().also {
            stateHolder = it
        }
        
        // Track access time
        DisposableEffect(key) {
            recordAccess(key)
            onDispose { }
        }
        
        // Provide saveable state for this entry
        holder.SaveableStateProvider(key = key) {
            content()
        }
    }
    
    /**
     * Locks an entry to prevent eviction.
     *
     * Call this before starting an animation to ensure both the
     * current and previous entries remain in cache throughout
     * the transition.
     *
     * @param key The entry key to lock
     */
    fun lock(key: String) {
        lockedEntries[key] = true
    }
    
    /**
     * Unlocks a previously locked entry.
     *
     * Call this after animation completes to allow normal eviction.
     * Triggers eviction check in case cache is over capacity.
     *
     * @param key The entry key to unlock
     */
    fun unlock(key: String) {
        lockedEntries.remove(key)
        evictIfNeeded()
    }
    
    /**
     * Marks an entry as priority (permanently protected).
     *
     * Priority entries are never evicted, even when cache is full.
     * Use this for screens that are expensive to recreate (e.g.,
     * screens containing nested navigators).
     *
     * @param key The entry key to prioritize
     * @param isPriority Whether to set or clear priority status
     */
    fun setPriority(key: String, isPriority: Boolean) {
        if (isPriority) {
            priorityEntries[key] = true
        } else {
            priorityEntries.remove(key)
        }
    }
    
    /**
     * Checks if an entry is currently protected from eviction.
     *
     * @param key The entry key to check
     * @return true if entry is locked or prioritized
     */
    fun isProtected(key: String): Boolean {
        return lockedEntries.containsKey(key) || priorityEntries.containsKey(key)
    }
    
    /**
     * Checks if an entry exists in the cache.
     *
     * @param key The entry key to check
     * @return true if entry has been accessed
     */
    fun contains(key: String): Boolean {
        return accessTimes.containsKey(key)
    }
    
    /**
     * Removes a specific entry from the cache.
     *
     * Use this when a screen is explicitly removed from the navigation
     * stack and its state should be discarded.
     *
     * @param key The entry key to remove
     * @param force If true, removes even if locked/prioritized
     */
    fun remove(key: String, force: Boolean = false) {
        if (force || !isProtected(key)) {
            accessTimes.remove(key)
            lockedEntries.remove(key)
            priorityEntries.remove(key)
            stateHolder?.removeState(key)
        }
    }
    
    /**
     * Clears all non-protected entries from the cache.
     */
    fun clearUnprotected() {
        val keysToRemove = accessTimes.keys.filter { !isProtected(it) }
        keysToRemove.forEach { key ->
            accessTimes.remove(key)
            stateHolder?.removeState(key)
        }
    }
    
    /**
     * Clears all entries including protected ones.
     *
     * Use with caution - this discards all cached state.
     */
    fun clearAll() {
        accessTimes.clear()
        lockedEntries.clear()
        priorityEntries.clear()
        // Note: Cannot clear SaveableStateHolder completely
    }
    
    /**
     * Returns current cache statistics.
     */
    fun getStats(): CacheStats {
        return CacheStats(
            size = accessTimes.size,
            maxSize = maxSize,
            lockedCount = lockedEntries.size,
            priorityCount = priorityEntries.size
        )
    }
    
    /**
     * Records access to an entry, updating LRU tracking.
     */
    private fun recordAccess(key: String) {
        accessTimes[key] = ++accessCounter
        evictIfNeeded()
    }
    
    /**
     * Evicts oldest unprotected entries if cache is over capacity.
     */
    private fun evictIfNeeded() {
        while (accessTimes.size > maxSize) {
            // Find oldest unprotected entry
            val oldestEntry = accessTimes.entries
                .filter { !isProtected(it.key) }
                .minByOrNull { it.value }
            
            if (oldestEntry != null) {
                remove(oldestEntry.key)
            } else {
                // All entries are protected - can't evict
                break
            }
        }
    }
    
    companion object {
        /**
         * Default maximum cache size.
         *
         * Sized to hold a typical navigation depth plus animation buffer.
         */
        const val DEFAULT_MAX_SIZE = 10
        
        /**
         * Recommended cache size for complex navigation.
         *
         * Use for apps with nested navigators or many parallel tabs.
         */
        const val RECOMMENDED_SIZE_COMPLEX = 20
    }
}

/**
 * Cache statistics for monitoring and debugging.
 *
 * @property size Current number of entries
 * @property maxSize Maximum allowed entries
 * @property lockedCount Temporarily locked entries
 * @property priorityCount Permanently protected entries
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val lockedCount: Int,
    val priorityCount: Int
) {
    /**
     * Percentage of cache capacity used.
     */
    val fillPercentage: Float
        get() = if (maxSize > 0) size.toFloat() / maxSize else 0f
    
    /**
     * Number of entries that can be evicted.
     */
    val evictableCount: Int
        get() = size - lockedCount - priorityCount
}

/**
 * Remembers a [ComposableCache] with the given configuration.
 *
 * The cache is preserved across recompositions but not across
 * configuration changes or process death.
 *
 * @param maxSize Maximum number of entries
 * @return Remembered cache instance
 */
@Composable
fun rememberComposableCache(
    maxSize: Int = ComposableCache.DEFAULT_MAX_SIZE
): ComposableCache {
    return androidx.compose.runtime.remember {
        ComposableCache(maxSize)
    }
}
```

---

## Integration Points

### Providers

- **HierarchicalQuoVadisHost** (HIER-024): Creates and provides cache

### Consumers

- **ScreenRenderer** (HIER-017): Caches individual screens
- **TabRenderer** (HIER-021): Caches entire tab structure
- **PaneRenderer** (HIER-022): Caches entire pane structure
- **PredictiveBackContent** (HIER-020): Locks entries during gesture
- **AnimatedNavContent** (HIER-019): Locks entries during animation

### Related Components

| Component | Relationship |
|-----------|--------------|
| `SaveableStateHolder` | State preservation mechanism |
| `NavRenderScope` | Provides cache to renderers (HIER-001) |

---

## Testing Requirements

### Unit Tests

```kotlin
class ComposableCacheTest {
    
    @Test
    fun `default max size is 10`() {
        val cache = ComposableCache()
        assertEquals(10, cache.getStats().maxSize)
    }
    
    @Test
    fun `custom max size is respected`() {
        val cache = ComposableCache(maxSize = 5)
        assertEquals(5, cache.getStats().maxSize)
    }
    
    @Test
    fun `lock prevents eviction`() {
        val cache = ComposableCache(maxSize = 2)
        
        // Simulate accessing entries
        simulateAccess(cache, "key1")
        cache.lock("key1")
        simulateAccess(cache, "key2")
        simulateAccess(cache, "key3")
        
        // key1 should still exist (locked)
        assertTrue(cache.contains("key1"))
    }
    
    @Test
    fun `unlock allows eviction`() {
        val cache = ComposableCache(maxSize = 2)
        
        simulateAccess(cache, "key1")
        cache.lock("key1")
        simulateAccess(cache, "key2")
        cache.unlock("key1")
        simulateAccess(cache, "key3")
        
        // key1 might be evicted now (oldest)
        // This depends on implementation timing
    }
    
    @Test
    fun `priority entries are never evicted`() {
        val cache = ComposableCache(maxSize = 2)
        
        simulateAccess(cache, "priority")
        cache.setPriority("priority", true)
        simulateAccess(cache, "key1")
        simulateAccess(cache, "key2")
        simulateAccess(cache, "key3")
        
        assertTrue(cache.contains("priority"))
    }
    
    @Test
    fun `isProtected returns true for locked entries`() {
        val cache = ComposableCache()
        cache.lock("key")
        assertTrue(cache.isProtected("key"))
    }
    
    @Test
    fun `isProtected returns true for priority entries`() {
        val cache = ComposableCache()
        cache.setPriority("key", true)
        assertTrue(cache.isProtected("key"))
    }
    
    @Test
    fun `isProtected returns false for unprotected entries`() {
        val cache = ComposableCache()
        assertFalse(cache.isProtected("key"))
    }
    
    @Test
    fun `remove with force removes protected entries`() {
        val cache = ComposableCache()
        simulateAccess(cache, "key")
        cache.lock("key")
        
        cache.remove("key", force = true)
        
        assertFalse(cache.contains("key"))
    }
    
    @Test
    fun `remove without force respects protection`() {
        val cache = ComposableCache()
        simulateAccess(cache, "key")
        cache.lock("key")
        
        cache.remove("key", force = false)
        
        assertTrue(cache.contains("key"))
    }
    
    @Test
    fun `clearUnprotected keeps protected entries`() {
        val cache = ComposableCache()
        simulateAccess(cache, "protected")
        cache.lock("protected")
        simulateAccess(cache, "unprotected")
        
        cache.clearUnprotected()
        
        assertTrue(cache.contains("protected"))
        assertFalse(cache.contains("unprotected"))
    }
    
    @Test
    fun `getStats returns correct values`() {
        val cache = ComposableCache(maxSize = 10)
        simulateAccess(cache, "key1")
        simulateAccess(cache, "key2")
        cache.lock("key1")
        cache.setPriority("key2", true)
        
        val stats = cache.getStats()
        
        assertEquals(2, stats.size)
        assertEquals(10, stats.maxSize)
        assertEquals(1, stats.lockedCount)
        assertEquals(1, stats.priorityCount)
    }
    
    @Test
    fun `CacheStats fillPercentage is correct`() {
        val stats = CacheStats(size = 5, maxSize = 10, lockedCount = 0, priorityCount = 0)
        assertEquals(0.5f, stats.fillPercentage)
    }
    
    @Test
    fun `CacheStats evictableCount is correct`() {
        val stats = CacheStats(size = 10, maxSize = 10, lockedCount = 2, priorityCount = 1)
        assertEquals(7, stats.evictableCount)
    }
    
    // Helper to simulate accessing cache entries
    private fun simulateAccess(cache: ComposableCache, key: String) {
        // This would need to be done through compose testing
        // For unit tests, we'd need to expose internal methods or use reflection
    }
}
```

### Compose Tests

```kotlin
class ComposableCacheComposeTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `CachedEntry preserves state`() {
        val cache = ComposableCache()
        var counter by mutableStateOf(0)
        
        composeTestRule.setContent {
            cache.CachedEntry("test") {
                var localState by rememberSaveable { mutableStateOf(0) }
                Button(onClick = { localState++ }) {
                    Text("Count: $localState")
                }
            }
        }
        
        // Click button
        composeTestRule.onNodeWithText("Count: 0").performClick()
        composeTestRule.onNodeWithText("Count: 1").assertExists()
        
        // Trigger recomposition
        counter++
        
        // State should be preserved
        composeTestRule.onNodeWithText("Count: 1").assertExists()
    }
}
```

---

## Acceptance Criteria

- [ ] `ComposableCache` class with `@Stable` annotation
- [ ] `CachedEntry(key, content)` composable with SaveableStateProvider
- [ ] `lock(key)` and `unlock(key)` for animation protection
- [ ] `setPriority(key, isPriority)` for permanent protection
- [ ] `isProtected(key)`, `contains(key)` query methods
- [ ] `remove(key, force)` for explicit removal
- [ ] `clearUnprotected()` and `clearAll()` bulk operations
- [ ] `getStats()` for monitoring
- [ ] LRU eviction respecting locked/priority entries
- [ ] `CacheStats` data class with computed properties
- [ ] `rememberComposableCache()` composable factory
- [ ] Full KDoc documentation
- [ ] Unit and compose tests pass

---

## Notes

### Open Questions

1. Should we add cache preloading for predictive prefetch?
2. Should we expose cache events for debugging/analytics?

### Design Rationale

- **LRU**: Simple, predictable eviction strategy that works well for navigation
- **Locking**: Prevents mid-animation eviction without permanent memory pressure
- **Priority**: Screens with nested navigators are expensive to recreate
- **SaveableStateHolder**: Standard Compose mechanism for state preservation
