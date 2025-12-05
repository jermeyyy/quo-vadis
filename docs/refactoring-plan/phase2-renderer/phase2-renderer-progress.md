# Phase 2: Unified Renderer - Progress

> **Last Updated**: 2025-12-05  
> **Phase Status**: 🟡 In Progress  
> **Progress**: 11/12 tasks (92%)

## Overview

This phase implements the single rendering component (`QuoVadisHost`) that projects the NavNode tree with user-controlled wrappers for TabNode and PaneNode.

---

## Task Progress

| ID | Task | Status | Completed | Notes |
|----|------|--------|-----------|-------|
| [RENDER-001](./RENDER-001-renderable-surface.md) | Define RenderableSurface Data Class | 🟢 Completed | 2025-12-05 | All types, builder, extensions |
| [RENDER-002A](./RENDER-002A-core-flatten.md) | Core flattenState Algorithm (Screen/Stack) | 🟢 Completed | 2025-12-05 | FlattenResult, TreeFlattener with Screen/Stack |
| [RENDER-002B](./RENDER-002B-tab-flattening.md) | TabNode Flattening with User Wrapper | 🟢 Completed | 2025-12-05 | TAB_WRAPPER/TAB_CONTENT surfaces, caching hints |
| [RENDER-002C](./RENDER-002C-pane-flattening.md) | PaneNode Adaptive Flattening | 🟢 Completed | 2025-12-05 | WindowSizeClass types, adaptive flattening |
| [RENDER-003](./RENDER-003-transition-state.md) | Create TransitionState Sealed Class | 🟢 Completed | 2025-12-05 | Tree-aware TransitionState with manager |
| [RENDER-004](./RENDER-004-quovadis-host.md) | Build QuoVadisHost Composable | 🟢 Completed | 2025-12-05 | Main host, 3 API variants, predictive back |
| [RENDER-005](./RENDER-005-predictive-back.md) | Integrate Predictive Back with Speculative Pop | 🟢 Completed | 2025-12-05 | Full platform support, speculative pop |
| [RENDER-006](./RENDER-006-animation-registry.md) | Create AnimationRegistry | 🟢 Completed | 2025-12-05 | AnimationRegistry, StandardAnimations |
| [RENDER-007](./RENDER-007-saveable-state.md) | SaveableStateHolder Integration | 🟢 Completed | 2025-12-05 | NavigationStateHolder wrapper, differentiated caching |
| [RENDER-008](./RENDER-008-user-wrapper-api.md) | User Wrapper API (TabNode/PaneNode) | 🟢 Completed | 2025-12-05 | TabWrapper, PaneWrapper, DefaultWrappers |
| [RENDER-009](./RENDER-009-window-size-integration.md) | WindowSizeClass Integration | 🟢 Completed | 2025-12-05 | expect/actual for all platforms |
| [RENDER-010](./RENDER-010-animation-pair-tracking.md) | Animation Pair Tracking | ⚪ Not Started | - | Depends on RENDER-003 |

---

## Completed Tasks

