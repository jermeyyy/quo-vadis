package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Simple test destination for registry tests.
 */
private data class TestDestination(
    val id: String,
    val ref: String? = null
) : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

/**
 * Unit tests for [RuntimeDeepLinkRegistry].
 *
 * Tests cover:
 * - Pattern registration and resolution
 * - Path parameter extraction
 * - Query parameter handling
 * - canHandle checks
 * - getRegisteredPatterns
 */
class RuntimeDeepLinkRegistryTest {

    @Test
    fun `register and resolve pattern with path params`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        val destination = registry.resolve("app://profile/123")
        assertNotNull(destination)
        assertTrue(destination is TestDestination)
        assertEquals("123", (destination as TestDestination).id)
    }

    @Test
    fun `resolve includes query params in allParams`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{id}") { params ->
            TestDestination(id = params["id"]!!, ref = params["ref"])
        }

        val destination = registry.resolve("app://profile/123?ref=email")
        assertNotNull(destination)
        assertTrue(destination is TestDestination)
        assertEquals("123", (destination as TestDestination).id)
        assertEquals("email", (destination as TestDestination).ref)
    }

    @Test
    fun `resolve returns null for unmatched patterns`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        val destination = registry.resolve("app://settings")
        assertNull(destination)
    }

    @Test
    fun `canHandle returns true for registered patterns`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        assertTrue(registry.canHandle("app://profile/123"))
        assertFalse(registry.canHandle("app://settings"))
    }

    @Test
    fun `getRegisteredPatterns returns all patterns`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { TestDestination(it["userId"]!!) }
        registry.register("settings") { TestDestination("settings") }

        val patterns = registry.getRegisteredPatterns()
        assertEquals(2, patterns.size)
        assertTrue(patterns.contains("profile/{userId}"))
        assertTrue(patterns.contains("settings"))
    }

    @Test
    fun `pattern matching handles multiple path params`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("user/{userId}/post/{postId}") { params ->
            TestDestination(id = "${params["userId"]}-${params["postId"]}")
        }

        val destination = registry.resolve("app://user/42/post/99")
        assertNotNull(destination)
        assertEquals("42-99", (destination as TestDestination).id)
    }

    @Test
    fun `pattern matching handles static segments`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("home/feed") { TestDestination("feed") }
        registry.register("home/detail/{id}") { TestDestination(it["id"]!!) }

        val feedDest = registry.resolve("app://home/feed")
        assertNotNull(feedDest)
        assertEquals("feed", (feedDest as TestDestination).id)

        val detailDest = registry.resolve("app://home/detail/123")
        assertNotNull(detailDest)
        assertEquals("123", (detailDest as TestDestination).id)
    }

    @Test
    fun `resolve with DeepLink object works`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{id}") { params ->
            TestDestination(id = params["id"]!!)
        }

        val deepLink = DeepLink.parse("app://profile/456")
        val destination = registry.resolve(deepLink)

        assertNotNull(destination)
        assertEquals("456", (destination as TestDestination).id)
    }

    @Test
    fun `canHandle works with different schemes`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        // canHandle checks path pattern regardless of scheme
        assertTrue(registry.canHandle("app://profile/123"))
        assertTrue(registry.canHandle("myapp://profile/456"))
        assertTrue(registry.canHandle("https://profile/789"))
    }

    @Test
    fun `createUri returns null for runtime registry`() {
        val registry = RuntimeDeepLinkRegistry()
        registry.register("profile/{id}") { TestDestination(it["id"]!!) }

        val destination = TestDestination("123")
        // Runtime registry doesn't track destination-to-route mapping
        assertNull(registry.createUri(destination))
    }

    @Test
    fun `registerAction and getRegisteredPatterns includes action patterns`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{id}") { TestDestination(it["id"]!!) }
        registry.registerAction("navigate/{target}") { _, _ -> }

        val patterns = registry.getRegisteredPatterns()
        assertEquals(2, patterns.size)
        assertTrue(patterns.contains("profile/{id}"))
        assertTrue(patterns.contains("navigate/{target}"))
    }

    @Test
    fun `empty registry returns empty patterns`() {
        val registry = RuntimeDeepLinkRegistry()

        assertEquals(emptyList(), registry.getRegisteredPatterns())
        assertFalse(registry.canHandle("app://anything"))
    }

    @Test
    fun `pattern with no params matches exact path`() {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("home") { TestDestination("home") }

        val destination = registry.resolve("app://home")
        assertNotNull(destination)
        assertEquals("home", (destination as TestDestination).id)

        // Should not match different paths
        assertNull(registry.resolve("app://home/extra"))
        assertNull(registry.resolve("app://homex"))
    }
}

