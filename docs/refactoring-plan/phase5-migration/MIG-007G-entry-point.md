# MIG-007G: App Entry Point Integration

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-007G |
| **Parent Task** | [MIG-007](./MIG-007-demo-app-rewrite.md) |
| **Complexity** | High |
| **Estimated Time** | 3-4 hours |
| **Dependencies** | MIG-007A through MIG-007F |
| **Output** | Fully migrated app entry point with new architecture |

## Objective

Replace the **manual navigation initialization** in `DemoApp.kt` with the new KSP-generated NavTree architecture. This is the **FINAL integration step** that:

1. Removes `initializeQuoVadisRoutes()` call
2. Replaces manual `appRootGraph()` with KSP-generated `buildMainTabsNavNode()`
3. Switches from `GraphNavHost` to `QuoVadisHost`
4. Implements the `TabWrapper` pattern for scaffold control
5. Configures `AnimationRegistry` for transitions
6. Deletes or simplifies `NavigationGraphs.kt`

**IMPORTANT:** This subtask focuses ONLY on the new architecture. NO backward compatibility is maintained.

---

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/
├── DemoApp.kt              # PRIMARY: Complete rewrite
├── graphs/
│   └── NavigationGraphs.kt # DELETE or simplify significantly
```

### Files Created by Dependencies

| Dependency | Generated/Modified Files |
|------------|-------------------------|
| MIG-007A | Destination classes with `@Stack`, `@Destination`, `@Argument` |
| MIG-007B | Tab container with `@Tab`, `@TabItem`, per-tab stacks |
| MIG-007C | MasterDetailDestination with proper annotations |
| MIG-007D | ProcessDestination flow |
| MIG-007E | SettingsDestination stack |
| MIG-007F | All `@Screen` bindings |

### Reference Code

| File | Purpose |
|------|---------|
| [BottomTabsRecipe.kt](../../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/tabs/BottomTabsRecipe.kt) | TabWrapper pattern reference |
| [MIG-007B](./MIG-007B-tab-system.md) | Tab system architecture |

---

## Migration Steps

### Step 1: Rewrite DemoApp.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DemoApp.kt`

#### 1.1 Complete Before/After Transformation

