package com.jermey.navplayground.demo.ui.components.explore

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jermey.navplayground.demo.ui.screens.explore.ExploreCategory

private const val SELECTED_SCALE = 1.05f
private const val UNSELECTED_SCALE = 1.0f

/**
 * A horizontally scrollable row of category filter chips with selection animations.
 *
 * @param categories List of available categories
 * @param selectedCategory Currently selected category
 * @param onCategorySelected Callback when a category is tapped
 * @param modifier Modifier for the row container
 */
@Composable
fun CategoryChipsRow(
    categories: List<ExploreCategory>,
    selectedCategory: ExploreCategory,
    onCategorySelected: (ExploreCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            CategoryChip(category, category == selectedCategory) { onCategorySelected(category) }
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

/** Individual category chip with animated selection state. */
@Composable
private fun CategoryChip(
    category: ExploreCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) SELECTED_SCALE else UNSELECTED_SCALE,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label = "chipScale"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipContainerColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipContentColor"
    )

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(category.displayName, style = MaterialTheme.typography.labelLarge, color = contentColor) },
        leadingIcon = { Icon(category.icon, null, Modifier.size(18.dp), contentColor) },
        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(20.dp),
        colors = chipColors(containerColor, contentColor),
        border = chipBorder(isSelected)
    )
}

@Composable
private fun chipColors(containerColor: Color, contentColor: Color) = FilterChipDefaults.filterChipColors(
    containerColor = containerColor,
    selectedContainerColor = containerColor,
    labelColor = contentColor,
    selectedLabelColor = contentColor,
    iconColor = contentColor,
    selectedLeadingIconColor = contentColor
)

@Composable
private fun chipBorder(isSelected: Boolean) = FilterChipDefaults.filterChipBorder(
    borderColor = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
    selectedBorderColor = Color.Transparent,
    enabled = true,
    selected = isSelected
)
