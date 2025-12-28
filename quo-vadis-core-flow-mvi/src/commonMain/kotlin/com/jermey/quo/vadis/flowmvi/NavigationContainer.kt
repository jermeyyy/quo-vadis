/*
 * Copyright 2025 Jermey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jermey.quo.vadis.flowmvi

import com.jermey.quo.vadis.core.navigation.core.Navigator
import kotlinx.coroutines.CoroutineScope
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store

/**
 * Base class for FlowMVI containers scoped to a screen's lifecycle.
 *
 * This replaces [BaseContainer] with a cleaner integration:
 * - No manual lifecycle registration - handled by the renderer
 * - Access to screen context via [NavigationContainerScope]
 * - Proper coroutine scope management
 *
 * ## Usage
 *
 * ```kotlin
 * class HomeContainer(scope: NavigationContainerScope) :
 *     NavigationContainer<HomeState, HomeIntent, HomeAction>(scope) {
 *
 *     override val store = store(HomeState()) {
 *         // Configure store
 *     }
 * }
 * ```
 *
 * ## Registration
 *
 * Register in a Koin module using [navigationContainer]:
 *
 * ```kotlin
 * val homeModule = module {
 *     navigationContainer<HomeContainer> { scope ->
 *         HomeContainer(scope)
 *     }
 * }
 * ```
 *
 * ## In Composable
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val store = rememberContainer<HomeContainer, HomeState, HomeIntent, HomeAction>()
 *     // Use store
 * }
 * ```
 *
 * @param S The MVI state type
 * @param I The MVI intent type
 * @param A The MVI action type
 * @property scope The navigation container scope
 */
abstract class NavigationContainer<S : MVIState, I : MVIIntent, A : MVIAction>(
    protected val scope: NavigationContainerScope,
) : Container<S, I, A> {

    /**
     * The Navigator instance for navigation operations.
     */
    protected val navigator: Navigator get() = scope.navigator

    /**
     * The unique key for this screen instance.
     */
    protected val screenKey: String get() = scope.screenKey

    /**
     * Coroutine scope tied to the screen's lifecycle.
     */
    protected val coroutineScope: CoroutineScope get() = scope.coroutineScope

    /**
     * The FlowMVI store. Must be overridden by subclasses.
     */
    abstract override val store: Store<S, I, A>
}
