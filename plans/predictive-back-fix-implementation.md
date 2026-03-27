# Implementation Plan: Predictive Back Composition Retention Fix

## Overview

This plan implements **Option E: Eliminate the Branch Switch** from the [design analysis](predictive-back-composition-retention-design.md). The core problem is that activating a predictive back gesture switches the composition tree from an `AnimatedContent` branch to a `PredictiveBackContent` branch, destroying and recreating all screens — losing `remember`, `LaunchedEffect`, ongoing animations, and other ephemeral state.

The fix keeps the current screen **always inside `AnimatedContent`** (stable composition position), applies gesture transforms via `graphicsLayer` inside the content lambda, renders the previous screen as a sibling underlay, and deletes `PredictiveBackContent.kt`.

## Requirements (Validated)

| # | Requirement | Status |
|---|-------------|--------|
| R1 | Current screen composition must be retained (zero recomposition) when predictive back gesture starts | Core goal |
| R2 | Gesture cancellation must not recompose the current screen | Follows from R1 |
| R3 | Gesture completion must transition smoothly without visible flash/glitch | Edge case handling |
| R4 | Previous screen may compose from scratch (acceptable trade-off) | User decision |
| R5 | Cache entries for both current and previous screen must be locked during gesture | User decision |
| R6 | Visual transforms: PARALLAX_FACTOR=0.15f, SCALE_FACTOR=0.15f (unchanged) | User decision |
| R7 | Modal destinations must also receive predictive back transforms | Current behavior preserved |
| R8 | `SinglePaneRenderer` must delegate predictive back to `AnimatedNavContent` (remove duplication) | Simplification goal |
| R9 | `PredictiveBackContent.kt` must be deleted | Cleanup goal |

## Technical Approach

### Composition Tree: Before vs. After

```
BEFORE:                                          AFTER:

AnimatedNavContent                               AnimatedNavContent
│                                                │
├─ if (gestureActive) {                          ├─ if (gestureActive && backTarget) {
│    PredictiveBackContent ──┐                   │    Box(parallax graphicsLayer) {
│    ├─ Box(parallax)        │                   │      StaticScope { content(previous) }
│    │  └─ content(prev)     │ ← recomposed!     │    }
│    └─ Box(slide)           │                   │  }                ← sibling underlay
│       └─ content(curr)     │ ← recomposed!     │
│  }                                             ├─ AnimatedContent(target) {  ← ALWAYS HERE
│                                                │    Box(gesture ? graphicsLayer : none) {
├─ else if (modal) { … }                        │      WithAnimatedVisibilityScope {
│                                                │        content(state)  ← NEVER MOVES
├─ else {                                        │      }
│    AnimatedContent(target)                     │    }
│      └─ content(curr)  ← only here             │  }
│  }                                             │
└── stateTracking                                └── stateTracking
```

**Key invariant:** The current screen exists at a single stable composition tree position (inside `AnimatedContent`) regardless of whether predictive back is active. No branch switch = no composition destruction.

### Design Principles

1. **Current screen never moves in the composition tree** → zero recomposition during gesture
2. **Previous screen as sibling underlay** → follows the PR #69 modal pattern (inverted: modal = overlay ON TOP, back target = underlay BEHIND)
3. **`graphicsLayer` for all transforms** → GPU-only, zero composition cost
4. **`AnimatedContent` transition suppressed during gesture** → `EnterTransition.None togetherWith ExitTransition.None`
5. **One-frame `recentlyCompletedGesture` flag** → prevents standard transition from running at completion boundary

---

## Phase 1: Core Refactor — AnimatedNavContent

