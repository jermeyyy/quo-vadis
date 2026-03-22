# Implementation Plan: quo-vadis-core Test Coverage to 70%

## Overview

Increase `quo-vadis-core` test coverage from an estimated 40–55% to **70% line coverage**. Add a per-module `koverVerify` rule that fails the build below 70%, keep `@Composable` code excluded, and add direct unit tests for all `operations/` files. Root merged reporting stays untouched.

## Requirements

- Enable per-module `koverVerify` for `quo-vadis-core` at 70% line coverage minimum
- Keep existing root merged Kover reporting as-is (no changes to root `build.gradle.kts`)
- Keep `@Composable` exclusion from coverage metrics
- Add sufficient tests to reach 70% coverage
- All new tests use Kotest FunSpec with `shouldBe`/`shouldThrow` assertions

---

## Kover Configuration Changes

### Task 0: Add per-module Kover verification to `quo-vadis-core/build.gradle.kts`

**File:** `quo-vadis-core/build.gradle.kts`

Append the following block at the end of the file (after the existing `tasks.withType<Test>` block):

```kotlin
kover {
    reports {
        filters {
            excludes {
                classes(
                    "*.generated.*",
                    "*.BuildConfig",
                    "*.Companion",
                    "*Test*",
                    "*Fake*",
                )
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }
        total {
            xml {
                onCheck = false
                xmlFile = layout.buildDirectory.file("reports/kover/report.xml")
            }
            html {
                onCheck = false
                htmlDir = layout.buildDirectory.dir("reports/kover/html")
            }
        }
        verify {
            rule {
                minBound(70) // Minimum 70% line coverage for quo-vadis-core
            }
        }
    }
}
```

**Acceptance criteria:**
- `./gradlew :quo-vadis-core:koverVerify` fails if line coverage < 70%
- `./gradlew :quo-vadis-core:koverHtmlReport` generates a module-level HTML report
- Root merged reporting (`./gradlew koverHtmlReport`) continues to work unchanged

**First action after applying:** Run `./gradlew :quo-vadis-core:koverHtmlReport` to establish baseline coverage number.

---

## Test Implementation Tasks

### Phase 1: Tree Operations & Utilities (Highest Impact)

These are pure `object` functions — no Compose dependencies, easy to test in `commonTest`, high line-count impact.

**Test base path:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/`

---

#### Task 1: PushOperations

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/PushOperationsTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/PushOperations.kt` (579 lines)

**Key symbols to test:**
- `push(root, destination, scopeRegistry)` — main entry point
- `pushToStack(root, destination)` — direct stack push
- `push(root, destinations, scopeRegistry)` — multi-destination push
- `clearAndPush(root, destination)` — clear root stack and push
- `clearStackAndPush(root, stackKey, destination)` — clear specific stack and push
- `replaceCurrent(root, destination)` — replace top screen
- `pushAll(root, destinations)` — batch push
- `determinePushStrategy(root, destination, scopeRegistry)` — strategy resolution
- `findTabWithDestination(tabNode, destinationClass)` — tab search
- `pushToActiveStack(root, destination)` — push to deepest active stack
- `pushOutOfScope(root, destination, scopeKey)` — push to parent scope
- `pushToPaneStack(paneNode, destination, role)` — push into specific pane

**What to test:**
- Push to simple stack (single screen, multiple screens)
- Push into nested stacks (tab → stack, pane → stack)
- Push with scope — destination routed to correct parent stack
- `determinePushStrategy` returns `PushToStack`, `SwitchToTab`, `PushToPaneStack`, `PushOutOfScope`
- `clearAndPush` removes all but root screen
- `clearStackAndPush` targets a specific stack by key
- `replaceCurrent` swaps top screen without growing stack
- `pushAll` adds multiple screens in order
- `pushToPaneStack` pushes to primary/secondary/extra pane
- Edge cases: push to empty stack, push duplicate destination, push to non-existent scope

**Dependencies:** None  
**Acceptance criteria:** All `PushOperations` public methods have at least one happy-path and one edge-case test.

---

