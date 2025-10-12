package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssistantDirection
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.NavigationPatternCard

/**
 * Home Screen - Main entry point with navigation to all patterns
 */
@Composable
fun HomeScreen(
    onNavigateToMasterDetail: () -> Unit,
    onNavigateToTabs: () -> Unit,
    onNavigateToProcess: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
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

        Spacer(Modifier.weight(1f))

        Text(
            "Use the bottom navigation to switch between main sections",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
