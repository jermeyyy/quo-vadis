# Design: Retaining Composition During Predictive Back Gestures

## Problem Statement

When a predictive back gesture activates, `AnimatedNavContent` switches from its `AnimatedContent` branch to the `PredictiveBackContent` branch. This structural change in the composition tree causes:

1. **Current screen (screenB)** — destroyed inside `AnimatedContent`, recreated inside `PredictiveBackContent`'s Box
2. **Previous screen (screenA)** — composed from scratch (was not in the tree at all)
3. **All `remember`, `LaunchedEffect`, `DisposableEffect`, ongoing animations** — reset, causing flickers

The `SaveableStateProvider` inside `CachedEntry` preserves `rememberSaveable`, but regular `remember`, effects, and animations are lost.

---

## Option Assessment

### Option A: `movableContentOf` Per Screen

**Description:** Wrap each screen's composition in `movableContentOf`, keyed by `NavNode.key`. When predictive back activates, "move" the existing composition from `AnimatedContent`'s slot to `PredictiveBackContent`'s Box.

**How It Works:**
```kotlin
// In AnimatedNavContent, create movable content per node key
val movableScreenContent = remember(node.key) {
    movableContentOf { content(node) }
}
// In both AnimatedContent slot and PredictiveBackContent, call the same movableScreenContent()
```

**Assessment:**

| Aspect | Evaluation |
|--------|------------|
| Current screen retention | ✅ Full — `movableContentOf` preserves all state when content moves between tree positions |
| Previous screen retention | ❌ Partial — Previous screen was NOT in the composition tree. It must be composed from scratch regardless. `movableContentOf` can only move content that is currently composed |
| AnimatedContent compatibility | ⚠️ Problematic — `AnimatedContent` manages its own keyed content slots internally. Injecting `movableContentOf` inside its content lambda means the movable content exits `AnimatedContent`'s managed scope during the move, conflicting with `AnimatedContent`'s internal content lifecycle management |
| Implementation complexity | 🔴 High — Requires a registry of movable content instances synchronized with node lifecycle. Timing at the branch switch point is critical: the movable content must be "taken" from `AnimatedContent` *before* it's destroyed, then placed in `PredictiveBackContent`. But Compose processes the branch switch atomically in one frame |
| Memory overhead | ✅ Minimal — Only screens currently in the tree have movable content instances |
| Risk | 🔴 High — `AnimatedContent` is not designed to have its content slots externally managed. Moving content out of an `AnimatedContent` during a transition may leave `AnimatedContent` in an inconsistent state |

**Verdict:** ❌ **Not recommended.** The fundamental issue is that `movableContentOf` cannot solve the "previous screen from scratch" problem, and it conflicts with `AnimatedContent`'s internal content management. Solves only half the problem with high complexity and risk.

---

### Option B: Always-Render Pattern (PR #69 Style)

**Description:** Keep both current and previous screens always composed at stable positions in the tree. During normal navigation, the previous screen is composed but invisible (`graphicsLayer { alpha = 0f }`). When predictive back starts, just change transforms — no composition restructuring.

**How It Works:**
```kotlin
// Always render previous as a sibling BEHIND AnimatedContent
if (previousScreen != null) {
    Box(
        modifier = Modifier.fillMaxSize().graphicsLayer {
            alpha = if (isPredictiveBackActive) 1f else 0f
            translationX = if (isPredictiveBackActive) -size.width * PARALLAX * (1f - progress) else 0f
        }
    ) {
        StaticAnimatedVisibilityScope { content(previousScreen) }
    }
}

// Current screen in AnimatedContent (stable position)
AnimatedContent(targetState = currentScreen, ...) { state ->
    Box(modifier = Modifier.graphicsLayer {
        if (isPredictiveBackActive) {
            translationX = size.width * progress
            scaleX = 1f - progress * SCALE_FACTOR
            scaleY = 1f - progress * SCALE_FACTOR
        }
    }) {
        content(state)
    }
}
```

