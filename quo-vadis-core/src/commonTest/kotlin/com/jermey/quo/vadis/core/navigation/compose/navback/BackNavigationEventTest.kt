@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.navback

import com.jermey.quo.vadis.core.compose.internal.navback.BackNavigationEvent
import com.jermey.quo.vadis.core.compose.internal.navback.BackTransitionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [BackNavigationEvent] and [BackTransitionState].
 *
 * Tests cover:
 * - BackNavigationEvent creation with all parameters
 * - BackNavigationEvent.EDGE_LEFT and EDGE_RIGHT constants
 * - BackTransitionState.Idle and BackTransitionState.InProgress states
 * - Data class equality and copy behavior
 */
class BackNavigationEventTest {

    // =========================================================================
    // BACK NAVIGATION EVENT TESTS
    // =========================================================================

    @Test
    fun `BackNavigationEvent creation with all parameters`() {
        // Given
        val progress = 0.5f
        val touchX = 100f
        val touchY = 200f
        val swipeEdge = BackNavigationEvent.EDGE_RIGHT

        // When
        val event = BackNavigationEvent(
            progress = progress,
            touchX = touchX,
            touchY = touchY,
            swipeEdge = swipeEdge
        )

        // Then
        assertEquals(progress, event.progress)
        assertEquals(touchX, event.touchX)
        assertEquals(touchY, event.touchY)
        assertEquals(swipeEdge, event.swipeEdge)
    }

    @Test
    fun `BackNavigationEvent creation with default values`() {
        // When
        val event = BackNavigationEvent(progress = 0.75f)

        // Then
        assertEquals(0.75f, event.progress)
        assertEquals(0f, event.touchX)
        assertEquals(0f, event.touchY)
        assertEquals(BackNavigationEvent.EDGE_LEFT, event.swipeEdge)
    }

    @Test
    fun `BackNavigationEvent progress at zero`() {
        // Given/When
        val event = BackNavigationEvent(progress = 0f)

        // Then
        assertEquals(0f, event.progress)
    }

    @Test
    fun `BackNavigationEvent progress at one`() {
        // Given/When
        val event = BackNavigationEvent(progress = 1f)

        // Then
        assertEquals(1f, event.progress)
    }

    @Test
    fun `BackNavigationEvent progress at intermediate value`() {
        // Given/When
        val event = BackNavigationEvent(progress = 0.33f)

        // Then
        assertEquals(0.33f, event.progress)
    }

    // =========================================================================
    // EDGE CONSTANT TESTS
    // =========================================================================

    @Test
    fun `EDGE_LEFT constant has value 0`() {
        assertEquals(0, BackNavigationEvent.EDGE_LEFT)
    }

    @Test
    fun `EDGE_RIGHT constant has value 1`() {
        assertEquals(1, BackNavigationEvent.EDGE_RIGHT)
    }

    @Test
    fun `EDGE_LEFT and EDGE_RIGHT are distinct`() {
        assertNotEquals(BackNavigationEvent.EDGE_LEFT, BackNavigationEvent.EDGE_RIGHT)
    }

    @Test
    fun `swipeEdge defaults to EDGE_LEFT`() {
        // Given/When
        val event = BackNavigationEvent(progress = 0f)

        // Then
        assertEquals(BackNavigationEvent.EDGE_LEFT, event.swipeEdge)
    }

    @Test
    fun `swipeEdge can be set to EDGE_RIGHT`() {
        // Given/When
        val event = BackNavigationEvent(
            progress = 0f,
            swipeEdge = BackNavigationEvent.EDGE_RIGHT
        )

        // Then
        assertEquals(BackNavigationEvent.EDGE_RIGHT, event.swipeEdge)
    }

    // =========================================================================
    // DATA CLASS BEHAVIOR TESTS
    // =========================================================================

