package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.internal.tree.config.PopBehavior
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs

/**
 * Unit tests for TreeMutator pop operations.
 *
 * Tests cover:
 * - `pop`: removes last screen from active stack
 * - `popTo`: removes screens until predicate matches
 * - `popToRoute`: removes screens until route matches
 * - `popToDestination`: removes screens until destination type matches
 * - `PopBehavior`: CASCADE vs PRESERVE_EMPTY behavior
 */
@OptIn(InternalQuoVadisApi::class)
class TreeMutatorPopTest : FunSpec() {

    object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    object ProfileDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "profile"
    }

    object SettingsDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "settings"
    }

    object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    init {

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    beforeTest {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // POP TESTS
    // =========================================================================

    test("pop removes last screen from stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val result = TreeMutator.pop(root)

        result.shouldNotBeNull()
        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("pop with single item at root returns empty stack with PRESERVE_EMPTY") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.pop(root, PopBehavior.PRESERVE_EMPTY)

        // PRESERVE_EMPTY returns a tree with empty stack
        result.shouldNotBeNull()
        (result as StackNode).isEmpty.shouldBeTrue()
    }

    test("pop returns null on empty stack") {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.pop(root)

        result.shouldBeNull()
    }

    test("pop with PRESERVE_EMPTY preserves empty stack structure") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("inner"), HomeDestination)
            )
        )
        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = null,
            children = listOf(innerStack)
        )

        val result = TreeMutator.pop(outerStack, PopBehavior.PRESERVE_EMPTY)

        // PRESERVE_EMPTY keeps the structure - inner stack becomes empty
        result.shouldNotBeNull()
        val resultOuter = result as StackNode
        val resultInner = resultOuter.children[0] as StackNode
        resultInner.isEmpty.shouldBeTrue()
    }

    test("pop removes screen and preserves non-empty stack") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("inner"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("inner"), ProfileDestination)
            )
        )
        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = null,
            children = listOf(innerStack)
        )

        val result = TreeMutator.pop(outerStack)

        result.shouldNotBeNull()
        val resultOuter = result as StackNode
        val resultInner = resultOuter.children[0] as StackNode
        resultInner.children.size shouldBe 1
        (resultInner.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("pop from tab affects active tab only") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs)

        result.shouldNotBeNull()
        val resultTabs = result as TabNode

        // Tab0 should have 1 item
        resultTabs.stacks[0].children.size shouldBe 1
        (resultTabs.stacks[0].activeChild as ScreenNode).destination shouldBe HomeDestination

        // Tab1 should be unchanged
        resultTabs.stacks[1].children.size shouldBe 1
    }

    test("pop preserves structural sharing for unchanged branches") {
        val tab1Screen = ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tab1Screen))

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                tab1Stack
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs) as TabNode

        // Tab1 stack should be same reference
        result.stacks[1] shouldBeSameInstanceAs tab1Stack
        result.stacks[1].children[0] shouldBeSameInstanceAs tab1Screen
    }

    test("pop in tabs with single item returns empty stack with PRESERVE_EMPTY") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs)

        // With default PRESERVE_EMPTY, tab's stack becomes empty but structure remains
        result.shouldNotBeNull()
        val resultTabs = result as TabNode
        resultTabs.stacks[0].isEmpty.shouldBeTrue()
    }

    test("pop returns null for ScreenNode root") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        val result = TreeMutator.pop(root)

        result.shouldBeNull()
    }

    // =========================================================================
    // POP TO TESTS
    // =========================================================================

    test("popTo removes screens until predicate matches") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.popTo(root, inclusive = false) { node ->
            node is ScreenNode && node.destination == ProfileDestination
        }

        (result as StackNode).children.size shouldBe 2
        (result.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("popTo with inclusive removes matching screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.popTo(root, inclusive = true) { node ->
            node is ScreenNode && node.destination == ProfileDestination
        }

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("popTo returns original when predicate not matched") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.popTo(root) { false }

        result shouldBeSameInstanceAs root
    }

    test("popTo returns original when no active stack") {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        val result = TreeMutator.popTo(root) { true }

        result shouldBeSameInstanceAs root
    }

    test("popTo preserves at least one item when inclusive would empty stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        // Try to pop inclusive to the first screen - should keep at least one
        val result = TreeMutator.popTo(root, inclusive = true) { node ->
            node is ScreenNode && node.destination == HomeDestination
        }

        // Since popping inclusive to the first would empty the stack, it returns original
        result shouldBeSameInstanceAs root
    }

    test("popTo works in tabs targeting active stack") {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination),
                        ScreenNode(NodeKey("s3"), NodeKey("tab0"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.popTo(tabs, inclusive = false) { node ->
            node is ScreenNode && node.destination == HomeDestination
        }

        val resultTabs = result as TabNode
        resultTabs.stacks[0].children.size shouldBe 1
        (resultTabs.stacks[0].activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    // =========================================================================
    // POP TO ROUTE TESTS
    // =========================================================================

    test("popToRoute finds screen by route") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.popToRoute(root, "home")

        (result as StackNode).children.size shouldBe 1
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("popToRoute with inclusive false keeps matching screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.popToRoute(root, "profile", inclusive = false)

        (result as StackNode).children.size shouldBe 2
        (result.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    test("popToRoute returns original when route not found") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.popToRoute(root, "nonexistent")

        result shouldBeSameInstanceAs root
    }

    test("popToRoute works with multiple screens of different routes") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), HomeDestination), // Same route as first
                ScreenNode(NodeKey("s4"), NodeKey("root"), SettingsDestination)
            )
        )

        // Should find the LAST occurrence of "home" (searching from back)
        val result = TreeMutator.popToRoute(root, "home", inclusive = false)

        (result as StackNode).children.size shouldBe 3
        // The third screen (index 2) is the last "home"
        (result.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    // =========================================================================
    // POP TO DESTINATION TESTS
    // =========================================================================

    test("popToDestination finds screen by destination type") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.popToDestination<HomeDestination>(root)

        (result as StackNode).children.size shouldBe 1
        (result.activeChild is ScreenNode).shouldBeTrue()
        ((result.activeChild as ScreenNode).destination is HomeDestination).shouldBeTrue()
    }

    test("popToDestination with inclusive removes matching screen") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.popToDestination<ProfileDestination>(root, inclusive = true)

        (result as StackNode).children.size shouldBe 1
        ((result.activeChild as ScreenNode).destination is HomeDestination).shouldBeTrue()
    }

    // =========================================================================
    // POP BEHAVIOR TESTS
    // =========================================================================

    test("pop with CASCADE behavior on tab preserves empty stack") {
        // In tabs, CASCADE cannot remove a stack - it would break the tab structure
        // So it preserves the empty stack structure
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs, PopBehavior.CASCADE)

        // CASCADE in tabs falls back to preserving empty stack
        result.shouldNotBeNull()
        val resultTabs = result as TabNode
        resultTabs.stacks[0].isEmpty.shouldBeTrue()
    }

    test("pop with CASCADE removes nested empty stack from parent stack") {
        val innerStack = StackNode(
            key = NodeKey("inner"),
            parentKey = NodeKey("outer"),
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("inner"), HomeDestination)
            )
        )
        val outerStack = StackNode(
            key = NodeKey("outer"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s0"), NodeKey("outer"), ProfileDestination),
                innerStack
            )
        )

        // Pop from inner stack - with CASCADE, should try to remove empty inner
        val result = TreeMutator.pop(outerStack, PopBehavior.CASCADE)

        result.shouldNotBeNull()
        val resultOuter = result as StackNode
        // Inner stack should be removed after becoming empty, leaving just s0
        resultOuter.children.size shouldBe 1
        (resultOuter.children[0] is ScreenNode).shouldBeTrue()
        (resultOuter.children[0] as ScreenNode).destination shouldBe ProfileDestination
    }

    test("pop multiple times until cannot go back") {
        var current: NavNode = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        var popCount = 0
        while (TreeMutator.canGoBack(current)) {
            val next = TreeMutator.pop(current)
            if (next == null) break
            current = next
            popCount++
        }

        // Should be able to pop 2 times (3 screens -> 2 -> 1 -> canGoBack false)
        popCount shouldBe 2
        (current as StackNode).children.size shouldBe 1
    }

    } // init
}
