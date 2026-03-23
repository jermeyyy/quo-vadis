@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class NavigationResultManagerTest : FunSpec({

    test("pendingCount is zero initially") {
        val manager = NavigationResultManager()
        manager.pendingCount() shouldBe 0
    }

    test("requestResult creates a pending result") {
        val manager = NavigationResultManager()
        manager.requestResult("screen-1")
        manager.hasPendingResult("screen-1").shouldBeTrue()
        manager.pendingCount() shouldBe 1
    }

    test("hasPendingResult returns false for unknown key") {
        val manager = NavigationResultManager()
        manager.hasPendingResult("unknown").shouldBeFalse()
    }

    test("completeResultSync delivers value and removes pending entry") {
        val manager = NavigationResultManager()
        val deferred = manager.requestResult("screen-1")
        manager.completeResultSync("screen-1", "hello")
        deferred.await() shouldBe "hello"
        manager.hasPendingResult("screen-1").shouldBeFalse()
        manager.pendingCount() shouldBe 0
    }

    test("completeResultSync with null result delivers null") {
        val manager = NavigationResultManager()
        val deferred = manager.requestResult("screen-1")
        manager.completeResultSync("screen-1", null)
        deferred.await().shouldBeNull()
    }

    test("completeResultSync for unknown key is a no-op") {
        val manager = NavigationResultManager()
        // Should not throw
        manager.completeResultSync("unknown", "data")
        manager.pendingCount() shouldBe 0
    }

    test("cancelResult completes deferred with null") {
        val manager = NavigationResultManager()
        val deferred = manager.requestResult("screen-1")
        manager.cancelResult("screen-1")
        deferred.await().shouldBeNull()
        manager.hasPendingResult("screen-1").shouldBeFalse()
    }

    test("cancelResult for unknown key is a no-op") {
        val manager = NavigationResultManager()
        // Should not throw
        manager.cancelResult("unknown")
        manager.pendingCount() shouldBe 0
    }

    test("multiple pending results tracked independently") {
        val manager = NavigationResultManager()
        val d1 = manager.requestResult("screen-1")
        val d2 = manager.requestResult("screen-2")
        manager.pendingCount() shouldBe 2

        manager.completeResultSync("screen-1", 42)
        d1.await() shouldBe 42
        manager.pendingCount() shouldBe 1
        manager.hasPendingResult("screen-2").shouldBeTrue()

        manager.cancelResult("screen-2")
        d2.await().shouldBeNull()
        manager.pendingCount() shouldBe 0
    }

    test("requestResult cancels previous pending result for same key") {
        val manager = NavigationResultManager()
        val d1 = manager.requestResult("screen-1")
        val d2 = manager.requestResult("screen-1")
        manager.pendingCount() shouldBe 1

        // d1 was completed with null when d2 was requested
        d1.isCompleted.shouldBeTrue()
        d1.await().shouldBeNull()

        manager.completeResultSync("screen-1", "result")
        d2.await() shouldBe "result"
    }

    test("completeResultSync delivers typed result values") {
        val manager = NavigationResultManager()
        val deferred = manager.requestResult("screen-1")
        val payload = listOf(1, 2, 3)
        manager.completeResultSync("screen-1", payload)
        deferred.await() shouldBe payload
    }
})
