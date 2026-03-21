package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemInfo
import com.jermey.quo.vadis.ksp.models.TabItemType
import com.jermey.quo.vadis.ksp.testutil.FakeKSClassDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSPLogger
import com.jermey.quo.vadis.ksp.testutil.fakeResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Tests for [ValidationEngine] ordinal validation methods:
 * - validateOrdinalZeroExists
 * - validateOrdinalCollisions
 * - validateOrdinalContinuity
 */
class ValidationEngineOrdinalTest : FunSpec({

    val logger = FakeKSPLogger()
    val engine = ValidationEngine(logger)
    val resolver = fakeResolver()

    // -- Helpers --

    fun sealedClassDecl(
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

    fun tabItemDecl(
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

    fun tabInfo(
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

    fun destinationTabItem(name: String, ordinal: Int) = TabItemInfo(
        classDeclaration = tabItemDecl(name),
        tabType = TabItemType.DESTINATION,
        ordinal = ordinal,
    )

    fun validate(tabs: List<TabInfo>): Boolean {
        logger.errors.clear()
        logger.warnings.clear()
        return engine.validate(
            stacks = emptyList(),
            tabs = tabs,
            panes = emptyList(),
            screens = emptyList(),
            allDestinations = emptyList(),
        )
    }

    // =========================================================================
    // validateOrdinalZeroExists
    // =========================================================================

    test("ordinal 0 missing produces error") {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 1),
            destinationTabItem("TabB", ordinal = 2),
        ))

        val result = validate(listOf(tab))

        result.shouldBeFalse()
        logger.errors.any { it.contains("no @TabItem with ordinal = 0") }.shouldBeTrue()
    }

    test("ordinal 0 present passes") {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 1),
        ))

        validate(listOf(tab))

        logger.errors.filter { it.contains("no @TabItem with ordinal = 0") }.shouldBeEmpty()
    }

    // =========================================================================
    // validateOrdinalCollisions
    // =========================================================================

    test("duplicate ordinals produce error") {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 0),
        ))

        val result = validate(listOf(tab))

        result.shouldBeFalse()
        logger.errors.any { it.contains("Duplicate ordinal") }.shouldBeTrue()
    }

    test("unique ordinals pass") {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 1),
            destinationTabItem("TabC", ordinal = 2),
        ))

        validate(listOf(tab))

        logger.errors.filter { it.contains("Duplicate ordinal") }.shouldBeEmpty()
    }

    // =========================================================================
    // validateOrdinalContinuity
    // =========================================================================

    test("ordinal gap (0, 2) produces error") {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 2),
        ))

        val result = validate(listOf(tab))

        result.shouldBeFalse()
        logger.errors.any { it.contains("ordinal gaps") }.shouldBeTrue()
    }

    test("consecutive ordinals (0, 1, 2) pass") {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
            destinationTabItem("TabB", ordinal = 1),
            destinationTabItem("TabC", ordinal = 2),
        ))

        validate(listOf(tab))

        logger.errors.filter { it.contains("ordinal gaps") }.shouldBeEmpty()
    }

    test("single tab item with ordinal 0 passes") {
        val tab = tabInfo(items = listOf(
            destinationTabItem("TabA", ordinal = 0),
        ))

        validate(listOf(tab))

        logger.errors.filter {
            it.contains("ordinal") && !it.contains("container")
        }.shouldBeEmpty()
    }
})
