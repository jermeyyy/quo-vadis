package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode

/**
 * Transforms a NavNode tree into a FlattenResult containing RenderableSurfaces.
 *
 * The TreeFlattener performs a depth-first traversal of the navigation tree,
 * extracting only the currently active/visible nodes and converting them into
 * renderable surfaces with appropriate z-ordering and animation pairing.
 *
 * ## Algorithm Overview (Screen/Stack)
 *
 * 1. Start at the root node with base z-order 0
 * 2. For ScreenNode: Create a single surface with SINGLE_SCREEN mode
 * 3. For StackNode: Flatten active child, track previous for animations
 * 4. Populate previousSurfaceId for animation coordination
 * 5. Return FlattenResult with surfaces, animation pairs, and caching hints
 *
 * ## Z-Order Strategy
 *
 * - Root level: 0
 * - Each depth level adds [zOrderIncrement] (default: 100)
 * - During transitions, exiting surface gets current z-order, entering gets +50
 * - This ensures proper layering during animations
 *
 * ## Usage Example
 *
 * ```kotlin
 * val flattener = TreeFlattener(
 *     contentResolver = { node ->
 *         @Composable { ScreenContent(node as ScreenNode) }
 *     }
 * )
 *
 * val result = flattener.flattenState(rootNode, previousRootNode)
 * // Use result.surfaces in QuoVadisHost
 * ```
 *
 * @property contentResolver Function to resolve NavNode to @Composable content
 * @property animationResolver Function to resolve animations for node transitions
 * @property zOrderIncrement Z-order increment per depth level
 */
