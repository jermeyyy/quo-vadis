package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.registry.BackHandlerRegistry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
class BackHandlerRegistryTest {

    private val screenKey = NodeKey("screen")
    private val stackKey = NodeKey("stack")
    private val tabKey = NodeKey("tab")
    private val defaultPath = listOf(screenKey, stackKey, tabKey)

    // =========================================================================
    // BASIC REGISTRATION AND HANDLING TESTS
    // =========================================================================

    @Test
    fun `handleBack returns false when no handlers registered`() {
        val registry = BackHandlerRegistry()
        assertFalse(registry.handleBack(defaultPath))
    }

    @Test
    fun `handleBack returns true when handler consumes event`() {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { true }
        assertTrue(registry.handleBack(defaultPath))
    }

    @Test
    fun `handleBack returns false when handler does not consume event`() {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { false }
        assertFalse(registry.handleBack(defaultPath))
    }

    @Test
    fun `handleBack ignores handlers not on active path`() {
        val registry = BackHandlerRegistry()
        val otherKey = NodeKey("other")
        registry.register(otherKey) { true }
        assertFalse(registry.handleBack(defaultPath))
    }

    // =========================================================================
    // LIFO ORDER TESTS (within a single scope)
    // =========================================================================

    @Test
    fun `handlers are invoked in LIFO order - last registered first`() {
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
        assertTrue(callOrder == listOf(3))
    }

    @Test
    fun `all handlers called when none consume - in LIFO order`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register(screenKey) { callOrder.add(1); false }
        registry.register(screenKey) { callOrder.add(2); false }
        registry.register(screenKey) { callOrder.add(3); false }

        registry.handleBack(defaultPath)

