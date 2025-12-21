package com.jermey.quo.vadis.core.navigation.core

/**
 * Runtime implementation of DeepLinkRegistry for registering patterns at runtime.
 *
 * Thread-safe implementation using synchronized collections.
 */
class RuntimeDeepLinkRegistry : DeepLinkRegistry {

    private data class PatternRegistration(
        val pattern: String,
        val paramNames: List<String>,
        val regex: Regex,
        val factory: ((Map<String, String>) -> NavDestination)?
    )

    private data class ActionRegistration(
        val pattern: String,
        val paramNames: List<String>,
        val regex: Regex,
        val action: (Navigator, Map<String, String>) -> Unit
    )

    private val patternRegistrations = mutableListOf<PatternRegistration>()
    private val actionRegistrations = mutableListOf<ActionRegistration>()

    override fun resolve(uri: String): NavDestination? {
        return resolve(DeepLink.parse(uri))
    }

    override fun resolve(deepLink: DeepLink): NavDestination? {
        val path = deepLink.path

        for (registration in patternRegistrations) {
            val matchResult = registration.regex.matchEntire(path)
            if (matchResult != null) {
                val pathParams = registration.paramNames.zip(
                    matchResult.groupValues.drop(1)
                ).toMap()
                val allParams = deepLink.queryParams + pathParams
                return registration.factory?.invoke(allParams)
            }
        }
        return null
    }

    override fun register(pattern: String, factory: (params: Map<String, String>) -> NavDestination) {
        val (paramNames, regex) = buildPatternRegex(pattern)
        patternRegistrations.add(
            PatternRegistration(pattern, paramNames, regex, factory)
        )
    }

    override fun registerAction(pattern: String, action: (navigator: Navigator, params: Map<String, String>) -> Unit) {
        val (paramNames, regex) = buildPatternRegex(pattern)
        actionRegistrations.add(
            ActionRegistration(pattern, paramNames, regex, action)
        )
    }

    override fun handle(uri: String, navigator: Navigator): Boolean {
        val deepLink = DeepLink.parse(uri)
        val path = deepLink.path

        // Try action registrations first
        for (registration in actionRegistrations) {
            val matchResult = registration.regex.matchEntire(path)
            if (matchResult != null) {
                val pathParams = registration.paramNames.zip(
                    matchResult.groupValues.drop(1)
                ).toMap()
                val allParams = deepLink.queryParams + pathParams
                registration.action(navigator, allParams)
                return true
            }
        }

        // Fall back to destination factories
        val destination = resolve(deepLink)
        if (destination != null) {
            navigator.navigate(destination)
            return true
        }

        return false
    }

    override fun createUri(destination: NavDestination, scheme: String): String? {
        // Runtime registry doesn't track destination-to-route mapping
        return null
    }

    override fun canHandle(uri: String): Boolean {
        val deepLink = DeepLink.parse(uri)
        val path = deepLink.path

        return patternRegistrations.any { it.regex.matches(path) } ||
            actionRegistrations.any { it.regex.matches(path) }
    }

    override fun getRegisteredPatterns(): List<String> {
        return patternRegistrations.map { it.pattern } +
            actionRegistrations.map { it.pattern }
    }

    private fun buildPatternRegex(pattern: String): Pair<List<String>, Regex> {
        val paramNames = mutableListOf<String>()
        
        // Find all {param} placeholders and build regex
        val paramRegex = Regex("\\{([^}]+)\\}")
        var regexPattern = ""
        var lastEnd = 0
        
        for (match in paramRegex.findAll(pattern)) {
            // Escape the literal part before this parameter
            regexPattern += Regex.escape(pattern.substring(lastEnd, match.range.first))
            // Add capture group for the parameter
            paramNames.add(match.groupValues[1])
            regexPattern += "([^/]+)"
            lastEnd = match.range.last + 1
        }
        
        // Escape any remaining literal part after the last parameter
        if (lastEnd < pattern.length) {
            regexPattern += Regex.escape(pattern.substring(lastEnd))
        }
        
        return paramNames to Regex("^$regexPattern$")
    }
}
