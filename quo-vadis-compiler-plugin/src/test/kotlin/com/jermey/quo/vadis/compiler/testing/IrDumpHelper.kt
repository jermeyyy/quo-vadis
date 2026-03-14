package com.jermey.quo.vadis.compiler.testing

import com.jermey.quo.vadis.compiler.QuoVadisCommandLineProcessor
import com.jermey.quo.vadis.compiler.QuoVadisCompilerPluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import kotlin.test.assertEquals

/**
 * Helper for compiling source with IR dump enabled.
 * Used for golden file regression testing in 5D.3.
 */
@OptIn(ExperimentalCompilerApi::class)
object IrDumpHelper {

    /**
     * Compiles source files with the plugin and returns both the result
     * and the captured IR dump output.
     */
    fun compileWithIrDump(
        vararg sourceFiles: SourceFile,
        modulePrefix: String = "Test",
    ): IrDumpResult {
        val irOutput = StringBuilder()
        val messageStream = java.io.PrintStream(object : java.io.OutputStream() {
            override fun write(b: Int) {
                irOutput.append(b.toChar())
            }
        })

        val result = KotlinCompilation().apply {
            sources = sourceFiles.toList()
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
            messageOutputStream = messageStream
            kotlincArguments = listOf("-Xverify-ir=error")
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, result.messages)

        return IrDumpResult(
            compilationResult = result,
            irOutput = irOutput.toString(),
        )
    }

    data class IrDumpResult(
        val compilationResult: JvmCompilationResult,
        val irOutput: String,
    )
}
