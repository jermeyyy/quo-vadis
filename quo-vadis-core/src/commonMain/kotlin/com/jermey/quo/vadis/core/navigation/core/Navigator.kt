package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.NavigationConfig
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
 * All mutations are performed through [TreeMutator] operations, ensuring
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

    /**
     * The current transition state for animations.
     *
     * During navigation, this holds transition metadata for animation
     * coordination. Observe this to drive animations in the renderer.
     */
    val transitionState: StateFlow<TransitionState>

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
     * Current transition animation (null if idle).
     *
     * Derived from [transitionState] for convenience.
     */
    val currentTransition: StateFlow<NavigationTransition?>

    /**
     * Whether back navigation is possible from the current state.
     */
    val canNavigateBack: StateFlow<Boolean>

    // =========================================================================
    // RESULT AND LIFECYCLE MANAGEMENT
    // =========================================================================

    /**
     * Manager for navigation result passing between screens.
     *
     * Used internally by [navigateForResult] and [navigateBackWithResult]
     * extension functions. Not typically accessed directly.
     */
    val resultManager: NavigationResultManager

    /**
     * Manager for navigation lifecycle callbacks.
     *
     * Used internally by [registerNavigationLifecycle] and
     * [unregisterNavigationLifecycle] extension functions.
     * Not typically accessed directly.
     */
    val lifecycleManager: NavigationLifecycleManager

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
    // TAB NAVIGATION
    // =========================================================================

    // =========================================================================
    // PANE NAVIGATION
    // =========================================================================

    /**
     * Navigate to a destination within a specific pane.
     *
     * This is the primary API for master-detail and supporting pane patterns.
     * The destination is pushed onto the target pane's stack.
     *
     * @param role Target pane role (Primary, Supporting, Extra)
     * @param destination Destination to navigate to
     * @param switchFocus If true, also changes activePaneRole to target role
     * @param transition Optional transition animation
     *
     * @throws IllegalStateException if no PaneNode found in current state
     * @throws IllegalArgumentException if role is not configured in the PaneNode
     */
    @Deprecated(
        message = "navigateToPane() is deprecated. Use navigate() with a destination instead. " +
            "Navigate will automatically target the correct pane based on destination.",
        replaceWith = ReplaceWith("navigate(destination)"),
        level = DeprecationLevel.WARNING
    )
    fun navigateToPane(
        role: PaneRole,
        destination: NavDestination,
        switchFocus: Boolean = true,
        transition: NavigationTransition? = null
    )

    /**
     * Switch the active (focused) pane without navigation.
     *
     * Changes which pane receives navigation focus. On compact screens,
     * this determines which pane is visible.
     *
     * @param role Pane role to activate
     * @throws IllegalStateException if no PaneNode found
     * @throws IllegalArgumentException if role is not configured
     */
    @Deprecated(
        message = "switchPane() is deprecated. Use navigate() with a destination instead. " +
            "Navigate will automatically switch to the pane containing the destination.",
        replaceWith = ReplaceWith("navigate(destination)"),
        level = DeprecationLevel.WARNING
    )
    fun switchPane(role: PaneRole)

    /**
     * Check if a pane role is available in the current state.
     *
     * @param role Pane role to check
     * @return true if the role is configured in the current PaneNode
     */
    fun isPaneAvailable(role: PaneRole): Boolean

    /**
     * Get the current content of a specific pane.
     *
     * @param role Pane role to query
     * @return The NavNode content of the pane, or null if role not configured
     */
    fun paneContent(role: PaneRole): NavNode?

    /**
     * Navigate back within a specific pane.
     *
     * Pops from the specified pane's stack regardless of which pane is active.
     *
     * @param role Pane role to pop from
     * @return true if navigation occurred, false if pane stack was empty
     */
    fun navigateBackInPane(role: PaneRole): Boolean

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

    /**
     * Get the deep link handler to register patterns.
     *
     * @return The configured DeepLinkHandler
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Use getDeepLinkRegistry() instead for the new unified API",
        replaceWith = ReplaceWith("getDeepLinkRegistry()"),
        level = DeprecationLevel.WARNING
    )
    fun getDeepLinkHandler(): DeepLinkHandler

    // =========================================================================
    // STATE MANIPULATION (Advanced)
    // =========================================================================

    /**
     * Update the navigation state directly.
     *
     * Use with caution - prefer higher-level navigation methods.
     * This is primarily for use by [TreeMutator] and state restoration.
     *
     * @param newState The new navigation tree
     * @param transition Optional transition for animation
     */
    fun updateState(newState: NavNode, transition: NavigationTransition? = null)

    // =========================================================================
    // TRANSITION CONTROL
    // =========================================================================

    /**
     * Update transition progress during animations.
     *
     * Called by the renderer to update animation progress.
     *
     * @param progress Animation progress from 0.0 to 1.0
     */
    fun updateTransitionProgress(progress: Float)

    /**
     * Start a predictive back gesture.
     *
     * Called when the user initiates a back gesture.
     */
    fun startPredictiveBack()

    /**
     * Update predictive back gesture progress.
     *
     * @param progress Gesture progress from 0.0 to 1.0
     * @param touchX Normalized x-coordinate of touch (0-1)
     * @param touchY Normalized y-coordinate of touch (0-1)
     */
    fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float)

    /**
     * Cancel the predictive back gesture.
     *
     * Called when the user releases the gesture without completing it.
     */
    fun cancelPredictiveBack()

    /**
     * Commit the predictive back gesture.
     *
     * Called when the user completes the back gesture.
     */
    fun commitPredictiveBack()

    /**
     * Complete the current transition animation.
     *
     * Called when the animation finishes.
     */
    fun completeTransition()
}
