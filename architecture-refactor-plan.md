# Architecture Refactor Plan: Unified Stateful BackStack

> **Created:** November 30, 2025  
> **Status:** 📋 Planning Complete - Ready for Implementation  
> **Branch:** `architecture-refactor`  
> **Goal:** Unify navigation architecture with stateful backstack capabilities while preserving all existing features

---

## Executive Summary

This plan outlines the refactoring of quo-vadis to support **stateful backstack manipulation** (Nav3-style patterns) within a **single unified architecture**, rather than maintaining parallel implementations.

### Current State (Problem)
- Two separate architectures exist:
  - **Production:** `BackStack` → `MutableBackStack` → `Navigator` → `GraphNavHost`
  - **State-Driven:** `StateBackStack` → `StateNavigator` → `StateNavHost`
- Features are split between them (predictive back, animations, KSP only in production path)
- Demo creates confusion by showcasing a separate, incomplete architecture

### Target State (Solution)
- **Single unified architecture** with stateful backstack capabilities
- Extend `BackStack` interface with Nav3-style operations
- Refactor `MutableBackStack` to use `SnapshotStateList` internally
- Keep `GraphNavHost` animation system completely unchanged
- Deprecate and remove `StateBackStack`, `StateNavigator`, `StateNavHost`

### Key Constraints
1. ❌ **DO NOT modify animation system** - `GraphNavHost` internals are sacred
2. ✅ **Backward compatibility** - Existing API must continue to work
3. ✅ **Single architecture** - No parallel implementations
4. ✅ **Full feature preservation** - Predictive back, shared transitions, KSP all work

---

## 1. Architecture Overview

### 1.1 Current Architecture (Before)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PRODUCTION PATH (Complete)                        │
│  Navigator ─► MutableBackStack ─► GraphNavHost ─► NavHost           │
│      │             │                   │                             │
│      │        StateFlow<List>     collectAsState()                   │
│      │             │                   │                             │
│  Features: ✅ Predictive Back, ✅ Shared Transitions, ✅ KSP         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    STATE-DRIVEN PATH (Incomplete)                    │
│  StateNavigator ─► StateBackStack ─► StateNavHost                   │
│        │               │                  │                          │
│        │        SnapshotStateList    Direct observation              │
│        │               │                  │                          │
│  Features: ❌ Predictive Back, ❌ Shared Transitions, ❌ KSP         │
│  Has: ✅ insert(), removeAt(), swap(), move()                        │
└─────────────────────────────────────────────────────────────────────┘
```

### 1.2 Target Architecture (After)

```
┌─────────────────────────────────────────────────────────────────────┐
│                    UNIFIED PATH (Complete)                           │
│  Navigator ─► MutableBackStack ─► GraphNavHost ─► NavHost           │
│      │             │                   │                             │
│      │   SnapshotStateList (internal)  collectAsState()              │
│      │   + StateFlow views (derived)   (unchanged!)                  │
│      │             │                                                 │
│  Features: ✅ Predictive Back, ✅ Shared Transitions, ✅ KSP         │
│  NEW: ✅ entries access, insert(), removeAt(), swap(), move()        │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    DEPRECATED (To Be Removed)                        │
│  StateNavigator, StateBackStack, StateNavHost                        │
│  - Functionality merged into main path                               │
│  - Demo updated to use unified API                                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Detailed Design

### 2.1 Extended BackStack Interface