### RENDER-007: SaveableStateHolder Integration ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavigationStateHolder.kt`
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavigationStateHolderTest.kt`
- **Files Modified**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt` (uses NavigationStateHolder)
- **Summary**:
  - **CacheScope enum** - Differentiated caching strategies:
    - `FULL_SCREEN` - Normal screen caching for ScreenNode/StackNode
    - `WHOLE_WRAPPER` - Cache entire wrapper for TabNode/PaneNode during cross-node navigation
    - `CONTENT_ONLY` - Cache only content for intra-tab/pane navigation
  - **NavigationStateHolder class** - Wrapper for SaveableStateHolder with navigation-specific logic:
    - `SaveableScreen(key, content)` - Wraps content in SaveableStateProvider
    - `retain(key)` / `release(key)` - Marks keys as retained (survives cleanup)
    - `removeState(key)` - Removes state if not retained
    - `cleanup(activeKeys, previousKeys)` - Cleans up removed keys
    - `determineCacheScope(transition, surfaceId, surfaceMode)` - Determines appropriate caching strategy
    - `SaveableWithScope(key, scope, wrapperContent, content)` - Applies scope-based caching
  - **Factory and Extension Functions**:
    - `rememberNavigationStateHolder()` - Creates and remembers instance
    - `NavigationStateHolder.PreserveTabStates(tabNode, content)` - Retains all tab states
  - **Helper Functions**:
    - `collectAllKeys(node)` - Traverses tree to collect all keys
    - `findAllTabNodes(node)` - Finds all TabNodes in tree
    - `findAllPaneNodes(node)` - Finds all PaneNodes in tree
  - **QuoVadisHost Integration**:
    - Replaced `rememberSaveableStateHolder()` with `rememberNavigationStateHolder()`
    - Added state cleanup via `LaunchedEffect(activeKeys)` with `cleanup()`
    - Added tab state preservation via `PreserveTabStates()` for all TabNodes
    - Updated internal functions to use `NavigationStateHolder` instead of raw `SaveableStateHolder`
  - **Comprehensive Test Suite** (30 tests):
    - `collectAllKeys` tests for all node types (ScreenNode, StackNode, TabNode, PaneNode)
    - `findAllTabNodes` / `findAllPaneNodes` tests
    - `CacheScope` enum tests
    - Complex nested structure tests
    - Consistency tests between helpers
  - Full KDoc documentation on all public APIs
- **Verified**:
  - `:composeApp:assembleDebug` ✓
  - `:quo-vadis-core:desktopTest` ✓

### RENDER-006: Create AnimationRegistry ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/AnimationRegistry.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/StandardAnimations.kt`
- **Files Modified**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt` (added `animationRegistry` parameter)
- **Summary**:
  - **AnimationRegistry class** - Centralized registry for navigation transition animations:
    - Lookup by (from, to, transitionType) with priority: exact → wildcard target → wildcard source → wildcards → defaults → global
    - `resolve()` method for animation lookup
    - `toAnimationResolver()` for TreeFlattener integration
    - `copy()` for extending existing registries
    - `plus` operator for combining registries
    - `Builder` class with `register()`, `registerDefault()`, convenience methods (`useSlideForward()`, `useSlideBackward()`, etc.)
    - `AnimationRegistry.Default` with standard slide/fade animations
    - `AnimationRegistry.None` for no animations
  - **StandardAnimations object** - Built-in animations:
    - `slideForward()` - slide in from right, push left out
    - `slideBackward()` - slide in from left, push right out  
    - `slideVertical()` - slide up to enter, slide down to exit
    - `fade()` - simple crossfade animation
    - `scale()` - zoom in/out animation
    - `sharedAxis()` - Material Design shared axis (X, Y, Z)
    - `containerTransform()` - Material container transform placeholder
    - `SharedAxis` enum (X, Y, Z)
  - **Animation combinators**:
    - `SurfaceAnimationSpec.plus()` operator for combining animations
    - `SurfaceAnimationSpec.reversed()` for backward animations
  - **DSL extensions**:
    - `animationRegistry { }` builder function
    - `forwardTransition<From, To>()` - PUSH transitions
    - `backwardTransition<From, To>()` - POP transitions
    - `biDirectionalTransition<From, To>()` - both directions
    - `tabSwitchTransition()`, `paneSwitchTransition()`
  - **QuoVadisHost integration**:
    - Added `animationRegistry: AnimationRegistry = AnimationRegistry.Default` parameter to all 3 overloads
    - Uses `animationRegistry.toAnimationResolver()` when creating TreeFlattener
  - Full KDoc documentation on all public APIs
- **Verified**:
  - `:quo-vadis-core:compileKotlinMetadata` ✓
  - `:quo-vadis-core:desktopTest` ✓
  - `:composeApp:assembleDebug` ✓
  - No errors ✓

### RENDER-004: Build QuoVadisHost Composable ✅
- **Completed**: 2025-12-05
- **File Created**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt`
- **Summary**:
  - **QuoVadisHostScope interface** - Extends SharedTransitionScope with Navigator access
  - **Main QuoVadisHost Composable** (~750 lines):
    - Observes Navigator state via `collectAsState()`
    - Calculates WindowSizeClass via `calculateWindowSizeClass()`
    - Flattens NavNode tree via TreeFlattener
    - Renders surfaces with proper z-ordering
    - Uses SaveableStateHolder for tab/pane state preservation
    - Wraps in SharedTransitionLayout for shared element transitions
    - Supports tabWrapper and paneWrapper parameters
  - **Internal rendering components**:
    - `QuoVadisHostContent` - Renders flattened surfaces sorted by z-order
    - `RenderableSurfaceContainer` - Single surface with AnimatedVisibility
    - `RenderSurfaceContent` - Dispatches to correct renderer based on renderingMode
    - `RenderTabWrapper` - TabNode rendering with user's tabWrapper
    - `RenderPaneWrapper` - PaneNode rendering with user's paneWrapper
  - **Helper functions**:
    - `findScreenNodeByKey()` - DFS for ScreenNode
    - `findTabNodeByKey()` - DFS for TabNode
    - `findPaneNodeByKey()` - DFS for PaneNode
    - `predictiveBackTransform` modifier - Scale/translate during gestures
  - **Alternative APIs**:
    - Content map variant: `QuoVadisHost(navigator, contentMap = mapOf(...))`
    - Graph-based variant: `QuoVadisHost(navigator, graph = navigationGraph)`
  - **Predictive back support** via `predictiveBackTransform` modifier
  - Full KDoc documentation on all public APIs
