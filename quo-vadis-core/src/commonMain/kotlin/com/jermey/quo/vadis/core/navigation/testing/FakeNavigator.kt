package com.jermey.quo.vadis.core.navigation.testing

import com.jermey.quo.vadis.core.navigation.core.BackPressHandler
import com.jermey.quo.vadis.core.navigation.core.DeepLink
import com.jermey.quo.vadis.core.navigation.core.route
import com.jermey.quo.vadis.core.navigation.core.DeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.DefaultDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.NavKeyGenerator
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.NavigationLifecycleManager
import com.jermey.quo.vadis.core.navigation.core.NavigationResultManager
import com.jermey.quo.vadis.core.navigation.core.NavigationTransition
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TransitionState
import com.jermey.quo.vadis.core.navigation.core.activeLeaf
import com.jermey.quo.vadis.core.navigation.core.activeStack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fake Navigator implementation for testing purposes.
 * Allows you to verify navigation calls without actual UI rendering.
 *
 * Uses the new tree-based navigation state model.
 */
@Suppress("TooManyFunctions")
class FakeNavigator : Navigator {

    // =========================================================================
    // TREE-BASED STATE
    // =========================================================================

    private val _state = MutableStateFlow<NavNode>(
        StackNode(
            key = NavKeyGenerator.generate(),
            parentKey = null,
            children = emptyList()
        )
    )
    override val state: StateFlow<NavNode> = _state.asStateFlow()

    private val _transitionState = MutableStateFlow<TransitionState>(TransitionState.Idle)
    override val transitionState: StateFlow<TransitionState> = _transitionState.asStateFlow()

    private val _canNavigateBack = MutableStateFlow(false)
    override val canNavigateBack: StateFlow<Boolean> = _canNavigateBack.asStateFlow()

    private val _currentDestination = MutableStateFlow<NavDestination?>(null)
    override val currentDestination: StateFlow<NavDestination?> = _currentDestination.asStateFlow()

    private val _previousDestination = MutableStateFlow<NavDestination?>(null)
    override val previousDestination: StateFlow<NavDestination?> = _previousDestination.asStateFlow()

    private val _currentTransition = MutableStateFlow<NavigationTransition?>(null)
    override val currentTransition: StateFlow<NavigationTransition?> = _currentTransition.asStateFlow()

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

    // Track navigation calls for verification
    val navigationCalls = mutableListOf<NavigationCall>()

    private fun updateDerivedState() {
        val currentState = _state.value
        val activeLeaf = currentState.activeLeaf()
        _currentDestination.value = (activeLeaf as? ScreenNode)?.destination

        // Update canNavigateBack based on stack depth
        val activeStack = currentState.activeStack()
        _canNavigateBack.value = activeStack != null && activeStack.children.size > 1

        // Update previous destination
        if (activeStack != null && activeStack.children.size > 1) {
            val previousNode = activeStack.children.getOrNull(activeStack.children.size - 2)
            _previousDestination.value = (previousNode as? ScreenNode)?.destination
        } else {
            _previousDestination.value = null
        }
    }

    // =========================================================================
    // NAVIGATION OPERATIONS
    // =========================================================================

    override fun navigate(destination: NavDestination, transition: NavigationTransition?) {
        navigationCalls.add(NavigationCall.Navigate(destination, transition))
        _currentTransition.value = transition

        val currentState = _state.value
        val activeStack = currentState.activeStack()
        if (activeStack != null) {
            val newScreen = ScreenNode(
                key = NavKeyGenerator.generate(),
                parentKey = activeStack.key,
                destination = destination
            )
            val newStack = activeStack.copy(
                children = activeStack.children + newScreen
            )
            _state.value = replaceStackInState(currentState, activeStack.key, newStack)
        } else {
            // Create initial stack
            val stackKey = NavKeyGenerator.generate()
            val newScreen = ScreenNode(
                key = NavKeyGenerator.generate(),
                parentKey = stackKey,
                destination = destination
            )
            _state.value = StackNode(
                key = stackKey,
                parentKey = null,
                children = listOf(newScreen)
            )
        }
        updateDerivedState()
    }

