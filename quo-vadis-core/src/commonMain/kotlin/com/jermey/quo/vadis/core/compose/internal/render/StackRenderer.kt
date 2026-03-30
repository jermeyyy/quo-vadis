@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.scope.NavRenderScope
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.compose.transition.toNavTransition
import com.jermey.quo.vadis.core.registry.ModalRegistry

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
 * ## Shared Element Transitions
 *
 * The [animatedVisibilityScope] parameter is passed through from the parent.
 * When AnimatedNavContent renders screen nodes, it provides its own scope
 * directly to NavNodeRenderer, ensuring both entering and exiting screens
 * use the same scope for shared element transitions.
 *
 * ## Modal Support
 *
 * When the active child is a modal destination (as determined by
 * [com.jermey.quo.vadis.core.registry.ModalRegistry]), the renderer determines
 * a background target (the topmost non-modal child) and keeps it rendered
 * through [AnimatedNavContent] at a stable composition tree position. Modal
 * nodes are rendered as sibling overlays after the animated content block.
 * This ensures the background screen's composition is retained when modals
 * are pushed or popped, avoiding unnecessary recomposition.
 *
 * If the modal is the only child in the stack, it renders normally through
 * [AnimatedNavContent] without any overlay siblings.
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
 * @param animatedVisibilityScope Optional AnimatedVisibilityScope from parent.
 *   Not used directly here but part of the signature for consistency.
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
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    val activeChild = node.activeChild ?: return
    val previousActiveChild = previousNode?.activeChild

    val modalRegistry = scope.modalRegistry
    val isActiveModal = isNodeModal(activeChild, modalRegistry)
    val hasModalOverlay = isActiveModal && node.children.size > 1

    // When a modal is active, find the topmost non-modal child index once and reuse it.
    val baseIndex = if (hasModalOverlay) findNonModalBaseIndex(node.children, modalRegistry) else -1

    // Determine the effective background target:
    // When a modal is active, the background is the topmost non-modal child.
    // Otherwise, it's simply the active child (normal stack behavior).
    val backgroundTarget = if (hasModalOverlay) {
        node.children[baseIndex]
    } else {
        activeChild
    }

    // Determine previous background target for animation pairing
    val previousBackgroundTarget = if (hasModalOverlay) {
        previousNode?.let {
            val baseIndex = findNonModalBaseIndex(it.children, modalRegistry)
            it.children.getOrNull(baseIndex)
        }
    } else {
        previousActiveChild
    }

    // Detect navigation direction based on the non-modal portion of the stack
    val isBackNavigation = detectBackNavigation(current = node, previous = previousNode)

    // Collect per-call transition override from navigator's TransitionController.
    // Must use collectAsState() because currentTransition is a WhileSubscribed StateFlow;
    // reading .value without an active collector would leave it stuck at null.
    val navigatorTransition = scope.transitionController
        ?.currentTransition
        ?.collectAsState()
        ?.value
        ?.toNavTransition()

    val transition = scope.animationCoordinator.getTransition(
        from = previousBackgroundTarget,
        to = backgroundTarget,
        isBack = isBackNavigation,
        transitionOverride = navigatorTransition,
    )

    val predictiveBackEnabled = scope.shouldEnablePredictiveBack(node)

    // ALWAYS render background through AnimatedNavContent.
    // This keeps the background screen at a stable composition tree position.
    // When a modal is pushed, backgroundTarget doesn't change → no recomposition.
    AnimatedNavContent(
        targetState = backgroundTarget,
        transition = transition,
        isBackNavigation = isBackNavigation,
        scope = scope,
        predictiveBackEnabled = predictiveBackEnabled,
        isTargetModal = isActiveModal && !hasModalOverlay,
    ) { child ->
        NavNodeRenderer(
            node = child,
            previousNode = previousBackgroundTarget,
            scope = scope
        )
    }

    // Overlay modal nodes as siblings when present
    if (hasModalOverlay) {
        val modalNodes = node.children.subList(baseIndex + 1, node.children.size)
        for ((i, modalNode) in modalNodes.withIndex()) {
            val previousChild = previousNode?.children?.getOrNull(baseIndex + 1 + i)
            StaticAnimatedVisibilityScope {
                NavNodeRenderer(
                    node = modalNode,
                    previousNode = previousChild,
                    scope = scope,
                )
            }
        }
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

/**
 * Determines whether a navigation node should be presented modally.
 *
 * For [ScreenNode], checks the destination class against the modal registry.
 * For container nodes ([StackNode], [TabNode], [PaneNode]), checks the
 * container key against the modal registry.
 *
 * @param node The navigation node to check
 * @param modalRegistry The registry to consult for modal status
 * @return `true` if the node should be presented modally
 */
internal fun isNodeModal(node: NavNode, modalRegistry: ModalRegistry): Boolean {
    return when (node) {
        is ScreenNode -> modalRegistry.isModalDestination(node.destination::class)
        is StackNode -> modalRegistry.isModalContainer(node.key.value)
        is TabNode -> modalRegistry.isModalContainer(node.key.value)
        is PaneNode -> modalRegistry.isModalContainer(node.key.value)
    }
}

/**
 * Finds the index of the topmost non-modal child in a stack's children list.
 *
 * Walks backwards through the children from the end (active child) until a
 * non-modal node is found. If all children are modal, returns 0 as a
 * fallback to use the first child as the background.
 *
 * @param children The stack's children list
 * @param modalRegistry The registry to consult for modal status
 * @return The index of the topmost non-modal child, or 0 if all are modal
 */
internal fun findNonModalBaseIndex(
    children: List<NavNode>,
    modalRegistry: ModalRegistry,
): Int {
    for (i in children.lastIndex downTo 0) {
        if (!isNodeModal(children[i], modalRegistry)) {
            return i
        }
    }
    return 0
}
