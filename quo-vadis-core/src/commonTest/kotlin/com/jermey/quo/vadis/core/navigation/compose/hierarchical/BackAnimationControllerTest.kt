package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.navigation.compose.animation.BackAnimationController
import com.jermey.quo.vadis.core.navigation.compose.navback.BackNavigationEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [com.jermey.quo.vadis.core.navigation.compose.animation.BackAnimationController].
 *
 * Tests cover:
 * - Initial state (not animating, progress = 0)
 * - startAnimation() method
 * - updateProgress() method
 * - completeAnimation() method
 * - cancelAnimation() method
 * - State transitions
 */
class BackAnimationControllerTest {

    // =========================================================================
    // INITIAL STATE TESTS
    // =========================================================================

    @Test
    fun `initial state is not animating`() {
        // Given/When
        val controller = BackAnimationController()

        // Then
        assertFalse(controller.isAnimating)
    }

    @Test
    fun `initial progress is zero`() {
        // Given/When
        val controller = BackAnimationController()

        // Then
        assertEquals(0f, controller.progress)
    }

    @Test
    fun `initial currentEvent is null`() {
        // Given/When
        val controller = BackAnimationController()

        // Then
        assertNull(controller.currentEvent)
    }

    // =========================================================================
    // START ANIMATION TESTS
    // =========================================================================

    @Test
    fun `startAnimation sets isAnimating to true`() {
        // Given
        val controller = BackAnimationController()
        val event = BackNavigationEvent(progress = 0f)

        // When
        controller.startAnimation(event)

        // Then
        assertTrue(controller.isAnimating)
    }

    @Test
    fun `startAnimation sets progress from event`() {
        // Given
        val controller = BackAnimationController()
        val event = BackNavigationEvent(progress = 0.1f)

        // When
        controller.startAnimation(event)

        // Then
        assertEquals(0.1f, controller.progress)
    }

    @Test
    fun `startAnimation sets currentEvent`() {
        // Given
        val controller = BackAnimationController()
        val event = BackNavigationEvent(
            progress = 0.1f,
            touchX = 50f,
            touchY = 100f,
            swipeEdge = BackNavigationEvent.EDGE_LEFT
        )

        // When
        controller.startAnimation(event)

        // Then
        assertNotNull(controller.currentEvent)
        assertEquals(event, controller.currentEvent)
    }

    @Test
    fun `startAnimation preserves all event properties`() {
        // Given
        val controller = BackAnimationController()
        val event = BackNavigationEvent(
            progress = 0.2f,
            touchX = 75f,
            touchY = 150f,
            swipeEdge = BackNavigationEvent.EDGE_RIGHT
        )

        // When
        controller.startAnimation(event)

        // Then
        val storedEvent = controller.currentEvent
        assertNotNull(storedEvent)
        assertEquals(0.2f, storedEvent.progress)
        assertEquals(75f, storedEvent.touchX)
        assertEquals(150f, storedEvent.touchY)
        assertEquals(BackNavigationEvent.EDGE_RIGHT, storedEvent.swipeEdge)
    }

    // =========================================================================
    // UPDATE PROGRESS TESTS
    // =========================================================================

