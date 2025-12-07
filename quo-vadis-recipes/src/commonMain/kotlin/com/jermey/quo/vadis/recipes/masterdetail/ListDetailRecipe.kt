@file:Suppress("unused")

package com.jermey.quo.vadis.recipes.masterdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.compose.AnimationRegistry
import com.jermey.quo.vadis.core.navigation.compose.StandardAnimations
import com.jermey.quo.vadis.core.navigation.compose.TransitionType
import com.jermey.quo.vadis.core.navigation.compose.animationRegistry
import com.jermey.quo.vadis.core.navigation.compose.forwardTransition
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
import com.jermey.quo.vadis.core.navigation.core.Navigator

// ============================================================
// MIG-002: MASTER-DETAIL NAVIGATION RECIPE
// ============================================================

/**
 * # Master-Detail Navigation Recipe
 *
 * Demonstrates the classic master-detail (list-detail) navigation pattern using
 * the NavNode architecture. This pattern is fundamental for e-commerce product
 * catalogs, contact lists, email apps, and any list-to-detail flow.
 *
 * ## What This Recipe Shows
 *
 * 1. **Route Template Parameters** - Using `{productId}` placeholders for dynamic routes
 * 2. **Parameterized Destinations** - Data classes with constructor parameters
 * 3. **Screen Parameter Injection** - Receiving destination instance in @Screen functions
 * 4. **AnimationRegistry** - Custom transitions for list-to-detail navigation
 * 5. **Navigation with Data** - Passing typed data through navigation
 *
 * ## Route Template Parameters
 *
 * Route templates use `{paramName}` syntax to define dynamic segments:
 *
 * ```kotlin
 * @Destination(route = "catalog/detail/{productId}")
 * data class ProductDetail(val productId: String) : CatalogDestination()
 * ```
 *
 * When navigating with `navigator.navigate(ProductDetail("123"))`:
 * - The route becomes `catalog/detail/123`
 * - Deep link `myapp://catalog/detail/456` extracts `productId = "456"`
 *
 * ## Screen Parameter Patterns
 *
 * For destinations **without** parameters (data objects):
 * ```kotlin
 * @Screen(CatalogDestination.ProductList::class)
 * @Composable
 * fun ProductListScreen(navigator: Navigator) { ... }
 * ```
 *
 * For destinations **with** parameters (data classes):
 * ```kotlin
 * @Screen(CatalogDestination.ProductDetail::class)
 * @Composable
 * fun ProductDetailScreen(destination: CatalogDestination.ProductDetail, navigator: Navigator) { ... }
 * ```
 *
 * ## AnimationRegistry Usage
 *
 * Configure custom transitions centrally:
 *
 * ```kotlin
 * val catalogAnimations = animationRegistry {
 *     // Default slide animations
 *     useSlideForward()
 *     useSlideBackward()
 *
 *     // Custom transition for list → detail
 *     forwardTransition<CatalogDestination.ProductList, CatalogDestination.ProductDetail>(
 *         StandardAnimations.sharedAxis(SharedAxis.X)
 *     )
 * }
 * ```
 *
 * ## Migration Summary (Old API → New API)
 *
 * | Old API | New API | Purpose |
 * |---------|---------|---------|
 * | `@Route("path/{param}")` | `@Destination(route = "path/{param}")` | Route with parameters |
 * | `@Argument(DataClass::class)` | Route template `{param}` | Parameter definition |
 * | `TypedDestination<T>` | Data class properties | Access to parameters |
 * | Per-call transitions | `AnimationRegistry` | Centralized transition config |
 * | `data` property override | Direct property access | Parameter access |
 *
 * ## Key Migration Steps
 *
 * 1. **Remove `@Argument`**: Use route template `{param}` instead
 * 2. **Remove `TypedDestination<T>`**: Use data class properties directly
 * 3. **Screen signature**: Add destination as first parameter for data classes
 * 4. **Transition config**: Use `AnimationRegistry` instead of per-call transitions
 *
 * ## Production App Setup (After KSP Processing)
 *
 * ```kotlin
 * @Composable
 * fun CatalogApp() {
 *     // KSP generates buildCatalogNavNode() from @Stack annotation
 *     val navTree = remember { buildCatalogNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         animationRegistry = catalogAnimations  // Custom transitions
 *     )
 * }
 * ```
 *
 * @see CatalogDestination for destination definitions with route templates
 * @see AnimationRegistry for transition configuration
 * @see com.jermey.quo.vadis.annotations.Destination
 * @see com.jermey.quo.vadis.annotations.Screen
 */

