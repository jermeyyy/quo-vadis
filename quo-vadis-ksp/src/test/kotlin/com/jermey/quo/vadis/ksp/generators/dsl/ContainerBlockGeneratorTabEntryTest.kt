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
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for [ContainerBlockGenerator] focusing on tab entry code generation
 * for each [TabItemType].
 */
class ContainerBlockGeneratorTabEntryTest {

    private val logger = FakeKSPLogger()
    private val generator = ContainerBlockGenerator(logger)

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

    private fun tabInfo(
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

    @Test
    fun `DESTINATION data object generates tab() call`() {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SettingsTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
            ),
            tabType = TabItemType.DESTINATION,
            ordinal = 0,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(
            output.contains("tab(com.example.SettingsTab)"),
            "Expected tab(SettingsTab) call for DESTINATION data object, got:\n$output"
        )
    }

    @Test
    fun `DESTINATION class generates containerTab call`() {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SettingsTab",
                classKind = ClassKind.CLASS,
            ),
            tabType = TabItemType.DESTINATION,
            ordinal = 0,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(
            output.contains("containerTab<com.example.SettingsTab>()"),
            "Expected containerTab<SettingsTab>() call for DESTINATION class, got:\n$output"
        )
    }

    // -- STACK tests --

    @Test
    fun `STACK generates containerTab call`() {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "HomeTab",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
            ordinal = 0,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(
            output.contains("containerTab<com.example.HomeTab>()"),
            "Expected containerTab<HomeTab>() for STACK, got:\n$output"
        )
    }

    // -- TABS tests --

    @Test
    fun `TABS with Stack generates containerTab call`() {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "FeatureDestination",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
            ordinal = 0,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(
            output.contains("containerTab<com.example.FeatureDestination>()"),
            "Expected containerTab<FeatureDestination>() for STACK, got:\n$output"
        )
    }

    @Test
    fun `TABS with Tabs annotation generates containerTab call`() {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SubTabs",
                annotations = listOf("Tabs"),
            ),
            tabType = TabItemType.TABS,
            ordinal = 0,
            destinationInfo = null,
            stackInfo = null,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(
            output.contains("containerTab<com.example.SubTabs>()"),
            "Expected containerTab<SubTabs>() for TABS @Tabs, got:\n$output"
        )
    }

    // -- Mixed tab types --

    @Test
    fun `mixed tab types generate correct calls`() {
        val flatItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "SettingsTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
            ),
            tabType = TabItemType.DESTINATION,
            ordinal = 0,
        )
        val nestedItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "HomeTab",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
            ordinal = 1,
        )
        val refItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "FeatureDestination",
                annotations = listOf("Stack"),
            ),
            tabType = TabItemType.STACK,
            ordinal = 2,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(items = listOf(flatItem, nestedItem, refItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(output.contains("tab(com.example.SettingsTab)"), "Missing flat tab call")
        assertTrue(output.contains("containerTab<com.example.HomeTab>()"), "Missing nested stack call")
        assertTrue(output.contains("containerTab<com.example.FeatureDestination>()"), "Missing stack tab call")
    }

    // -- Container generation structure --

    @Test
    fun `tabs block uses scopeKey from tab name`() {
        val tabItem = TabItemInfo(
            classDeclaration = sealedClassDecl(
                name = "HomeTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
            ),
            tabType = TabItemType.DESTINATION,
            ordinal = 0,
        )

        val output = generator.generate(
            tabs = listOf(tabInfo(name = "mainTabs", items = listOf(tabItem))),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(
            output.contains("scopeKey = \"mainTabs\""),
            "Expected scopeKey = \"mainTabs\" in output, got:\n$output"
        )
    }

    @Test
    fun `empty containers produce empty CodeBlock`() {
        val output = generator.generate(
            tabs = emptyList(),
            stacks = emptyList(),
            panes = emptyList(),
        ).toString()

        assertTrue(output.isEmpty(), "Expected empty output for no containers, got:\n$output")
    }
}
