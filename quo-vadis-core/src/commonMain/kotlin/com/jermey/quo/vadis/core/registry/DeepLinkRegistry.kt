package com.jermey.quo.vadis.core.registry

import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Registry for deep link patterns and their handlers.
 *
 * Combines generated route patterns with runtime registrations.
 * Supports both path parameters ({param}) and query parameters.
 */
interface DeepLinkRegistry {

    /**
     * Resolve a deep link URI to a destination.
     *
     * @param uri The full URI string (e.g., "app://profile/123?ref=email")
     * @return The destination if matched, null otherwise
     */
    fun resolve(uri: String): NavDestination?

    /**
     * Resolve a parsed DeepLink to a destination.
     *
     * @param deepLink The parsed deep link
     * @return The destination if matched, null otherwise
     */
    fun resolve(deepLink: DeepLink): NavDestination?

    /**
     * Register a runtime deep link pattern with a destination factory.
     *
     * @param pattern Route pattern (e.g., "profile/{userId}")
     * @param factory Factory function that creates a destination from extracted parameters
     */
    fun register(
        pattern: String,
        factory: (params: Map<String, String>) -> NavDestination
    )

    /**
     * Register a runtime deep link pattern with navigation action.
     *
     * @param pattern Route pattern (e.g., "profile/{userId}")
     * @param action Action to execute with navigator and extracted parameters
     */
    fun registerAction(
        pattern: String,
        action: (navigator: Navigator, params: Map<String, String>) -> Unit
    )

    /**
     * Handle a deep link by resolving and executing any registered action.
     *
     * @param uri The URI string to handle
     * @param navigator The navigator to pass to actions
     * @return true if handled, false otherwise
     */
    fun handle(uri: String, navigator: Navigator): Boolean

    /**
     * Create a deep link URI from a destination.
     *
     * @param destination The destination to create a URI for
     * @param scheme The URI scheme (default: "app")
     * @return The URI string, or null if destination has no route
     */
    fun createUri(destination: NavDestination, scheme: String = "app"): String?

    /**
     * Check if a URI matches any registered pattern.
     *
     * @param uri The URI to check
     * @return true if the URI can be handled
     */
    fun canHandle(uri: String): Boolean

    /**
     * Get all registered route patterns.
     *
     * @return List of pattern strings
     */
    fun getRegisteredPatterns(): List<String>

    /**
     * Companion object providing default implementations.
     */
    companion object {
        /**
         * Empty registry with no registered patterns.
         *
         * All lookups return null/false/empty. Registration methods are no-ops.
         * This is the identity element when no deep link support is needed.
         *
         * ## Usage
         *
         * ```kotlin
         * // Use when no deep links are registered
         * override val deepLinkRegistry: DeepLinkRegistry = DeepLinkRegistry.Empty
         * ```
         */
        val Empty: DeepLinkRegistry = object : DeepLinkRegistry {
            override fun resolve(uri: String): NavDestination? = null
            override fun resolve(deepLink: DeepLink): NavDestination? = null
            override fun register(pattern: String, factory: (params: Map<String, String>) -> NavDestination) { /* no-op */ }
            override fun registerAction(pattern: String, action: (navigator: Navigator, params: Map<String, String>) -> Unit) { /* no-op */ }
            override fun handle(uri: String, navigator: Navigator): Boolean = false
            override fun createUri(destination: NavDestination, scheme: String): String? = null
            override fun canHandle(uri: String): Boolean = false
            override fun getRegisteredPatterns(): List<String> = emptyList()
        }
    }
}