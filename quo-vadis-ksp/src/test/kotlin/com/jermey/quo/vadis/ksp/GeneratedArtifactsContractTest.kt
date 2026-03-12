package com.jermey.quo.vadis.ksp

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeneratedArtifactsContractTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `ksp generates module-prefixed config and deep link handler in generated package`() {
        val repoRoot = locateRepoRoot()
        val versionName = readVersionName(repoRoot)
        val projectDir = temporaryFolder.newFolder("ksp-contract-${System.nanoTime()}")

        File(projectDir, "settings.gradle.kts").writeText(
            """
            rootProject.name = "ksp-contract-test"

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

            includeBuild("${repoRoot.invariantSeparatorsPath}")
            """.trimIndent() + "\n",
        )

        File(projectDir, "build.gradle.kts").writeText(
            """
            plugins {
                kotlin("jvm") version "2.3.20-RC"
                id("com.google.devtools.ksp") version "2.3.4"
            }

            repositories {
                mavenLocal()
                mavenCentral()
                google()
            }

            dependencies {
                implementation("io.github.jermeyyy:quo-vadis-annotations:$versionName")
                implementation("io.github.jermeyyy:quo-vadis-core:$versionName")
                implementation("org.jetbrains.compose.runtime:runtime:1.11.0-alpha01")
                ksp("io.github.jermeyyy:quo-vadis-ksp:$versionName")
            }

            ksp {
                arg("quoVadis.modulePrefix", "Contract")
            }
            """.trimIndent() + "\n",
        )

        val sourceDir = File(projectDir, "src/main/kotlin/test").apply { mkdirs() }
        File(sourceDir, "ContractDestination.kt").writeText(
            """
            package test

            import com.jermey.quo.vadis.annotations.Argument
            import com.jermey.quo.vadis.annotations.Destination
            import com.jermey.quo.vadis.annotations.Stack
            import com.jermey.quo.vadis.core.navigation.destination.NavDestination

            @Stack(name = "contract", startDestination = ContractDestination.Home::class)
            sealed class ContractDestination : NavDestination {
                @Destination(route = "contract/home")
                data object Home : ContractDestination()

                @Destination(route = "contract/detail/{id}")
                data class Detail(@Argument val id: String) : ContractDestination()
            }
            """.trimIndent() + "\n",
        )

        File(sourceDir, "ContractScreens.kt").writeText(
            """
            package test

            import androidx.compose.runtime.Composable
            import com.jermey.quo.vadis.annotations.Screen
            import com.jermey.quo.vadis.core.navigation.navigator.Navigator

            @Screen(ContractDestination.Home::class)
            @Composable
            fun HomeScreen(navigator: Navigator) {
            }

            @Screen(ContractDestination.Detail::class)
            @Composable
            fun DetailScreen(destination: ContractDestination.Detail, navigator: Navigator) {
            }
            """.trimIndent() + "\n",
        )

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("kspKotlin", "--stacktrace", "--warning-mode=all")
            .forwardOutput()
            .build()

        val task = result.task(":kspKotlin")
        assertNotNull(task, "Expected :kspKotlin to run successfully")
        assertEquals(TaskOutcome.SUCCESS, task.outcome, result.output)

        val generatedDir = File(projectDir, "build/generated/ksp/main/kotlin/com/jermey/quo/vadis/generated")
        val navigationConfig = File(generatedDir, "ContractNavigationConfig.kt")
        val deepLinkHandler = File(generatedDir, "ContractDeepLinkHandler.kt")

        assertTrue(navigationConfig.exists(), "ContractNavigationConfig.kt should be generated in the shared package")
        assertTrue(deepLinkHandler.exists(), "ContractDeepLinkHandler.kt should be generated in the shared package")

        val navigationConfigText = navigationConfig.readText()
        val deepLinkHandlerText = deepLinkHandler.readText()

        assertTrue(
            navigationConfigText.contains("package com.jermey.quo.vadis.generated"),
            "Generated navigation config should use the shared generated package",
        )
        assertTrue(
            navigationConfigText.contains("object ContractNavigationConfig : NavigationConfig"),
            "Generated navigation config should use the module-prefixed contract name and implement NavigationConfig",
        )
        assertTrue(
            deepLinkHandlerText.contains("package com.jermey.quo.vadis.generated"),
            "Generated deep link handler should use the shared generated package",
        )
        assertTrue(
            deepLinkHandlerText.contains("object ContractDeepLinkHandler : DeepLinkRegistry"),
            "Generated deep link handler should use the module-prefixed contract name and implement DeepLinkRegistry",
        )
    }

    private fun locateRepoRoot(): File {
        var current = File(System.getProperty("user.dir")).canonicalFile
        while (true) {
            if (File(current, "settings.gradle.kts").exists() && File(current, "quo-vadis-ksp").exists()) {
                return current
            }
            val parent = current.parentFile ?: break
            current = parent
        }
        error("Could not locate NavPlayground repository root from ${System.getProperty("user.dir")}")
    }

    private fun readVersionName(repoRoot: File): String {
        return File(repoRoot, "gradle.properties")
            .readLines()
            .first { it.startsWith("VERSION_NAME=") }
            .substringAfter('=')
            .trim()
    }
}