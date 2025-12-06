package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Represents the current state of a navigation transition.
 *
 * TransitionState is a sealed class that models the complete lifecycle
 * of navigation transitions, including:
 *
 * - [Idle] - No transition in progress, showing current state
 * - [Proposed] - User is performing a gesture (predictive back)
 * - [Animating] - Transition animation is playing
 *
 * ## State Transitions
 *
 * ```
 * Idle ─────► Animating ─────► Idle
 *   │              ▲
 *   │              │ commit
 *   ▼              │
 * Proposed ────────┘
 *   │
 *   └────► Idle (cancel)
 * ```
 *
 * ## Usage with Navigator
 *
 * The Navigator exposes a `transitionState: StateFlow<TransitionState>` that
 * QuoVadisHost observes to coordinate rendering and animations.
 *
 * ```kotlin
 * navigator.transitionState.collect { state ->
 *     when (state) {
 *         is TransitionState.Idle -> // No animation
 *         is TransitionState.Proposed -> // Gesture in progress
 *         is TransitionState.Animating -> // Animate with state.progress
 *     }
 * }
 * ```
 *
 * @see TreeFlattener
 * @see QuoVadisHost
 */
@Serializable
sealed class TransitionState {

    /**
     * The navigation direction of this transition.
     */
    abstract val direction: TransitionDirection

    /**
     * The current (source) navigation state.
     */
    abstract val current: NavNode

    /**
     * Returns composable references for current and previous screens
     * for animation/shared element purposes.
     *
     * @return Pair of (current NavNode, target/previous NavNode or null if idle)
     */
    abstract fun animationComposablePair(): Pair<NavNode, NavNode?>

    /**
     * Determines whether this transition is an intra-tab navigation.
     *
     * For TabNode transitions, this checks if navigation occurs within
     * the same tab (cache content only) or across tabs (cache whole wrapper).
     *
     * @param tabKey The key of the TabNode to check
     * @return True if navigating within the same tab, false if switching tabs or not a tab transition
     */
    abstract fun isIntraTabNavigation(tabKey: String): Boolean

    /**
     * Determines whether this transition is an intra-pane navigation.
     *
     * For PaneNode transitions, this checks if navigation occurs within
     * the same pane structure (cache content only) or across panes.
     *
     * @param paneKey The key of the PaneNode to check
     * @return True if navigating within the same pane structure, false otherwise
     */
    abstract fun isIntraPaneNavigation(paneKey: String): Boolean

    /**
     * Checks if this transition navigates between different node types.
     *
     * For example, navigating from a StackNode to a TabNode.
     *
     * @return True if current and target are different NavNode types
     */
    abstract fun isCrossNodeTypeNavigation(): Boolean

    /**
     * Navigation is at rest, showing the current state.
     *
     * This is the default state when no transition is in progress.
     * The [current] property holds the active navigation tree.
     *
     * @property current The current navigation tree being displayed
     */
    @Serializable
    data class Idle(
        override val current: NavNode
    ) : TransitionState() {
        override val direction: TransitionDirection = TransitionDirection.NONE

        override fun animationComposablePair(): Pair<NavNode, NavNode?> = current to null

        override fun isIntraTabNavigation(tabKey: String): Boolean = false

        override fun isIntraPaneNavigation(paneKey: String): Boolean = false

        override fun isCrossNodeTypeNavigation(): Boolean = false
    }

    /**
     * A navigation change has been proposed but not committed.
     *
     * This state is used during predictive back gestures where the user
     * is dragging and the UI is showing a preview of the result. The
     * transition can be:
     *
     * - **Committed**: Transition to [Animating] to complete the navigation
     * - **Cancelled**: Return to [Idle] with the original state
     *
     * @property current The current (source) navigation state
     * @property proposed The proposed (target) navigation state if committed
     * @property progress Gesture progress from 0.0 (start) to 1.0 (threshold)
     */
    @Serializable
    data class Proposed(
        override val current: NavNode,
        val proposed: NavNode,
        val progress: Float = 0f
    ) : TransitionState() {
        override val direction: TransitionDirection = TransitionDirection.BACKWARD

        init {
            require(progress in 0f..1f) {
                "Progress must be between 0.0 and 1.0, was: $progress"
            }
        }

        /**
         * Updates the progress value.
         *
         * @param newProgress New progress value (will be clamped to 0.0-1.0)
         * @return A new Proposed state with the updated progress
         */
        fun withProgress(newProgress: Float): Proposed {
            return copy(progress = newProgress.coerceIn(0f, 1f))
        }

        override fun animationComposablePair(): Pair<NavNode, NavNode?> = current to proposed

        override fun isIntraTabNavigation(tabKey: String): Boolean {
            val currentTab = current.findByKey(tabKey) as? TabNode ?: return false
            val proposedTab = proposed.findByKey(tabKey) as? TabNode ?: return false
            // Intra-tab if both are TabNodes with same key and activeStackIndex is the same
            return currentTab.activeStackIndex == proposedTab.activeStackIndex
        }

        override fun isIntraPaneNavigation(paneKey: String): Boolean {
            val currentPane = current.findByKey(paneKey) as? PaneNode ?: return false
            val proposedPane = proposed.findByKey(paneKey) as? PaneNode ?: return false
            // Intra-pane if navigation is within the same pane structure
            return currentPane.key == proposedPane.key
        }

        override fun isCrossNodeTypeNavigation(): Boolean {
            return current::class != proposed::class
        }
    }

