`n
# MIG-007B: Tab Navigation System Migration

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-007B |
| **Parent Task** | [MIG-007](./MIG-007-demo-app-rewrite.md) |
| **Complexity** | High |
| **Estimated Time** | 4-5 hours |
| **Dependencies** | MIG-007A (Foundation Destinations) |
| **Output** | Migrated tab system in `composeApp/` |

## Objective

Migrate the **tab navigation system** from legacy annotations (`@TabGraph`, `@Tab` per-item, `TabDefinition` interface) to the new NavNode architecture (`@Tab` container, `@TabItem` per-item, `@Destination` routes) and implement the **TabWrapper pattern** for user-controlled scaffold rendering.

This migration transforms the demo app's tabbed navigation from imperative state management to a declarative, annotation-driven system with full support for:
- Independent per-tab back stacks
- TabWrapper pattern for custom scaffold/bottom bar control
- Deep linking to individual tabs
- Unified `navigator.switchTab()` API

---

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/
├── tabs/
│   ├── MainTabs.kt          # Main bottom navigation tabs (primary migration)
│   ├── DemoTabs.kt          # Nested demo tabs (secondary migration)
│   └── MainTabsUI.kt        # Tab host UI → TabWrapper pattern
├── ui/components/
│   └── BottomNavigationBar.kt  # Tab bar component updates
```

### Reference Code (Main Branch)

| File | GitHub Permalink |
|------|------------------|
| MainTabs.kt | https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabs.kt |
| DemoTabs.kt | https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/DemoTabs.kt |
| MainTabsUI.kt | https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabsUI.kt |
| BottomNavigationBar.kt | https://github.com/jermeyyy/quo-vadis/blob/main/composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/components/BottomNavigationBar.kt |

### Reference Recipes

| Recipe | Pattern |
|--------|---------|
| [MIG-003](./MIG-003-tabbed-navigation-example.md) | `@Tab`/`@TabItem` + TabWrapper pattern |
| [MIG-005](./MIG-005-nested-tabs-detail-example.md) | Nested tabs + full-screen overlays |
| [BottomTabsRecipe.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/tabs/BottomTabsRecipe.kt) | Complete TabWrapper implementation reference |

---

## Migration Steps

### Step 1: Convert MainTabs.kt

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabs.kt`

#### 1.1 Remove TabDefinition Interface and @TabGraph

```kotlin
// OLD: @TabGraph on sealed class with TabDefinition interface
@TabGraph(
    name = "main_tabs",
    initialTab = "Home",
    primaryTab = "Home"
)
sealed class MainTabs : TabDefinition {

    @Tab(
        route = "tab_home",
        label = "Home",
        icon = "home",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Home::class
    )
    data object Home : MainTabs() {
        override val route = "tab_home"
        override val rootDestination = TabDestination.Home
    }

    @Tab(
        route = "tab_explore",
        label = "Explore",
        icon = "explore",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Explore::class
    )
    data object Explore : MainTabs() {
        override val route = "tab_explore"
        override val rootDestination = TabDestination.Explore
    }

    @Tab(
        route = "tab_profile",
        label = "Profile",
        icon = "person",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Profile::class
    )
    data object Profile : MainTabs() {
        override val route = "tab_profile"
        override val rootDestination = TabDestination.Profile
    }

    @Tab(
        route = "tab_settings",
        label = "Settings",
        icon = "settings",
        rootGraph = TabDestination::class,
        rootDestination = TabDestination.Settings::class
    )
    data object Settings : MainTabs() {
        override val route = "tab_settings"
        override val rootDestination = TabDestination.Settings
    }
}

// NEW: @Tab container with @TabItem + @Destination per tab
@Tab(name = "mainTabs", initialTab = "Home")
sealed class MainTabs : DestinationInterface {

    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()

    @TabItem(label = "Explore", icon = "explore", rootGraph = ExploreDestination::class)
    @Destination(route = "tabs/explore")
    data object Explore : MainTabs()

    @TabItem(label = "Profile", icon = "person", rootGraph = ProfileDestination::class)
    @Destination(route = "tabs/profile")
    data object Profile : MainTabs()

    @TabItem(label = "Settings", icon = "settings", rootGraph = SettingsStackDestination::class)
    @Destination(route = "tabs/settings")
    data object Settings : MainTabs()
}
```

**Key Changes:**
- `@TabGraph(name, initialTab, primaryTab)` → `@Tab(name, initialTab)` (removed `primaryTab`)
- `sealed class : TabDefinition` → `sealed class : DestinationInterface`
- `@Tab(route, label, icon, rootGraph, rootDestination)` → `@TabItem(label, icon, rootGraph)` + `@Destination(route)`
- Remove `override val route` and `override val rootDestination` properties
- Each tab now references a per-tab stack class (`HomeDestination`, `ExploreDestination`, etc.)

#### 1.2 Update Imports

```kotlin
// OLD imports
import com.jermey.navplayground.demo.destinations.TabDestination
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabGraph
import com.jermey.quo.vadis.core.navigation.core.TabDefinition

