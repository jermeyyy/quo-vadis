package com.jermey.quo.vadis.core.navigation.node

import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
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
 * ## Lifecycle Management
 *
 * PaneNode implements [LifecycleAwareNode] to provide proper lifecycle
 * state management for the pane container itself. This enables:
 * - Container-scoped state management
 * - Shared MVI containers for pane coordination
 * - Proper cleanup when the pane container is removed
 *
 * ## Scope-Aware Navigation
 *
 * When [scopeKey] is set, [com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator.push] with a
 * [com.jermey.quo.vadis.core.registry.ScopeRegistry] will check
 * if destinations belong to this container's scope. Out-of-scope destinations
 * navigate to the parent stack instead, preserving the pane container for
 * predictive back gestures.
 *
 * ## Serialization
 *
 * Only persistent navigation state is serialized. Runtime state
 * ([isAttachedToNavigator], [isDisplayed], [composeSavedState]) is marked
 * as `@Transient` and regenerated on restoration.
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
class PaneNode(
    override val key: NodeKey,
    override val parentKey: NodeKey?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole = PaneRole.Primary,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
    val scopeKey: ScopeKey? = null
) : NavNode, LifecycleAwareNode by LifecycleDelegate() {

    init {
        require(paneConfigurations.containsKey(PaneRole.Primary)) {
            "PaneNode must have at least a Primary pane"
        }
        require(paneConfigurations.containsKey(activePaneRole)) {
            "activePaneRole ($activePaneRole) must exist in paneConfigurations"
        }
    }

    // --- Pane-specific properties ---

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

    // --- Copy function (replacing data class copy) ---

    /**
     * Creates a copy of this PaneNode with optionally modified properties.
     * Only navigation state is copied; lifecycle state is reset.
     */
    fun copy(
        key: NodeKey = this.key,
        parentKey: NodeKey? = this.parentKey,
        paneConfigurations: Map<PaneRole, PaneConfiguration> = this.paneConfigurations,
        activePaneRole: PaneRole = this.activePaneRole,
        backBehavior: PaneBackBehavior = this.backBehavior,
        scopeKey: ScopeKey? = this.scopeKey
    ): PaneNode = PaneNode(
        key = key,
        parentKey = parentKey,
        paneConfigurations = paneConfigurations,
        activePaneRole = activePaneRole,
        backBehavior = backBehavior,
        scopeKey = scopeKey
    )

    // --- Equality based on persistent properties ---

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PaneNode) return false
        return key == other.key &&
            parentKey == other.parentKey &&
            paneConfigurations == other.paneConfigurations &&
            activePaneRole == other.activePaneRole &&
            backBehavior == other.backBehavior &&
            scopeKey == other.scopeKey
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (parentKey?.hashCode() ?: 0)
        result = 31 * result + paneConfigurations.hashCode()
        result = 31 * result + activePaneRole.hashCode()
        result = 31 * result + backBehavior.hashCode()
        result = 31 * result + (scopeKey?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "PaneNode(key='$key', parentKey=$parentKey, " +
            "activePaneRole=$activePaneRole, paneCount=$paneCount)"
}
