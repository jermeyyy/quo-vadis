# Merged Implementation Plan: Issue #41 — Cross-Module Tab Nesting

> **GitHub Issue**: #41 — "Nesting TabContainer in TabContainer of different module"  
> **Created**: 2026-03-12  
> **Status**: Planning  
> **Supersedes**: `plans/cross-module-tab-nesting.md` (Plan A), `plans/issue-41-cross-module-tabs-container-reference.md` (Plan B)

---

## 1. Overview

Enable `@Tabs` containers to reference existing navigation containers declared in other modules. This covers two use cases:

1. **Cross-module stack tab**: A `@Stack` defined in a feature module can be used as a tab item in an app module's `@Tabs`.
2. **Tabs-within-tabs**: A `@Tabs` container defined in one module can be nested as a tab item inside another module's `@Tabs`.

The runtime DSL already supports both patterns via `TabEntry.ContainerReference` and `containerTab<T>()`. The gap is in the **annotation/KSP** layer, which currently assumes all tab items are locally declared.

---

## 2. Problem Statement

### Current Limitation

`@Tabs(items = [...])` extraction assumes each item is a `@TabItem` that can be fully extracted in the current module as:

- `@Destination` → flat screen tab
- `@Stack` → nested stack tab (local sealed member)

This breaks for cross-module composition because an imported container should be treated as a **reference**, not re-declared as a local tab item. It also prevents nesting a `@Tabs` container inside another `@Tabs`.

### What Already Works (Runtime)

- `TabsBuilder.containerTab<T>()` registers a `TabEntry.ContainerReference`
- `DslNavigationConfig.buildTabStack()` handles `ContainerReference` by calling `buildNavNode()` recursively
- `buildNavNode()` returns either `StackNode` or `TabNode` depending on what's registered
- `CompositeNavigationConfig` merges configs from multiple modules via `+` operator
- `CompositeContainerRegistry` performs cross-config container-member lookup

### The Real Gap

The KSP processor needs to:

1. Detect when a class in `items` is a **container reference** (cross-module `@Stack` or `@Tabs`) vs. a local tab item
2. Generate `containerTab<T>()` instead of trying to extract local tab content
3. Ensure referenced containers are registered in the generated config so `buildNavNode()` can resolve them
4. Validate the new patterns and detect circular nesting

---

## 3. User-Confirmed Design Decisions

These decisions resolve all conflicts between Plan A and Plan B:

| Decision | Resolution | Rationale |
|----------|------------|-----------|
| **`@TabItem` requirement** | ALWAYS required for all items in `@Tabs(items = [...])` | Explicit opt-in; consistent regardless of item type |
| **`TabItemType` granularity** | Single `CONTAINER_REFERENCE` type for both `@Stack` and `@Tabs` references | Handles both uniformly; no need for separate `NESTED_TABS` |
| **Wrapper API** | NO changes; keep `tabs: List<NavDestination>` with pattern matching | No `TabItemDescriptor` or `tabItems` property |
| **Scope** | `@Stack` and `@Tabs` references only; `@Pane` deferred to future | Smallest useful scope |

---

## 4. Technical Approach

### 4.1 `TabItemType` Enum (in `TabInfo.kt`)

```kotlin
enum class TabItemType {
    FLAT_SCREEN,          // @TabItem + @Destination
    NESTED_STACK,         // @TabItem + @Stack (local sealed member pattern)
    CONTAINER_REFERENCE   // @TabItem + (@Stack or @Tabs) — referenced container
}
```

### 4.2 Detection Rules

For each class listed in `@Tabs(items = [...])`:

| Annotations Present | Sealed Member? | Result |
|---------------------|----------------|--------|
| `@TabItem` + `@Destination` | Any | `FLAT_SCREEN` |
| `@TabItem` + `@Stack` | Yes (local sealed member) | `NESTED_STACK` |
| `@TabItem` + `@Stack` | No (cross-module / standalone) | `CONTAINER_REFERENCE` |
| `@TabItem` + `@Tabs` | Any | `CONTAINER_REFERENCE` |
| Missing `@TabItem` | Any | **Validation error** |
| `@TabItem` only (no `@Destination`/`@Stack`/`@Tabs`) | Any | **Validation error** |

