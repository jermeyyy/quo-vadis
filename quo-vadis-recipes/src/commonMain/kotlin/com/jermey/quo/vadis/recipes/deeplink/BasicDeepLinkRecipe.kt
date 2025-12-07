@file:Suppress("unused", "FunctionName")

package com.jermey.quo.vadis.recipes.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenRegistry
import com.jermey.quo.vadis.recipes.shared.NavigationButton
import com.jermey.quo.vadis.recipes.shared.RecipeScaffold

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
 * - [GeneratedDeepLinkHandler] - KSP-generated URI parser (placeholder in this recipe)
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
 * ## LLM Integration Notes
 *
 * To implement deep linking in a project:
 * 1. Add `@Destination(route = "...")` to destination classes
 * 2. Run KSP to generate `GeneratedDeepLinkHandler`
 * 3. Call `handleDeepLink(uri)` when receiving external URIs
 * 4. Handle both `Matched` and `NotMatched` results
 *
 * @see ProductsDestination for route definitions
 * @see com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
 */

// ============================================================
// KSP-GENERATED PLACEHOLDERS
// These represent what KSP generates from @Destination/@Screen annotations.
// In a real project, these are auto-generated - DO NOT write manually.
// ============================================================

/**
 * Placeholder for KSP-generated deep link handler.
 *
 * In production, KSP generates this from `@Destination(route = "...")` annotations.
 * This placeholder demonstrates the expected API and behavior.
 *
 * ## What KSP Generates
 *
 * ```kotlin
 * // Generated from:
 * // @Destination(route = "products")
 * // @Destination(route = "products/featured")
 * // @Destination(route = "products/{productId}")
 *
 * object GeneratedProductsDeepLinkHandler : GeneratedDeepLinkHandler {
 *     private val routes = listOf(
 *         RoutePattern("products", ProductsDestination.List::class),
 *         RoutePattern("products/featured", ProductsDestination.Featured::class),
 *         RoutePattern("products/{productId}", ProductsDestination.Detail::class)
 *     )
 *
 *     override fun handleDeepLink(uri: String): DeepLinkResult { ... }
 *     override fun createDeepLinkUri(destination: Destination, scheme: String): String? { ... }
 * }
 * ```
 */
private object RecipeDeepLinkHandler : GeneratedDeepLinkHandler {

    override fun handleDeepLink(uri: String): DeepLinkResult {
        // Extract path from URI (simplified implementation)
        val path = uri
            .removePrefix("myapp://")
            .removePrefix("https://example.com/")
            .trimEnd('/')

        return when {
            // Static route: products
            path == "products" -> DeepLinkResult.Matched(ProductsDestination.List)

            // Static route: products/featured
            path == "products/featured" -> DeepLinkResult.Matched(ProductsDestination.Featured)

            // Parameterized route: products/{productId}
            path.startsWith("products/") && path.substringAfter("products/").none { it == '/' } -> {
                val productId = path.removePrefix("products/")
                DeepLinkResult.Matched(ProductsDestination.Detail(productId))
            }

            else -> DeepLinkResult.NotMatched
        }
    }

    override fun createDeepLinkUri(destination: Destination, scheme: String): String? {
        return when (destination) {
            is ProductsDestination.List -> "$scheme://products"
            is ProductsDestination.Featured -> "$scheme://products/featured"
            is ProductsDestination.Detail -> "$scheme://products/${destination.productId}"
            else -> null
        }
    }
}

/**
 * Placeholder for KSP-generated screen registry.
 *
 * Maps destinations to their composable screen functions.
 * In production, this is generated from `@Screen` annotations.
 */
private object RecipeScreenRegistry : ScreenRegistry {

    override fun hasContent(destination: Destination): Boolean =
        destination is ProductsDestination

    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: androidx.compose.animation.SharedTransitionScope?,
        animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?
    ) {
        when (destination) {
            is ProductsDestination.List -> ProductListScreen(navigator)
            is ProductsDestination.Featured -> FeaturedProductsScreen(navigator)
            is ProductsDestination.Detail -> ProductDetailScreen(destination, navigator)
        }
    }
}

