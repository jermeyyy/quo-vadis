package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.registry.BackHandlerRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse

/**
 * Unit tests for [BackHandlerRegistry].
 *
 * Tests cover:
 * - Scope-aware handler registration and unregistration
 * - LIFO (Last-In-First-Out) invocation order within a scope
 * - Leaf-to-root scope traversal during handleBack
 * - hasHandlers state management
 * - unregisterAll bulk cleanup
 */
class BackHandlerRegistryTest : FunSpec({

    val screenKey = NodeKey("screen")
    val stackKey = NodeKey("stack")
    val tabKey = NodeKey("tab")
    val defaultPath = listOf(screenKey, stackKey, tabKey)

    // =========================================================================
    // BASIC REGISTRATION AND HANDLING TESTS
    // =========================================================================

    test("handleBack returns false when no handlers registered") {
        val registry = BackHandlerRegistry()
        registry.handleBack(defaultPath).shouldBeFalse()
    }

    test("handleBack returns true when handler consumes event") {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { true }
        registry.handleBack(defaultPath).shouldBeTrue()
    }

    test("handleBack returns false when handler does not consume event") {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { false }
        registry.handleBack(defaultPath).shouldBeFalse()
    }

    test("handleBack ignores handlers not on active path") {
        val registry = BackHandlerRegistry()
        val otherKey = NodeKey("other")
        registry.register(otherKey) { true }
        registry.handleBack(defaultPath).shouldBeFalse()
    }

    // =========================================================================
    // LIFO ORDER TESTS (within a single scope)
    // =========================================================================

    test("handlers are invoked in LIFO order - last registered first") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register(screenKey) {
            callOrder.add(1)
            false
        }
        registry.register(screenKey) {
            callOrder.add(2)
            false
        }
        registry.register(screenKey) {
            callOrder.add(3)
            true // This one consumes
        }

        registry.handleBack(defaultPath)

        // Only handler 3 should be called (LIFO, first one to consume)
        callOrder shouldBe listOf(3)
    }

    test("all handlers called when none consume - in LIFO order") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register(screenKey) { callOrder.add(1); false }
        registry.register(screenKey) { callOrder.add(2); false }
        registry.register(screenKey) { callOrder.add(3); false }

        registry.handleBack(defaultPath)

        // All called in reverse order (LIFO)
        callOrder shouldBe listOf(3, 2, 1)
    }

    test("first handler to consume stops propagation") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register(screenKey) {
            callOrder.add(1)
            true // Consumes - should not be reached
        }
        registry.register(screenKey) {
            callOrder.add(2)
            true // Consumes - stops here
        }
        registry.register(screenKey) {
            callOrder.add(3)
            false // Doesn't consume
        }

        registry.handleBack(defaultPath)

        // Handler 3 called first (doesn't consume), then 2 (consumes, stops)
        // Handler 1 should not be called
        callOrder shouldBe listOf(3, 2)
    }

    // =========================================================================
    // SCOPE TRAVERSAL TESTS (leaf-to-root)
    // =========================================================================

    test("handleBack checks leaf scope before parent scope") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<String>()

        registry.register(stackKey) { callOrder.add("stack"); true }
        registry.register(screenKey) { callOrder.add("screen"); true }

        registry.handleBack(defaultPath)

        // Screen (leaf) should be checked first
        callOrder shouldBe listOf("screen")
    }

    test("handleBack falls through to parent scope when leaf does not consume") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<String>()

        registry.register(stackKey) { callOrder.add("stack"); true }
        registry.register(screenKey) { callOrder.add("screen"); false }

        registry.handleBack(defaultPath)

        callOrder shouldBe listOf("screen", "stack")
    }

    test("handleBack cascades through all scopes leaf to root") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<String>()

        registry.register(tabKey) { callOrder.add("tab"); true }
        registry.register(stackKey) { callOrder.add("stack"); false }
        registry.register(screenKey) { callOrder.add("screen"); false }

        registry.handleBack(defaultPath)

        callOrder shouldBe listOf("screen", "stack", "tab")
    }

    // =========================================================================
    // UNREGISTER TESTS
    // =========================================================================

    test("unregister removes handler") {
        val registry = BackHandlerRegistry()
        var called = false

        val unregister = registry.register(screenKey) { called = true; true }
        unregister()

        registry.handleBack(defaultPath)
        called.shouldBeFalse()
    }

    test("unregister is idempotent - safe to call multiple times") {
        val registry = BackHandlerRegistry()
        val unregister = registry.register(screenKey) { true }

        unregister()
        unregister() // Should not throw
        unregister() // Should not throw

        registry.hasHandlers(defaultPath).shouldBeFalse()
    }

    test("unregistered handler does not affect other handlers") {
        val registry = BackHandlerRegistry()
        var handler1Called = false
        var handler2Called = false

        val unregister1 = registry.register(screenKey) { handler1Called = true; false }
        registry.register(screenKey) { handler2Called = true; true }

        unregister1()

        registry.handleBack(defaultPath)

        handler1Called.shouldBeFalse()
        handler2Called.shouldBeTrue()
    }

    test("unregister middle handler preserves order of remaining handlers") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register(screenKey) { callOrder.add(1); false }
        val unregister2 = registry.register(screenKey) { callOrder.add(2); false }
        registry.register(screenKey) { callOrder.add(3); false }

        unregister2()

        registry.handleBack(defaultPath)

        // Handler 2 was removed, remaining should still be LIFO
        callOrder shouldBe listOf(3, 1)
    }

    // =========================================================================
    // UNREGISTER ALL TESTS
    // =========================================================================

    test("unregisterAll removes all handlers for a key") {
        val registry = BackHandlerRegistry()

        registry.register(screenKey) { true }
        registry.register(screenKey) { true }

        registry.unregisterAll(screenKey)

        registry.hasHandlers(listOf(screenKey)).shouldBeFalse()
    }

    test("unregisterAll does not affect other keys") {
        val registry = BackHandlerRegistry()

        registry.register(screenKey) { true }
        registry.register(stackKey) { true }

        registry.unregisterAll(screenKey)

        registry.hasHandlers(listOf(screenKey)).shouldBeFalse()
        registry.hasHandlers(listOf(stackKey)).shouldBeTrue()
    }

    test("unregisterAll on empty key does not throw") {
        val registry = BackHandlerRegistry()
        registry.unregisterAll(screenKey) // Should not throw
    }

    // =========================================================================
    // HAS HANDLERS TESTS
    // =========================================================================

    test("hasHandlers returns false when empty") {
        val registry = BackHandlerRegistry()
        registry.hasHandlers(defaultPath).shouldBeFalse()
    }

    test("hasHandlers returns true after registration") {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { true }
        registry.hasHandlers(defaultPath).shouldBeTrue()
    }

    test("hasHandlers returns true when handler is on ancestor in path") {
        val registry = BackHandlerRegistry()
        registry.register(tabKey) { true }
        registry.hasHandlers(defaultPath).shouldBeTrue()
    }

    test("hasHandlers returns false when handler is not on path") {
        val registry = BackHandlerRegistry()
        val otherKey = NodeKey("other")
        registry.register(otherKey) { true }
        registry.hasHandlers(defaultPath).shouldBeFalse()
    }

    test("hasHandlers returns false after all handlers unregistered") {
        val registry = BackHandlerRegistry()

        val unregister1 = registry.register(screenKey) { true }
        val unregister2 = registry.register(screenKey) { false }

        registry.hasHandlers(defaultPath).shouldBeTrue()

        unregister1()
        registry.hasHandlers(defaultPath).shouldBeTrue() // Still has handler2

        unregister2()
        registry.hasHandlers(defaultPath).shouldBeFalse() // Now empty
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    test("handleBack can be called multiple times") {
        val registry = BackHandlerRegistry()
        var callCount = 0

        registry.register(screenKey) { callCount++; true }

        registry.handleBack(defaultPath).shouldBeTrue()
        registry.handleBack(defaultPath).shouldBeTrue()
        registry.handleBack(defaultPath).shouldBeTrue()

        callCount shouldBe 3
    }

    test("handleBack with empty path returns false") {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { true }
        registry.handleBack(emptyList()).shouldBeFalse()
    }

    test("registry handles many handlers correctly") {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        // Register 100 handlers
        repeat(100) { index ->
            registry.register(screenKey) {
                callOrder.add(index)
                index == 50 // Handler 50 consumes
            }
        }

        registry.handleBack(defaultPath)

        // Handlers 99 down to 50 should be called (LIFO)
        val expectedOrder = (99 downTo 50).toList()
        callOrder shouldBe expectedOrder
    }

    test("handler registration during handleBack is safe") {
        // This tests that we don't get ConcurrentModificationException
        val registry = BackHandlerRegistry()
        var secondHandlerCalled = false

        registry.register(screenKey) {
            // Register a new handler during iteration
            registry.register(screenKey) {
                secondHandlerCalled = true
                true
            }
            false
        }

        registry.handleBack(defaultPath)

        // First call won't invoke newly registered handler (already iterating)
        // But the handler should be registered for future calls
        secondHandlerCalled.shouldBeFalse()

        // Second call should invoke the new handler
        registry.handleBack(defaultPath)
        secondHandlerCalled.shouldBeTrue()
    }
})