The distinction between `NESTED_STACK` and `CONTAINER_REFERENCE` for `@Stack` classes depends on whether the class is a local sealed member of the declaring `@Tabs` class or an external class referenced from another module.

### 4.3 Code Generation

| Type | Generated DSL |
|------|---------------|
| `FLAT_SCREEN` | `tab(Object)` |
| `NESTED_STACK` | existing nested stack generation (inline block) |
| `CONTAINER_REFERENCE` | `containerTab<ContainerType>()` |

For `CONTAINER_REFERENCE`, the referenced container must also be registered in the generated `NavigationConfig` so `buildNavNode()` can resolve it at runtime.

### 4.4 Runtime

**No runtime changes needed.** The existing `TabEntry.ContainerReference` + `buildNavNode()` + `CompositeNavigationConfig` handles everything. When `buildTabStack` encounters a `ContainerReference`, it calls `buildNavNode()` which checks the `containers` map and returns either a `StackNode` or `TabNode` depending on what's registered.

### 4.5 FlowMVI

**No FlowMVI changes needed.** The existing scoping model works correctly:

- `rememberContainer(...)` scopes per `ScreenNode.key` — each screen in a nested tab gets its own container
- `rememberSharedContainer(...)` scopes per container `NavNode.key` — each `TabNode` gets its own shared scope
- Nested containers create nested shared-container scopes, not merged scopes
- `LocalContainerNode`/`LocalScreenNode` are set per-node by renderers regardless of nesting depth

### 4.6 Validation

- `@TabItem` is **required** for ALL items in the `items` array
- `CONTAINER_REFERENCE` items must have either `@Stack` or `@Tabs` annotation
- Circular nesting detection: A→B→A → **error**
- Max depth warning: > 3 levels → **warning**
- Do NOT validate internals of referenced containers in consuming module — treat as **opaque references**

---

## 5. End-User API Examples

### 5.1 Cross-Module Stack Tab

```kotlin
// ═══════════════════════════════════════════
// feature-settings module
// ═══════════════════════════════════════════

@TabItem  // Required!
@Stack(name = "settingsStack", startDestination = SettingsDestination.Main::class)
sealed class SettingsDestination : NavDestination {
    @Destination(route = "settings/main")
    data object Main : SettingsDestination()

    @Destination(route = "settings/profile")
    data object Profile : SettingsDestination()
}

@Screen(SettingsDestination.Main::class)
@Composable
fun SettingsMainScreen(navigator: Navigator) {
    val store = rememberContainer<SettingsMainContainer, SettingsMainState,
        SettingsMainIntent, SettingsMainAction>(
        qualifier = qualifier("SettingsMainContainer")
    )
    // ...
}

// ═══════════════════════════════════════════
// app module
// ═══════════════════════════════════════════

@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, SettingsDestination::class]
)
object MainTabs : NavDestination

@TabItem
@Destination(route = "main/home")
data object HomeTab : NavDestination
```

**Resulting Nav Tree:**

```
TabNode (mainTabs)
├── StackNode (tab0) → ScreenNode (HomeTab)
└── StackNode (tab1) → StackNode (settingsStack)
                        ├── ScreenNode (Settings.Main)
                        └── (pushable: Settings.Profile)
```

### 5.2 Nested Tabs (Tabs within Tabs)

```kotlin
// ═══════════════════════════════════════════
// feature-media module
// ═══════════════════════════════════════════

@TabItem  // Required! This is a tab item for the parent
@Tabs(    // AND itself a tabs container
    name = "mediaTabs",
    initialTab = MusicTab::class,
    items = [MusicTab::class, VideosTab::class]
)
object MediaTabs : NavDestination

@TabItem
@Stack(name = "musicStack", startDestination = MusicTab.Library::class)
sealed class MusicTab : NavDestination {
    @Destination(route = "media/music/library")
    data object Library : MusicTab()

    @Destination(route = "media/music/player/{trackId}")
    data class Player(@Argument val trackId: String) : MusicTab()
}

@TabItem
@Stack(name = "videosStack", startDestination = VideosTab.Browse::class)
sealed class VideosTab : NavDestination {
    @Destination(route = "media/videos/browse")
    data object Browse : VideosTab()
}

@TabsContainer(MediaTabs::class)
@Composable
fun MediaTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    Column {
        TabRow(selectedTabIndex = scope.activeTabIndex) {
            scope.tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == scope.activeTabIndex,
                    onClick = { scope.switchTab(index) },
                    text = { Text(when(tab) {
                        is MusicTab -> "Music"
                        is VideosTab -> "Videos"
                        else -> "Tab"
                    })}
                )
            }
        }
        content()
    }
}

// ═══════════════════════════════════════════
// app module
// ═══════════════════════════════════════════

@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, MediaTabs::class, ProfileTab::class]
)
object MainTabs : NavDestination

@TabItem
@Destination(route = "main/home")
data object HomeTab : NavDestination

@TabItem
@Destination(route = "main/profile")
data object ProfileTab : NavDestination
```

