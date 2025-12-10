@file:Suppress("unused")

package com.jermey.quo.vadis.recipes.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.TabWrapper
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
import com.jermey.quo.vadis.core.navigation.core.Navigator

// =============================================================================
// MIG-005: NESTED TABS + DETAIL (FULLSCREEN-OVER-TABS) RECIPE
// =============================================================================

/**
 * # Nested Tabs + Detail (Fullscreen-Over-Tabs) Navigation Recipe
 *
 * Demonstrates the **fullscreen-over-tabs** pattern where detail screens
 * render **OVER** the tab bar, covering the entire screen. This is achieved
 * using a **single unified [QuoVadisHost]** that manages both tabs AND
 * fullscreen content through z-ordering.
 *
 * ## The Pattern
 *
 * ```
 * ┌──────────────────────────┐
 * │   Tab Content Area       │  ← Regular tab screens
 * │                          │
 * │   (Home, Search, etc.)   │
 * │                          │
 * ├──────────────────────────┤
 * │ [Home] [Search] [Profile]│  ← Bottom navigation bar
 * └──────────────────────────┘
 *
 *            │
 *            │  Navigate to ProductDetail
 *            ▼
 *
 * ┌──────────────────────────┐
 * │   Fullscreen Detail      │  ← Covers ENTIRE screen
 * │                          │
 * │   (ProductDetail,        │     including tab bar
 * │    ImageGallery, etc.)   │
 * │                          │
 * │   [Back button]          │
 * └──────────────────────────┘
 * ```
 *
 * ## Why Unified QuoVadisHost Matters
 *
 * Traditional navigation libraries solve this with **nested NavHosts**:
 * - Root NavHost for fullscreen destinations
 * - Child NavHost inside tabs for tab content
 * - Problem: Requires parent navigator passing, complex state coordination
 *
 * **Quo Vadis Omni-Render Solution:**
 * - **Single QuoVadisHost** handles ALL navigation
 * - Z-ordering automatically layers fullscreen OVER tabs
 * - Single navigator instance - no parent/child complexity
 * - Shared element transitions work seamlessly across the boundary
 *
 * ## Architecture Overview
 *
 * ```
 * @Stack("app") AppDestination (root)
 * ├── @Tab ShopTabs (rendered with tabWrapper)
 * │   ├── @TabItem Home → @Stack ShopHomeDestination
 * │   │   └── Feed (start) - shows products
 * │   ├── @TabItem Search → @Stack ShopSearchDestination
 * │   │   └── SearchMain (start) - search interface
 * │   └── @TabItem Profile → @Stack ShopProfileDestination
 * │       └── ProfileMain (start) - user profile
 * │
 * └── @Stack FullscreenDestination (z-ordered ABOVE tabs)
 *     ├── ProductDetail(productId) - full-screen product view
 *     └── ImageGallery(productId) - full-screen image viewer
 * ```
 *
 * ## Key Concepts
 *
 * ### 1. Single Navigator, Single Host
 *
 * ```kotlin
 * @Composable
 * fun ShopApp() {
 *     val navTree = remember { buildAppDestinationNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     // ONE host renders everything
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         tabWrapper = shopTabsWrapper()
 *     )
 * }
 * ```
 *
 * ### 2. Navigate from Tab to Fullscreen
 *
 * From ANY tab screen, navigate to fullscreen destinations directly:
 *
 * ```kotlin
 * @Screen(ShopHomeDestination.Feed::class)
 * @Composable
 * fun FeedScreen(navigator: Navigator) {
 *     // Navigate to fullscreen - NO parent navigator needed!
 *     Button(onClick = {
 *         navigator.navigate(FullscreenDestination.ProductDetail("product-123"))
 *     }) {
 *         Text("View Product")
 *     }
 * }
 * ```
 *
 * ### 3. Z-Ordering (Automatic)
 *
 * The [QuoVadisHost] flattening algorithm assigns z-indices:
 * - Tab content and wrapper: z-index = 0
 * - Fullscreen destinations: z-index = 1 (covers tabs)
 *
 * ### 4. Back Navigation
 *
 * Back from fullscreen returns to the exact tab state:
 * - If user was on Home tab → returns to Home tab
 * - If user was on Search with results → returns to Search with results
 *
 * ## Production Setup
 *
 * ```kotlin
 * @Composable
 * fun ShopApp() {
 *     // KSP generates the navigation tree from annotations
 *     val navTree = remember { buildAppDestinationNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         tabWrapper = shopTabsWrapper(),
 *         // Predictive back works across tab ↔ fullscreen boundary
 *         enablePredictiveBack = true
 *     )
 * }
 * ```
 *
 * ## Comparison with Nested NavHost Approach
 *
 * | Aspect | Nested NavHosts | Unified QuoVadisHost |
 * |--------|-----------------|----------------------|
 * | Navigator instances | 2+ (root + per-tab) | 1 (single) |
 * | Navigate to fullscreen | Need parent reference | Direct `navigator.navigate()` |
 * | Shared element transitions | Complex/impossible | Native support |
 * | Predictive back | Requires coordination | Automatic |
 * | State preservation | Manual | Automatic |
 * | Deep linking | Complex routing | Single tree resolution |
 *
 * @see ShopTabs Tab container definition
 * @see FullscreenDestination Fullscreen destinations
 * @see AppDestination Root navigation structure
 * @see com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
 */

