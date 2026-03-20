package com.jermey.quo.vadis.core.registry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.jermey.quo.vadis.core.navigation.node.NodeKey

/**
 * Scope-aware registry for user-defined back handlers.
 *
 * Handlers are grouped by [NodeKey], so each navigation node (screen, stack, tab, pane)
 * maintains its own set of back handlers. During back navigation, handlers are consulted
 * following the active node path from leaf to root — the most specific scope is checked first.
 * Within each scope, handlers are consulted in LIFO order (last registered first).
 * The first handler that returns `true` consumes the event and stops propagation.
 *
 * The registry is Compose-observable via an internal version counter: reading [hasHandlers]
 * inside a Compose snapshot (e.g., during recomposition) will cause the reader to
 * recompose whenever handlers are added or removed.
 *
 * This registry is typically accessed via [LocalBackHandlerRegistry] and used internally
 * by navigation back-handler composables. Direct usage is rarely needed.
 *
 * Thread Safety: This class is not thread-safe and should only be accessed from the
 * main/UI thread, which is the expected usage pattern in Compose.
 */
class BackHandlerRegistry {
    private val handlers = mutableMapOf<NodeKey, MutableList<() -> Boolean>>()

    /**
     * Compose snapshot-observable version counter. Incremented on every mutation so that
     * composables reading [hasHandlers] recompose when the handler set changes.
     */
    private var version by mutableStateOf(0)

    /**
     * Register a back handler scoped to the given [key].
     *
     * @param key The [NodeKey] of the navigation node this handler belongs to.
     * @param handler A function that handles back events. Should return `true` if the
     *                back event was consumed and should not propagate further, or `false`
     *                to allow the event to propagate to the next handler.
     * @return A function that unregisters this handler when called. The unregister function
     *         is idempotent and safe to call multiple times.
     */
    fun register(key: NodeKey, handler: () -> Boolean): () -> Unit {
        handlers.getOrPut(key) { mutableListOf() }.add(handler)
        version++
        return {
            handlers[key]?.let { list ->
                if (list.remove(handler)) {
                    if (list.isEmpty()) handlers.remove(key)
                    version++
                }
            }
        }
    }

    /**
     * Attempt to handle a back event by consulting handlers along the active node path.
     *
     * For each [NodeKey] in [activeNodePath] (ordered leaf-first, root-last), the handlers
     * registered under that key are consulted in LIFO order. The first handler returning
     * `true` stops propagation.
     *
     * @param activeNodePath The path from the active leaf to the root, as returned by
     *                       [com.jermey.quo.vadis.core.navigation.node.activeNodePath].
     * @return `true` if any handler consumed the back event, `false` if no handlers
     *         consumed it or no handlers are registered for the given path.
     */
    fun handleBack(activeNodePath: List<NodeKey>): Boolean {
        for (key in activeNodePath) {
            val keyHandlers = handlers[key] ?: continue
            // Iterate in reverse order (LIFO) - innermost handlers get first chance
            for (i in keyHandlers.lastIndex downTo 0) {
                if (keyHandlers[i]()) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if any back handlers are registered along the given active node path.
     *
     * This method is Compose-observable: reading it inside a snapshot will subscribe
     * the reader to handler-set changes via the internal version counter.
     *
     * @param activeNodePath The path from the active leaf to the root.
     * @return `true` if at least one handler is registered for any key in the path.
     */
    fun hasHandlers(activeNodePath: List<NodeKey>): Boolean {
        // Read version to subscribe to snapshot changes
        @Suppress("UNUSED_VARIABLE")
        val v = version
        return activeNodePath.any { key -> handlers[key]?.isNotEmpty() == true }
    }

    /**
     * Remove all handlers registered under the given [key].
     *
     * Useful for bulk cleanup when a navigation node is removed from the tree.
     *
     * @param key The [NodeKey] whose handlers should be removed.
     */
    fun unregisterAll(key: NodeKey) {
        if (handlers.remove(key) != null) {
            version++
        }
    }
}

/**
 * CompositionLocal providing access to the back handler registry.
 *
 * The registry is used by [com.jermey.quo.vadis.core.compose.internal.navback.NavBackHandler] to register and unregister back handlers.
 * Navigation hosts (like [com.jermey.quo.vadis.core.compose.NavigationHost]) should provide a registry instance to enable
 * user-defined back handling within their scope.
 *
 * The default value creates a new registry, but navigation hosts typically provide
 * their own instance to integrate with the navigation system's back handling.
 */
val LocalBackHandlerRegistry = staticCompositionLocalOf { BackHandlerRegistry() }
