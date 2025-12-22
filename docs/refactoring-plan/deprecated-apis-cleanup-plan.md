# Deprecated APIs Cleanup Plan

## Overview

This document catalogs all deprecated APIs in the Quo Vadis navigation library and provides a phased refactoring plan for their cleanup. The goal is to remove these APIs in a future major version while ensuring users have adequate migration paths.

**Date**: December 2024  
**Target Version**: Next Major Release (after deprecation period)

---

## Summary of Deprecated APIs

| Category | API | Deprecation Level | Location | Replacement |
|----------|-----|-------------------|----------|-------------|
| Pane Navigation | `Navigator.navigateToPane()` | WARNING | Navigator.kt | `navigate(destination)` |
| Pane Navigation | `Navigator.switchPane()` | WARNING | Navigator.kt | `navigate(destination)` |
| Pane Navigation | `TreeNavigator.navigateToPane()` | WARNING | TreeNavigator.kt | `navigate(destination)` |
| Pane Navigation | `TreeNavigator.switchPane()` | WARNING | TreeNavigator.kt | `navigate(destination)` |
| Pane Navigation | `FakeNavigator.navigateToPane()` | WARNING | FakeNavigator.kt | `navigate(destination)` |
| Pane Navigation | `FakeNavigator.switchPane()` | WARNING | FakeNavigator.kt | `navigate(destination)` |
| Type Aliases | `PaneWrapperScope` | - | PaneContainerScope.kt | `PaneContainerScope` |
| Type Aliases | `TabWrapperScope` | - | TabsContainerScope.kt | `TabsContainerScope` |
| Internal | `createTabWrapperScope()` | - | TabsContainerScope.kt | `createTabsContainerScope()` |
| NavigationHost | `NavigationHost(navigator, config, ...)` | WARNING | NavigationHost.kt | `NavigationHost(navigator, ...)` |
| Annotation | `@TabItem.rootGraph` | - | TabAnnotations.kt | `@TabItem + @Stack` pattern |

---

## Detailed Analysis

### 1. Deprecated Pane Navigation APIs

#### 1.1 `navigateToPane()` and `switchPane()`

**Files Affected:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeMutator.kt` (non-deprecated but internal)

**Current Signature:**
```kotlin
@Deprecated(
    message = "navigateToPane() is deprecated. Use navigate() with a destination instead. " +
        "Navigate will automatically target the correct pane based on destination.",
    replaceWith = ReplaceWith("navigate(destination)"),
    level = DeprecationLevel.WARNING
)
fun navigateToPane(
    role: PaneRole,
    destination: NavDestination,
    switchFocus: Boolean = true,
    transition: NavigationTransition? = null
)

@Deprecated(
    message = "switchPane() is deprecated. Use navigate() with a destination instead. " +
        "Navigate will automatically switch to the pane containing the destination.",
    replaceWith = ReplaceWith("navigate(destination)"),
    level = DeprecationLevel.WARNING
)
fun switchPane(role: PaneRole)
```

**Reason for Deprecation:**
- These APIs expose implementation details (pane roles) to the user
- The `navigate(destination)` approach is more declarative and type-safe
- Navigation should be destination-centric, not container-centric

**Internal Usages Found:**
1. **NavTreeRenderer.kt (lines 582, 595)** - Uses `switchPane()` internally:
   ```kotlin
   onNavigateToPane = { role -> scope.navigator.switchPane(role) }
   ```
   
2. **Test files** - `TreeNavigatorTest.kt` and `TreeMutatorPaneTest.kt` contain extensive tests for these APIs

**Migration Path:**
```kotlin
// Before
navigator.navigateToPane(PaneRole.Supporting, DetailDestination(itemId))
navigator.switchPane(PaneRole.Primary)

