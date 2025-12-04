package com.jermey.navplayground.demo.destinations

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Graph
import com.jermey.quo.vadis.annotations.Route
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.RouteRegistry
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable

/**
 * # Quo Vadis Demo App - Navigation Destinations
 * 
 * This file demonstrates the **annotation-based API** for defining navigation destinations.
 * 
 * ## Key Annotations Used:
 * 
 * ### @Graph("name")
 * Marks a sealed class as a navigation graph. KSP generates:
 * - Route initializer: `{GraphName}RouteInitializer`
 * - Graph builder: `build{GraphName}Graph()`
 * 
 * ### @Route("path")
 * Specifies the route path for a destination.
 * Routes are automatically registered at initialization.
 * 
 * ### @Argument(DataClass::class)
 * Defines typed, serializable arguments for a destination.
 * - Data class must be annotated with @Serializable
 * - Destination must implement TypedDestination<T>
 * - KSP generates typed navigation extension functions
 * 
 * ## Generated Code:
 * 
 * For each graph, KSP generates:
 * ```kotlin
 * // Route registration (automatic)
 * object MainDestinationRouteInitializer {
 *     init {
 *         MainDestination.Home.registerRoute("main/home")
 *         // ... other routes
 *     }
 * }
 * 
 * // Graph builder function
 * fun buildMainDestinationGraph(): NavigationGraph { /* ... */ }
 * 
 * // Typed navigation extensions
 * fun Navigator.navigateToDetail(itemId: String) { /* ... */ }
 * ```
 * 
 * ## Benefits:
 * - 50-70% less boilerplate code
 * - Automatic serialization/deserialization
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
sealed class DemoDestination : Destination {
    @Route("demo_root")
    object Root : DemoDestination()
}

/**
 * Top-level app destinations.
 * 
 * Contains only the main entry point of the application.
 * This is the root graph registered with the parent navigator.
 */
@Graph("app", startDestination = "MainTabs")
sealed class AppDestination : Destination {
    @Route("app/main_tabs")
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
@Graph("tabs_content", startDestination = "Home")
sealed class TabDestination : Destination {
    @Route("tab/home")
    data object Home : TabDestination()
    
    @Route("tab/explore")
    data object Explore : TabDestination()
    
    @Route("tab/profile")
    data object Profile : TabDestination()
    
    @Route("tab/settings")
    data object Settings : TabDestination()
}

/**
 * Deep link demo destination.
 * 
 * Accessible from anywhere via modal bottom sheet.
 * Allows navigation to all linked screens for testing deep links.
 */
@Graph("deeplink", startDestination = "Demo")
sealed class DeepLinkDestination : Destination {
    @Route("deeplink/demo")
    data object Demo : DeepLinkDestination()
}

/**
 * State-Driven Navigation demo destination.
 * 
 * Demonstrates Navigation 3-style state-driven navigation API with
 * direct backstack manipulation and Compose state observation.
 */
@Graph("statedriven", startDestination = "Demo")
sealed class StateDrivenDemoDestination : Destination {
    @Route("statedriven/demo")
    data object Demo : StateDrivenDemoDestination()
}

/**
 * Master-Detail pattern destinations.
 * 
 * ANNOTATION PATTERN: Typed Destinations (With Arguments)
 * 
 * The Detail destination demonstrates:
 * - @Argument annotation with @Serializable data class
 * - Implementation of TypedDestination<T> interface
 * - Type-safe argument passing
 * 
 * Generated code includes:
 * - Typed extension: navigator.navigateToDetail(itemId = "123")
 * - Automatic serialization/deserialization
 * - Content function receives DetailData directly (see ContentDefinitions.kt)
 */
@Graph("master_detail")
sealed class MasterDetailDestination : Destination {
    @Route("master_detail/list")
    data object List : MasterDetailDestination()

    /**
     * Detail destination with typed argument.
     * 
     * KSP generates:
     * ```kotlin
     * fun Navigator.navigateToDetail(
     *     itemId: String,
     *     transition: NavigationTransition? = null
     * )
     * ```
     */
    @Route("master_detail/detail")
    @Argument(DetailData::class)
    data class Detail(val itemId: String) : MasterDetailDestination(), TypedDestination<DetailData> {
        override val data = DetailData(itemId)
    }
}

/**
 * Serializable data for Detail destination.
 */
@Serializable
data class DetailData(val itemId: String)

/**
 * Tabs navigation destinations
 */
@Graph("tabs")
sealed class TabsDestination : Destination {
    @Route("tabs/main")
    data object Main : TabsDestination()

    /**
     * Serializable data for SubItem destination.
     */
    @Serializable
    data class SubItemData(val tabId: String, val itemId: String)

    // Dynamic route - register manually at creation
    data class Tab(val tabId: String) : TabsDestination() {
        init {
            RouteRegistry.register(this::class, "tabs_tab_$tabId")
        }
        override val data = tabId
    }

    @Route("tabs/subitem")
    @Argument(SubItemData::class)
    data class SubItem(val tabId: String, val itemId: String) : TabsDestination(), TypedDestination<SubItemData> {
        override val data = SubItemData(tabId, itemId)
    }
}

/**
 * Process/Wizard flow destinations
 */
@Graph("process")
sealed class ProcessDestination : Destination {
    @Route("process/start")
    data object Start : ProcessDestination()

    /**
     * Serializable data for Step1 destination.
     */
    @Serializable
    data class Step1Data(val userType: String? = null)

    /**
     * Serializable data for Step2A/Step2B destinations.
     */
    @Serializable
    data class Step2Data(val stepData: String)

    /**
     * Serializable data for Step3 destination.
     */
    @Serializable
    data class Step3Data(val previousData: String, val branch: String)

    @Route("process/step1")
    @Argument(Step1Data::class)
    data class Step1(val userType: String? = null) : ProcessDestination(), TypedDestination<Step1Data> {
        override val data = Step1Data(userType)
    }

    @Route("process/step2a")
    @Argument(Step2Data::class)
    data class Step2A(val stepData: String) : ProcessDestination(), TypedDestination<Step2Data> {
        override val data = Step2Data(stepData)
    }

    @Route("process/step2b")
    @Argument(Step2Data::class)
    data class Step2B(val stepData: String) : ProcessDestination(), TypedDestination<Step2Data> {
        override val data = Step2Data(stepData)
    }

    @Route("process/step3")
    @Argument(Step3Data::class)
    data class Step3(val previousData: String, val branch: String) : ProcessDestination(), TypedDestination<Step3Data> {
        override val data = Step3Data(previousData, branch)
    }

    @Route("process/complete")
    data object Complete : ProcessDestination()
}
