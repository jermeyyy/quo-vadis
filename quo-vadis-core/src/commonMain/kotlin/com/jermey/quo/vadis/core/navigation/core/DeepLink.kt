package com.jermey.quo.vadis.core.navigation.core

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
     * @return the destination if resolved, null otherwise
     */
    fun resolve(deepLink: DeepLink): Destination?

    /**
     * Register a deep link pattern with an action to execute when matched.
     * 
     * The action receives:
     * - destination: The resolved destination to navigate to
     * - navigator: The navigator instance to use for navigation
     * - parameters: Extracted parameters from the URI pattern and query string
     * 
     * This allows for custom navigation behavior, such as:
     * - Switching tabs before navigating
     * - Clearing back stack
     * - Custom transition animations
     * - Conditional navigation logic
     * 
     * @param pattern The URI pattern to match (e.g., "app://demo/item/{id}")
     * @param action The action to execute when the pattern matches
     */
    fun register(
        pattern: String, 
        action: (destination: Destination, navigator: Navigator, parameters: Map<String, String>) -> Unit
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
    private data class DeepLinkRegistration(
        val pattern: String,
        val destinationFactory: (Map<String, String>) -> Destination,
        val action: (Destination, Navigator, Map<String, String>) -> Unit
    )
    
    private val registrations = mutableListOf<DeepLinkRegistration>()

    override fun resolve(deepLink: DeepLink): Destination? {
        // Try pattern matching
        registrations.forEach { registration ->
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
        action: (destination: Destination, navigator: Navigator, parameters: Map<String, String>) -> Unit
    ) {
        // Create a destination factory that will be used for resolution
        // The actual destination will be determined when the action is executed
        val destinationFactory: (Map<String, String>) -> Destination = { _ ->
            // This is a placeholder destination for resolution purposes
            // The action will handle the actual navigation
            BasicDestination(pattern)
        }
        
        registrations.add(DeepLinkRegistration(pattern, destinationFactory, action))
    }
    
    override fun handle(deepLink: DeepLink, navigator: Navigator) {
        // Find matching registration
        registrations.forEach { registration ->
            val pattern = registration.pattern
            if (deepLink.uri == pattern || matchesPattern(deepLink.uri, pattern)) {
                val params = extractParameters(deepLink.uri, pattern) + deepLink.parameters
                val destination = registration.destinationFactory(params)
                // Execute the action with the destination, navigator, and parameters
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
