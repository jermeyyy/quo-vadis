package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for TreeMutator edge cases and utility operations.
 *
 * Tests cover:
 * - `replaceNode`: replaces any node by key
 * - `removeNode`: removes node and handles various scenarios
 * - `replaceCurrent`: replaces top screen
 * - `canGoBack`: returns correct value
 * - `currentDestination`: returns active destination
 * - Deeply nested tree operations
 * - Empty tree edge cases
 */
@OptIn(InternalQuoVadisApi::class)
class TreeMutatorEdgeCasesTest {

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

    private object ListDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "list"
    }

    // =========================================================================
    // TEST SETUP
    // =========================================================================

    private fun createKeyGenerator(): () -> NodeKey {
        var counter = 0
        return { NodeKey("edge-key-${counter++}") }
    }

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // REPLACE NODE TESTS
    // =========================================================================

    @Test
    fun `replaceNode replaces root node`() {
        val oldRoot = StackNode(NodeKey("root"), null, emptyList())
        val newRoot = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination))
        )

        val result = TreeMutator.replaceNode(oldRoot, NodeKey("root"), newRoot)

        assertSame(newRoot, result)
    }

    @Test
    fun `replaceNode replaces nested screen`() {
        val targetScreen = ScreenNode(NodeKey("target"), NodeKey("stack"), HomeDestination)
        val root = StackNode(
            key = NodeKey("stack"),
            parentKey = null,
            children = listOf(targetScreen)
        )

        val newScreen = ScreenNode(NodeKey("target"), NodeKey("stack"), ProfileDestination)
        val result = TreeMutator.replaceNode(root, NodeKey("target"), newScreen)

        val resultStack = result as StackNode
        assertEquals(1, resultStack.children.size)
        val replacedScreen = resultStack.children[0] as ScreenNode
        assertEquals(ProfileDestination, replacedScreen.destination)
    }

    @Test
    fun `replaceNode replaces node in TabNode`() {
        val targetStack = StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList())
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                targetStack,
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val newStack = StackNode(
            key = NodeKey("tab0"),
            parentKey = NodeKey("tabs"),
            children = listOf(ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination))
        )

        val result = TreeMutator.replaceNode(root, NodeKey("tab0"), newStack) as TabNode

        assertEquals(1, result.stacks[0].children.size)
        assertEquals(0, result.stacks[1].children.size)
    }

    @Test
    fun `replaceNode throws for non-existent key`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.replaceNode(root, NodeKey("nonexistent"), ScreenNode(NodeKey("new"), null, HomeDestination))
        }
    }

    @Test
    fun `replaceNode preserves structural sharing for unchanged branches`() {
        val unchangedScreen = ScreenNode(NodeKey("s2"), NodeKey("tab1"), ProfileDestination)
        val unchangedStack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(unchangedScreen))

        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                unchangedStack
            ),
            activeStackIndex = 0
        )

        val newScreen = ScreenNode(NodeKey("s1"), NodeKey("tab0"), SettingsDestination)
        val result = TreeMutator.replaceNode(root, NodeKey("s1"), newScreen) as TabNode

        // tab1 stack should be same reference
        assertSame(unchangedStack, result.stacks[1])
        assertSame(unchangedScreen, result.stacks[1].children[0])
    }

    @Test
    fun `replaceNode works with PaneNode`() {
        val targetScreen = ScreenNode(NodeKey("target"), NodeKey("primary-stack"), HomeDestination)
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(targetScreen))
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val newScreen = ScreenNode(NodeKey("target"), NodeKey("primary-stack"), ProfileDestination)
        val result = TreeMutator.replaceNode(root, NodeKey("target"), newScreen) as PaneNode

        val primaryStack = result.paneContent(PaneRole.Primary) as StackNode
        val replacedScreen = primaryStack.children[0] as ScreenNode
        assertEquals(ProfileDestination, replacedScreen.destination)
    }

    // =========================================================================
    // REMOVE NODE TESTS
    // =========================================================================

    @Test
    fun `removeNode removes screen from stack`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val result = TreeMutator.removeNode(root, NodeKey("s2"))

        assertNotNull(result)
        val resultStack = result as StackNode
        assertEquals(1, resultStack.children.size)
        assertEquals(NodeKey("s1"), resultStack.children[0].key)
    }

    @Test
    fun `removeNode returns null when removing root`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.removeNode(root, NodeKey("root"))

        assertNull(result)
    }

    @Test
    fun `removeNode throws when removing stack from TabNode`() {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.removeNode(root, NodeKey("tab0"))
        }
    }

    @Test
    fun `removeNode removes node from nested stack`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination),
                                ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.removeNode(root, NodeKey("s2"))

        assertNotNull(result)
        val tabs = (result as StackNode).children[0] as TabNode
        val tab0 = tabs.stacks[0]
        assertEquals(1, tab0.children.size)
        assertEquals(NodeKey("s1"), tab0.children[0].key)
    }

    @Test
    fun `removeNode throws for non-existent key`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.removeNode(root, NodeKey("nonexistent"))
        }
    }

    @Test
    fun `removeNode throws when removing pane content directly`() {
        val primaryContent = ScreenNode(NodeKey("primary"), NodeKey("panes"), HomeDestination)
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent)
            ),
            activePaneRole = PaneRole.Primary
        )

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.removeNode(root, NodeKey("primary"))
        }
    }

    @Test
    fun `removeNode removes nested content from PaneNode`() {
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), HomeDestination),
                            ScreenNode(NodeKey("s2"), NodeKey("primary-stack"), ProfileDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        val result = TreeMutator.removeNode(root, NodeKey("s2"))

        assertNotNull(result)
        val resultPanes = result as PaneNode
        val primaryStack = resultPanes.paneContent(PaneRole.Primary) as StackNode
        assertEquals(1, primaryStack.children.size)
        assertEquals(NodeKey("s1"), primaryStack.children[0].key)
    }

    // =========================================================================
    // REPLACE CURRENT TESTS
    // =========================================================================

    @Test
    fun `replaceCurrent replaces top screen`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val result = TreeMutator.replaceCurrent(
            root = root,
            destination = SettingsDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        assertEquals(2, result.children.size) // Same size
        assertEquals(
            HomeDestination,
            (result.children[0] as ScreenNode).destination
        ) // First unchanged
        assertEquals(
            SettingsDestination,
            (result.activeChild as ScreenNode).destination
        ) // Top replaced
    }

    @Test
    fun `replaceCurrent works with single screen`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.replaceCurrent(
            root = root,
            destination = ProfileDestination,
            generateKey = createKeyGenerator()
        ) as StackNode

        assertEquals(1, result.children.size)
        assertEquals(ProfileDestination, (result.activeChild as ScreenNode).destination)
    }

    @Test
    fun `replaceCurrent throws on empty stack`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        assertFailsWith<IllegalArgumentException> {
            TreeMutator.replaceCurrent(root, HomeDestination)
        }
    }

    @Test
    fun `replaceCurrent throws when no active stack`() {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        assertFailsWith<IllegalStateException> {
            TreeMutator.replaceCurrent(root, ProfileDestination)
        }
    }

    @Test
    fun `replaceCurrent targets deepest active stack in tabs`() {
        val root = TabNode(
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

        val result = TreeMutator.replaceCurrent(
            root = root,
            destination = DetailDestination,
            generateKey = createKeyGenerator()
        ) as TabNode

        // tab0 should have the replacement
        assertEquals(2, result.stacks[0].children.size)
        assertEquals(DetailDestination, (result.stacks[0].activeChild as ScreenNode).destination)

        // tab1 should be unchanged
        assertEquals(1, result.stacks[1].children.size)
        assertEquals(SettingsDestination, (result.stacks[1].activeChild as ScreenNode).destination)
    }

    // =========================================================================
    // CAN GO BACK TESTS
    // =========================================================================

    @Test
    fun `canGoBack returns true when stack has multiple items`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        assertTrue(TreeMutator.canGoBack(root))
    }

    @Test
    fun `canGoBack returns false when stack has single item`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        assertFalse(TreeMutator.canGoBack(root))
    }

    @Test
    fun `canGoBack returns false for empty stack`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        assertFalse(TreeMutator.canGoBack(root))
    }

    @Test
    fun `canGoBack returns false for ScreenNode`() {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        assertFalse(TreeMutator.canGoBack(root))
    }

    @Test
    fun `canGoBack checks deepest active stack in tabs`() {
        val root = TabNode(
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

        assertTrue(TreeMutator.canGoBack(root)) // tab0 has 2 items
    }

    @Test
    fun `canGoBack reflects active tab state`() {
        val root = TabNode(
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
            activeStackIndex = 1 // tab1 is active with single item
        )

        assertFalse(TreeMutator.canGoBack(root))
    }

    // =========================================================================
    // CURRENT DESTINATION TESTS
    // =========================================================================

    @Test
    fun `currentDestination returns active destination`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination),
                ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
            )
        )

        val current = TreeMutator.currentDestination(root)

        assertEquals(ProfileDestination, current)
    }

    @Test
    fun `currentDestination returns null for empty stack`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val current = TreeMutator.currentDestination(root)

        assertNull(current)
    }

    @Test
    fun `currentDestination returns destination from ScreenNode`() {
        val root = ScreenNode(NodeKey("screen"), null, HomeDestination)

        val current = TreeMutator.currentDestination(root)

        assertEquals(HomeDestination, current)
    }

    @Test
    fun `currentDestination follows active path in tabs`() {
        val root = TabNode(
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
            activeStackIndex = 1
        )

        val current = TreeMutator.currentDestination(root)

        assertEquals(ProfileDestination, current)
    }

    @Test
    fun `currentDestination follows active pane`() {
        val root = PaneNode(
            key = NodeKey("panes"),
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s1"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                            ScreenNode(NodeKey("s2"), NodeKey("supporting-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Supporting
        )

        val current = TreeMutator.currentDestination(root)

        assertEquals(DetailDestination, current)
    }

    // =========================================================================
    // DEEPLY NESTED TREE TESTS
    // =========================================================================

    @Test
    fun `operations work on deeply nested structure`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("intro"), NodeKey("root"), HomeDestination),
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                PaneNode(
                                    key = NodeKey("panes"),
                                    parentKey = NodeKey("tab0"),
                                    paneConfigurations = mapOf(
                                        PaneRole.Primary to PaneConfiguration(
                                            StackNode(NodeKey("primary-stack"), NodeKey("panes"), listOf(
                                                    ScreenNode(NodeKey("list"), NodeKey("primary-stack"),
                                                        ListDestination
                                                    )
                                                )
                                            )
                                        ),
                                        PaneRole.Supporting to PaneConfiguration(
                                            StackNode(NodeKey("supporting-stack"), NodeKey("panes"), listOf(
                                                    ScreenNode(NodeKey("detail"), NodeKey("supporting-stack"),
                                                        DetailDestination
                                                    )
                                                )
                                            )
                                        )
                                    ),
                                    activePaneRole = PaneRole.Supporting
                                )
                            )
                        ),
                        StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("settings"), NodeKey("tab1"), SettingsDestination)
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        // Current destination should be in supporting pane of tab0
        assertEquals(DetailDestination, TreeMutator.currentDestination(root))

        // Push to deepest active stack
        val afterPush = TreeMutator.push(root, ProfileDestination) { NodeKey("new-screen") }
        val supportingStack = afterPush.findByKey(NodeKey("supporting-stack")) as StackNode
        assertEquals(2, supportingStack.children.size)

        // Switch tab
        val afterTabSwitch = TreeMutator.switchTab(afterPush, NodeKey("tabs"), 1)
        assertEquals(SettingsDestination, TreeMutator.currentDestination(afterTabSwitch))

        // CanGoBack should check active path
        assertFalse(TreeMutator.canGoBack(afterTabSwitch)) // tab1 has single item
    }

    @Test
    fun `structural sharing preserved in deeply nested operations`() {
        val tab1Screen = ScreenNode(NodeKey("settings"), NodeKey("tab1"), SettingsDestination)
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tab1Screen))

        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                TabNode(
                    key = NodeKey("tabs"),
                    parentKey = NodeKey("root"),
                    stacks = listOf(
                        StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                                ScreenNode(NodeKey("home"), NodeKey("tab0"), HomeDestination)
                            )
                        ),
                        tab1Stack
                    ),
                    activeStackIndex = 0
                )
            )
        )

        val result = TreeMutator.push(root, ProfileDestination) { NodeKey("new") }

        // tab1 branch should be completely unchanged
        val resultTabs = (result as StackNode).children[0] as TabNode
        assertSame(tab1Stack, resultTabs.stacks[1])
        assertSame(tab1Screen, resultTabs.stacks[1].children[0])
    }

    // =========================================================================
    // EMPTY TREE EDGE CASES
    // =========================================================================

    @Test
    fun `push to empty root stack`() {
        val root = StackNode(NodeKey("root"), null, emptyList())

        val result = TreeMutator.push(root, HomeDestination) { NodeKey("first") }

        val resultStack = result as StackNode
        assertEquals(1, resultStack.children.size)
    }

    @Test
    fun `pop from tree with single screen returns empty stack`() {
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("only"), NodeKey("root"), HomeDestination)
            )
        )

        val result = TreeMutator.pop(root)

        // With default PRESERVE_EMPTY, returns tree with empty stack
        assertNotNull(result)
        assertTrue((result as StackNode).isEmpty)
    }

    @Test
    fun `operations on empty TabNode stacks`() {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), emptyList()),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        // currentDestination should be null
        assertNull(TreeMutator.currentDestination(root))

        // canGoBack should be false
        assertFalse(TreeMutator.canGoBack(root))

        // pop should return null
        assertNull(TreeMutator.pop(root))

        // push should work
        val afterPush = TreeMutator.push(root, HomeDestination) { NodeKey("first") }
        val tabs = afterPush as TabNode
        assertEquals(1, tabs.stacks[0].children.size)
        assertEquals(0, tabs.stacks[1].children.size)
    }

    @Test
    fun `switchTab preserves empty stack state`() {
        val root = TabNode(
            key = NodeKey("tabs"),
            parentKey = null,
            stacks = listOf(
                StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(
                        ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
                    )
                ),
                StackNode(NodeKey("tab1"), NodeKey("tabs"), emptyList())
            ),
            activeStackIndex = 0
        )

        val result = TreeMutator.switchTab(root, NodeKey("tabs"), 1) as TabNode

        assertEquals(1, result.activeStackIndex)
        assertEquals(1, result.stacks[0].children.size) // tab0 unchanged
        assertEquals(0, result.stacks[1].children.size) // tab1 still empty
    }

    // =========================================================================
    // CONCURRENT-LIKE SCENARIOS
    // =========================================================================

    @Test
    fun `multiple sequential operations maintain integrity`() {
        var state: NavNode = StackNode(NodeKey("root"), null, emptyList())

        // Push multiple screens
        state = TreeMutator.push(state, HomeDestination) { NodeKey("s1") }
        state = TreeMutator.push(state, ProfileDestination) { NodeKey("s2") }
        state = TreeMutator.push(state, SettingsDestination) { NodeKey("s3") }

        assertEquals(3, (state as StackNode).children.size)
        assertEquals(SettingsDestination, TreeMutator.currentDestination(state))

        // Pop one
        state = TreeMutator.pop(state)!!
        assertEquals(2, (state as StackNode).children.size)
        assertEquals(ProfileDestination, TreeMutator.currentDestination(state))

        // Replace current
        state = TreeMutator.replaceCurrent(state, DetailDestination) { NodeKey("s4") }
        assertEquals(2, (state as StackNode).children.size)
        assertEquals(DetailDestination, TreeMutator.currentDestination(state))

        // Clear and push
        state = TreeMutator.clearAndPush(state, ListDestination) { NodeKey("s5") }
        assertEquals(1, (state as StackNode).children.size)
        assertEquals(ListDestination, TreeMutator.currentDestination(state))
    }

    @Test
    fun `immutability ensures old state is unchanged`() {
        val original = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
            )
        )

        val modified = TreeMutator.push(original, ProfileDestination) { NodeKey("s2") }

        // Original should be unchanged
        assertEquals(1, original.children.size)
        assertEquals(HomeDestination, (original.activeChild as ScreenNode).destination)

        // Modified should have new state
        assertEquals(2, (modified as StackNode).children.size)
        assertEquals(ProfileDestination, (modified.activeChild as ScreenNode).destination)
    }
}
