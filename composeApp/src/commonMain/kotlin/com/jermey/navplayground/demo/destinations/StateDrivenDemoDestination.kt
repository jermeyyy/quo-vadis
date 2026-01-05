package com.jermey.navplayground.demo.destinations

import com.jermey.navplayground.demo.destinations.StateDrivenDemoDestination.DemoTab
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.annotations.Tabs
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * # Quo Vadis Demo App - Navigation Destinations
 *
 * This file demonstrates the **annotation-based API** for defining navigation destinations
 * using the new NavNode architecture.
 *
 * ## Key Annotations Used:
 *
 * ### @Stack(name = "name", startDestinationLegacy = "ClassName")
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
 * Demonstrates a state-driven navigation API inspired by "Navigation 3" patterns,
 * with direct backstack manipulation and Compose state observation.
 *
 * This is a custom implementation built on Quo Vadis and is not the official
 * Android Navigation 3 library.
 *
 * Uses a single-tab @Tabs structure to contain a stack,
 * allowing the demo to manipulate the inner stack while rendering custom chrome
 * (the backstack editor panel).
 */
@Tabs(
    name = "stateDrivenDemo",
    initialTab = DemoTab::class,
    items = [DemoTab::class]
)
sealed class StateDrivenDemoDestination : NavDestination {

    companion object : NavDestination

    /**
     * The single tab containing the state-driven navigation stack.
     * Destinations are nested inside.
     */
    @TabItem(label = "Demo", icon = "layers")
    @Stack(name = "stateDrivenStack", startDestination = DemoTab.Home::class)
    sealed class DemoTab : StateDrivenDemoDestination() {
        /**
         * Home destination - the starting point of the demo.
         */
        @Destination(route = "state-driven/home")
        data object Home : DemoTab() {
            override fun toString(): String = "Home"
        }

        /**
         * Profile destination with a user ID parameter.
         *
         * @property userId The ID of the user to display
         */
        @Destination(route = "state-driven/profile/{userId}")
        data class Profile(val userId: String) : DemoTab() {
            override fun toString(): String = "Profile($userId)"
        }

        /**
         * Settings destination - no parameters.
         */
        @Destination(route = "state-driven/settings")
        data object Settings : DemoTab() {
            override fun toString(): String = "Settings"
        }

        /**
         * Detail destination with an item ID parameter.
         *
         * @property itemId The ID of the item to display
         */
        @Destination(route = "state-driven/detail/{itemId}")
        data class Detail(val itemId: String) : DemoTab() {
            override fun toString(): String = "Detail($itemId)"
        }

        companion object {
            /**
             * Returns a display name for the destination type (for UI).
             */
            fun getDisplayName(destination: DemoTab): String = when (destination) {
                is Home -> "Home"
                is Profile -> "Profile"
                is Settings -> "Settings"
                is Detail -> "Detail"
            }

            /**
             * Returns all available destination types for the picker.
             */
            val allTypes: List<String> = listOf("Home", "Profile", "Settings", "Detail")
        }
    }
}

