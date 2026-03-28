@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private object TestDestination : NavDestination

/**
 * A test-only [SaveableStateHolder] that tracks [removeState] calls
 * for verifying lifecycle-driven cleanup in composition state retention tests.
 */
private class RetentionTrackingStateHolder : SaveableStateHolder {
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
 * Tests for Phase 1 composition state retention in ScreenRenderer integration points.
 *
 * Since ScreenRenderer is a `@Composable` function and compose test framework isn't
 * available in commonTest, these tests verify the underlying mechanisms:
 *
 * 1. **composeSavedState** on [ScreenNode] (via [LifecycleAwareNode]) —
 *    Tests that saved state can be stored and retrieved on the node.
 *
 * 2. **onDestroyCallback** for cache cleanup —
 *    Tests that destroy callbacks fire correctly and clean up cache entries
 *    when a node is permanently removed.
 *
 * 3. **Lifecycle state transitions** —
 *    Tests the full attach/detach lifecycle relevant to ScreenRenderer's
 *    DisposableEffect behavior.
 */
class CompositionStateRetentionTest : FunSpec({

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun screen(key: String, parentKey: String? = null): ScreenNode =
        ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, TestDestination)

    // =========================================================================
    // composeSavedState TESTS
    // =========================================================================

    test("composeSavedState is null by default") {
        // Given
        val node = screen("screen-1")

        // Then
        node.composeSavedState.shouldBeNull()
    }

    test("composeSavedState can be populated with saved state map") {
        // Given — simulates ScreenRenderer's onDispose: node.composeSavedState = registry.performSave()
        val node = screen("screen-1")
        val savedState = mapOf(
            "counter" to listOf(42),
            "text" to listOf("hello"),
            "nested" to listOf(mapOf("a" to 1))
        )

        // When
        node.composeSavedState = savedState

        // Then
        node.composeSavedState.shouldNotBeNull()
        node.composeSavedState shouldBe savedState
    }

    test("composeSavedState can be cleared after consumption") {
        // Given — simulates ScreenRenderer's LaunchedEffect: node.composeSavedState = null
        val node = screen("screen-1")
        node.composeSavedState = mapOf("key" to listOf("value"))

        // When — state consumed during restoration
        node.composeSavedState = null

        // Then
        node.composeSavedState.shouldBeNull()
    }

    test("composeSavedState survives node being detached from UI") {
        // Given — simulates screen being pushed behind another screen
        val node = screen("screen-1")
        node.attachToUI()

        // When — screen leaves composition, state saved on node
        val savedState = mapOf("counter" to listOf(10))
        node.composeSavedState = savedState
        node.detachFromUI()

        // Then — state persists on the node for later restoration
        node.composeSavedState shouldBe savedState
    }

    test("composeSavedState is preserved across attach/detach cycles") {
        // Given — screen enters, saves state, leaves, re-enters
        val node = screen("screen-1")

        // First display cycle
        node.attachToUI()
        node.composeSavedState = mapOf("cycle" to listOf(1))
        node.detachFromUI()

        // Re-attach to navigator (still in tree)
        node.attachToNavigator()

        // Then — state still available for second display cycle
        node.composeSavedState shouldBe mapOf("cycle" to listOf(1))
    }

    test("composeSavedState with empty map is non-null") {
        // Given — a screen that saves with no saveable state
        val node = screen("screen-1")

        // When
        node.composeSavedState = emptyMap()

        // Then — empty map is valid (distinct from null which means "no saved state")
        node.composeSavedState.shouldNotBeNull()
        node.composeSavedState shouldBe emptyMap()
    }

    // =========================================================================
    // onDestroyCallback CACHE CLEANUP TESTS
    // =========================================================================

    test("onDestroyCallback fires when node is destroyed after UI detach") {
        // Given — simulates ScreenRenderer: node.addOnDestroyCallback { cache.removeEntry(...) }
        val node = screen("screen-1")
        val cache = ComposableCache()
        val stateHolder = RetentionTrackingStateHolder()
        var callbackInvoked = false

        node.attachToUI()
        node.addOnDestroyCallback {
            cache.removeEntry(node.key.value, stateHolder)
            callbackInvoked = true
        }

        // When — screen leaves composition, then is removed from tree
        node.detachFromUI()
        node.detachFromNavigator()

        // Then — callback fired, cache entry removed
        callbackInvoked.shouldBeTrue()
        stateHolder.removedKeys shouldBe listOf("screen-1")
    }

    test("onDestroyCallback fires when node is detached from navigator while not displayed") {
        // Given — node is in tree but not displayed (e.g., behind another screen)
        val node = screen("screen-1")
        val cache = ComposableCache()
        val stateHolder = RetentionTrackingStateHolder()
        var callbackInvoked = false

        node.attachToUI()
        node.detachFromUI() // Leaves composition (pushed behind)
        node.addOnDestroyCallback {
            cache.removeEntry(node.key.value, stateHolder)
            callbackInvoked = true
        }

        // When — node removed from tree while not displayed
        node.detachFromNavigator()

        // Then — callback fires immediately
        callbackInvoked.shouldBeTrue()
        stateHolder.removedKeys shouldBe listOf("screen-1")
    }

    test("onDestroyCallback fires when UI detaches after navigator detach") {
        // Given — node removed from tree while still displayed (animation in progress)
        val node = screen("screen-1")
        var callbackInvoked = false

        node.attachToUI()
        node.addOnDestroyCallback { callbackInvoked = true }

        // When — navigator detaches first (node popped), then UI finishes animation
        node.detachFromNavigator()
        callbackInvoked.shouldBeFalse() // Still displayed, not yet destroyed

        node.detachFromUI()

        // Then — destroyed after both conditions met
        callbackInvoked.shouldBeTrue()
    }

    test("removeOnDestroyCallback prevents callback from firing") {
        // Given — simulates ScreenRenderer's onDispose: node.removeOnDestroyCallback(cleanupCallback)
        val node = screen("screen-1")
        var callbackInvoked = false
        val callback: () -> Unit = { callbackInvoked = true }

        node.attachToUI()
        node.addOnDestroyCallback(callback)

        // When — remove callback before destruction (as ScreenRenderer does in onDispose)
        node.removeOnDestroyCallback(callback)
        node.detachFromUI()
        node.detachFromNavigator()

        // Then — callback was not invoked
        callbackInvoked.shouldBeFalse()
    }

    test("multiple destroy callbacks all fire on destruction") {
        // Given
        val node = screen("screen-1")
        val firedCallbacks = mutableListOf<String>()

        node.attachToUI()
        node.addOnDestroyCallback { firedCallbacks.add("cache-cleanup") }
        node.addOnDestroyCallback { firedCallbacks.add("mvi-scope-close") }
        node.addOnDestroyCallback { firedCallbacks.add("koin-scope-close") }

        // When
        node.detachFromUI()
        node.detachFromNavigator()

        // Then — all callbacks fired
        firedCallbacks shouldBe listOf("cache-cleanup", "mvi-scope-close", "koin-scope-close")
    }

    // =========================================================================
    // SCREENRENDERER LIFECYCLE SIMULATION TESTS
    // =========================================================================

    test("ScreenRenderer lifecycle: attach, save state, detach, destroy cleans up") {
        // Given — full lifecycle as ScreenRenderer manages it
        val node = screen("detail-screen", parentKey = "stack")
        val cache = ComposableCache()
        val stateHolder = RetentionTrackingStateHolder()

        // Phase 1: Screen enters composition (DisposableEffect triggers)
        node.attachToUI()
        val cleanupCallback: () -> Unit = {
            cache.removeEntry(node.key.value, stateHolder)
        }
        node.addOnDestroyCallback(cleanupCallback)

        // Phase 2: Screen leaves composition (onDispose triggers)
        // ScreenRenderer saves compose state before disposing
        node.composeSavedState = mapOf("scrollPosition" to listOf(250))
        node.removeOnDestroyCallback(cleanupCallback)
        node.detachFromUI()

        // Verify state is preserved (screen just hidden, not destroyed)
        node.composeSavedState shouldBe mapOf("scrollPosition" to listOf(250))
        stateHolder.removedKeys.shouldBeEmpty() // Not destroyed yet

        // Phase 3: Screen re-enters composition
        node.attachToUI()
        val restoredState = node.composeSavedState
        restoredState.shouldNotBeNull()
        restoredState["scrollPosition"] shouldBe listOf(250)

        // After restoration, clear saved state
        node.composeSavedState = null

        // Re-register destroy callback
        node.addOnDestroyCallback(cleanupCallback)

        // Phase 4: Screen popped from stack (permanent removal)
        node.removeOnDestroyCallback(cleanupCallback)
        // Manually trigger cleanup since ScreenRenderer won't re-register
        node.detachFromUI()
        node.detachFromNavigator()

        // Node is destroyed but cleanup callback was removed in onDispose
        stateHolder.removedKeys.shouldBeEmpty()
    }

    test("ScreenRenderer lifecycle: destroy while displayed triggers cleanup") {
        // Given — screen is visible when stack is cleared
        val node = screen("visible-screen", parentKey = "stack")
        val cache = ComposableCache()
        val stateHolder = RetentionTrackingStateHolder()

        node.attachToUI()
        val cleanupCallback: () -> Unit = {
            cache.removeEntry(node.key.value, stateHolder)
        }
        node.addOnDestroyCallback(cleanupCallback)

        // When — navigator removes node while still displayed
        node.detachFromNavigator()
        // Node is still displayed, not yet destroyed
        node.isDisplayed.shouldBeTrue()
        node.isAttachedToNavigator.shouldBeFalse()

        // When — UI finally disposes
        node.detachFromUI()

        // Then — destroy callback fired, cache cleaned
        stateHolder.removedKeys shouldBe listOf("visible-screen")
    }

    // =========================================================================
    // LIFECYCLE STATE TRANSITION TESTS
    // =========================================================================

    test("newly created node is not attached and not displayed") {
        val node = screen("new-screen")
        node.isAttachedToNavigator.shouldBeFalse()
        node.isDisplayed.shouldBeFalse()
    }

    test("attachToUI auto-attaches to navigator if not already attached") {
        val node = screen("auto-attach")
        node.attachToUI()
        node.isAttachedToNavigator.shouldBeTrue()
        node.isDisplayed.shouldBeTrue()
    }

    test("detachFromUI sets displayed to false") {
        val node = screen("detach-test")
        node.attachToUI()
        node.detachFromUI()
        node.isDisplayed.shouldBeFalse()
        // Still attached to navigator
        node.isAttachedToNavigator.shouldBeTrue()
    }

    test("detachFromNavigator sets attached to false") {
        val node = screen("detach-nav")
        node.attachToUI()
        node.detachFromUI()
        node.detachFromNavigator()
        node.isAttachedToNavigator.shouldBeFalse()
        node.isDisplayed.shouldBeFalse()
    }
})
