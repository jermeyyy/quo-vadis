package com.jermey.quo.vadis.core.navigation.tree.operations

import com.jermey.quo.vadis.core.dsl.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.dsl.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.NavNode
import com.jermey.quo.vadis.core.navigation.PaneNode
import com.jermey.quo.vadis.core.navigation.ScreenNode
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.navigation.TabNode
import com.jermey.quo.vadis.core.navigation.activePathToLeaf
import com.jermey.quo.vadis.core.navigation.activeStack
import com.jermey.quo.vadis.core.navigation.findByKey
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.navigation.tree.operations.TabOperations.switchTab
import com.jermey.quo.vadis.core.navigation.tree.operations.TreeNodeOperations.replaceNode
import com.jermey.quo.vadis.core.navigation.tree.result.PushStrategy
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Push operations for the navigation tree.
 *
 * Handles all forward navigation:
 * - Simple push to active stack
 * - Push to specific stack by key
 * - Scope-aware push with tab switching
 * - Pane role routing
 * - Multi-destination push
 * - Clear and push patterns
 */
object PushOperations {

    @OptIn(ExperimentalUuidApi::class)
    private val keyGenerator: () -> String = { Uuid.random().toString().take(8) }

    /**
     * Push a destination onto the deepest active stack.
     *
     * Traverses the tree following the active path (active children of TabNodes,
     * active panes of PaneNodes) until reaching the deepest StackNode, then
     * appends a new ScreenNode with the given destination.
     *
     * ## Example
     *
     * ```kotlin
     * // Given a tree: StackNode -> TabNode -> StackNode(active)
     * val newTree = TreeMutator.push(root, ProfileDestination)
     * // ProfileDestination is pushed to the innermost active StackNode
     * ```
     *
     * @param root The root NavNode of the navigation tree
     * @param destination The destination to navigate to
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the destination pushed to the active stack
     * @throws IllegalStateException if no active stack is found
     */
    @OptIn(ExperimentalUuidApi::class)
    fun push(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = keyGenerator
    ): NavNode {
        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found in tree")

        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )

        val newStack = targetStack.copy(
            children = targetStack.children + newScreen
        )

        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Push a destination onto a specific stack identified by key.
     *
     * Unlike [push], this allows targeting a specific stack regardless of
     * whether it's currently active. Useful for pre-populating tab stacks
     * or background navigation.
     *
     * @param root The root NavNode of the navigation tree
     * @param stackKey Key of the target StackNode
     * @param destination The destination to push
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the destination pushed to the specified stack
     * @throws IllegalArgumentException if stackKey doesn't exist or isn't a StackNode
     */
    @OptIn(ExperimentalUuidApi::class)
    fun pushToStack(
        root: NavNode,
        stackKey: String,
        destination: NavDestination,
        generateKey: () -> String = keyGenerator
    ): NavNode {
        val targetNode = root.findByKey(stackKey)
            ?: throw IllegalArgumentException("Node with key '$stackKey' not found")

        require(targetNode is StackNode) {
            "Node '$stackKey' is ${targetNode::class.simpleName}, expected StackNode"
        }

        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = stackKey,
            destination = destination
        )

        val newStack = targetNode.copy(
            children = targetNode.children + newScreen
        )

