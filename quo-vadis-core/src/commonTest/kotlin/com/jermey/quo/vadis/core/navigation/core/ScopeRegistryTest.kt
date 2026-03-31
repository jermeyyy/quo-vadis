package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

/**
 * Creates a test [ScopeRegistry] backed by the given scope-to-classes mapping.
 * Simulates what KSP would generate from sealed class hierarchies.
 */
private fun createTestScopeRegistry(
    scopes: Map<String, Set<KClass<out NavDestination>>>
): ScopeRegistry = object : ScopeRegistry {
    override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
        val scopeClasses = scopes[scopeKey.value] ?: return false
        return scopeClasses.any { it.isInstance(destination) }
    }

    override fun getScopeKey(destination: NavDestination): ScopeKey? {
        return scopes.entries.find { (_, classes) ->
            classes.any { it.isInstance(destination) }
        }?.key?.let { ScopeKey(it) }
    }
}

/**
 * Tests for [com.jermey.quo.vadis.core.registry.ScopeRegistry] interface and implementations.
 */
class ScopeRegistryTest : FunSpec() {

    sealed interface MainTabs : NavDestination {
        data object HomeTab : MainTabs {
            override val transition: NavigationTransition? = null
        }

        data object SettingsTab : MainTabs {
            override val transition: NavigationTransition? = null
        }
    }

    data object OutOfScopeDestination : NavDestination {
        override val transition: NavigationTransition? = null
    }

    sealed interface ProfileTabs : NavDestination {
        data object OverviewTab : ProfileTabs {
            override val transition: NavigationTransition? = null
        }

        data object HistoryTab : ProfileTabs {
            override val transition: NavigationTransition? = null
        }
    }

    sealed interface AuthFlow : NavDestination {
        data object Login : AuthFlow {
            override val transition: NavigationTransition? = null
        }

        data object Register : AuthFlow {
            override val transition: NavigationTransition? = null
        }

        data object ForgotPassword : AuthFlow {
            override val transition: NavigationTransition? = null
        }
    }

    sealed interface OnboardingFlow : NavDestination {
        data object Welcome : OnboardingFlow {
            override val transition: NavigationTransition? = null
        }

        data object Tutorial : OnboardingFlow {
            override val transition: NavigationTransition? = null
        }
    }

