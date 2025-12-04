# Phase 2: Unified Renderer - Summary

## Phase Overview

Phase 2 builds the **Single Rendering Component** (`QuoVadisHost`) that replaces all existing navigation hosts with a unified renderer capable of handling arbitrary NavNode tree structures. This phase transforms the recursive NavNode tree from Phase 1 into a flat, ordered list of renderable surfaces with proper animations, state preservation, and platform-adaptive layouts.

### Key Objectives

1. **Define RenderableSurface IR** - Intermediate representation for the rendering pipeline
2. **Implement TreeFlattener** - Convert NavNode tree to ordered surface list
3. **Build QuoVadisHost** - Single composable that renders any navigation structure
4. **Integrate Predictive Back** - Gesture-driven navigation with speculative pop
5. **Support Adaptive Layouts** - WindowSizeClass-based PaneNode rendering
6. **Enable User Customization** - Wrapper APIs for TabNode and PaneNode

### Architecture Position

```
Navigator (StateFlow<NavNode>) ────► QuoVadisHost ────► Compose UI
         │                                │
         │ TransitionState                │ SharedTransitionScope
         ▼                                │
    TreeFlattener ────► List<RenderableSurface>
                                          │
                        AnimatedContent ◄─┘
                        (per surface)
```

### Design Principles

- **Single Source of Truth**: QuoVadisHost observes Navigator's StateFlow
- **Composition over Inheritance**: Small composables compose into the host
- **Differentiated Caching**: Different navigation contexts use different caching strategies
- **User Control**: Wrapper APIs let users define TabNode/PaneNode presentation

---

## Task Summaries

### RENDER-001: Define RenderableSurface Data Class

| Property | Value |
|----------|-------|
| **Complexity** | Low |
| **Estimated Time** | 1.5 days |
| **Dependencies** | None |
| **Blocks** | RENDER-002A, RENDER-002B, RENDER-002C, RENDER-004, RENDER-008 |

**Summary**: Defines the intermediate representation (IR) between the NavNode tree and Compose UI rendering. Each `RenderableSurface` represents a single "layer" in the flattened rendering stack.

**Key Components**:
- `SurfaceNodeType` enum: SCREEN, STACK, TAB, PANE
- `SurfaceRenderingMode` enum: SINGLE_SCREEN, STACK_CONTENT, TAB_WRAPPER, TAB_CONTENT, PANE_WRAPPER, PANE_CONTENT, PANE_AS_STACK
- `SurfaceTransitionState` sealed interface: Visible, Entering, Exiting, Hidden
- `SurfaceAnimationSpec` data class: enter/exit transitions
- `PaneStructure` data class: paneRole and content for multi-pane layouts
- `RenderableSurface` data class: id, zOrder, nodeType, renderingMode, transitionState, content, parentWrapperId, previousSurfaceId, paneStructures

**Key Features**:
- `parentWrapperId` - Links content surfaces to their parent wrapper for caching
- `previousSurfaceId` - Enables animation pairing and predictive back
- Builder pattern and DSL extensions for fluent surface creation

---

### RENDER-002A: Core flattenState Algorithm (Screen/Stack)

| Property | Value |
|----------|-------|
| **Complexity** | High |
| **Estimated Time** | 2 days |
| **Dependencies** | CORE-001, RENDER-001 |
| **Blocks** | RENDER-002B, RENDER-002C, RENDER-004 |

**Summary**: Implements the core flattening algorithm for `ScreenNode` and `StackNode`, defining the base `TreeFlattener` class and fundamental output structures.

**Key Components**:
- `TransitionType` enum: PUSH, POP, TAB_SWITCH, PANE_SWITCH, NONE
- `AnimationPair` data class: currentId, previousId, transitionType
- `CachingHints` data class: shouldCacheWrapper, shouldCacheContent, cacheableIds, invalidatedIds
- `FlattenResult` data class: surfaces, animationPairs, cachingHints
- `TreeFlattener` class with `flattenState()` entry point
- `ContentResolver` interface for resolving node content
- `AnimationResolver` interface for resolving animations

**Flattening Behavior**:
- ScreenNode → Single surface with `SINGLE_SCREEN` mode
- StackNode → Top child surface with `STACK_CONTENT` mode, `previousSurfaceId` tracking

---

### RENDER-002B: TabNode Flattening with User Wrapper Support

| Property | Value |
|----------|-------|
| **Complexity** | High |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002A, RENDER-008 |
| **Blocks** | RENDER-004 |

