package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.WrapperInfo
import com.jermey.quo.vadis.ksp.models.WrapperType

/**
 * Extracts @TabsContainer and @PaneContainer annotations into [WrapperInfo] models.
 *
 * A wrapper binding connects a @Composable wrapper function to a tab/pane class.
 * This extractor handles:
 * - Function name and package extraction
 * - Target class reference resolution (tabClass/paneClass from annotation)
 * - File path tracking for incremental compilation
 * - Validation of function signatures
 *
 * ## Validation
 *
 * The extractor validates:
 * - Function is annotated with @Composable (warning if missing)
 * - Target class can be resolved from annotation argument
 *
 * Note: Parameter type validation (TabsContainerScope, PaneContainerScope, content lambda)
 * is intentionally lenient since these scope types may not be available during KSP
 * processing or may have different qualified names.
 *
 * @property logger KSP logger for error/warning output
 */
class WrapperExtractor(
    private val logger: KSPLogger
) {

    private companion object {
        const val TABS_CONTAINER_ANNOTATION = "com.jermey.quo.vadis.annotations.TabsContainer"
        const val PANE_CONTAINER_ANNOTATION = "com.jermey.quo.vadis.annotations.PaneContainer"
        const val COMPOSABLE_ANNOTATION = "Composable"
    }

    /**
     * Extract all @TabsContainer-annotated functions from the resolver.
     *
     * Finds all functions annotated with [com.jermey.quo.vadis.annotations.TabsContainer]
     * and extracts their metadata for code generation.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of [WrapperInfo] for all @TabsContainer-annotated functions
     */
    fun extractTabsContainers(resolver: Resolver): List<WrapperInfo> {
        return resolver.getSymbolsWithAnnotation(TABS_CONTAINER_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { extractTabsContainer(it) }
            .toList()
    }

    /**
     * Extract all @PaneContainer-annotated functions from the resolver.
     *
     * Finds all functions annotated with [com.jermey.quo.vadis.annotations.PaneContainer]
     * and extracts their metadata for code generation.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of [WrapperInfo] for all @PaneContainer-annotated functions
     */
    fun extractPaneContainers(resolver: Resolver): List<WrapperInfo> {
        return resolver.getSymbolsWithAnnotation(PANE_CONTAINER_ANNOTATION)
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { extractPaneContainer(it) }
            .toList()
    }

    /**
     * Extract WrapperInfo from a @TabsContainer-annotated function.
     *
     * @param functionDeclaration The @Composable function annotated with @TabsContainer
     * @return [WrapperInfo] or null if extraction fails
     */
    private fun extractTabsContainer(functionDeclaration: KSFunctionDeclaration): WrapperInfo? {
        return extractWrapper(
            functionDeclaration = functionDeclaration,
            annotationName = "TabsContainer",
            argumentName = "tabClass",
            wrapperType = WrapperType.TAB
        )
    }

    /**
     * Extract WrapperInfo from a @PaneContainer-annotated function.
     *
     * @param functionDeclaration The @Composable function annotated with @PaneContainer
     * @return [WrapperInfo] or null if extraction fails
     */
    private fun extractPaneContainer(functionDeclaration: KSFunctionDeclaration): WrapperInfo? {
        return extractWrapper(
            functionDeclaration = functionDeclaration,
            annotationName = "PaneContainer",
            argumentName = "paneClass",
            wrapperType = WrapperType.PANE
        )
    }

    /**
     * Common extraction logic for both TabsContainer and PaneContainer.
     *
     * @param functionDeclaration The annotated function
     * @param annotationName Short name of the annotation ("TabsContainer" or "PaneContainer")
     * @param argumentName Name of the class argument ("tabClass" or "paneClass")
     * @param wrapperType Type of wrapper being extracted
     * @return [WrapperInfo] or null if extraction fails
     */
    private fun extractWrapper(
        functionDeclaration: KSFunctionDeclaration,
        annotationName: String,
        argumentName: String,
        wrapperType: WrapperType
    ): WrapperInfo? {
        val functionName = functionDeclaration.simpleName.asString()

        // Find the wrapper annotation
        val annotation = functionDeclaration.annotations.find {
            it.shortName.asString() == annotationName
        }
        if (annotation == null) {
            logger.error("@$annotationName annotation not found on $functionName")
            return null
        }

        // Validate @Composable annotation
        validateComposableAnnotation(functionDeclaration, annotationName)

        // Extract target class from annotation argument
        // The annotation has a single argument: tabClass or paneClass
        val targetType = annotation.arguments.firstOrNull()?.value as? KSType
        val targetClass = targetType?.declaration as? KSClassDeclaration
        if (targetClass == null) {
            logger.error(
                "Could not resolve $argumentName for @$annotationName on $functionName. " +
                    "Ensure the target class exists and is accessible."
            )
            return null
        }

        val targetQualifiedName = targetClass.qualifiedName?.asString()
        if (targetQualifiedName == null) {
            logger.error(
                "Could not get qualified name for $argumentName in @$annotationName on $functionName"
            )
            return null
        }

        // Get containing file path for incremental compilation
        val containingFile = functionDeclaration.containingFile?.filePath ?: ""

        // For companion objects, use the enclosing class name instead of "Companion"
        // This handles cases like @TabsContainer(DemoTabs.Companion::class)
        val simpleName = targetClass.simpleName.asString()
        val effectiveSimpleName = if (simpleName == "Companion") {
            // Get parent class declaration
            val parentClass = targetClass.parentDeclaration as? KSClassDeclaration
            parentClass?.simpleName?.asString() ?: simpleName
        } else {
            simpleName
        }

        return WrapperInfo(
            functionDeclaration = functionDeclaration,
            functionName = functionName,
            packageName = functionDeclaration.packageName.asString(),
            targetClass = targetClass,
            targetClassQualifiedName = targetQualifiedName,
            targetClassSimpleName = effectiveSimpleName,
            containingFile = containingFile,
            wrapperType = wrapperType
        )
    }

    /**
     * Validates that the function has @Composable annotation.
     *
     * This is a warning rather than an error because:
     * 1. The function might work correctly at runtime even without proper annotation
     * 2. The @Composable annotation might be applied via other mechanisms
     *
     * @param functionDeclaration The function to validate
     * @param wrapperAnnotation The wrapper annotation name for error messages
     */
    private fun validateComposableAnnotation(
        functionDeclaration: KSFunctionDeclaration,
        wrapperAnnotation: String
    ) {
        val hasComposable = functionDeclaration.annotations.any { annotation ->
            annotation.shortName.asString() == COMPOSABLE_ANNOTATION
        }
        if (!hasComposable) {
            logger.warn(
                "Function ${functionDeclaration.simpleName.asString()} is annotated with " +
                    "@$wrapperAnnotation but not @Composable. " +
                    "Wrapper functions should be @Composable functions."
            )
        }
    }
}
