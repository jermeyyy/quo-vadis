@file:OptIn(ExperimentalCompilerApi::class)

package com.jermey.quo.vadis.compiler.testing

import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

fun CompilationResult.assertHasError(messageSubstring: String) {
    assert(exitCode != KotlinCompilation.ExitCode.OK) {
        "Expected compilation failure but got OK. Messages:\n$messages"
    }
    assert(messages.contains(messageSubstring, ignoreCase = true)) {
        "Expected error containing '$messageSubstring' but messages were:\n$messages"
    }
}

fun CompilationResult.assertHasWarning(messageSubstring: String) {
    assert(messages.contains(messageSubstring, ignoreCase = true)) {
        "Expected warning containing '$messageSubstring' but messages were:\n$messages"
    }
}

fun CompilationResult.assertNoDiagnostics() {
    assert(exitCode == KotlinCompilation.ExitCode.OK) {
        "Expected clean compilation but got $exitCode. Messages:\n$messages"
    }
    // Check no warning/error lines from QuoVadis diagnostics
    val diagnosticPatterns = listOf(
        "Duplicate route",
        "does not match any placeholder",
        "has no matching @Argument",
        "must have exactly one @PaneItem",
        "has multiple @PaneItem",
        "@Stack must be applied to a sealed",
        "@Destination must be a direct subclass",
        "may not work correctly",
        "which has no @Destination annotation",
        "unexpected parameter types",
    )
    for (pattern in diagnosticPatterns) {
        assert(!messages.contains(pattern, ignoreCase = true)) {
            "Expected no diagnostics but found '$pattern' in messages:\n$messages"
        }
    }
}

fun CompilationResult.assertErrorCount(count: Int) {
    val errorLines = messages.lines().filter { it.contains("error:", ignoreCase = true) }
    assert(errorLines.size == count) {
        "Expected $count errors but found ${errorLines.size}. Error lines:\n${errorLines.joinToString("\n")}\n\nFull messages:\n$messages"
    }
}
