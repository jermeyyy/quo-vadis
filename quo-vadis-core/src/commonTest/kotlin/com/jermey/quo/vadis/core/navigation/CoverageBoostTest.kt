@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.destination.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.GeneratedTabMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.runBlocking

// Simple destination for testing NavDestination defaults
private data object SimpleDestination : NavDestination

/**
 * Quick-win coverage tests for small uncovered classes in the navigation package.
 *
 * Targets:
 * - NavigationResultManager (public, com.jermey.quo.vadis.core.navigation)
 * - DeepLinkResult (Matched / NotMatched)
 * - NavDestination default properties
 * - GeneratedTabMetadata data class
 */
class CoverageBoostTest : FunSpec({

    // =========================================================================
    // NavigationResultManager (public package version)
    // =========================================================================

    test("NavigationResultManager starts with zero pending results") {
        val manager = NavigationResultManager()
        manager.pendingCount() shouldBe 0
        manager.hasPendingResult("any-key").shouldBeFalse()
    }

    test("NavigationResultManager requestResult creates a pending deferred") {
        val manager = NavigationResultManager()
        val deferred = runBlocking { manager.requestResult("screen-1") }

        manager.hasPendingResult("screen-1").shouldBeTrue()
        manager.pendingCount() shouldBe 1
        deferred.isCompleted.shouldBeFalse()
    }

    test("NavigationResultManager completeResultSync delivers result") {
        val manager = NavigationResultManager()
        val deferred = runBlocking { manager.requestResult("screen-1") }

        manager.completeResultSync("screen-1", "hello")

        deferred.isCompleted.shouldBeTrue()
        runBlocking { deferred.await() } shouldBe "hello"
        manager.pendingCount() shouldBe 0
    }

    test("NavigationResultManager completeResultSync for unknown key is no-op") {
        val manager = NavigationResultManager()
        manager.completeResultSync("unknown", "value")
        manager.pendingCount() shouldBe 0
    }

    test("NavigationResultManager cancelResult completes with null") {
        val manager = NavigationResultManager()
        val deferred = runBlocking { manager.requestResult("screen-1") }

        runBlocking { manager.cancelResult("screen-1") }

        deferred.isCompleted.shouldBeTrue()
        runBlocking { deferred.await() }.shouldBeNull()
        manager.pendingCount() shouldBe 0
    }

    test("NavigationResultManager cancelResult for unknown key is no-op") {
        val manager = NavigationResultManager()
        runBlocking { manager.cancelResult("unknown") }
        manager.pendingCount() shouldBe 0
    }

    // =========================================================================
    // DeepLinkResult
    // =========================================================================

    test("DeepLinkResult.Matched holds destination") {
        val dest = SimpleDestination
        val result: DeepLinkResult = DeepLinkResult.Matched(dest)
        result.shouldBeInstanceOf<DeepLinkResult.Matched>()
        result.destination shouldBe dest
    }

    test("DeepLinkResult.NotMatched is singleton") {
        val result: DeepLinkResult = DeepLinkResult.NotMatched
        result.shouldBeInstanceOf<DeepLinkResult.NotMatched>()
    }

    // =========================================================================
    // NavDestination default properties
    // =========================================================================

    test("NavDestination default data is null") {
        SimpleDestination.data.shouldBeNull()
    }

    test("NavDestination default transition is null") {
        SimpleDestination.transition.shouldBeNull()
    }

    // =========================================================================
    // GeneratedTabMetadata
    // =========================================================================

    test("GeneratedTabMetadata stores route") {
        val metadata = GeneratedTabMetadata(route = "home/feed")
        metadata.route shouldBe "home/feed"
    }

    test("GeneratedTabMetadata data class equality") {
        val a = GeneratedTabMetadata(route = "tab1")
        val b = GeneratedTabMetadata(route = "tab1")
        a shouldBe b
    }
})
