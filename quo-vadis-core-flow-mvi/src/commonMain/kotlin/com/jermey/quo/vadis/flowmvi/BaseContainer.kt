package com.jermey.quo.vadis.flowmvi

import androidx.annotation.CallSuper
import com.jermey.quo.vadis.core.navigation.core.NavigationLifecycle
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.registerNavigationLifecycle
import com.jermey.quo.vadis.core.navigation.core.unregisterNavigationLifecycle
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.get
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * Base class for MVI containers with navigation lifecycle integration.
 *
 * @param S The type of MVI state
 * @param I The type of MVI intent
 * @param A The type of MVI action
 * @property navigator The Navigator instance for navigation operations
 * @property screenKey The unique screen key for this container
 */
abstract class BaseContainer<S : MVIState, I : MVIIntent, A : MVIAction>(
    protected val navigator: Navigator,
    protected val screenKey: String,
) : Container<S, I, A>, NavigationLifecycle, DestinationScope {

    override val scope = getKoin().getOrCreateScope<DestinationScope>(screenKey)

    override val coroutineScope: CoroutineScope = get()

    init {
        navigator.registerNavigationLifecycle(this, screenKey)
    }

    @CallSuper
    override fun onDestroy() {
        navigator.unregisterNavigationLifecycle(this)
        scope.close()
    }

}