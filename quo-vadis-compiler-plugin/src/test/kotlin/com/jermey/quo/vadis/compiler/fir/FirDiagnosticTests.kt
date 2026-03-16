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
    fun `tabs without wrapper compile successfully for cross-module support`() {
        // Cross-module tabs may define @Tabs in one module and @TabsContainer in another.
        // The compiler plugin no longer errors on missing wrappers within a single module.
        val result = CompilerTestHelper.compile(TestSources.tabsWithItems)
        result.assertNoDiagnostics()
    }

    @Test
    fun `valid tabs with items and wrapper produce no error`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.tabsContainerWrapper,
        )
        result.assertNoDiagnostics()
    }

    @Test
    fun `tabs container with missing target compiles for cross-module support`() {
        // Cross-module tabs may define @Tabs in a dependency module and @TabsContainer locally.
        // The compiler plugin no longer errors on missing @Tabs within a single module.
        val result = CompilerTestHelper.compile(
            TestSources.basicStack,
            TestSources.missingTabsContainerTarget,
        )
        result.assertNoDiagnostics()
    }

    @Test
    fun `pane container with missing target produces actionable error`() {
        val result = CompilerTestHelper.compile(
            TestSources.basicStack,
            TestSources.missingPaneContainerTarget,
            expectSuccess = false,
        )

        result.assertHasError("@PaneContainer wrapper test.MissingPaneWrapper")
        result.assertHasError("targets test.MissingCatalogPane")
        result.assertHasError("no matching @Pane container was collected")
    }

    @Test
    fun `duplicate tabs wrappers collapsing to same normalized key produce error`() {
        val result = CompilerTestHelper.compile(
            TestSources.duplicateNormalizedTabsContainerWrappers,
            expectSuccess = false,
        )

        result.assertHasError("multiple @TabsContainer wrappers resolved to the same wrapper key")
        result.assertHasError("mainTabs")
        result.assertHasError("test.MainTabsWrapper")
        result.assertHasError("test.MainTabsCompanionWrapper")
    }

    @Test
    fun `duplicate pane wrappers collapsing to same normalized key produce error`() {
        val result = CompilerTestHelper.compile(
            TestSources.duplicateNormalizedPaneContainerWrappers,
            expectSuccess = false,
        )

        result.assertHasError("multiple @PaneContainer wrappers resolved to the same wrapper key")
        result.assertHasError("catalog")
        result.assertHasError("test.CatalogPaneWrapper")
        result.assertHasError("test.CatalogPaneCompanionWrapper")
    }

    @Test
    fun `tabs container wrapper without composable annotation produces actionable error`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.nonComposableTabsContainerWrapper,
            expectSuccess = false,
        )

        result.assertHasError("@TabsContainer wrapper test.InvalidTabsWrapper")
        result.assertHasError("must be annotated with @Composable")
    }

    @Test
    fun `tabs container wrapper with compose synthetic params is accepted`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.composeSyntheticTabsContainerWrapper,
        )

        result.assertNoDiagnostics()
    }

    @Test
    fun `tabs container wrapper with extra required user parameter produces actionable error`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabsWithItems,
            TestSources.invalidTabsContainerRequiredUserParameterWrapper,
            expectSuccess = false,
        )

        result.assertHasError("@TabsContainer wrapper test.InvalidMainTabsWrapper")
        result.assertHasError("unsupported required parameter(s): title")
    }

    @Test
    fun `pane container wrapper with wrong scope parameter produces actionable error`() {
        val result = CompilerTestHelper.compile(
            TestSources.paneWithRoles,
            TestSources.invalidPaneContainerScopeWrapper,
            expectSuccess = false,
        )

        result.assertHasError("@PaneContainer wrapper test.InvalidCatalogPaneWrapper")
        result.assertHasError("must declare parameter 'scope: PaneContainerScope'")
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

    // ── 5B.7: TabItem Ordinal Validation Tests ──────────────────────────

    @Test
    fun `duplicate ordinal produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabItemDuplicateOrdinal,
            expectSuccess = false,
        )
        result.assertHasError("duplicate ordinal")
    }

    @Test
    fun `missing ordinal zero produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabItemMissingOrdinalZero,
            expectSuccess = false,
        )
        result.assertHasError("no @TabItem with ordinal = 0")
    }

    @Test
    fun `ordinal gap produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabItemOrdinalGap,
            expectSuccess = false,
        )
        result.assertHasError("non-contiguous ordinals")
    }

    @Test
    fun `invalid TabItem parent produces error`() {
        val result = CompilerTestHelper.compile(
            TestSources.tabItemInvalidParent,
            expectSuccess = false,
        )
        result.assertHasError("parent must reference a class annotated with @Tabs")
    }
}
