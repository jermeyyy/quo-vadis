package com.jermey.quo.vadis.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.jermey.quo.vadis.gradle.internal.toCamelCase
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Gradle plugin for Quo Vadis navigation library.
 *
 * Supports two modes:
 * 1. **KSP mode** (default): Uses KSP for code generation
 * 2. **Compiler plugin mode**: Uses K2 compiler plugin for code generation
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     alias(libs.plugins.kotlinMultiplatform)
 *     alias(libs.plugins.ksp)          // only needed for KSP mode
 *     alias(libs.plugins.quoVadis)
 * }
 *
 * quoVadis {
 *     modulePrefix = "customPrefix"
 *     useCompilerPlugin = true  // opt into compiler plugin mode
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
        extension.useCompilerPlugin.convention(false)

        // Configure after evaluation so extension values are finalized
        project.afterEvaluate {
            // Kotlin Multiplatform is always required
            if (!project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                throw GradleException(
                    "Quo Vadis plugin requires the Kotlin Multiplatform plugin. " +
                        "Please apply 'org.jetbrains.kotlin.multiplatform' plugin before 'io.github.jermeyyy.quo-vadis'."
                )
            }

            if (extension.useCompilerPlugin.get()) {
                configureCompilerPlugin(project, extension)
            } else {
                // KSP mode requires KSP plugin
                if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
                    throw GradleException(
                        "Quo Vadis plugin requires the KSP plugin in KSP mode. " +
                            "Please apply 'com.google.devtools.ksp' plugin or set useCompilerPlugin = true."
                    )
                }
                configureKsp(project, extension)
            }
        }
    }

    private fun configureCompilerPlugin(project: Project, extension: QuoVadisExtension) {
        project.plugins.apply(QuoVadisCompilerSubplugin::class.java)
    }

    private fun configureKsp(project: Project, extension: QuoVadisExtension) {
        // Add KSP dependency
        val kspDependency = if (extension.useLocalKsp.get()) {
            project.dependencies.project(mapOf("path" to ":quo-vadis-ksp"))
        } else {
            "io.github.jermeyyy:quo-vadis-ksp:${BuildConfig.VERSION}"
        }
        project.dependencies.add("kspCommonMainMetadata", kspDependency)

        // Configure KSP arguments
        project.extensions.configure<KspExtension> {
            arg("quoVadis.modulePrefix", extension.modulePrefix.get())
        }

        // Configure generated source directory for commonMain
        project.extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain") {
                kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
            }
        }

        // Fix task dependencies - ensure KSP runs before compilation
        project.tasks.withType<KotlinCompilationTask<*>>().configureEach {
            if (!name.startsWith("ksp") && !name.contains("Test", ignoreCase = true)) {
                dependsOn("kspCommonMainKotlinMetadata")
            }
        }
    }
}
