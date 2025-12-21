package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Code
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
import com.jermey.navplayground.demo.destinations.PromoDestination
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.core.Navigator
import org.koin.compose.koinInject

/**
 * Promo Screen - Demonstrates runtime deep link registration
 *
 * This screen is navigated to via a deep link that was registered at RUNTIME,
 * not at compile time. The registration happens in DeepLinkDemoScreen:
 *
 * ```kotlin
 * LaunchedEffect(Unit) {
 *     navigator.getDeepLinkRegistry().register("promo/{code}") { params ->
 *         PromoDestination(code = params["code"]!!)
 *     }
 * }
 * ```
 *
 * This demonstrates:
 * - Runtime deep link registration via `DeepLinkRegistry.register()`
 * - Dynamic deep link patterns for marketing campaigns
 * - No @Destination(route=...) needed in the destination class
 */
@Screen(PromoDestination::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoScreen(
    destination: PromoDestination,
    navigator: Navigator = koinInject()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promo Code") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Promo card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.CardGiftcard,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "Promo Code Applied!",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            destination.code,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // How it works card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                            "Runtime Deep Link Registration",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Text(
                        "This destination was navigated to via a deep link pattern " +
                            "registered at RUNTIME, not at compile time.\n\n" +
                            "This is useful for:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "• Marketing campaigns with dynamic promo codes",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• A/B testing different deep link patterns",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Feature flags that enable/disable deep links",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "• Plugin systems with dynamic navigation",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Code example
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
                        "Registration Code",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "// In DeepLinkDemoScreen\n" +
                                "LaunchedEffect(Unit) {\n" +
                                "    navigator.getDeepLinkRegistry()\n" +
                                "        .register(\"promo/{code}\") { params ->\n" +
                                "            PromoDestination(\n" +
                                "                code = params[\"code\"]!!\n" +
                                "            )\n" +
                                "        }\n" +
                                "}",
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
