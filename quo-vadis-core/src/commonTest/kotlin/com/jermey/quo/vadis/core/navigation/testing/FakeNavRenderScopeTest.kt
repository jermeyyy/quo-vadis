package com.jermey.quo.vadis.core.navigation.testing

import com.jermey.quo.vadis.core.navigation.compose.render.ComposableCache
import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.navback.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.route
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [FakeNavRenderScope] test utility.
 *
 * Tests verify that:
 * - The fake can be instantiated with defaults
 * - Properties can be customized
 * - FakeSaveableStateHolder works correctly
 * - EmptyScreenRegistry works correctly
 * - Helper factory methods work
 */
class FakeNavRenderScopeTest {

    // =========================================================================
    // TEST DESTINATIONS
    // =========================================================================

    private object TestDestination : Destination {
        override val data: Any? = null
        override val transition: NavigationTransition? = null
    }

    // =========================================================================
    // INSTANTIATION TESTS
    // =========================================================================

    @Test
    fun `FakeNavRenderScope can be created with defaults`() {
        // When
        val scope = FakeNavRenderScope()

        // Then
        assertNotNull(scope.navigator)
        assertNotNull(scope.cache)
        assertNotNull(scope.saveableStateHolder)
        assertNotNull(scope.animationCoordinator)
        assertNotNull(scope.predictiveBackController)
        assertNotNull(scope.screenRegistry)
        assertNotNull(scope.containerRegistry)
        assertNull(scope.sharedTransitionScope)
    }

    @Test
    fun `FakeNavRenderScope uses FakeNavigator by default`() {
        // When
        val scope = FakeNavRenderScope()

        // Then
        assertTrue(scope.navigator is FakeNavigator)
    }

    @Test
    fun `FakeNavRenderScope uses FakeSaveableStateHolder by default`() {
        // When
        val scope = FakeNavRenderScope()

        // Then
        assertTrue(scope.saveableStateHolder is FakeSaveableStateHolder)
    }

    @Test
    fun `FakeNavRenderScope uses AnimationCoordinator Default by default`() {
        // When
        val scope = FakeNavRenderScope()

        // Then
        assertSame(AnimationCoordinator.Default, scope.animationCoordinator)
    }

    @Test
    fun `FakeNavRenderScope uses EmptyScreenRegistry by default`() {
        // When
        val scope = FakeNavRenderScope()

        // Then
        assertSame(EmptyScreenRegistry, scope.screenRegistry)
    }

    @Test
    fun `FakeNavRenderScope uses ContainerRegistry Empty by default`() {
        // When
        val scope = FakeNavRenderScope()

        // Then
        assertSame(ContainerRegistry.Empty, scope.containerRegistry)
    }

    // =========================================================================
    // CUSTOMIZATION TESTS
    // =========================================================================

    @Test
    fun `FakeNavRenderScope accepts custom navigator`() {
        // Given
        val customNavigator = FakeNavigator()

        // When
        val scope = FakeNavRenderScope(navigator = customNavigator)

        // Then
        assertSame(customNavigator, scope.navigator)
    }

    @Test
    fun `FakeNavRenderScope accepts custom cache`() {
        // Given
        val customCache = ComposableCache(maxCacheSize = 10)

        // When
        val scope = FakeNavRenderScope(cache = customCache)

        // Then
        assertSame(customCache, scope.cache)
    }

    @Test
    fun `FakeNavRenderScope accepts custom saveableStateHolder`() {
        // Given
        val customStateHolder = FakeSaveableStateHolder()

        // When
        val scope = FakeNavRenderScope(saveableStateHolder = customStateHolder)

        // Then
        assertSame(customStateHolder, scope.saveableStateHolder)
    }

    @Test
    fun `FakeNavRenderScope accepts custom animationCoordinator`() {
        // Given
        val customCoordinator = AnimationCoordinator()

        // When
        val scope = FakeNavRenderScope(animationCoordinator = customCoordinator)

        // Then
        assertSame(customCoordinator, scope.animationCoordinator)
    }

    @Test
    fun `FakeNavRenderScope accepts custom predictiveBackController`() {
        // Given
        val customController = PredictiveBackController()

        // When
        val scope = FakeNavRenderScope(predictiveBackController = customController)

        // Then
        assertSame(customController, scope.predictiveBackController)
    }

    // =========================================================================
    // FACTORY METHOD TESTS
    // =========================================================================

    @Test
    fun `withFakeNavigator returns scope and navigator pair`() {
        // When
        val (scope, navigator) = FakeNavRenderScope.withFakeNavigator()

        // Then
        assertNotNull(scope)
        assertNotNull(navigator)
        assertSame(navigator, scope.navigator)
    }

    @Ignore
    @Test
    fun `withFakeNavigator navigator is usable for verification`() {
        // Given
        val (scope, navigator) = FakeNavRenderScope.withFakeNavigator()

        // When
        scope.navigator.navigate(TestDestination)

        // Then
        assertTrue(navigator.verifyNavigateTo(TestDestination.route))
    }

    // =========================================================================
    // FAKE SAVEABLE STATE HOLDER TESTS
    // =========================================================================

    @Test
    fun `FakeSaveableStateHolder removeState is no-op`() {
        // Given
        val stateHolder = FakeSaveableStateHolder()

        // When/Then - should not throw
        stateHolder.removeState("any-key")
        stateHolder.removeState(123)
        stateHolder.removeState(Any())
    }

    // =========================================================================
    // EMPTY SCREEN REGISTRY TESTS
    // =========================================================================

    @Test
    fun `EmptyScreenRegistry hasContent returns false for any destination`() {
        // When/Then
        assertFalse(EmptyScreenRegistry.hasContent(TestDestination))
    }

    @Test
    fun `EmptyScreenRegistry hasContent returns false for different destinations`() {
        // Given
        val anotherDestination = object : Destination {
            override val data: Any? = "test"
            override val transition: NavigationTransition? = null
        }

        // When/Then
        assertFalse(EmptyScreenRegistry.hasContent(anotherDestination))
    }

    // =========================================================================
    // PREDICTIVE BACK CONTROLLER TESTS
    // =========================================================================

    @Test
    fun `predictiveBackController isActive is false by default`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertFalse(scope.predictiveBackController.isActive.value)
    }

    @Test
    fun `predictiveBackController progress is zero by default`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertEquals(0f, scope.predictiveBackController.progress.value)
    }

    // =========================================================================
    // ANIMATION COORDINATOR TESTS
    // =========================================================================

    @Test
    fun `animationCoordinator provides default transitions`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertNotNull(scope.animationCoordinator.defaultTransition)
        assertNotNull(scope.animationCoordinator.defaultTabTransition)
        assertNotNull(scope.animationCoordinator.defaultPaneTransition)
    }

    // =========================================================================
    // CONTAINER REGISTRY TESTS
    // =========================================================================

    @Test
    fun `containerRegistry hasTabsContainer returns false by default`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertFalse(scope.containerRegistry.hasTabsContainer("any-key"))
    }

    @Test
    fun `containerRegistry hasPaneContainer returns false by default`() {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        assertFalse(scope.containerRegistry.hasPaneContainer("any-key"))
    }
}
