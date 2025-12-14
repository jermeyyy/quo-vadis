package com.jermey.quo.vadis.core.navigation.testing

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.quo.vadis.core.navigation.compose.ComposableCache
import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackMode
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.LocalAnimatedVisibilityScope
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.NavRenderScope
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenRegistry

/**
 * Test fake implementation of [NavRenderScope] for composable testing.
 *
 * This class provides a controllable implementation of [NavRenderScope] that can be used
 * in unit tests and UI tests to verify composable behavior without setting up the full
 * navigation infrastructure.
 *
 * ## Features
 *
 * - **Controllable state**: Direct access to [predictiveBackController] for simulating gesture states
 * - **Sensible defaults**: All dependencies have working defaults suitable for most tests
 * - **Customizable**: All dependencies can be injected for specific test scenarios
 * - **No-op SaveableStateHolder**: Uses [FakeSaveableStateHolder] that doesn't persist state
 *
 * ## Usage
 *
 * ### Basic usage with defaults
 * ```kotlin
 * @Test
 * fun testScreenRendering() = runComposeTest {
 *     val fakeScope = FakeNavRenderScope()
 *     setContent {
 *         with(fakeScope) {
 *             // Use scope properties in your composable
 *             cache.CachedEntry("test-key", saveableStateHolder) {
 *                 Text("Content")
 *             }
 *         }
 *     }
 *     // assertions...
 * }
 * ```
 *
 * ### Testing with custom navigator
 * ```kotlin
 * @Test
 * fun testNavigationActions() = runComposeTest {
 *     val fakeNavigator = FakeNavigator()
 *     val fakeScope = FakeNavRenderScope(navigator = fakeNavigator)
 *     
 *     setContent {
 *         Button(onClick = { fakeScope.navigator.navigateBack() }) {
 *             Text("Back")
 *         }
 *     }
 *     
 *     // Click button
 *     onNodeWithText("Back").performClick()
 *     
 *     // Verify navigation
 *     assertTrue(fakeNavigator.verifyNavigateBack())
 * }
 * ```
 *
 * ### Testing predictive back gesture states
 * ```kotlin
 * @Test
 * fun testPredictiveBackRendering() = runComposeTest {
 *     val fakeScope = FakeNavRenderScope()
 *     
 *     setContent {
 *         if (fakeScope.predictiveBackController.isActive.value) {
 *             Text("Gesture Active")
 *         } else {
 *             Text("Idle")
 *         }
 *     }
 *     
 *     // Initially idle
 *     onNodeWithText("Idle").assertExists()
 *     
 *     // Simulate gesture (in coroutine context)
 *     // Note: For testing active state, use coroutine-based testing
 * }
 * ```
 *
 * ### Custom screen registry for testing specific screens
 * ```kotlin
 * val testRegistry = object : ScreenRegistry {
 *     @Composable
 *     override fun Content(
 *         destination: Destination,
 *         navigator: Navigator,
 *         sharedTransitionScope: SharedTransitionScope?,
 *         animatedVisibilityScope: AnimatedVisibilityScope?
 *     ) {
 *         when (destination) {
 *             is MyTestDestination -> TestScreen(destination, navigator)
 *             else -> {}
 *         }
 *     }
 *     override fun hasContent(destination: Destination) = destination is MyTestDestination
 * }
 * 
 * val fakeScope = FakeNavRenderScope(screenRegistry = testRegistry)
 * ```
 *
 * @property navigator The navigator instance for navigation operations. Defaults to [FakeNavigator].
 * @property cache The composable cache for managing composable lifecycle. Defaults to a new [ComposableCache].
 * @property saveableStateHolder State holder for saveable state. Defaults to [FakeSaveableStateHolder].
 * @property animationCoordinator Coordinator for transitions. Defaults to [AnimationCoordinator.Default].
 * @property predictiveBackController Controller for predictive back gestures. Created fresh by default.
 * @property screenRegistry Registry for screen composables. Defaults to [EmptyScreenRegistry].
 * @property wrapperRegistry Registry for wrapper composables. Defaults to [WrapperRegistry.Empty].
 * @property sharedTransitionScope Scope for shared element transitions. Defaults to `null`.
 *
 * @see NavRenderScope
 * @see FakeNavigator
 * @see FakeSaveableStateHolder
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
public class FakeNavRenderScope(
    override val navigator: Navigator = FakeNavigator(),
    override val cache: ComposableCache = ComposableCache(),
    override val saveableStateHolder: SaveableStateHolder = FakeSaveableStateHolder(),
    override val animationCoordinator: AnimationCoordinator = AnimationCoordinator.Default,
    override val predictiveBackController: PredictiveBackController = PredictiveBackController(),
    override val screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    override val wrapperRegistry: WrapperRegistry = WrapperRegistry.Empty,
    override val sharedTransitionScope: SharedTransitionScope? = null,
    override val predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY
) : NavRenderScope {

    /**
     * Provides an [AnimatedVisibilityScope] to the given content via [LocalAnimatedVisibilityScope].
     *
     * In tests, this enables content to access animation state for enter/exit transitions.
     * The provided [animatedVisibilityScope] is made available through the composition local.
     *
     * @param animatedVisibilityScope The scope from AnimatedContent or AnimatedVisibility
     * @param content The composable content that needs access to the animation scope
     */
    @Composable
    override fun withAnimatedVisibilityScope(
        animatedVisibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides animatedVisibilityScope
        ) {
            content()
        }
    }

    public companion object {
        /**
         * Creates a [FakeNavRenderScope] configured for testing navigation actions.
         *
         * The returned [FakeNavigator] is accessible for verifying navigation calls.
         *
         * ## Usage
         * ```kotlin
         * val (scope, navigator) = FakeNavRenderScope.withFakeNavigator()
         * // ... use scope in composable
         * assertTrue(navigator.verifyNavigateTo("home"))
         * ```
         *
         * @return A pair of the scope and the fake navigator for verification
         */
        public fun withFakeNavigator(): Pair<FakeNavRenderScope, FakeNavigator> {
            val navigator = FakeNavigator()
            return FakeNavRenderScope(navigator = navigator) to navigator
        }
    }
}

