# Unified BackStack Architecture Design

> **Created:** November 30, 2025  
> **Status:** 📐 Design Document  
> **Objective:** Unify `BackStack` and `StateBackStack` into a single architecture that supports both StateFlow-based observation AND direct mutable access

---

## Executive Summary

This design unifies quo-vadis's navigation architecture by:

1. **Extending** `BackStack` interface with mutable list access methods
2. **Refactoring** `MutableBackStack` to use `SnapshotStateList` internally while exposing `StateFlow` views
3. **Deprecating** the separate `StateBackStack` class (will be merged into `MutableBackStack`)
4. **Preserving** all existing animation system behavior in `GraphNavHost`

### Key Insight

The animation system uses `collectAsState()` on StateFlows, but `snapshotFlow { ... }.stateIn(scope, ...)` can convert Compose state to StateFlow. This allows us to:
- Store entries in `SnapshotStateList` for direct mutation
- Derive `StateFlow` properties from the snapshot state
- Keep `GraphNavHost` completely unchanged

---

## 1. Current Architecture Analysis

### 1.1 Current `BackStack` Interface

```kotlin
@Stable
interface BackStack {
    val stack: StateFlow<List<BackStackEntry>>
    val current: StateFlow<BackStackEntry?>
    val previous: StateFlow<BackStackEntry?>
    val canGoBack: StateFlow<Boolean>
    
    fun push(destination: Destination, transition: NavigationTransition? = null)
    fun pop(): Boolean
    fun popUntil(predicate: (Destination) -> Boolean): Boolean
    fun replace(destination: Destination, transition: NavigationTransition? = null)
    fun replaceAll(destinations: List<Destination>)
    fun clear()
    fun popToRoot(): Boolean
}
```

### 1.2 Current `MutableBackStack` Implementation

Uses `MutableStateFlow` internally:
```kotlin
class MutableBackStack : BackStack {
    private val _stack = MutableStateFlow<List<BackStackEntry>>(emptyList())
    override val stack: StateFlow<List<BackStackEntry>> = _stack
    // ... similar for current, previous, canGoBack
    
    private fun updateStack(newStack: List<BackStackEntry>) {
        _stack.value = newStack
        _current.value = newStack.lastOrNull()
        _previous.value = if (newStack.size > 1) newStack[newStack.lastIndex - 1] else null
        _canGoBack.value = newStack.size > 1
    }
}
```

### 1.3 Separate `StateBackStack` (Not Implementing `BackStack`)

Uses `SnapshotStateList` + `derivedStateOf`:
```kotlin
@Stable
class StateBackStack {
    val entries: SnapshotStateList<BackStackEntry> = mutableStateListOf()
    
    val current: BackStackEntry? by derivedStateOf { entries.lastOrNull() }
    val previous: BackStackEntry? by derivedStateOf { ... }
    val canGoBack: Boolean by derivedStateOf { entries.size > 1 }
    
    // Rich API: insert, removeAt, swap, move, etc.
}
```

### 1.4 Animation System Requirements (SACRED)

`GraphNavHost` requires these **EXACT** patterns:
```kotlin
val backStackEntries by navigator.backStack.stack.collectAsState()
val currentEntry by navigator.backStack.current.collectAsState()
val previousEntry by navigator.backStack.previous.collectAsState()
```

The animation system uses:
- `backStackEntries.size` for direction detection (forward vs back)
- `currentEntry?.id` for cache keys and animation targeting
- Size comparison across frames for animation triggering

---

## 2. Unified Architecture Design

### 2.1 Extended `BackStack` Interface

