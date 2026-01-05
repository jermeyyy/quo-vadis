package com.jermey.quo.vadis.core.navigation.internal.tree

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.compose.util.WindowSizeClass
import com.jermey.quo.vadis.core.navigation.destination.DeepLink
import com.jermey.quo.vadis.core.navigation.navigator.LifecycleAwareNode
import com.jermey.quo.vadis.core.navigation.transition.NavigationTransition
import com.jermey.quo.vadis.core.navigation.transition.TransitionState
import com.jermey.quo.vadis.core.navigation.config.NavigationConfig
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.internal.NavigationResultManager
import com.jermey.quo.vadis.core.navigation.internal.ResultCapable
import com.jermey.quo.vadis.core.navigation.internal.TransitionController
import com.jermey.quo.vadis.core.navigation.internal.tree.result.BackResult
import com.jermey.quo.vadis.core.navigation.internal.tree.result.PopResult
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.core.navigation.navigator.PaneNavigator
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.node.PaneNode
import com.jermey.quo.vadis.core.navigation.node.ScreenNode
import com.jermey.quo.vadis.core.navigation.node.StackNode
import com.jermey.quo.vadis.core.navigation.node.TabNode
import com.jermey.quo.vadis.core.navigation.node.activeLeaf
import com.jermey.quo.vadis.core.navigation.node.activeStack
import com.jermey.quo.vadis.core.navigation.pane.PaneConfiguration
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import com.jermey.quo.vadis.core.registry.BackHandlerRegistry
import com.jermey.quo.vadis.core.registry.ContainerInfo
import com.jermey.quo.vadis.core.registry.ContainerRegistry
import com.jermey.quo.vadis.core.registry.DeepLinkRegistry
import com.jermey.quo.vadis.core.registry.PaneRoleRegistry
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.registry.internal.CompositeDeepLinkRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
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
 * @param config Navigation configuration providing all registries. The navigator derives
 *   [scopeRegistry], [containerRegistry], and [deepLinkRegistry] from this config.
 *   Defaults to [NavigationConfig.Empty].
 * @param coroutineScope Scope for derived state computations
 * @param initialState Optional initial navigation state (defaults to empty stack)
 */