// =============================================================================
// ROOT NAVIGATION STRUCTURE
// =============================================================================

/**
 * Root navigation container for the shop application.
 *
 * This `@Stack` serves as the root of the navigation tree, containing:
 * 1. [ShopTabs] - The tabbed navigation (rendered with tabWrapper)
 * 2. [FullscreenDestination] - Fullscreen screens that cover tabs
 *
 * ## How Fullscreen-Over-Tabs Works
 *
 * When the navigation tree contains both TabNode and ScreenNode at the
 * same stack level, [QuoVadisHost] renders them with different z-indices:
 * - TabNode (with wrapper): z-index = 0
 * - Additional ScreenNodes: z-index = 1, 2, 3... (stacked above)
 *
 * This means navigating to [FullscreenDestination.ProductDetail] doesn't
 * "leave" the tabs - it renders OVER them with higher z-index.
 *
 * ## Generated Code (KSP)
 *
 * ```kotlin
 * fun buildAppDestinationNavNode(): StackNode = StackNode(
 *     key = "app",
 *     children = listOf(
 *         buildShopTabsNavNode()  // TabNode with all tabs
 *         // Additional children added dynamically when navigating
 *     )
 * )
 * ```
 *
 * @see ShopTabs
 * @see FullscreenDestination
 */
@Stack(name = "app", startDestination = "ShopTabs")
sealed class AppDestination : DestinationInterface {

    /**
     * Entry point to the tabbed section of the app.
     *
     * This destination wraps [ShopTabs] and is the initial screen.
     * It's rendered with the `tabWrapper` provided to [QuoVadisHost].
     */
    @Destination(route = "app/tabs")
    data object ShopTabs : AppDestination()

    /**
     * Marker for fullscreen destinations that overlay tabs.
     *
     * All [FullscreenDestination] subclasses are rendered at this level,
     * appearing ABOVE the tab wrapper in z-order.
     */
    @Destination(route = "app/fullscreen/{route}")
    data class Fullscreen(val route: String) : AppDestination()
}

// =============================================================================
// TAB CONTAINER
// =============================================================================

/**
 * Shop tabs container with Home, Search, and Profile tabs.
 *
 * Each tab has its own independent navigation stack, allowing users to:
 * - Navigate within tabs (e.g., Home → filters)
 * - Switch tabs while preserving stack state
 * - Navigate to fullscreen destinations from any tab
 *
 * ## Deep Linking
 *
 * Each tab supports direct deep links:
 * - `myapp://shop/home` → Home tab
 * - `myapp://shop/search` → Search tab
 * - `myapp://shop/profile` → Profile tab
 *
 * @see ShopHomeDestination
 * @see ShopSearchDestination
 * @see ShopProfileDestination
 */
