package com.jermey.quo.vadis.ksp.generators.base

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSNode
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Abstract base class for DSL code generators.
 *
 * Provides common utilities shared across all generators:
 * - Class name resolution from KSP symbols
 * - File generation helpers
 * - Logging utilities
 * - Import management
 *
 * ## Usage
 *
 * Extend this class and implement the generation logic:
 * ```kotlin
 * class MyGenerator(
 *     codeGenerator: CodeGenerator,
 *     logger: KSPLogger
 * ) : DslCodeGenerator(codeGenerator, logger) {
 *     fun generate(symbols: List<KSClassDeclaration>) {
 *         val fileSpec = createFileBuilder("MyFile").build()
 *         writeFile(fileSpec, originatingFiles)
 *     }
 * }
 * ```
 *
 * @property codeGenerator KSP code generator for writing output files
 * @property logger KSP logger for diagnostic messages
 */
abstract class DslCodeGenerator(
    protected val codeGenerator: CodeGenerator,
    protected val logger: KSPLogger
) {

    /**
     * The package name for generated code.
     * Override in subclasses if a different package is needed.
     */
    protected open val generatedPackage: String = DEFAULT_GENERATED_PACKAGE

    /**
     * Resolves a fully qualified class name from a KSClassDeclaration.
     * Handles nested classes correctly (e.g., `Outer.Inner`).
     *
     * Uses KotlinPoet's KSP extension for proper resolution.
     *
     * @param declaration The KSP class declaration
     * @return The resolved ClassName suitable for use with KotlinPoet
     */
    protected fun resolveClassName(declaration: KSClassDeclaration): ClassName {
        return declaration.toClassName()
    }

    /**
     * Resolves a class name string suitable for generated code.
     * Handles nested classes with proper dot-separated formatting.
     *
     * For a nested class like `MainTabs.HomeTab`, this returns `"MainTabs.HomeTab"`.
     *
     * @param declaration The KSP class declaration
     * @return String representation suitable for code generation
     */
    protected fun resolveClassNameString(declaration: KSClassDeclaration): String {
        val className = resolveClassName(declaration)
        return if (className.simpleNames.size > 1) {
            // Nested class: join with dots
            className.simpleNames.joinToString(".")
        } else {
            className.simpleName
        }
    }

    /**
     * Builds a ClassName for a KSClassDeclaration, handling nested classes manually.
     *
     * This is useful when you need explicit control over the ClassName construction.
     * For nested classes like `MainTabs.HomeTab`, this returns
     * `ClassName(packageName, "MainTabs", "HomeTab")`.
     *
     * @param declaration The KSP class declaration
     * @return ClassName with all enclosing class names
     */
    protected fun buildClassName(declaration: KSClassDeclaration): ClassName {
        val packageName = declaration.packageName.asString()
        val simpleNames = mutableListOf<String>()

        // Walk up the parent chain to collect all enclosing class names
        var current: com.google.devtools.ksp.symbol.KSDeclaration? = declaration
        while (current is KSClassDeclaration) {
            simpleNames.add(0, current.simpleName.asString())
            current = current.parentDeclaration
        }

        return ClassName(packageName, simpleNames)
    }

    /**
     * Finds the top-level containing class for a potentially nested class.
     *
     * Walks up the parent chain until finding a class whose parent is not a class.
     *
     * @param declaration The KSP class declaration
     * @return The top-level class declaration
     */
    protected fun findTopLevelClass(declaration: KSClassDeclaration): KSClassDeclaration {
        var current = declaration
        while (current.parentDeclaration is KSClassDeclaration) {
            current = current.parentDeclaration as KSClassDeclaration
        }
        return current
    }

    /**
     * Creates a FileSpec.Builder with standard configuration.
     *
     * Includes:
     * - Standard file header comment
     * - Configured package name
     *
     * @param fileName The name of the generated file (without .kt extension)
     * @param packageName Optional package name override; defaults to [generatedPackage]
     * @return Configured FileSpec.Builder ready for adding types
     */
    protected fun createFileBuilder(
        fileName: String,
        packageName: String = generatedPackage
    ): FileSpec.Builder {
        return FileSpec.builder(packageName, fileName)
            .addFileComment(StringTemplates.FILE_HEADER.trimMargin())
    }

    /**
     * Writes a FileSpec to the code generator output.
     *
     * @param fileSpec The file specification to write
     * @param originatingFiles The source files that triggered generation (for incremental compilation)
     */
    protected fun writeFile(
        fileSpec: FileSpec,
        originatingFiles: List<KSFile>
    ) {
        fileSpec.writeTo(codeGenerator, aggregating = true, originatingFiles)
    }

    /**
     * Logs an info-level message.
     *
     * @param message The message to log
     */
    protected fun logInfo(message: String) {
        logger.info("$LOG_PREFIX$message")
    }

    /**
     * Logs a warning-level message.
     *
     * @param message The warning message to log
     */
    protected fun logWarning(message: String) {
        logger.warn("$LOG_PREFIX$message")
    }

    /**
     * Logs an error-level message.
     *
     * @param message The error message to log
     * @param symbol Optional KSP symbol associated with the error
     */
    protected fun logError(message: String, symbol: KSNode? = null) {
        logger.error("$LOG_PREFIX$message", symbol)
    }

    /**
     * Creates a standard DSL block indentation string.
     *
     * @param level The indentation level (1 = 4 spaces)
     * @return String containing the appropriate number of spaces
     */
    protected fun indent(level: Int = 1): String = INDENT.repeat(level)

    /**
     * Common imports needed by most generators.
     *
     * Includes:
     * - NavigationConfig
     * - NavDestination
     * - KClass
     */
    protected val commonImports: List<ClassName> = listOf(
        ClassName("com.jermey.quo.vadis.core.navigation", "NavigationConfig"),
        ClassName("com.jermey.quo.vadis.core.navigation", "NavDestination"),
        ClassName("kotlin.reflect", "KClass")
    )

    companion object {
        /**
         * Default generated file name for navigation configuration.
         */
        const val GENERATED_FILE_NAME = "GeneratedNavigationConfig"

        /**
         * Default generated object name for navigation configuration.
         */
        const val GENERATED_OBJECT_NAME = "GeneratedNavigationConfig"

        /**
         * Default package for generated code.
         */
        const val DEFAULT_GENERATED_PACKAGE = "com.jermey.quo.vadis.generated"

        /**
         * Log message prefix for QuoVadis KSP processor.
         */
        private const val LOG_PREFIX = "QuoVadis: "

        /**
         * Standard indentation (4 spaces).
         */
        private const val INDENT = "    "
    }
}
