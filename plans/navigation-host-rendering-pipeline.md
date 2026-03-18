# NavigationHost Rendering Pipeline Analysis

## Purpose

Analysis of how `NavigationHost` renders different node types, how the content composition 
pipeline works, and what would need to change to support rendering two screens simultaneously 
(modal over background) for modal navigation support.

---

## 1. NavigationHost — Entry Point

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/NavigationHost.kt`

### Two Overloads

1. **Full overload** (lines 106–401) — takes explicit registries:
   ```kotlin
   @Composable
   fun NavigationHost(
       navigator: Navigator,
       modifier: Modifier = Modifier,
       screenRegistry: ScreenRegistry = EmptyScreenRegistry,
       containerRegistry: ContainerRegistry = ContainerRegistry.Empty,
       transitionRegistry: TransitionRegistry = TransitionRegistry.Empty,
       scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
       enablePredictiveBack: Boolean = true,
       windowSizeClass: WindowSizeClass? = null
   )
   ```

2. **Simplified overload** (lines 407–471) — reads config from `navigator.config`:
   ```kotlin
   @Composable
   fun NavigationHost(
       navigator: Navigator,
       modifier: Modifier = Modifier,
       enablePredictiveBack: Boolean = true,
       windowSizeClass: WindowSizeClass? = null
   )
   ```
   Delegates to the full overload extracting registries from `navigator.config`.

### Internal Setup (full overload)

The full overload performs these setup steps in order:

| Step | What | Purpose |
|------|------|---------|
| 1 | `navigator.state.collectAsState()` | Collect nav tree as Compose state |
| 2 | `previousState` tracking via `remember(navState)` + `SideEffect` | Track previous tree for animation direction |
| 3 | `rememberSaveableStateHolder()` | Saveable state persistence across navigation |
| 4 | `rememberComposableCache()` | LRU cache for composable lifecycle management |
| 5 | `AnimationCoordinator(transitionRegistry)` | Resolve transitions per destination |
| 6 | `PredictiveBackController()` | Gesture-driven back animation coordination |
| 7 | `rememberBackAnimationController()` | Back animation progress tracking |
| 8 | `BackHandlerRegistry()` | User-defined back handler support |
| 9 | Speculative pop state calculation | For predictive back gesture preview |

### Rendering Hierarchy

```
NavigateBackHandler(enabled, onBackProgress, onBackCancelled, onBackCompleted) {
    SharedTransitionLayout(modifier) {
        // Create NavRenderScopeImpl with all dependencies
        CompositionLocalProvider(
            LocalNavRenderScope, LocalNavigator, LocalBackHandlerRegistry, LocalBackAnimationController
        ) {
            NavNodeRenderer(node = navState, previousNode = previousState, scope = scope)
        }
    }
}
```

Key points:
- **NavigateBackHandler** wraps everything for predictive back gesture input
- **SharedTransitionLayout** enables shared element transitions
- **NavRenderScopeImpl** aggregates all dependencies into a single scope object
- **NavNodeRenderer** is the single recursive entry point for tree rendering

---

## 2. NavNodeRenderer — Central Dispatcher

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/NavTreeRenderer.kt`

```kotlin
@Composable
internal fun NavNodeRenderer(
    node: NavNode,
    previousNode: NavNode?,
    scope: NavRenderScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
)
```

Uses a `when` expression to dispatch to type-specific renderers:

| Node Type | Renderer | File |
|-----------|----------|------|
| `ScreenNode` | `ScreenRenderer` | `ScreenRenderer.kt` |
| `StackNode` | `StackRenderer` | `StackRenderer.kt` |
| `TabNode` | `TabRenderer` | `TabRenderer.kt` |
| `PaneNode` | `PaneRenderer` | `PaneRenderer.kt` |

The `previousNode` parameter is cast to the matching type for each renderer, enabling animation direction detection.

---

