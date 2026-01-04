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
import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.koin.core.component.KoinScopeComponent
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback

/**
 * Koin scope for shared FlowMVI containers bound to a Tab/Pane container.
 *
 * Shared containers are accessible from all screens within the container,
 * enabling cross-screen state sharing and communication.
 *
 * ## Usage
 *
 * This scope is created automatically by [rememberSharedContainer] and provides
 * dependency injection context for [SharedNavigationContainer] instances.
 *
 * ```kotlin
 * class MainTabsContainer(scope: SharedContainerScope) :
 *     SharedNavigationContainer<TabsState, TabsIntent, TabsAction>(scope) {
 *
 *     // Access navigator via scope.navigator
 *     // Access container key via scope.containerKey
 *     // Access coroutine scope via scope.coroutineScope
 * }
 * ```
 *
 * @property scope The Koin scope instance
 * @property containerNode The container node (TabNode or PaneNode)
 * @property navigator The Navigator instance
 */
@Stable
class SharedContainerScope(
    override val scope: Scope,
    val containerNode: LifecycleAwareNode,
    val navigator: Navigator,
) : KoinScopeComponent {

    /**
     * Coroutine scope for the container, tied to container lifecycle.
     * Injected from the Koin scope.
     */
    val coroutineScope: CoroutineScope by scope.inject()

    /**
     * Unique key for this container instance.
     */
    val containerKey: String get() = (containerNode as NavNode).key

    /**
     * Callback registered with the container node to close scope on destroy.
     */
    private val onDestroyCallback: () -> Unit = {
        scope.close()
    }

    init {
        // Cancel coroutine scope when Koin scope is closed
        scope.registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                coroutineScope.cancel()
            }
        })

        // Close Koin scope when container node is destroyed
        containerNode.addOnDestroyCallback(onDestroyCallback)
    }

}
