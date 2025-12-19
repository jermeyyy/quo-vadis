package com.jermey.quo.vadis.annotations

/**
 * Marks a constructor parameter as a navigation argument.
 *
 * Navigation arguments are values passed when navigating to a destination.
 * They are:
 * - Serialized for deep linking (if the destination has a route)
 * - Type-safe at compile time
 * - Accessible via the destination object in screen composables
 *
 * ## Usage
 *
 * ### Basic Argument
 * ```kotlin
 * @Destination(route = "profile/{userId}")
 * data class Profile(
 *     @Argument val userId: String
 * ) : ProfileDestination()
 * ```
 *
 * ### Custom Key for Deep Linking
 * ```kotlin
 * @Destination(route = "search?q={query}")
 * data class Search(
 *     @Argument(key = "query") val searchTerm: String
 * ) : SearchDestination()
 * ```
 *
 * ### Optional Argument
 * ```kotlin
 * @Destination(route = "products/detail/{id}")
 * data class Detail(
 *     @Argument val id: String,
 *     @Argument(optional = true) val referrer: String? = null,
 *     @Argument(optional = true) val showReviews: Boolean = false
 * ) : ProductsDestination()
 * ```
 *
 * ### Complex Types (require kotlinx.serialization)
 * ```kotlin
 * @Serializable
 * data class FilterOptions(
 *     val categories: List<String>,
 *     val priceRange: IntRange?
 * )
 *
 * @Destination(route = "search/results")
 * data class SearchResults(
 *     @Argument val query: String,
 *     @Argument val filters: FilterOptions  // JSON-serialized in deep links
 * ) : SearchDestination()
 * ```
 *
 * ## Supported Types
 *
 * | Type | Serialization | Notes |
 * |------|---------------|-------|
 * | `String` | Direct | No conversion |
 * | `Int`, `Long`, `Float`, `Double` | `.toString()` / `.toXxx()` | Numeric conversion |
 * | `Boolean` | `"true"` / `"false"` | Case-insensitive parsing |
 * | `Enum<T>` | `.name` / `enumValueOf()` | Enum name serialization |
 * | `@Serializable` | JSON | kotlinx.serialization required |
 * | `List<T>`, `Set<T>` | JSON | Where T is serializable |
 *
 * ## Deep Link Parameter Mapping
 *
 * Arguments are mapped to URL parameters based on their position in the route:
 *
 * ```kotlin
 * @Destination(route = "user/{userId}/post/{postId}")
 * data class UserPost(
 *     @Argument val userId: String,  // Maps to {userId}
 *     @Argument val postId: String   // Maps to {postId}
 * )
 * // Deep link: myapp://user/42/post/123
 * ```
 *
 * Query parameters are also supported:
 * ```kotlin
 * @Destination(route = "search?q={query}&page={page}")
 * data class Search(
 *     @Argument(key = "query") val q: String,
 *     @Argument(key = "page", optional = true) val page: Int = 1
 * )
 * // Deep link: myapp://search?q=kotlin&page=2
 * ```
 *
 * @property key Custom key for URL parameter mapping. Defaults to parameter name
 *   if empty. Use this when the URL parameter name differs from the Kotlin
 *   property name.
 *
 * @property optional Whether this argument can be omitted in deep links.
 *   - If `true`: Parameter must have a default value in the data class
 *   - If `false` (default): Parameter is required in deep links
 *   - Query parameters are typically optional; path parameters are required
 *
 * @see Destination
 * @see Screen
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class Argument(
    val key: String = "",
    val optional: Boolean = false
)