```kotlin
@Stable
interface BackStack {
    // ═══════════════════════════════════════════════════════════════
    // EXISTING STATEFLOW PROPERTIES (UNCHANGED - ANIMATION SYSTEM USES THESE)
    // ═══════════════════════════════════════════════════════════════
    
    /** Current stack as a flow of entries. Used by GraphNavHost. */
    val stack: StateFlow<List<BackStackEntry>>
    
    /** Current top entry. Used by GraphNavHost for animations. */
    val current: StateFlow<BackStackEntry?>
    
    /** Previous entry. Used by GraphNavHost for back animations. */
    val previous: StateFlow<BackStackEntry?>
    
    /** Whether back navigation is possible. Used by predictive back. */
    val canGoBack: StateFlow<Boolean>
    
    // ═══════════════════════════════════════════════════════════════
    // NEW: DIRECT MUTABLE ACCESS (NAV3-STYLE)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Direct access to the mutable entries list.
     * Mutations to this list are reflected in the StateFlow properties.
     * 
     * Example:
     * ```kotlin
     * backStack.entries.add(BackStackEntry.create(destination))
     * backStack.entries.removeAt(1)
     * ```
     */
    val entries: SnapshotStateList<BackStackEntry>
    
    /** Current stack size. */
    val size: Int get() = entries.size
    
    /** Whether the stack is empty. */
    val isEmpty: Boolean get() = entries.isEmpty()
    
    /** Whether the stack has entries. */
    val isNotEmpty: Boolean get() = entries.isNotEmpty()
    
    // ═══════════════════════════════════════════════════════════════
    // EXISTING METHODS (UNCHANGED)
    // ═══════════════════════════════════════════════════════════════
    
    fun push(destination: Destination, transition: NavigationTransition? = null)
    fun pop(): Boolean
    fun popUntil(predicate: (Destination) -> Boolean): Boolean
    fun replace(destination: Destination, transition: NavigationTransition? = null)
    fun replaceAll(destinations: List<Destination>)
    fun clear()
    fun popToRoot(): Boolean
    
    // ═══════════════════════════════════════════════════════════════
    // NEW: ADVANCED MANIPULATION METHODS (NAV3-STYLE)
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Insert a destination at the specified index.
     * @param index Position to insert at (0 = bottom, size = top)
     * @param destination The destination to insert
     * @param transition Optional transition for this entry
     */
    fun insert(index: Int, destination: Destination, transition: NavigationTransition? = null)
    
    /**
     * Remove entry at the specified index.
     * @param index Index to remove
     * @return The removed entry, or null if index invalid
     */
    fun removeAt(index: Int): BackStackEntry?
    
    /**
     * Remove entry by its unique ID.
     * @param id The entry ID to remove
     * @return true if entry was found and removed
     */
    fun removeById(id: String): Boolean
    
    /**
     * Swap two entries in the stack.
     * @param indexA First index
     * @param indexB Second index
     * @return true if swap was successful
     */
    fun swap(indexA: Int, indexB: Int): Boolean
    
    /**
     * Move an entry from one position to another.
     * @param fromIndex Current position
     * @param toIndex Target position
     * @return true if move was successful
     */
    fun move(fromIndex: Int, toIndex: Int): Boolean
    
    /**
     * Replace entire stack with pre-built entries.
     * Useful for restoring saved state.
     */
    fun replaceAllWithEntries(entries: List<BackStackEntry>)
}
```

### 2.2 Refactored MutableBackStack Implementation

