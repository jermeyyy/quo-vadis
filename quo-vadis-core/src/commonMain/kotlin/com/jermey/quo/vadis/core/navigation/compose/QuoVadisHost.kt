@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.jermey.quo.vadis.core.navigation.compose.wrapper.DefaultPaneWrapper
import com.jermey.quo.vadis.core.navigation.compose.wrapper.DefaultTabWrapper
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneContent
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapper
import com.jermey.quo.vadis.core.navigation.compose.wrapper.PaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapper
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.internal.createPaneWrapperScope
import com.jermey.quo.vadis.core.navigation.compose.wrapper.internal.createTabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.TabNode
import com.jermey.quo.vadis.core.navigation.core.TransitionStateManager
import com.jermey.quo.vadis.core.navigation.core.TreeMutator
import com.jermey.quo.vadis.core.navigation.core.activeLeaf
import com.jermey.quo.vadis.core.navigation.core.route
import kotlin.reflect.KClass

// =============================================================================
// QuoVadisHostScope Interface
// =============================================================================

/**
 * Scope provided to content lambdas within [QuoVadisHost].
 *
 * This scope provides access to:
 * - [SharedTransitionScope] for shared element transitions
 * - [Navigator] for programmatic navigation
 *
 * ## Usage with Shared Elements
 *
 * The scope extends [SharedTransitionScope], making shared element APIs
 * available directly in the content lambda:
 *
 * ```kotlin
 * QuoVadisHost(navigator = navigator) { destination ->
 *     // sharedElement, sharedBounds, etc. available here
 *     ProfileScreen(
 *         modifier = Modifier.sharedElement(
 *             state = rememberSharedContentState(key = "profile-${destination.id}"),
 *             animatedVisibilityScope = this@AnimatedVisibility
 *         )
 *     )
 * }
 * ```
 *
 * @see QuoVadisHost
 * @see SharedTransitionScope
 */
@Stable
public interface QuoVadisHostScope : SharedTransitionScope {

    /**
     * The navigator instance for programmatic navigation.
     *
     * Use this to navigate from within screen content or to access
     * navigation state.
     */
    public val navigator: Navigator
}

/**
 * Internal implementation of [QuoVadisHostScope].
 */
@Stable
private class QuoVadisHostScopeImpl(
    private val sharedTransitionScope: SharedTransitionScope,
    override val navigator: Navigator
) : QuoVadisHostScope, SharedTransitionScope by sharedTransitionScope

// =============================================================================
// Rendering Mode
// =============================================================================

/**
 * Specifies the rendering approach used by [QuoVadisHost].
 *
 * The rendering mode determines how the navigation tree is processed and displayed.
 * Two approaches are available, each with different trade-offs:
 *
 * ## Mode Comparison
 *
 * | Feature | Flattened | Hierarchical |
 * |---------|-----------|--------------|
 * | Tab/pane wrapper composition | Siblings (z-ordered) | True parent-child |
 * | Animation coordination | Per-surface | Per-container |
 * | Predictive back | Per-screen | Entire subtree |
 * | Complexity | Higher (flattening logic) | Lower (recursive) |
 * | Maturity | Stable | Experimental |
 *
 * ## Migration
 *
 * The hierarchical mode is the recommended approach for new projects, as it provides
 * better animation coordination and simpler mental model. Existing projects can
 * migrate by:
 *
 * 1. Adding `@TabWrapper` and `@PaneWrapper` annotations to wrapper composables
 * 2. Switching to `RenderingMode.Hierarchical`
 * 3. Verifying navigation behavior
 *
 * @see QuoVadisHost
 * @see HierarchicalQuoVadisHost
 */
