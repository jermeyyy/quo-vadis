# MIG-002: Master-Detail Pattern Example

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-002 |
| **Complexity** | Medium |
| **Estimated Time** | 1.5 days |
| **Dependencies** | MIG-001 |
| **Output** | `docs/migration-examples/02-master-detail.md` |

## Objective

Demonstrate migration of the master-detail navigation pattern, focusing on typed arguments, deep linking via route templates, and shared element transitions.

## Patterns Demonstrated

| Pattern | Old API | New API |
|---------|---------|---------|
| Typed arguments | `@Argument(Data::class)` + `TypedDestination<T>` | Route template `{param}` |
| Deep link route | Static route string | Template `"path/{id}"` |
| Content receiving data | `fun Content(data: T, nav)` | `fun Screen(dest: Dest, nav)` |
| Shared elements | `SharedTransitionLayout` wrapper | Built-in via `QuoVadisHost` |
| Transition per-call | `navigate(dest, transition)` | `AnimationRegistry` |

## Example Content Structure

### 1. Before (Old API)

```kotlin
// === Destination with Typed Arguments ===
@Graph("catalog", startDestination = "list")
sealed class CatalogDestination : Destination {
    
    @Route("catalog/list")
    data object ProductList : CatalogDestination()
    
    @Route("catalog/detail")
    @Argument(ProductDetailData::class)
    data class ProductDetail(val productId: String) : CatalogDestination(),
        TypedDestination<ProductDetailData> {
        override val data: ProductDetailData
            get() = ProductDetailData(productId)
    }
}

// Separate data class for arguments
@Serializable
data class ProductDetailData(val productId: String)

// === Content Functions ===
@Content(CatalogDestination.ProductList::class)
@Composable
fun ProductListContent(navigator: Navigator) {
    val products = remember { sampleProducts() }
    
    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = {
                    navigator.navigate(
                        CatalogDestination.ProductDetail(product.id),
                        NavigationTransitions.SlideHorizontal
                    )
                }
            )
        }
    }
}

@Content(CatalogDestination.ProductDetail::class)
@Composable
fun ProductDetailContent(
    data: ProductDetailData,  // Receives separate data object
    navigator: Navigator
) {
    val product = remember(data.productId) { 
        loadProduct(data.productId) 
    }
    
    Column {
        Text("Product: ${product.name}")
        Text("ID: ${data.productId}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back to List")
        }
    }
}

// === Shared Element Transitions (Complex Setup) ===
@Composable
fun CatalogAppWithSharedElements() {
    val navigator = rememberNavigator()
    val graph = remember { catalogGraph() }
    
    // Requires manual SharedTransitionLayout wrapper
    SharedTransitionLayout {
        GraphNavHost(
            graph = graph,
            navigator = navigator,
            defaultTransition = NavigationTransitions.SlideHorizontal
        ) { destination, entry ->
            // Manual shared element scope passing
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
```

### 2. After (New API)

```kotlin
// === Destination with Route Template Parameters ===
@Stack(name = "catalog", startDestination = "ProductList")
sealed class CatalogDestination : Destination {
    
    @Destination(route = "catalog/list")
    data object ProductList : CatalogDestination()
    
    @Destination(route = "catalog/detail/{productId}")  // Route template!
    data class ProductDetail(val productId: String) : CatalogDestination()
}

// No separate data class needed!

// === Screen Functions ===
@Screen(CatalogDestination.ProductList::class)
@Composable
fun ProductListScreen(navigator: Navigator) {
    val products = remember { sampleProducts() }
    
    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // Transition configured via AnimationRegistry
                    navigator.navigate(CatalogDestination.ProductDetail(product.id))
                }
            )
        }
    }
}

@Screen(CatalogDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: CatalogDestination.ProductDetail,  // Receives destination instance directly
    navigator: Navigator
) {
    val product = remember(destination.productId) { 
        loadProduct(destination.productId) 
    }
    
    Column {
        Text("Product: ${product.name}")
        Text("ID: ${destination.productId}")
        Button(onClick = { navigator.navigateBack() }) {
            Text("Back to List")
        }
    }
}

// === App with Shared Elements (Built-in!) ===
@Composable
fun CatalogApp() {
    val navTree = remember { buildCatalogNavNode() }
    val navigator = rememberNavigator(navTree)
    
    // Shared elements work automatically - no wrapper needed!
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        animationRegistry = catalogAnimations  // Optional: custom transitions
    )
}

// === Animation Registry (Optional) ===
val catalogAnimations = AnimationRegistry {
    // Configure transitions by source → destination type
    from(CatalogDestination.ProductList::class)
        .to(CatalogDestination.ProductDetail::class)
        .uses(SlideHorizontal)
    
    // Default for all others
    default(FadeThrough)
}
```

### 3. Key Migration Steps

1. **Remove `@Argument` annotation** - No longer needed
2. **Remove `TypedDestination<T>` interface** - Data class params are direct
3. **Add route template** - Use `{paramName}` in `@Destination(route = "...")`
4. **Update screen signature** - Receive destination instance, not separate data
5. **Remove per-call transitions** - Configure via `AnimationRegistry`
6. **Remove `SharedTransitionLayout`** - Built-in to `QuoVadisHost`

### 4. Deep Linking Support

Route templates enable automatic deep link handling:

```kotlin
// Old: Manual deep link parsing required
// New: KSP generates handler automatically

// URL: myapp://catalog/detail/prod-123
// Automatically creates: CatalogDestination.ProductDetail("prod-123")

// Generated: GeneratedDeepLinkHandler.kt
object GeneratedDeepLinkHandler : DeepLinkHandler {
    override fun parse(uri: Uri): Destination? {
        return when {
            uri.path?.matches(Regex("catalog/detail/(.+)")) == true -> {
                val productId = uri.pathSegments.last()
                CatalogDestination.ProductDetail(productId)
            }
            uri.path == "catalog/list" -> CatalogDestination.ProductList
            else -> null
        }
    }
}
```

### 5. Shared Element Transitions

```kotlin
// Old: Manual scope management
@Composable
fun ProductCard(
    product: Product,
    sharedTransitionScope: SharedTransitionScope,  // Passed manually
    onClick: () -> Unit
) {
    with(sharedTransitionScope) {
        Image(
            modifier = Modifier.sharedElement(
                rememberSharedContentState(key = "product-${product.id}"),
                animatedVisibilityScope = // Complex scope management
            )
        )
    }
}

// New: Simplified with QuoVadisHost context
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    // SharedTransitionScope provided via CompositionLocal
    val sharedScope = LocalSharedTransitionScope.current
    
    Image(
        modifier = Modifier.sharedElement(key = "product-${product.id}")
    )
}
```

## Acceptance Criteria

- [ ] Example shows complete before/after transformation
- [ ] Route template parameter extraction is demonstrated
- [ ] Deep linking example is included
- [ ] Shared element transition simplification is shown
- [ ] AnimationRegistry configuration is documented
- [ ] Common pitfalls are listed

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Route param name mismatch | Ensure `{paramName}` matches data class property name |
| Complex type parameters | Only `String`, `Int`, `Long`, `Boolean` supported in routes |
| Missing `@Screen` for data class dest | Every destination variant needs a screen binding |
| Expecting `TypedDestination` interface | Use destination instance directly |

## Related Tasks

- [MIG-001: Simple Stack Navigation](./MIG-001-simple-stack-example.md)
- [MIG-005: Nested Tabs + Detail](./MIG-005-nested-tabs-detail-example.md)
- [RENDER-006: AnimationRegistry](../phase2-renderer/RENDER-006-animation-registry.md)
