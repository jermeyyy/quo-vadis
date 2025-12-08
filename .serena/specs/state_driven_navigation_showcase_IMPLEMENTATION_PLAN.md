# State-Driven Navigation Architecture Showcase - Implementation Plan

> **Created:** November 30, 2025  
> **Status:** 📋 Planning Complete - Ready for Implementation  
> **Objective:** Showcase advanced navigation pattern where user manually mutates backstack, navigator observes and renders accordingly

---

## Executive Summary

This plan outlines the implementation of a **state-driven navigation showcase** for quo-vadis, inspired by Navigation 3's architecture where the developer owns and directly mutates a `SnapshotStateList` backstack, and the navigation UI observes and renders accordingly.

### Key Deliverables

1. **Extended BackStack API** - New `StateBackStack` exposing `SnapshotStateList<BackStackEntry>` for direct mutable access
2. **New NavHost Variant** - `StateNavHost` that observes mutable state list and renders accordingly
3. **Demo Application** - Interactive showcase demonstrating state-driven navigation patterns
4. **Documentation** - Architecture guide comparing approaches and demonstrating patterns

### Estimated Effort

| Phase | Description | Estimated Time |
|-------|-------------|----------------|
| Phase 1 | Core API Extension | 2-3 days |
| Phase 2 | Compose Integration | 2-3 days |
| Phase 3 | Demo Application | 3-4 days |
| Phase 4 | Documentation & Testing | 2-3 days |
| **Total** | | **9-13 days** |

---

## 1. Background & Motivation

### 1.1 Navigation 3 Approach

Google's Navigation 3 introduces a paradigm shift:

```kotlin
// Developer owns the backstack directly
val backStack = rememberNavBackStack(Home) // SnapshotStateList<NavKey>

// Direct mutations trigger recomposition
backStack.add(Profile("123"))
backStack.removeLastOrNull()
backStack.removeAt(2)
backStack[1] = EditProfile("456")

// NavDisplay observes and renders
NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = { key -> NavEntry(key) { Screen(key) } }
)
```

**Key Benefits:**
- Maximum flexibility for complex navigation patterns
- Full control over stack structure
- Predictable state-driven behavior
- Easy testing (just assert list contents)

### 1.2 Current Quo Vadis Approach

Quo Vadis wraps backstack operations in semantic methods:

```kotlin
// Operations through Navigator/BackStack
navigator.navigate(destination)
navigator.navigateBack()
backStack.push(destination)
backStack.pop()
backStack.popUntil { it.route == "home" }

// State observation via StateFlow
val current by backStack.current.collectAsState()
val stack by backStack.stack.collectAsState()
```

**Current Limitations for State-Driven Patterns:**
- ❌ No direct mutable list access
- ❌ Cannot remove arbitrary entries by index
- ❌ Cannot reorder entries
- ❌ Cannot insert at specific positions
- ❌ No swap operations

### 1.3 Goal

Extend quo-vadis to support **both** approaches:
1. **Existing API** - Semantic methods (navigate, navigateBack, etc.) for typical use cases
2. **New State-Driven API** - Direct mutable list access for advanced use cases

---

## 2. Architecture Design

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              State-Driven Demo Screen                │   │
│  │  ┌───────────────────┐  ┌────────────────────────┐  │   │
│  │  │  Backstack Panel  │  │   Navigation Content   │  │   │
│  │  │  (Interactive)    │  │   (Observed/Rendered)  │  │   │
│  │  │                   │  │                        │  │   │
│  │  │  [Home] ✕         │  │   ┌──────────────┐    │  │   │
│  │  │  [Profile] ✕      │  │   │ Current Dest │    │  │   │
│  │  │  [Settings] ✕     │  │   │   Content    │    │  │   │
│  │  │                   │  │   └──────────────┘    │  │   │
│  │  │  [+ Add] [↕ Move] │  │                        │  │   │
│  │  └───────────────────┘  └────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Compose Layer                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                  StateNavHost                        │   │
│  │    Observes: SnapshotStateList<BackStackEntry>      │   │
│  │    Renders:  Current destination from list          │   │
│  │    Handles:  Transitions, animations                │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Core Layer                              │
│  ┌─────────────────────┐  ┌────────────────────────────┐   │
│  │   StateBackStack    │  │      StateNavigator        │   │
│  │                     │  │                            │   │
│  │ entries:            │  │ stateBackStack:            │   │
│  │  SnapshotStateList  │◄─┤   StateBackStack          │   │
│  │  <BackStackEntry>   │  │                            │   │
│  │                     │  │ Provides convenience       │   │
│  │ Direct mutations:   │  │ methods + observes state   │   │
│  │ - add/remove        │  │                            │   │
│  │ - removeAt          │  │ currentDestination:        │   │
│  │ - swap/move         │  │   derivedStateOf { ... }   │   │
│  │ - clear/replaceAll  │  │                            │   │
│  └─────────────────────┘  └────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Core Components

