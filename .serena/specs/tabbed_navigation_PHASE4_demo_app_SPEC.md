# Phase 4: Demo App Refactoring

## Phase Overview

**Objective**: Refactor the `composeApp` demo application to showcase the new tabbed navigation API, replacing the manual workaround with the new library features.

**Scope**:
- Remove manual `BottomNavigationContainer` workaround
- Migrate bottom navigation to use `@TabGraph` annotations
- Demonstrate all tab navigation patterns
- Add comprehensive examples and documentation
- Ensure all existing demo patterns still work

**Timeline**: 2-3 days

**Dependencies**:
- Phase 1 (Core Foundation) ✅
- Phase 2 (Compose Integration) ✅
- Phase 3 (KSP Annotations) ✅

## Current State Analysis

### Files to Remove/Replace
1. ❌ `demo/ui/components/BottomNavigationContainer.kt` (manual workaround)
2. ❌ `demo/ui/components/MainContainer.kt` (manual container)
3. ✅ Keep `demo/ui/components/BottomNavigationBar.kt` (reusable component)

### Files to Modify
1. ✏️ `demo/destinations/Destinations.kt` - Add `@TabGraph` for MainDestination
2. ✏️ `demo/content/ContentDefinitions.kt` - Update content functions
3. ✏️ `demo/graphs/NavigationGraphs.kt` - Simplify with generated graphs
4. ✏️ Individual screen files - Remove dual-mode complexity

### New Files to Create
1. ➕ `demo/tabs/MainTabs.kt` - New `@TabGraph` definition
2. ➕ `demo/tabs/NestedTabsExample.kt` - Nested tabs demonstration
3. ➕ `demo/ui/screens/TabsDemo.kt` - Comprehensive tab examples

## Detailed Implementation Plan

### Step 1: Define Main Tab Graph with Annotations

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabs.kt`

**Purpose**: Replace manual bottom navigation with annotation-based tabs

```kotlin
package com.jermey.navplayground.demo.tabs

import com.jermey.navplayground.demo.destinations.*
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabGraph
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TabDefinition

/**
 * Main bottom navigation tabs for the Quo Vadis demo app.
 * 
 * Demonstrates the @TabGraph annotation with 4 tabs showcasing
 * different navigation patterns within each tab.
 * 
 * **Generated Code**:
 * - `MainTabsConfig`: Tab configuration
 * - `MainTabsContainer`: Composable container with bottom nav
 * - `buildMainTabsGraph()`: Graph builder function
 * 
 * Each tab contains its own independent navigation graph:
 * - **Home**: Navigation patterns showcase
 * - **Explore**: Master-detail pattern with deep navigation
 * - **Profile**: Profile management with editing
 * - **Settings**: App settings and theme management
 * 
 * ## Usage
 * 
 * ```kotlin
 * val navigator = rememberNavigator()
 * 
 * // Generated composable - includes bottom navigation bar
 * MainTabsContainer(parentNavigator = navigator)
 * ```
 * 
 * ## Back Press Behavior
 * 
 * 1. Pop from current tab's stack (if deeper than root)
 * 2. Switch to Home tab (if on another tab at root)
 * 3. Exit app (if on Home tab at root)
 */
@TabGraph(
    name = "main_tabs",
    initialTab = "Home",
    primaryTab = "Home"
)
sealed class MainTabs : TabDefinition {
    
    /**
     * Home tab - Navigation patterns showcase.
     * 
     * Contains cards demonstrating:
     * - Master-Detail navigation
     * - Nested tabs
     * - Process/Wizard flows
     * - Deep linking
     */
    @Tab(
        route = "main/home",
        label = "Home",
        icon = "home",
        rootGraph = HomeGraphDestination::class
    )
    data object Home : MainTabs() {
        override val id = "home"
        override val rootDestination = MainDestination.Home
    }
    
    /**
     * Explore tab - Master-Detail pattern.
     * 
     * Demonstrates:
     * - List with items
     * - Detail screens with deep navigation
     * - Shared element transitions
     * - Deep stack management
     */
    @Tab(
        route = "main/explore",
        label = "Explore",
        icon = "explore",
        rootGraph = ExploreGraphDestination::class
    )
    data object Explore : MainTabs() {
        override val id = "explore"
        override val rootDestination = MainDestination.Explore
    }
    
