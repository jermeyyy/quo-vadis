package com.jermey.feature1

import com.jermey.navplayground.navigation.ResultDemoDestination
import com.jermey.navplayground.navigation.SelectedItem
import com.jermey.navplayground.navigation.ResultDemoFeatureEntry
import com.jermey.navplayground.navigation.SelectedItemResult
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.result.navigateForResult
import org.koin.core.annotation.Single

/**
 * Concrete implementation of [ResultDemoFeatureEntry].
 * Maps internal [SelectedItem] to public [SelectedItemResult].
 */
@Single(binds = [ResultDemoFeatureEntry::class])
class ResultDemoFeatureEntryImpl(
    private val navigator: Navigator
) : ResultDemoFeatureEntry {
    override suspend fun start(): SelectedItemResult? {
        val result: SelectedItem? = navigator.navigateForResult(
            ResultDemoDestination.ItemPicker
        )
        return result?.let { SelectedItemResult(id = it.id, name = it.name) }
    }
}