// NEW imports
import com.jermey.navplayground.demo.destinations.HomeDestination
import com.jermey.navplayground.demo.destinations.ExploreDestination
import com.jermey.navplayground.demo.destinations.ProfileDestination
import com.jermey.navplayground.demo.destinations.SettingsStackDestination
import com.jermey.quo.vadis.annotations.Destination
import com.jermey.quo.vadis.annotations.Tab
import com.jermey.quo.vadis.annotations.TabItem
import com.jermey.quo.vadis.core.navigation.core.Destination as DestinationInterface
```

---

### Step 2: Convert DemoTabs.kt (Nested Tabs)

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/DemoTabs.kt`

```kotlin
// OLD: Nested tabs using @TabGraph
@TabGraph(
    name = "demo_tabs",
    initialTab = "Tab1",
    primaryTab = "Tab1"
)
sealed class DemoTabs : TabDefinition {

    @Tab(
        route = "demo_tab1",
        label = "Tab 1",
        icon = "star",
        rootGraph = TabsDestination::class,
        rootDestination = TabsDestination.Main::class
    )
    data object Tab1 : DemoTabs() {
        override val route = "demo_tab1"
        override val rootDestination = TabsDestination.Main
    }

    @Tab(
        route = "demo_tab2",
        label = "Tab 2",
        icon = "favorite",
        rootGraph = TabsDestination::class,
        rootDestination = TabsDestination.Main::class
    )
    data object Tab2 : DemoTabs() {
        override val route = "demo_tab2"
        override val rootDestination = TabsDestination.Main
    }

    @Tab(
        route = "demo_tab3",
        label = "Tab 3",
        icon = "bookmark",
        rootGraph = TabsDestination::class,
        rootDestination = TabsDestination.Main::class
    )
    data object Tab3 : DemoTabs() {
        override val route = "demo_tab3"
        override val rootDestination = TabsDestination.Main
    }
}

// NEW: Nested tabs with @Tab + @TabItem
@Tab(name = "demoTabs", initialTab = "Tab1")
sealed class DemoTabs : DestinationInterface {

    @TabItem(label = "Tab 1", icon = "star", rootGraph = DemoTab1Destination::class)
    @Destination(route = "demo/tab1")
    data object Tab1 : DemoTabs()

    @TabItem(label = "Tab 2", icon = "favorite", rootGraph = DemoTab2Destination::class)
    @Destination(route = "demo/tab2")
    data object Tab2 : DemoTabs()

    @TabItem(label = "Tab 3", icon = "bookmark", rootGraph = DemoTab3Destination::class)
    @Destination(route = "demo/tab3")
    data object Tab3 : DemoTabs()
}
```

**Note:** If all tabs share the same root graph (`TabsDestination`), you can either:
1. Create separate per-tab stacks (`DemoTab1Destination`, `DemoTab2Destination`, `DemoTab3Destination`)
2. Or reference the same shared stack class with different start configurations

---

