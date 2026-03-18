package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.internal.tree.result.BackResult
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for pane back navigation scenarios.
 * 
 * Specifically tests:
 * 1. PaneNode with siblings in parent stack → back removes PaneNode
 * 2. canHandleBackNavigation returns true for the above
 * 3. Different PaneBackBehavior values
 * 4. Compact vs expanded mode behavior
 */
@OptIn(InternalQuoVadisApi::class)
class PaneBackNavigationTest {

    private object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    private object ListDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "list"
    }

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
        override fun toString(): String = "detail"
    }

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // TREE BUILDERS
    // =========================================================================

    /**
     * Creates: RootStack → [HomeScreen, PaneNode(Primary:[ListScreen], Supporting:[DetailScreen])]
     * PaneNode has PopUntilScaffoldValueChange behavior, active on Primary
     */
    private fun createTreeWithPaneNodePushed(
        backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
    ): StackNode {
        val rootKey = NodeKey("root-stack")
        val paneKey = NodeKey("pane-node")

        val paneNode = PaneNode(
            key = paneKey,
            parentKey = rootKey,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        NodeKey("primary-stack"), paneKey, listOf(
                            ScreenNode(NodeKey("list-screen"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        NodeKey("supporting-stack"), paneKey, listOf(
                            ScreenNode(NodeKey("detail-screen"), NodeKey("supporting-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = backBehavior
        )

        return StackNode(
            key = rootKey,
            parentKey = null,
            children = listOf(
                ScreenNode(NodeKey("home-screen"), rootKey, HomeDestination),
                paneNode
            )
        )
    }

    /**
     * Creates: RootStack → [PaneNode(Primary:[ListScreen], Supporting:[DetailScreen])]
     * PaneNode is sole child (root's start destination)
     */
    private fun createTreeWithPaneNodeAsRoot(
        backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
    ): StackNode {
        val rootKey = NodeKey("root-stack")
        val paneKey = NodeKey("pane-node")

        val paneNode = PaneNode(
            key = paneKey,
            parentKey = rootKey,
            paneConfigurations = mapOf(
                PaneRole.Primary to PaneConfiguration(
                    StackNode(
                        NodeKey("primary-stack"), paneKey, listOf(
                            ScreenNode(NodeKey("list-screen"), NodeKey("primary-stack"), ListDestination)
                        )
                    )
                ),
                PaneRole.Supporting to PaneConfiguration(
                    StackNode(
                        NodeKey("supporting-stack"), paneKey, listOf(
                            ScreenNode(NodeKey("detail-screen"), NodeKey("supporting-stack"), DetailDestination)
                        )
                    )
                )
            ),
            activePaneRole = PaneRole.Primary,
            backBehavior = backBehavior
        )

        return StackNode(
            key = rootKey,
            parentKey = null,
            children = listOf(paneNode)
        )
    }

    // =========================================================================
    // canHandleBackNavigation TESTS
    // =========================================================================

    @Test
    fun `canHandleBackNavigation returns true when PaneNode has siblings in parent stack`() {
        val tree = createTreeWithPaneNodePushed()
        assertTrue(
            TreeMutator.canHandleBackNavigation(tree),
            "canHandleBackNavigation should return true when PaneNode has siblings"
        )
    }

    @Test
    fun `canHandleBackNavigation returns false when PaneNode is sole root child`() {
        val tree = createTreeWithPaneNodeAsRoot()
        // PaneNode is the only child of the root stack, no cascading possible
        assertEquals(
            false,
            TreeMutator.canHandleBackNavigation(tree),
            "canHandleBackNavigation should return false when PaneNode is sole root child"
        )
    }

    // =========================================================================
    // popWithTabBehavior TESTS - PopUntilScaffoldValueChange (default)
    // =========================================================================

    @Test
    fun `back on PaneNode with siblings - compact - PopUntilScaffoldValueChange removes PaneNode`() {
        val tree = createTreeWithPaneNodePushed(PaneBackBehavior.PopUntilScaffoldValueChange)

        val result = TreeMutator.popWithTabBehavior(tree, isCompact = true)

        assertIs<BackResult.Handled>(result, "Back should be handled")
        val newTree = result.newState as StackNode
        assertEquals(1, newTree.children.size, "PaneNode should be removed, leaving only HomeScreen")
        assertIs<ScreenNode>(newTree.children.first())
        assertEquals(HomeDestination, (newTree.children.first() as ScreenNode).destination)
    }

    @Test
    fun `back on PaneNode with siblings - expanded - PopUntilScaffoldValueChange removes PaneNode`() {
        val tree = createTreeWithPaneNodePushed(PaneBackBehavior.PopUntilScaffoldValueChange)

        val result = TreeMutator.popWithTabBehavior(tree, isCompact = false)

        assertIs<BackResult.Handled>(result, "Back should be handled in expanded mode")
        val newTree = result.newState as StackNode
        assertEquals(1, newTree.children.size, "PaneNode should be removed")
    }

    @Test
    fun `back on sole PaneNode - compact - PopUntilScaffoldValueChange delegates to system`() {
        val tree = createTreeWithPaneNodeAsRoot(PaneBackBehavior.PopUntilScaffoldValueChange)

        val result = TreeMutator.popWithTabBehavior(tree, isCompact = true)

        assertIs<BackResult.DelegateToSystem>(result, "Should delegate to system when PaneNode is root")
    }

    // =========================================================================
    // popWithTabBehavior TESTS - PopLatest
    // =========================================================================

    @Test
    fun `back on PaneNode with siblings - compact - PopLatest removes PaneNode`() {
        val tree = createTreeWithPaneNodePushed(PaneBackBehavior.PopLatest)

        val result = TreeMutator.popWithTabBehavior(tree, isCompact = true)

        assertIs<BackResult.Handled>(result, "Back should be handled for PopLatest")
        val newTree = result.newState as StackNode
        assertEquals(1, newTree.children.size, "PaneNode should be removed")
    }

    // =========================================================================
    // popWithTabBehavior TESTS - PopUntilContentChange
    // =========================================================================

    @Test
    fun `back on PaneNode with siblings - compact - PopUntilContentChange removes PaneNode`() {
        val tree = createTreeWithPaneNodePushed(PaneBackBehavior.PopUntilContentChange)

        val result = TreeMutator.popWithTabBehavior(tree, isCompact = true)

        assertIs<BackResult.Handled>(result, "Back should be handled for PopUntilContentChange")
        val newTree = result.newState as StackNode
        assertEquals(1, newTree.children.size, "PaneNode should be removed")
    }

    // =========================================================================
    // REGRESSION: Active pane at start dest → PaneNode removal
    // =========================================================================

    @Test
    fun `back removes PaneNode when active pane has only start destination`() {
        // This is the EXACT scenario from the bug report:
        // PaneNode pushed on stack, Primary active with 1 child, back should remove PaneNode
        val tree = createTreeWithPaneNodePushed()

        // Verify preconditions
        assertNotNull(tree.children.find { it is PaneNode }, "PaneNode should be in tree")
        assertEquals(2, tree.children.size, "Should have HomeScreen + PaneNode")

        // Execute back
        val result = TreeMutator.popWithTabBehavior(tree, isCompact = true)

        // Verify PaneNode was removed
        assertIs<BackResult.Handled>(result, "Back MUST be handled - PaneNode should be removed")
        val newTree = result.newState as StackNode
        assertEquals(1, newTree.children.size, "Only HomeScreen should remain")
        assertTrue(
            newTree.children.none { it is PaneNode },
            "PaneNode must be gone from the tree"
        )
    }
}