    private fun replaceStackInState(root: NavNode, targetKey: String, newStack: StackNode): NavNode {
        if (root.key == targetKey) return newStack
        return when (root) {
            is ScreenNode -> root
            is StackNode -> if (root.key == targetKey) newStack else root.copy(
                children = root.children.map { replaceStackInState(it, targetKey, newStack) }
            )
            else -> root
        }
    }

    override fun navigateBack(): Boolean {
        val result = onBack()
        navigationCalls.add(NavigationCall.NavigateBack(result))
        return result
    }

    override fun navigateAndClearTo(
        destination: NavDestination,
        clearRoute: String?,
        inclusive: Boolean
    ) {
        navigationCalls.add(NavigationCall.NavigateAndClearTo(destination, clearRoute, inclusive))
        // Simplified: just set the destination as the only item
        initializeWithDestination(destination)
    }

    override fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition?) {
        navigationCalls.add(NavigationCall.NavigateAndReplace(destination, transition))
        val currentState = _state.value
        val activeStack = currentState.activeStack()
        if (activeStack != null && activeStack.children.isNotEmpty()) {
            val newScreen = ScreenNode(
                key = NavKeyGenerator.generate(),
                parentKey = activeStack.key,
                destination = destination
            )
            val newStack = activeStack.copy(
                children = activeStack.children.dropLast(1) + newScreen
            )
            _state.value = replaceStackInState(currentState, activeStack.key, newStack)
        }
        updateDerivedState()
    }

    override fun navigateAndClearAll(destination: NavDestination) {
        navigationCalls.add(NavigationCall.NavigateAndClearAll(destination))
        initializeWithDestination(destination)
    }

    override fun handleDeepLink(deepLink: DeepLink) {
        navigationCalls.add(NavigationCall.HandleDeepLink(deepLink))
        // Default implementation does nothing in tests
    }

    /**
     * Internal method for test setup. Sets the start destination.
     * Creates a fresh stack with the given destination as root.
     *
     * @param destination The starting destination
     */
    internal fun initializeWithDestination(destination: NavDestination) {
        navigationCalls.add(NavigationCall.SetStartDestination(destination))
        val stackKey = NavKeyGenerator.generate()
        val screenKey = NavKeyGenerator.generate()
        _state.value = StackNode(
            key = stackKey,
            parentKey = null,
            children = listOf(
                ScreenNode(
                    key = screenKey,
                    parentKey = stackKey,
                    destination = destination
                )
            )
        )
        updateDerivedState()
    }

    // =========================================================================
    // TAB NAVIGATION
    // =========================================================================

    override val activeTabIndex: Int?
        get() = null

    // =========================================================================
    // PANE NAVIGATION (Stubbed for testing)
    // =========================================================================

    override fun navigateToPane(
        role: PaneRole,
        destination: NavDestination,
        switchFocus: Boolean,
        transition: NavigationTransition?
    ) {
        // Simplified: treat as regular navigate
        navigate(destination, transition)
    }

    override fun switchPane(role: PaneRole) {
        // No-op for fake navigator
    }

    override fun isPaneAvailable(role: PaneRole): Boolean = false

    override fun paneContent(role: PaneRole): NavNode? = null

    override fun navigateBackInPane(role: PaneRole): Boolean = navigateBack()

    override fun clearPane(role: PaneRole) {
        // No-op for fake navigator
    }

    // =========================================================================
    // STATE MANIPULATION
    // =========================================================================

    override fun updateState(newState: NavNode, transition: NavigationTransition?) {
        _state.value = newState
        _currentTransition.value = transition
        updateDerivedState()
    }

    // =========================================================================
    // TRANSITION CONTROL
    // =========================================================================

    override fun updateTransitionProgress(progress: Float) {
        val current = _transitionState.value
        when (current) {
            is TransitionState.InProgress -> {
                _transitionState.value = current.copy(progress = progress)
            }
            is TransitionState.PredictiveBack -> {
                _transitionState.value = current.copy(progress = progress)
            }
            else -> { /* Ignore if not in transition */ }
        }
    }

    override fun startPredictiveBack() {
        _transitionState.value = TransitionState.PredictiveBack(
            progress = 0f,
            touchX = 0f,
            touchY = 0f
        )
    }

    override fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float) {
        val current = _transitionState.value
        if (current is TransitionState.PredictiveBack) {
            _transitionState.value = current.copy(
                progress = progress,
                touchX = touchX,
                touchY = touchY
            )
        }
    }

    override fun cancelPredictiveBack() {
        _transitionState.value = TransitionState.Idle
    }

    override fun commitPredictiveBack() {
        navigateBack()
        _transitionState.value = TransitionState.Idle
    }

    override fun completeTransition() {
        _transitionState.value = TransitionState.Idle
    }

    // =========================================================================
    // DEEP LINK & CHILD SUPPORT
    // =========================================================================

    private val fakeDeepLinkHandler = DefaultDeepLinkHandler()

    override fun getDeepLinkHandler(): DeepLinkHandler {
        return fakeDeepLinkHandler
    }

    // Child navigator support for hierarchical navigation
    private var _activeChild: BackPressHandler? = null
    override val activeChild: BackPressHandler?
        get() = _activeChild

    override fun setActiveChild(child: BackPressHandler?) {
        _activeChild = child
    }

    override fun handleBackInternal(): Boolean {
        val currentState = _state.value
        val activeStack = currentState.activeStack()
        if (activeStack != null && activeStack.children.size > 1) {
            val newStack = activeStack.copy(
                children = activeStack.children.dropLast(1)
            )
            _state.value = replaceStackInState(currentState, activeStack.key, newStack)
            updateDerivedState()
            return true
        }
        return false
    }

    // =========================================================================
    // TEST UTILITIES
    // =========================================================================

    /**
     * Clear all recorded navigation calls.
     */
    fun clearCalls() {
        navigationCalls.clear()
    }

    /**
     * Verify that a specific navigation call was made.
     */
    fun verifyNavigateTo(route: String): Boolean {
        return navigationCalls.any { call ->
            call is NavigationCall.Navigate && call.destination.route == route
        }
    }

    /**
     * Verify that navigateBack was called.
     */
    fun verifyNavigateBack(): Boolean {
        return navigationCalls.any { it is NavigationCall.NavigateBack }
    }

    /**
     * Get the count of navigate calls.
     */
    fun getNavigateCallCount(route: String): Int {
        return navigationCalls.count { call ->
            call is NavigationCall.Navigate && call.destination.route == route
        }
    }

    /**
     * Get the current stack size for testing.
     */
    fun getStackSize(): Int {
        val activeStack = _state.value.activeStack()
        return activeStack?.children?.size ?: 0
    }

    companion object {
        /**
         * Creates a FakeNavigator initialized with a single destination.
         * Useful for simple test cases that don't need complex navigation state.
         *
         * @param destination The initial destination to navigate to
         * @return A FakeNavigator with the destination as its initial state
         */
        fun withDestination(destination: NavDestination): FakeNavigator {
            return FakeNavigator().apply {
                initializeWithDestination(destination)
            }
        }
    }
}

