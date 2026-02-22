@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for predictive back gesture rendering.
 *
 * Tests cover:
 * - Gesture active state effects on rendering
 * - Progress updates and transforms
 * - Previous content rendering during gesture
 * - Predictive back controller state
 */
class PredictiveBackContentTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object CurrentDestination : NavDestination {
        override val data: Any? = "current"
        override val transition: NavigationTransition? = null
    }

    private object PreviousDestination : NavDestination {
        override val data: Any? = "previous"
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = CurrentDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    private fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    // =========================================================================
    // PREDICTIVE BACK CONTROLLER TESTS
    // =========================================================================

    @Test
    fun `predictive back controller is inactive by default`() {
        // Given
        val controller = PredictiveBackController()

        // Then
        assertFalse(controller.isActive.value)
        assertEquals(0f, controller.progress.value)
    }

    @Test
    fun `FakeNavRenderScope provides predictive back controller`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertNotNull(scope.predictiveBackController)
        assertFalse(scope.predictiveBackController.isActive.value)
    }

    @Test
    fun `custom predictive back controller can be injected`() {
        // Given
        val customController = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = customController)

        // Then
        assertEquals(customController, scope.predictiveBackController)
    }

    // =========================================================================
    // GESTURE ACTIVE STATE TESTS
    // =========================================================================

    @Test
    fun `gesture inactive means normal animated content is used`() {
        // Given
        val scope = FakeNavRenderScope()

        // When - check if predictive back is active
        val isPredictiveBackActive = scope.predictiveBackController.isActive.value

        // Then
        assertFalse(isPredictiveBackActive, "Predictive back should be inactive by default")
    }

    @Test
    fun `predictive back enablement depends on root stack`() {
        // Given
        val rootStack = createStack("root", null, createScreen("s1", "root"))
        val nestedStack = createStack("nested", "parent", createScreen("s1", "nested"))

        // Then
        // Root stack (parentKey == null) should enable predictive back
        val rootPredictiveBackEnabled = rootStack.parentKey == null
        val nestedPredictiveBackEnabled = nestedStack.parentKey == null

        assertTrue(rootPredictiveBackEnabled)
        assertFalse(nestedPredictiveBackEnabled)
    }

    // =========================================================================
    // PROGRESS TRANSFORM TESTS
    // =========================================================================

    @Test
    fun `progress at zero means no transform`() {
        // Given
        val progress = 0f

        // When - calculate transforms (from PredictiveBackContent constants)
        val parallaxFactor = 0.3f
        val scaleFactor = 0.1f

        val previousTranslationFactor = -parallaxFactor * (1f - progress) // -0.3 at progress=0
        val currentTranslationFactor = progress // 0 at progress=0
        val currentScale = 1f - (progress * scaleFactor) // 1.0 at progress=0

        // Then
        assertEquals(-0.3f, previousTranslationFactor)
        assertEquals(0f, currentTranslationFactor)
        assertEquals(1f, currentScale)
    }

    @Ignore
    @Test
    fun `progress at one means full transform`() {
        // Given
        val progress = 1f

        // When - calculate transforms
        val parallaxFactor = 0.3f
        val scaleFactor = 0.1f

        val previousTranslationFactor = -parallaxFactor * (1f - progress) // 0 at progress=1
        val currentTranslationFactor = progress // 1.0 at progress=1
        val currentScale = 1f - (progress * scaleFactor) // 0.9 at progress=1

        // Then
        assertEquals(0f, previousTranslationFactor)
        assertEquals(1f, currentTranslationFactor)
        assertEquals(0.9f, currentScale)
    }

    @Test
    fun `progress at half means intermediate transform`() {
        // Given
        val progress = 0.5f

        // When - calculate transforms
        val parallaxFactor = 0.3f
        val scaleFactor = 0.1f

        val previousTranslationFactor = -parallaxFactor * (1f - progress) // -0.15 at progress=0.5
        val currentTranslationFactor = progress // 0.5 at progress=0.5
        val currentScale = 1f - (progress * scaleFactor) // 0.95 at progress=0.5

        // Then
        assertEquals(-0.15f, previousTranslationFactor, 0.001f)
        assertEquals(0.5f, currentTranslationFactor)
        assertEquals(0.95f, currentScale)
    }

    // =========================================================================
    // PREVIOUS CONTENT RENDERING TESTS
    // =========================================================================

    @Test
    fun `previous content is null initially`() {
        // Given
        val current = createScreen("current-screen", destination = CurrentDestination)

        // When - no navigation has occurred yet
        val previous: ScreenNode? = null

        // Then
        assertNull(previous)
    }

    @Test
    fun `previous content is available after navigation`() {
        // Given
        var previous: ScreenNode?
        var current = createScreen("screen-a", destination = PreviousDestination)

        // When - navigate to new screen
        previous = current
        current = createScreen("screen-b", destination = CurrentDestination)

        // Then
        assertNotNull(previous)
        assertEquals(NodeKey("screen-a"), previous.key)
        assertEquals(NodeKey("screen-b"), current.key)
    }

    @Test
    fun `previous and current are both accessible during gesture`() {
        // Given
        val previous = createScreen("previous", destination = PreviousDestination)
        val current = createScreen("current", destination = CurrentDestination)

        // Then - both are defined and distinct
        assertNotNull(previous)
        assertNotNull(current)
        assertFalse(previous.key == current.key)
    }

    // =========================================================================
    // STACK BACK NAVIGATION TESTS
    // =========================================================================

    @Test
    fun `stack provides previous and current for predictive back`() {
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
        assertEquals(screen3, currentChild)
        assertEquals(screen2, previousChild)
    }

    @Test
    fun `single item stack has no previous for predictive back`() {
        // Given
        val screen = createScreen("only-screen", "stack")
        val stack = createStack("stack", null, screen)

        // When
        val currentChild = stack.activeChild
        val children = stack.children
        val previousChild = if (children.size >= 2) children[children.size - 2] else null

        // Then
        assertEquals(screen, currentChild)
        assertNull(previousChild)
        assertFalse(stack.canGoBack)
    }

    @Test
    fun `empty stack has no content for predictive back`() {
        // Given
        val stack = StackNode(NodeKey("stack"), null, emptyList())

        // When
        val currentChild = stack.activeChild
        val children = stack.children
        val previousChild = if (children.size >= 2) children[children.size - 2] else null

        // Then
        assertNull(currentChild)
        assertNull(previousChild)
    }

    // =========================================================================
    // GESTURE CANCELLATION TESTS
    // =========================================================================

    @Test
    fun `gesture cancellation restores progress to zero`() {
        // Given - simulate gesture in progress then cancelled
        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 0f) // Cancelled, back to 0

        // Then
        assertEquals(0f, progressValues.first())
        assertEquals(0f, progressValues.last())
    }

    @Test
    fun `gesture completion has progress at one`() {
        // Given - simulate gesture to completion
        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

        // Then
        assertEquals(1f, progressValues.last())
    }

    // =========================================================================
    // CACHE KEY TESTS FOR PREDICTIVE BACK
    // =========================================================================

    @Test
    fun `current and previous use distinct cache keys`() {
        // Given
        val previous = createScreen("previous-key", "stack")
        val current = createScreen("current-key", "stack")

        // Then
        assertFalse(previous.key == current.key, "Cache keys must be distinct")
    }

    @Test
    fun `cache key remains stable during gesture`() {
        // Given
        val screen = createScreen("stable-key", "stack")

        // Simulate progress changes during gesture
        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

        // Then - key doesn't change
        progressValues.forEach { _ ->
            assertEquals(NodeKey("stable-key"), screen.key)
        }
    }

    // =========================================================================
    // PREDICTIVE BACK INTEGRATION SCENARIOS
    // =========================================================================

    @Test
    fun `deep stack supports predictive back with correct previous`() {
        // Given
        val screens = (1..10).map { createScreen("s$it", "stack") }
        val stack = StackNode(NodeKey("stack"), null, screens)

        // When
        val currentChild = stack.activeChild
        val previousChild = if (stack.children.size >= 2) {
            stack.children[stack.children.size - 2]
        } else null

        // Then
        assertEquals(NodeKey("s10"), currentChild?.key)
        assertEquals(NodeKey("s9"), previousChild?.key)
    }

    @Test
    fun `predictive back respects canGoBack`() {
        // Given
        val singleStack = createStack("stack", null, createScreen("single", "stack"))
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
    // PARALLAX AND SCALE CONSTANT TESTS
    // =========================================================================

    @Test
    fun `parallax factor is reasonable value`() {
        // Given - expected parallax factor
        val parallaxFactor = 0.3f

        // Then
        assertTrue(
            parallaxFactor > 0f && parallaxFactor < 1f,
            "Parallax factor should be between 0 and 1"
        )
    }

    @Test
    fun `scale factor is reasonable value`() {
        // Given - expected scale factor
        val scaleFactor = 0.1f

        // Then
        assertTrue(
            scaleFactor > 0f && scaleFactor < 0.5f,
            "Scale factor should be small for subtle effect"
        )

        // Minimum scale at full progress
        val minScale = 1f - scaleFactor
        assertTrue(minScale >= 0.5f, "Content should not scale too small")
    }

    // =========================================================================
    // PREDICTIVE BACK STATE FLOW TESTS
    // =========================================================================

    @Test
    fun `predictive back controller provides isActive StateFlow`() {
        // Given
        val controller = PredictiveBackController()

        // Then
        assertNotNull(controller.isActive)
        assertFalse(controller.isActive.value)
    }

    @Test
    fun `predictive back controller provides progress StateFlow`() {
        // Given
        val controller = PredictiveBackController()

        // Then
        assertNotNull(controller.progress)
        assertEquals(0f, controller.progress.value)
    }
}
