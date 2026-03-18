# Modal Navigation Support — Implementation Plan

> **Issue:** GitHub #43 — Modal navigation rendering support  
> **Target audience:** Developers implementing this feature  
> **Status:** Draft

---

## Overview

Add `@Modal` annotation and library support for draw-behind rendering in the quo-vadis navigation library.

**Library responsibility:** ONLY ensure content below a modal is drawn. No scrim, no dismiss behavior — the user controls all visual treatment in their composable. The library simply renders the background screen underneath the modal screen in the same `Box`.

---

## Requirements

| Requirement | Detail |
|---|---|
| **`@Modal` annotation** | Marker annotation, applicable to `@Destination`, `@Tabs`, `@Stack`, `@Pane` annotated classes |
| **DSL support** | `modal<T>()` for destinations, `modalContainer("name")` for containers |
| **Rendering** | When the active child of a `StackNode` is marked modal, `StackRenderer` renders BOTH the screen below AND the modal on top |
| **No library chrome** | No scrim, no click interception, no dismiss behavior from library side |
| **User control** | The user's composable has full control over modal visual presentation |
| **No restrictions** | Any `@Destination`/`@Tabs`/`@Stack`/`@Pane` can be `@Modal` |
| **Back navigation** | Works normally (stack pop) |
| **Predictive back** | Dismiss via gesture is supported |

---

## Technical Approach

### Registry Design

**`ModalRegistry`** interface with two lookup methods:

```kotlin
interface ModalRegistry {
    fun isModalDestination(destinationClass: KClass<*>): Boolean
    fun isModalContainer(containerKey: String): Boolean

    companion object {
        val Empty: ModalRegistry = object : ModalRegistry {
            override fun isModalDestination(destinationClass: KClass<*>) = false
            override fun isModalContainer(containerKey: String) = false
        }
    }
}
```

- **`DslModalRegistry`** — implementation backed by `Set<KClass<out NavDestination>>` (destinations) and `Set<String>` (container keys).
- **`CompositeModalRegistry`** — for multi-module support. Union semantics: if either primary or secondary says modal, it's modal (secondary wins first, fallback to primary — same pattern as other composite registries).
- **Key insight:** Container node keys are derived from `@Tabs/@Stack/@Pane` `name` property via `scopeKey.value`, so the KSP generates registry entries using the same name.

### Rendering Design (modeled on `PredictiveBackContent` pattern)

**`ModalContent`** composable — simple `Box` with two layers:

- **Background:** The node below the active child, rendered through `NavNodeRenderer` (which handles caching via `ComposableCache`)
- **Foreground:** The modal node, rendered through `NavNodeRenderer`
- No transforms, no scrim, no parallax — just two layers in a `Box`
- Uses `StaticAnimatedVisibilityScope` for both layers (same as `PredictiveBackContent`)

**`StackRenderer` modifications:**

1. Gets `ModalRegistry` via `NavRenderScope`
2. Before calling `AnimatedNavContent`, checks if active child is modal
3. If modal → gets the child below active child from `node.children`; renders `ModalContent(background=childBelow, foreground=activeChild)`
4. If not modal → existing `AnimatedNavContent` behavior unchanged
5. `AnimatedNavContent` needs adaptation for modal content support
6. Predictive back during modal works because `PredictiveBackContent` already handles dual rendering — when gesture is active on a modal, the modal slides away and reveals the screen below (which is already visible)

### KSP Pipeline (following exact `TransitionRegistry` pattern)

**New files:**

| # | File | Purpose |
|---|---|---|
| 1 | `quo-vadis-annotations/.../Modal.kt` | `@Modal` annotation |
| 2 | `quo-vadis-ksp/.../models/ModalInfo.kt` | Data class: `annotatedClass`, `qualifiedName`, `isDestination`, `containerName?`, `containingFile` |
| 3 | `quo-vadis-ksp/.../extractors/ModalExtractor.kt` | Extracts `@Modal` from classes, detects destination vs container |
| 4 | `quo-vadis-ksp/.../generators/dsl/ModalBlockGenerator.kt` | Generates `modal<T>()` / `modalContainer("name")` calls |
| 5 | `quo-vadis-core/.../registry/ModalRegistry.kt` | Interface + `Empty` companion |
| 6 | `quo-vadis-core/.../registry/internal/CompositeModalRegistry.kt` | Composite for multi-module |
| 7 | `quo-vadis-core/.../dsl/internal/DslModalRegistry.kt` | DSL-backed implementation |
| 8 | `quo-vadis-core/.../compose/internal/render/ModalContent.kt` | Dual-layer composable |

**Modified files:**