    /**
     * A navigation transition animation is in progress.
     *
     * This state is entered when:
     * - A navigation action is triggered (push, pop, switchTab)
     * - A [Proposed] gesture is committed
     *
     * The [progress] value is updated by the animation system and
     * used by the renderer to interpolate visual states.
     *
     * @property current The source (exiting) navigation state
     * @property target The target (entering) navigation state
     * @property progress Animation progress from 0.0 (start) to 1.0 (complete)
     * @property direction Whether navigating forward or backward
     */
    @Serializable
    data class Animating(
        override val current: NavNode,
        val target: NavNode,
        val progress: Float = 0f,
        override val direction: TransitionDirection
    ) : TransitionState() {

        init {
            require(progress in 0f..1f) {
                "Progress must be between 0.0 and 1.0, was: $progress"
            }
        }

        /**
         * Updates the animation progress.
         *
         * @param newProgress New progress value (will be clamped to 0.0-1.0)
         * @return A new Animating state with the updated progress
         */
        fun withProgress(newProgress: Float): Animating {
            return copy(progress = newProgress.coerceIn(0f, 1f))
        }

        /**
         * Completes the animation by returning an Idle state with the target.
         *
         * @return An Idle state containing the target NavNode
         */
        fun complete(): Idle {
            return Idle(current = target)
        }

        override fun animationComposablePair(): Pair<NavNode, NavNode?> = current to target

        override fun isIntraTabNavigation(tabKey: String): Boolean {
            val currentTab = current.findByKey(tabKey) as? TabNode ?: return false
            val targetTab = target.findByKey(tabKey) as? TabNode ?: return false
            // Intra-tab if both are TabNodes with same key and activeStackIndex is the same
            return currentTab.activeStackIndex == targetTab.activeStackIndex
        }

        override fun isIntraPaneNavigation(paneKey: String): Boolean {
            val currentPane = current.findByKey(paneKey) as? PaneNode ?: return false
            val targetPane = target.findByKey(paneKey) as? PaneNode ?: return false
            // Intra-pane if navigation is within the same pane structure
            return currentPane.key == targetPane.key
        }

        override fun isCrossNodeTypeNavigation(): Boolean {
            return current::class != target::class
        }
    }

    // =========================================================================
    // QUERY PROPERTIES
    // =========================================================================

    /**
     * Returns true if a transition animation is currently in progress.
     */
    val isAnimating: Boolean
        get() = this is Animating

    /**
     * Returns true if a predictive gesture is in progress.
     */
    val isProposed: Boolean
        get() = this is Proposed

    /**
     * Returns true if the navigation is at rest (no transition).
     */
    val isIdle: Boolean
        get() = this is Idle

    /**
     * Returns the current animation/gesture progress.
     *
     * @return Progress value (0.0 to 1.0) for Proposed/Animating states, 0.0 for Idle
     */
    val progressValue: Float
        get() = when (this) {
            is Idle -> 0f
            is Proposed -> progress
            is Animating -> progress
        }

    /**
     * Returns the target state if transitioning, or current if idle.
     */
    val effectiveTarget: NavNode
        get() = when (this) {
            is Idle -> current
            is Proposed -> proposed
            is Animating -> target
        }

    // =========================================================================
    // TRANSITION QUERY METHODS
    // =========================================================================

    /**
     * Checks if this transition affects a specific stack.
     *
     * Used by TreeFlattener to determine which stacks need
     * enter/exit animation handling.
     *
     * @param stackKey The key of the stack to check
     * @return True if the transition involves this stack
     */
    fun affectsStack(stackKey: String): Boolean {
        return when (this) {
            is Idle -> false
            is Proposed -> findChangedStacks(current, proposed).contains(stackKey)
            is Animating -> findChangedStacks(current, target).contains(stackKey)
        }
    }

    /**
     * Checks if this transition affects a specific tab container.
     *
     * @param tabKey The key of the TabNode to check
     * @return True if the transition involves a tab switch in this container
     */
    fun affectsTab(tabKey: String): Boolean {
        return when (this) {
            is Idle -> false
            is Proposed -> hasTabIndexChanged(current, proposed, tabKey)
            is Animating -> hasTabIndexChanged(current, target, tabKey)
        }
    }