// After
navigator.navigate(DetailDestination(itemId))  // Automatically targets correct pane
// For switchPane, navigate to a destination in the target pane
```

**Refactoring Tasks:**
- [ ] Update internal `NavTreeRenderer.kt` to use alternative approach
- [ ] Update tests to use new patterns OR keep tests to verify backward compatibility during deprecation period
- [ ] Raise deprecation level to ERROR in next minor version
- [ ] Remove APIs in next major version

---

### 2. Deprecated Type Aliases

#### 2.1 `PaneWrapperScope` and `TabWrapperScope`

**Files Affected:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/PaneContainerScope.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/TabsContainerScope.kt`

**Current Definition:**
```kotlin
@Deprecated(
    message = "Use PaneContainerScope instead",
    replaceWith = ReplaceWith("PaneContainerScope", "...")
)
typealias PaneWrapperScope = PaneContainerScope

@Deprecated(
    message = "Use TabsContainerScope instead",
    replaceWith = ReplaceWith("TabsContainerScope", "...")
)
typealias TabWrapperScope = TabsContainerScope
```

**Reason for Deprecation:**
- Naming consistency: "Container" is more descriptive than "Wrapper"
- Aligns with `ContainerRegistry` naming convention

**Usages Found:**
- No external usages detected in the codebase

**Migration Path:**
```kotlin
// Before
@Composable
fun MyTabBar(scope: TabWrapperScope, content: @Composable () -> Unit) { ... }

// After
@Composable
fun MyTabBar(scope: TabsContainerScope, content: @Composable () -> Unit) { ... }
```

**Refactoring Tasks:**
- [ ] Search for any usages in demo app or documentation
- [ ] Remove typealiases in next major version (simple removal, no internal dependencies)

---

### 3. Deprecated `createTabWrapperScope()`

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/TabsContainerScope.kt`

**Current Definition:**
```kotlin
@Deprecated(
    message = "Use createTabsContainerScope instead",
    replaceWith = ReplaceWith("createTabsContainerScope(...)")
)
internal fun createTabWrapperScope(...): TabsContainerScope = 
    createTabsContainerScope(...)
```

**Status:** Internal function, no external impact.

**Refactoring Tasks:**
- [ ] Remove in next major version (internal only, simple removal)

---

### 4. Deprecated `NavigationHost` with Config Parameter

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavigationHost.kt`

**Current Signature (lines 482-505):**
```kotlin
@Deprecated(
    message = "Config should be passed only to rememberQuoVadisNavigator. " +
        "Use NavigationHost(navigator) instead - config is read from navigator.",
    replaceWith = ReplaceWith("NavigationHost(navigator, modifier, enablePredictiveBack, windowSizeClass)"),
    level = DeprecationLevel.WARNING
)
@Composable
fun NavigationHost(
    navigator: Navigator,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    windowSizeClass: WindowSizeClass? = null
)
```

**Reason for Deprecation:**
- Redundancy: Config is already passed to `rememberQuoVadisNavigator()`
- Risk of mismatch: Passing different configs to navigator and host causes bugs
- API simplification: Navigator now carries its config

**Usages Found:**
- Documentation examples in multiple files still show the old pattern
- `DemoApp.kt` uses the new pattern (good)

**Migration Path:**
```kotlin
// Before
val navigator = rememberQuoVadisNavigator(MainTabs::class, config)
NavigationHost(navigator, config)  // Config passed twice

// After
val navigator = rememberQuoVadisNavigator(MainTabs::class, config)
NavigationHost(navigator)  // Config read from navigator
```

**Refactoring Tasks:**
- [ ] Update all documentation examples to use new pattern
- [ ] Update `StringTemplates.kt` KDoc examples
- [ ] Update `NavigationConfig.kt` KDoc examples
- [ ] Update `QuoVadisComposables.kt` KDoc examples
- [ ] Update `DslNavigationConfig.kt` KDoc examples
- [ ] Update `NavigationConfigBuilder.kt` KDoc examples
- [ ] Raise deprecation level to ERROR in next minor version
- [ ] Remove overload in next major version

---

