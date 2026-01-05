package com.jermey.quo.vadis.flowmvi

import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store

/**
 * Base class for MVI containers shared across all screens within a Tab or Pane container.
 *
 * Shared containers enable:
 * - Tab-wide state (e.g., unread badge count)
 * - Pane coordination (e.g., master-detail selection)
 * - Cross-screen communication within a container scope
 *
 * ## Usage
 *
 * ```kotlin
 * class MainTabsContainer(scope: SharedContainerScope) :
 *     SharedNavigationContainer<TabsState, TabsIntent, TabsAction>(scope) {
 *
 *     override val store = store(TabsState()) {
 *         // Configure store
 *     }
 * }
 * ```
 *
 * ## Registration
 *
 * Register in a Koin module using [sharedNavigationContainer]:
 *
 * ```kotlin
 * val tabsModule = module {
 *     sharedNavigationContainer<MainTabsContainer> { scope ->
 *         MainTabsContainer(scope)
 *     }
 * }
 * ```
 *
 * ## In Container Wrapper
 *
 * ```kotlin
 * @TabsContainer(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(scope: TabsContainerScope) {
 *     val store = rememberSharedContainer<MainTabsContainer, TabsState, TabsIntent, TabsAction>()
 *     // Provide store to children or use in wrapper
 * }
 * ```
 *
 * @param S The MVI state type
 * @param I The MVI intent type
 * @param A The MVI action type
 * @property scope The shared container scope
 */
abstract class SharedNavigationContainer<S : MVIState, I : MVIIntent, A : MVIAction>(
    protected val scope: SharedContainerScope,
) : Container<S, I, A> {

    /**
     * The Navigator instance for navigation operations.
     */
    protected val navigator: Navigator get() = scope.navigator

    /**
     * The unique key for this container instance.
     */
    protected val containerKey: String get() = scope.containerKey

    /**
     * Coroutine scope tied to the container's lifecycle.
     */
    protected val containerScope: CoroutineScope get() = scope.coroutineScope

    /**
     * The FlowMVI store. Must be overridden by subclasses.
     */
    abstract override val store: Store<S, I, A>
}