**Summary**: Extends TreeFlattener to handle `TabNode` with user-provided wrapper composables. Users control the wrapper (scaffold, tabs, bottom nav), library provides slot content.

**Key Features**:
- Dual surface output: `TAB_WRAPPER` + `TAB_CONTENT`
- `parentWrapperId` links content to wrapper
- `previousSurfaceId` tracks previous tab for animations
- Differentiated caching:
  - Cross-node navigation → Cache WHOLE wrapper
  - Intra-tab navigation → Cache ONLY content (not wrapper)

**Extended CachingHints**:
- `wrapperIds: Set<String>` - IDs of wrapper surfaces
- `contentIds: Set<String>` - IDs of content surfaces
- `isCrossNodeTypeNavigation: Boolean` - Navigation context flag

---

### RENDER-002C: PaneNode Adaptive Flattening

| Property | Value |
|----------|-------|
| **Complexity** | High |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002A, RENDER-009 |
| **Blocks** | RENDER-004 |

**Summary**: Implements adaptive PaneNode rendering based on `WindowSizeClass`. Small screens render as StackNode (single pane), large screens show all panes with user wrapper.

**Rendering Modes by WindowSizeClass**:

| Width Class | Output | Rendering Mode |
|-------------|--------|----------------|
| **Compact** | Single surface | `PANE_AS_STACK` |
| **Medium** | Wrapper + 2 content surfaces | `PANE_WRAPPER` + `PANE_CONTENT` |
| **Expanded** | Wrapper + all content surfaces | `PANE_WRAPPER` + `PANE_CONTENT` |

**Key Features**:
- `paneStructures` field provides pane layout info to user wrapper
- Same caching strategy as TabNode
- `PaneRole` enum: LIST, DETAIL, SUPPORTING, EXTRA

---

### RENDER-003: Create TransitionState Sealed Class

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | CORE-001 |
| **Blocks** | RENDER-002A, RENDER-002B, RENDER-002C, RENDER-004, RENDER-005 |

**Summary**: Manages navigation transitions including static states, proposed states (during predictive back gestures), and animating states.

**State Machine**:
```
┌─────────┐  navigate()   ┌───────────┐  animation   ┌─────────┐
│  Idle   │──────────────►│ Animating │──────────────►│  Idle   │
└─────────┘               └───────────┘  complete     └─────────┘
     ▲                         ▲
     │                         │ commit
     │    ┌───────────┐        │
     └────│ Proposed  │────────┘
  cancel  └───────────┘
```

**Key Components**:
- `TransitionState` sealed class: Idle, Proposed, Animating
- `TransitionDirection` enum: FORWARD, BACKWARD, NONE
- `TransitionStateManager` class for StateFlow management
- Query methods: `affectsStack()`, `affectsTab()`, `previousChildOf()`, `previousTabIndex()`
- Helper methods: `animationComposablePair()`, `isIntraTabNavigation()`, `isIntraPaneNavigation()`, `isCrossNodeTypeNavigation()`

---

### RENDER-004: Build QuoVadisHost Composable

| Property | Value |
|----------|-------|
| **Complexity** | High |
| **Estimated Time** | 6-8 days |
| **Dependencies** | CORE-001, RENDER-001, RENDER-002A, RENDER-002B, RENDER-002C, RENDER-003, RENDER-008, RENDER-009 |
| **Blocks** | RENDER-005, RENDER-007 |

**Summary**: The central rendering component that replaces all existing navigation hosts (`NavHost`, `GraphNavHost`, `TabbedNavHost`) with a unified renderer.

**Key Features**:
- Observes `Navigator.stateFlow` and `transitionStateFlow`
- Flattens NavNode tree via TreeFlattener
- Renders surfaces with z-ordering via `zIndex` modifier
- `SharedTransitionLayout` for shared element transitions
- `SaveableStateHolder` for tab state preservation
- `AnimatedVisibility` for enter/exit transitions
- Predictive back gesture transforms

**User Wrapper Parameters**:
- `tabWrapper: @Composable (TabNode, activeTabContent: @Composable () -> Unit) -> Unit`
- `paneWrapper: @Composable (PaneNode, paneContents: List<PaneContent>) -> Unit`

**Supporting Types**:
- `QuoVadisHostScope` interface: sharedTransitionScope, animatedContentScope, navigator
- `PaneContent` data class: paneType, content

