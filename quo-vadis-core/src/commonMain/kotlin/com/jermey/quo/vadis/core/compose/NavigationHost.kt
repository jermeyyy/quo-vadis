@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.jermey.quo.vadis.core.compose

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.activeLeaf
import com.jermey.quo.vadis.core.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.compose.animation.LocalBackAnimationController
import com.jermey.quo.vadis.core.compose.animation.LocalTransitionScope
import com.jermey.quo.vadis.core.compose.animation.TransitionScope
import com.jermey.quo.vadis.core.compose.animation.rememberBackAnimationController
import com.jermey.quo.vadis.core.compose.navback.NavigateBackHandler
import com.jermey.quo.vadis.core.compose.navback.PredictiveBackController
import com.jermey.quo.vadis.core.compose.navback.ScreenNavigationInfo
import com.jermey.quo.vadis.core.compose.navback.calculateCascadeBackState
import com.jermey.quo.vadis.core.compose.render.ComposableCache
import com.jermey.quo.vadis.core.compose.render.LocalAnimatedVisibilityScope
import com.jermey.quo.vadis.core.compose.render.LocalNavigator
import com.jermey.quo.vadis.core.compose.render.NavNodeRenderer
import com.jermey.quo.vadis.core.compose.render.NavRenderScope
import com.jermey.quo.vadis.core.compose.render.rememberComposableCache
import com.jermey.quo.vadis.core.compose.wrapper.WindowSizeClass
import com.jermey.quo.vadis.core.dsl.registry.BackHandlerRegistry
import com.jermey.quo.vadis.core.dsl.registry.ContainerRegistry
import com.jermey.quo.vadis.core.dsl.registry.LocalBackHandlerRegistry
import com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry
import com.jermey.quo.vadis.core.dsl.registry.ScreenRegistry
import com.jermey.quo.vadis.core.dsl.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.route
import com.jermey.quo.vadis.core.navigation.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.tree.TreeNavigator
import kotlinx.coroutines.launch

// =============================================================================
// Composition Local for NavRenderScope
// =============================================================================

/**
 * Provides access to the current [com.jermey.quo.vadis.core.compose.render.NavRenderScope] within the hierarchical navigation tree.
 *
 * This composition local is provided by [com.jermey.quo.vadis.core.compose.NavigationHost] and allows
 * any composable within the navigation hierarchy to access shared resources
 * and state required for rendering, including:
 *
 * - [Navigator] for navigation operations
 * - [com.jermey.quo.vadis.core.compose.render.ComposableCache] for state preservation
 * - [com.jermey.quo.vadis.core.compose.animation.AnimationCoordinator] for transition resolution
 * - [com.jermey.quo.vadis.core.compose.navback.PredictiveBackController] for gesture handling
 * - [ScreenRegistry] and [ContainerRegistry] for content resolution
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
 * This local is only available within content rendered by [com.jermey.quo.vadis.core.compose.NavigationHost].
 * Accessing it outside of this context will return `null`.
 *
 * @see com.jermey.quo.vadis.core.compose.render.NavRenderScope
 * @see com.jermey.quo.vadis.core.compose.NavigationHost
 */
val LocalNavRenderScope =
    compositionLocalOf<NavRenderScope?> { null }

// =============================================================================
// HierarchicalNavigationHost
// =============================================================================

