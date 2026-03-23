package com.jermey.quo.vadis.gradle

import org.gradle.api.provider.Property

/**
 * Extension for configuring the Quo Vadis navigation library KSP processor.
 *
 * Example usage:
 * ```kotlin
 * quoVadis {
 *     modulePrefix = "customPrefix"
 *     useLocalKsp = true // for development
 * }
 * ```
 */
abstract class QuoVadisExtension {
    /**
     * Module prefix for generated code.
     * Used in KSP to distinguish generated code from different modules.
     * Defaults to project.name converted to camelCase.
     */
    abstract val modulePrefix: Property<String>

    /**
     * Whether to use local KSP processor (project dependency).
     * Useful during development. Defaults to false.
     * When false, uses Maven Central artifact.
     */
    abstract val useLocalKsp: Property<Boolean>

    /**
     * Whether this module is an API-only module that defines destinations without @Screen bindings.
     * When true, the KSP processor skips "Missing @Screen binding" validation.
     * Defaults to false.
     */
    abstract val apiModule: Property<Boolean>
}