**Resulting Nav Tree:**

```
TabNode (mainTabs)
├── StackNode (tab0) → ScreenNode (HomeTab)
├── StackNode (tab1) → TabNode (mediaTabs)         ← nested tabs!
│                       ├── StackNode (musicStack) → ScreenNode (Music.Library)
│                       └── StackNode (videosStack) → ScreenNode (Videos.Browse)
└── StackNode (tab2) → ScreenNode (ProfileTab)
```

### 5.3 Wrapper (Unchanged API)

```kotlin
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    NavigationBar {
        scope.tabs.forEachIndexed { index, tab ->
            val (label, icon) = when (tab) {
                is HomeTab -> "Home" to Icons.Default.Home
                is MediaTabs -> "Media" to Icons.Default.PlayArrow
                is SettingsDestination -> "Settings" to Icons.Default.Settings
                is ProfileTab -> "Profile" to Icons.Default.Person
                else -> "Tab" to Icons.Default.Circle
            }
            NavigationBarItem(
                selected = index == scope.activeTabIndex,
                onClick = { scope.switchTab(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
    content()
}
```

### 5.4 FlowMVI with Nested Containers

```kotlin
// Shared container scoped to the parent MainTabs
class MainTabsContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<MainTabsState, MainTabsIntent, MainTabsAction>(scope) {
    override val store = store(MainTabsState()) { /* ... */ }
}

// Shared container scoped to the nested MediaTabs (independent lifecycle)
class MediaTabsSharedContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<MediaTabsState, MediaTabsIntent, MediaTabsAction>(scope) {
    override val store = store(MediaTabsState()) { /* ... */ }
}

// Screen-scoped container (independent of tab nesting depth)
class MusicLibraryContainer(scope: NavigationContainerScope) :
    NavigationContainer<MusicLibraryState, MusicLibraryIntent, MusicLibraryAction>(scope) {
    override val store = store(MusicLibraryState()) { /* ... */ }
}

// DI composition
fun navigationConfig(): NavigationConfig =
    AppNavigationConfig + FeatureMediaNavigationConfig + FeatureSettingsNavigationConfig
```

**Scope hierarchy:**

```
MainTabsContainer (shared, scoped to mainTabs TabNode)
├── HomeTab screens → own screen containers
├── MediaTabsSharedContainer (shared, scoped to mediaTabs TabNode — independent!)
│   ├── MusicLibraryContainer (screen-scoped)
│   └── VideosBrowseContainer (screen-scoped)
└── ProfileTab screens → own screen containers
```

---

## 6. Task Breakdown

### Phase 1: KSP Model Changes

#### Task 1.1: Add `CONTAINER_REFERENCE` to `TabItemType` enum

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`
- **Description**: Add `CONTAINER_REFERENCE` enum value to `TabItemType`. Add KDoc describing the pattern: a `@TabItem` class with `@Stack` or `@Tabs` that is referenced from another module's `@Tabs(items = [...])`. Remove any existing `NESTED_TABS` type if present.
- **Dependencies**: None
- **Acceptance Criteria**:
  - `TabItemType` has exactly three values: `FLAT_SCREEN`, `NESTED_STACK`, `CONTAINER_REFERENCE`
  - Existing code using `FLAT_SCREEN` and `NESTED_STACK` compiles unchanged

#### Task 1.2: Verify `TabItemInfo` model needs no structural changes

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`
- **Description**: Confirm `TabItemInfo` data class does NOT need a `nestedTabInfo` field. Since `CONTAINER_REFERENCE` is treated as an opaque reference (not recursively extracted), no additional metadata is needed beyond `tabType`, `classDeclaration`, and the existing `stackInfo`/`destinationInfo` (which will be null for container references).
- **Dependencies**: Task 1.1
- **Acceptance Criteria**: No changes needed to `TabItemInfo` beyond what `CONTAINER_REFERENCE` enum addition implies

