# Back Handling Refactoring Plan

## Overview

This document outlines the plan to refactor back handling logic in the Quo Vadis navigation library. The current implementation has accumulated complexity from the old architecture and previous refactoring decisions, resulting in inconsistent and sometimes broken behavior.

## Current State Analysis

### Architecture Components

The navigation library uses a tree-based navigation state represented by `NavNode`:

| Node Type | Purpose | Current Back Behavior |
|-----------|---------|----------------------|
| `ScreenNode` | Leaf node representing a single destination | N/A (popped by parent) |
| `StackNode` | Linear navigation stack | Pop last child |
| `TabNode` | Parallel stacks with tab switching | Pop from active stack OR switch tabs |
| `PaneNode` | Adaptive pane layouts | Complex behavior based on `PaneBackBehavior` |

### Current Back Handling Interfaces

```kotlin
// Base interface - returns true if consumed
interface BackPressHandler {
    fun onBack(): Boolean
}

// Parent navigator with child delegation
interface ParentNavigator : BackPressHandler {
    val activeChild: BackPressHandler? 
    override fun onBack(): Boolean  // Delegates to child first, then handleBackInternal()
    fun handleBackInternal(): Boolean
}
```

### Identified Issues

#### Issue 1: Root Stack "Always One Child" Constraint Missing

**Expected Behavior:** Root stack should always have at least one child. If only one child remains, back should be delegated to the system (close app).

**Current Behavior:** `TreeMutator.pop()` with `PRESERVE_EMPTY` allows popping the last item, leaving an empty stack. The `canGoBack` check only looks at `StackNode.canGoBack` which returns `children.size > 1`, but this doesn't account for the root constraint.

