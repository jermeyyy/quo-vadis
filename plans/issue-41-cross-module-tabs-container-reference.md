# Implementation Plan: Issue #41 Cross-Module Container Tabs

## Overview

Enable `@Tabs` containers to reference existing navigation containers declared in other modules.

The goal is to make this annotation-driven API support the same composition model that already exists in the runtime DSL via `containerTab<T>()`. This should allow a tab in one module to host:

- a flat destination declared locally
- a local nested `@Stack`
- an existing `@Stack` declared in another module
- an existing `@Tabs` container declared in another module

This solves the current limitation where tab content must be enclosed in one sealed inheritance tree in one module.

## Requirements

- Preserve existing `@Tabs` + `@TabItem` behavior.
- Support cross-module tab composition without sealed inheritance across modules.
- Align annotation API behavior with existing runtime support for `TabEntry.ContainerReference`.
- Keep generated configs composable through `NavigationConfig.plus`.
- Preserve FlowMVI semantics:
  - each screen keeps its own `rememberContainer(...)` scope
  - each tab or pane container keeps its own `rememberSharedContainer(...)` scope
  - nested containers create nested shared-container scopes, not a merged scope
- Avoid breaking existing `@TabsContainer` wrappers.

## Problem Statement

Today the runtime already supports container references inside tabs:

- `TabsBuilder.containerTab<T>()`
- `TabEntry.ContainerReference`
- recursive container-member lookup in `DslContainerRegistry`
- cross-config resolution in `CompositeContainerRegistry`

The gap is in the annotation/KSP layer.

Current `@Tabs(items = [...])` extraction assumes each item is a `@TabItem` that can be re-extracted in the current module as either:

- `@Destination` -> flat screen tab
- `@Stack` -> nested stack tab

That model breaks down for cross-module composition because an imported container should be treated as a reference, not re-declared as a local tab item. It also leaks into wrapper ergonomics: `TabsContainerScope.tabs` currently exposes the first rendered screen destination of each tab, which is not a stable identity for referenced containers, especially nested `@Tabs`.

## Recommendation

Adopt a general container-reference model in the annotation API.

### API Direction

Keep a single `items` array on `@Tabs`, but allow each item to be one of two categories:

- local tab item: `@TabItem` + `@Destination` or `@Stack`
- container reference: existing class annotated with `@Stack` or `@Tabs`, without requiring `@TabItem`

This keeps the call site compact and mirrors the runtime DSL semantics:

- `tab(...)` for local flat destinations
- `containerTab<T>()` for referenced containers

### Proposed End-User API

#### Feature module

```kotlin
@Stack(name = "settingsFlow", startDestination = SettingsDestination.General::class)
sealed class SettingsDestination : NavDestination {
    @Destination(route = "settings/general")
    data object General : SettingsDestination()

    @Destination(route = "settings/account")
    data object Account : SettingsDestination()
}

@Tabs(
    name = "advancedSettingsTabs",
    initialTab = NotificationsTab::class,
    items = [NotificationsTab::class, PrivacyTab::class]
)
object AdvancedSettingsTabs : NavDestination
```

#### App module

```kotlin
@TabItem
@Destination(route = "main/home")
data object HomeTab : NavDestination

@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [
        HomeTab::class,
        SettingsDestination::class,
        AdvancedSettingsTabs::class,
    ]
)
object MainTabs : NavDestination
```

This means:

- `HomeTab` is a local flat tab
- `SettingsDestination` is a referenced stack container tab
- `AdvancedSettingsTabs` is a referenced nested tabs container tab

### Wrapper API Recommendation

Do not rely solely on `TabsContainerScope.tabs: List<NavDestination>` for referenced-container tabs.

Add a new additive API that exposes stable tab identity, for example:

```kotlin
@Stable
interface TabItemDescriptor {
    val route: String
    val destinationClass: KClass<out NavDestination>
    val isContainerReference: Boolean
}

interface TabsContainerScope {
    val tabItems: List<TabItemDescriptor>
    val tabs: List<NavDestination> // keep for backward compatibility
}
```

This avoids inferring tab identity from the first screen rendered under the tab.

#### Example wrapper with FlowMVI shared container

