@file:Suppress("unused", "FunctionName")

package com.jermey.quo.vadis.recipes.deeplink

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * When a deep link targets a nested screen, the NavNode tree can be
 * reconstructed to show the correct back stack:
 *
 * ```
 * Deep link: myapp://categories/electronics/products/phone-123
 *
 * Option 1 - Direct navigation (default):
 *   Stack: [ProductInCategory(electronics, phone-123)]
 *   Back → exits navigation
 *
 * Option 2 - Reconstructed stack:
 *   Stack: [CategoryList → CategoryDetail(electronics) → ProductInCategory(electronics, phone-123)]
 *   Back → CategoryDetail → CategoryList → exits
 * ```
 *
 * The reconstructed approach provides a more natural back navigation experience.
 *
 * ## LLM Integration Notes
 *
 * To implement nested deep linking:
 * 1. Define hierarchical routes that mirror navigation structure
 * 2. Use multi-parameter routes: `{parentId}/child/{childId}`
 * 3. Consider whether back navigation should go through intermediate screens
 * 4. Configure path reconstruction strategy in QuoVadisHost if needed
 *
 * @see CategoryDestination for nested route definitions
 */

// ============================================================
// KSP-GENERATED PLACEHOLDERS
// These represent what KSP generates from @Destination/@Screen annotations.
// In a real project, these are auto-generated - DO NOT write manually.
// ============================================================

/**
 * Placeholder for KSP-generated nested deep link handler.
 *
 * Demonstrates handling multi-parameter routes like:
 * - `categories/{categoryId}`
 * - `categories/{categoryId}/products/{productId}`
 *
 * ## What KSP Generates
 *
 * ```kotlin
 * object GeneratedCategoriesDeepLinkHandler : GeneratedDeepLinkHandler {
 *     private val routes = listOf(
 *         RoutePattern("categories", CategoryDestination.CategoryList::class),
 *         RoutePattern("categories/{categoryId}", CategoryDestination.CategoryDetail::class),
 *         RoutePattern("categories/{categoryId}/products/{productId}", CategoryDestination.ProductInCategory::class)
 *     )
 *
 *     override fun handleDeepLink(uri: String): DeepLinkResult {
 *         // Matches routes from most specific to least specific
 *         // Extracts all parameters and constructs destination
 *     }
 * }
 * ```
 */
private object NestedRecipeDeepLinkHandler : GeneratedDeepLinkHandler {

    override fun handleDeepLink(uri: String): DeepLinkResult {
        // Extract path from URI (simplified implementation)
        val path = uri
            .removePrefix("myapp://")
            .removePrefix("https://example.com/")
            .trimEnd('/')

        // Match routes from most specific to least specific
        return when {
            // Most specific: categories/{categoryId}/products/{productId}
            path.matches(Regex("categories/([^/]+)/products/([^/]+)")) -> {
                val segments = path.split("/")
                val categoryId = segments[1]
                val productId = segments[3]
                DeepLinkResult.Matched(
                    CategoryDestination.ProductInCategory(categoryId, productId)
                )
            }

            // Medium specific: categories/{categoryId}
            path.matches(Regex("categories/([^/]+)")) && !path.contains("/products/") -> {
                val categoryId = path.removePrefix("categories/")
                DeepLinkResult.Matched(CategoryDestination.CategoryDetail(categoryId))
            }

            // Least specific: categories
            path == "categories" -> {
                DeepLinkResult.Matched(CategoryDestination.CategoryList)
            }

            else -> DeepLinkResult.NotMatched
        }
    }

    override fun createDeepLinkUri(destination: Destination, scheme: String): String? {
        return when (destination) {
            is CategoryDestination.CategoryList ->
                "$scheme://categories"

            is CategoryDestination.CategoryDetail ->
                "$scheme://categories/${destination.categoryId}"

            is CategoryDestination.ProductInCategory ->
                "$scheme://categories/${destination.categoryId}/products/${destination.productId}"

            else -> null
        }
    }
}

/**
 * Placeholder for KSP-generated screen registry.
 *
 * Maps category destinations to their composable screen functions.
 */
