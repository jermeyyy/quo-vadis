# Quo Vadis Navigation Code Analysis Report

**Date**: December 22, 2024  
**Branch**: `deeplinking-refactor`  
**Analysis Scope**: quo-vadis-core, quo-vadis-annotations, quo-vadis-ksp modules

---

## Executive Summary

This report analyzes the current state of the Quo Vadis navigation library code regarding:
1. **Deprecated APIs** - APIs marked or documented for deprecation
2. **Unused APIs** - Public/internal APIs with no references in the codebase
3. **Legacy Code** - Patterns from before architecture refactor that need cleanup

### Key Findings

| Category | Count | Priority |
|----------|-------|----------|
| Deprecated APIs (documented but not annotated) | 11 | Medium |
| Deprecated APIs (already removed) | 8+ | ✅ Completed |
| Unused APIs | 5 | High |
| Legacy Patterns | 7 | Medium |
| Legacy Naming Inconsistencies | 4 | Low |

---

## 1. Deprecated APIs Analysis

### 1.1 Status: Already Removed (Phase 4 Complete)

The following deprecated APIs documented in `deprecated-apis-cleanup-plan.md` have been **already removed** from the codebase:

| API | Former Location | Status |
|-----|-----------------|--------|
| `Navigator.navigateToPane()` | Navigator.kt | ❌ **Removed** |
| `Navigator.switchPane()` | Navigator.kt | ❌ **Removed** |
| `TreeNavigator.navigateToPane()` | TreeNavigator.kt | ❌ **Removed** |
| `TreeNavigator.switchPane()` | TreeNavigator.kt | ❌ **Removed** |
| `FakeNavigator.navigateToPane()` | FakeNavigator.kt | ❌ **Removed** |
| `FakeNavigator.switchPane()` | FakeNavigator.kt | ❌ **Removed** |
| `PaneWrapperScope` typealias | PaneContainerScope.kt | ❌ **Removed** |
| `TabWrapperScope` typealias | TabsContainerScope.kt | ❌ **Removed** |
| `createTabWrapperScope()` | TabsContainerScope.kt | ❌ **Removed** |
| `NavigationHost(navigator, config, ...)` | NavigationHost.kt | ❌ **Removed** |
| `@TabItem.rootGraph` parameter | TabAnnotations.kt | ❌ **Removed** |

**✅ Action Required**: Update `deprecated-apis-cleanup-plan.md` to reflect that Phase 4 is complete.

### 1.2 KSP-Generated Deprecations (Active)

The KSP processor generates `@Deprecated` annotations for legacy handlers:

**File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/DeepLinkHandlerGenerator.kt`

```kotlin
// Lines 180-193
private fun buildDeprecationAnnotation(): AnnotationSpec {
    return AnnotationSpec.builder(Deprecated::class)
        .addMember("message = %S", StringTemplates.deprecatedRegistryMessage(handlerName))
        .addMember("replaceWith = %T(%S, %S)", ReplaceWith::class, 
            "NavigationConfig.deepLinkRegistry", "$GENERATED_PACKAGE.NavigationConfig")
        .addMember("level = %T.%L", DeprecationLevel::class, "WARNING")
        .build()
}
```

This generates deprecation annotations on:
- `GeneratedDeepLinkHandler` - Points users to `NavigationConfig.deepLinkRegistry`
- `Generated*ScreenRegistry` - Points users to `NavigationConfig.screenRegistry`

**Status**: ✅ Working as intended - these are compile-time deprecation warnings for migration.

### 1.3 Internal Deprecation Suppression

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/render/NavTreeRenderer.kt`

Line ~372 contains:
```kotlin
@Suppress("DEPRECATION") // Internal usage - deprecation is for public API users
```

**Status**: ⚠️ **Orphaned suppression** - The deprecated API (`switchPane()`) has been removed. This suppression annotation is now unnecessary and should be cleaned up.

---

