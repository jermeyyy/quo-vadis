package com.jermey.navplayground.demo.ui.screens.statedriven

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.jermey.navplayground.demo.destinations.StateDrivenDemoDestination.DemoTab
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator
import com.jermey.quo.vadis.flowmvi.SharedContainerScope
import com.jermey.quo.vadis.flowmvi.SharedNavigationContainer
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.enableLogging
import pro.respawn.flowmvi.plugins.recover
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import kotlin.jvm.JvmSuppressWildcards
import androidx.compose.runtime.CompositionLocalProvider

@OptIn(InternalQuoVadisApi::class)
private typealias Ctx = PipelineContext<StateDrivenState, StateDrivenIntent, StateDrivenAction>

/**
 * State-driven navigation demo shared container with FlowMVI store.
 *
 * Demonstrates state-driven navigation by observing and manipulating the actual
 * [Navigator] state. This showcases how navigation can be controlled directly
 * through the navigation tree state, inspired by patterns from advanced navigation libraries.
 *
 * Key concepts:
 * - Observes [Navigator.state] to reflect the real navigation tree
 * - Uses [Navigator.navigate], [Navigator.navigateBack], and [Navigator.updateState]
 *   for backstack manipulation
 * - Uses [TreeMutator] for advanced tree operations (remove, swap)
 *
 * Uses [SharedNavigationContainer] so the state persists across the tabs container
 * and can be accessed from the TabsContainer wrapper.
 *
 * @param scope The shared container scope providing access to navigator.
 * @param debuggable Whether to enable debug logging for the store.
 */