---

### Phase 2: KSP Extractor Changes

#### Task 2.1: Update `detectTabItemType()` in `TabExtractor`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Description**: Modify `detectTabItemType()` to implement the full detection matrix:
  1. Check `@TabItem` is present (required for all items — enforced in validation)
  2. If `@TabItem` + `@Destination` → `FLAT_SCREEN`
  3. If `@TabItem` + `@Stack` + is sealed member of the declaring `@Tabs` class → `NESTED_STACK`
  4. If `@TabItem` + `@Stack` + is NOT a sealed member (cross-module / standalone) → `CONTAINER_REFERENCE`
  5. If `@TabItem` + `@Tabs` → `CONTAINER_REFERENCE`
  6. Otherwise → error (handled by validation)
  
  The "sealed member" check can use `classDeclaration.parentDeclaration` to determine if the class is nested within the `@Tabs` sealed class.
- **Dependencies**: Task 1.1
- **Acceptance Criteria**:
  - Cross-module `@Stack` with `@TabItem` detected as `CONTAINER_REFERENCE`
  - Local sealed `@Stack` with `@TabItem` still detected as `NESTED_STACK`
  - `@Tabs` with `@TabItem` detected as `CONTAINER_REFERENCE`
  - `@Destination` with `@TabItem` still detected as `FLAT_SCREEN`

#### Task 2.2: Update `extractTabItemNewPattern()` for `CONTAINER_REFERENCE`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Description**: When `detectTabItemType()` returns `CONTAINER_REFERENCE`, skip local destination/stack extraction. Create a `TabItemInfo` with:
  - `tabType = CONTAINER_REFERENCE`
  - `classDeclaration` = the referenced class
  - `destinationInfo = null`
  - `stackInfo = null` (do NOT attempt to extract stack info from the referenced container)
  
  The referenced container's internals are not extracted — they belong to the producing module's config.
- **Dependencies**: Task 2.1
- **Acceptance Criteria**:
  - `CONTAINER_REFERENCE` items produce a valid `TabItemInfo` without attempting local extraction
  - No KSP errors when the referenced class's destinations are in another compilation unit

#### Task 2.3: Ensure `extractFromItemsArray()` handles cross-module types

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Description**: Verify that `extractFromItemsArray()` correctly resolves `KSType` references from other modules when iterating the `items` array. The `KClass<*>` in the annotation should already resolve cross-module, but confirm that `KSClassDeclaration` retrieval works for classes compiled in other KSP rounds/modules.
- **Dependencies**: Task 2.2
- **Acceptance Criteria**: Cross-module class references in `items` array resolve to valid `KSClassDeclaration` instances

---

### Phase 3: KSP Generator Changes

#### Task 3.1: Update `ContainerBlockGenerator.generateTabEntry()` for `CONTAINER_REFERENCE`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`
- **Description**: Add `CONTAINER_REFERENCE` case in the `when` block that generates tab entries. Output `containerTab<ContainerType>()` — the same runtime DSL call used for referenced containers.
  
  ```kotlin
  CONTAINER_REFERENCE -> CodeBlock.of("containerTab<%T>()\n", tabClassName)
  ```
- **Dependencies**: Task 1.1, Task 2.2
- **Acceptance Criteria**:
  - `CONTAINER_REFERENCE` generates `containerTab<SettingsDestination>()` for a cross-module stack
  - `CONTAINER_REFERENCE` generates `containerTab<MediaTabs>()` for a nested tabs container
  - `FLAT_SCREEN` and `NESTED_STACK` generation unchanged