```kotlin
@TabsContainer(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabsContainerScope,
    content: @Composable () -> Unit,
) {
    val sharedStore = rememberSharedContainer<MainTabsContainer, MainTabsState, MainTabsIntent, MainTabsAction>()

    CompositionLocalProvider(LocalMainTabsStore provides sharedStore) {
        NavigationBar {
            scope.tabItems.forEachIndexed { index, item ->
                val label = when (item.destinationClass) {
                    HomeTab::class -> "Home"
                    SettingsDestination::class -> "Settings"
                    AdvancedSettingsTabs::class -> "Advanced"
                    else -> "Tab"
                }

                NavigationBarItem(
                    selected = index == scope.activeTabIndex,
                    onClick = { scope.switchTab(index) },
                    label = { Text(label) },
                    icon = { }
                )
            }
        }

        content()
    }
}
```

#### Example child screen with screen-scoped FlowMVI container

```kotlin
@Screen(SettingsDestination.General::class)
@Composable
fun SettingsGeneralScreen() {
    val screenStore = rememberContainer<SettingsGeneralContainer, SettingsGeneralState, SettingsGeneralIntent, SettingsGeneralAction>()
    val mainTabsStore = LocalMainTabsStore.current

    // screenStore is scoped to this screen instance
    // mainTabsStore is scoped to MainTabs container
}
```

### FlowMVI Semantics

This design is consistent with the current lifecycle/scoping model:

- `rememberContainer(...)` continues to scope per `ScreenNode.key`
- `rememberSharedContainer(...)` continues to scope per container `NavNode.key`
- if a `MainTabs` tab references `AdvancedSettingsTabs`, then:
  - `MainTabsWrapper` gets one shared container scope
  - `AdvancedSettingsTabs` gets its own nested shared container scope
  - screens inside `AdvancedSettingsTabs` still get their own screen scopes

That separation is desirable because each container owns its own lifecycle and should be able to host independent shared state.

## Technical Approach

### 1. Extend the KSP tab model

Add a third tab item type:

- `FLAT_SCREEN`
- `NESTED_STACK`
- `CONTAINER_REFERENCE`

Update `TabItemInfo` so a referenced container is represented explicitly instead of being coerced into the local `@TabItem` model.

### 2. Update extraction rules

For each class listed in `@Tabs.items`:

- if it has `@TabItem` + `@Destination`, extract as `FLAT_SCREEN`
- if it has `@TabItem` + `@Stack`, extract as `NESTED_STACK`
- if it has `@Stack` and no `@TabItem`, extract as `CONTAINER_REFERENCE`
- if it has `@Tabs` and no `@TabItem`, extract as `CONTAINER_REFERENCE`
- otherwise fail validation with a targeted error

Important: container references must not require local destination extraction in the consuming module.

### 3. Update generation

Generate tab entries as follows:

- `FLAT_SCREEN` -> `tab(Object)`
- `NESTED_STACK` -> `containerTab<StackType>()`
- `CONTAINER_REFERENCE` -> `containerTab<ContainerType>()`

This keeps generated code aligned with the existing DSL/runtime.

### 4. Update validation

Validation must distinguish between local tab items and referenced containers.

Checks:

- `@TabItem` still requires exactly one of `@Destination` or `@Stack`
- referenced containers must be annotated with `@Stack` or `@Tabs`
- `initialTab` must be allowed to target a referenced container
- avoid validating referenced container internals from the consuming module
  - those validations belong to the producing module's generated config

### 5. Improve wrapper metadata

Add explicit tab identity metadata for wrappers.

Minimum additive approach:

- extend `GeneratedTabMetadata` to include destination/container qualified name
- expose a new `TabsContainerScope.tabItems` property derived from `TabNode.tabMetadata`
- keep existing `tabs` for compatibility

This enables wrappers to label container-reference tabs without depending on the first rendered screen.

### 6. Documentation and examples

Update docs and samples to show:

- cross-module stack tab reference
- nested tabs reference inside another tabs container
- wrapper usage with `tabItems`
- FlowMVI integration using `rememberSharedContainer(...)` in parent and nested containers

## Tasks

### Phase 1: Model and extraction

#### Task 1.1: Add container-reference tab type
- **Files:** `quo-vadis-ksp/.../models/TabInfo.kt`
- **Description:** Add explicit representation for container references in tab metadata.
- **Acceptance Criteria:** Tab items can represent referenced `@Stack` and `@Tabs` containers.

#### Task 1.2: Update `TabExtractor`
- **Files:** `quo-vadis-ksp/.../extractors/TabExtractor.kt`
- **Description:** Detect referenced containers from `@Tabs.items` without requiring `@TabItem`.
- **Acceptance Criteria:** Imported `@Stack` and `@Tabs` classes in `items` are extracted successfully as references.

