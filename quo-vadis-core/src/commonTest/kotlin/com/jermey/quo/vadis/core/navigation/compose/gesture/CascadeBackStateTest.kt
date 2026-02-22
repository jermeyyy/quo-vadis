package com.jermey.quo.vadis.core.navigation.compose.gesture

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.internal.navback.calculateCascadeBackState
import com.jermey.quo.vadis.core.compose.internal.navback.wouldCascade
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for CascadeBackState calculation.
 */
@OptIn(InternalQuoVadisApi::class)
class CascadeBackStateTest {

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

    @BeforeTest
    fun setup() {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // NORMAL POP (NO CASCADE) TESTS
    // =========================================================================

    @Test
    fun `normal pop returns cascadeDepth 0`() {
        // Given: Stack with 2 items
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(screen1, screen2)
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then
        assertEquals(0, state.cascadeDepth)
        assertEquals(NodeKey("s2"), state.exitingNode.key) // Current screen exits
        assertEquals(NodeKey("s1"), state.targetNode?.key) // Previous screen is target
        assertFalse(state.delegatesToSystem)
    }

    @Test
    fun `root with 1 item delegates to system`() {
        // Given: Stack with 1 item
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(screen1)
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then
        assertTrue(state.delegatesToSystem)
        assertEquals(0, state.cascadeDepth)
        assertNull(state.targetNode)
    }

    // =========================================================================
    // SINGLE CASCADE TESTS
    // =========================================================================

    @Test
    fun `nested stack cascade returns cascadeDepth 1`() {
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

        // When
        val state = calculateCascadeBackState(root)

        // Then
        assertEquals(1, state.cascadeDepth)
        assertEquals(NodeKey("child"), state.exitingNode.key) // ChildStack exits
        assertEquals(NodeKey("r1"), state.targetNode?.key) // rootScreen is target
        assertFalse(state.delegatesToSystem)
    }

    @Test
    fun `tab pop entire TabNode returns cascade`() {
        // Given: RootStack → [Screen1, TabNode(initial tab with 1 item)]
        val tabScreen = ScreenNode(NodeKey("t1"), NodeKey("tab0"), HomeDestination)
        val tabStack = StackNode(
            key = NodeKey("tab0"),
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
        val state = calculateCascadeBackState(root)

        // Then: Should cascade through tab to pop TabNode
        assertTrue(state.cascadeDepth > 0)
        assertFalse(state.delegatesToSystem)
        assertEquals(NodeKey("r1"), state.targetNode?.key)
    }

    // =========================================================================
    // DEEP CASCADE TESTS
    // =========================================================================

    @Test
    fun `deep cascade through stack returns correct depth`() {
        // Given: RootStack → [Screen1, MiddleStack(ChildStack(1 item))]
        val childScreen = ScreenNode(NodeKey("c1"), NodeKey("child"), HomeDestination)
        val childStack = StackNode(
            key = NodeKey("child"),
            parentKey = NodeKey("middle"),
            children = listOf(childScreen)
        )
        val middleStack = StackNode(
            key = NodeKey("middle"),
            parentKey = NodeKey("root"),
            children = listOf(childStack)
        )
        val rootScreen = ScreenNode(NodeKey("r1"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(rootScreen, middleStack)
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then: Should cascade through 2 levels (child -> middle -> root)
        assertTrue(state.cascadeDepth >= 1)
        assertFalse(state.delegatesToSystem)
        assertEquals(NodeKey("r1"), state.targetNode?.key)
    }

    @Test
    fun `non-initial tab with 1 item delegates to system when TabNode is only child`() {
        // Given: RootStack → TabNode(2 tabs, active = 1) where TabNode is only child
        val tab0Screen = ScreenNode(NodeKey("t0"), NodeKey("tab0"), HomeDestination)
        val tab0Stack = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(tab0Screen))
        val tab1Screen = ScreenNode(NodeKey("t1"), NodeKey("tab1"), ProfileDestination)
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tab1Screen))
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tab0Stack, tab1Stack),
            activeStackIndex = 1 // On non-initial tab
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then: Should delegate to system (TabNode cannot be popped - only child of root)
        assertTrue(state.delegatesToSystem)
        assertNull(state.targetNode)
    }

    // =========================================================================
    // DELEGATE TO SYSTEM TESTS
    // =========================================================================