public enum class RenderingMode {
    /**
     * Flattening-based rendering approach.
     *
     * The navigation tree is flattened into a list of renderable surfaces,
     * which are then rendered with z-ordering. Tab and pane wrappers are
     * rendered as sibling composables with their content.
     *
     * This is the original and stable rendering approach.
     *
     * ## Characteristics
     * - Tab/pane wrappers rendered as siblings of their content
     * - Animations coordinated via surface-level transitions
     * - Predictive back applies to individual screens
     * - Runtime wrapper functions via `tabWrapper`/`paneWrapper` parameters
     *
     * @deprecated Use [Hierarchical] mode instead. The flattened rendering approach
     *   will be removed in a future version. Migrate by:
     *   1. Adding `@TabWrapper`/`@PaneWrapper` annotations to wrapper composables
     *   2. Using `@Screen` annotations for destination content
     *   3. Switching to `RenderingMode.Hierarchical`
     *   See migration guide: `quo-vadis-core/docs/MIGRATION_HIERARCHICAL_RENDERING.md`
     */
    @Deprecated(
        message = "Use RenderingMode.Hierarchical instead. The flattened rendering approach " +
            "will be removed in a future version. Migrate by using @TabWrapper/@PaneWrapper " +
            "annotations for wrappers and @Screen annotations for destination content.",
        replaceWith = ReplaceWith("Hierarchical")
    )
    Flattened,

    /**
     * Hierarchical tree-based rendering approach.
     *
     * The navigation tree is rendered recursively, preserving parent-child
     * relationships. Tab and pane wrappers truly contain their content
     * as Compose children.
     *
     * This is the newer, recommended approach for new projects.
     *
     * ## Characteristics
     * - Tab/pane wrappers contain content as true children
     * - Animations coordinated per-container (tabs animate tab content together)
     * - Predictive back transforms entire subtrees
     * - Annotation-based wrappers via `@TabWrapper`/`@PaneWrapper`
     *
     * @see HierarchicalQuoVadisHost
     */
    Hierarchical
}

// =============================================================================
// Main QuoVadisHost Composable
// =============================================================================

/**
 * The unified navigation host that renders any NavNode tree structure.
 *
 * QuoVadisHost is the **single rendering component** that replaces all previous
 * navigation hosts (NavHost, GraphNavHost, TabbedNavHost). It:
 *
 * 1. Observes the Navigator's state flow
 * 2. Flattens the NavNode tree into renderable surfaces
 * 3. Renders each surface with appropriate z-ordering
 * 4. Coordinates enter/exit animations
 * 5. Provides SharedTransitionScope for shared element transitions
 * 6. Preserves tab state via SaveableStateHolder
 *
 * ## Basic Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberNavigator(initialGraph)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         modifier = Modifier.fillMaxSize()
 *     ) { destination ->
 *         when (destination) {
 *             is HomeDestination -> HomeScreen()
 *             is ProfileDestination -> ProfileScreen(destination.userId)
 *         }
 *     }
 * }
 * ```
 *
 * ## With Custom Tab Wrapper
 *
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     tabWrapper = { tabContent ->
 *         Scaffold(
 *             bottomBar = {
 *                 NavigationBar {
 *                     tabMetadata.forEachIndexed { index, meta ->
 *                         NavigationBarItem(
 *                             selected = activeTabIndex == index,
 *                             onClick = { switchTab(index) },
 *                             icon = { Icon(meta.icon, meta.label) },
 *                             label = { Text(meta.label) }
 *                         )
 *                     }
 *                 }
 *             }
 *         ) { padding ->
 *             Box(modifier = Modifier.padding(padding)) {
 *                 tabContent()
 *             }
 *         }
 *     }
 * ) { destination ->
 *     // Screen content
 * }
 * ```
 *
 * ## With Custom Pane Wrapper
 *
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     paneWrapper = { paneContents ->
 *         Row(modifier = Modifier.fillMaxSize()) {
 *             paneContents.filter { it.isVisible }.forEach { pane ->
 *                 val weight = when (pane.role) {
 *                     PaneRole.Primary -> 0.65f
 *                     PaneRole.Supporting -> 0.35f
 *                     else -> 1f
 *                 }
 *                 Box(modifier = Modifier.weight(weight)) {
 *                     pane.content()
 *                 }
 *             }
 *         }
 *     }
 * ) { destination ->
 *     // Screen content
 * }
 * ```
 *
 * @param navigator The Navigator instance managing navigation state
 * @param modifier Modifier for the root container
 * @param renderingMode The rendering approach to use. [RenderingMode.Flattened] (default) uses the
 *   stable flattening approach, while [RenderingMode.Hierarchical] uses the newer tree-based
 *   rendering with better animation coordination. See [RenderingMode] for details.
 * @param enablePredictiveBack Whether to enable predictive back gesture handling.
 *   When enabled, users can preview the back navigation result while performing
 *   a back gesture (swipe on Android/iOS, system back on supported platforms).
 *   Defaults to `true`. Set to `false` to disable gesture-based back previews.
 * @param animationRegistry Registry for transition animations. Defaults to [AnimationRegistry.Default]
 *   which provides standard slide animations. Use [AnimationRegistry.None] for no animations.
 *   Only used when [renderingMode] is [RenderingMode.Flattened].
 * @param tabWrapper User-provided wrapper for TabNode rendering (default: bottom navigation).
 *   Only used when [renderingMode] is [RenderingMode.Flattened]. For hierarchical mode,
 *   use `@TabWrapper` annotation instead.
 * @param paneWrapper User-provided wrapper for PaneNode rendering (default: equal width row).
 *   Only used when [renderingMode] is [RenderingMode.Flattened]. For hierarchical mode,
 *   use `@PaneWrapper` annotation instead.
 * @param content Content resolver that maps [Destination] to composable content.
 *   Only used when [renderingMode] is [RenderingMode.Flattened]. For hierarchical mode,
 *   use `@Screen` annotations and provide a [ScreenRegistry].
 *
 * @see QuoVadisHostScope
 * @see AnimationRegistry
 * @see TabWrapper
 * @see PaneWrapper
 * @see PredictiveBackHandler
 * @see RenderingMode
 * @see HierarchicalQuoVadisHost
 */