```kotlin
/**
 * Mutable implementation of BackStack using SnapshotStateList internally.
 * 
 * This implementation:
 * 1. Uses SnapshotStateList as the source of truth for entries
 * 2. Derives StateFlow properties via snapshotFlow for animation system compatibility
 * 3. Supports both traditional navigation methods AND Nav3-style direct manipulation
 * 
 * @param scope CoroutineScope for StateFlow emissions. Typically rememberCoroutineScope() in Compose.
 */
class MutableBackStack(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) : BackStack {
    
    // ═══════════════════════════════════════════════════════════════
    // PRIMARY STATE (SOURCE OF TRUTH)
    // ═══════════════════════════════════════════════════════════════
    
    private val _entries: SnapshotStateList<BackStackEntry> = mutableStateListOf()
    
    override val entries: SnapshotStateList<BackStackEntry> get() = _entries
    
    // ═══════════════════════════════════════════════════════════════
    // DERIVED STATEFLOWS (FOR ANIMATION SYSTEM COMPATIBILITY)
    // These are derived from _entries via snapshotFlow
    // ═══════════════════════════════════════════════════════════════
    
    override val stack: StateFlow<List<BackStackEntry>> = snapshotFlow { 
        _entries.toList() 
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    
    override val current: StateFlow<BackStackEntry?> = snapshotFlow { 
        _entries.lastOrNull() 
    }.stateIn(scope, SharingStarted.Eagerly, null)
    
    override val previous: StateFlow<BackStackEntry?> = snapshotFlow { 
        if (_entries.size > 1) _entries[_entries.lastIndex - 1] else null 
    }.stateIn(scope, SharingStarted.Eagerly, null)
    
    override val canGoBack: StateFlow<Boolean> = snapshotFlow { 
        _entries.size > 1 
    }.stateIn(scope, SharingStarted.Eagerly, false)
    
    // ═══════════════════════════════════════════════════════════════
    // CONVENIENCE PROPERTIES
    // ═══════════════════════════════════════════════════════════════
    
    override val size: Int get() = _entries.size
    override val isEmpty: Boolean get() = _entries.isEmpty()
    override val isNotEmpty: Boolean get() = _entries.isNotEmpty()
    
    // ═══════════════════════════════════════════════════════════════
    // TRADITIONAL NAVIGATION METHODS (EXISTING API)
    // ═══════════════════════════════════════════════════════════════
    
    override fun push(destination: Destination, transition: NavigationTransition?) {
        _entries.add(BackStackEntry.create(destination, transition))
    }
    
    override fun pop(): Boolean {
        if (_entries.isEmpty()) return false
        _entries.removeAt(_entries.lastIndex)
        return true
    }
    
    override fun popUntil(predicate: (Destination) -> Boolean): Boolean {
        val index = _entries.indexOfLast { predicate(it.destination) }
        if (index == -1) return false
        
        // Remove entries after the found index
        while (_entries.size > index + 1) {
            _entries.removeAt(_entries.lastIndex)
        }
        return true
    }
    
    override fun replace(destination: Destination, transition: NavigationTransition?) {
        if (_entries.isEmpty()) {
            push(destination, transition)
            return
        }
        _entries[_entries.lastIndex] = BackStackEntry.create(destination, transition)
    }
    
    override fun replaceAll(destinations: List<Destination>) {
        _entries.clear()
        destinations.forEach { _entries.add(BackStackEntry.create(it)) }
    }
    
    override fun clear() {
        _entries.clear()
    }
    
    override fun popToRoot(): Boolean {
        if (_entries.size <= 1) return false
        while (_entries.size > 1) {
            _entries.removeAt(_entries.lastIndex)
        }
        return true
    }
    
    // ═══════════════════════════════════════════════════════════════
    // NEW: NAV3-STYLE ADVANCED MANIPULATION METHODS
    // ═══════════════════════════════════════════════════════════════
    
    override fun insert(index: Int, destination: Destination, transition: NavigationTransition?) {
        if (index < 0 || index > _entries.size) return
        _entries.add(index, BackStackEntry.create(destination, transition))
    }
    
    override fun removeAt(index: Int): BackStackEntry? {
        if (index < 0 || index >= _entries.size) return null
        return _entries.removeAt(index)
    }
    
    override fun removeById(id: String): Boolean {
        val index = _entries.indexOfFirst { it.id == id }
        if (index == -1) return false
        _entries.removeAt(index)
        return true
    }
    
    override fun swap(indexA: Int, indexB: Int): Boolean {
        if (indexA < 0 || indexA >= _entries.size) return false
        if (indexB < 0 || indexB >= _entries.size) return false
        if (indexA == indexB) return true
        
        val temp = _entries[indexA]
        _entries[indexA] = _entries[indexB]
        _entries[indexB] = temp
        return true
    }
    
    override fun move(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex < 0 || fromIndex >= _entries.size) return false
        if (toIndex < 0 || toIndex > _entries.size) return false
        if (fromIndex == toIndex) return true
        
        val entry = _entries.removeAt(fromIndex)
        val adjustedTo = if (toIndex > fromIndex) toIndex - 1 else toIndex
        _entries.add(adjustedTo.coerceIn(0, _entries.size), entry)
        return true
    }
    
    override fun replaceAllWithEntries(entries: List<BackStackEntry>) {
        _entries.clear()
        _entries.addAll(entries)
    }
}
```

