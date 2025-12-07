package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Stack

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
 * Top-level destinations exposed by the demo application.
 */
sealed class DemoDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "demo_root")
    data object Root : DemoDestination()
}

/**
 * Top-level app destinations.
 * 
 * Contains only the main entry point of the application.
 * This is the root stack registered with the parent navigator.
 */
@Stack(name = "app", startDestination = "MainTabs")
sealed class AppDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "app/main_tabs")
    data object MainTabs : AppDestination()
}

/**
 * Tab content destinations - used as root destinations for each tab.
 * 
 * These are the 4 main tabs in the bottom navigation:
 * - Home: Showcase of navigation patterns
 * - Explore: Master-detail and deep navigation
 * - Profile: Profile management flows
 * - Settings: App configuration
 * 
 * These destinations are used by tab navigators, not the parent navigator.
 */
@Stack(name = "tabs_content", startDestination = "Home")
sealed class TabDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "tab/home")
    data object Home : TabDestination()
    
    @Destination(route = "tab/explore")
    data object Explore : TabDestination()
    
    @Destination(route = "tab/profile")
    data object Profile : TabDestination()
    
    @Destination(route = "tab/settings")
    data object Settings : TabDestination()
}

/**
 * Deep link demo destination.
 * 
 * Accessible from anywhere via modal bottom sheet.
 * Allows navigation to all linked screens for testing deep links.
 */
@Stack(name = "deeplink", startDestination = "Demo")
sealed class DeepLinkDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "deeplink/demo")
    data object Demo : DeepLinkDestination()
}

/**
 * State-Driven Navigation demo destination.
 * 
 * Demonstrates Navigation 3-style state-driven navigation API with
 * direct backstack manipulation and Compose state observation.
 */
@Stack(name = "statedriven", startDestination = "Demo")
sealed class StateDrivenDemoDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "statedriven/demo")
    data object Demo : StateDrivenDemoDestination()
}

/**
 * Master-Detail pattern destinations.
 * 
 * ANNOTATION PATTERN: Destinations with Route Arguments
 * 
 * The Detail destination demonstrates:
 * - @Argument annotation on constructor parameters
 * - Route template with parameter placeholders
 * - Type-safe argument passing
 * 
 * Generated code includes:
 * - Typed extension: navigator.navigateToDetail(itemId = "123")
 * - Automatic route parameter extraction
 * - Content function receives arguments directly (see ContentDefinitions.kt)
 */
@Stack(name = "master_detail", startDestination = "List")
sealed class MasterDetailDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "master_detail/list")
    data object List : MasterDetailDestination()

    /**
     * Detail destination with route argument.
     * 
     * KSP generates:
     * ```kotlin
     * fun Navigator.navigateToDetail(
     *     itemId: String,
     *     transition: NavigationTransition? = null
     * )
     * ```
     */
    @Destination(route = "master_detail/detail/{itemId}")
    data class Detail(
        @Argument val itemId: String
    ) : MasterDetailDestination()
}

/**
 * Tabs navigation destinations
 */
@Stack(name = "tabs", startDestination = "Main")
sealed class TabsDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "tabs/main")
    data object Main : TabsDestination()

    /**
     * Dynamic tab destination.
     * Tab routes are handled dynamically based on tabId.
     */
    data class Tab(val tabId: String) : TabsDestination()

    @Destination(route = "tabs/subitem/{tabId}/{itemId}")
    data class SubItem(
        @Argument val tabId: String,
        @Argument val itemId: String
    ) : TabsDestination()
}

/**
 * Process/Wizard flow destinations
 */
@Stack(name = "process", startDestination = "Start")
sealed class ProcessDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "process/start")
    data object Start : ProcessDestination()

    @Destination(route = "process/step1")
    data class Step1(
        @Argument(optional = true) val userType: String? = null
    ) : ProcessDestination()

    @Destination(route = "process/step2a/{stepData}")
    data class Step2A(
        @Argument val stepData: String
    ) : ProcessDestination()

    @Destination(route = "process/step2b/{stepData}")
    data class Step2B(
        @Argument val stepData: String
    ) : ProcessDestination()

    @Destination(route = "process/step3/{previousData}/{branch}")
    data class Step3(
        @Argument val previousData: String,
        @Argument val branch: String
    ) : ProcessDestination()

    @Destination(route = "process/complete")
    data object Complete : ProcessDestination()
}
