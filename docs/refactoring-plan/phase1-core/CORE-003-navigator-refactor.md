# CORE-003: Refactor Navigator to StateFlow<NavNode>

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | CORE-003 |
| **Task Name** | Refactor Navigator to StateFlow<NavNode> |
| **Phase** | Phase 1: Core State Refactoring |
| **Complexity** | High |
| **Estimated Time** | 4-5 days |
| **Dependencies** | CORE-001, CORE-002 |
| **Blocked By** | CORE-001, CORE-002 |
| **Blocks** | CORE-004, CORE-005, RENDER-* |

---

## Overview

This task refactors the core `Navigator` interface and its `DefaultNavigator` implementation to use `StateFlow<NavNode>` as the single source of truth, replacing the current `BackStack`-based approach.

### Current vs Target Architecture

| Aspect | Current Implementation | Target Implementation |
|--------|----------------------|----------------------|
| **State Model** | `MutableBackStack` with `SnapshotStateList<BackStackEntry>` | `MutableStateFlow<NavNode>` |
| **State Access** | `backStack`, `entries`, `currentDestination`, `previousDestination` | `state: StateFlow<NavNode>` + derived properties |
| **Mutations** | Direct list manipulation via `BackStack` | Pure functions via `TreeMutator` |
| **Thread Safety** | Snapshot state synchronization | StateFlow atomic updates |
| **Serialization** | Per-entry serialization | Full tree serialization |

---

## Current Navigator Structure Analysis

Based on the existing codebase:

### Navigator Interface (Current)

```kotlin
@Stable
interface Navigator : ParentNavigator {
    val backStack: BackStack
    val entries: SnapshotStateList<BackStackEntry>
    val currentDestination: StateFlow<Destination?>
    val previousDestination: StateFlow<Destination?>
    val currentTransition: StateFlow<NavigationTransition?>
    
    fun navigate(destination: Destination, transition: NavigationTransition? = null)
    fun navigateBack(): Boolean
    fun navigateUp(): Boolean
    fun navigateAndClearTo(destination: Destination, clearRoute: String?, inclusive: Boolean)
    fun navigateAndReplace(destination: Destination, transition: NavigationTransition?)
    fun navigateAndClearAll(destination: Destination)
    fun handleDeepLink(deepLink: DeepLink)
    fun registerGraph(graph: NavigationGraph)
    fun setStartDestination(destination: Destination)
    fun getDeepLinkHandler(): DeepLinkHandler
    fun setActiveChild(child: BackPressHandler?)
}
```

### DefaultNavigator (Current)

```kotlin
class DefaultNavigator(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler()
) : Navigator {
    private val _backStack: MutableBackStack = MutableBackStack()
    override val backStack: BackStack = _backStack
    
    private val _currentDestination = MutableStateFlow<Destination?>(...)
    private val _previousDestination = MutableStateFlow<Destination?>(...)
    private val _currentTransition = MutableStateFlow<NavigationTransition?>(null)
    
    private val graphs = mutableMapOf<String, NavigationGraph>()
    private var _activeChild: BackPressHandler? = null
    
    // ... methods
}
```

---

## File Locations

| File | Action |
|------|--------|
| `quo-vadis-core/.../core/Navigator.kt` | Modify |
| `quo-vadis-core/.../core/TreeNavigator.kt` | Create (new implementation) |
| `quo-vadis-core/.../core/NavigatorFactory.kt` | Create (factory for both types) |

---

## Implementation

### Phase 1: Add Tree-Based State to Interface

First, extend the `Navigator` interface to support both models during migration:

