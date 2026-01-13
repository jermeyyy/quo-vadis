@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.compose.internal.render

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            scope.navigator.updateState(newState)
        }
    )

    // Lifecycle management: attach/detach UI lifecycle
    DisposableEffect(node.key) {
        node.attachToUI()
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
                key = cacheKey,
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
    // Use scopeKey for wrapper lookup since that's what KSP generates
    scope.containerRegistry.PaneContainer(
        paneNodeKey = node.scopeKey ?: node.key,
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
 * Predictive back IS enabled for pane switching when on a non-PRIMARY pane.
 * This allows gesture-driven animation showing PRIMARY pane content behind
 * the current SECONDARY pane during back gestures.
 *
 * @param node The pane node being rendered
 * @param previousNode Previous state for animation coordination
 * @param scope The render scope with dependencies
 */
@Suppress("VariableNeverRead", "AssignedValueIsNeverRead")
@Composable
private fun SinglePaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope
) {
    // Get the active pane content
    val activePaneContent = node.activePaneContent ?: return
    val previousActivePaneContent = previousNode?.activePaneContent

    // Track state for predictive back - must use remembered state to survive recomposition
    // This mirrors what AnimatedNavContent does internally
    var lastCommittedContent by remember { mutableStateOf(activePaneContent) }
    var lastCommittedRole by remember { mutableStateOf(node.activePaneRole) }

    // Get transition for pane switching
    val transition = scope.animationCoordinator.getPaneTransition(
        fromRole = previousNode?.activePaneRole,
        toRole = node.activePaneRole
    )

    // Determine if this is back navigation (switching from non-primary to primary)
    val isBackNavigation = previousNode != null &&
        previousNode.activePaneRole != PaneRole.Primary &&
        node.activePaneRole == PaneRole.Primary

    // Check predictive back state
    val cascadeState = scope.predictiveBackController.cascadeState.value
    val isGestureActive = scope.predictiveBackController.isActive.value

    // This pane handles predictive back if gesture is active AND cascade state targets this pane
    val isPredictiveBackActive = isGestureActive &&
        cascadeState != null &&
        cascadeState.animatingStackKey == node.key

    // Get the PRIMARY pane content for predictive back animation target
    val primaryPaneContent = node.paneContent(PaneRole.Primary)

    if (isPredictiveBackActive && primaryPaneContent != null) {
        // Gesture-driven animation - show PRIMARY behind SECONDARY
        // Use lastCommittedContent (the SECONDARY content before back started)
        PredictiveBackContent(
            current = lastCommittedContent,
            previous = primaryPaneContent,
            progress = scope.predictiveBackController.progress.value,
            scope = scope
        ) { paneNavNode ->
            // Render each pane content via NavNodeRenderer
            NavNodeRenderer(
                node = paneNavNode,
                previousNode = null,
                scope = scope,
            )
        }
        // Note: Do NOT update lastCommittedContent during predictive back
        // We want to keep showing the "old" state (SECONDARY) as current
    } else {
        // Standard AnimatedContent transition
        AnimatedNavContent(
            targetState = activePaneContent,
            transition = transition,
            isBackNavigation = isBackNavigation,
            scope = scope,
            predictiveBackEnabled = false, // We handle predictive back above
            modifier = Modifier
        ) { paneNavNode ->
            // Recurse to render the active pane content
            NavNodeRenderer(
                node = paneNavNode,
                previousNode = previousActivePaneContent,
                scope = scope
            )
        }

        // Update state tracking AFTER rendering, only when not in predictive back
        // This tracks the "committed" state for when predictive back starts
        if (activePaneContent.key != lastCommittedContent.key) {
            lastCommittedContent = activePaneContent
            lastCommittedRole = node.activePaneRole
        }
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