private object NestedRecipeScreenRegistry : ScreenRegistry {

    override fun hasContent(destination: Destination): Boolean =
        destination is CategoryDestination

    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: androidx.compose.animation.SharedTransitionScope?,
        animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope?
    ) {
        when (destination) {
            is CategoryDestination.CategoryList -> CategoryListScreen(navigator)
            is CategoryDestination.CategoryDetail -> CategoryDetailScreen(destination, navigator)
            is CategoryDestination.ProductInCategory -> ProductInCategoryScreen(destination, navigator)
        }
    }
}

/**
 * Placeholder for KSP-generated NavNode tree builder.
 *
 * Creates the initial navigation tree for the categories stack.
 * In production, this is generated from `@Stack` and `@Destination` annotations.
 *
 * ## What KSP Generates
 *
 * ```kotlin
 * // Generated from @Stack(name = "categories", startDestination = "CategoryList")
 * fun buildCategoriesNavNode(key: String = "categories-stack"): StackNode {
 *     return StackNode(
 *         key = key,
 *         children = listOf(
 *             ScreenNode(
 *                 key = "categories-list",
 *                 destination = CategoryDestination.CategoryList
 *             )
 *         )
 *     )
 * }
 * ```
 */
// NOTE: Actual NavNode tree creation is done by KSP-generated code.
// This function signature shows the expected pattern:
// private fun buildCategoriesNavNode(): NavNode = TODO("Use KSP-generated builder")

// ============================================================
// SCREENS
// ============================================================

/**
 * Category list screen - entry point for categories navigation.
 *
 * Demonstrates:
 * - Navigation to category details
 * - Testing nested deep links
 *
 * @param navigator The navigator for navigation actions
 */