    /**
     * Gets the previous child of a stack that is being replaced.
     *
     * During a push transition (FORWARD), this returns the screen being covered
     * (the last child of the current state before the push).
     * During a pop transition (BACKWARD), this returns the screen being revealed
     * (the second-to-last child of the current state, which will become active after pop).
     *
     * @param stackKey The key of the stack
     * @return The previous child NavNode, or null if not applicable
     */
    fun previousChildOf(stackKey: String): NavNode? {
        return when (this) {
            is Idle -> null
            is Proposed -> findLastChild(current, stackKey)
            is Animating -> when (direction) {
                TransitionDirection.FORWARD -> findLastChild(current, stackKey)
                TransitionDirection.BACKWARD -> findSecondToLastChild(current, stackKey)
                TransitionDirection.NONE -> null
            }
        }
    }

    /**
     * Gets the previous tab index during a tab switch.
     *
     * @param tabKey The key of the TabNode
     * @return The previous activeStackIndex, or null if not a tab switch
     */
    fun previousTabIndex(tabKey: String): Int? {
        return when (this) {
            is Idle -> null
            is Proposed, is Animating -> findPreviousTabIndex(current, tabKey)
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private companion object {
        /**
         * Finds stacks that have different children between two states.
         */
        fun findChangedStacks(from: NavNode, to: NavNode): Set<String> {
            val changedStacks = mutableSetOf<String>()
            compareStacks(from, to, changedStacks)
            return changedStacks
        }

        private fun compareStacks(from: NavNode?, to: NavNode?, result: MutableSet<String>) {
            if (from == null || to == null) return

            when {
                from is StackNode && to is StackNode && from.key == to.key -> {
                    if (from.children.size != to.children.size ||
                        from.activeChild?.key != to.activeChild?.key
                    ) {
                        result.add(from.key)
                    }
                    // Recursively check children
                    from.activeChild?.let { fromChild ->
                        to.activeChild?.let { toChild ->
                            compareStacks(fromChild, toChild, result)
                        }
                    }
                }
                from is TabNode && to is TabNode && from.key == to.key -> {
                    from.stacks.zip(to.stacks).forEach { (fromStack, toStack) ->
                        compareStacks(fromStack, toStack, result)
                    }
                }
                from is PaneNode && to is PaneNode && from.key == to.key -> {
                    // Compare pane contents by role
                    val fromPanes = from.paneConfigurations.values.map { it.content }
                    val toPanes = to.paneConfigurations.values.map { it.content }
                    fromPanes.zip(toPanes).forEach { (fromPane, toPane) ->
                        compareStacks(fromPane, toPane, result)
                    }
                }
            }
        }

        /**
         * Checks if a tab's activeStackIndex changed between two states.
         */
        fun hasTabIndexChanged(from: NavNode, to: NavNode, tabKey: String): Boolean {
            val fromTab = from.findByKey(tabKey) as? TabNode ?: return false
            val toTab = to.findByKey(tabKey) as? TabNode ?: return false
            return fromTab.activeStackIndex != toTab.activeStackIndex
        }

        /**
         * Finds the last (active) child of a stack in the given state.
         */
        fun findLastChild(state: NavNode, stackKey: String): NavNode? {
            val stack = state.findByKey(stackKey) as? StackNode ?: return null
            return stack.children.lastOrNull()
        }

        /**
         * Finds the second-to-last child of a stack in the given state.
         * Returns null if stack has fewer than 2 children.
         */
        fun findSecondToLastChild(state: NavNode, stackKey: String): NavNode? {
            val stack = state.findByKey(stackKey) as? StackNode ?: return null
            return if (stack.children.size >= 2) {
                stack.children[stack.children.size - 2]
            } else {
                null
            }
        }

        /**
         * Finds the activeStackIndex of a TabNode in the given state.
         */
        fun findPreviousTabIndex(state: NavNode, tabKey: String): Int? {
            val tab = state.findByKey(tabKey) as? TabNode ?: return null
            return tab.activeStackIndex
        }
    }
}

/**
 * Direction of navigation transition.
 */
@Serializable
enum class TransitionDirection {
    /** Moving forward in navigation (push, higher tab index) */
    FORWARD,

    /** Moving backward in navigation (pop, lower tab index) */
    BACKWARD,

    /** No transition or direction not applicable */
    NONE
}

/**
 * Manages TransitionState as a StateFlow for reactive observation.
 *
 * This class provides a controlled interface for updating transition state
 * with proper state machine semantics. It enforces valid state transitions
 * and provides reactive observation via StateFlow.
 *
 * ## State Machine
 *
 * ```
 * ┌─────────┐  startAnimation()  ┌───────────┐  completeAnimation()  ┌─────────┐
 * │  Idle   │───────────────────►│ Animating │──────────────────────►│  Idle   │
 * └─────────┘                    └───────────┘                       └─────────┘
 *      │                              ▲
 *      │ startProposed()              │ commitProposed()
 *      ▼                              │
 * ┌───────────┐───────────────────────┘
 * │ Proposed  │
 * └───────────┘
 *      │
 *      │ cancelProposed()
 *      ▼
 * ┌─────────┐
 * │  Idle   │
 * └─────────┘
 * ```
 *
 * @param initialState The initial NavNode to start with
 */
class TransitionStateManager(
    initialState: NavNode
) {
    private val _state = MutableStateFlow<TransitionState>(TransitionState.Idle(initialState))

    /**
     * Observable state flow of transition state.
     */
    val state: StateFlow<TransitionState> = _state.asStateFlow()

    /**
     * The current transition state value.
     */
    val currentState: TransitionState
        get() = _state.value

    /**
     * Starts a navigation animation.
     *
     * Transitions from Idle → Animating.
     *
     * @param target The target navigation state
     * @param direction Direction of the navigation
     * @throws IllegalStateException if not currently Idle
     */
    fun startAnimation(
        target: NavNode,
        direction: TransitionDirection
    ) {
        val current = _state.value
        require(current is TransitionState.Idle) {
            "Cannot start animation from state: ${current::class.simpleName}"
        }

        _state.value = TransitionState.Animating(
            current = current.current,
            target = target,
            progress = 0f,
            direction = direction
        )
    }

    /**
     * Starts a proposed (gesture-driven) transition.
     *
     * Transitions from Idle → Proposed.
     *
     * @param proposed The proposed target state if gesture completes
     * @throws IllegalStateException if not currently Idle
     */
    fun startProposed(proposed: NavNode) {
        val current = _state.value
        require(current is TransitionState.Idle) {
            "Cannot start proposed from state: ${current::class.simpleName}"
        }

        _state.value = TransitionState.Proposed(
            current = current.current,
            proposed = proposed,
            progress = 0f
        )
    }

    /**
     * Updates the progress of the current transition.
     *
     * Only affects Proposed and Animating states. Has no effect on Idle.
     *
     * @param progress New progress value (0.0 to 1.0)
     */
    fun updateProgress(progress: Float) {
        _state.value = when (val current = _state.value) {
            is TransitionState.Idle -> current
            is TransitionState.Proposed -> current.withProgress(progress)
            is TransitionState.Animating -> current.withProgress(progress)
        }
    }

    /**
     * Commits a proposed transition, starting the animation.
     *
     * Transitions from Proposed → Animating.
     *
     * @throws IllegalStateException if not currently Proposed
     */
    fun commitProposed() {
        val current = _state.value
        require(current is TransitionState.Proposed) {
            "Cannot commit from state: ${current::class.simpleName}"
        }

        _state.value = TransitionState.Animating(
            current = current.current,
            target = current.proposed,
            progress = current.progress, // Continue from gesture progress
            direction = TransitionDirection.BACKWARD
        )
    }

    /**
     * Cancels a proposed transition, returning to idle.
     *
     * Transitions from Proposed → Idle.
     *
     * @throws IllegalStateException if not currently Proposed
     */
    fun cancelProposed() {
        val current = _state.value
        require(current is TransitionState.Proposed) {
            "Cannot cancel from state: ${current::class.simpleName}"
        }

        _state.value = TransitionState.Idle(current = current.current)
    }

    /**
     * Completes an animation, transitioning to idle with the target state.
     *
     * Transitions from Animating → Idle.
     *
     * @throws IllegalStateException if not currently Animating
     */
    fun completeAnimation() {
        val current = _state.value
        require(current is TransitionState.Animating) {
            "Cannot complete from state: ${current::class.simpleName}"
        }

        _state.value = current.complete()
    }

    /**
     * Force sets the state to Idle with the given node.
     *
     * Use sparingly - this bypasses normal state transitions.
     * Useful for initialization or error recovery.
     *
     * @param state The NavNode to set as the current idle state
     */
    fun forceIdle(state: NavNode) {
        _state.value = TransitionState.Idle(current = state)
    }
}

// =============================================================================
// BACKWARD COMPATIBILITY EXTENSIONS
// =============================================================================

/**
 * Extension to check if any transition is active.
 *
 * @return True if the state is Animating, false otherwise
 */
val TransitionState.isAnimating: Boolean
    get() = this is TransitionState.Animating

/**
 * Extension to get the current progress, regardless of transition type.
 *
 * @return Progress value (0.0 to 1.0) for Proposed/Animating states, 0.0 for Idle
 */
val TransitionState.progress: Float
    get() = when (this) {
        is TransitionState.Idle -> 0f
        is TransitionState.Proposed -> progress
        is TransitionState.Animating -> progress
    }
