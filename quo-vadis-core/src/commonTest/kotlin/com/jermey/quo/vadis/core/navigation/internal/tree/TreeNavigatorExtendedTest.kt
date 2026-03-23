@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.navigator.NavigationErrorHandler
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.result.navigateBackWithResult
import com.jermey.quo.vadis.core.navigation.testing.withDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.TransitionState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers

private object HomeDest : NavDestination {
    override fun toString(): String = "home"
}

private object DetailDest : NavDestination {
    override fun toString(): String = "detail"
}

private object SettingsDest : NavDestination {
    override fun toString(): String = "settings"
}

private object ProfileDest : NavDestination {
    override fun toString(): String = "profile"
}

private object ListDest : NavDestination {
    override fun toString(): String = "list"
}

private object TestTransition : NavigationTransition {
    override val enter: EnterTransition = EnterTransition.None
    override val exit: ExitTransition = ExitTransition.None
    override val popEnter: EnterTransition = EnterTransition.None
    override val popExit: ExitTransition = ExitTransition.None
}

class TreeNavigatorExtendedTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // navigateAndClearAll
    // =========================================================================

    test("navigateAndClearAll clears deeply nested stack") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)
        navigator.navigate(SettingsDest)
        navigator.navigate(ProfileDest)

        navigator.navigateAndClearAll(ListDest)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 1
        navigator.currentDestination.value shouldBe ListDest
        navigator.previousDestination.value.shouldBeNull()
        navigator.canNavigateBack.value.shouldBeFalse()
    }

    // =========================================================================
    // navigateAndClearTo
    // =========================================================================

    test("navigateAndClearTo with null clearRoute appends without clearing") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.navigateAndClearTo(SettingsDest, null, false)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 3
        navigator.currentDestination.value shouldBe SettingsDest
    }

    // =========================================================================
    // navigateAndReplace
    // =========================================================================

    test("navigateAndReplace keeps stack size unchanged") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)
        navigator.navigate(SettingsDest)

        navigator.navigateAndReplace(ProfileDest)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 3
        navigator.currentDestination.value shouldBe ProfileDest
        navigator.previousDestination.value shouldBe DetailDest
    }

    test("navigateAndReplace with transition triggers InProgress state") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.navigateAndReplace(SettingsDest, TestTransition)

        navigator.transitionState.value.shouldBeInstanceOf<TransitionState.InProgress>()
    }

    test("navigateAndReplace on single-screen stack replaces the only screen") {
        val navigator = TreeNavigator.withDestination(HomeDest)

        navigator.navigateAndReplace(DetailDest)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 1
        navigator.currentDestination.value shouldBe DetailDest
    }

    // =========================================================================
    // navigateToPane
    // =========================================================================

    test("navigateToPane replaces supporting pane content") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("list"), NodeKey("primary-stack"), ListDest)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("detail"), NodeKey("supporting-stack"), DetailDest)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.navigateToPane(SettingsDest, PaneRole.Supporting)

        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
        (supportingStack.children[0] as ScreenNode).destination shouldBe SettingsDest
    }

    test("navigateToPane throws when no PaneNode exists") {
        val navigator = TreeNavigator.withDestination(HomeDest)

        shouldThrow<IllegalStateException> {
            navigator.navigateToPane(DetailDest, PaneRole.Supporting)
        }
    }

    test("navigateToPane creates new stack for unconfigured role") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("list"), NodeKey("primary-stack"), ListDest)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.navigateToPane(DetailDest, PaneRole.Supporting)

        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingContent = panes.paneContent(PaneRole.Supporting)
        supportingContent.shouldNotBeNull()
        val stack = supportingContent.shouldBeInstanceOf<StackNode>()
        stack.children.size shouldBe 1
        (stack.children[0] as ScreenNode).destination shouldBe DetailDest
    }

    // =========================================================================
    // clearPane
    // =========================================================================

    test("clearPane does nothing when pane has single child") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDest)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("detail1"), NodeKey("supporting-stack"), DetailDest)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)
        val stateBefore = navigator.state.value

        navigator.clearPane(PaneRole.Supporting)

        // State should be unchanged because only 1 child
        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
    }

    test("clearPane reports error for unconfigured pane role") {
        var errorReported = false
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDest)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)
        navigator.errorHandler = NavigationErrorHandler { _, _, _ -> errorReported = true }

        navigator.clearPane(PaneRole.Extra)

        errorReported.shouldBeTrue()
    }

    // =========================================================================
    // navigateBackInPane
    // =========================================================================

    test("navigateBackInPane pops correctly from supporting pane with 2 items") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDest)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("detail1"), NodeKey("supporting-stack"), DetailDest),
                            ScreenNode(NodeKey("detail2"), NodeKey("supporting-stack"), SettingsDest)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        val result = navigator.navigateBackInPane(PaneRole.Supporting)

        result.shouldBeTrue()
        val root = navigator.state.value as StackNode
        val panes = root.children[0] as PaneNode
        val supportingStack = panes.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 1
        (supportingStack.children[0] as ScreenNode).destination shouldBe DetailDest
    }

    // =========================================================================
    // createRootStack wrapping behavior
    // =========================================================================

    test("constructor wraps non-StackNode initial state in StackNode") {
        val screen = ScreenNode(
            key = NodeKey("screen1"),
            parentKey = null,
            destination = HomeDest
        )
        val navigator = TreeNavigator(initialState = screen, coroutineContext = Dispatchers.Unconfined)

        val state = navigator.state.value
        state.shouldBeInstanceOf<StackNode>()
        state.children.size shouldBe 1
    }

    test("constructor wraps TabNode initial state in StackNode") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDest)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val navigator = TreeNavigator(initialState = tabNode, coroutineContext = Dispatchers.Unconfined)

        val state = navigator.state.value
        state.shouldBeInstanceOf<StackNode>()
        state.children.size shouldBe 1
        state.children[0].shouldBeInstanceOf<TabNode>()
    }

    test("constructor uses StackNode with null parentKey directly") {
        val rootStack = StackNode(
            key = NodeKey("my-root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("my-root"), HomeDest)
            )
        )
        val navigator = TreeNavigator(initialState = rootStack, coroutineContext = Dispatchers.Unconfined)

        val state = navigator.state.value as StackNode
        state.key shouldBe NodeKey("my-root")
    }

    test("constructor wraps StackNode with non-null parentKey") {
        val childStack = StackNode(
            key = NodeKey("child"),
            parentKey = NodeKey("some-parent"),
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("child"), HomeDest)
            )
        )
        val navigator = TreeNavigator(initialState = childStack, coroutineContext = Dispatchers.Unconfined)

        val state = navigator.state.value as StackNode
        // Should be wrapped in a new root
        state.parentKey.shouldBeNull()
        state.children.size shouldBe 1
        (state.children[0] as StackNode).key shouldBe NodeKey("child")
    }

    // =========================================================================
    // navigate with destination transition
    // =========================================================================

    test("navigate uses destination's own transition when no explicit transition given") {
        val destWithTransition = object : NavDestination {
            override val transition: NavigationTransition = TestTransition
        }
        val navigator = TreeNavigator.withDestination(HomeDest)

        navigator.navigate(destWithTransition)

        navigator.transitionState.value.shouldBeInstanceOf<TransitionState.InProgress>()
    }

    test("navigate with explicit transition overrides destination transition") {
        val destWithTransition = object : NavDestination {
            override val transition: NavigationTransition = TestTransition
        }
        val customTransition = object : NavigationTransition {
            override val enter = EnterTransition.None
            override val exit = ExitTransition.None
            override val popEnter = EnterTransition.None
            override val popExit = ExitTransition.None
        }
        val navigator = TreeNavigator.withDestination(HomeDest)

        navigator.navigate(destWithTransition, customTransition)

        val state = navigator.transitionState.value as TransitionState.InProgress
        state.transition shouldBe customTransition
    }

    // =========================================================================
    // errorHandler
    // =========================================================================

    test("errorHandler defaults to LogAndRecover") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)
        navigator.errorHandler shouldBe NavigationErrorHandler.LogAndRecover
    }

    test("errorHandler can be set to ThrowOnError") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)
        navigator.errorHandler = NavigationErrorHandler.ThrowOnError

        // clearPane without PaneNode should throw
        shouldThrow<IllegalStateException> {
            navigator.clearPane(PaneRole.Primary)
        }
    }

    // =========================================================================
    // navigateBack edge cases
    // =========================================================================

    test("navigateBack on empty stack returns false") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)

        val result = navigator.navigateBack()

        result.shouldBeFalse()
    }

    test("navigateBack on single screen returns false") {
        val navigator = TreeNavigator.withDestination(HomeDest)

        val result = navigator.navigateBack()

        result.shouldBeFalse()
    }

    test("navigateBack restores previous destination after multiple navigations") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)
        navigator.navigate(SettingsDest)
        navigator.navigate(ProfileDest)

        navigator.navigateBack()
        navigator.currentDestination.value shouldBe SettingsDest
        navigator.previousDestination.value shouldBe DetailDest

        navigator.navigateBack()
        navigator.currentDestination.value shouldBe DetailDest
        navigator.previousDestination.value shouldBe HomeDest
    }

    // =========================================================================
    // Transition management
    // =========================================================================

    test("completeTransition after navigate resets to Idle") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest, TestTransition)

        navigator.transitionState.value.shouldBeInstanceOf<TransitionState.InProgress>()

        navigator.completeTransition()

        navigator.transitionState.value shouldBe TransitionState.Idle
    }

    test("updateTransitionProgress changes progress value") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest, TestTransition)

        navigator.updateTransitionProgress(0.75f)

        val state = navigator.transitionState.value as TransitionState.InProgress
        state.progress shouldBe 0.75f
    }

    test("predictive back full flow: start -> update -> commit") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.startPredictiveBack()
        navigator.transitionState.value.shouldBeInstanceOf<TransitionState.PredictiveBack>()

        navigator.updatePredictiveBack(0.5f, 0.3f, 0.4f)
        val pbState = navigator.transitionState.value as TransitionState.PredictiveBack
        pbState.progress shouldBe 0.5f

        navigator.commitPredictiveBack()
        navigator.transitionState.value shouldBe TransitionState.Idle
        navigator.currentDestination.value shouldBe HomeDest
    }

    test("predictive back full flow: start -> update -> cancel") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.startPredictiveBack()
        navigator.updatePredictiveBack(0.3f, 0.1f, 0.2f)

        navigator.cancelPredictiveBack()
        navigator.transitionState.value shouldBe TransitionState.Idle
        // After cancel, we should still be on the same destination
        navigator.currentDestination.value shouldBe DetailDest
    }

    // =========================================================================
    // navigateWithResult / navigateBackWithResult
    // =========================================================================

    test("navigateBackWithResult pops the current screen") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.navigateBackWithResult("hello")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult without pending result still pops") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)

        navigator.navigateBackWithResult("test")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("navigateBackWithResult from root stays on root") {
        val navigator = TreeNavigator.withDestination(HomeDest)

        navigator.navigateBackWithResult("test")

        navigator.currentDestination.value shouldBe HomeDest
    }

    test("resultManager completeResultSync completes deferred") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)
        val screenKey = navigator.state.value.activeLeaf()?.key?.value!!

        val deferred = navigator.resultManager.requestResult(screenKey)

        navigator.resultManager.completeResultSync(screenKey, "result-value")

        deferred.isCompleted.shouldBeTrue()
        deferred.await() shouldBe "result-value"
    }

    test("resultManager cancelResult completes with null") {
        val navigator = TreeNavigator.withDestination(HomeDest)
        navigator.navigate(DetailDest)
        val screenKey = navigator.state.value.activeLeaf()?.key?.value!!

        val deferred = navigator.resultManager.requestResult(screenKey)

        navigator.resultManager.cancelResult(screenKey)

        deferred.isCompleted.shouldBeTrue()
        deferred.await().shouldBeNull()
    }

    // =========================================================================
    // resultManager
    // =========================================================================

    test("resultManager is accessible and functional") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)

        navigator.resultManager.shouldNotBeNull()
        navigator.resultManager.pendingCount() shouldBe 0
    }

    // =========================================================================
    // isPaneAvailable edge cases
    // =========================================================================

    test("isPaneAvailable for Extra role when only Primary and Supporting configured") {
        val paneNode = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("list"), NodeKey("panes"), ListDest)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode(NodeKey("detail"), NodeKey("panes"), DetailDest)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.isPaneAvailable(PaneRole.Extra).shouldBeFalse()
    }

    // =========================================================================
    // activeTabIndex
    // =========================================================================

    test("activeTabIndex reflects initial active tab") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDest)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), DetailDest)
                    )
                ),
                StackNode(
                    NodeKey("tab2"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s3"), NodeKey("tab2"), SettingsDest)
                    )
                )
            ),
            activeStackIndex = 2
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.activeTabIndex shouldBe 2
    }

    // =========================================================================
    // destroy
    // =========================================================================

    test("destroy cancels navigator scope") {
        val navigator = TreeNavigator.withDestination(HomeDest)

        navigator.destroy()

        // After destroy, operations should still not crash (graceful)
        // The navigator scope is cancelled but state is still available
        navigator.state.value.shouldNotBeNull()
    }

    // =========================================================================
    // navigate into tabs (pushes to active tab stack)
    // =========================================================================

    test("navigate pushes into deeply nested active tab stack") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDest)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), DetailDest)
                    )
                )
            ),
            activeStackIndex = 1
        )
        val initialState = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )
        val navigator = TreeNavigator(initialState = initialState, coroutineContext = Dispatchers.Unconfined)

        navigator.navigate(SettingsDest)

        val root = navigator.state.value as StackNode
        val tabs = root.children[0] as TabNode
        // Tab 1 (active) should have 2 screens now
        tabs.stacks[1].children.size shouldBe 2
        // Tab 0 (inactive) should be unchanged
        tabs.stacks[0].children.size shouldBe 1
        navigator.currentDestination.value shouldBe SettingsDest
    }

    // =========================================================================
    // windowSizeClass
    // =========================================================================

    test("windowSizeClass is initially null") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)
        navigator.windowSizeClass.shouldBeNull()
    }

    // =========================================================================
    // backHandlerRegistry
    // =========================================================================

    test("backHandlerRegistry is initially null") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)
        navigator.backHandlerRegistry.shouldBeNull()
    }

    // =========================================================================
    // navigate to empty navigator (error recovery path)
    // =========================================================================

    test("navigate to empty navigator creates root stack with screen") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)

        navigator.navigate(HomeDest)

        val state = navigator.state.value
        state.shouldBeInstanceOf<StackNode>()
        state.children.size shouldBe 1
        navigator.currentDestination.value shouldBe HomeDest
    }

    // =========================================================================
    // deep link with DeepLink object
    // =========================================================================

    test("handleDeepLink returns false for unknown deep link") {
        val navigator = TreeNavigator.withDestination(HomeDest)

        val result = navigator.handleDeepLink("unknown://path")

        result.shouldBeFalse()
    }
})