```kotlin
@Stable
interface BackStack {
    // ══════════════════════════════════════════════════════════════════════
    // EXISTING: StateFlow Properties (For GraphNavHost Animation System)
    // These MUST remain StateFlow for backward compatibility
    // ══════════════════════════════════════════════════════════════════════
    
    val stack: StateFlow<List<BackStackEntry>>
    val current: StateFlow<BackStackEntry?>
    val previous: StateFlow<BackStackEntry?>
    val canGoBack: StateFlow<Boolean>
    
    // ══════════════════════════════════════════════════════════════════════
    // EXISTING: Basic Operations
    // ══════════════════════════════════════════════════════════════════════
    
    fun push(destination: Destination, transition: NavigationTransition? = null)
    fun pop(): Boolean
    fun popUntil(predicate: (Destination) -> Boolean): Boolean
    fun replace(destination: Destination, transition: NavigationTransition? = null)
    fun replaceAll(destinations: List<Destination>)
    fun clear()
    fun popToRoot(): Boolean
    
    // ══════════════════════════════════════════════════════════════════════
    // NEW: Direct Access to Mutable Entries List (Nav3-Style)
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * Direct access to the mutable entries list.
     * 
     * This enables Navigation 3-style patterns where developers can directly
     * manipulate the backstack state. Changes to this list are automatically
     * reflected in the [stack], [current], [previous], and [canGoBack] StateFlows.
     * 
     * Example usages:
     * ```kotlin
     * // Insert at specific position
     * backStack.entries.add(2, BackStackEntry.create(SettingsScreen))
     * 
     * // Remove specific entry
     * backStack.entries.removeAt(1)
     * 
     * // Swap entries
     * val temp = backStack.entries[0]
     * backStack.entries[0] = backStack.entries[1]
     * backStack.entries[1] = temp
     * ```
     * 
     * @return The mutable list of backstack entries
     */
    val entries: SnapshotStateList<BackStackEntry>
    
    // ══════════════════════════════════════════════════════════════════════
    // NEW: Convenience Methods for Common Patterns
    // ══════════════════════════════════════════════════════════════════════
    
    /**
     * Inserts a destination at the specified index.
     * 
     * @param index The index at which to insert (0 = bottom of stack)
     * @param destination The destination to insert
     * @param transition Optional transition animation
     */
    fun insert(index: Int, destination: Destination, transition: NavigationTransition? = null)
    
    /**
     * Removes the entry at the specified index.
     * 
     * @param index The index of the entry to remove
     * @return The removed entry, or null if index was out of bounds
     */
    fun removeAt(index: Int): BackStackEntry?
    
    /**
     * Removes the first entry with the specified ID.
     * 
     * @param id The unique identifier of the entry to remove
     * @return true if an entry was removed
     */
    fun removeById(id: String): Boolean
    
    /**
     * Swaps two entries in the backstack by their indices.
     * 
     * @param indexA The index of the first entry
     * @param indexB The index of the second entry
     */
    fun swap(indexA: Int, indexB: Int)
    
    /**
     * Moves an entry from one position to another.
     * 
     * @param fromIndex The current index of the entry to move
     * @param toIndex The target index for the entry
     */
    fun move(fromIndex: Int, toIndex: Int)
    
    /**
     * Replaces the entire backstack with pre-created entries.
     * 
     * Unlike [replaceAll] which creates new entries, this method uses
     * the provided entries directly, preserving their IDs.
     * 
     * @param newEntries The entries to replace the backstack with
     */
    fun replaceAllWithEntries(newEntries: List<BackStackEntry>)
    
    /**
     * Returns the number of entries in the backstack.
     */
    val size: Int
    
    /**
     * Returns true if the backstack is empty.
     */
    val isEmpty: Boolean
    
    /**
     * Returns true if the backstack is not empty.
     */
    val isNotEmpty: Boolean
}
```

### 2.2 Revised `MutableBackStack` Implementation

The key innovation is using `SnapshotStateList` internally and deriving `StateFlow` views:

