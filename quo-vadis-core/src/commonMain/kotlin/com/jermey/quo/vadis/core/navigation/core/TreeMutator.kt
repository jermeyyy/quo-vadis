package com.jermey.quo.vadis.core.navigation.core

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Pure functional operations for manipulating the NavNode tree.
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
 */
object TreeMutator {

    // =========================================================================
    // Configuration Types
    // =========================================================================

    /**
     * Configures behavior when a StackNode becomes empty after pop.
     */
    enum class PopBehavior {
        /**
         * Remove the empty stack from parent (cascading pop).
         * This continues popping until a non-empty container is found.
         */
        CASCADE,

        /**
         * Preserve the empty stack in place.
         * The stack remains but with no children.
         */
        PRESERVE_EMPTY
    }

    /**
     * Result of a pop operation that respects PaneBackBehavior.
     */
    sealed class PopResult {
        /** Successfully popped within current pane */
        data class Popped(val newState: NavNode) : PopResult()

        /** Pane is empty, behavior depends on PaneBackBehavior */
        data class PaneEmpty(val paneRole: PaneRole) : PopResult()

        /** Cannot pop - would leave tree in invalid state */
        data object CannotPop : PopResult()

        /** Back behavior requires scaffold/visual change (renderer must handle) */
        data object RequiresScaffoldChange : PopResult()
    }

    // =========================================================================
    // PUSH OPERATIONS
    // =========================================================================

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
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
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
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
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

    // =========================================================================
    // POP OPERATIONS
    // =========================================================================

