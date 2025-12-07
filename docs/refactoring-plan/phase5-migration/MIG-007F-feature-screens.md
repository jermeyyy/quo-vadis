# MIG-007F: Feature Screens - @Screen Annotations

## Overview

| Attribute | Value |
|-----------|-------|
| **Task ID** | MIG-007F |
| **Parent Task** | [MIG-007](./MIG-007-demo-app-rewrite.md) |
| **Complexity** | Medium |
| **Estimated Time** | 3-4 hours |
| **Dependencies** | MIG-007A (Foundation Destinations), MIG-007B (Tab System) |
| **Output** | Migrated screen files with `@Screen` annotations, **deleted** `ContentDefinitions.kt` |

## Objective

Add `@Screen` annotations directly to all remaining feature screens, replacing the centralized `@Content` binding pattern in `ContentDefinitions.kt`. After this migration, **ContentDefinitions.kt should be DELETED** as all bindings will live directly on the screen composables.

This is the **final subtask** that completes the screen binding migration, moving from:
- **OLD**: Centralized `@Content` functions in `ContentDefinitions.kt` that wrap screens
- **NEW**: `@Screen` annotations directly on screen composables

---

## Scope

### Files to Modify

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/
├── HomeScreen.kt                    # Tab root screen
├── ExploreScreen.kt                 # Tab root screen
├── DeepLinkDemoScreen.kt            # Deep link demo
├── profile/
│   └── ProfileScreen.kt             # FlowMVI screen (or ProfileContainer.kt)
├── statedriven/
│   └── StateDrivenDemoScreen.kt     # State-driven demo
└── tabs/
    └── TabsScreens.kt               # TabsMainScreen, TabSubItemScreen
```

### File to DELETE

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/content/
└── ContentDefinitions.kt            # DELETE after migration
```

### Reference Code

| File | Current Binding in ContentDefinitions.kt |
|------|------------------------------------------|
| HomeScreen.kt | `@Content(TabDestination.Home::class) fun HomeContent(...)` |
| ExploreScreen.kt | `@Content(TabDestination.Explore::class) fun ExploreContent(...)` |
| ProfileScreen.kt | `@Content(TabDestination.Profile::class) fun ProfileContent(...)` |
| DeepLinkDemoScreen.kt | `@Content(DeepLinkDestination.Demo::class) fun DeepLinkDemoContent(...)` |
| StateDrivenDemoScreen.kt | `@Content(StateDrivenDemoDestination.Demo::class) fun StateDrivenDemoContent(...)` |
| TabsScreens.kt | `@Content(TabsDestination.Main::class)` + `@Content(TabsDestination.SubItem::class)` |

---

## Migration Steps

### Step 1: Tab Root Screens - HomeScreen

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/HomeScreen.kt`

#### Current State (in ContentDefinitions.kt)

```kotlin
@Content(TabDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(
        onNavigateToMasterDetail = { navigator.navigate(...) },
        onNavigateToTabs = { navigator.navigate(...) },
        onNavigateToProcess = { navigator.navigate(...) },
        onNavigateToStateDriven = { navigator.navigate(...) },
        navigator = navigator
    )
}
```

#### Migration

```kotlin
// NEW: Add @Screen directly on HomeScreen
@Screen(HomeDestination.Home::class)
@Composable
fun HomeScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    // Simplified signature - navigation callbacks handled internally
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Home") },
                navigationIcon = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = modifier,
            paddingValues = paddingValues,
            onNavigateToMasterDetail = {
                navigator.navigate(
                    MasterDetailDestination.List,
                    NavigationTransitions.SlideHorizontal
                )
            },
            onNavigateToTabs = {
                navigator.navigate(TabsDestination.Main, NavigationTransitions.SlideHorizontal)
            },
            onNavigateToProcess = {
                navigator.navigate(ProcessDestination.Start, NavigationTransitions.SlideHorizontal)
            },
            onNavigateToStateDriven = {
                navigator.navigate(
                    StateDrivenDemoDestination.Demo,
                    NavigationTransitions.SlideHorizontal
                )
            }
        )
    }
    // ... rest of implementation (bottom sheet, etc.)
}
```

**Key Changes:**
- Add `@Screen(HomeDestination.Home::class)` annotation
- Remove lambda parameters (`onNavigateToMasterDetail`, etc.)
- Use `navigator` directly for navigation
- Import `HomeDestination` from new destination classes (MIG-007A)

---

### Step 2: Tab Root Screens - ExploreScreen

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/ExploreScreen.kt`

