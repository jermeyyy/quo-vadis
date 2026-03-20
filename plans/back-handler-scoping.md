# Implementation Plan: Scoped BackHandler — Fix #45

## Overview

Fix the bug where custom `BackHandler`s consume both programmatic `navigateBack()` calls AND user-initiated back events. The correct behavior is:
- **User-initiated back** (system back button, predictive back gesture) → consulted by `BackHandlerRegistry`
- **Programmatic back** (`navigator.navigateBack()`) → bypasses `BackHandlerRegistry` entirely

Additionally, provide easier APIs for registering lifecycle-aware back handlers:
- `NavBackHandler` composable in `quo-vadis-core` (Compose-side)
- `registerBackHandler()` on `NavigationContainerScope` and `SharedContainerScope` (FlowMVI container-side)

## Requirements (Validated)

1. `navigator.navigateBack()` must NOT consult `BackHandlerRegistry`
2. User-initiated system back / predictive back gesture MUST consult `BackHandlerRegistry`
3. Registry must be **scope-aware**: only handlers from the **currently active screen** fire on back
4. Registry data structure: `Map<NodeKey, List<Handler>>` keyed by screen node key
5. If active screen has registered handlers, predictive back gesture is **disabled** at gesture START — falls back to simple `onBack()` callback (no misleading preview animation)
6. `NavBackHandler` composable provided in core for non-FlowMVI users
7. `registerBackHandler()` on both `NavigationContainerScope` (screen-scoped) and `SharedContainerScope` (Tab/Pane-scoped)
8. Handlers auto-unregister when container scope is destroyed

## Technical Approach

### Current Architecture (Broken)

```
PROGRAMMATIC: navigator.navigateBack() → onBack() → backHandlerRegistry.handleBack() ← BUG (intercepted)
USER GESTURE: NavigateBackHandler.onBackCompleted → navigator.updateState(speculative) ← BUG (NOT intercepted)
```

### Target Architecture

```
PROGRAMMATIC: navigator.navigateBack() → TreeMutator.popWithTabBehavior() (direct, no registry)
USER GESTURE: NavigateBackHandler.onBackCompleted → navigator.onBack() → registry.handleBack(activeKey) → tree pop
PREDICTIVE:   NavigateBackHandler.enabled = canGoBack && !registry.hasHandlers(activeKey)
              (if handlers exist, predictive back disabled, simple onBack() used instead)
```

---

## Tasks

### Phase 1: Refactor `BackHandlerRegistry` to Scope-Aware

#### Task 1.1: Refactor `BackHandlerRegistry` Internal Data Structure

- **Description:** Change `BackHandlerRegistry` from flat `mutableListOf<() -> Boolean>()` to `mutableMapOf<NodeKey, MutableList<() -> Boolean>>()`. Update all methods to accept/use `NodeKey`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/BackHandlerRegistry.kt`
- **Dependencies:** None
- **Acceptance Criteria:**
  - `register(key: NodeKey, handler: () -> Boolean): () -> Unit` — registers handler under the given node key
  - `handleBack(activeKey: NodeKey): Boolean` — only consults handlers registered under `activeKey`, LIFO order
  - `hasHandlers(activeKey: NodeKey): Boolean` — returns true if any handlers registered for `activeKey`
  - `unregisterAll(key: NodeKey)` — removes all handlers for a given key (for cleanup)
  - Unregister function returned by `register()` still works (removes individual handler)
  - Old `handleBack()` and `hasHandlers()` (no args) removed — all callers must pass key

#### Task 1.2: Update `TreeNavigator` References to New Registry API

- **Description:** Update `TreeNavigator.onBack()` to pass the active screen's `NodeKey` to `handleBack(activeKey)`. The active screen key is derived from `_state.value.activeLeaf()?.key`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeNavigator.kt`
- **Dependencies:** Task 1.1
- **Acceptance Criteria:**
  - `onBack()` calls `backHandlerRegistry?.handleBack(activeKey)` where `activeKey = _state.value.activeLeaf()?.key`
  - If `activeLeaf()` is null, skip registry check entirely

---

### Phase 2: Decouple `navigateBack()` from `onBack()`

#### Task 2.1: Separate `navigateBack()` and `onBack()` in `TreeNavigator`

