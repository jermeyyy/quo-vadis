package com.jermey.quo.vadis.gradle

import org.gradle.api.provider.Property

/**
 * Extension for configuring the Quo Vadis navigation library.
 *
 * Example usage:
 * ```kotlin
 * quoVadis {
 *     modulePrefix = "customPrefix"
 *     useLocalKsp = true // for development
 *     useCompilerPlugin = true // use compiler plugin instead of KSP
 * }
 * ```
 */
abstract class QuoVadisExtension {
    /**
     * Module prefix for generated code.
     * Used to distinguish generated code from different modules.
     * Defaults to project.name converted to camelCase.
     */
    abstract val modulePrefix: Property<String>

    /**
     * Whether to use local KSP processor (project dependency).
     * Useful during development. Defaults to false.
     * When false, uses Maven Central artifact.
     * Only applies when [useCompilerPlugin] is false.
     */
    abstract val useLocalKsp: Property<Boolean>

    /**
     * Whether to use the K2 compiler plugin instead of KSP for code generation.
     * When true, the compiler plugin is used and KSP is not required.
     * When false (default), KSP mode is used.
     */
    abstract val useCompilerPlugin: Property<Boolean>
}