### 2.3 Navigator Interface Changes

The `Navigator` interface needs minimal changes - just expose the new BackStack capabilities:

```kotlin
interface Navigator : ParentNavigator {
    val backStack: BackStack  // Already exists, now has more capabilities
    
    // Existing methods remain unchanged
    // ...
    
    // NEW: Convenience access to backStack.entries
    val entries: SnapshotStateList<BackStackEntry> get() = backStack.entries
}
```

### 2.4 DefaultNavigator Changes

```kotlin
class DefaultNavigator(
    scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) : Navigator {
    
    // Change from MutableBackStack() to MutableBackStack(scope)
    override val backStack: BackStack = MutableBackStack(scope)
    
    // ... rest remains unchanged
}
```

### 2.5 Factory Function Updates

```kotlin
/**
 * Remember a Navigator with the new scope-aware BackStack.
 */
@Composable
fun rememberNavigator(
    startDestination: Destination? = null
): Navigator {
    val scope = rememberCoroutineScope()
    return remember {
        DefaultNavigator(scope).apply {
            startDestination?.let { setStartDestination(it) }
        }
    }
}
```

---

## 3. Animation System Compatibility

### 3.1 Why This Works (Critical Understanding)

The animation system in `GraphNavHost` uses:

```kotlin
val backStackEntries by navigator.backStack.stack.collectAsState()
val currentEntry by navigator.backStack.current.collectAsState()
val previousEntry by navigator.backStack.previous.collectAsState()
```

With our refactored `MutableBackStack`:
1. `_entries` (SnapshotStateList) is mutated
2. `snapshotFlow { _entries.toList() }` automatically captures the change
3. `.stateIn(scope, SharingStarted.Eagerly, ...)` converts to StateFlow
4. `collectAsState()` in GraphNavHost observes the StateFlow
5. Animation direction detection works because size changes are detected

### 3.2 Synchronization Guarantee

The key insight is that `SharingStarted.Eagerly` ensures:
- StateFlow is always active
- Updates propagate immediately on mutation
- No lag between SnapshotStateList mutation and StateFlow update

### 3.3 What Remains Unchanged

These files require **NO CHANGES**:
- `GraphNavHost.kt` - Animation logic untouched
- `PredictiveBackNavigation.kt` - Predictive back untouched
- `ComposableCache.kt` - Caching logic untouched
- `NavigationTransitions.kt` - Transition definitions untouched
- All KSP generators - Generated code still works

---

## 4. Demo Application Updates

### 4.1 Update State-Driven Demo

Transform the demo from using `StateBackStack`/`StateNavHost` to using the unified API:

**Before (Current Demo):**
```kotlin
@Composable
fun StateDrivenDemoScreen() {
    val backStack = rememberStateBackStack(StateDrivenDestination.Home)
    
    StateNavHost(
        stateBackStack = backStack,
        ...
    )
}
```

**After (Unified Demo):**
```kotlin
@Composable
fun StateDrivenDemoScreen() {
    val navigator = rememberNavigator(startDestination = StateDrivenDestination.Home)
    
    // Direct backStack access for Nav3-style manipulation
    val entries = navigator.backStack.entries
    
    NavHost(
        graph = stateDrivenGraph,
        navigator = navigator,
        ...
    )
}
```

### 4.2 Demo Features to Showcase

The demo should showcase:
1. **Direct entry manipulation** via `navigator.backStack.entries`
2. **Insert at position** via `navigator.backStack.insert()`
3. **Remove by index** via `navigator.backStack.removeAt()`
4. **Swap entries** via `navigator.backStack.swap()`
5. **Move entries** via `navigator.backStack.move()`
6. **All existing features** still work (predictive back, transitions, etc.)

---

## 5. Files to Modify

### 5.1 Core Library Changes

