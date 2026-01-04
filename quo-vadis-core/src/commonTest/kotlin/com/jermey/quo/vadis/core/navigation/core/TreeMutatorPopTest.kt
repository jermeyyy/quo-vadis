package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.internal.tree.config.PopBehavior
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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
class TreeMutatorPopTest {

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

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // POP TESTS
    // =========================================================================

    @Test
    fun `pop removes last screen from stack`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination)
            )
        )

        val result = TreeMutator.pop(root)

        assertNotNull(result)
        assertEquals(1, (result as StackNode).children.size)
        assertEquals(HomeDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `pop with single item at root returns empty stack with PRESERVE_EMPTY`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination)
            )
        )

        val result = TreeMutator.pop(root, PopBehavior.PRESERVE_EMPTY)

        // PRESERVE_EMPTY returns a tree with empty stack
        assertNotNull(result)
        assertTrue((result as StackNode).isEmpty)
    }

    @Test
    fun `pop returns null on empty stack`() {
        val root = StackNode("root", null, emptyList())

        val result = TreeMutator.pop(root)

        assertNull(result)
    }

    @Test
    fun `pop with PRESERVE_EMPTY preserves empty stack structure`() {
        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(
                ScreenNode("s1", "inner", HomeDestination)
            )
        )
        val outerStack = StackNode(
            key = "outer",
            parentKey = null,
            children = listOf(innerStack)
        )

        val result = TreeMutator.pop(outerStack, PopBehavior.PRESERVE_EMPTY)

        // PRESERVE_EMPTY keeps the structure - inner stack becomes empty
        assertNotNull(result)
        val resultOuter = result as StackNode
        val resultInner = resultOuter.children[0] as StackNode
        assertTrue(resultInner.isEmpty)
    }

    @Test
    fun `pop removes screen and preserves non-empty stack`() {
        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(
                ScreenNode("s1", "inner", HomeDestination),
                ScreenNode("s2", "inner", ProfileDestination)
            )
        )
        val outerStack = StackNode(
            key = "outer",
            parentKey = null,
            children = listOf(innerStack)
        )

        val result = TreeMutator.pop(outerStack)

        assertNotNull(result)
        val resultOuter = result as StackNode
        val resultInner = resultOuter.children[0] as StackNode
        assertEquals(1, resultInner.children.size)
        assertEquals(HomeDestination, (resultInner.activeChild as ScreenNode).destination)
    }

    @Test
    fun `pop from tab affects active tab only`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s1", "tab0", HomeDestination),
                        ScreenNode("s2", "tab0", ProfileDestination)
                    )
                ),
                StackNode(
                    "tab1", "tabs", listOf(
                        ScreenNode("s3", "tab1", SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs)

        assertNotNull(result)
        val resultTabs = result as TabNode

        // Tab0 should have 1 item
        assertEquals(1, resultTabs.stacks[0].children.size)
        assertEquals(HomeDestination, (resultTabs.stacks[0].activeChild as ScreenNode).destination)

        // Tab1 should be unchanged
        assertEquals(1, resultTabs.stacks[1].children.size)
    }

    @Test
    fun `pop preserves structural sharing for unchanged branches`() {
        val tab1Screen = ScreenNode("s3", "tab1", SettingsDestination)
        val tab1Stack = StackNode("tab1", "tabs", listOf(tab1Screen))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s1", "tab0", HomeDestination),
                        ScreenNode("s2", "tab0", ProfileDestination)
                    )
                ),
                tab1Stack
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs) as TabNode

        // Tab1 stack should be same reference
        assertSame(tab1Stack, result.stacks[1])
        assertSame(tab1Screen, result.stacks[1].children[0])
    }

    @Test
    fun `pop in tabs with single item returns empty stack with PRESERVE_EMPTY`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s1", "tab0", HomeDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs)

        // With default PRESERVE_EMPTY, tab's stack becomes empty but structure remains
        assertNotNull(result)
        val resultTabs = result as TabNode
        assertTrue(resultTabs.stacks[0].isEmpty)
    }

    @Test
    fun `pop returns null for ScreenNode root`() {
        val root = ScreenNode("screen", null, HomeDestination)

        val result = TreeMutator.pop(root)

        assertNull(result)
    }

    // =========================================================================
    // POP TO TESTS
    // =========================================================================

    @Test
    fun `popTo removes screens until predicate matches`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.popTo(root, inclusive = false) { node ->
            node is ScreenNode && node.destination == ProfileDestination
        }

        assertEquals(2, (result as StackNode).children.size)
        assertEquals(ProfileDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `popTo with inclusive removes matching screen`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.popTo(root, inclusive = true) { node ->
            node is ScreenNode && node.destination == ProfileDestination
        }

        assertEquals(1, (result as StackNode).children.size)
        assertEquals(HomeDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `popTo returns original when predicate not matched`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination)
            )
        )

        val result = TreeMutator.popTo(root) { false }

        assertSame(root, result)
    }

    @Test
    fun `popTo returns original when no active stack`() {
        val root = ScreenNode("screen", null, HomeDestination)

        val result = TreeMutator.popTo(root) { true }

        assertSame(root, result)
    }

    @Test
    fun `popTo preserves at least one item when inclusive would empty stack`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination)
            )
        )

        // Try to pop inclusive to the first screen - should keep at least one
        val result = TreeMutator.popTo(root, inclusive = true) { node ->
            node is ScreenNode && node.destination == HomeDestination
        }

        // Since popping inclusive to the first would empty the stack, it returns original
        assertSame(root, result)
    }

    @Test
    fun `popTo works in tabs targeting active stack`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s1", "tab0", HomeDestination),
                        ScreenNode("s2", "tab0", ProfileDestination),
                        ScreenNode("s3", "tab0", SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.popTo(tabs, inclusive = false) { node ->
            node is ScreenNode && node.destination == HomeDestination
        }

        val resultTabs = result as TabNode
        assertEquals(1, resultTabs.stacks[0].children.size)
        assertEquals(HomeDestination, (resultTabs.stacks[0].activeChild as ScreenNode).destination)
    }

    // =========================================================================
    // POP TO ROUTE TESTS
    // =========================================================================

    @Test
    fun `popToRoute finds screen by route`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.popToRoute(root, "home")

        assertEquals(1, (result as StackNode).children.size)
        assertEquals(HomeDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `popToRoute with inclusive false keeps matching screen`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.popToRoute(root, "profile", inclusive = false)

        assertEquals(2, (result as StackNode).children.size)
        assertEquals(ProfileDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `popToRoute returns original when route not found`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination)
            )
        )

        val result = TreeMutator.popToRoute(root, "nonexistent")

        assertSame(root, result)
    }

    @Test
    fun `popToRoute works with multiple screens of different routes`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", HomeDestination), // Same route as first
                ScreenNode("s4", "root", SettingsDestination)
            )
        )

        // Should find the LAST occurrence of "home" (searching from back)
        val result = TreeMutator.popToRoute(root, "home", inclusive = false)

        assertEquals(3, (result as StackNode).children.size)
        // The third screen (index 2) is the last "home"
        assertEquals(HomeDestination, (result.activeChild as ScreenNode).destination)
    }

    // =========================================================================
    // POP TO DESTINATION TESTS
    // =========================================================================

    @Test
    fun `popToDestination finds screen by destination type`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.popToDestination<HomeDestination>(root)

        assertEquals(1, (result as StackNode).children.size)
        assertTrue(result.activeChild is ScreenNode)
        assertTrue((result.activeChild as ScreenNode).destination is HomeDestination)
    }

    @Test
    fun `popToDestination with inclusive removes matching screen`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
            )
        )

        val result = TreeMutator.popToDestination<ProfileDestination>(root, inclusive = true)

        assertEquals(1, (result as StackNode).children.size)
        assertTrue((result.activeChild as ScreenNode).destination is HomeDestination)
    }

    // =========================================================================
    // POP BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun `pop with CASCADE behavior on tab preserves empty stack`() {
        // In tabs, CASCADE cannot remove a stack - it would break the tab structure
        // So it preserves the empty stack structure
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s1", "tab0", HomeDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.pop(tabs, PopBehavior.CASCADE)

        // CASCADE in tabs falls back to preserving empty stack
        assertNotNull(result)
        val resultTabs = result as TabNode
        assertTrue(resultTabs.stacks[0].isEmpty)
    }

    @Test
    fun `pop with CASCADE removes nested empty stack from parent stack`() {
        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(
                ScreenNode("s1", "inner", HomeDestination)
            )
        )
        val outerStack = StackNode(
            key = "outer",
            parentKey = null,
            children = listOf(
                ScreenNode("s0", "outer", ProfileDestination),
                innerStack
            )
        )

        // Pop from inner stack - with CASCADE, should try to remove empty inner
        val result = TreeMutator.pop(outerStack, PopBehavior.CASCADE)

        assertNotNull(result)
        val resultOuter = result as StackNode
        // Inner stack should be removed after becoming empty, leaving just s0
        assertEquals(1, resultOuter.children.size)
        assertTrue(resultOuter.children[0] is ScreenNode)
        assertEquals(ProfileDestination, (resultOuter.children[0] as ScreenNode).destination)
    }

    @Test
    fun `pop multiple times until cannot go back`() {
        var current: NavNode = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination),
                ScreenNode("s2", "root", ProfileDestination),
                ScreenNode("s3", "root", SettingsDestination)
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
        assertEquals(2, popCount)
        assertEquals(1, (current as StackNode).children.size)
    }
}
