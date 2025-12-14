package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode

/**
 * Defines the scope of state caching for navigation surfaces.
 *
 * Different navigation contexts require different caching strategies to optimize
 * performance and preserve the appropriate state during navigation transitions.
 *
 * ## Caching Strategy by Node Type
 *
 * | Node Type | Cross-Node Navigation | Intra-Node Navigation |
 * |-----------|----------------------|----------------------|
 * | TabNode | Cache whole wrapper | Cache only tab content |
 * | PaneNode | Cache whole wrapper | Cache only pane content |
 * | StackNode | N/A | Standard screen caching |
 * | ScreenNode | N/A | Full screen caching |
 *
 * @see NavigationStateHolder.SaveableWithScope
 */
public enum class CacheScope {
    /**
     * Normal screen caching for ScreenNode and StackNode.
     *
     * The entire screen composable is cached. Used for:
     * - Individual screens in a navigation stack
     * - Simple navigation between destinations
     * - Default caching when no special strategy is needed
     */
    FULL_SCREEN,

    /**
     * Cache entire wrapper for TabNode/PaneNode during cross-node navigation.
     *
     * Preserves scaffold, app bar, bottom navigation, and other wrapper elements.
     * Used when navigating between different node types (e.g., switching tabs).
     *
     * Benefits:
     * - Wrapper stability (scaffold doesn't recreate)
     * - Smooth transitions without visual glitches
     * - Navigation chrome remains stable
     */
    WHOLE_WRAPPER,

    /**
     * Cache only content, not wrapper, for intra-tab/pane navigation.
     *
     * Wrapper remains stable while content changes. Used for navigation
     * within the same tab or pane (e.g., pushing a screen within a tab).
     *
     * Benefits:
     * - Content independence (each tab's content preserved individually)
     * - Performance (avoiding unnecessary recomposition of wrapper)
     */
    CONTENT_ONLY
}

/**
 * Manages state preservation for navigation screens.
 *
 * NavigationStateHolder wraps [SaveableStateHolder] with navigation-specific
 * logic for:
 * - Automatic key management based on [NavNode.key]
 * - State cleanup when screens are removed
 * - Tab state preservation across tab switches
 * - Differentiated caching based on navigation context
 *
 * ## Usage
 *
 * ```kotlin
 * val stateHolder = rememberNavigationStateHolder()
 *
 * surfaces.forEach { surface ->
 *     stateHolder.SaveableScreen(key = surface.id) {
 *         surface.content()
 *     }
 * }
 * ```
 *
 * ## Differentiated Caching
 *
 * The state holder supports different caching strategies based on navigation
 * context. Use [determineCacheScope] to get the appropriate scope and
 * [SaveableWithScope] to apply the caching strategy.
 *
 * ```kotlin
 * val scope = stateHolder.determineCacheScope(
 *     transition = transitionState,
 *     surfaceId = surface.id,
 *     surfaceMode = surface.renderingMode
 * )
 *
 * stateHolder.SaveableWithScope(
 *     key = surface.id,
 *     scope = scope,
 *     wrapperContent = { content -> TabbedWrapper { content() } },
 *     content = { ScreenContent() }
 * )
 * ```
 *
 * ## Tab State Preservation
 *
 * For TabNode state preservation, use the [PreserveTabStates] extension:
 *
 * ```kotlin
 * stateHolder.PreserveTabStates(tabNode) {
 *     // Tab content - all tab states preserved across switches
 * }
 * ```
 *
 * @property saveableStateHolder The underlying Compose state holder
 * @see rememberNavigationStateHolder
 * @see CacheScope
 */
