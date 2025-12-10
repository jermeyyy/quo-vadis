@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.LocalAnimatedVisibilityScope
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.NavRenderScope
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.NavTreeRenderer
import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.core.TransitionStateManager
import com.jermey.quo.vadis.core.navigation.core.TreeMutator

// =============================================================================
// Composition Local for NavRenderScope
// =============================================================================

/**
 * Provides access to the current [NavRenderScope] within the hierarchical navigation tree.
 *
 * This composition local is provided by [NavigationHost] and allows
 * any composable within the navigation hierarchy to access shared resources
 * and state required for rendering, including:
 *
 * - [Navigator] for navigation operations
 * - [ComposableCache] for state preservation
 * - [AnimationCoordinator] for transition resolution
 * - [PredictiveBackController] for gesture handling
 * - [ScreenRegistry] and [WrapperRegistry] for content resolution
 * - [SharedTransitionScope] for shared element transitions
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun MyScreenContent() {
 *     val scope = LocalNavRenderScope.current
 *     scope?.let { navScope ->
 *         Button(onClick = { navScope.navigator.navigateBack() }) {
 *             Text("Go Back")
 *         }
 *     }
 * }
 * ```
 *
 * ## Availability
 *
 * This local is only available within content rendered by [NavigationHost].
 * Accessing it outside of this context will return `null`.
 *
 * @see NavRenderScope
 * @see NavigationHost
 */
public val LocalNavRenderScope = compositionLocalOf<NavRenderScope?> { null }

// =============================================================================
// HierarchicalQuoVadisHost
// =============================================================================

/**
 * Unified navigation host that renders NavNode trees using hierarchical rendering.
 *
 * `HierarchicalQuoVadisHost` is the new rendering approach that preserves the parent-child
 * relationships in the navigation tree, enabling proper wrapper composition, coordinated
 * animations, and seamless predictive back gestures.
 *
 * ## Architecture
 *
 * Unlike the flattening approach, hierarchical rendering:
 * - **Preserves hierarchy**: Tab wrappers contain tab content as proper Compose children
 * - **Enables scoped animations**: Each container type handles its own transitions
 * - **Supports predictive back**: Entire subtrees transform as units during gestures
 * - **Simplifies state management**: Each renderer manages only its immediate concerns
 *
 * ## Basic Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberNavigator(initialGraph)
 *
 *     HierarchicalQuoVadisHost(
 *         navigator = navigator,
 *         modifier = Modifier.fillMaxSize()
 *     )
 * }
 * ```
 *
 * ## With Custom Registries
 *
 * ```kotlin
 * HierarchicalQuoVadisHost(
 *     navigator = navigator,
 *     screenRegistry = MyGeneratedScreenRegistry,
 *     wrapperRegistry = MyGeneratedWrapperRegistry,
 *     transitionRegistry = MyGeneratedTransitionRegistry
 * )
 * ```
 *
 * ## Predictive Back Gestures
 *
 * When [enablePredictiveBack] is `true` (default), the host integrates with
 * platform back gesture APIs to provide visual feedback during back navigation.
 * The [PredictiveBackController] coordinates gesture state across all renderers.
 *
 * ## Shared Element Transitions
 *
 * The host wraps content in a [SharedTransitionLayout], enabling shared element
 * animations between screens. Access the [SharedTransitionScope] via [LocalNavRenderScope].
 *
 * @param navigator The [Navigator] instance managing navigation state
 * @param modifier [Modifier] applied to the root container
 * @param screenRegistry Registry for mapping destinations to screen composables.
 *   Defaults to [ScreenRegistry.Empty] - provide a KSP-generated registry for full functionality.
 * @param wrapperRegistry Registry for custom tab and pane wrappers.
 *   Defaults to [WrapperRegistry.Empty] which renders content without custom wrappers.
 * @param transitionRegistry Registry for annotation-based transitions.
 *   Defaults to [TransitionRegistry.Empty] which uses default transitions.
 * @param enablePredictiveBack Whether to enable predictive back gesture handling.
 *   When enabled, users can preview back navigation while performing a back gesture.
 *   Defaults to `true`. Set to `false` to disable gesture-based back previews.
 *
 * @see NavRenderScope
 * @see NavTreeRenderer
 * @see PredictiveBackController
 * @see LocalNavRenderScope
 */
