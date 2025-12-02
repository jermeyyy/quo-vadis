# Cross-Navigator Animation Fix Plan

## Problem Statement

When navigating between separate navigators (parent navigator and tabbed navigator), animations break because each navigator maintains its own independent backstack. The animation system relies on `displayedCurrent` and `displayedPrevious` entries from a single backstack, but cross-navigator navigation involves entries from different backstacks.

### Scenario: Navigate from Tab to Parent Destination

1. User is on **Explore tab** (inside `TabbedNavHost`, using `TabScopedNavigator`)
2. User clicks on an item to navigate to **Detail screen** (outside tab graph)
3. `TabScopedNavigator.navigate()` detects destination is NOT in tab graph
4. Delegates to **parent navigator**: `delegate.navigate(Detail)`
5. Parent navigator's backstack changes: `[MainTabs]` → `[MainTabs, Detail]`

**Animation problem:**
- Parent's `GraphNavHost` sees stack change and sets:
  - `displayedCurrent = Detail entry`
  - `displayedPrevious = MainTabs entry`
- Parent renders MainTabs (which contains TabbedNavHost with Explore tab selected)
- BUT: MainTabs is rendered from cache/SaveableStateHolder with **correct Explore state**
- **HOWEVER**: The TabScopedNavigator's backstack still has Explore as current
- When MainTabs recomposes during animation, it may briefly show wrong tab state

### Scenario: Navigate Back from Parent to Tab

1. User is on **Detail screen** (parent navigator stack: `[MainTabs, Detail]`)
2. User performs predictive back gesture
3. Parent navigator handles gesture, pops Detail
4. Parent's `displayedPrevious = MainTabs entry`
5. MainTabs is rendered and should show Explore tab (where user was before)

**Animation problem:**
- MainTabs renders during animation via `SaveableStateHolder`
- Tab state should be preserved via entry extras
- But tab state restoration depends on `parentEntry` which may not be properly propagated

---

## Root Cause Analysis

### Issue 1: Dual Backstack Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│ Parent Navigator (DefaultNavigator)                              │
│ Backstack: [MainTabs, Detail]                                   │
│ current: Detail                                                  │
│ previous: MainTabs                                               │
├─────────────────────────────────────────────────────────────────┤
│ Tab Navigator (TabScopedNavigator for Explore tab)              │
│ Backstack: [ExploreRoot, ExploreList]  (separate!)              │
│ current: ExploreList                                             │
│ previous: ExploreRoot                                            │
└─────────────────────────────────────────────────────────────────┘
```

When delegating navigation to parent:
- Tab navigator's backstack is **unchanged**
- Parent navigator's backstack is **updated**
- Animation system only sees parent's backstack
- Tab's internal state (which tab was selected) must be preserved separately

### Issue 2: Entry State Synchronization

The `MainTabs` entry in parent's backstack has `extras` map containing:
- `EXTRA_SELECTED_TAB_ROUTE = "explore"` (saved when user switched to Explore)

During animation rendering:
- `MainTabsContent` needs this entry to pass to `MainTabsScreen`
- `TabNavigatorState` initializes from `parentEntry.getExtra(EXTRA_SELECTED_TAB_ROUTE)`
- If wrong entry is passed, wrong tab is shown

### Issue 3: LocalBackStackEntry Fix (Already Implemented)

We added `LocalBackStackEntry` to provide the correct entry during animation:
```kotlin
CompositionLocalProvider(LocalBackStackEntry provides stackEntry) {
    config.content(stackEntry.destination, navigator)
}
```

And updated `MainTabsContent`:
```kotlin
val renderingEntry = currentBackStackEntry()
renderingEntry?.let { entry ->
    MainTabsScreen(parentNavigator = navigator, parentEntry = entry)
}
```

**This should work**, but there may be additional issues.

---

## Potential Remaining Issues

### Issue A: TabNavigatorState Key Not Updating

In `RememberTabNavigation.kt`:
```kotlin
val tabState = remember(config, parentEntry?.id) {
    TabNavigatorState(config, parentEntry)
}
```

If `parentEntry?.id` doesn't change during animation (same MainTabs entry), the `TabNavigatorState` is reused. This is correct. But if `remember` returns a cached instance that wasn't initialized with extras, it may use initial tab.

### Issue B: SaveableStateHolder Caching

The `ComposableCache.Entry()` uses `SaveableStateHolder.SaveableStateProvider(entryId)`:
```kotlin
saveableStateHolder.SaveableStateProvider(entryId) {
    content(entry)
}
```

During animation:
1. Forward navigation: MainTabs is rendered as `initialState` (exiting)
2. MainTabs composable is already in SaveableStateHolder with previous composition
3. New `LocalBackStackEntry` value may not propagate if composition is reused

**Potential fix**: Ensure `LocalBackStackEntry` is provided INSIDE the SaveableStateProvider.

### Issue C: Animation Content Recomposition

In `AnimatedContent`:
```kotlin
AnimatedContent(targetState = entry) { animatingEntry ->
    ScreenContent(entry = animatingEntry, ...)
}
```

The `animatingEntry` is passed to `ScreenContent`, which then provides it via `LocalBackStackEntry`. BUT if the content is cached and not recomposing, the old value may be used.

---

## Proposed Solutions

### Solution 1: Ensure LocalBackStackEntry Propagates Through Cache

Move the `CompositionLocalProvider` INSIDE the cache's `SaveableStateProvider`:

```kotlin
// In ComposableCache.Entry()
saveableStateHolder.SaveableStateProvider(entryId) {
    // Entry should already be provided by caller, but re-provide to ensure it's current
    content(entry)
}
```

The issue is that `LocalBackStackEntry` is set in `ScreenContent` BEFORE calling cache, but the cached content may not see the updated value.

**Fix approach**: Move `LocalBackStackEntry` provider to be the outermost wrapper in `ScreenContent`:

```kotlin
@Composable
private fun ScreenContent(entry: BackStackEntry, ...) {
    // Provide entry at the TOP level, before cache lookup
    CompositionLocalProvider(LocalBackStackEntry provides entry) {
        // ... existing destConfig lookup and rendering
    }
}
```

### Solution 2: Force Tab State Recalculation During Animation

Instead of relying on `remember(parentEntry?.id)`, use a key that includes animation state:

```kotlin
// In rememberTabNavigator
val tabState = remember(config, parentEntry?.id, parentEntry?.getExtra(EXTRA_SELECTED_TAB_ROUTE)) {
    TabNavigatorState(config, parentEntry)
}
```

This ensures TabNavigatorState is recreated if the selected tab route changes.

### Solution 3: Synchronize Tab Selection Before Animation

When delegating navigation from tab to parent, ensure tab state is synced:

```kotlin
// In TabScopedNavigator.navigate()
if (!isDestinationInTabGraph(destination)) {
    // Sync current tab state to parent entry BEFORE delegating
    // This ensures MainTabs entry has correct tab route in extras
    // (Already done via TabNavigatorState.selectTab -> parentEntry.setExtra)
    delegate.navigate(destination, transition)
}
```

The sync is already happening, but we need to ensure it's read correctly.

### Solution 4: Cross-Navigator Animation Coordinator

Create a coordinator that bridges animation state between navigators:

```kotlin
class CrossNavigatorAnimationCoordinator {
    // Track which navigator is currently animating
    var activeAnimatingNavigator: Navigator? = null
    