#### Task 1.3: Update validation rules
- **Files:** `quo-vadis-ksp/.../validation/ValidationEngine.kt`
- **Description:** Separate validation rules for local tab items vs referenced containers.
- **Acceptance Criteria:** Consuming module does not fail because referenced container destinations live in another module.

### Phase 2: Code generation

#### Task 2.1: Generate container-reference tab entries
- **Files:** `quo-vadis-ksp/.../generators/dsl/ContainerBlockGenerator.kt`
- **Description:** Emit `containerTab<...>()` for referenced containers.
- **Acceptance Criteria:** Generated DSL matches runtime semantics for both local nested stacks and imported containers.

#### Task 2.2: Extend generated tab metadata
- **Files:** `quo-vadis-core/.../navigation/GeneratedTabMetadata.kt`, `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt`, `quo-vadis-core/.../dsl/DslNavigationConfig.kt`
- **Description:** Carry stable tab identity into runtime `TabNode` metadata.
- **Acceptance Criteria:** Wrapper layer can identify container-reference tabs without inspecting rendered child content.

### Phase 3: Public wrapper API

#### Task 3.1: Add additive `TabsContainerScope` metadata API
- **Files:** `quo-vadis-core/.../compose/scope/TabsContainerScope.kt`, `quo-vadis-core/.../compose/internal/render/TabRenderer.kt`
- **Description:** Expose `tabItems` or equivalent while preserving `tabs`.
- **Acceptance Criteria:** Existing wrappers compile unchanged; new wrappers can distinguish local tabs from referenced containers.

### Phase 4: Samples and docs

#### Task 4.1: Add multi-module sample
- **Files:** `composeApp`, `feature1`, `feature2` sample destinations/wrappers as needed
- **Description:** Demonstrate one parent `@Tabs` with imported `@Stack` and imported nested `@Tabs`.
- **Acceptance Criteria:** Sample app composes multiple module configs and renders both referenced tab types.

#### Task 4.2: Add FlowMVI sample
- **Files:** `composeApp` sample wrapper/container files, docs
- **Description:** Show parent tabs shared container, nested tabs shared container, and screen container coexistence.
- **Acceptance Criteria:** Example clearly demonstrates scope separation.

#### Task 4.3: Update docs
- **Files:** `docs/ANNOTATIONS.md`, `docs/ARCHITECTURE.md`, `README.md` if needed
- **Description:** Document the new `@Tabs.items` semantics and wrapper metadata API.
- **Acceptance Criteria:** End-user guidance covers cross-module composition and FlowMVI usage.

## Sequencing

1. KSP model changes
2. Extractor and validation updates
3. Generator changes
4. Runtime tab metadata API additions
5. Samples and docs
6. Verification with combined multi-module config

## Verification

- Build generated configs for app and feature modules.
- Verify `ComposeAppNavigationConfig + Feature1NavigationConfig + Feature2NavigationConfig` builds successfully.
- Verify navigation directly to a destination inside a referenced container still creates the correct parent tab container.
- Verify parent and nested `rememberSharedContainer(...)` scopes are distinct and cleaned up with their container lifecycles.
- Verify each screen still resolves its own `rememberContainer(...)` scope.

## Risks and Mitigations

- **Risk:** Wrapper APIs become ambiguous for referenced containers.
  - **Mitigation:** Add explicit tab metadata instead of relying only on `tabs: List<NavDestination>`.

- **Risk:** Validation reintroduces cross-module false failures.
  - **Mitigation:** Treat referenced containers as opaque references in the consuming module.

- **Risk:** Breaking current wrappers.
  - **Mitigation:** Keep `tabs` unchanged and add a new additive metadata API.

- **Risk:** Nested shared containers are misunderstood as duplicate state.
  - **Mitigation:** Document lifecycle boundaries and show parent vs nested container responsibilities in FlowMVI examples.

## Open Questions

- Whether `GeneratedTabMetadata` should expose only qualified class name or a richer public descriptor type directly.
- Whether to allow referenced `@Pane` containers in tabs later, or intentionally scope the feature to `@Stack` and `@Tabs` only.

## Recommended Decision

Proceed with the general container-reference solution, limited to referenced `@Stack` and `@Tabs` in `@Tabs.items`.

This is the smallest architecture change that:

- solves the issue as reported
- matches the existing runtime DSL
- works naturally with multi-module `NavigationConfig.plus`
- preserves FlowMVI screen and container scope semantics
- opens the door to nested tab containers without forcing more sealed inheritance tricks