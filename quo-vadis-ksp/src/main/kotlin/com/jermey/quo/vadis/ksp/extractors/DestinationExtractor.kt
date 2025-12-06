package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.ParamInfo

/**
 * Extracts @Destination annotations into DestinationInfo models.
 *
 * This extractor is the foundation for parsing navigation destinations.
 * It handles:
 * - Route parameter extraction from pattern strings (e.g., "{id}" from "detail/{id}")
 * - Constructor parameter extraction for data classes
 * - Data object vs data class detection
 * - Parent sealed class detection
 *
 * @property logger KSP logger for error/warning output
 */
class DestinationExtractor(
    private val logger: KSPLogger
) {

    /**
     * Extract DestinationInfo from a class declaration.
     *
     * @param classDeclaration The class annotated with @Destination
     * @return DestinationInfo or null if extraction fails
     */
    fun extract(classDeclaration: KSClassDeclaration): DestinationInfo? {
        val annotation = classDeclaration.annotations.find {
            it.shortName.asString() == "Destination"
        } ?: return null

        val route = annotation.arguments.find {
            it.name?.asString() == "route"
        }?.value as? String

        val routeParams = route?.let { extractRouteParams(it) } ?: emptyList()

        val isDataModifier = classDeclaration.modifiers.contains(Modifier.DATA)
        val classKind = classDeclaration.classKind

        val isDataObject = isDataModifier && classKind == ClassKind.OBJECT
        val isDataClass = isDataModifier && classKind == ClassKind.CLASS

        val constructorParams = if (isDataClass) {
            extractConstructorParams(classDeclaration)
        } else {
            emptyList()
        }

        val parentSealedClass = classDeclaration.parentDeclaration?.let {
            (it as? KSClassDeclaration)?.simpleName?.asString()
        }

        return DestinationInfo(
            classDeclaration = classDeclaration,
            className = classDeclaration.simpleName.asString(),
            qualifiedName = classDeclaration.qualifiedName?.asString() ?: "",
            route = route?.takeIf { it.isNotEmpty() },
            routeParams = routeParams,
            isDataObject = isDataObject,
            isDataClass = isDataClass,
            constructorParams = constructorParams,
            parentSealedClass = parentSealedClass
        )
    }

    /**
     * Extract all destinations from a sealed class container.
     *
     * This method iterates through all sealed subclasses of the container
     * and extracts DestinationInfo for each one that has a @Destination annotation.
     *
     * @param containerClass The sealed class containing destination subclasses
     * @return List of DestinationInfo for all annotated subclasses
     */
    fun extractFromContainer(containerClass: KSClassDeclaration): List<DestinationInfo> {
        return containerClass.getSealedSubclasses()
            .mapNotNull { extract(it) }
            .toList()
    }

    /**
     * Extract route parameters from a route pattern.
     *
     * Finds all parameters enclosed in curly braces.
     * Examples:
     * - "home/detail/{id}" → ["id"]
     * - "user/{userId}/post/{postId}" → ["userId", "postId"]
     * - "search?query={q}&filter={f}" → ["q", "f"]
     *
     * @param route The route pattern string
     * @return List of parameter names found in the pattern
     */
    private fun extractRouteParams(route: String): List<String> {
        val regex = Regex("\\{([^}]+)\\}")
        return regex.findAll(route).map { it.groupValues[1] }.toList()
    }

    /**
     * Extract constructor parameters from a class declaration.
     *
     * @param classDeclaration The class to extract parameters from
     * @return List of ParamInfo for each constructor parameter
     */
    private fun extractConstructorParams(classDeclaration: KSClassDeclaration): List<ParamInfo> {
        val primaryConstructor = classDeclaration.primaryConstructor ?: return emptyList()
        return primaryConstructor.parameters.map { param ->
            ParamInfo(
                name = param.name?.asString() ?: "",
                type = param.type.resolve(),
                hasDefault = param.hasDefault
            )
        }
    }
}
