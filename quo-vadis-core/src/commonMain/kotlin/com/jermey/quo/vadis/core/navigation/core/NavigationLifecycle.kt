package com.jermey.quo.vadis.core.navigation.core

/**
 * Lifecycle callbacks for navigation-aware components.
 *
 * These callbacks are tied to a specific screen instance (identified by internal key),
 * allowing proper lifecycle management even with multiple instances of the same
 * destination type.
 *
 * ## Usage
 *
 * Implement this interface in business logic containers (like ViewModels or MVI containers)
 * and register with the Navigator to receive lifecycle events:
 *
 * ```kotlin
 * class MyContainer(
 *     private val navigator: Navigator,
 *     private val coroutineScope: CoroutineScope
 * ) : NavigationLifecycle {
 *
 *     init {
 *         navigator.registerNavigationLifecycle(this)
 *     }
 *
 *     override fun onEnter() {
 *         // Screen became active - start loading data
 *     }
 *
 *     override fun onExit() {
 *         // Screen became inactive - pause operations
 *     }
 *
 *     override fun onDestroy() {
 *         coroutineScope.cancel()
 *         navigator.unregisterNavigationLifecycle(this)
 *     }
 * }
 * ```
 *
 * ## Lifecycle Sequence
 *
 * 1. **onEnter()** - Called when navigation completes and screen becomes active
 * 2. **onExit()** - Called when navigating away (screen remains in stack)
 * 3. **onDestroy()** - Called when screen is removed from navigation stack
 *
 * Note: [onExit] may be called multiple times if the user navigates forward and back,
 * but [onDestroy] is called only once when the screen is permanently removed.
 */
interface NavigationLifecycle {

    /**
     * Called when the navigation to this screen completes (screen becomes active).
     *
     * This is called immediately after registration if the screen is already active,
     * and subsequently whenever the user navigates back to this screen.
     *
     * Use this to:
     * - Start or resume data loading
     * - Begin observing data sources
     * - Log analytics events
     */
    fun onEnter()

    /**
     * Called when navigating away from this screen (screen becomes inactive).
     *
     * This is called when another screen becomes active but this screen
     * remains in the navigation stack (i.e., not destroyed).
     *
     * Use this to:
     * - Pause expensive operations
     * - Save temporary state
     * - Release resources that can be re-acquired in onEnter
     */
    fun onExit()

    /**
     * Called when this screen is being removed from the navigation stack.
     *
     * This is the final lifecycle callback - after this, the screen is destroyed.
     * Always unregister the lifecycle in this callback to prevent memory leaks.
     *
     * Use this to:
     * - Cancel coroutines
     * - Unregister listeners
     * - Release permanent resources
     * - Call [Navigator.unregisterNavigationLifecycle]
     */
    fun onDestroy()
}