    @Test
    fun `updateProgress updates progress value`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))

        // Then
        assertEquals(0.5f, controller.progress)
    }

    @Test
    fun `updateProgress updates currentEvent`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f, touchX = 10f))

        // When
        val updatedEvent = BackNavigationEvent(progress = 0.5f, touchX = 100f)
        controller.updateProgress(updatedEvent)

        // Then
        assertEquals(updatedEvent, controller.currentEvent)
    }

    @Test
    fun `updateProgress works without prior startAnimation`() {
        // Given
        val controller = BackAnimationController()

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))

        // Then
        assertEquals(0.5f, controller.progress)
        assertNotNull(controller.currentEvent)
    }

    @Test
    fun `updateProgress can be called multiple times`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.25f))
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))
        controller.updateProgress(BackNavigationEvent(progress = 0.75f))

        // Then
        assertEquals(0.75f, controller.progress)
    }

    @Test
    fun `updateProgress preserves all event properties`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        val updatedEvent = BackNavigationEvent(
            progress = 0.6f,
            touchX = 200f,
            touchY = 300f,
            swipeEdge = BackNavigationEvent.EDGE_RIGHT
        )
        controller.updateProgress(updatedEvent)

        // Then
        val storedEvent = controller.currentEvent
        assertNotNull(storedEvent)
        assertEquals(200f, storedEvent.touchX)
        assertEquals(300f, storedEvent.touchY)
        assertEquals(BackNavigationEvent.EDGE_RIGHT, storedEvent.swipeEdge)
    }

    // =========================================================================
    // COMPLETE ANIMATION TESTS
    // =========================================================================

    @Test
    fun `completeAnimation sets isAnimating to false`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.completeAnimation()

        // Then
        assertFalse(controller.isAnimating)
    }

    @Test
    fun `completeAnimation resets progress to zero`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.9f))

        // When
        controller.completeAnimation()

        // Then
        assertEquals(0f, controller.progress)
    }

    @Test
    fun `completeAnimation sets currentEvent to null`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.completeAnimation()

        // Then
        assertNull(controller.currentEvent)
    }

    @Test
    fun `completeAnimation works when already not animating`() {
        // Given
        val controller = BackAnimationController()

        // When (no exception should be thrown)
        controller.completeAnimation()

        // Then
        assertFalse(controller.isAnimating)
        assertEquals(0f, controller.progress)
        assertNull(controller.currentEvent)
    }

    // =========================================================================
    // CANCEL ANIMATION TESTS
    // =========================================================================

    @Test
    fun `cancelAnimation sets isAnimating to false`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.cancelAnimation()

        // Then
        assertFalse(controller.isAnimating)
    }

    @Test
    fun `cancelAnimation resets progress to zero`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.4f))

        // When
        controller.cancelAnimation()

        // Then
        assertEquals(0f, controller.progress)
    }

    @Test
    fun `cancelAnimation sets currentEvent to null`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.cancelAnimation()

        // Then
        assertNull(controller.currentEvent)
    }

    @Test
    fun `cancelAnimation works when already not animating`() {
        // Given
        val controller = BackAnimationController()

        // When (no exception should be thrown)
        controller.cancelAnimation()

        // Then
        assertFalse(controller.isAnimating)
        assertEquals(0f, controller.progress)
        assertNull(controller.currentEvent)
    }

    // =========================================================================
    // STATE TRANSITION TESTS
    // =========================================================================

    @Test
    fun `full gesture lifecycle - start to complete`() {
        // Given
        val controller = BackAnimationController()

        // Initial state
        assertFalse(controller.isAnimating)
        assertEquals(0f, controller.progress)
        assertNull(controller.currentEvent)

        // Start
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        assertTrue(controller.isAnimating)
        assertEquals(0f, controller.progress)
        assertNotNull(controller.currentEvent)

        // Progress updates
        controller.updateProgress(BackNavigationEvent(progress = 0.33f))
        assertEquals(0.33f, controller.progress)

        controller.updateProgress(BackNavigationEvent(progress = 0.66f))
        assertEquals(0.66f, controller.progress)

        controller.updateProgress(BackNavigationEvent(progress = 1f))
        assertEquals(1f, controller.progress)

        // Complete
        controller.completeAnimation()
        assertFalse(controller.isAnimating)
        assertEquals(0f, controller.progress)
        assertNull(controller.currentEvent)
    }

    @Test
    fun `full gesture lifecycle - start to cancel`() {
        // Given
        val controller = BackAnimationController()

        // Start
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        assertTrue(controller.isAnimating)

        // Progress updates
        controller.updateProgress(BackNavigationEvent(progress = 0.2f))
        controller.updateProgress(BackNavigationEvent(progress = 0.3f))

        // Cancel (user released before threshold)
        controller.cancelAnimation()
        assertFalse(controller.isAnimating)
        assertEquals(0f, controller.progress)
        assertNull(controller.currentEvent)
    }

    @Test
    fun `can restart animation after completion`() {
        // Given
        val controller = BackAnimationController()

        // First gesture
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 1f))
        controller.completeAnimation()

        // Second gesture
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        assertTrue(controller.isAnimating)
        assertNotNull(controller.currentEvent)
    }

    @Test
    fun `can restart animation after cancellation`() {
        // Given
        val controller = BackAnimationController()

        // First gesture (cancelled)
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.2f))
        controller.cancelAnimation()

        // Second gesture
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        assertTrue(controller.isAnimating)
        assertNotNull(controller.currentEvent)
    }

    @Test
    fun `calling startAnimation while already animating restarts animation`() {
        // Given
        val controller = BackAnimationController()
        val firstEvent = BackNavigationEvent(
            progress = 0.5f,
            touchX = 100f
        )
        val secondEvent = BackNavigationEvent(
            progress = 0.1f,
            touchX = 200f
        )

        controller.startAnimation(firstEvent)
        assertEquals(0.5f, controller.progress)
        assertEquals(100f, controller.currentEvent?.touchX)

        // When - start new animation without completing first
        controller.startAnimation(secondEvent)

        // Then - new animation state
        assertTrue(controller.isAnimating)
        assertEquals(0.1f, controller.progress)
        assertEquals(200f, controller.currentEvent?.touchX)
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    fun `progress values near zero are handled correctly`() {
        // Given
        val controller = BackAnimationController()

        // When
        controller.startAnimation(BackNavigationEvent(progress = 0.001f))

        // Then
        assertEquals(0.001f, controller.progress)
    }

    @Test
    fun `progress values near one are handled correctly`() {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.999f))

        // Then
        assertEquals(0.999f, controller.progress)
    }

    @Test
    fun `rapid state changes are handled correctly`() {
        // Given
        val controller = BackAnimationController()

        // Rapid start/cancel cycles
        repeat(10) {
            controller.startAnimation(BackNavigationEvent(progress = 0f))
            controller.cancelAnimation()
        }

        // Then - still in clean state
        assertFalse(controller.isAnimating)
        assertEquals(0f, controller.progress)
        assertNull(controller.currentEvent)
    }

    @Test
    fun `different swipe edges are tracked correctly`() {
        // Given
        val controller = BackAnimationController()

        // Left edge gesture
        controller.startAnimation(
            BackNavigationEvent(
                progress = 0f,
                swipeEdge = BackNavigationEvent.EDGE_LEFT
            )
        )
        assertEquals(BackNavigationEvent.EDGE_LEFT, controller.currentEvent?.swipeEdge)
        controller.cancelAnimation()

        // Right edge gesture
        controller.startAnimation(
            BackNavigationEvent(
                progress = 0f,
                swipeEdge = BackNavigationEvent.EDGE_RIGHT
            )
        )
        assertEquals(BackNavigationEvent.EDGE_RIGHT, controller.currentEvent?.swipeEdge)
    }

    // =========================================================================
    // ANIMATION PROGRESS INTERPOLATION SCENARIOS
    // =========================================================================

    @Test
    fun `typical gesture progress sequence`() {
        // Given
        val controller = BackAnimationController()
        val progressValues = listOf(0f, 0.1f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.9f, 1f)

        // When
        controller.startAnimation(BackNavigationEvent(progress = progressValues[0]))
        for (i in 1 until progressValues.size) {
            controller.updateProgress(BackNavigationEvent(progress = progressValues[i]))
            assertEquals(progressValues[i], controller.progress)
        }

        // Then - final state
        assertEquals(1f, controller.progress)
        assertTrue(controller.isAnimating)
    }

    @Test
    fun `gesture with backwards progress - dragging back`() {
        // Given
        val controller = BackAnimationController()

        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))
        
        // When - user drags finger back
        controller.updateProgress(BackNavigationEvent(progress = 0.3f))

        // Then - progress can decrease
        assertEquals(0.3f, controller.progress)
    }
}
