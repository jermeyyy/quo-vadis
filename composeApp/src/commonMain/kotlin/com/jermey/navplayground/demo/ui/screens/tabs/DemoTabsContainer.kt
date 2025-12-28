package com.jermey.navplayground.demo.ui.screens.tabs

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.jermey.quo.vadis.flowmvi.SharedContainerScope
import com.jermey.quo.vadis.flowmvi.SharedNavigationContainer
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

/**
 * State shared across all tabs in DemoTabs.
 *
 * @property totalItemsViewed Count of items viewed across all tabs
 * @property favoriteItems List of favorited item IDs
 * @property notifications List of notification messages
 */
data class DemoTabsState(
    val totalItemsViewed: Int = 0,
    val favoriteItems: List<String> = emptyList(),
    val notifications: List<String> = emptyList()
) : MVIState

/**
 * Intents for the Demo Tabs shared container.
 */
sealed interface DemoTabsIntent : MVIIntent {
    /** Increment the total items viewed counter. */
    data object IncrementViewed : DemoTabsIntent

    /** Add an item to favorites. */
    data class AddFavorite(val itemId: String) : DemoTabsIntent

    /** Remove an item from favorites. */
    data class RemoveFavorite(val itemId: String) : DemoTabsIntent

    /** Add a notification message. */
    data class AddNotification(val message: String) : DemoTabsIntent

    /** Clear all notifications. */
    data object ClearNotifications : DemoTabsIntent
}

/**
 * Actions for the Demo Tabs shared container.
 * Currently empty, but can be extended for side effects.
 */
sealed interface DemoTabsAction : MVIAction

/**
 * Shared container for the Demo Tabs.
 *
 * Demonstrates:
 * - Cross-tab state sharing (items viewed, favorites)
 * - Actions accessible from any child screen
 * - State that persists across tab switches
 *
 * ## Usage
 *
 * In the tabs wrapper:
 * ```kotlin
 * @TabsContainer(DemoTabs.Companion::class)
 * @Composable
 * fun DemoTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
 *     val store = rememberSharedContainer<DemoTabsContainer, DemoTabsState, DemoTabsIntent, DemoTabsAction>()
 *     CompositionLocalProvider(LocalDemoTabsStore provides store) {
 *         // render content
 *     }
 * }
 * ```
 *
 * In child screens:
 * ```kotlin
 * val store = LocalDemoTabsStore.current
 * store?.intent(DemoTabsIntent.IncrementViewed)
 * ```
 *
 * @param scope The shared container scope providing access to navigator and lifecycle
 */
class DemoTabsContainer(
    scope: SharedContainerScope,
) : SharedNavigationContainer<DemoTabsState, DemoTabsIntent, DemoTabsAction>(scope) {

    override val store: Store<DemoTabsState, DemoTabsIntent, DemoTabsAction> =
        store(DemoTabsState()) {
            configure {
                name = "DemoTabsStore"
            }
            reduce { intent ->
                when (intent) {
                    is DemoTabsIntent.IncrementViewed -> updateState {
                        copy(totalItemsViewed = totalItemsViewed + 1)
                    }

                    is DemoTabsIntent.AddFavorite -> updateState {
                        if (intent.itemId !in favoriteItems) {
                            copy(favoriteItems = favoriteItems + intent.itemId)
                        } else this
                    }

                    is DemoTabsIntent.RemoveFavorite -> updateState {
                        copy(favoriteItems = favoriteItems - intent.itemId)
                    }

                    is DemoTabsIntent.AddNotification -> updateState {
                        copy(notifications = notifications + intent.message)
                    }

                    is DemoTabsIntent.ClearNotifications -> updateState {
                        copy(notifications = emptyList())
                    }
                }
            }
        }
}

/**
 * CompositionLocal providing access to the DemoTabs shared store.
 *
 * Usage in child screens:
 * ```kotlin
 * val store = LocalDemoTabsStore.current
 * store?.intent(DemoTabsIntent.AddFavorite("item_1"))
 * ```
 */
val LocalDemoTabsStore: ProvidableCompositionLocal<Store<DemoTabsState, DemoTabsIntent, DemoTabsAction>> =
    staticCompositionLocalOf { throw IllegalStateException("No DemoTabsStore provided") }
