package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.testutil.FakeKSAnnotation
import com.jermey.quo.vadis.ksp.testutil.FakeKSClassDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSFile
import com.jermey.quo.vadis.ksp.testutil.FakeKSFunctionDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSPLogger
import com.jermey.quo.vadis.ksp.testutil.FakeKSValueArgument
import com.jermey.quo.vadis.ksp.testutil.FakeKSValueParameter
import com.jermey.quo.vadis.ksp.testutil.fakeKSType
import com.jermey.quo.vadis.ksp.testutil.fakeResolver
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Tests for [TabExtractor] data class extraction logic.
 *
 * Verifies that [TabExtractor.extractAll] correctly detects object vs data class
 * `@Tabs` declarations and extracts constructor parameters with `@Argument` metadata.
 */
class TabExtractorDataClassTest : FunSpec({

    val logger = FakeKSPLogger()
    val destinationExtractor = DestinationExtractor(logger)

    fun tabExtractor() = TabExtractor(
        destinationExtractor = destinationExtractor,
        logger = logger,
    )

    // -- Annotation helpers --

    fun tabsAnnotation(name: String) = FakeKSAnnotation(
        annotationName = "Tabs",
        arguments = listOf(FakeKSValueArgument("name", name)),
    )

    fun tabItemAnnotation(
        parentQualifiedName: String,
        isDefault: Boolean = false,
    ) = FakeKSAnnotation(
        annotationName = "TabItem",
        arguments = listOf(
            FakeKSValueArgument("parent", fakeKSType(parentQualifiedName)),
            FakeKSValueArgument("isDefault", isDefault),
        ),
    )

    fun destinationAnnotation(route: String = "test_route") = FakeKSAnnotation(
        annotationName = "Destination",
        arguments = listOf(FakeKSValueArgument("route", route)),
    )

    // -- Declaration helpers --

    fun tabItemDecl(
        name: String,
        parentQualifiedName: String,
        isDefault: Boolean = false,
        pkg: String = "com.example",
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = ClassKind.OBJECT,
        modifiers = setOf(Modifier.DATA),
        customAnnotations = listOf(
            tabItemAnnotation(parentQualifiedName, isDefault),
            destinationAnnotation("$name/route"),
        ),
        containingFile = FakeKSFile(pkg = pkg),
    )

    fun objectTabsDecl(
        name: String,
        pkg: String = "com.example",
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = ClassKind.OBJECT,
        customAnnotations = listOf(tabsAnnotation(name)),
        containingFile = FakeKSFile(pkg = pkg),
    )

    fun dataClassTabsDecl(
        name: String,
        params: List<FakeKSValueParameter> = emptyList(),
        pkg: String = "com.example",
    ): FakeKSClassDeclaration {
        val constructor = FakeKSFunctionDeclaration(
            name = "<init>",
            packageName = pkg,
            parameterList = params,
        )
        return FakeKSClassDeclaration(
            name = name,
            qualifiedName = "$pkg.$name",
            packageName = pkg,
            classKind = ClassKind.CLASS,
            modifiers = setOf(Modifier.DATA),
            customAnnotations = listOf(tabsAnnotation(name)),
            containingFile = FakeKSFile(pkg = pkg),
            primaryConstructorOverride = constructor,
        )
    }

    fun argumentParam(
        name: String,
        argumentKey: String = "",
    ) = FakeKSValueParameter(
        paramName = name,
        paramType = fakeKSType("kotlin.String"),
        paramAnnotations = listOf(
            FakeKSAnnotation(
                annotationName = "Argument",
                arguments = listOf(
                    FakeKSValueArgument("key", argumentKey),
                    FakeKSValueArgument("optional", false),
                ),
            ),
        ),
    )

    fun plainParam(name: String) = FakeKSValueParameter(
        paramName = name,
        paramType = fakeKSType("kotlin.String"),
    )

    fun resolver(
        tabs: List<FakeKSClassDeclaration>,
        tabItems: List<FakeKSClassDeclaration>,
    ) = fakeResolver(
        symbolsByAnnotation = mapOf(
            "com.jermey.quo.vadis.annotations.Tabs" to tabs,
            "com.jermey.quo.vadis.annotations.TabItem" to tabItems,
        ),
    )

    beforeTest {
        logger.errors.clear()
        logger.warnings.clear()
        logger.infos.clear()
    }

    // =========================================================================
    // Object @Tabs → isDataClass=false, isObject=true, constructorParams=[]
    // =========================================================================

    test("object Tabs produces isDataClass=false, isObject=true, empty constructorParams") {
        val parentName = "com.example.ObjectTabs"
        val tabsDecl = objectTabsDecl("ObjectTabs")
        val childDecl = tabItemDecl("HomeTab", parentName, isDefault = true)

        val result = tabExtractor().extractAll(resolver(listOf(tabsDecl), listOf(childDecl)))

        result.shouldHaveSize(1)
        val info = result[0]
        info.name shouldBe "ObjectTabs"
        info.className shouldBe "ObjectTabs"
        info.isDataClass.shouldBeFalse()
        info.isObject.shouldBeTrue()
        info.constructorParams.shouldBeEmpty()
    }

    // =========================================================================
    // Data class @Tabs with single @Argument param
    // =========================================================================

    test("data class Tabs with single @Argument param extracts correctly") {
        val parentName = "com.example.DataTabs"
        val tabsDecl = dataClassTabsDecl(
            name = "DataTabs",
            params = listOf(argumentParam("userId")),
        )
        val childDecl = tabItemDecl("HomeTab", parentName, isDefault = true)

        val result = tabExtractor().extractAll(resolver(listOf(tabsDecl), listOf(childDecl)))

        result.shouldHaveSize(1)
        val info = result[0]
        info.name shouldBe "DataTabs"
        info.isDataClass.shouldBeTrue()
        info.isObject.shouldBeFalse()
        info.constructorParams.shouldHaveSize(1)
        info.constructorParams[0].name shouldBe "userId"
        info.constructorParams[0].isArgument.shouldBeTrue()
        info.constructorParams[0].argumentKey shouldBe "userId"
    }

    // =========================================================================
    // Data class @Tabs with multiple @Argument params
    // =========================================================================

    test("data class Tabs with multiple @Argument params extracts all correctly") {
        val parentName = "com.example.MultiParamTabs"
        val tabsDecl = dataClassTabsDecl(
            name = "MultiParamTabs",
            params = listOf(
                argumentParam("userId"),
                argumentParam("sessionId", argumentKey = "session"),
            ),
        )
        val childDecl = tabItemDecl("Tab1", parentName, isDefault = true)

        val result = tabExtractor().extractAll(resolver(listOf(tabsDecl), listOf(childDecl)))

        result.shouldHaveSize(1)
        val info = result[0]
        info.isDataClass.shouldBeTrue()
        info.isObject.shouldBeFalse()
        info.constructorParams.shouldHaveSize(2)
        info.constructorParams[0].name shouldBe "userId"
        info.constructorParams[0].isArgument.shouldBeTrue()
        info.constructorParams[0].argumentKey shouldBe "userId"
        info.constructorParams[1].name shouldBe "sessionId"
        info.constructorParams[1].isArgument.shouldBeTrue()
        info.constructorParams[1].argumentKey shouldBe "session"
    }

    // =========================================================================
    // Data class @Tabs with params lacking @Argument — extraction still works
    // (validation catches this later)
    // =========================================================================

    test("data class Tabs without @Argument params still extracts params correctly") {
        val parentName = "com.example.NoArgTabs"
        val tabsDecl = dataClassTabsDecl(
            name = "NoArgTabs",
            params = listOf(plainParam("id"), plainParam("label")),
        )
        val childDecl = tabItemDecl("Tab1", parentName, isDefault = true)

        val result = tabExtractor().extractAll(resolver(listOf(tabsDecl), listOf(childDecl)))

        result.shouldHaveSize(1)
        val info = result[0]
        info.isDataClass.shouldBeTrue()
        info.isObject.shouldBeFalse()
        info.constructorParams.shouldHaveSize(2)
        info.constructorParams[0].name shouldBe "id"
        info.constructorParams[0].isArgument.shouldBeFalse()
        info.constructorParams[1].name shouldBe "label"
        info.constructorParams[1].isArgument.shouldBeFalse()
    }
})