// NOTE: @Screen annotation omitted to avoid KSP processing in recipes module.
// In production code, add: @Screen(CategoryDestination.CategoryList::class)
@Composable
fun CategoryListScreen(navigator: Navigator) {
    RecipeScaffold(title = "Categories") { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Browse Categories",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Select a category or test nested deep links.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sample categories
            listOf(
                "electronics" to "Electronics",
                "clothing" to "Clothing",
                "books" to "Books"
            ).forEach { (id, name) ->
                NavigationButton(
                    text = name,
                    onClick = {
                        navigator.navigate(CategoryDestination.CategoryDetail(id))
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Deep link testing section
            Text(
                text = "Test Nested Deep Links",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "These simulate deep links to nested screens:",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Deep link to category detail
            Button(
                onClick = {
                    val uri = "myapp://categories/electronics"
                    handleNestedDeepLink(uri, navigator)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("→ categories/electronics")
            }

            // Deep link to product in category
            Button(
                onClick = {
                    val uri = "myapp://categories/electronics/products/phone-123"
                    handleNestedDeepLink(uri, navigator)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("→ categories/electronics/products/phone-123")
            }

            // Deep link to another product in category
            Button(
                onClick = {
                    val uri = "myapp://categories/books/products/novel-456"
                    handleNestedDeepLink(uri, navigator)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("→ categories/books/products/novel-456")
            }
        }
    }
}

/**
 * Category detail screen showing products in a category.
 *
 * Demonstrates:
 * - Accessing single path parameter from destination
 * - Navigating to nested product screens
 *
 * @param destination The destination containing extracted categoryId
 * @param navigator The navigator for navigation actions
 */
// NOTE: @Screen annotation omitted to avoid KSP processing in recipes module.
// In production code, add: @Screen(CategoryDestination.CategoryDetail::class)
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
            Text(
                text = "Products in ${destination.categoryId}",
                style = MaterialTheme.typography.headlineSmall
            )

            // Deep link URI for this screen
            val categoryUri = NestedRecipeDeepLinkHandler.createDeepLinkUri(
                destination,
                scheme = "myapp"
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Share this category:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = categoryUri ?: "N/A",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sample products in this category
            listOf(
                "product-1" to "Product One",
                "product-2" to "Product Two",
                "product-3" to "Product Three"
            ).forEach { (id, name) ->
                NavigationButton(
                    text = name,
                    onClick = {
                        navigator.navigate(
                            CategoryDestination.ProductInCategory(
                                categoryId = destination.categoryId,
                                productId = id
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * Product in category screen with multiple path parameters.
 *
 * Demonstrates:
 * - Accessing multiple path parameters from a single destination
 * - Context-aware navigation (product knows its category)
 * - Creating deep link URIs with multiple parameters
 *
 * @param destination The destination containing both categoryId and productId
 * @param navigator The navigator for navigation actions
 */
// NOTE: @Screen annotation omitted to avoid KSP processing in recipes module.
// In production code, add: @Screen(CategoryDestination.ProductInCategory::class)
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
            Text(
                text = "Product: ${destination.productId}",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Category: ${destination.categoryId}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Show the deep link URI that would reach this screen
            val uri = NestedRecipeDeepLinkHandler.createDeepLinkUri(
                destination,
                scheme = "myapp"
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Deep Link URI for this screen:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = uri ?: "N/A",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explain the path reconstruction concept
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Path Reconstruction",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = """
                            With path reconstruction enabled, back navigation would go:
                            
                            1. This screen (current)
                            2. ← CategoryDetail(${destination.categoryId})
                            3. ← CategoryList
                            4. ← Exit
                            
                            Without path reconstruction:
                            1. This screen (current)
                            2. ← Exit
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigate to related product in same category
            NavigationButton(
                text = "View Related Product in ${destination.categoryId}",
                onClick = {
                    navigator.navigate(
                        CategoryDestination.ProductInCategory(
                            categoryId = destination.categoryId,
                            productId = "related-item-999"
                        )
                    )
                }
            )
        }
    }
}

// ============================================================
// NESTED DEEP LINK HANDLING
// ============================================================

/**
 * Handle a nested deep link with optional path reconstruction.
 *
 * For deep links targeting nested screens, the NavNode architecture
 * can optionally reconstruct the full navigation path.
 *
 * ## Example
 *
 * URI: `myapp://categories/electronics/products/phone-123`
 *
 * ### Option 1: Direct Navigation (Default)
 *
 * ```kotlin
 * navigator.navigate(result.destination)
 * // Stack: [ProductInCategory(electronics, phone-123)]
 * // Back → exits navigation
 * ```
 *
 * ### Option 2: Path Reconstruction
 *
 * ```kotlin
 * navigator.navigateWithPathReconstruction(result.destination)
 * // Stack: [CategoryList, CategoryDetail(electronics), ProductInCategory(...)]
 * // Back → CategoryDetail → CategoryList → exits
 * ```
 *
 * ## When to Use Path Reconstruction
 *
 * - User expects to browse back through intermediate screens
 * - Deep link is from external source (app link, notification)
 * - Content makes sense to browse "upward" in hierarchy
 *
 * ## When NOT to Use Path Reconstruction
 *
 * - Quick action that should return to previous context
 * - Deep link is from within the app
 * - User initiated direct navigation
 *
 * @param uri The incoming deep link URI
 * @param navigator The navigator to use for navigation
 */
private fun handleNestedDeepLink(uri: String, navigator: Navigator) {
    when (val result = NestedRecipeDeepLinkHandler.handleDeepLink(uri)) {
        is DeepLinkResult.Matched -> {
            // Option 1: Direct navigation (simple, default)
            navigator.navigate(result.destination)

            // Option 2: Path reconstruction (if configured)
            // This would be implemented as:
            // navigator.navigateWithPathReconstruction(result.destination)
            //
            // Or via QuoVadisHost configuration:
            // QuoVadisHost(
            //     navigator = navigator,
            //     screenRegistry = registry,
            //     deepLinkPathReconstruction = true
            // )
        }

        is DeepLinkResult.NotMatched -> {
            println("Unknown nested deep link: $uri")
            // In production: show error toast, navigate to fallback
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
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     NestedDeepLinkApp()
 * }
 * ```
 */
@Composable
fun NestedDeepLinkApp() {
    // In production, use KSP-generated functions:
    // val navTree = remember { buildCategoriesNavNode() }
    // val navigator = rememberNavigator(navTree)

    // This is a conceptual example - actual implementation requires
    // KSP-generated code and proper navigator setup
    Text("NestedDeepLinkApp - See recipe source for implementation details")
}

// ============================================================
// MIGRATION & LLM NOTES
// ============================================================

/**
 * ## Multi-Parameter Route Patterns
 *
 * Routes can have multiple path parameters extracted in order:
 *
 * ```kotlin
 * @Destination(route = "categories/{categoryId}/products/{productId}")
 * data class ProductInCategory(
 *     val categoryId: String,
 *     val productId: String
 * ) : CategoryDestination()
 * ```
 *
 * The KSP generator:
 * 1. Parses the route template
 * 2. Identifies parameters by `{paramName}` syntax
 * 3. Matches parameter names to constructor arguments
 * 4. Generates parsing logic that extracts values from URI path segments
 *
 * ## Path Reconstruction Strategies
 *
 * Different strategies for handling deep links to nested screens:
 *
 * ### 1. Greedy (Default)
 *
 * Navigate directly to target screen. No intermediate screens in back stack.
 *
 * ```kotlin
 * navigator.navigate(destination)
 * ```
 *
 * ### 2. Full Stack Reconstruction
 *
 * Build the complete navigation path from root to target.
 *
 * ```kotlin
 * // Conceptual API - may vary by implementation
 * navigator.navigateWithFullPath(destination)
 * ```
 *
 * ### 3. Selective Reconstruction
 *
 * Configure which parent screens to include.
 *
 * ```kotlin
 * // In QuoVadisHost configuration
 * QuoVadisHost(
 *     navigator = navigator,
 *     screenRegistry = registry,
 *     deepLinkConfig = DeepLinkConfig(
 *         reconstructionDepth = 2,  // Include 2 parent levels
 *         includeRoot = true
 *     )
 * )
 * ```
 *
 * ## Testing Deep Links
 *
 * Use FakeNavigator to verify deep link handling without UI:
 *
 * ```kotlin
 * @Test
 * fun testNestedDeepLink_extractsMultipleParameters() {
 *     val navigator = FakeNavigator()
 *     val uri = "myapp://categories/electronics/products/phone-123"
 *
 *     handleNestedDeepLink(uri, navigator)
 *
 *     val lastNav = navigator.navigateCalls.last()
 *     val dest = lastNav.destination as CategoryDestination.ProductInCategory
 *
 *     assertEquals("electronics", dest.categoryId)
 *     assertEquals("phone-123", dest.productId)
 * }
 *
 * @Test
 * fun testNestedDeepLink_unknownPath_returnsNotMatched() {
 *     val result = handler.handleDeepLink("myapp://unknown/path")
 *     assertTrue(result is DeepLinkResult.NotMatched)
 * }
 *
 * @Test
 * fun testCreateUri_multipleParams_encodesCorrectly() {
 *     val destination = CategoryDestination.ProductInCategory(
 *         categoryId = "electronics",
 *         productId = "phone-123"
 *     )
 *     val uri = handler.createDeepLinkUri(destination, "myapp")
 *
 *     assertEquals("myapp://categories/electronics/products/phone-123", uri)
 * }
 * ```
 *
 * ## Migration from Legacy Nested Deep Links
 *
 * ### OLD PATTERN
 * ```kotlin
 * // Manual nested registration
 * handler.register("categories") { CategoryList }
 * handler.register("categories/{catId}") { params ->
 *     CategoryDetail(params["catId"]!!)
 * }
 * handler.register("categories/{catId}/products/{prodId}") { params ->
 *     ProductInCategory(params["catId"]!!, params["prodId"]!!)
 * }
 * ```
 *
 * ### NEW PATTERN
 * ```kotlin
 * // Automatic from annotations
 * @Destination(route = "categories")
 * data object CategoryList : CategoryDestination()
 *
 * @Destination(route = "categories/{categoryId}")
 * data class CategoryDetail(val categoryId: String) : CategoryDestination()
 *
 * @Destination(route = "categories/{categoryId}/products/{productId}")
 * data class ProductInCategory(
 *     val categoryId: String,
 *     val productId: String
 * ) : CategoryDestination()
 * ```
 */
private object MigrationNotesNestedDeepLink