### Task 1: Eliminate the branch switch in AnimatedNavContent

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt`

**Description:**
Refactor the `AnimatedNavContent` function to remove the three-way `if/else if/else` branch (`isPredictiveBackActive` → `PredictiveBackContent`, `isTargetModal` → `StaticAnimatedVisibilityScope`, else → `AnimatedContent`). Replace with a unified structure:

1. **Resolve back target** — extract `backTarget` from `cascadeState.targetNode` or `stateBeforeLast` (only when gesture active)
2. **Underlay Box** — render previous screen as a sibling Box BEFORE the main content block, only when `isPredictiveBackActive && backTarget != null && backTarget.key != lastCommittedState.key`. Apply parallax via `graphicsLayer { translationX = -size.width * PARALLAX_FACTOR * (1f - progress) }`. Wrap content in `StaticAnimatedVisibilityScope`.
3. **Modal path** — keep `isTargetModal` rendering via `StaticAnimatedVisibilityScope`, but wrap in a `Box` with `graphicsLayer` slide+scale transforms when `isPredictiveBackActive`
4. **AnimatedContent path** (main):
   - **`transitionSpec`**: check `isPredictiveBackActive || recentlyCompletedGesture` → use `EnterTransition.None togetherWith ExitTransition.None`; otherwise use standard `transition.createTransitionSpec(isBack = isBackNavigation)`
   - **Content lambda**: wrap `content(animatingState)` in a `Box` with conditional `graphicsLayer` for slide+scale transforms during gesture
5. **`recentlyCompletedGesture` tracking** — a `var` state that becomes `true` when `isPredictiveBackActive` transitions from true→false, and resets via `SideEffect` after one frame
6. **State tracking** — move OUTSIDE the `if/else` branches so it executes regardless of which path renders. Currently duplicated in both `isTargetModal` and `AnimatedContent` branches.
7. **Move constants** — bring `PARALLAX_FACTOR` (0.15f) and `SCALE_FACTOR` (0.15f) into this file as `private const val`.

**Detailed code structure:**

```kotlin
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    isBackNavigation: Boolean,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    isTargetModal: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    // ── State tracking ──
    var lastCommittedState by remember { mutableStateOf(targetState) }
    var stateBeforeLast by remember { mutableStateOf<T?>(null) }

    val isPredictiveBackActive = predictiveBackEnabled &&
        scope.predictiveBackController.isActive.value
    val progress = scope.predictiveBackController.progress.value

    // ── Recently-completed gesture flag (one-frame) ──
    var recentlyCompletedGesture by remember { mutableStateOf(false) }
    val wasActive = remember { mutableStateOf(false) }
    if (isPredictiveBackActive && !wasActive.value) {
        wasActive.value = true
    }
    if (!isPredictiveBackActive && wasActive.value) {
        recentlyCompletedGesture = true
        wasActive.value = false
    }
    SideEffect {
        if (recentlyCompletedGesture) recentlyCompletedGesture = false
    }

    // ── Resolve back target ──
    val backTarget: T? = if (isPredictiveBackActive) {
        val cascadeState = scope.predictiveBackController.cascadeState.value
        @Suppress("UNCHECKED_CAST")
        (cascadeState?.targetNode as? T) ?: stateBeforeLast
    } else null

    // ── LAYER 1: Underlay (previous screen, only during gesture) ──
    if (isPredictiveBackActive && backTarget != null &&
        backTarget.key != lastCommittedState.key) {
        Box(
            modifier = Modifier.fillMaxSize().graphicsLayer {
                translationX = -size.width * PARALLAX_FACTOR * (1f - progress)
            }
        ) {
            StaticAnimatedVisibilityScope {
                content(backTarget)
            }
        }
    }

    // ── LAYER 2: Main content (stable composition position) ──
    if (isTargetModal) {
        Box(
            modifier = if (isPredictiveBackActive) Modifier.fillMaxSize().graphicsLayer {
                translationX = size.width * progress
                val scale = 1f - (progress * SCALE_FACTOR)
                scaleX = scale; scaleY = scale
            } else Modifier
        ) {
            StaticAnimatedVisibilityScope {
                content(targetState)
            }
        }
    } else {
        AnimatedContent(
            targetState = targetState,
            contentKey = { it.key },
            transitionSpec = {
                if (isPredictiveBackActive || recentlyCompletedGesture) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    transition.createTransitionSpec(isBack = isBackNavigation)
                }
            },
            modifier = modifier,
            label = "AnimatedNavContent"
        ) { animatingState ->
            Box(
                modifier = if (isPredictiveBackActive) Modifier.fillMaxSize().graphicsLayer {
                    translationX = size.width * progress
                    val scale = 1f - (progress * SCALE_FACTOR)
                    scaleX = scale; scaleY = scale
                } else Modifier
            ) {
                scope.WithAnimatedVisibilityScope(this@AnimatedContent) {
                    content(animatingState)
                }
            }
        }
    }

    // ── State tracking (unified, always runs) ──
    if (targetState != lastCommittedState) {
        if (targetState.key != lastCommittedState.key) {
            stateBeforeLast = lastCommittedState
        }
        lastCommittedState = targetState
    }
}

