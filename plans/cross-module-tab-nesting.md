# Cross-Module Tab Nesting Implementation Plan

> **GitHub Issue**: #41 — "Nesting TabContainer in TabContainer of different module"
> **Created**: 2026-03-12
> **Status**: ⚠️ **Superseded** — The annotation model described here (`@Tabs(items=..., initialTab=...)`) was replaced by the child-to-parent pattern: `@Tabs(name)` + `@TabItem(parent, ordinal)`. See `plans/tab-child-to-parent-reversal.md` for the implemented design.

## Problem Statement

The Quo Vadis navigation library's `@Tabs` container currently requires all tab items to be defined within the same module. Issue #41 requests:

1. **Cross-module tab items**: Define `@TabItem` + `@Stack` in a feature module, reference from `@Tabs(items = [...])` in the app module.
2. **Tabs-within-tabs**: A `@TabItem` that is itself a `@Tabs` container (nested `TabNode` inside a parent `TabNode`).

The user confirmed:
- Pure annotation-based approach — KSP resolves cross-module references
- Both `@Stack` AND `@Tabs` types supported as `@TabItem`
- Scope limited to tabs (panes as future follow-up)

---

## Current Architecture

### Annotations (`quo-vadis-annotations`)

| Annotation | Purpose |
|------------|---------|
| `@Tabs(name, initialTab, items: Array<KClass<*>>)` | Marks a tab container |
| `@TabItem` | Marks a class as a tab item |
| `@Stack(name, startDestination)` | Marks a sealed class as a navigation stack |
| `@Destination(route)` | Marks individual screen destinations |
| `@Screen(destination)` | Binds composable to destination |
| `@TabsContainer(KClass)` | Binds composable wrapper for tab container |

### KSP Processing (`quo-vadis-ksp`)

- **`TabExtractor`** supports "new pattern" where `@Tabs(items = [...])` references external `@TabItem` classes
- `extractFromItemsArray()` resolves each `KSType` from the items array and calls `extractTabItemNewPattern()`
- `detectTabItemType()` checks for `@Stack` → `NESTED_STACK`, `@Destination` → `FLAT_SCREEN`
- **Missing**: No detection for `@Tabs` on a TabItem (tabs-within-tabs)

### Runtime DSL (`quo-vadis-core`)

- `TabsBuilder` has `tab()`, `containerTab()` methods
- `TabEntry` sealed class: `FlatScreen`, `NestedStack`, `ContainerReference`
- `DslNavigationConfig.buildTabStack()` handles `ContainerReference` by calling `buildNavNode()` recursively
- `CompositeNavigationConfig` merges configs from multiple modules via `+` operator

### Current Multi-Module Pattern

```kotlin
// feature1/ResultDemoDestination.kt
@Stack(name = "result_demo", startDestination = ResultDemoDestination.Demo::class)
sealed class ResultDemoDestination : NavDestination { ... }

// composeApp/DI.kt
fun navigationConfig(): NavigationConfig =
    ComposeAppNavigationConfig + Feature1NavigationConfig + Feature2NavigationConfig
```

Feature modules define their own destinations and generate their own `*NavigationConfig`. The app module composes them. But currently, you **cannot** reference a feature module's `@TabItem`/`@Stack` from the app module's `@Tabs(items = [...])`.

### The Real Problem

The issue is NOT in KSP resolution (the `items` array already uses `KClass<*>` which works cross-module). The real problems are:

1. **`TabItemType` detection**: `detectTabItemType()` only checks for `@Stack` and `@Destination`. It does **not** handle a `@TabItem` that is also a `@Tabs` container (tabs-within-tabs).

2. **Generated code structure**: When a `@TabItem` is a `@Tabs` container, the generated code needs to build a `TabNode` inside the parent `TabNode`'s stack, not a `StackNode`.

3. **Multi-module config merging**: When `SettingsTab` is defined in `feature-settings` module with its own `Feature_SettingsNavigationConfig`, and referenced in `MainTabs` in the app module, the `CompositeNavigationConfig` must correctly resolve the nested container when building the tab node.

4. **FlowMVI container scoping**: Each screen in a cross-module tab needs its own MVI container. Nested tab containers may need their own `SharedNavigationContainer`. The existing `SharedContainerScope` needs to work correctly for nested `TabNode` hierarchies.

