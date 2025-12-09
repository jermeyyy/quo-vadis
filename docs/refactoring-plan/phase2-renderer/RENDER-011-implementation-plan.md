# RENDER-011: Implementation Plan

**Related**: [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md)  
**Status**: In Progress (Phase 1 & 2 Complete)  
**Created**: 2025-12-09
**Last Updated**: 2025-12-09

## Overview

This document breaks down the hierarchical rendering engine architecture into actionable implementation tasks organized in phases.

---

## Task Summary

| Phase | Tasks | Estimated Effort | Parallel Streams | Status |
|-------|-------|------------------|------------------|--------|
| 1. Core Components | HIER-001 to HIER-008 | 2 weeks | 3 | ✅ Complete |
| 2. KSP Updates | HIER-009 to HIER-015 | 1.5 weeks | 2 | ✅ Complete |
| 3. Renderer Implementation | HIER-016 to HIER-023 | 2.5 weeks | 2 | ⚪ Not Started |
| 4. Integration | HIER-024 to HIER-028 | 1 week | 1 | ⚪ Not Started |
| 5. Migration | HIER-029 to HIER-033 | 0.5 weeks | 1 | ⚪ Not Started |

**Total Estimated Timeline**: ~7.5 weeks (with parallelization)

---

## Dependency Graph

```
Phase 1 (Parallel Streams):

Stream A:          Stream B:          Stream C:
HIER-004 ───┐     HIER-001          HIER-006
    │       │         │                 │
HIER-003 ───┤     HIER-008          HIER-007
    │       │
HIER-005 ◄──┘

Phase 2 (After Phase 1):

HIER-009 ──┐
           ├──► HIER-012 ──► HIER-013 ──┐
HIER-010 ──┘                            │
                                        ├──► HIER-015
HIER-011 ────► HIER-014 ───────────────┘

Phase 3 (After Phase 2):

HIER-016 ──► HIER-017
         ├─► HIER-018 ◄── HIER-019 ◄── HIER-020
         ├─► HIER-021
         └─► HIER-022
              
         HIER-023 (independent)

Phase 4 (After Phase 3):

HIER-024 ──► HIER-025 ──► HIER-028
    │
    └──► HIER-026 ──► HIER-027

Phase 5 (After Phase 4):

HIER-029 ──┐
           ├──► HIER-031 ──► HIER-032
HIER-030 ──┘
                        
HIER-033 (future, after deprecation period)
```


---

## Phase 1: Core Components ✅ COMPLETE

> **Goal**: Create foundational interfaces and classes that coexist with existing code.
> **Breaking Changes**: None
> **Status**: All 8 tasks completed (2025-12-09)

### HIER-001: NavRenderScope Interface ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Define the core interface providing context to all renderers |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavRenderScope.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Interface with navigator, cache, animationCoordinator, predictiveBackController, sharedTransitionScope
- [x] `screenRegistry` and `wrapperRegistry` properties
- [x] `withAnimatedVisibilityScope()` composable function
- [x] Full KDoc documentation

### HIER-002: WrapperRegistry Interface ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Define interface for KSP-generated wrapper registry |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/registry/WrapperRegistry.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `TabWrapper()` composable function signature
- [x] `PaneWrapper()` composable function signature
- [x] `hasTabWrapper(tabNodeKey: String)` function
- [x] `hasPaneWrapper(paneNodeKey: String)` function
- [x] Full KDoc documentation

### HIER-003: TransitionRegistry Interface ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Define interface for KSP-generated transition registry |
| **Dependencies** | HIER-004 |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/registry/TransitionRegistry.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `getTransition(destinationClass: KClass<*>): NavTransition?` function
- [x] Default implementation returning null
- [x] Full KDoc documentation

### HIER-004: NavTransition Data Class ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Define immutable transition configuration |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/animation/NavTransition.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Data class with `enter`, `exit`, `popEnter`, `popExit`
- [x] `createTransitionSpec(isBack: Boolean): ContentTransform` function
- [x] `reversed()` function for direction inversion
- [x] Companion with `SlideHorizontal`, `Fade`, `None` presets
- [x] Full KDoc documentation

