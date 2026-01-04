package com.jermey.quo.vadis.core.compose.animation

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.dsl.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * Coordinates transition animations based on NavNode types and annotations.
 *
 * The AnimationCoordinator is responsible for resolving which transition should
 * be used during navigation. It provides a layered resolution strategy:
 *
 * 1. **Annotation-based**: Check [TransitionRegistry] for `@Transition` annotations
 * 2. **Type-based defaults**: Fall back to sensible defaults based on node type
 *
 * ## Resolution Order
 *
 * When determining a transition for navigation:
 *
 * ```
 * 1. TransitionRegistry lookup (annotation on destination)
 * 2. Default transition based on navigation context
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val coordinator = AnimationCoordinator(registry)
 *
 * // Get transition for screen navigation
 * val transition = coordinator.getTransition(
 *     from = currentNode,
 *     to = targetNode,
 *     isBack = false
 * )
 *
 * // Use in AnimatedContent - direction is applied here
 * AnimatedContent(
 *     targetState = targetNode,
 *     transitionSpec = { transition.createTransitionSpec(isBack = isBackNavigation) }
 * ) { node -> ... }
 * ```
 *
 * ## Direction Handling
 *
 * This coordinator returns the base transition without direction modifications.
 * The navigation direction (forward/back) is handled by [NavTransition.createTransitionSpec]
 * which selects the appropriate enter/exit or popEnter/popExit animations.
 *
 * @property transitionRegistry Registry for annotation-based transition lookup
 *
 * @see TransitionRegistry
 * @see NavTransition
 */
@InternalQuoVadisApi
@Stable
class AnimationCoordinator(
    private val transitionRegistry: TransitionRegistry = TransitionRegistry.Empty
) {

    /**
     * Gets the default transition for standard screen navigation.
     *
     * @return The default [NavTransition] for screen transitions
     */
    val defaultTransition: NavTransition
        get() = NavTransition.SlideHorizontal

    /**
     * Gets the default transition for tab switching.
     *
     * @return The default [NavTransition] for tab transitions
     */
    val defaultTabTransition: NavTransition
        get() = NavTransition.Fade

    /**
     * Gets the default transition for pane changes.
     *
     * @return The default [NavTransition] for pane transitions
     */
    val defaultPaneTransition: NavTransition
        get() = NavTransition.Fade

    /**
     * Gets the appropriate transition for navigation between nodes.
     *
     * Resolution order for forward navigation:
     * 1. [TransitionRegistry] lookup for entering destination annotation
     * 2. Default transition based on node type
     *
     * Resolution order for back navigation:
     * 1. [TransitionRegistry] lookup for exiting destination annotation
     * 2. Default transition based on node type
     *
     * ## Direction Handling
     *
     * This method does NOT reverse transitions. The direction is handled by
     * [NavTransition.createTransitionSpec] which selects the appropriate
     * enter/exit or popEnter/popExit animations based on direction.
     *
     * For back navigation, the transition is looked up from the **exiting** screen
     * (the `from` node) rather than the entering screen, ensuring the popped
     * screen's defined exit animation is used.
     *
     * ## Example
     *
     * ```kotlin
     * // Forward: use transition from entering screen
     * val forwardTransition = coordinator.getTransition(
     *     from = screenA,
     *     to = screenB,  // B's transition used
     *     isBack = false
     * )
     *
     * // Back: use transition from exiting screen
     * val backTransition = coordinator.getTransition(
     *     from = screenB,  // B's transition used
     *     to = screenA,
     *     isBack = true
     * )
     *
     * // Direction is applied when creating the spec
     * transition.createTransitionSpec(isBack = isBackNavigation)
     * ```
     *
     * @param from Source node (`null` for initial navigation)
     * @param to Target node
     * @param isBack Whether this is back navigation (pop)
     * @return [NavTransition] to use for the animation
     *
     * @see NavTransition.createTransitionSpec
     */
    fun getTransition(from: NavNode?, to: NavNode, isBack: Boolean): NavTransition {
        // For back navigation, look up the transition from the exiting screen (from)
        // For forward navigation, look up the transition from the entering screen (to)
        val lookupNode = if (isBack) from else to

        val screenNode = lookupNode as? ScreenNode
        screenNode?.destination?.let { dest ->
            transitionRegistry.getTransition(dest::class)?.let { transition ->
                return transition
            }
        }

        // Fall back to defaults based on node type
        return defaultTransition
    }

    /**
     * Gets transition for pane role changes.
     *
     * Used when the visible panes in an adaptive layout change, such as
     * when transitioning from single-pane to multi-pane mode or when
     * showing/hiding supporting panes.
     *
     * Currently uses a simple fade transition for all pane changes.
     * Future implementations may provide role-specific transitions.
     *
     * ## Example
     *
     * ```kotlin
     * // Transition when showing a supporting pane
     * val transition = coordinator.getPaneTransition(
     *     fromRole = null,
     *     toRole = PaneRole.Supporting
     * )
     * ```
     *
     * @param fromRole Previous pane role (`null` for initial)
     * @param toRole New pane role
     * @return [NavTransition] for pane animation
     *
     * @see PaneRole
     */
    fun getPaneTransition(fromRole: PaneRole?, toRole: PaneRole): NavTransition {
        return defaultPaneTransition
    }

    /**
     * Companion object providing factory methods and default instances.
     */
    companion object {

        /**
         * A default [AnimationCoordinator] with no custom transitions.
         *
         * Uses [TransitionRegistry.Empty], so all lookups fall back to
         * default transitions based on navigation context.
         *
         * ## Usage
         *
         * ```kotlin
         * // Use when no custom transitions are needed
         * val scope = NavRenderScope(
         *     animationCoordinator = AnimationCoordinator.Default
         * )
         * ```
         */
        val Default: AnimationCoordinator = AnimationCoordinator(TransitionRegistry.Empty)
    }
}