---

## Solution Design

### Phase 1: Annotation Layer Changes

**No annotation changes needed** for `@Tabs`, `@TabItem`, `@Stack`. The existing `items: Array<KClass<*>>` already supports cross-module class references. The `@TabItem` annotation already works on any class.

The only addition: Support `@Tabs` + `@TabItem` on the same class (for tabs-within-tabs). Currently `@TabItem` is only expected with `@Stack` or `@Destination`.

### Phase 2: KSP Processor Changes

#### 2.1 Add `NESTED_TABS` to `TabItemType`

```kotlin
// quo-vadis-ksp/.../models/TabInfo.kt
enum class TabItemType {
    FLAT_SCREEN,
    NESTED_STACK,
    NESTED_TABS  // NEW: Tab item that is itself a @Tabs container
}
```

#### 2.2 Update `detectTabItemType()` in `TabExtractor`

```kotlin
private fun detectTabItemType(classDeclaration: KSClassDeclaration): TabItemType {
    val hasStack = classDeclaration.annotations.any {
        it.shortName.asString() == "Stack"
    }
    val hasTabs = classDeclaration.annotations.any {
        it.shortName.asString() == "Tab" || it.shortName.asString() == "Tabs"
    }
    val hasDestination = classDeclaration.annotations.any {
        it.shortName.asString() == "Destination"
    }

    return when {
        hasTabs -> TabItemType.NESTED_TABS
        hasStack -> TabItemType.NESTED_STACK
        hasDestination -> TabItemType.FLAT_SCREEN
        else -> TabItemType.FLAT_SCREEN
    }
}
```

#### 2.3 Update `extractTypeSpecificInfo()` in `TabExtractor`

Add handling for `NESTED_TABS` that extracts the `TabInfo` for the nested tabs container:

```kotlin
TabItemType.NESTED_TABS -> {
    val nestedTabInfo = extract(classDeclaration) // recursive call
    if (nestedTabInfo == null) {
        logger.warn("NESTED_TABS TabItem extraction failed", classDeclaration)
    }
    null to null // destInfo, stackInfo unused — nestedTabInfo stored separately
}
```

#### 2.4 Update `TabItemInfo` model

```kotlin
data class TabItemInfo(
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType = TabItemType.FLAT_SCREEN,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
    val nestedTabInfo: TabInfo? = null  // NEW: for NESTED_TABS type
)
```

#### 2.5 Update `ContainerBlockGenerator`

Generate `containerTab<Type>()` for `NESTED_TABS` same as `NESTED_STACK` (the runtime resolves through `buildNavNode` which checks both stack and tabs containers):

```kotlin
TabItemType.NESTED_TABS -> {
    CodeBlock.of("containerTab<%T>()\n", tabClassName)
}
```

#### 2.6 Update `NavigationConfigGenerator`

When processing `TabInfo`, if a tab item is `NESTED_TABS`, ensure the nested `TabInfo` is also registered as a container in the generated config (so `buildNavNode()` can resolve it).

### Phase 3: Runtime DSL Changes

**No runtime changes needed.**

The existing `TabEntry.ContainerReference` already handles this case. When `buildTabStack` encounters a `ContainerReference`, it calls `buildNavNode()` which checks the `containers` map. If the referenced class is a `@Tabs` container, it builds a `TabNode`. If it's a `@Stack`, it builds a `StackNode`.

Key insight: `DslNavigationConfig.buildTabStack()` already handles `TabEntry.ContainerReference` by calling `buildNavNode(tabEntry.containerClass, stackKey, tabNodeKey)`, which returns either a `StackNode` or `TabNode` depending on what's registered. And `CompositeNavigationConfig.buildNavNode()` tries secondary first, then primary — so cross-module resolution works through config composition.

### Phase 4: FlowMVI Integration

**No changes needed** to the FlowMVI module itself. The existing patterns work:

- **Screen-scoped containers**: Each `@Screen` in the nested tab gets its own `NavigationContainer` via `rememberContainer`. This already works because `LocalScreenNode` is set per screen regardless of nesting depth.

- **Shared containers for nested tabs**: The nested `@Tabs` container can have its own `SharedNavigationContainer` via `rememberSharedContainer`. This works because `LocalContainerNode` is set per `TabNode`/`PaneNode` by the renderer.

