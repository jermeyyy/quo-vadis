package com.jermey.navplayground.demo.simplified

import androidx.compose.runtime.Composable
import com.jermey.navplayground.demo.destinations.DetailData
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.ui.screens.masterdetail.DetailScreen
import com.jermey.navplayground.demo.ui.screens.masterdetail.MasterListScreen
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * SIMPLIFIED API DEMONSTRATION
 * 
 * With @Content annotation, you only need to:
 * 1. Define @Content functions for each destination
 * 2. Call the generated buildXxxGraph() function
 * 
 * NO MORE:
 * - Manual navigationGraph {} builders
 * - typedDestinationXxx() calls
 * - Boilerplate destination {} calls
 * 
 * Everything is auto-generated from @Content annotations!
 */

/**
 * Content for MasterDetailDestination.List
 * 
 * Simple destination - takes only Navigator parameter.
 */
@Content(MasterDetailDestination.List::class)
@Composable
fun MasterListContent(navigator: Navigator) {
    MasterListScreen(
        onItemClick = { itemId ->
            navigator.navigate(MasterDetailDestination.Detail(itemId))
        },
        onBack = { navigator.navigateBack() }
    )
}

/**
 * Content for MasterDetailDestination.Detail
 * 
 * Typed destination - takes typed data + Navigator.
 * The data is automatically extracted and deserialized by generated code!
 */
@Content(MasterDetailDestination.Detail::class)
@Composable
fun DetailContent(data: DetailData, navigator: Navigator) {
    DetailScreen(
        itemId = data.itemId,
        onBack = { navigator.navigateBack() },
        onNavigateToRelated = { relatedId ->
            navigator.navigate(MasterDetailDestination.Detail(relatedId))
        }
    )
}

/**
 * USAGE:
 * 
 * In your app setup:
 * ```kotlin
 * val masterDetailGraph = buildMasterDetailDestinationGraph()
 * 
 * GraphNavHost(
 *     graph = masterDetailGraph,
 *     navigator = navigator
 * )
 * ```
 * 
 * That's it! The generated buildMasterDetailDestinationGraph() function
 * automatically wires MasterListContent and DetailContent to their destinations.
 */