#### Task 2: PopOperations

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/PopOperationsTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/PopOperations.kt` (216 lines)

**Key symbols to test:**
- `pop(root)` — pop top screen from active stack
- `popTo(root, destination)` — pop to specific destination instance
- `popTo(root, destinationClass)` — pop to destination by class
- `popToRoute(root, route)` — pop to destination by route string
- `popToDestination(root, predicate)` — pop with custom predicate
- `handleEmptyStackPop(root, stackNode)` — behavior when stack has only root screen

**What to test:**
- Pop from stack with multiple screens → returns `Popped` with new root
- Pop from stack with single screen → returns `CannotPop` or `RequiresScaffoldChange`
- `popTo` with matching destination found → removes screens above it
- `popTo` with no match → returns failure
- `popToRoute` matches by route string
- `popToDestination` with predicate matching/not matching
- `handleEmptyStackPop` in nested tab/pane scenarios
- Pop from pane stack → `PaneEmpty` result when empty

**Dependencies:** None  
**Acceptance criteria:** All `PopOperations` public methods tested with success/failure paths.

---

#### Task 3: BackOperations

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/BackOperationsTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/BackOperations.kt` (323 lines)

**Key symbols to test:**
- `canGoBack(root)` — whether back navigation is possible
- `currentDestination(root)` — resolve current visible destination
- `popWithTabBehavior(root, popBehavior)` — pop with tab-aware behavior
- `canHandleBackNavigation(root)` — full back-handling check
- `handleRootStackBack(rootStack)` — back on root-level stack
- `handleTabBack(root, tabNode)` — back within tab navigation
- `handleNestedStackBack(root, stackNode)` — back in nested stack
- `handlePaneBack(root, paneNode)` — back in pane layout
- `popEntirePaneNode(root, paneNode)` — remove entire pane from parent stack

**What to test:**
- `canGoBack` returns true when stack has >1 screen, false for single-screen root
- `currentDestination` traverses tree to find active leaf screen
- `popWithTabBehavior` with different `PopBehavior` values
- `handleTabBack` switches to initial tab when non-initial tab active, delegates to system when initial tab at root
- `handleNestedStackBack` pops nested stack or bubbles up
- `handlePaneBack` with different `PaneBackBehavior` modes
- `popEntirePaneNode` removes pane and returns to parent stack
- `BackResult.Handled`, `BackResult.DelegateToSystem`, `BackResult.CannotHandle` scenarios

**Dependencies:** None  
**Acceptance criteria:** All `BackOperations` public methods tested; all `BackResult` variants exercised.

---

#### Task 4: TabOperations

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/TabOperationsTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/TabOperations.kt` (71 lines)

**Key symbols to test:**
- `switchTab(root, tabKey, tabIndex)` — switch to tab by key + index
- `switchActiveTab(root, tabNode, tabIndex)` — switch active tab on specific TabNode

**What to test:**
- Switch from tab 0 to tab 1 in a two-tab layout
- Switch to already-active tab → no-op / same tree
- Switch to out-of-bounds tab index → error result
- Switch tab by key when key not found in tree → error result
- Verify active tab index updates correctly

**Dependencies:** None  
**Acceptance criteria:** Both methods tested with valid/invalid inputs.

---

#### Task 5: PaneOperations

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/PaneOperationsTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/PaneOperations.kt` (423 lines)

**Key symbols to test:**
- `navigateToPane(root, paneKey, destination, role)` — push to a specific pane role
- `switchActivePane(root, paneNode, role)` — change which pane is active
- `popPane(root, paneKey, role)` — pop from specific pane
- `popWithPaneBehavior(root, paneNode, backBehavior)` — back with behavior modes
- `popPaneAdaptive(root, paneNode, windowSizeClass)` — adaptive pop based on window size
- `popFromActivePane(paneNode)` — pop from currently active pane stack
- `clearPaneStack(root, paneKey, role)` — clear all screens in a pane stack
- `setPaneConfiguration(root, paneKey, config)` — set pane layout configuration
- `removePaneConfiguration(root, paneKey)` — remove pane configuration

**What to test:**
- Navigate to primary/secondary pane
- Switch active pane from primary to secondary
- Pop from secondary pane → falls back to primary
- `popWithPaneBehavior` with `PaneBackBehavior` modes (PopFromActive, SwitchToFirst, etc.)
- `popPaneAdaptive` with compact vs expanded window size
- `clearPaneStack` removes all non-root screens
- `setPaneConfiguration` / `removePaneConfiguration` updates tree correctly
- Edge cases: navigate to non-existent pane key, pop from single-screen pane

**Dependencies:** None  
**Acceptance criteria:** All 9 public methods tested; different `PaneBackBehavior` and `PaneRole` variants exercised.

---

