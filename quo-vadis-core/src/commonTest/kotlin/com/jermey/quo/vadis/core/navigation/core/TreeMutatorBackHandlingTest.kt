package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.internal.tree.result.BackResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for TreeMutator back handling operations.
 *
 * Tests cover:
 * - `popWithTabBehavior`: intelligent back navigation with tab support
 * - `canHandleBackNavigation`: checking if system can handle back (vs delegate to OS)
 * - `BackResult`: all result variants
 *
 * These tests verify the new back handling architecture introduced in Phase 5 of the
 * back handling refactoring plan.
 */
private object HomeDestination : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "home"
}

private object ProfileDestination : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "profile"
}

private object SettingsDestination : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
    override fun toString(): String = "settings"
}

@OptIn(InternalQuoVadisApi::class)
class TreeMutatorBackHandlingTest : FunSpec({

    // =========================================================================
    // POP WITH TAB BEHAVIOR - STACK TESTS
    // =========================================================================

    test("popWithTabBehavior pops from stack when possible") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("screen2"), NodeKey("root"), ProfileDestination)
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        newState.children.size shouldBe 1
        (newState.activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("popWithTabBehavior returns DelegateToSystem for root with one item") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen1"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior returns DelegateToSystem for empty stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = emptyList()
        )

        val result = TreeMutator.popWithTabBehavior(root)

        // Empty root stack delegates to system (same as single-item stack)
        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior handles deep stack correctly") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        newState.children.size shouldBe 2
        (newState.activeChild as ScreenNode).destination shouldBe ProfileDestination
    }

    // =========================================================================
    // POP WITH TAB BEHAVIOR - TAB TESTS
    // =========================================================================

    test("popWithTabBehavior returns DelegateToSystem when on non-initial tab at root") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination))
                ),
                StackNode(
                    NodeKey("tab1"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), ProfileDestination))
                )
            ),
            activeStackIndex = 1 // Currently on tab 1
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        val result = TreeMutator.popWithTabBehavior(root)

        // TabNode is only child of root, so delegate to system (no tab switching)
        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior delegates to system when on initial tab at root") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination))
                ),
                StackNode(
                    NodeKey("tab1"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), ProfileDestination))
                )
            ),
            activeStackIndex = 0 // On initial tab
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        val result = TreeMutator.popWithTabBehavior(root)

        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("popWithTabBehavior pops from tab stack when stack has items") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s0a"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s0b"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(
                    NodeKey("tab1"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), SettingsDestination))
                )
            ),
            activeStackIndex = 0 // Tab 0 with 2 items
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        val result = TreeMutator.popWithTabBehavior(root)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        val newTabNode = newState.children.first() as TabNode
        newTabNode.stacks[0].children.size shouldBe 1
        (newTabNode.stacks[0].activeChild as ScreenNode).destination shouldBe HomeDestination
    }

    test("popWithTabBehavior delegates to system when TabNode is only child regardless of tab index") {
        // Start on non-initial tab with single item - TabNode is only child of root
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination))
                ),
                StackNode(
                    NodeKey("tab1"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), ProfileDestination))
                )
            ),
            activeStackIndex = 1 // On tab 1
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        // Back should delegate to system (TabNode is root's only child, cannot pop)
        val result = TreeMutator.popWithTabBehavior(root)
        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    // =========================================================================
    // POP WITH TAB BEHAVIOR - NESTED SCENARIOS
    // =========================================================================

    test("popWithTabBehavior handles stack above tabs") {
        // Stack > TabNode structure
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination))
                )
            ),
            activeStackIndex = 0
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                tabNode,
                ScreenNode(NodeKey("s1"), NodeKey("root"), ProfileDestination) // Screen above tabs
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        // Should pop from root stack (remove Profile screen)
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        newState.children.size shouldBe 1
        newState.children.first().shouldBeInstanceOf<TabNode>()
    }

    // =========================================================================
    // CAN HANDLE BACK NAVIGATION TESTS
    // =========================================================================

    test("canHandleBackNavigation returns true when stack can pop") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("screen2"), NodeKey("root"), ProfileDestination)
            )
        )

        TreeMutator.canHandleBackNavigation(root).shouldBeTrue()
    }

    test("canHandleBackNavigation returns false for root with one item") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("screen1"), NodeKey("root"), HomeDestination)
            )
        )

        TreeMutator.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns false for empty stack") {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = emptyList()
        )

        TreeMutator.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns false when TabNode is root's only child") {
        // TabNode on non-initial tab - but TabNode is root's only child so cannot be popped
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination))
                ),
                StackNode(
                    NodeKey("tab1"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), ProfileDestination))
                )
            ),
            activeStackIndex = 1 // Not on initial tab
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        // Should return false - TabNode cannot be popped (only child of root)
        TreeMutator.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns false when on initial tab at root") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s0"), NodeKey("tab0"), HomeDestination))
                ),
                StackNode(
                    NodeKey("tab1"),
                    NodeKey("tabs"),
                    listOf(ScreenNode(NodeKey("s1"), NodeKey("tab1"), ProfileDestination))
                )
            ),
            activeStackIndex = 0 // On initial tab
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        TreeMutator.canHandleBackNavigation(root).shouldBeFalse()
    }

    test("canHandleBackNavigation returns true when tab stack has items to pop") {
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s0a"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("s0b"), NodeKey("tab0"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0 // On initial tab but has 2 items
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        TreeMutator.canHandleBackNavigation(root).shouldBeTrue()
    }

    // =========================================================================
    // BACK RESULT TYPES TESTS
    // =========================================================================

    test("BackResult Handled contains new state") {
        val newState = StackNode(
            NodeKey("root"),
            null,
            listOf(ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination))
        )
        val result = BackResult.Handled(newState)

        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        handled.newState shouldBe newState
    }

    test("BackResult DelegateToSystem is singleton") {
        val result1 = BackResult.DelegateToSystem
        val result2 = BackResult.DelegateToSystem

        (result1 === result2).shouldBeTrue()
    }

    test("BackResult CannotHandle is singleton") {
        val result1 = BackResult.CannotHandle
        val result2 = BackResult.CannotHandle

        (result1 === result2).shouldBeTrue()
    }

    // =========================================================================
    // INTEGRATION SCENARIOS
    // =========================================================================

    test("full back navigation scenario - stack with tabs") {
        // Build: Stack[Tab[Stack[A,B], Stack[C]], D]
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("a"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("b"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("c"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                tabNode,
                ScreenNode(
                    NodeKey("d"),
                    NodeKey("root"),
                    HomeDestination
                ) // Screen pushed over tabs
            )
        )

        // Back 1: Pop D from root
        val result1 = TreeMutator.popWithTabBehavior(root)
        val handled1 = result1.shouldBeInstanceOf<BackResult.Handled>()
        val state1 = handled1.newState.shouldBeInstanceOf<StackNode>()
        state1.children.size shouldBe 1
        state1.children.first().shouldBeInstanceOf<TabNode>()

        // Back 2: Pop B from tab0
        val result2 = TreeMutator.popWithTabBehavior(state1)
        val handled2 = result2.shouldBeInstanceOf<BackResult.Handled>()
        val state2 = handled2.newState.shouldBeInstanceOf<StackNode>()
        val tabs2 = state2.children.first() as TabNode
        tabs2.stacks[0].children.size shouldBe 1

        // Back 3: Tab0 at root, delegate to system
        val result3 = TreeMutator.popWithTabBehavior(state2)
        result3.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("back navigation with tab switching") {
        // Start on tab1 with single item
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(
                    NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("a"), NodeKey("tab0"), HomeDestination),
                        ScreenNode(NodeKey("b"), NodeKey("tab0"), ProfileDestination)
                    )
                ),
                StackNode(
                    NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("c"), NodeKey("tab1"), SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 1 // On tab1
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        // Back 1: TabNode is only child of root, delegate to system (no tab switching)
        val result1 = TreeMutator.popWithTabBehavior(root)
        result1.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("canHandleBackNavigation tracks through full scenario") {
        var current: NavNode = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination),
                ScreenNode(NodeKey("s3"), NodeKey("root"), SettingsDestination)
            )
        )

        // Should be able to handle back until we get to single item
        var backCount = 0
        while (TreeMutator.canHandleBackNavigation(current)) {
            val result = TreeMutator.popWithTabBehavior(current)
            if (result is BackResult.Handled) {
                current = result.newState
                backCount++
            } else {
                break
            }
        }

        backCount shouldBe 2 // Popped twice (3 -> 2 -> 1)
        (current as StackNode).children.size shouldBe 1
        TreeMutator.canHandleBackNavigation(current).shouldBeFalse()
    }

    // =========================================================================
    // CASCADE BACK HANDLING TESTS
    // =========================================================================

    test("nested stack cascade - parent has 1 child - cascades to grandparent") {
        // Given: RootStack → ChildStack(1 item - GrandchildStack) → GrandchildStack(1 item)
        val grandchildScreen = ScreenNode(NodeKey("gc1"), NodeKey("grandchild"), HomeDestination)
        val grandchildStack = StackNode(
            key = NodeKey("grandchild"),
            parentKey = NodeKey("child"),
            children = listOf(grandchildScreen)
        )
        val childStack = StackNode(
            key = NodeKey("child"),
            parentKey = NodeKey("root"),
            children = listOf(grandchildStack)
        )
        val rootScreen = ScreenNode(NodeKey("r1"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, childStack)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should pop childStack from root, revealing rootScreen
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        newState.children.size shouldBe 1
        newState.activeChild?.key shouldBe NodeKey("r1")
    }

    test("nested stack cascade - parent is root with 1 child - delegates to system") {
        // Given: RootStack(1 child) → ChildStack(1 item)
        val childScreen = ScreenNode(NodeKey("c1"), NodeKey("child"), HomeDestination)
        val childStack = StackNode(
            key = NodeKey("child"),
            parentKey = NodeKey("root"),
            children = listOf(childScreen)
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(childStack)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should delegate to system since root has only 1 child
        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("tab cascade - initial tab with 1 item - pops entire TabNode") {
        // Given: RootStack → [Screen1, TabNode(initial tab with 1 item)]
        val tabScreen = ScreenNode(NodeKey("t1"), NodeKey("tab-stack-0"), HomeDestination)
        val tabStack = StackNode(
            key = NodeKey("tab-stack-0"),
            parentKey = NodeKey("tabs"),
            children = listOf(tabScreen)
        )
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val rootScreen = ScreenNode(NodeKey("r1"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, tabNode)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should pop TabNode from root, revealing rootScreen
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        newState.children.size shouldBe 1
        newState.activeChild?.key shouldBe NodeKey("r1")
    }

    test("tab cascade - initial tab with 1 item at root - delegates to system") {
        // Given: RootStack(only TabNode) → TabNode(initial tab with 1 item)
        val tabScreen = ScreenNode(NodeKey("t1"), NodeKey("tab-stack-0"), HomeDestination)
        val tabStack = StackNode(
            key = NodeKey("tab-stack-0"),
            parentKey = NodeKey("tabs"),
            children = listOf(tabScreen)
        )
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should delegate to system
        result.shouldBeInstanceOf<BackResult.DelegateToSystem>()
    }

    test("deep cascade - stack in tab in stack all with 1 item - cascades to grandparent") {
        // Given: RootStack → [Screen1, MiddleStack(only TabNode) → TabNode(initial tab with 1 item)]
        val deepScreen = ScreenNode(NodeKey("d1"), NodeKey("tab-stack-0"), HomeDestination)
        val tabStack = StackNode(
            key = NodeKey("tab-stack-0"),
            parentKey = NodeKey("tabs"),
            children = listOf(deepScreen)
        )
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("middle"),
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val middleStack = StackNode(
            key = NodeKey("middle"),
            parentKey = NodeKey("root"),
            children = listOf(tabNode)
        )
        val rootScreen = ScreenNode(NodeKey("r1"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, middleStack)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should cascade through TabNode and MiddleStack, popping MiddleStack from root
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        newState.children.size shouldBe 1
        newState.activeChild?.key shouldBe NodeKey("r1")
    }

    test("nested stack no cascade - parent has multiple children - normal pop") {
        // Given: RootStack → [Screen1, ChildStack(1 item), ChildStack(1 item)]
        val child1Screen = ScreenNode(NodeKey("c1"), NodeKey("child1"), HomeDestination)
        val child1Stack = StackNode(
            key = NodeKey("child1"),
            parentKey = NodeKey("root"),
            children = listOf(child1Screen)
        )
        val child2Screen = ScreenNode(NodeKey("c2"), NodeKey("child2"), ProfileDestination)
        val child2Stack = StackNode(
            key = NodeKey("child2"),
            parentKey = NodeKey("root"),
            children = listOf(child2Screen)
        )
        val rootScreen = ScreenNode(NodeKey("r1"), NodeKey("root"), SettingsDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, child1Stack, child2Stack)
        )

        // When: Back from child2 (active child)
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should pop child2Stack (parent has multiple children, no cascade needed)
        val handled = result.shouldBeInstanceOf<BackResult.Handled>()
        val newState = handled.newState.shouldBeInstanceOf<StackNode>()
        newState.children.size shouldBe 2
        newState.activeChild?.key shouldBe NodeKey("child1")
    }

    test("canHandleBackNavigation returns true when cascade would handle back") {
        // Given: RootStack → [Screen1, ChildStack(1 item)]
        val childScreen = ScreenNode(NodeKey("c1"), NodeKey("child"), HomeDestination)
        val childStack = StackNode(
            key = NodeKey("child"),
            parentKey = NodeKey("root"),
            children = listOf(childScreen)
        )
        val rootScreen = ScreenNode(NodeKey("r1"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, childStack)
        )

        // When/Then: Should return true because cascade can pop childStack
        TreeMutator.canHandleBackNavigation(root).shouldBeTrue()
    }

    test("canHandleBackNavigation returns true when TabNode can be cascade-popped") {
        // Given: RootStack → [Screen1, TabNode(initial tab with 1 item)]
        val tabScreen = ScreenNode(NodeKey("t1"), NodeKey("tab-stack-0"), HomeDestination)
        val tabStack = StackNode(
            key = NodeKey("tab-stack-0"),
            parentKey = NodeKey("tabs"),
            children = listOf(tabScreen)
        )
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val rootScreen = ScreenNode(NodeKey("r1"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, tabNode)
        )

        // When/Then: Should return true because TabNode can be popped from root
        TreeMutator.canHandleBackNavigation(root).shouldBeTrue()
    }

})