```kotlin
/**
 * Mutable implementation of BackStack using Compose's snapshot state system.
 * 
 * Internally uses [SnapshotStateList] for storage, enabling:
 * - Direct mutable access via [entries] property
 * - Automatic StateFlow derivation for animation system compatibility
 * - Efficient Compose recomposition
 * 
 * The StateFlow properties ([stack], [current], [previous], [canGoBack]) are
 * derived from the snapshot state, ensuring all observation patterns work correctly.
 */
@Stable
class MutableBackStack(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) : BackStack {
    
    // ══════════════════════════════════════════════════════════════════════
    // PRIMARY STATE: SnapshotStateList (Source of Truth)
    // ══════════════════════════════════════════════════════════════════════
    
    private val _entries: SnapshotStateList<BackStackEntry> = mutableStateListOf()
    
    override val entries: SnapshotStateList<BackStackEntry> 
        get() = _entries
    
    // ══════════════════════════════════════════════════════════════════════
    // DERIVED STATE: Compose-native Computed Properties
    // Used for efficient Composable observation without flows
    // ══════════════════════════════════════════════════════════════════════
    
    private val _currentDerived by derivedStateOf { _entries.lastOrNull() }
    private val _previousDerived by derivedStateOf { 
        if (_entries.size > 1) _entries[_entries.lastIndex - 1] else null 
    }
    private val _canGoBackDerived by derivedStateOf { _entries.size > 1 }
    
    // ══════════════════════════════════════════════════════════════════════
    // STATEFLOW VIEWS: For Animation System Compatibility
    // These wrap snapshotFlow to convert Compose state to Flow
    // ══════════════════════════════════════════════════════════════════════
    
    override val stack: StateFlow<List<BackStackEntry>> = snapshotFlow { 
        _entries.toList() 
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = _entries.toList()
    )
    
    override val current: StateFlow<BackStackEntry?> = snapshotFlow { 
        _currentDerived 
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = _currentDerived
    )
    
    override val previous: StateFlow<BackStackEntry?> = snapshotFlow { 
        _previousDerived 
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = _previousDerived
    )
    
    override val canGoBack: StateFlow<Boolean> = snapshotFlow { 
        _canGoBackDerived 
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = _canGoBackDerived
    )
    
    // ══════════════════════════════════════════════════════════════════════
    // SIZE/EMPTY PROPERTIES
    // ══════════════════════════════════════════════════════════════════════
    
    override val size: Int
        get() = _entries.size
    
    override val isEmpty: Boolean
        get() = _entries.isEmpty()
    
    override val isNotEmpty: Boolean
        get() = _entries.isNotEmpty()
    
    // ══════════════════════════════════════════════════════════════════════
    // EXISTING OPERATIONS (Implemented via SnapshotStateList)
    // ══════════════════════════════════════════════════════════════════════
    
    override fun push(destination: Destination, transition: NavigationTransition?) {
        val entry = BackStackEntry.create(destination, transition)
        _entries.add(entry)
    }
    
    override fun pop(): Boolean {
        return if (_entries.isNotEmpty()) {
            _entries.removeAt(_entries.lastIndex)
            true
        } else {
            false
        }
    }
    
    override fun popUntil(predicate: (Destination) -> Boolean): Boolean {
        val index = _entries.indexOfLast { predicate(it.destination) }
        if (index == -1) return false
        
        // Remove all entries after the found index
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
        val entry = BackStackEntry.create(destination, transition)
        _entries[_entries.lastIndex] = entry
    }
    
    override fun replaceAll(destinations: List<Destination>) {
        _entries.clear()
        destinations.forEach { destination ->
            _entries.add(BackStackEntry.create(destination))
        }
    }
    
    override fun replaceAllWithEntries(newEntries: List<BackStackEntry>) {
        _entries.clear()
        _entries.addAll(newEntries)
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
    
    // ══════════════════════════════════════════════════════════════════════
    // NEW OPERATIONS (Nav3-Style Direct Manipulation)
    // ══════════════════════════════════════════════════════════════════════
    
    override fun insert(index: Int, destination: Destination, transition: NavigationTransition?) {
        val entry = BackStackEntry.create(destination, transition)
        _entries.add(index, entry)
    }
    
    override fun removeAt(index: Int): BackStackEntry? {
        return if (index in _entries.indices) {
            _entries.removeAt(index)
        } else {
            null
        }
    }
    
    override fun removeById(id: String): Boolean {
        val index = _entries.indexOfFirst { it.id == id }
        return if (index >= 0) {
            _entries.removeAt(index)
            true
        } else {
            false
        }
    }
    
    override fun swap(indexA: Int, indexB: Int) {
        val temp = _entries[indexA]
        _entries[indexA] = _entries[indexB]
        _entries[indexB] = temp
    }
    
    override fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val entry = _entries.removeAt(fromIndex)
        _entries.add(toIndex, entry)
    }
}
```

### 2.3 CoroutineScope Management

For `MutableBackStack`, the `CoroutineScope` is needed for `stateIn()`. Several options:

