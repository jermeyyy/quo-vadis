# Tab API Usage Inventory — Complete Code Audit

## Purpose

Complete inventory of ALL code that references the index-based tab API across both the **quo-vadis** library and the **peopay-kmp** consumer project. This supports the planned migration from an index-based tab API (`activeTabIndex`, `switchTab(index)`) to a NavDestination-based API.

---

## Part 1: quo-vadis Library

### 1.1 `TabsContainerScope` Interface

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/TabsContainerScope.kt` (lines 6–109)

```kotlin
@Stable
interface TabsContainerScope {
    val navigator: Navigator
    val activeTabIndex: Int          // ← INDEX-BASED (to be replaced)
    val tabCount: Int
    val tabs: List<NavDestination>
    val isTransitioning: Boolean
    fun switchTab(index: Int)        // ← INDEX-BASED (to be replaced)
}
```

**Properties/methods that will change:**
| Member | Current Signature | Impact |
|--------|-------------------|--------|
| `activeTabIndex` | `val activeTabIndex: Int` | Replace with destination-based active tab |
| `switchTab` | `fun switchTab(index: Int)` | Replace with `switchTab(destination: NavDestination)` |

### 1.2 `TabsContainerScopeImpl` 

**File:** Same file (lines 111–140)

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

All constructor parameters and `switchTab` implementation use indices.

### 1.3 `createTabsContainerScope` Factory

**File:** Same file (lines 142–167)

```kotlin
internal fun createTabsContainerScope(
    navigator: Navigator,
    activeTabIndex: Int,
    tabs: List<NavDestination>,
    isTransitioning: Boolean,
    onSwitchTab: (Int) -> Unit
): TabsContainerScope = TabsContainerScopeImpl(...)
```

### 1.4 `TabRenderer` — Scope Creation & Tab Switching

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/TabRenderer.kt` (lines 24–188)

Key section (scope creation inside `TabRenderer`):
```kotlin
val tabsContainerScope = remember(node.key, node.activeStackIndex, node.tabCount) {
    createTabsContainerScope(
        navigator = scope.navigator,
        activeTabIndex = node.activeStackIndex,     // ← uses index
        tabs = getTabDestinations(node),
        isTransitioning = false,
        onSwitchTab = { index ->                     // ← callback uses index
            val newState = TreeMutator.switchActiveTab(scope.navigator.state.value, index)
            scope.navigator.updateState(newState)
        }
    )
}
```

The `onSwitchTab` callback calls `TreeMutator.switchActiveTab(root, index)` which delegates to `TabOperations.switchTab(root, tabNodeKey, newIndex)`.

**Also in TabRenderer:**
- `getTabDestinations(node)` (lines 190–217) — extracts `List<NavDestination>` from stacks for pattern matching
- `findFirstScreenDestination(node)` (lines 219–235) — recursive helper

### 1.5 `TabNode` — The Navigation Tree Node

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/node/TabNode.kt` (lines 13–163)

Key index-based properties:
```kotlin
class TabNode(
    override val key: NodeKey,
    override val parentKey: NodeKey?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,    // ← the core index
    val wrapperKey: String? = null,
    val tabMetadata: List<GeneratedTabMetadata> = emptyList(),
    val scopeKey: ScopeKey? = null
)
```

- `activeStack: StackNode get() = stacks[activeStackIndex]`
- `tabCount: Int get() = stacks.size`
- `stackAt(index: Int): StackNode`
- `copy(activeStackIndex = ...)` used by TreeMutator

### 1.6 `TabOperations` — Tree Mutation

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/TabOperations.kt`

```kotlin
// Direct tab switch by key + index
fun switchTab(root: NavNode, tabNodeKey: NodeKey, newIndex: Int): NavNode

// Convenience: find first TabNode in active path, switch by index
fun switchActiveTab(root: NavNode, newIndex: Int): NavNode
```

### 1.7 `TreeMutator` — Delegates

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeMutator.kt` (lines 225–231)

```kotlin
fun switchActiveTab(root: NavNode, newIndex: Int): NavNode =
    TabOperations.switchActiveTab(root, newIndex)
```

### 1.8 Annotations

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`

```kotlin
// Lines 4–68
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Tabs(val name: String)

// Lines 70–128
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TabItem(
    val parent: KClass<*>,
    val ordinal: Int,          // ← ordinal determines index order
)
```

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabsContainer.kt` (lines 4–133)

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TabsContainer(val tabClass: KClass<*>)
```

### 1.9 KSP Processors

