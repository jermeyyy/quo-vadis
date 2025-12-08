`n
# Task ANN-006: Define @Argument Parameter Annotation

## Metadata

| Field | Value |
|-------|-------|
| **Task ID** | ANN-006 |
| **Name** | Define @Argument Parameter Annotation |
| **Phase** | 4 - Annotations |
| **Complexity** | Low-Medium |
| **Estimated Time** | 1 day |
| **Dependencies** | ANN-001 (@Destination) |

## Overview

Create the `@Argument` parameter-level annotation that explicitly marks constructor parameters of `@Destination` data classes as navigation arguments. This annotation enables:

1. **Explicit argument declaration** - distinguishes navigation args from internal state
2. **Custom serialization keys** - for deep linking URL parameter names
3. **Optional argument support** - arguments not required in deep links
4. **Type-safe code generation** - KSP generates proper serializers for each type

### Replaces Deprecated @Argument

The old class-level `@Argument(dataClass: KClass<*>)` annotation from the TypedDestination pattern is **removed** and replaced with this parameter-level annotation.

## Implementation

### Annotation Definition

```kotlin
// quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Argument.kt

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
```

## Usage Examples

### Example 1: E-Commerce Product Detail

```kotlin
@Stack(name = "shop", startDestination = "Catalog")
sealed class ShopDestination {
    
    @Destination(route = "shop/catalog")
    data object Catalog : ShopDestination()
    
    @Destination(route = "shop/product/{productId}")
    data class ProductDetail(
        @Argument val productId: String,
        @Argument(optional = true) val variant: String? = null,
        @Argument(optional = true) val showReviews: Boolean = false
    ) : ShopDestination()
    
    @Destination(route = "shop/cart")
    data object Cart : ShopDestination()
}

// Navigation
navigator.navigate(ShopDestination.ProductDetail(
    productId = "SKU-12345",
    variant = "blue-xl",
    showReviews = true
))

// Deep link parsing
// URL: myapp://shop/product/SKU-12345?variant=blue-xl&showReviews=true
```

### Example 2: Multi-Step Form with Complex State

```kotlin
@Serializable
data class FormProgress(
    val completedSteps: List<Int>,
    val currentData: Map<String, String>
)

@Stack(name = "onboarding", startDestination = "Welcome")
sealed class OnboardingDestination {
    
    @Destination
    data object Welcome : OnboardingDestination()
    
    @Destination(route = "onboarding/step/{stepNumber}")
    data class Step(
        @Argument val stepNumber: Int,
        @Argument(optional = true) val progress: FormProgress? = null
    ) : OnboardingDestination()
    
    @Destination
    data object Complete : OnboardingDestination()
}
```

### Example 3: Search with Filters

```kotlin
@Serializable
data class SearchFilters(
    val categories: List<String> = emptyList(),
    val priceMin: Double? = null,
    val priceMax: Double? = null,
    val sortBy: SortOption = SortOption.RELEVANCE
)

@Serializable
enum class SortOption { RELEVANCE, PRICE_LOW, PRICE_HIGH, NEWEST }

@Stack(name = "search", startDestination = "SearchHome")
sealed class SearchDestination {
    
    @Destination(route = "search")
    data object SearchHome : SearchDestination()
    
    @Destination(route = "search/results")
    data class Results(
        @Argument val query: String,
        @Argument(optional = true) val filters: SearchFilters = SearchFilters(),
        @Argument(key = "p", optional = true) val page: Int = 1
    ) : SearchDestination()
}

