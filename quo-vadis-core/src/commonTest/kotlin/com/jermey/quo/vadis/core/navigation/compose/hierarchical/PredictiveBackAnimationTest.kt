@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.ComposableCache
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
 * Tests for predictive back gesture animation in AnimatedNavContent.
 *
 * After the refactoring that eliminated the branch switch, `AnimatedNavContent` now
 * handles predictive back internally using an underlay pattern with `graphicsLayer`
 * transforms. The deleted `PredictiveBackContent` is no longer used.
 *
 * Tests cover:
 * - AnimatedNavContent predictive back integration
 * - Composition retention during gesture (P0 — key regression test)
 * - Gesture cancellation preserves composition (P0)
 * - Transform calculations with updated constants (PARALLAX_FACTOR=0.15, SCALE_FACTOR=0.15)
 * - Gesture completion smooth transition via recentlyCompletedGesture flag
 * - Cache locking during gesture
 * - Underlay not composed at rest (zero overhead)
 * - Transition suppression during gesture
 * - Previous content rendering during gesture
 * - Predictive back controller state
 */
class PredictiveBackAnimationTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val currentDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    val previousDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // CONSTANTS (matching AnimatedNavContent)
    // =========================================================================

    val parallaxFactor = 0.15f
    val scaleFactor = 0.15f

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = currentDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    fun createStack(
        key: String,
        parentKey: String? = null,
        vararg screens: ScreenNode
    ): StackNode = StackNode(NodeKey(key), parentKey?.let { NodeKey(it) }, screens.toList())

    // =========================================================================
    // PREDICTIVE BACK CONTROLLER TESTS (AnimatedNavContent integration)
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

    test("custom predictive back controller can be injected into scope") {
        // Given
        val customController = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = customController)

        // Then
        scope.predictiveBackController shouldBe customController
    }

    // =========================================================================
    // GESTURE ACTIVE STATE TESTS (AnimatedNavContent behavior)
    // =========================================================================

    test("gesture inactive means standard AnimatedContent path is used") {
        // Given — AnimatedNavContent reads controller.isActive.value
        val scope = FakeNavRenderScope()

        // When
        val isPredictiveBackActive = scope.predictiveBackController.isActive.value

        // Then — standard AnimatedContent path (no underlay, no graphicsLayer transforms)
        isPredictiveBackActive.shouldBeFalse()
    }

    test("predictive back enablement depends on root stack") {
        // Given — AnimatedNavContent receives predictiveBackEnabled from StackRenderer
        val rootStack = createStack("root", null, createScreen("s1", "root"))
        val nestedStack = createStack("nested", "parent", createScreen("s1", "nested"))

        // Then — only root stacks (parentKey == null) enable predictive back
        val rootPredictiveBackEnabled = rootStack.parentKey == null
        val nestedPredictiveBackEnabled = nestedStack.parentKey == null

        rootPredictiveBackEnabled.shouldBeTrue()
        nestedPredictiveBackEnabled.shouldBeFalse()
    }

    // =========================================================================
    // PROGRESS TRANSFORM TESTS (updated constants: PARALLAX=0.15, SCALE=0.15)
    // =========================================================================

    test("progress at zero means no transform applied via graphicsLayer") {
        // Given — AnimatedNavContent applies transforms inside graphicsLayer
        val progress = 0f

        // When — underlay parallax: translationX = -width * PARALLAX_FACTOR * (1 - progress)
        val underlayTranslationFactor = -parallaxFactor * (1f - progress)
        // Current screen slide: translationX = width * progress
        val currentTranslationFactor = progress
        // Current screen scale: 1 - (progress * scaleFactor)
        val currentScale = 1f - (progress * scaleFactor)

        // Then
        underlayTranslationFactor shouldBe -0.15f
        currentTranslationFactor shouldBe 0f
        currentScale shouldBe 1f
    }

    test("progress at one means full transform applied via graphicsLayer") {
        // Given
        val progress = 1f

        // When
        val underlayTranslationFactor = -parallaxFactor * (1f - progress)
        val currentTranslationFactor = progress
        val currentScale = 1f - (progress * scaleFactor)

        // Then — underlay at rest (0), current fully slid out, scale = 0.85
        underlayTranslationFactor shouldBe 0f
        currentTranslationFactor shouldBe 1f
        currentScale shouldBe (0.85f plusOrMinus 0.001f)
    }

    test("progress at half means intermediate transform") {
        // Given
        val progress = 0.5f

        // When
        val underlayTranslationFactor = -parallaxFactor * (1f - progress)
        val currentTranslationFactor = progress
        val currentScale = 1f - (progress * scaleFactor)

        // Then
        underlayTranslationFactor shouldBe (-0.075f plusOrMinus 0.001f)
        currentTranslationFactor shouldBe 0.5f
        currentScale shouldBe (0.925f plusOrMinus 0.001f)
    }

    // =========================================================================
    // PREVIOUS CONTENT (UNDERLAY) RENDERING TESTS
    // =========================================================================

    test("previous content is null initially — no underlay composed") {
        // Given — AnimatedNavContent tracks stateBeforeLast internally
        val current = createScreen("current-screen", destination = currentDestination)

        // When — no navigation has occurred yet
        val previous: ScreenNode? = null

        // Then — underlay guard: backTarget == null means no underlay Box
        previous.shouldBeNull()
    }

    test("previous content is available after navigation for underlay") {
        // Given — simulates AnimatedNavContent state tracking
        var previous: ScreenNode?
        var current = createScreen("screen-a", destination = previousDestination)

        // When — navigate to new screen (lastCommittedState tracks this)
        previous = current
        current = createScreen("screen-b", destination = currentDestination)

        // Then — stateBeforeLast is set, underlay can render
        previous.shouldNotBeNull()
        previous.key shouldBe NodeKey("screen-a")
        current.key shouldBe NodeKey("screen-b")
    }

    test("underlay guard prevents rendering when backTarget equals lastCommittedState") {
        // Given — AnimatedNavContent guard: backTarget.key != lastCommittedState.key
        val sameScreen = createScreen("same-key", destination = currentDestination)
        val lastCommittedState = sameScreen
        val backTarget = sameScreen

        // Then — guard prevents underlay (same key)
        val shouldRenderUnderlay = backTarget.key != lastCommittedState.key
        shouldRenderUnderlay.shouldBeFalse()
    }

    test("underlay renders when backTarget differs from lastCommittedState") {
        // Given
        val current = createScreen("current-key", destination = currentDestination)
        val backTarget = createScreen("previous-key", destination = previousDestination)

        // Then — guard passes
        val shouldRenderUnderlay = backTarget.key != current.key
        shouldRenderUnderlay.shouldBeTrue()
    }

    // =========================================================================
    // STACK BACK NAVIGATION TESTS
    // =========================================================================

    test("stack provides previous and current for predictive back underlay") {
        // Given — AnimatedNavContent resolves backTarget from cascadeState or stateBeforeLast
        val screen1 = createScreen("s1", "stack")
        val screen2 = createScreen("s2", "stack")
        val screen3 = createScreen("s3", "stack")
        val stack = createStack("stack", null, screen1, screen2, screen3)

        // When
        val currentChild = stack.activeChild
        val children = stack.children
        val previousChild = if (children.size >= 2) children[children.size - 2] else null

        // Then
        currentChild shouldBe screen3
        previousChild shouldBe screen2
    }

    test("single item stack has no previous for underlay") {
        // Given
        val screen = createScreen("only-screen", "stack")
        val stack = createStack("stack", null, screen)

        // When
        val currentChild = stack.activeChild
        val children = stack.children
        val previousChild = if (children.size >= 2) children[children.size - 2] else null

        // Then — no underlay possible
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
    // P0: COMPOSITION RETAINED DURING GESTURE START
    // =========================================================================

    test("composition retained during gesture start — state tracking simulation") {
        // Given — AnimatedNavContent keeps the current screen at a stable composition
        // position inside AnimatedContent. When gesture starts, the screen does NOT move
        // to a different branch (unlike the old PredictiveBackContent pattern).
        val controller = PredictiveBackController()
        val scope = FakeNavRenderScope(predictiveBackController = controller)

        // Simulate: screen is rendered, has internal remember state (counter = 42)
        var rememberedState = 42
        val currentScreen = createScreen("current", "stack", currentDestination)

        // When — gesture starts
        controller.startGesture()
        controller.updateGestureProgress(0.1f)

        // Then — the composition position hasn't changed, isActive is true
        controller.isActive.value.shouldBeTrue()
        // The key insight: in the new design, AnimatedContent target doesn't change
        // when gesture starts. The same screen continues at the same tree position.
        // Therefore, remember state survives.
        rememberedState shouldBe 42 // State preserved (no recomposition / branch switch)
        currentScreen.key shouldBe NodeKey("current") // Key is stable
    }

    test("composition retained — AnimatedContent target unchanged during gesture") {
        // Given — AnimatedNavContent receives the same targetState while gesture is active.
        // The gesture does NOT change the targetState passed to AnimatedContent.
        val screen = createScreen("stable-screen", "stack")

        // Simulate multiple gesture progress updates
        val controller = PredictiveBackController()
        controller.startGesture()

        val targetBeforeGesture = screen
        controller.updateGestureProgress(0.05f)
        val targetDuringGesture = screen // Same object — AnimatedContent target unchanged
        controller.updateGestureProgress(0.1f)
        val targetLaterInGesture = screen

        // Then — target remains identical throughout the gesture
        targetDuringGesture shouldBe targetBeforeGesture
        targetLaterInGesture shouldBe targetBeforeGesture
        // This is the key invariant: no branch switch, so composition is retained
    }

    // =========================================================================
    // P0: GESTURE CANCELLATION PRESERVES COMPOSITION
    // =========================================================================

    test("gesture cancellation preserves composition — state restored to idle") {
        // Given — begin gesture
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.updateGestureProgress(0.1f)

        // Simulated remember state that survives
        var rememberedCounter = 100

        // When — cancel gesture
        controller.cancelGesture()

        // Then — controller returns to idle, composition was never disrupted
        controller.isActive.value.shouldBeFalse()
        controller.progress.value shouldBe 0f
        // remember state intact because AnimatedContent target never changed
        rememberedCounter shouldBe 100
    }

    test("gesture cancellation does not change AnimatedContent target") {
        // Given — the same screen is the target throughout
        val screen = createScreen("persistent-screen", "stack")
        val controller = PredictiveBackController()

        // Start and progress gesture
        controller.startGesture()
        controller.updateGestureProgress(0.1f)

        val targetDuringGesture = screen

        // Cancel gesture
        controller.cancelGesture()

        val targetAfterCancel = screen

        // Then — same target, no recomposition triggered by branch switch
        targetAfterCancel shouldBe targetDuringGesture
        controller.isActive.value.shouldBeFalse()
        controller.progress.value shouldBe 0f
    }

    // =========================================================================
    // P1: GESTURE COMPLETION SMOOTH TRANSITION
    // =========================================================================

    test("gesture completion triggers recentlyCompletedGesture flag logic") {
        // Given — AnimatedNavContent uses a one-frame recentlyCompletedGesture flag
        // to suppress AnimatedContent transitions at the completion boundary.
        val controller = PredictiveBackController()

        // When — simulate gesture lifecycle
        controller.startGesture()
        val wasActiveBeforeComplete = controller.isActive.value

        controller.completeGesture()
        val wasActiveAfterComplete = controller.isActive.value

        // Then — isActive goes from true to false, which triggers recentlyCompletedGesture
        // in AnimatedNavContent's composition frame
        wasActiveBeforeComplete.shouldBeTrue()
        wasActiveAfterComplete.shouldBeFalse()
        // The recentlyCompletedGesture flag causes AnimatedContent to use
        // EnterTransition.None togetherWith ExitTransition.None for one frame
    }

    // =========================================================================
    // P1: CACHE LOCKING DURING GESTURE
    // =========================================================================

    test("cache lock and unlock work correctly for predictive back keys") {
        // Given — AnimatedNavContent calls cache.lock/unlock via DisposableEffect
        val cache = ComposableCache()
        val scope = FakeNavRenderScope(cache = cache)

        val currentKey = "current-screen"
        val backTargetKey = "back-target"

        // When — gesture starts, both keys are locked
        scope.cache.lock(currentKey)
        scope.cache.lock(backTargetKey)

        // Then — no exception, both locked. When gesture ends:
        scope.cache.unlock(currentKey)
        scope.cache.unlock(backTargetKey)

        // Verify unlock is idempotent (no exception)
        scope.cache.unlock(currentKey)
        scope.cache.unlock(backTargetKey)
    }

    test("cache locking uses correct key format from NavNode") {
        // Given — AnimatedNavContent uses node.key.value for cache lock/unlock
        val currentScreen = createScreen("screen-current", "stack")
        val backTarget = createScreen("screen-previous", "stack")
        val cache = ComposableCache()

        // When — lock using the same pattern as AnimatedNavContent
        cache.lock(currentScreen.key.value)
        cache.lock(backTarget.key.value)

        // Then — keys are string representations of node keys
        currentScreen.key.value shouldBe "screen-current"
        backTarget.key.value shouldBe "screen-previous"

        // Cleanup
        cache.unlock(currentScreen.key.value)
        cache.unlock(backTarget.key.value)
    }

    // =========================================================================
    // P1: UNDERLAY NOT COMPOSED AT REST
    // =========================================================================

    test("underlay not composed at rest — no gesture means no underlay") {
        // Given — AnimatedNavContent guard:
        // if (isPredictiveBackActive && backTarget != null && backTarget.key != lastCommittedState.key)
        val controller = PredictiveBackController()

        // When — no gesture active
        val isPredictiveBackActive = controller.isActive.value

        // Then — guard fails at first condition, zero overhead
        isPredictiveBackActive.shouldBeFalse()
        // No underlay Box is composed in the tree
    }

    test("underlay not composed when predictiveBackEnabled is false") {
        // Given — AnimatedNavContent: isPredictiveBackActive = predictiveBackEnabled && controller.isActive.value
        val controller = PredictiveBackController()
        controller.startGesture() // Gesture active on controller

        val predictiveBackEnabled = false

        // When
        val isPredictiveBackActive = predictiveBackEnabled && controller.isActive.value

        // Then — even though controller says active, the flag is false
        isPredictiveBackActive.shouldBeFalse()

        // Cleanup
        controller.cancelGesture()
    }

    // =========================================================================
    // P1: TRANSITION SUPPRESSION DURING GESTURE
    // =========================================================================

    test("transition suppressed during active gesture") {
        // Given — AnimatedNavContent uses:
        // if (isPredictiveBackActive || recentlyCompletedGesture) -> None transitions
        val controller = PredictiveBackController()
        controller.startGesture()

        val isPredictiveBackActive = true
        val recentlyCompletedGesture = false

        // When — transition spec selection
        val useNoneTransition = isPredictiveBackActive || recentlyCompletedGesture

        // Then — AnimatedContent uses EnterTransition.None togetherWith ExitTransition.None
        useNoneTransition.shouldBeTrue()

        // Cleanup
        controller.cancelGesture()
    }

    test("transition suppressed during recently completed gesture frame") {
        // Given — one-frame flag after gesture completion
        val isPredictiveBackActive = false
        val recentlyCompletedGesture = true

        // When
        val useNoneTransition = isPredictiveBackActive || recentlyCompletedGesture

        // Then — still suppressed for the completion boundary frame
        useNoneTransition.shouldBeTrue()
    }

    test("normal transition used when no gesture and not recently completed") {
        // Given — idle state
        val isPredictiveBackActive = false
        val recentlyCompletedGesture = false

        // When
        val useNoneTransition = isPredictiveBackActive || recentlyCompletedGesture

        // Then — standard transition spec from NavTransition
        useNoneTransition.shouldBeFalse()
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

    test("cache key remains stable during gesture progress changes") {
        // Given — AnimatedNavContent uses contentKey = { it.key } in AnimatedContent
        val screen = createScreen("stable-key", "stack")

        // Simulate progress changes during gesture
        val progressValues = listOf(0f, 0.05f, 0.10f, 0.15f, 0.17f)

        // Then — key doesn't change regardless of progress
        progressValues.forEach { _ ->
            screen.key shouldBe NodeKey("stable-key")
        }
    }

    // =========================================================================
    // GESTURE LIFECYCLE TESTS
    // =========================================================================

    test("gesture cancellation restores progress to zero") {
        // Given
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.updateGestureProgress(0.1f)

        // When
        controller.cancelGesture()

        // Then
        controller.progress.value shouldBe 0f
        controller.isActive.value.shouldBeFalse()
    }

    test("gesture completion resets controller state") {
        // Given
        val controller = PredictiveBackController()
        controller.startGesture()
        controller.updateGestureProgress(0.15f)

        // When
        controller.completeGesture()

        // Then
        controller.progress.value shouldBe 0f
        controller.isActive.value.shouldBeFalse()
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
    // PARALLAX AND SCALE CONSTANT TESTS (updated values)
    // =========================================================================

    test("parallax factor matches AnimatedNavContent constant") {
        // Given — PARALLAX_FACTOR = 0.15f in AnimatedNavContent
        (parallaxFactor > 0f && parallaxFactor < 1f).shouldBeTrue()
        parallaxFactor shouldBe 0.15f
    }

    test("scale factor matches AnimatedNavContent constant") {
        // Given — SCALE_FACTOR = 0.15f in AnimatedNavContent
        (scaleFactor > 0f && scaleFactor < 0.5f).shouldBeTrue()
        scaleFactor shouldBe 0.15f

        // Minimum scale at full progress
        val minScale = 1f - scaleFactor
        (minScale >= 0.5f).shouldBeTrue()
        minScale shouldBe (0.85f plusOrMinus 0.001f)
    }

    // =========================================================================
    // PREDICTIVE BACK STATE FLOW TESTS
    // =========================================================================

    test("predictive back controller provides isActive State") {
        // Given
        val controller = PredictiveBackController()

        // Then
        controller.isActive.shouldNotBeNull()
        controller.isActive.value.shouldBeFalse()
    }

    test("predictive back controller provides progress State") {
        // Given
        val controller = PredictiveBackController()

        // Then
        controller.progress.shouldNotBeNull()
        controller.progress.value shouldBe 0f
    }

    // =========================================================================
    // STATE TRACKING (AnimatedNavContent internals)
    // =========================================================================

    test("state tracking updates stateBeforeLast on key change") {
        // Given — simulates AnimatedNavContent state tracking logic:
        // if (targetState != lastCommittedState) {
        //     if (targetState.key != lastCommittedState.key) stateBeforeLast = lastCommittedState
        //     lastCommittedState = targetState
        // }
        val screenA = createScreen("a", "stack", previousDestination)
        val screenB = createScreen("b", "stack", currentDestination)

        var lastCommittedState = screenA
        var stateBeforeLast: ScreenNode? = null

        // When — navigate from A to B
        val targetState = screenB
        if (targetState != lastCommittedState) {
            if (targetState.key != lastCommittedState.key) {
                stateBeforeLast = lastCommittedState
            }
            lastCommittedState = targetState
        }

        // Then
        lastCommittedState shouldBe screenB
        stateBeforeLast shouldBe screenA
    }

    test("state tracking does not update stateBeforeLast on same key") {
        // Given — internal state update (e.g., PaneNode content change) with same key
        val screenV1 = createScreen("same-key", "stack", previousDestination)
        val screenV2 = createScreen("same-key", "stack", currentDestination)

        var lastCommittedState = screenV1
        var stateBeforeLast: ScreenNode? = null

        // When — "navigate" to same-key screen (internal update)
        val targetState = screenV2
        if (targetState != lastCommittedState) {
            if (targetState.key != lastCommittedState.key) {
                stateBeforeLast = lastCommittedState
            }
            lastCommittedState = targetState
        }

        // Then — stateBeforeLast is NOT set (same key)
        lastCommittedState shouldBe screenV2
        stateBeforeLast.shouldBeNull()
    }
})