@Composable
public fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    renderingMode: RenderingMode = RenderingMode.Hierarchical,
    enablePredictiveBack: Boolean = true,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default,
    tabWrapper: TabWrapper = DefaultTabWrapper,
    paneWrapper: PaneWrapper = DefaultPaneWrapper,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    // Dispatch based on rendering mode
    when (renderingMode) {
        RenderingMode.Hierarchical -> {
            // Delegate to hierarchical rendering
            // Note: In hierarchical mode, tabWrapper/paneWrapper/content are ignored.
            // Use @TabWrapper/@PaneWrapper/@Screen annotations instead.
            HierarchicalQuoVadisHost(
                navigator = navigator,
                modifier = modifier,
                enablePredictiveBack = enablePredictiveBack,
            )
            return
        }
        RenderingMode.Flattened -> {
            // Continue with flattened rendering below
        }
    }

    // Collect navigation state
    val navState by navigator.state.collectAsState()

    // Calculate window size class for adaptive pane rendering
    val windowSizeClass = calculateWindowSizeClass()

    // State holder for preserving navigation states (tabs, screens)
    val stateHolder = rememberNavigationStateHolder()

    // Track active keys for state cleanup
    val activeKeys = remember(navState) { collectAllKeys(navState) }
    var previousKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Cleanup state for removed screens
    LaunchedEffect(activeKeys) {
        stateHolder.cleanup(activeKeys, previousKeys)
        previousKeys = activeKeys
    }

    // Preserve tab states for all TabNodes in the tree
    val tabNodes = remember(navState) { findAllTabNodes(navState) }
    tabNodes.forEach { tabNode ->
        stateHolder.PreserveTabStates(tabNode) {}
    }

    // Create content resolver for TreeFlattener
    val contentResolver = remember(content, navigator, tabWrapper, paneWrapper) {
        TreeFlattener.ContentResolver { node ->
            @Composable {
                when (node) {
                    is ScreenNode -> {
                        // Screen content will be rendered with full scope
                    }

                    is TabNode -> {
                        // Tab wrapper handled separately in RenderSurface
                    }

                    is PaneNode -> {
                        // Pane wrapper handled separately in RenderSurface
                    }

                    else -> {
                        // Other container types - render children
                    }
                }
            }
        }
    }

    // Create tree flattener with animation registry
    val flattener = remember(contentResolver, animationRegistry) {
        TreeFlattener(
            contentResolver = contentResolver,
            animationResolver = animationRegistry.toAnimationResolver()
        )
    }

    // Track previous state for transition detection
    val previousNavState = remember { androidx.compose.runtime.mutableStateOf<NavNode?>(null) }

    // Flatten the navigation tree with window size awareness
    val flattenResult = remember(navState, windowSizeClass) {
        val result = flattener.flattenState(
            root = navState,
            previousRoot = previousNavState.value,
            windowSizeClass = windowSizeClass
        )
        previousNavState.value = navState
        result
    }

    // Transition state manager for coordinating predictive back animations
    val transitionManager = remember(navigator) {
        TransitionStateManager(navState)
    }

    // Predictive back coordinator
    val backCoordinator = remember(navigator, transitionManager) {
        PredictiveBackCoordinator(navigator, transitionManager)
    }

    // Check if we can navigate back
    val canGoBack by remember(navState) {
        derivedStateOf { TreeMutator.pop(navState) != null }
    }

    // Root container with SharedTransitionLayout, wrapped with PredictiveBackHandler
    PredictiveBackHandler(
        enabled = enablePredictiveBack && canGoBack,
        callback = backCoordinator
    ) {
        SharedTransitionLayout(modifier = modifier) {
            val scope = remember(this, navigator) {
                QuoVadisHostScopeImpl(
                    sharedTransitionScope = this,
                    navigator = navigator
                )
            }

            QuoVadisHostContent(
                scope = scope,
                surfaces = flattenResult.surfaces,
                navState = navState,
                windowSizeClass = windowSizeClass,
                stateHolder = stateHolder,
                tabWrapper = tabWrapper,
                paneWrapper = paneWrapper,
                content = content
            )
        }
    }
}