### HIER-005: AnimationCoordinator Class ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Manages transition resolution based on NavNode and annotations |
| **Dependencies** | HIER-003, HIER-004 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/animation/AnimationCoordinator.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `getTransition(from: NavNode?, to: NavNode, isBack: Boolean): NavTransition`
- [x] `getTabTransition(fromIndex: Int?, toIndex: Int): NavTransition`
- [x] `getPaneTransition(fromRole: PaneRole?, toRole: PaneRole): NavTransition`
- [x] Uses TransitionRegistry for annotation-based lookup
- [x] Falls back to default transitions
- [x] @Stable annotation
- [x] Full KDoc documentation

### HIER-006: PredictiveBackController Class ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Centralized gesture handling for predictive back |
| **Dependencies** | None |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/gesture/PredictiveBackController.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `isActive` state property
- [x] `progress` state property (0f-1f)
- [x] `suspend fun handleGesture(backEvent: Flow<BackEventCompat>, onNavigateBack: () -> Unit)`
- [x] Internal `animateToCompletion()` with spring animation
- [x] Internal `animateToCancel()` with spring animation
- [x] Progress clamping to 0.25f during gesture
- [x] @Stable annotation
- [x] Full KDoc documentation

### HIER-007: ComposableCache Enhancement ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Enhance existing cache for NavNode-keyed caching |
| **Dependencies** | None |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/ComposableCache.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `CachedEntry(key: String, content: @Composable () -> Unit)` composable
- [x] `lock(key: String)` and `unlock(key: String)` for animation protection
- [x] LRU eviction that respects locked entries
- [x] Integration with `SaveableStateProvider`
- [x] Full KDoc documentation

### HIER-008: TabWrapperScope and PaneWrapperScope ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Define scope interfaces for wrapper composables |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/TabWrapperScope.kt`, `PaneWrapperScope.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `TabWrapperScope` with `navigator`, `activeIndex`, `tabs`, `switchTab()`
- [x] `PaneWrapperScope` with `navigator`, `paneContents`, `activePaneRole`, `isExpanded`
- [x] `PaneContentSlot` data class with `role`, `isVisible`, `content`
- [x] @Stable annotations
- [x] Full KDoc documentation

---

## Phase 2: KSP Updates ✅ COMPLETE

> **Goal**: Add new annotations and KSP code generation for wrappers and transitions.
> **Breaking Changes**: None (additive)
> **Status**: All 7 tasks completed (2025-12-09)

### HIER-009: @TabWrapper Annotation ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Define annotation for marking tab wrapper composables |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabWrapper.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `@Target(AnnotationTarget.FUNCTION)`
- [x] `@Retention(AnnotationRetention.RUNTIME)`
- [x] `tabClass: KClass<*>` parameter
- [x] Full KDoc documentation with example

### HIER-010: @PaneWrapper Annotation ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Define annotation for marking pane wrapper composables |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneWrapper.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `@Target(AnnotationTarget.FUNCTION)`
- [x] `@Retention(AnnotationRetention.RUNTIME)`
- [x] `paneClass: KClass<*>` parameter
- [x] Full KDoc documentation with example

### HIER-011: @Transition Annotation Enhancement ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Add/enhance annotation for per-destination transitions |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Transition.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `@Target(AnnotationTarget.CLASS)` (on Destination classes)
- [x] `type: TransitionType` parameter (enum: SlideHorizontal, SlideVertical, Fade, None, Custom)
- [x] `customTransition: KClass<*>` for custom implementations
- [x] Full KDoc documentation with examples

### HIER-012: KSP WrapperExtractor ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Create KSP extractor for @TabWrapper and @PaneWrapper |
| **Dependencies** | HIER-009, HIER-010 |
| **Effort** | L |
| **Files** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractor/WrapperExtractor.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Finds all `@TabWrapper` annotated functions
- [x] Validates function signature (TabWrapperScope receiver, content parameter)
- [x] Finds all `@PaneWrapper` annotated functions
- [x] Validates function signature (PaneWrapperScope receiver, content parameter)
- [x] Builds mapping of tab/pane class → wrapper function
- [x] Error reporting for invalid signatures
- [x] Returns `WrapperInfo` with all extracted data