```kotlin
// =====================================================================
// OLD: Manual initialization with GraphNavHost
// =====================================================================
package com.jermey.navplayground.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.jermey.navplayground.demo.destinations.AppDestination
import com.jermey.navplayground.demo.destinations.initializeQuoVadisRoutes
import com.jermey.navplayground.demo.graphs.appRootGraph
import com.jermey.quo.vadis.core.navigation.compose.GraphNavHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions

@Composable
fun DemoApp() {
    // Initialize auto-generated route registrations
    remember { initializeQuoVadisRoutes() }
    
    val navigator = rememberNavigator()
    val appGraph = remember<NavigationGraph> { appRootGraph() }

    LaunchedEffect(navigator, appGraph) {
        navigator.registerGraph(appGraph)
        navigator.setStartDestination(AppDestination.MainTabs)
    }

    GraphNavHost(
        graph = appGraph,
        navigator = navigator,
        defaultTransition = NavigationTransitions.SlideHorizontal,
        enablePredictiveBack = true
    )
}

// =====================================================================
// NEW: KSP-generated tree + QuoVadisHost + TabWrapper
// =====================================================================
package com.jermey.navplayground.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.navplayground.demo.tabs.MainTabs
import com.jermey.navplayground.demo.tabs.buildMainTabsNavNode  // KSP-generated
import com.jermey.navplayground.demo.ui.components.DemoBottomNavigationBar
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.compose.rememberNavigator
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapper
import com.jermey.quo.vadis.core.navigation.core.AnimationRegistry
import com.jermey.quo.vadis.core.navigation.core.Navigator

/**
 * Main entry point for the demo application.
 *
 * Architecture (New):
 * - KSP generates `buildMainTabsNavNode()` from `@Tab` annotations
 * - `QuoVadisHost` renders the navigation tree
 * - `TabWrapper` provides user-controlled scaffold with bottom navigation
 * - `GeneratedScreenRegistry` maps destinations to composables
 * - `AnimationRegistry` configures per-route transitions
 *
 * Navigation Structure:
 * ```
 * MainTabs (TabNode)
 * ├── Home (StackNode) → HomeDestination screens
 * ├── Explore (StackNode) → ExploreDestination screens
 * ├── Profile (StackNode) → ProfileDestination screens
 * └── Settings (StackNode) → SettingsDestination screens
 *
 * Full-screen overlays (render above tabs):
 * ├── MasterDetail flow
 * ├── Process wizard flow
 * ├── DeepLink demo
 * └── StateDriven demo
 * ```
 */
@Composable
fun DemoApp() {
    // Step 1: Build navigation tree from KSP-generated function
    val navTree = remember { buildMainTabsNavNode() }
    
    // Step 2: Create navigator with the tree
    val navigator = rememberNavigator(navTree)
    
    // Step 3: Configure animations (optional, can be inline)
    val animations = remember { demoAnimationRegistry() }
    
    // Step 4: Render with QuoVadisHost
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,  // KSP-generated
        animationRegistry = animations,
        tabWrapper = demoTabWrapper(navigator),
        enablePredictiveBack = true
    )
}

/**
 * Creates the TabWrapper for the demo app's bottom navigation.
 *
 * The TabWrapper pattern gives YOU full control over the scaffold structure:
 * - You control `Scaffold`, `TopAppBar`, `BottomBar`, `FAB`, etc.
 * - Library renders `tabContent()` with the active tab's screen
 * - `TabWrapperScope` provides `activeTabIndex`, `tabMetadata`, `switchTab()`
 *
 * @param navigator Navigator instance for full-screen navigation
 * @return TabWrapper for QuoVadisHost
 */
fun demoTabWrapper(navigator: Navigator): TabWrapper = { tabContent ->
    // 'this' is TabWrapperScope
    Scaffold(
        bottomBar = {
            DemoBottomNavigationBar(
                activeTabIndex = activeTabIndex,
                tabMetadata = tabMetadata,
                onTabSelected = { index -> switchTab(index) },
                isTransitioning = isTransitioning
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Library renders active tab's content here
            tabContent()
        }
    }
}

/**
 * Animation registry for demo app transitions.
 *
 * Configures per-route and per-pattern animations:
 * - Slide horizontal for most navigation
 * - Slide vertical for modals/bottom sheets
 * - Fade for same-level transitions within tabs
 */
fun demoAnimationRegistry(): AnimationRegistry = AnimationRegistry {
    // Default transition for all routes
    default(NavigationTransitions.SlideHorizontal)
    
    // Modal presentations (full-screen over tabs)
    route("masterDetail/*") { NavigationTransitions.SlideVertical }
    route("process/*") { NavigationTransitions.SlideVertical }
    route("deeplink/*") { NavigationTransitions.SlideVertical }
    route("statedriven/*") { NavigationTransitions.SlideVertical }
    
    // Settings sub-screens
    route("settings/*") { NavigationTransitions.SlideHorizontal }
    
    // Tab content - fade for smoother tab switching
    tabTransition { NavigationTransitions.Crossfade }
}
```

#### 1.2 Key Deletions

Remove these patterns completely:

```kotlin
// DELETE: Manual route initialization
remember { initializeQuoVadisRoutes() }

// DELETE: Manual graph creation
val appGraph = remember<NavigationGraph> { appRootGraph() }

// DELETE: LaunchedEffect for graph registration
LaunchedEffect(navigator, appGraph) {
    navigator.registerGraph(appGraph)
    navigator.setStartDestination(AppDestination.MainTabs)
}

// DELETE: GraphNavHost usage
GraphNavHost(
    graph = appGraph,
    navigator = navigator,
    defaultTransition = NavigationTransitions.SlideHorizontal,
    enablePredictiveBack = true
)
```

---

### Step 2: DELETE or Simplify NavigationGraphs.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/graphs/NavigationGraphs.kt`

#### Option A: DELETE ENTIRELY (Recommended)

The entire file becomes obsolete with KSP generation:

```kotlin
// OLD: NavigationGraphs.kt - 80+ lines of manual graph composition
fun appRootGraph() = navigationGraph("app_root") {
    startDestination(AppDestination.MainTabs)
    include(buildAppDestinationGraph())
    include(buildMasterDetailDestinationGraph())
    include(buildTabsDestinationGraph())
    include(buildProcessDestinationGraph())
    include(buildDeepLinkDestinationGraph())
    include(buildStateDrivenDemoDestinationGraph())
}

fun tabContentGraph() = navigationGraph("tab_content") {
    startDestination(TabDestination.Home)
    include(buildTabDestinationGraph())
    include(buildSettingsDestinationGraph())
}

// NEW: DELETE FILE - KSP generates buildMainTabsNavNode() which handles all of this
```

**Why delete:**
- `buildMainTabsNavNode()` generates the complete NavTree from `@Tab` annotations
- Per-tab stacks (`HomeDestination`, `ExploreDestination`, etc.) are generated from `@Stack` annotations
- Full-screen overlays are handled by the unified navigation tree
- No manual graph composition needed

#### Option B: Keep as Documentation Only

If you want to preserve the documentation comments, create a simpler version:

```kotlin
package com.jermey.navplayground.demo.graphs

/**
 * # Navigation Graph Composition (LEGACY REFERENCE)
 * 
 * This file is preserved for documentation purposes only.
 * The actual navigation tree is generated by KSP from annotations.
 * 
 * ## New Architecture
 * 
 * Navigation is now defined via annotations:
 * - `@Tab` on MainTabs sealed class → TabNode container
 * - `@TabItem` on each tab → Individual StackNodes
 * - `@Stack` on destination classes → Per-stack navigation
 * - `@Destination` on destinations → Routes and deep links
 * - `@Screen` on composables → Content binding
 * 
 * ## Generated Functions
 * 
 * KSP generates these functions (do not call manually):
 * - `buildMainTabsNavNode()` - Complete navigation tree
 * - `buildHomeStackNavNode()` - Home tab stack
 * - `buildExploreStackNavNode()` - Explore tab stack
 * - `buildProfileStackNavNode()` - Profile tab stack
 * - `buildSettingsStackNavNode()` - Settings tab stack
 * - `GeneratedScreenRegistry` - Destination → Composable mapping
 * 
 * ## Usage
 * 
 * ```kotlin
 * @Composable
 * fun DemoApp() {
 *     val navTree = remember { buildMainTabsNavNode() }
 *     val navigator = rememberNavigator(navTree)
 *     
 *     QuoVadisHost(
 *         navigator = navigator,
 *         screenRegistry = GeneratedScreenRegistry,
 *         tabWrapper = demoTabWrapper(navigator)
 *     )
 * }
 * ```
 * 
 * @see com.jermey.navplayground.demo.DemoApp
 * @see com.jermey.navplayground.demo.tabs.MainTabs
 */
@Deprecated(
    message = "Navigation graphs are now generated by KSP. Use buildMainTabsNavNode() instead.",
    level = DeprecationLevel.ERROR
)
object NavigationGraphsLegacy {
    // Empty - preserved for documentation only
}
```

---

### Step 3: Update Imports Throughout the App

After the migration, ensure all files use the new imports:

```kotlin
// OLD imports to REMOVE
import com.jermey.navplayground.demo.destinations.initializeQuoVadisRoutes
import com.jermey.navplayground.demo.graphs.appRootGraph
import com.jermey.navplayground.demo.graphs.tabContentGraph
import com.jermey.quo.vadis.core.navigation.compose.GraphNavHost
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph

// NEW imports to ADD
import com.jermey.navplayground.demo.tabs.buildMainTabsNavNode  // KSP-generated
import com.jermey.navplayground.demo.GeneratedScreenRegistry    // KSP-generated
import com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapper
import com.jermey.quo.vadis.core.navigation.compose.wrapper.TabWrapperScope
import com.jermey.quo.vadis.core.navigation.core.AnimationRegistry
```

---

### Step 4: Configure Full-Screen Overlay Navigation

Full-screen flows (MasterDetail, Process, etc.) that render above tabs are handled by the unified navigation tree:

```kotlin
// From any screen, navigate to full-screen overlay:
navigator.navigate(MasterDetailDestination.List)  // Opens above tabs
navigator.navigate(ProcessDestination.Start)       // Opens above tabs

// Back navigation automatically returns to tabs
navigator.navigateBack()  // Returns to previous state
```

**No additional configuration needed** - the NavTree structure handles this automatically:

```
MainTabs (TabNode)
├── [Per-tab StackNodes handle in-tab navigation]
│
└── [Full-screen destinations render above when navigated to]
    ├── MasterDetailDestination.*
    ├── ProcessDestination.*
    ├── DeepLinkDestination.*
    └── StateDrivenDemoDestination.*
```

---

### Step 5: Verify GeneratedScreenRegistry

KSP generates `GeneratedScreenRegistry` from all `@Screen` annotations:

```kotlin
// KSP generates this (you don't write it):
object GeneratedScreenRegistry : ScreenRegistry {
    override fun getScreen(destination: Destination): @Composable (Navigator) -> Unit {
        return when (destination) {
            // Tab screens
            is HomeDestination.Home -> { nav -> HomeScreen(nav) }
            is ExploreDestination.Explore -> { nav -> ExploreScreen(nav) }
            is ProfileDestination.Profile -> { nav -> ProfileScreen(nav) }
            is SettingsStackDestination.Main -> { nav -> SettingsMainScreen(nav) }
            
            // Settings sub-screens
            is SettingsDestination.Profile -> { nav -> SettingsProfileScreen(nav) }
            is SettingsDestination.Notifications -> { nav -> SettingsNotificationsScreen(nav) }
            is SettingsDestination.About -> { nav -> SettingsAboutScreen(nav) }
            
            // MasterDetail flow
            is MasterDetailDestination.List -> { nav -> MasterListScreen(nav) }
            is MasterDetailDestination.Detail -> { nav -> 
                MasterDetailScreen(destination, nav) 
            }
            
            // Process flow
            is ProcessDestination.Start -> { nav -> ProcessStartScreen(nav) }
            is ProcessDestination.Step1 -> { nav -> ProcessStep1Screen(destination, nav) }
            // ... etc
            
            else -> throw IllegalArgumentException("Unknown destination: $destination")
        }
    }
}
```

**Ensure all `@Screen` annotations are in place** (completed in MIG-007F).

---

## Checklist

### DemoApp.kt
- [ ] Remove `remember { initializeQuoVadisRoutes() }` call
- [ ] Remove `val appGraph = remember { appRootGraph() }` 
- [ ] Add `val navTree = remember { buildMainTabsNavNode() }` (KSP-generated)
- [ ] Change `rememberNavigator()` to `rememberNavigator(navTree)`
- [ ] Remove `LaunchedEffect` for graph registration
- [ ] Remove `LaunchedEffect` for start destination
- [ ] Remove `GraphNavHost` composable
- [ ] Add `QuoVadisHost` with all parameters
- [ ] Create `demoTabWrapper()` function returning `TabWrapper`
- [ ] Create `demoAnimationRegistry()` function
- [ ] Update all imports

### NavigationGraphs.kt
- [ ] DELETE file entirely (Option A - Recommended)
- [ ] OR Convert to documentation-only (Option B)
- [ ] Remove all `appRootGraph()` usages
- [ ] Remove all `tabContentGraph()` usages
- [ ] Remove all manual `navigationGraph {}` builders
- [ ] Remove all `include()` calls

### Integration Verification
- [ ] `buildMainTabsNavNode()` function exists (KSP-generated)
- [ ] `GeneratedScreenRegistry` object exists (KSP-generated)
- [ ] All `@Screen` bindings are in place (MIG-007F)
- [ ] All destination annotations are correct (MIG-007A-F)
- [ ] Tab switching works via `navigator.switchTab()`
- [ ] In-tab navigation works via `navigator.navigate()`
- [ ] Full-screen overlays render above tabs
- [ ] Back navigation works correctly
- [ ] Predictive back gestures work

---

## Verification Commands

### Compilation Check

