# Implementation Plan: Destination-Based Tab API

## Summary

Replace the index-based tab API (`activeTabIndex: Int`, `switchTab(Int)`) with a NavDestination-based API (`activeTab: NavDestination`, `switchTab(NavDestination)`) and remove `ordinal` from the `@TabItem` annotation. This eliminates the cross-module merge ordering bug by delegating all tab ordering/mapping to the `@TabsContainer` consumer.

**Type:** BREAKING CHANGE  
**Scope:** quo-vadis library + all consumers (peopay-kmp)  
**Root cause addressed:** `CompositeNavigationConfig.mergeTabNodes()` concatenates tabs without respecting ordinal order — rather than fixing the ordinal pipeline, we remove ordinals entirely and let the consumer handle ordering.

---

## Task Checklist

### Phase 1: Annotation Changes (quo-vadis)

- [ ] **1.1** Remove `ordinal: Int` from `@TabItem` annotation
  - **File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`
  - Before:
    ```kotlin
    annotation class TabItem(
        val parent: KClass<*>,
        val ordinal: Int,
    )
    ```
  - After:
    ```kotlin
    annotation class TabItem(
        val parent: KClass<*>,
    )
    ```

---

### Phase 2: KSP Processor Changes (quo-vadis)

- [ ] **2.1** Remove ordinal from `TabItemInfo` model
  - **File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`
  - Remove the `ordinal: Int` field from `TabItemInfo`

- [ ] **2.2** Remove ordinal extraction from `TabExtractor`
  - **File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
  - In `extractAllTabItems()`: remove reading the `ordinal` argument from the annotation
  - In `extractAllTabs()`: remove `sortedBy { ordinal }` — tabs are now in declaration/discovery order
  - In `extractTabItem()`: remove ordinal parameter

- [ ] **2.3** Remove ordinal sorting from `ContainerBlockGenerator`
  - **File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`
  - In `generateTabsBlock()`: remove `tab.tabs.sortedBy { it.ordinal }` — iterate `tab.tabs` directly
  - No other changes needed (ordinal was never emitted into generated `tab()` calls)

- [ ] **2.4** Remove ordinal validations from `ValidationEngine`
  - **File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
  - Remove: zero-ordinal-exists validation
  - Remove: ordinal-collision validation
  - Remove: ordinal-continuity validation
  - Keep: circular nesting validation (unrelated to ordinals)

- [ ] **2.5** Remove or gut `ValidationEngineOrdinalTest`
  - **File:** `quo-vadis-ksp/src/test/.../ValidationEngineOrdinalTest.kt`
  - Delete the file or remove all ordinal-specific test cases

- [ ] **2.6** Update `ContainerBlockGeneratorTabEntryTest`
  - **File:** `quo-vadis-ksp/src/test/.../ContainerBlockGeneratorTabEntryTest.kt`
  - Remove ordinal from test `TabItemInfo` construction

---

### Phase 3: Core Library — Public API Changes (quo-vadis)

- [ ] **3.1** Replace index-based API in `TabsContainerScope` interface
  - **File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/TabsContainerScope.kt`
  - Before:
    ```kotlin
    @Stable
    interface TabsContainerScope {
        val navigator: Navigator
        val activeTabIndex: Int
        val tabCount: Int
        val tabs: List<NavDestination>
        val isTransitioning: Boolean
        fun switchTab(index: Int)
    }
    ```
  - After:
    ```kotlin
    @Stable
    interface TabsContainerScope {
        val navigator: Navigator
        val activeTab: NavDestination
        val tabs: Set<NavDestination>
        val isTransitioning: Boolean
        fun switchTab(destination: NavDestination)
    }
    ```
  - **Removed `tabCount`** — redundant, consumers use `tabs.size` instead
  - **Changed `tabs` from `List` to `Set`** — tabs are an unordered collection of destinations; the consumer controls display ordering

