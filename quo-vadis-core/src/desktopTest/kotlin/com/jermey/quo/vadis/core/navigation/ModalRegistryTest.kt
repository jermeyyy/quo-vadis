@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.dsl.internal.DslModalRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.ModalRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeModalRegistry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [ModalRegistry] implementations:
 * - [DslModalRegistry]
 * - [CompositeModalRegistry]
 * - [ModalRegistry.Empty]
 */
class ModalRegistryTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private data object ModalDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private data object RegularDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private data object AnotherModalDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // DslModalRegistry TESTS
    // =========================================================================

    @Test
    fun `isModalDestination returns true for registered destination`() {
        val registry = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = emptySet()
        )

        assertTrue(registry.isModalDestination(ModalDestination::class))
    }

    @Test
    fun `isModalDestination returns false for unregistered destination`() {
        val registry = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = emptySet()
        )

        assertFalse(registry.isModalDestination(RegularDestination::class))
    }

    @Test
    fun `isModalContainer returns true for registered container`() {
        val registry = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("bottom-sheet")
        )

        assertTrue(registry.isModalContainer("bottom-sheet"))
    }

    @Test
    fun `isModalContainer returns false for unregistered container`() {
        val registry = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("bottom-sheet")
        )

        assertFalse(registry.isModalContainer("dialog"))
    }

    @Test
    fun `empty sets return false for all inputs`() {
        val registry = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )

        assertFalse(registry.isModalDestination(ModalDestination::class))
        assertFalse(registry.isModalDestination(RegularDestination::class))
        assertFalse(registry.isModalContainer("anything"))
        assertFalse(registry.isModalContainer(""))
    }

    // =========================================================================
    // CompositeModalRegistry TESTS
    // =========================================================================

    @Test
    fun `composite returns true when primary says modal`() {
        val primary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("primary-container")
        )
        val secondary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val composite = CompositeModalRegistry(primary, secondary)

        assertTrue(composite.isModalDestination(ModalDestination::class))
        assertTrue(composite.isModalContainer("primary-container"))
    }

    @Test
    fun `composite returns true when secondary says modal`() {
        val primary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val secondary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("secondary-container")
        )
        val composite = CompositeModalRegistry(primary, secondary)

        assertTrue(composite.isModalDestination(ModalDestination::class))
        assertTrue(composite.isModalContainer("secondary-container"))
    }

    @Test
    fun `composite returns true when both say modal`() {
        val primary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("shared-container")
        )
        val secondary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = setOf("shared-container")
        )
        val composite = CompositeModalRegistry(primary, secondary)

        assertTrue(composite.isModalDestination(ModalDestination::class))
        assertTrue(composite.isModalContainer("shared-container"))
    }

    @Test
    fun `composite returns false when neither says modal`() {
        val primary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val secondary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = emptySet()
        )
        val composite = CompositeModalRegistry(primary, secondary)

        assertFalse(composite.isModalDestination(ModalDestination::class))
        assertFalse(composite.isModalContainer("any-container"))
    }

    @Test
    fun `composite unions destinations from both registries`() {
        val primary = DslModalRegistry(
            modalDestinations = setOf(ModalDestination::class),
            modalContainers = emptySet()
        )
        val secondary = DslModalRegistry(
            modalDestinations = setOf(AnotherModalDestination::class),
            modalContainers = emptySet()
        )
        val composite = CompositeModalRegistry(primary, secondary)

        assertTrue(composite.isModalDestination(ModalDestination::class))
        assertTrue(composite.isModalDestination(AnotherModalDestination::class))
        assertFalse(composite.isModalDestination(RegularDestination::class))
    }

    @Test
    fun `composite unions containers from both registries`() {
        val primary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("bottom-sheet")
        )
        val secondary = DslModalRegistry(
            modalDestinations = emptySet(),
            modalContainers = setOf("dialog")
        )
        val composite = CompositeModalRegistry(primary, secondary)

        assertTrue(composite.isModalContainer("bottom-sheet"))
        assertTrue(composite.isModalContainer("dialog"))
        assertFalse(composite.isModalContainer("unknown"))
    }

    // =========================================================================
    // ModalRegistry.Empty TESTS
    // =========================================================================

    @Test
    fun `Empty returns false for any destination class`() {
        assertFalse(ModalRegistry.Empty.isModalDestination(ModalDestination::class))
        assertFalse(ModalRegistry.Empty.isModalDestination(RegularDestination::class))
        assertFalse(ModalRegistry.Empty.isModalDestination(AnotherModalDestination::class))
    }

    @Test
    fun `Empty returns false for any container key`() {
        assertFalse(ModalRegistry.Empty.isModalContainer("bottom-sheet"))
        assertFalse(ModalRegistry.Empty.isModalContainer("dialog"))
        assertFalse(ModalRegistry.Empty.isModalContainer(""))
        assertFalse(ModalRegistry.Empty.isModalContainer("any-key"))
    }
}
