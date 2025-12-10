# RENDER-011: Implementation Plan

**Related**: [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md)  
**Status**: ✅ Complete (All 5 Phases)  
**Created**: 2025-12-09
**Last Updated**: 2025-12-10

## Overview

This document breaks down the hierarchical rendering engine architecture into actionable implementation tasks organized in phases.

---

## Task Summary

| Phase | Tasks | Estimated Effort | Parallel Streams | Status |
|-------|-------|------------------|------------------|--------|
| 1. Core Components | HIER-001 to HIER-008 | 2 weeks | 3 | ✅ Complete |
| 2. KSP Updates | HIER-009 to HIER-015 | 1.5 weeks | 2 | ✅ Complete |
| 3. Renderer Implementation | HIER-016 to HIER-023 | 2.5 weeks | 2 | ✅ Complete |
| 4. Integration | HIER-024 to HIER-028 | 1 week | 1 | ✅ Complete |
| 5. Migration | HIER-029 to HIER-033 | 0.5 weeks | 1 | ✅ Complete |

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

## Phase 3: Renderer Implementation ✅ COMPLETE

> **Goal**: Implement the hierarchical rendering components.
> **Breaking Changes**: None (new components alongside existing)
> **Status**: All 8 tasks completed (2025-12-09)

### HIER-016: NavTreeRenderer ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Core recursive renderer that dispatches to node-specific renderers |
| **Dependencies** | HIER-001 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `@Composable fun NavTreeRenderer(node, previousNode, scope, modifier)`
- [x] When expression dispatching by NavNode type
- [x] Handles null previousNode gracefully
- [x] Full KDoc documentation

### HIER-017: ScreenRenderer ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for ScreenNode leaf nodes |
| **Dependencies** | HIER-016, HIER-007 |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Uses `ComposableCache.CachedEntry()` for state preservation
- [x] Provides `LocalScreenNode` (equivalent to LocalBackStackEntry)
- [x] Invokes `ScreenRegistry.Content()` with all required parameters
- [x] Passes shared transition and animation visibility scopes
- [x] Full KDoc documentation

### HIER-018: StackRenderer ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for StackNode with animated transitions |
| **Dependencies** | HIER-016, HIER-019, HIER-005 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Renders only active child (last in children list)
- [x] Detects back navigation via stack comparison
- [x] Uses AnimatedNavContent for transitions
- [x] Enables predictive back for root stacks only
- [x] Recurses via NavTreeRenderer for active child
- [x] Full KDoc documentation

### HIER-019: AnimatedNavContent ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Custom AnimatedContent variant for navigation |
| **Dependencies** | HIER-004, HIER-006, HIER-020 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/AnimatedNavContent.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Generic `<T : NavNode>` type parameter
- [x] Tracks displayedState and previousState for animation direction
- [x] Switches to PredictiveBackContent when gesture active
- [x] Uses AnimatedContent for standard transitions
- [x] Provides AnimatedVisibilityScope to content
- [x] Properly updates state tracking after animation
- [x] Full KDoc documentation

### HIER-020: PredictiveBackContent ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Renders overlapping content during predictive back gesture |
| **Dependencies** | HIER-006, HIER-007 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackContent.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Renders previous content statically behind current
- [x] Applies parallax transform to previous content
- [x] Applies translate + scale transform to current content based on progress
- [x] Uses CachedEntry for both current and previous
- [x] Provides StaticAnimatedVisibilityScope for content
- [x] Full KDoc documentation

### HIER-021: TabRenderer ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for TabNode with wrapper composition |
| **Dependencies** | HIER-016, HIER-008, HIER-002, HIER-019 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Creates TabWrapperScope with current tab state
- [x] Caches ENTIRE TabNode (wrapper + content)
- [x] Invokes WrapperRegistry.TabWrapper with content slot
- [x] Content slot uses AnimatedNavContent for tab switching
- [x] Recurses via NavTreeRenderer for active stack
- [x] Full KDoc documentation

### HIER-022: PaneRenderer ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Renderer for PaneNode with adaptive layout |
| **Dependencies** | HIER-016, HIER-008, HIER-002 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Detects window size class for expanded vs compact
- [x] Creates PaneWrapperScope with pane configuration
- [x] Caches ENTIRE PaneNode
- [x] For expanded: invokes WrapperRegistry.PaneWrapper with multiple slots
- [x] For compact: behaves like StackRenderer (single pane visible)
- [x] Full KDoc documentation

### HIER-023: StaticAnimatedVisibilityScope ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Fake AnimatedVisibilityScope for predictive back rendering |
| **Dependencies** | None |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/StaticAnimatedVisibilityScope.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Implements AnimatedVisibilityScope interface
- [x] Returns stable/completed transition values
- [x] Used when content is rendered outside AnimatedContent
- [x] Full KDoc documentation

---

## Phase 4: Integration ✅ COMPLETE

> **Goal**: Create the new QuoVadisHost and enable feature flag switching.
> **Breaking Changes**: None (new API alongside existing)
> **Status**: All 5 tasks completed (2025-12-10)

### HIER-024: HierarchicalQuoVadisHost ✅

| Attribute | Value |
|-----------|-------|
| **Description** | New QuoVadisHost using hierarchical rendering |
| **Dependencies** | HIER-001 through HIER-022 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/HierarchicalQuoVadisHost.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Collects navigator state
- [x] Tracks previous state for animations
- [x] Creates NavRenderScope implementation
- [x] Handles PredictiveBackHandler at root
- [x] Wraps in SharedTransitionLayout
- [x] Invokes NavTreeRenderer with root node
- [x] Provides LocalNavRenderScope
- [x] Full KDoc documentation

