package com.jermey.feature1.explore

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jermey.feature1.explore.components.FilterSheetContent
import com.jermey.feature1.ui.glassmorphism.GlassBottomSheet
import com.jermey.navplayground.navigation.ExploreFilterResult
import com.jermey.navplayground.navigation.ExploreTab
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.result.navigateBackWithResult
import dev.chrisbanes.haze.HazeState
import org.koin.compose.koinInject

/**
 * Modal screen for explore filters.
 *
 * Uses `@Modal` annotation — the library renders the explore screen underneath,
 * and this composable draws the filter sheet on top via [GlassBottomSheet].
 *
 * Filter values are initialized from the destination arguments (current state)
 * and returned as [ExploreFilterResult] when applied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Screen(ExploreTab.Filters::class)
@Composable
fun ExploreFiltersScreen(
    destination: ExploreTab.Filters,
    navigator: Navigator = koinInject(),
) {
    val hazeState = remember { HazeState() }
    val repository: ExploreRepository = koinInject()

    val categories = remember { repository.getCategories() }
    val allItems = remember { repository.getItems() }

    // Local filter state initialized from destination arguments
    var sortOrder by remember {
        mutableStateOf(
            SortOrder.entries.find { it.name == destination.sortOrder } ?: SortOrder.RATING_DESC
        )
    }
    var ratingFilter by remember {
        mutableFloatStateOf(destination.ratingFilter.toFloatOrNull() ?: 0f)
    }
    var selectedCategory by remember {
        mutableStateOf(
            ExploreCategory.entries.find { it.name == destination.category } ?: ExploreCategory.ALL
        )
    }

    // Compute result count based on current local filter state
    val resultCount = remember(sortOrder, ratingFilter, selectedCategory) {
        var filtered = allItems
        if (selectedCategory != ExploreCategory.ALL) {
            filtered = filtered.filter { it.category == selectedCategory }
        }
        if (ratingFilter > 0f) {
            filtered = filtered.filter { it.rating >= ratingFilter }
        }
        filtered.size
    }

    GlassBottomSheet(
        onDismissRequest = { navigator.navigateBack() },
        hazeState = hazeState
    ) {
        FilterSheetContent(
            currentSortOrder = sortOrder,
            currentRatingFilter = ratingFilter,
            selectedCategory = selectedCategory,
            categories = categories,
            resultCount = resultCount,
            onSortOrderChange = { sortOrder = it },
            onRatingFilterChange = { ratingFilter = it },
            onCategoryChange = { selectedCategory = it },
            onApply = {
                navigator.navigateBackWithResult(
                    ExploreFilterResult(
                        sortOrder = sortOrder.name,
                        ratingFilter = ratingFilter,
                        category = selectedCategory.name
                    )
                )
            },
            onReset = {
                sortOrder = SortOrder.RATING_DESC
                ratingFilter = 0f
                selectedCategory = ExploreCategory.ALL
            }
        )
    }
}