private const val PARALLAX_FACTOR = 0.15f
private const val SCALE_FACTOR = 0.15f
```

**Acceptance Criteria:**
- [ ] `AnimatedNavContent` has NO reference to `PredictiveBackContent`
- [ ] Current screen stays in `AnimatedContent` slot when gesture activates (verifiable by test: `remember` state preserved)
- [ ] Gesture cancellation returns `graphicsLayer` to identity without recomposition
- [ ] Gesture completion suppresses `AnimatedContent` transition for one frame
- [ ] PARALLAX_FACTOR and SCALE_FACTOR constants defined in AnimatedNavContent.kt
- [ ] KDoc updated to remove reference to `PredictiveBackContent` delegation, document the underlay pattern instead
- [ ] Underlay only composed when gesture is active (zero cost at rest)

### Task 2: Add cache locking during predictive back gesture

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt`

**Description:**
Add a `DisposableEffect` inside `AnimatedNavContent` that locks both the current screen and back target keys in the `ComposableCache` when the gesture is active. This prevents cache eviction during the gesture, ensuring both screens remain renderable. Unlock on dispose.

**Key API details:**
- `scope.cache` is `ComposableCache` (from `NavRenderScope`)
- `ComposableCache.lock(key: String)` / `ComposableCache.unlock(key: String)` — String param
- `NavNode.key` is `NodeKey` (inline value class) — access `.value` for the String

**Code:**

```kotlin
// Inside AnimatedNavContent, after resolving backTarget:
if (isPredictiveBackActive && backTarget != null) {
    DisposableEffect(backTarget.key) {
        scope.cache.lock(backTarget.key.value)
        scope.cache.lock(lastCommittedState.key.value)
        onDispose {
            scope.cache.unlock(backTarget.key.value)
            scope.cache.unlock(lastCommittedState.key.value)
        }
    }
}
```

