package com.jermey.quo.vadis.ksp.extractors

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.jermey.quo.vadis.ksp.models.PaneRole
import com.jermey.quo.vadis.ksp.models.ParamInfo
import com.jermey.quo.vadis.ksp.models.SerializerType

/**
 * Extracts @Destination annotations into DestinationInfo models.
 *
 * This extractor is the foundation for parsing navigation destinations.
 * It handles:
 * - Route parameter extraction from pattern strings (e.g., "{id}" from "detail/{id}")
 * - Constructor parameter extraction for data classes
 * - Data object vs data class detection
 * - Parent sealed class detection
 * - Pane role extraction for pane navigation routing
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

        // Extract paneRole if specified (non-default PRIMARY means it was explicitly set)
        val paneRoleValue = annotation.arguments.find {
            it.name?.asString() == "paneRole"
        }?.value
        val paneRole = parsePaneRole(paneRoleValue)

        val routeParams = route?.let { extractRouteParams(it) } ?: emptyList()

        val isDataModifier = classDeclaration.modifiers.contains(Modifier.DATA)
        val isSealedModifier = classDeclaration.modifiers.contains(Modifier.SEALED)
        val classKind = classDeclaration.classKind

        val isObject = classKind == ClassKind.OBJECT
        val isDataObject = isDataModifier && isObject
        val isDataClass = isDataModifier && classKind == ClassKind.CLASS
        val isSealedClass = isSealedModifier && (classKind == ClassKind.CLASS || classKind == ClassKind.INTERFACE)

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
            isObject = isObject,
            isDataObject = isDataObject,
            isDataClass = isDataClass,
            isSealedClass = isSealedClass,
            constructorParams = constructorParams,
            parentSealedClass = parentSealedClass,
            paneRole = paneRole
        )
    }

    /**
     * Parse PaneRole from annotation value.
     *
     * Returns null if the value is the default PRIMARY (meaning not explicitly set
     * for pane routing purposes).
     *
     * @param value The raw annotation value from KSP
     * @return Parsed PaneRole or null if default/unset
     */
    private fun parsePaneRole(value: Any?): PaneRole? {
        if (value == null) return null
        val str = value.toString()
        val simpleName = if (str.contains(".")) {
            str.substringAfterLast(".")
        } else {
            str
        }
        return try {
            val role = PaneRole.valueOf(simpleName)
            // Return null for PRIMARY (the default) - it means "not explicitly set"
            // Only SECONDARY and EXTRA indicate explicit pane routing
            if (role == PaneRole.PRIMARY) null else role
        } catch (e: IllegalArgumentException) {
            null
        }
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
            val argumentAnnotation = param.annotations.find {
                it.shortName.asString() == "Argument"
            }

            val isArgument = argumentAnnotation != null
            val argumentKey = if (isArgument) {
                val keyValue = argumentAnnotation.arguments.find {
                    it.name?.asString() == "key"
                }?.value as? String
                keyValue?.takeIf { it.isNotEmpty() } ?: param.name?.asString() ?: ""
            } else {
                ""
            }
            val isOptionalArgument = if (isArgument) {
                argumentAnnotation.arguments.find {
                    it.name?.asString() == "optional"
                }?.value as? Boolean ?: false
            } else {
                false
            }
            val paramType = param.type.resolve()
            val serializerType = if (isArgument) {
                determineSerializerType(paramType)
            } else {
                SerializerType.STRING
            }

            ParamInfo(
                name = param.name?.asString() ?: "",
                type = paramType,
                hasDefault = param.hasDefault,
                isArgument = isArgument,
                argumentKey = argumentKey,
                isOptionalArgument = isOptionalArgument,
                serializerType = serializerType
            )
        }
    }

    /**
     * Determine the SerializerType for a given KSType.
     *
     * Maps Kotlin types to their corresponding serialization strategy:
     * - Primitives (String, Int, Long, Float, Double, Boolean) → Direct conversion
     * - Enums → Enum name serialization
     * - Complex types (@Serializable) → JSON serialization
     *
     * @param type The KSType to analyze
     * @return The appropriate SerializerType for the type
     */
    private fun determineSerializerType(type: KSType): SerializerType {
        val declaration = type.declaration
        val qualifiedName = declaration.qualifiedName?.asString()

        // Handle nullable types - use the underlying type
        val nonNullType = if (type.isMarkedNullable) {
            type.makeNotNullable()
        } else {
            type
        }
        val nonNullQualifiedName = nonNullType.declaration.qualifiedName?.asString()

        return when (nonNullQualifiedName) {
            "kotlin.String" -> SerializerType.STRING
            "kotlin.Int" -> SerializerType.INT
            "kotlin.Long" -> SerializerType.LONG
            "kotlin.Float" -> SerializerType.FLOAT
            "kotlin.Double" -> SerializerType.DOUBLE
            "kotlin.Boolean" -> SerializerType.BOOLEAN
            else -> {
                // Check if it's an enum
                val typeDeclaration = nonNullType.declaration as? KSClassDeclaration
                if (typeDeclaration?.classKind == ClassKind.ENUM_CLASS) {
                    SerializerType.ENUM
                } else {
                    // Complex type - assume JSON serialization
                    SerializerType.JSON
                }
            }
        }
    }
}
