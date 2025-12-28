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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.compose.render.LocalContainerNode
import com.jermey.quo.vadis.core.navigation.compose.render.LocalNavigator
import com.jermey.quo.vadis.core.navigation.compose.render.LocalScreenNode
import com.jermey.quo.vadis.core.navigation.core.NavNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.compose.getKoin
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.emptyParametersHolder
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store

/**
 * Remembers a [NavigationContainer] scoped to the current screen.
 *
 * The container is:
 * - Created once per screen instance (keyed by screen key)
 * - Automatically started with the correct coroutine scope
 * - Cleaned up when the screen is destroyed
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun HomeScreen() {
 *     val store = rememberContainer<HomeContainer, HomeState, HomeIntent, HomeAction>()
 *
 *     with(store) {
 *         val state by subscribe()
 *         // Render UI
 *     }
 * }
 * ```
 *
 * @param C The container type
 * @param S The state type
 * @param I The intent type
 * @param A The action type
 * @param params Optional Koin parameters
 * @return The FlowMVI Store
 */
@FlowMVIDSL
@Composable
inline fun <reified C : NavigationContainer<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> rememberContainer(
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<S, I, A> {
    val screenNode = LocalScreenNode.current
        ?: error("rememberContainer must be called within a screen rendered by NavigationHost")
    val navigator = LocalNavigator.current
        ?: error("rememberContainer must be called within NavigationHost")

    val koin = getKoin()

    // Use screenNode.key for Koin scope identity - key is stable across state updates
    // unlike uuid which is @Transient and regenerated on each ScreenNode creation
    val containerScope = remember(screenNode.key) {
        val koinScope = koin.getOrCreateScope<NavigationContainerScope>(screenNode.key)

        // Declare coroutine scope in Koin scope if not already present
        if (koinScope.getOrNull<CoroutineScope>() == null) {
            koinScope.declare(CoroutineScope(Dispatchers.Default + SupervisorJob()))
        }

        val navContainerScope = NavigationContainerScope(
            scope = koinScope,
            screenNode = screenNode,
            navigator = navigator,
        )

        // Declare the scope itself so containers can inject it
        koinScope.declare(navContainerScope)

        navContainerScope
    }

    val container = remember(containerScope) {
        containerScope.scope.get<C>(parameters = params).also {
            if (!it.store.isActive) {
                it.store.start(containerScope.coroutineScope)
            }
        }
    }

    return container.store
}

/**
 * Remembers a [SharedNavigationContainer] scoped to the current Tab/Pane container.
 *
 * The container is:
 * - Created once per container instance (keyed by container key)
 * - Accessible from all child screens within the container
 * - Cleaned up when the container is destroyed
 *
 * ## Usage
 *
 * In a container wrapper:
 * ```kotlin
 * @TabsContainer(MainTabs::class)
 * @Composable
 * fun MainTabsWrapper(scope: TabsContainerScope) {
 *     val store = rememberSharedContainer<MainTabsContainer, TabsState, TabsIntent, TabsAction>()
 *
 *     CompositionLocalProvider(LocalMainTabsStore provides store) {
 *         Column {
 *             scope.TabContent()
 *             TabBar(store)
 *         }
 *     }
 * }
 * ```
 *
 * @param C The container type
 * @param S The state type
 * @param I The intent type
 * @param A The action type
 * @param params Optional Koin parameters
 * @return The FlowMVI Store
 */
@FlowMVIDSL
@Composable
inline fun <reified C : SharedNavigationContainer<S, I, A>, S, I, A> rememberSharedContainer(
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<S, I, A> where S : MVIState, I : MVIIntent, A : MVIAction {
    val containerNode = LocalContainerNode.current
        ?: error("rememberSharedContainer must be called within a Tab/Pane container wrapper")
    val navigator = LocalNavigator.current
        ?: error("rememberSharedContainer must be called within NavigationHost")

    val koin = getKoin()

    // Get the container key for stable scope identity
    val containerKey = (containerNode as? NavNode)?.key
        ?: error("Container node must be a NavNode with a key")

    // Use containerKey for Koin scope identity - key is stable across state updates
    // unlike uuid which is @Transient and regenerated on each node creation
    val sharedScope = remember(containerKey) {
        val koinScope = koin.getOrCreateScope<SharedContainerScope>(containerKey)

        if (koinScope.getOrNull<CoroutineScope>() == null) {
            koinScope.declare(CoroutineScope(Dispatchers.Default + SupervisorJob()))
        }

        val sharedContainerScope = SharedContainerScope(
            scope = koinScope,
            containerNode = containerNode,
            navigator = navigator,
        )

        // Declare the scope itself so containers can inject it
        koinScope.declare(sharedContainerScope)

        sharedContainerScope
    }

    val container = remember(sharedScope) {
        sharedScope.scope.get<C>(parameters = params).also {
            if (!it.store.isActive) {
                it.store.start(sharedScope.coroutineScope)
            }
        }
    }

    return container.store
}

/**
 * Koin module extension for declaring screen-scoped navigation containers.
 *
 * The container must accept a [NavigationContainerScope] in its constructor,
 * which will be provided by the [rememberContainer] composable.
 *
 * ## Usage
 *
 * ```kotlin
 * val myModule = module {
 *     navigationContainer { scope: NavigationContainerScope ->
 *         HomeContainer(scope)
 *     }
 * }
 * ```
 *
 * @param C The container type
 * @param factory Factory function that creates the container
 */
@FlowMVIDSL
inline fun <reified C : NavigationContainer<*, *, *>> Module.navigationContainer(
    crossinline factory: (NavigationContainerScope) -> C,
) {
    scope<NavigationContainerScope> {
        scoped<C> { factory(get()) }
    }
}

/**
 * Koin module extension for declaring container-scoped shared containers.
 *
 * The container must accept a [SharedContainerScope] in its constructor,
 * which will be provided by the [rememberSharedContainer] composable.
 *
 * ## Usage
 *
 * ```kotlin
 * val myModule = module {
 *     sharedNavigationContainer { scope: SharedContainerScope ->
 *         MainTabsContainer(scope)
 *     }
 * }
 * ```
 *
 * @param C The container type
 * @param factory Factory function that creates the container
 */
@FlowMVIDSL
inline fun <reified C : SharedNavigationContainer<*, *, *>> Module.sharedNavigationContainer(
    crossinline factory: (SharedContainerScope) -> C,
) {
    scope<SharedContainerScope> {
        scoped<C> { factory(get()) }
    }
}
