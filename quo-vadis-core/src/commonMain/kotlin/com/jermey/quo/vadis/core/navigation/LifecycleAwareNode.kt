package com.jermey.quo.vadis.core.navigation

/**
 * Interface for navigation nodes that provide lifecycle and state management.
 *
 * Both container nodes (TabNode, PaneNode) and screen nodes (ScreenNode)
 * implement this to provide consistent lifecycle behavior across the navigation tree.
 *
 * This interface is designed to be implemented alongside [com.jermey.quo.vadis.core.NavNode], not as a
 * replacement for it. The sealed [com.jermey.quo.vadis.core.NavNode] hierarchy handles navigation tree
 * structure, while [LifecycleAwareNode] handles runtime lifecycle state.
 *
 * ## Lifecycle States
 *
 * Navigation nodes transition through the following states:
 * - **Detached**: Node exists but is not part of the navigation tree
 * - **Attached**: Node is in the navigation tree but not currently displayed
 * - **Displayed**: Node is actively being displayed to the user
 *
 * ## State Transitions
 *
 * ```
 * [Created] -> attachToNavigator() -> [Attached] -> attachToUI() -> [Displayed]
 *                                          ^                            |
 *                                          |                            v
 *                                          +---- detachFromUI() --------+
 *                                          |
 *                                          v
 *                              detachFromNavigator() -> [Destroyed]
 * ```
 *
 * ## Saved State
 *
 * The [composeSavedState] property stores Compose `rememberSaveable` state
 * for this node, enabling state preservation across configuration changes
 * and process death.
 *
 * ## Lifecycle Callbacks
 *
 * External components (like MVI containers) can register for lifecycle events
 * via [addOnDestroyCallback]. Callbacks are invoked when the node is destroyed
 * (both detached from navigator and not displayed).
 */
interface LifecycleAwareNode {

    /**
     * Whether this node is currently attached to the navigator tree.
     *
     * A node becomes attached when it's added to the navigation tree
     * via [attachToNavigator] and remains attached until [detachFromNavigator]
     * is called.
     */
    val isAttachedToNavigator: Boolean

    /**
     * Whether this node is currently being displayed.
     *
     * A node becomes displayed when [attachToUI] is called (typically
     * when its composable enters composition) and stops being displayed
     * when [detachFromUI] is called.
     */
    val isDisplayed: Boolean

    /**
     * Saved state for Compose `rememberSaveable`.
     *
     * This map stores the saved state from `SaveableStateRegistry.performSave()`
     * and is used to restore state via `SaveableStateRegistry` on recomposition.
     *
     * The map is:
     * - `null` when no state has been saved yet
     * - Populated when the node's UI is disposed while the node remains in the tree
     * - Cleared when state is consumed during restoration
     */
    var composeSavedState: Map<String, List<Any?>>?

    /**
     * Called when this node is added to the navigator tree.
     *
     * This transition happens when:
     * - A new screen is pushed onto a stack
     * - A new tab/pane container is created
     * - Navigation tree is restored from saved state
     *
     * After this call, [isAttachedToNavigator] will be `true`.
     *
     * @throws IllegalStateException if already attached
     */
    fun attachToNavigator()

    /**
     * Called when this node's UI enters composition.
     *
     * This transition happens when the node's content composable
     * starts being displayed. After this call, [isDisplayed] will be `true`.
     *
     * @throws IllegalStateException if not attached to navigator
     */
    fun attachToUI()

    /**
     * Called when this node's UI leaves composition.
     *
     * This transition happens when:
     * - Another screen is pushed on top
     * - Tab is switched to another tab
     * - Screen is being animated out
     *
     * After this call, [isDisplayed] will be `false`.
     * If the node was already detached from navigator, this triggers cleanup.
     */
    fun detachFromUI()

    /**
     * Called when this node is removed from the navigator tree.
     *
     * This transition happens when:
     * - A screen is popped from the stack
     * - A tab container is removed
     * - Navigation tree is being cleared
     *
     * After this call, [isAttachedToNavigator] will be `false`.
     * If the node is not being displayed, this triggers cleanup.
     */
    fun detachFromNavigator()

    /**
     * Registers a callback to be invoked when this node is destroyed.
     *
     * A node is destroyed when it is both:
     * - Detached from the navigator tree
     * - Not being displayed
     *
     * This is used by MVI containers to close their coroutine scopes
     * and Koin scopes when the screen/container is destroyed.
     *
     * @param callback The callback to invoke on destruction
     */
    fun addOnDestroyCallback(callback: () -> Unit)

    /**
     * Removes a previously registered destroy callback.
     *
     * @param callback The callback to remove
     */
    fun removeOnDestroyCallback(callback: () -> Unit)
}
