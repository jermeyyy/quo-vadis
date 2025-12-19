package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSType

/**
 * Defines how a navigation argument should be serialized for deep links.
 *
 * The serializer type is determined by the parameter's Kotlin type and affects
 * how the argument is converted to/from URL string parameters.
 */
enum class SerializerType {
    /** Direct toString() - no conversion needed */
    STRING,
    /** toInt() parsing */
    INT,
    /** toLong() parsing */
    LONG,
    /** toFloat() parsing */
    FLOAT,
    /** toDouble() parsing */
    DOUBLE,
    /** "true"/"false" case-insensitive parsing */
    BOOLEAN,
    /** enumValueOf<T>() using enum name */
    ENUM,
    /** kotlinx.serialization Json for complex types */
    JSON
}

/**
 * Metadata for a constructor parameter.
 *
 * Contains information about a constructor parameter of a @Destination data class,
 * including @Argument annotation data if present.
 *
 * @property name Parameter name
 * @property type KSP type of the parameter
 * @property hasDefault True if the parameter has a default value
 * @property isArgument True if the parameter has @Argument annotation
 * @property argumentKey Key for URL parameter mapping (from @Argument.key or param name)
 * @property isOptionalArgument True if @Argument(optional = true)
 * @property serializerType How to serialize this argument for deep links
 */
data class ParamInfo(
    val name: String,
    val type: KSType,
    val hasDefault: Boolean,
    val isArgument: Boolean = false,
    val argumentKey: String = "",
    val isOptionalArgument: Boolean = false,
    val serializerType: SerializerType = SerializerType.STRING
)
