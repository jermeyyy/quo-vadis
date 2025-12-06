package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.jermey.quo.vadis.ksp.models.StackInfo

/**
 * Extracts @Stack annotations into StackInfo models.
 *
 * A stack represents a navigation graph with a linear back stack.
 * This extractor handles:
 * - Stack name extraction from annotation
 * - Start destination resolution from subclasses
 * - Delegation to DestinationExtractor for subclass extraction
 *
 * @property destinationExtractor Extractor for @Destination subclasses
 * @property logger KSP logger for error/warning output
 */
class StackExtractor(
    private val destinationExtractor: DestinationExtractor,
    private val logger: KSPLogger
) {

    /**
     * Extract StackInfo from a class declaration.
     *
     * @param classDeclaration The sealed class annotated with @Stack
     * @return StackInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): StackInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Stack"
        } ?: return null

        val name = annotation.arguments.find {
            it.name?.asString() == "name"
        }?.value as? String ?: return null

        val startDestination = annotation.arguments.find {
            it.name?.asString() == "startDestination"
        }?.value as? String ?: ""

        val destinations = destinationExtractor.extractFromContainer(classDeclaration)

        // Resolve start destination by matching class name
        val resolvedStart = destinations.find {
            it.className == startDestination
        } ?: destinations.firstOrNull()

        if (resolvedStart == null && destinations.isNotEmpty()) {
            logger.warn("Start destination '$startDestination' not found in ${classDeclaration.simpleName.asString()}")
        }

        return StackInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            startDestination = startDestination,
            destinations = destinations,
            resolvedStartDestination = resolvedStart
        )
    }

    /**
     * Extract all @Stack-annotated classes from the resolver.
     *
     * @param resolver KSP resolver to query for symbols
     * @return List of StackInfo for all @Stack-annotated classes
     */
    fun extractAll(resolver: Resolver): List<StackInfo> {
        return resolver.getSymbolsWithAnnotation("com.jermey.quo.vadis.annotations.Stack")
            .filterIsInstance<KSClassDeclaration>()
            .mapNotNull { extract(it) }
            .toList()
    }
}