    /**
     * Profile tab - Profile management.
     * 
     * Demonstrates:
     * - Profile viewing
     * - Editing flows
     * - Form state preservation
     * - Navigation within settings
     */
    @Tab(
        route = "main/profile",
        label = "Profile",
        icon = "person",
        rootGraph = ProfileGraphDestination::class
    )
    data object Profile : MainTabs() {
        override val id = "profile"
        override val rootDestination = MainDestination.Profile
    }
    
    /**
     * Settings tab - App configuration.
     * 
     * Demonstrates:
     * - Settings screens
     * - Theme management (preserved across tabs)
     * - Modal navigation
     * - Simple navigation flows
     */
    @Tab(
        route = "main/settings",
        label = "Settings",
        icon = "settings",
        rootGraph = SettingsGraphDestination::class
    )
    data object Settings : MainTabs() {
        override val id = "settings"
        override val rootDestination = MainDestination.Settings
    }
}

// Helper sealed classes for organizing each tab's graph
// These are annotated with @Graph to generate their own navigation graphs

/**
 * Home tab's navigation graph.
 */
@Graph("home_tab")
sealed class HomeGraphDestination : Destination {
    // Home screen is the root
}

/**
 * Explore tab's navigation graph.
 */
@Graph("explore_tab")
sealed class ExploreGraphDestination : Destination {
    // Explore screen is the root
}

/**
 * Profile tab's navigation graph.
 */
@Graph("profile_tab")
sealed class ProfileGraphDestination : Destination {
    // Profile screen is the root
}

/**
 * Settings tab's navigation graph.
 */
@Graph("settings_tab")
sealed class SettingsGraphDestination : Destination {
    // Settings screen is the root
}
```

**Key Design Decisions**:
- Each tab gets its own graph sealed class
- Clear documentation of behavior
- Follows quo-vadis naming conventions
- Uses existing `MainDestination` as root destinations

---

### Step 2: Create Custom Tab Container with Bottom Navigation

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabsUI.kt`

**Purpose**: Custom UI for main tabs (bottom navigation bar)

```kotlin
package com.jermey.navplayground.demo.tabs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.ui.components.BottomNavigationBar
import com.jermey.quo.vadis.core.navigation.compose.TabbedNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberTabNavigator
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Main tab container with bottom navigation bar.
 * 
 * This demonstrates how to customize the generated tab container
 * by adding a bottom navigation bar.
 * 
 * The generated `MainTabsContainer` could be used directly, but we
 * wrap it here to add the bottom bar UI.
 */
@Composable
fun MainTabsScreen(
    parentNavigator: Navigator,
    modifier: Modifier = Modifier
) {
    val tabState = rememberTabNavigator(MainTabsConfig, parentNavigator)
    val selectedTab by tabState.selectedTab.collectAsState()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                currentRoute = selectedTab.id,
                onNavigate = { destination ->
                    // Find tab by route
                    val targetTab = MainTabsConfig.allTabs.find {
                        (it as? MainTabs)?.rootDestination == destination
                    }
                    if (targetTab != null) {
                        tabState.selectTab(targetTab)
                    } else {
                        // Navigate within current tab
                        tabState.navigateInTab(destination)
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        // Use generated TabbedNavHost
        TabbedNavHost(
            tabState = tabState,
            tabGraphs = mapOf(
                MainTabs.Home to buildHomeGraphDestinationGraph(),
                MainTabs.Explore to buildExploreGraphDestinationGraph(),
                MainTabs.Profile to buildProfileGraphDestinationGraph(),
                MainTabs.Settings to buildSettingsGraphDestinationGraph()
            ),
            parentNavigator = parentNavigator,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

**Alternative Approach**: If we want to use the generated container, we can pass the bottom bar as a parameter when we add that capability to the generator.

---

### Step 3: Simplify Screen Components

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/HomeScreen.kt`

**Changes**: Remove dual-mode support, simplify to single Scaffold