/**
 * Internal composable that renders the flattened surfaces.
 */
@Composable
private fun QuoVadisHostContent(
    scope: QuoVadisHostScope,
    surfaces: List<RenderableSurface>,
    navState: NavNode,
    windowSizeClass: WindowSizeClass,
    stateHolder: NavigationStateHolder,
    tabWrapper: TabWrapper,
    paneWrapper: PaneWrapper,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Render each surface sorted by z-order
        surfaces
            .sortedBy { it.zOrder }
            .filter { it.shouldRender }
            .forEach { surface ->
                key(surface.id) {
                    RenderableSurfaceContainer(
                        surface = surface,
                        scope = scope,
                        navState = navState,
                        windowSizeClass = windowSizeClass,
                        stateHolder = stateHolder,
                        tabWrapper = tabWrapper,
                        paneWrapper = paneWrapper,
                        content = content
                    )
                }
            }
    }
}

/**
 * Container for rendering a single surface with animations.
 */
@Composable
private fun RenderableSurfaceContainer(
    surface: RenderableSurface,
    scope: QuoVadisHostScope,
    navState: NavNode,
    windowSizeClass: WindowSizeClass,
    stateHolder: NavigationStateHolder,
    tabWrapper: TabWrapper,
    paneWrapper: PaneWrapper,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    // Determine visibility for animation
    val isVisible = surface.transitionState !is SurfaceTransitionState.Exiting ||
        (surface.transitionState as? SurfaceTransitionState.Exiting)?.progress?.let { it < 1f } ?: true

    // Apply z-index for proper layering
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(surface.zOrder.toFloat())
    ) {
        // Wrap in SaveableScreen for state preservation
        stateHolder.SaveableScreen(key = surface.id) {
            // Animate enter/exit
            AnimatedVisibility(
                visible = isVisible,
                enter = surface.animationSpec.enter,
                exit = surface.animationSpec.exit,
                modifier = Modifier.fillMaxSize()
            ) {
                // Apply predictive back transformations if applicable
                val contentModifier = if (surface.isPredictive) {
                    Modifier.predictiveBackTransform(
                        progress = surface.animationProgress ?: 0f,
                        isExiting = surface.transitionState is SurfaceTransitionState.Exiting
                    )
                } else {
                    Modifier
                }

                Box(modifier = contentModifier.fillMaxSize()) {
                    RenderSurfaceContent(
                        surface = surface,
                        scope = scope,
                        navState = navState,
                        windowSizeClass = windowSizeClass,
                        tabWrapper = tabWrapper,
                        paneWrapper = paneWrapper,
                        content = content
                    )
                }
            }
        }
    }
}