### Step 3: Convert MainTabsUI.kt to TabWrapper Pattern

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabsUI.kt`

#### 3.1 Remove TabbedNavHost and Manual State Management

```kotlin
// OLD: TabbedNavHost with manual tab state
@Composable
fun MainTabsScreen(
    parentNavigator: Navigator,
    parentEntry: BackStackEntry,
    modifier: Modifier = Modifier
) {
    val tabGraph = remember<NavigationGraph> { tabContentGraph() }
    
    val tabState = rememberTabNavigator(
        config = MainTabsConfig,
        parentNavigator = parentNavigator,
        parentEntry = parentEntry
    )
    val selectedTab by tabState.selectedTab.collectAsState()
    
    TabbedNavHost(
        tabState = tabState,
        tabGraphs = MainTabsConfig.allTabs.associateWith { tabGraph },
        navigator = parentNavigator,
        modifier = modifier,
        tabUI = @Composable { content ->
            Scaffold(
                bottomBar = {
                    val currentDest = tabState.selectedTab.collectAsState().value
                    val currentRoute = currentDest?.rootDestination?.let { dest ->
                        when (dest) {
                            is TabDestination.Home -> "home"
                            is TabDestination.Explore -> "explore"
                            is TabDestination.Profile -> "profile"
                            is TabDestination.Settings -> "settings"
                            else -> null
                        }
                    }
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        onNavigate = { destination ->
                            val tab = MainTabsConfig.allTabs.find { 
                                it.rootDestination == destination 
                            }
                            if (tab != null) {
                                tabState.selectTab(tab)
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    content()
                }
            }
        },
        tabTransitionSpec = TabTransitionSpec.Crossfade,
        defaultTransition = NavigationTransitions.SlideHorizontal
    )
}

// NEW: TabWrapper pattern with QuoVadisHost
/**
 * Creates a TabWrapper for the main tabs with bottom navigation.
 *
 * The TabWrapper pattern gives full control over the scaffold structure
 * while the library handles tab content rendering and state management.
 */
fun mainTabsWrapper(): TabWrapper = { tabContent ->
    // 'this' is TabWrapperScope - provides activeTabIndex, tabMetadata, switchTab()
    Scaffold(
        bottomBar = {
            MainBottomNavigationBar(
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
            // Library renders active tab content
            tabContent()
        }
    }
}

/**
 * Main app entry point using QuoVadisHost with TabWrapper.
 */
@Composable
fun MainTabsApp() {
    // KSP generates buildMainTabsNavNode() from @Tab annotations
    val navTree = remember { buildMainTabsNavNode() }
    val navigator = rememberNavigator(navTree)
    
    QuoVadisHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        tabWrapper = mainTabsWrapper()
    )
}
```

#### 3.2 TabWrapperScope API Reference

The `tabWrapper` lambda provides access to `TabWrapperScope` with these properties:

| Property/Method | Type | Description |
|-----------------|------|-------------|
| `activeTabIndex` | `Int` | Currently selected tab (0-based) |
| `tabCount` | `Int` | Total number of tabs |
| `tabMetadata` | `List<TabMetadata>` | Tab UI metadata (label, icon) |
| `isTransitioning` | `Boolean` | Whether tab switch animation is in progress |
| `switchTab(index)` | `(Int) -> Unit` | Switch to tab by index |
| `switchTab(route)` | `(String) -> Unit` | Switch to tab by route |
| `navigator` | `Navigator` | Navigator instance |

---

### Step 4: Update BottomNavigationBar Component

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/components/BottomNavigationBar.kt`

```kotlin
// OLD: Route-based selection with Destination callback
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (Destination) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onNavigate(TabDestination.Home) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
            label = { Text("Explore") },
            selected = currentRoute == "explore",
            onClick = { onNavigate(TabDestination.Explore) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { onNavigate(TabDestination.Profile) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == "settings",
            onClick = { onNavigate(TabDestination.Settings) }
        )
    }
}

// NEW: Index-based selection with TabMetadata
@Composable
fun MainBottomNavigationBar(
    activeTabIndex: Int,
    tabMetadata: List<TabMetadata>,
    onTabSelected: (Int) -> Unit,
    isTransitioning: Boolean = false
) {
    NavigationBar {
        tabMetadata.forEachIndexed { index, meta ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = getTabIcon(meta.icon),
                        contentDescription = meta.label
                    )
                },
                label = { Text(meta.label) },
                selected = activeTabIndex == index,
                onClick = { onTabSelected(index) },
                enabled = !isTransitioning  // Disable during animations
            )
        }
    }
}

/**
 * Maps icon identifier string to Material icon.
 */
private fun getTabIcon(iconName: String): ImageVector = when (iconName) {
    "home" -> Icons.Default.Home
    "explore" -> Icons.Default.Explore
    "person" -> Icons.Default.Person
    "settings" -> Icons.Default.Settings
    "star" -> Icons.Default.Star
    "favorite" -> Icons.Default.Favorite
    "bookmark" -> Icons.Default.Bookmark
    else -> Icons.Default.Circle  // Fallback
}
```

**Key Changes:**
- `currentRoute: String?` → `activeTabIndex: Int`
- `onNavigate: (Destination) -> Unit` → `onTabSelected: (Int) -> Unit`
- Route comparison (`"home"`, `"explore"`) → Index comparison
- Added `isTransitioning` to disable clicks during animations
- Use `TabMetadata` from TabWrapperScope for labels/icons

---

### Step 5: Update Tab Switching Patterns

#### 5.1 Tab Switching in TabWrapper (Scaffold Level)

```kotlin
// OLD: tabState.selectTab(tab)
tabState.selectTab(MainTabs.Home)

// NEW: switchTab(index) from TabWrapperScope
switchTab(0)  // Home tab
switchTab(1)  // Explore tab
```

#### 5.2 Tab Switching from Screens (Navigator Level)

```kotlin
// OLD: tabState.switchTab(tab) or navigator + tab state coordination
val tab = MainTabsConfig.allTabs.find { it.rootDestination == destination }
tabState.selectTab(tab)

// NEW: navigator.switchTab(index) or navigator.switchTab(Tab)
navigator.switchTab(0)  // By index
navigator.switchTab(MainTabs.Home)  // By tab destination
```

---

### Step 6: Delete Obsolete Code

After migration, remove these obsolete patterns:

```kotlin
// DELETE: MainTabsConfig object (replaced by @Tab annotations)
object MainTabsConfig : TabbedNavigatorConfig<MainTabs> { ... }

// DELETE: TabDefinition overrides
override val route = "..."
override val rootDestination = ...

// DELETE: Manual graph mapping
val tabGraphs = MainTabsConfig.allTabs.associateWith { tabGraph }

// DELETE: rememberTabNavigator calls
val tabState = rememberTabNavigator(config = MainTabsConfig, ...)

// DELETE: TabbedNavHost usage
TabbedNavHost(tabState = ..., tabGraphs = ..., tabUI = { ... })
```

---

## Checklist

### MainTabs.kt
- [ ] Replace `@TabGraph` with `@Tab` annotation
- [ ] Replace per-item `@Tab` with `@TabItem` + `@Destination`
- [ ] Remove `TabDefinition` interface inheritance
- [ ] Use `DestinationInterface` as base class
- [ ] Remove `override val route` properties
- [ ] Remove `override val rootDestination` properties
- [ ] Update `rootGraph` references to per-tab stack classes
- [ ] Update imports

### DemoTabs.kt
- [ ] Replace `@TabGraph` with `@Tab` annotation
- [ ] Replace per-item `@Tab` with `@TabItem` + `@Destination`
- [ ] Remove `TabDefinition` interface inheritance
- [ ] Create per-tab stack classes if needed
- [ ] Update imports

### MainTabsUI.kt
- [ ] Remove `TabbedNavHost` composable
- [ ] Remove `rememberTabNavigator()` call
- [ ] Remove manual `tabGraphs` mapping
- [ ] Create `mainTabsWrapper()` function returning `TabWrapper`
- [ ] Use `TabWrapperScope` properties (`activeTabIndex`, `tabMetadata`, `switchTab`)
- [ ] Update to `QuoVadisHost` with `tabWrapper` parameter
- [ ] Remove debug logging statements

### BottomNavigationBar.kt
- [ ] Change signature to accept `activeTabIndex: Int`
- [ ] Change signature to accept `tabMetadata: List<TabMetadata>`
- [ ] Replace route-based selection with index-based
- [ ] Add `isTransitioning` parameter
- [ ] Use `TabMetadata.label` and `TabMetadata.icon`
- [ ] Create icon mapping helper function

### General
- [ ] Delete `MainTabsConfig` object
- [ ] Remove all `TabDefinition` interface usages
- [ ] Update all `tabState.selectTab()` to `switchTab()` or `navigator.switchTab()`
- [ ] Verify no remaining `@TabGraph` annotations

---

## Verification

```bash
# Verify compilation after tab system changes
./gradlew :composeApp:compileKotlinMetadata

# Check for remaining legacy tab annotations
grep -r "@TabGraph" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/
grep -r "TabDefinition" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/

# Check for remaining old tab state patterns
grep -r "rememberTabNavigator" composeApp/src/commonMain/kotlin/
grep -r "TabbedNavHost" composeApp/src/commonMain/kotlin/

# All should return empty results after migration
```

---

## Downstream Dependencies

After MIG-007B completion, these subtasks are unblocked:

| Subtask | Dependency |
|---------|------------|
| MIG-007F (Feature Screens) | Uses `@Screen` with tab destinations |
| MIG-007G (Integration) | Wires TabWrapper with full app navigation |

---

## Related Documents

- [MIG-007: Demo App Rewrite](./MIG-007-demo-app-rewrite.md) (Parent task)
- [MIG-007A: Foundation Destinations](./MIG-007A-foundation-destinations.md) (Prerequisite)
- [MIG-003: Tabbed Navigation Recipe](./MIG-003-tabbed-navigation-example.md) (`@Tab`/`@TabItem` pattern)
- [MIG-005: Nested Tabs + Detail Recipe](./MIG-005-nested-tabs-detail-example.md) (Full-screen over tabs)
- [BottomTabsRecipe.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-recipes/src/commonMain/kotlin/com/jermey/quo/vadis/recipes/tabs/BottomTabsRecipe.kt) (TabWrapper implementation)
- [TabAnnotations.kt](https://github.com/jermeyyy/quo-vadis/blob/main/quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt) (`@Tab`, `@TabItem` definitions)

````