**Alternative APIs**:
- Content map variant for compile-time destination mapping
- Graph-based variant using KSP-generated NavigationGraph

---

### RENDER-005: Integrate Predictive Back with Speculative Pop

| Property | Value |
|----------|-------|
| **Complexity** | High |
| **Estimated Time** | 4-5 days |
| **Dependencies** | RENDER-004 |
| **Blocks** | None |

**Summary**: Implements predictive back navigation where users can preview the result of a back action during the gesture before committing.

**Speculative Pop Algorithm**:
1. Gesture start → Compute pop result (speculative state)
2. Both states rendered with progress-based animation
3. Gesture commit → Animation completes to speculative result
4. Gesture cancel → Animation reverses to original

**Platform Support**:

| Platform | Gesture Type | API |
|----------|-------------|-----|
| Android 14+ | System back gesture | `PredictiveBackHandler` |
| Android <14 | Back button only | Immediate pop |
| iOS | Edge swipe gesture | Custom gesture recognizer |
| Desktop | Escape/back button | Immediate pop |
| Web | Browser back | `popstate` event |

**Key Components**:
- `PredictiveBackCallback` interface: onBackStarted, onBackProgress, onBackCancelled, onBackCommitted
- `PredictiveBackCoordinator` class: manages speculative pop and TransitionState updates
- `PredictiveBackHandler` expect/actual composable for platform implementations
- Visual effects: scale (1.0 → 0.9), parallax translation, opacity, corner radius

---

### RENDER-006: Create AnimationRegistry

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | RENDER-001 |
| **Blocks** | None |

**Summary**: Centralized management of navigation transition animations, registered by source/target destination class and direction.

**Lookup Priority**:
1. Exact match: `(HomeScreen::class, ProfileScreen::class, FORWARD)`
2. Wildcard target: `(HomeScreen::class, *, FORWARD)`
3. Wildcard source: `(*, ProfileScreen::class, FORWARD)`
4. Direction default: `(*, *, FORWARD)`
5. Global default: `(*, *, *)`

**Key Components**:
- `AnimationRegistry` class with Builder pattern
- `StandardAnimations` object: slideForward, slideBackward, slideVertical, fade, scale, sharedAxis
- Animation combinators: `plus`, `reversed()`, `withDelay()`
- DSL extensions: `forwardTransition<F, T>()`, `biDirectionalTransition<F, T>()`
- `AnimationRegistry.Default` and `AnimationRegistry.None` presets

---

### RENDER-007: SaveableStateHolder Integration

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 3-4 days |
| **Dependencies** | RENDER-004 |
| **Blocks** | None |

**Summary**: Integrates `SaveableStateHolder` for preserving state across composition lifecycle changes with differentiated caching strategies.

**CacheScope Types**:
- `FULL_SCREEN` - Normal screen caching
- `WHOLE_WRAPPER` - Cache entire TabNode/PaneNode wrapper during cross-node navigation
- `CONTENT_ONLY` - Cache only content during intra-tab/pane navigation

**Key Components**:
- `NavigationStateHolder` wrapper class with `determineCacheScope()`
- `SaveableWithScope()` composable for scope-aware caching
- `retain()` and `release()` methods for key retention
- Tab-specific state preservation via `PreserveTabStates()`

**State Isolation**:

| State Type | Survives Tab Switch | Survives Intra-Tab Nav | Survives Process Death |
|------------|---------------------|------------------------|------------------------|
| Scaffold state | ✅ | ✅ | ✅ (with rememberSaveable) |
| Tab scroll position | ✅ | ✅ | ✅ (with rememberSaveable) |

---

### RENDER-008: User Wrapper API for TabNode and PaneNode

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | RENDER-001, RENDER-002B, RENDER-002C |
| **Blocks** | RENDER-004 |

**Summary**: Defines the API contract for user-provided wrapper composables. Users control the wrapper structure (scaffold, app bar, tabs), while the library provides content slots.

**Tab Wrapper API**:
- `TabWrapperScope` interface: navigator, activeTabIndex, tabCount, tabMetadata, isTransitioning, switchTab()
- `TabMetadata` data class: label, icon, route, contentDescription, badge
- `TabWrapper` typealias: `@Composable TabWrapperScope.(tabContent: @Composable () -> Unit) -> Unit`

