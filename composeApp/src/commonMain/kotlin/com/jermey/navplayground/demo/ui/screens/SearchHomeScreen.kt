package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.SearchDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import org.koin.compose.koinInject

/**
 * Search Home Screen - Entry point for search functionality.
 *
 * This screen allows users to initiate a search, which navigates
 * to the SearchResults screen with query parameters.
 */
@Screen(SearchDestination.Home::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHomeScreen(
    navigator: Navigator = koinInject()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Search Demo",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Demonstrates typed query parameters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    navigator.navigate(
                        SearchDestination.Results(
                            query = "kotlin",
                            page = 1,
                            sortAsc = true
                        )
                    )
                }
            ) {
                Text("Search for 'kotlin'")
            }
        }
    }
}
