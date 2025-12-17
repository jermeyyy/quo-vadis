package com.jermey.navplayground.demo.container

import com.jermey.quo.vadis.core.navigation.core.NavigationLifecycle
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.registerNavigationLifecycle
import com.jermey.quo.vadis.core.navigation.core.unregisterNavigationLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Base container class that integrates with Navigator lifecycle.
 *
 * Creates and manages its own [CoroutineScope] with [SupervisorJob] that:
 * - Survives navigation (not tied to Compose lifecycle)
 * - Uses [Dispatchers.Default] for business logic
 * - Is cancelled automatically in [onDestroy]
 *
 * ## Usage with Compose
 *
 * ```kotlin
 * @Composable
 * fun MyScreen(navigator: Navigator) {
 *     val screenKey = LocalScreenNode.current?.key ?: return
 *     val container = remember(screenKey) {
 *         MyContainer(navigator, screenKey)
 *     }
 *     // Use container...
 * }
 *
 * class MyContainer(
 *     navigator: Navigator,
 *     screenKey: String
 * ) : BaseContainer(navigator, screenKey) {
 *
 *     override fun onEnter() {
 *         // Screen became active - start loading data
 *     }
 *
 *     override fun onExit() {
 *         // Screen became inactive - pause operations
 *     }
 * }
 * ```
 *
 * @param navigator The Navigator instance for navigation operations
 * @param screenKey The unique screen key from LocalScreenNode.current?.key
 */
abstract class BaseContainer(
    protected val navigator: Navigator,
    protected val screenKey: String,
) : NavigationLifecycle {

    /**
     * Container-owned coroutine scope for business logic.
     *
     * Uses [SupervisorJob] so child coroutine failures don't cancel siblings.
     * Uses [Dispatchers.Default] for CPU-bound work.
     * Survives navigation - not tied to Compose composition lifecycle.
     */
    protected val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        navigator.registerNavigationLifecycle(this, screenKey)
    }

    /**
     * Called when the navigation to this screen completes (screen becomes active).
     *
     * Override to start loading data or begin observing data sources.
     */
    override fun onEnter() {
        // Can be overridden by subclasses
    }

    /**
     * Called when navigating away from this screen (screen becomes inactive).
     *
     * Override to pause expensive operations or save temporary state.
     */
    override fun onExit() {
        // Can be overridden by subclasses
    }

    /**
     * Called when this screen is being removed from the navigation stack.
     *
     * Automatically cancels the coroutine scope and unregisters lifecycle.
     * Override to add additional cleanup, but always call super.
     */
    override fun onDestroy() {
        scope.cancel()
        navigator.unregisterNavigationLifecycle(this)
    }
}
