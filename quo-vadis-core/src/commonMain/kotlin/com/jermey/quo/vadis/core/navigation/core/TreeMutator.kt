package com.jermey.quo.vadis.core.navigation.core

import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.core.TreeMutator.canGoBack
import com.jermey.quo.vadis.core.navigation.core.TreeMutator.popTo
import com.jermey.quo.vadis.core.navigation.core.TreeMutator.push
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

    /**
     * Result of a tree-aware back operation.
     */
    sealed class BackResult {
        /** Back was handled, new tree state returned */
        data class Handled(val newState: NavNode) : BackResult()

        /** Back should be delegated to system (e.g., close app) */
        data object DelegateToSystem : BackResult()

        /** Back could not be handled (internal error) */
        data object CannotHandle : BackResult()
    }

    /**
     * Result of a push operation with tab awareness.
     *
     * Used internally to determine how a push should be handled when
     * navigating within a TabNode context.
     */
    sealed class PushStrategy {
        /** Push to the specified stack normally */
        data class PushToStack(val targetStack: StackNode) : PushStrategy()

        /** Switch to an existing tab that contains the destination */
        data class SwitchToTab(val tabNode: TabNode, val tabIndex: Int) : PushStrategy()

        /** Destination is out of scope - push to parent stack */
        data class PushOutOfScope(val parentStack: StackNode) : PushStrategy()
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
        destination: NavDestination,
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
        destination: NavDestination,
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
    // SCOPE-AWARE PUSH OPERATIONS
    // =========================================================================

    /**
     * Push a destination with scope awareness and tab switching.
     *
     * Navigation logic for TabNode context:
     * 1. First check if destination is in scope (using [scopeRegistry])
     * 2. If in scope, check if destination already exists in any TabNode stack
     *    - If found in another tab's stack, switch to that tab instead of pushing
     * 3. If not found and out of scope, delegate to parent stack
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
     * instead of creating a duplicate entry. This provides intuitive navigation
     * where calling navigate() with a tab destination switches to that tab.
     *
     * ## Example
     *
     * ```kotlin
     * // Given: Root -> Stack -> TabNode(scopeKey="MainTabs") -> Stack(active)
     * // MainTabs contains: Home (tab 0), Search (tab 1), Profile (tab 2)
     *
     * // HomeDestination is in scope AND exists in tab 0 - switches to tab 0
     * TreeMutator.push(root, HomeDestination, scopeRegistry)
     *
     * // NewScreen is in scope but doesn't exist - pushes to active stack
     * TreeMutator.push(root, NewScreenDestination, scopeRegistry)
     *
     * // DetailDestination is NOT in scope - pushes to parent stack (above TabNode)
     * TreeMutator.push(root, DetailDestination, scopeRegistry)
     * ```
     *
     * @param root The root NavNode of the navigation tree
     * @param destination The destination to navigate to
     * @param scopeRegistry Registry to check scope membership
     * @param generateKey Function to generate unique keys for new nodes
     * @return New tree with the destination pushed or tab switched appropriately
     * @throws IllegalStateException if no suitable stack is found
     */
    @OptIn(ExperimentalUuidApi::class)
    fun push(
        root: NavNode,
        destination: NavDestination,
        scopeRegistry: ScopeRegistry,
        generateKey: () -> String = { Uuid.random().toString().take(8) }
    ): NavNode {
        // If no scope registry (or Empty), use the simple push
        if (scopeRegistry === ScopeRegistry.Empty) {
            return push(root, destination, generateKey)
        }

        return when (val strategy = determinePushStrategy(root, destination, scopeRegistry)) {
            is PushStrategy.PushToStack -> {
                pushToActiveStack(root, strategy.targetStack, destination, generateKey)
            }

            is PushStrategy.SwitchToTab -> {
                switchTab(root, strategy.tabNode.key, strategy.tabIndex)
            }

            is PushStrategy.PushOutOfScope -> {
                pushOutOfScope(root, strategy.parentStack, destination, generateKey)
            }
        }
    }

    /**
     * Determines the push strategy for a destination considering scope and existing tabs.
     *
     * Walks up from the deepest active stack, checking each container (TabNode/PaneNode)
     * to determine how the navigation should be handled.
     *
     * @param root The root NavNode of the navigation tree
     * @param destination The destination to check
     * @param scopeRegistry Registry for scope membership checks
     * @return The appropriate PushStrategy for this navigation
     * @throws IllegalStateException if no valid strategy can be determined
     */
    private fun determinePushStrategy(
        root: NavNode,
        destination: NavDestination,
        scopeRegistry: ScopeRegistry
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
    inline fun <reified D : NavDestination> popToDestination(
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
        destination: NavDestination,
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
        val activeStack = paneNode.activePaneContent?.activeStack()

        if (activeStack == null) {
            return PopResult.PaneEmpty(paneNode.activePaneRole)
        }

        if (activeStack.children.size <= 1) {
            // Stack would become empty - switch to Primary if not already there
            return if (paneNode.activePaneRole != PaneRole.Primary) {
                val newState = switchActivePane(root, paneNode.key, PaneRole.Primary)
                PopResult.Popped(newState)
            } else {
                PopResult.PaneEmpty(paneNode.activePaneRole)
            }
        }

        // Pop from the active stack
        val newChildren = activeStack.children.dropLast(1)
        val newStack = activeStack.copy(children = newChildren)
        val newState = replaceNode(root, activeStack.key, newStack)
        return PopResult.Popped(newState)
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
                    else if (child.findByKey(targetKey) != null) replaceNode(
                        child,
                        targetKey,
                        newNode
                    )
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
        destination: NavDestination,
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
        destination: NavDestination,
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
        destination: NavDestination,
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
        destinations: List<NavDestination>,
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
    fun currentDestination(root: NavNode): NavDestination? {
        return root.activeLeaf()?.destination
    }

    // =========================================================================
    // TREE-AWARE BACK HANDLING
    // =========================================================================

    /**
     * Pop with intelligent tab handling.
     *
     * Handles back navigation with proper tab behavior:
     * 1. If active tab's stack has > 1 items → pop from stack
     * 2. If active tab's stack is at root AND not initial tab → switch to initial tab
     * 3. If on initial tab at root → continue to next stack level
     *
     * @param root The root NavNode
     * @return BackResult indicating the outcome
     */
    fun popWithTabBehavior(root: NavNode): BackResult {
        val activeStack = root.activeStack() ?: return BackResult.CannotHandle

        // Case 1: Stack has items to pop
        if (activeStack.canGoBack) {
            val newState = pop(root) ?: return BackResult.CannotHandle
            return BackResult.Handled(newState)
        }

        // Find the parent tab node if any
        val parentKey = activeStack.parentKey ?: return handleRootStackBack(root, activeStack)
        val parent = root.findByKey(parentKey)

        return when (parent) {
            is TabNode -> handleTabBack(root, parent, activeStack)
            is StackNode -> handleNestedStackBack(root, parent, activeStack)
            is PaneNode -> handlePaneBack(root, parent, activeStack)
            else -> BackResult.CannotHandle
        }
    }

    /**
     * Handle back when the active stack is a root stack.
     */
    private fun handleRootStackBack(root: NavNode, activeStack: StackNode): BackResult {
        // Root stack at minimum size - delegate to system
        return if (activeStack.children.size <= 1) {
            BackResult.DelegateToSystem
        } else {
            val newState = pop(root) ?: return BackResult.CannotHandle
            BackResult.Handled(newState)
        }
    }

    /**
     * Handle back when active stack is inside a TabNode.
     *
     * Simplified behavior:
     * - If active tab's stack has only 1 child: pop entire TabNode (regardless of which tab)
     * - If parent also has only one child: cascade further up the tree
     *
     * Note: This does NOT switch tabs on back - it pops the entire TabNode.
     */
    private fun handleTabBack(root: NavNode, tabNode: TabNode, activeStack: NavNode): BackResult {
        // Active tab's stack has only 1 child → try to pop the entire TabNode
        val tabParentKey = tabNode.parentKey
        if (tabParentKey == null) {
            // TabNode is root - delegate to system
            return BackResult.DelegateToSystem
        }

        // TabNode has a parent - try to pop from it
        return when (val tabParent = root.findByKey(tabParentKey)) {
            is StackNode -> {
                if (tabParent.children.size > 1) {
                    // Parent stack has multiple children - can pop TabNode
                    val newState = removeNode(root, tabNode.key)
                    if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
                } else if (tabParent.parentKey == null) {
                    // Parent is root stack with only TabNode - delegate to system
                    BackResult.DelegateToSystem
                } else {
                    // CASCADE: Parent stack has only TabNode - try to pop parent from grandparent
                    val grandparentKey = tabParent.parentKey
                    val grandparent = root.findByKey(grandparentKey)

                    when (grandparent) {
                        is StackNode -> handleNestedStackBack(root, grandparent, tabParent)
                        is TabNode -> handleTabBack(root, grandparent, tabParent)
                        is PaneNode -> handlePaneBack(root, grandparent, tabParent)
                        else -> BackResult.DelegateToSystem
                    }
                }
            }

            is TabNode -> {
                // TabNode inside another TabNode (edge case)
                // Treat parent TabNode as if we're on its stack
                handleTabBack(root, tabParent, tabNode.activeStack)
            }

            else -> BackResult.DelegateToSystem
        }
    }

    /**
     * Handle back when active stack is nested inside another stack.
     *
     * Cascade behavior:
     * - If parent can pop child (size > 1): pop child
     * - If parent is root: delegate to system
     * - If parent also has only 1 child: cascade to grandparent
     */
    private fun handleNestedStackBack(
        root: NavNode,
        parentStack: StackNode,
        childStack: StackNode
    ): BackResult {
        return if (parentStack.children.size > 1) {
            // Parent has multiple children - can remove the child stack
            val newState = removeNode(root, childStack.key)
            if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
        } else if (parentStack.parentKey == null) {
            // Parent is root with only one child - delegate to system
            BackResult.DelegateToSystem
        } else {
            // Parent has only one child and is not root - CASCADE UP
            // Try to pop the parent stack from its grandparent
            val grandparentKey = parentStack.parentKey
            val grandparent = root.findByKey(grandparentKey)

            when (grandparent) {
                is StackNode -> handleNestedStackBack(root, grandparent, parentStack)
                is TabNode -> handleTabBack(root, grandparent, parentStack)
                is PaneNode -> handlePaneBack(root, grandparent, parentStack)
                else -> BackResult.DelegateToSystem
            }
        }
    }

    /**
     * Handle back when active stack is inside a PaneNode.
     *
     * Cascade behavior:
     * - If pane can handle the pop: return handled
     * - If PaneNode is root: delegate to system
     * - If PaneNode's parent can pop it: pop PaneNode
     * - If parent also has only one child: cascade further up the tree
     */
    private fun handlePaneBack(
        root: NavNode,
        paneNode: PaneNode,
        activeStack: NavNode
    ): BackResult {
        return when (val result = popWithPaneBehavior(root)) {
            is PopResult.Popped -> BackResult.Handled(result.newState)
            is PopResult.CannotPop, is PopResult.PaneEmpty -> {
                // Check if PaneNode itself can be popped
                val paneParentKey = paneNode.parentKey
                if (paneParentKey == null) {
                    BackResult.DelegateToSystem
                } else {
                    // CASCADE: Try to pop PaneNode from its parent
                    val paneParent = root.findByKey(paneParentKey)
                    when (paneParent) {
                        is StackNode -> {
                            if (paneParent.children.size > 1) {
                                val newState = removeNode(root, paneNode.key)
                                if (newState != null) BackResult.Handled(newState) else BackResult.CannotHandle
                            } else if (paneParent.parentKey == null) {
                                BackResult.DelegateToSystem
                            } else {
                                // Continue cascading
                                val grandparentKey = paneParent.parentKey
                                val grandparent = root.findByKey(grandparentKey)
                                when (grandparent) {
                                    is StackNode -> handleNestedStackBack(
                                        root,
                                        grandparent,
                                        paneParent
                                    )

                                    is TabNode -> handleTabBack(root, grandparent, paneParent)
                                    is PaneNode -> handlePaneBack(root, grandparent, paneParent)
                                    else -> BackResult.DelegateToSystem
                                }
                            }
                        }

                        is TabNode -> handleTabBack(root, paneParent, activeStack)
                        is PaneNode -> handlePaneBack(root, paneParent, activeStack)
                        else -> BackResult.DelegateToSystem
                    }
                }
            }

            is PopResult.RequiresScaffoldChange -> BackResult.CannotHandle
        }
    }

    /**
     * Check if back navigation is possible, considering root constraints.
     *
     * Unlike [canGoBack], this method considers:
     * - Root stack must keep at least one item (would delegate to system)
     * - Tab switching as an alternative to popping
     * - Cascade back: removing TabNode from parent when on initial tab
     *
     * @param root The root NavNode
     * @return true if the navigation system can handle back (not delegated to system)
     */
    fun canHandleBackNavigation(root: NavNode): Boolean {
        val activeStack = root.activeStack() ?: return false

        // Can pop from active stack
        if (activeStack.canGoBack) return true

        // Check for tab switch opportunity or cascade back
        val parentKey = activeStack.parentKey ?: return false
        val parent = root.findByKey(parentKey)

        return when (parent) {
            is TabNode -> {
                // TabNode can be popped if its parent stack has multiple children
                val tabParentKey = parent.parentKey ?: return false
                val tabParent = root.findByKey(tabParentKey)
                (tabParent as? StackNode)?.children?.size?.let { it > 1 } ?: false
            }

            is StackNode -> parent.canGoBack
            is PaneNode -> parent.paneConfigurations.values.any {
                it.content.activeStack()?.canGoBack == true
            }

            else -> false
        }
    }

}