- **Description:** `navigateBack()` currently delegates to `onBack()`. Change `navigateBack()` to perform tree pop directly (skip `BackHandlerRegistry`). Keep `onBack()` as the user-back path that consults the registry first.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeNavigator.kt`
- **Dependencies:** Task 1.2
- **Acceptance Criteria:**
  - `navigateBack()` contains the tree pop logic (popWithTabBehavior + popPaneAdaptive fallback) WITHOUT consulting `BackHandlerRegistry`
  - `onBack()` checks `BackHandlerRegistry` first; if not consumed, falls through to tree pop logic
  - Both methods share the tree pop logic via a private helper (e.g. `performTreePop(): Boolean`) to avoid duplication
  - Existing tests still pass

#### Task 2.2: Document the Semantic Difference

- **Description:** Update KDoc on `Navigator.navigateBack()` and `BackPressHandler.onBack()` to clearly document the behavioral difference.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/navigator/Navigator.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/navigator/BackPressHandler.kt`
- **Dependencies:** Task 2.1
- **Acceptance Criteria:**
  - `navigateBack()` KDoc states it performs programmatic back navigation, bypassing user-defined back handlers
  - `onBack()` KDoc states it handles user-initiated back events, consulting BackHandlerRegistry first

---

### Phase 3: Wire User-Initiated Back Through Registry in `NavigationHost`

#### Task 3.1: Replace `updateState()` with `onBack()` in Gesture Completion

- **Description:** In `NavigationHost`, the `onBackCompleted` callback currently uses `navigator.updateState(speculativePopState)` directly. Replace this with `navigator.onBack()` so user-initiated back events flow through the `BackHandlerRegistry`. For the non-predictive path (when predictive back is disabled or when handlers exist), also wire through `onBack()`.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/NavigationHost.kt`
- **Dependencies:** Task 2.1
- **Acceptance Criteria:**
  - `onBackCompleted` calls `(navigator as? BackPressHandler)?.onBack()` or equivalent instead of `updateState()`
  - The speculative pop animation still works for the common case (no handlers registered)
  - When a handler consumes the event, the gesture completion doesn't navigate

#### Task 3.2: Disable Predictive Back When Active Screen Has Handlers

- **Description:** Modify the `enabled` condition for `NavigateBackHandler` in `NavigationHost` to also check `!backHandlerRegistry.hasHandlers(activeScreenKey)`. When handlers are registered, predictive back gesture is disabled entirely; a separate simple `NavigateBackHandler` (no progress/cancel) calls `navigator.onBack()` instead.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/NavigationHost.kt`
- **Dependencies:** Task 3.1, Task 1.1
- **Acceptance Criteria:**
  - Predictive back `enabled = enablePredictiveBack && canGoBack && !hasActiveHandlers`
  - When `hasActiveHandlers` is true, a simple (non-predictive) `NavigateBackHandler` handles back via `navigator.onBack()`
  - `hasActiveHandlers` is derived reactively (recomposes when registry state changes)
  - When handler is unregistered (screen changes, container destroyed), predictive back re-enables automatically

#### Task 3.3: Make `BackHandlerRegistry` Observable for Compose Reactivity

- **Description:** `BackHandlerRegistry.hasHandlers(key)` needs to trigger recomposition when handlers are added/removed. Add a `Compose State` or `StateFlow` that tracks handler count per key, so `NavigationHost` can use it in a derived state.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/BackHandlerRegistry.kt`
- **Dependencies:** Task 1.1
- **Acceptance Criteria:**
  - `hasHandlersState(key: NodeKey): State<Boolean>` or similar reactive API
  - OR: Use `mutableStateOf` internally so that `hasHandlers()` reads trigger Compose snapshot tracking
  - Registration/unregistration updates the observable state
  - `NavigationHost` can reactively check this in `derivedStateOf { ... }`

---

### Phase 4: Provide User-Facing Back Handler Registration APIs

#### Task 4.1: Create `NavBackHandler` Composable in Core

- **Description:** Create a `NavBackHandler` composable that registers a handler with `BackHandlerRegistry` scoped to the current `LocalScreenNode`. Uses `DisposableEffect` for auto-registration and cleanup.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/NavBackHandler.kt` (new file)
- **Dependencies:** Task 1.1
- **Acceptance Criteria:**
  - Signature: `@Composable fun NavBackHandler(enabled: Boolean = true, onBack: () -> Unit)`
  - Reads `LocalScreenNode.current` to get the `NodeKey`
  - Reads `LocalBackHandlerRegistry.current` to register
  - Uses `DisposableEffect(enabled, onBack)` to register/unregister
  - When `enabled = false`, handler is not registered (or unregistered)
  - KDoc with usage examples
  - Public API (exported from the module)

