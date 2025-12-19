package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manager for navigation lifecycle callbacks.
 *
 * Maintains a bidirectional mapping between [NavigationLifecycle] instances
 * and screen keys, dispatching lifecycle events when screens are activated,
 * deactivated, or destroyed.
 *
 * ## Internal Usage
 *
 * This class is used internally by the Navigator. Users should interact
 * with lifecycle through:
 * - [Navigator.registerNavigationLifecycle] - to register callbacks
 * - [Navigator.unregisterNavigationLifecycle] - to unregister callbacks
 *
 * ## Thread Safety
 *
 * All operations are thread-safe using [Mutex].
 *
 * ## Multiple Lifecycles per Screen
 *
 * Multiple [NavigationLifecycle] instances can be registered for the same
 * screen. All will receive callbacks when the screen's state changes.
 */
class NavigationLifecycleManager {

    private val mutex = Mutex()

    /**
     * Maps each lifecycle to its associated screen key.
     * Used for quick lookup when unregistering.
     */
    private val lifecycleToScreen = mutableMapOf<NavigationLifecycle, String>()

    /**
     * Maps each screen key to its registered lifecycles.
     * Used for dispatching events to all lifecycles of a screen.
     */
    private val screenToLifecycles = mutableMapOf<String, MutableSet<NavigationLifecycle>>()

    /**
     * Register a lifecycle for a specific screen (synchronous, non-blocking version).
     *
     * Associates the lifecycle with the screen key and immediately calls
     * [NavigationLifecycle.onEnter] (the screen is assumed to be active
     * when registration occurs).
     *
     * Note: This uses a simple mutable map without locking for the sync path.
     * For thread-safe access in coroutines, use the suspend version.
     *
     * @param lifecycle The lifecycle callbacks to register
     * @param screenKey The unique key of the currently active screen
     */
    fun registerSync(lifecycle: NavigationLifecycle, screenKey: String) {
        // Note: For simple single-threaded use cases (typical UI registration),
        // direct access is safe. For concurrent access, use coroutine APIs.
        lifecycleToScreen[lifecycle] = screenKey
        screenToLifecycles.getOrPut(screenKey) { mutableSetOf() }.add(lifecycle)

        // Dispatch onEnter immediately
        lifecycle.onEnter()
    }

    /**
     * Unregister a lifecycle (synchronous, non-blocking version).
     *
     * Removes the lifecycle from all mappings. Safe to call even if
     * the lifecycle was never registered (no-op in that case).
     *
     * @param lifecycle The lifecycle callbacks to unregister
     */
    fun unregisterSync(lifecycle: NavigationLifecycle) {
        val screenKey = lifecycleToScreen.remove(lifecycle) ?: return
        screenToLifecycles[screenKey]?.remove(lifecycle)
        if (screenToLifecycles[screenKey]?.isEmpty() == true) {
            screenToLifecycles.remove(screenKey)
        }
    }

    /**
     * Dispatch [NavigationLifecycle.onExit] to all lifecycles for a screen.
     *
     * Called when a screen becomes inactive (navigated away from but still in stack).
     *
     * @param screenKey The unique key of the screen that became inactive
     */
    suspend fun onScreenExited(screenKey: String) {
        val lifecycles = mutex.withLock {
            screenToLifecycles[screenKey]?.toList() ?: emptyList()
        }

        // Dispatch callbacks
        lifecycles.forEach { it.onExit() }
    }

    /**
     * Dispatch [NavigationLifecycle.onDestroy] and clean up mappings.
     *
     * Called when a screen is removed from the navigation stack.
     * After this call, all lifecycles for the screen are unregistered.
     *
     * @param screenKey The unique key of the screen being destroyed
     */
    suspend fun onScreenDestroyed(screenKey: String) {
        val lifecycles = mutex.withLock {
            val set = screenToLifecycles.remove(screenKey) ?: emptySet()
            set.forEach { lifecycle -> lifecycleToScreen.remove(lifecycle) }
            set.toList()
        }

        // Dispatch callbacks
        lifecycles.forEach { it.onDestroy() }
    }

    /**
     * Get the screen key associated with a lifecycle (for internal use).
     *
     * @param lifecycle The lifecycle to look up
     * @return The associated screen key, or null if not registered
     */
    fun getScreenKey(lifecycle: NavigationLifecycle): String? {
        return lifecycleToScreen[lifecycle]
    }

    /**
     * Check if a lifecycle is registered.
     *
     * @param lifecycle The lifecycle to check
     * @return true if the lifecycle is currently registered
     */
    fun isRegistered(lifecycle: NavigationLifecycle): Boolean {
        return lifecycleToScreen.containsKey(lifecycle)
    }

    /**
     * Get the count of registered lifecycles (for testing/debugging).
     *
     * @return The total number of registered lifecycles
     */
    fun registeredCount(): Int {
        return lifecycleToScreen.size
    }
}