### HIER-013: WrapperRegistry Code Generator ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Generate WrapperRegistry implementation |
| **Dependencies** | HIER-002, HIER-012 |
| **Effort** | M |
| **Files** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generator/WrapperRegistryGenerator.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Generates `GeneratedWrapperRegistry` object
- [x] Implements `WrapperRegistry` interface
- [x] `TabWrapper()` with when-expression dispatching to annotated functions
- [x] `PaneWrapper()` with when-expression dispatching to annotated functions
- [x] Default fallback for missing wrappers
- [x] Proper imports generated

### HIER-014: TransitionRegistry Code Generator ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Generate TransitionRegistry from @Transition annotations |
| **Dependencies** | HIER-003, HIER-011 |
| **Effort** | M |
| **Files** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generator/TransitionRegistryGenerator.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Generates `GeneratedTransitionRegistry` object
- [x] Implements `TransitionRegistry` interface
- [x] Maps destination classes to NavTransition instances
- [x] Handles TransitionType enum
- [x] Handles custom transition class references

### HIER-015: KSP Integration ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Integrate new extractors/generators into main KSP processing |
| **Dependencies** | HIER-012, HIER-013, HIER-014 |
| **Effort** | S |
| **Files** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt` (modified) |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] WrapperExtractor invoked
- [x] WrapperRegistryGenerator invoked
- [x] TransitionRegistryGenerator invoked
- [x] Proper ordering (registries generated after extraction)

---

## Phase 3: Renderer Implementation

> **Goal**: Implement the hierarchical rendering components.
> **Breaking Changes**: None (new components alongside existing)

### HIER-016: NavTreeRenderer

| Attribute | Value |
|-----------|-------|
| **Description** | Core recursive renderer that dispatches to node-specific renderers |
| **Dependencies** | HIER-001 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt` |

**Acceptance Criteria**:
- [ ] `@Composable fun NavTreeRenderer(node, previousNode, scope, modifier)`
- [ ] When expression dispatching by NavNode type
- [ ] Handles null previousNode gracefully
- [ ] Full KDoc documentation

### HIER-017: ScreenRenderer

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for ScreenNode leaf nodes |
| **Dependencies** | HIER-016, HIER-007 |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/ScreenRenderer.kt` |

**Acceptance Criteria**:
- [ ] Uses `ComposableCache.CachedEntry()` for state preservation
- [ ] Provides `LocalBackStackEntry`
- [ ] Invokes `ScreenRegistry.Content()` with all required parameters
- [ ] Passes shared transition and animation visibility scopes
- [ ] Full KDoc documentation

### HIER-018: StackRenderer

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for StackNode with animated transitions |
| **Dependencies** | HIER-016, HIER-019, HIER-005 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/StackRenderer.kt` |

**Acceptance Criteria**:
- [ ] Renders only active child (last in children list)
- [ ] Detects back navigation via stack comparison
- [ ] Uses AnimatedNavContent for transitions
- [ ] Enables predictive back for root stacks only
- [ ] Recurses via NavTreeRenderer for active child
- [ ] Full KDoc documentation

### HIER-019: AnimatedNavContent

| Attribute | Value |
|-----------|-------|
| **Description** | Custom AnimatedContent variant for navigation |
| **Dependencies** | HIER-004, HIER-006, HIER-020 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/AnimatedNavContent.kt` |

**Acceptance Criteria**:
- [ ] Generic `<T : NavNode>` type parameter
- [ ] Tracks displayedState and previousState for animation direction
- [ ] Switches to PredictiveBackContent when gesture active
- [ ] Uses AnimatedContent for standard transitions
- [ ] Provides AnimatedVisibilityScope to content
- [ ] Properly updates state tracking after animation
- [ ] Full KDoc documentation

### HIER-020: PredictiveBackContent

| Attribute | Value |
|-----------|-------|
| **Description** | Renders overlapping content during predictive back gesture |
| **Dependencies** | HIER-006, HIER-007 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackContent.kt` |

**Acceptance Criteria**:
- [ ] Renders previous content statically behind current
- [ ] Applies parallax transform to previous content
- [ ] Applies translate + scale transform to current content based on progress
- [ ] Uses CachedEntry for both current and previous
- [ ] Provides StaticAnimatedVisibilityScope for content
- [ ] Full KDoc documentation

