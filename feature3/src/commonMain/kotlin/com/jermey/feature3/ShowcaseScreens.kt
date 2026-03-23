package com.jermey.feature3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.navigation.ExploreTab
import com.jermey.navplayground.navigation.ShowcaseTab
import com.jermey.navplayground.navigation.ResultDemoFeatureEntry
import com.jermey.navplayground.navigation.SelectedItemResult
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Screen(ShowcaseTab.Overview::class)
@Composable
fun ShowcaseOverviewScreen(navigator: Navigator = koinInject()) {
    // FeatureEntry — no dependency on feature1, only on navigation-api
    val resultDemoEntry: ResultDemoFeatureEntry = koinInject()
    val scope = rememberCoroutineScope()
    var lastResult by remember { mutableStateOf<SelectedItemResult?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Showcase") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cross-Module Navigation Demo", style = MaterialTheme.typography.headlineMedium)
            Text(
                "This tab is defined in feature3-api, screens in feature3.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { navigator.navigate(ShowcaseTab.Detail("showcase_1")) }) {
                Text("Navigate to Detail (same module)")
            }
            // Cross-module navigation: navigate to feature1-api destination
            Button(onClick = { navigator.navigate(ExploreTab.Feed) }) {
                Text("Navigate to Explore Feed (feature1-api)")
            }
            // FeatureEntry pattern: suspend navigate-for-result, no feature1 dependency
            Button(onClick = {
                scope.launch {
                    val result = resultDemoEntry.start()
                    lastResult = result
                }
            }) {
                Text("Pick Item via Result Demo (FeatureEntry)")
            }
            // Show the result returned from the feature
            lastResult?.let { item ->
                Text(
                    "Last picked: ${item.name} (${item.id})",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Screen(ShowcaseTab.Detail::class)
@Composable
fun ShowcaseDetailScreen(
    destination: ShowcaseTab.Detail,
    navigator: Navigator = koinInject()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Showcase Detail") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Item: ${destination.itemId}", style = MaterialTheme.typography.headlineMedium)
            Text("Screen defined in feature3, destination in feature3-api")
        }
    }
}