@OptIn(InternalQuoVadisApi::class)
class StateDrivenContainer(
    scope: SharedContainerScope,
    private val debuggable: Boolean = false
) : SharedNavigationContainer<StateDrivenState, StateDrivenIntent, StateDrivenAction>(scope) {

    override val store: Store<StateDrivenState, StateDrivenIntent, StateDrivenAction> =
        store(initial = StateDrivenState()) {
            configure {
                debuggable = this@StateDrivenContainer.debuggable
                name = "StateDrivenStore"
                parallelIntents = false // Process intents sequentially
            }

            // Observe Navigator state and sync to our MVI state
            whileSubscribed {
                navigator.state
                    .onEach { navNode ->
                        updateState { copy(entries = extractEntriesFromNavNode(navNode)) }
                    }
                    .launchIn(this)
            }

            // Reduce: handle all intents
            reduce { intent ->
                when (intent) {
                    is StateDrivenIntent.PushDestination -> handlePush(intent.destination)
                    is StateDrivenIntent.Pop -> handlePop()
                    is StateDrivenIntent.Clear -> handleClear()
                    is StateDrivenIntent.Reset -> handleReset()
                    is StateDrivenIntent.RemoveAt -> handleRemoveAt(intent.index)
                    is StateDrivenIntent.Swap -> handleSwap(intent.index1, intent.index2)
                    is StateDrivenIntent.ShowAddDialog -> handleShowAddDialog()
                    is StateDrivenIntent.HideAddDialog -> handleHideAddDialog()
                    is StateDrivenIntent.NavigateBack -> handleNavigateBack()
                }
            }

            // Recover: handle errors gracefully
            recover { exception ->
                action(StateDrivenAction.ShowToast(exception.message ?: "An error occurred"))
                null // Suppress exception
            }

            // Enable logging for debugging
            if (debuggable) {
                enableLogging()
            }
        }

    /**
     * Push a destination onto the Navigator's backstack.
     */
    private fun Ctx.handlePush(destination: DemoTab) {
        navigator.navigate(destination)
    }

    /**
     * Pop the top entry from the Navigator's backstack.
     */
    private fun Ctx.handlePop() {
        navigator.navigateBack()
    }

    /**
     * Clear the backstack and navigate to the root.
     * Uses TreeMutator to create an empty stack and updates Navigator state.
     */
    private fun Ctx.handleClear() {
        val currentState = navigator.state.value
        // Find the deepest active StackNode and clear its children
        val clearedState = clearActiveStack(currentState)
        if (clearedState != null) {
            navigator.updateState(clearedState)
        }
    }

    /**
     * Reset: clear backstack and push Home destination.
     */
    private fun Ctx.handleReset() {
        navigator.navigateAndClearAll(DemoTab.Home)
    }

    /**
     * Remove entry at a specific index using TreeMutator.
     */
    private suspend fun Ctx.handleRemoveAt(index: Int) {
        withState {
            if (index < 0 || index >= entries.size) return@withState

            val entry = entries[index]
            val currentNavState = navigator.state.value
            val newState = TreeMutator.removeNode(currentNavState, entry.id)
            if (newState != null) {
                navigator.updateState(newState)
            }
        }
    }

    /**
     * Swap entries at two indices by manipulating the Navigator's state tree.
     */
    private suspend fun Ctx.handleSwap(index1: Int, index2: Int) {
        withState {
            if (index1 < 0 || index1 >= entries.size) return@withState
            if (index2 < 0 || index2 >= entries.size) return@withState
            if (index1 == index2) return@withState

            val currentNavState = navigator.state.value
            val swappedState = swapInActiveStack(currentNavState, index1, index2)
            if (swappedState != null) {
                navigator.updateState(swappedState)
            }
        }
    }

    /**
     * Show the add destination dialog.
     */
    private suspend fun Ctx.handleShowAddDialog() {
        updateState { copy(showAddDialog = true) }
    }

    /**
     * Hide the add destination dialog.
     */
    private suspend fun Ctx.handleHideAddDialog() {
        updateState { copy(showAddDialog = false) }
    }

    /**
     * Navigate back using the main navigator (exit the demo screen).
     */
    private fun Ctx.handleNavigateBack() {
        navigator.navigateBack()
    }

    /**
     * Extract BackStackEntry list from the NavNode tree.
     * Finds the deepest active StackNode and extracts its ScreenNode children.
     */
    private fun extractEntriesFromNavNode(node: NavNode): List<BackStackEntry> {
        return findDeepestActiveStack(node)?.children
            ?.filterIsInstance<ScreenNode>()
            ?.map { screen ->
                BackStackEntry(
                    id = screen.key,
                    destination = screen.destination
                )
            } ?: emptyList()
    }

    /**
     * Find the deepest active StackNode in the tree.
     * Handles TabNode by following the active stack.
     */
    private fun findDeepestActiveStack(node: NavNode): StackNode? {
        return when (node) {
            is TabNode -> {
                // Get the active stack from the TabNode
                val activeStack = node.stacks.getOrNull(node.activeStackIndex)
                activeStack?.let { findDeepestActiveStack(it) }
            }
            is StackNode -> {
                // Check if last child has a deeper stack or TabNode
                val lastChild = node.children.lastOrNull()
                if (lastChild != null) {
                    findDeepestActiveStack(lastChild) ?: node
                } else {
                    node
                }
            }
            else -> null
        }
    }

    /**
     * Clear the children of the deepest active StackNode.
     * Handles TabNode by following the active stack.
     */
    private fun clearActiveStack(node: NavNode): NavNode? {
        return when (node) {
            is TabNode -> {
                val activeStack = node.stacks.getOrNull(node.activeStackIndex) ?: return null
                val clearedStack = clearActiveStack(activeStack) as? StackNode ?: return null
                val newStacks = node.stacks.toMutableList()
                newStacks[node.activeStackIndex] = clearedStack
                node.copy(stacks = newStacks)
            }
            is StackNode -> {
                val lastChild = node.children.lastOrNull()
                if (lastChild != null) {
                    val clearedChild = clearActiveStack(lastChild)
                    if (clearedChild != null) {
                        node.copy(children = node.children.dropLast(1) + clearedChild)
                    } else {
                        // This is the deepest stack, clear it
                        node.copy(children = emptyList())
                    }
                } else {
                    node.copy(children = emptyList())
                }
            }
            else -> null
        }
    }

    /**
     * Swap children at given indices in the deepest active StackNode.
     * Handles TabNode by following the active stack.
     */
    private fun swapInActiveStack(node: NavNode, index1: Int, index2: Int): NavNode? {
        return when (node) {
            is TabNode -> {
                val activeStack = node.stacks.getOrNull(node.activeStackIndex) ?: return null
                val swappedStack = swapInActiveStack(activeStack, index1, index2) as? StackNode ?: return null
                val newStacks = node.stacks.toMutableList()
                newStacks[node.activeStackIndex] = swappedStack
                node.copy(stacks = newStacks)
            }
            is StackNode -> {
                val lastChild = node.children.lastOrNull()
                when (lastChild) {
                    is TabNode -> {
                        val swappedChild = swapInActiveStack(lastChild, index1, index2)
                        if (swappedChild != null) {
                            node.copy(children = node.children.dropLast(1) + swappedChild)
                        } else {
                            performSwap(node, index1, index2)
                        }
                    }
                    is StackNode -> {
                        val swappedChild = swapInActiveStack(lastChild, index1, index2)
                        if (swappedChild != null) {
                            node.copy(children = node.children.dropLast(1) + swappedChild)
                        } else {
                            // Swap in this stack
                            performSwap(node, index1, index2)
                        }
                    }
                    else -> {
                        // This is the deepest stack, perform swap
                        performSwap(node, index1, index2)
                    }
                }
            }
            else -> null
        }
    }

    /**
     * Perform the actual swap operation on a StackNode.
     */
    private fun performSwap(stack: StackNode, index1: Int, index2: Int): StackNode {
        val children = stack.children.toMutableList()
        if (index1 in children.indices && index2 in children.indices) {
            val temp = children[index1]
            children[index1] = children[index2]
            children[index2] = temp
        }
        return stack.copy(children = children)
    }
}

/**
 * CompositionLocal providing access to the StateDriven shared store.
 *
 * Usage in the TabsContainer:
 * ```kotlin
 * val store = LocalStateDrivenStore.current
 * store.intent(StateDrivenIntent.PushDestination(...))
 * ```
 */
val LocalStateDrivenStore: ProvidableCompositionLocal<Store<StateDrivenState, StateDrivenIntent, StateDrivenAction>> =
    staticCompositionLocalOf { error("No StateDrivenStore provided") }
