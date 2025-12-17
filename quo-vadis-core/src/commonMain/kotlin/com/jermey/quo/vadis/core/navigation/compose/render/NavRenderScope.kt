package com.jermey.quo.vadis.core.navigation.compose.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.SaveableStateHolder
import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.navback.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.core.StackNode

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
 * - Animated visibility for enter/exit animations via [withAnimatedVisibilityScope]
 *
 * Renderers should use [withAnimatedVisibilityScope] to provide the correct
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
 * @see WrapperRegistry
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Stable
public interface NavRenderScope {

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
    public val navigator: Navigator

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
    public val cache: ComposableCache

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
    public val saveableStateHolder: SaveableStateHolder

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
    public val animationCoordinator: AnimationCoordinator

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
    public val predictiveBackController: PredictiveBackController

    /**
     * Registry for looking up screen composables by destination class.
     *
     * The screen registry maps destination classes to their corresponding
     * composable content, typically populated by KSP-generated code from
     * `@Screen` annotations.
     *
     * @see ScreenRegistry
     */
    public val screenRegistry: ScreenRegistry

    /**
     * Registry for looking up wrapper composables by node key.
     *
     * The wrapper registry maps tab and pane node keys to their custom
     * wrapper composables, typically populated by KSP-generated code from
     * `@TabWrapper` and `@PaneWrapper` annotations.
     *
     * @see WrapperRegistry
     */
    public val wrapperRegistry: WrapperRegistry

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
    public val sharedTransitionScope: SharedTransitionScope?

    /**
     * Determines if predictive back should be enabled for a given node.
     *
     * This method considers:
     * 1. Whether a cascade animation is currently active
     * 2. Whether this stack can handle the back action internally
     *
     * For nested stacks inside tabs:
     * - If the stack has > 1 children, back pops within the stack → enable predictive back
     * - If the stack has 1 child and cascade is happening → only root handles animation
     *
     * @param node The node to check
     * @return true if predictive back gestures should be enabled for this node
     */
    public fun shouldEnablePredictiveBack(node: NavNode): Boolean {
        val cascadeState = predictiveBackController.cascadeState.value
        val isGestureActive = predictiveBackController.isActive.value
        
        // During a CASCADE animation (cascadeDepth > 0), only the appropriate level
        // should handle animation. For non-cascade (normal pop), any stack with
        // children > 1 can handle its own animation.
        if (cascadeState != null && isGestureActive) {
            // Non-cascade case (normal pop within a stack): cascadeDepth == 0
            // The exiting node's parent stack should handle the animation
            if (cascadeState.cascadeDepth == 0) {
                // Check if this node is the stack that contains the exiting node
                // That stack should enable predictive back for its animation
                val exitingNode = cascadeState.exitingNode
                if (node is StackNode && node.children.any { it.key == exitingNode.key }) {
                    return true
                }
                // Other stacks should not animate
                return false
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
    public fun withAnimatedVisibilityScope(
        animatedVisibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    )
}
