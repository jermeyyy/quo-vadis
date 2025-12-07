package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.compose.TransitionScope

/**
 * Represents a navigation graph for a feature module.
 * Supports modularization by allowing each module to expose navigation entry points
 * while hiding internal navigation details (gray box pattern).
 */
@Deprecated(
    message = "NavigationGraph has been replaced by NavNode tree structure. Use @Stack, @Tab, or @Pane annotations with KSP-generated builders. See: https://github.com/jermeyyy/quo-vadis/blob/main/docs/migration-examples/",
    level = DeprecationLevel.WARNING
)
interface NavigationGraph {
    /**
     * Unique identifier for this graph/module.
     */
    val graphRoute: String

    /**
     * Start destination for this graph.
     */
    val startDestination: Destination

    /**
     * All destinations exposed by this graph.
     * Used for deep linking and navigation resolution.
     */
    val destinations: List<DestinationConfig>

    /**
     * Optional parent graph for nested navigation.
     */
    val parentGraph: NavigationGraph? get() = null
}

/**
 * Configuration for a destination in the navigation graph.
 *
 * Supports two content signatures:
 * - Legacy: `content(destination, navigator)` - standard navigation
 * - Scoped: `contentWithTransitionScope(destination, navigator, transitionScope)` - with shared element support
 *
 * The scoped variant is preferred when both are provided.
 */
@Deprecated(
    message = "DestinationConfig is replaced by KSP-generated ScreenRegistry. Use @Screen annotation to bind composables to destinations.",
    level = DeprecationLevel.WARNING
)
@OptIn(ExperimentalSharedTransitionApi::class)
data class DestinationConfig(
    val destination: Destination,
    val content: @Composable (Destination, Navigator) -> Unit,
    val defaultTransition: NavigationTransition? = null,
    val contentWithTransitionScope: @Composable ((Destination, Navigator, TransitionScope?) -> Unit)? = null
)

/**
 * Builder for creating navigation graphs with DSL.
 */
@Deprecated(
    message = "NavigationGraphBuilder is replaced by @Stack/@Tab/@Pane annotations and KSP-generated NavNode builders.",
    level = DeprecationLevel.WARNING
)
class NavigationGraphBuilder(private val graphRoute: String) {
    private var startDestination: Destination? = null
    private val destinationConfigs = mutableListOf<DestinationConfig>()

    fun startDestination(destination: Destination) {
        startDestination = destination
    }

    /**
     * Register a destination with optional default transition.
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun destination(
        destination: Destination,
        transition: NavigationTransition? = null,
        content: @Composable (Destination, Navigator) -> Unit
    ) {
        println("DEBUG: destination - Adding destination with route: ${destination.route}")
        destinationConfigs.add(
            DestinationConfig(
                destination = destination,
                content = content,
                defaultTransition = transition,
                contentWithTransitionScope = null
            )
        )
        println("DEBUG: destination - Total destinations now: ${destinationConfigs.size}")
    }

    /**
     * Register a destination with TransitionScope-aware content.
     * Preferred over the basic signature when using shared element transitions.
     *
     * Usage:
     * ```kotlin
     * destinationWithScopes(
     *     destination = DetailsDestination,
     *     transition = NavigationTransitions.SlideHorizontal
     * ) { dest, nav, transitionScope ->
     *     DetailsScreen(
     *         destination = dest,
     *         navigator = nav,
     *         transitionScope = transitionScope
     *     )
     * }
     * ```
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun destinationWithScopes(
        destination: Destination,
        transition: NavigationTransition? = null,
        content: @Composable (Destination, Navigator, TransitionScope?) -> Unit
    ) {
        println("DEBUG: destinationWithScopes - Adding destination with route: ${destination.route}")
        destinationConfigs.add(
            DestinationConfig(
                destination = destination,
                content = { dest, nav ->
                    content(dest, nav, null)
                }, // Fallback when scopes unavailable
                defaultTransition = transition,
                contentWithTransitionScope = content
            )
        )
        println("DEBUG: destinationWithScopes - Total destinations now: ${destinationConfigs.size}")
    }

    /**
     * Include destinations from another graph.
     * Useful for nested navigation structures.
     */
    fun include(graph: NavigationGraph) {
        println("DEBUG: include - Including graph '${graph.graphRoute}' with ${graph.destinations.size} destinations")
        graph.destinations.forEach { config ->
            println("DEBUG: include - Including destination: ${config.destination.route}")
        }
        destinationConfigs.addAll(graph.destinations)
        println("DEBUG: include - Total destinations now: ${destinationConfigs.size}")
    }

    fun build(): NavigationGraph {
        requireNotNull(startDestination) { "Start destination must be set" }

        return object : NavigationGraph {
            override val graphRoute: String = this@NavigationGraphBuilder.graphRoute
            override val startDestination: Destination = this@NavigationGraphBuilder.startDestination!!
            override val destinations: List<DestinationConfig> = destinationConfigs
        }
    }
}

/**
 * DSL function to create navigation graphs.
 */
@Deprecated(
    message = "navigationGraph() DSL is replaced by @Stack annotation. Define a sealed class with @Stack and @Destination annotations.",
    level = DeprecationLevel.WARNING
)
fun navigationGraph(
    graphRoute: String,
    block: NavigationGraphBuilder.() -> Unit
): NavigationGraph {
    return NavigationGraphBuilder(graphRoute).apply(block).build()
}

/**
 * Module navigation interface - allows modules to expose their navigation graph
 * without exposing internal implementation details.
 */
@Deprecated(
    message = "ModuleNavigation is replaced by direct NavNode composition.",
    level = DeprecationLevel.WARNING
)
interface ModuleNavigation {
    /**
     * The navigation graph for this module.
     */
    fun provideGraph(): NavigationGraph

    /**
     * Entry points that other modules can use to navigate into this module.
     */
    fun entryPoints(): List<Destination>
}

/**
 * Base class for module navigation to simplify implementation.
 */
@Deprecated(
    message = "BaseModuleNavigation is replaced by direct NavNode composition.",
    level = DeprecationLevel.WARNING
)
abstract class BaseModuleNavigation : ModuleNavigation {
    private val graph by lazy { buildGraph() }

    protected abstract fun buildGraph(): NavigationGraph

    override fun provideGraph(): NavigationGraph = graph

    override fun entryPoints(): List<Destination> =
        graph.destinations.map(DestinationConfig::destination)
}
