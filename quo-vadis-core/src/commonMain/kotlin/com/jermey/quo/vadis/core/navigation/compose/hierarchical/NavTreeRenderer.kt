package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.WindowSizeClass
import com.jermey.quo.vadis.core.navigation.compose.WindowWidthSizeClass
import com.jermey.quo.vadis.core.navigation.compose.calculateWindowSizeClass
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContent
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabMetadata
import com.jermey.quo.vadis.core.navigation.compose.wrapper.internal.createPaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.internal.createTabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
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
 * // Inside QuoVadisHost
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
internal fun NavTreeRenderer(
    node: NavNode,
    previousNode: NavNode?,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    when (node) {
        is ScreenNode -> ScreenRenderer(
            node = node,
            scope = scope,
            modifier = modifier
        )

        is StackNode -> StackRenderer(
            node = node,
            previousNode = previousNode as? StackNode,
            scope = scope,
            modifier = modifier
        )

        is TabNode -> TabRenderer(
            node = node,
            previousNode = previousNode as? TabNode,
            scope = scope,
            modifier = modifier
        )

        is PaneNode -> PaneRenderer(
            node = node,
            previousNode = previousNode as? PaneNode,
            scope = scope,
            modifier = modifier
        )
    }
}

// region Placeholder Renderers (to be implemented in HIER-017 through HIER-022)

/**
 * Renders a [ScreenNode] leaf node.
 *
 * This renderer handles the terminal state in the navigation tree,
 * invoking the screen content via [NavRenderScope.screenRegistry].
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
 * ## ScreenRegistry Integration
 *
 * Invokes [ScreenRegistry.Content] with:
 * - `destination`: The destination from the ScreenNode
 * - `navigator`: For performing navigation actions
 * - `sharedTransitionScope`: For shared element transitions (may be null)
 * - `animatedVisibilityScope`: For coordinated animations (may be null)
 *
 * @param node The screen node containing the destination to render
 * @param scope The render scope with dependencies and context
 * @param modifier Modifier to apply to the screen content
 *
 * @see ScreenNode
 * @see LocalScreenNode
 * @see LocalAnimatedVisibilityScope
 */
