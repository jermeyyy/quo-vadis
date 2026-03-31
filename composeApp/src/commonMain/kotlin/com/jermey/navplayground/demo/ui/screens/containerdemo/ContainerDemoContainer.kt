package com.jermey.navplayground.demo.ui.screens.containerdemo

import com.jermey.navplayground.navigation.ContainerDemoDestination
import com.jermey.quo.vadis.flowmvi.SharedContainerScope
import com.jermey.quo.vadis.flowmvi.SharedNavigationContainer
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Qualifier
import org.koin.core.annotation.Scope
import org.koin.core.annotation.Scoped
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce

data class ContainerDemoState(
    val itemId: Int,
) : MVIState

sealed interface ContainerDemoIntent : MVIIntent

sealed interface ContainerDemoAction : MVIAction

/**
 * Shared MVI container scoped to the ContainerDemo tab container.
 *
 * Reads `itemId` from `SharedContainerScope.containerDestination` (backed by
 * `TabNode.destination`), proving that tab-wrapper containers can access the
 * triggering destination's arguments.
 */
@Scoped
@Scope(SharedContainerScope::class)
@Qualifier(ContainerDemoContainer::class)
class ContainerDemoContainer(
    @Provided scope: SharedContainerScope,
) : SharedNavigationContainer<ContainerDemoState, ContainerDemoIntent, ContainerDemoAction>(scope) {

    private val itemId: Int = run {
        val tabNode = scope.containerDestination as ContainerDemoDestination
        tabNode.itemId
    }

    override val store: Store<ContainerDemoState, ContainerDemoIntent, ContainerDemoAction> =
        store(ContainerDemoState(itemId = itemId)) {
            configure { name = "ContainerDemoStore" }
            reduce { intent ->

            }
        }
}
