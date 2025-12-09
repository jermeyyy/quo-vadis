# RENDER-011: Hierarchical Rendering Engine - Implementation Tasks

**Status**: Planning  
**Created**: 2025-12-09  
**Based on**: [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md)

---

## Overview

This document breaks down the Hierarchical Rendering Engine architecture into actionable implementation tasks across 5 phases. Tasks are designed to minimize breaking changes and allow parallel development where possible.

### Task Estimation Legend

| Size | Effort | Description |
|------|--------|-------------|
| **S** | 0.5-1 day | Simple, isolated change |
| **M** | 2-3 days | Moderate complexity, some integration |
| **L** | 4-5 days | Complex, multiple components involved |

### Dependency Notation

- `→` indicates a hard dependency (must complete before starting)
- `~` indicates a soft dependency (benefits from but doesn't require)

---

## Phase 1: Core Components

**Goal**: Create new interfaces, classes, and types that can coexist with existing code. No breaking changes.

### HIER-001: NavRenderScope Interface

| Field | Value |
|-------|-------|
| **Title** | Create NavRenderScope interface and core types |
| **Description** | Define the `NavRenderScope` interface that provides context to all hierarchical renderers. This is the foundational type that all other rendering components depend on. |
| **Dependencies** | None |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavRenderScope.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `NavRenderScope` interface defined with all required properties:
  - `navigator: Navigator`
  - `cache: ComposableCache`
  - `animationCoordinator: AnimationCoordinator`
  - `predictiveBackController: PredictiveBackController`
  - `sharedTransitionScope: SharedTransitionScope`
  - `screenRegistry: ScreenRegistry`
  - `wrapperRegistry: WrapperRegistry`
- [ ] `@Composable fun withAnimatedVisibilityScope()` method declared
- [ ] `LocalNavRenderScope` CompositionLocal created
- [ ] KDoc documentation complete

---

### HIER-002: WrapperRegistry Interface

| Field | Value |
|-------|-------|
| **Title** | Define WrapperRegistry interface for KSP-generated wrappers |
| **Description** | Create the `WrapperRegistry` interface that KSP will implement. This defines how tab and pane wrappers are resolved at runtime. |
| **Dependencies** | HIER-001 |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/WrapperRegistry.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `WrapperRegistry` interface with:
  - `@Composable fun TabWrapper(wrapperScope: TabWrapperScope, tabNodeKey: String, content: @Composable () -> Unit)`
  - `@Composable fun PaneWrapper(wrapperScope: PaneWrapperScope, paneNodeKey: String, content: @Composable PaneContentScope.() -> Unit)`
- [ ] `DefaultWrapperRegistry` object providing fallback implementations
- [ ] Unit tests for default registry behavior

---

### HIER-003: NavTransition Data Class

| Field | Value |
|-------|-------|
| **Title** | Create NavTransition animation type |
| **Description** | Define the `NavTransition` data class encapsulating enter/exit/popEnter/popExit animations. Include standard presets. |
| **Dependencies** | None |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTransition.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `NavTransition` data class with `enter`, `exit`, `popEnter`, `popExit` properties
- [ ] `createTransitionSpec(isBack: Boolean): ContentTransform` function
- [ ] `reversed()` function for direction swapping
- [ ] Companion object with presets:
  - `NavTransition.SlideHorizontal`
  - `NavTransition.Fade`
  - `NavTransition.None`
  - `NavTransition.SlideVertical`
- [ ] Unit tests for `createTransitionSpec` logic

---

### HIER-004: AnimationCoordinator

| Field | Value |
|-------|-------|
| **Title** | Create AnimationCoordinator for transition resolution |
| **Description** | Implement `AnimationCoordinator` that resolves transitions based on destination annotations and node types. |
| **Dependencies** | HIER-003 |
| **Effort** | M |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/AnimationCoordinator.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `AnimationCoordinator` class with:
  - Constructor accepting `TransitionRegistry` and `defaultTransition`
  - `getTransition(from: NavNode?, to: NavNode, isBack: Boolean): NavTransition`
  - `getTabTransition(fromIndex: Int?, toIndex: Int): NavTransition`
- [ ] Back navigation detection logic
- [ ] Integration with existing `TransitionRegistry` (if exists) or define new interface
- [ ] Unit tests covering:
  - Forward navigation transitions
  - Back navigation transitions
  - Tab switching directions
  - Annotation-based custom transitions

---

### HIER-005: PredictiveBackController

| Field | Value |
|-------|-------|
| **Title** | Create centralized PredictiveBackController |
| **Description** | Implement `PredictiveBackController` for gesture handling, progress tracking, and animation state management. |
| **Dependencies** | None |
| **Effort** | M |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackController.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `PredictiveBackController` class with:
  - `isActive: Boolean` state property
  - `progress: Float` state property (0.0 to 1.0, clamped at 0.25)
  - `suspend fun handleGesture(backEvent: Flow<BackEventCompat>, onNavigateBack: () -> Unit)`
- [ ] Animation to completion (`animateToCompletion()`)
- [ ] Animation on cancellation (`animateToCancel()`)
- [ ] Spring-based animation specs
- [ ] `PROGRESS_CLAMP` constant (0.25f)
- [ ] Unit tests for state transitions

---

### HIER-006: Enhanced ComposableCache

| Field | Value |
|-------|-------|
| **Title** | Enhance ComposableCache with LRU eviction and locking |
| **Description** | Extend or rewrite `ComposableCache` to support subtree caching with LRU eviction policy and entry locking during animations. |
| **Dependencies** | None |
| **Effort** | M |
| **Files to Modify** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/ComposableCache.kt` |

**Acceptance Criteria**:
- [ ] LRU eviction with configurable `maxSize` (default 10)
- [ ] `lock(key: String)` / `unlock(key: String)` methods for animation safety
- [ ] `@Composable fun CachedEntry(key: String, content: @Composable () -> Unit)` API
- [ ] Access order tracking via `accessOrder` list
- [ ] Locked entries excluded from eviction
- [ ] Thread-safe state management with `mutableStateMapOf`/`mutableStateListOf`
- [ ] Unit tests for:
  - Cache hit/miss
  - LRU eviction order
  - Lock prevents eviction
  - Unlock triggers eviction check

---

### HIER-007: TransitionRegistry Interface

| Field | Value |
|-------|-------|
| **Title** | Define TransitionRegistry for destination-specific animations |
| **Description** | Create interface for looking up custom transitions based on destination class annotations. |
| **Dependencies** | HIER-003 |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/TransitionRegistry.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `TransitionRegistry` interface with:
  - `fun getTransition(destinationClass: KClass<*>): NavTransition?`
- [ ] `EmptyTransitionRegistry` object returning `null` for all lookups
- [ ] Documentation explaining KSP will generate implementations

---

### HIER-008: NavRenderScopeImpl

| Field | Value |
|-------|-------|
| **Title** | Implement NavRenderScopeImpl |
| **Description** | Create the concrete implementation of `NavRenderScope` that wires together all components. |
| **Dependencies** | HIER-001, HIER-002, HIER-004, HIER-005, HIER-006, HIER-007 |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavRenderScopeImpl.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `NavRenderScopeImpl` implementing `NavRenderScope`
- [ ] All properties properly initialized from constructor
- [ ] `withAnimatedVisibilityScope` properly stores and provides scope
- [ ] `@Stable` annotation for Compose stability
- [ ] `rememberNavRenderScope()` factory function

---

## Phase 2: KSP Updates

**Goal**: Add new annotations and generate `WrapperRegistry` implementation.

### HIER-009: @TabWrapper Annotation

| Field | Value |
|-------|-------|
| **Title** | Add @TabWrapper annotation |
| **Description** | Create annotation for marking composable functions as tab wrappers. |
| **Dependencies** | None |
| **Effort** | S |
| **Files to Modify** | `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt` |

**Acceptance Criteria**:
- [ ] `@TabWrapper(tabClass: KClass<*>)` annotation defined
- [ ] `@Target(AnnotationTarget.FUNCTION)`
- [ ] `@Retention(AnnotationRetention.RUNTIME)` (or SOURCE if KSP handles it)
- [ ] KDoc with usage example
- [ ] Validates that `tabClass` is annotated with `@Tab`

---

### HIER-010: @PaneWrapper Annotation

| Field | Value |
|-------|-------|
| **Title** | Add @PaneWrapper annotation |
| **Description** | Create annotation for marking composable functions as pane wrappers. |
| **Dependencies** | None |
| **Effort** | S |
| **Files to Modify** | `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneAnnotations.kt` |

**Acceptance Criteria**:
- [ ] `@PaneWrapper(paneClass: KClass<*>)` annotation defined
- [ ] `@Target(AnnotationTarget.FUNCTION)`
- [ ] KDoc with usage example
- [ ] Validates that `paneClass` is annotated with `@Pane`

---

### HIER-011: @Transition Annotation

| Field | Value |
|-------|-------|
| **Title** | Add @Transition annotation for per-screen animations |
| **Description** | Create annotation for specifying custom transitions on screen composables. |
| **Dependencies** | HIER-003 |
| **Effort** | S |
| **Files to Create** | `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Transition.kt` |

**Acceptance Criteria**:
- [ ] `@Transition(type: TransitionType = TransitionType.DEFAULT)` annotation
- [ ] `TransitionType` enum with: `DEFAULT`, `SLIDE_HORIZONTAL`, `SLIDE_VERTICAL`, `FADE`, `NONE`, `CUSTOM`
- [ ] Optional `customTransition: KClass<*>` parameter for `CUSTOM` type
- [ ] Can be applied to `@Screen` functions or destination classes

---

### HIER-012: WrapperExtractor for KSP

| Field | Value |
|-------|-------|
| **Title** | Create WrapperExtractor for processing wrapper annotations |
| **Description** | KSP extractor that finds and validates `@TabWrapper` and `@PaneWrapper` annotated functions. |
| **Dependencies** | HIER-009, HIER-010 |
| **Effort** | M |
| **Files to Create** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/WrapperExtractor.kt` |
| **Files to Modify** | None |

**Acceptance Criteria**:
- [ ] `WrapperExtractor` class with `extract()` method
- [ ] Finds all `@TabWrapper` and `@PaneWrapper` functions
- [ ] Validates function signature:
  - TabWrapper: `(TabWrapperScope, @Composable () -> Unit) -> Unit`
  - PaneWrapper: `(PaneWrapperScope, @Composable PaneContentScope.() -> Unit) -> Unit`
- [ ] Returns `WrapperInfo` model with function reference and target class
- [ ] Error reporting for invalid signatures

---

### HIER-013: WrapperInfo Model

| Field | Value |
|-------|-------|
| **Title** | Create WrapperInfo model for KSP |
| **Description** | Data model representing extracted wrapper information. |
| **Dependencies** | None |
| **Effort** | S |
| **Files to Create** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/WrapperInfo.kt` |

**Acceptance Criteria**:
- [ ] `WrapperInfo` data class with:
  - `wrapperType: WrapperType` (TAB, PANE)
  - `functionName: String`
  - `packageName: String`
  - `targetClass: ClassName`
  - `targetClassSimpleName: String`
- [ ] `WrapperType` enum defined

---

### HIER-014: WrapperRegistryGenerator

| Field | Value |
|-------|-------|
| **Title** | Generate WrapperRegistry implementation |
| **Description** | KSP generator that produces `GeneratedWrapperRegistry` from extracted wrapper info. |
| **Dependencies** | HIER-012, HIER-013, HIER-002 |
| **Effort** | L |
| **Files to Create** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/WrapperRegistryGenerator.kt` |
| **Files to Modify** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt` |

**Acceptance Criteria**:
- [ ] `WrapperRegistryGenerator` class
- [ ] Generates `GeneratedWrapperRegistry` object implementing `WrapperRegistry`
- [ ] `when` expression mapping node keys to wrapper functions
- [ ] Fallback to default wrapper for unmapped keys
- [ ] Proper imports generated
- [ ] Integration with main `QuoVadisSymbolProcessor`
- [ ] Unit tests with sample input/output

---

### HIER-015: TransitionRegistryGenerator

| Field | Value |
|-------|-------|
| **Title** | Generate TransitionRegistry implementation |
| **Description** | KSP generator that produces `GeneratedTransitionRegistry` from `@Transition` annotations. |
| **Dependencies** | HIER-011, HIER-007 |
| **Effort** | M |
| **Files to Create** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/TransitionRegistryGenerator.kt` |
| **Files to Modify** | `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt` |

**Acceptance Criteria**:
- [ ] `TransitionRegistryGenerator` class
- [ ] Generates `GeneratedTransitionRegistry` object
- [ ] Maps destination classes to `NavTransition` instances
- [ ] Handles all `TransitionType` enum values
- [ ] Supports custom transition classes
- [ ] Unit tests

---

## Phase 3: Renderer Implementation

**Goal**: Implement the hierarchical rendering components.

### HIER-016: AnimatedNavContent Composable

| Field | Value |
|-------|-------|
| **Title** | Create AnimatedNavContent composable |
| **Description** | Custom `AnimatedContent` variant optimized for navigation with predictive back support. |
| **Dependencies** | HIER-003, HIER-005 |
| **Effort** | L |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/AnimatedNavContent.kt` |

**Acceptance Criteria**:
- [ ] `@Composable AnimatedNavContent<T : NavNode>()` function
- [ ] Parameters: `targetState`, `transition`, `scope`, `predictiveBackEnabled`, `modifier`, `content`
- [ ] Tracks `displayedState` and `previousState` for animation direction
- [ ] Delegates to `PredictiveBackContent` when gesture active
- [ ] Uses `AnimatedContent` with proper `transitionSpec`
- [ ] Provides `AnimatedVisibilityScope` to content via `withAnimatedVisibilityScope`
- [ ] Handles back navigation detection correctly

---

### HIER-017: PredictiveBackContent Composable

| Field | Value |
|-------|-------|
| **Title** | Create PredictiveBackContent for gesture-driven transforms |
| **Description** | Composable that renders current and previous content with transform animations based on gesture progress. |
| **Dependencies** | HIER-005, HIER-006 |
| **Effort** | M |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackContent.kt` |

**Acceptance Criteria**:
- [ ] `@Composable PredictiveBackContent<T : NavNode>()` function
- [ ] Renders previous content with parallax effect (30% factor)
- [ ] Renders current content with translation and scale (10% factor)
- [ ] Uses `graphicsLayer` for performant transforms
- [ ] Integrates with `ComposableCache` for state preservation
- [ ] `StaticAnimatedVisibilityScope` helper for content rendering
- [ ] Constants: `PARALLAX_FACTOR = 0.3f`, `SCALE_FACTOR = 0.1f`

---

### HIER-018: NavTreeRenderer Core

| Field | Value |
|-------|-------|
| **Title** | Create NavTreeRenderer entry point |
| **Description** | Main recursive renderer that dispatches to node-specific renderers based on `NavNode` type. |
| **Dependencies** | HIER-001, HIER-008 |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavTreeRenderer.kt` |

**Acceptance Criteria**:
- [ ] `@Composable internal fun NavTreeRenderer()` function
- [ ] Parameters: `node: NavNode`, `previousNode: NavNode?`, `scope: NavRenderScope`, `modifier: Modifier`
- [ ] `when` dispatch to: `ScreenRenderer`, `StackRenderer`, `TabRenderer`, `PaneRenderer`
- [ ] Proper type casting for `previousNode`

---

### HIER-019: ScreenRenderer

| Field | Value |
|-------|-------|
| **Title** | Implement ScreenRenderer for leaf nodes |
| **Description** | Renders `ScreenNode` destinations using the screen registry. |
| **Dependencies** | HIER-018, HIER-006 |
| **Effort** | M |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/ScreenRenderer.kt` |

**Acceptance Criteria**:
- [ ] `@Composable internal fun ScreenRenderer()` function
- [ ] Wraps content in `CachedEntry` with screen key
- [ ] Provides `LocalBackStackEntry` CompositionLocal
- [ ] Invokes `screenRegistry.Content()` with proper parameters
- [ ] Passes `SharedTransitionScope` and `AnimatedVisibilityScope`
- [ ] Handles `toBackStackEntry()` conversion

---

### HIER-020: StackRenderer

| Field | Value |
|-------|-------|
| **Title** | Implement StackRenderer with animations |
| **Description** | Renders `StackNode` with animated transitions between children. |
| **Dependencies** | HIER-018, HIER-016, HIER-004 |
| **Effort** | M |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/StackRenderer.kt` |

**Acceptance Criteria**:
- [ ] `@Composable internal fun StackRenderer()` function
- [ ] Gets active child from `node.children.lastOrNull()`
- [ ] Detects back navigation via child comparison
- [ ] Resolves transition from `AnimationCoordinator`
- [ ] Wraps in `AnimatedNavContent` with proper parameters
- [ ] Enables predictive back only for root stack (`parentKey == null`)
- [ ] Recursively calls `NavTreeRenderer` for child

---

### HIER-021: TabRenderer

| Field | Value |
|-------|-------|
| **Title** | Implement TabRenderer with wrapper composition |
| **Description** | Renders `TabNode` with wrapper containing animated tab content. This is the critical component that fixes the wrapper/content separation issue. |
| **Dependencies** | HIER-018, HIER-016, HIER-002 |
| **Effort** | L |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/TabRenderer.kt` |

**Acceptance Criteria**:
- [ ] `@Composable internal fun TabRenderer()` function
- [ ] Gets active stack from `node.stacks[node.activeStackIndex]`
- [ ] Creates `TabWrapperScope` via `rememberTabWrapperScope()`
- [ ] Caches ENTIRE tab (wrapper + content) with single key
- [ ] Invokes wrapper from `wrapperRegistry.TabWrapper()`
- [ ] Content slot contains `AnimatedNavContent` for tab switching
- [ ] Tab transition uses directional detection (left/right)
- [ ] Predictive back disabled for tab switching
- [ ] Recursively renders active stack

---

### HIER-022: PaneRenderer

| Field | Value |
|-------|-------|
| **Title** | Implement PaneRenderer with adaptive layout |
| **Description** | Renders `PaneNode` with adaptive multi-pane or single-pane layout. |
| **Dependencies** | HIER-018, HIER-002 |
| **Effort** | L |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PaneRenderer.kt` |

**Acceptance Criteria**:
- [ ] `@Composable internal fun PaneRenderer()` function
- [ ] Calculates `windowSizeClass` for adaptive behavior
- [ ] Caches entire pane configuration
- [ ] Delegates to `MultiPaneRenderer` when expanded
- [ ] Delegates to `SinglePaneRenderer` when compact
- [ ] `MultiPaneRenderer` helper renders wrapper with multiple slots
- [ ] `SinglePaneRenderer` helper behaves like a stack

---

### HIER-023: StaticAnimatedVisibilityScope

| Field | Value |
|-------|-------|
| **Title** | Create StaticAnimatedVisibilityScope helper |
| **Description** | Fake `AnimatedVisibilityScope` for content rendered outside animations (e.g., cached content during predictive back). |
| **Dependencies** | None |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/StaticAnimatedVisibilityScope.kt` |

**Acceptance Criteria**:
- [ ] `@Composable fun StaticAnimatedVisibilityScope(content: @Composable AnimatedVisibilityScope.() -> Unit)`
- [ ] Creates minimal `AnimatedVisibilityScope` implementation
- [ ] Transition always reports "finished" state
- [ ] No-op for animation callbacks

---

## Phase 4: Integration

**Goal**: Wire everything together with feature flag support.

### HIER-024: HierarchicalQuoVadisHost

| Field | Value |
|-------|-------|
| **Title** | Create HierarchicalQuoVadisHost composable |
| **Description** | New `QuoVadisHost` variant using hierarchical rendering. Initially separate, later merged with feature flag. |
| **Dependencies** | HIER-008, HIER-018, HIER-005 |
| **Effort** | L |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/HierarchicalQuoVadisHost.kt` |

**Acceptance Criteria**:
- [ ] `@Composable fun HierarchicalQuoVadisHost()` function
- [ ] Parameters: `navigator`, `modifier`, `screenRegistry`, `wrapperRegistry`, `transitionRegistry`, `defaultTransition`
- [ ] Tracks `currentState` and `previousState`
- [ ] Creates all scope components (`cache`, `animationCoordinator`, `predictiveBackController`)
- [ ] Handles `PredictiveBackHandler` integration
- [ ] Wraps in `SharedTransitionLayout` at root
- [ ] Provides `NavRenderScopeImpl` via `CompositionLocalProvider`
- [ ] Calls `NavTreeRenderer` with root node

---

### HIER-025: RenderingMode Enum and Feature Flag

| Field | Value |
|-------|-------|
| **Title** | Add RenderingMode feature flag to QuoVadisHost |
| **Description** | Allow switching between flattened and hierarchical rendering via parameter. |
| **Dependencies** | HIER-024 |
| **Effort** | M |
| **Files to Modify** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt` |

**Acceptance Criteria**:
- [ ] `RenderingMode` enum: `FLATTENED`, `HIERARCHICAL`
- [ ] `QuoVadisHost` gains `renderingMode: RenderingMode = RenderingMode.FLATTENED` parameter
- [ ] Conditional delegation to `HierarchicalQuoVadisHost` or existing implementation
- [ ] No behavior change when using default parameter
- [ ] KDoc documenting feature flag behavior

---

### HIER-026: LocalBackStackEntry and Helpers

| Field | Value |
|-------|-------|
| **Title** | Create navigation CompositionLocals |
| **Description** | Define CompositionLocals for accessing navigation context from screen composables. |
| **Dependencies** | None |
| **Effort** | S |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavigationLocals.kt` |

**Acceptance Criteria**:
- [ ] `LocalBackStackEntry` CompositionLocal
- [ ] `LocalNavigator` CompositionLocal (if not existing)
- [ ] `LocalAnimatedVisibilityScope` CompositionLocal
- [ ] Extension functions: `ScreenNode.toBackStackEntry()`
- [ ] Proper default error messages for missing providers

---

### HIER-027: Integration Tests

| Field | Value |
|-------|-------|
| **Title** | Create integration tests for hierarchical rendering |
| **Description** | Comprehensive test suite validating the hierarchical rendering behavior. |
| **Dependencies** | HIER-024 |
| **Effort** | L |
| **Files to Create** | `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/HierarchicalRenderingTest.kt` |

**Acceptance Criteria**:
- [ ] Test: Stack navigation renders correct active screen
- [ ] Test: Tab navigation renders wrapper containing content (not siblings)
- [ ] Test: Tab switching animates correctly
- [ ] Test: Predictive back transforms entire tab subtree
- [ ] Test: Cache preserves state across navigation
- [ ] Test: Shared elements work across NavNode boundaries
- [ ] Test: Feature flag switches between renderers
- [ ] Test: Default wrappers work when no custom wrapper provided

---

### HIER-028: FakeNavRenderScope for Testing

| Field | Value |
|-------|-------|
| **Title** | Create FakeNavRenderScope for composable tests |
| **Description** | Test double for `NavRenderScope` enabling isolated composable testing. |
| **Dependencies** | HIER-001 |
| **Effort** | M |
| **Files to Create** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavRenderScope.kt` |

**Acceptance Criteria**:
- [ ] `FakeNavRenderScope` class implementing `NavRenderScope`
- [ ] Configurable `FakeNavigator` integration
- [ ] Stub implementations for all properties
- [ ] `FakeComposableCache` that always renders
- [ ] `FakePredictiveBackController` with controllable state
- [ ] Builder pattern for test configuration
- [ ] Documentation with usage examples

---

## Phase 5: Migration

**Goal**: Deprecate old APIs, provide migration tooling, clean up.

### HIER-029: Deprecate RenderableSurface

| Field | Value |
|-------|-------|
| **Title** | Deprecate RenderableSurface and related types |
| **Description** | Mark the flattening approach as deprecated with migration hints. |
| **Dependencies** | HIER-027 (all tests passing) |
| **Effort** | S |
| **Files to Modify** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/RenderableSurface.kt`, `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattener.kt` |

**Acceptance Criteria**:
- [ ] `@Deprecated` annotation on `RenderableSurface` class
- [ ] `@Deprecated` annotation on `TreeFlattener` class
- [ ] Deprecation message points to `RenderingMode.HIERARCHICAL`
- [ ] `DeprecationLevel.WARNING` (not ERROR yet)
- [ ] All usages documented in migration guide

---

### HIER-030: Deprecate Runtime Wrapper Parameters

| Field | Value |
|-------|-------|
| **Title** | Deprecate runtime wrapper lambdas in favor of annotations |
| **Description** | Mark old wrapper parameter patterns as deprecated. |
| **Dependencies** | HIER-014 |
| **Effort** | S |
| **Files to Modify** | `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/TabWrapper.kt`, `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/PaneWrapper.kt` |

**Acceptance Criteria**:
- [ ] `@Deprecated` on runtime wrapper HOF overloads
- [ ] Migration message: "Use @TabWrapper/@PaneWrapper annotations"
- [ ] Existing functionality unchanged

---

### HIER-031: Migration Guide Document

| Field | Value |
|-------|-------|
| **Title** | Create migration guide from flattened to hierarchical rendering |
| **Description** | Comprehensive documentation for migrating existing code. |
| **Dependencies** | HIER-029, HIER-030 |
| **Effort** | M |
| **Files to Create** | `docs/migration-examples/06-hierarchical-rendering.md` |

**Acceptance Criteria**:
- [ ] Overview of changes and rationale
- [ ] Step-by-step migration checklist
- [ ] Before/after code examples for:
  - Simple stack navigation
  - Tabbed navigation with wrapper
  - Pane-based navigation
  - Custom animations
- [ ] Troubleshooting section
- [ ] FAQ section

---

### HIER-032: Update Documentation Site

| Field | Value |
|-------|-------|
| **Title** | Update docs site with hierarchical rendering content |
| **Description** | Add new architecture documentation to the docs site. |
| **Dependencies** | HIER-031 |
| **Effort** | M |
| **Files to Modify** | Multiple files in `docs/site/src/` |

**Acceptance Criteria**:
- [ ] Architecture page updated with hierarchical diagram
- [ ] API reference updated with new types
- [ ] Getting started guide updated (or versioned)
- [ ] New "Advanced: Hierarchical Rendering" section
- [ ] Animation customization documentation

---

### HIER-033: Remove Deprecated Code (Future)

| Field | Value |
|-------|-------|
| **Title** | Remove deprecated flattening code |
| **Description** | Final cleanup after deprecation period. **Not part of initial release.** |
| **Dependencies** | HIER-029, minimum 2 minor version deprecation period |
| **Effort** | M |
| **Files to Delete** | `RenderableSurface.kt`, `TreeFlattener.kt`, `FlattenResult.kt` |
| **Files to Modify** | `QuoVadisHost.kt` (remove feature flag, default to hierarchical) |

**Acceptance Criteria**:
- [ ] All deprecated types removed
- [ ] `RenderingMode` enum removed (hierarchical is only option)
- [ ] No references to flattening in codebase
- [ ] Changelog documents breaking change
- [ ] Major version bump

---

## Task Dependency Graph

```
Phase 1: Core Components
========================
HIER-001 ─────┬─────────────────────────────────────┐
              │                                     │
HIER-002 ────(001)                                  │
              │                                     │
HIER-003 ─────┼──────────────────────┐              │
              │                      │              │
HIER-004 ───(003)                    │              │
              │                      │              │
HIER-005 ─────┼──────────────────────┼──────────────┤
              │                      │              │
HIER-006 ─────┼──────────────────────┼──────────────┤
              │                      │              │
HIER-007 ───(003)                    │              │
              │                      │              │
HIER-008 ─(001,002,004,005,006,007)──┴──────────────┘

Phase 2: KSP Updates
====================
HIER-009 ─────┬─────────────────────────────────────┐
HIER-010 ─────┤                                     │
HIER-011 ───(003)                                   │
              │                                     │
HIER-013 ─────┤                                     │
              │                                     │
HIER-012 ─(009,010)                                 │
              │                                     │
HIER-014 ─(002,012,013)                             │
              │                                     │
HIER-015 ─(007,011)─────────────────────────────────┘

Phase 3: Renderer Implementation
================================
HIER-023 ─────┬─────────────────────────────────────┐
              │                                     │
HIER-016 ─(003,005)                                 │
              │                                     │
HIER-017 ─(005,006)                                 │
              │                                     │
HIER-018 ─(001,008)                                 │
              │                                     │
HIER-019 ─(006,018)                                 │
              │                                     │
HIER-020 ─(004,016,018)                             │
              │                                     │
HIER-021 ─(002,016,018)                             │
              │                                     │
HIER-022 ─(002,018)─────────────────────────────────┘

Phase 4: Integration
====================
HIER-026 ─────┬─────────────────────────────────────┐
              │                                     │
HIER-024 ─(005,008,018)                             │
              │                                     │
HIER-025 ───(024)                                   │
              │                                     │
HIER-028 ───(001)                                   │
              │                                     │
HIER-027 ───(024)───────────────────────────────────┘

Phase 5: Migration
==================
HIER-029 ───(027)─┬─────────────────────────────────┐
                  │                                 │
HIER-030 ───(014)─┤                                 │
                  │                                 │
HIER-031 ─(029,030)                                 │
                  │                                 │
HIER-032 ───(031)─┤                                 │
                  │                                 │
HIER-033 ─(029)───┴─ [FUTURE: after deprecation period]
```

---

## Parallel Work Streams

Based on dependencies, the following work can proceed in parallel:

### Stream A: Core Types (Week 1)
- HIER-001, HIER-002, HIER-003, HIER-005, HIER-006, HIER-007
- HIER-023, HIER-026

### Stream B: KSP Annotations (Week 1)
- HIER-009, HIER-010, HIER-011, HIER-013

### Stream C: KSP Generators (Week 2, depends on A + B)
- HIER-012, HIER-014, HIER-015

### Stream D: Animation Infrastructure (Week 2, depends on partial A)
- HIER-004, HIER-016, HIER-017

### Stream E: Renderers (Week 3, depends on A + D)
- HIER-018, HIER-019, HIER-020, HIER-021, HIER-022

### Stream F: Integration (Week 4, depends on E)
- HIER-008, HIER-024, HIER-025, HIER-027, HIER-028

### Stream G: Migration (Week 5, depends on F passing tests)
- HIER-029, HIER-030, HIER-031, HIER-032

---

## Estimated Timeline

| Phase | Tasks | Estimated Duration |
|-------|-------|-------------------|
| Phase 1 | HIER-001 to HIER-008 | 1.5 weeks |
| Phase 2 | HIER-009 to HIER-015 | 1.5 weeks |
| Phase 3 | HIER-016 to HIER-023 | 2 weeks |
| Phase 4 | HIER-024 to HIER-028 | 1.5 weeks |
| Phase 5 | HIER-029 to HIER-032 | 1 week |

**Total**: ~7.5 weeks (with parallel execution)

---

## Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Shared element timing issues | Medium | High | HIER-016 includes extensive testing |
| Predictive back edge cases | Medium | Medium | HIER-027 covers gesture cancellation |
| KSP incremental build issues | Low | Medium | Generate to separate source set |
| Cache memory pressure | Low | Low | Configurable max size + monitoring |
| Animation performance | Medium | High | Use `graphicsLayer`, benchmark early |

---

## Success Metrics

1. **Correctness**: All existing integration tests pass with `RenderingMode.HIERARCHICAL`
2. **Performance**: No measurable regression in frame times during navigation
3. **Predictive Back**: Tab wrapper transforms as unit during gesture (visual verification)
4. **Shared Elements**: Elements animate correctly across NavNode boundaries
5. **Code Quality**: All new code passes detekt and lint checks

---

## Appendix: File Structure After Implementation

```
quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/
├── Transition.kt               # NEW (HIER-011)
├── TabAnnotations.kt           # MODIFIED (HIER-009)
└── PaneAnnotations.kt          # MODIFIED (HIER-010)

quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/
├── hierarchical/               # NEW PACKAGE
│   ├── NavRenderScope.kt       # HIER-001
│   ├── NavRenderScopeImpl.kt   # HIER-008
│   ├── WrapperRegistry.kt      # HIER-002
│   ├── NavTransition.kt        # HIER-003
│   ├── AnimationCoordinator.kt # HIER-004
│   ├── PredictiveBackController.kt # HIER-005
│   ├── TransitionRegistry.kt   # HIER-007
│   ├── AnimatedNavContent.kt   # HIER-016
│   ├── PredictiveBackContent.kt # HIER-017
│   ├── NavTreeRenderer.kt      # HIER-018
│   ├── ScreenRenderer.kt       # HIER-019
│   ├── StackRenderer.kt        # HIER-020
│   ├── TabRenderer.kt          # HIER-021
│   ├── PaneRenderer.kt         # HIER-022
│   ├── StaticAnimatedVisibilityScope.kt # HIER-023
│   ├── HierarchicalQuoVadisHost.kt # HIER-024
│   └── NavigationLocals.kt     # HIER-026
├── ComposableCache.kt          # MODIFIED (HIER-006)
├── QuoVadisHost.kt             # MODIFIED (HIER-025)
├── RenderableSurface.kt        # DEPRECATED (HIER-029)
└── TreeFlattener.kt            # DEPRECATED (HIER-029)

quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/
├── models/
│   └── WrapperInfo.kt          # NEW (HIER-013)
├── extractors/
│   └── WrapperExtractor.kt     # NEW (HIER-012)
├── generators/
│   ├── WrapperRegistryGenerator.kt # NEW (HIER-014)
│   └── TransitionRegistryGenerator.kt # NEW (HIER-015)
└── QuoVadisSymbolProcessor.kt  # MODIFIED (HIER-014, HIER-015)

quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/
└── FakeNavRenderScope.kt       # NEW (HIER-028)

quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/
└── HierarchicalRenderingTest.kt # NEW (HIER-027)

docs/
├── migration-examples/
│   └── 06-hierarchical-rendering.md # NEW (HIER-031)
└── site/src/
    └── [various updates]       # HIER-032
```
