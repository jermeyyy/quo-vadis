# Implementation Plan: Container Destination Arguments

## Overview

Add support for `@Tabs` (and extensible to `@Pane`) data classes with `@Argument` constructor properties, enabling tab container destinations to carry arguments that are accessible to shared MVI containers and child screens.

**Example API:**
```kotlin
@Tabs(name = "containerDemo/{itemId}")
data class ContainerDemoDestination(
    @Argument val itemId: Int
) : NavDestination {
    @TabItem(parent = ContainerDemoDestination::class, isDefault = true)
    @Destination(route = "container-demo/{itemId}/info")
    data object Info : NavDestination

    @TabItem(parent = ContainerDemoDestination::class)
    @Destination(route = "container-demo/{itemId}/stats")
    data object Stats : NavDestination
}
```

**Navigation call:**
```kotlin
navigator.navigate(ContainerDemoDestination(itemId = 42))
// Tab container opens with itemId=42 accessible to SharedNavigationContainer
```

## Requirements

- `@Tabs`-annotated `data class` containers with `@Argument` properties
- Destination instance stored on `TabNode` (follows `ScreenNode` pattern)
- `SharedContainerScope.containerDestination` provides typed access in MVI containers
- Backward compatible: existing `object`-based `@Tabs` containers continue to work
- KSP auto-detects `@Argument` on `@Tabs` data classes and generates appropriate code
- Deep linking is out of scope (follow-up task)

## Technical Approach

### Data Flow

```
navigator.navigate(ContainerDemoDestination(itemId=42))
  → TreeNavigator.navigate(destination)
  → containerRegistry.getContainerInfo(destination)
      destination instance captured in ContainerInfo closure
  → ContainerInfo.TabContainer.builder(key, parentKey, tabIndex, destination)
      builder creates TabNode with destination stored
  → TabNode(destination = ContainerDemoDestination(42), ...)
  → TabRenderer reads node.destination
  → CompositionLocalProvider(LocalContainerDestination provides node.destination)
  → rememberSharedContainer reads LocalContainerDestination
  → SharedContainerScope(containerDestination = destination)
  → ContainerDemoContainer reads scope.containerDestination as ContainerDemoDestination
```

### Key Design Decisions

1. **TabNode stores `val destination: NavDestination? = null`** — nullable for backward compatibility with existing object-based tabs that have no meaningful destination data
2. **ContainerInfo.TabContainer.builder gets destination parameter** — the builder lambda gains a `NavDestination` parameter so it can pass it through to TabNode
3. **CompositionLocal for container destination** — `LocalContainerDestination` provides the destination to the compose tree, parallel to `LocalContainerNode`
4. **KSP reuses `DestinationExtractor.extractConstructorParams`** — same `@Argument` extraction logic already exists

## Tasks

### Phase 1: Core Node Layer

#### Task 1.1: Add `destination` property to `TabNode`
- **Description:** Add `val destination: NavDestination? = null` to `TabNode` constructor. Update `copy()`, `equals()`, `hashCode()`, and `toString()`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/node/TabNode.kt`
- **Dependencies:** None
- **Acceptance Criteria:**
  - `TabNode` has a `destination: NavDestination?` property, defaulting to `null`
  - `copy()` includes `destination` parameter
  - `equals()`/`hashCode()` include `destination`
  - Serialization works (NavDestination is already serializable via polymorphic serializer in ScreenNode)
  - All existing tests pass (null default is backward-compatible)

#### Task 1.2: Add `destination` property to `PaneNode` (extensibility)
- **Description:** Add `val destination: NavDestination? = null` to `PaneNode` constructor for future pane argument support. Update `copy()`, `equals()`, `hashCode()`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/node/PaneNode.kt`
- **Dependencies:** None
- **Acceptance Criteria:**
  - `PaneNode` has a `destination: NavDestination?` property, defaulting to `null`
  - Backward-compatible, no behavioral change

### Phase 2: ContainerInfo and Registry Layer

