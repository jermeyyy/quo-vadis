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

    private object HomeDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    private object ProfileDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "profile"
    }

    private object SettingsDestination : Destination {
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

        assertIs<TreeMutator.BackResult.Handled>(result)
        val newState = result.newState as StackNode
        val newTabNode = newState.children.first() as TabNode
        assertEquals(0, newTabNode.activeStackIndex) // Switched to initial tab
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
    fun `popWithTabBehavior switches tab then pops stack on subsequent back`() {
        // Start on non-initial tab with single item
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

        // First back: should switch to initial tab
        val result1 = TreeMutator.popWithTabBehavior(root)
        assertIs<TreeMutator.BackResult.Handled>(result1)

        val state1 = result1.newState as StackNode
        val tabs1 = state1.children.first() as TabNode
        assertEquals(0, tabs1.activeStackIndex) // Now on tab 0

        // Second back: should delegate to system (on initial tab with single item)
        val result2 = TreeMutator.popWithTabBehavior(state1)
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result2)
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
    fun `canHandleBackNavigation returns true when tab can switch to initial`() {
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

        assertTrue(TreeMutator.canHandleBackNavigation(root))
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

        // Back 1: Switch to tab0 (initial tab)
        val result1 = TreeMutator.popWithTabBehavior(root)
        assertIs<TreeMutator.BackResult.Handled>(result1)
        val state1 = result1.newState as StackNode
        val tabs1 = state1.children.first() as TabNode
        assertEquals(0, tabs1.activeStackIndex)

        // Back 2: Pop B from tab0
        val result2 = TreeMutator.popWithTabBehavior(state1)
        assertIs<TreeMutator.BackResult.Handled>(result2)
        val state2 = result2.newState as StackNode
        val tabs2 = state2.children.first() as TabNode
        assertEquals(1, tabs2.stacks[0].children.size)

        // Back 3: Tab0 at root with single item, delegate to system
        val result3 = TreeMutator.popWithTabBehavior(state2)
        assertIs<TreeMutator.BackResult.DelegateToSystem>(result3)
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
}