## 2. Unused APIs Analysis

### 2.1 HIGH Priority - Completely Unused

| File | API | References | Recommendation |
|------|-----|------------|----------------|
| `NavigationExtensions.kt` | `navigateTo()` | **0** | **Remove** |
| `RuntimeDeepLinkRegistry.kt` | `RuntimeDeepLinkRegistry` class | **0** | **Evaluate/Document** |
| `RuntimeDeepLinkRegistry.kt` | `createUri()` | **0** (only in interface) | Part of unused class |
| `RuntimeDeepLinkRegistry.kt` | `canHandle()` | **0** (only in interface) | Part of unused class |
| `RuntimeDeepLinkRegistry.kt` | `getRegisteredPatterns()` | **0** (only in interface) | Part of unused class |

#### 2.1.1 `navigateTo()` Extension Function

**Location**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/utils/NavigationExtensions.kt`

```kotlin
fun Navigator.navigateTo(
    transition: NavigationTransition? = null,
    builder: () -> NavDestination
) {
    navigate(builder(), transition)
}
```

**Issue**: This extension has **zero references** in the entire codebase. It provides marginal value over direct `navigate()` call.

**Recommendation**: **Remove** - The standard `navigate()` method is sufficient.

#### 2.1.2 `RuntimeDeepLinkRegistry` Class

**Location**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/RuntimeDeepLinkRegistry.kt`

**Issue**: This entire class has **zero references**. It provides runtime deep link registration, but:
- KSP generates `GeneratedDeepLinkRegistry` at compile time
- `CompositeDeepLinkRegistry` handles combining multiple registries
- No documentation shows when to use this class

**Recommendation**: Either:
1. **Remove** if not intended for user use
2. **Document use case** (e.g., for testing or dynamic route registration) and add examples
3. **Mark as experimental** if functionality is planned but incomplete

### 2.2 MEDIUM Priority - Limited Usage

| File | API | Issue |
|------|-----|-------|
| `SharedElementModifiers.kt` | `quoVadisSharedElement()` | Internal helper, zero direct references |
| `SharedElementModifiers.kt` | `quoVadisSharedBounds()` | Internal helper, zero direct references |
| `SharedElementModifiers.kt` | `quoVadisSharedElementOrNoop()` | Internal helper, zero direct references |
| `TransitionScope.kt` | `currentTransitionScope()` | Extension point, no usage |
| `TransitionScope.kt` | `requireTransitionScope()` | Extension point, no usage |

**Recommendation**: These are likely **intentional extension points** for users implementing custom transitions. Keep but verify with documentation.

---

## 3. Legacy Code Analysis

### 3.1 Legacy Annotation Parameters (Backward Compatibility)

#### 3.1.1 String-Based Start Destination

**Location**: `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt`

| Parameter | Status | Replacement |
|-----------|--------|-------------|
| `startDestinationLegacy: String` | Legacy | `startDestination: KClass<*>` |

The string-based parameter is still supported but type-safe `KClass<*>` is preferred.

**Files with KDoc examples still using legacy pattern**:
- `Stack.kt` - Examples show `startDestinationLegacy` usage
- `PaneAnnotations.kt` (line 347)
- `Transition.kt` (lines 160, 187)

#### 3.1.2 String-Based Initial Tab

**Location**: `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`

| Parameter | Status | Replacement |
|-----------|--------|-------------|
| `initialTabLegacy: String` | Legacy | `initialTab: KClass<*>` |

### 3.2 Legacy Tab Pattern (Nested Sealed Subclasses)

**Documentation Location**: `TabAnnotations.kt` lines 88-92, 191-195

**Old Pattern (Deprecated)**:
```kotlin
// DEPRECATED - Do not use this pattern
@Tab(name = "mainTabs")
sealed class MainTabs : Destination {
    @TabItem(label = "Home", icon = "home")
    @Destination(route = "tabs/home")
    data object Home : MainTabs()
}
```

