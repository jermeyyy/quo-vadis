@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.util.WindowSizeClass
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.navigator.NavigationErrorHandler
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.testing.withDestination
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers

private object CovHomeDest : NavDestination {
    override fun toString(): String = "home"
}

private object CovDetailDest : NavDestination {
    override fun toString(): String = "detail"
}

private object CovSettingsDest : NavDestination {
    override fun toString(): String = "settings"
}

private object CovProfileDest : NavDestination {
    override fun toString(): String = "profile"
}

private object CovListDest : NavDestination {
    override fun toString(): String = "list"
}

/**
 * Additional TreeNavigator coverage tests targeting undertested areas:
 * - findFirst / findFirstOfType extension functions
 * - destroy() lifecycle
 * - navigateAndClearTo with clearRoute
 * - errorHandler callback behavior
 * - onBack with no handler registry
 * - windowSizeClass effect on performTreePop
 * - pane operations edge cases
 * - computePreviousDestination edge cases
 */
class TreeNavigatorCoverageTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // findFirst and findFirstOfType extension functions
    // =========================================================================

    test("findFirst returns matching node type from tree") {
        val screen = ScreenNode(NodeKey("s1"), NodeKey("tabs"), CovHomeDest)
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen))
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = NodeKey("root"),
            stacks = listOf(tab0), activeStackIndex = 0
        )
        val root = StackNode(NodeKey("root"), null, listOf(tabs))

        root.findFirst<TabNode>().shouldNotBeNull()
        root.findFirst<TabNode>()!!.key shouldBe NodeKey("tabs")
    }

    test("findFirst returns null when type not found") {
        val screen = ScreenNode(NodeKey("s"), NodeKey("stack"), CovHomeDest)
        val stack = StackNode(NodeKey("stack"), null, listOf(screen))

        stack.findFirst<TabNode>().shouldBeNull()
        stack.findFirst<PaneNode>().shouldBeNull()
    }

    test("findFirst returns self when node is requested type") {
        val screen = ScreenNode(NodeKey("s"), null, CovHomeDest)
        screen.findFirst<ScreenNode>()!!.key shouldBe NodeKey("s")
    }

    test("findFirst finds PaneNode in nested tree") {
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("stack"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("s"), NodeKey("panes"), CovHomeDest)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val stack = StackNode(NodeKey("stack"), null, listOf(panes))

        stack.findFirst<PaneNode>()!!.key shouldBe NodeKey("panes")
    }

    test("findFirstOfType searches through TabNode stacks") {
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("tab0"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("s"), NodeKey("panes"), CovHomeDest)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(panes))
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(tab0), activeStackIndex = 0
        )

        tabs.findFirst<PaneNode>()!!.key shouldBe NodeKey("panes")
    }

    test("findFirstOfType searches through PaneNode configurations") {
        val innerTabs = TabNode(
            key = NodeKey("inner-tabs"), parentKey = NodeKey("panes"),
            stacks = listOf(StackNode(NodeKey("t0"), NodeKey("inner-tabs"), emptyList())),
            activeStackIndex = 0
        )
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(innerTabs)
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.findFirst<TabNode>()!!.key shouldBe NodeKey("inner-tabs")
    }

    // =========================================================================
    // destroy lifecycle
    // =========================================================================

    test("destroy cancels navigator scope") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.navigate(CovDetailDest)
        navigator.destroy()

        // After destroy, navigator state still holds last value
        navigator.currentDestination.value shouldBe CovDetailDest
    }

    // =========================================================================
    // navigateAndClearTo with clearRoute
    // =========================================================================

    test("navigateAndClearTo with null clearRoute just pushes") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.navigate(CovProfileDest)

        navigator.navigateAndClearTo(CovSettingsDest, null, false)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 3
        navigator.currentDestination.value shouldBe CovSettingsDest
    }

    // =========================================================================
    // errorHandler behavior
    // =========================================================================

    test("errorHandler LogAndRecover does not throw on navigate error") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)
        navigator.errorHandler shouldBe NavigationErrorHandler.LogAndRecover

        // Navigate to set up state, then trigger error via clearPane on non-pane tree
        navigator.navigate(CovHomeDest)
        navigator.clearPane(PaneRole.Primary)
        navigator.currentDestination.value shouldBe CovHomeDest
    }

    test("errorHandler ThrowOnError throws on navigation errors") {
        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)
        navigator.errorHandler = NavigationErrorHandler.ThrowOnError

        navigator.navigate(CovHomeDest)
        shouldThrow<Exception> {
            navigator.clearPane(PaneRole.Primary)
        }
    }

    test("custom errorHandler callback is invoked") {
        var capturedError: Throwable? = null
        var capturedContext: String? = null
        val customHandler = NavigationErrorHandler { error, _, context ->
            capturedError = error
            capturedContext = context
        }

        val navigator = TreeNavigator(coroutineContext = Dispatchers.Unconfined)
        navigator.errorHandler = customHandler

        // clearPane on no PaneNode triggers error
        navigator.navigate(CovHomeDest)
        navigator.clearPane(PaneRole.Primary)

        capturedError.shouldNotBeNull()
        capturedContext shouldBe "clearPane"
    }

    // =========================================================================
    // clearPane edge cases
    // =========================================================================

    test("clearPane with unconfigured role reports error") {
        var errorReported = false
        val handler = NavigationErrorHandler { _, _, _ -> errorReported = true }

        val primaryScreen = ScreenNode(NodeKey("s"), NodeKey("pstack"), CovHomeDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(primaryScreen))
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )
        navigator.errorHandler = handler

        navigator.clearPane(PaneRole.Supporting)

        errorReported.shouldBeTrue()
    }

    test("clearPane with multiple items clears to root") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("pstack"), CovHomeDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("pstack"), CovDetailDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(s1, s2))
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )

        navigator.clearPane(PaneRole.Primary)

        // After clear, pane stack should have only 1 child
        val updatedRoot = navigator.state.value as StackNode
        val updatedPanes = updatedRoot.children.first() as PaneNode
        val updatedStack = updatedPanes.paneConfigurations[PaneRole.Primary]!!.content as StackNode
        updatedStack.children.size shouldBe 1
        updatedStack.children.first().shouldBeInstanceOf<ScreenNode>()
    }

    // =========================================================================
    // onBack with no backHandlerRegistry
    // =========================================================================

    test("onBack pops the active stack when no handler registry") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.navigate(CovDetailDest)

        // backHandlerRegistry is null by default
        val result = navigator.onBack()

        result.shouldBeTrue()
        navigator.currentDestination.value shouldBe CovHomeDest
    }

    test("onBack returns false at root when no handler registry") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)

        val result = navigator.onBack()

        result.shouldBeFalse()
    }

    // =========================================================================
    // navigateToPane edge case: unconfigured role creates new stack
    // =========================================================================

    test("navigateToPane to existing role replaces content") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("pstack"), CovHomeDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(s1))
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        NodeKey("sstack"), NodeKey("panes"),
                        listOf(ScreenNode(NodeKey("s2"), NodeKey("sstack"), CovDetailDest))
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )

        navigator.navigateToPane(CovSettingsDest, PaneRole.Supporting)

        navigator.currentDestination.value shouldBe CovSettingsDest
    }

    // =========================================================================
    // isPaneAvailable / paneContent via navigator
    // =========================================================================

    test("isPaneAvailable works through navigator") {
        val pstack = StackNode(
            NodeKey("pstack"), NodeKey("panes"),
            listOf(ScreenNode(NodeKey("s1"), NodeKey("pstack"), CovHomeDest))
        )
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )

        navigator.isPaneAvailable(PaneRole.Primary).shouldBeTrue()
        navigator.isPaneAvailable(PaneRole.Supporting).shouldBeFalse()
    }

    test("paneContent returns correct content via navigator") {
        val screen = ScreenNode(NodeKey("s1"), NodeKey("pstack"), CovHomeDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(screen))
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )

        navigator.paneContent(PaneRole.Primary).shouldNotBeNull()
        navigator.paneContent(PaneRole.Supporting).shouldBeNull()
    }

    // =========================================================================
    // navigateBackInPane via navigator
    // =========================================================================

    test("navigateBackInPane pops from specified pane via navigator") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("pstack"), CovHomeDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("pstack"), CovDetailDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(s1, s2))
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )

        val result = navigator.navigateBackInPane(PaneRole.Primary)
        result.shouldBeTrue()
    }

    test("navigateBackInPane returns false when no PaneNode") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)

        val result = navigator.navigateBackInPane(PaneRole.Primary)
        result.shouldBeFalse()
    }

    // =========================================================================
    // computePreviousDestination edge cases
    // =========================================================================

    test("previousDestination is null for single-screen navigator") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.previousDestination.value.shouldBeNull()
    }

    test("previousDestination updates through navigation chain") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.navigate(CovDetailDest)
        navigator.navigate(CovSettingsDest)

        navigator.previousDestination.value shouldBe CovDetailDest
    }

    test("previousDestination after navigateAndClearAll is null") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.navigate(CovDetailDest)

        navigator.navigateAndClearAll(CovSettingsDest)

        navigator.previousDestination.value.shouldBeNull()
    }

    // =========================================================================
    // Tab operations through navigator
    // =========================================================================

    test("activeTabIndex after switchActiveTab") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), CovHomeDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("tab1"), CovDetailDest)
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(s1))
        val tab1 = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(s2))
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = NodeKey("root"),
            stacks = listOf(tab0, tab1), activeStackIndex = 0
        )
        val root = StackNode(NodeKey("root"), null, listOf(tabs))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )

        navigator.activeTabIndex shouldBe 0
        val newState = TreeMutator.switchActiveTab(navigator.state.value, 1)
        @Suppress("DEPRECATION")
        navigator.updateState(newState)
        navigator.activeTabIndex shouldBe 1
        navigator.currentDestination.value shouldBe CovDetailDest
    }

    test("activeTabIndex is null when no TabNode") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.activeTabIndex.shouldBeNull()
    }

    // =========================================================================
    // navigateAndReplace via navigator
    // =========================================================================

    test("navigateAndReplace with no transition") {
        val navigator = TreeNavigator.withDestination(CovHomeDest)
        navigator.navigate(CovDetailDest)

        navigator.navigateAndReplace(CovSettingsDest)

        val state = navigator.state.value as StackNode
        state.children.size shouldBe 2
        navigator.currentDestination.value shouldBe CovSettingsDest
        navigator.previousDestination.value shouldBe CovHomeDest
    }

    // =========================================================================
    // navigateToPane unconfigured role creates new stack
    // =========================================================================

    test("navigateToPane to unconfigured role creates new configuration") {
        val primaryScreen = ScreenNode(NodeKey("s1"), NodeKey("pstack"), CovHomeDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(primaryScreen))
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )

        navigator.navigateToPane(CovDetailDest, PaneRole.Supporting)

        navigator.currentDestination.value shouldBe CovDetailDest
    }

    // =========================================================================
    // performTreePop with non-compact windowSizeClass
    // =========================================================================

    test("onBack with expanded windowSizeClass on pane structure") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("pstack"), CovHomeDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("pstack"), CovDetailDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(s1, s2))
        val sstack = StackNode(
            NodeKey("sstack"), NodeKey("panes"),
            listOf(ScreenNode(NodeKey("s3"), NodeKey("sstack"), CovSettingsDest))
        )
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack),
                PaneRole.Supporting to PaneConfiguration(sstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        val root = StackNode(NodeKey("root"), null, listOf(panes))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )
        navigator.windowSizeClass = WindowSizeClass.Expanded

        val result = navigator.onBack()
        result.shouldBeTrue()
    }

    test("onBack with compact windowSizeClass on tab structure pops active tab") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), CovHomeDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("tab0"), CovDetailDest)
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(s1, s2))
        val tab1 = StackNode(
            NodeKey("tab1"), NodeKey("tabs"),
            listOf(ScreenNode(NodeKey("s3"), NodeKey("tab1"), CovSettingsDest))
        )
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = NodeKey("root"),
            stacks = listOf(tab0, tab1), activeStackIndex = 0
        )
        val root = StackNode(NodeKey("root"), null, listOf(tabs))
        val navigator = TreeNavigator(
            initialState = root,
            coroutineContext = Dispatchers.Unconfined
        )
        navigator.windowSizeClass = WindowSizeClass.Compact

        val result = navigator.onBack()
        result.shouldBeTrue()
        // Should have popped from tab0
    }

    // =========================================================================
    // TabNode equals/hashCode coverage
    // =========================================================================

    test("TabNode equals and hashCode for identical instances") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), CovHomeDest)
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(s1))

        val tabs1 = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(tab0), activeStackIndex = 0
        )
        val tabs2 = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(tab0), activeStackIndex = 0
        )

        (tabs1 == tabs2).shouldBeTrue()
        tabs1.hashCode() shouldBe tabs2.hashCode()
    }

    test("TabNode equals returns false for different activeStackIndex") {
        val t0 = StackNode(NodeKey("t0"), NodeKey("tabs"), emptyList())
        val t1 = StackNode(NodeKey("t1"), NodeKey("tabs"), emptyList())

        val tabs1 = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(t0, t1), activeStackIndex = 0
        )
        val tabs2 = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(t0, t1), activeStackIndex = 1
        )

        (tabs1 == tabs2).shouldBeFalse()
    }

    test("TabNode equals returns false for different keys") {
        val t0 = StackNode(NodeKey("t0"), NodeKey("tabs"), emptyList())

        val tabs1 = TabNode(
            key = NodeKey("tabs-1"), parentKey = null,
            stacks = listOf(t0), activeStackIndex = 0
        )
        val tabs2 = TabNode(
            key = NodeKey("tabs-2"), parentKey = null,
            stacks = listOf(t0), activeStackIndex = 0
        )

        (tabs1 == tabs2).shouldBeFalse()
        tabs1.hashCode() shouldNotBe tabs2.hashCode()
    }

    // =========================================================================
    // PaneNode equals/hashCode coverage
    // =========================================================================

    test("PaneNode equals and hashCode for identical instances") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("panes"), CovHomeDest)
        val panes1 = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1)
            ),
            activePaneRole = PaneRole.Primary
        )
        val panes2 = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1)
            ),
            activePaneRole = PaneRole.Primary
        )

        (panes1 == panes2).shouldBeTrue()
        panes1.hashCode() shouldBe panes2.hashCode()
    }

    test("PaneNode equals returns false for different activePaneRole") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("panes"), CovHomeDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("panes"), CovDetailDest)

        val panes1 = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1),
                PaneRole.Supporting to PaneConfiguration(s2)
            ),
            activePaneRole = PaneRole.Primary
        )
        val panes2 = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1),
                PaneRole.Supporting to PaneConfiguration(s2)
            ),
            activePaneRole = PaneRole.Supporting
        )

        (panes1 == panes2).shouldBeFalse()
    }

    test("PaneNode equals returns false for different keys") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("panes"), CovHomeDest)

        val panes1 = PaneNode(
            key = NodeKey("panes-1"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1)
            ),
            activePaneRole = PaneRole.Primary
        )
        val panes2 = PaneNode(
            key = NodeKey("panes-2"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1)
            ),
            activePaneRole = PaneRole.Primary
        )

        (panes1 == panes2).shouldBeFalse()
        panes1.hashCode() shouldNotBe panes2.hashCode()
    }
})
