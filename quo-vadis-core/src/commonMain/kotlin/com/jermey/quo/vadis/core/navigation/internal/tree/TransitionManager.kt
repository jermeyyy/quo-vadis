package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.internal.TransitionController
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.TransitionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * Manages transition state for navigation animations.
 *
 * Extracted from [TreeNavigator] to separate transition management
 * from core navigation logic. Handles both programmatic transitions
 * (push/pop animations) and gesture-driven predictive back transitions.
 *
 * @param navigatorScope Coroutine scope for flow operations
 * @param stateProvider Provider for the current navigation state
 * @param onCommitBack Callback invoked when predictive back gesture is committed
 */
@OptIn(InternalQuoVadisApi::class)
internal class TransitionManager(
    private val navigatorScope: CoroutineScope,
    private val stateProvider: () -> NavNode,
    private val onCommitBack: () -> Boolean
) : TransitionController {

    private val _transitionState: MutableStateFlow<TransitionState> =
        MutableStateFlow(TransitionState.Idle)

    override val transitionState: StateFlow<TransitionState> = _transitionState.asStateFlow()

    override val currentTransition: StateFlow<NavigationTransition?> = _transitionState
        .map { state ->
            when (state) {
                is TransitionState.Idle -> null
                is TransitionState.InProgress -> state.transition
                is TransitionState.PredictiveBack -> null
                is TransitionState.Seeking -> state.transition
            }
        }
        .stateIn(
            scope = navigatorScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    override fun updateTransitionProgress(progress: Float) {
        _transitionState.update { current ->
            when (current) {
                is TransitionState.Idle -> current
                is TransitionState.Active -> current.withProgress(progress)
            }
        }
    }

    override fun startPredictiveBack() {
        val state = stateProvider()
        val currentKey = state.activeLeaf()?.key?.value
        val activeStack = state.activeStack()
        val previousKey = if (activeStack != null && activeStack.children.size >= 2) {
            activeStack.children[activeStack.children.size - 2].activeLeaf()?.key?.value
        } else {
            null
        }

        _transitionState.update {
            TransitionState.PredictiveBack(
                progress = 0f,
                currentKey = currentKey,
                previousKey = previousKey
            )
        }
    }

    override fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float) {
        _transitionState.update { current ->
            if (current is TransitionState.PredictiveBack) {
                current.copy(
                    progress = progress.coerceIn(0f, 1f),
                    touchX = touchX.coerceIn(0f, 1f),
                    touchY = touchY.coerceIn(0f, 1f)
                )
            } else {
                current
            }
        }
    }

    override fun cancelPredictiveBack() {
        _transitionState.update { TransitionState.Idle }
    }

    override fun commitPredictiveBack() {
        val current = _transitionState.value
        if (current is TransitionState.PredictiveBack) {
            _transitionState.update { current.copy(isCommitted = true) }
            onCommitBack()
            _transitionState.update { TransitionState.Idle }
        }
    }

    override fun completeTransition() {
        _transitionState.update { TransitionState.Idle }
    }

    /**
     * Starts or resets a navigation transition.
     *
     * Called by [TreeNavigator] during navigate operations to coordinate
     * transition animations.
     *
     * @param transition The transition to start, or null to reset to idle
     * @param fromKey The key of the screen being navigated away from
     * @param toKey The key of the screen being navigated to
     */
    fun startNavigationTransition(
        transition: NavigationTransition?,
        fromKey: String?,
        toKey: String?
    ) {
        if (transition != null) {
            _transitionState.update {
                TransitionState.InProgress(
                    transition = transition,
                    progress = 0f,
                    fromKey = fromKey,
                    toKey = toKey
                )
            }
        } else {
            _transitionState.update { TransitionState.Idle }
        }
    }
}