**New Pattern (Recommended)**:
```kotlin
@TabItem(label = "Home", icon = "home")
@Stack(name = "homeStack", startDestination = Feed::class)
sealed class HomeTab : Destination {
    @Destination(route = "home/feed")
    data object Feed : HomeTab()
}

@Tab(name = "mainTabs", initialTab = HomeTab::class, items = [HomeTab::class, ...])
object MainTabs
```

**Warning Emitted**: The KSP processor emits warnings when legacy pattern is detected:
```kotlin
// TabExtractor.kt line 89
logger.warn("@Tab '$name' uses legacy pattern with sealed subclasses - may fail in KMP")
```

**Why Critical**: KSP has limitations resolving sealed subclasses in Kotlin Multiplatform metadata compilation - the legacy pattern may fail silently on certain platforms.

### 3.3 Naming Inconsistency: "Wrapper" vs "Container"

The codebase transitioned from "Wrapper" to "Container" terminology, but inconsistency remains:

| Location | Current Name | Expected Name |
|----------|--------------|---------------|
| `quo-vadis-ksp/.../models/WrapperInfo.kt` | `WrapperInfo` | `ContainerWrapperInfo`? |
| `quo-vadis-ksp/.../extractors/WrapperExtractor.kt` | `WrapperExtractor` | Align with Container? |
| `quo-vadis-ksp/.../generators/dsl/WrapperBlockGenerator.kt` | Multiple `Wrapper` refs | Container naming |

**Runtime Uses "Container"**:
- `TabsContainerScope`
- `PaneContainerScope`
- `ContainerRegistry`

**KSP Uses "Wrapper"**:
- `WrapperInfo`
- `WrapperExtractor`
- `WrapperBlockGenerator`

**Recommendation**: Consider renaming KSP classes to align with runtime terminology for consistency.

### 3.4 Deep Link Resolution Fallback

**Location**: `DeepLinkHandlerGenerator.kt` line 349

```kotlin
// Fallback to route params only (legacy behavior)
```

This fallback exists for backward compatibility with older deep link definitions.

---

## 4. Documentation Using Deprecated Patterns

The following KDoc examples still demonstrate legacy patterns and should be updated:

| File | Line(s) | Issue |
|------|---------|-------|
| `Stack.kt` | KDoc examples | Uses `startDestinationLegacy` |
| `TabAnnotations.kt` | Lines 88-109 | Shows deprecated nested pattern |
| `TabExtractor.kt` | KDoc | References legacy pattern |
| `TabInfo.kt` | Lines 65-76 | Documents legacy pattern |
| `PaneAnnotations.kt` | Line 347 | Uses `startDestinationLegacy` |
| `Transition.kt` | Lines 160, 187 | Uses `startDestinationLegacy` |
| `StringTemplates.kt` | KDoc examples | May reference deprecated patterns |

---

## 5. Cleanup Recommendations

### 5.1 Immediate Actions (High Priority)

| Task ID | Description | Effort |
|---------|-------------|--------|
| **CLEAN-1** | Remove `navigateTo()` extension from `NavigationExtensions.kt` | Low |
| **CLEAN-2** | Evaluate `RuntimeDeepLinkRegistry` - remove or document | Medium |
| **CLEAN-3** | Remove orphaned `@Suppress("DEPRECATION")` from `NavTreeRenderer.kt` | Low |
| **CLEAN-4** | Update `deprecated-apis-cleanup-plan.md` to mark Phase 4 complete | Low |

### 5.2 Documentation Updates (Medium Priority)

| Task ID | Description | Effort |
|---------|-------------|--------|
| **DOC-1** | Update `Stack.kt` KDoc to use type-safe `startDestination` | Low |
| **DOC-2** | Update `TabAnnotations.kt` to remove deprecated pattern examples | Low |
| **DOC-3** | Update `PaneAnnotations.kt` examples | Low |
| **DOC-4** | Update `Transition.kt` examples | Low |
| **DOC-5** | Update `TabExtractor.kt` and `TabInfo.kt` KDoc | Low |

