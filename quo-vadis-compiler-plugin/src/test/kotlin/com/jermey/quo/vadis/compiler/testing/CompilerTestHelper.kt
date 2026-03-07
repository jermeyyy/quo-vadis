package com.jermey.quo.vadis.compiler.testing

import com.jermey.quo.vadis.compiler.QuoVadisCommandLineProcessor
import com.jermey.quo.vadis.compiler.QuoVadisCompilerPluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import java.io.File
import kotlin.test.assertEquals

@OptIn(ExperimentalCompilerApi::class)
object CompilerTestHelper {

    fun compile(
        vararg sourceFiles: SourceFile,
        modulePrefix: String = "Test",
        classpaths: List<File> = emptyList(),
        expectSuccess: Boolean = true,
    ): JvmCompilationResult {
        return KotlinCompilation().apply {
            sources = sourceFiles.toList()
            this.classpaths = classpaths
            compilerPluginRegistrars = listOf(QuoVadisCompilerPluginRegistrar())
            commandLineProcessors = listOf(QuoVadisCommandLineProcessor())
            pluginOptions = listOf(
                PluginOption(
                    QuoVadisCommandLineProcessor.PLUGIN_ID,
                    "modulePrefix",
                    modulePrefix,
                ),
            )
            inheritClassPath = true
            messageOutputStream = System.out
            kotlincArguments = listOf("-Xverify-ir=error")
        }.compile().also { result ->
            if (expectSuccess) {
                assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)
            }
        }
    }
}