- **Verified**:
  - `:quo-vadis-core:compileCommonMainKotlinMetadata` ✓
  - `:quo-vadis-core:compileKotlinDesktop` ✓
  - `:quo-vadis-core:compileKotlinJs` ✓
  - `:quo-vadis-core:compileKotlinWasmJs` ✓
  - `:composeApp:assembleDebug` ✓

### RENDER-001: Define RenderableSurface Data Class ✅
- **Completed**: 2025-12-05
- **File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/RenderableSurface.kt`
- **Summary**: Created all types (SurfaceNodeType, SurfaceRenderingMode, SurfaceTransitionState, SurfaceAnimationSpec, PaneStructure, RenderableSurface), builder pattern, and list extension functions
- **Verified**: Compiles on Kotlin Metadata, Desktop (JVM), and JS targets

### RENDER-002A: Core flattenState Algorithm (Screen/Stack) ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/FlattenResult.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattener.kt`
- **Summary**: 
  - Created `TransitionType` enum (PUSH, POP, TAB_SWITCH, PANE_SWITCH, NONE)
  - Created `AnimationPair` data class for transition coordination
  - Created `CachingHints` data class for renderer caching optimization
  - Created `FlattenResult` data class with computed properties (renderableSurfaces, sortedSurfaces, isEmpty, etc.)
  - Created `TreeFlattener` class with:
    - `ContentResolver` interface for resolving NavNode to composable content
    - `AnimationResolver` interface for custom animations
    - `flattenState()` main entry point
    - `flattenScreen()` for ScreenNode → SINGLE_SCREEN surface
    - `flattenStack()` for StackNode → STACK_CONTENT surfaces with animation pairing
    - Placeholder `flattenTab()` and `flattenPane()` for RENDER-002B/C
    - Helper methods: detectTransitionType, findPreviousSiblingId, getActivePath, getActiveLeaf
    - DefaultAnimationResolver with slide animations
  - Full KDoc documentation on all public APIs
- **Verified**: Build passes on all targets (`:composeApp:assembleDebug`)

