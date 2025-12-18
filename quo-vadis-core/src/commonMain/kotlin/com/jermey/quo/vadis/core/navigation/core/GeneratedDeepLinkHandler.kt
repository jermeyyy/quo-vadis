package com.jermey.quo.vadis.core.navigation.core

/**
 * Interface for KSP-generated deep link handlers.
 *
 * Extends [DeepLinkHandler] to provide type-safe deep link handling based on
 * code-generated route patterns from `@Destination` annotations.
 *
 * ## Usage
 *
 * The KSP processor generates an implementation of this interface that:
 * 1. Parses incoming URIs against registered route patterns
 * 2. Extracts path parameters and maps them to destination constructor arguments
 * 3. Creates destination instances with the extracted parameters
 *
 * ## Example
 *
 * Given a destination with a route annotation:
 * ```kotlin
 * @Destination(route = "detail/{id}")
 * data class DetailScreen(val id: String) : Destination
 * ```
 *
 * The generated handler will:
 * - Match URIs like `myapp://detail/123`
 * - Extract `id = "123"` from the path
 * - Return `DeepLinkResult.Matched(DetailScreen("123"))`
 *
 * @see DeepLinkHandler for the base deep link handler interface
 * @see DeepLinkResult for possible handling outcomes
 */
interface GeneratedDeepLinkHandler : DeepLinkHandler {
    /**
     * Handle a deep link URI and return the matching destination if found.
     *
     * The implementation parses the URI, matches it against registered route patterns,
     * extracts any path parameters, and constructs the corresponding destination instance.
     *
     * @param uri The URI to handle (e.g., "myapp://home/detail/123").
     *            Should include scheme, host, and path components.
     * @return The result of handling the deep link:
     *         - [DeepLinkResult.Matched] if a route pattern matched
     *         - [DeepLinkResult.NotMatched] if no route pattern matched
     */
    fun handleDeepLink(uri: String): DeepLinkResult

    /**
     * Create a deep link URI from a destination instance.
     *
     * This is the inverse operation of [handleDeepLink]. Given a destination,
     * it generates the corresponding URI that would route back to that destination.
     *
     * ## Example
     *
     * ```kotlin
     * val destination = DetailScreen(id = "123")
     * val uri = handler.createDeepLinkUri(destination, scheme = "myapp")
     * // Result: "myapp://detail/123"
     * ```
     *
     * @param destination The destination to create a URI for.
     * @param scheme The URI scheme to use (default: "myapp").
     *               This should match the scheme registered in your app's manifest/info.plist.
     * @return The URI string representing the destination, or `null` if the destination
     *         type has no associated route pattern (i.e., was not annotated with a route).
     */
    fun createDeepLinkUri(destination: NavDestination, scheme: String = "myapp"): String?
}

/**
 * Result of handling a deep link through [GeneratedDeepLinkHandler.handleDeepLink].
 *
 * This sealed class represents the two possible outcomes of attempting to handle
 * a deep link URI:
 * - [Matched]: The URI matched a registered route pattern
 * - [NotMatched]: The URI did not match any registered route pattern
 *
 * ## Usage
 *
 * ```kotlin
 * when (val result = handler.handleDeepLink(uri)) {
 *     is DeepLinkResult.Matched -> navigator.navigate(result.destination)
 *     is DeepLinkResult.NotMatched -> showError("Unknown deep link")
 * }
 * ```
 */
sealed class DeepLinkResult {
    /**
     * The deep link matched a route pattern and produced a destination.
     *
     * This result contains the fully constructed destination instance with
     * all path parameters extracted from the URI and converted to the
     * appropriate types.
     *
     * @property destination The destination instance created from the deep link.
     */
    data class Matched(val destination: NavDestination) : DeepLinkResult()

    /**
     * The deep link did not match any registered route pattern.
     *
     * This can occur when:
     * - The URI scheme doesn't match the expected scheme
     * - The URI path doesn't match any registered route patterns
     * - The URI is malformed or invalid
     *
     * Applications should handle this case gracefully, typically by showing
     * an error message or navigating to a default screen.
     */
    data object NotMatched : DeepLinkResult()
}
