package com.jermey.navplayground.demo.graphs

import com.jermey.navplayground.demo.destinations.DemoDestination
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.ui.screens.DeepLinkDemoScreen
import com.jermey.navplayground.demo.ui.screens.masterdetail.DetailScreen
import com.jermey.navplayground.demo.ui.screens.ExploreScreen
import com.jermey.navplayground.demo.ui.screens.HomeScreen
import com.jermey.navplayground.demo.ui.screens.masterdetail.MasterListScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessCompleteScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStartScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep1Screen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep2AScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep2BScreen
import com.jermey.navplayground.demo.ui.screens.process.ProcessStep3Screen
import com.jermey.navplayground.demo.ui.screens.ProfileScreen
import com.jermey.navplayground.demo.ui.screens.SettingsScreen
import com.jermey.navplayground.demo.ui.screens.tabs.TabSubItemScreen
import com.jermey.navplayground.demo.ui.screens.tabs.TabsMainScreen
import com.jermey.navplayground.demo.destinations.RootScreen
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.SimpleDestination
import com.jermey.quo.vadis.core.navigation.core.navigationGraph

fun demoRootGraph() = navigationGraph("demo_root_graph") {
    startDestination(DemoDestination.Root)

    destination(DemoDestination.Root) { _, _ ->
        RootScreen()
    }
}

/**
 * Main bottom navigation graph
 */
fun mainBottomNavGraph() = navigationGraph("main") {
    startDestination(MainDestination.Home)

    destination(MainDestination.Home) { _, navigator ->
        HomeScreen(
            onNavigateToMasterDetail = {
                navigator.navigate(MasterDetailDestination.List)
            },
            onNavigateToTabs = {
                navigator.navigate(TabsDestination.Main)
            },
            onNavigateToProcess = {
                navigator.navigate(ProcessDestination.Start)
            }
        )
    }

    destination(MainDestination.Explore) { _, navigator ->
        ExploreScreen(
            onItemClick = { itemId ->
                navigator.navigate(MasterDetailDestination.Detail(itemId))
            }
        )
    }

    destination(MainDestination.Profile) { _, navigator ->
        ProfileScreen(
            onEditProfile = {
                // Could navigate to edit screen
            }
        )
    }

    destination(MainDestination.Settings) { _, navigator ->
        SettingsScreen(
        )
    }

    destination(MainDestination.DeepLinkDemo) { _, navigator ->
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
 * Master-Detail navigation graph
 */
fun masterDetailGraph() = navigationGraph("master_detail") {
    startDestination(MasterDetailDestination.List)

    destination(MasterDetailDestination.List) { _, navigator ->
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

    destination(SimpleDestination("master_detail_detail")) { dest, navigator ->
        val itemId = dest.arguments["itemId"] as? String ?: "unknown"
        DetailScreen(
            itemId = itemId,
            onBack = { navigator.navigateBack() },
            onNavigateToRelated = { relatedId ->
                navigator.navigate(MasterDetailDestination.Detail(relatedId))
            }
        )
    }
}

/**
 * Tabs navigation graph
 */
fun tabsGraph() = navigationGraph("tabs") {
    startDestination(TabsDestination.Main)

    destination(TabsDestination.Main) { _, navigator ->
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

    destination(SimpleDestination("tabs_subitem")) { dest, navigator ->
        val tabId = dest.arguments["tabId"] as? String ?: "tab1"
        val itemId = dest.arguments["itemId"] as? String ?: "unknown"
        TabSubItemScreen(
            tabId = tabId,
            itemId = itemId,
            onBack = { navigator.navigateBack() }
        )
    }
}

/**
 * Process/Wizard navigation graph with branching logic
 */
fun processGraph() = navigationGraph("process") {
    startDestination(ProcessDestination.Start)

    destination(ProcessDestination.Start) { _, navigator ->
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

    destination(SimpleDestination("process_step1")) { dest, navigator ->
        val userType = dest.arguments["userType"] as? String
        ProcessStep1Screen(
            initialUserType = userType,
            onNext = { selectedType, data ->
                // Branch based on user selection
                if (selectedType == "personal") {
                    navigator.navigate(
                        ProcessDestination.Step2A(data),
                        NavigationTransitions.SlideHorizontal
                    )
                } else {
                    navigator.navigate(
                        ProcessDestination.Step2B(data),
                        NavigationTransitions.SlideHorizontal
                    )
                }
            },
            onBack = { navigator.navigateBack() }
        )
    }

    destination(SimpleDestination("process_step2a")) { dest, navigator ->
        val data = dest.arguments["data"] as? String ?: ""
        ProcessStep2AScreen(
            previousData = data,
            onNext = { newData ->
                navigator.navigate(
                    ProcessDestination.Step3(newData, "personal"),
                    NavigationTransitions.SlideHorizontal
                )
            },
            onBack = { navigator.navigateBack() }
        )
    }

    destination(SimpleDestination("process_step2b")) { dest, navigator ->
        val data = dest.arguments["data"] as? String ?: ""
        ProcessStep2BScreen(
            previousData = data,
            onNext = { newData ->
                navigator.navigate(
                    ProcessDestination.Step3(newData, "business"),
                    NavigationTransitions.SlideHorizontal
                )
            },
            onBack = { navigator.navigateBack() }
        )
    }

    destination(SimpleDestination("process_step3")) { dest, navigator ->
        val previousData = dest.arguments["previousData"] as? String ?: ""
        val branch = dest.arguments["branch"] as? String ?: ""
        ProcessStep3Screen(
            previousData = previousData,
            branch = branch,
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

    destination(ProcessDestination.Complete) { _, navigator ->
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
