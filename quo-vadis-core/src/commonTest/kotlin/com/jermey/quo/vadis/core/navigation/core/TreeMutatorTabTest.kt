package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
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
@OptIn(InternalQuoVadisApi::class)
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
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab2"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(tabs, NodeKey("tabs"), 2)

        assertEquals(2, (result as TabNode).activeStackIndex)
    }

    @Test
    fun `switchTab preserves all stacks`() {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
        val screen3 = ScreenNode(NodeKey("s3"), NodeKey("tab1"), SettingsDestination)

        val stack0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen1))
        val stack1 = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(screen2, screen3))

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(tabs, NodeKey("tabs"), 1) as TabNode

        assertEquals(1, result.activeStackIndex)
        assertEquals(1, result.stacks[0].children.size) // tab0 unchanged
        assertEquals(2, result.stacks[1].children.size) // tab1 unchanged
    }

    @Test
    fun `switchTab preserves structural sharing`() {
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)

        val stack0 = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen1))
        val stack1 = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(screen2))

        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(tabs, NodeKey("tabs"), 1) as TabNode

        // Both stacks should be same references
        assertSame(stack0, result.stacks[0])
        assertSame(stack1, result.stacks[1])
        assertSame(screen1, result.stacks[0].children[0])
        assertSame(screen2, result.stacks[1].children[0])
    }

    @Test
    fun `switchTab returns same state when already on target tab`() {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 1
        )

        val result = TreeMutator.switchTab(tabs, NodeKey("tabs"), 1)

        // Should be same reference (no change)
        assertSame(tabs, result)
    }

    @Test
    fun `switchTab throws for invalid index - too high`() {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, NodeKey("tabs"), 5)
        }
    }

    @Test
    fun `switchTab throws for negative index`() {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, NodeKey("tabs"), -1)
        }
    }

    @Test
    fun `switchTab throws for non-existent TabNode key`() {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.switchTab(tabs, NodeKey("nonexistent"), 0)
        }
    }

    @Test
    fun `switchTab works with nested TabNode`() {
        val innerTabs = TabNode(
            key = NodeKey("inner-tabs"),
            parentKey = NodeKey("tab0"),
            stacks = listOf(
                StackNode(NodeKey("inner-tab0"), NodeKey("inner-tabs"), emptyList()),
                StackNode(NodeKey("inner-tab1"), NodeKey("inner-tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val root = TabNode(
            key = NodeKey("outer-tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("outer-tabs"), listOf(innerTabs))
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(root, NodeKey("inner-tabs"), 1)

        val resultOuterTabs = result as TabNode
        val resultInnerTabs = resultOuterTabs.stacks[0].children[0] as TabNode
        assertEquals(1, resultInnerTabs.activeStackIndex)
    }

    @Test
    fun `switchTab in StackNode containing TabNode`() {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabs)
        )

        val result = TreeMutator.switchTab(root, NodeKey("tabs"), 1)

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
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
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
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        assertFailsWith<IllegalStateException> {
            TreeMutator.switchActiveTab(root, 1)
        }
    }

    @Test
    fun `switchActiveTab throws for empty stack`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        assertFailsWith<IllegalStateException> {
            TreeMutator.switchActiveTab(root, 0)
        }
    }

    @Test
    fun `switchActiveTab finds first TabNode in active path with multiple tabs`() {
        // Nested tabs scenario - should find the FIRST tab in active path
        val innerTabs = TabNode(
            key = NodeKey("inner-tabs"),
            parentKey = NodeKey("tab0"),
            stacks = listOf(
                StackNode(NodeKey("inner-tab0"), NodeKey("inner-tabs"), emptyList()),
                StackNode(NodeKey("inner-tab1"), NodeKey("inner-tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val outerTabs = TabNode(
            key = NodeKey("outer-tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("outer-tabs"), listOf(innerTabs)),
                StackNode(NodeKey("tab1"), NodeKey("outer-tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchActiveTab(outerTabs, 1)

        // Should switch the FIRST TabNode found (outer-tabs)
        val resultTabs = result as TabNode
        assertEquals(1, resultTabs.activeStackIndex)
        assertEquals(NodeKey("outer-tabs"), resultTabs.key)
    }

    @Test
    fun `switchActiveTab returns same state when already on target index`() {
        val tabs = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 1
        )

        val root = StackNode(
            key = NodeKey("root"),
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
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())
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
            key = NodeKey("target-tabs"),
            parentKey = NodeKey("inner-stack"),
            stacks = listOf(
                StackNode(NodeKey("target-tab0"), NodeKey("target-tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("target-tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("target-tab1"), NodeKey("target-tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("target-tab1"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                StackNode(
                    key = NodeKey("inner-stack"),
                    parentKey = NodeKey("root"),
                    children = listOf(targetTabs)
                )
            )
        )

        val result = TreeMutator.switchActiveTab(root, 1)

        // Find the target-tabs and verify it switched
        val foundTabs = result.findByKey(NodeKey("target-tabs")) as TabNode
        assertEquals(1, foundTabs.activeStackIndex)
    }

    // =========================================================================
    // TAB NAVIGATION INTEGRATION TESTS
    // =========================================================================

    @Test
    fun `switching tabs then pushing maintains separate histories`() {
        var current: NavNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        // Switch to tab1
        current = TreeMutator.switchActiveTab(current, 1)
        assertEquals(1, (current as TabNode).activeStackIndex)

        // Push to tab1 (active)
        current = TreeMutator.push(current, SettingsDestination) { NodeKey("key-new") }

        val tabs = current as TabNode
        // Tab1 should have 2 items now
        assertEquals(2, tabs.stacks[1].children.size)
        // Tab0 should still have 1 item
        assertEquals(1, tabs.stacks[0].children.size)
    }

    @Test
    fun `switching back to previous tab preserves its history`() {
        var current: NavNode = TabNode(
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
