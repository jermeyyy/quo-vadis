package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.screens.explore.ExploreCategory
import kotlinx.coroutines.delay

private const val ICON_SIZE = 80
private const val ANIMATION_DELAY_MS = 100L

/**
 * Empty state component displayed when no explore items match the current filters.
 *
 * Shows an animated illustration with context-aware messaging based on active filters:
 * - Search query active: "No results for [query]"
 * - Category filter active: "No [category] items found"
 * - Both active: Combined message
 *
 * @param searchQuery Current search query text
 * @param selectedCategory Currently selected category filter
 * @param onClearFilters Callback to clear all active filters
 * @param modifier Modifier for the empty state container
 */
@Composable
fun ExploreEmptyState(
    searchQuery: String,
    selectedCategory: ExploreCategory,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(ANIMATION_DELAY_MS)
        isVisible = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "emptyStateScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "emptyStateAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .graphicsLayer {
                this.scaleX = scale
                this.scaleY = scale
                this.alpha = alpha
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        EmptyStateIcon(
            searchQuery = searchQuery,
            selectedCategory = selectedCategory
        )

        Spacer(modifier = Modifier.height(24.dp))

        EmptyStateTitle(
            searchQuery = searchQuery,
            selectedCategory = selectedCategory
        )

        Spacer(modifier = Modifier.height(8.dp))

        EmptyStateSubtitle(
            searchQuery = searchQuery,
            selectedCategory = selectedCategory
        )

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(
            visible = hasActiveFilters(searchQuery, selectedCategory),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Button(onClick = onClearFilters) {
                Text("Clear Filters")
            }
        }
    }
}

@Composable
private fun EmptyStateIcon(
    searchQuery: String,
    selectedCategory: ExploreCategory
) {
    val icon: ImageVector = when {
        searchQuery.isNotEmpty() -> Icons.Default.SearchOff
        selectedCategory != ExploreCategory.ALL -> selectedCategory.icon
        else -> Icons.Default.Search
    }

    val tint = when {
        searchQuery.isNotEmpty() -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
        selectedCategory != ExploreCategory.ALL -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(ICON_SIZE.dp),
        tint = tint
    )
}

@Composable
private fun EmptyStateTitle(
    searchQuery: String,
    selectedCategory: ExploreCategory
) {
    val title = when {
        searchQuery.isNotEmpty() && selectedCategory != ExploreCategory.ALL ->
            "No results found"
        searchQuery.isNotEmpty() ->
            "No results for \"$searchQuery\""
        selectedCategory != ExploreCategory.ALL ->
            "No ${selectedCategory.displayName} items"
        else ->
            "No items found"
    }

    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun EmptyStateSubtitle(
    searchQuery: String,
    selectedCategory: ExploreCategory
) {
    val subtitle = when {
        searchQuery.isNotEmpty() && selectedCategory != ExploreCategory.ALL ->
            "Try a different search term or category"
        searchQuery.isNotEmpty() ->
            "Try different keywords or check your spelling"
        selectedCategory != ExploreCategory.ALL ->
            "Try selecting a different category"
        else ->
            "Check back later for new content"
    }

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

private fun hasActiveFilters(
    searchQuery: String,
    selectedCategory: ExploreCategory
): Boolean = searchQuery.isNotEmpty() || selectedCategory != ExploreCategory.ALL
