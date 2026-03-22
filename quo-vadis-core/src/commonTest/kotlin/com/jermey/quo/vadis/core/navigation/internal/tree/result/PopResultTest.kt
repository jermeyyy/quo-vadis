@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.result

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

private object PopTestDest : NavDestination

class PopResultTest : FunSpec({

    // =========================================================================
    // Popped
    // =========================================================================

    test("Popped holds newState correctly") {
        val tree = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("root"), PopTestDest))
        )
        val result = PopResult.Popped(tree)

        result.newState shouldBe tree
    }

    test("Popped is instance of PopResult") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val result: PopResult = PopResult.Popped(tree)

        result.shouldBeInstanceOf<PopResult.Popped>()
    }

    test("Popped data class equality") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val a = PopResult.Popped(tree)
        val b = PopResult.Popped(tree)

        a shouldBe b
    }

    test("Popped data class inequality with different states") {
        val tree1 = StackNode(NodeKey("root1"), null, emptyList())
        val tree2 = StackNode(NodeKey("root2"), null, emptyList())

        val a = PopResult.Popped(tree1)
        val b = PopResult.Popped(tree2)

        (a == b) shouldBe false
    }

    // =========================================================================
    // PaneEmpty
    // =========================================================================

    test("PaneEmpty holds paneRole correctly") {
        val result = PopResult.PaneEmpty(PaneRole.Primary)

        result.paneRole shouldBe PaneRole.Primary
    }

    test("PaneEmpty with Supporting role") {
        val result = PopResult.PaneEmpty(PaneRole.Supporting)

        result.paneRole shouldBe PaneRole.Supporting
    }

    test("PaneEmpty is instance of PopResult") {
        val result: PopResult = PopResult.PaneEmpty(PaneRole.Primary)

        result.shouldBeInstanceOf<PopResult.PaneEmpty>()
    }

    test("PaneEmpty data class equality") {
        val a = PopResult.PaneEmpty(PaneRole.Primary)
        val b = PopResult.PaneEmpty(PaneRole.Primary)

        a shouldBe b
    }

    test("PaneEmpty inequality with different roles") {
        val a = PopResult.PaneEmpty(PaneRole.Primary)
        val b = PopResult.PaneEmpty(PaneRole.Supporting)

        (a == b) shouldBe false
    }

    // =========================================================================
    // CannotPop
    // =========================================================================

    test("CannotPop is a singleton object") {
        val a = PopResult.CannotPop
        val b = PopResult.CannotPop

        a shouldBeSameInstanceAs b
    }

    test("CannotPop is instance of PopResult") {
        val result: PopResult = PopResult.CannotPop

        result.shouldBeInstanceOf<PopResult.CannotPop>()
    }

    // =========================================================================
    // RequiresScaffoldChange
    // =========================================================================

    test("RequiresScaffoldChange is a singleton object") {
        val a = PopResult.RequiresScaffoldChange
        val b = PopResult.RequiresScaffoldChange

        a shouldBeSameInstanceAs b
    }

    test("RequiresScaffoldChange is instance of PopResult") {
        val result: PopResult = PopResult.RequiresScaffoldChange

        result.shouldBeInstanceOf<PopResult.RequiresScaffoldChange>()
    }

    // =========================================================================
    // Exhaustive when
    // =========================================================================

    test("exhaustive when covers all variants") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val results = listOf<PopResult>(
            PopResult.Popped(tree),
            PopResult.PaneEmpty(PaneRole.Primary),
            PopResult.CannotPop,
            PopResult.RequiresScaffoldChange
        )

        val labels = results.map { result ->
            when (result) {
                is PopResult.Popped -> "popped"
                is PopResult.PaneEmpty -> "pane_empty"
                is PopResult.CannotPop -> "cannot_pop"
                is PopResult.RequiresScaffoldChange -> "requires_scaffold_change"
            }
        }

        labels shouldBe listOf("popped", "pane_empty", "cannot_pop", "requires_scaffold_change")
    }
})