#### 2.2.1 StateBackStack

New class exposing mutable `SnapshotStateList`:

```kotlin
/**
 * State-driven backstack implementation using Compose's SnapshotStateList.
 * 
 * Provides direct mutable access to the navigation stack for advanced use cases
 * where the developer needs full control over stack structure.
 * 
 * This approach mirrors Navigation 3's architecture where the developer owns
 * the backstack state and navigation UI observes changes.
 */
class StateBackStack(initialEntries: List<BackStackEntry> = emptyList()) {
    
    /**
     * The mutable list of backstack entries.
     * Direct mutations to this list will trigger UI recomposition in observing composables.
     */
    val entries: SnapshotStateList<BackStackEntry> = mutableStateListOf(*initialEntries.toTypedArray())
    
    /**
     * Current (top) entry, computed from entries list.
     * Uses derivedStateOf for optimal recomposition.
     */
    val current: BackStackEntry? by derivedStateOf { entries.lastOrNull() }
    
    /**
     * Previous entry (for animations).
     */
    val previous: BackStackEntry? by derivedStateOf { 
        if (entries.size > 1) entries[entries.lastIndex - 1] else null 
    }
    
    /**
     * Whether back navigation is possible.
     */
    val canGoBack: Boolean by derivedStateOf { entries.size > 1 }
    
    /**
     * Immutable snapshot of the stack for Flow consumers.
     */
    val entriesFlow: Flow<List<BackStackEntry>> = snapshotFlow { entries.toList() }
    
    // Convenience methods (delegate to list operations)
    fun push(destination: Destination, transition: NavigationTransition? = null) {
        entries.add(BackStackEntry.create(destination, transition))
    }
    
    fun pop(): BackStackEntry? = entries.removeLastOrNull()
    
    fun removeAt(index: Int): BackStackEntry = entries.removeAt(index)
    
    fun removeById(id: String): Boolean {
        val index = entries.indexOfFirst { it.id == id }
        return if (index >= 0) { entries.removeAt(index); true } else false
    }
    
    fun insert(index: Int, destination: Destination, transition: NavigationTransition? = null) {
        entries.add(index, BackStackEntry.create(destination, transition))
    }
    
    fun swap(fromIndex: Int, toIndex: Int) {
        if (fromIndex in entries.indices && toIndex in entries.indices) {
            val temp = entries[fromIndex]
            entries[fromIndex] = entries[toIndex]
            entries[toIndex] = temp
        }
    }
    
    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex in entries.indices && toIndex in 0..entries.size) {
            val entry = entries.removeAt(fromIndex)
            entries.add(minOf(toIndex, entries.size), entry)
        }
    }
    
    fun clear() = entries.clear()
    
    fun replaceAll(destinations: List<Destination>) {
        entries.clear()
        entries.addAll(destinations.map { BackStackEntry.create(it) })
    }
}
```

#### 2.2.2 StateNavigator

Optional wrapper providing semantic navigation methods:

```kotlin
/**
 * State-driven navigator that wraps StateBackStack with convenience methods.
 * 
 * For direct stack manipulation, use stateBackStack.entries directly.
 * For semantic navigation, use the convenience methods.
 */
class StateNavigator(
    val stateBackStack: StateBackStack = StateBackStack()
) {
    val currentDestination: Destination? by derivedStateOf {
        stateBackStack.current?.destination
    }
    
    val previousDestination: Destination? by derivedStateOf {
        stateBackStack.previous?.destination
    }
    
    val canGoBack: Boolean by derivedStateOf { stateBackStack.canGoBack }
    
    // Convenience methods
    fun navigate(destination: Destination, transition: NavigationTransition? = null) {
        stateBackStack.push(destination, transition)
    }
    
    fun navigateBack(): Boolean {
        return stateBackStack.pop() != null
    }
    
    fun navigateAndReplace(destination: Destination, transition: NavigationTransition? = null) {
        stateBackStack.pop()
        stateBackStack.push(destination, transition)
    }
    
    fun navigateAndClearAll(destination: Destination) {
        stateBackStack.clear()
        stateBackStack.push(destination)
    }
    
    // Direct stack access for advanced operations
    val entries: SnapshotStateList<BackStackEntry> get() = stateBackStack.entries
}
```

