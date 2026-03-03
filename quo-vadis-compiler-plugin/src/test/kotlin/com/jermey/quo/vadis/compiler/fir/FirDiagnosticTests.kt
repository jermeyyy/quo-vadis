package com.jermey.quo.vadis.compiler.fir

import com.jermey.quo.vadis.compiler.testing.CompilerTestHelper
import com.jermey.quo.vadis.compiler.testing.TestSources
import com.jermey.quo.vadis.compiler.testing.assertHasError
import com.jermey.quo.vadis.compiler.testing.assertHasWarning
import com.jermey.quo.vadis.compiler.testing.assertNoDiagnostics
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
class FirDiagnosticTests {

    // ── 5B.1: Route Collision Tests ──────────────────────────────────────

    @Test
    fun `duplicate routes produce error`() {
        val result = CompilerTestHelper.compile(
            TestSources.duplicateRouteStack,
            expectSuccess = false,
        )
        result.assertHasError("Duplicate route")
    }

    @Test
    fun `different routes produce no error`() {
        val result = CompilerTestHelper.compile(TestSources.basicStack)
        result.assertNoDiagnostics()
    }

    // ── 5B.2: Argument Parity Tests ─────────────────────────────────────

    @Test
    fun `missing route argument produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.missingRouteArgument,
            expectSuccess = false,
        )
        result.assertHasError("has no matching @Argument")
    }

    @Test
    fun `extra argument produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.extraArgument,
            expectSuccess = false,
        )
        result.assertHasError("does not match any placeholder")
    }

    @Test
    fun `matching arguments produce no error`() {
        val result = CompilerTestHelper.compile(TestSources.basicStack)
        result.assertNoDiagnostics()
    }

    @Test
    fun `multiple argument types match correctly`() {
        val result = CompilerTestHelper.compile(TestSources.stackWithMultipleArgs)
        result.assertNoDiagnostics()
    }

    // ── 5B.3: Container Role Validation ─────────────────────────────────

    @Test
    fun `missing primary pane produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.missingPrimaryPane,
            expectSuccess = false,
        )
        result.assertHasError("must have exactly one @PaneItem")
    }

    @Test
    fun `valid pane with roles produces no error`() {
        val result = CompilerTestHelper.compile(TestSources.paneWithRoles)
        result.assertNoDiagnostics()
    }

    @Test
    fun `valid tabs with items produce no error`() {
        val result = CompilerTestHelper.compile(TestSources.tabsWithItems)
        result.assertNoDiagnostics()
    }

    // ── 5B.4: Transition Compatibility Tests ────────────────────────────

    @Test
    fun `valid transition produces no error`() {
        val result = CompilerTestHelper.compile(TestSources.destinationWithTransition)
        result.assertNoDiagnostics()
    }

    // ── 5B.5: Orphan Screen Tests ───────────────────────────────────────

    @Test
    fun `orphan screen produces warning`() {
        val result = CompilerTestHelper.compile(TestSources.orphanScreen)
        result.assertHasWarning("has no @Destination annotation")
    }

    @Test
    fun `valid screen binding produces no warning`() {
        val result = CompilerTestHelper.compile(TestSources.validScreenBinding)
        result.assertNoDiagnostics()
    }

    // ── 5B.6: Structural Validation Tests ───────────────────────────────

    @Test
    fun `stack not sealed produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.stackNotSealed,
            expectSuccess = false,
        )
        result.assertHasError("@Stack must be applied to a sealed")
    }

    @Test
    fun `destination not in stack produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.destinationNotInStack,
            expectSuccess = false,
        )
        result.assertHasError("@Destination must be a direct subclass")
    }

    @Test
    fun `full navigation graph compiles cleanly`() {
        val result = CompilerTestHelper.compile(TestSources.fullNavigationGraph)
        result.assertNoDiagnostics()
    }
}
