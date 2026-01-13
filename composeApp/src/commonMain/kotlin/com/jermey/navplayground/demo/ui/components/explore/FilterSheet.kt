package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.components.glassmorphism.GlassBottomSheet
import com.jermey.navplayground.demo.ui.screens.explore.ExploreCategory
import com.jermey.navplayground.demo.ui.screens.explore.SortOrder
import dev.chrisbanes.haze.HazeState

private const val CORNER_RADIUS = 16
private const val MAX_RATING_FILTER = 5f
private const val RATING_STEPS = 9

@Suppress("MagicNumber")
private val GOLD_COLOR = Color(0xFFFFD700)

/**
 * A filter bottom sheet with glassmorphic design for the Explore screen.
 *
 * Features:
 * - Sort order radio buttons
 * - Rating filter slider
 * - Category multi-select chips
 * - Apply button with result count
 * - Reset button to clear all filters
 *
 * @param currentSortOrder Currently selected sort order
 * @param currentRatingFilter Current minimum rating filter (0-5)
 * @param selectedCategory Currently selected category
 * @param categories Available categories to filter by
 * @param resultCount Number of items matching current filters
 * @param onSortOrderChange Callback when sort order changes
 * @param onRatingFilterChange Callback when rating filter changes
 * @param onCategoryChange Callback when category selection changes
 * @param onApply Callback when Apply button is clicked
 * @param onReset Callback when Reset button is clicked
 * @param onDismiss Callback when sheet is dismissed
 * @param hazeState The shared HazeState for glassmorphic effects
 * @param modifier Modifier for the sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    currentSortOrder: SortOrder,
    currentRatingFilter: Float,
    selectedCategory: ExploreCategory,
    categories: List<ExploreCategory>,
    resultCount: Int,
    onSortOrderChange: (SortOrder) -> Unit,
    onRatingFilterChange: (Float) -> Unit,
    onCategoryChange: (ExploreCategory) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    GlassBottomSheet(
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        sheetState = sheetState,
        modifier = modifier
    ) {
        FilterSheetContent(
            currentSortOrder = currentSortOrder,
            currentRatingFilter = currentRatingFilter,
            selectedCategory = selectedCategory,
            categories = categories,
            resultCount = resultCount,
            onSortOrderChange = onSortOrderChange,
            onRatingFilterChange = onRatingFilterChange,
            onCategoryChange = onCategoryChange,
            onApply = onApply,
            onReset = onReset
        )
    }
}

@Composable
private fun FilterSheetContent(
    currentSortOrder: SortOrder,
    currentRatingFilter: Float,
    selectedCategory: ExploreCategory,
    categories: List<ExploreCategory>,
    resultCount: Int,
    onSortOrderChange: (SortOrder) -> Unit,
    onRatingFilterChange: (Float) -> Unit,
    onCategoryChange: (ExploreCategory) -> Unit,
    onApply: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header with title and reset button
        FilterHeader(onReset = onReset)

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // Sort Order Section
        SortOrderSection(
            currentSortOrder = currentSortOrder,
            onSortOrderChange = onSortOrderChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Rating Filter Section
        RatingFilterSection(
            currentRating = currentRatingFilter,
            onRatingChange = onRatingFilterChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Category Section
        CategorySection(
            selectedCategory = selectedCategory,
            categories = categories,
            onCategoryChange = onCategoryChange
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Apply Button with result count
        ApplyButton(
            resultCount = resultCount,
            onApply = onApply
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FilterHeader(onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Filters",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        TextButton(onClick = onReset) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset")
        }
    }
}

@Composable
private fun SortOrderSection(
    currentSortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit
) {
    Column {
        Text(
            text = "Sort By",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(modifier = Modifier.selectableGroup()) {
            SortOrder.entries.forEach { sortOrder ->
                SortOrderOption(
                    sortOrder = sortOrder,
                    isSelected = currentSortOrder == sortOrder,
                    onSelect = { onSortOrderChange(sortOrder) }
                )
            }
        }
    }
}

@Composable
private fun SortOrderOption(
    sortOrder: SortOrder,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null // handled by row
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = sortOrder.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RatingFilterSection(
    currentRating: Float,
    onRatingChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Minimum Rating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = GOLD_COLOR
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatRatingValue(currentRating),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = currentRating,
            onValueChange = onRatingChange,
            valueRange = 0f..MAX_RATING_FILTER,
            steps = RATING_STEPS,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )

        // Rating labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Any",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "5.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    selectedCategory: ExploreCategory,
    categories: List<ExploreCategory>,
    onCategoryChange: (ExploreCategory) -> Unit
) {
    Column {
        Text(
            text = "Category",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                CategoryChip(
                    category = category,
                    isSelected = selectedCategory == category,
                    onSelect = { onCategoryChange(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: ExploreCategory,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = {
            Text(text = category.displayName)
        },
        leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            } else {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        }
    )
}

@Composable
private fun ApplyButton(
    resultCount: Int,
    onApply: () -> Unit
) {
    Button(
        onClick = onApply,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = if (resultCount > 0) {
                "Show $resultCount results"
            } else {
                "No results found"
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

/**
 * Formats rating value for display.
 */
private fun formatRatingValue(rating: Float): String {
    return if (rating == 0f) {
        "Any"
    } else {
        val rounded = (rating * 10).toInt() / 10.0
        if (rounded == rounded.toInt().toDouble()) {
            "${rounded.toInt()}.0+"
        } else {
            "$rounded+"
        }
    }
}
