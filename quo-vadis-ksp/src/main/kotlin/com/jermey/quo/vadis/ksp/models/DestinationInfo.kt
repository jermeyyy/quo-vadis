package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted metadata from a @Destination annotation.
 *
 * @property classDeclaration The KSP class declaration for this destination
 * @property className Simple class name (e.g., "Detail")
 * @property qualifiedName Fully qualified name (e.g., "com.example.HomeDestination.Detail")
 * @property route Deep link route pattern (e.g., "home/detail/{id}"), null if not specified
 * @property routeParams List of route parameter names extracted from the route pattern
 * @property isObject True if this is any kind of object (data object, companion object, regular object)
 * @property isDataObject True if this is a `data object`
 * @property isDataClass True if this is a `data class`
 * @property isSealedClass True if this is a `sealed class` or `sealed interface`
 * @property constructorParams List of constructor parameters (for data classes)
 * @property parentSealedClass Simple name of the parent sealed class, if any
 * @property paneRole Pane role for pane navigation routing (null if not in a pane context)
 */
data class DestinationInfo(
    val classDeclaration: KSClassDeclaration,
    val className: String,
    val qualifiedName: String,
    val route: String?,
    val routeParams: List<String>,
    val isObject: Boolean,
    val isDataObject: Boolean,
    val isDataClass: Boolean,
    val isSealedClass: Boolean,
    val constructorParams: List<ParamInfo>,
    val parentSealedClass: String?,
    val paneRole: PaneRole? = null
)
