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
 * ## Route Pattern Syntax
 *
 * | Pattern | Example URI | Extracted Parameters |
 * |---------|-------------|---------------------|
 * | `products` | `myapp://products` | (none) |
 * | `products/featured` | `myapp://products/featured` | (none) |
 * | `products/{productId}` | `myapp://products/123` | `productId = "123"` |
 * | `categories/{catId}/products/{prodId}` | `myapp://categories/a/products/b` | `catId = "a", prodId = "b"` |
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
 * ## Migration from Legacy API
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
 * when (val result = handler.handleDeepLink(uri)) {
 *     is DeepLinkResult.Matched -> navigator.navigate(result.destination)
 *     is DeepLinkResult.NotMatched -> handleError()
 * }
 * ```
 *
 * @see com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
 * @see com.jermey.quo.vadis.core.navigation.core.DeepLinkResult
 * @see com.jermey.quo.vadis.annotations.Destination
 */
package com.jermey.quo.vadis.recipes.deeplink