#### 2.2.3 StateNavHost

New composable that observes `StateBackStack` and renders accordingly:

```kotlin
/**
 * State-driven navigation host that observes a StateBackStack and renders content.
 * 
 * Similar to Navigation 3's NavDisplay, this composable renders the current
 * destination based on the entries in the provided StateBackStack.
 *
 * @param stateBackStack The mutable backstack to observe
 * @param entryProvider Lambda that maps destinations to composable content
 * @param modifier Modifier for the container
 * @param transitionSpec Animation spec for transitions
 */
@Composable
fun StateNavHost(
    stateBackStack: StateBackStack,
    entryProvider: @Composable (Destination) -> Unit,
    modifier: Modifier = Modifier,
    transitionSpec: ContentTransform = fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
) {
    val currentEntry = stateBackStack.current
    
    AnimatedContent(
        targetState = currentEntry,
        modifier = modifier,
        transitionSpec = { transitionSpec },
        contentKey = { it?.id }
    ) { entry ->
        if (entry != null) {
            entryProvider(entry.destination)
        }
    }
}
```

### 2.3 Package Structure

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/
├── core/
│   ├── BackStack.kt           # Existing (unchanged)
│   ├── StateBackStack.kt      # NEW: SnapshotStateList-based backstack
│   ├── Navigator.kt           # Existing (unchanged)
│   └── StateNavigator.kt      # NEW: State-driven navigator
├── compose/
│   ├── NavHost.kt             # Existing (unchanged)
│   ├── GraphNavHost.kt        # Existing (unchanged)
│   └── StateNavHost.kt        # NEW: State-driven nav host
└── testing/
    ├── FakeNavigator.kt       # Existing (unchanged)
    └── FakeStateNavigator.kt  # NEW: Testing utility

composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/
├── ui/screens/
│   └── statedriven/           # NEW: Demo screens
│       ├── StateDrivenDemoScreen.kt
│       ├── BackstackEditorPanel.kt
│       ├── BackstackEntryItem.kt
│       └── DestinationPicker.kt
├── destinations/
│   └── StateDrivenDestinations.kt  # NEW: Demo destinations
└── graphs/
    └── StateDrivenGraph.kt    # NEW: Demo graph registration
```

---

## 3. Phase 1: Core API Extension

### 3.1 Objectives

- [ ] Create `StateBackStack` class with `SnapshotStateList` exposure
- [ ] Create `StateNavigator` wrapper class
- [ ] Ensure backward compatibility (no changes to existing API)
- [ ] Add comprehensive KDoc documentation

### 3.2 Files to Create

| File | Lines (Est.) | Description |
|------|--------------|-------------|
| `StateBackStack.kt` | ~150 | SnapshotStateList-based backstack |
| `StateNavigator.kt` | ~100 | State-driven navigator wrapper |
| `FakeStateNavigator.kt` | ~80 | Testing utilities |

### 3.3 Implementation Details

#### 3.3.1 StateBackStack.kt

Location: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/StateBackStack.kt`

**Key Implementation Points:**

1. **Use `mutableStateListOf()`** for native Compose observation
2. **Use `derivedStateOf`** for computed properties (current, previous, canGoBack) to minimize recomposition
3. **Provide `snapshotFlow`** for non-Compose consumers
4. **Thread safety**: Document that mutations should occur on main thread or within `Snapshot.withMutableSnapshot`
5. **Entry IDs**: Use existing `BackStackEntry.create()` for consistent ID generation

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * State-driven backstack using Compose's SnapshotStateList for direct mutable access.
 *
 * This implementation provides a Navigation 3-style API where developers have full
 * control over the backstack as a mutable list. Changes to the list automatically
 * trigger recomposition in observing composables.
 *
 * ## Usage
 * ```kotlin
 * val backStack = rememberStateBackStack(HomeDestination)
 *
 * // Direct list manipulation
 * backStack.entries.add(BackStackEntry.create(ProfileDestination))
 * backStack.entries.removeAt(1)
 * backStack.entries.swap(0, 1)
 *
 * // Convenience methods
 * backStack.push(SettingsDestination)
 * backStack.pop()
 * ```
 *
 * ## Thread Safety
 * Mutations should occur on the main thread or within `Snapshot.withMutableSnapshot`
 * for background thread safety.
 *
 * @param initialEntries Optional initial entries for the backstack
 */