#### Task 4.2: Add `registerBackHandler` to `NavigationContainerScope`

- **Description:** Add a `registerBackHandler(handler: () -> Boolean): () -> Unit` method to `NavigationContainerScope`. It registers with `BackHandlerRegistry` using the screen's `NodeKey` and auto-unregisters when the Koin scope closes (via `ScopeCallback`).
- **Files:**
  - `quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/NavigationContainerScope.kt`
  - `quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/ContainerComposables.kt` (inject registry into scope)
- **Dependencies:** Task 1.1
- **Acceptance Criteria:**
  - `NavigationContainerScope` constructor receives `BackHandlerRegistry` parameter
  - `registerBackHandler(handler: () -> Boolean): () -> Unit` method available
  - Returns unregister function for manual early unregister
  - All handlers auto-unregistered when scope closes (tracked in scope, cleaned up in `ScopeCallback`)
  - `rememberContainer` passes `LocalBackHandlerRegistry.current` when constructing `NavigationContainerScope`
  - KDoc with usage examples

#### Task 4.3: Add `registerBackHandler` to `SharedContainerScope`

- **Description:** Same as Task 4.2 but for `SharedContainerScope`. Uses the container node's key (TabNode/PaneNode) for registration. Note: since shared containers span multiple screens, the key used should be the container's own key, and `handleBack` for shared container handlers should be checked separately (or the active screen lookup should also consider parent container keys).
- **Files:**
  - `quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/SharedContainerScope.kt`
  - `quo-vadis-core-flow-mvi/src/commonMain/kotlin/com/jermey/quo/vadis/flowmvi/SharedContainerComposables.kt` (inject registry)
- **Dependencies:** Task 1.1
- **Implementation Note:** For shared containers, we need to decide the NodeKey strategy. Two options:
  - Use the container's own `NodeKey` and modify `handleBack()` to check both active screen key AND ancestor container keys
  - Register under the active screen key and re-register when active screen changes
  - **Recommended**: Use container's own key + modify `handleBack()` to walk up the tree checking all ancestor keys. This way shared container handlers fire whenever any child screen is active.
- **Acceptance Criteria:**
  - `SharedContainerScope` constructor receives `BackHandlerRegistry` parameter
  - `registerBackHandler(handler: () -> Boolean): () -> Unit` method available
  - Auto-cleanup on scope close
  - `rememberSharedContainer` passes `LocalBackHandlerRegistry.current` when constructing

#### Task 4.4: Extend `handleBack()` to Check Ancestor Node Keys

- **Description:** Modify `BackHandlerRegistry.handleBack()` to accept the full active `NavNode` tree path (or walk up from active screen). Check handlers for the active screen key first, then each ancestor node key up to the root. This enables shared container handlers to fire.
- **Files:**
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/BackHandlerRegistry.kt`
  - `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeNavigator.kt` (update call site)
- **Dependencies:** Task 4.3, Task 1.1
- **Acceptance Criteria:**
  - `handleBack(activeNodePath: List<NodeKey>): Boolean` — checks keys from leaf to root
  - `TreeNavigator.onBack()` computes the active node path from `_state.value` and passes it
  - Active screen handlers checked first (LIFO), then parent tab/pane handlers (LIFO), etc.
  - `hasHandlers(activeNodePath: List<NodeKey>): Boolean` — for predictive back `enabled` check
  - Helper function `NavNode.activeNodePath(): List<NodeKey>` to compute the path from root to active leaf

---

### Phase 5: Tests and Demo Updates

#### Task 5.1: Unit Tests for Scoped BackHandlerRegistry

- **Description:** Write tests for the refactored `BackHandlerRegistry`.
- **Files:**
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/registry/BackHandlerRegistryTest.kt` (new or update existing)
- **Dependencies:** Task 4.4
- **Acceptance Criteria:**
  - Test: handler registered under key A is NOT invoked when `handleBack` is called with key B
  - Test: handler registered under key A IS invoked when `handleBack` is called with path containing A
  - Test: LIFO ordering within a single key
  - Test: unregister function removes individual handler
  - Test: `unregisterAll(key)` removes all handlers for that key
  - Test: `hasHandlers` returns correct state for given path
  - Test: ancestor key walking (shared container scenario)

