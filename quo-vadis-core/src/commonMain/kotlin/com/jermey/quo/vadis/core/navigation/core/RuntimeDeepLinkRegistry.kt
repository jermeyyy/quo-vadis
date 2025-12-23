package com.jermey.quo.vadis.core.navigation.core

/**
 * Runtime implementation of [DeepLinkRegistry] for registering deep link patterns dynamically.
 *
 * This class allows registering deep link patterns at runtime, as opposed to the compile-time
 * registration provided by KSP-generated `GeneratedDeepLinkRegistry`. Use this for:
 *
 * ## Use Cases
 *
 * - **Testing**: Create isolated registries for unit tests without full KSP processing
 * - **Dynamic routes**: Register routes that are determined at runtime (e.g., from config)
 * - **Feature modules**: Register routes from dynamically loaded feature modules
 * - **Prototyping**: Quick experimentation without annotation processing
 *
 * ## Usage Example
 *
 * ```kotlin
 * val registry = RuntimeDeepLinkRegistry()
 *
 * // Register a pattern with a factory
 * registry.register("product/{id}") { params ->
 *     ProductDestination(id = params["id"]!!)
 * }
 *
 * // Register an action (navigation side-effect)
 * registry.registerAction("logout") { navigator, _ ->
 *     navigator.popToRoot()
 *     // Perform logout logic
 * }
 *
 * // Resolve a deep link
 * val destination = registry.resolve("product/123")
 *
 * // Check if a URI can be handled
 * val canHandle = registry.canHandle("product/456") // true
 * ```
 *
 * ## Combining with Generated Registry
 *
 * Use [CompositeDeepLinkRegistry] to combine runtime and generated registries:
 *
 * ```kotlin
 * val combinedRegistry = CompositeDeepLinkRegistry(
 *     GeneratedDeepLinkRegistry,
 *     runtimeRegistry
 * )
 * ```
 *
 * ## Thread Safety
 *
 * This implementation uses mutable lists internally. For concurrent registration,
 * synchronize access externally or register all patterns during initialization.
 *
 * @see DeepLinkRegistry
 * @see CompositeDeepLinkRegistry
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
