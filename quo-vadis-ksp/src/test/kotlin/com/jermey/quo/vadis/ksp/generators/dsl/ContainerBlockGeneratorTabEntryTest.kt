package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.StackInfo
import com.jermey.quo.vadis.ksp.models.TabInfo
import com.jermey.quo.vadis.ksp.models.TabItemInfo
import com.jermey.quo.vadis.ksp.models.TabItemType
import com.jermey.quo.vadis.ksp.testutil.FakeKSClassDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSPLogger
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.string.shouldContain

/**
 * Tests for [ContainerBlockGenerator] focusing on tab entry code generation
 * for each [TabItemType].
 */
class ContainerBlockGeneratorTabEntryTest : FunSpec({

    val logger = FakeKSPLogger()
    val generator = ContainerBlockGenerator(logger)

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

    fun tabInfo(
        name: String = "TestTabs",
        items: List<TabItemInfo>,
    ): TabInfo {
        val containerDecl = sealedClassDecl(name)
        return TabInfo(
            classDeclaration = containerDecl,
            name = name,
            className = name,
            packageName = "com.example",
            tabs = items,
        )
    }

    // -- DESTINATION tests --

    test("DESTINATION data object generates tab() call") {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SettingsTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
            ),
            tabType = TabItemType.DESTINATION,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        output.shouldContain("tab(com.example.SettingsTab)")
    }

    test("DESTINATION class generates containerTab call") {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SettingsTab",
                classKind = ClassKind.CLASS,
            ),
            tabType = TabItemType.DESTINATION,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        output.shouldContain("containerTab<com.example.SettingsTab>()")
    }

    // -- STACK tests --

    test("STACK generates containerTab call") {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "HomeTab",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        output.shouldContain("containerTab<com.example.HomeTab>()")
    }

    // -- TABS tests --

    test("TABS with Stack generates containerTab call") {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "FeatureDestination",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        output.shouldContain("containerTab<com.example.FeatureDestination>()")
    }

    test("TABS with Tabs annotation generates containerTab call") {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SubTabs",
                annotations = listOf("Tabs"),
            ),
            tabType = TabItemType.TABS,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        output.shouldContain("containerTab<com.example.SubTabs>()")
    }

    // -- Mixed tab types --

    test("mixed tab types generate correct calls") {
        val flatItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SettingsTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
            ),
            tabType = TabItemType.DESTINATION,
        )
        val nestedItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "HomeTab",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
        )
        val refItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "FeatureDestination",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(flatItem, nestedItem, refItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        output.shouldContain("tab(com.example.SettingsTab)")
        output.shouldContain("containerTab<com.example.HomeTab>()")
        output.shouldContain("containerTab<com.example.FeatureDestination>()")
    }

    // -- Container generation structure --

    test("tabs block uses scopeKey from tab name") {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "HomeTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
            ),
            tabType = TabItemType.DESTINATION,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(name = "mainTabs", items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        output.shouldContain("scopeKey = \"mainTabs\"")
    }

    test("empty containers produce empty CodeBlock") {
        val output = generator.generate(
            tabs = emptyList(),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        withClue("Expected empty output for no containers, got:\n$output") {
            output.isEmpty().shouldBeTrue()
        }
    }
})