- [ ] **3.2** Update `TabsContainerScopeImpl`
  - **File:** same as 3.1
  - Before:
    ```kotlin
    internal class TabsContainerScopeImpl(
        override val navigator: Navigator,
        override val activeTabIndex: Int,
        override val tabCount: Int,
        override val tabs: List<NavDestination>,
        override val isTransitioning: Boolean,
        private val onSwitchTab: (Int) -> Unit
    ) : TabsContainerScope {
        override fun switchTab(index: Int) {
            require(index in 0 until tabCount) { "Tab index $index out of bounds [0, $tabCount)" }
            onSwitchTab(index)
        }
    }
    ```
  - After:
    ```kotlin
    internal class TabsContainerScopeImpl(
        override val navigator: Navigator,
        override val activeTab: NavDestination,
        override val tabs: Set<NavDestination>,
        override val isTransitioning: Boolean,
        private val onSwitchTab: (NavDestination) -> Unit
    ) : TabsContainerScope {
        override fun switchTab(destination: NavDestination) {
            require(destination in tabs) {
                "Destination $destination is not a tab in this container"
            }
            onSwitchTab(destination)
        }
    }
    ```
  - **Removed `tabCount` constructor parameter** — no longer in interface
  - **Changed `tabs` to `Set<NavDestination>`**

- [ ] **3.3** Update `createTabsContainerScope` factory
  - **File:** same as 3.1
  - Before:
    ```kotlin
    internal fun createTabsContainerScope(
        navigator: Navigator,
        activeTabIndex: Int,
        tabs: List<NavDestination>,
        isTransitioning: Boolean,
        onSwitchTab: (Int) -> Unit
    ): TabsContainerScope
    ```
  - After:
    ```kotlin
    internal fun createTabsContainerScope(
        navigator: Navigator,
        activeTab: NavDestination,
        tabs: Set<NavDestination>,
        isTransitioning: Boolean,
        onSwitchTab: (NavDestination) -> Unit
    ): TabsContainerScope
    ```

---

### Phase 4: Core Library — Internal Plumbing (quo-vadis)