**Pane Wrapper API**:
- `PaneWrapperScope` interface: navigator, activePaneRole, paneCount, visiblePaneCount, isExpanded, isTransitioning, navigateToPane()
- `PaneRole` enum: LIST, DETAIL, SUPPLEMENTARY
- `PaneContent` data class: role, content, isVisible
- `PaneWrapper` typealias: `@Composable PaneWrapperScope.(paneContents: List<PaneContent>) -> Unit`

**Default Implementations**:
- `DefaultTabWrapper` - Material 3 bottom navigation
- `TopTabWrapper` - Tab row variant
- `DefaultPaneWrapper` - Equal-weight side-by-side
- `WeightedPaneWrapper` - Role-based weight distribution

---

### RENDER-009: WindowSizeClass Integration for PaneNode

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002C |
| **Blocks** | RENDER-004 |

**Summary**: Implements window size observation and classification for adaptive PaneNode rendering across all platforms.

**WindowSizeClass Breakpoints**:

| Class | Width | Height | Typical Device |
|-------|-------|--------|----------------|
| Compact | < 600dp | < 480dp | Phones |
| Medium | 600-840dp | 480-900dp | Tablets, foldables |
| Expanded | > 840dp | > 900dp | Desktops |

**Platform Implementations**:
- **Android**: Material3 `calculateWindowSizeClass()`
- **iOS**: UIScreen bounds with orientation observer
- **Desktop**: `LocalWindowInfo` with `LocalDensity`
- **Web (JS/Wasm)**: `window.innerWidth/innerHeight` with resize listener

**PaneNode Behavior by Width Class**:

| Width Class | PaneNode Rendering |
|-------------|-------------------|
| Compact | As StackNode (single pane) |
| Medium | Multi-pane (2 visible) |
| Expanded | Multi-pane (all visible) |

---

### RENDER-010: Animation Pair Tracking for Transitions

| Property | Value |
|----------|-------|
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002A, RENDER-003 |
| **Blocks** | RENDER-005 |

**Summary**: Explicitly tracks current/previous screen pairs for coordinated animations, shared element transitions, and predictive back gestures.

**Key Components**:
- `AnimationPair` data class: currentId, previousId, transitionType, currentSurface, previousSurface, containerId
- `AnimationPairTracker` class: maintains state history and produces animation pairs
- Extended `FlattenResult` with `animationPairs` property and helper methods

**AnimationPair Properties**:
- `hasBothSurfaces` - True if both entering and exiting surfaces exist
- `shouldAnimate` - True if transition type is not NONE and has both surfaces
- `isStackTransition` - True for PUSH or POP
- `supportsSharedElements` - True for stack transitions with both surfaces

---

## Key Components/Features to Implement

### Data Structures

```
RenderableSurface
├── id: String
├── zOrder: Int
├── nodeType: SurfaceNodeType
├── renderingMode: SurfaceRenderingMode
├── transitionState: SurfaceTransitionState
├── animationSpec: SurfaceAnimationSpec
├── content: @Composable () -> Unit
├── parentWrapperId: String?
├── previousSurfaceId: String?
└── paneStructures: List<PaneStructure>?

FlattenResult
├── surfaces: List<RenderableSurface>
├── animationPairs: List<AnimationPair>
└── cachingHints: CachingHints

TransitionState (sealed)
├── Idle(current: NavNode)
├── Proposed(current, proposed, progress)
└── Animating(current, target, progress, direction)

WindowSizeClass
├── widthSizeClass: WindowWidthSizeClass
└── heightSizeClass: WindowHeightSizeClass
```

### Core Classes

| Class | Purpose |
|-------|---------|
| `TreeFlattener` | Converts NavNode tree to List<RenderableSurface> |
| `QuoVadisHost` | Single rendering component for all navigation |
| `TransitionStateManager` | Manages TransitionState as StateFlow |
| `AnimationRegistry` | Centralized animation configuration |
| `NavigationStateHolder` | SaveableStateHolder with differentiated caching |
| `AnimationPairTracker` | Tracks animation pairs for transitions |
| `PredictiveBackCoordinator` | Manages speculative pop for predictive back |

### Composables

| Composable | Purpose |
|------------|---------|
| `QuoVadisHost` | Main navigation host |
| `PredictiveBackHandler` | Platform-specific back gesture handling |
| `RenderableSurfaceContainer` | Individual surface rendering with animations |

---

## Dependencies

### Internal Dependencies (Task Order)

