package com.jermey.quo.vadis.flowmvi

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.registry.BackHandlerRegistry
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
    val containerDestination: NavDestination?,
    val navigator: Navigator,
    val backHandlerRegistry: BackHandlerRegistry,
) : KoinScopeComponent {

    /**
     * Coroutine scope for the container, tied to container lifecycle.
     * Injected from the Koin scope.
     */
    val coroutineScope: CoroutineScope by scope.inject()

    /**
     * Unique key for this container instance.
     */
    val containerKey: String get() = (containerNode as NavNode).key.value

    /**
     * Tracked handler unregister functions for cleanup on scope close.
     */
    private val handlerUnregisters = mutableListOf<() -> Unit>()

    /**
     * Register a back handler scoped to this container's lifecycle.
     *
     * The handler is registered with the [BackHandlerRegistry] using the container's
     * [NodeKey][com.jermey.quo.vadis.core.navigation.node.NodeKey]. Since shared containers
     * span multiple screens, this handler fires when any child screen is active
     * (via ancestor key walking in the registry).
     *
     * ## Usage
     *
     * ```kotlin
     * class MainTabsContainer(scope: SharedContainerScope) :
     *     SharedNavigationContainer<TabsState, TabsIntent, TabsAction>(scope) {
     *
     *     init {
     *         scope.registerBackHandler {
     *             if (currentState.shouldInterceptBack) {
     *                 intent(TabsIntent.ShowExitConfirmation)
     *                 true
     *             } else {
     *                 false
     *             }
     *         }
     *     }
     * }
     * ```
     *
     * @param handler A function returning `true` if the back event was consumed,
     *   `false` to allow propagation.
     * @return A function to manually unregister the handler before scope close.
     */
    fun registerBackHandler(handler: () -> Boolean): () -> Unit {
        val containerKey = (containerNode as NavNode).key
        val unregister = backHandlerRegistry.register(containerKey, handler)
        handlerUnregisters.add(unregister)
        return {
            unregister()
            handlerUnregisters.remove(unregister)
        }
    }

    /**
     * Callback registered with the container node to close scope on destroy.
     */
    private val onDestroyCallback: () -> Unit = {
        scope.close()
    }

    init {
        // Cancel coroutine scope and unregister back handlers when Koin scope is closed
        scope.registerCallback(object : ScopeCallback {
            override fun onScopeClose(scope: Scope) {
                handlerUnregisters.forEach { it() }
                handlerUnregisters.clear()
                coroutineScope.cancel()
            }
        })

        // Close Koin scope when container node is destroyed
        containerNode.addOnDestroyCallback(onDestroyCallback)
    }

}
