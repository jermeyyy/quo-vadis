package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * A state-driven navigator that provides a high-level navigation API on top of [StateBackStack].
 *
 * This navigator follows the composition pattern, wrapping a [StateBackStack] to provide
 * convenient navigation methods while exposing the underlying state for Compose observation.
 * Unlike the flow-based [Navigator] interface, this implementation uses Compose's state
 * system for reactive updates.
 *
 * Key features:
 * - **Compose-native**: All properties are observable in Compose without collecting flows
 * - **Simple API**: Common navigation patterns like navigate, navigateBack, replace, etc.
 * - **Full backstack access**: Direct access to the underlying [StateBackStack] for advanced use cases
 *
 * Example usage:
 * ```kotlin
 * val navigator = StateNavigator()
 *
 * // Navigate forward
 * navigator.navigate(HomeDestination)
 * navigator.navigate(DetailDestination(id = "123"))
 *
 * // Navigate back
 * if (navigator.canGoBack) {
 *     navigator.navigateBack()
 * }
 *
 * // Replace current destination
 * navigator.navigateAndReplace(NewDestination)
 *
 * // Clear and navigate
 * navigator.navigateAndClearAll(LoginDestination)
 * ```
 *
 * @param backStack The underlying backstack to use. Defaults to a new empty [StateBackStack].
 *
 * @see StateBackStack for the underlying state container
 * @see Navigator for the flow-based navigator interface
 */
@Stable
class StateNavigator(
    private val backStack: StateBackStack = StateBackStack()
) {

    /**
     * The list of backstack entries.
     *
     * This is the same list as [StateBackStack.entries], exposed for direct observation
     * in Compose. Modifications should be made through the navigation methods or
     * the [backStack] property for complex operations.
     */
    val entries: SnapshotStateList<BackStackEntry>
        get() = backStack.entries

    /**
     * The current destination being displayed.
     *
     * Returns `null` if the backstack is empty. This property is derived from
     * the current [BackStackEntry], extracting just the [Destination] for convenience.
     */
    val currentDestination: Destination? by derivedStateOf {
        backStack.current?.destination
    }

    /**
     * The previous destination in the backstack.
     *
     * Returns `null` if there are fewer than 2 entries. Useful for implementing
     * peek previews or understanding the navigation history.
     */
    val previousDestination: Destination? by derivedStateOf {
        backStack.previous?.destination
    }

    /**
     * Whether backward navigation is possible.
     *
     * Delegates to the underlying [StateBackStack.canGoBack]. Returns `true`
     * if there are at least 2 entries in the backstack.
     */
    val canGoBack: Boolean
        get() = backStack.canGoBack

    /**
     * The current backstack entry.
     *
     * Returns `null` if the backstack is empty. Provides access to the full
     * [BackStackEntry] including ID, saved state, and transition information.
     */
    val currentEntry: BackStackEntry?
        get() = backStack.current

    /**
     * The number of entries in the backstack.
     */
    val stackSize: Int
        get() = backStack.size

    /**
     * Navigates to a new destination by pushing it onto the backstack.
     *
     * The new destination becomes the [currentDestination] and the previous
     * destination becomes [previousDestination].
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation for this navigation
     */
    fun navigate(destination: Destination, transition: NavigationTransition? = null) {
        backStack.push(destination, transition)
    }

    /**
     * Navigates back by removing the current destination from the backstack.
     *
     * @return `true` if navigation was successful (there was a destination to pop),
     *         `false` if the backstack was empty or had only one entry
     */
    fun navigateBack(): Boolean {
        if (!canGoBack) return false
        backStack.pop()
        return true
    }

    /**
     * Navigates to a destination, replacing the current one.
     *
     * The new destination takes the place of the current destination without
     * adding to the backstack depth. If the backstack is empty, this behaves
     * like [navigate].
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation for this navigation
     */
    fun navigateAndReplace(destination: Destination, transition: NavigationTransition? = null) {
        if (backStack.isNotEmpty) {
            backStack.pop()
        }
        backStack.push(destination, transition)
    }

    /**
     * Navigates to a destination after clearing the entire backstack.
     *
     * The specified destination becomes the only entry in the backstack,
     * effectively resetting the navigation history. This is commonly used
     * for scenarios like returning to a login screen or resetting to home.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation for this navigation
     */
    fun navigateAndClearAll(destination: Destination, transition: NavigationTransition? = null) {
        backStack.clear()
        backStack.push(destination, transition)
    }

    /**
     * Clears the entire backstack.
     *
     * After this operation, [currentDestination] will be `null` and [canGoBack]
     * will be `false`. Use with caution as this removes all navigation history.
     */
    fun clear() {
        backStack.clear()
    }

    /**
     * Provides direct access to the underlying [StateBackStack].
     *
     * Use this for advanced backstack manipulation that isn't covered by
     * the convenience methods, such as inserting entries, swapping positions,
     * or accessing the [StateBackStack.entriesFlow].
     *
     * @return The underlying [StateBackStack] instance
     */
    fun getBackStack(): StateBackStack = backStack
}
