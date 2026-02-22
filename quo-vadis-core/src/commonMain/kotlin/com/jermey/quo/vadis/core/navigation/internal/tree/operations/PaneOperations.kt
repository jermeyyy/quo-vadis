package com.jermey.quo.vadis.core.navigation.internal.tree.operations

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.tree.operations.PopOperations.pop
import com.jermey.quo.vadis.core.navigation.internal.tree.operations.TreeNodeOperations.replaceNode
import com.jermey.quo.vadis.core.navigation.internal.tree.result.PopResult
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.activePathToLeaf
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.node.findByKey
import com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Pane navigation operations for PaneNode.
 *
 * Handles all pane-related navigation:
 * - Navigate to specific pane with destination
 * - Switch active pane
 * - Pop from pane with various behaviors
 * - Adaptive pop based on layout configuration
 * - Configure/remove pane configurations
 */
@InternalQuoVadisApi
object PaneOperations {

    /**
     * Navigates to a destination within the specified pane role.
     *
     * If the pane contains a StackNode (directly or as content), pushes the
     * destination onto that stack. Optionally switches [PaneNode.activePaneRole]
     * to the target pane.
     *
     * @param root The root NavNode of the navigation tree
     * @param nodeKey Key of the PaneNode to mutate
     * @param role Target pane role
     * @param destination Destination to navigate to
     * @param switchFocus Whether to also set activePaneRole to this role
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with navigation applied
     * @throws IllegalArgumentException if nodeKey not found or role not configured
     */
    @OptIn(ExperimentalUuidApi::class)
    fun navigateToPane(
        root: NavNode,
        nodeKey: NodeKey,
        role: PaneRole,
        destination: NavDestination,
        switchFocus: Boolean = true,
        generateKey: NavKeyGenerator = { NodeKey(Uuid.random().toString().take(8)) }
    ): NavNode {
        val paneNode = root.findByKey(nodeKey) as? PaneNode
            ?: throw IllegalArgumentException("PaneNode with key '$nodeKey' not found")

        val paneConfig = paneNode.paneConfigurations[role]
            ?: throw IllegalArgumentException("Pane role $role not configured in PaneNode '$nodeKey'")

        // Find or create a stack in this pane's content
        val targetStack = when (val paneContent = paneConfig.content) {
            is StackNode -> paneContent
            else -> paneContent.activeStack()
                ?: throw IllegalStateException("No stack found in pane $role content")
        }

        // Create new screen
        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack.key,
            destination = destination
        )

        // Update the stack
        val newStack = targetStack.copy(children = targetStack.children + newScreen)

        // Replace the stack in the tree
        var result = replaceNode(root, targetStack.key, newStack)

        // Optionally switch focus
        if (switchFocus && paneNode.activePaneRole != role) {
            result = switchActivePane(result, nodeKey, role)
        }

