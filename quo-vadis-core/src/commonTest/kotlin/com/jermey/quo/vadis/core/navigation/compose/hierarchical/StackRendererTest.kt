@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for stack navigation rendering with animations.
 *
 * Tests cover:
 * - Stack push/pop detection
 * - Animation transition creation
 * - Predictive back enablement for root stacks
 * - Back navigation detection logic
 * - Stack state changes
 */
class StackRendererTest {

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

    private object DetailDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = HomeDestination
    ): ScreenNode = ScreenNode(key, parentKey, destination)

    private fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(key, parentKey, screens.toList())

    /**
     * Helper to detect back navigation by comparing stack sizes.
     * This mirrors the logic in NavTreeRenderer.detectBackNavigation.
     */
    private fun detectBackNavigation(current: StackNode, previous: StackNode?): Boolean {
        if (previous == null) return false
        return current.children.size < previous.children.size
    }

    // =========================================================================
    // BACK NAVIGATION DETECTION TESTS
    // =========================================================================

    @Test
    fun `detectBackNavigation returns false when previous is null`() {
        // Given
        val currentStack = createStack(
            "stack",
            null,
            createScreen("s1", "stack")
        )

        // When
        val isBack = detectBackNavigation(currentStack, previous = null)

        // Then
        assertFalse(isBack, "Should not be back navigation when previous is null")
    }

    @Test
    fun `detectBackNavigation returns true when stack shrinks - pop`() {
        // Given - previous stack has 3 screens
        val screenA = createScreen("a", "stack", HomeDestination)
        val screenB = createScreen("b", "stack", ProfileDestination)
        val screenC = createScreen("c", "stack", SettingsDestination)

        val previousStack = createStack("stack", null, screenA, screenB, screenC)

        // Given - current stack has 2 screens (screenC was popped)
        val currentStack = createStack("stack", null, screenA, screenB)

        // When
        val isBack = detectBackNavigation(currentStack, previousStack)

        // Then
        assertTrue(isBack, "Should detect back navigation when stack shrinks")
    }

    @Test
    fun `detectBackNavigation returns false when stack grows - push`() {
        // Given - previous stack has 1 screen
        val screenA = createScreen("a", "stack", HomeDestination)
        val previousStack = createStack("stack", null, screenA)

        // Given - current stack has 2 screens (screenB was pushed)
        val screenB = createScreen("b", "stack", ProfileDestination)
        val currentStack = createStack("stack", null, screenA, screenB)

        // When
        val isBack = detectBackNavigation(currentStack, previousStack)

        // Then
        assertFalse(isBack, "Should not be back navigation when stack grows")
    }

    @Test
    fun `detectBackNavigation returns false when stack size unchanged`() {
        // Given
        val previousStack = createStack(
            "stack",
            null,
            createScreen("a", "stack", HomeDestination),
            createScreen("b", "stack", ProfileDestination)
        )

        // Replace second screen (same size)
        val currentStack = createStack(
            "stack",
            null,
            createScreen("a", "stack", HomeDestination),
            createScreen("c", "stack", SettingsDestination) // Different screen
        )

        // When
        val isBack = detectBackNavigation(currentStack, previousStack)

        // Then
        assertFalse(isBack, "Should not be back navigation when size unchanged")
    }

    @Test
    fun `detectBackNavigation works for multiple pops`() {
        // Given - previous stack has 5 screens
        val previousStack = createStack(
            "stack", null,
            createScreen("1", "stack"),
            createScreen("2", "stack"),
            createScreen("3", "stack"),
            createScreen("4", "stack"),
            createScreen("5", "stack")
        )

        // Given - current stack has 2 screens (3 screens popped)
        val currentStack = createStack(
            "stack", null,
            createScreen("1", "stack"),
            createScreen("2", "stack")
        )

        // When
        val isBack = detectBackNavigation(currentStack, previousStack)

        // Then
        assertTrue(isBack, "Should detect back navigation for multiple pops")
    }

    // =========================================================================
    // STACK PUSH TESTS
    // =========================================================================

    @Test
    fun `stack push adds screen to children`() {
        // Given - initial stack
        val screenA = createScreen("a", "stack", HomeDestination)
        val initialStack = createStack("stack", null, screenA)

        // When - push new screen
        val screenB = createScreen("b", "stack", ProfileDestination)
        val afterPushStack = createStack("stack", null, screenA, screenB)

        // Then
        assertEquals(1, initialStack.children.size)
        assertEquals(2, afterPushStack.children.size)
        assertEquals(screenA, afterPushStack.children[0])
        assertEquals(screenB, afterPushStack.children[1])
        assertEquals(screenB, afterPushStack.activeChild)
    }

    @Test
    fun `stack push changes active child`() {
        // Given
        val screenA = createScreen("a", "stack", HomeDestination)
        val screenB = createScreen("b", "stack", ProfileDestination)

        val beforeStack = createStack("stack", null, screenA)
        val afterStack = createStack("stack", null, screenA, screenB)

        // Then
        assertEquals(screenA, beforeStack.activeChild)
        assertEquals(screenB, afterStack.activeChild)
    }

    // =========================================================================
    // STACK POP TESTS
    // =========================================================================

    @Test
    fun `stack pop removes screen from children`() {
        // Given - initial stack with 2 screens
        val screenA = createScreen("a", "stack", HomeDestination)
        val screenB = createScreen("b", "stack", ProfileDestination)
        val initialStack = createStack("stack", null, screenA, screenB)

        // When - pop (remove last screen)
        val afterPopStack = createStack("stack", null, screenA)

        // Then
        assertEquals(2, initialStack.children.size)
        assertEquals(1, afterPopStack.children.size)
        assertEquals(screenA, afterPopStack.activeChild)
    }

    @Test
    fun `stack pop to root leaves single screen`() {
        // Given
        val screens = (1..5).map { createScreen("s$it", "stack") }
        val fullStack = StackNode("stack", null, screens)

        // When - pop to root
        val rootOnlyStack = createStack("stack", null, screens.first())

        // Then
        assertEquals(5, fullStack.children.size)
        assertEquals(1, rootOnlyStack.children.size)
        assertFalse(rootOnlyStack.canGoBack)
    }

    @Test
    fun `stack canGoBack reflects pop ability`() {
        // Given
        val singleStack = createStack("stack", null, createScreen("s1", "stack"))
        val multiStack = createStack(
            "stack", null,
            createScreen("s1", "stack"),
            createScreen("s2", "stack")
        )

        // Then
        assertFalse(singleStack.canGoBack, "Single item stack cannot go back")
        assertTrue(multiStack.canGoBack, "Multi item stack can go back")
    }

    // =========================================================================
    // PREDICTIVE BACK ENABLEMENT TESTS
    // =========================================================================

    @Test
    fun `root stack has null parentKey`() {
        // Given
        val rootStack = createStack(
            "root-stack",
            null, // Root stack has null parent
            createScreen("s1", "root-stack")
        )

        // Then
        assertNull(rootStack.parentKey)
    }

    @Test
    fun `nested stack has parentKey`() {
        // Given
        val nestedStack = createStack(
            "nested-stack",
            "parent-tabs", // Nested in tabs
            createScreen("s1", "nested-stack")
        )

        // Then
        assertEquals("parent-tabs", nestedStack.parentKey)
    }

    @Test
    fun `predictive back should be enabled only for root stacks`() {
        // Given
        val rootStack = createStack(
            "root-stack",
            null,
            createScreen("s1", "root-stack")
        )

        val nestedStack = createStack(
            "nested-stack",
            "parent-tabs",
            createScreen("s1", "nested-stack")
        )

        // Then - predictive back logic (from NavTreeRenderer)
        val rootPredictiveBackEnabled = rootStack.parentKey == null
        val nestedPredictiveBackEnabled = nestedStack.parentKey == null

        assertTrue(rootPredictiveBackEnabled, "Root stack should have predictive back")
        assertFalse(nestedPredictiveBackEnabled, "Nested stack should not have predictive back")
    }

    // =========================================================================
    // ANIMATION COORDINATOR TESTS
    // =========================================================================

    @Test
    fun `animation coordinator provides default transition`() {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        assertNotNull(coordinator.defaultTransition)
    }

    @Test
    fun `FakeNavRenderScope provides animation coordinator`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertNotNull(scope.animationCoordinator)
        assertEquals(AnimationCoordinator.Default, scope.animationCoordinator)
    }

    @Test
    fun `custom animation coordinator can be injected`() {
        // Given
        val customCoordinator = AnimationCoordinator()
        val scope = FakeNavRenderScope(animationCoordinator = customCoordinator)

        // Then
        assertEquals(customCoordinator, scope.animationCoordinator)
    }

    // =========================================================================
    // EMPTY STACK TESTS
    // =========================================================================

    @Test
    fun `empty stack has no active child`() {
        // Given
        val emptyStack = StackNode("stack", null, emptyList())

        // Then
        assertNull(emptyStack.activeChild)
        assertEquals(0, emptyStack.size)
        assertTrue(emptyStack.isEmpty)
        assertFalse(emptyStack.canGoBack)
    }

    @Test
    fun `empty stack to single screen is not back navigation`() {
        // Given
        val emptyStack = StackNode("stack", null, emptyList())
        val singleStack = createStack("stack", null, createScreen("s1", "stack"))

        // When
        val isBack = detectBackNavigation(singleStack, emptyStack)

        // Then
        assertFalse(isBack, "Growing from empty is not back navigation")
    }

    // =========================================================================
    // STACK STATE PRESERVATION TESTS
    // =========================================================================

    @Test
    fun `stack preserves all children order`() {
        // Given
        val children = (1..10).map { createScreen("screen-$it", "stack") }
        val stack = StackNode("stack", null, children)

        // Then
        assertEquals(10, stack.children.size)
        children.forEachIndexed { index, expected ->
            assertEquals(expected, stack.children[index])
        }
    }

    @Test
    fun `stack key is consistent across state changes`() {
        // Given
        val key = "my-stack"
        val stack1 = createStack(key, null, createScreen("s1", key))
        val stack2 = createStack(key, null, createScreen("s1", key), createScreen("s2", key))

        // Then
        assertEquals(key, stack1.key)
        assertEquals(key, stack2.key)
        assertEquals(stack1.key, stack2.key)
    }

    // =========================================================================
    // NESTED STACK TESTS
    // =========================================================================

    @Test
    fun `nested stack maintains parent reference`() {
        // Given - outer stack containing inner stack
        val innerScreen = createScreen("inner-screen", "inner-stack")
        val innerStack = createStack("inner-stack", "outer-stack", innerScreen)
        val outerStack = StackNode("outer-stack", null, listOf(innerStack))

        // Then
        assertEquals("outer-stack", innerStack.parentKey)
        assertNull(outerStack.parentKey)
        assertEquals(innerStack, outerStack.activeChild)
    }

    @Test
    fun `deeply nested structure maintains hierarchy`() {
        // Given - 3 levels deep
        val screen = createScreen("screen", "level2")
        val level2 = createStack("level2", "level1", screen)
        val level1 = StackNode("level1", "root", listOf(level2))
        val root = StackNode("root", null, listOf(level1))

        // Then
        assertNull(root.parentKey)
        assertEquals("root", level1.parentKey)
        assertEquals("level1", level2.parentKey)
        assertEquals("level2", screen.parentKey)
    }
}
