package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import com.jermey.quo.vadis.core.navigation.node.NodeKey

/**
 * Tests for [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] interface and implementations.
 */
class ScopeRegistryTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    /**
     * Simulates a sealed interface for tab destinations.
     * In real apps, this would be generated from @Tab annotations.
     */
    private sealed interface MainTabs : NavDestination {
        data object HomeTab : MainTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object SettingsTab : MainTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }
    }

    /**
     * A destination that is NOT part of MainTabs scope.
     */
    private data object OutOfScopeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST REGISTRY IMPLEMENTATION
    // =========================================================================

    /**
     * Test implementation of ScopeRegistry.
     * Simulates what KSP would generate from sealed class hierarchies.
     */
    private val testRegistry = object : ScopeRegistry {
        private val scopes = mapOf(
            "MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.SettingsTab::class)
        )

        override fun isInScope(scopeKey: String, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): String? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key
        }
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS
    // =========================================================================

    @Test
    fun `Empty registry always returns true for isInScope`() {
        assertTrue(ScopeRegistry.Empty.isInScope("AnyScope", MainTabs.HomeTab))
        assertTrue(ScopeRegistry.Empty.isInScope("AnyScope", OutOfScopeDestination))
    }

    @Test
    fun `Empty registry always returns null for getScopeKey`() {
        assertNull(ScopeRegistry.Empty.getScopeKey(MainTabs.HomeTab))
        assertNull(ScopeRegistry.Empty.getScopeKey(OutOfScopeDestination))
    }

    @Test
    fun `Empty registry allows any destination in any scope`() {
        // This is the backward-compatible behavior
        assertTrue(ScopeRegistry.Empty.isInScope("MainTabs", OutOfScopeDestination))
        assertTrue(ScopeRegistry.Empty.isInScope("NonExistent", MainTabs.HomeTab))
        assertTrue(ScopeRegistry.Empty.isInScope("", MainTabs.SettingsTab))
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS - isInScope
    // =========================================================================

    @Test
    fun `isInScope returns true for destinations in scope`() {
        assertTrue(testRegistry.isInScope("MainTabs", MainTabs.HomeTab))
        assertTrue(testRegistry.isInScope("MainTabs", MainTabs.SettingsTab))
    }

    @Test
    fun `isInScope returns false for destinations out of scope`() {
        assertFalse(testRegistry.isInScope("MainTabs", OutOfScopeDestination))
    }

    @Test
    fun `isInScope returns true for unknown scope keys`() {
        // Unknown scope keys allow all destinations (defensive behavior)
        // This prevents crashes when scope configuration is incomplete
        assertTrue(testRegistry.isInScope("UnknownScope", MainTabs.HomeTab))
        assertTrue(testRegistry.isInScope("UnknownScope", OutOfScopeDestination))
    }

    @Test
    fun `isInScope handles empty scope key`() {
        // Empty scope key is treated as unknown (allows all)
        assertTrue(testRegistry.isInScope("", MainTabs.HomeTab))
        assertTrue(testRegistry.isInScope("", OutOfScopeDestination))
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS - getScopeKey
    // =========================================================================

    @Test
    fun `getScopeKey returns correct scope for in-scope destinations`() {
        assertEquals("MainTabs", testRegistry.getScopeKey(MainTabs.HomeTab))
        assertEquals("MainTabs", testRegistry.getScopeKey(MainTabs.SettingsTab))
    }

    @Test
    fun `getScopeKey returns null for out-of-scope destinations`() {
        assertNull(testRegistry.getScopeKey(OutOfScopeDestination))
    }

    // =========================================================================
    // MULTIPLE SCOPES TESTS
    // =========================================================================

    /**
     * Additional destinations for testing multiple scopes.
     */
    private sealed interface ProfileTabs : NavDestination {
        data object OverviewTab : ProfileTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object HistoryTab : ProfileTabs {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }
    }

    /**
     * Registry with multiple scopes.
     */
    private val multiScopeRegistry = object : ScopeRegistry {
        private val scopes = mapOf(
            "MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.SettingsTab::class),
            "ProfileTabs" to setOf(ProfileTabs.OverviewTab::class, ProfileTabs.HistoryTab::class)
        )

        override fun isInScope(scopeKey: String, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): String? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key
        }
    }

    @Test
    fun `Multiple scopes - each destination maps to correct scope`() {
        assertEquals("MainTabs", multiScopeRegistry.getScopeKey(MainTabs.HomeTab))
        assertEquals("MainTabs", multiScopeRegistry.getScopeKey(MainTabs.SettingsTab))
        assertEquals("ProfileTabs", multiScopeRegistry.getScopeKey(ProfileTabs.OverviewTab))
        assertEquals("ProfileTabs", multiScopeRegistry.getScopeKey(ProfileTabs.HistoryTab))
    }

    @Test
    fun `Multiple scopes - destinations are not in wrong scope`() {
        // MainTabs destinations should not be in ProfileTabs scope
        assertFalse(multiScopeRegistry.isInScope("ProfileTabs", MainTabs.HomeTab))
        assertFalse(multiScopeRegistry.isInScope("ProfileTabs", MainTabs.SettingsTab))

        // ProfileTabs destinations should not be in MainTabs scope
        assertFalse(multiScopeRegistry.isInScope("MainTabs", ProfileTabs.OverviewTab))
        assertFalse(multiScopeRegistry.isInScope("MainTabs", ProfileTabs.HistoryTab))
    }

    @Test
    fun `Multiple scopes - destinations are in correct scope`() {
        assertTrue(multiScopeRegistry.isInScope("MainTabs", MainTabs.HomeTab))
        assertTrue(multiScopeRegistry.isInScope("MainTabs", MainTabs.SettingsTab))
        assertTrue(multiScopeRegistry.isInScope("ProfileTabs", ProfileTabs.OverviewTab))
        assertTrue(multiScopeRegistry.isInScope("ProfileTabs", ProfileTabs.HistoryTab))
    }

    @Test
    fun `Multiple scopes - out-of-scope destination returns null for getScopeKey`() {
        assertNull(multiScopeRegistry.getScopeKey(OutOfScopeDestination))
    }

    // =========================================================================
    // STACK SCOPE TESTS
    // =========================================================================

    /**
     * Simulates a sealed interface for stack destinations (auth flow).
     * In real apps, this would be generated from @Stack annotations.
     */
    private sealed interface AuthFlow : NavDestination {
        data object Login : AuthFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object Register : AuthFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object ForgotPassword : AuthFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }
    }

    /**
     * Another stack scope for testing.
     */
    private sealed interface OnboardingFlow : NavDestination {
        data object Welcome : OnboardingFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }

        data object Tutorial : OnboardingFlow {
            override val data: Any? = null
            override val transition: NavigationTransition? = null
        }
    }

    /**
     * Registry that includes stack scopes alongside tab scopes.
     * Simulates what KSP would generate from @Tab and @Stack annotations.
     */
    private val stackScopeRegistry = object : ScopeRegistry {
        private val scopes = mapOf(
            // Tab scopes
            "MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.SettingsTab::class),
            // Stack scopes
            "AuthFlow" to setOf(
                AuthFlow.Login::class,
                AuthFlow.Register::class,
                AuthFlow.ForgotPassword::class
            ),
            "OnboardingFlow" to setOf(OnboardingFlow.Welcome::class, OnboardingFlow.Tutorial::class)
        )

        override fun isInScope(scopeKey: String, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): String? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key
        }
    }

    @Test
    fun `registry includes stack destinations`() {
        // Verify stack destinations are in scope map
        assertTrue(stackScopeRegistry.isInScope("AuthFlow", AuthFlow.Login))
        assertTrue(stackScopeRegistry.isInScope("AuthFlow", AuthFlow.Register))
        assertTrue(stackScopeRegistry.isInScope("AuthFlow", AuthFlow.ForgotPassword))

        assertTrue(stackScopeRegistry.isInScope("OnboardingFlow", OnboardingFlow.Welcome))
        assertTrue(stackScopeRegistry.isInScope("OnboardingFlow", OnboardingFlow.Tutorial))
    }

    @Test
    fun `getScopeKey returns stack scope for stack destinations`() {
        // Verify scope key lookup works for stack destinations
        assertEquals("AuthFlow", stackScopeRegistry.getScopeKey(AuthFlow.Login))
        assertEquals("AuthFlow", stackScopeRegistry.getScopeKey(AuthFlow.Register))
        assertEquals("AuthFlow", stackScopeRegistry.getScopeKey(AuthFlow.ForgotPassword))

        assertEquals("OnboardingFlow", stackScopeRegistry.getScopeKey(OnboardingFlow.Welcome))
        assertEquals("OnboardingFlow", stackScopeRegistry.getScopeKey(OnboardingFlow.Tutorial))
    }

    @Test
    fun `stack destinations are not in wrong stack scope`() {
        // AuthFlow destinations should not be in OnboardingFlow scope
        assertFalse(stackScopeRegistry.isInScope("OnboardingFlow", AuthFlow.Login))
        assertFalse(stackScopeRegistry.isInScope("OnboardingFlow", AuthFlow.Register))

        // OnboardingFlow destinations should not be in AuthFlow scope
        assertFalse(stackScopeRegistry.isInScope("AuthFlow", OnboardingFlow.Welcome))
        assertFalse(stackScopeRegistry.isInScope("AuthFlow", OnboardingFlow.Tutorial))
    }

    @Test
    fun `stack destinations are not in tab scope`() {
        // Stack destinations should not be in tab scopes
        assertFalse(stackScopeRegistry.isInScope("MainTabs", AuthFlow.Login))
        assertFalse(stackScopeRegistry.isInScope("MainTabs", OnboardingFlow.Welcome))
    }

    @Test
    fun `tab destinations are not in stack scope`() {
        // Tab destinations should not be in stack scopes
        assertFalse(stackScopeRegistry.isInScope("AuthFlow", MainTabs.HomeTab))
        assertFalse(stackScopeRegistry.isInScope("OnboardingFlow", MainTabs.SettingsTab))
    }

    @Test
    fun `out-of-scope destination returns null for getScopeKey in mixed registry`() {
        assertNull(stackScopeRegistry.getScopeKey(OutOfScopeDestination))
    }

    @Test
    fun `unknown scope key allows all destinations including stack destinations`() {
        // Unknown scope keys should still allow all (defensive behavior)
        assertTrue(stackScopeRegistry.isInScope("UnknownScope", AuthFlow.Login))
        assertTrue(stackScopeRegistry.isInScope("UnknownScope", OnboardingFlow.Welcome))
        assertTrue(stackScopeRegistry.isInScope("UnknownScope", MainTabs.HomeTab))
        assertTrue(stackScopeRegistry.isInScope("UnknownScope", OutOfScopeDestination))
    }
}
