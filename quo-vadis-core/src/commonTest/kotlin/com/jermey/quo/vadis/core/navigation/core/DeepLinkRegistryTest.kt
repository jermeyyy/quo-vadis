package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.registry.internal.CompositeDeepLinkRegistry
import com.jermey.quo.vadis.core.registry.internal.RuntimeDeepLinkRegistry
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

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
 * Unit tests for [com.jermey.quo.vadis.core.dsl.registry.RuntimeDeepLinkRegistry].
 *
 * Tests cover:
 * - Pattern registration and resolution
 * - Path parameter extraction
 * - Query parameter handling
 * - canHandle checks
 * - getRegisteredPatterns
 */
@OptIn(InternalQuoVadisApi::class)
class RuntimeDeepLinkRegistryTest : FunSpec({

    test("register and resolve pattern with path params") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        val destination = registry.resolve("app://profile/123")
        destination.shouldNotBeNull()
        (destination is TestDestination).shouldBeTrue()
        (destination as TestDestination).id shouldBe "123"
    }

    test("resolve includes query params in allParams") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{id}") { params ->
            TestDestination(id = params["id"]!!, ref = params["ref"])
        }

        val destination = registry.resolve("app://profile/123?ref=email")
        destination.shouldNotBeNull()
        (destination is TestDestination).shouldBeTrue()
        (destination as TestDestination).id shouldBe "123"
        (destination as TestDestination).ref shouldBe "email"
    }

    test("resolve returns null for unmatched patterns") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        val destination = registry.resolve("app://settings")
        destination.shouldBeNull()
    }

    test("canHandle returns true for registered patterns") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        registry.canHandle("app://profile/123").shouldBeTrue()
        registry.canHandle("app://settings").shouldBeFalse()
    }

    test("getRegisteredPatterns returns all patterns") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { TestDestination(it["userId"]!!) }
        registry.register("settings") { TestDestination("settings") }

        val patterns = registry.getRegisteredPatterns()
        patterns.size shouldBe 2
        patterns.contains("profile/{userId}").shouldBeTrue()
        patterns.contains("settings").shouldBeTrue()
    }

    test("pattern matching handles multiple path params") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("user/{userId}/post/{postId}") { params ->
            TestDestination(id = "${params["userId"]}-${params["postId"]}")
        }

        val destination = registry.resolve("app://user/42/post/99")
        destination.shouldNotBeNull()
        (destination as TestDestination).id shouldBe "42-99"
    }

    test("pattern matching handles static segments") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("home/feed") { TestDestination("feed") }
        registry.register("home/detail/{id}") { TestDestination(it["id"]!!) }

        val feedDest = registry.resolve("app://home/feed")
        feedDest.shouldNotBeNull()
        (feedDest as TestDestination).id shouldBe "feed"

        val detailDest = registry.resolve("app://home/detail/123")
        detailDest.shouldNotBeNull()
        (detailDest as TestDestination).id shouldBe "123"
    }

    test("resolve with DeepLink object works") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{id}") { params ->
            TestDestination(id = params["id"]!!)
        }

        val deepLink = DeepLink.parse("app://profile/456")
        val destination = registry.resolve(deepLink)

        destination.shouldNotBeNull()
        (destination as TestDestination).id shouldBe "456"
    }

    test("canHandle works with different schemes") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{userId}") { params ->
            TestDestination(id = params["userId"]!!)
        }

        // canHandle checks path pattern regardless of scheme
        registry.canHandle("app://profile/123").shouldBeTrue()
        registry.canHandle("myapp://profile/456").shouldBeTrue()
        registry.canHandle("https://profile/789").shouldBeTrue()
    }

    test("createUri returns null for runtime registry") {
        val registry = RuntimeDeepLinkRegistry()
        registry.register("profile/{id}") { TestDestination(it["id"]!!) }

        val destination = TestDestination("123")
        // Runtime registry doesn't track destination-to-route mapping
        registry.createUri(destination).shouldBeNull()
    }

    test("registerAction and getRegisteredPatterns includes action patterns") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("profile/{id}") { TestDestination(it["id"]!!) }
        registry.registerAction("navigate/{target}") { _, _ -> }

        val patterns = registry.getRegisteredPatterns()
        patterns.size shouldBe 2
        patterns.contains("profile/{id}").shouldBeTrue()
        patterns.contains("navigate/{target}").shouldBeTrue()
    }

    test("empty registry returns empty patterns") {
        val registry = RuntimeDeepLinkRegistry()

        registry.getRegisteredPatterns() shouldBe emptyList()
        registry.canHandle("app://anything").shouldBeFalse()
    }

    test("pattern with no params matches exact path") {
        val registry = RuntimeDeepLinkRegistry()

        registry.register("home") { TestDestination("home") }

        val destination = registry.resolve("app://home")
        destination.shouldNotBeNull()
        (destination as TestDestination).id shouldBe "home"

        // Should not match different paths
        registry.resolve("app://home/extra").shouldBeNull()
        registry.resolve("app://homex").shouldBeNull()
    }
})

