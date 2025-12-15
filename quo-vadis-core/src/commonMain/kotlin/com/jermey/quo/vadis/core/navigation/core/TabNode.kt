package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
 * ## Scope-Aware Navigation
 *
 * When [scopeKey] is set, [TreeMutator.push] with a [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry] will check
 * if destinations belong to this container's scope. Out-of-scope destinations
 * navigate to the parent stack instead, preserving the tab container for
 * predictive back gestures.
 *
 * @property key Unique identifier for this tab container
 * @property parentKey Key of the containing node (null if root)
 * @property stacks List of StackNodes, one per tab
 * @property activeStackIndex Index of the currently active tab (0-based)
 * @property wrapperKey Key used to lookup the wrapper in [WrapperRegistry].
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
@Serializable
@SerialName("tab")
data class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,
    val wrapperKey: String? = null,
    val tabMetadata: List<GeneratedTabMetadata> = emptyList(),
    val scopeKey: String? = null
) : NavNode {

    init {
        require(stacks.isNotEmpty()) { "TabNode must have at least one stack" }
        require(activeStackIndex in stacks.indices) {
            "activeStackIndex ($activeStackIndex) out of bounds for ${stacks.size} stacks"
        }
    }

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
}