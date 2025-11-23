package com.jermey.quo.vadis.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted information about a navigation graph.
 */
data class GraphInfo(
    val graphClass: KSClassDeclaration,
    val graphName: String,
    val packageName: String,
    val className: String,
    val destinations: List<DestinationInfo>,
    val startDestinationName: String? = null
)

/**
 * Information about a single destination in a graph.
 */
data class DestinationInfo(
    val destinationClass: KSClassDeclaration,
    val name: String,
    val route: String,
    val isObject: Boolean,
    val isDataClass: Boolean,
    val argumentType: String?
)
