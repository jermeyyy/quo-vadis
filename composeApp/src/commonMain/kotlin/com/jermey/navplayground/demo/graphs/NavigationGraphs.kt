package com.jermey.navplayground.demo.graphs

// Import auto-generated graph builders
import com.jermey.navplayground.demo.destinations.AppDestination
import com.jermey.navplayground.demo.destinations.TabDestination
import com.jermey.navplayground.demo.destinations.buildAppDestinationGraph
import com.jermey.navplayground.demo.destinations.buildTabDestinationGraph
import com.jermey.navplayground.demo.destinations.buildDeepLinkDestinationGraph
import com.jermey.navplayground.demo.destinations.buildMasterDetailDestinationGraph
import com.jermey.navplayground.demo.destinations.buildProcessDestinationGraph
import com.jermey.navplayground.demo.destinations.buildTabsDestinationGraph
import com.jermey.navplayground.demo.destinations.buildSettingsDestinationGraph
import com.jermey.quo.vadis.core.navigation.core.navigationGraph

/**
 * # Quo Vadis Demo App - Navigation Graph Composition
 * 
 * This file demonstrates how to compose multiple auto-generated graphs
 * into a root navigation graph.
 * 
 * ## Auto-Generated Graph Builders
 * 
 * KSP generates these functions from @Graph annotations:
 * - `buildMainDestinationGraph()` - from MainDestination sealed class
 * - `buildMasterDetailDestinationGraph()` - from MasterDetailDestination
 * - `buildTabsDestinationGraph()` - from TabsDestination
 * - `buildProcessDestinationGraph()` - from ProcessDestination
 * 
 * Each generated function:
 * 1. Creates a navigationGraph with correct name
 * 2. Registers all @Route destinations
 * 3. Wires @Content functions to destinations
 * 4. Handles typed destination serialization
 * 
 * ## Manual vs Generated Comparison
 * 
 * ### Before (Manual DSL):
 * ```kotlin
 * fun masterDetailGraph() = navigationGraph("master_detail") {
 *     startDestination(MasterDetailDestination.List)
 *     
 *     destination(MasterDetailDestination.List) { _, nav ->
 *         MasterListScreen(nav)
 *     }
 *     
 *     destination(MasterDetailDestination.Detail) { dest, nav ->
 *         val detail = dest as MasterDetailDestination.Detail
 *         DetailScreen(itemId = detail.itemId, nav)
 *     }
 * }
 * ```
 * 
 * ### After (Annotation-Based):
 * ```kotlin
 * // Just call generated function!
 * val masterDetailGraph = buildMasterDetailDestinationGraph()
 * ```
 * 
 * All the graph building code is generated from:
 * - @Graph on sealed class
 * - @Route on destinations
 * - @Content on Composable functions
 * - @Argument for typed destinations
 * 
 * ## Benefits
 * - 70% less code to write and maintain
 * - Compile-time safety for all navigation
 * - Automatic argument serialization
 * - Easy to add new destinations (just annotate!)
 * - Generated code is tested and consistent
 * 
 * See: Destinations.kt for @Graph, @Route, @Argument
 * See: ContentDefinitions.kt for @Content
 */

/**
 * Root application navigation graph.
 *
 * This is the top-level graph that contains:
 * - MainTabs destination (the tabbed container)
 * - Full-screen flows that should render on top of tabs (MasterDetail, Process, etc.)
 *
 * This prevents conflicts by ensuring tab-level and app-level navigation are separate.
 */
fun appRootGraph() = navigationGraph("app_root") {
    startDestination(AppDestination.MainTabs)
    
    // Include the app-level graph with MainTabs destination
    include(buildAppDestinationGraph())
    
    // Include full-screen flows that render on top of tabs
    include(buildMasterDetailDestinationGraph())
    include(buildTabsDestinationGraph())
    include(buildProcessDestinationGraph())
    include(buildDeepLinkDestinationGraph())
}

/**
 * Tab content navigation graph.
 *
 * This graph includes ONLY destinations that should be accessible within tabs:
 * - Tab root destinations (Home, Explore, Profile, Settings)
 *
 * Important: This graph is used by tab navigators, NOT by the parent navigator.
 * Deep navigation flows (MasterDetail, Process, etc.) are in appRootGraph.
 */
fun tabContentGraph() = navigationGraph("tab_content") {
    // Tab root destinations only
    startDestination(TabDestination.Home)
    include(buildTabDestinationGraph())
    include(buildSettingsDestinationGraph())
}

