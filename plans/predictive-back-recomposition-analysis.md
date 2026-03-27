# Predictive Back Gesture Recomposition Analysis

## Problem Statement

When a user performs a predictive back gesture, the screen underneath the current one gets recomposed (separate composition branches are created), causing flickers and animations being retriggered unnecessarily. This is similar to the issue PR #69 fixed for modal push/pop rendering.

---

## 1. How Predictive Back Rendering Currently Works

### Gesture Flow (end-to-end)

```
User swipes back
  → NavigateBackHandler fires onBackProgress
    → NavigationHost.onBackProgress:
        1. TreeMutator.popWithTabBehavior(navState) → speculative next state
        2. calculateCascadeBackState(navState) → CascadeBackState
        3. predictiveBackController.startGestureWithCascade(cascadeState)
    → AnimatedNavContent detects isPredictiveBackActive = true
      → Switches from AnimatedContent to PredictiveBackContent
        → Renders BOTH current + previous in a Box with graphicsLayer transforms
```

### Key Composable Chain

```
NavigationHost
  └── NavigateBackHandler (gesture integration)
        └── NavNodeRenderer (dispatches by node type)
              └── StackRenderer (for StackNode)
                    └── AnimatedNavContent (core animation wrapper)
                          ├── [Normal] AnimatedContent → content(targetState)
                          ├── [Modal] StaticAnimatedVisibilityScope → content(targetState)
                          └── [Predictive Back] PredictiveBackContent
                                ├── Box: content(previous) — parallax, behind
                                └── Box: content(current) — slides right + scales down
```

### State Tracking in AnimatedNavContent

`AnimatedNavContent` maintains two key variables:
- **`lastCommittedState`**: The node currently shown on screen (updated after successful composition)
- **`stateBeforeLast`**: The previous node (used as the gesture target when no `cascadeState.targetNode` is available)

When predictive back is active:
- `current` = `lastCommittedState` (the screen being dismissed)
- `previous` = `cascadeState.targetNode` or `stateBeforeLast` (the screen being revealed)

---

## 2. How Current and Previous Screens Are Composed During Predictive Back

### Before Gesture (Normal Rendering)

```
AnimatedNavContent
  └── AnimatedContent(targetState = screenB)   ← only screenB is in composition
        └── NavNodeRenderer(screenB)
              └── ScreenRenderer(screenB)
                    └── CachedEntry(screenB.key)
                          └── SaveableStateProvider(screenB.key)
                                └── screenB composable content
```

Only the **active screen** (screenB) exists in the composition tree. ScreenA is NOT composed.

### After Gesture Starts (Predictive Back Active)

```
AnimatedNavContent
  └── PredictiveBackContent(current=screenB, previous=screenA)
        └── Box {
              ├── Box(parallax) {                          ← NEW composition slot
              │     └── StaticAnimatedVisibilityScope {
              │           └── content(screenA)              ← screenA composed FROM SCRATCH
              │                 └── NavNodeRenderer(screenA)
              │                       └── ScreenRenderer(screenA)
              │                             └── CachedEntry(screenA.key)
              │                                   └── SaveableStateProvider(screenA.key) ← rememberSaveable preserved
              │                                         └── screenA composable content   ← remember/LaunchedEffect re-run!
              │   }
              └── Box(slide+scale) {                       ← NEW composition slot
                    └── StaticAnimatedVisibilityScope {
                          └── content(screenB)              ← screenB RECREATED at new position
                                └── NavNodeRenderer(screenB)
                                      └── ScreenRenderer(screenB)
                                            └── CachedEntry(screenB.key)
                                                  └── SaveableStateProvider(screenB.key)
                                                        └── screenB composable content
                    }
              }
        }
```

---

## 3. Root Cause of Unnecessary Recomposition

### The Composition Tree Restructuring