@Tabs(name = "shopTabs", initialTab = "Home")
sealed class ShopTabs : DestinationInterface {

    /**
     * Home tab showing product feed.
     *
     * Users can browse products and tap to view fullscreen details.
     */
    @TabItem(label = "Home", icon = "home", rootGraph = ShopHomeDestination::class)
    @Destination(route = "shop/home")
    data object Home : ShopTabs()

    /**
     * Search tab for product discovery.
     *
     * Users can search and filter products.
     */
    @TabItem(label = "Search", icon = "search", rootGraph = ShopSearchDestination::class)
    @Destination(route = "shop/search")
    data object Search : ShopTabs()

    /**
     * Profile tab for user account.
     *
     * Shows orders, settings, and account info.
     */
    @TabItem(label = "Profile", icon = "person", rootGraph = ShopProfileDestination::class)
    @Destination(route = "shop/profile")
    data object Profile : ShopTabs()
}

// =============================================================================
// PER-TAB NAVIGATION STACKS
// =============================================================================

/**
 * Home tab navigation stack.
 *
 * ## Navigation Flow
 *
 * ```
 * Feed (start) → [tap product] → FullscreenDestination.ProductDetail
 *                                    ↓
 *                              [back] returns to Feed
 * ```
 *
 * Note: ProductDetail is NOT in this stack - it's in [FullscreenDestination].
 * This demonstrates the fullscreen-over-tabs pattern.
 *
 * @see ShopTabs.Home
 * @see FullscreenDestination.ProductDetail
 */
@Stack(name = "shopHome", startDestination = "Feed")
sealed class ShopHomeDestination : DestinationInterface {

    /**
     * Product feed screen - start destination for Home tab.
     *
     * Shows a scrollable list of products. Tapping a product navigates
     * to [FullscreenDestination.ProductDetail] (fullscreen, over tabs).
     */
    @Destination(route = "shop/home/feed")
    data object Feed : ShopHomeDestination()
}

/**
 * Search tab navigation stack.
 *
 * ## Navigation Flow
 *
 * ```
 * SearchMain (start) → [tap result] → FullscreenDestination.ProductDetail
 *                                          ↓
 *                                    [back] returns to SearchMain
 * ```
 *
 * @see ShopTabs.Search
 * @see FullscreenDestination.ProductDetail
 */
@Stack(name = "shopSearch", startDestination = "SearchMain")
sealed class ShopSearchDestination : DestinationInterface {

    /**
     * Search main screen - start destination for Search tab.
     *
     * Shows search input and results. Tapping a result navigates
     * to fullscreen product detail.
     */
    @Destination(route = "shop/search/main")
    data object SearchMain : ShopSearchDestination()
}

/**
 * Profile tab navigation stack.
 *
 * ## Navigation Flow
 *
 * ```
 * ProfileMain (start) → [tap order item] → FullscreenDestination.ProductDetail
 *                                               ↓
 *                                         [back] returns to ProfileMain
 * ```
 *
 * @see ShopTabs.Profile
 * @see FullscreenDestination.ProductDetail
 */
@Stack(name = "shopProfile", startDestination = "ProfileMain")
sealed class ShopProfileDestination : DestinationInterface {

    /**
     * Profile main screen - start destination for Profile tab.
     *
     * Shows user info, orders, and settings. Tapping an order item
     * can navigate to fullscreen product detail.
     */
    @Destination(route = "shop/profile/main")
    data object ProfileMain : ShopProfileDestination()
}

// =============================================================================
// FULLSCREEN DESTINATIONS (OVER TABS)
// =============================================================================

