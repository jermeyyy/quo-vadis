@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.scope.LocalContainerNode
import com.jermey.quo.vadis.core.compose.scope.NavRenderScope
import com.jermey.quo.vadis.core.compose.scope.PaneContainerScope
import com.jermey.quo.vadis.core.compose.scope.PaneContent
import com.jermey.quo.vadis.core.compose.scope.createPaneContainerScope
import com.jermey.quo.vadis.core.compose.util.calculateWindowSizeClass
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * Renders a [PaneNode] with adaptive layout based on window size.
 *
 * This renderer handles multi-pane layouts that adapt to different screen sizes,
 * showing multiple panes side-by-side on large screens or collapsing to single-pane
 * on compact screens.
 *
 * ## Lifecycle Management
 *
 * The renderer manages the pane container's UI lifecycle via [com.jermey.quo.vadis.core.navigation.LifecycleAwareNode]:
 * - Calls [PaneNode.attachToUI] when the composable enters composition
 * - Calls [PaneNode.detachFromUI] when the composable leaves composition
 * - Provides [LocalContainerNode] for child screens to access container context
 *
 * ## Adaptive Behavior
 *
 * The renderer detects the current window size class and adapts the layout:
 *
 * - **Expanded mode** (width >= [WindowWidthSizeClass.Medium]): Multiple panes displayed
 *   side-by-side using [com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry.PaneContainer]. The wrapper receives [PaneContent]
 *   slots for each configured pane, with visibility determined by [AdaptStrategy].
 *
 * - **Compact mode** (width < [WindowWidthSizeClass.Medium]): Single pane visible at a time,
 *   behaving like a stack. Only the [PaneNode.activePaneRole] is shown with animated
 *   transitions when switching between panes.
 *
 * ## Caching Strategy
 *
 * The **entire** PaneNode (wrapper + all pane contents) is cached as a single unit using
 * [ComposableCache.CachedEntry]. This ensures that:
 * - All pane states are preserved during layout changes
 * - Smooth transitions between expanded and compact modes
 * - The pane container is not recreated during navigation
 *
 * ## Pane Visibility
 *
 * In expanded mode, pane visibility is determined by [AdaptStrategy]:
 * - [AdaptStrategy.Hide]: Pane is not visible when space is limited
 * - [AdaptStrategy.Levitate]: Pane can overlay other content (not yet implemented)
 * - [AdaptStrategy.Reflow]: Pane reflows into available space (not yet implemented)
 *
 * In compact mode, only the active pane is visible regardless of adapt strategy.
 *
 * ## Example Structure
 *
 * ```
 * PaneRenderer(paneNode)
 *   └── CachedEntry(paneNode.key)
 *         └── [Expanded] PaneContainer(scope, content)
 *         │     └── Multiple NavTreeRenderer for each visible pane
 *         └── [Compact] AnimatedNavContent(activePaneContent)
 *               └── NavTreeRenderer(activePane)
 * ```
 *
 * @param node The pane node to render
 * @param previousNode The previous pane state for animation coordination
 * @param scope The render scope with dependencies and context
 *
 * @see PaneNode
 * @see PaneContainerScope
 * @see com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry.PaneContainer
 * @see WindowSizeClass
 * @see LocalContainerNode
 * @see com.jermey.quo.vadis.core.navigation.LifecycleAwareNode.attachToUI
 * @see com.jermey.quo.vadis.core.navigation.LifecycleAwareNode.detachFromUI
 */
