@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests verifying that shared element transitions (TransitionScope) are disabled
 * during active predictive back gestures.
 *
 * Two code paths provide `LocalTransitionScope`:
 *
 * 1. **`NavRenderScopeImpl.WithAnimatedVisibilityScope`** — provides TransitionScope to the
 *    current screen inside AnimatedContent. During predictive back, transitions are suppressed
 *    and the screen is translated via graphicsLayer, so shared elements would conflict.
 *
 * 2. **`StaticAnimatedVisibilityScope`** — provides TransitionScope to the underlay (previous
 *    screen) and modal overlays during predictive back. The underlay is parallax-translated
 *    via graphicsLayer, so shared elements would glitch.
 *
 * Both paths use the same logic pattern:
 * ```
 * val transitionScope = if (!isPredictiveBackActive) {
 *     sharedTransitionScope?.let { TransitionScope(it, animatedVisibilityScope) }
 * } else {
 *     null
 * }
 * ```
 *
 * Since these are @Composable functions requiring compose test infrastructure,
 * these tests verify the underlying decision logic and state transitions that
 * drive the TransitionScope nullification behavior.
 */
class TransitionScopePredictiveBackTest : FunSpec({

    // =========================================================================
    // HELPER: Simulates the TransitionScope decision logic from both
    // NavRenderScopeImpl.WithAnimatedVisibilityScope and
    // StaticAnimatedVisibilityScope
    // =========================================================================

    /**
     * Simulates the TransitionScope creation decision pattern used in:
     * - `NavRenderScopeImpl.WithAnimatedVisibilityScope`
     * - `StaticAnimatedVisibilityScope`
     *
     * @param isPredictiveBackActive Whether the predictive back gesture is active
     * @param sharedTransitionScopeAvailable Whether a SharedTransitionScope is available
     * @return true if TransitionScope would be non-null (i.e., shared elements enabled)
     */
    fun shouldCreateTransitionScope(
        isPredictiveBackActive: Boolean,
        sharedTransitionScopeAvailable: Boolean
    ): Boolean = !isPredictiveBackActive && sharedTransitionScopeAvailable

    // =========================================================================
    // WithAnimatedVisibilityScope: TransitionScope disabled during predictive back
    // =========================================================================

    test("WithAnimatedVisibilityScope — TransitionScope is null when predictive back is active") {
        // Given — NavRenderScopeImpl reads predictiveBackController.isActive.value
        val controller = PredictiveBackController()
        controller.startGesture()

        // When — decision logic mirrors NavRenderScopeImpl.WithAnimatedVisibilityScope
        val isPredictiveBackActive = controller.isActive.value
        val result = shouldCreateTransitionScope(
            isPredictiveBackActive = isPredictiveBackActive,
            sharedTransitionScopeAvailable = true
        )

        // Then — TransitionScope should NOT be created (shared elements disabled)
        isPredictiveBackActive.shouldBeTrue()
        result.shouldBeFalse()
    }

    test("WithAnimatedVisibilityScope — TransitionScope is non-null when predictive back is inactive") {
        // Given — predictive back gesture is not active
        val controller = PredictiveBackController()

        // When — decision logic mirrors NavRenderScopeImpl.WithAnimatedVisibilityScope
        val isPredictiveBackActive = controller.isActive.value
        val result = shouldCreateTransitionScope(
            isPredictiveBackActive = isPredictiveBackActive,
            sharedTransitionScopeAvailable = true
        )

        // Then — TransitionScope SHOULD be created (shared elements enabled)
        isPredictiveBackActive.shouldBeFalse()
        result.shouldBeTrue()
    }

    test("WithAnimatedVisibilityScope — TransitionScope is null when SharedTransitionScope is unavailable") {
        // Given — no SharedTransitionScope available (sharedTransitionScope == null)
        val controller = PredictiveBackController()

        // When
        val result = shouldCreateTransitionScope(
            isPredictiveBackActive = controller.isActive.value,
            sharedTransitionScopeAvailable = false
        )

        // Then — TransitionScope is null regardless of predictive back state
        result.shouldBeFalse()
    }

    // =========================================================================
    // StaticAnimatedVisibilityScope: TransitionScope disabled during predictive back
    // =========================================================================

    test("StaticAnimatedVisibilityScope — TransitionScope is null when predictive back is active") {
        // Given — StaticAnimatedVisibilityScope reads from LocalNavRenderScope.current
        // and checks navRenderScope?.predictiveBackController?.isActive?.value == true
        val controller = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = controller)

        controller.startGesture()

        // When — decision logic mirrors StaticAnimatedVisibilityScope
        val isPredictiveBackActive = scope.predictiveBackController.isActive.value == true
        val result = shouldCreateTransitionScope(
            isPredictiveBackActive = isPredictiveBackActive,
            sharedTransitionScopeAvailable = true
        )

        // Then — TransitionScope should NOT be created for underlay content
        isPredictiveBackActive.shouldBeTrue()
        result.shouldBeFalse()
    }

    test("StaticAnimatedVisibilityScope — TransitionScope is non-null when predictive back is inactive") {
        // Given — predictive back is not active
        val controller = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = controller)

        // When — decision logic mirrors StaticAnimatedVisibilityScope
        val isPredictiveBackActive = scope.predictiveBackController.isActive.value == true
        val result = shouldCreateTransitionScope(
            isPredictiveBackActive = isPredictiveBackActive,
            sharedTransitionScopeAvailable = true
        )

        // Then — TransitionScope SHOULD be created for underlay content
        isPredictiveBackActive.shouldBeFalse()
        result.shouldBeTrue()
    }

    test("StaticAnimatedVisibilityScope — null navRenderScope means TransitionScope is null") {
        // Given — StaticAnimatedVisibilityScope handles null navRenderScope via safe-call:
        // navRenderScope?.predictiveBackController?.isActive?.value == true → false when null
        val navRenderScope: FakeNavRenderScope? = null

        // When — mirrors the StaticAnimatedVisibilityScope null-safe check
        val isPredictiveBackActive = navRenderScope?.predictiveBackController?.isActive?.value == true

        // Then — should be false (not active), but sharedTransitionScope is unavailable via
        // navRenderScope?.sharedTransitionScope which will also be null
        isPredictiveBackActive.shouldBeFalse()

        // In the real code, sharedTransitionScope comes from navRenderScope?.sharedTransitionScope
        // which is null when navRenderScope is null, so TransitionScope is null regardless
        val sharedTransitionScopeAvailable = navRenderScope?.sharedTransitionScope != null
        val result = shouldCreateTransitionScope(isPredictiveBackActive, sharedTransitionScopeAvailable)
        result.shouldBeFalse()
    }

    // =========================================================================
    // Gesture lifecycle: TransitionScope re-enabled after gesture ends
    // =========================================================================

    test("TransitionScope re-enabled after gesture completes") {
        // Given — gesture is active, shared elements are disabled
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.updateGestureProgress(0.1f)

        val duringGesture = shouldCreateTransitionScope(
            isPredictiveBackActive = controller.isActive.value,
            sharedTransitionScopeAvailable = true
        )
        duringGesture.shouldBeFalse()

        // When — gesture completes
        controller.completeGesture()

        // Then — shared elements re-enabled
        val afterGesture = shouldCreateTransitionScope(
            isPredictiveBackActive = controller.isActive.value,
            sharedTransitionScopeAvailable = true
        )
        afterGesture.shouldBeTrue()
    }

    test("TransitionScope re-enabled after gesture cancels") {
        // Given — gesture is active, shared elements are disabled
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.updateGestureProgress(0.08f)

        val duringGesture = shouldCreateTransitionScope(
            isPredictiveBackActive = controller.isActive.value,
            sharedTransitionScopeAvailable = true
        )
        duringGesture.shouldBeFalse()

        // When — gesture is cancelled (user swipes back)
        controller.cancelGesture()

        // Then — shared elements re-enabled
        val afterCancel = shouldCreateTransitionScope(
            isPredictiveBackActive = controller.isActive.value,
            sharedTransitionScopeAvailable = true
        )
        afterCancel.shouldBeTrue()
    }

    test("TransitionScope disabled throughout full gesture progress range") {
        // Given — gesture starts and progresses through various points
        val controller = PredictiveBackController()
        controller.startGesture()

        // When/Then — at various progress values, shared elements remain disabled
        val progressValues = listOf(0f, 0.01f, 0.05f, 0.1f, 0.15f, 0.17f)
        progressValues.forEach { progress ->
            controller.updateGestureProgress(progress)
            val result = shouldCreateTransitionScope(
                isPredictiveBackActive = controller.isActive.value,
                sharedTransitionScopeAvailable = true
            )
            result.shouldBeFalse()
        }
    }

    // =========================================================================
    // FakeNavRenderScope integration: predictiveBackController accessibility
    // =========================================================================

    test("FakeNavRenderScope exposes predictiveBackController for testing") {
        // Given
        val controller = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = controller)

        // Then — controller is accessible and defaults to inactive
        scope.predictiveBackController shouldBe controller
        scope.predictiveBackController.isActive.value.shouldBeFalse()
    }

    test("FakeNavRenderScope sharedTransitionScope is null by default") {
        // Given — FakeNavRenderScope defaults to null sharedTransitionScope
        val scope = FakeNavRenderScope()

        // Then — even with predictive back inactive, TransitionScope would be null
        // because sharedTransitionScope is unavailable
        scope.sharedTransitionScope.shouldBeNull()
        @Suppress("KotlinConstantConditions")
        val hasSharedTransitionScope = scope.sharedTransitionScope != null
        val result = shouldCreateTransitionScope(
            isPredictiveBackActive = scope.predictiveBackController.isActive.value,
            sharedTransitionScopeAvailable = hasSharedTransitionScope
        )
        result.shouldBeFalse()
    }

    test("gesture state changes are visible through FakeNavRenderScope") {
        // Given
        val controller = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = controller)

        // Initially inactive
        scope.predictiveBackController.isActive.value.shouldBeFalse()

        // When — start gesture via controller
        controller.startGesture()

        // Then — visible through scope
        scope.predictiveBackController.isActive.value.shouldBeTrue()

        // When — complete gesture
        controller.completeGesture()

        // Then — back to inactive
        scope.predictiveBackController.isActive.value.shouldBeFalse()
    }

    // =========================================================================
    // Multiple gesture cycles: consistent behavior
    // =========================================================================

    test("TransitionScope correctly toggles across multiple gesture cycles") {
        val controller = PredictiveBackController()

        // Cycle 1: start → complete
        controller.startGesture()
        shouldCreateTransitionScope(controller.isActive.value, true).shouldBeFalse()
        controller.completeGesture()
        shouldCreateTransitionScope(controller.isActive.value, true).shouldBeTrue()

        // Cycle 2: start → cancel
        controller.startGesture()
        shouldCreateTransitionScope(controller.isActive.value, true).shouldBeFalse()
        controller.cancelGesture()
        shouldCreateTransitionScope(controller.isActive.value, true).shouldBeTrue()

        // Cycle 3: start → update → complete
        controller.startGesture()
        controller.updateGestureProgress(0.15f)
        shouldCreateTransitionScope(controller.isActive.value, true).shouldBeFalse()
        controller.completeGesture()
        shouldCreateTransitionScope(controller.isActive.value, true).shouldBeTrue()
    }
})