- [ ] **4.1** Update `TabRenderer` scope creation
  - **File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/TabRenderer.kt`
  - Before:
    ```kotlin
    val tabsContainerScope = remember(node.key, node.activeStackIndex, node.tabCount) {
        createTabsContainerScope(
            navigator = scope.navigator,
            activeTabIndex = node.activeStackIndex,
            tabs = getTabDestinations(node),
            isTransitioning = false,
            onSwitchTab = { index ->
                val newState = TreeMutator.switchActiveTab(scope.navigator.state.value, index)
                scope.navigator.updateState(newState)
            }
        )
    }
    ```
  - After:
    ```kotlin
    val tabDestinations = getTabDestinations(node)
    val activeTab = tabDestinations[node.activeStackIndex]

    val tabDestinationSet = tabDestinations.toSet()
    val tabsContainerScope = remember(node.key, node.activeStackIndex, tabDestinationSet.size) {
        createTabsContainerScope(
            navigator = scope.navigator,
            activeTab = activeTab,
            tabs = tabDestinationSet,
            isTransitioning = false,
            onSwitchTab = { destination ->
                val index = tabDestinations.indexOf(destination)
                require(index >= 0) { "Destination $destination not found in tabs" }
                val newState = TreeMutator.switchActiveTab(scope.navigator.state.value, index)
                scope.navigator.updateState(newState)
            }
        )
    }
    ```
  - **Key design:** The translation from `NavDestination` → index happens here in `TabRenderer`, keeping `TreeMutator`/`TabOperations` index-based internally.

- [ ] **4.2** `TabNode` — **NO CHANGE**
  - `activeStackIndex: Int` remains as internal implementation detail
  - The index is used throughout the tree structure internally

- [ ] **4.3** `TabOperations` / `TreeMutator` — **NO CHANGE**
  - Internal tree mutation stays index-based
  - Translation between public API (NavDestination) and internal API (index) happens in `TabRenderer`

- [ ] **4.4** `CompositeNavigationConfig.mergeTabNodes()` — **NO CHANGE NEEDED**
  - With ordinals removed, there is no ordinal-based sorting to do
  - Tab order = primary config tabs first, then additional tabs appended
  - Order depends on config composition order, which is acceptable since the `@TabsContainer` consumer controls all ordering/mapping via destination matching

- [ ] **4.5** `GeneratedTabMetadata` — verify no ordinal field exists
  - **File:** `quo-vadis-core/.../navigation/internal/GeneratedTabMetadata.kt`
  - Currently only has `route: String` — confirm no ordinal was added; no change needed

---

### Phase 5: Demo App Updates (quo-vadis)

- [ ] **5.1** Update `MainTabsUI.kt`
  - **File:** `composeApp/.../tabs/MainTabsUI.kt`
  - Replace `scope.activeTabIndex` → destination-based logic (e.g., `scope.activeTab == HomeTabDestination`)
  - Replace `scope.switchTab(index)` → `scope.switchTab(destination)`
  - Example migration:
    ```kotlin
    // Before
    val selectedIndex = scope.activeTabIndex
    scope.switchTab(2)

    // After
    val isSelected = scope.activeTab == SomeDestination
    scope.switchTab(SomeDestination)
    ```

- [ ] **5.2** Update `TabsDemoWrapper.kt`
  - **File:** `composeApp/.../tabs/TabsDemoWrapper.kt`
  - Same pattern as 5.1

- [ ] **5.3** Update `StateDrivenDemoScreen.kt`
  - **File:** `composeApp/.../statedriven/StateDrivenDemoScreen.kt`
  - Same pattern as 5.1

- [ ] **5.4** Remove `ordinal` from demo `@TabItem` annotations
  - **Files:**
    - `navigation-api/.../HomeTabDestination.kt` — `@TabItem(parent = MainTabs::class, ordinal = 0)` → `@TabItem(parent = MainTabs::class)`
    - `navigation-api/.../ExploreTabDestination.kt` — remove `ordinal = 1`
    - `navigation-api/.../ProfileTabDestination.kt` — remove `ordinal = 2`
    - `navigation-api/.../SettingsTabDestination.kt` — remove `ordinal = 3`
    - `navigation-api/.../ShowcaseTabDestination.kt` — remove `ordinal = 4`
    - `navigation-api/.../TabsDestination.kt` — remove ordinals from `@TabItem` children
    - `navigation-api/.../StateDrivenDemoDestination.kt` — remove ordinal from `@TabItem`

---

### Phase 6: Test Updates (quo-vadis)

- [ ] **6.1** `TabRendererTest.kt` — update for new scope API
  - Any assertions on `activeTabIndex` → `activeTab`
  - Any `switchTab(index)` calls → `switchTab(destination)`

- [ ] **6.2** `TreeMutatorTabTest.kt` — **likely no change**
  - These test internal `TreeMutator.switchActiveTab(root, index)` which remains index-based
  - Only update if tests create/assert on `TabsContainerScope`

- [ ] **6.3** `TabOperationsTest.kt` — **likely no change**
  - Tests internal `TabOperations.switchTab(root, key, index)` which remains index-based

- [ ] **6.4** `TabsBuilderTest.kt` — **likely no change**
  - No ordinal references existed in DSL builder

- [ ] **6.5** `ContainerBlockGeneratorTabEntryTest.kt` — remove ordinal from test data
  - Update `TabItemInfo` construction to omit `ordinal`

- [ ] **6.6** `ValidationEngineOrdinalTest.kt` — remove entirely
  - All test cases validate ordinal rules that no longer exist

---

### Phase 7: Consumer Migration — peopay-kmp

- [ ] **7.1** Update `@TabItem` annotations — remove `ordinal`
  - `feature-dashboard/.../DashboardDestination.kt`:
    - `@TabItem(parent = MainNavigation::class, ordinal = 0)` → `@TabItem(parent = MainNavigation::class)` (DashboardDestination)
    - `@TabItem(parent = MainNavigation::class, ordinal = 1)` → `@TabItem(parent = MainNavigation::class)` (Transfers)
    - `@TabItem(parent = MainNavigation::class, ordinal = 2)` → `@TabItem(parent = MainNavigation::class)` (Finance)
    - `@TabItem(parent = MainNavigation::class, ordinal = 4)` → `@TabItem(parent = MainNavigation::class)` (Benefits)
  - `feature-investments/.../InvestmentsDestination.kt`:
    - `@TabItem(parent = MainNavigation::class, ordinal = 3)` → `@TabItem(parent = MainNavigation::class)`
  - `feature-developer-console/.../QuickActivationTabsDestination.kt`:
    - Remove `ordinal = 0` and `ordinal = 1` from both `@TabItem` annotations

- [ ] **7.2** Update `DashboardScreen.kt` — main tab container
  - **File:** `feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/internal/presentation/dashboard/DashboardScreen.kt`
  - **Current pattern (index-based):**
    ```kotlin
    // Line 87 — maps index to bottom tab type
    dashboardBottomTabs[scope.activeTabIndex]
    
    // Line 94 — maps index to bottom tab for selection
    dashboardBottomTabs[scope.activeTabIndex]
    
    // Line 194 — converts selected tab back to index
    scope.switchTab(tabs.indexOf(tab))
    ```
  - **New pattern (destination-based):**
    ```kotlin
    // Create a mapping from NavDestination → DashboardBottomTab
    // This is where the consumer explicitly controls ordering/mapping
    val activeBottomTab = destinationToBottomTab(scope.activeTab)
    
    // Switch by destination directly
    scope.switchTab(bottomTabToDestination(tab))
    ```
  - **Key benefit:** The `dashboardBottomTabs` index-to-ordinal mismatch bug (identified in the usage inventory) is eliminated — mapping is now explicit by destination type, not positional.

- [ ] **7.3** Update `QuickActivationTabContainer.kt`
  - **File:** `feature-developer-console/src/commonMain/kotlin/com/pekao/peopay/feature/developerConsole/internal/ui/quickActivation/QuickActivationTabContainer.kt`
  - **Current pattern:**
    ```kotlin
    selectedTabIndex = scope.activeTabIndex           // line 63
    selected = scope.activeTabIndex == index           // line 67
    scope.switchTab(index)                              // line 68
    ```
  - **New pattern:**
    ```kotlin
    // Map tabs to their known destinations for selection
    val selectedTab = scope.activeTab
    selected = scope.tabs[index] == selectedTab
    scope.switchTab(scope.tabs[index])
    
    // Or better — use destination types directly:
    selected = scope.activeTab == FormDestination
    scope.switchTab(FormDestination)
    ```

- [ ] **7.4** No test updates needed in peopay-kmp
  - No existing tests reference `TabsContainerScope`, `activeTabIndex`, or `switchTab`

---

## Architecture Decision

### Why Remove Ordinals Instead of Fixing the Pipeline?

The original analysis (see [quo-vadis-tab-merge-analysis.md](quo-vadis-tab-merge-analysis.md)) identified Option A (propagate ordinals through the full pipeline) as the recommended fix. This plan takes a **different approach** — removing ordinals entirely — for these reasons:

| Concern | Ordinal Pipeline Fix | Remove Ordinals (this plan) |
|---------|---------------------|-----------------------------|
| Cross-module ordering | Fixed by sorting in `mergeTabNodes` | Consumer handles ordering explicitly |
| Ordinal collision detection | Needs runtime detection (KSP can't see cross-module) | No ordinals = no collisions |
| API complexity | Adds `ordinal` to `TabEntry`, `TabsBuilder.tab()`, `GeneratedTabMetadata` | Removes `ordinal` from annotation; simplifies KSP |
| Consumer flexibility | Consumer still index-based, fragile mapping | Consumer maps by destination type — explicit and type-safe |
| Index mismatch bugs | Still possible (consumer uses `tabsArray[index]`) | Eliminated (no indices in public API) |

### The Key Insight

The `@TabsContainer` wrapper (e.g., `DashboardScreen`) **already knows its tab destinations** — it renders UI for each one with icons, labels, and click handlers. Giving it `activeTab: NavDestination` instead of `activeTabIndex: Int` lets it match directly against destination types, eliminating an entire category of index-mapping bugs.

---

## Internal vs. Public API Boundary

```
┌─────────────────────────────────────────────────────┐
│  PUBLIC API (TabsContainerScope)                     │
│                                                      │
│  activeTab: NavDestination                           │
│  switchTab(destination: NavDestination)               │
│  tabs: Set<NavDestination>                            │
│                                                      │
├─────────────────────────────────────────────────────┤
│  TRANSLATION LAYER (TabRenderer)                     │
│                                                      │
│  activeTab = tabDestinations[node.activeStackIndex]  │
│  onSwitchTab = { dest ->                             │
│      index = tabDestinations.indexOf(dest)           │
│      TreeMutator.switchActiveTab(root, index)        │
│  }                                                   │
│                                                      │
├─────────────────────────────────────────────────────┤
│  INTERNAL API (TabNode, TreeMutator, TabOperations)  │
│                                                      │
│  activeStackIndex: Int                                │
│  switchTab(root, key, index: Int)                    │
│  switchActiveTab(root, index: Int)                   │
│                                                      │
│  (unchanged — stays index-based)                     │
└─────────────────────────────────────────────────────┘
```

---

## Sequencing & Dependencies

```
Phase 1 ──► Phase 2 ──► Phase 3 ──► Phase 4 ──► Phase 5
  │              │           │           │          │
  │              │           │           │          └── Demo app compiles
  │              │           │           └── TabRenderer bridges APIs
  │              │           └── Public interface defined
  │              └── KSP generates without ordinals
  └── Annotation simplified
                                                    │
                                              Phase 6 ──► Phase 7
                                                │           │
                                                │           └── peopay-kmp migrated
                                                └── Library tests pass
