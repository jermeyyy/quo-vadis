# Migration Guide: Master-Detail Pattern

> **Difficulty**: Medium | **Time Estimate**: 30-60 minutes | **Prerequisites**: [01-simple-stack.md](./01-simple-stack.md) complete

This guide demonstrates how to migrate master-detail navigation patterns (e.g., product list → product detail) from the old Quo Vadis API to the new NavNode architecture, with focus on typed arguments, deep linking via route templates, and shared element transitions.

---

## Table of Contents

1. [Overview](#overview)
2. [Before (Old API)](#before-old-api)
3. [After (New API)](#after-new-api)
4. [Key Migration Steps](#key-migration-steps)
5. [What KSP Generates](#what-ksp-generates)
6. [Common Pitfalls](#common-pitfalls)
7. [Next Steps](#next-steps)
8. [Related Resources](#related-resources)

---

## Overview

The master-detail pattern is common in catalog apps, email clients, and any UI showing a list that leads to detail views. The new NavNode architecture dramatically simplifies this pattern by:

- **Eliminating separate data classes** — Destination parameters are extracted directly from route templates
- **Simplifying typed arguments** — No more `TypedDestination<T>` interface or `@Argument` annotations
- **Enabling automatic deep linking** — Route templates like `{productId}` are parsed automatically
- **Building in shared element transitions** — No manual `SharedTransitionLayout` wrapper needed
- **Centralizing transition configuration** — `AnimationRegistry` replaces per-call transition arguments

### Annotation Changes Summary

| Old Annotation | New Annotation | Purpose |
|----------------|----------------|---------|
| `@Graph("name")` | `@Stack(name = "name", startDestination = "...")` | Define a navigation stack container |
| `@Route("path")` + `@Argument(Data::class)` | `@Destination(route = "path/{param}")` | Mark destination with typed route parameters |
| `@Content(Dest::class)` | `@Screen(Dest::class)` | Bind a Composable to render a destination |
| `TypedDestination<T>` interface | Not needed | Destination instance used directly |

### Host Changes Summary

| Old Component | New Component | Change |
|---------------|---------------|--------|
| `SharedTransitionLayout { GraphNavHost(...) }` | `QuoVadisHost(navigator, screenRegistry)` | Shared elements built-in |
| `navigate(dest, NavigationTransitions.Slide)` | `navigate(dest)` + `AnimationRegistry` | Centralized transition config |
| Manual deep link parsing | KSP-generated `DeepLinkHandler` | Automatic route template parsing |
| Separate `XxxDetailData` classes | Destination constructor parameters | Direct parameter access |

---

## Before (Old API)

### Complete Working Example

```kotlin
package com.example.catalog

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.*
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════
// STEP 1: Define the Navigation Graph with Typed Arguments
// ═══════════════════════════════════════════════════════════

/**
 * Product catalog navigation graph.
 * 
 * OLD: Uses @Graph, @Route, @Argument, and TypedDestination<T> for typed data.
 */
@Graph("catalog", startDestination = "catalog/list")
sealed class CatalogDestination : Destination {
    
    @Route("catalog/list")
    data object ProductList : CatalogDestination()
    
    @Route("catalog/detail")
    @Argument(ProductDetailData::class)  // Separate annotation for typed data
    data class ProductDetail(val productId: String) : CatalogDestination(),
        TypedDestination<ProductDetailData> {  // Must implement interface
        
        override val data: ProductDetailData
            get() = ProductDetailData(productId)  // Manual conversion required
    }
}

/**
 * OLD: Separate data class required for argument serialization.
 */
@Serializable
data class ProductDetailData(val productId: String)

// ═══════════════════════════════════════════════════════════
// STEP 2: Bind Content to Destinations
// ═══════════════════════════════════════════════════════════

/**
 * OLD: List screen uses @Content annotation.
 * Navigation requires explicit transition parameter.
 */
@Content(CatalogDestination.ProductList::class)
@Composable
fun ProductListContent(navigator: Navigator) {
    val products = remember { sampleProducts() }
    
    LazyColumn {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // OLD: Transition specified per-call
                    navigator.navigate(
                        CatalogDestination.ProductDetail(product.id),
                        NavigationTransitions.SlideHorizontal  // Repetitive!
                    )
                }
            )
        }
    }
}

/**
 * OLD: Detail screen receives SEPARATE data object, not the destination.
 */
@Content(CatalogDestination.ProductDetail::class)
@Composable
fun ProductDetailContent(
    data: ProductDetailData,  // Receives ProductDetailData, not ProductDetail
    navigator: Navigator
) {
    val product = remember(data.productId) { 
        loadProduct(data.productId) 
    }
    
    Column {
        // Product image
        ProductImage(productId = data.productId)
        
        Text(text = product.name, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Price: $${product.price}")
        Text(text = product.description)
        
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back to Catalog")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Set Up Navigation Host with Shared Elements
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Shared element transitions require manual wrapper and scope passing.
 */
@Composable
fun CatalogApp() {
    // Initialize routes globally
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val graph = remember { catalogGraph() }
    
    // Manual setup required
    LaunchedEffect(navigator, graph) {
        navigator.registerGraph(graph)
        navigator.setStartDestination(CatalogDestination.ProductList)
    }
    
    // OLD: SharedTransitionLayout wrapper required for shared elements
    SharedTransitionLayout {
        GraphNavHost(
            graph = graph,
            navigator = navigator,
            defaultTransition = NavigationTransitions.SlideHorizontal
        ) { destination, entry ->
            // Must manually pass SharedTransitionScope to each screen
            when (destination) {
                is CatalogDestination.ProductList -> 
                    ProductListWithSharedElements(navigator, this)
                is CatalogDestination.ProductDetail -> 
                    ProductDetailWithSharedElements(destination.data, navigator, this)
                else -> Unit
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 4: Shared Element Implementation (Complex!)
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Shared elements require explicit scope parameter and complex setup.
 */
@Composable
fun ProductListWithSharedElements(
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope  // Must be passed explicitly
) {
    val products = remember { sampleProducts() }
    
    LazyColumn {
        items(products, key = { it.id }) { product ->
            with(sharedTransitionScope) {  // Must use with() block
                Card(
                    modifier = Modifier
                        .clickable {
                            navigator.navigate(
                                CatalogDestination.ProductDetail(product.id),
                                NavigationTransitions.SlideHorizontal
                            )
                        }
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(
                                key = "card-${product.id}"
                            ),
                            animatedVisibilityScope = // Need to get scope from somewhere!
                        )
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(product.imageUrl),
                        contentDescription = product.name,
                        modifier = Modifier.sharedElement(
                            rememberSharedContentState(key = "image-${product.id}"),
                            animatedVisibilityScope = // Complex scope management!
                        )
                    )
                    Text(product.name)
                }
            }
        }
    }
}

@Composable
fun ProductDetailWithSharedElements(
    data: ProductDetailData,
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope
) {
    val product = remember(data.productId) { loadProduct(data.productId) }
    
    with(sharedTransitionScope) {
        Column {
            Image(
                painter = rememberAsyncImagePainter(product.imageUrl),
                contentDescription = product.name,
                modifier = Modifier.sharedElement(
                    rememberSharedContentState(key = "image-${data.productId}"),
                    animatedVisibilityScope = // Still need scope management!
                )
            )
            Text(product.name)
            Button(onClick = { navigator.navigateBack() }) {
                Text("Back")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Sample Data
// ═══════════════════════════════════════════════════════════

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String
)

fun sampleProducts() = listOf(
    Product("prod-001", "Wireless Headphones", 79.99, "Premium sound quality", "..."),
    Product("prod-002", "Smart Watch", 199.99, "Track your fitness", "..."),
    Product("prod-003", "Portable Charger", 29.99, "10000mAh capacity", "...")
)

fun loadProduct(id: String): Product = sampleProducts().first { it.id == id }
```

### Old API Characteristics

1. **`@Argument` annotation** required for typed parameters
2. **`TypedDestination<T>` interface** must be implemented for data access
3. **Separate data class** (`ProductDetailData`) for serialization
4. **`data` property override** to convert destination to data object
5. **Per-call transitions** — `navigate(dest, transition)` repeated everywhere
6. **`SharedTransitionLayout` wrapper** required for shared element animations
7. **Manual scope passing** — `SharedTransitionScope` parameter threading

---

## After (New API)

### Complete Migrated Example

```kotlin
package com.example.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.compose.LocalSharedTransitionScope
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.AnimationRegistry
import com.example.catalog.generated.GeneratedScreenRegistry
import com.example.catalog.generated.buildCatalogNavNode

// ═══════════════════════════════════════════════════════════
// STEP 1: Define the Navigation Stack with Route Templates
// ═══════════════════════════════════════════════════════════

/**
 * Product catalog navigation stack.
 * 
 * NEW: Uses @Stack and @Destination with route templates.
 * No @Argument annotation, no TypedDestination interface!
 */
@Stack(name = "catalog", startDestination = "ProductList")
sealed class CatalogDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    
    @Destination(route = "catalog/list")
    data object ProductList : CatalogDestination()
    
    // NEW: Route template {productId} — enables automatic deep linking!
    @Destination(route = "catalog/detail/{productId}")
    data class ProductDetail(val productId: String) : CatalogDestination()
    // No TypedDestination interface needed!
    // No separate data class needed!
    // No data property override needed!
}

// ═══════════════════════════════════════════════════════════
// STEP 2: Bind Screens to Destinations
// ═══════════════════════════════════════════════════════════

/**
 * NEW: List screen uses @Screen annotation.
 * No transition specified — configured via AnimationRegistry.
 */
@Screen(CatalogDestination.ProductList::class)
@Composable
fun ProductListScreen(navigator: Navigator) {
    val products = remember { sampleProducts() }
    
    LazyColumn {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // NEW: Just navigate! Transition comes from AnimationRegistry
                    navigator.navigate(CatalogDestination.ProductDetail(product.id))
                }
            )
        }
    }
}

/**
 * NEW: Detail screen receives the DESTINATION INSTANCE directly.
 * Access parameters via destination.productId — clean and type-safe!
 */
@Screen(CatalogDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: CatalogDestination.ProductDetail,  // Destination instance, not separate data class!
    navigator: Navigator
) {
    val product = remember(destination.productId) { 
        loadProduct(destination.productId)  // Direct property access
    }
    
    Column {
        // Product image with shared element
        ProductImage(productId = destination.productId)
        
        Text(text = product.name, style = MaterialTheme.typography.headlineMedium)
        Text(text = "Price: $${product.price}")
        Text(text = product.description)
        
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back to Catalog")
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Set Up Navigation Host (Simplified!)
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Minimal setup with built-in shared element support.
 * No SharedTransitionLayout wrapper needed!
 */
@Composable
fun CatalogApp() {
    // KSP generates buildCatalogNavNode() from @Stack annotation
    val navTree = remember { buildCatalogNavNode() }
    
    // Navigator initialized directly with NavNode tree
    val navigator = rememberNavigator(navTree)
    
    // NEW: QuoVadisHost with built-in shared elements
    // No SharedTransitionLayout wrapper required!
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        animationRegistry = catalogAnimations  // Optional: custom transitions
    )
}

// ═══════════════════════════════════════════════════════════
// STEP 4: Animation Registry (Centralized Configuration)
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Configure all transitions in one place.
 * No more repeating NavigationTransitions.SlideHorizontal everywhere!
 */
val catalogAnimations = AnimationRegistry {
    // Configure transitions by source → destination type
    from(CatalogDestination.ProductList::class)
        .to(CatalogDestination.ProductDetail::class)
        .uses(SlideHorizontal)
    
    // Configure reverse transition (detail → list)
    from(CatalogDestination.ProductDetail::class)
        .to(CatalogDestination.ProductList::class)
        .uses(SlideHorizontalReverse)
    
    // Default for all other transitions
    default(FadeThrough)
}

// ═══════════════════════════════════════════════════════════
// STEP 5: Shared Element Transitions (Simplified!)
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Shared elements work via CompositionLocal — no parameter passing!
 */
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    // NEW: SharedTransitionScope provided via CompositionLocal
    val sharedScope = LocalSharedTransitionScope.current
    
    Card(
        modifier = Modifier
            .clickable(onClick = onClick)
            .sharedBounds(key = "card-${product.id}")  // Simplified API
    ) {
        Image(
            painter = rememberAsyncImagePainter(product.imageUrl),
            contentDescription = product.name,
            modifier = Modifier.sharedElement(key = "image-${product.id}")  // Just a key!
        )
        Text(product.name)
    }
}

@Composable
fun ProductImage(productId: String) {
    val product = remember(productId) { loadProduct(productId) }
    
    Image(
        painter = rememberAsyncImagePainter(product.imageUrl),
        contentDescription = product.name,
        modifier = Modifier.sharedElement(key = "image-$productId")  // Same key = shared element!
    )
}

// ═══════════════════════════════════════════════════════════
// Sample Data (unchanged)
// ═══════════════════════════════════════════════════════════

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val description: String,
    val imageUrl: String
)

fun sampleProducts() = listOf(
    Product("prod-001", "Wireless Headphones", 79.99, "Premium sound quality", "..."),
    Product("prod-002", "Smart Watch", 199.99, "Track your fitness", "..."),
    Product("prod-003", "Portable Charger", 29.99, "10000mAh capacity", "...")
)

fun loadProduct(id: String): Product = sampleProducts().first { it.id == id }
```

### New API Characteristics

1. **Route templates** — `{productId}` in route enables automatic parameter extraction
2. **Direct destination access** — Screen receives `CatalogDestination.ProductDetail`, not separate data class
3. **No `TypedDestination` interface** — Eliminated completely
4. **No `@Argument` annotation** — Route template handles typed parameters
5. **`AnimationRegistry`** — Centralized transition configuration
6. **Built-in shared elements** — `QuoVadisHost` handles `SharedTransitionScope` automatically
7. **CompositionLocal for sharing** — `LocalSharedTransitionScope.current` replaces parameter passing

---

## Key Migration Steps

Follow these steps to migrate your master-detail navigation:

### Step 1: Update Graph Annotation

```diff
- @Graph("catalog", startDestination = "catalog/list")
+ @Stack(name = "catalog", startDestination = "ProductList")
  sealed class CatalogDestination : Destination {
```

> ⚠️ **Important**: `startDestination` now uses the **class name** (`"ProductList"`) not the route (`"catalog/list"`).

### Step 2: Update Route to Destination with Template

```diff
-     @Route("catalog/detail")
-     @Argument(ProductDetailData::class)
-     data class ProductDetail(val productId: String) : CatalogDestination(),
-         TypedDestination<ProductDetailData> {
-         override val data: ProductDetailData
-             get() = ProductDetailData(productId)
-     }
+     @Destination(route = "catalog/detail/{productId}")
+     data class ProductDetail(val productId: String) : CatalogDestination()
```

> 💡 **Tip**: The `{productId}` in the route template must match the constructor parameter name exactly.

### Step 3: Delete Separate Data Class

```diff
- /**
-  * OLD: Separate data class required for argument serialization.
-  */
- @Serializable
- data class ProductDetailData(val productId: String)
```

No separate data class needed! The destination itself carries the parameters.

### Step 4: Update Content to Screen Annotation

```diff
- @Content(CatalogDestination.ProductList::class)
+ @Screen(CatalogDestination.ProductList::class)
  @Composable
- fun ProductListContent(navigator: Navigator) {
+ fun ProductListScreen(navigator: Navigator) {
```

### Step 5: Update Screen Signature for Data Class Destinations

```diff
- @Content(CatalogDestination.ProductDetail::class)
+ @Screen(CatalogDestination.ProductDetail::class)
  @Composable
- fun ProductDetailContent(
-     data: ProductDetailData,  // Separate data object
+ fun ProductDetailScreen(
+     destination: CatalogDestination.ProductDetail,  // Destination instance
      navigator: Navigator
  ) {
-     val product = remember(data.productId) { 
-         loadProduct(data.productId) 
+     val product = remember(destination.productId) { 
+         loadProduct(destination.productId) 
      }
```

> ⚠️ **Important**: Destination parameter must come **before** navigator for data class destinations.

### Step 6: Remove Per-Call Transitions

```diff
  onClick = {
-     navigator.navigate(
-         CatalogDestination.ProductDetail(product.id),
-         NavigationTransitions.SlideHorizontal
-     )
+     navigator.navigate(CatalogDestination.ProductDetail(product.id))
  }
```

### Step 7: Create AnimationRegistry

```kotlin
// NEW: Centralized transition configuration
val catalogAnimations = AnimationRegistry {
    from(CatalogDestination.ProductList::class)
        .to(CatalogDestination.ProductDetail::class)
        .uses(SlideHorizontal)
    
    from(CatalogDestination.ProductDetail::class)
        .to(CatalogDestination.ProductList::class)
        .uses(SlideHorizontalReverse)
    
    default(FadeThrough)
}
```

### Step 8: Remove SharedTransitionLayout Wrapper

```diff
  @Composable
  fun CatalogApp() {
-     remember { initializeQuoVadisRoutes() }
-     
-     val navigator = rememberNavigator()
-     val graph = remember { catalogGraph() }
-     
-     LaunchedEffect(navigator, graph) {
-         navigator.registerGraph(graph)
-         navigator.setStartDestination(CatalogDestination.ProductList)
-     }
-     
-     SharedTransitionLayout {
-         GraphNavHost(
-             graph = graph,
-             navigator = navigator,
-             defaultTransition = NavigationTransitions.SlideHorizontal
-         ) { destination, entry ->
-             // Manual screen routing...
-         }
-     }
+     val navTree = remember { buildCatalogNavNode() }
+     val navigator = rememberNavigator(navTree)
+     
+     QuoVadisHost(
+         navigator = navigator,
+         screenRegistry = GeneratedScreenRegistry,
+         animationRegistry = catalogAnimations
+     )
  }
```

### Step 9: Simplify Shared Element Code

```diff
  @Composable
  fun ProductCard(
      product: Product,
-     sharedTransitionScope: SharedTransitionScope,
      onClick: () -> Unit
  ) {
-     with(sharedTransitionScope) {
-         Card(
-             modifier = Modifier
-                 .clickable(onClick = onClick)
-                 .sharedBounds(
-                     sharedContentState = rememberSharedContentState(
-                         key = "card-${product.id}"
-                     ),
-                     animatedVisibilityScope = // Complex scope...
-                 )
-         ) {
+     Card(
+         modifier = Modifier
+             .clickable(onClick = onClick)
+             .sharedBounds(key = "card-${product.id}")
+     ) {
              Image(
                  painter = rememberAsyncImagePainter(product.imageUrl),
                  contentDescription = product.name,
-                 modifier = Modifier.sharedElement(
-                     rememberSharedContentState(key = "image-${product.id}"),
-                     animatedVisibilityScope = // Complex scope...
-                 )
+                 modifier = Modifier.sharedElement(key = "image-${product.id}")
              )
              Text(product.name)
-         }
      }
  }
```

### Step 10: Update Imports

```diff
- import com.jermey.quo.vadis.core.navigation.Graph
- import com.jermey.quo.vadis.core.navigation.Route
- import com.jermey.quo.vadis.core.navigation.Argument
- import com.jermey.quo.vadis.core.navigation.Content
- import com.jermey.quo.vadis.core.navigation.TypedDestination
- import com.jermey.quo.vadis.core.navigation.GraphNavHost
- import com.jermey.quo.vadis.core.navigation.NavigationTransitions
- import androidx.compose.animation.SharedTransitionLayout
+ import com.jermey.quo.vadis.annotations.Destination
+ import com.jermey.quo.vadis.annotations.Screen
+ import com.jermey.quo.vadis.annotations.Stack
+ import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
+ import com.jermey.quo.vadis.core.navigation.compose.LocalSharedTransitionScope
+ import com.jermey.quo.vadis.core.navigation.core.AnimationRegistry
+ import com.example.catalog.generated.GeneratedScreenRegistry
+ import com.example.catalog.generated.buildCatalogNavNode
```

### Step 11: Build and Verify

```bash
./gradlew :app:assembleDebug
```

Check for generated files in:
```
build/generated/ksp/debug/kotlin/com/example/catalog/generated/
├── CatalogNavNodeBuilder.kt        # buildCatalogNavNode() function
├── GeneratedScreenRegistry.kt      # ScreenRegistry with destination mapping
└── GeneratedDeepLinkHandler.kt     # Deep link route template parser
```

---

## What KSP Generates

KSP processes your annotations and generates the following code:

### CatalogNavNodeBuilder.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.catalog.generated

import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.example.catalog.CatalogDestination

/**
 * Builds the initial NavNode tree for the "catalog" stack.
 *
 * @return A StackNode containing the start destination (ProductList)
 */
fun buildCatalogNavNode(): StackNode {
    return StackNode(
        key = "catalog",
        parentKey = null,
        children = listOf(
            ScreenNode(
                key = "catalog/ProductList",
                parentKey = "catalog",
                destination = CatalogDestination.ProductList
            )
        )
    )
}

/**
 * Creates a ProductDetail ScreenNode from route parameters.
 * Used for deep linking and programmatic navigation.
 *
 * @param productId The product identifier from route template
 * @return A ScreenNode for the ProductDetail destination
 */
fun buildProductDetailNode(
    parentKey: String,
    productId: String
): ScreenNode {
    return ScreenNode(
        key = "catalog/ProductDetail/$productId",
        parentKey = parentKey,
        destination = CatalogDestination.ProductDetail(productId)
    )
}
```

### GeneratedScreenRegistry.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.catalog.generated

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenRegistry
import com.example.catalog.CatalogDestination
import com.example.catalog.ProductListScreen
import com.example.catalog.ProductDetailScreen

/**
 * KSP-generated screen registry mapping destinations to composable content.
 */
object GeneratedScreenRegistry : ScreenRegistry {

    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        when (destination) {
            // CatalogDestination screens
            is CatalogDestination.ProductList -> ProductListScreen(navigator)
            
            // Data class destination — passes destination instance directly
            is CatalogDestination.ProductDetail -> ProductDetailScreen(
                destination,  // Full destination instance, not separate data class
                navigator
            )

            else -> error("No screen registered for destination: $destination")
        }
    }

    override fun hasContent(destination: Destination): Boolean = when (destination) {
        is CatalogDestination.ProductList,
        is CatalogDestination.ProductDetail -> true
        else -> false
    }
}
```

### GeneratedDeepLinkHandler.kt

```kotlin
// Generated by Quo Vadis KSP Processor
// DO NOT EDIT - This file is auto-generated

package com.example.catalog.generated

import android.net.Uri
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.DeepLinkHandler
import com.example.catalog.CatalogDestination

/**
 * KSP-generated deep link handler for catalog routes.
 * 
 * Supported deep links:
 * - myapp://catalog/list → CatalogDestination.ProductList
 * - myapp://catalog/detail/{productId} → CatalogDestination.ProductDetail(productId)
 */
object GeneratedDeepLinkHandler : DeepLinkHandler {

    // Route template pattern for ProductDetail
    private val productDetailPattern = Regex("catalog/detail/([^/]+)")

    override fun parse(uri: Uri): Destination? {
        val path = uri.path?.removePrefix("/") ?: return null
        
        return when {
            // Static route: catalog/list
            path == "catalog/list" -> CatalogDestination.ProductList
            
            // Template route: catalog/detail/{productId}
            productDetailPattern.matches(path) -> {
                val match = productDetailPattern.matchEntire(path)
                val productId = match?.groupValues?.get(1) ?: return null
                CatalogDestination.ProductDetail(productId)
            }
            
            else -> null
        }
    }

    override fun canHandle(uri: Uri): Boolean = parse(uri) != null
}

// Usage example:
// val uri = Uri.parse("myapp://catalog/detail/prod-123")
// val destination = GeneratedDeepLinkHandler.parse(uri)
// // destination == CatalogDestination.ProductDetail("prod-123")
```

### How the Generated Code Works

```
┌─────────────────────────────────────────────────────────────────┐
│                         Your Code                                │
├─────────────────────────────────────────────────────────────────┤
│  @Stack("catalog", startDestination = "ProductList")             │
│  sealed class CatalogDestination {                               │
│      @Destination(route = "catalog/detail/{productId}")          │
│      data class ProductDetail(val productId: String)             │
│  }                                                               │
│                                                                  │
│  @Screen(CatalogDestination.ProductDetail::class)                │
│  fun ProductDetailScreen(dest: ..., nav: Navigator)              │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼ KSP Processes
┌─────────────────────────────────────────────────────────────────┐
│                    Generated Code                                │
├─────────────────────────────────────────────────────────────────┤
│  buildCatalogNavNode()      →  Creates initial StackNode tree   │
│  GeneratedScreenRegistry    →  Maps dest → screen (with params) │
│  GeneratedDeepLinkHandler   →  Parses route templates           │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼ Used by
┌─────────────────────────────────────────────────────────────────┐
│                       Runtime                                    │
├─────────────────────────────────────────────────────────────────┤
│  URL: myapp://catalog/detail/prod-123                            │
│         │                                                        │
│         ▼                                                        │
│  DeepLinkHandler.parse(uri)                                      │
│         │                                                        │
│         ▼                                                        │
│  CatalogDestination.ProductDetail("prod-123")                    │
│         │                                                        │
│         ▼                                                        │
│  ScreenRegistry.Content(destination, navigator, ...)             │
│         │                                                        │
│         ▼                                                        │
│  ProductDetailScreen(destination, navigator)                     │
└─────────────────────────────────────────────────────────────────┘
```

### Deep Link Flow Visualization

```
┌─────────────────────────────────────────────────────────────────┐
│  Deep Link: myapp://catalog/detail/prod-123                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Route Template: "catalog/detail/{productId}"                    │
│                                                                  │
│  Pattern Match: catalog/detail/([^/]+)                           │
│  Captures: productId = "prod-123"                                │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Created: CatalogDestination.ProductDetail("prod-123")           │
│                                                                  │
│  Passed to: ProductDetailScreen(destination, navigator)          │
│  Access via: destination.productId // "prod-123"                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Common Pitfalls

| Pitfall | Symptom | Solution |
|---------|---------|----------|
| **Route param name mismatch** | `IllegalArgumentException: Missing parameter 'productId'` | Ensure `{paramName}` in route matches the data class property name exactly |
| **Complex type in route** | Compilation error or runtime crash | Only `String`, `Int`, `Long`, `Boolean` are supported in route templates. Use query params or state for complex types |
| **Wrong `startDestination` format** | `IllegalArgumentException: Cannot find destination "catalog/list"` | Use class name (`"ProductList"`) not route (`"catalog/list"`) |
| **Missing `@Screen` for data class** | `No screen registered for destination: CatalogDestination.ProductDetail` | Every destination variant needs a `@Screen` binding |
| **Expecting `TypedDestination` interface** | Compilation error trying to access `.data` | Remove interface, access properties directly from destination instance |
| **Using old `@Argument` annotation** | `Unresolved reference: Argument` | Remove completely — use route templates instead |
| **Wrong parameter order in screen** | Destination data not accessible | Put destination parameter **before** navigator |
| **Forgetting to delete data class** | Unused class warning, confusion | Delete separate `XxxData` classes — destination carries params |
| **SharedTransitionLayout still present** | Double wrapping, animation issues | Remove wrapper — `QuoVadisHost` handles it |
| **Per-call transitions mixed with registry** | Inconsistent behavior | Remove all per-call transitions, use `AnimationRegistry` only |
| **Optional params without defaults** | Deep link fails for missing params | Use default values: `data class Detail(val id: String = "")` |

### Debugging Tips

1. **Route template issues**: Print the route pattern regex generated by KSP to verify matching
2. **Parameter extraction**: Log `destination.productId` in your screen to verify values
3. **Deep link testing**: Use `adb shell am start -a android.intent.action.VIEW -d "myapp://catalog/detail/test-123"` to test
4. **Build clean**: `./gradlew clean build` forces KSP regeneration if templates change
5. **Check generated handler**: Review `GeneratedDeepLinkHandler.kt` for route patterns

### AnimationRegistry Configuration Tips

```kotlin
val catalogAnimations = AnimationRegistry {
    // Specific pair transitions
    from(CatalogDestination.ProductList::class)
        .to(CatalogDestination.ProductDetail::class)
        .uses(SlideHorizontal)
    
    // Wildcard: any destination to ProductDetail
    toAny(CatalogDestination.ProductDetail::class)
        .uses(SlideHorizontal)
    
    // Wildcard: from ProductDetail to any destination
    fromAny(CatalogDestination.ProductDetail::class)
        .uses(SlideHorizontalReverse)
    
    // Default fallback
    default(FadeThrough)
}
```

> 💡 **Tip**: More specific rules take precedence. `from().to()` beats `toAny()` beats `default()`.

---

## Next Steps

After migrating master-detail navigation:

- **[03-tabbed-navigation.md](./03-tabbed-navigation.md)** — Migrate tabbed navigation with bottom bars
- **[04-adaptive-panes.md](./04-adaptive-panes.md)** — Migrate adaptive multi-pane layouts
- **[05-nested-tabs-detail.md](./05-nested-tabs-detail.md)** — Migrate complex nested tabs with detail screens

---

## Related Resources

- [01-simple-stack.md](./01-simple-stack.md) — Prerequisite: Simple stack migration guide
- [API Change Summary](./api-change-summary.md) — Complete annotation and API reference
- [Phase 1: NavNode Architecture](../refactoring-plan/phase1-core/CORE-001-navnode-hierarchy.md) — NavNode type definitions
- [Phase 2: QuoVadisHost](../refactoring-plan/phase2-renderer/RENDER-004-quovadis-host.md) — Unified renderer details
- [Phase 2: AnimationRegistry](../refactoring-plan/phase2-renderer/RENDER-006-animation-registry.md) — Animation configuration details
- [Phase 4: Annotations](../refactoring-plan/phase4-annotations/) — Full annotation specifications
- [MIG-002 Spec](../refactoring-plan/phase5-migration/MIG-002-master-detail-example.md) — Original task specification
