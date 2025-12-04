# Tabbed Navigation + Predictive Back Issues Analysis

## Executive Summary

Two interconnected bugs occur when using predictive back animation to navigate from a normal screen back to a tabbed navigator:

1. **Visual Glitch**: Screen blinks briefly during the predictive back transition
2. **Tab Selection Lost**: Previously selected tab is not preserved after navigating back

## Bug Reproduction Scenario

```
Home tab → Switch to Explore tab → Open item from Explore list → Navigate back → Returns to Home tab instead of Explore tab
```

---

## Architecture Context

### Navigation Hierarchy

```
DemoApp (appRootGraph)
└── GraphNavHost (parent navigator)
    ├── AppDestination.MainTabs → MainTabsScreen
    │   └── TabbedNavHost (tab navigators)
    │       ├── Home tab → GraphNavHost (tabContentGraph)
    │       ├── Explore tab → GraphNavHost (tabContentGraph)  
    │       ├── Profile tab → GraphNavHost (tabContentGraph)
    │       └── Settings tab → GraphNavHost (tabContentGraph)
    │
    └── MasterDetailDestination.Detail (renders ON TOP of MainTabs in parent backstack)
```

### Key Files Involved

**Demo App:**
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/graphs/AppRootGraph.kt`
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/MainTabsUI.kt`
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/MainTabsConfig.kt`
- `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/tabs/TabScopedNavigator.kt`

**Core Library:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/GraphNavHost.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabbedNavHost.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabNavigationContainer.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/RememberTabNavigation.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabNavigatorState.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/ComposableCache.kt`

---

## Bug 1: Tab Selection Not Preserved

### Root Cause Analysis

When the user navigates from the Explore tab to a Detail screen:

1. **Navigation Flow:**
   ```kotlin
   // In TabScopedNavigator.navigate() - line ~77
   if (isDestinationInTabGraph(destination)) {
       backStack.push(destination, transition)  // Within tab
   } else {
       delegate.navigate(destination, transition)  // Delegates to PARENT navigator
   }
   ```

2. **`MasterDetailDestination.Detail`** is NOT in `tabContentGraph`, so navigation is delegated to the parent navigator

3. **Parent navigator pushes Detail on top of MainTabs** - MainTabs remains as the previous entry in the parent's backstack

4. **When navigating back:**
   - Parent navigator pops `Detail`
   - Shows `MainTabs` again
   - `MainTabsScreen` may be **recomposed fresh**

### The State Restoration Problem

**In `RememberTabNavigation.kt` (lines 55-72):**

```kotlin
private fun tabNavigatorStateSaver(config: TabNavigatorConfig): Saver<TabNavigatorState, List<Any>> {
    return Saver(
        save = { state -> listOf(state.selectedTab.value.route) },
        restore = { saved ->
            val selectedTabRoute = saved[0] as String
            val selectedTab = config.allTabs.first { it.route == selectedTabRoute }
            // Creates a NEW TabNavigatorState instance
            TabNavigatorState(config).apply {
                selectTab(selectedTab)  // Then switches tab
            }
        }
    )
}
```

**Problem #1: New instance created on restore**
- The `restore` function creates a NEW `TabNavigatorState`
- The `TabNavigatorState` constructor initializes with `config.initialTab` (Home)
- Then `selectTab()` is called, but there may be a frame where initial state shows

**Problem #2: rememberSaveable key instability**
- When MainTabs exits composition (Detail is pushed), its saveable state persistence depends on composition identity
- The `rememberSaveable` may not find the same saved value because composable identity changes

**Problem #3: Tab state NOT stored in BackStackEntry**
- The parent navigator only knows about `AppDestination.MainTabs` as an entry
- It doesn't store which tab was selected when navigating away
- Tab state lives inside the composable, not in the navigation state

---

## Bug 2: Visual Glitch (Screen Blinking)

### Root Cause Analysis

**In `GraphNavHost.kt` (lines 207-215):**

```kotlin
// Render previous screen during predictive gesture
if (isPredictiveGesture && displayedPrevious != null && displayedPrevious.id != displayedCurrent?.id) {
    ScreenContent(
        entry = displayedPrevious,  // <-- This renders MainTabsScreen fresh
        graph = graph,
        navigator = navigator,
        composableCache = composableCache,
        saveableStateHolder = saveableStateHolder,
        enableCache = enableCache
    )
}
```

