@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.node

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

private object TestDest : NavDestination

private object TestDest2 : NavDestination

/**
 * Tests for NavNode extension functions not covered by NavNodeTest:
 * - activeNodePath()
 * - fold()
 * - forEachNode()
 * - canHandleBackInternally()
 * Also covers additional branches in paneForRole, allPaneNodes, allTabNodes, allStackNodes, depth.
 */
class NavNodeExtensionsTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // activeNodePath
    // =========================================================================

    test("activeNodePath for ScreenNode returns single key") {
        val screen = ScreenNode(NodeKey("screen"), null, TestDest)
        val path = screen.activeNodePath()
        path shouldContainExactly listOf(NodeKey("screen"))
    }

    test("activeNodePath for StackNode returns leaf-first root-last") {
        val screen = ScreenNode(NodeKey("leaf"), NodeKey("stack"), TestDest)
        val stack = StackNode(NodeKey("stack"), null, listOf(screen))

        val path = stack.activeNodePath()
        path shouldContainExactly listOf(NodeKey("leaf"), NodeKey("stack"))
    }

    test("activeNodePath for empty StackNode returns only stack key") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())
        val path = stack.activeNodePath()
        path shouldContainExactly listOf(NodeKey("stack"))
    }

    test("activeNodePath through TabNode returns leaf-first root-last") {
        val screen = ScreenNode(NodeKey("leaf"), NodeKey("tab0"), TestDest)
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen))
        val tab1 = StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(tab0, tab1),
            activeStackIndex = 0
        )

        val path = tabs.activeNodePath()
        path shouldContainExactly listOf(NodeKey("leaf"), NodeKey("tab0"), NodeKey("tabs"))
    }

    test("activeNodePath through PaneNode returns active pane path") {
        val screen = ScreenNode(NodeKey("leaf"), NodeKey("pstack"), TestDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(screen))
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )

        val path = panes.activeNodePath()
        path shouldContainExactly listOf(NodeKey("leaf"), NodeKey("pstack"), NodeKey("panes"))
    }

    test("activeNodePath for deep nested tree") {
        val screen = ScreenNode(NodeKey("s"), NodeKey("inner"), TestDest)
        val innerStack = StackNode(NodeKey("inner"), NodeKey("tab0"), listOf(screen))
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(innerStack))
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tab0),
            activeStackIndex = 0
        )
        val root = StackNode(NodeKey("root"), null, listOf(tabs))

        val path = root.activeNodePath()
        path shouldContainExactly listOf(
            NodeKey("s"),
            NodeKey("inner"),
            NodeKey("tab0"),
            NodeKey("tabs"),
            NodeKey("root")
        )
    }

    // =========================================================================
    // fold
    // =========================================================================

    test("fold counts nodes in ScreenNode") {
        val screen = ScreenNode(NodeKey("s"), null, TestDest)
        val count = screen.fold(0, Int::plus) { 1 }
        count shouldBe 1
    }

    test("fold counts nodes in StackNode") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("stack"), TestDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("stack"), TestDest2)
        val stack = StackNode(NodeKey("stack"), null, listOf(s1, s2))

        val count = stack.fold(0, Int::plus) { 1 }
        count shouldBe 3 // stack + 2 screens
    }

    test("fold counts nodes in TabNode") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("t0"), TestDest)
        val t0 = StackNode(NodeKey("t0"), NodeKey("tabs"), listOf(s1))
        val t1 = StackNode(NodeKey("t1"), NodeKey("tabs"), emptyList())
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(t0, t1), activeStackIndex = 0
        )

        val count = tabs.fold(0, Int::plus) { 1 }
        count shouldBe 4 // tabs + t0 + s1 + t1
    }

    test("fold counts nodes in PaneNode") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("panes"), TestDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("panes"), TestDest2)
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1),
                PaneRole.Supporting to PaneConfiguration(s2)
            ),
            activePaneRole = PaneRole.Primary
        )

        val count = panes.fold(0, Int::plus) { 1 }
        count shouldBe 3 // panes + 2 screens
    }

    test("fold collects all keys") {
        val s = ScreenNode(NodeKey("s"), NodeKey("stack"), TestDest)
        val stack = StackNode(NodeKey("stack"), null, listOf(s))

        val keys = stack.fold(emptyList<NodeKey>(), List<NodeKey>::plus) { listOf(it.key) }
        keys shouldContainExactly listOf(NodeKey("stack"), NodeKey("s"))
    }

    // =========================================================================
    // forEachNode
    // =========================================================================

    test("forEachNode visits ScreenNode") {
        val screen = ScreenNode(NodeKey("s"), null, TestDest)
        val visited = mutableListOf<NodeKey>()
        screen.forEachNode { visited.add(it.key) }
        visited shouldContainExactly listOf(NodeKey("s"))
    }

    test("forEachNode visits all nodes in StackNode") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("stack"), TestDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("stack"), TestDest2)
        val stack = StackNode(NodeKey("stack"), null, listOf(s1, s2))

        val visited = mutableListOf<NodeKey>()
        stack.forEachNode { visited.add(it.key) }
        visited shouldContainExactly listOf(NodeKey("stack"), NodeKey("s1"), NodeKey("s2"))
    }

    test("forEachNode visits all nodes in TabNode") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("t0"), TestDest)
        val t0 = StackNode(NodeKey("t0"), NodeKey("tabs"), listOf(s1))
        val t1 = StackNode(NodeKey("t1"), NodeKey("tabs"), emptyList())
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(t0, t1), activeStackIndex = 0
        )

        val visited = mutableListOf<NodeKey>()
        tabs.forEachNode { visited.add(it.key) }
        visited shouldContainExactly listOf(
            NodeKey("tabs"), NodeKey("t0"), NodeKey("s1"), NodeKey("t1")
        )
    }

    test("forEachNode visits all nodes in PaneNode") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("panes"), TestDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("panes"), TestDest2)
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(s1),
                PaneRole.Supporting to PaneConfiguration(s2)
            ),
            activePaneRole = PaneRole.Primary
        )

        val visited = mutableListOf<NodeKey>()
        panes.forEachNode { visited.add(it.key) }
        visited shouldHaveSize 3
        visited[0] shouldBe NodeKey("panes")
    }

    // =========================================================================
    // canHandleBackInternally
    // =========================================================================

    test("canHandleBackInternally false for ScreenNode") {
        val screen = ScreenNode(NodeKey("s"), null, TestDest)
        screen.canHandleBackInternally().shouldBeFalse()
    }

    test("canHandleBackInternally false for StackNode with single child") {
        val stack = StackNode(
            NodeKey("stack"), null,
            listOf(ScreenNode(NodeKey("s"), NodeKey("stack"), TestDest))
        )
        stack.canHandleBackInternally().shouldBeFalse()
    }

    test("canHandleBackInternally true for StackNode with multiple children") {
        val stack = StackNode(
            NodeKey("stack"), null,
            listOf(
                ScreenNode(NodeKey("s1"), NodeKey("stack"), TestDest),
                ScreenNode(NodeKey("s2"), NodeKey("stack"), TestDest2)
            )
        )
        stack.canHandleBackInternally().shouldBeTrue()
    }

    test("canHandleBackInternally false for empty StackNode") {
        val stack = StackNode(NodeKey("stack"), null, emptyList())
        stack.canHandleBackInternally().shouldBeFalse()
    }

    test("canHandleBackInternally true for TabNode on non-initial tab") {
        val t0 = StackNode(NodeKey("t0"), NodeKey("tabs"), emptyList())
        val t1 = StackNode(NodeKey("t1"), NodeKey("tabs"), emptyList())
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(t0, t1), activeStackIndex = 1
        )
        tabs.canHandleBackInternally().shouldBeTrue()
    }

    test("canHandleBackInternally false for TabNode on initial tab with no back in stack") {
        val t0 = StackNode(
            NodeKey("t0"), NodeKey("tabs"),
            listOf(ScreenNode(NodeKey("s"), NodeKey("t0"), TestDest))
        )
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(t0), activeStackIndex = 0
        )
        tabs.canHandleBackInternally().shouldBeFalse()
    }

    test("canHandleBackInternally true for TabNode on initial tab with stack that can go back") {
        val t0 = StackNode(
            NodeKey("t0"), NodeKey("tabs"),
            listOf(
                ScreenNode(NodeKey("s1"), NodeKey("t0"), TestDest),
                ScreenNode(NodeKey("s2"), NodeKey("t0"), TestDest2)
            )
        )
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(t0), activeStackIndex = 0
        )
        tabs.canHandleBackInternally().shouldBeTrue()
    }

    test("canHandleBackInternally true for PaneNode with poppable pane stack") {
        val pstack = StackNode(
            NodeKey("pstack"), NodeKey("panes"),
            listOf(
                ScreenNode(NodeKey("s1"), NodeKey("pstack"), TestDest),
                ScreenNode(NodeKey("s2"), NodeKey("pstack"), TestDest2)
            )
        )
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        panes.canHandleBackInternally().shouldBeTrue()
    }

    test("canHandleBackInternally false for PaneNode with single-item pane stacks") {
        val pstack = StackNode(
            NodeKey("pstack"), NodeKey("panes"),
            listOf(ScreenNode(NodeKey("s"), NodeKey("pstack"), TestDest))
        )
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )
        panes.canHandleBackInternally().shouldBeFalse()
    }

    // =========================================================================
    // Additional branch coverage for paneForRole through TabNode
    // =========================================================================

    test("paneForRole searches recursively through TabNode") {
        val primaryContent = ScreenNode(NodeKey("primary"), NodeKey("panes"), TestDest)
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("tab0"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent)
            ),
            activePaneRole = PaneRole.Primary
        )
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(panes))
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(tab0), activeStackIndex = 0
        )

        tabs.paneForRole(PaneRole.Primary) shouldBe primaryContent
    }

    test("paneForRole returns null for non-existent role") {
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("s"), NodeKey("panes"), TestDest)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        panes.paneForRole(PaneRole.Extra).shouldBeNull()
    }

    test("paneForRole searches nested PaneNode inside PaneNode") {
        val innerContent = ScreenNode(NodeKey("inner-s"), NodeKey("inner-panes"), TestDest)
        val innerPane = PaneNode(
            key = NodeKey("inner-panes"),
            parentKey = NodeKey("panes"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("inner-primary"), NodeKey("inner-panes"), TestDest2)
                ),
                PaneRole.Supporting to PaneConfiguration(innerContent)
            ),
            activePaneRole = PaneRole.Primary
        )
        val outerPane = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(innerPane)
            ),
            activePaneRole = PaneRole.Primary
        )

        outerPane.paneForRole(PaneRole.Supporting) shouldBe innerContent
    }

    // =========================================================================
    // allPaneNodes through TabNode
    // =========================================================================

    test("allPaneNodes finds PaneNode inside TabNode") {
        val panes = PaneNode(
            key = NodeKey("panes"),
            parentKey = NodeKey("tab0"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode(NodeKey("s"), NodeKey("panes"), TestDest)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val tab0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(panes))
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(tab0), activeStackIndex = 0
        )

        val all = tabs.allPaneNodes()
        all shouldHaveSize 1
        all[0] shouldBe panes
    }

    // =========================================================================
    // allTabNodes through PaneNode
    // =========================================================================

    test("allTabNodes finds TabNode inside PaneNode") {
        val innerTabs = TabNode(
            key = NodeKey("inner-tabs"),
            parentKey = NodeKey("panes"),
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

        val all = panes.allTabNodes()
        all shouldHaveSize 1
        all[0] shouldBe innerTabs
    }

    // =========================================================================
    // allStackNodes through PaneNode
    // =========================================================================

    test("allStackNodes finds StackNodes inside PaneNode") {
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), emptyList())
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )

        val all = panes.allStackNodes()
        all shouldHaveSize 1
        all[0] shouldBe pstack
    }

    // =========================================================================
    // depth through PaneNode
    // =========================================================================

    test("depth calculates correctly through PaneNode") {
        val screen = ScreenNode(NodeKey("s"), NodeKey("pstack"), TestDest)
        val pstack = StackNode(NodeKey("pstack"), NodeKey("panes"), listOf(screen))
        val panes = PaneNode(
            key = NodeKey("panes"), parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(pstack)
            ),
            activePaneRole = PaneRole.Primary
        )

        panes.depth() shouldBe 2 // panes -> stack -> screen
    }

    test("depth for TabNode with empty stacks") {
        val tabs = TabNode(
            key = NodeKey("tabs"), parentKey = null,
            stacks = listOf(StackNode(NodeKey("t0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0
        )

        tabs.depth() shouldBe 1 // tabs -> empty stack (depth 0) + 1
    }
})