#### Task 2.1: Extend `ContainerInfo.TabContainer` builder signature
- **Description:** Change `ContainerInfo.TabContainer.builder` from `(NodeKey, NodeKey?, Int) -> TabNode` to `(NodeKey, NodeKey?, Int, NavDestination?) -> TabNode`. The new `NavDestination?` parameter carries the container destination instance.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/ContainerRegistry.kt` (the `ContainerInfo` sealed interface)
- **Dependencies:** Task 1.1
- **Acceptance Criteria:**
  - `TabContainer.builder` signature includes `NavDestination?` parameter
  - KDoc updated to document the new parameter

#### Task 2.2: Update `DslContainerRegistry.createTabContainerInfo` to pass destination
- **Description:** The `createTabContainerInfo` method creates the builder lambda. It currently calls `navNodeBuilder(containerClass, key, parentKey)`. It must now also accept and forward a `NavDestination?` to the built `TabNode`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/internal/DslContainerRegistry.kt`
- **Dependencies:** Task 2.1
- **Acceptance Criteria:**
  - Builder lambda captures and passes destination to TabNode
  - `TabNode.copy(destination = ...)` called when destination is non-null

#### Task 2.3: Update `DslContainerRegistry.getContainerInfo` and callers
- **Description:** Currently `getContainerInfo(destination: NavDestination)` passes `destination` as a lookup key but doesn't capture its instance for the builder. The destination instance must be captured in the returned `ContainerInfo` closure so the builder can access it.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/internal/DslContainerRegistry.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/internal/CompositeContainerRegistry.kt`
- **Dependencies:** Task 2.2
- **Acceptance Criteria:**
  - When `getContainerInfo(destination)` is called, the returned `ContainerInfo.TabContainer` captures the destination instance
  - The builder lambda passes it through to TabNode construction

### Phase 3: TreeNavigator Integration

#### Task 3.1: Pass destination through navigator's container creation
- **Description:** `TreeNavigator.navigateWithContainer` and `pushContainer` call `containerInfo.builder(key, parentKey, tabIndex)`. Update to pass destination: `containerInfo.builder(key, parentKey, tabIndex, destination)`. Since `getContainerInfo(destination)` already has the destination, it's captured in the closure — verify this works, or thread it explicitly.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeNavigator.kt`
- **Dependencies:** Task 2.3
- **Acceptance Criteria:**
  - Container builder calls include destination parameter
  - TabNode created with destination set when the container is a data class with arguments

### Phase 4: Compose Rendering Layer

#### Task 4.1: Add `LocalContainerDestination` CompositionLocal
- **Description:** Create a new CompositionLocal `LocalContainerDestination` of type `NavDestination?` alongside existing `LocalContainerNode`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/CompositionLocals.kt`
- **Dependencies:** None
- **Acceptance Criteria:**
  - `LocalContainerDestination` declared with default `null`
  - KDoc matches style of `LocalContainerNode`

#### Task 4.2: Provide `LocalContainerDestination` in `TabRenderer`
- **Description:** In `TabRenderer`, after providing `LocalContainerNode`, also provide `LocalContainerDestination` from `node.destination`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/TabRenderer.kt`
- **Dependencies:** Tasks 1.1, 4.1
- **Acceptance Criteria:**
  - `CompositionLocalProvider(LocalContainerDestination provides node.destination)` added
  - For tabs without destination (null), local is null (backward compatible)

