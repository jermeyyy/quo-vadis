package com.jermey.quo.vadis.core.compose.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * Represents the content of a single pane in a multi-pane layout.
 *
 * Used by [PaneContainer] to provide pane information and content
 * to user-defined container composables.
 *
 * @property role The semantic role of this pane (Primary, Supporting, or Extra)
 * @property content The composable content to render in this pane
 * @property isVisible Whether this pane should be visible in the current layout
 * @property hasContent Whether this pane has navigable content (non-empty stack)
 */
data class PaneContent(
    val role: PaneRole,
    val content: @Composable () -> Unit,
    val isVisible: Boolean = true,
    val hasContent: Boolean = true
)

/**
 * Scope interface for pane container composables.
 *
 * This scope provides access to the pane layout state and configuration,
 * allowing user-defined containers to implement custom multi-pane layouts
 * while delegating content rendering to the library.
 *
 * ## Usage
 *
 * The scope is receiver for [PaneContainer] composables. In expanded mode,
 * use [paneContents] to arrange panes in a custom layout:
 *
 * ```kotlin
 * @PaneContainer(MyPane::class)
 * @Composable
 * fun MyPaneContainer(scope: PaneContainerScope, content: @Composable () -> Unit) {
 *     if (scope.isExpanded) {
 *         // Custom layout for expanded mode
 *         Row(modifier = Modifier.fillMaxSize()) {
 *             scope.paneContents.filter { it.isVisible }.forEach { pane ->
 *                 val weight = when (pane.role) {
 *                     PaneRole.Primary -> 0.4f
 *                     PaneRole.Supporting -> 0.6f
 *                     PaneRole.Extra -> 0.25f
 *                 }
 *                 Box(modifier = Modifier.weight(weight)) {
 *                     pane.content()
 *                 }
 *             }
 *         }
 *     } else {
 *         // Compact mode: single pane navigation
 *         content()
 *     }
 * }
 * ```
 *
 * @see PaneContainer
 * @see PaneContent
 * @see PaneRole
 */
@Stable
interface PaneContainerScope {

    /**
     * The navigator instance for this pane container.
     *
     * Can be used for programmatic navigation or accessing
     * navigation state.
     */
    val navigator: Navigator

    /**
     * The currently active pane role.
     *
     * In single-pane mode (e.g., on phones), this indicates
     * which pane is currently visible.
     */
    val activePaneRole: PaneRole

    /**
     * Total number of panes configured in this container.
     */
    val paneCount: Int

    /**
     * Number of panes currently visible.
     *
     * This may differ from [paneCount] based on screen size
     * and adaptive layout configuration.
     */
    val visiblePaneCount: Int

    /**
     * Whether the layout is in expanded (multi-pane) mode.
     *
     * - `true`: Multiple panes are visible side-by-side
     * - `false`: Single pane mode (stack-like behavior)
     */
    val isExpanded: Boolean

    /**
     * Whether a pane transition animation is in progress.
     */
    val isTransitioning: Boolean

    /**
     * List of pane content slots for custom layout arrangement.
     *
     * In expanded mode, use this to render panes in a custom layout (e.g., Row).
     * Each [PaneContent] contains the role, visibility flag, and content composable.
     *
     * Example:
     * ```kotlin
     * if (scope.isExpanded) {
     *     Row {
     *         scope.paneContents.filter { it.isVisible }.forEach { pane ->
     *             Box(Modifier.weight(1f)) { pane.content() }
     *         }
     *     }
     * }
     * ```
     */
    val paneContents: List<PaneContent>

    /**
     * Navigate to show the specified pane role.
     *
     * In expanded mode, this may highlight or focus the pane.
     * In compact mode, this switches to show that pane.
     *
     * @param role The pane role to navigate to
     */
    fun navigateToPane(role: PaneRole)
}

/**
 * Internal implementation of [PaneContainerScope].
 *
 * Created when processing PaneNode to provide the container composable
 * with access to pane layout state and actions.
 *
 * @property navigator The navigator instance for this pane container
 * @property activePaneRole The currently active pane role
 * @property paneCount Total number of configured panes
 * @property visiblePaneCount Number of currently visible panes
 * @property isExpanded Whether in expanded (multi-pane) mode
 * @property isTransitioning Whether a pane transition is in progress
 * @property paneContents List of pane content slots for custom layout
 * @property onNavigateToPane Callback invoked when navigating to a pane
 */
internal class PaneContainerScopeImpl(
    override val navigator: Navigator,
    override val activePaneRole: PaneRole,
    override val paneCount: Int,
    override val visiblePaneCount: Int,
    override val isExpanded: Boolean,
    override val isTransitioning: Boolean,
    override val paneContents: List<PaneContent>,
    private val onNavigateToPane: (PaneRole) -> Unit
) : PaneContainerScope {

    override fun navigateToPane(role: PaneRole) {
        onNavigateToPane(role)
    }
}

/**
 * Creates a [PaneContainerScopeImpl] with the given parameters.
 *
 * Factory function for creating pane container scopes.
 *
 * @param navigator The navigator instance
 * @param activePaneRole Currently active pane role
 * @param paneContents List of pane contents with visibility and content lambdas
 * @param isExpanded Whether in expanded mode
 * @param isTransitioning Whether transitioning between panes
 * @param onNavigateToPane Callback for pane navigation
 * @return A new [PaneContainerScope] implementation
 */
internal fun createPaneContainerScope(
    navigator: Navigator,
    activePaneRole: PaneRole,
    paneContents: List<PaneContent>,
    isExpanded: Boolean,
    isTransitioning: Boolean,
    onNavigateToPane: (PaneRole) -> Unit
): PaneContainerScope = PaneContainerScopeImpl(
    navigator = navigator,
    activePaneRole = activePaneRole,
    paneCount = paneContents.size,
    visiblePaneCount = paneContents.count { it.isVisible },
    isExpanded = isExpanded,
    isTransitioning = isTransitioning,
    paneContents = paneContents,
    onNavigateToPane = onNavigateToPane
)
