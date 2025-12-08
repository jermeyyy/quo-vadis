# Phase 5: Documentation & Testing

## Phase Overview

**Objective**: Complete comprehensive documentation and testing for the tabbed navigation feature, ensuring production-ready quality.

**Scope**:
- Architecture documentation
- API reference documentation
- Migration guide for existing apps
- Comprehensive test suite (unit, integration, UI)
- Multiplatform testing
- Performance benchmarks
- Best practices guide

**Timeline**: 2-3 days

**Dependencies**: 
- Phase 1 (Core Foundation) ✅
- Phase 2 (Compose Integration) ✅
- Phase 3 (KSP Annotations) ✅
- Phase 4 (Demo App) ✅

## Documentation Plan

### Document 1: TABBED_NAVIGATION.md

**File**: `quo-vadis-core/docs/TABBED_NAVIGATION.md`

**Structure**:

n
# Tabbed Navigation in Quo Vadis

## Overview

Quo Vadis provides first-class support for tabbed navigation with independent,
parallel navigation stacks. This enables common UX patterns like bottom navigation
bars where each tab maintains its own navigation state.

## Features

- ✅ **Independent Stacks**: Each tab has its own navigation backstack
- ✅ **State Preservation**: Tab state preserved when switching
- ✅ **Type-Safe API**: Compile-time safe tab definitions
- ✅ **Hierarchical Back Press**: Intelligent delegation through navigation tree
- ✅ **KSP Code Generation**: Minimal boilerplate with annotations
- ✅ **Predictive Back**: Gesture support on Android/iOS
- ✅ **Multiplatform**: Works on all supported platforms
- ✅ **Nested Tabs**: Tabs within tabs support
- ✅ **Deep Linking**: Direct navigation to tab content

## Quick Start

### 1. Define Tab Container

```kotlin
@TabGraph("main_tabs")
sealed class MainTabs : TabDefinition {
    @Tab(
        route = "home",
        label = "Home",
        icon = "home",
        rootGraph = HomeDestination::class
    )
    data object Home : MainTabs() {
        override val id = "home"
        override val rootDestination = HomeDestination.Root
    }
    
    @Tab(
        route = "profile",
        label = "Profile",
        icon = "person",
        rootGraph = ProfileDestination::class
    )
    data object Profile : MainTabs() {
        override val id = "profile"
        override val rootDestination = ProfileDestination.Root
    }
}
```

### 2. Use Generated Container

```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator()
    
    // Generated from @TabGraph annotation
    MainTabsContainer(parentNavigator = navigator)
}
```

That's it! The container includes:
- Tab state management
- Independent navigation stacks per tab
- Intelligent back press handling
- State preservation across tab switches

## Architecture

[Include diagram and detailed architecture explanation]

## API Reference

[Complete API documentation for all components]

## Advanced Usage

### Custom Tab UI

[How to customize generated containers]

### Nested Tabs

[How to implement tabs within tabs]

### Deep Linking

[How to deep link to tab content]

### State Persistence

[How to save/restore tab state]

## Testing

[How to test tabbed navigation]

## Migration Guide

[How to migrate from manual implementations]

## Best Practices

[Recommended patterns and anti-patterns]

## Performance

[Performance considerations and optimizations]

## Troubleshooting

