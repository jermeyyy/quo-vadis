# Modal Navigation Documentation — Implementation Plan

> **Purpose:** Update all Quo Vadis documentation to cover the modal navigation feature  
> **Prerequisite:** Modal navigation feature implemented per `plans/modal-navigation.md`  
> **Status:** Draft

---

## Table of Contents

- [Phase 1: Core Markdown Docs](#phase-1-core-markdown-docs-docsmd)
  - [Task 1.1: docs/ANNOTATIONS.md — @Modal Annotation Section](#task-11-docsannotationsmd--modal-annotation-section)
  - [Task 1.2: docs/NAV-NODES.md — Modal Rendering Behavior](#task-12-docsnav-nodesmd--modal-rendering-behavior)
  - [Task 1.3: docs/ARCHITECTURE.md — Architecture Updates](#task-13-docsarchitecturemd--architecture-updates)
  - [Task 1.4: docs/DSL-CONFIG.md — ModalRegistry Section](#task-14-docsdsl-configmd--modalregistry-section)
  - [Task 1.5: docs/TRANSITIONS.md — Modal Transition Behavior](#task-15-docstransitionsmd--modal-transition-behavior)
- [Phase 2: Documentation Site Content](#phase-2-documentation-site-content)
  - [Task 2.1: New ModalNavigation Page](#task-21-new-modalnavigation-page)
  - [Task 2.2: Code Examples Data](#task-22-code-examples-data)
  - [Task 2.3: Navigation Data Entry](#task-23-navigation-data-entry)
  - [Task 2.4: App.tsx Route Registration](#task-24-apptsx-route-registration)
  - [Task 2.5: AnnotationAPI Page Updates](#task-25-annotationapi-page-updates)
  - [Task 2.6: CoreConcepts Page Updates](#task-26-coreconcepts-page-updates)
  - [Task 2.7: DSLConfig Page Updates](#task-27-dslconfig-page-updates)
- [Phase 3: Supporting Files](#phase-3-supporting-files)
  - [Task 3.1: README.md Updates](#task-31-readmemd-updates)
  - [Task 3.2: copilot-instructions.md Updates](#task-32-copilot-instructionsmd-updates)
- [Sequencing & Dependencies](#sequencing--dependencies)

---

## Phase 1: Core Markdown Docs (`docs/*.md`)

### Task 1.1: docs/ANNOTATIONS.md — @Modal Annotation Section

**Description:** Add a full `@Modal` annotation section and update the "What Gets Generated" and "Generated Code" sections.

**Dependencies:** None

#### Location: New `## @Modal Annotation` section

Insert **after** the `## @Transition Annotation` section (after line ~870, before `## Generated Code` at line ~876).

#### Content Outline

```markdown
## @Modal Annotation

### Purpose

Marks a destination or container for draw-behind rendering. When a modal-flagged node is the 
active child of a `StackNode`, the library renders both the screen below AND the modal on top 
in a `Box` layout. The user composable controls all visual treatment (scrim, bottom sheet, 
dialog, glass effect, etc.) — the library provides no chrome or dismiss behavior.

### Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| *(none)* | — | — | Marker annotation — no parameters |

### Target

`@Modal` must appear alongside one of:
- `@Destination` — makes a single destination render modally
- `@Tabs` — makes a tab container render modally when pushed on a stack
- `@Stack` — makes a nested stack render modally
- `@Pane` — makes a pane container render modally

### Examples
```

Include these code examples:

**Example 1: Modal Destination**
```kotlin
@Modal
@Destination(route = "navigation_menu")
data object NavigationMenuDestination : NavDestination

@Screen(NavigationMenuDestination::class)
@Composable
fun NavigationMenuScreen(navigator: Navigator = koinInject()) {
    GlassBottomSheet(
        onDismissRequest = { navigator.navigateBack() },
    ) {
        // Menu content — user controls all presentation
    }
}
```

**Example 2: Modal Container (Tabs pushed on stack)**
```kotlin
@Modal
@Tabs(name = "overlayTabs")
sealed class OverlayTabs : NavDestination {
    companion object : NavDestination

    @TabItem(OverlayTabs::class, ordinal = 0)
    @Destination(route = "overlay/info")
    data object InfoTab : OverlayTabs()

    @TabItem(OverlayTabs::class, ordinal = 1)
    @Destination(route = "overlay/actions")
    data object ActionsTab : OverlayTabs()
}
```

**Example 3: Combining @Modal with @Transition**
```kotlin
@Modal
@Transition(type = TransitionType.SlideVertical)
@Destination(route = "settings_sheet")
data object SettingsSheet : HomeDestination()
```

#### Subsections

```markdown
### How It Works

- `@Modal` causes the KSP code generator to emit `modal<DestinationType>()` or `modalContainer("name")` 
  in the generated `NavigationConfig`
- At runtime, `StackRenderer` checks `ModalRegistry.isModalDestination()` / `isModalContainer()` 
  for the active child node
- If modal: renders background node + modal node in a `Box` via `ModalContent` composable
- If not modal: standard `AnimatedNavContent` behavior with transitions

### Dismissal

Modal destinations are regular stack entries. Dismiss by calling:
```kotlin
navigator.navigateBack()
```
Predictive back gestures (Android 13+ swipe, iOS swipe-back) work normally.

### Nested Modals

Multiple modal destinations can be stacked. The library walks backwards through 
the stack to find the first non-modal node, then renders all layers from that base 
to the top:

```
StackNode
├── ScreenNode (Home)          ← background (rendered)
├── ScreenNode (ModalA @Modal) ← middle layer (rendered)
└── ScreenNode (ModalB @Modal) ← top layer (rendered)
```

### Cross-Module Usage

Feature modules can use `@Modal` on their destinations independently. The generated 
`ModalRegistry` from each module is composed via `CompositeModalRegistry` when configs 
are combined with `+`.

### Validation Rules

| Rule | Severity | Example Message |
|------|----------|-----------------|
| Must co-exist with qualifying annotation | Error | `@Modal on 'InvalidModal' requires @Destination, @Tabs, @Stack, or @Pane in file 'Invalid.kt' (line 5). Fix: Add one of @Destination, @Tabs, @Stack, or @Pane` |

### MVI Integration

Modal screens work identically with `NavigationContainer` and `rememberContainer()`. 
No special MVI support is needed — the container lifecycle follows normal stack behavior.
```

#### Location: Update `### What Gets Generated` section (line ~16)

In the existing bullet list under "What Gets Generated" (near line 16–25), add a bullet:

```markdown
5. **Modal registry entries** — Mappings from destinations/containers to modal rendering behavior
```

#### Location: Update `## Generated Code` → `### What Gets Generated` section (line ~876)

In the "What Gets Generated" subsection under "Generated Code", add to the numbered list:

```markdown
5. **Modal registry entries** — `modal<T>()` and `modalContainer("name")` calls for `ModalRegistry`
```

---

### Task 1.2: docs/NAV-NODES.md — Modal Rendering Behavior

**Description:** Add a section on modal rendering to the StackNode documentation and add a Tree Structure example showing modals.

**Dependencies:** None

#### Location 1: New `#### Modal Rendering` subsection under `### StackNode`

Insert **after** the `#### Scope-Aware Navigation` subsection (after line ~167, before `#### When to Use` at line ~169).

#### Content Outline

```markdown
#### Modal Rendering

When the active child of a `StackNode` is registered as modal in the `ModalRegistry`, 
the `StackRenderer` uses a draw-behind pattern instead of standard animated transitions.

##### How StackRenderer Detects Modals

```kotlin
// For ScreenNode children:
modalRegistry.isModalDestination(screenNode.destination::class)

// For container children (TabNode, StackNode, PaneNode):
modalRegistry.isModalContainer(node.key)
```

##### ModalContent Box Layout

When a modal child is detected, `ModalContent` renders layers in a `Box`:

```
Box {
    // Layer 1: Background — the node below the modal in the stack
    NavNodeRenderer(backgroundNode)
    
    // Layer 2: Foreground — the modal node
    NavNodeRenderer(modalNode)
}
```

- Both layers use `StaticAnimatedVisibilityScope` (no animated enter/exit)
- Content goes through `NavNodeRenderer` → `ComposableCache` for state preservation
- No scrim, no chrome, no dismiss behavior — user composable controls everything

##### Nested Modal Support

When multiple consecutive modal nodes are on the stack, `StackRenderer` walks backwards 
through `node.children` to find the first non-modal node (`findNonModalBaseIndex`), 
then renders all layers from that base to the top:

```
// Stack: [Home, ModalA, ModalB]
// findNonModalBaseIndex → 0 (Home)
// Renders: Home (base) → ModalA (layer) → ModalB (layer)
```

##### Animation Bypass

Modal targets set `isTargetModal = true`, which bypasses `AnimatedContent` to prevent 
exit animations from removing the background. This is similar to how predictive back 
bypasses standard animation.

##### Edge Case: Modal as Only Child

If a modal destination is the only child in the stack (no screen below), it renders 
normally without a background layer.

##### Important

Modals do NOT create new node types. A modal destination is a regular `ScreenNode` 
(or `TabNode`/`StackNode`/`PaneNode`) — the modal flag is a runtime registry lookup, 
not a tree structure difference.
```

#### Location 2: Update `### Tree Structure Example` (line ~45)

After the existing ASCII tree, add a note:

```markdown
> **Modal nodes** are regular nodes in the tree. A `ScreenNode` marked `@Modal` appears 
> as a normal stack child — the modal behavior is determined at render time via `ModalRegistry`, 
> not by the node type.

Example with modal:
```
StackNode (root)
├── TabNode (MainTabs)
│   └── StackNode (HomeTab)
│       ├── ScreenNode (Home)
│       └── ScreenNode (Detail)
├── ScreenNode (NavigationMenu @Modal)  ← draw-behind rendering
└── ScreenNode (SettingsSheet @Modal)   ← nested modal layer
```
```

---

### Task 1.3: docs/ARCHITECTURE.md — Architecture Updates

**Description:** Update the architecture diagram, rendering layer docs, and StackRenderer description.

**Dependencies:** None

#### Location 1: Architecture Diagram (line ~41)

Add `ModalRegistry` to the architecture diagram. The diagram is an ASCII/text diagram showing layers. Add `ModalRegistry` alongside other registries in the Configuration layer.

Look for the existing diagram text and add `ModalRegistry` to the list of registries. Specific text depends on diagram format — find the line listing `ScreenRegistry`, `ContainerRegistry`, `TransitionRegistry`, etc. and append `ModalRegistry`.

#### Location 2: Rendering Layer → StackRenderer (line ~446)

Update the `#### StackRenderer` subsection to mention modal detection:

After the existing features list, add:

```markdown
- Detects modal children via `ModalRegistry` → renders `ModalContent` with draw-behind layering
- Supports nested modal stacking (walks back to find non-modal base)
- Bypasses `AnimatedContent` for modal targets to preserve background rendering
```

#### Location 3: New subsection after `#### PaneRenderer` (after line ~477)

Add:

```markdown
#### ModalContent

Renders modal destinations with draw-behind semantics inside a `Box`.

**Features**:
- Renders background (non-modal node) and foreground (modal node) as stacked layers
- Uses `StaticAnimatedVisibilityScope` for both layers
- Supports nested modals (3+ layers via recursive stacking)
- No scrim or dismiss behavior — user composable controls all presentation
- Content routed through `NavNodeRenderer` → `ComposableCache` for state preservation
```

#### Location 4: Table of Contents (line ~5)

If the Table of Contents lists renderers, add `ModalContent` to the list.

#### Location 5: Overview section (line ~27)

Add `ModalRegistry` to the overview text that describes registries. Find where it mentions `ScreenRegistry`, `ContainerRegistry`, etc. and add `ModalRegistry` to the list.

---

### Task 1.4: docs/DSL-CONFIG.md — ModalRegistry Section

**Description:** Add a complete `## ModalRegistry` section with interface definition, DSL builder functions, and examples.

**Dependencies:** None

#### Location: New `## ModalRegistry` section

Insert **after** `## RouteRegistry` section (after line ~524, before `## Complete Configuration Example` at line ~526).

#### Content Outline

```markdown
## ModalRegistry

### Purpose

The `ModalRegistry` tells the rendering system which destinations and containers should 
use draw-behind (modal) rendering instead of standard animated transitions.

### Interface

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

### Registering Modal Destinations

Use `modal<D>()` to register a destination class as modal:

```kotlin
val config = navigationConfig {
    // ... screens and containers ...

    // Register modal destinations
    modal<NavigationMenuDestination>()
    modal<SettingsSheetDestination>()
}
```

### Registering Modal Containers

Use `modalContainer("name")` to register a named container as modal:

```kotlin
val config = navigationConfig {
    // ... screens and containers ...

    // Register modal containers
    modalContainer("overlayTabs")
    modalContainer("detailStack")
}
```

### Combined Example

```kotlin
val config = navigationConfig {
    // Screen registry
    screen<HomeDestination.Feed> { dest, nav -> FeedScreen(nav) }
    screen<NavigationMenuDestination> { dest, nav -> NavigationMenuScreen(nav) }

    // Container registry
    stack("home", HomeDestination.Feed::class) { /* ... */ }
    tabs("mainTabs") { /* ... */ }

    // Modal registry
    modal<NavigationMenuDestination>()
    modal<SettingsSheetDestination>()
    modalContainer("overlayTabs")

    // Transitions
    transition<HomeDestination.Feed>(NavigationTransition.SlideHorizontal)
    transition<NavigationMenuDestination>(NavigationTransition.SlideVertical)
}
```

### Multi-Module Composition

When combining configs with `+`, modal registries are composited via `CompositeModalRegistry`:

```kotlin
val appConfig = mainModuleConfig + feature1Config + feature2Config
// Each module's modal registrations are merged — if any says modal, it's modal
```
```

#### Location: Update `### Key Components` (line ~24)

In the "Key Components" bullet list, add:

```markdown
- **ModalRegistry** — Flags destinations/containers for draw-behind rendering
```

#### Location: Update `### Interface Structure` (line ~41)

In the `NavigationConfig` interface code block, add:

```kotlin
val modalRegistry: ModalRegistry  // defaults to ModalRegistry.Empty
```

#### Location: Update `## Complete Configuration Example` (line ~526)

In the complete DSL example, add modal registration calls:

```kotlin
// Modal destinations
modal<NavigationMenuDestination>()
```

#### Location: Update `## Comparison: DSL vs Annotations` table (line ~692)

If the comparison table lists features by name, add a row:

```markdown
| Modal destinations | `@Modal` | `modal<D>()`, `modalContainer("name")` |
```

---

### Task 1.5: docs/TRANSITIONS.md — Modal Transition Behavior

**Description:** Add a section clarifying how transitions interact with modal destinations and update predictive back docs.

**Dependencies:** None

#### Location 1: New `## Modal Transitions` section

Insert **after** `## Stack vs Tab vs Pane Transitions` (after line ~706, before `## Examples from Demo App` at line ~708).

#### Content Outline

```markdown
## Modal Transitions

### Animation Bypass

When navigating TO a modal destination, `StackRenderer` bypasses `AnimatedContent` to prevent 
the exit animation from removing the background node. The `isTargetModal` flag triggers this bypass.

This means:
- The standard enter/exit transition pair does NOT run for modal navigation
- The modal composable appears immediately in the foreground layer
- The background node remains rendered (draw-behind)
- User composable can implement its own enter animation (e.g., slide-up for bottom sheet)

### Recommended Transitions for Modals

While the library bypasses `AnimatedContent` for modals, the `@Transition` annotation 
still influences predictive back animations:

| Pattern | Recommended Transition | Reason |
|---------|----------------------|--------|
| Bottom sheet | `SlideVertical` | Natural slide-up/down gesture |
| Dialog overlay | `Fade` | Smooth appear/disappear |
| Full-screen modal | `SlideVertical` | iOS-style presentation |
| No animation needed | `None` | Skip all animation |

### Predictive Back with Modals

Predictive back gestures work normally with modal destinations:
- The modal is a regular stack entry → back gesture does a stack pop
- `PredictiveBackContent` handles the dual rendering during the gesture
- The modal slides away revealing the background (which is already rendered)
- Completing the gesture removes the modal from the stack
- Cancelling the gesture restores the modal to its full position

### @Transition with @Modal

You can combine `@Transition` with `@Modal`:

```kotlin
@Modal
@Transition(type = TransitionType.SlideVertical)
@Destination(route = "settings_sheet")
data object SettingsSheet : HomeDestination()
```

The transition type is used for predictive back animation direction, but the forward 
navigation bypasses `AnimatedContent`.
```

#### Location 2: Update `## Predictive Back Animations` section (line ~382)

After the existing content in "Predictive Back Animations", add a note:

```markdown
### Predictive Back with Modal Destinations

Modal destinations participate in predictive back normally. Since modals use draw-behind 
rendering (the background is already visible), the back gesture smoothly transitions from 
the modal to the underlying screen. The `PredictiveBackContent` composable handles this 
dual rendering during the gesture.
```

#### Location 3: Update `### Stack Transitions` (line ~642)

Add a note in the Stack Transitions section:

```markdown
> **Note:** Modal destinations within a stack bypass the standard `AnimatedContent` 
> transition. See [Modal Transitions](#modal-transitions) for details.
```

---

## Phase 2: Documentation Site Content

### Task 2.1: New ModalNavigation Page

**Description:** Create a new dedicated Modal Navigation feature page for the documentation site.

**Dependencies:** Task 2.2 (code examples)

#### File: `docs/site/src/pages/Features/ModalNavigation/ModalNavigation.tsx` (NEW)

Follow the pattern of existing feature pages (e.g., `PaneLayouts.tsx`, `TabbedNavigation.tsx`). Use the shared `Features.module.css` styles.

#### Page Structure

```tsx
import { Link } from 'react-router-dom'
import CodeBlock from '@components/CodeBlock/CodeBlock'
import {
  modalAnnotationBasic,
  modalAnnotationContainer,
  modalDSLConfig,
  modalScreenExample,
  modalNestedExample,
  modalCrossModuleExample,
} from '@data/codeExamples'
import styles from '../Features.module.css'
```

#### Sections (with `id` anchors)

1. **`#overview`** — Overview
   - What modal navigation is: draw-behind rendering where background content is visible beneath the modal
   - Key principle: library renders layers, user controls all visual treatment
   - No scrim, no chrome, no dismiss behavior from the library
   - Use cases: bottom sheets, dialogs, full-screen overlays, glass effects, menus

2. **`#how-it-works`** — How It Works
   - Diagram/explanation of the draw-behind pattern:
     - `StackRenderer` detects modal child via `ModalRegistry`
     - Renders `ModalContent` Box with background + foreground layers
     - `NavNodeRenderer` handles each layer with caching
   - ASCII diagram of Box layout:
     ```
     Box {
       Background: NavNodeRenderer(previousNode)
       Foreground: NavNodeRenderer(modalNode)   ← user composable here
     }
     ```

3. **`#annotation-based`** — Annotation-Based (`@Modal`)
   - Marker annotation, no parameters
   - Must co-exist with `@Destination`, `@Tabs`, `@Stack`, or `@Pane`
   - Code example: `modalAnnotationBasic` (simple modal destination)
   - Code example: `modalAnnotationContainer` (modal tabs container)

4. **`#dsl-based`** — DSL-Based Configuration
   - `modal<D>()` for destinations
   - `modalContainer("name")` for containers
   - Code example: `modalDSLConfig`

5. **`#modal-rendering`** — Modal Rendering
   - `StackRenderer` detection: checks `ModalRegistry` for active child
   - `ModalContent` composable: simple `Box` with stacked layers
   - `StaticAnimatedVisibilityScope` for both layers
   - Animation bypass: `isTargetModal` flag skips `AnimatedContent`
   - Why: prevents exit animation from removing the visible background

6. **`#nested-modals`** — Nested Modals
   - Multiple modal layers supported
   - `findNonModalBaseIndex` walks backwards to find base
   - Code example: `modalNestedExample`
   - ASCII diagram:
     ```
     Stack: [Home] → [ModalA @Modal] → [ModalB @Modal]
     Rendered: Home (base) | ModalA (layer) | ModalB (top)
     ```

7. **`#custom-presentation`** — Custom Presentation
   - Library provides NO visual treatment — user composable controls everything
   - Examples of what the user composable can implement:
     - Bottom sheet with Material 3 `ModalBottomSheet`
     - Dialog with scrim and click-outside-to-dismiss
     - Glass/blur effect overlay
     - Full-screen modal with custom animation
   - Code example: `modalScreenExample` (screen with GlassBottomSheet)

8. **`#back-navigation`** — Back Navigation
   - `navigator.navigateBack()` dismisses the modal (stack pop)
   - Predictive back gestures work normally
   - No special dismiss API needed

9. **`#cross-module`** — Cross-Module Modals
   - Each feature module can declare `@Modal` destinations independently
   - `CompositeModalRegistry` merges registries when configs are combined with `+`
   - Code example: `modalCrossModuleExample`

10. **`#best-practices`** — Best Practices
    - Use `@Transition(type = TransitionType.SlideVertical)` for bottom sheets
    - Keep modal composables self-contained (own scrim, dismiss, animation)
    - Use `navigator.navigateBack()` for dismiss — don't create separate dismiss mechanisms
    - Test with `FakeNavigator` — modal behavior is just stack push/pop
    - Consider accessibility: ensure modals can be dismissed with back button/gesture

11. **`#next-steps`** — Next Steps
    - Links to: Annotations docs, DSL Config docs, Transitions docs, Nav Nodes docs
    - Link back to Core Concepts

---

### Task 2.2: Code Examples Data

**Description:** Add modal-related code example constants to the shared code examples file.

**Dependencies:** None

#### File: `docs/site/src/data/codeExamples.ts`

Append the following constants after the last existing export:

```typescript
// ============================================================================
// MODAL NAVIGATION EXAMPLES
// ============================================================================

/**
 * Basic modal destination with @Modal annotation
 */
export const modalAnnotationBasic = `@Modal
@Destination(route = "navigation_menu")
data object NavigationMenuDestination : NavDestination

@Screen(NavigationMenuDestination::class)
@Composable
fun NavigationMenuScreen(navigator: Navigator = koinInject()) {
    GlassBottomSheet(
        onDismissRequest = { navigator.navigateBack() },
    ) {
        // Menu content — user controls all visual treatment
        Column {
            Text("Navigation Menu")
            Button(onClick = { navigator.navigate(HomeDestination.Feed) }) {
                Text("Go to Feed")
            }
        }
    }
}`;

/**
 * Modal container (tabs pushed on stack)
 */
export const modalAnnotationContainer = `@Modal
@Tabs(name = "overlayTabs")
sealed class OverlayTabs : NavDestination {
    companion object : NavDestination

    @TabItem(OverlayTabs::class, ordinal = 0)
    @Destination(route = "overlay/info")
    data object InfoTab : OverlayTabs()

    @TabItem(OverlayTabs::class, ordinal = 1)
    @Destination(route = "overlay/actions")
    data object ActionsTab : OverlayTabs()
}`;

/**
 * DSL-based modal configuration
 */
export const modalDSLConfig = `val config = navigationConfig {
    // Screen registry
    screen<HomeDestination.Feed> { dest, nav -> FeedScreen(nav) }
    screen<NavigationMenuDestination> { dest, nav -> NavigationMenuScreen(nav) }

    // Container registry
    stack("home", HomeDestination.Feed::class) { /* ... */ }

    // Modal registry — flag destinations for draw-behind rendering
    modal<NavigationMenuDestination>()
    modal<SettingsSheetDestination>()
    modalContainer("overlayTabs")

    // Transitions
    transition<NavigationMenuDestination>(NavigationTransition.SlideVertical)
}`;

/**
 * Modal screen composable with custom presentation
 */
export const modalScreenExample = `@Screen(NavigationMenuDestination::class)
@Composable
fun NavigationMenuScreen(navigator: Navigator = koinInject()) {
    // User controls ALL visual treatment:
    // - Scrim/backdrop
    // - Dismiss behavior (tap outside, swipe down)
    // - Animation (slide up, fade in)
    // - Shape, elevation, padding
    GlassBottomSheet(
        onDismissRequest = { navigator.navigateBack() },
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Navigation Menu", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            
            NavigationMenuItem("Home") { 
                navigator.navigate(HomeDestination.Feed) 
            }
            NavigationMenuItem("Profile") { 
                navigator.navigate(HomeDestination.Profile) 
            }
            NavigationMenuItem("Settings") { 
                navigator.navigate(HomeDestination.Settings) 
            }
        }
    }
}`;

/**
 * Nested modals example
 */
export const modalNestedExample = `// First modal — navigation menu
@Modal
@Transition(type = TransitionType.SlideVertical)
@Destination(route = "navigation_menu")
data object NavigationMenu : HomeDestination()

// Second modal — can be pushed on top of first
@Modal
@Transition(type = TransitionType.Fade)
@Destination(route = "confirmation_dialog")
data object ConfirmationDialog : HomeDestination()

// Navigating:
// navigator.navigate(NavigationMenu)    → Home (bg) + Menu (fg)
// navigator.navigate(ConfirmationDialog) → Home (bg) + Menu (mid) + Dialog (fg)
// navigator.navigateBack()              → Home (bg) + Menu (fg)
// navigator.navigateBack()              → Home (full screen)`;

/**
 * Cross-module modal example
 */
export const modalCrossModuleExample = `// In feature1 module:
@Modal
@Destination(route = "feature1/overlay")
data object Feature1Overlay : Feature1Destination()

// In feature2 module:
@Modal
@Destination(route = "feature2/sheet")
data object Feature2Sheet : Feature2Destination()

// Generated configs are combined:
val appConfig = mainConfig + feature1Config + feature2Config
// CompositeModalRegistry merges all modal registrations`;
```

---

### Task 2.3: Navigation Data Entry

**Description:** Add the Modal Navigation entry to the site navigation.

**Dependencies:** None

#### File: `docs/site/src/data/navigation.ts`

Insert a new entry in the Features children array, **after** `Pane Layouts` (after line ~38):

```typescript
{ label: 'Modal Navigation', path: '/features/modal-navigation' },
```

The Features children array should become:
```typescript
children: [
  { label: 'Core Concepts', path: '/features/core-concepts' },
  { label: 'Annotation-Based API', path: '/features/annotation-api' },
  { label: 'DSL Configuration', path: '/features/dsl-config' },
  { label: 'Type-Safe Navigation', path: '/features/type-safe' },
  { label: 'Multiplatform Support', path: '/features/multiplatform' },
  { label: 'Deep Links', path: '/features/deep-links' },
  { label: 'Predictive Back', path: '/features/predictive-back' },
  { label: 'Transitions & Animations', path: '/features/transitions' },
  { label: 'Tabbed Navigation', path: '/features/tabbed-navigation' },
  { label: 'Pane Layouts', path: '/features/pane-layouts' },
  { label: 'Modal Navigation', path: '/features/modal-navigation' },  // NEW
  {
    label: 'FlowMVI & Koin',
    // ...
  },
  { label: 'Testing Support', path: '/features/testing' },
  { label: 'Modular Architecture', path: '/features/modular' },
]
```

---

### Task 2.4: App.tsx Route Registration

**Description:** Add the route for the new Modal Navigation page.

**Dependencies:** Task 2.1 (page component exists)

#### File: `docs/site/src/App.tsx`

1. Add import at the top with other feature page imports (after line ~25):

```typescript
import ModalNavigation from '@pages/Features/ModalNavigation/ModalNavigation'
```

2. Add route inside `<Routes>`, after the `pane-layouts` route (after line ~54):

```tsx
<Route path="/features/modal-navigation" element={<ModalNavigation />} />
```

---

### Task 2.5: AnnotationAPI Page Updates

**Description:** Add `@Modal` to the Core Annotations grid and add a `#modal-annotation` section.

**Dependencies:** Task 2.2 (code examples)

#### File: `docs/site/src/pages/Features/AnnotationAPI/AnnotationAPI.tsx`

#### Change 1: Add import for modal code example

Add to the imports from `@data/codeExamples`:

```typescript
import {
  // ... existing imports ...
  modalAnnotationBasic,
} from '@data/codeExamples'
```

#### Change 2: Add card to Core Annotations grid

Add a new card in the second `annotationGrid` div (after the `Validation` card, around line ~185):

```tsx
<a href="#modal-annotation" className={styles.annotationCard}>
  <h4>@Modal</h4>
  <p>Marks destinations for draw-behind modal rendering with user-controlled presentation.</p>
</a>
```

#### Change 3: Add `#modal-annotation` section

Add a new `<section>` **after** the `@Transition` section and **before** the `Generated Code` section. Follow the existing section pattern:

```tsx
<section>
  <h2 id="modal-annotation">@Modal Annotation</h2>
  <p>
    The <code>@Modal</code> annotation marks a destination or container for draw-behind 
    rendering. When pushed onto a stack, the library renders both the screen below and 
    the modal on top — the user composable controls all visual treatment.
  </p>

  <h3>Properties</h3>
  <p>
    <code>@Modal</code> is a marker annotation with no parameters. It must appear 
    alongside <code>@Destination</code>, <code>@Tabs</code>, <code>@Stack</code>, 
    or <code>@Pane</code>.
  </p>

  <h3>Example: Modal Destination</h3>
  <CodeBlock code={modalAnnotationBasic} language="kotlin" />

  <h3>Key Behaviors</h3>
  <ul>
    <li>Library renders background + modal in a <code>Box</code> — no scrim, no chrome</li>
    <li>User composable controls all presentation (bottom sheet, dialog, glass effect)</li>
    <li>Dismissal via <code>navigator.navigateBack()</code> (standard stack pop)</li>
    <li>Nested modals supported (multiple layers stack)</li>
    <li>Predictive back gestures work normally</li>
  </ul>
  
  <p>
    See the <Link to="/features/modal-navigation">Modal Navigation</Link> guide 
    for complete documentation.
  </p>
</section>
```

---

### Task 2.6: CoreConcepts Page Updates

**Description:** Update the NavNode tree example and rendering description to mention modals.

**Dependencies:** None

#### File: `docs/site/src/pages/Features/CoreConcepts/CoreConcepts.tsx`

#### Change 1: Update ASCII tree example

Find the existing ASCII tree diagram in the page. Add a modal example node. If the tree looks like:

```
StackNode (root)
├── TabNode (MainTabs)
...
```

Update to include:

```
StackNode (root)
├── TabNode (MainTabs)
│   ├── StackNode (HomeTab)
│   │   ├── ScreenNode (Home)
│   │   └── ScreenNode (Detail)
│   └── StackNode (ProfileTab)
│       └── ScreenNode (Profile)
├── ScreenNode (Menu @Modal)        ← draw-behind rendering
└── PaneNode (adaptive layout)
    ├── StackNode (primary)
    └── StackNode (detail)
```

#### Change 2: Add modal mention to rendering description

Find the section that describes how `StackRenderer` works. Add a brief note:

> When a child node is registered as modal, `StackRenderer` renders both the background node 
> and the modal in a layered `Box` instead of using animated transitions.

#### Change 3: Add `ModalRegistry` mention

Find where other registries are listed (ScreenRegistry, ContainerRegistry, TransitionRegistry). Add `ModalRegistry` to the list with brief description:

> **ModalRegistry** — Flags destinations/containers for draw-behind rendering

---

### Task 2.7: DSLConfig Page Updates

**Description:** Add modal registry section to the DSL configuration page.

**Dependencies:** Task 2.2 (code examples)

#### File: `docs/site/src/pages/Features/DSLConfig/DSLConfig.tsx`

#### Change 1: Add import for modal code example

```typescript
import { modalDSLConfig } from '@data/codeExamples'
```

#### Change 2: Add Modal Registry section

Find the section after `RouteRegistry` (or at an appropriate location) and add:

```tsx
<section>
  <h2 id="modal-registry">Modal Registry</h2>
  <p>
    The <code>ModalRegistry</code> tells the rendering system which destinations and containers
    should use draw-behind (modal) rendering.
  </p>

  <h3>Builder Functions</h3>
  <table className={styles.propsTable}>
    <thead>
      <tr><th>Function</th><th>Purpose</th></tr>
    </thead>
    <tbody>
      <tr><td><code>modal&lt;D&gt;()</code></td><td>Register a destination class as modal</td></tr>
      <tr><td><code>modalContainer("name")</code></td><td>Register a named container as modal</td></tr>
    </tbody>
  </table>

  <h3>Example</h3>
  <CodeBlock code={modalDSLConfig} language="kotlin" />
</section>
```

#### Change 3: Update comparison table

If the page has a DSL vs Annotations comparison table, add a row for modal.

---

## Phase 3: Supporting Files

### Task 3.1: README.md Updates

**Description:** Add modal navigation to the features list and update the architecture tree.

**Dependencies:** None

#### File: `README.md`

#### Change 1: Add to Key Features list (after line ~48)

Insert after the existing feature bullets, in the appropriate position (after "Adaptive Layouts" or "Custom Transitions"):

```markdown
- ✅ **Modal Navigation** - Draw-behind rendering for bottom sheets, dialogs, and overlays via `@Modal`
```

#### Change 2: Update Architecture tree (after line ~383)

If space allows, add a modal node to the ASCII tree:

```
NavNode (root)
├── StackNode (main stack)
│   ├── ScreenNode (Home)
│   ├── ScreenNode (List)
│   ├── ScreenNode (Detail)
│   └── ScreenNode (Menu @Modal)    ← draw-behind rendering
├── TabNode (bottom tabs)
│   ├── StackNode (Tab 1 stack)
│   │   └── ScreenNode
│   └── StackNode (Tab 2 stack)
│       └── ScreenNode
└── PaneNode (adaptive layout)
    ├── StackNode (primary)
    └── StackNode (detail)
```

#### Change 3: Update Node Types table (after line ~407)

Add to the node types table note:

> **Note:** Modal nodes use the same node types above. `@Modal` is a rendering flag, not a new node type.

---

### Task 3.2: copilot-instructions.md Updates

**Description:** Add `@Modal` to the annotations documentation in the Copilot instructions file.

**Dependencies:** None

#### File: `.github/copilot-instructions.md`

#### Change 1: Update the Annotations section

In the `### Defining Destinations` section or nearby, add a note about `@Modal`:

```markdown
### Modal Destinations

```kotlin
@Modal
@Destination(route = "navigation_menu")
data object NavigationMenuDestination : NavDestination
```

- `@Modal` (marker, no params) — marks destination for draw-behind rendering
- Must co-exist with `@Destination`, `@Tabs`, `@Stack`, or `@Pane`
- Library renders background + modal in `Box` — user controls all visual treatment
- Dismiss via `navigator.navigateBack()` (standard stack pop)
```

#### Change 2: Update the KSP annotations list

In the module table for `quo-vadis-annotations`, update the parenthetical:

```
`@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@Pane`, `@Modal`
```

#### Change 3: Update Key Patterns section

Add to the Key Patterns numbered list:

```markdown
6. **Modal rendering**: `@Modal` destinations render with draw-behind layering in `StackRenderer`
```

#### Change 4: Update Naming conventions

Add to the "Code Style & Conventions" section:

```markdown
- **Modal destinations**: `@Modal` marker annotation on `@Destination`/`@Tabs`/`@Stack`/`@Pane` classes
```

---

## Sequencing & Dependencies

```
Phase 1 (all tasks independent — can be done in parallel)
├── Task 1.1: ANNOTATIONS.md
├── Task 1.2: NAV-NODES.md
├── Task 1.3: ARCHITECTURE.md
├── Task 1.4: DSL-CONFIG.md
└── Task 1.5: TRANSITIONS.md

Phase 2 (some dependencies)
├── Task 2.2: Code Examples ← independent, do first
├── Task 2.3: Navigation Data ← independent
├── Task 2.1: ModalNavigation page ← depends on 2.2
├── Task 2.4: App.tsx route ← depends on 2.1
├── Task 2.5: AnnotationAPI updates ← depends on 2.2
├── Task 2.6: CoreConcepts updates ← independent
└── Task 2.7: DSLConfig updates ← depends on 2.2

Phase 3 (independent — can be done in parallel with Phase 2)
├── Task 3.1: README.md
└── Task 3.2: copilot-instructions.md
```

### Recommended execution order

1. **Phase 1** tasks (all parallel)
2. **Task 2.2** (code examples — unblocks several Phase 2 tasks)
3. **Task 2.3** (navigation data)
4. **Task 2.1** (new page — largest task)
5. **Tasks 2.4, 2.5, 2.6, 2.7** (remaining site updates — parallel)
6. **Phase 3** tasks (parallel)

### Effort Estimates

| Task | Size | Notes |
|------|------|-------|
| 1.1 ANNOTATIONS.md | Medium | ~150 lines of new content |
| 1.2 NAV-NODES.md | Medium | ~100 lines of new content |
| 1.3 ARCHITECTURE.md | Small | ~30 lines across 5 locations |
| 1.4 DSL-CONFIG.md | Medium | ~120 lines of new section + scattered updates |
| 1.5 TRANSITIONS.md | Medium | ~80 lines of new section + scattered notes |
| 2.1 ModalNavigation.tsx | Large | New ~400-line page component |
| 2.2 codeExamples.ts | Small | ~100 lines of string constants |
| 2.3 navigation.ts | Trivial | 1 line |
| 2.4 App.tsx | Trivial | 2 lines (import + route) |
| 2.5 AnnotationAPI.tsx | Small | ~30 lines across 3 locations |
| 2.6 CoreConcepts.tsx | Small | ~20 lines across 3 locations |
| 2.7 DSLConfig.tsx | Small | ~30 lines |
| 3.1 README.md | Small | ~10 lines across 3 locations |
| 3.2 copilot-instructions.md | Small | ~15 lines across 4 locations |

---

## Verification Checklist

After completing all tasks:

- [ ] `@Modal` annotation documented in ANNOTATIONS.md with examples, validation rules
- [ ] Modal rendering explained in NAV-NODES.md under StackNode
- [ ] Architecture diagram includes ModalRegistry
- [ ] DSL-CONFIG.md has ModalRegistry section with `modal<D>()` and `modalContainer()`
- [ ] TRANSITIONS.md explains modal animation bypass and predictive back interaction
- [ ] New ModalNavigation.tsx page loads at `/features/modal-navigation`
- [ ] Navigation sidebar shows "Modal Navigation" entry
- [ ] Code examples compile (verify Kotlin syntax correctness)
- [ ] All cross-references/links between docs are valid
- [ ] AnnotationAPI page shows @Modal card in grid
- [ ] CoreConcepts tree example includes modal node
- [ ] DSLConfig page shows modal registry section
- [ ] README features list includes modal navigation
- [ ] copilot-instructions.md mentions @Modal
- [ ] No broken internal links between markdown docs
- [ ] Site builds without TypeScript errors (`npm run build` in docs/site)