## 3. ScreenRenderer — Leaf Node Rendering

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/ScreenRenderer.kt`

```kotlin
@Composable
internal fun ScreenRenderer(
    node: ScreenNode,
    scope: NavRenderScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
)
```

### Rendering Pipeline

```
ScreenRenderer
  └── ComposableCache.CachedEntry(key = node.key.value)
        └── DisposableEffect: attachToUI() / detachFromUI()
        └── CompositionLocalProvider(LocalScreenNode, LocalAnimatedVisibilityScope)
              └── screenRegistry.Content(destination, sharedTransitionScope, animatedVisibilityScope)
```

Key aspects:
- **Lifecycle management**: `attachToUI()` on enter, `detachFromUI()` on dispose
- **Cache**: Uses `ComposableCache.CachedEntry` with `SaveableStateHolder` for state preservation
- **Content resolution**: Delegates to `ScreenRegistry.Content()` which is KSP-generated
- **Composition locals provided**: `LocalScreenNode`, `LocalAnimatedVisibilityScope`
- **This is the ONLY place** where actual screen content (user-defined `@Screen` composables) is invoked

---

## 4. StackRenderer — Push/Pop Stack

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/StackRenderer.kt`

```kotlin
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
)
```

### Rendering Pipeline

```
StackRenderer
  ├── activeChild = node.activeChild (last child in stack)
  ├── detectBackNavigation() — compares stack sizes
  ├── animationCoordinator.getTransition()
  └── AnimatedNavContent(targetState = activeChild, transition, isBackNavigation)
        └── NavNodeRenderer(node = child, previousNode = previousActiveChild)
```

### Critical: Only Renders the Active (Top) Child

- `node.activeChild` returns the **last** child in `node.children` — the top of the stack
- **Items below the top are NOT rendered** in normal mode
- During **predictive back gestures**, `AnimatedNavContent` switches to `PredictiveBackContent` which renders TWO items (current + previous) simultaneously with transform effects

### Back Navigation Detection

```kotlin
internal fun detectBackNavigation(current: StackNode, previous: StackNode?): Boolean {
    if (previous == null) return false
    return current.children.size < previous.children.size
}
```

Simple size comparison: smaller stack = pop = back navigation.

---

## 5. TabRenderer — Tab Container

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/TabRenderer.kt`

### Rendering Pipeline

```
TabRenderer
  └── ComposableCache.CachedEntry(key = node.key.value)  ← caches ENTIRE tab structure
        └── DisposableEffect: attachToUI() / detachFromUI()
        └── CompositionLocalProvider(LocalContainerNode provides node)
              └── containerRegistry.TabsContainer(tabNodeKey, scope = tabsContainerScope) {
                    └── AnimatedNavContent(targetState = activeStack, transition = defaultTabTransition)
                          └── NavNodeRenderer(node = stack, previousNode = previousActiveStack)
              }
```

Key aspects:
- **Wrapper composition**: `containerRegistry.TabsContainer()` renders the custom tab bar UI (e.g., BottomNavigation)
- **Content slot**: The wrapper receives a `content` lambda containing `AnimatedNavContent` for animated tab switching
- **Tab switching**: Uses `AnimatedNavContent` with `defaultTabTransition` (typically fade)
- **No predictive back on tab switching**: `predictiveBackEnabled = false` — predictive back is handled within each tab's inner stack
- **Lifecycle management**: `TabNode.attachToUI()` / `detachFromUI()`
- **TabsContainerScope**: Provides `activeTabIndex`, `tabs` list, `onSwitchTab` callback to wrapper

---

## 6. PaneRenderer — Adaptive Layout

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PaneRenderer.kt`

### Two Modes

```
PaneRenderer
  ├── [Expanded] → MultiPaneRenderer
  │     └── containerRegistry.PaneContainer(paneNodeKey, scope = paneContainerScope) {
  │           └── paneContents.filter { it.isVisible }.forEach { pane.content() }
  │     }
  │
  └── [Compact] → SinglePaneRenderer
        └── AnimatedNavContent(targetState = activePaneContent)
              └── NavNodeRenderer(node = paneNavNode)
```