#### Option A: Constructor Injection (Recommended)

```kotlin
class MutableBackStack(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) : BackStack
```

Usage:
```kotlin
// In ViewModel
class NavigationViewModel : ViewModel() {
    val backStack = MutableBackStack(viewModelScope)
}

// In Compose with rememberCoroutineScope
@Composable
fun rememberBackStack(): MutableBackStack {
    val scope = rememberCoroutineScope()
    return remember { MutableBackStack(scope) }
}
```

#### Option B: Lazy StateFlow Creation

```kotlin
class MutableBackStack : BackStack {
    private var _scope: CoroutineScope? = null
    private val scope: CoroutineScope
        get() = _scope ?: error("BackStack not initialized. Call initialize(scope) first.")
    
    private var _stackFlow: StateFlow<List<BackStackEntry>>? = null
    override val stack: StateFlow<List<BackStackEntry>>
        get() = _stackFlow ?: snapshotFlow { _entries.toList() }
            .stateIn(scope, SharingStarted.Eagerly, _entries.toList())
            .also { _stackFlow = it }
    
    fun initialize(scope: CoroutineScope) {
        _scope = scope
    }
}
```

**Recommendation:** Option A is cleaner and more explicit.

---

## 3. How GraphNavHost Remains Unchanged

### 3.1 Current GraphNavHost Usage

```kotlin
@Composable
private fun GraphNavHostContent(
    navigator: Navigator,
    // ...
) {
    val backStackEntries by navigator.backStack.stack.collectAsState()
    val currentEntry by navigator.backStack.current.collectAsState()
    val previousEntry by navigator.backStack.previous.collectAsState()
    val canGoBack by remember { derivedStateOf { backStackEntries.size > 1 } }
    // ... rest of animation logic
}
```

### 3.2 Why This Still Works

1. **`stack: StateFlow<List<BackStackEntry>>`** - Now derived from `snapshotFlow { _entries.toList() }.stateIn(...)`. The `collectAsState()` call will receive updates whenever the `SnapshotStateList` changes.

2. **Size-based direction detection** - `backStackEntries.size` comparison works identically. When you call `_entries.add(...)` or `_entries.removeAt(...)`, the `snapshotFlow` emits a new list, which flows through `stateIn()` to the `StateFlow`, which is collected by `collectAsState()`.

3. **Entry ID caching** - `currentEntry?.id` for cache keys works identically since `BackStackEntry` instances are preserved.

4. **Timing** - `SharingStarted.Eagerly` ensures StateFlows are always hot and emit immediately on change.

### 3.3 Synchronization Considerations

**Question:** Is there any race condition between SnapshotStateList mutation and StateFlow emission?

**Answer:** No, because:
1. All mutations happen on Main thread (Compose requirement)
2. `snapshotFlow` captures state atomically in snapshots
3. `stateIn(Dispatchers.Main.immediate)` ensures emissions happen synchronously on Main
4. `collectAsState()` collects on Main dispatcher

The emission path is:
```
SnapshotStateList mutation (Main)
    → Snapshot notification
    → snapshotFlow emission
    → stateIn transforms to StateFlow
    → collectAsState() updates Compose state (same frame)
```

---

## 4. Navigator Integration

### 4.1 DefaultNavigator Updates

The `DefaultNavigator` creates a `MutableBackStack` internally. With the new design:

```kotlin
class DefaultNavigator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main.immediate)
) : Navigator {
    
    private val _backStack = MutableBackStack(scope)
    override val backStack: BackStack = _backStack
    
    // ... rest unchanged
}
```

### 4.2 Direct Backstack Manipulation

Users can now do:

```kotlin
val navigator: Navigator = /* ... */

// Existing methods work
navigator.navigate(DetailScreen)
navigator.navigateBack()

// NEW: Direct entry manipulation
navigator.backStack.entries.add(2, BackStackEntry.create(SettingsScreen))
navigator.backStack.insert(1, ProfileScreen)
navigator.backStack.removeAt(3)
navigator.backStack.swap(0, 1)
navigator.backStack.move(2, 0)

// Direct indexed access
val firstEntry = navigator.backStack.entries[0]
navigator.backStack.entries[1] = BackStackEntry.create(NewScreen)
```

---

