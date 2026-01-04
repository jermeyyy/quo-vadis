package com.jermey.quo.vadis.core.registry

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Registry for user-defined back handlers.
 *
 * Maintains a stack of handlers that are consulted in LIFO order during back navigation.
 * When a back event occurs, handlers are consulted from most recently registered to oldest.
 * The first handler that returns `true` consumes the event and stops propagation.
 *
 * This registry is typically accessed via [LocalBackHandlerRegistry] and used internally
 * by [com.jermey.quo.vadis.core.navigation.compose.navback.NavBackHandler] composable. Direct usage is rarely needed.
 *
 * Thread Safety: This class is not thread-safe and should only be accessed from the
 * main/UI thread, which is the expected usage pattern in Compose.
 */
class BackHandlerRegistry {
    private val handlers = mutableListOf<() -> Boolean>()

    /**
     * Register a back handler.
     *
     * @param handler A function that handles back events. Should return `true` if the
     *                back event was consumed and should not propagate further, or `false`
     *                to allow the event to propagate to the next handler.
     * @return A function that unregisters this handler when called. The unregister function
     *         is idempotent and safe to call multiple times.
     */
    fun register(handler: () -> Boolean): () -> Unit {
        handlers.add(handler)
        return { handlers.remove(handler) }
    }

    /**
     * Attempt to handle a back event with registered handlers.
     *
     * Handlers are consulted in LIFO order (last registered first, i.e., innermost
     * composable first). The first handler that returns `true` stops propagation.
     *
     * @return `true` if any handler consumed the back event, `false` if no handlers
     *         consumed it or no handlers are registered.
     */
    fun handleBack(): Boolean {
        // Iterate in reverse order (LIFO) - innermost handlers get first chance
        for (i in handlers.lastIndex downTo 0) {
            if (handlers[i]()) {
                return true
            }
        }
        return false
    }

    /**
     * Check if any back handlers are registered.
     *
     * @return `true` if at least one handler is registered, `false` otherwise.
     */
    fun hasHandlers(): Boolean = handlers.isNotEmpty()
}

/**
 * CompositionLocal providing access to the back handler registry.
 *
 * The registry is used by [com.jermey.quo.vadis.core.navigation.compose.navback.NavBackHandler] to register and unregister back handlers.
 * Navigation hosts (like [com.jermey.quo.vadis.core.navigation.compose.NavigationHost]) should provide a registry instance to enable
 * user-defined back handling within their scope.
 *
 * The default value creates a new registry, but navigation hosts typically provide
 * their own instance to integrate with the navigation system's back handling.
 */
val LocalBackHandlerRegistry = staticCompositionLocalOf { BackHandlerRegistry() }