**Assessment:**

| Aspect | Evaluation |
|--------|------------|
| Current screen retention | ✅ Full — Stays at the same composition tree position forever |
| Previous screen retention | ⚠️ Partial for remember/effects — Previous screen is composed from scratch when first pushed to the "previous" slot. But once composed, it stays. Effects run once at composition, not again when gesture starts |
| AnimatedContent compatibility | ✅ Full — Current screen stays inside `AnimatedContent` as before |
| Implementation complexity | 🟡 Medium — Pattern proven by PR #69 for modals. Previous screen as underlay is the inverse of modal overlay |
| Memory overhead | 🔴 Non-trivial — Previous screen is ALWAYS in composition, even when invisible. For simple screens this is negligible, but screens with heavy content (maps, video, large lists) add continuous memory/recomposition cost |
| Risk | 🟡 Medium — The previous screen running LaunchedEffects, making API calls, observing Flows while invisible could cause side effects |

**Verdict:** ⚠️ **Partially acceptable, but has side-effect concerns.** The always-on previous screen is wasteful and potentially dangerous (invisible screens triggering business logic). Requires careful lifecycle management to prevent phantom effects.

---

### Option C: Frozen State Pattern

**Description:** When predictive back starts, capture a static snapshot of the current screen's visual output, display the frozen snapshot during the gesture, and compose the previous screen fresh.

**Assessment:**

| Aspect | Evaluation |
|--------|------------|
| Current screen retention | 🟡 Visual-only — A snapshot image, not live composition |
| Previous screen retention | ❌ None — Composed from scratch |
| Implementation complexity | 🔴 Very high — No standard Compose API for visual snapshots. Would require `GraphicsLayer.toImageBitmap()` or `captureToImagBitmap()`, which have platform-specific limitations |
| Interaction during gesture | ❌ Dead snapshot — Can't respond to touches or updates during the gesture |
| Risk | 🔴 Very high — Platform-dependent, fragile, and produces a jarring "dead UI" feel |

**Verdict:** ❌ **Not recommended.** No standard API, platform fragility, and no real composition preservation.

---

### Option D: Two-Phase `movableContentOf` (Hybrid)

**Description:** A more sophisticated version of Option A that pre-creates `movableContentOf` instances in a registry. Both `AnimatedContent` and `PredictiveBackContent` reference the same movable instance.

**How It Works:**
```kotlin
// Registry of movable content, keyed by NavNode.key
class MovableContentRegistry {
    private val registry = mutableMapOf<NodeKey, @Composable () -> Unit>()
    
    fun getOrCreate(key: NodeKey, content: @Composable () -> Unit): @Composable () -> Unit {
        return registry.getOrPut(key) { movableContentOf { content() } }
    }
}

// In AnimatedContent's content lambda:
val movable = registry.getOrCreate(node.key) { NavNodeRenderer(node, ...) }
movable() // Placed inside AnimatedContent

// In PredictiveBackContent:
val movableCurrent = registry.getOrCreate(currentNode.key) { ... }
val movablePrevious = registry.getOrCreate(previousNode.key) { ... }
movableCurrent() // Moved here from AnimatedContent — state preserved!
movablePrevious() // Might be new or moved from a kept-alive slot
```

**Assessment:**

