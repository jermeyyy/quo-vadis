package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.SearchDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import org.koin.compose.koinInject

/**
 * Search Results Screen - Demonstrates typed query parameters
 *
 * This screen receives:
 * - `query: String` (required) - The search term
 * - `page: Int = 1` (optional) - Current page number
 * - `sortAsc: Boolean = true` (optional) - Sort direction
 *
 * These parameters are parsed from the deep link:
 * `app://search/results?query=kotlin&page=2&sortAsc=true`
 *
 * The KSP-generated deep link handler performs type conversions:
 * - `page` is converted to Int via `.toIntOrNull() ?: 1`
 * - `sortAsc` is converted to Boolean via `.toBooleanStrictOrNull() ?: true`
 */
@Screen(SearchDestination.Results::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    destination: SearchDestination.Results,
    navigator: Navigator = koinInject()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Results") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        SearchResultsContent(
            destination = destination,
            padding = padding
        )
    }
}

@Composable
private fun SearchResultsContent(
    destination: SearchDestination.Results,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Parameter info card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, null)
                        Text(
                            "Typed Query Parameters",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        "This destination received the following parameters " +
                            "from the deep link, with automatic type conversion:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ParameterRow("query", destination.query, "String")
                            ParameterRow("page", destination.page.toString(), "Int")
                            ParameterRow("sortAsc", destination.sortAsc.toString(), "Boolean")
                        }
                    }
                }
            }
        }

        // Code example card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Destination Definition",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "@Destination(route = \"search/results\")\n" +
                                "data class Results(\n" +
                                "    @Argument val query: String,\n" +
                                "    @Argument(optional = true) val page: Int = 1,\n" +
                                "    @Argument(optional = true) val sortAsc: Boolean = true\n" +
                                ") : SearchDestination()",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Mock search results
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "Mock Results for \"${destination.query}\"",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Page ${destination.page}, sorted ${if (destination.sortAsc) "ascending" else "descending"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Generate some mock results
        val mockResults = (1..5).map { i ->
            val index = (destination.page - 1) * 5 + i
            "Result #$index: ${destination.query} related item"
        }

        items(mockResults) { result ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    result,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ParameterRow(name: String, value: String, type: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$name: $value",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
        Text(
            type,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
