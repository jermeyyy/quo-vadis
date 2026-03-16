package com.jermey.quo.vadis.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property

/**
 * Extension for configuring the Quo Vadis navigation library.
 *
 * Example usage:
 * ```kotlin
 * quoVadis {
 *     modulePrefix = "customPrefix"
 *     backend = QuoVadisBackend.COMPILER
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
     * Only applies when the resolved backend is [QuoVadisBackend.KSP].
     */
    @Deprecated("Migrate to the compiler plugin. KSP support will be removed in a future version.")
    abstract val useLocalKsp: Property<Boolean>

    /**
     * Backend used for navigation code generation.
     * Defaults to [QuoVadisBackend.KSP].
     */
    abstract val backend: Property<QuoVadisBackend>

    /**
     * Deprecated compatibility alias for [backend].
     * Set to true for [QuoVadisBackend.COMPILER], false for [QuoVadisBackend.KSP].
     */
    @Deprecated("Use backend = QuoVadisBackend.COMPILER or backend = QuoVadisBackend.KSP instead.")
    abstract val useCompilerPlugin: Property<Boolean>
}

enum class QuoVadisBackend {
    KSP,
    COMPILER;

    companion object {
        fun valueOfNormalized(value: String): QuoVadisBackend = when (value.trim().lowercase()) {
            "ksp" -> KSP
            "compiler" -> COMPILER
            else -> throw GradleException(
                "Unknown Quo Vadis backend '$value'. Expected one of: ksp, compiler."
            )
        }
    }
}

private const val BACKEND_PROPERTY = "quoVadis.backend"
private const val DEPRECATED_COMPILER_ALIAS_PROPERTY = "quoVadis.useCompilerPlugin"
private const val USE_LOCAL_KSP_PROPERTY = "quoVadis.useLocalKsp"

internal fun QuoVadisExtension.resolveBackend(project: Project): QuoVadisBackend {
    val configuredBackend = backend.orNull
        ?: project.providers.gradleProperty(BACKEND_PROPERTY).orNull?.let(QuoVadisBackend::valueOfNormalized)
    if (configuredBackend != null) {
        return configuredBackend
    }

    val deprecatedAlias = useCompilerPlugin.orNull?.let { useCompilerPlugin ->
        if (useCompilerPlugin) QuoVadisBackend.COMPILER else QuoVadisBackend.KSP
    }
        ?: project.providers.gradleProperty(DEPRECATED_COMPILER_ALIAS_PROPERTY).orNull?.let(::parseCompilerPluginAlias)
    return deprecatedAlias ?: QuoVadisBackend.KSP
}

internal fun QuoVadisExtension.hasConfiguredBackend(project: Project): Boolean {
    return backend.orNull != null || project.providers.gradleProperty(BACKEND_PROPERTY).orNull != null
}

internal fun QuoVadisExtension.hasDeprecatedCompilerAlias(project: Project): Boolean {
    return useCompilerPlugin.orNull != null ||
        project.providers.gradleProperty(DEPRECATED_COMPILER_ALIAS_PROPERTY).orNull != null
}

private fun parseCompilerPluginAlias(value: String): QuoVadisBackend = when (value.trim().lowercase()) {
    "true" -> QuoVadisBackend.COMPILER
    "false" -> QuoVadisBackend.KSP
    else -> throw GradleException(
        "Invalid value '$value' for quoVadis.useCompilerPlugin. Expected true or false."
    )
}

internal fun QuoVadisExtension.resolveUseLocalKsp(project: Project): Boolean {
    // Extension property takes precedence when explicitly set to true
    if (useLocalKsp.orNull == true) return true

    // Fall back to gradle property (ignoring convention default of false)
    return project.providers.gradleProperty(USE_LOCAL_KSP_PROPERTY).orNull
        ?.trim()?.lowercase()?.toBooleanStrictOrNull()
        ?: false
}