| Aspect | Evaluation |
|--------|------------|
| Current screen retention | ✅ Full — `movableContentOf` instance moves from `AnimatedContent` to `PredictiveBackContent` |
| Previous screen retention | ⚠️ Only if previously cached — If the registry kept the movable content alive from a prior navigation, state is preserved. If evicted, composed from scratch |
| AnimatedContent compatibility | 🔴 Critical issue — Same atomic-frame problem as Option A. `AnimatedContent` internally destroys its content slot before `PredictiveBackContent` can claim the movable instance. Within a single frame: (1) `AnimatedContent` leaves composition → calls dispose on its content, (2) `PredictiveBackContent` enters composition → calls movable. The `movableContentOf` documentation states the content is **moved** when it appears at a new call site and disappears from the old one "within the same composition." But the if/else branch switch means the old call site disappears (AnimatedContent) and the new appears (PredictiveBackContent) in the same recomposition — which should trigger the move. **However**, `AnimatedContent` adds internal container layers between the content and the call site, which may interfere. |
| Implementation complexity | 🔴 High — Registry lifecycle, eviction synchronization with `ComposableCache`, `AnimatedContent` internal layering |
| Memory overhead | 🟡 Moderate — Registry keeps movable instances for cached screens |
| Risk | 🔴 High — `AnimatedContent`'s internal slot management may not correctly release the movable content for move. Untested interaction. |

**Verdict:** ⚠️ **Theoretically elegant but practically risky.** The interaction between `movableContentOf` and `AnimatedContent`'s internal content management is not well-documented and could fail in subtle ways.

---

### Option E: Eliminate the Branch Switch (Recommended)

**Description:** Instead of `if/else` between `AnimatedContent` and `PredictiveBackContent`, keep the current screen ALWAYS inside `AnimatedContent` (stable position) and apply predictive back transforms via `graphicsLayer` WITHIN the content lambda. Render the previous screen as a sibling underlay (behind `AnimatedContent`), only when predictive back is active.

**This is the modal pattern inverted**: modals are sibling overlays ON TOP; the predictive back target is a sibling underlay BEHIND.

**Key Insight:** The current screen's `graphicsLayer` transforms can be applied INSIDE `AnimatedContent`'s content slot without changing the composition tree structure. `graphicsLayer` is a modifier-only operation — zero composition cost, GPU-only transforms.

