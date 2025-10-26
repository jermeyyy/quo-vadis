# TypedDestination Guide

## Overview

**TypedDestination** is a powerful feature in Quo Vadis that provides compile-time type-safety for navigation arguments using serializable data classes. Instead of manually parsing string-based arguments, you work with strongly-typed data objects that are automatically serialized and deserialized.

### The Problem: Manual Argument Parsing

Traditional navigation often requires manual argument handling:

```kotlin
// ❌ Manual approach - error-prone
destination(SimpleDestination("product")) { dest, navigator ->
    val productId = dest.arguments["productId"] as? String ?: ""
    val category = dest.arguments["category"] as? String ?: "unknown"
    val price = (dest.arguments["price"] as? String)?.toDoubleOrNull() ?: 0.0
    
    ProductScreen(
        productId = productId,
        category = category,
        price = price,
        navigator = navigator
    )
}
```

**Problems:**
- ❌ Manual type casting required
- ❌ No compile-time safety
- ❌ Null handling boilerplate
- ❌ Easy to misspell argument keys
- ❌ Type conversion errors at runtime

### The Solution: TypedDestination

With `TypedDestination`, you define a serializable data class and let the library handle serialization automatically:

```kotlin
// ✅ TypedDestination approach - type-safe
@Serializable
data class ProductData(
    val productId: String,
    val category: String,
    val price: Double
)

@Route("product")
@Argument(ProductData::class)
data class Product(
    val productId: String,
    val category: String,
    val price: Double
) : Destination, TypedDestination<ProductData> {
    override val data = ProductData(productId, category, price)
}

// Content function receives typed data
@Content(Product::class)
@Composable
fun ProductContent(data: ProductData, navigator: Navigator) {
    ProductScreen(
        productId = data.productId,
        category = data.category,
        price = data.price,
        navigator = navigator
    )
}
```

**Benefits:**
- ✅ Compile-time type safety
- ✅ Automatic serialization/deserialization
- ✅ IDE autocompletion
- ✅ Refactoring support
- ✅ No manual null handling
- ✅ Works across all platforms

---

## TypedDestination Interface

### Interface Definition

```kotlin
interface TypedDestination<T : Any> : Destination {
    val data: T
}
```

**Type Parameter:**
- `T`: The serializable data class type (must be annotated with `@Serializable`)

**Property:**
- `data`: The typed data object for this destination

### Requirements

For a destination to implement `TypedDestination<T>`:

1. **Data class must be serializable:**
   ```kotlin
   @Serializable
   data class YourData(...)
   ```

2. **Destination must implement the interface:**
   ```kotlin
   data class YourDestination(...) : Destination, TypedDestination<YourData>
   ```

3. **Override the data property:**
   ```kotlin
   override val data = YourData(...)
   ```

4. **kotlinx.serialization must be configured** (see Setup section)

---

## Using with Annotations

The annotation-based API provides the smoothest integration with `TypedDestination`.

### Step 1: Define Serializable Data Class

```kotlin
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileData(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val isFollowing: Boolean = false
)
```

**Serialization Features You Can Use:**
- Default values
- Nullable types
- Collections (List, Set, Map)
- Nested data classes
- Enums
- Custom serializers

### Step 2: Create Typed Destination

```kotlin
import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TypedDestination

@Graph("profile")
sealed class ProfileDestination : Destination {
    @Route("profile/user")
    @Argument(UserProfileData::class)
    data class UserProfile(
        val userId: String,
        val displayName: String,
        val avatarUrl: String? = null,
        val isFollowing: Boolean = false
    ) : ProfileDestination(), TypedDestination<UserProfileData> {
        override val data = UserProfileData(userId, displayName, avatarUrl, isFollowing)
    }
}
```

**Pattern:**
- Constructor parameters mirror data class fields
- Implement `TypedDestination<YourData>`
- Override `data` property with data class instance
- Use `@Argument(YourData::class)` annotation

### Step 3: Define Content with Typed Parameter

```kotlin
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.core.navigation.core.Navigator

@Content(ProfileDestination.UserProfile::class)
@Composable
fun UserProfileContent(data: UserProfileData, navigator: Navigator) {
    UserProfileScreen(
        userId = data.userId,
        displayName = data.displayName,
        avatarUrl = data.avatarUrl,
        isFollowing = data.isFollowing,
        onFollowToggle = { /* ... */ },
        onBack = { navigator.navigateBack() }
    )
}
```

