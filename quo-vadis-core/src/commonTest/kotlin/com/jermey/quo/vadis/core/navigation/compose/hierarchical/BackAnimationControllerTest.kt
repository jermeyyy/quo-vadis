package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.internal.BackAnimationController
import com.jermey.quo.vadis.core.compose.internal.navback.BackNavigationEvent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

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
@OptIn(InternalQuoVadisApi::class)
class BackAnimationControllerTest : FunSpec({

    // =========================================================================
    // INITIAL STATE TESTS
    // =========================================================================

    test("initial state is not animating") {
        // Given/When
        val controller = BackAnimationController()

        // Then
        controller.isAnimating.shouldBeFalse()
    }

    test("initial progress is zero") {
        // Given/When
        val controller = BackAnimationController()

        // Then
        controller.progress shouldBe 0f
    }

    test("initial currentEvent is null") {
        // Given/When
        val controller = BackAnimationController()

        // Then
        controller.currentEvent.shouldBeNull()
    }

    // =========================================================================
    // START ANIMATION TESTS
    // =========================================================================

    test("startAnimation sets isAnimating to true") {
        // Given
        val controller = BackAnimationController()
        val event = BackNavigationEvent(progress = 0f)

        // When
        controller.startAnimation(event)

        // Then
        controller.isAnimating.shouldBeTrue()
    }

    test("startAnimation sets progress from event") {
        // Given
        val controller = BackAnimationController()
        val event = BackNavigationEvent(progress = 0.1f)

        // When
        controller.startAnimation(event)

        // Then
        controller.progress shouldBe 0.1f
    }

    test("startAnimation sets currentEvent") {
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
        controller.currentEvent.shouldNotBeNull()
        controller.currentEvent shouldBe event
    }

    test("startAnimation preserves all event properties") {
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
        storedEvent.shouldNotBeNull()
        storedEvent.progress shouldBe 0.2f
        storedEvent.touchX shouldBe 75f
        storedEvent.touchY shouldBe 150f
        storedEvent.swipeEdge shouldBe BackNavigationEvent.EDGE_RIGHT
    }

    // =========================================================================
    // UPDATE PROGRESS TESTS
    // =========================================================================

    test("updateProgress updates progress value") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))

        // Then
        controller.progress shouldBe 0.5f
    }

    test("updateProgress updates currentEvent") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f, touchX = 10f))

        // When
        val updatedEvent = BackNavigationEvent(progress = 0.5f, touchX = 100f)
        controller.updateProgress(updatedEvent)

        // Then
        controller.currentEvent shouldBe updatedEvent
    }

    test("updateProgress works without prior startAnimation") {
        // Given
        val controller = BackAnimationController()

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))

        // Then
        controller.progress shouldBe 0.5f
        controller.currentEvent.shouldNotBeNull()
    }

    test("updateProgress can be called multiple times") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.25f))
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))
        controller.updateProgress(BackNavigationEvent(progress = 0.75f))

        // Then
        controller.progress shouldBe 0.75f
    }

    test("updateProgress preserves all event properties") {
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
        storedEvent.shouldNotBeNull()
        storedEvent.touchX shouldBe 200f
        storedEvent.touchY shouldBe 300f
        storedEvent.swipeEdge shouldBe BackNavigationEvent.EDGE_RIGHT
    }

    // =========================================================================
    // COMPLETE ANIMATION TESTS
    // =========================================================================

    test("completeAnimation sets isAnimating to false") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.completeAnimation()

        // Then
        controller.isAnimating.shouldBeFalse()
    }

    test("completeAnimation resets progress to zero") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.9f))

        // When
        controller.completeAnimation()

        // Then
        controller.progress shouldBe 0f
    }

    test("completeAnimation sets currentEvent to null") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.completeAnimation()

        // Then
        controller.currentEvent.shouldBeNull()
    }

    test("completeAnimation works when already not animating") {
        // Given
        val controller = BackAnimationController()

        // When (no exception should be thrown)
        controller.completeAnimation()

        // Then
        controller.isAnimating.shouldBeFalse()
        controller.progress shouldBe 0f
        controller.currentEvent.shouldBeNull()
    }

    // =========================================================================
    // CANCEL ANIMATION TESTS
    // =========================================================================

    test("cancelAnimation sets isAnimating to false") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.cancelAnimation()

        // Then
        controller.isAnimating.shouldBeFalse()
    }

    test("cancelAnimation resets progress to zero") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.4f))

        // When
        controller.cancelAnimation()

        // Then
        controller.progress shouldBe 0f
    }

    test("cancelAnimation sets currentEvent to null") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.cancelAnimation()

        // Then
        controller.currentEvent.shouldBeNull()
    }

    test("cancelAnimation works when already not animating") {
        // Given
        val controller = BackAnimationController()

        // When (no exception should be thrown)
        controller.cancelAnimation()

        // Then
        controller.isAnimating.shouldBeFalse()
        controller.progress shouldBe 0f
        controller.currentEvent.shouldBeNull()
    }

    // =========================================================================
    // STATE TRANSITION TESTS
    // =========================================================================

    test("full gesture lifecycle - start to complete") {
        // Given
        val controller = BackAnimationController()

        // Initial state
        controller.isAnimating.shouldBeFalse()
        controller.progress shouldBe 0f
        controller.currentEvent.shouldBeNull()

        // Start
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.isAnimating.shouldBeTrue()
        controller.progress shouldBe 0f
        controller.currentEvent.shouldNotBeNull()

        // Progress updates
        controller.updateProgress(BackNavigationEvent(progress = 0.33f))
        controller.progress shouldBe 0.33f

        controller.updateProgress(BackNavigationEvent(progress = 0.66f))
        controller.progress shouldBe 0.66f

        controller.updateProgress(BackNavigationEvent(progress = 1f))
        controller.progress shouldBe 1f

        // Complete
        controller.completeAnimation()
        controller.isAnimating.shouldBeFalse()
        controller.progress shouldBe 0f
        controller.currentEvent.shouldBeNull()
    }

    test("full gesture lifecycle - start to cancel") {
        // Given
        val controller = BackAnimationController()

        // Start
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.isAnimating.shouldBeTrue()

        // Progress updates
        controller.updateProgress(BackNavigationEvent(progress = 0.2f))
        controller.updateProgress(BackNavigationEvent(progress = 0.3f))

        // Cancel (user released before threshold)
        controller.cancelAnimation()
        controller.isAnimating.shouldBeFalse()
        controller.progress shouldBe 0f
        controller.currentEvent.shouldBeNull()
    }

    test("can restart animation after completion") {
        // Given
        val controller = BackAnimationController()

        // First gesture
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 1f))
        controller.completeAnimation()

        // Second gesture
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.isAnimating.shouldBeTrue()
        controller.currentEvent.shouldNotBeNull()
    }

    test("can restart animation after cancellation") {
        // Given
        val controller = BackAnimationController()

        // First gesture (cancelled)
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.2f))
        controller.cancelAnimation()

        // Second gesture
        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.isAnimating.shouldBeTrue()
        controller.currentEvent.shouldNotBeNull()
    }

    test("calling startAnimation while already animating restarts animation") {
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
        controller.progress shouldBe 0.5f
        controller.currentEvent?.touchX shouldBe 100f

        // When - start new animation without completing first
        controller.startAnimation(secondEvent)

        // Then - new animation state
        controller.isAnimating.shouldBeTrue()
        controller.progress shouldBe 0.1f
        controller.currentEvent?.touchX shouldBe 200f
    }

    // =========================================================================
    // EDGE CASE TESTS
    // =========================================================================

    test("progress values near zero are handled correctly") {
        // Given
        val controller = BackAnimationController()

        // When
        controller.startAnimation(BackNavigationEvent(progress = 0.001f))

        // Then
        controller.progress shouldBe 0.001f
    }

    test("progress values near one are handled correctly") {
        // Given
        val controller = BackAnimationController()
        controller.startAnimation(BackNavigationEvent(progress = 0f))

        // When
        controller.updateProgress(BackNavigationEvent(progress = 0.999f))

        // Then
        controller.progress shouldBe 0.999f
    }

    test("rapid state changes are handled correctly") {
        // Given
        val controller = BackAnimationController()

        // Rapid start/cancel cycles
        repeat(10) {
            controller.startAnimation(BackNavigationEvent(progress = 0f))
            controller.cancelAnimation()
        }

        // Then - still in clean state
        controller.isAnimating.shouldBeFalse()
        controller.progress shouldBe 0f
        controller.currentEvent.shouldBeNull()
    }

    test("different swipe edges are tracked correctly") {
        // Given
        val controller = BackAnimationController()

        // Left edge gesture
        controller.startAnimation(
            BackNavigationEvent(
                progress = 0f,
                swipeEdge = BackNavigationEvent.EDGE_LEFT
            )
        )
        controller.currentEvent?.swipeEdge shouldBe BackNavigationEvent.EDGE_LEFT
        controller.cancelAnimation()

        // Right edge gesture
        controller.startAnimation(
            BackNavigationEvent(
                progress = 0f,
                swipeEdge = BackNavigationEvent.EDGE_RIGHT
            )
        )
        controller.currentEvent?.swipeEdge shouldBe BackNavigationEvent.EDGE_RIGHT
    }

    // =========================================================================
    // ANIMATION PROGRESS INTERPOLATION SCENARIOS
    // =========================================================================

    test("typical gesture progress sequence") {
        // Given
        val controller = BackAnimationController()
        val progressValues = listOf(0f, 0.1f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.9f, 1f)

        // When
        controller.startAnimation(BackNavigationEvent(progress = progressValues[0]))
        for (i in 1 until progressValues.size) {
            controller.updateProgress(BackNavigationEvent(progress = progressValues[i]))
            controller.progress shouldBe progressValues[i]
        }

        // Then - final state
        controller.progress shouldBe 1f
        controller.isAnimating.shouldBeTrue()
    }

    test("gesture with backwards progress - dragging back") {
        // Given
        val controller = BackAnimationController()

        controller.startAnimation(BackNavigationEvent(progress = 0f))
        controller.updateProgress(BackNavigationEvent(progress = 0.5f))

        // When - user drags finger back
        controller.updateProgress(BackNavigationEvent(progress = 0.3f))

        // Then - progress can decrease
        controller.progress shouldBe 0.3f
    }
})
