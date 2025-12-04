# MIG-005: Nested Tabs + Detail Example

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-005 |
| **Complexity** | Medium |
| **Estimated Time** | 1.5 days |
| **Dependencies** | MIG-002, MIG-003 |
| **Output** | `docs/migration-examples/05-nested-tabs-detail.md` |

## Objective

Demonstrate migration of complex navigation hierarchies where detail screens render on top of (covering) the tab bar, with proper predictive back behavior across navigation layers.

## Patterns Demonstrated

| Pattern | Old API | New API |
|---------|---------|---------|
| Full-screen detail | Nested `NavHost` in root | Unified `QuoVadisHost` |
| Covering tab bar | Complex `GraphNavHost` structure | Automatic z-ordering |
| Predictive back across layers | Manual `BackHandler` coordination | Unified speculative pop |
| Shared elements tab ↔ detail | Not possible (different hosts) | Native support |
| Deep navigation from tabs | Manual parent navigator access | Single navigator |

## Example Content Structure

### 1. Before (Old API) - The Nested Host Problem

```kotlin
// === Root Level: Handles full-screen destinations ===
@Graph("app", startDestination = "main_tabs")
sealed class AppDestination : Destination {
    @Route("app/main_tabs")
    data object MainTabs : AppDestination()
    
    // Full-screen detail that covers tabs
    @Route("app/product_detail")
    @Argument(ProductDetailData::class)
    data class ProductDetail(val productId: String) : AppDestination(),
        TypedDestination<ProductDetailData> {
        override val data = ProductDetailData(productId)
    }
    
    @Route("app/image_gallery")
    data class ImageGallery(val productId: String) : AppDestination()
}

// === Tab Destinations (separate graphs) ===
@Graph("home", startDestination = "feed")
sealed class HomeDestination : Destination {
    @Route("home/feed") data object Feed : HomeDestination()
    @Route("home/category") data class Category(val id: String) : HomeDestination()
}

// === Complex Nested Host Structure ===
@Composable
fun AppRoot() {
    val rootNavigator = rememberNavigator()
    val rootGraph = remember { appGraph() }
    
    // Root level host
    GraphNavHost(
        graph = rootGraph,
        navigator = rootNavigator,
        defaultTransition = NavigationTransitions.FadeThrough
    )
}

@Content(AppDestination.MainTabs::class)
@Composable
fun MainTabsContent(
    parentNavigator: Navigator,  // Need parent reference!
    parentEntry: BackStackEntry
) {
    val tabState = rememberTabNavigator(
        config = MainTabsConfig,
        parentNavigator = parentNavigator,  // Pass parent for full-screen nav
        parentEntry = parentEntry
    )
    
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = mapOf(
            MainTab.Home to homeGraph(),
            MainTab.Search to searchGraph(),
            MainTab.Profile to profileGraph()
        ),
        tabUI = { tabContent ->
            Scaffold(bottomBar = { BottomNav(tabState) }) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    tabContent()
                }
            }
        }
    )
}

// === Navigate to full-screen from within tab ===
@Content(HomeDestination.Feed::class)
@Composable
fun FeedContent(navigator: Navigator, parentNavigator: Navigator?) {
    // Must use PARENT navigator to show detail over tabs!
    val products = loadProducts()
    
    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // Navigate on PARENT to cover tabs
                    parentNavigator?.navigate(
                        AppDestination.ProductDetail(product.id)
                    ) ?: run {
                        // Fallback if no parent - shows within tab (wrong!)
                        navigator.navigate(/* ... */)
                    }
                }
            )
        }
    }
}

// === Predictive Back Issues ===
// Problem 1: Back gesture in ProductDetail handled by root
// Problem 2: Back gesture in Feed handled by tab navigator
// Problem 3: No coordination between them
// Problem 4: Shared elements IMPOSSIBLE across the boundary
```

### 2. After (New API) - Unified Host