// ============================================================
// DESTINATION DEFINITION
// ============================================================

/**
 * Product catalog navigation destinations demonstrating master-detail pattern.
 *
 * ## Route Template Parameters
 *
 * The `{productId}` in the route template:
 * - Maps to the `productId` constructor parameter
 * - Enables deep linking: `myapp://catalog/detail/SKU-123`
 * - Extracted automatically by KSP-generated deep link handler
 *
 * ## Sealed Class Structure
 *
 * ```
 * CatalogDestination (sealed)
 * ├── ProductList    (data object) - No parameters
 * └── ProductDetail  (data class)  - Has productId parameter
 * ```
 *
 * ## Migration from Old API
 *
 * **Before (Old API):**
 * ```kotlin
 * @Graph("catalog", startDestination = "catalog/list")
 * sealed class CatalogDestination : Destination {
 *
 *     @Route("catalog/list")
 *     data object ProductList : CatalogDestination()
 *
 *     @Route("catalog/detail")
 *     @Argument(ProductDetailData::class)
 *     data class ProductDetail(val productId: String) : CatalogDestination(),
 *         TypedDestination<ProductDetailData> {
 *         override val data = ProductDetailData(productId)
 *     }
 * }
 *
 * data class ProductDetailData(val productId: String)
 * ```
 *
 * **After (New API):**
 * ```kotlin
 * @Stack(name = "catalog", startDestination = "ProductList")
 * sealed class CatalogDestination : DestinationInterface {
 *
 *     @Destination(route = "catalog/list")
 *     data object ProductList : CatalogDestination()
 *
 *     @Destination(route = "catalog/detail/{productId}")
 *     data class ProductDetail(val productId: String) : CatalogDestination()
 * }
 * ```
 *
 * ## Key Differences
 *
 * 1. No separate data class needed - destination IS the data
 * 2. No `TypedDestination<T>` interface
 * 3. No `@Argument` annotation - route template defines parameters
 * 4. `startDestination` uses class name ("ProductList") not route
 */
@Stack(name = "catalog", startDestination = "ProductList")
sealed class CatalogDestination : DestinationInterface {

    /**
     * Product list screen - displays all available products.
     *
     * This is a **parameter-less destination** (data object).
     * The corresponding @Screen function receives only [Navigator].
     *
     * Defined as `startDestination = "ProductList"` in the @Stack annotation.
     */
    @Destination(route = "catalog/list")
    data object ProductList : CatalogDestination()

    /**
     * Product detail screen - displays details for a specific product.
     *
     * This is a **parameterized destination** (data class).
     * The `{productId}` route template parameter maps to [productId] property.
     *
     * ## Route Template Mapping
     *
     * - Route: `catalog/detail/{productId}`
     * - Navigation: `navigator.navigate(ProductDetail("SKU-123"))`
     * - Deep link: `myapp://catalog/detail/SKU-456` → `ProductDetail("SKU-456")`
     *
     * ## Screen Function Signature
     *
     * ```kotlin
     * @Screen(CatalogDestination.ProductDetail::class)
     * @Composable
     * fun ProductDetailScreen(
     *     destination: CatalogDestination.ProductDetail,  // ← Receives this instance
     *     navigator: Navigator
     * ) {
     *     val productId = destination.productId  // ← Access parameters
     * }
     * ```
     *
     * @property productId Unique identifier for the product to display
     */
    @Destination(route = "catalog/detail/{productId}")
    data class ProductDetail(val productId: String) : CatalogDestination()
}

// ============================================================
// ANIMATION CONFIGURATION
// ============================================================

/**
 * Custom animation registry for catalog navigation.
 *
 * Demonstrates centralized transition configuration using [AnimationRegistry].
 *
 * ## Configuration Options
 *
 * ```kotlin
 * val registry = animationRegistry {
 *     // Built-in presets
 *     useSlideForward()        // Default PUSH animation
 *     useSlideBackward()       // Default POP animation
 *     useFadeForTabs()         // TAB_SWITCH animation
 *     useNoAnimationForPanes() // PANE_SWITCH animation
 *
 *     // Custom per-transition
 *     forwardTransition<From, To>(spec)
 *     backwardTransition<From, To>(spec)
 *     biDirectionalTransition<From, To>(forward, backward)
 *
 *     // Low-level registration
 *     register(From::class, To::class, TransitionType.PUSH, spec)
 *     registerDefault(TransitionType.PUSH, spec)
 * }
 * ```
 *
 * ## Standard Animations
 *
 * - `StandardAnimations.slideForward()` - Slide in from right
 * - `StandardAnimations.slideBackward()` - Slide out to right
 * - `StandardAnimations.fade()` - Crossfade transition
 * - `StandardAnimations.scale()` - Scale up/down transition
 *
 * ## Usage with QuoVadisHost
 *
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     animationRegistry = catalogAnimations,  // ← Pass registry here
 *     screenRegistry = GeneratedScreenRegistry
 * )
 * ```
 */
