package com.jermey.quo.vadis.core.navigation.core

/**
 * Register lifecycle callbacks for a specific screen.
 *
 * Associates the lifecycle with the specified screen key. The key should be
 * obtained from [LocalScreenNode] in Compose or passed explicitly.
 *
 * ## Usage with Compose
 *
 * ```kotlin
 * @Composable
 * fun MyScreen(navigator: Navigator) {
 *     val screenKey = LocalScreenNode.current?.key ?: return
 *     val scope = rememberCoroutineScope()
 *     val container = remember(screenKey) {
 *         MyContainer(navigator, scope, screenKey)
 *     }
 *     // ...
 * }
 *
 * class MyContainer(
 *     private val navigator: Navigator,
 *     private val coroutineScope: CoroutineScope,
 *     screenKey: String
 * ) : NavigationLifecycle {
 *
 *     init {
 *         navigator.registerNavigationLifecycle(this, screenKey)
 *     }
 *
 *     override fun onEnter() { /* Called immediately after registration */ }
 *     override fun onExit() { /* Called when navigating away */ }
 *     override fun onDestroy() {
 *         coroutineScope.cancel()
 *         navigator.unregisterNavigationLifecycle(this)
 *     }
 * }
 * ```
 *
 * ## Lifecycle Callbacks
 *
 * - [NavigationLifecycle.onEnter] is called immediately (screen is active)
 * - [NavigationLifecycle.onExit] is called when another screen becomes active
 * - [NavigationLifecycle.onDestroy] is called when screen is removed from stack
 *
 * @param lifecycle The lifecycle callbacks to register
 * @param screenKey The unique key of the screen to associate with
 */
fun Navigator.registerNavigationLifecycle(lifecycle: NavigationLifecycle, screenKey: String) {
    println("Navigator.registerNavigationLifecycle: Registering lifecycle for screenKey=$screenKey")
    lifecycleManager.registerSync(lifecycle, screenKey)
}

/**
 * Unregister lifecycle callbacks.
 *
 * The Navigator finds and removes the lifecycle from its registered screen.
 * Safe to call even if the lifecycle was never registered (no-op).
 *
 * This should typically be called in [NavigationLifecycle.onDestroy]:
 *
 * ```kotlin
 * override fun onDestroy() {
 *     coroutineScope.cancel()
 *     navigator.unregisterNavigationLifecycle(this)
 * }
 * ```
 *
 * @param lifecycle The lifecycle callbacks to unregister
 */
fun Navigator.unregisterNavigationLifecycle(lifecycle: NavigationLifecycle) {
    println("Navigator.unregisterNavigationLifecycle: Unregistering lifecycle")
    lifecycleManager.unregisterSync(lifecycle)
}
