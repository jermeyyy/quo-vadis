package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.ParamInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.testutil.FakeKSClassDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSPLogger
import com.jermey.quo.vadis.ksp.testutil.fakeKSType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.string.shouldContain

/**
 * Tests for [ValidationEngine] @Tabs data class validation rules.
 *
 * Covers:
 * - Data class @Tabs with no constructor params → warning (use object)
 * - Data class @Tabs where no params have @Argument → error
 * - Data class @Tabs where some params have @Argument and some don't → error
 * - Data class @Tabs where all params have @Argument → valid
 * - Object @Tabs → no data class validations triggered
 */
class ValidationEngineTabsDataClassTest : FunSpec({

    val logger = FakeKSPLogger()
    val engine = ValidationEngine(logger)

    fun dataClassDecl(
        name: String,
        pkg: String = "com.example",
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = ClassKind.CLASS,
        modifiers = setOf(Modifier.DATA),
        annotationNames = listOf("Tabs"),
    )

    fun objectDecl(
        name: String,
        pkg: String = "com.example",
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = ClassKind.OBJECT,
        modifiers = emptySet(),
        annotationNames = listOf("Tabs"),
    )

    fun param(name: String, isArgument: Boolean = false) = ParamInfo(
        name = name,
        type = fakeKSType(),
        hasDefault = false,
        isArgument = isArgument,
        argumentKey = if (isArgument) name else "",
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
    // Data class @Tabs with no constructor params → warning
    // =========================================================================

    test("data class Tabs with no constructor params produces warning") {
        val tab = TabInfo(
            classDeclaration = dataClassDecl("EmptyDataTabs"),
            name = "EmptyDataTabs",
            className = "EmptyDataTabs",
            packageName = "com.example",
            tabs = emptyList(),
            isDataClass = true,
            isObject = false,
            constructorParams = emptyList(),
        )

        val result = validate(listOf(tab))

        // Warning does not cause failure
        result.shouldBeTrue()
        logger.warnings.shouldHaveSize(2) // empty container + empty data class warnings
        logger.warnings.any {
            it.contains("no constructor parameters") && it.contains("EmptyDataTabs")
        }.shouldBeTrue()
    }

    // =========================================================================
    // Data class @Tabs where no params have @Argument → error
    // =========================================================================

    test("data class Tabs with params missing all @Argument annotations produces error") {
        val tab = TabInfo(
            classDeclaration = dataClassDecl("BadTabs"),
            name = "BadTabs",
            className = "BadTabs",
            packageName = "com.example",
            tabs = emptyList(),
            isDataClass = true,
            isObject = false,
            constructorParams = listOf(
                param("id"),
                param("label"),
            ),
        )

        val result = validate(listOf(tab))

        result.shouldBeFalse()
        logger.errors.shouldHaveSize(1)
        logger.errors[0].shouldContain("without @Argument annotations")
        logger.errors[0].shouldContain("BadTabs")
    }

    // =========================================================================
    // Data class @Tabs where some params have @Argument and some don't → error
    // =========================================================================

    test("data class Tabs with mixed @Argument annotations produces error") {
        val tab = TabInfo(
            classDeclaration = dataClassDecl("MixedTabs"),
            name = "MixedTabs",
            className = "MixedTabs",
            packageName = "com.example",
            tabs = emptyList(),
            isDataClass = true,
            isObject = false,
            constructorParams = listOf(
                param("id", isArgument = true),
                param("label", isArgument = false),
            ),
        )

        val result = validate(listOf(tab))

        result.shouldBeFalse()
        logger.errors.shouldHaveSize(1)
        logger.errors[0].shouldContain("missing @Argument annotation")
        logger.errors[0].shouldContain("'label'")
        logger.errors[0].shouldContain("MixedTabs")
    }

    // =========================================================================
    // Data class @Tabs where all params have @Argument → valid
    // =========================================================================

    test("data class Tabs with all @Argument params is valid") {
        val tab = TabInfo(
            classDeclaration = dataClassDecl("GoodTabs"),
            name = "GoodTabs",
            className = "GoodTabs",
            packageName = "com.example",
            tabs = emptyList(),
            isDataClass = true,
            isObject = false,
            constructorParams = listOf(
                param("id", isArgument = true),
                param("label", isArgument = true),
            ),
        )

        val result = validate(listOf(tab))

        // Only the "empty container" warning expected, no errors
        result.shouldBeTrue()
        logger.errors.shouldBeEmpty()
    }

    // =========================================================================
    // Object @Tabs → no data class validation triggered
    // =========================================================================

    test("object Tabs does not trigger data class validation") {
        val tab = TabInfo(
            classDeclaration = objectDecl("ObjectTabs"),
            name = "ObjectTabs",
            className = "ObjectTabs",
            packageName = "com.example",
            tabs = emptyList(),
            isDataClass = false,
            isObject = true,
            constructorParams = emptyList(),
        )

        val result = validate(listOf(tab))

        // Only the "empty container" warning expected, no data class errors
        result.shouldBeTrue()
        logger.errors.shouldBeEmpty()
    }
})