#### Task 6: TreeNodeOperations

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/TreeNodeOperationsTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/operations/TreeNodeOperations.kt` (200 lines)

**Key symbols to test:**
- `replaceNode(root, nodeKey, newNode)` — replace a node by key
- `tryReplaceNode(node, nodeKey, newNode)` — recursive replacement attempt
- `removeNode(root, nodeKey)` — remove a node by key
- `tryRemoveNode(node, nodeKey)` — recursive removal attempt

**What to test:**
- Replace a ScreenNode in a stack → new node in correct position
- Replace a TabNode's child stack → tree structure preserved
- Replace with nodeKey not found → `NodeNotFound` result
- Remove a ScreenNode from stack → stack shrinks by one
- Remove from nested tree (tab → stack → screen)
- Remove node that doesn't exist → `NodeNotFound` result
- Replace/remove root node behavior

**Dependencies:** None  
**Acceptance criteria:** All 4 public methods tested with found/not-found cases across stack, tab, and pane trees.

---

#### Task 7: TreeDiffCalculator

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeDiffCalculatorTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeDiffCalculator.kt` (104 lines)

**Key symbols to test:**
- `computeDiff(oldTree, newTree)` — compute `TreeDiff` between two tree states
- `collectNodeInfo(node)` — collect all node keys and info from a tree
- `collectRecursive(node, result)` — recursive traversal

**What to test:**
- Identical trees → empty diff (no added, no removed)
- New screen pushed → diff contains one added node
- Screen popped → diff contains one removed node
- Tab switch → diff shows reordering changes
- Complete tree replacement → all old removed, all new added
- Nested pane changes detected correctly

**Dependencies:** None  
**Acceptance criteria:** `computeDiff` tested for add, remove, reorder, and no-change scenarios.

---

#### Task 8: ScreenKeyCollector

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/ScreenKeyCollectorTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/ScreenKeyCollector.kt` (85 lines)

**Key symbols to test:**
- `cancelResultsForKeys(keys)` — cancel pending results for specific keys
- `cancelResultsForDestroyedScreens(oldTree, newTree)` — cancel results for removed screens
- `collectScreenKeys(node)` — collect all screen keys from tree
- `collectScreenKeysRecursive(node, result)` — recursive collection

**What to test:**
- Collect keys from flat stack → returns all screen keys
- Collect keys from tab tree → returns keys from all tabs
- Collect keys from pane tree → returns keys from all panes
- `cancelResultsForDestroyedScreens` identifies removed screens and cancels their results
- `cancelResultsForKeys` with no pending results → no-op

**Dependencies:** Task 13 (NavigationResultManager) may help, but can mock/fake  
**Acceptance criteria:** All public methods tested; key collection covers stack, tab, and pane tree shapes.

---

### Phase 2: Result Types & Config (Quick Wins)

Small files, easy to test, fills coverage gaps efficiently.

**Test base path:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/`

---

#### Task 9: TreeOperationResult

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/TreeOperationResultTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/TreeOperationResult.kt`

**Key symbols:** `TreeOperationResult.Success`, `TreeOperationResult.NodeNotFound`, `getOrElse()`, `getOrNull()`

**What to test:**
- `Success.getOrNull()` returns the new root
- `Success.getOrElse { }` returns the new root (doesn't call fallback)
- `NodeNotFound.getOrNull()` returns null
- `NodeNotFound.getOrElse { default }` returns default value
- `Success` data equality
- `NodeNotFound` data equality and message

**Dependencies:** None  
**Acceptance criteria:** All sealed variants and extension functions tested.

---

#### Task 10: PushStrategy

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/PushStrategyTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/PushStrategy.kt`

**Key symbols:** `PushToStack`, `SwitchToTab`, `PushToPaneStack`, `PushOutOfScope`

**What to test:**
- Each sealed variant can be constructed with correct properties
- Data class equality for each variant
- Destructuring / property access on each variant

**Dependencies:** None  
**Acceptance criteria:** All 4 sealed variants instantiated and verified.

---

#### Task 11: PopResult

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/PopResultTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/PopResult.kt`

**Key symbols:** `Popped`, `PaneEmpty`, `CannotPop`, `RequiresScaffoldChange`

**What to test:**
- `Popped` with new root and popped destination
- `PaneEmpty` with pane info
- `CannotPop` is a singleton object
- `RequiresScaffoldChange` is a singleton object
- Exhaustive when-expression coverage

**Dependencies:** None  
**Acceptance criteria:** All 4 sealed variants verified.

---

#### Task 12: BackResult

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/BackResultTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/result/BackResult.kt`

**Key symbols:** `Handled`, `DelegateToSystem`, `CannotHandle`