**Key Points:**
- First parameter is the typed data (not `Destination`)
- Second parameter is always `Navigator`
- KSP generates the wiring code automatically

### Step 4: Navigate with Type-Safety

```kotlin
// In your UI code
navigator.navigate(
    ProfileDestination.UserProfile(
        userId = "user123",
        displayName = "Jane Doe",
        avatarUrl = "https://example.com/avatar.jpg",
        isFollowing = true
    )
)
```

**Type-safety guarantees:**
- Compiler ensures all required parameters provided
- No typos in parameter names
- Correct types enforced
- Default values work automatically

---

## Using with Manual DSL

You can use `TypedDestination` without annotations using the manual DSL approach.

### Step 1: Define Data Class and Destination

```kotlin
@Serializable
data class ArticleData(
    val articleId: String,
    val title: String,
    val authorId: String
)

data class Article(
    val articleId: String,
    val title: String,
    val authorId: String
) : Destination, TypedDestination<ArticleData> {
    override val route = "article"
    override val data = ArticleData(articleId, title, authorId)
}
```

### Step 2: Register Route Manually

```kotlin
object ArticleRoutes {
    init {
        RouteRegistry.register(Article::class, "article")
    }
}
```

### Step 3: Register with typedDestination() DSL

```kotlin
import com.jermey.quo.vadis.core.navigation.core.typedDestination

fun articleGraph() = navigationGraph("articles") {
    startDestination(Article("", "", ""))
    
    typedDestination<ArticleData>(
        destination = Article::class,
        dataClass = ArticleData::class
    ) { data, navigator ->
        ArticleScreen(
            articleId = data.articleId,
            title = data.title,
            authorId = data.authorId,
            navigator = navigator
        )
    }
}
```

**typedDestination() Parameters:**
- `destination: KClass<*>` - The destination class
- `dataClass: KClass<T>` - The data class for deserialization
- `transition: NavigationTransition?` - Optional transition animation
- `content: @Composable (T, Navigator) -> Unit` - Content lambda with typed data

---

## Serialization Setup

### Add kotlinx.serialization

#### 1. Add Plugin

In your module's `build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.2.20"
    // ... other plugins
}
```

#### 2. Add Dependency

```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                // ... other dependencies
            }
        }
    }
}
```

#### 3. Configure Version Catalog (Optional)

In `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.2.20"
kotlinxSerialization = "1.6.0"

[libraries]
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }

[plugins]
kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Then in `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
```

---

## Supported Types

kotlinx.serialization supports a wide range of types out of the box:

### Primitive Types

```kotlin
@Serializable
data class PrimitivesData(
    val intValue: Int,
    val longValue: Long,
    val floatValue: Float,
    val doubleValue: Double,
    val booleanValue: Boolean,
    val stringValue: String,
    val charValue: Char
)
```

### Nullable Types

```kotlin
@Serializable
data class NullableData(
    val optionalString: String?,
    val optionalInt: Int?,
    val optionalObject: NestedData?
)
```

### Collections

```kotlin
@Serializable
data class CollectionsData(
    val stringList: List<String>,
    val intSet: Set<Int>,
    val stringMap: Map<String, String>,
    val nestedList: List<List<Int>>
)
```

### Enums

```kotlin
@Serializable
enum class Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
data class TaskData(
    val title: String,
    val priority: Priority
)
```

### Nested Data Classes

```kotlin
@Serializable
data class Address(
    val street: String,
    val city: String,
    val zipCode: String
)

@Serializable
data class UserData(
    val name: String,
    val email: String,
    val address: Address
)
```

### Sealed Classes

```kotlin
@Serializable
sealed class Result {
    @Serializable
    data class Success(val value: String) : Result()
    
    @Serializable
    data class Error(val message: String) : Result()
}

@Serializable
data class OperationData(
    val result: Result
)
```

---

## Custom Serializers

For types that aren't supported by default, you can write custom serializers.

### Example: Date/Time Serialization

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable(with = InstantSerializer::class)
data class Instant(val epochMillis: Long)

object InstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.LONG)
    
    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.epochMillis)
    }
    
    override fun deserialize(decoder: Decoder): Instant {
        return Instant(decoder.decodeLong())
    }
}

@Serializable
data class EventData(
    val title: String,
    val timestamp: Instant
)
```

### Example: UUID Serialization

```kotlin
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    
    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }
    
    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@Serializable
data class EntityData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val name: String
)
```

---

## Multiplatform Considerations

### Platform-Specific Types

Avoid platform-specific types in data classes:

```kotlin
// ❌ Avoid - platform-specific
@Serializable
data class BadData(
    val timestamp: java.time.Instant  // Android/JVM only
)

// ✅ Good - multiplatform compatible
@Serializable
data class GoodData(
    val timestampMillis: Long
)
```

### Expect/Actual Pattern

For platform-specific data, use expect/actual:

```kotlin
// commonMain
@Serializable
data class PlatformData(
    val commonField: String,
    val platformField: PlatformSpecificData
)

expect class PlatformSpecificData

// androidMain
@Serializable
actual class PlatformSpecificData(val androidValue: String)

// iosMain
@Serializable
actual class PlatformSpecificData(val iosValue: String)
```

### Serialization Format

The default JSON format works identically across all platforms:

```kotlin
import kotlinx.serialization.json.Json

val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}
```

---

## Complete Examples

### Example 1: E-Commerce Product Details

```kotlin
@Serializable
data class ProductDetailData(
    val productId: String,
    val name: String,
    val description: String,
    val price: Double,
    val currency: String = "USD",
    val imageUrls: List<String>,
    val inStock: Boolean,
    val rating: Float?,
    val reviewCount: Int,
    val variants: List<ProductVariant>
)

@Serializable
data class ProductVariant(
    val variantId: String,
    val name: String,
    val priceModifier: Double
)

@Graph("shop")
sealed class ShopDestination : Destination {
    @Route("shop/product")
    @Argument(ProductDetailData::class)
    data class ProductDetail(
        val productId: String,
        val name: String,
        val description: String,
        val price: Double,
        val currency: String = "USD",
        val imageUrls: List<String>,
        val inStock: Boolean,
        val rating: Float?,
        val reviewCount: Int,
        val variants: List<ProductVariant>
    ) : ShopDestination(), TypedDestination<ProductDetailData> {
        override val data = ProductDetailData(
            productId, name, description, price, currency,
            imageUrls, inStock, rating, reviewCount, variants
        )
    }
}

@Content(ShopDestination.ProductDetail::class)
@Composable
fun ProductDetailContent(data: ProductDetailData, navigator: Navigator) {
    ProductDetailScreen(
        product = data,
        onAddToCart = { variant ->
            // Handle add to cart
        },
        onBack = { navigator.navigateBack() }
    )
}
```

### Example 2: Form Wizard with Optional Fields

```kotlin
@Serializable
enum class AccountType {
    PERSONAL, BUSINESS, ENTERPRISE
}

@Serializable
data class RegistrationStep2Data(
    val email: String,
    val accountType: AccountType,
    val companyName: String? = null,
    val taxId: String? = null,
    val agreedToTerms: Boolean
)

@Graph("registration")
sealed class RegistrationDestination : Destination {
    @Route("registration/step1")
    data object Step1 : RegistrationDestination()
    
    @Route("registration/step2")
    @Argument(RegistrationStep2Data::class)
    data class Step2(
        val email: String,
        val accountType: AccountType,
        val companyName: String? = null,
        val taxId: String? = null,
        val agreedToTerms: Boolean
    ) : RegistrationDestination(), TypedDestination<RegistrationStep2Data> {
        override val data = RegistrationStep2Data(
            email, accountType, companyName, taxId, agreedToTerms
        )
    }
}