### HIER-021: TabRenderer

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for TabNode with wrapper composition |
| **Dependencies** | HIER-016, HIER-008, HIER-002, HIER-019 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/TabRenderer.kt` |

**Acceptance Criteria**:
- [ ] Creates TabWrapperScope with current tab state
- [ ] Caches ENTIRE TabNode (wrapper + content)
- [ ] Invokes WrapperRegistry.TabWrapper with content slot
- [ ] Content slot uses AnimatedNavContent for tab switching
- [ ] Recurses via NavTreeRenderer for active stack
- [ ] Full KDoc documentation

### HIER-022: PaneRenderer

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for PaneNode with adaptive layout |
| **Dependencies** | HIER-016, HIER-008, HIER-002 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PaneRenderer.kt` |

**Acceptance Criteria**:
- [ ] Detects window size class for expanded vs compact
- [ ] Creates PaneWrapperScope with pane configuration
- [ ] Caches ENTIRE PaneNode
- [ ] For expanded: invokes WrapperRegistry.PaneWrapper with multiple slots
- [ ] For compact: behaves like StackRenderer (single pane visible)
- [ ] Full KDoc documentation

### HIER-023: StaticAnimatedVisibilityScope

| Attribute | Value |
|-----------|-------|
| **Description** | Fake AnimatedVisibilityScope for predictive back rendering |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/StaticAnimatedVisibilityScope.kt` |

**Acceptance Criteria**:
- [ ] Implements AnimatedVisibilityScope interface
- [ ] Returns stable/completed transition values
- [ ] Used when content is rendered outside AnimatedContent
- [ ] Full KDoc documentation

---

## Phase 4: Integration

> **Goal**: Create the new QuoVadisHost and enable feature flag switching.
> **Breaking Changes**: None (new API alongside existing)

### HIER-024: HierarchicalQuoVadisHost

| Attribute | Value |
|-----------|-------|
| **Description** | New QuoVadisHost using hierarchical rendering |
| **Dependencies** | HIER-001 through HIER-022 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/HierarchicalQuoVadisHost.kt` |

**Acceptance Criteria**:
- [ ] Collects navigator state
- [ ] Tracks previous state for animations
- [ ] Creates NavRenderScope implementation
- [ ] Handles PredictiveBackHandler at root
- [ ] Wraps in SharedTransitionLayout
- [ ] Invokes NavTreeRenderer with root node
- [ ] Provides LocalNavRenderScope
- [ ] Full KDoc documentation

### HIER-025: RenderingMode Enum and Feature Flag

| Attribute | Value |
|-----------|-------|
| **Description** | Add rendering mode selection to QuoVadisHost |
| **Dependencies** | HIER-024 |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt` (modify) |

**Acceptance Criteria**:
- [ ] `enum class RenderingMode { Flattened, Hierarchical }`
- [ ] Parameter added to QuoVadisHost: `renderingMode: RenderingMode = RenderingMode.Flattened`
- [ ] Dispatches to existing or new implementation based on mode
- [ ] Documentation explaining the two modes
- [ ] Default to Flattened for backward compatibility

### HIER-026: FakeNavRenderScope

| Attribute | Value |
|-----------|-------|
| **Description** | Test fake for composable testing |
| **Dependencies** | HIER-001 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavRenderScope.kt` |

**Acceptance Criteria**:
- [ ] Implements NavRenderScope
- [ ] Uses FakeNavigator internally
- [ ] Provides controllable animation state
- [ ] Provides controllable predictive back state
- [ ] Documentation with usage examples
- [ ] Unit tests for the fake itself

### HIER-027: Integration Tests

| Attribute | Value |
|-----------|-------|
| **Description** | Comprehensive integration tests for hierarchical rendering |
| **Dependencies** | HIER-024, HIER-026 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/` |

**Acceptance Criteria**:
- [ ] Test screen navigation with animations
- [ ] Test stack push/pop
- [ ] Test tab switching
- [ ] Test pane layout changes
- [ ] Test predictive back gesture
- [ ] Test shared element transitions
- [ ] Test cache eviction behavior
- [ ] Tests pass on all platforms

### HIER-028: Demo App Update

| Attribute | Value |
|-----------|-------|
| **Description** | Update demo app to use hierarchical rendering |
| **Dependencies** | HIER-025 |
| **Effort** | M |
| **Files** | `composeApp/src/commonMain/kotlin/` (multiple files) |

**Acceptance Criteria**:
- [ ] Add @TabWrapper annotation to existing tab wrapper
- [ ] Add @PaneWrapper annotation to existing pane wrapper
- [ ] Switch to RenderingMode.Hierarchical
- [ ] Verify all demo patterns still work
- [ ] Visual verification on Android and iOS
- [ ] No regression in existing functionality

---

## Phase 5: Migration

> **Goal**: Deprecate old API and provide migration guidance.
> **Breaking Changes**: Deprecations (warnings only)

### HIER-029: Deprecate Runtime Wrappers

| Attribute | Value |
|-----------|-------|
| **Description** | Mark runtime wrapper parameters as deprecated |
| **Dependencies** | HIER-028 |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt` |

