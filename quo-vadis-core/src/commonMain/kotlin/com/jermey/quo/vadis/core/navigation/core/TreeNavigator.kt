package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.compose.BackHandlerRegistry
import com.jermey.quo.vadis.core.navigation.compose.WindowSizeClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Tree-based implementation of Navigator using StateFlow<NavNode>.
 *
 * This implementation represents navigation state as an immutable tree structure,
 * replacing the linear backstack model with support for:
 *
 * - **Linear stacks** ([StackNode]) - Standard push/pop navigation
 * - **Tabbed navigation** ([TabNode]) - Parallel stacks with active tab tracking
 * - **Adaptive panes** ([PaneNode]) - Master-detail and multi-pane layouts
 *
 * ## State Management
 *
 * All state mutations are performed through [TreeMutator] operations, ensuring:
 * - **Immutability** - New tree instances for every state change
 * - **Thread safety** - StateFlow atomic updates
 * - **Structural sharing** - Unchanged subtrees are reused
 *
 * ## Usage
 *
 * ```kotlin
 * val navigator = TreeNavigator()
 *
 * // Set initial state
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
 * @param deepLinkHandler Handler for deep link navigation
 * @param coroutineScope Scope for derived state computations
 * @param initialState Optional initial navigation state (defaults to empty stack)
 * @property scopeRegistry Registry for scope-aware navigation. When a destination is
 *   out of the current container's scope (TabNode/PaneNode), navigation pushes to
 *   the parent stack instead of the deepest active stack. Defaults to [ScopeRegistry.Empty]
 *   which allows all destinations in all scopes (backward compatible behavior).
 */
@OptIn(ExperimentalUuidApi::class)
@Stable
class TreeNavigator(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler(),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    initialState: NavNode? = null,
    private val scopeRegistry: ScopeRegistry = ScopeRegistry.Empty
) : Navigator {

    // =========================================================================
    // TREE-BASED STATE
    // =========================================================================

    private val _state: MutableStateFlow<NavNode> = MutableStateFlow(createRootStack(initialState))

    /**
     * The current navigation state as an immutable tree.
     *
     * This is the primary source of truth for navigation state.
     * All state changes flow through this property.
     */
    override val state: StateFlow<NavNode> = _state.asStateFlow()

    private val _transitionState: MutableStateFlow<LegacyTransitionState> =
        MutableStateFlow(LegacyTransitionState.Idle)

    /**
     * The current transition state for animations.
     *
     * Observe this to coordinate navigation animations:
     *
     * ```kotlin
     * navigator.transitionState.collect { state ->
     *     when (state) {
     *         is TransitionState.Idle -> hideAnimation()
     *         is TransitionState.InProgress -> animateProgress(state.progress)
     *         is TransitionState.PredictiveBack -> handleGesture(state)
     *     }
     * }
     * ```
     */
    override val transitionState: StateFlow<LegacyTransitionState> = _transitionState.asStateFlow()

    // =========================================================================
    // DERIVED CONVENIENCE PROPERTIES
    // Backed by MutableStateFlow for synchronous updates in tests
    // =========================================================================

    private val _currentDestination = MutableStateFlow<Destination?>(
        _state.value.activeLeaf()?.destination
    )

    /**
     * The currently active destination, derived from the active leaf node.
     */
    override val currentDestination: StateFlow<Destination?> = _currentDestination.asStateFlow()

    private val _previousDestination = MutableStateFlow<Destination?>(
        computePreviousDestination(_state.value)
    )

    /**
     * The previous destination in the active stack.
     *
     * This is the destination that would be shown after a back navigation,
     * or null if there is no previous destination.
     */
    override val previousDestination: StateFlow<Destination?> = _previousDestination.asStateFlow()

    private val _canNavigateBack = MutableStateFlow(
        TreeMutator.canHandleBackNavigation(_state.value)
    )

    /**
     * Flow indicating whether back navigation is possible.
     */
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    /**
     * Updates derived state properties when the main state changes.
     * Called after every state mutation to keep derived values in sync.
     */
    private fun updateDerivedState(newState: NavNode) {
        _currentDestination.value = newState.activeLeaf()?.destination
        _previousDestination.value = computePreviousDestination(newState)
        _canNavigateBack.value = TreeMutator.canHandleBackNavigation(newState)
    }

    /**
     * Current transition (derived from transitionState).
     */
    override val currentTransition: StateFlow<NavigationTransition?> = _transitionState
        .map { transitionState ->
            when (transitionState) {
                is LegacyTransitionState.Idle -> null
                is LegacyTransitionState.InProgress -> transitionState.transition
                is LegacyTransitionState.PredictiveBack -> null
                is LegacyTransitionState.Seeking -> transitionState.transition
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // =========================================================================
    // GRAPH AND DEEP LINK MANAGEMENT
    // =========================================================================

    private val graphs = mutableMapOf<String, NavigationGraph>()

    // =========================================================================
    // BACK HANDLER REGISTRY
    // =========================================================================

    /**
     * Optional back handler registry for user-defined back handlers.
     * Set by the navigation host during composition.
     *
     * When set, [handleBackInternal] will first consult registered handlers
     * before falling back to tree-based back navigation.
     */
    var backHandlerRegistry: BackHandlerRegistry? = null

    /**
     * Current window size class for adaptive pane behavior.
     * Updated by the navigation host based on the current window size.
     *
     * When set, pane back behavior adapts:
     * - In compact mode (single pane visible), back behaves like a simple stack
     * - In expanded mode (multiple panes visible), the configured [PaneBackBehavior] applies
     *
     * When null, defaults to compact behavior for safety.
     */
    var windowSizeClass: WindowSizeClass? = null

    // =========================================================================
    // PARENT NAVIGATOR SUPPORT
    // =========================================================================

    private var _activeChild: BackPressHandler? = null

    override val activeChild: BackPressHandler?
        get() = _activeChild

    override fun setActiveChild(child: BackPressHandler?) {
        _activeChild = child
    }

    // =========================================================================
    // NAVIGATION OPERATIONS
    // =========================================================================

    /**
     * Navigate to a destination with optional transition.
     *
     * Pushes the destination onto the deepest active stack in the tree.
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation
     */
    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        val effectiveTransition = transition ?: destination.transition
        val oldState = _state.value
        val fromKey = oldState.activeLeaf()?.key

        try {
            val newState = TreeMutator.push(oldState, destination, scopeRegistry) { generateKey() }
            val toKey = newState.activeLeaf()?.key

            _state.value = newState
            updateDerivedState(newState)

            // Update transition state
            if (effectiveTransition != null) {
                _transitionState.value = LegacyTransitionState.InProgress(
                    transition = effectiveTransition,
                    progress = 0f,
                    fromKey = fromKey,
                    toKey = toKey
                )
            }
        } catch (_: IllegalStateException) {
            // No active stack found, create one
            val screenKey = generateKey()
            val stackKey = generateKey()
            val newState = StackNode(
                key = stackKey,
                parentKey = null,
                children = listOf(
                    ScreenNode(key = screenKey, parentKey = stackKey, destination = destination)
                )
            )
            _state.value = newState
            updateDerivedState(newState)

            if (effectiveTransition != null) {
                _transitionState.value = LegacyTransitionState.InProgress(
                    transition = effectiveTransition,
                    progress = 0f,
                    fromKey = fromKey,
                    toKey = screenKey
                )
            }
        }
    }

    /**
     * Navigate back in the stack.
     *
     * @return true if navigation was successful
     */
    override fun navigateBack(): Boolean {
        // Use ParentNavigator's delegation logic (tries child first, then handleBackInternal)
        return onBack()
    }

    /**
     * Handle back press for this navigator's state.
     * Implementation of ParentNavigator.handleBackInternal().
     *
     * Uses intelligent tree-based back handling:
     * 1. Check user-defined handlers first (via BackHandlerRegistry)
     * 2. Pop from active stack if possible
     * 3. For tabs: switch to initial tab if not already there
     * 4. For nested structures: cascade up the tree
     * 5. Return false to delegate to system (e.g., close app)
     */
    override fun handleBackInternal(): Boolean {
        // 1. Check user-defined handlers first
        if (backHandlerRegistry?.handleBack() == true) {
            return true
        }

        val currentState = _state.value

        // 2. Use tree-aware back handling
        return when (val result = TreeMutator.popWithTabBehavior(currentState)) {
            is TreeMutator.BackResult.Handled -> {
                updateStateWithTransition(result.newState, null)
                true
            }
            is TreeMutator.BackResult.DelegateToSystem -> false
            is TreeMutator.BackResult.CannotHandle -> {
                // Fallback to pane-specific behavior with window size awareness
                // In compact mode, treat as simple stack; in expanded mode, use configured behavior
                val isCompact = windowSizeClass?.isCompactWidth ?: true
                val popResult = TreeMutator.popPaneAdaptive(currentState, isCompact)
                when (popResult) {
                    is TreeMutator.PopResult.Popped -> {
                        updateStateWithTransition(popResult.newState, null)
                        true
                    }
                    else -> false
                }
            }
        }
    }

    /**
     * Navigate to a destination and clear the backstack up to a certain point.
     *
     * @param destination The destination to navigate to
     * @param clearRoute Route to clear up to (null clears nothing)
     * @param inclusive If true, also remove the destination at clearRoute
     */
    override fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String?,
        inclusive: Boolean
    ) {
        var newState = _state.value

        if (clearRoute != null) {
            newState = TreeMutator.popToRoute(newState, clearRoute, inclusive)
        }

        newState = TreeMutator.push(newState, destination, scopeRegistry) { generateKey() }
        updateStateWithTransition(newState, null)
    }

    /**
     * Navigate to a destination and replace the current one.
     *
     * @param destination The replacement destination
     * @param transition Optional transition animation
     */
    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        val newState = TreeMutator.replaceCurrent(_state.value, destination) { generateKey() }
        updateStateWithTransition(newState, transition)
    }

    /**
     * Navigate to a destination and clear the entire backstack.
     *
     * @param destination The destination to set as the new root
     */
    override fun navigateAndClearAll(destination: Destination) {
        val newState = TreeMutator.clearAndPush(_state.value, destination) { generateKey() }
        updateStateWithTransition(newState, null)
    }

    /**
     * Handle deep link navigation.
     *
     * @param deepLink The deep link to handle
     */
    override fun handleDeepLink(deepLink: DeepLink) {
        deepLinkHandler.handle(deepLink, this, graphs)
    }

    /**
     * Register a navigation graph for modular navigation.
     *
     * @param graph The graph to register
     */
    override fun registerGraph(graph: NavigationGraph) {
        graphs[graph.graphRoute] = graph
    }

    /**
     * Set the start destination.
     *
     * Clears all existing state and sets up a fresh stack with the given destination.
     *
     * @param destination The starting destination
     */
    override fun setStartDestination(destination: Destination) {
        val stackKey = generateKey()
        val screenKey = generateKey()

        val newState = StackNode(
            key = stackKey,
            parentKey = null,
            children = listOf(
                ScreenNode(key = screenKey, parentKey = stackKey, destination = destination)
            )
        )
        _state.value = newState
        updateDerivedState(newState)

        _transitionState.value = LegacyTransitionState.Idle
    }

    /**
     * Get the deep link handler to register patterns.
     *
     * @return The configured DeepLinkHandler
     */
    override fun getDeepLinkHandler(): DeepLinkHandler = deepLinkHandler

    // =========================================================================
    // PANE-SPECIFIC OPERATIONS
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
     *
     * Example usage:
     * ```kotlin
     * // Master-detail: show item detail in supporting pane
     * navigator.navigateToPane(PaneRole.Supporting, ItemDetailDestination(itemId))
     *
     * // Keep focus on list while loading detail
     * navigator.navigateToPane(PaneRole.Supporting, ItemDetailDestination(itemId), switchFocus = false)
     * ```
     */
    override fun navigateToPane(
        role: PaneRole,
        destination: Destination,
        switchFocus: Boolean,
        transition: NavigationTransition?
    ) {
        val currentState = _state.value
        val paneNode = currentState.findFirst<PaneNode>()
            ?: throw IllegalStateException("No PaneNode found in current navigation state")

        val newState = TreeMutator.navigateToPane(
            root = currentState,
            nodeKey = paneNode.key,
            role = role,
            destination = destination,
            switchFocus = switchFocus
        ) { generateKey() }

        updateStateWithTransition(newState, transition)
    }

    /**
     * Switch the active (focused) pane without navigation.
     *
     * Changes which pane receives navigation focus. On compact screens,
     * this determines which pane is visible.
     *
     * @param role Pane role to activate
     * @throws IllegalStateException if no PaneNode found in current state
     * @throws IllegalArgumentException if role is not configured
     *
     * Example usage:
     * ```kotlin
     * // Return focus to list pane
     * navigator.switchPane(PaneRole.Primary)
     * ```
     */
    override fun switchPane(role: PaneRole) {
        val currentState = _state.value
        val paneNode = currentState.findFirst<PaneNode>()
            ?: throw IllegalStateException("No PaneNode found in current navigation state")

        val newState = TreeMutator.switchActivePane(currentState, paneNode.key, role)
        _state.value = newState
        updateDerivedState(newState)
    }

    /**
     * Check if a pane role is available in the current state.
     *
     * @param role Pane role to check
     * @return true if the role is configured in the current PaneNode
     *
     * Example usage:
     * ```kotlin
     * if (navigator.isPaneAvailable(PaneRole.Extra)) {
     *     navigator.navigateToPane(PaneRole.Extra, SettingsDestination)
     * }
     * ```
     */
    override fun isPaneAvailable(role: PaneRole): Boolean {
        val paneNode = _state.value.findFirst<PaneNode>() ?: return false
        return paneNode.paneConfigurations.containsKey(role)
    }

    /**
     * Get the current content of a specific pane.
     *
     * @param role Pane role to query
     * @return The NavNode content of the pane, or null if role not configured
     */
    override fun paneContent(role: PaneRole): NavNode? {
        val paneNode = _state.value.findFirst<PaneNode>() ?: return null
        return paneNode.paneContent(role)
    }

    /**
     * Navigate back within a specific pane.
     *
     * Pops from the specified pane's stack regardless of which pane is active.
     *
     * @param role Pane role to pop from
     * @return true if navigation occurred, false if pane stack was empty
     *
     * Example usage:
     * ```kotlin
     * // Clear detail pane when closing detail view
     * navigator.navigateBackInPane(PaneRole.Supporting)
     * ```
     */
    override fun navigateBackInPane(role: PaneRole): Boolean {
        val currentState = _state.value
        val paneNode = currentState.findFirst<PaneNode>() ?: return false

        val newState = TreeMutator.popPane(currentState, paneNode.key, role)
        if (newState != null) {
            _state.value = newState
            updateDerivedState(newState)
            return true
        }
        return false
    }

    /**
     * Clear a pane's navigation stack back to its root.
     *
     * @param role Pane role to clear
     *
     * Example usage:
     * ```kotlin
     * // Reset detail pane when selecting new list item
     * navigator.clearPane(PaneRole.Supporting)
     * navigator.navigateToPane(PaneRole.Supporting, newDetailDestination)
     * ```
     */
    override fun clearPane(role: PaneRole) {
        val currentState = _state.value
        val paneNode = currentState.findFirst<PaneNode>()
            ?: throw IllegalStateException("No PaneNode found in current navigation state")

        val paneConfig = paneNode.paneConfigurations[role]
            ?: throw IllegalArgumentException("Pane role $role not configured")

        val paneContent = paneConfig.content
        val targetStack = when (paneContent) {
            is StackNode -> paneContent
            else -> paneContent.activeStack() ?: return
        }

        // Keep only the first child (root)
        if (targetStack.children.size <= 1) return

        val newStack = targetStack.copy(children = listOf(targetStack.children.first()))
        val newState = TreeMutator.replaceNode(currentState, targetStack.key, newStack)
        _state.value = newState
        updateDerivedState(newState)
    }

    // =========================================================================
    // TAB OPERATIONS
    // =========================================================================

    /**
     * Switch to a specific tab.
     *
     * @param index The tab index to switch to
     * @throws IllegalStateException if no TabNode in the current state
     * @throws IndexOutOfBoundsException if index is invalid
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "switchTab() is deprecated. Use navigate() with a destination instead. " +
            "Navigate will automatically switch to the tab containing the destination.",
        replaceWith = ReplaceWith("navigate(destination)"),
        level = DeprecationLevel.WARNING
    )
    override fun switchTab(index: Int) {
        val newState = TreeMutator.switchActiveTab(_state.value, index)
        _state.value = newState
        updateDerivedState(newState)
    }

    /**
     * Get the current active tab index.
     *
     * @return The active tab index, or null if no TabNode exists
     */
    override val activeTabIndex: Int?
        get() = _state.value.findFirst<TabNode>()?.activeStackIndex

    // =========================================================================
    // TREE STATE OPERATIONS
    // =========================================================================

    /**
     * Update the navigation state directly.
     *
     * This is the low-level API for state mutations. Most consumers should
     * use the higher-level navigation methods instead.
     *
     * @param newState The new navigation state
     * @param transition Optional transition for animation
     */
    override fun updateState(newState: NavNode, transition: NavigationTransition?) {
        updateStateWithTransition(newState, transition)
    }

    /**
     * Update transition progress during animations.
     *
     * Called by the renderer to update animation progress.
     *
     * @param progress Animation progress from 0.0 to 1.0
     */
    override fun updateTransitionProgress(progress: Float) {
        val current = _transitionState.value
        _transitionState.value = when (current) {
            is LegacyTransitionState.Idle -> current
            is LegacyTransitionState.InProgress -> current.copy(progress = progress)
            is LegacyTransitionState.PredictiveBack -> current.copy(progress = progress)
            is LegacyTransitionState.Seeking -> current.copy(progress = progress)
        }
    }

    /**
     * Start a predictive back gesture.
     *
     * Called when the user initiates a back gesture.
     */
    override fun startPredictiveBack() {
        val currentKey = _state.value.activeLeaf()?.key
        val activeStack = _state.value.activeStack()
        val previousKey = if (activeStack != null && activeStack.children.size >= 2) {
            activeStack.children[activeStack.children.size - 2].activeLeaf()?.key
        } else {
            null
        }

        _transitionState.value = LegacyTransitionState.PredictiveBack(
            progress = 0f,
            currentKey = currentKey,
            previousKey = previousKey
        )
    }

    /**
     * Update predictive back gesture progress.
     *
     * @param progress Gesture progress from 0.0 to 1.0
     * @param touchX Normalized x-coordinate of touch (0-1)
     * @param touchY Normalized y-coordinate of touch (0-1)
     */
    override fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float) {
        val current = _transitionState.value
        if (current is LegacyTransitionState.PredictiveBack) {
            _transitionState.value = current.copy(
                progress = progress.coerceIn(0f, 1f),
                touchX = touchX.coerceIn(0f, 1f),
                touchY = touchY.coerceIn(0f, 1f)
            )
        }
    }

    /**
     * Cancel the predictive back gesture.
     *
     * Called when the user releases the gesture without completing it.
     */
    override fun cancelPredictiveBack() {
        _transitionState.value = LegacyTransitionState.Idle
    }

    /**
     * Commit the predictive back gesture.
     *
     * Called when the user completes the back gesture.
     */
    override fun commitPredictiveBack() {
        val current = _transitionState.value
        if (current is LegacyTransitionState.PredictiveBack) {
            _transitionState.value = current.copy(isCommitted = true)
            handleBackInternal()
            _transitionState.value = LegacyTransitionState.Idle
        }
    }

    /**
     * Complete the current transition animation.
     *
     * Called when the animation finishes.
     */
    override fun completeTransition() {
        _transitionState.value = LegacyTransitionState.Idle
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private fun updateStateWithTransition(newState: NavNode, transition: NavigationTransition?) {
        val oldState = _state.value
        val fromKey = oldState.activeLeaf()?.key
        _state.value = newState
        updateDerivedState(newState)
        val toKey = newState.activeLeaf()?.key

        if (transition != null) {
            _transitionState.value = LegacyTransitionState.InProgress(
                transition = transition,
                progress = 0f,
                fromKey = fromKey,
                toKey = toKey
            )
        } else {
            _transitionState.value = LegacyTransitionState.Idle
        }
    }

    private fun computePreviousDestination(state: NavNode): Destination? {
        val stack = state.activeStack() ?: return null
        if (stack.children.size >= 2) {
            val previousNode = stack.children[stack.children.size - 2]
            return previousNode.activeLeaf()?.destination
        }
        return null
    }

    /**
     * Creates the root navigation state.
     *
     * If the initial state is already a StackNode with no parent (a valid root),
     * it's used directly. Otherwise, the initial state is wrapped in a new root StackNode
     * and the child's parentKey is updated to reference the new root.
     * This ensures the navigation tree always has a StackNode as root for proper
     * back handling behavior.
     */
    private fun createRootStack(initialState: NavNode?): NavNode {
        // If initialState is already a root StackNode, use it directly
        if (initialState is StackNode && initialState.parentKey == null) {
            return initialState
        }
        // Otherwise, wrap it in a new root StackNode
        val rootKey = generateKey()
        val childWithUpdatedParent = initialState?.let { state ->
            when (state) {
                is ScreenNode -> state.copy(parentKey = rootKey)
                is TabNode -> state.copy(parentKey = rootKey)
                is PaneNode -> state.copy(parentKey = rootKey)
                is StackNode -> state.copy(parentKey = rootKey)
            }
        }
        return StackNode(
            key = rootKey,
            parentKey = null,
            children = listOfNotNull(childWithUpdatedParent)
        )
    }

    private fun generateKey(): String = Uuid.random().toString().take(8)
}

// =========================================================================
// NAVNODE EXTENSION FOR FINDING FIRST NODE OF TYPE
// =========================================================================

/**
 * Finds the first node of the specified type in the tree.
 *
 * @return The first matching node, or null if not found
 */
inline fun <reified T : NavNode> NavNode.findFirst(): T? {
    return findFirstOfType(T::class)
}

/**
 * Non-inline helper for recursive tree search.
 */
@Suppress("UNCHECKED_CAST")
fun <T : NavNode> NavNode.findFirstOfType(clazz: kotlin.reflect.KClass<T>): T? {
    if (clazz.isInstance(this)) return this as T

    return when (this) {
        is ScreenNode -> null
        is StackNode -> children.firstNotNullOfOrNull { it.findFirstOfType(clazz) }
        is TabNode -> stacks.firstNotNullOfOrNull { it.findFirstOfType(clazz) }
        is PaneNode -> paneConfigurations.values.firstNotNullOfOrNull {
            it.content.findFirstOfType(
                clazz
            )
        }
    }
}
