package com.jermey.quo.vadis.core.navigation.internal.tree

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.internal.NavigationResultManager
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.NodeKey
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Collects screen keys from the navigation tree and manages
 * result cancellation for destroyed screens.
 *
 * When screens are removed from the navigation tree, any pending
 * result requests for those screens are cancelled to prevent leaks.
 *
 * @param navigatorScope Coroutine scope for async result cancellation
 * @param resultManager Manager for navigation results between screens
 */
@OptIn(InternalQuoVadisApi::class)
internal class ScreenKeyCollector(
    private val navigatorScope: CoroutineScope,
    private val resultManager: NavigationResultManager
) {

    /**
     * Cancel pending results for pre-computed removed screen keys.
     *
     * Used with [TreeDiffCalculator] for single-pass tree diffing.
     *
     * @param removedScreenKeys Screen keys that were removed from the tree
     */
    fun cancelResultsForKeys(removedScreenKeys: Set<NodeKey>) {
        if (removedScreenKeys.isEmpty()) return

        navigatorScope.launch {
            removedScreenKeys.forEach { screenKey ->
                resultManager.cancelResult(screenKey.value)
            }
        }
    }

    /**
     * Cancel pending results for screens that were removed from the tree.
     *
     * Compares the old and new tree states, identifies destroyed screens,
     * and cancels any pending results for those screens.
     *
     * @param oldState The previous navigation tree state
     * @param newState The new navigation tree state
     */
    fun cancelResultsForDestroyedScreens(oldState: NavNode, newState: NavNode) {
        val oldScreenKeys = collectScreenKeys(oldState)
        val newScreenKeys = collectScreenKeys(newState)
        val destroyedKeys = oldScreenKeys - newScreenKeys

        if (destroyedKeys.isEmpty()) return

        navigatorScope.launch {
            destroyedKeys.forEach { screenKey ->
                resultManager.cancelResult(screenKey.value)
            }
        }
    }

    private fun collectScreenKeys(node: NavNode): Set<NodeKey> {
        val keys = mutableSetOf<NodeKey>()
        collectScreenKeysRecursive(node, keys)
        return keys
    }

    private fun collectScreenKeysRecursive(node: NavNode, keys: MutableSet<NodeKey>) {
        when (node) {
            is ScreenNode -> keys.add(node.key)
            is StackNode -> node.children.forEach { collectScreenKeysRecursive(it, keys) }
            is TabNode -> node.stacks.forEach { collectScreenKeysRecursive(it, keys) }
            is PaneNode -> node.paneConfigurations.values.forEach {
                collectScreenKeysRecursive(it.content, keys)
            }
        }
    }
}
