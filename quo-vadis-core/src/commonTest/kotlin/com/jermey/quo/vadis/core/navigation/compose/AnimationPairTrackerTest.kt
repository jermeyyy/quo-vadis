package com.jermey.quo.vadis.core.navigation.compose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [AnimationPairTracker] and related animation pair tracking functionality.
 */
class AnimationPairTrackerTest {

    @Test
    fun `push creates animation pair with new and old surfaces`() {
        val tracker = AnimationPairTracker()

        // Initial state
        val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
        tracker.trackTransition(listOf(surface1))

        // Push new screen
        val surface2 = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT)
        val pairs = tracker.trackTransition(listOf(surface2))

        assertEquals(1, pairs.size)
        assertEquals("screen2", pairs[0].currentId)
        assertEquals("screen1", pairs[0].previousId)
        assertEquals(TransitionType.PUSH, pairs[0].transitionType)
        assertEquals(surface2.id, pairs[0].currentSurface?.id)
        assertEquals(surface1.id, pairs[0].previousSurface?.id)
    }

    @Test
    fun `pop creates animation pair with revealed and removed surfaces`() {
        val tracker = AnimationPairTracker()

        // Initial: only screen1
        val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
        tracker.trackTransition(listOf(surface1))

        // Push screen2
        val surface2 = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT)
        tracker.trackTransition(listOf(surface2))

        // Pop back to first screen - surface1 re-enters, surface2 exits
        val pairs = tracker.trackTransition(
            newSurfaces = listOf(surface1),
            transitionState = TrackerTransitionState.Pop("screen2")
        )

        // Should have a pair for screen1 entering and screen2 exiting
        assertEquals(1, pairs.size)
        assertEquals("screen1", pairs[0].currentId)
        assertEquals("screen2", pairs[0].previousId)
        assertEquals(TransitionType.POP, pairs[0].transitionType)
    }

    @Test
    fun `tab switch creates pair for tab content`() {
        val tracker = AnimationPairTracker()

        // Tab A active
        val tabWrapper = createTestSurface("tabs", SurfaceRenderingMode.TAB_WRAPPER)
        val tabA = createTestSurface(
            "tabA",
            SurfaceRenderingMode.TAB_CONTENT,
            parentWrapperId = "tabs"
        )
        tracker.trackTransition(listOf(tabWrapper, tabA))

        // Switch to Tab B
        val tabB = createTestSurface(
            "tabB",
            SurfaceRenderingMode.TAB_CONTENT,
            parentWrapperId = "tabs"
        )
        val pairs = tracker.trackTransition(listOf(tabWrapper, tabB))

        // Wrapper should not create a pair (unchanged)
        // Only tab content should create a pair
        val contentPairs = pairs.filter {
            it.currentSurface?.renderingMode == SurfaceRenderingMode.TAB_CONTENT
        }
        assertEquals(1, contentPairs.size)
        assertEquals("tabB", contentPairs[0].currentId)
        assertEquals("tabA", contentPairs[0].previousId)
        assertEquals(TransitionType.TAB_SWITCH, contentPairs[0].transitionType)
    }

    @Test
    fun `initial render creates pair with no previous`() {
        val tracker = AnimationPairTracker()

        val surface = createTestSurface("screen1", SurfaceRenderingMode.SINGLE_SCREEN)
        val pairs = tracker.trackTransition(listOf(surface))

        assertEquals(1, pairs.size)
        assertEquals("screen1", pairs[0].currentId)
        assertNull(pairs[0].previousId)
        assertEquals(TransitionType.NONE, pairs[0].transitionType)
        assertFalse(pairs[0].shouldAnimate)
    }

    @Test
    fun `animation pair supports shared elements for stack transitions`() {
        val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
        val surface2 = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT)

        val pair = AnimationPair(
            currentId = "screen2",
            previousId = "screen1",
            transitionType = TransitionType.PUSH,
            currentSurface = surface2,
            previousSurface = surface1
        )

        assertTrue(pair.supportsSharedElements)
        assertTrue(pair.isStackTransition)
        assertTrue(pair.shouldAnimate)
    }

    @Test
    fun `tab switch does not support shared elements`() {
        val tabA = createTestSurface("tabA", SurfaceRenderingMode.TAB_CONTENT)
        val tabB = createTestSurface("tabB", SurfaceRenderingMode.TAB_CONTENT)

        val pair = AnimationPair(
            currentId = "tabB",
            previousId = "tabA",
            transitionType = TransitionType.TAB_SWITCH,
            currentSurface = tabB,
            previousSurface = tabA
        )

        assertFalse(pair.supportsSharedElements)
        assertFalse(pair.isStackTransition)
        assertTrue(pair.shouldAnimate)
    }

    @Test
    fun `container matching pairs surfaces in same container`() {
        val tracker = AnimationPairTracker()

        // Initial: two containers with content
        val container1 = createTestSurface("container1", SurfaceRenderingMode.TAB_WRAPPER)
        val content1a = createTestSurface(
            "content1a",
            SurfaceRenderingMode.TAB_CONTENT,
            parentWrapperId = "container1"
        )
        val container2 = createTestSurface("container2", SurfaceRenderingMode.TAB_WRAPPER)
        val content2a = createTestSurface(
            "content2a",
            SurfaceRenderingMode.TAB_CONTENT,
            parentWrapperId = "container2"
        )
        tracker.trackTransition(listOf(container1, content1a, container2, content2a))

        // Change content in container1 only
        val content1b = createTestSurface(
            "content1b",
            SurfaceRenderingMode.TAB_CONTENT,
            parentWrapperId = "container1"
        )
        val pairs = tracker.trackTransition(listOf(container1, content1b, container2, content2a))

        // Should create pair for content1a -> content1b only
        val contentPairs = pairs.filter {
            it.currentSurface?.renderingMode == SurfaceRenderingMode.TAB_CONTENT
        }
        assertEquals(1, contentPairs.size)
        assertEquals("content1b", contentPairs[0].currentId)
        assertEquals("content1a", contentPairs[0].previousId)
        assertEquals("container1", contentPairs[0].containerId)
    }

    @Test
    fun `unmatched exiting surfaces create exit pairs`() {
        val tracker = AnimationPairTracker()

        // Initial: screen with overlay
        val screen = createTestSurface("screen", SurfaceRenderingMode.STACK_CONTENT)
        val overlay = createTestSurface(
            "overlay",
            SurfaceRenderingMode.SINGLE_SCREEN,
            parentWrapperId = "overlay-container"
        )
        tracker.trackTransition(listOf(screen, overlay))

        // Remove overlay (no entering surface in same container)
        val pairs = tracker.trackTransition(listOf(screen))

        // Should have a pair for the exiting overlay
        val exitPair = pairs.find { it.previousId == "overlay" }
        assertTrue(exitPair != null, "Should have exit pair for overlay")
        assertEquals(TransitionType.POP, exitPair!!.transitionType)
    }

    @Test
    fun `reset clears state`() {
        val tracker = AnimationPairTracker()

        // Initial state
        val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
        tracker.trackTransition(listOf(surface1))

        // Reset
        tracker.reset()

        // Same surface should appear as new (no previous)
        val pairs = tracker.trackTransition(listOf(surface1))

        assertEquals(1, pairs.size)
        assertEquals("screen1", pairs[0].currentId)
        assertNull(pairs[0].previousId)
        assertEquals(TransitionType.NONE, pairs[0].transitionType)
    }

    @Test
    fun `hasBothSurfaces returns true when both surfaces present`() {
        val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
        val surface2 = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT)

        val pairWithBoth = AnimationPair(
            currentId = "screen2",
            previousId = "screen1",
            transitionType = TransitionType.PUSH,
            currentSurface = surface2,
            previousSurface = surface1
        )

        val pairWithoutPrevious = AnimationPair(
            currentId = "screen1",
            previousId = null,
            transitionType = TransitionType.NONE,
            currentSurface = surface1,
            previousSurface = null
        )

        assertTrue(pairWithBoth.hasBothSurfaces)
        assertFalse(pairWithoutPrevious.hasBothSurfaces)
    }

    @Test
    fun `hasFullSurfaces returns true when currentSurface is set`() {
        val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)

        val fullPair = AnimationPair(
            currentId = "screen1",
            previousId = null,
            transitionType = TransitionType.NONE,
            currentSurface = surface1,
            previousSurface = null
        )

        val basicPair = AnimationPair(
            currentId = "screen1",
            previousId = null,
            transitionType = TransitionType.NONE
            // No surfaces - basic pair from TreeFlattener
        )

        assertTrue(fullPair.hasFullSurfaces)
        assertFalse(basicPair.hasFullSurfaces)
    }

    @Test
    fun `isContentMode extension works correctly`() {
        assertTrue(SurfaceRenderingMode.STACK_CONTENT.isContentMode())
        assertTrue(SurfaceRenderingMode.TAB_CONTENT.isContentMode())
        assertTrue(SurfaceRenderingMode.PANE_CONTENT.isContentMode())
        assertTrue(SurfaceRenderingMode.SINGLE_SCREEN.isContentMode())
        assertFalse(SurfaceRenderingMode.TAB_WRAPPER.isContentMode())
        assertFalse(SurfaceRenderingMode.PANE_WRAPPER.isContentMode())
        assertFalse(SurfaceRenderingMode.PANE_AS_STACK.isContentMode())
    }

    @Test
    fun `pane switch creates correct pair`() {
        val tracker = AnimationPairTracker()

        // Initial: pane A active
        val paneWrapper = createTestSurface("panes", SurfaceRenderingMode.PANE_WRAPPER)
        val paneA = createTestSurface(
            "paneA",
            SurfaceRenderingMode.PANE_CONTENT,
            parentWrapperId = "panes"
        )
        tracker.trackTransition(listOf(paneWrapper, paneA))

        // Switch to pane B
        val paneB = createTestSurface(
            "paneB",
            SurfaceRenderingMode.PANE_CONTENT,
            parentWrapperId = "panes"
        )
        val pairs = tracker.trackTransition(listOf(paneWrapper, paneB))

        val contentPairs = pairs.filter {
            it.currentSurface?.renderingMode == SurfaceRenderingMode.PANE_CONTENT
        }
        assertEquals(1, contentPairs.size)
        assertEquals("paneB", contentPairs[0].currentId)
        assertEquals("paneA", contentPairs[0].previousId)
        assertEquals(TransitionType.PANE_SWITCH, contentPairs[0].transitionType)
    }

    @Test
    fun `explicit transition state overrides inference`() {
        val tracker = AnimationPairTracker()

        // Initial state
        val surface1 = createTestSurface("screen1", SurfaceRenderingMode.STACK_CONTENT)
        tracker.trackTransition(listOf(surface1))

        // Push with explicit POP state (unusual but should respect explicit state)
        val surface2 = createTestSurface("screen2", SurfaceRenderingMode.STACK_CONTENT)
        val pairs = tracker.trackTransition(
            newSurfaces = listOf(surface2),
            transitionState = TrackerTransitionState.Pop("screen1")
        )

        assertEquals(1, pairs.size)
        assertEquals(TransitionType.POP, pairs[0].transitionType)
    }

    // Helper function to create test surfaces
    private fun createTestSurface(
        id: String,
        mode: SurfaceRenderingMode,
        parentWrapperId: String? = null
    ): RenderableSurface {
        return RenderableSurface(
            id = id,
            zOrder = 0,
            nodeType = when (mode) {
                SurfaceRenderingMode.TAB_WRAPPER,
                SurfaceRenderingMode.TAB_CONTENT -> SurfaceNodeType.TAB

                SurfaceRenderingMode.PANE_WRAPPER,
                SurfaceRenderingMode.PANE_CONTENT -> SurfaceNodeType.PANE

                SurfaceRenderingMode.STACK_CONTENT -> SurfaceNodeType.STACK
                else -> SurfaceNodeType.SCREEN
            },
            renderingMode = mode,
            content = {},
            parentWrapperId = parentWrapperId
        )
    }
}
