@file:Suppress("unused")

package com.jermey.quo.vadis.recipes.arguments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
import com.jermey.quo.vadis.core.navigation.core.Navigator

// ============================================================
// TYPE-SAFE ARGUMENTS RECIPE
// ============================================================

/**
 * # Type-Safe Arguments Recipe
 *
 * Comprehensive guide to using the `@Argument` annotation for type-safe navigation
 * parameters in Quo Vadis. This recipe demonstrates all annotation features
 * including custom key mapping, optional parameters, and type conversions.
 *
 * ## What This Recipe Shows
 *
 * 1. **Basic Arguments** - Single and multiple path parameters
 * 2. **Custom Key Mapping** - URL parameter name differs from property name
 * 3. **Optional Arguments** - Query parameters with default values
 * 4. **Enum Type Support** - Type-safe enum serialization
 * 5. **Path vs Query Parameters** - Best practices for parameter placement
 * 6. **Complex Types** - JSON serialization patterns (conceptual)
 *
 * ## @Argument Annotation
 *
 * ```kotlin
 * @Target(AnnotationTarget.VALUE_PARAMETER)
 * @Retention(AnnotationRetention.SOURCE)
 * annotation class Argument(
 *     val key: String = "",       // Custom URL parameter name
 *     val optional: Boolean = false // Whether parameter can be omitted
 * )
 * ```
 *
 * ## Route Template vs @Argument
 *
 * Both approaches work together:
 * - **Route template** `{param}` defines WHERE the parameter appears in the URL
 * - **@Argument** annotation provides additional METADATA about the parameter
 *
 * ```kotlin
 * @Destination(route = "search?q={query}&page={page}")
 * data class Search(
 *     @Argument(key = "query") val searchTerm: String,  // Maps to {query}
 *     @Argument(key = "page", optional = true) val pageNum: Int = 1
 * )
 * ```
 *
 * ## When to Use @Argument
 *
 * | Scenario | @Argument Needed? |
 * |----------|------------------|
 * | Property name matches URL param | Optional (for documentation) |
 * | Property name differs from URL param | **Required** with `key` |
 * | Optional query parameter | **Required** with `optional = true` |
 * | Complex type needing serialization | Recommended for clarity |
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
 *
 * @see Argument
 * @see Destination
 * @see com.jermey.quo.vadis.recipes.masterdetail.CatalogDestination
 */

// ============================================================
// 1. BASIC ARGUMENTS - Single and Multiple Path Parameters
// ============================================================

/**
 * Demonstrates basic `@Argument` usage with single and multiple path parameters.
 *
 * ## Single Path Parameter
 *
 * The simplest case - property name matches URL parameter:
 *
 * ```kotlin
 * @Destination(route = "user/{userId}")
 * data class UserProfile(
 *     @Argument val userId: String  // Maps to {userId}
 * )
 * ```
 *
 * **Deep link**: `myapp://user/12345` → `UserProfile(userId = "12345")`
 *
 * ## Multiple Path Parameters
 *
 * Multiple parameters in a single route:
 *
 * ```kotlin
 * @Destination(route = "user/{userId}/post/{postId}")
 * data class UserPost(
 *     @Argument val userId: String,
 *     @Argument val postId: String
 * )
 * ```
 *
 * **Deep link**: `myapp://user/42/post/789` → `UserPost(userId = "42", postId = "789")`
 *
 * ## Best Practices
 *
 * 1. Use descriptive parameter names (`userId` not `id`)
 * 2. Keep routes RESTful: `resource/{resourceId}`
 * 3. Place required identifiers in the path, not query string
 * 4. Consider URL readability for shared links
 *
 * ## KSP Generation
 *
 * KSP generates deep link parsing that:
 * 1. Matches route pattern against incoming URI
 * 2. Extracts parameter values from path segments
 * 3. Constructs destination instance with extracted values
 */
@Stack(name = "basic_args", startDestination = "UserList")
sealed class BasicArgumentsDestination : DestinationInterface {

    /**
     * User list - no arguments needed.
     *
     * **Deep link**: `myapp://user`
     */
    @Destination(route = "user")
    data object UserList : BasicArgumentsDestination()

    /**
     * Single argument example - user profile by ID.
     *
     * **Deep link**: `myapp://user/{userId}`
     *
     * ## Example URLs
     * - `myapp://user/john-doe` → `UserProfile(userId = "john-doe")`
     * - `myapp://user/12345` → `UserProfile(userId = "12345")`
     *
     * @property userId Unique identifier for the user
     */
    @Destination(route = "user/{userId}")
    data class UserProfile(
        @Argument val userId: String
    ) : BasicArgumentsDestination()

