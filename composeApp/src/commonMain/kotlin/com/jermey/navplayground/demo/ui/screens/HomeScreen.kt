package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssistantDirection
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.NavigationBottomSheetContent
import com.jermey.navplayground.demo.ui.components.NavigationPatternCard
import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlinx.coroutines.launch

/**
 * Home Screen - Main entry point with navigation to all patterns
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMasterDetail: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onNavigateToProcess: () -> Unit,
    onNavigateToStateDriven: () -> Unit,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
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
            modifier,
            paddingValues,
            onNavigateToMasterDetail,
            onNavigateToTabs,
            onNavigateToProcess,
            onNavigateToStateDriven
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
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
    onNavigateToStateDriven: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Navigation Patterns Demo",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Explore different navigation patterns implemented with our custom navigation library",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

        Spacer(Modifier.weight(1f))

        Text(
            "Use the bottom navigation to switch between main sections",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

