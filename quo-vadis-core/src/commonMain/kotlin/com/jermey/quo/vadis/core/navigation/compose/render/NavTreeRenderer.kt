package com.jermey.quo.vadis.core.navigation.compose.render

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode

/**
 * Core recursive renderer that dispatches to node-specific renderers based on [NavNode] type.
 *
 * The `NavTreeRenderer` is the central component of the hierarchical rendering engine.
 * It recursively traverses the navigation tree, delegating rendering of each node type
 * to specialized renderers while maintaining the proper Compose hierarchy.
 *
 * ## Hierarchical Rendering
 *
 * Unlike flat rendering approaches, this renderer preserves the parent-child relationships
 * defined by the navigation structure. This enables:
 *
 * - **Proper wrapper composition**: Tab wrappers contain tab content as children
 * - **Coordinated animations**: Parent and child nodes animate together
 * - **Predictive back gestures**: Entire subtrees transform as units
 * - **Shared element transitions**: Work naturally across NavNode boundaries
 *
 * ## Dispatch Logic
 *
 * The renderer uses a `when` expression to dispatch to type-specific renderers:
 *
 * ```kotlin
 * NavTreeRenderer(node, previousNode, scope) // Dispatches based on node type:
 * // - ScreenNode → ScreenRenderer (leaf content)
 * // - StackNode → StackRenderer (animated stack transitions)
 * // - TabNode → TabRenderer (wrapper + tab switching)
 * // - PaneNode → PaneRenderer (adaptive multi-pane layout)
 * ```
 *
 * ## Animation Pairing
 *
 * The [previousNode] parameter enables animation coordination by providing the
 * previous state of the navigation tree. Each specialized renderer uses this to:
 *
 * - Determine animation direction (forward vs back)
 * - Calculate transition specs
 * - Handle predictive back gestures
 *
 * When [previousNode] is null (initial render), renderers should handle gracefully
 * by using default enter animations or no animation.
 *
 * ## Example Usage
 *
 * ```kotlin
 * // Inside NavigationHost
 * val currentState by navigator.state.collectAsState()
 * var previousState by remember { mutableStateOf<NavNode?>(null) }
 *
 * NavTreeRenderer(
 *     node = currentState,
 *     previousNode = previousState,
 *     scope = navRenderScope,
 *     modifier = Modifier.fillMaxSize()
 * )
 *
 * LaunchedEffect(currentState) {
 *     previousState = currentState
 * }
 * ```
 *
 * @param node The current navigation node to render. The concrete type determines
 *   which specialized renderer will be invoked.
 * @param previousNode The previous navigation node state for animation pairing.
 *   May be null on initial render or when previous state is unavailable.
 * @param scope The render scope providing context, dependencies, and resources
 *   required by all renderers in the hierarchy.
 * @param modifier Modifier to apply to the rendered content. Applied by the
 *   top-level renderer component.
 *
 * @see NavRenderScope
 * @see ScreenNode
 * @see StackNode
 * @see TabNode
 * @see PaneNode
 */
@Composable
internal fun NavNodeRenderer(
    node: NavNode,
    previousNode: NavNode?,
    scope: NavRenderScope,
) {
    when (node) {
        is ScreenNode -> ScreenRenderer(
            node = node,
            scope = scope,
        )

        is StackNode -> StackRenderer(
            node = node,
            previousNode = previousNode as? StackNode,
            scope = scope,
        )

        is TabNode -> TabRenderer(
            node = node,
            previousNode = previousNode as? TabNode,
            scope = scope,
        )

        is PaneNode -> PaneRenderer(
            node = node,
            previousNode = previousNode as? PaneNode,
            scope = scope,
        )
    }
}
