package com.jermey.navplayground.demo.ui.screens.explore

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * MVI State for Explore feature.
 *
 * Demonstrates complex state management patterns:
 * - Loading states (Loading, Content, Error)
 * - Search and filter state
 * - Category selection
 * - Preview modal state
 */
sealed interface ExploreState : MVIState {
    /**
     * Initial loading state.
     */
    data object Loading : ExploreState

    /**
     * Content state with explore items and filter options.
     */
    data class Content(
        val items: List<ExploreItem>,
        val filteredItems: List<ExploreItem>,
        val categories: List<ExploreCategory>,
        val selectedCategory: ExploreCategory,
        val searchQuery: String,
        val sortOrder: SortOrder,
        val ratingFilter: Float,
        val isSearchFocused: Boolean,
        val isFilterSheetVisible: Boolean,
        val previewItem: ExploreItem?
    ) : ExploreState {
        /**
         * Whether any filters are currently applied.
         */
        val isFiltered: Boolean
            get() = searchQuery.isNotEmpty() ||
                    selectedCategory != ExploreCategory.ALL ||
                    ratingFilter > 0f
    }

    /**
     * Error state with retry option.
     */
    data class Error(val message: String) : ExploreState
}

/**
 * MVI Intents for Explore feature.
 *
 * Demonstrates various intent types:
 * - Data loading
 * - Search and filtering
 * - Category selection
 * - Navigation
 * - UI state changes
 */
sealed interface ExploreIntent : MVIIntent {
    /**
     * Load explore items (initial or refresh).
     */
    data object LoadItems : ExploreIntent

    /**
     * Search for items by query.
     */
    data class Search(val query: String) : ExploreIntent

    /**
     * Select a category filter.
     */
    data class SelectCategory(val category: ExploreCategory) : ExploreIntent

    /**
     * Set minimum rating filter.
     */
    data class SetRatingFilter(val minRating: Float) : ExploreIntent

    /**
     * Set sort order for items.
     */
    data class SetSortOrder(val order: SortOrder) : ExploreIntent

    /**
     * Toggle filter bottom sheet visibility.
     */
    data object ToggleFilterSheet : ExploreIntent

    /**
     * Show quick preview for an item.
     */
    data class ShowPreview(val item: ExploreItem) : ExploreIntent

    /**
     * Dismiss the quick preview sheet.
     */
    data object DismissPreview : ExploreIntent

    /**
     * Navigate to item detail screen.
     */
    data class NavigateToDetail(val item: ExploreItem) : ExploreIntent

    /**
     * Clear all applied filters.
     */
    data object ClearFilters : ExploreIntent

    /**
     * Update search focus state.
     */
    data class SetSearchFocus(val focused: Boolean) : ExploreIntent
}

/**
 * MVI Side Effects for Explore feature.
 *
 * Demonstrates various action types:
 * - User feedback (errors)
 * - Navigation events
 * - UI commands
 */
sealed interface ExploreAction : MVIAction {
    /**
     * Show an error message.
     */
    data class ShowError(val message: String) : ExploreAction

    /**
     * Navigate to detail screen.
     */
    data class NavigateToDetail(val itemId: String) : ExploreAction

    /**
     * Scroll the list to top.
     */
    data object ScrollToTop : ExploreAction
}
