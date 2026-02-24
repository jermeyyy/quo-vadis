package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
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

        override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey.value] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): ScopeKey? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key?.let { ScopeKey(it) }
        }
    }

    // =========================================================================
    // EMPTY REGISTRY TESTS
    // =========================================================================

    @Test
    fun `Empty registry always returns true for isInScope`() {
        assertTrue(ScopeRegistry.Empty.isInScope(ScopeKey("AnyScope"), MainTabs.HomeTab))
        assertTrue(ScopeRegistry.Empty.isInScope(ScopeKey("AnyScope"), OutOfScopeDestination))
    }

    @Test
    fun `Empty registry always returns null for getScopeKey`() {
        assertNull(ScopeRegistry.Empty.getScopeKey(MainTabs.HomeTab))
        assertNull(ScopeRegistry.Empty.getScopeKey(OutOfScopeDestination))
    }

    @Test
    fun `Empty registry allows any destination in any scope`() {
        // This is the backward-compatible behavior
        assertTrue(ScopeRegistry.Empty.isInScope(ScopeKey("MainTabs"), OutOfScopeDestination))
        assertTrue(ScopeRegistry.Empty.isInScope(ScopeKey("NonExistent"), MainTabs.HomeTab))
        assertTrue(ScopeRegistry.Empty.isInScope(ScopeKey(""), MainTabs.SettingsTab))
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS - isInScope
    // =========================================================================

    @Test
    fun `isInScope returns true for destinations in scope`() {
        assertTrue(testRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.HomeTab))
        assertTrue(testRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.SettingsTab))
    }

    @Test
    fun `isInScope returns false for destinations out of scope`() {
        assertFalse(testRegistry.isInScope(ScopeKey("MainTabs"), OutOfScopeDestination))
    }

    @Test
    fun `isInScope returns true for unknown scope keys`() {
        // Unknown scope keys allow all destinations (defensive behavior)
        // This prevents crashes when scope configuration is incomplete
        assertTrue(testRegistry.isInScope(ScopeKey("UnknownScope"), MainTabs.HomeTab))
        assertTrue(testRegistry.isInScope(ScopeKey("UnknownScope"), OutOfScopeDestination))
    }

    @Test
    fun `isInScope handles empty scope key`() {
        // Empty scope key is treated as unknown (allows all)
        assertTrue(testRegistry.isInScope(ScopeKey(""), MainTabs.HomeTab))
        assertTrue(testRegistry.isInScope(ScopeKey(""), OutOfScopeDestination))
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS - getScopeKey
    // =========================================================================

    @Test
    fun `getScopeKey returns correct scope for in-scope destinations`() {
        assertEquals(ScopeKey("MainTabs"), testRegistry.getScopeKey(MainTabs.HomeTab))
        assertEquals(ScopeKey("MainTabs"), testRegistry.getScopeKey(MainTabs.SettingsTab))
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

        override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey.value] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): ScopeKey? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key?.let { ScopeKey(it) }
        }
    }

    @Test
    fun `Multiple scopes - each destination maps to correct scope`() {
        assertEquals(ScopeKey("MainTabs"), multiScopeRegistry.getScopeKey(MainTabs.HomeTab))
        assertEquals(ScopeKey("MainTabs"), multiScopeRegistry.getScopeKey(MainTabs.SettingsTab))
        assertEquals(ScopeKey("ProfileTabs"), multiScopeRegistry.getScopeKey(ProfileTabs.OverviewTab))
        assertEquals(ScopeKey("ProfileTabs"), multiScopeRegistry.getScopeKey(ProfileTabs.HistoryTab))
    }

    @Test
    fun `Multiple scopes - destinations are not in wrong scope`() {
        // MainTabs destinations should not be in ProfileTabs scope
        assertFalse(multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), MainTabs.HomeTab))
        assertFalse(multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), MainTabs.SettingsTab))

        // ProfileTabs destinations should not be in MainTabs scope
        assertFalse(multiScopeRegistry.isInScope(ScopeKey("MainTabs"), ProfileTabs.OverviewTab))
        assertFalse(multiScopeRegistry.isInScope(ScopeKey("MainTabs"), ProfileTabs.HistoryTab))
    }

    @Test
    fun `Multiple scopes - destinations are in correct scope`() {
        assertTrue(multiScopeRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.HomeTab))
        assertTrue(multiScopeRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.SettingsTab))
        assertTrue(multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), ProfileTabs.OverviewTab))
        assertTrue(multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), ProfileTabs.HistoryTab))
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

        override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
            val scopeClasses = scopes[scopeKey.value] ?: return true
            return scopeClasses.any { it.isInstance(destination) }
        }

        override fun getScopeKey(destination: NavDestination): ScopeKey? {
            return scopes.entries.find { (_, classes) ->
                classes.any { it.isInstance(destination) }
            }?.key?.let { ScopeKey(it) }
        }
    }

    @Test
    fun `registry includes stack destinations`() {
        // Verify stack destinations are in scope map
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), AuthFlow.Login))
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), AuthFlow.Register))
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), AuthFlow.ForgotPassword))

        assertTrue(stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), OnboardingFlow.Welcome))
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), OnboardingFlow.Tutorial))
    }

    @Test
    fun `getScopeKey returns stack scope for stack destinations`() {
        // Verify scope key lookup works for stack destinations
        assertEquals(ScopeKey("AuthFlow"), stackScopeRegistry.getScopeKey(AuthFlow.Login))
        assertEquals(ScopeKey("AuthFlow"), stackScopeRegistry.getScopeKey(AuthFlow.Register))
        assertEquals(ScopeKey("AuthFlow"), stackScopeRegistry.getScopeKey(AuthFlow.ForgotPassword))

        assertEquals(ScopeKey("OnboardingFlow"), stackScopeRegistry.getScopeKey(OnboardingFlow.Welcome))
        assertEquals(ScopeKey("OnboardingFlow"), stackScopeRegistry.getScopeKey(OnboardingFlow.Tutorial))
    }

    @Test
    fun `stack destinations are not in wrong stack scope`() {
        // AuthFlow destinations should not be in OnboardingFlow scope
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), AuthFlow.Login))
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), AuthFlow.Register))

        // OnboardingFlow destinations should not be in AuthFlow scope
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), OnboardingFlow.Welcome))
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), OnboardingFlow.Tutorial))
    }

    @Test
    fun `stack destinations are not in tab scope`() {
        // Stack destinations should not be in tab scopes
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("MainTabs"), AuthFlow.Login))
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("MainTabs"), OnboardingFlow.Welcome))
    }

    @Test
    fun `tab destinations are not in stack scope`() {
        // Tab destinations should not be in stack scopes
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), MainTabs.HomeTab))
        assertFalse(stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), MainTabs.SettingsTab))
    }

    @Test
    fun `out-of-scope destination returns null for getScopeKey in mixed registry`() {
        assertNull(stackScopeRegistry.getScopeKey(OutOfScopeDestination))
    }

    @Test
    fun `unknown scope key allows all destinations including stack destinations`() {
        // Unknown scope keys should still allow all (defensive behavior)
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), AuthFlow.Login))
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), OnboardingFlow.Welcome))
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), MainTabs.HomeTab))
        assertTrue(stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), OutOfScopeDestination))
    }
}
