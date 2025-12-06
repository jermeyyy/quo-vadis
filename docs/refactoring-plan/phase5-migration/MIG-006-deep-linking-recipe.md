# MIG-006: Deep Linking Recipe

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-006 |
| **Complexity** | Medium |
| **Estimated Time** | 1 day |
| **Dependencies** | PREP-001, MIG-001, MIG-002 |
| **Output** | `quo-vadis-recipes/src/commonMain/.../deeplink/` |

## Objective

Create recipe examples demonstrating URI-based deep linking in the NavNode architecture. This includes:

1. **Basic Deep Link Handling** - Simple URI to destination routing
2. **Nested Deep Links** - Deep links resolving to nested screens
3. **Parameterized Deep Links** - Routes with path and query parameters
4. **Path Reconstruction** - Building URIs from destinations

## Recipe Files

```
quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/deeplink/
├── package-info.kt
├── DeepLinkDestinations.kt       # Shared destination definitions
├── BasicDeepLinkRecipe.kt        # Simple URI routing
└── NestedDeepLinkRecipe.kt       # Deep link to nested screens
```

## Legacy References

### Old Deep Link APIs

| File | GitHub Link |
|------|-------------|
| DeepLink.kt | [DeepLink.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/DeepLink.kt) |
| Navigator (handleDeepLink) | [Navigator.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt) |
| GeneratedDeepLinkHandler | [GeneratedDeepLinkHandler.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/GeneratedDeepLinkHandler.kt) |

### Demo App Usage

| File | GitHub Link |
|------|-------------|
| Deep Link Demo | [DeepLinkDemoScreen.kt](https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/DeepLinkDemoScreen.kt) |

## Key Transformations

| Old Pattern | New Pattern |
|-------------|-------------|
| `DeepLinkHandler` interface | KSP-generated `GeneratedDeepLinkHandler` |
| Manual route registration | Automatic from `@Destination(route)` |
| `navigator.handleDeepLink(DeepLink.parse(uri))` | `navigator.navigate(handler.handleDeepLink(uri))` |
| Flat route matching | Tree path reconstruction |
| Runtime pattern registration | Compile-time route extraction |

---

## Recipe Content

### 1. Package Info (`package-info.kt`)

```kotlin
/**
 * # Deep Linking Recipes
 * 
 * Demonstrates URI-based deep linking with the Quo Vadis NavNode architecture.
 * 
 * ## When to Use Deep Linking
 * 
 * - App links / Universal links from external sources
 * - Push notification navigation
 * - Marketing campaign tracking
 * - Cross-app navigation
 * - Browser URL handling (web targets)
 * 
 * ## Key Concepts
 * 
 * 1. **Route Templates** - Define URI patterns with `@Destination(route = "path/{param}")`
 * 2. **KSP Generation** - `GeneratedDeepLinkHandler` parses URIs automatically
 * 3. **Path Reconstruction** - Build tree state from deep URI (e.g., `/tabs/home/article/123`)
 * 4. **Scheme Handling** - Support custom schemes (`myapp://`) and https
 * 
 * ## Recipes
 * 
 * - [BasicDeepLinkRecipe] - Simple URI to destination routing
 * - [NestedDeepLinkRecipe] - Deep links that resolve to nested screens
 * 
 * ## Integration Steps
 * 
 * 1. Define routes with `@Destination(route = "path/{param}")`
 * 2. Configure platform deep link handling (AndroidManifest, Info.plist)
 * 3. Pass incoming URIs to `GeneratedDeepLinkHandler.handleDeepLink(uri)`
 * 4. Navigate to the matched destination
 * 
 * @see com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
 * @see com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
 */
package com.jermey.quo.vadis.recipes.deeplink
```

### 2. Shared Destinations (`DeepLinkDestinations.kt`)

```kotlin
package com.jermey.quo.vadis.recipes.deeplink

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.Destination as BaseDestination

/**
 * # Deep Link Destination Definitions
 * 
 * These destinations demonstrate deep link route patterns.
 * 
 * ## Route Pattern Syntax
 * 
 * - Static segments: `products/featured`
 * - Path parameters: `products/{productId}`
 * - Multiple parameters: `category/{categoryId}/product/{productId}`
 * - Optional query params handled separately
 * 
 * ## Generated Deep Link Handler
 * 
 * KSP generates `GeneratedDeepLinkHandler` that:
 * - Parses URIs against route patterns
 * - Extracts path parameters
 * - Creates destination instances
 * - Supports reverse URI generation
 */

