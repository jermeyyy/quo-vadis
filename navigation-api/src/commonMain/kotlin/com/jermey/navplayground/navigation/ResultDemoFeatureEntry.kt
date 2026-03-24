package com.jermey.navplayground.navigation

/**
 * Entry point for the Result Demo feature (feature1).
 *
 * Consuming modules inject this interface via Koin and call [start]
 * to navigate into the Result Demo and await a [SelectedItemResult].
 */
interface ResultDemoFeatureEntry : FeatureEntry<SelectedItemResult>