### 5.3 Naming Consistency (Low Priority)

| Task ID | Description | Effort |
|---------|-------------|--------|
| **NAME-1** | Consider renaming `WrapperInfo` → `ContainerWrapperInfo` | Medium |
| **NAME-2** | Consider renaming `WrapperExtractor` for consistency | Medium |
| **NAME-3** | Consider renaming `WrapperBlockGenerator` | Medium |

### 5.4 Future Major Version (Breaking Changes)

| Task ID | Description | Effort |
|---------|-------------|--------|
| **MAJOR-1** | Remove `startDestinationLegacy` parameter from `@Stack` | Low |
| **MAJOR-2** | Remove `initialTabLegacy` parameter from `@Tab` | Low |
| **MAJOR-3** | Remove legacy nested tab pattern support from KSP | Medium |
| **MAJOR-4** | Remove deep link fallback behavior | Low |

---

## 6. Code Quality Metrics

### 6.1 Current State

| Metric | Value | Notes |
|--------|-------|-------|
| Explicit `@Deprecated` annotations in source | 0 | All in generated code |
| KSP-generated deprecations | 2 patterns | DeepLinkHandler, ScreenRegistry |
| Completely unused public APIs | 2 | `navigateTo`, `RuntimeDeepLinkRegistry` |
| Legacy parameters still supported | 2 | `startDestinationLegacy`, `initialTabLegacy` |
| Naming inconsistencies | ~4 files | Wrapper vs Container |

### 6.2 Technical Debt Score

| Area | Score (1-5) | Notes |
|------|-------------|-------|
| Deprecated API cleanup | 4/5 | Phase 4 mostly complete |
| Unused code removal | 3/5 | Some unused APIs remain |
| Documentation freshness | 2/5 | Many examples use legacy patterns |
| Naming consistency | 3/5 | Runtime/KSP terminology mismatch |
| **Overall** | **3/5** | Good progress, some cleanup needed |

---

## 7. Appendices

### A. Files Analyzed

**quo-vadis-core** (125 Kotlin files):
- Core interfaces: Navigator, NavNode, Destination
- Compose integration: NavigationHost, NavTreeRenderer
- Registries: ScreenRegistry, ContainerRegistry, etc.
- Testing: FakeNavigator, FakeNavRenderScope

**quo-vadis-annotations** (9 Kotlin files):
- All annotation definitions
- No deprecated annotations found

**quo-vadis-ksp** (25+ Kotlin files):
- Symbol processor and generators
- Contains deprecation generation logic

### B. Search Patterns Used

- `@Deprecated` - Explicit deprecation annotations
- `TODO|FIXME|HACK|XXX|legacy|deprecated|remove` - Legacy markers
- Symbol reference analysis for unused APIs
- Memory file review for architecture context

### C. Related Documents

- [deprecated-apis-cleanup-plan.md](deprecated-apis-cleanup-plan.md) - Original cleanup plan
- [CHANGELOG.md](../../CHANGELOG.md) - Version history
- [ARCHITECTURE.md](../../ARCHITECTURE.md) - Current architecture

---

## 8. Conclusion

The Quo Vadis navigation library is in good shape regarding deprecated API cleanup. The major deprecation phase (Phase 4) appears to be complete, with deprecated APIs already removed from the codebase.

**Key Actions**:
1. **Immediate**: Remove 2 completely unused APIs (`navigateTo`, evaluate `RuntimeDeepLinkRegistry`)
2. **Short-term**: Update documentation to use current patterns instead of legacy examples
3. **Future**: Consider naming consistency improvements in KSP module

The library maintains backward compatibility through legacy annotation parameters while encouraging migration to type-safe alternatives via KDoc and generated deprecation warnings.
