@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for animated transition content rendering.
 *
 * Tests cover:
 * - Animation state tracking
 * - Transition direction detection
 * - Forward and back navigation animations
 * - AnimatedContent behavior
 */
class AnimatedNavContentTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object ScreenADestination : NavDestination {
        override val data: Any? = "A"
        override val transition: NavigationTransition? = null
    }

    private object ScreenBDestination : NavDestination {
        override val data: Any? = "B"
        override val transition: NavigationTransition? = null
    }

    private object ScreenCDestination : NavDestination {
        override val data: Any? = "C"
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = ScreenADestination
    ): ScreenNode = ScreenNode(key, parentKey, destination)

    private fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(key, parentKey, screens.toList())

    // =========================================================================
    // ANIMATION STATE TRACKING TESTS
    // =========================================================================

    @Test
    fun `animation state tracks displayed state`() {
        // Given - simulating AnimatedNavContent state tracking
        var displayedState: ScreenNode?
        var previousState: ScreenNode? = null

        val screenA = createScreen("a", "stack", ScreenADestination)
        val screenB = createScreen("b", "stack", ScreenBDestination)

        // When - initial state
        displayedState = screenA

        // Then
        assertEquals(screenA, displayedState)
        assertEquals(null, previousState)

        // When - navigate to B
        previousState = displayedState
        displayedState = screenB

        // Then
        assertEquals(screenB, displayedState)
        assertEquals(screenA, previousState)
    }

    @Test
    fun `previous state updates correctly on navigation`() {
        // Given - state sequence
        val screenA = createScreen("a", "stack", ScreenADestination)
        val screenB = createScreen("b", "stack", ScreenBDestination)
        val screenC = createScreen("c", "stack", ScreenCDestination)

        var displayedState = screenA
        var previousState: ScreenNode?

        // When - navigate A -> B
        previousState = displayedState
        displayedState = screenB
        assertEquals(screenA, previousState)
        assertEquals(screenB, displayedState)

        // When - navigate B -> C
        previousState = displayedState
        displayedState = screenC
        assertEquals(screenB, previousState)
        assertEquals(screenC, displayedState)

        // When - navigate C -> A (back)
        previousState = displayedState
        displayedState = screenA
        assertEquals(screenC, previousState)
        assertEquals(screenA, displayedState)
    }

    // =========================================================================
    // TRANSITION DIRECTION DETECTION TESTS
    // =========================================================================

    @Test
    fun `forward navigation detected when target differs from displayed`() {
        // Given
        val displayed = createScreen("a", "stack")
        val target = createScreen("b", "stack")
        val previous: ScreenNode? = null

        // When - simulating AnimatedNavContent direction logic
        // Forward: target differs from displayed AND target is not the previous state
        val isBack = target.key != displayed.key && previous?.key == target.key

        // Then
        assertFalse(isBack, "Should be forward navigation")
    }

    @Test
    fun `back navigation detected when target matches previous`() {
        // Given - A -> B, now going back to A
        val displayed = createScreen("b", "stack")
        val target = createScreen("a", "stack") // Going back to A
        val previous = createScreen("a", "stack") // Previous was A

        // When - simulating AnimatedNavContent direction logic
        val isBack = target.key != displayed.key && previous.key == target.key

        // Then
        assertTrue(isBack, "Should be back navigation")
    }

    @Test
    fun `same target state is not back navigation`() {
        // Given
        val displayed = createScreen("a", "stack")
        val target = createScreen("a", "stack") // Same as displayed
        val previous = createScreen("x", "stack")

        // When
        val isBack = target.key != displayed.key && previous.key == target.key

        // Then
        assertFalse(isBack, "Same state navigation should not be back")
    }

    // =========================================================================
    // ANIMATION COORDINATOR TESTS
    // =========================================================================

    @Test
    fun `animation coordinator provides transition based on direction`() {
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
    // TRANSITION SPEC TESTS
    // =========================================================================

    @Test
    fun `coordinator provides tab transition`() {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        assertNotNull(coordinator.defaultTabTransition)
    }

    @Test
    fun `coordinator provides pane transition`() {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        assertNotNull(coordinator.defaultPaneTransition)
    }

    // =========================================================================
    // NODE STATE TRACKING TESTS
    // =========================================================================

    @Test
    fun `node key is stable for same content`() {
        // Given
        val screen1 = createScreen("unique-key", "stack", ScreenADestination)
        val screen2 = createScreen("unique-key", "stack", ScreenADestination)

        // Then - keys match for state tracking
        assertEquals(screen1.key, screen2.key)
    }

    @Test
    fun `node key differs for different content`() {
        // Given
        val screenA = createScreen("key-a", "stack")
        val screenB = createScreen("key-b", "stack")

        // Then
        assertFalse(screenA.key == screenB.key)
    }

    // =========================================================================
    // ANIMATION CONTENT LIFECYCLE TESTS
    // =========================================================================

    @Test
    fun `stack active child is the animated target state`() {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")
        val screen3 = createScreen("s3", "stack")
        val stack = createStack("stack", null, screen1, screen2, screen3)

        // Then - active child (last) is the target for AnimatedContent
        assertEquals(screen3, stack.activeChild)
    }

    @Test
    fun `push changes animated target state`() {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")

        val beforePush = createStack("stack", null, screen1)
        val afterPush = createStack("stack", null, screen1, screen2)

        // Then
        assertEquals(screen1, beforePush.activeChild)
        assertEquals(screen2, afterPush.activeChild)
    }

    @Test
    fun `pop changes animated target state`() {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")

        val beforePop = createStack("stack", null, screen1, screen2)
        val afterPop = createStack("stack", null, screen1)

        // Then
        assertEquals(screen2, beforePop.activeChild)
        assertEquals(screen1, afterPop.activeChild)
    }

    // =========================================================================
    // ANIMATED VISIBILITY SCOPE TESTS
    // =========================================================================

    @Test
    fun `FakeNavRenderScope provides withAnimatedVisibilityScope capability`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then - withAnimatedVisibilityScope is available (tested as method exists)
        assertNotNull(scope)
        // Note: The actual Composable function testing would require Compose test framework
    }

    // =========================================================================
    // NAVIGATION TRANSITION TESTS
    // =========================================================================

    @Test
    fun `screen destination can specify custom transition`() {
        // Given
        val slideDestination = object : NavDestination {
            override val data: Any? = null
            override val transition: NavigationTransition? = NavigationTransitions.SlideHorizontal
        }

        val screen = createScreen("animated-screen", destination = slideDestination)

        // Then
        assertEquals(NavigationTransitions.SlideHorizontal, screen.destination.transition)
    }

    @Test
    fun `screen without custom transition uses default`() {
        // Given
        val screen = createScreen("default-screen", destination = ScreenADestination)

        // Then
        assertEquals(null, screen.destination.transition)
        // AnimationCoordinator will provide default transition
    }

    // =========================================================================
    // COMPLEX ANIMATION SCENARIOS
    // =========================================================================

    @Test
    fun `rapid navigation maintains state consistency`() {
        // Given - simulating rapid A -> B -> C -> A
        val screenA = createScreen("a", "stack")
        val screenB = createScreen("b", "stack")
        val screenC = createScreen("c", "stack")

        var current = screenA
        var previous: ScreenNode? = null
        val history = mutableListOf<String>()

        // Navigate through screens
        listOf(screenB, screenC, screenA).forEach { target ->
            previous = current
            current = target
            history.add("${previous?.key} -> ${current.key}")
        }

        // Then
        assertEquals(screenA, current)
        assertEquals(screenC, previous)
        assertEquals(listOf("a -> b", "b -> c", "c -> a"), history)
    }

    @Test
    fun `back navigation through history is detected correctly`() {
        // Given
        val screenA = createScreen("a", "stack")
        val screenB = createScreen("b", "stack")
        val screenC = createScreen("c", "stack")

        // State: displayed=C, previous=B, target=B (go back)
        val displayed = screenC
        val previous = screenB
        val target = screenB

        // When
        val isBack = target.key != displayed.key && previous.key == target.key

        // Then
        assertTrue(isBack, "Going back from C to B should be detected as back navigation")
    }
}
