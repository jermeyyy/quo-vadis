@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.registry

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.registry.TransitionRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeTransitionRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private class FakeTransitionRegistry(
    private val transitions: Map<String, NavTransition> = emptyMap()
) : TransitionRegistry {
    override fun getTransition(destinationClass: kotlin.reflect.KClass<*>): NavTransition? {
        return transitions[destinationClass.simpleName]
    }
}

class CompositeTransitionRegistryTest : FunSpec({

    test("returns secondary transition when available") {
        val secondaryTransition = NavTransition.Fade
        val primary = FakeTransitionRegistry()
        val secondary = FakeTransitionRegistry(mapOf("ScreenA" to secondaryTransition))

        val composite = CompositeTransitionRegistry(primary, secondary)

        // Create a simple class to look up by simpleName
        composite.getTransition(ScreenA::class) shouldBe secondaryTransition
    }

    test("falls back to primary when secondary returns null") {
        val primaryTransition = NavTransition.SlideHorizontal
        val primary = FakeTransitionRegistry(mapOf("ScreenB" to primaryTransition))
        val secondary = FakeTransitionRegistry()

        val composite = CompositeTransitionRegistry(primary, secondary)

        composite.getTransition(ScreenB::class) shouldBe primaryTransition
    }

    test("returns null when neither has a match") {
        val primary = FakeTransitionRegistry()
        val secondary = FakeTransitionRegistry()

        val composite = CompositeTransitionRegistry(primary, secondary)

        composite.getTransition(ScreenA::class).shouldBeNull()
    }

    test("secondary takes precedence over primary for same key") {
        val primaryTransition = NavTransition.SlideHorizontal
        val secondaryTransition = NavTransition.Fade
        val primary = FakeTransitionRegistry(mapOf("ScreenA" to primaryTransition))
        val secondary = FakeTransitionRegistry(mapOf("ScreenA" to secondaryTransition))

        val composite = CompositeTransitionRegistry(primary, secondary)

        composite.getTransition(ScreenA::class) shouldBe secondaryTransition
    }

    test("returns primary transition for key only in primary") {
        val primaryTransition = NavTransition.SlideVertical
        val secondaryTransition = NavTransition.Fade
        val primary = FakeTransitionRegistry(
            mapOf("ScreenA" to primaryTransition, "ScreenB" to NavTransition.SlideHorizontal)
        )
        val secondary = FakeTransitionRegistry(mapOf("ScreenB" to secondaryTransition))

        val composite = CompositeTransitionRegistry(primary, secondary)

        composite.getTransition(ScreenA::class) shouldBe primaryTransition
        composite.getTransition(ScreenB::class) shouldBe secondaryTransition
    }
})

// Simple marker classes for KClass lookups
private class ScreenA
private class ScreenB
