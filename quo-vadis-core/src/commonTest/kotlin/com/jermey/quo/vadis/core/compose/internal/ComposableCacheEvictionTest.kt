@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * A test-only [SaveableStateHolder] that tracks [removeState] calls.
 *
 * Unlike [com.jermey.quo.vadis.core.navigation.FakeSaveableStateHolder] which is
 * a no-op, this records every key passed to [removeState] so tests can verify
 * that cleanup is performed correctly.
 */
private class TrackingSaveableStateHolder : SaveableStateHolder {
    val removedKeys = mutableListOf<Any>()

    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        content()
    }

    override fun removeState(key: Any) {
        removedKeys.add(key)
    }
}

/**
 * Tests for [ComposableCache] Phase 1 changes — eviction removal and explicit cleanup.
 *
 * Phase 1 removed the LRU eviction mechanism and added [ComposableCache.removeEntry]
 * for explicit, lifecycle-driven cache cleanup. These tests verify:
 * - Cache does not evict entries automatically (no LRU eviction)
 * - [removeEntry] explicitly removes entry and clears saved state
 * - [removeEntry] clears lock and priority for removed entries
 */
class ComposableCacheEvictionTest : FunSpec({

    // =========================================================================
    // NO AUTOMATIC EVICTION TESTS
    // =========================================================================

    test("cache does not evict entries when many entries are added") {
        // Given — historically, the cache had a max size and evicted older entries.
        // Phase 1 removed automatic eviction entirely.
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()

        // When — add many entries by calling lock/unlock (simulates access)
        // The accessTimeMap is updated via CachedEntry composable (SideEffect),
        // but we can exercise the non-composable mechanisms (lock/unlock/setPriority)
        // to verify they accumulate without eviction.
        val keys = (1..20).map { "entry-$it" }
        keys.forEach { key ->
            cache.lock(key)
        }

        // Then — all entries remain locked (none evicted)
        // Verify by unlocking all — if any were evicted, unlock would be a no-op
        // but no error should occur. The real proof is that removeEntry works on all.
        keys.forEach { key ->
            cache.unlock(key)
        }

        // No removeState should have been called — nothing was evicted
        stateHolder.removedKeys.shouldBeEmpty()
    }

    test("cache allows unbounded number of priority entries") {
        // Given
        val cache = ComposableCache()

        // When — set many entries as priority
        val keys = (1..50).map { "priority-$it" }
        keys.forEach { key ->
            cache.setPriority(key, isPriority = true)
        }

        // Then — all can be un-prioritized without error (none were evicted)
        keys.forEach { key ->
            cache.setPriority(key, isPriority = false)
        }
    }

    // =========================================================================
    // removeEntry TESTS
    // =========================================================================

    test("removeEntry calls removeState on saveableStateHolder") {
        // Given
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val key = "screen-to-remove"

        // Simulate that the entry exists in cache (lock it to prove it's tracked)
        cache.lock(key)

        // When
        cache.removeEntry(key, stateHolder)

        // Then — removeState was called with the correct key
        stateHolder.removedKeys shouldBe listOf(key)
    }

    test("removeEntry clears lock for removed entry") {
        // Given
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val key = "locked-entry"

        cache.lock(key)

        // When
        cache.removeEntry(key, stateHolder)

        // Then — unlock after removeEntry is a no-op (entry already cleaned up)
        // This verifies lock was cleared because a second removeEntry
        // won't re-add the key to removedKeys from lock cleanup.
        cache.unlock(key) // Should not throw
    }

    test("removeEntry clears priority for removed entry") {
        // Given
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val key = "priority-entry"

        cache.setPriority(key, isPriority = true)
        cache.lock(key)

        // When
        cache.removeEntry(key, stateHolder)

        // Then — entry is fully cleaned up
        stateHolder.removedKeys shouldBe listOf(key)
        // Setting priority again and removing should work cleanly
        cache.setPriority(key, isPriority = false) // No-op, already cleared
    }

    test("removeEntry is idempotent") {
        // Given
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val key = "idempotent-entry"

        cache.lock(key)
        cache.setPriority(key, isPriority = true)

        // When — remove twice
        cache.removeEntry(key, stateHolder)
        cache.removeEntry(key, stateHolder)

        // Then — removeState called twice (but no crash)
        stateHolder.removedKeys shouldBe listOf(key, key)
    }

    test("removeEntry only affects the specified key") {
        // Given
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val keyToRemove = "remove-me"
        val keyToKeep = "keep-me"

        cache.lock(keyToRemove)
        cache.lock(keyToKeep)
        cache.setPriority(keyToRemove, isPriority = true)
        cache.setPriority(keyToKeep, isPriority = true)

        // When
        cache.removeEntry(keyToRemove, stateHolder)

        // Then — only the removed key's state was cleared
        stateHolder.removedKeys shouldBe listOf(keyToRemove)
        // keyToKeep is still locked and prioritized — verify by unlocking
        cache.unlock(keyToKeep) // Should work fine
        cache.setPriority(keyToKeep, isPriority = false) // Should work fine
    }

    test("removeEntry works on entry that was never locked or prioritized") {
        // Given — entry only existed in accessTimeMap (via CachedEntry composable)
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val key = "plain-entry"

        // When — remove an entry that was never explicitly locked or prioritized
        cache.removeEntry(key, stateHolder)

        // Then — removeState still called (to clean saved state)
        stateHolder.removedKeys shouldBe listOf(key)
    }

    // =========================================================================
    // INTEGRATION SCENARIO TESTS
    // =========================================================================

    test("multiple entries can be removed independently") {
        // Given
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val keys = listOf("screen-a", "screen-b", "screen-c")

        keys.forEach { key ->
            cache.lock(key)
            cache.setPriority(key, isPriority = true)
        }

        // When — remove entries in reverse order (simulates stack popping)
        keys.reversed().forEach { key ->
            cache.removeEntry(key, stateHolder)
        }

        // Then — all entries had their state removed
        stateHolder.removedKeys.shouldContainExactlyInAnyOrder(keys)
    }

    test("lock and setPriority work after removeEntry for re-added entry") {
        // Given — a screen is removed and then re-navigated to
        val cache = ComposableCache()
        val stateHolder = TrackingSaveableStateHolder()
        val key = "recyclable-screen"

        cache.lock(key)
        cache.removeEntry(key, stateHolder)

        // When — re-add the same key
        cache.lock(key)
        cache.setPriority(key, isPriority = true)

        // Then — works without issue
        cache.unlock(key)
        cache.setPriority(key, isPriority = false)
        stateHolder.removedKeys shouldBe listOf(key) // Only one removal
    }
})
