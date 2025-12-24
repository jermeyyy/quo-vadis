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

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope

/**
 * Koin scope for FlowMVI containers tied to a screen's lifecycle.
 *
 * This scope provides:
 * - Access to the [Navigator] for navigation operations
 * - Access to the [ScreenNode] for screen context
 * - A [CoroutineScope] tied to the screen's lifecycle
 *
 * The scope is automatically closed when the screen is destroyed.
 *
 * ## Usage
 *
 * This scope is created automatically by [rememberContainer] and provides
 * dependency injection context for [NavigationContainer] instances.
 *
 * ```kotlin
 * class HomeContainer(scope: NavigationContainerScope) :
 *     NavigationContainer<HomeState, HomeIntent, HomeAction>(scope) {
 *
 *     // Access navigator via scope.navigator
 *     // Access screen key via scope.screenKey
 *     // Access coroutine scope via scope.coroutineScope
 * }
 * ```
 *
 * @property scope The Koin scope instance
 * @property screenNode The screen node this container is attached to
 * @property navigator The Navigator instance for navigation operations
 */
@Stable
class NavigationContainerScope(
    override val scope: Scope,
    val screenNode: ScreenNode,
    val navigator: Navigator,
) : KoinScopeComponent {

    /**
     * Coroutine scope for the container, tied to screen lifecycle.
     * Injected from the Koin scope.
     */
    val coroutineScope: CoroutineScope by scope.inject()

    /**
     * Unique key for this screen instance.
     */
    val screenKey: String get() = screenNode.key

}
