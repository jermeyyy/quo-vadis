package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jermey.feature1.resultdemo.ResultDemoDestination
import com.jermey.feature2.AuthFlowDestination
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.DemoTabs
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MainTabs
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MasterDetailDestination
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MessagesPane
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.ProcessDestination
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.StateDrivenDemoDestination
import com.jermey.navplayground.demo.ui.components.NavigationBottomSheetContent
import com.jermey.navplayground.demo.ui.components.NavigationPatternCard
import com.jermey.navplayground.demo.ui.components.glassmorphism.GlassBottomSheet
import com.jermey.quo.vadis.annotations.Screen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions
import kotlinx.coroutines.launch
import navplayground.composeapp.generated.resources.Res
import navplayground.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.imageResource
import org.koin.compose.koinInject

/**
 * Home Screen - Main entry point with navigation to all patterns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(MainTabs.HomeTab::class)
@Composable
fun HomeScreen(
    navigator: Navigator = koinInject(),
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                navigationIcon = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
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
            }
        )
    }

    if (showBottomSheet) {
        GlassBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            hazeState = hazeState,
            sheetState = sheetState
        ) {
            NavigationBottomSheetContent(
                currentRoute = "home",
                onNavigate = { destination ->
                    navigator.navigate(destination)
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                    }
                }
            )
        }
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
    onNavigateToMessagesPane: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(scrollState)
            .padding(16.dp),
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
        Spacer(modifier = Modifier.height(64.dp))
    }
}

