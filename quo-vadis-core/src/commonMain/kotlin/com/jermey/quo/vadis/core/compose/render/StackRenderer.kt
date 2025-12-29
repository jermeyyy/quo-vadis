package com.jermey.quo.vadis.core.compose.render

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.StackNode

/**
 * Renders a [StackNode] with animated transitions between children.
 *
 * This renderer handles linear navigation stacks, animating transitions
 * when the active child changes (push/pop operations). It renders only
 * the active child (last in the children list) with smooth animations
 * for both forward navigation (push) and back navigation (pop).
 *
 * ## Animation Direction
 *
 * The renderer detects navigation direction by comparing stack sizes:
 * - **Forward**: Stack grew (push operation) → use enter transitions
 * - **Back**: Stack shrunk (pop operation) → use reversed transitions
 *
 * This direction is passed to
 * [com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator.getTransition] to select
 * appropriate animations based on the navigation context.
 *
 * ## Predictive Back Support
 *
 * Predictive back gestures are enabled only for root stacks (where [StackNode.parentKey]
 * is null). This ensures that:
 * - Root navigation supports gesture-driven back animations
 * - Nested stacks within tabs/panes don't interfere with parent predictive back
 *
 * ## Empty Stack Handling
 *
 * If the stack is empty (no children), this renderer returns early without
 * producing any UI. Parent nodes should handle cascading empty stack removal.
 *
 * ## Example
 *
 * ```kotlin
 * // Stack with 3 screens, screen C is active (last)
 * val stack = StackNode(
 *     key = "main",
 *     children = listOf(screenA, screenB, screenC)
 * )
 *
 * // Only screenC is rendered with transition from previous state
 * StackRenderer(node = stack, previousNode = previousStack, scope = scope)
 * ```
 *
 * @param node The stack node to render
 * @param previousNode The previous stack state for animation direction detection.
 *   Used to determine if this is forward or back navigation.
 * @param scope The render scope with dependencies (AnimationCoordinator, etc.)
 *
 * @see StackNode
 * @see AnimatedNavContent
 * @see NavNodeRenderer
 */
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
) {
    // Early return for empty stack - no content to render
    val activeChild = node.activeChild ?: return
    val previousActiveChild = previousNode?.activeChild

    // Detect navigation direction by comparing stack sizes
    // Back navigation occurs when the stack shrinks (pop operation)
    val isBackNavigation = detectBackNavigation(current = node, previous = previousNode)

    // Get appropriate transition based on navigation direction
    val transition = scope.animationCoordinator.getTransition(
        from = previousActiveChild,
        to = activeChild,
        isBack = isBackNavigation
    )

    // Use configurable predictive back mode to determine if this stack
    // should handle predictive back gestures
    val predictiveBackEnabled = scope.shouldEnablePredictiveBack(node)

    // Animated content switching with transition
    AnimatedNavContent(
        targetState = activeChild,
        transition = transition,
        scope = scope,
        predictiveBackEnabled = predictiveBackEnabled,
    ) { child ->
        // Recurse to render the active child
        NavNodeRenderer(
            node = child,
            previousNode = previousActiveChild,
            scope = scope
        )
    }
}

/**
 * Detects whether the current navigation is a back navigation (pop).
 *
 * Back navigation is determined by comparing stack sizes:
 * - If the current stack is smaller than the previous, it's a pop (back)
 * - If the current stack is larger or equal, it's a push (forward)
 *
 * @param current The current stack state
 * @param previous The previous stack state (null if initial render)
 * @return `true` if this is back navigation, `false` otherwise
 */
internal fun detectBackNavigation(current: StackNode, previous: StackNode?): Boolean {
    if (previous == null) return false
    // Back navigation: stack shrunk (pop operation)
    return current.children.size < previous.children.size
}