// ============================================================
// SIMPLE ROUTES
// ============================================================

/**
 * Products navigation stack with deep-linkable routes.
 * 
 * Supports URIs like:
 * - `myapp://products` → List
 * - `myapp://products/featured` → Featured
 * - `myapp://products/123` → Detail(id="123")
 */
@Stack(name = "products", startDestination = "List")
sealed class ProductsDestination : BaseDestination {
    
    /**
     * Product list screen.
     * 
     * Deep link: `myapp://products`
     */
    @Destination(route = "products")
    data object List : ProductsDestination()
    
    /**
     * Featured products screen.
     * 
     * Deep link: `myapp://products/featured`
     */
    @Destination(route = "products/featured")
    data object Featured : ProductsDestination()
    
    /**
     * Product detail screen with ID parameter.
     * 
     * Deep link: `myapp://products/{productId}`
     * 
     * Example URIs:
     * - `myapp://products/abc-123` → Detail(productId="abc-123")
     * - `https://example.com/products/xyz` → Detail(productId="xyz")
     * 
     * @property productId The product identifier from the URI path
     */
    @Destination(route = "products/{productId}")
    data class Detail(val productId: String) : ProductsDestination()
}

// ============================================================
// NESTED ROUTES
// ============================================================

/**
 * Categories navigation with nested product routes.
 * 
 * Demonstrates multi-level deep linking:
 * - `myapp://categories` → List all categories
 * - `myapp://categories/{id}` → Category detail
 * - `myapp://categories/{catId}/products/{prodId}` → Product in category
 */
@Stack(name = "categories", startDestination = "CategoryList")
sealed class CategoryDestination : BaseDestination {
    
    @Destination(route = "categories")
    data object CategoryList : CategoryDestination()
    
    @Destination(route = "categories/{categoryId}")
    data class CategoryDetail(val categoryId: String) : CategoryDestination()
    
    /**
     * Product within a category context.
     * 
     * Deep link: `myapp://categories/{categoryId}/products/{productId}`
     * 
     * This demonstrates:
     * - Multiple path parameters
     * - Contextual navigation (product in category context)
     */
    @Destination(route = "categories/{categoryId}/products/{productId}")
    data class ProductInCategory(
        val categoryId: String,
        val productId: String
    ) : CategoryDestination()
}
```

### 3. Basic Deep Link Recipe (`BasicDeepLinkRecipe.kt`)

```kotlin
package com.jermey.quo.vadis.recipes.deeplink

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.recipes.shared.RecipeScaffold
import com.jermey.quo.vadis.recipes.shared.NavigationButton

/**
 * # Basic Deep Linking Recipe
 * 
 * Demonstrates simple URI-based deep linking with the NavNode architecture.
 * 
 * ## What This Recipe Shows
 * 
 * 1. Route definition with `@Destination(route = "path/{param}")`
 * 2. Using KSP-generated `GeneratedDeepLinkHandler`
 * 3. Handling incoming URIs
 * 4. Creating URIs from destinations (reverse lookup)
 * 
 * ## Key Components
 * 
 * - [ProductsDestination] - Destinations with deep link routes
 * - [GeneratedDeepLinkHandler] - KSP-generated URI parser
 * - [DeepLinkResult] - Matched/NotMatched result type
 * 
 * ## Platform Integration
 * 
 * ### Android (AndroidManifest.xml)
 * ```xml
 * <intent-filter>
 *     <action android:name="android.intent.action.VIEW" />
 *     <category android:name="android.intent.category.DEFAULT" />
 *     <category android:name="android.intent.category.BROWSABLE" />
 *     <data android:scheme="myapp" />
 *     <data android:scheme="https" android:host="example.com" />
 * </intent-filter>
 * ```
 * 
 * ### iOS (Info.plist)
 * ```xml
 * <key>CFBundleURLTypes</key>
 * <array>
 *     <dict>
 *         <key>CFBundleURLSchemes</key>
 *         <array><string>myapp</string></array>
 *     </dict>
 * </array>
 * ```
 * 
 * @see ProductsDestination for route definitions
 * @see com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
 */

// ============================================================
// SCREENS
// ============================================================