        // All called in reverse order (LIFO)
        assertTrue(callOrder == listOf(3, 2, 1))
    }

    @Test
    fun `first handler to consume stops propagation`() {
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
        assertTrue(callOrder == listOf(3, 2))
    }

    // =========================================================================
    // SCOPE TRAVERSAL TESTS (leaf-to-root)
    // =========================================================================

    @Test
    fun `handleBack checks leaf scope before parent scope`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<String>()

        registry.register(stackKey) { callOrder.add("stack"); true }
        registry.register(screenKey) { callOrder.add("screen"); true }

        registry.handleBack(defaultPath)

        // Screen (leaf) should be checked first
        assertTrue(callOrder == listOf("screen"))
    }

    @Test
    fun `handleBack falls through to parent scope when leaf does not consume`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<String>()

        registry.register(stackKey) { callOrder.add("stack"); true }
        registry.register(screenKey) { callOrder.add("screen"); false }

        registry.handleBack(defaultPath)

        assertTrue(callOrder == listOf("screen", "stack"))
    }

    @Test
    fun `handleBack cascades through all scopes leaf to root`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<String>()

        registry.register(tabKey) { callOrder.add("tab"); true }
        registry.register(stackKey) { callOrder.add("stack"); false }
        registry.register(screenKey) { callOrder.add("screen"); false }

        registry.handleBack(defaultPath)

        assertTrue(callOrder == listOf("screen", "stack", "tab"))
    }

    // =========================================================================
    // UNREGISTER TESTS
    // =========================================================================

    @Test
    fun `unregister removes handler`() {
        val registry = BackHandlerRegistry()
        var called = false

        val unregister = registry.register(screenKey) { called = true; true }
        unregister()

        registry.handleBack(defaultPath)
        assertFalse(called)
    }

    @Test
    fun `unregister is idempotent - safe to call multiple times`() {
        val registry = BackHandlerRegistry()
        val unregister = registry.register(screenKey) { true }

        unregister()
        unregister() // Should not throw
        unregister() // Should not throw

        assertFalse(registry.hasHandlers(defaultPath))
    }

    @Test
    fun `unregistered handler does not affect other handlers`() {
        val registry = BackHandlerRegistry()
        var handler1Called = false
        var handler2Called = false

        val unregister1 = registry.register(screenKey) { handler1Called = true; false }
        registry.register(screenKey) { handler2Called = true; true }

        unregister1()

        registry.handleBack(defaultPath)

        assertFalse(handler1Called)
        assertTrue(handler2Called)
    }

    @Test
    fun `unregister middle handler preserves order of remaining handlers`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register(screenKey) { callOrder.add(1); false }
        val unregister2 = registry.register(screenKey) { callOrder.add(2); false }
        registry.register(screenKey) { callOrder.add(3); false }

        unregister2()

        registry.handleBack(defaultPath)

        // Handler 2 was removed, remaining should still be LIFO
        assertTrue(callOrder == listOf(3, 1))
    }

    // =========================================================================
    // UNREGISTER ALL TESTS
    // =========================================================================

    @Test
    fun `unregisterAll removes all handlers for a key`() {
        val registry = BackHandlerRegistry()

        registry.register(screenKey) { true }
        registry.register(screenKey) { true }

        registry.unregisterAll(screenKey)

        assertFalse(registry.hasHandlers(listOf(screenKey)))
    }

    @Test
    fun `unregisterAll does not affect other keys`() {
        val registry = BackHandlerRegistry()

        registry.register(screenKey) { true }
        registry.register(stackKey) { true }

        registry.unregisterAll(screenKey)

        assertFalse(registry.hasHandlers(listOf(screenKey)))
        assertTrue(registry.hasHandlers(listOf(stackKey)))
    }

    @Test
    fun `unregisterAll on empty key does not throw`() {
        val registry = BackHandlerRegistry()
        registry.unregisterAll(screenKey) // Should not throw
    }

    // =========================================================================
    // HAS HANDLERS TESTS
    // =========================================================================

    @Test
    fun `hasHandlers returns false when empty`() {
        val registry = BackHandlerRegistry()
        assertFalse(registry.hasHandlers(defaultPath))
    }

    @Test
    fun `hasHandlers returns true after registration`() {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { true }
        assertTrue(registry.hasHandlers(defaultPath))
    }

    @Test
    fun `hasHandlers returns true when handler is on ancestor in path`() {
        val registry = BackHandlerRegistry()
        registry.register(tabKey) { true }
        assertTrue(registry.hasHandlers(defaultPath))
    }

    @Test
    fun `hasHandlers returns false when handler is not on path`() {
        val registry = BackHandlerRegistry()
        val otherKey = NodeKey("other")
        registry.register(otherKey) { true }
        assertFalse(registry.hasHandlers(defaultPath))
    }

    @Test
    fun `hasHandlers returns false after all handlers unregistered`() {
        val registry = BackHandlerRegistry()

        val unregister1 = registry.register(screenKey) { true }
        val unregister2 = registry.register(screenKey) { false }

        assertTrue(registry.hasHandlers(defaultPath))

        unregister1()
        assertTrue(registry.hasHandlers(defaultPath)) // Still has handler2

        unregister2()
        assertFalse(registry.hasHandlers(defaultPath)) // Now empty
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `handleBack can be called multiple times`() {
        val registry = BackHandlerRegistry()
        var callCount = 0

        registry.register(screenKey) { callCount++; true }

        assertTrue(registry.handleBack(defaultPath))
        assertTrue(registry.handleBack(defaultPath))
        assertTrue(registry.handleBack(defaultPath))

        assertTrue(callCount == 3)
    }

    @Test
    fun `handleBack with empty path returns false`() {
        val registry = BackHandlerRegistry()
        registry.register(screenKey) { true }
        assertFalse(registry.handleBack(emptyList()))
    }

    @Test
    fun `registry handles many handlers correctly`() {
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
        assertTrue(callOrder == expectedOrder)
    }

    @Test
    fun `handler registration during handleBack is safe`() {
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
        assertFalse(secondHandlerCalled)

        // Second call should invoke the new handler
        registry.handleBack(defaultPath)
        assertTrue(secondHandlerCalled)
    }
}