    /**
     * Pop the active screen from the deepest active stack.
     *
     * Removes the last child from the deepest active StackNode. The [behavior]
     * parameter controls what happens if the stack becomes empty after popping.
     *
     * ## Cascade Behavior
     *
     * With [PopBehavior.CASCADE], if popping leaves a stack empty:
     * - The empty stack is removed from its parent
     * - This may recursively propagate up the tree
     * - Returns null if popping would leave the root empty
     *
     * ## Example
     *
     * ```kotlin
     * val result = TreeMutator.pop(root, PopBehavior.PRESERVE_EMPTY)
     * if (result == null) {
     *     // Cannot pop - at root or tree is empty
     * }
     * ```
     *
     * @param root The root NavNode of the navigation tree
     * @param behavior How to handle an empty stack after popping
     * @return New tree with the top screen popped, or null if cannot pop
     */
    fun pop(
        root: NavNode,
        behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY
    ): NavNode? {
        val targetStack = root.activeStack() ?: return null

        // Cannot pop from empty stack
        if (targetStack.isEmpty) return null

        // Pop the last child
        val newChildren = targetStack.children.dropLast(1)

        // Handle empty stack based on behavior
        if (newChildren.isEmpty()) {
            return handleEmptyStackPop(root, targetStack, behavior)
        }

        val newStack = targetStack.copy(children = newChildren)
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Pop all screens from the active stack until the predicate matches.
     *
     * Searches from the back (most recent) to the front of the active stack
     * for a node matching the predicate. All nodes after the match are removed.
     *
     * @param root The root NavNode of the navigation tree
     * @param inclusive If true, also removes the matching node
     * @param predicate Function to identify the target node
     * @return New tree with nodes popped, or original tree if no match found
     */
    fun popTo(
        root: NavNode,
        inclusive: Boolean = false,
        predicate: (NavNode) -> Boolean
    ): NavNode {
        val targetStack = root.activeStack() ?: return root

        // Find the index of the matching node (searching from back to front)
        val matchIndex = targetStack.children.indexOfLast { predicate(it) }
        if (matchIndex == -1) return root

        // Calculate how many to keep
        val keepCount = if (inclusive) matchIndex else matchIndex + 1
        if (keepCount == 0) return root // Would make stack empty

        val newChildren = targetStack.children.take(keepCount)
        val newStack = targetStack.copy(children = newChildren)
        return replaceNode(root, targetStack.key, newStack)
    }

    /**
     * Pop to a screen with the given route.
     *
     * Convenience wrapper around [popTo] that matches by destination route.
     * Compares the route against [Destination.toString] or a route property
     * if available.
     *
     * @param root The root NavNode of the navigation tree
     * @param route The route string to match
     * @param inclusive If true, also removes the matching screen
     * @return New tree with nodes popped, or original tree if route not found
     */
    fun popToRoute(
        root: NavNode,
        route: String,
        inclusive: Boolean = false
    ): NavNode {
        return popTo(root, inclusive) { node ->
            node is ScreenNode && node.destination.toString() == route
        }
    }

    /**
     * Pop to a screen with destination matching the given type.
     *
     * @param root The root NavNode of the navigation tree
     * @param inclusive If true, also removes the matching screen
     * @return New tree with nodes popped, or original tree if not found
     */
    inline fun <reified D : Destination> popToDestination(
        root: NavNode,
        inclusive: Boolean = false
    ): NavNode {
        return popTo(root, inclusive) { node ->
            node is ScreenNode && node.destination is D
        }
    }

    /**
     * Handle pop when the active stack would become empty.
     */
    private fun handleEmptyStackPop(
        root: NavNode,
        emptyStack: StackNode,
        behavior: PopBehavior
    ): NavNode? {
        return when (behavior) {
            PopBehavior.PRESERVE_EMPTY -> {
                // Just update to empty stack
                val newStack = emptyStack.copy(children = emptyList())
                replaceNode(root, emptyStack.key, newStack)
            }
            PopBehavior.CASCADE -> {
                // If this is the root, cannot cascade further
                if (emptyStack.parentKey == null) return null

                // Try to remove this stack from its parent
                val parent = root.findByKey(emptyStack.parentKey)
                    ?: return null // Parent not found, cannot cascade

                when (parent) {
                    is TabNode -> {
                        // Cannot remove a tab's stack - that would break the tab structure
                        // Instead, just preserve the empty stack
                        val newStack = emptyStack.copy(children = emptyList())
                        replaceNode(root, emptyStack.key, newStack)
                    }
                    is StackNode -> {
                        // Remove the empty stack from parent's children
                        removeNode(root, emptyStack.key)
                    }
                    is PaneNode -> {
                        // Cannot remove a pane's content - preserve empty
                        val newStack = emptyStack.copy(children = emptyList())
                        replaceNode(root, emptyStack.key, newStack)
                    }
                    is ScreenNode -> {
                        // ScreenNode cannot be a parent - this shouldn't happen
                        null
                    }
                }
            }
        }
    }

    // =========================================================================
    // TAB OPERATIONS
    // =========================================================================

    /**
     * Switch to a different tab in a TabNode.
     *
     * Updates the [TabNode.activeStackIndex] to the specified index.
     * The stacks themselves are unchanged - each tab preserves its history.
     *
     * @param root The root NavNode of the navigation tree
     * @param tabNodeKey Key of the TabNode to modify
     * @param newIndex The tab index to switch to (0-based)
     * @return New tree with the tab switched
     * @throws IllegalArgumentException if tabNodeKey not found or not a TabNode
     * @throws IndexOutOfBoundsException if newIndex is out of bounds
     */
    fun switchTab(
        root: NavNode,
        tabNodeKey: String,
        newIndex: Int
    ): NavNode {
        val tabNode = root.findByKey(tabNodeKey) as? TabNode
            ?: throw IllegalArgumentException("TabNode with key '$tabNodeKey' not found")

        require(newIndex in 0 until tabNode.tabCount) {
            "Tab index $newIndex out of bounds for ${tabNode.tabCount} tabs"
        }

        if (tabNode.activeStackIndex == newIndex) return root

        val newTabNode = tabNode.copy(activeStackIndex = newIndex)
        return replaceNode(root, tabNodeKey, newTabNode)
    }

    /**
     * Switch to a different tab in the first TabNode found in the active path.
     *
     * Traverses the active path to find the first TabNode, then switches
     * to the specified tab index. Useful when you don't know or don't want
     * to specify the exact TabNode key.
     *
     * @param root The root NavNode of the navigation tree
     * @param newIndex The tab index to switch to (0-based)
     * @return New tree with the tab switched
     * @throws IllegalStateException if no TabNode found in active path
     */
    fun switchActiveTab(root: NavNode, newIndex: Int): NavNode {
        // Find the first TabNode in the active path
        val activePath = root.activePathToLeaf()
        val tabNode = activePath.filterIsInstance<TabNode>().firstOrNull()
            ?: throw IllegalStateException("No TabNode found in active path")

        return switchTab(root, tabNode.key, newIndex)
    }

    // =========================================================================
    // PANE OPERATIONS (from CORE-002 Impact Notes)
    // =========================================================================

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
        nodeKey: String,
        role: PaneRole,
        destination: Destination,
        switchFocus: Boolean = true,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
    ): NavNode {
        val paneNode = root.findByKey(nodeKey) as? PaneNode
            ?: throw IllegalArgumentException("PaneNode with key '$nodeKey' not found")

        val paneConfig = paneNode.paneConfigurations[role]
            ?: throw IllegalArgumentException("Pane role $role not configured in PaneNode '$nodeKey'")

        // Find or create a stack in this pane's content
        val paneContent = paneConfig.content
        val targetStack = when (paneContent) {
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
        nodeKey: String,
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
        nodeKey: String,
        role: PaneRole
    ): NavNode? {
        val paneNode = root.findByKey(nodeKey) as? PaneNode
            ?: throw IllegalArgumentException("PaneNode with key '$nodeKey' not found")

        val paneConfig = paneNode.paneConfigurations[role]
            ?: throw IllegalArgumentException("Pane role $role not configured in PaneNode '$nodeKey'")

        // Find the stack in this pane
        val paneContent = paneConfig.content
        val targetStack = when (paneContent) {
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
                    popPane(root, paneNode.key, targetRole)?.let { PopResult.Popped(it) }
                        ?: PopResult.PaneEmpty(paneNode.activePaneRole)
                } else {
                    PopResult.PaneEmpty(paneNode.activePaneRole)
                }
            }
        }
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
        nodeKey: String,
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
        nodeKey: String,
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

