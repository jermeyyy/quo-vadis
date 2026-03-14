package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemInfo
import com.jermey.quo.vadis.ksp.models.TabItemType
import com.jermey.quo.vadis.ksp.testutil.FakeKSClassDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSPLogger
import com.jermey.quo.vadis.ksp.testutil.fakeResolver
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [ValidationEngine] ordinal validation methods:
 * - validateOrdinalZeroExists
 * - validateOrdinalCollisions
 * - validateOrdinalContinuity
 */
class ValidationEngineOrdinalTest {

    private val logger = FakeKSPLogger()
    private val engine = ValidationEngine(logger)
    private val resolver = fakeResolver()

    // -- Helpers --

    private fun sealedClassDecl(
        name: String,
        pkg: String = "com.example",
        classKind: ClassKind = ClassKind.CLASS,
        modifiers: Set<Modifier> = setOf(Modifier.SEALED),
        annotations: List<String> = emptyList(),
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = classKind,
        modifiers = modifiers,
        annotationNames = annotations,
    )

    private fun tabItemDecl(
        name: String,
        pkg: String = "com.example",
        classKind: ClassKind = ClassKind.OBJECT,
        modifiers: Set<Modifier> = setOf(Modifier.DATA),
        annotations: List<String> = listOf("Destination"),
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = classKind,
        modifiers = modifiers,
        annotationNames = annotations,
    )

    private fun tabInfo(
        name: String = "TestTabs",
        items: List<TabItemInfo>,
    ): TabInfo {
        val containerDecl = sealedClassDecl(name, annotations = listOf("Tabs"))
        return TabInfo(
            classDeclaration = containerDecl,
            name = name,
            className = name,
            packageName = "com.example",
            tabs = items,
        )
    }

    private fun destinationTabItem(name: String, ordinal: Int) = TabItemInfo(
        classDeclaration = tabItemDecl(name),
        tabType = TabItemType.DESTINATION,
        ordinal = ordinal,
    )

    private fun validate(tabs: List<TabInfo>): Boolean {
        logger.errors.clear()
        logger.warnings.clear()
        return engine.validate(
            stacks = emptyList(),
            tabs = tabs,
            panes = emptyList(),
            screens = emptyList(),
            allDestinations = emptyList(),
            resolver = resolver,
        )
    }

    // =========================================================================
    // validateOrdinalZeroExists
    // =========================================================================

    @Test
    fun `ordinal 0 missing produces error`() {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 1),
            destinationTabItem("TabB", ordinal = 2),
        ))

        val result = validate(listOf(tab))

        assertFalse(result, "Validation should fail when no tab has ordinal 0")
        assertTrue(
            logger.errors.any { it.contains("no @TabItem with ordinal = 0") },
            "Expected ordinal 0 missing error, got: ${logger.errors}"
        )
    }

    @Test
    fun `ordinal 0 present passes`() {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 1),
        ))

        val result = validate(listOf(tab))

        val ordinalZeroErrors = logger.errors.filter { it.contains("no @TabItem with ordinal = 0") }
        assertTrue(
            ordinalZeroErrors.isEmpty(),
            "Should not have ordinal 0 errors, got: $ordinalZeroErrors"
        )
    }

    // =========================================================================
    // validateOrdinalCollisions
    // =========================================================================

    @Test
    fun `duplicate ordinals produce error`() {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 0),
        ))

        val result = validate(listOf(tab))

        assertFalse(result, "Validation should fail for duplicate ordinals")
        assertTrue(
            logger.errors.any { it.contains("Duplicate ordinal") },
            "Expected duplicate ordinal error, got: ${logger.errors}"
        )
    }

    @Test
    fun `unique ordinals pass`() {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 1),
            destinationTabItem("TabC", ordinal = 2),
        ))

        val result = validate(listOf(tab))

        val duplicateErrors = logger.errors.filter { it.contains("Duplicate ordinal") }
        assertTrue(
            duplicateErrors.isEmpty(),
            "Should not have duplicate ordinal errors, got: $duplicateErrors"
        )
    }

    // =========================================================================
    // validateOrdinalContinuity
    // =========================================================================

    @Test
    fun `ordinal gap (0, 2) produces error`() {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 2),
        ))

        val result = validate(listOf(tab))

        assertFalse(result, "Validation should fail for ordinal gaps")
        assertTrue(
            logger.errors.any { it.contains("ordinal gaps") },
            "Expected ordinal gap error, got: ${logger.errors}"
        )
    }

    @Test
    fun `consecutive ordinals (0, 1, 2) pass`() {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 1),
            destinationTabItem("TabC", ordinal = 2),
        ))

        val result = validate(listOf(tab))

        val gapErrors = logger.errors.filter { it.contains("ordinal gaps") }
        assertTrue(
            gapErrors.isEmpty(),
            "Should not have ordinal gap errors, got: $gapErrors"
        )
    }

    @Test
    fun `single tab item with ordinal 0 passes`() {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
        ))

        val result = validate(listOf(tab))

        val ordinalErrors = logger.errors.filter {
            it.contains("ordinal") && !it.contains("container")
        }
        assertTrue(
            ordinalErrors.isEmpty(),
            "Single tab with ordinal 0 should produce no ordinal errors, got: $ordinalErrors"
        )
    }
}