    /**
     * Multiple arguments example - specific post by a user.
     *
     * **Deep link**: `myapp://user/{userId}/post/{postId}`
     *
     * ## Example URLs
     * - `myapp://user/42/post/789` → `UserPost(userId = "42", postId = "789")`
     * - `myapp://user/alice/post/hello-world` → `UserPost(userId = "alice", postId = "hello-world")`
     *
     * ## Navigation Code
     *
     * ```kotlin
     * navigator.navigate(
     *     BasicArgumentsDestination.UserPost(
     *         userId = "alice",
     *         postId = "my-first-post"
     *     )
     * )
     * ```
     *
     * @property userId The user who owns the post
     * @property postId The post identifier
     */
    @Destination(route = "user/{userId}/post/{postId}")
    data class UserPost(
        @Argument val userId: String,
        @Argument val postId: String
    ) : BasicArgumentsDestination()
}

// ============================================================
// 2. CUSTOM KEY MAPPING - URL Param Differs from Property Name
// ============================================================

/**
 * Demonstrates `@Argument(key = "...")` for custom URL parameter mapping.
 *
 * ## Why Custom Keys?
 *
 * Use `key` when:
 * 1. **API compatibility** - External API uses different naming (`q` vs `searchQuery`)
 * 2. **URL brevity** - Short URL params with descriptive Kotlin properties
 * 3. **Migration** - Preserve existing URLs while improving code
 *
 * ## Example: Search API Compatibility
 *
 * Many search APIs use `q` as the query parameter:
 * - Google: `google.com/search?q=kotlin`
 * - GitHub: `github.com/search?q=compose`
 *
 * ```kotlin
 * @Destination(route = "search?q={q}")
 * data class Search(
 *     @Argument(key = "q") val searchQuery: String  // Kotlin: searchQuery, URL: q
 * )
 * ```
 *
 * **Deep link**: `myapp://search?q=navigation` → `Search(searchQuery = "navigation")`
 *
 * ## Key Mapping Rules
 *
 * 1. `key` value must match the route template placeholder
 * 2. If `key` is empty, property name is used
 * 3. Multiple properties can have different keys in the same destination
 *
 * ## Migration Example
 *
 * **Before (matching names, verbose URL):**
 * ```kotlin
 * @Destination(route = "search?searchQuery={searchQuery}")
 * data class Search(val searchQuery: String)
 * // URL: myapp://search?searchQuery=kotlin
 * ```
 *
 * **After (custom key, clean URL):**
 * ```kotlin
 * @Destination(route = "search?q={q}")
 * data class Search(@Argument(key = "q") val searchQuery: String)
 * // URL: myapp://search?q=kotlin
 * ```
 */
@Stack(name = "custom_key", startDestination = "SearchHome")
sealed class CustomKeyDestination : DestinationInterface {

    /**
     * Search home screen.
     *
     * **Deep link**: `myapp://search`
     */
    @Destination(route = "search")
    data object SearchHome : CustomKeyDestination()

    /**
     * Search results with custom key mapping.
     *
     * **Deep link**: `myapp://search/results?q={q}`
     *
     * The `@Argument(key = "q")` maps the short URL parameter `q` to the
     * descriptive Kotlin property `searchQuery`.
     *
     * ## Example URLs
     * - `myapp://search/results?q=kotlin` → `SearchResults(searchQuery = "kotlin")`
     * - `myapp://search/results?q=compose+navigation` → URL-decoded automatically
     *
     * ## Accessing in Screen
     *
     * ```kotlin
     * @Screen(CustomKeyDestination.SearchResults::class)
     * @Composable
     * fun SearchResultsScreen(
     *     destination: CustomKeyDestination.SearchResults,
     *     navigator: Navigator
     * ) {
     *     val query = destination.searchQuery  // Use Kotlin name, not URL key
     * }
     * ```
     *
     * @property searchQuery The search query (mapped from URL param `q`)
     */
    @Destination(route = "search/results?q={q}")
    data class SearchResults(
        @Argument(key = "q") val searchQuery: String
    ) : CustomKeyDestination()

    /**
     * Advanced search with multiple custom key mappings.
     *
     * **Deep link**: `myapp://search/advanced?q={q}&cat={cat}&sort={sort}`
     *
     * Demonstrates multiple custom keys in a single destination:
     * - `q` → `searchQuery`
     * - `cat` → `category`
     * - `sort` → `sortOrder`
     *
     * ## Example URLs
     * - `myapp://search/advanced?q=phone&cat=electronics&sort=price`
     *   → `AdvancedSearch(searchQuery = "phone", category = "electronics", sortOrder = "price")`
     *
     * @property searchQuery The search query text (URL: `q`)
     * @property category Filter by category (URL: `cat`)
     * @property sortOrder Sort results by this field (URL: `sort`)
     */
    @Destination(route = "search/advanced?q={q}&cat={cat}&sort={sort}")
    data class AdvancedSearch(
        @Argument(key = "q") val searchQuery: String,
        @Argument(key = "cat") val category: String,
        @Argument(key = "sort") val sortOrder: String
    ) : CustomKeyDestination()
}