```kotlin
package com.jermey.quo.vadis.core.navigation.core

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
 * All mutations are performed through [TreeMutator] operations.
 */
@Stable
interface Navigator : ParentNavigator {
    
    // =========================================================================
    // NEW TREE-BASED STATE
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
     * During navigation, this holds the previous and next states
     * along with transition metadata for animation coordination.
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
    val currentDestination: StateFlow<Destination?>
    
    /**
     * The previous destination before the current one.
     * 
     * Derived from the active stack's second-to-last entry.
     */
    val previousDestination: StateFlow<Destination?>
    
    /**
     * Current transition animation (null if idle).
     * 
     * @deprecated Use [transitionState] for more detailed transition info
     */
    @Deprecated("Use transitionState instead", ReplaceWith("transitionState"))
    val currentTransition: StateFlow<NavigationTransition?>
    
    // =========================================================================
    // LEGACY COMPATIBILITY (Deprecated)
    // =========================================================================
    
    /**
     * Access to the backstack for direct manipulation.
     * 
     * @deprecated Use [state] and [TreeMutator] operations instead.
     *             This property is provided for backward compatibility during migration.
     */
    @Deprecated(
        message = "Use state and TreeMutator for navigation operations",
        replaceWith = ReplaceWith("state")
    )
    val backStack: BackStack
    
    /**
     * Direct access to backstack entries for navigation.
     * 
     * @deprecated Use [state] tree traversal instead.
     */
    @Deprecated(
        message = "Use state tree traversal instead",
        replaceWith = ReplaceWith("state.value.activePathToLeaf()")
    )
    val entries: SnapshotStateList<BackStackEntry>
        get() = backStack.entries
    
    // =========================================================================
    // NAVIGATION OPERATIONS
    // =========================================================================
    
    /**
     * Navigate to a destination with optional transition.
     * 
     * Pushes the destination onto the deepest active stack.
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
     */
    fun navigateUp(): Boolean = navigateBack()
    
    /**
     * Navigate to a destination and clear the backstack up to a certain point.
     */
    fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String? = null,
        inclusive: Boolean = false
    )
    
    /**
     * Navigate to a destination and replace the current one.
     */
    fun navigateAndReplace(
        destination: Destination,
        transition: NavigationTransition? = null
    )
    
    /**
     * Navigate to a destination and clear the entire active stack.
     */
    fun navigateAndClearAll(destination: Destination)
    
    // =========================================================================
    // TAB NAVIGATION
    // =========================================================================
    
    /**
     * Switch to a different tab in the active TabNode.
     * 
     * @param tabIndex The index of the tab to switch to
     * @return true if successful, false if no TabNode in active path
     */
    fun switchTab(tabIndex: Int): Boolean
    
    /**
     * Switch to a tab in a specific TabNode.
     * 
     * @param tabNodeKey Key of the target TabNode
     * @param tabIndex The index of the tab to switch to
     */
    fun switchTab(tabNodeKey: String, tabIndex: Int)
    
    // =========================================================================
    // PANE NAVIGATION
    // =========================================================================
    
    /**
     * Set the active pane in a PaneNode.
     * 
     * @param paneNodeKey Key of the target PaneNode
     * @param paneIndex The index of the pane to activate
     */
    fun setActivePane(paneNodeKey: String, paneIndex: Int)
    
    /**
     * Navigate within a specific pane.
     * 
     * @param paneNodeKey Key of the target PaneNode
     * @param paneIndex The index of the target pane
     * @param destination The destination to navigate to
     */
    fun navigateInPane(
        paneNodeKey: String,
        paneIndex: Int,
        destination: Destination,
        transition: NavigationTransition? = null
    )
    
    // =========================================================================
    // DEEP LINK & GRAPH REGISTRATION
    // =========================================================================
    
    /**
     * Handle deep link navigation.
     */
    fun handleDeepLink(deepLink: DeepLink)
    
    /**
     * Register a navigation graph for modular navigation.
     */
    fun registerGraph(graph: NavigationGraph)
    
    /**
     * Set the start destination (resets navigation state).
     */
    fun setStartDestination(destination: Destination)
    
    /**
     * Get the deep link handler to register patterns.
     */
    fun getDeepLinkHandler(): DeepLinkHandler
    
    // =========================================================================
    // CHILD NAVIGATOR SUPPORT
    // =========================================================================
    
    /**
     * Set the active child navigator for back press delegation.
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
}
```

### Phase 2: TransitionState for Animations