        return replaceNode(root, stackKey, newStack)
    }

    /**
     * Push a destination with scope awareness, tab switching, and pane role routing.
     *
     * Navigation logic:
     * 1. First check if destination is in scope (using [scopeRegistry])
     * 2. For TabNode: check if destination already exists in any tab's stack
     *    - If found in another tab's stack, switch to that tab instead of pushing
     * 3. For PaneNode: check if destination has a specific pane role (using [paneRoleRegistry])
     *    - If destination has a role, push to that pane's stack
     * 4. If out of scope, delegate to parent stack
     *
     * ## Scope Resolution
     *
     * The method walks up from the deepest active stack, checking each container
     * (TabNode/PaneNode) with a [scopeKey][TabNode.scopeKey]. If the destination
     * is not in that scope (according to [scopeRegistry]), navigation targets
     * the parent stack instead.
     *
     * ## Tab Switching
     *
     * When navigating to a destination that already exists in a different tab's
     * stack (within the same TabNode), the navigator will switch to that tab
     * instead of creating a duplicate entry.
     *
     * ## Pane Role Routing
     *
     * When inside a PaneNode context, the [paneRoleRegistry] determines which
     * pane a destination belongs to. If the destination has a registered role,
     * navigation pushes to that pane's stack instead of the active stack.
     *
     * ## Example
     *
     * ```kotlin
     * // Given: Root -> Stack -> PaneNode(scopeKey="messages") -> Stack(PRIMARY, active)
     *
     * // ConversationDetail has paneRole=SECONDARY - pushes to SECONDARY pane's stack
     * TreeMutator.push(root, ConversationDetail("123"), scopeRegistry, paneRoleRegistry)
     *
     * // DetailDestination is NOT in scope - pushes to parent stack (above PaneNode)
     * TreeMutator.push(root, DetailDestination, scopeRegistry, paneRoleRegistry)
     * ```
     *
     * @param root The root NavNode of the navigation tree
     * @param destination The destination to navigate to
     * @param scopeRegistry Registry to check scope membership
     * @param paneRoleRegistry Registry to determine pane roles for destinations
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the destination pushed or tab switched appropriately
     * @throws IllegalStateException if no suitable stack is found
     */
    @OptIn(ExperimentalUuidApi::class)
    fun push(
        root: NavNode,
        destination: NavDestination,
        scopeRegistry: ScopeRegistry,
        paneRoleRegistry: PaneRoleRegistry = PaneRoleRegistry.Empty,
        generateKey: () -> String = keyGenerator
    ): NavNode {
        // If no scope registry (or Empty), use the simple push
        if (scopeRegistry === ScopeRegistry.Empty && paneRoleRegistry === PaneRoleRegistry.Empty) {
            return push(root, destination, generateKey)
        }

        return when (val strategy =
            determinePushStrategy(root, destination, scopeRegistry, paneRoleRegistry)) {
            is PushStrategy.PushToStack -> {
                pushToActiveStack(root, strategy.targetStack, destination, generateKey)
            }

            is PushStrategy.SwitchToTab -> {
                switchTab(root, strategy.tabNode.key, strategy.tabIndex)
            }

            is PushStrategy.PushToPaneStack -> {
                pushToPaneStack(root, strategy.paneNode, strategy.role, destination, generateKey)
            }

            is PushStrategy.PushOutOfScope -> {
                pushOutOfScope(root, strategy.parentStack, destination, generateKey)
            }
        }
    }

    /**
     * Clear a stack and push a single screen.
     *
     * Replaces all content in the deepest active stack with a single screen.
     * Useful for "navigate and clear" patterns like returning to a home screen.
     *
     * @param root The root NavNode of the navigation tree
     * @param destination The destination to navigate to
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with cleared stack containing only the new screen
     * @throws IllegalStateException if no active stack found
     */
    @OptIn(ExperimentalUuidApi::class)
    fun clearAndPush(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = keyGenerator
    ): NavNode {
        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found in tree")

        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )

        val newStack = targetStack.copy(children = listOf(newScreen))
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Clear a specific stack and push a single screen.
     *
     * @param root The root NavNode of the navigation tree
     * @param stackKey Key of the stack to clear
     * @param destination The destination to navigate to
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with cleared stack containing only the new screen
     * @throws IllegalArgumentException if stackKey not found or not a StackNode
     */
    @OptIn(ExperimentalUuidApi::class)
    fun clearStackAndPush(
        root: NavNode,
        stackKey: String,
        destination: NavDestination,
        generateKey: () -> String = keyGenerator
    ): NavNode {
        val targetNode = root.findByKey(stackKey)
            ?: throw IllegalArgumentException("Node with key '$stackKey' not found")

        require(targetNode is StackNode) {
            "Node '$stackKey' is ${targetNode::class.simpleName}, expected StackNode"
        }

        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = stackKey,
            destination = destination
        )

        val newStack = targetNode.copy(children = listOf(newScreen))
        return replaceNode(root, stackKey, newStack)
    }

    /**
     * Replace the currently active screen with a new destination.
     *
     * Replaces the top screen in the deepest active stack without adding to
     * the back stack. The stack size remains the same.
     *
     * @param root The root NavNode of the navigation tree
     * @param destination The destination to replace with
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the top screen replaced
     * @throws IllegalStateException if no active stack or stack is empty
     */
    @OptIn(ExperimentalUuidApi::class)
    fun replaceCurrent(
        root: NavNode,
        destination: NavDestination,
        generateKey: () -> String = keyGenerator
    ): NavNode {
        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found in tree")

        require(targetStack.children.isNotEmpty()) {
            "Cannot replace in empty stack"
        }

        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )

        val newChildren = targetStack.children.dropLast(1) + newScreen
        val newStack = targetStack.copy(children = newChildren)
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Push multiple destinations at once.
     *
     * Appends all destinations to the deepest active stack in order.
     * More efficient than calling push multiple times as the tree is
     * only rebuilt once.
     *
     * @param root The root NavNode of the navigation tree
     * @param destinations The destinations to push (in order, first = bottom)
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with all destinations pushed
     * @throws IllegalStateException if no active stack found
     */
    @OptIn(ExperimentalUuidApi::class)
    fun pushAll(
        root: NavNode,
        destinations: List<NavDestination>,
        generateKey: () -> String = keyGenerator
    ): NavNode {
        if (destinations.isEmpty()) return root

        val targetStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found in tree")

        val newScreens = destinations.map { destination ->
            ScreenNode(
                key = generateKey(),
                parentKey = targetStack.key,
                destination = destination
            )
        }

        val newStack = targetStack.copy(children = targetStack.children + newScreens)
        return replaceNode(root, targetStack.key, newStack)
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Determines the push strategy for a destination considering scope, tabs, and pane roles.
     *
     * Walks up from the deepest active stack, checking each container (TabNode/PaneNode)
     * to determine how the navigation should be handled.
     *
     * @param root The root NavNode of the navigation tree
     * @param destination The destination to check
     * @param scopeRegistry Registry for scope membership checks
     * @param paneRoleRegistry Registry for pane role lookups
     * @return The appropriate PushStrategy for this navigation
     * @throws IllegalStateException if no valid strategy can be determined
     */
    private fun determinePushStrategy(
        root: NavNode,
        destination: NavDestination,
        scopeRegistry: ScopeRegistry,
        paneRoleRegistry: PaneRoleRegistry = PaneRoleRegistry.Empty
    ): PushStrategy {
        val activeStack = root.activeStack()
            ?: throw IllegalStateException("No active stack found in tree")

        // Walk the active path to find containers
        val activePath = root.activePathToLeaf()

        // Check containers from deepest to shallowest
        for (node in activePath.reversed()) {
            when (node) {
                is StackNode -> {
                    // Check if this stack has a scope and if destination is out of scope
                    val stackScope = node.scopeKey
                    if (stackScope != null && !scopeRegistry.isInScope(stackScope, destination)) {
                        // Out of scope - find the stack that contains this StackNode
                        val parentKey = node.parentKey ?: continue
                        val parent = root.findByKey(parentKey)
                        if (parent is StackNode) {
                            return PushStrategy.PushOutOfScope(parent)
                        }
                    }
                }

                is TabNode -> {
                    val scopeKey = node.scopeKey

                    // First, check if destination is in scope
                    if (scopeKey != null && !scopeRegistry.isInScope(scopeKey, destination)) {
                        // Out of scope - find the stack that contains this TabNode
                        val parentKey = node.parentKey ?: continue
                        val parent = root.findByKey(parentKey)
                        if (parent is StackNode) {
                            return PushStrategy.PushOutOfScope(parent)
                        }
                        continue
                    }

                    // Destination is in scope - check if it exists in any tab's stack
                    val existingTabIndex = findTabWithDestination(node, destination)
                    if (existingTabIndex != null && existingTabIndex != node.activeStackIndex) {
                        // Destination exists in a different tab - switch to it
                        return PushStrategy.SwitchToTab(node, existingTabIndex)
                    }

                    // Destination is in scope but not in another tab - push to active stack
                    // Continue checking other containers up the tree
                }

                is PaneNode -> {
                    val scopeKey = node.scopeKey
                    if (scopeKey != null && !scopeRegistry.isInScope(scopeKey, destination)) {
                        // Out of scope - find the stack that contains this PaneNode
                        val parentKey = node.parentKey ?: continue
                        val parent = root.findByKey(parentKey)
                        if (parent is StackNode) {
                            return PushStrategy.PushOutOfScope(parent)
                        }
                        continue
                    }

                    // Destination is in scope - check if it has a specific pane role
                    if (scopeKey != null) {
                        val targetRole = paneRoleRegistry.getPaneRole(scopeKey, destination)
                        if (targetRole != null && node.paneConfigurations.containsKey(targetRole)) {
                            // Destination has a registered pane role - push to that pane's stack
                            return PushStrategy.PushToPaneStack(node, targetRole)
                        }
                    }
                }

                else -> { /* Continue checking */
                }
            }
        }

        // All containers allow this destination, push to deepest active stack
        return PushStrategy.PushToStack(activeStack)
    }

    /**
     * Finds the tab index containing a destination with matching type.
     *
     * Searches through all stacks in a TabNode to find one that contains
     * a ScreenNode with a destination of the same type (class) as the target.
     *
     * @param tabNode The TabNode to search within
     * @param destination The destination to find
     * @return The tab index if found, null otherwise
     */
    private fun findTabWithDestination(tabNode: TabNode, destination: NavDestination): Int? {
        val destinationClass = destination::class
        tabNode.stacks.forEachIndexed { index, stack ->
            // Check if this stack contains a screen with matching destination type
            val hasDestination = stack.children.any { child ->
                child is ScreenNode && child.destination::class == destinationClass
            }
            if (hasDestination) {
                return index
            }
        }
        return null
    }

    /**
     * Push directly to a specific stack (in-scope case).
     *
     * @param root The root NavNode of the navigation tree
     * @param targetStack The stack to push to
     * @param destination The destination to push
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the destination pushed to the target stack
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun pushToActiveStack(
        root: NavNode,
        targetStack: StackNode,
        destination: NavDestination,
        generateKey: () -> String
    ): NavNode {
        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )

        val newStack = targetStack.copy(
            children = targetStack.children + newScreen
        )

        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Push a destination outside the current container's scope.
     *
     * Creates a new ScreenNode as a child of the parent stack, effectively
     * navigating "on top of" the container. This preserves the container
     * (and its state) for back navigation.
     *
     * @param root The root NavNode of the navigation tree
     * @param parentStack The stack to push to (parent of the scoped container)
     * @param destination The destination to push
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the destination pushed to the parent stack
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun pushOutOfScope(
        root: NavNode,
        parentStack: StackNode,
        destination: NavDestination,
        generateKey: () -> String
    ): NavNode {
        val screenKey = generateKey()

        // Create new screen as direct child of parent stack
        val newScreen = ScreenNode(
            key = screenKey,
            parentKey = parentStack.key,
            destination = destination
        )

        // Add new screen to parent stack's children
        val updatedParentStack = parentStack.copy(
            children = parentStack.children + newScreen
        )

        return replaceNode(root, parentStack.key, updatedParentStack)
    }

    /**
     * Push a destination to a specific pane's stack within a PaneNode.
     *
     * Finds the stack for the given pane role and pushes the destination there,
     * optionally switching the active pane to the target role.
     *
     * @param root The root NavNode of the navigation tree
     * @param paneNode The PaneNode containing the target pane
     * @param role The pane role to push to
     * @param destination The destination to push
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the destination pushed to the pane's stack
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun pushToPaneStack(
        root: NavNode,
        paneNode: PaneNode,
        role: PaneRole,
        destination: NavDestination,
        generateKey: () -> String
    ): NavNode {
        val paneConfig = paneNode.paneConfigurations[role]
            ?: return root // Role not configured, return unchanged

        val paneStack = paneConfig.content as? StackNode
            ?: return root // Content is not a StackNode, return unchanged

        // Create new screen node
        val screenKey = generateKey()
        val newScreen = ScreenNode(
            key = screenKey,
            parentKey = paneStack.key,
            destination = destination
        )

        // Update the pane's stack with the new screen
        val updatedStack = paneStack.copy(
            children = paneStack.children + newScreen
        )

        // Update the pane configuration with the new stack
        val updatedPaneConfig = paneConfig.copy(content = updatedStack)
        val updatedPaneConfigurations = paneNode.paneConfigurations.toMutableMap()
        updatedPaneConfigurations[role] = updatedPaneConfig

        // Update the PaneNode with new configurations AND switch active pane
        val updatedPaneNode = paneNode.copy(
            paneConfigurations = updatedPaneConfigurations,
            activePaneRole = role // Switch focus to the target pane
        )

        return replaceNode(root, paneNode.key, updatedPaneNode)
    }
}