## 5. New Operations Available

### 5.1 Navigation 3-Style Patterns

```kotlin
// ═══════════════════════════════════════════════════════════════════
// Pattern 1: Insert screen in middle of stack
// ═══════════════════════════════════════════════════════════════════
// Before: [Home, List, Detail]
// After:  [Home, List, Filter, Detail]
navigator.backStack.insert(2, FilterScreen)

// ═══════════════════════════════════════════════════════════════════
// Pattern 2: Remove specific screen without affecting others
// ═══════════════════════════════════════════════════════════════════
// Before: [Home, TempAuth, List, Detail]
// After:  [Home, List, Detail]
navigator.backStack.removeAt(1)

// ═══════════════════════════════════════════════════════════════════
// Pattern 3: Reorder tabs
// ═══════════════════════════════════════════════════════════════════
// Before: [Home, Profile, Settings]
// After:  [Profile, Home, Settings]
navigator.backStack.swap(0, 1)

// ═══════════════════════════════════════════════════════════════════
// Pattern 4: Bring to front
// ═══════════════════════════════════════════════════════════════════
// Before: [Home, Profile, Settings, List]
// After:  [Home, Settings, List, Profile]
navigator.backStack.move(1, 3)

// ═══════════════════════════════════════════════════════════════════
// Pattern 5: Complex state restoration
// ═══════════════════════════════════════════════════════════════════
val savedEntries: List<BackStackEntry> = loadFromStorage()
navigator.backStack.replaceAllWithEntries(savedEntries)

// ═══════════════════════════════════════════════════════════════════
// Pattern 6: Direct list manipulation (advanced)
// ═══════════════════════════════════════════════════════════════════
navigator.backStack.entries.add(0, BackStackEntry.create(SplashScreen))
navigator.backStack.entries.removeAll { it.destination is TempScreen }
```

### 5.2 Type-Safe Convenience Extensions

```kotlin
// In NavigationExtensions.kt

/**
 * Removes all entries matching the predicate.
 */
fun BackStack.removeWhere(predicate: (BackStackEntry) -> Boolean) {
    entries.removeAll(predicate)
}

/**
 * Finds the index of the first entry with the given destination type.
 */
inline fun <reified D : Destination> BackStack.indexOfDestination(): Int {
    return entries.indexOfFirst { it.destination is D }
}

/**
 * Brings the first entry with the given destination type to the top.
 */
inline fun <reified D : Destination> BackStack.bringToFront(): Boolean {
    val index = indexOfDestination<D>()
    if (index < 0 || index == entries.lastIndex) return false
    move(index, entries.lastIndex)
    return true
}
```

---

## 6. Migration Guide

### 6.1 For Existing `BackStack` Users

**No changes required!** The interface remains backward compatible. Existing code using:
```kotlin
navigator.backStack.push(destination)
navigator.backStack.pop()
navigator.backStack.stack.collectAsState()
```
...continues to work identically.

### 6.2 For `StateBackStack` Users

**Migration path:**

Before:
```kotlin
val stateBackStack = rememberStateBackStack(HomeScreen)
StateNavHost(stateBackStack = stateBackStack) { destination ->
    // render
}
```

After:
```kotlin
val navigator = rememberNavigator(HomeScreen)
GraphNavHost(graph = myGraph, navigator = navigator)

// Direct manipulation now available:
navigator.backStack.entries.add(...)
navigator.backStack.insert(...)
```

### 6.3 Deprecation Timeline

1. **Phase 1 (Immediate):** Mark `StateBackStack` as `@Deprecated`
2. **Phase 2 (Next major version):** Remove `StateBackStack` and `StateNavHost`
3. **Migration aids:** Provide `typealias StateBackStack = MutableBackStack` temporarily

---

## 7. Testing Strategy

### 7.1 Unit Tests

