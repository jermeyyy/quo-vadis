package com.jermey.navplayground.demo.ui.screens.explore

import com.jermey.navplayground.demo.app.sample.showcase.destinations.veeeeery.looong.packages.names.length.test.destinations.MainTabs
import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import org.koin.core.annotation.Qualifier
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.enableLogging
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce

/**
 * Type alias for the store context to simplify handler signatures.
 */
private typealias Ctx = pro.respawn.flowmvi.api.PipelineContext<ExploreState, ExploreIntent, ExploreAction>

/**
 * Explore feature container with FlowMVI store.
 *
 * Demonstrates:
 * - Complex filtering and sorting logic
 * - Search functionality
 * - Category-based filtering
 * - Rating filter
 * - Navigation integration
 * - Preview modal state
 *
 * Uses [NavigationContainer] for proper lifecycle integration with the navigation system.
 */
@Scoped
@Scope(NavigationContainerScope::class)
@Qualifier(ExploreContainer::class)
class ExploreContainer(
    scope: NavigationContainerScope,
    repository: ExploreRepository,
    private val debuggable: Boolean = false
) : NavigationContainer<ExploreState, ExploreIntent, ExploreAction>(scope) {

    // Load data synchronously at construction time for immediate rendering
    private val initialState: ExploreState = run {
        val items = repository.getItems()
        val categories = repository.getCategories()
        ExploreState.Content(
            items = items,
            filteredItems = items.sortedByDescending { it.rating },
            categories = categories,
            selectedCategory = ExploreCategory.ALL,
            searchQuery = "",
            sortOrder = SortOrder.RATING_DESC,
            ratingFilter = 0f,
            isSearchFocused = false,
            isFilterSheetVisible = false,
            previewItem = null
        )
    }

    override val store = store(initial = initialState) {
        configure {
            debuggable = this@ExploreContainer.debuggable
            name = "ExploreStore"
            parallelIntents = true // Allow parallel filtering operations
        }

        // Reduce: handle all intents
        reduce { intent ->
            when (intent) {
                is ExploreIntent.LoadItems -> { /* Data loaded at construction, no-op */ }
                is ExploreIntent.Search -> handleSearch(intent.query)
                is ExploreIntent.SelectCategory -> handleSelectCategory(intent.category)
                is ExploreIntent.SetRatingFilter -> handleSetRatingFilter(intent.minRating)
                is ExploreIntent.SetSortOrder -> handleSetSortOrder(intent.order)
                is ExploreIntent.ToggleFilterSheet -> handleToggleFilterSheet()
                is ExploreIntent.ShowPreview -> handleShowPreview(intent.item)
                is ExploreIntent.DismissPreview -> handleDismissPreview()
                is ExploreIntent.NavigateToDetail -> handleNavigateToDetail(intent.item)
                is ExploreIntent.ClearFilters -> handleClearFilters()
                is ExploreIntent.SetSearchFocus -> handleSetSearchFocus(intent.focused)
            }
        }

        // Recover: handle errors gracefully
        recover { exception ->
            action(ExploreAction.ShowError(exception.message ?: "Unknown error"))
            null // Suppress exception
        }

        // Enable logging for debugging
        if (debuggable) {
            enableLogging()
        }
    }

    /**
     * Handle search query changes.
     */
    private suspend fun Ctx.handleSearch(query: String) {
        withContentState { content ->
            val updated = content.copy(searchQuery = query)
            updateState { applyFiltersAndSort(updated) }
            action(ExploreAction.ScrollToTop)
        }
    }

    /**
     * Handle category selection.
     */
    private suspend fun Ctx.handleSelectCategory(category: ExploreCategory) {
        withContentState { content ->
            val updated = content.copy(selectedCategory = category)
            updateState { applyFiltersAndSort(updated) }
            action(ExploreAction.ScrollToTop)
        }
    }

    /**
     * Handle rating filter changes.
     */
    private suspend fun Ctx.handleSetRatingFilter(minRating: Float) {
        withContentState { content ->
            val updated = content.copy(ratingFilter = minRating)
            updateState { applyFiltersAndSort(updated) }
        }
    }

    /**
     * Handle sort order changes.
     */
    private suspend fun Ctx.handleSetSortOrder(order: SortOrder) {
        withContentState { content ->
            val updated = content.copy(sortOrder = order)
            updateState { applyFiltersAndSort(updated) }
        }
    }

    /**
     * Toggle filter sheet visibility.
     */
    private suspend fun Ctx.handleToggleFilterSheet() {
        withContentState { content ->
            updateState { content.copy(isFilterSheetVisible = !content.isFilterSheetVisible) }
        }
    }

    /**
     * Show preview for an item.
     */
    private suspend fun Ctx.handleShowPreview(item: ExploreItem) {
        withContentState { content ->
            updateState { content.copy(previewItem = item) }
        }
    }

    /**
     * Dismiss preview sheet.
     */
    private suspend fun Ctx.handleDismissPreview() {
        withContentState { content ->
            updateState { content.copy(previewItem = null) }
        }
    }

    /**
     * Navigate to item detail.
     */
    private suspend fun Ctx.handleNavigateToDetail(item: ExploreItem) {
        try {
            navigator.navigate(MainTabs.ExploreTab.Detail(itemId = item.id))
        } catch (e: Exception) {
            action(ExploreAction.ShowError("Navigation failed"))
        }
    }

    /**
     * Clear all filters.
     */
    private suspend fun Ctx.handleClearFilters() {
        withContentState { content ->
            val cleared = content.copy(
                selectedCategory = ExploreCategory.ALL,
                searchQuery = "",
                ratingFilter = 0f,
                sortOrder = SortOrder.RATING_DESC
            )
            updateState { applyFiltersAndSort(cleared) }
            action(ExploreAction.ScrollToTop)
        }
    }

    /**
     * Set search focus state.
     */
    private suspend fun Ctx.handleSetSearchFocus(focused: Boolean) {
        withContentState { content ->
            updateState { content.copy(isSearchFocused = focused) }
        }
    }

    /**
     * Apply all filters and sorting to the items list.
     */
    private fun applyFiltersAndSort(content: ExploreState.Content): ExploreState.Content {
        var filtered = content.items

        // Apply category filter
        if (content.selectedCategory != ExploreCategory.ALL) {
            filtered = filtered.filter { it.category == content.selectedCategory }
        }

        // Apply search query filter
        if (content.searchQuery.isNotEmpty()) {
            val query = content.searchQuery.lowercase()
            filtered = filtered.filter {
                it.title.lowercase().contains(query) ||
                        it.subtitle.lowercase().contains(query) ||
                        it.category.displayName.lowercase().contains(query)
            }
        }

        // Apply rating filter
        if (content.ratingFilter > 0f) {
            filtered = filtered.filter { it.rating >= content.ratingFilter }
        }

        // Apply sorting
        filtered = when (content.sortOrder) {
            SortOrder.RATING_DESC -> filtered.sortedByDescending { it.rating }
            SortOrder.RATING_ASC -> filtered.sortedBy { it.rating }
            SortOrder.TITLE_ASC -> filtered.sortedBy { it.title }
            SortOrder.TITLE_DESC -> filtered.sortedByDescending { it.title }
        }

        return content.copy(filteredItems = filtered)
    }

    /**
     * Helper to safely access Content state.
     */
    private suspend inline fun Ctx.withContentState(crossinline block: suspend (ExploreState.Content) -> Unit) {
        withState {
            if (this is ExploreState.Content) {
                block(this)
            }
        }
    }
}