/**
 * Unit tests for [CompositeDeepLinkRegistry].
 *
 * Tests cover:
 * - Runtime registry precedence over generated
 * - Fallback to generated registry
 * - Combined pattern listing
 * - canHandle across both registries
 */
class CompositeDeepLinkRegistryTest {

    @Test
    fun `runtime registry takes precedence over generated`() {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination("generated-${it["id"]}") }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)

        // Register override in runtime
        composite.register("profile/{id}") { TestDestination("runtime-${it["id"]}") }

        val destination = composite.resolve("app://profile/123")
        assertNotNull(destination)
        assertEquals("runtime-123", (destination as TestDestination).id)
    }

    @Test
    fun `falls back to generated registry when runtime has no match`() {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination("generated-${it["id"]}") }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)

        val destination = composite.resolve("app://profile/123")
        assertNotNull(destination)
        assertEquals("generated-123", (destination as TestDestination).id)
    }

    @Test
    fun `getRegisteredPatterns combines both registries`() {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination(it["id"]!!) }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)
        composite.register("settings") { TestDestination("settings") }

        val patterns = composite.getRegisteredPatterns()
        assertEquals(2, patterns.size)
        assertTrue(patterns.contains("settings"))
        assertTrue(patterns.contains("profile/{id}"))
    }

    @Test
    fun `canHandle checks both registries`() {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination(it["id"]!!) }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)
        composite.register("settings") { TestDestination("settings") }

        assertTrue(composite.canHandle("app://profile/123"))
        assertTrue(composite.canHandle("app://settings"))
        assertFalse(composite.canHandle("app://unknown"))
    }

    @Test
    fun `resolve with null generated registry`() {
        val composite = CompositeDeepLinkRegistry(null)
        composite.register("profile/{id}") { TestDestination(it["id"]!!) }

        val destination = composite.resolve("app://profile/123")
        assertNotNull(destination)
        assertEquals("123", (destination as TestDestination).id)

        // No fallback available
        assertNull(composite.resolve("app://unknown"))
    }

    @Test
    fun `canHandle with null generated registry`() {
        val composite = CompositeDeepLinkRegistry(null)
        composite.register("profile/{id}") { TestDestination(it["id"]!!) }

        assertTrue(composite.canHandle("app://profile/123"))
        assertFalse(composite.canHandle("app://unknown"))
    }

    @Test
    fun `getRegisteredPatterns with null generated registry`() {
        val composite = CompositeDeepLinkRegistry(null)
        composite.register("settings") { TestDestination("settings") }

        val patterns = composite.getRegisteredPatterns()
        assertEquals(1, patterns.size)
        assertTrue(patterns.contains("settings"))
    }

    @Test
    fun `resolve DeepLink object uses both registries`() {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("generated-path") { TestDestination("from-generated") }
        }
        val composite = CompositeDeepLinkRegistry(generated)
        composite.register("runtime-path") { TestDestination("from-runtime") }

        val runtimeDeepLink = DeepLink.parse("app://runtime-path")
        val generatedDeepLink = DeepLink.parse("app://generated-path")

        val runtimeDest = composite.resolve(runtimeDeepLink)
        val generatedDest = composite.resolve(generatedDeepLink)

        assertNotNull(runtimeDest)
        assertEquals("from-runtime", (runtimeDest as TestDestination).id)

        assertNotNull(generatedDest)
        assertEquals("from-generated", (generatedDest as TestDestination).id)
    }

    @Test
    fun `registerAction delegates to runtime`() {
        val composite = CompositeDeepLinkRegistry(null)

        var actionCalled = false
        composite.registerAction("action/{id}") { _, _ ->
            actionCalled = true
        }

        val patterns = composite.getRegisteredPatterns()
        assertTrue(patterns.contains("action/{id}"))
        assertTrue(composite.canHandle("app://action/123"))
    }

    @Test
    fun `createUri delegates to generated registry`() {
        // Generated registry with createUri implementation could return a value
        // but RuntimeDeepLinkRegistry returns null
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination(it["id"]!!) }
        }
        val composite = CompositeDeepLinkRegistry(generated)

        val destination = TestDestination("123")
        // RuntimeDeepLinkRegistry.createUri returns null
        assertNull(composite.createUri(destination))
    }

    @Test
    fun `createUri returns null when generated is null`() {
        val composite = CompositeDeepLinkRegistry(null)

        val destination = TestDestination("123")
        assertNull(composite.createUri(destination))
    }
}