**What to test:**
- `Handled` with new root node
- `DelegateToSystem` singleton identity
- `CannotHandle` singleton identity
- Exhaustive when-expression coverage

**Dependencies:** None  
**Acceptance criteria:** All 3 sealed variants verified.

---

#### Task 13: NavigationResultManager

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/NavigationResultManagerTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/NavigationResultManager.kt` (107 lines)

**Key symbols:** `requestResult`, `completeResultSync`, `cancelResult`, `hasPendingResult`, `pendingCount`

**What to test:**
- `requestResult` creates a deferred result and increments `pendingCount`
- `completeResultSync` delivers value to waiting caller
- `cancelResult` cancels the deferred without delivering
- `hasPendingResult` returns true/false correctly
- `pendingCount` reflects current number of pending results
- Multiple concurrent pending results
- Complete a result that doesn't exist → no crash

**Dependencies:** None (uses coroutines, but testable with `runTest`)  
**Acceptance criteria:** All 5 public methods tested.

---

#### Task 14: NavKeyGenerator

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/NavKeyGeneratorTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/NavKeyGenerator.kt` (28 lines)

**Key symbols:** `generate(prefix)`, `reset()`

**What to test:**
- `generate` returns keys with given prefix
- Sequential calls return incrementing keys
- `reset` resets the counter so next key starts from 0
- Different prefixes produce distinct keys

**Dependencies:** None  
**Acceptance criteria:** Both methods tested.

---

#### Task 15: CompositeNavigationConfig (internal)

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/config/CompositeNavigationConfigTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/config/CompositeNavigationConfig.kt` (339 lines)

**Key symbols:** `screenRegistry`, `scopeRegistry`, `transitionRegistry`, `modalRegistry`, `containerRegistry`, `deepLinkRegistry`, `paneRoleRegistry`, `buildNavNode`, `mergeTabNodes`, `rekeyStack`, `rekeySubtree`, `plus`

**What to test:**
- Composite registry lookups prefer secondary over primary
- Primary used as fallback when secondary returns null
- `buildNavNode` delegates to container registries
- `mergeTabNodes` combines tabs from multiple configs
- `rekeyStack` / `rekeySubtree` assign unique keys to overlapping nodes
- `plus` creates a new `CompositeNavigationConfig`

**Dependencies:** None (can use `EmptyNavigationConfig` and DSL-built configs)  
**Acceptance criteria:** Registry delegation and node building tested.

---

#### Task 16: EmptyNavigationConfig

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/config/EmptyNavigationConfigTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/config/EmptyNavigationConfig.kt` (118 lines)

**Key symbols:** `screenRegistry`, `scopeRegistry`, `transitionRegistry`, `modalRegistry`, `containerRegistry`, `deepLinkRegistry`, `paneRoleRegistry`, `buildNavNode`, `plus`

**What to test:**
- All registries return empty/null lookups
- `buildNavNode` returns a default StackNode
- `plus` with another config returns a `CompositeNavigationConfig`
- `plus` with `EmptyNavigationConfig` is effectively identity

**Dependencies:** None  
**Acceptance criteria:** All properties and methods return expected defaults.

---

#### Task 17: CompositeTransitionRegistry

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/registry/CompositeTransitionRegistryTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/internal/CompositeTransitionRegistry.kt` (19 lines)

**Key symbols:** `getTransition(from, to)`

**What to test:**
- Returns secondary transition when available
- Falls back to primary when secondary returns null
- Returns null when neither has a match

**Dependencies:** None  
**Acceptance criteria:** Delegation priority verified.

---

#### Task 18: RouteRegistry

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/registry/RouteRegistryTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/RouteRegistry.kt` (27 lines)

**Key symbols:** `register(destinationClass, route)`, `getRoute(destinationClass)`

**What to test:**
- Register a route and retrieve it
- `getRoute` for unregistered destination → null
- Register overwrites previous route for same destination
- Multiple destinations with different routes

**Dependencies:** None  
**Acceptance criteria:** Both methods tested with present/absent cases.

---

### Phase 3: DSL Builders (Buffer for 70%)

Medium effort but good coverage gain to ensure the threshold is met.

**Test base path:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/dsl/`

---

#### Task 19: NavigationConfigBuilder + ScopeBuilder

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/dsl/NavigationConfigBuilderTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/NavigationConfigBuilder.kt` (529 lines)

**Key symbols:** `screen()`, `stack()`, `tabs()`, `panes()`, `scope()`, `transition()`, `modal()`, `tabsContainer()`, `paneContainer()`, `modalContainer()`, `build()`, `ScopeBuilder` (`unaryPlus`, `include`, `addMember`)

