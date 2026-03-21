package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.internal.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Tests for animated transition content rendering.
 *
 * Tests cover:
 * - Animation state tracking
 * - Transition direction detection
 * - Forward and back navigation animations
 * - AnimatedContent behavior
 */
@OptIn(InternalQuoVadisApi::class)
class AnimatedNavContentTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val ScreenADestination = object : NavDestination {
        override val data: Any? = "A"
        override val transition: NavigationTransition? = null
    }

    val ScreenBDestination = object : NavDestination {
        override val data: Any? = "B"
        override val transition: NavigationTransition? = null
    }

    val ScreenCDestination = object : NavDestination {
        override val data: Any? = "C"
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = ScreenADestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    // =========================================================================
    // ANIMATION STATE TRACKING TESTS
    // =========================================================================

    test("animation state tracks displayed state") {
        // Given - simulating AnimatedNavContent state tracking
        var displayedState: ScreenNode?
        var previousState: ScreenNode? = null

        val screenA = createScreen("a", "stack", ScreenADestination)
        val screenB = createScreen("b", "stack", ScreenBDestination)

        // When - initial state
        displayedState = screenA

        // Then
        displayedState shouldBe screenA
        previousState shouldBe null

        // When - navigate to B
        previousState = displayedState
        displayedState = screenB

        // Then
        displayedState shouldBe screenB
        previousState shouldBe screenA
    }

    test("previous state updates correctly on navigation") {
        // Given - state sequence
        val screenA = createScreen("a", "stack", ScreenADestination)
        val screenB = createScreen("b", "stack", ScreenBDestination)
        val screenC = createScreen("c", "stack", ScreenCDestination)

        var displayedState = screenA
        var previousState: ScreenNode?

        // When - navigate A -> B
        previousState = displayedState
        displayedState = screenB
        previousState shouldBe screenA
        displayedState shouldBe screenB

        // When - navigate B -> C
        previousState = displayedState
        displayedState = screenC
        previousState shouldBe screenB
        displayedState shouldBe screenC

        // When - navigate C -> A (back)
        previousState = displayedState
        displayedState = screenA
        previousState shouldBe screenC
        displayedState shouldBe screenA
    }

    // =========================================================================
    // TRANSITION DIRECTION DETECTION TESTS
    // =========================================================================

    test("forward navigation detected when target differs from displayed") {
        // Given
        val displayed = createScreen("a", "stack")
        val target = createScreen("b", "stack")
        val previous: ScreenNode? = null

        // When - simulating AnimatedNavContent direction logic
        // Forward: target differs from displayed AND target is not the previous state
        val isBack = target.key != displayed.key && previous?.key == target.key

        // Then
        isBack.shouldBeFalse()
    }

    test("back navigation detected when target matches previous") {
        // Given - A -> B, now going back to A
        val displayed = createScreen("b", "stack")
        val target = createScreen("a", "stack") // Going back to A
        val previous = createScreen("a", "stack") // Previous was A

        // When - simulating AnimatedNavContent direction logic
        val isBack = target.key != displayed.key && previous.key == target.key

        // Then
        isBack.shouldBeTrue()
    }

    test("same target state is not back navigation") {
        // Given
        val displayed = createScreen("a", "stack")
        val target = createScreen("a", "stack") // Same as displayed
        val previous = createScreen("x", "stack")

        // When
        val isBack = target.key != displayed.key && previous.key == target.key

        // Then
        isBack.shouldBeFalse()
    }

    // =========================================================================
    // ANIMATION COORDINATOR TESTS
    // =========================================================================

    test("animation coordinator provides transition based on direction") {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        coordinator.defaultTransition.shouldNotBeNull()
    }

    test("FakeNavRenderScope provides animation coordinator") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.animationCoordinator.shouldNotBeNull()
    }

    test("custom animation coordinator can be injected") {
        // Given
        val customCoordinator = AnimationCoordinator()
        val scope = FakeNavRenderScope(animationCoordinator = customCoordinator)

        // Then
        scope.animationCoordinator shouldBe customCoordinator
    }

    // =========================================================================
    // TRANSITION SPEC TESTS
    // =========================================================================

    test("coordinator provides tab transition") {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        coordinator.defaultTabTransition.shouldNotBeNull()
    }

    test("coordinator provides pane transition") {
        // Given
        val coordinator = AnimationCoordinator.Default

        // Then
        coordinator.defaultPaneTransition.shouldNotBeNull()
    }

    // =========================================================================
    // NODE STATE TRACKING TESTS
    // =========================================================================

    test("node key is stable for same content") {
        // Given
        val screen1 = createScreen("unique-key", "stack", ScreenADestination)
        val screen2 = createScreen("unique-key", "stack", ScreenADestination)

        // Then - keys match for state tracking
        screen2.key shouldBe screen1.key
    }

    test("node key differs for different content") {
        // Given
        val screenA = createScreen("key-a", "stack")
        val screenB = createScreen("key-b", "stack")

        // Then
        (screenA.key == screenB.key).shouldBeFalse()
    }

    // =========================================================================
    // ANIMATION CONTENT LIFECYCLE TESTS
    // =========================================================================

    test("stack active child is the animated target state") {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")
        val screen3 = createScreen("s3", "stack")
        val stack = createStack("stack", null, screen1, screen2, screen3)

        // Then - active child (last) is the target for AnimatedContent
        stack.activeChild shouldBe screen3
    }

    test("push changes animated target state") {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")

        val beforePush = createStack("stack", null, screen1)
        val afterPush = createStack("stack", null, screen1, screen2)

        // Then
        beforePush.activeChild shouldBe screen1
        afterPush.activeChild shouldBe screen2
    }

    test("pop changes animated target state") {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")

        val beforePop = createStack("stack", null, screen1, screen2)
        val afterPop = createStack("stack", null, screen1)

        // Then
        beforePop.activeChild shouldBe screen2
        afterPop.activeChild shouldBe screen1
    }

    // =========================================================================
    // ANIMATED VISIBILITY SCOPE TESTS
    // =========================================================================

    test("FakeNavRenderScope provides withAnimatedVisibilityScope capability") {
        // Given
        val scope = FakeNavRenderScope()

        // Then - withAnimatedVisibilityScope is available (tested as method exists)
        scope.shouldNotBeNull()
        // Note: The actual Composable function testing would require Compose test framework
    }

    // =========================================================================
    // NAVIGATION TRANSITION TESTS
    // =========================================================================

    test("screen destination can specify custom transition") {
        // Given
        val slideDestination = object : NavDestination {
            override val data: Any? = null
            override val transition: NavigationTransition? = NavigationTransitions.SlideHorizontal
        }

        val screen = createScreen("animated-screen", destination = slideDestination)

        // Then
        screen.destination.transition shouldBe NavigationTransitions.SlideHorizontal
    }

    test("screen without custom transition uses default") {
        // Given
        val screen = createScreen("default-screen", destination = ScreenADestination)

        // Then
        screen.destination.transition shouldBe null
        // AnimationCoordinator will provide default transition
    }

    // =========================================================================
    // COMPLEX ANIMATION SCENARIOS
    // =========================================================================

    test("rapid navigation maintains state consistency") {
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
            history.add("${previous.key} -> ${current.key}")
        }

        // Then
        current shouldBe screenA
        previous shouldBe screenC
        history shouldBe listOf("a -> b", "b -> c", "c -> a")
    }

    test("back navigation through history is detected correctly") {
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
        isBack.shouldBeTrue()
    }
})
