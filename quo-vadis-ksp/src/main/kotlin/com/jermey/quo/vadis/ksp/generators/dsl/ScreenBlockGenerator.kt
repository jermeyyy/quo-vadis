package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.ScreenInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Result of screen block generation containing code and imports.
 *
 * @property codeBlock The generated screen registration code
 * @property imports Set of fully qualified function names that need to be imported
 */
data class ScreenBlockResult(
    val codeBlock: CodeBlock,
    val imports: Set<String>
)

/**
 * Generates `screen<T>` DSL blocks for screen registrations.
 *
 * This generator transforms [ScreenInfo] data into KotlinPoet [CodeBlock]s
 * that represent screen registration DSL calls within the `navigationConfig { }` block.
 *
 * ## Input
 *
 * List of [ScreenInfo] from ScreenExtractor containing:
 * - Destination class reference
 * - Screen composable function name
 * - Parameter information (destination, navigator, animation scopes)
 *
 * ## Output
 *
 * CodeBlock containing all screen registrations in DSL format:
 *
 * ```kotlin
 * screen<HomeDestination.Feed> { destination, navigator, sharedTransitionScope, animatedVisibilityScope ->
 *     FeedScreen(navigator = navigator)
 * }
 *
 * screen<HomeDestination.Detail> { destination, navigator, sharedTransitionScope, animatedVisibilityScope ->
 *     DetailScreen(destination = destination, navigator = navigator)
 * }
 * ```
 *
 * ## Parameter Handling
 *
 * The generator inspects [ScreenInfo] to determine which parameters the screen
 * composable function requires:
 * - `navigator` - Always passed if needed
 * - `destination` - Passed when `hasDestinationParam` is true
 * - `sharedTransitionScope` - Passed when `hasSharedTransitionScope` is true
 * - `animatedVisibilityScope` - Passed when `hasAnimatedVisibilityScope` is true
 *
 * @property logger KSP logger for debugging output
 */
class ScreenBlockGenerator(
    private val logger: KSPLogger
) {

    /**
     * Generates screen registration blocks for all screens.
     *
     * @param screens List of screen information from extractor
     * @return CodeBlock containing all `screen<T> { }` registrations
     */
    fun generate(screens: List<ScreenInfo>): CodeBlock {
        return generateWithImports(screens).codeBlock
    }

    /**
     * Generates screen registration blocks with import information.
     *
     * @param screens List of screen information from extractor
     * @return [ScreenBlockResult] containing code and required imports
     */
    fun generateWithImports(screens: List<ScreenInfo>): ScreenBlockResult {
        if (screens.isEmpty()) {
            logger.info("ScreenBlockGenerator: No screens to generate")
            return ScreenBlockResult(CodeBlock.of(""), emptySet())
        }

        logger.info("ScreenBlockGenerator: Generating ${screens.size} screen blocks")

        val builder = CodeBlock.builder()
        val imports = mutableSetOf<String>()

        screens.forEachIndexed { index, screen ->
            builder.add(generateScreenBlock(screen))
            if (index < screens.size - 1) {
                builder.add("\n")
            }
            // Collect import for the screen function
            val qualifiedName = "${screen.packageName}.${screen.functionName}"
            imports.add(qualifiedName)
        }

        return ScreenBlockResult(builder.build(), imports)
    }

    /**
     * Generates a single `screen<T>` block for a screen.
     *
     * @param screen The screen info to generate code for
     * @return CodeBlock for the screen registration
     */
    private fun generateScreenBlock(screen: ScreenInfo): CodeBlock {
        val destinationClass = screen.destinationClass.toClassName()
        val functionCall = buildFunctionCall(screen)

        return CodeBlock.builder()
            .beginControlFlow(
                "screen<%T> { destination, navigator, sharedTransitionScope, animatedVisibilityScope ->",
                destinationClass
            )
            .addStatement(functionCall)
            .endControlFlow()
            .build()
    }

    /**
     * Builds the composable function call with appropriate parameters.
     *
     * Determines which parameters to pass based on [ScreenInfo] flags:
     * - `hasDestinationParam` -> `destination = destination`
     * - Always includes `navigator = navigator` for navigation capability
     * - `hasSharedTransitionScope` -> `sharedTransitionScope = sharedTransitionScope`
     * - `hasAnimatedVisibilityScope` -> `animatedVisibilityScope = animatedVisibilityScope`
     *
     * @param screen The screen info to build the call for
     * @return String representing the function call
     */
    private fun buildFunctionCall(screen: ScreenInfo): String {
        val params = mutableListOf<String>()

        // Add destination parameter if the screen function accepts it
        if (screen.hasDestinationParam) {
            params.add("destination = destination")
        }

        // Add navigator parameter (most screens need navigation capability)
        params.add("navigator = navigator")

        // Add shared transition scope if needed
        if (screen.hasSharedTransitionScope) {
            params.add("sharedTransitionScope = sharedTransitionScope")
        }

        // Add animated visibility scope if needed
        if (screen.hasAnimatedVisibilityScope) {
            params.add("animatedVisibilityScope = animatedVisibilityScope")
        }

        return "${screen.functionName}(${params.joinToString(", ")})"
    }
}
