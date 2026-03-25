# Implementation Plan: peopay-kmp Consumer Changes for Destination-Based Tab API

## Summary

Adapt peopay-kmp to the new quo-vadis API that replaces index-based tab selection with destination-based selection:

| Old API | New API |
|---------|---------|
| `scope.activeTabIndex: Int` | `scope.activeTab: NavDestination` |
| `scope.switchTab(index: Int)` | `scope.switchTab(destination: NavDestination)` |
| `@TabItem(parent = X::class, ordinal = N)` | `@TabItem(parent = X::class)` |

`scope.tabs: Set<NavDestination>` remains available.

---

## Scope

- **7 `@TabItem` annotations** — remove `ordinal` parameter
- **2 `@TabsContainer` composables** — rewrite to use `scope.activeTab` / `scope.switchTab(destination)`
- **1 sealed class** — add route-based mapping to `DashboardBottomTab`
- **Cross-module visibility** — resolve `InvestmentsTab` (internal in `feature-investments`) used from `feature-dashboard`

---

## Change 1: Remove `ordinal` from all `@TabItem` annotations

**Rationale**: The new API drops `ordinal` from the annotation. Tab ordering is now determined by the library internally (declaration order or registration order).

### Files & diffs

#### [DashboardDestination.kt](feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/DashboardDestination.kt)

4 annotations to update:

```diff
-@TabItem(parent = MainNavigation::class, ordinal = 0)
+@TabItem(parent = MainNavigation::class)
 @Destination(route = "main/dashboard")
 @Transition(type = TransitionType.Fade)
 internal data object DashboardDestination : NavDestination

-@TabItem(parent = MainNavigation::class, ordinal = 1)
+@TabItem(parent = MainNavigation::class)
 @Destination(route = "main/transfers")
 @Transition(type = TransitionType.Fade)
 internal data object Transfers : NavDestination

-@TabItem(parent = MainNavigation::class, ordinal = 2)
+@TabItem(parent = MainNavigation::class)
 @Destination(route = "main/finance")
 @Transition(type = TransitionType.Fade)
 internal data object Finance : NavDestination

-@TabItem(parent = MainNavigation::class, ordinal = 4)
+@TabItem(parent = MainNavigation::class)
 @Destination(route = "main/benefits")
 @Transition(type = TransitionType.Fade)
 internal data object Benefits : NavDestination
```

#### [InvestmentsDestination.kt](feature-investments/src/commonMain/kotlin/com/pekao/peopay/feature/investments/internal/presentation/InvestmentsDestination.kt)

1 annotation to update:

```diff
-@TabItem(parent = MainNavigation::class, ordinal = 3)
+@TabItem(parent = MainNavigation::class)
 @Destination(route = "main/investments")
 @Transition(type = TransitionType.Fade)
 internal data object InvestmentsTab : NavDestination
```

#### [QuickActivationTabsDestination.kt](feature-developer-console/src/commonMain/kotlin/com/pekao/peopay/feature/developerConsole/internal/ui/quickActivation/QuickActivationTabsDestination.kt)

2 annotations to update:

```diff
 @Tabs(name = "quick_activation")
 internal sealed class QuickActivationTabsDestination : NavDestination {
     companion object : NavDestination

-    @TabItem(parent = QuickActivationTabsDestination::class, ordinal = 0)
+    @TabItem(parent = QuickActivationTabsDestination::class)
     @Destination(route = "dev_console/quick_activation/form")
     data object Form : QuickActivationTabsDestination()

-    @TabItem(parent = QuickActivationTabsDestination::class, ordinal = 1)
+    @TabItem(parent = QuickActivationTabsDestination::class)
     @Destination(route = "dev_console/quick_activation/last_used")
     data object LastUsed : QuickActivationTabsDestination()
 }
```

