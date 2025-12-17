package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.jermey.quo.vadis.ksp.models.TransitionInfo

/**
 * Extracts @Transition annotations into [TransitionInfo] models.
 *
 * Scans for classes annotated with `@Transition` and extracts their
 * transition configuration. This extractor handles:
 * - Destination class identification
 * - TransitionType enum value extraction
 * - Custom transition class resolution (for TransitionType.Custom)
 * - Validation of custom transition configuration
 *
 * ## Validation Rules
 *
 * - If `type` is `Custom`, then `customTransition` must not be `Unit::class`
 * - If `type` is not `Custom`, `customTransition` is ignored
 *
 * ## Example Usage
 *
 * ```kotlin
 * val extractor = TransitionExtractor(logger)
 * val transitions = extractor.extractAll(resolver)
 *
 * transitions.forEach { info ->
 *     println("${info.destinationQualifiedName} -> ${info.transitionType}")
 * }
 * ```
 *
 * @property logger KSP logger for error/warning output
 */
class TransitionExtractor(
    private val logger: KSPLogger
) {

    private companion object {
        private const val TRANSITION_ANNOTATION = "com.jermey.quo.vadis.annotations.Transition"
        private const val TRANSITION_TYPE_CUSTOM = "Custom"
        private const val UNIT_CLASS_NAME = "kotlin.Unit"
    }

    /**
     * Extract TransitionInfo from a class declaration.
     *
     * @param classDeclaration The class annotated with @Transition
     * @return TransitionInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): TransitionInfo? {
        val annotation = classDeclaration.annotations.find {
            it.annotationType.resolve().declaration.qualifiedName?.asString() == TRANSITION_ANNOTATION
        } ?: return null

        val containingFile = classDeclaration.containingFile
        if (containingFile == null) {
            logger.warn("Could not determine containing file for @Transition on ${classDeclaration.simpleName.asString()}")
            return null
        }

        val qualifiedName = classDeclaration.qualifiedName?.asString()
        if (qualifiedName == null) {
            logger.warn("Could not resolve qualified name for @Transition on ${classDeclaration.simpleName.asString()}")
            return null
        }

        // Extract 'type' argument (TransitionType enum)
        val typeArgument = annotation.arguments.find { it.name?.asString() == "type" }
        val transitionType = extractEnumValue(typeArgument?.value) ?: "SlideHorizontal"

        // Extract 'customTransition' argument (KClass<*>)
        val customTransitionArgument = annotation.arguments.find { it.name?.asString() == "customTransition" }
        val customTransitionType = customTransitionArgument?.value as? KSType
        val customTransitionClassName = customTransitionType?.declaration?.qualifiedName?.asString()

        // Determine effective custom class (null if Unit or not Custom type)
        val effectiveCustomClass = when {
            transitionType != TRANSITION_TYPE_CUSTOM -> null
            customTransitionClassName == null || customTransitionClassName == UNIT_CLASS_NAME -> {
                logger.error(
                    "@Transition(type = Custom) on $qualifiedName requires a non-Unit customTransition class"
                )
                return null
            }
            else -> customTransitionClassName
        }

        return TransitionInfo(
            destinationClass = classDeclaration,
            destinationQualifiedName = qualifiedName,
            transitionType = transitionType,
            customTransitionClass = effectiveCustomClass,
            containingFile = containingFile
        )
    }

    /**
     * Extract all @Transition-annotated classes from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of TransitionInfo for all @Transition-annotated classes
     */
    fun extractAll(resolver: Resolver): List<TransitionInfo> {
        return resolver.getSymbolsWithAnnotation(TRANSITION_ANNOTATION)
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }

    /**
     * Extract the simple name from an enum value.
     *
     * KSP represents enum values as KSType, and we need to extract the simple name
     * of the enum constant (e.g., "SlideHorizontal" from TransitionType.SlideHorizontal).
     *
     * @param value The annotation argument value (expected to be a KSType for enum)
     * @return The simple name of the enum constant, or null if extraction fails
     */
    private fun extractEnumValue(value: Any?): String? {
        // KSP represents enum values in different ways depending on version
        // Try to extract the enum constant name
        return when (value) {
            is KSType -> {
                // For enum entries, the declaration's simple name is the constant name
                value.declaration.simpleName.asString()
            }
            else -> {
                // Fallback: try toString and extract the last part
                value?.toString()?.substringAfterLast(".")
            }
        }
    }
}