        return result
    }

    /**
     * Switches the active pane role without navigating.
     *
     * This affects which pane receives subsequent navigation operations
     * and may influence visibility on compact screens.
     *
     * @param root The root NavNode of the navigation tree
     * @param nodeKey Key of the PaneNode to mutate
     * @param role New active pane role (must exist in paneConfigurations)
     * @return New tree with active pane switched
     * @throws IllegalArgumentException if nodeKey not found or role not configured
     */
    fun switchActivePane(
        root: NavNode,
        nodeKey: NodeKey,
        role: PaneRole
    ): NavNode {
        val paneNode = root.findByKey(nodeKey) as? PaneNode
            ?: throw IllegalArgumentException("PaneNode with key '$nodeKey' not found")

        require(paneNode.paneConfigurations.containsKey(role)) {
            "Pane role $role not configured in PaneNode '$nodeKey'"
        }

        if (paneNode.activePaneRole == role) return root

        val newPaneNode = paneNode.copy(activePaneRole = role)
        return replaceNode(root, nodeKey, newPaneNode)
    }

    /**
     * Pops the top entry from the specified pane's stack.
     *
     * @param root The root NavNode of the navigation tree
     * @param nodeKey Key of the PaneNode to mutate
     * @param role Pane role to pop from
     * @return Updated NavNode, or null if the pane's stack is empty
     */
    fun popPane(
        root: NavNode,
        nodeKey: NodeKey,
        role: PaneRole
    ): NavNode? {
        val paneNode = root.findByKey(nodeKey) as? PaneNode
            ?: throw IllegalArgumentException("PaneNode with key '$nodeKey' not found")

        val paneConfig = paneNode.paneConfigurations[role]
            ?: throw IllegalArgumentException("Pane role $role not configured in PaneNode '$nodeKey'")

        // Find the stack in this pane
        val targetStack = when (val paneContent = paneConfig.content) {
            is StackNode -> paneContent
            else -> paneContent.activeStack() ?: return null
        }

        // Cannot pop from empty stack
        if (targetStack.isEmpty || targetStack.children.size <= 1) return null

        val newChildren = targetStack.children.dropLast(1)
        val newStack = targetStack.copy(children = newChildren)
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Pop with respect to PaneBackBehavior.
     *
     * This operation considers the [PaneBackBehavior] setting of the active
     * PaneNode (if any) and returns a [PopResult] indicating what action
     * was taken or should be taken.
     *
     * @param root The root NavNode of the navigation tree
     * @return PopResult indicating the outcome
     */
    fun popWithPaneBehavior(root: NavNode): PopResult {
        // Find if we're in a PaneNode context
        val activePath = root.activePathToLeaf()
        val paneNode = activePath.filterIsInstance<PaneNode>().lastOrNull()

        // If no PaneNode, just do a regular pop
        if (paneNode == null) {
            return pop(root)?.let { PopResult.Popped(it) } ?: PopResult.CannotPop
        }

        val activeStack = root.activeStack() ?: return PopResult.CannotPop

        // If stack has content to pop, pop it
        if (activeStack.children.size > 1) {
            return pop(root)?.let { PopResult.Popped(it) } ?: PopResult.CannotPop
        }

        // Stack would become empty or is at root - apply PaneBackBehavior
        return when (paneNode.backBehavior) {
            PaneBackBehavior.PopLatest -> {
                // Simple pop - if we can't, we can't
                pop(root)?.let { PopResult.Popped(it) } ?: PopResult.CannotPop
            }

            PaneBackBehavior.PopUntilScaffoldValueChange -> {
                // Try to switch to Primary pane if we're in a different pane
                if (paneNode.activePaneRole != PaneRole.Primary) {
                    val newRoot = switchActivePane(root, paneNode.key, PaneRole.Primary)
                    PopResult.Popped(newRoot)
                } else {
                    // Already on Primary, renderer needs to handle scaffold change
                    PopResult.RequiresScaffoldChange
                }
            }

            PaneBackBehavior.PopUntilCurrentDestinationChange -> {
                // Try switching to a different pane with content
                val alternativePanes = paneNode.configuredRoles
                    .filter { it != paneNode.activePaneRole }
                    .filter { role ->
                        val content = paneNode.paneContent(role)
                        content?.activeStack()?.children?.isNotEmpty() == true
                    }

                if (alternativePanes.isNotEmpty()) {
                    val newRole = alternativePanes.first()
                    val newRoot = switchActivePane(root, paneNode.key, newRole)
                    PopResult.Popped(newRoot)
                } else {
                    PopResult.PaneEmpty(paneNode.activePaneRole)
                }
            }

            PaneBackBehavior.PopUntilContentChange -> {
                // Pop from any pane that has content
                val panesWithContent = paneNode.configuredRoles.filter { role ->
                    val content = paneNode.paneContent(role)
                    val stack = content?.activeStack()
                    stack != null && stack.children.size > 1
                }

                if (panesWithContent.isNotEmpty()) {
                    // Pop from the first pane with content (preferring active)
                    val targetRole = if (paneNode.activePaneRole in panesWithContent) {
                        paneNode.activePaneRole
                    } else {
                        panesWithContent.first()
                    }
                    val poppedResult = popPane(root, paneNode.key, targetRole)
                    if (poppedResult != null) {
                        // After popping, check if the target pane returned to root state
                        // If so, and it's not PRIMARY, clear the pane and switch to PRIMARY
                        val updatedPaneNode = poppedResult.findByKey(paneNode.key) as? PaneNode
                        if (updatedPaneNode != null && targetRole != PaneRole.Primary) {
                            val targetStack = updatedPaneNode.paneContent(targetRole)?.activeStack()
                            if (targetStack != null && targetStack.children.size <= 1) {
                                // Target pane is now at root - clear it and switch to PRIMARY
                                var newState =
                                    clearPaneStack(poppedResult, paneNode.key, targetRole)
                                newState =
                                    switchActivePane(newState, paneNode.key, PaneRole.Primary)
                                return PopResult.Popped(newState)
                            }
                        }
                        PopResult.Popped(poppedResult)
                    } else {
                        PopResult.PaneEmpty(paneNode.activePaneRole)
                    }
                } else {
                    // No panes have content to pop
                    // If we're on a non-PRIMARY pane, clear it and switch to PRIMARY
                    if (paneNode.activePaneRole != PaneRole.Primary) {
                        var newRoot = clearPaneStack(root, paneNode.key, paneNode.activePaneRole)
                        newRoot = switchActivePane(newRoot, paneNode.key, PaneRole.Primary)
                        PopResult.Popped(newRoot)
                    } else {
                        PopResult.PaneEmpty(paneNode.activePaneRole)
                    }
                }
            }
        }
    }

    /**
     * Pop from a pane with awareness of window size.
     *
     * In compact mode (single pane visible), back behaves like a simple stack,
     * ignoring the configured [PaneBackBehavior]. This ensures predictable
     * back navigation when only one pane is shown.
     *
     * In expanded mode (multiple panes visible), the configured [PaneBackBehavior]
     * applies, allowing for sophisticated multi-pane back behaviors.
     *
     * @param root The root NavNode
     * @param isCompact Whether currently in compact/single-pane mode
     * @return PopResult indicating the outcome
     */
    fun popPaneAdaptive(root: NavNode, isCompact: Boolean): PopResult {
        // Find the active pane node if any
        val activePath = root.activePathToLeaf()
        val paneNode = activePath.filterIsInstance<PaneNode>().firstOrNull()

        if (paneNode == null) {
            // No pane node - use standard pop
            val result = pop(root)
            return if (result != null) PopResult.Popped(result) else PopResult.CannotPop
        }

        if (isCompact) {
            // In compact mode, treat as simple stack
            // Just pop from the active stack, ignoring pane configuration
            return popFromActivePane(root, paneNode)
        }

        // In expanded mode, use configured behavior
        return popWithPaneBehavior(root)
    }

    /**
     * Simple pop from the active pane's stack.
     *
     * This is used in compact mode where we want simple stack-like
     * back navigation regardless of pane configuration.
     *
     * @param root The root NavNode
     * @param paneNode The PaneNode to pop from
     * @return PopResult indicating the outcome
     */
    private fun popFromActivePane(root: NavNode, paneNode: PaneNode): PopResult {
        val activePaneRole = paneNode.activePaneRole
        val activeStack = paneNode.activePaneContent?.activeStack()

        if (activeStack == null) {
            return PopResult.PaneEmpty(activePaneRole)
        }

        if (activeStack.children.size <= 1) {
            // Stack would become empty or is at root state
            return if (activePaneRole != PaneRole.Primary) {
                // Non-PRIMARY pane at root: clear this pane's stack and switch to PRIMARY
                // This ensures back navigation properly clears the secondary pane
                var newState = clearPaneStack(root, paneNode.key, activePaneRole)
                newState = switchActivePane(newState, paneNode.key, PaneRole.Primary)
                PopResult.Popped(newState)
            } else {
                PopResult.PaneEmpty(activePaneRole)
            }
        }

        // Pop from the active stack
        val newChildren = activeStack.children.dropLast(1)
        val newStack = activeStack.copy(children = newChildren)
        val newState = replaceNode(root, activeStack.key, newStack)
        return PopResult.Popped(newState)
    }

    /**
     * Clears a pane's stack, removing all children.
     *
     * @param root The root NavNode
     * @param paneNodeKey Key of the PaneNode
     * @param role The pane role to clear
     * @return New tree with the pane's stack cleared
     */
    private fun clearPaneStack(root: NavNode, paneNodeKey: NodeKey, role: PaneRole): NavNode {
        val paneNode = root.findByKey(paneNodeKey) as? PaneNode ?: return root
        val paneConfig = paneNode.paneConfigurations[role] ?: return root
        val stackNode = paneConfig.content as? StackNode ?: return root

        // Clear the stack by removing all children
        val clearedStack = stackNode.copy(children = emptyList())
        val newConfig = paneConfig.copy(content = clearedStack)
        val newConfigurations = paneNode.paneConfigurations + (role to newConfig)
        val newPaneNode = paneNode.copy(paneConfigurations = newConfigurations)
        return replaceNode(root, paneNodeKey, newPaneNode)
    }

    /**
     * Sets or updates the configuration for a pane role.
     *
     * @param root The root NavNode of the navigation tree
     * @param nodeKey Key of the PaneNode to mutate
     * @param role Pane role to configure
     * @param config New pane configuration
     * @return New tree with configuration updated
     * @throws IllegalArgumentException if nodeKey not found
     */
    fun setPaneConfiguration(
        root: NavNode,
        nodeKey: NodeKey,
        role: PaneRole,
        config: PaneConfiguration
    ): NavNode {
        val paneNode = root.findByKey(nodeKey) as? PaneNode
            ?: throw IllegalArgumentException("PaneNode with key '$nodeKey' not found")

        val newConfigurations = paneNode.paneConfigurations + (role to config)
        val newPaneNode = paneNode.copy(paneConfigurations = newConfigurations)
        return replaceNode(root, nodeKey, newPaneNode)
    }

    /**
     * Removes a pane configuration.
     *
     * @param root The root NavNode of the navigation tree
     * @param nodeKey Key of the PaneNode to mutate
     * @param role Pane role to remove (cannot be Primary)
     * @return New tree with configuration removed
     * @throws IllegalArgumentException if trying to remove Primary pane or nodeKey not found
     */
    fun removePaneConfiguration(
        root: NavNode,
        nodeKey: NodeKey,
        role: PaneRole
    ): NavNode {
        require(role != PaneRole.Primary) {
            "Cannot remove Primary pane - it is required"
        }

        val paneNode = root.findByKey(nodeKey) as? PaneNode
            ?: throw IllegalArgumentException("PaneNode with key '$nodeKey' not found")

        val newConfigurations = paneNode.paneConfigurations - role

        // If active role is being removed, switch to Primary
        val newActiveRole = if (paneNode.activePaneRole == role) {
            PaneRole.Primary
        } else {
            paneNode.activePaneRole
        }

        val newPaneNode = paneNode.copy(
            paneConfigurations = newConfigurations,
            activePaneRole = newActiveRole
        )
        return replaceNode(root, nodeKey, newPaneNode)
    }
}