#### Task 5.2: Integration Tests for `navigateBack()` vs `onBack()` Separation

- **Description:** Write tests proving programmatic `navigateBack()` bypasses handlers while `onBack()` consults them.
- **Files:**
  - `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/internal/tree/TreeNavigatorBackHandlerTest.kt` (new)
- **Dependencies:** Task 2.1, Task 4.4
- **Acceptance Criteria:**
  - Test: `navigateBack()` does NOT trigger registered back handler
  - Test: `onBack()` DOES trigger registered back handler
  - Test: `onBack()` falls through to tree pop when handler returns false
  - Test: `onBack()` stops propagation when handler returns true

#### Task 5.3: Demo App Back Handler Example

- **Description:** Add a demo screen in `composeApp` that showcases `NavBackHandler` usage (e.g., "unsaved changes" confirmation dialog on back).
- **Files:**
  - `composeApp/src/commonMain/kotlin/...` (existing demo screens, add back handler example)
- **Dependencies:** Task 4.1
- **Acceptance Criteria:**
  - Demo screen with a text field and "unsaved changes" state
  - `NavBackHandler` registered when changes exist
  - Back gesture shows confirmation dialog instead of navigating
  - Demonstrates both `NavBackHandler` composable and container scope `registerBackHandler`

---

## Sequencing

```
Phase 1 (Foundation)
  Task 1.1 → Task 1.2

Phase 2 (Core Fix) [depends on Phase 1]
  Task 2.1 → Task 2.2

Phase 3 (Gesture Wiring) [depends on Phase 2]
  Task 3.3 (can start with Phase 1)
  Task 3.1 → Task 3.2

Phase 4 (User APIs) [depends on Phase 1, can parallel with Phase 2-3]
  Task 4.1 (independent, needs only Task 1.1)
  Task 4.2 (independent, needs only Task 1.1)
  Task 4.3 (independent, needs only Task 1.1)
  Task 4.4 (needs Task 4.3)

Phase 5 (Validation) [depends on all above]
  Task 5.1 → Task 5.2 → Task 5.3
```

**Parallelizable groups:**
- After Task 1.1: Tasks 1.2, 3.3, 4.1, 4.2, 4.3 can all start in parallel
- After Task 1.2: Task 2.1
- After Task 2.1: Tasks 2.2, 3.1
- After Task 3.1: Task 3.2
- After Task 4.3: Task 4.4

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Breaking change in `BackHandlerRegistry` public API | Medium — users calling `register()` directly need to pass `NodeKey` | Mark old signatures `@Deprecated` with migration path; `NavBackHandler` composable abstracts this away |
| Predictive back UX degradation when handlers exist | Low — no gesture preview shown | This is the agreed behavior; document clearly that custom back handlers disable predictive back animation |
| `SharedContainerScope` key strategy complexity | Medium — ancestor key walking adds complexity to `handleBack()` | Keep the tree walk simple (just collect keys from `activeLeaf` to root); cache the path |
| Compose snapshot thread safety for `BackHandlerRegistry` state | Low — registry only accessed from main/UI thread | Document thread safety contract; use `mutableStateOf` for Compose reactivity |
| Speculative pop state divergence | Medium — `onBackCompleted` now calls `onBack()` which may produce different state than speculative | For the non-handler case, `onBack()` produces the same tree pop result; for handler case, gesture is disabled, so no speculative state |

## Open Questions

1. **Deprecation strategy**: Should the old `BackHandlerRegistry.register(handler)` (without key) be kept as deprecated or removed immediately? Recommendation: deprecate with `@Deprecated` and `ReplaceWith`.
2. **SharedContainerScope handler priority**: If both a screen handler and its parent shared container handler are registered, should the screen handler take priority? Recommendation: yes, LIFO within each key, leaf-to-root key ordering.
3. **Multiple active screens** (Pane layouts): In expanded pane mode, there may be two active screens. Should `handleBack()` check both pane screens' handlers? Recommendation: check only the primary/focused pane's active screen, but this needs investigation into how pane focus works.
