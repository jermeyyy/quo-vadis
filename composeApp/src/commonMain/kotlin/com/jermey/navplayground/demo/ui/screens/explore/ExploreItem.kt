package com.jermey.navplayground.demo.ui.screens.explore

/**
 * Data model for explore feed items.
 *
 * @property id Unique identifier
 * @property title Display title
 * @property subtitle Secondary text
 * @property category Item category
 * @property imageUrl URL to picsum.photos image
 * @property rating Rating (0.0 - 5.0)
 * @property isFeatured Whether item should be featured/highlighted
 */
data class ExploreItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: ExploreCategory,
    val imageUrl: String,
    val rating: Float,
    val isFeatured: Boolean = false
) {
    companion object {
        private const val STANDARD_WIDTH = 800
        private const val STANDARD_HEIGHT = 600

        /**
         * Generate deterministic picsum URL based on item ID.
         *
         * Uses standardized 800×600 resolution for optimal cache hits across
         * all usages (card, detail, preview).
         */
        fun imageUrlForId(id: String): String =
            "https://picsum.photos/seed/$id/$STANDARD_WIDTH/$STANDARD_HEIGHT"
    }
}