**What to test:**
- Build a config with a single screen → screenRegistry has it
- Build a config with a stack → containerRegistry has stack builder
- Build a config with tabs → containerRegistry has tabs builder
- Build a config with panes → containerRegistry has panes builder
- Register a scope → scopeRegistry returns correct scope members
- Register transitions → transitionRegistry returns them
- Register modal destinations → modalRegistry recognizes them
- `ScopeBuilder` `unaryPlus` and `include` add members correctly
- `build()` produces a valid `NavigationConfig`

**Dependencies:** Tasks 15-16 (to understand config composition)  
**Acceptance criteria:** Builder DSL produces configs with correct registries.

---

#### Task 20: StackBuilder

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/dsl/StackBuilderTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/StackBuilder.kt` (97 lines)

**Key symbols:** `screen()` (2 overloads), `build()`

**What to test:**
- Add screens via DSL and `build()` → returns registered screen list
- Add screen with destination class binding
- Empty builder → builds empty screen list

**Dependencies:** None  
**Acceptance criteria:** Both `screen` overloads and `build` tested.

---

#### Task 21: TabsBuilder

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/dsl/TabsBuilderTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/TabsBuilder.kt` (199 lines)

**Key symbols:** `tab()` (2 overloads), `containerTab()` (2 overloads), `initialTab`, `build()`

**What to test:**
- Add destination tabs and build → correct tab list
- Add stack tabs with nested screens
- Set `initialTab` → reflected in built config
- `containerTab` for nested tab or pane containers
- Build with no initial tab → uses first tab as default

**Dependencies:** None  
**Acceptance criteria:** All tab types and `build()` tested.

---

#### Task 22: PanesBuilder

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/dsl/PanesBuilderTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/PanesBuilder.kt` (203 lines)

**Key symbols:** `primary()`, `secondary()`, `extra()`, `tertiary()`, `initialPane`, `backBehavior`, `build()`

**What to test:**
- Configure primary + secondary panes and build
- Set `initialPane` and `backBehavior`  
- Add extra/tertiary panes
- Build with only primary → valid config

**Dependencies:** None  
**Acceptance criteria:** All pane role methods and `build()` tested.

---

#### Task 23: ContainerBuilder

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/dsl/ContainerBuilderTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/ContainerBuilder.kt` (84 lines)

**Key symbols:** `ContainerBuilder.Stack`, `ContainerBuilder.Tabs`, `ContainerBuilder.Panes`

**What to test:**
- Create each sealed variant with destination class and scope key
- Verify `destinationClass` and `scopeKey` properties
- Data class equality for each variant

**Dependencies:** None  
**Acceptance criteria:** All 3 sealed variants instantiated and verified.

---

### Phase 4: Internal Utilities (Additional Buffer)

Only implement if Phase 1-3 do not reach 70%.

---

#### Task 24: LifecycleNotifier

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/LifecycleNotifierTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/LifecycleNotifier.kt` (103 lines)

**Key symbols:** `notifyRemovedNodes`, `notifyRemovedNodesDetached`, `collectLifecycleAwareNodes`, `collectLifecycleAwareNodeKeys`

**What to test:**
- Removed nodes trigger lifecycle callbacks
- Detached notification for nodes that left tree
- Collect lifecycle-aware nodes from various tree shapes
- No callbacks for nodes still in tree

**Dependencies:** Requires `LifecycleAwareNode` implementations or fakes  
**Acceptance criteria:** Lifecycle notification flows tested.

---

#### Task 25: TransitionManager

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TransitionManagerTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TransitionManager.kt` (143 lines)

**Key symbols:** `transitionState`, `currentTransition`, `updateTransitionProgress`, `startPredictiveBack`, `updatePredictiveBack`, `cancelPredictiveBack`, `commitPredictiveBack`, `completeTransition`, `startNavigationTransition`

**What to test:**
- Start a navigation transition → `transitionState` updates
- Complete transition → state resets
- Start/update/cancel/commit predictive back flow
- `currentTransition` resolves correct transition for current state

**Dependencies:** Requires constructing with navigator scope and state provider (can fake)  
**Acceptance criteria:** Full transition lifecycle (start → update → complete/cancel) tested.

---

