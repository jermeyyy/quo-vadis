# RENDER-003: Create TransitionState Sealed Class

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-003 |
| **Task Name** | Create TransitionState Sealed Class |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | CORE-001 |
| **Blocked By** | CORE-001 |
| **Blocks** | RENDER-002, RENDER-004, RENDER-005 |

---

## Overview

The `TransitionState` sealed class manages navigation transitions, including:

1. **Static states** - When navigation is at rest
2. **Proposed states** - During predictive back gestures (user dragging)
3. **Animating states** - After user commits/releases gesture

This state machine enables smooth coordination between:

- The TreeFlattener (which surfaces to render)
- The QuoVadisHost (what animations to play)
- The PredictiveBackHandler (gesture-driven transitions)

### State Machine

```
┌─────────┐  navigate()   ┌───────────┐  animation   ┌─────────┐
│  Idle   │──────────────►│ Animating │──────────────►│  Idle   │
└─────────┘               └───────────┘  complete     └─────────┘
     ▲                         ▲
     │                         │ commit
     │    ┌───────────┐        │
     └────│ Proposed  │────────┘
  cancel  └───────────┘
          gesture start
```

### Design Goals

| Goal | Approach |
|------|----------|
| **State consistency** | Single source of truth via StateFlow |
| **Gesture support** | Proposed state tracks gesture progress |
| **Animation coordination** | Animating state drives enter/exit |
| **Rollback capability** | Proposed can be cancelled |
| **Type safety** | Sealed class with exhaustive handling |

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TransitionState.kt
```

---

## Implementation

### Core Sealed Hierarchy

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
 * @see Navigator
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
         * Returns a new Proposed state with the updated progress.
         */
        fun withProgress(newProgress: Float): Proposed {
            return copy(progress = newProgress.coerceIn(0f, 1f))
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
         * Returns a new Animating state with the updated progress.
         */
        fun withProgress(newProgress: Float): Animating {
            return copy(progress = newProgress.coerceIn(0f, 1f))
        }
        
        /**
         * Completes the animation by returning an Idle state with the target.
         */
        fun complete(): Idle {
            return Idle(current = target)
        }
    }
    
    // =========================================================================
    // QUERY METHODS
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
     * Returns the current animation/gesture progress, or null if idle.
     */
    val progress: Float?
        get() = when (this) {
            is Idle -> null
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
     * During a push transition, this returns the screen being covered.
     * During a pop transition, this returns the screen being revealed.
     * 
     * @param stackKey The key of the stack
     * @return The previous child NavNode, or null if not applicable
     */
    fun previousChildOf(stackKey: String): NavNode? {
        return when (this) {
            is Idle -> null
            is Proposed -> findPreviousChild(current, stackKey)
            is Animating -> findPreviousChild(current, stackKey)
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
                        from.activeChild?.key != to.activeChild?.key) {
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
                    from.panes.zip(to.panes).forEach { (fromPane, toPane) ->
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
         * Finds the previous active child of a stack in the given state.
         */
        fun findPreviousChild(state: NavNode, stackKey: String): NavNode? {
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
```

### StateFlow Management

```kotlin
/**
 * Manages TransitionState as a StateFlow for reactive observation.
 * 
 * This class provides a controlled interface for updating transition state
 * with proper state machine semantics.
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
     */
    fun forceIdle(state: NavNode) {
        _state.value = TransitionState.Idle(current = state)
    }
}
```

### Integration with Navigator

