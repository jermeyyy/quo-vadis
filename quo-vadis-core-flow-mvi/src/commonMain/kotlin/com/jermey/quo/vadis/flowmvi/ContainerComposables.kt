package com.jermey.quo.vadis.flowmvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.compose.scope.LocalContainerNode
import com.jermey.quo.vadis.core.compose.scope.LocalNavigator
import com.jermey.quo.vadis.core.compose.scope.LocalScreenNode
import com.jermey.quo.vadis.core.navigation.node.NavNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.compose.getKoin
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.emptyParametersHolder
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.qualifier
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
 * @param Container The container type
 * @param State The state type
 * @param Intent The intent type
 * @param Action The action type
 * @param params Optional Koin parameters
 * @return The FlowMVI Store
 */
@FlowMVIDSL
@Composable
inline fun <reified Container, State, Intent, Action> rememberContainer(
    qualifier: Qualifier,
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<State, Intent, Action>
        where Container : NavigationContainer<State, Intent, Action>,
              State : MVIState,
              Intent : MVIIntent,
              Action : MVIAction {

    val screenNode = LocalScreenNode.current
        ?: error("rememberContainer must be called within a screen rendered by NavigationHost")
    val navigator = LocalNavigator.current
        ?: error("rememberContainer must be called within NavigationHost")

    val koin = getKoin()

    // Use screenNode.key for Koin scope identity - key is stable across state updates
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
        containerScope.scope.get<NavigationContainer<State, Intent, Action>>(
            qualifier = qualifier,
            parameters = params
        ).also {
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
 * @param Container The container type
 * @param State The state type
 * @param Intent The intent type
 * @param Action The action type
 * @param params Optional Koin parameters
 * @return The FlowMVI Store
 */
@FlowMVIDSL
@Composable
inline fun <reified Container, State, Intent, Action> rememberSharedContainer(
    qualifier: Qualifier,
    noinline params: ParametersDefinition = { emptyParametersHolder() },
): Store<State, Intent, Action>
        where Container : SharedNavigationContainer<State, Intent, Action>,
              State : MVIState,
              Intent : MVIIntent,
              Action : MVIAction {

    val containerNode = LocalContainerNode.current
        ?: error("rememberSharedContainer must be called within a Tab/Pane container wrapper")
    val navigator = LocalNavigator.current
        ?: error("rememberSharedContainer must be called within NavigationHost")

    val koin = getKoin()

    // Get the container key for stable scope identity
    val containerKey = (containerNode as? NavNode)?.key
        ?: error("Container node must be a NavNode with a key")

    // Use containerKey for Koin scope identity - key is stable across state updates
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
        sharedScope.scope.get<Container>(
            qualifier = qualifier,
            parameters = params
        ).also {
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
 * @param Container The container type
 * @param factory Factory function that creates the container
 */
@FlowMVIDSL
inline fun <reified Container : NavigationContainer<*, *, *>> Module.navigationContainer(
    qualifier: Qualifier = qualifier<Container>(),
    crossinline factory: (NavigationContainerScope) -> Container,
) {
    scope<NavigationContainerScope> {
        scoped<Container>(qualifier = qualifier) { factory(get()) }
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
 * @param Container The container type
 * @param factory Factory function that creates the container
 */
@FlowMVIDSL
inline fun <reified Container : SharedNavigationContainer<*, *, *>> Module.sharedNavigationContainer(
    crossinline factory: (SharedContainerScope) -> Container,
) {
    scope<SharedContainerScope> {
        scoped<Container> { factory(get()) }
    }
}