/**
 * Placeholder for KSP-generated NavNode tree builder.
 *
 * Creates the initial navigation tree for the products stack.
 * In production, this is generated from `@Stack` and `@Destination` annotations.
 *
 * ## What KSP Generates
 *
 * ```kotlin
 * // Generated from @Stack(name = "products", startDestination = "List")
 * fun buildProductsNavNode(key: String = "products-stack"): StackNode {
 *     return StackNode(
 *         key = key,
 *         children = listOf(
 *             ScreenNode(
 *                 key = "products-list",
 *                 destination = ProductsDestination.List
 *             )
 *         )
 *     )
 * }
 * ```
 */
// NOTE: Actual NavNode tree creation is done by KSP-generated code.
// This function signature shows the expected pattern:
// private fun buildProductsNavNode(): NavNode = TODO("Use KSP-generated builder")

// ============================================================
// SCREENS
// ============================================================

/**
 * Product list screen - entry point for products navigation.
 *
 * Demonstrates:
 * - Navigation to other screens via [Navigator.navigate]
 * - Simulating incoming deep links
 * - Handling deep link results
 *
 * ## Screen Binding (Applied in Production)
 *
 * In production code, this function would be annotated with:
 * ```kotlin
 * @Screen(ProductsDestination.List::class)
 * @Composable
 * fun ProductListScreen(navigator: Navigator) { ... }
 * ```
 *
 * @param navigator The navigator for navigation actions
 */