```
                                   RENDER-001 (RenderableSurface)
                                           │
                ┌──────────────────────────┼──────────────────────────┐
                │                          │                          │
                ▼                          ▼                          ▼
         RENDER-006               RENDER-003                  RENDER-008
         (AnimationRegistry)      (TransitionState)           (User Wrapper API)
                                           │
                                           ▼
                                   RENDER-002A (Core Flatten)
                                           │
                ┌──────────────────────────┼──────────────────────────┐
                │                                                     │
                ▼                                                     ▼
         RENDER-002B                                           RENDER-002C
         (Tab Flattening)                                      (Pane Flattening)
                │                                                     │
                │                                                     ▼
                │                                              RENDER-009
                │                                              (WindowSizeClass)
                │                                                     │
                └──────────────────────────┬──────────────────────────┘
                                           │
                                   RENDER-010 (Animation Pair Tracking)
                                           │
                                           ▼
                                   RENDER-004 (QuoVadisHost)
                                           │
                ┌──────────────────────────┼──────────────────────────┐
                │                                                     │
                ▼                                                     ▼
         RENDER-005                                            RENDER-007
         (Predictive Back)                                     (SaveableState)
```

### Phase 1 Dependencies

All Phase 2 tasks depend on Phase 1 completion:

| Phase 1 Task | Phase 2 Dependency |
|--------------|-------------------|
| CORE-001 (NavNode) | All RENDER-* tasks |
| CORE-002 (TreeMutator) | RENDER-005 (speculative pop) |
| CORE-003 (Navigator) | RENDER-004 (state observation) |

### External Dependencies

| Dependency | Purpose |
|------------|---------|
| `androidx.compose.animation` | EnterTransition, ExitTransition, AnimatedContent |
| `androidx.compose.material3` | Material 3 wrapper defaults |
| `material3-window-size-class` (Android) | WindowSizeClass calculation |
| `kotlinx.coroutines` | StateFlow for reactive updates |

---

## File References

### Files to Create

| File Path | Task |
|-----------|------|
| `quo-vadis-core/.../compose/RenderableSurface.kt` | RENDER-001 |
| `quo-vadis-core/.../compose/FlattenResult.kt` | RENDER-002A |
| `quo-vadis-core/.../compose/TreeFlattener.kt` | RENDER-002A, 002B, 002C |
| `quo-vadis-core/.../core/TransitionState.kt` | RENDER-003 |
| `quo-vadis-core/.../compose/QuoVadisHost.kt` | RENDER-004 |
| `quo-vadis-core/.../compose/PredictiveBackIntegration.kt` | RENDER-005 |
| `quo-vadis-core/src/androidMain/.../AndroidPredictiveBack.kt` | RENDER-005 |
| `quo-vadis-core/src/iosMain/.../IosPredictiveBack.kt` | RENDER-005 |
| `quo-vadis-core/.../compose/AnimationRegistry.kt` | RENDER-006 |
| `quo-vadis-core/.../compose/StandardAnimations.kt` | RENDER-006 |
| `quo-vadis-core/.../compose/NavigationStateHolder.kt` | RENDER-007 |
| `quo-vadis-core/.../compose/wrapper/TabWrapperScope.kt` | RENDER-008 |
| `quo-vadis-core/.../compose/wrapper/TabWrapper.kt` | RENDER-008 |
| `quo-vadis-core/.../compose/wrapper/PaneWrapperScope.kt` | RENDER-008 |
| `quo-vadis-core/.../compose/wrapper/PaneWrapper.kt` | RENDER-008 |
| `quo-vadis-core/.../compose/wrapper/DefaultWrappers.kt` | RENDER-008 |
| `quo-vadis-core/.../compose/WindowSizeClass.kt` | RENDER-009 |
| `quo-vadis-core/src/androidMain/.../WindowSizeClass.android.kt` | RENDER-009 |
| `quo-vadis-core/src/iosMain/.../WindowSizeClass.ios.kt` | RENDER-009 |
| `quo-vadis-core/src/desktopMain/.../WindowSizeClass.desktop.kt` | RENDER-009 |
| `quo-vadis-core/src/jsMain/.../WindowSizeClass.js.kt` | RENDER-009 |
| `quo-vadis-core/src/wasmJsMain/.../WindowSizeClass.wasmJs.kt` | RENDER-009 |
| `quo-vadis-core/.../compose/AnimationPairTracker.kt` | RENDER-010 |

### Files to Modify