@Screen(ProductsDestination.List::class)
@Composable
fun ProductListScreen(navigator: Navigator) {
    RecipeScaffold(title = "Products") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Welcome to Products")
            
            NavigationButton(
                text = "View Featured",
                onClick = { navigator.navigate(ProductsDestination.Featured) }
            )
            
            NavigationButton(
                text = "View Product ABC-123",
                onClick = { navigator.navigate(ProductsDestination.Detail("abc-123")) }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simulate incoming deep link
            var deepLinkUri by remember { mutableStateOf("") }
            OutlinedTextField(
                value = deepLinkUri,
                onValueChange = { deepLinkUri = it },
                label = { Text("Enter Deep Link URI") },
                placeholder = { Text("myapp://products/xyz") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    handleDeepLink(deepLinkUri, navigator)
                },
                enabled = deepLinkUri.isNotBlank()
            ) {
                Text("Handle Deep Link")
            }
        }
    }
}

@Screen(ProductsDestination.Featured::class)
@Composable
fun FeaturedProductsScreen(navigator: Navigator) {
    RecipeScaffold(
        title = "Featured Products",
        showBackButton = true,
        onBackClick = { navigator.navigateBack() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Featured products go here")
            
            // Show how to create a deep link URI for this screen
            val uri = GeneratedScreenRegistry.createDeepLinkUri(
                ProductsDestination.Featured,
                scheme = "myapp"
            )
            Text("Deep link: $uri", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Screen(ProductsDestination.Detail::class)
@Composable
fun ProductDetailScreen(
    destination: ProductsDestination.Detail,
    navigator: Navigator
) {
    RecipeScaffold(
        title = "Product: ${destination.productId}",
        showBackButton = true,
        onBackClick = { navigator.navigateBack() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Product ID: ${destination.productId}")
            Text("(This screen was reached via deep link or navigation)")
            
            // Show how to create a deep link URI for this screen
            val uri = GeneratedScreenRegistry.createDeepLinkUri(
                destination,
                scheme = "myapp"
            )
            Text("Deep link: $uri", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ============================================================
// DEEP LINK HANDLING
// ============================================================

/**
 * Handle an incoming deep link URI.
 * 
 * This function demonstrates the pattern for processing deep links:
 * 1. Parse URI with GeneratedDeepLinkHandler
 * 2. Check result type (Matched/NotMatched)
 * 3. Navigate to matched destination
 * 
 * @param uri The incoming deep link URI
 * @param navigator The navigator to use for navigation
 */
private fun handleDeepLink(uri: String, navigator: Navigator) {
    // KSP-generated handler parses the URI
    when (val result = GeneratedDeepLinkHandler.handleDeepLink(uri)) {
        is DeepLinkResult.Matched -> {
            // Navigate to the matched destination
            navigator.navigate(result.destination)
        }
        is DeepLinkResult.NotMatched -> {
            // Handle unknown deep link
            // In production: show error, log analytics, fallback to home
            println("Unknown deep link: $uri")
        }
    }
}

// ============================================================
// APP ENTRY POINT
// ============================================================

/**
 * Entry point for the Basic Deep Linking recipe.
 * 
 * ## Usage
 * 
 * ```kotlin
 * @Composable
 * fun App() {
 *     BasicDeepLinkApp()
 * }
 * ```
 * 
 * ## Deep Link Integration
 * 
 * To handle deep links from outside the app:
 * 
 * ```kotlin
 * // In your platform-specific entry point
 * fun onDeepLink(uri: String) {
 *     handleDeepLink(uri, navigator)
 * }
 * ```
 */
@Composable
fun BasicDeepLinkApp() {
    val navTree = remember { buildProductsNavNode() }  // KSP-generated
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry  // KSP-generated
    )
}

// ============================================================
// NOTES FOR LLM AGENTS
// ============================================================

/**
 * ## Migration from Legacy Deep Linking
 * 
 * ### OLD PATTERN (Legacy)
 * ```kotlin
 * // Manual registration
 * val handler = DefaultDeepLinkHandler()
 * handler.register("products/{id}") { params ->
 *     ProductDetail(params["id"]!!)
 * }
 * 
 * // Handling
 * val deepLink = DeepLink.parse(uri)
 * navigator.handleDeepLink(deepLink)
 * ```
 * 
 * ### NEW PATTERN (NavNode)
 * ```kotlin
 * // Automatic from annotations
 * @Destination(route = "products/{productId}")
 * data class Detail(val productId: String) : ProductsDestination()
 * 
 * // Handling
 * when (val result = GeneratedDeepLinkHandler.handleDeepLink(uri)) {
 *     is DeepLinkResult.Matched -> navigator.navigate(result.destination)
 *     is DeepLinkResult.NotMatched -> handleError()
 * }
 * ```
 * 
 * ### Key Changes
 * 
 * 1. Route patterns are defined in annotations, not registered at runtime
 * 2. KSP generates the handler automatically
 * 3. Parameters become constructor arguments on destination classes
 * 4. Result is a sealed class for type-safe handling
 */
```

### 4. Nested Deep Link Recipe (`NestedDeepLinkRecipe.kt`)

```kotlin
package com.jermey.quo.vadis.recipes.deeplink

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.recipes.shared.RecipeScaffold
import com.jermey.quo.vadis.recipes.shared.NavigationButton

/**
 * # Nested Deep Linking Recipe
 * 
 * Demonstrates deep links that resolve to screens within nested navigation hierarchies.
 * 
 * ## What This Recipe Shows
 * 
 * 1. Multi-level route patterns: `categories/{catId}/products/{prodId}`
 * 2. Tree path reconstruction - building the full navigation stack
 * 3. Multiple path parameters
 * 4. Context-aware navigation (product in category context)
 * 
 * ## Path Reconstruction
 * 
 * When a deep link targets a nested screen, the NavNode tree must be 
 * reconstructed to show the correct back stack:
 * 
 * ```
 * Deep link: myapp://categories/electronics/products/phone-123
 * 
 * Reconstructed stack:
 * 1. CategoryList (root)
 * 2. CategoryDetail(categoryId="electronics")
 * 3. ProductInCategory(categoryId="electronics", productId="phone-123") ← current
 * ```
 * 
 * This allows natural back navigation through the expected flow.
 * 
 * @see CategoryDestination for nested route definitions
 */

// ============================================================
// SCREENS
// ============================================================

@Screen(CategoryDestination.CategoryList::class)
@Composable
fun CategoryListScreen(navigator: Navigator) {
    RecipeScaffold(title = "Categories") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Browse Categories")
            
            // Sample categories
            listOf("electronics", "clothing", "books").forEach { category ->
                NavigationButton(
                    text = category.replaceFirstChar { it.uppercase() },
                    onClick = { 
                        navigator.navigate(CategoryDestination.CategoryDetail(category)) 
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Deep link testing
            Text("Test Nested Deep Link:", style = MaterialTheme.typography.titleSmall)
            
            Button(onClick = {
                // Simulate deep link to nested product
                val uri = "myapp://categories/electronics/products/phone-123"
                handleNestedDeepLink(uri, navigator)
            }) {
                Text("Deep Link → Electronics/Phone")
            }
        }
    }
}

@Screen(CategoryDestination.CategoryDetail::class)
@Composable
fun CategoryDetailScreen(
    destination: CategoryDestination.CategoryDetail,
    navigator: Navigator
) {
    RecipeScaffold(
        title = "Category: ${destination.categoryId}",
        showBackButton = true,
        onBackClick = { navigator.navigateBack() }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Category: ${destination.categoryId}")
            
            // Sample products in this category
            listOf("product-1", "product-2", "product-3").forEach { product ->
                NavigationButton(
                    text = "View $product",
                    onClick = {
                        navigator.navigate(
                            CategoryDestination.ProductInCategory(
                                categoryId = destination.categoryId,
                                productId = product
                            )
                        )
                    }
                )
            }
        }
    }
}

@Screen(CategoryDestination.ProductInCategory::class)
@Composable
fun ProductInCategoryScreen(
    destination: CategoryDestination.ProductInCategory,
    navigator: Navigator
) {
    RecipeScaffold(
        title = destination.productId,
        showBackButton = true,
        onBackClick = { navigator.navigateBack() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Product: ${destination.productId}")
            Text("Category: ${destination.categoryId}")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show the deep link URI that would reach this screen
            val uri = GeneratedScreenRegistry.createDeepLinkUri(
                destination,
                scheme = "myapp"
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Deep Link URI:", style = MaterialTheme.typography.labelMedium)
                    Text(uri ?: "N/A", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Back button will navigate through:\n" +
                "1. This screen → Category Detail → Category List",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// ============================================================
// NESTED DEEP LINK HANDLING
// ============================================================

/**
 * Handle a nested deep link with path reconstruction.
 * 
 * For deep links targeting nested screens, the NavNode architecture
 * can optionally reconstruct the full navigation path.
 * 
 * ## Example
 * 
 * URI: `myapp://categories/electronics/products/phone-123`
 * 
 * Options:
 * 1. **Direct navigation** - Jump directly to ProductInCategory
 * 2. **Path reconstruction** - Build stack: List → CategoryDetail → ProductInCategory
 * 
 * The NavNode tree supports both patterns through configuration.
 */
private fun handleNestedDeepLink(uri: String, navigator: Navigator) {
    when (val result = GeneratedDeepLinkHandler.handleDeepLink(uri)) {
        is DeepLinkResult.Matched -> {
            // Option 1: Direct navigation (simple)
            navigator.navigate(result.destination)
            
            // Option 2: Path reconstruction (if needed)
            // navigator.navigateWithPathReconstruction(result.destination)
        }
        is DeepLinkResult.NotMatched -> {
            println("Unknown nested deep link: $uri")
        }
    }
}

// ============================================================
// APP ENTRY POINT
// ============================================================

/**
 * Entry point for the Nested Deep Linking recipe.
 * 
 * Demonstrates handling deep links that target screens
 * within nested navigation hierarchies.
 */
@Composable
fun NestedDeepLinkApp() {
    val navTree = remember { buildCategoriesNavNode() }  // KSP-generated
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry  // KSP-generated
    )
}

// ============================================================
// NOTES FOR LLM AGENTS
// ============================================================

/**
 * ## Multi-Parameter Route Patterns
 * 
 * Routes can have multiple path parameters:
 * 
 * ```kotlin
 * @Destination(route = "categories/{categoryId}/products/{productId}")
 * data class ProductInCategory(
 *     val categoryId: String,
 *     val productId: String
 * ) : CategoryDestination()
 * ```
 * 
 * The KSP generator extracts parameters in order from the route template
 * and matches them to constructor parameters by name.
 * 
 * ## Path Reconstruction Strategies
 * 
 * 1. **Greedy** - Navigate directly to target (default)
 * 2. **Full Stack** - Reconstruct intermediate screens
 * 3. **Selective** - Configure which levels to include
 * 
 * Configuration via `QuoVadisHost`:
 * 
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     screenRegistry = registry,
 *     deepLinkStrategy = DeepLinkStrategy.ReconstructPath
 * )
 * ```
 * 
 * ## Testing Deep Links
 * 
 * Use `FakeNavigator` to verify deep link handling:
 * 
 * ```kotlin
 * @Test
 * fun testNestedDeepLink() {
 *     val navigator = FakeNavigator()
 *     
 *     handleNestedDeepLink("myapp://categories/a/products/b", navigator)
 *     
 *     val lastNav = navigator.navigationCalls.last()
 *     assert(lastNav is NavigationCall.Navigate)
 *     val dest = (lastNav as NavigationCall.Navigate).destination
 *     assert(dest is CategoryDestination.ProductInCategory)
 *     assert((dest as CategoryDestination.ProductInCategory).categoryId == "a")
 *     assert(dest.productId == "b")
 * }
 * ```
 */
```

---

## Implementation Checklist

### Step 1: Create Package Directory

```bash
mkdir -p quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/deeplink
```

### Step 2: Create Package Info

Create `package-info.kt` with LLM-optimized documentation.

### Step 3: Create Shared Destinations

Create `DeepLinkDestinations.kt` with:
- `ProductsDestination` (simple routes)
- `CategoryDestination` (nested routes)

### Step 4: Create Basic Recipe

Create `BasicDeepLinkRecipe.kt` demonstrating:
- Simple URI handling
- `GeneratedDeepLinkHandler` usage
- URI creation from destinations

### Step 5: Create Nested Recipe

Create `NestedDeepLinkRecipe.kt` demonstrating:
- Multi-parameter routes
- Path reconstruction concept
- Context-aware navigation

### Step 6: Verify Compilation

```bash
./gradlew :quo-vadis-recipes:compileKotlinMetadata
```

---

## Acceptance Criteria

- [ ] Package directory created
- [ ] `package-info.kt` with comprehensive KDoc
- [ ] `DeepLinkDestinations.kt` with route examples
- [ ] `BasicDeepLinkRecipe.kt` complete and documented
- [ ] `NestedDeepLinkRecipe.kt` complete and documented
- [ ] All screens have `@Screen` annotations
- [ ] Legacy API references included as comments
- [ ] Migration patterns documented
- [ ] LLM integration notes present
- [ ] Compiles successfully

## Related Tasks

- [PREP-001](./PREP-001-recipes-module.md) - Module setup (dependency)
- [MIG-001](./MIG-001-simple-stack-example.md) - Basic navigation patterns
- [MIG-002](./MIG-002-master-detail-example.md) - Parameterized destinations
- [KSP-004](../phase3-ksp/KSP-004-deep-link-handler.md) - Deep link handler generation
