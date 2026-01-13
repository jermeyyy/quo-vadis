# Implementation Plan: Simplify @TabItem Annotation

## Overview

Remove `label` and `icon` properties from `@TabItem` annotation. Instead of generating metadata, provide a list of destination instances (`List<NavDestination>`) to `@TabsContainer` wrappers, allowing users to customize tab UI (labels, icons) at the usage site with type-safe pattern matching.

## Requirements (Validated)

1. Remove `label` and `icon` from `@TabItem` annotation
2. Replace `tabMetadata: List<TabMetadata>` with `tabs: List<NavDestination>` in `TabsContainerScope`
3. Keep `GeneratedTabMetadata` with only `route` field for internal use (deep linking, serialization)
4. Update all demo app usages
5. Update documentation markdown files
6. Prepare plan for website-docs agent

## Technical Approach

### New Pattern

**Before:**
```kotlin
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination { ... }

// In @TabsContainer
scope.tabMetadata.forEachIndexed { index, meta ->
    NavigationBarItem(
        label = { Text(meta.label) },
        icon = { Icon(getIcon(meta.icon), ...) },
        ...
    )
}
```

**After:**
```kotlin
@TabItem  // No label/icon parameters
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination { ... }

// In @TabsContainer - type-safe pattern matching
scope.tabs.forEachIndexed { index, tab ->
    val (label, icon) = when (tab) {
        is HomeTab -> "Home" to Icons.Default.Home
        is ExploreTab -> "Explore" to Icons.Default.Explore
        is ProfileTab -> "Profile" to Icons.Default.Person
        is SettingsTab -> "Settings" to Icons.Default.Settings
        else -> "Tab" to Icons.Default.Circle
    }
    NavigationBarItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = label) },
        ...
    )
}
```

---

## Tasks

### Phase 1: Core Library Changes

#### Task 1.1: Simplify @TabItem Annotation
- **Description:** Remove `label` and `icon` properties from `@TabItem` annotation
- **Files:** 
  - `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`
- **Changes:**
  - Remove `label: String` property
  - Remove `icon: String = ""` property
  - Update KDoc to reflect new usage pattern
- **Acceptance Criteria:**
  - [ ] `@TabItem` annotation has no parameters
  - [ ] KDoc updated with new pattern examples

#### Task 1.2: Simplify GeneratedTabMetadata
- **Description:** Remove `label` and `icon` from `GeneratedTabMetadata`, keep only `route`
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/GeneratedTabMetadata.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/GeneratedTabMetadata.kt` (if duplicate exists)
- **Changes:**
  - Remove `label: String` property
  - Remove `icon: String` property
  - Keep only `route: String`
  - Update KDoc
- **Acceptance Criteria:**
  - [ ] `GeneratedTabMetadata` contains only `route`
  - [ ] Serialization still works

#### Task 1.3: Update TabsContainerScope
- **Description:** Replace `tabMetadata` with `tabs` list of NavDestination instances
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/TabsContainerScope.kt`
- **Changes:**
  - Remove `tabMetadata: List<TabMetadata>` property
  - Add `tabs: List<NavDestination>` property
  - Remove `TabMetadata` data class entirely
  - Update KDoc with new usage pattern
- **Acceptance Criteria:**
  - [ ] `tabs: List<NavDestination>` property exists
  - [ ] `tabMetadata` property removed
  - [ ] `TabMetadata` class removed

#### Task 1.4: Update TabsContainerScope Implementation
- **Description:** Update the implementation class that provides TabsContainerScope
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/` (find implementation)
  - Search for classes implementing `TabsContainerScope`
- **Changes:**
  - Implement new `tabs` property
  - Get destination instances from TabNode's stacks
- **Acceptance Criteria:**
  - [ ] `tabs` property returns correct destination instances in order

---

### Phase 2: KSP Generator Changes

#### Task 2.1: Update TabItemInfo Model
- **Description:** Remove `label` and `icon` fields from `TabItemInfo` data class
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`
- **Changes:**
  - Remove `label: String` property
  - Remove `icon: String` property
- **Acceptance Criteria:**
  - [ ] `TabItemInfo` has no label/icon fields

#### Task 2.2: Update TabExtractor
- **Description:** Remove extraction of label/icon from @TabItem annotations
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Changes:**
  - Remove `label` argument extraction in `extractTabItemNewPattern`
  - Remove `icon` argument extraction in `extractTabItemNewPattern`
  - Remove `label` argument extraction in `extractTabItemLegacy`
  - Remove `icon` argument extraction in `extractTabItemLegacy`
- **Acceptance Criteria:**
  - [ ] Extractor no longer reads label/icon from annotations

