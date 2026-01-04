@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.registry.BackHandlerRegistry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [com.jermey.quo.vadis.core.navigation.compose.registry.BackHandlerRegistry].
 *
 * Tests cover:
 * - Handler registration and unregistration
 * - LIFO (Last-In-First-Out) invocation order
 * - Handler consumption behavior
 * - hasHandlers state management
 */
class BackHandlerRegistryTest {

    // =========================================================================
    // BASIC REGISTRATION AND HANDLING TESTS
    // =========================================================================

    @Test
    fun `handleBack returns false when no handlers registered`() {
        val registry = BackHandlerRegistry()
        assertFalse(registry.handleBack())
    }

    @Test
    fun `handleBack returns true when handler consumes event`() {
        val registry = BackHandlerRegistry()
        registry.register { true }
        assertTrue(registry.handleBack())
    }

    @Test
    fun `handleBack returns false when handler does not consume event`() {
        val registry = BackHandlerRegistry()
        registry.register { false }
        assertFalse(registry.handleBack())
    }

    // =========================================================================
    // LIFO ORDER TESTS
    // =========================================================================

    @Test
    fun `handlers are invoked in LIFO order - last registered first`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register {
            callOrder.add(1)
            false
        }
        registry.register {
            callOrder.add(2)
            false
        }
        registry.register {
            callOrder.add(3)
            true // This one consumes
        }

        registry.handleBack()

        // Only handler 3 should be called (LIFO, first one to consume)
        assertTrue(callOrder == listOf(3))
    }

    @Test
    fun `all handlers called when none consume - in LIFO order`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register { callOrder.add(1); false }
        registry.register { callOrder.add(2); false }
        registry.register { callOrder.add(3); false }

        registry.handleBack()

        // All called in reverse order (LIFO)
        assertTrue(callOrder == listOf(3, 2, 1))
    }

    @Test
    fun `first handler to consume stops propagation`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register {
            callOrder.add(1)
            true // Consumes - should not be reached
        }
        registry.register {
            callOrder.add(2)
            true // Consumes - stops here
        }
        registry.register {
            callOrder.add(3)
            false // Doesn't consume
        }

        registry.handleBack()

        // Handler 3 called first (doesn't consume), then 2 (consumes, stops)
        // Handler 1 should not be called
        assertTrue(callOrder == listOf(3, 2))
    }

    // =========================================================================
    // UNREGISTER TESTS
    // =========================================================================

    @Test
    fun `unregister removes handler`() {
        val registry = BackHandlerRegistry()
        var called = false

        val unregister = registry.register { called = true; true }
        unregister()

        registry.handleBack()
        assertFalse(called)
    }

    @Test
    fun `unregister is idempotent - safe to call multiple times`() {
        val registry = BackHandlerRegistry()
        val unregister = registry.register { true }

        unregister()
        unregister() // Should not throw
        unregister() // Should not throw

        assertFalse(registry.hasHandlers())
    }

    @Test
    fun `unregistered handler does not affect other handlers`() {
        val registry = BackHandlerRegistry()
        var handler1Called = false
        var handler2Called = false

        val unregister1 = registry.register { handler1Called = true; false }
        registry.register { handler2Called = true; true }

        unregister1()

        registry.handleBack()

        assertFalse(handler1Called)
        assertTrue(handler2Called)
    }

    @Test
    fun `unregister middle handler preserves order of remaining handlers`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        registry.register { callOrder.add(1); false }
        val unregister2 = registry.register { callOrder.add(2); false }
        registry.register { callOrder.add(3); false }

        unregister2()

        registry.handleBack()

        // Handler 2 was removed, remaining should still be LIFO
        assertTrue(callOrder == listOf(3, 1))
    }

    // =========================================================================
    // HAS HANDLERS TESTS
    // =========================================================================

    @Test
    fun `hasHandlers returns false when empty`() {
        val registry = BackHandlerRegistry()
        assertFalse(registry.hasHandlers())
    }

    @Test
    fun `hasHandlers returns true after registration`() {
        val registry = BackHandlerRegistry()
        registry.register { true }
        assertTrue(registry.hasHandlers())
    }

    @Test
    fun `hasHandlers returns false after all handlers unregistered`() {
        val registry = BackHandlerRegistry()

        val unregister1 = registry.register { true }
        val unregister2 = registry.register { false }

        assertTrue(registry.hasHandlers())

        unregister1()
        assertTrue(registry.hasHandlers()) // Still has handler2

        unregister2()
        assertFalse(registry.hasHandlers()) // Now empty
    }

    // =========================================================================
    // EDGE CASES
    // =========================================================================

    @Test
    fun `handleBack can be called multiple times`() {
        val registry = BackHandlerRegistry()
        var callCount = 0

        registry.register { callCount++; true }

        assertTrue(registry.handleBack())
        assertTrue(registry.handleBack())
        assertTrue(registry.handleBack())

        assertTrue(callCount == 3)
    }

    @Test
    fun `registry handles many handlers correctly`() {
        val registry = BackHandlerRegistry()
        val callOrder = mutableListOf<Int>()

        // Register 100 handlers
        repeat(100) { index ->
            registry.register {
                callOrder.add(index)
                index == 50 // Handler 50 consumes
            }
        }

        registry.handleBack()

        // Handlers 99 down to 50 should be called (LIFO)
        val expectedOrder = (99 downTo 50).toList()
        assertTrue(callOrder == expectedOrder)
    }

    @Test
    fun `handler registration during handleBack is safe`() {
        // This tests that we don't get ConcurrentModificationException
        val registry = BackHandlerRegistry()
        var secondHandlerCalled = false

        registry.register {
            // Register a new handler during iteration
            registry.register {
                secondHandlerCalled = true
                true
            }
            false
        }

        registry.handleBack()

        // First call won't invoke newly registered handler (already iterating)
        // But the handler should be registered for future calls
        assertFalse(secondHandlerCalled)

        // Second call should invoke the new handler
        registry.handleBack()
        assertTrue(secondHandlerCalled)
    }
}