```kotlin
/**
 * Home screen - Navigation patterns showcase.
 * 
 * Displays cards for navigating to different demo patterns:
 * - Master-Detail navigation
 * - Nested tabs
 * - Process/Wizard flows
 * - Deep linking
 * 
 * Now simplified - always renders with its own top bar.
 * Bottom navigation is handled by MainTabsScreen container.
 */
@Composable
fun HomeScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quo Vadis Demo") },
                actions = {
                    IconButton(onClick = { /* Navigation drawer */ }) {
                        Icon(Icons.Default.Menu, "Menu")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Navigation Patterns",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(8.dp))
            }
            
            item {
                NavigationPatternCard(
                    title = "Master-Detail",
                    description = "List with detail screens and deep navigation",
                    icon = Icons.Default.List,
                    onClick = {
                        navigator.navigate(
                            MasterDetailDestination.List,
                            NavigationTransitions.SlideHorizontal
                        )
                    }
                )
            }
            
            item {
                NavigationPatternCard(
                    title = "Nested Tabs",
                    description = "Tabs within tabs with independent stacks",
                    icon = Icons.Default.Tab,
                    onClick = {
                        navigator.navigate(
                            TabsDestination.Main,
                            NavigationTransitions.SlideHorizontal
                        )
                    }
                )
            }
            
            item {
                NavigationPatternCard(
                    title = "Process Flow",
                    description = "Multi-step wizard with branching logic",
                    icon = Icons.Default.AccountTree,
                    onClick = {
                        navigator.navigate(
                            ProcessDestination.Start,
                            NavigationTransitions.SlideHorizontal
                        )
                    }
                )
            }
            
            item {
                NavigationPatternCard(
                    title = "Deep Linking",
                    description = "URI-based navigation examples",
                    icon = Icons.Default.Link,
                    onClick = {
                        navigator.navigate(
                            MainDestination.DeepLinkDemo,
                            NavigationTransitions.SlideHorizontal
                        )
                    }
                )
            }
        }
    }
}
```

**Remove Parameters**:
- ❌ `hideBottomBar: Boolean`
- ❌ `paddingValues: PaddingValues?`

**Apply Same Pattern to**:
- `ExploreScreen.kt`
- `ProfileScreen.kt`
- `SettingsScreen.kt`

---

### Step 4: Create Nested Tabs Demo

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/NestedTabsExample.kt`

**Purpose**: Demonstrate tabs within tabs (nested navigation)

```kotlin
package com.jermey.navplayground.demo.tabs

import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabGraph
import com.jermey.quo.vadis.core.navigation.core.Destination
import com.jermey.quo.vadis.core.navigation.core.TabDefinition

/**
 * Nested tabs example - demonstrates tabs within tabs.
 * 
 * This is accessed from the main app and contains its own
 * set of tabs with independent navigation stacks.
 * 
 * **Navigation Hierarchy**:
 * ```
 * App Root
 *   └─ Main Tabs (bottom nav)
 *       └─ Home Tab
 *           └─ Navigate to Nested Tabs Demo
 *               └─ Nested Tabs (top tab bar)
 *                   ├─ Tab A (with stack)
 *                   ├─ Tab B (with stack)
 *                   └─ Tab C (with stack)
 * ```
 * 
 * **Back Press Behavior**:
 * 1. Pop from current nested tab's stack
 * 2. Switch to primary nested tab (Tab A)
 * 3. Return to Home tab (parent navigator)
 */
@TabGraph(
    name = "nested_tabs",
    initialTab = "TabA",
    primaryTab = "TabA"
)
sealed class NestedTabs : TabDefinition {
    
    @Tab(
        route = "nested/tab_a",
        label = "Tab A",
        icon = "looks_one",
        rootGraph = NestedTabADestination::class
    )
    data object TabA : NestedTabs() {
        override val id = "nested_tab_a"
        override val rootDestination = TabsDestination.TabA
    }
    
    @Tab(
        route = "nested/tab_b",
        label = "Tab B",
        icon = "looks_two",
        rootGraph = NestedTabBDestination::class
    )
    data object TabB : NestedTabs() {
        override val id = "nested_tab_b"
        override val rootDestination = TabsDestination.TabB
    }
    
    @Tab(
        route = "nested/tab_c",
        label = "Tab C",
        icon = "looks_3",
        rootGraph = NestedTabCDestination::class
    )
    data object TabC : NestedTabs() {
        override val id = "nested_tab_c"
        override val rootDestination = TabsDestination.TabC
    }
}

@Graph("nested_tab_a")
sealed class NestedTabADestination : Destination