    @Test
    fun `BackNavigationEvent equals works correctly`() {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)
        val event2 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)
        val event3 = BackNavigationEvent(progress = 0.6f, touchX = 10f, touchY = 20f)

        // Then
        assertEquals(event1, event2)
        assertNotEquals(event1, event3)
    }

    @Test
    fun `BackNavigationEvent copy works correctly`() {
        // Given
        val original = BackNavigationEvent(
            progress = 0.5f,
            touchX = 100f,
            touchY = 200f,
            swipeEdge = BackNavigationEvent.EDGE_LEFT
        )

        // When
        val copied = original.copy(progress = 0.75f)

        // Then
        assertEquals(0.75f, copied.progress)
        assertEquals(100f, copied.touchX)
        assertEquals(200f, copied.touchY)
        assertEquals(BackNavigationEvent.EDGE_LEFT, copied.swipeEdge)
    }

    @Test
    fun `BackNavigationEvent hashCode is consistent with equals`() {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)
        val event2 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)

        // Then
        assertEquals(event1.hashCode(), event2.hashCode())
    }

    // =========================================================================
    // BACK TRANSITION STATE TESTS - IDLE
    // =========================================================================

    @Test
    fun `BackTransitionState Idle is a data object`() {
        // Given/When
        val idle = BackTransitionState.Idle

        // Then
        assertIs<BackTransitionState>(idle)
    }

    @Test
    fun `BackTransitionState Idle is singleton`() {
        // Given
        val idle1 = BackTransitionState.Idle
        val idle2 = BackTransitionState.Idle

        // Then
        assertSame(idle1, idle2)
    }

    @Test
    fun `BackTransitionState Idle equals itself`() {
        // Given
        val idle1 = BackTransitionState.Idle
        val idle2 = BackTransitionState.Idle

        // Then
        assertEquals(idle1, idle2)
    }

    // =========================================================================
    // BACK TRANSITION STATE TESTS - IN PROGRESS
    // =========================================================================

    @Test
    fun `BackTransitionState InProgress contains event`() {
        // Given
        val event = BackNavigationEvent(progress = 0.5f)

        // When
        val inProgress = BackTransitionState.InProgress(event)

        // Then
        assertIs<BackTransitionState>(inProgress)
        assertEquals(event, inProgress.event)
    }

    @Test
    fun `BackTransitionState InProgress with different events are not equal`() {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f)
        val event2 = BackNavigationEvent(progress = 0.6f)

        // When
        val inProgress1 = BackTransitionState.InProgress(event1)
        val inProgress2 = BackTransitionState.InProgress(event2)

        // Then
        assertNotEquals(inProgress1, inProgress2)
    }

    @Test
    fun `BackTransitionState InProgress with same event are equal`() {
        // Given
        val event = BackNavigationEvent(progress = 0.5f)

        // When
        val inProgress1 = BackTransitionState.InProgress(event)
        val inProgress2 = BackTransitionState.InProgress(event.copy())

        // Then
        assertEquals(inProgress1, inProgress2)
    }

    @Test
    fun `BackTransitionState InProgress copy works correctly`() {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f)
        val event2 = BackNavigationEvent(progress = 0.75f)
        val original = BackTransitionState.InProgress(event1)

        // When
        val copied = original.copy(event = event2)

        // Then
        assertEquals(event2, copied.event)
    }

    // =========================================================================
    // BACK TRANSITION STATE TYPE CHECKING TESTS
    // =========================================================================

    @Test
    fun `BackTransitionState Idle is not InProgress`() {
        // Given
        val state: BackTransitionState = BackTransitionState.Idle

        // Then
        assertFalse(state is BackTransitionState.InProgress)
    }

    @Test
    fun `BackTransitionState InProgress is not Idle`() {
        // Given
        val event = BackNavigationEvent(progress = 0.5f)
        val state: BackTransitionState = BackTransitionState.InProgress(event)

        // Then
        assertTrue(state is BackTransitionState.InProgress)
    }

    @Test
    fun `when expression works with BackTransitionState`() {
        // Given
        val idleState: BackTransitionState = BackTransitionState.Idle
        val inProgressState: BackTransitionState = BackTransitionState.InProgress(
            BackNavigationEvent(progress = 0.5f)
        )

        // When
        val idleResult = when (idleState) {
            is BackTransitionState.Idle -> "idle"
            is BackTransitionState.InProgress -> "in-progress"
        }

        val inProgressResult = when (inProgressState) {
            is BackTransitionState.Idle -> "idle"
            is BackTransitionState.InProgress -> "in-progress"
        }

        // Then
        assertEquals("idle", idleResult)
        assertEquals("in-progress", inProgressResult)
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    @Test
    fun `BackNavigationEvent with negative touchX and touchY`() {
        // Given/When (edge case - shouldn't happen but testing robustness)
        val event = BackNavigationEvent(
            progress = 0.5f,
            touchX = -10f,
            touchY = -20f
        )

        // Then
        assertEquals(-10f, event.touchX)
        assertEquals(-20f, event.touchY)
    }

    @Test
    fun `BackNavigationEvent with very large touchX and touchY`() {
        // Given/When
        val event = BackNavigationEvent(
            progress = 0.5f,
            touchX = 10000f,
            touchY = 20000f
        )

        // Then
        assertEquals(10000f, event.touchX)
        assertEquals(20000f, event.touchY)
    }

    @Test
    fun `BackNavigationEvent progress can be negative`() {
        // Given/When (edge case - shouldn't happen but testing robustness)
        val event = BackNavigationEvent(progress = -0.1f)

        // Then
        assertEquals(-0.1f, event.progress)
    }

    @Test
    fun `BackNavigationEvent progress can exceed 1`() {
        // Given/When (edge case - shouldn't happen but testing robustness)
        val event = BackNavigationEvent(progress = 1.5f)

        // Then
        assertEquals(1.5f, event.progress)
    }
}