```bash
# Full project compilation
./gradlew :composeApp:compileKotlinMetadata

# Android compilation
./gradlew :composeApp:compileDebugKotlinAndroid

# Desktop compilation
./gradlew :composeApp:compileKotlinDesktop

# iOS compilation (requires macOS)
./gradlew :composeApp:compileKotlinIosArm64
./gradlew :composeApp:compileKotlinIosSimulatorArm64
```

### Legacy Pattern Cleanup Verification

```bash
# Should return EMPTY after migration:
grep -r "initializeQuoVadisRoutes" composeApp/src/
grep -r "appRootGraph" composeApp/src/
grep -r "tabContentGraph" composeApp/src/
grep -r "GraphNavHost" composeApp/src/
grep -r "registerGraph" composeApp/src/
grep -r "setStartDestination" composeApp/src/
grep -r "NavigationGraph" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/
```

### New Architecture Verification

```bash
# Should find the new patterns:
grep -r "buildMainTabsNavNode" composeApp/src/
grep -r "QuoVadisHost" composeApp/src/
grep -r "GeneratedScreenRegistry" composeApp/src/
grep -r "TabWrapper" composeApp/src/
grep -r "AnimationRegistry" composeApp/src/
```

---

## Platform Testing Checklist

After integration, test on all platforms:

### Android

```bash
./gradlew :composeApp:installDebug
```

- [ ] App launches without crash
- [ ] Bottom navigation bar visible
- [ ] Tab switching works
- [ ] In-tab navigation works (push/pop)
- [ ] Full-screen overlays render correctly
- [ ] Back button navigates correctly
- [ ] Predictive back gesture works
- [ ] Deep links work (if configured)

### iOS (macOS only)

```bash
# Simulator
./gradlew :composeApp:iosSimulatorArm64Test
# Or open in Xcode and run
```

- [ ] App launches without crash
- [ ] Tab bar visible
- [ ] Tab switching works
- [ ] Navigation works
- [ ] Swipe-back gesture works

### Desktop

```bash
./gradlew :composeApp:runDistributable
# Or
./gradlew :composeApp:run
```

- [ ] App window opens
- [ ] Tab bar visible
- [ ] Click navigation works
- [ ] Keyboard shortcuts (if any) work

### Web (WASM/JS)

```bash
./gradlew :composeApp:wasmJsBrowserRun
# Or
./gradlew :composeApp:jsBrowserRun
```

- [ ] App loads in browser
- [ ] Tab bar visible
- [ ] Click navigation works
- [ ] Browser back button works (if configured)

---

## Rollback Plan

If critical issues arise, the migration can be reverted:

1. Restore `DemoApp.kt` from git:
   ```bash
   git checkout HEAD~1 -- composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DemoApp.kt
   ```

2. Restore `NavigationGraphs.kt` if deleted:
   ```bash
   git checkout HEAD~1 -- composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/graphs/NavigationGraphs.kt
   ```

3. Revert destination changes (MIG-007A):
   ```bash
   git checkout HEAD~1 -- composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/
   ```

**Note:** Full rollback requires reverting all MIG-007 subtasks.

---

## Related Documents

- [MIG-007: Demo App Rewrite](./MIG-007-demo-app-rewrite.md) (Parent task)
- [MIG-007A: Foundation Destinations](./MIG-007A-foundation-destinations.md) (Destination migrations)
- [MIG-007B: Tab System](./MIG-007B-tab-system.md) (TabWrapper pattern)
- [MIG-007C: Master-Detail](./MIG-007C-master-detail.md) (Full-screen flow)
- [MIG-007D: Process/Wizard](./MIG-007D-process-wizard.md) (Full-screen flow)
- [MIG-007E: Settings Stack](./MIG-007E-settings-stack.md) (In-tab navigation)
- [MIG-007F: Feature Screens](./MIG-007F-feature-screens.md) (`@Screen` bindings)
- [MIG-003: Tabbed Navigation Recipe](./MIG-003-tabbed-navigation-example.md) (Reference implementation)
- [BottomTabsRecipe.kt](../../../quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/tabs/BottomTabsRecipe.kt) (TabWrapper reference)
