package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable

/**
 * Represents a navigation graph for a feature module.
 * Supports modularization by allowing each module to expose navigation entry points
 * while hiding internal navigation details (gray box pattern).
 */
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
 * Configuration for a destination including its composable content.
 *
 * Supports two content signatures:
 * - Legacy: `content(destination, navigator)` - standard navigation
 * - Scoped: `contentWithScopes(destination, navigator, sharedTransitionScope, animatedVisibilityScope)` - with shared element support
 *
 * The scoped variant is preferred when both are provided.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
data class DestinationConfig(
    val destination: Destination,
    val content: @Composable (Destination, Navigator) -> Unit,
    val defaultTransition: NavigationTransition? = null,
    val contentWithScopes: @Composable ((Destination, Navigator, SharedTransitionScope?, AnimatedVisibilityScope?) -> Unit)? = null
)

/**
 * Builder for creating navigation graphs with DSL.
 */
class NavigationGraphBuilder(private val graphRoute: String) {
    private var startDest: Destination? = null
    private val dests = mutableListOf<DestinationConfig>()

    fun startDestination(destination: Destination) {
        startDest = destination
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
        dests.add(DestinationConfig(destination, content, transition, contentWithScopes = null))
    }

    /**
     * Register a destination with scope-aware content that receives SharedTransitionScope and AnimatedVisibilityScope.
     * Preferred over the basic signature when using shared element transitions.
     *
     * Usage:
     * ```kotlin
     * destinationWithScopes(
     *     destination = DetailsDestination,
     *     transition = NavigationTransitions.SlideHorizontal
     * ) { dest, nav, sharedScope, animScope ->
     *     DetailsScreen(
     *         destination = dest,
     *         navigator = nav,
     *         sharedTransitionScope = sharedScope,
     *         animatedVisibilityScope = animScope
     *     )
     * }
     * ```
     */
    @OptIn(ExperimentalSharedTransitionApi::class)
    fun destinationWithScopes(
        destination: Destination,
        transition: NavigationTransition? = null,
        content: @Composable (Destination, Navigator, SharedTransitionScope?, AnimatedVisibilityScope?) -> Unit
    ) {
        dests.add(
            DestinationConfig(
                destination = destination,
                content = { dest, nav -> content(dest, nav, null, null) }, // Fallback when scopes unavailable
                defaultTransition = transition,
                contentWithScopes = content
            )
        )
    }

    /**
     * Include destinations from another graph.
     * Useful for nested navigation structures.
     */
    fun include(graph: NavigationGraph) {
        dests.addAll(graph.destinations)
    }

    fun build(): NavigationGraph {
        requireNotNull(startDest) { "Start destination must be set" }

        return object : NavigationGraph {
            override val graphRoute: String = this@NavigationGraphBuilder.graphRoute
            override val startDestination: Destination = startDest!!
            override val destinations: List<DestinationConfig> = dests
        }
    }
}

/**
 * DSL function to create navigation graphs.
 */
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
abstract class BaseModuleNavigation : ModuleNavigation {
    private val graph by lazy { buildGraph() }

    protected abstract fun buildGraph(): NavigationGraph

    override fun provideGraph(): NavigationGraph = graph

    override fun entryPoints(): List<Destination> =
        graph.destinations.map(DestinationConfig::destination)
}
