@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.tree.result.PopResult
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

// =============================================================================
// Test destinations
// =============================================================================

private object PaneListDest : NavDestination {
    override fun toString(): String = "PaneListDest"
}

private object PaneDetailDest : NavDestination {
    override fun toString(): String = "PaneDetailDest"
}

private object PaneNewDest : NavDestination {
    override fun toString(): String = "PaneNewDest"
}

private object PaneExtraDest : NavDestination {
    override fun toString(): String = "PaneExtraDest"
}

class PaneOperationsTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    // =========================================================================
    // Helpers
    // =========================================================================

    var keyCounter = 0
    beforeTest { keyCounter = 0 }
    fun testKeyGen(): NodeKey = NodeKey("gen-${keyCounter++}")

    /**
     * Creates a root StackNode containing a PaneNode with Primary and Supporting panes.
     * Each pane holds a StackNode with configurable children.
     */
    fun createDualPaneTree(
        backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
        activePaneRole: PaneRole = PaneRole.Primary,
        primaryChildren: List<ScreenNode>? = null,
        supportingChildren: List<ScreenNode>? = null,
    ): StackNode {
        val defaultPrimary = listOf(
            ScreenNode(NodeKey("ps"), NodeKey("primary-stack"), PaneListDest)
        )
        val defaultSupporting = listOf(
            ScreenNode(NodeKey("ss"), NodeKey("supporting-stack"), PaneDetailDest)
        )

        val primaryStack = StackNode(
            key = NodeKey("primary-stack"),
            parentKey = NodeKey("pane"),
            children = primaryChildren ?: defaultPrimary
        )
        val supportingStack = StackNode(
            key = NodeKey("supporting-stack"),
            parentKey = NodeKey("pane"),
            children = supportingChildren ?: defaultSupporting
        )

        val paneNode = PaneNode(
            key = NodeKey("pane"),
            parentKey = NodeKey("root"),
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(content = primaryStack),
                PaneRole.Supporting to PaneConfiguration(content = supportingStack)
            ),
            activePaneRole = activePaneRole,
            backBehavior = backBehavior
        )

        return StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(paneNode)
        )
    }

    // =========================================================================
    // navigateToPane
    // =========================================================================

    test("navigateToPane pushes destination to primary pane stack") {
        val root = createDualPaneTree()
        val result = PaneOperations.navigateToPane(
            root = root,
            nodeKey = NodeKey("pane"),
            role = PaneRole.Primary,
            destination = PaneNewDest,
            generateKey = ::testKeyGen
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        val primaryStack = pane.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 2
        (primaryStack.children.last() as ScreenNode).destination shouldBe PaneNewDest
    }

    test("navigateToPane pushes destination to supporting pane stack") {
        val root = createDualPaneTree()
        val result = PaneOperations.navigateToPane(
            root = root,
            nodeKey = NodeKey("pane"),
            role = PaneRole.Supporting,
            destination = PaneNewDest,
            generateKey = ::testKeyGen
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        val supportingStack = pane.paneContent(PaneRole.Supporting) as StackNode
        supportingStack.children.size shouldBe 2
        (supportingStack.children.last() as ScreenNode).destination shouldBe PaneNewDest
    }

    test("navigateToPane switches focus to target pane by default") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Primary)
        val result = PaneOperations.navigateToPane(
            root = root,
            nodeKey = NodeKey("pane"),
            role = PaneRole.Supporting,
            destination = PaneNewDest,
            generateKey = ::testKeyGen
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Supporting
    }

    test("navigateToPane does not switch focus when switchFocus is false") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Primary)
        val result = PaneOperations.navigateToPane(
            root = root,
            nodeKey = NodeKey("pane"),
            role = PaneRole.Supporting,
            destination = PaneNewDest,
            switchFocus = false,
            generateKey = ::testKeyGen
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("navigateToPane keeps focus when navigating to already-active pane") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Primary)
        val result = PaneOperations.navigateToPane(
            root = root,
            nodeKey = NodeKey("pane"),
            role = PaneRole.Primary,
            destination = PaneNewDest,
            generateKey = ::testKeyGen
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("navigateToPane throws for non-existent pane key") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.navigateToPane(
                root = root,
                nodeKey = NodeKey("nonexistent"),
                role = PaneRole.Primary,
                destination = PaneNewDest
            )
        }
    }

    test("navigateToPane throws for unconfigured role") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.navigateToPane(
                root = root,
                nodeKey = NodeKey("pane"),
                role = PaneRole.Extra,
                destination = PaneNewDest
            )
        }
    }

    // =========================================================================
    // switchActivePane
    // =========================================================================

    test("switchActivePane changes active pane from Primary to Supporting") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Primary)
        val result = PaneOperations.switchActivePane(root, NodeKey("pane"), PaneRole.Supporting)

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Supporting
    }

    test("switchActivePane changes active pane from Supporting to Primary") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Supporting)
        val result = PaneOperations.switchActivePane(root, NodeKey("pane"), PaneRole.Primary)

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("switchActivePane returns same root when already on target pane") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Primary)
        val result = PaneOperations.switchActivePane(root, NodeKey("pane"), PaneRole.Primary)
        result shouldBe root
    }

    test("switchActivePane throws for non-existent pane key") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.switchActivePane(root, NodeKey("nonexistent"), PaneRole.Primary)
        }
    }

    test("switchActivePane throws for unconfigured role") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.switchActivePane(root, NodeKey("pane"), PaneRole.Extra)
        }
    }

    // =========================================================================
    // popPane
    // =========================================================================

    test("popPane removes top screen from primary pane") {
        val ps = ScreenNode(NodeKey("ps"), NodeKey("primary-stack"), PaneListDest)
        val ps2 = ScreenNode(NodeKey("ps2"), NodeKey("primary-stack"), PaneNewDest)
        val root = createDualPaneTree(primaryChildren = listOf(ps, ps2))

        val result = PaneOperations.popPane(root, NodeKey("pane"), PaneRole.Primary)!!
        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        val primaryStack = pane.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
        (primaryStack.children.first() as ScreenNode).destination shouldBe PaneListDest
    }

    test("popPane removes top screen from supporting pane") {
        val ss = ScreenNode(NodeKey("ss"), NodeKey("supporting-stack"), PaneDetailDest)
        val ss2 = ScreenNode(NodeKey("ss2"), NodeKey("supporting-stack"), PaneNewDest)
        val root = createDualPaneTree(supportingChildren = listOf(ss, ss2))

        val result = PaneOperations.popPane(root, NodeKey("pane"), PaneRole.Supporting)!!
        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        val suppStack = pane.paneContent(PaneRole.Supporting) as StackNode
        suppStack.children.size shouldBe 1
        (suppStack.children.first() as ScreenNode).destination shouldBe PaneDetailDest
    }

    test("popPane returns null when pane stack has single screen") {
        val root = createDualPaneTree()
        val result = PaneOperations.popPane(root, NodeKey("pane"), PaneRole.Primary)
        result.shouldBeNull()
    }

    test("popPane returns null for empty pane stack") {
        val root = createDualPaneTree(primaryChildren = emptyList())
        val result = PaneOperations.popPane(root, NodeKey("pane"), PaneRole.Primary)
        result.shouldBeNull()
    }

    test("popPane throws for non-existent pane key") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.popPane(root, NodeKey("nonexistent"), PaneRole.Primary)
        }
    }

    test("popPane throws for unconfigured role") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.popPane(root, NodeKey("pane"), PaneRole.Extra)
        }
    }

    // =========================================================================
    // popWithPaneBehavior — no PaneNode in tree
    // =========================================================================

    test("popWithPaneBehavior without PaneNode does regular pop") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("stack"), PaneListDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("stack"), PaneNewDest)
        val root = StackNode(NodeKey("stack"), null, listOf(s1, s2))

        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
    }

    test("popWithPaneBehavior without PaneNode returns CannotPop for empty stack") {
        val root = StackNode(NodeKey("stack"), null, emptyList())
        val result = PaneOperations.popWithPaneBehavior(root)
        result shouldBe PopResult.CannotPop
    }

    // =========================================================================
    // popWithPaneBehavior — active stack has multiple screens (common case)
    // =========================================================================

    test("popWithPaneBehavior pops normally when active stack has multiple screens") {
        val ps = ScreenNode(NodeKey("ps"), NodeKey("primary-stack"), PaneListDest)
        val ps2 = ScreenNode(NodeKey("ps2"), NodeKey("primary-stack"), PaneNewDest)
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopLatest,
            primaryChildren = listOf(ps, ps2)
        )

        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        val primaryStack = pane.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
    }

    // =========================================================================
    // popWithPaneBehavior — PopLatest
    // =========================================================================

    test("popWithPaneBehavior PopLatest pops single-screen stack to empty") {
        val root = createDualPaneTree(backBehavior = PaneBackBehavior.PopLatest)
        // Primary has 1 child → pop empties it (PRESERVE_EMPTY) → Popped
        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
    }

    test("popWithPaneBehavior PopLatest returns CannotPop for empty active stack") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopLatest,
            primaryChildren = emptyList()
        )
        val result = PaneOperations.popWithPaneBehavior(root)
        result shouldBe PopResult.CannotPop
    }

    // =========================================================================
    // popWithPaneBehavior — PopUntilScaffoldValueChange
    // =========================================================================

    test("popWithPaneBehavior PopUntilScaffoldValueChange switches to Primary from Supporting") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
            activePaneRole = PaneRole.Supporting
        )

        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("popWithPaneBehavior PopUntilScaffoldValueChange returns RequiresScaffoldChange on Primary") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
            activePaneRole = PaneRole.Primary
        )

        val result = PaneOperations.popWithPaneBehavior(root)
        result shouldBe PopResult.RequiresScaffoldChange
    }

    // =========================================================================
    // popWithPaneBehavior — PopUntilCurrentDestinationChange
    // =========================================================================

    test("popWithPaneBehavior PopUntilCurrentDestinationChange switches to alternative pane with content") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilCurrentDestinationChange,
            activePaneRole = PaneRole.Primary
        )
        // Supporting has 1 child (non-empty) → qualifies as alternative

        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Supporting
    }

    test("popWithPaneBehavior PopUntilCurrentDestinationChange returns PaneEmpty when no alternatives have content") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilCurrentDestinationChange,
            activePaneRole = PaneRole.Primary,
            supportingChildren = emptyList()
        )
        // Supporting stack is empty → no alternatives with content

        val result = PaneOperations.popWithPaneBehavior(root)
        result shouldBe PopResult.PaneEmpty(PaneRole.Primary)
    }

    // =========================================================================
    // popWithPaneBehavior — PopUntilContentChange
    // =========================================================================

    test("popWithPaneBehavior PopUntilContentChange pops from non-active pane with content and clears when at root") {
        // Primary has 1 child (active), Supporting has 2 children
        val ss = ScreenNode(NodeKey("ss"), NodeKey("supporting-stack"), PaneDetailDest)
        val ss2 = ScreenNode(NodeKey("ss2"), NodeKey("supporting-stack"), PaneNewDest)
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilContentChange,
            activePaneRole = PaneRole.Primary,
            supportingChildren = listOf(ss, ss2)
        )

        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        // Supporting was popped (2→1), then since 1 ≤ 1 and not Primary, cleared and switched
        val suppStack = pane.paneContent(PaneRole.Supporting) as StackNode
        suppStack.children.size shouldBe 0
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("popWithPaneBehavior PopUntilContentChange pops from non-active pane without clearing when still has content") {
        // Primary has 1 child (active), Supporting has 3 children
        val ss = ScreenNode(NodeKey("ss"), NodeKey("supporting-stack"), PaneDetailDest)
        val ss2 = ScreenNode(NodeKey("ss2"), NodeKey("supporting-stack"), PaneNewDest)
        val ss3 = ScreenNode(NodeKey("ss3"), NodeKey("supporting-stack"), PaneExtraDest)
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilContentChange,
            activePaneRole = PaneRole.Primary,
            supportingChildren = listOf(ss, ss2, ss3)
        )

        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        // Supporting was popped (3→2), still has >1 children so no clearing
        val suppStack = pane.paneContent(PaneRole.Supporting) as StackNode
        suppStack.children.size shouldBe 2
    }

    test("popWithPaneBehavior PopUntilContentChange returns PaneEmpty on Primary when no panes have poppable content") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilContentChange,
            activePaneRole = PaneRole.Primary
        )
        // Both panes have 1 child → neither has size > 1 → PaneEmpty

        val result = PaneOperations.popWithPaneBehavior(root)
        result shouldBe PopResult.PaneEmpty(PaneRole.Primary)
    }

    test("popWithPaneBehavior PopUntilContentChange clears and switches to Primary when on non-Primary with no poppable content") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilContentChange,
            activePaneRole = PaneRole.Supporting
        )
        // Both panes have 1 child → no poppable content, active is Supporting → clear and switch

        val result = PaneOperations.popWithPaneBehavior(root)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    // =========================================================================
    // popPaneAdaptive — compact mode (popFromActivePane)
    // =========================================================================

    test("popPaneAdaptive compact mode pops from active stack with multiple screens") {
        val ps = ScreenNode(NodeKey("ps"), NodeKey("primary-stack"), PaneListDest)
        val ps2 = ScreenNode(NodeKey("ps2"), NodeKey("primary-stack"), PaneNewDest)
        val root = createDualPaneTree(primaryChildren = listOf(ps, ps2))

        val result = PaneOperations.popPaneAdaptive(root, isCompact = true)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        val primaryStack = pane.paneContent(PaneRole.Primary) as StackNode
        primaryStack.children.size shouldBe 1
    }

    test("popPaneAdaptive compact mode returns PaneEmpty on Primary at root") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Primary)
        // Primary has 1 child → ≤1 and is Primary → PaneEmpty

        val result = PaneOperations.popPaneAdaptive(root, isCompact = true)
        result shouldBe PopResult.PaneEmpty(PaneRole.Primary)
    }

    test("popPaneAdaptive compact mode clears and switches to Primary from non-Primary at root") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Supporting)
        // Supporting has 1 child → ≤1 and not Primary → clear and switch to Primary

        val result = PaneOperations.popPaneAdaptive(root, isCompact = true)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("popPaneAdaptive compact mode pops from active Supporting stack with multiple screens") {
        val ss = ScreenNode(NodeKey("ss"), NodeKey("supporting-stack"), PaneDetailDest)
        val ss2 = ScreenNode(NodeKey("ss2"), NodeKey("supporting-stack"), PaneNewDest)
        val root = createDualPaneTree(
            activePaneRole = PaneRole.Supporting,
            supportingChildren = listOf(ss, ss2)
        )

        val result = PaneOperations.popPaneAdaptive(root, isCompact = true)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        val suppStack = pane.paneContent(PaneRole.Supporting) as StackNode
        suppStack.children.size shouldBe 1
    }

    // =========================================================================
    // popPaneAdaptive — expanded mode (delegates to popWithPaneBehavior)
    // =========================================================================

    test("popPaneAdaptive expanded mode uses configured PaneBackBehavior") {
        val root = createDualPaneTree(
            backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
            activePaneRole = PaneRole.Supporting
        )

        val result = PaneOperations.popPaneAdaptive(root, isCompact = false)
        result.shouldBeInstanceOf<PopResult.Popped>()
        val pane = (result as PopResult.Popped).newState.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    // =========================================================================
    // popPaneAdaptive — no PaneNode
    // =========================================================================

    test("popPaneAdaptive without PaneNode does standard pop") {
        val s1 = ScreenNode(NodeKey("s1"), NodeKey("st"), PaneListDest)
        val s2 = ScreenNode(NodeKey("s2"), NodeKey("st"), PaneNewDest)
        val root = StackNode(NodeKey("st"), null, listOf(s1, s2))

        val result = PaneOperations.popPaneAdaptive(root, isCompact = true)
        result.shouldBeInstanceOf<PopResult.Popped>()
    }

    test("popPaneAdaptive without PaneNode returns CannotPop for empty stack") {
        val root = StackNode(NodeKey("st"), null, emptyList())
        val result = PaneOperations.popPaneAdaptive(root, isCompact = false)
        result shouldBe PopResult.CannotPop
    }

    // =========================================================================
    // setPaneConfiguration
    // =========================================================================

    test("setPaneConfiguration adds new Extra role") {
        val root = createDualPaneTree()
        val extraStack = StackNode(
            NodeKey("extra-stack"),
            NodeKey("pane"),
            listOf(ScreenNode(NodeKey("es"), NodeKey("extra-stack"), PaneExtraDest))
        )
        val extraConfig = PaneConfiguration(content = extraStack)

        val result = PaneOperations.setPaneConfiguration(
            root, NodeKey("pane"), PaneRole.Extra, extraConfig
        )
        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.paneCount shouldBe 3
        pane.configuredRoles shouldBe setOf(PaneRole.Primary, PaneRole.Supporting, PaneRole.Extra)
        pane.paneContent(PaneRole.Extra).shouldBeInstanceOf<StackNode>()
    }

    test("setPaneConfiguration updates existing role configuration") {
        val root = createDualPaneTree()
        val newSuppStack = StackNode(
            NodeKey("new-supp"),
            NodeKey("pane"),
            listOf(ScreenNode(NodeKey("ns"), NodeKey("new-supp"), PaneExtraDest))
        )
        val newConfig = PaneConfiguration(
            content = newSuppStack,
            adaptStrategy = AdaptStrategy.Levitate
        )

        val result = PaneOperations.setPaneConfiguration(
            root, NodeKey("pane"), PaneRole.Supporting, newConfig
        )
        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.paneCount shouldBe 2
        pane.adaptStrategy(PaneRole.Supporting) shouldBe AdaptStrategy.Levitate
    }

    test("setPaneConfiguration throws for non-existent pane key") {
        val root = createDualPaneTree()
        val config = PaneConfiguration(content = StackNode(NodeKey("s"), null))
        shouldThrow<IllegalArgumentException> {
            PaneOperations.setPaneConfiguration(root, NodeKey("nonexistent"), PaneRole.Extra, config)
        }
    }

    // =========================================================================
    // removePaneConfiguration
    // =========================================================================

    test("removePaneConfiguration removes Supporting role") {
        val root = createDualPaneTree()
        val result = PaneOperations.removePaneConfiguration(
            root, NodeKey("pane"), PaneRole.Supporting
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.paneCount shouldBe 1
        pane.configuredRoles shouldBe setOf(PaneRole.Primary)
    }

    test("removePaneConfiguration throws when trying to remove Primary") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.removePaneConfiguration(root, NodeKey("pane"), PaneRole.Primary)
        }
    }

    test("removePaneConfiguration switches to Primary when removing active role") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Supporting)
        val result = PaneOperations.removePaneConfiguration(
            root, NodeKey("pane"), PaneRole.Supporting
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
        pane.paneCount shouldBe 1
    }

    test("removePaneConfiguration preserves active role when removing non-active role") {
        val root = createDualPaneTree(activePaneRole = PaneRole.Primary)
        val result = PaneOperations.removePaneConfiguration(
            root, NodeKey("pane"), PaneRole.Supporting
        )

        val pane = result.findByKey(NodeKey("pane")) as PaneNode
        pane.activePaneRole shouldBe PaneRole.Primary
    }

    test("removePaneConfiguration throws for non-existent pane key") {
        val root = createDualPaneTree()
        shouldThrow<IllegalArgumentException> {
            PaneOperations.removePaneConfiguration(
                root, NodeKey("nonexistent"), PaneRole.Supporting
            )
        }
    }
})