### HIER-025: RenderingMode Enum and Feature Flag ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Add rendering mode selection to QuoVadisHost |
| **Dependencies** | HIER-024 |
| **Effort** | S |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt` (modify) |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `enum class RenderingMode { Flattened, Hierarchical }`
- [x] Parameter added to QuoVadisHost: `renderingMode: RenderingMode = RenderingMode.Flattened`
- [x] Dispatches to existing or new implementation based on mode
- [x] Documentation explaining the two modes
- [x] Default to Flattened for backward compatibility

### HIER-026: FakeNavRenderScope ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Test fake for composable testing |
| **Dependencies** | HIER-001 |
| **Effort** | M |
| **Files** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavRenderScope.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Implements NavRenderScope
- [x] Uses FakeNavigator internally
- [x] Provides controllable animation state
- [x] Provides controllable predictive back state
- [x] Documentation with usage examples
- [x] Unit tests for the fake itself

### HIER-027: Integration Tests ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Comprehensive integration tests for hierarchical rendering |
| **Dependencies** | HIER-024, HIER-026 |
| **Effort** | L |
| **Files** | `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Test screen navigation with animations
- [x] Test stack push/pop
- [x] Test tab switching
- [x] Test pane layout changes
- [x] Test predictive back gesture
- [x] Test shared element transitions (limited - no full compose test framework)
- [x] Test cache eviction behavior
- [x] Tests in commonTest (platform-agnostic)

### HIER-028: Demo App Update ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Update demo app to use hierarchical rendering |
| **Dependencies** | HIER-025 |
| **Effort** | M |
| **Files** | `composeApp/src/commonMain/kotlin/` (multiple files) |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Add rendering mode toggle in Settings → Developer Options
- [x] Created `RenderingModeManager` for preference persistence
- [x] Created `RenderingModeSettingItem` UI component
- [x] Pass `renderingMode` to `QuoVadisHost`
- [x] Verify all demo patterns still work (build passes)
- [x] Visual verification guidance documented
- [x] No regression in existing functionality

---

## Phase 5: Migration ✅ COMPLETE

> **Goal**: Deprecate old API and provide migration guidance.
> **Breaking Changes**: Deprecations (warnings only)
> **Status**: All 4 active tasks completed (2025-12-10). HIER-033 deferred.

### HIER-029: Deprecate Runtime Wrappers ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Mark runtime wrapper parameters as deprecated |
| **Dependencies** | HIER-028 |
| **Effort** | S |
| **Files** | `quo-vadis-core/.../compose/wrapper/TabWrapper.kt`, `PaneWrapper.kt`, `QuoVadisHost.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `@Deprecated` annotation on `TabWrapper` type alias
- [x] `@Deprecated` annotation on `PaneWrapper` type alias
- [x] `@Deprecated` on `RenderingMode.Flattened` enum value
- [x] Deprecation message pointing to @TabWrapper/@PaneWrapper annotations
- [x] ReplaceWith hint on RenderingMode.Flattened

### HIER-030: Deprecate RenderableSurface API ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Mark flattening API as deprecated |
| **Dependencies** | HIER-028 |
| **Effort** | S |
| **Files** | `quo-vadis-core/.../compose/RenderableSurface.kt`, `TreeFlattener.kt` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] `@Deprecated` on `RenderableSurface` data class
- [x] `@Deprecated` on `TreeFlattener` class
- [x] `@Deprecated` on `SurfaceNodeType`, `SurfaceRenderingMode`, `SurfaceTransitionState`, `SurfaceAnimationSpec`, `PaneStructure`
- [x] Clear migration guidance in deprecation messages

### HIER-031: Migration Guide Document ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Create comprehensive migration documentation |
| **Dependencies** | HIER-029, HIER-030 |
| **Effort** | M |
| **Files** | `quo-vadis-core/docs/MIGRATION_HIERARCHICAL_RENDERING.md` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] Overview of changes (mode comparison table)
- [x] Step-by-step migration instructions (checklist)
- [x] Before/after code examples for TabWrapper, PaneWrapper, Screen
- [x] Runtime wrapper → annotation migration
- [x] Custom animation migration (@Transition)
- [x] Common issues and solutions (4 documented)
- [x] Version timeline section

### HIER-032: Documentation Update ✅

| Attribute | Value |
|-----------|-------|
| **Description** | Update all documentation to reflect new architecture |
| **Dependencies** | HIER-031 |
| **Effort** | M |
| **Files** | `quo-vadis-core/docs/ARCHITECTURE.md`, `API_REFERENCE.md`, `README.md` |
| **Status** | ✅ Complete |

**Acceptance Criteria**:
- [x] ARCHITECTURE.md updated (Section 9: Hierarchical Rendering Architecture)
- [x] API_REFERENCE.md updated (@TabWrapper, @PaneWrapper, @Transition, @Screen sections)
- [x] README.md updated (Hierarchical Rendering in Key Features)
- [x] New annotations documented with examples
- [x] Link to migration guide from ARCHITECTURE.md

### HIER-033: Cleanup Task (Future) ⏸️ DEFERRED

| Attribute | Value |
|-----------|-------|
| **Description** | Remove deprecated flattening code after deprecation period |
| **Dependencies** | HIER-030 (after deprecation period) |
| **Effort** | M |
| **Files** | Multiple |
| **Status** | ⏸️ Deferred to future major version |

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