### RENDER-002B: TabNode Flattening with User Wrapper Support ✅
- **Completed**: 2025-12-05
- **Files Modified**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/FlattenResult.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattener.kt`
- **Files Created**:
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattenerTabTest.kt`
- **Summary**:
  - Extended `CachingHints` with new properties:
    - `wrapperIds: Set<String>` - IDs of wrapper surfaces (TAB_WRAPPER, PANE_WRAPPER)
    - `contentIds: Set<String>` - IDs of content surfaces (TAB_CONTENT, PANE_CONTENT)
    - `isCrossNodeTypeNavigation: Boolean` - Flag for cross-node type navigation
  - Updated `FlattenAccumulator` with tracking fields:
    - `wrapperIds`, `contentIds`, `isCrossNodeNavigation`
    - Updated `toResult()` to populate new CachingHints fields
  - Implemented full `flattenTab()` method:
    - Creates TAB_WRAPPER surface for user's wrapper composable
    - Creates TAB_CONTENT surface for active tab's content
    - Links content to wrapper via `parentWrapperId`
    - Detects tab switches by comparing with previousRoot
    - Generates AnimationPair for TAB_SWITCH transitions
    - Implements dual caching strategy:
      - Cross-node navigation: cache whole wrapper + content
      - Intra-tab navigation: cache only content (not wrapper)
    - Recursively flattens active stack's content
  - Added helper methods:
    - `detectPreviousTabIndex()` - Compares with previous root to detect tab switches
    - `findTabNodeByKey()` - Depth-first search for TabNode in tree
    - `detectCrossNodeNavigation()` - Determines if cross-node navigation
    - `flattenStackContent()` - Flattens nested stack content within tabs
  - Created comprehensive test suite with 20+ tests covering:
    - Wrapper and content surface creation
    - Parent-child linking via parentWrapperId
    - Tab switch animation pairing
    - AnimationPair generation for TAB_SWITCH
    - Caching strategy (intra-tab vs cross-node)
    - Nested content flattening
    - Edge cases (single tab, multiple tabs, empty stacks)
  - Full KDoc documentation on all public changes
- **Verified**: 
  - Build passes: `:quo-vadis-core:build`, `:composeApp:assembleDebug` ✓
  - Tests pass: `:quo-vadis-core:desktopTest` ✓

### RENDER-002C: PaneNode Adaptive Flattening ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.kt`
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattenerPaneTest.kt`
- **Files Modified**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TreeFlattener.kt`
- **Summary**:
  - Created WindowSizeClass types (data structures only, no expect/actual):
    - `WindowWidthSizeClass` enum: Compact (< 600dp), Medium (600-840dp), Expanded (> 840dp)
    - `WindowHeightSizeClass` enum: Compact (< 480dp), Medium (480-900dp), Expanded (> 900dp)
    - `WindowSizeClass` data class with companion factory methods (calculateFromSize)
    - Helper properties: isCompactWidth, isAtLeastMediumWidth, isExpandedWidth
  - Extended `FlattenContext` with `windowSizeClass` parameter
  - Extended `flattenState()` to accept optional `windowSizeClass` parameter
  - Implemented full `flattenPane()` method with adaptive behavior:
    - Routes to `flattenPaneAsStack()` for Compact width
    - Routes to `flattenPaneMultiPane()` for Medium/Expanded width
  - Implemented `flattenPaneAsStack()`:
    - Produces PANE_AS_STACK surface for single pane (stack-like behavior)
    - Tracks previousSurfaceId for back navigation animations
    - Generates PANE_SWITCH AnimationPair for pane switches
    - Stack-like caching behavior
  - Implemented `flattenPaneMultiPane()`:
    - Produces PANE_WRAPPER surface with paneStructures list
    - Produces PANE_CONTENT surfaces for each pane
    - Links content surfaces via parentWrapperId
    - Tab-like caching strategy (wrapper vs content)
    - Cross-node navigation detection and animation pairs
  - Added helper methods:
    - `flattenPaneContent()` - Recursively flattens nested content
    - `detectPreviousPaneRole()` - Detects pane switches
    - `findPaneNodeByKey()` - DFS for PaneNode in tree
    - `detectCrossNodePaneNavigation()` - Cross-node detection
  - Created comprehensive test suite with 30+ tests covering:
    - Compact width → PANE_AS_STACK surface
    - Compact width → previousSurfaceId for back navigation
    - Expanded width → PANE_WRAPPER + PANE_CONTENT surfaces
    - paneStructures populated with PaneRole + content
    - parentWrapperId linking
    - PANE_SWITCH animation pair generation
    - Caching hints for both modes
    - Medium width also produces multi-pane output
    - Cross-node navigation handling
    - Window size class boundary tests
  - Full KDoc documentation on all public APIs