/**
 * Fullscreen destinations that render OVER the tab bar.
 *
 * ## Z-Ordering Behavior
 *
 * When navigated to from any tab, these destinations appear fullscreen,
 * completely covering the tab bar. The [QuoVadisHost] achieves this through
 * its flattening algorithm which assigns higher z-indices to nodes added
 * after the TabNode.
 *
 * ## Navigation Pattern
 *
 * From ANY screen (tab or fullscreen), navigate directly:
 *
 * ```kotlin
 * // From tab content
 * navigator.navigate(FullscreenDestination.ProductDetail("product-123"))
 *
 * // From another fullscreen
 * navigator.navigate(FullscreenDestination.ImageGallery("product-123"))
 * ```
 *
 * ## Deep Linking
 *
 * Fullscreen destinations support deep links:
 * - `myapp://product/abc123` → ProductDetail
 * - `myapp://product/abc123/gallery` → ImageGallery
 *
 * ## Back Navigation
 *
 * ```kotlin
 * // Returns to previous screen (tab content or another fullscreen)
 * navigator.navigateBack()
 * ```
 *
 * @see AppDestination Root stack containing these destinations
 */
@Stack(name = "fullscreen", startDestination = "ProductDetail")
sealed class FullscreenDestination : DestinationInterface {

    /**
     * Full-screen product detail view.
     *
     * Covers the entire screen including tab bar. Shows:
     * - Product images (tappable for gallery)
     * - Product info, price, description
     * - Add to cart button
     * - Back navigation button
     *
     * ## Route Parameters
     *
     * - `productId` - Unique product identifier
     *
     * ## Deep Link
     *
     * Route: `myapp://product/{productId}`
     *
     * @property productId Unique identifier for the product
     */
    @Destination(route = "product/{productId}")
    data class ProductDetail(val productId: String) : FullscreenDestination()

    /**
     * Full-screen image gallery viewer.
     *
     * Immersive image viewing experience. Features:
     * - Swipeable image carousel
     * - Pinch-to-zoom
     * - Close button returns to ProductDetail
     *
     * ## Route Parameters
     *
     * - `productId` - Product whose images to display
     *
     * ## Deep Link
     *
     * Route: `myapp://product/{productId}/gallery`
     *
     * @property productId Product identifier for fetching images
     */
    @Destination(route = "product/{productId}/gallery")
    data class ImageGallery(val productId: String) : FullscreenDestination()
}

// =============================================================================
// TAB WRAPPER
// =============================================================================

/**
 * Tab wrapper for ShopTabs with bottom navigation.
 *
 * ## @TabWrapper Pattern
 *
 * 1. Render the scaffold structure (TopAppBar, BottomBar)
 * 2. Place `content()` in the appropriate location
 * 3. Handle tab switching via `scope.switchTab(index)`
 *
 * ## Important: Fullscreen Content
 *
 * The tabWrapper is ONLY used when rendering TabNode content.
 * Fullscreen destinations ([FullscreenDestination]) are rendered
 * WITHOUT the wrapper - directly in the QuoVadisHost's root Box.
 *
 * This is how fullscreen-over-tabs works:
 * - TabNode renders: wrapper + content (z-index 0)
 * - FullscreenDestination renders: just content (z-index 1+)
 *
 * @param scope The TabWrapperScope providing access to tab state and navigation
 * @param content The content slot where active tab content is rendered
 */
@TabWrapper(ShopTabs::class)
@Composable
fun ShopTabsWrapper(
    scope: TabWrapperScope,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabMetadata.forEachIndexed { index, meta ->
                    NavigationBarItem(
                        icon = { Text(getShopTabIcon(index)) },
                        label = { Text(meta.label) },
                        selected = scope.activeTabIndex == index,
                        onClick = { scope.switchTab(index) },
                        enabled = !scope.isTransitioning
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            content()
        }
    }
}

/**
 * Returns emoji icon for shop tab index.
 *
 * @param index Tab index (0-based)
 * @return Emoji representing the tab
 */
private fun getShopTabIcon(index: Int): String = when (index) {
    0 -> "🏠"  // Home
    1 -> "🔍"  // Search
    2 -> "👤"  // Profile
    else -> "•"
}

// =============================================================================
// TAB SCREEN COMPOSABLES
// =============================================================================

