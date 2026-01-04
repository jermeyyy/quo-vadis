@file:OptIn(InternalQuoVadisApi::class)

package com.jermey.quo.vadis.core.navigation.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.dsl.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.tree.config.PopBehavior
import com.jermey.quo.vadis.core.navigation.tree.operations.BackOperations
import com.jermey.quo.vadis.core.navigation.tree.operations.PaneOperations
import com.jermey.quo.vadis.core.navigation.tree.operations.PopOperations
import com.jermey.quo.vadis.core.navigation.tree.operations.PushOperations
import com.jermey.quo.vadis.core.navigation.tree.operations.TabOperations
import com.jermey.quo.vadis.core.navigation.tree.operations.TreeNodeOperations
import com.jermey.quo.vadis.core.navigation.tree.result.BackResult
import com.jermey.quo.vadis.core.navigation.tree.result.PopResult
import com.jermey.quo.vadis.core.navigation.tree.util.KeyGenerator

/**
 * Pure functional operations for manipulating the NavNode tree.
 *
 * This object serves as a façade that delegates to specialized operation classes:
 * - [PushOperations] for forward navigation
 * - [PopOperations] for backward navigation
 * - [TabOperations] for tab switching
 * - [PaneOperations] for pane navigation
 * - [BackOperations] for tree-aware back handling
 * - [TreeNodeOperations] for low-level tree manipulation
 *
 * All operations are immutable - they return new tree instances rather than
 * modifying existing ones. This enables:
 *
 * - **Structural sharing**: Unchanged subtrees are reused by reference
 * - **Predictable updates**: No side effects, easy to test
 * - **Transaction support**: Compare old/new states for animations
 *
 * ## Mathematical Model
 *
 * Navigation operations are functional reducers:
 *
 * ```
 * S_new = f(S_old, Action)
 * ```
 *
 * Where:
 * - `S_old` is the current NavNode tree
 * - `Action` is a navigation intent (push, pop, switchTab, etc.)
 * - `S_new` is the resulting NavNode tree
 *
 * ## Usage
 *
 * ```kotlin
 * val navigator = Navigator()
 * val newState = TreeMutator.push(navigator.state.value, destination)
 * navigator.updateState(newState)
 * ```
 *
 * ## Thread Safety
 *
 * All operations are pure functions with no mutable state, making them
 * inherently thread-safe without requiring synchronization.
 *
 * @see PushOperations
 * @see PopOperations
 * @see TabOperations
 * @see PaneOperations
 * @see BackOperations
 * @see TreeNodeOperations
 */
object TreeMutator {

    // =========================================================================
    // PUSH OPERATIONS (delegate to PushOperations)
    // =========================================================================

