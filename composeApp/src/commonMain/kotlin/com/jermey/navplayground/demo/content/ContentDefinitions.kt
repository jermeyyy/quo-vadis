@file:Suppress("TooManyFunctions")

package com.jermey.navplayground.demo.content

import androidx.compose.runtime.Composable
import com.jermey.navplayground.demo.destinations.DetailData
import com.jermey.navplayground.demo.destinations.TabsDestination
import com.jermey.navplayground.demo.destinations.MainDestination
import com.jermey.navplayground.demo.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.destinations.ProcessDestination
import com.jermey.navplayground.demo.ui.screens.DeepLinkDemoScreen
import com.jermey.navplayground.demo.ui.screens.HomeScreen
import com.jermey.navplayground.demo.ui.screens.ExploreScreen
import com.jermey.navplayground.demo.ui.screens.ProfileScreen
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
import com.jermey.quo.vadis.annotations.Content
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

/**
 * Content definitions for all navigation destinations.
 * 
 * This file replaces manual graph builders with simple @Content annotations.
 * KSP generates the complete graph DSL automatically.
 */

// ============================================================================
// MAIN DESTINATIONS
// ============================================================================

@Content(MainDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
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

@Content(MainDestination.Explore::class)
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

@Content(MainDestination.Profile::class)
@Composable
fun ProfileContent(navigator: Navigator) {
    ProfileScreen(
        onEditProfile = {
            // Could navigate to edit screen
        },
        navigator = navigator
    )
}

@Content(MainDestination.Settings::class)
@Composable
fun SettingsContent(navigator: Navigator) {
    SettingsScreen(navigator = navigator)
}

@Content(MainDestination.DeepLinkDemo::class)
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

@Content(MasterDetailDestination.Detail::class)
@Composable
fun DetailContent(data: DetailData, navigator: Navigator) {
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
        onBack = { navigator.navigateBack() }
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