@Content(RegistrationDestination.Step2::class)
@Composable
fun RegistrationStep2Content(data: RegistrationStep2Data, navigator: Navigator) {
    RegistrationStep2Screen(
        initialData = data,
        onComplete = { finalData ->
            // Navigate to next step with updated data
            navigator.navigate(
                RegistrationDestination.Step3(/* ... */)
            )
        },
        onBack = { navigator.navigateBack() }
    )
}
```

### Example 3: Search Results with Filters

```kotlin
@Serializable
data class SearchFilter(
    val categories: List<String> = emptyList(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val inStockOnly: Boolean = false,
    val sortBy: SortOption = SortOption.RELEVANCE
)

@Serializable
enum class SortOption {
    RELEVANCE, PRICE_LOW_HIGH, PRICE_HIGH_LOW, RATING, NEWEST
}

@Serializable
data class SearchResultsData(
    val query: String,
    val filters: SearchFilter
)

@Graph("search")
sealed class SearchDestination : Destination {
    @Route("search/results")
    @Argument(SearchResultsData::class)
    data class Results(
        val query: String,
        val filters: SearchFilter = SearchFilter()
    ) : SearchDestination(), TypedDestination<SearchResultsData> {
        override val data = SearchResultsData(query, filters)
    }
}

@Content(SearchDestination.Results::class)
@Composable
fun SearchResultsContent(data: SearchResultsData, navigator: Navigator) {
    SearchResultsScreen(
        query = data.query,
        filters = data.filters,
        onFilterChange = { newFilters ->
            // Update search with new filters
            navigator.navigateAndReplace(
                SearchDestination.Results(data.query, newFilters)
            )
        },
        onProductClick = { productId ->
            // Navigate to product details
        },
        onBack = { navigator.navigateBack() }
    )
}
```

---

## Migration Guide

### From Manual Arguments to TypedDestination

**Before:**

```kotlin
// Old manual approach
data class ArticleDestination(val articleId: String, val title: String) : Destination {
    override val route = "article"
    override val arguments = mapOf(
        "articleId" to articleId,
        "title" to title
    )
}

// Graph registration
destination(SimpleDestination("article")) { dest, navigator ->
    val articleId = dest.arguments["articleId"] as String
    val title = dest.arguments["title"] as String
    
    ArticleScreen(articleId = articleId, title = title, navigator = navigator)
}
```

**After:**

```kotlin
// New TypedDestination approach
@Serializable
data class ArticleData(val articleId: String, val title: String)

@Route("article")
@Argument(ArticleData::class)
data class ArticleDestination(
    val articleId: String,
    val title: String
) : Destination, TypedDestination<ArticleData> {
    override val data = ArticleData(articleId, title)
}

@Content(ArticleDestination::class)
@Composable
fun ArticleContent(data: ArticleData, navigator: Navigator) {
    ArticleScreen(
        articleId = data.articleId,
        title = data.title,
        navigator = navigator
    )
}
```

**Migration Steps:**

1. Create `@Serializable` data class mirroring your arguments
2. Make destination implement `TypedDestination<YourData>`
3. Add `@Argument(YourData::class)` annotation
4. Change content function to receive typed data
5. Update navigation calls (usually no changes needed)
6. Remove manual argument parsing

---

## Best Practices

### 1. Keep Data Classes Simple

```kotlin
// ✅ Good - simple, focused data
@Serializable
data class UserData(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?
)

// ❌ Avoid - too much logic
@Serializable
data class UserData(
    val userId: String,
    val displayName: String
) {
    val formattedName: String get() = displayName.uppercase()  // Business logic
    fun isValid(): Boolean = userId.isNotEmpty()              // Validation logic
}
```

Keep data classes as pure data containers. Put logic in your UI or ViewModel.

### 2. Use Default Values Wisely

```kotlin
// ✅ Good - sensible defaults
@Serializable
data class FilterData(
    val category: String? = null,
    val sortOrder: SortOrder = SortOrder.DEFAULT,
    val maxResults: Int = 20
)

// Navigate with defaults
navigator.navigate(SearchDestination.Results(query = "kotlin"))
```

### 3. Group Related Data

```kotlin
// ✅ Good - grouped related fields
@Serializable
data class UserAddress(
    val street: String,
    val city: String,
    val country: String
)

@Serializable
data class UserProfileData(
    val userId: String,
    val name: String,
    val address: UserAddress
)

// ❌ Avoid - flat structure
@Serializable
data class UserProfileData(
    val userId: String,
    val name: String,
    val street: String,
    val city: String,
    val country: String
)
```

### 4. Version Your Data Classes

Plan for evolution by including version markers:

```kotlin
@Serializable
data class OrderData(
    val orderId: String,
    val items: List<OrderItem>,
    val version: Int = 1  // For future migrations
)
```

### 5. Document Complex Data

```kotlin
/**
 * Data for the checkout confirmation screen.
 *
 * @property orderId The unique order identifier
 * @property items List of items being purchased (must not be empty)
 * @property subtotal Price before taxes and shipping
 * @property taxAmount Calculated tax amount
 * @property shippingCost Shipping fee (0.0 for free shipping)
 * @property totalAmount Final amount to charge (subtotal + tax + shipping)
 * @property currency ISO 4217 currency code (e.g., "USD", "EUR")
 */
@Serializable
data class CheckoutData(
    val orderId: String,
    val items: List<OrderItem>,
    val subtotal: Double,
    val taxAmount: Double,
    val shippingCost: Double,
    val totalAmount: Double,
    val currency: String = "USD"
)
```

---

## Troubleshooting

### Problem: Serialization Exception

```
SerializationException: Serializer for class 'MyData' is not found
```

**Solution:** Ensure data class is annotated with `@Serializable`:

```kotlin
@Serializable  // ← Don't forget!
data class MyData(val value: String)
```

### Problem: Platform Type Not Serializable

```
SerializationException: Class 'java.time.LocalDateTime' is not serializable
```

**Solution:** Use multiplatform-compatible types or custom serializers:

```kotlin
// Instead of LocalDateTime, use epoch millis
@Serializable
data class EventData(
    val timestampMillis: Long  // Compatible everywhere
)
```

### Problem: Circular Reference

```
SerializationException: Circular reference detected
```

**Solution:** Avoid circular references in data structures:

```kotlin
// ❌ Circular reference
@Serializable
data class Node(
    val value: String,
    val parent: Node?  // References back to parent
)

// ✅ Use IDs instead
@Serializable
data class Node(
    val nodeId: String,
    val value: String,
    val parentId: String?
)
```

### Problem: Data Class Null Safety Issues

```
NullPointerException: Required value was null
```

**Solution:** Ensure nullable types match between destination and data:

```kotlin
@Serializable
data class ProfileData(
    val bio: String?  // ← Nullable in data class
)

data class Profile(...) : TypedDestination<ProfileData> {
    override val data = ProfileData(
        bio = bio  // ← Must be nullable here too
    )
}
```

---

## Performance Considerations

### Serialization Overhead

TypedDestination uses JSON serialization under the hood. For typical navigation scenarios (< 1KB of data), this is negligible. For larger data:

**Option 1: Pass IDs, not full objects**
```kotlin
// ✅ Efficient - pass ID only
@Serializable
data class ArticleRefData(val articleId: String)

// Fetch full article in the screen
```

**Option 2: Use ViewModel/State management**
```kotlin
// For very large or complex data, use shared state
@Composable
fun DetailScreen(articleId: String, viewModel: ArticleViewModel) {
    val article by viewModel.getArticle(articleId).collectAsState()
    // ...
}
```

### Memory Considerations

Each serializable destination stores its data. For simple types (strings, numbers), this is trivial. For complex objects with collections, consider:

1. Passing minimal data needed for navigation
2. Loading additional data in the destination screen
3. Using a shared state management solution for large data

---

## See Also

- [ANNOTATION_API.md](ANNOTATION_API.md) - Complete annotation-based API guide
- [MIGRATION.md](MIGRATION.md) - Migrating existing code
- [API_REFERENCE.md](API_REFERENCE.md) - Full API documentation
- [kotlinx.serialization documentation](https://github.com/Kotlin/kotlinx.serialization)

---

## Summary

TypedDestination provides:

✅ **Type Safety** - Compile-time checking of navigation arguments  
✅ **Zero Boilerplate** - No manual parsing or type casting  
✅ **IDE Support** - Full autocompletion and refactoring  
✅ **Multiplatform** - Works identically on all platforms  
✅ **Scalable** - Handles simple to complex data structures  
✅ **Testable** - Easy to unit test with concrete data  

Use TypedDestination for any destination that needs to pass data. The small upfront cost of defining a data class pays dividends in type-safety, maintainability, and developer experience.
