package com.jermey.quo.vadis.core.compose.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.compose.navback.PredictiveBackController
import com.jermey.quo.vadis.core.dsl.registry.ContainerRegistry
import com.jermey.quo.vadis.core.dsl.registry.ScreenRegistry

/**
 * Core scope interface that provides context to all hierarchical renderers.
 *
 * This scope is the central nervous system of the hierarchical rendering engine,
 * providing access to navigation state, caching, animations, transitions, and
 * shared element scopes. It is passed through the render tree and enables each
 * renderer to access the resources it needs for proper rendering.
 *
 * ## Architecture
 *
 * The `NavRenderScope` serves as a dependency injection mechanism for the rendering
 * hierarchy. Instead of threading multiple parameters through deeply nested composables,
 * renderers receive this scope and extract what they need:
 *
 * ```kotlin
 * @Composable
 * fun ScreenRenderer(node: ScreenNode, scope: NavRenderScope) {
 *     val cache = scope.cache
 *     val registry = scope.screenRegistry
 *
 *     cache.CachedEntry(node.key) {
 *         registry.Content(node.destination, scope.navigator)
 *     }
 * }
 * ```
 *
 * ## Scopes and Contexts
 *
 * The scope provides access to animation-related scopes that enable:
 * - Shared element transitions via [sharedTransitionScope]
 * - Animated visibility for enter/exit animations via [WithAnimatedVisibilityScope]
 *
 * Renderers should use [WithAnimatedVisibilityScope] to provide the correct
 * animation context to screen content:
 *
 * ```kotlin
 * scope.withAnimatedVisibilityScope(animatedVisibilityScope) {
 *     // Screen content can now use animatedVisibility modifier
 *     ScreenContent()
 * }
 * ```
 *
 * ## Thread Safety
 *
 * This interface and its implementations are marked with [Stable], indicating
 * that they can be safely read during composition without triggering
 * unnecessary recompositions.
 *
 * @see Navigator
 * @see ComposableCache
 * @see AnimationCoordinator
 * @see PredictiveBackController
 * @see ScreenRegistry
 * @see ContainerRegistry
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
interface NavRenderScope {

    /**
     * The navigator instance for performing navigation operations.
     *
     * Provides access to navigation state and methods for navigating between
     * destinations, switching tabs, managing panes, and handling back navigation.
     *
     * ## Usage
     *
     * ```kotlin
     * Button(onClick = { scope.navigator.navigateBack() }) {
     *     Text("Back")
     * }
     * ```
     *
     * @see Navigator
     */
    val navigator: Navigator

    /**
     * Cache for managing composable lifecycle and state preservation.
     *
     * The cache ensures that screen content is preserved during animations
     * and transitions, preventing premature disposal of composables and
     * maintaining smooth visual continuity.
     *
     * ## Cache Management
     *
     * - Entries are keyed by unique identifiers (typically NavNode keys)
     * - Supports LRU eviction with configurable size limits
     * - Provides locking mechanism to protect entries during animations
     *
     * @see ComposableCache
     */
    val cache: ComposableCache

    /**
     * State holder for preserving saveable state across navigation.
     *
     * The saveable state holder ensures that composable state marked with
     * `rememberSaveable` is preserved during navigation transitions and
     * can be restored after process death.
     *
     * ## Usage
     *
     * Used by [ComposableCache.CachedEntry] to wrap screen content with
     * state preservation:
     *
     * ```kotlin
     * cache.CachedEntry(
     *     key = node.key,
     *     saveableStateHolder = saveableStateHolder
     * ) {
     *     ScreenContent()
     * }
     * ```
     *
     * @see SaveableStateHolder
     * @see ComposableCache.CachedEntry
     */
    val saveableStateHolder: SaveableStateHolder

    /**
     * Coordinator for resolving and managing navigation transitions.
     *
     * The animation coordinator determines which transitions to use based on:
     * - Destination-level annotations (via TransitionRegistry)
     * - Navigation direction (forward vs back)
     * - Node type (stack, tab, pane)
     *
     * @see AnimationCoordinator
     */
    val animationCoordinator: AnimationCoordinator

    /**
     * Controller for predictive back gesture handling.
     *
     * Provides centralized state and coordination for predictive back gestures,
     * including:
     * - Gesture active state
     * - Progress tracking (0-1)
     * - Animation completion/cancellation
     *
     * Renderers check [PredictiveBackController.isActive] to determine whether
     * to use gesture-based rendering vs standard animated transitions.
     *
     * @see PredictiveBackController
     */
    val predictiveBackController: PredictiveBackController

    /**
     * Registry for looking up screen composables by destination class.
     *
     * The screen registry maps destination classes to their corresponding
     * composable content, typically populated by KSP-generated code from
     * `@Screen` annotations.
     *
     * @see ScreenRegistry
     */
    val screenRegistry: ScreenRegistry

    /**
     * Registry for looking up wrapper composables and container builders.
     *
     * The container registry provides:
     * - Wrapper composables for tab and pane containers (from `@TabsContainer` and `@PaneContainer` annotations)
     * - Container builders for creating TabNode/PaneNode structures
     *
     * @see ContainerRegistry
     */
    val containerRegistry: ContainerRegistry

    /**
     * Shared transition scope for coordinating shared element animations.
     *
     * When non-null, enables shared element transitions between screens.
     * Renderers should propagate this scope to screen content so that
     * shared element modifiers can function correctly.
     *
     * ## Availability
     *
     * This scope is only available when the root host wraps content in a
     * `SharedTransitionLayout`. If null, shared element transitions are
     * disabled but navigation still functions normally.
     *
     * @see SharedTransitionScope
     */
    val sharedTransitionScope: SharedTransitionScope?

    /**
     * Determines if predictive back should be enabled for a given node.
     *
     * This method considers:
     * 1. Whether a cascade animation is currently active
     * 2. Whether this stack was designated to handle the animation at gesture start
     *
     * For nested stacks inside tabs:
     * - If the stack has > 1 children, back pops within the stack → enable predictive back
     * - If the stack has 1 child and cascade is happening → only root handles animation
     *
     * For pane switching in compact mode:
     * - The animatingStackKey is set to the PaneNode's key
     * - Only the PaneNode (via SinglePaneRenderer) should handle the animation
     * - StackNodes should NOT handle predictive back when a PaneNode is animating
     *
     * @param node The node to check
     * @return true if predictive back gestures should be enabled for this node
     */
    fun shouldEnablePredictiveBack(node: NavNode): Boolean {
        val cascadeState = predictiveBackController.cascadeState.value
        val isGestureActive = predictiveBackController.isActive.value

        // During a gesture or animation, use the pre-calculated animating stack key
        // This ensures the same stack handles the animation even after navigation happens
        if (cascadeState != null && isGestureActive) {
            // Non-cascade case (normal pop within a stack): cascadeDepth == 0
            // The animatingStackKey tells us which node should handle the animation
            if (cascadeState.cascadeDepth == 0) {
                val animatingKey = cascadeState.animatingStackKey
                // For pane switching, animatingKey is the PaneNode's key
                // Only StackNodes should be checked here - PaneNode handles via SinglePaneRenderer
                if (animatingKey != null && node is StackNode) {
                    // If the animating key matches this stack, enable predictive back
                    // If the animating key is a PaneNode key (not matching any stack),
                    // stacks should NOT handle predictive back - the pane will handle it
                    return node.key == animatingKey
                }
                // If animatingKey is null or node is not a StackNode, disable for this node
                return animatingKey == null
            }

            // True cascade case (cascadeDepth > 0): only root handles animation
            // because the entire container (TabNode etc.) is being removed
            if (node.parentKey != null) {
                return false
            }
        }

        return true
    }

    /**
     * Provides an [AnimatedVisibilityScope] to the given content.
     *
     * This method enables screen content to access animation state for
     * enter/exit transitions. Screen composables that use animated visibility
     * modifiers (like `animateEnterExit`) require this scope to function.
     *
     * ## Usage
     *
     * ```kotlin
     * AnimatedContent(targetState = currentNode) { node ->
     *     scope.withAnimatedVisibilityScope(this) {
     *         // Content can now use this@AnimatedContent for animations
     *         screenRegistry.Content(node.destination, navigator)
     *     }
     * }
     * ```
     *
     * @param animatedVisibilityScope The scope from AnimatedContent or AnimatedVisibility
     * @param content The composable content that needs access to the animation scope
     */
    @Composable
    fun WithAnimatedVisibilityScope(
        animatedVisibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    )
}
