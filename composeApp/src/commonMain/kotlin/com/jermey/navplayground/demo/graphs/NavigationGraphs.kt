package com.jermey.navplayground.demo.graphs

// Import auto-generated graph builders
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.destinations.buildMainDestinationGraph
import com.jermey.navplayground.demo.destinations.buildMasterDetailDestinationGraph
import com.jermey.navplayground.demo.destinations.buildProcessDestinationGraph
import com.jermey.navplayground.demo.destinations.buildTabsDestinationGraph
import com.jermey.quo.vadis.core.navigation.core.navigationGraph

/**
 * Root application navigation graph.
 *
 * This graph now uses auto-generated graph builders from KSP!
 * All destination content is wired via @Content annotations in ContentDefinitions.kt.
 *
 * Benefits:
 * - No manual destination registration needed
 * - Type-safe argument passing handled automatically  
 * - Content functions matched to destinations at compile time
 * - Much less boilerplate code!
 */
fun appRootGraph() = navigationGraph("app_root") {
    startDestination(MainDestination.Home)
    
    // Include all auto-generated graphs
    include(buildMainDestinationGraph())
    include(buildMasterDetailDestinationGraph())
    include(buildTabsDestinationGraph())
    include(buildProcessDestinationGraph())
}