- [ ] Remove `ordinal` from 4 `@TabItem` in `DashboardDestination.kt`
- [ ] Remove `ordinal` from 1 `@TabItem` in `InvestmentsDestination.kt`
- [ ] Remove `ordinal` from 2 `@TabItem` in `QuickActivationTabsDestination.kt`

---

## Change 2: Add route-based nav mapping to `DashboardBottomTab`

**File**: [DashboardBottomTab.kt](feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/internal/presentation/dashboard/tabs/DashboardBottomTab.kt)

### Problem: Cross-module visibility

`InvestmentsTab` is `internal` in `feature-investments`. `DashboardScreen` lives in `feature-dashboard` and cannot import it. The modular architecture strictly enforces this boundary (see [modular architecture memory](#)).

### Solution: Route-based matching

Add a `navRoute` property to each `DashboardBottomTab` variant, and use `scope.tabs` (which provides `NavDestination` objects with their `route` property) for bidirectional mapping. This avoids any cross-module type reference.

### Code

```kotlin
internal sealed class DashboardBottomTab(
    val icon: DrawableResource,
    val label: StringResource,
    val contentDescription: StringResource = label,
    val navRoute: String,
) {
    object Home : DashboardBottomTab(
        icon = DSRes.drawable.ic_logo_pekao,
        label = Res.string.dashboard_tab_home,
        navRoute = "main/dashboard",
    )

    object Finance : DashboardBottomTab(
        icon = DSRes.drawable.ic_coins,
        label = Res.string.dashboard_tab_finance,
        navRoute = "main/finance",
    )

    object Transfers : DashboardBottomTab(
        icon = DSRes.drawable.ic_arrows_transfer,
        label = Res.string.dashboard_tab_payments,
        navRoute = "main/transfers",
    )

    object Investments : DashboardBottomTab(
        icon = DSRes.drawable.ic_chart_monitoring,
        label = Res.string.dashboard_tab_investments,
        navRoute = "main/investments",
    )

    object Benefits : DashboardBottomTab(
        icon = DSRes.drawable.ic_star,
        label = Res.string.dashboard_tab_benefits,
        navRoute = "main/benefits",
    )
}
```

Add two extension functions for mapping between `NavDestination` and `DashboardBottomTab`:

```kotlin
/**
 * Maps a NavDestination to its corresponding DashboardBottomTab by route.
 * Falls back to Home if no match found.
 */
internal fun NavDestination.toDashboardTab(): DashboardBottomTab =
    dashboardBottomTabs.firstOrNull { it.navRoute == this.route }
        ?: DashboardBottomTab.Home

/**
 * Finds the NavDestination corresponding to this tab in the available scope tabs.
 */
internal fun DashboardBottomTab.toNavDestination(tabs: Set<NavDestination>): NavDestination =
    tabs.first { it.route == this.navRoute }
```

- [ ] Add `navRoute` constructor parameter to `DashboardBottomTab`
- [ ] Add `navRoute` value to each subclass (`Home`, `Finance`, `Transfers`, `Investments`, `Benefits`)
- [ ] Add `NavDestination.toDashboardTab()` extension function
- [ ] Add `DashboardBottomTab.toNavDestination()` extension function

### Route reference table

| DashboardBottomTab | Route | Source destination |
|---|---|---|
| `Home` | `main/dashboard` | `DashboardDestination` (feature-dashboard) |
| `Finance` | `main/finance` | `Finance` (feature-dashboard) |
| `Transfers` | `main/transfers` | `Transfers` (feature-dashboard) |
| `Investments` | `main/investments` | `InvestmentsTab` (feature-investments) |
| `Benefits` | `main/benefits` | `Benefits` (feature-dashboard) |

---

## Change 3: Rewrite `DashboardScreen` to use destination-based API

**File**: [DashboardScreen.kt](feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/internal/presentation/dashboard/DashboardScreen.kt)

### Current code (lines 70–98)

```kotlin
@TabsContainer(MainNavigation::class)
@Composable
internal fun DashboardScreen(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    // ...
    Scaffold(
        topBar = {
            DashboardTopNavigation(
                selectedElement = dashboardBottomTabs[scope.activeTabIndex],  // ← index-based
                // ...
            )
        },
        bottomBar = {
            DashboardBottomNavigationBar(
                selectedElement = dashboardBottomTabs[scope.activeTabIndex],  // ← index-based
                scope = scope,
            )
        }
    )
}
```

### New code

Replace both `dashboardBottomTabs[scope.activeTabIndex]` with `scope.activeTab.toDashboardTab()`:

```kotlin
@TabsContainer(MainNavigation::class)
@Composable
internal fun DashboardScreen(
    scope: TabsContainerScope,
    content: @Composable () -> Unit
) {
    // ...
    val selectedTab = scope.activeTab.toDashboardTab()

    Scaffold(
        topBar = {
            DashboardTopNavigation(
                selectedElement = selectedTab,
                state = state,
                onIntent = container::intent
            )
        },
        bottomBar = {
            DashboardBottomNavigationBar(
                selectedElement = selectedTab,
                scope = scope,
            )
        }
    ) { /* ... unchanged ... */ }
}
```

New import needed:

```kotlin
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
```

- [ ] Replace `dashboardBottomTabs[scope.activeTabIndex]` with `scope.activeTab.toDashboardTab()` (2 occurrences)
- [ ] Add import for `toDashboardTab` extension

---

## Change 4: Rewrite `DashboardBottomNavigationBar`

**File**: [DashboardScreen.kt](feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/internal/presentation/dashboard/DashboardScreen.kt) (lines 184–201)

### Current code

```kotlin
@Composable
private fun DashboardBottomNavigationBar(
    selectedElement: DashboardBottomTab,
    scope: TabsContainerScope,
) {
    val tabs = dashboardBottomTabs.map { tab -> tab.toBottomNavigationTab() }
    val selectedNavigationIndex = dashboardBottomTabs.indexOf(selectedElement)
    BottomNavigation(
        tabs = tabs,
        selectedTab = tabs[selectedNavigationIndex],
        onTabSelected = { tab ->
            scope.switchTab(tabs.indexOf(tab))           // ← index-based
        },
        onAiButtonClick = { },
        state = BottomNavigationState.START,
        aiButtonText = stringResource(Res.string.dashboard_ai_button_title),
        modifier = Modifier.padding(paddingValues = WindowInsets.navigationBars.asPaddingValues())
    )
}
```

### New code

```kotlin
@Composable
private fun DashboardBottomNavigationBar(
    selectedElement: DashboardBottomTab,
    scope: TabsContainerScope,
) {
    val tabs = dashboardBottomTabs.map { tab -> tab.toBottomNavigationTab() }
    val selectedNavigationIndex = dashboardBottomTabs.indexOf(selectedElement)
    BottomNavigation(
        tabs = tabs,
        selectedTab = tabs[selectedNavigationIndex],
        onTabSelected = { tab ->
            val dashboardTab = dashboardBottomTabs[tabs.indexOf(tab)]
            scope.switchTab(dashboardTab.toNavDestination(scope.tabs))
        },
        onAiButtonClick = { },
        state = BottomNavigationState.START,
        aiButtonText = stringResource(Res.string.dashboard_ai_button_title),
        modifier = Modifier.padding(paddingValues = WindowInsets.navigationBars.asPaddingValues())
    )
}
```

The key change: `scope.switchTab(tabs.indexOf(tab))` → `scope.switchTab(dashboardTab.toNavDestination(scope.tabs))`.

- [ ] Replace `scope.switchTab(tabs.indexOf(tab))` with destination-based call
- [ ] Add import for `toNavDestination` extension

---

## Change 5: Rewrite `QuickActivationTabContainer`

**File**: [QuickActivationTabContainer.kt](feature-developer-console/src/commonMain/kotlin/com/pekao/peopay/feature/developerConsole/internal/ui/quickActivation/QuickActivationTabContainer.kt)

### Current code (lines 60–72)

```kotlin
SecondaryTabRow(
    selectedTabIndex = scope.activeTabIndex
) {
    listOf("Form", "Last Used").forEachIndexed { index, label ->
        Tab(
            selected = scope.activeTabIndex == index,
            onClick = { scope.switchTab(index) },
            text = { Text(label) }
        )
    }
}
```

### New code

```kotlin
SecondaryTabRow(
    selectedTabIndex = scope.tabs.toList().indexOf(scope.activeTab)
) {
    scope.tabs.forEach { destination ->
        val label = when (destination) {
            is QuickActivationTabsDestination.Form -> "Form"
            is QuickActivationTabsDestination.LastUsed -> "Last Used"
            else -> ""
        }
        Tab(
            selected = scope.activeTab == destination,
            onClick = { scope.switchTab(destination) },
            text = { Text(label) }
        )
    }
}
```

**Note**: `SecondaryTabRow` (Material3) requires `selectedTabIndex: Int`. Since `scope.tabs` is now a `Set<NavDestination>`, we convert to list for index computation: `scope.tabs.toList().indexOf(scope.activeTab)`. Alternatively, iterate tabs with `forEachIndexed` and track the selected index manually.

- [ ] Replace `scope.activeTabIndex` with index derived from `scope.activeTab` for `SecondaryTabRow`
- [ ] Replace hardcoded string list iteration with `scope.tabs.forEach` + `when` type matching
- [ ] Replace `scope.activeTabIndex == index` with `scope.activeTab == destination`
- [ ] Replace `scope.switchTab(index)` with `scope.switchTab(destination)`

---

## Cross-Module Visibility Analysis

### The problem

`InvestmentsTab` is declared as `internal data object` in `feature-investments`. Per project architecture rules:
- Feature implementation modules (`feature-*`) mark all internal-package declarations `internal` or `private` (enforced by Konsist)
- Feature modules cannot import other feature implementation modules

### Options considered

| Option | Approach | Pros | Cons |
|--------|----------|------|------|
| **(a)** Make `InvestmentsTab` public | Change visibility to `public` | Simple, type-safe | Violates architecture rules; Konsist tests would fail |
| **(b)** Route-based matching | Match via `NavDestination.route` string | No visibility changes; minimal diff; respects module boundaries | String-based (no compile-time safety on route values) |
| **(c)** Move to `feature-investments-api` | Create public destination type in API module | Type-safe; proper architecture | Larger change; may pull in transitive deps |

### Recommendation: Option (b) — Route-based matching

This is the approach adopted in Change 2 above. Routes are already stable string constants co-located with `@Destination` annotations. The `navRoute` property on `DashboardBottomTab` mirrors these constants, keeping the mapping localized and easy to verify.

If stronger type safety is desired later, option (c) can be pursued as a follow-up.

---

## Ordering Guarantee

With `ordinal` removed, the tab ordering in `scope.tabs` is determined by quo-vadis's internal registration order. The `dashboardBottomTabs` list in `DashboardBottomTab.kt` continues to control the **display order** of bottom nav items independently. The two are decoupled: `dashboardBottomTabs` defines what the user sees; `scope.tabs` (now a `Set`) defines the available navigation destinations. The route-based mapping bridges them.

**Important**: Verify after migration that `scope.tabs` ordering matches expectations. If quo-vadis uses declaration/registration order, the order depends on KSP processing order across modules (which was the original cross-module merge ordering issue from the [quo-vadis-tab-merge-analysis](plans/quo-vadis-tab-merge-analysis.md)). With destination-based switching, **ordering no longer matters for correctness** — only the identity of the destination matters.

---

## Files Modified (Summary)

| File | Module | Changes |
|------|--------|---------|
| [DashboardDestination.kt](feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/DashboardDestination.kt) | feature-dashboard | Remove `ordinal` from 4 `@TabItem` |
| [InvestmentsDestination.kt](feature-investments/src/commonMain/kotlin/com/pekao/peopay/feature/investments/internal/presentation/InvestmentsDestination.kt) | feature-investments | Remove `ordinal` from 1 `@TabItem` |
| [QuickActivationTabsDestination.kt](feature-developer-console/src/commonMain/kotlin/com/pekao/peopay/feature/developerConsole/internal/ui/quickActivation/QuickActivationTabsDestination.kt) | feature-developer-console | Remove `ordinal` from 2 `@TabItem` |
| [DashboardBottomTab.kt](feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/internal/presentation/dashboard/tabs/DashboardBottomTab.kt) | feature-dashboard | Add `navRoute`; add mapping extensions |
| [DashboardScreen.kt](feature-dashboard/src/commonMain/kotlin/com/pekao/peopay/feature/dashboard/internal/presentation/dashboard/DashboardScreen.kt) | feature-dashboard | Use `scope.activeTab` + `toDashboardTab()`; use `scope.switchTab(destination)` |
| [QuickActivationTabContainer.kt](feature-developer-console/src/commonMain/kotlin/com/pekao/peopay/feature/developerConsole/internal/ui/quickActivation/QuickActivationTabContainer.kt) | feature-developer-console | Use `scope.activeTab`, `scope.tabs`, `scope.switchTab(destination)` |

---

## Task Checklist

### Phase 1: Annotation cleanup
- [ ] Remove `ordinal` from `@TabItem` in `DashboardDestination.kt` (4 annotations)
- [ ] Remove `ordinal` from `@TabItem` in `InvestmentsDestination.kt` (1 annotation)
- [ ] Remove `ordinal` from `@TabItem` in `QuickActivationTabsDestination.kt` (2 annotations)

### Phase 2: Dashboard mapping layer
- [ ] Add `navRoute` property to `DashboardBottomTab` sealed class constructor
- [ ] Add `navRoute` value to each `DashboardBottomTab` subclass
- [ ] Add `NavDestination.toDashboardTab()` extension function
- [ ] Add `DashboardBottomTab.toNavDestination(tabs)` extension function

### Phase 3: Dashboard consumer rewrite
- [ ] Replace `dashboardBottomTabs[scope.activeTabIndex]` with `scope.activeTab.toDashboardTab()` in `DashboardScreen`
- [ ] Replace `scope.switchTab(tabs.indexOf(tab))` with destination-based `scope.switchTab(...)` in `DashboardBottomNavigationBar`

### Phase 4: Developer console consumer rewrite
- [ ] Rewrite `QuickActivationTabContainer` to use `scope.activeTab`, `scope.tabs`, and `scope.switchTab(destination)`

### Phase 5: Verification
- [ ] Build compiles successfully (`./gradlew composeApp:compileKotlinAndroid`)
- [ ] Tab selection works correctly on Dashboard (all 5 tabs)
- [ ] Tab selection works correctly on Quick Activation (Form / Last Used)
- [ ] Bottom navigation highlights correct tab after switching
- [ ] Top navigation title updates correctly per tab
- [ ] No Konsist architecture test failures

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Route strings drift between `DashboardBottomTab.navRoute` and `@Destination(route = ...)` | Tab switching silently breaks | Add a unit test that verifies each `navRoute` matches an actual destination route |
| `scope.tabs` ordering differs from `dashboardBottomTabs` ordering | No impact on correctness (destination-based switching is order-independent) | N/A — visual order is controlled by `dashboardBottomTabs`, not `scope.tabs` |
| Material3 `SecondaryTabRow` requires `selectedTabIndex: Int` | Must still compute index | Use `scope.tabs.indexOf(scope.activeTab)` — guaranteed correct within `scope.tabs` |
| quo-vadis library update not yet published | Cannot compile until library update lands | Coordinate: apply peopay-kmp changes in same PR or after quo-vadis release |
