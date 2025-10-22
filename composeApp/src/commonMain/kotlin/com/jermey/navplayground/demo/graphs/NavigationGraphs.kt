package com.jermey.navplayground.demo.graphs

import androidx.compose.animation.ExperimentalSharedTransitionApi
import com.jermey.navplayground.SampleDestinations
import com.jermey.navplayground.demo.destinations.DetailData
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.ui.screens.DeepLinkDemoScreen
import com.jermey.navplayground.demo.ui.screens.ExploreScreen
import com.jermey.navplayground.demo.ui.screens.HomeScreen
import com.jermey.navplayground.demo.ui.screens.ProfileScreen
import com.jermey.navplayground.demo.ui.screens.SettingsScreen
import com.jermey.navplayground.demo.ui.screens.masterdetail.DetailScreen
import com.jermey.navplayground.demo.ui.screens.masterdetail.MasterListScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessCompleteScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStartScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep1Screen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep2AScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep2BScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep3Screen
import com.jermey.navplayground.demo.ui.screens.tabs.TabSubItemScreen
import com.jermey.navplayground.demo.ui.screens.tabs.TabsMainScreen
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.navigationGraph
import com.jermey.quo.vadis.core.navigation.core.typedDestination
import kotlinx.serialization.serializer

/**
 * Root application navigation graph.
 *
 * This graph contains all main bottom navigation destinations
 *
 * Nested graphs (master-detail, tabs, process) are included as full-screen destinations.
 */
fun appRootGraph() = navigationGraph("app_root") {
    startDestination(MainDestination.Home)

    // Main bottom nav screens use Fade transition
    destination(MainDestination.Home, NavigationTransitions.Fade) { _, navigator ->
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
            navigator = navigator
        )
    }

    destination(MainDestination.Explore, NavigationTransitions.Fade) { _, navigator ->
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

    destination(MainDestination.Profile, NavigationTransitions.Fade) { _, navigator ->
        ProfileScreen(
            onEditProfile = {
                // Could navigate to edit screen
            },
            navigator = navigator
        )
    }

    destination(MainDestination.Settings, NavigationTransitions.Fade) { _, navigator ->
        SettingsScreen(
            navigator = navigator
        )
    }

    destination(MainDestination.DeepLinkDemo, NavigationTransitions.Fade) { _, navigator ->
        DeepLinkDemoScreen(
            onBack = { navigator.navigateBack() },
            onNavigateViaDeepLink = { deepLinkUri ->
                navigator.handleDeepLink(DeepLink.parse(deepLinkUri))
            }
        )
    }

    // Include other graphs as nested destinations
    include(masterDetailGraph())
    include(tabsGraph())
    include(processGraph())
}

/**
 * Master-Detail navigation graph with shared element transitions
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun masterDetailGraph() = navigationGraph("master_detail") {
    startDestination(MasterDetailDestination.List)

    // Use scope-aware destination for shared element support
    destinationWithScopes(
        destination = MasterDetailDestination.List,
        transition = NavigationTransitions.SlideHorizontal
    ) { _, navigator, transitionScope ->
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

    @OptIn(ExperimentalSharedTransitionApi::class)
    typedDestination(MasterDetailDestination.Detail.ROUTE) { data: DetailData, navigator ->
        DetailScreen(
            itemId = data.itemId,
            onBack = { navigator.navigateBack() },
            onNavigateToRelated = { relatedId ->
                navigator.navigate(
                    MasterDetailDestination.Detail(relatedId),
                    NavigationTransitions.SlideHorizontal
                )
            }
        )
    }
}

/**
 * Tabs navigation graph
 */
fun tabsGraph() = navigationGraph("tabs") {
    startDestination(TabsDestination.Main)

    destination(TabsDestination.Main, NavigationTransitions.SlideVertical) { _, navigator ->
        TabsMainScreen(
            onNavigateToSubItem = { tabId, itemId ->
                navigator.navigate(
                    TabsDestination.SubItem(tabId, itemId),
                    NavigationTransitions.SlideVertical
                )
            },
            onBack = { navigator.navigateBack() }
        )
    }

    typedDestination(TabsDestination.SubItem.ROUTE) { data: TabsDestination.SubItemData, navigator ->
        TabSubItemScreen(
            tabId = data.tabId,
            itemId = data.itemId,
            onBack = { navigator.navigateBack() }
        )
    }
}

/**
 * Process/Wizard navigation graph with branching logic
 */
fun processGraph() = navigationGraph("process") {
    startDestination(ProcessDestination.Start)

    destination(ProcessDestination.Start, NavigationTransitions.SlideHorizontal) { _, navigator ->
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

    typedDestination(ProcessDestination.Step1.ROUTE) { data: ProcessDestination.Step1Data, navigator ->
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

    typedDestination(ProcessDestination.Step2A.ROUTE) { data: ProcessDestination.Step2Data, navigator ->
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

    typedDestination(ProcessDestination.Step2B.ROUTE) { data: ProcessDestination.Step2Data, navigator ->
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

    typedDestination(ProcessDestination.Step3.ROUTE) { data: ProcessDestination.Step3Data, navigator ->
        ProcessStep3Screen(
            previousData = data.previousData,
            branch = data.branch,
            onComplete = {
                navigator.navigateAndClearTo(
                    ProcessDestination.Complete,
                    clearRoute = "process_start",
                    inclusive = false
                )
            },
            onBack = { navigator.navigateBack() }
        )
    }

    destination(ProcessDestination.Complete, NavigationTransitions.ScaleIn) { _, navigator ->
        ProcessCompleteScreen(
            onDone = {
                navigator.navigateAndClearAll(MainDestination.Home)
            },
            onRestart = {
                navigator.navigateAndClearTo(
                    ProcessDestination.Start,
                    clearRoute = "process_complete",
                    inclusive = true
                )
            }
        )
    }
}

