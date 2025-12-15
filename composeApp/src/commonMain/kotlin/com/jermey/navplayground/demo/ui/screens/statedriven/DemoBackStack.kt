package com.jermey.navplayground.demo.ui.screens.statedriven

import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TreeMutator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry in the demo backstack.
 * Wraps a ScreenNode to provide a simpler API.
 */
data class BackStackEntry(
    val id: String,
    val destination: Destination
)

/**
 * Simple backstack wrapper for the state-driven demo.
 * 
 * This wraps the tree-based NavNode structure to provide a simpler
 * list-like API for demonstration purposes.
 */
class DemoBackStack {
    
    private val _state = MutableStateFlow<NavNode>(
        StackNode(
            key = NavKeyGenerator.generate(),
            parentKey = null,
            children = emptyList()
        )
    )
    
    /**
     * The underlying navigation state.
     */
    val state: StateFlow<NavNode> = _state
    
    /**
     * Flow that emits whether back navigation is possible.
     */
    val canGoBack: StateFlow<Boolean> = MutableStateFlow(false).also { flow ->
        // Manually sync since we can't use derived states in common code easily
    }
    
    /**
     * Current list of entries in the backstack.
     */
    val entries: List<BackStackEntry>
        get() {
            val node = _state.value
            return when (node) {
                is StackNode -> node.children.filterIsInstance<ScreenNode>().map { screen ->
                    BackStackEntry(
                        id = screen.key,
                        destination = screen.destination
                    )
                }
                else -> emptyList()
            }
        }
    
    /**
     * Size of the backstack.
     */
    val size: Int
        get() = entries.size
    
    /**
     * Whether the backstack is empty.
     */
    val isEmpty: Boolean
        get() = entries.isEmpty()
    
    /**
     * Whether the backstack is not empty.
     */
    val isNotEmpty: Boolean
        get() = entries.isNotEmpty()
    
    /**
     * Current (topmost) entry, or null if stack is empty.
     */
    val current: BackStackEntry?
        get() = entries.lastOrNull()
    
    /**
     * Whether we can go back (more than one entry).
     */
    val canNavigateBack: Boolean
        get() = entries.size > 1
    
    /**
     * Push a destination onto the backstack.
     */
    fun push(destination: Destination) {
        val newState = TreeMutator.push(_state.value, destination)
        _state.value = newState
    }
    
    /**
     * Pop the top entry from the backstack.
     * @return true if pop was successful
     */
    fun pop(): Boolean {
        if (!canNavigateBack) return false
        val newState = TreeMutator.pop(_state.value)
        if (newState != null) {
            _state.value = newState
            return true
        }
        return false
    }
    
    /**
     * Clear all entries from the backstack.
     */
    fun clear() {
        _state.value = StackNode(
            key = NavKeyGenerator.generate(),
            parentKey = null,
            children = emptyList()
        )
    }
    
    /**
     * Remove entry at a specific index.
     */
    fun removeAt(index: Int) {
        val currentEntries = entries
        if (index < 0 || index >= currentEntries.size) return
        
        val entry = currentEntries[index]
        val newState = TreeMutator.removeNode(_state.value, entry.id)
        if (newState != null) {
            _state.value = newState
        }
    }
    
    /**
     * Swap entries at two indices.
     */
    fun swap(index1: Int, index2: Int) {
        val currentEntries = entries
        if (index1 < 0 || index1 >= currentEntries.size) return
        if (index2 < 0 || index2 >= currentEntries.size) return
        if (index1 == index2) return
        
        val root = _state.value
        if (root !is StackNode) return
        
        val children = root.children.toMutableList()
        val temp = children[index1]
        children[index1] = children[index2]
        children[index2] = temp
        
        _state.value = root.copy(children = children)
    }
}
