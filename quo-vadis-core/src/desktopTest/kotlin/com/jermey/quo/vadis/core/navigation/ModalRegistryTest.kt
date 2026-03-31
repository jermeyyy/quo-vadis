@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.dsl.internal.DslModalRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.ModalRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeModalRegistry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

// =========================================================================
// TEST DESTINATIONS
// =========================================================================

private data object ModalDestination : NavDestination {
    override val transition: NavigationTransition? = null
}

private data object RegularDestination : NavDestination {
    override val transition: NavigationTransition? = null
}

private data object AnotherModalDestination : NavDestination {
    override val transition: NavigationTransition? = null
}

/**
 * Tests for [ModalRegistry] implementations:
 * - [DslModalRegistry]
 * - [CompositeModalRegistry]
 * - [ModalRegistry.Empty]
 */
class ModalRegistryTest : FunSpec({

    // =========================================================================
    // DslModalRegistry TESTS
    // =========================================================================

    test("isModalDestination returns true for registered destination") {
        val registry = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = emptySet()
        )

        registry.isModalDestination(ModalDestination::class).shouldBeTrue()
    }

    test("isModalDestination returns false for unregistered destination") {
        val registry = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = emptySet()
        )

        registry.isModalDestination(RegularDestination::class).shouldBeFalse()
    }

    test("isModalContainer returns true for registered container") {
        val registry = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("bottom-sheet")
        )

        registry.isModalContainer("bottom-sheet").shouldBeTrue()
    }

    test("isModalContainer returns false for unregistered container") {
        val registry = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("bottom-sheet")
        )

        registry.isModalContainer("dialog").shouldBeFalse()
    }

    test("empty sets return false for all inputs") {
        val registry = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )

        registry.isModalDestination(ModalDestination::class).shouldBeFalse()
        registry.isModalDestination(RegularDestination::class).shouldBeFalse()
        registry.isModalContainer("anything").shouldBeFalse()
        registry.isModalContainer("").shouldBeFalse()
    }

    // =========================================================================
    // CompositeModalRegistry TESTS
    // =========================================================================

    test("composite returns true when primary says modal") {
        val primary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("primary-container")
        )
        val secondary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val composite = CompositeModalRegistry(primary, secondary)

        composite.isModalDestination(ModalDestination::class).shouldBeTrue()
        composite.isModalContainer("primary-container").shouldBeTrue()
    }

    test("composite returns true when secondary says modal") {
        val primary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val secondary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("secondary-container")
        )
        val composite = CompositeModalRegistry(primary, secondary)

        composite.isModalDestination(ModalDestination::class).shouldBeTrue()
        composite.isModalContainer("secondary-container").shouldBeTrue()
    }

    test("composite returns true when both say modal") {
        val primary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("shared-container")
        )
        val secondary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("shared-container")
        )
        val composite = CompositeModalRegistry(primary, secondary)

        composite.isModalDestination(ModalDestination::class).shouldBeTrue()
        composite.isModalContainer("shared-container").shouldBeTrue()
    }

    test("composite returns false when neither says modal") {
        val primary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val secondary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val composite = CompositeModalRegistry(primary, secondary)

        composite.isModalDestination(ModalDestination::class).shouldBeFalse()
        composite.isModalContainer("any-container").shouldBeFalse()
    }

    test("composite unions destinations from both registries") {
        val primary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = emptySet()
        )
        val secondary = DslModalRegistry(
            modalDestinations = setOf(AnotherModalDestination::class),
            modalContainers = emptySet()
        )
        val composite = CompositeModalRegistry(primary, secondary)

        composite.isModalDestination(ModalDestination::class).shouldBeTrue()
        composite.isModalDestination(AnotherModalDestination::class).shouldBeTrue()
        composite.isModalDestination(RegularDestination::class).shouldBeFalse()
    }

    test("composite unions containers from both registries") {
        val primary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("bottom-sheet")
        )
        val secondary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("dialog")
        )
        val composite = CompositeModalRegistry(primary, secondary)

        composite.isModalContainer("bottom-sheet").shouldBeTrue()
        composite.isModalContainer("dialog").shouldBeTrue()
        composite.isModalContainer("unknown").shouldBeFalse()
    }

    // =========================================================================
    // ModalRegistry.Empty TESTS
    // =========================================================================

    test("Empty returns false for any destination class") {
        ModalRegistry.Empty.isModalDestination(ModalDestination::class).shouldBeFalse()
        ModalRegistry.Empty.isModalDestination(RegularDestination::class).shouldBeFalse()
        ModalRegistry.Empty.isModalDestination(AnotherModalDestination::class).shouldBeFalse()
    }

    test("Empty returns false for any container key") {
        ModalRegistry.Empty.isModalContainer("bottom-sheet").shouldBeFalse()
        ModalRegistry.Empty.isModalContainer("dialog").shouldBeFalse()
        ModalRegistry.Empty.isModalContainer("").shouldBeFalse()
        ModalRegistry.Empty.isModalContainer("any-key").shouldBeFalse()
    }
})
