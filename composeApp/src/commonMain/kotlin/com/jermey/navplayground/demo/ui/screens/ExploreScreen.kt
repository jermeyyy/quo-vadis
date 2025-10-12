package com.jermey.navplayground.demo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val EXPLORE_ITEMS_COUNT = 20

/**
 * Explore Screen - Shows a grid/list of items
 */
@Composable
fun ExploreScreen(
    onItemClick: (String) -> Unit
) {
    val items = remember {
        (1..EXPLORE_ITEMS_COUNT).map { "Item $it" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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