**Note:** The `DisposableEffect` key is `backTarget.key` so it re-runs if the back target changes (shouldn't happen during a single gesture, but safe). The `lastCommittedState` is stable during the gesture because state tracking doesn't update `lastCommittedState` while the gesture is active (targetState doesn't change until completion).

**Acceptance Criteria:**
- [ ] Both current and back target keys are locked when gesture starts
- [ ] Both keys are unlocked when gesture ends (via dispose)
- [ ] No crash from double-lock or lock on non-existent key (lock/unlock are idempotent set operations)

---

## Phase 2: Simplify Callers

### Task 3: Simplify SinglePaneRenderer

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PaneRenderer.kt`  
**Function:** `SinglePaneRenderer` (lines 218–324)

**Description:**
Remove the duplicated predictive back handling from `SinglePaneRenderer`. Currently it has:
- `lastCommittedContent` / `lastCommittedRole` state variables (for its own predictive back tracking)
- An `if (isPredictiveBackActive && primaryPaneContent != null) { PredictiveBackContent(...) } else { AnimatedNavContent(...) }` branch

After the fix, `AnimatedNavContent` handles everything internally. `SinglePaneRenderer` should:

1. **Remove** `lastCommittedContent` and `lastCommittedRole` state variables
2. **Remove** the `if/else` branch for predictive back
3. **Always** call `AnimatedNavContent` with `predictiveBackEnabled` set based on whether this pane is the gesture target
4. **Remove** the `PredictiveBackContent` import

**Computing `predictiveBackEnabled`:**
```kotlin
val predictiveBackEnabled = scope.predictiveBackController.let { ctrl ->
    val cascadeState = ctrl.cascadeState.value
    !ctrl.isActive.value ||  // Not in a gesture → enable (lets AnimatedNavContent decide)
        (cascadeState != null && cascadeState.animatingStackKey == node.key)  // This pane is the target
}
```

Logic: `predictiveBackEnabled = true` means "AnimatedNavContent is allowed to handle predictive back." It should be true when (a) no gesture is active (default behavior), or (b) the gesture targets this specific pane node. It should be false only when a gesture is active but targets a *different* node (to prevent this pane from reacting).

**Simplified code:**
```kotlin
@Composable
private fun SinglePaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope
) {
    val activePaneContent = node.activePaneContent ?: return
    val previousActivePaneContent = previousNode?.activePaneContent

    val transition = scope.animationCoordinator.getPaneTransition(
        fromRole = previousNode?.activePaneRole,
        toRole = node.activePaneRole
    )

    val isBackNavigation = previousNode != null &&
        previousNode.activePaneRole != PaneRole.Primary &&
        node.activePaneRole == PaneRole.Primary

    val predictiveBackEnabled = scope.predictiveBackController.let { ctrl ->
        val cascadeState = ctrl.cascadeState.value
        !ctrl.isActive.value ||
            (cascadeState != null && cascadeState.animatingStackKey == node.key)
    }

    AnimatedNavContent(
        targetState = activePaneContent,
        transition = transition,
        isBackNavigation = isBackNavigation,
        scope = scope,
        predictiveBackEnabled = predictiveBackEnabled,
        modifier = Modifier
    ) { paneNavNode ->
        NavNodeRenderer(
            node = paneNavNode,
            previousNode = previousActivePaneContent,
            scope = scope
        )
    }
}
```

**Acceptance Criteria:**
- [ ] `SinglePaneRenderer` has NO reference to `PredictiveBackContent`
- [ ] `SinglePaneRenderer` has NO `lastCommittedContent` or `lastCommittedRole` state
- [ ] `SinglePaneRenderer` always delegates to `AnimatedNavContent`
- [ ] `predictiveBackEnabled` is correctly computed (true when no gesture OR when this pane is the target)
- [ ] KDoc updated to reflect that predictive back is delegated to `AnimatedNavContent`
- [ ] `@Suppress("VariableNeverRead", "AssignedValueIsNeverRead")` annotation removed (no longer needed)
- [ ] Pane switching animations still work correctly when no gesture is active

### Task 4: Delete PredictiveBackContent.kt

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PredictiveBackContent.kt`

**Description:**
Delete the entire file. All its logic (underlay rendering, `graphicsLayer` transforms, constants) has been inlined into `AnimatedNavContent`. Verify no remaining references exist.

**Pre-deletion checklist:**
- [ ] `AnimatedNavContent.kt` no longer imports `PredictiveBackContent`
- [ ] `PaneRenderer.kt` (`SinglePaneRenderer`) no longer imports `PredictiveBackContent`
- [ ] `PARALLAX_FACTOR` and `SCALE_FACTOR` constants migrated to `AnimatedNavContent.kt`
- [ ] No other files reference `PredictiveBackContent` (verify via find references)

**Acceptance Criteria:**
- [ ] File deleted
- [ ] Project compiles without errors
- [ ] No dangling imports referencing the deleted file

---

## Phase 3: Tests

### Task 5: Update PredictiveBackContentTest

**File:** `quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackContentTest.kt`

**Description:**
Update the existing test class (~396 lines, ~20 tests) to test the new underlay pattern inside `AnimatedNavContent` instead of the deleted `PredictiveBackContent` function. The test class may be renamed to `PredictiveBackAnimationTest` to reflect the broader scope.

**Test categories to update/add:**

#### Existing tests to migrate:
Most existing tests verify transform values and state tracking. These should be adapted to call `AnimatedNavContent` (with `predictiveBackEnabled = true`) instead of `PredictiveBackContent` directly.

- **Transform value tests** — verify parallax on underlay and slide+scale on current screen via `graphicsLayer` semantics
- **Progress tracking tests** — verify transforms respond correctly to progress values 0.0, 0.5, 1.0
- **Previous screen guard** — verify underlay only renders when `backTarget.key != lastCommittedState.key`
- **Null previous** — verify no underlay when `backTarget` is null

#### New tests to add:

| Test | Description | Priority |
|------|-------------|----------|
| Composition retained during gesture start | Set up `AnimatedContent` with a screen that has `remember` state. Activate gesture. Verify `remember` state is NOT lost. **This is the key regression test for this fix.** | P0 |
| Gesture cancellation preserves composition | Start gesture, cancel (progress → 0). Verify current screen's `remember` state is intact, no recomposition count increase. | P0 |
| Gesture completion smooth transition | Complete gesture. Verify no visible flash (AnimatedContent transition suppressed via `recentlyCompletedGesture`). | P1 |
| Cache locking during gesture | Verify `scope.cache.lock()` is called for both keys when gesture starts, `unlock()` called when it ends. | P1 |
| SinglePaneRenderer delegates predictive back | Verify `SinglePaneRenderer` no longer handles predictive back directly — gesture transforms appear inside `AnimatedNavContent`. | P2 |
| Underlay not composed at rest | When no gesture is active, verify zero overhead: no underlay Box in the composition tree. | P1 |
| Transition suppression during gesture | Verify `AnimatedContent` uses `None` transition when gesture is active. | P1 |

**Acceptance Criteria:**
- [ ] All migrated tests pass
- [ ] New composition retention test (P0) passes
- [ ] New gesture cancellation test (P0) passes
- [ ] No test references `PredictiveBackContent` (deleted function)
- [ ] `PredictiveBackControllerTest.kt` untouched and still passing

---

## Phase 4: Documentation

### Task 6: Update KDoc

**Files:**
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt`
- `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PaneRenderer.kt`

**Description:**
1. **AnimatedNavContent KDoc**: Replace the "Predictive Back Integration" section. Remove "bypasses `AnimatedContent` entirely and delegates to `PredictiveBackContent`". Replace with description of the underlay pattern: gesture transforms applied via `graphicsLayer` inside the content lambda; previous screen rendered as a sibling underlay behind main content; `AnimatedContent` transitions suppressed during gesture. Remove `@see PredictiveBackContent` reference.

2. **SinglePaneRenderer KDoc**: Update the "Predictive Back" section to state that predictive back is delegated to `AnimatedNavContent` via the `predictiveBackEnabled` parameter. Remove mention of gesture-driven animation handling at the pane level.

**Acceptance Criteria:**
- [ ] No KDoc references to `PredictiveBackContent`
- [ ] Underlay pattern documented in AnimatedNavContent
- [ ] Cache locking behavior documented

---

## Task Sequencing

```
┌─────────────────────────────┐
│ Task 1: Refactor             │  ← Core change: eliminate branch switch
│ AnimatedNavContent           │     in AnimatedNavContent
│                              │
│ Task 2: Cache locking        │  ← Can be done simultaneously with Task 1
│ (same file)                  │     (adds DisposableEffect to same function)
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Task 4: Delete               │  ← After Task 1 (no more references)
│ PredictiveBackContent.kt     │
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Task 3: Simplify             │  ← After Tasks 1 & 4
│ SinglePaneRenderer           │     (PredictiveBackContent import removed)
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Task 5: Update tests         │  ← After all implementation tasks
└──────────────┬──────────────┘
               │
               ▼
┌─────────────────────────────┐
│ Task 6: Update KDoc          │  ← After all tasks
└─────────────────────────────┘
```

**Practical grouping:** Tasks 1+2 can be implemented as a single commit (same file). Task 4 should be a separate commit for clean git history. Task 3 can be its own commit. Tasks 5+6 can be combined.

---

## Files Changed Summary

| File | Change Type | Lines (est.) |
|------|-------------|-------------|
| [AnimatedNavContent.kt](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt) | **Major refactor** — eliminate branch switch, add underlay, add graphicsLayer, add cache locking, add transition suppression, move constants | ~190 → ~170 |
| [PredictiveBackContent.kt](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PredictiveBackContent.kt) | **Delete** | -145 lines |
| [PaneRenderer.kt](../quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PaneRenderer.kt) (SinglePaneRenderer) | **Simplify** — remove duplicated predictive back logic, always delegate to AnimatedNavContent | ~107 → ~50 |
| [PredictiveBackContentTest.kt](../quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackContentTest.kt) | **Update** — migrate tests to new pattern, add composition retention tests | ~396 → ~350 |

**Files NOT changed:**
- `StackRenderer.kt` — already delegates correctly to `AnimatedNavContent`
- `ScreenRenderer.kt` — uses `CachedEntry(key)` with `SaveableStateProvider`, orthogonal
- `ComposableCache.kt` — `lock`/`unlock` API already exists, no changes
- `NavRenderScope.kt` — `shouldEnablePredictiveBack` unchanged
- `PredictiveBackControllerTest.kt` — tests the state machine, not rendering
- `PredictiveBackController.kt` — no changes to the controller itself

---

## Risks & Mitigations

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|-----------|--------|------------|
| 1 | `AnimatedContent` runs exit animation on stale content during gesture completion, causing a visual flash | Medium | Visual glitch | `recentlyCompletedGesture` one-frame flag suppresses transition. If insufficient, extend to 2 frames or use `LaunchedEffect` with `awaitFrame()`. |
| 2 | Dual `SaveableStateProvider` with same key (underlay + AnimatedContent) causes crash | Low | Crash | `SaveableStateHolder` uses key-based lookup; two providers with the same key share state safely. Verified in design analysis. Covered by test. |
| 3 | Previous screen's `LaunchedEffect`s trigger during gesture (API calls, analytics) | Medium | Unwanted side effects | This exists in the current implementation too — no regression. Document as known limitation for future lifecycle-awareness improvement. |
| 4 | `graphicsLayer` transform doesn't apply to popups/dialogs displayed by the screen | Low | Visual artifact | All screen content is within the Box that has `graphicsLayer`. Compose dialogs use separate windows and are naturally unaffected. |
| 5 | `AnimatedContent` recomposition during gesture due to `progress` state reads in composition | Low | Performance | `progress` is read inside `graphicsLayer` lambda (layout/draw phase, not composition). `isPredictiveBackActive` causes one recomposition when gesture starts — same as current behavior. |
| 6 | `SinglePaneRenderer` `predictiveBackEnabled` logic incorrect, causing pane to not animate or to animate when it shouldn't | Medium | Broken pane back gesture | The boolean logic `!isActive || (cascade targets this pane)` is clear. Test with: (a) no gesture, (b) gesture targets this pane, (c) gesture targets different node. |
| 7 | Detekt/lint violations from new code structure | Low | CI failure | Run `./gradlew detekt` before committing. Update `detekt-baseline.xml` if needed. |

---

## Validation Plan

After implementation, verify:

1. **Unit tests:** `./gradlew allTests` — all existing + new tests pass
2. **Detekt:** `./gradlew detekt` — no new violations
3. **Manual test (Android):** Navigate A→B, start predictive back gesture, verify B's UI state preserved (e.g., scroll position, text input), cancel gesture, verify B unchanged, complete gesture, verify smooth transition to A
4. **Manual test (iOS):** Same flow with swipe-back gesture
5. **Manual test (Desktop):** Verify no regression in standard back navigation
6. **Edge cases:** Nested stacks in tabs, cascade back through TabNode, rapid gesture after completion