- **Expanded mode** (`isAtLeastMediumWidth`): Renders ALL visible panes side-by-side via `MultiPaneRenderer` → `PaneContainer` wrapper
- **Compact mode**: Renders only the active pane with animated transitions via `SinglePaneRenderer`, behaving like a stack
- **Predictive back in compact mode**: Supported for pane switching (e.g., showing PRIMARY behind SECONDARY during gesture)

---

## 7. AnimatedNavContent — Core Animation Component

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/AnimatedNavContent.kt`

```kotlin
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    isBackNavigation: Boolean,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
)
```

### Two Rendering Paths

1. **Predictive back active** (`isPredictiveBackActive`):
   - Delegates to `PredictiveBackContent` for gesture-driven animation
   - Renders BOTH current and previous content simultaneously

2. **Standard mode**:
   - Uses Compose's `AnimatedContent` with `transitionSpec` from `NavTransition`
   - `contentKey = { it.key }` prevents duplicate SaveableStateProvider keys
   - Provides `AnimatedVisibilityScope` to content via `scope.WithAnimatedVisibilityScope(this)`

---

## 8. PredictiveBackContent — Dual Screen Rendering

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/internal/render/PredictiveBackContent.kt`

```kotlin
@Composable
internal fun <T : NavNode> PredictiveBackContent(
    current: T,
    previous: T?,
    progress: Float,
    scope: NavRenderScope,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
)
```

**This is the closest existing analog to modal rendering.** It renders two screens simultaneously:

```
Box(fillMaxSize) {
    // Layer 1: Previous (incoming) content — behind, with parallax
    Box(graphicsLayer { translationX = parallax offset }) {
        StaticAnimatedVisibilityScope { content(previous) }
    }
    
    // Layer 2: Current (exiting) content — on top, sliding + scaling
    Box(graphicsLayer { translationX, scaleX, scaleY based on progress }) {
        StaticAnimatedVisibilityScope { content(current) }
    }
}
```

- **Parallax factor**: 0.3 (previous content moves at 30% of gesture distance)
- **Scale factor**: 0.1 (current content scales down to 90% at full gesture)
- Does NOT use `ComposableCache.CachedEntry` directly — content lambda recurses through `NavNodeRenderer` → `ScreenRenderer` which handles caching

---

## 9. Content Resolution Pipeline

The complete path from NavNode to actual screen content:

```
NavNode (tree state)
  → NavNodeRenderer (dispatch by type)
    → ScreenRenderer (for ScreenNode leaves)
      → ComposableCache.CachedEntry (state preservation)
        → ScreenRegistry.Content(destination, ...) (KSP-generated)
          → Matches destination class to @Screen-annotated composable
            → Invokes user's @Composable function with (destination, navigator)
```

### ScreenRegistry Interface

```kotlin
interface ScreenRegistry {
    @Composable
    fun Content(
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope? = null,
        animatedVisibilityScope: AnimatedVisibilityScope? = null
    )
    
    fun hasContent(destination: NavDestination): Boolean
}
```

KSP generates concrete implementations that use a `when` expression on `destination::class` to dispatch to the correct `@Screen`-annotated composable.

---

## 10. NavRenderScope — Dependency Container