#### Task 4.3: Provide `LocalContainerDestination` in `PaneRenderer`
- **Description:** Same as Task 4.2 but for `PaneRenderer`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PaneRenderer.kt`
- **Dependencies:** Tasks 1.2, 4.1
- **Acceptance Criteria:**
  - `CompositionLocalProvider(LocalContainerDestination provides node.destination)` added

### Phase 5: FlowMVI Integration

#### Task 5.1: Update `rememberSharedContainer` to pass `containerDestination`
- **Description:** `rememberSharedContainer` reads `LocalContainerDestination.current` and passes it to `SharedContainerScope`. Currently, `SharedContainerScope` already has a `containerDestination: NavDestination` property but the composable doesn't pass it.
- **Files:**
  - `quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/ContainerComposables.kt`
- **Dependencies:** Tasks 4.1, 4.2
- **Acceptance Criteria:**
  - `rememberSharedContainer` reads `LocalContainerDestination.current`
  - Passes it to `SharedContainerScope(containerDestination = ...)`
  - When null (object container), a sensible default is provided or property is nullable

#### Task 5.2: Adjust `SharedContainerScope.containerDestination` nullability
- **Description:** `containerDestination` is currently `NavDestination` (non-null). For backward compatibility with containers without arguments, consider making it `NavDestination?` or providing a sentinel default.
- **Files:**
  - `quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/SharedContainerScope.kt`
- **Dependencies:** Task 5.1
- **Acceptance Criteria:**
  - Property is nullable or has a safe default
  - Existing SharedNavigationContainer implementations continue to work
  - Demo `ContainerDemoContainer` can safely cast to `ContainerDemoDestination`

### Phase 6: KSP Code Generation

#### Task 6.1: Extract constructor params from `@Tabs` data classes
- **Description:** Extend `TabExtractor` to detect when a `@Tabs` class is a `data class` (not an `object`) and extract `@Argument`-annotated constructor parameters using the same `extractConstructorParams` logic from `DestinationExtractor`.
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Dependencies:** None
- **Acceptance Criteria:**
  - `TabExtractor` detects data class vs object `@Tabs`
  - Calls `extractConstructorParams` for data classes
  - Extracts `@Argument`-annotated params with types

#### Task 6.2: Extend `TabInfo` model with constructor params
- **Description:** Add `constructorParams: List<ParamInfo>`, `isDataClass: Boolean`, and `isObject: Boolean` fields to `TabInfo`.
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`
- **Dependencies:** Task 6.1
- **Acceptance Criteria:**
  - `TabInfo` has new fields with backward-compatible defaults (`emptyList()`, `false`, `true`)

#### Task 6.3: Update `ContainerBlockGenerator` for data class tabs
- **Description:** When generating DSL code for a `@Tabs` data class, the generator needs to handle the fact that the container is no longer just a type reference. The generated DSL code should remain `tabs<T>(scopeKey = ...)` — the data class aspect is handled at navigation time when `getContainerInfo` captures the destination instance. **No DSL syntax change needed** since `getContainerInfo(destination)` already receives the destination instance.
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`
- **Dependencies:** Task 6.2
- **Acceptance Criteria:**
  - Generated DSL code for data class `@Tabs` is valid
  - Code review for correctness

#### Task 6.4: Update `NavigationConfigGenerator` node builder for data class tabs
- **Description:** The `buildBuildNavNodeFunction` generates `buildNavNode(destinationClass, key, parentKey)`. When the `@Tabs` class is a data class, the node builder needs to also handle constructing the destination instance from passed arguments — or defer to the runtime (since `getContainerInfo` captures the destination). Assess whether generator changes are needed or if the runtime chain from Phase 2-3 handles this.
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/NavigationConfigGenerator.kt`
- **Dependencies:** Tasks 6.2, 6.3
- **Acceptance Criteria:**
  - Generated `buildNavNode` handles data class `@Tabs` containers correctly
  - TabNode is built with appropriate destination at runtime

#### Task 6.5: Validation rules for `@Tabs` data classes
- **Description:** Add KSP validation:
  - `@Tabs` data classes MUST have all constructor params annotated with `@Argument`
  - `@Tabs` data class with no `@Argument` params emits a warning
  - `@Tabs` data class with params BUT no `@Argument` emits an error
