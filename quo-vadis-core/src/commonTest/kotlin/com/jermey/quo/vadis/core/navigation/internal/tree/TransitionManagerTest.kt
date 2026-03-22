@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.transition.TransitionState
import com.jermey.quo.vadis.core.navigation.transition.progress
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private object TransitionTestDest : NavDestination

class TransitionManagerTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    fun screen(key: String, parentKey: String? = null) =
        ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, TransitionTestDest)

    fun stack(key: String, parentKey: String? = null, vararg children: ScreenNode) =
        StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, children.toList())

    fun createManager(
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        stateProvider: () -> NavNode = {
            stack("root", null, screen("s1", "root"), screen("s2", "root"))
        },
        onCommitBack: () -> Boolean = { true }
    ): TransitionManager = TransitionManager(scope, stateProvider, onCommitBack)

    // =========================================================================
    // Initial state
    // =========================================================================

    test("initial transitionState is Idle") {
        val manager = createManager()
        manager.transitionState.value shouldBe TransitionState.Idle
    }

    test("initial currentTransition is null") {
        val manager = createManager()
        manager.currentTransition.value.shouldBeNull()
    }

    // =========================================================================
    // startNavigationTransition
    // =========================================================================

    test("startNavigationTransition with transition sets InProgress state") {
        val manager = createManager()
        manager.startNavigationTransition(NavigationTransitions.Fade, "from", "to")

        val state = manager.transitionState.value
        state.shouldBeInstanceOf<TransitionState.InProgress>()
        state.transition shouldBe NavigationTransitions.Fade
        state.fromKey shouldBe "from"
        state.toKey shouldBe "to"
        state.progress shouldBe 0f
    }

    test("startNavigationTransition with null transition resets to Idle") {
        val manager = createManager()
        // First set some transition
        manager.startNavigationTransition(NavigationTransitions.Fade, "a", "b")
        manager.transitionState.value.shouldBeInstanceOf<TransitionState.InProgress>()

        // Then reset
        manager.startNavigationTransition(null, null, null)
        manager.transitionState.value shouldBe TransitionState.Idle
    }

    // =========================================================================
    // completeTransition
    // =========================================================================

    test("completeTransition resets to Idle") {
        val manager = createManager()
        manager.startNavigationTransition(NavigationTransitions.Fade, "a", "b")

        manager.completeTransition()

        manager.transitionState.value shouldBe TransitionState.Idle
    }

    test("completeTransition on already idle state stays Idle") {
        val manager = createManager()
        manager.completeTransition()
        manager.transitionState.value shouldBe TransitionState.Idle
    }

    // =========================================================================
    // updateTransitionProgress
    // =========================================================================

    test("updateTransitionProgress updates InProgress state") {
        val manager = createManager()
        manager.startNavigationTransition(NavigationTransitions.Fade, "a", "b")

        manager.updateTransitionProgress(0.5f)

        val state = manager.transitionState.value
        state.shouldBeInstanceOf<TransitionState.InProgress>()
        state.progress shouldBe 0.5f
    }

    test("updateTransitionProgress on Idle does nothing") {
        val manager = createManager()
        manager.updateTransitionProgress(0.5f)
        manager.transitionState.value shouldBe TransitionState.Idle
    }

    // =========================================================================
    // Predictive back flow
    // =========================================================================

    test("startPredictiveBack sets PredictiveBack state") {
        val s1 = screen("s1", "root")
        val s2 = screen("s2", "root")
        val root = stack("root", null, s1, s2)

        val manager = createManager(stateProvider = { root })
        manager.startPredictiveBack()

        val state = manager.transitionState.value
        state.shouldBeInstanceOf<TransitionState.PredictiveBack>()
        state.progress shouldBe 0f
        state.currentKey shouldBe "s2"
        state.previousKey shouldBe "s1"
    }

    test("startPredictiveBack with single-item stack has null previousKey") {
        val s1 = screen("s1", "root")
        val root = stack("root", null, s1)

        val manager = createManager(stateProvider = { root })
        manager.startPredictiveBack()

        val state = manager.transitionState.value
        state.shouldBeInstanceOf<TransitionState.PredictiveBack>()
        state.previousKey.shouldBeNull()
    }

    test("updatePredictiveBack updates progress and touch coordinates") {
        val s1 = screen("s1", "root")
        val s2 = screen("s2", "root")
        val root = stack("root", null, s1, s2)

        val manager = createManager(stateProvider = { root })
        manager.startPredictiveBack()

        manager.updatePredictiveBack(0.4f, 0.3f, 0.7f)

        val state = manager.transitionState.value
        state.shouldBeInstanceOf<TransitionState.PredictiveBack>()
        state.progress shouldBe 0.4f
        state.touchX shouldBe 0.3f
        state.touchY shouldBe 0.7f
    }

    test("updatePredictiveBack coerces values to 0-1 range") {
        val s1 = screen("s1", "root")
        val s2 = screen("s2", "root")
        val root = stack("root", null, s1, s2)

        val manager = createManager(stateProvider = { root })
        manager.startPredictiveBack()

        manager.updatePredictiveBack(1.5f, -0.2f, 2.0f)

        val state = manager.transitionState.value
        state.shouldBeInstanceOf<TransitionState.PredictiveBack>()
        state.progress shouldBe 1.0f
        state.touchX shouldBe 0.0f
        state.touchY shouldBe 1.0f
    }

    test("updatePredictiveBack on non-predictive state does nothing") {
        val manager = createManager()
        manager.startNavigationTransition(NavigationTransitions.Fade, "a", "b")

        manager.updatePredictiveBack(0.5f, 0.5f, 0.5f)

        // Should still be InProgress, not changed
        manager.transitionState.value.shouldBeInstanceOf<TransitionState.InProgress>()
    }

    test("cancelPredictiveBack resets to Idle") {
        val s1 = screen("s1", "root")
        val s2 = screen("s2", "root")
        val root = stack("root", null, s1, s2)

        val manager = createManager(stateProvider = { root })
        manager.startPredictiveBack()
        manager.transitionState.value.shouldBeInstanceOf<TransitionState.PredictiveBack>()

        manager.cancelPredictiveBack()
        manager.transitionState.value shouldBe TransitionState.Idle
    }

    test("commitPredictiveBack calls onCommitBack and resets to Idle") {
        val s1 = screen("s1", "root")
        val s2 = screen("s2", "root")
        val root = stack("root", null, s1, s2)

        var commitCalled = false
        val manager = createManager(
            stateProvider = { root },
            onCommitBack = { commitCalled = true; true }
        )
        manager.startPredictiveBack()

        manager.commitPredictiveBack()

        commitCalled.shouldBeTrue()
        manager.transitionState.value shouldBe TransitionState.Idle
    }

    test("commitPredictiveBack from non-predictive state does nothing") {
        var commitCalled = false
        val manager = createManager(onCommitBack = { commitCalled = true; true })

        manager.commitPredictiveBack()

        commitCalled.shouldBeFalse()
        manager.transitionState.value shouldBe TransitionState.Idle
    }

    test("commitPredictiveBack sets isCommitted before calling onCommitBack") {
        val s1 = screen("s1", "root")
        val s2 = screen("s2", "root")
        val root = stack("root", null, s1, s2)

        var observedCommittedState = false
        lateinit var mgr: TransitionManager
        mgr = createManager(
            stateProvider = { root },
            onCommitBack = {
                // During the callback, isCommitted should be true on the state
                val state = mgr.transitionState.value
                observedCommittedState = (state as? TransitionState.PredictiveBack)?.isCommitted == true
                true
            }
        )
        mgr.startPredictiveBack()
        mgr.commitPredictiveBack()

        observedCommittedState.shouldBeTrue()
    }

    // =========================================================================
    // progress extension
    // =========================================================================

    test("TransitionState.Idle progress is 0") {
        TransitionState.Idle.progress shouldBe 0f
    }

    test("TransitionState.InProgress progress reflects value") {
        val state = TransitionState.InProgress(
            transition = NavigationTransitions.Fade,
            progress = 0.75f,
            fromKey = "a",
            toKey = "b"
        )
        state.progress shouldBe 0.75f
    }

    test("TransitionState.PredictiveBack progress reflects value") {
        val state = TransitionState.PredictiveBack(progress = 0.3f)
        state.progress shouldBe 0.3f
    }
})