**Location:** 
- [TreeMutator.kt#L370-L415](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt#L370-L415) - `pop()` function
- [TreeMutator.kt#L1145-L1157](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt#L1145-L1157) - `canGoBack()` function

#### Issue 2: Back Propagation Inconsistency

**Expected Behavior:** Back should be handled in current navnode scope first. If not handled, propagate upwards in tree.

**Current Behavior:** 
- `TreeNavigator.navigateBack()` calls `onBack()` which delegates to `handleBackInternal()`
- `handleBackInternal()` first tries `TreeMutator.pop()`, then falls back to `popWithPaneBehavior()`
- The `activeChild` delegation exists in `ParentNavigator` but is often not used correctly

**Inconsistencies:**
1. `TreeNavigator` uses the delegation pattern but doesn't always have the correct `activeChild` set
2. `TabScopedNavigator` has its own `navigateBack()` that bypasses the delegation pattern
3. `TabNavigatorState` implements its own `onBack()` but isn't integrated into the tree

**Locations:**
- [TreeNavigator.kt#L239-L279](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt#L239-L279)
- [TabScopedNavigator.kt#L283-L297](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabScopedNavigator.kt#L283-L297)
- [TabNavigatorState.kt#L169-L200](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorState.kt#L169-L200)

#### Issue 3: Tab Back Handling Not Integrated with TreeNavigator

**Expected Behavior:** 
1. Back should first pop from current tab's stack (if size > 1)
2. If tab stack is at root, switch to initial tab
3. If already on initial tab, delegate to parent (pop whole TabNode)

**Current Behavior:** 
- `TabNavigatorState.onBack()` implements correct logic but is a separate component
- `TreeNavigator` doesn't know about tab-specific back behavior
- The hierarchical renderer doesn't integrate `TabNavigatorState` back handling

**Location:** 
- [TabNavigatorState.kt#L169-L200](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorState.kt#L169-L200)
- [NavTreeRenderer.kt#L317-L446](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt#L317-L446) - TabRenderer doesn't handle back

#### Issue 4: Pane Back Handling Complexity

**Expected Behavior:**
- Small screens (single pane): Follow stack back handling rules
- Large screens (multi-pane): Handle back within pane scope first

**Current Behavior:**
- `PaneBackBehavior` enum exists with 4 strategies
- `TreeMutator.popWithPaneBehavior()` implements complex logic
- No integration with screen size awareness

**Location:**
- [NavNode.kt#L247-L263](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavNode.kt#L247-L263) - `PaneBackBehavior` enum
- [TreeMutator.kt#L717-L799](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt#L717-L799) - `popWithPaneBehavior()`

#### Issue 5: No User-Defined Back Handler Support

**Expected Behavior:** Users should be able to provide custom back handlers per screen with a composable wrapper like:
```kotlin
@Composable
fun NavBackHandler(
    onBack: () -> Boolean,  // Return true if handled
    content: @Composable () -> Unit
)
```

**Current Behavior:** No such API exists. Back handling is entirely managed by the navigation system.

---

## Target Behavior Specification

### 1. Screen Node Back Handling
When back is pressed on a screen:
1. **Check user-defined handler first** - If screen has `NavBackHandler`, invoke it
2. **If not handled** - Pop the screen from its parent stack

### 2. Stack Node Back Handling
When back is pressed on a stack:
1. **If stack size > 1** - Pop the last screen
2. **If stack size == 1 AND is root** - Delegate to system (close app)
3. **If stack size == 1 AND not root** - Pop the entire stack from parent

### 3. Tab Node Back Handling
When back is pressed on tabs:
1. **If active tab's stack size > 1** - Pop from tab stack
2. **If active tab's stack at root AND not initial tab** - Switch to initial tab
3. **If on initial tab at root** - Delegate to parent

### 4. Pane Node Back Handling
Back behavior depends on display mode:

**Single Pane Mode (compact screens):**
1. Follow stack back handling rules
2. Behave as if only one pane exists

**Multi Pane Mode (expanded screens):**
1. Apply `PaneBackBehavior` strategy:
   - `PopLatest` - Simple pop from active pane
   - `PopUntilScaffoldValueChange` - Pop until pane visibility changes
   - `PopUntilCurrentDestinationChange` - Pop until active pane changes
   - `PopUntilContentChange` - Pop from any pane with content

### 5. User-Defined Back Handler
```kotlin
@Composable
fun NavBackHandler(
    enabled: Boolean = true,
    onBack: () -> Boolean,  // Return true if handled
    content: @Composable () -> Unit
)
```

Priority order:
1. User-defined `NavBackHandler` (innermost first)
2. Node-specific back logic (Stack → Tab → Pane)
3. Parent delegation
4. System back (close app)

---

## Implementation Plan

### Phase 1: Core Back Handler Infrastructure

#### Task 1.1: Create NavBackHandler Composable
Create a composable that allows users to intercept back events.

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavBackHandler.kt`

```kotlin
/**
 * A composable that intercepts back press events for custom handling.
 * 
 * When back is pressed, [onBack] is invoked. If it returns true, the back
 * event is consumed. If false, the event propagates to the navigation system.
 *
 * Multiple NavBackHandler composables can be nested. The innermost one
 * gets first chance to handle the event.
 *
 * @param enabled Whether this handler is active
 * @param onBack Callback invoked on back press. Return true to consume.
 * @param content The content to wrap
 */
@Composable
fun NavBackHandler(
    enabled: Boolean = true,
    onBack: () -> Boolean,
    content: @Composable () -> Unit
)
```

**Implementation Details:**
- Use `CompositionLocal` to register handlers in a stack
- Each `NavBackHandler` pushes its callback on composition
- Pops callback on disposal
- The navigation system queries this stack before processing back

**Effort:** 2-3 hours

#### Task 1.2: Create BackHandlerRegistry
A registry that maintains the stack of user-defined back handlers.

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/BackHandlerRegistry.kt`

```kotlin
/**
 * Registry for user-defined back handlers.
 * Maintains a stack of handlers, invoking them in LIFO order.
 */
class BackHandlerRegistry {
    private val handlers = mutableListOf<() -> Boolean>()
    
    fun register(handler: () -> Boolean): () -> Unit
    fun unregister(handler: () -> Boolean)
    fun handleBack(): Boolean // Returns true if any handler consumed
}

val LocalBackHandlerRegistry = staticCompositionLocalOf { BackHandlerRegistry() }
```

**Effort:** 1 hour

### Phase 2: Tree-Based Back Handling Refactor

#### Task 2.1: Add Back Handling to NavNode
Add a protocol for node-specific back handling.

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/NavNode.kt`

Add extension functions for back handling logic:

```kotlin
/**
 * Determines if this node can handle back internally.
 * 
 * @return true if back can be handled without popping this node
 */
fun NavNode.canHandleBackInternally(): Boolean

/**
 * Handle back press for this node.
 * 
 * @return New node state after handling back, or null if should propagate
 */
fun NavNode.handleBackInNode(): NavNode?
```

**Effort:** 2 hours

#### Task 2.2: Refactor TreeMutator.pop()
Update pop logic to respect the root constraint and node-specific behavior.

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt`

**Changes:**
1. Add `isRoot` parameter or detection
2. Return `null` when root stack would become empty
3. Add `popWithTabBehavior()` similar to `popWithPaneBehavior()`

```kotlin
/**
 * Pop with respect to root constraints.
 * 
 * Ensures root stack maintains at least one child.
 * Returns null if pop would violate this constraint (system back needed).
 */
fun popRespectingRoot(root: NavNode): NavNode? {
    val activeStack = root.activeStack() ?: return null
    
    // If this IS the root and only has one child, cannot pop
    if (activeStack.parentKey == null && activeStack.children.size <= 1) {
        return null
    }
    
    return pop(root)
}
```

**Effort:** 3 hours

#### Task 2.3: Add Tab-Aware Back Handling to TreeMutator
Create a unified pop function that handles tabs correctly.

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt`

```kotlin
/**
 * Pop with intelligent tab handling.
 * 
 * When the active stack is inside a TabNode:
 * 1. If tab stack has > 1 child, pop from tab
 * 2. If tab stack at root and not initial tab, switch to initial tab
 * 3. If on initial tab at root, propagate to parent
 */
fun popWithTabBehavior(root: NavNode): PopResult
```

**Effort:** 3 hours

#### Task 2.4: Unify Back Handling in TreeNavigator
Consolidate back handling into a single, well-defined flow.

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt`

**Changes:**
1. Integrate `BackHandlerRegistry` check
2. Use unified pop logic
3. Proper root constraint enforcement

```kotlin
override fun handleBackInternal(): Boolean {
    // 1. Check user-defined handlers (via registry)
    if (backHandlerRegistry.handleBack()) {
        return true
    }
    
    // 2. Try tree-aware pop (handles tabs, panes, root constraint)
    val currentState = _state.value
    val newState = TreeMutator.popTreeAware(currentState)
    
    return if (newState != null) {
        updateStateWithTransition(newState, null)
        true
    } else {
        false // Delegate to system (close app)
    }
}
```

**Effort:** 2 hours

### Phase 3: Renderer Integration

#### Task 3.1: Integrate BackHandlerRegistry in Hosts
Provide the registry via CompositionLocal in navigation hosts.

**Files:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/HierarchicalQuoVadisHost.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/GraphNavHost.kt`

**Changes:**
```kotlin
@Composable
fun NavigationHost(...) {
    val backHandlerRegistry = remember { BackHandlerRegistry() }
    
    CompositionLocalProvider(
        LocalBackHandlerRegistry provides backHandlerRegistry
    ) {
        // Existing content...
    }
}
```

**Effort:** 1 hour

#### Task 3.2: Connect Back Handling with Predictive Back
Ensure user-defined handlers work with predictive back gestures.

**File:** Various predictive back files

**Considerations:**
- User handlers should be consulted at gesture start
- If user handler returns true, it should handle the gesture (or allow default)
- Need to design how `NavBackHandler` interacts with gesture progress

**Effort:** 4 hours

### Phase 4: Pane Adaptive Back Handling

#### Task 4.1: Add WindowSizeClass-Aware Back Handling
Create logic that adapts back behavior based on screen size.

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt`

**Changes:**
- Pass `WindowSizeClass` or equivalent to pane renderer
- Adjust back behavior based on whether multi-pane or single-pane mode

**Effort:** 3 hours

### Phase 5: Deprecation and Cleanup

#### Task 5.1: Deprecate TabNavigatorState.onBack()
The legacy back handling in `TabNavigatorState` should be deprecated in favor of the unified tree-based approach.

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorState.kt`

```kotlin
@Deprecated(
    "Use Navigator.navigateBack() which handles tab-aware back navigation",
    ReplaceWith("navigator.navigateBack()")
)
override fun onBack(): Boolean { ... }
```

**Effort:** 30 minutes

#### Task 5.2: Deprecate TabScopedNavigator Back Methods
Similar deprecation for the scoped navigator.

**Effort:** 30 minutes

### Phase 6: Testing

#### Task 6.1: Unit Tests for NavBackHandler
Test scenarios:
- Single handler, returns true
- Single handler, returns false
- Nested handlers, inner consumes
- Nested handlers, outer consumes
- Disabled handler

**Effort:** 2 hours

#### Task 6.2: Unit Tests for Tree-Aware Pop
Test scenarios:
- Stack pop with > 1 children
- Stack pop at root (should fail)
- Tab pop with > 1 in tab stack
- Tab switch to initial
- Tab propagation from initial
- Pane behaviors

**Effort:** 3 hours

#### Task 6.3: Integration Tests
Test complete back flows through the navigation system.

**Effort:** 3 hours

---

## File Changes Summary

### New Files
| File | Purpose |
|------|---------|
| `NavBackHandler.kt` | User-defined back handler composable |
| `BackHandlerRegistry.kt` | Registry for managing back handler stack |

### Modified Files
| File | Changes |
|------|---------|
| `NavNode.kt` | Add back handling extension functions |
| `TreeMutator.kt` | Add `popTreeAware()`, `popWithTabBehavior()`, `popRespectingRoot()` |
| `TreeNavigator.kt` | Integrate unified back handling |
| `HierarchicalQuoVadisHost.kt` | Provide `BackHandlerRegistry` |
| `GraphNavHost.kt` | Provide `BackHandlerRegistry` |
| `TabNavigatorState.kt` | Deprecate `onBack()` |
| `TabScopedNavigator.kt` | Deprecate direct back handling |

---

## Implementation Order

```
Phase 1: Core Infrastructure (4-5 hours)
├── Task 1.1: NavBackHandler composable
└── Task 1.2: BackHandlerRegistry

Phase 2: Tree-Based Back Handling (10 hours)
├── Task 2.1: NavNode back handling extensions
├── Task 2.2: Refactor TreeMutator.pop()
├── Task 2.3: Tab-aware back handling
└── Task 2.4: Unify TreeNavigator back handling

Phase 3: Renderer Integration (5 hours)
├── Task 3.1: Integrate registry in hosts
└── Task 3.2: Predictive back integration

Phase 4: Pane Adaptive (3 hours)
└── Task 4.1: WindowSizeClass-aware back handling

Phase 5: Deprecation (1 hour)
├── Task 5.1: Deprecate TabNavigatorState.onBack()
└── Task 5.2: Deprecate TabScopedNavigator methods

Phase 6: Testing (8 hours)
├── Task 6.1: NavBackHandler tests
├── Task 6.2: Tree-aware pop tests
└── Task 6.3: Integration tests

Total Estimated Effort: 31-33 hours
```

---

## Risk Assessment

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking existing navigation flows | High | Extensive testing, feature flags for rollback |
| Predictive back gesture conflicts | Medium | Careful integration, separate testing |
| Performance impact from handler registry | Low | Efficient stack-based implementation |
| Complex pane scenarios not covered | Medium | Document edge cases, add escape hatches |

---

## Success Criteria

1. **Root constraint enforced:** Root stack never becomes empty; last item triggers system back
2. **Tab back behavior correct:** Pop → Switch to initial → Delegate to parent
3. **Pane adaptive behavior:** Single pane = stack behavior; Multi pane = pane behavior
4. **User handlers work:** `NavBackHandler` can intercept and handle back events
5. **Predictive back integrated:** User handlers work with gesture-based back
6. **All existing tests pass:** No regressions
7. **New tests cover all scenarios:** Unit and integration tests for new functionality

---

## API Examples

### Basic NavBackHandler Usage
```kotlin
@Screen(SomeDestination::class)
@Composable
fun SomeScreen(navigator: Navigator) {
    var showDialog by remember { mutableStateOf(false) }
    
    NavBackHandler(
        enabled = showDialog,
        onBack = { 
            showDialog = false
            true // Consumed - don't navigate back
        }
    ) {
        Column {
            Button(onClick = { showDialog = true }) {
                Text("Show Dialog")
            }
            
            if (showDialog) {
                AlertDialog(...)
            }
        }
    }
}
```

### Nested NavBackHandlers
```kotlin
@Composable
fun ParentContent() {
    NavBackHandler(onBack = { 
        println("Parent handler")
        false // Not consumed, propagate
    }) {
        ChildContent()
    }
}

@Composable
fun ChildContent() {
    NavBackHandler(onBack = { 
        println("Child handler") 
        true // Consumed
    }) {
        // Content
    }
}
// Back press prints "Child handler" only
```

### Conditional Back Handling
```kotlin
@Composable
fun FormScreen(navigator: Navigator) {
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    NavBackHandler(
        enabled = hasUnsavedChanges,
        onBack = {
            showConfirmDialog = true
            true // Block navigation until confirmed
        }
    ) {
        // Form content...
        
        if (showConfirmDialog) {
            ConfirmDialog(
                onConfirm = { 
                    hasUnsavedChanges = false
                    navigator.navigateBack()
                },
                onDismiss = { showConfirmDialog = false }
            )
        }
    }
}
```

---

## References

- [Architecture Patterns Memory](/.serena/memory/architecture_patterns) - Overall architecture documentation
- [Compose BackHandler](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#BackHandler(kotlin.Boolean,kotlin.Function0)) - Android's BackHandler for reference