/**
 * Home feed screen showing product listings.
 *
 * ## Fullscreen Navigation
 *
 * Tapping a product navigates to [FullscreenDestination.ProductDetail]:
 *
 * ```kotlin
 * navigator.navigate(FullscreenDestination.ProductDetail(product.id))
 * ```
 *
 * **Key Insight**: We use the same `navigator` instance for ALL navigation.
 * No "parent navigator" or "root navigator" concept needed.
 *
 * @param navigator Single navigator instance for all navigation
 */
@Screen(ShopHomeDestination.Feed::class)
@Composable
fun ShopFeedScreen(navigator: Navigator) {
    // Sample products
    val products = listOf(
        "product-001" to "Wireless Headphones",
        "product-002" to "Smart Watch",
        "product-003" to "Portable Speaker",
        "product-004" to "Fitness Tracker",
        "product-005" to "Bluetooth Earbuds"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "🏠 Product Feed",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Tap a product to view fullscreen details (covers tab bar)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { (productId, productName) ->
                ProductCard(
                    productId = productId,
                    productName = productName,
                    onClick = {
                        // Navigate to FULLSCREEN product detail
                        // This covers the tab bar automatically
                        navigator.navigate(FullscreenDestination.ProductDetail(productId))
                    }
                )
            }
        }
    }
}

/**
 * Search main screen with product search.
 *
 * @param navigator Single navigator instance for all navigation
 */
@Screen(ShopSearchDestination.SearchMain::class)
@Composable
fun ShopSearchMainScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "🔍 Search Products",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Search results navigate to fullscreen product details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Sample search results
        val searchResults = listOf(
            "search-001" to "Found: Gaming Mouse",
            "search-002" to "Found: Mechanical Keyboard",
            "search-003" to "Found: Monitor Stand"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(searchResults) { (productId, productName) ->
                ProductCard(
                    productId = productId,
                    productName = productName,
                    onClick = {
                        navigator.navigate(FullscreenDestination.ProductDetail(productId))
                    }
                )
            }
        }
    }
}

/**
 * Profile main screen with user info and order history.
 *
 * @param navigator Single navigator instance for all navigation
 */
@Screen(ShopProfileDestination.ProfileMain::class)
@Composable
fun ShopProfileMainScreen(navigator: Navigator) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "👤 My Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Tap order items to view fullscreen product details",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Recent Orders",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Sample order history
        val orderHistory = listOf(
            "order-item-001" to "Ordered: USB-C Hub",
            "order-item-002" to "Ordered: Webcam HD",
            "order-item-003" to "Ordered: Desk Lamp"
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orderHistory) { (productId, productName) ->
                ProductCard(
                    productId = productId,
                    productName = productName,
                    onClick = {
                        navigator.navigate(FullscreenDestination.ProductDetail(productId))
                    }
                )
            }
        }
    }
}

// =============================================================================
// FULLSCREEN SCREEN COMPOSABLES
// =============================================================================

