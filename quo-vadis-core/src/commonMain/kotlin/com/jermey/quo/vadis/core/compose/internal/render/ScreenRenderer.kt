@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.ScreenNode

/**
 * Renders a [ScreenNode] by delegating to the registered screen content.
 *
 * This renderer is the leaf node of the hierarchical rendering engine.
 * It looks up the screen content via [ScreenRegistry] and renders it
 * with the appropriate animation scopes for shared element transitions
 * and enter/exit animations.
 *
 * ## Screen Content Resolution
 *
 * The renderer uses the [NavDestination] contained in the [ScreenNode]
 * to look up the registered screen content via the [NavRenderScope.screenRegistry].
 * If no content is registered for the destination, nothing is rendered.
 *
 * ## Animation Scopes
 *
 * Screen content receives both [SharedTransitionScope] and [AnimatedVisibilityScope]
 * to enable:
 * - Shared element transitions between screens
 * - Coordinated enter/exit animations
 * - Proper animation lifecycle management
 *
 * ## Usage
 *
 * This renderer is typically called from within an [AnimatedVisibilityScope] context
 * provided by parent renderers (like [StackRenderer] via [AnimatedNavContent]).
 * The animated visibility scope is automatically provided via composition locals.
 *
 * ## Example
 *
 * ```kotlin
 * // Screen registration via DSL
 * navigationConfig {
 *     screen<HomeScreen> { destination, sharedScope, animScope ->
 *         HomeScreenContent(
 *             destination = destination,
 *             sharedTransitionScope = sharedScope,
 *             animatedVisibilityScope = animScope
 *         )
 *     }
 * }
 *
 * // Rendering (called from within AnimatedVisibilityScope)
 * ScreenRenderer(node = screenNode, scope = navRenderScope)
 * ```
 *
 * @param node The screen node to render
 * @param scope The render scope with registries and dependencies
 *
 * @see ScreenNode
 * @see ScreenRegistry
 * @see NavRenderScope
 * @see AnimatedNavContent
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ScreenRenderer(
    node: ScreenNode,
    scope: NavRenderScope,
) {
    // Delegate to screen registry for content rendering
    // The AnimatedVisibilityScope is provided via composition locals by the parent renderer
    scope.screenRegistry.Content(
        destination = node.destination,
        sharedTransitionScope = scope.sharedTransitionScope
    )
}
