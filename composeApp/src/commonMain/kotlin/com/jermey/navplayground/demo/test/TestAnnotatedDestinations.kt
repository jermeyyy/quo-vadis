package com.jermey.navplayground.demo.test

import com.jermey.quo.vadis.annotations.Argument
import com.jermey.quo.vadis.annotations.Graph
import com.jermey.quo.vadis.annotations.Route
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TypedDestination
import kotlinx.serialization.Serializable

/**
 * Test graph for validating KSP code generation.
 * 
 * This should generate:
 * - TestGraphRoutes object with route constants
 * - Route registration in RouteRegistry
 * - testGraphGraph() builder function
 */
@Graph("test_graph")
sealed class TestGraph : Destination {
    
    @Route("test_graph/home")
    data object Home : TestGraph()
    
    @Route("test_graph/details")
    @Argument(DetailsData::class)
    data class Details(override val data: DetailsData) : TestGraph(), TypedDestination<DetailsData>
    
    @Route("test_graph/settings")
    data object Settings : TestGraph()
}

/**
 * Data class for Details destination.
 */
@Serializable
data class DetailsData(
    val id: String,
    val title: String
)

/**
 * Marker interface for typed destinations.
 */
interface TypedDestination<T> {
    val data: T
}
