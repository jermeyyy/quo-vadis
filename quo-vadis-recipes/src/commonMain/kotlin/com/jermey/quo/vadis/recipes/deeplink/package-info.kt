/**
 * # Deep Linking Recipes
 *
 * URI-based navigation and path reconstruction patterns.
 *
 * ## When to Use
 *
 * - App links / Universal links
 * - Push notification navigation
 * - External links to specific content
 * - State restoration from URLs
 *
 * ## Pattern Overview
 *
 * Deep linking allows navigation to specific screens via URIs.
 * This includes parsing incoming URIs to navigate to the correct destination
 * and reconstructing URIs from the current navigation state for sharing.
 *
 * ## Available Recipes
 *
 * - [BasicDeepLinkRecipe] - Simple URI-to-destination mapping
 * - [NestedDeepLinkRecipe] - Deep links that navigate through nested structures
 *
 * @see com.jermey.quo.vadis.annotations.Destination
 */
package com.jermey.quo.vadis.recipes.deeplink
