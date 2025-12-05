package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TransitionState] sealed class and [TransitionStateManager].
 *
 * Tests cover:
 * - State variants (Idle, Proposed, Animating)
 * - Query methods (affectsStack, affectsTab, previousChildOf, previousTabIndex)
 * - Progress management
 * - State machine transitions
 */
class TransitionStateTest {

    // =========================================================================
    // TEST FIXTURES
    // =========================================================================

    // Using BasicDestination since route is resolved via RouteRegistry for regular Destinations
    private val mockDestination = BasicDestination("test")

    private fun createScreenNode(key: String, parentKey: String? = null) = ScreenNode(
        key = key,
        parentKey = parentKey,
        destination = mockDestination
    )

    private fun createStackNode(
        key: String,
        children: List<NavNode> = emptyList(),
        parentKey: String? = null
    ) = StackNode(
        key = key,
        parentKey = parentKey,
        children = children
    )

    private fun createTabNode(
        key: String,
        stacks: List<StackNode>,
        activeStackIndex: Int = 0,
        parentKey: String? = null
    ) = TabNode(
        key = key,
        parentKey = parentKey,
        stacks = stacks,
        activeStackIndex = activeStackIndex
    )

    // =========================================================================
    // IDLE STATE TESTS
    // =========================================================================

    @Test
    fun `Idle state holds NavNode`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        assertEquals(screen, idle.current)
    }

    @Test
    fun `Idle state has no progress`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        assertEquals(0f, idle.progressValue)
    }

    @Test
    fun `Idle state direction is NONE`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        assertEquals(TransitionDirection.NONE, idle.direction)
    }

    @Test
    fun `Idle state isIdle returns true`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        assertTrue(idle.isIdle)
        assertFalse(idle.isAnimating)
        assertFalse(idle.isProposed)
    }

    @Test
    fun `Idle state animationComposablePair returns current and null`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        val (current, target) = idle.animationComposablePair()
        assertEquals(screen, current)
        assertNull(target)
    }

    @Test
    fun `Idle state effectiveTarget returns current`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        assertEquals(screen, idle.effectiveTarget)
    }

    @Test
    fun `Idle state does not affect any stack`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        assertFalse(idle.affectsStack("anyStack"))
    }

    @Test
    fun `Idle state does not affect any tab`() {
        val screen = createScreenNode("screen1")
        val idle = TransitionState.Idle(screen)

        assertFalse(idle.affectsTab("anyTab"))
    }

    // =========================================================================
    // PROPOSED STATE TESTS
    // =========================================================================

    @Test
    fun `Proposed state tracks gesture progress`() {
        val current = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val state = TransitionState.Proposed(
            current = current,
            proposed = proposed,
            progress = 0.5f
        )

        assertEquals(0.5f, state.progress)
        assertEquals(0.5f, state.progressValue)
    }

    @Test
    fun `Proposed state direction is BACKWARD`() {
        val current = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val state = TransitionState.Proposed(current, proposed, 0.5f)

        assertEquals(TransitionDirection.BACKWARD, state.direction)
    }

    @Test
    fun `Proposed state isProposed returns true`() {
        val current = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val state = TransitionState.Proposed(current, proposed)

        assertTrue(state.isProposed)
        assertFalse(state.isIdle)
        assertFalse(state.isAnimating)
    }

    @Test
    fun `Proposed withProgress clamps value to 0-1`() {
        val current = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val state = TransitionState.Proposed(current, proposed, 0.5f)

        val clamped1 = state.withProgress(1.5f)
        assertEquals(1.0f, clamped1.progress)

        val clamped2 = state.withProgress(-0.5f)
        assertEquals(0.0f, clamped2.progress)

        val normal = state.withProgress(0.7f)
        assertEquals(0.7f, normal.progress)
    }

    @Test
    fun `Proposed state progress must be between 0 and 1`() {
        val current = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")

        assertFailsWith<IllegalArgumentException> {
            TransitionState.Proposed(current, proposed, progress = 1.5f)
        }

        assertFailsWith<IllegalArgumentException> {
            TransitionState.Proposed(current, proposed, progress = -0.1f)
        }
    }

    @Test
    fun `Proposed state effectiveTarget returns proposed`() {
        val current = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val state = TransitionState.Proposed(current, proposed)

        assertEquals(proposed, state.effectiveTarget)
    }

    @Test
    fun `Proposed state animationComposablePair returns current and proposed`() {
        val current = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val state = TransitionState.Proposed(current, proposed)

        val (first, second) = state.animationComposablePair()
        assertEquals(current, first)
        assertEquals(proposed, second)
    }

    // =========================================================================
    // ANIMATING STATE TESTS
    // =========================================================================

    @Test
    fun `Animating state tracks animation progress`() {
        val current = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val state = TransitionState.Animating(
            current = current,
            target = target,
            progress = 0.75f,
            direction = TransitionDirection.FORWARD
        )

        assertEquals(0.75f, state.progress)
        assertEquals(0.75f, state.progressValue)
    }

    @Test
    fun `Animating state isAnimating returns true`() {
        val current = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val state = TransitionState.Animating(
            current = current,
            target = target,
            direction = TransitionDirection.FORWARD
        )

        assertTrue(state.isAnimating)
        assertFalse(state.isIdle)
        assertFalse(state.isProposed)
    }

    @Test
    fun `Animating withProgress clamps value to 0-1`() {
        val current = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val state = TransitionState.Animating(
            current = current,
            target = target,
            progress = 0.5f,
            direction = TransitionDirection.FORWARD
        )

        val clamped1 = state.withProgress(1.5f)
        assertEquals(1.0f, clamped1.progress)

        val clamped2 = state.withProgress(-0.5f)
        assertEquals(0.0f, clamped2.progress)
    }

    @Test
    fun `Animating complete returns Idle with target`() {
        val current = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val animating = TransitionState.Animating(
            current = current,
            target = target,
            progress = 1.0f,
            direction = TransitionDirection.FORWARD
        )

        val idle = animating.complete()

        assertTrue(idle is TransitionState.Idle)
        assertEquals(target, idle.current)
    }

    @Test
    fun `Animating state progress must be between 0 and 1`() {
        val current = createScreenNode("screen1")
        val target = createScreenNode("screen2")

        assertFailsWith<IllegalArgumentException> {
            TransitionState.Animating(current, target, progress = 1.1f, direction = TransitionDirection.FORWARD)
        }
    }

    @Test
    fun `Animating state effectiveTarget returns target`() {
        val current = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val state = TransitionState.Animating(
            current = current,
            target = target,
            direction = TransitionDirection.FORWARD
        )

        assertEquals(target, state.effectiveTarget)
    }

    // =========================================================================
    // AFFECTS STACK TESTS
    // =========================================================================

    @Test
    fun `affectsStack returns true when stack children changed`() {
        val screen1 = createScreenNode("s1", "stack")
        val screen2 = createScreenNode("s2", "stack")
        val oldStack = createStackNode("stack", listOf(screen1))
        val newStack = createStackNode("stack", listOf(screen1, screen2))

        val animating = TransitionState.Animating(
            current = oldStack,
            target = newStack,
            progress = 0.5f,
            direction = TransitionDirection.FORWARD
        )

        assertTrue(animating.affectsStack("stack"))
    }

    @Test
    fun `affectsStack returns false when stack unchanged`() {
        val screen1 = createScreenNode("s1", "stack")
        val stack = createStackNode("stack", listOf(screen1))

        val idle = TransitionState.Idle(stack)

        assertFalse(idle.affectsStack("stack"))
    }

    @Test
    fun `affectsStack returns false for non-existent stack`() {
        val screen1 = createScreenNode("s1", "stack")
        val screen2 = createScreenNode("s2", "stack")
        val oldStack = createStackNode("stack", listOf(screen1))
        val newStack = createStackNode("stack", listOf(screen1, screen2))

        val animating = TransitionState.Animating(
            current = oldStack,
            target = newStack,
            direction = TransitionDirection.FORWARD
        )

        assertFalse(animating.affectsStack("nonexistent"))
    }

    // =========================================================================
    // AFFECTS TAB TESTS
    // =========================================================================

    @Test
    fun `affectsTab returns true when tab index changed`() {
        val screen1 = createScreenNode("s1", "stack1")
        val screen2 = createScreenNode("s2", "stack2")
        val stack1 = createStackNode("stack1", listOf(screen1), "tab")
        val stack2 = createStackNode("stack2", listOf(screen2), "tab")

        val oldTab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 0)
        val newTab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 1)

        val animating = TransitionState.Animating(
            current = oldTab,
            target = newTab,
            direction = TransitionDirection.FORWARD
        )

        assertTrue(animating.affectsTab("tab"))
    }

    @Test
    fun `affectsTab returns false when tab index unchanged`() {
        val screen1 = createScreenNode("s1", "stack1")
        val screen2 = createScreenNode("s2", "stack2")
        val stack1 = createStackNode("stack1", listOf(screen1), "tab")
        val stack2 = createStackNode("stack2", listOf(screen2), "tab")

        val tab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 0)

        val idle = TransitionState.Idle(tab)

        assertFalse(idle.affectsTab("tab"))
    }

    @Test
    fun `affectsTab returns false for non-existent tab`() {
        val screen = createScreenNode("s1")
        val stack = createStackNode("stack", listOf(screen))

        val idle = TransitionState.Idle(stack)

        assertFalse(idle.affectsTab("nonexistent"))
    }

    // =========================================================================
    // PREVIOUS CHILD TESTS
    // =========================================================================

    @Test
    fun `previousChildOf returns the screen being covered`() {
        val screen1 = createScreenNode("s1", "stack")
        val screen2 = createScreenNode("s2", "stack")
        val screen3 = createScreenNode("s3", "stack")
        val stack = createStackNode("stack", listOf(screen1, screen2, screen3))

        val animating = TransitionState.Animating(
            current = stack,
            target = stack, // Target doesn't matter for previousChildOf
            direction = TransitionDirection.BACKWARD
        )

        val previous = animating.previousChildOf("stack")
        assertEquals(screen2, previous)
    }

    @Test
    fun `previousChildOf returns null when stack has only one child`() {
        val screen1 = createScreenNode("s1", "stack")
        val stack = createStackNode("stack", listOf(screen1))

        val animating = TransitionState.Animating(
            current = stack,
            target = stack,
            direction = TransitionDirection.BACKWARD
        )

        assertNull(animating.previousChildOf("stack"))
    }

    @Test
    fun `previousChildOf returns null for Idle state`() {
        val screen1 = createScreenNode("s1", "stack")
        val screen2 = createScreenNode("s2", "stack")
        val stack = createStackNode("stack", listOf(screen1, screen2))

        val idle = TransitionState.Idle(stack)

        assertNull(idle.previousChildOf("stack"))
    }

    // =========================================================================
    // PREVIOUS TAB INDEX TESTS
    // =========================================================================

    @Test
    fun `previousTabIndex returns previous tab index during switch`() {
        val screen1 = createScreenNode("s1", "stack1")
        val screen2 = createScreenNode("s2", "stack2")
        val stack1 = createStackNode("stack1", listOf(screen1), "tab")
        val stack2 = createStackNode("stack2", listOf(screen2), "tab")

        val oldTab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 0)
        val newTab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 1)

        val animating = TransitionState.Animating(
            current = oldTab,
            target = newTab,
            direction = TransitionDirection.FORWARD
        )

        assertEquals(0, animating.previousTabIndex("tab"))
    }

    @Test
    fun `previousTabIndex returns null for Idle state`() {
        val screen1 = createScreenNode("s1", "stack1")
        val stack1 = createStackNode("stack1", listOf(screen1), "tab")
        val tab = createTabNode("tab", listOf(stack1), activeStackIndex = 0)

        val idle = TransitionState.Idle(tab)

        assertNull(idle.previousTabIndex("tab"))
    }

    @Test
    fun `previousTabIndex returns null for non-existent tab`() {
        val screen = createScreenNode("s1")
        val stack = createStackNode("stack", listOf(screen))

        val animating = TransitionState.Animating(
            current = stack,
            target = stack,
            direction = TransitionDirection.FORWARD
        )

        assertNull(animating.previousTabIndex("nonexistent"))
    }

    // =========================================================================
    // INTRA-TAB NAVIGATION TESTS
    // =========================================================================

    @Test
    fun `isIntraTabNavigation returns true when navigating within same tab`() {
        val screen1 = createScreenNode("s1", "stack1")
        val screen2 = createScreenNode("s2", "stack1")
        val stack1 = createStackNode("stack1", listOf(screen1), "tab")
        val stack1Updated = createStackNode("stack1", listOf(screen1, screen2), "tab")
        val stack2 = createStackNode("stack2", emptyList(), "tab")

        val oldTab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 0)
        val newTab = createTabNode("tab", listOf(stack1Updated, stack2), activeStackIndex = 0)

        val animating = TransitionState.Animating(
            current = oldTab,
            target = newTab,
            direction = TransitionDirection.FORWARD
        )

        assertTrue(animating.isIntraTabNavigation("tab"))
    }

    @Test
    fun `isIntraTabNavigation returns false when switching tabs`() {
        val screen1 = createScreenNode("s1", "stack1")
        val screen2 = createScreenNode("s2", "stack2")
        val stack1 = createStackNode("stack1", listOf(screen1), "tab")
        val stack2 = createStackNode("stack2", listOf(screen2), "tab")

        val oldTab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 0)
        val newTab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 1)

        val animating = TransitionState.Animating(
            current = oldTab,
            target = newTab,
            direction = TransitionDirection.FORWARD
        )

        assertFalse(animating.isIntraTabNavigation("tab"))
    }

    // =========================================================================
    // CROSS NODE TYPE NAVIGATION TESTS
    // =========================================================================

    @Test
    fun `isCrossNodeTypeNavigation returns false when same node type`() {
        val screen1 = createScreenNode("s1")
        val screen2 = createScreenNode("s2")

        val animating = TransitionState.Animating(
            current = screen1,
            target = screen2,
            direction = TransitionDirection.FORWARD
        )

        assertFalse(animating.isCrossNodeTypeNavigation())
    }

    @Test
    fun `isCrossNodeTypeNavigation returns true when different node types`() {
        val screen = createScreenNode("s1")
        val stack = createStackNode("stack1", listOf(screen))

        val animating = TransitionState.Animating(
            current = screen,
            target = stack,
            direction = TransitionDirection.FORWARD
        )

        assertTrue(animating.isCrossNodeTypeNavigation())
    }

    // =========================================================================
    // TRANSITION STATE MANAGER TESTS
    // =========================================================================

    @Test
    fun `TransitionStateManager starts in Idle state`() {
        val screen = createScreenNode("screen1")
        val manager = TransitionStateManager(screen)

        assertTrue(manager.currentState is TransitionState.Idle)
        assertEquals(screen, manager.currentState.current)
    }

    @Test
    fun `TransitionStateManager enforces state machine - Idle to Animating`() {
        val screen = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        // Valid: Idle → Animating
        manager.startAnimation(target, TransitionDirection.FORWARD)

        assertTrue(manager.currentState.isAnimating)
        val animating = manager.currentState as TransitionState.Animating
        assertEquals(screen, animating.current)
        assertEquals(target, animating.target)
        assertEquals(TransitionDirection.FORWARD, animating.direction)
    }

    @Test
    fun `TransitionStateManager enforces state machine - Idle to Proposed`() {
        val screen = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        // Valid: Idle → Proposed
        manager.startProposed(proposed)

        assertTrue(manager.currentState.isProposed)
        val state = manager.currentState as TransitionState.Proposed
        assertEquals(screen, state.current)
        assertEquals(proposed, state.proposed)
    }

    @Test
    fun `TransitionStateManager enforces state machine - Proposed to Animating`() {
        val screen = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        manager.startProposed(proposed)
        manager.updateProgress(0.5f)

        // Valid: Proposed → Animating
        manager.commitProposed()

        assertTrue(manager.currentState.isAnimating)
        val animating = manager.currentState as TransitionState.Animating
        assertEquals(0.5f, animating.progress) // Progress preserved
        assertEquals(TransitionDirection.BACKWARD, animating.direction)
    }

    @Test
    fun `TransitionStateManager enforces state machine - Proposed to Idle (cancel)`() {
        val screen = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        manager.startProposed(proposed)

        // Valid: Proposed → Idle (cancel)
        manager.cancelProposed()

        assertTrue(manager.currentState.isIdle)
        assertEquals(screen, manager.currentState.current)
    }

    @Test
    fun `TransitionStateManager enforces state machine - Animating to Idle`() {
        val screen = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        manager.startAnimation(target, TransitionDirection.FORWARD)
        manager.updateProgress(1.0f)

        // Valid: Animating → Idle
        manager.completeAnimation()

        assertTrue(manager.currentState.isIdle)
        assertEquals(target, manager.currentState.current)
    }

    @Test
    fun `TransitionStateManager prevents invalid transition - Animating to Proposed`() {
        val screen = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val proposed = createScreenNode("screen3")
        val manager = TransitionStateManager(screen)

        manager.startAnimation(target, TransitionDirection.FORWARD)

        // Invalid: Animating → Proposed
        assertFailsWith<IllegalArgumentException> {
            manager.startProposed(proposed)
        }
    }

    @Test
    fun `TransitionStateManager prevents invalid transition - Animating to Animating`() {
        val screen = createScreenNode("screen1")
        val target1 = createScreenNode("screen2")
        val target2 = createScreenNode("screen3")
        val manager = TransitionStateManager(screen)

        manager.startAnimation(target1, TransitionDirection.FORWARD)

        // Invalid: Animating → Animating
        assertFailsWith<IllegalArgumentException> {
            manager.startAnimation(target2, TransitionDirection.FORWARD)
        }
    }

    @Test
    fun `TransitionStateManager prevents invalid transition - Idle to commitProposed`() {
        val screen = createScreenNode("screen1")
        val manager = TransitionStateManager(screen)

        // Invalid: Idle → commitProposed (need to be in Proposed first)
        assertFailsWith<IllegalArgumentException> {
            manager.commitProposed()
        }
    }

    @Test
    fun `TransitionStateManager prevents invalid transition - Idle to cancelProposed`() {
        val screen = createScreenNode("screen1")
        val manager = TransitionStateManager(screen)

        // Invalid: Idle → cancelProposed (need to be in Proposed first)
        assertFailsWith<IllegalArgumentException> {
            manager.cancelProposed()
        }
    }

    @Test
    fun `TransitionStateManager prevents invalid transition - Idle to completeAnimation`() {
        val screen = createScreenNode("screen1")
        val manager = TransitionStateManager(screen)

        // Invalid: Idle → completeAnimation (need to be in Animating first)
        assertFailsWith<IllegalArgumentException> {
            manager.completeAnimation()
        }
    }

    @Test
    fun `TransitionStateManager updateProgress works for Proposed state`() {
        val screen = createScreenNode("screen1")
        val proposed = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        manager.startProposed(proposed)
        manager.updateProgress(0.75f)

        val state = manager.currentState as TransitionState.Proposed
        assertEquals(0.75f, state.progress)
    }

    @Test
    fun `TransitionStateManager updateProgress works for Animating state`() {
        val screen = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        manager.startAnimation(target, TransitionDirection.FORWARD)
        manager.updateProgress(0.8f)

        val state = manager.currentState as TransitionState.Animating
        assertEquals(0.8f, state.progress)
    }

    @Test
    fun `TransitionStateManager updateProgress has no effect on Idle state`() {
        val screen = createScreenNode("screen1")
        val manager = TransitionStateManager(screen)

        manager.updateProgress(0.5f)

        assertTrue(manager.currentState.isIdle)
    }

    @Test
    fun `TransitionStateManager forceIdle works from any state`() {
        val screen1 = createScreenNode("screen1")
        val screen2 = createScreenNode("screen2")
        val screen3 = createScreenNode("screen3")
        val manager = TransitionStateManager(screen1)

        // Force from Idle
        manager.forceIdle(screen2)
        assertTrue(manager.currentState.isIdle)
        assertEquals(screen2, manager.currentState.current)

        // Force from Animating
        manager.startAnimation(screen3, TransitionDirection.FORWARD)
        manager.forceIdle(screen1)
        assertTrue(manager.currentState.isIdle)
        assertEquals(screen1, manager.currentState.current)
    }

    @Test
    fun `TransitionStateManager state flow emits updates`() {
        val screen = createScreenNode("screen1")
        val target = createScreenNode("screen2")
        val manager = TransitionStateManager(screen)

        // Initial state
        assertTrue(manager.state.value.isIdle)

        // Start animation
        manager.startAnimation(target, TransitionDirection.FORWARD)
        assertTrue(manager.state.value.isAnimating)

        // Complete
        manager.completeAnimation()
        assertTrue(manager.state.value.isIdle)
        assertEquals(target, manager.state.value.current)
    }

    // =========================================================================
    // NESTED STRUCTURE TESTS
    // =========================================================================

    @Test
    fun `query methods work correctly for nested structures`() {
        // Create a nested structure: Tab -> Stack -> Screen
        val screen1 = createScreenNode("s1", "stack1")
        val screen2 = createScreenNode("s2", "stack1")
        val screen3 = createScreenNode("s3", "stack2")

        val stack1 = createStackNode("stack1", listOf(screen1, screen2), "tab")
        val stack2 = createStackNode("stack2", listOf(screen3), "tab")

        val tab = createTabNode("tab", listOf(stack1, stack2), activeStackIndex = 0)

        // Push a new screen to stack1
        val screen4 = createScreenNode("s4", "stack1")
        val stack1Updated = createStackNode("stack1", listOf(screen1, screen2, screen4), "tab")
        val tabUpdated = createTabNode("tab", listOf(stack1Updated, stack2), activeStackIndex = 0)

        val animating = TransitionState.Animating(
            current = tab,
            target = tabUpdated,
            direction = TransitionDirection.FORWARD
        )

        // Should affect stack1 (children changed)
        assertTrue(animating.affectsStack("stack1"))
        // Should not affect stack2 (unchanged)
        assertFalse(animating.affectsStack("stack2"))
        // Should not affect tab index (same active stack)
        assertFalse(animating.affectsTab("tab"))
        // Previous child of stack1 should be screen2
        val previous = animating.previousChildOf("stack1")
        assertNotNull(previous)
        assertEquals("s2", previous.key)
    }

    // =========================================================================
    // BACKWARD COMPATIBILITY EXTENSION TESTS
    // =========================================================================

    @Test
    fun `backward compatible isAnimating extension works`() {
        val screen = createScreenNode("screen1")
        val target = createScreenNode("screen2")

        val idle = TransitionState.Idle(screen)
        assertFalse(idle.isAnimating)

        val proposed = TransitionState.Proposed(screen, target)
        assertFalse(proposed.isAnimating)

        val animating = TransitionState.Animating(screen, target, direction = TransitionDirection.FORWARD)
        assertTrue(animating.isAnimating)
    }

    @Test
    fun `backward compatible progress extension works`() {
        val screen = createScreenNode("screen1")
        val target = createScreenNode("screen2")

        val idle: TransitionState = TransitionState.Idle(screen)
        assertEquals(0f, idle.progress)

        val proposed: TransitionState = TransitionState.Proposed(screen, target, 0.3f)
        assertEquals(0.3f, proposed.progress)

        val animating: TransitionState = TransitionState.Animating(screen, target, 0.7f, TransitionDirection.FORWARD)
        assertEquals(0.7f, animating.progress)
    }
}