- **Verified**: 
  - Build passes: `:quo-vadis-core:build -x detekt` ✓
  - Tests pass: `:quo-vadis-core:desktopTest` ✓

### RENDER-003: Create TransitionState Sealed Class ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TransitionState.kt` (completely redesigned)
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/LegacyTransitionState.kt`
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TransitionStateTest.kt`
- **Files Modified**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/Navigator.kt` (uses LegacyTransitionState)
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigator.kt` (uses LegacyTransitionState)
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabScopedNavigator.kt` (uses LegacyTransitionState)
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/testing/FakeNavigator.kt` (uses LegacyTransitionState)
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/core/TreeNavigatorTest.kt` (uses LegacyTransitionState)
- **Summary**:
  - **New TransitionState** - Complete tree-aware redesign:
    - `TransitionDirection` enum: FORWARD, BACKWARD, NONE
    - `TransitionState` sealed class with:
      - `Idle(current: NavNode)` - holds current navigation tree
      - `Proposed(current: NavNode, proposed: NavNode, progress: Float)` - predictive back gestures
      - `Animating(current: NavNode, target: NavNode, progress: Float, direction: TransitionDirection)` - animations
    - All states are `@Serializable` for state restoration
    - Abstract properties: `direction`, `current`, `target` (nullable), `effectiveTarget`
    - Computed properties: `isIdle`, `isProposed`, `isAnimating`, `progress`
    - Query methods:
      - `affectsStack(stackKey)` - identifies stacks with changed children
      - `affectsTab(tabKey)` - identifies tab switches
      - `previousChildOf(stackKey)` - gets screen being covered/revealed
      - `previousTabIndex(tabKey)` - gets previous tab during switch
      - `animationComposablePair()` - returns composable pair for animation
      - `isIntraTabNavigation(tabKey)` - checks if navigation is within same tab
      - `isIntraPaneNavigation(paneKey)` - checks if navigation is within same pane
      - `isCrossNodeTypeNavigation()` - checks if navigation crosses node types
    - Progress methods:
      - `Proposed.withProgress(newProgress)` - returns updated copy with clamped progress
      - `Animating.withProgress(newProgress)` - returns updated copy with clamped progress
      - `Animating.complete()` - returns `Idle(target)`
    - Private helpers for tree diffing: `findChangedStacks()`, `hasTabIndexChanged()`, `findLastChild()`, `findSecondToLastChild()`, `findPreviousTabIndex()`
  - **TransitionStateManager** - State machine manager:
    - `_state: MutableStateFlow<TransitionState>` internal
    - `state: StateFlow<TransitionState>` public
    - `currentState: TransitionState` getter
    - `startAnimation(target, direction)` - Idle → Animating
    - `startProposed(proposed)` - Idle → Proposed
    - `updateProgress(progress)` - updates current state progress
    - `commitProposed()` - Proposed → Animating(BACKWARD)
    - `cancelProposed()` - Proposed → Idle
    - `completeAnimation()` - Animating → Idle(target)
    - `forceIdle(state)` - force set to Idle
    - Throws `IllegalStateException` for invalid transitions
  - **Backward compatibility**:
    - Created `LegacyTransitionState.kt` with old API (Idle, InProgress, PredictiveBack, Seeking)
    - Updated Navigator interface to use `LegacyTransitionState`
    - Updated all navigator implementations (TreeNavigator, TabScopedNavigator, FakeNavigator)
    - Migrated TreeNavigatorTest to use LegacyTransitionState
    - Old code continues to work until migrated to new tree-based architecture
  - **Comprehensive test suite** (55+ tests):
    - Idle state tests: holds NavNode, has no progress, direction is NONE
    - Proposed state tests: tracks gesture progress, progress clamping (0-1)
    - Animating state tests: holds current/target, direction tracking, complete() returns Idle
    - Query method tests: affectsStack, affectsTab, previousChildOf, previousTabIndex
    - TransitionStateManager tests: valid transitions, invalid transition exceptions, state machine semantics
    - Nested structure tests: tab → stack → screen hierarchies
    - Backward compatibility extension tests: isAnimating, progress
  - Full KDoc documentation on all public APIs
- **Verified**: 
  - Build passes: `:composeApp:assembleDebug` ✓
  - Tests pass: `test` ✓ (518 tests, including 55 new TransitionState tests)

---

## In Progress Tasks

_None currently in progress._

---

## Blocked Tasks

_None currently blocked._

---

## Ready to Start

- **RENDER-010**: Animation Pair Tracking (RENDER-003 complete)

---

## Completed This Session

### RENDER-004: Build QuoVadisHost Composable ✅
- **Completed**: 2025-12-05
- **File Created**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt`
- **Summary**: Main QuoVadisHost composable with SharedTransitionLayout, TreeFlattener integration, SaveableStateHolder, TabWrapper/PaneWrapper support, three API variants (lambda, contentMap, graph), predictive back transforms
- **Verified**: All platform targets compile successfully, `:composeApp:assembleDebug` ✓

