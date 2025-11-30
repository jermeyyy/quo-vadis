@file:Suppress("TooManyFunctions")

package com.jermey.navplayground.demo.content

import androidx.compose.runtime.Composable
import com.jermey.navplayground.demo.destinations.DetailData
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.destinations.AppDestination
import com.jermey.navplayground.demo.destinations.TabDestination
import com.jermey.navplayground.demo.destinations.DeepLinkDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.ui.screens.DeepLinkDemoScreen
import com.jermey.navplayground.demo.ui.screens.HomeScreen
import com.jermey.navplayground.demo.ui.screens.ExploreScreen
import com.jermey.navplayground.demo.ui.screens.SettingsScreen
import com.jermey.navplayground.demo.ui.screens.masterdetail.DetailScreen
import com.jermey.navplayground.demo.ui.screens.masterdetail.MasterListScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStartScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep1Screen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep2AScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep2BScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep3Screen
import com.jermey.navplayground.demo.ui.screens.process.ProcessCompleteScreen
import com.jermey.navplayground.demo.ui.screens.tabs.TabSubItemScreen
import com.jermey.navplayground.demo.ui.screens.tabs.TabsMainScreen
import com.jermey.navplayground.demo.tabs.MainTabsScreen
import com.jermey.navplayground.demo.ui.screens.profile.ProfileScreen
import com.jermey.navplayground.demo.destinations.SettingsDestination
import com.jermey.navplayground.demo.destinations.StateDrivenDemoDestination
import com.jermey.navplayground.demo.ui.screens.SettingsDetailScreen
import com.jermey.navplayground.demo.ui.screens.statedriven.StateDrivenDemoScreen
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

