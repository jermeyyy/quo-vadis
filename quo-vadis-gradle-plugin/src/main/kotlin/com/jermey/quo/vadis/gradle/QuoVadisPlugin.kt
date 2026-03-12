package com.jermey.quo.vadis.gradle

import com.jermey.quo.vadis.gradle.internal.toCamelCase
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.create

/**
 * Gradle plugin for Quo Vadis navigation library.
 *
 * Supports two modes:
 * 1. **KSP mode** (default): Uses KSP for code generation
 * 2. **Compiler plugin mode** (experimental): Uses K2 compiler plugin for code generation
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     alias(libs.plugins.kotlinMultiplatform)
 *     alias(libs.plugins.quoVadis)
 * }
 *
 * quoVadis {
 *     modulePrefix = "customPrefix"
 *     backend = QuoVadisBackend.COMPILER
 * }
 * ```
 */
class QuoVadisPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create extension
        val extension = project.extensions.create<QuoVadisExtension>("quoVadis")

        // Set defaults
        extension.modulePrefix.convention(project.name.toCamelCase())
        extension.useLocalKsp.convention(false)

        // Apply compiler subplugin eagerly — KotlinCompilerPluginSupportPlugin must be
        // registered before Kotlin compile tasks are configured (afterEvaluate is too late).
        // The subplugin's isApplicable() checks the resolved backend to gate activation.
        configureCompilerPlugin(project)

        // Configure the selected backend after evaluation so extension values are finalized.
        project.afterEvaluate {
            if (!project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                throw GradleException(
                    "Quo Vadis plugin requires the Kotlin Multiplatform plugin. " +
                        "Please apply 'org.jetbrains.kotlin.multiplatform' plugin before 'io.github.jermeyyy.quo-vadis'."
                )
            }

            val backend = extension.resolveBackend(project)
            warnAboutDeprecatedConfiguration(project, extension)
            validateBackendConfiguration(project, extension, backend)

            when (backend) {
                QuoVadisBackend.COMPILER -> {
                    project.logger.lifecycle(
                        "Quo Vadis: using experimental compiler backend. " +
                            "Module-level generated configs remain the supported interchangeability contract."
                    )
                }

                QuoVadisBackend.KSP -> configureKsp(project, extension)
            }
        }
    }

    private fun configureCompilerPlugin(project: Project) {
        project.plugins.apply(QuoVadisCompilerSubplugin::class.java)
    }

    private fun warnAboutDeprecatedConfiguration(project: Project, extension: QuoVadisExtension) {
        if (extension.hasConfiguredBackend(project) && extension.hasDeprecatedCompilerAlias(project)) {
            project.logger.warn(
                "Quo Vadis: Both 'backend' and deprecated 'useCompilerPlugin' are configured. " +
                    "The backend setting takes precedence."
            )
            return
        }

        if (extension.hasDeprecatedCompilerAlias(project)) {
            project.logger.warn(
                "Quo Vadis: 'useCompilerPlugin' is deprecated. " +
                    "Use 'backend = QuoVadisBackend.COMPILER' or set quoVadis.backend=compiler instead."
            )
        }
    }

    private fun validateBackendConfiguration(
        project: Project,
        extension: QuoVadisExtension,
        backend: QuoVadisBackend
    ) {
        when (backend) {
            QuoVadisBackend.COMPILER -> {
                if (extension.useLocalKsp.get()) {
                    throw GradleException(
                        "Quo Vadis compiler backend does not support useLocalKsp=true. " +
                            "Remove the KSP-only setting or switch backend = QuoVadisBackend.KSP."
                    )
                }

                if (hasQuoVadisKspDependency(project)) {
                    throw GradleException(
                        "Quo Vadis compiler backend cannot run with Quo Vadis KSP processor dependencies present. " +
                            "Remove any io.github.jermeyyy:quo-vadis-ksp or :quo-vadis-ksp dependency from ksp configurations, " +
                            "then run a clean build before switching backends."
                    )
                }
            }

            QuoVadisBackend.KSP -> {
                if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
                    throw GradleException(
                        "Quo Vadis KSP backend requires the KSP plugin. " +
                            "Please apply 'com.google.devtools.ksp' or set backend = QuoVadisBackend.COMPILER."
                    )
                }
            }
        }
    }

    private fun configureKsp(project: Project, extension: QuoVadisExtension) {
        // Add KSP dependency
        val kspDependency = if (extension.useLocalKsp.get()) {
            project.dependencies.project(mapOf("path" to ":quo-vadis-ksp"))
        } else {
            "io.github.jermeyyy:quo-vadis-ksp:${BuildConfig.VERSION}"
        }
        project.dependencies.add("kspCommonMainMetadata", kspDependency)

        configureKspArguments(project, extension)

        configureGeneratedKspSources(project)
        configureKspTaskDependencies(project)
    }

    private fun hasQuoVadisKspDependency(project: Project): Boolean {
        return project.configurations
            .matching { it.name.startsWith("ksp", ignoreCase = true) }
            .any { configuration ->
                configuration.dependencies.any { dependency -> dependency.isQuoVadisKspDependency() }
            }
    }

    private fun configureKspArguments(project: Project, extension: QuoVadisExtension) {
        val kspExtension = project.extensions.findByName("ksp")
            ?: throw GradleException(
                "Quo Vadis KSP backend requires the KSP extension to be available. " +
                    "Please apply 'com.google.devtools.ksp' before configuring backend = QuoVadisBackend.KSP."
            )

        val argMethod = kspExtension.javaClass.methods.firstOrNull { method ->
            method.name == "arg" && method.parameterCount == 2
        } ?: throw GradleException(
            "Quo Vadis could not configure the KSP extension. " +
                "The loaded KSP plugin does not expose the expected arg(String, String) API."
        )

        argMethod.invoke(kspExtension, "quoVadis.modulePrefix", extension.modulePrefix.get())
    }

    private fun configureGeneratedKspSources(project: Project) {
        val kotlinExtension = project.extensions.findByName("kotlin")
            ?: throw GradleException(
                "Quo Vadis KSP backend requires the Kotlin Multiplatform extension to be available."
            )

        val sourceSets = kotlinExtension.invokeNoArg("getSourceSets")
        val commonMain = sourceSets.invokeMethod("getByName", "commonMain")
        val kotlinSources = commonMain.invokeNoArg("getKotlin")
        kotlinSources.invokeMethod("srcDir", "build/generated/ksp/metadata/commonMain/kotlin")
    }

    private fun configureKspTaskDependencies(project: Project) {
        project.tasks.configureEach {
            if (
                name.startsWith("compile") &&
                name.contains("Kotlin") &&
                !name.startsWith("ksp") &&
                !name.contains("Test", ignoreCase = true)
            ) {
                dependsOn("kspCommonMainKotlinMetadata")
            }
        }
    }

    private fun Any.invokeNoArg(methodName: String): Any {
        return javaClass.methods.firstOrNull { method ->
            method.name == methodName && method.parameterCount == 0
        }?.invoke(this) ?: throw GradleException(
            "Quo Vadis could not call $methodName() on ${javaClass.name}."
        )
    }

    private fun Any.invokeMethod(methodName: String, vararg args: Any): Any {
        return javaClass.methods.firstOrNull { method ->
            method.name == methodName && method.parameterCount == args.size
        }?.invoke(this, *args) ?: throw GradleException(
            "Quo Vadis could not call $methodName(${args.size} args) on ${javaClass.name}."
        )
    }

    private fun Dependency.isQuoVadisKspDependency(): Boolean {
        if (group == "io.github.jermeyyy" && name == "quo-vadis-ksp") {
            return true
        }

        return this is ProjectDependency && name == "quo-vadis-ksp"
    }
}