@Composable
internal fun ScreenRenderer(
    node: ScreenNode,
    scope: NavRenderScope,
    modifier: Modifier
) {
    // Use cache for state preservation across navigation transitions
    scope.cache.CachedEntry(
        key = node.key,
        saveableStateHolder = scope.saveableStateHolder
    ) {
        // Provide composition locals for screen content access
        CompositionLocalProvider(
            LocalScreenNode provides node,
            LocalAnimatedVisibilityScope provides LocalAnimatedVisibilityScope.current
        ) {
            // Wrap content with modifier (typically fillMaxSize from parent)
            Box(modifier = modifier) {
                // Invoke screen content via registry
                scope.screenRegistry.Content(
                    destination = node.destination,
                    navigator = scope.navigator,
                    sharedTransitionScope = scope.sharedTransitionScope,
                    animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                )
            }
        }
    }
}

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
 * This direction is passed to [AnimationCoordinator.getTransition] to select
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
 * @param modifier Modifier to apply to the stack container
 *
 * @see StackNode
 * @see AnimatedNavContent
 * @see NavTreeRenderer
 */
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
    modifier: Modifier
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

    // Enable predictive back only for root stacks (no parent container)
    // Nested stacks within tabs/panes should not handle predictive back
    val predictiveBackEnabled = node.parentKey == null

    // Animated content switching with transition
    AnimatedNavContent(
        targetState = activeChild,
        transition = transition,
        scope = scope,
        predictiveBackEnabled = predictiveBackEnabled,
        modifier = modifier
    ) { child ->
        // Recurse to render the active child
        NavTreeRenderer(
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
private fun detectBackNavigation(current: StackNode, previous: StackNode?): Boolean {
    if (previous == null) return false
    // Back navigation: stack shrunk (pop operation)
    return current.children.size < previous.children.size
}

/**
 * Renders a [TabNode] with wrapper composition and tab switching animations.
 *
 * This renderer maintains the parent-child relationship between tab wrappers
 * and their content, ensuring coordinated animations and predictive back gestures.
 * It creates a [TabWrapperScope] for the wrapper composable, caches the entire
 * tab structure (wrapper + content), and handles animated transitions between tabs.
 *
 * ## Wrapper Composition
 *
 * The renderer invokes [WrapperRegistry.TabWrapper] with a content slot. The wrapper
 * is responsible for rendering the tab UI (bottom navigation, tab bar, etc.), while
 * the content slot renders the active tab's content with animations.
 *
 * ## Caching Strategy
 *
 * The **entire** TabNode (wrapper + content) is cached as a single unit using
 * [ComposableCache.CachedEntry]. This ensures that:
 * - The wrapper maintains its state across navigations within tabs
 * - Tab switching animations are smooth
 * - The tab container is not recreated during back navigation
 *
 * ## Tab Switching Animation
 *
 * When the active tab changes, [AnimatedNavContent] animates the transition
 * between tab contents. The animation direction is determined by comparing
 * the previous and current tab indices:
 * - Lower to higher index: Slide left-to-right
 * - Higher to lower index: Slide right-to-left
 * - Same index (initial): Fade in
 *
 * Note: Tab switching does NOT use predictive back gestures. Predictive back
 * is handled at the stack level within each tab.
 *
 * ## Example Structure
 *
 * ```
 * TabRenderer(tabNode)
 *   └── CachedEntry(tabNode.key)
 *         └── TabWrapper(scope, content)
 *               └── AnimatedNavContent(activeStack)
 *                     └── NavTreeRenderer(stack)
 *                           └── StackRenderer/ScreenRenderer...
 * ```
 *
 * @param node The tab node to render
 * @param previousNode The previous tab state for animation direction detection
 * @param scope The render scope with dependencies and context
 * @param modifier Modifier to apply to the tab container
 *
 * @see TabNode
 * @see TabWrapperScope
 * @see WrapperRegistry.TabWrapper
 * @see AnimatedNavContent
 */
@Composable
internal fun TabRenderer(
    node: TabNode,
    previousNode: TabNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    // Get active stack for the current and previous state
    val activeStack = node.activeStack
    val previousActiveStack = previousNode?.activeStack

    // Create TabWrapperScope with tab navigation state and actions
    // The scope is remembered to maintain referential stability
    val tabWrapperScope = remember(node.key, node.activeStackIndex, node.tabCount) {
        createTabWrapperScope(
            navigator = scope.navigator,
            activeTabIndex = node.activeStackIndex,
            tabMetadata = createTabMetadataFromStacks(node),
            isTransitioning = false, // Transition state is tracked by AnimatedNavContent
            onSwitchTab = { index -> scope.navigator.switchTab(index) }
        )
    }

    // Derive updated scope when active index changes (without recreating)
    val updatedTabWrapperScope by remember(tabWrapperScope) {
        derivedStateOf {
            createTabWrapperScope(
                navigator = scope.navigator,
                activeTabIndex = node.activeStackIndex,
                tabMetadata = createTabMetadataFromStacks(node),
                isTransitioning = false,
                onSwitchTab = { index -> scope.navigator.switchTab(index) }
            )
        }
    }

    // Cache the ENTIRE TabNode (wrapper + content) as a unit
    // This ensures the wrapper maintains state during navigation
    scope.cache.CachedEntry(
        key = node.key,
        saveableStateHolder = scope.saveableStateHolder
    ) {
        // Apply the modifier to the wrapper
        Box(modifier = modifier) {
            // Invoke the registered tab wrapper (KSP-generated or default)
            // The wrapper receives the scope and a content slot
            // Use wrapperKey for registry lookup (class simple name), fallback to node.key
            scope.wrapperRegistry.TabWrapper(
                tabNodeKey = node.wrapperKey ?: node.key,
                scope = updatedTabWrapperScope
            ) {
                // Content slot: animate between tabs (within the wrapper)
                AnimatedNavContent(
                    targetState = activeStack,
                    transition = scope.animationCoordinator.getTabTransition(
                        fromIndex = previousNode?.activeStackIndex,
                        toIndex = node.activeStackIndex
                    ),
                    scope = scope,
                    // Tab switching is NOT via predictive back
                    // Predictive back is handled within each tab's stack
                    predictiveBackEnabled = false,
                    modifier = Modifier
                ) { stack ->
                    // Recurse to render the active stack
                    NavTreeRenderer(
                        node = stack,
                        previousNode = previousActiveStack,
                        scope = scope
                    )
                }
            }
        }
    }
}

/**
 * Creates [TabMetadata] list from a [TabNode].
 *
 * If the [TabNode.tabMetadata] list (from KSP-generated code) is non-empty,
 * it is converted to runtime [TabMetadata] objects. Otherwise, fallback metadata
 * is generated from stack keys and indices.
 *
 * @param node The TabNode to extract metadata from
 * @return List of [TabMetadata] for each tab in order
 */
private fun createTabMetadataFromStacks(node: TabNode): List<TabMetadata> {
    // Use KSP-generated metadata if available
    if (node.tabMetadata.isNotEmpty()) {
        return node.tabMetadata.map { generated ->
            TabMetadata(
                label = generated.label,
                icon = null, // Icons are resolved by wrapper via route-based fallback
                route = generated.route,
                contentDescription = null,
                badge = null
            )
        }
    }

    // Fallback: generate metadata from stack keys
    return node.stacks.mapIndexed { index, stack ->
        TabMetadata(
            label = stack.key.substringAfterLast("/").takeIf { it.isNotEmpty() }
                ?: "Tab ${index + 1}",
            icon = null,
            route = stack.key,
            contentDescription = null,
            badge = null
        )
    }
}

/**
 * Renders a [PaneNode] with adaptive layout based on window size.
 *
 * This renderer handles multi-pane layouts that adapt to different screen sizes,
 * showing multiple panes side-by-side on large screens or collapsing to single-pane
 * on compact screens.
 *
 * ## Adaptive Behavior
 *
 * The renderer detects the current window size class and adapts the layout:
 *
 * - **Expanded mode** (width >= [WindowWidthSizeClass.Medium]): Multiple panes displayed
 *   side-by-side using [WrapperRegistry.PaneWrapper]. The wrapper receives [PaneContent]
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
 *         └── [Expanded] PaneWrapper(scope, content)
 *         │     └── Multiple NavTreeRenderer for each visible pane
 *         └── [Compact] AnimatedNavContent(activePaneContent)
 *               └── NavTreeRenderer(activePane)
 * ```
 *
 * @param node The pane node to render
 * @param previousNode The previous pane state for animation coordination
 * @param scope The render scope with dependencies and context
 * @param modifier Modifier to apply to the pane container
 *
 * @see PaneNode
 * @see PaneWrapperScope
 * @see WrapperRegistry.PaneWrapper
 * @see WindowSizeClass
 */
@Composable
internal fun PaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    // Detect window size class for adaptive layout decision
    val windowSizeClass = calculateWindowSizeClass()
    val isExpanded = windowSizeClass.isAtLeastMediumWidth

    // Build PaneContent list for each configured pane
    // This creates content slots with visibility based on adaptive mode
    val paneContents = remember(node.paneConfigurations, isExpanded, node.activePaneRole) {
        buildPaneContentList(node, isExpanded)
    }

    // Create PaneWrapperScope with pane navigation state and actions
    val paneWrapperScope = remember(
        node.key,
        node.activePaneRole,
        paneContents,
        isExpanded
    ) {
        createPaneWrapperScope(
            navigator = scope.navigator,
            activePaneRole = node.activePaneRole,
            paneContents = paneContents,
            isExpanded = isExpanded,
            isTransitioning = false, // Transition state tracked by AnimatedNavContent
            onNavigateToPane = { role -> scope.navigator.switchPane(role) }
        )
    }

    // Derive updated scope when active role or expanded state changes
    val updatedPaneWrapperScope by remember(paneWrapperScope) {
        derivedStateOf {
            createPaneWrapperScope(
                navigator = scope.navigator,
                activePaneRole = node.activePaneRole,
                paneContents = buildPaneContentList(node, isExpanded),
                isExpanded = isExpanded,
                isTransitioning = false,
                onNavigateToPane = { role -> scope.navigator.switchPane(role) }
            )
        }
    }

    // Cache the ENTIRE PaneNode (wrapper + all pane contents) as a unit
    // This ensures all pane states are preserved during layout changes
    scope.cache.CachedEntry(
        key = node.key,
        saveableStateHolder = scope.saveableStateHolder
    ) {
        // Apply the modifier to the wrapper
        Box(modifier = modifier) {
            if (isExpanded) {
                // Expanded mode: render wrapper with multiple pane content slots
                MultiPaneRenderer(
                    node = node,
                    previousNode = previousNode,
                    scope = scope,
                    paneWrapperScope = updatedPaneWrapperScope,
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
 * registered [WrapperRegistry.PaneWrapper] with content slots for each visible pane.
 * The wrapper is responsible for arranging panes side-by-side based on their roles.
 *
 * ## Content Slot Rendering
 *
 * Each [PaneContent] slot's content lambda recursively renders its [NavNode]
 * via [NavTreeRenderer]. This maintains the proper Compose hierarchy and enables:
 * - Independent state management per pane
 * - Proper animations within each pane
 * - Nested navigation support (stacks within panes)
 *
 * @param node The pane node being rendered
 * @param previousNode Previous state for animation coordination
 * @param scope The render scope with dependencies
 * @param paneWrapperScope Scope for the wrapper with pane state
 * @param paneContents List of content slots for each pane
 */
@Composable
private fun MultiPaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope,
    paneWrapperScope: PaneWrapperScope,
    paneContents: List<PaneContent>
) {
    // Invoke the registered pane wrapper (KSP-generated or default)
    // The wrapper receives the scope and a content slot
    scope.wrapperRegistry.PaneWrapper(
        paneNodeKey = node.key,
        scope = paneWrapperScope
    ) {
        // Content slot: render each visible pane
        // The wrapper is responsible for layout arrangement
        paneContents.filter { it.isVisible }.forEach { paneContent ->
            // Get the NavNode content for this pane role
            val paneNavNode = node.paneContent(paneContent.role)
            val previousPaneNavNode = previousNode?.paneContent(paneContent.role)

            if (paneNavNode != null) {
                // Recurse to render the pane content
                NavTreeRenderer(
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
 * [AnimationCoordinator.getPaneTransition]. The animation direction is
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
        NavTreeRenderer(
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
private fun buildPaneContentList(
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

// endregion