### Phase 5: Validation

Update `ValidationEngine` to:
- Accept `@TabItem` + `@Tabs` combination as valid
- Validate that nested `@Tabs` items are themselves valid tab containers
- Warn about circular tab nesting dependencies

---

## End-User API Examples

### Example 1: Settings Tab as Cross-Module Stack

```kotlin
// ═══════════════════════════════════════════════════════════════
// feature-settings module
// ═══════════════════════════════════════════════════════════════

// feature-settings/src/.../SettingsDestination.kt
@TabItem
@Stack(name = "settingsStack", startDestination = SettingsDestination.Main::class)
sealed class SettingsDestination : NavDestination {
    @Destination(route = "settings/main")
    data object Main : SettingsDestination()

    @Destination(route = "settings/profile")
    data object Profile : SettingsDestination()

    @Destination(route = "settings/notifications")
    data object Notifications : SettingsDestination()

    @Destination(route = "settings/about")
    data object About : SettingsDestination()
}

// feature-settings/src/.../screens/SettingsMainScreen.kt
@Screen(SettingsDestination.Main::class)
@Composable
fun SettingsMainScreen(navigator: Navigator) {
    val store = rememberContainer<SettingsMainContainer, SettingsMainState,
        SettingsMainIntent, SettingsMainAction>(
        qualifier = qualifier("SettingsMainContainer")
    )
    with(store) {
        val state by subscribe()
        SettingsMainContent(state = state, onIntent = { intent(it) })
    }
}

// feature-settings/src/.../containers/SettingsMainContainer.kt
class SettingsMainContainer(scope: NavigationContainerScope) :
    NavigationContainer<SettingsMainState, SettingsMainIntent, SettingsMainAction>(scope) {

    override val store = store(SettingsMainState()) {
        reduce { intent ->
            when (intent) {
                is SettingsMainIntent.NavigateToProfile -> {
                    navigator.navigate(SettingsDestination.Profile)
                    state
                }
                is SettingsMainIntent.NavigateToNotifications -> {
                    navigator.navigate(SettingsDestination.Notifications)
                    state
                }
                // ...
            }
        }
    }
}

// feature-settings/src/.../Di.kt (Koin)
val settingsModule = module {
    navigationContainer<SettingsMainContainer>(qualifier("SettingsMainContainer")) { scope ->
        SettingsMainContainer(scope)
    }
    // ... other screen containers
}
```

```kotlin
// ═══════════════════════════════════════════════════════════════
// app module
// ═══════════════════════════════════════════════════════════════

// app/src/.../MainTabs.kt
@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class, ProfileTab::class, SettingsDestination::class]
    //                                                              ^^^^^^^^^^^^^^^^^^^^^^^^^^
    //                                          Cross-module reference! Defined in feature-settings
)
sealed class MainTabs : NavDestination {
    companion object : NavDestination

    @TabItem
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem
    @Stack(name = "exploreStack", startDestination = ExploreTab.Feed::class)
    sealed class ExploreTab : MainTabs() {
        @Destination(route = "explore/feed")
        data object Feed : ExploreTab()
    }

    @TabItem
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()
}

// app/src/.../MainTabsWrapper.kt
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabs.forEachIndexed { index, tab ->
                    val (label, icon) = when (tab) {
                        is MainTabs.HomeTab -> "Home" to Icons.Default.Home
                        is MainTabs.ExploreTab -> "Explore" to Icons.Default.Explore
                        is MainTabs.ProfileTab -> "Profile" to Icons.Default.Person
                        is SettingsDestination -> "Settings" to Icons.Default.Settings
                        //  ^^^^^^^^^^^^^^^^^^ cross-module type matching works!
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
        }
    ) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }
}

// app/src/.../DI.kt
fun navigationConfig(): NavigationConfig =
    AppNavigationConfig + FeatureSettingsNavigationConfig
    // CompositeNavigationConfig resolves SettingsDestination from feature module's config
```

### Example 2: Nested Tabs (Tabs within Tabs)