    /**
     * Push a destination onto the deepest active stack.
     *
     * @see PushOperations.push
     */
    fun push(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.push(root, destination, generateKey)

    /**
     * Push a destination onto a specific stack identified by key.
     *
     * @see PushOperations.pushToStack
     */
    fun pushToStack(
        root: NavNode,
        stackKey: String,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.pushToStack(root, stackKey, destination, generateKey)

    /**
     * Push a destination with scope awareness, tab switching, and pane role routing.
     *
     * @see PushOperations.push
     */
    fun push(
        root: NavNode,
        destination: NavDestination,
        scopeRegistry: ScopeRegistry,
        paneRoleRegistry: PaneRoleRegistry = PaneRoleRegistry.Empty,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode =
        PushOperations.push(root, destination, scopeRegistry, paneRoleRegistry, generateKey)

    /**
     * Push multiple destinations at once.
     *
     * @see PushOperations.pushAll
     */
    fun pushAll(
        root: NavNode,
        destinations: List<NavDestination>,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.pushAll(root, destinations, generateKey)

    /**
     * Clear a stack and push a single screen.
     *
     * @see PushOperations.clearAndPush
     */
    fun clearAndPush(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.clearAndPush(root, destination, generateKey)

    /**
     * Clear a specific stack and push a single screen.
     *
     * @see PushOperations.clearStackAndPush
     */
    fun clearStackAndPush(
        root: NavNode,
        stackKey: String,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.clearStackAndPush(root, stackKey, destination, generateKey)

    /**
     * Replace the currently active screen with a new destination.
     *
     * @see PushOperations.replaceCurrent
     */
    fun replaceCurrent(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode = PushOperations.replaceCurrent(root, destination, generateKey)

    // =========================================================================
    // POP OPERATIONS (delegate to PopOperations)
    // =========================================================================

    /**
     * Pop the active screen from the deepest active stack.
     *
     * @see PopOperations.pop
     */
    fun pop(
        root: NavNode,
        behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY
    ): NavNode? = PopOperations.pop(root, behavior)

    /**
     * Pop all screens from the active stack until the predicate matches.
     *
     * @see PopOperations.popTo
     */
    fun popTo(
        root: NavNode,
        inclusive: Boolean = false,
        predicate: (NavNode) -> Boolean
    ): NavNode = PopOperations.popTo(root, inclusive, predicate)

    /**
     * Pop to a screen with the given route.
     *
     * @see PopOperations.popToRoute
     */
    fun popToRoute(
        root: NavNode,
        route: String,
        inclusive: Boolean = false
    ): NavNode = PopOperations.popToRoute(root, route, inclusive)

    /**
     * Pop to a screen with destination matching the given type.
     *
     * @see PopOperations.popToDestination
     */
    inline fun <reified D : NavDestination> popToDestination(
        root: NavNode,
        inclusive: Boolean = false
    ): NavNode = PopOperations.popToDestination<D>(root, inclusive)

    // =========================================================================
    // TAB OPERATIONS (delegate to TabOperations)
    // =========================================================================

    /**
     * Switch to a different tab in a TabNode.
     *
     * @see TabOperations.switchTab
     */
    fun switchTab(
        root: NavNode,
        tabNodeKey: String,
        newIndex: Int
    ): NavNode = TabOperations.switchTab(root, tabNodeKey, newIndex)

    /**
     * Switch to a different tab in the first TabNode found in the active path.
     *
     * @see TabOperations.switchActiveTab
     */
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode =
        TabOperations.switchActiveTab(root, newIndex)

    // =========================================================================
    // PANE OPERATIONS (delegate to PaneOperations)
    // =========================================================================

    /**
     * Navigates to a destination within the specified pane role.
     *
     * @see PaneOperations.navigateToPane
     */
    fun navigateToPane(
        root: NavNode,
        nodeKey: String,
        role: PaneRole,
        destination: NavDestination,
        switchFocus: Boolean = true,
        generateKey: () -> String = KeyGenerator.Default::generate
    ): NavNode =
        PaneOperations.navigateToPane(root, nodeKey, role, destination, switchFocus, generateKey)

    /**
     * Switches the active pane role without navigating.
     *
     * @see PaneOperations.switchActivePane
     */
    fun switchActivePane(
        root: NavNode,
        nodeKey: String,
        role: PaneRole
    ): NavNode = PaneOperations.switchActivePane(root, nodeKey, role)

    /**
     * Pops the top entry from the specified pane's stack.
     *
     * @see PaneOperations.popPane
     */
    fun popPane(
        root: NavNode,
        nodeKey: String,
        role: PaneRole
    ): NavNode? = PaneOperations.popPane(root, nodeKey, role)

    /**
     * Pop with respect to PaneBackBehavior.
     *
     * @see PaneOperations.popWithPaneBehavior
     */
    fun popWithPaneBehavior(root: NavNode): PopResult =
        PaneOperations.popWithPaneBehavior(root)

    /**
     * Pop from a pane with awareness of window size.
     *
     * @see PaneOperations.popPaneAdaptive
     */
    fun popPaneAdaptive(root: NavNode, isCompact: Boolean): PopResult =
        PaneOperations.popPaneAdaptive(root, isCompact)

    /**
     * Sets or updates the configuration for a pane role.
     *
     * @see PaneOperations.setPaneConfiguration
     */
    fun setPaneConfiguration(
        root: NavNode,
        nodeKey: String,
        role: PaneRole,
        config: PaneConfiguration
    ): NavNode = PaneOperations.setPaneConfiguration(root, nodeKey, role, config)

    /**
     * Removes a pane configuration.
     *
     * @see PaneOperations.removePaneConfiguration
     */
    fun removePaneConfiguration(
        root: NavNode,
        nodeKey: String,
        role: PaneRole
    ): NavNode = PaneOperations.removePaneConfiguration(root, nodeKey, role)

    // =========================================================================
    // UTILITY OPERATIONS (delegate to TreeNodeOperations)
    // =========================================================================

    /**
     * Replace a node in the tree by key.
     *
     * @see TreeNodeOperations.replaceNode
     */
    fun replaceNode(root: NavNode, targetKey: String, newNode: NavNode): NavNode =
        TreeNodeOperations.replaceNode(root, targetKey, newNode)

    /**
     * Remove a node from the tree by key.
     *
     * @see TreeNodeOperations.removeNode
     */
    fun removeNode(root: NavNode, targetKey: String): NavNode? =
        TreeNodeOperations.removeNode(root, targetKey)

    // =========================================================================
    // BACK OPERATIONS (delegate to BackOperations)
    // =========================================================================

    /**
     * Check if back navigation is possible from the current state.
     *
     * @see BackOperations.canGoBack
     */
    fun canGoBack(root: NavNode): Boolean = BackOperations.canGoBack(root)

    /**
     * Get the current active destination.
     *
     * @see BackOperations.currentDestination
     */
    fun currentDestination(root: NavNode): NavDestination? =
        BackOperations.currentDestination(root)

    /**
     * Pop with intelligent tab handling.
     *
     * @see BackOperations.popWithTabBehavior
     */
    fun popWithTabBehavior(root: NavNode, isCompact: Boolean = true): BackResult =
        BackOperations.popWithTabBehavior(root, isCompact)

    /**
     * Check if back navigation is possible, considering root constraints.
     *
     * @see BackOperations.canHandleBackNavigation
     */
    fun canHandleBackNavigation(root: NavNode): Boolean =
        BackOperations.canHandleBackNavigation(root)
}
