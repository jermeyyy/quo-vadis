package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Composable

/**
 * Represents a deep link for navigation.
 * Supports URI-based and custom deep linking schemes.
 */
data class DeepLink(
    val uri: String,
    val parameters: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Parse a URI string into a DeepLink.
         */
        fun parse(uri: String): DeepLink {
            val parts = uri.split("?")
            val path = parts[0]
            val params = if (parts.size > 1) {
                parseParameters(parts[1])
            } else {
                emptyMap()
            }
            return DeepLink(path, params)
        }

        private fun parseParameters(query: String): Map<String, String> {
            return query.split("&")
                .mapNotNull { param ->
                    val parts = param.split("=")
                    if (parts.size == 2) {
                        parts[0] to parts[1]
                    } else null
                }
                .toMap()
        }
    }
}

/**
 * Configuration for deep link support on a destination.
 */
data class DeepLinkConfig(
    val uriPattern: String,
    val destinationFactory: (Map<String, String>) -> Destination
)

/**
 * Handler for resolving deep links to destinations.
 */
interface DeepLinkHandler {
    /**
     * Resolve a deep link to a destination.
     * @param deepLink the deep link to resolve
     * @param graphs available navigation graphs
     * @return the destination if resolved, null otherwise
     */
    fun resolve(deepLink: DeepLink, graphs: Map<String, NavigationGraph>): Destination?

    /**
     * Register a deep link pattern.
     */
    fun register(pattern: String, handler: (Map<String, String>) -> Destination)
}

/**
 * Default implementation of DeepLinkHandler.
 */
class DefaultDeepLinkHandler : DeepLinkHandler {
    private val patterns = mutableMapOf<String, (Map<String, String>) -> Destination>()

    override fun resolve(deepLink: DeepLink, graphs: Map<String, NavigationGraph>): Destination? {
        // First, try exact match
        patterns[deepLink.uri]?.let { factory ->
            return factory(deepLink.parameters)
        }

        // Then, try pattern matching
        patterns.forEach { (pattern, factory) ->
            if (matchesPattern(deepLink.uri, pattern)) {
                val params = extractParameters(deepLink.uri, pattern) + deepLink.parameters
                return factory(params)
            }
        }

        // Finally, search through registered graphs
        graphs.values.forEach { graph ->
            graph.destinations.forEach { destConfig ->
                if (destConfig.destination.route == deepLink.uri) {
                    return destConfig.destination
                }
            }
        }

        return null
    }

    override fun register(pattern: String, handler: (Map<String, String>) -> Destination) {
        patterns[pattern] = handler
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

/**
 * Extension function to create destinations with deep link support.
 */
fun NavigationGraphBuilder.deepLinkDestination(
    route: String,
    deepLinkPattern: String,
    content: @Composable (Destination, Navigator) -> Unit
) {
    val dest = SimpleDestination(route)
    destination(dest, transition = null, content = content)
}