public class NavigationStateHolder internal constructor(
    private val saveableStateHolder: SaveableStateHolder
) {
    /**
     * Keys that should not be cleaned up even if not currently rendered.
     *
     * Useful for tabs - we want to retain all tab states even when only
     * the active tab is rendered.
     */
    private val retainedKeys = mutableSetOf<String>()

    /**
     * Provides saveable state for a screen.
     *
     * Wraps the content in a [SaveableStateHolder.SaveableStateProvider] to
     * preserve state across composition lifecycle changes. This ensures that:
     *
     * - Tab state is preserved when switching tabs
     * - Back stack screens maintain their state
     * - State survives process death when using [rememberSaveable][androidx.compose.runtime.saveable.rememberSaveable]
     *
     * @param key Unique identifier for this screen (typically [NavNode.key])
     * @param content The screen content to be wrapped
     */
    @Composable
    public fun SaveableScreen(
        key: String,
        content: @Composable () -> Unit
    ) {
        saveableStateHolder.SaveableStateProvider(key = key) {
            content()
        }
    }

    /**
     * Marks a key as retained (should not be cleaned up even if not rendered).
     *
     * Useful for tabs - we want to retain all tab states even when only
     * the active tab is rendered. Call [release] when the key should no
     * longer be retained.
     *
     * @param key The key to retain
     * @see release
     */
    public fun retain(key: String) {
        retainedKeys.add(key)
    }

    /**
     * Marks a key as no longer retained.
     *
     * After releasing a key, it will be eligible for cleanup when it's
     * no longer in the active navigation tree.
     *
     * @param key The key to release
     * @see retain
     */
    public fun release(key: String) {
        retainedKeys.remove(key)
    }

    /**
     * Removes saved state for the given key.
     *
     * State is only removed if the key is not in the retained set.
     * This prevents accidental cleanup of tab states that should survive
     * across tab switches.
     *
     * @param key The key whose state should be removed
     */
    public fun removeState(key: String) {
        if (key !in retainedKeys) {
            saveableStateHolder.removeState(key)
        }
    }

    /**
     * Updates the set of active keys and cleans up removed ones.
     *
     * This method should be called when the navigation state changes to
     * clean up state for screens that have been removed (e.g., after a pop).
     *
     * Keys in [retainedKeys] are never removed, even if they're not in
     * [activeKeys]. This preserves tab states across tab switches.
     *
     * @param activeKeys Set of keys currently in the navigation tree
     * @param previousKeys Set of keys from the previous state
     */
    public fun cleanup(activeKeys: Set<String>, previousKeys: Set<String>) {
        val removedKeys = previousKeys - activeKeys - retainedKeys
        removedKeys.forEach { key ->
            saveableStateHolder.removeState(key)
        }
    }

    /**
     * Applies the appropriate caching strategy based on scope.
     *
     * This composable wraps content with the correct [SaveableStateProvider]
     * configuration based on the determined [CacheScope]:
     *
     * - **FULL_SCREEN**: Both wrapper and content cached together
     * - **WHOLE_WRAPPER**: Wrapper cached separately, content inside
     * - **CONTENT_ONLY**: Only content cached, wrapper remains stable
     *
     * ## Example
     *
     * ```kotlin
     * stateHolder.SaveableWithScope(
     *     key = "tab-0",
     *     scope = CacheScope.CONTENT_ONLY,
     *     wrapperContent = { content ->
     *         Scaffold(bottomBar = { BottomNavigation() }) {
     *             content()
     *         }
     *     },
     *     content = { HomeScreen() }
     * )
     * ```
     *
     * @param key Unique identifier for this surface
     * @param scope The caching scope to apply
     * @param wrapperContent Optional wrapper composable (scaffold, app bar, etc.)
     *        that receives the content as a parameter
     * @param content The main content to be rendered
     */
    @Composable
    public fun SaveableWithScope(
        key: String,
        scope: CacheScope,
        wrapperContent: @Composable (@Composable () -> Unit) -> Unit = { it() },
        content: @Composable () -> Unit
    ) {
        when (scope) {
            CacheScope.FULL_SCREEN -> {
                saveableStateHolder.SaveableStateProvider(key = key) {
                    wrapperContent(content)
                }
            }
            CacheScope.WHOLE_WRAPPER -> {
                saveableStateHolder.SaveableStateProvider(key = "wrapper-$key") {
                    wrapperContent(content)
                }
            }
            CacheScope.CONTENT_ONLY -> {
                // Wrapper is NOT wrapped in SaveableStateProvider
                // Only content is cached
                wrapperContent {
                    saveableStateHolder.SaveableStateProvider(key = "content-$key") {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Creates and remembers a [NavigationStateHolder].
 *
 * This is the primary entry point for creating a state holder instance.
 * The returned instance is remembered across recompositions and tied
 * to the composition lifecycle.
 *
 * ## Example
 *
 * ```kotlin
 * @Composable
 * fun QuoVadisHost(navigator: Navigator) {
 *     val stateHolder = rememberNavigationStateHolder()
 *
 *     // Use stateHolder for state preservation
 *     surfaces.forEach { surface ->
 *         stateHolder.SaveableScreen(key = surface.id) {
 *             surface.content()
 *         }
 *     }
 * }
 * ```
 *
 * @return A remembered [NavigationStateHolder] instance
 */
@Composable
public fun rememberNavigationStateHolder(): NavigationStateHolder {
    val saveableStateHolder = rememberSaveableStateHolder()
    return remember { NavigationStateHolder(saveableStateHolder) }
}

/**
 * Extension for preserving all tab states.
 *
 * This ensures that inactive tabs maintain their state even when
 * not being rendered. All stack keys in the [TabNode] are retained
 * during the composition, and released when disposed.
 *
 * ## State Preservation Behavior
 *
 * Without this extension, only the active tab's state is preserved.
 * With this extension:
 * - All tab stack keys are marked as retained
 * - Inactive tab states survive tab switches
 * - States are properly cleaned up when the TabNode leaves composition
 *
 * ## Example
 *
 * ```kotlin
 * @Composable
 * fun TabbedNavigation(tabNode: TabNode, stateHolder: NavigationStateHolder) {
 *     stateHolder.PreserveTabStates(tabNode) {
 *         // All tab states are preserved
 *         TabContent(tabNode.activeStack)
 *     }
 * }
 * ```
 *
 * @param tabNode The TabNode whose states should be preserved
 * @param content The content to render (typically the active tab)
 */
@Composable
public fun NavigationStateHolder.PreserveTabStates(
    tabNode: TabNode,
    content: @Composable () -> Unit
) {
    // Collect all keys from the tab stacks before entering the effect
    // This ensures we don't call composable-context-requiring code inside DisposableEffect
    val allTabKeys = remember(tabNode.key) {
        tabNode.stacks.flatMap { stack ->
            collectAllKeys(stack)
        }.toSet()
    }

    // Retain all tab stack keys
    DisposableEffect(tabNode.key, allTabKeys) {
        allTabKeys.forEach { key ->
            retain(key)
        }

        onDispose {
            allTabKeys.forEach { key ->
                release(key)
            }
        }
    }

    content()
}

/**
 * Collects all keys from a [NavNode] tree.
 *
 * Recursively traverses the navigation tree and collects all node keys.
 * This is useful for:
 * - Tracking active keys for state cleanup
 * - Retaining all keys in a subtree (e.g., for tab preservation)
 * - Debugging navigation state
 *
 * ## Example
 *
 * ```kotlin
 * val activeKeys = collectAllKeys(navState)
 * val removedKeys = previousKeys - activeKeys
 * removedKeys.forEach { stateHolder.removeState(it) }
 * ```
 *
 * @param node The root node of the tree to traverse
 * @return A set containing all keys in the tree
 */
public fun collectAllKeys(node: NavNode): Set<String> {
    val keys = mutableSetOf<String>()
    collectKeysRecursive(node, keys)
    return keys
}

/**
 * Recursively collects keys from a [NavNode] tree into the provided mutable set.
 *
 * This is the internal implementation of [collectAllKeys] that uses a mutable
 * accumulator for efficiency.
 *
 * @param node The current node to process
 * @param keys The mutable set to add keys to
 */
private fun collectKeysRecursive(node: NavNode, keys: MutableSet<String>) {
    keys.add(node.key)
    when (node) {
        is ScreenNode -> {
            // Leaf node, already added above
        }
        is StackNode -> {
            node.children.forEach { child ->
                collectKeysRecursive(child, keys)
            }
        }
        is TabNode -> {
            node.stacks.forEach { stack ->
                collectKeysRecursive(stack, keys)
            }
        }
        is PaneNode -> {
            node.paneConfigurations.values.forEach { config ->
                collectKeysRecursive(config.content, keys)
            }
        }
    }
}

/**
 * Finds all [TabNode]s in a navigation tree.
 *
 * Recursively searches the tree and returns all TabNodes found.
 * This is useful for:
 * - Retaining all tab states in a complex navigation hierarchy
 * - Analyzing navigation structure
 *
 * @param node The root node of the tree to search
 * @return A list of all TabNodes found in the tree
 */
public fun findAllTabNodes(node: NavNode): List<TabNode> {
    val result = mutableListOf<TabNode>()
    findTabNodesRecursive(node, result)
    return result
}

/**
 * Recursively searches for [TabNode]s in a navigation tree.
 *
 * @param node The current node to search
 * @param result The mutable list to add found TabNodes to
 */
private fun findTabNodesRecursive(node: NavNode, result: MutableList<TabNode>) {
    when (node) {
        is ScreenNode -> {
            // No tabs in screen
        }
        is StackNode -> {
            node.children.forEach { child ->
                findTabNodesRecursive(child, result)
            }
        }
        is TabNode -> {
            result.add(node)
            node.stacks.forEach { stack ->
                findTabNodesRecursive(stack, result)
            }
        }
        is PaneNode -> {
            node.paneConfigurations.values.forEach { config ->
                findTabNodesRecursive(config.content, result)
            }
        }
    }
}

/**
 * Finds all [PaneNode]s in a navigation tree.
 *
 * Recursively searches the tree and returns all PaneNodes found.
 * This is useful for:
 * - Retaining all pane states in a complex navigation hierarchy
 * - Analyzing multi-pane navigation structure
 *
 * @param node The root node of the tree to search
 * @return A list of all PaneNodes found in the tree
 */
public fun findAllPaneNodes(node: NavNode): List<PaneNode> {
    val result = mutableListOf<PaneNode>()
    findPaneNodesRecursive(node, result)
    return result
}

/**
 * Recursively searches for [PaneNode]s in a navigation tree.
 *
 * @param node The current node to search
 * @param result The mutable list to add found PaneNodes to
 */
private fun findPaneNodesRecursive(node: NavNode, result: MutableList<PaneNode>) {
    when (node) {
        is ScreenNode -> {
            // No panes in screen
        }
        is StackNode -> {
            node.children.forEach { child ->
                findPaneNodesRecursive(child, result)
            }
        }
        is TabNode -> {
            node.stacks.forEach { stack ->
                findPaneNodesRecursive(stack, result)
            }
        }
        is PaneNode -> {
            result.add(node)
            node.paneConfigurations.values.forEach { config ->
                findPaneNodesRecursive(config.content, result)
            }
        }
    }
}