#### Task 2.3: Update ContainerBlockGenerator
- **Description:** Remove title/icon parameters from generated tab entries
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`
- **Changes:**
  - Remove `buildTabParams` method (or simplify)
  - Remove `buildTabParamsForContainerTab` method (or simplify)
  - Update `generateTabEntry` to not emit title/icon params
- **Acceptance Criteria:**
  - [ ] Generated code doesn't include title/icon for tabs

#### Task 2.4: Update ValidationEngine (if needed)
- **Description:** Remove validation rules for label/icon
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Changes:**
  - Remove any validation related to @TabItem label/icon
- **Acceptance Criteria:**
  - [ ] No validation errors for @TabItem without label/icon

---

### Phase 3: Demo App Updates

#### Task 3.1: Update MainTabs Destination
- **Description:** Remove label/icon from @TabItem annotations
- **Files:**
  - `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/MainTabs.kt`
- **Changes:**
  - Change `@TabItem(label = "Home", icon = "home")` to `@TabItem`
  - Same for Explore, Profile, Settings tabs
- **Acceptance Criteria:**
  - [ ] All @TabItem annotations have no parameters

#### Task 3.2: Update TabsDestination (DemoTabs)
- **Description:** Remove label/icon from @TabItem annotations
- **Files:**
  - `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/TabsDestination.kt`
- **Changes:**
  - Change `@TabItem(label = "Music", icon = "music_note")` to `@TabItem`
  - Same for Movies, Books tabs
- **Acceptance Criteria:**
  - [ ] All @TabItem annotations have no parameters

#### Task 3.3: Update StateDrivenDemoDestination
- **Description:** Remove label/icon from @TabItem annotation
- **Files:**
  - `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/destinations/StateDrivenDemoDestination.kt`
- **Changes:**
  - Change `@TabItem(label = "Demo", icon = "layers")` to `@TabItem`
- **Acceptance Criteria:**
  - [ ] @TabItem annotation has no parameters

#### Task 3.4: Update MainTabsUI Container
- **Description:** Update container to use `tabs` instead of `tabMetadata` with pattern matching
- **Files:**
  - `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/tabs/MainTabsUI.kt`
- **Changes:**
  - Replace `scope.tabMetadata` with `scope.tabs`
  - Use `when(tab)` pattern to determine label/icon
  - Update `GlassBottomNavigationBar` and `MainBottomNavigationBar` functions
  - Remove fallback icon function or update it
- **Acceptance Criteria:**
  - [ ] Uses `scope.tabs` with pattern matching
  - [ ] Correctly displays Home, Explore, Profile, Settings icons/labels

#### Task 3.5: Update TabsDemoWrapper Container
- **Description:** Update container to use `tabs` instead of `tabMetadata`
- **Files:**
  - `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/tabs/TabsDemoWrapper.kt`
- **Changes:**
  - Replace `scope.tabMetadata` with `scope.tabs`
  - Use pattern matching for Music, Movies, Books tabs
- **Acceptance Criteria:**
  - [ ] Uses pattern matching for tab UI

#### Task 3.6: Update StateDrivenDemoScreen Container
- **Description:** Update container to use `tabs` instead of `tabMetadata`
- **Files:**
  - `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/ui/screens/statedriven/StateDrivenDemoScreen.kt`
- **Changes:**
  - Replace `scope.tabMetadata` with `scope.tabs`
  - Use pattern matching
- **Acceptance Criteria:**
  - [ ] Uses pattern matching for tab UI

---

### Phase 4: Documentation Updates

#### Task 4.1: Update ANNOTATIONS.md
- **Description:** Update @TabItem and @TabsContainer documentation
- **Files:**
  - `docs/ANNOTATIONS.md`
- **Changes:**
  - Remove @TabItem Properties section (label, icon)
  - Update examples to show new pattern
  - Update @TabsContainer examples to show pattern matching
  - Update validation rules section
- **Acceptance Criteria:**
  - [ ] All @TabItem examples use new pattern
  - [ ] TabsContainer examples show pattern matching

#### Task 4.2: Update NAV-NODES.md
- **Description:** Update TabNode documentation
- **Files:**
  - `docs/NAV-NODES.md`
- **Changes:**
  - Update `tabMetadata` property description (route only)
  - Update @TabItem examples
- **Acceptance Criteria:**
  - [ ] TabNode documentation reflects simplified metadata

#### Task 4.3: Update DSL-CONFIG.md
- **Description:** Update DSL configuration documentation
- **Files:**
  - `docs/DSL-CONFIG.md`
- **Changes:**
  - Update TabsContainerScope properties table
  - Update examples to use `tabs` instead of `tabMetadata`
- **Acceptance Criteria:**
  - [ ] All examples use new pattern

#### Task 4.4: Update ARCHITECTURE.md
- **Description:** Update architecture documentation
- **Files:**
  - `docs/ARCHITECTURE.md`
- **Changes:**
  - Update TabNode properties
  - Update any @TabItem examples
- **Acceptance Criteria:**
  - [ ] Architecture docs reflect new design

#### Task 4.5: Update NAVIGATOR.md
- **Description:** Update navigator documentation
- **Files:**
  - `docs/NAVIGATOR.md`
- **Changes:**
  - Update any tab-related examples
- **Acceptance Criteria:**
  - [ ] Navigator docs use new pattern

#### Task 4.6: Update TRANSITIONS.md
- **Description:** Update transitions documentation
- **Files:**
  - `docs/TRANSITIONS.md`
- **Changes:**
  - Update @TabItem examples if present
- **Acceptance Criteria:**
  - [ ] Transition docs use new pattern

---

### Phase 5: Cleanup

#### Task 5.1: Remove Obsolete Code
- **Description:** Clean up any remaining references to old pattern
- **Files:**
  - Search entire codebase for `tabMetadata`, `TabMetadata` (runtime class), old pattern references
- **Changes:**
  - Remove any dead code
  - Update imports
- **Acceptance Criteria:**
  - [ ] No references to removed APIs
  - [ ] Build succeeds

#### Task 5.2: Update KDoc Comments
- **Description:** Update inline documentation throughout codebase
- **Files:**
  - Various files with KDoc mentioning old pattern
- **Changes:**
  - Update all KDoc to reflect new pattern
- **Acceptance Criteria:**
  - [ ] KDoc is accurate

#### Task 5.3: Update Detekt Baselines
- **Description:** Update detekt baselines if method signatures changed
- **Files:**
  - `composeApp/detekt-baseline.xml`
  - `quo-vadis-ksp/detekt-baseline.xml`
- **Changes:**
  - Regenerate baselines if needed
- **Acceptance Criteria:**
  - [ ] Detekt passes

---

## Phase 6: Website Documentation Plan

### For website-docs Agent

The following changes need to be made to the documentation website after the core implementation is complete:

#### Source Documentation Files Changed
1. `docs/ANNOTATIONS.md` - @TabItem and @TabsContainer sections
2. `docs/NAV-NODES.md` - TabNode section
3. `docs/DSL-CONFIG.md` - TabsContainerScope section
4. `docs/ARCHITECTURE.md` - TabNode properties
5. `docs/NAVIGATOR.md` - Tab-related examples
6. `docs/TRANSITIONS.md` - @TabItem examples

#### Website Pages to Update
Based on the docs/site structure, update any pages that render content from the above markdown files:
- Check `docs/site/src/` for components rendering tab-related documentation
- Update code examples in website to show new pattern
- Update API reference sections if they exist

#### Key Changes to Communicate
1. **Breaking Change**: `@TabItem` no longer accepts `label` and `icon` parameters
2. **New Pattern**: Tab customization (icons, labels) now done in `@TabsContainer` composable
3. **TabsContainerScope Changes**: `tabMetadata: List<TabMetadata>` replaced with `tabs: List<NavDestination>`
4. **Pattern Matching**: Use `when(tab)` for type-safe tab customization

#### Example Code for Website
```kotlin
// Old pattern (DEPRECATED)
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination { ... }