#### Current State (in ContentDefinitions.kt)

```kotlin
@Content(TabDestination.Explore::class)
@Composable
fun ExploreContent(navigator: Navigator) {
    ExploreScreen(
        onItemClick = { itemId ->
            navigator.navigate(
                MasterDetailDestination.Detail(itemId),
                NavigationTransitions.SlideHorizontal
            )
        },
        navigator = navigator
    )
}
```

#### Migration

```kotlin
// NEW: Add @Screen directly on ExploreScreen
@Screen(ExploreDestination.Explore::class)
@Composable
fun ExploreScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val items = remember { (1..EXPLORE_ITEMS_COUNT).map { "Item $it" } }
    
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Explore") },
                navigationIcon = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        ExploreScreenContent(
            modifier = modifier,
            paddingValues = paddingValues,
            items = items,
            onItemClick = { itemId ->
                navigator.navigate(
                    MasterDetailDestination.Detail(itemId),
                    NavigationTransitions.SlideHorizontal
                )
            }
        )
    }
    // ... rest of implementation (bottom sheet, etc.)
}
```

**Key Changes:**
- Add `@Screen(ExploreDestination.Explore::class)` annotation
- Remove `onItemClick` lambda parameter
- Use `navigator` directly for item click navigation
- Import `ExploreDestination` from new destination classes

---

### Step 3: Profile Screen (FlowMVI Pattern)

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/profile/ProfileScreen.kt`

The Profile feature uses the FlowMVI `StoreScreen` pattern. The `@Screen` annotation should go on `ProfileScreen`, NOT on `ProfileContainer`.

#### Current State (in ContentDefinitions.kt)

```kotlin
@Content(TabDestination.Profile::class)
@Composable
fun ProfileContent(navigator: Navigator) {
    ProfileScreen(navigator = navigator)
}
```

#### Migration

```kotlin
// NEW: Add @Screen directly on ProfileScreen
@Screen(ProfileDestination.Profile::class)
@Composable
fun ProfileScreen(
    navigator: Navigator = koinInject(),
    container: ProfileContainer = koinInject()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    StoreScreen(
        container = container,
        onAction = { action ->
            scope.launch {
                when (action) {
                    // ... action handling unchanged
                }
            }
        }
    ) { state, intentReceiver ->
        // ... UI implementation unchanged
    }
}
```

**Key Changes:**
- Add `@Screen(ProfileDestination.Profile::class)` annotation
- No signature changes needed (already has `navigator` parameter with default)
- Import `ProfileDestination` from new destination classes

---

### Step 4: Deep Link Demo Screen

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/DeepLinkDemoScreen.kt`

#### Current State (in ContentDefinitions.kt)

```kotlin
@Content(DeepLinkDestination.Demo::class)
@Composable
fun DeepLinkDemoContent(navigator: Navigator) {
    DeepLinkDemoScreen(
        onBack = { navigator.navigateBack() },
        onNavigateViaDeepLink = { deepLinkUri ->
            navigator.handleDeepLink(DeepLink.parse(deepLinkUri))
        }
    )
}
```

#### Migration

```kotlin
// NEW: Add @Screen directly on DeepLinkDemoScreen
@Screen(DeepLinkDestination.Demo::class)
@Composable
fun DeepLinkDemoScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deep Link Demo") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        DeepLinkDemoScreenContent(
            padding = padding,
            onNavigateViaDeepLink = { deepLinkUri ->
                navigator.handleDeepLink(DeepLink.parse(deepLinkUri))
            }
        )
    }
}
```

**Key Changes:**
- Add `@Screen(DeepLinkDestination.Demo::class)` annotation
- Replace lambda parameters with direct `navigator` usage
- Keep internal `DeepLinkDemoScreenContent` helper as-is

---

