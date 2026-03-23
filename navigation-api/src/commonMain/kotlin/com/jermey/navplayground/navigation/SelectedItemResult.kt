package com.jermey.navplayground.navigation

/**
 * Result returned by the Result Demo feature's item picker.
 *
 * Defined in `navigation-api` so consuming modules can use the result type
 * without depending on feature1.
 *
 * @property id Unique identifier of the selected item
 * @property name Display name of the selected item
 */
data class SelectedItemResult(
    val id: String,
    val name: String
)