// ============================================================
// 3. OPTIONAL ARGUMENTS - Query Parameters with Defaults
// ============================================================

/**
 * Demonstrates `@Argument(optional = true)` for optional query parameters.
 *
 * ## Optional vs Required Parameters
 *
 * | Parameter Type | @Argument | Kotlin Default | Deep Link |
 * |---------------|-----------|----------------|-----------|
 * | Required path | `optional = false` | None | Must be in URL |
 * | Required query | `optional = false` | None | Must be in URL |
 * | Optional query | `optional = true` | **Required** | Can be omitted |
 *
 * ## Rules for Optional Parameters
 *
 * 1. Must have `optional = true` in the annotation
 * 2. Must have a default value in Kotlin
 * 3. Nullable types should default to `null`
 * 4. Non-nullable types need sensible defaults
 *
 * ## Example: Pagination
 *
 * ```kotlin
 * @Destination(route = "products?page={page}&limit={limit}")
 * data class ProductList(
 *     @Argument(optional = true) val page: Int = 1,
 *     @Argument(optional = true) val limit: Int = 20
 * )
 * ```
 *
 * **Deep links:**
 * - `myapp://products` → `ProductList(page = 1, limit = 20)`
 * - `myapp://products?page=3` → `ProductList(page = 3, limit = 20)`
 * - `myapp://products?page=2&limit=50` → `ProductList(page = 2, limit = 50)`
 *
 * ## Mixing Required and Optional
 *
 * Path parameters are typically required, query parameters optional:
 *
 * ```kotlin
 * @Destination(route = "category/{categoryId}/products?page={page}")
 * data class CategoryProducts(
 *     @Argument val categoryId: String,           // Required (path)
 *     @Argument(optional = true) val page: Int = 1  // Optional (query)
 * )
 * ```
 */
@Stack(name = "optional_args", startDestination = "ProductsHome")
sealed class OptionalArgumentsDestination : DestinationInterface {

    /**
     * Products home screen.
     *
     * **Deep link**: `myapp://products`
     */
    @Destination(route = "products")
    data object ProductsHome : OptionalArgumentsDestination()

    /**
     * Paginated product list with optional pagination parameters.
     *
     * **Deep link**: `myapp://products/list?page={page}&limit={limit}`
     *
     * Both `page` and `limit` are optional with sensible defaults.
     *
     * ## Example URLs
     * - `myapp://products/list` → `ProductList(page = 1, limit = 20)`
     * - `myapp://products/list?page=5` → `ProductList(page = 5, limit = 20)`
     * - `myapp://products/list?limit=100` → `ProductList(page = 1, limit = 100)`
     * - `myapp://products/list?page=2&limit=50` → `ProductList(page = 2, limit = 50)`
     *
     * @property page Current page number (1-indexed), defaults to 1
     * @property limit Items per page, defaults to 20
     */
    @Destination(route = "products/list?page={page}&limit={limit}")
    data class ProductList(
        @Argument(optional = true) val page: Int = 1,
        @Argument(optional = true) val limit: Int = 20
    ) : OptionalArgumentsDestination()

    /**
     * Product detail with required ID and optional referrer tracking.
     *
     * **Deep link**: `myapp://products/detail/{productId}?ref={ref}&promo={promo}`
     *
     * Demonstrates mixing required path parameters with optional query parameters.
     *
     * ## Example URLs
     * - `myapp://products/detail/SKU-123` → `ProductDetail(productId = "SKU-123", referrer = null, promoCode = null)`
     * - `myapp://products/detail/SKU-123?ref=email` → tracking referrer
     * - `myapp://products/detail/SKU-123?promo=SAVE20` → apply promo code
     * - `myapp://products/detail/SKU-123?ref=social&promo=SUMMER` → both
     *
     * ## Use Cases
     *
     * - **referrer**: Track where user came from (analytics)
     * - **promoCode**: Apply discount automatically from link
     *
     * @property productId Required product identifier (path parameter)
     * @property referrer Optional tracking source (query parameter)
     * @property promoCode Optional promotional code (query parameter)
     */
    @Destination(route = "products/detail/{productId}?ref={ref}&promo={promo}")
    data class ProductDetail(
        @Argument val productId: String,
        @Argument(key = "ref", optional = true) val referrer: String? = null,
        @Argument(key = "promo", optional = true) val promoCode: String? = null
    ) : OptionalArgumentsDestination()