// Navigation
navigator.navigate(SearchDestination.Results(
    query = "kotlin books",
    filters = SearchFilters(
        categories = listOf("books", "ebooks"),
        sortBy = SortOption.PRICE_LOW
    )
))
```

## Screen Access Pattern

With the **Direct Parameter Pattern** (selected approach), screens receive the full destination object:

```kotlin
@Screen(ShopDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: ShopDestination.ProductDetail,
    navigator: Navigator
) {
    // Direct property access - full IDE support, compile-time safety
    val productId = destination.productId
    val variant = destination.variant
    val showReviews = destination.showReviews
    
    ProductDetailContent(
        productId = productId,
        variant = variant,
        showReviews = showReviews,
        onBack = { navigator.navigateBack() }
    )
}
```

## Validation Rules (KSP)

The KSP processor validates `@Argument` usage:

| Rule | Error Message |
|------|---------------|
| Optional argument without default | `@Argument(optional = true) on 'param' requires a default value` |
| Path parameter marked optional | `Path parameter '{param}' cannot be optional` |
| Key mismatch with route | `@Argument key 'foo' not found in route pattern` |
| Unsupported type without @Serializable | `Type 'CustomClass' requires @Serializable annotation` |
| Duplicate keys | `Duplicate argument key 'id' in destination` |

## KSP Integration

### Updated ParamInfo Model

```kotlin
data class ParamInfo(
    val name: String,
    val type: KSType,
    val hasDefault: Boolean,
    // NEW fields for @Argument
    val isArgument: Boolean,           // Has @Argument annotation
    val argumentKey: String,           // key from @Argument or param name
    val isOptionalArgument: Boolean,   // @Argument(optional = true)
    val serializerType: SerializerType // How to serialize for deep links
)

enum class SerializerType {
    STRING,        // Direct toString()
    INT,           // toInt() parsing
    LONG,          // toLong() parsing
    FLOAT,         // toFloat() parsing  
    DOUBLE,        // toDouble() parsing
    BOOLEAN,       // "true"/"false" parsing
    ENUM,          // enumValueOf<T>()
    JSON           // kotlinx.serialization Json
}
```

### Extractor Changes

`DestinationExtractor` must:
1. Check each constructor parameter for `@Argument` annotation
2. Extract `key` and `optional` values
3. Determine `SerializerType` from parameter type
4. Validate against route pattern

## Files Affected

| File | Change Type | Description |
|------|-------------|-------------|
| `quo-vadis-annotations/.../Argument.kt` | **Modify** | Replace class-level with parameter-level annotation |
| `quo-vadis-annotations/.../Annotations.kt` | **Modify** | Remove deprecated `@Argument(dataClass)` |
| `quo-vadis-ksp/.../models/ParamInfo.kt` | **Modify** | Add argument metadata fields |
| `quo-vadis-ksp/.../extractors/DestinationExtractor.kt` | **Modify** | Extract @Argument from parameters |
| `quo-vadis-ksp/.../validation/ValidationEngine.kt` | **Modify** | Add argument validation rules |

## Acceptance Criteria

- [ ] Old class-level `@Argument(dataClass: KClass<*>)` removed
- [ ] New parameter-level `@Argument(key, optional)` created
- [ ] `@Target` is `AnnotationTarget.VALUE_PARAMETER`
- [ ] `@Retention` is `AnnotationRetention.SOURCE`
- [ ] Comprehensive KDoc with all usage examples
- [ ] Default values: `key = ""`, `optional = false`
- [ ] Accessible from all KMP platforms (commonMain)
- [ ] KSP extractor updated to read @Argument
- [ ] Validation rules implemented in ValidationEngine
- [ ] Unit tests for annotation retention and target

## Migration from Old @Argument

### Before (Deprecated)
```kotlin
@Serializable
data class DetailData(val itemId: String)

@Route("detail")
@Argument(DetailData::class)  // Class-level
data class Detail(val itemId: String) 
    : Destination, TypedDestination<DetailData> {
    override val data = DetailData(itemId)
}

@Content(Detail::class)
@Composable
fun DetailContent(data: DetailData, navigator: Navigator) {
    Text("Item: ${data.itemId}")
}
```

### After (New)
```kotlin
@Destination(route = "detail/{itemId}")
data class Detail(
    @Argument val itemId: String  // Parameter-level
) : ShopDestination()

@Screen(ShopDestination.Detail::class)
@Composable
fun DetailScreen(destination: ShopDestination.Detail, navigator: Navigator) {
    Text("Item: ${destination.itemId}")
}
```

## References

- [INDEX.md](../INDEX.md) - Refactoring Plan Index
- [ANN-001](./ANN-001-graph-type.md) - @Destination annotation
- [KSP-001](../phase3-ksp/KSP-001-graph-type-enum.md) - Extractors (ParamInfo)
- [KSP-004](../phase3-ksp/KSP-004-deep-link-handler.md) - Deep Link Handler
- [OPTION_B_DIRECT_DESTINATION_PARAMETER.md](../../examples/OPTION_B_DIRECT_DESTINATION_PARAMETER.md) - Usage examples

````
