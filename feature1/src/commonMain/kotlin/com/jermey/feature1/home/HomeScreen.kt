package com.jermey.feature1.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssistantDirection
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jermey.feature2.api.AuthFlowDestination
import com.jermey.navplayground.navigation.BackHandlerDemoDestination
import com.jermey.navplayground.navigation.DemoTabs
import com.jermey.navplayground.navigation.HomeTab
import com.jermey.navplayground.navigation.MasterDetailDestination
import com.jermey.navplayground.navigation.MessagesPane
import com.jermey.navplayground.navigation.NavigationMenuDestination
import com.jermey.navplayground.navigation.ProcessDestination
import com.jermey.navplayground.navigation.ResultDemoDestination
import com.jermey.navplayground.navigation.StateDrivenDemoDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import navplayground.feature1.generated.resources.Res
import navplayground.feature1.generated.resources.logo
import org.jetbrains.compose.resources.imageResource
import org.koin.compose.koinInject

/**
 * Home Screen - Main entry point with navigation to all patterns
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Screen(HomeTab::class)
@Composable
fun HomeScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    val hazeState = remember { HazeState() }

    val hazeStyle = HazeMaterials.ultraThin()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigate(NavigationMenuDestination) }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .hazeEffect(state = hazeState) {
                        style = hazeStyle
                    }
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            )
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = modifier.hazeSource(state = hazeState),
            paddingValues = paddingValues,
            onNavigateToMasterDetail = {
                navigator.navigate(
                    MasterDetailDestination.List,
                    NavigationTransitions.SlideHorizontal
                )
            },
            onNavigateToTabs = {
                navigator.navigate(DemoTabs.BooksTab.List, NavigationTransitions.SlideHorizontal)
            },
            onNavigateToProcess = {
                navigator.navigate(ProcessDestination.Start, NavigationTransitions.SlideHorizontal)
            },
            onNavigateToStateDriven = {
                navigator.navigate(
                    StateDrivenDemoDestination.DemoTab.Home,
                    NavigationTransitions.SlideHorizontal
                )
            },
            onNavigateToAuthFlow = {
                navigator.navigate(
                    AuthFlowDestination.Login,
                    NavigationTransitions.SlideHorizontal
                )
            },
            onNavigateToResultDemo = {
                navigator.navigate(
                    ResultDemoDestination.Demo,
                    NavigationTransitions.SlideHorizontal
                )
            },
            onNavigateToMessagesPane = {
                navigator.navigate(
                    MessagesPane.ConversationList,
                    NavigationTransitions.SlideHorizontal
                )
            },
            onNavigateToBackHandlerDemo = {
                navigator.navigate(
                    BackHandlerDemoDestination,
                    NavigationTransitions.SlideHorizontal
                )
            }
        )
    }
}

@Composable
private fun HomeScreenContent(
    modifier: Modifier,
    paddingValues: PaddingValues,
    onNavigateToMasterDetail: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onNavigateToProcess: () -> Unit,
    onNavigateToStateDriven: () -> Unit,
    onNavigateToAuthFlow: () -> Unit,
    onNavigateToResultDemo: () -> Unit,
    onNavigateToMessagesPane: () -> Unit,
    onNavigateToBackHandlerDemo: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = paddingValues.calculateTopPadding() + 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 16.dp,
                end = 16.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Quo Vadis Demo",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Image(
            imageResource(Res.drawable.logo),
            contentDescription = "Logo",
            modifier = Modifier.width(72.dp).height(72.dp).align(Alignment.CenterHorizontally)
        )

        Text(
            "Explore different navigation patterns implemented with our custom navigation library",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        NavigationPatternCard(
            icon = Icons.AutoMirrored.Filled.List,
            title = "Master-Detail",
            description = "List with detail view navigation pattern",
            onClick = onNavigateToMasterDetail
        )

        NavigationPatternCard(
            icon = Icons.Default.Dashboard,
            title = "Tabs Navigation",
            description = "Nested tabs with sub-navigation",
            onClick = onNavigateToTabs
        )

        NavigationPatternCard(
            icon = Icons.AutoMirrored.Filled.AssistantDirection,
            title = "Process Flow",
            description = "Multi-step wizard with branching logic",
            onClick = onNavigateToProcess
        )

        NavigationPatternCard(
            icon = Icons.Default.Layers,
            title = "State-Driven Navigation",
            description = "Navigation 3-style backstack manipulation",
            onClick = onNavigateToStateDriven
        )

        NavigationPatternCard(
            icon = Icons.Default.Lock,
            title = "Auth Flow (Scoped Stack)",
            description = "Scope-aware navigation with in/out of scope destinations",
            onClick = onNavigateToAuthFlow
        )

        NavigationPatternCard(
            icon = Icons.AutoMirrored.Filled.Send,
            title = "Navigation with Result",
            description = "Navigate to a screen and await a result",
            onClick = onNavigateToResultDemo
        )

        NavigationPatternCard(
            icon = Icons.Default.Smartphone,
            title = "Adaptive Panes (Foldable)",
            description = "List-detail pane layout for foldables and tablets",
            onClick = onNavigateToMessagesPane
        )

        NavigationPatternCard(
            icon = Icons.Default.SwipeLeft,
            title = "Back Handler",
            description = "Custom back handling with unsaved changes pattern",
            onClick = onNavigateToBackHandlerDemo
        )
        Spacer(modifier = Modifier.height(64.dp))
    }
}