@Composable
public fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    wrapperRegistry: WrapperRegistry = WrapperRegistry.Empty,
    transitionRegistry: TransitionRegistry = TransitionRegistry.Empty,
    enablePredictiveBack: Boolean = true
) {
    // Collect navigation state
    val navState by navigator.state.collectAsState()

    // Track previous state for animation pairing
    // We use a separate remember to avoid updating during recomposition
    var previousState by remember { mutableStateOf<NavNode?>(null) }
    var lastProcessedState by remember { mutableStateOf<NavNode?>(null) }

    // State holder for preserving saveable state
    val saveableStateHolder = rememberSaveableStateHolder()

    // Composable cache for lifecycle management
    val cache = remember { ComposableCache() }

    // Animation coordinator for transition resolution
    val animationCoordinator = remember(transitionRegistry) {
        AnimationCoordinator(transitionRegistry)
    }

    // Predictive back controller for gesture handling
    val predictiveBackController = remember { PredictiveBackController() }

    // Transition state manager for coordinating predictive back
    val transitionManager = remember(navigator) {
        TransitionStateManager(navState)
    }

    // Predictive back coordinator
    val backCoordinator = remember(navigator, transitionManager) {
        PredictiveBackCoordinator(navigator, transitionManager)
    }

    // Check if we can navigate back
    val canGoBack by remember(navState) {
        derivedStateOf { TreeMutator.pop(navState) != null }
    }

    // Root container with PredictiveBackHandler and SharedTransitionLayout
    PredictiveBackHandler(
        enabled = enablePredictiveBack && canGoBack,
        callback = backCoordinator
    ) {
        SharedTransitionLayout(modifier = modifier) {
            // Create the NavRenderScope implementation
            val scope = remember(
                this,
                navigator,
                cache,
                saveableStateHolder,
                animationCoordinator,
                predictiveBackController,
                screenRegistry,
                wrapperRegistry
            ) {
                NavRenderScopeImpl(
                    navigator = navigator,
                    cache = cache,
                    saveableStateHolder = saveableStateHolder,
                    animationCoordinator = animationCoordinator,
                    predictiveBackController = predictiveBackController,
                    screenRegistry = screenRegistry,
                    wrapperRegistry = wrapperRegistry,
                    sharedTransitionScope = this
                )
            }

            // Provide scope to children via CompositionLocal
            CompositionLocalProvider(LocalNavRenderScope provides scope) {
                // Render the navigation tree
                NavTreeRenderer(
                    node = navState,
                    previousNode = previousState,
                    scope = scope,
                    modifier = Modifier
                )

                // Update previous state after rendering to track state changes
                LaunchedEffect(navState) {
                    if (lastProcessedState != navState) {
                        previousState = lastProcessedState
                        lastProcessedState = navState
                    }
                }
            }
        }
    }
}

// =============================================================================
// NavRenderScope Implementation
// =============================================================================

/**
 * Implementation of [NavRenderScope] providing all dependencies for hierarchical rendering.
 *
 * This class aggregates all the components needed by the rendering hierarchy:
 * - Navigation state and operations via [navigator]
 * - Composable caching via [cache]
 * - State preservation via [saveableStateHolder]
 * - Animation coordination via [animationCoordinator]
 * - Predictive back handling via [predictiveBackController]
 * - Content resolution via [screenRegistry] and [wrapperRegistry]
 * - Shared element support via [sharedTransitionScope]
 *
 * @property navigator Navigator instance for navigation operations
 * @property cache ComposableCache for managing composable lifecycle
 * @property saveableStateHolder State holder for saveable state preservation
 * @property animationCoordinator Coordinator for resolving transitions
 * @property predictiveBackController Controller for predictive back gestures
 * @property screenRegistry Registry for screen content lookup
 * @property wrapperRegistry Registry for wrapper lookup
 * @property sharedTransitionScope Scope for shared element transitions
 */
@Stable
private class NavRenderScopeImpl(
    override val navigator: Navigator,
    override val cache: ComposableCache,
    override val saveableStateHolder: SaveableStateHolder,
    override val animationCoordinator: AnimationCoordinator,
    override val predictiveBackController: PredictiveBackController,
    override val screenRegistry: ScreenRegistry,
    override val wrapperRegistry: WrapperRegistry,
    override val sharedTransitionScope: SharedTransitionScope?
) : NavRenderScope {

    /**
     * Provides an [AnimatedVisibilityScope] to the given content via [LocalAnimatedVisibilityScope].
     *
     * This enables screen content to access animation state for enter/exit transitions,
     * allowing the use of animated visibility modifiers like `animateEnterExit`.
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
}

// =============================================================================
// Empty ScreenRegistry Implementation
// =============================================================================

/**
 * Empty implementation of [ScreenRegistry] for use when no screens are registered.
 *
 * This is useful for testing or as a default value when the KSP-generated
 * registry is not available.
 */
private object EmptyScreenRegistry : ScreenRegistry {

    @Composable
    override fun Content(
        destination: com.jermey.quo.vadis.core.navigation.core.Destination,
        navigator: Navigator,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        // No content registered - render nothing
    }

    override fun hasContent(
        destination: com.jermey.quo.vadis.core.navigation.core.Destination
    ): Boolean = false
}