**Interface:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/NavRenderScope.kt`
**Implementation:** `NavRenderScopeImpl` in `NavigationHost.kt` (private class, lines 477–537)

| Property | Type | Purpose |
|----------|------|---------|
| `navigator` | `Navigator` | Navigation operations |
| `cache` | `ComposableCache` | LRU composable lifecycle cache |
| `saveableStateHolder` | `SaveableStateHolder` | Saveable state persistence |
| `animationCoordinator` | `AnimationCoordinator` | Transition resolution |
| `predictiveBackController` | `PredictiveBackController` | Gesture state coordination |
| `screenRegistry` | `ScreenRegistry` | Destination → content lookup |
| `containerRegistry` | `ContainerRegistry` | Tab/Pane wrapper lookup |
| `sharedTransitionScope` | `SharedTransitionScope?` | Shared element transitions |

Key methods:
- `shouldEnablePredictiveBack(node)` — determines if a specific node should handle predictive back
- `WithAnimatedVisibilityScope(scope, content)` — provides `LocalAnimatedVisibilityScope` + `LocalTransitionScope`

---

## 11. CompositionLocals

**File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/compose/scope/CompositionLocals.kt`

| Local | Type | Set By |
|-------|------|--------|
| `LocalScreenNode` | `ScreenNode?` | `ScreenRenderer` |
| `LocalAnimatedVisibilityScope` | `AnimatedVisibilityScope?` | `ScreenRenderer`, `NavRenderScopeImpl.WithAnimatedVisibilityScope` |
| `LocalNavigator` | `Navigator?` | `NavigationHost` |
| `LocalContainerNode` | `LifecycleAwareNode?` | `TabRenderer`, `PaneRenderer` |
| `LocalNavRenderScope` | `NavRenderScope?` | `NavigationHost` |

---

## 12. Complete File Inventory

### Core Rendering Pipeline

| File | Key Symbols | Role |
|------|-------------|------|
| `compose/NavigationHost.kt` | `NavigationHost` (×2), `NavRenderScopeImpl`, `EmptyScreenRegistry` | Entry point, scope creation |
| `compose/internal/render/NavTreeRenderer.kt` | `NavNodeRenderer` | Type-based dispatch |
| `compose/internal/render/ScreenRenderer.kt` | `ScreenRenderer` | Leaf node content rendering |
| `compose/internal/render/StackRenderer.kt` | `StackRenderer`, `detectBackNavigation` | Stack navigation with animation |
| `compose/internal/render/TabRenderer.kt` | `TabRenderer`, `getTabDestinations`, `findFirstScreenDestination` | Tab container rendering |
| `compose/internal/render/PaneRenderer.kt` | `PaneRenderer`, `MultiPaneRenderer`, `SinglePaneRenderer`, `buildPaneContentListWithRenderers` | Adaptive pane rendering |
| `compose/internal/render/AnimatedNavContent.kt` | `AnimatedNavContent` | Core animation wrapper |
| `compose/internal/render/PredictiveBackContent.kt` | `PredictiveBackContent` | Dual-screen gesture rendering |
| `compose/internal/render/StaticAnimatedVisibilityScope.kt` | `StaticAnimatedVisibilityScope` | Static scope for non-animated content |

### Supporting Infrastructure

| File | Key Symbols | Role |
|------|-------------|------|
| `compose/scope/NavRenderScope.kt` | `NavRenderScope` | Dependency interface for renderers |
| `compose/scope/CompositionLocals.kt` | `LocalScreenNode`, `LocalNavigator`, etc. | Composition locals |
| `compose/scope/TabsContainerScope.kt` | `TabsContainerScope` | Tab wrapper scope |
| `compose/scope/PaneContainerScope.kt` | `PaneContainerScope` | Pane wrapper scope |
| `compose/internal/ComposableCache.kt` | `ComposableCache` | LRU state preservation |
| `compose/internal/AnimationCoordinator.kt` | `AnimationCoordinator` | Transition resolution |
| `compose/internal/PredictiveBackController.kt` | `PredictiveBackController` | Gesture state management |
| `compose/internal/BackAnimationController.kt` | `BackAnimationController` | Back animation progress |
| `compose/internal/navback/NavigateBackHandler.kt` | `NavigateBackHandler` | Platform back input handling |

### Registries