/**
 * Renders the actual content for a surface based on its rendering mode.
 */
@Composable
private fun RenderSurfaceContent(
    surface: RenderableSurface,
    scope: QuoVadisHostScope,
    navState: NavNode,
    windowSizeClass: WindowSizeClass,
    tabWrapper: TabWrapper,
    paneWrapper: PaneWrapper,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    when (surface.renderingMode) {
        SurfaceRenderingMode.SINGLE_SCREEN,
        SurfaceRenderingMode.STACK_CONTENT -> {
            // Find the ScreenNode and render its destination
            val screenNode = findScreenNodeByKey(navState, surface.id)
            screenNode?.let { node ->
                scope.content(node.destination)
            }
        }

        SurfaceRenderingMode.TAB_WRAPPER -> {
            // Find TabNode and render with user's tabWrapper
            val tabNode = findTabNodeByKey(navState, surface.id.removeSuffix("-wrapper"))
            tabNode?.let { node ->
                RenderTabWrapper(
                    tabNode = node,
                    scope = scope,
                    tabWrapper = tabWrapper,
                    content = content
                )
            }
        }

        SurfaceRenderingMode.TAB_CONTENT -> {
            // Tab content is rendered within the wrapper
            // This surface type may be handled by the wrapper itself
            surface.content()
        }

        SurfaceRenderingMode.PANE_WRAPPER -> {
            // Find PaneNode and render with user's paneWrapper
            val paneNode = findPaneNodeByKey(navState, surface.id.removeSuffix("-wrapper"))
            paneNode?.let { node ->
                RenderPaneWrapper(
                    paneNode = node,
                    scope = scope,
                    windowSizeClass = windowSizeClass,
                    paneWrapper = paneWrapper,
                    content = content
                )
            }
        }

        SurfaceRenderingMode.PANE_CONTENT -> {
            // Pane content is rendered within the wrapper
            surface.content()
        }

        SurfaceRenderingMode.PANE_AS_STACK -> {
            // PaneNode rendered as stack on small screens
            val paneKey = surface.id.substringBefore("-pane-")
            val paneNode = findPaneNodeByKey(navState, paneKey)
            paneNode?.let { node ->
                val activePaneContent = node.activePaneContent
                if (activePaneContent is ScreenNode) {
                    scope.content(activePaneContent.destination)
                } else {
                    // For nested containers, render the surface content
                    surface.content()
                }
            }
        }
    }
}

/**
 * Renders a TabNode with the user's tabWrapper.
 */
@Composable
private fun RenderTabWrapper(
    tabNode: TabNode,
    scope: QuoVadisHostScope,
    tabWrapper: TabWrapper,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    // Create tab wrapper scope
    val tabWrapperScope = createTabWrapperScope(
        navigator = scope.navigator,
        activeTabIndex = tabNode.activeStackIndex,
        tabMetadata = tabNode.stacks.mapIndexed { index, stack ->
            com.jermey.quo.vadis.core.navigation.compose.wrapper.TabMetadata(
                label = "Tab ${index + 1}",
                route = stack.key,
                icon = null,
                contentDescription = null,
                badge = null
            )
        },
        isTransitioning = false,
        onSwitchTab = { index -> scope.navigator.switchTab(index) }
    )

    // Render the wrapper with tab content
    tabWrapperScope.tabWrapper {
        // Render active tab's content
        val activeStack = tabNode.activeStack
        val activeScreen = activeStack.children.lastOrNull() as? ScreenNode
        activeScreen?.let { node ->
            scope.content(node.destination)
        }
    }
}

/**
 * Renders a PaneNode with the user's paneWrapper.
 */