| File | Change Type | Description |
|------|-------------|-------------|
| `BackStack.kt` | MODIFY | Extend interface with new properties/methods |
| `MutableBackStack` (in BackStack.kt) | REWRITE | Use SnapshotStateList + derived StateFlows |
| `Navigator.kt` | MINOR | Add convenience `entries` property |
| `DefaultNavigator.kt` | MINOR | Pass scope to MutableBackStack |
| `NavigatorHelpers.kt` (if exists) | MINOR | Update factory functions |

### 5.2 Files to Deprecate/Remove

| File | Action | Reason |
|------|--------|--------|
| `StateBackStack.kt` | DEPRECATE → REMOVE | Merged into MutableBackStack |
| `StateNavigator.kt` | DEPRECATE → REMOVE | Unnecessary wrapper |
| `StateNavHost.kt` | DEPRECATE → REMOVE | Use GraphNavHost instead |
| `StateNavHostHelpers.kt` | DEPRECATE → REMOVE | Unnecessary helpers |
| `FakeStateNavigator.kt` | DEPRECATE → REMOVE | Use FakeNavigator |

### 5.3 Demo Application Changes

| File | Change Type | Description |
|------|-------------|-------------|
| `StateDrivenDemoScreen.kt` | REWRITE | Use unified Navigator/NavHost |
| `BackstackEditorPanel.kt` | MODIFY | Use `navigator.backStack.entries` |
| `StateDrivenContentScreens.kt` | KEEP | Content screens unchanged |
| `StateDrivenDestinations.kt` | KEEP | Destinations unchanged |

### 5.4 Documentation Changes

| File | Change Type | Description |
|------|-------------|-------------|
| `STATE_DRIVEN_NAVIGATION.md` | REWRITE | Document unified API |
| `API_REFERENCE.md` | UPDATE | Add new BackStack methods |
| `ARCHITECTURE.md` | UPDATE | Reflect unified architecture |

---

## 6. Implementation Phases

### Phase 1: Core BackStack Refactor (Day 1-2)

1. **Extend `BackStack` interface** with new properties and methods
2. **Rewrite `MutableBackStack`** to use SnapshotStateList internally
3. **Update `DefaultNavigator`** to pass scope
4. **Update factory functions** (`rememberNavigator`, etc.)
5. **Run existing tests** to verify backward compatibility
6. **Build verification**: `./gradlew :quo-vadis-core:build -x detekt -x test`

### Phase 2: Deprecate State-Driven Classes (Day 2-3)

1. **Mark deprecated** with `@Deprecated` annotation:
   - `StateBackStack`
   - `StateNavigator`
   - `StateNavHost`
   - `StateNavHostHelpers`
   - `FakeStateNavigator`
2. **Add deprecation messages** pointing to unified API
3. **Keep classes functional** for migration period

### Phase 3: Update Demo Application (Day 3-4)

1. **Rewrite `StateDrivenDemoScreen`** to use Navigator/NavHost
2. **Update `BackstackEditorPanel`** to use `navigator.backStack.entries`
3. **Verify all demo features** work:
   - Entry manipulation
   - Predictive back
   - Animations
   - Shared transitions (if demo uses them)
4. **Test on all platforms**

### Phase 4: Documentation & Cleanup (Day 4-5)

1. **Update `STATE_DRIVEN_NAVIGATION.md`** to document unified API
2. **Update `API_REFERENCE.md`** with new BackStack methods
3. **Remove deprecated files** (after verification)
4. **Final testing** across all platforms
5. **Update CHANGELOG.md**

---

## 7. Migration Guide

### For Users of StateBackStack

**Before:**
```kotlin
val backStack = rememberStateBackStack(HomeDestination)

// Direct list manipulation
backStack.entries.add(BackStackEntry.create(destination))
backStack.entries.removeAt(1)
backStack.swap(0, 1)

StateNavHost(stateBackStack = backStack) { destination ->
    // content
}
```