/**
 * Sealed class representing different navigation calls for testing.
 */
sealed class NavigationCall {
    data class Navigate(
        val destination: NavDestination,
        val transition: NavigationTransition?
    ) : NavigationCall()

    data class NavigateBack(val success: Boolean) : NavigationCall()

    data class NavigateAndClearTo(
        val destination: NavDestination,
        val clearRoute: String?,
        val inclusive: Boolean
    ) : NavigationCall()

    data class NavigateAndReplace(
        val destination: NavDestination,
        val transition: NavigationTransition?
    ) : NavigationCall()

    data class NavigateAndClearAll(val destination: NavDestination) : NavigationCall()

    data class HandleDeepLink(val deepLink: DeepLink) : NavigationCall()

    data class SetStartDestination(val destination: NavDestination) : NavigationCall()
}

/**
 * Test builder for creating test navigation scenarios.
 */
@Suppress("FunctionNaming")
class NavigationTestBuilder {
    private val navigator = FakeNavigator()

    fun given(block: FakeNavigator.() -> Unit): NavigationTestBuilder {
        navigator.block()
        return this
    }

    fun `when`(block: FakeNavigator.() -> Unit): NavigationTestBuilder {
        navigator.block()
        return this
    }

    fun then(block: FakeNavigator.() -> Unit): NavigationTestBuilder {
        navigator.block()
        return this
    }

    fun build() = navigator
}

/**
 * DSL for creating navigation tests.
 */
fun navigationTest(block: NavigationTestBuilder.() -> Unit): FakeNavigator {
    return NavigationTestBuilder().apply(block).build()
}
