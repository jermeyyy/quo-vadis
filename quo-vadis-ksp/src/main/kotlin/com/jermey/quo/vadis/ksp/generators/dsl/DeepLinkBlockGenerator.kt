package com.jermey.quo.vadis.ksp.generators.dsl

import com.google.devtools.ksp.processing.KSPLogger
import com.jermey.quo.vadis.ksp.models.DestinationInfo
import com.squareup.kotlinpoet.CodeBlock

/**
 * Generates deep link configuration in the DSL.
 *
 * This generator is a placeholder for future deep link DSL expansion.
 * Currently, deep links are handled through the route parameter in `@Destination`
 * annotations and processed by the `DeepLinkHandler` implementation.
 *
 * ## Current Behavior
 *
 * Deep links are configured via `@Destination(route = "...")` annotations:
 *
 * ```kotlin
 * @Destination(route = "profile/{userId}")
 * data class ProfileDetail(val userId: String) : ProfileDestination()
 * ```
 *
 * The KSP processor extracts route information from [DestinationInfo] and
 * generates appropriate `DeepLinkHandler` code separately.
 *
 * ## Future Expansion
 *
 * This generator is designed to support future DSL-based deep link configuration:
 *
 * ```kotlin
 * navigationConfig {
 *     // Future: DSL-based deep link configuration
 *     deepLink<ProfileDetail>("profile/{userId}") {
 *         argument("userId") {
 *             type = NavType.StringType
 *             nullable = false
 *         }
 *     }
 * }
 * ```
 *
 * ## Input
 *
 * List of [DestinationInfo] from DestinationExtractor containing:
 * - Route pattern (if specified)
 * - Route parameters extracted from the pattern
 * - Constructor parameters for argument binding
 *
 * ## Output
 *
 * Currently returns an empty CodeBlock or a comment indicating that
 * deep links are handled by the generated DeepLinkHandler.
 *
 * @property logger KSP logger for debugging output
 */
class DeepLinkBlockGenerator(
    private val logger: KSPLogger
) {

    /**
     * Generates deep link related code.
     *
     * For now, this is a placeholder for future deep link DSL expansion.
     * The actual deep link handling is done by the generated DeepLinkHandler.
     *
     * @param destinations List of destinations with route information
     * @return CodeBlock (currently empty or comment, reserved for future use)
     */
    fun generate(destinations: List<DestinationInfo>): CodeBlock {
        // Filter destinations that have routes defined
        val destinationsWithRoutes = destinations.filter { !it.route.isNullOrEmpty() }

        if (destinationsWithRoutes.isEmpty()) {
            logger.info("DeepLinkBlockGenerator: No destinations with routes found")
            return CodeBlock.of("")
        }

        logger.info("DeepLinkBlockGenerator: Found ${destinationsWithRoutes.size} destinations with routes")

        // Log route information for debugging
        destinationsWithRoutes.forEach { dest ->
            logger.info("  - ${dest.qualifiedName}: route='${dest.route}', " +
                "params=${dest.routeParams}")
        }

        // Deep links are currently handled outside the DSL config
        // through the DeepLinkHandler implementation
        // This generator is reserved for future DSL-based deep link configuration
        return CodeBlock.of("// Deep links configured via @Destination routes, handled by DeepLinkHandler\n")
    }

    /**
     * Checks if any destinations have deep link routes defined.
     *
     * Utility method that can be used to determine whether to include
     * the deep links section in the generated output.
     *
     * @param destinations List of destination info
     * @return True if any destination has a route defined
     */
    fun hasDeepLinks(destinations: List<DestinationInfo>): Boolean {
        return destinations.any { !it.route.isNullOrEmpty() }
    }

}
