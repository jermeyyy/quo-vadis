package com.jermey.quo.vadis.gradle.internal

/**
 * Converts a kebab-case or snake_case string to PascalCase.
 *
 * This matches the behavior of Android's toCamelCase extension for project names,
 * which actually produces PascalCase (first letter capitalized).
 *
 * Examples:
 * - "compose-app" -> "ComposeApp"
 * - "composeApp" -> "ComposeApp"
 * - "feature-one" -> "FeatureOne"
 * - "feature_one" -> "FeatureOne"
 * - "feature1" -> "Feature1"
 */
internal fun String.toCamelCase(): String {
    return split('-', '_', ' ')
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercase() }
        }
}