[Common issues and solutions]
```

**Length**: ~2,500 lines (comprehensive guide)

---

### Document 2: API Reference Updates

**File**: `quo-vadis-core/docs/API_REFERENCE.md`

**Add New Sections**:

n
## Tabbed Navigation

### TabDefinition

Interface for defining tabs in a tabbed navigation container.

**Package**: `com.jermey.quo.vadis.core.navigation.core`

```kotlin
interface TabDefinition {
    val id: String
    val rootDestination: Destination
    val label: String?
    val icon: String?
}
```

**Properties**:
- `id`: Unique identifier for this tab
- `rootDestination`: The destination this tab starts with
- `label`: Optional display label for UI
- `icon`: Optional icon identifier for UI

**Usage**:
```kotlin
sealed class MyTabs : TabDefinition {
    data object Home : MyTabs() {
        override val id = "home"
        override val rootDestination = HomeScreen
        override val label = "Home"
        override val icon = "home"
    }
}
```

---

### TabNavigatorState

State holder for tabbed navigation.

**Package**: `com.jermey.quo.vadis.core.navigation.core`

```kotlin
class TabNavigatorState(
    val config: TabNavigatorConfig
) : BackPressHandler
```

**Properties**:
- `selectedTab: StateFlow<TabDefinition>` - Currently selected tab
- `tabStacks: StateFlow<Map<TabDefinition, List<Destination>>>` - All tab stacks
- `currentTabStack: StateFlow<List<Destination>>` - Current tab's stack

**Methods**:
- `selectTab(tab: TabDefinition): Boolean` - Switch to a tab
- `navigateInTab(destination: Destination)` - Navigate within current tab
- `navigateBackInTab(): Boolean` - Go back within current tab
- `clearTabToRoot(tab: TabDefinition)` - Clear a tab to its root
- `onBack(): Boolean` - Handle back press
- `canHandleBack(): Boolean` - Check if back press can be handled

[Continue with all API components...]

---

### @TabGraph Annotation

Marks a sealed class as a tab container.

**Package**: `com.jermey.quo.vadis.annotations`

```kotlin
@Target(AnnotationTarget.CLASS)
annotation class TabGraph(
    val name: String,
    val initialTab: String = "",
    val primaryTab: String = ""
)
```

**Parameters**:
- `name`: Unique identifier for this tab container
- `initialTab`: Class name of initial tab (defaults to first @Tab)
- `primaryTab`: Class name of primary/home tab (defaults to initialTab)

**Generates**:
- `{ClassName}Config: TabNavigatorConfig` - Configuration object
- `{ClassName}Container: @Composable` - Container composable
- `build{ClassName}Graph(): NavigationGraph` - Graph builder

[Continue with @Tab, @TabContent annotations...]
```

---

### Document 3: Migration Guide

**File**: `quo-vadis-core/docs/MIGRATION_TO_TABBED_NAVIGATION.md`

**Contents**:

n
# Migrating to Tabbed Navigation

This guide helps you migrate from manual tab implementations to the
quo-vadis tabbed navigation API.

## Why Migrate?

- ✅ 87% less boilerplate code
- ✅ Automatic state preservation
- ✅ Type-safe navigation
- ✅ Better back press handling
- ✅ Built-in testing utilities

## Migration Scenarios

### Scenario 1: Manual State Management

**Before**:
```kotlin
// 150+ lines of manual state management
class TabState {
    var selectedTab by mutableStateOf(Tab.Home)
    val tabStacks = mutableStateMapOf<Tab, List<Route>>()
    
    fun selectTab(tab: Tab) { ... }
    fun navigateInTab(route: Route) { ... }
    fun onBack(): Boolean { ... }
}

@Composable
fun TabContainer() {
    val tabState = remember { TabState() }
    // Manual visibility management...
}
```

**After**:
```kotlin
// ~20 lines with annotations
@TabGraph("main")
sealed class MainTabs : TabDefinition {
    @Tab(route = "home", label = "Home", icon = "home", rootGraph = HomeDestination::class)
    data object Home : MainTabs()
}

@Composable
fun App() {
    MainTabsContainer(parentNavigator = navigator)
}
```

---

### Scenario 2: NavigateAndReplace Pattern

**Before**:
```kotlin
// Loses state on tab switch
bottomNavBar.onClick = { tab ->
    navigator.navigateAndReplace(tab.destination)
}
```

**After**:
```kotlin
// Preserves state automatically
bottomNavBar.onClick = { tab ->
    tabState.selectTab(tab)
}
```

---

### Scenario 3: Custom Bottom Bar

**Before**:
```kotlin
Scaffold(
    bottomBar = { BottomBar(onTabClick = { ... }) }
) {
    // Manual content switching
    when (selectedTab) {
        Tab.Home -> HomeScreen()
        Tab.Profile -> ProfileScreen()
    }
}
```

