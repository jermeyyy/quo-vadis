package com.jermey.quo.vadis.core.compose.render

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContainerScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContent
import com.jermey.quo.vadis.core.navigation.compose.wrapper.WindowSizeClass
import com.jermey.quo.vadis.core.navigation.compose.wrapper.WindowWidthSizeClass
import com.jermey.quo.vadis.core.navigation.compose.wrapper.calculateWindowSizeClass
import com.jermey.quo.vadis.core.navigation.compose.wrapper.createPaneContainerScope
import com.jermey.quo.vadis.core.navigation.core.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.TreeMutator

/**
 * Renders a [PaneNode] with adaptive layout based on window size.
 *
 * This renderer handles multi-pane layouts that adapt to different screen sizes,
 * showing multiple panes side-by-side on large screens or collapsing to single-pane
 * on compact screens.
 *
 * ## Lifecycle Management
 *
 * The renderer manages the pane container's UI lifecycle via [com.jermey.quo.vadis.core.navigation.core.LifecycleAwareNode]:
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
 * @see com.jermey.quo.vadis.core.navigation.core.LifecycleAwareNode.attachToUI
 * @see com.jermey.quo.vadis.core.navigation.core.LifecycleAwareNode.detachFromUI
 */
@Composable
internal fun PaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope,
) {
    // Detect window size class for adaptive layout decision
    val windowSizeClass = calculateWindowSizeClass()
    val isExpanded = windowSizeClass.isAtLeastMediumWidth

    // Build PaneContent list for each configured pane
    // This creates content slots with visibility based on adaptive mode
    val paneContents = remember(node.paneConfigurations, isExpanded, node.activePaneRole) {
        buildPaneContentList(node, isExpanded)
    }

    // Create PaneContainerScope with pane navigation state and actions
    val paneContainerScope = remember(
        node.key,
        node.activePaneRole,
        paneContents,
        isExpanded
    ) {
        createPaneContainerScope(
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
    }

    // Derive updated scope when active role or expanded state changes
    val updatedPaneContainerScope by remember(paneContainerScope) {
        derivedStateOf {
            createPaneContainerScope(
                navigator = scope.navigator,
                activePaneRole = node.activePaneRole,
                paneContents = buildPaneContentList(node, isExpanded),
                isExpanded = isExpanded,
                isTransitioning = false,
                onNavigateToPane = { role ->
                    // Use TreeMutator directly to switch active pane
                    val currentState = scope.navigator.state.value
                    val newState = TreeMutator.switchActivePane(currentState, node.key, role)
                    scope.navigator.updateState(newState)
                }
            )
        }
    }

    // Cache the ENTIRE PaneNode (wrapper + all pane contents) as a unit
    // This ensures all pane states are preserved during layout changes
    scope.cache.CachedEntry(
        key = node.key,
        saveableStateHolder = scope.saveableStateHolder
    ) {
        // Lifecycle management: attach/detach UI lifecycle
        DisposableEffect(node) {
            node.attachToUI()
            onDispose {
                node.detachFromUI()
            }
        }

        // Provide container node to children for container-scoped operations
        CompositionLocalProvider(LocalContainerNode provides node) {
            if (isExpanded) {
                // Expanded mode: render wrapper with multiple pane content slots
                MultiPaneRenderer(
                    node = node,
                    previousNode = previousNode,
                    scope = scope,
                    paneContainerScope = updatedPaneContainerScope,
                    paneContents = paneContents
                )
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
}

/**
 * Renders multiple panes in expanded mode using the pane wrapper.
 *
 * This helper function handles expanded (multi-pane) layout by invoking the
 * registered [com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry.PaneContainer] with content slots for each visible pane.
 * The wrapper is responsible for arranging panes side-by-side based on their roles.
 *
 * ## Content Slot Rendering
 *
 * Each [PaneContent] slot's content lambda recursively renders its [NavNode]
 * via [NavNodeRenderer]. This maintains the proper Compose hierarchy and enables:
 * - Independent state management per pane
 * - Proper animations within each pane
 * - Nested navigation support (stacks within panes)
 *
 * @param node The pane node being rendered
 * @param previousNode Previous state for animation coordination
 * @param scope The render scope with dependencies
 * @param paneContainerScope Scope for the wrapper with pane state
 * @param paneContents List of content slots for each pane
 */
@Composable
private fun MultiPaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope,
    paneContainerScope: PaneContainerScope,
    paneContents: List<PaneContent>
) {
    // Invoke the registered pane container (KSP-generated or default)
    // The container receives the scope and a content slot
    scope.containerRegistry.PaneContainer(
        paneNodeKey = node.key,
        scope = paneContainerScope
    ) {
        // Content slot: render each visible pane
        // The wrapper is responsible for layout arrangement
        paneContents.filter { it.isVisible }.forEach { paneContent ->
            // Get the NavNode content for this pane role
            val paneNavNode = node.paneContent(paneContent.role)
            val previousPaneNavNode = previousNode?.paneContent(paneContent.role)

            if (paneNavNode != null) {
                // Recurse to render the pane content
                NavNodeRenderer(
                    node = paneNavNode,
                    previousNode = previousPaneNavNode,
                    scope = scope
                )
            }
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
 * Predictive back is NOT enabled for pane switching, as this is semantic
 * navigation (changing which pane is focused) rather than hierarchical
 * back navigation. Predictive back is handled by the stacks within panes.
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
    // Get the active pane content
    val activePaneContent = node.activePaneContent ?: return
    val previousActivePaneContent = previousNode?.activePaneContent

    // Get transition for pane switching
    val transition = scope.animationCoordinator.getPaneTransition(
        fromRole = previousNode?.activePaneRole,
        toRole = node.activePaneRole
    )

    // Animated content switching between panes
    AnimatedNavContent(
        targetState = activePaneContent,
        transition = transition,
        scope = scope,
        // Pane switching is NOT via predictive back
        // Predictive back is handled within each pane's stack
        predictiveBackEnabled = false,
        modifier = Modifier
    ) { paneNavNode ->
        // Recurse to render the active pane content
        NavNodeRenderer(
            node = paneNavNode,
            previousNode = previousActivePaneContent,
            scope = scope
        )
    }
}

/**
 * Builds a list of [PaneContent] for each configured pane in the node.
 *
 * This function creates content slots for the pane wrapper, determining
 * visibility based on the adaptive mode and each pane's [AdaptStrategy].
 *
 * ## Visibility Logic
 *
 * - **Expanded mode**: All panes are potentially visible
 *   - Primary: Always visible
 *   - Other roles: Visible unless their [AdaptStrategy] is [AdaptStrategy.Hide]
 *
 * - **Compact mode**: Only the active pane is visible
 *
 * @param node The pane node containing configurations
 * @param isExpanded Whether in expanded (multi-pane) mode
 * @return List of [PaneContent] for each configured pane
 */
internal fun buildPaneContentList(
    node: PaneNode,
    isExpanded: Boolean
): List<PaneContent> {
    return node.configuredRoles.map { role ->
        val config = node.paneConfigurations[role]!!
        PaneContent(
            role = role,
            content = {}, // Content is rendered separately in the wrapper
            isVisible = when {
                !isExpanded -> role == node.activePaneRole
                role == PaneRole.Primary -> true
                else -> config.adaptStrategy != AdaptStrategy.Hide
            }
        )
    }
}
