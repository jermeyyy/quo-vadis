package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Container node for adaptive pane layouts.
 *
 * PaneNode represents layouts where multiple panes can be displayed
 * simultaneously on large screens or collapsed to single-pane on compact screens.
 *
 * ## Adaptive Behavior
 *
 * The NavNode tree represents LOGICAL navigation state, not visual layout.
 * Visual adaptation (which panes are visible, side-by-side vs stacked) is
 * determined by the renderer based on:
 * - WindowSizeClass (observed from platform)
 * - AdaptStrategy (stored per pane)
 * - Animation state (during transitions)
 *
 * - On **compact** screens: Only [activePaneRole] is visible
 * - On **medium** screens: Primary visible, others can levitate (overlay)
 * - On **expanded** screens: Multiple panes displayed side-by-side
 *
 * ## Use Cases
 *
 * - **Master-Detail**: List pane (Primary) + Detail pane (Supporting)
 * - **Supporting Pane**: Main content (Primary) + Context panel (Supporting)
 * - **Multi-Column**: Navigation rail + Content + Detail (all three roles)
 *
 * ## Scope-Aware Navigation
 *
 * When [scopeKey] is set, [TreeMutator.push] with a [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] will check
 * if destinations belong to this container's scope. Out-of-scope destinations
 * navigate to the parent stack instead, preserving the pane container for
 * predictive back gestures.
 *
 * @property key Unique identifier for this pane container
 * @property parentKey Key of containing node (null if root)
 * @property paneConfigurations Map of pane roles to their configurations
 * @property activePaneRole The pane that currently has navigation focus
 * @property backBehavior How back navigation should behave in this container
 * @property scopeKey Identifier for scope-aware navigation. When set, destinations
 *   not in this scope will navigate outside the pane container. Typically the
 *   sealed class simple name. Defaults to null (no scope enforcement).
 */
@Serializable
@SerialName("pane")
data class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole = PaneRole.Primary,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
    val scopeKey: String? = null
) : NavNode {

    init {
        require(paneConfigurations.containsKey(PaneRole.Primary)) {
            "PaneNode must have at least a Primary pane"
        }
        require(paneConfigurations.containsKey(activePaneRole)) {
            "activePaneRole ($activePaneRole) must exist in paneConfigurations"
        }
    }

    /**
     * The content node for the given role, or null if role not configured.
     */
    fun paneContent(role: PaneRole): NavNode? = paneConfigurations[role]?.content

    /**
     * The adaptation strategy for the given role.
     */
    fun adaptStrategy(role: PaneRole): AdaptStrategy? = paneConfigurations[role]?.adaptStrategy

    /**
     * The actively focused pane's content.
     */
    val activePaneContent: NavNode?
        get() = paneContent(activePaneRole)

    /**
     * Number of configured panes.
     */
    val paneCount: Int
        get() = paneConfigurations.size

    /**
     * All configured pane roles.
     */
    val configuredRoles: Set<PaneRole>
        get() = paneConfigurations.keys
}