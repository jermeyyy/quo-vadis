package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.compose.scope.NavRenderScope
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.registry.ModalRegistry

/**
 * Renders overlapping content layers for modal navigation.
 *
 * This composable handles modal presentation by rendering a non-modal
 * background node beneath one or more modal foreground nodes in a simple [Box].
 * Each layer uses [StaticAnimatedVisibilityScope] for compatibility with
 * shared element transitions and animated visibility modifiers.
 *
 * ## Visual Behavior
 *
 * - **Background layer**: The topmost non-modal node in the stack, rendered
 *   at full size beneath all modal layers.
 * - **Foreground layers**: All modal nodes stacked on top of the background,
 *   rendered in order from bottom to top.
 *
 * ## Rendering
 *
 * Each layer is rendered through [NavNodeRenderer], preserving the full
 * hierarchical rendering pipeline (screen registry, container wrappers, etc.).
 * No transforms, scrim, or parallax effects are applied — layers are simply
 * stacked in a [Box].
 *
 * @param backgroundNode The non-modal node to render as the background layer
 * @param previousBackgroundNode The previous state of the background node
 *   for animation pairing within the background subtree
 * @param modalNodes The modal nodes to render as foreground layers, ordered
 *   from bottom to top
 * @param scope The [NavRenderScope] providing rendering dependencies
 *
 * @see NavNodeRenderer
 * @see StaticAnimatedVisibilityScope
 * @see ModalRegistry
 */
@Composable
internal fun ModalContent(
    backgroundNode: NavNode,
    previousBackgroundNode: NavNode?,
    modalNodes: List<NavNode>,
    scope: NavRenderScope,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Background layer: non-modal content beneath the modal
        StaticAnimatedVisibilityScope {
            NavNodeRenderer(
                node = backgroundNode,
                previousNode = previousBackgroundNode,
                scope = scope,
            )
        }

        // Foreground layers: modal nodes stacked on top
        for (modalNode in modalNodes) {
            StaticAnimatedVisibilityScope {
                NavNodeRenderer(
                    node = modalNode,
                    previousNode = null,
                    scope = scope,
                )
            }
        }
    }
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