/**
 * Unified navigation host that renders NavNode trees using hierarchical rendering.
 *
 * `HierarchicalNavigationHost` is the new rendering approach that preserves the parent-child
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
 *     HierarchicalNavigationHost(
 *         navigator = navigator,
 *         modifier = Modifier.fillMaxSize()
 *     )
 * }
 * ```
 *
 * ## With Custom Registries
 *
 * ```kotlin
 * HierarchicalNavigationHost(
 *     navigator = navigator,
 *     screenRegistry = MyGeneratedScreenRegistry,
 *     containerRegistry = MyGeneratedContainerRegistry,
 *     transitionRegistry = MyGeneratedTransitionRegistry
 * )
 * ```
 *
 * ## Predictive Back Gestures
 *
 * When [enablePredictiveBack] is `true` (default), the host integrates with
 * platform back gesture APIs to provide visual feedback during back navigation.
 * The [com.jermey.quo.vadis.core.compose.navback.PredictiveBackController] coordinates gesture state across all renderers.
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
 * @param containerRegistry Registry for container builders and custom tab/pane wrappers.
 *   Defaults to [ContainerRegistry.Empty] which renders content without custom wrappers.
 * @param transitionRegistry Registry for annotation-based transitions.
 *   Defaults to [TransitionRegistry.Empty] which uses default transitions.
 * @param scopeRegistry Registry for scope-aware navigation. When navigating to a
 *   destination outside the current container's scope, navigation pushes to the
 *   parent stack. Defaults to [ScopeRegistry.Empty] for backward compatibility.
 * @param enablePredictiveBack Whether to enable predictive back gesture handling.
 *   When enabled, users can preview back navigation while performing a back gesture.
 *   Defaults to `true`. Set to `false` to disable gesture-based back previews.
 * @param windowSizeClass Current window size class for adaptive pane back behavior.
 *   When provided, pane back navigation adapts based on display mode:
 *   - Compact mode (single pane): Back behaves like a simple stack
 *   - Expanded mode (multiple panes): Configured [PaneBackBehavior] applies
 *   Defaults to `null` which uses compact behavior for safety.
 *
 * @see NavRenderScope
 * @see NavNodeRenderer
 * @see PredictiveBackController
 * @see LocalNavRenderScope
 */
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    containerRegistry: ContainerRegistry = ContainerRegistry.Empty,
    transitionRegistry: TransitionRegistry = TransitionRegistry.Empty,
    scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    enablePredictiveBack: Boolean = true,
    windowSizeClass: WindowSizeClass? = null
) {
    // Collect navigation state
    val navState by navigator.state.collectAsState()

    // Track previous state for animation pairing
    // Use derivedStateOf to compute previousState synchronously during composition
    // This ensures the correct previous state is available for transition direction detection
    var lastProcessedState by remember { mutableStateOf<NavNode?>(null) }
    
    // Compute previousState synchronously: it's whatever lastProcessedState was before navState changed
    // We track state transitions: lastProcessedState -> navState
    val previousState = remember(navState) {
        val previous = lastProcessedState
        // Don't update lastProcessedState here - it's done in SideEffect below
        previous
    }
    
    // Update lastProcessedState after the current composition succeeds
    // Using SideEffect ensures this runs synchronously at the end of composition
    SideEffect {
        if (lastProcessedState != navState) {
            lastProcessedState = navState
        }
    }

    // State holder for preserving saveable state
    val saveableStateHolder = rememberSaveableStateHolder()

    // Composable cache for lifecycle management
    val cache = rememberComposableCache()

    // Animation coordinator for transition resolution
    val animationCoordinator = remember(transitionRegistry) {
        AnimationCoordinator(transitionRegistry)
    }

    // Predictive back controller for gesture handling
    val predictiveBackController = remember { PredictiveBackController() }

    // Back animation controller for predictive back animations
    val backAnimationController = rememberBackAnimationController()

    // Check if we can navigate back using tab-aware logic
    val canGoBack by remember(navState) {
        derivedStateOf { TreeMutator.canHandleBackNavigation(navState) }
    }

    // Get current and previous screen info for QuoVadisBackHandler
    val currentScreenNode = remember(navState) { navState.activeLeaf() }
    val previousScreenNode = remember(navState) {
        // Use popWithTabBehavior to get the correct previous screen
        val backResult = TreeMutator.popWithTabBehavior(navState)
        (backResult as? TreeMutator.BackResult.Handled)?.newState?.activeLeaf()
    }

    // Create ScreenNavigationInfo for predictive back
    val currentScreenInfo = remember(currentScreenNode) {
        currentScreenNode?.let {
            ScreenNavigationInfo(
                screenId = it.key,
                displayName = it.destination::class.simpleName,
                route = runCatching { it.destination.route }.getOrNull()
            )
        } ?: ScreenNavigationInfo(screenId = navState.key)
    }

    val previousScreenInfo = remember(previousScreenNode) {
        previousScreenNode?.let {
            ScreenNavigationInfo(
                screenId = it.key,
                displayName = it.destination::class.simpleName,
                route = runCatching { it.destination.route }.getOrNull()
            )
        }
    }

    // Back handler registry for user-defined back handlers
    val backHandlerRegistry = remember { BackHandlerRegistry() }

    // Connect registry to navigator (if it's a TreeNavigator)
    LaunchedEffect(navigator, backHandlerRegistry) {
        (navigator as? TreeNavigator)?.backHandlerRegistry = backHandlerRegistry
    }

    // Update window size class on navigator for adaptive pane back behavior
    LaunchedEffect(navigator, windowSizeClass) {
        (navigator as? TreeNavigator)?.windowSizeClass = windowSizeClass
    }

    // Clean up registry connection on disposal
    DisposableEffect(navigator) {
        onDispose {
            (navigator as? TreeNavigator)?.backHandlerRegistry = null
            (navigator as? TreeNavigator)?.windowSizeClass = null
        }
    }

    // Speculative state for predictive back - computed when gesture starts
    var speculativePopState by remember { mutableStateOf<NavNode?>(null) }

    // Coroutine scope for launching animations
    val coroutineScope = rememberCoroutineScope()

    // Root container with QuoVadisBackHandler and SharedTransitionLayout
    NavigateBackHandler(
        enabled = enablePredictiveBack && canGoBack,
        currentScreenInfo = currentScreenInfo,
        previousScreenInfo = previousScreenInfo,
        onBackProgress = { event ->

            // On first progress event, start the animation
            if (!backAnimationController.isAnimating) {
                // Compute speculative pop result at gesture start using tab-aware logic
                val backResult = TreeMutator.popWithTabBehavior(navState)
                val popResult = (backResult as? TreeMutator.BackResult.Handled)?.newState
                if (popResult != null) {
                    speculativePopState = popResult

                    // Calculate cascade state for proper animation targeting
                    val cascadeState = calculateCascadeBackState(navState)

                    // Start animation
                    backAnimationController.startAnimation(event)

                    // CRITICAL: Update predictiveBackController so AnimatedNavContent
                    // switches to PredictiveBackContent for visual animation
                    // Pass cascade state so renderers know what to animate
                    predictiveBackController.startGestureWithCascade(cascadeState)
                }
            } else {
                backAnimationController.updateProgress(event)
                // Update predictiveBackController progress for visual animation
                predictiveBackController.updateGestureProgress(event.progress)
            }
        },
        onBackCancelled = {
            // Cancel animation and reset state
            backAnimationController.cancelAnimation()

            // Animate the cancellation so PredictiveBackContent smoothly returns to start
            coroutineScope.launch {
                predictiveBackController.animateCancelGesture()
            }
            speculativePopState = null
        },
        onBackCompleted = {
            // Complete animation and perform navigation
            backAnimationController.completeAnimation()

            // Use the speculative state computed at gesture start
            val targetState = speculativePopState
            speculativePopState = null

            // Animate the completion - navigation happens at the start of animation
            // so PredictiveBackContent shows the correct "previous" screen
            coroutineScope.launch {
                predictiveBackController.animateCompleteGesture {
                    if (targetState != null) {
                        navigator.updateState(targetState)
                    }
                }
            }
        }
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
                containerRegistry,
            ) {
                NavRenderScopeImpl(
                    navigator = navigator,
                    cache = cache,
                    saveableStateHolder = saveableStateHolder,
                    animationCoordinator = animationCoordinator,
                    predictiveBackController = predictiveBackController,
                    screenRegistry = screenRegistry,
                    containerRegistry = containerRegistry,
                    sharedTransitionScope = this,
                )
            }

            // Provide scope to children via CompositionLocal
            CompositionLocalProvider(
                LocalNavRenderScope provides scope,
                LocalNavigator provides navigator,
                LocalBackHandlerRegistry provides backHandlerRegistry,
                LocalBackAnimationController provides backAnimationController
            ) {
                // Render the navigation tree
                NavNodeRenderer(
                    node = navState,
                    previousNode = previousState,
                    scope = scope,
                )
            }
        }
    }
}

