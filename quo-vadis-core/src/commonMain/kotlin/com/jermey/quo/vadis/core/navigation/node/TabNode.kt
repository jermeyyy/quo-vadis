@file:OptIn(com.jermey.quo.vadis.core.InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.node

import com.jermey.quo.vadis.core.navigation.internal.GeneratedTabMetadata
import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Container node representing tabbed navigation with parallel stacks.
 *
 * A TabNode maintains multiple [StackNode]s, one for each tab, along with
 * an [activeStackIndex] indicating which tab is currently selected. Each
 * tab preserves its own navigation history independently.
 *
 * ## Behavior
 *
 * - **SwitchTab**: Updates [activeStackIndex]
 * - **Push**: Affects only the active stack
 * - **Pop**: Removes from active stack; if empty, may switch tabs (configurable)
 *
 * ## Lifecycle Management
 *
 * TabNode implements [LifecycleAwareNode] to provide proper lifecycle
 * state management for the tab container itself. This enables:
 * - Container-scoped state management
 * - Shared MVI containers accessible to all child screens
 * - Proper cleanup when the tab container is removed
 *
 * ## Scope-Aware Navigation
 *
 * When [scopeKey] is set, [com.jermey.quo.vadis.core.navigation.tree.TreeMutator.push] with a
 * [com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry] will check
 * if destinations belong to this container's scope. Out-of-scope destinations
 * navigate to the parent stack instead, preserving the tab container for
 * predictive back gestures.
 *
 * ## Serialization
 *
 * Only persistent navigation state is serialized. Runtime state ([uuid],
 * [isAttachedToNavigator], [isDisplayed], [composeSavedState]) is marked
 * as `@Transient` and regenerated on restoration.
 *
 * @property key Unique identifier for this tab container
 * @property parentKey Key of the containing node (null if root)
 * @property stacks List of StackNodes, one per tab
 * @property activeStackIndex Index of the currently active tab (0-based)
 * @property wrapperKey Key used to lookup the wrapper in [com.jermey.quo.vadis.core.dsl.registry.ContainerRegistry].
 *   This is typically the simple name of the tab class (e.g., "MainTabs")
 *   and is used by the hierarchical renderer to find the correct wrapper.
 *   Defaults to null, which means no custom wrapper is registered.
 * @property tabMetadata Metadata for each tab (label, icon, route) from @TabItem annotations.
 *   This is populated by KSP-generated code and used by the renderer to provide
 *   proper tab information to wrapper composables. Empty list uses fallback generation.
 * @property scopeKey Identifier for scope-aware navigation. When set, destinations
 *   not in this scope will navigate outside the tab container. Typically the
 *   sealed class simple name (e.g., "MainTabs"). Defaults to null (no scope enforcement).
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
@SerialName("tab")
class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,
    val wrapperKey: String? = null,
    val tabMetadata: List<GeneratedTabMetadata> = emptyList(),
    val scopeKey: String? = null
) : NavNode, LifecycleAwareNode {

    init {
        require(stacks.isNotEmpty()) { "TabNode must have at least one stack" }
        require(activeStackIndex in stacks.indices) {
            "activeStackIndex ($activeStackIndex) out of bounds for ${stacks.size} stacks"
        }
    }

    // --- Lifecycle Infrastructure (Transient - not serialized) ---

    /**
     * Unique stable identifier for this node instance.
     * Generated fresh on creation and after deserialization.
     */
    @Transient
    val uuid: String = Uuid.random().toHexString()

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

    // --- Tab-specific properties ---

    /**
     * The currently active stack.
     */
    val activeStack: StackNode
        get() = stacks[activeStackIndex]

    /**
     * Number of tabs in this container.
     */
    val tabCount: Int
        get() = stacks.size

    /**
     * Returns the stack at the given index.
     * @throws IndexOutOfBoundsException if index is invalid
     */
    fun stackAt(index: Int): StackNode = stacks[index]

    // --- Copy function (replacing data class copy) ---

    /**
     * Creates a copy of this TabNode with optionally modified properties.
     * Only navigation state is copied; lifecycle state is reset.
     */
    fun copy(
        key: String = this.key,
        parentKey: String? = this.parentKey,
        stacks: List<StackNode> = this.stacks,
        activeStackIndex: Int = this.activeStackIndex,
        wrapperKey: String? = this.wrapperKey,
        tabMetadata: List<GeneratedTabMetadata> = this.tabMetadata,
        scopeKey: String? = this.scopeKey
    ): TabNode = TabNode(
        key = key,
        parentKey = parentKey,
        stacks = stacks,
        activeStackIndex = activeStackIndex,
        wrapperKey = wrapperKey,
        tabMetadata = tabMetadata,
        scopeKey = scopeKey
    )

    // --- Equality based on persistent properties ---

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TabNode) return false
        return key == other.key &&
            parentKey == other.parentKey &&
            stacks == other.stacks &&
            activeStackIndex == other.activeStackIndex &&
            wrapperKey == other.wrapperKey &&
            tabMetadata == other.tabMetadata &&
            scopeKey == other.scopeKey
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + (parentKey?.hashCode() ?: 0)
        result = 31 * result + stacks.hashCode()
        result = 31 * result + activeStackIndex
        result = 31 * result + (wrapperKey?.hashCode() ?: 0)
        result = 31 * result + tabMetadata.hashCode()
        result = 31 * result + (scopeKey?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "TabNode(key='$key', parentKey=$parentKey, " +
            "activeStackIndex=$activeStackIndex, tabCount=$tabCount)"
}
