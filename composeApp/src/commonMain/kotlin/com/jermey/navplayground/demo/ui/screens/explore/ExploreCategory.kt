package com.jermey.navplayground.demo.ui.screens.explore

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Categories for explore items with display names and icons.
 */
enum class ExploreCategory(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.Apps),
    TECHNOLOGY("Technology", Icons.Default.Computer),
    DESIGN("Design", Icons.Default.Palette),
    TRAVEL("Travel", Icons.Default.Flight),
    FOOD("Food", Icons.Default.Restaurant),
    NATURE("Nature", Icons.Default.Park),
    MUSIC("Music", Icons.Default.MusicNote),
    SPORTS("Sports", Icons.Default.SportsBaseball)
}

/**
 * Sort order options for explore items.
 */
enum class SortOrder(val displayName: String) {
    RATING_DESC("Rating (High to Low)"),
    RATING_ASC("Rating (Low to High)"),
    TITLE_ASC("Title (A-Z)"),
    TITLE_DESC("Title (Z-A)")
}
