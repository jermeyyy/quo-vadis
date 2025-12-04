package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.jermey.quo.vadis.core.navigation.core.BackStackEntry
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TabDefinition
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorConfig
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * Remember a [TabNavigatorState] that survives configuration changes.
 *
 * This function creates and remembers a tab navigation state that:
 * - Persists across configuration changes (rotation, etc.)
 * - Restores the selected tab
 * - Maintains individual tab navigation stacks
 * - Survives process death (when possible)
 *
 * @param config The tab navigator configuration defining tabs and behavior.
 * @return A [TabNavigatorState] instance bound to the current composition.
 *
 * @sample
 * ```kotlin
 * val tabState = rememberTabNavigatorState(
 *     TabNavigatorConfig(
 *         allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
 *         initialTab = HomeTab,
 *         primaryTab = HomeTab
 *     )
 * )
 * ```
 *
 * @deprecated Use [rememberTabNavigator] instead, which provides proper integration
 *             with parent navigator for back press handling and state restoration
 *             via parent entry extras. This legacy function does not support state
 *             restoration during predictive back gestures.
 */
@Deprecated(
    message = "Use rememberTabNavigator for proper back press handling and state restoration",
    replaceWith = ReplaceWith("rememberTabNavigator(config, parentNavigator, parentEntry)")
)
@Composable
fun rememberTabNavigatorState(
    config: TabNavigatorConfig
): TabNavigatorState {
    return rememberSaveable(
        inputs = arrayOf(config),
        saver = tabNavigatorStateSaver(config)
    ) {
        TabNavigatorState(config)
    }
}

/**
 * Saver for [TabNavigatorState] that preserves:
 * - Currently selected tab
 * - Navigation stack for each tab (when serializable)
 *
 * Note: Individual tab navigation stacks are saved through their respective navigators.
 * This saver primarily handles tab selection state.
 */
private fun tabNavigatorStateSaver(
    config: TabNavigatorConfig
): Saver<TabNavigatorState, List<Any>> {
    return Saver(
        save = { state ->
            // Save minimal state: selected tab route
            listOf(state.selectedTab.value.route)
        },
        restore = { saved ->
            val selectedTabRoute = saved[0] as String
            val selectedTab = config.allTabs.first { it.route == selectedTabRoute }
            
            // Create new state with saved selection
            TabNavigatorState(config).apply {
                selectTab(selectedTab)
            }
        }
    )
}

/**
 * Remember a tab navigator that integrates with a parent [Navigator].
 *
 * This function:
 * - Creates a tab navigation state
 * - Registers it with the parent navigator for back press handling
 * - Automatically cleans up on disposal
 * - Optionally syncs state with the parent back stack entry for state restoration
 *
 * @param config The tab navigator configuration.
 * @param parentNavigator The parent navigator for back press delegation.
 * @param parentEntry Optional parent back stack entry. When provided, the tab selection
 *                    state will be persisted to and restored from the entry's extras.
 *                    This enables proper state restoration during predictive back gestures
 *                    and when navigating back to this screen. The state is keyed on
 *                    `parentEntry.id` so a new state is created when the entry changes.
 * @return A [TabNavigatorState] instance integrated with the parent.
 *
 * @sample
 * ```kotlin
 * val tabState = rememberTabNavigator(
 *     config = TabNavigatorConfig(...),
 *     parentNavigator = navigator,
 *     parentEntry = entry  // Optional: for state sync
 * )
 * ```
 */
@Composable
fun rememberTabNavigator(
    config: TabNavigatorConfig,
    parentNavigator: Navigator,
    parentEntry: BackStackEntry? = null
): TabNavigatorState {
    // Use entry-based state if available, key on entry.id for proper recreation
    val tabState = remember(config, parentEntry?.id) {
        TabNavigatorState(config, parentEntry)
    }
    
    // Register with parent for back press delegation
    DisposableEffect(tabState, parentNavigator) {
        parentNavigator.setActiveChild(tabState)
        
        onDispose {
            // Clean up: unregister from parent
            parentNavigator.setActiveChild(null)
        }
    }
    
    return tabState
}
