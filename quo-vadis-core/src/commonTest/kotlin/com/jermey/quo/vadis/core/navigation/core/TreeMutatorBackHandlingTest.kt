package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
class TreeMutatorBackHandlingTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

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

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // POP WITH TAB BEHAVIOR - STACK TESTS
    // =========================================================================

    @Test
    fun `popWithTabBehavior pops from stack when possible`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("screen1", "root", HomeDestination),
                ScreenNode("screen2", "root", ProfileDestination)
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState
        assertIs<StackNode>(newState)
        assertEquals(1, newState.children.size)
        assertEquals(HomeDestination, (newState.activeChild as ScreenNode).destination)
    }

    @Test
    fun `popWithTabBehavior returns DelegateToSystem for root with one item`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("screen1", "root", HomeDestination)
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        assertIs<TreeMutator.BackResult.DelegateToSystem>(result)
    }

    @Test
    fun `popWithTabBehavior returns DelegateToSystem for empty stack`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = emptyList()
        )

        val result = TreeMutator.popWithTabBehavior(root)

        // Empty root stack delegates to system (same as single-item stack)
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result)
    }

    @Test
    fun `popWithTabBehavior handles deep stack correctly`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(2, newState.children.size)
        assertEquals(ProfileDestination, (newState.activeChild as ScreenNode).destination)
    }

    // =========================================================================
    // POP WITH TAB BEHAVIOR - TAB TESTS
    // =========================================================================

    @Test
    fun `popWithTabBehavior switches to initial tab when on non-initial tab at root`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(ScreenNode("s0", "tab0", HomeDestination))),
                StackNode("tab1", "tabs", listOf(ScreenNode("s1", "tab1", ProfileDestination)))
            ),
            activeStackIndex = 1 // Currently on tab 1
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        val result = TreeMutator.popWithTabBehavior(root)

        // TabNode is only child of root, so delegate to system (no tab switching)
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result)
    }

    @Test
    fun `popWithTabBehavior delegates to system when on initial tab at root`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(ScreenNode("s0", "tab0", HomeDestination))),
                StackNode("tab1", "tabs", listOf(ScreenNode("s1", "tab1", ProfileDestination)))
            ),
            activeStackIndex = 0 // On initial tab
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        val result = TreeMutator.popWithTabBehavior(root)

        assertIs<TreeMutator.BackResult.DelegateToSystem>(result)
    }

    @Test
    fun `popWithTabBehavior pops from tab stack when stack has items`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s0a", "tab0", HomeDestination),
                    ScreenNode("s0b", "tab0", ProfileDestination)
                )),
                StackNode("tab1", "tabs", listOf(ScreenNode("s1", "tab1", SettingsDestination)))
            ),
            activeStackIndex = 0 // Tab 0 with 2 items
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        val result = TreeMutator.popWithTabBehavior(root)

        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        val newTabNode = newState.children.first() as TabNode
        assertEquals(1, newTabNode.stacks[0].children.size)
        assertEquals(HomeDestination, (newTabNode.stacks[0].activeChild as ScreenNode).destination)
    }

    @Test
    fun `popWithTabBehavior delegates to system when TabNode is only child regardless of tab index`() {
        // Start on non-initial tab with single item - TabNode is only child of root
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(ScreenNode("s0", "tab0", HomeDestination))),
                StackNode("tab1", "tabs", listOf(ScreenNode("s1", "tab1", ProfileDestination)))
            ),
            activeStackIndex = 1 // On tab 1
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        // Back should delegate to system (TabNode is root's only child, cannot pop)
        val result = TreeMutator.popWithTabBehavior(root)
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result)
    }

    // =========================================================================
    // POP WITH TAB BEHAVIOR - NESTED SCENARIOS
    // =========================================================================

    @Test
    fun `popWithTabBehavior handles stack above tabs`() {
        // Stack > TabNode structure
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(ScreenNode("s0", "tab0", HomeDestination)))
            ),
            activeStackIndex = 0
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                tabNode,
                ScreenNode("s1", "root", ProfileDestination) // Screen above tabs
            )
        )

        val result = TreeMutator.popWithTabBehavior(root)

        // Should pop from root stack (remove Profile screen)
        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(1, newState.children.size)
        assertIs<TabNode>(newState.children.first())
    }

    // =========================================================================
    // CAN HANDLE BACK NAVIGATION TESTS
    // =========================================================================

    @Test
    fun `canHandleBackNavigation returns true when stack can pop`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("screen1", "root", HomeDestination),
                ScreenNode("screen2", "root", ProfileDestination)
            )
        )

        assertTrue(TreeMutator.canHandleBackNavigation(root))
    }

    @Test
    fun `canHandleBackNavigation returns false for root with one item`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("screen1", "root", HomeDestination)
            )
        )

        assertFalse(TreeMutator.canHandleBackNavigation(root))
    }

    @Test
    fun `canHandleBackNavigation returns false for empty stack`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = emptyList()
        )

        assertFalse(TreeMutator.canHandleBackNavigation(root))
    }

    @Test
    fun `canHandleBackNavigation returns false when TabNode is root's only child`() {
        // TabNode on non-initial tab - but TabNode is root's only child so cannot be popped
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(ScreenNode("s0", "tab0", HomeDestination))),
                StackNode("tab1", "tabs", listOf(ScreenNode("s1", "tab1", ProfileDestination)))
            ),
            activeStackIndex = 1 // Not on initial tab
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        // Should return false - TabNode cannot be popped (only child of root)
        assertFalse(TreeMutator.canHandleBackNavigation(root))
    }

    @Test
    fun `canHandleBackNavigation returns false when on initial tab at root`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(ScreenNode("s0", "tab0", HomeDestination))),
                StackNode("tab1", "tabs", listOf(ScreenNode("s1", "tab1", ProfileDestination)))
            ),
            activeStackIndex = 0 // On initial tab
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        assertFalse(TreeMutator.canHandleBackNavigation(root))
    }

    @Test
    fun `canHandleBackNavigation returns true when tab stack has items to pop`() {
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("s0a", "tab0", HomeDestination),
                    ScreenNode("s0b", "tab0", ProfileDestination)
                ))
            ),
            activeStackIndex = 0 // On initial tab but has 2 items
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        assertTrue(TreeMutator.canHandleBackNavigation(root))
    }

    // =========================================================================
    // BACK RESULT TYPES TESTS
    // =========================================================================

    @Test
    fun `BackResult Handled contains new state`() {
        val newState = StackNode("root", null, listOf(ScreenNode("s1", "root", HomeDestination)))
        val result = TreeMutator.BackResult.Handled(newState)

        assertIs<TreeMutator.BackResult.Handled>(result)
        assertEquals(newState, result.newState)
    }

    @Test
    fun `BackResult DelegateToSystem is singleton`() {
        val result1 = TreeMutator.BackResult.DelegateToSystem
        val result2 = TreeMutator.BackResult.DelegateToSystem

        assertTrue(result1 === result2)
    }

    @Test
    fun `BackResult CannotHandle is singleton`() {
        val result1 = TreeMutator.BackResult.CannotHandle
        val result2 = TreeMutator.BackResult.CannotHandle

        assertTrue(result1 === result2)
    }

    // =========================================================================
    // INTEGRATION SCENARIOS
    // =========================================================================

    @Test
    fun `full back navigation scenario - stack with tabs`() {
        // Build: Stack[Tab[Stack[A,B], Stack[C]], D]
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("a", "tab0", HomeDestination),
                    ScreenNode("b", "tab0", ProfileDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("c", "tab1", SettingsDestination)
                ))
            ),
            activeStackIndex = 0
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                tabNode,
                ScreenNode("d", "root", HomeDestination) // Screen pushed over tabs
            )
        )

        // Back 1: Pop D from root
        val result1 = TreeMutator.popWithTabBehavior(root)
        assertIs<TreeMutator.BackResult.Handled>(result1)
        val state1 = result1.newState as StackNode
        assertEquals(1, state1.children.size)
        assertIs<TabNode>(state1.children.first())

        // Back 2: Pop B from tab0
        val result2 = TreeMutator.popWithTabBehavior(state1)
        assertIs<TreeMutator.BackResult.Handled>(result2)
        val state2 = result2.newState as StackNode
        val tabs2 = state2.children.first() as TabNode
        assertEquals(1, tabs2.stacks[0].children.size)

        // Back 3: Tab0 at root, delegate to system
        val result3 = TreeMutator.popWithTabBehavior(state2)
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result3)
    }

    @Test
    fun `back navigation with tab switching`() {
        // Start on tab1 with single item
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(
                    ScreenNode("a", "tab0", HomeDestination),
                    ScreenNode("b", "tab0", ProfileDestination)
                )),
                StackNode("tab1", "tabs", listOf(
                    ScreenNode("c", "tab1", SettingsDestination)
                ))
            ),
            activeStackIndex = 1 // On tab1
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        // Back 1: TabNode is only child of root, delegate to system (no tab switching)
        val result1 = TreeMutator.popWithTabBehavior(root)
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result1)
    }

    @Test
    fun `canHandleBackNavigation tracks through full scenario`() {
        var current: NavNode = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        // Should be able to handle back until we get to single item
        var backCount = 0
        while (TreeMutator.canHandleBackNavigation(current)) {
            val result = TreeMutator.popWithTabBehavior(current)
            if (result is TreeMutator.BackResult.Handled) {
                current = result.newState
                backCount++
            } else {
                break
            }
        }

        assertEquals(2, backCount) // Popped twice (3 -> 2 -> 1)
        assertEquals(1, (current as StackNode).children.size)
        assertFalse(TreeMutator.canHandleBackNavigation(current))
    }

    // =========================================================================
    // CASCADE BACK HANDLING TESTS
    // =========================================================================

    @Test
    fun `nested stack cascade - parent has 1 child - cascades to grandparent`() {
        // Given: RootStack → ChildStack(1 item - GrandchildStack) → GrandchildStack(1 item)
        val grandchildScreen = ScreenNode("gc1", "grandchild", HomeDestination)
        val grandchildStack = StackNode(
            key = "grandchild",
            parentKey = "child",
            children = listOf(grandchildScreen)
        )
        val childStack = StackNode(
            key = "child",
            parentKey = "root",
            children = listOf(grandchildStack)
        )
        val rootScreen = ScreenNode("r1", "root", ProfileDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(rootScreen, childStack)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should pop childStack from root, revealing rootScreen
        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(1, newState.children.size)
        assertEquals("r1", newState.activeChild?.key)
    }

    @Test
    fun `nested stack cascade - parent is root with 1 child - delegates to system`() {
        // Given: RootStack(1 child) → ChildStack(1 item)
        val childScreen = ScreenNode("c1", "child", HomeDestination)
        val childStack = StackNode(
            key = "child",
            parentKey = "root",
            children = listOf(childScreen)
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(childStack)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should delegate to system since root has only 1 child
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result)
    }

    @Test
    fun `tab cascade - initial tab with 1 item - pops entire TabNode`() {
        // Given: RootStack → [Screen1, TabNode(initial tab with 1 item)]
        val tabScreen = ScreenNode("t1", "tab-stack-0", HomeDestination)
        val tabStack = StackNode(
            key = "tab-stack-0",
            parentKey = "tabs",
            children = listOf(tabScreen)
        )
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val rootScreen = ScreenNode("r1", "root", ProfileDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(rootScreen, tabNode)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should pop TabNode from root, revealing rootScreen
        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(1, newState.children.size)
        assertEquals("r1", newState.activeChild?.key)
    }

    @Test
    fun `tab cascade - initial tab with 1 item at root - delegates to system`() {
        // Given: RootStack(only TabNode) → TabNode(initial tab with 1 item)
        val tabScreen = ScreenNode("t1", "tab-stack-0", HomeDestination)
        val tabStack = StackNode(
            key = "tab-stack-0",
            parentKey = "tabs",
            children = listOf(tabScreen)
        )
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabNode)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should delegate to system
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result)
    }

    @Test
    fun `deep cascade - stack in tab in stack all with 1 item - cascades to grandparent`() {
        // Given: RootStack → [Screen1, MiddleStack(only TabNode) → TabNode(initial tab with 1 item)]
        val deepScreen = ScreenNode("d1", "tab-stack-0", HomeDestination)
        val tabStack = StackNode(
            key = "tab-stack-0",
            parentKey = "tabs",
            children = listOf(deepScreen)
        )
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "middle",
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val middleStack = StackNode(
            key = "middle",
            parentKey = "root",
            children = listOf(tabNode)
        )
        val rootScreen = ScreenNode("r1", "root", ProfileDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(rootScreen, middleStack)
        )

        // When
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should cascade through TabNode and MiddleStack, popping MiddleStack from root
        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(1, newState.children.size)
        assertEquals("r1", newState.activeChild?.key)
    }

    @Test
    fun `nested stack no cascade - parent has multiple children - normal pop`() {
        // Given: RootStack → [Screen1, ChildStack(1 item), ChildStack(1 item)]
        val child1Screen = ScreenNode("c1", "child1", HomeDestination)
        val child1Stack = StackNode(
            key = "child1",
            parentKey = "root",
            children = listOf(child1Screen)
        )
        val child2Screen = ScreenNode("c2", "child2", ProfileDestination)
        val child2Stack = StackNode(
            key = "child2",
            parentKey = "root",
            children = listOf(child2Screen)
        )
        val rootScreen = ScreenNode("r1", "root", SettingsDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(rootScreen, child1Stack, child2Stack)
        )

        // When: Back from child2 (active child)
        val result = TreeMutator.popWithTabBehavior(root)

        // Then: Should pop child2Stack (parent has multiple children, no cascade needed)
        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        assertEquals(2, newState.children.size)
        assertEquals("child1", newState.activeChild?.key)
    }

    @Test
    fun `canHandleBackNavigation returns true when cascade would handle back`() {
        // Given: RootStack → [Screen1, ChildStack(1 item)]
        val childScreen = ScreenNode("c1", "child", HomeDestination)
        val childStack = StackNode(
            key = "child",
            parentKey = "root",
            children = listOf(childScreen)
        )
        val rootScreen = ScreenNode("r1", "root", ProfileDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(rootScreen, childStack)
        )

        // When/Then: Should return true because cascade can pop childStack
        assertTrue(TreeMutator.canHandleBackNavigation(root))
    }

    @Test
    fun `canHandleBackNavigation returns true when TabNode can be cascade-popped`() {
        // Given: RootStack → [Screen1, TabNode(initial tab with 1 item)]
        val tabScreen = ScreenNode("t1", "tab-stack-0", HomeDestination)
        val tabStack = StackNode(
            key = "tab-stack-0",
            parentKey = "tabs",
            children = listOf(tabScreen)
        )
        val tabNode = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )
        val rootScreen = ScreenNode("r1", "root", ProfileDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(rootScreen, tabNode)
        )

        // When/Then: Should return true because TabNode can be popped from root
        assertTrue(TreeMutator.canHandleBackNavigation(root))
    }
}
