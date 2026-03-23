@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.result

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

private object PushTestDest : NavDestination

class PushStrategyTest : FunSpec({

    // =========================================================================
    // PushToStack
    // =========================================================================

    test("PushToStack holds targetStack") {
        val stack = StackNode(NodeKey("s"), null, emptyList())
        val strategy = PushStrategy.PushToStack(stack)

        strategy.targetStack shouldBe stack
    }

    test("PushToStack is instance of PushStrategy") {
        val stack = StackNode(NodeKey("s"), null, emptyList())
        val strategy: PushStrategy = PushStrategy.PushToStack(stack)

        strategy.shouldBeInstanceOf<PushStrategy.PushToStack>()
    }

    test("PushToStack data class equality") {
        val stack = StackNode(NodeKey("s"), null, emptyList())
        val a = PushStrategy.PushToStack(stack)
        val b = PushStrategy.PushToStack(stack)

        a shouldBe b
    }

    // =========================================================================
    // SwitchToTab
    // =========================================================================

    test("SwitchToTab holds tabNode and tabIndex") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("t0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("t0"), PushTestDest))
                )
            ),
            activeStackIndex = 0
        )
        val strategy = PushStrategy.SwitchToTab(tabNode, tabIndex = 0)

        strategy.tabNode shouldBe tabNode
        strategy.tabIndex shouldBe 0
    }

    test("SwitchToTab is instance of PushStrategy") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("t0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("t0"), PushTestDest))
                )
            ),
            activeStackIndex = 0
        )
        val strategy: PushStrategy = PushStrategy.SwitchToTab(tabNode, 0)

        strategy.shouldBeInstanceOf<PushStrategy.SwitchToTab>()
    }

    test("SwitchToTab data class equality") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("t0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("t0"), PushTestDest))
                )
            ),
            activeStackIndex = 0
        )
        val a = PushStrategy.SwitchToTab(tabNode, 1)
        val b = PushStrategy.SwitchToTab(tabNode, 1)

        a shouldBe b
    }

    test("SwitchToTab inequality with different tabIndex") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("t0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("t0"), PushTestDest))
                ),
                StackNode(
                    NodeKey("t1"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("t1"), PushTestDest))
                )
            ),
            activeStackIndex = 0
        )
        val a = PushStrategy.SwitchToTab(tabNode, 0)
        val b = PushStrategy.SwitchToTab(tabNode, 1)

        a shouldNotBe b
    }

    // =========================================================================
    // PushToPaneStack
    // =========================================================================

    test("PushToPaneStack holds paneNode and role") {
        val primaryStack = StackNode(
            NodeKey("ps"),
            NodeKey("pane"),
            listOf(ScreenNode(NodeKey("s"), NodeKey("ps"), PushTestDest))
        )
        val paneNode = PaneNode(
            key = NodeKey("pane"),
            parentKey = null,
            paneConfigurations = mapOf(PaneRole.Primary to PaneConfiguration(primaryStack)),
            activePaneRole = PaneRole.Primary
        )
        val strategy = PushStrategy.PushToPaneStack(paneNode, PaneRole.Primary)

        strategy.paneNode shouldBe paneNode
        strategy.role shouldBe PaneRole.Primary
    }

    test("PushToPaneStack is instance of PushStrategy") {
        val primaryStack = StackNode(
            NodeKey("ps"),
            NodeKey("pane"),
            listOf(ScreenNode(NodeKey("s"), NodeKey("ps"), PushTestDest))
        )
        val paneNode = PaneNode(
            key = NodeKey("pane"),
            parentKey = null,
            paneConfigurations = mapOf(PaneRole.Primary to PaneConfiguration(primaryStack)),
            activePaneRole = PaneRole.Primary
        )
        val strategy: PushStrategy = PushStrategy.PushToPaneStack(paneNode, PaneRole.Primary)

        strategy.shouldBeInstanceOf<PushStrategy.PushToPaneStack>()
    }

    test("PushToPaneStack data class equality") {
        val primaryStack = StackNode(
            NodeKey("ps"),
            NodeKey("pane"),
            listOf(ScreenNode(NodeKey("s"), NodeKey("ps"), PushTestDest))
        )
        val paneNode = PaneNode(
            key = NodeKey("pane"),
            parentKey = null,
            paneConfigurations = mapOf(PaneRole.Primary to PaneConfiguration(primaryStack)),
            activePaneRole = PaneRole.Primary
        )
        val a = PushStrategy.PushToPaneStack(paneNode, PaneRole.Primary)
        val b = PushStrategy.PushToPaneStack(paneNode, PaneRole.Primary)

        a shouldBe b
    }

    // =========================================================================
    // PushOutOfScope
    // =========================================================================

    test("PushOutOfScope holds parentStack") {
        val stack = StackNode(NodeKey("parent"), null, emptyList())
        val strategy = PushStrategy.PushOutOfScope(stack)

        strategy.parentStack shouldBe stack
    }

    test("PushOutOfScope is instance of PushStrategy") {
        val stack = StackNode(NodeKey("parent"), null, emptyList())
        val strategy: PushStrategy = PushStrategy.PushOutOfScope(stack)

        strategy.shouldBeInstanceOf<PushStrategy.PushOutOfScope>()
    }

    test("PushOutOfScope data class equality") {
        val stack = StackNode(NodeKey("parent"), null, emptyList())
        val a = PushStrategy.PushOutOfScope(stack)
        val b = PushStrategy.PushOutOfScope(stack)

        a shouldBe b
    }

    // =========================================================================
    // Exhaustive when
    // =========================================================================

    test("exhaustive when covers all variants") {
        val stack = StackNode(NodeKey("s"), null, emptyList())
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(
                    NodeKey("t0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("t0"), PushTestDest))
                )
            ),
            activeStackIndex = 0
        )
        val primaryStack = StackNode(
            NodeKey("ps"),
            NodeKey("pane"),
            listOf(ScreenNode(NodeKey("s1"), NodeKey("ps"), PushTestDest))
        )
        val paneNode = PaneNode(
            key = NodeKey("pane"),
            parentKey = null,
            paneConfigurations = mapOf(PaneRole.Primary to PaneConfiguration(primaryStack)),
            activePaneRole = PaneRole.Primary
        )

        val strategies = listOf<PushStrategy>(
            PushStrategy.PushToStack(stack),
            PushStrategy.SwitchToTab(tabNode, 0),
            PushStrategy.PushToPaneStack(paneNode, PaneRole.Primary),
            PushStrategy.PushOutOfScope(stack)
        )

        val labels = strategies.map { strategy ->
            when (strategy) {
                is PushStrategy.PushToStack -> "push_to_stack"
                is PushStrategy.SwitchToTab -> "switch_to_tab"
                is PushStrategy.PushToPaneStack -> "push_to_pane"
                is PushStrategy.PushOutOfScope -> "push_out_of_scope"
            }
        }

        labels shouldBe listOf(
            "push_to_stack",
            "switch_to_tab",
            "push_to_pane",
            "push_out_of_scope"
        )
    }
})