#### Task 3.2: Register referenced containers in `NavigationConfigGenerator`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/NavigationConfigGenerator.kt`
- **Description**: When processing a `TabInfo` whose tab items contain `CONTAINER_REFERENCE` entries, ensure the referenced container class is collected and registered in the generated config. This is critical so that `buildNavNode()` can resolve the container at runtime.
  
  For cross-module references where the container is defined in another module: the referenced container's config is expected to be composed via `CompositeNavigationConfig` (`+` operator). The **consuming** module's generated config should emit the `containerTab<T>()` DSL call but does **not** need to re-declare the container's internal structure. The producing module handles that.
  
  For same-module nested `@Tabs` references: ensure both the parent `tabs<MainTabs>` and the child `tabs<MediaTabs>` blocks are generated.
- **Dependencies**: Task 3.1
- **Acceptance Criteria**:
  - Generated config for the consuming module includes `containerTab<T>()` calls
  - Generated config for the producing module includes the full container definition
  - `CompositeNavigationConfig` resolves cross-module container references correctly

#### Task 3.3: Update collector in `QuoVadisSymbolProcessor`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt`
- **Description**: When a class is annotated with both `@TabItem` and `@Tabs` (or `@TabItem` and `@Stack` as a non-sealed-member), ensure it is processed both:
  1. As a tab item for its parent `@Tabs` container
  2. As an independent container in its own right (generates its own container block in the config)
  
  This may require adjusting the collection pass to recognize `@TabItem` + `@Tabs` classes and include them in the tabs container processing pipeline.
- **Dependencies**: Task 2.2, Task 3.2
- **Acceptance Criteria**:
  - A `@TabItem` + `@Tabs` class produces both a tab item entry in the parent AND its own tabs container block
  - A `@TabItem` + `@Stack` class that's a container reference produces a tab item entry in the parent; the stack's own container block comes from its own module's processor

---

### Phase 4: Validation Changes

#### Task 4.1: Validate `@TabItem` requirement on all items

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Description**: Add validation that every class in `@Tabs(items = [...])` must have the `@TabItem` annotation. Emit a clear error if missing:
  
  > "Class `{ClassName}` is listed in @Tabs(items = [...]) but is missing @TabItem annotation. All tab items require @TabItem."
- **Dependencies**: Phase 2 (extraction must handle all types first)
- **Acceptance Criteria**: Classes without `@TabItem` in the items array produce a compile-time error

#### Task 4.2: Validate `CONTAINER_REFERENCE` items

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Description**: For items detected as `CONTAINER_REFERENCE`, validate that they have either `@Stack` or `@Tabs` annotation. Do NOT validate internals of the referenced container (treat as opaque).
  
  Error message if neither annotation present:
  > "Container reference `{ClassName}` must be annotated with either @Stack or @Tabs."
- **Dependencies**: Task 4.1
- **Acceptance Criteria**:
  - `@TabItem` + `@Stack` (cross-module) passes
  - `@TabItem` + `@Tabs` passes
  - `@TabItem` without `@Stack`/`@Tabs`/`@Destination` fails with clear error

#### Task 4.3: Accept `@TabItem` + `@Tabs` as valid combination

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Description**: Update any existing validation that rejects `@TabItem` combined with `@Tabs`. The combination is now valid and represents a container reference.
- **Dependencies**: Task 4.1
- **Acceptance Criteria**: `@TabItem` + `@Tabs` does not produce a validation error

#### Task 4.4: Add circular nesting detection

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Description**: Detect circular tab nesting where container A references B, and B references A (directly or transitively). Implement as a graph cycle detection during validation pass.
  
  Error message:
  > "Circular tab nesting detected: {A} → {B} → {A}. Tab containers cannot reference each other cyclically."
- **Dependencies**: Task 4.2
- **Acceptance Criteria**:
  - A→B→A produces compile-time error with clear cycle path
  - A→B→C (no cycle) passes
  - A→B→C→A produces error

#### Task 4.5: Add max depth warning

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Description**: Warn when tab nesting exceeds 3 levels deep. This is a warning, not an error.
  
  Warning message:
  > "Tab nesting depth exceeds 3 levels at `{ClassName}`. Deep nesting may cause usability issues."
- **Dependencies**: Task 4.4
- **Acceptance Criteria**: Nesting > 3 levels produces a warning; ≤ 3 levels produces no warning

---

### Phase 5: Runtime Verification

#### Task 5.1: Verify `DslNavigationConfig.buildTabStack()` handles `TabNode` return

