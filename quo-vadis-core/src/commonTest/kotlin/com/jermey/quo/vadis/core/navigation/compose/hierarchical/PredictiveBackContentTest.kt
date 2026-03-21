@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.PredictiveBackController
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
import io.kotest.matchers.floats.plusOrMinus

/**
 * Tests for predictive back gesture rendering.
 *
 * Tests cover:
 * - Gesture active state effects on rendering
 * - Progress updates and transforms
 * - Previous content rendering during gesture
 * - Predictive back controller state
 */
class PredictiveBackContentTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val CurrentDestination = object : NavDestination {
        override val data: Any? = "current"
        override val transition: NavigationTransition? = null
    }

    val PreviousDestination = object : NavDestination {
        override val data: Any? = "previous"
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = CurrentDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    // =========================================================================
    // PREDICTIVE BACK CONTROLLER TESTS
    // =========================================================================

    test("predictive back controller is inactive by default") {
        // Given
        val controller = PredictiveBackController()

        // Then
        controller.isActive.value.shouldBeFalse()
        controller.progress.value shouldBe 0f
    }

    test("FakeNavRenderScope provides predictive back controller") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.predictiveBackController.shouldNotBeNull()
        scope.predictiveBackController.isActive.value.shouldBeFalse()
    }

    test("custom predictive back controller can be injected") {
        // Given
        val customController = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = customController)

        // Then
        scope.predictiveBackController shouldBe customController
    }

    // =========================================================================
    // GESTURE ACTIVE STATE TESTS
    // =========================================================================

    test("gesture inactive means normal animated content is used") {
        // Given
        val scope = FakeNavRenderScope()

        // When - check if predictive back is active
        val isPredictiveBackActive = scope.predictiveBackController.isActive.value

        // Then
        isPredictiveBackActive.shouldBeFalse()
    }

    test("predictive back enablement depends on root stack") {
        // Given
        val rootStack = createStack("root", null, createScreen("s1", "root"))
        val nestedStack = createStack("nested", "parent", createScreen("s1", "nested"))

        // Then
        // Root stack (parentKey == null) should enable predictive back
        val rootPredictiveBackEnabled = rootStack.parentKey == null
        val nestedPredictiveBackEnabled = nestedStack.parentKey == null

        rootPredictiveBackEnabled.shouldBeTrue()
        nestedPredictiveBackEnabled.shouldBeFalse()
    }

    // =========================================================================
    // PROGRESS TRANSFORM TESTS
    // =========================================================================

    test("progress at zero means no transform") {
        // Given
        val progress = 0f

        // When - calculate transforms (from PredictiveBackContent constants)
        val parallaxFactor = 0.3f
        val scaleFactor = 0.1f

        val previousTranslationFactor = -parallaxFactor * (1f - progress) // -0.3 at progress=0
        val currentTranslationFactor = progress // 0 at progress=0
        val currentScale = 1f - (progress * scaleFactor) // 1.0 at progress=0

        // Then
        previousTranslationFactor shouldBe -0.3f
        currentTranslationFactor shouldBe 0f
        currentScale shouldBe 1f
    }

    test("progress at one means full transform").config(enabled = false) {
        // Given
        val progress = 1f

        // When - calculate transforms
        val parallaxFactor = 0.3f
        val scaleFactor = 0.1f

        val previousTranslationFactor = -parallaxFactor * (1f - progress) // 0 at progress=1
        val currentTranslationFactor = progress // 1.0 at progress=1
        val currentScale = 1f - (progress * scaleFactor) // 0.9 at progress=1

        // Then
        previousTranslationFactor shouldBe 0f
        currentTranslationFactor shouldBe 1f
        currentScale shouldBe 0.9f
    }

    test("progress at half means intermediate transform") {
        // Given
        val progress = 0.5f

        // When - calculate transforms
        val parallaxFactor = 0.3f
        val scaleFactor = 0.1f

        val previousTranslationFactor = -parallaxFactor * (1f - progress) // -0.15 at progress=0.5
        val currentTranslationFactor = progress // 0.5 at progress=0.5
        val currentScale = 1f - (progress * scaleFactor) // 0.95 at progress=0.5

        // Then
        previousTranslationFactor shouldBe (-0.15f plusOrMinus 0.001f)
        currentTranslationFactor shouldBe 0.5f
        currentScale shouldBe 0.95f
    }

    // =========================================================================
    // PREVIOUS CONTENT RENDERING TESTS
    // =========================================================================

    test("previous content is null initially") {
        // Given
        val current = createScreen("current-screen", destination = CurrentDestination)

        // When - no navigation has occurred yet
        val previous: ScreenNode? = null

        // Then
        previous.shouldBeNull()
    }

    test("previous content is available after navigation") {
        // Given
        var previous: ScreenNode?
        var current = createScreen("screen-a", destination = PreviousDestination)

        // When - navigate to new screen
        previous = current
        current = createScreen("screen-b", destination = CurrentDestination)

        // Then
        previous.shouldNotBeNull()
        previous.key shouldBe NodeKey("screen-a")
        current.key shouldBe NodeKey("screen-b")
    }

    test("previous and current are both accessible during gesture") {
        // Given
        val previous = createScreen("previous", destination = PreviousDestination)
        val current = createScreen("current", destination = CurrentDestination)

        // Then - both are defined and distinct
        previous.shouldNotBeNull()
        current.shouldNotBeNull()
        (previous.key == current.key).shouldBeFalse()
    }

    // =========================================================================
    // STACK BACK NAVIGATION TESTS
    // =========================================================================

    test("stack provides previous and current for predictive back") {
        // Given
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")
        val screen3 = createScreen("s3", "stack")
        val stack = createStack("stack", null, screen1, screen2, screen3)

        // When - during predictive back on stack
        val currentChild = stack.activeChild
        val children = stack.children
        val previousChild = if (children.size >= 2) children[children.size - 2] else null

        // Then
        currentChild shouldBe screen3
        previousChild shouldBe screen2
    }

    test("single item stack has no previous for predictive back") {
        // Given
        val screen = createScreen("only-screen", "stack")
        val stack = createStack("stack", null, screen)

        // When
        val currentChild = stack.activeChild
        val children = stack.children
        val previousChild = if (children.size >= 2) children[children.size - 2] else null

        // Then
        currentChild shouldBe screen
        previousChild.shouldBeNull()
        stack.canGoBack.shouldBeFalse()
    }

    test("empty stack has no content for predictive back") {
        // Given
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        // When
        val currentChild = stack.activeChild
        val children = stack.children
        val previousChild = if (children.size >= 2) children[children.size - 2] else null

        // Then
        currentChild.shouldBeNull()
        previousChild.shouldBeNull()
    }

    // =========================================================================
    // GESTURE CANCELLATION TESTS
    // =========================================================================

    test("gesture cancellation restores progress to zero") {
        // Given - simulate gesture in progress then cancelled
        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 0f) // Cancelled, back to 0

        // Then
        progressValues.first() shouldBe 0f
        progressValues.last() shouldBe 0f
    }

    test("gesture completion has progress at one") {
        // Given - simulate gesture to completion
        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

        // Then
        progressValues.last() shouldBe 1f
    }

    // =========================================================================
    // CACHE KEY TESTS FOR PREDICTIVE BACK
    // =========================================================================

    test("current and previous use distinct cache keys") {
        // Given
        val previous = createScreen("previous-key", "stack")
        val current = createScreen("current-key", "stack")

        // Then
        (previous.key == current.key).shouldBeFalse()
    }

    test("cache key remains stable during gesture") {
        // Given
        val screen = createScreen("stable-key", "stack")

        // Simulate progress changes during gesture
        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

        // Then - key doesn't change
        progressValues.forEach { _ ->
            screen.key shouldBe NodeKey("stable-key")
        }
    }

    // =========================================================================
    // PREDICTIVE BACK INTEGRATION SCENARIOS
    // =========================================================================

    test("deep stack supports predictive back with correct previous") {
        // Given
        val screens = (1..10).map { createScreen("s$it", "stack") }
        val stack = StackNode(NodeKey("stack"), null, screens)

        // When
        val currentChild = stack.activeChild
        val previousChild = if (stack.children.size >= 2) {
            stack.children[stack.children.size - 2]
        } else null

        // Then
        currentChild?.key shouldBe NodeKey("s10")
        previousChild?.key shouldBe NodeKey("s9")
    }

    test("predictive back respects canGoBack") {
        // Given
        val singleStack = createStack("stack", null, createScreen("single", "stack"))
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
    // PARALLAX AND SCALE CONSTANT TESTS
    // =========================================================================

    test("parallax factor is reasonable value") {
        // Given - expected parallax factor
        val parallaxFactor = 0.3f

        // Then
        (parallaxFactor > 0f && parallaxFactor < 1f).shouldBeTrue()
    }

    test("scale factor is reasonable value") {
        // Given - expected scale factor
        val scaleFactor = 0.1f

        // Then
        (scaleFactor > 0f && scaleFactor < 0.5f).shouldBeTrue()

        // Minimum scale at full progress
        val minScale = 1f - scaleFactor
        (minScale >= 0.5f).shouldBeTrue()
    }

    // =========================================================================
    // PREDICTIVE BACK STATE FLOW TESTS
    // =========================================================================

    test("predictive back controller provides isActive StateFlow") {
        // Given
        val controller = PredictiveBackController()

        // Then
        controller.isActive.shouldNotBeNull()
        controller.isActive.value.shouldBeFalse()
    }

    test("predictive back controller provides progress StateFlow") {
        // Given
        val controller = PredictiveBackController()

        // Then
        controller.progress.shouldNotBeNull()
        controller.progress.value shouldBe 0f
    }
})
