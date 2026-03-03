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
    @Deprecated("Migrate to the compiler plugin. KSP support will be removed in a future version.")
    abstract val useLocalKsp: Property<Boolean>

    /**
     * Whether to use the K2 compiler plugin instead of KSP for code generation.
     * Defaults to true. Set to false to use legacy KSP mode (deprecated).
     */
    abstract val useCompilerPlugin: Property<Boolean>
}