### RENDER-009: WindowSizeClass Integration ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.android.kt`
  - `quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.ios.kt`
  - `quo-vadis-core/src/desktopMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.desktop.kt`
  - `quo-vadis-core/src/jsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.js.kt`
  - `quo-vadis-core/src/wasmJsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.wasmJs.kt`
- **Files Modified**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.kt` (added expect fun)
  - `quo-vadis-core/build.gradle.kts` (added M3 window-size-class dependency for Android)
  - `gradle/libs.versions.toml` (added M3 window-size-class version)
- **Summary**:
  - Added `expect fun calculateWindowSizeClass(): WindowSizeClass` to commonMain
  - **Android implementation**: Uses Material3 `calculateWindowSizeClass()` API, maps M3 enums to our enums
  - **iOS implementation**: Uses UIScreen.mainScreen.bounds with orientation change observer via NSNotificationCenter
  - **Desktop implementation**: Uses LocalWindowInfo.current.containerSize with LocalDensity conversion
  - **JS/Browser implementation**: Uses window.innerWidth/innerHeight with resize event listener
  - **WasmJS implementation**: Same as JS implementation
  - All implementations automatically recompose on window size changes
  - Full KDoc documentation on all implementations
- **Verified**:
  - `:quo-vadis-core:compileKotlinMetadata` ✓
  - `:quo-vadis-core:compileAndroidMain` ✓
  - `:quo-vadis-core:compileKotlinIosArm64` ✓
  - `:quo-vadis-core:compileKotlinDesktop` ✓
  - `:quo-vadis-core:compileKotlinJs` ✓
  - `:quo-vadis-core:compileKotlinWasmJs` ✓

### RENDER-008: User Wrapper API (TabNode/PaneNode) ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/TabWrapperScope.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/TabWrapper.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/PaneWrapperScope.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/PaneWrapper.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/DefaultWrappers.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/wrapper/internal/ScopeImpls.kt`
- **Summary**:
  - **TabWrapperScope interface** - Tab navigation scope:
    - Properties: `navigator`, `activeTabIndex`, `tabCount`, `tabMetadata`, `isTransitioning`
    - Methods: `switchTab(index)`, `switchTab(route)`
  - **TabMetadata data class** - Tab info: `label`, `icon`, `route`, `contentDescription`, `badge`
  - **TabWrapper typealias** - `@Composable TabWrapperScope.(tabContent: @Composable () -> Unit) -> Unit`
  - **PaneWrapperScope interface** - Pane navigation scope:
    - Properties: `navigator`, `activePaneRole`, `paneCount`, `visiblePaneCount`, `isExpanded`, `isTransitioning`
    - Methods: `navigateToPane(role)`
    - Uses existing `PaneRole` from NavNode.kt (Primary, Supporting, Extra)
  - **PaneContent data class** - Pane info: `role`, `content`, `isVisible`
  - **PaneWrapper typealias** - `@Composable PaneWrapperScope.(paneContents: List<PaneContent>) -> Unit`
  - **Default implementations**:
    - `DefaultTabWrapper` - Material 3 Scaffold with bottom NavigationBar
    - `TopTabWrapper` - Column with TabRow at top
    - `DefaultPaneWrapper` - Row with equal weight panes and VerticalDivider
    - `WeightedPaneWrapper` - Row with role-based weights (Primary: 65%, Supporting: 35%, Extra: 25%)
  - **Internal implementations**:
    - `TabWrapperScopeImpl` - Validates index bounds, routes by route string
    - `PaneWrapperScopeImpl` - Simple delegation to callbacks
    - Factory functions: `createTabWrapperScope()`, `createPaneWrapperScope()`
  - **Design Decision**: Reused existing `PaneRole` enum (Primary, Supporting, Extra) instead of spec's LIST, DETAIL, SUPPLEMENTARY values to avoid breaking changes
  - Full KDoc documentation on all public APIs
