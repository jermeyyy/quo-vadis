package com.jermey.quo.vadis.flowmvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.compose.render.LocalScreenNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.component.KoinScopeComponent
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeCallback
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store

@Stable
interface DestinationScope : KoinScopeComponent {
    val coroutineScope: CoroutineScope
}

@FlowMVIDSL
inline fun <reified T : Container<*, *, *>> Module.container(noinline definition: Definition<T>) {
    scope<DestinationScope> {
        scoped<T> { params ->
            definition(this, params).apply {
                store.start(get<CoroutineScope>())
            }
        }
    }
}

@FlowMVIDSL
@Composable
inline fun <reified T : BaseContainer<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> container(
    noinline params: ParametersDefinition,
): Store<S, I, A> {
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val scope = getKoin().getOrCreateScope<DestinationScope>(LocalScreenNode.current!!.key)
    scope.declare(instance = coroutineScope, allowOverride = false)

    scope.registerCallback(object : ScopeCallback {
        override fun onScopeClose(scope: Scope) {
            println("Scope ${scope.id} is being closed.")
            coroutineScope.cancel()
        }
    })

    return koinInject<T>(
        scope = scope, parameters = params
    ).store
}

@FlowMVIDSL
@Composable
inline fun <reified T : BaseContainer<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> container(): Store<S, I, A> {
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val scope = getKoin().getOrCreateScope<DestinationScope>(LocalScreenNode.current!!.key)
    scope.declare(instance = coroutineScope, allowOverride = false)

    scope.registerCallback(object : ScopeCallback {
        override fun onScopeClose(scope: Scope) {
            println("Scope ${scope.id} is being closed.")
            coroutineScope.cancel()
        }
    })

    return koinInject<T>(
        scope = scope
    ).store
}
