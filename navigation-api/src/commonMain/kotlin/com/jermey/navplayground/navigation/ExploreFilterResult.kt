package com.jermey.navplayground.navigation

/**
 * Result returned from the explore filters modal screen.
 *
 * Uses string representations of enum values for cross-module compatibility.
 * The [ExploreContainer] converts these back to the appropriate enum types.
 *
 * @property sortOrder String name of the selected [SortOrder] enum value
 * @property ratingFilter Minimum rating filter value (0-5)
 * @property category String name of the selected [ExploreCategory] enum value
 */
data class ExploreFilterResult(
    val sortOrder: String,
    val ratingFilter: Float,
    val category: String
)
