package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted metadata from a @Stack annotation.
 *
 * @property classDeclaration The KSP class declaration for this stack
 * @property name Stack identifier from annotation (e.g., "home")
 * @property className Simple class name (e.g., "HomeDestination")
 * @property packageName Package containing this class
 * @property startDestination Simple name of the start destination (e.g., "Feed")
 * @property destinations List of all @Destination subclasses within this sealed class
 * @property resolvedStartDestination The DestinationInfo matching startDestination, if found
 */
data class StackInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val startDestination: String,
    val destinations: List<DestinationInfo>,
    val resolvedStartDestination: DestinationInfo?
)
