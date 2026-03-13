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
 * Tests for [ValidationEngine] container reference validation (Phases 1-4 of Issue #41).
 *
 * Covers:
 * - validateContainerReferences: CONTAINER_REFERENCE items must have @Stack or @Tabs
 * - validateCircularTabNesting: cycle detection in tab nesting graph
 * - validateTabNestingDepth: warning when nesting exceeds 3 levels
 * - validateTabItemAnnotations: annotation combination checks for tab items
 */
class ValidationEngineContainerReferenceTest {

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
        classKind: ClassKind = ClassKind.CLASS,
        modifiers: Set<Modifier> = emptySet(),
        annotations: List<String> = emptyList(),
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = classKind,
        modifiers = modifiers,
        annotationNames = annotations,
    )

    /**
     * Creates a [TabInfo] for the sealed container with the given items.
     * Sets isNewPattern = false to avoid validateTabItemsHaveAnnotation requiring @TabItem.
     */
    private fun tabInfo(
        name: String = "TestTabs",
        items: List<TabItemInfo>,
        isNewPattern: Boolean = false,
    ): TabInfo {
        val containerDecl = sealedClassDecl(name, annotations = listOf("Tabs"))
        return TabInfo(
            classDeclaration = containerDecl,
            name = name,
            className = name,
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = isNewPattern,
            tabs = items,
        )
    }

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
    // validateTabItemAnnotations — annotation combination checks
    // =========================================================================

    @Test
    fun `TabItem without Stack or Tabs or Destination produces error`() {
        val item = TabItemInfo(
            classDeclaration = tabItemDecl("Orphan"),
            tabType = TabItemType.CONTAINER_REFERENCE,
        )
        val result = validate(listOf(tabInfo(items = listOf(item))))

        assertFalse(result, "Validation should fail for tab item without @Stack/@Tabs/@Destination")
        assertTrue(
            logger.errors.any { it.contains("neither @Stack") },
            "Expected error about missing annotations, got: ${logger.errors}"
        )
    }

    @Test
    fun `TabItem with Tabs annotation is accepted as valid`() {
        val item = TabItemInfo(
            classDeclaration = tabItemDecl("SubTabs", annotations = listOf("Tabs", "TabItem")),
            tabType = TabItemType.CONTAINER_REFERENCE,
        )
        val result = validate(listOf(tabInfo(items = listOf(item))))

        assertTrue(result, "Validation should pass for @TabItem + @Tabs, errors: ${logger.errors}")
    }

    @Test
    fun `TabItem with Stack annotation is accepted as CONTAINER_REFERENCE`() {
        val item = TabItemInfo(
            classDeclaration = tabItemDecl("FeatureStack", annotations = listOf("Stack", "TabItem")),
            tabType = TabItemType.CONTAINER_REFERENCE,
        )
        val result = validate(listOf(tabInfo(items = listOf(item))))

        assertTrue(result, "Validation should pass for @TabItem + @Stack, errors: ${logger.errors}")
    }

    // =========================================================================
    // validateContainerReferences — CONTAINER_REFERENCE must have @Stack or @Tabs
    // =========================================================================

    @Test
    fun `CONTAINER_REFERENCE without Stack or Tabs annotation produces error`() {
        // TabItem has @TabItem + @Destination to pass validateTabItemAnnotations,
        // but is typed as CONTAINER_REFERENCE → validateContainerReferences should report error
        val item = TabItemInfo(
            classDeclaration = tabItemDecl("BadRef", annotations = listOf("Destination")),
            tabType = TabItemType.CONTAINER_REFERENCE,
        )
        val result = validate(listOf(tabInfo(items = listOf(item))))

        assertFalse(result, "Validation should fail for CONTAINER_REFERENCE without @Stack/@Tabs")
        assertTrue(
            logger.errors.any { it.contains("must be annotated with either @Stack or @Tabs") },
            "Expected container reference error, got: ${logger.errors}"
        )
    }

    @Test
    fun `CONTAINER_REFERENCE with Stack annotation passes container reference validation`() {
        val item = TabItemInfo(
            classDeclaration = tabItemDecl("FeatureStack", annotations = listOf("Stack")),
            tabType = TabItemType.CONTAINER_REFERENCE,
        )
        val result = validate(listOf(tabInfo(items = listOf(item))))

        val containerRefErrors = logger.errors.filter {
            it.contains("must be annotated with either @Stack or @Tabs")
        }
        assertTrue(
            containerRefErrors.isEmpty(),
            "Should not have container reference errors, got: $containerRefErrors"
        )
    }

    @Test
    fun `CONTAINER_REFERENCE with Tabs annotation passes container reference validation`() {
        val item = TabItemInfo(
            classDeclaration = tabItemDecl("SubTabs", annotations = listOf("Tabs")),
            tabType = TabItemType.CONTAINER_REFERENCE,
        )
        val result = validate(listOf(tabInfo(items = listOf(item))))

        val containerRefErrors = logger.errors.filter {
            it.contains("must be annotated with either @Stack or @Tabs")
        }
        assertTrue(
            containerRefErrors.isEmpty(),
            "Should not have container reference errors, got: $containerRefErrors"
        )
    }

    // =========================================================================
    // validateCircularTabNesting — cycle detection
    // =========================================================================

    @Test
    fun `circular nesting A to B to A produces error`() {
        // A references B, B references A
        val declA = sealedClassDecl("TabsA", annotations = listOf("Tabs"))
        val declB = sealedClassDecl("TabsB", annotations = listOf("Tabs"))

        val tabA = TabInfo(
            classDeclaration = declA,
            name = "TabsA",
            className = "TabsA",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(
                    classDeclaration = declB,
                    tabType = TabItemType.CONTAINER_REFERENCE,
                )
            ),
        )
        val tabB = TabInfo(
            classDeclaration = declB,
            name = "TabsB",
            className = "TabsB",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(
                    classDeclaration = declA,
                    tabType = TabItemType.CONTAINER_REFERENCE,
                )
            ),
        )

        val result = validate(listOf(tabA, tabB))

        assertFalse(result, "Validation should fail for circular A→B→A")
        assertTrue(
            logger.errors.any { it.contains("Circular tab nesting detected") },
            "Expected circular nesting error, got: ${logger.errors}"
        )
    }

    @Test
    fun `circular nesting A to B to C to A produces error`() {
        val declA = sealedClassDecl("TabsA", annotations = listOf("Tabs"))
        val declB = sealedClassDecl("TabsB", annotations = listOf("Tabs"))
        val declC = sealedClassDecl("TabsC", annotations = listOf("Tabs"))

        val tabA = TabInfo(
            classDeclaration = declA,
            name = "TabsA",
            className = "TabsA",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declB, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabB = TabInfo(
            classDeclaration = declB,
            name = "TabsB",
            className = "TabsB",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declC, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabC = TabInfo(
            classDeclaration = declC,
            name = "TabsC",
            className = "TabsC",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declA, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )

        val result = validate(listOf(tabA, tabB, tabC))

        assertFalse(result, "Validation should fail for circular A→B→C→A")
        assertTrue(
            logger.errors.any { it.contains("Circular tab nesting detected") },
            "Expected circular nesting error, got: ${logger.errors}"
        )
    }

    @Test
    fun `non-circular nesting A to B to C passes`() {
        val declA = sealedClassDecl("TabsA", annotations = listOf("Tabs"))
        val declB = sealedClassDecl("TabsB", annotations = listOf("Tabs"))
        val declC = sealedClassDecl("TabsC", annotations = listOf("Tabs"))

        // A→B, B→C, C has a flat screen (no CONTAINER_REFERENCE)
        val flatItem = TabItemInfo(
            classDeclaration = tabItemDecl(
                "LeafTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
                annotations = listOf("Destination"),
            ),
            tabType = TabItemType.FLAT_SCREEN,
            destinationInfo = null,
        )

        val tabA = TabInfo(
            classDeclaration = declA,
            name = "TabsA",
            className = "TabsA",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declB, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabB = TabInfo(
            classDeclaration = declB,
            name = "TabsB",
            className = "TabsB",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declC, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabC = TabInfo(
            classDeclaration = declC,
            name = "TabsC",
            className = "TabsC",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(flatItem),
        )

        validate(listOf(tabA, tabB, tabC))

        val circularErrors = logger.errors.filter { it.contains("Circular tab nesting") }
        assertTrue(
            circularErrors.isEmpty(),
            "Should have no circular nesting errors, got: $circularErrors"
        )
    }

    // =========================================================================
    // validateTabNestingDepth — depth warning at > 3 levels
    // =========================================================================

    @Test
    fun `nesting depth of 3 or less produces no warning`() {
        // A→B→C (depth 3), no warning expected
        val declA = sealedClassDecl("TabsA", annotations = listOf("Tabs"))
        val declB = sealedClassDecl("TabsB", annotations = listOf("Tabs"))
        val declC = sealedClassDecl("TabsC", annotations = listOf("Tabs"))

        val flatItem = TabItemInfo(
            classDeclaration = tabItemDecl(
                "LeafTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
                annotations = listOf("Destination"),
            ),
            tabType = TabItemType.FLAT_SCREEN,
        )

        val tabA = TabInfo(
            classDeclaration = declA,
            name = "TabsA",
            className = "TabsA",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declB, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabB = TabInfo(
            classDeclaration = declB,
            name = "TabsB",
            className = "TabsB",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declC, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabC = TabInfo(
            classDeclaration = declC,
            name = "TabsC",
            className = "TabsC",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(flatItem),
        )

        validate(listOf(tabA, tabB, tabC))

        val depthWarnings = logger.warnings.filter { it.contains("nesting depth exceeds") }
        assertTrue(
            depthWarnings.isEmpty(),
            "Should not warn for depth ≤ 3, got: $depthWarnings"
        )
    }

    @Test
    fun `nesting depth greater than 3 produces warning`() {
        // A→B→C→D (depth 4), warning expected on A
        val declA = sealedClassDecl("TabsA", annotations = listOf("Tabs"))
        val declB = sealedClassDecl("TabsB", annotations = listOf("Tabs"))
        val declC = sealedClassDecl("TabsC", annotations = listOf("Tabs"))
        val declD = sealedClassDecl("TabsD", annotations = listOf("Tabs"))

        val flatItem = TabItemInfo(
            classDeclaration = tabItemDecl(
                "LeafTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
                annotations = listOf("Destination"),
            ),
            tabType = TabItemType.FLAT_SCREEN,
        )

        val tabA = TabInfo(
            classDeclaration = declA,
            name = "TabsA",
            className = "TabsA",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declB, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabB = TabInfo(
            classDeclaration = declB,
            name = "TabsB",
            className = "TabsB",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declC, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabC = TabInfo(
            classDeclaration = declC,
            name = "TabsC",
            className = "TabsC",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(
                TabItemInfo(classDeclaration = declD, tabType = TabItemType.CONTAINER_REFERENCE)
            ),
        )
        val tabD = TabInfo(
            classDeclaration = declD,
            name = "TabsD",
            className = "TabsD",
            packageName = "com.example",
            initialTabClass = null,
            isNewPattern = false,
            tabs = listOf(flatItem),
        )

        validate(listOf(tabA, tabB, tabC, tabD))

        val depthWarnings = logger.warnings.filter { it.contains("nesting depth exceeds") }
        assertTrue(
            depthWarnings.isNotEmpty(),
            "Expected depth warning for 4 levels of nesting, warnings: ${logger.warnings}"
        )
    }

    @Test
    fun `single tab container has depth 1 and no warning`() {
        val flatItem = TabItemInfo(
            classDeclaration = tabItemDecl(
                "HomeTab",
                classKind = ClassKind.OBJECT,
                modifiers = setOf(Modifier.DATA),
                annotations = listOf("Destination"),
            ),
            tabType = TabItemType.FLAT_SCREEN,
        )

        validate(listOf(tabInfo(items = listOf(flatItem))))

        val depthWarnings = logger.warnings.filter { it.contains("nesting depth exceeds") }
        assertTrue(
            depthWarnings.isEmpty(),
            "Single tab should not produce depth warning, got: $depthWarnings"
        )
    }
}
