package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Compatibility layer for bridging tree-based navigation state to BackStack API.
 *
 * This is a temporary solution to allow compose files to continue working
 * during the Phase 1 to Phase 2 transition. It will be removed once the
 * renderer (Phase 2) is fully implemented with tree-based rendering.
 *
 * **NOTE**: This class is deprecated and should not be used in new code.
 * Use [Navigator.state] directly for tree-based navigation.
 */
@Suppress("TooManyFunctions")
@Deprecated(
    message = "Use Navigator.state for tree-based navigation. This compatibility layer will be removed in Phase 2.",
    level = DeprecationLevel.WARNING
)
class NavigatorBackStackCompat(
    private val navigator: Navigator
) : BackStack {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // Convert tree state to flat list of BackStackEntry
    private val _entries: SnapshotStateList<BackStackEntry> = mutableStateListOf()
    override val entries: SnapshotStateList<BackStackEntry>
        get() = _entries

    init {
        // Observe navigator state and convert to backstack entries
        scope.launch {
            navigator.state.collect { navNode ->
                updateEntriesFromTree(navNode)
            }
        }
    }

    private fun updateEntriesFromTree(navNode: NavNode) {
        val activeStack = navNode.activeStack()
        val newEntries = activeStack?.children?.mapNotNull { child ->
            (child as? ScreenNode)?.let { screen ->
                BackStackEntry(
                    id = screen.key,
                    destination = screen.destination,
                    transition = null // Transition info not stored in tree
                )
            }
        } ?: emptyList()

        _entries.clear()
        _entries.addAll(newEntries)
        updateFlows()
    }

    private fun updateFlows() {
        _stack.value = _entries.toList()
        _current.value = _entries.lastOrNull()
        _previous.value = _entries.getOrNull(_entries.size - 2)
        _canGoBack.value = _entries.size > 1
    }

    // Flow-based state
    private val _stack = MutableStateFlow<List<BackStackEntry>>(emptyList())
    override val stack: StateFlow<List<BackStackEntry>> = _stack.asStateFlow()

    private val _current = MutableStateFlow<BackStackEntry?>(null)
    override val current: StateFlow<BackStackEntry?> = _current.asStateFlow()

    private val _previous = MutableStateFlow<BackStackEntry?>(null)
    override val previous: StateFlow<BackStackEntry?> = _previous.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    // Navigation operations delegate to navigator
    override fun push(destination: Destination, transition: NavigationTransition?) {
        navigator.navigate(destination, transition)
    }

    override fun pop(): Boolean {
        return navigator.navigateBack()
    }

    override fun popUntil(predicate: (Destination) -> Boolean): Boolean {
        // Find the target destination in the current state
        val activeStack = navigator.state.value.activeStack() ?: return false
        val targetIndex = activeStack.children.indexOfLast { child ->
            (child as? ScreenNode)?.destination?.let { predicate(it) } ?: false
        }
        if (targetIndex < 0) return false

        // Pop entries until we reach the target
        val toPop = activeStack.children.size - 1 - targetIndex
        repeat(toPop) {
            if (!navigator.navigateBack()) return false
        }
        return true
    }

    override fun replace(destination: Destination, transition: NavigationTransition?) {
        navigator.navigateAndReplace(destination, transition)
    }

    override fun replaceAll(destinations: List<Destination>) {
        if (destinations.isEmpty()) return
        navigator.navigateAndClearAll(destinations.first())
        destinations.drop(1).forEach { dest ->
            navigator.navigate(dest)
        }
    }

    override fun clear() {
        // Clear to empty stack (just keep root)
        val activeStack = navigator.state.value.activeStack() ?: return
        if (activeStack.children.size > 1) {
            val rootDest = (activeStack.children.firstOrNull() as? ScreenNode)?.destination
            if (rootDest != null) {
                navigator.navigateAndClearAll(rootDest)
            }
        }
    }

    override fun popToRoot(): Boolean {
        val activeStack = navigator.state.value.activeStack() ?: return false
        if (activeStack.children.size <= 1) return false
        
        // Pop until we're at root (size == 1)
        while (navigator.state.value.activeStack()?.children?.size ?: 0 > 1) {
            if (!navigator.navigateBack()) break
        }
        return true
    }

    override fun insert(index: Int, destination: Destination, transition: NavigationTransition?) {
        // Not directly supported in tree navigation - throw for now
        throw UnsupportedOperationException(
            "insert() is not supported in compatibility layer. Use Navigator.state directly."
        )
    }

    override fun removeAt(index: Int): BackStackEntry {
        // Not directly supported - throw for now
        throw UnsupportedOperationException(
            "removeAt() is not supported in compatibility layer. Use Navigator.state directly."
        )
    }

    override fun removeById(id: String): Boolean {
        // Not directly supported - throw for now
        throw UnsupportedOperationException(
            "removeById() is not supported in compatibility layer. Use Navigator.state directly."
        )
    }

    override fun swap(indexA: Int, indexB: Int) {
        // Not directly supported - throw for now
        throw UnsupportedOperationException(
            "swap() is not supported in compatibility layer. Use Navigator.state directly."
        )
    }

    override fun move(fromIndex: Int, toIndex: Int) {
        // Not directly supported - throw for now
        throw UnsupportedOperationException(
            "move() is not supported in compatibility layer. Use Navigator.state directly."
        )
    }

    override fun replaceAllWithEntries(entries: List<BackStackEntry>) {
        // Not directly supported - throw for now
        throw UnsupportedOperationException(
            "replaceAllWithEntries() is not supported in compatibility layer. Use Navigator.state directly."
        )
    }
}

/**
 * Extension property to get a BackStack-compatible view of the navigator.
 *
 * **DEPRECATED**: This is a compatibility layer for Phase 1 to Phase 2 transition.
 * Use [Navigator.state] directly for tree-based navigation.
 *
 * @return A BackStack implementation that mirrors the active stack in the tree state
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Use Navigator.state for tree-based navigation. This compatibility layer will be removed in Phase 2.",
    level = DeprecationLevel.WARNING
)
val Navigator.backStack: BackStack
    get() {
        // Cache the compat instance to avoid recreating it
        return backStackCompatCache.getOrPut(this) {
            NavigatorBackStackCompat(this)
        }
    }

// Cache to avoid recreating compat instances
private val backStackCompatCache = mutableMapOf<Navigator, BackStack>()
