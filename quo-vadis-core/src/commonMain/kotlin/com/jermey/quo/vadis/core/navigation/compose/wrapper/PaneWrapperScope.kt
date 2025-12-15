package com.jermey.quo.vadis.core.navigation.compose.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneRole

/**
 * Represents the content of a single pane in a multi-pane layout.
 *
 * Used by [PaneWrapper] to provide pane information and content
 * to user-defined wrapper composables.
 *
 * @property role The semantic role of this pane (Primary, Supporting, or Extra)
 * @property content The composable content to render in this pane
 * @property isVisible Whether this pane should be visible in the current layout
 */
public data class PaneContent(
    val role: PaneRole,
    val content: @Composable () -> Unit,
    val isVisible: Boolean = true
)

/**
 * Scope interface for pane wrapper composables.
 *
 * This scope provides access to the pane layout state and configuration,
 * allowing user-defined wrappers to implement custom multi-pane layouts
 * while delegating content rendering to the library.
 *
 * ## Usage
 *
 * The scope is receiver for [PaneWrapper] composables:
 *
 * ```kotlin
 * val myPaneWrapper: PaneWrapper = { paneContents ->
 *     Row(modifier = Modifier.fillMaxSize()) {
 *         paneContents.filter { it.isVisible }.forEach { pane ->
 *             val weight = when (pane.role) {
 *                 PaneRole.Primary -> 0.65f
 *                 PaneRole.Supporting -> 0.35f
 *                 PaneRole.Extra -> 0.25f
 *             }
 *             Box(modifier = Modifier.weight(weight)) {
 *                 pane.content()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @see PaneWrapper
 * @see PaneContent
 * @see PaneRole
 */
@Stable
public interface PaneWrapperScope {

    /**
     * The navigator instance for this pane container.
     *
     * Can be used for programmatic navigation or accessing
     * navigation state.
     */
    public val navigator: Navigator

    /**
     * The currently active pane role.
     *
     * In single-pane mode (e.g., on phones), this indicates
     * which pane is currently visible.
     */
    public val activePaneRole: PaneRole

    /**
     * Total number of panes configured in this container.
     */
    public val paneCount: Int

    /**
     * Number of panes currently visible.
     *
     * This may differ from [paneCount] based on screen size
     * and adaptive layout configuration.
     */
    public val visiblePaneCount: Int

    /**
     * Whether the layout is in expanded (multi-pane) mode.
     *
     * - `true`: Multiple panes are visible side-by-side
     * - `false`: Single pane mode (stack-like behavior)
     */
    public val isExpanded: Boolean

    /**
     * Whether a pane transition animation is in progress.
     */
    public val isTransitioning: Boolean

    /**
     * Navigate to show the specified pane role.
     *
     * In expanded mode, this may highlight or focus the pane.
     * In compact mode, this switches to show that pane.
     *
     * @param role The pane role to navigate to
     */
    public fun navigateToPane(role: PaneRole)
}

/**
 * Internal implementation of [PaneWrapperScope].
 *
 * Created when processing PaneNode to provide the wrapper composable
 * with access to pane layout state and actions.
 *
 * @property navigator The navigator instance for this pane container
 * @property activePaneRole The currently active pane role
 * @property paneCount Total number of configured panes
 * @property visiblePaneCount Number of currently visible panes
 * @property isExpanded Whether in expanded (multi-pane) mode
 * @property isTransitioning Whether a pane transition is in progress
 * @property onNavigateToPane Callback invoked when navigating to a pane
 */
internal class PaneWrapperScopeImpl(
    override val navigator: Navigator,
    override val activePaneRole: PaneRole,
    override val paneCount: Int,
    override val visiblePaneCount: Int,
    override val isExpanded: Boolean,
    override val isTransitioning: Boolean,
    private val onNavigateToPane: (PaneRole) -> Unit
) : PaneWrapperScope {

    override fun navigateToPane(role: PaneRole) {
        onNavigateToPane(role)
    }
}

/**
 * Creates a [PaneWrapperScopeImpl] with the given parameters.
 *
 * Factory function for creating pane wrapper scopes.
 *
 * @param navigator The navigator instance
 * @param activePaneRole Currently active pane role
 * @param paneContents List of pane contents with visibility
 * @param isExpanded Whether in expanded mode
 * @param isTransitioning Whether transitioning between panes
 * @param onNavigateToPane Callback for pane navigation
 * @return A new [PaneWrapperScope] implementation
 */
internal fun createPaneWrapperScope(
    navigator: Navigator,
    activePaneRole: PaneRole,
    paneContents: List<PaneContent>,
    isExpanded: Boolean,
    isTransitioning: Boolean,
    onNavigateToPane: (PaneRole) -> Unit
): PaneWrapperScope = PaneWrapperScopeImpl(
    navigator = navigator,
    activePaneRole = activePaneRole,
    paneCount = paneContents.size,
    visiblePaneCount = paneContents.count { it.isVisible },
    isExpanded = isExpanded,
    isTransitioning = isTransitioning,
    onNavigateToPane = onNavigateToPane
)