| # | File | Change |
|---|---|---|
| 9 | `quo-vadis-ksp/.../QuoVadisSymbolProcessor.kt` | Add `ModalExtractor`, `collectedModals`, `collectModals()`, wire into `NavigationData` |
| 10 | `quo-vadis-ksp/.../QuoVadisClassNames.kt` | Add `MODAL_REGISTRY` `ClassName` |
| 11 | `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt` | Add `ModalBlockGenerator`, `modals` to `NavigationData`, MODALS section, `modalRegistry` delegation |
| 12 | `quo-vadis-ksp/.../generators/base/StringTemplates.kt` | Add `MODALS_SECTION` header |
| 13 | `quo-vadis-core/.../navigation/config/NavigationConfig.kt` | Add `val modalRegistry: ModalRegistry` with default `ModalRegistry.Empty` |
| 14 | `quo-vadis-core/.../dsl/NavigationConfigBuilder.kt` | Add modal storage, `modal<D>()`, `modalContainer("name")` DSL functions |
| 15 | `quo-vadis-core/.../dsl/internal/DslNavigationConfig.kt` | Accept modal data, create `DslModalRegistry` |
| 16 | `quo-vadis-core/.../navigation/internal/config/CompositeNavigationConfig.kt` | Add `modalRegistry` composite |
| 17 | `quo-vadis-core/.../navigation/internal/config/EmptyNavigationConfig.kt` | Add `modalRegistry = ModalRegistry.Empty` |
| 18 | `quo-vadis-core/.../compose/NavigationHost.kt` | Extract `modalRegistry`, pass to `NavRenderScope` |
| 19 | `quo-vadis-core/.../compose/scope/NavRenderScope.kt` | Add `modalRegistry` property |
| 20 | `quo-vadis-core/.../compose/internal/render/StackRenderer.kt` | Add modal check and dual-layer rendering |
| 21 | `quo-vadis-core/.../compose/internal/render/AnimatedNavContent.kt` | Adapt for modal content support |

---

## Tasks

### Phase 1: Core Registry Infrastructure

#### Task 1.1 — Create `@Modal` annotation

- **File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Modal.kt`
- Simple marker annotation:
  ```kotlin
  @Target(AnnotationTarget.CLASS)
  @Retention(AnnotationRetention.SOURCE)
  annotation class Modal
  ```

#### Task 1.2 — Create `ModalRegistry` interface

- **File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/ModalRegistry.kt`
- Interface with `isModalDestination(KClass<*>): Boolean` and `isModalContainer(String): Boolean`
- `companion object` with `Empty` singleton returning `false` for both

#### Task 1.3 — Create `DslModalRegistry`

- **File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/internal/DslModalRegistry.kt`
- Implementation backed by `Set<KClass<out NavDestination>>` and `Set<String>`
- Lookups via `contains()` on the respective set

#### Task 1.4 — Create `CompositeModalRegistry`

- **File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/internal/CompositeModalRegistry.kt`
- Primary + secondary union: if either says modal, it's modal
- Follow same pattern as other composite registries in the codebase

---

### Phase 2: Wire Registry into Config/DSL

#### Task 2.1 — Add `modalRegistry` to `NavigationConfig`

- Add `val modalRegistry: ModalRegistry` with default `ModalRegistry.Empty`
- Matches pattern of `transitionRegistry` on the same interface

#### Task 2.2 — Add DSL builder functions to `NavigationConfigBuilder`

- Add `modal<D>()` inline reified function for destinations
- Add `modalContainer(name: String)` function for containers
- Add internal storage: `MutableSet<KClass<out NavDestination>>`, `MutableSet<String>`

#### Task 2.3 — Wire into `DslNavigationConfig`

- Accept modal data in constructor (destination set + container set)
- Create `DslModalRegistry` lazily from the sets

#### Task 2.4 — Wire into `CompositeNavigationConfig`

- Compose primary + secondary modal registries via `CompositeModalRegistry`

#### Task 2.5 — Wire into `EmptyNavigationConfig`

- Return `ModalRegistry.Empty`

---

### Phase 3: Rendering Support

#### Task 3.1 — Add `modalRegistry` to `NavRenderScope`

- Add property to interface and implementation class
- Available to all renderers via scope

#### Task 3.2 — Pass `modalRegistry` from `NavigationHost`

- Extract `modalRegistry` from `NavigationConfig`
- Pass to `NavRenderScopeImpl` constructor

#### Task 3.3 — Create `ModalContent` composable

- **File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/ModalContent.kt`
- Simple `Box` with two layers: background + foreground
- Uses `StaticAnimatedVisibilityScope` for both layers
- Content goes through `NavNodeRenderer` which handles caching via `ComposableCache`

#### Task 3.4 — Modify `StackRenderer` for modal awareness

- Check if active child is modal via `ModalRegistry`:
  - For `ScreenNode`: `modalRegistry.isModalDestination(screenNode.destination::class)`
  - For container nodes (`TabNode`, `StackNode`, `PaneNode`): `modalRegistry.isModalContainer(node.key)`
- If modal: get child below from `node.children`, render `ModalContent`
- If not modal: existing `AnimatedNavContent` behavior unchanged
- **Edge case:** Modal as only child in stack (no screen below) → render normally without background layer

#### Task 3.5 — Adapt `AnimatedNavContent` for modal support

- When transitioning **TO** a modal destination, the exit animation should NOT remove the background
- When transitioning **FROM** a modal destination back, the enter animation should handle properly
- Integration with predictive back: `PredictiveBackContent` already handles dual rendering — when back gesture is active on a modal, it's a normal stack pop

---

### Phase 4: KSP Code Generation

#### Task 4.1 — Create `ModalInfo` data class

- **File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/ModalInfo.kt`
- Fields: `annotatedClass: KSClassDeclaration`, `qualifiedName: String`, `isDestination: Boolean`, `containerName: String?`, `containingFile: KSFile`

