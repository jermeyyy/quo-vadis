package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.isPane
import com.jermey.quo.vadis.core.navigation.node.isScreen
import com.jermey.quo.vadis.core.navigation.node.isStack
import com.jermey.quo.vadis.core.navigation.node.isTab
import com.jermey.quo.vadis.core.navigation.node.requirePane
import com.jermey.quo.vadis.core.navigation.node.requireScreen
import com.jermey.quo.vadis.core.navigation.node.requireStack
import com.jermey.quo.vadis.core.navigation.node.requireTab
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Tests for NavNode type-checking and type-requiring extension functions
 * defined in NavNodeTypeChecks.kt.
 */
@OptIn(InternalQuoVadisApi::class)
class NavNodeTypeChecksTest : FunSpec() {

    object TestDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    init {

    val screenNode: NavNode = ScreenNode(
        key = NodeKey("screen-1"),
        parentKey = null,
        destination = TestDestination,
    )

    val stackNode: NavNode = StackNode(
        key = NodeKey("stack-1"),
        parentKey = null,
        children = listOf(
            ScreenNode(NodeKey("s1"), NodeKey("stack-1"), TestDestination),
        ),
    )

    val tabNode: NavNode = TabNode(
        key = NodeKey("tab-1"),
        parentKey = null,
        stacks = listOf(
            StackNode(
                key = NodeKey("tab-stack-1"),
                parentKey = NodeKey("tab-1"),
                children = listOf(
                    ScreenNode(NodeKey("ts1"), NodeKey("tab-stack-1"), TestDestination),
                ),
            ),
        ),
        activeStackIndex = 0,
    )

    val paneNode: NavNode = PaneNode(
        key = NodeKey("pane-1"),
        parentKey = null,
        paneConfigurations = mapOf(
            PaneRole.Primary to PaneConfiguration(
                content = StackNode(
                    key = NodeKey("pane-stack-1"),
                    parentKey = NodeKey("pane-1"),
                    children = listOf(
                        ScreenNode(NodeKey("ps1"), NodeKey("pane-stack-1"), TestDestination),
                    ),
                ),
            ),
        ),
        activePaneRole = PaneRole.Primary,
        backBehavior = PaneBackBehavior.PopLatest,
    )

    // =========================================================================
    // isScreen
    // =========================================================================

    test("isScreen returns true for ScreenNode") {
        screenNode.isScreen().shouldBeTrue()
    }

    test("isScreen returns false for non-ScreenNode") {
        stackNode.isScreen().shouldBeFalse()
        tabNode.isScreen().shouldBeFalse()
        paneNode.isScreen().shouldBeFalse()
    }

    // =========================================================================
    // isStack
    // =========================================================================

    test("isStack returns true for StackNode") {
        stackNode.isStack().shouldBeTrue()
    }

    test("isStack returns false for non-StackNode") {
        screenNode.isStack().shouldBeFalse()
        tabNode.isStack().shouldBeFalse()
        paneNode.isStack().shouldBeFalse()
    }

    // =========================================================================
    // isTab
    // =========================================================================

    test("isTab returns true for TabNode") {
        tabNode.isTab().shouldBeTrue()
    }

    test("isTab returns false for non-TabNode") {
        screenNode.isTab().shouldBeFalse()
        stackNode.isTab().shouldBeFalse()
        paneNode.isTab().shouldBeFalse()
    }

    // =========================================================================
    // isPane
    // =========================================================================

    test("isPane returns true for PaneNode") {
        paneNode.isPane().shouldBeTrue()
    }

    test("isPane returns false for non-PaneNode") {
        screenNode.isPane().shouldBeFalse()
        stackNode.isPane().shouldBeFalse()
        tabNode.isPane().shouldBeFalse()
    }

    // =========================================================================
    // requireScreen
    // =========================================================================

    test("requireScreen returns ScreenNode for ScreenNode") {
        val result = screenNode.requireScreen()
        result.key shouldBe NodeKey("screen-1")
    }

    test("requireScreen throws for non-ScreenNode") {
        shouldThrow<IllegalStateException> { stackNode.requireScreen() }
        shouldThrow<IllegalStateException> { tabNode.requireScreen() }
        shouldThrow<IllegalStateException> { paneNode.requireScreen() }
    }

    // =========================================================================
    // requireStack
    // =========================================================================

    test("requireStack returns StackNode for StackNode") {
        val result = stackNode.requireStack()
        result.key shouldBe NodeKey("stack-1")
    }

    test("requireStack throws for non-StackNode") {
        shouldThrow<IllegalStateException> { screenNode.requireStack() }
        shouldThrow<IllegalStateException> { tabNode.requireStack() }
        shouldThrow<IllegalStateException> { paneNode.requireStack() }
    }

    // =========================================================================
    // requireTab
    // =========================================================================

    test("requireTab returns TabNode for TabNode") {
        val result = tabNode.requireTab()
        result.key shouldBe NodeKey("tab-1")
    }

    test("requireTab throws for non-TabNode") {
        shouldThrow<IllegalStateException> { screenNode.requireTab() }
        shouldThrow<IllegalStateException> { stackNode.requireTab() }
        shouldThrow<IllegalStateException> { paneNode.requireTab() }
    }

    // =========================================================================
    // requirePane
    // =========================================================================

    test("requirePane returns PaneNode for PaneNode") {
        val result = paneNode.requirePane()
        result.key shouldBe NodeKey("pane-1")
    }

    test("requirePane throws for non-PaneNode") {
        shouldThrow<IllegalStateException> { screenNode.requirePane() }
        shouldThrow<IllegalStateException> { stackNode.requirePane() }
        shouldThrow<IllegalStateException> { tabNode.requirePane() }
    }

    } // init
}
