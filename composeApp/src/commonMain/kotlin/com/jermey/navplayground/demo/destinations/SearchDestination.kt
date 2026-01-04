package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Search feature destinations demonstrating typed query parameters.
 *
 * ANNOTATION PATTERN: Destinations with Typed Query Parameters
 *
 * The SearchResults destination demonstrates:
 * - `@Argument(optional = true)` for query parameters with defaults
 * - Type conversions: `Int` (page), `Boolean` (sortAsc)
 * - Mix of required and optional parameters
 *
 * Example deep link:
 * ```
 * app://search/results?query=kotlin&page=2&sortAsc=true
 * ```
 *
 * KSP generates type-safe code that:
 * - Parses `page` as Int using `.toIntOrNull() ?: defaultValue`
 * - Parses `sortAsc` as Boolean using `.toBooleanStrictOrNull()`
 * - Falls back to default values for missing optional params
 *
 * @see com.jermey.quo.vadis.annotations.Argument
 */
@Stack(name = "search", startDestination = SearchDestination.Home::class)
sealed class SearchDestination : NavDestination {

    /**
     * Search home screen with search input.
     */
    @Destination(route = "search/home")
    data object Home : SearchDestination()

    /**
     * Search results with typed query parameters.
     *
     * KSP generates:
     * ```kotlin
     * fun Navigator.navigateToSearchResults(
     *     query: String,
     *     page: Int = 1,
     *     sortAsc: Boolean = true,
     *     transition: NavigationTransition? = null
     * )
     * ```
     *
     * Deep link handler uses type conversions:
     * ```kotlin
     * RoutePattern("search/results", listOf()) { params ->
     *     SearchDestination.Results(
     *         query = params["query"]!!,
     *         page = params["page"]?.toIntOrNull() ?: 1,
     *         sortAsc = params["sortAsc"]?.toBooleanStrictOrNull() ?: true
     *     )
     * }
     * ```
     *
     * @property query The search query string (required)
     * @property page Page number for pagination (optional, defaults to 1)
     * @property sortAsc Sort direction: true = ascending, false = descending (optional, defaults to true)
     */
    @Destination(route = "search/results")
    data class Results(
        @Argument val query: String,
        @Argument(optional = true) val page: Int = 1,
        @Argument(optional = true) val sortAsc: Boolean = true
    ) : SearchDestination()

    /**
     * Item detail within search results.
     *
     * Demonstrates path parameter with optional query params.
     *
     * @property itemId The ID of the item to display (path param)
     * @property highlightTerm Optional term to highlight in detail view
     * @property showRelated Whether to show related items section
     */
    @Destination(route = "search/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String,
        @Argument(optional = true) val highlightTerm: String? = null,
        @Argument(optional = true) val showRelated: Boolean = true
    ) : SearchDestination()
}
