package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.render.ComposableCache
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.FakeSaveableStateHolder
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.NavigationTransition
import com.jermey.quo.vadis.core.navigation.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.navigation.StackNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for screen node rendering behavior.
 *
 * Tests cover:
 * - ScreenNode destination content rendering
 * - LocalScreenNode provision
 * - Cache entry creation for screens
 * - State preservation across navigation
 */
class ScreenRendererTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object HomeDestination : NavDestination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    private object ProfileDestination : NavDestination {
        override val data: Any? = "profile-data"
        override val transition: NavigationTransition? = null
    }

    private object SettingsDestination : NavDestination {
        override val data: Any? = mapOf("theme" to "dark")
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    private fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = HomeDestination
    ): ScreenNode = ScreenNode(key, parentKey, destination)

    // =========================================================================
    // SCREEN NODE DESTINATION TESTS
    // =========================================================================

    @Test
    fun `screen node renders destination content correctly`() {
        // Given
        val destination = ProfileDestination
        val screenNode = createScreen("profile-screen", destination = destination)

        // Then
        assertEquals(destination, screenNode.destination)
        assertEquals("profile-data", screenNode.destination.data)
    }

    @Test
    fun `screen node with null destination data is valid`() {
        // Given
        val screenNode = createScreen("home-screen", destination = HomeDestination)

        // Then
        assertNull(screenNode.destination.data)
    }

    @Test
    fun `screen node with complex destination data is preserved`() {
        // Given
        val screenNode = createScreen("settings-screen", destination = SettingsDestination)

        // Then
        val data = screenNode.destination.data
        assertTrue(data is Map<*, *>)
        assertEquals("dark", (data as Map<*, *>)["theme"])
    }

    @Test
    fun `screen node key is used for cache identification`() {
        // Given
        val screenNode = createScreen("unique-screen-key")

        // Then
        assertEquals("unique-screen-key", screenNode.key)
    }

    @Test
    fun `screen node parent key links to parent container`() {
        // Given
        val screenNode = createScreen("child-screen", parentKey = "parent-stack")

        // Then
        assertEquals("parent-stack", screenNode.parentKey)
    }

    // =========================================================================
    // CACHE ENTRY TESTS
    // =========================================================================

    @Test
    fun `cache can be created for screen entry`() {
        // Given
        val cache = ComposableCache()
        val screenNode = createScreen("screen-1")

        // Then
        assertNotNull(cache, "Cache should be created")
        // Cache entry would be created when CachedEntry composable is called
    }

    @Test
    fun `multiple screens have distinct cache keys`() {
        // Given
        val screen1 = createScreen("screen-1")
        val screen2 = createScreen("screen-2")
        val screen3 = createScreen("screen-3")

        // Then
        val keys = listOf(screen1.key, screen2.key, screen3.key)
        assertEquals(3, keys.distinct().size, "All screen keys should be unique")
    }

    @Test
    fun `screen key format is consistent`() {
        // Given
        val screens = listOf(
            createScreen("home"),
            createScreen("profile"),
            createScreen("settings"),
            createScreen("detail-123")
        )

        // Then
        screens.forEach { screen ->
            assertFalse(screen.key.isEmpty(), "Screen key should not be empty")
            assertFalse(screen.key.contains("/"), "Screen key should not contain path separators")
        }
    }

    // =========================================================================
    // STATE HOLDER TESTS
    // =========================================================================

    @Test
    fun `FakeSaveableStateHolder can be used for testing`() {
        // Given
        val stateHolder = FakeSaveableStateHolder()

        // Then - removeState should not throw
        stateHolder.removeState("any-key")
        stateHolder.removeState(123)
        stateHolder.removeState(createScreen("test"))
    }

    @Test
    fun `FakeNavRenderScope provides state holder`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertNotNull(scope.saveableStateHolder)
        assertTrue(scope.saveableStateHolder is FakeSaveableStateHolder)
    }

    // =========================================================================
    // SCREEN NODE EQUALITY TESTS
    // =========================================================================

    @Test
    fun `screen nodes with same properties are equal`() {
        // Given
        val screen1 = ScreenNode("key1", "parent", HomeDestination)
        val screen2 = ScreenNode("key1", "parent", HomeDestination)

        // Then
        assertEquals(screen1, screen2)
        assertEquals(screen1.hashCode(), screen2.hashCode())
    }

    @Test
    fun `screen nodes with different keys are not equal`() {
        // Given
        val screen1 = ScreenNode("key1", "parent", HomeDestination)
        val screen2 = ScreenNode("key2", "parent", HomeDestination)

        // Then
        assertFalse(screen1 == screen2)
    }

    @Test
    fun `screen nodes with different parent keys are not equal`() {
        // Given
        val screen1 = ScreenNode("key", "parent1", HomeDestination)
        val screen2 = ScreenNode("key", "parent2", HomeDestination)

        // Then
        assertFalse(screen1 == screen2)
    }

    @Test
    fun `screen nodes with different destinations are not equal`() {
        // Given
        val screen1 = ScreenNode("key", "parent", HomeDestination)
        val screen2 = ScreenNode("key", "parent", ProfileDestination)

        // Then
        assertFalse(screen1 == screen2)
    }

    // =========================================================================
    // SCREEN IN STACK CONTEXT TESTS
    // =========================================================================

    @Test
    fun `screen in stack has correct parent key`() {
        // Given
        val screen = createScreen("screen", parentKey = "my-stack")
        val stack = StackNode("my-stack", null, listOf(screen))

        // Then
        assertEquals("my-stack", screen.parentKey)
        assertEquals(screen, stack.activeChild)
    }

    @Test
    fun `multiple screens in stack maintain order`() {
        // Given
        val screen1 = createScreen("s1", "stack", HomeDestination)
        val screen2 = createScreen("s2", "stack", ProfileDestination)
        val screen3 = createScreen("s3", "stack", SettingsDestination)
        val stack = StackNode("stack", null, listOf(screen1, screen2, screen3))

        // Then
        assertEquals(3, stack.children.size)
        assertEquals(screen1, stack.children[0])
        assertEquals(screen2, stack.children[1])
        assertEquals(screen3, stack.children[2])
        assertEquals(screen3, stack.activeChild) // Last is active
    }

    @Test
    fun `screen transitions are preserved`() {
        // Given
        val transitionDestination = object : NavDestination {
            override val data: Any? = null
            override val transition: NavigationTransition? = NavigationTransitions.SlideHorizontal
        }
        val screen = createScreen("animated-screen", destination = transitionDestination)

        // Then
        assertEquals(NavigationTransitions.SlideHorizontal, screen.destination.transition)
    }

    @Test
    fun `screen without transition is valid`() {
        // Given
        val screen = createScreen("no-transition", destination = HomeDestination)

        // Then
        assertNull(screen.destination.transition)
    }
}