@Composable
private fun RenderPaneWrapper(
    paneNode: PaneNode,
    scope: QuoVadisHostScope,
    windowSizeClass: WindowSizeClass,
    paneWrapper: PaneWrapper,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    // Create pane contents list
    val paneContents = paneNode.paneConfigurations.map { (role, config) ->
        PaneContent(
            role = role,
            content = {
                val paneContent = config.content
                if (paneContent is ScreenNode) {
                    scope.content(paneContent.destination)
                } else {
                    // For nested containers, render recursively
                    val activeScreen = paneContent.activeLeaf()
                    activeScreen?.let { node ->
                        scope.content(node.destination)
                    }
                }
            },
            isVisible = !windowSizeClass.isCompactWidth || role == paneNode.activePaneRole
        )
    }

    // Create pane wrapper scope
    val paneWrapperScope = createPaneWrapperScope(
        navigator = scope.navigator,
        activePaneRole = paneNode.activePaneRole,
        paneContents = paneContents,
        isExpanded = !windowSizeClass.isCompactWidth,
        isTransitioning = false,
        onNavigateToPane = { role -> scope.navigator.switchPane(role) }
    )

    // Render the wrapper with pane contents
    paneWrapperScope.paneWrapper(paneContents)
}

// =============================================================================
// Helper Functions
// =============================================================================

/**
 * Finds a ScreenNode by key in the navigation tree.
 */
private fun findScreenNodeByKey(node: NavNode, key: String): ScreenNode? {
    return when (node) {
        is ScreenNode -> if (node.key == key) node else null
        is com.jermey.quo.vadis.core.navigation.core.StackNode -> {
            node.children.firstNotNullOfOrNull { findScreenNodeByKey(it, key) }
        }

        is TabNode -> {
            node.stacks.firstNotNullOfOrNull { stack ->
                stack.children.firstNotNullOfOrNull { findScreenNodeByKey(it, key) }
            }
        }

        is PaneNode -> {
            node.paneConfigurations.values.firstNotNullOfOrNull { config ->
                findScreenNodeByKey(config.content, key)
            }
        }
    }
}

/**
 * Finds a TabNode by key in the navigation tree.
 */
private fun findTabNodeByKey(node: NavNode, key: String): TabNode? {
    return when (node) {
        is TabNode -> if (node.key == key) node else {
            node.stacks.firstNotNullOfOrNull { stack ->
                stack.children.firstNotNullOfOrNull { findTabNodeByKey(it, key) }
            }
        }

        is com.jermey.quo.vadis.core.navigation.core.StackNode -> {
            node.children.firstNotNullOfOrNull { findTabNodeByKey(it, key) }
        }

        is PaneNode -> {
            node.paneConfigurations.values.firstNotNullOfOrNull { config ->
                findTabNodeByKey(config.content, key)
            }
        }

        is ScreenNode -> null
    }
}

/**
 * Finds a PaneNode by key in the navigation tree.
 */
private fun findPaneNodeByKey(node: NavNode, key: String): PaneNode? {
    return when (node) {
        is PaneNode -> if (node.key == key) node else {
            node.paneConfigurations.values.firstNotNullOfOrNull { config ->
                findPaneNodeByKey(config.content, key)
            }
        }

        is com.jermey.quo.vadis.core.navigation.core.StackNode -> {
            node.children.firstNotNullOfOrNull { findPaneNodeByKey(it, key) }
        }

        is TabNode -> {
            node.stacks.firstNotNullOfOrNull { stack ->
                stack.children.firstNotNullOfOrNull { findPaneNodeByKey(it, key) }
            }
        }

        is ScreenNode -> null
    }
}

/**
 * Modifier for predictive back gesture transformations.
 *
 * Applies scale and translation transformations during predictive back gestures
 * to provide visual feedback to the user.
 *
 * @param progress Gesture progress from 0.0 to 1.0
 * @param isExiting Whether this surface is exiting (true) or entering (false)
 */
private fun Modifier.predictiveBackTransform(
    progress: Float,
    isExiting: Boolean
): Modifier {
    return if (isExiting) {
        // Exiting surface: scale down and shift
        this.graphicsLayer {
            val scale = 1f - (progress * PREDICTIVE_BACK_SCALE_FACTOR)
            scaleX = scale
            scaleY = scale

            // Slight parallax shift
            translationX = progress * size.width * PREDICTIVE_BACK_PARALLAX_FACTOR
        }
    } else {
        // Entering surface (below): scale up from 0.9
        this.graphicsLayer {
            val scale = (1f - PREDICTIVE_BACK_SCALE_FACTOR) + (progress * PREDICTIVE_BACK_SCALE_FACTOR)
            scaleX = scale
            scaleY = scale
        }
    }
}

