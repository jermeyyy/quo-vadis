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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.destinations.DeepLinkDestination
import com.jermey.navplayground.demo.destinations.PromoDestination
import com.jermey.navplayground.demo.ui.components.DeepLinkCard
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.Navigator
import org.koin.compose.koinInject

/**
 * DeepLink Demo Screen - Shows deep linking functionality
 *
 * Demonstrates:
 * - New simplified `navigator.handleDeepLink(uri)` API
 * - Runtime deep link registration via `getDeepLinkRegistry()`
 * - Query parameter handling with `@Argument(optional = true)`
 * - Type conversions (Int, Boolean) in deep link parameters
 */
@Screen(DeepLinkDestination.Demo::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkDemoScreen(
    navigator: Navigator = koinInject()
) {
    // Example: Register a promo code handler at runtime
    // This demonstrates runtime deep link registration
    LaunchedEffect(Unit) {
        navigator.getDeepLinkRegistry().register("promo/{code}") { params ->
            PromoDestination(code = params["code"]!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deep Link Demo") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        DeepLinkDemoScreenContent(padding) { deepLinkUri ->
            // New simplified API - just pass the URI string directly
            navigator.handleDeepLink(deepLinkUri)
        }
    }
}

@Composable
private fun DeepLinkDemoScreenContent(
    padding: PaddingValues,
    onNavigateViaDeepLink: (String) -> Unit
) {
    var customDeepLink by remember { mutableStateOf("app://search/results?query=kotlin&page=2") }
    var lastParsedDeepLink by remember { mutableStateOf<DeepLink?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Deep Link Navigation",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Click on any example below to navigate using deep links. " +
                                "This demonstrates how the navigation library handles URI-based navigation.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Custom Deep Link Tester
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                        Icon(Icons.Default.Code, null)
                        Text(
                            "Test Custom Deep Link",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    OutlinedTextField(
                        value = customDeepLink,
                        onValueChange = {
                            customDeepLink = it
                            // Parse on change to show parsed properties
                            lastParsedDeepLink = runCatching { DeepLink.parse(it) }.getOrNull()
                        },
                        label = { Text("Deep Link URI") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Show parsed deep link properties
                    lastParsedDeepLink?.let { parsed ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Parsed DeepLink:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Text(
                                    "scheme: ${parsed.scheme}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "path: ${parsed.path}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (parsed.queryParams.isNotEmpty()) {
                                    Text(
                                        "queryParams: ${parsed.queryParams}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onNavigateViaDeepLink(customDeepLink) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Navigate")
                    }
                }
            }
        }

        item {
            Text(
                "Try These Deep Links",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // Home deep link
        item {
            DeepLinkCard(
                title = "Navigate to Home",
                deepLink = "app://main/home",
                description = "Simple deep link to home screen",
                onClick = { onNavigateViaDeepLink("app://main/home") }
            )
        }

        // Master-Detail with parameter
        item {
            DeepLinkCard(
                title = "Navigate to Item Detail",
                deepLink = "app://master_detail/detail/42",
                description = "Deep link with path parameter (item ID: 42)",
                onClick = { onNavigateViaDeepLink("app://master_detail/detail/42") }
            )
        }

        // Process flow
        item {
            DeepLinkCard(
                title = "Start Process Flow",
                deepLink = "app://process/start",
                description = "Deep link to wizard process",
                onClick = { onNavigateViaDeepLink("app://process/start") }
            )
        }

        // Tabs navigation
        item {
            DeepLinkCard(
                title = "Open Tabs Example",
                deepLink = "app://demo/tabs",
                description = "Deep link to tabs navigation",
                onClick = { onNavigateViaDeepLink("app://demo/tabs") }
            )
        }

        // Deep link with query parameters
        item {
            DeepLinkCard(
                title = "Item with Query Parameters",
                deepLink = "app://demo/item/99?source=deeplink&ref=email",
                description = "Deep link with query parameters for analytics",
                onClick = { onNavigateViaDeepLink("app://demo/item/99?source=deeplink&ref=email") }
            )
        }

        // Search results with typed query params
        item {
            DeepLinkCard(
                title = "Search with Query Params",
                deepLink = "app://search/results?query=kotlin&page=2&sortAsc=true",
                description = "Deep link with typed params: query (String), page (Int), sortAsc (Boolean)",
                onClick = { onNavigateViaDeepLink("app://search/results?query=kotlin&page=2&sortAsc=true") }
            )
        }

        // Runtime-registered promo deep link
        item {
            DeepLinkCard(
                title = "Promo Code (Runtime Registered)",
                deepLink = "app://promo/SAVE20",
                description = "This pattern was registered at runtime via getDeepLinkRegistry().register()",
                onClick = { onNavigateViaDeepLink("app://promo/SAVE20") }
            )
        }

        item {
            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null)
                        Text(
                            "How It Works",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        "• Deep links use URI patterns (app://...)\n" +
                                "• Parameters can be in the path ({id}) or query string (?key=value)\n" +
                                "• The DeepLinkRegistry resolves URIs to destinations\n" +
                                "• Supports pattern matching with path placeholders\n" +
                                "• Type conversions: String, Int, Long, Float, Boolean\n" +
                                "• @Argument(optional = true) for query params with defaults\n" +
                                "• Runtime registration via getDeepLinkRegistry().register()",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // New API Code Examples
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Code, null)
                        Text(
                            "New API Examples",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "// Simplified navigation\n" +
                                    "navigator.handleDeepLink(uri)\n\n" +
                                    "// Runtime registration\n" +
                                    "navigator.getDeepLinkRegistry()\n" +
                                    "    .register(\"promo/{code}\") { params ->\n" +
                                    "        PromoDestination(params[\"code\"]!!)\n" +
                                    "    }\n\n" +
                                    "// Destination with typed params\n" +
                                    "@Destination(route = \"search/results\")\n" +
                                    "data class SearchResults(\n" +
                                    "    @Argument val query: String,\n" +
                                    "    @Argument(optional = true) val page: Int = 1\n" +
                                    ") : SearchDestination()",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

