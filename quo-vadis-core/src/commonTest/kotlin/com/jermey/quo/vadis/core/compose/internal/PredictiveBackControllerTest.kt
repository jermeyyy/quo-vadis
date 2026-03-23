@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.internal.navback.CascadeBackState
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

private object PBCTestDestination : NavDestination

class PredictiveBackControllerTest : FunSpec({

    beforeTest { NavKeyGenerator.reset() }

    fun screen(key: String) = ScreenNode(NodeKey(key), null, PBCTestDestination)

    // =========================================================================
    // Initial state
    // =========================================================================

    test("initial isActive is false") {
        val controller = PredictiveBackController()
        controller.isActive.value.shouldBeFalse()
    }

    test("initial progress is 0") {
        val controller = PredictiveBackController()
        controller.progress.value shouldBe 0f
    }

    test("initial cascadeState is null") {
        val controller = PredictiveBackController()
        controller.cascadeState.value.shouldBeNull()
    }

    // =========================================================================
    // startGesture
    // =========================================================================

    test("startGesture sets isActive to true") {
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.isActive.value.shouldBeTrue()
    }

    test("startGesture resets progress to 0") {
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.progress.value shouldBe 0f
    }

    test("startGesture clears cascadeState") {
        val controller = PredictiveBackController()
        // Start with cascade, then start fresh
        val cascadeState = CascadeBackState(
            sourceNode = screen("s1"),
            exitingNode = screen("s1"),
            targetNode = screen("s2"),
            animatingStackKey = NodeKey("stack"),
            cascadeDepth = 0,
            delegatesToSystem = false
        )
        controller.startGestureWithCascade(cascadeState)
        controller.cascadeState.value.shouldNotBeNull()

        controller.startGesture()
        controller.cascadeState.value.shouldBeNull()
    }

    // =========================================================================
    // startGestureWithCascade
    // =========================================================================

    test("startGestureWithCascade sets isActive to true") {
        val controller = PredictiveBackController()
        val cascadeState = CascadeBackState(
            sourceNode = screen("s1"),
            exitingNode = screen("s1"),
            targetNode = screen("s2"),
            animatingStackKey = NodeKey("stack"),
            cascadeDepth = 1,
            delegatesToSystem = false
        )
        controller.startGestureWithCascade(cascadeState)
        controller.isActive.value.shouldBeTrue()
    }

    test("startGestureWithCascade sets cascadeState") {
        val controller = PredictiveBackController()
        val cascadeState = CascadeBackState(
            sourceNode = screen("s1"),
            exitingNode = screen("s1"),
            targetNode = screen("s2"),
            animatingStackKey = NodeKey("stack"),
            cascadeDepth = 2,
            delegatesToSystem = false
        )
        controller.startGestureWithCascade(cascadeState)

        val result = controller.cascadeState.value
        result.shouldNotBeNull()
        result.cascadeDepth shouldBe 2
        result.delegatesToSystem.shouldBeFalse()
    }

    test("startGestureWithCascade resets progress to 0") {
        val controller = PredictiveBackController()
        val cascadeState = CascadeBackState(
            sourceNode = screen("s1"),
            exitingNode = screen("s1"),
            targetNode = null,
            animatingStackKey = null,
            cascadeDepth = 0,
            delegatesToSystem = true
        )
        controller.startGestureWithCascade(cascadeState)
        controller.progress.value shouldBe 0f
    }

    // =========================================================================
    // updateGestureProgress
    // =========================================================================

    test("updateGestureProgress updates progress when active") {
        val controller = PredictiveBackController()
        controller.startGesture()

        controller.updateGestureProgress(0.1f)

        controller.progress.value shouldBe (0.1f plusOrMinus 0.001f)
    }

    test("updateGestureProgress clamps to GESTURE_MAX_PROGRESS") {
        val controller = PredictiveBackController()
        controller.startGesture()

        // GESTURE_MAX_PROGRESS is 0.17f
        controller.updateGestureProgress(0.5f)

        controller.progress.value shouldBe (0.17f plusOrMinus 0.001f)
    }

    test("updateGestureProgress clamps negative to 0") {
        val controller = PredictiveBackController()
        controller.startGesture()

        controller.updateGestureProgress(-0.5f)

        controller.progress.value shouldBe 0f
    }

    test("updateGestureProgress does nothing when not active") {
        val controller = PredictiveBackController()
        // Not active, don't call startGesture

        controller.updateGestureProgress(0.1f)

        controller.progress.value shouldBe 0f
    }

    // =========================================================================
    // completeGesture
    // =========================================================================

    test("completeGesture sets isActive to false") {
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.isActive.value.shouldBeTrue()

        controller.completeGesture()

        controller.isActive.value.shouldBeFalse()
    }

    test("completeGesture resets progress to 0") {
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.updateGestureProgress(0.1f)

        controller.completeGesture()

        controller.progress.value shouldBe 0f
    }

    test("completeGesture clears cascadeState") {
        val controller = PredictiveBackController()
        val cascadeState = CascadeBackState(
            sourceNode = screen("s1"),
            exitingNode = screen("s1"),
            targetNode = screen("s2"),
            animatingStackKey = NodeKey("stack"),
            cascadeDepth = 0,
            delegatesToSystem = false
        )
        controller.startGestureWithCascade(cascadeState)

        controller.completeGesture()

        controller.cascadeState.value.shouldBeNull()
    }

    // =========================================================================
    // cancelGesture
    // =========================================================================

    test("cancelGesture sets isActive to false") {
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.isActive.value.shouldBeTrue()

        controller.cancelGesture()

        controller.isActive.value.shouldBeFalse()
    }

    test("cancelGesture resets progress to 0") {
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.updateGestureProgress(0.1f)

        controller.cancelGesture()

        controller.progress.value shouldBe 0f
    }

    test("cancelGesture clears cascadeState") {
        val controller = PredictiveBackController()
        val cascadeState = CascadeBackState(
            sourceNode = screen("s1"),
            exitingNode = screen("s1"),
            targetNode = screen("s2"),
            animatingStackKey = NodeKey("stack"),
            cascadeDepth = 1,
            delegatesToSystem = false
        )
        controller.startGestureWithCascade(cascadeState)

        controller.cancelGesture()

        controller.cascadeState.value.shouldBeNull()
    }

    // =========================================================================
    // Full gesture lifecycle
    // =========================================================================

    test("full gesture lifecycle: start -> update -> complete") {
        val controller = PredictiveBackController()

        // Start
        controller.startGesture()
        controller.isActive.value.shouldBeTrue()
        controller.progress.value shouldBe 0f

        // Update
        controller.updateGestureProgress(0.05f)
        controller.progress.value shouldBe (0.05f plusOrMinus 0.001f)

        // Complete
        controller.completeGesture()
        controller.isActive.value.shouldBeFalse()
        controller.progress.value shouldBe 0f
    }

    test("full gesture lifecycle: start -> update -> cancel") {
        val controller = PredictiveBackController()

        // Start
        controller.startGesture()
        controller.isActive.value.shouldBeTrue()

        // Update
        controller.updateGestureProgress(0.1f)
        controller.progress.value shouldBe (0.1f plusOrMinus 0.001f)

        // Cancel
        controller.cancelGesture()
        controller.isActive.value.shouldBeFalse()
        controller.progress.value shouldBe 0f
    }

    test("multiple gesture cycles work correctly") {
        val controller = PredictiveBackController()

        // First gesture - complete
        controller.startGesture()
        controller.updateGestureProgress(0.1f)
        controller.completeGesture()

        // Second gesture - cancel
        controller.startGesture()
        controller.updateGestureProgress(0.05f)
        controller.cancelGesture()

        // Third gesture - verify clean state
        controller.startGesture()
        controller.isActive.value.shouldBeTrue()
        controller.progress.value shouldBe 0f
        controller.cascadeState.value.shouldBeNull()
    }

    test("cascade gesture lifecycle") {
        val controller = PredictiveBackController()
        val cascadeState = CascadeBackState(
            sourceNode = screen("s1"),
            exitingNode = screen("s1"),
            targetNode = screen("s2"),
            animatingStackKey = NodeKey("stack"),
            cascadeDepth = 1,
            delegatesToSystem = false
        )

        controller.startGestureWithCascade(cascadeState)
        controller.isActive.value.shouldBeTrue()
        val cascade = controller.cascadeState.value
        cascade.shouldNotBeNull()
        cascade.cascadeDepth shouldBe 1

        controller.updateGestureProgress(0.08f)
        controller.progress.value shouldBe (0.08f plusOrMinus 0.001f)

        controller.completeGesture()
        controller.isActive.value.shouldBeFalse()
        controller.cascadeState.value.shouldBeNull()
    }
})