    /**
     * Filtered product listing with all optional filters.
     *
     * **Deep link**: `myapp://products/filter?category={category}&minPrice={minPrice}&maxPrice={maxPrice}&inStock={inStock}`
     *
     * ## Example URLs
     * - `myapp://products/filter` → All defaults
     * - `myapp://products/filter?category=electronics` → Filter by category
     * - `myapp://products/filter?minPrice=100&maxPrice=500` → Price range
     * - `myapp://products/filter?inStock=true` → Only in-stock items
     *
     * @property category Filter by category, null means all categories
     * @property minPrice Minimum price filter, null means no minimum
     * @property maxPrice Maximum price filter, null means no maximum
     * @property inStock Filter by stock availability, null means show all
     */
    @Destination(route = "products/filter?category={category}&minPrice={minPrice}&maxPrice={maxPrice}&inStock={inStock}")
    data class FilteredProducts(
        @Argument(optional = true) val category: String? = null,
        @Argument(optional = true) val minPrice: Int? = null,
        @Argument(optional = true) val maxPrice: Int? = null,
        @Argument(optional = true) val inStock: Boolean? = null
    ) : OptionalArgumentsDestination()
}

// ============================================================
// 4. ENUM TYPE SUPPORT - Type-Safe Enum Serialization
// ============================================================

/**
 * Sort direction for ordering results.
 *
 * ## Serialization
 *
 * Enums are serialized using their `.name` property:
 * - `SortDirection.ASCENDING` → `"ASCENDING"`
 * - `SortDirection.DESCENDING` → `"DESCENDING"`
 *
 * Deep link parsing uses `enumValueOf<T>(value)` for deserialization.
 */
enum class SortDirection {
    ASCENDING,
    DESCENDING
}

/**
 * Sort field options for product listing.
 */
enum class SortField {
    NAME,
    PRICE,
    RATING,
    DATE_ADDED
}

/**
 * Product availability status.
 */
enum class AvailabilityStatus {
    IN_STOCK,
    OUT_OF_STOCK,
    PRE_ORDER,
    DISCONTINUED
}

/**
 * Demonstrates enum type arguments for type-safe navigation parameters.
 *
 * ## Enum Serialization
 *
 * Enums are automatically serialized/deserialized:
 * - **Serialization**: `enum.name` (e.g., `ASCENDING`)
 * - **Deserialization**: `enumValueOf<T>(value)`
 * - **Case sensitivity**: Enum values are case-sensitive in URLs
 *
 * ## Example
 *
 * ```kotlin
 * @Destination(route = "articles?sort={sort}")
 * data class ArticleList(
 *     @Argument val sort: SortDirection
 * )
 * ```
 *
 * **Deep link**: `myapp://articles?sort=ASCENDING` → `ArticleList(sort = SortDirection.ASCENDING)`
 *
 * ## Benefits of Enum Arguments
 *
 * 1. **Compile-time safety** - Invalid values caught at compile time
 * 2. **IDE support** - Autocomplete for valid values
 * 3. **Self-documenting** - Enum defines all valid options
 * 4. **Exhaustive when** - Compiler ensures all cases handled
 *
 * ## URL Considerations
 *
 * Enum names appear directly in URLs. Consider:
 * - Keep names URL-friendly (no spaces, special chars)
 * - Use SCREAMING_SNAKE_CASE or lowercase for consistency
 * - Document valid values for API consumers
 */
@Stack(name = "enum_args", startDestination = "ArticleList")
sealed class EnumArgumentsDestination : DestinationInterface {

    /**
     * Article list with enum-based sorting.
     *
     * **Deep link**: `myapp://articles?sort={sort}&order={order}`
     *
     * ## Example URLs
     * - `myapp://articles?sort=DATE_ADDED&order=DESCENDING` → Latest first
     * - `myapp://articles?sort=RATING&order=DESCENDING` → Top rated
     * - `myapp://articles?sort=NAME&order=ASCENDING` → Alphabetical
     *
     * ## Navigation Code
     *
     * ```kotlin
     * navigator.navigate(
     *     EnumArgumentsDestination.SortedArticles(
     *         sortField = SortField.RATING,
     *         sortDirection = SortDirection.DESCENDING
     *     )
     * )
     * ```
     *
     * @property sortField Which field to sort by
     * @property sortDirection Ascending or descending order
     */
    @Destination(route = "articles?sort={sort}&order={order}")
    data class SortedArticles(
        @Argument(key = "sort") val sortField: SortField,
        @Argument(key = "order") val sortDirection: SortDirection
    ) : EnumArgumentsDestination()

    /**
     * Default article list - no sorting parameters.
     *
     * **Deep link**: `myapp://articles`
     */
    @Destination(route = "articles")
    data object ArticleList : EnumArgumentsDestination()

    /**
     * Product listing filtered by availability status.
     *
     * **Deep link**: `myapp://inventory/{status}`
     *
     * ## Example URLs
     * - `myapp://inventory/IN_STOCK` → Show available products
     * - `myapp://inventory/PRE_ORDER` → Show pre-order items
     * - `myapp://inventory/DISCONTINUED` → Show discontinued
     *
     * ## Path Parameter Enum
     *
     * Unlike query parameters, path enums are part of the URL structure.
     * This creates distinct, bookmarkable URLs for each status.
     *
     * @property status Filter products by availability
     */
    @Destination(route = "inventory/{status}")
    data class InventoryByStatus(
        @Argument val status: AvailabilityStatus
    ) : EnumArgumentsDestination()

