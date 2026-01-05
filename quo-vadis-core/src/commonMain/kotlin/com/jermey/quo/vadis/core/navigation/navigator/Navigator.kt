package com.jermey.quo.vadis.core.navigation.navigator

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import kotlinx.coroutines.flow.StateFlow

/**
 * Central navigation controller for the application.
 *
 * The Navigator manages navigation state as an immutable tree ([NavNode]),
 * coordinating with navigation graphs and handling deep links.
 *
 * ## State Model
 *
 * Navigation state is represented as a tree structure where:
 * - [StackNode] represents linear navigation history
 * - [TabNode] represents parallel tab-based navigation
 * - [PaneNode] represents adaptive multi-pane layouts
 * - [ScreenNode] represents individual destinations
 *
 * All mutations are performed through [com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator] operations, ensuring
 * immutability, thread safety, and structural sharing.
 *
 * ## Usage
 *
 * ```kotlin
 * val navigator = TreeNavigator(
 *     config = GeneratedNavigationConfig,
 *     initialState = buildInitialState()
 * )
 *
 * // Navigate
 * navigator.navigate(DetailDestination("123"))
 *
 * // Navigate back
 * navigator.navigateBack()
 *
 * // Observe state
 * navigator.state.collect { navNode ->
 *     // React to state changes
 * }
 * ```
 *
 * Thread-safe and designed to work with MVI architecture pattern.
 *
 * Implements [ParentNavigator] to support hierarchical navigation with child navigators.
 *
 * ## Extended Capabilities
 *
 * For specialized functionality, cast to extension interfaces:
 * - [PaneNavigator] - For pane-specific operations via [asPaneNavigator]
 * - [TransitionController] - For transition/animation control (internal)
 * - [ResultCapable] - For result passing support (internal)
 *
 * @see PaneNavigator
 * @see TransitionController
 * @see ResultCapable
 */
@Stable
interface Navigator : BackPressHandler {

    // =========================================================================
    // TREE-BASED STATE
    // =========================================================================

    /**
     * The current navigation state as an immutable tree.
     *
     * This is the single source of truth for all navigation state.
     * UI components observe this flow to render the appropriate content.
     */
    val state: StateFlow<NavNode>

    // =========================================================================
    // DERIVED CONVENIENCE PROPERTIES
    // =========================================================================

    /**
     * The currently active destination (deepest active ScreenNode).
     *
     * This is derived from [state] for convenience.
     */
    val currentDestination: StateFlow<NavDestination?>

    /**
     * The previous destination before the current one.
     *
     * Derived from the active stack's second-to-last entry.
     * Useful for determining if back navigation is semantically meaningful.
     */
    val previousDestination: StateFlow<NavDestination?>

    /**
     * Whether back navigation is possible from the current state.
     */
    val canNavigateBack: StateFlow<Boolean>

    // =========================================================================
    // CONFIGURATION
    // =========================================================================

    /**
     * The navigation configuration.
     *
     * Provides access to all registries for rendering and navigation.
     * NavigationHost uses this to resolve screen content, transitions,
     * and container wrappers.
     *
     * This property consolidates all navigation-related configuration,
     * eliminating the need to pass config separately to NavigationHost.
     *
     * @see NavigationConfig
     */
    val config: NavigationConfig

    // =========================================================================
    // NAVIGATION OPERATIONS
    // ===========================================================================

    /**
     * Navigate to a destination with optional transition.
     *
     * Pushes the destination onto the deepest active stack.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation (defaults to destination's transition)
     */
    fun navigate(
        destination: NavDestination,
        transition: NavigationTransition? = null
    )

    /**
     * Navigate back in the active stack.
     *
     * @return true if navigation was successful, false if at root
     */
    fun navigateBack(): Boolean

    /**
     * Navigate to a destination and clear the backstack up to a certain point.
     *
     * @param destination The destination to navigate to
     * @param clearRoute Route to clear up to (null clears nothing)
     * @param inclusive If true, also remove the destination matching clearRoute
     */
    fun navigateAndClearTo(
        destination: NavDestination,
        clearRoute: String? = null,
        inclusive: Boolean = false
    )

    /**
     * Navigate to a destination and replace the current one.
     *
     * @param destination The replacement destination
     * @param transition Optional transition animation
     */
    fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition? = null)

    /**
     * Navigate to a destination and clear the entire active stack.
     *
     * @param destination The destination to set as the new root
     */
    fun navigateAndClearAll(destination: NavDestination)

    // =========================================================================
    // DEEP LINK & GRAPH REGISTRATION
    // =========================================================================

    /**
     * Handle a deep link URI string and navigate to the matched destination.
     *
     * @param uri The deep link URI string (e.g., "app://profile/123")
     * @return true if navigation occurred, false if no match
     */
    fun handleDeepLink(uri: String): Boolean

    /**
     * Handle deep link navigation.
     *
     * @param deepLink The deep link to process
     */
    fun handleDeepLink(deepLink: DeepLink)

    /**
     * Get the deep link registry for pattern registration and resolution.
     *
     * @return The configured DeepLinkRegistry
     */
    fun getDeepLinkRegistry(): DeepLinkRegistry

    // =========================================================================
    // STATE MANIPULATION (Advanced)
    // =========================================================================

    /**
     * Update the navigation state directly.
     *
     * Use with caution - prefer higher-level navigation methods.
     * This is primarily for use by [com.jermey.quo.vadis.core.navigation.internal.tree.TreeMutator] and state restoration.
     *
     * @param newState The new navigation tree
     * @param transition Optional transition for animation
     */
    fun updateState(newState: NavNode, transition: NavigationTransition? = null)
}