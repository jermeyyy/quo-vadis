package com.jermey.navplayground.demo.ui.screens.statedriven

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Content screens for the State-Driven Navigation Demo.
 *
 * Each screen represents a destination and displays relevant information
 * about itself, including any parameters passed to it.
 */

/**
 * Home content screen - the default starting destination.
 */
@Composable
fun HomeContent(
    modifier: Modifier = Modifier
) {
    DestinationContent(
        modifier = modifier,
        icon = Icons.Default.Home,
        title = "Home",
        subtitle = "Welcome to the state-driven navigation demo!",
        description = "This is the home destination. Use the backstack editor panel " +
                "to add, remove, or reorder destinations in the navigation stack.",
        accentColor = MaterialTheme.colorScheme.primary
    ) {
        // Additional home-specific content
        InfoCard(
            title = "State-Driven Navigation",
            items = listOf(
                "Direct control over the backstack",
                "No NavController required",
                "Compose's snapshot state system",
                "Navigation 3-style API"
            )
        )
    }
}

/**
 * Profile content screen - displays user profile with userId parameter.
 */
@Composable
fun ProfileContent(
    userId: String,
    modifier: Modifier = Modifier
) {
    DestinationContent(
        modifier = modifier,
        icon = Icons.Default.Person,
        title = "Profile",
        subtitle = "User: $userId",
        description = "Viewing profile for user '$userId'. This demonstrates " +
                "a destination with a parameter passed through the backstack.",
        accentColor = MaterialTheme.colorScheme.tertiary
    ) {
        ParameterCard(
            parameterName = "userId",
            parameterValue = userId
        )
    }
}

/**
 * Settings content screen - app settings.
 */
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier
) {
    DestinationContent(
        modifier = modifier,
        icon = Icons.Default.Settings,
        title = "Settings",
        subtitle = "Application Settings",
        description = "This is the settings destination. It's an object destination " +
                "with no parameters - simple and straightforward.",
        accentColor = MaterialTheme.colorScheme.secondary
    ) {
        InfoCard(
            title = "Object Destinations",
            items = listOf(
                "No parameters required",
                "Singleton-like behavior",
                "Simple navigation targets",
                "Great for static screens"
            )
        )
    }
}

/**
 * Detail content screen - displays item details with itemId parameter.
 */
@Composable
fun DetailContent(
    itemId: String,
    modifier: Modifier = Modifier
) {
    DestinationContent(
        modifier = modifier,
        icon = Icons.Default.Info,
        title = "Detail",
        subtitle = "Item: $itemId",
        description = "Viewing details for item '$itemId'. Another example of " +
                "a parameterized destination in state-driven navigation.",
        accentColor = MaterialTheme.colorScheme.error
    ) {
        ParameterCard(
            parameterName = "itemId",
            parameterValue = itemId
        )
    }
}

/**
 * Reusable base layout for destination content screens.
 */
@Composable
private fun DestinationContent(
    icon: ImageVector,
    title: String,
    subtitle: String,
    description: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    extraContent: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Icon with colored background
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = accentColor
            )
        }

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Subtitle
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor
        )

        Spacer(Modifier.height(8.dp))

        // Description
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Extra content
        extraContent()

        Spacer(Modifier.weight(1f))
    }
}

/**
 * Card displaying a parameter and its value.
 */
@Composable
private fun ParameterCard(
    parameterName: String,
    parameterValue: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Parameter",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = parameterName,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Value",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = parameterValue,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Card displaying informational bullet points.
 */
@Composable
private fun InfoCard(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