    /**
     * Optional enum with default value.
     *
     * **Deep link**: `myapp://catalog?availability={availability}`
     *
     * ## Example URLs
     * - `myapp://catalog` → `CatalogFilter(availability = AvailabilityStatus.IN_STOCK)`
     * - `myapp://catalog?availability=PRE_ORDER` → Show pre-orders
     *
     * @property availability Filter by status, defaults to IN_STOCK
     */
    @Destination(route = "catalog?availability={availability}")
    data class CatalogFilter(
        @Argument(optional = true) val availability: AvailabilityStatus = AvailabilityStatus.IN_STOCK
    ) : EnumArgumentsDestination()
}

// ============================================================
// 5. PATH VS QUERY PARAMETERS - Best Practices
// ============================================================

/**
 * Demonstrates best practices for choosing between path and query parameters.
 *
 * ## Path Parameters (Required, Resource Identity)
 *
 * Use for:
 * - Resource identifiers (`/user/{userId}`)
 * - Hierarchical relationships (`/category/{catId}/product/{prodId}`)
 * - Required parameters that define WHAT resource
 *
 * ## Query Parameters (Optional, Modifiers)
 *
 * Use for:
 * - Filtering (`?category=electronics`)
 * - Sorting (`?sort=price&order=asc`)
 * - Pagination (`?page=2&limit=20`)
 * - Optional parameters that define HOW to show resource
 *
 * ## Decision Matrix
 *
 * | Question | Path | Query |
 * |----------|------|-------|
 * | Is it a resource identifier? | ✓ | |
 * | Can it be omitted? | | ✓ |
 * | Does it filter/modify the view? | | ✓ |
 * | Is it hierarchical? | ✓ | |
 * | Should URL be bookmarkable with this value? | ✓ | Optional |
 *
 * ## Examples
 *
 * **Good**: `/user/123/orders?status=pending&page=2`
 * - Path: user ID (required, identifies resource)
 * - Query: status filter, pagination (optional modifiers)
 *
 * **Avoid**: `/user?id=123` (ID should be path)
 * **Avoid**: `/user/123/pending/page/2` (filters should be query)
 */
@Stack(name = "path_query", startDestination = "Dashboard")
sealed class PathQueryDestination : DestinationInterface {

    /**
     * Dashboard home.
     *
     * **Deep link**: `myapp://dashboard`
     */
    @Destination(route = "dashboard")
    data object Dashboard : PathQueryDestination()

    /**
     * Well-structured URL combining path and query parameters.
     *
     * **Deep link**: `myapp://orders/{orderId}/items?highlight={highlight}`
     *
     * ## URL Structure
     * - **Path**: `orderId` - identifies the specific order (required)
     * - **Query**: `highlight` - optional UI hint (which item to scroll to)
     *
     * ## Example URLs
     * - `myapp://orders/ORD-123/items` → View all items in order
     * - `myapp://orders/ORD-123/items?highlight=ITEM-5` → Scroll to specific item
     *
     * ## Why This Structure?
     *
     * 1. `orderId` in path: Essential to identify the resource
     * 2. `highlight` in query: Optional UI behavior, not resource identity
     *
     * @property orderId Order identifier (required, path)
     * @property highlightItemId Item to highlight/scroll to (optional, query)
     */
    @Destination(route = "orders/{orderId}/items?highlight={highlight}")
    data class OrderItems(
        @Argument val orderId: String,
        @Argument(key = "highlight", optional = true) val highlightItemId: String? = null
    ) : PathQueryDestination()

    /**
     * Demonstrates proper RESTful hierarchy.
     *
     * **Deep link**: `myapp://users/{userId}/orders/{orderId}`
     *
     * ## Hierarchical Path Parameters
     *
     * Shows ownership: User → Orders
     * - User `userId` owns the order
     * - Order `orderId` belongs to that user
     *
     * This enables:
     * - Authorization checks (user can only see their orders)
     * - Proper back navigation (back goes to user's order list)
     * - Clear URL semantics for sharing
     *
     * @property userId User who owns the order
     * @property orderId Specific order to display
     */
    @Destination(route = "users/{userId}/orders/{orderId}")
    data class UserOrder(
        @Argument val userId: String,
        @Argument val orderId: String
    ) : PathQueryDestination()

