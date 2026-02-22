@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.scope.LocalAnimatedVisibilityScope
import com.jermey.quo.vadis.core.compose.scope.LocalScreenNode
import com.jermey.quo.vadis.core.compose.scope.NavRenderScope
import com.jermey.quo.vadis.core.navigation.node.ScreenNode

/**
 * Renders a [ScreenNode] leaf node.
 *
 * This renderer handles the terminal state in the navigation tree,
 * invoking the screen content via [NavRenderScope.screenRegistry].
 *
 * ## Lifecycle Management
 *
 * The renderer manages the screen's UI lifecycle via [com.jermey.quo.vadis.core.navigation.LifecycleAwareNode]:
 * - Calls [ScreenNode.attachToUI] when the composable enters composition
 * - Calls [ScreenNode.detachFromUI] when the composable leaves composition
 *
 * ## State Preservation
 *
 * Uses [ComposableCache.CachedEntry] to preserve composable state across
 * navigation transitions. The cache ensures that:
 * - Screen state (rememberSaveable) survives navigation
 * - Animations remain smooth during transitions
 * - LRU eviction manages memory efficiently
 *
 * ## Composition Locals
 *
 * Provides the following composition locals to screen content:
 * - [LocalScreenNode]: The current ScreenNode for navigation context
 * - [LocalAnimatedVisibilityScope]: Animation scope for enter/exit animations
 *
 * ## Shared Element Transitions
 *
 * The [animatedVisibilityScope] parameter is passed DIRECTLY from the parent
 * AnimatedContent. This is critical for shared element transitions to work
 * correctly - both entering and exiting screens must use the SAME scope object.
 *
 * ## ScreenRegistry Integration
 *
 * Invokes [com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry.Content] with:
 * - `destination`: The destination from the ScreenNode
 * - `sharedTransitionScope`: For shared element transitions (may be null)
 * - `animatedVisibilityScope`: For coordinated animations (may be null)
 *
 * @param node The screen node containing the destination to render
 * @param scope The render scope with dependencies and context
 * @param animatedVisibilityScope The AnimatedVisibilityScope from the containing
 *   AnimatedContent. Passed directly to ensure shared element transitions work.
 *   May be null when not inside an animating context.
 *
 * @see ScreenNode
 * @see LocalScreenNode
 * @see LocalAnimatedVisibilityScope
 * @see com.jermey.quo.vadis.core.navigation.LifecycleAwareNode.attachToUI
 * @see com.jermey.quo.vadis.core.navigation.LifecycleAwareNode.detachFromUI
 */
@Composable
internal fun ScreenRenderer(
    node: ScreenNode,
    scope: NavRenderScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    // Use cache for state preservation across navigation transitions
    scope.cache.CachedEntry(
        key = node.key.value,
        saveableStateHolder = scope.saveableStateHolder
    ) {
        // Lifecycle management: attach/detach UI lifecycle
        DisposableEffect(node) {
            node.attachToUI()
            onDispose {
                node.detachFromUI()
            }
        }

        // Use the animatedVisibilityScope from AnimatedContent if provided,
        // otherwise fall back to the composition local. The key() wrapper in
        // AnimatedNavContent ensures exit content recomposes and picks up
        // the new scope during transitions.
        val effectiveScope = animatedVisibilityScope ?: LocalAnimatedVisibilityScope.current

        // Provide composition locals for screen content access
        CompositionLocalProvider(
            LocalScreenNode provides node,
            LocalAnimatedVisibilityScope provides effectiveScope
        ) {
            // Invoke screen content via registry
            scope.screenRegistry.Content(
                destination = node.destination,
                sharedTransitionScope = scope.sharedTransitionScope,
                animatedVisibilityScope = effectiveScope
            )
        }
    }
}