    // =========================================================================
    // UTILITY OPERATIONS
    // =========================================================================

    /**
     * Replace a node in the tree by key.
     *
     * Performs a depth-first search for the node with [targetKey], then
     * rebuilds the tree path with [newNode] in place of the old node.
     * Unchanged subtrees are reused by reference (structural sharing).
     *
     * @param root The root NavNode of the navigation tree
     * @param targetKey Key of the node to replace
     * @param newNode The replacement node
     * @return New tree with the node replaced
     * @throws IllegalArgumentException if targetKey not found
     */
    fun replaceNode(root: NavNode, targetKey: String, newNode: NavNode): NavNode {
        if (root.key == targetKey) return newNode

        return when (root) {
            is ScreenNode -> {
                throw IllegalArgumentException("Node with key '$targetKey' not found")
            }
            is StackNode -> {
                val newChildren = root.children.map { child ->
                    if (child.key == targetKey) newNode
                    else if (child.findByKey(targetKey) != null) replaceNode(child, targetKey, newNode)
                    else child
                }
                if (newChildren == root.children) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(children = newChildren)
            }
            is TabNode -> {
                val newStacks = root.stacks.map { stack ->
                    if (stack.key == targetKey) newNode as StackNode
                    else if (stack.findByKey(targetKey) != null) {
                        replaceNode(stack, targetKey, newNode) as StackNode
                    } else stack
                }
                if (newStacks == root.stacks) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(stacks = newStacks)
            }
            is PaneNode -> {
                val newConfigurations = root.paneConfigurations.mapValues { (_, config) ->
                    if (config.content.key == targetKey) {
                        config.copy(content = newNode)
                    } else if (config.content.findByKey(targetKey) != null) {
                        config.copy(content = replaceNode(config.content, targetKey, newNode))
                    } else {
                        config
                    }
                }
                if (newConfigurations == root.paneConfigurations) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(paneConfigurations = newConfigurations)
            }
        }
    }