@Graph("nested_tab_b")
sealed class NestedTabBDestination : Destination

@Graph("nested_tab_c")
sealed class NestedTabCDestination : Destination
```

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/NestedTabsUI.kt`

```kotlin
package com.jermey.navplayground.demo.tabs

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.TabbedNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberTabNavigator
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Nested tabs UI with top tab bar (ScrollableTabRow).
 * 
 * Demonstrates:
 * - Tabs at a different level than main bottom nav
 * - Tab bar at top (different placement)
 * - Independent navigation within each nested tab
 * - Hierarchical back press (nested → main tabs → app)
 */
@Composable
fun NestedTabsScreen(
    parentNavigator: Navigator,
    modifier: Modifier = Modifier
) {
    val tabState = rememberTabNavigator(NestedTabsConfig, parentNavigator)
    val selectedTab by tabState.selectedTab.collectAsState()
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Nested Tabs Demo") },
                    navigationIcon = {
                        IconButton(onClick = { parentNavigator.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                )
                
                // Tab bar
                ScrollableTabRow(
                    selectedTabIndex = NestedTabsConfig.allTabs.indexOf(selectedTab)
                ) {
                    NestedTabsConfig.allTabs.forEach { tab ->
                        Tab(
                            selected = tab == selectedTab,
                            onClick = { tabState.selectTab(tab) },
                            text = { Text(tab.label ?: tab.id) }
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        TabbedNavHost(
            tabState = tabState,
            tabGraphs = mapOf(
                NestedTabs.TabA to buildNestedTabADestinationGraph(),
                NestedTabs.TabB to buildNestedTabBDestinationGraph(),
                NestedTabs.TabC to buildNestedTabCDestinationGraph()
            ),
            parentNavigator = parentNavigator,
            modifier = Modifier.padding(paddingValues)
        )
    }
}
```

---

### Step 5: Update Navigation Graphs

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/graphs/NavigationGraphs.kt`

**Changes**: Use generated tab graphs

```kotlin
/**
 * Root application navigation graph.
 * 
 * Now uses generated tab graphs for bottom navigation!
 * 
 * Structure:
 * - Main Tabs (generated from @TabGraph)
 *   - Home, Explore, Profile, Settings tabs
 * - Nested tabs demo (accessed from Home)
 * - Master-Detail flow
 * - Process/Wizard flow
 */
fun appRootGraph() = navigationGraph("app_root") {
    startDestination(MainDestination.Home)
    
    // Include main tabs graph (auto-generated)
    include(buildMainTabsGraph())
    
    // Include nested tabs graph (auto-generated)
    include(buildNestedTabsGraph())
    
    // Include other feature graphs
    include(buildMasterDetailDestinationGraph())
    include(buildProcessDestinationGraph())
    
    // Deep link demo (standalone screen)
    destination(MainDestination.DeepLinkDemo) { _, nav ->
        DeepLinkDemoScreen(nav)
    }
}
```

---

### Step 6: Update Main App Entry Point

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DemoApp.kt`

**Changes**: Update to use new tab screen

```kotlin
@Composable
fun DemoApp() {
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val appGraph = remember<NavigationGraph> { appRootGraph() }

    LaunchedEffect(navigator, appGraph) {
        navigator.registerGraph(appGraph)
        // Start at the main tabs screen
        navigator.setStartDestination(MainTabs.Home.rootDestination)
        setupDemoDeepLinks(navigator)
    }

    GraphNavHost(
        graph = appGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal,
        enablePredictiveBack = true
    )
}
```

---

