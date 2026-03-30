package com.jermey.quo.vadis.core.compose.transition

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NavTransitionConversionTest : FunSpec({

    test("toNavTransition maps all properties correctly") {
        val navigationTransition = object : NavigationTransition {
            override val enter = fadeIn()
            override val exit = slideOutHorizontally()
            override val popEnter = slideInHorizontally()
            override val popExit = fadeOut()
        }

        val result = navigationTransition.toNavTransition()

        result.enter shouldBe navigationTransition.enter
        result.exit shouldBe navigationTransition.exit
        result.popEnter shouldBe navigationTransition.popEnter
        result.popExit shouldBe navigationTransition.popExit
    }
})
