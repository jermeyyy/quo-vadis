package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.isPane
import com.jermey.quo.vadis.core.navigation.node.isScreen
import com.jermey.quo.vadis.core.navigation.node.isStack
import com.jermey.quo.vadis.core.navigation.node.isTab
import com.jermey.quo.vadis.core.navigation.node.requirePane
import com.jermey.quo.vadis.core.navigation.node.requireScreen
import com.jermey.quo.vadis.core.navigation.node.requireStack
import com.jermey.quo.vadis.core.navigation.node.requireTab
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for NavNode type-checking and type-requiring extension functions
 * defined in NavNodeTypeChecks.kt.
 */
@OptIn(InternalQuoVadisApi::class)
class NavNodeTypeChecksTest {

    private object TestDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private val screenNode: NavNode = ScreenNode(
        key = NodeKey("screen-1"),
        parentKey = null,
        destination = TestDestination,
    )

    private val stackNode: NavNode = StackNode(
        key = NodeKey("stack-1"),
        parentKey = null,
        children = listOf(
            ScreenNode(NodeKey("s1"), NodeKey("stack-1"), TestDestination),
        ),
    )

    private val tabNode: NavNode = TabNode(
        key = NodeKey("tab-1"),
        parentKey = null,
        stacks = listOf(
            StackNode(
                key = NodeKey("tab-stack-1"),
                parentKey = NodeKey("tab-1"),
                children = listOf(
                    ScreenNode(NodeKey("ts1"), NodeKey("tab-stack-1"), TestDestination),
                ),
            ),
        ),
        activeStackIndex = 0,
    )

    private val paneNode: NavNode = PaneNode(
        key = NodeKey("pane-1"),
        parentKey = null,
        paneConfigurations = mapOf(
            PaneRole.Primary to PaneConfiguration(
                content = StackNode(
                    key = NodeKey("pane-stack-1"),
                    parentKey = NodeKey("pane-1"),
                    children = listOf(
                        ScreenNode(NodeKey("ps1"), NodeKey("pane-stack-1"), TestDestination),
                    ),
                ),
            ),
        ),
        activePaneRole = PaneRole.Primary,
        backBehavior = PaneBackBehavior.PopLatest,
    )

    // =========================================================================
    // isScreen
    // =========================================================================

    @Test
    fun `isScreen returns true for ScreenNode`() {
        assertTrue(screenNode.isScreen())
    }

    @Test
    fun `isScreen returns false for non-ScreenNode`() {
        assertFalse(stackNode.isScreen())
        assertFalse(tabNode.isScreen())
        assertFalse(paneNode.isScreen())
    }

    // =========================================================================
    // isStack
    // =========================================================================

    @Test
    fun `isStack returns true for StackNode`() {
        assertTrue(stackNode.isStack())
    }

    @Test
    fun `isStack returns false for non-StackNode`() {
        assertFalse(screenNode.isStack())
        assertFalse(tabNode.isStack())
        assertFalse(paneNode.isStack())
    }

    // =========================================================================
    // isTab
    // =========================================================================

    @Test
    fun `isTab returns true for TabNode`() {
        assertTrue(tabNode.isTab())
    }

    @Test
    fun `isTab returns false for non-TabNode`() {
        assertFalse(screenNode.isTab())
        assertFalse(stackNode.isTab())
        assertFalse(paneNode.isTab())
    }

    // =========================================================================
    // isPane
    // =========================================================================

    @Test
    fun `isPane returns true for PaneNode`() {
        assertTrue(paneNode.isPane())
    }

    @Test
    fun `isPane returns false for non-PaneNode`() {
        assertFalse(screenNode.isPane())
        assertFalse(stackNode.isPane())
        assertFalse(tabNode.isPane())
    }

    // =========================================================================
    // requireScreen
    // =========================================================================

    @Test
    fun `requireScreen returns ScreenNode for ScreenNode`() {
        val result = screenNode.requireScreen()
        assertEquals(NodeKey("screen-1"), result.key)
    }

    @Test
    fun `requireScreen throws for non-ScreenNode`() {
        assertFailsWith<IllegalStateException> { stackNode.requireScreen() }
        assertFailsWith<IllegalStateException> { tabNode.requireScreen() }
        assertFailsWith<IllegalStateException> { paneNode.requireScreen() }
    }

    // =========================================================================
    // requireStack
    // =========================================================================

    @Test
    fun `requireStack returns StackNode for StackNode`() {
        val result = stackNode.requireStack()
        assertEquals(NodeKey("stack-1"), result.key)
    }

    @Test
    fun `requireStack throws for non-StackNode`() {
        assertFailsWith<IllegalStateException> { screenNode.requireStack() }
        assertFailsWith<IllegalStateException> { tabNode.requireStack() }
        assertFailsWith<IllegalStateException> { paneNode.requireStack() }
    }

    // =========================================================================
    // requireTab
    // =========================================================================

    @Test
    fun `requireTab returns TabNode for TabNode`() {
        val result = tabNode.requireTab()
        assertEquals(NodeKey("tab-1"), result.key)
    }

    @Test
    fun `requireTab throws for non-TabNode`() {
        assertFailsWith<IllegalStateException> { screenNode.requireTab() }
        assertFailsWith<IllegalStateException> { stackNode.requireTab() }
        assertFailsWith<IllegalStateException> { paneNode.requireTab() }
    }

    // =========================================================================
    // requirePane
    // =========================================================================

    @Test
    fun `requirePane returns PaneNode for PaneNode`() {
        val result = paneNode.requirePane()
        assertEquals(NodeKey("pane-1"), result.key)
    }

    @Test
    fun `requirePane throws for non-PaneNode`() {
        assertFailsWith<IllegalStateException> { screenNode.requirePane() }
        assertFailsWith<IllegalStateException> { stackNode.requirePane() }
        assertFailsWith<IllegalStateException> { tabNode.requirePane() }
    }
}
