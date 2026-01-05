@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.scope.NavRenderScope
import com.jermey.quo.vadis.core.compose.transition.NavTransition
import com.jermey.quo.vadis.core.navigation.node.NavNode

/**
 * Custom AnimatedContent variant optimized for navigation transitions.
 *
 * This composable is the core animation component for the hierarchical rendering engine.
 * It handles transitions between navigation states with support for both standard
 * `AnimatedContent` animations and predictive back gesture-driven animations.
 *
 * ## Animation Direction
 *
 * The direction of navigation is determined by the caller (typically the parent renderer
 * like [StackRenderer]) and passed via the [isBackNavigation] parameter. This ensures
 * consistent direction detection across different rendering contexts.
 *
 * ## Predictive Back Integration
 *
 * When predictive back gestures are enabled and active, this component bypasses
 * `AnimatedContent` entirely and delegates to [PredictiveBackContent], which provides
 * gesture-driven animations with proper state preservation.
 *
 * ## State Tracking
 *
 * The component maintains internal state for predictive back:
 * - **lastCommittedState**: The state currently shown on screen
 * - **stateBeforeLast**: The previous state for gesture target resolution
 *
 * ## Usage
 *
 * ```kotlin
 * AnimatedNavContent(
 *     targetState = currentNode,
 *     transition = navTransition,
 *     isBackNavigation = isBack,  // Determined by caller
 *     scope = renderScope,
 *     predictiveBackEnabled = true
 * ) { node ->
 *     // Content is provided an AnimatedVisibilityScope
 *     ScreenContent(node)
 * }
 * ```
 *
 * @param T The type of navigation node being animated (must extend [NavNode])
 * @param targetState The current target state to animate to
 * @param transition The [NavTransition] defining enter/exit animations for both directions
 * @param isBackNavigation Whether this is a back navigation (pop). Used to select
 *                         the appropriate animation direction (enter/exit vs popEnter/popExit)
 * @param scope The [NavRenderScope] providing access to predictive back controller,
 *              animation scopes, and other rendering dependencies
 * @param predictiveBackEnabled Whether predictive back gesture handling should be active.
 *                              When `false`, always uses standard `AnimatedContent`
 * @param modifier Optional [Modifier] applied to the container
 * @param content The composable content to render for each state, receiving an
 *                [AnimatedVisibilityScope] for enter/exit animation coordination
 *
 * @see NavTransition
 * @see NavRenderScope
 * @see PredictiveBackContent
 * @see AnimatedContent
 */
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    isBackNavigation: Boolean,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    // Track the last committed state for predictive back gesture handling
    // Use object reference tracking, not just key, because a node's internal state
    // can change (e.g., PaneNode content) while keeping the same key
    var lastCommittedState by remember { mutableStateOf(targetState) }
    var stateBeforeLast by remember { mutableStateOf<T?>(null) }

    // Determine if predictive back gesture is currently active
    val isPredictiveBackActive = predictiveBackEnabled &&
            scope.predictiveBackController.isActive.value

    if (isPredictiveBackActive) {
        // For predictive back, prefer cascadeState.targetNode over local stateBeforeLast
        // This ensures correct animation target even for deep links or restored state
        val cascadeState = scope.predictiveBackController.cascadeState.value

        @Suppress("UNCHECKED_CAST")
        val backTarget = (cascadeState?.targetNode as? T) ?: stateBeforeLast

        // Gesture-driven animation - bypass AnimatedContent
        PredictiveBackContent(
            current = lastCommittedState,
            previous = backTarget,
            progress = scope.predictiveBackController.progress.value,
            scope = scope,
            content = content
        )
    } else {
        // Standard AnimatedContent transition
        // Use contentKey to compare nodes by their key, not object reference.
        // This prevents duplicate SaveableStateProvider keys when a node's
        // internal state changes (e.g., TabNode.activeStackIndex) but its key
        // stays the same.
        AnimatedContent(
            targetState = targetState,
            contentKey = { it.key },
            transitionSpec = {
                // Use the isBackNavigation flag passed from caller for proper animation selection
                transition.createTransitionSpec(isBack = isBackNavigation)
            },
            modifier = modifier,
            label = "AnimatedNavContent"
        ) { animatingState ->
            // Provide AnimatedVisibilityScope to content via NavRenderScope
            scope.WithAnimatedVisibilityScope(this) {
                content(animatingState)
            }
        }

        // Detect changes by semantic equality, not just key.
        // A node's internal state can change (e.g., PaneNode with new pane content)
        // while keeping the same key. We need to track when the actual state of
        // the node changes for predictive back to show the correct content.
        if (targetState != lastCommittedState) {
            // Only update stateBeforeLast when the key actually changes
            // (real navigation, not just internal state update)
            if (targetState.key != lastCommittedState.key) {
                stateBeforeLast = lastCommittedState
            }
            lastCommittedState = targetState
        }
    }
}
