package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for TransitionState sealed interface.
 *
 * Tests cover:
 * - TransitionState.Idle: singleton behavior, properties
 * - TransitionState.InProgress: creation, validation, properties
 * - TransitionState.PredictiveBack: creation, validation, shouldComplete logic
 * - TransitionState.Seeking: creation, validation, properties
 * - Convenience extension properties: isAnimating, progress
 */
class TransitionStateTest {

    // =========================================================================
    // TEST TRANSITIONS
    // =========================================================================

    private object TestTransition : NavigationTransition {
        override val enter: EnterTransition = EnterTransition.None
        override val exit: ExitTransition = ExitTransition.None
        override val popEnter: EnterTransition = EnterTransition.None
        override val popExit: ExitTransition = ExitTransition.None
    }

    // =========================================================================
    // IDLE STATE TESTS
    // =========================================================================

    @Test
    fun `Idle is a singleton`() {
        val idle1 = TransitionState.Idle
        val idle2 = TransitionState.Idle

        assertTrue(idle1 === idle2)
    }

    @Test
    fun `Idle isAnimating returns false`() {
        val idle = TransitionState.Idle

        assertFalse(idle.isAnimating)
    }

    @Test
    fun `Idle progress returns 0`() {
        val idle = TransitionState.Idle

        assertEquals(0f, idle.progress)
    }

    // =========================================================================
    // IN PROGRESS STATE TESTS
    // =========================================================================

    @Test
    fun `InProgress creation with valid parameters`() {
        val state = TransitionState.InProgress(
            transition = TestTransition,
            progress = 0.5f,
            fromKey = "from-key",
            toKey = "to-key"
        )

        assertEquals(TestTransition, state.transition)
        assertEquals(0.5f, state.progress)
        assertEquals("from-key", state.fromKey)
        assertEquals("to-key", state.toKey)
    }

    @Test
    fun `InProgress with default parameters`() {
        val state = TransitionState.InProgress(transition = TestTransition)

        assertEquals(0f, state.progress)
        assertNull(state.fromKey)
        assertNull(state.toKey)
    }

    @Test
    fun `InProgress progress at minimum bound`() {
        val state = TransitionState.InProgress(
            transition = TestTransition,
            progress = 0f
        )

        assertEquals(0f, state.progress)
    }

    @Test
    fun `InProgress progress at maximum bound`() {
        val state = TransitionState.InProgress(
            transition = TestTransition,
            progress = 1f
        )

        assertEquals(1f, state.progress)
    }