/**
 * A fake implementation of [SaveableStateHolder] for testing purposes.
 *
 * This implementation provides no-op state persistence, which is appropriate for
 * unit tests where state restoration is not being tested. The [SaveableStateProvider]
 * simply renders its content without any state management.
 *
 * ## Behavior
 *
 * - [SaveableStateProvider]: Renders content directly without state wrapping
 * - [removeState]: No-op (nothing to remove)
 *
 * ## Usage
 *
 * ```kotlin
 * val stateHolder = FakeSaveableStateHolder()
 * 
 * // In a composable test
 * stateHolder.SaveableStateProvider("key") {
 *     // Content renders normally, but rememberSaveable won't persist
 *     var count by rememberSaveable { mutableStateOf(0) }
 *     Text("Count: $count")
 * }
 * ```
 *
 * ## Limitations
 *
 * - State marked with `rememberSaveable` will NOT be preserved across recompositions
 *   that would normally trigger state restoration
 * - If testing state restoration behavior, consider using a real implementation
 *
 * @see SaveableStateHolder
 */
@Stable
public class FakeSaveableStateHolder : SaveableStateHolder {

    /**
     * Provides state for the given [key] by simply rendering the [content].
     *
     * Unlike a real implementation, this does not actually save or restore state.
     * The content is rendered directly without any state management wrapper.
     *
     * @param key Unique key for the state (ignored in this fake implementation)
     * @param content The composable content to render
     */
    @Composable
    override fun SaveableStateProvider(key: Any, content: @Composable () -> Unit) {
        // Simply render content without state management
        // This is appropriate for unit tests where state restoration isn't being tested
        content()
    }

    /**
     * Removes any saved state associated with the given [key].
     *
     * This is a no-op in the fake implementation since no state is actually saved.
     *
     * @param key Unique key for the state to remove (ignored in this fake implementation)
     */
    override fun removeState(key: Any) {
        // No-op: we don't actually save state, so there's nothing to remove
    }
}

/**
 * Empty implementation of [ScreenRegistry] for use when no screens are registered.
 *
 * This is the default registry used by [FakeNavRenderScope] when no custom
 * registry is provided. It renders nothing for any destination.
 *
 * ## Behavior
 *
 * - [Content]: Renders nothing (empty composable)
 * - [hasContent]: Always returns `false`
 *
 * ## Usage
 *
 * For tests that need to render specific screens, create a custom registry:
 *
 * ```kotlin
 * val testRegistry = object : ScreenRegistry {
 *     @Composable
 *     override fun Content(
 *         destination: Destination,
 *         navigator: Navigator,
 *         sharedTransitionScope: SharedTransitionScope?,
 *         animatedVisibilityScope: AnimatedVisibilityScope?
 *     ) {
 *         when (destination) {
 *             is TestDestination -> TestScreen(navigator)
 *             else -> {}
 *         }
 *     }
 *     
 *     override fun hasContent(destination: Destination) = 
 *         destination is TestDestination
 * }
 * ```
 *
 * @see ScreenRegistry
 * @see FakeNavRenderScope
 */
@OptIn(ExperimentalSharedTransitionApi::class)
public object EmptyScreenRegistry : ScreenRegistry {

    /**
     * Renders nothing for any destination.
     *
     * @param destination The destination to render (ignored)
     * @param navigator The Navigator instance (ignored)
     * @param sharedTransitionScope Shared transition scope (ignored)
     * @param animatedVisibilityScope Animated visibility scope (ignored)
     */
    @Composable
    override fun Content(
        destination: Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        // Empty - render nothing
    }

    /**
     * Always returns `false` since no content is registered.
     *
     * @param destination The destination to check
     * @return Always `false`
     */
    override fun hasContent(destination: Destination): Boolean = false
}