```kotlin
// === Single Destination Hierarchy ===
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : Destination {
    
    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()
    
    @TabItem(label = "Search", icon = "search", rootGraph = SearchDestination::class)
    @Destination(route = "tabs/search")
    data object Search : MainTabs()
    
    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tabs/profile")
    data object Profile : MainTabs()
}

@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/category/{id}")
    data class Category(val id: String) : HomeDestination()
}

// Full-screen destinations - part of the SAME tree!
@Stack(name = "fullscreen", startDestination = "ProductDetail")
sealed class FullscreenDestination : Destination {
    
    @Destination(route = "product/{productId}")
    data class ProductDetail(val productId: String) : FullscreenDestination()
    
    @Destination(route = "gallery/{productId}")
    data class ImageGallery(val productId: String) : FullscreenDestination()
}

// === Single QuoVadisHost Handles Everything ===
@Composable
fun AppRoot() {
    val navTree = remember { buildMainTabsNavNode() }
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        tabWrapper = { tabNode, tabContent ->
            Scaffold(
                bottomBar = {
                    // Tab bar is part of wrapper - detail screens cover it!
                    MainBottomNav(
                        activeIndex = tabNode.activeStackIndex,
                        onTabSelected = { navigator.switchTab(it) }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    tabContent()
                }
            }
        }
        // No special handling needed for full-screen!
        // QuoVadisHost z-orders automatically
    )
}

// === Navigate to full-screen from within tab ===
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {  // Single navigator!
    val products = loadProducts()
    
    LazyColumn {
        items(products) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // Same navigator - detail renders on top automatically
                    navigator.navigate(FullscreenDestination.ProductDetail(product.id))
                },
                modifier = Modifier.sharedElement(key = "product-${product.id}")
            )
        }
    }
}

@Screen(FullscreenDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: FullscreenDestination.ProductDetail,
    navigator: Navigator
) {
    // Full screen - no padding from tab bar
    Column(modifier = Modifier.fillMaxSize()) {
        // Back button or gesture takes user back to tabs
        IconButton(onClick = { navigator.navigateBack() }) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        
        // Shared element continues transition from tab
        Image(
            painter = rememberProductImage(destination.productId),
            modifier = Modifier.sharedElement(key = "product-${destination.productId}")
        )
        
        // Navigate deeper
        Button(onClick = {
            navigator.navigate(FullscreenDestination.ImageGallery(destination.productId))
        }) {
            Text("View Gallery")
        }
    }
}
```

### 3. How the Unified Host Works

```kotlin
// === Z-Ordering via Flattening ===

// State tree structure:
// TabNode("mainTabs")
//   ├── StackNode("home")
//   │     └── ScreenNode(HomeDestination.Feed)
//   ├── StackNode("search")
//   │     └── ScreenNode(SearchDestination.Main)
//   └── StackNode("profile")
//         └── ScreenNode(ProfileDestination.Main)
//
// After pushing ProductDetail:
// TabNode("mainTabs")
//   ├── ... (tabs unchanged)
//   └── ScreenNode(FullscreenDestination.ProductDetail)  // Higher z-index!

// flattenState() produces:
// 1. TabNode wrapper (z=0) - contains bottom nav
// 2. Active tab content (z=1) - Feed screen
// 3. ProductDetail (z=2) - Covers everything

// QuoVadisHost renders:
Box {
    // z=0: Tab wrapper with bottom nav
    tabWrapper(tabNode) {
        // z=1: Active tab content
        FeedScreen(navigator)
    }
    // z=2: Full-screen detail (outside wrapper!)
    ProductDetailScreen(destination, navigator)
}
```

### 4. Predictive Back Across Layers

```kotlin
// === Unified Predictive Back ===

// Old: Two separate back handlers fighting
// New: Single QuoVadisHost manages all

// User swipes back from ProductDetail:
// 1. QuoVadisHost detects gesture
// 2. Creates "speculative pop" state
// 3. Renders BOTH ProductDetail AND Feed simultaneously
// 4. Applies physics-based transforms:
//    - ProductDetail: scales/slides away
//    - Feed (behind tabs): scales up from 0.9 → 1.0
// 5. On commit: NavNode tree updated, ProductDetail removed
// 6. Tab wrapper animates back to full visibility

// Configuration for predictive back animations:
QuoVadisHost(
    navigator = navigator,
    screenRegistry = GeneratedScreenRegistry,
    predictiveBackConfig = PredictiveBackConfig(
        // Scale the surface below during drag
        behindSurfaceScale = 0.9f..1.0f,
        // Parallax effect for surface below
        behindSurfaceParallax = 0.1f,
        // Threshold to commit back
        commitThreshold = 0.3f
    )
)
```