/**
 * Fullscreen product detail screen.
 *
 * ## Key Characteristics
 *
 * 1. **Covers Tab Bar** - Renders over entire screen (z-index > 0)
 * 2. **Custom Back Navigation** - TopAppBar with back button
 * 3. **Same Navigator** - Uses same navigator instance as tab screens
 *
 * ## Navigation Actions
 *
 * ```kotlin
 * // Go back (to tab or previous fullscreen)
 * navigator.navigateBack()
 *
 * // Navigate to gallery (another fullscreen)
 * navigator.navigate(FullscreenDestination.ImageGallery(productId))
 * ```
 *
 * @param destination Destination with product ID
 * @param navigator Single navigator instance for all navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(FullscreenDestination.ProductDetail::class)
@Composable
fun ProductDetailScreen(
    destination: FullscreenDestination.ProductDetail,
    navigator: Navigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
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
            // Product image placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷 Product Image",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Text(
                text = "Product ID: ${destination.productId}",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "This is a fullscreen detail view that covers the tab bar. " +
                    "Notice how the bottom navigation is NOT visible.",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "💡 Single Navigator Pattern: This screen uses the exact same " +
                    "navigator instance as the tab screens. No 'parent navigator' needed!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Navigate to another fullscreen destination
            androidx.compose.material3.Button(
                onClick = {
                    navigator.navigate(FullscreenDestination.ImageGallery(destination.productId))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🖼️ View Image Gallery")
            }

            // Back to tabs
            androidx.compose.material3.OutlinedButton(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("← Back to Tabs")
            }
        }
    }
}

/**
 * Fullscreen image gallery viewer.
 *
 * ## Fullscreen Stack Navigation
 *
 * This demonstrates navigation between fullscreen destinations:
 * - ProductDetail → ImageGallery (both fullscreen, over tabs)
 * - Back from ImageGallery → ProductDetail (still fullscreen)
 * - Back from ProductDetail → Original tab screen
 *
 * @param destination Destination with product ID
 * @param navigator Single navigator instance for all navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(FullscreenDestination.ImageGallery::class)
@Composable
fun ImageGalleryScreen(
    destination: FullscreenDestination.ImageGallery,
    navigator: Navigator
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Gallery") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Text("✕", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gallery image placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🖼️",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Image Gallery for ${destination.productId}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Image thumbnails row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                repeat(4) { index ->
                    Card(
                        modifier = Modifier.weight(1f).padding(4.dp).height(60.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}")
                        }
                    }
                }
            }

            Text(
                text = "This is another fullscreen view. Back returns to ProductDetail, " +
                    "not directly to tabs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            androidx.compose.material3.Button(
                onClick = { navigator.navigateBack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("← Back to Product Detail")
            }
        }
    }
}

// =============================================================================
// SHARED COMPONENTS
// =============================================================================

/**
 * Reusable product card component.
 *
 * @param productId Product identifier
 * @param productName Display name for the product
 * @param onClick Callback when card is tapped
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(
    productId: String,
    productName: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📦",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = productId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =============================================================================
// APP ENTRY POINT (Documentation)
// =============================================================================

/**
 * Entry point for the Nested Tabs + Detail recipe.
 *
 * ## Production Usage
 *
 * ```kotlin
 * @Composable
 * fun ShopApp() {
 *     // KSP-generated navigation tree
 *     val navTree = remember { buildAppDestinationNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         tabWrapper = shopTabsWrapper(),
 *         enablePredictiveBack = true  // Works across tab ↔ fullscreen
 *     )
 * }
 * ```
 *
 * ## Navigation Summary
 *
 * | Action | Code | Result |
 * |--------|------|--------|
 * | Tab → Fullscreen | `navigator.navigate(FullscreenDestination.ProductDetail("id"))` | Shows fullscreen over tabs |
 * | Fullscreen → Fullscreen | `navigator.navigate(FullscreenDestination.ImageGallery("id"))` | Pushes another fullscreen |
 * | Back from Fullscreen | `navigator.navigateBack()` | Returns to previous (tab or fullscreen) |
 * | Switch Tabs | `navigator.switchTab(index)` | Changes active tab |
 *
 * ## Key Benefits
 *
 * 1. **Single Navigator** - No parent/child navigator complexity
 * 2. **Automatic Z-Ordering** - Fullscreen destinations cover tabs
 * 3. **State Preservation** - Tab state preserved when going fullscreen
 * 4. **Shared Elements** - Work seamlessly across tab ↔ fullscreen
 * 5. **Predictive Back** - Native gesture support everywhere
 * 6. **Deep Linking** - Single tree handles all routes
 */
@Composable
fun NestedTabsWithDetailApp() {
    // Placeholder - in production, use:
    //   val navTree = remember { buildAppDestinationNavNode() }
    //   val navigator = rememberNavigator(navTree)
    //   QuoVadisHost(navigator, GeneratedScreenRegistry, shopTabsWrapper())

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nested Tabs + Detail Recipe",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Fullscreen-over-tabs pattern",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "See KDoc for production implementation",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