**How It Works:**

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
    var lastCommittedState by remember { mutableStateOf(targetState) }
    var stateBeforeLast by remember { mutableStateOf<T?>(null) }

    val isPredictiveBackActive = predictiveBackEnabled &&
        scope.predictiveBackController.isActive.value
    val progress = scope.predictiveBackController.progress.value
    
    // Resolve back target
    val cascadeState = scope.predictiveBackController.cascadeState.value
    @Suppress("UNCHECKED_CAST")
    val backTarget = if (isPredictiveBackActive) {
        (cascadeState?.targetNode as? T) ?: stateBeforeLast
    } else null

    // ── LAYER 1: Previous screen underlay (only during gesture) ──
    if (isPredictiveBackActive && backTarget != null && backTarget.key != lastCommittedState.key) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Parallax: slides in from left
                    translationX = -size.width * PARALLAX_FACTOR * (1f - progress)
                }
        ) {
            StaticAnimatedVisibilityScope {
                content(backTarget)
            }
        }
    }

    // ── LAYER 2: Current screen (ALWAYS in AnimatedContent — stable position) ──
    if (isTargetModal) {
        Box(
            modifier = if (isPredictiveBackActive) Modifier.fillMaxSize().graphicsLayer {
                translationX = size.width * progress
                val scale = 1f - (progress * SCALE_FACTOR)
                scaleX = scale; scaleY = scale
            } else Modifier
        ) {
            StaticAnimatedVisibilityScope { content(targetState) }
        }
    } else {
        AnimatedContent(
            targetState = targetState,
            contentKey = { it.key },
            transitionSpec = {
                if (isPredictiveBackActive) {
                    // No animation during gesture — transforms handled by graphicsLayer
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    transition.createTransitionSpec(isBack = isBackNavigation)
                }
            },
            modifier = modifier,
            label = "AnimatedNavContent"
        ) { animatingState ->
            // Apply predictive back transforms INSIDE AnimatedContent's slot
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

    // State tracking (unchanged)
    if (targetState != lastCommittedState) {
        if (targetState.key != lastCommittedState.key) {
            stateBeforeLast = lastCommittedState
        }
        lastCommittedState = targetState
    }
}
```

**Assessment:**

| Aspect | Evaluation |
|--------|------------|
| Current screen retention | ✅ **Full** — Stays inside `AnimatedContent` at the same tree position. Only `graphicsLayer` modifier changes. Zero composition work |
| Previous screen retention | ⚠️ Composed from scratch when gesture starts — But this is **unavoidable** for any approach. The previous screen simply isn't in the composition tree before the gesture |
| AnimatedContent compatibility | ✅ **Full** — `AnimatedContent` is never bypassed. The content is always rendered through its slot |
| Implementation complexity | 🟢 **Low** — Remove the if/else branch; add a sibling Box before AnimatedContent; add `graphicsLayer` modifier inside the content lambda |
| Memory overhead | ✅ Minimal — Previous screen only composed while gesture is active |
| Transition behavior | ✅ `AnimatedContent` transition can be set to `None` during gesture, so it doesn't interfere with `graphicsLayer` transforms |
| Risk | 🟢 Low — Follows the proven modal pattern. No novel APIs needed |

**Verdict:** ✅ **Recommended.** Solves the current screen recomposition problem completely. Simple, proven pattern. The previous screen still composes from scratch, but this is inherent to the architecture.

---

## Recommended Approach: Option E — Eliminate the Branch Switch

### Design Principles

1. **Current screen NEVER moves in the composition tree** → zero recomposition during gesture
2. **Previous screen as sibling underlay** → follows the PR #69 modal pattern (inverted)
3. **`graphicsLayer` for all transforms** → GPU-only, zero composition cost
4. **`AnimatedContent` stays on screen** with `None` transitions during gesture → no internal state disruption
5. **`PredictiveBackContent.kt` is eliminated** → no more separate branch

### Detailed Pseudocode

#### AnimatedNavContent.kt (Revised)

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
    // ── State tracking (unchanged) ──
    var lastCommittedState by remember { mutableStateOf(targetState) }
    var stateBeforeLast by remember { mutableStateOf<T?>(null) }

    val isPredictiveBackActive = predictiveBackEnabled &&
        scope.predictiveBackController.isActive.value
    val progress = scope.predictiveBackController.progress.value

    // Resolve back target for underlay
    val backTarget: T? = if (isPredictiveBackActive) {
        val cascadeState = scope.predictiveBackController.cascadeState.value
        @Suppress("UNCHECKED_CAST")
        (cascadeState?.targetNode as? T) ?: stateBeforeLast
    } else null

    // ── UNDERLAY: Previous screen (sibling, rendered BEHIND main content) ──
    // Only composed when gesture is active — zero cost when idle
    if (isPredictiveBackActive && backTarget != null && backTarget.key != lastCommittedState.key) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -size.width * PARALLAX_FACTOR * (1f - progress)
                }
        ) {
            StaticAnimatedVisibilityScope {
                content(backTarget)
            }
        }
    }

    // ── MAIN CONTENT: Always at the same composition tree position ──
    if (isTargetModal) {
        // Modal bypass — same as before, plus predictive back transform
        Box(
            modifier = if (isPredictiveBackActive) {
                Modifier.fillMaxSize().graphicsLayer {
                    translationX = size.width * progress
                    val scale = 1f - (progress * SCALE_FACTOR)
                    scaleX = scale; scaleY = scale
                }
            } else Modifier
        ) {
            StaticAnimatedVisibilityScope {
                content(targetState)
            }
        }
    } else {
        // Standard AnimatedContent — NEVER bypassed
        AnimatedContent(
            targetState = targetState,
            contentKey = { it.key },
            transitionSpec = {
                if (isPredictiveBackActive) {
                    // Suppress AnimatedContent animations during gesture
                    // The visual movement is handled by graphicsLayer
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    transition.createTransitionSpec(isBack = isBackNavigation)
                }
            },
            modifier = modifier,
            label = "AnimatedNavContent"
        ) { animatingState ->
            // Wrap in Box for predictive back graphicsLayer transforms
            // When not active, the Box has no modifier overhead
            Box(
                modifier = if (isPredictiveBackActive) {
                    Modifier.fillMaxSize().graphicsLayer {
                        translationX = size.width * progress
                        val scale = 1f - (progress * SCALE_FACTOR)
                        scaleX = scale; scaleY = scale
                    }
                } else Modifier
            ) {
                scope.WithAnimatedVisibilityScope(this@AnimatedContent) {
                    content(animatingState)
                }
            }
        }
    }

    // ── State tracking (unchanged from current implementation) ──
    if (targetState != lastCommittedState) {
        if (targetState.key != lastCommittedState.key) {
            stateBeforeLast = lastCommittedState
        }
        lastCommittedState = targetState
    }
}
```

#### PredictiveBackContent.kt — To Be Deleted

This file is no longer needed. All its functionality is inlined into `AnimatedNavContent`:
- The underlay (previous screen) as a sibling Box before AnimatedContent
- The current screen transforms via `graphicsLayer` inside AnimatedContent

### Visual Composition Tree: Before vs After

```
BEFORE (current architecture):                   AFTER (proposed):

AnimatedNavContent                               AnimatedNavContent
│                                                │
├─ if(gestureActive) {                           ├─ if(gestureActive && backTarget != null) {
│    PredictiveBackContent ──┐                   │    Box(parallax graphicsLayer) {
│    ├─ Box(parallax)        │                   │      StaticScope { content(previous) }
│    │  └─ content(prev) NEW │ ← recomposed!     │    }
│    └─ Box(slide)           │                   │  }                ← sibling underlay
│       └─ content(curr) NEW │ ← recomposed!     │
│  }                         │                   ├─ AnimatedContent(target) {  ← ALWAYS HERE
│                                                │    Box(gestureActive ? slide graphicsLayer : none) {
├─ else {                                        │      WithAnimatedVisibilityScope {
│    AnimatedContent(target)                     │        content(animatingState)  ← NEVER MOVES
│      └─ content(curr) ────────── only here     │      }
│  }                                             │    }
│                                                │  }
└── stateTracking                                │
                                                 └── stateTracking
```

**Key difference:** The current screen stays at a **single stable composition tree position** (inside `AnimatedContent`) regardless of whether predictive back is active. No branch switch = no composition destruction.

---

## Edge Cases

### 1. Nested Stacks (tabs containing stacks)

**Scenario:** Root stack → TabNode → inner StackNode with 2+ screens. Back gesture pops within inner stack.

**Behavior:** `shouldEnablePredictiveBack` routes the animation to the correct stack via `animatingStackKey`. The inner stack's `AnimatedNavContent` handles the gesture. The inner stack's current screen stays in its `AnimatedContent` slot — no issue.

**No change needed** — the fix is entirely within `AnimatedNavContent`, which is used by all stacks.

### 2. Cascade Back (entire TabNode removed)

**Scenario:** Root stack → TabNode (single child in each tab stack). Back gesture cascades: removes TabNode, reveals previous screen in root stack.

**Behavior:** `cascadeDepth > 0`, so `shouldEnablePredictiveBack` returns true only for the root stack. The root stack's `AnimatedNavContent` handles it:
- **Current content** = TabNode (stays in root's `AnimatedContent`) → transforms via `graphicsLayer`
- **Previous content** = the screen below the TabNode in the root stack → composed as underlay

This works because `AnimatedContent`'s content lambda receives the `TabNode` as `animatingState`, and `NavNodeRenderer` recursively renders it. The `graphicsLayer` on the outer `Box` transforms the entire subtree.

**No change needed** — cascade is handled at the root stack level.

### 3. Gesture Cancellation

**Scenario:** User starts gesture, then moves finger back (cancels).

**Behavior:** 
1. `isActive` stays true during cancellation animation
2. `progress` animates from current value → 0
3. `graphicsLayer` transforms smoothly return to identity (no translation, scale=1)
4. When animation completes: `isActive = false`, underlay Box leaves composition
5. Current screen stays in `AnimatedContent` throughout — **zero composition work**

**This is significantly better than current behavior**, where cancellation means switching back from `PredictiveBackContent` to `AnimatedContent` (another destructive branch switch).

### 4. Gesture Completion

**Scenario:** User completes the swipe. Navigation commits.

**Behavior:**
1. `predictiveBackController.animateToCompletion` calls `onNavigate()` first (updates backstack)
2. `progress` animates from current → 1.0
3. During animation: current screen slides fully off-screen via `graphicsLayer`
4. When `isActive` becomes false: `graphicsLayer` transforms removed, `AnimatedContent` sees the new `targetState` → runs standard transition
5. But since the previous screen is already composed as the underlay, and now becomes the `targetState`, `AnimatedContent` can use `None` transition or a quick snap

**Subtle issue:** At completion, the `targetState` passed to `AnimatedContent` changes to the new top of stack. `AnimatedContent` runs its standard transition. Since the previous screen was an underlay sibling, not inside `AnimatedContent`, there could be a brief duplicate render (the new screen appearing both as underlay and as `AnimatedContent`'s new target).

**Resolution:** When completion animation ends and `isActive` goes false, the underlay disappears (conditional on `isPredictiveBackActive`). The `AnimatedContent` should already be rendering the new target. To prevent a flash, use `EnterTransition.None` for the `AnimatedContent` transition when the state change happens during a completion animation:

```kotlin
transitionSpec = {
    if (isPredictiveBackActive) {
        // During gesture: no AnimatedContent animation
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        transition.createTransitionSpec(isBack = isBackNavigation)
    }
}
```

The frame sequence at completion:
1. Frame N: gesture active, progress=0.95, underlay shows previous, graphicsLayer transforms current
2. Frame N+1: `onNavigate()` → targetState changes, progress=0.98
3. Frame N+2: progress=1.0, `isActive` → false
4. Frame N+3: underlay gone, `AnimatedContent` renders new target (no transition because the completion was instant)

**Additional safeguard:** Track a `wasJustCompleted` flag that persists for one frame after completion, ensuring the first `AnimatedContent` transition after predictive back is `None`:

```kotlin
var wasGestureCompleted by remember { mutableStateOf(false) }

// Detect gesture completion
LaunchedEffect(isPredictiveBackActive) {
    if (!isPredictiveBackActive && wasGestureCompleted) {
        // One frame after completion — reset
        wasGestureCompleted = false
    }
}

// Set on completion
if (!isPredictiveBackActive && backTarget != null) {
    wasGestureCompleted = true
}
```

### 5. Rapid Gesture After Completion

**Scenario:** User completes a back gesture and immediately starts another.

**Behavior:** The state tracking (`lastCommittedState`, `stateBeforeLast`) updates during the transition. If a new gesture starts before the transition completes:
- `lastCommittedState` has the new top-of-stack
- `stateBeforeLast` has the new previous screen
- The new gesture renders correctly

**No special handling needed** — state tracking is independent of the gesture state machine.

### 6. Modal on Top During Gesture

**Scenario:** A modal is visible when the user starts a back gesture.

**Behavior:** `StackRenderer` already separates modals from the background target. Predictive back is only enabled on the `backgroundTarget`, not the modal overlay. If back would pop the modal:
- `cascadeState` targets the modal's removal
- The modal overlay is a sibling in `StackRenderer`, not inside `AnimatedContent`
- The gesture operates on the `backgroundTarget` which is already stable

**No change needed** — modal handling is orthogonal to this fix.

---

## Integration with ComposableCache and SaveableStateHolder

### Current Integration

```
StackRenderer
  └── AnimatedNavContent
        └── AnimatedContent { animatingState ->
              └── NavNodeRenderer(animatingState)
                    └── ScreenRenderer(node)
                          └── CachedEntry(key, saveableStateHolder) {
                                └── SaveableStateProvider(key) {
                                      └── ScreenContent(...)
                                }
                          }
              }
```

### After Fix

```
StackRenderer
  └── AnimatedNavContent
        ├── [Underlay] Box(graphicsLayer) {                     ← NEW: sibling
        │     └── StaticAnimatedVisibilityScope {
        │           └── content(backTarget)                     ← Goes through NavNodeRenderer → ScreenRenderer
        │                 └── CachedEntry(key, saveableStateHolder)
        │                       └── SaveableStateProvider(key)
        │                             └── ScreenContent(...)
        │     }
        │   }
        │
        └── AnimatedContent { animatingState →
              └── Box(graphicsLayer) {                          ← NEW: transform wrapper
                    └── WithAnimatedVisibilityScope {
                          └── content(animatingState)           ← UNCHANGED position
                                └── ScreenRenderer → CachedEntry → SaveableStateProvider  
                    }
              }
        }
```

**Key points:**
- `CachedEntry` and `SaveableStateProvider` work identically — they're keyed by `node.key`, not by position
- The underlay's `content(backTarget)` goes through the same `NavNodeRenderer → ScreenRenderer → CachedEntry` path
- `SaveableStateProvider` with the same key in two simultaneous composition positions: **this is safe** because `SaveableStateHolder` uses the key as a lookup, and both positions contribute to the same saved state bucket
- `ComposableCache` LRU tracking: the underlay's composition will call `CachedEntry(backTarget.key)`, which updates the access time. This is correct — the back target should be protected from eviction during the gesture

### Cache Lock Recommendation

During predictive back, both the current and previous screen keys should be locked in the cache to prevent eviction:

```kotlin
// In AnimatedNavContent, when gesture starts:
DisposableEffect(isPredictiveBackActive, backTarget) {
    if (isPredictiveBackActive && backTarget != null) {
        scope.cache.lock(backTarget.key.value)
        scope.cache.lock(lastCommittedState.key.value)
    }
    onDispose {
        if (backTarget != null) {
            scope.cache.unlock(backTarget.key.value)
            scope.cache.unlock(lastCommittedState.key.value)
        }
    }
}
```

---

## Impact on SinglePaneRenderer

`SinglePaneRenderer` has the **same bifurcated if/else pattern**:

```kotlin
if (isPredictiveBackActive && primaryPaneContent != null) {
    PredictiveBackContent(current = lastCommittedContent, previous = primaryPaneContent, ...)
} else {
    AnimatedNavContent(targetState = activePaneContent, ...)
}
```

### Recommended Fix: Unified Pattern

Apply the same "eliminate the branch switch" pattern. `SinglePaneRenderer` should:

1. Always render through `AnimatedNavContent`
2. Pass `predictiveBackEnabled = true` to `AnimatedNavContent` when pane switching supports gestures
3. Let `AnimatedNavContent` handle the underlay + transform internally

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

    // Let AnimatedNavContent handle everything — including predictive back
    val predictiveBackEnabled = scope.predictiveBackController.let { ctrl ->
        val cascadeState = ctrl.cascadeState.value
        ctrl.isActive.value &&
            cascadeState != null &&
            cascadeState.animatingStackKey == node.key
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

This **removes 40+ lines of duplicated predictive back logic** from `SinglePaneRenderer` and centralizes it in `AnimatedNavContent`.

---

## AnimatedContent Transition Suppression During Gesture

A subtle concern: when `isPredictiveBackActive` is true and progress is being driven by the gesture, the `graphicsLayer` modifier on the inner Box handles all visual transforms. But `AnimatedContent` is still running — if `targetState` changes (e.g., from `onNavigate()` during completion), `AnimatedContent` would normally animate the transition.

**Solution:** The `transitionSpec` checks `isPredictiveBackActive`:

```kotlin
transitionSpec = {
    if (isPredictiveBackActive) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        transition.createTransitionSpec(isBack = isBackNavigation)
    }
}
```

However, `transitionSpec` is evaluated when `AnimatedContent` starts a new transition, not continuously. If `isActive` becomes false between when the `targetState` changes and when the transition starts, the standard transition might run.

**Mitigation:** Track `recentlyCompletedGesture` state that stays true for 1 composition cycle:

```kotlin
var recentlyCompletedGesture by remember { mutableStateOf(false) }

// Detect completion
val wasActive = remember { mutableStateOf(false) }
if (isPredictiveBackActive && !wasActive.value) {
    wasActive.value = true
}
if (!isPredictiveBackActive && wasActive.value) {
    recentlyCompletedGesture = true
    wasActive.value = false
}

// Reset after one frame
SideEffect {
    if (recentlyCompletedGesture) recentlyCompletedGesture = false
}

// Use in transitionSpec
transitionSpec = {
    if (isPredictiveBackActive || recentlyCompletedGesture) {
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        transition.createTransitionSpec(isBack = isBackNavigation)
    }
}
```

---

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `AnimatedContent` running exit animation on stale content during completion | Medium | Visual glitch (old screen flashes) | Suppress transitions with `None` spec during gesture + 1 frame after completion |
| Dual `SaveableStateProvider` with same key (underlay + AnimatedContent) | Low | State corruption | `SaveableStateHolder` uses key-based lookup; two providers with the same key share state — this is the intended behavior |
| Previous screen `LaunchedEffect`s triggering during gesture | Medium | Unwanted API calls or state changes | This exists in the current implementation too. Future improvement: add lifecycle-awareness to underlay content |
| `graphicsLayer` transform not applying to all content (e.g., popups) | Low | Visual artifacts | All screen content is within the Box that has `graphicsLayer` — nested dialogs/popups may be outside |
| `AnimatedContent` recomposing during gesture due to progress state reads | Low | Performance concern | `progress` is read inside `graphicsLayer` lambda (not in composition scope). `isPredictiveBackActive` causes one recomposition when gesture starts, which is the same as current behavior |

---

## Summary

### Recommendation: Option E — Eliminate the Branch Switch

**Changes required:**

| File | Change |
|------|--------|
| [AnimatedNavContent.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt) | Remove `if(isPredictiveBackActive)` branch. Add underlay Box before `AnimatedContent`/modal. Add `graphicsLayer` transform inside `AnimatedContent`'s content lambda. Add transition suppression. |
| [PredictiveBackContent.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PredictiveBackContent.kt) | **Delete** — no longer needed |
| [PaneRenderer.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PaneRenderer.kt) (`SinglePaneRenderer`) | Remove direct `PredictiveBackContent` usage. Pass `predictiveBackEnabled = true` to `AnimatedNavContent`. Remove duplicated state tracking. |
| [PredictiveBackContentTest.kt](quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/PredictiveBackContentTest.kt) | Update or replace with tests for the new underlay behavior in `AnimatedNavContent` |

**What this solves:**
- ✅ Current screen composition fully preserved during gesture (zero recomposition)
- ✅ Gesture cancellation is free (just animate `graphicsLayer` back to identity)
- ✅ Gesture completion is smooth (no branch switch at any point)
- ✅ Simplifies codebase (removes `PredictiveBackContent.kt`, removes duplication in `SinglePaneRenderer`)

**What this does NOT solve:**
- ⚠️ Previous screen still composed from scratch when gesture starts (inherent — it's not in the tree)
- ⚠️ Previous screen's `LaunchedEffect`s will run (same as current, no regression)

**Future enhancement:** For the previous screen, a separate follow-up could explore keeping the previous screen in a "dormant" always-composed state (Option B variant) with suppressed effects, but this is a larger architectural change that should be evaluated separately.