| File Path | Task | Changes |
|-----------|------|---------|
| `quo-vadis-core/build.gradle.kts` | RENDER-009 | Add Material3 window-size-class dependency |

---

## Estimated Effort

| Task | Complexity | Time Estimate |
|------|------------|---------------|
| RENDER-001 | Low | 1.5 days |
| RENDER-002A | High | 2 days |
| RENDER-002B | High | 2 days |
| RENDER-002C | High | 2 days |
| RENDER-003 | Medium | 2 days |
| RENDER-004 | High | 6-8 days |
| RENDER-005 | High | 4-5 days |
| RENDER-006 | Medium | 2-3 days |
| RENDER-007 | Medium | 3-4 days |
| RENDER-008 | Medium | 2-3 days |
| RENDER-009 | Medium | 2 days |
| RENDER-010 | Medium | 2 days |
| **Total** | | **31-38 days** |

### Risk Factors

- QuoVadisHost complexity with multiple rendering modes
- Cross-platform predictive back implementation consistency
- Performance of tree flattening for deep navigation trees
- WindowSizeClass edge cases on foldables and dynamic window resizing
- Animation coordination between entering/exiting surfaces

---

## Acceptance Criteria Summary

### RENDER-001 (RenderableSurface)
- [ ] All surface types and rendering modes defined
- [ ] `SurfaceTransitionState` sealed interface complete
- [ ] `parentWrapperId` and `previousSurfaceId` for caching/animation
- [ ] Builder pattern and extension functions implemented

### RENDER-002A (Core Flatten)
- [ ] `FlattenResult`, `AnimationPair`, `CachingHints` defined
- [ ] ScreenNode → `SINGLE_SCREEN` surface
- [ ] StackNode → `STACK_CONTENT` surface with `previousSurfaceId`
- [ ] AnimationPair populated for stack transitions

### RENDER-002B (Tab Flattening)
- [ ] `TAB_WRAPPER` + `TAB_CONTENT` dual surface output
- [ ] Cross-node caches wrapper, intra-tab caches only content
- [ ] AnimationPairs for tab switches

### RENDER-002C (Pane Flattening)
- [ ] Compact width → `PANE_AS_STACK`
- [ ] Medium/Expanded → `PANE_WRAPPER` + `PANE_CONTENT`
- [ ] `paneStructures` populated with roles and content

### RENDER-003 (TransitionState)
- [ ] Idle, Proposed, Animating states complete
- [ ] Query methods for navigation context detection
- [ ] TransitionStateManager with proper state machine

### RENDER-004 (QuoVadisHost)
- [ ] Renders NavNode tree correctly
- [ ] `tabWrapper` and `paneWrapper` parameters functional
- [ ] SharedTransitionLayout for shared elements
- [ ] Differentiated caching based on navigation type

### RENDER-005 (Predictive Back)
- [ ] Speculative pop computed correctly
- [ ] Platform implementations for Android, iOS, Desktop, Web
- [ ] Visual transforms match Material/iOS guidelines
- [ ] Gesture cancel/commit work correctly

### RENDER-006 (AnimationRegistry)
- [ ] Lookup priority works correctly
- [ ] Standard animations implemented
- [ ] Builder DSL functional
- [ ] Integration with TreeFlattener

### RENDER-007 (SaveableState)
- [ ] Tab state preserved across switches
- [ ] Differentiated caching scopes working
- [ ] State cleanup on screen removal

### RENDER-008 (User Wrapper API)
- [ ] `TabWrapperScope` and `PaneWrapperScope` interfaces complete
- [ ] Default wrapper implementations functional
- [ ] Integration with TabNode/PaneNode

### RENDER-009 (WindowSizeClass)
- [ ] All platform implementations working
- [ ] Breakpoints match Material Design specs
- [ ] PaneNode adapts correctly to size changes

### RENDER-010 (Animation Pair Tracking)
- [ ] AnimationPairTracker correctly identifies transitions
- [ ] FlattenResult includes animation pairs
- [ ] Shared element support for stack transitions

---

## References

- [Phase 1 Summary](../phase1-core/phase1-core-summary.md)
- [Phase Index](../INDEX.md)
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md)
- [Material Design Window Size Classes](https://m3.material.io/foundations/layout/applying-layout/window-size-classes)
- [Android Predictive Back](https://developer.android.com/guide/navigation/predictive-back-gesture)
- [Material Motion Guidelines](https://m3.material.io/styles/motion/overview)
