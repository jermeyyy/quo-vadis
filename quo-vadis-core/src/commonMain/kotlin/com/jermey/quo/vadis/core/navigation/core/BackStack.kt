package com.jermey.navplayground.navigation.core

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Represents the navigation backstack with direct access and modification capabilities.
 * Allows for advanced navigation scenarios like clearing stack, replacing, etc.
 */
@Stable
interface BackStack {
    /**
     * Current stack as a flow of entries.
     */
    val stack: StateFlow<List<BackStackEntry>>

    /**
     * Current top entry in the stack.
     */
    val current: StateFlow<BackStackEntry?>

    /**
     * Previous entry in the stack (if any).
     */
    val previous: StateFlow<BackStackEntry?>

    /**
     * Whether we can navigate back.
     */
    val canGoBack: StateFlow<Boolean>

    /**
     * Push a destination onto the stack.
     */
    fun push(destination: Destination)

    /**
     * Pop the current destination from the stack.
     */
    fun pop(): Boolean

    /**
     * Pop until a destination matching the predicate is found.
     */
    fun popUntil(predicate: (Destination) -> Boolean): Boolean

    /**
     * Replace the current destination.
     */
    fun replace(destination: Destination)

    /**
     * Replace the entire stack with new destinations.
     */
    fun replaceAll(destinations: List<Destination>)

    /**
     * Clear the entire stack.
     */
    fun clear()

    /**
     * Pop to the root (first) destination.
     */
    fun popToRoot(): Boolean
}

/**
 * Entry in the backstack.
 */
data class BackStackEntry(
    val id: String = generateId(),
    val destination: Destination,
    val savedState: Map<String, Any?> = emptyMap()
) {
    companion object {
        fun create(destination: Destination): BackStackEntry {
            return BackStackEntry(
                id = generateId(),
                destination = destination
            )
        }

        private fun generateId(): String {
            return "${currentTimeMillis()}-${(0..999999).random()}"
        }

        private fun currentTimeMillis(): Long {
            // Platform-agnostic time (simplified)
            return (0..Long.MAX_VALUE).random()
        }
    }
}

/**
 * Mutable implementation of BackStack.
 */
class MutableBackStack : BackStack {
    private val _stack = MutableStateFlow<List<BackStackEntry>>(emptyList())
    override val stack: StateFlow<List<BackStackEntry>> = _stack

    private val _current = MutableStateFlow<BackStackEntry?>(null)
    override val current: StateFlow<BackStackEntry?> = _current

    private val _previous = MutableStateFlow<BackStackEntry?>(null)
    override val previous: StateFlow<BackStackEntry?> = _previous

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack

    override fun push(destination: Destination) {
        val entry = BackStackEntry.create(destination)
        val newStack = _stack.value + entry
        updateStack(newStack)
    }

    override fun pop(): Boolean {
        val currentStack = _stack.value
        if (currentStack.isEmpty()) return false

        val newStack = currentStack.dropLast(1)
        updateStack(newStack)
        return true
    }

    override fun popUntil(predicate: (Destination) -> Boolean): Boolean {
        val currentStack = _stack.value
        val index = currentStack.indexOfLast { predicate(it.destination) }

        if (index == -1) return false

        val newStack = currentStack.take(index + 1)
        updateStack(newStack)
        return true
    }

    override fun replace(destination: Destination) {
        val currentStack = _stack.value
        if (currentStack.isEmpty()) {
            push(destination)
            return
        }

        val entry = BackStackEntry.create(destination)
        val newStack = currentStack.dropLast(1) + entry
        updateStack(newStack)
    }

    override fun replaceAll(destinations: List<Destination>) {
        val newStack = destinations.map { BackStackEntry.create(it) }
        updateStack(newStack)
    }

    override fun clear() {
        updateStack(emptyList())
    }

    override fun popToRoot(): Boolean {
        val currentStack = _stack.value
        if (currentStack.size <= 1) return false

        val newStack = currentStack.take(1)
        updateStack(newStack)
        return true
    }

    private fun updateStack(newStack: List<BackStackEntry>) {
        _stack.value = newStack
        _current.value = newStack.lastOrNull()
        _previous.value = if (newStack.size > 1) newStack[newStack.lastIndex - 1] else null
        _canGoBack.value = newStack.size > 1
    }
}