public class TreeFlattener(
    private val contentResolver: ContentResolver,
    private val animationResolver: AnimationResolver = DefaultAnimationResolver,
    private val zOrderIncrement: Int = DEFAULT_Z_ORDER_INCREMENT
) {

    /**
     * Resolves a NavNode to its composable content.
     *
     * Implementations should return a stable composable lambda that renders
     * the appropriate content for the given node type. Note that this function
     * is NOT @Composable itself - it creates a lambda that will be invoked
     * during composition.
     *
     * ## Example Implementation
     *
     * ```kotlin
     * val resolver = ContentResolver { node ->
     *     { // This lambda will be invoked during composition
     *         when (node) {
     *             is ScreenNode -> ScreenContent(node.destination)
     *             else -> { /* Container handled by flattener */ }
     *         }
     *     }
     * }
     * ```
     */
    public fun interface ContentResolver {
        /**
         * Resolves a NavNode to its composable content.
         *
         * This method is called during tree flattening (NOT during composition).
         * The returned lambda will be invoked later during the actual Compose
         * rendering phase.
         *
         * @param node The node to resolve content for
         * @return A composable lambda that renders the node's content
         */
        public fun resolve(node: NavNode): @Composable () -> Unit
    }

    /**
     * Resolves animation specs for node transitions.
     *
     * Implementations can provide custom animations based on:
     * - The from/to nodes involved in the transition
     * - The type of transition (push, pop, tab switch, etc.)
     *
     * ## Example Implementation
     *
     * ```kotlin
     * val resolver = AnimationResolver { from, to, transitionType ->
     *     when {
     *         transitionType == TransitionType.PUSH -> slideInFromRight()
     *         transitionType == TransitionType.POP -> slideInFromLeft()
     *         else -> SurfaceAnimationSpec.None
     *     }
     * }
     * ```
     */
    public fun interface AnimationResolver {
        /**
         * Resolves the animation specification for a transition.
         *
         * @param from The node being navigated from (null if entering for first time)
         * @param to The node being navigated to (null if exiting completely)
         * @param transitionType The type of transition occurring
         * @return The animation specification to apply
         */
        public fun resolve(
            from: NavNode?,
            to: NavNode?,
            transitionType: TransitionType
        ): SurfaceAnimationSpec
    }

    /**
     * Context passed during flattening to track state.
     *
     * This internal class carries context information through the recursive
     * flattening process, avoiding the need for mutable state.
     *
     * @property baseZOrder The z-order to use for surfaces at this level
     * @property parentId The ID of the parent container, if any
     * @property previousSiblingId ID of the previous sibling for animation pairing
     * @property transitionType The type of transition being processed
     * @property previousRoot The previous navigation tree root for tab switch detection
     * @property windowSizeClass The current window size classification for adaptive rendering
     */
    private data class FlattenContext(
        val baseZOrder: Int,
        val parentId: String?,
        val previousSiblingId: String?,
        val transitionType: TransitionType,
        val previousRoot: NavNode? = null,
        val windowSizeClass: WindowSizeClass = WindowSizeClass.Compact
    )

    /**
     * Accumulated state during flattening.
     *
     * This mutable accumulator collects surfaces and animation pairs
     * during the recursive traversal, then produces an immutable result.
     */
    private class FlattenAccumulator {
        val surfaces = mutableListOf<RenderableSurface>()
        val animationPairs = mutableListOf<AnimationPair>()
        val cacheableIds = mutableSetOf<String>()
        val invalidatedIds = mutableSetOf<String>()
        val wrapperIds = mutableSetOf<String>()
        val contentIds = mutableSetOf<String>()
        var isCrossNodeNavigation = false

        fun addSurface(surface: RenderableSurface) {
            surfaces.add(surface)
        }

        fun addAnimationPair(pair: AnimationPair) {
            animationPairs.add(pair)
        }

        fun markCacheable(id: String) {
            cacheableIds.add(id)
        }

        fun markInvalidated(id: String) {
            invalidatedIds.add(id)
        }

        fun toResult(): FlattenResult {
            return FlattenResult(
                surfaces = surfaces.sortedBy { it.zOrder },
                animationPairs = animationPairs.toList(),
                cachingHints = CachingHints(
                    shouldCacheWrapper = isCrossNodeNavigation,
                    shouldCacheContent = true,
                    cacheableIds = cacheableIds.toSet(),
                    invalidatedIds = invalidatedIds.toSet(),
                    wrapperIds = wrapperIds.toSet(),
                    contentIds = contentIds.toSet(),
                    isCrossNodeTypeNavigation = isCrossNodeNavigation
                )
            )
        }
    }

    /**
     * Flattens the NavNode tree into a FlattenResult.
     *
     * This is the main entry point for tree flattening. It takes the current
     * root of the navigation tree and optionally the previous root for
     * transition detection.
     *
     * ## Behavior
     *
     * - For [ScreenNode]: Produces a single surface with [SurfaceRenderingMode.SINGLE_SCREEN]
     * - For [StackNode]: Produces surfaces with [SurfaceRenderingMode.STACK_CONTENT]
     *   for the active child, tracking previous child for animations
     * - For [TabNode]: Produces [SurfaceRenderingMode.TAB_WRAPPER] and [SurfaceRenderingMode.TAB_CONTENT]
     * - For [PaneNode]: Adaptive behavior based on [WindowSizeClass]:
     *   - Compact width: [SurfaceRenderingMode.PANE_AS_STACK] (single pane, stack-like)
     *   - Medium/Expanded: [SurfaceRenderingMode.PANE_WRAPPER] + [SurfaceRenderingMode.PANE_CONTENT]
     *
     * ## Example
     *
     * ```kotlin
     * // Initial navigation
     * val result1 = flattener.flattenState(rootNode)
     *
     * // After push navigation - detects PUSH transition
     * val result2 = flattener.flattenState(newRootNode, rootNode)
     *
     * // After pop navigation - detects POP transition
     * val result3 = flattener.flattenState(poppedRootNode, newRootNode)
     *
     * // With window size class for adaptive pane rendering
     * val result4 = flattener.flattenState(paneNode, null, WindowSizeClass.Expanded)
     * ```
     *
     * @param root The root of the navigation tree
     * @param previousRoot Optional previous root for transition detection
     * @param windowSizeClass The window size classification for adaptive PaneNode rendering
     * @return FlattenResult containing surfaces, animation pairs, and caching hints
     */
    public fun flattenState(
        root: NavNode,
        previousRoot: NavNode? = null,
        windowSizeClass: WindowSizeClass = WindowSizeClass.Compact
    ): FlattenResult {
        val accumulator = FlattenAccumulator()
        val transitionType = detectTransitionType(previousRoot, root)

        val context = FlattenContext(
            baseZOrder = 0,
            parentId = null,
            previousSiblingId = findPreviousSiblingId(previousRoot, root),
            transitionType = transitionType,
            previousRoot = previousRoot,
            windowSizeClass = windowSizeClass
        )

        flatten(root, context, accumulator)

        return accumulator.toResult()
    }

    /**
     * Internal recursive flattening function.
     *
     * Dispatches to node-type-specific handlers based on the concrete type.
     */
    private fun flatten(
        node: NavNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        when (node) {
            is ScreenNode -> flattenScreen(node, context, accumulator)
            is StackNode -> flattenStack(node, context, accumulator)
            is TabNode -> flattenTab(node, context, accumulator)
            is PaneNode -> flattenPane(node, context, accumulator)
        }
    }

    /**
     * Flattens a ScreenNode into a single surface.
     *
     * ScreenNodes are leaf nodes that always produce exactly one surface
     * with [SurfaceRenderingMode.SINGLE_SCREEN]. This is the simplest
     * flattening case.
     *
     * ## Output
     *
     * - Single [RenderableSurface] with:
     *   - `id` = screen.key
     *   - `renderingMode` = [SurfaceRenderingMode.SINGLE_SCREEN]
     *   - `previousSurfaceId` = context.previousSiblingId (for animations)
     *
     * @param screen The ScreenNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenScreen(
        screen: ScreenNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val animationSpec = animationResolver.resolve(
            from = null,
            to = screen,
            transitionType = context.transitionType
        )

        val surface = RenderableSurface(
            id = screen.key,
            zOrder = context.baseZOrder,
            nodeType = SurfaceNodeType.SCREEN,
            renderingMode = SurfaceRenderingMode.SINGLE_SCREEN,
            transitionState = SurfaceTransitionState.Visible,
            animationSpec = animationSpec,
            content = contentResolver.resolve(screen),
            parentWrapperId = context.parentId,
            previousSurfaceId = context.previousSiblingId
        )

        accumulator.addSurface(surface)

        // Add animation pair if there's a previous surface
        if (context.previousSiblingId != null) {
            accumulator.addAnimationPair(
                AnimationPair(
                    currentId = screen.key,
                    previousId = context.previousSiblingId,
                    transitionType = context.transitionType
                )
            )
        }
    }

    /**
     * Flattens a StackNode by processing its active child.
     *
     * Tracks the previous child for animation pairing and sets
     * [SurfaceRenderingMode.STACK_CONTENT] for proper rendering.
     *
     * ## Key Behaviors
     *
     * - Only the active (top) child is rendered as a surface
     * - Previous child's key is tracked for animation pairing
     * - Z-order increments for nested content
     * - Nested containers (Tab/Pane inside Stack) recurse naturally
     *
     * ## Output for Stack with ScreenNode children
     *
     * Given: `StackNode { ScreenNode("a"), ScreenNode("b") }`
     *
     * - Single [RenderableSurface] with:
     *   - `id` = "b" (active child)
     *   - `renderingMode` = [SurfaceRenderingMode.STACK_CONTENT]
     *   - `previousSurfaceId` = "a" (for animation pairing)
     *   - [AnimationPair] linking "b" to "a"
     *
     * @param stack The StackNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenStack(
        stack: StackNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val children = stack.children
        if (children.isEmpty()) return

        val activeChild = children.last()
        val previousChild = if (children.size > 1) children[children.size - 2] else null

        // Determine transition type based on stack state
        val transitionType = when {
            previousChild != null && context.transitionType == TransitionType.NONE -> TransitionType.PUSH
            else -> context.transitionType
        }

        val childContext = FlattenContext(
            baseZOrder = context.baseZOrder + zOrderIncrement,
            parentId = stack.key,
            previousSiblingId = previousChild?.key,
            transitionType = transitionType
        )

        // Flatten the active child
        when (activeChild) {
            is ScreenNode -> {
                // Use STACK_CONTENT mode for screens in a stack
                val animationSpec = animationResolver.resolve(
                    from = previousChild,
                    to = activeChild,
                    transitionType = transitionType
                )

                val surface = RenderableSurface(
                    id = activeChild.key,
                    zOrder = childContext.baseZOrder,
                    nodeType = SurfaceNodeType.SCREEN,
                    renderingMode = SurfaceRenderingMode.STACK_CONTENT,
                    transitionState = SurfaceTransitionState.Visible,
                    animationSpec = animationSpec,
                    content = contentResolver.resolve(activeChild),
                    parentWrapperId = stack.key,
                    previousSurfaceId = previousChild?.key
                )

                accumulator.addSurface(surface)

                // Add animation pair for stack navigation
                if (previousChild != null) {
                    accumulator.addAnimationPair(
                        AnimationPair(
                            currentId = activeChild.key,
                            previousId = previousChild.key,
                            transitionType = transitionType
                        )
                    )
                }
            }
            else -> {
                // For non-screen children (nested containers), recurse
                flatten(activeChild, childContext, accumulator)
            }
        }
    }

    /**
     * Flattens a TabNode into wrapper and content surfaces.
     *
     * Produces two surfaces:
     * 1. TAB_WRAPPER - Contains user's wrapper composable (scaffold, nav bar, etc.)
     * 2. TAB_CONTENT - Contains active tab's content
     *
     * ## Caching Strategy
     *
     * - **Cross-node navigation** (e.g., Stack → Tab): Cache WHOLE wrapper + content
     * - **Intra-tab navigation** (tab switch): Cache ONLY content surfaces, not wrapper
     *
     * ## Animation Support
     *
     * Content surface tracks `previousSurfaceId` for tab switch animations.
     * Tab switches are detected by comparing the current active index with
     * the previous root's active index (if it was also a TabNode with the same key).
     *
     * @param tab The TabNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenTab(
        tab: TabNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val activeStackIndex = tab.activeStackIndex
        val activeStack = tab.activeStack

        // Detect previous tab index by comparing with previous root state
        val previousTabIndex = detectPreviousTabIndex(tab, context.previousRoot)

        // Generate wrapper surface ID
        val wrapperSurfaceId = "${tab.key}-wrapper"

        // Generate content surface ID (includes tab index for uniqueness)
        val contentSurfaceId = "${tab.key}-content-$activeStackIndex"
        val previousContentSurfaceId = if (previousTabIndex != null && previousTabIndex != activeStackIndex) {
            "${tab.key}-content-$previousTabIndex"
        } else {
            null
        }

        // Determine if this is a cross-node type navigation
        // Cross-node means we're navigating from a different node type (e.g., StackNode → TabNode)
        val isCrossNodeNavigation = detectCrossNodeNavigation(tab, context)

        // 1. Create wrapper surface
        val wrapperAnimationSpec = if (isCrossNodeNavigation) {
            animationResolver.resolve(null, tab, context.transitionType)
        } else {
            SurfaceAnimationSpec.None // No animation for wrapper during tab switches
        }

        val wrapperSurface = RenderableSurface(
            id = wrapperSurfaceId,
            zOrder = context.baseZOrder,
            nodeType = SurfaceNodeType.TAB,
            renderingMode = SurfaceRenderingMode.TAB_WRAPPER,
            transitionState = SurfaceTransitionState.Visible,
            animationSpec = wrapperAnimationSpec,
            content = contentResolver.resolve(tab),
            parentWrapperId = context.parentId,
            previousSurfaceId = if (isCrossNodeNavigation) context.previousSiblingId else null
        )

        accumulator.addSurface(wrapperSurface)

        // 2. Create content surface for active tab
        val contentTransitionType = if (previousContentSurfaceId != null) {
            TransitionType.TAB_SWITCH
        } else {
            TransitionType.NONE
        }

        val contentAnimationSpec = if (previousContentSurfaceId != null) {
            animationResolver.resolve(
                from = null,
                to = activeStack,
                transitionType = TransitionType.TAB_SWITCH
            )
        } else {
            SurfaceAnimationSpec.None
        }

        val contentSurface = RenderableSurface(
            id = contentSurfaceId,
            zOrder = context.baseZOrder + zOrderIncrement,
            nodeType = SurfaceNodeType.TAB,
            renderingMode = SurfaceRenderingMode.TAB_CONTENT,
            transitionState = SurfaceTransitionState.Visible,
            animationSpec = contentAnimationSpec,
            content = contentResolver.resolve(activeStack),
            parentWrapperId = wrapperSurfaceId,
            previousSurfaceId = previousContentSurfaceId
        )

        accumulator.addSurface(contentSurface)

        // 3. Add animation pair for tab switch
        if (previousContentSurfaceId != null) {
            accumulator.addAnimationPair(
                AnimationPair(
                    currentId = contentSurfaceId,
                    previousId = previousContentSurfaceId,
                    transitionType = TransitionType.TAB_SWITCH
                )
            )
        }

        // 4. Add animation pair for cross-node navigation
        if (isCrossNodeNavigation && context.previousSiblingId != null) {
            accumulator.addAnimationPair(
                AnimationPair(
                    currentId = wrapperSurfaceId,
                    previousId = context.previousSiblingId,
                    transitionType = context.transitionType
                )
            )
        }

        // 5. Update caching hints tracking
        accumulator.wrapperIds.add(wrapperSurfaceId)
        accumulator.contentIds.add(contentSurfaceId)
        if (isCrossNodeNavigation) {
            accumulator.isCrossNodeNavigation = true
        }

        // 6. Set cacheable IDs based on navigation type
        if (isCrossNodeNavigation) {
            // Cross-node: cache whole wrapper + content
            accumulator.markCacheable(wrapperSurfaceId)
            accumulator.markCacheable(contentSurfaceId)
        } else {
            // Intra-tab: only cache content
            accumulator.markCacheable(contentSurfaceId)
            if (previousContentSurfaceId != null) {
                accumulator.markInvalidated(previousContentSurfaceId)
            }
        }

        // 7. Recursively flatten active stack's content
        val contentContext = FlattenContext(
            baseZOrder = context.baseZOrder + (2 * zOrderIncrement),
            parentId = contentSurfaceId,
            previousSiblingId = null, // Reset for nested content
            transitionType = TransitionType.NONE,
            previousRoot = context.previousRoot
        )

        // Flatten the active stack's children (screens within the tab)
        flattenStackContent(activeStack, contentContext, accumulator)
    }

    /**
     * Detects the previous tab index by comparing with the previous root state.
     *
     * If the previous root was a TabNode with the same key, returns its activeStackIndex.
     * Otherwise, returns null (indicating this is either the initial render or
     * a cross-node navigation).
     *
     * @param currentTab The current TabNode
     * @param previousRoot The previous navigation tree root
     * @return The previous tab index, or null if not a tab switch
     */
    private fun detectPreviousTabIndex(
        currentTab: TabNode,
        previousRoot: NavNode?
    ): Int? {
        if (previousRoot == null) return null

        // Find a TabNode with the same key in the previous tree
        val previousTab = findTabNodeByKey(previousRoot, currentTab.key)
        return previousTab?.activeStackIndex
    }

    /**
     * Finds a TabNode with the given key in the navigation tree.
     *
     * Performs a depth-first search to locate the TabNode.
     *
     * @param node The starting node for the search
     * @param key The key to search for
     * @return The matching TabNode, or null if not found
     */
    private fun findTabNodeByKey(node: NavNode, key: String): TabNode? {
        return when (node) {
            is TabNode -> if (node.key == key) node else {
                node.stacks.asSequence()
                    .flatMap { stack -> stack.children.asSequence() }
                    .mapNotNull { child -> findTabNodeByKey(child, key) }
                    .firstOrNull()
            }
            is StackNode -> node.children.asSequence()
                .mapNotNull { child -> findTabNodeByKey(child, key) }
                .firstOrNull()
            is PaneNode -> node.paneConfigurations.values.asSequence()
                .mapNotNull { config -> config.content?.let { findTabNodeByKey(it, key) } }
                .firstOrNull()
            is ScreenNode -> null
        }
    }

    /**
     * Detects if this is a cross-node type navigation.
     *
     * Cross-node navigation occurs when:
     * - There was a previous root that is not the same type as the current node
     * - Or the previous root doesn't contain a matching TabNode
     *
     * @param tab The current TabNode
     * @param context The flattening context
     * @return True if this is cross-node navigation
     */
    private fun detectCrossNodeNavigation(
        tab: TabNode,
        context: FlattenContext
    ): Boolean {
        val previousRoot = context.previousRoot ?: return false

        // If the previous root was a different node type at the same level
        if (context.previousSiblingId != null && !context.previousSiblingId.startsWith(tab.key)) {
            return previousRoot !is TabNode || previousRoot.key != tab.key
        }

        // Check if this TabNode existed in the previous state
        val previousTab = findTabNodeByKey(previousRoot, tab.key)
        return previousTab == null
    }

    /**
     * Flattens the content within a stack (for nested tab content).
     *
     * This handles the children of a StackNode within a TabNode,
     * creating appropriate surfaces for each active screen.
     *
     * @param stack The StackNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenStackContent(
        stack: StackNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val children = stack.children
        if (children.isEmpty()) return

        val activeChild = children.last()
        val previousChild = if (children.size > 1) children[children.size - 2] else null

        when (activeChild) {
            is ScreenNode -> {
                val surface = RenderableSurface(
                    id = activeChild.key,
                    zOrder = context.baseZOrder,
                    nodeType = SurfaceNodeType.SCREEN,
                    renderingMode = SurfaceRenderingMode.STACK_CONTENT,
                    transitionState = SurfaceTransitionState.Visible,
                    animationSpec = SurfaceAnimationSpec.None,
                    content = contentResolver.resolve(activeChild),
                    parentWrapperId = context.parentId,
                    previousSurfaceId = previousChild?.key
                )
                accumulator.addSurface(surface)

                // Add animation pair if there's navigation within the stack
                if (previousChild != null) {
                    accumulator.addAnimationPair(
                        AnimationPair(
                            currentId = activeChild.key,
                            previousId = previousChild.key,
                            transitionType = TransitionType.PUSH
                        )
                    )
                }
            }
            else -> flatten(activeChild, context, accumulator)
        }
    }

    // =========================================================================
    // PANE NODE FLATTENING
    // =========================================================================

    /**
     * Flattens a PaneNode with adaptive rendering based on screen size.
     *
     * ## Compact Width (Small Screens)
     *
     * Renders like StackNode - only active pane visible with back navigation support.
     * Uses [SurfaceRenderingMode.PANE_AS_STACK] for stack-like behavior.
     *
     * ## Medium/Expanded Width (Large Screens)
     *
     * Renders all panes with user wrapper composable controlling layout.
     * Uses [SurfaceRenderingMode.PANE_WRAPPER] for the wrapper surface and
     * [SurfaceRenderingMode.PANE_CONTENT] for individual pane content surfaces.
     *
     * @param pane The PaneNode to flatten
     * @param context Current flattening context (contains windowSizeClass)
     * @param accumulator Accumulator for results
     */
    private fun flattenPane(
        pane: PaneNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        if (context.windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact) {
            flattenPaneAsStack(pane, context, accumulator)
        } else {
            flattenPaneMultiPane(pane, context, accumulator)
        }
    }

    /**
     * Flattens PaneNode for small screens (Compact width).
     *
     * Behaves like StackNode:
     * - Only one pane visible at a time
     * - Tracks previous pane for back navigation animations
     * - Uses [SurfaceRenderingMode.PANE_AS_STACK] rendering mode
     *
     * @param pane The PaneNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenPaneAsStack(
        pane: PaneNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val activePaneRole = pane.activePaneRole
        val activePaneConfig = pane.paneConfigurations[activePaneRole] ?: return
        val activePaneContent = activePaneConfig.content

        // Get the ordered list of pane roles for consistent indexing
        val orderedRoles = pane.configuredRoles.toList()
        val activePaneIndex = orderedRoles.indexOf(activePaneRole)

        // Generate surface ID including pane role for uniqueness
        val surfaceId = "${pane.key}-pane-${activePaneRole.name.lowercase()}"

        // Detect previous pane for animation pairing
        val previousPaneRole = detectPreviousPaneRole(pane, context.previousRoot)
        val previousPaneId = if (previousPaneRole != null && previousPaneRole != activePaneRole) {
            "${pane.key}-pane-${previousPaneRole.name.lowercase()}"
        } else {
            null
        }

        // Determine if this is cross-node navigation
        val isCrossNodeNavigation = context.previousSiblingId != null &&
            !context.previousSiblingId.startsWith(pane.key)

        // Resolve animation spec
        val animationSpec = when {
            previousPaneId != null -> {
                // Pane switch within the PaneNode
                animationResolver.resolve(
                    from = pane.paneConfigurations[previousPaneRole]?.content,
                    to = activePaneContent,
                    transitionType = TransitionType.PANE_SWITCH
                )
            }
            isCrossNodeNavigation -> {
                // Cross-node navigation (entering from different node type)
                animationResolver.resolve(null, activePaneContent, context.transitionType)
            }
            else -> SurfaceAnimationSpec.None
        }

        val surface = RenderableSurface(
            id = surfaceId,
            zOrder = context.baseZOrder,
            nodeType = SurfaceNodeType.PANE,
            renderingMode = SurfaceRenderingMode.PANE_AS_STACK,
            transitionState = SurfaceTransitionState.Visible,
            animationSpec = animationSpec,
            content = contentResolver.resolve(activePaneContent),
            parentWrapperId = context.parentId,
            previousSurfaceId = previousPaneId ?: context.previousSiblingId
        )

        accumulator.addSurface(surface)

        // Add animation pair for pane navigation
        if (previousPaneId != null) {
            accumulator.addAnimationPair(
                AnimationPair(
                    currentId = surfaceId,
                    previousId = previousPaneId,
                    transitionType = TransitionType.PANE_SWITCH
                )
            )
        }

        // Add animation pair for cross-node navigation
        if (isCrossNodeNavigation && context.previousSiblingId != null) {
            accumulator.addAnimationPair(
                AnimationPair(
                    currentId = surfaceId,
                    previousId = context.previousSiblingId,
                    transitionType = context.transitionType
                )
            )
            accumulator.isCrossNodeNavigation = true
        }

        // Update caching hints for stack-like behavior
        accumulator.markCacheable(surfaceId)
        if (previousPaneId != null) {
            accumulator.markInvalidated(previousPaneId)
        }

        // Recursively flatten the active pane's content if it's a container
        flattenPaneContent(activePaneContent, surfaceId, context, accumulator)
    }

    /**
     * Flattens PaneNode for large screens (Medium/Expanded width).
     *
     * All panes visible with user-controlled wrapper layout:
     * - [SurfaceRenderingMode.PANE_WRAPPER] surface contains user's layout composable
     * - [SurfaceRenderingMode.PANE_CONTENT] surfaces for each pane
     * - [PaneStructure] provides pane metadata to user wrapper
     *
     * @param pane The PaneNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenPaneMultiPane(
        pane: PaneNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        val wrapperSurfaceId = "${pane.key}-wrapper"

        // Determine if this is cross-node navigation
        val isCrossNodeNavigation = detectCrossNodePaneNavigation(pane, context)

        // Build pane structures for user wrapper
        val paneStructures = pane.paneConfigurations.map { (role, config) ->
            PaneStructure(
                paneRole = role,
                content = contentResolver.resolve(config.content)
            )
        }

        // Resolve wrapper animation
        val wrapperAnimationSpec = if (isCrossNodeNavigation) {
            animationResolver.resolve(null, pane, context.transitionType)
        } else {
            SurfaceAnimationSpec.None
        }

        // 1. Create wrapper surface
        val wrapperSurface = RenderableSurface(
            id = wrapperSurfaceId,
            zOrder = context.baseZOrder,
            nodeType = SurfaceNodeType.PANE,
            renderingMode = SurfaceRenderingMode.PANE_WRAPPER,
            transitionState = SurfaceTransitionState.Visible,
            animationSpec = wrapperAnimationSpec,
            content = contentResolver.resolve(pane),
            parentWrapperId = context.parentId,
            previousSurfaceId = if (isCrossNodeNavigation) context.previousSiblingId else null,
            paneStructures = paneStructures
        )

        accumulator.addSurface(wrapperSurface)

        // 2. Create content surfaces for each pane
        var contentZOrderOffset = zOrderIncrement
        pane.paneConfigurations.forEach { (role, config) ->
            val contentSurfaceId = "${pane.key}-content-${role.name.lowercase()}"

            val contentSurface = RenderableSurface(
                id = contentSurfaceId,
                zOrder = context.baseZOrder + contentZOrderOffset,
                nodeType = SurfaceNodeType.PANE,
                renderingMode = SurfaceRenderingMode.PANE_CONTENT,
                transitionState = SurfaceTransitionState.Visible,
                animationSpec = SurfaceAnimationSpec.None, // Content doesn't animate independently
                content = contentResolver.resolve(config.content),
                parentWrapperId = wrapperSurfaceId,
                previousSurfaceId = null
            )

            accumulator.addSurface(contentSurface)
            accumulator.contentIds.add(contentSurfaceId)
            contentZOrderOffset += zOrderIncrement
        }

        // 3. Add animation pair for cross-node navigation
        if (isCrossNodeNavigation && context.previousSiblingId != null) {
            accumulator.addAnimationPair(
                AnimationPair(
                    currentId = wrapperSurfaceId,
                    previousId = context.previousSiblingId,
                    transitionType = context.transitionType
                )
            )
        }

        // 4. Update caching hints
        accumulator.wrapperIds.add(wrapperSurfaceId)
        if (isCrossNodeNavigation) {
            accumulator.isCrossNodeNavigation = true
            // Cross-node: cache whole wrapper + content
            accumulator.markCacheable(wrapperSurfaceId)
            pane.paneConfigurations.keys.forEach { role ->
                accumulator.markCacheable("${pane.key}-content-${role.name.lowercase()}")
            }
        } else {
            // Intra-pane: only cache content surfaces
            pane.paneConfigurations.keys.forEach { role ->
                accumulator.markCacheable("${pane.key}-content-${role.name.lowercase()}")
            }
        }

        // 5. Recursively flatten content within each pane
        pane.paneConfigurations.forEach { (role, config) ->
            val contentSurfaceId = "${pane.key}-content-${role.name.lowercase()}"
            flattenPaneContent(config.content, contentSurfaceId, context, accumulator)
        }
    }

    /**
     * Recursively flattens content within a pane.
     *
     * Handles nested containers (StackNode, TabNode, etc.) within pane content.
     *
     * @param content The content node to flatten
     * @param parentSurfaceId The parent surface ID for linking
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenPaneContent(
        content: NavNode,
        parentSurfaceId: String,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        when (content) {
            is ScreenNode -> {
                // Screen nodes within panes are already rendered as part of the pane content
                // No additional surfaces needed
            }
            is StackNode -> {
                // Flatten the stack's active child
                val contentContext = context.copy(
                    baseZOrder = context.baseZOrder + (2 * zOrderIncrement),
                    parentId = parentSurfaceId,
                    previousSiblingId = null,
                    transitionType = TransitionType.NONE
                )
                flattenStackContent(content, contentContext, accumulator)
            }
            is TabNode -> {
                // Handle nested tab within pane
                val contentContext = context.copy(
                    baseZOrder = context.baseZOrder + (2 * zOrderIncrement),
                    parentId = parentSurfaceId,
                    previousSiblingId = null,
                    transitionType = TransitionType.NONE
                )
                flattenTab(content, contentContext, accumulator)
            }
            is PaneNode -> {
                // Nested panes - flatten recursively with same window size
                val contentContext = context.copy(
                    baseZOrder = context.baseZOrder + (2 * zOrderIncrement),
                    parentId = parentSurfaceId,
                    previousSiblingId = null,
                    transitionType = TransitionType.NONE
                )
                flattenPane(content, contentContext, accumulator)
            }
        }
    }

    /**
     * Detects the previous pane role by comparing with the previous root state.
     *
     * If the previous root was a PaneNode with the same key, returns its activePaneRole.
     * Otherwise, returns null (indicating this is either the initial render or
     * a cross-node navigation).
     *
     * @param currentPane The current PaneNode
     * @param previousRoot The previous navigation tree root
     * @return The previous pane role, or null if not a pane switch
     */
    private fun detectPreviousPaneRole(
        currentPane: PaneNode,
        previousRoot: NavNode?
    ): PaneRole? {
        if (previousRoot == null) return null

        // Find a PaneNode with the same key in the previous tree
        val previousPane = findPaneNodeByKey(previousRoot, currentPane.key)
        return previousPane?.activePaneRole
    }

    /**
     * Finds a PaneNode with the given key in the navigation tree.
     *
     * Performs a depth-first search to locate the PaneNode.
     *
     * @param node The starting node for the search
     * @param key The key to search for
     * @return The matching PaneNode, or null if not found
     */
    private fun findPaneNodeByKey(node: NavNode, key: String): PaneNode? {
        return when (node) {
            is PaneNode -> if (node.key == key) node else {
                node.paneConfigurations.values.asSequence()
                    .mapNotNull { config -> findPaneNodeByKey(config.content, key) }
                    .firstOrNull()
            }
            is StackNode -> node.children.asSequence()
                .mapNotNull { child -> findPaneNodeByKey(child, key) }
                .firstOrNull()
            is TabNode -> node.stacks.asSequence()
                .flatMap { stack -> stack.children.asSequence() }
                .mapNotNull { child -> findPaneNodeByKey(child, key) }
                .firstOrNull()
            is ScreenNode -> null
        }
    }

    /**
     * Detects if this is a cross-node type navigation for PaneNode.
     *
     * Cross-node navigation occurs when:
     * - There was a previous root that is not the same type as the current node
     * - Or the previous root doesn't contain a matching PaneNode
     *
     * @param pane The current PaneNode
     * @param context The flattening context
     * @return True if this is cross-node navigation
     */
    private fun detectCrossNodePaneNavigation(
        pane: PaneNode,
        context: FlattenContext
    ): Boolean {
        val previousRoot = context.previousRoot ?: return false

        // If the previous root was a different node type at the same level
        if (context.previousSiblingId != null && !context.previousSiblingId.startsWith(pane.key)) {
            return previousRoot !is PaneNode || previousRoot.key != pane.key
        }

        // Check if this PaneNode existed in the previous state
        val previousPane = findPaneNodeByKey(previousRoot, pane.key)
        return previousPane == null
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Detects the type of transition between previous and current state.
     *
     * Uses a heuristic based on comparing the active path lengths:
     * - Longer current path = PUSH (forward navigation)
     * - Shorter current path = POP (backward navigation)
     * - Same length = NONE (no animation needed)
     *
     * @param previousRoot The previous navigation tree root
     * @param currentRoot The current navigation tree root
     * @return The detected [TransitionType]
     */
    private fun detectTransitionType(
        previousRoot: NavNode?,
        currentRoot: NavNode
    ): TransitionType {
        if (previousRoot == null) return TransitionType.NONE

        // Compare active paths to detect transition direction
        val previousPath = getActivePath(previousRoot)
        val currentPath = getActivePath(currentRoot)

        return when {
            currentPath.size > previousPath.size -> TransitionType.PUSH
            currentPath.size < previousPath.size -> TransitionType.POP
            else -> TransitionType.NONE
        }
    }

    /**
     * Finds the previous sibling ID for animation pairing.
     *
     * Compares the active leaves of previous and current trees to determine
     * if a transition occurred that requires animation pairing.
     *
     * @param previousRoot The previous navigation tree root
     * @param currentRoot The current navigation tree root
     * @return The key of the previous leaf, or null if no transition
     */
    private fun findPreviousSiblingId(
        previousRoot: NavNode?,
        currentRoot: NavNode
    ): String? {
        if (previousRoot == null) return null

        val previousLeaf = getActiveLeaf(previousRoot)
        val currentLeaf = getActiveLeaf(currentRoot)

        return if (previousLeaf?.key != currentLeaf?.key) {
            previousLeaf?.key
        } else {
            null
        }
    }

    /**
     * Gets the active path from root to leaf.
     *
     * Traverses the navigation tree following the "active" path at each level:
     * - StackNode: follows activeChild
     * - TabNode: follows activeStack
     * - PaneNode: follows activePaneContent
     * - ScreenNode: terminates (leaf)
     *
     * @param node The starting node
     * @return List of nodes from root to active leaf
     */
    private fun getActivePath(node: NavNode): List<NavNode> {
        val path = mutableListOf<NavNode>()
        var current: NavNode? = node

        while (current != null) {
            path.add(current)
            current = when (current) {
                is ScreenNode -> null
                is StackNode -> current.activeChild
                is TabNode -> current.activeStack
                is PaneNode -> current.activePaneContent
            }
        }

        return path
    }

    /**
     * Gets the active leaf node (deepest active ScreenNode).
     *
     * @param node The starting node
     * @return The active ScreenNode, or null if tree is empty
     */
    private fun getActiveLeaf(node: NavNode): ScreenNode? {
        return when (node) {
            is ScreenNode -> node
            is StackNode -> node.activeChild?.let { getActiveLeaf(it) }
            is TabNode -> getActiveLeaf(node.activeStack)
            is PaneNode -> node.activePaneContent?.let { getActiveLeaf(it) }
        }
    }

    public companion object {
        /**
         * Default z-order increment between depth levels.
         *
         * A value of 100 leaves room for intermediate z-orders during
         * transitions (e.g., +50 for entering surfaces).
         */
        public const val DEFAULT_Z_ORDER_INCREMENT: Int = 100

        /**
         * Default animation resolver providing standard slide animations.
         *
         * ## Animations by Transition Type
         *
         * - **PUSH**: Slide in from right with fade, slide out partial left with fade
         * - **POP**: Slide in from left with fade, slide out right with fade
         * - **TAB_SWITCH**: Crossfade only
         * - **PANE_SWITCH**: No animation
         * - **NONE**: No animation
         */
        public val DefaultAnimationResolver: AnimationResolver = AnimationResolver { _, _, transitionType ->
            when (transitionType) {
                TransitionType.PUSH -> SurfaceAnimationSpec(
                    enter = slideInHorizontally { fullWidth -> fullWidth } + fadeIn(),
                    exit = slideOutHorizontally { fullWidth -> -fullWidth / 3 } + fadeOut()
                )
                TransitionType.POP -> SurfaceAnimationSpec(
                    enter = slideInHorizontally { fullWidth -> -fullWidth / 3 } + fadeIn(),
                    exit = slideOutHorizontally { fullWidth -> fullWidth } + fadeOut()
                )
                TransitionType.TAB_SWITCH -> SurfaceAnimationSpec(
                    enter = fadeIn(),
                    exit = fadeOut()
                )
                TransitionType.PANE_SWITCH -> SurfaceAnimationSpec.None
                TransitionType.NONE -> SurfaceAnimationSpec.None
            }
        }
    }
}