The core problem is in [AnimatedNavContent.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt#L126-L140):

```kotlin
if (isPredictiveBackActive) {
    // ← BRANCH A: PredictiveBackContent
    PredictiveBackContent(
        current = lastCommittedState,
        previous = backTarget,
        progress = scope.predictiveBackController.progress.value,
        scope = scope,
        content = content
    )
} else if (isTargetModal) {
    // ...
} else {
    // ← BRANCH B: AnimatedContent
    AnimatedContent(targetState = targetState, ...) { animatingState ->
        scope.WithAnimatedVisibilityScope(this) {
            content(animatingState)
        }
    }
}
```

When the gesture starts, Compose switches from **Branch B** to **Branch A**. This is a **structural change** in the composition tree:

1. **`AnimatedContent` leaves composition** — its entire subtree (including the current screen) is disposed
2. **`PredictiveBackContent` enters composition** — creates two entirely new composition subtrees

### What Gets Destroyed and Recreated

**For the current screen (screenB):**
- `DisposableEffect(node)` in `ScreenRenderer` fires `onDispose { node.detachFromUI() }` when leaving `AnimatedContent`
- Then `DisposableEffect(node)` fires `node.attachToUI()` when entering `PredictiveBackContent`
- All `remember` values are re-initialized
- All `LaunchedEffect`s restart (data fetches, animations, etc.)
- Any ongoing animations restart from initial state

**For the previous screen (screenA):**
- Composed entirely from scratch (it was NOT in the composition tree before)
- `attachToUI()` called
- All `remember` states initialized fresh
- All entrance animations trigger
- All `LaunchedEffect` side effects run

### What IS Preserved

`ComposableCache.CachedEntry` uses `SaveableStateProvider(key)`, so `rememberSaveable` state survives. But this does NOT cover:
- Regular `remember` values
- `LaunchedEffect` side effects (they restart)
- Ongoing `Animatable` / animation state
- `DisposableEffect` lifecycles (they re-run)

### The Concrete Manifestation

Code in [PredictiveBackContent.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PredictiveBackContent.kt#L87-L122):

```kotlin
// Previous (incoming) content - composed FROM SCRATCH
if (previous != null && previous.key != current.key) {
    Box(modifier = Modifier.fillMaxSize().graphicsLayer { ... }) {
        StaticAnimatedVisibilityScope {
            content(previous)  // ← This creates a NEW composition for the previous screen
        }
    }
}

// Current (exiting) content - RECREATED at new tree position
Box(modifier = Modifier.fillMaxSize().graphicsLayer { ... }) {
    StaticAnimatedVisibilityScope {
        content(current)  // ← This creates a NEW composition for the current screen
    }
}
```

Both calls to `content(...)` go through `NavNodeRenderer` → `ScreenRenderer`, which invoke screen composables at entirely new positions in the Compose tree.

---

## 4. How PR #69 Solved the Modal Problem

### The Modal Composition Problem (before PR #69)

When a modal was pushed, `AnimatedContent` would transition from the background screen to the modal. The background screen's `AnimatedContent` exit animation would remove it from composition, causing recomposition when the modal was dismissed.

### PR #69's Solution: Stable Composition Position

The fix in [StackRenderer.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/StackRenderer.kt#L97-L165):

```kotlin
// Key insight: backgroundTarget stays the same when modals are pushed/popped
val backgroundTarget = if (hasModalOverlay) {
    val baseIndex = findNonModalBaseIndex(node.children, modalRegistry)
    node.children[baseIndex]
} else {
    activeChild
}

// ALWAYS render background through AnimatedNavContent
// This keeps the background screen at a STABLE composition tree position
AnimatedNavContent(
    targetState = backgroundTarget,  // Doesn't change when modal pushed!
    // ... 
) { child ->
    NavNodeRenderer(node = child, ...)
}

// Overlay modal nodes as SIBLINGS (outside AnimatedNavContent)
if (hasModalOverlay) {
    for (modalNode in modalNodes) {
        StaticAnimatedVisibilityScope {
            NavNodeRenderer(node = modalNode, ...)
        }
    }
}
```

**Technique**: Keep the background at a **stable composition tree position**. The `backgroundTarget` doesn't change when modals are pushed/popped → `AnimatedContent` sees no state change → no recomposition.

### Key Principle

> **Don't move content between composition tree positions** — instead, keep it at a stable position and render additional content as siblings.

---

## 5. Key Files and Their Roles

| File | Symbol | Role |
|------|--------|------|
| [NavigationHost.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/NavigationHost.kt) | `NavigationHost` | Entry point; creates `PredictiveBackController`; wires up `NavigateBackHandler`; computes `CascadeBackState` at gesture start |
| [StackRenderer.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/StackRenderer.kt) | `StackRenderer` | Renders `StackNode`; determines `predictiveBackEnabled` per stack; delegates to `AnimatedNavContent` |
| [AnimatedNavContent.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt) | `AnimatedNavContent` | **The branch point** — switches between `AnimatedContent` (normal) and `PredictiveBackContent` (gesture active). This is where the composition tree restructuring happens. |
| [PredictiveBackContent.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PredictiveBackContent.kt) | `PredictiveBackContent` | Renders both current and previous screens in a `Box` with `graphicsLayer` transforms (parallax + slide/scale) |
| [PredictiveBackController.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/PredictiveBackController.kt) | `PredictiveBackController` | State machine for gesture lifecycle (idle → gesturing → animating); exposes `isActive`, `progress`, `cascadeState` |
| [NavTreeRenderer.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/NavTreeRenderer.kt) | `NavNodeRenderer` | Recursive dispatcher from `NavNode` type → specialized renderer |
| [ScreenRenderer.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/ScreenRenderer.kt) | `ScreenRenderer` | Leaf renderer; manages `CachedEntry` + `SaveableStateProvider`; calls `attachToUI()`/`detachFromUI()` |
| [PaneRenderer.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PaneRenderer.kt) | `SinglePaneRenderer` | Compact pane mode; has its own `PredictiveBackContent` integration with the **same recomposition issue** |
| [CascadeBackState.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/navback/CascadeBackState.kt) | `CascadeBackState`, `calculateCascadeBackState` | Determines what exits, what's revealed, which stack animates, and cascade depth |
| [NavRenderScope.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/NavRenderScope.kt) | `shouldEnablePredictiveBack` | Determines if a specific node should handle predictive back (avoids conflicts between nested stacks) |
| [ComposableCache.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/ComposableCache.kt) | `ComposableCache` | LRU cache with lock/priority protection; `CachedEntry` wraps content in `SaveableStateProvider` |
| [StaticAnimatedVisibilityScope.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/StaticAnimatedVisibilityScope.kt) | `StaticAnimatedVisibilityScope` | Provides a no-op `AnimatedVisibilityScope` for content outside `AnimatedContent` |
| [NavigateBackHandler.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/navback/NavigateBackHandler.kt) | `NavigateBackHandler` | Platform back gesture integration; feeds progress/cancel/complete to `NavigationHost` |

---

## 6. Affected Node Types

| Node Type | Affected? | Mechanism |
|-----------|-----------|-----------|
| **StackNode** | **Yes (primary)** | `AnimatedNavContent` in `StackRenderer` switches to `PredictiveBackContent` — both current and previous screens get new composition slots |
| **PaneNode (compact)** | **Yes** | `SinglePaneRenderer` has its own direct `PredictiveBackContent` call with the same issue — the active pane content is removed from `AnimatedNavContent` and recreated inside `PredictiveBackContent` |
| **TabNode** | **Indirectly** | Tab switching itself doesn't use predictive back (`predictiveBackEnabled = false` in `TabRenderer`). But cascade back through a parent stack CAN animate the entire `TabNode` via the parent's `PredictiveBackContent`, causing the tab wrapper + content to be recreated |
| **ScreenNode** | **Always affected** | As leaf nodes, screens are the ones that visually flicker. Every `ScreenRenderer` re-runs `attachToUI()`, `DisposableEffect`, `LaunchedEffect`, and `remember` when the composition slot changes |

---

## 7. Potential Solution Direction: `movableContentOf`

### Why `movableContentOf`?

Compose's `movableContentOf` API allows moving a composable between different positions in the composition tree **without losing state**. When content is "moved," Compose preserves:
- All `remember` values
- All `LaunchedEffect` / `DisposableEffect` states (no re-trigger)
- Animation states
- The entire composition subtree

### Conceptual Approach

Currently, `movableContentOf` is **not used anywhere** in the codebase. Introducing it would allow:

1. **Wrapping screen rendering in movable content**: Create a `movableContentOf` for each screen, keyed by `NavNode.key`
2. **Moving (not recreating) the current screen** when switching from `AnimatedContent` to `PredictiveBackContent`
3. **Pre-caching previous screen content** using the `ComposableCache` to keep it alive between navigations

### Key Challenge

The branch switch in `AnimatedNavContent` from `AnimatedContent` → `PredictiveBackContent` is the structural change. One alternative to `movableContentOf` would be to follow PR #69's "stable position" pattern:

- **Always render both current and previous** but control visibility/transforms
- During normal navigation: show only current (previous has `alpha = 0` or is out of viewport)
- During predictive back: transform both with gesture progress
- This avoids any composition tree restructuring entirely

### Trade-off Comparison

| Approach | Pros | Cons |
|----------|------|------|
| `movableContentOf` | Surgical fix; only changes what's needed; works with existing branching | Complex lifecycle management; needs movable content registry; careful timing at branch switch |
| Stable position (PR #69 style) | Proven pattern; no composition restructuring ever; simple mental model | May increase baseline composition cost (always composing the previous screen); needs careful visibility management |

---

## 8. Summary Diagram

```
NORMAL RENDERING                    PREDICTIVE BACK ACTIVE
═══════════════                    ══════════════════════

AnimatedNavContent                 AnimatedNavContent
│                                  │
├─ AnimatedContent ──────┐         ├─ PredictiveBackContent ──┐
│  └─ screenB (ACTIVE)   │   →→→   │  ├─ Box(parallax)        │
│     └─ remember ✓      │         │  │  └─ screenA (NEW!)     │ ← composed from scratch
│     └─ LaunchedEffect ✓│         │  │     └─ remember INIT   │
│     └─ animations ✓    │         │  │     └─ LaunchedEffect ! │ ← re-triggers!
│                         │         │  └─ Box(slide+scale)      │
│ [screenA NOT in tree]   │         │     └─ screenB (MOVED!)   │ ← new tree slot
│                         │         │        └─ remember INIT   │ ← re-initialized!
└─────────────────────────┘         │        └─ LaunchedEffect ! │ ← re-triggers!
                                    └──────────────────────────────┘

                          ↑ COMPOSITION TREE RESTRUCTURING ↑
                          This is the root cause of the flickers
```

---

## Open Questions

1. **Performance impact of "always compose both"**: If using the stable-position approach, how much overhead does composing an invisible previous screen add? Need benchmarking.
2. **`movableContentOf` compatibility**: Does `movableContentOf` work correctly with `SaveableStateProvider` and `AnimatedContent` in current Compose Multiplatform versions?
3. **Cascade back complexity**: For cascade scenarios (entire TabNode being removed), the "previous" is a completely different tree structure. How should this be handled?
4. **`SinglePaneRenderer` alignment**: Should the `SinglePaneRenderer` fix be identical to the `StackRenderer` fix, or does pane switching have unique considerations?
