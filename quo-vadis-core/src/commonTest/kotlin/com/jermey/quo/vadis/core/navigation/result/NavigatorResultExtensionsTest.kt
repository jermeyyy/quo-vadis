@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.result

import com.jermey.quo.vadis.core.navigation.FakeNavigator
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.ResultCapable
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private object HomeDest : NavDestination

private object DetailDest : NavDestination

class NavigatorResultExtensionsTest : FunSpec({

    // =========================================================================
    // navigateBackWithResult
    // =========================================================================

    test("navigateBackWithResult pops the current screen") {
        val navigator = FakeNavigator().apply { initializeWithDestination(HomeDest) }
        navigator.navigate(DetailDest)

        navigator.navigateBackWithResult("result-value")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult completes pending result before popping") {
        val navigator = FakeNavigator().apply { initializeWithDestination(HomeDest) }
        navigator.navigate(DetailDest)
        val screenKey = navigator.state.value.activeLeaf()?.key?.value
        screenKey.shouldNotBeNull()

        val resultManager = (navigator as ResultCapable).resultManager
        val deferred = resultManager.requestResult(screenKey)

        navigator.navigateBackWithResult("my-result")

        deferred.isCompleted.shouldBeTrue()
        deferred.await() shouldBe "my-result"
        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult without pending result still navigates back") {
        val navigator = FakeNavigator().apply { initializeWithDestination(HomeDest) }
        navigator.navigate(DetailDest)

        navigator.navigateBackWithResult("orphan-result")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult from root with no pending result stays on root") {
        val navigator = FakeNavigator().apply { initializeWithDestination(HomeDest) }

        navigator.navigateBackWithResult("test")

        navigator.currentDestination.value shouldBe HomeDest
    }

})