    /**
     * Search with all query parameters (no path identifiers).
     *
     * **Deep link**: `myapp://search?q={q}&type={type}&from={from}&size={size}`
     *
     * ## All Query Parameters
     *
     * Search endpoints often use only query parameters because:
     * - No single resource is being identified
     * - All parameters modify the search behavior
     * - Users may want to bookmark searches with any combination
     *
     * ## Example URLs
     * - `myapp://search?q=kotlin` → Basic search
     * - `myapp://search?q=kotlin&type=articles` → Filtered search
     * - `myapp://search?q=kotlin&from=20&size=10` → Paginated search
     *
     * @property query Search text (required for meaningful search)
     * @property type Content type filter
     * @property from Pagination offset
     * @property size Results per page
     */
    @Destination(route = "search?q={q}&type={type}&from={from}&size={size}")
    data class SearchResults(
        @Argument(key = "q") val query: String,
        @Argument(key = "type", optional = true) val type: String? = null,
        @Argument(key = "from", optional = true) val from: Int = 0,
        @Argument(key = "size", optional = true) val size: Int = 20
    ) : PathQueryDestination()
}

// ============================================================
// 6. COMPLEX TYPES - JSON Serialization (Conceptual)
// ============================================================

/**
 * Filter options for complex search scenarios.
 *
 * ## Serialization Requirements
 *
 * Complex types require `@Serializable` annotation from kotlinx.serialization:
 *
 * ```kotlin
 * @Serializable
 * data class FilterOptions(
 *     val categories: List<String>,
 *     val priceRange: PriceRange?,
 *     val brands: Set<String>
 * )
 * ```
 *
 * ## How It Works
 *
 * 1. Object is serialized to JSON string
 * 2. JSON is URL-encoded for safe transmission
 * 3. On receipt, JSON is decoded and deserialized
 *
 * ## Example URL
 *
 * ```
 * myapp://products/search?filters=%7B%22categories%22%3A%5B%22electronics%22%5D%7D
 * ```
 * (URL-encoded JSON: `{"categories":["electronics"]}`)
 *
 * ## Note
 *
 * This example is conceptual. To use complex types:
 * 1. Add kotlinx.serialization dependency
 * 2. Add `@Serializable` annotation
 * 3. KSP generates JSON serialization code
 */
// NOTE: @Serializable would be added in production code
// @Serializable
data class FilterOptions(
    val categories: List<String> = emptyList(),
    val minPrice: Int? = null,
    val maxPrice: Int? = null,
    val brands: List<String> = emptyList(),
    val inStockOnly: Boolean = false
)

/**
 * Demonstrates complex type arguments (conceptual).
 *
 * ## Complex Type Serialization
 *
 * For complex types beyond primitives/enums, Quo Vadis uses JSON serialization:
 *
 * ```kotlin
 * @Destination(route = "products/filtered?options={options}")
 * data class FilteredProducts(
 *     @Argument val options: FilterOptions  // JSON-serialized
 * )
 * ```
 *
 * ## Requirements
 *
 * 1. Add kotlinx.serialization to your project
 * 2. Annotate complex types with `@Serializable`
 * 3. KSP generates serialization glue code
 *
 * ## When to Use
 *
 * | Scenario | Recommendation |
 * |----------|----------------|
 * | Few simple filters | Individual parameters |
 * | Many filter options | Complex type |
 * | Nested structures | Complex type |
 * | API shares filter format | Complex type |
 *
 * ## Alternative: Multiple Optional Parameters
 *
 * Instead of complex types, you can use many optional params:
 *
 * ```kotlin
 * @Destination(route = "products?cat={cat}&minPrice={min}&maxPrice={max}")
 * data class FilteredProducts(
 *     @Argument(key = "cat", optional = true) val category: String? = null,
 *     @Argument(key = "min", optional = true) val minPrice: Int? = null,
 *     @Argument(key = "max", optional = true) val maxPrice: Int? = null
 * )
 * ```
 *
 * This is simpler but becomes unwieldy with many parameters.
 */
@Stack(name = "complex_types", startDestination = "ProductSearch")
sealed class ComplexTypeDestination : DestinationInterface {

    /**
     * Product search home.
     *
     * **Deep link**: `myapp://products/search`
     */
    @Destination(route = "products/search")
    data object ProductSearch : ComplexTypeDestination()

