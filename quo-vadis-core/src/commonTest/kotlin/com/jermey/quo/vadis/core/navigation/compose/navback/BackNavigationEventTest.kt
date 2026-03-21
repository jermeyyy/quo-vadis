package com.jermey.quo.vadis.core.navigation.compose.navback

import com.jermey.quo.vadis.core.compose.internal.navback.BackNavigationEvent
import com.jermey.quo.vadis.core.compose.internal.navback.BackTransitionState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unit tests for [BackNavigationEvent] and [BackTransitionState].
 *
 * Tests cover:
 * - BackNavigationEvent creation with all parameters
 * - BackNavigationEvent.EDGE_LEFT and EDGE_RIGHT constants
 * - BackTransitionState.Idle and BackTransitionState.InProgress states
 * - Data class equality and copy behavior
 */
class BackNavigationEventTest : FunSpec({

    // =========================================================================
    // BACK NAVIGATION EVENT TESTS
    // =========================================================================

    test("BackNavigationEvent creation with all parameters") {
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
        event.progress shouldBe progress
        event.touchX shouldBe touchX
        event.touchY shouldBe touchY
        event.swipeEdge shouldBe swipeEdge
    }

    test("BackNavigationEvent creation with default values") {
        // When
        val event = BackNavigationEvent(progress = 0.75f)

        // Then
        event.progress shouldBe 0.75f
        event.touchX shouldBe 0f
        event.touchY shouldBe 0f
        event.swipeEdge shouldBe BackNavigationEvent.EDGE_LEFT
    }

    test("BackNavigationEvent progress at zero") {
        // Given/When
        val event = BackNavigationEvent(progress = 0f)

        // Then
        event.progress shouldBe 0f
    }

    test("BackNavigationEvent progress at one") {
        // Given/When
        val event = BackNavigationEvent(progress = 1f)

        // Then
        event.progress shouldBe 1f
    }

    test("BackNavigationEvent progress at intermediate value") {
        // Given/When
        val event = BackNavigationEvent(progress = 0.33f)

        // Then
        event.progress shouldBe 0.33f
    }

    // =========================================================================
    // EDGE CONSTANT TESTS
    // =========================================================================

    test("EDGE_LEFT constant has value 0") {
        BackNavigationEvent.EDGE_LEFT shouldBe 0
    }

    test("EDGE_RIGHT constant has value 1") {
        BackNavigationEvent.EDGE_RIGHT shouldBe 1
    }

    test("EDGE_LEFT and EDGE_RIGHT are distinct") {
        BackNavigationEvent.EDGE_RIGHT shouldNotBe BackNavigationEvent.EDGE_LEFT
    }

    test("swipeEdge defaults to EDGE_LEFT") {
        // Given/When
        val event = BackNavigationEvent(progress = 0f)

        // Then
        event.swipeEdge shouldBe BackNavigationEvent.EDGE_LEFT
    }

    test("swipeEdge can be set to EDGE_RIGHT") {
        // Given/When
        val event = BackNavigationEvent(
            progress = 0f,
            swipeEdge = BackNavigationEvent.EDGE_RIGHT
        )

        // Then
        event.swipeEdge shouldBe BackNavigationEvent.EDGE_RIGHT
    }

    // =========================================================================
    // DATA CLASS BEHAVIOR TESTS
    // =========================================================================

    test("BackNavigationEvent equals works correctly") {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)
        val event2 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)
        val event3 = BackNavigationEvent(progress = 0.6f, touchX = 10f, touchY = 20f)

        // Then
        event2 shouldBe event1
        event3 shouldNotBe event1
    }

    test("BackNavigationEvent copy works correctly") {
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
        copied.progress shouldBe 0.75f
        copied.touchX shouldBe 100f
        copied.touchY shouldBe 200f
        copied.swipeEdge shouldBe BackNavigationEvent.EDGE_LEFT
    }

    test("BackNavigationEvent hashCode is consistent with equals") {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)
        val event2 = BackNavigationEvent(progress = 0.5f, touchX = 10f, touchY = 20f)

        // Then
        event2.hashCode() shouldBe event1.hashCode()
    }

    // =========================================================================
    // BACK TRANSITION STATE TESTS - IDLE
    // =========================================================================

    test("BackTransitionState Idle is a data object") {
        // Given/When
        val idle = BackTransitionState.Idle

        // Then
        idle.shouldBeInstanceOf<BackTransitionState>()
    }

    test("BackTransitionState Idle is singleton") {
        // Given
        val idle1 = BackTransitionState.Idle
        val idle2 = BackTransitionState.Idle

        // Then
        idle2 shouldBeSameInstanceAs idle1
    }

    test("BackTransitionState Idle equals itself") {
        // Given
        val idle1 = BackTransitionState.Idle
        val idle2 = BackTransitionState.Idle

        // Then
        idle2 shouldBe idle1
    }

    // =========================================================================
    // BACK TRANSITION STATE TESTS - IN PROGRESS
    // =========================================================================

    test("BackTransitionState InProgress contains event") {
        // Given
        val event = BackNavigationEvent(progress = 0.5f)

        // When
        val inProgress = BackTransitionState.InProgress(event)

        // Then
        inProgress.shouldBeInstanceOf<BackTransitionState>()
        inProgress.event shouldBe event
    }

    test("BackTransitionState InProgress with different events are not equal") {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f)
        val event2 = BackNavigationEvent(progress = 0.6f)

        // When
        val inProgress1 = BackTransitionState.InProgress(event1)
        val inProgress2 = BackTransitionState.InProgress(event2)

        // Then
        inProgress2 shouldNotBe inProgress1
    }

    test("BackTransitionState InProgress with same event are equal") {
        // Given
        val event = BackNavigationEvent(progress = 0.5f)

        // When
        val inProgress1 = BackTransitionState.InProgress(event)
        val inProgress2 = BackTransitionState.InProgress(event.copy())

        // Then
        inProgress2 shouldBe inProgress1
    }

    test("BackTransitionState InProgress copy works correctly") {
        // Given
        val event1 = BackNavigationEvent(progress = 0.5f)
        val event2 = BackNavigationEvent(progress = 0.75f)
        val original = BackTransitionState.InProgress(event1)

        // When
        val copied = original.copy(event = event2)

        // Then
        copied.event shouldBe event2
    }

    // =========================================================================
    // BACK TRANSITION STATE TYPE CHECKING TESTS
    // =========================================================================

    test("BackTransitionState Idle is not InProgress") {
        // Given
        val state: BackTransitionState = BackTransitionState.Idle

        // Then
        (state is BackTransitionState.InProgress).shouldBeFalse()
    }

    test("BackTransitionState InProgress is not Idle") {
        // Given
        val event = BackNavigationEvent(progress = 0.5f)
        val state: BackTransitionState = BackTransitionState.InProgress(event)

        // Then
        state.shouldBeInstanceOf<BackTransitionState.InProgress>()
    }

    test("when expression works with BackTransitionState") {
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
        idleResult shouldBe "idle"
        inProgressResult shouldBe "in-progress"
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    test("BackNavigationEvent with negative touchX and touchY") {
        // Given/When (edge case - shouldn't happen but testing robustness)
        val event = BackNavigationEvent(
            progress = 0.5f,
            touchX = -10f,
            touchY = -20f
        )

        // Then
        event.touchX shouldBe -10f
        event.touchY shouldBe -20f
    }

    test("BackNavigationEvent with very large touchX and touchY") {
        // Given/When
        val event = BackNavigationEvent(
            progress = 0.5f,
            touchX = 10000f,
            touchY = 20000f
        )

        // Then
        event.touchX shouldBe 10000f
        event.touchY shouldBe 20000f
    }

    test("BackNavigationEvent progress can be negative") {
        // Given/When (edge case - shouldn't happen but testing robustness)
        val event = BackNavigationEvent(progress = -0.1f)

        // Then
        event.progress shouldBe -0.1f
    }

    test("BackNavigationEvent progress can exceed 1") {
        // Given/When (edge case - shouldn't happen but testing robustness)
        val event = BackNavigationEvent(progress = 1.5f)

        // Then
        event.progress shouldBe 1.5f
    }
})