/**
 * # Quo Vadis Demo App - Content Definitions
 * 
 * This file demonstrates the **@Content annotation** for connecting Composable
 * functions to navigation destinations.
 * 
 * ## @Content Annotation
 * 
 * The @Content annotation replaces manual graph builder code:
 * 
 * ### Before (Manual DSL):
 * ```kotlin
 * val graph = navigationGraph("main") {
 *     destination(MainDestination.Home) { _, navigator ->
 *         HomeScreen(navigator)
 *     }
 * }
 * ```
 * 
 * ### After (Annotation-Based):
 * ```kotlin
 * @Content(MainDestination.Home::class)
 * @Composable
 * fun HomeContent(navigator: Navigator) {
 *     HomeScreen(navigator)
 * }
 * ```
 * 
 * ## Function Signatures
 * 
 * ### Simple Destinations (no @Argument):
 * ```kotlin
 * @Content(Home::class)
 * @Composable
 * fun HomeContent(navigator: Navigator) { /* ... */ }
 * ```
 * 
 * ### Typed Destinations (with @Argument):
 * ```kotlin
 * @Content(Detail::class)
 * @Composable
 * fun DetailContent(data: DetailData, navigator: Navigator) { /* ... */ }
 * ```
 * Note: First parameter is the @Serializable data class type
 * 
 * ## Generated Code
 * 
 * KSP automatically generates graph builder functions that:
 * 1. Register all destinations from @Route annotations
 * 2. Wire @Content functions to their destinations
 * 3. Handle argument deserialization for typed destinations
 * 
 * Example generated code:
 * ```kotlin
 * fun buildMainDestinationGraph(): NavigationGraph {
 *     return navigationGraph("main") {
 *         startDestination(MainDestination.Home)
 *         
 *         destination(MainDestination.Home) { _, navigator ->
 *             HomeContent(navigator)
 *         }
 *         
 *         typedDestinationDetail(
 *             destination = MasterDetailDestination.Detail::class
 *         ) { data, navigator ->
 *             DetailContent(data, navigator)
 *         }
 *     }
 * }
 * ```
 * 
 * ## Benefits
 * - Clean separation of UI and navigation logic
 * - Type-safe content function signatures
 * - Automatic wiring in generated graph builders
 * - Easier to test (mock Navigator)
 * 
 * See: Destinations.kt for @Graph, @Route, @Argument usage
 * See: NavigationGraphs.kt for graph composition
 */

// ============================================================================
// APP LEVEL DESTINATIONS
// ============================================================================

@Content(AppDestination.MainTabs::class)
@Composable
fun MainTabsContent(navigator: Navigator) {
    MainTabsScreen(parentNavigator = navigator)
}

// ============================================================================
// TAB DESTINATIONS
// ============================================================================

@Content(TabDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    println("DEBUG_TAB_NAV: HomeContent - Composing")
    HomeScreen(
        onNavigateToMasterDetail = {
            navigator.navigate(
                MasterDetailDestination.List,
                NavigationTransitions.SlideHorizontal
            )
        },
        onNavigateToTabs = {
            navigator.navigate(TabsDestination.Main, NavigationTransitions.SlideHorizontal)
        },
        onNavigateToProcess = {
            navigator.navigate(ProcessDestination.Start, NavigationTransitions.SlideHorizontal)
        },
        onNavigateToStateDriven = {
            navigator.navigate(
                StateDrivenDemoDestination.Demo,
                NavigationTransitions.SlideHorizontal
            )
        },
        navigator = navigator
    )
    println("DEBUG_TAB_NAV: HomeContent - HomeScreen rendered")
}

@Content(TabDestination.Explore::class)
@Composable
fun ExploreContent(navigator: Navigator) {
    ExploreScreen(
        onItemClick = { itemId ->
            navigator.navigate(
                MasterDetailDestination.Detail(itemId),
                NavigationTransitions.SlideHorizontal
            )
        },
        navigator = navigator
    )
}

@Content(TabDestination.Profile::class)
@Composable
fun ProfileContent(navigator: Navigator) {
    // FlowMVI-based Profile screen (demo implementation)
    ProfileScreen(navigator = navigator)
}

@Content(TabDestination.Settings::class)
@Composable
fun SettingsContent(navigator: Navigator) {
    SettingsScreen(navigator = navigator)
}

// ============================================================================
// DEEPLINK DESTINATIONS
// ============================================================================

@Content(DeepLinkDestination.Demo::class)
@Composable
fun DeepLinkDemoContent(navigator: Navigator) {
    DeepLinkDemoScreen(
        onBack = { navigator.navigateBack() },
        onNavigateViaDeepLink = { deepLinkUri ->
            navigator.handleDeepLink(DeepLink.parse(deepLinkUri))
        }
    )
}

// ============================================================================
// STATE-DRIVEN NAVIGATION DEMO
// ============================================================================

@Content(StateDrivenDemoDestination.Demo::class)
@Composable
fun StateDrivenDemoContent(navigator: Navigator) {
    StateDrivenDemoScreen(
        onBack = { navigator.navigateBack() }
    )
}

// ============================================================================
// MASTER-DETAIL DESTINATIONS
// ============================================================================

@Content(MasterDetailDestination.List::class)
@Composable
fun MasterListContent(navigator: Navigator) {
    MasterListScreen(
        onItemClick = { itemId ->
            navigator.navigate(
                MasterDetailDestination.Detail(itemId),
                NavigationTransitions.SlideHorizontal
            )
        },
        onBack = { navigator.navigateBack() }
    )
}

/**
 * TYPED DESTINATION EXAMPLE
 * 
 * This demonstrates @Content with a typed destination:
 * - First parameter is DetailData (the @Serializable data class from @Argument)
 * - Navigator is second parameter
 * - KSP automatically deserializes DetailData from navigation arguments
 * - No manual argument extraction needed!
 * 
 * Compare with manual approach:
 * ```kotlin
 * destination(MasterDetailDestination.Detail) { dest, navigator ->
 *     val detail = dest as MasterDetailDestination.Detail
 *     DetailScreen(itemId = detail.itemId, ...)  // Manual extraction
 * }
 * ```
 * 
 * With annotations, data is already deserialized and type-safe.
 */
@Content(MasterDetailDestination.Detail::class)
@Composable
fun DetailContent(data: DetailData, navigator: Navigator) {
    DetailScreen(
        itemId = data.itemId,  // Data already deserialized!
        onBack = { navigator.navigateBack() },
        onNavigateToRelated = { relatedId ->
            // Can still navigate manually, or use generated extension
            navigator.navigate(
                MasterDetailDestination.Detail(relatedId),
                NavigationTransitions.SlideHorizontal
            )
        }
    )
}

// ============================================================================
// TABS DESTINATIONS
// ============================================================================

@Content(TabsDestination.Main::class)
@Composable
fun TabsMainContent(navigator: Navigator) {
    TabsMainScreen(
        onNavigateToSubItem = { tabId, itemId ->
            navigator.navigate(
                TabsDestination.SubItem(tabId, itemId),
                NavigationTransitions.SlideHorizontal
            )
        },
        onBack = { navigator.navigateBack() },
        navigator = navigator
    )
}

@Content(TabsDestination.SubItem::class)
@Composable
fun TabSubItemContent(data: TabsDestination.SubItemData, navigator: Navigator) {
    TabSubItemScreen(
        tabId = data.tabId,
        itemId = data.itemId,
        onBack = { navigator.navigateBack() }
    )
}

// ============================================================================
// PROCESS/WIZARD DESTINATIONS
// ============================================================================

@Content(ProcessDestination.Start::class)
@Composable
fun ProcessStartContent(navigator: Navigator) {
    ProcessStartScreen(
        onStart = {
            navigator.navigate(
                ProcessDestination.Step1(),
                NavigationTransitions.SlideHorizontal
            )
        },
        onCancel = { navigator.navigateBack() }
    )
}

@Content(ProcessDestination.Step1::class)
@Composable
fun ProcessStep1Content(data: ProcessDestination.Step1Data, navigator: Navigator) {
    ProcessStep1Screen(
        initialUserType = data.userType,
        onNext = { selectedType, stepData ->
            // Branch based on user selection
            if (selectedType == "personal") {
                navigator.navigate(
                    ProcessDestination.Step2A(stepData),
                    NavigationTransitions.SlideHorizontal
                )
            } else {
                navigator.navigate(
                    ProcessDestination.Step2B(stepData),
                    NavigationTransitions.SlideHorizontal
                )
            }
        },
        onBack = { navigator.navigateBack() }
    )
}

@Content(ProcessDestination.Step2A::class)
@Composable
fun ProcessStep2AContent(data: ProcessDestination.Step2Data, navigator: Navigator) {
    ProcessStep2AScreen(
        previousData = data.stepData,
        onNext = { newData ->
            navigator.navigate(
                ProcessDestination.Step3(newData, "personal"),
                NavigationTransitions.SlideHorizontal
            )
        },
        onBack = { navigator.navigateBack() }
    )
}

@Content(ProcessDestination.Step2B::class)
@Composable
fun ProcessStep2BContent(data: ProcessDestination.Step2Data, navigator: Navigator) {
    ProcessStep2BScreen(
        previousData = data.stepData,
        onNext = { newData ->
            navigator.navigate(
                ProcessDestination.Step3(newData, "business"),
                NavigationTransitions.SlideHorizontal
            )
        },
        onBack = { navigator.navigateBack() }
    )
}

@Content(ProcessDestination.Step3::class)
@Composable
fun ProcessStep3Content(data: ProcessDestination.Step3Data, navigator: Navigator) {
    ProcessStep3Screen(
        previousData = data.previousData,
        branch = data.branch,
        onComplete = {
            navigator.navigate(
                ProcessDestination.Complete,
                NavigationTransitions.SlideHorizontal
            )
        },
        onBack = { navigator.navigateBack() }
    )
}

@Content(ProcessDestination.Complete::class)
@Composable
fun ProcessCompleteContent(navigator: Navigator) {
    ProcessCompleteScreen(
        onDone = {
            // Navigate back to root, clearing the process flow
            navigator.navigateBack()
        },
        onRestart = {
            // Restart the process
            navigator.navigate(
                ProcessDestination.Start,
                NavigationTransitions.SlideHorizontal
            )
        }
    )
}

@Content(SettingsDestination.Profile::class)
@Composable
fun ProfileSettingsContent(navigator: Navigator) {
    SettingsDetailScreen("Profile", navigator)
}

@Content(SettingsDestination.Notifications::class)
@Composable
fun NotificationsSettingsContent(navigator: Navigator) {
    SettingsDetailScreen("Notifications", navigator)
}

@Content(SettingsDestination.About::class)
@Composable
fun AboutSettingsContent(navigator: Navigator) {
    SettingsDetailScreen("About", navigator)
}
