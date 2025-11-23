package com.jermey.quo.vadis.flowmvi.integration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store

/**
 * Android ViewModel wrapper for FlowMVI Container.
 * 
 * Provides:
 * - Automatic lifecycle management via ViewModel
 * - Configuration change survival
 * - Process death handling (when combined with savedstate plugin)
 * - Proper cleanup on ViewModel clear
 * 
 * The store is automatically started with viewModelScope and closed when
 * the ViewModel is cleared.
 * 
 * Usage with Koin:
 * ```kotlin
 * val profileModule = module {
 *     viewModel {
 *         ContainerViewModel(
 *             container = ProfileContainer(
 *                 navigator = get(),
 *                 repository = get()
 *             )
 *         )
 *     }
 * }
 * 
 * @Composable
 * fun ProfileScreen(viewModel: ContainerViewModel<ProfileState, ProfileIntent, ProfileAction> = koinViewModel()) {
 *     with(viewModel.store) {
 *         val state by subscribe { action -> /* handle */ }
 *         // ... UI
 *     }
 * }
 * ```
 * 
 * Alternative usage without Koin:
 * ```kotlin
 * class ProfileViewModel(
 *     private val container: ProfileContainer
 * ) : ContainerViewModel<ProfileState, ProfileIntent, ProfileAction>(container)
 * 
 * @Composable
 * fun ProfileScreen(viewModel: ProfileViewModel = viewModel()) {
 *     // Use viewModel.store
 * }
 * ```
 * 
 * @param S State type (extends MVIState)
 * @param I Intent type (extends MVIIntent)
 * @param A Action type (extends MVIAction)
 * @param container The FlowMVI container to wrap
 */
open class ContainerViewModel<S : MVIState, I : MVIIntent, A : MVIAction>(
    private val container: Container<S, I, A>
) : ViewModel(), Container<S, I, A> by container {

    /**
     * The FlowMVI store, automatically started with viewModelScope.
     * 
     * The store lifecycle is managed by the ViewModel:
     * - Started when first accessed
     * - Closed when ViewModel is cleared
     */
    override val store: Store<S, I, A> by lazy {
        container.store.also { it.start(viewModelScope) }
    }

    /**
     * Called when the ViewModel is cleared (activity/fragment destroyed).
     * Closes the store to clean up resources.
     */
    override fun onCleared() {
        super.onCleared()
        store.close()
    }
}

/**
 * Factory function to create ContainerViewModel with Koin.
 * 
 * Usage:
 * ```kotlin
 * val profileModule = module {
 *     viewModel {
 *         containerViewModel {
 *             ProfileContainer(navigator = get(), repository = get())
 *         }
 *     }
 * }
 * ```
 */
inline fun <reified S : MVIState, reified I : MVIIntent, reified A : MVIAction> containerViewModel(
    crossinline factory: () -> Container<S, I, A>
): ContainerViewModel<S, I, A> {
    return ContainerViewModel(factory())
}
