package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.TransitionInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Generates `transition<T>` DSL calls for transition registrations.
 *
 * This generator transforms [TransitionInfo] data into KotlinPoet [CodeBlock]s
 * representing transition registration DSL calls within the `navigationConfig { }` block.
 *
 * ## Input
 *
 * List of [TransitionInfo] from TransitionExtractor containing:
 * - Destination class reference
 * - Transition type (preset name or "Custom")
 * - Custom transition class (if using custom transition)
 *
 * ## Output
 *
 * ```kotlin
 * transition<HomeDestination.Detail>(NavTransition.SlideHorizontal)
 * transition<ProfileDestination>(NavTransition.Fade)
 * transition<CustomScreen>(CustomTransitionProvider().transition)
 * ```
 *
 * ## Transition Types
 *
 * The generator handles several transition type patterns:
 *
 * ### Preset Transitions
 * Standard transitions available in `NavTransition`:
 * - `SlideHorizontal` - Horizontal slide animation
 * - `SlideVertical` - Vertical slide animation
 * - `Fade` - Fade in/out animation
 * - `None` - No animation
 * - `Default` - Platform default animation
 *
 * ### Custom Transitions
 * When `transitionType` is "Custom" and `customTransitionClass` is specified,
 * generates a call to instantiate the custom transition provider.
 *
 * @property logger KSP logger for debugging output
 */
class TransitionBlockGenerator(
    private val logger: KSPLogger
) {

    /**
     * Generates transition registration blocks.
     *
     * @param transitions List of transition info from extractor
     * @return CodeBlock containing all transition definitions
     */
    fun generate(transitions: List<TransitionInfo>): CodeBlock {
        if (transitions.isEmpty()) {
            logger.info("TransitionBlockGenerator: No transitions to generate")
            return CodeBlock.of("")
        }

        logger.info("TransitionBlockGenerator: Generating ${transitions.size} transition blocks")

        val builder = CodeBlock.builder()

        transitions.forEachIndexed { index, transition ->
            builder.add(generateTransitionBlock(transition))
            if (index < transitions.size - 1) {
                builder.add("\n")
            }
        }

        return builder.build()
    }

    /**
     * Generates a single `transition<T>` call.
     *
     * @param transition The transition info to generate code for
     * @return CodeBlock for the transition registration
     */
    private fun generateTransitionBlock(transition: TransitionInfo): CodeBlock {
        val destClass = transition.destinationClass.toClassName()
        val transitionExpr = buildTransitionExpression(transition)

        return CodeBlock.of("transition<%T>(%L)\n", destClass, transitionExpr)
    }

    /**
     * Builds the transition expression from TransitionInfo.
     *
     * Converts the transition type to the appropriate expression:
     * - Preset types → `NavTransition.{TypeName}`
     * - Custom type → `{CustomClass}().transition`
     *
     * @param transition The transition info to build expression for
     * @return String representing the transition expression
     */
    private fun buildTransitionExpression(transition: TransitionInfo): String {
        return when {
            // Custom transition with a specified class
            transition.transitionType == "Custom" && !transition.customTransitionClass.isNullOrEmpty() -> {
                "${transition.customTransitionClass}().transition"
            }
            // Preset transition - use NavTransition.{Type}
            transition.transitionType.isNotEmpty() -> {
                "NavTransition.${transition.transitionType}"
            }
            // Fallback to default
            else -> {
                logger.warn("TransitionBlockGenerator: No transition type specified for " +
                    "${transition.destinationQualifiedName}, using Default")
                "NavTransition.Default"
            }
        }
    }

    companion object {
        /**
         * Set of known preset transition types.
         * Used for validation if needed.
         */
        @Suppress("unused")
        private val PRESET_TRANSITIONS = setOf(
            "SlideHorizontal",
            "SlideVertical",
            "Fade",
            "None",
            "Default",
            "SharedElement"
        )
    }
}