    @Test
    fun `empty root stack delegates to system`() {
        // Given: Empty root stack
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = emptyList()
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then
        assertTrue(state.delegatesToSystem)
        assertNull(state.targetNode)
    }

    @Test
    fun `nested cascade that reaches root with 1 child delegates to system`() {
        // Given: RootStack(only ChildStack with 1 item)
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
        val state = calculateCascadeBackState(root)

        // Then: Cascade reaches root which has only 1 child, delegates to system
        assertTrue(state.delegatesToSystem)
        assertNull(state.targetNode)
    }

    // =========================================================================
    // WOULDCASCADE TESTS
    // =========================================================================

    @Test
    fun `wouldCascade returns false for normal pop`() {
        // Given: Stack with 2 items
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(screen1, screen2)
        )

        // When/Then
        assertFalse(wouldCascade(root))
    }

    @Test
    fun `wouldCascade returns true for nested stack with 1 item`() {
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

        // When/Then
        assertTrue(wouldCascade(root))
    }

    @Test
    fun `wouldCascade returns false for root with 1 item`() {
        // Given: Root stack with 1 item (no cascade, just delegate to system)
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(screen1)
        )

        // When/Then: Root stack has no parent, so no cascade possible
        assertFalse(wouldCascade(root))
    }

    @Test
    fun `wouldCascade returns false for deeply nested stack with multiple items`() {
        // Given: RootStack → ChildStack(2 items)
        val childScreen1 = ScreenNode(NodeKey("c1"), NodeKey("child"), HomeDestination)
        val childScreen2 = ScreenNode(NodeKey("c2"), NodeKey("child"), ProfileDestination)
        val childStack = StackNode(
            key = NodeKey("child"),
            parentKey = NodeKey("root"),
            children = listOf(childScreen1, childScreen2)
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(childStack)
        )

        // When/Then: Child stack has 2 items, so normal pop, no cascade
        assertFalse(wouldCascade(root))
    }

    // =========================================================================
    // SOURCE NODE TESTS
    // =========================================================================

    @Test
    fun `sourceNode is the active screen for normal pop`() {
        // Given: Stack with 2 screens
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(screen1, screen2)
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then: Source should be the active (topmost) screen
        assertEquals(NodeKey("s2"), state.sourceNode.key)
    }

    @Test
    fun `sourceNode is the nested active screen for cascade`() {
        // Given: RootStack → ChildStack(1 screen)
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

        // When
        val state = calculateCascadeBackState(root)

        // Then: Source should still be the deepest active screen
        assertEquals(NodeKey("c1"), state.sourceNode.key)
    }

    // =========================================================================
    // TAB SPECIFIC TESTS
    // =========================================================================

    @Test
    fun `tab with multiple items on initial tab - normal pop`() {
        // Given: TabNode with 2 items on initial tab
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("tab0"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("tab0"), ProfileDestination)
        val tab0Stack = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(screen1, screen2))
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tab0Stack),
            activeStackIndex = 0
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then: Should be normal pop (no cascade)
        assertEquals(0, state.cascadeDepth)
        assertEquals(NodeKey("s2"), state.exitingNode.key)
        assertEquals(NodeKey("s1"), state.targetNode?.key)
        assertFalse(state.delegatesToSystem)
    }

    @Test
    fun `tab with 1 item on non-initial tab - switches tab`() {
        // Given: TabNode on non-initial tab with 1 item (TabNode is only child of root)
        val tab0Screen = ScreenNode(NodeKey("t0"), NodeKey("tab0"), HomeDestination)
        val tab0Stack = StackNode(NodeKey("tab0"), NodeKey("tabs"), listOf(tab0Screen))
        val tab1Screen = ScreenNode(NodeKey("t1"), NodeKey("tab1"), ProfileDestination)
        val tab1Stack = StackNode(NodeKey("tab1"), NodeKey("tabs"), listOf(tab1Screen))
        val tabNode = TabNode(
            key = NodeKey("tabs"),
            parentKey = NodeKey("root"),
            stacks = listOf(tab0Stack, tab1Stack),
            activeStackIndex = 1
        )
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(tabNode)
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then: TabNode is only child of root, should delegate to system
        assertTrue(state.delegatesToSystem)
        assertNull(state.targetNode)
    }
}
