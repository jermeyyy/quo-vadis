package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.navigation.TabNode
import com.jermey.quo.vadis.core.navigation.findByKey
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.NavigationTransition
import com.jermey.quo.vadis.core.navigation.tree.TreeMutator
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Unit tests for TreeMutator tab operations.
 *
 * Tests cover:
 * - `switchTab`: changes activeStackIndex on specific TabNode
 * - `switchActiveTab`: finds TabNode in active path and switches
 */
class TreeMutatorTabTest {

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
    // SWITCH TAB TESTS
    // =========================================================================

    @Test
    fun `switchTab updates activeStackIndex`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList()),
                StackNode("tab2", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(tabs, "tabs", 2)

        assertEquals(2, (result as TabNode).activeStackIndex)
    }

    @Test
    fun `switchTab preserves all stacks`() {
        val screen1 = ScreenNode("s1", "tab0", HomeDestination)
        val screen2 = ScreenNode("s2", "tab1", ProfileDestination)
        val screen3 = ScreenNode("s3", "tab1", SettingsDestination)

        val stack0 = StackNode("tab0", "tabs", listOf(screen1))
        val stack1 = StackNode("tab1", "tabs", listOf(screen2, screen3))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(tabs, "tabs", 1) as TabNode

        assertEquals(1, result.activeStackIndex)
        assertEquals(1, result.stacks[0].children.size) // tab0 unchanged
        assertEquals(2, result.stacks[1].children.size) // tab1 unchanged
    }

    @Test
    fun `switchTab preserves structural sharing`() {
        val screen1 = ScreenNode("s1", "tab0", HomeDestination)
        val screen2 = ScreenNode("s2", "tab1", ProfileDestination)

        val stack0 = StackNode("tab0", "tabs", listOf(screen1))
        val stack1 = StackNode("tab1", "tabs", listOf(screen2))

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(tabs, "tabs", 1) as TabNode

        // Both stacks should be same references
        assertSame(stack0, result.stacks[0])
        assertSame(stack1, result.stacks[1])
        assertSame(screen1, result.stacks[0].children[0])
        assertSame(screen2, result.stacks[1].children[0])
    }

    @Test
    fun `switchTab returns same state when already on target tab`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 1
        )

        val result = TreeMutator.switchTab(tabs, "tabs", 1)

        // Should be same reference (no change)
        assertSame(tabs, result)
    }

    @Test
    fun `switchTab throws for invalid index - too high`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(StackNode("tab0", "tabs", emptyList())),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, "tabs", 5)
        }
    }

    @Test
    fun `switchTab throws for negative index`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(StackNode("tab0", "tabs", emptyList())),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, "tabs", -1)
        }
    }

    @Test
    fun `switchTab throws for non-existent TabNode key`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(StackNode("tab0", "tabs", emptyList())),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, "nonexistent", 0)
        }
    }

    @Test
    fun `switchTab works with nested TabNode`() {
        val innerTabs = TabNode(
            key = "inner-tabs",
            parentKey = "tab0",
            stacks = listOf(
                StackNode("inner-tab0", "inner-tabs", emptyList()),
                StackNode("inner-tab1", "inner-tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        val root = TabNode(
            key = "outer-tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "outer-tabs", listOf(innerTabs))
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(root, "inner-tabs", 1)

        val resultOuterTabs = result as TabNode
        val resultInnerTabs = resultOuterTabs.stacks[0].children[0] as TabNode
        assertEquals(1, resultInnerTabs.activeStackIndex)
    }

    @Test
    fun `switchTab in StackNode containing TabNode`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s1", "tab0", HomeDestination)
                    )
                ),
                StackNode(
                    "tab1", "tabs", listOf(
                        ScreenNode("s2", "tab1", ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabs)
        )

        val result = TreeMutator.switchTab(root, "tabs", 1)

        val resultStack = result as StackNode
        val resultTabs = resultStack.children[0] as TabNode
        assertEquals(1, resultTabs.activeStackIndex)
    }

    // =========================================================================
    // SWITCH ACTIVE TAB TESTS
    // =========================================================================

    @Test
    fun `switchActiveTab finds TabNode in active path`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode("tab0", "tabs", emptyList()),
                        StackNode("tab1", "tabs", emptyList())
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.switchActiveTab(root, 1)

        val tabs = (result as StackNode).children[0] as TabNode
        assertEquals(1, tabs.activeStackIndex)
    }

    @Test
    fun `switchActiveTab throws when no TabNode in path`() {
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", HomeDestination)
            )
        )

        assertFailsWith<IllegalStateException> {
            TreeMutator.switchActiveTab(root, 1)
        }
    }

    @Test
    fun `switchActiveTab throws for empty stack`() {
        val root = StackNode("root", null, emptyList())

        assertFailsWith<IllegalStateException> {
            TreeMutator.switchActiveTab(root, 0)
        }
    }

    @Test
    fun `switchActiveTab finds first TabNode in active path with multiple tabs`() {
        // Nested tabs scenario - should find the FIRST tab in active path
        val innerTabs = TabNode(
            key = "inner-tabs",
            parentKey = "tab0",
            stacks = listOf(
                StackNode("inner-tab0", "inner-tabs", emptyList()),
                StackNode("inner-tab1", "inner-tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        val outerTabs = TabNode(
            key = "outer-tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "outer-tabs", listOf(innerTabs)),
                StackNode("tab1", "outer-tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchActiveTab(outerTabs, 1)

        // Should switch the FIRST TabNode found (outer-tabs)
        val resultTabs = result as TabNode
        assertEquals(1, resultTabs.activeStackIndex)
        assertEquals("outer-tabs", resultTabs.key)
    }

    @Test
    fun `switchActiveTab returns same state when already on target index`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 1
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(tabs)
        )

        val result = TreeMutator.switchActiveTab(root, 1)

        // Should be same reference since target index is already active
        assertSame(root, result)
    }

    @Test
    fun `switchActiveTab throws for invalid index`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchActiveTab(tabs, 5)
        }
    }

    @Test
    fun `switchActiveTab works with deeply nested structure`() {
        val targetTabs = TabNode(
            key = "target-tabs",
            parentKey = "inner-stack",
            stacks = listOf(
                StackNode(
                    "target-tab0", "target-tabs", listOf(
                        ScreenNode("s1", "target-tab0", HomeDestination)
                    )
                ),
                StackNode(
                    "target-tab1", "target-tabs", listOf(
                        ScreenNode("s2", "target-tab1", ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                StackNode(
                    key = "inner-stack",
                    parentKey = "root",
                    children = listOf(targetTabs)
                )
            )
        )

        val result = TreeMutator.switchActiveTab(root, 1)

        // Find the target-tabs and verify it switched
        val foundTabs = result.findByKey("target-tabs") as TabNode
        assertEquals(1, foundTabs.activeStackIndex)
    }

    // =========================================================================
    // TAB NAVIGATION INTEGRATION TESTS
    // =========================================================================

    @Test
    fun `switching tabs then pushing maintains separate histories`() {
        var current: NavNode = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s1", "tab0", HomeDestination)
                    )
                ),
                StackNode(
                    "tab1", "tabs", listOf(
                        ScreenNode("s2", "tab1", ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        // Switch to tab1
        current = TreeMutator.switchActiveTab(current, 1)
        assertEquals(1, (current as TabNode).activeStackIndex)

        // Push to tab1 (active)
        current = TreeMutator.push(current, SettingsDestination) { "key-new" }

        val tabs = current as TabNode
        // Tab1 should have 2 items now
        assertEquals(2, tabs.stacks[1].children.size)
        // Tab0 should still have 1 item
        assertEquals(1, tabs.stacks[0].children.size)
    }

    @Test
    fun `switching back to previous tab preserves its history`() {
        var current: NavNode = TabNode(
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

        // Switch to tab1
        current = TreeMutator.switchActiveTab(current, 1)

        // Switch back to tab0
        current = TreeMutator.switchActiveTab(current, 0)

        val tabs = current as TabNode
        assertEquals(0, tabs.activeStackIndex)
        // Tab0 should still have its 2 items
        assertEquals(2, tabs.stacks[0].children.size)
        assertEquals(ProfileDestination, (tabs.stacks[0].activeChild as ScreenNode).destination)
    }
}