val catalogAnimations: AnimationRegistry = animationRegistry {
    // Default animations for all navigation
    useSlideForward()
    useSlideBackward()

    // Custom animation for list → detail transition
    // This overrides the default for this specific transition
    forwardTransition<CatalogDestination.ProductList, CatalogDestination.ProductDetail>(
        StandardAnimations.slideForward()
    )

    // You can also register per-destination-type defaults:
    // registerDefault(TransitionType.PUSH, StandardAnimations.slideForward())
}

// ============================================================
// SAMPLE DATA
// ============================================================

/**
 * Sample product data for demonstration.
 */
private data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: String
)

/**
 * Sample product catalog.
 */
private val sampleProducts = listOf(
    Product("SKU-001", "Wireless Headphones", "Premium noise-canceling headphones", "$299.99"),
    Product("SKU-002", "Mechanical Keyboard", "RGB backlit gaming keyboard", "$149.99"),
    Product("SKU-003", "4K Monitor", "32-inch ultra-wide display", "$599.99"),
    Product("SKU-004", "USB-C Hub", "7-in-1 multiport adapter", "$79.99"),
    Product("SKU-005", "Webcam Pro", "1080p HD streaming camera", "$129.99")
)

// ============================================================
// SCREENS
// ============================================================

/**
 * Product list screen - master view in master-detail pattern.
 *
 * Demonstrates:
 * - Parameterless destination (data object) → Navigator-only signature
 * - Navigation with data: `navigator.navigate(ProductDetail(productId))`
 *
 * ## Screen Binding
 *
 * For **parameterless destinations**, the @Screen function receives only [Navigator]:
 *
 * ```kotlin
 * @Screen(CatalogDestination.ProductList::class)
 * @Composable
 * fun ProductListScreen(navigator: Navigator) { ... }
 * ```
 *
 * ## Migration from Old API
 *
 * **Before (Old API):**
 * ```kotlin
 * @Content(CatalogDestination.ProductList::class)
 * @Composable
 * fun ProductListContent(navigator: Navigator) {
 *     // Navigate with transition argument
 *     navigator.navigate(
 *         ProductDetail(productId),
 *         NavigationTransitions.SlideHorizontal
 *     )
 * }
 * ```
 *
 * **After (New API):**
 * ```kotlin
 * @Screen(CatalogDestination.ProductList::class)
 * @Composable
 * fun ProductListScreen(navigator: Navigator) {
 *     // Transition configured via AnimationRegistry, not per-call
 *     navigator.navigate(ProductDetail(productId))
 * }
 * ```
 *
 * @param navigator The navigator for navigation actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(CatalogDestination.ProductList::class)
@Composable
fun ProductListScreen(navigator: Navigator) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Catalog") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleProducts, key = { it.id }) { product ->
                ProductListItem(
                    product = product,
                    onClick = {
                        // Navigate to detail with product ID
                        // Transition animation comes from AnimationRegistry
                        navigator.navigate(CatalogDestination.ProductDetail(product.id))
                    }
                )
            }
        }
    }
}

/**
 * Product list item component.
 */
