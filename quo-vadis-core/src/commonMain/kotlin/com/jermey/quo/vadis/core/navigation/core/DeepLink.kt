package com.jermey.quo.vadis.core.navigation.core

/**
 * Represents a deep link for navigation.
 *
 * Supports URI-based and custom deep linking schemes with proper separation
 * of scheme, path, path parameters, and query parameters.
 *
 * @param scheme The URI scheme (e.g., "app", "myapp", "https")
 * @param path The path portion of the URI without leading slash (e.g., "profile/123")
 * @param pathParams Parameters extracted from path segments by the registry during matching
 * @param queryParams Query string parameters from the URI
 */
data class DeepLink(
    val scheme: String,
    val path: String,
    val pathParams: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap()
) {
    /**
     * All parameters merged, with path params taking precedence over query params.
     */
    val allParams: Map<String, String>
        get() = queryParams + pathParams

    /**
     * Reconstruct the URI from components.
     *
     * This provides backward compatibility with code expecting a `uri` property.
     */
    val uri: String
        get() = buildString {
            append(scheme)
            append("://")
            append(path)
            if (queryParams.isNotEmpty()) {
                append("?")
                append(queryParams.entries.joinToString("&") { "${it.key}=${it.value}" })
            }
        }

    /**
     * Parameters - alias for queryParams for backward compatibility.
     */
    @Deprecated(
        message = "Use queryParams or allParams instead",
        replaceWith = ReplaceWith("queryParams"),
        level = DeprecationLevel.WARNING
    )
    val parameters: Map<String, String>
        get() = queryParams

    companion object {
        /**
         * Parse a URI string into a DeepLink.
         *
         * Extracts scheme, path, and query parameters from the URI.
         * Path parameters are populated by the registry during matching.
         *
         * @param uri The URI string to parse (e.g., "app://profile/123?ref=email")
         * @return Parsed DeepLink instance
         */
        fun parse(uri: String): DeepLink {
            val schemeEnd = uri.indexOf("://")
            val scheme = if (schemeEnd >= 0) uri.substring(0, schemeEnd) else "app"

            val pathStart = if (schemeEnd >= 0) schemeEnd + 3 else 0
            val queryStart = uri.indexOf("?", pathStart)

            val path = if (queryStart >= 0) {
                uri.substring(pathStart, queryStart)
            } else {
                uri.substring(pathStart)
            }.trimStart('/')

            val queryParams = if (queryStart >= 0) {
                parseQueryParams(uri.substring(queryStart + 1))
            } else {
                emptyMap()
            }

            return DeepLink(
                scheme = scheme,
                path = path,
                pathParams = emptyMap(), // Populated by registry during matching
                queryParams = queryParams
            )
        }

        private fun parseQueryParams(query: String): Map<String, String> {
            return query.split("&")
                .mapNotNull { param ->
                    val parts = param.split("=")
                    if (parts.size == 2) parts[0] to parts[1] else null
                }
                .toMap()
        }
    }
}

/**
 * Handler for resolving deep links to destinations.
 */
@Deprecated(
    message = "Use DeepLinkRegistry instead for the new unified API",
    replaceWith = ReplaceWith("DeepLinkRegistry"),
    level = DeprecationLevel.WARNING
)
interface DeepLinkHandler {
    /**
     * Resolve a deep link to a destination.
     * @param deepLink the deep link to resolve
     * @return the destination if resolved, null otherwise
     */
    fun resolve(deepLink: DeepLink): NavDestination?

    /**
     * Register a deep link pattern with an action to execute when matched.
     * 
     * @param pattern The URI pattern to match (e.g., "app://demo/item/{id}")
     * @param action The action to execute when pattern matches.
     *               Receives navigator and extracted parameters.
     */
    fun register(
        pattern: String,
        action: (navigator: Navigator, parameters: Map<String, String>) -> Unit
    )

    /**
     * Handle a deep link by resolving it and executing the registered action.
     * 
     * @param deepLink The deep link to handle
     * @param navigator The navigator to pass to the action
     */
    fun handle(deepLink: DeepLink, navigator: Navigator)
}

/**
 * Default implementation of DeepLinkHandler.
 */
class DefaultDeepLinkHandler : DeepLinkHandler {
    /**
     * New simplified registration format without destination parameter.
     */
    private data class SimpleDeepLinkRegistration(
        val pattern: String,
        val action: (navigator: Navigator, parameters: Map<String, String>) -> Unit
    )

    /**
     * Legacy registration format with destination parameter (deprecated).
     */
    private data class LegacyDeepLinkRegistration(
        val pattern: String,
        val destinationFactory: (Map<String, String>) -> NavDestination,
        val action: (NavDestination, Navigator, Map<String, String>) -> Unit
    )
    
    private val simpleRegistrations = mutableListOf<SimpleDeepLinkRegistration>()
    private val legacyRegistrations = mutableListOf<LegacyDeepLinkRegistration>()

    override fun resolve(deepLink: DeepLink): NavDestination? {
        // Try legacy pattern matching (only legacy registrations have destination factories)
        legacyRegistrations.forEach { registration ->
            val pattern = registration.pattern
            if (deepLink.uri == pattern || matchesPattern(deepLink.uri, pattern)) {
                val params = extractParameters(deepLink.uri, pattern) + deepLink.parameters
                return registration.destinationFactory(params)
            }
        }

        return null
    }

    override fun register(
        pattern: String,
        action: (navigator: Navigator, parameters: Map<String, String>) -> Unit
    ) {
        simpleRegistrations.add(SimpleDeepLinkRegistration(pattern, action))
    }

    override fun handle(deepLink: DeepLink, navigator: Navigator) {
        // Try simple registrations first (new format)
        simpleRegistrations.forEach { registration ->
            val pattern = registration.pattern
            if (deepLink.uri == pattern || matchesPattern(deepLink.uri, pattern)) {
                val params = extractParameters(deepLink.uri, pattern) + deepLink.parameters
                registration.action(navigator, params)
                return
            }
        }

        // Try legacy registrations (deprecated format)
        legacyRegistrations.forEach { registration ->
            val pattern = registration.pattern
            if (deepLink.uri == pattern || matchesPattern(deepLink.uri, pattern)) {
                val params = extractParameters(deepLink.uri, pattern) + deepLink.parameters
                val destination = registration.destinationFactory(params)
                registration.action(destination, navigator, params)
                return
            }
        }
        
        // Fallback: try to find destination and navigate directly
        val destination = resolve(deepLink)
        destination?.let { navigator.navigate(it) }
    }

    private fun matchesPattern(uri: String, pattern: String): Boolean {
        val regex = pattern
            .replace(Regex("\\{[^}]+\\}"), "[^/]+")
            .toRegex()
        return regex.matches(uri)
    }

    private fun extractParameters(uri: String, pattern: String): Map<String, String> {
        // Simple parameter extraction without named groups
        val paramNames = mutableListOf<String>()
        val regexPattern = pattern.replace(Regex("\\{([^}]+)\\}")) { matchResult ->
            paramNames.add(matchResult.groupValues[1])
            "([^/]+)"
        }

        val regex = regexPattern.toRegex()
        val match = regex.matchEntire(uri) ?: return emptyMap()

        val result = mutableMapOf<String, String>()
        paramNames.forEachIndexed { index, name ->
            match.groupValues.getOrNull(index + 1)?.let { value ->
                result[name] = value
            }
        }
        return result
    }
}
