package com.jermey.quo.vadis.ksp.validation

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.jermey.quo.vadis.ksp.testutil.FakeKSClassDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSFile
import com.jermey.quo.vadis.ksp.testutil.FakeKSFunctionDeclaration
import com.jermey.quo.vadis.ksp.testutil.FakeKSPLogger
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Tests for [ValidationEngine.validateScreenBindings] — specifically the cross-module
 * `@Screen` binding fix where destinations from compiled dependencies (containingFile == null)
 * should not produce "references a class without @Destination" errors.
 */
class ValidationEngineScreenBindingTest : FunSpec({

    val logger = FakeKSPLogger()
    val engine = ValidationEngine(logger)

    // -- Helpers --

    fun destClassDecl(
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

    fun screenInfo(
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

    fun destinationInfo(
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

    fun validate(
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

    test("cross-module Screen referencing destination not in local list does not produce error") {
        // Destination from compiled dependency: containingFile == null, not in allDestinations
        val crossModuleDest = destClassDecl(
            name = "RemoteDestination",
            pkg = "com.other.module",
            containingFile = null, // compiled dependency
        )
        val screen = screenInfo("RemoteScreen", crossModuleDest)

        validate(screens = listOf(screen), destinations = emptyList())

        logger.errors.filter { it.contains("references a class without @Destination") }.shouldBeEmpty()
    }

    test("local Screen referencing class without Destination produces error") {
        // Local class (has containingFile) NOT in allDestinations → should error
        val localNonDest = destClassDecl(
            name = "NotADestination",
            containingFile = FakeKSFile(),
            annotations = emptyList(), // no @Destination
        )
        val screen = screenInfo("BadScreen", localNonDest)

        val result = validate(screens = listOf(screen), destinations = emptyList())

        result.shouldBeFalse()
        logger.errors.any { it.contains("references a class without @Destination") }.shouldBeTrue()
    }

    test("local Screen referencing valid destination in list does not produce error") {
        val destDecl = destClassDecl("ValidDest", containingFile = FakeKSFile())
        val dest = destinationInfo(destDecl)
        val screen = screenInfo("GoodScreen", destDecl)

        validate(screens = listOf(screen), destinations = listOf(dest))

        logger.errors.filter { it.contains("references a class without @Destination") }.shouldBeEmpty()
    }

    test("duplicate Screen bindings for same destination produce errors") {
        val destDecl = destClassDecl("SharedDest", containingFile = FakeKSFile())
        val dest = destinationInfo(destDecl)
        val screen1 = screenInfo("ScreenA", destDecl)
        val screen2 = screenInfo("ScreenB", destDecl)

        val result = validate(screens = listOf(screen1, screen2), destinations = listOf(dest))

        result.shouldBeFalse()
        logger.errors.any { it.contains("Multiple @Screen bindings") }.shouldBeTrue()
    }

    test("missing Screen binding for local destination produces error") {
        val destDecl = destClassDecl("UnboundDest", containingFile = FakeKSFile())
        val dest = destinationInfo(destDecl, isCrossModule = false)

        val result = validate(screens = emptyList(), destinations = listOf(dest))

        result.shouldBeFalse()
        logger.errors.any { it.contains("Missing @Screen binding") }.shouldBeTrue()
    }

    test("missing Screen binding for cross-module destination is skipped") {
        val destDecl = destClassDecl("CrossModuleDest", containingFile = null)
        val dest = destinationInfo(destDecl, isCrossModule = true)

        validate(screens = emptyList(), destinations = listOf(dest))

        logger.errors.filter { it.contains("Missing @Screen binding") }.shouldBeEmpty()
    }
})