    /**
     * Filtered products with complex filter object (conceptual).
     *
     * **Deep link**: `myapp://products/filtered?filters={filters}`
     *
     * ## Note
     *
     * This is a conceptual example. In production:
     * 1. Add `@Serializable` to `FilterOptions`
     * 2. Add kotlinx.serialization dependency
     * 3. KSP generates the JSON handling
     *
     * ## Example Deep Link (URL-encoded)
     *
     * ```
     * myapp://products/filtered?filters=%7B%22categories%22%3A%5B%22electronics%22%2C%22computers%22%5D%2C%22minPrice%22%3A100%2C%22maxPrice%22%3A1000%2C%22inStockOnly%22%3Atrue%7D
     * ```
     *
     * Decoded JSON:
     * ```json
     * {
     *   "categories": ["electronics", "computers"],
     *   "minPrice": 100,
     *   "maxPrice": 1000,
     *   "inStockOnly": true
     * }
     * ```
     *
     * ## Navigation Code
     *
     * ```kotlin
     * navigator.navigate(
     *     ComplexTypeDestination.FilteredProducts(
     *         filters = FilterOptions(
     *             categories = listOf("electronics", "computers"),
     *             minPrice = 100,
     *             maxPrice = 1000,
     *             inStockOnly = true
     *         )
     *     )
     * )
     * ```
     *
     * @property filters Complex filter options (JSON-serialized)
     */
    // NOTE: In production, FilterOptions would have @Serializable annotation
    @Destination(route = "products/filtered?filters={filters}")
    data class FilteredProducts(
        @Argument val filters: FilterOptions
    ) : ComplexTypeDestination()
}

// ============================================================
// SCREEN COMPOSABLES
// ============================================================

/**
 * User profile screen demonstrating basic argument access.
 *
 * For destinations **with** parameters (data classes), the @Screen function
 * receives the destination instance as the first parameter.
 *
 * ## Accessing Arguments
 *
 * Arguments are accessed directly as properties on the destination:
 *
 * ```kotlin
 * val userId = destination.userId  // Direct property access
 * ```
 *
 * @param destination The destination instance containing navigation arguments
 * @param navigator The navigator for navigation actions
 */
@Screen(BasicArgumentsDestination.UserProfile::class)
@Composable
fun UserProfileScreen(
    destination: BasicArgumentsDestination.UserProfile,
    navigator: Navigator
) {
    // Access argument from destination instance
    val userId = destination.userId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "User Profile",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArgumentRow(label = "User ID", value = userId)
                ArgumentRow(
                    label = "Deep Link",
                    value = "myapp://user/$userId"
                )
                ArgumentRow(
                    label = "Argument Type",
                    value = "String (required path parameter)"
                )
            }
        }

        TextButton(onClick = { navigator.navigateBack() }) {
            Text("← Back")
        }
    }
}

/**
 * Product detail screen demonstrating optional argument access.
 *
 * Shows how to handle optional arguments with nullable types and defaults.
 *
 * ## Optional Argument Handling
 *
 * ```kotlin
 * val referrer = destination.referrer ?: "direct"  // Use default if null
 * destination.promoCode?.let { applyPromo(it) }    // Conditional logic
 * ```
 *
 * @param destination The destination with required and optional arguments
 * @param navigator The navigator for navigation actions
 */
@Screen(OptionalArgumentsDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: OptionalArgumentsDestination.ProductDetail,
    navigator: Navigator
) {
    // Access required argument
    val productId = destination.productId

    // Access optional arguments (may be null)
    val referrer = destination.referrer
    val promoCode = destination.promoCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Product Detail",
            style = MaterialTheme.typography.headlineMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Required Arguments",
                    style = MaterialTheme.typography.titleMedium
                )
                ArgumentRow(label = "Product ID", value = productId)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Optional Arguments",
                    style = MaterialTheme.typography.titleMedium
                )
                ArgumentRow(
                    label = "Referrer",
                    value = referrer ?: "(not provided)"
                )
                ArgumentRow(
                    label = "Promo Code",
                    value = promoCode ?: "(not provided)"
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Example Deep Links",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• myapp://products/detail/$productId",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• myapp://products/detail/$productId?ref=email",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• myapp://products/detail/$productId?promo=SAVE20",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        TextButton(onClick = { navigator.navigateBack() }) {
            Text("← Back")
        }
    }
}

/**
 * Helper composable for displaying argument key-value pairs.
 */
@Composable
private fun ArgumentRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

// ============================================================
// APP ENTRY POINT
// ============================================================

/**
 * Entry point for the Type-Safe Arguments recipe.
 *
 * ## Production Setup (After KSP Processing)
 *
 * ```kotlin
 * @Composable
 * fun TypeSafeArgumentsApp() {
 *     // KSP generates builders from @Stack annotations
 *     val navTree = remember { buildBasicArgumentsNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry
 *     )
 * }
 * ```
 *
 * ## Testing Different Argument Types
 *
 * This recipe includes multiple @Stack definitions to demonstrate
 * different argument patterns. In production, use the appropriate
 * builder for your navigation structure:
 *
 * - `buildBasicArgumentsNavNode()` - Basic path parameters
 * - `buildCustomKeyNavNode()` - Custom key mapping
 * - `buildOptionalArgumentsNavNode()` - Optional query parameters
 * - `buildEnumArgumentsNavNode()` - Enum type arguments
 * - `buildPathQueryNavNode()` - Path vs query best practices
 * - `buildComplexTypesNavNode()` - Complex type serialization
 *
 * ## Deep Link Testing
 *
 * ```kotlin
 * // Parse deep link
 * val destination = GeneratedDeepLinkHandler.parse("myapp://user/123/post/456")
 * // Result: BasicArgumentsDestination.UserPost(userId = "123", postId = "456")
 *
 * // Navigate via deep link
 * navigator.handleDeepLink(DeepLink("myapp://user/123"))
 * ```
 */
