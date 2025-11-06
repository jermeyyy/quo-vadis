package com.jermey.quo.vadis.core.navigation.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for Navigator's child delegation functionality.
 *
 * Verifies that:
 * - Back press events are properly delegated to child navigators
 * - Parent navigator handles back when child doesn't consume
 * - Child can be set and cleared
 */
class NavigatorChildDelegationTest {

    private object RootDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object DetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ChildRootDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ChildDetailDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    /**
     * Simple fake BackPressHandler for testing.
     */
    private class FakeBackPressHandler(
        private val shouldConsume: Boolean = true
    ) : BackPressHandler {
        var backPressCount = 0

        override fun onBack(): Boolean {
            backPressCount++
            return shouldConsume
        }
    }

    @Test
    fun `navigator with no child handles back itself`() {
        val navigator = DefaultNavigator()
        navigator.setStartDestination(RootDestination)
        navigator.navigate(DetailDestination)

        // No child set, navigator should handle back itself
        val consumed = navigator.navigateBack()

        assertTrue(consumed)
        assertEquals(RootDestination, navigator.currentDestination.value)
    }

    @Test
    fun `navigator delegates to child when child consumes`() {
        val navigator = DefaultNavigator()
        navigator.setStartDestination(RootDestination)
        navigator.navigate(DetailDestination)

        val child = FakeBackPressHandler(shouldConsume = true)
        navigator.setActiveChild(child)

        // Child consumes, parent should not pop
        val consumed = navigator.navigateBack()

        assertTrue(consumed)
        assertEquals(1, child.backPressCount)
        assertEquals(DetailDestination, navigator.currentDestination.value) // Parent didn't pop
    }

    @Test
    fun `navigator handles back when child does not consume`() {
        val navigator = DefaultNavigator()
        navigator.setStartDestination(RootDestination)
        navigator.navigate(DetailDestination)

        val child = FakeBackPressHandler(shouldConsume = false)
        navigator.setActiveChild(child)

        // Child doesn't consume, parent should handle
        val consumed = navigator.navigateBack()

        assertTrue(consumed)
        assertEquals(1, child.backPressCount)
        assertEquals(RootDestination, navigator.currentDestination.value) // Parent did pop
    }

    @Test
    fun `setActiveChild null clears delegation`() {
        val navigator = DefaultNavigator()
        navigator.setStartDestination(RootDestination)
        navigator.navigate(DetailDestination)

        val child = FakeBackPressHandler(shouldConsume = true)
        navigator.setActiveChild(child)
        navigator.setActiveChild(null) // Clear child

        // No child, navigator should handle itself
        val consumed = navigator.navigateBack()

        assertTrue(consumed)
        assertEquals(0, child.backPressCount) // Child not called
        assertEquals(RootDestination, navigator.currentDestination.value)
    }

    @Test
    fun `child navigator can be replaced`() {
        val navigator = DefaultNavigator()
        navigator.setStartDestination(RootDestination)
        navigator.navigate(DetailDestination)

        val child1 = FakeBackPressHandler(shouldConsume = true)
        val child2 = FakeBackPressHandler(shouldConsume = false)

        navigator.setActiveChild(child1)
        navigator.navigateBack()

        assertEquals(1, child1.backPressCount)
        assertEquals(0, child2.backPressCount)

        // Replace child
        navigator.setActiveChild(child2)
        navigator.navigateBack()

        assertEquals(1, child1.backPressCount) // Not called again
        assertEquals(1, child2.backPressCount) // Called once
    }

    @Test
    fun `nested navigation with real child navigator`() {
        // Parent navigator
        val parentNav = DefaultNavigator()
        parentNav.setStartDestination(RootDestination)
        parentNav.navigate(DetailDestination)

        // Child navigator with its own stack
        val childNav = DefaultNavigator()
        childNav.setStartDestination(ChildRootDestination)
        childNav.navigate(ChildDetailDestination)

        // Set child as active
        parentNav.setActiveChild(childNav)

        // Back press 1: Child should handle (pop from child stack)
        val consumed1 = parentNav.navigateBack()
        assertTrue(consumed1)
        assertEquals(ChildRootDestination, childNav.currentDestination.value)
        assertEquals(DetailDestination, parentNav.currentDestination.value) // Parent unchanged

        // Back press 2: Child at root, so parent should handle
        val consumed2 = parentNav.navigateBack()
        assertTrue(consumed2)
        assertEquals(ChildRootDestination, childNav.currentDestination.value) // Child unchanged
        assertEquals(RootDestination, parentNav.currentDestination.value) // Parent popped
    }

    @Test
    fun `parent navigator returns false when both parent and child are at root`() {
        val parentNav = DefaultNavigator()
        parentNav.setStartDestination(RootDestination)

        val childNav = DefaultNavigator()
        childNav.setStartDestination(ChildRootDestination)

        parentNav.setActiveChild(childNav)

        // Both at root, should not consume
        val consumed = parentNav.navigateBack()

        assertFalse(consumed)
        assertEquals(RootDestination, parentNav.currentDestination.value)
        assertEquals(ChildRootDestination, childNav.currentDestination.value)
    }

    @Test
    fun `multiple levels of nesting work correctly`() {
        // Level 1 (root)
        val level1Nav = DefaultNavigator()
        level1Nav.setStartDestination(RootDestination)
        level1Nav.navigate(DetailDestination)

        // Level 2 (child of level 1)
        val level2Nav = DefaultNavigator()
        level2Nav.setStartDestination(ChildRootDestination)
        level2Nav.navigate(ChildDetailDestination)

        // Level 3 (child of level 2)
        val level3Nav = DefaultNavigator()
        level3Nav.setStartDestination(RootDestination)
        level3Nav.navigate(DetailDestination)

        // Set up hierarchy
        level2Nav.setActiveChild(level3Nav)
        level1Nav.setActiveChild(level2Nav)

        // Back press 1: Level 3 handles
        assertTrue(level1Nav.navigateBack())
        assertEquals(RootDestination, level3Nav.currentDestination.value)
        assertEquals(ChildDetailDestination, level2Nav.currentDestination.value)
        assertEquals(DetailDestination, level1Nav.currentDestination.value)

        // Back press 2: Level 3 can't handle, level 2 handles
        assertTrue(level1Nav.navigateBack())
        assertEquals(RootDestination, level3Nav.currentDestination.value)
        assertEquals(ChildRootDestination, level2Nav.currentDestination.value)
        assertEquals(DetailDestination, level1Nav.currentDestination.value)

        // Back press 3: Level 2 can't handle, level 1 handles
        assertTrue(level1Nav.navigateBack())
        assertEquals(RootDestination, level3Nav.currentDestination.value)
        assertEquals(ChildRootDestination, level2Nav.currentDestination.value)
        assertEquals(RootDestination, level1Nav.currentDestination.value)

        // Back press 4: All at root, should pass to system
        assertFalse(level1Nav.navigateBack())
    }
}
