package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
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
     * Supports both type-safe (KClass) and string-based start destination resolution:
     * 1. If `startDestinationClass` is specified and not Unit::class, use it (type-safe)
     * 2. Otherwise, if `startDestination` string is not empty, match by class name
     * 3. Otherwise, use the first destination in declaration order
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

        // Extract legacy string-based startDestination
        val startDestination = annotation.arguments.find {
            it.name?.asString() == "startDestination"
        }?.value as? String ?: ""

        // Extract type-safe startDestinationClass (new)
        val startDestinationClassType = annotation.arguments.find {
            it.name?.asString() == "startDestinationClass"
        }?.value as? KSType

        val startDestinationClassDecl = startDestinationClassType?.declaration as? KSClassDeclaration
        val startDestinationClassQualified = startDestinationClassDecl?.qualifiedName?.asString()

        // Resolve to null if Unit::class (default) or not specified
        val startDestinationClass = if (
            startDestinationClassQualified == "kotlin.Unit" ||
            startDestinationClassQualified == null
        ) {
            null
        } else {
            startDestinationClassDecl
        }

        val destinations = destinationExtractor.extractFromContainer(classDeclaration)

        // Resolve start destination with priority:
        // 1. Type-safe KClass (startDestinationClass)
        // 2. String-based (startDestination)
        // 3. First destination in declaration order
        val resolvedStart = resolveStartDestination(
            startDestinationClass = startDestinationClass,
            startDestinationString = startDestination,
            destinations = destinations,
            containerClassName = classDeclaration.simpleName.asString()
        )

        return StackInfo(
            classDeclaration = classDeclaration,
            name = name,
            className = classDeclaration.simpleName.asString(),
            packageName = classDeclaration.packageName.asString(),
            startDestination = startDestination,
            startDestinationClass = startDestinationClass,
            destinations = destinations,
            resolvedStartDestination = resolvedStart
        )
    }

    /**
     * Resolve the start destination using priority-based resolution.
     *
     * Priority:
     * 1. Type-safe KClass reference (if provided and found)
     * 2. String-based class name match (if provided and found)
     * 3. First destination in declaration order
     *
     * @param startDestinationClass Type-safe class reference (may be null)
     * @param startDestinationString Legacy string-based class name (may be empty)
     * @param destinations List of all destinations in this stack
     * @param containerClassName Name of the container class (for logging)
     * @return Resolved DestinationInfo or null if no destinations exist
     */
    @Suppress("ReturnCount")
    private fun resolveStartDestination(
        startDestinationClass: KSClassDeclaration?,
        startDestinationString: String,
        destinations: List<com.jermey.quo.vadis.ksp.models.DestinationInfo>,
        containerClassName: String
    ): com.jermey.quo.vadis.ksp.models.DestinationInfo? {
        // Priority 1: Type-safe KClass reference
        if (startDestinationClass != null) {
            val qualifiedName = startDestinationClass.qualifiedName?.asString()
            val match = destinations.find {
                it.classDeclaration.qualifiedName?.asString() == qualifiedName
            }
            if (match != null) {
                logger.info(
                    "Resolved startDestinationClass to '${match.className}' in $containerClassName"
                )
                return match
            }
            logger.warn(
                "startDestinationClass '$qualifiedName' not found in $containerClassName, " +
                    "falling back to string or first destination"
            )
        }

        // Priority 2: String-based class name match
        if (startDestinationString.isNotEmpty()) {
            val match = destinations.find {
                it.className == startDestinationString
            }
            if (match != null) {
                logger.info(
                    "Resolved startDestination string '$startDestinationString' in $containerClassName"
                )
                return match
            }
            logger.warn(
                "startDestination '$startDestinationString' not found in $containerClassName, " +
                    "falling back to first destination"
            )
        }

        // Priority 3: First destination in declaration order
        val first = destinations.firstOrNull()
        if (first != null) {
            logger.info(
                "Using first destination '${first.className}' as start in $containerClassName"
            )
        } else {
            logger.warn("No destinations found in $containerClassName")
        }
        return first
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