@Composable
private fun ProductListItem(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = product.price,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "ID: ${product.id}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

/**
 * Product detail screen - detail view in master-detail pattern.
 *
 * Demonstrates:
 * - Parameterized destination (data class) → Destination + Navigator signature
 * - Accessing route parameters via destination instance
 *
 * ## Screen Binding for Parameterized Destinations
 *
 * For **parameterized destinations**, the @Screen function receives the destination instance:
 *
 * ```kotlin
 * @Screen(CatalogDestination.ProductDetail::class)
 * @Composable
 * fun ProductDetailScreen(
 *     destination: CatalogDestination.ProductDetail,  // ← First parameter
 *     navigator: Navigator                             // ← Second parameter
 * ) {
 *     val productId = destination.productId  // ← Access parameters
 * }
 * ```
 *
 * ## Migration from Old API
 *
 * **Before (Old API):**
 * ```kotlin
 * @Content(CatalogDestination.ProductDetail::class)
 * @Composable
 * fun ProductDetailContent(data: ProductDetailData, navigator: Navigator) {
 *     val productId = data.productId
 * }
 * ```
 *
 * **After (New API):**
 * ```kotlin
 * @Screen(CatalogDestination.ProductDetail::class)
 * @Composable
 * fun ProductDetailScreen(
 *     destination: CatalogDestination.ProductDetail,
 *     navigator: Navigator
 * ) {
 *     val productId = destination.productId
 * }
 * ```
 *
 * ## Key Differences
 *
 * 1. **First parameter type**: Changed from separate data class to destination itself
 * 2. **No TypedDestination interface**: Parameters are direct properties
 * 3. **Type safety**: Destination class carries its own data
 *
 * @param destination The destination instance containing [CatalogDestination.ProductDetail.productId]
 * @param navigator The navigator for navigation actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(CatalogDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: CatalogDestination.ProductDetail,
    navigator: Navigator
) {
    // Access parameter from destination instance
    val productId = destination.productId

    // Find product from sample data (in real app, this would be a repository call)
    val product = sampleProducts.find { it.id == productId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Product Detail") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (product != null) {
                // Product info
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = product.price,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Technical details
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Product Details",
                            style = MaterialTheme.typography.titleMedium
                        )

                        DetailRow(label = "Product ID", value = product.id)
                        DetailRow(label = "Route Parameter", value = productId)
                        DetailRow(label = "Deep Link", value = "myapp://catalog/detail/$productId")
                    }
                }
            } else {
                // Product not found
                Text(
                    text = "Product not found",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )

                Text(
                    text = "Product ID: $productId",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Detail row component for displaying label-value pairs.
 */
@Composable
private fun DetailRow(label: String, value: String) {
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
// APP ENTRY POINT (Conceptual - requires KSP generation)
// ============================================================

/**
 * Entry point for the Master-Detail Navigation recipe.
 *
 * ## Production Setup (After KSP Processing)
 *
 * ```kotlin
 * @Composable
 * fun CatalogApp() {
 *     // KSP generates buildCatalogNavNode() from @Stack annotation
 *     val navTree = remember { buildCatalogNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         animationRegistry = catalogAnimations  // Custom transitions
 *     )
 * }
 * ```
 *
 * ## Comparison with Old API
 *
 * **Before (Old API):**
 * ```kotlin
 * @Composable
 * fun CatalogApp() {
 *     remember { initializeQuoVadisRoutes() }
 *
 *     val navigator = rememberNavigator()
 *     val graph = remember { catalogGraph() }
 *
 *     LaunchedEffect(navigator, graph) {
 *         navigator.registerGraph(graph)
 *         navigator.setStartDestination(CatalogDestination.ProductList)
 *     }
 *
 *     GraphNavHost(
 *         graph = graph,
 *         navigator = navigator,
 *         defaultTransition = NavigationTransitions.SlideHorizontal
 *     )
 * }
 * ```
 *
 * **After (New API):**
 * ```kotlin
 * @Composable
 * fun CatalogApp() {
 *     val navTree = remember { buildCatalogNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         animationRegistry = catalogAnimations
 *     )
 * }
 * ```
 *
 * ## Deep Linking
 *
 * KSP generates a `GeneratedDeepLinkHandler` that parses URIs:
 *
 * ```kotlin
 * // Parse deep link
 * val destination = GeneratedDeepLinkHandler.parse("myapp://catalog/detail/SKU-123")
 * // Result: CatalogDestination.ProductDetail("SKU-123")
 *
 * // Navigate via deep link
 * navigator.handleDeepLink(DeepLink("myapp://catalog/detail/SKU-123"))
 * ```
 */
@Composable
fun CatalogApp() {
    // This is a conceptual placeholder.
    // In production, KSP generates:
    // - buildCatalogNavNode() from @Stack/@Destination annotations
    // - GeneratedScreenRegistry from @Screen annotations
    // - GeneratedDeepLinkHandler for URI parsing
    //
    // Usage:
    //   val navTree = remember { buildCatalogNavNode() }
    //   val navigator = rememberNavigator(navTree)
    //   QuoVadisHost(
    //       navigator = navigator,
    //       screenRegistry = GeneratedScreenRegistry,
    //       animationRegistry = catalogAnimations
    //   )

    Text("CatalogApp - See KDoc for production implementation")
}
