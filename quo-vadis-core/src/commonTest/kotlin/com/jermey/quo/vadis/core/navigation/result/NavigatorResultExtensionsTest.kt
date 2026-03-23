@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.result

import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.NavigationResultManager
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeNavigator
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.testing.withDestination
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private object HomeDest : NavDestination

private object DetailDest : NavDestination

class NavigatorResultExtensionsTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // navigateBackWithResult
    // =========================================================================

    test("navigateBackWithResult pops the current screen") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.navigateBackWithResult("result-value")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult completes pending result before popping") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)
        val screenKey = navigator.state.value.activeLeaf()?.key?.value
        screenKey.shouldNotBeNull()

        navigator.navigateBackWithResult("my-result")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult without pending result still navigates back") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.navigateBackWithResult("orphan-result")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult from root with no pending result stays on root") {
        val navigator = TreeNavigator.withDestination(HomeDest)

        navigator.navigateBackWithResult("test")

        navigator.currentDestination.value shouldBe HomeDest
    }

    // =========================================================================
    // NavigationResultManager integration
    // =========================================================================

    test("resultManager requestResult creates pending deferred") {
        val manager = NavigationResultManager()

        manager.hasPendingResult("screen-1").shouldBeFalse()

        val deferred = manager.requestResult("screen-1")

        manager.hasPendingResult("screen-1").shouldBeTrue()
        manager.pendingCount() shouldBe 1
        deferred.isCompleted.shouldBeFalse()
    }

    test("resultManager completeResultSync delivers result to deferred") {
        val manager = NavigationResultManager()
        val deferred = manager.requestResult("screen-1")

        manager.completeResultSync("screen-1", "hello")

        deferred.isCompleted.shouldBeTrue()
        deferred.await() shouldBe "hello"
    }

    test("resultManager cancelResult delivers null") {
        val manager = NavigationResultManager()
        val deferred = manager.requestResult("screen-1")

        manager.cancelResult("screen-1")

        deferred.isCompleted.shouldBeTrue()
        deferred.await().shouldBeNull()
    }

    test("resultManager completeResultSync for unknown key is no-op") {
        val manager = NavigationResultManager()

        // Should not throw
        manager.completeResultSync("unknown-key", "value")

        manager.pendingCount() shouldBe 0
    }

    test("resultManager cancelResult for unknown key is no-op") {
        val manager = NavigationResultManager()

        // Should not throw
        manager.cancelResult("unknown-key")

        manager.pendingCount() shouldBe 0
    }

    test("resultManager supports multiple pending results") {
        val manager = NavigationResultManager()
        val deferred1 = manager.requestResult("s1")
        val deferred2 = manager.requestResult("s2")

        manager.pendingCount() shouldBe 2

        manager.completeResultSync("s1", "result-a")
        manager.pendingCount() shouldBe 1

        manager.completeResultSync("s2", "result-b")
        manager.pendingCount() shouldBe 0

        deferred1.await() shouldBe "result-a"
        deferred2.await() shouldBe "result-b"
    }
})