**After**:
```kotlin
@Composable
fun CustomTabContainer(parentNavigator: Navigator) {
    val tabState = rememberTabNavigator(MainTabsConfig, parentNavigator)
    
    Scaffold(
        bottomBar = {
            BottomBar(
                selected = tabState.selectedTab.collectAsState().value,
                onTabClick = { tabState.selectTab(it) }
            )
        }
    ) { padding ->
        TabbedNavHost(
            tabState = tabState,
            tabGraphs = mapOf(...),
            modifier = Modifier.padding(padding)
        )
    }
}
```

---

## Step-by-Step Migration

### Step 1: Define Tab Container with @TabGraph

[Detailed instructions...]

### Step 2: Create Tab Graphs

[Detailed instructions...]

### Step 3: Update Bottom Bar

[Detailed instructions...]

### Step 4: Remove Manual State Management

[Detailed instructions...]

### Step 5: Test Thoroughly

[Detailed instructions...]

---

## Common Pitfalls

### Pitfall 1: Mixing Navigation Paradigms

❌ **Wrong**:
```kotlin
bottomBar.onClick = {
    navigator.navigateAndReplace(tab.destination) // Loses state!
}
```

✅ **Correct**:
```kotlin
bottomBar.onClick = {
    tabState.selectTab(tab) // Preserves state
}
```

[More pitfalls...]

---

## Testing After Migration

[How to verify migration was successful...]

---

## Troubleshooting

[Common migration issues and solutions...]
```

---

## Testing Plan

### Unit Tests (Phase 1 Core)

**Target Coverage**: ≥ 90%

**Files to Test**:

1. `TabNavigatorStateTest.kt` (already in Phase 1 spec)
   - ✅ Tab selection
   - ✅ Navigation within tabs
   - ✅ Back press logic
   - ✅ State independence
   - ✅ StateFlow reactivity

2. `TabDefinitionTest.kt`
   ```kotlin
   class TabDefinitionTest {
       @Test fun `tab id must be unique`()
       @Test fun `tab must have root destination`()
       @Test fun `tab equals uses id`()
       @Test fun `tab hashCode uses id`()
   }
   ```

3. `TabNavigatorConfigTest.kt`
   ```kotlin
   class TabNavigatorConfigTest {
       @Test fun `requires non-empty tabs`()
       @Test fun `requires unique tab IDs`()
       @Test fun `initialTab must be in allTabs`()
       @Test fun `primaryTab must be in allTabs`()
       @Test fun `resolves initial and primary tabs correctly`()
   }
   ```

4. `BackPressHandlerTest.kt`
   ```kotlin
   class BackPressHandlerTest {
       @Test fun `parent delegates to child first`()
       @Test fun `parent handles if child returns false`()
       @Test fun `handles null child gracefully`()
   }
   ```

---

### Integration Tests (Phase 2 Compose)

**Target Coverage**: ≥ 85%

**Files to Test**:

1. `TabbedNavHostIntegrationTest.kt`
   ```kotlin
   @OptIn(ExperimentalTestApi::class)
   class TabbedNavHostIntegrationTest {
       @Test fun `renders selected tab content`()
       @Test fun `switches tabs on selection`()
       @Test fun `preserves state when switching tabs`()
       @Test fun `integrates with parent navigator`()
       @Test fun `handles back press correctly`()
       @Test fun `animates tab transitions`()
   }
   ```

2. `TabNavigationContainerTest.kt`
   ```kotlin
   @OptIn(ExperimentalTestApi::class)
   class TabNavigationContainerTest {
       @Test fun `keeps all tabs in composition`()
       @Test fun `shows only selected tab`()
       @Test fun `applies correct animations`()
       @Test fun `handles rapid tab switching`()
   }
   ```

3. `RememberTabNavigationTest.kt`
   ```kotlin
   @OptIn(ExperimentalTestApi::class)
   class RememberTabNavigationTest {
       @Test fun `rememberTabNavigatorState survives recomposition`()
       @Test fun `rememberTabNavigatorState saves state on config change`()
       @Test fun `rememberTabNavigator registers with parent`()
       @Test fun `rememberTabNavigator cleans up on dispose`()
   }
   ```

---

### UI Tests (Platform-Specific)

**Android** (`androidInstrumentedTest`):

```kotlin
@RunWith(AndroidJUnit4::class)
class TabbedNavigationUITest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun bottomNavigationTabsWorkCorrectly() {
        composeTestRule.setContent {
            MainTabsContainer(parentNavigator = rememberNavigator())
        }
        
