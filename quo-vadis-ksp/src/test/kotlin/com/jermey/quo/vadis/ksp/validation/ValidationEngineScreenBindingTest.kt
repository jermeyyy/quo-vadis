package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.testutil.FakeKSClassDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSFile
import com.jermey.quo.vadis.ksp.testutil.FakeKSFunctionDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSPLogger
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [ValidationEngine.validateScreenBindings] — specifically the cross-module
 * `@Screen` binding fix where destinations from compiled dependencies (containingFile == null)
 * should not produce "references a class without @Destination" errors.
 */
class ValidationEngineScreenBindingTest {

    private val logger = FakeKSPLogger()
    private val engine = ValidationEngine(logger)

    // -- Helpers --

    private fun destClassDecl(
        name: String,
        pkg: String = "com.example",
        containingFile: com.google.devtools.ksp.symbol.KSFile? = FakeKSFile(pkg = pkg),
        annotations: List<String> = listOf("Destination"),
    ) = FakeKSClassDeclaration(
        name = name,
        qualifiedName = "$pkg.$name",
        packageName = pkg,
        classKind = ClassKind.OBJECT,
        modifiers = setOf(Modifier.DATA),
        annotationNames = annotations,
        containingFile = containingFile,
    )

    private fun screenInfo(
        functionName: String,
        destinationClass: FakeKSClassDeclaration,
        pkg: String = "com.example",
    ) = ScreenInfo(
        functionDeclaration = FakeKSFunctionDeclaration(name = functionName, packageName = pkg),
        functionName = functionName,
        destinationClass = destinationClass,
        hasDestinationParam = false,
        hasSharedTransitionScope = false,
        hasAnimatedVisibilityScope = false,
        packageName = pkg,
    )

    private fun destinationInfo(
        classDecl: FakeKSClassDeclaration,
        isCrossModule: Boolean = false,
    ) = DestinationInfo(
        classDeclaration = classDecl,
        className = classDecl.simpleName.asString(),
        qualifiedName = classDecl.qualifiedName.asString(),
        route = null,
        routeParams = emptyList(),
        isObject = true,
        isDataObject = true,
        isDataClass = false,
        isSealedClass = false,
        constructorParams = emptyList(),
        parentSealedClass = null,
        isCrossModule = isCrossModule,
    )

    private fun validate(
        screens: List<ScreenInfo>,
        destinations: List<DestinationInfo> = emptyList(),
    ): Boolean {
        logger.errors.clear()
        logger.warnings.clear()
        return engine.validate(
            stacks = emptyList(),
            tabs = emptyList(),
            panes = emptyList(),
            screens = screens,
            allDestinations = destinations,
        )
    }

    // =========================================================================
    // Cross-module @Screen binding (the bug fix)
    // =========================================================================

    @Test
    fun `cross-module Screen referencing destination not in local list does not produce error`() {
        // Destination from compiled dependency: containingFile == null, not in allDestinations
        val crossModuleDest = destClassDecl(
            name = "RemoteDestination",
            pkg = "com.other.module",
            containingFile = null, // compiled dependency
        )
        val screen = screenInfo("RemoteScreen", crossModuleDest)

        validate(screens = listOf(screen), destinations = emptyList())

        val screenErrors = logger.errors.filter { it.contains("references a class without @Destination") }
        assertTrue(
            screenErrors.isEmpty(),
            "Cross-module @Screen should NOT produce 'class without @Destination' error, got: $screenErrors",
        )
    }

    @Test
    fun `local Screen referencing class without Destination produces error`() {
        // Local class (has containingFile) NOT in allDestinations → should error
        val localNonDest = destClassDecl(
            name = "NotADestination",
            containingFile = FakeKSFile(),
            annotations = emptyList(), // no @Destination
        )
        val screen = screenInfo("BadScreen", localNonDest)

        val result = validate(screens = listOf(screen), destinations = emptyList())

        assertFalse(result, "Validation should fail for local @Screen referencing non-destination")
        assertTrue(
            logger.errors.any { it.contains("references a class without @Destination") },
            "Expected 'references a class without @Destination' error, got: ${logger.errors}",
        )
    }

    @Test
    fun `local Screen referencing valid destination in list does not produce error`() {
        val destDecl = destClassDecl("ValidDest", containingFile = FakeKSFile())
        val dest = destinationInfo(destDecl)
        val screen = screenInfo("GoodScreen", destDecl)

        validate(screens = listOf(screen), destinations = listOf(dest))

        val screenErrors = logger.errors.filter { it.contains("references a class without @Destination") }
        assertTrue(
            screenErrors.isEmpty(),
            "Valid local @Screen should not produce error, got: $screenErrors",
        )
    }

    @Test
    fun `duplicate Screen bindings for same destination produce errors`() {
        val destDecl = destClassDecl("SharedDest", containingFile = FakeKSFile())
        val dest = destinationInfo(destDecl)
        val screen1 = screenInfo("ScreenA", destDecl)
        val screen2 = screenInfo("ScreenB", destDecl)

        val result = validate(screens = listOf(screen1, screen2), destinations = listOf(dest))

        assertFalse(result, "Validation should fail for duplicate screen bindings")
        assertTrue(
            logger.errors.any { it.contains("Multiple @Screen bindings") },
            "Expected duplicate binding error, got: ${logger.errors}",
        )
    }

    @Test
    fun `missing Screen binding for local destination produces error`() {
        val destDecl = destClassDecl("UnboundDest", containingFile = FakeKSFile())
        val dest = destinationInfo(destDecl, isCrossModule = false)

        val result = validate(screens = emptyList(), destinations = listOf(dest))

        assertFalse(result, "Validation should fail for destination without @Screen")
        assertTrue(
            logger.errors.any { it.contains("Missing @Screen binding") },
            "Expected missing screen binding error, got: ${logger.errors}",
        )
    }

    @Test
    fun `missing Screen binding for cross-module destination is skipped`() {
        val destDecl = destClassDecl("CrossModuleDest", containingFile = null)
        val dest = destinationInfo(destDecl, isCrossModule = true)

        validate(screens = emptyList(), destinations = listOf(dest))

        val missingErrors = logger.errors.filter { it.contains("Missing @Screen binding") }
        assertTrue(
            missingErrors.isEmpty(),
            "Cross-module destination should not require @Screen binding, got: $missingErrors",
        )
    }
}