@Composable
internal fun PaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope,
    @Suppress("UNUSED_PARAMETER")
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
) {
    // Detect window size class for adaptive layout decision
    val windowSizeClass = calculateWindowSizeClass()
    val isExpanded = windowSizeClass.isAtLeastMediumWidth

    // Build PaneContent list with actual content rendering lambdas
    // Each PaneContent.content() will render the corresponding pane
    val paneContents = buildPaneContentListWithRenderers(
        node = node,
        previousNode = previousNode,
        isExpanded = isExpanded,
        scope = scope
    )

    // Create PaneContainerScope with pane navigation state and actions
    val paneContainerScope = createPaneContainerScope(
        navigator = scope.navigator,
        activePaneRole = node.activePaneRole,
        paneContents = paneContents,
        isExpanded = isExpanded,
        isTransitioning = false, // Transition state tracked by AnimatedNavContent
        onNavigateToPane = { role ->
            // Use TreeMutator directly to switch active pane
            val currentState = scope.navigator.state.value
            val newState = TreeMutator.switchActivePane(currentState, node.key, role)
            @Suppress("DEPRECATION")
            scope.navigator.updateState(newState)
        }
    )

    // Lifecycle management: attach/detach UI lifecycle
    // Register destroy callback for explicit cache cleanup when the node
    // is permanently removed from the navigation tree.
    DisposableEffect(node) {
        node.attachToUI()

        val cleanupCallback: () -> Unit = {
            scope.cache.removeEntry(node.key.value, scope.saveableStateHolder)
        }
        node.addOnDestroyCallback(cleanupCallback)

        onDispose {
            node.detachFromUI()
        }
    }

    // Provide container node to children for container-scoped operations
    CompositionLocalProvider(LocalContainerNode provides node) {
        if (isExpanded) {
            // Expanded mode: render wrapper with multiple pane content slots
            // The wrapper can use paneContainerScope.paneContents to arrange panes
            // Use CachedEntry to preserve pane state, but key by node.hashCode()
            // during predictive back to allow rendering both old and new states
            // (different node objects will have different hashCodes even with same key)
            val cacheKey = node.key
            scope.cache.CachedEntry(
                key = cacheKey.value,
                saveableStateHolder = scope.saveableStateHolder
            ) {
                MultiPaneRenderer(
                    node = node,
                    scope = scope,
                    paneContainerScope = paneContainerScope
                )
            }
        } else {
            // Compact mode: behave like a stack (single pane visible)
            SinglePaneRenderer(
                node = node,
                previousNode = previousNode,
                scope = scope
            )
        }
    }
}

/**
 * Renders multiple panes in expanded mode using the pane wrapper.
 *
 * This helper function handles expanded (multi-pane) layout by invoking the
 * registered [com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry.PaneContainer].
 * The wrapper receives [PaneContainerScope] which includes [PaneContainerScope.paneContents]
 * - a list of [PaneContent] slots that can be arranged in a custom layout.
 *
 * ## Wrapper Layout Control
 *
 * Wrappers can use `scope.paneContents` to arrange panes in any layout:
 * ```kotlin
 * @PaneContainer(MyPane::class)
 * fun MyPaneWrapper(scope: PaneContainerScope, content: @Composable () -> Unit) {
 *     Row {
 *         scope.paneContents.filter { it.isVisible }.forEach { pane ->
 *             Box(Modifier.weight(if (pane.role == PaneRole.Primary) 0.4f else 0.6f)) {
 *                 pane.content()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * The `content` slot is also provided for backward compatibility but renders
 * panes sequentially. Custom layouts should use `scope.paneContents` instead.
 *
 * @param node The pane node being rendered
 * @param scope The render scope with dependencies
 * @param paneContainerScope Scope for the wrapper with pane state and content slots
 */
@Composable
private fun MultiPaneRenderer(
    node: PaneNode,
    scope: NavRenderScope,
    paneContainerScope: PaneContainerScope
) {
    // Invoke the registered pane container (KSP-generated or default)
    // The container receives the scope with paneContents for custom layout
    // Use wrapperKey (FQCN from DSL/KSP) for stable lookup; fall back to node.key for manual construction
    scope.containerRegistry.PaneContainer(
        paneNodeKey = node.wrapperKey ?: node.key.value,
        scope = paneContainerScope
    ) {
        // Default content slot: renders each visible pane sequentially
        // Custom wrappers should use scope.paneContents instead for layout control
        paneContainerScope.paneContents.filter { it.isVisible }.forEach { paneContent ->
            paneContent.content()
        }
    }
}

