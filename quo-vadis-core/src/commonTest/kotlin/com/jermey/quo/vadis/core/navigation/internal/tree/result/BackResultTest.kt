@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.result

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

private object BackTestDest : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

class BackResultTest : FunSpec({

    // =========================================================================
    // Handled
    // =========================================================================

    test("Handled holds newState correctly") {
        val tree = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("root"), BackTestDest))
        )
        val result = BackResult.Handled(tree)

        result.newState shouldBe tree
    }

    test("Handled is instance of BackResult") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val result: BackResult = BackResult.Handled(tree)

        result.shouldBeInstanceOf<BackResult.Handled>()
    }

    test("Handled data class equality") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val a = BackResult.Handled(tree)
        val b = BackResult.Handled(tree)

        a shouldBe b
    }

    test("Handled data class inequality with different states") {
        val tree1 = StackNode(NodeKey("root1"), null, emptyList())
        val tree2 = StackNode(NodeKey("root2"), null, emptyList())

        val a = BackResult.Handled(tree1)
        val b = BackResult.Handled(tree2)

        (a == b) shouldBe false
    }

    // =========================================================================
    // DelegateToSystem
    // =========================================================================

    test("DelegateToSystem is a singleton object") {
        val a = BackResult.DelegateToSystem
        val b = BackResult.DelegateToSystem

        a shouldBeSameInstanceAs b
    }

    test("DelegateToSystem is instance of BackResult") {
        val result: BackResult = BackResult.DelegateToSystem

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    // =========================================================================
    // CannotHandle
    // =========================================================================

    test("CannotHandle is a singleton object") {
        val a = BackResult.CannotHandle
        val b = BackResult.CannotHandle

        a shouldBeSameInstanceAs b
    }

    test("CannotHandle is instance of BackResult") {
        val result: BackResult = BackResult.CannotHandle

        result.shouldBeInstanceOf<BackResult.CannotHandle>()
    }

    // =========================================================================
    // Exhaustive when
    // =========================================================================

    test("exhaustive when covers all variants") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val results = listOf<BackResult>(
            BackResult.Handled(tree),
            BackResult.DelegateToSystem,
            BackResult.CannotHandle
        )

        val labels = results.map { result ->
            when (result) {
                is BackResult.Handled -> "handled"
                is BackResult.DelegateToSystem -> "delegate_to_system"
                is BackResult.CannotHandle -> "cannot_handle"
            }
        }

        labels shouldBe listOf("handled", "delegate_to_system", "cannot_handle")
    }
})