### Step 7: Add Comprehensive Tab Demo Documentation

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/README.md`

n
# Tabbed Navigation Demo

This package demonstrates the Quo Vadis tabbed navigation API.

## Examples

### 1. Main Bottom Navigation (`MainTabs`)

**File**: `MainTabs.kt`

Four tabs with independent navigation stacks:
- **Home**: Navigation patterns showcase
- **Explore**: Master-detail with deep stacks
- **Profile**: Profile management flows
- **Settings**: App configuration

**Features**:
- Bottom navigation bar
- State preservation across tab switches
- Intelligent back press (pop → switch to home → exit)
- Deep linking to tab content

**Usage**:
```kotlin
MainTabsScreen(parentNavigator = navigator)
```

### 2. Nested Tabs (`NestedTabs`)

**File**: `NestedTabsExample.kt`

Three nested tabs demonstrating tabs within tabs:
- **Tab A**: Simple content with navigation
- **Tab B**: Form with state preservation
- **Tab C**: List with detail navigation

**Features**:
- Top tab bar (ScrollableTabRow)
- Hierarchical back press delegation
- Independent stacks per nested tab
- Accessible from main tabs

**Usage**:
```kotlin
NestedTabsScreen(parentNavigator = navigator)
```

## Architecture

### Navigation Hierarchy

```
App Root Navigator
  └─ Main Tabs Screen (TabNavigator)
      ├─ Home Tab Navigator
      │   ├─ Home Screen (root)
      │   ├─ Master-Detail Flow
      │   ├─ Nested Tabs Screen (TabNavigator)
      │   │   ├─ Tab A Navigator
      │   │   ├─ Tab B Navigator
      │   │   └─ Tab C Navigator
      │   └─ Process Flow
      ├─ Explore Tab Navigator
      │   ├─ List Screen (root)
      │   └─ Detail Screens (stack)
      ├─ Profile Tab Navigator
      │   ├─ Profile Screen (root)
      │   └─ Edit Screens (stack)
      └─ Settings Tab Navigator
          └─ Settings Screen (root)
```

### Back Press Flow

```
User presses back
  ↓
Active Navigator receives event
  ↓
If Nested Tabs active:
  ├─ Pop from nested tab stack? → DONE
  ├─ Switch to primary nested tab? → DONE
  └─ Return false (pass to parent)
      ↓
Main Tabs Navigator:
  ├─ Pop from current tab stack? → DONE
  ├─ Switch to Home tab? → DONE
  └─ Return false (pass to parent)
      ↓
App Root Navigator:
  └─ Exit app
```

## Code Generation

All tab containers use `@TabGraph` annotation for code generation:

**Input**:
```kotlin
@TabGraph("main_tabs")
sealed class MainTabs : TabDefinition {
    @Tab(...) data object Home : MainTabs()
    @Tab(...) data object Profile : MainTabs()
}
```

**Generated**:
- `MainTabsConfig`: TabNavigatorConfig
- `MainTabsContainer`: @Composable container
- `buildMainTabsGraph()`: NavigationGraph builder

**Benefit**: ~87% less boilerplate code!

## Testing

Test tab navigation with:

```kotlin
val fakeTabNavigator = FakeTabNavigator(MainTabsConfig)

// Select tab
fakeTabNavigator.selectTab(MainTabs.Profile)
assertEquals(MainTabs.Profile, fakeTabNavigator.currentTab)

// Navigate within tab
fakeTabNavigator.navigateInTab(ProfileDestination.Edit)
assertEquals(2, fakeTabNavigator.getTabStack(MainTabs.Profile).size)

// Back press
assertTrue(fakeTabNavigator.onBack()) // pops from tab
assertTrue(fakeTabNavigator.onBack()) // switches to home
assertFalse(fakeTabNavigator.onBack()) // doesn't consume (at home root)
```

## Best Practices

1. **Use @TabGraph**: Don't manually create tab configurations
2. **State Preservation**: Tabs automatically preserve state
3. **Hierarchical Back**: Trust the delegation chain
4. **Deep Linking**: Use tab routes for deep links
5. **Testing**: Use FakeTabNavigator for unit tests

## Migration from Manual Implementation

**Before** (manual container):
```kotlin
// 150+ lines of boilerplate
sealed class Tab { ... }
val config = TabNavigatorConfig(...)
fun Container() { ... }
```

**After** (annotations):
```kotlin
// ~20 lines
@TabGraph("main")
sealed class MainTabs : TabDefinition {
    @Tab(...) data object Home : MainTabs()
}
```

See `bottom_navigation_state_retention` memory for the old manual approach.
```

---

### Step 8: Update Demo README

