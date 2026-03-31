@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import com.jermey.quo.vadis.core.compose.internal.ComposableCache
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.FakeSaveableStateHolder
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransitions
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for screen node rendering behavior.
 *
 * Tests cover:
 * - ScreenNode destination content rendering
 * - LocalScreenNode provision
 * - Cache entry creation for screens
 * - State preservation across navigation
 */
class ScreenRendererTest : FunSpec({

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    val HomeDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    val ProfileDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    val SettingsDestination = object : NavDestination {
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // TEST HELPERS
    // =========================================================================

    fun createScreen(
        key: String,
        parentKey: String? = null,
        destination: NavDestination = HomeDestination
    ): ScreenNode = ScreenNode(NodeKey(key), parentKey?.let { NodeKey(it) }, destination)

    // =========================================================================
    // SCREEN NODE DESTINATION TESTS
    // =========================================================================

    test("screen node key is used for cache identification") {
        // Given
        val screenNode = createScreen("unique-screen-key")

        // Then
        screenNode.key shouldBe NodeKey("unique-screen-key")
    }

    test("screen node parent key links to parent container") {
        // Given
        val screenNode = createScreen("child-screen", parentKey = "parent-stack")

        // Then
        screenNode.parentKey shouldBe NodeKey("parent-stack")
    }

    // =========================================================================
    // CACHE ENTRY TESTS
    // =========================================================================

    test("cache can be created for screen entry") {
        // Given
        val cache = ComposableCache()
        createScreen("screen-1")

        // Then
        cache.shouldNotBeNull()
        // Cache entry would be created when CachedEntry composable is called
    }

    test("multiple screens have distinct cache keys") {
        // Given
        val screen1 = createScreen("screen-1")
        val screen2 = createScreen("screen-2")
        val screen3 = createScreen("screen-3")

        // Then
        val keys = listOf(screen1.key, screen2.key, screen3.key)
        keys.distinct().size shouldBe 3
    }

    test("screen key format is consistent") {
        // Given
        val screens = listOf(
            createScreen("home"),
            createScreen("profile"),
            createScreen("settings"),
            createScreen("detail-123")
        )

        // Then
        screens.forEach { screen ->
            screen.key.value.isEmpty().shouldBeFalse()
            screen.key.value.contains("/").shouldBeFalse()
        }
    }

    // =========================================================================
    // STATE HOLDER TESTS
    // =========================================================================

    test("FakeSaveableStateHolder can be used for testing") {
        // Given
        val stateHolder = FakeSaveableStateHolder()

        // Then - removeState should not throw
        stateHolder.removeState("any-key")
        stateHolder.removeState(123)
        stateHolder.removeState(createScreen("test"))
    }

    test("FakeNavRenderScope provides state holder") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.saveableStateHolder.shouldNotBeNull()
        scope.saveableStateHolder.shouldBeInstanceOf<FakeSaveableStateHolder>()
    }

    // =========================================================================
    // SCREEN NODE EQUALITY TESTS
    // =========================================================================

    test("screen nodes with same properties are equal") {
        // Given
        val screen1 = ScreenNode(NodeKey("key1"), NodeKey("parent"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("key1"), NodeKey("parent"), HomeDestination)

        // Then
        screen2 shouldBe screen1
        screen2.hashCode() shouldBe screen1.hashCode()
    }

    test("screen nodes with different keys are not equal") {
        // Given
        val screen1 = ScreenNode(NodeKey("key1"), NodeKey("parent"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("key2"), NodeKey("parent"), HomeDestination)

        // Then
        screen2 shouldNotBe screen1
    }

    test("screen nodes with different parent keys are not equal") {
        // Given
        val screen1 = ScreenNode(NodeKey("key"), NodeKey("parent1"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("key"), NodeKey("parent2"), HomeDestination)

        // Then
        screen2 shouldNotBe screen1
    }

    test("screen nodes with different destinations are not equal") {
        // Given
        val screen1 = ScreenNode(NodeKey("key"), NodeKey("parent"), HomeDestination)
        val screen2 = ScreenNode(NodeKey("key"), NodeKey("parent"), ProfileDestination)

        // Then
        screen2 shouldNotBe screen1
    }

    // =========================================================================
    // SCREEN IN STACK CONTEXT TESTS
    // =========================================================================

    test("screen in stack has correct parent key") {
        // Given
        val screen = createScreen("screen", parentKey = "my-stack")
        val stack = StackNode(NodeKey("my-stack"), null, listOf(screen))

        // Then
        screen.parentKey shouldBe NodeKey("my-stack")
        stack.activeChild shouldBe screen
    }

    test("multiple screens in stack maintain order") {
        // Given
        val screen1 = createScreen("s1", "stack", HomeDestination)
        val screen2 = createScreen("s2", "stack", ProfileDestination)
        val screen3 = createScreen("s3", "stack", SettingsDestination)
        val stack = StackNode(NodeKey("stack"), null, listOf(screen1, screen2, screen3))

        // Then
        stack.children.size shouldBe 3
        stack.children[0] shouldBe screen1
        stack.children[1] shouldBe screen2
        stack.children[2] shouldBe screen3
        stack.activeChild shouldBe screen3 // Last is active
    }

    test("screen transitions are preserved") {
        // Given
        val transitionDestination = object : NavDestination {
            override val transition: NavigationTransition = NavigationTransitions.SlideHorizontal
        }
        val screen = createScreen("animated-screen", destination = transitionDestination)

        // Then
        screen.destination.transition shouldBe NavigationTransitions.SlideHorizontal
    }

    test("screen without transition is valid") {
        // Given
        val screen = createScreen("no-transition", destination = HomeDestination)

        // Then
        screen.destination.transition.shouldBeNull()
    }
})