### 5. Shared Elements Across Tab/Detail Boundary

```kotlin
// Old: IMPOSSIBLE - different SharedTransitionLayout roots

// New: Works automatically!

@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {
    LazyColumn {
        items(products) { product ->
            // Shared element in tab
            AsyncImage(
                model = product.imageUrl,
                modifier = Modifier
                    .sharedElement(key = "image-${product.id}")
                    .clickable {
                        navigator.navigate(
                            FullscreenDestination.ProductDetail(product.id)
                        )
                    }
            )
        }
    }
}

@Screen(FullscreenDestination.ProductDetail::class)
@Composable  
fun ProductDetailScreen(destination: FullscreenDestination.ProductDetail) {
    Column {
        // Same shared element key - transition works!
        AsyncImage(
            model = loadProduct(destination.productId).imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .sharedElement(key = "image-${destination.productId}")
        )
    }
}

// Why it works:
// - QuoVadisHost uses SharedTransitionLayout at root
// - Both FeedScreen and ProductDetailScreen are rendered as siblings
// - During transition, both exist in the layout tree
// - Compose can interpolate bounds between them
```

### 6. Deep Navigation Flow Example

```kotlin
// User journey:
// 1. Home tab → Feed
// 2. Tap product → ProductDetail (covers tabs)
// 3. Tap "View Gallery" → ImageGallery
// 4. Tap "Related Products" → Another ProductDetail
// 5. Back gesture (×3) → Returns to Feed in Home tab

// All handled by single navigator!
@Screen(FullscreenDestination.ImageGallery::class)
@Composable
fun ImageGalleryScreen(
    destination: FullscreenDestination.ImageGallery,
    navigator: Navigator
) {
    val relatedProducts = loadRelatedProducts(destination.productId)
    
    Column {
        // Gallery content...
        
        LazyRow {
            items(relatedProducts) { related ->
                ProductThumbnail(
                    product = related,
                    onClick = {
                        // Push another full-screen detail
                        navigator.navigate(
                            FullscreenDestination.ProductDetail(related.id)
                        )
                    }
                )
            }
        }
    }
}

// Stack after this flow:
// TabNode("mainTabs")
//   ├── StackNode("home") - activeStackIndex = 0
//   │     └── Feed (preserved!)
//   └── StackNode("fullscreen") - pushed on top
//         ├── ProductDetail("prod-1")
//         ├── ImageGallery("prod-1")
//         └── ProductDetail("prod-2")  // Current
```

## Acceptance Criteria

- [ ] Complete before/after with nested hosts vs unified host
- [ ] Z-ordering/flattening explanation included
- [ ] Predictive back across layers documented
- [ ] Shared element transition across boundary shown
- [ ] Deep navigation flow example complete
- [ ] Common complex navigation patterns covered

## Common Pitfalls

| Pitfall | Solution |
|---------|----------|
| Expecting separate navigators | Use single Navigator for all navigation |
| Manual z-index management | QuoVadisHost handles z-ordering automatically |
| Shared elements not working | Ensure same key string and both screens render simultaneously |
| Tab state lost after full-screen | Tab state preserved in NavNode tree |

## Related Tasks

- [MIG-002: Master-Detail Pattern](./MIG-002-master-detail-example.md)
- [MIG-003: Tabbed Navigation](./MIG-003-tabbed-navigation-example.md)
- [RENDER-002B: TabNode Flattening](../phase2-renderer/RENDER-002B-tab-flattening.md)
- [RENDER-005: Predictive Back](../phase2-renderer/RENDER-005-predictive-back.md)