**After:**
```kotlin
val navigator = rememberNavigator(startDestination = HomeDestination)

// Same direct list manipulation - now via navigator.backStack
navigator.backStack.entries.add(BackStackEntry.create(destination))
navigator.backStack.removeAt(1)  // or navigator.backStack.entries.removeAt(1)
navigator.backStack.swap(0, 1)

NavHost(graph = graph, navigator = navigator)
```

### For Users of Traditional Navigator

**No changes required!** Existing code continues to work:

```kotlin
val navigator = rememberNavigator(startDestination = HomeDestination)

navigator.navigate(ProfileDestination)
navigator.navigateBack()
navigator.navigateAndClearTo(HomeDestination, "auth", inclusive = true)

NavHost(graph = graph, navigator = navigator)
```

**NEW capabilities available:**
```kotlin
// Direct entry manipulation (optional - use when needed)
navigator.backStack.insert(1, FilterDestination)
navigator.backStack.removeAt(2)
navigator.backStack.swap(0, 1)
navigator.entries.forEachIndexed { i, entry -> ... }
```

---

## 8. Testing Strategy

### 8.1 Backward Compatibility Tests

Verify existing behavior unchanged:
- `push()` adds entry to end
- `pop()` removes last entry
- `popUntil()` removes until predicate
- `replace()` replaces top entry
- `replaceAll()` replaces entire stack
- `clear()` empties stack
- `popToRoot()` keeps only first entry
- StateFlow properties update correctly
- `collectAsState()` in composables works

### 8.2 New Functionality Tests

Test new methods:
- `insert(index, destination)` adds at correct position
- `removeAt(index)` removes correct entry
- `removeById(id)` finds and removes entry
- `swap(a, b)` exchanges entries
- `move(from, to)` repositions entry
- `entries` list is mutable and observable
- Size, isEmpty, isNotEmpty properties work

### 8.3 Animation System Tests

Verify animations unaffected:
- Forward navigation animates correctly
- Back navigation animates correctly
- Replace animation works
- Predictive back gesture works
- Entry capture during gesture works
- Cache locking during animation works

### 8.4 Integration Tests

Test full navigation flows:
- Demo app compiles and runs
- All demo features functional
- Multi-platform builds succeed

---

## 9. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| StateFlow sync issues | Low | High | Use `SharingStarted.Eagerly`, test extensively |
| Animation timing changes | Low | High | Don't modify GraphNavHost, only BackStack |
| CoroutineScope lifecycle | Medium | Medium | Document scope requirements, provide factory |
| Breaking changes to API | Low | Medium | Extend interface, don't change existing signatures |
| Demo regression | Medium | Low | Comprehensive demo testing |

---

## 10. Success Criteria

### Must Have
- [ ] All existing tests pass
- [ ] Demo works with unified API
- [ ] Predictive back functions correctly
- [ ] Animations unchanged
- [ ] Build succeeds on all platforms

### Should Have
- [ ] Documentation updated
- [ ] Deprecated classes marked
- [ ] Migration guide complete
- [ ] CHANGELOG updated

### Nice to Have
- [ ] Performance benchmarks showing no regression
- [ ] Additional unit tests for new methods
- [ ] Demo showcases all new capabilities

---

## 11. Open Questions

### Resolved

1. **Q:** Use adapter pattern or modify MutableBackStack directly?  
   **A:** Modify MutableBackStack directly - simpler, no extra allocations

2. **Q:** How to handle CoroutineScope?  
   **A:** Pass scope to MutableBackStack constructor, provide factory functions

3. **Q:** Keep StateBackStack as alternative?  
   **A:** No - deprecate and remove to maintain single architecture

### To Be Decided During Implementation

1. **Q:** Default scope for MutableBackStack when none provided?  
   **Options:** `GlobalScope`, `CoroutineScope(Dispatchers.Main)`, require scope  
   **Recommendation:** Default to `CoroutineScope(Dispatchers.Main.immediate)` for simplicity

2. **Q:** Keep `pop()` returning `Boolean` or change to `BackStackEntry?`?  
   **Options:** Keep Boolean (backward compatible), add `popAndGet(): BackStackEntry?`  
   **Recommendation:** Keep Boolean, add `popAndGet()` as new method

