@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.internal.tree.result

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private object TreeOpTestDest : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

class TreeOperationResultTest : FunSpec({

    // =========================================================================
    // Success
    // =========================================================================

    test("Success holds newTree correctly") {
        val tree = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("root"), TreeOpTestDest))
        )
        val result = TreeOperationResult.Success(tree)

        result.newTree shouldBe tree
    }

    test("Success is instance of TreeOperationResult") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val result: TreeOperationResult = TreeOperationResult.Success(tree)

        result.shouldBeInstanceOf<TreeOperationResult.Success>()
    }

    test("Success data class equality") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val a = TreeOperationResult.Success(tree)
        val b = TreeOperationResult.Success(tree)

        a shouldBe b
    }

    test("Success data class inequality with different trees") {
        val tree1 = StackNode(NodeKey("root1"), null, emptyList())
        val tree2 = StackNode(NodeKey("root2"), null, emptyList())

        val a = TreeOperationResult.Success(tree1)
        val b = TreeOperationResult.Success(tree2)

        (a == b) shouldBe false
    }

    // =========================================================================
    // NodeNotFound
    // =========================================================================

    test("NodeNotFound holds key correctly") {
        val key = NodeKey("missing")
        val result = TreeOperationResult.NodeNotFound(key)

        result.key shouldBe key
    }

    test("NodeNotFound is instance of TreeOperationResult") {
        val result: TreeOperationResult = TreeOperationResult.NodeNotFound(NodeKey("x"))

        result.shouldBeInstanceOf<TreeOperationResult.NodeNotFound>()
    }

    test("NodeNotFound data class equality") {
        val a = TreeOperationResult.NodeNotFound(NodeKey("k"))
        val b = TreeOperationResult.NodeNotFound(NodeKey("k"))

        a shouldBe b
    }

    // =========================================================================
    // getOrNull
    // =========================================================================

    test("getOrNull returns tree for Success") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val result: TreeOperationResult = TreeOperationResult.Success(tree)

        result.getOrNull().shouldNotBeNull()
        result.getOrNull() shouldBe tree
    }

    test("getOrNull returns null for NodeNotFound") {
        val result: TreeOperationResult = TreeOperationResult.NodeNotFound(NodeKey("x"))

        result.getOrNull().shouldBeNull()
    }

    // =========================================================================
    // getOrElse
    // =========================================================================

    test("getOrElse returns tree for Success") {
        val tree = StackNode(NodeKey("root"), null, emptyList())
        val fallback = StackNode(NodeKey("fallback"), null, emptyList())
        val result: TreeOperationResult = TreeOperationResult.Success(tree)

        result.getOrElse(fallback) shouldBe tree
    }

    test("getOrElse returns fallback for NodeNotFound") {
        val fallback = StackNode(NodeKey("fallback"), null, emptyList())
        val result: TreeOperationResult = TreeOperationResult.NodeNotFound(NodeKey("x"))

        result.getOrElse(fallback) shouldBe fallback
    }

    // =========================================================================
    // Exhaustive when
    // =========================================================================

    test("exhaustive when covers all variants") {
        val results = listOf<TreeOperationResult>(
            TreeOperationResult.Success(StackNode(NodeKey("r"), null, emptyList())),
            TreeOperationResult.NodeNotFound(NodeKey("x"))
        )

        val labels = results.map { result ->
            when (result) {
                is TreeOperationResult.Success -> "success"
                is TreeOperationResult.NodeNotFound -> "not_found"
            }
        }

        labels shouldBe listOf("success", "not_found")
    }
})