/**
 * Unit tests for [com.jermey.quo.vadis.core.dsl.registry.CompositeDeepLinkRegistry].
 *
 * Tests cover:
 * - Runtime registry precedence over generated
 * - Fallback to generated registry
 * - Combined pattern listing
 * - canHandle across both registries
 */
@OptIn(InternalQuoVadisApi::class)
class CompositeDeepLinkRegistryTest : FunSpec({

    test("runtime registry takes precedence over generated") {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination("generated-${it["id"]}") }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)

        // Register override in runtime
        composite.register("profile/{id}") { TestDestination("runtime-${it["id"]}") }

        val destination = composite.resolve("app://profile/123")
        destination.shouldNotBeNull()
        (destination as TestDestination).id shouldBe "runtime-123"
    }

    test("falls back to generated registry when runtime has no match") {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination("generated-${it["id"]}") }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)

        val destination = composite.resolve("app://profile/123")
        destination.shouldNotBeNull()
        (destination as TestDestination).id shouldBe "generated-123"
    }

    test("getRegisteredPatterns combines both registries") {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination(it["id"]!!) }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)
        composite.register("settings") { TestDestination("settings") }

        val patterns = composite.getRegisteredPatterns()
        patterns.size shouldBe 2
        patterns.contains("settings").shouldBeTrue()
        patterns.contains("profile/{id}").shouldBeTrue()
    }

    test("canHandle checks both registries") {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination(it["id"]!!) }
        }
        val runtime = RuntimeDeepLinkRegistry()

        val composite = CompositeDeepLinkRegistry(generated, runtime)
        composite.register("settings") { TestDestination("settings") }

        composite.canHandle("app://profile/123").shouldBeTrue()
        composite.canHandle("app://settings").shouldBeTrue()
        composite.canHandle("app://unknown").shouldBeFalse()
    }

    test("resolve with null generated registry") {
        val composite = CompositeDeepLinkRegistry(null)
        composite.register("profile/{id}") { TestDestination(it["id"]!!) }

        val destination = composite.resolve("app://profile/123")
        destination.shouldNotBeNull()
        (destination as TestDestination).id shouldBe "123"

        // No fallback available
        composite.resolve("app://unknown").shouldBeNull()
    }

    test("canHandle with null generated registry") {
        val composite = CompositeDeepLinkRegistry(null)
        composite.register("profile/{id}") { TestDestination(it["id"]!!) }

        composite.canHandle("app://profile/123").shouldBeTrue()
        composite.canHandle("app://unknown").shouldBeFalse()
    }

    test("getRegisteredPatterns with null generated registry") {
        val composite = CompositeDeepLinkRegistry(null)
        composite.register("settings") { TestDestination("settings") }

        val patterns = composite.getRegisteredPatterns()
        patterns.size shouldBe 1
        patterns.contains("settings").shouldBeTrue()
    }

    test("resolve DeepLink object uses both registries") {
        val generated = RuntimeDeepLinkRegistry().apply {
            register("generated-path") { TestDestination("from-generated") }
        }
        val composite = CompositeDeepLinkRegistry(generated)
        composite.register("runtime-path") { TestDestination("from-runtime") }

        val runtimeDeepLink = DeepLink.parse("app://runtime-path")
        val generatedDeepLink = DeepLink.parse("app://generated-path")

        val runtimeDest = composite.resolve(runtimeDeepLink)
        val generatedDest = composite.resolve(generatedDeepLink)

        runtimeDest.shouldNotBeNull()
        (runtimeDest as TestDestination).id shouldBe "from-runtime"

        generatedDest.shouldNotBeNull()
        (generatedDest as TestDestination).id shouldBe "from-generated"
    }

    test("registerAction delegates to runtime") {
        val composite = CompositeDeepLinkRegistry(null)

        var actionCalled = false
        composite.registerAction("action/{id}") { _, _ ->
            actionCalled = true
        }

        val patterns = composite.getRegisteredPatterns()
        patterns.contains("action/{id}").shouldBeTrue()
        composite.canHandle("app://action/123").shouldBeTrue()
    }

    test("createUri delegates to generated registry") {
        // Generated registry with createUri implementation could return a value
        // but RuntimeDeepLinkRegistry returns null
        val generated = RuntimeDeepLinkRegistry().apply {
            register("profile/{id}") { TestDestination(it["id"]!!) }
        }
        val composite = CompositeDeepLinkRegistry(generated)

        val destination = TestDestination("123")
        // RuntimeDeepLinkRegistry.createUri returns null
        composite.createUri(destination).shouldBeNull()
    }

    test("createUri returns null when generated is null") {
        val composite = CompositeDeepLinkRegistry(null)

        val destination = TestDestination("123")
        composite.createUri(destination).shouldBeNull()
    }
})
