package com.jermey.quo.vadis.core.dsl

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * DSL builder for configuring pane container entries.
 *
 * Pane containers support adaptive multi-pane layouts that can adjust
 * based on screen size (e.g., master-detail, three-column layouts).
 *
 * ## Usage
 *
 * ### Two-pane master-detail layout
 * ```kotlin
 * panes<ListDetail>("list-detail") {
 *     initialPane = PaneRole.Primary
 *     backBehavior = PaneBackBehavior.PopUntilScaffoldValueChange
 *
 *     primary(weight = 0.4f, minWidth = 300.dp) {
 *         root(ListScreen)
 *         alwaysVisible()
 *     }
 *
 *     secondary(weight = 0.6f) {
 *         root(DetailPlaceholder)
 *     }
 * }
 * ```
 *
 * ### Three-pane layout
 * ```kotlin
 * panes<EmailLayout>("email") {
 *     primary(weight = 0.25f) {
 *         root(FolderList)
 *         alwaysVisible()
 *     }
 *
 *     secondary(weight = 0.35f) {
 *         root(EmailList)
 *     }
 *
 *     extra(weight = 0.4f) {
 *         root(EmailDetail)
 *     }
 * }
 * ```
 *
 * @see NavigationConfigBuilder.panes
 * @see PaneEntry
 * @see PaneRole
 */
@NavigationConfigDsl
class PanesBuilder {

    /**
     * Map of pane roles to their configuration.
     */
    internal val panes: MutableMap<PaneRole, PaneEntry> = mutableMapOf()

    /**
     * The initially active/focused pane role.
     *
     * On compact screens, this determines which pane is visible initially.
     * Defaults to [PaneRole.Primary].
     */
    var initialPane: PaneRole = PaneRole.Primary

    /**
     * Back navigation behavior for this pane container.
     *
     * Controls how back gestures are handled when multiple panes are visible.
     * Defaults to [PaneBackBehavior.PopLatest].
     */
    var backBehavior: PaneBackBehavior = PaneBackBehavior.PopLatest

    /**
     * Configures the primary pane.
     *
     * The primary pane typically contains the main navigation list or content.
     * It's usually visible in all layout configurations.
     *
     * ## Example
     *
     * ```kotlin
     * primary(weight = 0.4f, minWidth = 300.dp) {
     *     root(ListScreen)
     *     alwaysVisible()
     * }
     * ```
     *
     * @param weight Relative width weight in expanded layouts (0.0 to 1.0)
     * @param minWidth Minimum width for this pane
     * @param builder Content configuration lambda
     */
    fun primary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        val contentBuilder = PaneContentBuilder().apply(builder)
        panes[PaneRole.Primary] = PaneEntry(
            role = PaneRole.Primary,
            weight = weight,
            minWidth = minWidth,
            content = contentBuilder.build()
        )
    }

    /**
     * Configures the supporting/secondary pane.
     *
     * The supporting pane typically shows detail content related to the
     * primary selection. It may be hidden on compact screens.
     *
     * ## Example
     *
     * ```kotlin
     * secondary(weight = 0.6f) {
     *     root(DetailScreen)
     * }
     * ```
     *
     * @param weight Relative width weight in expanded layouts
     * @param minWidth Minimum width for this pane
     * @param builder Content configuration lambda
     */
    fun secondary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        val contentBuilder = PaneContentBuilder().apply(builder)
        panes[PaneRole.Supporting] = PaneEntry(
            role = PaneRole.Supporting,
            weight = weight,
            minWidth = minWidth,
            content = contentBuilder.build()
        )
    }

    /**
     * Configures the extra/tertiary pane.
     *
     * The extra pane is for supplementary content and is typically only
     * visible on large screens (e.g., tablets in landscape, desktops).
     *
     * ## Example
     *
     * ```kotlin
     * extra(weight = 0.25f, minWidth = 200.dp) {
     *     root(PropertiesPanel)
     * }
     * ```
     *
     * @param weight Relative width weight in expanded layouts
     * @param minWidth Minimum width for this pane
     * @param builder Content configuration lambda
     */
    fun extra(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        val contentBuilder = PaneContentBuilder().apply(builder)
        panes[PaneRole.Extra] = PaneEntry(
            role = PaneRole.Extra,
            weight = weight,
            minWidth = minWidth,
            content = contentBuilder.build()
        )
    }

    /**
     * Alias for [secondary] - configures the tertiary/supporting pane.
     *
     * @param weight Relative width weight in expanded layouts
     * @param minWidth Minimum width for this pane
     * @param builder Content configuration lambda
     */
    fun tertiary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        secondary(weight, minWidth, builder)
    }

    /**
     * Builds the panes configuration.
     *
     * @return [BuiltPanesConfig] containing all pane entries and settings
     */
    fun build(): BuiltPanesConfig = BuiltPanesConfig(
        panes = panes.toMap(),
        initialPane = initialPane,
        backBehavior = backBehavior
    )
}

/**
 * Represents a single pane entry in a pane container.
 *
 * @property role The semantic role of this pane
 * @property weight Relative width weight in expanded layouts (0.0 to 1.0)
 * @property minWidth Minimum width for this pane
 * @property content The built content configuration
 */
data class PaneEntry(
    val role: PaneRole,
    val weight: Float,
    val minWidth: Dp,
    val content: BuiltPaneContent
)

/**
 * DSL builder for configuring pane content.
 *
 * Specifies the root destination and visibility behavior for a pane.
 *
 * ## Usage
 *
 * ```kotlin
 * primary(weight = 0.4f) {
 *     root(ListScreen)
 *     alwaysVisible() // Pane is always visible even on compact screens
 * }
 * ```
 *
 * @see PanesBuilder
 */
@NavigationConfigDsl
class PaneContentBuilder {

    /**
     * The root destination for this pane.
     */
    internal var rootDestination: NavDestination? = null

    /**
     * Whether this pane should always be visible, even on compact screens.
     */
    internal var isAlwaysVisible: Boolean = false

    /**
     * Sets the root destination for this pane.
     *
     * The root destination is displayed when the pane is first shown or
     * when navigation is cleared back to the root.
     *
     * @param destination The root destination
     */
    fun root(destination: NavDestination) {
        rootDestination = destination
    }

    /**
     * Marks this pane as always visible.
     *
     * By default, panes may be hidden on compact screens to show only
     * the active pane. Calling this ensures the pane remains visible
     * regardless of screen size.
     *
     * Useful for primary navigation panes that should always be accessible.
     */
    fun alwaysVisible() {
        isAlwaysVisible = true
    }

    /**
     * Builds the pane content configuration.
     *
     * @return [BuiltPaneContent] with the configured settings
     */
    fun build(): BuiltPaneContent = BuiltPaneContent(
        rootDestination = rootDestination,
        isAlwaysVisible = isAlwaysVisible
    )
}
