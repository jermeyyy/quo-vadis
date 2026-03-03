package com.jermey.quo.vadis.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Kotlin compiler plugin support for Quo Vadis.
 *
 * This plugin injects the Quo Vadis K2 compiler plugin into the Kotlin compilation
 * and passes configuration options (modulePrefix) to it.
 *
 * Applied conditionally by [QuoVadisPlugin] when `useCompilerPlugin = true`.
 */
class QuoVadisCompilerSubplugin : KotlinCompilerPluginSupportPlugin {

    override fun getCompilerPluginId(): String = "com.jermey.quo-vadis"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-compiler-plugin",
        version = BuildConfig.VERSION
    )

    override fun getPluginArtifactForNative(): SubpluginArtifact = SubpluginArtifact(
        groupId = "io.github.jermeyyy",
        artifactId = "quo-vadis-compiler-plugin-native",
        version = BuildConfig.VERSION
    )

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.findByType(QuoVadisExtension::class.java)
            ?: return false
        return extension.useCompilerPlugin.get()
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(QuoVadisExtension::class.java)
        return project.provider {
            listOf(SubpluginOption("modulePrefix", extension.modulePrefix.get()))
        }
    }

    override fun apply(target: Project) {
        // No-op — configuration is handled by QuoVadisPlugin
    }
}