/**
 * Renders a single pane in compact mode with stack-like behavior.
 *
 * This helper function handles compact (single-pane) layout, showing only the
 * active pane with animated transitions when switching between panes. It behaves
 * similarly to [StackRenderer], providing smooth transitions while preserving
 * the state of all panes in the background.
 *
 * ## Animation
 *
 * Uses [AnimatedNavContent] with pane-specific transitions from
 * [com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator.getPaneTransition]. The animation direction is
 * determined by comparing the previous and current active pane roles.
 *
 * ## Predictive Back
 *
 * Predictive back is delegated to [AnimatedNavContent] via the `predictiveBackEnabled`
 * parameter. The pane computes whether predictive back should be active based on
 * whether the cascade state targets this pane node.
 *
 * @param node The pane node being rendered
 * @param previousNode Previous state for animation coordination
 * @param scope The render scope with dependencies
 */
@Composable
private fun SinglePaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope
) {
    val activePaneContent = node.activePaneContent ?: return
    val previousActivePaneContent = previousNode?.activePaneContent

    val transition = scope.animationCoordinator.getPaneTransition(
        fromRole = previousNode?.activePaneRole,
        toRole = node.activePaneRole
    )

    val isBackNavigation = previousNode != null &&
        previousNode.activePaneRole != PaneRole.Primary &&
        node.activePaneRole == PaneRole.Primary

    val predictiveBackEnabled = scope.predictiveBackController.let { ctrl ->
        val cascadeState = ctrl.cascadeState.value
        !ctrl.isActive.value ||
            (cascadeState != null && cascadeState.animatingStackKey == node.key)
    }

    AnimatedNavContent(
        targetState = activePaneContent,
        transition = transition,
        isBackNavigation = isBackNavigation,
        scope = scope,
        predictiveBackEnabled = predictiveBackEnabled,
        modifier = Modifier
    ) { paneNavNode ->
        NavNodeRenderer(
            node = paneNavNode,
            previousNode = previousActivePaneContent,
            scope = scope
        )
    }
}

/**
 * Builds a list of [PaneContent] for each configured pane in the node,
 * with content lambdas that render the pane's NavNode.
 *
 * This function creates content slots for the pane wrapper, determining
 * visibility based on the adaptive mode and each pane's [AdaptStrategy].
 * Each [PaneContent.content] lambda renders the pane's content via [NavNodeRenderer].
 *
 * ## Visibility Logic
 *
 * - **Expanded mode**: All configured panes are visible by default.
 *   This enables side-by-side layouts on tablets, foldables, and desktops.
 *
 * - **Compact mode**: Only the active pane is visible.
 *   AdaptStrategy determines behavior when navigating between panes.
 *
 * ## Content Rendering
 *
 * Each content lambda captures the pane's NavNode and render scope,
 * enabling the wrapper to call `paneContent.content()` to render each pane.
 *
 * @param node The pane node containing configurations
 * @param previousNode Previous state for animation coordination
 * @param isExpanded Whether in expanded (multi-pane) mode
 * @param scope The render scope with dependencies
 * @return List of [PaneContent] for each configured pane with rendering lambdas
 */
@Composable
private fun buildPaneContentListWithRenderers(
    node: PaneNode,
    previousNode: PaneNode?,
    isExpanded: Boolean,
    scope: NavRenderScope
): List<PaneContent> {
    return node.configuredRoles.map { role ->
        val paneNavNode = node.paneContent(role)
        val previousPaneNavNode = previousNode?.paneContent(role)

        // Check if this pane has actual content (non-empty stack)
        val paneHasContent = paneNavNode?.activeStack()?.children?.isNotEmpty() == true

        PaneContent(
            role = role,
            content = {
                if (paneNavNode != null) {
                    NavNodeRenderer(
                        node = paneNavNode,
                        previousNode = previousPaneNavNode,
                        scope = scope
                    )
                }
            },
            // In expanded mode, ALL configured panes are visible for side-by-side layout.
            // In compact mode, only the active pane is visible.
            isVisible = if (isExpanded) true else role == node.activePaneRole,
            hasContent = paneHasContent
        )
    }
}

