package com.jermey.quo.vadis.gradle

import com.google.devtools.ksp.gradle.KspExtension
import com.jermey.quo.vadis.gradle.internal.toCamelCase
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Gradle plugin for Quo Vadis navigation library KSP configuration.
 *
 * This plugin automatically:
 * - Adds the kspCommonMainMetadata dependency for quo-vadis-ksp
 * - Configures the KSP argument `quoVadis.modulePrefix` with a sensible default
 * - Registers the generated source directory
 * - Sets up proper task dependencies for KMP
 *
 * Note: This plugin requires both the KSP plugin and Kotlin Multiplatform plugin
 * to be applied before or alongside this plugin.
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     alias(libs.plugins.kotlinMultiplatform)
 *     alias(libs.plugins.ksp)
 *     alias(libs.plugins.quoVadis)
 * }
 *
 * // Optional configuration:
 * quoVadis {
 *     modulePrefix = "customPrefix"
 *     useLocalKsp = true // for development
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

        // Configure after both KMP and KSP plugins are applied
        project.afterEvaluate {
            // Check required plugins
            if (!project.plugins.hasPlugin("com.google.devtools.ksp")) {
                throw GradleException(
                    "Quo Vadis plugin requires the KSP plugin. " +
                        "Please apply 'com.google.devtools.ksp' plugin before 'io.github.jermeyyy.quo-vadis'."
                )
            }
            if (!project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
                throw GradleException(
                    "Quo Vadis plugin requires the Kotlin Multiplatform plugin. " +
                        "Please apply 'org.jetbrains.kotlin.multiplatform' plugin before 'io.github.jermeyyy.quo-vadis'."
                )
            }

            configureKsp(project, extension)
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