### 5. Deprecated `@TabItem.rootGraph` Parameter

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`

**Current Definition:**
```kotlin
annotation class TabItem(
    val label: String,
    val icon: String = "",
    @Deprecated("Use @TabItem + @Stack on the same class instead. The class itself becomes the root graph.")
    val rootGraph: KClass<*> = Unit::class
)
```

**Reason for Deprecation:**
- KSP limitations in KMP metadata compilation with nested class references
- New pattern is cleaner: `@TabItem + @Stack` on the same class

**Old Pattern (Deprecated):**
```kotlin
@Tab(name = "mainTabs")
sealed class MainTabs : Destination {
    @TabItem(label = "Home", icon = "home", rootGraph = HomeDestination::class)
    @Destination(route = "tabs/home")
    data object Home : MainTabs()
}
```

**New Pattern (Recommended):**
```kotlin
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestinationLegacy = "Feed")
sealed class HomeTab : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeTab()
}

@Tab(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class]
)
object MainTabs
```

**Usages Found:**
- KDoc documentation examples in `TabAnnotations.kt`
- KDoc examples in `TabExtractor.kt`
- KDoc examples in `TabInfo.kt`
- Validation logic in `ValidationEngine.kt`

**Refactoring Tasks:**
- [ ] Update all KDoc examples to use new pattern
- [ ] Keep parameter for backward compatibility during deprecation period
- [ ] Add validation warning when `rootGraph` is used
- [ ] Remove parameter in next major version

---

## Refactoring Phases

### Phase 1: Documentation Updates (Current Release)

**Priority: High**  
**Effort: Low**

1. Update all KDoc examples to use non-deprecated patterns
2. Update website documentation
3. Add migration guides to changelog

**Files to Update:**
- [ ] `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/NavigationConfig.kt`
- [ ] `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisComposables.kt`
- [ ] `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/DslNavigationConfig.kt`
- [ ] `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/dsl/NavigationConfigBuilder.kt`
- [ ] `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/base/StringTemplates.kt`
- [ ] `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`
- [ ] `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- [ ] `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`

---

### Phase 2: Internal Cleanup (Current Release)

**Priority: Medium**  
**Effort: Medium**

1. Fix internal usage of deprecated `switchPane()` in `NavTreeRenderer.kt`
2. Review and update tests that use deprecated APIs

**Current Internal Usage Issue:**
```kotlin
// NavTreeRenderer.kt line 582 and 595
onNavigateToPane = { role -> scope.navigator.switchPane(role) }
```

**Proposed Fix:**
The `PaneContainerScope.navigateToPane(role)` method is NOT deprecated and is the correct API for pane switching from within container wrappers. The issue is that this calls through to `Navigator.switchPane()` which IS deprecated.

**Options:**
1. **Keep as-is during deprecation period** - Internal usage with `@Suppress("DEPRECATION")` comment
2. **Introduce internal non-deprecated API** - Add `internal` overload without deprecation
3. **Refactor pane switching logic** - Move to TreeMutator direct usage

**Recommended:** Option 1 for now, then refactor in Phase 3.

---

### Phase 3: Escalate Deprecation Level (Next Minor Version)

**Priority: Medium**  
**Effort: Low**

1. Change deprecation level from `WARNING` to `ERROR` for:
   - `Navigator.navigateToPane()`
   - `Navigator.switchPane()`
   - `NavigationHost(navigator, config, ...)`

2. This forces migration but doesn't break compilation (IDE shows errors as warnings by default)

---

### Phase 4: API Removal (Next Major Version)

**Priority: Low**  
**Effort: Medium**

Remove all deprecated APIs:

1. **Remove from Navigator interface:**
   - `navigateToPane()`
   - `switchPane()`

2. **Remove from TreeNavigator:**
   - `navigateToPane()` implementation
   - `switchPane()` implementation

3. **Remove from FakeNavigator:**
   - `navigateToPane()` implementation
   - `switchPane()` implementation

4. **Remove type aliases:**
   - `PaneWrapperScope`
   - `TabWrapperScope`

5. **Remove internal functions:**
   - `createTabWrapperScope()`

6. **Remove NavigationHost overload:**
   - Overload with `config` parameter

7. **Remove @TabItem parameter:**
   - `rootGraph` parameter (or mark as hidden)