```kotlin
/**
 * Extension functions for Navigator integration.
 */

/**
 * Performs a navigation action with proper transition state management.
 * 
 * @param action The navigation action (returns new state if successful)
 * @param direction The direction of navigation
 * @param transitionManager The TransitionStateManager to update
 */
suspend fun Navigator.navigateWithTransition(
    action: (NavNode) -> NavNode?,
    direction: TransitionDirection,
    transitionManager: TransitionStateManager,
    animationDuration: Long = 300
) {
    val currentState = transitionManager.currentState
    if (!currentState.isIdle) return // Don't interrupt existing transition
    
    val current = currentState.current
    val newState = action(current) ?: return
    
    // Start animation
    transitionManager.startAnimation(newState, direction)
    
    // Animate progress (simplified - real implementation uses Animatable)
    // ...animation logic...
    
    // Complete transition
    transitionManager.completeAnimation()
}
```

---

## State Transition Diagrams

### Push Navigation

```
User calls: navigator.push(destination)

┌─────────────────┐
│  Idle(current)  │
└────────┬────────┘
         │ push()
         ▼
┌───────────────────────────────┐
│  Animating(                   │
│    current = old,             │
│    target = new,              │
│    direction = FORWARD        │
│  )                            │
└────────┬──────────────────────┘
         │ animation complete
         ▼
┌─────────────────┐
│  Idle(new)      │
└─────────────────┘
```

### Predictive Back Pop

```
User drags back gesture

┌─────────────────┐
│  Idle(current)  │
└────────┬────────┘
         │ onBackStarted()
         ▼
┌───────────────────────────────┐
│  Proposed(                    │
│    current = stack[A,B],      │
│    proposed = stack[A],       │
│    progress = 0.0             │
│  )                            │
└────────┬──────────────────────┘
         │ onBackProgress(0.3)
         ▼
┌───────────────────────────────┐
│  Proposed(                    │
│    current = stack[A,B],      │
│    proposed = stack[A],       │
│    progress = 0.3             │
│  )                            │
└──────────┬────────────────────┘
           │
     ┌─────┴─────┐
     │           │
     ▼           ▼
onCommit()   onCancel()
     │           │
     ▼           ▼
┌────────────┐  ┌────────────┐
│ Animating  │  │ Idle       │
│ (complete  │  │ (original) │
│  pop)      │  │            │
└────────────┘  └────────────┘
```

### Tab Switch

```
User switches from tab 0 to tab 1

┌─────────────────────┐
│  Idle(              │
│    TabNode(         │
│      activeIndex=0  │
│    )                │
│  )                  │
└────────┬────────────┘
         │ switchTab(1)
         ▼
┌─────────────────────────────────┐
│  Animating(                     │
│    current = TabNode(index=0),  │
│    target = TabNode(index=1),   │
│    direction = FORWARD          │
│  )                              │
└────────┬────────────────────────┘
         │ animation complete
         ▼
┌─────────────────────┐
│  Idle(              │
│    TabNode(         │
│      activeIndex=1  │
│    )                │
│  )                  │
└─────────────────────┘
```

---

## Implementation Steps

### Step 1: Create Core Types

1. Create `TransitionState.kt` file
2. Define `TransitionDirection` enum
3. Implement `TransitionState` sealed class with Idle, Proposed, Animating

### Step 2: Add Query Methods

1. Implement `affectsStack()` with tree diffing
2. Implement `affectsTab()` for tab switch detection
3. Implement `previousChildOf()` and `previousTabIndex()`
4. Add computed properties (`isAnimating`, `progress`, etc.)

### Step 3: Create State Manager

1. Implement `TransitionStateManager` class
2. Add `startAnimation()`, `startProposed()` methods
3. Add `updateProgress()`, `commitProposed()`, `cancelProposed()`
4. Add `completeAnimation()` and `forceIdle()`

### Step 4: Integration

1. Add Navigator extension functions
2. Wire up with QuoVadisHost (in RENDER-004)
3. Connect to PredictiveBackHandler (in RENDER-005)

### Step 5: Testing