**Acceptance Criteria**:
- [ ] `@Deprecated` annotation on tabWrapper parameter
- [ ] `@Deprecated` annotation on paneWrapper parameter
- [ ] Deprecation message pointing to @TabWrapper/@PaneWrapper annotations
- [ ] ReplaceWith hint where applicable

### HIER-030: Deprecate RenderableSurface API

| Attribute | Value |
|-----------|-------|
| **Description** | Mark flattening API as deprecated |
| **Dependencies** | HIER-028 |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/` (multiple) |

**Acceptance Criteria**:
- [ ] `@Deprecated` on RenderableSurface class
- [ ] `@Deprecated` on TreeFlattener class
- [ ] `@Deprecated` on RenderingMode.Flattened
- [ ] Clear migration guidance in deprecation message

### HIER-031: Migration Guide Document

| Attribute | Value |
|-----------|-------|
| **Description** | Create comprehensive migration documentation |
| **Dependencies** | HIER-029, HIER-030 |
| **Effort** | M |
| **Files** | `quo-vadis-core/docs/MIGRATION_HIERARCHICAL_RENDERING.md` |

**Acceptance Criteria**:
- [ ] Overview of changes
- [ ] Step-by-step migration instructions
- [ ] Before/after code examples
- [ ] Runtime wrapper → annotation migration
- [ ] Common issues and solutions
- [ ] FAQ section

### HIER-032: Documentation Update

| Attribute | Value |
|-----------|-------|
| **Description** | Update all documentation to reflect new architecture |
| **Dependencies** | HIER-031 |
| **Effort** | M |
| **Files** | `quo-vadis-core/docs/` (multiple), `README.md` |

**Acceptance Criteria**:
- [ ] ARCHITECTURE.md updated
- [ ] API_REFERENCE.md updated
- [ ] All code examples updated
- [ ] Deprecation notices added where relevant
- [ ] New annotations documented

### HIER-033: Cleanup Task (Future)

| Attribute | Value |
|-----------|-------|
| **Description** | Remove deprecated flattening code after deprecation period |
| **Dependencies** | HIER-030 (after deprecation period) |
| **Effort** | M |
| **Files** | Multiple |

**Acceptance Criteria**:
- [ ] Remove RenderableSurface and related classes
- [ ] Remove TreeFlattener
- [ ] Remove RenderingMode enum (hierarchical becomes default)
- [ ] Remove runtime wrapper parameters
- [ ] Update minimum library version requirements
- [ ] Version bump to next major

**Note**: This task should be executed after a deprecation period (e.g., 2 minor versions).

---

## Risk Mitigation

| Risk | Mitigation |
|------|------------|
| **Animation regressions** | Comprehensive visual testing on demo app; keep flattened mode as fallback |
| **KSP complexity** | Incremental development with unit tests for each generator |
| **Performance** | Benchmark cache hit rates and recomposition counts |
| **Breaking existing code** | Feature flag allows gradual migration; deprecation warnings |
| **Platform differences** | Test on Android, iOS, Desktop, Web for each phase |

---

## Success Metrics

1. **Wrapper/content composition**: Layout inspector shows parent-child relationship (not siblings)
2. **Animation coordination**: Tab wrapper + content animate as single unit
3. **Predictive back**: Entire TabNode transforms together during gesture
4. **No visual regressions**: Demo app looks and behaves identically (better animations)
5. **Test coverage**: >80% for new components
6. **Documentation**: All new APIs have KDoc; migration guide reviewed

---

## Next Steps

1. Review and approve this implementation plan
2. Create GitHub issues for Phase 1 tasks
3. Begin parallel implementation of Phase 1 streams
4. Set up visual regression testing infrastructure
