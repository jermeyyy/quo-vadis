package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.screens.explore.ExploreItem

private const val GRID_COLUMNS = 2
private const val GRID_SPACING = 12

/**
 * A grid layout for explore items.
 *
 * Features:
 * - 2-column LazyVerticalGrid layout
 * - Loading state indicator
 * - Empty state when no items match filters
 *
 * @param items List of explore items to display
 * @param onItemClick Callback when an item is tapped
 * @param onItemLongClick Callback when an item is long-pressed
 * @param modifier Modifier for the grid container
 * @param contentPadding Padding for the grid content
 * @param isLoading Whether data is currently loading
 * @param emptyContent Content to show when items list is empty
 */
@Composable
fun ExploreGrid(
    items: List<ExploreItem>,
    onItemClick: (ExploreItem) -> Unit,
    onItemLongClick: (ExploreItem) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    gridState: LazyGridState = rememberLazyGridState(),
    isLoading: Boolean = false,
    emptyContent: @Composable () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            isLoading -> LoadingState()
            items.isEmpty() -> emptyContent()
            else -> GridContent(
                items = items,
                onItemClick = onItemClick,
                onItemLongClick = onItemLongClick,
                contentPadding = contentPadding,
                gridState = gridState
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun GridContent(
    items: List<ExploreItem>,
    onItemClick: (ExploreItem) -> Unit,
    onItemLongClick: (ExploreItem) -> Unit,
    contentPadding: PaddingValues,
    gridState: LazyGridState
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        state = gridState,
        horizontalArrangement = Arrangement.spacedBy(GRID_SPACING.dp),
        verticalArrangement = Arrangement.spacedBy(GRID_SPACING.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { _, item ->
            ExploreCard(
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
