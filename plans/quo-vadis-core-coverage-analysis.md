# Quo-Vadis-Core Test Coverage Analysis

## 1. Current Kover Configuration

### Version
- **Kover**: `0.9.7` (defined in [gradle/libs.versions.toml](gradle/libs.versions.toml#L34))
- **Plugin alias**: `org.jetbrains.kotlinx.kover`

### Plugin Application
- Applied in root [build.gradle.kts](build.gradle.kts#L14)
- Applied in [quo-vadis-core/build.gradle.kts](quo-vadis-core/build.gradle.kts#L9)
- Applied in `quo-vadis-ksp` and `quo-vadis-core-flow-mvi`

### Merged Reporting (Root build.gradle.kts)

```kotlin
dependencies {
    kover(projects.quoVadisCore)
    kover(projects.quoVadisKsp)
    kover(projects.quoVadisCoreFlowMvi)
}
```

### Report Configuration

- **XML report**: `build/reports/kover/report.xml` (not on-check)
- **HTML report**: `build/reports/kover/html/` (not on-check)
- **Excludes**: `*.generated.*`, `*.BuildConfig`, `*.Companion`, `*Test*`, `*Fake*`, `@Composable` annotated
- **Verification rules**: Commented out (`minBound(50)` prepared but disabled)

### Key Observation
No per-module Kover configuration beyond the plugin application. All reporting/filtering is centralized in root. The 50% threshold is prepared but not enforced.

---

## 2. Complete Source File Inventory (quo-vadis-core/src/commonMain)

### navigation/node/ — NavNode Types

| File | Key Symbols | Kind |
|------|-------------|------|
| `NavNode.kt` | `NavNode` (sealed interface) + 20 extension functions (`findByKey`, `activePathToLeaf`, `activeLeaf`, `activeNodePath`, `activeStack`, `allScreens`, `collectScreens`, `paneForRole`, `allPaneNodes`, `collectPaneNodes`, `allTabNodes`, `collectTabNodes`, `allStackNodes`, `collectStackNodes`, `fold`, `forEachNode`, `depth`, `nodeCount`, `canHandleBackInternally`) | Interface + Extensions |
| `StackNode.kt` | `StackNode` | Data class |
| `ScreenNode.kt` | `ScreenNode` | Data class |
| `TabNode.kt` | `TabNode` | Class |
| `PaneNode.kt` | `PaneNode` | Class |
| `NodeKey.kt` | `NodeKey` | Class |
| `ScopeKey.kt` | `ScopeKey` | Class |
| `LifecycleDelegate.kt` | `LifecycleDelegate` | Class |
| `NavNodeTypeChecks.kt` | `isScreen`, `isStack`, `isTab`, `isPane`, `requireScreen`, `requireStack`, `requireTab`, `requirePane` | Extension functions |

### navigation/navigator/ — Navigator API

| File | Key Symbols | Kind |
|------|-------------|------|
| `Navigator.kt` | `Navigator` interface (15 members: `state`, `currentDestination`, `previousDestination`, `canNavigateBack`, `config`, `navigate`, `navigateBack`, `navigateAndClearTo`, `navigateAndReplace`, `navigateAndClearAll`, `handleDeepLink` ×2, `getDeepLinkRegistry`, `updateState`) | Interface |
| `PaneNavigator.kt` | `PaneNavigator` interface, `asPaneNavigator` extension | Interface |
| `LifecycleAwareNode.kt` | `LifecycleAwareNode` | Interface |
| `BackPressHandler.kt` | `BackPressHandler` | Interface |
| `NavigationErrorHandler.kt` | `NavigationErrorHandler` | Interface |

### navigation/internal/tree/ — TreeMutator & TreeNavigator (Core Logic)

| File | Key Symbols | Kind |
|------|-------------|------|
| `TreeMutator.kt` | `TreeMutator` object (27 methods: `push` ×2, `pushToStack`, `pushAll`, `clearAndPush`, `clearStackAndPush`, `replaceCurrent`, `pop`, `popTo`, `popToRoute`, `popToDestination`, `switchTab`, `switchActiveTab`, `navigateToPane`, `switchActivePane`, `popPane`, `popWithPaneBehavior`, `popPaneAdaptive`, `setPaneConfiguration`, `removePaneConfiguration`, `replaceNode`, `removeNode`, `canGoBack`, `currentDestination`, `popWithTabBehavior`, `canHandleBackNavigation`) | Object |
| `TreeNavigator.kt` | `TreeNavigator` class (949 lines, many methods), `findFirst`, `findFirstOfType` extensions | Class |
| `TreeDiffCalculator.kt` | `TreeDiff` data class, `TreeDiffCalculator` object | Object |
| `LifecycleNotifier.kt` | `LifecycleNotifier` | Class |
| `ScreenKeyCollector.kt` | `ScreenKeyCollector` | Class |
| `TransitionManager.kt` | `TransitionManager` | Class |

### navigation/internal/tree/operations/ — Tree Operations

| File | Key Symbols | Kind |
|------|-------------|------|
| `PushOperations.kt` | `PushOperations` object | Object |
| `PopOperations.kt` | `PopOperations` object | Object |
| `BackOperations.kt` | `BackOperations` object | Object |
| `TabOperations.kt` | `TabOperations` object | Object |
| `PaneOperations.kt` | `PaneOperations` object | Object |
| `TreeNodeOperations.kt` | `TreeNodeOperations` object | Object |

### navigation/internal/tree/result/ — Operation Results

| File | Key Symbols | Kind |
|------|-------------|------|
| `PushStrategy.kt` | `PushStrategy` | Interface/Sealed |
| `PopResult.kt` | `PopResult` | Interface/Sealed |
| `BackResult.kt` | `BackResult` | Interface/Sealed |
| `TreeOperationResult.kt` | `TreeOperationResult`, `getOrElse`, `getOrNull` | Interface + Extensions |

### navigation/internal/tree/util/ & config/

| File | Key Symbols | Kind |
|------|-------------|------|
| `KeyGenerator.kt` | `KeyGenerator` | Interface |
| `PopBehavior.kt` | `PopBehavior` | Enum |

### navigation/internal/ — Other Internal

| File | Key Symbols | Kind |
|------|-------------|------|
| `NavigationResultManager.kt` | `NavigationResultManager` | Class |
| `TransitionController.kt` | `TransitionController` | Interface |
| `ResultCapable.kt` | `ResultCapable` | Interface |
| `NavKeyGenerator.kt` | `NavKeyGenerator` | Object |
| `GeneratedTabMetadata.kt` | `GeneratedTabMetadata` | Data class |
| `config/EmptyNavigationConfig.kt` | `EmptyNavigationConfig` | Object |
| `config/CompositeNavigationConfig.kt` | `CompositeNavigationConfig` | Class |

### navigation/destination/ — Destination Types

| File | Key Symbols | Kind |
|------|-------------|------|
| `NavDestination.kt` | `NavDestination` interface, `route` property | Interface |
| `DeepLink.kt` | `DeepLink` | Data class |
| `DeepLinkResult.kt` | `DeepLinkResult` | Class |

### navigation/config/ — Public Config

| File | Key Symbols | Kind |
|------|-------------|------|
| `NavigationConfig.kt` | `NavigationConfig` | Interface |
| `CompositeNavigationConfig.kt` | `CompositeNavigationConfig` | Class |

### navigation/transition/ — Transitions

| File | Key Symbols | Kind |
|------|-------------|------|
| `NavigationTransition.kt` | `NavigationTransition` interface, `NavigationTransitions` object, `TransitionBuilder`, `customTransition`, `SharedElementConfig`, `SharedElementType`, `sharedElement`, `sharedBounds` | Mixed |
| `TransitionState.kt` | `TransitionState` interface, `progress` property | Interface |

### navigation/result/ — Navigation Results

| File | Key Symbols | Kind |
|------|-------------|------|
| `ReturnsResult.kt` | `ReturnsResult` | Interface |
| `NavigatorResultExtensions.kt` | `navigateForResult`, `navigateBackWithResult` | Extension functions |

### navigation/pane/ — Pane Configuration

| File | Key Symbols | Kind |
|------|-------------|------|
| `PaneConfiguration.kt` | `PaneConfiguration` | Data class |
| `PaneBackBehavior.kt` | `PaneBackBehavior` | Enum |
| `AdaptStrategy.kt` | `AdaptStrategy` | Enum |
| `PaneRole.kt` | `PaneRole` | Enum |

### navigation/ — Top-level

| File | Key Symbols | Kind |
|------|-------------|------|
| `TypeAliases.kt` | `NavKeyGenerator`, `OnDestroyCallback`, `NavTransitionProvider` type aliases | Type aliases |
| `GeneratedTabMetadata.kt` | `GeneratedTabMetadata` | Data class |
| `NavigationResultManager.kt` | `NavigationResultManager` | Class (public API) |

### registry/ — Registry Interfaces & Implementations

| File | Key Symbols | Kind |
|------|-------------|------|
| `ScreenRegistry.kt` | `ScreenRegistry` | Interface |
| `ContainerRegistry.kt` | `ContainerInfo`, `ContainerRegistry` | Interface |
| `DeepLinkRegistry.kt` | `DeepLinkRegistry` | Interface |
| `ModalRegistry.kt` | `ModalRegistry` | Interface |
| `ScopeRegistry.kt` | `ScopeRegistry` | Interface |
| `RouteRegistry.kt` | `RouteRegistry` | Object |
| `TransitionRegistry.kt` | `TransitionRegistry` | Interface |
| `PaneRoleRegistry.kt` | `PaneRoleRegistry` | Interface |
| `BackHandlerRegistry.kt` | `BackHandlerRegistry` class, `LocalBackHandlerRegistry` | Class |
| `internal/CompositeScreenRegistry.kt` | `CompositeScreenRegistry` | Class |
| `internal/CompositeContainerRegistry.kt` | `CompositeContainerRegistry` | Class |
| `internal/RuntimeDeepLinkRegistry.kt` | `RuntimeDeepLinkRegistry` | Class |
| `internal/CompositeDeepLinkRegistry.kt` | `CompositeDeepLinkRegistry` | Class |
| `internal/CompositeTransitionRegistry.kt` | `CompositeTransitionRegistry` | Class |
| `internal/CompositeModalRegistry.kt` | `CompositeModalRegistry` | Class |
| `internal/CompositeScopeRegistry.kt` | `CompositeScopeRegistry` | Class |

### dsl/ — DSL Navigation Configuration

| File | Key Symbols | Kind |
|------|-------------|------|
| `NavigationConfigBuilder.kt` | `NavigationConfigBuilder`, `navigationConfig`, `ScopeBuilder` | Class |
| `NavigationConfigDsl.kt` | `NavigationConfigDsl` | Annotation/Class |
| `StackBuilder.kt` | `StackBuilder`, `StackScreenEntry` | Class |
| `TabsBuilder.kt` | `TabsBuilder`, `TabEntry` | Class |
| `PanesBuilder.kt` | `PanesBuilder`, `PaneEntry`, `PaneContentBuilder` | Class |
| `ContainerBuilder.kt` | `ContainerBuilder` | Class |
| `DslNavigationConfig.kt` | `DslNavigationConfig` (public) | Class |
| `DslScreenRegistry.kt` | `DslScreenRegistry` (public) | Class |
| `DslContainerRegistry.kt` | `DslContainerRegistry` (public) | Class |
| `DslTransitionRegistry.kt` | `DslTransitionRegistry` (public) | Class |
| `DslModalRegistry.kt` | `DslModalRegistry` (public) | Class |
| `DslScopeRegistry.kt` | `DslScopeRegistry` (public) | Class |
| `internal/DslNavigationConfig.kt` | `DslNavigationConfig` (internal) | Class |
| `internal/DslScreenRegistry.kt` | `DslScreenRegistry` (internal) | Class |
| `internal/DslContainerRegistry.kt` | `DslContainerRegistry` (internal) | Class |
| `internal/DslTransitionRegistry.kt` | `DslTransitionRegistry` (internal) | Class |
| `internal/DslModalRegistry.kt` | `DslModalRegistry` (internal) | Class |
| `internal/DslScopeRegistry.kt` | `DslScopeRegistry` (internal) | Class |
| `internal/BuilderDataClasses.kt` | `ScreenEntry`, `BuiltPaneContent`, `BuiltTabsConfig`, `BuiltPanesConfig` | Data classes |

### compose/ — Compose UI Layer

| File | Key Symbols | Kind |
|------|-------------|------|
| `NavigationHost.kt` | `LocalNavRenderScope`, `NavigationHost` ×2, `NavRenderScopeImpl`, `EmptyScreenRegistry` | Composable + Class |
| `NavBackHandler.kt` | `NavBackHandler` | Composable |
| `platformDefaultPredictiveBack.kt` | (expect/actual) | Function |
| `scope/NavRenderScope.kt` | `NavRenderScope` | Interface |
| `scope/CompositionLocals.kt` | `LocalScreenNode`, `LocalAnimatedVisibilityScope`, `LocalNavigator`, `LocalContainerNode` | CompositionLocals |
| `scope/PaneContainerScope.kt` | `PaneContent`, `PaneContainerScope`, `PaneContainerScopeImpl`, `createPaneContainerScope` | Interface + Impl |
| `scope/TabsContainerScope.kt` | `TabsContainerScope`, `TabsContainerScopeImpl`, `createTabsContainerScope` | Interface + Impl |
| `transition/NavTransition.kt` | `NavTransition` | Data class |
| `transition/TransitionScope.kt` | `TransitionScope`, `TransitionScopeImpl`, `LocalTransitionScope`, `rememberTransitionScope` | Interface + Composable |
| `util/WindowSizeClass.kt` | `WindowWidthSizeClass`, `WindowHeightSizeClass`, `WindowSizeClass`, `calculateWindowSizeClass` | Enum + Data class |
| `internal/BackAnimationController.kt` | `BackAnimationController` | Class |
| `internal/PredictiveBackController.kt` | `GESTURE_MAX_PROGRESS`, `PredictiveBackController` | Class |
| `internal/ComposableCache.kt` | `ComposableCache`, `rememberComposableCache` | Class |
| `internal/AnimationCoordinator.kt` | `AnimationCoordinator` | Class |
| `internal/navback/NavigateBackHandler.kt` | `NavigateBackHandler` ×2 | Composable |
| `internal/navback/CascadeBackState.kt` | (CascadeBackState) | Class |
| `internal/navback/PlatformBackInput.kt` | `RegisterPlatformBackInput` | Composable |
| `internal/navback/ScreenNavigationInfo.kt` | (ScreenNavigationInfo) | Data class |
| `internal/navback/BackNavigationEvent.kt` | (BackNavigationEvent) | Class |
| `internal/render/NavTreeRenderer.kt` | (NavTreeRenderer) | Composable |
| `internal/render/StackRenderer.kt` | (StackRenderer) | Composable |
| `internal/render/TabRenderer.kt` | (TabRenderer) | Composable |
| `internal/render/PaneRenderer.kt` | (PaneRenderer) | Composable |
| `internal/render/ScreenRenderer.kt` | (ScreenRenderer) | Composable |
| `internal/render/AnimatedNavContent.kt` | (AnimatedNavContent) | Composable |
| `internal/render/PredictiveBackContent.kt` | (PredictiveBackContent) | Composable |
| `internal/render/ModalContent.kt` | `ModalContent`, `isNodeModal`, `findNonModalBaseIndex` | Composable + Functions |
| `internal/render/StaticAnimatedVisibilityScope.kt` | `StaticAnimatedVisibilityScope`, `rememberStaticAnimatedVisibilityScope`, `StaticAnimatedVisibilityScopeImpl` | Composable + Class |

### Platform Source Sets

| Source Set | Files |
|-----------|-------|
| `androidMain` | `WindowSizeClass.android.kt`, `PlatformBackInput.android.kt` |
| `desktopMain` | `WindowSizeClass.desktop.kt`, `PlatformBackInput.desktop.kt` |
| `iosMain` | `WindowSizeClass.ios.kt`, `PlatformBackInput.ios.kt` |
| `appleMain` | (empty directory) |

### Other

| File | Key Symbols | Kind |
|------|-------------|------|
| `InternalQuoVadisApi.kt` | `InternalQuoVadisApi` | Annotation |

---

## 3. Complete Test File Inventory

### commonTest (34 files)

#### Core TreeMutator Tests (8 files)
| Test File | Tests For |
|-----------|-----------|
| `core/TreeMutatorPushTest.kt` | `TreeMutator.push`, `pushToStack`, `pushAll`, `clearAndPush`, `clearStackAndPush`, `replaceCurrent` |
| `core/TreeMutatorPopTest.kt` | `TreeMutator.pop`, `popTo`, `popToRoute`, `popToDestination` |
| `core/TreeMutatorTabTest.kt` | `TreeMutator.switchTab`, `switchActiveTab`, tab operations |
| `core/TreeMutatorPaneTest.kt` | `TreeMutator.navigateToPane`, `switchActivePane`, `popPane`, `popWithPaneBehavior`, `popPaneAdaptive`, `setPaneConfiguration`, `removePaneConfiguration` |
| `core/TreeMutatorScopeTest.kt` | Scope-aware push/navigation (pushing to specific scoped stacks) |
| `core/TreeMutatorStackScopeTest.kt` | Stack scope-specific push behavior with nested stacks |
| `core/TreeMutatorEdgeCasesTest.kt` | Edge cases: `replaceNode`, `removeNode`, boundary conditions |
| `core/TreeMutatorBackHandlingTest.kt` | `TreeMutator.canGoBack`, `canHandleBackNavigation`, `popWithTabBehavior`, back handling logic |

#### Core Navigator & Node Tests (3 files)
| Test File | Tests For |
|-----------|-----------|
| `core/TreeNavigatorTest.kt` | `TreeNavigator` integration (navigate, navigateBack, state management) |
| `core/NavNodeTest.kt` | `NavNode` extension functions (findByKey, activeLeaf, depth, nodeCount, etc.) |
| `core/NavNodeTypeChecksTest.kt` | `isScreen`, `isStack`, `isTab`, `isPane`, `requireScreen`, `requireStack`, etc. |

#### Registry & Deep Link Tests (3 files)
| Test File | Tests For |
|-----------|-----------|
| `core/DeepLinkRegistryTest.kt` | `RuntimeDeepLinkRegistry`, `CompositeDeepLinkRegistry` |
| `core/DeepLinkTest.kt` | `DeepLink` data class (route matching, parameter extraction) |
| `core/ScopeRegistryTest.kt` | `ScopeRegistry` (scope resolution, key management) |

#### Composite Registry Tests (2 files)
| Test File | Tests For |
|-----------|-----------|
| `CompositeScreenRegistryTest.kt` | `CompositeScreenRegistry` (multi-module screen resolution) |
| `CompositeContainerRegistryTest.kt` | `CompositeContainerRegistry` (multi-module container resolution) |

#### Compose/Render Tests (8 files)
| Test File | Tests For |
|-----------|-----------|
| `compose/hierarchical/StackRendererTest.kt` | `StackRenderer` composable logic |
| `compose/hierarchical/PaneRendererTest.kt` | `PaneRenderer` composable logic |
| `compose/hierarchical/TabRendererTest.kt` | `TabRenderer` composable logic |
| `compose/hierarchical/NavTreeRendererTest.kt` | `NavTreeRenderer` composable logic |
| `compose/hierarchical/ScreenRendererTest.kt` | `ScreenRenderer` composable logic |
| `compose/hierarchical/AnimatedNavContentTest.kt` | `AnimatedNavContent` composable logic |
| `compose/hierarchical/PredictiveBackContentTest.kt` | `PredictiveBackContent` composable logic |
| `compose/hierarchical/BackAnimationControllerTest.kt` | `BackAnimationController` |

#### Navigation Back Tests (2 files)
| Test File | Tests For |
|-----------|-----------|
| `compose/navback/ScreenNavigationInfoTest.kt` | `ScreenNavigationInfo` data class |
| `compose/navback/BackNavigationEventTest.kt` | `BackNavigationEvent` |

#### Gesture/Back Tests (1 file)
| Test File | Tests For |
|-----------|-----------|
| `compose/gesture/CascadeBackStateTest.kt` | `CascadeBackState` |

#### Container & Registry Tests (2 files)
| Test File | Tests For |
|-----------|-----------|
| `compose/registry/ContainerRegistryTest.kt` | `ContainerRegistry` (tab/pane container building) |
| `compose/BackHandlerRegistryTest.kt` | `BackHandlerRegistry` |

#### Internal Navigator Tests (1 file)
| Test File | Tests For |
|-----------|-----------|
| `internal/tree/TreeNavigatorBackHandlerTest.kt` | `TreeNavigator` back handler registration/handling |

#### DSL Tests (1 file)
| Test File | Tests For |
|-----------|-----------|
| `DslNavigationConfigCrossModuleTest.kt` | Cross-module DSL navigation config |

#### Test Infrastructure (4 files — not test classes)
| File | Purpose |
|------|---------|
| `FakeNavigator.kt` | `FakeNavigator` class + `NavigationCall` — mock Navigator for testing |
| `FakeNavRenderScope.kt` | `FakeNavRenderScope`, `FakeSaveableStateHolder`, `EmptyScreenRegistry` — mock compose scope |
| `testing/NavigatorTestHelpers.kt` | `withDestination`, `withState`, `singleScreen` — helper extensions |
| `testing/NavigationTestDsl.kt` | `NavigationTestDsl`, `NavigationTestScope`, `navigationTest` — DSL for structured tests |
| `testing/FakeNavRenderScopeTest.kt` | Tests for the `FakeNavRenderScope` itself |

### desktopTest (1 file)
| Test File | Tests For |
|-----------|-----------|
| `ModalRegistryTest.kt` | `ModalRegistry` / `CompositeModalRegistry` |

---

## 4. Coverage Gap Analysis

### ✅ Well-Covered Areas

| Area | Source | Tests | Coverage Quality |
|------|--------|-------|-----------------|
| **TreeMutator** | `TreeMutator.kt` + 6 operation files | 8 test files (Push, Pop, Tab, Pane, Scope, StackScope, EdgeCases, BackHandling) | **Excellent** — all 27 methods covered across multiple test files |
| **NavNode extensions** | `NavNode.kt` (20 extensions) | `NavNodeTest.kt` | **Good** |
| **NavNode type checks** | `NavNodeTypeChecks.kt` | `NavNodeTypeChecksTest.kt` | **Good** |
| **TreeNavigator** | `TreeNavigator.kt` | `TreeNavigatorTest.kt` + `TreeNavigatorBackHandlerTest.kt` | **Good** |
| **Deep links** | `DeepLink.kt`, `DeepLinkResult.kt`, registry | `DeepLinkTest.kt`, `DeepLinkRegistryTest.kt` | **Good** |
| **Scope registry** | `ScopeRegistry.kt`, `CompositeScopeRegistry.kt` | `ScopeRegistryTest.kt` | **Good** |
| **Container registry** | `ContainerRegistry.kt`, `CompositeContainerRegistry.kt` | `CompositeContainerRegistryTest.kt`, `ContainerRegistryTest.kt` | **Good** |
| **Screen registry** | `CompositeScreenRegistry.kt` | `CompositeScreenRegistryTest.kt` | **Good** |
| **Modal registry** | `CompositeModalRegistry.kt` | `ModalRegistryTest.kt` (desktopTest) | **Good** |
| **Back handler** | `BackHandlerRegistry.kt` | `BackHandlerRegistryTest.kt` | **Good** |
| **Compose renderers** | 7 renderer files | 7 matching test files | **Good** (though likely limited — Compose tests are constrained) |
| **Back navigation** | `CascadeBackState`, `ScreenNavigationInfo`, `BackNavigationEvent`, `BackAnimationController` | 4 matching test files | **Good** |
| **DSL cross-module** | DSL config | `DslNavigationConfigCrossModuleTest.kt` | **Partial** |

### ❌ Missing or Insufficient Test Coverage

#### High-Impact Gaps (Pure Logic — Easy to Test)

| Source File | Symbols | Why Important | Difficulty |
|-------------|---------|---------------|------------|
| **`operations/PushOperations.kt`** | `PushOperations` object | Core push logic delegated from TreeMutator — tested indirectly but no unit tests | **Easy** |
| **`operations/PopOperations.kt`** | `PopOperations` object | Core pop logic — tested indirectly | **Easy** |
| **`operations/BackOperations.kt`** | `BackOperations` object | Core back logic — tested indirectly | **Easy** |
| **`operations/TabOperations.kt`** | `TabOperations` object | Tab switching logic — tested indirectly | **Easy** |
| **`operations/PaneOperations.kt`** | `PaneOperations` object | Pane navigation logic — tested indirectly | **Easy** |
| **`operations/TreeNodeOperations.kt`** | `TreeNodeOperations` object | Node replace/remove — tested indirectly by EdgeCases test | **Easy** |
| **`TreeDiffCalculator.kt`** | `TreeDiff`, `TreeDiffCalculator` | Tree diffing for lifecycle — no tests found | **Easy** |
| **`ScreenKeyCollector.kt`** | `ScreenKeyCollector` | Collects screen keys for lifecycle — no tests found | **Easy** |
| **`LifecycleNotifier.kt`** | `LifecycleNotifier` | Lifecycle notifications on tree changes — no tests found | **Medium** |
| **`TransitionManager.kt`** | `TransitionManager` | Manages transitions between states — no tests found | **Medium** |
| **`result/TreeOperationResult.kt`** | `TreeOperationResult`, `getOrElse`, `getOrNull` | Result wrapper — no unit tests | **Easy** |
| **`result/PushStrategy.kt`** | `PushStrategy` sealed hierarchy | Push result types — no unit tests | **Easy** |
| **`result/PopResult.kt`** | `PopResult` sealed hierarchy | Pop result types — no unit tests | **Easy** |
| **`result/BackResult.kt`** | `BackResult` sealed hierarchy | Back result types — no unit tests | **Easy** |
| **`config/PopBehavior.kt`** | `PopBehavior` enum | Pop behavior types — simple, but untested | **Easy** |
| **`util/KeyGenerator.kt`** | `KeyGenerator` interface | Key generation — no unit tests | **Easy** |
| **`NavigationResultManager.kt`** (internal) | `NavigationResultManager` | Result passing between screens — no tests | **Medium** |
| **`NavKeyGenerator.kt`** (internal) | `NavKeyGenerator` object | Key generation utility — no tests | **Easy** |

#### Medium-Impact Gaps (Config/Registry — Moderate to Test)

| Source File | Symbols | Why Important | Difficulty |
|-------------|---------|---------------|------------|
| **`config/NavigationConfig.kt`** | `NavigationConfig` interface | Core configuration interface — no tests | **Easy** (interface) |
| **`config/CompositeNavigationConfig.kt`** | `CompositeNavigationConfig` | Composes multiple configs — no tests | **Easy** |
| **`internal/config/EmptyNavigationConfig.kt`** | `EmptyNavigationConfig` | Default empty config — no tests | **Easy** |
| **`internal/config/CompositeNavigationConfig.kt`** | `CompositeNavigationConfig` (internal) | Internal composite — no tests | **Easy** |
| **`registry/RouteRegistry.kt`** | `RouteRegistry` object | Route registration — no tests | **Easy** |
| **`registry/TransitionRegistry.kt`** | `TransitionRegistry` interface | Transition lookup — interface only | **N/A** |
| **`registry/PaneRoleRegistry.kt`** | `PaneRoleRegistry` interface | Pane role lookup — interface only | **N/A** |
| **`CompositeTransitionRegistry.kt`** | `CompositeTransitionRegistry` | Multi-module transitions — no tests | **Easy** |
| **`RuntimeDeepLinkRegistry.kt`** | `RuntimeDeepLinkRegistry` | Tested in DeepLinkRegistryTest ✅ | Already covered |
| **`CompositeDeepLinkRegistry.kt`** | `CompositeDeepLinkRegistry` | Tested in DeepLinkRegistryTest ✅ | Already covered |

#### DSL Gaps (Moderate to Test)

| Source File | Symbols | Status | Difficulty |
|-------------|---------|--------|------------|
| **`NavigationConfigBuilder.kt`** | `NavigationConfigBuilder`, `ScopeBuilder` | Only cross-module tested | **Medium** |
| **`StackBuilder.kt`** | `StackBuilder`, `StackScreenEntry` | No direct tests | **Medium** |
| **`TabsBuilder.kt`** | `TabsBuilder`, `TabEntry` | No direct tests | **Medium** |
| **`PanesBuilder.kt`** | `PanesBuilder`, `PaneEntry`, `PaneContentBuilder` | No direct tests | **Medium** |
| **`ContainerBuilder.kt`** | `ContainerBuilder` | No direct tests | **Medium** |
| All `dsl/internal/*` | Internal DSL implementations | No direct tests | **Medium** |
| All `dsl/Dsl*Registry.kt` | DSL registry wrappers | No direct tests | **Medium** |

#### Compose/UI Gaps (Hard to Test — Excluded by Kover Filter)

| Source File | Status | Note |
|-------------|--------|------|
| `NavigationHost.kt` | `NavigationHost` composable — @Composable excluded from Kover | Excluded by config |
| `NavBackHandler.kt` | @Composable excluded | Excluded by config |
| `NavigateBackHandler.kt` | @Composable excluded | Excluded by config |
| `PlatformBackInput.kt` | @Composable excluded | Excluded by config |
| All `internal/render/*.kt` | @Composable excluded | Excluded by config |
| `ComposableCache.kt` | Mixed — has non-composable logic | **Medium** |
| `AnimationCoordinator.kt` | Non-composable class — no tests | **Medium** |
| `PredictiveBackController.kt` | Non-composable class — no tests | **Medium** |
| All `scope/*.kt` | Mostly interfaces + @Composable | Partially excluded |
| `WindowSizeClass.kt` | Non-composable logic | **Easy** |

#### Other Gaps

| Source File | Status | Difficulty |
|-------------|--------|------------|
| `NavDestination.kt` | Interface — minimal testable logic | **N/A** |
| `navigation/transition/NavigationTransition.kt` | Complex, has builders | **Medium** |
| `navigation/transition/TransitionState.kt` | Interface | **N/A** |
| `navigation/result/ReturnsResult.kt` | Interface | **N/A** |
| `navigation/result/NavigatorResultExtensions.kt` | Extension functions | **Medium** |
| `navigation/pane/PaneConfiguration.kt` | Data class | **Easy** |
| `navigation/pane/PaneBackBehavior.kt` | Enum | **Easy** |
| `navigation/pane/AdaptStrategy.kt` | Enum | **Easy** |
| `navigation/pane/PaneRole.kt` | Enum | **Easy** |
| `InternalQuoVadisApi.kt` | Annotation | **N/A** |
| `TypeAliases.kt` | Type aliases | **N/A** |
| `GeneratedTabMetadata.kt` | Data class | **Easy** |

---

## 5. Test Infrastructure

### FakeNavigator
- Location: `commonTest/.../FakeNavigator.kt`
- Full `Navigator` interface implementation
- Records `NavigationCall` history for assertion
- Supports: state, currentDestination, previousDestination, canNavigateBack, config
- Supports: navigate, navigateBack, navigateAndClearTo, navigateAndReplace, navigateAndClearAll, handleDeepLink, updateState

### FakeNavRenderScope
- Location: `commonTest/.../FakeNavRenderScope.kt`
- Implements `NavRenderScope` for compose tests
- Includes `FakeSaveableStateHolder`, `EmptyScreenRegistry`

### NavigationTestDsl
- Location: `commonTest/.../testing/NavigationTestDsl.kt`
- `NavigationTestScope` wrapping `TreeNavigator`
- Rich assertion helpers: `assertCurrentDestination`, `assertBackStackSize`, `assertCanNavigateBack`, `assertNodeCount`, `assertDestinationInBackStack`
- Factory: `navigationTest { ... }` DSL

### NavigatorTestHelpers
- Location: `commonTest/.../testing/NavigatorTestHelpers.kt`
- `withDestination`, `withState`, `singleScreen` — convenience builders

### Test Framework
- **Kotest** FunSpec with `shouldBe` / `shouldThrow` assertions
- Tests run as `commonTest` (platform-agnostic) + 1 `desktopTest`

---

## 6. Prioritized Recommendations for Reaching 70% Coverage

### Tier 1: Highest Impact / Easiest (Pure Logic Objects)

These are **internal pure-function objects** that represent the bulk of navigation logic. They're tested *indirectly* through TreeMutator tests, but adding direct unit tests will significantly boost line coverage.

| Priority | File | Estimated Lines | Effort |
|----------|------|----------------|--------|
| 1 | `TreeDiffCalculator.kt` | Medium | Low |
| 2 | `operations/PushOperations.kt` | High | Low |
| 3 | `operations/PopOperations.kt` | High | Low |
| 4 | `operations/BackOperations.kt` | Medium | Low |
| 5 | `operations/TabOperations.kt` | Medium | Low |
| 6 | `operations/PaneOperations.kt` | Medium | Low |
| 7 | `operations/TreeNodeOperations.kt` | Medium | Low |
| 8 | `ScreenKeyCollector.kt` | Small | Low |
| 9 | `LifecycleNotifier.kt` | Medium | Medium |
| 10 | `TransitionManager.kt` | Medium | Medium |

### Tier 2: Result/Config Types (Quick Wins)

| Priority | File | Estimated Lines | Effort |
|----------|------|----------------|--------|
| 11 | `result/TreeOperationResult.kt` | Small | Very Low |
| 12 | `result/PushStrategy.kt` | Small | Very Low |
| 13 | `result/PopResult.kt` | Small | Very Low |
| 14 | `result/BackResult.kt` | Small | Very Low |
| 15 | `NavigationResultManager.kt` (internal) | Medium | Medium |
| 16 | `NavKeyGenerator.kt` | Small | Very Low |
| 17 | `config/CompositeNavigationConfig.kt` (internal) | Small | Low |
| 18 | `config/EmptyNavigationConfig.kt` | Tiny | Very Low |
| 19 | `CompositeTransitionRegistry.kt` | Small | Low |
| 20 | `RouteRegistry.kt` | Small | Low |

### Tier 3: DSL Builders (Medium Impact)

| Priority | File | Estimated Lines | Effort |
|----------|------|----------------|--------|
| 21 | `NavigationConfigBuilder.kt` | Medium | Medium |
| 22 | `StackBuilder.kt` | Medium | Medium |
| 23 | `TabsBuilder.kt` | Medium | Medium |
| 24 | `PanesBuilder.kt` | Medium | Medium |
| 25 | `ContainerBuilder.kt` | Small | Medium |
| 26-31 | `dsl/internal/*` | Medium total | Medium |

### Tier 4: Non-Composable Compose Internals

| Priority | File | Note |
|----------|------|------|
| 32 | `AnimationCoordinator.kt` | Non-composable class |
| 33 | `PredictiveBackController.kt` | Non-composable class |
| 34 | `ComposableCache.kt` | Has non-composable logic |
| 35 | `WindowSizeClass.kt` | Calculation logic testable |

### Not Recommended for Coverage Push
- **All `@Composable` functions** — excluded by Kover filter, hard to unit test
- **Pure interfaces** with no logic (`NavigationConfig`, `ScreenRegistry`, `TransitionRegistry`, etc.)
- **Enums/data classes** with no logic (`PaneRole`, `AdaptStrategy`, etc.)
- **Type aliases** and **annotations**

---

## 7. Summary Statistics

| Category | Count |
|----------|-------|
| **Source files** (commonMain) | ~100 |
| **Test files** (commonTest + desktopTest) | 31 test classes + 4 infrastructure files |
| **Source files with direct tests** | ~25 |
| **Source files without tests** | ~45 testable files |
| **Source files excluded by Kover** | ~25 (Composable-annotated) |
| **Interfaces with no logic** | ~10 (not worth testing directly) |

### Estimated Current Coverage
Based on the pattern of what's tested:
- **TreeMutator + Operations**: Likely ~60-70% (well-tested but operations are tested indirectly)
- **NavNode types + extensions**: Likely ~70-80%
- **TreeNavigator**: Likely ~50-60% (complex class, only partially tested)
- **Registries**: Likely ~50-70% (most composites have tests)
- **DSL**: Likely ~20-30% (only cross-module test exists)
- **Compose layer**: ~0% counted (excluded by Kover) but some non-composable parts counted
- **Result types / Config**: Likely ~10-20%

**Overall estimated coverage**: **40-55%** (before enforcement)

### Path to 70%
Focus on **Tier 1** (operations + tree utilities) and **Tier 2** (result types + config). Together these represent the largest blocks of untested pure logic and should push coverage from ~50% to ~70% with relatively low effort. DSL tests (Tier 3) would provide the remaining buffer if needed.