#### Task 4.2 — Create `ModalExtractor`

- **File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/ModalExtractor.kt`
- Detect `@Modal` on classes
- Determine if it's on a `@Destination` or a container (`@Tabs`/`@Stack`/`@Pane`)
- Extract appropriate `ModalInfo`

#### Task 4.3 — Create `ModalBlockGenerator`

- **File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ModalBlockGenerator.kt`
- Generate `modal<T>()` for destinations
- Generate `modalContainer("name")` for containers

#### Task 4.4 — Update `NavigationConfigGenerator`

- Add `modals` to `NavigationData`
- Instantiate `ModalBlockGenerator`
- Add `MODALS` section in `buildDslContent()`
- Add `modalRegistry` delegation property

#### Task 4.5 — Update `QuoVadisSymbolProcessor`

- Add `ModalExtractor` instantiation
- Add `collectedModals` list
- Add `collectModals()` method
- Wire into `NavigationData`

#### Task 4.6 — Update `QuoVadisClassNames`

- Add `MODAL_REGISTRY` `ClassName`

#### Task 4.7 — Update `StringTemplates`

- Add `MODALS_SECTION` header constant

---

### Phase 5: Testing & Demo

#### Task 5.1 — Unit tests for `ModalRegistry`

- Test `DslModalRegistry` for destinations and containers
- Test `CompositeModalRegistry` union behavior
- Test `Empty` returns false for all inputs

#### Task 5.2 — Unit tests for `ModalExtractor`

- Test extraction from `@Modal @Destination`
- Test extraction from `@Modal @Tabs`
- Test extraction from `@Modal @Stack`

#### Task 5.3 — Integration test / Demo

- Add a modal destination to the demo app (`composeApp`)
- Demonstrate draw-behind behavior with custom scrim in user composable

---

## Sequencing

```
Phase 1 (Core Registry) ──→ Phase 2 (Config/DSL wiring) ──→ Phase 3 (Rendering)
                                                          ╲
                                                           ──→ Phase 4 (KSP)  ──→ Phase 5 (Testing)
```

- **Phase 1 → Phase 2:** Config/DSL depends on registry types
- **Phase 2 → Phase 3:** Rendering reads registry from config via render scope
- **Phase 2 → Phase 4:** KSP generates DSL calls that depend on builder functions
- **Phase 3 and Phase 4 can run in parallel** since they touch different modules
- **Phase 5** depends on both Phase 3 and Phase 4

---

## Risks & Mitigations

| # | Risk | Mitigation |
|---|---|---|
| 1 | **`ComposableCache` eviction during modal** — background screen could be evicted while modal is showing | Since the background is actively composed (rendered in the `Box`), it naturally stays in cache. No special locking needed unless testing reveals otherwise. |
| 2 | **Predictive back interaction** — back gesture while modal is showing might conflict | `PredictiveBackContent` already handles dual rendering. Back from a modal is a normal stack pop. The cascade state calculation handles this naturally since the modal is just a regular node in the stack. |
| 3 | **Animation transition to/from modal** — `AnimatedContent` transition behavior when navigating to/from a modal | When navigating TO a modal, the enter transition animates the modal while the background stays. Need to handle `AnimatedContent` so the exit side doesn't remove the background. This is the trickiest part — may need to bypass `AnimatedContent` for modals (like predictive back does) or use a custom arrangement. |
| 4 | **Nested modals** — a modal navigates to another modal | `StackRenderer` handles this by showing: (background) + (first modal as background) + (second modal). Since we look at the active child and below, this works naturally with the stack walk-back approach. |
| 5 | **Multiple consecutive modals stacked** | For v1: walk backwards through the stack until a non-modal node is found, render everything from there to the top. This gives the most natural layered behavior. |

---

## Open Questions

1. **Multiple consecutive modal rendering** — Should the stack walk-back render all intermediate modals? (e.g., `ScreenA → ModalB → ModalC` — should `ModalB` be visible behind `ModalC`, and `ScreenA` behind `ModalB`?)
   - **Recommendation:** Yes. Walk backwards through the stack until a non-modal node is found, then render everything from there to the top. This gives the most natural behavior.

2. **`AnimatedNavContent` integration** — The exact interaction between `AnimatedNavContent` and modal rendering needs careful implementation — especially how to prevent the background from participating in exit animations while still animating the modal's entrance. This may require bypassing `AnimatedContent` for modal transitions (similar to how predictive back bypasses it).