8. **Update tests:**
   - Remove or update tests that rely on deprecated APIs
   - Tests in `TreeNavigatorTest.kt` for `navigateToPane()`/`switchPane()`
   - Tests in `TreeMutatorPaneTest.kt` for `TreeMutator.navigateToPane()`

---

## Breaking Changes Checklist

When removing deprecated APIs, the following are breaking changes:

| Change | Impact | Mitigation |
|--------|--------|------------|
| Remove `navigateToPane()` | Users calling this method | Replace with `navigate()` |
| Remove `switchPane()` | Users calling this method | Navigate to destination in target pane |
| Remove config-based `NavigationHost()` | Users passing config to host | Use navigator-based overload |
| Remove `PaneWrapperScope` | Custom pane wrappers using old type | Rename to `PaneContainerScope` |
| Remove `TabWrapperScope` | Custom tab bars using old type | Rename to `TabsContainerScope` |
| Remove `@TabItem.rootGraph` | Legacy tab definitions | Migrate to `@TabItem + @Stack` pattern |

---

## Timeline Recommendation

| Phase | Version | Timeline |
|-------|---------|----------|
| Phase 1: Documentation | Current | Immediate |
| Phase 2: Internal Cleanup | Current | Immediate |
| Phase 3: Escalate to ERROR | v1.x.0 | Next minor release |
| Phase 4: Remove APIs | v2.0.0 | Next major release |

---

## Related Documents

- [Screen Registry Simplification Plan](screen-registry-simplification-plan.md) - Related refactoring for ScreenRegistry
- [ANNOTATION_API.md](../../quo-vadis-core/docs/ANNOTATION_API.md) - Current annotation documentation
- [CHANGELOG.md](../../CHANGELOG.md) - Version history and migration notes

---

## Implementation Tasks Summary

### Immediate (Phase 1 & 2)

- [ ] **DOC-1**: Update `NavigationConfig.kt` KDoc to remove deprecated patterns
- [ ] **DOC-2**: Update `QuoVadisComposables.kt` KDoc examples
- [ ] **DOC-3**: Update `DslNavigationConfig.kt` KDoc examples  
- [ ] **DOC-4**: Update `NavigationConfigBuilder.kt` KDoc examples
- [ ] **DOC-5**: Update `StringTemplates.kt` generated KDoc templates
- [ ] **DOC-6**: Update `TabAnnotations.kt` KDoc examples for new pattern
- [ ] **DOC-7**: Update `TabExtractor.kt` KDoc examples
- [ ] **DOC-8**: Update `TabInfo.kt` KDoc examples
- [ ] **INT-1**: Add `@Suppress("DEPRECATION")` to internal `switchPane()` usage in `NavTreeRenderer.kt`
- [ ] **INT-2**: Review test files for deprecated API usage and add suppression or update

### Next Minor Version (Phase 3)

- [ ] **DEP-1**: Change `navigateToPane()` deprecation to ERROR level
- [ ] **DEP-2**: Change `switchPane()` deprecation to ERROR level
- [ ] **DEP-3**: Change `NavigationHost(config)` deprecation to ERROR level

### Next Major Version (Phase 4)

- [ ] **REM-1**: Remove `navigateToPane()` from Navigator interface
- [ ] **REM-2**: Remove `switchPane()` from Navigator interface
- [ ] **REM-3**: Remove implementations from TreeNavigator
- [ ] **REM-4**: Remove implementations from FakeNavigator
- [ ] **REM-5**: Remove `TreeMutator.navigateToPane()` (or make private)
- [ ] **REM-6**: Remove `PaneWrapperScope` typealias
- [ ] **REM-7**: Remove `TabWrapperScope` typealias
- [ ] **REM-8**: Remove `createTabWrapperScope()` function
- [ ] **REM-9**: Remove `NavigationHost(config)` overload
- [ ] **REM-10**: Remove `@TabItem.rootGraph` parameter
- [ ] **REM-11**: Update tests in `TreeNavigatorTest.kt`
- [ ] **REM-12**: Update tests in `TreeMutatorPaneTest.kt`
- [ ] **REM-13**: Update CHANGELOG with breaking changes
