# Migration Guide: Manual DSL to Annotation-Based API

This guide helps you migrate existing Quo Vadis navigation code from the manual DSL approach to the modern annotation-based API with KSP code generation.

## Table of Contents
- [Why Migrate?](#why-migrate)
- [Prerequisites](#prerequisites)
- [Migration Steps](#migration-steps)
- [Step-by-Step Examples](#step-by-step-examples)
- [Common Patterns](#common-patterns)
- [Troubleshooting](#troubleshooting)
- [Hybrid Approach](#hybrid-approach)

## Why Migrate?

The annotation-based API offers several advantages:

- **50-70% Less Code**: Eliminate route registration and graph builder boilerplate
- **Automatic Serialization**: kotlinx.serialization handles complex arguments automatically
- **Generated Extensions**: Type-safe navigation functions generated for you
- **Better IDE Support**: Navigate to generated code, better autocompletion
- **Easier Maintenance**: Less code to maintain and update
- **Compile-Time Safety**: Errors caught earlier in the development cycle

**Note**: The manual DSL approach remains fully supported and both approaches can coexist in the same project.

## Prerequisites

### 1. Add Dependencies

Update your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.20-1.0.29"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.jermey.quo-vadis:quo-vadis-core:1.0.0")
            implementation("com.jermey.quo-vadis:quo-vadis-annotations:1.0.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "com.jermey.quo-vadis:quo-vadis-ksp:1.0.0")
    add("kspAndroid", "com.jermey.quo-vadis:quo-vadis-ksp:1.0.0")
    add("kspIosX64", "com.jermey.quo-vadis:quo-vadis-ksp:1.0.0")
    add("kspIosArm64", "com.jermey.quo-vadis:quo-vadis-ksp:1.0.0")
    add("kspIosSimulatorArm64", "com.jermey.quo-vadis:quo-vadis-ksp:1.0.0")
    // Add for other platforms as needed
}
```

### 2. Verify KSP Setup

After adding dependencies, rebuild your project:
```bash
./gradlew clean build
```

Generated code will appear in `build/generated/ksp/`.

## Migration Steps

### Step 1: Convert Destination Sealed Class

**Before (Manual DSL):**
```kotlin
sealed class FeatureDestination : Destination {
    object Home : FeatureDestination() {
        override val route = "feature/home"
    }
    
    data class Details(val itemId: String) : FeatureDestination() {
        override val route = "feature/details"
        override val arguments = mapOf("itemId" to itemId)
    }
}
```

**After (Annotation-Based):**
```kotlin
@Graph("feature")
sealed class FeatureDestination : Destination

@Route("feature/home")
data object Home : FeatureDestination()

@Serializable
data class DetailsData(val itemId: String)

@Route("feature/details")
@Argument(DetailsData::class)
data class Details(val itemId: String) 
    : FeatureDestination(), TypedDestination<DetailsData> {
    override val data = DetailsData(itemId)
}
```

**Key Changes:**
- Add `@Graph` to sealed class
- Remove route from nested classes, add `@Route` annotation
- For destinations with arguments:
  - Create separate `@Serializable` data class
  - Add `@Argument` annotation
  - Implement `TypedDestination<T>`
  - Provide `data` property

### Step 2: Convert Composable Content Functions

**Before (Manual DSL):**
```kotlin
fun createFeatureGraph() = navigationGraph("feature") {
    startDestination(FeatureDestination.Home)
    
    destination(FeatureDestination.Home) { _, navigator ->
        HomeScreen(
            onNavigateToDetails = { id ->
                navigator.navigate(FeatureDestination.Details(id))
            }
        )
    }
    
    destination(FeatureDestination.Details) { destination, navigator ->
        val details = destination as FeatureDestination.Details
        DetailsScreen(
            itemId = details.itemId,
            onBack = { navigator.navigateBack() }
        )
    }
}
```

**After (Annotation-Based):**
```kotlin
@Content(FeatureDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(
        onNavigateToDetails = { id ->
            // Use generated extension
            navigator.navigateToDetails(itemId = id)
        }
    )
}

@Content(FeatureDestination.Details::class)
@Composable
fun DetailsContent(data: DetailsData, navigator: Navigator) {
    DetailsScreen(
        itemId = data.itemId,
        onBack = { navigator.navigateBack() }
    )
}
```

**Key Changes:**
- Extract UI code into separate Composable functions
- Add `@Content` annotation with destination class reference
- For simple destinations: `(Navigator) -> Unit`
- For typed destinations: `(DataClass, Navigator) -> Unit`
- Use generated `navigateTo*()` extension functions

### Step 3: Use Generated Graph Builder

**Before (Manual DSL):**
```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator()
    val graph = remember { createFeatureGraph() }
    
    LaunchedEffect(Unit) {
        navigator.registerGraph(graph)
        navigator.setStartDestination(FeatureDestination.Home)
    }
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = graphToScreenRegistry(graph)
    )
}
```

**After (Annotation-Based):**
```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator(startDestination = FeatureDestination.Home)
    
    // Use generated screen registry
    NavigationHost(
        navigator = navigator,
        screenRegistry = FeatureDestinationScreenRegistry
    )
}
```

**Key Changes:**
- Replace manual graph creation with generated screen registry
- Start destination passed to `rememberNavigator`
- NavigationHost replaces GraphNavHost

## Step-by-Step Examples

### Example 1: Simple Navigation

**Before:**
```kotlin
sealed class AppDestination : Destination {
    object Home : AppDestination() {
        override val route = "home"
    }
    object Settings : AppDestination() {
        override val route = "settings"
    }
}

val appGraph = navigationGraph("app") {
    startDestination(AppDestination.Home)
    destination(AppDestination.Home) { _, nav -> HomeScreen(nav) }
    destination(AppDestination.Settings) { _, nav -> SettingsScreen(nav) }
}
```

**After:**
```kotlin
@Graph("app")
sealed class AppDestination : Destination

@Route("home")
data object Home : AppDestination()

@Route("settings")
data object Settings : AppDestination()

@Content(Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(navigator)
}

@Content(Settings::class)
@Composable
fun SettingsContent(navigator: Navigator) {
    SettingsScreen(navigator)
}

// Use: buildAppDestinationGraph()
```

### Example 2: Navigation with Arguments

**Before:**
```kotlin
sealed class ShopDestination : Destination {
    data class Product(
        val productId: String,
        val categoryId: String? = null
    ) : ShopDestination() {
        override val route = "product"
        override val arguments = buildMap {
            put("productId", productId)
            categoryId?.let { put("categoryId", it) }
        }
    }
}

val shopGraph = navigationGraph("shop") {
    destination(ShopDestination.Product) { dest, nav ->
        val product = dest as ShopDestination.Product
        ProductScreen(
            productId = product.productId,
            categoryId = product.categoryId,
            navigator = nav
        )
    }
}

// Navigate
navigator.navigate(ShopDestination.Product("123", "electronics"))
```

**After:**
```kotlin
@Graph("shop")
sealed class ShopDestination : Destination

@Serializable
data class ProductData(
    val productId: String,
    val categoryId: String? = null
)

@Route("shop/product")
@Argument(ProductData::class)
data class Product(
    val productId: String,
    val categoryId: String? = null
) : ShopDestination(), TypedDestination<ProductData> {
    override val data = ProductData(productId, categoryId)
}

@Content(Product::class)
@Composable
fun ProductContent(data: ProductData, navigator: Navigator) {
    ProductScreen(
        productId = data.productId,
        categoryId = data.categoryId,
        navigator = navigator
    )
}

// Navigate with generated extension
navigator.navigateToProduct(
    productId = "123",
    categoryId = "electronics"
)
```

### Example 3: Complex Arguments

**Before:**
```kotlin
enum class SortOption { PRICE, RATING, NEWEST }

data class FilterOptions(
    val categories: List<String>,
    val minPrice: Double?,
    val maxPrice: Double?,
    val sortBy: SortOption
)

sealed class SearchDestination : Destination {
    data class Results(val filters: FilterOptions) : SearchDestination() {
        override val route = "search/results"
        override val arguments = mapOf(
            "filters" to Json.encodeToString(filters)
        )
    }
}

// Manual deserialization needed
val graph = navigationGraph("search") {
    destination(SearchDestination.Results) { dest, nav ->
        val results = dest as SearchDestination.Results
        SearchResultsScreen(filters = results.filters)
    }
}
```

**After:**
```kotlin
@Serializable
enum class SortOption { PRICE, RATING, NEWEST }

@Serializable
data class FilterData(
    val categories: List<String> = emptyList(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sortBy: SortOption = SortOption.PRICE
)

@Graph("search")
sealed class SearchDestination : Destination

@Route("search/results")
@Argument(FilterData::class)
data class Results(
    val categories: List<String> = emptyList(),
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sortBy: SortOption = SortOption.PRICE
) : SearchDestination(), TypedDestination<FilterData> {
    override val data = FilterData(categories, minPrice, maxPrice, sortBy)
}

@Content(Results::class)
@Composable
fun ResultsContent(data: FilterData, navigator: Navigator) {
    SearchResultsScreen(filters = data)
}

// Navigate - serialization handled automatically
navigator.navigateToResults(
    categories = listOf("electronics", "books"),
    minPrice = 10.0,
    maxPrice = 100.0,
    sortBy = SortOption.RATING
)
```

### Example 4: Multiple Graphs

**Before:**
```kotlin
sealed class MainDestination : Destination { /* ... */ }
sealed class AuthDestination : Destination { /* ... */ }

fun createMainGraph() = navigationGraph("main") { /* ... */ }
fun createAuthGraph() = navigationGraph("auth") { /* ... */ }

val rootGraph = navigationGraph("root") {
    includeGraph(createMainGraph())
    includeGraph(createAuthGraph())
}
```

**After:**
```kotlin
@Graph("main")
sealed class MainDestination : Destination
// Define @Route and @Content for main...

@Graph("auth")
sealed class AuthDestination : Destination
// Define @Route and @Content for auth...

// Use generated builders
val rootGraph = navigationGraph("root") {
    includeGraph(buildMainDestinationGraph())
    includeGraph(buildAuthDestinationGraph())
}
```

## Common Patterns

### Pattern 1: Optional Arguments

**Before:**
```kotlin
data class UserProfile(
    val userId: String,
    val tab: String? = null
) : Destination {
    override val route = "user/$userId${tab?.let { "/$it" } ?: ""}"
}
```

**After:**
```kotlin
@Serializable
data class UserProfileData(
    val userId: String,
    val tab: String? = null
)

@Route("user/profile")
@Argument(UserProfileData::class)
data class UserProfile(
    val userId: String,
    val tab: String? = null
) : Destination, TypedDestination<UserProfileData> {
    override val data = UserProfileData(userId, tab)
}

// Navigate
navigator.navigateToUserProfile(userId = "123", tab = "posts")
navigator.navigateToUserProfile(userId = "123")  // tab is optional
```

### Pattern 2: List Arguments

**Before:**
```kotlin
data class Gallery(val imageIds: List<String>) : Destination {
    override val route = "gallery"
    override val arguments = mapOf(
        "imageIds" to imageIds.joinToString(",")
    )
}

// Manual parsing needed
destination(Gallery) { dest, nav ->
    val gallery = dest as Gallery
    GalleryScreen(imageIds = gallery.imageIds)
}
```

**After:**
```kotlin
@Serializable
data class GalleryData(val imageIds: List<String>)

@Route("gallery")
@Argument(GalleryData::class)
data class Gallery(val imageIds: List<String>) 
    : Destination, TypedDestination<GalleryData> {
    override val data = GalleryData(imageIds)
}

@Content(Gallery::class)
@Composable
fun GalleryContent(data: GalleryData, navigator: Navigator) {
    GalleryScreen(imageIds = data.imageIds)
}

// Automatic serialization/deserialization
navigator.navigateToGallery(imageIds = listOf("img1", "img2", "img3"))
```

### Pattern 3: Nested Objects

**Before:**
```kotlin
data class Address(val street: String, val city: String)
data class UserData(val name: String, val address: Address)

data class UserDetails(val user: UserData) : Destination {
    override val route = "user/details"
    override val arguments = mapOf(
        "user" to Json.encodeToString(user)
    )
}
```

**After:**
```kotlin
@Serializable
data class Address(val street: String, val city: String)

@Serializable
data class UserDetailsData(
    val name: String,
    val address: Address
)

@Route("user/details")
@Argument(UserDetailsData::class)
data class UserDetails(
    val name: String,
    val address: Address
) : Destination, TypedDestination<UserDetailsData> {
    override val data = UserDetailsData(name, address)
}

// Automatic nested serialization
navigator.navigateToUserDetails(
    name = "John Doe",
    address = Address("123 Main St", "Springfield")
)
```

### Pattern 4: Default Values

**Before:**
```kotlin
data class Settings(
    val theme: String = "auto",
    val notifications: Boolean = true
) : Destination {
    override val route = "settings"
    override val arguments = mapOf(
        "theme" to theme,
        "notifications" to notifications.toString()
    )
}
```

**After:**
```kotlin
@Serializable
data class SettingsData(
    val theme: String = "auto",
    val notifications: Boolean = true
)

@Route("settings")
@Argument(SettingsData::class)
data class Settings(
    val theme: String = "auto",
    val notifications: Boolean = true
) : Destination, TypedDestination<SettingsData> {
    override val data = SettingsData(theme, notifications)
}

// Use defaults
navigator.navigateToSettings()  // Uses all defaults
navigator.navigateToSettings(theme = "dark")  // Override one
```

## Troubleshooting

### Issue 1: Generated Code Not Found

**Symptoms:**
- Unresolved reference: `build*Graph()`
- Unresolved reference: `navigateTo*()`

**Solutions:**

1. **Rebuild project:**
   ```bash
   ./gradlew clean build
   ```

2. **Verify KSP configuration:**
   ```kotlin
   dependencies {
       add("kspCommonMainMetadata", "com.jermey.quo-vadis:quo-vadis-ksp:1.0.0")
       // Add for all your target platforms
   }
   ```

3. **Check build output:**
   Look in `build/generated/ksp/commonMain/kotlin/` for generated files.

4. **IDE sync:**
   - IntelliJ IDEA: File → Invalidate Caches → Restart
   - Android Studio: File → Sync Project with Gradle Files

### Issue 2: Serialization Errors

**Symptoms:**
- `SerializationException` at runtime
- "Serializer not found" errors

**Solutions:**

1. **Add @Serializable annotation:**
   ```kotlin
   @Serializable  // Don't forget this!
   data class MyData(val id: String)
   ```

2. **Add kotlinx-serialization plugin:**
   ```kotlin
   plugins {
       kotlin("plugin.serialization") version "2.2.20"
   }
   ```

3. **For custom types, provide serializer:**
   ```kotlin
   @Serializable
   data class MyData(
       @Serializable(with = UUIDSerializer::class)
       val id: UUID
   )
   ```

### Issue 3: Duplicate Routes

**Symptoms:**
- `IllegalArgumentException: Route 'xyz' already registered`

**Solution:**

Ensure each `@Route` value is unique:

```kotlin
@Route("feature/home")  // ✅ Unique
data object Home : FeatureDestination()

@Route("feature/list")  // ✅ Unique
data object List : FeatureDestination()

// NOT: @Route("home") for both!
```

### Issue 4: Wrong @Content Signature

**Symptoms:**
- Type mismatch errors
- Build failures in generated code

**Solution:**

Match signature to destination type:

```kotlin
// Simple destination (no @Argument)
@Content(Home::class)
@Composable
fun HomeContent(navigator: Navigator) { /* ... */ }

// Typed destination (with @Argument)
@Content(Details::class)
@Composable
fun DetailsContent(data: DetailsData, navigator: Navigator) { /* ... */ }
```

## Hybrid Approach

You can mix both approaches in the same project:

```kotlin
// Some features use annotations
@Graph("shop")
sealed class ShopDestination : Destination
// ... with @Route, @Content, etc.

// Others use manual DSL
sealed class AdminDestination : Destination {
    object Dashboard : AdminDestination() {
        override val route = "admin/dashboard"
    }
}

fun createAdminGraph() = navigationGraph("admin") {
    // Manual graph building
}

// Combine in root graph
val rootGraph = navigationGraph("root") {
    includeGraph(buildShopDestinationGraph())  // Generated
    includeGraph(createAdminGraph())            // Manual
}
```

**When to use each:**

| Use Annotation-Based When | Use Manual DSL When |
|---------------------------|---------------------|
| Standard navigation patterns | Dynamic destination creation |
| Serializable data types | Custom serialization logic |
| Most feature modules | Complex conditional graphs |
| New code | Legacy code (no need to migrate) |
| Developer convenience | Fine-grained control needed |

## Migration Checklist

- [ ] Add KSP and kotlinx-serialization dependencies
- [ ] Add `@Graph` to sealed destination classes
- [ ] Convert route strings to `@Route` annotations
- [ ] Create `@Serializable` data classes for typed destinations
- [ ] Add `@Argument` and implement `TypedDestination<T>`
- [ ] Extract UI into separate functions with `@Content`
- [ ] Replace manual graph builders with generated ones
- [ ] Update navigation calls to use generated extensions
- [ ] Rebuild project and verify generated code
- [ ] Test all navigation flows
- [ ] Update tests to use new destinations/navigation
- [ ] Remove old manual graph building code

## Next Steps

- Read [ANNOTATION_API.md](ANNOTATION_API.md) for complete annotation reference
- See [TYPED_DESTINATIONS.md](TYPED_DESTINATIONS.md) for serialization details
- Check [API_REFERENCE.md](API_REFERENCE.md) for full API documentation
- Review demo app in `composeApp/` for real-world examples

## Need Help?

- GitHub Issues: https://github.com/jermeyyy/quo-vadis/issues
- Discussions: https://github.com/jermeyyy/quo-vadis/discussions
- Documentation: https://jermeyyy.github.io/quo-vadis/