// New pattern
@TabItem
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination { ... }

// In TabsContainer
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsContainer(scope: TabsContainerScope, content: @Composable () -> Unit) {
    NavigationBar {
        scope.tabs.forEachIndexed { index, tab ->
            val (label, icon) = when (tab) {
                is HomeTab -> "Home" to Icons.Default.Home
                is ExploreTab -> "Explore" to Icons.Default.Explore
                else -> "Tab" to Icons.Default.Circle
            }
            NavigationBarItem(
                selected = index == scope.activeTabIndex,
                onClick = { scope.switchTab(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
```

---

## Sequencing

```
Phase 1 (Core Library) → Phase 2 (KSP) → Phase 3 (Demo App) → Phase 4 (Docs) → Phase 5 (Cleanup)
                                                                                      ↓
                                                                              Phase 6 (Website - Manual)
```

**Dependencies:**
- Phase 2 depends on Phase 1 (annotations must be updated before KSP)
- Phase 3 depends on Phase 1 & 2 (core changes must be complete)
- Phase 4 can run in parallel with Phase 5
- Phase 6 is manual, run after all other phases

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| Breaking existing code | Clear migration guide in documentation |
| Serialization compatibility | Keep `GeneratedTabMetadata` with route field |
| KSP compilation issues | Incremental testing after each KSP change |
| Demo app compilation failures | Update demo app immediately after core changes |

---

## Open Questions

None - all questions resolved during planning.

---

## Checklist Before Starting

- [ ] Ensure clean git state
- [ ] Run full build to establish baseline
- [ ] Create feature branch: `feat/simplify-tabitem`
