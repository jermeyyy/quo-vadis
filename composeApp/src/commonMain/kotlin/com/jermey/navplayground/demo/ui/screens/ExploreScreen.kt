package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
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
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlinx.coroutines.launch

private const val EXPLORE_ITEMS_COUNT = 20

/**
 * Explore Screen - Shows a grid/list of items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onItemClick: (String) -> Unit,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val items = remember {
        (1..EXPLORE_ITEMS_COUNT).map { "Item $it" }
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Explore") },
                navigationIcon = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        ExploreScreenContent(modifier, paddingValues, items, onItemClick)
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            NavigationBottomSheetContent(
                currentRoute = "explore",
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
private fun ExploreScreenContent(
    modifier: Modifier,
    paddingValues: PaddingValues,
    items: List<String>,
    onItemClick: (String) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "Explore Items",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items(items) { item ->
            Card(
                onClick = { onItemClick(item) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(item, style = MaterialTheme.typography.bodyLarge)
                    Icon(Icons.Default.ChevronRight, "Open")
                }
            }
        }
    }
}

@Composable
private fun remember(calculation: () -> List<String>): List<String> {
    return androidx.compose.runtime.remember(calculation)
}