- **File**: `quo-vadis-core/src/.../navigation/config/DslNavigationConfig.kt`
- **Description**: The `TabEntry.ContainerReference` path calls `buildNavNode()` which returns either `StackNode` or `TabNode`. Verify this already works correctly for the tabs-within-tabs case (i.e., a `TabNode` returned and correctly placed inside the parent `TabNode`'s stack). **No code changes expected** — this is a verification task.
- **Dependencies**: None (can start independently)
- **Acceptance Criteria**: Write a unit test that builds a tab container with a `ContainerReference` pointing to another tabs container, confirming the resulting tree has `TabNode → StackNode → TabNode`

#### Task 5.2: Verify `CompositeNavigationConfig` cross-module resolution

- **File**: `quo-vadis-core/src/.../navigation/config/CompositeNavigationConfig.kt`
- **Description**: Verify that config composition (`config1 + config2`) correctly resolves a container reference from config1 using a container defined in config2. **No code changes expected.**
- **Dependencies**: None (can start independently)
- **Acceptance Criteria**: Write a unit test composing two configs where one references a container from the other

---

### Phase 6: Tests

#### Task 6.1: KSP tests — `CONTAINER_REFERENCE` detection

- **New test file(s)** in `quo-vadis-ksp/src/test/`
- **Description**:
  - Test: `@TabItem` + `@Stack` (non-sealed-member) → detected as `CONTAINER_REFERENCE`
  - Test: `@TabItem` + `@Tabs` → detected as `CONTAINER_REFERENCE`
  - Test: `@TabItem` + `@Stack` (sealed member) → still detected as `NESTED_STACK`
  - Test: `@TabItem` + `@Destination` → still detected as `FLAT_SCREEN`
- **Dependencies**: Phase 2
- **Acceptance Criteria**: All detection tests pass

#### Task 6.2: KSP tests — cross-module tab items

- **New test file(s)** in `quo-vadis-ksp/src/test/`
- **Description**:
  - Test: `@Tabs` with `items` array referencing a class that simulates cross-module origin
  - Verify generated config contains `containerTab<T>()` for the reference
- **Dependencies**: Phase 3
- **Acceptance Criteria**: Generated DSL code matches expected output

#### Task 6.3: KSP tests — code generation for `CONTAINER_REFERENCE`

- **New test file(s)** in `quo-vadis-ksp/src/test/`
- **Description**:
  - Test: Generated config for a `@Tabs` with mixed item types (`FLAT_SCREEN`, `NESTED_STACK`, `CONTAINER_REFERENCE`)
  - Verify each type generates the correct DSL call
  - Verify nested `@Tabs` containers are registered as independent container blocks
- **Dependencies**: Phase 3
- **Acceptance Criteria**: Generated code output matches expected DSL for all item types

#### Task 6.4: Validation tests — `@TabItem` requirement

- **New test file(s)** in `quo-vadis-ksp/src/test/`
- **Description**:
  - Test: Class in `items` without `@TabItem` → error
  - Test: `@TabItem` + `@Tabs` accepted
  - Test: `@TabItem` without `@Destination`/`@Stack`/`@Tabs` → error
- **Dependencies**: Phase 4
- **Acceptance Criteria**: Validation errors/successes match expected behavior

#### Task 6.5: Validation tests — circular nesting detection

- **New test file(s)** in `quo-vadis-ksp/src/test/`
- **Description**:
  - Test: A→B→A circular → error
  - Test: A→B→C→A circular → error
  - Test: A→B→C (no cycle) → passes
  - Test: Nesting depth > 3 → warning
  - Test: Nesting depth ≤ 3 → no warning
- **Dependencies**: Phase 4
- **Acceptance Criteria**: Cycle detection and depth warnings work correctly

#### Task 6.6: Runtime tests — nested `TabNode` building

- **New test file(s)** in `quo-vadis-core/src/.../test/`
- **Description**:
  - Test: `buildNavNode` for a tabs container with `ContainerReference` pointing to another tabs container
  - Verify resulting tree: `TabNode → StackNode → TabNode → StackNodes`
  - Test: `CompositeNavigationConfig` resolving cross-config container references
- **Dependencies**: Phase 5
- **Acceptance Criteria**: Nav tree structure matches expected nesting

---

### Phase 7: Demo App & Documentation

#### Task 7.1: Add cross-module tab demo

- **Files**: `feature1/` or `feature2/` destinations, `composeApp/` MainTabs
- **Description**: Move or create a `@TabItem` + `@Stack` definition in `feature1`, reference it from `MainTabs` in `composeApp` via the `items` array. Verify compile + run on Desktop and Android.
- **Dependencies**: Phases 1–4
- **Acceptance Criteria**: Demo app compiles and runs with a cross-module stack tab

#### Task 7.2: Add nested tabs demo (optional, depends on scope)

- **Files**: Feature module with `@TabItem` + `@Tabs`, `composeApp/` referencing it
- **Description**: Create a nested `@Tabs` container in a feature module and reference it as a tab item in the main tabs. Include a `@TabsContainer` wrapper for the inner tabs.
- **Dependencies**: Task 7.1
- **Acceptance Criteria**: Demo shows tabs-within-tabs with independent navigation stacks

#### Task 7.3: Update documentation

- **Files**: `docs/ANNOTATIONS.md`, optionally `docs/ARCHITECTURE.md`
- **Description**: Document:
  - `@TabItem` requirement for all items in `@Tabs(items = [...])`
  - Cross-module stack tab pattern
  - Nested tabs pattern
  - `CONTAINER_REFERENCE` tab item type
  - FlowMVI scope behavior with nested containers
- **Dependencies**: Phases 1–6
- **Acceptance Criteria**: Documentation covers all new patterns with examples

---

## 7. Sequencing & Dependencies

```
Phase 1: Models ──────────────────────────────────┐
    │                                              │
    ▼                                              │
Phase 2: Extractors                                │
    │                                              │
    ▼                                              │
Phase 3: Generators                     Phase 5: Runtime Verification
    │                                   (can start independently)
    ▼                                              │
Phase 4: Validation                                │
    │                                              │
    ├──────────────────────────────────────────────┤
    ▼                                              ▼
Phase 6: Tests (spans all prior phases)
    │
    ▼
Phase 7: Demo & Documentation
```

**Key dependency notes:**

- Phases 1→2→3→4 are strictly sequential (each depends on the prior)
- Phase 5 (runtime verification) can start independently at any time — it verifies existing behavior
- Phase 6 tests span all prior phases; individual test tasks can start as their dependency phases complete
- Phase 7 is integration-level and should come last

---

## 8. Files Affected

| File | Phase | Change |
|------|-------|--------|
| `quo-vadis-ksp/.../models/TabInfo.kt` | 1 | Add `CONTAINER_REFERENCE` to `TabItemType` enum |
| `quo-vadis-ksp/.../extractors/TabExtractor.kt` | 2 | Update detection + extraction for `CONTAINER_REFERENCE` |
| `quo-vadis-ksp/.../generators/dsl/ContainerBlockGenerator.kt` | 3 | Handle `CONTAINER_REFERENCE` in tab entry generation |
| `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt` | 3 | Register referenced containers in generated config |
| `quo-vadis-ksp/.../QuoVadisSymbolProcessor.kt` | 3 | Collect container references as containers |
| `quo-vadis-ksp/.../validation/ValidationEngine.kt` | 4 | Validate `@TabItem` requirement, `CONTAINER_REFERENCE`, circular detection, depth warning |
| `quo-vadis-core/.../config/DslNavigationConfig.kt` | 5 | Verify only (no changes expected) |
| `quo-vadis-core/.../config/CompositeNavigationConfig.kt` | 5 | Verify only (no changes expected) |
| Test files (new) | 6 | KSP detection, generation, validation, runtime tests |
| `composeApp/` / `feature1/` / `feature2/` | 7 | Demo integration |
| `docs/ANNOTATIONS.md` | 7 | Document new patterns |

---

## 9. FlowMVI Semantics

The existing scoping model requires no changes but benefits from documentation of the nesting behavior:

| Scope Type | Keyed By | Behavior with Nesting |
|------------|----------|----------------------|
| `rememberContainer(...)` | `ScreenNode.key` | Each screen gets its own container regardless of nesting depth |
| `rememberSharedContainer(...)` | Container `NavNode.key` | Each `TabNode`/`PaneNode` gets its own independent shared scope |

**Example scope hierarchy for nested tabs:**

```
MainTabs TabNode
├── MainTabsContainer (shared, scoped to mainTabs key)
│
├── HomeTab ScreenNode
│   └── HomeContainer (screen-scoped)
│
├── MediaTabs TabNode
│   ├── MediaTabsSharedContainer (shared, scoped to mediaTabs key — INDEPENDENT from MainTabsContainer)
│   │
│   ├── MusicStack
│   │   └── MusicLibrary ScreenNode
│   │       └── MusicLibraryContainer (screen-scoped)
│   └── VideosStack
│       └── VideosBrowse ScreenNode
│           └── VideosBrowseContainer (screen-scoped)
│
└── ProfileTab ScreenNode
    └── ProfileContainer (screen-scoped)
```

Key invariants:
- `MainTabsContainer` and `MediaTabsSharedContainer` are **independent** — not merged
- Each container owns its own lifecycle
- Screen containers are always scoped to their individual `ScreenNode`, regardless of how deep the tab nesting is
- Destroying a tab (e.g., MediaTabs gets removed) destroys its shared container and all child screen containers

---

## 10. Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Validation reintroduces cross-module false failures | Medium | Medium | Treat referenced containers as opaque — do not validate their internals in the consuming module |
| Deep nesting infinite recursion in KSP extraction | High | Low | `CONTAINER_REFERENCE` is opaque — no recursive extraction. Circular detection in validation prevents runtime loops |
| Generated code ordering — parent references container before it's registered | Medium | Medium | Rely on runtime lazy resolution via `buildNavNode()` + `CompositeNavigationConfig`; emit container blocks before parent blocks when possible |
| Back navigation behavior with nested tabs | Medium | Medium | Existing back navigation traverses the tree depth-first; verify with integration tests in Phase 6 |
| `TabsContainerScope.tabs` type matching for cross-module types | Low | Low | Already uses `NavDestination` instances; `when` pattern matching works across modules with proper imports |
| FlowMVI nested shared containers misunderstood as duplicate state | Low | Low | Document lifecycle boundaries and scope separation in Phase 7 |
| KSP multi-round processing with cross-module types | Low | Low | `KClass<*>` in `items` array already resolves cross-module; `KSType` retrieval is standard KSP behavior |

---

## 11. Verification Criteria

### Build Verification
- [ ] `./gradlew build` passes for all modules
- [ ] Generated configs compile for both producing and consuming modules
- [ ] `CompositeNavigationConfig` (`config1 + config2`) resolves cross-module container references

### Runtime Verification
- [ ] Navigation to a destination inside a referenced container creates the correct parent tab container
- [ ] Nested tabs render with independent navigation stacks
- [ ] Back navigation works correctly through nested tab boundaries
- [ ] Tab switching preserves back stacks at each nesting level

### FlowMVI Verification
- [ ] Parent and nested `rememberSharedContainer(...)` scopes are distinct
- [ ] Each screen resolves its own `rememberContainer(...)` scope
- [ ] Container lifecycle cleanup works when tabs are destroyed

### KSP Verification
- [ ] `CONTAINER_REFERENCE` detection works for both `@Stack` and `@Tabs` references
- [ ] `NESTED_STACK` detection unchanged for local sealed members
- [ ] Circular nesting produces compile-time error
- [ ] Missing `@TabItem` produces compile-time error
- [ ] Depth > 3 produces warning

---

## 12. Open Questions

| Question | Current Decision | Notes |
|----------|-----------------|-------|
| Allow `@Pane` containers in tabs? | Deferred | Intentionally scoping to `@Stack` and `@Tabs` only for now. Can be added later with the same `CONTAINER_REFERENCE` pattern. |
| `initialTab` targeting a container reference? | Allowed | The `initialTab` KClass in `@Tabs` should accept a `CONTAINER_REFERENCE` class. `buildNavNode()` resolves it at runtime. |

---

## Summary

This plan adds **one new enum value** (`CONTAINER_REFERENCE`) to the KSP model and updates the extraction, generation, and validation pipelines to handle it. The runtime requires **no changes** — existing `containerTab<T>()`, `TabEntry.ContainerReference`, and `CompositeNavigationConfig` already support the pattern. The work is organized into 7 phases with 20 tasks, progressing from models through extractors, generators, validation, runtime verification, tests, and demo/documentation.