// =============================================================================
// NavigationHost Simplified Overload (Recommended)
// =============================================================================

/**
 * NavigationHost that renders navigation content by reading config from the navigator.
 *
 * This is the **recommended overload** as it eliminates the need to pass config twice.
 * The navigator already holds the [NavigationConfig], so NavigationHost reads it
 * directly from there.
 *
 * ## Basic Usage (Simplest)
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(MainTabs::class, GeneratedNavigationConfig)
 *
 *     // Config is now implicit - read from navigator
 *     NavigationHost(navigator)
 * }
 * ```
 *
 * ## With Options
 *
 * ```kotlin
 * NavigationHost(
 *     navigator = navigator,
 *     modifier = Modifier.fillMaxSize(),
 *     enablePredictiveBack = true,
 *     windowSizeClass = currentWindowSizeClass()
 * )
 * ```
 *
 * ## Benefits
 *
 * - **No redundancy**: Config passed once to navigator, not repeated here
 * - **No mismatch risk**: Impossible to pass different configs to navigator and host
 * - **Cleaner API**: Fewer parameters to manage
 *
 * @param navigator The Navigator managing navigation state. Must have been created
 *   with a valid [NavigationConfig].
 * @param modifier Modifier to apply to the host container.
 * @param enablePredictiveBack Whether to enable predictive back gesture support.
 *   When enabled, back gestures provide visual feedback before completing the navigation.
 * @param windowSizeClass Optional window size class for responsive layouts.
 *   When provided, navigation containers can adapt their presentation based on available space.
 */
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    windowSizeClass: WindowSizeClass? = null
) {
    // Read config from navigator
    val config = navigator.config

    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        screenRegistry = config.screenRegistry,
        containerRegistry = config.containerRegistry,
        transitionRegistry = config.transitionRegistry,
        scopeRegistry = config.scopeRegistry,
        enablePredictiveBack = enablePredictiveBack,
        windowSizeClass = windowSizeClass
    )
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
 * - Content resolution via [screenRegistry] and [containerRegistry]
 * - Shared element support via [sharedTransitionScope]
 *
 * @property navigator Navigator instance for navigation operations
 * @property cache ComposableCache for managing composable lifecycle
 * @property saveableStateHolder State holder for saveable state preservation
 * @property animationCoordinator Coordinator for resolving transitions
 * @property predictiveBackController Controller for predictive back gestures
 * @property screenRegistry Registry for screen content lookup
 * @property containerRegistry Registry for container and wrapper lookup
 * @property sharedTransitionScope Scope for shared element transitions
 * @property predictiveBackMode Mode for predictive back gesture handling
 */
@Stable
private class NavRenderScopeImpl(
    override val navigator: Navigator,
    override val cache: ComposableCache,
    override val saveableStateHolder: SaveableStateHolder,
    override val animationCoordinator: AnimationCoordinator,
    override val predictiveBackController: PredictiveBackController,
    override val screenRegistry: ScreenRegistry,
    override val containerRegistry: ContainerRegistry,
    override val sharedTransitionScope: SharedTransitionScope?,
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
    override fun WithAnimatedVisibilityScope(
        animatedVisibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    ) {
        // Create TransitionScope if SharedTransitionScope is available
        val transitionScope = sharedTransitionScope?.let {
            TransitionScope(it, animatedVisibilityScope)
        }

        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides animatedVisibilityScope,
            LocalTransitionScope provides transitionScope
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
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope?,
        animatedVisibilityScope: AnimatedVisibilityScope?
    ) {
        // No content registered - render nothing
    }

    override fun hasContent(
        destination: NavDestination
    ): Boolean = false
}