    /**
     * Remove a node from the tree by key.
     *
     * The behavior depends on the parent node type:
     * - From StackNode: Removes child from children list
     * - From TabNode: Cannot remove stacks (throws exception)
     * - From PaneNode: Cannot remove panes (throws exception)
     *
     * @param root The root NavNode of the navigation tree
     * @param targetKey Key of the node to remove
     * @return New tree with the node removed, or null if root itself would be removed
     * @throws IllegalArgumentException if targetKey not found or removal not allowed
     */
    fun removeNode(root: NavNode, targetKey: String): NavNode? {
        // Cannot remove the root
        if (root.key == targetKey) return null

        return when (root) {
            is ScreenNode -> {
                throw IllegalArgumentException("Node with key '$targetKey' not found")
            }
            is StackNode -> {
                // Check if any child has this key
                val childIndex = root.children.indexOfFirst { it.key == targetKey }
                if (childIndex != -1) {
                    val newChildren = root.children.toMutableList().apply { removeAt(childIndex) }
                    return root.copy(children = newChildren)
                }

                // Search recursively
                val newChildren = root.children.map { child ->
                    if (child.findByKey(targetKey) != null) {
                        removeNode(child, targetKey)
                            ?: throw IllegalArgumentException("Cannot remove root of subtree '$targetKey'")
                    } else child
                }
                if (newChildren == root.children) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(children = newChildren)
            }
            is TabNode -> {
                // Cannot remove stacks directly from TabNode
                if (root.stacks.any { it.key == targetKey }) {
                    throw IllegalArgumentException("Cannot remove stack '$targetKey' from TabNode - use switchTab instead")
                }

                val newStacks = root.stacks.map { stack ->
                    if (stack.findByKey(targetKey) != null) {
                        removeNode(stack, targetKey) as? StackNode
                            ?: throw IllegalArgumentException("Cannot remove root of subtree '$targetKey'")
                    } else stack
                }
                if (newStacks == root.stacks) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(stacks = newStacks)
            }
            is PaneNode -> {
                // Cannot remove pane configurations directly
                if (root.paneConfigurations.values.any { it.content.key == targetKey }) {
                    throw IllegalArgumentException("Cannot remove pane content '$targetKey' directly - use removePaneConfiguration instead")
                }

                val newConfigurations = root.paneConfigurations.mapValues { (_, config) ->
                    if (config.content.findByKey(targetKey) != null) {
                        val newContent = removeNode(config.content, targetKey)
                            ?: throw IllegalArgumentException("Cannot remove root of subtree '$targetKey'")
                        config.copy(content = newContent)
                    } else {
                        config
                    }
                }
                if (newConfigurations == root.paneConfigurations) {
                    throw IllegalArgumentException("Node with key '$targetKey' not found")
                }
                root.copy(paneConfigurations = newConfigurations)
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
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
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
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
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
        destination: Destination,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
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
        destinations: List<Destination>,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
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

    /**
     * Check if back navigation is possible from the current state.
     *
     * Returns true if there is at least one screen that can be popped
     * from any active stack in the tree.
     *
     * @param root The root NavNode of the navigation tree
     * @return true if back navigation is possible
     */
    fun canGoBack(root: NavNode): Boolean {
        val activeStack = root.activeStack() ?: return false
        return activeStack.canGoBack
    }

    /**
     * Get the current active destination.
     *
     * @param root The root NavNode of the navigation tree
     * @return The current active Destination, or null if tree is empty
     */
    fun currentDestination(root: NavNode): Destination? {
        return root.activeLeaf()?.destination
    }
}
