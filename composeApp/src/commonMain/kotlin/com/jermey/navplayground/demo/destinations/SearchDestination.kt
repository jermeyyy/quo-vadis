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
     * Note: Query parameters are passed programmatically, not via deep links,
     * due to KSP generator limitations with optional route params.
     *
     * @property query The search query string (required)
     * @property page Page number for pagination (optional, defaults to 1)
     * @property sortAsc Sort direction: true = ascending, false = descending (optional, defaults to true)
     */
    @Destination(route = "search/results/{query}")
    data class Results(
        @Argument val query: String,
        val page: Int = 1,
        val sortAsc: Boolean = true
    ) : SearchDestination()

    /**
     * Item detail within search results.
     *
     * Demonstrates path parameter with optional programmatic params.
     * Note: highlightTerm and showRelated are not in the route because they're
     * passed programmatically (not via deep links).
     *
     * @property itemId The ID of the item to display (path param)
     * @property highlightTerm Optional term to highlight in detail view (programmatic only)
     * @property showRelated Whether to show related items section (programmatic only)
     */
    @Destination(route = "search/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String,
        val highlightTerm: String? = null,
        val showRelated: Boolean = true
    ) : SearchDestination()
}