### Step 5: State-Driven Demo Screen

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/statedriven/StateDrivenDemoScreen.kt`

#### Current State (in ContentDefinitions.kt)

```kotlin
@Content(StateDrivenDemoDestination.Demo::class)
@Composable
fun StateDrivenDemoContent(navigator: Navigator) {
    StateDrivenDemoScreen(
        onBack = { navigator.navigateBack() }
    )
}
```

#### Migration

```kotlin
// NEW: Add @Screen directly on StateDrivenDemoScreen
@Screen(StateDrivenDemoDestination.Demo::class)
@Composable
fun StateDrivenDemoScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val backStack = remember {
        MutableBackStack().apply {
            push(StateDrivenDestination.Home)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("State-Driven Navigation") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        StateDrivenDemoContent(
            backStack = backStack,
            modifier = Modifier.padding(padding)
        )
    }
}
```

**Key Changes:**
- Add `@Screen(StateDrivenDemoDestination.Demo::class)` annotation
- Replace `onBack` lambda with direct `navigator.navigateBack()` call
- Rename internal content composable to avoid naming conflict

---

### Step 6: Tabs Demo Screens

**File:** `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/tabs/TabsScreens.kt`

#### 6.1 TabsMainScreen

##### Current State (in ContentDefinitions.kt)

```kotlin
@Content(TabsDestination.Main::class)
@Composable
fun TabsMainContent(navigator: Navigator) {
    TabsMainScreen(
        onNavigateToSubItem = { tabId, itemId ->
            navigator.navigate(
                TabsDestination.SubItem(tabId, itemId),
                NavigationTransitions.SlideHorizontal
            )
        },
        onBack = { navigator.navigateBack() },
        navigator = navigator
    )
}
```

##### Migration

```kotlin
// NEW: Add @Screen directly on TabsMainScreen
@Screen(TabsDestination.Main::class)
@Composable
fun TabsMainScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    val tabState = rememberTabNavigator(DemoTabsConfig, navigator)
    val selectedTab by tabState.selectedTab.collectAsState()

    val tabsGraph = remember<NavigationGraph> {
        buildTabsDestinationGraph()
    }

    LaunchedEffect(navigator, tabsGraph) {
        navigator.registerGraph(tabsGraph)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Tabs Navigation Demo") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ... tab UI unchanged
            
            // Tab content with inline navigation
            when (selectedTab) {
                DemoTabs.Tab1 -> TabContent(
                    tabId = "tab1",
                    title = "First Tab",
                    items = (1..10).map { "Item $it in Tab 1" },
                    onItemClick = { itemId ->
                        navigator.navigate(
                            TabsDestination.SubItem("tab1", itemId),
                            NavigationTransitions.SlideHorizontal
                        )
                    },
                    icon = Icons.Default.Star
                )
                // ... similar for Tab2, Tab3
            }
        }
    }
}
```

#### 6.2 TabSubItemScreen

##### Current State (in ContentDefinitions.kt)

```kotlin
@Content(TabsDestination.SubItem::class)
@Composable
fun TabSubItemContent(data: TabsDestination.SubItemData, navigator: Navigator) {
    TabSubItemScreen(
        tabId = data.tabId,
        itemId = data.itemId,
        onBack = { navigator.navigateBack() }
    )
}
```

##### Migration

```kotlin
// NEW: Add @Screen with arguments
@Screen(TabsDestination.SubItem::class)
@Composable
fun TabSubItemScreen(
    @Argument tabId: String,
    @Argument itemId: String,
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ... content using tabId, itemId directly (no data wrapper)
        }
    }
}
```

**Key Changes:**
- Add `@Screen(TabsDestination.SubItem::class)` annotation
- Replace `data: TabsDestination.SubItemData` wrapper with individual `@Argument` parameters
- Replace `onBack` lambda with direct `navigator.navigateBack()` call

---

### Step 7: DELETE ContentDefinitions.kt

After all screens have `@Screen` annotations, **DELETE the entire file**:

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/content/ContentDefinitions.kt
```

**Why Delete:**
- All `@Content` bindings are replaced by `@Screen` annotations on screens
- No backward compatibility needed - new architecture only
- KSP will generate graph builders directly from `@Screen` annotations

---

## Required Imports

Each modified screen file needs these imports:

```kotlin
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.annotations.Argument  // For screens with arguments
import com.jermey.navplayground.demo.destinations.HomeDestination  // Or appropriate destination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.NavigationTransitions
```

---

## Checklist

### Tab Root Screens
- [ ] Add `@Screen(HomeDestination.Home::class)` to `HomeScreen`
- [ ] Refactor `HomeScreen` to use `navigator` directly (remove lambda params)
- [ ] Add `@Screen(ExploreDestination.Explore::class)` to `ExploreScreen`
- [ ] Refactor `ExploreScreen` to use `navigator` directly

### Profile (FlowMVI)
- [ ] Add `@Screen(ProfileDestination.Profile::class)` to `ProfileScreen`
- [ ] Verify `ProfileContainer` does NOT get `@Screen` annotation

### Deep Link Demo
- [ ] Add `@Screen(DeepLinkDestination.Demo::class)` to `DeepLinkDemoScreen`
- [ ] Refactor to use `navigator` directly

### State-Driven Demo
- [ ] Add `@Screen(StateDrivenDemoDestination.Demo::class)` to `StateDrivenDemoScreen`
- [ ] Refactor to use `navigator` directly

### Tabs Demo
- [ ] Add `@Screen(TabsDestination.Main::class)` to `TabsMainScreen`
- [ ] Add `@Screen(TabsDestination.SubItem::class)` to `TabSubItemScreen`
- [ ] Convert `SubItemData` wrapper to individual `@Argument` parameters

### Cleanup
- [ ] **DELETE** `ContentDefinitions.kt`
- [ ] Delete empty `content/` directory if applicable
- [ ] Update any imports referencing deleted file

### Verification
- [ ] Run `./gradlew :composeApp:compileKotlinMetadata` - no compilation errors
- [ ] Run `./gradlew :composeApp:kspKotlin` - KSP generates graph builders
- [ ] No `@Content` annotations remain in codebase

---

## Verification Commands

```bash
# Verify compilation after all changes
./gradlew :composeApp:compileKotlinMetadata

# Verify KSP processing generates new graph builders
./gradlew :composeApp:kspKotlin

# Verify ContentDefinitions.kt is deleted
test ! -f composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/content/ContentDefinitions.kt && echo "✓ Deleted" || echo "✗ Still exists"

# Check for any remaining @Content annotations (should be empty)
grep -r "@Content" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/

# Check all @Screen annotations are present
grep -r "@Screen" composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/

# Full build test
./gradlew :composeApp:build
```

---

## Transformation Summary

| Screen | Old Pattern | New Pattern |
|--------|------------|-------------|
| HomeScreen | `@Content` wrapper in ContentDefinitions.kt | `@Screen(HomeDestination.Home::class)` on composable |
| ExploreScreen | `@Content` wrapper | `@Screen(ExploreDestination.Explore::class)` |
| ProfileScreen | `@Content` wrapper | `@Screen(ProfileDestination.Profile::class)` |
| DeepLinkDemoScreen | `@Content` wrapper | `@Screen(DeepLinkDestination.Demo::class)` |
| StateDrivenDemoScreen | `@Content` wrapper | `@Screen(StateDrivenDemoDestination.Demo::class)` |
| TabsMainScreen | `@Content` wrapper | `@Screen(TabsDestination.Main::class)` |
| TabSubItemScreen | `@Content` + data class wrapper | `@Screen` + `@Argument` params |

---

## Related Documents

- [MIG-007: Demo App Rewrite](./MIG-007-demo-app-rewrite.md) (Parent task)
- [MIG-007A: Foundation Destinations](./MIG-007A-foundation-destinations.md) (Prerequisite - defines destination classes)
- [MIG-007B: Tab System Migration](./MIG-007B-tab-system.md) (Prerequisite - tab destinations)
- [MIG-001: Simple Stack Recipe](./MIG-001-simple-stack-example.md) (`@Screen` annotation pattern)
- [MIG-002: Master-Detail Recipe](./MIG-002-master-detail-example.md) (`@Argument` pattern)
- [PREP-002: Deprecated Annotations](./PREP-002-deprecated-annotations.md) (Legacy `@Content` reference)