class StateBackStack(initialEntries: List<BackStackEntry> = emptyList()) {
    // ... implementation as designed above
}
```

#### 3.3.2 StateNavigator.kt

Location: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/StateNavigator.kt`

**Key Implementation Points:**

1. **Composition over inheritance** - wraps StateBackStack rather than extending
2. **Exposes both** semantic methods (navigate, navigateBack) AND direct list access
3. **Uses `derivedStateOf`** for computed properties
4. **Optionally accepts graphs** for destination resolution (deep links, etc.)

#### 3.3.3 FakeStateNavigator.kt

Location: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeStateNavigator.kt`

**Key Implementation Points:**

1. **Records all operations** for verification in tests
2. **Provides assertion helpers** (`verifyNavigatedTo`, `verifyStackSize`, etc.)
3. **Mirrors StateNavigator API** for drop-in test replacement

### 3.4 Acceptance Criteria

- [ ] `StateBackStack.entries` is a `SnapshotStateList<BackStackEntry>`
- [ ] Direct mutations trigger observation updates
- [ ] `derivedStateOf` properties update correctly
- [ ] `snapshotFlow` emits on changes
- [ ] Existing `BackStack`/`Navigator` unchanged
- [ ] All KDoc documentation complete
- [ ] Unit tests pass

---

## 4. Phase 2: Compose Integration

### 4.1 Objectives

- [ ] Create `StateNavHost` composable
- [ ] Create `rememberStateBackStack` helper
- [ ] Create `rememberStateNavigator` helper
- [ ] Integrate with existing transition animations
- [ ] Support predictive back gestures

### 4.2 Files to Create

| File | Lines (Est.) | Description |
|------|--------------|-------------|
| `StateNavHost.kt` | ~200 | State-driven navigation host |
| `StateNavHostHelpers.kt` | ~100 | Remember functions, utilities |

### 4.3 Implementation Details

#### 4.3.1 StateNavHost.kt

Location: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/StateNavHost.kt`

**Key Implementation Points:**