| File | Role |
|------|------|
| `quo-vadis-ksp/.../QuoVadisSymbolProcessor.kt` | Main processor. Calls `collectTabs()` and `collectWrappers()` |
| `quo-vadis-ksp/.../extractors/TabExtractor.kt` | Discovers `@TabItem`, groups by parent `@Tabs`, assembles `TabInfo` |
| `quo-vadis-ksp/.../extractors/ContainerExtractor.kt` | Discovers `@TabsContainer` annotations, extracts wrapper metadata |
| `quo-vadis-ksp/.../models/TabInfo.kt` | `TabInfo`, `TabItemInfo`, `TabItemType` data models |
| `quo-vadis-ksp/.../models/ContainerInfoModel.kt` | `ContainerInfoModel` for `@TabsContainer` metadata |
| `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt` | Generates container registry entries with wrapper functions |
| `quo-vadis-ksp/.../validation/ValidationEngine.kt` | Validates ordinals (zero exists, no collisions, continuity), circular nesting |

### 1.10 `ContainerRegistry` 

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/ContainerRegistry.kt`

The `TabsContainer(tabNodeKey, scope)` method is called by `TabRenderer` to invoke the wrapper composable. Internally maps `wrapperKey` → composable function.

### 1.11 Demo App Usages (quo-vadis)

| File | Usage |
|------|-------|
| `composeApp/.../tabs/MainTabsUI.kt` (line 61) | `@TabsContainer(MainTabs::class)` — uses `scope.activeTabIndex`, `scope.switchTab(index)`, `scope.tabs` |
| `composeApp/.../tabs/TabsDemoWrapper.kt` (line 50) | `@TabsContainer(DemoTabs::class)` — uses `scope.activeTabIndex`, `scope.switchTab(index)` |
| `composeApp/.../statedriven/StateDrivenDemoScreen.kt` (line 64) | `@TabsContainer(StateDrivenDemoDestination.Companion::class)` — uses `scope.activeTabIndex`, `scope.switchTab` |
| `navigation-api/.../MainTabs.kt` (line 17) | `@Tabs(name = "mainTabs")` |
| `navigation-api/.../TabsDestination.kt` (lines 21–60) | `@Tabs(name = "demoTabs")` with 3 `@TabItem` children |
| `navigation-api/.../StateDrivenDemoDestination.kt` (line 67) | `@Tabs(name = "stateDrivenDemo")` with 1 `@TabItem` |
| `navigation-api/.../HomeTabDestination.kt` (line 9) | `@TabItem(parent = MainTabs::class, ordinal = 0)` |
| `navigation-api/.../ExploreTabDestination.kt` (line 11) | `@TabItem(parent = MainTabs::class, ordinal = 1)` |
| `navigation-api/.../ProfileTabDestination.kt` (line 9) | `@TabItem(parent = MainTabs::class, ordinal = 2)` |
| `navigation-api/.../SettingsTabDestination.kt` (line 10) | `@TabItem(parent = MainTabs::class, ordinal = 3)` |
| `navigation-api/.../ShowcaseTabDestination.kt` (line 17) | `@TabItem(parent = MainTabs::class, ordinal = 4)` |

### 1.12 Tests That Need Updating

| Test File | Description |
|-----------|-------------|
| `quo-vadis-core/src/commonTest/.../TreeMutatorTabTest.kt` | ~430 lines. Tests `switchTab`, `switchActiveTab` with indices |
| `quo-vadis-core/src/commonTest/.../TabRendererTest.kt` | Tests tab destination extraction, scope creation |
| `quo-vadis-core/src/commonTest/.../TabOperationsTest.kt` | Tests `switchTab(root, key, index)` and `switchActiveTab(root, index)` |
| `quo-vadis-core/src/commonTest/.../TabsBuilderTest.kt` | Tests DSL tab builder |
| `quo-vadis-ksp/src/test/.../TabItemTypeTest.kt` | Tests `TabItemType` enum |
| `quo-vadis-ksp/src/test/.../ContainerBlockGeneratorTabEntryTest.kt` | Tests container block code generation |
| `quo-vadis-ksp/src/test/.../ValidationEngineOrdinalTest.kt` | Tests ordinal validation |

### 1.13 Testing Utilities

No testing utilities (`FakeNavigator`, `FakeNavRenderScope`) reference `activeTabIndex`, `switchTab`, or `TabsContainerScope` directly.

---

## Part 2: peopay-kmp Project

### 2.1 `@Tabs` Containers (2 total)

| Location | Declaration |
|----------|-------------|
| `feature-main-navigation-api/.../MainNavigation.kt` | `@Tabs(name = "mainNavigationTabs") data object MainNavigation : NavDestination` |
| `feature-developer-console/.../QuickActivationTabsDestination.kt` | `@Tabs(name = "quick_activation") internal sealed class QuickActivationTabsDestination` |

### 2.2 `@TabItem` Declarations (7 total)

| File | Annotation | Ordinal | Type |
|------|-----------|---------|------|
| `feature-dashboard/.../DashboardDestination.kt` | `@TabItem(parent = MainNavigation::class, ordinal = 0)` | 0 | `@Destination` |
| `feature-dashboard/.../DashboardDestination.kt` | `@TabItem(parent = MainNavigation::class, ordinal = 1)` | 1 | `@Destination` (Transfers) |
| `feature-dashboard/.../DashboardDestination.kt` | `@TabItem(parent = MainNavigation::class, ordinal = 2)` | 2 | `@Destination` (Finance) |
| `feature-investments/.../InvestmentsDestination.kt` | `@TabItem(parent = MainNavigation::class, ordinal = 3)` | 3 | `@Destination` (InvestmentsTab) |
| `feature-dashboard/.../DashboardDestination.kt` | `@TabItem(parent = MainNavigation::class, ordinal = 4)` | 4 | `@Destination` (Benefits) |
| `feature-developer-console/.../QuickActivationTabsDestination.kt` | `@TabItem(parent = QuickActivationTabsDestination::class, ordinal = 0)` | 0 | `@Destination` (Form) |
| `feature-developer-console/.../QuickActivationTabsDestination.kt` | `@TabItem(parent = QuickActivationTabsDestination::class, ordinal = 1)` | 1 | `@Destination` (LastUsed) |

### 2.3 `@TabsContainer` Wrappers (2 total)

#### A. `DashboardScreen` — Main Navigation Wrapper

**File:** `feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/internal/presentation/dashboard/DashboardScreen.kt`

```kotlin
@TabsContainer(MainNavigation::class)
@Composable
internal fun DashboardScreen(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) { ... }
```

**Index-based usages inside this file:**

| Line | Code | Description |
|------|------|-------------|
| 87 | `dashboardBottomTabs[scope.activeTabIndex]` | Maps tab index → `DashboardBottomTab` for top navigation title |
| 94 | `dashboardBottomTabs[scope.activeTabIndex]` | Maps tab index → `DashboardBottomTab` for bottom nav selection |
| 194 | `scope.switchTab(tabs.indexOf(tab))` | Converts selected `BottomNavigationTab` back to index for switching |

**Supporting types:**
- `DashboardBottomTab` (sealed class at `feature-dashboard/.../tabs/DashboardBottomTab.kt`) — 5 tabs: Home, Finance, Transfers, Investments, Benefits
- `dashboardBottomTabs` — ordered list: `[Home, Finance, Transfers, Investments, Benefits]`
- **WARNING:** The `dashboardBottomTabs` list order (Home=0, Finance=1, Transfers=2, Investments=3, Benefits=4) matches ordinals by position, NOT by ordinal value. Note that `@TabItem` ordinals are 0,1,2,3,4 but map to DashboardDestination, Transfers, Finance, InvestmentsTab, Benefits — there's a potential **index mismatch** between `dashboardBottomTabs` ordering and `@TabItem` ordinal ordering that the new API should make explicit.

#### B. `QuickActivationTabContainer` — Developer Console Tabs

**File:** `feature-developer-console/src/commonMain/kotlin/com/pekao/peopay/feature/developerConsole/internal/ui/quickActivation/QuickActivationTabContainer.kt`

```kotlin
@TabsContainer(QuickActivationTabsDestination::class)
@Composable
internal fun QuickActivationTabContainer(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) { ... }
```

**Index-based usages inside this file:**

| Line | Code | Description |
|------|------|-------------|
| 63 | `selectedTabIndex = scope.activeTabIndex` | `SecondaryTabRow` selection state |
| 67 | `selected = scope.activeTabIndex == index` | Per-tab selected state check |
| 68 | `scope.switchTab(index)` | Tab click handler |

### 2.4 `TabsContainerScope` Type References

| File | Type | Usage |
|------|------|-------|
| `DashboardScreen.kt` line 28 | Import | `import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope` |
| `DashboardScreen.kt` line 72 | Parameter | `scope: TabsContainerScope` |
| `DashboardScreen.kt` line 186 | Parameter | `scope: TabsContainerScope` (in `DashboardBottomNavigationBar`) |
| `QuickActivationTabContainer.kt` line 23 | Import | `import com.jermey.quo.vadis.core.compose.scope.TabsContainerScope` |
| `QuickActivationTabContainer.kt` line 34 | Parameter | `scope: TabsContainerScope` |

### 2.5 Tests Referencing Tab API

**None.** No test files in peopay-kmp reference `TabsContainerScope`, `activeTabIndex`, `switchTab`, or `@TabsContainer`.

---

## Part 3: Summary — All Impacted Locations

### Library (quo-vadis) — Files to Modify

| # | File | Changes Required |
|---|------|-----------------|
| 1 | `TabsContainerScope.kt` | Replace `activeTabIndex: Int` with destination-based property; change `switchTab(Int)` signature |
| 2 | `TabsContainerScopeImpl` (same file) | Update constructor, implementation |
| 3 | `createTabsContainerScope` (same file) | Update factory parameters |
| 4 | `TabRenderer.kt` | Update scope creation to pass destination-based values; update `onSwitchTab` callback |
| 5 | `TabNode.kt` | `activeStackIndex` stays (internal), but add destination-based lookup utilities |
| 6 | `TabOperations.kt` | Add destination-based `switchTab` variant |
| 7 | `TreeMutator.kt` | Expose new destination-based method |
| 8 | `TabAnnotations.kt` | `@TabItem.ordinal` stays (internal ordering), no change needed |
| 9 | `TabsContainer.kt` (annotation) | No change needed |
| 10 | `ContainerRegistry.kt` | No change needed (passes scope transparently) |
| 11 | Demo: `MainTabsUI.kt` | Update to use new API |
| 12 | Demo: `TabsDemoWrapper.kt` | Update to use new API |
| 13 | Demo: `StateDrivenDemoScreen.kt` | Update to use new API |

### Library (quo-vadis) — Tests to Update

| # | Test File |
|---|-----------|
| 1 | `TreeMutatorTabTest.kt` |
| 2 | `TabRendererTest.kt` |
| 3 | `TabOperationsTest.kt` |
| 4 | `TabsBuilderTest.kt` |
| 5 | `TabItemTypeTest.kt` (possibly) |
| 6 | `ContainerBlockGeneratorTabEntryTest.kt` (possibly) |
| 7 | `ValidationEngineOrdinalTest.kt` (possibly) |

### Consumer (peopay-kmp) — Files to Modify

| # | File | Changes Required |
|---|------|-----------------|
| 1 | `DashboardScreen.kt` | Replace `scope.activeTabIndex` with destination-based lookup; replace `scope.switchTab(index)` with destination-based switch; update `DashboardBottomNavigationBar` |
| 2 | `QuickActivationTabContainer.kt` | Replace `scope.activeTabIndex` with destination-based lookup; replace `scope.switchTab(index)` with destination-based switch |
| 3 | `DashboardBottomTab.kt` | No structural change, but the index-mapping logic (`dashboardBottomTabs[scope.activeTabIndex]`) moves to destination-based mapping |

### Consumer (peopay-kmp) — No Test Impact

No consumer tests reference the tab API.

---

## Part 4: Critical Observations

### Index-Ordinal Mapping Risk in DashboardScreen

The `DashboardScreen` maps `scope.activeTabIndex` → `dashboardBottomTabs[index]` to get the active `DashboardBottomTab`. The `dashboardBottomTabs` list is:
```
[Home(ord=0), Finance(ord=2), Transfers(ord=1), Investments(ord=3), Benefits(ord=4)]
```

But `@TabItem` ordinals are: DashboardDestination=0, Transfers=1, Finance=2, InvestmentsTab=3, Benefits=4.

The `dashboardBottomTabs` list ORDER is `[Home, Finance, Transfers, Investments, Benefits]` but the ORDINAL order is `[Home=0, Transfers=1, Finance=2, Investments=3, Benefits=4]`. This means:
- **Tab index 1** shows **Transfers** (ordinal 1), but `dashboardBottomTabs[1]` is **Finance**
- This is a **potential bug** in the current codebase, or the `dashboardBottomTabs` list ordering intentionally doesn't follow ordinal order

**The new destination-based API would eliminate this class of bug entirely.**

### Scope of Change

- **2 @TabsContainer wrappers** in peopay-kmp need updating
- **3 @TabsContainer wrappers** in the quo-vadis demo app need updating
- **Core interface change** (`TabsContainerScope`) affects all wrappers automatically
- `@Tabs` and `@TabItem` annotations do NOT need changing (ordinals are internal)
- `TabNode.activeStackIndex` can remain as an internal detail