```kotlin
// ═══════════════════════════════════════════════════════════════
// feature-media module
// ═══════════════════════════════════════════════════════════════

// feature-media/src/.../MediaTabs.kt
@TabItem  // <-- This is a tab item for the parent
@Tabs(    // <-- AND itself a tabs container with sub-tabs
    name = "mediaTabs",
    initialTab = MusicTab::class,
    items = [MusicTab::class, VideosTab::class, PodcastsTab::class]
)
sealed class MediaTabs : NavDestination {
    companion object : NavDestination
}

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

@TabItem
@Stack(name = "podcastsStack", startDestination = PodcastsTab.Feed::class)
sealed class PodcastsTab : NavDestination {
    @Destination(route = "media/podcasts/feed")
    data object Feed : PodcastsTab()
}

// feature-media/src/.../MediaTabsWrapper.kt
@TabsContainer(MediaTabs::class)
@Composable
fun MediaTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    // Inner tab bar (e.g., top tab strip within the "Media" parent tab)
    Column {
        TabRow(selectedTabIndex = scope.activeTabIndex) {
            scope.tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = index == scope.activeTabIndex,
                    onClick = { scope.switchTab(index) },
                    text = { Text(when(tab) {
                        is MusicTab -> "Music"
                        is VideosTab -> "Videos"
                        is PodcastsTab -> "Podcasts"
                        else -> "Tab"
                    })}
                )
            }
        }
        content()
    }
}

// feature-media/src/.../screens/MusicLibraryScreen.kt
@Screen(MusicTab.Library::class)
@Composable
fun MusicLibraryScreen(navigator: Navigator) {
    val store = rememberContainer<MusicLibraryContainer, MusicLibraryState,
        MusicLibraryIntent, MusicLibraryAction>(
        qualifier = qualifier("MusicLibraryContainer")
    )
    with(store) {
        val state by subscribe()
        MusicLibraryContent(state = state, onIntent = { intent(it) })
    }
}

// feature-media/src/.../containers/MusicLibraryContainer.kt
class MusicLibraryContainer(scope: NavigationContainerScope) :
    NavigationContainer<MusicLibraryState, MusicLibraryIntent, MusicLibraryAction>(scope) {

    override val store = store(MusicLibraryState()) {
        reduce { intent ->
            when (intent) {
                is MusicLibraryIntent.PlayTrack -> {
                    navigator.navigate(MusicTab.Player(intent.trackId))
                    state
                }
                // ...
            }
        }
    }
}

// Shared container for the entire Media tabs section
class MediaTabsSharedContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<MediaTabsState, MediaTabsIntent, MediaTabsAction>(scope) {

    override val store = store(MediaTabsState()) {
        reduce { intent ->
            when (intent) {
                is MediaTabsIntent.RefreshAll -> {
                    updateState { copy(isRefreshing = true) }
                }
                // ...
            }
        }
    }
}
```

```kotlin
// ═══════════════════════════════════════════════════════════════
// app module
// ═══════════════════════════════════════════════════════════════

// app/src/.../MainTabs.kt
@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, MediaTabs::class, ProfileTab::class]
    //                       ^^^^^^^^^^^^^^^
    //   Cross-module tabs-within-tabs! MediaTabs is itself a @Tabs container
)
sealed class MainTabs : NavDestination {
    companion object : NavDestination

    @TabItem
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()
}

// app/src/.../MainTabsWrapper.kt
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(scope: TabsContainerScope, content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabs.forEachIndexed { index, tab ->
                    val (label, icon) = when (tab) {
                        is MainTabs.HomeTab -> "Home" to Icons.Default.Home
                        is MediaTabs -> "Media" to Icons.Default.PlayArrow
                        is MainTabs.ProfileTab -> "Profile" to Icons.Default.Person
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
        }
    ) { padding ->
        Box(Modifier.padding(padding)) { content() }
    }
}

// app/src/.../DI.kt
fun navigationConfig(): NavigationConfig =
    AppNavigationConfig + FeatureMediaNavigationConfig
```

**Resulting Nav Tree:**

```
TabNode (mainTabs)
├── StackNode (tab0) → ScreenNode (HomeTab)
├── StackNode (tab1) → TabNode (mediaTabs)         ← nested tabs!
│                       ├── StackNode (musicStack) → ScreenNode (Music.Library)
│                       ├── StackNode (videosStack) → ScreenNode (Videos.Browse)
│                       └── StackNode (podcastsStack) → ScreenNode (Podcasts.Feed)
└── StackNode (tab2) → ScreenNode (ProfileTab)
```

---

## Task Breakdown

### Phase 1: KSP Model Changes

