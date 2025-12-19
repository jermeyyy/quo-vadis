package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.compose.registry.BackHandlerRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerInfo
import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.wrapper.WindowSizeClass
import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
 *   the parent stack instead of the deepest active stack. Defaults to [com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry.Empty]
 *   which allows all destinations in all scopes (backward compatible behavior).
 * @property containerRegistry Registry for container-aware navigation. When navigating
 *   to a destination that belongs to a @Tabs or @Pane container, this registry provides
 *   the builder function to create the appropriate container node. Defaults to
 *   [ContainerRegistry.Empty] which never creates containers (backward compatible behavior).
 */
@OptIn(ExperimentalUuidApi::class)
@Stable
class TreeNavigator(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler(),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    initialState: NavNode? = null,
    private val scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    private val containerRegistry: ContainerRegistry = ContainerRegistry.Empty
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

    private val _transitionState: MutableStateFlow<TransitionState> =
        MutableStateFlow(TransitionState.Idle)

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
    override val transitionState: StateFlow<TransitionState> = _transitionState.asStateFlow()

    // =========================================================================
    // DERIVED CONVENIENCE PROPERTIES
    // Backed by MutableStateFlow for synchronous updates in tests
    // =========================================================================

    private val _currentDestination = MutableStateFlow<NavDestination?>(
        _state.value.activeLeaf()?.destination
    )

    /**
     * The currently active destination, derived from the active leaf node.
     */
    override val currentDestination: StateFlow<NavDestination?> = _currentDestination.asStateFlow()

    private val _previousDestination = MutableStateFlow<NavDestination?>(
        computePreviousDestination(_state.value)
    )

    /**
     * The previous destination in the active stack.
     *
     * This is the destination that would be shown after a back navigation,
     * or null if there is no previous destination.
     */
    override val previousDestination: StateFlow<NavDestination?> = _previousDestination.asStateFlow()

    private val _canNavigateBack = MutableStateFlow(
        TreeMutator.canHandleBackNavigation(_state.value)
    )

    /**
     * Flow indicating whether back navigation is possible.
     */
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    // =========================================================================
    // RESULT AND LIFECYCLE MANAGERS
    // =========================================================================

    /**
     * Manager for navigation result passing between screens.
     */
    override val resultManager: NavigationResultManager = NavigationResultManager()

    /**
     * Manager for navigation lifecycle callbacks.
     */
    override val lifecycleManager: NavigationLifecycleManager = NavigationLifecycleManager()

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
                is TransitionState.Idle -> null
                is TransitionState.InProgress -> transitionState.transition
                is TransitionState.PredictiveBack -> null
                is TransitionState.Seeking -> transitionState.transition
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

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
     * Uses container-aware navigation logic:
     * 1. Check if destination needs a container AND we're not already inside one
     * 2. Default - push as ScreenNode (TreeMutator handles scope-aware navigation)
     *
     * @param destination The destination to navigate to
     * @param transition Optional transition animation
     */
    override fun navigate(destination: NavDestination, transition: NavigationTransition?) {
        val effectiveTransition = transition ?: destination.transition
        val oldState = _state.value
        val fromKey = oldState.activeLeaf()?.key

        // Step 1: Check if destination needs a container
        val containerInfo = containerRegistry.getContainerInfo(destination)
        if (containerInfo != null) {
            // Check if we're already inside a container with the same scope
            val currentScopeKey = getCurrentScopeKey(oldState)
            if (currentScopeKey != containerInfo.scopeKey) {
                // Not inside the required container - create it
                navigateWithContainer(oldState, containerInfo, effectiveTransition, fromKey)
                return
            }
            // Already inside the container - fall through to normal navigation
        }

        // Step 2: Default - push as ScreenNode (TreeMutator handles scope-aware navigation)
        navigateDefault(oldState, destination, effectiveTransition, fromKey)
    }

    /**
     * Get the current scope key from the navigation state.
     *
     * Traverses to the active container (TabNode or PaneNode) and returns its scopeKey.
     */
    private fun getCurrentScopeKey(state: NavNode): String? {
        return when (state) {
            is TabNode -> state.scopeKey
            is PaneNode -> state.scopeKey
            is StackNode -> {
                // Check if the active child is a container
                state.children.lastOrNull()?.let { activeChild ->
                    getCurrentScopeKey(activeChild)
                }
            }
            is ScreenNode -> null
        }
    }

    /**
     * Navigate by creating a container structure.
     */
    private fun navigateWithContainer(
        oldState: NavNode,
        containerInfo: ContainerInfo,
        effectiveTransition: NavigationTransition?,
        fromKey: String?
    ) {
        try {
            val newState = pushContainer(oldState, containerInfo)
            val toKey = newState.activeLeaf()?.key

            _state.value = newState
            updateDerivedState(newState)

            if (effectiveTransition != null) {
                _transitionState.value = TransitionState.InProgress(
                    transition = effectiveTransition,
                    progress = 0f,
                    fromKey = fromKey,
                    toKey = toKey
                )
            }
        } catch (e: IllegalStateException) {
            // If no active stack, create root stack with container
            val rootKey = generateKey()
            val containerKey = generateKey()
            
            val containerNode = when (containerInfo) {
                is ContainerInfo.TabContainer -> containerInfo.builder(
                    containerKey,
                    rootKey,
                    containerInfo.initialTabIndex
                )
                is ContainerInfo.PaneContainer -> containerInfo.builder(
                    containerKey,
                    rootKey
                )
            }
            
            val newState = StackNode(
                key = rootKey,
                parentKey = null,
                children = listOf(containerNode)
            )
            _state.value = newState
            updateDerivedState(newState)

            if (effectiveTransition != null) {
                _transitionState.value = TransitionState.InProgress(
                    transition = effectiveTransition,
                    progress = 0f,
                    fromKey = fromKey,
                    toKey = newState.activeLeaf()?.key
                )
            }
        }
    }

    /**
     * Default navigation - push destination as ScreenNode.
     */
    private fun navigateDefault(
        oldState: NavNode,
        destination: NavDestination,
        effectiveTransition: NavigationTransition?,
        fromKey: String?
    ) {
        try {
            val newState = TreeMutator.push(oldState, destination, scopeRegistry) { generateKey() }
            val toKey = newState.activeLeaf()?.key

            _state.value = newState
            updateDerivedState(newState)

            // Update transition state
            if (effectiveTransition != null) {
                _transitionState.value = TransitionState.InProgress(
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
                _transitionState.value = TransitionState.InProgress(
                    transition = effectiveTransition,
                    progress = 0f,
                    fromKey = fromKey,
                    toKey = screenKey
                )
            }
        }
    }

    /**
     * Push a container onto the appropriate stack.
     *
     * When navigating to a new container (like DemoTabs from MainTabs), we need to
     * push onto the stack that CONTAINS the current container, not the stack INSIDE it.
     *
     * For example, if tree is:
     * ```
     * RootStack
     *   └── TabNode (MainTabs)
     *         └── StackNode (HomeTab) <- activeStack()
     * ```
     *
     * We should push DemoTabs onto RootStack, not HomeTab's stack.
     *
     * @param root The current navigation state
     * @param containerInfo Information about the container to create
     * @return New navigation state with container pushed onto appropriate stack
     * @throws IllegalStateException if no appropriate stack is found
     */
    private fun pushContainer(
        root: NavNode,
        containerInfo: ContainerInfo
    ): NavNode {
        // Find the stack that should receive the new container
        // This is the stack containing the current container, or root if no container
        val targetStack = findContainerParentStack(root)
            ?: throw IllegalStateException("No appropriate stack found for container navigation")

        return when (containerInfo) {
            is ContainerInfo.TabContainer -> {
                val containerKey = generateKey()
                val tabNode = containerInfo.builder(
                    containerKey,
                    targetStack.key,
                    containerInfo.initialTabIndex
                )
                val newStack = targetStack.copy(
                    children = targetStack.children + tabNode
                )
                TreeMutator.replaceNode(root, targetStack.key, newStack)
            }
            is ContainerInfo.PaneContainer -> {
                val containerKey = generateKey()
                val paneNode = containerInfo.builder(containerKey, targetStack.key)
                val newStack = targetStack.copy(
                    children = targetStack.children + paneNode
                )
                TreeMutator.replaceNode(root, targetStack.key, newStack)
            }
        }
    }

    /**
     * Find the stack that should receive a new container.
     *
     * This finds the stack that contains a TabNode or PaneNode in the active path,
     * or returns the root stack if no container exists.
     *
     * @param root The current navigation state
     * @return The stack that should receive new containers
     */
    private fun findContainerParentStack(root: NavNode): StackNode? {
        return when (root) {
            is StackNode -> {
                // Check if any child in the active path is a container
                val activeChild = root.activeChild
                when (activeChild) {
                    is TabNode, is PaneNode -> {
                        // This stack contains a container - return it
                        root
                    }
                    is StackNode -> {
                        // Recurse into nested stack
                        findContainerParentStack(activeChild) ?: root
                    }
                    else -> {
                        // No container found, return this stack
                        root
                    }
                }
            }
            is TabNode -> {
                // Inside a tab - return null to indicate we need to go up
                // The parent call will return the containing stack
                null
            }
            is PaneNode -> {
                // Inside a pane - return null to indicate we need to go up
                null
            }
            is ScreenNode -> null
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
        destination: NavDestination,
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
    override fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition?) {
        val newState = TreeMutator.replaceCurrent(_state.value, destination) { generateKey() }
        updateStateWithTransition(newState, transition)
    }

    /**
     * Navigate to a destination and clear the entire backstack.
     *
     * @param destination The destination to set as the new root
     */
    override fun navigateAndClearAll(destination: NavDestination) {
        val newState = TreeMutator.clearAndPush(_state.value, destination) { generateKey() }
        updateStateWithTransition(newState, null)
    }

    /**
     * Handle deep link navigation.
     *
     * @param deepLink The deep link to handle
     */
    override fun handleDeepLink(deepLink: DeepLink) {
        deepLinkHandler.handle(deepLink, this)
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
        destination: NavDestination,
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
            is TransitionState.Idle -> current
            is TransitionState.InProgress -> current.copy(progress = progress)
            is TransitionState.PredictiveBack -> current.copy(progress = progress)
            is TransitionState.Seeking -> current.copy(progress = progress)
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

        _transitionState.value = TransitionState.PredictiveBack(
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
        if (current is TransitionState.PredictiveBack) {
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
        _transitionState.value = TransitionState.Idle
    }

    /**
     * Commit the predictive back gesture.
     *
     * Called when the user completes the back gesture.
     */
    override fun commitPredictiveBack() {
        val current = _transitionState.value
        if (current is TransitionState.PredictiveBack) {
            _transitionState.value = current.copy(isCommitted = true)
            handleBackInternal()
            _transitionState.value = TransitionState.Idle
        }
    }

    /**
     * Complete the current transition animation.
     *
     * Called when the animation finishes.
     */
    override fun completeTransition() {
        _transitionState.value = TransitionState.Idle
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

        // Dispatch lifecycle events for screen changes
        dispatchLifecycleEvents(oldState, newState, fromKey, toKey)

        if (transition != null) {
            _transitionState.value = TransitionState.InProgress(
                transition = transition,
                progress = 0f,
                fromKey = fromKey,
                toKey = toKey
            )
        } else {
            _transitionState.value = TransitionState.Idle
        }
    }

    /**
     * Dispatch lifecycle events based on state changes.
     *
     * Compares old and new state to determine:
     * - Which screens were destroyed (removed from tree)
     * - Which screen became inactive (fromKey)
     * - Which screen became active (toKey)
     *
     * Events are dispatched asynchronously to avoid blocking navigation.
     */
    private fun dispatchLifecycleEvents(
        oldState: NavNode,
        newState: NavNode,
        fromKey: String?,
        toKey: String?
    ) {
        // Find all screen keys in old and new state
        val oldScreenKeys = collectScreenKeys(oldState)
        val newScreenKeys = collectScreenKeys(newState)

        // Destroyed screens = in old state but not in new state
        val destroyedKeys = oldScreenKeys - newScreenKeys

        // Only dispatch if there are changes to handle
        if (destroyedKeys.isEmpty() && (fromKey == null || fromKey == toKey)) {
            return
        }

        // Dispatch events using coroutineScope
        // Use try-catch to handle cases where dispatcher is not available (tests)
        try {
            coroutineScope.launch {
                // Dispatch onExit for the previously active screen if it changed
                if (fromKey != null && fromKey != toKey && fromKey !in destroyedKeys) {
                    lifecycleManager.onScreenExited(fromKey)
                }

                // Dispatch onDestroy and cancel results for destroyed screens
                destroyedKeys.forEach { screenKey ->
                    lifecycleManager.onScreenDestroyed(screenKey)
                    resultManager.cancelResult(screenKey)
                }
            }
        } catch (_: IllegalStateException) {
            // Dispatcher not available (e.g., Main dispatcher in tests)
            // Lifecycle events are optional, so we silently ignore this
        }
    }

    /**
     * Collect all screen keys from a navigation tree.
     */
    private fun collectScreenKeys(node: NavNode): Set<String> {
        val keys = mutableSetOf<String>()
        collectScreenKeysRecursive(node, keys)
        return keys
    }

    private fun collectScreenKeysRecursive(node: NavNode, keys: MutableSet<String>) {
        when (node) {
            is ScreenNode -> keys.add(node.key)
            is StackNode -> node.children.forEach { collectScreenKeysRecursive(it, keys) }
            is TabNode -> node.stacks.forEach { collectScreenKeysRecursive(it, keys) }
            is PaneNode -> node.paneConfigurations.values.forEach {
                collectScreenKeysRecursive(it.content, keys)
            }
        }
    }

    private fun computePreviousDestination(state: NavNode): NavDestination? {
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

    companion object
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