// =============================================================================
// Alternative APIs
// =============================================================================

/**
 * QuoVadisHost variant that uses a content map instead of a lambda.
 *
 * Useful when destinations are known at compile time and you want
 * to avoid recomposition when the content lambda changes.
 *
 * ## Usage
 *
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     contentMap = mapOf(
 *         HomeDestination::class to { HomeScreen() },
 *         ProfileDestination::class to { dest -> ProfileScreen((dest as ProfileDestination).userId) }
 *     )
 * )
 * ```
 *
 * @param navigator The Navigator instance managing navigation state
 * @param contentMap Map from destination class to content composable
 * @param modifier Modifier for the root container
 * @param enablePredictiveBack Whether to enable predictive back gesture handling.
 *   When enabled, users can preview the back navigation result while performing
 *   a back gesture. Defaults to `true`.
 * @param animationRegistry Registry for transition animations. Defaults to [AnimationRegistry.Default].
 * @param tabWrapper User-provided wrapper for TabNode rendering
 * @param paneWrapper User-provided wrapper for PaneNode rendering
 * @param fallback Fallback content when no mapping exists for a destination
 */
@Composable
public fun <D : Destination> QuoVadisHost(
    navigator: Navigator,
    contentMap: Map<KClass<out D>, @Composable QuoVadisHostScope.(D) -> Unit>,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default,
    tabWrapper: TabWrapper = DefaultTabWrapper,
    paneWrapper: PaneWrapper = DefaultPaneWrapper,
    fallback: @Composable QuoVadisHostScope.(Destination) -> Unit = {
        error("No content registered for ${it::class.simpleName}")
    }
) {
    QuoVadisHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
        animationRegistry = animationRegistry,
        tabWrapper = tabWrapper,
        paneWrapper = paneWrapper
    ) { destination ->
        @Suppress("UNCHECKED_CAST")
        val contentProvider = contentMap[destination::class]
        if (contentProvider != null) {
            @Suppress("UNCHECKED_CAST")
            contentProvider(destination as D)
        } else {
            fallback(destination)
        }
    }
}

/**
 * QuoVadisHost variant that uses a [NavigationGraph] for content resolution.
 *
 * This is the most type-safe approach, using KSP-generated graphs.
 *
 * ## Usage
 *
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     graph = AppNavGraph // KSP-generated
 * )
 * ```
 *
 * @param navigator The Navigator instance managing navigation state
 * @param graph The navigation graph containing destination-to-content mappings
 * @param modifier Modifier for the root container
 * @param enablePredictiveBack Whether to enable predictive back gesture handling.
 *   When enabled, users can preview the back navigation result while performing
 *   a back gesture. Defaults to `true`.
 * @param animationRegistry Registry for transition animations. Defaults to [AnimationRegistry.Default].
 * @param tabWrapper User-provided wrapper for TabNode rendering
 * @param paneWrapper User-provided wrapper for PaneNode rendering
 */
@Composable
public fun QuoVadisHost(
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default,
    tabWrapper: TabWrapper = DefaultTabWrapper,
    paneWrapper: PaneWrapper = DefaultPaneWrapper
) {
    QuoVadisHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
        animationRegistry = animationRegistry,
        tabWrapper = tabWrapper,
        paneWrapper = paneWrapper
    ) { destination ->
        // Find the destination config from the graph
        val destConfig = graph.destinations.find { it.destination.route == destination.route }
        destConfig?.let { config ->
            // Prefer contentWithTransitionScope if available
            if (config.contentWithTransitionScope != null) {
                config.contentWithTransitionScope.invoke(destination, navigator, null)
            } else {
                config.content(destination, navigator)
            }
        }
    }
}

// =============================================================================
// Constants
// =============================================================================

/** Scale factor applied during predictive back (0.1 = 10% scale reduction) */
private const val PREDICTIVE_BACK_SCALE_FACTOR = 0.1f

/** Parallax shift factor during predictive back (0.1 = 10% width shift) */
private const val PREDICTIVE_BACK_PARALLAX_FACTOR = 0.1f
