package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.jermey.quo.vadis.ksp.models.ModalInfo

/**
 * Extracts @Modal annotations into [ModalInfo] models.
 *
 * Scans for classes annotated with `@Modal` and extracts their
 * modal presentation metadata. This extractor handles:
 * - Destination class identification (classes also annotated with @Destination)
 * - Container identification (classes also annotated with @Tabs, @Stack, or @Pane)
 * - Container name extraction from the `name` parameter
 * - Validation that @Modal is only applied to qualifying classes
 *
 * ## Validation Rules
 *
 * - The annotated class must also have `@Destination`, `@Tabs`, `@Stack`, or `@Pane`
 * - Classes without a qualifying annotation trigger an error
 *
 * ## Example Usage
 *
 * ```kotlin
 * val extractor = ModalExtractor(logger)
 * val modals = extractor.extractAll(resolver)
 *
 * modals.forEach { info ->
 *     if (info.isDestination) println("modal destination: ${info.qualifiedName}")
 *     else println("modal container: ${info.containerName}")
 * }
 * ```
 *
 * @property logger KSP logger for error/warning output
 */
class ModalExtractor(
    private val logger: KSPLogger
) {

    private companion object {
        private const val MODAL_ANNOTATION = "com.jermey.quo.vadis.annotations.Modal"
        private const val DESTINATION_ANNOTATION = "com.jermey.quo.vadis.annotations.Destination"
        private const val TABS_ANNOTATION = "com.jermey.quo.vadis.annotations.Tabs"
        private const val STACK_ANNOTATION = "com.jermey.quo.vadis.annotations.Stack"
        private const val PANE_ANNOTATION = "com.jermey.quo.vadis.annotations.Pane"
    }

    /**
     * Extract ModalInfo from a class declaration.
     *
     * @param classDeclaration The class annotated with @Modal
     * @return ModalInfo or null if extraction fails
     */
    @Suppress("ReturnCount")
    fun extract(classDeclaration: KSClassDeclaration): ModalInfo? {
        val containingFile = classDeclaration.containingFile
        if (containingFile == null) {
            logger.warn("Could not determine containing file for @Modal on ${classDeclaration.simpleName.asString()}")
            return null
        }

        val qualifiedName = classDeclaration.qualifiedName?.asString()
        if (qualifiedName == null) {
            logger.warn("Could not resolve qualified name for @Modal on ${classDeclaration.simpleName.asString()}")
            return null
        }

        val annotationNames = classDeclaration.annotations.map {
            it.annotationType.resolve().declaration.qualifiedName?.asString()
        }.toSet()

        val hasDestination = DESTINATION_ANNOTATION in annotationNames
        val containerAnnotation = findContainerAnnotation(annotationNames)

        if (!hasDestination && containerAnnotation == null) {
            logger.error(
                "@Modal on $qualifiedName requires @Destination, @Tabs, @Stack, or @Pane annotation"
            )
            return null
        }

        val isDestination = hasDestination
        val containerName = containerAnnotation?.let { extractContainerName(classDeclaration, it) }

        return ModalInfo(
            annotatedClass = classDeclaration,
            qualifiedName = qualifiedName,
            isDestination = isDestination,
            containerName = containerName,
            containingFile = containingFile
        )
    }

    /**
     * Extract all @Modal-annotated classes from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of ModalInfo for all @Modal-annotated classes
     */
    fun extractAll(resolver: Resolver): List<ModalInfo> {
        return resolver.getSymbolsWithAnnotation(MODAL_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }

    /**
     * Finds the container annotation on the class, if any.
     *
     * @param annotationNames Set of annotation qualified names on the class
     * @return The container annotation qualified name, or null if not a container
     */
    private fun findContainerAnnotation(
        annotationNames: Set<String?>
    ): String? {
        return when {
            TABS_ANNOTATION in annotationNames -> TABS_ANNOTATION
            STACK_ANNOTATION in annotationNames -> STACK_ANNOTATION
            PANE_ANNOTATION in annotationNames -> PANE_ANNOTATION
            else -> null
        }
    }

    /**
     * Extracts the container name from the `name` parameter of @Tabs, @Stack, or @Pane.
     *
     * @param classDeclaration The class with the container annotation
     * @param containerAnnotationName The qualified name of the container annotation
     * @return The container name, or the class simple name as fallback
     */
    private fun extractContainerName(
        classDeclaration: KSClassDeclaration,
        containerAnnotationName: String
    ): String {
        val annotation = classDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == containerAnnotationName
        }

        val nameArg = annotation?.arguments?.find { it.name?.asString() == "name" }
        val name = nameArg?.value as? String

        return if (!name.isNullOrEmpty()) {
            name
        } else {
            classDeclaration.simpleName.asString()
        }
    }
}