        // Click Profile tab
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.onNodeWithText("Profile Screen").assertIsDisplayed()
        
        // Navigate within Profile tab
        composeTestRule.onNodeWithText("Edit Profile").performClick()
        composeTestRule.onNodeWithText("Edit Profile Screen").assertIsDisplayed()
        
        // Switch to Home tab (Profile state should be preserved)
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
        
        // Return to Profile tab (should still be on Edit screen)
        composeTestRule.onNodeWithText("Profile").performClick()
        composeTestRule.onNodeWithText("Edit Profile Screen").assertIsDisplayed()
    }
    
    @Test
    fun backPressFromTabNavigationWorks() {
        // Test back press logic through UI
    }
    
    @Test
    fun predictiveBackGestureWorks() {
        // Test Android 13+ predictive back
    }
    
    @Test
    fun deepLinkToTabContentWorks() {
        // Test deep linking to specific tab
    }
}
```

**iOS** (`iosTest` - if applicable):

```kotlin
class TabbedNavigationIOSTest {
    @Test
    fun swipeBackGestureWorks() {
        // Test iOS swipe-back gesture
    }
}
```

---

### KSP Generation Tests (Phase 3)

**Files to Test**:

1. `TabGraphExtractorTest.kt`
   ```kotlin
   class TabGraphExtractorTest {
       @Test fun `extracts @TabGraph annotation correctly`()
       @Test fun `validates sealed class requirement`()
       @Test fun `extracts all @Tab subclasses`()
       @Test fun `validates tab parameters`()
       @Test fun `finds @TabContent functions`()
       @Test fun `handles missing annotations gracefully`()
   }
   ```

2. `TabGraphGeneratorTest.kt`
   ```kotlin
   class TabGraphGeneratorTest {
       @Test fun `generates valid config object`()
       @Test fun `generates container composable`()
       @Test fun `generates graph builder function`()
       @Test fun `generates proper imports`()
       @Test fun `generates KDoc comments`()
       @Test fun `handles custom initial tab`()
   }
   ```

3. `KSPEndToEndTest.kt`
   ```kotlin
   class KSPEndToEndTest {
       @Test fun `generated code compiles successfully`()
       @Test fun `generated container works at runtime`()
       @Test fun `handles complex tab hierarchies`()
   }
   ```

---

### Performance Tests

**File**: `quo-vadis-core/src/androidInstrumentedTest/kotlin/.../TabNavigationPerformanceTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class TabNavigationPerformanceTest {
    
    @Test
    fun tabSwitchingMeetsPerformanceTarget() {
        val tabState = TabNavigatorState(testConfig)
        val times = mutableListOf<Long>()
        
        repeat(100) {
            val start = System.nanoTime()
            tabState.selectTab(testTabs[it % testTabs.size])
            val end = System.nanoTime()
            times.add(end - start)
        }
        
        val avgTime = times.average()
        val avgMillis = avgTime / 1_000_000.0
        
        // Target: <16ms (60fps)
        assertTrue("Tab switching too slow: ${avgMillis}ms", avgMillis < 16.0)
    }
    
    @Test
    fun navigationWithinTabMeetsPerformanceTarget() {
        // Similar test for navigation within tabs
    }
    
    @Test
    fun memoryUsageIsAcceptableWith8Tabs() {
        // Memory pressure test
        val runtime = Runtime.getRuntime()
        val before = runtime.totalMemory() - runtime.freeMemory()
        
        // Create 8 tabs with navigation
        val tabState = TabNavigatorState(configWith8Tabs)
        repeat(8) { tabIndex ->
            tabState.selectTab(testTabs[tabIndex])
            repeat(10) {
                tabState.navigateInTab(TestDestination(it))
            }
        }
        
        val after = runtime.totalMemory() - runtime.freeMemory()
        val usedMB = (after - before) / (1024.0 * 1024.0)
        
        // Target: <50MB for 8 tabs with 10 destinations each
        assertTrue("Memory usage too high: ${usedMB}MB", usedMB < 50.0)
    }
}
```

---

### Multiplatform Tests

**Common Tests** (`commonTest`):
- All Phase 1 core tests
- Logic tests that don't require UI

**Android Tests** (`androidInstrumentedTest`):
- Compose UI tests
- Performance tests
- Predictive back gesture tests
- Deep linking tests

**iOS Tests** (`iosTest`):
- Swipe-back gesture tests
- Navigation bar integration tests

**Desktop Tests** (`desktopTest`):
- Keyboard shortcut tests
- Window management tests

**Web Tests** (`jsTest`, `wasmJsTest`):
- Browser back button tests
- URL navigation tests

---

## Test Coverage Goals

| Component | Target | Critical |
|-----------|--------|----------|
| Core (Phase 1) | ≥ 90% | 100% |
| Compose (Phase 2) | ≥ 85% | ≥ 90% |
| KSP (Phase 3) | ≥ 80% | ≥ 85% |
| Integration | ≥ 80% | ≥ 85% |
| Overall | ≥ 85% | ≥ 90% |

**Critical paths** (must be 100% covered):
- Back press delegation logic
- Tab state management (selection, navigation)
- StateFlow updates
- Configuration validation

---

## Documentation Checklist

### Core Documentation
- [ ] `TABBED_NAVIGATION.md` - Complete architecture guide
- [ ] `API_REFERENCE.md` - Updated with all new APIs
- [ ] `MIGRATION_TO_TABBED_NAVIGATION.md` - Migration guide
- [ ] README updates in `quo-vadis-core` and `composeApp`

### Code Documentation
- [ ] All public APIs have comprehensive KDoc
- [ ] All annotations have usage examples in KDoc
- [ ] Generated code includes explanatory comments
- [ ] Complex algorithms documented inline

### Examples & Guides
- [ ] Quick start guide (5 minutes)
- [ ] Common patterns guide
- [ ] Advanced usage guide
- [ ] Troubleshooting guide
- [ ] Best practices guide

### Website Updates
- [ ] Update `docs/site/index.html` with tab navigation
- [ ] Add examples to documentation site
- [ ] Update feature list
- [ ] Add migration guide to website

---

## Quality Gates

Before marking Phase 5 complete, ALL must pass:

### Testing
- [ ] All unit tests pass on all platforms
- [ ] Test coverage ≥ 85% overall
- [ ] Critical paths 100% covered
- [ ] UI tests pass on Android
- [ ] Performance tests meet targets (<16ms, <50MB)
- [ ] No flaky tests

### Documentation
- [ ] All public APIs documented
- [ ] All documents reviewed for accuracy
- [ ] Code samples compile and run
- [ ] Migration guide tested with real migration
- [ ] Troubleshooting guide covers observed issues

### Code Quality
- [ ] No compilation warnings
- [ ] Detekt passes with no violations
- [ ] KDoc linter passes
- [ ] Generated code is clean and readable
- [ ] No TODOs or FIXMEs in production code

### Integration
- [ ] Demo app fully functional
- [ ] All navigation patterns work
- [ ] Deep linking works
- [ ] Back press behavior correct
- [ ] No performance regressions

---

## Files Summary

**New Documentation**:
```
quo-vadis-core/docs/
├── TABBED_NAVIGATION.md                      (~2,500 lines)
├── MIGRATION_TO_TABBED_NAVIGATION.md         (~1,000 lines)
└── API_REFERENCE.md                          (updated +500 lines)