- **Verified**:
  - Build passes: `:quo-vadis-core:compileKotlinMetadata` ✓
  - Build passes: `:quo-vadis-core:compileKotlinDesktop` ✓
  - No errors in created files ✓

### RENDER-005: Integrate Predictive Back with Speculative Pop ✅
- **Completed**: 2025-12-05
- **Files Created**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PredictiveBackIntegration.kt` - Core infrastructure
  - `quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PredictiveBackHandler.android.kt` - Android implementation
  - `quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PredictiveBackHandler.ios.kt` - iOS implementation
  - `quo-vadis-core/src/desktopMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PredictiveBackHandler.desktop.kt` - Desktop stub
  - `quo-vadis-core/src/jsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PredictiveBackHandler.js.kt` - JS stub
  - `quo-vadis-core/src/wasmJsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PredictiveBackHandler.wasmJs.kt` - WasmJS stub
- **Files Modified**:
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt` - Added `enablePredictiveBack` parameter
- **Summary**:
  - **PredictiveBackCallback interface** - Gesture event callbacks (onBackStarted, onBackProgress, onBackCancelled, onBackCommitted)
  - **PredictiveBackCoordinator class** - Manages speculative pop via TreeMutator.pop() and TransitionStateManager
  - **expect/actual PredictiveBackHandler composable**:
    - **Android**: Uses AndroidX `PredictiveBackHandler` API with Flow<BackEventCompat>
    - **iOS**: Custom edge swipe gesture recognizer (20dp edge threshold, 100dp complete threshold)
    - **Desktop/Web**: No-op stubs (back handled via keyboard or browser)
  - **QuoVadisHost Integration**:
    - Added `enablePredictiveBack: Boolean = true` parameter to all 3 overloads
    - Creates TransitionStateManager and PredictiveBackCoordinator
    - Wraps content with PredictiveBackHandler (enabled when canGoBack)
  - Full KDoc documentation on all public APIs
- **Verified**:
  - `:composeApp:assembleDebug` ✓
  - `test` ✓
  - All platform targets compile successfully

---

## Dependencies

```
Phase 1 ─► RENDER-001 ─► RENDER-002A ─┬─► RENDER-002B ✓
                │                      ├─► RENDER-002C ✓
                │                      ├─► RENDER-008 ✓
                │                      └─► RENDER-009 ✓
                │
                └─► RENDER-003 ✓ ─► RENDER-010
                         │
                         ▼
                    RENDER-004 ✓ ─┬─► RENDER-005 ✓
                                  ├─► RENDER-006
                                  └─► RENDER-007
```

---

## Notes

- This is the largest phase with 12 tasks
- Estimated 31-37.5 days total
- Key architecture: User Wrapper API for TabNode/PaneNode customization
- WindowSizeClass integration for adaptive layouts
- RENDER-002C provides WindowSizeClass data types; RENDER-009 will add platform-specific `calculateWindowSizeClass()` implementations

---

## Related Documents

- [Phase 2 Summary](./phase2-renderer-summary.md)