- **Files:**
  - `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Dependencies:** Task 6.1
- **Acceptance Criteria:**
  - Compile-time error for `@Tabs` data class with unannotated params
  - Warning for empty-param data class (suggest using `object`)

### Phase 7: Demo and Tests

#### Task 7.1: Wire up `ContainerDemoDestination` in navigation config
- **Description:** Ensure the demo app registers `ContainerDemoDestination` and its tab items properly. Verify the generated code handles the data class.
- **Files:**
  - `navigation-api/src/commonMain/kotlin/com/jermey/navplayground/navigation/ContainerDemoDestination.kt` (already exists)
  - Generated navigation config in `composeApp/build/generated/`
- **Dependencies:** Phase 1-6
- **Acceptance Criteria:**
  - Demo app compiles and runs
  - Navigating to `ContainerDemoDestination(itemId = 42)` opens tabs
  - `ContainerDemoContainer` reads `itemId` from `scope.containerDestination`
  - Tab switching works with shared state

#### Task 7.2: Unit tests for TabNode with destination
- **Description:** Add tests for `TabNode` with destination property.
- **Files:**
  - `quo-vadis-core/src/commonTest/kotlin/` (appropriate test file)
- **Dependencies:** Task 1.1
- **Acceptance Criteria:**
  - TabNode creation with destination
  - TabNode copy with destination
  - TabNode equality/hashCode with destination
  - Serialization round-trip with destination

#### Task 7.3: Unit tests for ContainerInfo builder with destination
- **Description:** Test that `ContainerInfo.TabContainer.builder` correctly passes destination to TabNode.
- **Files:**
  - `quo-vadis-core/src/commonTest/kotlin/`
- **Dependencies:** Tasks 2.1, 2.2
- **Acceptance Criteria:**
  - Builder creates TabNode with destination set
  - Builder handles null destination (backward compat)

#### Task 7.4: KSP tests for `@Tabs` data class extraction
- **Description:** Test that TabExtractor correctly extracts params from `@Tabs` data classes.
- **Files:**
  - `quo-vadis-ksp/src/test/kotlin/`
- **Dependencies:** Tasks 6.1, 6.2
- **Acceptance Criteria:**
  - Data class `@Tabs` with `@Argument` params correctly extracted
  - Object `@Tabs` still works as before
  - Validation errors emitted for invalid configurations

## Sequencing

```
Phase 1 (Core Nodes)     ─── Tasks 1.1, 1.2  (parallel)
        │
Phase 2 (Registry)       ─── Task 2.1 → 2.2 → 2.3  (sequential)
        │
Phase 3 (Navigator)      ─── Task 3.1
        │
Phase 4 (Compose)        ─── Tasks 4.1, 4.2, 4.3  (4.1 first, then 4.2/4.3 parallel)
        │
Phase 5 (FlowMVI)        ─── Task 5.1 → 5.2
        │
Phase 6 (KSP)            ─── Tasks 6.1/6.2 (parallel) → 6.3 → 6.4 → 6.5
        │
Phase 7 (Demo + Tests)   ─── Tasks 7.1, 7.2, 7.3, 7.4  (mostly parallel)
```

Note: Phase 6 (KSP) can be done in parallel with Phases 1-5 since it generates code independently. However, integration testing (Phase 7) requires all phases complete.

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| NavDestination polymorphic serialization may not handle new data class types | TabNode serialization/deserialization fails | Verify existing serialization setup handles new NavDestination subclasses. The KSP already registers serializers for @Destination classes — extend to @Tabs data classes. |
| Breaking `ContainerInfo.TabContainer.builder` signature | All existing implementations and tests break | Use default parameter `destination: NavDestination? = null` to maintain backward compatibility in the lambda type. |
| `SharedContainerScope.containerDestination` nullability change | Existing shared containers may need update | Change to `NavDestination?` (nullable) — existing containers that don't use it are unaffected. |
| `rememberSharedContainer` needs `LocalContainerDestination` which must be provided before it runs | Race condition if CompositionLocal isn't set | `TabRenderer` provides the local before rendering children — the composition tree guarantees ordering. |
| Data class `@Tabs` with many arguments creates complex route patterns | Route parsing complexity | Defer deep linking to follow-up task. Arguments are only passed via `navigate(destination)` for now. |

## Open Questions

- ~~Deep linking for parameterized tab containers~~ → Deferred to follow-up task
- Should `TabsContainerScope` also expose `containerDestination`? (Currently only `SharedContainerScope` exposes it.) Consider adding `val containerDestination: NavDestination?` to `TabsContainerScope` for non-MVI access patterns.