```kotlin
/**
 * Represents the current state of navigation transitions.
 * 
 * Used by renderers to coordinate animations between states.
 */
sealed interface TransitionState {
    
    /**
     * No transition in progress - showing current state.
     */
    data class Idle(
        val currentState: NavNode
    ) : TransitionState
    
    /**
     * User is performing a predictive back gesture.
     * 
     * Renders both [exitingState] and [enteringState] based on [progress].
     */
    data class Proposed(
        val exitingState: NavNode,
        val enteringState: NavNode,
        val progress: Float,
        val transition: NavigationTransition
    ) : TransitionState
    
    /**
     * Automated transition animation in progress.
     * 
     * The renderer interpolates between states based on [progress].
     */
    data class Animating(
        val exitingState: NavNode,
        val enteringState: NavNode,
        val progress: Float,
        val transition: NavigationTransition
    ) : TransitionState
}
```

### Phase 3: TreeNavigator Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshots.SnapshotStateList
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
 * This is the new primary implementation of Navigator that uses an
 * immutable tree structure for navigation state management.
 */
@OptIn(ExperimentalUuidApi::class)
@Stable
class TreeNavigator(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
    initialState: NavNode? = null
) : Navigator {
    
    // =========================================================================
    // STATE MANAGEMENT
    // =========================================================================
    
    private val _state = MutableStateFlow<NavNode>(
        initialState ?: createDefaultState()
    )
    override val state: StateFlow<NavNode> = _state.asStateFlow()
    
    private val _transitionState = MutableStateFlow<TransitionState>(
        TransitionState.Idle(_state.value)
    )
    override val transitionState: StateFlow<TransitionState> = _transitionState.asStateFlow()
    
    // Derived state flows
    override val currentDestination: StateFlow<Destination?> = state
        .map { it.activeLeaf()?.destination }
        .stateIn(scope, SharingStarted.Eagerly, state.value.activeLeaf()?.destination)
    
    override val previousDestination: StateFlow<Destination?> = state
        .map { findPreviousDestination(it) }
        .stateIn(scope, SharingStarted.Eagerly, findPreviousDestination(state.value))
    
    @Deprecated("Use transitionState instead")
    private val _currentTransition = MutableStateFlow<NavigationTransition?>(null)
    @Deprecated("Use transitionState instead")
    override val currentTransition: StateFlow<NavigationTransition?> = _currentTransition.asStateFlow()
    
    // =========================================================================
    // LEGACY COMPATIBILITY
    // =========================================================================
    
    @Deprecated("Use state and TreeMutator for navigation operations")
    private val _legacyBackStack = LegacyBackStackAdapter(this)
    
    @Deprecated("Use state and TreeMutator for navigation operations")
    override val backStack: BackStack
        get() = _legacyBackStack
    
    // =========================================================================
    // GRAPHS & DEEP LINKS
    // =========================================================================
    
    private val graphs = mutableMapOf<String, NavigationGraph>()
    
    // =========================================================================
    // CHILD NAVIGATOR
    // =========================================================================
    
    private var _activeChild: BackPressHandler? = null
    override val activeChild: BackPressHandler?
        get() = _activeChild
    
    // =========================================================================
    // NAVIGATION OPERATIONS
    // =========================================================================
    
    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        val effectiveTransition = transition ?: destination.transition
        val currentState = _state.value
        
        try {
            val newState = TreeMutator.push(
                root = currentState,
                destination = destination,
                generateKey = { Uuid.random().toString().take(8) }
            )
            
            applyStateChange(currentState, newState, effectiveTransition)
        } catch (e: IllegalStateException) {
            // No active stack found - might need to create root structure
            val rootStack = StackNode(
                key = "root",
                parentKey = null,
                children = listOf(
                    ScreenNode(
                        key = Uuid.random().toString().take(8),
                        parentKey = "root",
                        destination = destination
                    )
                )
            )
            applyStateChange(currentState, rootStack, effectiveTransition)
        }
    }
    
    override fun navigateBack(): Boolean {
        // First try to delegate to child
        if (onBack()) {
            return true
        }
        
        return handleBackInternal()
    }
    
    override fun handleBackInternal(): Boolean {
        val currentState = _state.value
        val newState = TreeMutator.pop(currentState) ?: return false
        
        val previousLeaf = newState.activeLeaf()
        val transition = previousLeaf?.destination?.transition
        
        applyStateChange(currentState, newState, transition)
        return true
    }
    
    override fun navigateAndClearTo(
        destination: Destination,
        clearRoute: String?,
        inclusive: Boolean
    ) {
        val currentState = _state.value
        
        var newState = if (clearRoute != null) {
            TreeMutator.popToRoute(currentState, clearRoute, inclusive)
        } else {
            currentState
        }
        
        newState = TreeMutator.push(newState, destination)
        applyStateChange(currentState, newState, null)
    }
    
    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        val currentState = _state.value
        val newState = TreeMutator.replaceCurrent(currentState, destination)
        applyStateChange(currentState, newState, transition)
    }
    
    override fun navigateAndClearAll(destination: Destination) {
        val currentState = _state.value
        val newState = TreeMutator.clearAndPush(currentState, destination)
        applyStateChange(currentState, newState, null)
    }
    
    // =========================================================================
    // TAB OPERATIONS
    // =========================================================================
    
    override fun switchTab(tabIndex: Int): Boolean {
        return try {
            val currentState = _state.value
            val newState = TreeMutator.switchActiveTab(currentState, tabIndex)
            applyStateChange(currentState, newState, null)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }
    
    override fun switchTab(tabNodeKey: String, tabIndex: Int) {
        val currentState = _state.value
        val newState = TreeMutator.switchTab(currentState, tabNodeKey, tabIndex)
        applyStateChange(currentState, newState, null)
    }
    
    // =========================================================================
    // PANE OPERATIONS
    // =========================================================================
    
    override fun setActivePane(paneNodeKey: String, paneIndex: Int) {
        val currentState = _state.value
        val newState = TreeMutator.setActivePane(currentState, paneNodeKey, paneIndex)
        applyStateChange(currentState, newState, null)
    }
    
    override fun navigateInPane(
        paneNodeKey: String,
        paneIndex: Int,
        destination: Destination,
        transition: NavigationTransition?
    ) {
        val currentState = _state.value
        val paneNode = currentState.findByKey(paneNodeKey) as? PaneNode
            ?: throw IllegalArgumentException("No PaneNode found: $paneNodeKey")
        
        val targetPane = paneNode.panes[paneIndex]
        val targetStack = targetPane.activeStack()
            ?: throw IllegalArgumentException("No stack in pane $paneIndex")
        
        val newState = TreeMutator.pushToStack(currentState, targetStack.key, destination)
        applyStateChange(currentState, newState, transition)
    }
    
    // =========================================================================
    // STATE MANAGEMENT
    // =========================================================================
    
    override fun updateState(newState: NavNode, transition: NavigationTransition?) {
        val currentState = _state.value
        applyStateChange(currentState, newState, transition)
    }
    
    private fun applyStateChange(
        oldState: NavNode,
        newState: NavNode,
        transition: NavigationTransition?
    ) {
        if (transition != null) {
            // Start animated transition
            _transitionState.value = TransitionState.Animating(
                exitingState = oldState,
                enteringState = newState,
                progress = 0f,
                transition = transition
            )
        }
        
        _state.value = newState
        @Suppress("DEPRECATION")
        _currentTransition.value = transition
        
        if (transition == null) {
            _transitionState.value = TransitionState.Idle(newState)
        }
    }
    
    /**
     * Update transition progress during predictive back.
     */
    fun updateTransitionProgress(progress: Float) {
        val current = _transitionState.value
        when (current) {
            is TransitionState.Proposed -> {
                _transitionState.value = current.copy(progress = progress)
            }
            is TransitionState.Animating -> {
                _transitionState.value = current.copy(progress = progress)
            }
            is TransitionState.Idle -> {
                // Start proposed state for predictive back
                val previous = TreeMutator.pop(_state.value)
                if (previous != null) {
                    _transitionState.value = TransitionState.Proposed(
                        exitingState = _state.value,
                        enteringState = previous,
                        progress = progress,
                        transition = NavigationTransition.Default
                    )
                }
            }
        }
    }
    
    /**
     * Complete the current transition.
     */
    fun completeTransition() {
        _transitionState.value = TransitionState.Idle(_state.value)
        @Suppress("DEPRECATION")
        _currentTransition.value = null
    }
    
    /**
     * Cancel the current transition (e.g., predictive back cancelled).
     */
    fun cancelTransition() {
        val current = _transitionState.value
        if (current is TransitionState.Proposed) {
            _transitionState.value = TransitionState.Idle(current.exitingState)
        }
    }
    
    // =========================================================================
    // DEEP LINK & GRAPH
    // =========================================================================
    
    override fun handleDeepLink(deepLink: DeepLink) {
        deepLinkHandler.handle(deepLink, this, graphs)
    }
    
    override fun registerGraph(graph: NavigationGraph) {
        graphs[graph.graphRoute] = graph
    }
    
    override fun setStartDestination(destination: Destination) {
        val newState = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode(
                    key = Uuid.random().toString().take(8),
                    parentKey = "root",
                    destination = destination
                )
            )
        )
        _state.value = newState
        _transitionState.value = TransitionState.Idle(newState)
    }
    
    override fun getDeepLinkHandler(): DeepLinkHandler = deepLinkHandler
    
    override fun setActiveChild(child: BackPressHandler?) {
        _activeChild = child
    }
    
    // =========================================================================
    // HELPERS
    // =========================================================================
    
    private fun createDefaultState(): NavNode {
        return StackNode(key = "root", parentKey = null, children = emptyList())
    }
    
    private fun findPreviousDestination(state: NavNode): Destination? {
        val activeStack = state.activeStack() ?: return null
        val children = activeStack.children
        if (children.size < 2) return null
        
        val previousChild = children[children.size - 2]
        return (previousChild as? ScreenNode)?.destination
    }
}
```

### Phase 4: Legacy BackStack Adapter

```kotlin
/**
 * Adapter that provides backward-compatible BackStack interface
 * by projecting the NavNode tree state.
 * 
 * @see BackStack
 */
