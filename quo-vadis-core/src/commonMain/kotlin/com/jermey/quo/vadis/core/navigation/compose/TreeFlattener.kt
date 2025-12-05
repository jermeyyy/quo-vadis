package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
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
     */
    private data class FlattenContext(
        val baseZOrder: Int,
        val parentId: String?,
        val previousSiblingId: String?,
        val transitionType: TransitionType
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

        fun toResult(shouldCacheWrapper: Boolean = false): FlattenResult {
            return FlattenResult(
                surfaces = surfaces.sortedBy { it.zOrder },
                animationPairs = animationPairs.toList(),
                cachingHints = CachingHints(
                    shouldCacheWrapper = shouldCacheWrapper,
                    shouldCacheContent = true,
                    cacheableIds = cacheableIds.toSet(),
                    invalidatedIds = invalidatedIds.toSet()
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
     * - For [TabNode]/[PaneNode]: Placeholder behavior (full impl in RENDER-002B/C)
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
     * ```
     *
     * @param root The root of the navigation tree
     * @param previousRoot Optional previous root for transition detection
     * @return FlattenResult containing surfaces, animation pairs, and caching hints
     */
    public fun flattenState(
        root: NavNode,
        previousRoot: NavNode? = null
    ): FlattenResult {
        val accumulator = FlattenAccumulator()
        val transitionType = detectTransitionType(previousRoot, root)

        val context = FlattenContext(
            baseZOrder = 0,
            parentId = null,
            previousSiblingId = findPreviousSiblingId(previousRoot, root),
            transitionType = transitionType
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
     * Placeholder for TabNode flattening.
     *
     * This is a basic implementation that flattens only the active stack.
     * Full implementation with TAB_WRAPPER, TAB_CONTENT modes and proper
     * tab switching animations will be added in RENDER-002B.
     *
     * ## Current Behavior
     *
     * - Flattens only the active stack's content
     * - Does not create TAB_WRAPPER surface
     * - Does not handle tab switching transitions
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
        // Placeholder - will be fully implemented in RENDER-002B
        // For now, just flatten the active stack
        val activeStack = tab.activeStack
        val childContext = context.copy(
            baseZOrder = context.baseZOrder + zOrderIncrement,
            parentId = tab.key
        )
        flatten(activeStack, childContext, accumulator)
    }

    /**
     * Placeholder for PaneNode flattening.
     *
     * This is a basic implementation that flattens only the active pane's content.
     * Full implementation with PANE_WRAPPER, PANE_CONTENT, PANE_AS_STACK modes
     * and proper adaptive behavior will be added in RENDER-002C.
     *
     * ## Current Behavior
     *
     * - Flattens only the active pane's content
     * - Does not create PANE_WRAPPER surface
     * - Does not handle multi-pane layouts
     * - Does not handle adaptive morphing
     *
     * @param pane The PaneNode to flatten
     * @param context Current flattening context
     * @param accumulator Accumulator for results
     */
    private fun flattenPane(
        pane: PaneNode,
        context: FlattenContext,
        accumulator: FlattenAccumulator
    ) {
        // Placeholder - will be fully implemented in RENDER-002C
        // For now, flatten the active pane's content
        val activePaneContent = pane.activePaneContent
        if (activePaneContent != null) {
            val paneContext = context.copy(
                baseZOrder = context.baseZOrder + zOrderIncrement,
                parentId = pane.key
            )
            flatten(activePaneContent, paneContext, accumulator)
        }
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
