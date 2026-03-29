package com.jermey.quo.vadis.core.navigation.testing

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.internal.AnimationCoordinator
import com.jermey.quo.vadis.core.compose.internal.PredictiveBackController
import com.jermey.quo.vadis.core.compose.internal.ComposableCache
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.EmptyScreenRegistry
import com.jermey.quo.vadis.core.navigation.FakeNavRenderScope
import com.jermey.quo.vadis.core.navigation.FakeNavigator
import com.jermey.quo.vadis.core.navigation.FakeSaveableStateHolder
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.destination.route
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs

// =========================================================================
// TEST DESTINATIONS
// =========================================================================

private object TestDestination : NavDestination {
    override val data: Any? = null
    override val transition: NavigationTransition? = null
}

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
@OptIn(InternalQuoVadisApi::class)
class FakeNavRenderScopeTest : FunSpec({

    // =========================================================================
    // INSTANTIATION TESTS
    // =========================================================================

    test("FakeNavRenderScope can be created with defaults") {
        // When
        val scope = FakeNavRenderScope()

        // Then
        scope.navigator.shouldNotBeNull()
        scope.cache.shouldNotBeNull()
        scope.saveableStateHolder.shouldNotBeNull()
        scope.animationCoordinator.shouldNotBeNull()
        scope.predictiveBackController.shouldNotBeNull()
        scope.screenRegistry.shouldNotBeNull()
        scope.containerRegistry.shouldNotBeNull()
        scope.sharedTransitionScope.shouldBeNull()
    }

    test("FakeNavRenderScope uses FakeNavigator by default") {
        // When
        val scope = FakeNavRenderScope()

        // Then
        scope.navigator.shouldBeInstanceOf<FakeNavigator>()
    }

    test("FakeNavRenderScope uses FakeSaveableStateHolder by default") {
        // When
        val scope = FakeNavRenderScope()

        // Then
        scope.saveableStateHolder.shouldBeInstanceOf<FakeSaveableStateHolder>()
    }

    test("FakeNavRenderScope uses AnimationCoordinator Default by default") {
        // When
        val scope = FakeNavRenderScope()

        // Then
        scope.animationCoordinator shouldBeSameInstanceAs AnimationCoordinator.Default
    }

    test("FakeNavRenderScope uses EmptyScreenRegistry by default") {
        // When
        val scope = FakeNavRenderScope()

        // Then
        scope.screenRegistry shouldBeSameInstanceAs EmptyScreenRegistry
    }

    test("FakeNavRenderScope uses ContainerRegistry Empty by default") {
        // When
        val scope = FakeNavRenderScope()

        // Then
        scope.containerRegistry shouldBeSameInstanceAs ContainerRegistry.Empty
    }

    // =========================================================================
    // CUSTOMIZATION TESTS
    // =========================================================================

    test("FakeNavRenderScope accepts custom navigator") {
        // Given
        val customNavigator = FakeNavigator()

        // When
        val scope = FakeNavRenderScope(navigator = customNavigator)

        // Then
        scope.navigator shouldBeSameInstanceAs customNavigator
    }

    test("FakeNavRenderScope accepts custom cache") {
        // Given
        val customCache = ComposableCache()

        // When
        val scope = FakeNavRenderScope(cache = customCache)

        // Then
        scope.cache shouldBeSameInstanceAs customCache
    }

    test("FakeNavRenderScope accepts custom saveableStateHolder") {
        // Given
        val customStateHolder = FakeSaveableStateHolder()

        // When
        val scope = FakeNavRenderScope(saveableStateHolder = customStateHolder)

        // Then
        scope.saveableStateHolder shouldBeSameInstanceAs customStateHolder
    }

    test("FakeNavRenderScope accepts custom animationCoordinator") {
        // Given
        val customCoordinator = AnimationCoordinator()

        // When
        val scope = FakeNavRenderScope(animationCoordinator = customCoordinator)

        // Then
        scope.animationCoordinator shouldBeSameInstanceAs customCoordinator
    }

    test("FakeNavRenderScope accepts custom predictiveBackController") {
        // Given
        val customController = PredictiveBackController()

        // When
        val scope = FakeNavRenderScope(predictiveBackController = customController)

        // Then
        scope.predictiveBackController shouldBeSameInstanceAs customController
    }

    // =========================================================================
    // FACTORY METHOD TESTS
    // =========================================================================

    test("withFakeNavigator returns scope and navigator pair") {
        // When
        val (scope, navigator) = FakeNavRenderScope.withFakeNavigator()

        // Then
        scope.shouldNotBeNull()
        navigator.shouldNotBeNull()
        scope.navigator shouldBeSameInstanceAs navigator
    }

    // TODO: TestDestination.route requires KSP-generated RouteRegistry registration,
    //  which is not available in unit tests. Re-enable once route resolution is decoupled or
    //  a test-friendly route registration mechanism is provided.
    xtest("withFakeNavigator navigator is usable for verification") {
        // Given
        val (scope, navigator) = FakeNavRenderScope.withFakeNavigator()

        // When
        scope.navigator.navigate(TestDestination)

        // Then
        navigator.verifyNavigateTo(TestDestination.route).shouldBeTrue()
    }

    // =========================================================================
    // FAKE SAVEABLE STATE HOLDER TESTS
    // =========================================================================

    test("FakeSaveableStateHolder removeState is no-op") {
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

    test("EmptyScreenRegistry hasContent returns false for any destination") {
        // When/Then
        EmptyScreenRegistry.hasContent(TestDestination).shouldBeFalse()
    }

    test("EmptyScreenRegistry hasContent returns false for different destinations") {
        // Given
        val anotherDestination = object : NavDestination {
            override val data: Any? = "test"
            override val transition: NavigationTransition? = null
        }

        // When/Then
        EmptyScreenRegistry.hasContent(anotherDestination).shouldBeFalse()
    }

    // =========================================================================
    // PREDICTIVE BACK CONTROLLER TESTS
    // =========================================================================

    test("predictiveBackController isActive is false by default") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.predictiveBackController.isActive.value.shouldBeFalse()
    }

    test("predictiveBackController progress is zero by default") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.predictiveBackController.progress.value shouldBe 0f
    }

    // =========================================================================
    // ANIMATION COORDINATOR TESTS
    // =========================================================================

    test("animationCoordinator provides default transitions") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.animationCoordinator.defaultTransition.shouldNotBeNull()
        scope.animationCoordinator.defaultTabTransition.shouldNotBeNull()
        scope.animationCoordinator.defaultPaneTransition.shouldNotBeNull()
    }

    // =========================================================================
    // CONTAINER REGISTRY TESTS
    // =========================================================================

    test("containerRegistry hasTabsContainer returns false by default") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.containerRegistry.hasTabsContainer("any-key").shouldBeFalse()
    }

    test("containerRegistry hasPaneContainer returns false by default") {
        // Given
        val scope = FakeNavRenderScope()

        // Then
        scope.containerRegistry.hasPaneContainer("any-key").shouldBeFalse()
    }
})