#### Task 26: WindowSizeClass

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/compose/util/WindowSizeClassTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/util/WindowSizeClass.kt`

**Key symbols:** `calculateFromSize(DpSize)`, `calculateFromSize(width, height)`, `Compact`, `Medium`, `Expanded`, `isCompactWidth`, `isAtLeastMediumWidth`, `isExpandedWidth`

**What to test:**
- `calculateFromSize` with small dimensions → Compact
- `calculateFromSize` with medium dimensions → Medium  
- `calculateFromSize` with large dimensions → Expanded
- `isCompactWidth`, `isAtLeastMediumWidth`, `isExpandedWidth` boolean properties
- Companion constants (`Compact`, `Medium`, `Expanded`) have correct values

**Dependencies:** Requires Compose `Dp`/`DpSize` — may need `desktopTest` source set  
**Acceptance criteria:** All size class calculations and convenience properties verified.

---

#### Task 27: AnimationCoordinator

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/compose/internal/AnimationCoordinatorTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/AnimationCoordinator.kt` (205 lines)

**Key symbols:** `getTransition(from, to, isPush)`, `getPaneTransition(paneRole, isPush)`, `Default`

**What to test:**
- `getTransition` with registered transition → returns it
- `getTransition` with no registration → returns default
- `getPaneTransition` with/without registration
- `Default` companion uses `TransitionRegistry.Empty`

**Dependencies:** Requires `TransitionRegistry` (can use `Empty` or create simple fake)  
**Acceptance criteria:** Transition resolution logic with registry lookups tested.

---

#### Task 28: PredictiveBackController

**Test file:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/compose/internal/PredictiveBackControllerTest.kt`

**Source file:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/PredictiveBackController.kt` (386 lines)

**Key symbols:** `isActive`, `progress`, `cascadeState`, `startGesture`, `startGestureWithCascade`, `updateGestureProgress`, `completeGesture`, `cancelGesture`

**What to test:**
- Initial state: `isActive` = false, `progress` = 0
- `startGesture` → `isActive` = true
- `updateGestureProgress(0.5f)` → `progress` = 0.5
- `completeGesture` → triggers completion flow
- `cancelGesture` → triggers cancel flow, resets to initial state
- `startGestureWithCascade` sets cascade state

**Dependencies:** Uses Compose `Animatable` — may need `desktopTest` or coroutine test scope  
**Acceptance criteria:** Gesture lifecycle (start → update → complete/cancel) tested.

---

## Sequencing

```
Task 0 (Kover config) ← DO FIRST — enables measurement
    │
    ├── Run: ./gradlew :quo-vadis-core:koverHtmlReport  (establish baseline)
    │
    ▼
Phase 1 (Tasks 1-8) ← PARALLEL — all independent, highest impact
    │
    ├── Run: ./gradlew :quo-vadis-core:koverHtmlReport  (check progress)
    │
    ▼
Phase 2 (Tasks 9-18) ← PARALLEL — all independent, quick wins
    │
    ├── Run: ./gradlew :quo-vadis-core:koverHtmlReport  (check progress)
    │
    ▼
Phase 3 (Tasks 19-23) ← PARALLEL — independent, buffer for 70%
    │
    ├── Run: ./gradlew :quo-vadis-core:koverVerify  (should pass at 70%)
    │
    ▼
Phase 4 (Tasks 24-28) ← ONLY IF NEEDED — additional buffer
    │
    ├── Run: ./gradlew :quo-vadis-core:koverVerify  (final verification)
```

After each phase, run `./gradlew :quo-vadis-core:koverHtmlReport` and review the report at `quo-vadis-core/build/reports/kover/html/index.html` to identify remaining gaps.

---

## Risks & Mitigations

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Actual coverage is lower than estimated (< 40%) | Medium | Run `koverHtmlReport` after Task 0 to get exact baseline; may need all 4 phases |
| Actual coverage is higher than estimated (> 55%) | Medium | Phase 1-2 alone may suffice; skip Phase 3-4 |
| Some internal classes hard to instantiate (dependencies on navigator scope) | Low | Use existing `NavigationTestDsl`, `NavigatorTestHelpers`, and `FakeNavigator` infrastructure |
| Operations tests overlap with existing `TreeMutator*Test` classes | Medium | Focus on edge cases, boundary conditions, and error paths not covered by TreeMutator tests |
| `@Composable` exclusion removes too many lines from denominator | Low | Phase 4 targets non-Composable internal utilities as additional buffer |
| `PredictiveBackController` / `AnimationCoordinator` need Compose runtime in tests | Medium | Place in `desktopTest` source set which has JVM Compose runtime available, or test only non-Compose parts |

---

## Open Questions

None — all requirements clarified with user.