```kotlin
class MutableBackStackTest {
    
    @Test
    fun `push adds entry to top`() {
        val backStack = MutableBackStack(TestScope())
        backStack.push(HomeScreen)
        backStack.push(DetailScreen)
        
        assertEquals(2, backStack.size)
        assertEquals(DetailScreen, backStack.current.value?.destination)
    }
    
    @Test
    fun `insert at index works correctly`() {
        val backStack = MutableBackStack(TestScope())
        backStack.push(HomeScreen)
        backStack.push(ListScreen)
        backStack.push(DetailScreen)
        
        backStack.insert(1, FilterScreen)
        
        assertEquals(4, backStack.size)
        assertEquals(FilterScreen, backStack.entries[1].destination)
        assertEquals(DetailScreen, backStack.current.value?.destination)
    }
    
    @Test
    fun `StateFlow updates synchronously with SnapshotStateList changes`() = runTest {
        val backStack = MutableBackStack(this)
        val collected = mutableListOf<Int>()
        
        backgroundScope.launch {
            backStack.stack.collect { collected.add(it.size) }
        }
        
        backStack.push(HomeScreen)
        advanceUntilIdle()
        backStack.push(DetailScreen)
        advanceUntilIdle()
        backStack.pop()
        advanceUntilIdle()
        
        assertEquals(listOf(0, 1, 2, 1), collected)
    }
    
    @Test
    fun `entries list is same instance as internal list`() {
        val backStack = MutableBackStack(TestScope())
        backStack.push(HomeScreen)
        
        // Direct manipulation should work
        backStack.entries.add(BackStackEntry.create(DetailScreen))
        
        assertEquals(2, backStack.size)
        assertEquals(2, backStack.stack.value.size)
    }
}
```

### 7.2 Animation System Integration Test

```kotlin
@Test
fun `GraphNavHost receives StateFlow updates for animation`() {
    // This test ensures animation system compatibility
    composeTestRule.setContent {
        val navigator = rememberNavigator(HomeScreen)
        
        GraphNavHost(
            graph = testGraph,
            navigator = navigator
        )
        
        LaunchedEffect(Unit) {
            // Direct manipulation should trigger animation
            navigator.backStack.insert(1, MiddleScreen)
        }
    }
    
    // Verify animation triggered correctly
    composeTestRule.onNodeWithTag("MiddleScreen").assertExists()
}
```

---

## 8. Performance Considerations

### 8.1 Memory

- **SnapshotStateList** - O(n) memory, same as List
- **StateFlow derivation** - Small overhead for snapshotFlow wrapper
- **derivedStateOf** - Memoized, only recomputes when dependencies change

### 8.2 CPU

- **snapshotFlow emission** - Only triggers when snapshot state actually changes
- **stateIn transformation** - Negligible overhead
- **List copying in toList()** - O(n) per emission, but necessary for immutable StateFlow

### 8.3 Optimization: Structural Sharing

For large backstacks, consider using `ImmutableList` from kotlinx.collections.immutable:

```kotlin
override val stack: StateFlow<List<BackStackEntry>> = snapshotFlow { 
    _entries.toImmutableList()  // Structural sharing for large lists
}.stateIn(...)
```

---

## 9. Summary

### 9.1 What Changes

| Aspect | Before | After |
|--------|--------|-------|
| `BackStack` interface | StateFlow-only | StateFlow + SnapshotStateList access |
| `MutableBackStack` internal | MutableStateFlow | SnapshotStateList + derived StateFlows |
| Direct entry manipulation | Not available | Full access via `entries` property |
| Nav3-style operations | Separate `StateBackStack` class | Unified in `BackStack` interface |

### 9.2 What Stays The Same

- `Navigator` interface
- `GraphNavHost` implementation
- Animation system behavior
- All existing navigation patterns
- StateFlow-based observation in UI

### 9.3 New Capabilities

- `insert(index, destination)` - Insert at position
- `removeAt(index)` - Remove by position
- `removeById(id)` - Remove by entry ID
- `swap(a, b)` - Swap entries
- `move(from, to)` - Move entry
- `entries` - Direct SnapshotStateList access
- `replaceAllWithEntries()` - State restoration

---

## 10. Next Steps

1. **Implement** revised `BackStack` interface
2. **Refactor** `MutableBackStack` to use SnapshotStateList
3. **Update** `DefaultNavigator` to pass scope
4. **Add** `rememberNavigator` composable helper
5. **Deprecate** `StateBackStack` and `StateNavHost`
6. **Write** comprehensive tests
7. **Update** documentation

---

*This design maintains architectural simplicity while enabling powerful new navigation patterns.*