#### Task 1.1: Add `NESTED_TABS` to `TabItemType` enum

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`
- **Description**: Add `NESTED_TABS` enum value with KDoc describing the pattern (`@TabItem` + `@Tabs`)
- **Acceptance**: Enum compiles with three values

#### Task 1.2: Add `nestedTabInfo` field to `TabItemInfo`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`
- **Description**: Add optional `nestedTabInfo: TabInfo? = null` property. Update KDoc to document the `NESTED_TABS` pattern.
- **Acceptance**: Data class compiles, existing code unaffected (default value is null)

### Phase 2: KSP Extractor Changes

#### Task 2.1: Update `detectTabItemType()` in `TabExtractor`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Description**: Add check for `@Tabs`/`@Tab` annotation **before** `@Stack` check. A class with `@TabItem` + `@Tabs` returns `NESTED_TABS`.
- **Acceptance**: A class with both `@TabItem` and `@Tabs` is detected as `NESTED_TABS`

#### Task 2.2: Update `extractTypeSpecificInfo()` in `TabExtractor`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Description**: Add `NESTED_TABS` case that calls `extract()` recursively to get the `TabInfo` for the nested container. Return the `TabInfo` as `nestedTabInfo`.
- **Risk**: Potential infinite recursion if circular nesting. Add depth guard (max 3 levels).
- **Acceptance**: `TabItemInfo.nestedTabInfo` is populated correctly for `@TabItem` + `@Tabs` classes

#### Task 2.3: Update `extractTabItemNewPattern()` to pass `nestedTabInfo`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`
- **Description**: Pass `nestedTabInfo` from `extractTypeSpecificInfo()` through to the `TabItemInfo` constructor.
- **Acceptance**: `TabItemInfo` includes `nestedTabInfo` when applicable

### Phase 3: KSP Generator Changes

#### Task 3.1: Update `ContainerBlockGenerator.generateTabEntry()`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`
- **Description**: Add `NESTED_TABS` case in `when` block that generates `containerTab<Type>()` — same as `NESTED_STACK`.
- **Acceptance**: `NESTED_TABS` generates `containerTab<MediaTabs>()` in the DSL block

#### Task 3.2: Update `NavigationConfigGenerator` to register nested tabs containers

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/NavigationConfigGenerator.kt`
- **Description**: When processing a `TabInfo` whose tab items contain `NESTED_TABS` entries, ensure the nested tab container is **also** registered as a container block in the generated config. This is critical so that `buildNavNode()` can resolve it at runtime.
- **Acceptance**: Generated `NavigationConfig` includes both `tabs<MainTabs>` and `tabs<MediaTabs>` blocks

#### Task 3.3: Update collector in `QuoVadisSymbolProcessor`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt`
- **Description**: When a `@TabItem` + `@Tabs` class is encountered, process it both as a tab item (for the parent) and as a tabs container (for itself). The nested `TabInfo`'s sub-tabs should be collected as containers too.
- **Acceptance**: Cross-module `@Tabs` referenced in `items` array are discovered and included in generated config

### Phase 4: Validation Changes

#### Task 4.1: Update `ValidationEngine` for `NESTED_TABS`

- **File**: `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`
- **Description**:
  - Accept `@TabItem` + `@Tabs` as valid combination
  - Add `validateNestedTabsTabs()` method parallel to `validateNestedStackTabs()`:
    - `NESTED_TABS` tab must have valid `@Tabs` annotation
    - `NESTED_TABS` tab must have at least one sub-tab item
  - Add circular nesting detection (A→B→A fails with clear error)
  - Add max depth warning (> 3 levels deep)
- **Acceptance**: `@TabItem` + `@Tabs` passes validation; circular `A→B→A` fails

### Phase 5: Runtime DSL Verification

#### Task 5.1: Verify `DslNavigationConfig.buildTabStack()` handles nested tabs

- **File**: `quo-vadis-core/src/.../navigation/config/DslNavigationConfig.kt`
- **Description**: The `TabEntry.ContainerReference` path already calls `buildNavNode()` which returns either `StackNode` or `TabNode`. Verify this works correctly for `TabNode` return (tabs-within-tabs). Write a unit test.
- **Acceptance**: Unit test passes showing `TabNode` inside `TabNode`

### Phase 6: Tests

