n
# Migration Guide: Nested Tabs + Detail

> **Difficulty**: Medium-Advanced | **Time Estimate**: 60-120 minutes | **Prerequisites**: [02-master-detail.md](./02-master-detail.md), [03-tabbed-navigation.md](./03-tabbed-navigation.md) complete

This guide demonstrates how to migrate complex navigation hierarchies where detail screens render on top of (covering) the tab bar, with proper predictive back behavior across navigation layers and shared element transitions between tab content and full-screen details.

---

## Table of Contents

1. [Overview](#overview)
2. [Before (Old API)](#before-old-api)
3. [After (New API)](#after-new-api)
4. [How the Unified Host Works](#how-the-unified-host-works)
5. [Predictive Back Across Layers](#predictive-back-across-layers)
6. [Shared Elements Across Tab/Detail Boundary](#shared-elements-across-tabdetail-boundary)
7. [Deep Navigation Flow Example](#deep-navigation-flow-example)
8. [Key Migration Steps](#key-migration-steps)
9. [Common Pitfalls](#common-pitfalls)
10. [Next Steps](#next-steps)
11. [Related Resources](#related-resources)

---

## Overview

The nested tabs + detail pattern is common in e-commerce apps, social media, and content browsers where tapping an item in a tab should show a full-screen detail that **covers the tab bar**. The old API required complex nested host structures and manual parent navigator coordination. The new NavNode architecture unifies everything into a single `QuoVadisHost` with automatic z-ordering.

### When to Use This Pattern

- Product catalog with full-screen product details
- Social media feeds with full-screen post views
- Media galleries with immersive viewer
- Any tabbed UI where drill-down should be full-screen

### Key Improvements

| Problem | Old Solution | New Solution |
|---------|--------------|--------------|
| Detail covering tabs | Nested `NavHost` at root level | Single `QuoVadisHost` with automatic z-ordering |
| Parent navigator access | Manual reference passing | Single navigator handles all layers |
| Predictive back coordination | Multiple `BackHandler`s fighting | Unified speculative pop |
| Shared elements across boundary | **Impossible** (different hosts) | Native support via single `SharedTransitionLayout` |
| State preservation | Complex manual management | Automatic via NavNode tree |

### Annotation Changes Summary

| Old Pattern | New Pattern | Purpose |
|-------------|-------------|---------|
| Root `@Graph` + Tab `@Graph` | `@Tab` + `@Stack` hierarchy | Define tab + full-screen structure |
| `parentNavigator` parameter | Single `Navigator` | Navigate from any screen |
| `@Content` with parent reference | `@Screen` with navigator only | Screen binding |
| Manual z-index management | Automatic flattening | Layered rendering |

### Host Changes Summary

| Old Component | New Component | Change |
|---------------|---------------|--------|
| Root `GraphNavHost` + `TabbedNavHost` | Single `QuoVadisHost` | Unified rendering |
| `parentNavigator.navigate()` | `navigator.navigate()` | Same navigator everywhere |
| Manual `SharedTransitionLayout` per host | Built-in shared elements | Automatic scope management |
| Separate back handlers | Unified predictive back | Automatic coordination |

---

## Before (Old API)

### The Nested Host Problem

The old API required a complex structure with multiple navigation hosts:
- **Root host** for full-screen destinations
- **Tab host** nested inside a root destination
- **Parent navigator reference** passed to child screens

### Complete Working Example

```kotlin
package com.example.shop

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.*
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════
// STEP 1: Root Level - Handles Full-Screen Destinations
// ═══════════════════════════════════════════════════════════

/**
 * Root navigation graph containing tabs AND full-screen destinations.
 * 
 * OLD: Full-screen details must be at root level to cover tabs.
 */
@Graph("app", startDestination = "app/main_tabs")
sealed class AppDestination : Destination {
    
    @Route("app/main_tabs")
    data object MainTabs : AppDestination()
    
    // Full-screen detail that covers tabs - must be at ROOT level!
    @Route("app/product_detail")
    @Argument(ProductDetailData::class)
    data class ProductDetail(val productId: String) : AppDestination(),
        TypedDestination<ProductDetailData> {
        override val data = ProductDetailData(productId)
    }
    
    @Route("app/image_gallery")
    @Argument(ImageGalleryData::class)
    data class ImageGallery(val productId: String) : AppDestination(),
        TypedDestination<ImageGalleryData> {
        override val data = ImageGalleryData(productId)
    }
}

@Serializable
data class ProductDetailData(val productId: String)

@Serializable  
data class ImageGalleryData(val productId: String)

// ═══════════════════════════════════════════════════════════
// STEP 2: Tab Destinations (Separate Graphs)
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Each tab has its own @Graph - completely separate from root.
 */
@Graph("home", startDestination = "home/feed")
sealed class HomeDestination : Destination {
    @Route("home/feed")
    data object Feed : HomeDestination()
    
    @Route("home/category/{id}")
    data class Category(val id: String) : HomeDestination()
}

@Graph("search", startDestination = "search/main")
sealed class SearchDestination : Destination {
    @Route("search/main")
    data object Main : SearchDestination()
    
    @Route("search/results/{query}")
    data class Results(val query: String) : SearchDestination()
}

@Graph("profile", startDestination = "profile/main")
sealed class ProfileDestination : Destination {
    @Route("profile/main")
    data object Main : ProfileDestination()
    
    @Route("profile/settings")
    data object Settings : ProfileDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Tab Configuration
// ═══════════════════════════════════════════════════════════

@TabGraph(name = "main_tabs", initialTab = "Home", primaryTab = "Home")
sealed class MainTabs : TabDefinition {
    
    @Tab(route = "tab_home", label = "Home", icon = "home",
         rootGraph = HomeDestination::class, rootDestination = HomeDestination.Feed::class)
    data object Home : MainTabs() {
        override val route = "tab_home"
        override val rootDestination = HomeDestination.Feed
    }
    
    @Tab(route = "tab_search", label = "Search", icon = "search",
         rootGraph = SearchDestination::class, rootDestination = SearchDestination.Main::class)
    data object Search : MainTabs() {
        override val route = "tab_search"
        override val rootDestination = SearchDestination.Main
    }
    
    @Tab(route = "tab_profile", label = "Profile", icon = "person",
         rootGraph = ProfileDestination::class, rootDestination = ProfileDestination.Main::class)
    data object Profile : MainTabs() {
        override val route = "tab_profile"
        override val rootDestination = ProfileDestination.Main
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 4: Complex Nested Host Structure
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Root app with GraphNavHost for full-screen destinations.
 */
@Composable
fun AppRoot() {
    remember { initializeQuoVadisRoutes() }
    
    val rootNavigator = rememberNavigator()
    val rootGraph = remember { appGraph() }
    
    LaunchedEffect(rootNavigator, rootGraph) {
        rootNavigator.registerGraph(rootGraph)
        rootNavigator.setStartDestination(AppDestination.MainTabs)
    }
    
    // Root level host - handles full-screen destinations
    SharedTransitionLayout {  // Separate SharedTransitionLayout!
        GraphNavHost(
            graph = rootGraph,
            navigator = rootNavigator,
            defaultTransition = NavigationTransitions.FadeThrough
        )
    }
}

/**
 * OLD: MainTabs content needs PARENT navigator reference!
 */
@Content(AppDestination.MainTabs::class)
@Composable
fun MainTabsContent(
    parentNavigator: Navigator,  // Need parent reference for full-screen nav!
    parentEntry: BackStackEntry
) {
    val homeGraph = remember { homeGraph() }
    val searchGraph = remember { searchGraph() }
    val profileGraph = remember { profileGraph() }
    
    val tabGraphs = remember {
        mapOf(
            MainTabs.Home to homeGraph,
            MainTabs.Search to searchGraph,
            MainTabs.Profile to profileGraph
        )
    }
    
    val tabState = rememberTabNavigator(
        config = MainTabsConfig,
        parentNavigator = parentNavigator,  // Pass parent for full-screen nav
        parentEntry = parentEntry
    )
    val selectedTab by tabState.selectedTab.collectAsState()
    
    // Nested TabbedNavHost - separate from root GraphNavHost!
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = tabGraphs,
        navigator = parentNavigator,
        tabUI = { tabContent ->
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        MainTabsConfig.allTabs.forEach { tab ->
                            NavigationBarItem(
                                icon = { Icon(tabIcon(tab), contentDescription = null) },
                                label = { Text(tabLabel(tab)) },
                                selected = selectedTab == tab,
                                onClick = { tabState.selectTab(tab) }
                            )
                        }
                    }
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding)) {
                    tabContent()
                }
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════
// STEP 5: Navigate to Full-Screen from Within Tab
// ═══════════════════════════════════════════════════════════

/**
 * OLD: Must use PARENT navigator to show detail over tabs!
 * Requires parentNavigator reference passed through layers.
 */
@Content(HomeDestination.Feed::class)
@Composable
fun FeedContent(
    navigator: Navigator,          // Tab's navigator - for in-tab navigation
    parentNavigator: Navigator?    // Root navigator - for full-screen navigation!
) {
    val products = remember { sampleProducts() }
    
    LazyColumn {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // MUST use PARENT navigator to cover tabs!
                    parentNavigator?.navigate(
                        AppDestination.ProductDetail(product.id),
                        NavigationTransitions.SlideHorizontal
                    ) ?: run {
                        // Fallback if no parent - shows within tab (wrong!)
                        // This breaks the UX - detail shows below tab bar
                        navigator.navigate(/* ... */)
                    }
                }
            )
        }
    }
}

/**
 * OLD: Full-screen detail at root level.
 */
@Content(AppDestination.ProductDetail::class)
@Composable
fun ProductDetailContent(
    data: ProductDetailData,
    navigator: Navigator
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Back button - no gesture support without manual BackHandler!
        IconButton(onClick = { navigator.navigateBack() }) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        
        ProductImage(productId = data.productId)
        
        Text("Product: ${data.productId}")
        
        Button(onClick = {
            navigator.navigate(
                AppDestination.ImageGallery(data.productId),
                NavigationTransitions.SlideHorizontal
            )
        }) {
            Text("View Gallery")
        }
    }
}

@Content(AppDestination.ImageGallery::class)
@Composable
fun ImageGalleryContent(data: ImageGalleryData, navigator: Navigator) {
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = { navigator.navigateBack() }) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        Text("Gallery for: ${data.productId}")
    }
}

// ═══════════════════════════════════════════════════════════
// PROBLEMS WITH OLD APPROACH
// ═══════════════════════════════════════════════════════════

// Problem 1: parentNavigator reference must be passed through ALL layers
// Problem 2: Back gesture in ProductDetail handled by root BackHandler
// Problem 3: Back gesture in Feed handled by tab BackHandler  
// Problem 4: NO coordination between back handlers - gesture can conflict!
// Problem 5: Shared elements IMPOSSIBLE between Feed and ProductDetail
//            (they're in different SharedTransitionLayout roots!)
// Problem 6: Complex error-prone wiring just to show full-screen detail

// Sample data
data class Product(val id: String, val name: String, val price: Double, val imageUrl: String)

fun sampleProducts() = listOf(
    Product("prod-001", "Wireless Headphones", 79.99, "..."),
    Product("prod-002", "Smart Watch", 199.99, "..."),
    Product("prod-003", "Portable Charger", 29.99, "...")
)
```

### Old API Characteristics

1. **Nested host hierarchy** — Root `GraphNavHost` contains `TabbedNavHost`
2. **Parent navigator passing** — Manual threading of `parentNavigator` through layers
3. **Separate graphs** — Root graph and tab graphs are completely separate
4. **`TypedDestination<T>` interface** — Separate data classes for arguments
5. **Multiple `SharedTransitionLayout`** — Shared elements don't work across boundary
6. **Separate back handlers** — Can conflict and cause issues
7. **Complex wiring** — Easy to make mistakes with navigator references

---

## After (New API)

### Unified Host Solution

The new API replaces the complex nested structure with a single `QuoVadisHost` that handles everything:

### Complete Migrated Example

```kotlin
package com.example.shop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.annotations.*
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.compose.LocalSharedTransitionScope
import com.jermey.quo.vadis.core.navigation.core.*
import com.example.shop.generated.*

// ═══════════════════════════════════════════════════════════
// STEP 1: Single Destination Hierarchy
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Tab container using @Tab annotation.
 * All destinations are part of the SAME tree!
 */
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

// ═══════════════════════════════════════════════════════════
// STEP 2: Per-Tab Stacks (Simplified)
// ═══════════════════════════════════════════════════════════

@Stack(name = "home", startDestination = "Feed")
sealed class HomeDestination : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()
    
    @Destination(route = "home/category/{id}")
    data class Category(val id: String) : HomeDestination()
}

@Stack(name = "search", startDestination = "Main")
sealed class SearchDestination : Destination {
    @Destination(route = "search/main")
    data object Main : SearchDestination()
    
    @Destination(route = "search/results/{query}")
    data class Results(val query: String) : SearchDestination()
}

@Stack(name = "profile", startDestination = "Main")
sealed class ProfileDestination : Destination {
    @Destination(route = "profile/main")
    data object Main : ProfileDestination()
    
    @Destination(route = "profile/settings")
    data object Settings : ProfileDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 3: Full-Screen Destinations - Part of the SAME Tree!
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Full-screen destinations in their own @Stack.
 * No separate root graph needed - same NavNode tree!
 */
@Stack(name = "fullscreen", startDestination = "ProductDetail")
sealed class FullscreenDestination : Destination {
    
    @Destination(route = "product/{productId}")
    data class ProductDetail(val productId: String) : FullscreenDestination()
    
    @Destination(route = "gallery/{productId}")
    data class ImageGallery(val productId: String) : FullscreenDestination()
}

// ═══════════════════════════════════════════════════════════
// STEP 4: Single QuoVadisHost Handles Everything
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Minimal setup - one host, one navigator, automatic z-ordering.
 */
@Composable
fun AppRoot() {
    // KSP generates buildMainTabsNavNode() from @Tab annotation
    val navTree = remember { buildMainTabsNavNode() }
    val navigator = rememberNavigator(navTree)
    
    // Single host handles tabs AND full-screen destinations
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        tabWrapper = { tabNode, tabContent ->
            // Tab bar is part of wrapper - detail screens cover it!
            MainTabScaffold(
                tabNode = tabNode,
                navigator = navigator,
                content = tabContent
            )
        }
        // No special handling needed for full-screen!
        // QuoVadisHost z-orders automatically
    )
}

@Composable
fun MainTabScaffold(
    tabNode: TabNode,
    navigator: Navigator,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            // Tab bar rendered here - detail screens render ABOVE this!
            NavigationBar {
                MainTabsMetadata.tabs.forEachIndexed { index, tabInfo ->
                    NavigationBarItem(
                        icon = { Icon(tabInfo.icon, contentDescription = null) },
                        label = { Text(tabInfo.label) },
                        selected = tabNode.activeStackIndex == index,
                        onClick = { navigator.switchTab(tabInfo.tab) }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════
// STEP 5: Navigate to Full-Screen from Within Tab
// ═══════════════════════════════════════════════════════════

/**
 * NEW: Single navigator - detail renders on top automatically.
 * No parentNavigator needed!
 */
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) {  // Just one navigator!
    val products = remember { sampleProducts() }
    
    LazyColumn {
        items(products, key = { it.id }) { product ->
            ProductCard(
                product = product,
                onClick = {
                    // Same navigator - detail renders on top automatically!
                    navigator.navigate(FullscreenDestination.ProductDetail(product.id))
                },
                // Shared element just works!
                modifier = Modifier.sharedElement(key = "product-${product.id}")
            )
        }
    }
}

/**
 * NEW: Full-screen detail receives destination directly.
 */
@Screen(FullscreenDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: FullscreenDestination.ProductDetail,
    navigator: Navigator
) {
    // Full screen - no padding from tab bar because we're rendered above it!
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = { navigator.navigateBack() }) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        
        // Shared element continues transition from tab!
        AsyncImage(
            model = loadProduct(destination.productId).imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .sharedElement(key = "product-${destination.productId}")
        )
        
        Text("Product: ${destination.productId}")
        
        Button(onClick = {
            navigator.navigate(FullscreenDestination.ImageGallery(destination.productId))
        }) {
            Text("View Gallery")
        }
    }
}

@Screen(FullscreenDestination.ImageGallery::class)
@Composable
fun ImageGalleryScreen(
    destination: FullscreenDestination.ImageGallery,
    navigator: Navigator
) {
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = { navigator.navigateBack() }) {
            Icon(Icons.Default.ArrowBack, "Back")
        }
        
        Text("Gallery for: ${destination.productId}")
        
        // Can navigate to more detail screens
        val relatedProducts = loadRelatedProducts(destination.productId)
        LazyRow {
            items(relatedProducts) { related ->
                ProductThumbnail(
                    product = related,
                    onClick = {
                        // Push another full-screen detail
                        navigator.navigate(FullscreenDestination.ProductDetail(related.id))
                    }
                )
            }
        }
    }
}

// Sample data (unchanged)
data class Product(val id: String, val name: String, val price: Double, val imageUrl: String)
fun sampleProducts() = listOf(...)
fun loadProduct(id: String): Product = sampleProducts().first { it.id == id }
fun loadRelatedProducts(id: String): List<Product> = sampleProducts().filter { it.id != id }
```

### New API Characteristics

1. **Single `QuoVadisHost`** — Handles tabs AND full-screen destinations
2. **Single `Navigator`** — No parent reference needed
3. **Automatic z-ordering** — Detail screens render above tab wrapper
4. **Built-in shared elements** — Work across tab/detail boundary
5. **Unified predictive back** — Automatic coordination
6. **`@Tab` + `@Stack` hierarchy** — Declarative structure definition
7. **Direct destination access** — No separate data classes

---

## How the Unified Host Works

### Z-Ordering via Flattening

The `QuoVadisHost` uses a "flattening" algorithm to render the NavNode tree with proper z-ordering:

```kotlin
// === NavNode Tree Structure ===

// Initial state (user on Home/Feed):
// TabNode("mainTabs")
//   ├── StackNode("home")
//   │     └── ScreenNode(HomeDestination.Feed)
//   ├── StackNode("search")
//   │     └── ScreenNode(SearchDestination.Main)
//   └── StackNode("profile")
//         └── ScreenNode(ProfileDestination.Main)

// After pushing ProductDetail from Feed:
// TabNode("mainTabs")
//   ├── StackNode("home")
//   │     └── ScreenNode(HomeDestination.Feed)  // Still preserved!
//   ├── StackNode("search")
//   │     └── ScreenNode(SearchDestination.Main)
//   ├── StackNode("profile")
//   │     └── ScreenNode(ProfileDestination.Main)
//   └── ScreenNode(FullscreenDestination.ProductDetail)  // Higher z-index!
```

### Flattening Output

```kotlin
// flattenState() produces render layers:

// Layer 0: TabNode wrapper (contains bottom nav)
// Layer 1: Active tab content (Feed screen, inside tab wrapper)
// Layer 2: ProductDetail (full-screen, OUTSIDE tab wrapper)

// QuoVadisHost renders as:
Box {
    // Layer 0+1: Tab wrapper with bottom nav + active tab content
    tabWrapper(tabNode) {
        // Active tab content rendered here (with bottom padding)
        FeedScreen(navigator)
    }
    
    // Layer 2: Full-screen detail (no tab wrapper!)
    // Rendered as sibling, covers tab bar
    ProductDetailScreen(destination, navigator)
}
```

### Visual Representation

```
┌─────────────────────────────────────────┐
│                                         │
│    ┌───────────────────────────────┐    │
│    │                               │    │
│    │      ProductDetail            │    │  ← z=2 (covers everything)
│    │      (full-screen)            │    │
│    │                               │    │
│    └───────────────────────────────┘    │
│                                         │
├─────────────────────────────────────────┤
│    ┌───────────────────────────────┐    │
│    │      Feed Content             │    │  ← z=1 (inside tab wrapper)
│    │      (with bottom padding)    │    │
│    └───────────────────────────────┘    │
│    ┌───────────────────────────────┐    │
│    │  🏠   🔍   👤                 │    │  ← z=0 (tab bar, hidden)
│    └───────────────────────────────┘    │
└─────────────────────────────────────────┘
```

---

## Predictive Back Across Layers

### The Problem with Old API

```kotlin
// OLD: Two separate back handlers fighting

// Root BackHandler (for ProductDetail):
BackHandler(enabled = isProductDetailVisible) {
    rootNavigator.navigateBack()
}

// Tab BackHandler (for tab content):
BackHandler(enabled = isTabContentNotAtRoot) {
    tabNavigator.navigateBack()
}

// Problems:
// 1. Which handler wins on back gesture?
// 2. No coordination between them
// 3. No predictive back animation across boundary
// 4. Gesture can show wrong "behind" content
```

### Unified Predictive Back (New API)

```kotlin
// NEW: Single QuoVadisHost manages all back gestures

// User swipes back from ProductDetail:
// 1. QuoVadisHost detects gesture on ProductDetail
// 2. Creates "speculative pop" state:
//    - ProductDetail marked as "popping"
//    - Feed (behind it) marked as "appearing"
// 3. Renders BOTH screens simultaneously during gesture:
//    - ProductDetail: scales/slides away with finger
//    - Feed (with tab bar): scales up from 0.9 → 1.0
// 4. On commit (gesture completes):
//    - NavNode tree updated
//    - ProductDetail removed
//    - Tab wrapper animates to full visibility
// 5. On cancel (gesture aborted):
//    - State reverts
//    - ProductDetail snaps back
```

### Configuration Options

```kotlin
QuoVadisHost(
    navigator = navigator,
    screenRegistry = GeneratedScreenRegistry,
    tabWrapper = { tabNode, tabContent -> /* ... */ },
    
    // Optional: Configure predictive back behavior
    predictiveBackConfig = PredictiveBackConfig(
        // Scale the surface below during drag
        behindSurfaceScale = 0.9f..1.0f,
        
        // Parallax effect for surface below
        behindSurfaceParallax = 0.1f,
        
        // Threshold to commit back (0.3 = 30% of screen)
        commitThreshold = 0.3f,
        
        // Enable edge-to-edge gesture
        edgeToEdge = true
    )
)
```

### Back Navigation Flow

```
User drags from left edge:
    │
    ├─► [0-30% progress]
    │     ProductDetail: translateX(progress * screenWidth)
    │     Feed + TabBar: scale(0.9 + 0.1 * progress), alpha(progress)
    │
    ├─► [30% threshold crossed - haptic feedback]
    │     System indicates "will go back"
    │
    └─► [Released after threshold]
          ProductDetail: animate out
          Feed + TabBar: animate to full scale
          NavNode tree: remove ProductDetail
```

---

## Shared Elements Across Tab/Detail Boundary

### Why It Works Now

```kotlin
// OLD: IMPOSSIBLE - different SharedTransitionLayout roots

// Root had its own SharedTransitionLayout:
SharedTransitionLayout {
    GraphNavHost(rootGraph, ...) {
        // ProductDetail here - can animate with other root destinations
    }
}

// TabbedNavHost had NO SharedTransitionLayout (or its own):
TabbedNavHost(...) {
    // Feed here - CANNOT animate with ProductDetail!
}

// Result: Shared elements between Feed ↔ ProductDetail don't work
```

```kotlin
// NEW: Works automatically!

// QuoVadisHost wraps EVERYTHING in single SharedTransitionLayout:
@Composable
fun QuoVadisHost(...) {
    SharedTransitionLayout {  // Single root!
        CompositionLocalProvider(
            LocalSharedTransitionScope provides this
        ) {
            // TabWrapper + Feed
            // ProductDetail
            // Everything is a sibling in the layout tree!
        }
    }
}

// Both Feed and ProductDetail are direct children
// Compose can interpolate bounds between them during transition
```

### Implementation Example

```kotlin
// In Feed (tab content):
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

// In ProductDetail (full-screen):
@Screen(FullscreenDestination.ProductDetail::class)
@Composable  
fun ProductDetailScreen(
    destination: FullscreenDestination.ProductDetail,
    navigator: Navigator
) {
    Column {
        // Same shared element key - transition works!
        AsyncImage(
            model = loadProduct(destination.productId).imageUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .sharedElement(key = "image-${destination.productId}")
        )
        
        // Rest of detail content...
    }
}
```

### Shared Element + Predictive Back

```kotlin
// During predictive back gesture from ProductDetail → Feed:

// 1. Both screens render simultaneously
// 2. Shared element (product image) interpolates:
//    - Position: Detail center → Feed list item position
//    - Size: Full width → Thumbnail size
//    - Bounds: Animated based on gesture progress
// 3. On commit: Final positions snap into place
// 4. On cancel: Reverts to detail position
```

---

## Deep Navigation Flow Example

### Complete User Journey

```kotlin
// User journey:
// 1. App starts → Home tab → Feed
// 2. Tap product → ProductDetail (covers tabs)
// 3. Tap "View Gallery" → ImageGallery
// 4. Tap "Related Product" → Another ProductDetail
// 5. Back gesture (×3) → Returns to Feed in Home tab

// === Step 1: Initial state ===
// TabNode("mainTabs", activeStackIndex=0)
//   ├── StackNode("home") → [Feed]
//   ├── StackNode("search") → [Main]
//   └── StackNode("profile") → [Main]

// === Step 2: After navigate(ProductDetail("prod-1")) ===
// TabNode("mainTabs", activeStackIndex=0)
//   ├── StackNode("home") → [Feed]  // Preserved!
//   ├── StackNode("search") → [Main]
//   ├── StackNode("profile") → [Main]
//   └── ScreenNode(ProductDetail("prod-1"))  // z-index higher

// === Step 3: After navigate(ImageGallery("prod-1")) ===
// TabNode("mainTabs", activeStackIndex=0)
//   ├── StackNode("home") → [Feed]
//   ├── StackNode("search") → [Main]
//   ├── StackNode("profile") → [Main]
//   └── StackNode("fullscreen") → [ProductDetail("prod-1"), ImageGallery("prod-1")]

// === Step 4: After navigate(ProductDetail("prod-2")) ===
// TabNode("mainTabs", activeStackIndex=0)
//   ├── StackNode("home") → [Feed]
//   ├── StackNode("search") → [Main]
//   ├── StackNode("profile") → [Main]
//   └── StackNode("fullscreen") → [ProductDetail("prod-1"), ImageGallery("prod-1"), ProductDetail("prod-2")]

// === Step 5: After 3 back presses ===
// Back 1: Remove ProductDetail("prod-2")
// Back 2: Remove ImageGallery("prod-1")
// Back 3: Remove ProductDetail("prod-1")
// Result: Back at Feed in Home tab!
```

### Code Example

```kotlin
@Screen(FullscreenDestination.ImageGallery::class)
@Composable
fun ImageGalleryScreen(
    destination: FullscreenDestination.ImageGallery,
    navigator: Navigator
) {
    val relatedProducts = remember(destination.productId) {
        loadRelatedProducts(destination.productId)
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Gallery header with back
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navigator.navigateBack() }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
            Text("Gallery", style = MaterialTheme.typography.titleLarge)
        }
        
        // Gallery content...
        
        // Related products - can navigate to more detail screens
        Text("Related Products", style = MaterialTheme.typography.titleMedium)
        LazyRow {
            items(relatedProducts) { related ->
                ProductThumbnail(
                    product = related,
                    modifier = Modifier.sharedElement(key = "thumb-${related.id}"),
                    onClick = {
                        // Push another full-screen detail!
                        navigator.navigate(
                            FullscreenDestination.ProductDetail(related.id)
                        )
                    }
                )
            }
        }
    }
}
```

---

## Key Migration Steps

### Step 1: Flatten the Graph Hierarchy

```diff
- // OLD: Separate root and tab graphs
- @Graph("app", startDestination = "app/main_tabs")
- sealed class AppDestination : Destination {
-     @Route("app/main_tabs") data object MainTabs : AppDestination()
-     @Route("app/product_detail") @Argument(...)
-     data class ProductDetail(...) : AppDestination(), TypedDestination<...>
- }
- 
- @Graph("home", startDestination = "home/feed")
- sealed class HomeDestination : Destination { ... }

+ // NEW: Single hierarchy with @Tab and @Stack
+ @Tab(name = "mainTabs", initialTab = "Home")
+ sealed class MainTabs : Destination {
+     @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
+     @Destination(route = "tabs/home")
+     data object Home : MainTabs()
+     // ... other tabs
+ }
+ 
+ @Stack(name = "home", startDestination = "Feed")
+ sealed class HomeDestination : Destination { ... }
+ 
+ @Stack(name = "fullscreen", startDestination = "ProductDetail")
+ sealed class FullscreenDestination : Destination {
+     @Destination(route = "product/{productId}")
+     data class ProductDetail(val productId: String) : FullscreenDestination()
+ }
```

### Step 2: Remove Parent Navigator References

```diff
  @Content(HomeDestination.Feed::class)
  @Composable
  fun FeedContent(
      navigator: Navigator,
-     parentNavigator: Navigator?  // Remove!
  ) {
      // ...
-     parentNavigator?.navigate(
-         AppDestination.ProductDetail(product.id)
-     )
+     navigator.navigate(
+         FullscreenDestination.ProductDetail(product.id)
+     )
  }
```

### Step 3: Replace Nested Hosts with Single QuoVadisHost

```diff
  @Composable
  fun AppRoot() {
-     val rootNavigator = rememberNavigator()
-     val rootGraph = remember { appGraph() }
-     
-     SharedTransitionLayout {
-         GraphNavHost(graph = rootGraph, navigator = rootNavigator, ...) 
-     }
+     val navTree = remember { buildMainTabsNavNode() }
+     val navigator = rememberNavigator(navTree)
+     
+     QuoVadisHost(
+         navigator = navigator,
+         screenRegistry = GeneratedScreenRegistry,
+         tabWrapper = { tabNode, tabContent ->
+             MainTabScaffold(tabNode, navigator, tabContent)
+         }
+     )
  }
```

### Step 4: Update Screen Annotations

```diff
- @Content(AppDestination.ProductDetail::class)
+ @Screen(FullscreenDestination.ProductDetail::class)
  @Composable
- fun ProductDetailContent(
-     data: ProductDetailData,
+ fun ProductDetailScreen(
+     destination: FullscreenDestination.ProductDetail,
      navigator: Navigator
  ) {
-     val productId = data.productId
+     val productId = destination.productId
      // ...
  }
```

### Step 5: Simplify Shared Elements

```diff
  @Composable
  fun ProductCard(
      product: Product,
-     sharedTransitionScope: SharedTransitionScope,  // Remove!
      onClick: () -> Unit
  ) {
-     with(sharedTransitionScope) {
-         Card(
-             modifier = Modifier.sharedBounds(
-                 sharedContentState = rememberSharedContentState(key = "..."),
-                 animatedVisibilityScope = // complex scope management
-             )
-         )
-     }
+     Card(
+         modifier = Modifier
+             .sharedBounds(key = "card-${product.id}")
+             .clickable(onClick = onClick)
+     ) {
+         AsyncImage(
+             modifier = Modifier.sharedElement(key = "image-${product.id}")
+         )
+     }
  }
```

### Step 6: Remove Manual Back Handler Coordination

```diff
  @Composable
  fun AppRoot() {
-     // Manual back handler for full-screen
-     BackHandler(enabled = isFullScreenVisible) {
-         rootNavigator.navigateBack()
-     }
      
+     // QuoVadisHost handles all back navigation automatically!
      QuoVadisHost(...)
  }
```

### Step 7: Build and Verify

```bash
./gradlew :app:assembleDebug
```

Test these scenarios:
1. Navigate from tab to full-screen detail
2. Verify tab bar is covered
3. Predictive back gesture from detail to tab
4. Shared element animation across boundary
5. Deep navigation (detail → gallery → detail)
6. Multiple back presses return to original tab

---

## Common Pitfalls

| Pitfall | Symptom | Solution |
|---------|---------|----------|
| **Expecting separate navigators** | Trying to access `parentNavigator` | Use single `Navigator` for all navigation |
| **Manual z-index management** | Detail not covering tabs | Remove manual z-indexing — `QuoVadisHost` handles automatically |
| **Shared elements not working** | No animation between tab and detail | Ensure same key string; verify both screens use `sharedElement()` modifier |
| **Tab state lost after full-screen** | Tab resets to root on back | Tab state preserved in NavNode tree automatically |
| **Multiple back handlers** | Conflicting back behavior | Remove manual `BackHandler` — unified handling in host |
| **Full-screen in wrong stack** | Detail appears inside tab (with tab bar) | Ensure full-screen destinations are in separate `@Stack` |
| **Wrong destination hierarchy** | Full-screen destinations as tab children | Full-screen should be sibling to tabs, not child |
| **SharedTransitionLayout wrapper** | Double-wrapping issues | Remove manual wrapper — built into `QuoVadisHost` |
| **Predictive back not working** | No gesture animation | Verify Android 14+ or custom gesture handling enabled |

### Debugging Tips

1. **Log NavNode tree**: Print tree structure to verify hierarchy
2. **Check z-ordering**: Add colored backgrounds to see layer order
3. **Verify shared element keys**: Log keys in both source and destination
4. **Test predictive back**: Slow down animations to see full transition
5. **Inspect generated code**: Review KSP output for correct structure

---

## Next Steps

After migrating nested tabs + detail:

- **[04-process-flow.md](./04-process-flow.md)** — Migrate wizard/process flows with results
- **[06-deep-linking.md](./06-deep-linking.md)** — Configure deep linking for complex hierarchies

---

## Related Resources

- [02-master-detail.md](./02-master-detail.md) — Prerequisite: Master-detail pattern
- [03-tabbed-navigation.md](./03-tabbed-navigation.md) — Prerequisite: Tabbed navigation
- [API Change Summary](./api-change-summary.md) — Complete annotation and API reference
- [Phase 1: NavNode Architecture](../refactoring-plan/phase1-core/CORE-001-navnode-hierarchy.md) — NavNode type definitions
- [Phase 2: TabNode Flattening](../refactoring-plan/phase2-renderer/RENDER-002B-tab-flattening.md) — Z-ordering algorithm
- [Phase 2: QuoVadisHost](../refactoring-plan/phase2-renderer/RENDER-004-quovadis-host.md) — Unified renderer details
- [Phase 2: Predictive Back](../refactoring-plan/phase2-renderer/RENDER-005-predictive-back.md) — Predictive back implementation
- [MIG-005 Spec](../refactoring-plan/phase5-migration/MIG-005-nested-tabs-detail-example.md) — Original task specification

```