### Why the blink occurs:

1. **During predictive back from Detail to MainTabs:**
   - `displayedPrevious` = `AppDestination.MainTabs` entry
   - `ScreenContent` renders `MainTabsContent` → `MainTabsScreen`

2. **MainTabsScreen contains TabbedNavHost which:**
   - Creates/restores `TabNavigatorState` (may default to Home initially)
   - Renders `TabNavigationContainer` with all tabs
   - Each tab has its own `GraphNavHost`

3. **TabNavigationContainer uses AnimatedVisibility (lines 81-89):**
   ```kotlin
   AnimatedVisibility(
       visible = visible,
       enter = transitionSpec.enter,  // fadeIn(300ms)
       exit = transitionSpec.exit
   ) {
       content()
   }
   ```

4. **The blink happens because:**
   - `MainTabsScreen` is freshly entering composition during the gesture
   - `rememberTabNavigator` may restore to wrong tab initially
   - Nested `GraphNavHost` instances are being set up fresh
   - The fadeIn animation starts from 0% opacity → visual gap
   - There's a frame or two where content flashes

### Contributing Factors:

1. **Independent cache hierarchies:**
   - Parent `GraphNavHost` has its cache for MainTabsScreen
   - Each tab's `GraphNavHost` has separate cache
   - When MainTabsScreen re-enters composition, tab caches need time to warm up

2. **Race condition potential:**
   - Parent starts predictive back animation
   - Tab content is being composed simultaneously
   - Both navigators may animate independently

---

## State Flow Diagram

```
┌──────────────────────────────────────────────────────────────────┐
│ User on Explore Tab viewing item list                            │
│ TabNavigatorState.selectedTab = ExploreTab                       │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼ User taps item
┌──────────────────────────────────────────────────────────────────┐
│ TabScopedNavigator.navigate(MasterDetailDestination.Detail)      │
│   → isDestinationInTabGraph(Detail) = FALSE                      │
│   → delegate.navigate(Detail) [parent navigator]                 │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼ Parent pushes Detail
┌──────────────────────────────────────────────────────────────────┐
│ Parent BackStack: [MainTabs, Detail]                             │
│ MainTabsScreen exits composition (or remains cached)             │
│ Detail screen renders on top                                     │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼ User initiates predictive back
┌──────────────────────────────────────────────────────────────────┐
│ PredictiveBackHandler triggered                                  │
│   → capturedPrevious = MainTabs entry                            │
│   → capturedCurrent = Detail entry                               │
│   → Both locked in cache                                         │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼ Render previous screen
┌──────────────────────────────────────────────────────────────────┐
│ ScreenContent(entry = MainTabs)                                  │
│   → MainTabsScreen composed                                      │
│   → rememberTabNavigator() called                                │
│   → Saver.restore() creates NEW TabNavigatorState                │
│   → May restore to wrong tab or flash initial tab                │
│   → TabbedNavHost → TabNavigationContainer                       │
│   → AnimatedVisibility fadeIn(300ms) causes blink                │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼ Back navigation completes
┌──────────────────────────────────────────────────────────────────┐
│ User sees Home tab instead of Explore tab                        │
│ OR sees brief flash/blink of wrong content                       │
└──────────────────────────────────────────────────────────────────┘
```

---

## Summary of Issues

| Issue | Root Cause | Location |
|-------|------------|----------|
| Tab not preserved | Tab state not stored in BackStackEntry, only in composable | `RememberTabNavigation.kt`, `TabNavigatorState.kt` |
| Tab not preserved | Saver creates new instance with initial tab first | `RememberTabNavigation.kt:63-71` |
| Tab not preserved | rememberSaveable key may change on recomposition | `MainTabsUI.kt:60` |
| Visual blink | MainTabsScreen freshly composed during gesture | `GraphNavHost.kt:207-215` |
| Visual blink | AnimatedVisibility fadeIn animation | `TabNavigationContainer.kt:81-89` |
| Visual blink | Nested GraphNavHost caches need warm-up time | Cache architecture |

---

## Related Documentation

- `quo-vadis-core/docs/ARCHITECTURE.md`
- `quo-vadis-core/docs/MULTIPLATFORM_PREDICTIVE_BACK.md`
- `TABBED_NAV_PREDICTIVE_BACK_FIX_PLAN.md` (implementation plan)
