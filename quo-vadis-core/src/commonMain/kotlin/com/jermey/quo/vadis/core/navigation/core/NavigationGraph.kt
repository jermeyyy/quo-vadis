package com.jermey.quo.vadis.core.navigation.core

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
 */
data class DestinationConfig(
    val destination: Destination,
    val content: @Composable (Destination, Navigator) -> Unit
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

    fun destination(
        destination: Destination,
        content: @Composable (Destination, Navigator) -> Unit
    ) {
        dests.add(DestinationConfig(destination, content))
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

    override fun entryPoints(): List<Destination> {
        return graph.destinations.map { it.destination }
    }
}
