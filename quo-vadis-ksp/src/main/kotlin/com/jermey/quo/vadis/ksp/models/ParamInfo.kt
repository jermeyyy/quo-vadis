package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSType

/**
 * Metadata for a constructor parameter.
 *
 * @property name Parameter name
 * @property type KSP type of the parameter
 * @property hasDefault True if the parameter has a default value
 */
data class ParamInfo(
    val name: String,
    val type: KSType,
    val hasDefault: Boolean
)