1. **Observe `stateBackStack.current`** directly (it's already a derived state)
2. **Use `AnimatedContent`** for transitions
3. **Content key by entry ID** for proper animation targeting
4. **Support custom transition specs**
5. **Integrate predictive back** (optional)

```kotlin
@Composable
fun StateNavHost(
    stateBackStack: StateBackStack,
    modifier: Modifier = Modifier,
    transitionSpec: AnimatedContentTransitionScope<BackStackEntry?>.() -> ContentTransform = {
        // Default: slide + fade
        slideInHorizontally { width -> width } + fadeIn() togetherWith
        slideOutHorizontally { width -> -width } + fadeOut()
    },
    entryProvider: @Composable AnimatedContentScope.(Destination) -> Unit
) {
    // Implementation
}
```

#### 4.3.2 rememberStateBackStack

```kotlin
/**
 * Remember a StateBackStack with optional initial destination.
 *
 * @param initialDestination The start destination
 * @return A remembered StateBackStack instance
 */
@Composable
fun rememberStateBackStack(
    initialDestination: Destination? = null
): StateBackStack {
    return remember {
        StateBackStack(
            initialEntries = if (initialDestination != null) {
                listOf(BackStackEntry.create(initialDestination))
            } else {
                emptyList()
            }
        )
    }
}

/**
 * Remember a StateBackStack with saveable state across configuration changes.
 *
 * @param initialDestination The start destination
 * @param destinationSerializer Serializer for destination persistence
 * @return A saveable StateBackStack instance
 */
@Composable
fun rememberSaveableStateBackStack(
    initialDestination: Destination,
    destinationSerializer: (Destination) -> String,
    destinationDeserializer: (String) -> Destination
): StateBackStack {
    // Implementation using rememberSaveable
}
```

### 4.4 Predictive Back Integration

The state-driven pattern needs special handling for predictive back:

```kotlin
@Composable
fun StateNavHost(
    stateBackStack: StateBackStack,
    enablePredictiveBack: Boolean = true,
    onBack: () -> Unit = { stateBackStack.pop() },
    // ...
) {
    if (enablePredictiveBack && stateBackStack.canGoBack) {
        PredictiveBackHandler(enabled = true) { progress ->
            // Handle gesture progress
        }
    }
    
    // AnimatedContent for rendering
}
```

### 4.5 Acceptance Criteria

- [ ] `StateNavHost` renders current destination from StateBackStack
- [ ] Direct `entries` mutations trigger UI updates
- [ ] Transitions animate correctly
- [ ] Predictive back gestures work
- [ ] `rememberStateBackStack` preserves state across recomposition
- [ ] `rememberSaveableStateBackStack` survives configuration changes

---

## 5. Phase 3: Demo Application

### 5.1 Objectives

- [ ] Create interactive backstack manipulation UI
- [ ] Demonstrate state-driven patterns
- [ ] Compare with existing quo-vadis approach
- [ ] Showcase advanced operations (reorder, remove arbitrary, insert)

### 5.2 Demo Screen Design

```
┌─────────────────────────────────────────────────────────────────┐
│  State-Driven Navigation Demo                              [×] │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─── Backstack ───────────────────┐  ┌─── Content ──────────┐ │
│  │                                 │  │                      │ │
│  │  Current Stack (drag to reorder)│  │                      │ │
│  │  ┌───────────────────────────┐  │  │   ┌──────────────┐   │ │
│  │  │ 3. Settings         [×]  │  │  │   │              │   │ │
│  │  └───────────────────────────┘  │  │   │  Settings    │   │ │
│  │  ┌───────────────────────────┐  │  │   │   Screen     │   │ │
│  │  │ 2. Profile (id=123) [×]  │  │  │   │              │   │ │
│  │  └───────────────────────────┘  │  │   │  Content     │   │ │
│  │  ┌───────────────────────────┐  │  │   │  renders     │   │ │
│  │  │ 1. Home             [×]  │  │  │   │  here        │   │ │
│  │  └───────────────────────────┘  │  │   │              │   │ │
│  │                                 │  │   └──────────────┘   │ │
│  │  ─────────────────────────────  │  │                      │ │
│  │                                 │  │                      │ │
│  │  Quick Actions:                 │  │                      │ │
│  │  [+ Add] [Pop] [Clear] [Reset]  │  │                      │ │
│  │                                 │  │                      │ │
│  │  Add Destination:               │  │                      │ │
│  │  ○ Home  ○ Profile  ○ Settings  │  │                      │ │
│  │  ○ Detail(id)  ○ Custom         │  │                      │ │
│  │                                 │  │                      │ │
│  └─────────────────────────────────┘  └──────────────────────┘ │
│                                                                 │
│  ┌─── State Info ──────────────────────────────────────────┐   │
│  │  Stack size: 3 | Can go back: true | Current: Settings  │   │
│  │  Last operation: push(Settings)                         │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
│  [View Code Examples]  [Compare with Traditional API]          │
└─────────────────────────────────────────────────────────────────┘
```

### 5.3 Files to Create

| File | Lines (Est.) | Description |
|------|--------------|-------------|
| `StateDrivenDestinations.kt` | ~50 | Demo destination definitions |
| `StateDrivenGraph.kt` | ~30 | Navigation graph registration |
| `StateDrivenDemoScreen.kt` | ~250 | Main demo screen |
| `BackstackEditorPanel.kt` | ~200 | Interactive backstack editor |
| `BackstackEntryItem.kt` | ~100 | Draggable entry item |
| `DestinationPicker.kt` | ~80 | Destination selection UI |
| `CodeExamplesSheet.kt` | ~150 | Code examples bottom sheet |

**Total:** ~860 lines

### 5.4 Demo Destinations

```kotlin
@Graph("state_driven_demo")
sealed class StateDrivenDestination : Destination {
    
    @Route("state_demo/home")
    data object Home : StateDrivenDestination()
    
    @Route("state_demo/profile")
    @Argument(ProfileData::class)
    data class Profile(val userId: String) : StateDrivenDestination(),
        TypedDestination<ProfileData> {
        override val data = ProfileData(userId)
    }
    
    @Route("state_demo/settings")
    data object Settings : StateDrivenDestination()
    
    @Route("state_demo/detail")
    @Argument(DetailData::class)
    data class Detail(val itemId: String) : StateDrivenDestination(),
        TypedDestination<DetailData> {
        override val data = DetailData(itemId)
    }
    
    @Route("state_demo/custom")
    @Argument(CustomData::class)
    data class Custom(val title: String, val content: String) : StateDrivenDestination(),
        TypedDestination<CustomData> {
        override val data = CustomData(title, content)
    }
}
```

### 5.5 Interactive Features

1. **Drag-to-Reorder** - Use `LazyColumn` with drag handles to reorder entries
2. **Remove Entry** - Swipe-to-dismiss or X button to remove arbitrary entries
3. **Insert Entry** - Long-press between entries to insert new destination
4. **Quick Actions** - Push, Pop, Clear, Reset buttons
5. **State Display** - Real-time display of stack state, size, current, canGoBack
6. **Operation Log** - Show last N operations performed
7. **Code Examples** - Show corresponding code for each operation

### 5.6 Acceptance Criteria

- [ ] Demo accessible from main app menu
- [ ] All backstack operations demonstrable interactively
- [ ] Drag-to-reorder works smoothly
- [ ] Real-time state display updates correctly
- [ ] Code examples are accurate and runnable
- [ ] Compare view shows both API styles

---

## 6. Phase 4: Documentation & Testing

### 6.1 Documentation

#### 6.1.1 API Reference Updates

Update `quo-vadis-core/docs/API_REFERENCE.md`:

- Add `StateBackStack` section
- Add `StateNavigator` section
- Add `StateNavHost` section
- Add `remember*` helpers section

#### 6.1.2 Architecture Guide

Create `quo-vadis-core/docs/STATE_DRIVEN_NAVIGATION.md`:

n
# State-Driven Navigation in Quo Vadis

## Overview
Quo Vadis supports two navigation paradigms:
1. **Semantic Navigation** - Traditional Navigator API with semantic methods
2. **State-Driven Navigation** - Direct backstack manipulation (Nav3-style)

## When to Use State-Driven Navigation
- Complex stack manipulation requirements
- Need for arbitrary entry removal/insertion
- Custom navigation patterns
- Testing scenarios requiring direct state control

## Comparison with Navigation 3
[Table comparing approaches]

## Migration Guide
[How to switch between approaches]

## Best Practices
[Guidelines for state-driven navigation]
```

#### 6.1.3 Demo README

Update `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/README.md`:

- Add state-driven demo section
- Document available operations
- Add code examples

### 6.2 Testing

#### 6.2.1 Unit Tests

| Test File | Coverage |
|-----------|----------|
| `StateBackStackTest.kt` | All operations, derived state, flows |
| `StateNavigatorTest.kt` | Convenience methods, state delegation |
| `StateNavHostTest.kt` | Rendering, transitions |

#### 6.2.2 Test Cases

**StateBackStack Tests:**
- `entries_initiallyEmpty_whenNoInitialEntries`
- `entries_containsInitialEntry_whenProvided`
- `push_addsEntryToEnd`
- `pop_removesLastEntry`
- `pop_returnsRemovedEntry`
- `pop_returnsNull_whenEmpty`
- `removeAt_removesCorrectEntry`
- `removeAt_updatesCurrentCorrectly`
- `insert_addsAtCorrectPosition`
- `swap_exchangesEntries`
- `move_repositionsEntry`
- `clear_emptiesStack`
- `current_updatesOnMutation`
- `canGoBack_updatesOnMutation`
- `entriesFlow_emitsOnMutation`

**StateNavigator Tests:**
- `navigate_pushesToBackStack`
- `navigateBack_popsFromBackStack`
- `navigateAndReplace_replacesTopEntry`
- `navigateAndClearAll_clearsAndPushes`
- `currentDestination_derivedFromBackStack`

### 6.3 Acceptance Criteria

- [ ] All KDoc documentation complete
- [ ] STATE_DRIVEN_NAVIGATION.md guide written
- [ ] API_REFERENCE.md updated
- [ ] Demo README updated
- [ ] Unit test coverage ≥ 85%
- [ ] All tests pass on all platforms

---

## 7. Technical Decisions

### 7.1 Why SnapshotStateList over StateFlow<List>?

| Aspect | SnapshotStateList | StateFlow<List> |
|--------|-------------------|-----------------|
| Direct mutation | ✅ Native | ❌ Copy required |
| Compose observation | ✅ Automatic | ✅ Via collectAsState |
| Granular updates | ✅ Element-level | ❌ Full list replacement |
| Memory efficiency | ✅ Copy-on-write | ❌ Full copies |
| Nav3 compatibility | ✅ Same pattern | ❌ Different pattern |

**Decision:** Use `SnapshotStateList` for primary state, provide `snapshotFlow` for Flow consumers.

### 7.2 Why New Classes vs Extending Existing?

| Approach | Pros | Cons |
|----------|------|------|
| Extend existing | Single API surface | Breaking changes, complexity |
| New classes | Backward compatible, clear purpose | Two API surfaces |

**Decision:** Create new `StateBackStack`/`StateNavigator` classes alongside existing `BackStack`/`Navigator`. This:
- Preserves backward compatibility
- Allows gradual migration
- Keeps each approach focused

### 7.3 Thread Safety Approach

| Approach | Pros | Cons |
|----------|------|------|
| Force main thread | Simple, matches Compose | Limits background ops |
| Always withMutableSnapshot | Thread safe | Verbose for main thread |
| Document & let user choose | Flexible | User error risk |

**Decision:** Document that main-thread mutation is safe, background requires `Snapshot.withMutableSnapshot`. Provide extension for safe background mutations:

```kotlin
suspend fun StateBackStack.mutateOnBackground(block: SnapshotStateList<BackStackEntry>.() -> Unit) {
    withContext(Dispatchers.Default) {
        Snapshot.withMutableSnapshot {
            entries.block()
        }
    }
}
```

### 7.4 Derived State vs Flow for computed properties

| Approach | Pros | Cons |
|----------|------|------|
| derivedStateOf | Minimal recomposition | Compose-only |
| StateFlow | Works everywhere | More boilerplate |
| Both | Maximum flexibility | Maintenance burden |

**Decision:** Use `derivedStateOf` for Compose consumers (primary), provide `snapshotFlow` for Flow consumers (secondary).

---

## 8. Risk Assessment

### 8.1 Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Performance with large stacks | Low | Medium | Use derivedStateOf, lazy rendering |
| Thread safety issues | Medium | High | Clear documentation, helper extensions |
| Animation conflicts | Low | Medium | Coordinate with existing animation system |
| Platform inconsistencies | Low | Medium | Thorough multiplatform testing |

### 8.2 Schedule Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Demo complexity creep | Medium | Medium | Fixed feature set, no scope creep |
| Testing overhead | Medium | Low | Prioritize critical path tests |
| Documentation debt | Low | Medium | Document as we implement |

---

## 9. Success Metrics

### 9.1 Functional Metrics

- [ ] All BackStack operations work via direct list manipulation
- [ ] UI correctly observes and renders state changes
- [ ] Transitions animate smoothly
- [ ] Predictive back gestures work
- [ ] Demo is intuitive and educational

### 9.2 Performance Metrics

- [ ] Stack mutations complete in < 1ms
- [ ] UI updates complete in < 16ms (60fps)
- [ ] Memory overhead < 1KB per entry

### 9.3 Quality Metrics

- [ ] Test coverage ≥ 85%
- [ ] Zero breaking changes to existing API
- [ ] Complete KDoc documentation
- [ ] All platforms pass tests

---

## 10. Implementation Order

### Phase 1: Core API (Days 1-3)
1. `StateBackStack.kt`
2. `StateNavigator.kt`
3. `FakeStateNavigator.kt`
4. Unit tests for core classes

### Phase 2: Compose Integration (Days 4-6)
1. `StateNavHost.kt`
2. `rememberStateBackStack`
3. `rememberSaveableStateBackStack`
4. Predictive back integration
5. Integration tests

### Phase 3: Demo Application (Days 7-10)
1. Destinations and graph setup
2. `StateDrivenDemoScreen.kt`
3. `BackstackEditorPanel.kt` with drag-to-reorder
4. Quick actions and state display
5. Code examples view
6. Integration into main demo menu

### Phase 4: Documentation & Polish (Days 11-13)
1. `STATE_DRIVEN_NAVIGATION.md` guide
2. API reference updates
3. Demo README updates
4. Final testing across platforms
5. Code review and cleanup

---

## 11. Open Questions

### 11.1 Resolved

- **Q:** Extend existing or new classes? **A:** New classes for backward compatibility
- **Q:** SnapshotStateList or StateFlow? **A:** SnapshotStateList primary, snapshotFlow secondary
- **Q:** Thread safety approach? **A:** Document main-thread default, provide helper for background

### 11.2 To Be Decided During Implementation

- **Q:** Should `StateBackStack` implement `BackStack` interface?
  - Pro: Polymorphism with existing code
  - Con: Interface uses StateFlow, not SnapshotStateList
  - **Recommendation:** No, keep them separate for clarity

- **Q:** Should demo include comparison toggle between approaches?
  - Pro: Educational value
  - Con: Additional complexity
  - **Recommendation:** Yes, add a simple toggle

---

## 12. Dependencies

### 12.1 Internal Dependencies

- Existing `BackStackEntry` data class (reuse)
- Existing `Destination` interface (reuse)
- Existing `NavigationTransition` (reuse)
- Existing animation utilities (reuse)

### 12.2 External Dependencies

- Compose Runtime (SnapshotStateList, derivedStateOf)
- Compose Animation (AnimatedContent)
- kotlinx.coroutines (Flow)
- kotlinx.serialization (for saveable state)

### 12.3 No New Dependencies Required

All functionality can be implemented with existing project dependencies.

---

## 13. Appendix

### A. Code Examples for Documentation

#### A.1 Basic State-Driven Navigation

```kotlin
@Composable
fun MyApp() {
    val backStack = rememberStateBackStack(HomeDestination)
    
    Row {
        // Sidebar with backstack editor
        BackstackEditor(
            entries = backStack.entries,
            onRemove = { index -> backStack.entries.removeAt(index) },
            onAdd = { dest -> backStack.push(dest) }
        )
        
        // Content area
        StateNavHost(stateBackStack = backStack) { destination ->
            when (destination) {
                HomeDestination -> HomeScreen()
                is ProfileDestination -> ProfileScreen(destination.userId)
                SettingsDestination -> SettingsScreen()
            }
        }
    }
}
```

#### A.2 Direct List Manipulation

```kotlin
// Push
backStack.entries.add(BackStackEntry.create(ProfileDestination("123")))

// Pop
backStack.entries.removeLastOrNull()

// Remove arbitrary entry
backStack.entries.removeAt(1)

// Swap entries
val temp = backStack.entries[0]
backStack.entries[0] = backStack.entries[1]
backStack.entries[1] = temp

// Insert at position
backStack.entries.add(1, BackStackEntry.create(SettingsDestination))

// Clear all
backStack.entries.clear()

// Replace all
backStack.entries.clear()
backStack.entries.addAll(listOf(
    BackStackEntry.create(HomeDestination),
    BackStackEntry.create(ProfileDestination("123"))
))
```

#### A.3 Observation in Non-Compose Context

```kotlin
// Using Flow
lifecycleScope.launch {
    backStack.entriesFlow.collect { entries ->
        println("Stack: ${entries.map { it.destination.route }}")
    }
}

// Using snapshotFlow directly
lifecycleScope.launch {
    snapshotFlow { backStack.entries.toList() }
        .collect { entries ->
            // Handle changes
        }
}
```

### B. Nav3 vs Quo Vadis Comparison Table

| Operation | Navigation 3 | Quo Vadis (Traditional) | Quo Vadis (State-Driven) |
|-----------|--------------|-------------------------|--------------------------|
| Push | `backStack.add(key)` | `navigator.navigate(dest)` | `backStack.push(dest)` or `entries.add(entry)` |
| Pop | `backStack.removeLastOrNull()` | `navigator.navigateBack()` | `backStack.pop()` or `entries.removeLastOrNull()` |
| Remove at index | `backStack.removeAt(i)` | ❌ Not supported | `backStack.removeAt(i)` or `entries.removeAt(i)` |
| Reorder | Direct list manipulation | ❌ Not supported | `backStack.swap(i,j)` or `entries[i] = ...` |
| Insert at index | `backStack.add(i, key)` | ❌ Not supported | `backStack.insert(i, dest)` or `entries.add(i, entry)` |
| Clear | `backStack.clear()` | `backStack.clear()` | `backStack.clear()` or `entries.clear()` |
| Observe | Via Compose observation | `stack.collectAsState()` | Direct or via `derivedStateOf` |

---

## 14. References

- [Jetpack Navigation 3 Announcement](https://android-developers.googleblog.com/2025/05/announcing-jetpack-navigation-3-for-compose.html)
- [Navigation 3 Recipes Repository](https://github.com/android/navigation3)
- [Compose Snapshot System](https://developer.android.com/jetpack/compose/state#snapshot-system)
- [SnapshotStateList Documentation](https://developer.android.com/reference/kotlin/androidx/compose/runtime/snapshots/SnapshotStateList)
- Quo Vadis Architecture Memory: `architecture_patterns`
- Quo Vadis Project Overview Memory: `project_overview`

---

**End of Implementation Plan**

*This document should be referenced throughout implementation. Update status checkboxes as work progresses.*