// NOTE: @Screen annotation omitted to avoid KSP processing in recipes module.
// In production code, add: @Screen(ProductsDestination.List::class)
@Composable
fun ProductListScreen(navigator: Navigator) {
    RecipeScaffold(title = "Products") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Welcome to Products",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Navigate programmatically or test deep links below.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Programmatic navigation examples
            NavigationButton(
                text = "View Featured",
                onClick = { navigator.navigate(ProductsDestination.Featured) }
            )

            NavigationButton(
                text = "View Product ABC-123",
                onClick = { navigator.navigate(ProductsDestination.Detail("abc-123")) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Deep link simulation section
            Text(
                text = "Test Deep Linking",
                style = MaterialTheme.typography.titleMedium
            )

            var deepLinkUri by remember { mutableStateOf("myapp://products/test-123") }

            OutlinedTextField(
                value = deepLinkUri,
                onValueChange = { deepLinkUri = it },
                label = { Text("Enter Deep Link URI") },
                placeholder = { Text("myapp://products/xyz") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    handleBasicDeepLink(deepLinkUri, navigator)
                },
                enabled = deepLinkUri.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Handle Deep Link")
            }

            // Show quick test URIs
            Text(
                text = "Quick test URIs:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            listOf(
                "myapp://products",
                "myapp://products/featured",
                "myapp://products/phone-456"
            ).forEach { testUri ->
                Button(
                    onClick = { deepLinkUri = testUri },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(testUri, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/**
 * Featured products screen.
 *
 * Demonstrates:
 * - Accessing deep link URI for sharing
 * - Back navigation
 *
 * @param navigator The navigator for navigation actions
 */
// NOTE: @Screen annotation omitted to avoid KSP processing in recipes module.
// In production code, add: @Screen(ProductsDestination.Featured::class)
@Composable
fun FeaturedProductsScreen(navigator: Navigator) {
    RecipeScaffold(
        title = "Featured Products",
        showBackButton = true,
        onBackClick = { navigator.navigateBack() }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(
                text = "Featured products would be displayed here.",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show how to create a deep link URI for this screen
            val uri = RecipeDeepLinkHandler.createDeepLinkUri(
                ProductsDestination.Featured,
                scheme = "myapp"
            )

            Text(
                text = "Share this screen:",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = uri ?: "N/A",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Product detail screen with extracted product ID.
 *
 * Demonstrates:
 * - Accessing path parameters from the destination
 * - Creating deep link URIs with parameters
 *
 * @param destination The destination containing extracted productId
 * @param navigator The navigator for navigation actions
 */
// NOTE: @Screen annotation omitted to avoid KSP processing in recipes module.
// In production code, add: @Screen(ProductsDestination.Detail::class)
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
            Text(
                text = "Product ID: ${destination.productId}",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "(This screen was reached via deep link or navigation)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show how to create a deep link URI for this specific product
            val uri = RecipeDeepLinkHandler.createDeepLinkUri(
                destination,
                scheme = "myapp"
            )

            Text(
                text = "Deep link for this product:",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = uri ?: "N/A",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Navigate to another product
            NavigationButton(
                text = "View Related Product XYZ-999",
                onClick = { navigator.navigate(ProductsDestination.Detail("xyz-999")) }
            )
        }
    }
}

// ============================================================
// DEEP LINK HANDLING
// ============================================================

/**
 * Handle an incoming deep link URI.
 *
 * This function demonstrates the standard pattern for processing deep links:
 * 1. Parse URI with `GeneratedDeepLinkHandler.handleDeepLink()`
 * 2. Check result type (`Matched`/`NotMatched`)
 * 3. Navigate to matched destination or handle error
 *
 * ## Usage
 *
 * ```kotlin
 * // In your platform-specific entry point (Activity, UIApplicationDelegate, etc.)
 * fun onDeepLink(uri: String) {
 *     handleBasicDeepLink(uri, navigator)
 * }
 * ```
 *
 * ## Error Handling
 *
 * When [DeepLinkResult.NotMatched] is returned, you should:
 * - Log the unrecognized URI for debugging
 * - Show an error message to the user
 * - Optionally navigate to a fallback screen (e.g., home)
 *
 * @param uri The incoming deep link URI (e.g., "myapp://products/123")
 * @param navigator The navigator to use for navigation
 */
private fun handleBasicDeepLink(uri: String, navigator: Navigator) {
    // In production, use the KSP-generated handler:
    // when (val result = GeneratedDeepLinkHandler.handleDeepLink(uri)) { ... }

    when (val result = RecipeDeepLinkHandler.handleDeepLink(uri)) {
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
 * lateinit var globalNavigator: Navigator
 *
 * @Composable
 * fun App() {
 *     val navigator = rememberNavigator(buildProductsNavNode())
 *     globalNavigator = navigator
 *
 *     QuoVadisHost(navigator = navigator, screenRegistry = GeneratedScreenRegistry)
 * }
 *
 * fun onDeepLink(uri: String) {
 *     handleBasicDeepLink(uri, globalNavigator)
 * }
 * ```
 */
@Composable
fun BasicDeepLinkApp() {
    // In production, use KSP-generated functions:
    // val navTree = remember { buildProductsNavNode() }
    // val navigator = rememberNavigator(navTree)

    // This is a conceptual example - actual implementation requires
    // KSP-generated code and proper navigator setup
    Text("BasicDeepLinkApp - See recipe source for implementation details")
}

// ============================================================
// MIGRATION NOTES FOR LLM AGENTS
// ============================================================

/**
 * ## Migration from Legacy Deep Linking
 *
 * ### OLD PATTERN (Legacy)
 * ```kotlin
 * // Manual registration at runtime
 * val handler = DefaultDeepLinkHandler()
 * handler.register("products/{id}") { params ->
 *     ProductDetail(params["id"]!!)
 * }
 *
 * // Handling incoming URI
 * val deepLink = DeepLink.parse(uri)
 * navigator.handleDeepLink(deepLink)
 * ```
 *
 * ### NEW PATTERN (NavNode)
 * ```kotlin
 * // Automatic from annotations - no runtime registration
 * @Destination(route = "products/{productId}")
 * data class Detail(val productId: String) : ProductsDestination()
 *
 * // Handling incoming URI
 * when (val result = GeneratedDeepLinkHandler.handleDeepLink(uri)) {
 *     is DeepLinkResult.Matched -> navigator.navigate(result.destination)
 *     is DeepLinkResult.NotMatched -> showErrorOrFallback()
 * }
 * ```
 *
 * ### Key Changes
 *
 * 1. **Route Registration**
 *    - OLD: Runtime registration via `handler.register(pattern) { ... }`
 *    - NEW: Compile-time via `@Destination(route = "pattern")`
 *
 * 2. **Parameter Handling**
 *    - OLD: Manual extraction from `Map<String, String>`
 *    - NEW: Automatic injection into destination constructor
 *
 * 3. **Result Type**
 *    - OLD: `DeepLink?` (nullable)
 *    - NEW: `DeepLinkResult` sealed class (type-safe)
 *
 * 4. **URI Creation**
 *    - OLD: Manual string building
 *    - NEW: `handler.createDeepLinkUri(destination, scheme)`
 *
 * 5. **Type Safety**
 *    - OLD: String-based, runtime errors possible
 *    - NEW: Type-safe destinations with compile-time validation
 */
private object MigrationNotesBasicDeepLink
