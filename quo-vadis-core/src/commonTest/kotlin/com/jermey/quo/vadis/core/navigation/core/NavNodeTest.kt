@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.node.activePathToLeaf
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.node.allPaneNodes
import com.jermey.quo.vadis.core.navigation.node.allScreens
import com.jermey.quo.vadis.core.navigation.node.allStackNodes
import com.jermey.quo.vadis.core.navigation.node.allTabNodes
import com.jermey.quo.vadis.core.navigation.node.depth
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.node.nodeCount
import com.jermey.quo.vadis.core.navigation.node.paneForRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for the NavNode hierarchy.
 *
 * Tests cover:
 * - ScreenNode: creation, validation, properties
 * - StackNode: activeChild, canGoBack, isEmpty, size
 * - TabNode: validation (at least one stack, bounds checking), activeStack, stackAt, tabCount
 * - PaneNode: validation, activePane, paneCount
 * - Extension functions: findByKey, activePathToLeaf, activeLeaf, activeStack, allScreens, etc.
 */
class NavNodeTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object SettingsDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object FeedDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ListDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // SCREEN NODE TESTS
    // =========================================================================

    @Test
    fun `ScreenNode holds destination correctly`() {
        val node = ScreenNode(
            key = "screen-1",
            parentKey = "stack-1",
            destination = HomeDestination
        )

        assertEquals("screen-1", node.key)
        assertEquals("stack-1", node.parentKey)
        assertEquals(HomeDestination, node.destination)
    }

    @Test
    fun `ScreenNode with null parentKey is valid root screen`() {
        val node = ScreenNode(
            key = "root-screen",
            parentKey = null,
            destination = HomeDestination
        )

        assertNull(node.parentKey)
        assertEquals("root-screen", node.key)
    }

    @Test
    fun `ScreenNode equality based on properties`() {
        val node1 = ScreenNode("key1", "parent", HomeDestination)
        val node2 = ScreenNode("key1", "parent", HomeDestination)
        val node3 = ScreenNode("key2", "parent", HomeDestination)

        assertEquals(node1, node2)
        assertFalse(node1 == node3)
    }

    // =========================================================================
    // STACK NODE TESTS
    // =========================================================================

    @Test
    fun `StackNode activeChild returns last element`() {
        val screen1 = ScreenNode("s1", "stack", HomeDestination)
        val screen2 = ScreenNode("s2", "stack", ProfileDestination)
        val screen3 = ScreenNode("s3", "stack", SettingsDestination)

        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(screen1, screen2, screen3)
        )

        assertEquals(screen3, stack.activeChild)
    }

    @Test
    fun `StackNode activeChild returns null when empty`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = emptyList()
        )

        assertNull(stack.activeChild)
    }

    @Test
    fun `StackNode canGoBack true when multiple children`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "stack", HomeDestination),
                ScreenNode("s2", "stack", ProfileDestination)
            )
        )

        assertTrue(stack.canGoBack)
    }

    @Test
    fun `StackNode canGoBack false when single child`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(ScreenNode("s1", "stack", HomeDestination))
        )

        assertFalse(stack.canGoBack)
    }

    @Test
    fun `StackNode canGoBack false when empty`() {
        val stack = StackNode("stack", null, emptyList())

        assertFalse(stack.canGoBack)
    }

    @Test
    fun `StackNode isEmpty true when no children`() {
        val stack = StackNode("stack", null, emptyList())

        assertTrue(stack.isEmpty)
        assertEquals(0, stack.size)
    }

    @Test
    fun `StackNode isEmpty false when has children`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(ScreenNode("s1", "stack", HomeDestination))
        )

        assertFalse(stack.isEmpty)
        assertEquals(1, stack.size)
    }

    @Test
    fun `StackNode size reflects children count`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "stack", HomeDestination),
                ScreenNode("s2", "stack", ProfileDestination),
                ScreenNode("s3", "stack", SettingsDestination)
            )
        )

        assertEquals(3, stack.size)
    }

    @Test
    fun `StackNode with nested stack`() {
        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(ScreenNode("s1", "inner", HomeDestination))
        )
        val outerStack = StackNode(
            key = "outer",
            parentKey = null,
            children = listOf(innerStack)
        )

        assertEquals(innerStack, outerStack.activeChild)
        assertFalse(outerStack.canGoBack)
    }

    // =========================================================================
    // TAB NODE TESTS
    // =========================================================================

    @Test
    fun `TabNode requires at least one stack`() {
        assertFailsWith<IllegalArgumentException> {
            TabNode(
                key = "tabs",
                parentKey = null,
                stacks = emptyList(),
                activeStackIndex = 0
            )
        }
    }

    @Test
    fun `TabNode validates activeStackIndex bounds - too high`() {
        val stack = StackNode("s1", "tabs", emptyList())

        assertFailsWith<IllegalArgumentException> {
            TabNode(
                key = "tabs",
                parentKey = null,
                stacks = listOf(stack),
                activeStackIndex = 5
            )
        }
    }

    @Test
    fun `TabNode validates negative activeStackIndex`() {
        val stack = StackNode("s1", "tabs", emptyList())

        assertFailsWith<IllegalArgumentException> {
            TabNode(
                key = "tabs",
                parentKey = null,
                stacks = listOf(stack),
                activeStackIndex = -1
            )
        }
    }

    @Test
    fun `TabNode activeStack returns correct stack`() {
        val stack0 = StackNode("s0", "tabs", emptyList())
        val stack1 = StackNode("s1", "tabs", emptyList())
        val stack2 = StackNode("s2", "tabs", emptyList())

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1, stack2),
            activeStackIndex = 1
        )

        assertEquals(stack1, tabs.activeStack)
        assertEquals(3, tabs.tabCount)
    }

    @Test
    fun `TabNode stackAt returns correct stack`() {
        val stack0 = StackNode("s0", "tabs", emptyList())
        val stack1 = StackNode("s1", "tabs", emptyList())

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack0, stack1),
            activeStackIndex = 0
        )

        assertEquals(stack0, tabs.stackAt(0))
        assertEquals(stack1, tabs.stackAt(1))
    }

    @Test
    fun `TabNode stackAt throws for invalid index`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(StackNode("s0", "tabs", emptyList())),
            activeStackIndex = 0
        )

        assertFailsWith<IndexOutOfBoundsException> {
            tabs.stackAt(5)
        }
    }

    @Test
    fun `TabNode tabCount returns number of stacks`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("s0", "tabs", emptyList()),
                StackNode("s1", "tabs", emptyList()),
                StackNode("s2", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        assertEquals(3, tabs.tabCount)
    }

    @Test
    fun `TabNode activeStackIndex at boundary is valid`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("s0", "tabs", emptyList()),
                StackNode("s1", "tabs", emptyList())
            ),
            activeStackIndex = 1 // Last valid index
        )

        assertEquals(1, tabs.activeStackIndex)
        assertEquals(tabs.stacks[1], tabs.activeStack)
    }

    // =========================================================================
    // PANE NODE TESTS
    // =========================================================================

    @Test
    fun `PaneNode requires Primary pane`() {
        assertFailsWith<IllegalArgumentException> {
            PaneNode(
                key = "panes",
                parentKey = null,
                paneConfigurations = mapOf(
                    PaneRole.Supporting to PaneConfiguration(
                        ScreenNode("p1", "panes", HomeDestination)
                    )
                ),
                activePaneRole = PaneRole.Supporting
            )
        }
    }

    @Test
    fun `PaneNode validates activePaneRole exists in configurations`() {
        assertFailsWith<IllegalArgumentException> {
            PaneNode(
                key = "panes",
                parentKey = null,
                paneConfigurations = mapOf(
                    PaneRole.Primary to PaneConfiguration(
                        ScreenNode("p1", "panes", HomeDestination)
                    )
                ),
                activePaneRole = PaneRole.Supporting // Not in configurations
            )
        }
    }

    @Test
    fun `PaneNode activePane returns correct pane content`() {
        val primaryContent = ScreenNode("primary", "panes", ListDestination)
        val supportingContent = ScreenNode("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Supporting
        )

        assertEquals(supportingContent, panes.activePaneContent)
        assertEquals(2, panes.paneCount)
    }

    @Test
    fun `PaneNode paneCount returns number of configured panes`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("p1", "panes", HomeDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode("p2", "panes", DetailDestination)
                ),
                PaneRole.Extra to PaneConfiguration(
                    ScreenNode("p3", "panes", SettingsDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(3, panes.paneCount)
    }

    @Test
    fun `PaneNode paneContent returns content for given role`() {
        val primaryContent = ScreenNode("primary", "panes", ListDestination)
        val supportingContent = ScreenNode("supporting", "panes", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent),
                PaneRole.Supporting to PaneConfiguration(supportingContent)
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(primaryContent, panes.paneContent(PaneRole.Primary))
        assertEquals(supportingContent, panes.paneContent(PaneRole.Supporting))
        assertNull(panes.paneContent(PaneRole.Extra))
    }

    @Test
    fun `PaneNode adaptStrategy returns strategy for given role`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("p1", "panes", HomeDestination),
                    AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode("p2", "panes", DetailDestination),
                    AdaptStrategy.Levitate
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(AdaptStrategy.Hide, panes.adaptStrategy(PaneRole.Primary))
        assertEquals(AdaptStrategy.Levitate, panes.adaptStrategy(PaneRole.Supporting))
        assertNull(panes.adaptStrategy(PaneRole.Extra))
    }

    @Test
    fun `PaneNode configuredRoles returns all configured roles`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("p1", "panes", HomeDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode("p2", "panes", DetailDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(setOf(PaneRole.Primary, PaneRole.Supporting), panes.configuredRoles)
    }

    @Test
    fun `PaneNode with default backBehavior`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("p1", "panes", HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(PaneBackBehavior.PopUntilScaffoldValueChange, panes.backBehavior)
    }

    @Test
    fun `PaneNode with custom backBehavior`() {
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("p1", "panes", HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = PaneBackBehavior.PopLatest
        )

        assertEquals(PaneBackBehavior.PopLatest, panes.backBehavior)
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - findByKey
    // =========================================================================

    @Test
    fun `findByKey finds root node`() {
        val root = StackNode("root", null, emptyList())

        assertEquals(root, root.findByKey("root"))
    }

    @Test
    fun `findByKey finds nested screen in StackNode`() {
        val screen = ScreenNode("target", "stack", HomeDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("other", "root", ProfileDestination),
                screen
            )
        )

        assertEquals(screen, root.findByKey("target"))
    }

    @Test
    fun `findByKey returns null when not found`() {
        val root = StackNode("root", null, emptyList())

        assertNull(root.findByKey("nonexistent"))
    }

    @Test
    fun `findByKey finds node in TabNode`() {
        val targetScreen = ScreenNode("target", "tab1", HomeDestination)
        val root = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", emptyList()),
                StackNode("tab1", "tabs", listOf(targetScreen))
            ),
            activeStackIndex = 0
        )

        assertEquals(targetScreen, root.findByKey("target"))
    }

    @Test
    fun `findByKey finds node in inactive tab`() {
        val targetScreen = ScreenNode("target", "tab1", HomeDestination)
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s0", "tab0", ProfileDestination)
                    )
                ),
                StackNode("tab1", "tabs", listOf(targetScreen))
            ),
            activeStackIndex = 0 // tab0 is active, but we're looking in tab1
        )

        assertEquals(targetScreen, tabs.findByKey("target"))
    }

    @Test
    fun `findByKey finds node in PaneNode`() {
        val targetScreen = ScreenNode("target", "panes", HomeDestination)
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("other", "panes", ProfileDestination)
                ),
                PaneRole.Supporting to PaneConfiguration(targetScreen)
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(targetScreen, panes.findByKey("target"))
    }

    @Test
    fun `findByKey in deeply nested structure`() {
        val targetScreen = ScreenNode("deep-target", "inner-stack", HomeDestination)
        val root = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                TabNode(
                    key = "tabs",
                    parentKey = "root",
                    stacks = listOf(
                        StackNode(
                            "tab0", "tabs", listOf(
                                PaneNode(
                                    key = "panes",
                                    parentKey = "tab0",
                                    paneConfigurations = mapOf(
                                        PaneRole.Primary to PaneConfiguration(
                                            StackNode("inner-stack", "panes", listOf(targetScreen))
                                        )
                                    ),
                                    activePaneRole = PaneRole.Primary
                                )
                            )
                        )
                    ),
                    activeStackIndex = 0
                )
            )
        )

        assertEquals(targetScreen, root.findByKey("deep-target"))
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - activePathToLeaf
    // =========================================================================

    @Test
    fun `activePathToLeaf returns single element for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        val path = screen.activePathToLeaf()

        assertEquals(1, path.size)
        assertEquals(screen, path[0])
    }

    @Test
    fun `activePathToLeaf returns empty-ish path for empty StackNode`() {
        val stack = StackNode("stack", null, emptyList())

        val path = stack.activePathToLeaf()

        assertEquals(1, path.size)
        assertEquals(stack, path[0])
    }

    @Test
    fun `activePathToLeaf returns complete path through StackNode`() {
        val screen = ScreenNode("leaf", "stack", HomeDestination)
        val stack = StackNode("stack", null, listOf(screen))

        val path = stack.activePathToLeaf()

        assertEquals(2, path.size)
        assertEquals(stack, path[0])
        assertEquals(screen, path[1])
    }

    @Test
    fun `activePathToLeaf returns complete path through TabNode`() {
        val screen = ScreenNode("leaf", "stack", HomeDestination)
        val stack = StackNode("stack", "tabs", listOf(screen))
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(stack),
            activeStackIndex = 0
        )

        val path = tabs.activePathToLeaf()

        assertEquals(3, path.size)
        assertEquals(tabs, path[0])
        assertEquals(stack, path[1])
        assertEquals(screen, path[2])
    }

    @Test
    fun `activePathToLeaf returns complete path through PaneNode`() {
        val screen = ScreenNode("leaf", "panes", HomeDestination)
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(screen)
            ),
            activePaneRole = PaneRole.Primary
        )

        val path = panes.activePathToLeaf()

        assertEquals(2, path.size)
        assertEquals(panes, path[0])
        assertEquals(screen, path[1])
    }

    @Test
    fun `activePathToLeaf follows active tab only`() {
        val activeScreen = ScreenNode("active-screen", "tab0", HomeDestination)
        val inactiveScreen = ScreenNode("inactive-screen", "tab1", ProfileDestination)

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(activeScreen)),
                StackNode("tab1", "tabs", listOf(inactiveScreen))
            ),
            activeStackIndex = 0
        )

        val path = tabs.activePathToLeaf()

        assertEquals(3, path.size)
        assertTrue(path.contains(activeScreen))
        assertFalse(path.contains(inactiveScreen))
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - activeLeaf
    // =========================================================================

    @Test
    fun `activeLeaf returns ScreenNode itself`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertEquals(screen, screen.activeLeaf())
    }

    @Test
    fun `activeLeaf returns null when no screens in stack`() {
        val stack = StackNode("stack", null, emptyList())

        assertNull(stack.activeLeaf())
    }

    @Test
    fun `activeLeaf returns deepest ScreenNode in StackNode`() {
        val screen = ScreenNode("leaf", "stack", HomeDestination)
        val stack = StackNode("stack", null, listOf(screen))

        assertEquals(screen, stack.activeLeaf())
    }

    @Test
    fun `activeLeaf returns deepest active ScreenNode in TabNode`() {
        val activeScreen = ScreenNode("active", "tab0", HomeDestination)
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(activeScreen)),
                StackNode(
                    "tab1", "tabs", listOf(
                        ScreenNode("inactive", "tab1", ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        assertEquals(activeScreen, tabs.activeLeaf())
    }

    @Test
    fun `activeLeaf returns deepest active ScreenNode in PaneNode`() {
        val activeScreen = ScreenNode("active", "primary-stack", HomeDestination)
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary-stack", "panes", listOf(activeScreen))
                ),
                PaneRole.Supporting to PaneConfiguration(
                    ScreenNode("inactive", "panes", DetailDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(activeScreen, panes.activeLeaf())
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - activeStack
    // =========================================================================

    @Test
    fun `activeStack returns null for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertNull(screen.activeStack())
    }

    @Test
    fun `activeStack returns self for StackNode with no deeper stacks`() {
        val stack = StackNode(
            key = "stack",
            parentKey = null,
            children = listOf(ScreenNode("s1", "stack", HomeDestination))
        )

        assertEquals(stack, stack.activeStack())
    }

    @Test
    fun `activeStack returns deepest active StackNode`() {
        val innerStack = StackNode(
            key = "inner",
            parentKey = "outer",
            children = listOf(ScreenNode("s", "inner", HomeDestination))
        )
        val outerStack = StackNode(
            key = "outer",
            parentKey = null,
            children = listOf(innerStack)
        )

        assertEquals(innerStack, outerStack.activeStack())
    }

    @Test
    fun `activeStack returns deepest stack in TabNode`() {
        val deepStack = StackNode(
            "deep", "tab0", listOf(
                ScreenNode("s", "deep", HomeDestination)
            )
        )
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(deepStack)),
                StackNode("tab1", "tabs", emptyList())
            ),
            activeStackIndex = 0
        )

        assertEquals(deepStack, tabs.activeStack())
    }

    @Test
    fun `activeStack returns deepest stack in PaneNode`() {
        val deepStack = StackNode(
            "deep", "primary", listOf(
                ScreenNode("s", "deep", HomeDestination)
            )
        )
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode("primary", "panes", listOf(deepStack))
                )
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(deepStack, panes.activeStack())
    }

    @Test
    fun `activeStack returns TabNode activeStack when it has no deeper stacks`() {
        val tabStack = StackNode(
            "tab0", "tabs", listOf(
                ScreenNode("s", "tab0", HomeDestination)
            )
        )
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(tabStack),
            activeStackIndex = 0
        )

        assertEquals(tabStack, tabs.activeStack())
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allScreens
    // =========================================================================

    @Test
    fun `allScreens returns single screen for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        val screens = screen.allScreens()

        assertEquals(1, screens.size)
        assertTrue(screens.contains(screen))
    }

    @Test
    fun `allScreens returns empty list for empty StackNode`() {
        val stack = StackNode("stack", null, emptyList())

        val screens = stack.allScreens()

        assertTrue(screens.isEmpty())
    }

    @Test
    fun `allScreens returns all screens in StackNode`() {
        val screen1 = ScreenNode("s1", "stack", HomeDestination)
        val screen2 = ScreenNode("s2", "stack", ProfileDestination)
        val stack = StackNode("stack", null, listOf(screen1, screen2))

        val screens = stack.allScreens()

        assertEquals(2, screens.size)
        assertTrue(screens.contains(screen1))
        assertTrue(screens.contains(screen2))
    }

    @Test
    fun `allScreens returns all screens from all tabs in TabNode`() {
        val screen1 = ScreenNode("s1", "tab0", HomeDestination)
        val screen2 = ScreenNode("s2", "tab0", ProfileDestination)
        val screen3 = ScreenNode("s3", "tab1", SettingsDestination)

        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "tabs", listOf(screen1, screen2)),
                StackNode("tab1", "tabs", listOf(screen3))
            ),
            activeStackIndex = 0
        )

        val allScreens = tabs.allScreens()

        assertEquals(3, allScreens.size)
        assertTrue(allScreens.contains(screen1))
        assertTrue(allScreens.contains(screen2))
        assertTrue(allScreens.contains(screen3))
    }

    @Test
    fun `allScreens returns all screens from all panes in PaneNode`() {
        val screen1 = ScreenNode("s1", "primary", HomeDestination)
        val screen2 = ScreenNode("s2", "supporting", DetailDestination)

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(screen1),
                PaneRole.Supporting to PaneConfiguration(screen2)
            ),
            activePaneRole = PaneRole.Primary
        )

        val allScreens = panes.allScreens()

        assertEquals(2, allScreens.size)
        assertTrue(allScreens.contains(screen1))
        assertTrue(allScreens.contains(screen2))
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - paneForRole
    // =========================================================================

    @Test
    fun `paneForRole returns null for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertNull(screen.paneForRole(PaneRole.Primary))
    }

    @Test
    fun `paneForRole returns content for matching role in PaneNode`() {
        val primaryContent = ScreenNode("primary", "panes", HomeDestination)
        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent)
            ),
            activePaneRole = PaneRole.Primary
        )

        assertEquals(primaryContent, panes.paneForRole(PaneRole.Primary))
    }

    @Test
    fun `paneForRole searches recursively through StackNode`() {
        val primaryContent = ScreenNode("primary", "panes", HomeDestination)
        val panes = PaneNode(
            key = "panes",
            parentKey = "stack",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(primaryContent)
            ),
            activePaneRole = PaneRole.Primary
        )
        val stack = StackNode("stack", null, listOf(panes))

        assertEquals(primaryContent, stack.paneForRole(PaneRole.Primary))
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allPaneNodes
    // =========================================================================

    @Test
    fun `allPaneNodes returns empty for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertTrue(screen.allPaneNodes().isEmpty())
    }

    @Test
    fun `allPaneNodes returns all PaneNodes in tree`() {
        val innerPane = PaneNode(
            key = "inner-pane",
            parentKey = "outer-pane",
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    ScreenNode("s1", "inner-pane", HomeDestination)
                )
            ),
            activePaneRole = PaneRole.Primary
        )
        val outerPane = PaneNode(
            key = "outer-pane",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(innerPane)
            ),
            activePaneRole = PaneRole.Primary
        )

        val allPanes = outerPane.allPaneNodes()

        assertEquals(2, allPanes.size)
        assertTrue(allPanes.contains(outerPane))
        assertTrue(allPanes.contains(innerPane))
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allTabNodes
    // =========================================================================

    @Test
    fun `allTabNodes returns empty for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertTrue(screen.allTabNodes().isEmpty())
    }

    @Test
    fun `allTabNodes returns all TabNodes in tree`() {
        val innerTabs = TabNode(
            key = "inner-tabs",
            parentKey = "tab0",
            stacks = listOf(StackNode("inner-tab0", "inner-tabs", emptyList())),
            activeStackIndex = 0
        )
        val outerTabs = TabNode(
            key = "outer-tabs",
            parentKey = null,
            stacks = listOf(
                StackNode("tab0", "outer-tabs", listOf(innerTabs))
            ),
            activeStackIndex = 0
        )

        val allTabs = outerTabs.allTabNodes()

        assertEquals(2, allTabs.size)
        assertTrue(allTabs.contains(outerTabs))
        assertTrue(allTabs.contains(innerTabs))
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - allStackNodes
    // =========================================================================

    @Test
    fun `allStackNodes returns empty for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertTrue(screen.allStackNodes().isEmpty())
    }

    @Test
    fun `allStackNodes returns all StackNodes in tree`() {
        val stack1 = StackNode("stack1", "tabs", emptyList())
        val stack2 = StackNode("stack2", "tabs", emptyList())
        val tabs = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(stack1, stack2),
            activeStackIndex = 0
        )
        val root = StackNode("root", null, listOf(tabs))

        val allStacks = root.allStackNodes()

        assertEquals(3, allStacks.size)
        assertTrue(allStacks.contains(root))
        assertTrue(allStacks.contains(stack1))
        assertTrue(allStacks.contains(stack2))
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - depth
    // =========================================================================

    @Test
    fun `depth returns 0 for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertEquals(0, screen.depth())
    }

    @Test
    fun `depth returns 0 for empty StackNode`() {
        val stack = StackNode("stack", null, emptyList())

        assertEquals(0, stack.depth())
    }

    @Test
    fun `depth returns correct value for nested structure`() {
        val screen = ScreenNode("screen", "stack", HomeDestination)
        val stack = StackNode("stack", null, listOf(screen))

        assertEquals(1, stack.depth())
    }

    @Test
    fun `depth calculates max depth in TabNode`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s0", "tab0", HomeDestination)
                    )
                ),
                StackNode(
                    "tab1", "tabs", listOf(
                        ScreenNode("s1", "tab1", ProfileDestination),
                        ScreenNode("s2", "tab1", SettingsDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        assertEquals(2, tabs.depth()) // tabs -> stack -> screen
    }

    // =========================================================================
    // EXTENSION FUNCTION TESTS - nodeCount
    // =========================================================================

    @Test
    fun `nodeCount returns 1 for ScreenNode`() {
        val screen = ScreenNode("screen", null, HomeDestination)

        assertEquals(1, screen.nodeCount())
    }

    @Test
    fun `nodeCount returns 1 for empty StackNode`() {
        val stack = StackNode("stack", null, emptyList())

        assertEquals(1, stack.nodeCount())
    }

    @Test
    fun `nodeCount returns correct total for nested structure`() {
        val tabs = TabNode(
            key = "tabs",
            parentKey = null,
            stacks = listOf(
                StackNode(
                    "tab0", "tabs", listOf(
                        ScreenNode("s0", "tab0", HomeDestination)
                    )
                ),
                StackNode(
                    "tab1", "tabs", listOf(
                        ScreenNode("s1", "tab1", ProfileDestination)
                    )
                )
            ),
            activeStackIndex = 0
        )

        // 1 (tabs) + 2 (stacks) + 2 (screens) = 5
        assertEquals(5, tabs.nodeCount())
    }

    // =========================================================================
    // NAV KEY GENERATOR TESTS
    // =========================================================================

    @Test
    fun `NavKeyGenerator generates unique keys`() {
        NavKeyGenerator.reset()

        val key1 = NavKeyGenerator.generate()
        val key2 = NavKeyGenerator.generate()
        val key3 = NavKeyGenerator.generate()

        assertFalse(key1 == key2)
        assertFalse(key2 == key3)
        assertFalse(key1 == key3)
    }

    @Test
    fun `NavKeyGenerator includes debug label when provided`() {
        NavKeyGenerator.reset()

        val key = NavKeyGenerator.generate("home")

        assertTrue(key.startsWith("home-"))
    }

    @Test
    fun `NavKeyGenerator uses default prefix when no label`() {
        NavKeyGenerator.reset()

        val key = NavKeyGenerator.generate()

        assertTrue(key.startsWith("node-"))
    }

    @Test
    fun `NavKeyGenerator reset restarts counter`() {
        NavKeyGenerator.reset()
        val key1 = NavKeyGenerator.generate()

        NavKeyGenerator.reset()
        val key2 = NavKeyGenerator.generate()

        assertEquals(key1, key2)
    }

    // =========================================================================
    // COMPLEX INTEGRATION TESTS
    // =========================================================================

    @Test
    fun `complex tree navigation scenario`() {
        // Build a complex tree: root stack -> tabs -> nested stacks with screens
        val homeScreen = ScreenNode("home-screen", "home-stack", HomeDestination)
        val profileScreen1 = ScreenNode("profile-screen-1", "profile-stack", ProfileDestination)
        val profileScreen2 = ScreenNode("profile-screen-2", "profile-stack", DetailDestination)

        val homeStack = StackNode("home-stack", "tabs", listOf(homeScreen))
        val profileStack =
            StackNode("profile-stack", "tabs", listOf(profileScreen1, profileScreen2))

        val tabs = TabNode(
            key = "tabs",
            parentKey = "root",
            stacks = listOf(homeStack, profileStack),
            activeStackIndex = 1 // Profile tab is active
        )

        val rootStack = StackNode("root", null, listOf(tabs))

        // Verify activeLeaf
        assertEquals(profileScreen2, rootStack.activeLeaf())

        // Verify activeStack
        assertEquals(profileStack, rootStack.activeStack())

        // Verify activePathToLeaf
        val path = rootStack.activePathToLeaf()
        assertEquals(4, path.size)
        assertEquals(rootStack, path[0])
        assertEquals(tabs, path[1])
        assertEquals(profileStack, path[2])
        assertEquals(profileScreen2, path[3])

        // Verify allScreens
        val allScreens = rootStack.allScreens()
        assertEquals(3, allScreens.size)

        // Verify findByKey works across the tree
        assertEquals(homeScreen, rootStack.findByKey("home-screen"))
        assertEquals(profileScreen1, rootStack.findByKey("profile-screen-1"))
        assertEquals(tabs, rootStack.findByKey("tabs"))
    }

    @Test
    fun `pane-based adaptive layout scenario`() {
        // Build a list-detail pane layout
        val listScreen = ScreenNode("list", "list-stack", ListDestination)
        val detailScreen = ScreenNode("detail", "detail-stack", DetailDestination)

        val listStack = StackNode("list-stack", "panes", listOf(listScreen))
        val detailStack = StackNode("detail-stack", "panes", listOf(detailScreen))

        val panes = PaneNode(
            key = "panes",
            parentKey = null,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    content = listStack,
                    adaptStrategy = AdaptStrategy.Hide
                ),
                PaneRole.Supporting to PaneConfiguration(
                    content = detailStack,
                    adaptStrategy = AdaptStrategy.Levitate
                )
            ),
            activePaneRole = PaneRole.Supporting,
            backBehavior = PaneBackBehavior.PopUntilCurrentDestinationChange
        )

        // Verify activeLeaf follows activePaneRole
        assertEquals(detailScreen, panes.activeLeaf())

        // Verify activeStack follows activePaneRole
        assertEquals(detailStack, panes.activeStack())

        // Verify paneContent
        assertEquals(listStack, panes.paneContent(PaneRole.Primary))
        assertEquals(detailStack, panes.paneContent(PaneRole.Supporting))

        // Verify adaptStrategies
        assertEquals(AdaptStrategy.Hide, panes.adaptStrategy(PaneRole.Primary))
        assertEquals(AdaptStrategy.Levitate, panes.adaptStrategy(PaneRole.Supporting))

        // Verify allScreens includes both panes
        val allScreens = panes.allScreens()
        assertEquals(2, allScreens.size)
        assertTrue(allScreens.contains(listScreen))
        assertTrue(allScreens.contains(detailScreen))
    }
}