---

## 12. Implementation Checklist

### Phase 1: Core
- [ ] Extend `BackStack` interface with new properties/methods
- [ ] Rewrite `MutableBackStack` with SnapshotStateList
- [ ] Update `DefaultNavigator` to pass scope
- [ ] Update `rememberNavigator` factory function
- [ ] Verify build: `./gradlew :quo-vadis-core:assembleDebug`
- [ ] Run existing tests

### Phase 2: Deprecation
- [ ] Add `@Deprecated` to `StateBackStack`
- [ ] Add `@Deprecated` to `StateNavigator`
- [ ] Add `@Deprecated` to `StateNavHost`
- [ ] Add `@Deprecated` to helper functions
- [ ] Verify deprecation warnings appear

### Phase 3: Demo
- [ ] Rewrite `StateDrivenDemoScreen` to use Navigator
- [ ] Update `BackstackEditorPanel` to use `navigator.backStack.entries`
- [ ] Verify demo compiles: `./gradlew :composeApp:assembleDebug`
- [ ] Test demo on Android
- [ ] Test demo on iOS (if possible)

### Phase 4: Documentation
- [ ] Update `STATE_DRIVEN_NAVIGATION.md`
- [ ] Update `API_REFERENCE.md`
- [ ] Update CHANGELOG.md
- [ ] Remove deprecated files (after testing)

---

## Appendix A: Code Examples

### A.1 Direct Entry Manipulation

```kotlin
@Composable
fun MyScreen() {
    val navigator = rememberNavigator(startDestination = Home)
    
    // Insert a filter screen between home and current
    Button(onClick = {
        navigator.backStack.insert(1, FilterScreen)
    }) {
        Text("Add Filter")
    }
    
    // Remove a specific entry by index
    Button(onClick = {
        navigator.backStack.removeAt(1)
    }) {
        Text("Remove Middle")
    }
    
    // Swap two entries
    Button(onClick = {
        navigator.backStack.swap(0, 1)
    }) {
        Text("Swap")
    }
}
```

### A.2 Observing Entries

```kotlin
@Composable
fun BackStackViewer(navigator: Navigator) {
    // Option 1: Observe via StateFlow (traditional)
    val entries by navigator.backStack.stack.collectAsState()
    
    // Option 2: Direct access (new)
    val directEntries = navigator.backStack.entries
    
    LazyColumn {
        items(entries) { entry ->
            Text("${entry.destination.route} (${entry.id})")
        }
    }
}
```

### A.3 Custom Navigation Logic

```kotlin
fun Navigator.navigateWithHistoryCleanup(destination: Destination) {
    // Remove duplicate destinations from history
    val duplicateIndex = backStack.entries.indexOfFirst { 
        it.destination.route == destination.route 
    }
    if (duplicateIndex >= 0) {
        backStack.removeAt(duplicateIndex)
    }
    navigate(destination)
}
```

---

## Appendix B: Comparison with Navigation 3

| Operation | Navigation 3 | Quo Vadis (Unified) |
|-----------|--------------|---------------------|
| Create backstack | `rememberNavBackStack(Home)` | `rememberNavigator(Home)` |
| Access entries | `backStack` (SnapshotStateList) | `navigator.backStack.entries` |
| Push | `backStack.add(key)` | `navigator.navigate(dest)` or `entries.add(entry)` |
| Pop | `backStack.removeLastOrNull()` | `navigator.navigateBack()` or `entries.removeAt(lastIndex)` |
| Remove at index | `backStack.removeAt(i)` | `navigator.backStack.removeAt(i)` |
| Insert at index | `backStack.add(i, key)` | `navigator.backStack.insert(i, dest)` |
| Observe | Direct (SnapshotStateList) | Direct (`entries`) or Flow (`stack.collectAsState()`) |
| Render | `NavDisplay(backStack)` | `NavHost(graph, navigator)` |

---

**End of Implementation Plan**

*This document serves as the blueprint for the architecture refactor. Update checkboxes as work progresses.*