    init {

    // =========================================================================
    // TEST REGISTRY IMPLEMENTATION
    // =========================================================================

    /**
     * Test implementation of ScopeRegistry.
     * Simulates what KSP would generate from sealed class hierarchies.
     */
    val testRegistry = createTestScopeRegistry(
        mapOf("MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.SettingsTab::class))
    )

    // =========================================================================
    // EMPTY REGISTRY TESTS
    // =========================================================================

    test("Empty registry always returns true for isInScope") {
        (ScopeRegistry.Empty.isInScope(ScopeKey("AnyScope"), MainTabs.HomeTab)).shouldBeTrue()
        (ScopeRegistry.Empty.isInScope(ScopeKey("AnyScope"), OutOfScopeDestination)).shouldBeTrue()
    }

    test("Empty registry always returns null for getScopeKey") {
        ScopeRegistry.Empty.getScopeKey(MainTabs.HomeTab).shouldBeNull()
        ScopeRegistry.Empty.getScopeKey(OutOfScopeDestination).shouldBeNull()
    }

    test("Empty registry allows any destination in any scope") {
        // This is the backward-compatible behavior
        (ScopeRegistry.Empty.isInScope(ScopeKey("MainTabs"), OutOfScopeDestination)).shouldBeTrue()
        (ScopeRegistry.Empty.isInScope(ScopeKey("NonExistent"), MainTabs.HomeTab)).shouldBeTrue()
        (ScopeRegistry.Empty.isInScope(ScopeKey(""), MainTabs.SettingsTab)).shouldBeTrue()
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS - isInScope
    // =========================================================================

    test("isInScope returns true for destinations in scope") {
        (testRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.HomeTab)).shouldBeTrue()
        (testRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.SettingsTab)).shouldBeTrue()
    }

    test("isInScope returns false for destinations out of scope") {
        (testRegistry.isInScope(ScopeKey("MainTabs"), OutOfScopeDestination)).shouldBeFalse()
    }

    test("isInScope returns false for unknown scope keys") {
        // Unknown scope keys reject all destinations (matches production DslScopeRegistry)
        (testRegistry.isInScope(ScopeKey("UnknownScope"), MainTabs.HomeTab)).shouldBeFalse()
        (testRegistry.isInScope(ScopeKey("UnknownScope"), OutOfScopeDestination)).shouldBeFalse()
    }

    test("isInScope handles empty scope key") {
        // Empty scope key is treated as unknown (rejects all, matches production)
        (testRegistry.isInScope(ScopeKey(""), MainTabs.HomeTab)).shouldBeFalse()
        (testRegistry.isInScope(ScopeKey(""), OutOfScopeDestination)).shouldBeFalse()
    }

    // =========================================================================
    // CUSTOM REGISTRY TESTS - getScopeKey
    // =========================================================================

    test("getScopeKey returns correct scope for in-scope destinations") {
        testRegistry.getScopeKey(MainTabs.HomeTab) shouldBe ScopeKey("MainTabs")
        testRegistry.getScopeKey(MainTabs.SettingsTab) shouldBe ScopeKey("MainTabs")
    }

    test("getScopeKey returns null for out-of-scope destinations") {
        testRegistry.getScopeKey(OutOfScopeDestination).shouldBeNull()
    }

    // =========================================================================
    // MULTIPLE SCOPES TESTS
    // =========================================================================

    /**
     * Registry with multiple scopes.
     */
    val multiScopeRegistry = createTestScopeRegistry(
        mapOf(
            "MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.SettingsTab::class),
            "ProfileTabs" to setOf(ProfileTabs.OverviewTab::class, ProfileTabs.HistoryTab::class)
        )
    )

    test("Multiple scopes - each destination maps to correct scope") {
        multiScopeRegistry.getScopeKey(MainTabs.HomeTab) shouldBe ScopeKey("MainTabs")
        multiScopeRegistry.getScopeKey(MainTabs.SettingsTab) shouldBe ScopeKey("MainTabs")
        multiScopeRegistry.getScopeKey(ProfileTabs.OverviewTab) shouldBe ScopeKey("ProfileTabs")
        multiScopeRegistry.getScopeKey(ProfileTabs.HistoryTab) shouldBe ScopeKey("ProfileTabs")
    }

    test("Multiple scopes - destinations are not in wrong scope") {
        // MainTabs destinations should not be in ProfileTabs scope
        (multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), MainTabs.HomeTab)).shouldBeFalse()
        (multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), MainTabs.SettingsTab)).shouldBeFalse()

        // ProfileTabs destinations should not be in MainTabs scope
        (multiScopeRegistry.isInScope(ScopeKey("MainTabs"), ProfileTabs.OverviewTab)).shouldBeFalse()
        (multiScopeRegistry.isInScope(ScopeKey("MainTabs"), ProfileTabs.HistoryTab)).shouldBeFalse()
    }

    test("Multiple scopes - destinations are in correct scope") {
        (multiScopeRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.HomeTab)).shouldBeTrue()
        (multiScopeRegistry.isInScope(ScopeKey("MainTabs"), MainTabs.SettingsTab)).shouldBeTrue()
        (multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), ProfileTabs.OverviewTab)).shouldBeTrue()
        (multiScopeRegistry.isInScope(ScopeKey("ProfileTabs"), ProfileTabs.HistoryTab)).shouldBeTrue()
    }

    test("Multiple scopes - out-of-scope destination returns null for getScopeKey") {
        multiScopeRegistry.getScopeKey(OutOfScopeDestination).shouldBeNull()
    }

    // =========================================================================
    // STACK SCOPE TESTS
    // =========================================================================

    /**
     * Registry that includes stack scopes alongside tab scopes.
     * Simulates what KSP would generate from @Tab and @Stack annotations.
     */
    val stackScopeRegistry = createTestScopeRegistry(
        mapOf(
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
    )

    test("registry includes stack destinations") {
        // Verify stack destinations are in scope map
        (stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), AuthFlow.Login)).shouldBeTrue()
        (stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), AuthFlow.Register)).shouldBeTrue()
        (stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), AuthFlow.ForgotPassword)).shouldBeTrue()

        (stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), OnboardingFlow.Welcome)).shouldBeTrue()
        (stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), OnboardingFlow.Tutorial)).shouldBeTrue()
    }

    test("getScopeKey returns stack scope for stack destinations") {
        // Verify scope key lookup works for stack destinations
        stackScopeRegistry.getScopeKey(AuthFlow.Login) shouldBe ScopeKey("AuthFlow")
        stackScopeRegistry.getScopeKey(AuthFlow.Register) shouldBe ScopeKey("AuthFlow")
        stackScopeRegistry.getScopeKey(AuthFlow.ForgotPassword) shouldBe ScopeKey("AuthFlow")

        stackScopeRegistry.getScopeKey(OnboardingFlow.Welcome) shouldBe ScopeKey("OnboardingFlow")
        stackScopeRegistry.getScopeKey(OnboardingFlow.Tutorial) shouldBe ScopeKey("OnboardingFlow")
    }

    test("stack destinations are not in wrong stack scope") {
        // AuthFlow destinations should not be in OnboardingFlow scope
        (stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), AuthFlow.Login)).shouldBeFalse()
        (stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), AuthFlow.Register)).shouldBeFalse()

        // OnboardingFlow destinations should not be in AuthFlow scope
        (stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), OnboardingFlow.Welcome)).shouldBeFalse()
        (stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), OnboardingFlow.Tutorial)).shouldBeFalse()
    }

    test("stack destinations are not in tab scope") {
        // Stack destinations should not be in tab scopes
        (stackScopeRegistry.isInScope(ScopeKey("MainTabs"), AuthFlow.Login)).shouldBeFalse()
        (stackScopeRegistry.isInScope(ScopeKey("MainTabs"), OnboardingFlow.Welcome)).shouldBeFalse()
    }

    test("tab destinations are not in stack scope") {
        // Tab destinations should not be in stack scopes
        (stackScopeRegistry.isInScope(ScopeKey("AuthFlow"), MainTabs.HomeTab)).shouldBeFalse()
        (stackScopeRegistry.isInScope(ScopeKey("OnboardingFlow"), MainTabs.SettingsTab)).shouldBeFalse()
    }

    test("out-of-scope destination returns null for getScopeKey in mixed registry") {
        stackScopeRegistry.getScopeKey(OutOfScopeDestination).shouldBeNull()
    }

    test("unknown scope key rejects all destinations including stack destinations") {
        // Unknown scope keys reject all destinations (matches production DslScopeRegistry)
        (stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), AuthFlow.Login)).shouldBeFalse()
        (stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), OnboardingFlow.Welcome)).shouldBeFalse()
        (stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), MainTabs.HomeTab)).shouldBeFalse()
        (stackScopeRegistry.isInScope(ScopeKey("UnknownScope"), OutOfScopeDestination)).shouldBeFalse()
    }

    } // init
}
