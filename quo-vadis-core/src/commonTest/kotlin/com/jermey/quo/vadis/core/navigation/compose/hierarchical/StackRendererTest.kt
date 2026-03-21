@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

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
class StackRendererTest : FunSpec() {
    init {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val HomeDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    val ProfileDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    val SettingsDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    val DetailDestination = object : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = HomeDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    /**
     * Helper to detect back navigation by comparing stack sizes.
     * This mirrors the logic in NavTreeRenderer.detectBackNavigation.
     */
    fun detectBackNavigation(current: StackNode, previous: StackNode?): Boolean {
        if (previous == null) return false
        return current.children.size < previous.children.size
    }

    // =========================================================================
    // BACK NAVIGATION DETECTION TESTS
    // =========================================================================

    test("detectBackNavigation returns false when previous is null") {
        // Given
        val currentStack = createStack(
            "stack",
            null,
            createScreen("s1", "stack")
        )

        // When
        val isBack = detectBackNavigation(currentStack, previous = null)

        // Then
        isBack.shouldBeFalse()
    }

    test("detectBackNavigation returns true when stack shrinks - pop") {
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
        isBack.shouldBeTrue()
    }

    test("detectBackNavigation returns false when stack grows - push") {
        // Given - previous stack has 1 screen
        val screenA = createScreen("a", "stack", HomeDestination)
        val previousStack = createStack("stack", null, screenA)

        // Given - current stack has 2 screens (screenB was pushed)
        val screenB = createScreen("b", "stack", ProfileDestination)
        val currentStack = createStack("stack", null, screenA, screenB)

        // When
        val isBack = detectBackNavigation(currentStack, previousStack)

        // Then
        isBack.shouldBeFalse()
    }

    test("detectBackNavigation returns false when stack size unchanged") {
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
        isBack.shouldBeFalse()
    }

    test("detectBackNavigation works for multiple pops") {
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
        isBack.shouldBeTrue()
    }

    // =========================================================================
    // STACK PUSH TESTS
    // =========================================================================

    test("stack push adds screen to children") {
        // Given - initial stack
        val screenA = createScreen("a", "stack", HomeDestination)
        val initialStack = createStack("stack", null, screenA)

        // When - push new screen
        val screenB = createScreen("b", "stack", ProfileDestination)
        val afterPushStack = createStack("stack", null, screenA, screenB)

        // Then
        initialStack.children.size shouldBe 1
        afterPushStack.children.size shouldBe 2
        afterPushStack.children[0] shouldBe screenA
        afterPushStack.children[1] shouldBe screenB
        afterPushStack.activeChild shouldBe screenB
    }

    test("stack push changes active child") {
        // Given
        val screenA = createScreen("a", "stack", HomeDestination)
        val screenB = createScreen("b", "stack", ProfileDestination)

        val beforeStack = createStack("stack", null, screenA)
        val afterStack = createStack("stack", null, screenA, screenB)

        // Then
        beforeStack.activeChild shouldBe screenA
        afterStack.activeChild shouldBe screenB
    }

    // =========================================================================
    // STACK POP TESTS
    // =========================================================================

    test("stack pop removes screen from children") {
        // Given - initial stack with 2 screens
        val screenA = createScreen("a", "stack", HomeDestination)
        val screenB = createScreen("b", "stack", ProfileDestination)
        val initialStack = createStack("stack", null, screenA, screenB)

        // When - pop (remove last screen)
        val afterPopStack = createStack("stack", null, screenA)

        // Then
        initialStack.children.size shouldBe 2
        afterPopStack.children.size shouldBe 1
        afterPopStack.activeChild shouldBe screenA
    }

    test("stack pop to root leaves single screen") {
        // Given
        val screens = (1..5).map { createScreen("s$it", "stack") }
        val fullStack = StackNode(NodeKey("stack"), null, screens)

        // When - pop to root
        val rootOnlyStack = createStack("stack", null, screens.first())

        // Then
        fullStack.children.size shouldBe 5
        rootOnlyStack.children.size shouldBe 1
        rootOnlyStack.canGoBack.shouldBeFalse()
    }

    test("stack canGoBack reflects pop ability") {
        // Given
        val singleStack = createStack("stack", null, createScreen("s1", "stack"))
        val multiStack = createStack(
            "stack", null,
            createScreen("s1", "stack"),
            createScreen("s2", "stack")
        )

        // Then
        singleStack.canGoBack.shouldBeFalse()
        multiStack.canGoBack.shouldBeTrue()
    }

    // =========================================================================
    // PREDICTIVE BACK ENABLEMENT TESTS
    // =========================================================================

    test("root stack has null parentKey") {
        // Given
        val rootStack = createStack(
            "root-stack",
            null, // Root stack has null parent
            createScreen("s1", "root-stack")
        )

        // Then
        rootStack.parentKey.shouldBeNull()
    }

    test("nested stack has parentKey") {
        // Given
        val nestedStack = createStack(
            "nested-stack",
            "parent-tabs", // Nested in tabs
            createScreen("s1", "nested-stack")
        )

        // Then
        nestedStack.parentKey shouldBe NodeKey("parent-tabs")
    }

    test("predictive back should be enabled only for root stacks") {
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

        rootPredictiveBackEnabled.shouldBeTrue()
        nestedPredictiveBackEnabled.shouldBeFalse()
    }

    // =========================================================================
    // ANIMATION COORDINATOR TESTS
    // =========================================================================

    test("animation coordinator provides default transition") {
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
        scope.animationCoordinator shouldBe AnimationCoordinator.Default
    }

    test("custom animation coordinator can be injected") {
        // Given
        val customCoordinator = AnimationCoordinator()
        val scope = FakeNavRenderScope(animationCoordinator = customCoordinator)

        // Then
        scope.animationCoordinator shouldBe customCoordinator
    }

    // =========================================================================
    // EMPTY STACK TESTS
    // =========================================================================

    test("empty stack has no active child") {
        // Given
        val emptyStack = StackNode(NodeKey("stack"), null, emptyList())

        // Then
        emptyStack.activeChild.shouldBeNull()
        emptyStack.size shouldBe 0
        emptyStack.isEmpty.shouldBeTrue()
        emptyStack.canGoBack.shouldBeFalse()
    }

    test("empty stack to single screen is not back navigation") {
        // Given
        val emptyStack = StackNode(NodeKey("stack"), null, emptyList())
        val singleStack = createStack("stack", null, createScreen("s1", "stack"))

        // When
        val isBack = detectBackNavigation(singleStack, emptyStack)

        // Then
        isBack.shouldBeFalse()
    }

    // =========================================================================
    // STACK STATE PRESERVATION TESTS
    // =========================================================================

    test("stack preserves all children order") {
        // Given
        val children = (1..10).map { createScreen("screen-$it", "stack") }
        val stack = StackNode(NodeKey("stack"), null, children)

        // Then
        stack.children.size shouldBe 10
        children.forEachIndexed { index, expected ->
            stack.children[index] shouldBe expected
        }
    }

    test("stack key is consistent across state changes") {
        // Given
        val key = NodeKey("my-stack")
        val stack1 = createStack(key.value, null, createScreen("s1", key.value))
        val stack2 = createStack(key.value, null, createScreen("s1", key.value), createScreen("s2", key.value))

        // Then
        stack1.key shouldBe key
        stack2.key shouldBe key
        stack2.key shouldBe stack1.key
    }

    // =========================================================================
    // NESTED STACK TESTS
    // =========================================================================

    test("nested stack maintains parent reference") {
        // Given - outer stack containing inner stack
        val innerScreen = createScreen("inner-screen", "inner-stack")
        val innerStack = createStack("inner-stack", "outer-stack", innerScreen)
        val outerStack = StackNode(NodeKey("outer-stack"), null, listOf(innerStack))

        // Then
        innerStack.parentKey shouldBe NodeKey("outer-stack")
        outerStack.parentKey.shouldBeNull()
        outerStack.activeChild shouldBe innerStack
    }

    test("deeply nested structure maintains hierarchy") {
        // Given - 3 levels deep
        val screen = createScreen("screen", "level2")
        val level2 = createStack("level2", "level1", screen)
        val level1 = StackNode(NodeKey("level1"), NodeKey("root"), listOf(level2))
        val root = StackNode(NodeKey("root"), null, listOf(level1))

        // Then
        root.parentKey.shouldBeNull()
        level1.parentKey shouldBe NodeKey("root")
        level2.parentKey shouldBe NodeKey("level1")
        screen.parentKey shouldBe NodeKey("level2")
    }
    } // init
}