composeApp/src/commonMain/kotlin/.../demo/
└── tabs/README.md                            (~400 lines)
```

**New Tests**:
```
quo-vadis-core/src/commonTest/kotlin/
├── TabDefinitionTest.kt                      (~200 lines)
├── TabNavigatorConfigTest.kt                 (~250 lines)
├── BackPressHandlerTest.kt                   (~150 lines)
└── (TabNavigatorStateTest.kt from Phase 1)

quo-vadis-core/src/androidInstrumentedTest/kotlin/
├── TabbedNavHostIntegrationTest.kt           (~400 lines)
├── TabNavigationContainerTest.kt             (~300 lines)
├── RememberTabNavigationTest.kt              (~250 lines)
├── TabbedNavigationUITest.kt                 (~500 lines)
└── TabNavigationPerformanceTest.kt           (~300 lines)

quo-vadis-ksp/src/test/kotlin/
├── TabGraphExtractorTest.kt                  (~400 lines)
├── TabGraphGeneratorTest.kt                  (~450 lines)
└── KSPEndToEndTest.kt                        (~300 lines)
```

**Total New Content**: ~8,400 lines

---

## Verification Steps

After completing Phase 5:

1. **Documentation Review**:
   ```bash
   # Check all docs exist and are complete
   ls -lh quo-vadis-core/docs/TABBED_NAVIGATION.md
   ls -lh quo-vadis-core/docs/MIGRATION_TO_TABBED_NAVIGATION.md
   ```

2. **Test Execution**:
   ```bash
   # Run all tests
   ./gradlew :quo-vadis-core:test
   ./gradlew :quo-vadis-core:connectedAndroidTest
   ./gradlew :quo-vadis-ksp:test
   ```

3. **Coverage Report**:
   ```bash
   # Generate coverage report
   ./gradlew :quo-vadis-core:koverHtmlReport
   open quo-vadis-core/build/reports/kover/html/index.html
   
   # Verify ≥85% coverage
   ```

4. **Performance Verification**:
   ```bash
   # Run performance tests
   ./gradlew :quo-vadis-core:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=...TabNavigationPerformanceTest
   
   # Check results:
   # - Tab switching: <16ms ✓
   # - Memory usage: <50MB for 8 tabs ✓
   ```

5. **Demo App Verification**:
   ```bash
   # Build and run demo
   ./gradlew :composeApp:assembleDebug
   ./gradlew :composeApp:run
   
   # Manual testing:
   # - Tab switching smooth ✓
   # - State preserved ✓
   # - Back press correct ✓
   # - Deep links work ✓
   ```

6. **Documentation Site**:
   ```bash
   # Build documentation
   ./gradlew :quo-vadis-core:dokkaGenerateHtml
   
   # Verify locally
   open quo-vadis-core/build/dokka/html/index.html
   ```

---

## Success Criteria

Phase 5 is complete when:

- ✅ **Documentation**: All 4 major documents complete and reviewed
- ✅ **Test Coverage**: ≥85% overall, 100% on critical paths
- ✅ **All Tests Pass**: On all platforms (Android, iOS, Desktop, Web)
- ✅ **Performance**: All benchmarks meet targets
- ✅ **Demo App**: Fully functional with tabs
- ✅ **Quality Gates**: All pass (no warnings, linting, etc.)
- ✅ **Website**: Updated with new features
- ✅ **Migration Guide**: Tested with real migration
- ✅ **User Acceptance**: Demo reviewed and approved

---

## Post-Phase 5 Checklist

Before final release:

- [ ] User documentation reviewed
- [ ] API freeze confirmed
- [ ] Breaking changes documented
- [ ] Changelog updated
- [ ] Version number incremented
- [ ] Release notes prepared
- [ ] Blog post drafted
- [ ] Sample apps updated
- [ ] Community announcement prepared
- [ ] Release branch created

---

**Status**: 🔴 Not Started

**Next Steps**: Implementation of Phase 1

**Depends On**: 
- Phase 1 (Core Foundation) ✅
- Phase 2 (Compose Integration) ✅
- Phase 3 (KSP Annotations) ✅
- Phase 4 (Demo App) ✅

**Completion**: This phase completes the entire tabbed navigation feature!