@Composable
fun TypeSafeArgumentsApp() {
    // This is a conceptual placeholder.
    // In production, KSP generates builders from @Stack/@Destination annotations.
    //
    // Usage:
    //   val navTree = remember { buildBasicArgumentsNavNode() }
    //   val navigator = rememberNavigator(navTree)
    //   QuoVadisHost(
    //       navigator = navigator,
    //       screenRegistry = GeneratedScreenRegistry
    //   )

    Text("TypeSafeArgumentsApp - See KDoc for production implementation")
}

// ============================================================
// MIGRATION CHECKLIST
// ============================================================

/**
 * Migration checklist for adding type-safe arguments to destinations.
 *
 * ## When to Add @Argument Annotation
 *
 * | Scenario | Add @Argument? | Configuration |
 * |----------|---------------|---------------|
 * | Property name matches URL param | Optional | `@Argument val foo: String` |
 * | Property name differs from URL | **Required** | `@Argument(key = "url_name")` |
 * | Optional query parameter | **Required** | `@Argument(optional = true)` |
 * | Complex/serializable type | Recommended | For documentation |
 *
 * ## key Parameter Rules
 *
 * 1. If `key` is empty (default), property name is used
 * 2. `key` must match placeholder in route template: `route = "?q={q}"` → `key = "q"`
 * 3. Use `key` for API compatibility (`q` vs `searchQuery`)
 * 4. Keep URLs short, Kotlin names descriptive
 *
 * ## optional Parameter Rules
 *
 * 1. `optional = false` (default): Parameter required in URL
 * 2. `optional = true`: Parameter can be omitted, MUST have Kotlin default
 * 3. Nullable types with `optional = true` should default to `null`
 * 4. Non-nullable optional params need sensible non-null defaults
 *
 * ## Enum Handling
 *
 * 1. Enums serialize using `.name` property
 * 2. URL values are case-sensitive (`ASCENDING` not `ascending`)
 * 3. Invalid enum values throw exception - consider error handling
 * 4. Optional enums: `@Argument(optional = true) val sort: SortDir = SortDir.ASC`
 *
 * ## Complex Type Handling
 *
 * 1. Add kotlinx.serialization dependency
 * 2. Annotate type with `@Serializable`
 * 3. Complex types are JSON-encoded in URLs
 * 4. Consider URL length limits for deeply nested objects
 *
 * ## Common Migration Patterns
 *
 * ### From Separate Data Class
 *
 * **Before:**
 * ```kotlin
 * data class ProfileArgs(val userId: String)
 *
 * @Route("profile")
 * @Argument(ProfileArgs::class)
 * data class Profile(val userId: String) : Destination,
 *     TypedDestination<ProfileArgs> {
 *     override val data = ProfileArgs(userId)
 * }
 * ```
 *
 * **After:**
 * ```kotlin
 * @Destination(route = "profile/{userId}")
 * data class Profile(
 *     @Argument val userId: String
 * ) : Destination
 * ```
 *
 * ### From Query String Parsing
 *
 * **Before:**
 * ```kotlin
 * @Route("search")
 * data class Search(val query: String) : Destination {
 *     companion object {
 *         fun fromQueryString(qs: Map<String, String>): Search {
 *             return Search(qs["q"] ?: "")
 *         }
 *     }
 * }
 * ```
 *
 * **After:**
 * ```kotlin
 * @Destination(route = "search?q={q}")
 * data class Search(
 *     @Argument(key = "q") val query: String
 * ) : Destination
 * ```
 */
object TypeSafeArgumentsMigrationChecklist {

    /**
     * Checklist items for @Argument migration.
     */
    val checklistItems = listOf(
        "1. Identify all constructor parameters that come from navigation",
        "2. Add @Argument to parameters that need custom key or optional flag",
        "3. Update route template to include all parameters",
        "4. Add default values for optional parameters",
        "5. Update screen functions to access properties directly",
        "6. Test deep link parsing with various URL formats",
        "7. Verify enum values serialize/deserialize correctly",
        "8. Add @Serializable to complex types (requires dependency)"
    )

    /**
     * Common mistakes to avoid.
     */
    val commonMistakes = listOf(
        "❌ Forgetting default value for optional = true parameters",
        "❌ Mismatched key name and route template placeholder",
        "❌ Using optional = true for path parameters (they're always required)",
        "❌ Case mismatch in enum values (ASCENDING vs ascending)",
        "❌ Missing @Serializable on complex types"
    )
}
