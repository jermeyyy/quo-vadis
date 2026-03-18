package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.ModalInfo
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Generates `modal<T>()` and `modalContainer("name")` DSL calls for modal registrations.
 *
 * This generator transforms [ModalInfo] data into KotlinPoet [CodeBlock]s
 * representing modal registration DSL calls within the `navigationConfig { }` block.
 *
 * ## Input
 *
 * List of [ModalInfo] from ModalExtractor containing:
 * - Annotated class reference
 * - Whether it is a destination or container
 * - Container name (for containers)
 *
 * ## Output
 *
 * ```kotlin
 * modal<ConfirmDialog>()
 * modal<PhotoPicker>()
 * modalContainer("ModalTabs")
 * ```
 *
 * @property logger KSP logger for debugging output
 */
class ModalBlockGenerator(
    private val logger: KSPLogger
) {

    /**
     * Generates modal registration blocks.
     *
     * @param modals List of modal info from extractor
     * @return CodeBlock containing all modal definitions
     */
    fun generate(modals: List<ModalInfo>): CodeBlock {
        if (modals.isEmpty()) {
            logger.info("ModalBlockGenerator: No modals to generate")
            return CodeBlock.of("")
        }

        logger.info("ModalBlockGenerator: Generating ${modals.size} modal blocks")

        val builder = CodeBlock.builder()

        modals.forEachIndexed { index, modal ->
            builder.add(generateModalBlock(modal))
            if (index < modals.size - 1) {
                builder.add("\n")
            }
        }

        return builder.build()
    }

    /**
     * Generates a single modal registration call.
     *
     * @param modal The modal info to generate code for
     * @return CodeBlock for the modal registration
     */
    private fun generateModalBlock(modal: ModalInfo): CodeBlock {
        return if (modal.isDestination) {
            val destClass = modal.annotatedClass.toClassName()
            CodeBlock.of("modal<%T>()\n", destClass)
        } else {
            val containerName = modal.containerName
                ?: modal.annotatedClass.simpleName.asString()
            CodeBlock.of("modalContainer(%S)\n", containerName)
        }
    }
}