@OptIn(ExperimentalUuidApi::class, InternalQuoVadisApi::class)
@Stable
class TreeNavigator(
    override val config: NavigationConfig = NavigationConfig.Empty,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    initialState: NavNode? = null
) : PaneNavigator, TransitionController, ResultCapable {

    // Registries derived from config for internal use
    private val scopeRegistry: ScopeRegistry get() = config.scopeRegistry
    private val containerRegistry: ContainerRegistry get() = config.containerRegistry
    private val paneRoleRegistry: PaneRoleRegistry get() = config.paneRoleRegistry

    // Deep link registry combining generated and runtime handlers
    private val deepLinkRegistry: CompositeDeepLinkRegistry = CompositeDeepLinkRegistry(
        generated = config.deepLinkRegistry
    )

    // TREE-BASED STATE
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

    private val _currentDestination = MutableStateFlow(
        _state.value.activeLeaf()?.destination
    )

    /**
     * The currently active destination, derived from the active leaf node.
     */
    override val currentDestination: StateFlow<NavDestination?> = _currentDestination.asStateFlow()

    private val _previousDestination = MutableStateFlow(
        computePreviousDestination(_state.value)
    )

    /**
     * The previous destination in the active stack.
     *
     * This is the destination that would be shown after a back navigation,
     * or null if there is no previous destination.
     */
    override val previousDestination: StateFlow<NavDestination?> =
        _previousDestination.asStateFlow()

    /**
     * Extension function to get the active child from a NavNode.
     */
    private val NavNode.activeChild: NavNode?
        get() = when (this) {
            is StackNode -> children.lastOrNull()
            is TabNode -> stacks.getOrNull(activeStackIndex)
            is PaneNode -> activePaneContent
            is ScreenNode -> null
        }

    private val _canNavigateBack = MutableStateFlow(
        TreeMutator.canHandleBackNavigation(_state.value)
    )

    /**
     * Flow indicating whether back navigation is possible.
     */
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    /**
     * Manager for navigation result passing between screens.
     */
    override val resultManager: NavigationResultManager = NavigationResultManager()

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

    /**
     * Optional back handler registry for user-defined back handlers.
     * Set by the navigation host during composition.
     *
     * When set, [onBack] will first consult registered handlers
     * before falling back to tree-based back navigation.
     */
    var backHandlerRegistry: BackHandlerRegistry? = null

    /**
     * Current window size class for adaptive pane behavior.
     * Updated by the navigation host based on the current window size.
     *
     * When set, pane back behavior adapts:
     * - In compact mode (single pane visible), back behaves like a simple stack
     * - In expanded mode (multiple panes visible), the configured [com.jermey.quo.vadis.core.navigation.pane.PaneBackBehavior] applies
     *
     * When null, defaults to compact behavior for safety.
     */
    var windowSizeClass: WindowSizeClass? = null

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
        } catch (_: IllegalStateException) {
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
            val newState = TreeMutator.push(
                oldState,
                destination,
                scopeRegistry,
                paneRoleRegistry
            ) { generateKey() }
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
                when (val activeChild = root.activeChild) {
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
    override fun onBack(): Boolean {
        // 1. Check user-defined handlers first
        if (backHandlerRegistry?.handleBack() == true) {
            return true
        }

        val currentState = _state.value

        // Determine compact mode for pane handling
        val isCompact = windowSizeClass?.isCompactWidth ?: true

        // 2. Use tree-aware back handling with window size awareness
        return when (val result = TreeMutator.popWithTabBehavior(currentState, isCompact)) {
            is BackResult.Handled -> {
                updateStateWithTransition(result.newState, null)
                true
            }

            is BackResult.DelegateToSystem -> false
            is BackResult.CannotHandle -> {
                // Fallback to pane-specific behavior with window size awareness
                // In compact mode, treat as simple stack; in expanded mode, use configured behavior
                when (val popResult = TreeMutator.popPaneAdaptive(currentState, isCompact)) {
                    is PopResult.Popped -> {
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
    override fun navigateAndReplace(
        destination: NavDestination,
        transition: NavigationTransition?
    ) {
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
     * Handle deep link navigation from URI string.
     *
     * @param uri The deep link URI to handle
     * @return true if navigation occurred, false otherwise
     */
    override fun handleDeepLink(uri: String): Boolean {
        return deepLinkRegistry.handle(uri, this)
    }

    /**
     * Handle deep link navigation.
     *
     * @param deepLink The deep link to handle
     */
    override fun handleDeepLink(deepLink: DeepLink) {
        deepLinkRegistry.handle(deepLink.uri, this)
    }

    /**
     * Get the deep link registry for pattern registration and resolution.
     *
     * @return The configured DeepLinkRegistry
     */
    override fun getDeepLinkRegistry(): DeepLinkRegistry = deepLinkRegistry

    // =========================================================================
    // PANE-SPECIFIC OPERATIONS
    // =========================================================================

    /**
     * Check if a pane role is available in the current state.
     *
     * @param role Pane role to check
     * @return true if the role is configured in the current PaneNode
     *
     * Example usage:
     * ```kotlin
     * if (navigator.isPaneAvailable(PaneRole.Extra)) {
     *     navigator.navigate(SettingsDestination)
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
     * navigator.navigate(newDetailDestination)
     * ```
     */
    fun clearPane(role: PaneRole) {
        val currentState = _state.value
        val paneNode = currentState.findFirst<PaneNode>()
            ?: throw IllegalStateException("No PaneNode found in current navigation state")

        val paneConfig = paneNode.paneConfigurations[role]
            ?: throw IllegalArgumentException("Pane role $role not configured")

        val targetStack = when (val paneContent = paneConfig.content) {
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

    /**
     * Navigate to a destination in a specific pane, replacing the pane's content.
     *
     * This method clears the target pane's stack and pushes the new destination,
     * effectively replacing the pane's content. It also switches the active pane
     * to the target role.
     *
     * Use this instead of `navigate()` when you want to replace pane content
     * rather than push onto it. This is ideal for list-detail patterns where
     * selecting a new item should replace (not push) the detail view.
     *
     * @param destination The destination to navigate to
     * @param role The pane role to navigate in (default: Supporting)
     *
     * Example usage:
     * ```kotlin
     * // Replace detail pane content when selecting new list item
     * navigator.navigateToPane(ItemDetail(itemId), PaneRole.Supporting)
     * ```
     */
    override fun navigateToPane(destination: NavDestination, role: PaneRole) {
        val currentState = _state.value
        val paneNode = currentState.findFirst<PaneNode>()
            ?: throw IllegalStateException("No PaneNode found in current navigation state")

        val paneConfig = paneNode.paneConfigurations[role]
        val targetStack = when (val paneContent = paneConfig?.content) {
            is StackNode -> paneContent
            else -> paneContent?.activeStack()
        }

        // Create new screen
        val newScreen = ScreenNode(
            key = generateKey(),
            parentKey = targetStack?.key ?: paneNode.key,
            destination = destination
        )

        val newState = if (targetStack != null) {
            // Replace stack content with single new screen
            val newStack = targetStack.copy(children = listOf(newScreen))
            val updated = TreeMutator.replaceNode(currentState, targetStack.key, newStack)
            // Switch to this pane
            TreeMutator.switchActivePane(updated, paneNode.key, role)
        } else {
            // No stack exists for this role - create configuration with new stack
            val newStack = StackNode(
                key = generateKey(),
                parentKey = paneNode.key,
                children = listOf(newScreen)
            )
            val newConfig = PaneConfiguration(content = newStack)
            val updated = TreeMutator.setPaneConfiguration(currentState, paneNode.key, role, newConfig)
            TreeMutator.switchActivePane(updated, paneNode.key, role)
        }

        updateStateWithTransition(newState, null)
    }

    // =========================================================================
    // TAB OPERATIONS
    // =========================================================================

    /**
     * Get the current active tab index.
     *
     * @return The active tab index, or null if no TabNode exists
     */
    val activeTabIndex: Int?
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
            onBack()
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

        // Notify lifecycle-aware nodes that were removed from the tree
        notifyRemovedNodesDetached(oldState, newState)

        // Cancel results for destroyed screens
        cancelResultsForDestroyedScreens(oldState, newState)

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
     * Cancel pending results for screens that were removed from the tree.
     */
    private fun cancelResultsForDestroyedScreens(oldState: NavNode, newState: NavNode) {
        val oldScreenKeys = collectScreenKeys(oldState)
        val newScreenKeys = collectScreenKeys(newState)
        val destroyedKeys = oldScreenKeys - newScreenKeys

        if (destroyedKeys.isEmpty()) return

        // Launch in coroutine scope since cancelResult is a suspend function
        try {
            coroutineScope.launch {
                destroyedKeys.forEach { screenKey ->
                    resultManager.cancelResult(screenKey)
                }
            }
        } catch (_: IllegalStateException) {
            // Dispatcher not available (e.g., Main dispatcher in tests)
            // Result cancellation is optional, so we silently ignore this
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

    /**
     * Notify lifecycle-aware nodes that were removed from the navigation tree.
     *
     * This calls [LifecycleAwareNode.detachFromNavigator] on all nodes
     * (ScreenNode, TabNode, PaneNode) that existed in the old state but
     * are not present in the new state.
     */
    private fun notifyRemovedNodesDetached(oldState: NavNode, newState: NavNode) {
        val oldNodes = collectLifecycleAwareNodes(oldState)
        val newNodeKeys = collectLifecycleAwareNodeKeys(newState)

        oldNodes.forEach { node ->
            if (node.key !in newNodeKeys) {
                (node as? LifecycleAwareNode)?.detachFromNavigator()
            }
        }
    }

    /**
     * Collect all lifecycle-aware nodes from a navigation tree.
     */
    private fun collectLifecycleAwareNodes(node: NavNode): List<NavNode> {
        val nodes = mutableListOf<NavNode>()
        collectLifecycleAwareNodesRecursive(node, nodes)
        return nodes
    }

    private fun collectLifecycleAwareNodesRecursive(node: NavNode, nodes: MutableList<NavNode>) {
        when (node) {
            is ScreenNode -> nodes.add(node)
            is StackNode -> node.children.forEach { collectLifecycleAwareNodesRecursive(it, nodes) }
            is TabNode -> {
                nodes.add(node)
                node.stacks.forEach { collectLifecycleAwareNodesRecursive(it, nodes) }
            }

            is PaneNode -> {
                nodes.add(node)
                node.paneConfigurations.values.forEach {
                    collectLifecycleAwareNodesRecursive(it.content, nodes)
                }
            }
        }
    }

    /**
     * Collect all lifecycle-aware node keys from a navigation tree.
     */
    private fun collectLifecycleAwareNodeKeys(node: NavNode): Set<String> {
        val keys = mutableSetOf<String>()
        collectLifecycleAwareNodeKeysRecursive(node, keys)
        return keys
    }

    private fun collectLifecycleAwareNodeKeysRecursive(node: NavNode, keys: MutableSet<String>) {
        when (node) {
            is ScreenNode -> keys.add(node.key)
            is StackNode -> node.children.forEach {
                collectLifecycleAwareNodeKeysRecursive(
                    it,
                    keys
                )
            }

            is TabNode -> {
                keys.add(node.key)
                node.stacks.forEach { collectLifecycleAwareNodeKeysRecursive(it, keys) }
            }

            is PaneNode -> {
                keys.add(node.key)
                node.paneConfigurations.values.forEach {
                    collectLifecycleAwareNodeKeysRecursive(it.content, keys)
                }
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
fun <T : NavNode> NavNode.findFirstOfType(clazz: KClass<T>): T? {
    if (clazz.isInstance(this)) return this as T

    return when (this) {
        is ScreenNode -> null
        is StackNode -> children.firstNotNullOfOrNull { it.findFirstOfType(clazz) }
        is TabNode -> stacks.firstNotNullOfOrNull { it.findFirstOfType(clazz) }
        is PaneNode -> paneConfigurations.values.firstNotNullOfOrNull {
            it.content.findFirstOfType(clazz)
        }
    }
}
