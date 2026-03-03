package com.jermey.quo.vadis.compiler.ir

import com.jermey.quo.vadis.compiler.testing.GoldenFileComparator
import com.jermey.quo.vadis.compiler.testing.IrDumpHelper
import com.jermey.quo.vadis.compiler.testing.TestSources
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Test

/**
 * IR structural verification and regression tests (Sub-phase 5D).
 *
 * These tests ensure:
 * - `-Xverify-ir` passes for all sources (structural integrity)
 * - IR output is deterministic (golden file comparison)
 */
@OptIn(ExperimentalCompilerApi::class)
class IrVerificationTests {

    // ── 5D.1: -Xverify-ir passes for all sources ──────────────────

    @Test
    fun `basic stack passes IR verification`() {
        // -Xverify-ir is enabled in CompilerTestHelper, so just compiling verifies
        IrDumpHelper.compileWithIrDump(TestSources.basicStack)
    }

    @Test
    fun `tabs source passes IR verification`() {
        IrDumpHelper.compileWithIrDump(TestSources.tabsWithItems)
    }

    @Test
    fun `pane source passes IR verification`() {
        IrDumpHelper.compileWithIrDump(TestSources.paneWithRoles)
    }

    @Test
    fun `full navigation graph passes IR verification`() {
        IrDumpHelper.compileWithIrDump(TestSources.fullNavigationGraph)
    }

    @Test
    fun `all argument types pass IR verification`() {
        IrDumpHelper.compileWithIrDump(TestSources.deepLinkArgumentTypes)
    }

    @Test
    fun `transitions pass IR verification`() {
        IrDumpHelper.compileWithIrDump(TestSources.destinationWithTransition)
    }

    @Test
    fun `screen bindings pass IR verification`() {
        IrDumpHelper.compileWithIrDump(TestSources.validScreenBinding)
    }

    // ── 5D.3: Golden file regression tests ─────────────────────────

    @Test
    fun `basic stack IR output matches golden file`() {
        val result = IrDumpHelper.compileWithIrDump(TestSources.basicStack)
        GoldenFileComparator.assertMatchesGolden("basic-stack", result.irOutput)
    }

    @Test
    fun `tabs IR output matches golden file`() {
        val result = IrDumpHelper.compileWithIrDump(TestSources.tabsWithItems)
        GoldenFileComparator.assertMatchesGolden("tabs-with-items", result.irOutput)
    }

    @Test
    fun `full graph IR output matches golden file`() {
        val result = IrDumpHelper.compileWithIrDump(TestSources.fullNavigationGraph)
        GoldenFileComparator.assertMatchesGolden("full-navigation-graph", result.irOutput)
    }
}
