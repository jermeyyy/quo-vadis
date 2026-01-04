package com.jermey.quo.vadis.core.navigation.node

import com.jermey.quo.vadis.core.navigation.pane.AdaptStrategy
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi

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
 * When [scopeKey] is set, [com.jermey.quo.vadis.core.navigation.tree.TreeMutator.push] with a
 * [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] will check
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
@OptIn(ExperimentalUuidApi::class)
@Serializable
@SerialName("pane")
class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole = PaneRole.Primary,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
    val scopeKey: String? = null
) : NavNode, LifecycleAwareNode {

    init {
        require(paneConfigurations.containsKey(PaneRole.Primary)) {
            "PaneNode must have at least a Primary pane"
        }
        require(paneConfigurations.containsKey(activePaneRole)) {
            "activePaneRole ($activePaneRole) must exist in paneConfigurations"
        }
    }

    // --- Lifecycle Infrastructure (Transient - not serialized) ---

    /**
     * Whether this node is attached to the navigator tree.
     */
    @Transient
    override var isAttachedToNavigator: Boolean = false
        private set

    /**
     * Whether this node is currently being displayed.
     */
    @Transient
    override var isDisplayed: Boolean = false
        private set

    /**
     * Saved state for Compose rememberSaveable.
     */
    @Transient
    override var composeSavedState: Map<String, List<Any?>>? = null

    /**
     * Callbacks to invoke when this node is destroyed.
     */
    @Transient
    private val onDestroyCallbacks = mutableListOf<() -> Unit>()

    // --- Lifecycle Transitions ---

    override fun attachToNavigator() {
        // Idempotent - safe to call multiple times
        isAttachedToNavigator = true
    }

    override fun attachToUI() {
        // Auto-attach to navigator if not already attached
        // This handles cases where nodes are created and immediately rendered
        if (!isAttachedToNavigator) {
            attachToNavigator()
        }
        isDisplayed = true
    }

    override fun detachFromUI() {
        isDisplayed = false
        if (!isAttachedToNavigator) {
            close()
        }
    }

    override fun detachFromNavigator() {
        isAttachedToNavigator = false
        if (!isDisplayed) {
            close()
        }
    }

    override fun addOnDestroyCallback(callback: () -> Unit) {
        onDestroyCallbacks.add(callback)
    }

    override fun removeOnDestroyCallback(callback: () -> Unit) {
        onDestroyCallbacks.remove(callback)
    }

    /**
     * Cleanup when node is fully detached.
     */
    private fun close() {
        // Invoke all destroy callbacks
        onDestroyCallbacks.forEach { it.invoke() }
        onDestroyCallbacks.clear()
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
        key: String = this.key,
        parentKey: String? = this.parentKey,
        paneConfigurations: Map<PaneRole, PaneConfiguration> = this.paneConfigurations,
        activePaneRole: PaneRole = this.activePaneRole,
        backBehavior: PaneBackBehavior = this.backBehavior,
        scopeKey: String? = this.scopeKey
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
