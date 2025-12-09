# HIER-028: Demo App Update

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-028 |
| **Task Name** | Update Demo App for Hierarchical Rendering |
| **Phase** | Phase 4: Integration |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | HIER-024, HIER-025, HIER-027 |
| **Blocked By** | HIER-024, HIER-027 |
| **Blocks** | HIER-029 |

---

## Overview

Update the `composeApp` demo application to use `HierarchicalQuoVadisHost` and showcase all new features: proper tab animations, pane adaptation, predictive back with subtree transforms, and shared elements.

---

## File Locations

```
composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/
├── DemoApp.kt                    # Update to use HierarchicalQuoVadisHost
├── wrappers/
│   ├── MainTabsWrapper.kt        # @TabWrapper for main tabs
│   └── MasterDetailWrapper.kt    # @PaneWrapper for pane demo
└── screens/
    ├── TabDemoScreen.kt          # Enhanced tab demo
    └── PaneDemoScreen.kt         # Enhanced pane demo
```

---

## Implementation Tasks

### 1. Update DemoApp.kt

```kotlin
@Composable
fun DemoApp() {
    val navigator = rememberNavigator(startDestination = DemoHome)
    
    // Use new hierarchical host
    HierarchicalQuoVadisHost(
        navigator = navigator,
        modifier = Modifier.fillMaxSize(),
        // KSP-generated registries
        screenRegistry = GeneratedScreenRegistry,
        wrapperRegistry = GeneratedWrapperRegistry,
        transitionRegistry = GeneratedTransitionRegistry
    )
}
```

### 2. Create Tab Wrapper

```kotlin
@TabWrapper(MainTabsDestination::class)
@Composable
fun MainTabsWrapper(
    scope: TabWrapperScope,
    tabContent: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = scope.activeIndex == index,
                        onClick = { scope.switchTab(index) },
                        icon = { Icon(tab.iconFor(scope.activeIndex == index), tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            tabContent()  // ← Content renders INSIDE wrapper
        }
    }
}
```

### 3. Create Pane Wrapper

```kotlin
@PaneWrapper(MasterDetailDestination::class)
@Composable
fun MasterDetailWrapper(
    scope: PaneWrapperScope,
    paneContent: @Composable PaneContentScope.() -> Unit
) {
    if (scope.isExpanded) {
        // Multi-pane
        Row {
            Box(Modifier.weight(0.4f)) {
                scope.paneContents.first { it.role == PaneRole.Primary }.content()
            }
            Box(Modifier.weight(0.6f)) {
                scope.paneContents.first { it.role == PaneRole.Secondary }.content()
            }
        }
    } else {
        // Single-pane - defer to default behavior
        DefaultPaneLayout(scope)
    }
}
```

### 4. Add Shared Element Demo

```kotlin
@Screen
@Composable
fun SharedElementDemoList(navigator: Navigator, scope: SharedTransitionScope) {
    scope.sharedBounds(
        sharedContentState = scope.rememberSharedContentState("hero"),
        animatedVisibilityScope = LocalAnimatedVisibilityScope.current
    ) {
        // Hero image
    }
}
```

### 5. Add Comparison Toggle

```kotlin
@Composable
fun RenderingComparison() {
    var mode by remember { mutableStateOf(RenderingMode.Hierarchical) }
    
    Column {
        SegmentedButton(
            options = RenderingMode.values().toList(),
            selected = mode,
            onSelect = { mode = it }
        )
        
        QuoVadisNavHost(
            navigator = navigator,
            renderingMode = mode
        )
    }
}
```

---

## Acceptance Criteria

- [ ] DemoApp uses `HierarchicalQuoVadisHost`
- [ ] `@TabWrapper` for main tabs with bottom navigation
- [ ] `@PaneWrapper` for master-detail demo
- [ ] Shared element demo working
- [ ] Side-by-side rendering mode comparison
- [ ] All existing demos work with new host
- [ ] Predictive back demo shows subtree transforms
- [ ] Documentation updated for demo usage