| File | Key Symbols | Role |
|------|-------------|------|
| `registry/ScreenRegistry.kt` | `ScreenRegistry` | Destination → composable mapping |
| `registry/ContainerRegistry.kt` | `ContainerRegistry` | Tab/Pane wrapper lookup |
| `registry/TransitionRegistry.kt` | `TransitionRegistry` | Per-destination transitions |

---

## 13. Analysis for Modal Navigation Support

### Q: What would need to change to render TWO screens at once (modal over background)?

The existing architecture has **one precedent** for dual-screen rendering: `PredictiveBackContent`. This renders both current and previous screens simultaneously with transforms. However, it is temporary (gesture-only) and not a persistent rendering state.

### Key Challenges

1. **StackRenderer renders only the top child:**
   ```kotlin
   val activeChild = node.activeChild ?: return
   AnimatedNavContent(targetState = activeChild, ...) { child ->
       NavNodeRenderer(node = child, ...)
   }
   ```
   Only `node.activeChild` (last child) is rendered. For modals, we need to render the top-of-stack AND the screen beneath it simultaneously.

2. **AnimatedNavContent replaces content:**
   Standard `AnimatedContent` transitions OUT the old content and transitions IN the new. For modals, the background screen must remain **fully composed and visible** behind the modal.

3. **ComposableCache eviction:**
   The LRU cache (`maxCacheSize = 5`) might evict the background screen. The background must be kept alive while the modal is up.

### Possible Approaches

#### Approach A: Modal-Aware StackRenderer

Modify `StackRenderer` to detect when the top child is a modal and render both background + modal:

```
StackRenderer
  ├── if activeChild is modal:
  │     Box(fillMaxSize) {
  │         NavNodeRenderer(node = backgroundChild)   ← background screen
  │         ModalOverlay {
  │             NavNodeRenderer(node = activeChild)    ← modal screen
  │         }
  │     }
  └── else: (existing) AnimatedNavContent(activeChild)
```

Requires:
- A way to identify modal destinations (new `ModalNode` type or flag on `ScreenNode`)
- `StackRenderer` changes to render two children when modal is active
- Cache locking for background screen during modal display
- Modal-specific transition animations (slide up from bottom, fade scrim, etc.)

#### Approach B: New ModalNode Type

Add a new `NavNode` subtype:

```kotlin
data class ModalNode(
    override val key: NodeKey,
    override val parentKey: NodeKey?,
    val backgroundNode: NavNode,    // what's behind
    val modalContent: ScreenNode,   // the modal
) : NavNode
```

Add `ModalRenderer` to `NavNodeRenderer`'s dispatch, plus a new `is ModalNode -> ModalRenderer(...)` case.

Requires:
- New `NavNode` subtype
- New `ModalRenderer` composable
- `TreeMutator` changes for modal push/pop
- KSP changes for `@Modal` annotation

#### Approach C: Overlay Layer in NavigationHost

Add a separate overlay composition layer in `NavigationHost` alongside the main tree:

```kotlin
NavigationHost {
    SharedTransitionLayout {
        // Main tree rendering
        NavNodeRenderer(navState, previousState, scope)
        
        // Modal overlay layer
        ModalOverlay(navigator.modalState) { modalNode ->
            ScreenRenderer(modalNode, scope)
        }
    }
}
```

Requires:
- `Navigator` to track modal state separately from the main tree
- A `ModalOverlay` composable
- Simpler tree changes (modal isn't in the tree, it's a parallel state)

### Recommendation

**Approach A** (Modal-Aware StackRenderer) is the most natural fit because:
- Modals are conceptually stack entries that happen to show the previous screen behind them
- It preserves the existing tree-based architecture
- `PredictiveBackContent` already proves the dual-rendering pattern works
- Minimal API surface change (add a flag or node variant, not a whole new system)
- Back navigation naturally pops the modal since it's a stack child

The key insight is that **StackRenderer is the right place** to add modal support — it already manages push/pop semantics, just currently renders only the top item.
