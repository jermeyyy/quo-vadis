package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
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
 * val navigator = TreeNavigator()
 * navigator.setStartDestination(HomeDestination)
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
interface Navigator : ParentNavigator {

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
    val transitionState: StateFlow<LegacyTransitionState>

    // =========================================================================
    // DERIVED CONVENIENCE PROPERTIES
    // =========================================================================

    /**
     * The currently active destination (deepest active ScreenNode).
     *
     * This is derived from [state] for convenience.
     */
    val currentDestination: StateFlow<Destination?>

    /**
     * The previous destination before the current one.
     *
     * Derived from the active stack's second-to-last entry.
     * Useful for determining if back navigation is semantically meaningful.
     */
    val previousDestination: StateFlow<Destination?>

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
    // NAVIGATION OPERATIONS
    // =========================================================================

    /**
     * Navigate to a destination with optional transition.
     *
     * Pushes the destination onto the deepest active stack.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation (defaults to destination's transition)
     */
    fun navigate(
        destination: Destination,
        transition: NavigationTransition? = null
    )

    /**
     * Navigate back in the active stack.
     *
     * @return true if navigation was successful, false if at root
     */
    fun navigateBack(): Boolean

    /**
     * Navigate up in the hierarchy (semantic equivalent of back).
     *
     * Default implementation delegates to [navigateBack].
     */
    fun navigateUp(): Boolean = navigateBack()

    /**
     * Navigate to a destination and clear the backstack up to a certain point.
     *
     * @param destination The destination to navigate to
     * @param clearRoute Route to clear up to (null clears nothing)
     * @param inclusive If true, also remove the destination matching clearRoute
     */
    @Deprecated(
        message = "navigateAndClearTo with string route is replaced by type-safe version. Use navigateAndClear(destination, clearUpTo::class, inclusive).",
        level = DeprecationLevel.WARNING
    )
    fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String? = null,
        inclusive: Boolean = false
    )

    /**
     * Navigate to a destination and replace the current one.
     *
     * @param destination The replacement destination
     * @param transition Optional transition animation
     */
    fun navigateAndReplace(destination: Destination, transition: NavigationTransition? = null)

    /**
     * Navigate to a destination and clear the entire active stack.
     *
     * @param destination The destination to set as the new root
     */
    fun navigateAndClearAll(destination: Destination)

    // =========================================================================
    // TAB NAVIGATION
    // =========================================================================

    /**
     * Switch to a different tab in the active TabNode.
     *
     * @param index The index of the tab to switch to
     * @throws IllegalStateException if no TabNode in current state
     * @throws IndexOutOfBoundsException if index is invalid
     */
    fun switchTab(index: Int)

    /**
     * The currently active tab index, or null if no TabNode exists.
     */
    val activeTabIndex: Int?

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
    fun navigateToPane(
        role: PaneRole,
        destination: Destination,
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

    /**
     * Clear a pane's navigation stack back to its root.
     *
     * @param role Pane role to clear
     * @throws IllegalStateException if no PaneNode found
     * @throws IllegalArgumentException if role is not configured
     */
    fun clearPane(role: PaneRole)

    // =========================================================================
    // DEEP LINK & GRAPH REGISTRATION
    // =========================================================================

    /**
     * Handle deep link navigation.
     *
     * @param deepLink The deep link to process
     */
    fun handleDeepLink(deepLink: DeepLink)

    /**
     * Register a navigation graph for modular navigation.
     *
     * @param graph The navigation graph to register
     */
    @Deprecated(
        message = "registerGraph() is no longer needed. Use rememberNavigator(navTree) with KSP-generated tree.",
        level = DeprecationLevel.WARNING
    )
    fun registerGraph(graph: NavigationGraph)

    /**
     * Set the start destination (resets navigation state).
     *
     * Creates a fresh stack with the given destination as root.
     *
     * @param destination The starting destination
     */
    @Deprecated(
        message = "setStartDestination() is no longer needed. Start destination is defined in @Stack/@Tab/@Pane annotations.",
        level = DeprecationLevel.WARNING
    )
    fun setStartDestination(destination: Destination)

    /**
     * Get the deep link handler to register patterns.
     *
     * @return The configured DeepLinkHandler
     */
    fun getDeepLinkHandler(): DeepLinkHandler

    // =========================================================================
    // CHILD NAVIGATOR SUPPORT
    // =========================================================================

    /**
     * Set the active child navigator for back press delegation.
     *
     * When a child navigator is set, back press events will be delegated
     * to the child first before being handled by this navigator.
     *
     * @param child The child navigator to delegate to, or null to clear
     */
    fun setActiveChild(child: BackPressHandler?)

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

// =========================================================================
// CONVENIENCE EXTENSION FUNCTIONS
// =========================================================================

/**
 * Navigate to a pane and switch focus in one call.
 *
 * This is the most common pattern for master-detail navigation.
 *
 * @param role Target pane role
 * @param destination Destination to show in the pane
 */
fun Navigator.showInPane(role: PaneRole, destination: Destination) {
    navigateToPane(role, destination, switchFocus = true)
}

/**
 * Show detail content while keeping focus on the current pane.
 *
 * Useful for "peek" scenarios or preloading content.
 *
 * @param role Target pane role
 * @param destination Destination to preload
 */
fun Navigator.preloadPane(role: PaneRole, destination: Destination) {
    navigateToPane(role, destination, switchFocus = false)
}

/**
 * Show detail content in the Supporting pane.
 *
 * Typed extension for the common master-detail pattern.
 *
 * @param destination The detail destination to display
 */
fun Navigator.showDetail(destination: Destination) {
    navigateToPane(PaneRole.Supporting, destination, switchFocus = true)
}

/**
 * Return focus to the Primary pane.
 *
 * Common operation after viewing detail content.
 */
fun Navigator.showPrimary() {
    switchPane(PaneRole.Primary)
}

/**
 * The current active pane role, or null if no PaneNode in state.
 */
val Navigator.activePaneRole: PaneRole?
    get() = state.value.findFirst<PaneNode>()?.activePaneRole

/**
 * Whether the current state contains a PaneNode.
 */
val Navigator.hasPaneLayout: Boolean
    get() = state.value.findFirst<PaneNode>() != null
