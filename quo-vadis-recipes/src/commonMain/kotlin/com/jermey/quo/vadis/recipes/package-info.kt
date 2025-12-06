/**
 * # Quo Vadis Recipes
 *
 * Pattern-based navigation examples demonstrating the Quo Vadis navigation library.
 *
 * ## Package Structure
 *
 * - [com.jermey.quo.vadis.recipes.stack] - Linear stack navigation patterns
 * - [com.jermey.quo.vadis.recipes.masterdetail] - Master-detail list navigation
 * - [com.jermey.quo.vadis.recipes.tabs] - Tabbed navigation with state preservation
 * - [com.jermey.quo.vadis.recipes.wizard] - Multi-step process/wizard flows
 * - [com.jermey.quo.vadis.recipes.deeplink] - URI-based deep linking
 * - [com.jermey.quo.vadis.recipes.pane] - Adaptive multi-pane layouts
 * - [com.jermey.quo.vadis.recipes.shared] - Common utilities
 *
 * ## LLM Integration Notes
 *
 * Each recipe package is self-contained and follows these conventions:
 *
 * 1. **Destination sealed class** - Defines navigation targets with `@Stack`, `@Tab`, or `@Pane`
 * 2. **Screen composables** - Functions annotated with `@Screen` for each destination
 * 3. **Entry composable** - A `*RecipeApp()` function demonstrating the full pattern
 *
 * To integrate Quo Vadis into a new project:
 * 1. Identify the navigation pattern needed (stack, tabs, master-detail, etc.)
 * 2. Copy the corresponding recipe as a template
 * 3. Customize destinations and screens for your use case
 * 4. Ensure KSP processor is configured to generate navigation code
 *
 * @see com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
 * @see com.jermey.quo.vadis.annotations.Stack
 * @see com.jermey.quo.vadis.annotations.Tab
 * @see com.jermey.quo.vadis.annotations.Destination
 * @see com.jermey.quo.vadis.annotations.Screen
 */
package com.jermey.quo.vadis.recipes