```

**Phases 1–4** must be sequential (each builds on the previous).  
**Phase 5** (demo apps) and **Phase 6** (tests) can be done in parallel after Phase 4.  
**Phase 7** (peopay-kmp migration) requires the library to be published with the new API.

---

## Migration Guide for Consumers

### Step 1: Update `@TabItem` annotations

```kotlin
// Before
@TabItem(parent = MainNavigation::class, ordinal = 0)
data object DashboardDestination : NavDestination

// After
@TabItem(parent = MainNavigation::class)
data object DashboardDestination : NavDestination
```

### Step 2: Update `@TabsContainer` implementations

```kotlin
// Before
@TabsContainer(MainNavigation::class)
@Composable
fun MyTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    val selectedIndex = scope.activeTabIndex
    val tabs = myTabList
    
    BottomNavigation {
        tabs.forEachIndexed { index, tab ->
            BottomNavigationItem(
                selected = index == selectedIndex,
                onClick = { scope.switchTab(index) }
            )
        }
    }
    content()
}

// After
@TabsContainer(MainNavigation::class)
@Composable
fun MyTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    val activeTab = scope.activeTab
    
    BottomNavigation {
        myTabConfigs.forEach { config ->
            BottomNavigationItem(
                selected = activeTab == config.destination,
                onClick = { scope.switchTab(config.destination) }
            )
        }
    }
    content()
}
```

### Step 3: Tab ordering is now consumer-controlled

The order of tabs in `scope.tabs` depends on config composition order and is **not guaranteed**. The `@TabsContainer` wrapper should define its own display order:

```kotlin
// Define explicit display order in the consumer
val orderedTabs = listOf(
    DashboardDestination,
    TransfersDestination,
    FinanceDestination,
    InvestmentsDestination,
    BenefitsDestination
)

