package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock

/**
 * Generates `scope()` DSL calls for scope definitions.
 *
 * This generator transforms scope membership data into KotlinPoet [CodeBlock]s
 * representing scope registration DSL calls within the `navigationConfig { }` block.
 *
 * Scopes define which destinations belong to a particular navigation context,
 * enabling scope-aware navigation features like clearing back stacks when
 * switching contexts.
 *
 * ## Input
 *
 * Map of scope key to list of destination [ClassName]s that belong to that scope.
 *
 * ## Output
 *
 * ```kotlin
 * scope("MainTabs",
 *     MainTabs.HomeTab::class,
 *     MainTabs.ExploreTab::class,
 *     MainTabs.ProfileTab::class
 * )
 *
 * scope("ProfileStack",
 *     ProfileDestination.Main::class,
 *     ProfileDestination.Settings::class
 * )
 * ```
 *
 * ## Scope Collection
 *
 * Scopes are typically collected from containers:
 * - **Tabs**: Each tab's destinations belong to the tab container's scope
 * - **Stacks**: Each stack's destinations belong to the stack's scope
 * - **Panes**: Each pane's destinations belong to the pane container's scope
 *
 * @property logger KSP logger for debugging output
 *
 * @see com.jermey.quo.vadis.ksp.generators.dsl.NavigationConfigGenerator.collectScopeData
 */
class ScopeBlockGenerator(
    private val logger: KSPLogger
) {

    /**
     * Generates scope definition blocks.
     *
     * @param scopes Map of scope key to list of destination classes
     * @return CodeBlock containing all scope definitions
     */
    fun generate(scopes: Map<String, List<ClassName>>): CodeBlock {
        if (scopes.isEmpty()) {
            logger.info("ScopeBlockGenerator: No scopes to generate")
            return CodeBlock.of("")
        }

        // Filter out empty scopes
        val nonEmptyScopes = scopes.filterValues { it.isNotEmpty() }
        if (nonEmptyScopes.isEmpty()) {
            logger.info("ScopeBlockGenerator: All scopes are empty, skipping")
            return CodeBlock.of("")
        }

        logger.info("ScopeBlockGenerator: Generating ${nonEmptyScopes.size} scope blocks")

        val builder = CodeBlock.builder()

        nonEmptyScopes.entries.forEachIndexed { index, (scopeKey, destinations) ->
            builder.add(generateScopeBlock(scopeKey, destinations))
            if (index < nonEmptyScopes.size - 1) {
                builder.add("\n")
            }
        }

        return builder.build()
    }

    /**
     * Generates a single `scope()` call.
     *
     * The generated code follows this format:
     * ```kotlin
     * scope("ScopeKey") {
     *     +Destination1::class
     *     +Destination2::class
     *     +Destination3::class
     * }
     * ```
     *
     * @param scopeKey The scope key identifier
     * @param destinations List of destination classes in the scope
     * @return CodeBlock for the scope definition
     */
    private fun generateScopeBlock(scopeKey: String, destinations: List<ClassName>): CodeBlock {
        if (destinations.isEmpty()) {
            return CodeBlock.of("")
        }

        val builder = CodeBlock.builder()
            .beginControlFlow("scope(%S)", scopeKey)

        // Add each destination class reference using unaryPlus operator
        destinations.forEach { className ->
            builder.addStatement("+%T::class", className)
        }

        builder.endControlFlow()

        return builder.build()
    }
}
