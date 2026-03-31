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
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull

/**
 * Unit tests for CascadeBackState calculation.
 */
@OptIn(InternalQuoVadisApi::class)
class CascadeBackStateTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val HomeDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "home"
    }

    val ProfileDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
        override fun toString(): String = "profile"
    }

    beforeTest {
        NavKeyGenerator.reset()
    }

    // =========================================================================
    // NORMAL POP (NO CASCADE) TESTS
    // =========================================================================

    test("normal pop returns cascadeDepth 0") {
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
        state.cascadeDepth shouldBe 0
        state.exitingNode.key shouldBe NodeKey("s2") // Current screen exits
        state.targetNode?.key shouldBe NodeKey("s1") // Previous screen is target
        state.delegatesToSystem.shouldBeFalse()
    }

    test("root with 1 item delegates to system") {
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
        state.delegatesToSystem.shouldBeTrue()
        state.cascadeDepth shouldBe 0
        state.targetNode.shouldBeNull()
    }

    // =========================================================================
    // SINGLE CASCADE TESTS
    // =========================================================================

    test("nested stack cascade returns cascadeDepth 1") {
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
        state.cascadeDepth shouldBe 1
        state.exitingNode.key shouldBe NodeKey("child") // ChildStack exits
        state.targetNode?.key shouldBe NodeKey("r1") // rootScreen is target
        state.delegatesToSystem.shouldBeFalse()
    }

    test("tab pop entire TabNode returns cascade") {
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
        (state.cascadeDepth > 0).shouldBeTrue()
        state.delegatesToSystem.shouldBeFalse()
        state.targetNode?.key shouldBe NodeKey("r1")
    }

    // =========================================================================
    // DEEP CASCADE TESTS
    // =========================================================================

    test("deep cascade through stack returns correct depth") {
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
        (state.cascadeDepth >= 1).shouldBeTrue()
        state.delegatesToSystem.shouldBeFalse()
        state.targetNode?.key shouldBe NodeKey("r1")
    }

    test("non-initial tab with 1 item delegates to system when TabNode is only child") {
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
        state.delegatesToSystem.shouldBeTrue()
        state.targetNode.shouldBeNull()
    }

    // =========================================================================
    // DELEGATE TO SYSTEM TESTS
    // =========================================================================

    test("empty root stack delegates to system") {
        // Given: Empty root stack
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = emptyList()
        )

        // When
        val state = calculateCascadeBackState(root)

        // Then
        state.delegatesToSystem.shouldBeTrue()
        state.targetNode.shouldBeNull()
    }

    test("nested cascade that reaches root with 1 child delegates to system") {
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
        state.delegatesToSystem.shouldBeTrue()
        state.targetNode.shouldBeNull()
    }

    // =========================================================================
    // WOULDCASCADE TESTS
    // =========================================================================

    test("wouldCascade returns false for normal pop") {
        // Given: Stack with 2 items
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("s2"), NodeKey("root"), ProfileDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(screen1, screen2)
        )

        // When/Then
        wouldCascade(root).shouldBeFalse()
    }

    test("wouldCascade returns true for nested stack with 1 item") {
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
        wouldCascade(root).shouldBeTrue()
    }

    test("wouldCascade returns false for root with 1 item") {
        // Given: Root stack with 1 item (no cascade, just delegate to system)
        val screen1 = ScreenNode(NodeKey("s1"), NodeKey("root"), HomeDestination)
        val root = StackNode(
            key = NodeKey("root"),
            parentKey = null,
            children = listOf(screen1)
        )

        // When/Then: Root stack has no parent, so no cascade possible
        wouldCascade(root).shouldBeFalse()
    }

    test("wouldCascade returns false for deeply nested stack with multiple items") {
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
        wouldCascade(root).shouldBeFalse()
    }

    // =========================================================================
    // SOURCE NODE TESTS
    // =========================================================================

    test("sourceNode is the active screen for normal pop") {
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
        state.sourceNode.key shouldBe NodeKey("s2")
    }

    test("sourceNode is the nested active screen for cascade") {
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
        state.sourceNode.key shouldBe NodeKey("c1")
    }

    // =========================================================================
    // TAB SPECIFIC TESTS
    // =========================================================================

    test("tab with multiple items on initial tab - normal pop") {
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
        state.cascadeDepth shouldBe 0
        state.exitingNode.key shouldBe NodeKey("s2")
        state.targetNode?.key shouldBe NodeKey("s1")
        state.delegatesToSystem.shouldBeFalse()
    }

    test("tab with 1 item on non-initial tab - switches tab") {
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
        state.delegatesToSystem.shouldBeTrue()
        state.targetNode.shouldBeNull()
    }
})