**File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/README.md`

**Add section**:

n
## Tabbed Navigation

The demo app now uses the Quo Vadis tabbed navigation API!

### Main Bottom Navigation

Four tabs with independent navigation stacks:
- **Home**: Navigation patterns showcase
- **Explore**: Master-detail pattern
- **Profile**: Profile management
- **Settings**: App settings

**Implementation**: See `tabs/MainTabs.kt`

### Nested Tabs

Tabs within tabs, accessible from Home screen.

**Implementation**: See `tabs/NestedTabsExample.kt`

### Benefits

- ✅ 87% less boilerplate code
- ✅ Automatic state preservation
- ✅ Type-safe navigation
- ✅ Intelligent back press handling
- ✅ Full KSP code generation

### Migration

The old manual implementation (using `BottomNavigationContainer`) has been
replaced with the new annotation-based API. See commit history for migration example.
```

---

## Verification Checklist

### Functional Testing
- [ ] Main bottom navigation works
- [ ] Tab switching preserves state (scroll positions, inputs)
- [ ] Back press behaves correctly:
  - [ ] Pops from current tab stack
  - [ ] Switches to Home tab
  - [ ] Exits app at Home root
- [ ] Nested tabs demo works
- [ ] Deep linking to tabs works
- [ ] All existing demos still work (master-detail, process, etc.)

### Visual Testing
- [ ] Bottom navigation bar displays correctly
- [ ] Tab transitions are smooth (fade animations)
- [ ] No visual glitches when switching tabs
- [ ] Top app bar updates correctly per screen
- [ ] Nested tabs show top tab bar correctly

### Code Quality
- [ ] No manual workaround code remains
- [ ] Screens simplified (no dual-mode)
- [ ] Clear documentation
- [ ] Follows quo-vadis conventions
- [ ] KSP generates correct code

### Performance
- [ ] Tab switching < 16ms (60fps)
- [ ] No memory leaks with repeated tab switches
- [ ] Smooth animations with 4 tabs in memory
- [ ] App start time not affected

---

## Files Summary

**Files to Delete**:
```
❌ demo/ui/components/BottomNavigationContainer.kt
❌ demo/ui/components/MainContainer.kt
```

**New Files**:
```
➕ demo/tabs/MainTabs.kt                (~200 lines)
➕ demo/tabs/MainTabsUI.kt              (~80 lines)
➕ demo/tabs/NestedTabsExample.kt       (~150 lines)
➕ demo/tabs/NestedTabsUI.kt            (~100 lines)
➕ demo/tabs/README.md                  (documentation)
```

**Modified Files**:
```
✏️ demo/DemoApp.kt                      (minor updates)
✏️ demo/graphs/NavigationGraphs.kt     (use generated graphs)
✏️ demo/ui/screens/HomeScreen.kt       (simplify - remove ~50 lines)
✏️ demo/ui/screens/ExploreScreen.kt    (simplify - remove ~50 lines)
✏️ demo/ui/screens/ProfileScreen.kt    (simplify - remove ~50 lines)
✏️ demo/ui/screens/SettingsScreen.kt   (simplify - remove ~50 lines)
✏️ demo/README.md                       (add tab navigation section)
```

**Net Change**: ~+330 lines, -450 lines = **-120 lines** (cleaner code!)

---

## Risks & Mitigation

### Risk: Breaking Existing Demos
**Mitigation**:
- Test all navigation patterns after refactor
- Keep git history for rollback if needed
- Gradual migration (tabs first, then screens)

### Risk: Generated Code Issues
**Mitigation**:
- Verify generated code compiles before using
- Add logging to KSP processor
- Test with simple examples first

### Risk: Performance Regression
**Mitigation**:
- Profile before and after refactor
- Measure tab switching time
- Monitor memory usage

---

## Verification Steps

After implementation:

1. **Clean Build**: `./gradlew clean :composeApp:assembleDebug`
2. **Run App**: Test on Android device/emulator
3. **Test Tabs**: Switch between all 4 main tabs multiple times
4. **Test State**: Scroll in a tab, switch away, return (verify scroll preserved)
5. **Test Back Press**: Navigate deep in tab, test back press behavior
6. **Test Nested Tabs**: Access from Home, verify nested behavior
7. **Test Existing**: Verify master-detail, process flows still work
8. **Performance**: Profile tab switching (target <16ms)

---

**Status**: 🔴 Not Started

**Next Phase**: Phase 5 - Documentation & Testing

**Depends On**: 
- Phase 1 (Core Foundation) ✅
- Phase 2 (Compose Integration) ✅
- Phase 3 (KSP Annotations) ✅