#### Task 6.1: KSP processor tests — cross-module tab items

- Add test: `@Tabs` with `items` array referencing class from simulated external module
- Verify generated config contains both the parent tabs block and the referenced stack block

#### Task 6.2: KSP processor tests — `NESTED_TABS` detection

- Add test: `@TabItem` + `@Tabs` class → detected as `NESTED_TABS`
- Add test: `TabItemInfo.nestedTabInfo` is populated with sub-tab metadata

#### Task 6.3: Runtime tests — nested `TabNode` building

- Add test: `buildNavNode` for a tabs container that has a `NESTED_TABS` entry
- Verify resulting tree: `TabNode → StackNode → TabNode → StackNodes`

#### Task 6.4: Validation tests — circular tab nesting

- Add test: `A` has tab item `B`, `B` has tab item `A` → validation error
- Add test: `A` has tab item `B`, `B` has tab item `C` (no cycle) → passes
- Add test: nesting depth > 3 → validation warning

### Phase 7: Demo App Update

#### Task 7.1: Demonstrate cross-module tab nesting

- Move a tab definition to `feature1` or create a new feature module
- Reference it from `MainTabs` in `composeApp` via `items` array
- Verify compile + run on Desktop and Android

---

## Sequencing

```
Phase 1 (Models)
    │
    ▼
Phase 2 (Extractors)
    │
    ▼
Phase 3 (Generators)
    │
    ▼
Phase 4 (Validation)
    │
    ▼
Phase 5 (Runtime verification)
    │
    ▼
Phase 6 (Tests)
    │
    ▼
Phase 7 (Demo)
```

Phases 1–4 are strictly sequential (each depends on the prior). Phase 5 can start independently but is best verified after Phase 3. Phase 6 tests span all prior phases. Phase 7 is integration-level.

---

## Files Modified

| File | Change |
|------|--------|
| `quo-vadis-ksp/.../models/TabInfo.kt` | Add `NESTED_TABS` enum, `nestedTabInfo` field |
| `quo-vadis-ksp/.../extractors/TabExtractor.kt` | Update detection + extraction for `NESTED_TABS` |
| `quo-vadis-ksp/.../generators/dsl/ContainerBlockGenerator.kt` | Handle `NESTED_TABS` in tab entry generation |
| `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt` | Register nested tab containers |
| `quo-vadis-ksp/.../QuoVadisSymbolProcessor.kt` | Collect nested tabs as containers |
| `quo-vadis-ksp/.../validation/ValidationEngine.kt` | Validate `NESTED_TABS`, circular detection |
| `quo-vadis-core/.../config/DslNavigationConfig.kt` | Verify only (no changes expected) |
| Test files (new) | KSP + runtime tests for all phases |
| `composeApp/` or feature modules | Demo integration |

---

## Risks and Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| KSP multi-round processing issues with cross-module types | Medium | Low | Types in `items` array are resolved as `KSType` — already works for `@Stack` references |
| Deep nesting (tabs→tabs→tabs) causes infinite recursion | High | Low | Add max depth check (3 levels) in `TabExtractor` and circular reference detection in `ValidationEngine` |
| `TabsContainerScope.tabs` property type matching for cross-module types | Low | Low | Already uses `NavDestination` instances — `when` pattern matching works across modules |
| Back navigation behavior with nested tabs | Medium | Medium | Existing back navigation traverses the tree depth-first — verify with integration tests |
| FlowMVI container discovery in nested tab scopes | Low | Low | `LocalContainerNode`/`LocalScreenNode` are already set per-node by renderers regardless of depth |
| Generated code ordering — nested container must be generated before parent references it | Medium | Medium | `NavigationConfigGenerator` must emit nested `tabs<>` blocks before parent `tabs<>` block, or rely on runtime lazy resolution |

---

## Estimation

| Phase | Tasks | Complexity |
|-------|-------|------------|
| Phase 1: Models | 2 | Low |
| Phase 2: Extractors | 3 | Medium |
| Phase 3: Generators | 3 | Medium |
| Phase 4: Validation | 1 | Medium |
| Phase 5: Runtime verification | 1 | Low |
| Phase 6: Tests | 4 | Medium |
| Phase 7: Demo | 1 | Low |
| **Total** | **15 tasks** | |

---

## Open Questions

None remaining — all clarified through HITL process.
