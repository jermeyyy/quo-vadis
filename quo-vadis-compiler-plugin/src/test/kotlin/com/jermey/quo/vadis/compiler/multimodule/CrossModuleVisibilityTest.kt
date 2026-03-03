package com.jermey.quo.vadis.compiler.multimodule

import com.jermey.quo.vadis.core.navigation.config.GeneratedNavigationConfig
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests that verify cross-module visibility of the [GeneratedNavigationConfig]
 * marker interface for Phase 4 multi-module auto-discovery.
 *
 * These tests ensure that:
 * - [GeneratedNavigationConfig] extends [NavigationConfig]
 * - The marker interface is visible from dependent modules
 * - Generated configs with this supertype can be discovered via classpath scanning
 */
class CrossModuleVisibilityTest {

    @Test
    fun `GeneratedNavigationConfig extends NavigationConfig`() {
        val superInterfaces = GeneratedNavigationConfig::class.supertypes
        assertTrue(
            superInterfaces.any { it.classifier == NavigationConfig::class },
            "GeneratedNavigationConfig should extend NavigationConfig"
        )
    }

    @Test
    fun `GeneratedNavigationConfig is an interface`() {
        assertTrue(
            GeneratedNavigationConfig::class.java.isInterface,
            "GeneratedNavigationConfig should be an interface"
        )
    }

    @Test
    fun `GeneratedNavigationConfig does not add members beyond NavigationConfig`() {
        val generatedMembers = GeneratedNavigationConfig::class.members.map { it.name }.toSet()
        val navigationConfigMembers = NavigationConfig::class.members.map { it.name }.toSet()

        // GeneratedNavigationConfig is a pure marker — it should only have members
        // inherited from NavigationConfig (plus standard Any members)
        val ownMembers = generatedMembers - navigationConfigMembers
        assertTrue(
            ownMembers.isEmpty(),
            "GeneratedNavigationConfig should not add any members beyond NavigationConfig, " +
                "but found: $ownMembers"
        )
    }

    @Test
    fun `GeneratedNavigationConfig has exactly one direct supertype`() {
        val supertypes = GeneratedNavigationConfig::class.supertypes
        // Should have exactly one declared supertype: NavigationConfig
        // (Any is implicit and may or may not appear depending on reflection implementation)
        val nonAnySupertypes = supertypes.filter { it.classifier != Any::class }
        assertEquals(
            1,
            nonAnySupertypes.size,
            "GeneratedNavigationConfig should have exactly one non-Any supertype (NavigationConfig)"
        )
        assertEquals(
            NavigationConfig::class,
            nonAnySupertypes.first().classifier,
            "The single supertype should be NavigationConfig"
        )
    }

    @Test
    fun `anonymous GeneratedNavigationConfig implementor is assignable to NavigationConfig`() {
        // Verify that an object implementing GeneratedNavigationConfig
        // is also assignable to NavigationConfig (interface hierarchy)
        val configClass = GeneratedNavigationConfig::class.java
        val navigationConfigClass = NavigationConfig::class.java
        assertTrue(
            navigationConfigClass.isAssignableFrom(configClass),
            "NavigationConfig should be assignable from GeneratedNavigationConfig"
        )
    }
}
