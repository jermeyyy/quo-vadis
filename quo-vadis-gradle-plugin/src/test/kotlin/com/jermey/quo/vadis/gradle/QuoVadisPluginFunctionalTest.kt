package com.jermey.quo.vadis.gradle

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuoVadisPluginFunctionalTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `compiler backend property enables compiler backend`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(),
            gradleProperties = """
                quoVadis.backend=compiler
            """.trimIndent(),
        )

        val result = runHelp(projectDir)

        assertHelpSucceeded(result)
        assertOutputContains(
            result,
            "Quo Vadis: using experimental compiler backend.",
        )
    }

    @Test
    fun `backend property wins over deprecated alias when they conflict`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(),
            gradleProperties = """
                quoVadis.backend=compiler
                quoVadis.useCompilerPlugin=false
            """.trimIndent(),
        )

        val result = runHelp(projectDir)

        assertHelpSucceeded(result)
        assertOutputContains(
            result,
            "Quo Vadis: Both 'backend' and deprecated 'useCompilerPlugin' are configured. The backend setting takes precedence.",
        )
        assertOutputContains(
            result,
            "Quo Vadis: using experimental compiler backend.",
        )
    }

    @Test
    fun `explicit ksp backend succeeds with ksp plugin applied`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(),
            gradleProperties = """
                quoVadis.backend=ksp
            """.trimIndent(),
        )

        val result = runHelp(projectDir)

        assertHelpSucceeded(result)
        assertOutputDoesNotContain(
            result,
            "Quo Vadis: using experimental compiler backend.",
        )
        assertOutputDoesNotContain(
            result,
            "Quo Vadis: 'useCompilerPlugin' is deprecated.",
        )
    }

    @Test
    fun `deprecated alias true selects compiler backend and warns`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(),
            gradleProperties = """
                quoVadis.useCompilerPlugin=true
            """.trimIndent(),
        )

        val result = runHelp(projectDir)

        assertHelpSucceeded(result)
        assertOutputContains(
            result,
            "Quo Vadis: 'useCompilerPlugin' is deprecated.",
        )
        assertOutputContains(
            result,
            "Quo Vadis: using experimental compiler backend.",
        )
    }

    @Test
    fun `deprecated alias false selects ksp backend and warns`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(),
            gradleProperties = """
                quoVadis.useCompilerPlugin=false
            """.trimIndent(),
        )

        val result = runHelp(projectDir)

        assertHelpSucceeded(result)
        assertOutputContains(
            result,
            "Quo Vadis: 'useCompilerPlugin' is deprecated.",
        )
        assertOutputDoesNotContain(
            result,
            "Quo Vadis: using experimental compiler backend.",
        )
    }

    @Test
    fun `compiler backend fails when useLocalKsp is enabled`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(
                quoVadisBlock = """
                    quoVadis {
                        useLocalKsp.set(true)
                    }
                """.trimIndent(),
            ),
            gradleProperties = """
                quoVadis.backend=compiler
            """.trimIndent(),
        )

        val result = runHelp(projectDir, expectFailure = true)

        assertOutputContains(
            result,
            "Quo Vadis compiler backend does not support useLocalKsp=true.",
        )
    }

    @Test
    fun `ksp backend fails when ksp plugin is not applied`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(applyKspPlugin = false),
        )

        val result = runHelp(projectDir, expectFailure = true)

        assertOutputContains(
            result,
            "Quo Vadis KSP backend requires the KSP plugin.",
        )
    }

    @Test
    fun `compiler backend fails when quo vadis ksp dependency is present`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(
                extraBuildScript = """
                    dependencies {
                        add("kspCommonMainMetadata", "io.github.jermeyyy:quo-vadis-ksp:0.0.1")
                    }
                """.trimIndent(),
            ),
            gradleProperties = """
                quoVadis.backend=compiler
            """.trimIndent(),
        )

        val result = runHelp(projectDir, expectFailure = true)

        assertOutputContains(
            result,
            "Quo Vadis compiler backend cannot run with Quo Vadis KSP processor dependencies present.",
        )
    }

    @Test
    fun `invalid backend property fails with clear error`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(),
            gradleProperties = """
                quoVadis.backend=unknown
            """.trimIndent(),
        )

        val result = runHelp(projectDir, expectFailure = true)

        assertOutputContains(
            result,
            "Unknown Quo Vadis backend 'unknown'. Expected one of: ksp, compiler.",
        )
    }

    @Test
    fun `invalid deprecated alias fails with clear error`() {
        val projectDir = writeProject(
            buildScript = baseBuildScript(),
            gradleProperties = """
                quoVadis.useCompilerPlugin=maybe
            """.trimIndent(),
        )

        val result = runHelp(projectDir, expectFailure = true)

        assertOutputContains(
            result,
            "Invalid value 'maybe' for quoVadis.useCompilerPlugin. Expected true or false.",
        )
    }

    private fun writeProject(
        buildScript: String,
        gradleProperties: String = "",
        settingsScript: String = defaultSettingsScript(),
    ): File {
        val projectDir = temporaryFolder.newFolder("project-${System.nanoTime()}")
        File(projectDir, "settings.gradle.kts").writeText(settingsScript)
        File(projectDir, "build.gradle.kts").writeText(buildScript)
        if (gradleProperties.isNotBlank()) {
            File(projectDir, "gradle.properties").writeText(gradleProperties + "\n")
        }
        return projectDir
    }

    private fun runHelp(projectDir: File, expectFailure: Boolean = false): BuildResult {
        val runner = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("help", "--stacktrace", "--warning-mode=all")

        return if (expectFailure) runner.buildAndFail() else runner.build()
    }

    private fun assertHelpSucceeded(result: BuildResult) {
        val helpTask = result.task(":help")
        assertNotNull(helpTask, "Expected :help task to run successfully. Output:\n${result.output}")
        assertEquals(TaskOutcome.SUCCESS, helpTask.outcome, result.output)
    }

    private fun assertOutputContains(result: BuildResult, expected: String) {
        assertTrue(
            result.output.contains(expected),
            "Expected output to contain '$expected'. Actual output:\n${result.output}",
        )
    }

    private fun assertOutputDoesNotContain(result: BuildResult, unexpected: String) {
        assertTrue(
            !result.output.contains(unexpected),
            "Did not expect output to contain '$unexpected'. Actual output:\n${result.output}",
        )
    }

    private fun defaultSettingsScript(): String =
        """
        rootProject.name = "test-project"

        pluginManagement {
            repositories {
                gradlePluginPortal()
                mavenCentral()
                google()
                mavenLocal()
            }
        }

        dependencyResolutionManagement {
            repositories {
                mavenLocal()
                mavenCentral()
                google()
            }
        }
        """.trimIndent()

    private fun baseBuildScript(
        applyKspPlugin: Boolean = true,
        quoVadisBlock: String = "",
        extraBuildScript: String = "",
    ): String {
        val kspPluginLine = if (applyKspPlugin) {
            "id(\"com.google.devtools.ksp\") version \"2.3.4\""
        } else {
            ""
        }
        val quoVadisConfig = if (quoVadisBlock.isBlank()) {
            ""
        } else {
            "\n$quoVadisBlock\n"
        }
        val trailingBuildScript = if (extraBuildScript.isBlank()) {
            ""
        } else {
            "\n$extraBuildScript\n"
        }

        return """
            plugins {
                kotlin("multiplatform") version "2.3.20-RC"
                id("io.github.jermeyyy.quo-vadis")
                $kspPluginLine
            }

            repositories {
                mavenLocal()
                mavenCentral()
                google()
            }

            kotlin {
                jvm()
            }
$quoVadisConfig$trailingBuildScript
        """.trimIndent()
    }
}