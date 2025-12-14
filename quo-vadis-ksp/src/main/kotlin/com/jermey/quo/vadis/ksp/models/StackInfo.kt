package com.jermey.quo.vadis.ksp.models

import com.google.devtools.ksp.symbol.KSClassDeclaration

/**
 * Extracted metadata from a @Stack annotation.
 *
 * ## Start Destination Resolution
 *
 * The start destination is resolved using the following priority:
 * 1. If [startDestinationClass] is not null (and not Unit::class), use it (type-safe)
 * 2. Otherwise, if [startDestination] is not empty, match by class name (legacy string-based)
 * 3. Otherwise, use the first destination in declaration order
 *
 * @property classDeclaration The KSP class declaration for this stack
 * @property name Stack identifier from annotation (e.g., "home")
 * @property className Simple class name (e.g., "HomeDestination")
 * @property packageName Package containing this class
 * @property startDestination Simple name of the start destination (e.g., "Feed").
 *                            Legacy string-based approach. Prefer [startDestinationClass].
 * @property startDestinationClass Type-safe KClass reference to the start destination.
 *                                 Null if not specified or if Unit::class was used.
 *                                 Takes precedence over [startDestination] when set.
 * @property destinations List of all @Destination subclasses within this sealed class
 * @property resolvedStartDestination The DestinationInfo for the resolved start destination.
 *                                    Resolution follows the priority order described above.
 */
data class StackInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val startDestination: String,
    val startDestinationClass: KSClassDeclaration?,
    val destinations: List<DestinationInfo>,
    val resolvedStartDestination: DestinationInfo?
)
