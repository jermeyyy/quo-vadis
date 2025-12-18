package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.core.navigation.core.NavDestination

/**
 * # Quo Vadis Demo App - Navigation Destinations
 * 
 * This file demonstrates the **annotation-based API** for defining navigation destinations
 * using the new NavNode architecture.
 * 
 * ## Key Annotations Used:
 * 
 * ### @Stack(name = "name", startDestination = "ClassName")
 * Marks a sealed class as a navigation stack. KSP generates:
 * - Stack builder: `build{StackName}Stack()`
 * - Navigation extensions for destinations
 * 
 * ### @Destination(route = "path")
 * Specifies the route path for a destination.
 * Route templates support parameters: `"path/{param}"`
 * 
 * ### @Argument
 * Marks constructor parameters as route arguments.
 * - Applied to constructor parameters directly
 * - Supports optional arguments with `optional = true`
 * - KSP generates typed navigation extension functions
 * 
 * ## Generated Code:
 * 
 * For each stack, KSP generates:
 * ```kotlin
 * // Stack builder function
 * fun buildAppStack(): NavNode { /* ... */ }
 * 
 * // Typed navigation extensions
 * fun Navigator.navigateToDetail(itemId: String) { /* ... */ }
 * ```
 * 
 * ## Benefits:
 * - 50-70% less boilerplate code
 * - Route template-based argument passing
 * - Type-safe navigation extensions
 * - Compile-time verification
 * - Better IDE support
 * 
 * See: ContentDefinitions.kt for @Content annotation usage
 * See: NavigationGraphs.kt for graph composition
 */

/**
 * State-Driven Navigation demo destination.
 * 
 * Demonstrates Navigation 3-style state-driven navigation API with
 * direct backstack manipulation and Compose state observation.
 */
@Stack(name = "statedriven", startDestination = "Demo")
sealed class StateDrivenDemoDestination : NavDestination {
    @Destination(route = "statedriven/demo")
    data object Demo : StateDrivenDemoDestination()
}