1. Unit tests for state transitions
2. Unit tests for query methods
3. Integration tests with TreeFlattener

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../core/TransitionState.kt` | Create | Main implementation |
| `quo-vadis-core/.../core/NavNode.kt` | Reference | Uses NavNode types |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| CORE-001 (NavNode Hierarchy) | Hard | Must complete first |

---

## Acceptance Criteria

- [ ] `TransitionState` sealed class with Idle, Proposed, Animating variants
- [ ] `TransitionDirection` enum with FORWARD, BACKWARD, NONE
- [ ] `Idle` state holds current NavNode
- [ ] `Proposed` state holds current, proposed, and progress
- [ ] `Animating` state holds current, target, progress, and direction
- [ ] `affectsStack()` correctly identifies stacks involved in transition
- [ ] `affectsTab()` correctly identifies tab switches
- [ ] `previousChildOf()` returns the screen being covered/revealed
- [ ] `previousTabIndex()` returns previous tab during switch
- [ ] `TransitionStateManager` with proper state machine semantics
- [ ] StateFlow exposure for reactive observation
- [ ] All states are `@Serializable` (for state restoration)
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for all state transitions
- [ ] Unit tests for query methods

---

## Testing Notes

```kotlin
@Test
fun `Idle state has no animation progress`() {
    val idle = TransitionState.Idle(mockNavNode)
    
    assertNull(idle.progress)
    assertTrue(idle.isIdle)
    assertFalse(idle.isAnimating)
}

@Test
fun `Proposed state tracks gesture progress`() {
    val proposed = TransitionState.Proposed(
        current = mockCurrent,
        proposed = mockProposed,
        progress = 0.5f
    )
    
    assertEquals(0.5f, proposed.progress)
    assertTrue(proposed.isProposed)
}

@Test
fun `Proposed withProgress clamps value`() {
    val proposed = TransitionState.Proposed(mockCurrent, mockProposed, 0.5f)
    
    val updated = proposed.withProgress(1.5f)
    
    assertEquals(1.0f, updated.progress)
}

@Test
fun `Animating complete returns Idle with target`() {
    val animating = TransitionState.Animating(
        current = mockCurrent,
        target = mockTarget,
        progress = 1.0f,
        direction = TransitionDirection.FORWARD
    )
    
    val idle = animating.complete()
    
    assertTrue(idle is TransitionState.Idle)
    assertEquals(mockTarget, idle.current)
}

@Test
fun `affectsStack returns true when stack children changed`() {
    val screen1 = ScreenNode("s1", "stack", mockDest)
    val screen2 = ScreenNode("s2", "stack", mockDest)
    val oldStack = StackNode("stack", null, listOf(screen1))
    val newStack = StackNode("stack", null, listOf(screen1, screen2))
    
    val animating = TransitionState.Animating(
        current = oldStack,
        target = newStack,
        progress = 0.5f,
        direction = TransitionDirection.FORWARD
    )
    
    assertTrue(animating.affectsStack("stack"))
}

@Test
fun `TransitionStateManager enforces state machine`() {
    val manager = TransitionStateManager(mockNavNode)
    
    // Valid: Idle → Animating
    manager.startAnimation(mockTarget, TransitionDirection.FORWARD)
    assertTrue(manager.currentState.isAnimating)
    
    // Complete: Animating → Idle
    manager.updateProgress(1.0f)
    manager.completeAnimation()
    assertTrue(manager.currentState.isIdle)
}

@Test
fun `TransitionStateManager prevents invalid transitions`() {
    val manager = TransitionStateManager(mockNavNode)
    manager.startAnimation(mockTarget, TransitionDirection.FORWARD)
    
    // Invalid: Animating → Proposed
    assertThrows<IllegalArgumentException> {
        manager.startProposed(mockProposed)
    }
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-002](./RENDER-002-flatten-algorithm.md) - TreeFlattener uses TransitionState
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost observes TransitionState
- [RENDER-005](./RENDER-005-predictive-back.md) - Predictive back creates Proposed states
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [CORE-003](../phase1-core/CORE-003-navigator-refactor.md) - Navigator integration
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.2.3 "Transition State Machine"