    @Test
    fun `InProgress throws for negative progress`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.InProgress(
                transition = TestTransition,
                progress = -0.1f
            )
        }
    }

    @Test
    fun `InProgress throws for progress greater than 1`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.InProgress(
                transition = TestTransition,
                progress = 1.1f
            )
        }
    }

    @Test
    fun `InProgress isAnimating returns true`() {
        val state = TransitionState.InProgress(transition = TestTransition)

        assertTrue(state.isAnimating)
    }

    @Test
    fun `InProgress progress extension returns correct value`() {
        val state: TransitionState = TransitionState.InProgress(
            transition = TestTransition,
            progress = 0.75f
        )

        assertEquals(0.75f, state.progress)
    }

    @Test
    fun `InProgress equality based on properties`() {
        val state1 = TransitionState.InProgress(
            transition = TestTransition,
            progress = 0.5f,
            fromKey = "from",
            toKey = "to"
        )
        val state2 = TransitionState.InProgress(
            transition = TestTransition,
            progress = 0.5f,
            fromKey = "from",
            toKey = "to"
        )

        assertEquals(state1, state2)
    }

    @Test
    fun `InProgress copy works correctly`() {
        val original = TransitionState.InProgress(
            transition = TestTransition,
            progress = 0.3f,
            fromKey = "from",
            toKey = "to"
        )

        val updated = original.copy(progress = 0.8f)

        assertEquals(0.8f, updated.progress)
        assertEquals("from", updated.fromKey)
        assertEquals("to", updated.toKey)
    }

    // =========================================================================
    // PREDICTIVE BACK STATE TESTS
    // =========================================================================

    @Test
    fun `PredictiveBack creation with valid parameters`() {
        val state = TransitionState.PredictiveBack(
            progress = 0.5f,
            currentKey = "current",
            previousKey = "previous",
            touchX = 0.3f,
            touchY = 0.7f,
            isCommitted = false
        )

        assertEquals(0.5f, state.progress)
        assertEquals("current", state.currentKey)
        assertEquals("previous", state.previousKey)
        assertEquals(0.3f, state.touchX)
        assertEquals(0.7f, state.touchY)
        assertFalse(state.isCommitted)
    }

    @Test
    fun `PredictiveBack with default parameters`() {
        val state = TransitionState.PredictiveBack(progress = 0f)

        assertEquals(0f, state.progress)
        assertNull(state.currentKey)
        assertNull(state.previousKey)
        assertEquals(0f, state.touchX)
        assertEquals(0f, state.touchY)
        assertFalse(state.isCommitted)
    }

    @Test
    fun `PredictiveBack progress at boundary values`() {
        val stateMin = TransitionState.PredictiveBack(progress = 0f)
        val stateMax = TransitionState.PredictiveBack(progress = 1f)

        assertEquals(0f, stateMin.progress)
        assertEquals(1f, stateMax.progress)
    }

    @Test
    fun `PredictiveBack throws for negative progress`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.PredictiveBack(progress = -0.01f)
        }
    }

    @Test
    fun `PredictiveBack throws for progress greater than 1`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.PredictiveBack(progress = 1.01f)
        }
    }

    @Test
    fun `PredictiveBack throws for negative touchX`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.PredictiveBack(
                progress = 0.5f,
                touchX = -0.1f
            )
        }
    }

    @Test
    fun `PredictiveBack throws for touchX greater than 1`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.PredictiveBack(
                progress = 0.5f,
                touchX = 1.1f
            )
        }
    }

    @Test
    fun `PredictiveBack throws for negative touchY`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.PredictiveBack(
                progress = 0.5f,
                touchY = -0.1f
            )
        }
    }

    @Test
    fun `PredictiveBack throws for touchY greater than 1`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.PredictiveBack(
                progress = 0.5f,
                touchY = 1.1f
            )
        }
    }

    @Test
    fun `PredictiveBack touchX and touchY at boundary values`() {
        val state = TransitionState.PredictiveBack(
            progress = 0.5f,
            touchX = 1f,
            touchY = 1f
        )

        assertEquals(1f, state.touchX)
        assertEquals(1f, state.touchY)
    }

    @Test
    fun `PredictiveBack shouldComplete returns false below threshold`() {
        val state = TransitionState.PredictiveBack(progress = 0.1f)

        assertFalse(state.shouldComplete())
        assertFalse(state.shouldComplete(0.2f))
    }

    @Test
    fun `PredictiveBack shouldComplete returns true at threshold`() {
        val state = TransitionState.PredictiveBack(progress = 0.2f)

        assertTrue(state.shouldComplete()) // Default threshold is 0.2
    }

    @Test
    fun `PredictiveBack shouldComplete returns true above threshold`() {
        val state = TransitionState.PredictiveBack(progress = 0.5f)

        assertTrue(state.shouldComplete())
    }

    @Test
    fun `PredictiveBack shouldComplete with custom threshold`() {
        val state = TransitionState.PredictiveBack(progress = 0.4f)

        assertFalse(state.shouldComplete(0.5f))
        assertTrue(state.shouldComplete(0.3f))
    }

    @Test
    fun `PredictiveBack shouldComplete returns true when committed regardless of progress`() {
        val state = TransitionState.PredictiveBack(
            progress = 0.05f,
            isCommitted = true
        )

        assertTrue(state.shouldComplete())
    }

    @Test
    fun `PredictiveBack isAnimating returns true`() {
        val state = TransitionState.PredictiveBack(progress = 0.5f)

        assertTrue(state.isAnimating)
    }

    @Test
    fun `PredictiveBack progress extension returns correct value`() {
        val state: TransitionState = TransitionState.PredictiveBack(progress = 0.6f)

        assertEquals(0.6f, state.progress)
    }

    @Test
    fun `PredictiveBack copy works correctly`() {
        val original = TransitionState.PredictiveBack(
            progress = 0.3f,
            currentKey = "current",
            previousKey = "previous"
        )

        val updated = original.copy(progress = 0.7f, isCommitted = true)

        assertEquals(0.7f, updated.progress)
        assertEquals("current", updated.currentKey)
        assertEquals("previous", updated.previousKey)
        assertTrue(updated.isCommitted)
    }

    // =========================================================================
    // SEEKING STATE TESTS
    // =========================================================================

    @Test
    fun `Seeking creation with valid parameters`() {
        val state = TransitionState.Seeking(
            transition = TestTransition,
            progress = 0.5f,
            isPaused = true
        )

        assertEquals(TestTransition, state.transition)
        assertEquals(0.5f, state.progress)
        assertTrue(state.isPaused)
    }

    @Test
    fun `Seeking with default parameters`() {
        val state = TransitionState.Seeking(
            transition = TestTransition,
            progress = 0.5f
        )

        assertFalse(state.isPaused)
    }

    @Test
    fun `Seeking progress at boundary values`() {
        val stateMin = TransitionState.Seeking(transition = TestTransition, progress = 0f)
        val stateMax = TransitionState.Seeking(transition = TestTransition, progress = 1f)

        assertEquals(0f, stateMin.progress)
        assertEquals(1f, stateMax.progress)
    }

    @Test
    fun `Seeking throws for negative progress`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.Seeking(
                transition = TestTransition,
                progress = -0.01f
            )
        }
    }

    @Test
    fun `Seeking throws for progress greater than 1`() {
        assertFailsWith<IllegalArgumentException> {
            TransitionState.Seeking(
                transition = TestTransition,
                progress = 1.01f
            )
        }
    }

    @Test
    fun `Seeking isAnimating returns true`() {
        val state = TransitionState.Seeking(
            transition = TestTransition,
            progress = 0.5f
        )

        assertTrue(state.isAnimating)
    }

    @Test
    fun `Seeking progress extension returns correct value`() {
        val state: TransitionState = TransitionState.Seeking(
            transition = TestTransition,
            progress = 0.45f
        )

        assertEquals(0.45f, state.progress)
    }

    @Test
    fun `Seeking copy works correctly`() {
        val original = TransitionState.Seeking(
            transition = TestTransition,
            progress = 0.3f,
            isPaused = false
        )

        val updated = original.copy(progress = 0.8f, isPaused = true)

        assertEquals(0.8f, updated.progress)
        assertTrue(updated.isPaused)
    }

    // =========================================================================
    // TYPE CHECKING TESTS
    // =========================================================================

    @Test
    fun `TransitionState types can be pattern matched`() {
        val states = listOf<TransitionState>(
            TransitionState.Idle,
            TransitionState.InProgress(transition = TestTransition),
            TransitionState.PredictiveBack(progress = 0.5f),
            TransitionState.Seeking(transition = TestTransition, progress = 0.5f)
        )

        var idleCount = 0
        var inProgressCount = 0
        var predictiveBackCount = 0
        var seekingCount = 0

        for (state in states) {
            when (state) {
                is TransitionState.Idle -> idleCount++
                is TransitionState.InProgress -> inProgressCount++
                is TransitionState.PredictiveBack -> predictiveBackCount++
                is TransitionState.Seeking -> seekingCount++
            }
        }

        assertEquals(1, idleCount)
        assertEquals(1, inProgressCount)
        assertEquals(1, predictiveBackCount)
        assertEquals(1, seekingCount)
    }

    @Test
    fun `isAnimating property distinguishes Idle from animating states`() {
        val idle = TransitionState.Idle
        val inProgress = TransitionState.InProgress(transition = TestTransition)
        val predictiveBack = TransitionState.PredictiveBack(progress = 0.5f)
        val seeking = TransitionState.Seeking(transition = TestTransition, progress = 0.5f)

        assertFalse(idle.isAnimating)
        assertTrue(inProgress.isAnimating)
        assertTrue(predictiveBack.isAnimating)
        assertTrue(seeking.isAnimating)
    }

    @Test
    fun `progress property works across all state types`() {
        val idle: TransitionState = TransitionState.Idle
        val inProgress: TransitionState = TransitionState.InProgress(
            transition = TestTransition,
            progress = 0.3f
        )
        val predictiveBack: TransitionState = TransitionState.PredictiveBack(progress = 0.5f)
        val seeking: TransitionState = TransitionState.Seeking(
            transition = TestTransition,
            progress = 0.7f
        )

        assertEquals(0f, idle.progress)
        assertEquals(0.3f, inProgress.progress)
        assertEquals(0.5f, predictiveBack.progress)
        assertEquals(0.7f, seeking.progress)
    }
}