@Deprecated("For migration only - use TreeMutator operations")
internal class LegacyBackStackAdapter(
    private val navigator: TreeNavigator
) : BackStack {
    
    private val _entries = mutableStateListOf<BackStackEntry>()
    override val entries: SnapshotStateList<BackStackEntry>
        get() {
            // Sync entries with current tree state
            syncEntries()
            return _entries
        }
    
    override val current: StateFlow<BackStackEntry?> = navigator.state
        .map { state ->
            val leaf = state.activeLeaf() ?: return@map null
            BackStackEntry(leaf.destination, leaf.destination.transition)
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    
    override val previous: StateFlow<BackStackEntry?> = navigator.state
        .map { state ->
            val stack = state.activeStack() ?: return@map null
            if (stack.children.size < 2) return@map null
            val prev = stack.children[stack.children.size - 2] as? ScreenNode
            prev?.let { BackStackEntry(it.destination, it.destination.transition) }
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    
    override val canGoBack: StateFlow<Boolean> = navigator.state
        .map { state ->
            val stack = state.activeStack() ?: return@map false
            stack.canGoBack
        }
        .stateIn(
            scope = CoroutineScope(Dispatchers.Main),
            started = SharingStarted.Eagerly,
            initialValue = false
        )
    
    private fun syncEntries() {
        val currentPath = navigator.state.value.activePathToLeaf()
            .filterIsInstance<ScreenNode>()
            .map { BackStackEntry(it.destination, it.destination.transition) }
        
        _entries.clear()
        _entries.addAll(currentPath)
    }
    
    // Write operations delegate to TreeMutator
    // These are deprecated and should not be used
}
```

---

## Thread Safety Considerations

### StateFlow Guarantees

- `MutableStateFlow.value = newValue` is atomic
- Collectors receive consistent snapshots
- No locks required for read operations

### Derived State Flows

Use `stateIn()` with appropriate sharing:

```kotlin
override val currentDestination: StateFlow<Destination?> = state
    .map { it.activeLeaf()?.destination }
    .stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,  // For immediate access
        initialValue = state.value.activeLeaf()?.destination
    )
```

### Transition State Updates

Transition progress updates during gestures must be fast:

```kotlin
fun updateTransitionProgress(progress: Float) {
    // Direct StateFlow update - no computation
    val current = _transitionState.value
    if (current is TransitionState.Proposed) {
        _transitionState.value = current.copy(progress = progress)
    }
}
```

---

## Migration Strategy

### Phase 1: Parallel Implementation

1. Create `TreeNavigator` as a new class
2. Keep `DefaultNavigator` unchanged
3. Provide factory function to choose implementation

### Phase 2: Interface Evolution

1. Add tree-based properties to `Navigator` interface
2. Implement adapters in `DefaultNavigator`
3. Mark legacy properties as deprecated

### Phase 3: Gradual Migration

1. Update consuming code to use new APIs
2. Monitor for issues
3. Eventually remove deprecated APIs

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../core/Navigator.kt` | Modify | Add tree-based properties, deprecate legacy |
| `quo-vadis-core/.../core/TreeNavigator.kt` | Create | New tree-based implementation |
| `quo-vadis-core/.../core/TransitionState.kt` | Create | Transition state sealed class |
| `quo-vadis-core/.../core/LegacyBackStackAdapter.kt` | Create | Backward compatibility adapter |

---

## Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| CORE-001 | Hard | NavNode hierarchy definition |
| CORE-002 | Hard | TreeMutator operations |

---

## Acceptance Criteria

- [ ] `Navigator.state` property exposes `StateFlow<NavNode>`
- [ ] `Navigator.transitionState` exposes `StateFlow<TransitionState>`
- [ ] `TreeNavigator` implementation fully functional
- [ ] All `TreeMutator` operations integrated (push, pop, switchTab, etc.)
- [ ] Legacy `backStack` and `entries` work via adapter
- [ ] Derived flows (`currentDestination`, `previousDestination`) work correctly
- [ ] Thread-safe state updates
- [ ] Transition state management for animations
- [ ] Predictive back support via `updateTransitionProgress()`
- [ ] Deep link handling preserved
- [ ] Graph registration preserved
- [ ] Comprehensive KDoc on all public APIs
- [ ] Unit tests for all navigation operations

---

## Testing Notes

See [CORE-006](./CORE-006-unit-tests.md) for comprehensive test requirements.

```kotlin
@Test
fun `navigate pushes to active stack`() {
    val navigator = TreeNavigator()
    navigator.setStartDestination(homeDestination)
    
    navigator.navigate(profileDestination)
    
    assertEquals(profileDestination, navigator.currentDestination.value)
    assertEquals(homeDestination, navigator.previousDestination.value)
}

@Test
fun `navigateBack pops from active stack`() {
    val navigator = TreeNavigator()
    navigator.setStartDestination(homeDestination)
    navigator.navigate(profileDestination)
    
    val result = navigator.navigateBack()
    
    assertTrue(result)
    assertEquals(homeDestination, navigator.currentDestination.value)
}

@Test
fun `switchTab changes active tab index`() {
    val navigator = TreeNavigator(initialState = createTabState())
    
    navigator.switchTab(1)
    
    val tabNode = navigator.state.value.findByKey("tabs") as TabNode
    assertEquals(1, tabNode.activeStackIndex)
}
```

---

## References

- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.1 "Phase 1: Core State Refactoring"
- [INDEX](../INDEX.md) - Phase 1 Overview
- [CORE-001](./CORE-001-navnode-hierarchy.md) - NavNode definitions
- [CORE-002](./CORE-002-tree-mutator.md) - TreeMutator operations
- [Current Navigator Implementation](../../../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt)