    // Store the "virtual previous" for cross-navigator back
    var crossNavigatorPrevious: BackStackEntry? = null
    
    fun prepareForCrossNavigation(from: Navigator, to: Navigator, entry: BackStackEntry) {
        crossNavigatorPrevious = from.backStack.current.value
    }
}
```

This is more complex but provides explicit control over cross-navigator animations.

---

## Recommended Implementation Order

1. **First**: Verify `LocalBackStackEntry` is being read correctly
   - Add debug logging to confirm entry.id during animation
   - Check if entry.getExtra() returns correct tab route

2. **Second**: Move `LocalBackStackEntry` provider to top of `ScreenContent`
   - Ensures it wraps everything including cache

3. **Third**: Add defensive tab state initialization
   - In `TabNavigatorState` constructor, log restoration details
   - Ensure `parentEntry` is not null when expected

4. **Fourth**: Consider key-based recomposition
   - If issues persist, add animation-aware keys to force recomposition

---

## Already Implemented Fixes (Context)

### Fix 1: LocalPredictiveBackInProgress Provider
**File**: `GraphNavHost.kt`
- Added `CompositionLocalProvider(LocalPredictiveBackInProgress provides (isPredictiveGesture || justCompletedGesture))`
- Allows `TabContent` to skip animations during predictive back

### Fix 2: Debug Logging in TabNavigatorState
**File**: `TabNavigatorState.kt`
- Added logging in `_selectedTab` initialization
- Added logging in `selectTab()` method

### Fix 3: Deprecated Legacy Saver
**File**: `RememberTabNavigation.kt`
- Deprecated `rememberTabNavigatorState()` (doesn't support parentEntry)
- Recommended `rememberTabNavigator()` instead

### Fix 4: Extras Serialization
**File**: `StateSerializer.kt`
- Added `extras` field to `SerializableBackStackEntry`
- Enables tab state preservation across process death

### Fix 5: LocalBackStackEntry
**File**: `GraphNavHost.kt`
- Created `LocalBackStackEntry` CompositionLocal
- Provides correct entry during animation rendering
- Updated to wrap content with provider

### Fix 6: MainTabsContent Update
**File**: `ContentDefinitions.kt`
- Changed from `navigator.backStack.current.collectAsState()` to `currentBackStackEntry()`
- Ensures correct entry is used during animations

---

## Next Steps for Subagent

1. **Investigate**: Run the app and add logging to trace:
   - What entry ID is being passed to MainTabsContent during forward navigation animation
   - What tab route is in the entry's extras
   - Whether TabNavigatorState is correctly restoring the tab

2. **Apply Solution 1**: Move `LocalBackStackEntry` provider to wrap entire `ScreenContent` body

3. **Test**: Verify the flow:
   - Home → Explore → tap item → Detail (should not show Home flash)
   - Detail → back → Explore (should land on Explore, not Home)

4. **If issues persist**: Implement Solution 2 (key-based recomposition)

---

## Debug Logging Points

Add these logs to trace the issue:

```kotlin
// In ScreenContent
println("DEBUG_ANIM: ScreenContent entry.id=${entry.id}, dest=${entry.destination.route}")

// In MainTabsContent  
println("DEBUG_ANIM: MainTabsContent renderingEntry.id=${renderingEntry?.id}")
println("DEBUG_ANIM: MainTabsContent extras=${renderingEntry?.getExtra(EXTRA_SELECTED_TAB_ROUTE)}")

// In TabNavigatorState init
println("DEBUG_ANIM: TabNavigatorState init parentEntry.id=${parentEntry?.id}")
println("DEBUG_ANIM: TabNavigatorState init restoredTab=${restoredTab?.route}")
```