// Use orderedTabs for rendering, scope.tabs only for querying available tabs
// scope.switchTab(destination) for switching
```

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking change for all consumers | High | Clear migration guide; coordinate with peopay-kmp team |
| `scope.tabs` order not deterministic across builds | Medium | Document that consumers should define their own display order |
| `indexOf(destination)` in TabRenderer returns -1 | Low | `require(index >= 0)` with descriptive error message |
| Demo apps break during development | Low | Update demo apps in Phase 5 before publishing |
| Consumer forgets to update after library bump | High | Compile error — `activeTabIndex` and `switchTab(Int)` no longer exist |

---

## Files Changed Summary

### quo-vadis library

| File | Change Type |
|------|-------------|
| `quo-vadis-annotations/.../TabAnnotations.kt` | Remove `ordinal` field |
| `quo-vadis-ksp/.../models/TabInfo.kt` | Remove `ordinal` from `TabItemInfo` |
| `quo-vadis-ksp/.../extractors/TabExtractor.kt` | Remove ordinal extraction and sorting |
| `quo-vadis-ksp/.../generators/dsl/ContainerBlockGenerator.kt` | Remove ordinal sorting |
| `quo-vadis-ksp/.../validation/ValidationEngine.kt` | Remove ordinal validations |
| `quo-vadis-core/.../compose/scope/TabsContainerScope.kt` | Replace index API with destination API |
| `quo-vadis-core/.../compose/internal/render/TabRenderer.kt` | Bridge destination API to internal index API |
| `composeApp/.../tabs/MainTabsUI.kt` | Migrate to destination API |
| `composeApp/.../tabs/TabsDemoWrapper.kt` | Migrate to destination API |
| `composeApp/.../statedriven/StateDrivenDemoScreen.kt` | Migrate to destination API |
| `navigation-api/...` (5+ destination files) | Remove `ordinal` from `@TabItem` |
| `quo-vadis-ksp/src/test/.../ValidationEngineOrdinalTest.kt` | Delete or gut |
| `quo-vadis-ksp/src/test/.../ContainerBlockGeneratorTabEntryTest.kt` | Remove ordinal from test data |
| `quo-vadis-core/src/commonTest/.../TabRendererTest.kt` | Update for new scope API |

### peopay-kmp consumer

| File | Change Type |
|------|-------------|
| `feature-dashboard/.../DashboardDestination.kt` | Remove ordinals from 4 `@TabItem` |
| `feature-investments/.../InvestmentsDestination.kt` | Remove ordinal from 1 `@TabItem` |
| `feature-developer-console/.../QuickActivationTabsDestination.kt` | Remove ordinals from 2 `@TabItem` |
| `feature-dashboard/.../DashboardScreen.kt` | Rewrite index-based tab logic to destination-based |
| `feature-developer-console/.../QuickActivationTabContainer.kt` | Rewrite index-based tab logic to destination-based |

### Files NOT changed (internal, stays index-based)

| File | Reason |
|------|--------|
| `TabNode.kt` | `activeStackIndex` is internal tree detail |
| `TabOperations.kt` | Internal tree mutation |
| `TreeMutator.kt` | Internal tree mutation |
| `CompositeNavigationConfig.kt` | Merge logic unchanged; no ordinals to sort |
| `DslNavigationConfig.kt` | Builds from list position (unchanged) |
| `TabsBuilder.kt` / `TabEntry` | No ordinal was ever in DSL; no change needed |
| `GeneratedTabMetadata.kt` | Only has `route`; no change needed |
