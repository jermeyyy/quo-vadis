package com.jermey.quo.vadis.compiler.testing

import java.io.File

/**
 * Compares compiler output against golden (expected) files.
 *
 * Golden files are stored in `src/test/resources/ir-golden/`.
 * Set system property `-DupdateGoldenFiles=true` to overwrite golden files with actual output.
 */
object GoldenFileComparator {

    private val goldenDir: File
        get() {
            // Look for the golden dir relative to the project
            val projectDir = File(System.getProperty("user.dir"))
            return File(projectDir, "src/test/resources/ir-golden").also {
                it.mkdirs()
            }
        }

    private val updateMode: Boolean
        get() = System.getProperty("updateGoldenFiles")?.toBoolean() == true

    /**
     * Compares [actual] output against the golden file named [testName].txt.
     *
     * If the golden file doesn't exist or updateMode is enabled, writes actual as the new golden file.
     * Otherwise, asserts the content matches.
     */
    fun assertMatchesGolden(testName: String, actual: String) {
        val goldenFile = File(goldenDir, "$testName.txt")

        if (updateMode || !goldenFile.exists()) {
            goldenFile.writeText(actual)
            if (!updateMode) {
                println("Golden file created: ${goldenFile.absolutePath}")
                println("Re-run the test to verify against the golden file.")
            }
            return
        }

        val expected = goldenFile.readText()
        if (expected != actual) {
            val diff = buildDiff(expected, actual)
            throw AssertionError(
                "Golden file mismatch for '$testName'.\n" +
                    "Golden file: ${goldenFile.absolutePath}\n" +
                    "To update, run with -DupdateGoldenFiles=true\n\n" +
                    "Diff:\n$diff"
            )
        }
    }

    private fun buildDiff(expected: String, actual: String): String {
        val expectedLines = expected.lines()
        val actualLines = actual.lines()
        val sb = StringBuilder()
        val maxLines = maxOf(expectedLines.size, actualLines.size)

        for (i in 0 until maxLines) {
            val exp = expectedLines.getOrNull(i)
            val act = actualLines.getOrNull(i)
            when {
                exp == act -> sb.appendLine("  $exp")
                exp == null -> sb.appendLine("+ $act")
                act == null -> sb.appendLine("- $exp")
                else -> {
                    sb.appendLine("- $exp")
                    sb.appendLine("+ $act")
                }
            }
        }
        return sb.toString()
    }
}
