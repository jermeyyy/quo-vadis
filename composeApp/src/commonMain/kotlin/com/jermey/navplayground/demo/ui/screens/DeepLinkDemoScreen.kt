package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.DeepLinkCard

/**
 * DeepLink Demo Screen - Shows deep linking functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepLinkDemoScreen(
    onBack: () -> Unit,
    onNavigateViaDeepLink: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deep Link Demo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
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
                    deepLink = "app://demo/home",
                    description = "Simple deep link to home screen",
                    onClick = { onNavigateViaDeepLink("app://demo/home") }
                )
            }

            // Master-Detail with parameter
            item {
                DeepLinkCard(
                    title = "Navigate to Item Detail",
                    deepLink = "app://demo/item/42",
                    description = "Deep link with path parameter (item ID: 42)",
                    onClick = { onNavigateViaDeepLink("app://demo/item/42") }
                )
            }

            // Process flow
            item {
                DeepLinkCard(
                    title = "Start Process Flow",
                    deepLink = "app://demo/process/start",
                    description = "Deep link to wizard process",
                    onClick = { onNavigateViaDeepLink("app://demo/process/start") }
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
                                    "• The DeepLinkHandler resolves URIs to destinations\n" +
                                    "• Supports pattern matching with wildcards\n" +
                                    "• Can handle universal links from web (https://...)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

