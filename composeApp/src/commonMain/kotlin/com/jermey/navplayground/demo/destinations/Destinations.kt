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
 * Auth flow destinations - Demonstrates scope-aware stack navigation.
 *
 * This stack has a scopeKey automatically generated from the sealed class name ("AuthFlowDestination").
 * When navigating from within this stack to a destination outside the scope (e.g., MainTabs),
 * the navigation will go to the parent stack instead of staying inside AuthFlow.
 *
 * This demonstrates Phase 4 of Stack Scope Navigation:
 * - In-scope navigation: Login → Register stays within AuthFlow stack
 * - Out-of-scope navigation: AuthFlow → MainTabs navigates above the AuthFlow stack
 */
@Stack(name = "auth", startDestination = "Login")
sealed class AuthFlowDestination : com.jermey.quo.vadis.core.navigation.core.Destination {
    @Destination(route = "auth/login")
    data object Login : AuthFlowDestination()

    @Destination(route = "auth/register")
    data object Register : AuthFlowDestination()

    @Destination(route = "auth/forgot-password")
    data object ForgotPassword : AuthFlowDestination()
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
